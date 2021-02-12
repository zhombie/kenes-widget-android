package q19.kenes.widget.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.q19.domain.model.keyboard.button.RateButton
import kz.q19.utils.view.inflate
import q19.kenes_widget.R

internal class FeedbackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val titleView: TextView
    private val ratingView: RecyclerView
    private val rateButton: AppCompatButton

    private var selectedRateButton: RateButton? = null

    init {
        val view = inflate(context, R.layout.kenes_view_feedback, this)

        titleView = view.findViewById(R.id.titleView)
        ratingView = view.findViewById(R.id.ratingView)
        rateButton = view.findViewById(R.id.rateButton)

        ratingView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    fun setDefaultState() {
        rateButton.isEnabled = false
        titleView.text = null
        ratingView.adapter = null
    }

    fun setTitle(title: String) {
        titleView.text = title
    }

    fun setRatingButtons(rateButtons: List<RateButton>) {
        val ratingAdapter = RatingAdapter(rateButtons) {
            selectedRateButton = it

            if (selectedRateButton != null) {
                rateButton.isEnabled = true
            }
        }

        ratingView.adapter = ratingAdapter

        ratingAdapter.notifyDataSetChanged()
    }

    fun setOnRateButtonClickListener(callback: (rateButton: RateButton) -> Unit) {
        rateButton.setOnClickListener {
            callback(selectedRateButton ?: return@setOnClickListener)

            setDefaultState()

            selectedRateButton = null
        }
    }

    fun clear() {
        ratingView.adapter = null
    }

}

private class RatingAdapter(
    private val rateButtons: List<RateButton>,
    private val callback: (rateButton: RateButton) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val LAYOUT_RATING = R.layout.kenes_cell_rating
    }

    init {
        setHasStableIds(true)
    }

    private var selectedRatingButtonPosition: Int = -1

    fun getSelectedRatingButton(): RateButton? {
        return if (selectedRatingButtonPosition > -1 && selectedRatingButtonPosition < rateButtons.size) {
            rateButtons[selectedRatingButtonPosition]
        } else {
            null
        }
    }

    override fun getItemId(position: Int): Long {
        val ratingButton = rateButtons[position]
        return ratingButton.chatId + ratingButton.rating
    }

    override fun getItemCount(): Int = rateButtons.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(LAYOUT_RATING))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(rateButtons[position], callback)
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<TextView>(R.id.textView)

        fun bind(
            rateButton: RateButton,
            callback: (ratingButton: RateButton) -> Unit
        ) {
            textView?.isActivated = selectedRatingButtonPosition == absoluteAdapterPosition

            textView?.text = rateButton.text

            textView?.setOnClickListener {
                val tempPosition = selectedRatingButtonPosition

                selectedRatingButtonPosition = absoluteAdapterPosition
                notifyItemChanged(tempPosition)
                notifyItemChanged(selectedRatingButtonPosition)
                callback(rateButton)
            }
        }
    }

}