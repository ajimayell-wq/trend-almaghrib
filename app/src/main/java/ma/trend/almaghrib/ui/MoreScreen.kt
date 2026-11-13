package ma.trend.almaghrib.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ma.trend.almaghrib.data.AppState
import ma.trend.almaghrib.data.Repository

/** Raison lisible d'une panne de source, à partir du message technique enregistré. */
private fun stopReason(e: String): String = when {
    e.isBlank() -> "سبب غير معروف"
    e.contains("timeout", ignoreCase = true) -> "انتهت المهلة"
    e.startsWith("HTTP 401") || e.startsWith("HTTP 403") -> "الموقع يرفض الوصول"
    e.startsWith("HTTP 404") || e.startsWith("HTTP 410") -> "الرابط لم يعد موجوداً"
    e.startsWith("HTTP 429") -> "طلبات كثيرة، أعد المحاولة لاحقاً"
    e.startsWith("HTTP 5") -> "الموقع معطل حالياً"
    e.contains("resolve host", ignoreCase = true) || e.contains("connect", ignoreCase = true) -> "تعذر الاتصال بالموقع"
    else -> "الرد ليس خلاصة RSS صالحة"
}

@Composable
fun MoreScreen(st: AppState, repo: Repository) {
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    var key by remember { mutableStateOf(repo.youtubeKey) }
    var msg by remember { mutableStateOf<Pair<String, Color>?>(null) }
    var testing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("المزيد", style = MaterialTheme.typography.headlineSmall)

        SectionHeader("مفتاح YouTube (اختياري)")
        NCard {
            Text(
                "بدون مفتاح: يعرض التطبيق فيديوهات القنوات المغربية ومشاهداتها. مع مفتاح مجاني من Google: قائمة الفيديوهات الرائجة في المغرب، عدد التعليقات، وتحليل تعليقات يوتيوب في «آراء المغاربة».",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD9D4C8)),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = key, onValueChange = { key = it; msg = null }, singleLine = true,
                placeholder = { Text("AIza…") },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.gold2, unfocusedBorderColor = C.line, cursorColor = C.gold2),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !testing,
                    onClick = {
                        scope.launch {
                            if (key.isBlank()) { repo.youtubeKey = ""; msg = "تم حذف المفتاح." to C.mut; return@launch }
                            testing = true
                            val ok = repo.testYoutubeKey(key.trim())
                            testing = false
                            if (ok) { repo.youtubeKey = key.trim(); msg = "المفتاح يعمل وتم حفظه. حدّث من الرئيسية." to C.green }
                            else msg = "المفتاح غير صالح أو أن YouTube Data API v3 غير مفعّلة." to C.red
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = C.gold, contentColor = Color(0xFF1A1405)),
                ) { Text(if (testing) "جارٍ التحقق…" else "تحقق واحفظ") }
                OutlinedButton(onClick = { uri.openUri("https://console.cloud.google.com/apis/library/youtube.googleapis.com") }) {
                    Text("الحصول على مفتاح", color = C.gold2)
                }
            }
            msg?.let { (m, c) -> Spacer(Modifier.height(6.dp)); Text(m, style = MaterialTheme.typography.labelMedium.copy(color = c)) }
            Spacer(Modifier.height(6.dp))
            Text(
                "المفتاح يبقى على هاتفك فقط ويُستعمل مباشرة مع Google. الحصة المجانية (10,000 وحدة/يوم) تكفي للاستعمال الشخصي.",
                style = MaterialTheme.typography.labelSmall.copy(color = C.mut),
            )
        }

        SectionHeader("حالة المصادر", trailing = st.lastRefresh?.let { "آخر تحديث ${Fmt.dateTime(it)}" })
        NCard {
            if (st.sources.isEmpty()) Text("لم يتم أي تحديث بعد.", style = MaterialTheme.typography.bodySmall.copy(color = C.mut))
            st.sources.sortedWith(compareBy({ it.platform }, { it.name })).forEach { s ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (s.ok) C.green else C.red))
                    Spacer(Modifier.width(8.dp))
                    Text(s.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(
                        if (s.ok) "${s.count} منشور" else "متوقف · ${stopReason(s.error)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = if (s.ok) C.mut else C.red),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            listOf("فيسبوك", "إنستغرام", "تيك توك", "X").forEach { p ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(C.mut2))
                    Spacer(Modifier.width(8.dp))
                    Text(p, style = MaterialTheme.typography.bodySmall.copy(color = C.mut), modifier = Modifier.weight(1f))
                    Text(if (p == "X") "يتطلب اشتراكاً مدفوعاً" else "لا وصول قانوني للتطبيقات", style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
                }
            }
        }

        SectionHeader("كيف يعمل التطبيق؟")
        NCard {
            listOf(
                "يتصل هاتفك مباشرة بالمصادر العامة (صحف مغربية، Google Trends، أخبار Google، قنوات يوتيوب، Reddit) دون أي خادم وسيط.",
                "تُجمَع المنشورات التي تتحدث عن الحدث نفسه بالعربية والفرنسية والدارجة، وتُحذف المكررات.",
                "الترتيب شفاف: عدد المصادر المختلفة، عدد المنشورات، تنوع المنصات، عمليات البحث على Google، مشاهدات يوتيوب، مع مراعاة الحداثة والسرعة.",
                "مؤشر التقدم = تغيّر عدد المنشورات بين آخر 6 ساعات والـ 6 ساعات السابقة.",
                "تحليل الآراء = تصنيف آلي تقريبي لتعليقات عامة متاحة فعلاً، مع ذكر حجم العينة دائماً.",
                "لا تُعرض أي أرقام مخترعة. الملخصات مقتطفات من المصادر وليست نصوصاً مولّدة.",
                "تُحفظ البيانات على هاتفك فقط لمدة 48 ساعة.",
            ).forEach {
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("•", color = C.gold2)
                    Spacer(Modifier.width(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD9D4C8), lineHeight = 19.sp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("ترند المغرب · الإصدار 1.4", style = MaterialTheme.typography.labelSmall.copy(color = C.mut2), modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(20.dp))
    }
}
