package q19.kenes_widget

import android.content.Context
import android.content.Intent
import q19.kenes_widget.core.locale.LanguageSetting
import q19.kenes_widget.data.model.Language
import q19.kenes_widget.ui.presentation.KenesWidgetV2Activity

object KenesWidget {

    data class EntryParams @JvmOverloads constructor(
        val hostname: String,
        val language: Language? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val phoneNumber: String? = null
    )

    @JvmStatic
    fun open(context: Context, entryParams: EntryParams): Intent {
        val locale = entryParams.language?.locale
        if (locale != null) {
            LanguageSetting.setLanguage(context, locale)
        }
        return KenesWidgetV2Activity.newIntent(
            context,
            hostname = entryParams.hostname,
            language = entryParams.language,
            firstName = entryParams.firstName,
            lastName = entryParams.lastName,
            phoneNumber = entryParams.phoneNumber
        )
    }

}