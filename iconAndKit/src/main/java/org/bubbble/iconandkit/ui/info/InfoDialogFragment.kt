package org.bubbble.iconandkit.ui.info

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lollipop.iconcore.util.UpdateInfoManager
import com.lollipop.iconcore.util.doAsync
import com.lollipop.iconcore.util.onUI
import org.bubbble.iconandkit.AndIconKit
import org.bubbble.iconandkit.databinding.FragmentInfoDialogBinding
import org.bubbble.iconandkit.databinding.ItemUpdateInfoBinding
import java.lang.StringBuilder

/**
 * A simple [Fragment] subclass.
 * Use the [InfoDialogFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class InfoDialogFragment : AppCompatDialogFragment() {

    private lateinit var binding: FragmentInfoDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // 我们想创建一个对话框，但是我们也想对内容视图使用DataBinding。
        // 我们可以通过创建一个空对话框并稍后添加内容来做到这一点。
        return MaterialAlertDialogBuilder(requireContext()).create()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInfoDialogBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = InfoAdapter()
        binding.infoList.layoutManager = LinearLayoutManager(context)
        binding.infoList.adapter = adapter
        doAsync {
            AndIconKit.createUpdateInfoProvider(requireContext())?.let {
                val updateInfo = UpdateInfoManager(it)

                val mutableList = mutableListOf<UpdateInfoManager.VersionInfo>()
                for (position in 0 until updateInfo.infoCount) {
                    mutableList.add(updateInfo.getVersionInfo(position))
                }

                onUI {
                    adapter.submitList(mutableList)
                }
            }

        }

        binding.close.setOnClickListener {
            dismiss()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (showsDialog) {
            (requireDialog() as AlertDialog).setView(binding.root)
        }
    }

    class InfoAdapter : ListAdapter<UpdateInfoManager.VersionInfo, InfoAdapter.InfoViewHolder>(InfoDiffCallback) {

        inner class InfoViewHolder(private val binding: ItemUpdateInfoBinding) : RecyclerView.ViewHolder(binding.root) {
            fun onBind(info: UpdateInfoManager.VersionInfo) {
                binding.appVersion.text = info.name

                val stringBuilder = StringBuilder()
                for ((index, value) in info.info.withIndex()) {
                    if (index == info.info.size -1) {
                        stringBuilder.append("• $value")
                    } else {
                        stringBuilder.append("• $value \r\n")
                    }
                }
                binding.updateMessage.text = stringBuilder
            }
        }

        object InfoDiffCallback : DiffUtil.ItemCallback<UpdateInfoManager.VersionInfo>() {

            override fun areItemsTheSame(oldItem: UpdateInfoManager.VersionInfo, newItem: UpdateInfoManager.VersionInfo): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: UpdateInfoManager.VersionInfo, newItem: UpdateInfoManager.VersionInfo): Boolean {
                return oldItem.name == newItem.name
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return InfoViewHolder(ItemUpdateInfoBinding.inflate(inflater, parent, false))
        }

        override fun onBindViewHolder(holder: InfoViewHolder, position: Int) {
            holder.onBind(getItem(position))
        }
    }
}