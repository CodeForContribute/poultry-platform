package com.poultry.buyer.core.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security Manager for detecting potential security threats
 *
 * This class provides comprehensive security checks including:
 * - Root detection
 * - Emulator detection
 * - Debugger detection
 * - App tampering detection
 * - Frida/Xposed detection
 *
 * IMPORTANT: These checks are designed to make reverse engineering more difficult,
 * but determined attackers can still bypass them. Use these as part of a defense-in-depth
 * strategy, not as the sole security measure.
 */
@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Data class representing the result of security checks
     */
    data class SecurityStatus(
        val isSecure: Boolean,
        val isRooted: Boolean,
        val isEmulator: Boolean,
        val isDebuggerAttached: Boolean,
        val isTampered: Boolean,
        val hasHookingFramework: Boolean,
        val failureReason: String?
    )

    /**
     * Performs all security checks and returns a comprehensive status
     */
    fun performSecurityCheck(): SecurityStatus {
        val isRooted = checkRootAccess()
        val isEmulator = checkEmulator()
        val isDebuggerAttached = checkDebugger()
        val isTampered = checkTamper()
        val hasHookingFramework = checkHookingFrameworks()

        val failures = mutableListOf<String>()
        if (isRooted) failures.add("root_detected")
        if (isEmulator) failures.add("emulator_detected")
        if (isDebuggerAttached) failures.add("debugger_attached")
        if (isTampered) failures.add("app_tampered")
        if (hasHookingFramework) failures.add("hooking_framework_detected")

        val isSecure = failures.isEmpty()
        val failureReason = if (failures.isNotEmpty()) failures.joinToString(", ") else null

        return SecurityStatus(
            isSecure = isSecure,
            isRooted = isRooted,
            isEmulator = isEmulator,
            isDebuggerAttached = isDebuggerAttached,
            isTampered = isTampered,
            hasHookingFramework = hasHookingFramework,
            failureReason = failureReason
        )
    }

    // ========== Root Detection ==========

    /**
     * Comprehensive root detection using multiple methods
     */
    fun checkRootAccess(): Boolean {
        return checkRootBinaries() ||
                checkSuCommand() ||
                checkRootPackages() ||
                checkRootPaths() ||
                checkBuildTags() ||
                checkRootProps() ||
                checkRootCloaking()
    }

    private fun checkRootBinaries(): Boolean {
        val rootBinaries = listOf(
            "su", "busybox", "supersu", "Superuser.apk",
            "KingoUser.apk", "SuperSu.apk", "magisk"
        )

        val paths = listOf(
            "/system/bin/", "/system/xbin/", "/sbin/", "/system/sd/xbin/",
            "/system/bin/failsafe/", "/data/local/xbin/", "/data/local/bin/",
            "/data/local/", "/system/app/", "/system/usr/we-need-root/",
            "/cache/", "/data/", "/dev/"
        )

        for (path in paths) {
            for (binary in rootBinaries) {
                if (File(path + binary).exists()) {
                    return true
                }
            }
        }
        return false
    }

    private fun checkSuCommand(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine() != null
        } catch (e: Exception) {
            false
        }
    }

    private fun checkRootPackages(): Boolean {
        val rootPackages = listOf(
            "com.topjohnwu.magisk",
            "com.koushikdutta.superuser",
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.yellowes.su",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot"
        )

        val pm = context.packageManager
        for (packageName in rootPackages) {
            try {
                pm.getPackageInfo(packageName, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // Package not found, continue checking
            }
        }
        return false
    }

    private fun checkRootPaths(): Boolean {
        val rootPaths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/data/adb/magisk"
        )

        for (path in rootPaths) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }

    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootProps(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", "ro.debuggable"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            result == "1"
        } catch (e: Exception) {
            false
        }
    }

    private fun checkRootCloaking(): Boolean {
        // Check for root cloaking apps like RootCloak
        val cloakingPackages = listOf(
            "com.devadvance.rootcloak",
            "com.devadvance.rootcloakplus",
            "de.robv.android.xposed.installer",
            "com.saurik.substrate"
        )

        val pm = context.packageManager
        for (packageName in cloakingPackages) {
            try {
                pm.getPackageInfo(packageName, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // Package not found
            }
        }
        return false
    }

    // ========== Emulator Detection ==========

    /**
     * Comprehensive emulator detection
     */
    fun checkEmulator(): Boolean {
        return checkEmulatorBuild() ||
                checkEmulatorHardware() ||
                checkEmulatorFiles() ||
                checkEmulatorProps() ||
                checkQEMU()
    }

    private fun checkEmulatorBuild(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.BRAND.startsWith("generic") ||
                Build.DEVICE.startsWith("generic") ||
                Build.PRODUCT == "sdk" ||
                Build.PRODUCT == "google_sdk" ||
                Build.PRODUCT == "sdk_x86" ||
                Build.PRODUCT == "sdk_google" ||
                Build.PRODUCT == "vbox86p" ||
                Build.PRODUCT == "emulator" ||
                Build.PRODUCT == "simulator" ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.BOARD == "unknown" ||
                Build.ID == "FRF91" ||
                Build.MANUFACTURER == "unknown" ||
                Build.USER == "android-build")
    }

    private fun checkEmulatorHardware(): Boolean {
        return try {
            val cpuInfo = File("/proc/cpuinfo").readText()
            cpuInfo.contains("goldfish") || cpuInfo.contains("ranchu")
        } catch (e: Exception) {
            false
        }
    }

    private fun checkEmulatorFiles(): Boolean {
        val emulatorFiles = listOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props",
            "/dev/goldfish_pipe",
            "/init.goldfish.rc",
            "/sys/devices/virtual/misc/vbox",
            "/data/app/com.bluestacks.appmart",
            "/data/app/com.bluestacks.BstCommandProcessor",
            "/data/app/com.bluestacks.help",
            "/data/app/com.bluestacks.home",
            "/data/app/com.bluestacks.s2p",
            "/data/app/com.bluestacks.searchapp"
        )

        for (file in emulatorFiles) {
            if (File(file).exists()) {
                return true
            }
        }
        return false
    }

    private fun checkEmulatorProps(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", "ro.hardware"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val hardware = reader.readLine() ?: ""
            hardware.contains("goldfish") || hardware.contains("ranchu") || hardware.contains("vbox")
        } catch (e: Exception) {
            false
        }
    }

    private fun checkQEMU(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", "ro.kernel.qemu"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine() == "1"
        } catch (e: Exception) {
            false
        }
    }

    // ========== Debugger Detection ==========

    /**
     * Detects if a debugger is currently attached
     */
    fun checkDebugger(): Boolean {
        return Debug.isDebuggerConnected() ||
                Debug.waitingForDebugger() ||
                checkDebuggerFlag() ||
                checkTracerPid()
    }

    private fun checkDebuggerFlag(): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private fun checkTracerPid(): Boolean {
        return try {
            val statusFile = File("/proc/self/status")
            val lines = statusFile.readLines()
            for (line in lines) {
                if (line.startsWith("TracerPid:")) {
                    val pid = line.split(":")[1].trim().toIntOrNull() ?: 0
                    return pid != 0
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    // ========== Tamper Detection ==========

    /**
     * Detects if the app has been tampered with
     */
    fun checkTamper(): Boolean {
        return checkSignature() || checkInstaller()
    }

    private fun checkSignature(): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            // In production, compare signature hash with known good hash
            // This is a placeholder - replace with actual signature verification
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            // Return false for now - actual implementation should verify against
            // expected signature hash stored securely (e.g., in native code)
            signatures == null || signatures.isEmpty()
        } catch (e: Exception) {
            true // Consider tampered if we can't verify
        }
    }

    private fun checkInstaller(): Boolean {
        val validInstallers = listOf(
            "com.android.vending",  // Google Play Store
            "com.google.android.packageinstaller"
        )

        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }

            // In production, reject unknown installers
            // For development, allow any installer
            installer != null && !validInstallers.contains(installer)
        } catch (e: Exception) {
            false
        }
    }

    // ========== Hooking Framework Detection ==========

    /**
     * Detects common hooking frameworks like Frida, Xposed, etc.
     */
    fun checkHookingFrameworks(): Boolean {
        return checkFrida() || checkXposed() || checkSubstrate()
    }

    private fun checkFrida(): Boolean {
        // Check for Frida server process
        val fridaIndicators = listOf(
            "frida-server", "frida-gadget", "frida-agent",
            "libfrida-gadget.so", "libfrida-gadget-arm.so",
            "libfrida-gadget-arm64.so"
        )

        // Check running processes
        try {
            val process = Runtime.getRuntime().exec(arrayOf("ps"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                for (indicator in fridaIndicators) {
                    if (line?.contains(indicator) == true) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        // Check for Frida ports
        try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress("127.0.0.1", 27042), 1000)
            socket.close()
            return true // Frida default port is open
        } catch (e: Exception) {
            // Port not open, good
        }

        // Check loaded libraries
        try {
            val mapsFile = File("/proc/self/maps")
            val content = mapsFile.readText()
            for (indicator in fridaIndicators) {
                if (content.contains(indicator)) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        return false
    }

    private fun checkXposed(): Boolean {
        // Check for Xposed installer
        val xposedPackages = listOf(
            "de.robv.android.xposed.installer",
            "io.va.exposed",
            "org.meowcat.edxposed.manager",
            "com.topjohnwu.magisk" // Magisk can install LSPosed
        )

        val pm = context.packageManager
        for (packageName in xposedPackages) {
            try {
                pm.getPackageInfo(packageName, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // Package not found
            }
        }

        // Check for Xposed in stack traces
        try {
            throw Exception("XposedCheck")
        } catch (e: Exception) {
            val stackTrace = e.stackTrace
            for (element in stackTrace) {
                if (element.className.contains("de.robv.android.xposed") ||
                    element.className.contains("EdHooker") ||
                    element.className.contains("LSPosed")
                ) {
                    return true
                }
            }
        }

        // Check system properties
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getDeclaredMethod("get", String::class.java)
            val xposedVersion = method.invoke(null, "ro.xposed.version") as String?
            !xposedVersion.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun checkSubstrate(): Boolean {
        // Check for Cydia Substrate
        return try {
            Class.forName("com.saurik.substrate.MS")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    // ========== Additional Security Checks ==========

    /**
     * Checks if developer options are enabled
     */
    fun isDeveloperOptionsEnabled(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) != 0
    }

    /**
     * Checks if USB debugging is enabled
     */
    fun isUsbDebuggingEnabled(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED,
            0
        ) != 0
    }

    /**
     * Checks if installation from unknown sources is allowed
     */
    fun isUnknownSourcesEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.INSTALL_NON_MARKET_APPS,
                0
            ) != 0
        }
    }
}
