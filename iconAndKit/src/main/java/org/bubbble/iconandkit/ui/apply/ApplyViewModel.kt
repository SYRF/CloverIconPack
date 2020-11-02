package org.bubbble.iconandkit.ui.apply

import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bubbble.iconandkit.R
import org.bubbble.iconandkit.util.PreferencesUtil
import org.bubbble.life.shared.util.logger

/**
 * @author Andrew
 * @date 2020/10/29 10:32
 */
class ApplyViewModel : ViewModel() {

    var launcherList: MutableList<LauncherItem>? = null

    private val _launchers = MutableLiveData<MutableList<LauncherItem>>()
    val launchers: LiveData<MutableList<LauncherItem>>
        get() = _launchers

    fun loadLaunchers(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                launcherList = getLaunchers(context)
                if (launcherList != null) {
                    _launchers.postValue(launcherList)
                }
            }
        }
    }

    private fun getLaunchers(context: Context): MutableList<LauncherItem> {
        return mutableListOf<LauncherItem>().apply {
            val launcherArray = context.resources.getStringArray(R.array.launchers)
            val launcherColors = context.resources.getIntArray(R.array.launcher_colors)

            logger("线程： ${Thread.currentThread().name}")
            if (tips(context)) {
                add(LauncherItem("", "", 0, isTips = true))
                _tipsVisible.postValue(true)
            } else {
                _tipsVisible.postValue(false)
            }

            for ((index,value) in launcherArray.withIndex()){
                val s = value.split("|")
                add(LauncherItem(s[0],s[1],launcherColors[index], isTips = false))
            }
        }
    }

    private val _tipsVisible = MutableLiveData<Boolean>()
    val tipsVisible: LiveData<Boolean>
        get() = _tipsVisible

    private fun tips(context: Context): Boolean {
        val a = PreferencesUtil.get(context, "tips", true)
        logger("ApplyViewModel $a")
        return a
    }

    fun hideTips(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PreferencesUtil.put(context, "tips", false)
                _tipsVisible.postValue(false)
            }
        }
    }

}