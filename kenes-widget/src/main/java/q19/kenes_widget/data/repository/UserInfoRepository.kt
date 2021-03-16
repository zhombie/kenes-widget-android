package q19.kenes_widget.data.repository

import q19.kenes_widget.data.model.User

internal interface UserInfoRepository {
    fun getUserInfo(): User?
    fun saveUserInfo(user: User): Boolean
}


internal class UserInfoRepositoryImpl : UserInfoRepository {

    private var user: User? = null

    override fun getUserInfo(): User? {
        return user
    }

    override fun saveUserInfo(user: User): Boolean {
        this.user = user
        return !user.isEmpty()
    }

}