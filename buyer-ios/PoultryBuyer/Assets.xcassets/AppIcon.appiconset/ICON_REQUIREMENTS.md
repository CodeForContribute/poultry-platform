# Poultry Buyer - iOS App Icon Requirements

This document describes the required app icon sizes for iOS App Store submission.

## Required Icon Files

| Filename | Size (pixels) | Purpose | Device |
|----------|---------------|---------|--------|
| Icon-App-1024x1024@1x.png | 1024x1024 | App Store | All |
| Icon-App-60x60@2x.png | 120x120 | App Icon | iPhone @2x |
| Icon-App-60x60@3x.png | 180x180 | App Icon | iPhone @3x |
| Icon-App-76x76@1x.png | 76x76 | App Icon | iPad @1x |
| Icon-App-76x76@2x.png | 152x152 | App Icon | iPad @2x |
| Icon-App-83.5x83.5@2x.png | 167x167 | App Icon | iPad Pro |
| Icon-App-40x40@2x.png | 80x80 | Spotlight | iPhone @2x |
| Icon-App-40x40@3x.png | 120x120 | Spotlight | iPhone @3x |
| Icon-App-40x40@1x.png | 40x40 | Spotlight | iPad @1x |
| Icon-App-40x40@2x-ipad.png | 80x80 | Spotlight | iPad @2x |
| Icon-App-29x29@2x.png | 58x58 | Settings | iPhone @2x |
| Icon-App-29x29@3x.png | 87x87 | Settings | iPhone @3x |
| Icon-App-29x29@1x.png | 29x29 | Settings | iPad @1x |
| Icon-App-29x29@2x-ipad.png | 58x58 | Settings | iPad @2x |
| Icon-App-20x20@2x.png | 40x40 | Notification | iPhone @2x |
| Icon-App-20x20@3x.png | 60x60 | Notification | iPhone @3x |
| Icon-App-20x20@1x.png | 20x20 | Notification | iPad @1x |
| Icon-App-20x20@2x-ipad.png | 40x40 | Notification | iPad @2x |

## Design Guidelines

### Brand Colors
- Primary Green: #4CAF50
- Accent Orange: #FF6B35
- Background: White (#FFFFFF)

### Icon Design
- Feature a stylized chicken/poultry silhouette
- Use the brand green as the primary background
- Keep the design simple and recognizable at small sizes
- Avoid text in the icon (not readable at small sizes)
- Use appropriate padding (Apple recommends ~10% margin)

### Technical Requirements
- Format: PNG
- Color space: sRGB
- Bit depth: 24-bit color (no alpha for App Store icon)
- No rounded corners (iOS applies them automatically)
- No transparency for the App Store icon
- Other icons can have transparency

### Generation Tools
1. **Xcode Asset Catalog**: Use Xcode's built-in icon generator
2. **App Icon Generator**: https://appicon.co/
3. **Figma**: Export with iOS App Icon template
4. **Sketch**: Use built-in iOS App Icon export

## Validation
After adding icons, validate in Xcode:
1. Open Assets.xcassets
2. Select AppIcon
3. Ensure no yellow warnings appear
4. Build and run to verify icons appear correctly

## App Store Submission
The 1024x1024 icon is used for:
- App Store listing
- Search results
- Featured placements

Ensure this icon is:
- High quality and crisp
- Representative of the app brand
- Without transparency or alpha channel
