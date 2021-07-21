package q19.kenes.widget.ui.presentation.calls

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.core.permission.PermissionManager
import q19.kenes.widget.ui.presentation.HomeFragmentDelegate
import q19.kenes.widget.ui.presentation.calls.media.VideoCallFragment
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

    // RecyclerView adapter
    private var callsAdapter: CallsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = injection?.provideCallsPresenter(getCurrentLanguage())
        presenter?.attachView(this)

        permissionManager = PermissionManager(requireActivity())
    }

    override fun onResume() {
        super.onResume()
        Logger.debug(TAG, "onResume()")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)

        setupRecyclerView()
    }

    override fun onDestroy() {
        super.onDestroy()

        permissionManager?.removeAllListeners()
        permissionManager = null

        presenter?.detachView()
    }

    private fun setupRecyclerView() {
        callsAdapter = CallsAdapter(this)
        recyclerView?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView?.adapter = callsAdapter
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
        permissionManager?.checkPermission(PermissionManager.Permission.VIDEO_CALL) {
            if (it) {
                presenter?.onCallClicked(call)
            }
        }
    }

    /**
     * [CallsView] implementation
     */

    override fun showMediaCalls(calls: List<Call>) {
        Logger.debug(TAG, "calls: $calls")
        callsAdapter?.calls = calls
    }

    override fun launchVideoCall(call: Call) {
        VideoCallFragment.newInstance()
            .show(childFragmentManager, null)
    }

}