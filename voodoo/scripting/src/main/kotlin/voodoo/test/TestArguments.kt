package voodoo.test

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

class TestArguments(parser: ArgParser) {
    val method by parser.positional(
        "METHOD",
        help = "testing provider to use"
    ) { this.toLowerCase() }
        .default("")

    val clean by parser.flagging(
        "--clean", "-c",
        help = "clean output rootFolder before packaging"
    )
        .default(true)
}