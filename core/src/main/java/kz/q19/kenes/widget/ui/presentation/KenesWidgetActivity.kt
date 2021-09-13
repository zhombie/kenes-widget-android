package kz.q19.kenes.widget.ui.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.viewpager2.widget.ViewPager2
import kz.q19.domain.model.call.Call
import kz.q19.domain.model.configs.Configs
import kz.q19.domain.model.language.Language
import kz.q19.kenes.widget.BuildConfig
import kz.q19.kenes.widget.KenesWidget
import kz.q19.kenes.widget.R
import kz.q19.kenes.widget.core.Settings
import kz.q19.kenes.widget.core.URLManager
import kz.q19.kenes.widget.core.logging.Logger
import kz.q19.kenes.widget.ui.components.KenesBottomNavigationView
import kz.q19.kenes.widget.ui.components.KenesToolbar
import kz.q19.kenes.widget.ui.presentation.call.CallsFragment
import kz.q19.kenes.widget.ui.presentation.call.text.TextChatFragment
import kz.q19.kenes.widget.ui.presentation.call.video.VideoCallFragment
import kz.q19.kenes.widget.ui.presentation.common.BottomSheetState
import kz.q19.kenes.widget.ui.presentation.common.HomeFragment
import kz.q19.kenes.widget.ui.presentation.common.Screen
import kz.q19.kenes.widget.ui.presentation.home.ChatbotFragment
import kz.q19.kenes.widget.ui.presentation.info.InfoFragment
import kz.q19.kenes.widget.ui.presentation.platform.BaseActivity
import kz.q19.kenes.widget.ui.presentation.services.ServicesFragment
import kz.q19.kenes.widget.util.addKeyboardVisibilityListener
import kz.q19.utils.animation.AbstractAnimationListener
import kz.q19.utils.view.binding.bind
import kz.zhombie.cinema.CinemaDialogFragment
import kz.zhombie.museum.MuseumDialogFragment
import kz.zhombie.radio.Radio

internal class KenesWidgetActivity : BaseActivity<KenesWidgetPresenter>(),
    KenesWidgetView,
    HomeFragment.Listener,
    ChatbotFragment.Listener,
    CallsFragment.Listener,
    VideoCallFragment.Listener,
    TextChatFragment.Listener {

    companion object {
        private val TAG = KenesWidgetActivity::class.java.simpleName

        fun newIntent(
            context: Context,
            hostname: String,
            languageKey: String,
            user: KenesWidget.Builder.User? = null
        ): Intent =
            Intent(context, KenesWidgetActivity::class.java)
                .putExtra(IntentKey.HOSTNAME, hostname)
                .putExtra(IntentKey.LANGUAGE_KEY, languageKey)
                .putExtra(IntentKey.USER, user)
    }

    // Intent Arguments
    private object IntentKey {
        const val HOSTNAME = "hostname"
        const val LANGUAGE_KEY = "language"
        const val USER = "user"
    }

    // UI Views
    private val toolbar by bind<KenesToolbar>(R.id.toolbar)
    private val viewPager by bind<ViewPager2>(R.id.viewPager)
    private val fragmentContainerView by bind<FragmentContainerView>(R.id.fragmentContainerView)
    private val bottomNavigationView by bind<KenesBottomNavigationView>(R.id.bottomNavigationView)

    // BottomNavigationView + ViewPager2 adapter
    private var viewPagerAdapter: ViewPagerAdapter? = null

    // Fragments for ViewPager2 adapter
    private val fragments: MutableList<Fragment> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kenes_activity_kenes_widget)

        // Hostname
        val hostname: String? = intent.getStringExtra(IntentKey.HOSTNAME)
        URLManager.setHostname(hostname)

        // Attach view to MVP presenter
        presenter.attachView(this)

        // Cinema (video fullscreen preview)
        CinemaDialogFragment.init(BuildConfig.DEBUG)

        // Museum (image fullscreen preview)
//        MuseumDialogFragment.init(Settings.getImageLoader(), BuildConfig.DEBUG)

        // Radio (audio player)
        Radio.init(BuildConfig.DEBUG)

        // Toolbar
        setupToolbar()

        // ViewPager + Fragments
        setupViewPager()

        // Keyboard
        setupKeyboard()
    }

    override fun createPresenter(): KenesWidgetPresenter {
        // Language
        val languageKey = intent.getStringExtra(IntentKey.LANGUAGE_KEY)

        var language = languageKey?.let { Language.by(key = it) }

        val currentLocale = getCurrentLocale()

        if (language == null) {
            language = currentLocale?.let { Language.from(it) } ?: Language.DEFAULT
        }

        // Set language
        Logger.debug(TAG, "locale: $currentLocale")
        if (currentLocale == null || language.locale != currentLocale) {
            Logger.debug(TAG, "setLocale() -> language.locale: ${language.locale}")
            setLocale(language.locale)
        }

        // Presenter
        return injection.provideKenesWidgetPresenter(language)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Logger.debug(TAG, "onBackPressed()")
    }

//    override fun onBackPressed() {
//        Logger.debug(TAG, "onBackPressed()")
//
//        val fragment = supportFragmentManager.findFragmentByTag("video_call")
//        if (fragment is VideoCallFragment) {
//            super.onBackPressed()
//        } else {
//            if (viewPager.currentItem > 0) {
//                bottomNavigationView.setFirstNavigationButtonActive()
//            } else {
//                super.onBackPressed()
//            }
//        }
//    }

    override fun onDestroy() {
        Logger.debug(TAG, "onDestroy()")

        MuseumDialogFragment.clear()

        Settings.clear()

        fragments.clear()

        super.onDestroy()

        injection.destroy()
    }

    private fun setupToolbar() {
        toolbar.setLeftButtonEnabled(false)
    }

    private fun setupViewPager() {
        fragments.add(ChatbotFragment.newInstance())
        fragments.add(CallsFragment.newInstance())
        fragments.add(ServicesFragment.newInstance())
        fragments.add(InfoFragment.newInstance())

        viewPagerAdapter = ViewPagerAdapter(this, fragments)
        viewPager.adapter = viewPagerAdapter
        viewPager.isUserInputEnabled = false
        viewPager.offscreenPageLimit = fragments.size

        bottomNavigationView.callback = object : KenesBottomNavigationView.Callback {
            override fun onBottomNavigationSelected(screen: Screen) {
                Logger.debug(TAG, "onBottomNavigationSelected() -> $screen")
                presenter.onBottomNavigationButtonSelected(screen)
            }

            override fun onBottomNavigationReselected(screen: Screen) {
                Logger.debug(TAG, "onBottomNavigationReselected() -> $screen")

                with(viewPagerAdapter?.getFragment(viewPager.currentItem)) {
                    if (this is HomeScreenDelegate) {
                        onScreenRenavigate()
                    }
                }
            }
        }
    }

    private fun setupKeyboard() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.addKeyboardVisibilityListener { isVisible ->
            if (isVisible) {
                bottomNavigationView.visibility = View.GONE
            } else {
                bottomNavigationView.visibility = View.VISIBLE
            }
        }
    }

    /**
     * [KenesWidgetView] implementation
     */

    override fun showBotInfo(bot: Configs.Bot) {
        toolbar.showImage(bot.image)
        toolbar.setTitle(bot.title)
        toolbar.setSubtitle("Smart Bot")
        toolbar.reveal()
    }

    override fun showBottomSheetCloseButton() {
        toolbar.setRightButtonEnabled(true)
        toolbar.setRightButtonIcon(R.drawable.kenes_ic_cancel)
        toolbar.setRightButtonIconTint(R.color.kenes_gray)
        toolbar.setRightButtonOnClickListener {
            val fragment = viewPagerAdapter?.getFragment(viewPager.currentItem)
            if (fragment is ChatbotFragment) {
                fragment.collapseBottomSheet()
            }
        }
    }

    override fun hideBottomSheetCloseButton() {
        toolbar.setRightButtonEnabled(false)
        toolbar.setRightButtonOnClickListener(null)
    }

    override fun navigateTo(index: Int) {
        viewPager.setCurrentItem(index, false)
    }

    /**
     * [HomeFragment.Listener] implementation
     */

    override fun onVerticalScroll(scrollYPosition: Int) {
//        Logger.debug(TAG, "onVerticalScroll() -> $scrollYPosition")

        toolbar.elevation = scrollYPosition.toFloat()
    }

    /**
     * [ChatbotFragment.Listener] implementation
     */

    override fun onBottomSheetSlide(slideOffset: Float) {
//        Logger.debug(TAG, "onBottomSheetSlide() -> $slideOffset")

        toolbar.setRightButtonAlpha(slideOffset)

//        toolbar.setBackgroundColor(
//            ColorUtils.blendARGB(
//                Color.parseColor("#FFFFFF"),
//                Color.parseColor("#F23B4859"),
//                slideOffset
//            )
//        )
//
//        toolbar.setTitleTextColor(
//            ColorUtils.blendARGB(
//                ContextCompat.getColor(this, R.color.kenes_dark_charcoal),
//                Color.parseColor("#FFFFFF"),
//                slideOffset
//            )
//        )
//
//        toolbar.setSubtitleTextColor(
//            ColorUtils.blendARGB(
//                ContextCompat.getColor(this, R.color.kenes_dark_charcoal),
//                Color.parseColor("#75FFFFFF"),
//                slideOffset
//            )
//        )
    }

    override fun onBottomSheetStateChanged(state: BottomSheetState) {
//        Logger.debug(TAG, "onBottomSheetStateChanged() -> $state")

        presenter.onBottomSheetStateChanged(state)
    }

    /**
     * [CallsFragment.Listener] implementation
     */

    override fun onLaunchCall(call: Call) {
        Logger.debug(TAG, "onLaunchCall() -> $call")

        if (call is Call.Video) {
            fragmentContainerView.visibility = View.VISIBLE
            supportFragmentManager.commit(false) {
                setCustomAnimations(R.anim.kenes_slide_up, R.anim.kenes_slide_down)
                add(
                    fragmentContainerView.id,
                    VideoCallFragment.newInstance(call),
                    "video_call"
                )
            }
        }
    }

    /**
     * [VideoCallFragment.Listener] implementation
     */

    override fun onCallFinished() {
        Logger.debug(TAG, "onCallFinished()")

        val animation = AnimationUtils.loadAnimation(this, R.anim.kenes_slide_down)
        animation.setAnimationListener(object : AbstractAnimationListener() {
            override fun onAnimationEnd(animation: Animation?) {
                supportFragmentManager.commit(false) {
                    runOnCommit {
                        fragmentContainerView.visibility = View.GONE
                    }
                    with(supportFragmentManager.findFragmentByTag("video_call")) {
                        if (this != null) {
                            remove(this)
                        }
                    }
                }
            }
        })
        fragmentContainerView.startAnimation(animation)
    }

    /**
     * [TextChatFragment.Listener] implementation
     */

    override fun onViewReady() {
        runOnVideoCallScreen {
            onViewReady()
        }
    }

    override fun onNavigationBackPressed() {
        runOnVideoCallScreen {
            onNavigationBackPressed()
        }
    }

    override fun onShowVideoCallScreen() {
        runOnVideoCallScreen {
            onShowVideoCallScreen()
        }
    }

    override fun onSendTextMessage(message: String?) {
        runOnVideoCallScreen {
            onSendTextMessage(message)
        }
    }

    override fun onHangupCall() {
        runOnVideoCallScreen {
            onHangupCall()
        }
    }

    private fun runOnVideoCallScreen(block: VideoCallFragment.() -> Unit) =
        with(supportFragmentManager.findFragmentByTag("video_call")) {
            if (this is VideoCallFragment) {
                block.invoke(this)
            }
        }

}