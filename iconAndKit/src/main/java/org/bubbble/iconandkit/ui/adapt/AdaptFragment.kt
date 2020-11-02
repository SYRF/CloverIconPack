package org.bubbble.iconandkit.ui.adapt

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.lollipop.iconcore.util.doAsync
import com.lollipop.iconcore.util.onUI
import org.bubbble.iconandkit.MainViewModel
import org.bubbble.iconandkit.R
import org.bubbble.iconandkit.databinding.FragmentAdaptBinding
import org.bubbble.iconandkit.widget.PreviewIconDialog
import org.bubbble.life.shared.util.logger

/**
 * A simple [Fragment] subclass.
 * Use the [AdaptFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AdaptFragment : Fragment() {

    private var _binding: FragmentAdaptBinding? = null
    private val binding get() = _binding!!

    private val activityViewModel: MainViewModel by activityViewModels()

    private val previewIconDialog = PreviewIconDialog()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAdaptBinding.inflate(inflater, container, false)
        logger("onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = GridLayoutManager(context, 4)
        activityViewModel.addBackPressedListener(previewIconDialog)
        val adapter = IconsAdapter { iconView, icon ->
            previewIconDialog.show(iconView, icon)
        }
        binding.run {
            recyclerIcons.layoutManager = layoutManager
            recyclerIcons.adapter = adapter
        }

        activityViewModel.onIconPackOnlyData.observe(viewLifecycleOwner, {
            activityViewModel.xmlMap?.let {

                val titlePosition = ArrayList<Int>()
                doAsync {
                    val list = mutableListOf<IconsItem>()
                    for (value in 0 until it.categoryCount) {
                        list.add(IconsItem(it.getCategory(value), 0, ""))
                        titlePosition.add(list.size - 1)
                        for (index in 0 until it.iconCountByCategory(it.getCategory(value))) {
                            val icon = it.getIcon(it.getCategory(value), index)
                            list.add(IconsItem("", icon.resId, icon.name.toString()))
                        }
                    }

//                    val list = mutableListOf<IconsItem>()
//                    for (value in 0 until iconHelper.iconCount) {
//                        val icon = iconHelper.getIconInfo(value)
//                        list.add(IconsItem("", icon.resId, icon.name.toString()))
//                    }
                    onUI {
                        logger("submitList")
                        adapter.submitList(list)
                        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                            override fun getSpanSize(position: Int): Int {
                                return if (titlePosition.contains(position)) 4 else 1
                            }
                        }
                    }
                }
            }
        })

        view.post {
            previewIconDialog.attach(requireActivity())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        previewIconDialog.onDestroy()
        activityViewModel.removeBackPressedListener(previewIconDialog)
    }
}