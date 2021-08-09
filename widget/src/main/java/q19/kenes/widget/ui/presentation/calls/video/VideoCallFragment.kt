package q19.kenes.widget.ui.presentation.calls.video

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.button.MaterialButton
import kz.q19.webrtc.PeerConnectionClient
import kz.q19.webrtc.core.ui.SurfaceViewRenderer
import q19.kenes.widget.ui.presentation.platform.BaseFullscreenDialogFragment
import q19.kenes_widget.R

internal class VideoCallFragment :
    BaseFullscreenDialogFragment<VideoCallPresenter>(R.layout.fragment_video_call),
    VideoCallView {

    companion object {
        private val TAG = VideoCallFragment::class.java.simpleName

        fun newInstance(): VideoCallFragment {
            val fragment = VideoCallFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    // UI Views
    private var videoView: FrameLayout? = null
    private var miniSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var fullscreenSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var floatingSurfaceViewRenderer: SurfaceViewRenderer? = null

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

        videoView = view.findViewById(R.id.videoView)
        miniSurfaceViewRenderer = view.findViewById(R.id.miniSurfaceViewRenderer)
        fullscreenSurfaceViewRenderer = view.findViewById(R.id.fullscreenSurfaceViewRenderer)
        floatingSurfaceViewRenderer = view.findViewById(R.id.floatingSurfaceViewRenderer)

        miniSurfaceViewRenderer?.let {
            presenter.initLocalVideostream(it)
        }

        fullscreenSurfaceViewRenderer?.let {
            presenter.initRemoteVideostream(it)
        }

        view.findViewById<MaterialButton>(R.id.button).setOnClickListener {
            if (videoView?.visibility == View.VISIBLE) {
                floatingSurfaceViewRenderer?.let {
                    presenter.setRemoteVideostream(it, true)
                }

                videoView?.visibility = View.GONE
                floatingSurfaceViewRenderer?.visibility = View.VISIBLE
            } else {
                fullscreenSurfaceViewRenderer?.let {
                    presenter.setRemoteVideostream(it, false)
                }

                floatingSurfaceViewRenderer?.visibility = View.GONE
                videoView?.visibility = View.VISIBLE
            }
        }

        view.findViewById<MaterialButton>(R.id.button2).setOnClickListener {
            if (videoView?.visibility == View.VISIBLE) {
                floatingSurfaceViewRenderer?.let {
                    presenter.setRemoteVideostream(it, true)
                }

                videoView?.visibility = View.GONE
                floatingSurfaceViewRenderer?.visibility = View.VISIBLE
            } else {
                fullscreenSurfaceViewRenderer?.let {
                    presenter.setRemoteVideostream(it, false)
                }

                floatingSurfaceViewRenderer?.visibility = View.GONE
                videoView?.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        peerConnectionClient = null
    }

}