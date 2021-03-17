package q19.kenes.widget.ui.presentation.platform

import android.content.Context
import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment
import q19.kenes.widget.di.Injection

open class BaseDialogFragment constructor(
    @LayoutRes contentLayoutId: Int
) : DialogFragment(contentLayoutId) {

    constructor() : this(0)

    internal val Context?.injection: Injection?
        get() = if (context == null) null else Injection.getInstance(requireContext())

}