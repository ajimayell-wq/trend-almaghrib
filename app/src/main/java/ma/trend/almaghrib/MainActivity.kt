package ma.trend.almaghrib

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ma.trend.almaghrib.data.Repository
import ma.trend.almaghrib.ui.AnalystScreen
import ma.trend.almaghrib.ui.BottomBar
import ma.trend.almaghrib.ui.C
import ma.trend.almaghrib.ui.HomeScreen
import ma.trend.almaghrib.ui.MoreScreen
import ma.trend.almaghrib.ui.OpinionScreen
import ma.trend.almaghrib.ui.Tab
import ma.trend.almaghrib.ui.TrendTheme
import ma.trend.almaghrib.ui.TrendsScreen
import ma.trend.almaghrib.ui.TopScreen
import ma.trend.almaghrib.ui.VideosScreen
import java.time.Duration

class TrendApp : Application() {
    val repo by lazy { Repository(this) }
}

/** Écrans superposés aux onglets (pile de navigation simple). */
sealed interface Overlay {
    data class TopicDetail(val id: Int) : Overlay
    data object Analyst : Overlay
    data class Top(val tab: Int) : Overlay
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        val repo = (application as TrendApp).repo
        setContent {
            TrendTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Root(repo)
                }
            }
        }
    }
}

@Composable
private fun Root(repo: Repository) {
    val st by repo.state.collectAsState()
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var opinionTopic by rememberSaveable { mutableStateOf<Int?>(null) }
    val stack = remember { mutableStateListOf<Overlay>() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Chargement du cache, actualisation à l'ouverture, puis toutes les 30 minutes tant que l'application est visible.
    LaunchedEffect(Unit) {
        repo.load()
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                if (repo.isStale(Duration.ofMinutes(10))) repo.refresh()
                delay(30 * 60 * 1000L)
            }
        }
    }

    BackHandler(enabled = stack.isNotEmpty() || tab != Tab.HOME) {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) else tab = Tab.HOME
    }
    val openTopic: (Int) -> Unit = { stack.add(Overlay.TopicDetail(it)) }
    val openAnalyst: () -> Unit = { stack.add(Overlay.Analyst) }
    val refresh: () -> Unit = { scope.launch { repo.refresh() } }

    Box(Modifier.fillMaxSize().background(C.bg)) {
        when (val top = stack.lastOrNull()) {
            is Overlay.TopicDetail -> OpinionScreen(st, repo, top.id, onBack = { stack.removeAt(stack.lastIndex) }, onPick = { })
            Overlay.Analyst -> AnalystScreen(st, repo, onBack = { stack.removeAt(stack.lastIndex) }, onTopic = openTopic)
            is Overlay.Top -> TopScreen(st, top.tab, onBack = { stack.removeAt(stack.lastIndex) },
                onSettings = { stack.clear(); tab = Tab.MORE })
            null -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.HOME -> HomeScreen(st, refresh, openTopic, { tab = Tab.TRENDS }, openAnalyst, { tab = Tab.MORE }, { stack.add(Overlay.Top(it)) })
                        Tab.TRENDS -> TrendsScreen(st, openTopic, openAnalyst)
                        Tab.OPINION -> OpinionScreen(st, repo, opinionTopic, onBack = null, onPick = { opinionTopic = it })
                        Tab.VIDEOS -> VideosScreen(st) { tab = Tab.MORE }
                        Tab.MORE -> MoreScreen(st, repo)
                    }
                }
                BottomBar(tab) { tab = it }
            }
        }
    }
}
