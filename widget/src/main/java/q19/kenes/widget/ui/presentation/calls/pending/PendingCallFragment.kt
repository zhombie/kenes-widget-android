package q19.kenes.widget.ui.presentation.calls.pending

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButton
import q19.kenes.widget.ui.presentation.calls.Call
import q19.kenes.widget.ui.presentation.platform.BaseFullscreenDialogFragment
import q19.kenes_widget.R

internal class PendingCallFragment : BaseFullscreenDialogFragment(R.layout.fragment_pending_call),
    PendingCallView {

    companion object {
        private val TAG = PendingCallFragment::class.java.simpleName
    }

    class Builder {
        private var call: Call? = null

        fun setCall(call: Call): Builder {
            this.call = call
            return this
        }

        fun show(fragmentManager: FragmentManager): PendingCallFragment {
            val fragment = PendingCallFragment()
            fragment.arguments = Bundle().apply {
                putParcelable("call", call)
            }
            fragment.show(fragmentManager, TAG)
            return fragment
        }
    }

    init {
        isCancelable = false
    }

    // (MVP) Presenter
    private var presenter: PendingCallPresenter? = null

    // UI Views
    private var cancelButton: MaterialButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val call = arguments?.getParcelable<Call>("call")
            ?: throw IllegalStateException("Where is call?")

        presenter = injection?.providePendingCallPresenter(call, getCurrentLanguage())
        presenter?.attachView(this)
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setWindowAnimations(R.style.Kenes_Widget_Slide)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cancelButton = view.findViewById(R.id.cancelButton)

        cancelButton?.setOnClickListener {
            presenter?.onCancelCall()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        presenter?.detachView()
    }

    /**
     * [PendingCallView] implementation
     */

    override fun navigateToHome() {
        dismiss()
    }

}