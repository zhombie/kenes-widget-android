package q19.kenes.widget.ui.presentation.calls

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes.widget.ui.presentation.calls.media.VideoCallFragment
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes.widget.util.Logger
import q19.kenes_widget.R

class CallsFragment : BaseFragment(R.layout.fragment_calls), CallsView, CallsAdapter.Callback {

    companion object {
        private val TAG = CallsFragment::class.java.simpleName

        fun newInstance(): CallsFragment {
            val fragment = CallsFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    private var recyclerView: RecyclerView? = null

    private var presenter: CallsPresenter? = null

    private var callsAdapter: CallsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = context.injection?.provideCallsPresenter()
        presenter?.attachView(this)
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

    private fun setupRecyclerView() {
        callsAdapter = CallsAdapter(this)
        recyclerView?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView?.adapter = callsAdapter
    }

    /**
     * [CallsAdapter.Callback] implementation
     */

    override fun onCallClicked(call: Call) {
        presenter?.onCallClicked(call)
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