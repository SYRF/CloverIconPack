package org.bubbble.iconandkit.ui.adapted

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
import org.bubbble.iconandkit.databinding.FragmentAdaptedBinding
import org.bubbble.iconandkit.ui.adapt.IconsAdapter
import org.bubbble.iconandkit.ui.adapt.IconsItem
import org.bubbble.iconandkit.widget.PreviewIconDialog
import org.bubbble.life.shared.util.logger

/**
 * A simple [Fragment] subclass.
 * Use the [AdaptedFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AdaptedFragment : Fragment() {

    private var _binding: FragmentAdaptedBinding? = null
    private val binding get() = _binding!!

    private val activityViewModel: MainViewModel by activityViewModels()

    private val previewIconDialog = PreviewIconDialog()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAdaptedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activityViewModel.addBackPressedListener(previewIconDialog)
        val adapter = IconsAdapter { iconView, icon ->
            previewIconDialog.show(iconView, icon)
        }
        binding.run {
            recyclerIcons.layoutManager = GridLayoutManager(context, 4)
            recyclerIcons.adapter = adapter
        }

        activityViewModel.onSupportedOnlyData.observe(viewLifecycleOwner, { iconHelper ->
            activityViewModel.xmlMap?.let {
                doAsync {
                    val list = mutableListOf<IconsItem>()
                    for (value in 0 until iconHelper.supportedCount) {
                        val icon = iconHelper.getSupportedInfo(value)
                        list.add(IconsItem("", icon.iconPack[0], icon.getLabel(requireContext()).toString()))
                    }
                    onUI {
                        adapter.submitList(list)
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