import SwiftUI

struct QuantitySelectorView: View {
    @Binding var quantity: Double
    var minQuantity: Double = 1.0
    var maxQuantity: Double? = nil
    var unit: String = ""
    var step: Double = 1.0

    var body: some View {
        HStack(spacing: 12) {
            Button {
                let newQty = quantity - step
                if newQty >= minQuantity {
                    quantity = newQty
                }
            } label: {
                Image(systemName: "minus.circle.fill")
                    .font(.title2)
                    .foregroundColor(quantity <= minQuantity ? .gray : .blue)
            }
            .disabled(quantity <= minQuantity)

            VStack(spacing: 2) {
                Text(String(format: quantity == floor(quantity) ? "%.0f" : "%.1f", quantity))
                    .font(.headline)
                    .fontWeight(.semibold)
                    .frame(minWidth: 40)

                if !unit.isEmpty {
                    Text(unit.lowercased())
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }

            Button {
                let newQty = quantity + step
                if let max = maxQuantity {
                    if newQty <= max {
                        quantity = newQty
                    }
                } else {
                    quantity = newQty
                }
            } label: {
                Image(systemName: "plus.circle.fill")
                    .font(.title2)
                    .foregroundColor(maxQuantity != nil && quantity >= maxQuantity! ? .gray : .blue)
            }
            .disabled(maxQuantity != nil && quantity >= maxQuantity!)
        }
    }
}

#Preview {
    @Previewable @State var quantity: Double = 5.0

    VStack(spacing: 20) {
        QuantitySelectorView(quantity: $quantity, minQuantity: 1, maxQuantity: 100, unit: "kg")
        Text("Selected: \(quantity)")
    }
}
