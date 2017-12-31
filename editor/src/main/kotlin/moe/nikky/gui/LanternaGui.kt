package moe.nikky.gui


import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.bundle.LanternaThemes
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.input.MouseAction
import com.googlecode.lanterna.input.MouseActionType
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.*
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorColorConfiguration
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorPalette
import moe.nikky.util.ByteArrayInOutStream

import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Arrays
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger

abstract class LanternaGui @Throws(IOException::class)
constructor() {
    val textBoxHandler: TextBoxHandler
    val centerPanel: Panel
    val sidePanel: Panel
    private val screen: Screen
    val terminal: Terminal
    private val window: SimpleWindow
    val gui: WindowBasedTextGUI

    init {
        val logTextBox = TextBox("", TextBox.Style.MULTI_LINE)
        prepLogger()
        textBoxHandler = TextBoxHandler()
        initLogger(textBoxHandler)
        textBoxHandler.setLogTextBox(logTextBox)

        logger.info("Setup terminal and screen layers")
        // Setup terminal and screen layers
        //Theme theme = LanternaThemes.getRegisteredTheme("businessmachine");
        val colorConfig = TerminalEmulatorColorConfiguration.newInstance(TerminalEmulatorPalette.GNOME_TERMINAL)
        terminal = DefaultTerminalFactory().setTerminalEmulatorColorConfiguration(colorConfig).createTerminal()
        (terminal as? ExtendedTerminal)?.setMouseCaptureMode(MouseCaptureMode.CLICK_RELEASE_DRAG)

        screen = TerminalScreen(terminal)
        screen.startScreen()

        //MAINPANEL
        logger.info("creating main panel")
        val mainPanel = Panel()
        mainPanel.layoutManager = BorderLayout()

        //CENTERPANEL
        logger.info("creating center panel")
        centerPanel = Panel()
        mainPanel.addComponent(centerPanel.withBorder(Borders.singleLineBevel("content")))

        //SIDEPANEL
        logger.info("creating side panel")
        sidePanel = Panel()
        sidePanel.layoutData = BorderLayout.Location.LEFT
        mainPanel.addComponent(sidePanel.withBorder(Borders.singleLineBevel("navigation")))

        //BOTTOMPANEL
        logger.info("creating bottom panel")
        val bottom = Panel()
        bottom.layoutManager = BorderLayout()

        logTextBox.isReadOnly = true
        logTextBox.isHorizontalFocusSwitching = false
        logTextBox.theme = LanternaThemes.getRegisteredTheme("conqueror")
        logTextBox.layoutData = BorderLayout.Location.CENTER

        bottom.addComponent(logTextBox)
        //        bottom.addComponent(commandTextBox);
        bottom.preferredSize = TerminalSize(80, 10)
        bottom.layoutData = BorderLayout.Location.BOTTOM
        mainPanel.addComponent(bottom/*.withBorder(Borders.singleLineBevel("log"))*/)


        // Create window to hold the panel
        logger.info("creating window")
        window = SimpleWindow()
        window.setComponent(mainPanel)
        window.setHints(listOf(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS/*, Window.Hint.CENTERED*/))

        logger.info("creating windowlistener")

        window.addWindowListener(object : WindowListenerAdapter() {
            override fun onResized(window: Window?, oldSize: TerminalSize?, newSize: TerminalSize?) {
                //                int sideWidth = sidePanel.calculatePreferredSize().getColumns();
                //                int centerHeight = centerPanel.calculatePreferredSize().getRows();
                val newHeight = (newSize!!.rows / 4f).toInt()
                bottom.preferredSize = TerminalSize(newSize.columns, newHeight)
                //                commandTextBox.setPreferredSize(new TerminalSize(newSize.getColumns(), 1));

                //resize other panels
                //Collection<Component> side = sidePanel.getChildren();
                val center = centerPanel.children
                for (c in center) {
                    val size = c.parent.size
                    c.preferredSize = size
                }
            }

            override fun onUnhandledInput(basePane: Window?, keyStroke: KeyStroke?, hasBeenHandled: AtomicBoolean?) {
                if (!hasBeenHandled!!.get() && keyStroke is MouseAction) {
                    val m = keyStroke as MouseAction?
                    if (m!!.button == 3) {
                        val found = window.find(m.position)
                        if (found != null)
                            logger.info("info: " + found!!.toString())
                    }
                    if (m.button == 1 && m.actionType == MouseActionType.CLICK_DOWN) {
                        window.select(m.position)
                    }
                    if (m.actionType == MouseActionType.SCROLL_UP) {
                        window.handleInput(KeyStroke(KeyType.ArrowUp))
                    }
                    if (m.actionType == MouseActionType.SCROLL_DOWN) {
                        window.handleInput(KeyStroke(KeyType.ArrowDown))
                    }
                    hasBeenHandled.set(true)
                }
            }
        })

        logger.info("Create and start gui")
        gui = MultiWindowTextGUI(screen, TextColor.ANSI.DEFAULT)

        logger.info("creating navigation")
        val navigation = buildNavigation(ActionList(centerPanel))
        sidePanel.addComponent(navigation)
        for (c in navigation.getChildren()) {
            if (c is AbstractInteractableComponent<*>) {
                (c as AbstractInteractableComponent<*>).takeFocus()
                break
            }
        }


        logger.info("initialize settings")
        Settings.init(gui)
        Settings.addThemeOverride(logTextBox, "blaster") //readability of multiline text
    }

    /**
     * starts the gui thread
     */
    fun start() {
        // Create and start gui
        gui.addWindowAndWait(window)
        println("exit gui")

    }

    private fun initLogger(textBoxHandler: TextBoxHandler): Logger {
        //textBoxHandler = new TextBoxHandler();
        val logger = Logger.getLogger("main")
        logger.addHandler(textBoxHandler)
        logger.level = Level.INFO
        return logger
    }

    /**
     * is called by the GUI builder and provides a default @ActionList to add navigation elements
     * @param navigation navigation panel
     * @return navigation panel populated with buttons
     */
    protected abstract fun buildNavigation(navigation: ActionList): ActionList

    protected fun buildSettings(settingsList: ActionList): ActionList {
        return settingsList.addItem("themes", Settings.makeThemeSwitcher())
    }

    companion object {
        val logger = Logger.getLogger("main")

        private fun prepLogger() {
            val loggingProperties = Properties()
            //handler = moe.nikky.gui.TextBoxHandler
            loggingProperties.setProperty("main.handlers", "moe.nikky.gui.TextBoxHandler")
            loggingProperties.setProperty("main.level", "INFO")

            val baios = ByteArrayInOutStream()
            val inputStream: ByteArrayInputStream
            try {
                loggingProperties.store(baios, "")
                inputStream = baios.inputStream
                LogManager.getLogManager().readConfiguration(inputStream)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }
}
