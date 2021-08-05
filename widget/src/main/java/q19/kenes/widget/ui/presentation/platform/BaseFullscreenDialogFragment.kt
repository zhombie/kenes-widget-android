package q19.kenes.widget.ui.presentation.platform

import android.os.Bundle
import androidx.annotation.LayoutRes

internal abstract class BaseFullscreenDialogFragment<Presenter : BasePresenter<*>> constructor(
    @LayoutRes override val contentLayoutId: Int
) : BaseDialogFragment<Presenter>(contentLayoutId) {

    constructor() : this(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Material_NoActionBar_Fullscreen)

        isCancelable = false
    }

}