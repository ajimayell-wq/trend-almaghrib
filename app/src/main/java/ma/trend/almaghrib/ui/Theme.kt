package ma.trend.almaghrib.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ma.trend.almaghrib.R

/** Palette « ترند المغرب » : noir profond, finitions dorées, rouge marocain pour les alertes. */
object C {
    val bg = Color(0xFF07070A)
    val bg2 = Color(0xFF0E0E13)
    val card = Color(0xFF15151C)
    val card2 = Color(0xFF1B1B24)
    val line = Color(0x12FFFFFF)
    val gold = Color(0xFFD4AF37)
    val gold2 = Color(0xFFF3D27A)
    val gold3 = Color(0xFF8A6A1C)
    val red = Color(0xFFE5383B)
    val red2 = Color(0xFFC1121F)
    val green = Color(0xFF2FD27A)
    val txt = Color(0xFFF5F1E8)
    val mut = Color(0xFF9A958A)
    val mut2 = Color(0xFF6D695F)
    val goldBrush = Brush.linearGradient(listOf(Color(0xFFFFF1B8), Color(0xFFF3D27A), Color(0xFFD4AF37), Color(0xFF9C7618)))
    val cardBrush = Brush.verticalGradient(listOf(card2, card))
}

val Tajawal = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold),
    Font(R.font.tajawal_extrabold, FontWeight.ExtraBold),
    Font(R.font.tajawal_black, FontWeight.Black),
)

private val base = TextStyle(fontFamily = Tajawal, color = C.txt)

private val typography = Typography(
    displaySmall = base.copy(fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = base.copy(fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = base.copy(fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, lineHeight = 26.sp),
    titleMedium = base.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = base.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = base.copy(fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = base.copy(fontSize = 13.5.sp, lineHeight = 21.sp),
    bodySmall = base.copy(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = base.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
    labelMedium = base.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
    labelSmall = base.copy(fontSize = 10.5.sp),
)

@Composable
fun TrendTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.gold, onPrimary = Color(0xFF1A1405), secondary = C.gold2, error = C.red,
            background = C.bg, onBackground = C.txt, surface = C.card, onSurface = C.txt,
            surfaceVariant = C.card2, onSurfaceVariant = C.mut, outline = C.line,
        ),
        typography = typography,
        content = content,
    )
}
