package q19.kenes_widget.di

import q19.kenes_widget.data.repository.UserInfoRepository
import q19.kenes_widget.data.repository.UserInfoRepositoryImpl

class AppProvider {

    private var userInfoRepository: UserInfoRepository? = null

    fun provideUserInfoRepository(): UserInfoRepository {
        return userInfoRepository ?: UserInfoRepositoryImpl().also {
            this.userInfoRepository = it
        }
    }

}