package moe.nikky.gui

import com.googlecode.lanterna.gui2.Interactable
import com.googlecode.lanterna.gui2.TextBox
import com.googlecode.lanterna.input.KeyStroke

/**
 * Created by nikky on 14/05/2017.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
class TextField(initialContent: String) : TextBox(initialContent) {

    private var onChange: OnChange? = null

    fun onChange(onChange: OnChange): TextField {
        this.onChange = onChange
        return this
    }

    @Synchronized override fun handleKeyStroke(keyStroke: KeyStroke): Interactable.Result {
        val text = text
        val r = super.handleKeyStroke(keyStroke)
        if (text != getText() && r == Interactable.Result.HANDLED && onChange != null) onChange!!.run(getText(), "handleKeyStroke")
        return r
    }

    //    protected void afterLeaveFocus(FocusChangeDirection direction, Interactable nextInFocus) {
    //        if(onChange != null)
    //            onChange.run(getText(), "afterLeaveFocus");
    //    }

    //    protected void afterEnterFocus(FocusChangeDirection direction, Interactable nextInFocus) {
    //        if(onChange != null)
    //            onChange.run(getText(), "afterEnterFocus");
    //    }

    interface OnChange {
        fun run(text: String, event: String)
    }

    override fun toString(): String {
        return String.format("TextField{\"%s\"}", text)
    }
}
