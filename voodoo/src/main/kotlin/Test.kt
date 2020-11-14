import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

fun main() {
    val json = Json {
        prettyPrint = true
        classDiscriminator = "typeKey"
    }

    val data = TreeNode.One(
        data_one = DataOne("value"),
        children = listOf(
            TreeNode.Two(
                data_two = DataTwo("value2")
            )
        )
    )

    val jsonString = json.encodeToString(TreeNode.serializer(), data)
    println(jsonString)

    val decoded_data = json.decodeFromString(TreeNode.serializer(), jsonString)
    println(decoded_data)
}

@Serializable
data class DataOne(
    val field: String
)
@Serializable
data class DataTwo(
    val field2: String
)

@Serializable
sealed class TreeNode {
    abstract val children: List<TreeNode>

    @Serializable
    @SerialName("data_one")
    data class One(
        val data_one: DataOne,
        override val children: List<TreeNode> = listOf()
    ): TreeNode()

    @Serializable
    @SerialName("data_two")
    data class Two(
        val data_two: DataTwo,
        override val children: List<TreeNode> = listOf()
    ): TreeNode()

    @Serializer(forClass = TreeNode::class)
    companion object : KSerializer<TreeNode> {
        override fun deserialize(decoder: Decoder): TreeNode {
            require(decoder is JsonDecoder)
            val element = decoder.decodeJsonElement().jsonObject
            return when {
                element.keys.contains("data_one") -> {
                    One(
                        data_one = decoder.json.decodeFromJsonElement(DataOne.serializer(), element["data_one"]!!),
                        children = element["data_two"]?.let {
                            decoder.json.decodeFromJsonElement(ListSerializer(TreeNode.Companion), it)
                        } ?: listOf()
                    )
                }
                element.keys.contains("data_two") -> {
                    Two(
                        data_two = decoder.json.decodeFromJsonElement(DataTwo.serializer(), element["data_two"]!!),
                        children = element["data_two"]?.let {
                            decoder.json.decodeFromJsonElement(ListSerializer(TreeNode.Companion), it)
                        } ?: listOf()
                    )

                }
                else -> error("required keys were missing")
            }
        }

        override fun serialize(encoder: Encoder, value: TreeNode) {
            require(encoder is JsonEncoder)
            val jsonObject = when (value) {
                is TreeNode.One -> {
                    encoder.json.encodeToJsonElement(One.serializer(), value)
                }
                is TreeNode.Two -> {
                    encoder.json.encodeToJsonElement(Two.serializer(), value)
                }
            }
            encoder.encodeJsonElement(jsonObject)
        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TreeNode")
    }
}