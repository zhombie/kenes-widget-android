package q19.kenes_widget.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.model.RatingButton

internal class RatingAdapter(
    private val ratingButtons: List<RatingButton>,
    private val callback: (ratingButton: RatingButton) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val LAYOUT_RATING = R.layout.kenes_cell_rating
    }

    init {
        setHasStableIds(true)
    }

    private var selectedRatingButtonPosition: Int = -1

    fun getSelectedRatingButton(): RatingButton? {
        return if (selectedRatingButtonPosition > -1 && selectedRatingButtonPosition < ratingButtons.size) {
            ratingButtons[selectedRatingButtonPosition]
        } else {
            null
        }
    }

    override fun getItemId(position: Int): Long {
        val ratingButton = ratingButtons[position]
        return ratingButton.chatId + ratingButton.rating
    }

    override fun getItemCount(): Int = ratingButtons.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(LAYOUT_RATING, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(ratingButtons[position], callback)
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var textView: TextView? = null

        init {
            textView = view.findViewById(R.id.textView)
        }

        fun bind(
            ratingButton: RatingButton,
            callback: (ratingButton: RatingButton) -> Unit
        ) {
            textView?.isActivated = selectedRatingButtonPosition == adapterPosition

            textView?.text = ratingButton.title

            textView?.setOnClickListener {
                val tempPosition = selectedRatingButtonPosition

                Log.d("LOL", "selected -> tempPosition:" + tempPosition + ", adapterPosition: " + adapterPosition)

                selectedRatingButtonPosition = adapterPosition
                notifyItemChanged(tempPosition)
                notifyItemChanged(selectedRatingButtonPosition)
                callback(ratingButton)
            }
        }
    }

}