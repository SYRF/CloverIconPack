package org.bubbble.iconandkit.ui.apply

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import org.bubbble.iconandkit.R
import org.bubbble.iconandkit.databinding.FragmentApplyBinding
import org.bubbble.iconandkit.util.LauncherIntents
import org.bubbble.iconandkit.util.Utils
import org.bubbble.life.shared.util.logger

/**
 * A simple [Fragment] subclass.
 * Use the [ApplyFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ApplyFragment : Fragment() {

    private var _binding: FragmentApplyBinding? = null

    private val binding get() = _binding!!

    private val viewModel: ApplyViewModel by viewModels()

    companion object {
        const val MARKET_URL = "https://play.google.com/store/apps/details?id="
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentApplyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.launcherList = null

        val adapter = LauncherAdapter(requireContext()) {
            if (it.isTips) {
                viewModel.hideTips(requireContext())
            } else {
                onLauncherClick(it)
            }
        }
        logger("ApplyFragment-onViewCreated")

        val layoutManager = GridLayoutManager(context, 3)

        binding.run {

            launcherList.layoutManager = layoutManager

            launcherList.itemAnimator = DefaultItemAnimator()

            launcherList.adapter = adapter
        }

        viewModel.launchers.observe(viewLifecycleOwner, {
            adapter.submitList(it)
        })

        viewModel.tipsVisible.observe(viewLifecycleOwner, {
            if (it) {
                layoutManager.spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (position == 0) 3 else 1
                    }
                }
            } else {
                val originList = viewModel.launcherList
                if (originList != null) {
                    if (originList[0].isTips) {
                        originList.removeAt(0)
                        adapter.submitList(originList)
                        layoutManager.spanSizeLookup = GridLayoutManager.DefaultSpanSizeLookup()
                    }
                }
            }
        })

        logger("线程： ${Thread.currentThread().name}")
        viewModel.loadLaunchers(requireContext())
    }

    private fun onLauncherClick(item: LauncherItem) {
        when(item.name){
            "Google Now" -> {
                val appLink = MARKET_URL + resources.getString(R.string.extraapp)
                AlertDialog.Builder(requireContext())
                    .setTitle(resources.getString(R.string.gnl_title))
                    .setMessage(resources.getString(R.string.gnl_content))
                    .setPositiveButton(
                        resources.getString(R.string.download)
                    ) { _, _ ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(appLink)
                        startActivity(intent)

                    }.setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->

                    }.show()
            }
            "LG Home" -> {
                if (Utils.isAppInstalled(
                        requireContext(), item.packageName
                    )
                ) {
                    openLauncher(item.name)
                } else {
                    AlertDialog.Builder(requireContext())
                        .setMessage(resources.getString(R.string.lg_dialog_content))
                        .setPositiveButton(
                            "Ok"
                        ) { _, _ -> }.show()
                }
            }
            "CM Theme Engine" -> {
                when {
                    Utils.isAppInstalled(
                        requireContext(),
                        "com.cyngn.theme.chooser"
                    ) -> openLauncher(
                        "CM Theme Engine"
                    )
                    Utils.isAppInstalled(
                        requireContext(),
                        item.packageName
                    ) -> openLauncher(item.name)
                    else -> openInPlayStore(item)
                }
            }else ->{
            if (Utils.isAppInstalled(requireContext(), item.packageName)){
                openLauncher(item.name)
            }else{
                openInPlayStore(item)
            }
        }
        }
    }

    private fun openLauncher(name: String) {
        val launcherName =
            Character.toUpperCase(name[0]) + name.substring(1).toLowerCase().replace(" ", "").replace(
                "launcher",
                ""
            )
        try {
            LauncherIntents(requireActivity(), launcherName)
        } catch (ex: IllegalArgumentException) {
            Snackbar.make(binding.launcherList, R.string.no_launcher_intent, Snackbar.LENGTH_LONG).show()
        }

    }

    private fun openInPlayStore(launcher: LauncherItem) {
        val intentString: String
        val launcherName = launcher.name
        val cmName = "CM Theme Engine"
        val dialogContent: String
        if (launcherName == cmName) {
            dialogContent = resources.getString(R.string.cm_dialog_content, launcher.name)
            intentString = "http://download.cyanogenmod.org/"
        } else {
            dialogContent = resources.getString(R.string.lni_content, launcher.name)
            intentString = MARKET_URL + launcher.packageName
        }

        AlertDialog.Builder(requireContext())
            .setTitle(launcher.name)
            .setMessage(dialogContent)
            .setPositiveButton(
                resources.getString(R.string.download)
            ) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(intentString)
                startActivity(intent)
            }.setNeutralButton(
                resources.getString(R.string.cancel)
            ){ _, _->

            }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}