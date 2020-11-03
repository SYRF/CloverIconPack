package org.bubbble.iconandkit.ui.request

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.iconcore.ui.IconHelper
import org.bubbble.iconandkit.R
import org.bubbble.iconandkit.databinding.ItemHeaderRequestsBinding
import org.bubbble.iconandkit.databinding.ItemRequestAppBinding
import org.bubbble.iconandkit.ui.adapt.IconsItem
import org.bubbble.iconandkit.ui.adapt.IconsViewHolder

/**
 * @author Andrew
 * @date 2020/10/30 9:41
 */
internal class RequestAdapter(private var listener: (Int) -> Unit) : ListAdapter<RequestItem, RequestViewHolder>(RequestDiffCallback) {

    val selectedApp = ArrayList<RequestItem>()

    companion object {
        private const val HEADER_EMPTY = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
                R.layout.item_request_app -> {
                    RequestViewHolder.AppViewHolder(
                        ItemRequestAppBinding.inflate(inflater, parent, false)
                    )
                }

                R.layout.item_header_requests -> {
                    RequestViewHolder.HeaderViewHolder(
                        ItemHeaderRequestsBinding.inflate(inflater, parent, false)
                    )
                }
                else -> throw IllegalArgumentException("Invalid viewType")
            }
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val data = getItem(position) as RequestItem
        if (holder is RequestViewHolder.AppViewHolder) {
            holder.onBind(data, ::onAppClick, ::isChecked)
        }

        if (holder is RequestViewHolder.HeaderViewHolder) {
            holder.onBind(data, ::selectAll)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item.isHeader) {
            false -> R.layout.item_request_app
            true -> R.layout.item_header_requests
        }
    }

    override fun submitList(list: List<RequestItem>?) {
        super.submitList(list?.let { ArrayList(it) })
    }

    private fun onAppClick(position: Int): Boolean {
        if (currentList.isEmpty()) {
            return false
        }
        val real = position
        val app = currentList[real]
        if (selectedApp.remove(app)) {
            listener(selectedApp.size)
            return false
        }
        selectedApp.add(app)
        listener(selectedApp.size)
        return true
    }

    private fun selectAll() {
        if (selectedApp.size < currentList.size - HEADER_EMPTY) {

            selectedApp.clear()
            for (value in HEADER_EMPTY until currentList.size) {
                selectedApp.add(currentList[value])
            }
        } else {
            selectedApp.clear()
        }
        listener(selectedApp.size)
        notifyDataSetChanged()
    }

    private fun isChecked(position: Int): Boolean {
        if (currentList.isEmpty()) {
            return false
        }
        val app = currentList[position]
        return selectedApp.contains(app)
    }

}

internal sealed class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    class AppViewHolder(private val binding: ItemRequestAppBinding): RequestViewHolder(binding.root) {
        fun onBind(data: RequestItem,
                   onClick: (Int) -> Boolean,
                   isChecked: (Int) -> Boolean) {
            data.info?.let {
                binding.imgIcon.loadAppIcon(it)
                binding.appName.text = it.getLabel(binding.appName.context)
                binding.requestCard.setOnClickListener {
                    binding.chkSelected.isChecked = onClick(adapterPosition)
                }
                binding.chkSelected.isChecked = isChecked(adapterPosition)
            }
        }
    }

    class HeaderViewHolder(private val binding: ItemHeaderRequestsBinding): RequestViewHolder(binding.root) {
        fun onBind(data: RequestItem,
                   onClick: () -> Unit) {
            binding.adaptation.text = "适配 ${data.adapted}"
            binding.notAdaptation.text = "未适配 ${data.notAdapt}"
            binding.selectAll.setOnClickListener {
                onClick()
            }
        }
    }
}

object RequestDiffCallback : DiffUtil.ItemCallback<RequestItem>() {

    override fun areItemsTheSame(oldItem: RequestItem, newItem: RequestItem): Boolean {
        return oldItem.info?.drawableName == newItem.info?.drawableName
    }

    override fun areContentsTheSame(oldItem: RequestItem, newItem: RequestItem): Boolean {
        return oldItem.info?.drawableName == newItem.info?.drawableName
    }

}