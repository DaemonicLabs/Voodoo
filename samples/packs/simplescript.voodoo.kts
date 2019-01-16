import com.skcraft.launcher.model.modpack.Recommendation
import voodoo.data.Side
import voodoo.data.UserFiles
import voodoo.data.curse.FileType

class Hello(val content: String = "World") {
    override fun toString(): String {
        return "Hello $content"
    }
}

val hello = Hello("World")

println(hello)

