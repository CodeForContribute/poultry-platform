import SwiftUI

struct PhoneInputView: View {
    @EnvironmentObject var authManager: AuthManager
    @State private var phoneNumber = ""
    @State private var showOTPView = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()

                // Logo and title
                VStack(spacing: 8) {
                    Image(systemName: "leaf.fill")
                        .font(.system(size: 60))
                        .foregroundColor(.green)

                    Text("Poultry Platform")
                        .font(.title)
                        .fontWeight(.bold)

                    Text("Fresh poultry products delivered to your doorstep")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }

                Spacer()

                // Phone input
                VStack(alignment: .leading, spacing: 8) {
                    Text("Enter your phone number")
                        .font(.headline)

                    HStack {
                        Text("+91")
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 14)
                            .background(Color(.systemGray6))
                            .cornerRadius(10)

                        TextField("Phone number", text: $phoneNumber)
                            .keyboardType(.phonePad)
                            .padding()
                            .background(Color(.systemGray6))
                            .cornerRadius(10)
                    }

                    if let error = errorMessage {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(.red)
                    }
                }
                .padding(.horizontal)

                // Continue button
                Button(action: requestOTP) {
                    if authManager.isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    } else {
                        Text("Continue")
                            .fontWeight(.semibold)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(isValidPhone ? Color.green : Color.gray)
                .foregroundColor(.white)
                .cornerRadius(12)
                .padding(.horizontal)
                .disabled(!isValidPhone || authManager.isLoading)

                Spacer()

                // Terms
                Text("By continuing, you agree to our Terms of Service and Privacy Policy")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding()
            }
            .navigationDestination(isPresented: $showOTPView) {
                OTPVerificationView(phoneNumber: phoneNumber)
            }
        }
    }

    private var isValidPhone: Bool {
        phoneNumber.count == 10 && phoneNumber.allSatisfy { $0.isNumber }
    }

    private func requestOTP() {
        errorMessage = nil
        Task {
            do {
                _ = try await authManager.requestOTP(phone: phoneNumber)
                await MainActor.run {
                    showOTPView = true
                }
            } catch {
                await MainActor.run {
                    errorMessage = "Failed to send OTP. Please try again."
                }
            }
        }
    }
}

#Preview {
    PhoneInputView()
        .environmentObject(AuthManager())
}
