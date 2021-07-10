package q19.kenes.widget.ui.presentation.home

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.q19.domain.model.knowledge_base.Response
import kz.q19.domain.model.knowledge_base.ResponseGroup
import kz.q19.domain.model.knowledge_base.ResponseInfo
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes_widget.R

internal class ChatBotFragment : BaseFragment(R.layout.fragment_chatbot), ChatBotView {

    companion object {
        private val TAG = ChatBotFragment::class.java.simpleName

        fun newInstance(): ChatBotFragment {
            val fragment = ChatBotFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    private var recyclerView: RecyclerView? = null
    private var editText: AppCompatEditText? = null

    private var presenter: ChatBotPresenter? = null

    private var adapter: ResponseGroupsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = ChatBotPresenter(Database.getInstance(requireContext()))
        presenter?.attachView(this)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        editText = view.findViewById(R.id.editText)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        adapter = ResponseGroupsAdapter(object : ResponseGroupsAdapter.Callback {
            override fun onResponseGroupClicked(responseGroup: ResponseGroup) {
                presenter?.onResponseGroupClicked(responseGroup)
            }

            override fun onResponseClicked(response: Response) {
                presenter?.onResponseClicked(response)
            }

            override fun onGoBackButtonClicked(responseGroup: ResponseGroup) {
                presenter?.onGoBackButtonClicked(responseGroup)
            }
        })
        recyclerView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerView?.adapter = adapter
    }


    /**
     * [ChatBotView] implementation
     */

    override fun showResponseGroups(responseGroups: List<ResponseGroup>) {
//        Log.d(TAG, "responseGroups: $responseGroups")
        activity?.runOnUiThread {
            adapter?.submitList(responseGroups)
        }
    }

    override fun showResponseInfo(responseInfo: ResponseInfo) {
        activity?.runOnUiThread {
        }
    }

}