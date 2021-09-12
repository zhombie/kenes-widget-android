package kz.q19.kenes.widget.ui.presentation.deprecated

import android.os.Parcelable
import com.loopj.android.http.AsyncHttpClient
import kz.q19.domain.model.call.CallType
import kz.q19.domain.model.configs.Configs
import kz.q19.domain.model.file.File
import kz.q19.domain.model.form.Form
import kz.q19.domain.model.keyboard.button.Button
import kz.q19.domain.model.keyboard.button.CallbackButton
import kz.q19.domain.model.keyboard.button.RateButton
import kz.q19.domain.model.keyboard.button.UrlButton
import kz.q19.domain.model.language.Language
import kz.q19.domain.model.media.Media
import kz.q19.domain.model.message.CallAction
import kz.q19.domain.model.message.Message
import kz.q19.domain.model.message.QRTCAction
import kz.q19.domain.model.webrtc.IceCandidate
import kz.q19.domain.model.webrtc.IceConnectionState
import kz.q19.domain.model.webrtc.IceServer
import kz.q19.domain.model.webrtc.SessionDescription
import kz.q19.kenes.widget.core.UrlManager
import kz.q19.kenes.widget.core.device.DeviceInfo
import kz.q19.kenes.widget.core.logging.Logger.debug
import kz.q19.kenes.widget.data.remote.file.DownloadResult
import kz.q19.kenes.widget.data.remote.file.downloadFile
import kz.q19.kenes.widget.ui.presentation.common.Screen
import kz.q19.kenes.widget.ui.presentation.home.ChatbotInteractor
import kz.q19.kenes.widget.ui.presentation.model.ViewState
import kz.q19.kenes.widget.ui.presentation.platform.BasePresenter
import kz.q19.socket.SocketClient
import kz.q19.socket.SocketClientConfig
import kz.q19.socket.listener.*
import kz.q19.socket.model.CallInitialization
import kz.q19.socket.model.Category
import kz.q19.webrtc.Options
import kz.q19.webrtc.PeerConnectionClient
import org.webrtc.MediaStream
import org.webrtc.PeerConnection

internal class OldKenesWidgetPresenter constructor(
    private val deviceInfo: DeviceInfo,
    private val language: Language,
    private val peerConnectionClient: PeerConnectionClient
) : BasePresenter<OldKenesWidgetView>(), ChatBotListener, SocketStateListener, WebRTCListener, CallListener, FormListener,
    PeerConnectionClient.Listener {

    companion object {
        private val TAG = OldKenesWidgetPresenter::class.java.simpleName
    }

    override fun onDestroy() {
        super.onDestroy()

        viewState = ViewState.ChatBot.Dashboard(false)

        chatListViewState = null

        configs = null

//        chatBot.clear()

//        dialog.clear()

        socketClient?.release()
        socketClient = null
    }

    private var viewState: ViewState = ViewState.ChatBot.Dashboard(false)
        set(value) {
            field = value

            if (value is ViewState.ChatBot || value is ViewState.Services || value is ViewState.Info || value is ViewState.TextDialog.IDLE || value is ViewState.CallAgentCall) {
                val callAgent = configs?.callAgent
                if (callAgent != null) {
                    getView().showPeerInfo(callAgent)
                }
            }

            if (value is ViewState.ChatBot.UserPrompt) {
//                chatBot.activeCategory = null
            }

//            if (value is ViewState.AudioDialog.Live || value is ViewState.VideoDialog.Live) {
//                dialog.unreadMessages = 0
//            }

            getView().setViewState(value)
        }

    private val httpClient by lazy { AsyncHttpClient() }

    private var socketClient: SocketClient? = null

    private var configs: Configs? = null
    private var chatBot = ChatbotInteractor()
//    private var dialog = Dialog()

    private var activeCall: Configs.Call? = null
    private var activeService: Configs.Service? = null

    private var activeServiceSession: Configs.Service? = null

    private var activeForm: Form? = null

    private var iceServers: List<IceServer> = listOf()

    private var chatListViewState: Parcelable? = null

    fun setChatListViewState(chatListViewState: Parcelable?) {
        this.chatListViewState = chatListViewState
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        listOf(
            Screen.HOME,
            Screen.CALLS,
            Screen.SERVICES
        ).forEach {
            getView().hideScreen(it)
        }
        getView().showScreen(Screen.INFO)
//        viewState = ViewState.Info

        getView().showDefaultPeerInfo()

        getView().showCurrentLanguage(language)

        getView().hideHangupButton()

//        chatBot.callback = ChatBot.Callback { categories ->
//            debug(TAG, "onBasicCategoriesLoaded() -> categories: $categories")

//            val messages = categories
//                .sortedBy { it.config?.order }
//                .map { category ->
//                    Message.Builder()
//                        .setType(Message.Type.CATEGORY)
//                        .setCategory(category)
//                        .build()
//                }

//            getView().clearChatFooterMessages()
//            getView().setNewMessages(messages)

//            viewState = ViewState.ChatBot.Dashboard(false)
//        }

        initSocket()
    }

    override fun onViewResume() {
//        if (configs?.isChabotEnabled == true && !dialog.isActive && viewState is ViewState.ChatBot) {
//            socketClient?.getBasicCategories()
//        }
    }

    private fun initSocket() {
        SocketClientConfig.init(true, language)
        socketClient = SocketClient.getInstance()
        socketClient?.create(UrlManager.getSocketUrl())
        socketClient?.connect()

        socketClient?.registerSocketConnectEventListener()
        socketClient?.registerMessageEventListener()
        socketClient?.registerChatBotDashboardEventListener()
        socketClient?.registerUsersQueueEventListener()
        socketClient?.registerCallAgentGreetEventListener()
        socketClient?.registerCallAgentTypingEventListener()
        socketClient?.registerUserCallFeedbackEventListener()
        socketClient?.registerFormInitializeEventListener()
        socketClient?.registerFormFinalizeEventListener()
        socketClient?.registerSocketDisconnectEventListener()

        socketClient?.setSocketStateListener(this)
        socketClient?.setChatBotListener(this)
        socketClient?.setWebRTCListener(this)
        socketClient?.setCallListener(this)
        socketClient?.setFormListener(this)
    }

    private fun fetchWidgetConfigs() {
        val url = UrlManager.buildUrl("/configs") ?: return
        debug(TAG, "fetchWidgetConfigs() -> url: $url")

        val data: Configs? = null
        debug(TAG, "fetchWidgetConfigs() -> data: $data")

        if (data == null) {
            getView().showDefaultPeerInfo()

            viewState = when {
                configs?.preferences?.isChatBotEnabled == true ->
                    ViewState.ChatBot.Dashboard(false)
                configs?.preferences?.isAudioCallEnabled == true || configs?.preferences?.isVideoCallEnabled == true ->
                    ViewState.CallAgentCall
                configs?.preferences?.isServicesEnabled == true ->
                    ViewState.Services.IDLE
                else ->
                    ViewState.Info
            }
        } else {
            configs = data.also { configs ->
                configs.contacts?.let { contacts ->
                    val socials = contacts.socials
                    if (!socials.isNullOrEmpty()) {
                        getView().showSocials(socials)
                    }

                    if (configs.preferences.isPhonesListShown) {
                        val phoneNumbers = contacts.phoneNumbers
                        if (!phoneNumbers.isNullOrEmpty()) {
                            getView().showPhoneNumbers(phoneNumbers)
                        }
                    }
                }

                getView().showPeerInfo(configs.callAgent)

                if (configs.preferences.isChatBotEnabled) {
                    getView().setDefaultFooterView()
                    getView().showScreen(Screen.HOME)
                } else {
                    getView().hideScreen(Screen.HOME)
                }

                if (configs.preferences.isAudioCallEnabled || configs.preferences.isVideoCallEnabled) {
                    if (configs.preferences.isCallAgentsScoped) {
                        if (!configs.calls.isNullOrEmpty()) {
                            val parentCalls = configs.calls?.filter { it.isParent() && it.isMediaCall() }
                            if (parentCalls.isNullOrEmpty()) {
                                getView().showCalls(calls = listOf())
                            } else {
                                getView().showCalls(calls = parentCalls)
                            }

                            getView().showScreen(Screen.CALLS)
                        }
                    } else {
                        if (configs.preferences.isAudioCallEnabled) {
                            getView().showOperatorCallButton(CallType.AUDIO)
                        } else {
                            getView().hideOperatorCallButton(CallType.AUDIO)
                        }
                        if (configs.preferences.isVideoCallEnabled) {
                            getView().showOperatorCallButton(CallType.VIDEO)
                        } else {
                            getView().hideOperatorCallButton(CallType.VIDEO)
                        }

                        getView().showScreen(Screen.CALLS)
                    }
                } else {
                    getView().hideScreen(Screen.CALLS)
                }

                if (configs.preferences.isServicesEnabled) {
                    val parentServices = configs.services?.filter { it.isParent() }
                    debug(TAG, "parentServices: $parentServices")
                    if (parentServices.isNullOrEmpty()) {
                        getView().showServices(services = listOf())
                    } else {
                        getView().showServices(services = parentServices)
                    }
                    getView().showScreen(Screen.SERVICES)
                } else {
                    getView().hideScreen(Screen.SERVICES)
                }
            }
        }
    }

    fun onMediaClicked(media: Media, file: File, itemPosition: Int) {
        if (file.get().exists()) {
            if (media.type == Media.Type.AUDIO) {
                getView().playAudio(file.absolutePath, itemPosition)
            } else if (media.type == Media.Type.FILE) {
                getView().openFile(file)
            }
        } else {
            try {
                if (media.type == Media.Type.AUDIO) {
                    file.downloadFile(media.urlPath, "media", itemPosition) {
                        getView().playAudio(file.absolutePath, itemPosition)
                    }
                } else if (media.type == Media.Type.FILE) {
                    file.downloadFile(media.urlPath, "media", itemPosition) {}
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
        httpClient.downloadFile(this.get(), url) { downloadResult ->
            when (downloadResult) {
                is DownloadResult.Success -> {
                    callback()
                    getView().showFileDownloadStatus(
                        File.DownloadStatus.COMPLETED,
                        itemPosition
                    )
                }
                is DownloadResult.Error ->
                    getView().showFileDownloadStatus(File.DownloadStatus.ERROR, itemPosition)
                is DownloadResult.Progress ->
                    getView().showFileDownloadProgress(downloadResult.progress, fileType, itemPosition)
            }
        }
    }

    fun onAttachmentClicked(attachment: Media, file: File, itemPosition: Int) {
        if (file.get().exists()) {
            getView().openFile(file)
        } else {
            try {
                file.downloadFile(attachment.urlPath, "attachment", itemPosition) {
                    getView().openFile(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onAttachmentClicked(file: File) {
        getView().openFile(file)
    }

    fun onBottomNavigationButtonClicked(screen: Screen) {
        fun clear() {
//            chatBot.clear()

            getView().clearChatMessages()
            getView().clearChatFooterMessages()

            if (activeServiceSession != null) {
                socketClient?.sendCancel()
                activeServiceSession = null
            }
        }

        when (screen) {
            Screen.HOME -> {
                if (configs?.preferences?.isChatBotEnabled == false) return

//                if (dialog.isInitiator) {
//                    getView().showAlreadyCallingAlert(navigationButton)
//                    return
//                }

                clear()

//                socketClient?.requestParentCategories()
                viewState = ViewState.ChatBot.Dashboard(true)
            }
            Screen.CALLS -> {
//                if (dialog.isInitiator) {
//                    getView().showAlreadyCallingAlert(navigationButton)
//                    return
//                }

                clear()

                val parentCalls = configs?.calls?.filter { it.isParent() && it.isMediaCall() }
                if (parentCalls.isNullOrEmpty()) {
                    getView().showCalls(calls = listOf())
                } else {
                    getView().showCalls(calls = parentCalls)
                }

                viewState = ViewState.CallAgentCall
            }
            Screen.SERVICES -> {
//                if (dialog.isInitiator) {
//                    getView().showAlreadyCallingAlert(navigationButton)
//                    return
//                }

                viewState = if (activeServiceSession != null) {
                    ViewState.Services.Process
                } else {
                    clear()

                    ViewState.Services.IDLE
                }
            }
            Screen.INFO -> {
//                if (dialog.isInitiator) {
//                    getView().showAlreadyCallingAlert(navigationButton)
//                    return
//                }

                clear()

                viewState = ViewState.Info
            }
        }
    }

    fun onRateButtonClicked(rateButton: RateButton) {
        socketClient?.sendUserCallFeedback(rateButton.rating, rateButton.chatId)

        when (viewState) {
            is ViewState.TextDialog -> {
                viewState = ViewState.TextDialog.UserFeedback(true)
                viewState = ViewState.ChatBot.UserPrompt(false)
            }
            is ViewState.AudioDialog ->
                viewState = ViewState.AudioDialog.UserFeedback(true)
            is ViewState.VideoDialog ->
                viewState = ViewState.VideoDialog.UserFeedback(true)
            else -> {
            }
        }
    }

    private fun cancelPendingCall() {
        socketClient?.sendPendingCallCancellation()

//        dialog.isInitiator = false
    }

    fun onCancelPendingCallClicked(screen: Screen) {
        cancelPendingCall()

        viewState = when (screen) {
            Screen.HOME -> {
//                chatBot.clear()
                getView().clearChatMessages()
                getView().clearChatFooterMessages()

//                socketClient?.requestParentCategories()

                ViewState.ChatBot.Dashboard(true)
            }
            Screen.CALLS -> ViewState.CallAgentCall
            Screen.SERVICES -> ViewState.Services.IDLE
            Screen.INFO -> ViewState.Info
        }
    }

    fun onCallTypeClicked(callType: CallType) {
        tryToCall(callType)
    }

    private fun tryToCall(callType: CallType, scope: String? = null) {
        if (callType == CallType.AUDIO) {
            if (configs?.preferences?.isAudioCallEnabled == false) return

            getView().resolvePermissions(callType, scope)
        } else if (callType == CallType.VIDEO) {
            if (configs?.preferences?.isVideoCallEnabled == false) return

            getView().resolvePermissions(callType, scope)
        }
    }

    fun onCallOperator(callType: CallType, scope: String? = null) {
        debug(TAG, "onCallOperator() -> viewState: $viewState")
//
//        if (dialog.isInitiator) {
//            getView().showAlreadyCallingAlert(callType)
//            return
//        }
//
//        dialog.isInitiator = true

//        getView().clearChatMessages()

        viewState = when (callType) {
            CallType.TEXT -> ViewState.TextDialog.Pending
            CallType.AUDIO -> ViewState.AudioDialog.Pending
            CallType.VIDEO -> ViewState.VideoDialog.Pending
        }

        val callInitialization = CallInitialization(
            callType = callType,
            topic = scope,
            language = language,
        )

        socketClient?.sendCallInitialization(callInitialization)
    }

    fun onCallClicked(call: Configs.Call) {
        debug(TAG, "onCallClicked() -> callScope: $call")

        if (call.type == Configs.Nestable.Type.FOLDER) {
            val calls = configs?.calls?.filter { it.isMediaCall() || it.parentId == call.id }
            debug(TAG, "onCallClicked() -> configs.calls: ${configs?.calls}")
            debug(TAG, "onCallClicked() -> calls: $calls")
            if (calls.isNullOrEmpty()) {
                activeCall = call

                getView().showCalls(parentCall = call, calls = listOf())
            } else {
                activeCall = call

                getView().showCalls(parentCall = call, calls = calls)
            }
        } else if (call.type == Configs.Nestable.Type.LINK) {
            if (call.callType == CallType.AUDIO) {
                tryToCall(CallType.AUDIO, scope = call.scope)
            } else if (call.callType == CallType.VIDEO) {
                tryToCall(CallType.VIDEO, scope = call.scope)
            }
        }
    }

    fun onCallBackClicked() {
        debug(TAG, "onCallBackClicked() -> activeCallScope: $activeCall")

        activeCall?.let { callScope ->
            activeCall = configs?.calls?.find { it.id == callScope.parentId }
            val calls = configs?.calls?.filter { it.isMediaCall() && it.parentId == activeCall?.id }
            if (calls.isNullOrEmpty()) {
                val parentCalls = configs?.calls?.filter { it.isParent() && it.isMediaCall() }
                if (parentCalls.isNullOrEmpty()) {
                    getView().showCalls(calls = listOf())
                } else {
                    getView().showCalls(calls = parentCalls)
                }
            } else {
                getView().showCalls(parentCall = activeCall, calls = calls)
            }
        }
    }

    fun onServiceClicked(service: Configs.Service) {
        debug(TAG, "onServiceClicked() -> service: $service")

        if (service.type == Configs.Nestable.Type.FOLDER) {
            val services = configs?.services?.filter { it.parentId == service.id }
            debug(TAG, "onCallScopeClicked() -> configs.services: ${configs?.services}")
            debug(TAG, "onCallScopeClicked() -> services: $services")
            if (services.isNullOrEmpty()) {
                activeService = service

                getView().showServices(
                    parentService = service,
                    services = listOf()
                )
            } else {
                activeService = service

                getView().showServices(parentService = service, services = services)
            }
        } else if (service.type == Configs.Nestable.Type.LINK) {
            activeServiceSession = service

            val text = service.title.get(language)

            socketClient?.sendUserMessage(text ?: "")

            getView().addNewMessage(
                Message.Builder()
                    .setType(Message.Type.OUTGOING)
                    .setText(text)
                    .build()
            )

            viewState = ViewState.Services.Process
        }
    }

    fun onServiceBackClicked() {
        debug(TAG, "onServiceBackClicked() -> activeService: $activeService")

        activeService?.let { callScope ->
            activeService = configs?.services?.find { it.id == callScope.parentId }
            val services = configs?.services?.filter { it.parentId == activeService?.id }
            if (services.isNullOrEmpty()) {
                val parentServices = configs?.services?.filter { it.isParent() }
                if (parentServices.isNullOrEmpty()) {
                    getView().showServices(services = listOf())
                } else {
                    getView().showServices(services = parentServices)
                }
            } else {
                getView().showServices(parentService = activeService, services = services)
            }
        }
    }

    fun onSendMessageButtonClicked(message: String?) {
        debug(TAG, "onSendMessageButtonClicked -> viewState: $viewState")

        if (!message.isNullOrBlank()) {
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
            getView().clearMessageInputViewText()
        }

        getView().addNewMessage(Message.Builder()
            .setType(Message.Type.OUTGOING)
            .setText(cleanMessage)
            .build()
        )
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

    fun onLanguageSelected(languageKey: String) {
        val language = Language.by(languageKey)
        SocketClientConfig.init(SocketClientConfig.isLoggingEnabled(), language)
        socketClient?.sendUserLanguage(language)
    }

//    fun onShowAllCategoryChildClicked(category: Category) {
//        chatBot.activeCategory = category
//
//        getView().setNewMessages(
//            Message(
//                type = Message.Type.CROSS_CHILDREN,
//                category = chatBot.activeCategory
//            )
//        )
//
//        getView().showGoToHomeButton()
//    }

    fun onCategoryChildClicked(category: Category) {
//        chatBot.activeCategory = category

        if (category.responses.isNotEmpty()) {
//            socketClient?.requestResponse(category.responses.first())
        } else {
//            socketClient?.requestCategories(category.id)
        }

        viewState = ViewState.ChatBot.Dashboard(true)
    }

    fun onGoBackClicked(category: Category) {
//        val categories = chatBot.allCategories.filter { it.id == category.parentId }

//        val messages = if (categories.all { it.parentId == Category.NO_PARENT_ID }) {
//            getView().clearChatFooterMessages()
//
//            chatBot.dashboardCategories.map {
//                Message.Builder()
//                    .setType(Message.Type.CATEGORY)
////                    .setCategory(category)
//                    .build()
//            }
//        } else {
//            categories.map {
//                Message.Builder()
//                    .setType(Message.Type.CROSS_CHILDREN)
////                    .setCategory(category)
//                    .build()
//            }
//        }

//        chatBot.activeCategory = null

//        getView().setNewMessages(messages)

        chatListViewState?.let { getView().restoreChatListViewState(it) }
    }

    fun onUrlInTextClicked(url: String) {
        debug(TAG, "onUrlInTextClicked -> viewState: $viewState")

        if (url.startsWith("#")) {
            if (viewState is ViewState.ChatBot) {
                viewState = ViewState.ChatBot.UserPrompt(true)
            }

            val text = url.removePrefix("#")
            sendUserMessage(text, false)

            getView().showGoToHomeButton()
        } else {
            getView().showOpenLinkConfirmAlert(url)
        }

    }

    fun onGoToHomeButtonClicked() {
        debug(TAG, "onGoToHomeClicked() -> viewState: $viewState")

        when (viewState) {
            is ViewState.AudioDialog, is ViewState.VideoDialog -> {
                getView().clearChatMessages()
                getView().clearChatFooterMessages()

                viewState = ViewState.CallAgentCall
            }
            is ViewState.Services -> {
                if (activeService != null) {
                    socketClient?.sendCancel()
                }

                if (activeServiceSession != null) {
                    socketClient?.sendCancel()
                    activeServiceSession = null
                }

                getView().clearChatMessages()
                getView().clearChatFooterMessages()

                viewState = ViewState.Services.IDLE
            }
            is ViewState.DynamicForm -> {
                activeForm = null

                getView().clearDynamicForm()

                getView().clearChatFooterMessages()

                if (activeServiceSession != null) {
                    activeServiceSession = null

                    getView().clearChatMessages()

                    viewState = ViewState.Services.IDLE
                } else {
                    viewState = ViewState.ChatBot.Dashboard(false)
                }
            }
            else -> {
                if (activeServiceSession != null) {
                    activeServiceSession = null

                    getView().clearDynamicForm()

                    getView().clearChatMessages()
                    getView().clearChatFooterMessages()

                    viewState = ViewState.Services.IDLE
                } else {
//                    val messages = chatBot.dashboardCategories.map { category ->
//                        Message.Builder()
//                            .setType(Message.Type.CATEGORY)
////                            .setCategory(category)
//                            .build()
//                    }

                    getView().clearChatFooterMessages()

//                    viewState = if (messages.isEmpty()) {
////                        socketClient?.requestParentCategories()
//
//                        ViewState.ChatBot.Dashboard(true)
//                    } else {
////                        getView().setNewMessages(messages)
//
//                        chatListViewState?.let { getView().restoreChatListViewState(it) }
//
//                        ViewState.ChatBot.Dashboard(false)
//                    }
                }
            }
        }
    }

    fun onCallCancelClicked(callType: CallType? = null) {
        cancelPendingCall()

        viewState = if (callType == null) {
            ViewState.CallAgentCall
        } else {
            when (callType) {
                CallType.TEXT -> ViewState.ChatBot.UserPrompt(false)
                CallType.AUDIO, CallType.VIDEO -> ViewState.CallAgentCall
            }
        }
    }

    fun onUploadFile(filePath: String) {
//        val file = File(File(filePath))
//        val type = file.getFileType() ?: return
//
//        val params = RequestParams().apply {
//            put("type", type)
//            put("file", file)
//        }
//
//        httpClient.uploadFile(UrlUtil.buildUrl("/upload") ?: return, params) { path, hash ->
//            debug(TAG, "uploadFile: $path, $hash")
//
//            val fullUrl = UrlUtil.buildUrl(path)
//
//            if (activeForm != null) {
//                getView().showAttachmentThumbnail(
//                    Attachment(
//                        title = hash,
//                        ext = hash.split(".").last(),
//                        type = type,
//                        url = fullUrl
//                    )
//                )
//            } else {
//                socketClient?.sendUserMediaMessage(type, path)
//
//                val media = if (type == "image") {
//                    Media(
//                        imageUrl = fullUrl,
//                        hash = hash,
//                        ext = hash.split(".").last(),
//                        local = file
//                    )
//                } else {
//                    Media(
//                        fileUrl = fullUrl,
//                        hash = hash,
//                        ext = hash.split(".").last(),
//                        local = file
//                    )
//                }
//
//                getView().addNewMessage(Message(type = Message.Type.OUTGOING, media = media))
//            }
//        }
    }

    fun onLocalDescription(sessionDescription: SessionDescription) {
        socketClient?.sendLocalSessionDescription(sessionDescription)
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

//        dialog.clear()

        getView().releaseMediaPlayer()

        getView().releaseVideoDialog()

        peerConnectionClient.dispose()

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
//        dialog.clear()

        getView().releaseMediaPlayer()

        getView().showUserDisconnectedMessage()

        peerConnectionClient.dispose()

        if (viewState is ViewState.TextDialog.Pending) {
            socketClient?.sendCancel()
        } else {
            socketClient?.sendCallAction(action = CallAction.FINISH)
        }

        viewState = when (viewState) {
            is ViewState.TextDialog -> ViewState.TextDialog.UserDisconnected
            is ViewState.AudioDialog -> ViewState.AudioDialog.UserDisconnected
            is ViewState.VideoDialog -> {
                getView().releaseVideoDialog()

                ViewState.VideoDialog.UserDisconnected
            }
            else -> ViewState.ChatBot.UserPrompt(false)
        }
    }

    fun onIceCandidate(iceCandidate: IceCandidate) {
        socketClient?.sendLocalIceCandidate(iceCandidate)
    }

    fun onFormSendButtonClicked(name: String, email: String, phone: String) {
        debug(TAG, "onSendClicked -> viewState: $viewState")

        socketClient?.sendFuzzyTaskConfirmation(name, email, phone)

        getView().showFormSentSuccessAlert()
    }

    fun onFormSendButtonClicked(form: Form) {
        debug(TAG, "onSendClicked() -> form: $form")

//        socketClient?.sendFormFinalize(null, dynamicForm, emptyList())

        activeForm = null

        getView().clearDynamicForm()

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

        activeForm = null

        getView().clearDynamicForm()

        viewState = if (activeServiceSession == null) {
            if (viewState is ViewState.Services) {
                ViewState.Services.Cancelled
            } else {
                ViewState.ChatBot.UserPrompt(false)
            }
        } else {
            getView().showGoToHomeButton()
            ViewState.Services.Cancelled
        }
    }

    fun onRegisterAppealButtonClicked() {
        getView().showGoToHomeButton()

        viewState = ViewState.Form
    }

    fun onAppealRegistered() {
        viewState = ViewState.ChatBot.UserPrompt(false)
    }

    fun onNewChatMessagesInserted() {
        if (viewState is ViewState.ChatBot.Dashboard) return

        getView().scrollToBottom()
    }

    fun onHangupButtonClicked() {
        getView().showHangupConfirmationAlert()
    }

    fun onAddAttachmentButtonClicked() {
        getView().showAttachmentPicker(forced = false)
    }

    fun onReplyMarkupButtonClicked(button: Button) {
        if (button is UrlButton) {
            getView().openLink(button.url)
        } else if (button is CallbackButton) {
            getView().addNewMessage(
                Message.Builder()
                    .setType(Message.Type.OUTGOING)
                    .setText(button.text)
                    .build()
            )

            socketClient?.sendExternal(button.payload)
        }
    }

    fun onSelectAttachmentButtonClicked(field: Form.Field) {
        debug(TAG, "field: $field")
        getView().showAttachmentPicker(forced = true)
    }

    /**
     * [SocketStateListener] implementation
     */

    override fun onSocketConnect() {
        socketClient?.sendUserLanguage(language)

        viewState = if (configs?.preferences?.isChatBotEnabled == true) {
//            socketClient?.requestParentCategories()
            ViewState.ChatBot.Dashboard(true)
        } else {
            when {
                configs?.preferences?.isAudioCallEnabled == true || configs?.preferences?.isVideoCallEnabled == true ->
                    ViewState.CallAgentCall
                configs?.preferences?.isServicesEnabled == true ->
                    ViewState.Services.IDLE
                else ->
                    ViewState.Info
            }
        }
    }

    override fun onSocketDisconnect() {
        onCloseLiveCall()
    }

    /**
     * [ChatBotListener] implementation
     */

    override fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean {
        debug(TAG, "onFuzzyTaskOffered -> viewState: $viewState")

        getView().addNewMessage(
            Message.Builder()
                .setType(Message.Type.INCOMING)
                .setText(text)
                .setCreatedAt(timestamp)
                .build()
        )
        getView().showFuzzyQuestionButtons()

        viewState = ViewState.ChatBot.UserPrompt(false)

        return true
    }

    override fun onNoResultsFound(text: String, timestamp: Long): Boolean {
        debug(TAG, "onNoResultsFound -> viewState: $viewState")

        getView().addNewMessage(
            Message.Builder()
                .setType(Message.Type.INCOMING)
                .setText(text)
                .setCreatedAt(timestamp)
                .build()
        )

//        if (viewState is ViewState.ChatBot.UserPrompt && !dialog.isInitiator) {
//            getView().showSwitchToCallAgentButton()
//        }

        if (viewState is ViewState.ChatBot.UserPrompt) {
            viewState = ViewState.ChatBot.UserPrompt(false)
        }

        return true
    }

    override fun onMessage(message: Message) {
        debug(TAG, "onTextMessage -> viewState: $viewState")

        // onMediaMessage()
//        if (media.type == Media.Type.IMAGE || media.type == Media.Type.AUDIO || media.type == Media.Type.FILE) {
//            getView().addNewMessage(
//                Message(
//                    type = Message.Type.INCOMING,
//                    media = media,
//                    replyMarkup = replyMarkup,
//                    timestamp = timestamp
//                )
//            )
//        }

//        if (chatBot.activeCategory != null) {
//            val newMessage = message.copy(category = chatBot.activeCategory)

//            getView().setNewMessages(newMessage)
//            getView().scrollToTop()
//            getView().showGoToHomeButton()

//            viewState = ViewState.ChatBot.Dashboard(false)
//
//            return
//        }

        if (viewState is ViewState.AudioDialog.Live || viewState is ViewState.VideoDialog.Live) {
            getView().showFooterView()
        }

        if (viewState is ViewState.AudioDialog.Live && (viewState as ViewState.AudioDialog.Live).isDialogScreenShown) {
//            dialog.unreadMessages += 1
//            if (dialog.unreadMessages >= Dialog.MAX_UNREAD_MESSAGES_COUNT) {
//                getView().setUnreadMessagesCountOnCall(
//                    CallType.AUDIO,
//                    "${dialog.unreadMessages}+"
//                )
//            } else {
//                getView().setUnreadMessagesCountOnCall(
//                    CallType.AUDIO,
//                    "${dialog.unreadMessages}"
//                )
//            }
        } else if (viewState is ViewState.VideoDialog.Live && (viewState as ViewState.VideoDialog.Live).isDialogScreenShown) {
//            dialog.unreadMessages += 1
//            if (dialog.unreadMessages >= Dialog.MAX_UNREAD_MESSAGES_COUNT) {
//                getView().setUnreadMessagesCountOnCall(
//                    CallType.VIDEO,
//                    "${dialog.unreadMessages}+"
//                )
//            } else {
//                getView().setUnreadMessagesCountOnCall(
//                    CallType.VIDEO,
//                    "${dialog.unreadMessages}"
//                )
//            }
        }

        getView().addNewMessage(message)

//        if (form != null) {
//            socketClient?.sendFormInitialize(formId = form.id)
//        }

        if (viewState is ViewState.ChatBot) {
//                    debug(TAG, "onTextMessage: chatFooterAdapter?.showGoToHomeButton()")

            getView().showGoToHomeButton()

            viewState = ViewState.ChatBot.UserPrompt(false)
        } else if (viewState is ViewState.Services) {
            getView().showGoToHomeButton()
        }
    }

    override fun onCategories(categories: List<Category>) {
        debug(TAG, "onCategories() -> categories: $categories")

//        if (viewState is ViewState.ChatBot.UserPrompt) return

//        val sortedCategories = categories.sortedBy { it.config?.order }
//        chatBot.allCategories.addAll(categories)

//        if (!chatBot.isParentResponseGroupChildrenRequested) {
//            chatBot.allCategories.forEach { category ->
//                debug(TAG, "category: $category, ${category.parentId == null}")

//                if (category.parentId == Category.NO_PARENT_ID) {
//                    socketClient?.requestCategories(category.id)
//                }
//            }

//            chatBot.isParentResponseGroupChildrenRequested = true
//        }

//        if (chatBot.activeCategory != null) {
//            if (chatBot.activeCategory?.children?.containsAll(categories) == false) {
//                chatBot.activeCategory?.children?.addAll(categories)
//            }
//            }
//            getView().setNewMessages(
//                Message.Builder()
//                    .setType(Message.Type.CROSS_CHILDREN)
//                    .apply {
//                        chatBot.activeCategory?.let {
////                            setCategory(it)
//                        }
//                    }
//                    .build()
//            )
//            getView().scrollToTop()
//            getView().showGoToHomeButton()
//        }

//        if (viewState is ViewState.ChatBot.Dashboard && (viewState as ViewState.ChatBot.Dashboard).isLoading) {
//            viewState = ViewState.ChatBot.Dashboard(false)
//        }
    }

    /**
     * [DialogListener] implementation
     */

    override fun onPendingUsersQueueCount(text: String?, count: Int) {
        if (viewState is ViewState.AudioDialog || viewState is ViewState.VideoDialog) {
            if (!text.isNullOrBlank()) {
                getView().setOperatorCallInfoText(text)
            }
            if (count > 1) {
                getView().setOperatorCallPendingQueueCount(count)
            }
        }
    }

    override fun onNoOnlineCallAgents(text: String?): Boolean {
        debug(TAG, "onNoOnlineCallAgents -> viewState: $viewState")

//        dialog.isInitiator = false

//        getView().addNewMessage(Messag(type = Messag.Type.INCOMING, text = text))

        getView().showNoOnlineCallAgentsAlert(text ?: "")

        when (viewState) {
            is ViewState.TextDialog -> {
                getView().showGoToHomeButton()
                viewState = ViewState.ChatBot.UserPrompt(false)
            }
            is ViewState.AudioDialog, is ViewState.VideoDialog ->
                viewState = ViewState.CallAgentCall
            else -> {
            }
        }

        return true
    }

    override fun onCallAgentGreet(fullName: String, photoUrl: String?, text: String) {
        debug(TAG, "onCallAgentGreet() -> viewState: $viewState")

        if (viewState is ViewState.TextDialog) {
//            dialog.isActive = true
            viewState = ViewState.TextDialog.Live
        }

        val newText = text.replace("{}", fullName)

        getView().showPeerInfo(fullName, photoUrl)

        if (viewState is ViewState.AudioDialog) {
            getView().showAudioCallerInformation(fullName, photoUrl)
        }

//        getView().addNewMessage(Messag(type = Messag.Type.INCOMING, text = newText))
    }

    override fun onCallFeedback(text: String, rateButtons: List<RateButton>?) {
        debug(TAG, "onFeedback() -> viewState: $viewState")

        getView().showFeedback(text, rateButtons)

        when (viewState) {
            is ViewState.TextDialog ->
                viewState = ViewState.TextDialog.UserFeedback(false)
            is ViewState.AudioDialog ->
                viewState = ViewState.AudioDialog.UserFeedback(false)
            is ViewState.VideoDialog ->
                viewState = ViewState.VideoDialog.UserFeedback(false)
            else -> {
            }
        }
    }

    override fun onLiveChatTimeout(text: String?, timestamp: Long): Boolean {
        debug(TAG, "onChatTimeout -> viewState: $viewState")

        disconnectFromCall(text ?: "", timestamp)

        return true
    }

    override fun onUserRedirected(text: String?, timestamp: Long): Boolean {
        debug(TAG, "onUserRedirected() -> viewState: $viewState")

//        dialog.clear()

        getView().releaseMediaPlayer()

        getView().releaseVideoDialog()

        peerConnectionClient.dispose()

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

//        getView().addNewMessage(
//            Messag(
//                type = Messag.Type.NOTIFICATION,
//                text = text,
//                timestamp = timestamp
//            )
//        )

        return true
    }

    override fun onCallAgentDisconnected(text: String?, timestamp: Long): Boolean {
        debug(TAG, "onCallAgentDisconnected() -> viewState: $viewState")

        disconnectFromCall(text ?: "", timestamp)

        return true
    }

    private fun disconnectFromCall(text: String, timestamp: Long) {
        onCloseLiveCall()

//        getView().addNewMessage(
//            Messag(
//                type = Messag.Type.NOTIFICATION,
//                text = text,
//                timestamp = timestamp
//            )
//        )
        getView().showGoToHomeButton()
    }

    /**
     * [FormListener] implementation
     */

    override fun onFormInit(form: Form) {
        activeForm = form

        getView().showForm(form)

        viewState = ViewState.DynamicForm
    }

    override fun onFormFound(message: Message, form: Form): Boolean {
        return true
    }

    override fun onFormFinal(trackId: String?, taskId: Long?, message: String?, success: Boolean) {
//        getView().addNewMessage(Messag(type = Messag.Type.INCOMING, text = message))

        activeForm = null

        getView().clearDynamicForm()

        getView().showGoToHomeButton()

        debug(TAG, "onFormFinal() -> viewState: $viewState")

        viewState = if (viewState is ViewState.Services) {
            ViewState.Services.Completed
        } else {
            ViewState.ChatBot.Dashboard(isLoading = false)
        }
    }

    /**
     * [WebRTCListener] implementation
     */

    override fun onCallAccept() {
        debug(TAG, "onCallAccept -> viewState: $viewState")

        if (viewState is ViewState.AudioDialog) {
//            dialog.isActive = true

            viewState = ViewState.AudioDialog.Start

            peerConnectionClient.createPeerConnection(
                options = Options(
                    isLocalAudioEnabled = true,
                    isRemoteAudioEnabled = true,
                    isLocalVideoEnabled = false,
                    isRemoteVideoEnabled = false,
                    iceServers = iceServers
                ),
                listener = this
            )

            socketClient?.sendQRTCAction(QRTCAction.PREPARE)
        } else if (viewState is ViewState.VideoDialog) {
//            dialog.isActive = true

            viewState = ViewState.VideoDialog.Start

            peerConnectionClient.createPeerConnection(
                options = Options(
                    isLocalAudioEnabled = true,
                    isRemoteAudioEnabled = true,
                    isLocalVideoEnabled = true,
                    isRemoteVideoEnabled = true,
                    iceServers = iceServers
                ),
                listener = this
            )

            peerConnectionClient.initLocalCameraStream()

            socketClient?.sendQRTCAction(QRTCAction.PREPARE)
        }
    }

    override fun onCallRedirect() {

    }

    override fun onCallRedial() {
    }

    override fun onCallPrepare() {
        debug(TAG, "onCallPrepare() -> viewState: $viewState")

        if (viewState is ViewState.AudioDialog) {
            viewState = ViewState.AudioDialog.Preparation

            peerConnectionClient.addLocalStreamToPeer()

            socketClient?.sendQRTCAction(QRTCAction.READY)
        } else if (viewState is ViewState.VideoDialog) {
            viewState = ViewState.VideoDialog.Preparation

            peerConnectionClient.addLocalStreamToPeer()

            socketClient?.sendQRTCAction(QRTCAction.READY)
        }
    }

    override fun onCallReady() {
        debug(TAG, "onCallReady() -> viewState: $viewState")

        if (viewState is ViewState.AudioDialog) {
            viewState = ViewState.AudioDialog.Ready

            peerConnectionClient.addLocalStreamToPeer()
            peerConnectionClient.createOffer()
        } else if (viewState is ViewState.VideoDialog) {
            viewState = ViewState.VideoDialog.Ready

            peerConnectionClient.addLocalStreamToPeer()
            peerConnectionClient.createOffer()
        }
    }

    override fun onCallAnswer(sessionDescription: SessionDescription) {
        peerConnectionClient.setRemoteDescription(sessionDescription)
    }

    override fun onCallOffer(sessionDescription: SessionDescription) {
        peerConnectionClient.setRemoteDescription(sessionDescription)
        peerConnectionClient.createAnswer()
    }

    override fun onRemoteIceCandidate(iceCandidate: IceCandidate) {
        peerConnectionClient.addRemoteIceCandidate(iceCandidate)
    }

    override fun onPeerHangupCall() {
        onCloseLiveCall()
    }


    /**
     * [PeerConnectionClient.Listener] implementation
     */

    override fun onLocalSessionDescription(sessionDescription: SessionDescription) {
        socketClient?.sendLocalSessionDescription(sessionDescription)
    }

    override fun onLocalIceCandidate(iceCandidate: IceCandidate) {
        socketClient?.sendLocalIceCandidate(iceCandidate)
    }

    override fun onIceConnectionChange(iceConnectionState: IceConnectionState) {
    }

    override fun onRenegotiationNeeded() {
    }

    override fun onAddRemoteStream(mediaStream: MediaStream) {
        peerConnectionClient.addRemoteStreamToPeer(mediaStream)
    }

    override fun onRemoveStream(mediaStream: MediaStream) {
        peerConnectionClient.removeStream(mediaStream)
    }

    override fun onLocalVideoCapturerCreateError(e: Exception) {
    }

    override fun onPeerConnectionError(errorMessage: String) {
    }

}