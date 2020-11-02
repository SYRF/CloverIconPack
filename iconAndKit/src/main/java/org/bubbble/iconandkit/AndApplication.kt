package org.bubbble.iconandkit

import com.lollipop.iconcore.IconPackCore
import com.lollipop.iconcore.provider.MainPageProvider
import com.lollipop.iconcore.provider.MainPageRenderer
import com.lollipop.iconcore.ui.IconApplication

/**
 * @author Andrew
 * @date 2020/10/28 9:55
 */
open class AndApplication : IconApplication(), MainPageProvider {

    override fun onCreate() {
        super.onCreate()
        IconPackCore.init(this)
    }

    override fun createRenderer(): MainPageRenderer {
        return MainActivity()
    }
}