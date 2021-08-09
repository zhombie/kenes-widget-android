package q19.kenes.widget.ui.presentation.calls.pending

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButton
import q19.kenes.widget.ui.presentation.calls.Call
import q19.kenes.widget.ui.presentation.platform.BaseFullscreenDialogFragment
import q19.kenes.widget.util.AlertDialogBuilder
import q19.kenes_widget.R

internal class PendingCallFragment :
    BaseFullscreenDialogFragment<PendingCallPresenter>(R.layout.fragment_pending_call),
    PendingCallView {

    companion object {
        private val TAG = PendingCallFragment::class.java.simpleName
    }

    class Builder constructor(
        private var call: Call,
        private val onCallStarted: (call: Call) -> Unit
    ) {

        fun setCall(call: Call): Builder {
            this.call = call
            return this
        }

        fun show(fragmentManager: FragmentManager): PendingCallFragment {
            val fragment = PendingCallFragment()
            fragment.arguments = Bundle().apply {
                putParcelable("call", call)
            }
            fragment.setOnCallStarted(onCallStarted)
            fragment.show(fragmentManager, TAG)
            return fragment
        }
    }

    // UI Views
    private var cancelButton: MaterialButton? = null

    private var onCallStarted: ((call: Call) -> Unit)? = null

    private fun setOnCallStarted(onCallStarted: (call: Call) -> Unit) {
        this.onCallStarted = onCallStarted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter.attachView(this)
    }

    override fun createPresenter(): PendingCallPresenter {
        val call = arguments?.getParcelable<Call>("call")
            ?: throw IllegalStateException("Where is call?")

        return injection.providePendingCallPresenter(call, getCurrentLanguage())
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setWindowAnimations(R.style.Kenes_Widget_Slide)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cancelButton = view.findViewById(R.id.cancelButton)

        cancelButton?.setOnClickListener {
            AlertDialogBuilder(requireContext())
                .setTitle(R.string.kenes_attention)
                .setMessage(R.string.kenes_cancel_call)
                .setNegativeButton(R.string.kenes_no) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.kenes_yes) { dialog, _ ->
                    dialog.dismiss()
                    presenter.onCancelCall()
                }
                .show()
        }
    }

    /**
     * [PendingCallView] implementation
     */

    override fun showNoOnlineCallAgentsMessage(text: String?) {
        activity?.runOnUiThread {
            AlertDialogBuilder(requireContext())
                .setCancelable(false)
                .setTitle(R.string.kenes_attention)
                .setMessage(text ?: "Sorry, no online operators")
                .setPositiveButton(R.string.kenes_ok) { dialog, _ ->
                    dialog.dismiss()
                    dismiss()
                }
                .show()
        }
    }

    override fun navigateToHome() {
        dismiss()
    }

    override fun navigateToCall(call: Call) {
        dismiss()

        onCallStarted?.invoke(call)
    }

}