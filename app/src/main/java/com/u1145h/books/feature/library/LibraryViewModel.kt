package com.u1145h.books.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.u1145h.books.core.util.CoverUrlBuilder
import com.u1145h.books.data.repository.LibraryRepository
import com.u1145h.books.data.repository.SettingsRepository
import com.u1145h.books.domain.model.Series
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val isLoading: Boolean = true,
    val series: List<Series> = emptyList(),
    val error: String? = null,
    val page: Int = 0,
    val hasMore: Boolean = true,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
    val coverUrlBuilder: CoverUrlBuilder,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val libraryId: Int = savedStateHandle["libraryId"] ?: 0

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    val serverUrl: String get() = settingsRepository.currentServerUrl

    init { loadPage() }

    fun loadPage() {
        if (_state.value.isLoading && _state.value.page > 0) return
        viewModelScope.launch {
            val page = _state.value.page
            _state.update { it.copy(isLoading = true, error = null) }
            libraryRepository.getAllSeries(libraryId, page).fold(
                onSuccess = { incoming ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            series = if (page == 0) incoming else it.series + incoming,
                            page = page + 1,
                            hasMore = incoming.size >= 30,
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    fun refresh() {
        _state.update { LibraryUiState() }
        loadPage()
    }
}
