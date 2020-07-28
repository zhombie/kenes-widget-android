package q19.kenes_widget.ui.presentation

import android.os.Parcelable
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import q19.kenes_widget.model.*
import java.io.File

internal interface KenesWidgetV2View {
    fun showCurrentLanguage(language: Language)
    fun showContacts(contacts: List<Configs.Contact>)
    fun showPhones(phones: List<String>)
    fun showOpponentInfo(opponent: Configs.Opponent)
    fun showOpponentInfo(name: String, photoUrl: String?)
    fun showInfoBlocks(infoBlocks: List<Configs.InfoBlock>)
    fun showAudioCallerInformation(fullName: String, photoUrl: String?)
    fun showFeedback(text: String, ratingButtons: List<RatingButton>)

    fun showNavButton(bottomNavigation: BottomNavigation)
    fun hideNavButton(bottomNavigation: BottomNavigation)

    fun hideHangupButton()

    fun showFileDownloadStatus(status: Message.File.DownloadStatus, itemPosition: Int)
    fun showFileDownloadProgress(progress: Int, fileType: String, itemPosition: Int)

    fun setViewState(viewState: ViewState)

    fun setDefaultFooterView()
    fun setDefaultOperatorCallView()

    fun showOperatorCallButton(operatorCall: OperatorCall)
    fun hideOperatorCallButton(operatorCall: OperatorCall)

    fun setOperatorCallInfoText(text: String)
    fun setOperatorCallPendingQueueCount(count: Int)

    fun setUnreadMessagesCountOnCall(operatorCall: OperatorCall, count: String)

    fun addNewMessage(message: Message)
    fun setNewMessages(message: Message)
    fun setNewMessages(messages: List<Message>)
    fun showUserDisconnectedMessage()

    fun showSwitchToCallAgentButton()
    fun showFuzzyQuestionButtons()
    fun showGoToHomeButton()

    fun showFooterView()

    fun openFile(file: File)
    fun playAudio(path: String, itemPosition: Int)

    fun clearChatMessages()
    fun clearChatFooterMessages()
    fun clearMessageInputViewText()

    fun resolvePermissions(operatorCall: OperatorCall)

    fun restoreChatListViewState(chatListViewState: Parcelable)

    fun releaseMediaPlayer()
    fun releaseVideoDialog()
    fun releasePeerConnection()

    fun showAlreadyCallingAlert(bottomNavigation: BottomNavigation)
    fun showAlreadyCallingAlert(operatorCall: OperatorCall)
    fun showNoOnlineCallAgentsAlert(text: String)
    fun showOpenLinkConfirmAlert(url: String)
    fun showFormSentSuccessAlert()
    fun showHangupConfirmationAlert()

    fun createPeerConnection(
        isMicrophoneEnabled: Boolean,
        isCameraEnabled: Boolean,
        iceServers: List<PeerConnection.IceServer>
    )

    fun initLocalVideoStream()
    fun startLocalMediaStream()
    fun sendOfferToOpponent()
    fun sendAnswerToOpponent()
    fun setRemoteDescription(sessionDescription: SessionDescription)
    fun addRemoteIceCandidate(iceCandidate: IceCandidate)

    fun scrollToBottom()

    fun showAttachmentPicker()
}