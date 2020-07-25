package q19.kenes_widget.ui.presentation

import android.os.Parcelable
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.RequestParams
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import q19.kenes_widget.model.*
import q19.kenes_widget.network.file.DownloadResult
import q19.kenes_widget.network.file.downloadFile
import q19.kenes_widget.network.file.uploadFile
import q19.kenes_widget.network.http.IceServersTask
import q19.kenes_widget.network.http.WidgetConfigsTask
import q19.kenes_widget.network.socket.SocketClient
import q19.kenes_widget.util.FileUtil.getFileType
import q19.kenes_widget.util.Logger.debug
import q19.kenes_widget.util.UrlUtil
import java.io.File

internal class KenesWidgetV2Presenter(
    private val language: Language,
    private val palette: IntArray
) {

    companion object {
        private const val TAG = "KenesWidgetV2Presenter"
    }

    private var view: KenesWidgetV2View? = null

    fun attachView(view: KenesWidgetV2View) {
        this.view = view

        onFirstViewAttach()
    }

    fun detachView() {
        viewState = ViewState.ChatBot.Categories(false)

        chatListViewState = null

        configs?.clear()
        configs = null

        chatBot.clear()

        dialog.clear()

        socketClient?.release()
        socketClient?.listener = null
        socketClient = null
    }

    private var viewState: ViewState = ViewState.ChatBot.Categories(false)
        set(value) {
            field = value

            if (value is ViewState.ChatBot || value is ViewState.Contacts || value is ViewState.Info || value is ViewState.TextDialog.IDLE || value is ViewState.AudioDialog.IDLE || value is ViewState.VideoDialog.IDLE) {
                configs?.opponent?.let { view?.showOpponentInfo(it) }
            }

            if (value is ViewState.ChatBot.UserPrompt) {
                chatBot.activeCategory = null
            }

            if (value is ViewState.AudioDialog.Live || value is ViewState.VideoDialog.Live) {
                dialog.unreadMessages = 0
            }

            view?.setViewState(value)
        }

    private val httpClient by lazy { AsyncHttpClient() }

    private var socketClient: SocketClient? = null

    private var configs: Configs? = null
    private var chatBot = ChatBot()
    private var dialog = Dialog()

    private var iceServers = listOf<PeerConnection.IceServer>()

    private var chatListViewState: Parcelable? = null

    fun setChatListViewState(chatListViewState: Parcelable?) {
        this.chatListViewState = chatListViewState
    }

    private fun onFirstViewAttach() {
        listOf(
            BottomNavigation.HOME,
            BottomNavigation.VIDEO,
            BottomNavigation.AUDIO,
            BottomNavigation.CONTACTS
        ).forEach {
            view?.hideNavButton(it)
        }
        view?.showNavButton(BottomNavigation.INFO)

        view?.showOpponentInfo(Configs.Opponent.getDefault())

        view?.showCurrentLanguage(language)

        view?.hideHangupButton()

        chatBot.callback = object : ChatBot.Callback {
            override fun onBasicCategoriesLoaded(categories: List<Category>) {
                val messages = categories
                    .sortedBy { it.id }
                    .mapIndexed { index, category ->
                        if (palette.isNotEmpty()) {
                            category.color = palette[index % palette.size]
                        }
                        Message(type = Message.Type.CATEGORY, category = category)
                    }

                view?.clearChatFooterMessages()
                view?.setNewMessages(messages)

                viewState = ViewState.ChatBot.Categories(false)
            }
        }

        fetchWidgetConfigs()
        fetchIceServers()

        initSocket()
    }

    private fun initSocket() {
        val socketUrl = UrlUtil.getSocketUrl()
        if (socketUrl.isNullOrBlank()) {
            throw NullPointerException("Signalling server url is null. Please, provide a valid url.")
        } else {
            debug(TAG, "initSocket() -> socketUrl: $socketUrl")
            socketClient = SocketClient()
            socketClient?.start(socketUrl, language.locale.language)
        }

        socketClient?.listener = object : SocketClient.Listener {
            override fun onConnect() {
                viewState = if (configs?.isChabotEnabled == true) {
                    socketClient?.getBasicCategories()
                    ViewState.ChatBot.Categories(true)
                } else {
                    when {
                        configs?.isVideoCallEnabled == true ->
                            ViewState.VideoDialog.IDLE
                        configs?.isAudioCallEnabled == true ->
                            ViewState.AudioDialog.IDLE
                        configs?.isContactSectionsShown == true ->
                            ViewState.Contacts
                        else ->
                            ViewState.Info
                    }
                }
            }

            override fun onOperatorGreet(fullName: String, photoUrl: String?, text: String) {
                debug(TAG, "onCallAgentGreet -> viewState: $viewState")

                if (viewState is ViewState.TextDialog) {
                    viewState = ViewState.TextDialog.Live
                }

                val newText = text.replace("{}", fullName)

                view?.showOpponentInfo(fullName, photoUrl)

                if (viewState is ViewState.AudioDialog) {
                    view?.showAudioCallerInformation(fullName, photoUrl)
                }

                view?.addNewMessage(Message(type = Message.Type.OPPONENT, text = newText))
            }

            override fun onFormInit(dynamicForm: DynamicForm) {}

            override fun onFeedback(text: String, ratingButtons: List<RatingButton>) {
                debug(TAG, "onFeedback -> viewState: $viewState")

                view?.showFeedback(text, ratingButtons)

                when (viewState) {
                    is ViewState.TextDialog ->
                        viewState = ViewState.TextDialog.UserFeedback(false)
                    is ViewState.AudioDialog ->
                        viewState = ViewState.AudioDialog.UserFeedback(false)
                    is ViewState.VideoDialog ->
                        viewState = ViewState.VideoDialog.UserFeedback(false)
                }
            }

            override fun onPendingUsersQueueCount(text: String?, count: Int) {
                if (viewState is ViewState.AudioDialog) {
                    if (!text.isNullOrBlank()) {
                        view?.setAudioCallInfoText(text)
                    }
                    if (count > 1) {
                        view?.setAudioCallPendingQueueCount(count)
                    }
                } else if (viewState is ViewState.VideoDialog) {
                    if (!text.isNullOrBlank()) {
                        view?.setVideoCallInfoText(text)
                    }
                    if (count > 1) {
                        view?.setVideoCallPendingQueueCount(count)
                    }
                }
            }

            override fun onNoResultsFound(text: String, timestamp: Long): Boolean {
                debug(TAG, "onNoResultsFound -> viewState: $viewState")

                view?.addNewMessage(
                    Message(
                        type = Message.Type.OPPONENT,
                        text = text,
                        timestamp = timestamp
                    )
                )

                if (viewState is ViewState.ChatBot.UserPrompt && !dialog.isInitiator) {
                    view?.showSwitchToCallAgentButton()
                }

                if (viewState is ViewState.ChatBot.UserPrompt) {
                    viewState = ViewState.ChatBot.UserPrompt(false)
                }

                return true
            }

            override fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean {
                debug(TAG, "onFuzzyTaskOffered -> viewState: $viewState")

                view?.addNewMessage(
                    Message(
                        type = Message.Type.OPPONENT,
                        text = text,
                        timestamp = timestamp
                    )
                )
                view?.showFuzzyQuestionButtons()

                viewState = ViewState.ChatBot.UserPrompt(false)

                return true
            }

            override fun onNoOnlineOperators(text: String): Boolean {
                debug(TAG, "onNoOnlineCallAgents -> viewState: $viewState")

                dialog.isInitiator = false

                view?.addNewMessage(Message(type = Message.Type.OPPONENT, text = text))

                view?.showNoOnlineCallAgentsAlert(text)

                when (viewState) {
                    is ViewState.TextDialog -> {
                        view?.showGoToHomeButton()
                        viewState = ViewState.ChatBot.UserPrompt(false)
                    }
                    is ViewState.AudioDialog ->
                        viewState = ViewState.AudioDialog.IDLE
                    is ViewState.VideoDialog ->
                        viewState = ViewState.VideoDialog.IDLE
                }

                return true
            }

            override fun onChatTimeout(text: String, timestamp: Long): Boolean {
                debug(TAG, "onChatTimeout -> viewState: $viewState")

                disconnect(text, timestamp)

                return true
            }

            override fun onOperatorDisconnected(text: String, timestamp: Long): Boolean {
                debug(TAG, "onCallAgentDisconnected -> viewState: $viewState")

                disconnect(text, timestamp)

                return true
            }

            private fun disconnect(text: String, timestamp: Long) {
                onCloseLiveCall()

                view?.addNewMessage(
                    Message(
                        type = Message.Type.NOTIFICATION,
                        text = text,
                        timestamp = timestamp
                    )
                )
                view?.showGoToHomeButton()
            }

            override fun onCallAccept() {
                debug(TAG, "onCallAccept -> viewState: $viewState")

                if (viewState is ViewState.AudioDialog) {
                    viewState = ViewState.AudioDialog.Start

                    view?.createPeerConnection(
                        isMicrophoneEnabled = true,
                        isCameraEnabled = false,
                        iceServers = iceServers
                    )

                    socketClient?.sendMessage(rtc = rtc { type = RTC.Type.PREPARE })
                } else if (viewState is ViewState.VideoDialog) {
                    viewState = ViewState.VideoDialog.Start

                    view?.createPeerConnection(
                        isMicrophoneEnabled = true,
                        isCameraEnabled = true,
                        iceServers = iceServers
                    )

                    view?.initLocalVideoStream()

                    socketClient?.sendMessage(rtc = rtc { type = RTC.Type.PREPARE })
                }
            }

            override fun onRTCPrepare() {
                debug(TAG, "onRTCPrepare: $viewState")

                if (viewState is ViewState.AudioDialog) {
                    viewState = ViewState.AudioDialog.Preparation

                    view?.startLocalMediaStream()

                    socketClient?.sendMessage(rtc = rtc { type = RTC.Type.READY })
                } else if (viewState is ViewState.VideoDialog) {
                    viewState = ViewState.VideoDialog.Preparation

                    view?.startLocalMediaStream()

                    socketClient?.sendMessage(rtc = rtc { type = RTC.Type.READY })
                }
            }

            override fun onRTCReady() {
                debug(TAG, "onRTCReady -> viewState: $viewState")

                if (viewState is ViewState.AudioDialog) {
                    viewState = ViewState.AudioDialog.Ready

                    view?.startLocalMediaStream()
                    view?.sendOfferToOpponent()
                } else if (viewState is ViewState.VideoDialog) {
                    viewState = ViewState.VideoDialog.Ready

                    view?.startLocalMediaStream()
                    view?.sendOfferToOpponent()
                }
            }

            override fun onRTCOffer(sessionDescription: SessionDescription) {
                view?.setRemoteDescription(sessionDescription)
                view?.sendAnswerToOpponent()
            }

            override fun onRTCAnswer(sessionDescription: SessionDescription) {
                view?.setRemoteDescription(sessionDescription)
            }

            override fun onRTCIceCandidate(iceCandidate: IceCandidate) {
                view?.addRemoteIceCandidate(iceCandidate)
            }

            override fun onRTCHangup() {
                onCloseLiveCall()
            }

            override fun onTextMessage(
                text: String,
                attachments: List<Attachment>?,
                timestamp: Long
            ) {
                debug(TAG, "onTextMessage -> viewState: $viewState")

                if (chatBot.activeCategory != null) {
                    view?.setNewMessages(
                        Message(
                            type = Message.Type.RESPONSE,
                            text = text,
                            attachments = attachments,
                            timestamp = timestamp,
                            category = chatBot.activeCategory
                        )
                    )
                    view?.showGoToHomeButton()

                    viewState = ViewState.ChatBot.Categories(false)

                    return
                }

                if (viewState is ViewState.AudioDialog.Live || viewState is ViewState.VideoDialog.Live) {
                    view?.showFooterView()
                }

                if (viewState is ViewState.AudioDialog.Live && (viewState as ViewState.AudioDialog.Live).isDialogScreenShown) {
                    dialog.unreadMessages += 1
                    if (dialog.unreadMessages >= Dialog.MAX_UNREAD_MESSAGES_COUNT) {
                        view?.setUnreadMessagesCountOnCall(
                            OperatorCall.AUDIO,
                            "${dialog.unreadMessages}+"
                        )
                    } else {
                        view?.setUnreadMessagesCountOnCall(
                            OperatorCall.AUDIO,
                            "${dialog.unreadMessages}"
                        )
                    }
                } else if (viewState is ViewState.VideoDialog.Live && (viewState as ViewState.VideoDialog.Live).isDialogScreenShown) {
                    dialog.unreadMessages += 1
                    if (dialog.unreadMessages >= Dialog.MAX_UNREAD_MESSAGES_COUNT) {
                        view?.setUnreadMessagesCountOnCall(
                            OperatorCall.VIDEO,
                            "${dialog.unreadMessages}+"
                        )
                    } else {
                        view?.setUnreadMessagesCountOnCall(
                            OperatorCall.VIDEO,
                            "${dialog.unreadMessages}"
                        )
                    }
                }

                view?.addNewMessage(
                    Message(
                        type = Message.Type.OPPONENT,
                        text = text,
                        attachments = attachments,
                        timestamp = timestamp
                    )
                )

                if (viewState is ViewState.ChatBot) {
                    debug(TAG, "onTextMessage: chatFooterAdapter?.showGoToHomeButton()")

                    view?.showGoToHomeButton()

                    viewState = ViewState.ChatBot.UserPrompt(false)
                }
            }

            override fun onMediaMessage(media: Media, timestamp: Long) {
                if (media.isImage || media.isAudio || media.isFile) {
                    view?.addNewMessage(
                        Message(
                            type = Message.Type.OPPONENT,
                            media = media,
                            timestamp = timestamp
                        )
                    )
                }
            }

            override fun onEmptyMessage() {
                if (viewState is ViewState.ChatBot.Categories) {
                    if (chatBot.activeCategory != null) {
                        viewState = ViewState.ChatBot.Categories(false)
                    }
                }
            }

            override fun onCategories(categories: List<Category>) {
//                debug(TAG, "onCategories -> viewState: $viewState")

                if (viewState is ViewState.ChatBot.UserPrompt) return

                val sortedCategories = categories.sortedBy { it.id }
                chatBot.allCategories.addAll(sortedCategories)

                if (!chatBot.isBasicCategoriesFilled) {
                    chatBot.allCategories.forEach { category ->
//                        debug(TAG, "category: $category, ${category.parentId == null}")

                        if (category.parentId == null) {
                            socketClient?.getCategories(category.id)
                        }
                    }

                    chatBot.isBasicCategoriesFilled = true
                }

                if (chatBot.activeCategory != null) {
                    if (chatBot.activeCategory?.children?.containsAll(sortedCategories) == false) {
                        chatBot.activeCategory?.children?.addAll(sortedCategories)
                    }
                    view?.setNewMessages(
                        Message(
                            type = Message.Type.CROSS_CHILDREN,
                            category = chatBot.activeCategory
                        )
                    )
                    view?.showGoToHomeButton()
                }

                if (viewState is ViewState.ChatBot.Categories && (viewState as ViewState.ChatBot.Categories).isLoading) {
                    viewState = ViewState.ChatBot.Categories(false)
                }
            }

            override fun onDisconnect() {
                onCloseLiveCall()
            }
        }
    }

    private fun fetchWidgetConfigs() {
        val url = UrlUtil.buildUrl("/configs") ?: return
        debug(TAG, "fetchWidgetConfigs() -> url: $url")

        val task = WidgetConfigsTask(url)

        val data = task.run()

        debug(TAG, "fetchWidgetConfigs() -> data: $data")

        if (data == null) {
            view?.showOpponentInfo(Configs.Opponent.getDefault())

            viewState = when {
                configs?.isChabotEnabled == true ->
                    ViewState.ChatBot.Categories(false)
                configs?.isVideoCallEnabled == true ->
                    ViewState.VideoDialog.IDLE
                configs?.isAudioCallEnabled == true ->
                    ViewState.AudioDialog.IDLE
                configs?.isContactSectionsShown == true ->
                    ViewState.Contacts
                else ->
                    ViewState.Info
            }
        } else {
            configs = data.also { configs ->
                configs.contacts?.let { contacts ->
                    if (!contacts.isNullOrEmpty()) {
                        view?.showContacts(contacts)
                    }
                }

                if (configs.isPhonesListShown) {
                    configs.phones?.let { phones ->
                        if (!phones.isNullOrEmpty()) {
                            view?.showPhones(phones)
                        }
                    }
                }

                configs.opponent?.let {
                    view?.showOpponentInfo(it)
                }

                if (configs.isChabotEnabled) {
                    view?.setDefaultFooterView()
                    view?.showNavButton(BottomNavigation.HOME)
                } else {
                    view?.hideNavButton(BottomNavigation.HOME)
                }

                if (configs.isAudioCallEnabled) {
                    view?.setDefaultAudioCallView()
                    view?.showNavButton(BottomNavigation.AUDIO)
                } else {
                    view?.hideNavButton(BottomNavigation.AUDIO)
                }

                if (configs.isVideoCallEnabled) {
                    view?.setDefaultVideoCallView()
                    view?.showNavButton(BottomNavigation.VIDEO)
                } else {
                    view?.hideNavButton(BottomNavigation.VIDEO)
                }

                if (configs.isContactSectionsShown && !configs.infoBlocks.isNullOrEmpty()) {
                    view?.showInfoBlocks(configs.infoBlocks)
                    view?.showNavButton(BottomNavigation.CONTACTS)
                } else {
                    view?.hideNavButton(BottomNavigation.CONTACTS)
                }
            }
        }
    }

    private fun fetchIceServers() {
        val url = UrlUtil.buildUrl("/ice_servers") ?: return
        debug(TAG, "fetchIceServers -> url: $url")

        val task = IceServersTask(url)

        val data = task.run()

        data?.let {
            iceServers = data.map {
                PeerConnection.IceServer.builder(it.url)
                    .setUsername(it.username)
                    .setPassword(it.credential)
                    .createIceServer()
            }
        }
    }

    fun onMediaClicked(media: Media, file: File, itemPosition: Int) {
        if (file.exists()) {
            if (media.isAudio) {
                view?.playAudio(file.absolutePath, itemPosition)
            } else if (media.isFile) {
                view?.openFile(file)
            }
        } else {
            try {
                if (media.isAudio) {
                    file.downloadFile(media.audioUrl, "media", itemPosition) {
                        view?.playAudio(file.absolutePath, itemPosition)
                    }
                } else if (media.isFile) {
                    file.downloadFile(media.fileUrl, "media", itemPosition) {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun File.downloadFile(
        url: String?,
        fileType: String,
        itemPosition: Int,
        callback: () -> Unit
    ) {
        if (url.isNullOrBlank()) return
        httpClient.downloadFile(this, url) { downloadResult ->
            when (downloadResult) {
                is DownloadResult.Success -> {
                    callback()
                    view?.showFileDownloadStatus(
                        Message.File.DownloadStatus.COMPLETED,
                        itemPosition
                    )
                }
                is DownloadResult.Error ->
                    view?.showFileDownloadStatus(Message.File.DownloadStatus.ERROR, itemPosition)
                is DownloadResult.Progress ->
                    view?.showFileDownloadProgress(downloadResult.progress, fileType, itemPosition)
            }
        }
    }

    fun onAttachmentClicked(attachment: Attachment, file: File, itemPosition: Int) {
        if (file.exists()) {
            view?.openFile(file)
        } else {
            try {
                file.downloadFile(attachment.url, "attachment", itemPosition) {
                    view?.openFile(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onNavButtonClicked(bottomNavigation: BottomNavigation) {
        fun clear() {
            chatBot.clear()
            view?.clearChatMessages()
            view?.clearChatFooterMessages()
        }

        when (bottomNavigation) {
            BottomNavigation.HOME -> {
                if (configs?.isChabotEnabled == false) return

                if (dialog.isInitiator) {
                    view?.showAlreadyCallingAlert(bottomNavigation)
                    return
                }

                clear()

                socketClient?.getBasicCategories()
                viewState = ViewState.ChatBot.Categories(true)
            }
            BottomNavigation.AUDIO -> {
                if (dialog.isInitiator) {
                    view?.showAlreadyCallingAlert(bottomNavigation)
                    return
                }

                clear()

                viewState = ViewState.AudioDialog.IDLE
            }
            BottomNavigation.VIDEO -> {
                if (dialog.isInitiator) {
                    view?.showAlreadyCallingAlert(bottomNavigation)
                    return
                }

                clear()

                viewState = ViewState.VideoDialog.IDLE
            }
            BottomNavigation.CONTACTS -> {
                if (dialog.isInitiator) {
                    view?.showAlreadyCallingAlert(bottomNavigation)
                    return
                }

                clear()

                viewState = ViewState.Contacts
            }
            BottomNavigation.INFO -> {
                if (dialog.isInitiator) {
                    view?.showAlreadyCallingAlert(bottomNavigation)
                    return
                }

                clear()

                viewState = ViewState.Info
            }
        }
    }

    fun onRateButtonClicked(ratingButton: RatingButton) {
        socketClient?.sendFeedback(ratingButton.rating, ratingButton.chatId)

        when (viewState) {
            is ViewState.TextDialog -> {
                viewState = ViewState.TextDialog.UserFeedback(true)
                viewState = ViewState.ChatBot.UserPrompt(false)
            }
            is ViewState.AudioDialog ->
                viewState = ViewState.AudioDialog.UserFeedback(true)
            is ViewState.VideoDialog ->
                viewState = ViewState.VideoDialog.UserFeedback(true)
        }
    }

    private fun cancelPendingCall() {
        socketClient?.cancelPendingCall()

        dialog.isInitiator = false
    }

    fun onCancelPendingCallClicked(bottomNavigation: BottomNavigation) {
        cancelPendingCall()

        viewState = when (bottomNavigation) {
            BottomNavigation.HOME -> {
                chatBot.clear()
                view?.clearChatMessages()
                view?.clearChatFooterMessages()

                socketClient?.getBasicCategories()

                ViewState.ChatBot.Categories(true)
            }
            BottomNavigation.AUDIO -> ViewState.AudioDialog.IDLE
            BottomNavigation.VIDEO -> ViewState.VideoDialog.IDLE
            BottomNavigation.CONTACTS -> ViewState.Contacts
            BottomNavigation.INFO -> ViewState.Info
        }
    }

    fun onCallOperatorClicked(operatorCall: OperatorCall) {
        if (operatorCall == OperatorCall.AUDIO) {
            if (configs?.isAudioCallEnabled == false) return

            view?.resolvePermissions(operatorCall)
        } else if (operatorCall == OperatorCall.VIDEO) {
            if (configs?.isVideoCallEnabled == false) return

            view?.resolvePermissions(operatorCall)
        }
    }

    fun onCallOperator(operatorCall: OperatorCall) {
        debug(TAG, "onCallOperator() -> viewState: $viewState")

        if (dialog.isInitiator) {
            view?.showAlreadyCallingAlert(operatorCall)
            return
        }

        dialog.isInitiator = true

        view?.clearChatMessages()

        viewState = when (operatorCall) {
            OperatorCall.TEXT -> ViewState.TextDialog.Pending
            OperatorCall.AUDIO -> ViewState.AudioDialog.Pending
            OperatorCall.VIDEO -> ViewState.VideoDialog.Pending
        }

        socketClient?.callOperator(operatorCall)
    }

    fun onSendMessageButtonClicked(message: String) {
        debug(TAG, "onSendMessageButtonClicked -> viewState: $viewState")

        if (message.isNotBlank()) {
            if (viewState is ViewState.ChatBot) {
                viewState = ViewState.ChatBot.UserPrompt(true)
            }

            sendUserMessage(message, true)
        }
    }

    private fun sendUserMessage(message: String, isInputClearText: Boolean = true) {
        val cleanMessage = message.trim()

        socketClient?.sendUserMessage(cleanMessage)

        if (isInputClearText) {
            view?.clearMessageInputViewText()
        }

        view?.addNewMessage(Message(type = Message.Type.USER, text = cleanMessage))
    }

    fun onGoToActiveDialogButtonClicked() {
        if (viewState is ViewState.AudioDialog) {
            viewState = ViewState.AudioDialog.Live(true)
        } else if (viewState is ViewState.VideoDialog) {
            viewState = ViewState.VideoDialog.Live(true)
        }
    }

    fun onGoToChatButtonClicked(operatorCall: OperatorCall) {
        if (operatorCall == OperatorCall.AUDIO) {
            viewState = ViewState.AudioDialog.Live(false)
        } else if (operatorCall == OperatorCall.VIDEO) {
            viewState = ViewState.VideoDialog.Live(false)
        }
    }

    fun onLanguageSelected(language: String) {
        socketClient?.setLanguage(language)
        socketClient?.sendUserLanguage(language)
    }

    fun onShowAllCategoryChildClicked(category: Category) {
        chatBot.activeCategory = category

        view?.setNewMessages(
            Message(
                type = Message.Type.CROSS_CHILDREN,
                category = chatBot.activeCategory
            )
        )

        view?.showGoToHomeButton()
    }

    fun onCategoryChildClicked(category: Category) {
        chatBot.activeCategory = category

        if (category.responses.isNotEmpty()) {
            socketClient?.getResponse(category.responses.first())
        } else {
            socketClient?.getCategories(category.id)
        }

        viewState = ViewState.ChatBot.Categories(true)
    }

    fun onGoBackClicked(category: Category) {
        val categories = chatBot.allCategories.filter { it.id == category.parentId }

        val messages = if (categories.all { it.parentId == null }) {
            view?.clearChatFooterMessages()

            chatBot.basicCategories.map {
                Message(type = Message.Type.CATEGORY, category = it)
            }
        } else {
            categories.map { Message(type = Message.Type.CROSS_CHILDREN, category = it) }
        }

        chatBot.activeCategory = null

        view?.setNewMessages(messages)

        chatListViewState?.let { view?.restoreChatListViewState(it) }
    }

    fun onUrlInTextClicked(url: String) {
        debug(TAG, "onUrlInTextClicked -> viewState: $viewState")

        if (url.startsWith("#")) {
            if (viewState is ViewState.ChatBot) {
                viewState = ViewState.ChatBot.UserPrompt(true)
            }

            val text = url.removePrefix("#")
            sendUserMessage(text, false)

            view?.showGoToHomeButton()
        } else {
            view?.showOpenLinkConfirmAlert(url)
        }

    }

    fun onGoToHomeClicked() {
        when (viewState) {
            is ViewState.AudioDialog -> {
                view?.clearChatMessages()
                view?.clearChatFooterMessages()

                viewState = ViewState.AudioDialog.IDLE
            }
            is ViewState.VideoDialog -> {
                view?.clearChatMessages()
                view?.clearChatFooterMessages()

                viewState = ViewState.VideoDialog.IDLE
            }
            else -> {
                val messages = chatBot.basicCategories.map { category ->
                    Message(type = Message.Type.CATEGORY, category = category)
                }

                view?.clearChatFooterMessages()

                viewState = if (messages.isEmpty()) {
                    socketClient?.getBasicCategories()

                    ViewState.ChatBot.Categories(true)
                } else {
                    view?.setNewMessages(messages)

                    chatListViewState?.let { view?.restoreChatListViewState(it) }

                    ViewState.ChatBot.Categories(false)
                }
            }
        }
    }

    fun onCallCancelClicked(operatorCall: OperatorCall) {
        cancelPendingCall()

        viewState = when (operatorCall) {
            OperatorCall.TEXT -> ViewState.ChatBot.UserPrompt(false)
            OperatorCall.AUDIO -> ViewState.AudioDialog.IDLE
            OperatorCall.VIDEO -> ViewState.VideoDialog.IDLE
        }
    }

    fun onFormCancelClicked() {
        debug(TAG, "onCancelClicked -> viewState: $viewState")

        viewState = ViewState.ChatBot.UserPrompt(false)
    }

    fun onRegisterAppealClicked() {
        view?.showGoToHomeButton()

        viewState = ViewState.Form
    }

    fun onUploadFile(filePath: String) {
        val file = File(filePath)
        val type = file.getFileType() ?: return

        val params = RequestParams().apply {
            put("type", type)
            put("file", file)
        }

        httpClient.uploadFile(UrlUtil.buildUrl("/upload") ?: return, params) { path, hash ->
            debug(TAG, "uploadFile: $path, $hash")

            socketClient?.sendUserMediaMessage(type, path)

            val fullUrl = UrlUtil.buildUrl(path)

            val media = if (type == "image") {
                Media(
                    imageUrl = fullUrl,
                    hash = hash,
                    ext = hash.split(".").last(),
                    local = file
                )
            } else {
                Media(
                    fileUrl = fullUrl,
                    hash = hash,
                    ext = hash.split(".").last(),
                    local = file
                )
            }

            view?.addNewMessage(Message(type = Message.Type.USER, media = media))
        }
    }

    fun onLocalDescription(sessionDescription: SessionDescription) {
        socketClient?.sendMessage(
            rtc = rtc {
                this.type = RTC.Type.to(sessionDescription.type)
                this.sdp = sessionDescription.description
            }
        )
    }

    fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
        when (iceConnectionState) {
            PeerConnection.IceConnectionState.CONNECTED -> {
//                peerConnectionClient?.setVideoMaxBitrate(500)
                if (viewState is ViewState.AudioDialog) {
                    viewState = ViewState.AudioDialog.Live(true)
                } else if (viewState is ViewState.VideoDialog) {
                    viewState = ViewState.VideoDialog.Live(true)
                }
            }
            PeerConnection.IceConnectionState.COMPLETED -> {
                if (viewState is ViewState.AudioDialog) {
                    if (viewState is ViewState.AudioDialog.Live && (viewState as ViewState.AudioDialog.Live).isDialogScreenShown) {
                        debug(TAG, "Already is on Live Mode")
                    } else {
                        viewState = ViewState.AudioDialog.Live(true)
                    }
                } else if (viewState is ViewState.VideoDialog) {
                    if (viewState is ViewState.VideoDialog.Live && (viewState as ViewState.VideoDialog.Live).isDialogScreenShown) {
                        debug(TAG, "Already is on Live Mode")
                    } else {
                        viewState = ViewState.VideoDialog.Live(true)
                    }
                }
            }
            PeerConnection.IceConnectionState.DISCONNECTED ->
                onCloseLiveCall()
            else -> {
            }
        }
    }

    fun onCloseLiveCall() {
        debug(TAG, "closeLiveCall -> viewState: $viewState")

        dialog.clear()

        view?.releaseMediaPlayer()

        view?.releaseVideoDialog()

        view?.releasePeerConnection()

        when (viewState) {
            is ViewState.TextDialog -> {
                if (viewState !is ViewState.TextDialog.UserFeedback) {
                    viewState = ViewState.TextDialog.CallAgentDisconnected
                }
            }
            is ViewState.AudioDialog -> {
                if (viewState !is ViewState.AudioDialog.UserFeedback) {
                    viewState = ViewState.AudioDialog.CallAgentDisconnected
                }
            }
            is ViewState.VideoDialog -> {
                if (viewState !is ViewState.VideoDialog.UserFeedback) {
                    viewState = ViewState.VideoDialog.CallAgentDisconnected
                }
            }
            else -> {
                viewState = ViewState.ChatBot.UserPrompt(false)
            }
        }
    }

    fun onHangupLiveCall() {
        dialog.clear()

        view?.releaseMediaPlayer()

        view?.showUserDisconnectedMessage()

        view?.releasePeerConnection()

        socketClient?.sendMessage(action = UserMessage.Action.FINISH)

        viewState = when (viewState) {
            is ViewState.TextDialog -> ViewState.TextDialog.UserDisconnected
            is ViewState.AudioDialog -> ViewState.AudioDialog.UserDisconnected
            is ViewState.VideoDialog -> {
                view?.releaseVideoDialog()

                ViewState.VideoDialog.UserDisconnected
            }
            else -> ViewState.ChatBot.UserPrompt(false)
        }
    }

    fun onIceCandidate(iceCandidate: IceCandidate) {
        socketClient?.sendMessage(
            rtc = rtc {
                type = RTC.Type.CANDIDATE
                id = iceCandidate.sdpMid
                label = iceCandidate.sdpMLineIndex
                candidate = iceCandidate.sdp
            }
        )
    }

    fun onFormSendClicked(name: String, email: String, phone: String) {
        debug(TAG, "onSendClicked -> viewState: $viewState")

        socketClient?.sendFuzzyTaskConfirmation(name, email, phone)

        view?.showFormSentSuccessAlert()
    }

    fun onAppealRegistered() {
        viewState = ViewState.ChatBot.UserPrompt(false)
    }

    fun onNewChatMessagesInserted() {
        if (viewState is ViewState.ChatBot.Categories) return

        view?.scrollToBottom()
    }

    fun onHangupButtonClicked() {
        view?.showHangupConfirmationAlert()
    }

}