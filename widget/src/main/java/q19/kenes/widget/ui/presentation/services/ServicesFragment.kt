package q19.kenes.widget.ui.presentation.services

import android.os.Bundle
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes_widget.R

internal class ServicesFragment : BaseFragment<ServicesPresenter>(R.layout.fragment_services) {

    companion object {
        private val TAG = ServicesFragment::class.java.simpleName

        fun newInstance(): ServicesFragment {
            val fragment = ServicesFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    override fun createPresenter(): ServicesPresenter {
        return injection.provideServicesPresenter(getCurrentLanguage())
    }

}