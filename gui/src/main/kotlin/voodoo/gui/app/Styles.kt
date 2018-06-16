package voodoo.gui.app

import javafx.scene.text.FontWeight
import tornadofx.*

class Styles : Stylesheet() {
    companion object {
        val heading by cssclass()
        val wrapper by cssclass()
    }

    init {
        label and heading {
            padding = box(10.px)
            fontSize = 20.px
            fontWeight = FontWeight.BOLD
        }
        s(wrapper) {
            padding = box(10.px)
            spacing = 10.px
        }
//        button {
//            backgroundColor = multi(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)
//            backgroundRadius += box(3.px)
//            backgroundInsets += box(0.px)
//        }
    }
}