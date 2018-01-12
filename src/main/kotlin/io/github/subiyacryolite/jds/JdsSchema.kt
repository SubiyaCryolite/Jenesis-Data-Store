package io.github.subiyacryolite.jds

import io.github.subiyacryolite.jds.enums.JdsFieldType

object JdsSchema {

    /**
     * @param jdsDb
     * @param reportName
     * @param appendOnly
     * @return
     */
    fun generateTable(jdsDb: IJdsDb, reportName: String, appendOnly: Boolean): String {
        val guidDataType = getDbDataType(jdsDb, JdsFieldType.STRING, 96)
        val sb = StringBuilder()
        sb.append("CREATE TABLE ")
        sb.append(reportName)
        sb.append("( ${getPrimaryKey()} $guidDataType ${when (appendOnly) {
            true -> "PRIMARY KEY"
            else -> ""
        }})")
        return sb.toString()
    }

    /**
     * @param jdsDb
     * @param reportName
     * @param fields
     * @param columnToFieldMap
     * @param enumOrdinals
     * @return
     */
    fun generateColumns(jdsDb: IJdsDb, reportName: String, fields: Collection<JdsField>, columnToFieldMap: LinkedHashMap<String, JdsField>, enumOrdinals: HashMap<String, Int>): LinkedHashMap<String, String> {
        val collection = LinkedHashMap<String, String>()
        fields.sortedBy { it.name }.forEach { field ->
            when (field.type) {
                JdsFieldType.BLOB,
                JdsFieldType.ENTITY_COLLECTION,
                JdsFieldType.FLOAT_COLLECTION,
                JdsFieldType.INT_COLLECTION,
                JdsFieldType.DOUBLE_COLLECTION,
                JdsFieldType.LONG_COLLECTION,
                JdsFieldType.STRING_COLLECTION,
                JdsFieldType.DATE_TIME_COLLECTION -> {
                }
                JdsFieldType.ENUM_COLLECTION -> JdsFieldEnum.enums[field.id]!!.values.forEachIndexed { _, enum ->
                    val columnName = "${field.name}_${enum!!.ordinal}"
                    val columnDefinition = getDbDataType(jdsDb, JdsFieldType.BOOLEAN)
                    collection.put(columnName, String.format(jdsDb.getDbAddColumnSyntax(), reportName, columnName, columnDefinition))
                    columnToFieldMap.put(columnName, field)

                    enumOrdinals.put(columnName, enum!!.ordinal)
                }
                else -> {
                    collection.put(field.name, generateColumn(jdsDb, reportName, field))
                    columnToFieldMap.put(field.name, field)
                }
            }
        }
        return collection
    }

    /**
     * @param jdsDb
     * @param reportName
     * @param field
     * @param max
     * @return
     */
    @JvmOverloads
    private fun generateColumn(jdsDb: IJdsDb, reportName: String, field: JdsField, max: Int = 0): String {
        val columnName = field.name
        val columnType = getDbDataType(jdsDb, field.type, max)
        return String.format(jdsDb.getDbAddColumnSyntax(), reportName, columnName, columnType)
    }

    /**
     * @param jdsDb
     * @param fieldType
     * @param max
     * @return
     */
    @JvmOverloads
    fun getDbDataType(jdsDb: IJdsDb, fieldType: JdsFieldType, max: Int = 0): String {
        when (fieldType) {
            JdsFieldType.ENTITY -> return jdsDb.getDbStringDataType(96)//act as a fk if you will
            JdsFieldType.FLOAT -> return jdsDb.getDbFloatDataType()
            JdsFieldType.DOUBLE -> return jdsDb.getDbDoubleDataType()
            JdsFieldType.ZONED_DATE_TIME -> return jdsDb.getDbZonedDateTimeDataType()
            JdsFieldType.TIME -> return jdsDb.getDbTimeDataType()
            JdsFieldType.BLOB -> return jdsDb.getDbBlobDataType(max)
            JdsFieldType.BOOLEAN -> return jdsDb.getDbBooleanDataType()
            JdsFieldType.ENUM, JdsFieldType.INT -> return jdsDb.getDbIntegerDataType()
            JdsFieldType.DATE, JdsFieldType.DATE_TIME -> return jdsDb.getDbDateTimeDataType()
            JdsFieldType.LONG, JdsFieldType.DURATION -> return jdsDb.getDbLongDataType()
            JdsFieldType.PERIOD, JdsFieldType.STRING, JdsFieldType.YEAR_MONTH, JdsFieldType.MONTH_DAY -> return jdsDb.getDbStringDataType(max)
        }
        return "invalid"
    }

    /**
     * @return
     */
    fun getPrimaryKey(): String {
        return "uuid"
    }
}