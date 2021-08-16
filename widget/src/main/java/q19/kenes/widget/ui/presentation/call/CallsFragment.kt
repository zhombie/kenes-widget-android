package q19.kenes.widget.ui.presentation.call

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EdgeEffect
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.core.permission.PermissionManager
import q19.kenes.widget.ui.presentation.HomeFragmentDelegate
import q19.kenes.widget.ui.presentation.call.pending.PendingCallFragment
import q19.kenes.widget.ui.presentation.call.video.VideoCallFragment
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes_widget.R

internal class CallsFragment : BaseFragment<CallsPresenter>(R.layout.fragment_calls), CallsView,
    HomeFragmentDelegate, CallsAdapter.Callback {

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
    private var onBackPressedDispatcherCallback: OnBackPressedCallback? = null

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

        if (onBackPressedDispatcherCallback == null) {
            onBackPressedDispatcherCallback = activity?.onBackPressedDispatcher?.addCallback(this) {
                if (presenter.onBackPressed()) {
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

        setupRecyclerView()
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

    /**
     * [HomeFragmentDelegate] implementation
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
     * [CallsAdapter.Callback] implementation
     */

    override fun onCallClicked(call: Call) {
        presenter.onCallClicked(call)
    }

    override fun onCallGroupClicked(callGroup: CallGroup) {
        presenter.onCallGroupClicked(callGroup)
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
                        presenter.onCallPermissionsGranted(call)
                    }
                }
            }
            is Call.Audio -> {
                permissionManager?.checkPermission(PermissionManager.Permission.AUDIO_CALL) {
                    if (it) {
                        presenter.onCallPermissionsGranted(call)
                    }
                }
            }
            is Call.Video -> {
                permissionManager?.checkPermission(PermissionManager.Permission.VIDEO_CALL) {
                    if (it) {
                        presenter.onCallPermissionsGranted(call)
                    }
                }
            }
        }
    }

    override fun launchPendingCall(call: Call) {
        PendingCallFragment.Builder(call) {
            if (call is Call.Video) {
                val fragment = VideoCallFragment.newInstance()
                fragment.arguments = Bundle().apply {
                    putParcelable("call", it)
                }
                fragment.show(parentFragmentManager, "video_call")
            }
        }.show(childFragmentManager)
    }

}