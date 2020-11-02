package org.bubbble.iconandkit.ui.request

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lollipop.iconcore.util.*
import org.bubbble.iconandkit.AndIconKit
import org.bubbble.iconandkit.MainViewModel
import org.bubbble.iconandkit.R
import org.bubbble.iconandkit.databinding.FragmentRequestBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max


/**
 * A simple [Fragment] subclass.
 * Use the [RequestFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RequestFragment : Fragment() {

    private lateinit var binding: FragmentRequestBinding

    private val activityViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = RequestAdapter {
            binding.sendRequest.text = "已选择 $it"
        }

        binding.sendRequest.setOnClickListener {
            createRequest(adapter.selectedApp)
        }

        binding.requestList.layoutManager = LinearLayoutManager(context)
        binding.requestList.adapter = adapter

        activityViewModel.unsupported.observe(viewLifecycleOwner, {
            adapter.submitList(it)
        })
    }


    private fun createRequest(selectedApp: ArrayList<RequestItem>) {
        if (selectedApp.isEmpty()) {
            return
        }
        val cont = activity?:return
        binding.zipLoad.progress = 0
        binding.zipLoad.visibility = View.VISIBLE
        doAsync {
            val builder = XmlBuilder.create(cont, selectedApp.size) { selectedApp[it].info!! }
            val display = cont.resources.displayMetrics
            builder.addComment("OS: ${android.os.Build.VERSION.RELEASE}")
                .addComment("Api: ${android.os.Build.VERSION.SDK_INT}")
                .addComment("Model: ${android.os.Build.MODEL}")
                .addComment("Brand: ${android.os.Build.BRAND}")
                .addComment("Product: ${android.os.Build.PRODUCT}")
                .addComment("Rom: ${android.os.Build.DISPLAY}")
                .addComment("DPI: ${display.densityDpi}")
                .addComment("Screen: ${display.widthPixels} * ${display.heightPixels}")
                .addComment("Language: ${Locale.getDefault().language}")
                .addComment("App: ${cont.versionName()}")

            val cacheDir = cont.cacheDir
            val xmlFile = File(cacheDir, "request.xml")
            builder.writeTo(xmlFile)

            val iconSaveHelper = IconSaveHelper(220)
            for ((index, icon) in selectedApp.withIndex()) {
                iconSaveHelper.add(icon.info!!.loadIcon(cont), icon.info.drawableName)
                onUI {
                    val progress = 100 * (index.toFloat() / selectedApp.size.toFloat())
                    binding.zipLoad.progress = progress.toInt()
                }
            }
            iconSaveHelper.saveTo(cacheDir)

            onUI {
                binding.zipLoad.visibility = View.GONE
            }

            ZipHelper.zipTo(cacheDir, "Request_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}_${selectedApp.size}")
                .addFile(xmlFile)
                .addFiles(iconSaveHelper.getFiles())
                .removeExists()
                .startUp { zipFile ->
                    emailTo(zipFile)
                }
        }
    }

    private fun emailTo(path: File){
        // 必须明确使用mailto前缀来修饰邮件地址,如果使用
        // intent.putExtra(Intent.EXTRA_EMAIL, email)，结果将匹配不到任何应用
        val requireActivity = requireActivity()
        val emailId = AndIconKit.createMakerInfoProvider(requireActivity)?.email?:0
        val email = resources.getString(emailId)
        val uri = Uri.parse("mailto:$email")
        val intent = Intent(Intent.ACTION_SEND, uri)
        intent.type = "application/octet-stream"
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        intent.putExtra(Intent.EXTRA_SUBJECT,"${getString(R.string.app_name)} ${getString(R.string.email_request_subject)}") // 主题
        intent.putExtra(Intent.EXTRA_TEXT, requireActivity.packageName) // 正文
        intent.putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(requireActivity,
                "${requireActivity.packageName}.provider", path))
        startActivity(Intent.createChooser(intent, getString(R.string.title_choose_email)))
    }

}