package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Geometric Balance Palette
val GeoPrimary = Color(0xFF386B01)       // Vibrant Leaf Green / Primary
val GeoSecondary = Color(0xFFDDE6C7)     // Soft light green wash for container background
val GeoAccent = Color(0xFFBCF276)        // Sharp high contrast chartreuse lime
val GeoBackground = Color(0xFFF7F9F2)    // Light pristine organic background
val GeoSurface = Color(0xFFFFFFFF)       // Clean white for cards
val GeoTextDark = Color(0xFF1A1C18)      // Sleek near-black for absolute legibility
val GeoTextMuted = Color(0xFF43483E)     // Cool medium gray-green for body copy
val GeoBorder = Color(0xFFE2E4D8)        // Neutral light line divider/border green
val GeoBottomNav = Color(0xFFEFF2E6)     // Muted sand green for MD3 taskbars

// Old colors mapped forward gracefully to prevent compilation errors
val PrimaryGreen = GeoPrimary
val SecondaryGreen = GeoPrimary
val BrightAccentGreen = GeoAccent
val CreamBackground = GeoBackground
val CardSurfaceCream = GeoSurface
val TextDarkGreen = GeoTextDark
val LightMistGreen = GeoSecondary
val OrangeRustAccent = Color(0xFFD96B43)
val SoftSand = GeoBottomNav

// Status specific colors
val ThreatLightRed = Color(0xFFFDCFCE)
val ThreatDarkRed = Color(0xFF3E0007)

