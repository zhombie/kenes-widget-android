package q19.kenes_widget.ui.presentation

import android.annotation.SuppressLint
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
import android.os.Parcelable
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nbsp.materialfilepicker.MaterialFilePicker
import com.nbsp.materialfilepicker.ui.FilePickerActivity
import com.squareup.picasso.Picasso
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import q19.kenes_widget.PermissionManager
import q19.kenes_widget.R
import q19.kenes_widget.adapter.ChatAdapter
import q19.kenes_widget.adapter.ChatAdapterItemDecoration
import q19.kenes_widget.adapter.ChatFooterAdapter
import q19.kenes_widget.core.locale.LocalizationActivity
import q19.kenes_widget.model.*
import q19.kenes_widget.ui.components.*
import q19.kenes_widget.util.*
import q19.kenes_widget.util.FileUtil.openFile
import q19.kenes_widget.util.Logger.debug
import q19.kenes_widget.webrtc.PeerConnectionClient
import java.io.File

internal class KenesWidgetV2Activity : LocalizationActivity(), KenesWidgetV2View {

    companion object {
        private const val TAG = "KenesWidgetV2Activity"

        private const val KEY_HOSTNAME = "hostname"

        private const val FILE_PICKER_REQUEST_CODE = 101

        @JvmStatic
        fun newIntent(context: Context, hostname: String): Intent {
            return Intent(context, KenesWidgetV2Activity::class.java)
                .putExtra(KEY_HOSTNAME, hostname)
        }
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
     * Video call [videoCallView] screen view.
     */
    private val videoCallView by bind<VideoCallView>(R.id.videoCallView)

    /**
     * Audio call [audioCallView] screen view.
     */
    private val audioCallView by bind<AudioCallView>(R.id.audioCallView)

    /**
     * Info screen view [infoView] with extra information & contacts.
     */
    private val infoView by bind<InfoView>(R.id.infoView)

    /**
     * Footer view [footerView] for messenger.
     */
    private val footerView by bind<FooterView>(R.id.footerView)

    /**
     * View [recyclerView] for chat.
     */
    private val recyclerView by bind<RecyclerView>(R.id.recyclerView)

    /**
     * User feedback view [feedbackView] after dialog view.
     */
    private val feedbackView by bind<FeedbackView>(R.id.feedbackView)

    private val formView by bind<FormView>(R.id.formView)

//    private val dynamicFormView by bind<DynamicFormView>(R.id.dynamicFormView)

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

    private val contactsView by bind<ContactsView>(R.id.contactsView)

    // ------------------------------------------------------------------------

    private val palette by lazy {
        try {
            resources.getIntArray(R.array.kenes_palette)
        } catch (e: Exception) {
            e.printStackTrace()
            intArrayOf()
        }
    }

    // ------------------------------------------------------------------------

    private var concatAdapter: ConcatAdapter? = null
    private var chatAdapter: ChatAdapter? = null
    private var chatFooterAdapter: ChatFooterAdapter? = null

    // ------------------------------------------------------------------------

    private var peerConnectionClient: PeerConnectionClient? = null

    // ------------------------------------------------------------------------

    private val permissionManager by lazy { PermissionManager(this) }

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

    private lateinit var presenter: KenesWidgetV2Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kenes_activity_widget_v2)

        val hostname = intent.getStringExtra(KEY_HOSTNAME)

        if (hostname.isNullOrBlank()) {
            throwError()
        } else {
            UrlUtil.setHostname(hostname)
        }

        // ------------------------------------------------------------------------

        presenter = KenesWidgetV2Presenter(
            language = Language.from(getCurrentLanguage()),
            palette = palette
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
            override fun onNavButtonClicked(bottomNavigation: BottomNavigation) {
                presenter.onNavButtonClicked(bottomNavigation)
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

        videoCallView.setOnCallClickListener {
            presenter.onCallOperatorClicked(OperatorCall.VIDEO)
        }

        videoCallView.setOnCancelCallClickListener {
            presenter.onCallCancelClicked(OperatorCall.VIDEO)
        }

        audioCallView.setOnCallClickListener {
            presenter.onCallOperatorClicked(OperatorCall.AUDIO)
        }

        contactsView.callback = object : ContactsView.Callback {
            override fun onInfoBlockItemClicked(item: Configs.Item) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.action))
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
        }

        formView.callback = object : FormView.Callback {
            override fun onCancelClicked() {
                presenter.onFormCancelClicked()
            }

            override fun onSendClicked(name: String, email: String, phone: String) {
                presenter.onFormSendClicked(name, email, phone)
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
            val rec = Rect()
            rootView.getWindowVisibleDisplayFrame(rec)

            // finding screen height
            val screenHeight = rootView.rootView?.height ?: 0

            // finding keyboard height
            val keypadHeight = screenHeight - rec.bottom

            if (keypadHeight > screenHeight * 0.15) {
                bottomNavigationView.hideBottomNavigationView()
            } else {
                bottomNavigationView.showBottomNavigationView()
            }
        }

        footerView.callback = object : FooterView.Callback {
            override fun onGoToActiveDialogButtonClicked() {
                presenter.onGoToActiveDialogButtonClicked()
            }

            override fun onAddAttachmentButtonClicked() {
                presenter.onAddAttachmentButtonClicked()
            }

            override fun onInputViewFocusChangeListener(v: View, hasFocus: Boolean) {
                if (!hasFocus) hideKeyboard(v)
            }

            override fun onInputViewClicked() {
            }

            override fun onSendMessageButtonClicked(message: String) {
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
                presenter.onGoToChatButtonClicked(OperatorCall.VIDEO)
            }

            override fun onHangupButtonClicked() {
                presenter.onHangupButtonClicked()
            }

            override fun onSwitchSourceButtonClicked() {
                peerConnectionClient?.onSwitchCamera()
            }

            override fun onSwitchScalingButtonClicked() {
                peerConnectionClient?.switchScalingType()
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
                presenter.onGoToChatButtonClicked(OperatorCall.AUDIO)
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

            override fun onSocialClicked(contact: Configs.Contact) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contact.url))
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }

            override fun onLanguageChangeClicked(language: Language) {
                val supportedLanguages = Language.getSupportedLanguages()
                val items = supportedLanguages.map { it.value }.toTypedArray()
                showLanguageSelectionAlert(items) { which ->
                    val selected = supportedLanguages[which]

                    presenter.onLanguageSelected(selected.key)

                    setLanguage(selected.locale)
                }
            }
        }

        peerConnectionClient = PeerConnectionClient()

        setupRecyclerView()

        setupKeyboardBehavior()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(object : ChatAdapter.Callback {
            override fun onShowAllCategoryChildClicked(category: Category) {
                presenter.onShowAllCategoryChildClicked(category)
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
                imageView.showFullscreenImage(imageUrl)
            }

            override fun onImageClicked(imageView: ImageView, bitmap: Bitmap) {
                imageView.showFullscreenImage(bitmap)
            }

            override fun onImageLoadCompleted() {
            }

            override fun onStopTrackingTouch(progress: Int, itemPosition: Int) {
                debug(TAG, "onStopTrackingTouch -> progress: $progress, itemPosition: $itemPosition")

                if (currentAudioPlayingItemPosition == itemPosition) {
                    isAudioPaused = false
                    isAudioPlayCompleted = false
                    val milliseconds = (progress * (mediaPlayer?.duration ?: 1)) / 100
                    mediaPlayer?.seekTo(milliseconds)
                    mediaPlayer?.start()
                    updateProgress(itemPosition)
                }
            }

            override fun onMediaClicked(media: Media, itemPosition: Int) {
                debug(TAG, "onMediaClicked: $media, itemPosition: $itemPosition")

                val file = media.getFile(this@KenesWidgetV2Activity)
                presenter.onMediaClicked(media, file, itemPosition)
            }

            override fun onAttachmentClicked(attachment: Attachment, itemPosition: Int) {
                debug(TAG, "onAttachmentClicked: $attachment, itemPosition: $itemPosition")

                val file = attachment.getFile(this@KenesWidgetV2Activity)
                presenter.onAttachmentClicked(attachment, file, itemPosition)
            }
        })

        recyclerView.setOverscrollColor(R.color.kenes_light_blue)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.isNestedScrollingEnabled = false

        chatAdapter?.registerAdapterDataObserver(chatAdapterDataObserver)
        chatFooterAdapter?.registerAdapterDataObserver(chatFooterAdapterDataObserver)

        chatFooterAdapter = ChatFooterAdapter()
        chatFooterAdapter?.callback = object : ChatFooterAdapter.Callback {
            override fun onGoToHomeClicked() {
                presenter.onGoToHomeClicked()
            }

            override fun onSwitchToCallAgentClicked() {
                presenter.onCallOperator(OperatorCall.TEXT)
            }

            override fun onRegisterAppealClicked() {
                presenter.onRegisterAppealClicked()
            }
        }

        concatAdapter = ConcatAdapter(chatAdapter, chatFooterAdapter)

        recyclerView.adapter = concatAdapter
        recyclerView.itemAnimator = null
        recyclerView.addItemDecoration(ChatAdapterItemDecoration(this))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupKeyboardBehavior() {
        recyclerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                hideKeyboard()
            }
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val filePath = data?.getStringExtra(FilePickerActivity.RESULT_FILE_PATH) ?: return

            presenter.onUploadFile(filePath)
        }
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

    override fun showContacts(contacts: List<Configs.Contact>) {
        runOnUiThread {
            infoView.setContacts(contacts)
        }
    }

    override fun showPhones(phones: List<String>) {
        runOnUiThread {
            infoView.setPhones(phones)
        }
    }

    override fun showInfoBlocks(infoBlocks: List<Configs.InfoBlock>) {
        runOnUiThread {
            contactsView.show(infoBlocks, Language.from(getCurrentLanguage()))
        }
    }

    override fun showOpponentInfo(opponent: Configs.Opponent) {
        runOnUiThread {
            headerView.setOpponentInfo(opponent)
        }
    }

    override fun showOpponentInfo(name: String, photoUrl: String?) {
        runOnUiThread {
            headerView.setOpponentInfo(
                Configs.Opponent(
                    name = name,
                    secondName = getString(R.string.kenes_operator),
                    avatarUrl = photoUrl
                )
            )
        }
    }

    override fun showNavButton(bottomNavigation: BottomNavigation) {
        runOnUiThread {
            bottomNavigationView.showNavButton(bottomNavigation)
        }
    }

    override fun hideNavButton(bottomNavigation: BottomNavigation) {
        runOnUiThread {
            bottomNavigationView.hideNavButton(bottomNavigation)
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

            footerView.setGoToActiveDialogButtonState(null)

            footerView.disableSendMessageButton()
        }
    }

    override fun setDefaultAudioCallView() {
        runOnUiThread {
            audioCallView.setCallButtonEnabled()

            audioCallView.setInfoViewText(null)
            audioCallView.hideInfoViewText()

            audioCallView.setPendingQueueCountViewText(null)
            audioCallView.hidePendingQueueCountView()
        }
    }

    override fun setDefaultVideoCallView() {
        runOnUiThread {
            videoCallView.setCallButtonEnabled()

            videoCallView.hideCancelCallButton()

            videoCallView.setInfoViewText(null)
            videoCallView.hideInfoViewText()

            videoCallView.setPendingQueueCountViewText(null)
            videoCallView.hidePendingQueueCountView()
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
                Message(
                    type = Message.Type.NOTIFICATION,
                    text = getString(R.string.kenes_user_disconnected)
                )
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

    override fun showFeedback(text: String, ratingButtons: List<RatingButton>) {
        runOnUiThread {
            feedbackView.setTitle(text)
            feedbackView.setRatingButtons(ratingButtons)
            feedbackView.setOnRateButtonClickListener { ratingButton ->
                presenter.onRateButtonClicked(ratingButton)
            }
        }
    }

    override fun setAudioCallInfoText(text: String) {
        runOnUiThread {
            audioCallView.setInfoViewText(text)
            audioCallView.showInfoViewText()
        }
    }

    override fun setAudioCallPendingQueueCount(count: Int) {
        runOnUiThread {
            audioCallView.setPendingQueueCountViewText(getString(R.string.kenes_queue_count, count))
            audioCallView.showPendingQueueCountView()
        }
    }

    override fun setVideoCallInfoText(text: String) {
        runOnUiThread {
            videoCallView.setInfoViewText(text)
            videoCallView.showInfoViewText()
        }
    }

    override fun setVideoCallPendingQueueCount(count: Int) {
        runOnUiThread {
            videoCallView.setPendingQueueCountViewText(getString(R.string.kenes_queue_count, count))
            videoCallView.showPendingQueueCountView()
        }
    }

    override fun showHangupConfirmationAlert() {
        runOnUiThread {
            showHangupConfirmAlert {
                presenter.onHangupLiveCall()
            }
        }
    }

    override fun showAlreadyCallingAlert(bottomNavigation: BottomNavigation) {
        runOnUiThread {
            showAlreadyCallingAlert {
                presenter.onCancelPendingCallClicked(bottomNavigation)
            }
        }
    }

    override fun showAlreadyCallingAlert(operatorCall: OperatorCall) {
        runOnUiThread {
            showAlreadyCallingAlert {
                presenter.onCallCancelClicked(operatorCall)
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
            if (footerView.visibility != View.VISIBLE) {
                footerView.visibility = View.VISIBLE
            }
        }
    }

    override fun setUnreadMessagesCountOnCall(operatorCall: OperatorCall, count: String) {
        if (operatorCall == OperatorCall.AUDIO) {
            runOnUiThread {
                audioDialogView.setUnreadMessagesCount(count)
                if (audioDialogView.isUnreadMessagesCounterHidden()) {
                    audioDialogView.showUnreadMessagesCounter()
                }
            }
        } else if (operatorCall == OperatorCall.VIDEO) {
            runOnUiThread {
                videoDialogView.setUnreadMessagesCount(count)
                if (videoDialogView.isUnreadMessagesCounterHidden()) {
                    videoDialogView.showUnreadMessagesCounter()
                }
            }
        }
    }

    override fun openFile(file: File) {
        try {
            file.openFile(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
            toast("")
        }
    }

    override fun playAudio(path: String, itemPosition: Int) {
        debug(TAG, "playAudio -> currentAudioPlayingItemPosition: $currentAudioPlayingItemPosition, itemPosition: $itemPosition")

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

    override fun showFileDownloadStatus(status: Message.File.DownloadStatus, itemPosition: Int) {
        chatAdapter?.setDownloading(status, itemPosition)
    }

    override fun showFileDownloadProgress(progress: Int, fileType: String, itemPosition: Int) {
        chatAdapter?.setProgress(progress, fileType, itemPosition)
    }

    override fun resolvePermissions(operatorCall: OperatorCall) {
        permissionManager.checkPermission(
            when (operatorCall) {
                OperatorCall.AUDIO -> PermissionManager.Permission.AUDIO_CALL
                OperatorCall.VIDEO -> PermissionManager.Permission.VIDEO_CALL
                else -> return
            }
        ) {
            if (it) {
                presenter.onCallOperator(operatorCall)
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

    override fun releasePeerConnection() {
        peerConnectionClient?.dispose()
    }

    override fun showFormSentSuccessAlert() {
        showFormSentSuccess {
            formView.clearInputViews()

            presenter.onAppealRegistered()
        }
    }

    override fun createPeerConnection(
        isMicrophoneEnabled: Boolean,
        isCameraEnabled: Boolean,
        iceServers: List<PeerConnection.IceServer>
    ) {
        peerConnectionClient?.createPeerConnection(
            activity = this@KenesWidgetV2Activity,
            isMicrophoneEnabled = isMicrophoneEnabled,
            isCameraEnabled = isCameraEnabled,
            iceServers = iceServers,
            listener = peerConnectionClientListener
        )
    }

    override fun initLocalVideoStream() {
        peerConnectionClient?.initLocalCameraStream(videoDialogView.localSurfaceView)
    }

    override fun startLocalMediaStream() {
        peerConnectionClient?.addLocalStreamToPeer()
    }

    override fun sendOfferToOpponent() {
        peerConnectionClient?.createOffer()
    }

    override fun setRemoteDescription(sessionDescription: SessionDescription) {
        peerConnectionClient?.setRemoteDescription(sessionDescription)
    }

    override fun sendAnswerToOpponent() {
        peerConnectionClient?.createAnswer()
    }

    override fun addRemoteIceCandidate(iceCandidate: IceCandidate) {
        peerConnectionClient?.addRemoteIceCandidate(iceCandidate)
    }

    private val peerConnectionClientListener = object : PeerConnectionClient.Listener {
        override fun onIceCandidate(iceCandidate: IceCandidate) {
            presenter.onIceCandidate(iceCandidate)
        }

        override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
            presenter.onIceConnectionChange(iceConnectionState)
        }

        override fun onRenegotiationNeeded() {
//            if (dialog.isInitiator) {
//                peerConnectionClient?.createOffer()
//            } else {
//                peerConnectionClient?.createAnswer()
//            }
        }

        override fun onLocalDescription(sessionDescription: SessionDescription) {
            presenter.onLocalDescription(sessionDescription)
        }

        override fun onAddRemoteStream(mediaStream: MediaStream) {
            peerConnectionClient?.initRemoteCameraStream(videoDialogView.remoteSurfaceView)
            peerConnectionClient?.addRemoteStreamToPeer(mediaStream)
        }

        override fun onPeerConnectionError(errorMessage: String) {
        }

        override fun onRemoteScreenScaleChanged(isFilled: Boolean) {
            videoDialogView.setSwitchScaleIcon(isFilled)
        }
    }

    override fun scrollToBottom() {
        recyclerView.adapter?.let { adapter ->
            recyclerView.scrollToPosition(adapter.itemCount - 1)
        }
    }

    override fun showAttachmentPicker() {
        permissionManager.checkPermission(PermissionManager.Permission.EXTERNAL_STORAGE) {
            if (it) {
                if (footerView.isAttachmentButtonEnabled) {
                    MaterialFilePicker()
                        .withActivity(this@KenesWidgetV2Activity)
                        .withHiddenFiles(true)
                        .withFilterDirectories(false)
                        .withCloseMenu(true)
                        .withRequestCode(FILE_PICKER_REQUEST_CODE)
                        .start()
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

            setDefaultAudioCallView()
            audioCallView.visibility = View.GONE

            setDefaultVideoCallView()
            videoCallView.visibility = View.GONE

            audioDialogView.setDefaultState()
            audioDialogView.visibility = View.GONE

            videoDialogView.setDefaultState()
            videoDialogView.visibility = View.GONE

            feedbackView.setDefaultState()
            feedbackView.visibility = View.GONE

            recyclerView.visibility = View.GONE

            setDefaultFooterView()
            footerView.visibility = View.GONE
        }

        when (viewState) {
            is ViewState.ChatBot -> {
                runOnUiThread {
                    headerView.hideHangupButton()

                    setDefaultAudioCallView()
                    audioCallView.visibility = View.GONE

                    setDefaultVideoCallView()
                    videoCallView.visibility = View.GONE

                    audioDialogView.setDefaultState()
                    audioDialogView.visibility = View.GONE

                    videoDialogView.setDefaultState()
                    videoDialogView.visibility = View.GONE

                    feedbackView.setDefaultState()
                    feedbackView.visibility = View.GONE

                    formView.visibility = View.GONE

                    infoView.visibility = View.GONE

                    contactsView.visibility = View.GONE

                    bottomNavigationView.setNavButtonsEnabled()
                    bottomNavigationView.setNavButtonActive(BottomNavigation.HOME)

                    recyclerView.visibility = View.VISIBLE

                    setDefaultFooterView()
                    footerView.visibility = View.VISIBLE
                }

                when (viewState) {
                    is ViewState.ChatBot.Categories -> {
                        runOnUiThread {
                            if (viewState.isLoading) {
                                if (recyclerView.visibility != View.GONE) {
                                    recyclerView.visibility = View.GONE
                                }

                                if (progressView.isProgressHidden()) {
                                    progressView.show()
                                }
                            } else {
                                if (progressView.isProgressShown()) {
                                    progressView.hide()
                                }

                                if (recyclerView.visibility != View.VISIBLE) {
                                    recyclerView.visibility = View.VISIBLE
                                }
                            }
                        }
                    }
                    is ViewState.ChatBot.UserPrompt -> {
                        runOnUiThread {
                            chatAdapter?.clearCategoryMessages()

                            if (viewState.isLoading) {
                                if (recyclerView.visibility != View.GONE) {
                                    recyclerView.visibility = View.GONE
                                }

                                if (progressView.isProgressHidden()) {
                                    progressView.show()
                                }
                            } else {
                                if (progressView.isProgressShown()) {
                                    progressView.hide()
                                }

                                if (recyclerView.visibility != View.VISIBLE) {
                                    recyclerView.visibility = View.VISIBLE
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
                            feedbackView.visibility = View.GONE

                            footerView.disableAttachmentButton()

                            bottomNavigationView.setNavButtonsEnabled()
                            bottomNavigationView.setNavButtonActive(BottomNavigation.HOME)
                        }
                    }
                    ViewState.TextDialog.Pending -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()
                        }
                    }
                    ViewState.TextDialog.Live -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()

                            headerView.showHangupButton()

                            footerView.enableAttachmentButton()

                            bottomNavigationView.setNavButtonsDisabled()
                        }
                    }
                    ViewState.TextDialog.UserDisconnected, ViewState.TextDialog.CallAgentDisconnected -> {
                        runOnUiThread {
                            headerView.hideHangupButton()

                            footerView.disableAttachmentButton()

                            bottomNavigationView.setNavButtonsEnabled()
                        }
                    }
                    is ViewState.TextDialog.UserFeedback -> {
                        runOnUiThread {
                            if (viewState.isFeedbackSent) {
                                chatFooterAdapter?.showGoToHomeButton()

                                feedbackView.visibility = View.GONE

                                footerView.visibility = View.VISIBLE

                                recyclerView.visibility = View.VISIBLE

                                bottomNavigationView.setNavButtonsEnabled()
                            } else {
                                hideKeyboard(footerView.inputView)

                                recyclerView.visibility = View.GONE

                                footerView.visibility = View.GONE

                                bottomNavigationView.setNavButtonsDisabled()

                                feedbackView.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
            is ViewState.AudioDialog -> {
                runOnUiThread {
                    infoView.visibility = View.GONE

                    contactsView.visibility = View.GONE

                    videoCallView.visibility = View.GONE
                }

                when (viewState) {
                    ViewState.AudioDialog.IDLE -> {
                        runOnUiThread {
                            progressView.hide()

                            chatFooterAdapter?.clear()

                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                            headerView.hideHangupButton()

                            audioDialogView.setDefaultState()
                            audioDialogView.visibility = View.GONE

                            feedbackView.setDefaultState()
                            feedbackView.visibility = View.GONE

                            recyclerView.visibility = View.GONE

                            footerView.setGoToActiveDialogButtonState(null)
                            footerView.disableAttachmentButton()
                            footerView.visibility = View.GONE

                            bottomNavigationView.setNavButtonsEnabled()
                            bottomNavigationView.setNavButtonActive(BottomNavigation.AUDIO)

                            setDefaultAudioCallView()
                            audioCallView.visibility = View.VISIBLE
                        }
                    }
                    ViewState.AudioDialog.Pending -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()

                            audioCallView.setCallButtonDisabled()
                        }
                    }
                    ViewState.AudioDialog.Start -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()

                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                            hideKeyboard(footerView.inputView)

                            headerView.showHangupButton()

                            audioCallView.setCallButtonDisabled()
                            audioCallView.visibility = View.GONE

                            recyclerView.visibility = View.VISIBLE

                            footerView.enableAttachmentButton()
                            footerView.visibility = View.VISIBLE

                            bottomNavigationView.setNavButtonsDisabled()
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
                                hideKeyboard(footerView.inputView)

                                footerView.setGoToActiveDialogButtonState(null)

                                audioDialogView.visibility = View.VISIBLE
                            } else {
                                footerView.setGoToActiveDialogButtonState(R.string.kenes_return_to_audio_call)

                                audioDialogView.visibility = View.GONE
                            }
                        }
                    }
                    ViewState.AudioDialog.UserDisconnected, ViewState.AudioDialog.CallAgentDisconnected -> {
                        runOnUiThread {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                            headerView.hideHangupButton()

                            setDefaultFooterView()
                            footerView.disableAttachmentButton()
                            footerView.visibility = View.GONE

                            audioDialogView.visibility = View.GONE

                            bottomNavigationView.setNavButtonsEnabled()
                        }
                    }
                    is ViewState.AudioDialog.UserFeedback -> {
                        runOnUiThread {
                            if (viewState.isFeedbackSent) {
                                chatFooterAdapter?.showGoToHomeButton()

                                feedbackView.visibility = View.GONE

                                bottomNavigationView.setNavButtonsEnabled()

                                recyclerView.visibility = View.VISIBLE
                            } else {
                                hideKeyboard(footerView.inputView)

                                audioCallView.visibility = View.GONE

                                recyclerView.visibility = View.GONE

                                bottomNavigationView.setNavButtonsDisabled()

                                feedbackView.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
            is ViewState.VideoDialog -> {
                runOnUiThread {
                    infoView.visibility = View.GONE

                    contactsView.visibility = View.GONE

                    audioCallView.visibility = View.GONE
                }

                when (viewState) {
                    ViewState.VideoDialog.IDLE -> {
                        runOnUiThread {
                            progressView.hide()

                            chatFooterAdapter?.clear()

                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                            headerView.hideHangupButton()

                            videoDialogView.setDefaultState()
                            videoDialogView.visibility = View.GONE

                            feedbackView.setDefaultState()
                            feedbackView.visibility = View.GONE

                            recyclerView.visibility = View.GONE

                            footerView.setGoToActiveDialogButtonState(null)
                            footerView.disableAttachmentButton()
                            footerView.visibility = View.GONE

                            bottomNavigationView.setNavButtonsEnabled()
                            bottomNavigationView.setNavButtonActive(BottomNavigation.VIDEO)

                            setDefaultVideoCallView()
                            videoCallView.visibility = View.VISIBLE
                        }
                    }
                    ViewState.VideoDialog.Pending -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()

                            videoCallView.setCallButtonDisabled()
                            videoCallView.showCancelCallButton()
                        }
                    }
                    ViewState.VideoDialog.Start -> {
                        runOnUiThread {
                            chatFooterAdapter?.clear()

                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                            hideKeyboard(footerView.inputView)

                            headerView.showHangupButton()

                            setDefaultVideoCallView()
                            videoCallView.visibility = View.GONE

                            recyclerView.visibility = View.VISIBLE

                            footerView.enableAttachmentButton()
                            footerView.visibility = View.VISIBLE

                            bottomNavigationView.setNavButtonsDisabled()
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
                                hideKeyboard(footerView.inputView)

                                footerView.setGoToActiveDialogButtonState(null)

                                videoDialogView.showControlButtons()
                                videoDialogView.visibility = View.VISIBLE
                            } else {
                                footerView.setGoToActiveDialogButtonState(R.string.kenes_return_to_video_call)

                                videoDialogView.visibility = View.GONE
                            }
                        }
                    }
                    ViewState.VideoDialog.UserDisconnected, ViewState.VideoDialog.CallAgentDisconnected -> {
                        runOnUiThread {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                            headerView.hideHangupButton()

                            setDefaultFooterView()
                            footerView.visibility = View.GONE

                            footerView.disableAttachmentButton()

                            videoDialogView.visibility = View.GONE

                            bottomNavigationView.setNavButtonsEnabled()
                        }
                    }
                    is ViewState.VideoDialog.UserFeedback -> {
                        runOnUiThread {
                            if (viewState.isFeedbackSent) {
                                chatFooterAdapter?.showGoToHomeButton()

                                feedbackView.visibility = View.GONE

                                recyclerView.visibility = View.VISIBLE

                                bottomNavigationView.setNavButtonsEnabled()
                            } else {
                                hideKeyboard(footerView.inputView)

                                videoCallView.visibility = View.GONE

                                recyclerView.visibility = View.GONE

                                bottomNavigationView.setNavButtonsDisabled()

                                feedbackView.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
            ViewState.Form -> {
                runOnUiThread {
                    recyclerView.visibility = View.GONE

                    footerView.visibility = View.GONE

                    bottomNavigationView.setNavButtonsDisabled()

                    formView.visibility = View.VISIBLE
                }
            }
            ViewState.Contacts -> {
                runOnUiThread {
                    hideOtherViews()

                    infoView.visibility = View.GONE

                    bottomNavigationView.setNavButtonActive(BottomNavigation.CONTACTS)

                    contactsView.visibility = View.VISIBLE
                }
            }
            ViewState.Info -> {
                runOnUiThread {
                    hideOtherViews()

                    contactsView.visibility = View.GONE

                    bottomNavigationView.setNavButtonActive(BottomNavigation.INFO)

                    infoView.visibility = View.VISIBLE
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

        peerConnectionClient?.removeListeners()
        peerConnectionClient = null

//        headerView = null

        setDefaultAudioCallView()
//        audioCallView = null

        audioDialogView.setDefaultState()
        audioDialogView.callback = null
//        audioDialogView = null

        setDefaultVideoCallView()
//        videoCallView = null

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