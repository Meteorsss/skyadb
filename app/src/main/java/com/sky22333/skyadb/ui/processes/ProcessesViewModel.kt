package com.sky22333.skyadb.ui.processes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky22333.skyadb.AppServices
import com.sky22333.skyadb.R
import com.sky22333.skyadb.i18n.appString
import com.sky22333.skyadb.model.AdbOperationResult
import com.sky22333.skyadb.model.AppInfo
import com.sky22333.skyadb.model.operationCatching
import com.sky22333.skyadb.processes.DeviceMemory
import com.sky22333.skyadb.processes.DeviceProcess
import com.sky22333.skyadb.processes.ProcessSnapshotParser
import com.sky22333.skyadb.repository.AdbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProcessesUiState(
    val memory: DeviceMemory? = null,
    val processes: List<DeviceProcess> = emptyList(),
    val apps: Map<String, AppInfo> = emptyMap(),
    val query: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val pendingStop: DeviceProcess? = null,
) {
    fun app(process: DeviceProcess): AppInfo? = process.packageName(apps.keys)?.let(apps::get)
    val filtered: List<DeviceProcess> get() = processes.filter {
        it.name.contains(query, true) || it.pid.toString().contains(query) || app(it)?.label?.contains(query, true) == true
    }
}

class ProcessesViewModel(private val repository: AdbRepository = AppServices.adbRepository) : ViewModel() {
    private val state = MutableStateFlow(ProcessesUiState())
    val uiState = state.asStateFlow()

    fun query(value: String) { state.update { it.copy(query = value) } }
    fun requestStop(process: DeviceProcess) {
        if (!state.value.loading && state.value.app(process) != null) state.update { it.copy(pendingStop = process) }
    }
    fun cancelStop() { state.update { it.copy(pendingStop = null) } }

    fun refresh() {
        if (state.value.loading) return
        viewModelScope.launch {
            state.update { it.copy(loading = true, error = null, message = null) }
            readSnapshot(reloadApps = true)
            state.update { it.copy(loading = false) }
        }
    }

    private suspend fun shell(command: String): String = when (val result = repository.runShell(command)) {
        is AdbOperationResult.Failure -> error("${result.message}\n${result.suggestion}")
        is AdbOperationResult.Success -> {
            check(result.data.exitCode == 0) { result.data.errorOutput.ifBlank { result.data.output } }
            result.data.output
        }
    }

    private suspend fun readSnapshot(reloadApps: Boolean) {
        operationCatching {
            if (reloadApps || state.value.apps.isEmpty()) {
                when (val apps = repository.listApps()) {
                    is AdbOperationResult.Success -> state.update { it.copy(apps = apps.data.associateBy(AppInfo::packageName)) }
                    is AdbOperationResult.Failure -> error("${apps.message}\n${apps.suggestion}")
                }
            }
            val memory = ProcessSnapshotParser.memory(shell("cat /proc/meminfo"))
            val processes = ProcessSnapshotParser.processes(shell("dumpsys meminfo"))
            state.update { it.copy(memory = memory, processes = processes, error = null) }
        }.onFailure { error ->
            // Never leave stale, actionable PIDs visible after a failed refresh/disconnect.
            state.update { it.copy(memory = null, processes = emptyList(), error = error.message ?: appString(R.string.processes_load_failed)) }
        }
    }

    fun confirmStop() {
        val snapshot = state.value
        val process = snapshot.pendingStop ?: return
        val app = snapshot.app(process) ?: return
        if (snapshot.loading) return
        state.update { it.copy(pendingStop = null, loading = true, error = null, message = null) }
        viewModelScope.launch {
            when (val result = repository.forceStopApp(app.packageName)) {
                is AdbOperationResult.Failure -> state.update { it.copy(error = "${result.message}\n${result.suggestion}") }
                is AdbOperationResult.Success -> {
                    state.update { it.copy(message = appString(R.string.processes_stopped, app.label)) }
                    readSnapshot(reloadApps = false)
                }
            }
            state.update { it.copy(loading = false) }
        }
    }
}
