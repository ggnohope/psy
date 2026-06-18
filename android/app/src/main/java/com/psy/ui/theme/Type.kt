package com.psy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.psy.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val quicksandFont = GoogleFont("Quicksand")

val Quicksand: FontFamily = FontFamily(
    Font(googleFont = quicksandFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = quicksandFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = quicksandFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = quicksandFont, fontProvider = provider, weight = FontWeight.ExtraBold),
)

val CandyTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
    titleMedium = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
)
