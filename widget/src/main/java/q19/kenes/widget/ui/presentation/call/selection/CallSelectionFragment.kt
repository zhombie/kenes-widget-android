package q19.kenes.widget.ui.presentation.call.selection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import q19.kenes.widget.ui.presentation.call.Call
import q19.kenes.widget.ui.presentation.platform.BaseBottomSheetDialogFragment
import q19.kenes_widget.R

internal class CallSelectionFragment : BaseBottomSheetDialogFragment() {

    companion object {
        fun newInstance(callSelection: CallSelection): CallSelectionFragment {
            val fragment = CallSelectionFragment()
            fragment.arguments = Bundle().apply {
                putParcelable("call_selection", callSelection)
            }
            return fragment
        }
    }

    private var titleView: MaterialTextView? = null
    private var callsView: RecyclerView? = null

    private var callSelection: CallSelection? = null

    private var callSelectionAdapter: CallSelectionAdapter? = null

    private var listener: ((call: Call) -> Unit)? = null

    fun setListener(listener: ((call: Call) -> Unit)?): CallSelectionFragment {
        this.listener = listener
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        callSelection = requireArguments().getParcelable("call_selection")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_call_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleView = view.findViewById(R.id.titleView)
        callsView = view.findViewById(R.id.callsView)

        titleView?.text = callSelection?.title

        callSelectionAdapter = CallSelectionAdapter {
            dismiss()

            setFragmentResult("request_key.call_selection", bundleOf("call" to it))
        }
        callsView?.adapter = callSelectionAdapter
        callsView?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        callSelectionAdapter?.calls = callSelection?.calls ?: emptyList()
    }

}