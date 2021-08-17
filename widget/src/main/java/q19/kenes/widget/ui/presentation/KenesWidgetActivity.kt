package q19.kenes.widget.ui.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import androidx.viewpager2.widget.ViewPager2
import kz.q19.domain.model.configs.Configs
import kz.q19.domain.model.language.Language
import kz.q19.utils.android.dp2Px
import kz.q19.utils.view.binding.bind
import q19.kenes.widget.KenesWidget
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.ui.components.BottomNavigationView
import q19.kenes.widget.ui.components.Toolbar
import q19.kenes.widget.ui.presentation.call.CallsFragment
import q19.kenes.widget.ui.presentation.home.ChatbotFragment
import q19.kenes.widget.ui.presentation.platform.BaseActivity
import q19.kenes.widget.util.UrlUtil
import q19.kenes.widget.util.addKeyboardInsetListener
import q19.kenes_widget.R

internal class KenesWidgetActivity : BaseActivity<KenesWidgetPresenter>(), KenesWidgetView, ChatbotFragment.Listener,
    FragmentOnAttachListener {

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
    private val toolbar by bind<Toolbar>(R.id.toolbar)
    private val viewPager by bind<ViewPager2>(R.id.viewPager)
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

        // FragmentManager Listener
        supportFragmentManager.addFragmentOnAttachListener(this)

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

    override fun onDestroy() {
        super.onDestroy()

        supportFragmentManager.removeFragmentOnAttachListener(this)

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
            override fun onBottomNavigationButtonSelected(navigationButton: BottomNavigationView.NavigationButton) {
                Logger.debug(TAG, "onBottomNavigationButtonSelected() -> $navigationButton")
                presenter.onBottomNavigationButtonSelected(navigationButton.index)
            }

            override fun onBottomNavigationButtonReselected(navigationButton: BottomNavigationView.NavigationButton) {
                Logger.debug(TAG, "onBottomNavigationButtonReselected() -> $navigationButton")

                val fragment = viewPagerAdapter?.getFragment(viewPager.currentItem)
                Logger.debug(TAG, "onBottomNavigationButtonReselected() -> $fragment")
                if (fragment is HomeFragmentDelegate) {
                    fragment.onScreenRenavigate()
                }
            }
        }
    }

    private fun setupKeyboard() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.decorView.addKeyboardInsetListener { isKeyboardVisible ->
            Logger.debug(TAG, "isKeyboardVisible: $isKeyboardVisible")
            if (isKeyboardVisible) {
                bottomNavigationView.visibility = View.GONE
            } else {
                bottomNavigationView.visibility = View.VISIBLE
            }
        }
    }

    /**
     * [FragmentOnAttachListener] implementation
     */

    override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment is ChatbotFragment) {
            fragment.setListener(this)
        }
    }

    /**
     * [KenesWidgetView] implementation
     */

    override fun showBotInfo(bot: Configs.Bot) {
        toolbar.showImage(bot.image)
        toolbar.setTitle(bot.title)
        toolbar.setSubtitle("Smart Bot")
    }

    override fun navigateTo(index: Int) {
        viewPager.setCurrentItem(index, false)
    }

    /**
     * [ChatbotFragment.Listener] implementation
     */

    override fun onResponsesViewScrolled(scrollYPosition: Int) {
        Logger.debug(TAG, "onResponsesViewScrolled() -> $scrollYPosition")
        if (toolbar.elevation > MAX_TOOLBAR_ELEVATION) return
        var elevation: Float = scrollYPosition.toFloat()
        if (elevation < 0F) {
            elevation = 0F
        }
        if (elevation > MAX_TOOLBAR_ELEVATION) {
            elevation = MAX_TOOLBAR_ELEVATION
        }
        toolbar.elevation = elevation
    }

    override fun onBottomSheetSlide(slideOffset: Float) {
//        Logger.debug(TAG, "onBottomSheetSlide() -> $slideOffset")
//
//        toolbar.setBackgroundColor(
//            ColorUtils.blendARGB(
//                Color.parseColor("#FFFFFF"),
//                Color.parseColor("#F23B4859"),
//                slideOffset
//            )
//        )
//
//        titleView.setTextColor(
//            ColorUtils.blendARGB(
//                ContextCompat.getColor(this, R.color.kenes_dark_charcoal),
//                Color.parseColor("#FFFFFF"),
//                slideOffset
//            )
//        )
//
//        subtitleView.setTextColor(
//            ColorUtils.blendARGB(
//                ContextCompat.getColor(this, R.color.kenes_dark_charcoal),
//                Color.parseColor("#FFFFFF"),
//                slideOffset
//            )
//        )
    }

}