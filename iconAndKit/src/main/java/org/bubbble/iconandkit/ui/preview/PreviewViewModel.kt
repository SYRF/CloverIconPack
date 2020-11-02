package org.bubbble.iconandkit.ui.preview

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lollipop.iconcore.ui.IconHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bubbble.iconandkit.AndIconKit
import org.bubbble.iconandkit.result.Event
import org.bubbble.life.shared.util.logger

/**
 * @author Andrew
 * @date 2020/10/28 16:19
 */
class PreviewViewModel : ViewModel() {

    private val _permissionAction = MutableLiveData<Boolean>()
    val permissionAction: LiveData<Boolean>
        get() = _permissionAction

    /**
     * 获取必要权限
     */
    fun getPermission(context: Fragment){
        val permission = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE)
        permissionList = mutableListOf()
        permissionList.clear()

        //获取未授权的权限
        for (value in permission){
            if (ContextCompat.checkSelfPermission(context.requireContext(), value) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(value)
            }
        }
        if (permissionList.isNotEmpty()){
            _permissionAction.value = false
            //请求权限方法
            val permissions = permissionList.toTypedArray()
            context.requestPermissions(permissions, PERMISSION_CODE)
        }else{
            _permissionAction.value = true
        }
    }

    /**
     * 获取未授权的权限
     */
    private lateinit var permissionList: MutableList<String>

    companion object {
        const val PERMISSION_CODE = 1
    }

    private var iconHelper = IconHelper.supportedOnly {
        AndIconKit.createHomePageMap(it)
    }

    val onLoadData = MutableLiveData<IconHelper>()

    fun setupIconHelper(context: Context) {
        if (onLoadData.value == null) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    iconHelper.loadAppInfo(context)
                    onLoadData.postValue(iconHelper)
                }
            }
        } else {
            onLoadData.value = iconHelper
        }
    }

}