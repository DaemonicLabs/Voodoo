/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
/*      $OpenBSD: fnmatch.c,v 1.13 2006/03/31 05:34:14 deraadt Exp $        */

package com.skcraft.launcher.builder

import java.util.EnumSet

/*
 * Function fnmatch() as specified in POSIX 1003.2-1992, section B.6.
 * Compares a filename or pathname to a pattern.
 */
object FnMatch {
    private const val RANGE_ERROR = -1
    private const val RANGE_NOMATCH = 0

    enum class Flag {

        /** Disable backslash escaping.  */
        NOESCAPE,
        /** Slash must be matched by slash.  */
        PATHNAME,
        /** Period must be matched by period.  */
        PERIOD,
        /** Ignore /<tail> after Imatch. </tail> */
        LEADING_DIR,
        /** Case insensitive search.  */
        CASEFOLD
    }

    fun fnmatch(pattern: String, string: String, stringPos: Int, flag: Flag): Boolean {
        return match(pattern, 0, string, stringPos, EnumSet.of(flag))
    }

    fun fnmatch(pattern: String, string: String, stringPos: Int = 0): Boolean {
        return match(pattern, 0, string, stringPos, EnumSet.noneOf(Flag::class.java))
    }

    fun match(pattern: String, patternPos: Int = 0,
                      string: String, stringPos: Int = 0, flags: EnumSet<Flag>): Boolean {
        var patternPos = patternPos
        var stringPos = stringPos
        var flags = flags
        var c: Char

        loop@while (true) {
            if (patternPos >= pattern.length) {
                return if (flags.contains(Flag.LEADING_DIR) && string[stringPos] == '/') {
                    true
                } else stringPos == string.length
            }
            c = pattern[patternPos++]
            when (c) {
                '?' -> {
                    if (stringPos >= string.length) {
                        return false
                    }
                    if (string[stringPos] == '/' && flags.contains(Flag.PATHNAME)) {
                        return false
                    }
                    if (hasLeadingPeriod(string, stringPos, flags)) {
                        return false
                    }
                    ++stringPos
                }
                '*' -> {
                    /* Collapse multiple stars. */
                    while (patternPos < pattern.length && pattern[patternPos].let { c = it ; it == '*' }) {
                        patternPos++
                    }

                    if (hasLeadingPeriod(string, stringPos, flags)) {
                        return false
                    }

                    /* Optimize for pattern with * at end or before /. */
                    if (patternPos == pattern.length) {
                        return if (flags.contains(Flag.PATHNAME)) {
                            flags.contains(Flag.LEADING_DIR) || string.indexOf('/', stringPos) == -1
                        } else true
                    } else if (c == '/' && flags.contains(Flag.PATHNAME)) {
                        stringPos = string.indexOf('/', stringPos)
                        if (stringPos == -1) {
                            return false
                        }
                    }

                    /* General case, use recursion. */
                    while (stringPos < string.length) {
                        if (flags.contains(Flag.PERIOD)) {
                            flags = EnumSet.copyOf(flags)
                            flags.remove(Flag.PERIOD)
                        }
                        if (match(pattern, patternPos, string, stringPos, flags)) {
                            return true
                        }
                        if (string[stringPos] == '/' && flags.contains(Flag.PATHNAME)) {
                            break
                        }
                        ++stringPos
                    }
                    return false
                }

                '[' -> {
                    if (stringPos >= string.length) {
                        return false
                    }
                    if (string[stringPos] == '/' && flags.contains(Flag.PATHNAME)) {
                        return false
                    }
                    if (hasLeadingPeriod(string, stringPos, flags)) {
                        return false
                    }

                    val result = matchRange(pattern, patternPos, string[stringPos], flags)
                    if (result == RANGE_ERROR)
                    /* not a good range, treat as normal text */ {
                        break@loop
                    }

                    if (result == RANGE_NOMATCH) {
                        return false
                    }

                    patternPos = result
                    ++stringPos
                }

                '\\' -> if (!flags.contains(Flag.NOESCAPE)) {
                    c = if (patternPos >= pattern.length) {
                        '\\'
                    } else {
                        pattern[patternPos++]
                    }
                }
            }

            if (stringPos >= string.length) {
                return false
            }
            if (c != string[stringPos] && !(flags.contains(Flag.CASEFOLD) && Character.toLowerCase(c) == Character.toLowerCase(string[stringPos]))) {
                return false
            }
            ++stringPos
        }
        /* NOTREACHED */
        return false
    }

    private fun hasLeadingPeriod(string: String, stringPos: Int, flags: EnumSet<Flag>): Boolean {
        return if (stringPos > string.length - 1) false else (stringPos == 0 || flags.contains(Flag.PATHNAME) && string[stringPos - 1] == '/')
                && string[stringPos] == '.' && flags.contains(Flag.PERIOD)
    }

    private fun matchRange(pattern: String, patternPos: Int, test: Char, flags: EnumSet<Flag>): Int {
        var patternPos = patternPos
        var test = test
        val negate: Boolean
        var ok: Boolean
        var c: Char
        var c2: Char

        if (patternPos >= pattern.length) {
            return RANGE_ERROR
        }

        /*
         * A bracket expression starting with an unquoted circumflex
         * character produces unspecified results (IEEE 1003.2-1992,
         * 3.13.2).  This implementation treats it like '!', for
         * consistency with the regular expression syntax.
         * J.T. Conklin (conklin@ngai.kaleida.com)
         */
        c = pattern[patternPos]
        negate = c == '!' || c == '^'
        if (negate) {
            ++patternPos
        }

        if (flags.contains(Flag.CASEFOLD)) {
            test = Character.toLowerCase(test)
        }

        /*
         * A right bracket shall lose its special meaning and represent
         * itself in a bracket expression if it occurs first in the list.
         * -- POSIX.2 2.8.3.2
         */
        ok = false
        while (true) {
            if (patternPos >= pattern.length) {
                return RANGE_ERROR
            }

            c = pattern[patternPos++]
            if (c == ']') {
                break
            }

            if (c == '\\' && !flags.contains(Flag.NOESCAPE)) {
                c = pattern[patternPos++]
            }
            if (c == '/' && flags.contains(Flag.PATHNAME)) {
                return RANGE_NOMATCH
            }
            if (flags.contains(Flag.CASEFOLD)) {
                c = Character.toLowerCase(c)
            }
            if (pattern[patternPos] == '-' &&
                    patternPos + 1 < pattern.length && pattern[patternPos + 1] != ']') {
                c2 = pattern[patternPos + 1]
                patternPos += 2
                if (c2 == '\\' && !flags.contains(Flag.NOESCAPE)) {
                    if (patternPos >= pattern.length) {
                        return RANGE_ERROR
                    }
                    c = pattern[patternPos++]
                }
                if (flags.contains(Flag.CASEFOLD)) {
                    c2 = Character.toLowerCase(c2)
                }
                if (test in c..c2) {
                    ok = true
                }
            } else if (c == test) {
                ok = true
            }
        }

        return if (ok == negate) RANGE_NOMATCH else patternPos
    }
}