//
//  LaunchScreenView.swift
//  PoultryBuyer
//
//  SwiftUI alternative to LaunchScreen.storyboard
//  Can be used as the app's initial view with animations
//

import SwiftUI

/// Launch screen view with brand colors and logo
/// This view mirrors the LaunchScreen.storyboard design but supports animations
struct LaunchScreenView: View {

    // MARK: - Properties

    @State private var isAnimating = false
    @State private var logoScale: CGFloat = 0.8
    @State private var logoOpacity: Double = 0
    @State private var textOpacity: Double = 0

    // MARK: - Body

    var body: some View {
        ZStack {
            // Background
            Color("BrandGreen")
                .ignoresSafeArea()

            VStack(spacing: 24) {
                Spacer()

                // Logo Placeholder
                // Replace with actual logo image when available
                logoView
                    .scaleEffect(logoScale)
                    .opacity(logoOpacity)

                // App Name
                Text("Poultry Buyer")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .opacity(textOpacity)

                // Tagline
                Text("Fresh Poultry, Direct to You")
                    .font(.subheadline)
                    .foregroundColor(.white.opacity(0.8))
                    .opacity(textOpacity)

                Spacer()

                // Loading Indicator
                if isAnimating {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(1.2)
                }

                Spacer()
                    .frame(height: 40)
            }
        }
        .onAppear {
            startAnimation()
        }
    }

    // MARK: - Subviews

    /// Logo placeholder view
    /// Replace with Image("LaunchLogo") when the actual logo is available
    private var logoView: some View {
        ZStack {
            // Circular background
            Circle()
                .fill(Color.white)
                .frame(width: 160, height: 160)

            // Chicken icon placeholder
            // Replace with actual logo/icon
            Image(systemName: "bird.fill")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 80, height: 80)
                .foregroundColor(Color("BrandOrange"))
        }
    }

    // MARK: - Animation

    /// Starts the entrance animation sequence
    private func startAnimation() {
        // Logo fade in and scale
        withAnimation(.easeOut(duration: 0.6)) {
            logoScale = 1.0
            logoOpacity = 1.0
        }

        // Text fade in (delayed)
        withAnimation(.easeOut(duration: 0.4).delay(0.3)) {
            textOpacity = 1.0
        }

        // Start loading indicator
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            isAnimating = true
        }
    }
}

// MARK: - Preview

#Preview {
    LaunchScreenView()
}

// MARK: - Launch Screen Coordinator

/// Coordinator to manage launch screen to main content transition
/// Use this when you want animated transitions from launch screen
@MainActor
final class LaunchScreenCoordinator: ObservableObject {

    @Published var isShowingLaunchScreen = true

    /// Minimum time to show launch screen (for branding)
    private let minimumDisplayTime: TimeInterval = 2.0

    /// Completes the launch screen animation and transitions to main content
    /// - Parameter completion: Called when transition is complete
    func completeLaunch(after delay: TimeInterval = 0) async {
        let totalDelay = max(minimumDisplayTime, delay)

        try? await Task.sleep(nanoseconds: UInt64(totalDelay * 1_000_000_000))

        withAnimation(.easeInOut(duration: 0.3)) {
            isShowingLaunchScreen = false
        }
    }
}
