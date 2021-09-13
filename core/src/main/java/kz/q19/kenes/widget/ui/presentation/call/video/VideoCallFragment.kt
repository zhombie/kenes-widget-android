package kz.q19.kenes.widget.ui.presentation.call.video

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import kz.q19.domain.model.call.Call
import kz.q19.domain.model.message.Message
import kz.q19.kenes.widget.R
import kz.q19.kenes.widget.core.logging.Logger
import kz.q19.kenes.widget.ui.components.KenesFloatingLayout
import kz.q19.kenes.widget.ui.components.KenesToolbar
import kz.q19.kenes.widget.ui.presentation.call.text.TextChatFragment
import kz.q19.kenes.widget.ui.presentation.common.BottomSheetState
import kz.q19.kenes.widget.ui.presentation.platform.BaseFragment
import kz.q19.kenes.widget.util.AlertDialogBuilder
import kz.q19.kenes.widget.util.hideKeyboardCompat
import kz.q19.webrtc.PeerConnectionClient
import kz.q19.webrtc.core.ui.SurfaceViewRenderer

internal class VideoCallFragment :
    BaseFragment<VideoCallPresenter>(R.layout.kenes_fragment_video_call),
    VideoCallView,
    TextChatFragment.Listener {

    companion object {
        private val TAG = VideoCallFragment::class.java.simpleName

        fun newInstance(call: Call): VideoCallFragment {
            val fragment = VideoCallFragment()
            fragment.arguments = Bundle().apply {
                putParcelable("call", call)
            }
            return fragment
        }
    }

    // UI Views
    private var chatView: FragmentContainerView? = null
    private var floatingLayout: KenesFloatingLayout? = null
    private var floatingSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var videoView: FrameLayout? = null
    private var fullscreenSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var minimizeButton: MaterialButton? = null
    private var toolbar: KenesToolbar? = null
    private var miniSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var toggleCameraButton: MaterialButton? = null
    private var toggleAudioButton: MaterialButton? = null
    private var toggleCameraSourceButton: MaterialButton? = null
    private var hangupButton: MaterialButton? = null

    // WebRTC Wrapper
    private var peerConnectionClient: PeerConnectionClient? = null

    // CoordinatorLayout + BottomSheet
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var bottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback? = null

    // onBackPressed() dispatcher for Fragment
    private var onBackPressedCallback: OnBackPressedCallback? = null

    // Activity + Fragment communication
    private var listener: Listener? = null

    interface Listener {
        fun onCallFinished()
    }

    private var floatingVideostreamEnterAnimator: ViewPropertyAnimator? = null
    private var floatingVideostreamExitAnimator: ViewPropertyAnimator? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is Listener) {
            this.listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter.attachView(this)
    }

    override fun createPresenter(): VideoCallPresenter {
        val call = requireArguments().getParcelable<Call>("call")
            ?: throw IllegalStateException("Where is Call?")
        return injection.provideVideoCallPresenter(
            getCurrentLanguage(),
            call,
            PeerConnectionClient(requireContext()).also { peerConnectionClient = it }
        )
    }

    override fun onResume() {
        super.onResume()

        Logger.debug(TAG, "onResume()")

        if (onBackPressedCallback == null) {
            onBackPressedCallback = activity?.onBackPressedDispatcher?.addCallback(this) {
                if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    if (presenter.onBackPressed()) {
                        isEnabled = false
                        listener?.onCallFinished()
                    }
                }
            }
        } else {
            onBackPressedCallback?.isEnabled = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatView = view.findViewById(R.id.chatView)
        floatingLayout = view.findViewById(R.id.floatingLayout)
        floatingSurfaceViewRenderer = view.findViewById(R.id.floatingSurfaceViewRenderer)
        videoView = view.findViewById(R.id.videoView)
        fullscreenSurfaceViewRenderer = view.findViewById(R.id.fullscreenSurfaceViewRenderer)
        minimizeButton = view.findViewById(R.id.minimizeButton)
        toolbar = view.findViewById(R.id.toolbar)
        miniSurfaceViewRenderer = view.findViewById(R.id.miniSurfaceViewRenderer)
        toggleCameraButton = view.findViewById(R.id.toggleCameraButton)
        toggleAudioButton = view.findViewById(R.id.toggleAudioButton)
        toggleCameraSourceButton = view.findViewById(R.id.toggleCameraSourceButton)
        hangupButton = view.findViewById(R.id.hangupButton)

        setupTextChat()
        setupBottomSheet()
        setupVideostreamControlButtons()
        setupVideostreams()
    }

    override fun onPause() {
        super.onPause()

        view?.hideKeyboardCompat()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        floatingVideostreamEnterAnimator?.cancel()
        floatingVideostreamEnterAnimator = null

        floatingVideostreamExitAnimator?.cancel()
        floatingVideostreamExitAnimator = null

        bottomSheetBehaviorCallback?.let { bottomSheetBehavior?.removeBottomSheetCallback(it) }
        bottomSheetBehaviorCallback = null
        bottomSheetBehavior = null

        try {
            floatingSurfaceViewRenderer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            floatingSurfaceViewRenderer = null
        }

        try {
            miniSurfaceViewRenderer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            miniSurfaceViewRenderer = null
        }

        try {
            fullscreenSurfaceViewRenderer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fullscreenSurfaceViewRenderer = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        onBackPressedCallback?.remove()
        onBackPressedCallback = null

        peerConnectionClient = null
    }

    private fun setupTextChat() {
        childFragmentManager.commit(false) {
            add(
                R.id.chatView,
                TextChatFragment.newInstance(),
                "text_chat"
            )
        }
    }

    private fun setupBottomSheet() {
        videoView?.let { view ->
            bottomSheetBehavior = BottomSheetBehavior.from(view)
            bottomSheetBehavior?.isDraggable = true

            bottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    presenter.onBottomSheetStateChanged(BottomSheetState.from(newState) ?: return)
                }
            }

            bottomSheetBehaviorCallback?.let {
                bottomSheetBehavior?.addBottomSheetCallback(it)
            }
        }
    }

    private fun setupVideostreamControlButtons() {
        minimizeButton?.setOnClickListener {
            presenter.onMinimizeClicked()
        }

        toggleCameraButton?.setOnClickListener {
            presenter.onToggleLocalCamera()
        }

        toggleAudioButton?.setOnClickListener {
            presenter.onToggleLocalAudio()
        }

        toggleCameraSourceButton?.setOnClickListener {
            presenter.onToggleLocalCameraSource()
        }

        hangupButton?.setOnClickListener {
            presenter.onHangupCall()
        }
    }

    private fun setupVideostreams() {
        miniSurfaceViewRenderer?.let {
            presenter.initLocalVideostream(it)
        }

        fullscreenSurfaceViewRenderer?.let {
            presenter.initRemoteVideostream(it)
        }
    }

    /**
     * [TextChatFragment.Listener] implementation
     */

    override fun onViewReady() {
        presenter.onViewReady()
    }

    override fun onNavigationBackPressed() {
        if (presenter.onBackPressed()) {
            onBackPressedCallback?.isEnabled = false
            listener?.onCallFinished()
        }
    }

    override fun onShowVideoCallScreen() {
        presenter.onShowVideoCallScreen()
    }

    override fun onSelectAttachment() {
        presenter.onSelectAttachment()
    }

    override fun onSendTextMessage(message: String?) {
        presenter.onSendTextMessage(message)
    }

    override fun onHangupCall() {
        presenter.onHangupCall()
    }

    /**
     * [VideoCallView] implementation
     */

    override fun showCallAgentInfo(fullName: String, photoUrl: String?) {
        Logger.debug(TAG, "showCallAgentInfo() -> $fullName, $photoUrl")

        runOnUiThread {
            toolbar?.setImageContentPadding(0)
            toolbar?.showImage(photoUrl)

            toolbar?.setTitle(fullName)
            toolbar?.setSubtitle("Оператор")

            toolbar?.reveal()
        }

        runOnTextChatScreen {
            showCallAgentInfo(fullName, photoUrl)
        }
    }

    override fun showNewChatMessage(message: Message) {
        Logger.debug(TAG, "showNewChatMessage() -> $message")

        runOnTextChatScreen {
            onNewChatMessage(message)
        }
    }

    override fun showFloatingVideostreamView() = runOnUiThread {
        floatingVideostreamEnterAnimator?.cancel()
        floatingVideostreamEnterAnimator = null

        floatingVideostreamExitAnimator?.cancel()
        floatingVideostreamExitAnimator = null

        floatingSurfaceViewRenderer?.visibility = View.VISIBLE
        floatingLayout?.visibility = View.VISIBLE
    }

    override fun hideFloatingVideostreamView() = runOnUiThread {
        floatingVideostreamEnterAnimator?.cancel()
        floatingVideostreamEnterAnimator = null

        floatingVideostreamExitAnimator?.cancel()
        floatingVideostreamExitAnimator = null

        floatingSurfaceViewRenderer?.visibility = View.GONE
        floatingLayout?.visibility = View.GONE
    }

    override fun showVideoCallScreenSwitcher() {
        Logger.debug(TAG, "showVideoCallScreenSwitcher()")

        runOnTextChatScreen {
            showVideoCallScreenSwitcher()
        }
    }

    override fun hideVideoCallScreenSwitcher() {
        Logger.debug(TAG, "showVideoCallScreenSwitcher()")

        runOnTextChatScreen {
            hideVideoCallScreenSwitcher()
        }
    }

    override fun showHangupCallButton() {
        Logger.debug(TAG, "showHangupCallButton()")

        runOnTextChatScreen {
            showHangupCallButton()
        }
    }

    override fun hideHangupCallButton() {
        runOnTextChatScreen {
            hideHangupCallButton()
        }
    }

    override fun setLocalAudioEnabled() = runOnUiThread {
        toggleAudioButton?.apply {
            backgroundTintList = getColorStateList(R.color.kenes_white)
            icon = getDrawable(R.drawable.kenes_ic_mic_on_stroke)
            iconTint = getColorStateList(R.color.kenes_black)
        }
    }

    override fun setLocalAudioDisabled() = runOnUiThread {
        toggleAudioButton?.apply {
            backgroundTintList = getColorStateList(R.color.kenes_white_with_opacity_40)
            icon = getDrawable(R.drawable.kenes_ic_mic_off_stroke)
            iconTint = getColorStateList(R.color.kenes_white)
        }
    }

    override fun setLocalVideoEnabled() = runOnUiThread {
        toggleCameraButton?.apply {
            backgroundTintList = getColorStateList(R.color.kenes_white)
            icon = getDrawable(R.drawable.kenes_ic_camera_on_stroke)
            iconTint = getColorStateList(R.color.kenes_black)
        }
    }

    override fun setLocalVideoDisabled() = runOnUiThread {
        toggleCameraButton?.apply {
            backgroundTintList = getColorStateList(R.color.kenes_white_with_opacity_40)
            icon = getDrawable(R.drawable.kenes_ic_camera_off_stroke)
            iconTint = getColorStateList(R.color.kenes_white)
        }
    }

    override fun enterFloatingVideostream() = runOnUiThread {
        presenter.setLocalVideostreamPaused()
        presenter.setRemoteVideostreamPaused()

        floatingSurfaceViewRenderer?.let {
            presenter.setRemoteVideostream(it, true)
        }

        presenter.setRemoteVideostreamResumed()

        floatingLayout?.visibility = View.VISIBLE
        floatingLayout?.alpha = 0.25F
        floatingLayout?.scaleX = 0.5F
        floatingLayout?.scaleY = 0.5F
        floatingVideostreamEnterAnimator?.cancel()
        floatingVideostreamEnterAnimator = null
        floatingVideostreamEnterAnimator = floatingLayout?.animate()
            ?.setDuration(150L)
            ?.alpha(1F)
            ?.scaleX(1F)
            ?.scaleY(1F)
            ?.withEndAction {
                floatingSurfaceViewRenderer?.visibility = View.VISIBLE
            }
        floatingVideostreamEnterAnimator?.start()
    }

    override fun exitFloatingVideostream() = runOnUiThread {
        presenter.setLocalVideostreamPaused()
        presenter.setRemoteVideostreamPaused()

        floatingLayout?.visibility = View.GONE
        floatingLayout?.alpha = 0.75F
        floatingLayout?.scaleX = 1F
        floatingLayout?.scaleY = 1F
        floatingVideostreamExitAnimator?.cancel()
        floatingVideostreamExitAnimator = null
        floatingVideostreamExitAnimator = floatingLayout?.animate()
            ?.setDuration(100L)
            ?.alpha(0F)
            ?.scaleX(0.35F)
            ?.scaleY(0.35F)
            ?.withEndAction {
                floatingSurfaceViewRenderer?.visibility = View.GONE

                fullscreenSurfaceViewRenderer?.let {
                    presenter.setRemoteVideostream(it, false)
                }

                presenter.setLocalVideostreamResumed()
                presenter.setRemoteVideostreamResumed()
            }
        floatingVideostreamExitAnimator?.start()
    }

    override fun clearMessageInput() {
        runOnTextChatScreen {
            clearMessageInput()
        }
    }

    override fun collapseBottomSheet() {
        Logger.debug(TAG, "collapseBottomSheet() -> ${bottomSheetBehavior?.state}")

        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun expandBottomSheet() {
        Logger.debug(TAG, "expandBottomSheet() -> ${bottomSheetBehavior?.state}")

        view?.hideKeyboardCompat()

        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun showNoOnlineCallAgentsMessage(text: String?) = runOnUiThread {
        AlertDialogBuilder(requireContext())
            .setTitle(R.string.kenes_attention)
            .setMessage(text ?: "No online call agents, please, try later")
            .setPositiveButton(R.string.kenes_ok) { dialog, _ ->
                dialog.dismiss()

                presenter.onCancelPendingCall()
            }
            .show()
    }

    override fun showOperationAvailableOnlyDuringLiveCallMessage() {
        AlertDialogBuilder(requireContext())
            .setTitle(R.string.kenes_attention)
            .setMessage(R.string.kenes_select_attachment_button_disabled)
            .setPositiveButton(R.string.kenes_ok) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun showCancelPendingConfirmationMessage() {
        AlertDialogBuilder(requireContext())
            .setTitle(R.string.kenes_cancel_call)
            .setMessage(R.string.kenes_cancel_call)
            .setNegativeButton(R.string.kenes_no) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.kenes_yes) { dialog, _ ->
                dialog.dismiss()

                presenter.onCancelPendingCall()
            }
            .show()
    }

    override fun showCancelLiveCallConfirmationMessage() {
        AlertDialogBuilder(requireContext())
            .setTitle(R.string.kenes_attention)
            .setMessage(R.string.kenes_end_dialog)
            .setNegativeButton(R.string.kenes_no) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.kenes_yes) { dialog, _ ->
                dialog.dismiss()

                presenter.onCancelLiveCall()
            }
            .show()
    }

    override fun showAttachmentSelection() {
        val items = arrayOf(
            requireContext().getString(R.string.kenes_image),
            requireContext().getString(R.string.kenes_video),
            requireContext().getString(R.string.kenes_audio),
            requireContext().getString(R.string.kenes_document)
        )
        val checkedItem = 0
        AlertDialogBuilder(requireContext())
            .setTitle(requireContext().getString(R.string.kenes_select))
            .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                dialog.dismiss()

                Logger.debug(TAG, "SELECTED: " + items[which])
            }
            .setNegativeButton(R.string.file) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun navigateToHome() {
        listener?.onCallFinished()
    }

    private fun runOnTextChatScreen(block: TextChatFragment.() -> Unit) =
        with(childFragmentManager.findFragmentByTag("text_chat")) {
            if (this is TextChatFragment) {
                block.invoke(this)
            }
        }

}