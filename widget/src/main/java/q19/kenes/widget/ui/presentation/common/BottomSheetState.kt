package q19.kenes.widget.ui.presentation.common

import com.google.android.material.bottomsheet.BottomSheetBehavior

internal enum class BottomSheetState {
    HIDDEN,
    COLLAPSED,
    DRAGGING,
    SETTLING,
    HALF_EXPANDED,
    EXPANDED;

    companion object {
        fun from(@BottomSheetBehavior.State state: Int): BottomSheetState? {
            return when (state) {
                BottomSheetBehavior.STATE_HIDDEN -> HIDDEN
                BottomSheetBehavior.STATE_COLLAPSED -> COLLAPSED
                BottomSheetBehavior.STATE_DRAGGING -> DRAGGING
                BottomSheetBehavior.STATE_SETTLING -> SETTLING
                BottomSheetBehavior.STATE_HALF_EXPANDED -> HALF_EXPANDED
                BottomSheetBehavior.STATE_EXPANDED -> EXPANDED
                else -> null
            }
        }
    }
}