import SwiftUI

struct OTPVerificationView: View {
    @EnvironmentObject var authManager: AuthManager
    @Environment(\.dismiss) var dismiss

    let phoneNumber: String
    @State private var otp = ""
    @State private var errorMessage: String?
    @State private var resendTimer = 30
    @State private var canResend = false

    var body: some View {
        VStack(spacing: 24) {
            // Header
            VStack(spacing: 8) {
                Text("Verify OTP")
                    .font(.title)
                    .fontWeight(.bold)

                Text("Enter the 6-digit code sent to")
                    .foregroundColor(.secondary)

                Text("+91 \(phoneNumber)")
                    .fontWeight(.semibold)
            }
            .padding(.top, 40)

            // OTP Input
            HStack(spacing: 12) {
                ForEach(0..<6, id: \.self) { index in
                    OTPDigitView(
                        digit: index < otp.count ? String(otp[otp.index(otp.startIndex, offsetBy: index)]) : ""
                    )
                }
            }
            .overlay(
                TextField("", text: $otp)
                    .keyboardType(.numberPad)
                    .textContentType(.oneTimeCode)
                    .frame(maxWidth: .infinity)
                    .opacity(0.01)
                    .onChange(of: otp) { _, newValue in
                        if newValue.count > 6 {
                            otp = String(newValue.prefix(6))
                        }
                        if newValue.count == 6 {
                            verifyOTP()
                        }
                    }
            )
            .padding(.horizontal)

            if let error = errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
            }

            // Resend button
            Button(action: resendOTP) {
                if canResend {
                    Text("Resend OTP")
                        .foregroundColor(.green)
                } else {
                    Text("Resend in \(resendTimer)s")
                        .foregroundColor(.secondary)
                }
            }
            .disabled(!canResend || authManager.isLoading)

            Spacer()

            // Verify button
            Button(action: verifyOTP) {
                if authManager.isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Text("Verify")
                        .fontWeight(.semibold)
                }
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(otp.count == 6 ? Color.green : Color.gray)
            .foregroundColor(.white)
            .cornerRadius(12)
            .padding(.horizontal)
            .disabled(otp.count != 6 || authManager.isLoading)
        }
        .navigationBarTitleDisplayMode(.inline)
        .onAppear(perform: startTimer)
    }

    private func startTimer() {
        Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { timer in
            if resendTimer > 0 {
                resendTimer -= 1
            } else {
                canResend = true
                timer.invalidate()
            }
        }
    }

    private func verifyOTP() {
        errorMessage = nil
        Task {
            do {
                try await authManager.verifyOTP(phone: phoneNumber, otp: otp)
            } catch {
                await MainActor.run {
                    errorMessage = "Invalid OTP. Please try again."
                    otp = ""
                }
            }
        }
    }

    private func resendOTP() {
        errorMessage = nil
        canResend = false
        resendTimer = 30
        startTimer()

        Task {
            do {
                _ = try await authManager.requestOTP(phone: phoneNumber)
            } catch {
                await MainActor.run {
                    errorMessage = "Failed to resend OTP."
                }
            }
        }
    }
}

struct OTPDigitView: View {
    let digit: String

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .stroke(digit.isEmpty ? Color.gray.opacity(0.3) : Color.green, lineWidth: 2)
                .frame(width: 45, height: 55)

            Text(digit)
                .font(.title)
                .fontWeight(.bold)
        }
    }
}

#Preview {
    NavigationStack {
        OTPVerificationView(phoneNumber: "9876543210")
            .environmentObject(AuthManager())
    }
}
