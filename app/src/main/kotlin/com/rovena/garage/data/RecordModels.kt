package com.rovena.garage.data

data class MaintenanceDraft(
    val serviceType: String,
    val mileage: Long,
    val cost: Double,
    val notes: String = "",
    val nextDueMileage: Long? = null,
    val nextDueAt: Long? = null
)

data class FuelDraft(
    val mileage: Long,
    val liters: Double,
    val totalCost: Double,
    val notes: String = ""
)

data class ExpenseDraft(
    val category: String,
    val amount: Double,
    val notes: String = ""
)

data class DocumentDraft(
    val title: String,
    val category: String,
    val expiryAt: Long? = null,
    val notes: String = "",
    val fileUri: String? = null
)

object RecordValidator {
    fun validMaintenance(draft: MaintenanceDraft): Boolean =
        draft.serviceType.isNotBlank() && draft.mileage >= 0L && draft.cost >= 0.0 &&
            (draft.nextDueMileage == null || draft.nextDueMileage >= draft.mileage)

    fun validFuel(draft: FuelDraft): Boolean =
        draft.mileage >= 0L && draft.liters > 0.0 && draft.totalCost >= 0.0

    fun validExpense(draft: ExpenseDraft): Boolean =
        draft.category.isNotBlank() && draft.amount > 0.0

    fun validDocument(draft: DocumentDraft): Boolean =
        draft.title.isNotBlank() && draft.category.isNotBlank()
}
