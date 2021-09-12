package kz.q19.kenes.widget.ui.presentation.platform

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kz.q19.kenes.widget.di.Injection

internal abstract class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    internal val injection: Injection
        get() = Injection.getInstance(requireContext())

}