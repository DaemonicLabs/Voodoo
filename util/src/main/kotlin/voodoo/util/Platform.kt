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
        if (osName.startsWith("Linux")) {
            if ("dalvik" == System.getProperty("java.vm.name").toLowerCase()) {
                osType = OSType.ANDROID
                // Native libraries on android must be bundled with the APK
                System.setProperty("jna.nounpack", "true")
            } else {
                osType = OSType.LINUX
            }
        } else if (osName.startsWith("AIX")) {
            osType = OSType.AIX
        } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            osType = OSType.MAC
        } else if (osName.startsWith("Windows CE")) {
            osType = OSType.WINDOWSCE
        } else if (osName.startsWith("Windows")) {
            osType = OSType.WINDOWS
        } else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            osType = OSType.SOLARIS
        } else if (osName.startsWith("FreeBSD")) {
            osType = OSType.FREEBSD
        } else if (osName.startsWith("OpenBSD")) {
            osType = OSType.OPENBSD
        } else if (osName.equals("gnu", ignoreCase = true)) {
            osType = OSType.GNU
        } else if (osName.equals("gnu/kfreebsd", ignoreCase = true)) {
            osType = OSType.KFREEBSD
        } else if (osName.equals("netbsd", ignoreCase = true)) {
            osType = OSType.NETBSD
        } else {
            osType = OSType.UNSPECIFIED
        }
    }

    val isLinux = osType == OSType.LINUX
    val isSolaris = osType == OSType.SOLARIS
    val isAIX = osType == OSType.AIX
    val isMac = osType == OSType.MAC
    val isWindows = osType == OSType.WINDOWS || osType == OSType.WINDOWSCE
    val isX11 = !isWindows && !isMac
}

