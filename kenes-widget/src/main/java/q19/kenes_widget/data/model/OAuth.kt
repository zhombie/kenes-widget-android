package q19.kenes_widget.data.model

data class OAuth constructor(
    val accessToken: String,
    val tokenType: String,
    val refreshToken: String?,
    val expiresIn: Long?,
    val scope: String?
) {

    fun getScopes(): List<String>? {
        return scope?.split(" ")
    }

}