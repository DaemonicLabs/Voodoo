package voodoo.util

enum class OSType {
    UNSPECIFIED,
    MAC,
    LINUX,
    WINDOWS,
    SOLARIS,
    FREEBSD,
    OPENBSD,
    WINDOWSCE,
    AIX,
    ANDROID,
    GNU,
    KFREEBSD,
    NETBSD
}

object Platform {
    val osType: OSType

    init {
        val osName = System.getProperty("os.name")
        osType = when {
            osName.startsWith("Linux") -> {
                when (System.getProperty("java.vm.name").toLowerCase()) {
                    "dalvik" -> {
                        // Native libraries on android must be bundled with the APK
                        System.setProperty("jna.nounpack", "true")
                        OSType.ANDROID
                    }
                    else -> OSType.LINUX
                }
            }
            osName.startsWith("AIX") -> OSType.AIX
            osName.startsWith("Mac") || osName.startsWith("Darwin") -> OSType.MAC
            osName.startsWith("Windows CE") -> OSType.WINDOWSCE
            osName.startsWith("Windows") -> OSType.WINDOWS
            osName.startsWith("Solaris") || osName.startsWith("SunOS") -> OSType.SOLARIS
            osName.startsWith("FreeBSD") -> OSType.FREEBSD
            osName.startsWith("OpenBSD") -> OSType.OPENBSD
            osName.equals("gnu", ignoreCase = true) -> OSType.GNU
            osName.equals("gnu/kfreebsd", ignoreCase = true) -> OSType.KFREEBSD
            osName.equals("netbsd", ignoreCase = true) -> OSType.NETBSD
            else -> OSType.UNSPECIFIED
        }
    }

    val isLinux = osType == OSType.LINUX
    val isSolaris = osType == OSType.SOLARIS
    val isAIX = osType == OSType.AIX
    val isMac = osType == OSType.MAC
    val isWindows = osType == OSType.WINDOWS || osType == OSType.WINDOWSCE
    val isX11 = !isWindows && !isMac
}
