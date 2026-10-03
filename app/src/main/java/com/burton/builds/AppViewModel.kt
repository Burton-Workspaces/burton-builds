package com.burton.builds

import androidx.lifecycle.ViewModel
import com.burton.builds.data.repository.BuildsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    repository: BuildsRepository,
) : ViewModel() {
    val state = repository.state

    init {
        repository.start()
    }
}
