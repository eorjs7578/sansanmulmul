package com.sansantek.sansanmulmul.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sansantek.sansanmulmul.data.model.MountainPeakStone
import com.sansantek.sansanmulmul.data.repository.MountainPeakStoneRepository
import kotlinx.coroutines.launch

class MountainPeakStoneViewModel : ViewModel() {
    private val repository = MountainPeakStoneRepository()

    private val _stoneId = MutableLiveData<Int?>()
    val stoneId: LiveData<Int?> get() = _stoneId

    private val _AIReseultStoneId = MutableLiveData<Int?>()
    val AIReseultStoneId: LiveData<Int?> get() = _AIReseultStoneId

    private val _mountainPeakStoneList = MutableLiveData<List<MountainPeakStone>?>()
    val mountainPeakStoneList: LiveData<List<MountainPeakStone>?> get() = _mountainPeakStoneList

    private val _myMountainPeakStoneList = MutableLiveData<List<MountainPeakStone>?>()
    val myMountainPeakStoneList: LiveData<List<MountainPeakStone>?> get() = _myMountainPeakStoneList

    private val _isAddSuccess = MutableLiveData<Boolean?>()
    val isAddSuccess: LiveData<Boolean?> get() = _isAddSuccess

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun setStoneId(stoneId: Int) {
        _stoneId.value = stoneId
    }

    fun setAIResultStoneId(stoneId: Int) {
        _AIReseultStoneId.value = stoneId
    }

    fun addMountainPeakStone(accessToken: String, stoneId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.addStone(accessToken, stoneId)
                if (response != null) {
                    _isAddSuccess.postValue(response)
                } else {
                    _error.postValue("데이터를 불러오는 데 실패했습니다!ㅠ.ㅠ")
                }
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }

    fun fetchAllMountainPeakStones() {
        viewModelScope.launch {
            try {
                val response = repository.searchAllStones()
                if (response != null) {
                    _mountainPeakStoneList.postValue(response)
                } else {
                    _error.postValue("데이터를 불러오는 데 실패했습니다!ㅠ.ㅠ")
                }
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }

    fun fetchMyAllMountainPeakStones(userId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.getAllStonesByUserId(userId)
                if (response != null) {
                    _myMountainPeakStoneList.postValue(response)
                } else {
                    _error.postValue("데이터를 불러오는 데 실패했습니다!ㅠ.ㅠ")
                }
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }
}