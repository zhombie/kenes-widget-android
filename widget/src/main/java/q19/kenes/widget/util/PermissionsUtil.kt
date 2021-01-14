package q19.kenes.widget.util

import com.fondesa.kpermissions.PermissionStatus

internal inline fun <reified T : PermissionStatus> List<PermissionStatus>.toMessage(): String =
    filterIsInstance<T>().joinToString { it.permission }