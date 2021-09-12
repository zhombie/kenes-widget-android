package kz.q19.kenes.widget.ui.presentation.call

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EdgeEffect
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.q19.domain.model.call.AnyCall
import kz.q19.domain.model.call.Call
import kz.q19.domain.model.call.CallGroup
import kz.q19.utils.android.dp2Px
import kz.q19.kenes.widget.core.logging.Logger
import kz.q19.kenes.widget.core.permission.PermissionManager
import kz.q19.kenes.widget.ui.presentation.HomeScreenDelegate
import kz.q19.kenes.widget.ui.presentation.call.selection.CallSelection
import kz.q19.kenes.widget.ui.presentation.call.selection.CallSelectionBottomSheetDialogFragment
import kz.q19.kenes.widget.ui.presentation.common.HomeFragment
import kz.q19.kenes.widget.ui.presentation.common.recycler_view.SpacingItemDecoration
import kz.q19.kenes.widget.R

internal class CallsFragment : HomeFragment<CallsPresenter>(R.layout.kenes_fragment_calls),
    CallsView,
    HomeScreenDelegate,
    CallsHeaderAdapter.Callback,
    CallsAdapter.Callback {

    companion object {
        private val TAG = CallsFragment::class.java.simpleName

        fun newInstance(): CallsFragment {
            val fragment = CallsFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    // Android permissions manager
    private var permissionManager: PermissionManager? = null

    // UI Views
    private var recyclerView: RecyclerView? = null

    // RecyclerView adapter
    private var concatAdapter: ConcatAdapter? = null
    private var callsHeaderAdapter: CallsHeaderAdapter? = null
    private var callsAdapter: CallsAdapter? = null

    // onBackPressed() dispatcher for Fragment
    private var onBackPressedCallback: OnBackPressedCallback? = null

    // Activity + Fragment communication
    private var listener: Listener? = null

    interface Listener : HomeFragment.Listener {
        fun onLaunchCall(call: Call)
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

        permissionManager = PermissionManager(requireActivity())
    }

    override fun createPresenter(): CallsPresenter {
        return injection.provideCallsPresenter(getCurrentLanguage())
    }

    override fun onResume() {
        super.onResume()

        Logger.debug(TAG, "onResume()")

        if (onBackPressedCallback == null) {
            onBackPressedCallback = activity?.onBackPressedDispatcher?.addCallback(this) {
                if (presenter.onBackPressed()) {
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
        } else {
            onBackPressedCallback?.isEnabled = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)

        setupRecyclerView()
    }

    override fun onPause() {
        super.onPause()

        Logger.debug(TAG, "onPause()")

        onBackPressedCallback?.isEnabled = false
    }

    override fun onDetach() {
        super.onDetach()

        listener = null
    }

    override fun onDestroy() {
        super.onDestroy()

        childFragmentManager.clearFragmentResultListener("request_key.call_selection")

        recyclerView?.clearOnScrollListeners()

        onBackPressedCallback?.remove()
        onBackPressedCallback = null

        permissionManager?.removeAllListeners()
        permissionManager = null
    }

    private fun setupRecyclerView() {
        recyclerView?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        callsHeaderAdapter = CallsHeaderAdapter(this)
        callsAdapter = CallsAdapter(this)
        concatAdapter = ConcatAdapter(callsHeaderAdapter, callsAdapter)
        recyclerView?.adapter = concatAdapter

        recyclerView?.addItemDecoration(SpacingItemDecoration(10F.dp2Px()))

        recyclerView?.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return EdgeEffect(view.context).apply {
                    color = Color.parseColor("#2667E5")
                }
            }
        }

        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                listener?.onVerticalScroll(recyclerView.computeVerticalScrollOffset())
            }
        })
    }

    /**
     * [HomeScreenDelegate] implementation
     */

    override fun onScreenRenavigate() {
        with(recyclerView?.layoutManager) {
            if (this is LinearLayoutManager) {
                if (findFirstCompletelyVisibleItemPosition() == 0) {
                    presenter.onResetDataRequested()
                } else {
                    recyclerView?.smoothScrollToPosition(0)
                }
            }
        }
    }

    /**
     * [CallsHeaderAdapter.Callback] implementation
     */

    override fun onBackPressed() {
        presenter.onBackPressed()
    }

    /**
     * [CallsAdapter.Callback] implementation
     */

    override fun onCallClicked(call: Call) {
        presenter.onCallSelected(call)
    }

    override fun onCallGroupClicked(callGroup: CallGroup) {
        presenter.onCallGroupClicked(callGroup)
    }

    /**
     * [CallsView] implementation
     */

    override fun showCalls(anyCalls: List<AnyCall>) {
//        Logger.debug(TAG, "showCalls() -> anyCalls: $anyCalls")

        callsHeaderAdapter?.isToolbarVisible = anyCalls.all { it is CallGroup.Secondary }

        callsAdapter?.anyCalls = anyCalls
    }

    override fun showCallSelection(callSelection: CallSelection) {
//        Logger.debug(TAG, "showCallSelection() -> callSelection: $callSelection")

        val fragment = childFragmentManager.findFragmentByTag("call_selection")
        if (fragment is CallSelectionBottomSheetDialogFragment) {
            fragment.dismiss()
        }

        childFragmentManager.setFragmentResultListener("request_key.call_selection", this) { _, bundle ->
            val call = bundle.getParcelable<Call>("call")
            if (call != null) {
                childFragmentManager.clearFragmentResultListener("request_key.call_selection")

                presenter.onCallSelected(call)
            }
        }

        CallSelectionBottomSheetDialogFragment.newInstance(callSelection)
            .show(childFragmentManager, "call_selection")
    }

    override fun tryToResolvePermissions(call: Call) {
        when (call) {
            is Call.Text ->
                permissionManager?.checkPermission(PermissionManager.Permission.EXTERNAL_STORAGE) {
                    if (it) {
                        presenter.onCallPermissionsGranted(call)
                    }
                }
            is Call.Audio ->
                permissionManager?.checkPermission(PermissionManager.Permission.AUDIO_CALL) {
                    if (it) {
                        presenter.onCallPermissionsGranted(call)
                    }
                }
            is Call.Video ->
                permissionManager?.checkPermission(PermissionManager.Permission.VIDEO_CALL) {
                    if (it) {
                        presenter.onCallPermissionsGranted(call)
                    }
                }
        }
    }

    override fun launchCall(call: Call) {
        listener?.onLaunchCall(call)
    }

}