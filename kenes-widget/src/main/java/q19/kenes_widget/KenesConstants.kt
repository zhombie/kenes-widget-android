package q19.kenes_widget

import android.content.Context

internal object KenesConstants {

    private const val PROTOCOL = "https://"

    var PROJECT: Project? = null

    private object RealProjectType {
        const val KAZPOST = "kazpost"
        const val KENES = "kenes"
        const val MON_RK = "mon"
        const val NITEC = "nitec"
        const val SAMRUK = "skc"
        const val VLX = "vlx"
    }

    fun getProjectName(context: Context): String = context.getString(R.string.PROJECT_NAME)

    fun getHostname(context: Context): String? {
        val project = if (PROJECT != null) {
            when (PROJECT) {
                Project.KNS -> RealProjectType.KENES
                Project.KPT -> RealProjectType.KAZPOST
                Project.MRK -> RealProjectType.MON_RK
                Project.NTC -> RealProjectType.NITEC
                Project.SKC -> RealProjectType.SAMRUK
                Project.VLX -> RealProjectType.VLX
                else -> null
            }
        } else {
            getProjectName(context)
        }
        return when (project) {
            RealProjectType.KAZPOST -> "${PROTOCOL}help.post.kz"
            RealProjectType.KENES -> "${PROTOCOL}kenes.1414.kz"
            RealProjectType.MON_RK -> "${PROTOCOL}help.edu.kz"
            RealProjectType.NITEC -> "${PROTOCOL}bot.nitec.kz"
            RealProjectType.SAMRUK -> "${PROTOCOL}help.skc.kz"
            RealProjectType.VLX -> "${PROTOCOL}kenes.vlx.kz"
            else -> null
        }
    }

    fun getUrl(context: Context): String? =
        getHostname(context)?.let { "$it/admin/widget?is_mobile=true" }

}