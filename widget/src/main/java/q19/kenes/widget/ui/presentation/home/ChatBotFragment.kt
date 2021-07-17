package q19.kenes.widget.ui.presentation.home

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.domain.model.Nestable
import q19.kenes.widget.domain.model.ResponseGroup
import q19.kenes.widget.domain.model.ResponseInfo
import q19.kenes.widget.ui.components.MessageInputView
import q19.kenes.widget.ui.presentation.HomeFragmentDelegate
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes_widget.R

internal class ChatBotFragment : BaseFragment(R.layout.fragment_chatbot), ChatBotView,
    HomeFragmentDelegate {

    companion object {
        private val TAG = ChatBotFragment::class.java.simpleName

        fun newInstance(): ChatBotFragment {
            val fragment = ChatBotFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    // (MVP) Presenter
    private var presenter: ChatBotPresenter? = null

    // UI Views
    private var recyclerView: RecyclerView? = null
    private var messageInputView: MessageInputView? = null
    private var chatView: LinearLayout? = null
    private var peekView: LinearLayout? = null
    private var toggleButton: MaterialButton? = null
    private var closeButton: MaterialButton? = null

    // RecyclerView adapter
    private var adapter: ResponseGroupsAdapter? = null

    // CoordinatorLayout + BottomSheet
    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = ChatBotPresenter(Database.getInstance(requireContext()))
        presenter?.attachView(this)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        messageInputView = view.findViewById(R.id.messageInputView)
        chatView = view.findViewById(R.id.chatView)
        peekView = view.findViewById(R.id.peekView)
        toggleButton = view.findViewById(R.id.toggleButton)
        closeButton = view.findViewById(R.id.closeButton)

        setupRecyclerView()
        setupBottomSheet()
    }

    override fun onDestroy() {
        super.onDestroy()

        presenter?.detachView()
        presenter = null
    }

    private fun setupRecyclerView() {
        adapter = ResponseGroupsAdapter(object : ResponseGroupsAdapter.Callback {
            override fun onResponseGroupClicked(responseGroup: ResponseGroup) {
                presenter?.onResponseGroupClicked(responseGroup)
            }

            override fun onResponseGroupChildClicked(child: ResponseGroup.Child) {
                presenter?.onResponseGroupChildClicked(child)
            }

            override fun onGoBackButtonClicked(responseGroup: ResponseGroup) {
                presenter?.onGoBackButtonClicked(responseGroup)
            }
        })
        recyclerView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerView?.adapter = adapter
    }

    private fun setupBottomSheet() {
        chatView?.let {
            bottomSheetBehavior = BottomSheetBehavior.from(it)

            bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    Logger.debug(TAG, "onStateChanged() -> $slideOffset")

                    val alpha = 1F - slideOffset
                    peekView?.alpha = alpha
                    if (alpha == 0F) {
                        peekView?.visibility = View.INVISIBLE
                    } else {
                        peekView?.visibility = View.VISIBLE
                    }
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    Logger.debug(TAG, "onStateChanged() -> $newState")
                }
            })
        }

        toggleButton?.setOnClickListener {
            if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        closeButton?.setOnClickListener {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    /**
     * [ChatBotView] implementation
     */

    override fun showResponseGroups(responseGroups: List<Nestable>) {
        adapter?.submitList(responseGroups)
    }

    override fun showResponseInfo(responseInfo: ResponseInfo) {
        activity?.runOnUiThread {
        }
    }

    /**
     * [HomeFragmentDelegate] implementation
     */

    override fun onScreenRenavigate() {
        Logger.debug(TAG, "onScreenRenavigate()")

        with(recyclerView?.layoutManager) {
            if (this is LinearLayoutManager) {
                if (findFirstCompletelyVisibleItemPosition() == 0) {
                    presenter?.onResetDataRequested()
                } else {
                    recyclerView?.smoothScrollToPosition(0)
                }
            }
        }
    }

}