package moe.nikky.gui

import com.googlecode.lanterna.bundle.LanternaThemes
import com.googlecode.lanterna.graphics.Theme
import com.googlecode.lanterna.gui2.AbstractComponent
import com.googlecode.lanterna.gui2.Component
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.WindowBasedTextGUI
import moe.nikky.util.Tuple

import java.util.ArrayList

/**
 * Created by nikky on 12/10/16.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
object Settings {
    private var gui: WindowBasedTextGUI? = null
    val default_theme = "businessmachine"
    private var theme: String? = null
    //private static ArrayList<String> themes = new ArrayList<>();
    private val themeOverrides = ArrayList<Tuple<Component, String>>()
    private var init = false

    private var settingsPanel: Panel? = null
    private var themeSwitcher: ActionList? = null

    @JvmOverloads
    fun init(gui: WindowBasedTextGUI, theme: String = default_theme) {
        //themes.addAll(LanternaThemes.getRegisteredThemes());
        Settings.gui = gui
        setTheme(theme)
        init = true
    }

    private fun theme(theme: String?): Theme {
        return LanternaThemes.getRegisteredTheme(theme)
    }

    fun setTheme(theme: String?) {
        Settings.theme = theme
        gui!!.theme = LanternaThemes.getRegisteredTheme(theme)
        for (pair in themeOverrides) {
            pair.left?.theme = theme(pair.right)
        }
    }

    fun applyTheme() {
        setTheme(theme)
    }

    fun addThemeOverride(component: AbstractComponent<*>, theme: String?) {
        if (init) {
            component.theme = theme(theme)
            themeOverrides.add(Tuple(component, theme))
            if (theme != null) {
                applyTheme()
            }
        } else {
            LanternaGui.logger.severe("Settings not initilaized")
        }
    }

    fun makeThemeSwitcher(): ActionList {
        if (themeSwitcher != null) {
            return themeSwitcher as ActionList
        }
        themeSwitcher = ActionList()
        for (theme in LanternaThemes.getRegisteredThemes()) {
            themeSwitcher!!.addAction(theme, Runnable { setTheme(theme) })
        }
        return themeSwitcher as ActionList
    }

    fun settingsPanel(): Panel {
        if (settingsPanel == null) {
            settingsPanel = Panel()
            makeThemeSwitcher().addTo(settingsPanel!!)
        }
        return settingsPanel as Panel
    }
}
