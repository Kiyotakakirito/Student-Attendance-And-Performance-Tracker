package com.example.studenttracker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class DirectoryRecord(val id: String, val title: String, val subtitle: String)

@Composable
fun DirectoryScreen(title: String, onBack: () -> Unit, onAdd: () -> Unit, onOpen: (String) -> Unit, load: suspend () -> List<DirectoryRecord>) {
    var refresh by remember { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    WorkspacePage(title, onBack, actions = {
        TextButton(onClick = { refresh++ }) { Text("Refresh") }
        TextButton(onClick = onAdd) { Text("Add") }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(query, { query = it }, label = { Text("Search $title") }, modifier = Modifier.fillMaxWidth().padding(16.dp), singleLine = true)
            RemoteContent(refresh, load) { records ->
                val matches = records.filter { "${it.id} ${it.title} ${it.subtitle}".contains(query.trim(), ignoreCase = true) }
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (matches.isEmpty()) item { Text(if (records.isEmpty()) "No records yet. Tap Add to get started." else "No matching records.") }
                    items(matches, key = { it.id }) { record ->
                        Card(onClick = { onOpen(record.id) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(record.title, style = MaterialTheme.typography.titleMedium)
                                Text(record.id, color = MaterialTheme.colorScheme.primary)
                                Text(record.subtitle, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
