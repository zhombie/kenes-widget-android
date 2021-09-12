package kz.q19.kenes.widget.ui.presentation.platform

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

internal abstract class BaseResourceFragment constructor(
    @LayoutRes contentLayoutId: Int
) : Fragment(contentLayoutId) {

    fun getColor(@ColorRes resId: Int): Int =
        ContextCompat.getColor(requireContext(), resId)

    fun getColorStateList(@ColorRes resId: Int): ColorStateList? =
        ContextCompat.getColorStateList(requireContext(), resId)

    fun getDrawable(@DrawableRes resId: Int): Drawable? =
        AppCompatResources.getDrawable(requireContext(), resId)

}