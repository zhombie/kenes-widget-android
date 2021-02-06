package q19.kenes_widget.ui.presentation

import android.os.Parcelable
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.RequestParams
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import q19.kenes_widget.core.device.DeviceInfo
import q19.kenes_widget.data.model.*
import q19.kenes_widget.data.network.file.DownloadResult
import q19.kenes_widget.data.network.file.downloadFile
import q19.kenes_widget.data.network.file.uploadFile
import q19.kenes_widget.data.network.http.IceServersTask
import q19.kenes_widget.data.network.http.WidgetConfigsTask
import q19.kenes_widget.data.network.socket.CallInitialization
import q19.kenes_widget.data.network.socket.SocketClient
import q19.kenes_widget.ui.presentation.model.BottomNavigation
import q19.kenes_widget.ui.presentation.model.ChatBot
import q19.kenes_widget.ui.presentation.model.Dialog
import q19.kenes_widget.ui.presentation.model.ViewState
import q19.kenes_widget.util.FileUtil.getFileType
import q19.kenes_widget.util.Logger.debug
import q19.kenes_widget.util.UrlUtil
import java.io.File

class KenesWidgetV2Presenter constructor(
//    private val appProvider: AppProvider,
    private val deviceInfo: DeviceInfo,
    private val language: Language,
    private val palette: IntArray
) {

    companion object {
        private val TAG = KenesWidgetV2Presenter::class.java.simpleName
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

            if (value is ViewState.ChatBot || value is ViewState.Services || value is ViewState.Info || value is ViewState.TextDialog.IDLE || value is ViewState.OperatorCall) {
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

    private var activeCallScope: Configs.CallScope? = null
    private var activeService: Service? = null

    private var activeServiceSession: Service? = null

    private var activeDynamicForm: DynamicForm? = null

    private var iceServers = listOf<PeerConnection.IceServer>()

    private var chatListViewState: Parcelable? = null

    fun setChatListViewState(chatListViewState: Parcelable?) {
        this.chatListViewState = chatListViewState
    }

    private fun onFirstViewAttach() {
        listOf(
            BottomNavigation.HOME,
            BottomNavigation.OPERATOR_CALL,
            BottomNavigation.SERVICES
        ).forEach {
            view?.hideNavButton(it)
        }
        view?.showNavButton(BottomNavigation.INFO)
//        viewState = ViewState.Info

        view?.showOpponentInfo(Configs.Opponent.DEFAULT)

        view?.showCurrentLanguage(language)

        view?.hideHangupButton()

        chatBot.callback = object : ChatBot.Callback {
            override fun onBasicCategoriesLoaded(categories: List<Category>) {
//                debug(TAG, "onBasicCategoriesLoaded() -> categories: $categories")

                val messages = categories
//                    .sortedBy { it.config?.order }
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

    fun onResume() {
//        if (configs?.isChabotEnabled == true && !dialog.isActive && viewState is ViewState.ChatBot) {
//            socketClient?.getBasicCategories()
//        }
    }

    private fun initSocket() {
        val socketUrl = UrlUtil.getSocketUrl(UrlUtil.getHostname())
        if (socketUrl.isNullOrBlank()) {
            throw NullPointerException("Signalling server url is null. Please, provide a valid url.")
        } else {
            debug(TAG, "initSocket() -> socketUrl: $socketUrl")
            socketClient = SocketClient()
            socketClient?.start(socketUrl, language.key)
        }

        socketClient?.listener = object : SocketClient.Listener {
            override fun onConnect() {
                socketClient?.setLanguage(language.key)
                socketClient?.sendUserLanguage(language.key)

                viewState = if (configs?.booleans?.isChabotEnabled == true) {
                    socketClient?.getBasicCategories()
                    ViewState.ChatBot.Categories(true)
                } else {
                    when {
                        configs?.booleans?.isAudioCallEnabled == true || configs?.booleans?.isVideoCallEnabled == true ->
                            ViewState.OperatorCall
                        configs?.booleans?.isServicesEnabled == true ->
                            ViewState.Services.IDLE
                        else ->
                            ViewState.Info
                    }
                }
            }

            override fun onOperatorGreet(fullName: String, photoUrl: String?, text: String) {
                debug(TAG, "onCallAgentGreet -> viewState: $viewState")

                if (viewState is ViewState.TextDialog) {
                    dialog.isActive = true
                    viewState = ViewState.TextDialog.Live
                }

                val newText = text.replace("{}", fullName)

                view?.showOpponentInfo(fullName, photoUrl)

                if (viewState is ViewState.AudioDialog) {
                    view?.showAudioCallerInformation(fullName, photoUrl)
                }

                view?.addNewMessage(Message(type = Message.Type.INCOMING, text = newText))
            }

            override fun onFormInit(dynamicForm: DynamicForm) {
                activeDynamicForm = dynamicForm

                view?.showDynamicForm(dynamicForm)

                viewState = ViewState.DynamicForm
            }

            override fun onFormFinal(text: String) {
                view?.addNewMessage(Message(type = Message.Type.INCOMING, text = text))

                activeDynamicForm = null

                view?.clearDynamicForm()

                view?.showGoToHomeButton()

                debug(TAG, "onFormFinal() -> viewState: $viewState")

                viewState = if (viewState is ViewState.Services) {
                    ViewState.Services.Completed
                } else {
                    ViewState.ChatBot.Categories(isLoading = false)
                }
            }

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
                if (viewState is ViewState.AudioDialog || viewState is ViewState.VideoDialog) {
                    if (!text.isNullOrBlank()) {
                        view?.setOperatorCallInfoText(text)
                    }
                    if (count > 1) {
                        view?.setOperatorCallPendingQueueCount(count)
                    }
                }
            }

            override fun onNoResultsFound(text: String, timestamp: Long): Boolean {
                debug(TAG, "onNoResultsFound -> viewState: $viewState")

                view?.addNewMessage(
                    Message(
                        type = Message.Type.INCOMING,
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
                        type = Message.Type.INCOMING,
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

                view?.addNewMessage(Message(type = Message.Type.INCOMING, text = text))

                view?.showNoOnlineCallAgentsAlert(text)

                when (viewState) {
                    is ViewState.TextDialog -> {
                        view?.showGoToHomeButton()
                        viewState = ViewState.ChatBot.UserPrompt(false)
                    }
                    is ViewState.AudioDialog, is ViewState.VideoDialog ->
                        viewState = ViewState.OperatorCall
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

            override fun onUserRedirected(text: String, timestamp: Long): Boolean {
                debug(TAG, "onUserRedirected() -> viewState: $viewState")

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
                    else ->
                        viewState = ViewState.ChatBot.UserPrompt(false)
                }

                view?.addNewMessage(
                    Message(
                        type = Message.Type.NOTIFICATION,
                        text = text,
                        timestamp = timestamp
                    )
                )

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
                    dialog.isActive = true

                    viewState = ViewState.AudioDialog.Start

                    view?.createPeerConnection(
                        isMicrophoneEnabled = true,
                        isCameraEnabled = false,
                        iceServers = iceServers
                    )

                    socketClient?.sendMessage(rtc = rtc { type = RTC.Type.PREPARE })
                } else if (viewState is ViewState.VideoDialog) {
                    dialog.isActive = true

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
                text: String?,
                replyMarkup: Message.ReplyMarkup?,
                attachments: List<Attachment>?,
                dynamicForm: DynamicForm?,
                timestamp: Long
            ) {
                debug(TAG, "onTextMessage -> viewState: $viewState")

                if (chatBot.activeCategory != null) {
                    view?.setNewMessages(
                        Message(
                            type = Message.Type.RESPONSE,
                            text = if (text.isNullOrBlank()) {
                                Configs.I18NString.NOT_FOUND.get(language)
                            } else {
                                text
                            },
                            replyMarkup = replyMarkup,
                            attachments = attachments,
                            timestamp = timestamp,
                            category = chatBot.activeCategory
                        )
                    )
                    view?.scrollToTop()
//                    view?.showGoToHomeButton()

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
                            CallType.AUDIO,
                            "${dialog.unreadMessages}+"
                        )
                    } else {
                        view?.setUnreadMessagesCountOnCall(
                            CallType.AUDIO,
                            "${dialog.unreadMessages}"
                        )
                    }
                } else if (viewState is ViewState.VideoDialog.Live && (viewState as ViewState.VideoDialog.Live).isDialogScreenShown) {
                    dialog.unreadMessages += 1
                    if (dialog.unreadMessages >= Dialog.MAX_UNREAD_MESSAGES_COUNT) {
                        view?.setUnreadMessagesCountOnCall(
                            CallType.VIDEO,
                            "${dialog.unreadMessages}+"
                        )
                    } else {
                        view?.setUnreadMessagesCountOnCall(
                            CallType.VIDEO,
                            "${dialog.unreadMessages}"
                        )
                    }
                }

                view?.addNewMessage(
                    Message(
                        type = Message.Type.INCOMING,
                        text = text,
                        replyMarkup = replyMarkup,
                        attachments = attachments,
                        timestamp = timestamp
                    )
                )

                if (dynamicForm != null) {
                    socketClient?.sendFormInit(formId = dynamicForm.id)
                }

                if (viewState is ViewState.ChatBot) {
//                    debug(TAG, "onTextMessage: chatFooterAdapter?.showGoToHomeButton()")

                    view?.showGoToHomeButton()

                    viewState = ViewState.ChatBot.UserPrompt(false)
                } else if (viewState is ViewState.Services) {
                    view?.showGoToHomeButton()
                }
            }

            override fun onMediaMessage(
                media: Media,
                replyMarkup: Message.ReplyMarkup?,
                timestamp: Long
            ) {
                if (media.isImage || media.isAudio || media.isFile) {
                    view?.addNewMessage(
                        Message(
                            type = Message.Type.INCOMING,
                            media = media,
                            replyMarkup = replyMarkup,
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
                debug(TAG, "onCategories() -> categories: $categories")

                if (viewState is ViewState.ChatBot.UserPrompt) return

//                val sortedCategories = categories.sortedBy { it.config?.order }
                chatBot.allCategories.addAll(categories)

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
                    if (chatBot.activeCategory?.children?.containsAll(categories) == false) {
                        chatBot.activeCategory?.children?.addAll(categories)
                    }
                    view?.setNewMessages(
                        Message(
                            type = Message.Type.CROSS_CHILDREN,
                            category = chatBot.activeCategory
                        )
                    )
                    view?.scrollToTop()
//                    view?.showGoToHomeButton()
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

        val data = task.execute()

        debug(TAG, "fetchWidgetConfigs() -> data: $data")

        if (data == null) {
            view?.showOpponentInfo(Configs.Opponent.DEFAULT)

            viewState = when {
                configs?.booleans?.isChabotEnabled == true ->
                    ViewState.ChatBot.Categories(false)
                configs?.booleans?.isAudioCallEnabled == true || configs?.booleans?.isVideoCallEnabled == true ->
                    ViewState.OperatorCall
                configs?.booleans?.isServicesEnabled == true ->
                    ViewState.Services.IDLE
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

                if (configs.booleans.isPhonesListShown) {
                    configs.phones?.let { phones ->
                        if (!phones.isNullOrEmpty()) {
                            view?.showPhones(phones)
                        }
                    }
                }

                configs.opponent?.let {
                    view?.showOpponentInfo(it)
                }

                if (configs.booleans.isChabotEnabled) {
                    view?.setDefaultFooterView()
                    view?.showNavButton(BottomNavigation.HOME)
                } else {
                    view?.hideNavButton(BottomNavigation.HOME)
                }

                if (configs.booleans.isAudioCallEnabled || configs.booleans.isVideoCallEnabled) {
                    if (configs.booleans.isOperatorsScoped) {
                        if (!configs.callScopes.isNullOrEmpty()) {
                            val parentScopes = Configs.CallScope.getMediaCallScopes(configs.callScopes)
                            if (parentScopes.isNullOrEmpty()) {
                                view?.showCallScopes(callScopes = listOf(Configs.CallScope.EMPTY))
                            } else {
                                view?.showCallScopes(callScopes = parentScopes)
                            }

                            view?.showNavButton(BottomNavigation.OPERATOR_CALL)
                        }
                    } else {
                        if (configs.booleans.isAudioCallEnabled) {
                            view?.showOperatorCallButton(CallType.AUDIO)
                        } else {
                            view?.hideOperatorCallButton(CallType.AUDIO)
                        }
                        if (configs.booleans.isVideoCallEnabled) {
                            view?.showOperatorCallButton(CallType.VIDEO)
                        } else {
                            view?.hideOperatorCallButton(CallType.VIDEO)
                        }

                        view?.showNavButton(BottomNavigation.OPERATOR_CALL)
                    }
                } else {
                    view?.hideNavButton(BottomNavigation.OPERATOR_CALL)
                }

                if (configs.booleans.isServicesEnabled) {
                    val parentServices = Configs.CallScope.getExternalServices(configs.callScopes)
                    debug(TAG, "parentServices: $parentServices")
                    if (parentServices.isNullOrEmpty()) {
                        view?.showExternalServices(services = listOf(Configs.CallScope.EMPTY))
                    } else {
                        view?.showExternalServices(services = parentServices)
                    }
                    view?.showNavButton(BottomNavigation.SERVICES)
                } else {
                    view?.hideNavButton(BottomNavigation.SERVICES)
                }
            }
        }
    }

    private fun fetchIceServers() {
        val url = UrlUtil.buildUrl("/ice_servers") ?: return
        debug(TAG, "fetchIceServers -> url: $url")

        val task = IceServersTask(url)

        val data = task.execute()

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

    fun onAttachmentClicked(file: File) {
        view?.openFile(file)
    }

    fun onBottomNavigationButtonClicked(bottomNavigation: BottomNavigation) {
        fun clear() {
            chatBot.clear()

            view?.clearChatMessages()
            view?.clearChatFooterMessages()

            if (activeServiceSession != null) {
                socketClient?.sendCancel()
                activeServiceSession = null
            }
        }

        when (bottomNavigation) {
            BottomNavigation.HOME -> {
                if (configs?.booleans?.isChabotEnabled == false) return

                if (dialog.isInitiator) {
                    view?.showAlreadyCallingAlert(bottomNavigation)
                    return
                }

                clear()

                socketClient?.getBasicCategories()
                viewState = ViewState.ChatBot.Categories(true)
            }
            BottomNavigation.OPERATOR_CALL -> {
                if (dialog.isInitiator) {
                    view?.showAlreadyCallingAlert(bottomNavigation)
                    return
                }

                clear()

                val parentScopes = Configs.CallScope.getMediaCallScopes(configs?.callScopes)
                if (parentScopes.isNullOrEmpty()) {
                    view?.showCallScopes(callScopes = listOf(Configs.CallScope.EMPTY))
                } else {
                    view?.showCallScopes(callScopes = parentScopes)
                }

                viewState = ViewState.OperatorCall
            }
            BottomNavigation.SERVICES -> {
                if (dialog.isInitiator) {
                    view?.showAlreadyCallingAlert(bottomNavigation)
                    return
                }

                viewState = if (activeServiceSession != null) {
                    ViewState.Services.Process
                } else {
                    clear()

                    ViewState.Services.IDLE
                }
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
            BottomNavigation.OPERATOR_CALL -> ViewState.OperatorCall
            BottomNavigation.SERVICES -> ViewState.Services.IDLE
            BottomNavigation.INFO -> ViewState.Info
        }
    }

    fun onCallOperatorClicked(callType: CallType) {
        tryToCall(callType)
    }

    private fun tryToCall(callType: CallType, scope: String? = null) {
        if (callType == CallType.AUDIO) {
            if (configs?.booleans?.isAudioCallEnabled == false) return

            view?.resolvePermissions(callType, scope)
        } else if (callType == CallType.VIDEO) {
            if (configs?.booleans?.isVideoCallEnabled == false) return

            view?.resolvePermissions(callType, scope)
        }
    }

    fun onCallOperator(callType: CallType, scope: String? = null) {
        debug(TAG, "onCallOperator() -> viewState: $viewState")

        if (dialog.isInitiator) {
            view?.showAlreadyCallingAlert(callType)
            return
        }

        dialog.isInitiator = true

//        view?.clearChatMessages()

        viewState = when (callType) {
            CallType.TEXT -> ViewState.TextDialog.Pending
            CallType.AUDIO -> ViewState.AudioDialog.Pending
            CallType.VIDEO -> ViewState.VideoDialog.Pending
        }

        val callInitialization = CallInitialization(
            callType = callType,
            domain = UrlUtil.getHostname()?.removePrefix("https://"),
            topic = scope,
            device = CallInitialization.Device(
                os = deviceInfo.os,
                osVersion = deviceInfo.osVersion,
                appVersion = deviceInfo.versionName,
                name = deviceInfo.deviceName,
                mobileOperator = deviceInfo.operator,
                battery = CallInitialization.Device.Battery(
                    percentage = deviceInfo.batteryPercent,
                    isCharging = deviceInfo.isPhoneCharging,
                    temperature = deviceInfo.batteryTemperature
                )
            ),
            language = language
        )

        socketClient?.sendCallInitialization(callInitialization)
    }

    fun onCallScopeClicked(callScope: Configs.CallScope) {
        debug(TAG, "onCallScopeClicked() -> callScope: $callScope")

        if (callScope.type == Configs.CallScope.Type.FOLDER) {
            val callScopes = Configs.CallScope.getCallScopes(
                configs?.callScopes,
                callScope.id
            )
            debug(TAG, "onCallScopeClicked() -> configs.callScopes: ${configs?.callScopes}")
            debug(TAG, "onCallScopeClicked() -> callScopes: $callScopes")
            if (callScopes.isNullOrEmpty()) {
                activeCallScope = callScope

                view?.showCallScopes(
                    parentCallScope = callScope,
                    callScopes = listOf(Configs.CallScope.EMPTY)
                )
            } else {
                activeCallScope = callScope

                view?.showCallScopes(parentCallScope = callScope, callScopes = callScopes)
            }
        } else if (callScope.type == Configs.CallScope.Type.LINK) {
            if (callScope.action == Configs.CallScope.Action.AUDIO_CALL) {
                tryToCall(CallType.AUDIO, scope = callScope.scope)
            } else if (callScope.action == Configs.CallScope.Action.VIDEO_CALL) {
                tryToCall(CallType.VIDEO, scope = callScope.scope)
            }
        }
    }

    fun onCallScopeBackClicked() {
        debug(TAG, "onCallScopeBackClicked() -> activeCallScope: $activeCallScope")

        activeCallScope?.let { callScope ->
            activeCallScope = configs?.callScopes?.find {
                it.id == callScope.parentId
            }
            val callScopes = Configs.CallScope.getCallScopes(
                configs?.callScopes,
                activeCallScope?.id
            )
            if (callScopes.isNullOrEmpty()) {
                val parentScopes = Configs.CallScope.getMediaCallScopes(configs?.callScopes)
                if (parentScopes.isNullOrEmpty()) {
                    view?.showCallScopes(callScopes = listOf(Configs.CallScope.EMPTY))
                } else {
                    view?.showCallScopes(callScopes = parentScopes)
                }
            } else {
                view?.showCallScopes(parentCallScope = activeCallScope, callScopes = callScopes)
            }
        }
    }

    fun onServiceClicked(service: Service) {
        debug(TAG, "onServiceClicked() -> service: $service")

        if (service.type == Configs.CallScope.Type.FOLDER) {
            val services = Configs.CallScope.getCallScopes(
                configs?.callScopes,
                service.id
            )
            debug(TAG, "onCallScopeClicked() -> configs.callScopes: ${configs?.callScopes}")
            debug(TAG, "onCallScopeClicked() -> services: $services")
            if (services.isNullOrEmpty()) {
                activeService = service

                view?.showExternalServices(
                    parentService = service,
                    services = listOf(Configs.CallScope.EMPTY)
                )
            } else {
                activeService = service

                view?.showExternalServices(parentService = service, services = services)
            }
        } else if (service.type == Configs.CallScope.Type.LINK) {
            activeServiceSession = service

            val text = service.title.get(language)

            socketClient?.sendUserMessage(text)

            view?.addNewMessage(Message(type = Message.Type.OUTGOING, text = text))

            viewState = ViewState.Services.Process
        }
    }

    fun onServiceBackClicked() {
        debug(TAG, "onServiceBackClicked() -> activeService: $activeService")

        activeService?.let { callScope ->
            activeService = configs?.callScopes?.find {
                it.id == callScope.parentId
            }
            val callScopes = Configs.CallScope.getCallScopes(
                configs?.callScopes,
                activeService?.id
            )
            if (callScopes.isNullOrEmpty()) {
                val parentServices = Configs.CallScope.getExternalServices(configs?.callScopes)
                if (parentServices.isNullOrEmpty()) {
                    view?.showExternalServices(services = listOf(Configs.CallScope.EMPTY))
                } else {
                    view?.showExternalServices(services = parentServices)
                }
            } else {
                view?.showExternalServices(parentService = activeService, services = callScopes)
            }
        }
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

        view?.addNewMessage(Message(type = Message.Type.OUTGOING, text = cleanMessage))
    }

    fun onGoToActiveDialogButtonClicked() {
        if (viewState is ViewState.AudioDialog) {
            viewState = ViewState.AudioDialog.Live(true)
        } else if (viewState is ViewState.VideoDialog) {
            viewState = ViewState.VideoDialog.Live(true)
        }
    }

    fun onGoToChatButtonClicked(callType: CallType) {
        if (callType == CallType.AUDIO) {
            viewState = ViewState.AudioDialog.Live(false)
        } else if (callType == CallType.VIDEO) {
            viewState = ViewState.VideoDialog.Live(false)
        }
    }

    fun onLanguageSelected(language: String) {
        socketClient?.setLanguage(language)
        socketClient?.sendUserLanguage(language)
    }

//    fun onShowAllCategoryChildClicked(category: Category) {
//        chatBot.activeCategory = category
//
//        view?.setNewMessages(
//            Message(
//                type = Message.Type.CROSS_CHILDREN,
//                category = chatBot.activeCategory
//            )
//        )
//
//        view?.showGoToHomeButton()
//    }

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

    fun onGoToHomeButtonClicked() {
        debug(TAG, "onGoToHomeClicked() -> viewState: $viewState")

        when (viewState) {
            is ViewState.AudioDialog, is ViewState.VideoDialog -> {
                view?.clearChatMessages()
                view?.clearChatFooterMessages()

                viewState = ViewState.OperatorCall
            }
            is ViewState.Services -> {
                if (activeService != null) {
                    socketClient?.sendCancel()
                }

                if (activeServiceSession != null) {
                    socketClient?.sendCancel()
                    activeServiceSession = null
                }

                view?.clearChatMessages()
                view?.clearChatFooterMessages()

                viewState = ViewState.Services.IDLE
            }
            is ViewState.DynamicForm -> {
                activeDynamicForm = null

                view?.clearDynamicForm()

                view?.clearChatFooterMessages()

                if (activeServiceSession != null) {
                    activeServiceSession = null

                    view?.clearChatMessages()

                    viewState = ViewState.Services.IDLE
                } else {
                    viewState = ViewState.ChatBot.Categories(false)
                }
            }
            else -> {
                if (activeServiceSession != null) {
                    activeServiceSession = null

                    view?.clearDynamicForm()

                    view?.clearChatMessages()
                    view?.clearChatFooterMessages()

                    viewState = ViewState.Services.IDLE
                } else {
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
    }

    fun onCallCancelClicked(callType: CallType? = null) {
        cancelPendingCall()

        viewState = if (callType == null) {
            ViewState.OperatorCall
        } else {
            when (callType) {
                CallType.TEXT -> ViewState.ChatBot.UserPrompt(false)
                CallType.AUDIO, CallType.VIDEO -> ViewState.OperatorCall
            }
        }
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

            val fullUrl = UrlUtil.buildUrl(path)

            if (activeDynamicForm != null) {
                view?.showAttachmentThumbnail(
                    Attachment(
                        title = hash,
                        ext = hash.split(".").last(),
                        type = type,
                        url = fullUrl
                    )
                )
            } else {
                socketClient?.sendUserMediaMessage(type, path)

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

                view?.addNewMessage(Message(type = Message.Type.OUTGOING, media = media))
            }
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

        if (viewState is ViewState.TextDialog.Pending) {
            socketClient?.sendCancel()
        } else {
            socketClient?.sendMessage(action = UserMessage.Action.FINISH)
        }

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

    fun onFormSendButtonClicked(name: String, email: String, phone: String) {
        debug(TAG, "onSendClicked -> viewState: $viewState")

        socketClient?.sendFuzzyTaskConfirmation(name, email, phone)

        view?.showFormSentSuccessAlert()
    }

    fun onFormSendButtonClicked(dynamicForm: DynamicForm) {
        debug(TAG, "onSendClicked() -> dynamicForm: $dynamicForm")

        socketClient?.sendFormFinal(dynamicForm)

        activeDynamicForm = null

        view?.clearDynamicForm()

        debug(TAG, "onSendClicked() -> viewState: $viewState")

        viewState = if (activeServiceSession == null) {
            if (viewState is ViewState.Services) {
                ViewState.Services.Pending
            } else {
                ViewState.ChatBot.UserPrompt(false)
            }
        } else {
            ViewState.Services.Pending
        }
    }

    fun onFormCancelClicked() {
        debug(TAG, "onFormCancelClicked()-> viewState: $viewState")

        activeDynamicForm = null

        view?.clearDynamicForm()

        viewState = if (activeServiceSession == null) {
            if (viewState is ViewState.Services) {
                ViewState.Services.Cancelled
            } else {
                ViewState.ChatBot.UserPrompt(false)
            }
        } else {
            view?.showGoToHomeButton()
            ViewState.Services.Cancelled
        }
    }

    fun onRegisterAppealButtonClicked() {
        view?.showGoToHomeButton()

        viewState = ViewState.Form
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

    fun onAddAttachmentButtonClicked() {
        view?.showAttachmentPicker(forced = false)
    }

    fun onReplyMarkupButtonClicked(button: Message.ReplyMarkup.Button) {
        if (!button.url.isNullOrBlank()) {
            view?.openLink(button.url)
        } else {
            view?.addNewMessage(Message(type = Message.Type.OUTGOING, text = button.text))

            socketClient?.sendExternal(button.callbackData)
        }
    }

    fun onSelectAttachmentButtonClicked(field: DynamicFormField) {
        debug(TAG, "field: $field")
        view?.showAttachmentPicker(forced = true)
    }

}