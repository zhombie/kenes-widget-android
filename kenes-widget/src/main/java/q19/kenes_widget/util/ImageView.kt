package q19.kenes_widget.util

import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import q19.kenes_widget.util.picasso.CircleTransformation
import q19.kenes_widget.util.picasso.RoundedTransformation

internal fun ImageView.loadCircleImage(photoUrl: String) {
    loadImage(
        url = photoUrl,
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
    isFit: Boolean = false,
    isCenterCrop: Boolean = false,
    priority: Picasso.Priority = Picasso.Priority.NORMAL,
    transformation: Transformation? = null
) {
    val requestCreator = Picasso.get().load(url)

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