import Foundation
import Security
import CommonCrypto

/// SSL Pinning Manager for secure network communication
///
/// This manager provides SSL certificate pinning to prevent man-in-the-middle attacks.
/// It supports both certificate pinning and public key pinning.
///
/// IMPORTANT: Update the certificate pins before deploying to production.
/// Use the following command to extract the public key hash:
///
/// ```bash
/// openssl s_client -servername api.poultry-platform.com -connect api.poultry-platform.com:443 | \
/// openssl x509 -pubkey -noout | \
/// openssl pkey -pubin -outform der | \
/// openssl dgst -sha256 -binary | \
/// openssl enc -base64
/// ```
final class SSLPinningManager: NSObject {

    // MARK: - Singleton

    static let shared = SSLPinningManager()

    // MARK: - Configuration

    /// Pinning mode options
    enum PinningMode {
        case certificate    // Pin the entire certificate
        case publicKey      // Pin only the public key (recommended for key rotation)
    }

    /// Current pinning mode
    private let pinningMode: PinningMode = .publicKey

    /// Whether SSL pinning is enabled
    var isPinningEnabled: Bool {
        #if DEBUG
        return false  // Disable pinning in debug for development flexibility
        #else
        return true
        #endif
    }

    // MARK: - Certificate Pins

    /// Public key pins for production API
    /// IMPORTANT: Replace these with your actual certificate public key hashes
    private let productionPins: Set<String> = [
        // Primary certificate pin (current production certificate)
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        // Backup certificate pin (next certificate for rotation)
        "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=",
        // Root CA pin (emergency fallback)
        "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=",
        // Let's Encrypt ISRG Root X1
        "C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=",
        // Let's Encrypt ISRG Root X2
        "diGVwiVYbubAI3RW4hB9xU8e/CH2GnkuvVFZE8zmgzI="
    ]

    /// Public key pins for staging API
    private let stagingPins: Set<String> = [
        "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD=",
        "EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE=",
        // Let's Encrypt roots
        "C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=",
        "diGVwiVYbubAI3RW4hB9xU8e/CH2GnkuvVFZE8zmgzI="
    ]

    /// Domains that require SSL pinning
    private let pinnedDomains: Set<String> = [
        "api.poultry-platform.com",
        "poultry-platform.com",
        "staging-api.poultry-platform.com"
    ]

    // MARK: - Initialization

    private override init() {
        super.init()
    }

    // MARK: - Public Methods

    /// Returns the appropriate pins for the given host
    func pins(for host: String) -> Set<String> {
        if host.contains("staging") {
            return stagingPins
        }
        return productionPins
    }

    /// Checks if the given host requires SSL pinning
    func requiresPinning(for host: String) -> Bool {
        guard isPinningEnabled else { return false }
        return pinnedDomains.contains { host.contains($0) }
    }

    /// Creates a URLSession configured with SSL pinning
    func createPinnedSession(configuration: URLSessionConfiguration = .default) -> URLSession {
        return URLSession(
            configuration: configuration,
            delegate: self,
            delegateQueue: nil
        )
    }

    // MARK: - Certificate Validation

    /// Validates a server trust against pinned certificates
    /// - Parameters:
    ///   - serverTrust: The server trust to validate
    ///   - host: The host being connected to
    /// - Returns: Whether the server trust is valid
    func validateServerTrust(_ serverTrust: SecTrust, for host: String) -> Bool {
        guard requiresPinning(for: host) else {
            // No pinning required, perform standard validation
            return performStandardValidation(serverTrust)
        }

        let expectedPins = pins(for: host)

        // Get certificate chain
        let certificateCount = SecTrustGetCertificateCount(serverTrust)
        guard certificateCount > 0 else { return false }

        // Validate each certificate in the chain
        for i in 0..<certificateCount {
            guard let certificate = SecTrustCopyCertificateChain(serverTrust)?[i] as? SecCertificate else {
                continue
            }

            let hash: String?

            switch pinningMode {
            case .certificate:
                hash = sha256Hash(of: certificate)
            case .publicKey:
                hash = publicKeyHash(of: certificate)
            }

            if let hash = hash, expectedPins.contains(hash) {
                // Found a matching pin, also verify standard trust
                return performStandardValidation(serverTrust)
            }
        }

        // No matching pin found
        return false
    }

    // MARK: - Private Methods

    /// Performs standard SSL validation
    private func performStandardValidation(_ serverTrust: SecTrust) -> Bool {
        var error: CFError?
        let isValid = SecTrustEvaluateWithError(serverTrust, &error)

        if !isValid {
            print("[SSLPinning] Standard validation failed: \(error?.localizedDescription ?? "Unknown error")")
        }

        return isValid
    }

    /// Computes SHA-256 hash of a certificate
    private func sha256Hash(of certificate: SecCertificate) -> String? {
        guard let data = SecCertificateCopyData(certificate) as Data? else {
            return nil
        }

        return sha256(data: data)
    }

    /// Extracts and hashes the public key from a certificate
    private func publicKeyHash(of certificate: SecCertificate) -> String? {
        guard let publicKey = SecCertificateCopyKey(certificate) else {
            return nil
        }

        var error: Unmanaged<CFError>?
        guard let publicKeyData = SecKeyCopyExternalRepresentation(publicKey, &error) as Data? else {
            return nil
        }

        // Add ASN.1 header for RSA public keys
        // This matches the format used by OpenSSL
        let rsa2048ASN1Header: [UInt8] = [
            0x30, 0x82, 0x01, 0x22, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86,
            0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00, 0x03, 0x82, 0x01, 0x0f, 0x00
        ]

        var keyWithHeader = Data(rsa2048ASN1Header)
        keyWithHeader.append(publicKeyData)

        return sha256(data: keyWithHeader)
    }

    /// Computes SHA-256 hash of data
    private func sha256(data: Data) -> String {
        var hash = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))

        data.withUnsafeBytes { buffer in
            _ = CC_SHA256(buffer.baseAddress, CC_LONG(buffer.count), &hash)
        }

        return Data(hash).base64EncodedString()
    }
}

// MARK: - URLSessionDelegate

extension SSLPinningManager: URLSessionDelegate {

    func urlSession(
        _ session: URLSession,
        didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void
    ) {
        handleChallenge(challenge, completionHandler: completionHandler)
    }
}

// MARK: - URLSessionTaskDelegate

extension SSLPinningManager: URLSessionTaskDelegate {

    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void
    ) {
        handleChallenge(challenge, completionHandler: completionHandler)
    }

    private func handleChallenge(
        _ challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void
    ) {
        guard challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust,
              let serverTrust = challenge.protectionSpace.serverTrust else {
            completionHandler(.performDefaultHandling, nil)
            return
        }

        let host = challenge.protectionSpace.host

        if validateServerTrust(serverTrust, for: host) {
            let credential = URLCredential(trust: serverTrust)
            completionHandler(.useCredential, credential)
        } else {
            print("[SSLPinning] Certificate pinning failed for host: \(host)")
            completionHandler(.cancelAuthenticationChallenge, nil)
        }
    }
}

// MARK: - SSL Pinning Error

enum SSLPinningError: Error, LocalizedError {
    case pinningFailed(host: String)
    case certificateNotFound
    case publicKeyExtractionFailed
    case invalidCertificateChain

    var errorDescription: String? {
        switch self {
        case .pinningFailed(let host):
            return "SSL certificate pinning failed for \(host). Possible man-in-the-middle attack detected."
        case .certificateNotFound:
            return "Server certificate not found in the trust chain."
        case .publicKeyExtractionFailed:
            return "Failed to extract public key from certificate."
        case .invalidCertificateChain:
            return "Invalid certificate chain received from server."
        }
    }
}
