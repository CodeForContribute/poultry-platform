import Foundation
import UIKit
import Security
import MachO

/// Security Manager for detecting potential security threats
///
/// This class provides comprehensive security checks including:
/// - Jailbreak detection
/// - Debugger detection
/// - Integrity checks
/// - Simulator detection
/// - Code injection detection
///
/// IMPORTANT: These checks are designed to make reverse engineering more difficult,
/// but determined attackers can still bypass them. Use these as part of a defense-in-depth
/// strategy, not as the sole security measure.
final class SecurityManager {

    // MARK: - Singleton

    static let shared = SecurityManager()

    // MARK: - Types

    /// Result of security checks
    struct SecurityStatus {
        let isSecure: Bool
        let isJailbroken: Bool
        let isDebuggerAttached: Bool
        let isSimulator: Bool
        let isTampered: Bool
        let hasCodeInjection: Bool
        let failureReasons: [String]

        static let secure = SecurityStatus(
            isSecure: true,
            isJailbroken: false,
            isDebuggerAttached: false,
            isSimulator: false,
            isTampered: false,
            hasCodeInjection: false,
            failureReasons: []
        )
    }

    // MARK: - Properties

    /// Whether to enforce security checks (disabled in DEBUG)
    var isEnforced: Bool {
        #if DEBUG
        return false
        #else
        return true
        #endif
    }

    // MARK: - Initialization

    private init() {}

    // MARK: - Public Methods

    /// Performs all security checks and returns a comprehensive status
    func performSecurityCheck() -> SecurityStatus {
        var failureReasons: [String] = []

        let isJailbroken = checkJailbreak()
        let isDebuggerAttached = checkDebugger()
        let isSimulator = checkSimulator()
        let isTampered = checkTamper()
        let hasCodeInjection = checkCodeInjection()

        if isJailbroken {
            failureReasons.append("jailbreak_detected")
        }
        if isDebuggerAttached {
            failureReasons.append("debugger_attached")
        }
        if isSimulator {
            failureReasons.append("simulator_detected")
        }
        if isTampered {
            failureReasons.append("app_tampered")
        }
        if hasCodeInjection {
            failureReasons.append("code_injection_detected")
        }

        let isSecure = failureReasons.isEmpty

        return SecurityStatus(
            isSecure: isSecure,
            isJailbroken: isJailbroken,
            isDebuggerAttached: isDebuggerAttached,
            isSimulator: isSimulator,
            isTampered: isTampered,
            hasCodeInjection: hasCodeInjection,
            failureReasons: failureReasons
        )
    }

    // MARK: - Jailbreak Detection

    /// Comprehensive jailbreak detection using multiple methods
    func checkJailbreak() -> Bool {
        guard isEnforced else { return false }

        return checkJailbreakFiles() ||
               checkJailbreakPaths() ||
               checkSandboxViolation() ||
               checkJailbreakURLSchemes() ||
               checkJailbreakDylibs() ||
               checkSymbolicLinks() ||
               checkWritableSystemPaths()
    }

    /// Checks for common jailbreak files
    private func checkJailbreakFiles() -> Bool {
        let jailbreakFiles = [
            "/Applications/Cydia.app",
            "/Applications/blackra1n.app",
            "/Applications/FakeCarrier.app",
            "/Applications/Icy.app",
            "/Applications/IntelliScreen.app",
            "/Applications/MxTube.app",
            "/Applications/RockApp.app",
            "/Applications/SBSettings.app",
            "/Applications/Snoop-itConfig.app",
            "/Applications/WinterBoard.app",
            "/Library/MobileSubstrate/MobileSubstrate.dylib",
            "/Library/MobileSubstrate/DynamicLibraries/LiveClock.plist",
            "/Library/MobileSubstrate/DynamicLibraries/Veency.plist",
            "/private/var/lib/apt",
            "/private/var/lib/cydia",
            "/private/var/mobile/Library/SBSettings/Themes",
            "/private/var/stash",
            "/System/Library/LaunchDaemons/com.ikey.bbot.plist",
            "/System/Library/LaunchDaemons/com.saurik.Cydia.Startup.plist",
            "/usr/libexec/sftp-server",
            "/usr/sbin/sshd",
            "/usr/bin/sshd",
            "/var/cache/apt",
            "/var/lib/apt",
            "/var/lib/cydia",
            "/etc/apt",
            "/bin/bash",
            "/bin/sh",
            "/usr/sbin/frida-server",
            "/usr/bin/cycript",
            "/usr/local/bin/cycript",
            "/usr/lib/libcycript.dylib",
            "/var/log/syslog"
        ]

        for path in jailbreakFiles {
            if FileManager.default.fileExists(atPath: path) {
                return true
            }
        }

        return false
    }

    /// Checks for common jailbreak paths accessible
    private func checkJailbreakPaths() -> Bool {
        let paths = [
            "/private/var/lib/apt/",
            "/Applications/Cydia.app",
            "/etc/apt",
            "/private/var/tmp/cydia.log",
            "/var/tmp/cydia.log"
        ]

        for path in paths {
            if canOpenPath(path) {
                return true
            }
        }

        return false
    }

    /// Checks if we can write outside our sandbox
    private func checkSandboxViolation() -> Bool {
        let testPath = "/private/jailbreak_test_\(UUID().uuidString)"

        do {
            try "test".write(toFile: testPath, atomically: true, encoding: .utf8)
            try FileManager.default.removeItem(atPath: testPath)
            return true // Should not be able to write here
        } catch {
            return false // Correctly denied
        }
    }

    /// Checks for jailbreak-related URL schemes
    private func checkJailbreakURLSchemes() -> Bool {
        let urlSchemes = [
            "cydia://package/com.example.package",
            "sileo://package/com.example.package",
            "zbra://packages/com.example.package",
            "filza://view"
        ]

        for scheme in urlSchemes {
            if let url = URL(string: scheme),
               UIApplication.shared.canOpenURL(url) {
                return true
            }
        }

        return false
    }

    /// Checks for suspicious dylibs
    private func checkJailbreakDylibs() -> Bool {
        let suspiciousDylibs = [
            "MobileSubstrate",
            "libcycript",
            "frida",
            "SSLKillSwitch",
            "TrustMe",
            "xCon",
            "Liberty",
            "shadow",
            "FlyJB"
        ]

        let imageCount = _dyld_image_count()

        for i in 0..<imageCount {
            if let imageName = _dyld_get_image_name(i) {
                let name = String(cString: imageName)

                for dylib in suspiciousDylibs {
                    if name.lowercased().contains(dylib.lowercased()) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /// Checks for symbolic links indicating jailbreak
    private func checkSymbolicLinks() -> Bool {
        let symLinks = [
            "/Applications",
            "/var/stash/Library/Ringtones",
            "/var/stash/Library/Wallpaper",
            "/var/stash/usr/include",
            "/var/stash/usr/libexec",
            "/var/stash/usr/share",
            "/Library/Ringtones",
            "/Library/Wallpaper",
            "/usr/arm-apple-darwin9",
            "/usr/include",
            "/usr/libexec",
            "/usr/share"
        ]

        for path in symLinks {
            do {
                let attributes = try FileManager.default.attributesOfItem(atPath: path)
                if attributes[.type] as? FileAttributeType == .typeSymbolicLink {
                    return true
                }
            } catch {
                // Path doesn't exist or is inaccessible, which is fine
            }
        }

        return false
    }

    /// Checks if system paths are writable
    private func checkWritableSystemPaths() -> Bool {
        let systemPaths = [
            "/",
            "/root/",
            "/private/",
            "/jb/"
        ]

        for path in systemPaths {
            if FileManager.default.isWritableFile(atPath: path) {
                return true
            }
        }

        return false
    }

    private func canOpenPath(_ path: String) -> Bool {
        if let pointer = fopen(path, "r") {
            fclose(pointer)
            return true
        }
        return false
    }

    // MARK: - Debugger Detection

    /// Detects if a debugger is attached
    func checkDebugger() -> Bool {
        guard isEnforced else { return false }

        return checkSysctl() || checkPTrace() || checkDenyAttach()
    }

    /// Uses sysctl to check for debugger
    private func checkSysctl() -> Bool {
        var info = kinfo_proc()
        var size = MemoryLayout.stride(ofValue: info)
        var mib: [Int32] = [CTL_KERN, KERN_PROC, KERN_PROC_PID, getpid()]

        let result = sysctl(&mib, UInt32(mib.count), &info, &size, nil, 0)

        if result != 0 {
            return false
        }

        return (info.kp_proc.p_flag & P_TRACED) != 0
    }

    /// Checks if ptrace would fail (indicating debugger)
    private func checkPTrace() -> Bool {
        // This is a passive check - we don't actually deny ptrace
        // as that can cause legitimate debuggers to fail
        return false
    }

    /// Uses PT_DENY_ATTACH to prevent debugging
    private func checkDenyAttach() -> Bool {
        // On release builds, we can call ptrace with PT_DENY_ATTACH
        // This will prevent debuggers from attaching
        // Note: This is commented out as it can cause issues with crash reporting
        // let PT_DENY_ATTACH: CInt = 31
        // ptrace(PT_DENY_ATTACH, 0, nil, 0)
        return false
    }

    // MARK: - Simulator Detection

    /// Detects if running in iOS Simulator
    func checkSimulator() -> Bool {
        guard isEnforced else { return false }

        #if targetEnvironment(simulator)
        return true
        #else
        // Additional runtime checks
        if ProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != nil {
            return true
        }

        // Check for simulator-specific hardware model
        var systemInfo = utsname()
        uname(&systemInfo)
        let machine = withUnsafePointer(to: &systemInfo.machine) {
            $0.withMemoryRebound(to: CChar.self, capacity: 1) {
                String(validatingUTF8: $0)
            }
        }

        if let machine = machine {
            return machine.contains("x86") || machine.contains("i386")
        }

        return false
        #endif
    }

    // MARK: - Tamper Detection

    /// Detects if the app has been tampered with
    func checkTamper() -> Bool {
        guard isEnforced else { return false }

        return checkBundleIntegrity() || checkExecutableIntegrity()
    }

    /// Checks bundle integrity
    private func checkBundleIntegrity() -> Bool {
        // Verify that _CodeSignature exists
        guard let bundlePath = Bundle.main.bundlePath as NSString? else {
            return true
        }

        let codeSignPath = bundlePath.appendingPathComponent("_CodeSignature")
        if !FileManager.default.fileExists(atPath: codeSignPath) {
            return true
        }

        // Verify Info.plist exists and is not modified
        guard let infoDictionary = Bundle.main.infoDictionary,
              let bundleIdentifier = infoDictionary["CFBundleIdentifier"] as? String else {
            return true
        }

        // Check that bundle ID matches expected (prevents repackaging)
        let expectedBundleID = "com.poultry.buyer"
        if !bundleIdentifier.hasPrefix(expectedBundleID) {
            return true
        }

        return false
    }

    /// Checks executable integrity
    private func checkExecutableIntegrity() -> Bool {
        // This would ideally check the hash of the executable
        // against a known good hash stored securely
        // Implementation depends on your build process
        return false
    }

    // MARK: - Code Injection Detection

    /// Detects common code injection frameworks
    func checkCodeInjection() -> Bool {
        guard isEnforced else { return false }

        return checkFrida() || checkCycript() || checkObjectionHooks()
    }

    /// Checks for Frida injection
    private func checkFrida() -> Bool {
        // Check for Frida server
        let fridaPaths = [
            "/usr/sbin/frida-server",
            "/usr/bin/frida-server",
            "/usr/local/bin/frida-server"
        ]

        for path in fridaPaths {
            if FileManager.default.fileExists(atPath: path) {
                return true
            }
        }

        // Check for Frida ports
        let fridaPorts = [27042, 27043]
        for port in fridaPorts {
            if isPortOpen(port) {
                return true
            }
        }

        // Check loaded libraries for Frida
        let imageCount = _dyld_image_count()
        for i in 0..<imageCount {
            if let imageName = _dyld_get_image_name(i) {
                let name = String(cString: imageName).lowercased()
                if name.contains("frida") || name.contains("gadget") {
                    return true
                }
            }
        }

        return false
    }

    /// Checks for Cycript
    private func checkCycript() -> Bool {
        // Check for Cycript port
        if isPortOpen(8556) {
            return true
        }

        // Check for Cycript dylib
        let imageCount = _dyld_image_count()
        for i in 0..<imageCount {
            if let imageName = _dyld_get_image_name(i) {
                let name = String(cString: imageName).lowercased()
                if name.contains("cycript") {
                    return true
                }
            }
        }

        return false
    }

    /// Checks for Objection hooks
    private func checkObjectionHooks() -> Bool {
        // Check for common Objection environment variables
        let objectionEnvVars = [
            "OBJECTION_LOG_LEVEL",
            "OBJECTION_HOOKS"
        ]

        for envVar in objectionEnvVars {
            if ProcessInfo.processInfo.environment[envVar] != nil {
                return true
            }
        }

        return false
    }

    /// Checks if a port is open
    private func isPortOpen(_ port: Int) -> Bool {
        let socketFD = socket(AF_INET, SOCK_STREAM, 0)
        guard socketFD != -1 else { return false }

        defer { close(socketFD) }

        var addr = sockaddr_in()
        addr.sin_family = sa_family_t(AF_INET)
        addr.sin_port = in_port_t(port).bigEndian
        addr.sin_addr.s_addr = inet_addr("127.0.0.1")

        var timeout = timeval()
        timeout.tv_sec = 1
        timeout.tv_usec = 0

        setsockopt(socketFD, SOL_SOCKET, SO_RCVTIMEO, &timeout, socklen_t(MemoryLayout.size(ofValue: timeout)))
        setsockopt(socketFD, SOL_SOCKET, SO_SNDTIMEO, &timeout, socklen_t(MemoryLayout.size(ofValue: timeout)))

        let result = withUnsafePointer(to: &addr) {
            $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                connect(socketFD, $0, socklen_t(MemoryLayout<sockaddr_in>.size))
            }
        }

        return result == 0
    }

    // MARK: - Additional Security Checks

    /// Checks if screen capture prevention should be enabled
    func shouldPreventScreenCapture() -> Bool {
        #if DEBUG
        return false
        #else
        return true
        #endif
    }

    /// Checks if the device has a passcode set
    func isDevicePasscodeSet() -> Bool {
        // Try to access a keychain item that requires device authentication
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "SecurityCheck",
            kSecAttrAccount as String: "PasscodeCheck",
            kSecAttrAccessible as String: kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
        ]

        var status = SecItemCopyMatching(query as CFDictionary, nil)

        if status == errSecItemNotFound {
            // Try to add the item
            var addQuery = query
            addQuery[kSecValueData as String] = "test".data(using: .utf8)
            status = SecItemAdd(addQuery as CFDictionary, nil)

            if status == errSecSuccess {
                // Clean up
                SecItemDelete(query as CFDictionary)
                return true
            }
        }

        return status != errSecNotAvailable
    }
}
