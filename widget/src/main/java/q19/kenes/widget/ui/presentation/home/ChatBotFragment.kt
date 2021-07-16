package q19.kenes.widget.ui.presentation.home

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.domain.model.Response
import q19.kenes.widget.domain.model.ResponseGroup
import q19.kenes.widget.ui.components.MessageInputView
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes_widget.R

internal class ChatBotFragment : BaseFragment(R.layout.fragment_chatbot), ChatBotView, ChatBotFragmentDelegate {

    companion object {
        private val TAG = ChatBotFragment::class.java.simpleName

        fun newInstance(): ChatBotFragment {
            val fragment = ChatBotFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    private var recyclerView: RecyclerView? = null
    private var messageInputView: MessageInputView? = null

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
        messageInputView = view.findViewById(R.id.messageInputView)

        setupRecyclerView()
    }

    override fun onDestroy() {
        presenter?.detachView()
        super.onDestroy()
    }

    private fun setupRecyclerView() {
        adapter = ResponseGroupsAdapter(object : ResponseGroupsAdapter.Callback {
            override fun onResponseGroupClicked(responseGroup: ResponseGroup) {
                presenter?.onResponseGroupClicked(responseGroup)
            }

            override fun onResponseGroupChildClicked(child: ResponseGroup.Child) {
                presenter?.onResponseGroupChildClicked(child)
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
        adapter?.submitList(responseGroups)
    }

    override fun showResponse(response: Response) {
        activity?.runOnUiThread {
        }
    }

    /**
     * [ChatBotFragmentDelegate] implementation
     */

    override fun onScreenRenavigate() {
        Logger.debug(TAG, "onScreenRenavigate()")

        with(recyclerView?.layoutManager) {
            if (this is LinearLayoutManager) {
                if (findFirstCompletelyVisibleItemPosition() == 0) {
                    presenter?.onResetDataRequested()
                } else {
                    recyclerView?.smoothScrollToPosition(0)
                }
            }
        }
    }

}