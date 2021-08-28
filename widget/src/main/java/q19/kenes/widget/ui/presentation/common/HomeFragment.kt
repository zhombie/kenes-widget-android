package q19.kenes.widget.ui.presentation.common

import androidx.annotation.LayoutRes
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes.widget.ui.presentation.platform.BasePresenter

internal abstract class HomeFragment<Presenter : BasePresenter<*>> constructor(
    @LayoutRes contentLayoutId: Int
) : BaseFragment<Presenter>(contentLayoutId)