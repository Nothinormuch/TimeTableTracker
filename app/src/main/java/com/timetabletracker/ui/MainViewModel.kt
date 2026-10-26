package com.timetabletracker.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.timetabletracker.R
import com.timetabletracker.data.api.models.DisplaySlot
import com.timetabletracker.data.local.PreferencesManager
import com.timetabletracker.data.local.SnapshotStorage
import com.timetabletracker.data.repository.Result
import com.timetabletracker.data.repository.TimetableRepository
import com.timetabletracker.scheduler.AlarmScheduler
import com.timetabletracker.worker.TimetableFetchWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class AppStatus {
    IDLE, CHECKING, CHANGES_FOUND, NO_CHANGES, ERROR
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val context = application.applicationContext
    private val prefsManager = PreferencesManager(context)
    private val snapshotStorage = SnapshotStorage(context)
    private val repository = TimetableRepository(prefsManager)

    // ─── LiveData ─────────────────────────────────────────────────────────────

    private val _lastCheckTime = MutableLiveData<String>()
    val lastCheckTime: LiveData<String> = _lastCheckTime

    private val _nextCheckTime = MutableLiveData<String>()
    val nextCheckTime: LiveData<String> = _nextCheckTime

    private val _status = MutableLiveData<AppStatus>(AppStatus.IDLE)
    val status: LiveData<AppStatus> = _status

    private val _displaySlots = MutableLiveData<List<DisplaySlot>>()
    val displaySlots: LiveData<List<DisplaySlot>> = _displaySlots

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _loginStatus = MutableLiveData<String>()
    val loginStatus: LiveData<String> = _loginStatus

    init {
        refreshUiState()
    }

    // ─── Public actions ───────────────────────────────────────────────────────

    /**
     * Refreshes all UI state from persisted storage.
     */
    fun refreshUiState() {
        _lastCheckTime.value = prefsManager.lastCheckTime.ifBlank { "Not checked yet" }
        _nextCheckTime.value = AlarmScheduler.getNextAlarmTimeString(context)
        _isLoggedIn.value = prefsManager.isLoggedIn()
        _displaySlots.value = snapshotStorage.loadDisplaySlots()
    }

    /**
     * Enqueues an immediate WorkManager job to check the timetable now.
     */
    fun checkNow() {
        if (!prefsManager.isLoggedIn()) {
            _errorMessage.value = "Please save your credentials first."
            return
        }

        _status.value = AppStatus.CHECKING

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TimetableFetchWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
            .addTag(TimetableFetchWorker.WORK_NAME_IMMEDIATE)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        // Observe the work state
        WorkManager.getInstance(context)
            .getWorkInfosByTagLiveData(TimetableFetchWorker.WORK_NAME_IMMEDIATE)
            .observeForever { workInfos ->
                val info = workInfos?.firstOrNull() ?: return@observeForever
                when {
                    info.state.isFinished -> {
                        refreshUiState()
                        val slots = snapshotStorage.loadDisplaySlots()
                        _displaySlots.value = slots
                        val hasChanges = slots.any { it.changeStatus != com.timetabletracker.data.api.models.SlotChangeStatus.UNCHANGED }
                        _status.value = if (hasChanges) AppStatus.CHANGES_FOUND else AppStatus.NO_CHANGES
                    }
                }
            }
    }

    /**
     * Saves credentials and attempts silent login to verify them.
     */
    fun saveCredentials(email: String, password: String) {
        prefsManager.userId = email
        prefsManager.password = password
        prefsManager.clearTokens()
        prefsManager.studentId = ""

        _loginStatus.value = "Verifying credentials…"

        viewModelScope.launch {
            val result = repository.authenticate()
            when (result) {
                is Result.Success -> {
                    _isLoggedIn.value = true
                    _loginStatus.value = getApplication<Application>().getString(R.string.msg_login_success)
                    Log.d(TAG, "Credentials verified successfully")
                }
                is Result.Error -> {
                    _isLoggedIn.value = false
                    _loginStatus.value = getApplication<Application>().getString(R.string.msg_login_failure, result.message)
                    Log.e(TAG, "Login verification failed: ${result.message}")
                }
            }
        }
    }

    /**
     * Saves the check time and reschedules the alarm.
     */
    fun saveCheckTime(hour: Int, minute: Int): Boolean {
        prefsManager.checkHour = hour
        prefsManager.checkMinute = minute
        val scheduled = AlarmScheduler.scheduleAlarms(context, resetAnchor = true)
        _nextCheckTime.value = AlarmScheduler.getNextAlarmTimeString(context)
        Log.d(TAG, "Check time saved: $hour:${minute.toString().padStart(2, '0')}")
        return scheduled
    }

    /**
     * Saves the repeat interval and starts a new schedule from the selected time.
     */
    fun saveCheckIntervalHours(intervalHours: Int): Boolean {
        require(intervalHours in setOf(1, 2, 5, 24)) {
            "Unsupported check interval: $intervalHours hours"
        }
        prefsManager.checkIntervalHours = intervalHours
        val scheduled = AlarmScheduler.scheduleAlarms(context, resetAnchor = true)
        _nextCheckTime.value = AlarmScheduler.getNextAlarmTimeString(context)
        Log.d(TAG, "Check interval saved: $intervalHours hour(s)")
        return scheduled
    }

    fun saveNotificationsEnabled(enabled: Boolean) {
        prefsManager.notificationsEnabled = enabled
    }

    fun getCheckHour(): Int = prefsManager.checkHour
    fun getCheckMinute(): Int = prefsManager.checkMinute
    fun getCheckIntervalHours(): Int = prefsManager.checkIntervalHours
    fun isNotificationsEnabled(): Boolean = prefsManager.notificationsEnabled
    fun clearErrorMessage() { _errorMessage.value = null }
}
