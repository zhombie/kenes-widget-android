package q19.kenes.widget.ui.presentation.info

import android.os.Bundle
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes_widget.R

internal class InfoFragment : BaseFragment<InfoPresenter>(R.layout.fragment_info) {

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