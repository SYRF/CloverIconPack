package org.bubbble.iconandkit.ui.icons

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.activityViewModels
import androidx.viewpager.widget.ViewPager
import org.bubbble.iconandkit.MainViewModel
import org.bubbble.iconandkit.R
import org.bubbble.iconandkit.databinding.FragmentApplyBinding
import org.bubbble.iconandkit.databinding.FragmentIconsBinding
import org.bubbble.iconandkit.ui.adapt.AdaptFragment
import org.bubbble.iconandkit.ui.adapted.AdaptedFragment
import org.bubbble.iconandkit.ui.request.RequestFragment
import org.bubbble.life.shared.util.logger

/**
 * A simple [Fragment] subclass.
 * Use the [IconsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class IconsFragment : Fragment() {

    private var _binding: FragmentIconsBinding? = null

    private val binding get() = _binding!!

    private val activityViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentIconsBinding.inflate(inflater, container, false)
        logger("IconsFragment-onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.run {
            viewpager.offscreenPageLimit = INFO_PAGES.size
            viewpager.adapter = InfoAdapter(childFragmentManager)
            tabs.setupWithViewPager(binding.viewpager)

            viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}
                override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {}
                override fun onPageSelected(position: Int) {}
            })


            activityViewModel.setAdaptedIconPage {
                viewpager.currentItem = it
            }
        }
    }

    /**
     * Adapter that builds a page for each info screen.
     */
    inner class InfoAdapter(
        fm: FragmentManager
    ) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getCount() = INFO_PAGES.size

        override fun getItem(position: Int) = INFO_PAGES[position]()

        override fun getPageTitle(position: Int): CharSequence {
            return resources.getString(INFO_TITLES[position])
        }
    }

    companion object {

        private val INFO_TITLES = arrayOf(
            R.string.adapt,
            R.string.adapted,
            R.string.request_icons,
        )
        private val INFO_PAGES = arrayOf(
            { AdaptFragment() },
            { AdaptedFragment() },
            { RequestFragment() }
        )

        const val ADAPT = 0
        const val ADAPTED = 1
        const val REQUEST = 2
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}