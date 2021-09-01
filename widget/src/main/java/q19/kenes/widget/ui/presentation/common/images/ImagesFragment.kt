package q19.kenes.widget.ui.presentation.common.images

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.q19.utils.android.dp2Px
import kz.zhombie.museum.MuseumDialogFragment
import kz.zhombie.museum.model.Painting
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.ui.components.KenesToolbar
import q19.kenes.widget.ui.presentation.common.chat.SpacingItemDecoration
import q19.kenes.widget.ui.presentation.platform.BaseFullscreenDialogFragment
import q19.kenes_widget.R

internal class ImagesFragment : BaseFullscreenDialogFragment<ImagesPresenter>(R.layout.fragment_images, true) {

    companion object {
        private val TAG = ImagesFragment::class.java.simpleName

        fun newInstance(images: ArrayList<Uri>): ImagesFragment {
            val fragment = ImagesFragment()
            fragment.arguments = Bundle().apply {
                putStringArrayList("images", ArrayList(images.map { it.toString() }))
            }
            return fragment
        }
    }

    private var toolbar: KenesToolbar? = null
    private var recyclerView: RecyclerView? = null

    private var images: List<Uri>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        images = requireArguments().getStringArrayList("images")?.map { Uri.parse(it) }
    }

    override fun createPresenter(): ImagesPresenter {
        return ImagesPresenter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        recyclerView = view.findViewById(R.id.recyclerView)

        Logger.debug(TAG, "images: $images")

        setupToolbar()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        toolbar?.setLeftButtonEnabled(true)
        toolbar?.setLeftButtonIcon(R.drawable.ic_arrow_left)
        toolbar?.setLeftButtonIconTint(R.color.kenes_white)
        toolbar?.setLeftButtonOnClickListener {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        val adapter = ImagesAdapter { imageView, image ->
            MuseumDialogFragment.Builder()
                .setPainting(Painting(image))
                .setImageView(imageView)
                .setFooterViewEnabled(false)
                .showSafely(childFragmentManager)
        }

        recyclerView?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerView?.adapter = adapter
        adapter.images = this@ImagesFragment.images ?: emptyList()

        recyclerView?.addItemDecoration(SpacingItemDecoration(10F.dp2Px()))
    }

}