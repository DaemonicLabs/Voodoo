package moe.nikky.gui

import com.googlecode.lanterna.gui2.*
import moe.nikky.gui.content.BaseMatrixPanel
import moe.nikky.gui.content.ContentMatrixPanel

/**
 * Created by nikky on 01/11/16.
 * basically not more than a list of buttons that execute actions
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
class ActionList : Panel {
    private var panel: Panel? = null

    constructor() : super() {}

    constructor(panel: Panel) : super() {
        this.panel = panel
    }

    fun setDefaultPanel(panel: Panel): ActionList {
        this.panel = panel
        return this
    }

    /**
     * add button with a runnable action
     * @param label button label
     * @param action rrunnable that will be executed on buttonpress
     * @return button with referenced action
     */
    fun addAction(label: String, action: Runnable): ActionList {
        val button = Button(label, action)
        this.addComponent(button)
        return this
    }

    /**
     * add action on default panel
     * @param label button label
     * @param component component that will be displayed
     * @return `this` for method chaining
     */
    fun addItem(label: String, component: Component): ActionList {
        return addItem(label, panel, component)
    }


    /**
     * add component to be opened in the specified panel
     *
     * @param label button label
     * @param base matrix base panel
     * @param content content that will be displayed under the input panel
     * @return `this` for method chaining
     */
    fun addItem(label: String, base: BaseMatrixPanel, content: ContentMatrixPanel): ActionList {
        return addItem(label, panel, base, content)
    }

    /**
     * add component to be opened in the specified panel
     *
     * @param label button label
     * @param panel target that component will be opened in
     * @param component component that will be displayed
     * @return `this` for method chaining
     */
    private fun addItem(label: String, panel: Panel?, component: Component): ActionList {
        if (panel == null) throw NullPointerException("panel cannot be null")
        val button = Button(label, open_in(panel, component))
        this.addComponent(button)
        return this
    }

    private fun addItem(label: String, panel: Panel?, base: BaseMatrixPanel, content: ContentMatrixPanel): ActionList {
        if (panel == null) throw NullPointerException("panel cannot be null")
        val button = Button(label, open_in(panel, base, content))
        this.addComponent(button)
        return this
    }

    /**
     * create a action that opens a actionlist
     * @param sublist nested sublist
     * @param sidePanel panel that the actionlist will be opened in
     * @param label list name
     * @return `this` for method chaining
     */
    fun sublist(sublist: ActionList, sidePanel: Panel, label: String): ActionList {
        sublist.addItem("..", sidePanel, this)
        this.addItem(label + "/", sidePanel, sublist)
        return this
    }

    /**
     *
     * @param panel will be cleared to add `component`
     * @param component item to be added to `panel`
     * @return runnable that opens `component` in `panel`
     */
    private fun open_in(panel: Panel, component: Component): Runnable {
        return Runnable {
            panel.removeAllComponents()
            panel.addComponent(component)
            //panel.setPreferredSize(panel.calculatePreferredSize());
            focus(panel)

            //            if(component instanceof Container) {
            //                for(Component c : ((Container)component).getChildren()){
            //                    if(c instanceof Interactable) {
            //                        Interactable in = (Interactable) c;
            //                        in.setContent();
            //                        return;
            //                    }
            //                }
            //            }
        }
    }

    /**
     *
     * @param base matrix base panel
     * @param content item to be added to `base`
     * @return runnable that opens `content` in `base`
     */
    private fun open_in(panel: Panel, base: BaseMatrixPanel, content: ContentMatrixPanel): Runnable {
        return Runnable {
            panel.removeAllComponents()
            panel.addComponent(base)
            base.setContent(content)
            focus(base)
        }
    }

    private fun focus(container: Container): Boolean {
        for (c in container.children) {
            if (c is Interactable) {
                c.takeFocus()
                return true
            }
            if (c is Container) {
                val found = focus(c)
                if (found) return true
            }
        }
        return false
    }
}
