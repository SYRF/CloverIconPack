package org.bubbble.iconandkit.ui.preview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.lollipop.iconcore.ui.IconHelper
import com.lollipop.iconcore.ui.IconView
import com.lollipop.iconcore.util.ExternalLinkManager
import com.lollipop.iconcore.util.IconGroup
import com.lollipop.iconcore.util.MakerInfoManager
import org.bubbble.bubbble.utils.load
import org.bubbble.iconandkit.AndIconKit
import org.bubbble.iconandkit.MainActivity
import org.bubbble.iconandkit.MainViewModel
import org.bubbble.iconandkit.R
import org.bubbble.iconandkit.databinding.FragmentPreviewBinding
import org.bubbble.iconandkit.databinding.ItemLinkBinding
import org.bubbble.iconandkit.ui.author.AuthorDialogFragment
import org.bubbble.iconandkit.ui.icons.IconsFragment
import org.bubbble.iconandkit.ui.info.InfoDialogFragment
import org.bubbble.iconandkit.ui.preview.PreviewViewModel.Companion.PERMISSION_CODE
import org.bubbble.iconandkit.util.UnboundedImageViewHelper
import org.bubbble.life.shared.util.logger
import kotlin.math.roundToInt

/**
 * A simple [Fragment] subclass.
 * Use the [PreviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PreviewViewModel by viewModels()
    private val activityViewModel: MainViewModel by activityViewModels()

    private lateinit var iconGroup: IconGroup
    private var iconHelper: IconHelper? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        logger("PreviewFragment-onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val makerInfoManager = MakerInfoManager(AndIconKit.createMakerInfoProvider(view.context))
        iconGroup = IconGroup(binding.iconRoot)

        binding.run {

            authorIcon.load(makerInfoManager.icon)

            authorIcon.setOnClickListener {
                AuthorDialogFragment().show(childFragmentManager, "DIALOG_AUTHOR")
            }
            infoIcon.setOnClickListener {
                InfoDialogFragment().show(childFragmentManager, "DIALOG_INFO")
            }

            iconsPage.setOnClickListener {
                activityViewModel.getAdaptIconPage()?.invoke(IconsFragment.ADAPT)
            }

            newDialog.setOnClickListener {
                activityViewModel.getAdaptIconPage()?.invoke(IconsFragment.ADAPTED)
            }
        }
        bindLinkInfo(ExternalLinkManager(AndIconKit.createLinkInfoProvider(requireContext())))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activityViewModel.onIconPackOnlyData.observe(viewLifecycleOwner, {
            iconHelper = it
            refresh(it)
        })

        viewModel.permissionAction.observe(viewLifecycleOwner, {
            if (it) {
                checkWallpaper(true)
            } else {
                checkWallpaper(false)
            }
        })
        viewModel.getPermission(this)

        activityViewModel.onUnsupportedOnlyData.observe(viewLifecycleOwner, {
            binding.appCount.text = it.allAppCount.toString()
            binding.adaptationCount.text = it.supportedCount.toString()

            val rate = (it.supportedCount.toFloat() / it.allAppCount.toFloat() * 100F).roundToInt()
            val spannableString = SpannableString("设备适配率 $rate%")
            spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.color_primary)), spannableString.indexOf("率") + 1, spannableString.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
            binding.adaptRate.text = spannableString

            binding.iconNumber.text = it.iconCount.toString()
            binding.newNumber.text = it.supportedCount.toString()

             val endProgress = if (rate < 20) {
                10 + (rate / 2)
            } else {
                rate
            }
            animProgress(endProgress, binding.progress)
        })
    }

    private fun refresh(iconHelper: IconHelper) {
        val fit: (icon: IconView, index: Int) -> Unit = { icon, index ->
            val iconRes = iconHelper.getIconInfo(index).resId
            icon.loadIcon(iconRes)
            if (icon is View) {
                iconAnimator(icon as View)
            }

        }
        iconGroup.autoFit(iconHelper.iconCount, fit)
    }


    private fun iconAnimator(v: View){

        val animator = ValueAnimator()
        animator.setFloatValues(0f, 1f)
        animator.interpolator = DecelerateInterpolator()
        animator.duration = 600
        animator.addUpdateListener {
            v.scaleX = it.animatedValue as Float
            v.scaleY = it.animatedValue as Float
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                v.visibility = View.VISIBLE
            }
        })
        animator.start()
    }

    private fun bindLinkInfo(linkManager: ExternalLinkManager) {
        if (linkManager.linkCount < 1) {
            return
        }
        for (index in 0 until linkManager.linkCount) {
            val info = linkManager.getLink(index)
            if (ExternalLinkManager.getLinkUrl(info.url).startsWith("app_store", true)) {

                if (binding.storeLinks.visibility != View.VISIBLE) {
                    binding.storeLinks.visibility = View.VISIBLE
                    binding.appLayout.visibility = View.VISIBLE
                }
                val holder = LinkItemHolder.create(binding.appLayout)
                holder.bind(info, true)
                binding.appLayout.addView(holder.binding.root)
            } else {
                if (binding.otherLinks.visibility != View.VISIBLE) {
                    binding.otherLinks.visibility = View.VISIBLE
                    binding.linksLayout.visibility = View.VISIBLE
                }
                val holder = LinkItemHolder.create(binding.linksLayout)
                holder.bind(info, false)
                binding.linksLayout.addView(holder.binding.root)
            }
        }
    }

    private class LinkItemHolder(val binding: ItemLinkBinding) {
        companion object {
            fun create(group: ViewGroup): LinkItemHolder {
                return LinkItemHolder(
                    ItemLinkBinding.inflate(LayoutInflater.from(group.context), group, false)
                )
            }
        }

        fun bind(info: ExternalLinkManager.LinkInfo, isAppDownload: Boolean) {

            binding.idTitle.text = info.title
            binding.idSubTitle.text = info.summary
            binding.logo.load(info.icon)

            if (isAppDownload) {
                binding.iconView.setImageResource(R.drawable.ic_file_download)
            }

            binding.linkLayout.setOnClickListener {
                try {
                    when(ExternalLinkManager.getLinkType(info.url)) {
                        ExternalLinkManager.LINK_TYPE_APP -> {
                            it.context.startActivity(info.url)
                        }
                        ExternalLinkManager.LINK_TYPE_STORE -> {
                            val uri = Uri.parse("market://details?id=${it.context.packageName}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            it.context.startActivity(intent)
                        }
                        ExternalLinkManager.LINK_TYPE_WEB -> {
                            val webUrl = ExternalLinkManager.getWebUrl(info.url)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                            it.context.startActivity(intent)
                        }
                        else -> {
                            if (ExternalLinkManager.getLinkUrl(info.url).startsWith("app_store", true)) {
                                val uri = Uri.parse("market://details?id=${info.attr1}")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                it.context.startActivity(intent)
                            }
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    Snackbar.make(it, R.string.open_link_error, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun animProgress(endValue: Int, progress: ProgressBar) {
        val valueAnimator = ValueAnimator.ofInt(0, endValue)
        valueAnimator.duration = 300
        valueAnimator.interpolator = AccelerateDecelerateInterpolator()
        valueAnimator.addUpdateListener {
            progress.progress = it.animatedValue as Int
        }
        valueAnimator.start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        logger("$requestCode  -  $grantResults  -  ${grantResults[0]}")
        when(requestCode){
            PERMISSION_CODE ->{
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    checkWallpaper(false)
                }else{
                    checkWallpaper(true)
                }
            }
            else ->{}
        }
    }


    private fun checkWallpaper(loadWallpaper: Boolean) {
        if (loadWallpaper) {
            val wallpaperManager = WallpaperManager.getInstance(this.context)
            binding.headImg.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.headImg.setImageDrawable(wallpaperManager.drawable)
            UnboundedImageViewHelper.with(binding.headImg).addClickListener(object : UnboundedImageViewHelper.ClickListener {
                override fun onClick(view: View, count: Int) {
                    iconHelper?.let { refresh(it) }
                }
            })
        } else {
            binding.headImg.setImageResource(AndIconKit.createMakerInfoProvider(requireContext())?.background?:0)
            binding.headImg.scaleType = ImageView.ScaleType.CENTER_CROP
            UnboundedImageViewHelper.with(binding.headImg).addClickListener(object : UnboundedImageViewHelper.ClickListener {
                override fun onClick(view: View, count: Int) {
                    iconHelper?.let { refresh(it) }
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}