package com.rovena.garage.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.utils.EnumLabels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

enum class SearchResultType { VEHICLE, MAINTENANCE, FUEL, EXPENSE, DOCUMENT, PART, NOTE }

data class GlobalSearchResult(
    val type: SearchResultType,
    val id: Long,
    val vehicleId: Long,
    val title: String,
    val subtitle: String,
    /** Localized label resource for the record's fixed-vocabulary category/type, appended to [subtitle] at render time - never a raw enum constant name. */
    val subtitleCategoryRes: Int? = null
)

data class GlobalSearchUiState(
    val query: String = "",
    val results: List<GlobalSearchResult> = emptyList(),
    val isSearching: Boolean = false
)

private data class SearchPartial(
    val vehicles: List<com.rovena.garage.data.local.entities.VehicleEntity>,
    val maintenance: List<com.rovena.garage.data.local.entities.MaintenanceRecordEntity>,
    val fuel: List<com.rovena.garage.data.local.entities.FuelRecordEntity>,
    val expenses: List<com.rovena.garage.data.local.entities.ExpenseEntity>
)

private data class SearchPartial2(
    val parts: List<com.rovena.garage.data.local.entities.PartEntity>,
    val notes: List<com.rovena.garage.data.local.entities.VehicleNoteEntity>
)

/**
 * Compact offline global search (spec: Global Search) across every record
 * type in the whole garage, not scoped to the currently-selected vehicle -
 * genuinely useful the moment you have more than one car and can't remember
 * which vehicle a receipt or workshop visit was logged under. Every DAO
 * query it drives already existed (MaintenanceDao/FuelDao/ExpenseDao/
 * VehicleDao.search) but was never actually wired to any screen before this.
 * Only searches once the query is at least 2 characters, both to avoid a
 * full-table scan on every keystroke of a 1-character query and because a
 * single character is rarely a useful search anyway.
 */
class GlobalSearchViewModel(private val container: AppContainer) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    val uiState: StateFlow<GlobalSearchUiState> = queryFlow.flatMapLatest { query ->
        if (query.length < 2) return@flatMapLatest flowOf(GlobalSearchUiState(query = query))

        val partial = combine(
            container.vehicleRepository.search(query),
            container.maintenanceRepository.searchAcrossGarage(query),
            container.fuelRepository.searchAcrossGarage(query),
            container.expenseRepository.searchAcrossGarage(query)
        ) { vehicles, maintenance, fuel, expenses -> SearchPartial(vehicles, maintenance, fuel, expenses) }

        val partial2 = combine(
            container.partRepository.searchAcrossGarage(query),
            container.vehicleNoteRepository.searchAcrossGarage(query)
        ) { parts, notes -> SearchPartial2(parts, notes) }

        combine(
            partial,
            partial2,
            container.documentRepository.searchAcrossGarage(query),
            container.vehicleRepository.observeAll()
        ) { p, p2, documents, allVehicles ->
            val vehicleNameById = allVehicles.associate { it.id to "${it.make} ${it.model}" }
            fun vehicleName(vehicleId: Long) = vehicleNameById[vehicleId].orEmpty()

            val results = buildList {
                p.vehicles.forEach { add(GlobalSearchResult(SearchResultType.VEHICLE, it.id, it.id, "${it.make} ${it.model}", it.year.toString())) }
                p.maintenance.forEach {
                    add(GlobalSearchResult(SearchResultType.MAINTENANCE, it.id, it.vehicleId, it.description, vehicleName(it.vehicleId), EnumLabels.of(it.category)))
                }
                p.fuel.forEach { add(GlobalSearchResult(SearchResultType.FUEL, it.id, it.vehicleId, it.station ?: "", vehicleName(it.vehicleId))) }
                p.expenses.forEach {
                    add(GlobalSearchResult(SearchResultType.EXPENSE, it.id, it.vehicleId, it.description ?: it.vendor.orEmpty(), vehicleName(it.vehicleId), EnumLabels.of(it.category)))
                }
                documents.forEach {
                    add(GlobalSearchResult(SearchResultType.DOCUMENT, it.id, it.vehicleId, it.name, vehicleName(it.vehicleId), EnumLabels.of(it.type)))
                }
                p2.parts.forEach { add(GlobalSearchResult(SearchResultType.PART, it.id, it.vehicleId, it.name, vehicleName(it.vehicleId))) }
                p2.notes.forEach { add(GlobalSearchResult(SearchResultType.NOTE, it.id, it.vehicleId, it.text, vehicleName(it.vehicleId))) }
            }
            GlobalSearchUiState(query, results, false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GlobalSearchUiState())

    fun setQuery(query: String) {
        queryFlow.value = query
    }
}
