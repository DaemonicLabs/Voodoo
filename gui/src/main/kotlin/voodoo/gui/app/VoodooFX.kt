package voodoo.gui.app

import com.jfoenix.controls.JFXDecorator
import com.jfoenix.svg.SVGGlyph
import javafx.scene.Scene
import javafx.stage.Stage
import tornadofx.*
import voodoo.gui.view.MainView

class VoodooFX : App(MainView::class, Styles::class) {
    init {
        importStylesheet("/css/jfoenix-fonts.css")
        importStylesheet("/css/jfoenix-design.css")
        importStylesheet("/css/jfoenix-components.css")
        importStylesheet("/css/jfoenix-main-demo.css")
    }

    private lateinit var stage: Stage

    override fun start(stage: Stage) {
        this.stage = stage
        super.start(stage)
    }

    override fun createPrimaryScene(view: UIComponent): Scene {
        val decorator = JFXDecorator(stage, view.root)
        decorator.isCustomMaximize = true
        decorator.setGraphic(SVGGlyph(""))
        return Scene(decorator)
    }

}