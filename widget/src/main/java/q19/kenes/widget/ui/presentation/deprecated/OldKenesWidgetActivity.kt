package q19.kenes.widget.ui.presentation.deprecated

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kz.q19.domain.model.call.CallType
import kz.q19.domain.model.configs.Configs
import kz.q19.domain.model.form.Form
import kz.q19.domain.model.keyboard.button.Button
import kz.q19.domain.model.keyboard.button.RateButton
import kz.q19.domain.model.language.Language
import kz.q19.domain.model.media.Media
import kz.q19.domain.model.message.Message
import kz.q19.socket.model.Category
import kz.q19.utils.keyboard.hideKeyboard
import kz.q19.utils.recyclerview.disableChangeAnimations
import kz.q19.utils.recyclerview.setOverScrollColor
import kz.q19.utils.view.binding.bind
import kz.q19.utils.view.isVisible
import kz.q19.webrtc.Options
import kz.q19.webrtc.PeerConnectionClient
import q19.kenes.widget.core.device.DeviceInfo
import q19.kenes.widget.core.logging.Logger.debug
import q19.kenes.widget.core.permission.PermissionManager
import q19.kenes.widget.ui.components.*
import q19.kenes.widget.ui.presentation.adapter.ChatAdapter
import q19.kenes.widget.ui.presentation.adapter.ChatAdapterItemDecoration
import q19.kenes.widget.ui.presentation.adapter.ChatFooterAdapter
import q19.kenes.widget.ui.presentation.model.ViewState
import q19.kenes.widget.ui.presentation.platform.BaseActivity
import q19.kenes.widget.util.*
import q19.kenes_widget.R

internal class OldKenesWidgetActivity : BaseActivity(), OldKenesWidgetView {

    companion object {
        private val TAG = OldKenesWidgetActivity::class.java.simpleName

        fun newIntent(
            context: Context,
            hostname: String,
            language: Language? = null,
            firstName: String? = null,
            lastName: String? = null,
            phoneNumber: String? = null
        ): Intent =
            Intent(context, OldKenesWidgetActivity::class.java)
                .putExtra(IntentKey.HOSTNAME, hostname)
                .putExtra(IntentKey.LANGUAGE, language?.key)
                .putExtra(IntentKey.FIRST_NAME, firstName)
                .putExtra(IntentKey.LAST_NAME, lastName)
                .putExtra(IntentKey.PHONE_NUMBER, phoneNumber)
    }

    private object IntentKey {
        const val HOSTNAME = "hostname"
        const val LANGUAGE = "language"
        const val FIRST_NAME = "first_name"
        const val LAST_NAME = "last_name"
        const val PHONE_NUMBER = "phone_number"
    }
    
    private object PermissionRequestCode {
        const val PICK_FILE = 101
    }

    // -------------------------- Binding views -----------------------------------

    /**
     * Parent root view [rootView].
     */
    private val rootView by bind<FrameLayout>(R.id.rootView)

    /**
     * Header view [headerView] for opponent info display.
     */
    private val headerView by bind<HeaderView>(R.id.headerView)

    /**
     * Audio call [operatorCallView] screen view.
     */
    private val operatorCallView by bind<CallTypeView>(R.id.operatorCallView)

    private val operatorCallPendingView by bind<OperatorCallPendingView>(R.id.operatorCallPendingView)

    /**
     * Info screen view [infoView] with extra information & contacts.
     */
    private val infoView by bind<InfoView>(R.id.infoView)

    /**
     * Footer view [footerView] for messenger.
     */
    private val footerView by bind<MessageInputView>(R.id.footerView)

    /**
     * View [recyclerView] for chat.
     */
    private val recyclerView by bind<RecyclerView>(R.id.recyclerView)

    /**
     * User feedback view [feedbackView] after dialog view.
     */
    private val feedbackView by bind<FeedbackView>(R.id.feedbackView)

    private val formView by bind<FormView>(R.id.formView)

    private val dynamicFormView by bind<DynamicFormView>(R.id.dynamicFormView)

    private val progressView by bind<ProgressView>(R.id.progressView)

    /**
     * Bottom navigation view variables: [bottomNavigationView]
     */
    private val bottomNavigationView by bind<BottomNavigationView>(R.id.bottomNavigationView)

    /**
     * Video dialog view variables: [videoDialogView]
     */
    private val videoDialogView by bind<VideoDialogView>(R.id.videoDialogView)

    /**
     * Audio dialog view variables: [audioDialogView]
     */
    private val audioDialogView by bind<AudioDialogView>(R.id.audioDialogView)

//    private val contactsView by bind<ContactsView>(R.id.contactsView)

    private val servicesView by bind<ServicesView>(R.id.servicesView)

    private val keyboardView by bind<KeyboardView>(R.id.keyboardView)

    // ------------------------------------------------------------------------

    private var concatAdapter: ConcatAdapter? = null
    private var chatAdapter: ChatAdapter? = null
    private var chatFooterAdapter: ChatFooterAdapter? = null

    // ------------------------------------------------------------------------

    private val permissionManager by lazy {
        PermissionManager(this)
    }

    // ------------------------------------------------------------------------

    private val chatAdapterDataObserver by lazy {
        object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)

                presenter.onNewChatMessagesInserted()
            }
        }
    }

    private val chatFooterAdapterDataObserver by lazy {
        object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)

                presenter.onNewChatMessagesInserted()
            }
        }
    }

    // ------------------------------------------------------------------------

    private var mediaPlayer: MediaPlayer? = null
    private var handler = Handler()
    @Volatile
    private var currentAudioPlayingItemPosition: Int = -1
    @Volatile
    private var isAudioPlayCompleted = false
    @Volatile
    private var isAudioPaused: Boolean = false

    private lateinit var presenter: OldKenesWidgetPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kenes_activity_widget_v2)

        // Status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // Hostname
        val hostname = intent.getStringExtra(IntentKey.HOSTNAME)
        if (hostname.isNullOrBlank() || !hostname.startsWith("https://")) {
            throwError()
        } else {
            UrlUtil.setHostname(hostname)
        }

        // ------------------------------------------------------------------------

        presenter = OldKenesWidgetPresenter(
            deviceInfo = DeviceInfo(this),
            language = Language.DEFAULT,
            peerConnectionClient = PeerConnectionClient(
                context = this,
                options = Options(),
                localSurfaceViewRenderer = videoDialogView.localSurfaceView,
                remoteSurfaceViewRenderer = videoDialogView.remoteSurfaceView
            )
        )
        presenter.attachView(this)

        // ------------------------------------------------------------------------

        if (Picasso.get() == null) {
            Picasso.setSingletonInstance(Picasso.Builder(this).build())
        }

        // ------------------------------------------------------------------------


        /**
         * Configuration of home bottom navigation button action listeners (click/touch)
         */
        bottomNavigationView.callback = object : BottomNavigationView.Callback {
            override fun onBottomNavigationButtonSelected(navigationButton: BottomNavigationView.NavigationButton) {
                presenter.onBottomNavigationButtonClicked(navigationButton)
            }

            override fun onBottomNavigationButtonReselected(navigationButton: BottomNavigationView.NavigationButton) {
                presenter.onBottomNavigationButtonClicked(navigationButton)
            }
        }

        /**
         * Configuration of other button action listeners (click/touch)
         */
        headerView.callback = object : HeaderView.Callback {
            override fun onHangupButtonClicked() {
                presenter.onHangupButtonClicked()
            }
        }

        operatorCallView.callback = object : CallTypeView.Callback {
            override fun onCallTypeClicked(callType: CallType) {
                presenter.onCallTypeClicked(callType)
            }

            override fun onCallClicked(call: Configs.Call) {
                presenter.onCallClicked(call)
            }

            override fun onCallBackClicked() {
                presenter.onCallBackClicked()
            }
        }

        operatorCallPendingView.setOnCancelCallButtonClickListener {
            presenter.onCallCancelClicked()
        }

//        contactsView.callback = object : ContactsView.Callback {
//            override fun onInfoBlockItemClicked(item: Configs.Item) {
//                try {
//                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.action))
//                    startActivity(intent)
//                } catch (e: ActivityNotFoundException) {
//                    e.printStackTrace()
//                }
//            }
//        }

        servicesView.callback = object : ServicesView.Callback {
            override fun onServiceClicked(service: Configs.Service) {
                presenter.onServiceClicked(service)
            }

            override fun onServiceBackClicked() {
                presenter.onServiceBackClicked()
            }
        }

        keyboardView.callback = object : KeyboardView.Callback {
            override fun onReplyMarkupButtonClicked(button: Button) {
                presenter.onReplyMarkupButtonClicked(button)
            }
        }

        formView.callback = object : FormView.Callback {
            override fun onCancelClicked() {
                presenter.onFormCancelClicked()
            }

            override fun onSendClicked(name: String, email: String, phone: String) {
                presenter.onFormSendButtonClicked(name, email, phone)
            }
        }

        dynamicFormView.callback = object : DynamicFormView.Callback {
            override fun onCancelButtonClicked() {
                presenter.onFormCancelClicked()
            }

            override fun onSendButtonClicked(form: Form) {
                presenter.onFormSendButtonClicked(form)
            }

            override fun onSelectAttachmentButtonClicked(field: Form.Field) {
                presenter.onSelectAttachmentButtonClicked(field)
            }

            override fun onAttachmentClicked(attachment: Media) {
//                val file = attachment.getFile(this@KenesWidgetActivity)
//                presenter.onAttachmentClicked(file)
            }
        }

//        recyclerView.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
//            if (viewState is ViewState.ChatBot.UserPrompt && bottom < oldBottom) {
//                recyclerView.postDelayed(
//                    { concatAdapter?.let { recyclerView.scrollToPosition(it.itemCount - 1) } },
//                    1
//                )
//            }
//        }

        rootView.viewTreeObserver?.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)

            // finding screen height
            val screenHeight = rootView.rootView?.height ?: 0

            // finding keyboard height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                bottomNavigationView.hide()
            } else {
                bottomNavigationView.show()
            }
        }

        footerView.callback = object : MessageInputView.Callback {
            override fun onNewMediaSelection() {
                presenter.onAddAttachmentButtonClicked()
            }

            override fun onSendTextMessage(message: String?) {
                presenter.onSendMessageButtonClicked(message)
            }
        }

//        footerView.setOnInputViewFocusChangeListener { v, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_SEND || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
//                debug(TAG, "IME_ACTION_SEND || KEYCODE_ENTER -> viewState: $viewState")
//
//                val text = v?.text.toString()
//
//                if (text.isBlank()) {
//                    return@setOnInputViewFocusChangeListener false
//                }
//
//                if (viewState is ViewState.ChatBot) {
//                    viewState = ViewState.ChatBot.UserPrompt(true)
//                }
//
//                sendUserMessage(text, true)
//
//                return@setOnInputViewFocusChangeListener true
//            }
//            return@setOnInputViewFocusChangeListener false
//        }

        footerView.setOnTextChangedListener { s, _, _, _ ->
            if (s.isNullOrBlank()) {
                footerView.disableSendMessageButton()
            } else {
                footerView.enableSendMessageButton()
            }
        }

        videoDialogView.callback = object : VideoDialogView.Callback {
            override fun onGoToChatButtonClicked() {
                presenter.onGoToChatButtonClicked(CallType.VIDEO)
            }

            override fun onHangupButtonClicked() {
                presenter.onHangupButtonClicked()
            }

            override fun onSwitchSourceButtonClicked() {
//                peerConnectionClient?.onSwitchCamera(onDone = {}, onError = {})
            }

            override fun onSwitchScalingButtonClicked() {
                // TODO: Toggle
//                peerConnectionClient?.setLocalVideoScalingType(ScalingType.SCALE_ASPECT_FIT)
            }

            override fun onFullscreenScreenClicked() {
                if (videoDialogView.isControlButtonsVisible()) {
                    videoDialogView.hideControlButtons()
                } else {
                    videoDialogView.showControlButtons()
                }
            }
        }

        audioDialogView.callback = object : AudioDialogView.Callback {
            override fun onGoToChatButtonClicked() {
                presenter.onGoToChatButtonClicked(CallType.AUDIO)
            }

            override fun onHangupButtonClicked() {
                presenter.onHangupButtonClicked()
            }
        }

        infoView.callback = object : InfoView.Callback {
            override fun onPhoneNumberClicked(phoneNumber: String) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                startActivity(intent)
            }

            override fun onSocialClicked(contact: Configs.Contacts.Social) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contact.url))
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }

            override fun onLanguageChangeClicked(language: Language) {
                val supportedLanguages = Language.getSupportedLanguages()
                val items = supportedLanguages.map { it.key }.toTypedArray()
                showLanguageSelectionAlert(items) { which ->
                    val selected = supportedLanguages[which]

                    presenter.onLanguageSelected(selected.key)

                    setLocale(selected.locale)
                }
            }
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(object : ChatAdapter.Callback {
            override fun onShowAllCategoryChildClicked(category: Category) {
//                presenter.onShowAllCategoryChildClicked(category)
            }

            override fun onCategoryChildClicked(category: Category) {
                hideKeyboard()

                presenter.onCategoryChildClicked(category)
                presenter.setChatListViewState(recyclerView.layoutManager?.onSaveInstanceState())
            }

            override fun onGoBackClicked(category: Category) {
                hideKeyboard()

                presenter.onGoBackClicked(category)
            }

            override fun onUrlInTextClicked(url: String) {
                presenter.onUrlInTextClicked(url)
            }

            override fun onImageClicked(imageView: ImageView, imageUrl: String) {
//                imageView.showFullscreenImage(imageUrl)
            }

            override fun onImageClicked(imageView: ImageView, bitmap: Bitmap) {
//                imageView.showFullscreenImage(bitmap)
            }

            override fun onImageLoadCompleted() {
            }

            override fun onMediaClicked(media: Media, itemPosition: Int) {
                debug(TAG, "onMediaClicked: $media, itemPosition: $itemPosition")

//                val file = media.getFile(this@KenesWidgetActivity)
//                presenter.onMediaClicked(media, file, itemPosition)
            }

            override fun onAttachmentClicked(attachment: Media, itemPosition: Int) {
                debug(TAG, "onAttachmentClicked: $attachment, itemPosition: $itemPosition")

//                val file = attachment.getFile(this@KenesWidgetActivity)
//                presenter.onAttachmentClicked(attachment, file, itemPosition)
            }

            override fun onReplyMarkupButtonClicked(button: Button) {
                presenter.onReplyMarkupButtonClicked(button)
            }

            override fun onStopTrackingTouch(progress: Int, itemPosition: Int) {
                debug(
                    TAG,
                    "onStopTrackingTouch -> progress: $progress, itemPosition: $itemPosition"
                )

                if (currentAudioPlayingItemPosition == itemPosition) {
                    isAudioPaused = false
                    isAudioPlayCompleted = false
                    val milliseconds = (progress * (mediaPlayer?.duration ?: 1)) / 100
                    mediaPlayer?.seekTo(milliseconds)
                    mediaPlayer?.start()
                    updateProgress(itemPosition)
                }
            }
        })

        recyclerView.setOverScrollColor(R.color.kenes_very_light_blue)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.isNestedScrollingEnabled = false

        chatAdapter?.registerAdapterDataObserver(chatAdapterDataObserver)
        chatFooterAdapter?.registerAdapterDataObserver(chatFooterAdapterDataObserver)

        chatFooterAdapter = ChatFooterAdapter()
        chatFooterAdapter?.callback = object : ChatFooterAdapter.Callback {
            override fun onGoToHomeButtonClicked() {
                presenter.onGoToHomeButtonClicked()
            }

            override fun onSwitchToCallAgentButtonClicked() {
                presenter.onCallOperator(CallType.TEXT)
            }

            override fun onRegisterAppealButtonClicked() {
                presenter.onRegisterAppealButtonClicked()
            }
        }

        concatAdapter = ConcatAdapter(chatAdapter, chatFooterAdapter)

        recyclerView.adapter = concatAdapter
        recyclerView.disableChangeAnimations()
        recyclerView.addItemDecoration(ChatAdapterItemDecoration(this))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PermissionRequestCode.PICK_FILE && resultCode == Activity.RESULT_OK) {
//            val filePath = data?.getStringExtra(FilePickerActivity.RESULT_FILE_PATH) ?: return

//            presenter.onUploadFile(filePath)
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }

    override fun onPause() {
        super.onPause()
        releaseMediaPlayer()
    }

    override fun onBackPressed() {
        showWidgetCloseConfirmDialog { finish() }
    }

    override fun finish() {
        presenter.onCloseLiveCall()
        super.finish()
    }

    override fun clearChatMessages() {
        chatAdapter?.clear()
    }

    override fun restoreChatListViewState(chatListViewState: Parcelable) {
        recyclerView.layoutManager?.onRestoreInstanceState(chatListViewState)
    }

    override fun showCurrentLanguage(language: Language) {
        infoView.setLanguage(language)
    }

    override fun showSocials(contacts: List<Configs.Contacts.Social>) {
        runOnUiThread {
            infoView.setContacts(contacts)
        }
    }

    override fun showPhoneNumbers(phones: List<Configs.Contacts.PhoneNumber>) {
        runOnUiThread {
            infoView.setPhones(phones)
        }
    }

    override fun showDefaultPeerInfo() {

    }

    override fun showPeerInfo(opponent: Configs.CallAgent) {
        runOnUiThread {
            headerView.setOpponentInfo(opponent)
        }
    }

    override fun showPeerInfo(name: String, photoUrl: String?) {
        runOnUiThread {
//            headerView.setOpponentInfo(
//                Peer(
//                    name = name,
//                    subname = getString(R.string.kenes_operator),
//                    photoUrl = photoUrl
//                )
//            )
        }
    }

    override fun showNavButton(navigationButton: BottomNavigationView.NavigationButton) {
        runOnUiThread {
            bottomNavigationView.showNavigationButton(navigationButton)
        }
    }

    override fun hideNavButton(navigationButton: BottomNavigationView.NavigationButton) {
        runOnUiThread {
            bottomNavigationView.hideNavigationButton(navigationButton)
        }
    }

    override fun hideHangupButton() {
        runOnUiThread {
            headerView.hideHangupButton()
        }
    }

    override fun showAudioCallerInformation(fullName: String, photoUrl: String?) {
        runOnUiThread {
            audioDialogView.setName(fullName)
            audioDialogView.setAvatar(photoUrl)
        }
    }

    override fun setDefaultFooterView() {
        runOnUiThread {
            footerView.disableAttachmentButton()

//            footerView.setGoToActiveDialogButtonState(null)

            footerView.disableSendMessageButton()
        }
    }

    override fun showOperatorCallButton(callType: CallType) {
        runOnUiThread {
            operatorCallView.showCallButton(callType)
            operatorCallView.setCallButtonEnabled(callType)
        }
    }

    override fun hideOperatorCallButton(callType: CallType) {
        runOnUiThread {
            operatorCallView.hideCallButton(callType)
            operatorCallView.setCallButtonDisabled(callType)
            operatorCallView.removeListener(callType)
        }
    }

    override fun setDefaultOperatorCallView() {
        runOnUiThread {
            operatorCallPendingView.setCallTypeViewText(null)
            operatorCallPendingView.hideProgress()
            operatorCallPendingView.setInfoViewText(null)
            operatorCallPendingView.hideInfoViewText()
            operatorCallPendingView.setCancelCallButtonDisabled()
            operatorCallPendingView.isVisible = false

            operatorCallView.setCallButtonEnabled(CallType.AUDIO)
            operatorCallView.setCallButtonEnabled(CallType.VIDEO)
        }
    }

    override fun addNewMessage(message: Message) {
        runOnUiThread {
            chatAdapter?.addNewMessage(message)
        }
    }

    override fun setNewMessages(messages: List<Message>) {
        runOnUiThread {
            chatAdapter?.setNewMessages(messages)
        }
    }

    override fun setNewMessages(message: Message) {
        runOnUiThread {
            chatAdapter?.setNewMessages(message)
        }
    }

    override fun showUserDisconnectedMessage() {
        runOnUiThread {
            chatAdapter?.addNewMessage(
                Message.Builder()
                    .setType(Message.Type.NOTIFICATION)
                    .setText(getString(R.string.kenes_user_disconnected))
                    .build()
            )
        }
    }

    override fun showSwitchToCallAgentButton() {
        runOnUiThread {
            chatFooterAdapter?.showSwitchToCallAgentButton()
        }
    }

    override fun showFuzzyQuestionButtons() {
        runOnUiThread {
            chatFooterAdapter?.showFuzzyQuestionButtons()
        }
    }

    override fun showGoToHomeButton() {
        runOnUiThread {
            chatFooterAdapter?.showGoToHomeButton()
        }
    }

    override fun clearChatFooterMessages() {
        runOnUiThread {
            chatFooterAdapter?.clear()
        }
    }

    override fun showFeedback(text: String, rateButtons: List<RateButton>?) {
        runOnUiThread {
            feedbackView.setTitle(text)
            feedbackView.setRatingButtons(rateButtons ?: emptyList())
            feedbackView.setOnRateButtonClickListener { ratingButton ->
                presenter.onRateButtonClicked(ratingButton)
            }
        }
    }

    override fun showForm(form: Form) {
        runOnUiThread {
            dynamicFormView.form = form
        }
    }

    override fun showAttachmentThumbnail(attachment: Media) {
        runOnUiThread {
            dynamicFormView.attachment = attachment
        }
    }

    override fun clearDynamicForm() {
        runOnUiThread {
            dynamicFormView.resetData()
        }
    }

    override fun showCalls(
        parentCall: Configs.Call?,
        calls: List<Configs.Call>
    ) {
        runOnUiThread {
//            operatorCallView.showCalls(parentCall, calls, Language.from(getCurrentLocale()))
        }
    }

    override fun showServices(parentService: Configs.Service?, services: List<Configs.Service>) {
        runOnUiThread {
//            servicesView.showServices(parentService, services, Language.from(getCurrentLocale()))
        }
    }

    override fun setOperatorCallInfoText(text: String) {
        runOnUiThread {
            operatorCallPendingView.hideProgress()
            operatorCallPendingView.setInfoViewText(text)
            operatorCallPendingView.showInfoViewText()
        }
    }

    override fun setOperatorCallPendingQueueCount(count: Int) {
        runOnUiThread {
            operatorCallPendingView.setPendingQueueCountViewText(
                getString(R.string.kenes_queue_count, count)
            )
            operatorCallPendingView.showPendingQueueCountView()
        }
    }

    override fun showHangupConfirmationAlert() {
        runOnUiThread {
            showHangupConfirmAlert {
                presenter.onHangupLiveCall()
            }
        }
    }

    override fun showAlreadyCallingAlert(navigationButton: BottomNavigationView.NavigationButton) {
        runOnUiThread {
            showAlreadyCallingAlert {
                presenter.onCancelPendingCallClicked(navigationButton)
            }
        }
    }

    override fun showAlreadyCallingAlert(callType: CallType) {
        runOnUiThread {
            showAlreadyCallingAlert {
                presenter.onCallCancelClicked(callType)
            }
        }
    }

    override fun showNoOnlineCallAgentsAlert(text: String) {
        runOnUiThread {
            showNoOnlineCallAgents(text) {}
        }
    }

    override fun showOpenLinkConfirmAlert(url: String) {
        runOnUiThread {
            showOpenLinkConfirmAlert(url) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun showFooterView() {
        runOnUiThread {
            if (!footerView.isAttachmentButtonEnabled) {
                footerView.enableAttachmentButton()
            }
            footerView.isVisible = true
        }
    }

    override fun setUnreadMessagesCountOnCall(callType: CallType, count: String) {
        if (callType == CallType.AUDIO) {
            runOnUiThread {
                audioDialogView.setUnreadMessagesCount(count)
                if (audioDialogView.isUnreadMessagesCounterHidden()) {
                    audioDialogView.showUnreadMessagesCounter()
                }
            }
        } else if (callType == CallType.VIDEO) {
            runOnUiThread {
                videoDialogView.setUnreadMessagesCount(count)
                if (videoDialogView.isUnreadMessagesCounterHidden()) {
                    videoDialogView.showUnreadMessagesCounter()
                }
            }
        }
    }

    override fun openFile(file: kz.q19.domain.model.file.File) {
        try {
            FileUtil.openFile(this, file.get())
        } catch (e: Exception) {
            e.printStackTrace()
            toast(e.toString())
        }
    }

    override fun openLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun playAudio(path: String, itemPosition: Int) {
        debug(
            TAG,
            "playAudio -> currentAudioPlayingItemPosition: $currentAudioPlayingItemPosition, itemPosition: $itemPosition"
        )

        if (currentAudioPlayingItemPosition == itemPosition) {
            if (mediaPlayer?.isPlaying == true) {
                isAudioPaused = true
                mediaPlayer?.pause()
                chatAdapter?.setAudioPaused(itemPosition)
            } else {
                if (isAudioPlayCompleted) {
                    isAudioPlayCompleted = false
                    chatAdapter?.setAudioProgress(
                        progress = 0,
                        currentPosition = 0,
                        duration = mediaPlayer?.duration ?: 0,
                        itemPosition = itemPosition
                    )
                }

                isAudioPaused = false
                mediaPlayer?.start()
                updateProgress(itemPosition)
            }
            return
        }

        releaseMediaPlayer()
        chatAdapter?.setAudioProgress(
            progress = 0,
            currentPosition = 0,
            duration = 0,
            itemPosition = currentAudioPlayingItemPosition
        )

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()

            mediaPlayer?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            mediaPlayer?.setOnCompletionListener {
                isAudioPlayCompleted = true
            }

            mediaPlayer?.isLooping = false
        }

        try {
            mediaPlayer?.setDataSource(path)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        chatAdapter?.setAudioStartTime(0, itemPosition)
        chatAdapter?.setAudioEndTime(100, itemPosition)

        updateProgress(itemPosition)

        currentAudioPlayingItemPosition = itemPosition
    }

    private fun updateProgress(itemPosition: Int) {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (currentAudioPlayingItemPosition != itemPosition) return
                if (isAudioPaused) return

//                debug(TAG, "mediaPlayer.currentPosition: ${mediaPlayer?.currentPosition}")
//                debug(TAG, "mediaPlayer.duration: ${mediaPlayer?.duration}")
//                debug(TAG, "isAudioPlayCompleted: $isAudioPlayCompleted")
//                debug(TAG, "mediaPlayer.isPlaying: ${mediaPlayer?.isPlaying}")

                val progress = mediaPlayer?.let {
                    (it.currentPosition * 100) / it.duration
                } ?: 0

                if (isAudioPlayCompleted) {
                    chatAdapter?.setAudioProgress(
                        progress = 100,
                        currentPosition = mediaPlayer?.duration ?: 0,
                        duration = mediaPlayer?.duration ?: 0,
                        itemPosition = itemPosition
                    )
                } else {
                    if (mediaPlayer?.isPlaying == true) {
                        chatAdapter?.setAudioProgress(
                            progress = progress,
                            currentPosition = mediaPlayer?.currentPosition ?: 0,
                            duration = mediaPlayer?.duration ?: 0,
                            itemPosition = itemPosition
                        )
                        handler.postDelayed(this, 250)
                    }
                }
            }
        }, 250)
    }

    override fun showFileDownloadStatus(status: kz.q19.domain.model.file.File.DownloadStatus, itemPosition: Int) {
        chatAdapter?.setDownloading(status, itemPosition)
    }

    override fun showFileDownloadProgress(progress: Int, fileType: String, itemPosition: Int) {
        chatAdapter?.setProgress(progress, fileType, itemPosition)
    }

    override fun resolvePermissions(callType: CallType, scope: String?) {
        permissionManager.checkPermission(
            when (callType) {
                CallType.AUDIO -> PermissionManager.Permission.AUDIO_CALL
                CallType.VIDEO -> PermissionManager.Permission.VIDEO_CALL
                else -> return
            }
        ) {
            if (it) {
                presenter.onCallOperator(callType, scope)
            }
        }
    }

    override fun clearMessageInputViewText() {
        footerView.clearInputViewText()
    }

    override fun releaseMediaPlayer() {
        currentAudioPlayingItemPosition = -1
        isAudioPlayCompleted = false
        isAudioPaused = false

        try {
            mediaPlayer?.pause()
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
        } finally {
            mediaPlayer = null
        }
    }

    override fun releaseVideoDialog() {
        videoDialogView.release()
    }

    override fun showFormSentSuccessAlert() {
        showFormSentSuccess {
            formView.clearInputViews()

            presenter.onAppealRegistered()
        }
    }

    override fun scrollToTop() {
        runOnUiThread {
            recyclerView.scrollToPosition(0)
        }
    }

    override fun scrollToBottom() {
        runOnUiThread {
            recyclerView.adapter?.let { adapter ->
                recyclerView.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    override fun showAttachmentPicker(forced: Boolean) {
        permissionManager.checkPermission(PermissionManager.Permission.EXTERNAL_STORAGE) {
            if (it) {
                var isPermitted = footerView.isAttachmentButtonEnabled
                if (forced) {
                    isPermitted = true
                }
                if (isPermitted) {
//                    MaterialFilePicker()
//                        .withActivity(this@OldKenesWidgetActivity)
//                        .withHiddenFiles(true)
//                        .withFilterDirectories(false)
//                        .withCloseMenu(true)
//                        .withRequestCode(PermissionRequestCode.PICK_FILE)
//                        .start()
                } else {
                    showAddAttachmentButtonDisabledAlert {}
                }
            }
        }
    }

    override fun setViewState(viewState: ViewState) {
        debug(TAG, "[renderViewState] -> viewState: $viewState")

        fun hideOtherViews() {
            progressView.hide()

            chatFooterAdapter?.clear()

            headerView.hideHangupButton()

            setDefaultOperatorCallView()
            operatorCallView.isVisible = false
            operatorCallPendingView.isVisible = false

            audioDialogView.setDefaultState()
            audioDialogView.isVisible = false

            videoDialogView.setDefaultState()
            videoDialogView.isVisible = false

            feedbackView.setDefaultState()
            feedbackView.isVisible = false

            formView.isVisible = false

            dynamicFormView.isVisible = false

            recyclerView.isVisible = false

            setDefaultFooterView()
            footerView.isVisible = false
        }

        when (viewState) {
            is ViewState.ChatBot -> {
                runOnUiThread {
                    headerView.hideHangupButton()

                    operatorCallView.isVisible = false
                    operatorCallPendingView.isVisible = false

                    audioDialogView.setDefaultState()
                    audioDialogView.isVisible = false

                    videoDialogView.setDefaultState()
                    videoDialogView.isVisible = false

                    feedbackView.setDefaultState()
                    feedbackView.isVisible = false

                    formView.isVisible = false

                    dynamicFormView.isVisible = false

                    infoView.isVisible = false

//                    contactsView.isVisible = false
                    servicesView.isVisible = false

                    bottomNavigationView.setNavigationButtonsEnabled()
//                    bottomNavigationView.setNavigationButtonActive(BottomNavigationView.NavigationButton.HOME)

                    recyclerView.isVisible = true

                    setDefaultFooterView()
                    footerView.isVisible = true
                }

                when (viewState) {
                    is ViewState.ChatBot.Dashboard -> {
                        runOnUiThread {
                            if (viewState.isLoading) {
                                if (recyclerView.isVisible) {
                                    recyclerView.isVisible = false
                                }

                                if (progressView.isProgressHidden()) {
                                    progressView.show()
                                }
                            } else {
                                if (progressView.isProgressShown()) {
                                    progressView.hide()
                                }

                                if (!recyclerView.isVisible) {
                                    recyclerView.isVisible = true
                                }
                            }
                        }
                    }
                    is ViewState.ChatBot.UserPrompt -> {
                        runOnUiThread {
                            chatAdapter?.clearCategoryMessages()

                            if (viewState.isLoading) {
                                if (recyclerView.isVisible) {
                                    recyclerView.isVisible = false
                                }

                                if (progressView.isProgressHidden()) {
                                    progressView.show()
                                }
                            } else {
                                if (progressView.isProgressShown()) {
                                    progressView.hide()
                                }

                                if (!recyclerView.isVisible) {
                                    recyclerView.isVisible = true
                                }
                            }
                        }
                    }
                }
            }
            is ViewState.TextDialog -> {
                when (viewState) {
                    ViewState.TextDialog.IDLE -> {
                        runOnUiThread {
                            headerView.hideHangupButton()

                            feedbackView.setDefaultState()
                            feedbackView.isVisible = false

                            footerView.disableAttachmentButton()

                            bottomNavigationView.setNavigationButtonsEnabled()
//                            bottomNavigationView.setNavigationButtonActive(BottomNavigationView.NavigationButton.HOME)
                        }
                    }
                    ViewState.TextDialog.Pending -> {
                        runOnUiThread {
                            headerView.showHangupButton()

                            chatFooterAdapter?.clear()
                        }
                    }
                    ViewState.TextDialog.Live -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()

                            headerView.showHangupButton()

                            footerView.enableAttachmentButton()

                            bottomNavigationView.setNavigationButtonsDisabled()
                        }
                    }
                    ViewState.TextDialog.CallAgentDisconnected -> {
                        runOnUiThread {
                            footerView.disableAttachmentButton()

                            bottomNavigationView.setNavigationButtonsEnabled()
                        }
                    }
                    ViewState.TextDialog.UserDisconnected -> {
                        runOnUiThread {
                            headerView.hideHangupButton()

                            footerView.disableAttachmentButton()

                            bottomNavigationView.setNavigationButtonsEnabled()
                        }
                    }
                    is ViewState.TextDialog.UserFeedback -> {
                        runOnUiThread {
                            headerView.hideHangupButton()

                            if (viewState.isFeedbackSent) {
                                chatFooterAdapter?.showGoToHomeButton()

                                feedbackView.isVisible = false

                                footerView.isVisible = true

                                recyclerView.isVisible = true

                                bottomNavigationView.setNavigationButtonsEnabled()
                            } else {
//                                hideKeyboard(footerView.inputView)

                                recyclerView.isVisible = false

                                footerView.isVisible = false

                                bottomNavigationView.setNavigationButtonsDisabled()

                                feedbackView.isVisible = true
                            }
                        }
                    }
                }
            }
            is ViewState.CallAgentCall -> {
                runOnUiThread {
                    progressView.hide()

                    infoView.isVisible = false

//                    contactsView.isVisible = false
                    servicesView.isVisible = false

                    chatFooterAdapter?.clear()

                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                    headerView.hideHangupButton()

                    audioDialogView.setDefaultState()
                    audioDialogView.isVisible = false

                    videoDialogView.setDefaultState()
                    videoDialogView.isVisible = false

                    feedbackView.setDefaultState()
                    feedbackView.isVisible = false

                    recyclerView.isVisible = false

//                    footerView.setGoToActiveDialogButtonState(null)
                    footerView.disableAttachmentButton()
                    footerView.isVisible = false

                    bottomNavigationView.setNavigationButtonsEnabled()
//                    bottomNavigationView.setNavigationButtonActive(BottomNavigationView.NavigationButton.CALLS)

                    setDefaultOperatorCallView()
                    operatorCallView.isVisible = true
                }
            }
            is ViewState.AudioDialog -> {
                runOnUiThread {
                    infoView.isVisible = false

//                    contactsView.isVisible = false
                    servicesView.isVisible = false
                }

                when (viewState) {
                    ViewState.AudioDialog.Pending -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()

                            operatorCallView.setCallButtonDisabled(CallType.AUDIO)
                            operatorCallPendingView.setCallTypeViewText(getString(R.string.kenes_audio_call))
                            operatorCallPendingView.showProgress()
                            operatorCallPendingView.setCancelCallButtonEnabled()
                            operatorCallPendingView.isVisible = true
                        }
                    }
                    ViewState.AudioDialog.Start -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()

                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

//                            hideKeyboard(footerView.inputView)

                            headerView.showHangupButton()

                            operatorCallView.setCallButtonDisabled(CallType.AUDIO)
                            operatorCallView.isVisible = false
                            operatorCallPendingView.isVisible = false

                            recyclerView.isVisible = true

                            footerView.enableAttachmentButton()
                            footerView.isVisible = true

                            bottomNavigationView.setNavigationButtonsDisabled()
                        }
                    }
                    ViewState.AudioDialog.Preparation -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()
                        }
                    }
                    ViewState.AudioDialog.Ready -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()
                        }
                    }
                    is ViewState.AudioDialog.Live -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()

                            footerView.enableAttachmentButton()

                            audioDialogView.setUnreadMessagesCount("0")
                            audioDialogView.hideUnreadMessagesCounter()

                            if (viewState.isDialogScreenShown) {
//                                hideKeyboard(footerView.inputView)

//                                footerView.setGoToActiveDialogButtonState(null)

                                audioDialogView.isVisible = true
                            } else {
//                                footerView.setGoToActiveDialogButtonState(R.string.kenes_return_to_audio_call)

                                audioDialogView.isVisible = false
                            }
                        }
                    }
                    ViewState.AudioDialog.CallAgentDisconnected -> {
                        runOnUiThread {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                            setDefaultFooterView()
                            footerView.disableAttachmentButton()
                            footerView.isVisible = false

                            audioDialogView.isVisible = false

                            bottomNavigationView.setNavigationButtonsEnabled()
                        }
                    }
                    ViewState.AudioDialog.UserDisconnected -> {
                        runOnUiThread {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                            headerView.hideHangupButton()

                            setDefaultFooterView()
                            footerView.disableAttachmentButton()
                            footerView.isVisible = false

                            audioDialogView.isVisible = false

                            bottomNavigationView.setNavigationButtonsEnabled()
                        }
                    }
                    is ViewState.AudioDialog.UserFeedback -> {
                        runOnUiThread {
                            headerView.hideHangupButton()

                            if (viewState.isFeedbackSent) {
                                chatFooterAdapter?.showGoToHomeButton()

                                feedbackView.isVisible = false

                                bottomNavigationView.setNavigationButtonsEnabled()

                                recyclerView.isVisible = true
                            } else {
//                                hideKeyboard(footerView.inputView)

                                operatorCallView.isVisible = false
                                operatorCallPendingView.isVisible = false

                                recyclerView.isVisible = false

                                bottomNavigationView.setNavigationButtonsDisabled()

                                feedbackView.isVisible = true
                            }
                        }
                    }
                }
            }
            is ViewState.VideoDialog -> {
                runOnUiThread {
                    infoView.isVisible = false

//                    contactsView.isVisible = false
                    servicesView.isVisible = false

                    operatorCallPendingView.isVisible = false
                }

                when (viewState) {
                    ViewState.VideoDialog.Pending -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()

                            operatorCallView.setCallButtonDisabled(CallType.VIDEO)
                            operatorCallPendingView.setCallTypeViewText(getString(R.string.kenes_video_call))
                            operatorCallPendingView.showProgress()
                            operatorCallPendingView.setCancelCallButtonEnabled()
                            operatorCallPendingView.isVisible = true
                        }
                    }
                    ViewState.VideoDialog.Start -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()

                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

//                            hideKeyboard(footerView.inputView)

                            headerView.showHangupButton()

                            setDefaultOperatorCallView()
                            operatorCallView.isVisible = false
                            operatorCallPendingView.isVisible = false

                            recyclerView.isVisible = true

                            footerView.enableAttachmentButton()
                            footerView.isVisible = true

                            bottomNavigationView.setNavigationButtonsDisabled()
                        }
                    }
                    ViewState.VideoDialog.Preparation -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()
                        }
                    }
                    ViewState.VideoDialog.Ready -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()
                        }
                    }
                    is ViewState.VideoDialog.Live -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()

                            footerView.enableAttachmentButton()

                            videoDialogView.setUnreadMessagesCount("0")
                            videoDialogView.hideUnreadMessagesCounter()

                            if (viewState.isDialogScreenShown) {
//                                hideKeyboard(footerView.inputView)

//                                footerView.setGoToActiveDialogButtonState(null)

                                videoDialogView.showControlButtons()
                                videoDialogView.isVisible = true
                            } else {
//                                footerView.setGoToActiveDialogButtonState(R.string.kenes_return_to_video_call)

                                videoDialogView.isVisible = false
                            }
                        }
                    }
                    is ViewState.VideoDialog.CallAgentDisconnected -> {
                        runOnUiThread {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                            setDefaultFooterView()
                            footerView.isVisible = false

                            footerView.disableAttachmentButton()

                            videoDialogView.isVisible = false

                            bottomNavigationView.setNavigationButtonsEnabled()
                        }
                    }
                    is ViewState.VideoDialog.UserDisconnected -> {
                        runOnUiThread {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                            headerView.hideHangupButton()

                            setDefaultFooterView()
                            footerView.isVisible = false

                            footerView.disableAttachmentButton()

                            videoDialogView.isVisible = false

                            bottomNavigationView.setNavigationButtonsEnabled()
                        }
                    }
                    is ViewState.VideoDialog.UserFeedback -> {
                        runOnUiThread {
                            headerView.hideHangupButton()

                            if (viewState.isFeedbackSent) {
                                chatFooterAdapter?.showGoToHomeButton()

                                feedbackView.isVisible = false

                                recyclerView.isVisible = true

                                bottomNavigationView.setNavigationButtonsEnabled()
                            } else {
//                                hideKeyboard(footerView.inputView)

                                operatorCallView.isVisible = false
                                operatorCallPendingView.isVisible = false

                                recyclerView.isVisible = false

                                bottomNavigationView.setNavigationButtonsDisabled()

                                feedbackView.isVisible = true
                            }
                        }
                    }
                }
            }
            ViewState.Form -> {
                runOnUiThread {
                    recyclerView.isVisible = false

                    footerView.isVisible = false

                    bottomNavigationView.setNavigationButtonsDisabled()

                    formView.isVisible = true
                }
            }
            ViewState.DynamicForm -> {
                Handler(Looper.getMainLooper())
                    .postDelayed(
                    {
                        servicesView.isVisible = false

                        recyclerView.isVisible = false

                        footerView.isVisible = false

                        bottomNavigationView.setNavigationButtonsDisabled()

                        dynamicFormView.isVisible = true
                    },
                    350
                )
            }
//            ViewState.Contacts -> {
//                runOnUiThread {
//                    hideOtherViews()
//
//                    infoView.isVisible = false
//
//                    bottomNavigationView.setNavigationButtonActive(BottomNavigationView.NavigationButton.SERVICES)
//
//                    contactsView.isVisible = true
//                }
//            }
            is ViewState.Services -> {
                when (viewState) {
                    ViewState.Services.IDLE -> {
                        runOnUiThread {
                            hideOtherViews()

                            infoView.isVisible = false

                            bottomNavigationView.setNavigationButtonsEnabled()
//                            bottomNavigationView.setNavigationButtonActive(BottomNavigationView.NavigationButton.SERVICES)

                            servicesView.isVisible = true
                        }
                    }
                    ViewState.Services.Process -> {
                        runOnUiThread {
                            dynamicFormView.isVisible = false

                            servicesView.isVisible = false

                            recyclerView.isVisible = true

                            footerView.enableAttachmentButton()
                            footerView.isVisible = true
                        }
                    }
                    ViewState.Services.Cancelled, ViewState.Services.Pending, ViewState.Services.Completed -> {
                        runOnUiThread {
                            dynamicFormView.isVisible = false

                            servicesView.isVisible = false

                            recyclerView.isVisible = true

                            footerView.enableAttachmentButton()
                            footerView.isVisible = true

                            bottomNavigationView.setNavigationButtonsEnabled()
                        }
                    }
                }
            }
            ViewState.Info -> {
                runOnUiThread {
                    hideOtherViews()

//                    contactsView.isVisible = false
                    servicesView.isVisible = false

//                    bottomNavigationView.setNavigationButtonActive(BottomNavigationView.NavigationButton.INFO)

                    infoView.isVisible = true
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        footerView.disableAttachmentButton()

        presenter.detachView()

        presenter.onCloseLiveCall()

        permissionManager.removeAllListeners()

//        headerView = null

        setDefaultOperatorCallView()
//        operatorCallView = null

        audioDialogView.setDefaultState()
        audioDialogView.callback = null
//        audioDialogView = null

        videoDialogView.setDefaultState()
        videoDialogView.callback = null
//        videoDialogView = null

        formView.clear()

        setDefaultFooterView()
//        footerView = null

        chatAdapter?.callback = null
        chatAdapter?.clear()

        chatFooterAdapter?.callback = null
        chatFooterAdapter?.clear()

        chatAdapter?.let { concatAdapter?.removeAdapter(it) }

        chatFooterAdapter?.let { concatAdapter?.removeAdapter(it) }

        concatAdapter = null

        try {
            chatAdapter?.unregisterAdapterDataObserver(chatAdapterDataObserver)
            chatFooterAdapter?.unregisterAdapterDataObserver(chatFooterAdapterDataObserver)
        } catch (e: IllegalStateException) {
//            e.printStackTrace()
        }

        chatAdapter = null

        chatFooterAdapter = null

        recyclerView.adapter = null
//        recyclerView = null

//        progressView = null

        bottomNavigationView.callback = null

//        rootView = null
    }

    private fun throwError() {
        toast(R.string.kenes_error_invalid_hostname)
        finish()
    }

}