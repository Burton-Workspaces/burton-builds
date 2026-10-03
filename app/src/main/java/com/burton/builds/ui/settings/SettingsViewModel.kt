package com.burton.builds.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.builds.data.repository.BuildsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: BuildsRepository,
) : ViewModel() {
    val state = repository.state

    fun signOut() {
        viewModelScope.launch { repository.signOut() }
    }
}
