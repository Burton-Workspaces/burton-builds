package com.burton.builds.ui.run

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.builds.data.repository.BuildsRepository
import com.burton.builds.domain.RunDetail
import com.burton.builds.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RunUi(
    val repo: String,
    val runId: Long,
    val scanning: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val detail: RunDetail? = null,
)

@HiltViewModel
class RunViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BuildsRepository,
) : ViewModel() {
    val repo: String = Routes.decode(savedStateHandle.get<String>("repo"))
    val runId: Long = savedStateHandle.get<Long>("runId")
        ?: savedStateHandle.get<String>("runId")?.toLongOrNull()
        ?: 0L
    private val _ui = MutableStateFlow(RunUi(repo = repo, runId = runId))
    val ui = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(scanning = true, error = null) }
            runCatching { repository.getRun(repo, runId) }
                .onSuccess { detail ->
                    _ui.update { it.copy(scanning = false, detail = detail) }
                }
                .onFailure { error ->
                    _ui.update { it.copy(scanning = false, error = repository.publicError(error)) }
                }
        }
    }

    fun rerun(failedOnly: Boolean) {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null) }
            runCatching { repository.rerun(repo, runId, failedOnly) }
                .onSuccess {
                    refresh()
                    _ui.update { it.copy(busy = false) }
                }
                .onFailure { error ->
                    _ui.update { it.copy(busy = false, error = repository.publicError(error)) }
                }
        }
    }

    fun cancel() {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null) }
            runCatching { repository.cancel(repo, runId) }
                .onSuccess {
                    refresh()
                    _ui.update { it.copy(busy = false) }
                }
                .onFailure { error ->
                    _ui.update { it.copy(busy = false, error = repository.publicError(error)) }
                }
        }
    }
}
