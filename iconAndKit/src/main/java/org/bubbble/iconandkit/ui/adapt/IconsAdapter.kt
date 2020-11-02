package org.bubbble.iconandkit.ui.adapt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.bubbble.bubbble.utils.load
import org.bubbble.iconandkit.R
import org.bubbble.iconandkit.databinding.ItemIconsBinding
import org.bubbble.iconandkit.databinding.ItemIconsTitleBinding
import org.bubbble.iconandkit.ui.apply.ApplyViewHolder
import org.bubbble.iconandkit.ui.apply.LauncherItem

/**
 * @author Andrew
 * @date 2020/10/30 9:53
 */
internal class IconsAdapter(private var listener: (iconView: View, icon: IconsItem) -> Unit) : ListAdapter<IconsItem, IconsViewHolder>(IconsDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_icons -> {
                IconsViewHolder.IconViewHolder(
                    ItemIconsBinding.inflate(inflater, parent, false)
                )
            }

            R.layout.item_icons_title -> {
                IconsViewHolder.IconTitleViewHolder(
                    ItemIconsTitleBinding.inflate(inflater, parent, false)
                )
            }

            else -> throw IllegalArgumentException("Invalid viewType")
        }
    }

    override fun onBindViewHolder(holder: IconsViewHolder, position: Int) {
        val data = getItem(position) as IconsItem
        if (holder is IconsViewHolder.IconViewHolder) {
            holder.onBind(listener, data)
        }
        if (holder is IconsViewHolder.IconTitleViewHolder) {
            holder.onBind(data)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item.category.isNotEmpty()) {
            false -> R.layout.item_icons
            true -> R.layout.item_icons_title
        }
    }

    override fun submitList(list: List<IconsItem>?) {
        super.submitList(list?.let { ArrayList(it) })
    }
}

internal sealed class IconsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    class IconViewHolder(
        val binding: ItemIconsBinding
    ) : IconsViewHolder(binding.root) {
        fun onBind(listener: (iconView: View, icon: IconsItem) -> Unit, data: IconsItem) {
            binding.icons.load(data.icon)
            binding.icons.setOnClickListener {
                listener(binding.icons, data)
            }
        }
    }


    class IconTitleViewHolder(
        val binding: ItemIconsTitleBinding
    ) : IconsViewHolder(binding.root) {
        fun onBind(data: IconsItem) {
            binding.title.text = data.category
        }
    }
}

object IconsDiffCallback : DiffUtil.ItemCallback<IconsItem>() {

    override fun areItemsTheSame(oldItem: IconsItem, newItem: IconsItem): Boolean {
        return oldItem.icon == newItem.icon || oldItem.category == newItem.category
    }

    override fun areContentsTheSame(oldItem: IconsItem, newItem: IconsItem): Boolean {
        return oldItem.icon == newItem.icon || oldItem.category == newItem.category
    }

}