package com.example.turtle.ui.addeditbill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turtle.Resource
import com.example.turtle.data.AuthRepository
import com.example.turtle.data.Bill
import com.example.turtle.data.BillRepository
import com.example.turtle.data.Profile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject


class AddEditBillViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val repository: BillRepository,
    private val userId: String,
    private val billId: String?,
    private val userEmail: String,
): ViewModel() {

    private var isNewBill: Boolean = true

    private val _snackbarText = MutableSharedFlow<String>()
    val snackbarText = _snackbarText.asSharedFlow()

    private val _showFriendsTitle = MutableStateFlow<Boolean>(false)
    val showFriendsTitle = _showFriendsTitle.asStateFlow()

    private val _isDone = MutableStateFlow<Boolean>(false)
    val isDone = _isDone.asStateFlow()

    private val _friendsProfiles = MutableStateFlow<Map<String, Profile>>(mapOf())
    val friendsProfiles = _friendsProfiles.asStateFlow()

    private val _bill = MutableStateFlow<Bill?>(null)
    val bill = _bill.shareIn(viewModelScope, SharingStarted.Eagerly)

    fun getBill() = viewModelScope.launch {
        if (isNewBill && billId != null) {
            repository.getBillAndExpenses(billId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data!!.users!!.filter { profile ->
                            profile.userId != userId
                        }.forEach { addFriend(it) }

                        _bill.value = result.data
                        isNewBill = false
                    }
                    is Resource.Error -> _snackbarText.emit(result.message!!)
                }
            }
        }
    }

    fun saveBill(title: String, description: String?) = viewModelScope.launch {
        if (title.isEmpty()) {
            _snackbarText.emit("Bill title cannot be empty")
            return@launch
        }

        val currentUserProfile = getCurrentUserProfile() ?:run { return@launch }
        val users = _friendsProfiles.value.values.toMutableList()
        users.add(currentUserProfile)

        val billObject = _bill.value?.copy(
            userOwnerId = userId,
            title = title,
            description = description,
            usersId = users.map { it.userId!! },
            users = users,
        ) ?: Bill(
            userOwnerId = userId,
            title = title,
            description = description,
            usersId = users.map { it.userId!! },
            users = users,
        )

        val result = if (isNewBill) {
            repository.createBill(billObject)
        } else {
            repository.updateBill(billId!!, billObject)
        }

        _snackbarText.emit(result.message!!)

        if (result is Resource.Success)
            _isDone.emit(true)
    }

    fun addFriend(email: String) = viewModelScope.launch {
        if (email == userEmail) {
            _snackbarText.emit("You cannot add yourself")
            return@launch
        }
        if (friendAlreadyAdded(email)) {
            _snackbarText.emit("Friend already added")
            return@launch
        }

        when(val result = authRepository.getProfileByEmail(email)) {
            is Resource.Success -> _friendsProfiles.add(email, result.data!!)
            is Resource.Error -> {
                _snackbarText.emit(result.message!!)
                return@launch
            }
        }

        isFriendsListEmpty()
    }

    private fun addFriend(profile: Profile) = viewModelScope.launch {
        val email = profile.email!!
        _friendsProfiles.add(email, profile)
        isFriendsListEmpty()
    }

    fun removeFriend(email: String) = viewModelScope.launch {
        if (!isNewBill && isUserInvolvedInExpenses(email)) {
            _snackbarText.emit(
                "This user is involved in at least one expense, so it is not possible to remove it"
            )
            return@launch
        }

        _friendsProfiles.remove(email)
        isFriendsListEmpty()
    }

    private fun isFriendsListEmpty() = viewModelScope.launch {
        if (_friendsProfiles.value.isEmpty())
            _showFriendsTitle.emit(false)
        else
            _showFriendsTitle.emit(true)
    }

    private fun isUserInvolvedInExpenses(userEmail: String): Boolean {
        val profile = _friendsProfiles.value[userEmail]!!
        return repository.isUserInvolvedInExpenses(_bill.value!!, profile.userId!!)
    }

    private fun friendAlreadyAdded(email: String): Boolean {
        return _friendsProfiles.value.containsKey(email)
    }

    private suspend fun getCurrentUserProfile(): Profile? {
        return when(val result = authRepository.getProfileByUserId(userId)) {
            is Resource.Success -> result.data!!
            is Resource.Error -> {
                _snackbarText.emit("An error occurred. Try again later.")
                null
            }
        }
    }

    private fun <K, V> MutableStateFlow<Map<K, V>>.remove(elementKey: K) {
        val newMap = value.toMutableMap()
        newMap.remove(elementKey)
        value = newMap
    }

    private fun <K, V> MutableStateFlow<Map<K, V>>.add(elementKey: K, elementValue: V) {
        val newMap = value.toMutableMap()
        newMap[elementKey] = elementValue
        value = newMap
    }
}
