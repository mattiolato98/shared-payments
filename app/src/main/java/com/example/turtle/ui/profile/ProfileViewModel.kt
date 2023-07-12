package com.example.turtle.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turtle.Resource
import com.example.turtle.data.AuthRepository
import com.example.turtle.data.Profile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


class ProfileViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val userId: String
): ViewModel() {

    private val _snackbarText = MutableSharedFlow<String>()
    val snackbarText = _snackbarText.asSharedFlow()

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile = _profile.asStateFlow()

    fun getProfile() = viewModelScope.launch {
        when (val result = repository.getProfileByUserId(userId)) {
            is Resource.Success -> _profile.emit(result.data!!)
            is Resource.Error -> _snackbarText.emit(result.message!!)
        }
    }
}