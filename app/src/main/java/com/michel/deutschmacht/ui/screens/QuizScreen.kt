package com.michel.deutschmacht.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.michel.deutschmacht.data.Entry
import com.michel.deutschmacht.data.Lessons
import com.michel.deutschmacht.data.ProgressStore
import com.michel.deutschmacht.speech.Speaker
import kotlin.random.Random

private data class Question(val prompt: Entry, val options: List<Entry>, val answer: Int, val deToFa: Boolean)

private fun buildQuiz(entries: List<Entry>, count: Int): List<Question> {
    val rng = Random(System.currentTimeMillis())
    val picked = entries.shuffled(rng).take(count)
    return picked.map { e ->
        val deToFa = rng.nextBoolean()
        val distract = entries.filter { it != e }.shuffled(rng).take(3)
        val opts = (distract + e).shuffled(rng)
        Question(e, opts, opts.indexOf(e), deToFa)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(nav: NavController, num: Int) {
    val context = LocalContext.current
    val lesson = remember(num) { Lessons.lesson(context, num) }
    val store = remember { ProgressStore(context) }
    val questions = remember(num) { buildQuiz(lesson.entries, minOf(12, lesson.entries.size)) }

    var idx by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var correctCount by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    fun commit(choice: Int) {
        if (selected != -1) return
        selected = choice
        if (choice == questions[idx].answer) correctCount++
        Speaker.speak(questions[idx].prompt.de)
    }

    fun next() {
        if (idx + 1 >= questions.size) {
            store.setBestScore(num, maxOf(store.bestScore(num), correctCount * 100 / questions.size))
            finished = true
        } else {
            idx++; selected = -1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("آزمون درس $num") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { pad ->
        if (finished) {
            ResultView(
                score = correctCount * 100 / questions.size,
                correct = correctCount, total = questions.size,
                onRetry = {
                    finished = false; idx = 0; selected = -1; correctCount = 0
                },
                onBack = { nav.popBackStack() }
            )
            return@Scaffold
        }
        val q = questions[idx]
        Column(
            Modifier.padding(pad).fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { (idx + 1f) / questions.size },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text("${idx + 1} از ${questions.size}", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))

            Text(
                if (q.deToFa) "معنی این جمله چیست؟" else "معادل آلمانی کدام است؟",
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            ElevatedCard(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(18.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (q.deToFa) q.prompt.de else q.prompt.fa,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, lineHeight = 30.sp
                    )
                    if (q.deToFa) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { Speaker.speak(q.prompt.de) }) { Text("🔊 تلفظ") }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            q.options.forEachIndexed { i, opt ->
                val isAnswer = i == q.answer
                val chosen = i == selected
                val color = when {
                    selected == -1 -> MaterialTheme.colorScheme.surface
                    isAnswer -> Color(0xFFBFE6CF)
                    chosen -> Color(0xFFF6C6C0)
                    else -> MaterialTheme.colorScheme.surface
                }
                OutlinedButton(
                    onClick = { commit(i) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = color)
                ) {
                    Text(
                        if (q.deToFa) opt.fa else opt.de,
                        fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp
                    )
                }
            }

            if (selected != -1) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { next() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (idx + 1 >= questions.size) "دیدن نتیجه" else "بعدی",
                        fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun ResultView(score: Int, correct: Int, total: Int, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val msg = when {
            score >= 90 -> "عالی! 🎉"
            score >= 70 -> "خوب بود! 👏"
            score >= 50 -> "بد نیست، تمرین کن 💪"
            else -> "دوباره تلاش کن 🌱"
        }
        Text("$score٪", fontSize = 56.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(msg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("$correct از $total درست", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onRetry, Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)) { Text("آزمون مجدد", fontSize = 16.sp) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)) { Text("بازگشت به درس", fontSize = 16.sp) }
    }
}
