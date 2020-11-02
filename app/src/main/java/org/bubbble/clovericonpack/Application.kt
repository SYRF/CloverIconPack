package org.bubbble.clovericonpack

import android.content.Context
import com.lollipop.iconcore.ui.IconHelper
import com.lollipop.iconcore.util.ExternalLinkManager
import com.lollipop.iconcore.util.MakerInfoManager
import com.lollipop.iconcore.util.UpdateInfoManager
import org.bubbble.iconandkit.AndApplication
import org.bubbble.iconandkit.AndIconKit

/**
 * @author Andrew
 * @date 2020/11/02 22:09
 */
class Application: AndApplication() {

    override fun onCreate() {
        super.onCreate()
        AndIconKit.init(object : AndIconKit.IconMapCreator{
            override fun createHomePageMap(context: Context): IconHelper.DrawableMap {
                return AndIconKit.createDefXmlMapFromResource(context, R.xml.appfilter)
            }

            override fun createAppsPageMap(context: Context): IconHelper.DrawableMap {
                return AndIconKit.createDefXmlMapFromResource(context, R.xml.drawable)
            }

            override fun createRequestPageMap(context: Context): IconHelper.DrawableMap {
                return AndIconKit.createDefXmlMapFromResource(context, R.xml.appfilter)
            }

            override fun createUpdateInfoProvider(context: Context):
                    UpdateInfoManager.UpdateInfoProvider? {
                return AndIconKit.readUpdateInfoByXml(context, R.xml.updates)
            }

            override fun createLinkInfoProvider(context: Context):
                    ExternalLinkManager.ExternalLinkProvider? {
                return AndIconKit.readLinkInfoByXml(context, R.xml.links)
            }

            override fun createMakerInfoProvider(context: Context): MakerInfoManager.MakerInfoProvider? {
                return object: MakerInfoManager.MakerInfoProvider {
                    override val icon = R.drawable.author_icon
                    override val name = R.string.maker_name
                    override val signature = R.string.maker_sign
                    override val mottoArray = R.array.maker_motto
                    override val background = R.drawable.wallpaper
                    override val email = R.string.maker_email
                    override val appIcon = R.mipmap.ic_launcher_round
                }
            }
        })
    }
}