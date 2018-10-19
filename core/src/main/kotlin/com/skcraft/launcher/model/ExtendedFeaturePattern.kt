package com.skcraft.launcher.model

import com.skcraft.launcher.builder.FnPatternList
import com.skcraft.launcher.model.modpack.Feature
import kotlinx.serialization.CompositeEncoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
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
        override fun serialize(output: Encoder, obj: ExtendedFeaturePattern) {
            val elemOutput = output.beginStructure(descriptor)
            elemOutput.encodeSerializableElement(descriptor, 0, String.serializer().set, obj.entries)
            elemOutput.encodeSerializableElement(descriptor, 1, Feature.Companion, obj.feature)
            with(ExtendedFeaturePattern(obj.entries, obj.feature)) {
                elemOutput.serializeObj(this.files, obj.files, FnPatternList.Companion, 2)
            }
            elemOutput.endStructure(descriptor)
        }

        private fun <T : Any> CompositeEncoder.serializeObj(
            default: T?,
            actual: T?,
            saver: SerializationStrategy<T>,
            index: Int
        ) {
            if (default != actual && actual != null) {
                this.encodeSerializableElement(descriptor, index, saver, actual)
            }
        }
    }
}
