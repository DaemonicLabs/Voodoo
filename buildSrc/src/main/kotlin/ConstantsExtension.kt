import java.io.Serializable

class ConstantsBuilder(val pkg: String, val className: String) : Serializable {
    var fields: Map<String, Any> = mapOf()
        private set

    fun field(name: String) = ConstantField(name)

    infix fun ConstantField.value(value: String) {
        fields += this.name to value
    }

    infix fun ConstantField.value(value: Int) {
        fields += this.name to value
    }
}

data class ConstantField (
    val name: String
)

open class ConstantsExtension {
    var files: List<ConstantsBuilder> = listOf()
        private set

    fun constantsObject(pkg: String, className: String, initConstants: ConstantsBuilder.() -> Unit) {
        val builder = ConstantsBuilder(pkg, className)
        builder.initConstants()
        files += builder
    }
}