package org.bubbble.iconandkit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lollipop.iconcore.ui.IconPackActivity
import com.lollipop.iconcore.ui.SimpleActivityRenderer
import org.bubbble.iconandkit.databinding.ActivityMainBinding
import org.bubbble.iconandkit.ui.adapt.AdaptFragment
import org.bubbble.iconandkit.ui.adapted.AdaptedFragment
import org.bubbble.iconandkit.ui.apply.ApplyFragment
import org.bubbble.iconandkit.ui.icons.IconsFragment
import org.bubbble.iconandkit.ui.preview.PreviewFragment
import org.bubbble.iconandkit.ui.request.RequestFragment
import org.bubbble.life.shared.util.logger

open class MainActivity : SimpleActivityRenderer(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var viewModel: MainViewModel

    private lateinit var context: Context

    companion object {

        private val INFO_TITLES = arrayOf(
            R.string.preview,
            R.string.icons,
            R.string.apply,
        )
        private val INFO_PAGES = arrayOf(
            { PreviewFragment() },
            { IconsFragment() },
            { ApplyFragment() }
        )

        const val PREVIEW = 0
        const val ICON_PAGE = 1
        const val APPLY = 2
    }

    override fun onCreate(target: IconPackActivity, savedInstanceState: Bundle?) {
        super.onCreate(target, savedInstanceState)
        binding = ActivityMainBinding.inflate(target.layoutInflater)
        val viewModel: MainViewModel by target.viewModels()
        this.viewModel = viewModel
        viewModel.setupIconHelper(target)

        setContentView(target, binding.root)
        context = target

        if (savedInstanceState == null) {
            setupBottomNavigationBar(target)
        }

        viewModel.setAdaptIconPage {
            binding.viewPager.currentItem = ICON_PAGE
            viewModel.getAdaptedIconPage()?.invoke(it)
        }
    }

    override fun onRestoreInstanceState(target: IconPackActivity, savedInstanceState: Bundle) {
        super.onRestoreInstanceState(target, savedInstanceState)
        setupBottomNavigationBar(target)
    }

    private fun setupBottomNavigationBar(target: IconPackActivity) {
        binding.viewPager.offscreenPageLimit = INFO_PAGES.size
        binding.viewPager.adapter = InfoAdapter(target.supportFragmentManager)
        binding.navigation.setOnNavigationItemSelectedListener(this)
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                binding.navigation.menu.getItem(position).isChecked = true
            }
        })
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
            return context.resources.getString(INFO_TITLES[position])
        }
    }

    override fun onBackPressed(): Boolean {

        for (value in viewModel.getBackPressedListener()) {
            if (value.onBackPressed()) {
                return true
            }
        }

        return if (binding.viewPager.currentItem != PREVIEW) {
            binding.viewPager.currentItem = PREVIEW
            true
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.preview -> binding.viewPager.currentItem = PREVIEW
            R.id.icons -> binding.viewPager.currentItem = ICON_PAGE
            R.id.apply -> binding.viewPager.currentItem = APPLY
        }
        return true
    }
}