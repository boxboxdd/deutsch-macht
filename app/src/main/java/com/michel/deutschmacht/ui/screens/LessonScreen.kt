package com.michel.deutschmacht.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.michel.deutschmacht.data.Lessons
import com.michel.deutschmacht.data.ProgressStore
import com.michel.deutschmacht.speech.Speaker
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(nav: NavController, num: Int) {
    val context = LocalContext.current
    val lesson = remember(num) { Lessons.lesson(context, num) }
    val store = remember { ProgressStore(context) }
    var revision by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("درس $num") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    TextButton(onClick = { nav.navigate("quiz/$num") }) { Text("آزمون") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate("quiz/$num") },
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                text = { Text("آزمون") },
                containerColor = MaterialTheme.colorScheme.secondary
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(lesson.entries, key = { i, _ -> i }) { i, e ->
                EntryRow(
                    entry = e,
                    learned = store.isLearned(num, i),
                    onToggle = {
                        store.toggleLearned(num, i, !store.isLearned(num, i))
                        revision++
                    }
                )
            }
        }
    }
}

@Composable
private fun EntryRow(entry: com.michel.deutschmacht.data.Entry, learned: Boolean, onToggle: () -> Unit) {
    val bg by animateColorAsState(
        if (learned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "bg"
    )
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bg)
    ) {
        Row(Modifier.padding(12.dp, 10.dp), verticalAlignment = Alignment.Top) {
            IconButton(onClick = { Speaker.speak(entry.de) }, modifier = Modifier.size(36.dp)) {
                Text("🔊", fontSize = 18.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    entry.de, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    entry.fa, fontSize = 15.sp, lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (learned) {
                Icon(
                    Icons.Default.Check, contentDescription = "یاد گرفته شد",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp).size(20.dp)
                        .clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                        .padding(2.dp)
                )
            }
        }
    }
}
