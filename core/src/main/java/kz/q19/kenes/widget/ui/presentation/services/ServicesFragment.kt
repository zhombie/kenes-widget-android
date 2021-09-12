package kz.q19.kenes.widget.ui.presentation.services

import android.os.Bundle
import kz.q19.kenes.widget.R
import kz.q19.kenes.widget.ui.presentation.platform.BaseFragment

internal class ServicesFragment : BaseFragment<ServicesPresenter>(R.layout.kenes_fragment_services) {

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