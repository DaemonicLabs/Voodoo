package voodoo.util.json

import io.ktor.client.call.TypeInfo
import io.ktor.client.features.json.JsonSerializer
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.JSON
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass

class TestKotlinxSerializer (
        private val json: JSON = JSON(
                unquoted = false,
                indented = false,
                nonstrict = false,
                context = SerialContext()
        ),
        block: SerialContext.() -> Unit = {}
) : JsonSerializer {
    init {
        json.context?.apply {
            block()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun serializerByTypeToken(type: java.lang.reflect.Type): KSerializer<Any> = when (type) {
        is Class<*> -> if (!type.isArray) {
            serializerByClass(type.kotlin)
        } else {
            val eType: Class<*> = type.componentType
            val s = kotlinx.serialization.serializerByTypeToken(eType)
            ReferenceArraySerializer<Any, Any>(eType.kotlin as KClass<Any>, s) as KSerializer<Any>
        }
        is ParameterizedType -> {
            val rootClass = (type.rawType as Class<*>)
            val args = type.actualTypeArguments.map { argument ->
                when (argument) {
                    is WildcardType -> argument.upperBounds.first() as Class<*>
                    else -> argument
                }
            }
            when {
                List::class.java.isAssignableFrom(rootClass) -> ArrayListSerializer(serializerByTypeToken(args[0])) as KSerializer<Any>
                Set::class.java.isAssignableFrom(rootClass) -> HashSetSerializer(serializerByTypeToken(args[0])) as KSerializer<Any>
                Map::class.java.isAssignableFrom(rootClass) -> HashMapSerializer(serializerByTypeToken(args[0]), serializerByTypeToken(args[1])) as KSerializer<Any>
                Map.Entry::class.java.isAssignableFrom(rootClass) -> MapEntrySerializer(serializerByTypeToken(args[0]), serializerByTypeToken(args[1])) as KSerializer<Any>

                else -> {
                    throw IllegalStateException("ParameterizedType '${type.rawType}' is not implemented")
                    // Cannot access 'invokeSerializerGetter': it is internal in 'kotlinx.serialization'
                    //                    val varargs = args.map { kotlinx.serialization.serializerByTypeToken(it) }.toTypedArray()
                    //                    (rootClass.invokeSerializerGetter(*varargs) as? KSerializer<Any>) ?: serializerByClass<Any>(rootClass.kotlin)
                }
            }
        }
        else -> throw IllegalArgumentException("type should be instance of Class<?> or ParametrizedType")
    }


    override fun write(data: Any): OutgoingContent {
        @Suppress("UNCHECKED_CAST")
        val clazz: KClass<Any> = data::class as KClass<Any>
        val serializer = clazz.serializer()
        return TextContent(json.stringify(serializer, data), ContentType.Application.Json)
    }

    override suspend fun read(type: TypeInfo, response: HttpResponse): Any {
        val serializer = serializerByTypeToken(type.reifiedType)
        return json.parse(serializer, response.readText())
    }
}