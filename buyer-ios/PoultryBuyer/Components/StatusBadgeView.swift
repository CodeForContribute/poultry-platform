import SwiftUI

struct OrderStatusBadge: View {
    let status: OrderStatus

    var body: some View {
        Text(status.displayName)
            .font(.caption)
            .fontWeight(.medium)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(backgroundColor)
            .foregroundColor(textColor)
            .clipShape(RoundedRectangle(cornerRadius: 4))
    }

    private var backgroundColor: Color {
        switch status {
        case .draft:
            return Color.gray.opacity(0.2)
        case .placed:
            return Color.blue.opacity(0.2)
        case .sellerConfirmed:
            return Color.green.opacity(0.2)
        case .sellerRejected:
            return Color.red.opacity(0.2)
        case .dispatched:
            return Color.orange.opacity(0.2)
        case .delivered:
            return Color.green.opacity(0.2)
        case .cancelledByBuyer, .cancelledBySeller:
            return Color.red.opacity(0.2)
        case .paymentPending:
            return Color.yellow.opacity(0.2)
        }
    }

    private var textColor: Color {
        switch status {
        case .draft:
            return .gray
        case .placed:
            return .blue
        case .sellerConfirmed:
            return .green
        case .sellerRejected:
            return .red
        case .dispatched:
            return .orange
        case .delivered:
            return Color(red: 0.1, green: 0.4, blue: 0.1)
        case .cancelledByBuyer, .cancelledBySeller:
            return Color(red: 0.7, green: 0.1, blue: 0.1)
        case .paymentPending:
            return Color(red: 0.6, green: 0.5, blue: 0)
        }
    }
}

struct ProductStatusBadge: View {
    let status: ProductStatus

    var body: some View {
        Text(status.displayName)
            .font(.caption)
            .fontWeight(.medium)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(backgroundColor)
            .foregroundColor(textColor)
            .clipShape(RoundedRectangle(cornerRadius: 4))
    }

    private var backgroundColor: Color {
        switch status {
        case .active:
            return Color.green.opacity(0.2)
        case .inactive:
            return Color.gray.opacity(0.2)
        case .outOfStock:
            return Color.red.opacity(0.2)
        case .deleted:
            return Color.red.opacity(0.2)
        }
    }

    private var textColor: Color {
        switch status {
        case .active:
            return .green
        case .inactive:
            return .gray
        case .outOfStock:
            return .red
        case .deleted:
            return Color(red: 0.7, green: 0.1, blue: 0.1)
        }
    }
}

extension ProductStatus {
    var displayName: String {
        switch self {
        case .active: return "Active"
        case .inactive: return "Inactive"
        case .outOfStock: return "Out of Stock"
        case .deleted: return "Deleted"
        }
    }
}

#Preview {
    VStack(spacing: 8) {
        OrderStatusBadge(status: .placed)
        OrderStatusBadge(status: .sellerConfirmed)
        OrderStatusBadge(status: .dispatched)
        OrderStatusBadge(status: .delivered)
        OrderStatusBadge(status: .cancelledByBuyer)
    }
}
