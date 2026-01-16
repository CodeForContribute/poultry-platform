import SwiftUI

struct PriceView: View {
    let amount: Double
    var unit: String? = nil
    var style: PriceStyle = .regular

    var body: some View {
        HStack(spacing: 2) {
            Text("₹\(String(format: "%.2f", amount))")
                .font(style.font)
                .fontWeight(style.weight)
                .foregroundColor(style.color)

            if let unit = unit {
                Text("/\(unit.lowercased())")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}

enum PriceStyle {
    case regular
    case large
    case small

    var font: Font {
        switch self {
        case .regular: return .body
        case .large: return .title2
        case .small: return .caption
        }
    }

    var weight: Font.Weight {
        switch self {
        case .regular: return .semibold
        case .large: return .bold
        case .small: return .medium
        }
    }

    var color: Color {
        return .primary
    }
}

#Preview {
    VStack(spacing: 16) {
        PriceView(amount: 280.00, unit: "kg")
        PriceView(amount: 1500.00, style: .large)
        PriceView(amount: 50.00, style: .small)
    }
}
