package q19.kenes.widget.ui.presentation.platform

abstract class BasePresenter<View: BaseView> {

    private var isFirstLaunch: Boolean = true

    private var view: View? = null

    fun getView(): View = requireNotNull(view)

    fun attachView(view: View) {
        this.view = view

        if (isFirstLaunch) {
            isFirstLaunch = false

            onFirstViewAttach()
        }
    }

    protected open fun onFirstViewAttach() {
    }

    fun isViewAttached(): Boolean {
        return view != null
    }

    fun detachView() {
        onDestroy()
        view = null
    }

    protected open fun onDestroy() {
    }

}