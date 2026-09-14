package com.michel.deutschmacht.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.michel.deutschmacht.data.Lessons
import com.michel.deutschmacht.speech.Speaker

private data class Hit(val lesson: Int, val entry: com.michel.deutschmacht.data.Entry)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(nav: NavController) {
    val context = LocalContext.current
    val all = remember {
        Lessons.load(context).flatMap { L -> L.entries.map { Hit(L.num, it) } }
    }
    var query by remember { mutableStateOf("") }
    val hits = remember(query, all) {
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else all.filter { it.entry.de.contains(q, true) || it.entry.fa.contains(q) }.take(60)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جستجو در همه درس‌ها") },
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
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("واژه آلمانی یا فارسی…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "پاک کردن")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            if (query.isBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("برای جستجو شروع به تایپ کنید",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp, 4.dp, 12.dp, 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(hits) { h ->
                        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.padding(12.dp, 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(h.entry.de, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Text(h.entry.fa, fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("درس ${h.lesson}", fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary)
                                IconButton(onClick = { Speaker.speak(h.entry.de) }) {
                                    Text("🔊", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
