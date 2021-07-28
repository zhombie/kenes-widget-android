package q19.kenes.widget.ui.presentation.platform

import java.lang.ref.WeakReference

internal abstract class BasePresenter<View: BaseView> {

    private var isFirstLaunch: Boolean = true

    private var view: WeakReference<View>? = null

    fun getView(): View = requireNotNull(view?.get())

    fun attachView(view: View) {
        this.view = WeakReference(view)

        if (isFirstLaunch) {
            isFirstLaunch = false

            onFirstViewAttach()
        }
    }

    protected open fun onFirstViewAttach() {
    }

    fun isViewAttached(): Boolean {
        return view?.get() != null
    }

    fun detachView() {
        onDestroy()

        isFirstLaunch = false

        view?.clear()
        view = null
    }

    protected open fun onDestroy() {
    }

}