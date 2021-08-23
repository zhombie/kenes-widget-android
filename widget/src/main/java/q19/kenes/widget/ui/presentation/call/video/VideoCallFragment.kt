package q19.kenes.widget.ui.presentation.call.video

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.google.android.material.button.MaterialButton
import kz.q19.domain.model.message.Message
import kz.q19.utils.keyboard.hideKeyboard
import kz.q19.webrtc.PeerConnectionClient
import kz.q19.webrtc.core.ui.SurfaceViewRenderer
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.ui.components.FloatingLayout
import q19.kenes.widget.ui.presentation.call.Call
import q19.kenes.widget.ui.presentation.call.text.TextChatFragment
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes.widget.util.AlertDialogBuilder
import q19.kenes_widget.R

internal class VideoCallFragment :
    BaseFragment<VideoCallPresenter>(R.layout.fragment_video_call),
    VideoCallView,
    MotionLayout.TransitionListener {

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
    private var rootView: MotionLayout? = null
    private var chatView: FragmentContainerView? = null
    private var videoView: FrameLayout? = null
    private var floatingLayout: FloatingLayout? = null
    private var floatingSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var miniSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var fullscreenSurfaceViewRenderer: SurfaceViewRenderer? = null

    // WebRTC Wrapper
    private var peerConnectionClient: PeerConnectionClient? = null

    // Activity + Fragment communication
    private var listener: Listener? = null

    interface Listener {
        fun onCallFinished()
    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view.findViewById(R.id.rootView)
        chatView = view.findViewById(R.id.chatView)
        videoView = view.findViewById(R.id.videoView)
        floatingLayout = view.findViewById(R.id.floatingLayout)
        floatingSurfaceViewRenderer = view.findViewById(R.id.floatingSurfaceViewRenderer)
        miniSurfaceViewRenderer = view.findViewById(R.id.miniSurfaceViewRenderer)
        fullscreenSurfaceViewRenderer = view.findViewById(R.id.fullscreenSurfaceViewRenderer)

        setupTextChat()
        setupVideostreams()
        setupMotionLayout()

        view.findViewById<MaterialButton>(R.id.button2).setOnClickListener {
            if (rootView?.currentState == R.id.start) {
                rootView?.transitionToEnd()
            } else {
                rootView?.transitionToStart()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        activity?.hideKeyboard(view)
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            floatingSurfaceViewRenderer?.release()
            miniSurfaceViewRenderer?.release()
            fullscreenSurfaceViewRenderer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        peerConnectionClient = null

        rootView?.removeTransitionListener(this)
        rootView = null
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

    private fun setupVideostreams() {
        miniSurfaceViewRenderer?.let {
            presenter.initLocalVideostream(it)
        }

        fullscreenSurfaceViewRenderer?.let {
            presenter.initRemoteVideostream(it)
        }
    }

    private fun setupMotionLayout() {
        rootView?.isInteractionEnabled = false

        rootView?.addTransitionListener(this)
    }

    fun onShowVideoCallScreen() {
        Logger.debug(TAG, "onShowVideoCallScreen()")

        if (rootView?.currentState == R.id.start) {
            rootView?.transitionToEnd()
        } else {
            rootView?.transitionToStart()
        }
    }

    fun onHangupCall() {
        Logger.debug(TAG, "onHangupCall()")

        if (rootView?.currentState == R.id.end) {
            presenter.onHangupCall()
        } else {
            rootView?.transitionToEnd()
        }
    }

    /**
     * [MotionLayout.TransitionListener] implementation
     */

    override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {
        Logger.debug(TAG, "onTransitionStarted(): $p1, $p2")

        if (p1 == R.id.start) {
            presenter.setLocalVideostreamPaused()
            presenter.setRemoteVideostreamPaused()
        } else if (p1 == R.id.end) {
            presenter.setLocalVideostreamPaused()
            presenter.setRemoteVideostreamPaused()
        }
    }

    override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
        Logger.debug(TAG, "onTransitionChange(): $p1, $p2, $p3")
    }

    override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
        Logger.debug(TAG, "onTransitionCompleted(): $p1")

        if (p1 == R.id.start) {
            fullscreenSurfaceViewRenderer?.let {
                presenter.setRemoteVideostream(it, false)
            }

            presenter.setLocalVideostreamResumed()
            presenter.setRemoteVideostreamResumed()

            floatingSurfaceViewRenderer?.visibility = View.GONE
            floatingLayout?.visibility = View.GONE
            miniSurfaceViewRenderer?.visibility = View.VISIBLE
            videoView?.visibility = View.VISIBLE
        } else if (p1 == R.id.end) {
            floatingSurfaceViewRenderer?.let {
                presenter.setRemoteVideostream(it, true)
            }

            presenter.setRemoteVideostreamResumed()

            miniSurfaceViewRenderer?.visibility = View.GONE
            videoView?.visibility = View.GONE
            floatingSurfaceViewRenderer?.visibility = View.VISIBLE
            floatingLayout?.visibility = View.VISIBLE
        }
    }

    override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {
        Logger.debug(TAG, "onTransitionTrigger(): $p1, $p2, $p3")
    }

    /**
     * [VideoCallView] implementation
     */

    override fun showCallAgentInfo(fullName: String, photoUrl: String?) {
        Logger.debug(TAG, "showCallAgentInfo() -> $fullName, $photoUrl")

        val fragment = childFragmentManager.findFragmentByTag("text_chat")
        if (fragment is TextChatFragment) {
            fragment.showCallAgentInfo(fullName, photoUrl)
        }
    }

    override fun showNewChatMessage(message: Message) {
        Logger.debug(TAG, "showNewMessage() -> $message")

        val fragment = childFragmentManager.findFragmentByTag("text_chat")
        if (fragment is TextChatFragment) {
            fragment.onNewChatMessage(message)
        }
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
            .setTitle(R.string.kenes_cancel_call)
            .setMessage(R.string.kenes_cancel_call)
            .setNegativeButton(R.string.kenes_no) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.kenes_yes) { dialog, _ ->
                dialog.dismiss()

                presenter.onCancelLiveCall()
            }
            .show()
    }

    override fun navigateToHome() {
        listener?.onCallFinished()
    }

}