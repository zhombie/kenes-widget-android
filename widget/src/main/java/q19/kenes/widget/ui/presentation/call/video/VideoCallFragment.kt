package q19.kenes.widget.ui.presentation.call.video

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.google.android.material.button.MaterialButton
import kz.q19.domain.model.message.Message
import kz.q19.webrtc.PeerConnectionClient
import kz.q19.webrtc.core.ui.SurfaceViewRenderer
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.ui.components.FloatingLayout
import q19.kenes.widget.ui.presentation.call.Call
import q19.kenes.widget.ui.presentation.call.text.TextChatFragment
import q19.kenes.widget.ui.presentation.platform.BaseFullscreenDialogFragment
import q19.kenes.widget.util.AlertDialogBuilder
import q19.kenes_widget.R

internal class VideoCallFragment :
    BaseFullscreenDialogFragment<VideoCallPresenter>(R.layout.fragment_video_call, true),
    VideoCallView,
    MotionLayout.TransitionListener,
    FragmentOnAttachListener {

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

    // Fragment
    private var textChatFragment: TextChatFragment? = null

    // WebRTC Wrapper
    private var peerConnectionClient: PeerConnectionClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter.attachView(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                presenter.onBackPressed()
            }
        }
    }

    override fun createPresenter(): VideoCallPresenter {
        val call = arguments?.getParcelable<Call>("call")
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

    override fun onDestroy() {
        super.onDestroy()

        childFragmentManager.removeFragmentOnAttachListener(this)

        textChatFragment?.setListener(null)
        textChatFragment = null

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
        childFragmentManager.addFragmentOnAttachListener(this)

        childFragmentManager.beginTransaction()
            .replace(R.id.chatView, TextChatFragment.newInstance().also { textChatFragment = it })
            .commit()
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

    /**
     * [FragmentOnAttachListener] implementation
     */

    override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment is TextChatFragment) {
            fragment.setListener {
                if (rootView?.currentState == R.id.start) {
                    rootView?.transitionToEnd()
                } else {
                    rootView?.transitionToStart()
                }
            }
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
            presenter.setLocalVideostreamResumed()
            presenter.setRemoteVideostreamResumed()

            fullscreenSurfaceViewRenderer?.let {
                presenter.setRemoteVideostream(it, false)
            }

            floatingSurfaceViewRenderer?.visibility = View.GONE
            floatingLayout?.visibility = View.GONE
            miniSurfaceViewRenderer?.visibility = View.VISIBLE
            videoView?.visibility = View.VISIBLE
        } else if (p1 == R.id.end) {
            presenter.setLocalVideostreamPaused()
            presenter.setRemoteVideostreamResumed()

            floatingSurfaceViewRenderer?.let {
                presenter.setRemoteVideostream(it, true)
            }

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

        textChatFragment?.showCallAgentInfo(fullName, photoUrl)
    }

    override fun showNewChatMessage(message: Message) {
        Logger.debug(TAG, "showNewMessage() -> $message")

        textChatFragment?.onNewChatMessage(message)
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

                if (rootView?.currentState == R.id.start) {
                    rootView?.transitionToEnd()
                } else {
                    presenter.onCancelPendingCall()
                }
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

                if (rootView?.currentState == R.id.start) {
                    rootView?.transitionToEnd()
                } else {
                    presenter.onCancelLiveCall()
                }
            }
            .show()
    }

    override fun navigateToHome() {
        dismiss()
    }

}