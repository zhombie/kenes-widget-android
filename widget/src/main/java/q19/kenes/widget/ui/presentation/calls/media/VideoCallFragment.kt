package q19.kenes.widget.ui.presentation.calls.media

import android.os.Bundle
import android.view.View
import kz.q19.webrtc.PeerConnectionClient
import kz.q19.webrtc.core.ui.SurfaceViewRenderer
import q19.kenes.widget.ui.presentation.platform.BaseDialogFragment
import q19.kenes_widget.R

internal class VideoCallFragment : BaseDialogFragment(R.layout.fragment_video_call),
    VideoCallView {

    companion object {
        private val TAG = VideoCallFragment::class.java.simpleName

        fun newInstance(): VideoCallFragment {
            val fragment = VideoCallFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    private var presenter: VideoCallPresenter? = null

    private var peerConnectionClient: PeerConnectionClient? = null

    private var fullscreenSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var floatingSurfaceViewRenderer: SurfaceViewRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Material_NoActionBar_Fullscreen)

        peerConnectionClient = PeerConnectionClient(requireContext())
        presenter = injection?.provideVideoCallPresenter(getCurrentLanguage(), peerConnectionClient!!)
        presenter?.attachView(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fullscreenSurfaceViewRenderer = view.findViewById(R.id.fullscreenSurfaceViewRenderer)
        floatingSurfaceViewRenderer = view.findViewById(R.id.floatingSurfaceViewRenderer)

        floatingSurfaceViewRenderer?.let {
            presenter?.setLocalSurfaceViewRenderer(it)
        }

        fullscreenSurfaceViewRenderer?.let {
            presenter?.setRemoteSurfaceViewRenderer(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        peerConnectionClient = null

        presenter?.detachView()
        presenter = null
    }

}