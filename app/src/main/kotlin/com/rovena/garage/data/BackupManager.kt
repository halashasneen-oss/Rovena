package com.rovena.garage.data

import androidx.room.withTransaction
import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.ExpenseEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.RovenaDatabase
import com.rovena.garage.data.local.VehicleEntity
import org.json.JSONArray
import org.json.JSONObject

data class RestoreSummary(
    val vehicles: Int,
    val maintenance: Int,
    val fuel: Int,
    val expenses: Int,
    val documents: Int
)

class RovenaBackupManager(private val database: RovenaDatabase) {
    suspend fun exportJson(): String {
        val vehicleDao = database.vehicleDao()
        val recordDao = database.recordDao()
        val vehicles = vehicleDao.getAllOnce()

        val root = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("generatedAt", System.currentTimeMillis())
            .put("app", "Rovena")

        val vehicleArray = JSONArray()
        val maintenanceArray = JSONArray()
        val fuelArray = JSONArray()
        val expenseArray = JSONArray()
        val documentArray = JSONArray()

        vehicles.forEach { vehicle ->
            vehicleArray.put(vehicle.toJson())
            recordDao.getMaintenanceOnce(vehicle.id).forEach { maintenanceArray.put(it.toJson()) }
            recordDao.getFuelOnce(vehicle.id).forEach { fuelArray.put(it.toJson()) }
            recordDao.getExpensesOnce(vehicle.id).forEach { expenseArray.put(it.toJson()) }
            recordDao.getDocumentsOnce(vehicle.id).forEach { documentArray.put(it.toJson()) }
        }

        root.put("vehicles", vehicleArray)
        root.put("maintenance", maintenanceArray)
        root.put("fuel", fuelArray)
        root.put("expenses", expenseArray)
        root.put("documents", documentArray)
        return root.toString(2)
    }

    suspend fun restoreJson(json: String): RestoreSummary {
        val root = JSONObject(json)
        require(root.optInt("schemaVersion", -1) == SCHEMA_VERSION) { "Unsupported backup version" }

        val vehicles = root.optJSONArray("vehicles").toVehicleList()
        val vehicleIds = vehicles.map { it.id }.toSet()
        require(vehicles.all { it.id > 0L }) { "Backup contains invalid vehicle IDs" }
        require(vehicleIds.size == vehicles.size) { "Backup contains duplicate vehicle IDs" }

        val maintenance = root.optJSONArray("maintenance").toMaintenanceList()
        val fuel = root.optJSONArray("fuel").toFuelList()
        val expenses = root.optJSONArray("expenses").toExpenseList()
        val documents = root.optJSONArray("documents").toDocumentList()
        require((maintenance.map { it.vehicleId } + fuel.map { it.vehicleId } + expenses.map { it.vehicleId } + documents.map { it.vehicleId })
            .all { it in vehicleIds }) { "Backup contains orphan records" }

        val vehicleDao = database.vehicleDao()
        val recordDao = database.recordDao()
        database.withTransaction {
            recordDao.deleteAllDocuments()
            recordDao.deleteAllExpenses()
            recordDao.deleteAllFuel()
            recordDao.deleteAllMaintenance()
            vehicleDao.deleteAll()

            vehicles.forEach { vehicleDao.insert(it) }
            maintenance.forEach { recordDao.insertMaintenance(it) }
            fuel.forEach { recordDao.insertFuel(it) }
            expenses.forEach { recordDao.insertExpense(it) }
            documents.forEach { recordDao.insertDocument(it) }
        }

        return RestoreSummary(
            vehicles = vehicles.size,
            maintenance = maintenance.size,
            fuel = fuel.size,
            expenses = expenses.size,
            documents = documents.size
        )
    }

    private fun VehicleEntity.toJson() = JSONObject()
        .put("id", id)
        .put("make", make)
        .put("model", model)
        .put("year", year)
        .put("mileage", mileage)
        .put("fuelType", fuelType)
        .put("nickname", nickname)
        .put("plateNumber", plateNumber)
        .put("vin", vin)
        .put("currencyCode", currencyCode)
        .putNullable("photoUri", photoUri)
        .put("isPrimary", isPrimary)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)

    private fun MaintenanceEntity.toJson() = JSONObject()
        .put("id", id)
        .put("vehicleId", vehicleId)
        .put("serviceType", serviceType)
        .put("performedAt", performedAt)
        .put("mileage", mileage)
        .put("cost", cost)
        .put("notes", notes)
        .putNullable("nextDueMileage", nextDueMileage)
        .putNullable("nextDueAt", nextDueAt)
        .putNullable("attachmentUri", attachmentUri)

    private fun FuelEntryEntity.toJson() = JSONObject()
        .put("id", id)
        .put("vehicleId", vehicleId)
        .put("filledAt", filledAt)
        .put("mileage", mileage)
        .put("liters", liters)
        .put("totalCost", totalCost)
        .put("notes", notes)
        .putNullable("attachmentUri", attachmentUri)

    private fun ExpenseEntity.toJson() = JSONObject()
        .put("id", id)
        .put("vehicleId", vehicleId)
        .put("spentAt", spentAt)
        .put("category", category)
        .put("amount", amount)
        .put("notes", notes)
        .putNullable("attachmentUri", attachmentUri)

    private fun DocumentEntity.toJson() = JSONObject()
        .put("id", id)
        .put("vehicleId", vehicleId)
        .put("title", title)
        .put("category", category)
        .putNullable("expiryAt", expiryAt)
        .put("notes", notes)
        .putNullable("fileUri", fileUri)
        .put("createdAt", createdAt)

    private fun JSONArray?.toVehicleList(): List<VehicleEntity> = mapObjects { item ->
        VehicleEntity(
            id = item.getLong("id"),
            make = item.getString("make"),
            model = item.getString("model"),
            year = item.getInt("year"),
            mileage = item.getLong("mileage"),
            fuelType = item.getString("fuelType"),
            nickname = item.optString("nickname", ""),
            plateNumber = item.optString("plateNumber", ""),
            vin = item.optString("vin", ""),
            currencyCode = item.optString("currencyCode", "JOD"),
            photoUri = item.optNullableString("photoUri"),
            isPrimary = item.optBoolean("isPrimary", false),
            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
        )
    }

    private fun JSONArray?.toMaintenanceList(): List<MaintenanceEntity> = mapObjects { item ->
        MaintenanceEntity(
            id = item.getLong("id"),
            vehicleId = item.getLong("vehicleId"),
            serviceType = item.getString("serviceType"),
            performedAt = item.getLong("performedAt"),
            mileage = item.getLong("mileage"),
            cost = item.optDouble("cost", 0.0),
            notes = item.optString("notes", ""),
            nextDueMileage = item.optNullableLong("nextDueMileage"),
            nextDueAt = item.optNullableLong("nextDueAt"),
            attachmentUri = item.optNullableString("attachmentUri")
        )
    }

    private fun JSONArray?.toFuelList(): List<FuelEntryEntity> = mapObjects { item ->
        FuelEntryEntity(
            id = item.getLong("id"),
            vehicleId = item.getLong("vehicleId"),
            filledAt = item.getLong("filledAt"),
            mileage = item.getLong("mileage"),
            liters = item.getDouble("liters"),
            totalCost = item.optDouble("totalCost", 0.0),
            notes = item.optString("notes", ""),
            attachmentUri = item.optNullableString("attachmentUri")
        )
    }

    private fun JSONArray?.toExpenseList(): List<ExpenseEntity> = mapObjects { item ->
        ExpenseEntity(
            id = item.getLong("id"),
            vehicleId = item.getLong("vehicleId"),
            spentAt = item.getLong("spentAt"),
            category = item.getString("category"),
            amount = item.getDouble("amount"),
            notes = item.optString("notes", ""),
            attachmentUri = item.optNullableString("attachmentUri")
        )
    }

    private fun JSONArray?.toDocumentList(): List<DocumentEntity> = mapObjects { item ->
        DocumentEntity(
            id = item.getLong("id"),
            vehicleId = item.getLong("vehicleId"),
            title = item.getString("title"),
            category = item.getString("category"),
            expiryAt = item.optNullableLong("expiryAt"),
            notes = item.optString("notes", ""),
            fileUri = item.optNullableString("fileUri"),
            createdAt = item.getLong("createdAt")
        )
    }

    private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (index in 0 until length()) add(transform(getJSONObject(index)))
        }
    }

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
        put(key, value ?: JSONObject.NULL)

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else getLong(key)

    companion object {
        const val SCHEMA_VERSION = 1
    }
}
