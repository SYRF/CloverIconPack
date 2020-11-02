package org.bubbble.iconandkit.ui.author

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lollipop.iconcore.util.MakerInfoManager
import org.bubbble.bubbble.utils.load
import org.bubbble.iconandkit.AndIconKit
import org.bubbble.iconandkit.R
import org.bubbble.iconandkit.databinding.FragmentAuthorDialogBinding
import org.bubbble.iconandkit.databinding.FragmentInfoDialogBinding

/**
 * A simple [Fragment] subclass.
 * Use the [AuthorDialogFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AuthorDialogFragment : AppCompatDialogFragment() {

    private lateinit var binding: FragmentAuthorDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // 我们想创建一个对话框，但是我们也想对内容视图使用DataBinding。
        // 我们可以通过创建一个空对话框并稍后添加内容来做到这一点。
        return MaterialAlertDialogBuilder(requireContext()).create()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAuthorDialogBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val makerInfoManager = MakerInfoManager(AndIconKit.createMakerInfoProvider(view.context))
        binding.run {
            close.setOnClickListener {
                dismiss()
            }
            authorName.setText(makerInfoManager.name)
            authorAvatar.load(makerInfoManager.icon)
            authorMessage.setText(makerInfoManager.signature)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (showsDialog) {
            (requireDialog() as AlertDialog).setView(binding.root)
        }
    }
}