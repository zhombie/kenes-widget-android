package q19.kenes.widget.ui.presentation.calls.video

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.motion.widget.MotionLayout
import com.google.android.material.button.MaterialButton
import kz.q19.webrtc.PeerConnectionClient
import kz.q19.webrtc.core.ui.SurfaceViewRenderer
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.ui.components.FloatingLayout
import q19.kenes.widget.ui.presentation.platform.BaseFullscreenDialogFragment
import q19.kenes_widget.R

internal class VideoCallFragment :
    BaseFullscreenDialogFragment<VideoCallPresenter>(R.layout.fragment_video_call, true),
    VideoCallView, MotionLayout.TransitionListener {

    companion object {
        private val TAG = VideoCallFragment::class.java.simpleName

        fun newInstance(): VideoCallFragment {
            val fragment = VideoCallFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    // UI Views
    private var rootView: MotionLayout? = null
    private var videoView: FrameLayout? = null
    private var floatingLayout: FloatingLayout? = null
    private var floatingSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var miniSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var fullscreenSurfaceViewRenderer: SurfaceViewRenderer? = null

    // WebRTC Wrapper
    private var peerConnectionClient: PeerConnectionClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter.attachView(this)
    }

    override fun createPresenter(): VideoCallPresenter {
        return injection.provideVideoCallPresenter(
            getCurrentLanguage(),
            PeerConnectionClient(requireContext()).also { peerConnectionClient = it }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view.findViewById(R.id.rootView)
        videoView = view.findViewById(R.id.videoView)
        floatingLayout = view.findViewById(R.id.floatingLayout)
        floatingSurfaceViewRenderer = view.findViewById(R.id.floatingSurfaceViewRenderer)
        miniSurfaceViewRenderer = view.findViewById(R.id.miniSurfaceViewRenderer)
        fullscreenSurfaceViewRenderer = view.findViewById(R.id.fullscreenSurfaceViewRenderer)

        miniSurfaceViewRenderer?.let {
            presenter.initLocalVideostream(it)
        }

        fullscreenSurfaceViewRenderer?.let {
            presenter.initRemoteVideostream(it)
        }

        setupMotionLayout()

        view.findViewById<MaterialButton>(R.id.button).setOnClickListener {
            if (rootView?.currentState == R.id.start) {
                rootView?.transitionToEnd()
            } else {
                rootView?.transitionToStart()
            }
        }

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

        peerConnectionClient = null

        rootView?.removeTransitionListener(this)
        rootView = null
    }

    private fun setupMotionLayout() {
        rootView?.isInteractionEnabled = true

        rootView?.addTransitionListener(this)
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

            rootView?.isInteractionEnabled = true

            floatingSurfaceViewRenderer?.visibility = View.GONE
            floatingLayout?.visibility = View.GONE
            miniSurfaceViewRenderer?.visibility = View.VISIBLE
            videoView?.visibility = View.VISIBLE
        } else if (p1 == R.id.end) {
            presenter.setLocalVideostreamResumed()
            presenter.setRemoteVideostreamResumed()

            floatingSurfaceViewRenderer?.let {
                presenter.setRemoteVideostream(it, true)
            }

            rootView?.isInteractionEnabled = false

            miniSurfaceViewRenderer?.visibility = View.GONE
            videoView?.visibility = View.GONE
            floatingSurfaceViewRenderer?.visibility = View.VISIBLE
            floatingLayout?.visibility = View.VISIBLE
        }
    }

    override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {
        Logger.debug(TAG, "onTransitionTrigger(): $p1, $p2, $p3")
    }

}