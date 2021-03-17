package q19.kenes.widget.util

import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import q19.kenes_widget.R
import q19.kenes.widget.util.picasso.CircleTransformation
import q19.kenes.widget.util.picasso.RoundedTransformation

internal fun ImageView.loadCircleImage(photoUrl: String) {
    loadImage(
        url = photoUrl,
        placeholderResId = R.drawable.kenes_placeholder_circle_gray,
        isFit = true,
        isCenterCrop = true,
        priority = Picasso.Priority.NORMAL,
        transformation = CircleTransformation()
    )
}

internal fun ImageView.loadRoundedImage(
    photoUrl: String?,
    radius: Int
) {
    loadImage(
        url = photoUrl,
        isFit = true,
        isCenterCrop = true,
        priority = Picasso.Priority.HIGH,
        transformation = RoundedTransformation(radius)
    )
}

internal fun ImageView.loadImage(
    url: String?,
    @DrawableRes placeholderResId: Int = -1,
    isFit: Boolean = false,
    isCenterCrop: Boolean = false,
    priority: Picasso.Priority = Picasso.Priority.NORMAL,
    transformation: Transformation? = null
) {
    val requestCreator = Picasso.get().load(url)

    if (placeholderResId != -1) {
        requestCreator.placeholder(placeholderResId)
    }

    if (isFit) {
        requestCreator.fit()
    }

    if (isCenterCrop) {
        requestCreator.centerCrop()
    }

    requestCreator.priority(priority)

    transformation?.let {
        requestCreator.transform(transformation)
    }

    requestCreator.into(this)
}