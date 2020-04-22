package voodoo.builder

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

class BuilderArguments(parser: ArgParser) {
    val noUpdate by parser.flagging(
        "--noUpdate",
        help = "do not update entries"
    )
        .default(false)

    val entries by parser.adding(
        "-E", help = "select specific entries to update"
    )
}