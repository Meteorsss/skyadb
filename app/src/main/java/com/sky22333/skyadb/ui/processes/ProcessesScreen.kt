package com.sky22333.skyadb.ui.processes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky22333.skyadb.R
import com.sky22333.skyadb.ui.components.AppTopBar
import java.util.Locale

private fun memorySize(kib: Long): String = if (kib >= 1024 * 1024) {
    String.format(Locale.getDefault(), "%.2f GiB", kib / (1024.0 * 1024))
} else String.format(Locale.getDefault(), "%.1f MiB", kib / 1024.0)

@Composable
fun ProcessesScreen(
    bottomPadding: Dp = 0.dp,
    onBackClick: () -> Unit,
    viewModel: ProcessesViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    Column(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding)) {
        AppTopBar(
            title = { Text(stringResource(R.string.processes_title)) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                }
            },
            actions = {
                IconButton(onClick = viewModel::refresh, enabled = !state.loading) {
                    Icon(Icons.Outlined.Refresh, stringResource(R.string.action_refresh))
                }
            },
        )
        if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.memory?.let { memory ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.processes_device_memory, memorySize(memory.totalKiB)), style = MaterialTheme.typography.titleMedium)
                            LinearProgressIndicator(
                                progress = { (memory.usedKiB.toDouble() / memory.totalKiB).toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(stringResource(R.string.processes_used_memory, memorySize(memory.usedKiB)))
                            Text(stringResource(R.string.processes_available_memory, memorySize(memory.availableKiB)))
                            Text(stringResource(R.string.processes_running_memory, memorySize(state.processes.sumOf { it.pssKiB })))
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.query, onValueChange = viewModel::query,
                    label = { Text(stringResource(R.string.processes_search)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
            }
            item {
                Text(stringResource(R.string.processes_explanation), style = MaterialTheme.typography.bodySmall)
            }
            state.error?.let { error -> item {
                Text(stringResource(R.string.processes_load_failed) + "\n" + error, color = MaterialTheme.colorScheme.error)
            } }
            state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.primary) } }
            item { Text(stringResource(R.string.processes_count, state.filtered.size), style = MaterialTheme.typography.titleMedium) }
            items(state.filtered, key = { it.pid }) { process ->
                val app = state.app(process)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(app?.label ?: process.name, style = MaterialTheme.typography.titleMedium)
                            Text(process.name, style = MaterialTheme.typography.bodySmall)
                            Text("PID ${process.pid} · ${memorySize(process.pssKiB)} PSS", style = MaterialTheme.typography.bodyMedium)
                            if (app == null) Text(stringResource(R.string.processes_native_readonly), style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { viewModel.requestStop(process) }, enabled = app != null && !state.loading) {
                            Icon(Icons.Outlined.StopCircle, stringResource(R.string.processes_stop))
                        }
                    }
                }
            }
        }
    }
    state.pendingStop?.let { process ->
        val app = state.app(process)
        AlertDialog(
            onDismissRequest = viewModel::cancelStop,
            title = { Text(stringResource(R.string.processes_stop)) },
            text = { Text(stringResource(R.string.processes_stop_confirm, app?.label ?: process.name, app?.packageName ?: process.name)) },
            confirmButton = { TextButton(onClick = viewModel::confirmStop) { Text(stringResource(R.string.processes_stop)) } },
            dismissButton = { TextButton(onClick = viewModel::cancelStop) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
}
