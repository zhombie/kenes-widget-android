package q19.kenes.widget.ui.presentation.call.text

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import q19.kenes_widget.R

class TextChatFragment : Fragment(R.layout.fragment_text_chat) {

    companion object {
        private val TAG = TextChatFragment::class.java.simpleName

        fun newInstance(): TextChatFragment {
            return TextChatFragment()
        }
    }

    private var listener: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val videoCallButton = view.findViewById<MaterialButton>(R.id.videoCallButton)

        videoCallButton?.setOnClickListener {
            listener?.invoke()
        }
    }

    fun setListener(listener: (() -> Unit)?) {
        this.listener = listener
    }

}