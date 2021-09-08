package q19.kenes.widget.ui.presentation.call

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kz.q19.utils.view.inflate
import q19.kenes_widget.R

internal class CallsHeaderAdapter constructor(
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = CallsHeaderAdapter::class.java.simpleName
    }

    var isToolbarVisible: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(R.layout.kenes_cell_calls_header))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind()
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val toolbar = view.findViewById<LinearLayout>(R.id.toolbar)
        private val backButton = view.findViewById<MaterialButton>(R.id.backButton)

        fun bind() {
            if (isToolbarVisible) {
                toolbar.visibility = View.VISIBLE
            } else {
                toolbar.visibility = View.GONE
            }

            backButton.setOnClickListener { callback.onBackPressed() }
        }

    }

    interface Callback {
        fun onBackPressed()
    }

}