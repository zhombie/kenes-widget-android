package q19.kenes.widget.ui.presentation.calls.video

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
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

    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null

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

        view.findViewById<MaterialButton>(R.id.button).setOnClickListener {
            if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        bottomSheetBehavior = BottomSheetBehavior.from(view.findViewById(R.id.videoView))
        bottomSheetBehavior?.isDraggable = true

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