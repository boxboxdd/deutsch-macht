package com.michel.deutschmacht.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.michel.deutschmacht.data.Lessons
import com.michel.deutschmacht.data.ProgressStore

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController) {
    val context = LocalContext.current
    val lessons = remember { Lessons.load(context) }
    val store = remember { ProgressStore(context) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate("search") },
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                text = { Text("جستجو") },
                containerColor = MaterialTheme.colorScheme.secondary
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Header(learnedTotal = lessons.sumOf { store.countLearned(it) },
                total = lessons.sumOf { it.entries.size })
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lessons, key = { it.num }) { L ->
                    val best = store.bestScore(L.num)
                    LessonCard(
                        num = L.num,
                        count = L.entries.size,
                        learned = store.countLearned(L),
                        best = best,
                        onClick = { nav.navigate("lesson/${L.num}") }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(learnedTotal: Int, total: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF1B4D3E), Color(0xFF2E7D5B))))
            .padding(20.dp, 24.dp, 20.dp, 20.dp)
    ) {
        Text("دوره آلمانی میشل توماس", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("۱۰ درس • ۱۱۱۴ واژه و جمله با ترجمه فارسی", color = Color(0xFFD8EFE3), fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        val pct = if (total == 0) 0f else learnedTotal.toFloat() / total
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFF2A94C),
                trackColor = Color(0x33FFFFFF)
            )
            Spacer(Modifier.width(10.dp))
            Text("%d%%".format((pct * 100).toInt()), color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LessonCard(num: Int, count: Int, learned: Int, best: Int, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$num", color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("درس $num", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("$count واژه و جمله", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (best >= 0) {
                    AssistChip(
                        onClick = onClick,
                        label = { Text("آزمون: $best٪") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    )
                } else {
                    AssistChip(
                        onClick = onClick,
                        label = { Text("شروع") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            val pct = if (count == 0) 0f else learned.toFloat() / count
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
