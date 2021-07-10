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

    private lateinit var fullscreenSurfaceViewRenderer: SurfaceViewRenderer
    private lateinit var floatingSurfaceViewRenderer: SurfaceViewRenderer

    private var presenter: VideoCallPresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Material_NoActionBar_Fullscreen)

        val peerConnectionClient = PeerConnectionClient(requireContext())
        presenter = context.injection?.provideVideoCallPresenter(peerConnectionClient)
        presenter?.attachView(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fullscreenSurfaceViewRenderer = view.findViewById(R.id.fullscreenSurfaceViewRenderer)
        floatingSurfaceViewRenderer = view.findViewById(R.id.floatingSurfaceViewRenderer)

        presenter?.setLocalSurfaceViewRenderer(floatingSurfaceViewRenderer)
        presenter?.setRemoteSurfaceViewRenderer(fullscreenSurfaceViewRenderer)
    }

    override fun onDestroy() {
        presenter?.detachView()
        super.onDestroy()
    }

}