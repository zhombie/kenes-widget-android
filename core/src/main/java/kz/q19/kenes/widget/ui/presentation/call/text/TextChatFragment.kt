package kz.q19.kenes.widget.ui.presentation.call.text

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.EdgeEffect
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.resample.AudioResampler
import com.otaliastudios.transcoder.resize.FractionResizer
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import kz.q19.domain.model.media.Media
import kz.q19.domain.model.message.Message
import kz.q19.kenes.widget.R
import kz.q19.kenes.widget.api.ImageLoader
import kz.q19.kenes.widget.core.Settings
import kz.q19.kenes.widget.core.logging.Logger
import kz.q19.kenes.widget.domain.model.media.Content
import kz.q19.kenes.widget.domain.model.media.Image
import kz.q19.kenes.widget.domain.model.media.Video
import kz.q19.kenes.widget.domain.model.sourceUri
import kz.q19.kenes.widget.ui.components.KenesMessageInputView
import kz.q19.kenes.widget.ui.components.KenesToolbar
import kz.q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import kz.q19.kenes.widget.ui.presentation.common.contract.GetImage
import kz.q19.kenes.widget.ui.presentation.common.contract.GetVideo
import kz.q19.kenes.widget.ui.presentation.common.recycler_view.SpacingItemDecoration
import kz.q19.kenes.widget.ui.presentation.platform.BaseFragment
import kz.q19.kenes.widget.util.ImageCompressor
import kz.q19.kenes.widget.util.bindAutoClearedValue
import kz.q19.utils.android.dp2Px
import kz.zhombie.cinema.CinemaDialogFragment
import kz.zhombie.cinema.model.Movie
import kz.zhombie.museum.MuseumDialogFragment
import kz.zhombie.museum.model.Painting
import kz.zhombie.radio.Radio
import kz.zhombie.radio.getDurationOrZeroIfUnset
import kz.zhombie.radio.getPositionByProgress
import java.io.File
import kotlin.math.roundToInt

internal class TextChatFragment : BaseFragment<TextChatPresenter>(R.layout.kenes_fragment_text_chat),
    TextChatView,
    ChatMessagesAdapter.Callback {

    companion object {
        private val TAG = TextChatFragment::class.java.simpleName

        fun newInstance(): TextChatFragment {
            return TextChatFragment()
        }
    }

    // UI Views
    private var toolbar: KenesToolbar? = null
    private var showVideoCallScreenButton: MaterialButton? = null
    private var messagesView: RecyclerView? = null
    private var messageInputView: KenesMessageInputView? = null

    // RecyclerView adapter
    private var chatMessagesAdapter: ChatMessagesAdapter? = null

    private val chatMessagesAdapterDataObserver by lazy(LazyThreadSafetyMode.NONE) {
        object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)

                messagesView?.smoothScrollToPosition(0)
            }
        }
    }

    private var imageLoader by bindAutoClearedValue<ImageLoader>()

    private var radio: Radio? = null

    // Activity + Fragment communication
    private var listener: Listener? = null

    interface Listener {
        fun onViewReady()

        fun onNavigationBackPressed()

        fun onShowVideoCallScreen()

        fun onSelectMedia()
        fun onMediaSelected(content: Content)

        fun onSendTextMessage(message: String?)

        fun onHangupCall()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is Listener) {
            this.listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageLoader = Settings.getImageLoader()

        presenter.attachView(this)
    }

    override fun createPresenter(): TextChatPresenter {
        return injection.provideTextChatPresenter(getCurrentLanguage())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        showVideoCallScreenButton = view.findViewById(R.id.showVideoCallScreenButton)
        messagesView = view.findViewById(R.id.messagesView)
        messageInputView = view.findViewById(R.id.messageInputView)

        setupToolbar()
        setupVideoCallButton()
        setupMessagesView()

        listener?.onViewReady()
    }

    override fun onDetach() {
        super.onDetach()

        listener = null
    }

    override fun onDestroyView() {
        radio?.release()
        radio = null

        super.onDestroyView()

        chatMessagesAdapter?.unregisterAdapterDataObserver(chatMessagesAdapterDataObserver)
        chatMessagesAdapter?.callback = null
        chatMessagesAdapter = null
    }

    private fun setupToolbar() {
        toolbar?.setLeftButtonEnabled(true)
        toolbar?.setLeftButtonIcon(R.drawable.kenes_ic_arrow_left)
        toolbar?.setLeftButtonIconSize(17F.dp2Px().roundToInt())
        toolbar?.setLeftButtonIconTint(R.color.kenes_black)
        toolbar?.setLeftButtonOnClickListener {
            listener?.onNavigationBackPressed()
        }
    }

    private fun setupVideoCallButton() {
        showVideoCallScreenButton?.setOnClickListener {
            listener?.onShowVideoCallScreen()
        }
    }

    private fun setupMessagesView() {
        messagesView?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)

        chatMessagesAdapter = ChatMessagesAdapter(this)
        messagesView?.addItemDecoration(SpacingItemDecoration(5F.dp2Px()))
        messagesView?.adapter = chatMessagesAdapter

        chatMessagesAdapter?.registerAdapterDataObserver(chatMessagesAdapterDataObserver)

        messagesView?.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return EdgeEffect(view.context).apply {
                    color = Color.parseColor("#2667E5")
                }
            }
        }

        messagesView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (showVideoCallScreenButton?.visibility == View.VISIBLE) {
                    toolbar?.elevation = 0F
                } else {
                    toolbar?.elevation = recyclerView.computeVerticalScrollOffset().toFloat()
                }
            }
        })

        messageInputView?.setSelectMediaButtonEnabled(true)
        messageInputView?.setMediaSelectionEnabled(false)
        messageInputView?.setOnSelectMediaClickListener {
            listener?.onSelectMedia()
        }

        messageInputView?.setOnSendMessageClickListener { _, message ->
            listener?.onSendTextMessage(message)
        }

        messageInputView?.setSendMessageButtonEnabled(false)
        messageInputView?.setOnTextChangedListener { s, _, _, _ ->
            if (s.isNullOrBlank()) {
                messageInputView?.setSendMessageButtonEnabled(false)
            } else {
                messageInputView?.setSendMessageButtonEnabled(true)
            }
        }
    }

    fun showCallAgentInfo(title: String, subtitle: String, photoUrl: String?) = runOnUiThread {
        if (photoUrl.isNullOrBlank()) {
            toolbar?.setImageContentPadding(5F.dp2Px().roundToInt())
            toolbar?.showImage(R.drawable.kenes_ic_user)
        } else {
            toolbar?.setImageContentPadding(0)
            toolbar?.showImage(photoUrl)
        }

        toolbar?.setTitle(title)
        toolbar?.setSubtitle(subtitle)

        toolbar?.reveal()
    }

    fun showHangupCallButton() = runOnUiThread {
        if (toolbar?.isRightButtonEnabled() == false) {
            toolbar?.setRightButtonEnabled(true)
            toolbar?.setRightButtonBackgroundTint(R.color.kenes_soft_red)
            toolbar?.setRightButtonIcon(R.drawable.kenes_ic_phone)
            toolbar?.setRightButtonIconTint(R.color.kenes_white)
            toolbar?.setRightButtonOnClickListener {
                listener?.onHangupCall()
            }
        }
    }

    fun hideHangupCallButton() = runOnUiThread {
        toolbar?.setRightButtonOnClickListener(null)
        toolbar?.setRightButtonEnabled(false)
    }

    fun showVideoCallScreenSwitcher() = runOnUiThread {
        showVideoCallScreenButton?.visibility = View.VISIBLE
    }

    fun hideVideoCallScreenSwitcher() = runOnUiThread {
        showVideoCallScreenButton?.visibility = View.GONE
    }

    fun onNewChatMessage(message: Message) = runOnUiThread {
        presenter.onNewChatMessage(message)
    }

    fun onSelectImage() = runOnUiThread {
        getImage.launch(Any())
    }

    fun onSelectVideo() = runOnUiThread {
        getVideo.launch(Any())
    }

    /**
     * [ChatMessagesAdapter.Callback] implementation
     */

    override fun onUrlInTextClicked(url: String) {
        toast("url: $url")
    }

    override fun onImageClicked(imageView: ImageView, media: Media) {
        MuseumDialogFragment.Builder()
            .setPaintingLoader(imageLoader ?: return)
            .setPainting(Painting(Uri.parse(media.urlPath), Painting.Info(media.title)))
            .setImageView(imageView)
            .setFooterViewEnabled(true)
            .showSafely(childFragmentManager)
    }

    override fun onImagesClicked(
        recyclerView: RecyclerView,
        images: List<Media>,
        imagePosition: Int
    ) {
        MuseumDialogFragment.Builder()
            .setPaintingLoader(imageLoader ?: return)
            .setPaintings(images.map { media ->
                Painting(
                    Uri.parse(media.urlPath),
                    Painting.Info(media.title)
                )
            })
            .setStartPosition(imagePosition)
            .setFooterViewEnabled(false)
            .showSafely(childFragmentManager)
    }

    override fun onVideoClicked(imageView: ImageView, media: Media) {
        CinemaDialogFragment.Builder()
            .setMovie(Movie(Uri.parse(media.urlPath), Movie.Info(media.title)))
            .setScreenView(imageView)
            .setFooterViewEnabled(false)
            .showSafely(childFragmentManager)
    }

    override fun onAudioClicked(media: Media, itemPosition: Int) {
        Logger.debug(TAG, "onAudioClicked() -> $media, $itemPosition")

        val uri = media.sourceUri ?: return

        radio?.let {
            if (uri == it.currentSource && !it.isReleased()) {
                it.playOrPause()
                return
            }
        }

        radio?.release()
        radio = null
        radio = Radio.Builder(requireContext())
            .create(object : Radio.Listener {
                private fun getAudioDuration(): Long {
                    return radio?.getDurationOrZeroIfUnset() ?: 0L
                }

                override fun onPlaybackStateChanged(state: Radio.PlaybackState) {
                    Logger.debug(TAG, "onPlaybackStateChanged() -> state: $state")
                    when (state) {
                        Radio.PlaybackState.READY -> {
                            chatMessagesAdapter?.resetAudioPlaybackState(
                                itemPosition = itemPosition,
                                duration = getAudioDuration()
                            )
                        }
                        Radio.PlaybackState.ENDED -> {
                            chatMessagesAdapter?.resetAudioPlaybackState(
                                itemPosition = itemPosition,
                                duration = getAudioDuration()
                            )

                            radio?.release()
                            radio = null
                        }
                        else -> {
                        }
                    }
                }

                override fun onIsPlayingStateChanged(isPlaying: Boolean) {
                    Logger.debug(TAG, "onIsPlayingStateChanged() -> isPlaying: $isPlaying")

                    chatMessagesAdapter?.setAudioPlaybackState(itemPosition, isPlaying)
                }

                override fun onPlaybackPositionChanged(position: Long) {
                    Logger.debug(TAG, "onPlaybackPositionChanged() -> position: $position")

                    chatMessagesAdapter?.setAudioPlayProgress(
                        itemPosition = itemPosition,
                        progress = radio?.currentPercentage ?: 0F,
                        currentPosition = position,
                        duration = getAudioDuration()
                    )
                }

                override fun onPlayerError(cause: Throwable?) {
                    radio?.release()
                    radio = null

                    chatMessagesAdapter?.resetAudioPlaybackState(itemPosition, getAudioDuration())

                    toast("ERROR", Toast.LENGTH_SHORT)
                }
            })

        radio?.start(uri, true)
    }

    override fun onSeekBarChange(media: Media, progress: Int): Boolean {
        if (radio == null) return false
        if (radio?.isReleased() == true) return false
        if (media.sourceUri == radio?.currentSource) {
            val position = radio?.getPositionByProgress(progress)
            if (position != null) {
                radio?.seekTo(position)
                return true
            }
        }
        return false
    }

    override fun onMessageLongClicked(text: String) {
        toast("copy: $text")
    }

    /**
     * [TextChatView] implementation
     */

    override fun showNewMessage(message: Message) = runOnUiThread {
        chatMessagesAdapter?.addNewMessage(message)
    }

    override fun clearMessageInput() = runOnUiThread {
        messageInputView?.clearInputViewText()
    }

    private val getImage = registerForActivityResult(GetImage()) {
        Logger.debug(TAG, "image: $it")

        val uri = it ?: return@registerForActivityResult

        val projection = arrayOf(
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE
        )

        context?.contentResolver
            ?.query(uri, projection, null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayName =
                        cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))

                    val mimeType = requireContext().contentResolver?.getType(uri)

                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)

                    val compressed = ImageCompressor(requireContext())
                        .compress(
                            imageUri = uri,
                            compressFormat = Bitmap.CompressFormat.JPEG,
                            maxWidth = 1280F,
                            maxHeight = 1280F,
                            useMaxScale = true,
                            quality = 75,
                            minWidth = 150F,
                            minHeight = 150F
                        )

                    val image = if (compressed == null) {
                        Image(
                            uri = uri,
                            displayName = displayName
                        )
                    } else {
                        val file = compressed.toFile()
                        Image(
                            uri = uri,
                            displayName = displayName,
                            title = file.name,
                            duplicateFile = Content.DuplicateFile(
                                mimeType = mimeType,
                                extension = extension,
                                uri = compressed
                            ),
                            history = Content.History(modifiedAt = file.lastModified())
                        )
                    }

                    listener?.onMediaSelected(image)
                }
            }
    }

    private val getVideo = registerForActivityResult(GetVideo()) {
        Logger.debug(TAG, "video: $it")

        val uri = it ?: return@registerForActivityResult

        val projection = arrayOf(
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE
        )

        context?.contentResolver
            ?.query(uri, projection, null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayName =
                        cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))

                    val mimeType = requireContext().contentResolver?.getType(uri)

                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)

                    val strategy = DefaultVideoStrategy.Builder()
                        .addResizer(FractionResizer(0.5F))
                        .frameRate(24)
                        .build()

                    val filename = requireContext().packageName + "_" + System.currentTimeMillis() + ".mp4"
                    val directory = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                    val file = File(directory, filename)

                    val transcoder = Transcoder.into(file.absolutePath)
                        .addDataSource(requireContext(), uri)
                        .setVideoTrackStrategy(strategy)
                        .setAudioResampler(AudioResampler.DOWNSAMPLE)
                        .build()

                    val video = Video(
                        uri = uri,
                        displayName = displayName,
                        duplicateFile = Content.DuplicateFile(
                            mimeType = mimeType,
                            extension = extension,
                            uri = file.toUri()
                        )
                    )
                }
            }
    }

}