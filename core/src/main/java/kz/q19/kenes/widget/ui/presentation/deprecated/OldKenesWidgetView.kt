package kz.q19.kenes.widget.ui.presentation.deprecated

import android.os.Parcelable
import kz.q19.domain.model.call.CallType
import kz.q19.domain.model.configs.Configs
import kz.q19.domain.model.file.File
import kz.q19.domain.model.form.Form
import kz.q19.domain.model.keyboard.button.RateButton
import kz.q19.domain.model.language.Language
import kz.q19.domain.model.media.Media
import kz.q19.domain.model.message.Message
import kz.q19.kenes.widget.ui.presentation.common.Screen
import kz.q19.kenes.widget.ui.presentation.model.ViewState
import kz.q19.kenes.widget.ui.platform.BaseView

internal interface OldKenesWidgetView : BaseView {
    fun showCurrentLanguage(language: Language)
    fun showSocials(contacts: List<Configs.Contacts.Social>)
    fun showPhoneNumbers(phones: List<Configs.Contacts.PhoneNumber>)
    fun showDefaultPeerInfo()
    fun showPeerInfo(opponent: Configs.CallAgent)
    fun showPeerInfo(name: String, photoUrl: String?)
    fun showAudioCallerInformation(fullName: String, photoUrl: String?)
    fun showFeedback(text: String, ratingButtons: List<RateButton>?)

    fun showForm(form: Form)
    fun showAttachmentThumbnail(attachment: Media)
    fun clearDynamicForm()

    fun showCalls(parentCall: Configs.Call? = null, calls: List<Configs.Call>)
    fun showServices(parentService: Configs.Service? = null, services: List<Configs.Service>)

    fun showScreen(screen: Screen)
    fun hideScreen(screen: Screen)

    fun hideHangupButton()

    fun showFileDownloadStatus(status: File.DownloadStatus, itemPosition: Int)
    fun showFileDownloadProgress(progress: Int, fileType: String, itemPosition: Int)

    fun setViewState(viewState: ViewState)

    fun setDefaultFooterView()
    fun setDefaultOperatorCallView()

    fun showOperatorCallButton(callType: CallType)
    fun hideOperatorCallButton(callType: CallType)

    fun setOperatorCallInfoText(text: String)
    fun setOperatorCallPendingQueueCount(count: Int)

    fun setUnreadMessagesCountOnCall(callType: CallType, count: String)

    fun addNewMessage(message: Message)
    fun setNewMessages(message: Message)
    fun setNewMessages(messages: List<Message>)
    fun showUserDisconnectedMessage()

    fun showSwitchToCallAgentButton()
    fun showFuzzyQuestionButtons()
    fun showGoToHomeButton()

    fun showFooterView()

    fun openFile(file: File)
    fun openLink(url: String)

    fun playAudio(path: String, itemPosition: Int)

    fun clearChatMessages()
    fun clearChatFooterMessages()
    fun clearMessageInputViewText()

    fun resolvePermissions(callType: CallType, scope: String? = null)

    fun restoreChatListViewState(chatListViewState: Parcelable)

    fun releaseMediaPlayer()
    fun releaseVideoDialog()

    fun showAlreadyCallingAlert(screen: Screen)
    fun showAlreadyCallingAlert(callType: CallType)
    fun showNoOnlineCallAgentsAlert(text: String)
    fun showOpenLinkConfirmAlert(url: String)
    fun showFormSentSuccessAlert()
    fun showHangupConfirmationAlert()

    fun scrollToTop()
    fun scrollToBottom()

    fun showAttachmentPicker(forced: Boolean)
}