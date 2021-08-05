package q19.kenes.widget.ui.presentation.calls.video

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
    private var fullscreenSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var floatingSurfaceViewRenderer: SurfaceViewRenderer? = null

    // CoordinatorLayout + BottomSheet
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null

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

        fullscreenSurfaceViewRenderer = view.findViewById(R.id.fullscreenSurfaceViewRenderer)
        floatingSurfaceViewRenderer = view.findViewById(R.id.floatingSurfaceViewRenderer)

        floatingSurfaceViewRenderer?.let {
            presenter.setLocalSurfaceViewRenderer(it)
        }

        fullscreenSurfaceViewRenderer?.let {
            presenter.setRemoteSurfaceViewRenderer(it)
        }

        view.findViewById<MaterialButton>(R.id.button).setOnClickListener {
            if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        bottomSheetBehavior = BottomSheetBehavior.from(view.findViewById(R.id.videoView))
        bottomSheetBehavior?.isDraggable = true

        if (bottomSheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        peerConnectionClient = null
    }

}