package org.bubbble.iconandkit.ui.apply

import android.content.Context
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.request.RequestOptions
import org.bubbble.iconandkit.R
import org.bubbble.iconandkit.databinding.ItemLauncherBinding
import org.bubbble.iconandkit.databinding.ItemTipsCardBinding
import org.bubbble.iconandkit.util.Utils


/**
 * @author Andrew
 * @date 2020/10/29 9:46
 */
internal class LauncherAdapter(private val context: Context, private var listener: (item: LauncherItem) -> Unit) : ListAdapter<LauncherItem, ApplyViewHolder>(LauncherDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_launcher -> {
                ApplyViewHolder.LauncherViewHolder(
                    ItemLauncherBinding.inflate(inflater, parent, false)
                )
            }

            R.layout.item_link -> {
                ApplyViewHolder.TipsViewHolder(
                    ItemTipsCardBinding.inflate(inflater, parent, false)
                )
            }
            else -> throw IllegalArgumentException("Invalid viewType")
        }
    }

    override fun onBindViewHolder(holder: ApplyViewHolder, position: Int) {
        val data = getItem(position) as LauncherItem
        if (holder is ApplyViewHolder.LauncherViewHolder) {
            holder.onBind(context, listener, data)
        }

        if (holder is ApplyViewHolder.TipsViewHolder) {
            holder.onBind(listener, data)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item.isTips) {
            false -> R.layout.item_launcher
            true -> R.layout.item_link
        }
    }

    override fun submitList(list: List<LauncherItem>?) {
        super.submitList(list?.let { ArrayList(it) })
    }

}

internal sealed class ApplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    class LauncherViewHolder(
        val binding: ItemLauncherBinding
    ) : ApplyViewHolder(binding.root) {

        fun onBind(context: Context, listener: (item: LauncherItem) -> Unit, item: LauncherItem) {
            binding.run {
                launcherCard.setOnClickListener {
                    listener(item)
                }

                val iconName = "ic_" + item.name.toLowerCase().replace(" ", "_")
                val iconResource = Utils.getIconResId(context.resources,context.packageName,iconName)

                val option = RequestOptions().priority(Priority.IMMEDIATE)

                Glide.with(launcherLogo)
                    .load(
                        if (iconResource != 0)
                            iconResource
                        else
                            Utils.getIconResId(
                                context
                                    .resources, context.packageName, "ic_na_launcher"
                            )
                    ).apply(option)
                    .into(launcherLogo)
                launcherName.text = item.name

                if (isInstalled(context,item.isInstalled,item.packageName)){
                    launcherLogo.colorFilter = null
                    launcherName.setBackgroundColor(item.launcherColor)
                    launcherName.setTextColor(ContextCompat.getColor(context, R.color.white))
                }else{
                    launcherLogo.colorFilter = bnwFilter()
                    launcherName.setBackgroundColor(Color.TRANSPARENT)
                    launcherName.setTextColor(ContextCompat.getColor(context, R.color.scrim))
                }
            }
        }

        private fun isInstalled(context: Context, isInstall: Int, packageName: String): Boolean {
            var endData = false
            if (isInstall == -1) {
                if ("org.cyanogenmod.theme.chooser" == packageName) {
                    if (Utils.isAppInstalled(context, "org.cyanogenmod.theme.chooser") || Utils.isAppInstalled(
                            context,
                            "com.cyngn.theme.chooser"
                        )
                    ) {
                        return true
                    }
                } else {
                    endData = Utils.isAppInstalled(context, packageName)
                }
            }
            // Caches this value, checking if a launcher is installed is intensive on processing
            return endData
        }

        private fun bnwFilter(): ColorFilter {
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            return ColorMatrixColorFilter(matrix)
        }

    }

    class TipsViewHolder(
        val binding: ItemTipsCardBinding
    ) : ApplyViewHolder(binding.root) {
        fun onBind(listener: (item: LauncherItem) -> Unit, item: LauncherItem) {
            binding.dismissCard.setOnClickListener {
                listener(item)
            }
        }
    }
}

object LauncherDiffCallback : DiffUtil.ItemCallback<LauncherItem>() {

    override fun areItemsTheSame(oldItem: LauncherItem, newItem: LauncherItem): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: LauncherItem, newItem: LauncherItem): Boolean {
        return oldItem.name == newItem.name && oldItem.packageName == newItem.packageName
    }

}