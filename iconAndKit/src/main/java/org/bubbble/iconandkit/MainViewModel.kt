package org.bubbble.iconandkit

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lollipop.iconcore.ui.IconHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bubbble.iconandkit.base.BackPressedListener
import org.bubbble.iconandkit.ui.request.RequestItem
import org.bubbble.life.shared.util.logger

/**
 * @author Andrew
 * @date 2020/10/28 17:59
 */
class MainViewModel : ViewModel() {

    var xmlMap: IconHelper.DefaultXmlMap? = null

    private var iconPackHelper = IconHelper.iconPackOnly(false) {
        val a = AndIconKit.createAppsPageMap(it)
        xmlMap = a as IconHelper.DefaultXmlMap?
        a
    }

    private val iconUnsupportedHelper = IconHelper.unsupportedOnly {
        AndIconKit.createRequestPageMap(it)
    }

    private val iconSupportedHelper = IconHelper.supportedOnly {
        AndIconKit.createHomePageMap(it)
    }
    val onIconPackOnlyData = MutableLiveData<IconHelper>()
    val onUnsupportedOnlyData = MutableLiveData<IconHelper>()
    val onSupportedOnlyData = MutableLiveData<IconHelper>()

    fun setupIconHelper(context: Context) {
        if (onIconPackOnlyData.value == null) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    iconPackHelper.loadAppInfo(context)
                    iconUnsupportedHelper.loadAppInfo(context)
                    iconSupportedHelper.loadAppInfo(context)
                    onIconPackOnlyData.postValue(iconPackHelper)
                    onUnsupportedOnlyData.postValue(iconUnsupportedHelper)
                    onSupportedOnlyData.postValue(iconSupportedHelper)
                    loadUnsupported()
                }
            }
        } else {
            onIconPackOnlyData.value = iconPackHelper
            onUnsupportedOnlyData.value = iconUnsupportedHelper
            onSupportedOnlyData.value = iconSupportedHelper
            loadUnsupported()
        }
    }

    val unsupported = MutableLiveData<MutableList<RequestItem>>()

    private fun loadUnsupported() {

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val list = mutableListOf<RequestItem>()
                list.add(RequestItem(null, iconUnsupportedHelper.supportedCount, iconUnsupportedHelper.notSupportCount, isHeader = true))
                for (index in 0 until iconUnsupportedHelper.notSupportCount) {
                    val info = iconUnsupportedHelper.getNotSupportInfo(index)
                    list.add(RequestItem(info, null, null, isHeader = false))
                }
                unsupported.postValue(list)
            }
        }
    }

    private var backPressed: ArrayList<BackPressedListener> = ArrayList()
    fun addBackPressedListener(backPressed: BackPressedListener) {
        this.backPressed.add(backPressed)
    }
    fun removeBackPressedListener(backPressed: BackPressedListener) {
        this.backPressed.remove(backPressed)
    }
    fun getBackPressedListener(): ArrayList<BackPressedListener> {
        return backPressed
    }

    private var adaptIconPage: ((Int) -> Unit)? = null
    fun getAdaptIconPage(): ((Int) -> Unit)? {
        return adaptIconPage
    }
    fun setAdaptIconPage(adaptIconPage : (Int) -> Unit) {
        this.adaptIconPage = adaptIconPage
    }

    private var adaptedIconPage: ((Int) -> Unit)? = null
    fun getAdaptedIconPage(): ((Int) -> Unit)? {
        return adaptedIconPage
    }
    fun setAdaptedIconPage(adaptedIconPage : (Int) -> Unit) {
        this.adaptedIconPage = adaptedIconPage
    }
}