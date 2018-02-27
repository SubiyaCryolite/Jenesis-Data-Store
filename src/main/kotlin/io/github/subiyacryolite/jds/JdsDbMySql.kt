/**
 * Jenesis Data Store Copyright (c) 2017 Ifunga Ndana. All rights reserved.
 *
 * 1. Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 2. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 3. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * Neither the name Jenesis Data Store nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.subiyacryolite.jds

import com.javaworld.NamedPreparedStatement
import io.github.subiyacryolite.jds.enums.JdsComponent
import io.github.subiyacryolite.jds.enums.JdsComponentType
import io.github.subiyacryolite.jds.enums.JdsImplementation
import java.sql.Connection

/**
 * The MySQL implementation of [JdsDataBase][JdsDb]
 */
abstract class JdsDbMySql : JdsDb {

    constructor(implementation: JdsImplementation, supportsStatements: Boolean) : super(implementation, supportsStatements)

    constructor() : this(JdsImplementation.MYSQL, true)

    override fun tableExists(connection: Connection, tableName: String): Int {
        var toReturn = 0
        val sql = "SELECT COUNT(table_schema) AS Result FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = :tableName AND TABLE_SCHEMA = :tableSchema"
        try {
            NamedPreparedStatement(connection, sql).use {
                it.setString("tableName", tableName)
                it.setString("tableSchema", connection.catalog)
                it.executeQuery().use {
                    while (it.next())
                        toReturn = it.getInt("Result")
                }
            }
        } catch (ex: Exception) {
            toReturn = 0
            ex.printStackTrace(System.err)
        }
        return toReturn
    }

    override fun procedureExists(connection: Connection, procedureName: String): Int {
        var toReturn = 0
        val sql = "SELECT COUNT(ROUTINE_NAME) AS Result FROM INFORMATION_SCHEMA.ROUTINES WHERE ROUTINE_TYPE='PROCEDURE' AND ROUTINE_NAME = :procedureName AND ROUTINE_SCHEMA = :procedureSchema"
        try {
            NamedPreparedStatement(connection, sql).use {
                it.setString("procedureName", procedureName)
                it.setString("procedureSchema", connection.catalog)
                it.executeQuery().use {
                    while (it.next())
                        toReturn = it.getInt("Result")
                }
            }
        } catch (ex: Exception) {
            toReturn = 0
            ex.printStackTrace(System.err)
        }
        return toReturn
    }

    override fun viewExists(connection: Connection, viewName: String): Int {
        var toReturn = 0
        val sql = "SELECT COUNT(ROUTINE_NAME) AS Result FROM INFORMATION_SCHEMA.VIEWS WHERE TABLE_NAME = :viewName AND TABLE_SCHEMA = :viewSchema"
        try {
            NamedPreparedStatement(connection, sql).use {
                it.setString("viewName", viewName)
                it.setString("viewSchema", connection.catalog)
                it.executeQuery().use {
                    while (it.next())
                        toReturn = it.getInt("Result")
                }
            }
        } catch (ex: Exception) {
            toReturn = 0
            ex.printStackTrace(System.err)
        }
        return toReturn
    }

    override fun columnExists(connection: Connection, tableName: String, columnName: String): Int {
        var toReturn = 0
        val sql = "SELECT COUNT(COLUMN_NAME) AS Result FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = :tableCatalog AND TABLE_NAME = :tableName AND COLUMN_NAME = :columnName"
        toReturn = columnExistsCommonImpl(connection, tableName, columnName, toReturn, sql)
        return toReturn
    }

    override fun createStoreEntityInheritance(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_entity_instance.sql")
    }

    override fun createStoreBoolean(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_store_boolean.sql")
    }

    override fun createStoreText(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_store_text.sql")
    }

    override fun createStoreDateTime(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_store_date_time.sql")
    }

    override fun createStoreZonedDateTime(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_store_zoned_date_time.sql")
    }

    override fun createStoreInteger(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_store_integer.sql")
    }

    override fun createStoreFloat(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_store_float.sql")
    }

    override fun createStoreDouble(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_store_double.sql")
    }

    override fun createStoreLong(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_store_long.sql")
    }

    override fun createStoreEntities(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_ref_entity.sql")
    }

    override fun createRefEnumValues(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_ref_enum.sql")
    }

    override fun createRefFields(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_ref_field.sql")
    }

    override fun createRefFieldTypes(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_ref_field_type.sql")
    }

    override fun createBindEntityFields(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_ref_entity_field.sql")
    }

    override fun createBindEntityEnums(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_ref_entity_enum.sql")
    }

    override fun createRefEntityOverview(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_entity_overview.sql")
    }

    override fun createStoreEntityBinding(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_entity_binding.sql")
    }

    override fun createStoreTime(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_store_time.sql")
    }

    override fun createStoreBlob(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_store_blob.sql")
    }

    override fun createRefInheritance(connection: Connection) {
        executeSqlFromFile(connection, "sql/mysql/jds_ref_entity_inheritance.sql")
    }

    override fun prepareCustomDatabaseComponents(connection: Connection) {
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.SAVE_BOOLEAN)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.SAVE_BLOB)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.SAVE_TEXT)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.SAVE_LONG)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.SAVE_INTEGER)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.SAVE_FLOAT)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.SAVE_DOUBLE)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.SAVE_DATE_TIME)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.SAVE_TIME)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.SAVE_ZONED_DATE_TIME)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.SAVE_ENTITY_V_3)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.MAP_ENTITY_FIELDS)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.MAP_ENTITY_ENUMS)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.MAP_CLASS_NAME)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.MAP_ENUM_VALUES)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.MAP_FIELD_NAMES)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.MAP_ENTITY_INHERITANCE)
        prepareDatabaseComponent(connection, JdsComponentType.STORED_PROCEDURE, JdsComponent.SAVE_ENTITY_INHERITANCE)
    }

    override fun prepareCustomDatabaseComponents(connection: Connection, jdsComponent: JdsComponent) {
        when (jdsComponent) {
            JdsComponent.SAVE_ENTITY_V_3 -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_store_entity_overview_v3.sql")
            JdsComponent.SAVE_ENTITY_INHERITANCE -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_store_entity_inheritance.sql")
            JdsComponent.MAP_FIELD_NAMES -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_ref_field.sql")
            JdsComponent.SAVE_BLOB -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_store_blob.sql")
            JdsComponent.SAVE_BOOLEAN -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_store_boolean.sql")
            JdsComponent.SAVE_TIME -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_store_time.sql")
            JdsComponent.SAVE_TEXT -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_store_text.sql")
            JdsComponent.SAVE_LONG -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_store_long.sql")
            JdsComponent.SAVE_INTEGER -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_store_integer.sql")
            JdsComponent.SAVE_FLOAT -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_store_float.sql")
            JdsComponent.SAVE_DOUBLE -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_store_double.sql")
            JdsComponent.SAVE_DATE_TIME -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_store_date_time.sql")
            JdsComponent.SAVE_ZONED_DATE_TIME -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_store_zoned_date_time.sql")
            JdsComponent.MAP_ENTITY_FIELDS -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_ref_entity_field.sql")
            JdsComponent.MAP_ENTITY_ENUMS -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_ref_entity_enum.sql")
            JdsComponent.MAP_CLASS_NAME -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_ref_entity.sql")
            JdsComponent.MAP_ENUM_VALUES -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_ref_enum.sql")
            JdsComponent.MAP_ENTITY_INHERITANCE -> executeSqlFromFile(connection, "sql/mysql/procedures/proc_bind_parent_to_child.sql")
            else -> {
            }
        }
    }

    override fun getDbFloatDataType(): String {
        return "FLOAT"
    }

    override fun getDbDoubleDataType(): String {
        return "DOUBLE"
    }

    override fun getDbZonedDateTimeDataType(): String {
        return "TIMESTAMP"
    }

    override fun getDbTimeDataType(): String {
        return "TIME"
    }

    override fun getDbBlobDataType(max: Int): String {
        return "BLOB"
    }

    override fun getDbIntegerDataType(): String {
        return "INT"
    }

    override fun getDbDateTimeDataType(): String {
        return "DATETIME"
    }

    override fun getDbLongDataType(): String {
        return "BIGINT"
    }

    override fun getDbStringDataType(max: Int): String {
        return if (max == 0)
            "TEXT"
        else
            "VARCHAR($max)"
    }

    override fun getDbBooleanDataType(): String {
        return "BOOLEAN"
    }

    override fun getDbCreateIndexSyntax(tableName: String, columnName: String, indexName: String): String {
        return "CREATE INDEX $indexName ON $tableName($columnName);"
    }
}