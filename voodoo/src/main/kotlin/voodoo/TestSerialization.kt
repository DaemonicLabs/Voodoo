package voodoo

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import voodoo.pack.FileEntry

@Serializable
sealed class Base(
    val baseProp: String = "default"
) {
    @Serializable
    @SerialName("test")
    class Test(
        val testProp: String = "testProp"
    ): Base() {
        override fun toString(): String {
            return super.toString()
        }
    }
}

//fun main(args: Array<String>) {
//    val decoded = Json.decodeFromString(Base.serializer(),
//        """
//            {
//                "type": "test",
//                "baseProp": "different value",
//                "testProp": "test value"
//            }
//        """.trimIndent()
//        )
//
//    require(decoded.baseProp == "different value") {
//        "decoding failed"
//    }
//}

fun main(args: Array<String>) {
    val decoded = Json.decodeFromString(FileEntry.serializer(),
        """
    {
      "type": "curse",
      "projectName": "Fabric16/tab-inventory-fabric",
      "validMcVersions": ["1.16", "1.16.1", "1.16.2", "1.16.3"]
    }
        """.trimIndent()
        ) as FileEntry.Curse

    require(decoded.validMcVersions.isNotEmpty()) {
        "decoding failed"
    }
}