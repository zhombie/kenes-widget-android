package q19.kenes.widget.ui.presentation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

internal class ViewPagerAdapter constructor(
    fragmentActivity: FragmentActivity,
    private val fragments: List<Fragment>
) : FragmentStateAdapter(fragmentActivity) {

    fun getFragment(position: Int): Fragment? {
        if (fragments.isEmpty()) return null
        return if (position < fragments.size) {
            fragments[position]
        } else {
            null
        }
    }

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

}