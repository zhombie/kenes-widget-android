package q19.kenes_widget.ui.components

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.annotation.*
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.core.errors.ViewHolderViewTypeException
import q19.kenes_widget.model.Configs
import q19.kenes_widget.model.Language
import q19.kenes_widget.model.OperatorCall
import q19.kenes_widget.util.Logger.debug
import q19.kenes_widget.util.inflate
import q19.kenes_widget.util.px

internal class OperatorCallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var contentView: LinearLayout? = null

    private var audioCallButton: LinearLayout? = null
    private var videoCallButton: LinearLayout? = null

    private var titleView: TextView? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: CallScopesAdapter? = null

    var callback: Callback? = null

    init {
        orientation = VERTICAL
    }

    private fun buildContentView() {
        val scrollView = ScrollView(context)
        scrollView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        contentView = LinearLayout(context)
        contentView?.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        contentView?.setPadding(
            resources.getDimensionPixelOffset(R.dimen.kenes_horizontal_spacing),
            20.px,
            resources.getDimensionPixelOffset(R.dimen.kenes_horizontal_spacing),
            0
        )
        contentView?.orientation = VERTICAL

        scrollView.addView(contentView)

        addView(scrollView)
    }

    private fun buildButtonView(
        @DrawableRes background: Int,
        @DrawableRes icon: Int,
        titleHexColor: String,
        @StringRes titleText: Int,
        definitionHexColor: String,
        @StringRes definitionText: Int
    ): LinearLayout {
        // Parent view
        val callButton = LinearLayout(context)
        callButton.setPadding(
            context.resources.getDimensionPixelOffset(R.dimen.kenes_call_button_padding),
            context.resources.getDimensionPixelOffset(R.dimen.kenes_call_button_padding),
            context.resources.getDimensionPixelOffset(R.dimen.kenes_call_button_padding),
            context.resources.getDimensionPixelOffset(R.dimen.kenes_call_button_padding)
        )
        callButton.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        callButton.orientation = VERTICAL
        callButton.background = ResourcesCompat.getDrawable(
            resources,
            background,
            context.theme
        )
        callButton.isClickable = true
        callButton.isFocusable = true

        // Icon
        val callIconView = ImageView(context)
        val callIconViewLayoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            context.resources.getDimensionPixelOffset(R.dimen.kenes_call_icon_height)
        )
        callIconViewLayoutParams.bottomMargin = 15
        callIconView.layoutParams = callIconViewLayoutParams
        callIconView.adjustViewBounds = true
        callIconView.setImageResource(icon)

        callButton.addView(callIconView)

        // Title
        val titleView = TextView(context)
        titleView.layoutParams =
            MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        titleView.setFont(R.font.helvetica_neue_bold, Typeface.BOLD)
        titleView.letterSpacing = 0.015F
        titleView.setText(titleText)
        titleView.setTextColor(Color.parseColor(titleHexColor))
        titleView.textSize = 26F

        callButton.addView(titleView)

        // Definition
        val definitionView = TextView(context)
        val definitionViewLayoutParams =
            MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        definitionViewLayoutParams.topMargin = 3
        definitionView.layoutParams = definitionViewLayoutParams
        definitionView.setFont(R.font.helvetica_roman)
        definitionView.letterSpacing = 0.01F
        definitionView.setText(definitionText)
        definitionView.setTextColor(Color.parseColor(definitionHexColor))
        definitionView.textSize = 14F

        callButton.addView(definitionView)

        return callButton
    }

    fun showCallButton(operatorCall: OperatorCall) {
        setCallButtonVisibility(operatorCall, true)
    }

    fun hideCallButton(operatorCall: OperatorCall) {
        setCallButtonVisibility(operatorCall, false)
    }

    private fun setCallButtonVisibility(operatorCall: OperatorCall, isVisible: Boolean) {
        if ((operatorCall == OperatorCall.AUDIO || operatorCall == OperatorCall.VIDEO) &&
            contentView == null
        ) {
            buildContentView()
        }

        if (operatorCall == OperatorCall.AUDIO) {
            if (isVisible) {
                if (!contentView.isViewAdded(audioCallButton)) {
                    audioCallButton = buildButtonView(
                        background = R.drawable.kenes_bg_orange,
                        icon = R.drawable.kenes_ic_man_raising_hand,
                        titleHexColor = "#FFA000",
                        titleText = R.string.kenes_audio_call,
                        definitionHexColor = "#95FFA000",
                        definitionText = R.string.kenes_audio_call_to_operator
                    )

                    audioCallButton?.setOnClickListener {
                        callback?.onOperatorCallClicked(operatorCall)
                    }

                    contentView?.addView(audioCallButton)
                }
            } else {
                if (contentView.isViewAdded(audioCallButton)) {
                    contentView?.removeView(audioCallButton)
                }
            }
        } else if (operatorCall == OperatorCall.VIDEO) {
            if (isVisible) {
                if (!contentView.isViewAdded(videoCallButton)) {
                    videoCallButton = buildButtonView(
                        background = R.drawable.kenes_bg_green,
                        icon = R.drawable.kenes_ic_female_technologist,
                        titleHexColor = "#4BB34B",
                        titleText = R.string.kenes_video_call,
                        definitionHexColor = "#954BB34B",
                        definitionText = R.string.kenes_video_call_to_operator
                    )

                    videoCallButton?.setOnClickListener {
                        callback?.onOperatorCallClicked(operatorCall)
                    }

                    if (contentView.isViewAdded(audioCallButton)) {
                        (videoCallButton?.layoutParams as? MarginLayoutParams)?.topMargin = 15
                    }

                    contentView?.addView(videoCallButton)
                }
            } else {
                if (contentView.isViewAdded(videoCallButton)) {
                    contentView?.removeView(videoCallButton)
                }
            }
        }
    }

    private fun ViewGroup?.isViewAdded(view: View?): Boolean {
        if (this == null) return false
        return view != null && this.indexOfChild(view) != -1
    }

    fun setCallButtonEnabled(operatorCall: OperatorCall) {
        setCallButtonEnabled(operatorCall, true)
    }

    fun setCallButtonDisabled(operatorCall: OperatorCall) {
        setCallButtonEnabled(operatorCall, false)
    }

    private fun setCallButtonEnabled(operatorCall: OperatorCall, isEnabled: Boolean) {
        if (operatorCall == OperatorCall.AUDIO) {
            if (audioCallButton?.isEnabled == isEnabled) return
            audioCallButton?.isEnabled = isEnabled
        } else if (operatorCall == OperatorCall.VIDEO) {
            if (videoCallButton?.isEnabled == isEnabled) return
            videoCallButton?.isEnabled = isEnabled
        }
    }

    fun showCallScopes(parentCallScope: Configs.CallScope?, callScopes: List<Configs.CallScope>, language: Language) {
        if (titleView == null) {
            titleView = TextView(context)
            titleView?.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            titleView?.setTextColor(ContextCompat.getColor(context, R.color.kenes_dark_black))
            titleView?.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20.px.toFloat())
            titleView?.setPadding(
                resources.getDimensionPixelOffset(R.dimen.kenes_horizontal_spacing),
                20.px,
                resources.getDimensionPixelOffset(R.dimen.kenes_horizontal_spacing),
                20.px
            )
        }

        // TODO: Set font only once
        titleView?.setFont(R.font.helvetica_black)

        if (parentCallScope == null) {
            titleView?.setText(R.string.kenes_call_with_operator)

            adapter?.isFooterEnabled = false
        } else {
            titleView?.text = parentCallScope.title.get(language)

            adapter?.isFooterEnabled = true
        }

        if (!isViewAdded(titleView)) {
            addView(titleView)
        }

        if (recyclerView == null) {
            recyclerView = RecyclerView(context)
            val layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )

            layoutParams.setMargins(
                resources.getDimensionPixelOffset(R.dimen.kenes_horizontal_spacing),
                0,
                resources.getDimensionPixelOffset(R.dimen.kenes_horizontal_spacing),
                20.px
            )

            recyclerView?.layoutParams = layoutParams

            recyclerView?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                val drawable = GradientDrawable()
//                drawable.setStroke(1.px, Color.parseColor("#EBEEF5"))
//                drawable.cornerRadius = 10.px.toFloat()
//                recyclerView?.foreground = drawable
//            }
        }

        if (!isViewAdded(recyclerView)) {
            addView(recyclerView)
        }

        if (adapter == null) {
            adapter = CallScopesAdapter(language, object : CallScopesAdapter.Callback {
                override fun onCallScopeClicked(callScope: Configs.CallScope) {
                    callback?.onCallScopeClicked(callScope)
                }

                override fun onCallScopeBackClicked() {
                    callback?.onCallScopeBackClicked()
                }
            })

            recyclerView?.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            recyclerView?.adapter = adapter

            recyclerView?.addItemDecoration(CallScopesAdapterItemDecoration(10.px.toFloat()))
        }

        adapter?.callScopes = callScopes
    }

    fun removeListener(operatorCall: OperatorCall) {
        if (operatorCall == OperatorCall.AUDIO) {
            audioCallButton?.setOnClickListener(null)
        } else if (operatorCall == OperatorCall.VIDEO) {
            videoCallButton?.setOnClickListener(null)
        }
    }

    interface Callback {
        fun onCallScopeClicked(callScope: Configs.CallScope)
        fun onCallScopeBackClicked()

        fun onOperatorCallClicked(operatorCall: OperatorCall)
    }

}


private class CallScopesAdapter(
    private val language: Language,
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "CallScopesAdapter"

        private val LAYOUT_CALL_SCOPE = R.layout.kenes_cell_scope

        const val VIEW_TYPE_CALL_SCOPE = 100
        const val VIEW_TYPE_FOOTER = 101
    }

    var callScopes: List<Configs.CallScope> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var isFooterEnabled: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemViewType(position: Int): Int {
        return if (isFooterEnabled) {
            if (position == itemCount - 1) {
                VIEW_TYPE_FOOTER
            }  else {
                VIEW_TYPE_CALL_SCOPE
            }
        } else {
            VIEW_TYPE_CALL_SCOPE
        }
    }

    override fun getItemCount(): Int = callScopes.size + if (isFooterEnabled) 1 else 0

    fun getItem(position: Int): Configs.CallScope? {
        if (position < 0) {
            return null
        }
        return callScopes[position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CALL_SCOPE -> CallScopeViewHolder(parent.inflate(LAYOUT_CALL_SCOPE))
            VIEW_TYPE_FOOTER -> FooterViewHolder(parent.inflate(LAYOUT_CALL_SCOPE))
            else -> throw ViewHolderViewTypeException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CallScopeViewHolder) {
            val item = getItem(position)
            item?.let { holder.bind(it) }
        } else if (holder is FooterViewHolder) {
            holder.bind()
        }
    }

    private inner class CallScopeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var textView = view.findViewById<AppCompatTextView>(R.id.textView)

        fun bind(callScope: Configs.CallScope) {
            textView.text = callScope.title.get(language)

            debug(TAG, "callScope: $callScope")

            itemView.setOnClickListener { callback.onCallScopeClicked(callScope) }
        }
    }

    private inner class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var textView = view.findViewById<AppCompatTextView>(R.id.textView)

        fun bind() {
            textView.setText(R.string.kenes_back)

            itemView.setOnClickListener { callback.onCallScopeBackClicked() }
        }
    }

    interface Callback {
        fun onCallScopeClicked(callScope: Configs.CallScope)
        fun onCallScopeBackClicked()
    }

}


private class CallScopesAdapterItemDecoration(
    private val cornerRadius: Float
) : RecyclerView.ItemDecoration() {

    private var paint: Paint = Paint()

    /**
     * Enum describes mode round corners
     */
    enum class RoundMode {
        TOP,
        BOTTOM,
        ALL,
        NONE
    }

    /**
     * [ViewOutlineProvider] witch works with [RoundMode]
     * @param outlineRadius corner radius
     * @param roundMode mode for corners
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    class RoundOutlineProvider(
        var outlineRadius: Float = 0F,
        var roundMode: RoundMode = RoundMode.NONE
    ) : ViewOutlineProvider() {

        private val topOffset
            get() = when (roundMode) {
                RoundMode.ALL, RoundMode.TOP -> 0
                RoundMode.NONE, RoundMode.BOTTOM -> cornerRadius.toInt()
            }

        private val bottomOffset
            get() = when (roundMode) {
                RoundMode.ALL, RoundMode.BOTTOM -> 0
                RoundMode.NONE, RoundMode.TOP -> cornerRadius.toInt()
            }

        private val cornerRadius
            get() = if (roundMode == RoundMode.NONE) {
                0f
            } else {
                outlineRadius
            }

        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(
                0,
                0 - topOffset,
                view.width,
                view.height + bottomOffset,
                cornerRadius
            )
        }
    }

    init {
        paint.color = Color.parseColor("#EBEEF5")
//        paint.color = Color.parseColor("#555555")
        paint.strokeWidth = 1.px.toFloat()
        paint.style = Paint.Style.STROKE
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = (parent.adapter as? CallScopesAdapter?) ?: return

//        val childCount = parent.childCount

        val itemCount = adapter.itemCount

        if (parent.childCount == 1) {
            val child = parent.getChildAt(0)
            c.drawRoundRect(
                child.left.toFloat(),
                child.top.toFloat(),
                child.right.toFloat(),
                child.bottom.toFloat(),
                10.px.toFloat(),
                10.px.toFloat(),
                paint
            )
        } else {
            for (index in 0 until itemCount) {
                val child = parent.getChildAt(index)

                val layoutParams = child.layoutParams as RecyclerView.LayoutParams

                val viewType = adapter.getItemViewType(index)

                if (viewType == CallScopesAdapter.VIEW_TYPE_FOOTER) {
                    val path = getPathOfRoundedRectF(
                        child,
                        topLeftRadius = 10.px.toFloat(),
                        topRightRadius = 10.px.toFloat(),
                        bottomLeftRadius = 10.px.toFloat(),
                        bottomRightRadius = 10.px.toFloat()
                    )
                    c.drawPath(path, paint)
                } else if (viewType == CallScopesAdapter.VIEW_TYPE_CALL_SCOPE) {
                    // Divider
                    if (itemCount > 3 && index < itemCount - 1) {
                        val startX = parent.paddingStart
                        val stopX = parent.width
                        val y = child.bottom + layoutParams.bottomMargin
                        c.drawLine(
                            startX.toFloat(),
                            y.toFloat(),
                            stopX.toFloat(),
                            y.toFloat(),
                            paint
                        )
                    }

                    val relationalItemCount = if (adapter.isFooterEnabled) {
                        itemCount - 1
                    } else {
                        itemCount
                    }

                    if (relationalItemCount == 1) {
                        val path = getPathOfRoundedRectF(
                            child,
                            topLeftRadius = 10.px.toFloat(),
                            topRightRadius = 10.px.toFloat(),
                            bottomLeftRadius = 10.px.toFloat(),
                            bottomRightRadius = 10.px.toFloat()
                        )
                        c.drawPath(path, paint)
                    } else {
                        when (index) {
                            0 -> {
                                val path = getPathOfRoundedRectF(
                                    child,
                                    topLeftRadius = 10.px.toFloat(),
                                    topRightRadius = 10.px.toFloat()
                                )
                                c.drawPath(path, paint)
                            }
                            relationalItemCount - 1 -> {
                                val path = getPathOfRoundedRectF(
                                    child,
                                    bottomLeftRadius = 10.px.toFloat(),
                                    bottomRightRadius = 10.px.toFloat()
                                )
                                c.drawPath(path, paint)
                            }
                            else -> {
                                val path = Path()
                                path.moveTo(child.left.toFloat(), child.top.toFloat())
                                path.lineTo(child.left.toFloat(), child.bottom.toFloat())
                                path.moveTo(child.right.toFloat(), child.top.toFloat())
                                path.lineTo(child.right.toFloat(), child.bottom.toFloat())
                                path.close()
                                c.drawPath(path, paint)
                            }
                        }
                    }
                }
            }
        }
    }

    fun getPathOfRoundedRectF(
        view: View,
        topLeftRadius: Float = 0f,
        topRightRadius: Float = 0f,
        bottomRightRadius: Float = 0f,
        bottomLeftRadius: Float = 0f
    ): Path {
        val tlRadius = topLeftRadius.coerceAtLeast(0f)
        val trRadius = topRightRadius.coerceAtLeast(0f)
        val brRadius = bottomRightRadius.coerceAtLeast(0f)
        val blRadius = bottomLeftRadius.coerceAtLeast(0f)

        with(Path()) {
            moveTo(view.left + tlRadius, view.top.toFloat())

            //setup top border
            lineTo(view.right - trRadius, view.top.toFloat())

            //setup top-right corner
            arcTo(
                RectF(
                    view.right - trRadius * 2f,
                    view.top.toFloat(),
                    view.right.toFloat(),
                    view.top + trRadius * 2f
                ),
                -90f,
                90f
            )

            //setup right border
            lineTo(view.right.toFloat(), view.bottom - trRadius - brRadius)

            //setup bottom-right corner
            arcTo(
                RectF(
                    view.right - brRadius * 2f,
                    view.bottom - brRadius * 2f,
                    view.right.toFloat(),
                    view.bottom.toFloat()
                ),
                0f,
                90f
            )

            //setup bottom border
            lineTo(view.left + blRadius, view.bottom.toFloat())

            //setup bottom-left corner
            arcTo(
                RectF(
                    view.left.toFloat(),
                    view.bottom - blRadius * 2f,
                    view.left + blRadius * 2f,
                    view.bottom.toFloat()
                ),
                90f,
                90f
            )

            //setup left border
            lineTo(view.left.toFloat(), view.top + tlRadius)

            //setup top-left corner
            arcTo(
                RectF(
                    view.left.toFloat(),
                    view.top.toFloat(),
                    view.left + tlRadius * 2f,
                    view.top + tlRadius * 2f
                ),
                180f,
                90f
            )

            close()

            return this
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val adapter = (parent.adapter as? CallScopesAdapter?) ?: return

        val itemCount = adapter.itemCount

        val position = parent.layoutManager?.getPosition(view) ?: -1

        val viewType = adapter.getItemViewType(position)

        // Offset between elements
        when (viewType) {
            CallScopesAdapter.VIEW_TYPE_FOOTER -> {
                outRect.top = 15.px
            }
            CallScopesAdapter.VIEW_TYPE_CALL_SCOPE -> {
                outRect.setEmpty()
            }
            else -> {
                outRect.setEmpty()
            }
        }

        // Draw rounded background
        if (cornerRadius.compareTo(0f) != 0) {
            val roundMode = if (viewType == CallScopesAdapter.VIEW_TYPE_FOOTER) {
                RoundMode.ALL
            } else if (viewType == CallScopesAdapter.VIEW_TYPE_CALL_SCOPE) {
                val relationalItemCount = if (adapter.isFooterEnabled) {
                    itemCount - 1
                } else {
                    itemCount
                }

                if (relationalItemCount == 1) {
                    RoundMode.ALL
                } else {
                    when (parent.getChildAdapterPosition(view)) {
                        0 -> RoundMode.TOP
                        relationalItemCount - 1 -> RoundMode.BOTTOM
                        else -> RoundMode.NONE
                    }
                }
            } else {
                RoundMode.NONE
            }

            val outlineProvider = view.outlineProvider
            if (outlineProvider is RoundOutlineProvider) {
                outlineProvider.roundMode = roundMode
                view.invalidateOutline()
            } else {
                view.outlineProvider = RoundOutlineProvider(cornerRadius, roundMode)
                view.clipToOutline = true
            }
        }
    }

}