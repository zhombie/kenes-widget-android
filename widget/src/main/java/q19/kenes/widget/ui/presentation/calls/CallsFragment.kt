package q19.kenes.widget.ui.presentation.calls

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EdgeEffect
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.core.permission.PermissionManager
import q19.kenes.widget.ui.presentation.HomeFragmentDelegate
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes_widget.R

internal class CallsFragment : BaseFragment(R.layout.fragment_calls), CallsView,
    HomeFragmentDelegate, CallsAdapter.Callback {

    companion object {
        private val TAG = CallsFragment::class.java.simpleName

        fun newInstance(): CallsFragment {
            val fragment = CallsFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    // (MVP) Presenter
    private var presenter: CallsPresenter? = null

    // Android permissions manager
    private var permissionManager: PermissionManager? = null

    // UI Views
    private var recyclerView: RecyclerView? = null
    private var bottomSheet: LinearLayout? = null

    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>? = null

    // RecyclerView adapter
    private var concatAdapter: ConcatAdapter? = null
    private var callsHeaderAdapter: CallsHeaderAdapter? = null
    private var callsAdapter: CallsAdapter? = null

    // onBackPressed() dispatcher for Fragment
    private var onBackPressedDispatcherCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = injection?.provideCallsPresenter(getCurrentLanguage())
        presenter?.attachView(this)

        permissionManager = PermissionManager(requireActivity())
    }

    override fun onResume() {
        super.onResume()

        Logger.debug(TAG, "onResume()")

        if (onBackPressedDispatcherCallback == null) {
            onBackPressedDispatcherCallback = activity?.onBackPressedDispatcher?.addCallback(this) {
                if (presenter?.onBackPressed() == true) {
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
        } else {
            onBackPressedDispatcherCallback?.isEnabled = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        bottomSheet = view.findViewById(R.id.bottomSheet)

        setupRecyclerView()
        setupBottomSheetBehavior()
    }

    override fun onPause() {
        super.onPause()

        Logger.debug(TAG, "onPause()")

        onBackPressedDispatcherCallback?.isEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()

        onBackPressedDispatcherCallback?.remove()
        onBackPressedDispatcherCallback = null

        permissionManager?.removeAllListeners()
        permissionManager = null

        presenter?.detachView()
    }

    private fun setupRecyclerView() {
        recyclerView?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        callsHeaderAdapter = CallsHeaderAdapter()
        callsAdapter = CallsAdapter(this)
        concatAdapter = ConcatAdapter(callsHeaderAdapter, callsAdapter)
        recyclerView?.adapter = concatAdapter

        recyclerView?.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return EdgeEffect(view.context).apply {
                    color = Color.parseColor("#2667E5")
                }
            }
        }
    }

    private fun setupBottomSheetBehavior() {
        bottomSheet?.let {
            bottomSheetBehavior = BottomSheetBehavior.from(it)
            bottomSheetBehavior?.isDraggable = false

            bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    presenter?.onBottomSheetStateChanged(newState == BottomSheetBehavior.STATE_EXPANDED)
                }
            })
        }
    }

    /**
     * [HomeFragmentDelegate] implementation
     */

    override fun onScreenRenavigate() {
    }

    /**
     * [CallsAdapter.Callback] implementation
     */

    override fun onCallClicked(call: Call) {
        presenter?.onCallClicked(call)
    }

    override fun onCallGroupClicked(callGroup: CallGroup) {
        presenter?.onCallGroupClicked(callGroup)
    }

    /**
     * [CallsView] implementation
     */

    override fun showCalls(anyCalls: List<AnyCall>) {
        Logger.debug(TAG, "anyCalls: $anyCalls")
        callsAdapter?.anyCalls = anyCalls
    }

    override fun tryToResolvePermissions(call: Call) {
        when (call) {
            is Call.Text -> {
                permissionManager?.checkPermission(PermissionManager.Permission.EXTERNAL_STORAGE) {
                    if (it) {
                        presenter?.onCallPermissionsGranted(call)
                    }
                }
            }
            is Call.Audio -> {
                permissionManager?.checkPermission(PermissionManager.Permission.AUDIO_CALL) {
                    if (it) {
                        presenter?.onCallPermissionsGranted(call)
                    }
                }
            }
            is Call.Video -> {
                permissionManager?.checkPermission(PermissionManager.Permission.VIDEO_CALL) {
                    if (it) {
                        presenter?.onCallPermissionsGranted(call)
                    }
                }
            }
        }
    }

    override fun launchPendingCall(call: Call) {
        if (bottomSheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun toggleBottomSheet() {
        if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

}