package kz.q19.kenes.widget.ui.presentation.info

import android.os.Bundle
import kz.q19.kenes.widget.R
import kz.q19.kenes.widget.ui.presentation.platform.BaseFragment

internal class InfoFragment : BaseFragment<InfoPresenter>(R.layout.kenes_fragment_info) {

    companion object {
        private val TAG = InfoFragment::class.java.simpleName

        fun newInstance(): InfoFragment {
            val fragment = InfoFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    override fun createPresenter(): InfoPresenter {
        return injection.provideInfoPresenter(getCurrentLanguage())
    }

}