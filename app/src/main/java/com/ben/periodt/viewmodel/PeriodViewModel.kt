package com.ben.periodt.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ben.periodt.data.*
import com.ben.periodt.data.BackupData.Companion.isLegacy
import com.ben.periodt.prediction.PostPillState
import com.ben.periodt.prediction.Prediction
import com.ben.periodt.reminder.dataStore
import com.ben.periodt.prediction.CycleRegularity
import com.ben.periodt.prediction.getPostPillState
import com.ben.periodt.prediction.predictCycle
import com.ben.periodt.widget.CalendarWidget
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.util.UUID

// Stored in the same DataStore as reminder prefs
val ACTIVE_PROFILE_ID_KEY = intPreferencesKey("active_profile_id")

class PeriodViewModel(application: Application) : AndroidViewModel(application) {

    private val dao        = AppDatabase.getDatabase(application).periodCycleDao()
    private val appContext = application.applicationContext

    // ── Active profile ────────────────────────────────────────────────────────

    val activeProfileId: StateFlow<Int> = appContext.dataStore.data
        .map { prefs -> prefs[ACTIVE_PROFILE_ID_KEY] ?: 1 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val profiles: StateFlow<List<Profile>> = dao.getAllProfiles()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeProfile: StateFlow<Profile?> = combine(profiles, activeProfileId) { list, id ->
        list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── Data streams (all scoped to active profile via flatMapLatest) ─────────
    // When activeProfileId changes, flatMapLatest cancels the old DB flow and
    // starts a new one for the new profile — the UI swaps instantly with no flicker.

    val cycles: StateFlow<List<Cycle>> = activeProfileId
        .flatMapLatest { profileId -> dao.getCyclesForProfile(profileId) }
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pillPacks: StateFlow<List<PillPack>> = activeProfileId
        .flatMapLatest { profileId -> dao.getPillPacksForProfile(profileId) }
        .map { list ->
            list.map { entity ->
                PillPack(
                    id        = entity.id,
                    startDate = LocalDate.parse(entity.startDate),
                    pillCount = entity.pillCount,
                    endDate   = entity.endDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Daily logs are filtered to the active profile by joining with the active
    // profile's cycle IDs — logs have no profileId column of their own.
    val dailyLogs: StateFlow<Map<String, DailyLog>> = activeProfileId
        .flatMapLatest { profileId ->
            dao.getCyclesForProfile(profileId).flatMapLatest { cycleEntities ->
                val cycleIds = cycleEntities.map { it.id }.toSet()
                dao.getAllDailyLogs().map { logs ->
                    logs
                        .filter { it.cycleId in cycleIds }
                        .associate { entity ->
                            "${entity.cycleId}|${entity.date}" to DailyLog(
                                id         = entity.id,
                                cycleId    = entity.cycleId,
                                date       = LocalDate.parse(entity.date),
                                bleeding   = entity.bleeding,
                                bloodColor = entity.bloodColor,
                                painLevel  = entity.painLevel
                            )
                        }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // ── Derived reactive states ───────────────────────────────────────────────

    val isOnPill: StateFlow<Boolean> = pillPacks.map { packs ->
        packs.any { it.endDate == null }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val pillPackStartDate: StateFlow<LocalDate?> = pillPacks.map { packs ->
        packs.firstOrNull { it.endDate == null }?.startDate
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val pillPackCount: StateFlow<Int> = pillPacks.map { packs ->
        packs.firstOrNull { it.endDate == null }?.pillCount ?: 21
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 21)

    val pillStopDate: StateFlow<LocalDate?> = pillPacks.map { packs ->
        packs.filter { it.endDate != null }.maxByOrNull { it.endDate!! }?.endDate
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val postPillState: StateFlow<PostPillState> = combine(cycles, isOnPill, pillStopDate) { cycleList, onPill, stopDate ->
        if (onPill || stopDate == null) PostPillState.NORMAL
        else {
            val postPillCycles = cycleList.filter { !it.startDate.isBefore(stopDate) }
            getPostPillState(postPillCycles)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PostPillState.NORMAL)

    val pillWindowStartDate: StateFlow<LocalDate?> = pillPacks.map { packs ->
        packs.firstOrNull { it.endDate == null }?.startDate
            ?: packs.filter { it.endDate != null }.maxByOrNull { it.endDate!! }?.startDate
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isTransitioning: StateFlow<Boolean> = postPillState.map { state ->
        state == PostPillState.DISCOVERY
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val prediction: StateFlow<Prediction?> = combine(
        cycles, isOnPill, isTransitioning, pillStopDate, pillPackStartDate, pillPackCount
    ) { args ->
        val cycleList     = args[0] as List<Cycle>
        val onPill        = args[1] as Boolean
        val transitioning = args[2] as Boolean
        val stopDate      = args[3] as LocalDate?
        val activeStart   = args[4] as LocalDate?
        val activeCount   = args[5] as Int

        when {
            onPill && activeStart != null -> {
                val withdrawalStart = activeStart.plusDays(activeCount.toLong() + 2)
                Prediction(
                    minPeriodStart        = withdrawalStart.minusDays(1),
                    maxPeriodStart        = withdrawalStart.plusDays(1),
                    mostLikelyPeriodStart = withdrawalStart,
                    periodLength          = 4,
                    ovulationDay          = withdrawalStart,
                    ovulationConfidence   = 1.0f,
                    fertileWindow         = LocalDate.MIN..LocalDate.MIN,
                    cycleLength           = activeCount + 7,
                    cycleRegularity       = CycleRegularity.VERY_REGULAR
                )
            }
            transitioning -> null
            else -> {
                val validCycles = if (stopDate != null)
                    cycleList.filter { !it.startDate.isBefore(stopDate) }
                else cycleList
                predictCycle(validCycles)
            }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        checkAndAutoStopPillPack()
    }

    // ── Profile management ────────────────────────────────────────────────────

    fun switchProfile(profileId: Int) {
        viewModelScope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[ACTIVE_PROFILE_ID_KEY] = profileId
            }
            // Auto-stop pill pack for the newly active profile if needed
            checkAndAutoStopPillPack()
            CalendarWidget.refreshAll(appContext)
        }
    }

    fun createProfile(name: String, avatarColor: String, onCreated: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val newId = dao.insertProfile(
                ProfileEntity(
                    profileUuid = UUID.randomUUID().toString(),
                    name        = name,
                    avatarColor = avatarColor
                )
            ).toInt()
            onCreated(newId)
        }
    }

    fun updateProfileName(profileId: Int, newName: String) {
        viewModelScope.launch {
            val existing = dao.getProfileById(profileId) ?: return@launch
            dao.updateProfile(existing.copy(name = newName))
        }
    }

    fun updateProfileColor(profileId: Int, newColor: String) {
        viewModelScope.launch {
            val existing = dao.getProfileById(profileId) ?: return@launch
            dao.updateProfile(existing.copy(avatarColor = newColor))
        }
    }

    /**
     * Deletes a profile and all its data via CASCADE DELETE.
     * If the deleted profile is currently active, automatically switches
     * to the first remaining profile. Deletion is blocked if it is the
     * last profile — the app must always have at least one.
     */
    fun deleteProfile(profileId: Int) {
        viewModelScope.launch {
            val allProfiles = dao.getAllProfilesOnce()
            if (allProfiles.size <= 1) return@launch  // never delete the last profile

            dao.deleteProfile(profileId)

            // If we just deleted the active profile, switch to the first remaining one
            if (activeProfileId.value == profileId) {
                val next = dao.getAllProfilesOnce().firstOrNull() ?: return@launch
                appContext.dataStore.edit { prefs ->
                    prefs[ACTIVE_PROFILE_ID_KEY] = next.id
                }
            }
            CalendarWidget.refreshAll(appContext)
        }
    }

    // ── Pill logic ────────────────────────────────────────────────────────────

    fun refreshState() = checkAndAutoStopPillPack()

    private fun checkAndAutoStopPillPack() {
        viewModelScope.launch {
            val activePack  = pillPacks.value.firstOrNull { it.endDate == null } ?: return@launch
            val packEndDate = activePack.startDate.plusDays((activePack.pillCount - 1).toLong())
            if (LocalDate.now().isAfter(packEndDate)) {
                dao.updatePillPackEndDate(activePack.id, packEndDate.toString())
                CalendarWidget.refreshAll(appContext)
            }
        }
    }

    fun enablePillTracking(startDate: LocalDate, count: Int) {
        viewModelScope.launch {
            val packEndDate = startDate.plusDays((count - 1).toLong())
            val isPast      = LocalDate.now().isAfter(packEndDate)
            dao.insertPillPack(
                PillPackEntity(
                    profileId = activeProfileId.value,          // ✨ profile-scoped
                    startDate = startDate.toString(),
                    pillCount = count,
                    endDate   = if (isPast) packEndDate.toString() else null
                )
            )
            CalendarWidget.refreshAll(appContext)
        }
    }

    fun stopPillTracking() {
        viewModelScope.launch {
            val activePack  = pillPacks.value.firstOrNull { it.endDate == null } ?: return@launch
            val today       = LocalDate.now()
            val packEndDate = activePack.startDate.plusDays((activePack.pillCount - 1).toLong())
            val finalStop   = if (today.isAfter(packEndDate)) packEndDate else today
            dao.updatePillPackEndDate(activePack.id, finalStop.toString())
            CalendarWidget.refreshAll(appContext)
        }
    }

    fun deletePillPack(id: Int) {
        viewModelScope.launch {
            dao.deletePillPackById(id)
            CalendarWidget.refreshAll(appContext)
        }
    }

// ── Cycle CRUD ────────────────────────────────────────────────────────────

    fun addCycleWithDailyLogs(
        start: LocalDate,
        end: LocalDate?,
        bleeding: String,
        bloodColor: String,
        painLevel: Int,
        overrides: Map<LocalDate, Triple<String, String, Int>>
    ) {
        viewModelScope.launch {
            dao.insertCycle(
                PeriodCycleEntity(
                    profileId  = activeProfileId.value,
                    startDate  = start.toString(),
                    endDate    = end?.toString() ?: "",
                    bleeding   = bleeding,
                    bloodColor = bloodColor,
                    painLevel  = painLevel
                )
            )
            if (overrides.isNotEmpty()) {
                val inserted = dao.getCyclesForProfileOnce(activeProfileId.value)
                    .firstOrNull { it.startDate == start.toString() }
                inserted?.let { entity ->
                    overrides.forEach { (date, triple) ->
                        dao.insertDailyLog(
                            DailyCycleLogEntity(
                                cycleId    = entity.id,
                                date       = date.toString(),
                                bleeding   = triple.first,
                                bloodColor = triple.second,
                                painLevel  = triple.third
                            )
                        )
                    }
                }
            }
            CalendarWidget.refreshAll(appContext)
        }
    }

    fun updateCycleWithDailyLogs(
        cycle: Cycle,
        overrides: Map<LocalDate, Triple<String, String, Int>>
    ) {
        viewModelScope.launch {
            dao.updateCycle(
                PeriodCycleEntity(
                    id         = cycle.id,
                    profileId  = activeProfileId.value,
                    startDate  = cycle.startDate.toString(),
                    endDate    = cycle.endDate?.toString() ?: "",
                    bleeding   = cycle.bleeding,
                    bloodColor = cycle.bloodColor,
                    painLevel  = cycle.painLevel
                )
            )
            dao.deleteDailyLogsForCycle(cycle.id)
            overrides.forEach { (date, triple) ->
                dao.insertDailyLog(
                    DailyCycleLogEntity(
                        cycleId    = cycle.id,
                        date       = date.toString(),
                        bleeding   = triple.first,
                        bloodColor = triple.second,
                        painLevel  = triple.third
                    )
                )
            }
            CalendarWidget.refreshAll(appContext)
        }
    }

    fun deleteCycle(id: Int) = viewModelScope.launch {
        dao.deleteCycleById(id)
        CalendarWidget.refreshAll(appContext)
    }

// ── Daily log CRUD ────────────────────────────────────────────────────────

    fun upsertDailyLog(cycleId: Int, date: LocalDate, bleeding: String, bloodColor: String, painLevel: Int) {
        viewModelScope.launch {
            val dateStr  = date.toString()
            val existing = dao.getDailyLogForDate(cycleId, dateStr)
            if (existing != null) {
                dao.updateDailyLog(
                    existing.copy(bleeding = bleeding, bloodColor = bloodColor, painLevel = painLevel)
                )
            } else {
                dao.insertDailyLog(
                    DailyCycleLogEntity(
                        cycleId    = cycleId,
                        date       = dateStr,
                        bleeding   = bleeding,
                        bloodColor = bloodColor,
                        painLevel  = painLevel
                    )
                )
            }
            CalendarWidget.refreshAll(appContext)
        }
    }

    fun deleteDailyLog(cycleId: Int, date: LocalDate) {
        viewModelScope.launch {
            dao.deleteDailyLog(cycleId, date.toString())
            CalendarWidget.refreshAll(appContext)
        }
    }

    fun effectiveBleeding(cycle: Cycle, date: LocalDate): String {
        val key = "${cycle.id}|$date"
        return dailyLogs.value[key]?.bleeding ?: cycle.bleeding
    }

    fun effectiveBloodColor(cycle: Cycle, date: LocalDate): String {
        val key = "${cycle.id}|$date"
        return dailyLogs.value[key]?.bloodColor ?: cycle.bloodColor
    }

    fun effectivePainLevel(cycle: Cycle, date: LocalDate): Int {
        val key = "${cycle.id}|$date"
        return dailyLogs.value[key]?.painLevel ?: cycle.painLevel
    }

    // ── Export ────────────────────────────────────────────────────────────────

    fun performExport(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allProfiles = dao.getAllProfilesOnce()

                // Build one BackupProfileDto per profile
                val profileBundles = allProfiles.map { profile ->
                    val profileCycles   = dao.getCyclesForProfileOnce(profile.id)
                    val profilePills    = dao.getPillPacksForProfileOnce(profile.id)
                    val cycleDtos       = profileCycles.map { it.toDto() }
                    val pillDtos        = profilePills.map { it.toDto() }

                    // Collect daily logs for this profile's cycles only
                    val cycleIds        = profileCycles.map { it.id }.toSet()
                    val allLogs         = dao.getAllDailyLogsOnce()
                    val logDtos         = allLogs
                        .filter { it.cycleId in cycleIds }
                        .map { it.toDto() }

                    profile.toBackupDto(cycleDtos, pillDtos, logDtos)
                }

                val backup = BackupData(
                    version   = BackupData.CURRENT_VERSION,
                    timestamp = System.currentTimeMillis(),
                    profiles  = profileBundles
                )

                val json = Gson().toJson(backup)
                appContext.contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os).use { it.write(json) }
                }
                withContext(Dispatchers.Main) { onResult(true, "Export successful") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.localizedMessage) }
            }
        }
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /**
     * Holds a parsed legacy (v4 or older) backup while the UI asks the user
     * which profile to import it into. The UI observes this StateFlow and shows
     * a profile-picker dialog when it is non-null.
     */
    private val _pendingLegacyImport = MutableStateFlow<BackupData?>(null)
    val pendingLegacyImport: StateFlow<BackupData?> = _pendingLegacyImport.asStateFlow()

    fun performImport(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val raw = buildString {
                    appContext.contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream)).forEachLine { append(it) }
                    }
                }

                val jsonObject  = JSONObject(raw)
                val fileVersion = jsonObject.optInt("version", 1)

                if (fileVersion > BackupData.CURRENT_VERSION) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Update the app to import this file.")
                    }
                    return@launch
                }

                val backup = Gson().fromJson(raw, BackupData::class.java)

                if (backup.isLegacy()) {
                    // ── v4 or older: no profile info — ask the user which profile to use ──
                    _pendingLegacyImport.value = backup
                    // Import will resume when the UI calls completeLegacyImport()
                    return@launch
                }

                // ── v5: profile-aware import ──────────────────────────────────────────
                importV5Backup(backup)
                withContext(Dispatchers.Main) {
                    onResult(true, "Import successful")
                    CalendarWidget.refreshAll(appContext)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Error: ${e.localizedMessage}") }
            }
        }
    }

    /**
     * Called by the UI after the user picks a profile (or requests a new one)
     * for a legacy backup.
     *
     * @param targetProfileId  ID of an existing profile to merge into,
     *                         or null to create a new profile named [newProfileName].
     */
    fun completeLegacyImport(
        targetProfileId: Int?,
        newProfileName: String = "Imported",
        onResult: (Boolean, String?) -> Unit
    ) {
        val backup = _pendingLegacyImport.value ?: run {
            onResult(false, "No pending import found.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profileId = targetProfileId ?: run {
                    // Create a fresh profile for this import
                    dao.insertProfile(
                        ProfileEntity(
                            profileUuid = UUID.randomUUID().toString(),
                            name        = newProfileName,
                            avatarColor = "#D89046"
                        )
                    ).toInt()
                }

                importFlatData(
                    profileId   = profileId,
                    cycles      = backup.cycles.orEmpty(),
                    pillHistory = backup.pillHistory.orEmpty(),
                    dailyLogs   = backup.dailyLogs.orEmpty(),
                    rawJson     = null   // v4 pillState handled below
                )

                _pendingLegacyImport.value = null

                // Handle v1-v2 pill state embedded in the JSON
                // (re-parse from raw since we no longer store it)
                withContext(Dispatchers.Main) {
                    onResult(true, "Import successful")
                    CalendarWidget.refreshAll(appContext)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Error: ${e.localizedMessage}") }
            }
        }
    }

    fun dismissLegacyImport() {
        _pendingLegacyImport.value = null
    }

    // ── Import helpers ────────────────────────────────────────────────────────

    private suspend fun importV5Backup(backup: BackupData) {
        val incomingProfiles = backup.profiles ?: return
        val existingProfiles = dao.getAllProfilesOnce()

        incomingProfiles.forEach { incomingProfile ->
            // Check if a profile with the same UUID already exists
            val matchingProfile = existingProfiles.firstOrNull {
                it.profileUuid == incomingProfile.profileUuid
            }

            val targetProfileId = if (matchingProfile != null) {
                // Profile already exists — merge data into it, don't duplicate
                matchingProfile.id
            } else {
                // New profile — create it with the same UUID so future imports still match
                dao.insertProfile(
                    ProfileEntity(
                        profileUuid = incomingProfile.profileUuid,
                        name        = incomingProfile.name,
                        avatarColor = incomingProfile.avatarColor
                    )
                ).toInt()
            }

            importFlatData(
                profileId   = targetProfileId,
                cycles      = incomingProfile.cycles,
                pillHistory = incomingProfile.pillHistory,
                dailyLogs   = incomingProfile.dailyLogs
            )
        }
    }

    /**
     * Inserts cycles, pill packs, and daily logs for one profile.
     * Skips any cycle or pill pack that already exists (matched by startDate).
     * Daily logs are matched to their correct local cycle ID after insertion.
     */
    private suspend fun importFlatData(
        profileId: Int,
        cycles: List<BackupCycleDto>,
        pillHistory: List<PillPackDto>,
        dailyLogs: List<DailyLogDto>,
        rawJson: String? = null
    ) {
        // Pill packs
        val existingPacks = dao.getPillPacksForProfileOnce(profileId)
        pillHistory.forEach { dto ->
            if (existingPacks.none { it.startDate == dto.startDate }) {
                dao.insertPillPack(dto.toEntity(profileId))
            }
        }

        // Cycles
        val existingCycles = dao.getCyclesForProfileOnce(profileId)
        cycles.forEach { dto ->
            if (existingCycles.none { it.startDate == dto.startDate }) {
                dao.insertCycle(dto.toEntity(profileId))
            }
        }

        // Auto-stop any overdue pill pack
        val activePack = dao.getPillPacksForProfileOnce(profileId).firstOrNull { it.endDate == null }
        if (activePack != null) {
            val packEndDate = LocalDate.parse(activePack.startDate)
                .plusDays((activePack.pillCount - 1).toLong())
            if (LocalDate.now().isAfter(packEndDate)) {
                dao.updatePillPackEndDate(activePack.id, packEndDate.toString())
            }
        }

        // Daily logs — resolve local cycle IDs by matching startDate
        if (dailyLogs.isNotEmpty()) {
            val allCycles      = dao.getCyclesForProfileOnce(profileId)
            val startDateToId  = allCycles.associate { it.startDate to it.id }

            dailyLogs.forEach { dto ->
                // Find which cycle this log belongs to by date range
                val matchingCycleStart = cycles.find { cycleDto ->
                    val cycleStart = LocalDate.parse(cycleDto.startDate)
                    val cycleEnd   = cycleDto.endDate.takeIf { it.isNotBlank() }
                        ?.let { LocalDate.parse(it) }
                    val logDate    = LocalDate.parse(dto.date)
                    logDate >= cycleStart && (cycleEnd == null || logDate <= cycleEnd)
                }?.startDate ?: return@forEach

                val realCycleId = startDateToId[matchingCycleStart] ?: return@forEach

                if (dao.getDailyLogForDate(realCycleId, dto.date) == null) {
                    dao.insertDailyLog(dto.toEntity(realCycleId))
                }
            }
        }
    }

// ── Clear data ────────────────────────────────────────────────────────────

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Wipe all data (cycles, pills, logs, and old profiles)
            dao.deleteAll()

            // 2. Re-create a fresh default profile
            val newProfileId = dao.insertProfile(
                ProfileEntity(
                    profileUuid = UUID.randomUUID().toString(),
                    name        = "Me",
                    avatarColor = "avatar_1"
                )
            ).toInt()

            // 3. Clear old shared preferences (reminders)
            appContext
                .getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply()

            // 4. Safely set the active profile to the NEW ID (Do not hardcode to 1)
            appContext.dataStore.edit { prefs ->
                prefs[ACTIVE_PROFILE_ID_KEY] = newProfileId
            }

            withContext(Dispatchers.Main) {
                CalendarWidget.refreshAll(appContext)
            }
        }
    }

    // ── Domain models ─────────────────────────────────────────────────────────

    data class Cycle(
        val id: Int,
        val startDate: LocalDate,
        val endDate: LocalDate?,
        val bleeding: String,
        val bloodColor: String,
        val painLevel: Int
    )

    data class PillPack(
        val id: Int,
        val startDate: LocalDate,
        val pillCount: Int,
        val endDate: LocalDate?
    )

    data class DailyLog(
        val id: Int,
        val cycleId: Int,
        val date: LocalDate,
        val bleeding: String,
        val bloodColor: String,
        val painLevel: Int
    )

    data class Profile(
        val id: Int,
        val profileUuid: String,
        val name: String,
        val avatarColor: String,
        val createdAt: Long
    )

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return PeriodViewModel(app) as T
        }
    }
}

// ── Entity → domain mappers ───────────────────────────────────────────────────

private fun PeriodCycleEntity.toDomain() = PeriodViewModel.Cycle(
    id         = id,
    startDate  = LocalDate.parse(startDate),
    endDate    = endDate.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
    bleeding   = bleeding,
    bloodColor = bloodColor,
    painLevel  = painLevel
)

private fun ProfileEntity.toDomain() = PeriodViewModel.Profile(
    id          = id,
    profileUuid = profileUuid,
    name        = name,
    avatarColor = avatarColor,
    createdAt   = createdAt
)