package com.burton.builds.ui.runs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.builds.data.repository.BuildsRepository
import com.burton.builds.domain.RunFilter
import com.burton.builds.domain.Workflow
import com.burton.builds.domain.WorkflowRun
import com.burton.builds.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppRunsUi(
    val repo: String,
    val appName: String = "",
    val defaultBranch: String = "master",
    val scanning: Boolean = true,
    val error: String? = null,
    val filter: RunFilter = RunFilter.ALL,
    val runs: List<WorkflowRun> = emptyList(),
    val workflows: List<Workflow> = emptyList(),
    val dispatching: Boolean = false,
) {
    val visible: List<WorkflowRun>
        get() = when (filter) {
            RunFilter.ALL -> runs
            RunFilter.FAILED -> runs.filter { it.failed }
            RunFilter.ACTIVE -> runs.filter { it.inProgress }
        }
}

@HiltViewModel
class AppRunsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BuildsRepository,
) : ViewModel() {
    val repo: String = Routes.decode(savedStateHandle.get<String>("repo"))
    private val _ui = MutableStateFlow(
        AppRunsUi(
            repo = repo,
            appName = repository.state.value.catalog.firstOrNull { it.repo == repo }?.name.orEmpty()
                .ifBlank { repo.substringAfterLast('/') },
            defaultBranch = repository.defaultBranch(repo),
        ),
    )
    val ui = _ui.asStateFlow()

    init {
        refresh()
    }

    fun setFilter(filter: RunFilter) {
        _ui.update { it.copy(filter = filter) }
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(scanning = true, error = null) }
            val runs = runCatching { repository.listRuns(repo) }
            val workflows = runCatching { repository.listWorkflows(repo) }
            _ui.update {
                it.copy(
                    scanning = false,
                    runs = runs.getOrDefault(emptyList()),
                    workflows = workflows.getOrDefault(emptyList()),
                    error = runs.exceptionOrNull()?.let(repository::publicError)
                        ?: workflows.exceptionOrNull()?.let(repository::publicError),
                )
            }
        }
    }

    fun dispatch(workflow: Workflow) {
        viewModelScope.launch {
            _ui.update { it.copy(dispatching = true, error = null) }
            runCatching { repository.dispatch(repo, workflow.id, _ui.value.defaultBranch) }
                .onSuccess { refresh() }
                .onFailure { error ->
                    _ui.update { it.copy(dispatching = false, error = repository.publicError(error)) }
                }
            _ui.update { it.copy(dispatching = false) }
        }
    }
}
