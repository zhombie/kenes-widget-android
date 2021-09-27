package kz.q19.kenes.widget.ui.presentation.common

import androidx.annotation.LayoutRes
import kz.q19.kenes.widget.ui.platform.BaseFragment
import kz.q19.kenes.widget.ui.platform.BasePresenter

internal abstract class HomeFragment<Presenter : BasePresenter<*>> constructor(
    @LayoutRes contentLayoutId: Int
) : BaseFragment<Presenter>(contentLayoutId) {

    interface Listener {
        fun onVerticalScroll(scrollYPosition: Int) {}
    }

}