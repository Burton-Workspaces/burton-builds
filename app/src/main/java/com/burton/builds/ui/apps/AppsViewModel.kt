package com.burton.builds.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.builds.data.repository.BuildsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val repository: BuildsRepository,
) : ViewModel() {
    val state = repository.state

    fun refresh() {
        viewModelScope.launch { repository.refreshCatalogAndInbox() }
    }

    fun setSubscribed(repo: String, on: Boolean) {
        viewModelScope.launch { repository.setSubscribed(repo, on) }
    }
}
