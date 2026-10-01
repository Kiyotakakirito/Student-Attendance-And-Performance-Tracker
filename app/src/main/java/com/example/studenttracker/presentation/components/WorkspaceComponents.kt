package com.example.studenttracker.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspacePage(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(title) }, navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        }, actions = actions)
    }, content = content)
}

@Composable
fun <T : Any> RemoteContent(key: Any = Unit, load: suspend () -> T, onBack: (() -> Unit)? = null, content: @Composable (T) -> Unit) {
    var data by remember(key) { mutableStateOf<T?>(null) }
    var error by remember(key) { mutableStateOf<String?>(null) }
    var attempt by remember(key) { mutableIntStateOf(0) }
    var loading by remember(key) { mutableStateOf(true) }
    LaunchedEffect(key, attempt) {
        loading = true
        error = null
        try { data = load() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "Unable to load data. Please try again." }
        finally { loading = false }
    }
    when {
        loading -> Column(Modifier.fillMaxWidth().safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onBack != null) TextButton(onClick = onBack) { Text("Back") }
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Loading records…")
        }
        error != null -> Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onBack != null) TextButton(onClick = onBack) { Text("Back") }
            Text(error!!, color = MaterialTheme.colorScheme.error)
            Button(onClick = { attempt++ }) { Text("Retry") }
        }
        data != null -> content(data!!)
    }
}

@Composable
fun InfoCard(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit = {}) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
        }
    }
}

@Composable
fun guardedBack(dirty: Boolean, busy: Boolean, onBack: () -> Unit): () -> Unit {
    var confirm by remember { mutableStateOf(false) }
    val back = { if (!busy) { if (dirty) confirm = true else onBack() } }
    BackHandler(enabled = dirty || busy, onBack = back)
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Discard unsaved changes?") },
        text = { Text("Changes on this screen have not been saved.") },
        confirmButton = { TextButton(onClick = onBack) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep editing") } })
    return back
}
