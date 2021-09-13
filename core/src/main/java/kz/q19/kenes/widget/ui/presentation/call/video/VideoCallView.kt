package kz.q19.kenes.widget.ui.presentation.call.video

import kz.q19.domain.model.message.Message
import kz.q19.kenes.widget.ui.presentation.platform.BaseView

internal interface VideoCallView : BaseView {
    fun showCallAgentInfo(title: String, subtitle: String, photoUrl: String?)
    fun showNewChatMessage(message: Message)

    fun showFloatingVideostreamView()
    fun hideFloatingVideostreamView()

    fun showVideoCallScreenSwitcher()
    fun hideVideoCallScreenSwitcher()

    fun showHangupCallButton()
    fun hideHangupCallButton()

    fun setLocalAudioEnabled()
    fun setLocalAudioDisabled()

    fun setLocalVideoEnabled()
    fun setLocalVideoDisabled()

    fun enterFloatingVideostream()
    fun exitFloatingVideostream()

    fun clearMessageInput()

    fun collapseBottomSheet()
    fun expandBottomSheet()

    fun showNoOnlineCallAgentsMessage(text: String?)
    fun showOperationAvailableOnlyDuringLiveCallMessage()
    fun showCancelPendingConfirmationMessage()
    fun showCancelLiveCallConfirmationMessage()
    fun showMediaSelection()

    fun navigateToHome()
}