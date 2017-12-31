package moe.nikky.gui

import com.googlecode.lanterna.gui2.TextBox
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType

import java.util.logging.LogRecord
import java.util.logging.StreamHandler

/**
 * Created by nikky on 13/10/16.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
class TextBoxHandler : StreamHandler {
    private var textBox: TextBox? = null

    constructor() {}
    constructor(logTextBox: TextBox) {
        logTextBox.isReadOnly = true
        setLogTextBox(logTextBox)
    }

    fun setLogTextBox(logTextBox: TextBox) {
        logTextBox.isReadOnly = true
        this.textBox = logTextBox
    }

    override fun publish(record: LogRecord) {
        super.publish(record)
        flush()

        if (textBox != null) {
            var millis = record.millis
            val second = millis / 1000 % 60
            val minute = millis / (1000 * 60) % 60
            val hour = millis / (1000 * 60 * 60) % 24
            millis %= 1000
            textBox!!.addLine(String.format("[%02d:%02d:%02d:%03d %s %s] %s", hour, minute, second, millis,
                    record.level.localizedName,
                    record.sourceClassName.replaceFirst("moe\\.nikky\\.".toRegex(), ""),
                    record.message))
            //TODO use autoscroll setting
            textBox!!.handleKeyStroke(KeyStroke(KeyType.End))
        }
    }

}
