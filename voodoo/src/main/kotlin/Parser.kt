import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.lexer.*

fun parse(content: String) {
    val keyword = regexToken("\\b\\w+\\b")
    val colon = literalToken(":")
    val eq = literalToken("=")


}