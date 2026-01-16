import Foundation

/// Represents a saved delivery address
struct SavedAddress: Codable, Identifiable, Equatable {
    let id: String
    var label: String?
    var line1: String
    var line2: String?
    var city: String
    var state: String
    var pincode: String
    var landmark: String?
    var latitude: Double?
    var longitude: Double?
    var contactName: String
    var contactPhone: String
    var isDefault: Bool

    /// User-friendly display label
    var displayLabel: String {
        label ?? "Address"
    }

    /// Full address formatted for display
    var formattedAddress: String {
        var parts: [String] = []
        parts.append(line1)
        if let line2 = line2, !line2.isEmpty {
            parts.append(line2)
        }
        if let landmark = landmark, !landmark.isEmpty {
            parts.append("Near \(landmark)")
        }
        parts.append("\(city), \(state) - \(pincode)")
        return parts.joined(separator: ", ")
    }

    /// Short address for list display
    var shortAddress: String {
        "\(line1), \(city) - \(pincode)"
    }

    /// Formatted contact details
    var contactDetails: String {
        "\(contactName) - \(formattedPhone)"
    }

    /// Formatted phone number
    var formattedPhone: String {
        if contactPhone.count == 10 {
            let areaCode = contactPhone.prefix(5)
            let remaining = contactPhone.suffix(5)
            return "\(areaCode) \(remaining)"
        }
        return contactPhone
    }

    static func == (lhs: SavedAddress, rhs: SavedAddress) -> Bool {
        lhs.id == rhs.id
    }
}

/// Request model for creating or updating an address
struct AddressRequest: Encodable {
    let label: String?
    let line1: String
    let line2: String?
    let city: String
    let state: String
    let pincode: String
    let landmark: String?
    let latitude: Double?
    let longitude: Double?
    let contactName: String
    let contactPhone: String
    let isDefault: Bool

    init(from address: SavedAddress) {
        self.label = address.label
        self.line1 = address.line1
        self.line2 = address.line2
        self.city = address.city
        self.state = address.state
        self.pincode = address.pincode
        self.landmark = address.landmark
        self.latitude = address.latitude
        self.longitude = address.longitude
        self.contactName = address.contactName
        self.contactPhone = address.contactPhone
        self.isDefault = address.isDefault
    }

    init(
        label: String? = nil,
        line1: String,
        line2: String? = nil,
        city: String,
        state: String,
        pincode: String,
        landmark: String? = nil,
        latitude: Double? = nil,
        longitude: Double? = nil,
        contactName: String,
        contactPhone: String,
        isDefault: Bool = false
    ) {
        self.label = label
        self.line1 = line1
        self.line2 = line2
        self.city = city
        self.state = state
        self.pincode = pincode
        self.landmark = landmark
        self.latitude = latitude
        self.longitude = longitude
        self.contactName = contactName
        self.contactPhone = contactPhone
        self.isDefault = isDefault
    }
}

/// Common Indian states for picker
enum IndianState: String, CaseIterable {
    case andhraPradesh = "Andhra Pradesh"
    case arunachalPradesh = "Arunachal Pradesh"
    case assam = "Assam"
    case bihar = "Bihar"
    case chhattisgarh = "Chhattisgarh"
    case goa = "Goa"
    case gujarat = "Gujarat"
    case haryana = "Haryana"
    case himachalPradesh = "Himachal Pradesh"
    case jharkhand = "Jharkhand"
    case karnataka = "Karnataka"
    case kerala = "Kerala"
    case madhyaPradesh = "Madhya Pradesh"
    case maharashtra = "Maharashtra"
    case manipur = "Manipur"
    case meghalaya = "Meghalaya"
    case mizoram = "Mizoram"
    case nagaland = "Nagaland"
    case odisha = "Odisha"
    case punjab = "Punjab"
    case rajasthan = "Rajasthan"
    case sikkim = "Sikkim"
    case tamilNadu = "Tamil Nadu"
    case telangana = "Telangana"
    case tripura = "Tripura"
    case uttarPradesh = "Uttar Pradesh"
    case uttarakhand = "Uttarakhand"
    case westBengal = "West Bengal"
    case delhi = "Delhi"
}
