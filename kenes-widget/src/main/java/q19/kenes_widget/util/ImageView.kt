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
        transformation = CircleTransformation()
    )
}

internal fun ImageView.loadRoundedImage(
    photoUrl: String,
    radius: Int
) {
    loadImage(
        url = photoUrl,
        isFit = true,
        isCenterCrop = true,
        transformation = RoundedTransformation(radius)
    )
}

internal fun ImageView.loadImage(
    url: String?,
    isFit: Boolean = false,
    isCenterCrop: Boolean = false,
    transformation: Transformation? = null
) {
    val requestCreator = Picasso.get().load(url)

    if (isFit) {
        requestCreator.fit()
    }

    if (isCenterCrop) {
        requestCreator.centerCrop()
    }

    transformation?.let {
        requestCreator.transform(transformation)
    }

    requestCreator.into(this)
}