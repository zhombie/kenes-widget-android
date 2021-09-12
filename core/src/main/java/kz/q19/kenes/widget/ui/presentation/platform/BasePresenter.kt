package kz.q19.kenes.widget.ui.presentation.platform

import java.lang.ref.WeakReference

internal abstract class BasePresenter<View: BaseView> {

    private var isFirstLaunch: Boolean = true

    private var view: WeakReference<View>? = null

    fun getView(): View = requireNotNull(view?.get())

    fun attachView(view: View) {
        if (isFirstLaunch) {
            isFirstLaunch = false

            this.view = WeakReference(view)

            onFirstViewAttach()
        }

        onAttachView()
    }

    protected open fun onFirstViewAttach() {
    }

    protected open fun onAttachView() {
    }

    fun onViewResumed() {
        onViewResume()
    }

    protected open fun onViewResume() {
    }

    fun isViewAttached(): Boolean {
        return view?.get() != null
    }

    fun detachView() {
        if (isViewAttached()) {
            onDestroy()

            isFirstLaunch = false

            view?.clear()
            view = null
        }
    }

    protected open fun onDestroy() {
    }

}