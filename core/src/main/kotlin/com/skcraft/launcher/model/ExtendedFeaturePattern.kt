package com.skcraft.launcher.model

import com.skcraft.launcher.builder.FnPatternList
import com.skcraft.launcher.model.modpack.Feature
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerialSaver
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.serializer
import kotlinx.serialization.set

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@Serializable
data class ExtendedFeaturePattern(
    var entries: Set<String>,
    @Serializable(with = Feature.Companion::class)
    var feature: Feature,
    @Optional
    @Serializable(with = FnPatternList.Companion::class)
    var files: FnPatternList = FnPatternList()
) {
    @Serializer(forClass = ExtendedFeaturePattern::class)
    companion object {
        override fun save(output: KOutput, obj: ExtendedFeaturePattern) {
            val elemOutput = output.writeBegin(serialClassDesc)

            elemOutput.writeElement(serialClassDesc, 0)
            elemOutput.write(String.serializer().set, obj.entries)

            elemOutput.writeElement(serialClassDesc, 1)
            elemOutput.write(Feature.Companion, obj.feature)

            with(ExtendedFeaturePattern(obj.entries, obj.feature)) {
                elemOutput.serializeObj(this.files, obj.files, FnPatternList.Companion, 2)
            }

            elemOutput.writeEnd(serialClassDesc)
        }

        private fun <T : Any?> KOutput.serializeObj(default: T?, actual: T, saver: KSerialSaver<T>, index: Int) {
            if (default != actual || default != null) {
                this.writeElement(serialClassDesc, index)
                this.write(saver, actual)
            }
        }
    }
}
