package q19.kenes.widget.ui.presentation.home

import android.os.Bundle
import android.util.Log
import android.view.View
import kz.q19.domain.model.configs.Configs
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes_widget.R

class ChatBotFragment : BaseFragment(R.layout.fragment_chatbot), ChatBotView {

    companion object {
        private val TAG = ChatBotFragment::class.java.simpleName

        fun newInstance(): ChatBotFragment {
            val fragment = ChatBotFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    private var presenter: ChatBotPresenter? = null

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
    }

    override fun showMediaCalls(calls: List<Configs.Call>) {
        Log.d(TAG, "calls: $calls")
    }

}