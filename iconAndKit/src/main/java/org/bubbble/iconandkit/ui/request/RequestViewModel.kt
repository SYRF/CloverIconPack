package org.bubbble.iconandkit.ui.request

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lollipop.iconcore.ui.IconHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bubbble.iconandkit.AndIconKit

/**
 * @author Andrew
 * @date 2020/10/30 19:02
 */
class RequestViewModel : ViewModel() {

    private val iconHelper = IconHelper.unsupportedOnly {
        AndIconKit.createRequestPageMap(it)
    }

    val onLoadData = MutableLiveData<IconHelper>()

    fun setupIconHelper(context: Context) {
        if (onLoadData.value == null) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    iconHelper.loadAppInfo(context)
                    onLoadData.postValue(iconHelper)
                    loadUnsupported()
                }
            }
        } else {
            onLoadData.value = iconHelper
            if (unsupported.value == null) {
                loadUnsupported()
            }
        }
    }

    val unsupported = MutableLiveData<MutableList<RequestItem>>()

    private fun loadUnsupported() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val list = mutableListOf<RequestItem>()
                list.add(RequestItem(null, iconHelper.supportedCount, iconHelper.notSupportCount, isHeader = true))
                for (index in 0 until iconHelper.notSupportCount) {
                    val info = iconHelper.getNotSupportInfo(index)
                    list.add(RequestItem(info, null, null, isHeader = false))
                }
                unsupported.postValue(list)
            }
        }
    }
}