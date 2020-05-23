package q19.kenes_widget.util.picasso

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.lang.Exception

internal abstract class Target : Target {

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {}

    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

}