package org.bubbble.iconandkit.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import org.bubbble.iconandkit.R

/**
 * @author Andrew
 * @date 2020/10/29 10:18
 */

class LauncherIntents {

    @Throws(IllegalArgumentException::class)
    constructor(context: Context, key: String) {
        when (key) {
            "com.actionlauncher.playstore", "Action" -> ActionLauncher(context)
            "org.adw.launcher", "Adw" -> AdwLauncher(context)
            "org.adwfreak.launcher", "Adwex" -> AdwEXLauncher(context)
            "com.anddoes.launcher", "Apex" -> ApexLauncher(context)
            "com.dlto.atom.launcher", "Atom" -> AtomLauncher(context)
            "com.tul.aviate", "Aviate" -> AviateLauncher(context)
            "org.cyanogenmod.theme.chooser", "org.cyanogenmod.theme.chooser2", "com.cyngn.theme.chooser", "Cmthemeengine" -> CMThemeEngine(
                context
            )
            "com.gau.go.launcherex", "Go" -> GoLauncher(context)
            "com.mobint.hololauncher", "Holo" -> HoloLauncher(context)
            "com.mobint.hololauncher.hd", "Holoics" -> HoloLauncherICS(context)
            "com.kk.launcher", "KK", "Kk" -> KkLauncher(context)
            "com.lge.launcher2", "Lghome" -> LgHomeLauncher(context)
            "com.l.launcher", "L" -> LLauncher(context)
            "com.powerpoint45.launcher", "Lucid" -> LucidLauncher(context)
            "com.jiubang.go.mini.launcher", "Mini" -> MiniLauncher(context)
            "com.gtp.nextlauncher", "Next" -> NextLauncher(context)
            "com.teslacoilsw.launcher", "Nova" -> NovaLauncher(context)
            "com.s.launcher", "S" -> SLauncher(context)
            "ginlemon.flowerfree", "Smart" -> SmartLauncher(context)
            "ginlemon.flowerpro", "Smartpro" -> SmartLauncherPro(context)
            "home.solo.launcher.free", "Solo" -> SoloLauncher(context)
            "com.tsf.shell", "Tsf" -> TsfLauncher(context)
            "sg.ruqqq.IconThemer", "Uniconpro" -> Unicon(context)
            else -> {
                Log.e("No launcher: ", key)
                throw IllegalArgumentException(
                    "Couldn't find method for launcher or package: "
                            + key
                )
            }
        }
    }

    private fun ActionLauncher(context: Context) {
        val action = context.packageManager.getLaunchIntentForPackage(
            "com.actionlauncher" +
                    ".playstore"
        )
        action!!.putExtra("apply_icon_pack", context.packageName)
        context.startActivity(action)
    }

    private fun AdwLauncher(context: Context) {
        val intent = Intent("org.adw.launcher.SET_THEME")
        intent.putExtra("org.adw.launcher.theme.NAME", context.packageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun AdwEXLauncher(context: Context) {
        val intent = Intent("org.adwfreak.launcher.SET_THEME")
        intent.putExtra("org.adwfreak.launcher.theme.NAME", context.packageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun ApexLauncher(context: Context) {
        val intent = Intent("com.anddoes.launcher.SET_THEME")
        intent.putExtra("com.anddoes.launcher.THEME_PACKAGE_NAME", context.packageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun AtomLauncher(context: Context) {
        val atom = Intent("com.dlto.atom.launcher.intent.action.ACTION_VIEW_THEME_SETTINGS")
        atom.setPackage("com.dlto.atom.launcher")
        atom.putExtra("packageName", context.packageName)
        atom.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(atom)
    }

    private fun AviateLauncher(context: Context) {
        val aviate = Intent("com.tul.aviate.SET_THEME")
        aviate.setPackage("com.tul.aviate")
        aviate.putExtra("THEME_PACKAGE", context.packageName)
        aviate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(aviate)
    }

    private fun CMThemeEngine(context: Context) {
        var themesAppInstalled = true
        val intent = Intent("android.intent.action.MAIN")
        if (Utils.isAppInstalled(context, "org.cyanogenmod.theme.chooser")) {
            intent.component = ComponentName(
                "org.cyanogenmod.theme.chooser",
                "org.cyanogenmod.theme.chooser.ChooserActivity"
            )
        } else if (Utils.isAppInstalled(context, "org.cyanogenmod.theme.chooser2")) {
            intent.component = ComponentName(
                "org.cyanogenmod.theme.chooser2",
                "org.cyanogenmod.theme.chooser2.ChooserActivity"
            )
        } else if (Utils.isAppInstalled(context, "com.cyngn.theme.chooser")) {
            intent.component = ComponentName(
                "com.cyngn.theme.chooser",
                "com.cyngn.theme.chooser.ChooserActivity"
            )
        } else {
            themesAppInstalled = false
        }
        if (themesAppInstalled) {
            intent.putExtra("pkgName", context.packageName)
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    context, "Impossible to open themes app.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context, "Themes app is not installed in this device.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun GoLauncher(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(
            "com.gau.go" +
                    ".launcherex"
        )
        val go = Intent("com.gau.go.launcherex.MyThemes.mythemeaction")
        go.putExtra("type", 1)
        go.putExtra("pkgname", context.packageName)
        context.sendBroadcast(go)
        context.startActivity(intent)
    }

    private fun HoloLauncher(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.component = ComponentName(
            "com.mobint.hololauncher", "com.mobint.hololauncher" +
                    ".Settings"
        )
        context.startActivity(intent)
    }

    private fun HoloLauncherICS(context: Context) {
        val holohdApply = Intent(Intent.ACTION_MAIN)
        holohdApply.component = ComponentName(
            "com.mobint.hololauncher.hd", "com.mobint" +
                    ".hololauncher.SettingsActivity"
        )
        context.startActivity(holohdApply)
    }

    private fun KkLauncher(context: Context) {
        val kkApply = Intent("com.kk.launcher.APPLY_ICON_THEME")
        kkApply.putExtra("com.kk.launcher.theme.EXTRA_PKG", context.packageName)
        kkApply.putExtra(
            "com.kk.launcher.theme.EXTRA_NAME",
            context.resources.getString(R.string.app_name)
        )
        context.startActivity(kkApply)
    }

    private fun LgHomeLauncher(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.component = ComponentName(
            "com.lge.launcher2", "com.lge.launcher2" +
                    ".homesettings.HomeSettingsPrefActivity"
        )
        context.startActivity(intent)
    }

    private fun LLauncher(context: Context) {
        val l = Intent("com.l.launcher.APPLY_ICON_THEME", null)
        l.putExtra("com.l.launcher.theme.EXTRA_PKG", context.packageName)
        context.startActivity(l)
    }

    private fun LucidLauncher(context: Context) {
        val lucidApply = Intent("com.powerpoint45.action.APPLY_THEME", null)
        lucidApply.putExtra("icontheme", context.packageName)
        context.startActivity(lucidApply)
    }

    private fun MiniLauncher(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.component = ComponentName(
            "com.jiubang.go.mini.launcher", "com.jiubang.go" +
                    ".mini.launcher.setting.MiniLauncherSettingActivity"
        )
        context.startActivity(intent)
    }

    private fun NextLauncher(context: Context) {
        var nextApply = context.packageManager.getLaunchIntentForPackage(
            "com.gtp" +
                    ".nextlauncher"
        )
        if (nextApply == null) {
            nextApply = context.packageManager.getLaunchIntentForPackage(
                "com.gtp" +
                        ".nextlauncher.trial"
            )
        }
        val next = Intent("com.gau.go.launcherex.MyThemes.mythemeaction")
        next.putExtra("type", 1)
        next.putExtra("pkgname", context.packageName)
        context.sendBroadcast(next)
        context.startActivity(nextApply)
    }

    private fun NovaLauncher(context: Context) {
        val intent = Intent("com.teslacoilsw.launcher.APPLY_ICON_THEME")
        intent.setPackage("com.teslacoilsw.launcher")
        intent.putExtra("com.teslacoilsw.launcher.extra.ICON_THEME_TYPE", "GO")
        intent.putExtra(
            "com.teslacoilsw.launcher.extra.ICON_THEME_PACKAGE", context
                .packageName
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun SLauncher(context: Context) {
        val s = Intent("com.s.launcher.APPLY_ICON_THEME")
        s.putExtra("com.s.launcher.theme.EXTRA_PKG", context.packageName)
        s.putExtra(
            "com.s.launcher.theme.EXTRA_NAME",
            context.resources.getString(R.string.app_name)
        )
        context.startActivity(s)
    }

    private fun SmartLauncher(context: Context) {
        val smartlauncherIntent = Intent("ginlemon.smartlauncher.setGSLTHEME")
        smartlauncherIntent.putExtra("package", context.packageName)
        context.startActivity(smartlauncherIntent)
    }

    private fun SmartLauncherPro(context: Context) {
        val smartlauncherproIntent = Intent("ginlemon.smartlauncher.setGSLTHEME")
        smartlauncherproIntent.putExtra("package", context.packageName)
        context.startActivity(smartlauncherproIntent)
    }

    private fun SoloLauncher(context: Context) {
        val soloApply = context.packageManager.getLaunchIntentForPackage(
            "home.solo" +
                    ".launcher.free"
        )
        val solo = Intent("home.solo.launcher.free.APPLY_THEME")
        solo.putExtra("EXTRA_PACKAGENAME", context.packageName)
        solo.putExtra("EXTRA_THEMENAME", context.getString(R.string.app_name))
        context.sendBroadcast(solo)
        context.startActivity(soloApply)
    }

    private fun TsfLauncher(context: Context) {
        val tsfApply = context.packageManager.getLaunchIntentForPackage("com.tsf.shell")
        val tsf = Intent("android.intent.action.MAIN")
        tsf.component = ComponentName("com.tsf.shell", "com.tsf.shell.ShellActivity")
        context.sendBroadcast(tsf)
        context.startActivity(tsfApply)
    }

    private fun Unicon(context: Context) {
        val unicon = Intent("android.intent.action.MAIN")
        unicon.addCategory("android.intent.category.LAUNCHER")
        unicon.setPackage("sg.ruqqq.IconThemer")
        context.startActivity(unicon)
    }
}