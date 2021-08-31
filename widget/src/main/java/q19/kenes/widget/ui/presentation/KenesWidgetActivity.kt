package q19.kenes.widget.ui.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.viewpager2.widget.ViewPager2
import kz.q19.domain.model.configs.Configs
import kz.q19.domain.model.language.Language
import kz.q19.utils.android.dp2Px
import kz.q19.utils.view.binding.bind
import kz.zhombie.cinema.CinemaDialogFragment
import kz.zhombie.museum.MuseumDialogFragment
import kz.zhombie.radio.Radio
import q19.kenes.widget.KenesWidget
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.ui.components.BottomNavigationView
import q19.kenes.widget.ui.components.KenesToolbar
import q19.kenes.widget.ui.presentation.call.Call
import q19.kenes.widget.ui.presentation.call.CallsFragment
import q19.kenes.widget.ui.presentation.call.text.TextChatFragment
import q19.kenes.widget.ui.presentation.call.video.VideoCallFragment
import q19.kenes.widget.ui.presentation.common.BottomSheetState
import q19.kenes.widget.ui.presentation.common.HomeFragment
import q19.kenes.widget.ui.presentation.common.Screen
import q19.kenes.widget.ui.presentation.home.ChatbotFragment
import q19.kenes.widget.ui.presentation.platform.BaseActivity
import q19.kenes.widget.util.UrlUtil
import q19.kenes.widget.util.addKeyboardInsetListener
import q19.kenes_widget.BuildConfig
import q19.kenes_widget.R

internal class KenesWidgetActivity : BaseActivity<KenesWidgetPresenter>(),
    KenesWidgetView,
    HomeFragment.Listener,
    ChatbotFragment.Listener,
    CallsFragment.Listener,
    VideoCallFragment.Listener,
    TextChatFragment.Listener {

    companion object {
        private val TAG = KenesWidgetActivity::class.java.simpleName

        private val MAX_TOOLBAR_ELEVATION = 3F.dp2Px()

        fun newIntent(
            context: Context,
            hostname: String,
            language: Language? = null,
            user: KenesWidget.Builder.User? = null
        ): Intent =
            Intent(context, KenesWidgetActivity::class.java)
                .putExtra(IntentKey.HOSTNAME, hostname)
                .putExtra(IntentKey.LANGUAGE, language)
                .putExtra(IntentKey.USER, user)
    }

    // Intent Arguments
    private object IntentKey {
        const val HOSTNAME = "hostname"
        const val LANGUAGE = "language"
        const val USER = "user"
    }

    // UI Views
    private val rootView by bind<LinearLayout>(R.id.rootView)
    private val toolbar by bind<KenesToolbar>(R.id.toolbar)
    private val viewPager by bind<ViewPager2>(R.id.viewPager)
    private val fragmentContainerView by bind<FragmentContainerView>(R.id.fragmentContainerView)
    private val bottomNavigationView by bind<BottomNavigationView>(R.id.bottomNavigationView)

    // BottomNavigationView + ViewPager2 adapter
    private var viewPagerAdapter: ViewPagerAdapter? = null

    // Fragments for ViewPager2 adapter
    private var fragments: Array<Fragment> = arrayOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kenes_widget)

        // Hostname
        val hostname: String? = intent.getStringExtra(IntentKey.HOSTNAME)
        if (hostname.isNullOrBlank() || !hostname.startsWith("https://")) {
            toast("hostname is blank or null, provide with hostname at first!")
            finish()
        } else {
            UrlUtil.setHostname(hostname)
        }

        // Attach view to MVP presenter
        presenter.attachView(this)

        // Cinema (video fullscreen preview)
        CinemaDialogFragment.init(BuildConfig.DEBUG)

        // Museum (image fullscreen preview)
        MuseumDialogFragment.init(CoilImageLoader(this), BuildConfig.DEBUG)

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
        var language = intent.getParcelableExtra<Language>(IntentKey.LANGUAGE)
        if (language == null) {
            language = getCurrentLanguage()
        }

        // Set language
        val currentLocale = getCurrentLocale()
        Logger.debug(TAG, "currentLocale: $currentLocale")
        if (currentLocale == null || language.locale != currentLocale) {
            Logger.debug(TAG, "setLocale() -> language.locale: ${language.locale}")
            setLocale(language.locale)
        }

        // Presenter
        return injection.provideKenesWidgetPresenter(language)
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
        super.onDestroy()

        injection.destroy()
    }

    private fun setupToolbar() {
        toolbar.setLeftButtonEnabled(false)
    }

    private fun setupViewPager() {
        fragments = arrayOf(
            ChatbotFragment.newInstance(),
            CallsFragment.newInstance()
        )

        viewPagerAdapter = ViewPagerAdapter(this, fragments)
        viewPager.adapter = viewPagerAdapter
        viewPager.isUserInputEnabled = false
        viewPager.offscreenPageLimit = fragments.size

        bottomNavigationView.callback = object : BottomNavigationView.Callback {
            override fun onBottomNavigationSelected(screen: Screen) {
                Logger.debug(TAG, "onBottomNavigationSelected() -> $screen")
                presenter.onBottomNavigationButtonSelected(screen)
            }

            override fun onBottomNavigationReselected(screen: Screen) {
                Logger.debug(TAG, "onBottomNavigationReselected() -> $screen")

                val fragment = viewPagerAdapter?.getFragment(viewPager.currentItem)
                Logger.debug(TAG, "onBottomNavigationReselected() -> $fragment")
                if (fragment is HomeScreenDelegate) {
                    fragment.onScreenRenavigate()
                }
            }
        }
    }

    private fun setupKeyboard() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.decorView.addKeyboardInsetListener { isKeyboardVisible ->
//            Logger.debug(TAG, "isKeyboardVisible: $isKeyboardVisible")
            if (isKeyboardVisible) {
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
        toolbar.setRightButtonIcon(R.drawable.ic_cancel)
        toolbar.setRightButtonIconTint(R.color.kenes_gray)
        toolbar.setRightButtonOnClickListener {
            supportFragmentManager.fragments.forEach {
                if (it is ChatbotFragment) {
                    it.collapseBottomSheet()
                }
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
        Logger.debug(TAG, "onBottomSheetStateChanged() -> $state")

        presenter.onBottomSheetStateChanged(state)
    }

    /**
     * [CallsFragment.Listener] implementation
     */

    override fun onLaunchCall(call: Call) {
        Logger.debug(TAG, "onLaunchCall() -> $call")

        if (call is Call.Video) {
            supportFragmentManager.commit(false) {
                add(
                    fragmentContainerView.id,
                    VideoCallFragment.newInstance(call),
                    "video_call"
                )
            }

            fragmentContainerView.alpha = 0F
            fragmentContainerView.visibility = View.VISIBLE
            fragmentContainerView.animate()
                .setDuration(200L)
                .alpha(1F)
                .start()
        }
    }

    /**
     * [VideoCallFragment.Listener] implementation
     */

    override fun onCallFinished() {
        supportFragmentManager.commit {
            val fragment = supportFragmentManager.findFragmentByTag("video_call")
            if (fragment != null) {
                remove(fragment)
            }
        }

        fragmentContainerView.alpha = 1F
        fragmentContainerView.visibility = View.VISIBLE
        fragmentContainerView.animate()
            .setDuration(200L)
            .alpha(0F)
            .withEndAction { fragmentContainerView.visibility = View.GONE }
            .start()
    }

    /**
     * [TextChatFragment.Listener] implementation
     */

    override fun onShowVideoCallScreen() {
        val fragment = supportFragmentManager.findFragmentByTag("video_call")
        if (fragment is VideoCallFragment) {
            fragment.onShowVideoCallScreen()
        }
    }

    override fun onHangupCall() {
        val fragment = supportFragmentManager.findFragmentByTag("video_call")
        if (fragment is VideoCallFragment) {
            fragment.onHangupCall()
        }
    }

}