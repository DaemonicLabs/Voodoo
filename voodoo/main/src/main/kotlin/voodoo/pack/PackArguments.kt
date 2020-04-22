package voodoo.pack

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

class PackArguments(parser: ArgParser) {
    val method by parser.positional(
        "METHOD",
        help = "format to package into"
    ) { this.toLowerCase() }
        .default("")
}