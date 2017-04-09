/*
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
package io.github.subiyacryolite.jds;


import io.github.subiyacryolite.jds.enums.JdsDatabaseComponent;
import io.github.subiyacryolite.jds.enums.JdsEnumTable;
import io.github.subiyacryolite.jds.enums.JdsImplementation;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;
import java.util.Set;


/**
 * This class is responsible for the setup of SQL connections, default database write statements, as well as the
 * initialization of core and custom components that will support JDS on the underlying Database implementation
 */
public abstract class JdsDatabase {
    /**
     * The JDBC driver class name
     */
    private String className;
    /**
     * The database connection string
     */
    private String url;
    /**
     * The database user name
     */
    private String userName;
    /**
     * The database user password
     */
    private String passWord;
    /**
     * A value indicating whether JDS should log every write in the system
     */
    private boolean logEdits;
    /**
     * A value indicating whether JDS should print internal log information
     */
    private boolean printOutput;
    /**
     * Checks if SQLIte connection properties have been set
     */
    private boolean propertiesSet;
    /**
     * A value indicating whether the underlying database implementation supports callable statements (Stored Procedures)
     */
    protected boolean supportsStatements;
    /**
     * SQLite connection properties
     */
    private Properties properties;
    /**
     * The underlying database implementation
     */
    protected JdsImplementation implementation;

    /**
     * Initialise JDS base tables
     */
    public void init() {
        prepareDatabaseComponents();
        prepareCustomDatabaseComponents();
    }

    /**
     * Initialise core database components
     */
    private void prepareDatabaseComponents() {
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.RefEntities);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreEntityOverview);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreEntityBinding);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.RefEnumValues);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.RefFields);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.RefFieldTypes);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreTextArray);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreFloatArray);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreIntegerArray);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreLongArray);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreDoubleArray);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreDateTimeArray);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreText);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreFloat);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreInteger);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreLong);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreDouble);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreDateTime);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreZonedDateTime);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.StoreOldFieldValues);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.BindEntityFields);
        prepareDatabaseComponent(JdsDatabaseComponent.TABLE, JdsEnumTable.BindEntityEnums);
    }

    /**
     * Set database connection properties
     *
     * @param className the JDBC driver class name
     * @param url       the JDBC driver connection string
     * @param userName  the database username
     * @param passWord  the users password
     */
    public final void setConnectionProperties(String className, String url, String userName, String passWord) {
        if (className == null || url == null || userName == null || passWord == null)
            throw new RuntimeException("Please supply valid values. Nulls not permitted");
        this.className = className;
        this.url = url;
        this.userName = userName;
        this.passWord = passWord;
        this.propertiesSet = true;
    }

    /**
     * Set SQLite database connection properties
     *
     * @param url        the JDBC driver connection string
     * @param properties SQLite properties including foreign key support
     */
    public void setConnectionProperties(String url, java.util.Properties properties) {
        if (url == null)
            throw new RuntimeException("Please supply valid values. Nulls not permitted");
        this.url = url;
        this.propertiesSet = true;
        this.properties = properties;
    }

    /**
     * Acquire standard connection to the database
     *
     * @return standard connection to the database
     * @throws ClassNotFoundException when JDBC driver is not configured correctly
     * @throws SQLException           when a standard SQL Exception occurs
     */
    public final synchronized Connection getConnection() throws ClassNotFoundException, SQLException {
        if (!propertiesSet)
            throw new RuntimeException("Please set connection properties before requesting a database connection");
        if (userName != null && passWord != null) {
            Class.forName(className);
            return DriverManager.getConnection(url, userName, passWord);
        } else {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(url, properties);
        }
    }

    /**
     * Indicates the underlying implementation of this JDS Database instance
     *
     * @return the underlying implementation of this JDS Database instance
     */
    public JdsImplementation getImplementation() {
        return this.implementation;
    }

    /**
     * Returns an instance of the requested JDS Database implementation
     *
     * @param implementation the target database implementation
     * @return an instance of the requested JDS Database implementation
     */
    public final static JdsDatabase getImplementation(JdsImplementation implementation) {
        switch (implementation) {
            case SQLITE:
                return new JdsDatabaseSqlite();
            case POSTGRES:
                return new JdsDatabasePostgres();
            case TSQL:
                return new JdsDatabaseTransactionalSql();
            case MYSQL:
                return new JdsDatabaseMySql();
        }
        return null;
    }

    /**
     * Delegates the creation of custom database components depending on the underlying JDS Database implementation
     *
     * @param databaseComponent the type of database component to create
     * @param jdsEnumTable      an enum that maps to the components concrete implementation details
     */
    protected final void prepareDatabaseComponent(JdsDatabaseComponent databaseComponent, JdsEnumTable jdsEnumTable) {
        switch (databaseComponent) {
            case TABLE:
                if (!doesTableExist(jdsEnumTable.getName()))
                    initiateDatabaseComponent(jdsEnumTable);
                break;
            case STORED_PROCEDURE:
                if (!doesProcedureExist(jdsEnumTable.getName()))
                    initiateDatabaseComponent(jdsEnumTable);
                break;
            case TRIGGER:
                if (!doesTriggerExist(jdsEnumTable.getName()))
                    initiateDatabaseComponent(jdsEnumTable);
                break;
        }
    }

    /**
     * Initialises core JDS Database components
     *
     * @param jdsEnumTable an enum that maps to the components concrete implementation details
     */
    private final void initiateDatabaseComponent(JdsEnumTable jdsEnumTable) {
        switch (jdsEnumTable) {
            case StoreZonedDateTime:
                createStoreZonedDateTime();
                break;
            case StoreTextArray:
                createStoreTextArray();
                break;
            case StoreFloatArray:
                createStoreFloatArray();
                break;
            case StoreIntegerArray:
                createStoreIntegerArray();
                break;
            case StoreLongArray:
                createStoreLongArray();
                break;
            case StoreDoubleArray:
                createStoreDoubleArray();
                break;
            case StoreDateTimeArray:
                createStoreDateTimeArray();
                break;
            case StoreText:
                createStoreText();
                break;
            case StoreFloat:
                createStoreFloat();
                break;
            case StoreInteger:
                createStoreInteger();
                break;
            case StoreLong:
                createStoreLong();
                break;
            case StoreDouble:
                createStoreDouble();
                break;
            case StoreDateTime:
                createStoreDateTime();
                break;
            case RefEntities:
                createStoreEntities();
                break;
            case RefEnumValues:
                createRefEnumValues();
                break;
            case RefFields:
                createRefFields();
                break;
            case RefFieldTypes:
                createRefFieldTypes();
                break;
            case BindEntityFields:
                createBindEntityFields();
                break;
            case BindEntityEnums:
                createBindEntityEnums();
                break;
            case StoreEntityOverview:
                createRefEntityOverview();
                break;
            case StoreOldFieldValues:
                createRefOldFieldValues();
                break;
            case StoreEntityBinding:
                createStoreEntityBinding();
                break;
        }
        prepareCustomDatabaseComponents(jdsEnumTable);
    }

    /**
     * Initialises custom JDS Database components
     *
     * @param jdsEnumTable an enum that maps to the components concrete implementation details
     */
    protected void prepareCustomDatabaseComponents(JdsEnumTable jdsEnumTable) {
    }

    /**
     * Checks if the specified table exists the the database
     *
     * @param tableName the table to look up
     * @return true if the specified table exists the the database
     */
    private final boolean doesTableExist(String tableName) {
        int answer = tableExists(tableName);
        return answer == 1;
    }

    /**
     * Checks if the specified procedure exists the the database
     *
     * @param procedureName the procedure to look up
     * @return true if the specified procedure exists the the database
     */
    private final boolean doesProcedureExist(String procedureName) {
        int answer = procedureExists(procedureName);
        return answer == 1;
    }

    /**
     * Checks if the specified trigger exists the the database
     *
     * @param triggerName the trigger to look up
     * @return true if the specified trigger exists the the database
     */
    private final boolean doesTriggerExist(String triggerName) {
        int answer = triggerExists(triggerName);
        return answer == 1;
    }

    /**
     * Checks if the specified index exists the the database
     *
     * @param indexName the index to look up
     * @return true if the specified index exists the the database
     */
    private final boolean doesIndexExist(String indexName) {
        int answer = indexExists(indexName);
        return answer == 1;
    }

    /**
     * Executes SQL found in the specified file. We recommend having one statement per file.
     *
     * @param fileName the file containing SQL to find
     */
    protected final void executeSqlFromFile(String fileName) {
        try (Connection connection = getConnection(); Statement innerStmt = connection.createStatement();) {
            String innerSql = fileToString(this.getClass().getClassLoader().getResourceAsStream(fileName));
            innerStmt.executeUpdate(innerSql);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * Method to read contents of a file to a String variable
     *
     * @param inputStream the stream containing a files contents
     * @return the contents of a file contained in the input stream
     * @throws Exception
     */
    private String fileToString(InputStream inputStream) throws Exception {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int result = bufferedInputStream.read();
        while (result != -1) {
            byteArrayOutputStream.write((byte) result);
            result = bufferedInputStream.read();
        }
        byteArrayOutputStream.close();
        bufferedInputStream.close();
        return byteArrayOutputStream.toString();
    }

    /**
     * Override this method with custom implementations of {@link #prepareDatabaseComponent(JdsDatabaseComponent, JdsEnumTable) prepareDatabaseComponents}
     * {@link #prepareDatabaseComponent(JdsDatabaseComponent, JdsEnumTable) prepareDatabaseComponents} delegates the creation of custom database components
     * depending on the underlying JDS Database implementation
     */
    protected void prepareCustomDatabaseComponents() {
    }

    /**
     * Database specific check to see if the specified table exists in the database
     *
     * @param tableName the table to look up
     * @return 1 if the specified table exists in the database
     */
    public abstract int tableExists(String tableName);

    /**
     * Database specific check to see if the specified procedure exists in the database
     *
     * @param procedureName the procedure to look up
     * @return 1 if the specified procedure exists in the database
     */
    public int procedureExists(String procedureName) {
        return 0;
    }

    /**
     * Database specific check to see if the specified trigger exists in the database
     *
     * @param triggerName the trigger to look up
     * @return 1 if the specified trigger exists in the database
     */
    public int triggerExists(String triggerName) {
        return 0;
    }

    /**
     * Database specific check to see if the specified index exists in the database
     *
     * @param indexName the trigger to look up
     * @return 1 if the specified index exists in the database
     */
    public int indexExists(String indexName) {
        return 0;
    }

    /**
     * Database specific SQL used to create the schema that stores text values
     */
    abstract void createStoreText();

    /**
     * Database specific SQL used to create the schema that stores datetime values
     */
    abstract void createStoreDateTime();

    /**
     * Database specific SQL used to create the schema that stores zoned datetime values
     */
    abstract void createStoreZonedDateTime();

    /**
     * Database specific SQL used to create the schema that stores integer values
     */
    abstract void createStoreInteger();

    /**
     * Database specific SQL used to create the schema that stores float values
     */
    abstract void createStoreFloat();

    /**
     * Database specific SQL used to create the schema that stores double values
     */
    abstract void createStoreDouble();

    /**
     * Database specific SQL used to create the schema that stores long values
     */
    abstract void createStoreLong();

    /**
     * Database specific SQL used to create the schema that stores text array values
     */
    abstract void createStoreTextArray();

    /**
     * Database specific SQL used to create the schema that stores datetime array values
     */
    abstract void createStoreDateTimeArray();

    /**
     * Database specific SQL used to create the schema that stores integer array values
     */
    abstract void createStoreIntegerArray();

    /**
     * Database specific SQL used to create the schema that stores float array values
     */
    abstract void createStoreFloatArray();

    /**
     * Database specific SQL used to create the schema that stores double array values
     */
    abstract void createStoreDoubleArray();

    /**
     * Database specific SQL used to create the schema that stores long array values
     */
    abstract void createStoreLongArray();

    /**
     * Database specific SQL used to create the schema that stores entity definitions
     */
    abstract void createStoreEntities();

    /**
     * Database specific SQL used to create the schema that stores enum definitions
     */
    abstract void createRefEnumValues();

    /**
     * Database specific SQL used to create the schema that stores field definitions
     */
    abstract void createRefFields();

    /**
     * Database specific SQL used to create the schema that stores field type definitions
     */
    abstract void createRefFieldTypes();

    /**
     * Database specific SQL used to create the schema that stores entity binding information
     */
    abstract void createBindEntityFields();

    /**
     * Database specific SQL used to create the schema that stores entity to enum binding information
     */
    abstract void createBindEntityEnums();

    /**
     * Database specific SQL used to create the schema that stores entity overview
     */
    abstract void createRefEntityOverview();

    /**
     * Database specific SQL used to create the schema that stores old field values of every type
     */
    abstract void createRefOldFieldValues();

    /**
     * Database specific SQL used to create the schema that stores entity to entity bindings
     */
    abstract void createStoreEntityBinding();

    /**
     * Binds all the fields attached to an entity
     *
     * @param entityId the value representing the entity
     * @param fieldIds the values representing the entity's fields
     */
    public final synchronized void mapClassFields(final long entityId, final Set<Long> fieldIds) {
        try (Connection connection = getConnection();
             PreparedStatement statement = supportsStatements() ? connection.prepareCall(mapClassFields()) : connection.prepareStatement(mapClassFields())) {
            connection.setAutoCommit(false);
            for (Long fieldId : fieldIds) {
                statement.setLong(1, entityId);
                statement.setLong(2, fieldId);
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
            System.out.printf("Mapped Fields for Entity[%s]\n", entityId);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * Binds all the enums attached to an entity
     *
     * @param entityId the value representing the entity
     * @param fields   the entity's enums
     */
    public final synchronized void mapClassEnums(final long entityId, final Set<JdsFieldEnum> fields) {
        mapEnumValues(fields);
        mapClassEnumsImplementation(entityId, fields);
        System.out.printf("Mapped Enums for Entity[%s]\n", entityId);
    }

    /**
     * Binds all the enums attached to an entity
     *
     * @param entityId the value representing the entity
     * @param fields   the entity's enums
     */
    private final synchronized void mapClassEnumsImplementation(final long entityId, final Set<JdsFieldEnum> fields) {
        try (Connection connection = getConnection();
             PreparedStatement statement = supportsStatements() ? connection.prepareCall(mapClassEnumsImplementation()) : connection.prepareStatement(mapClassEnumsImplementation())) {
            connection.setAutoCommit(false);
            for (JdsFieldEnum field : fields)
                for (int index = 0; index < field.getSequenceValues().size(); index++) {
                    statement.setLong(1, entityId);
                    statement.setLong(2, field.getField().getId());
                    statement.addBatch();
                }
            statement.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * Binds all the values attached to an enum
     *
     * @param fieldEnums the field enum
     */
    private final synchronized void mapEnumValues(final Set<JdsFieldEnum> fieldEnums) {
        try (Connection connection = getConnection(); PreparedStatement statement = supportsStatements() ? connection.prepareCall(mapEnumValues()) : connection.prepareStatement(mapEnumValues())) {
            connection.setAutoCommit(false);
            for (JdsFieldEnum field : fieldEnums) {
                for (int index = 0; index < field.getSequenceValues().size(); index++) {
                    statement.setLong(1, field.getField().getId());
                    statement.setInt(2, index);
                    statement.setString(3, field.getSequenceValues().get(index));
                    statement.addBatch();
                }
            }
            statement.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * Maps an entity's name to its id
     *
     * @param entityId   the entity's id
     * @param entityName the entity's name
     */
    public final synchronized void mapClassName(final long entityId, final String entityName) {
        try (Connection connection = getConnection();
             PreparedStatement statement = supportsStatements() ? connection.prepareCall(mapClassName()) : connection.prepareStatement(mapClassName())) {
            statement.setLong(1, entityId);
            statement.setString(2, entityName);
            statement.executeUpdate();
            System.out.printf("Mapped Entity [%S - %s]\n", entityName, entityId);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * A value indicating whether JDS is logging every write in the system
     *
     * @return true if JDS is logging every write in the system
     */
    public final boolean logEdits() {
        return logEdits;
    }

    /**
     * Determine whether JDS should log every write in the system
     *
     * @param value whether JDS should log every write in the system
     */
    public final void logEdits(boolean value) {
        this.logEdits = value;
    }

    /**
     * A value indicating whether JDS is printing internal log information
     *
     * @return true if JDS is printing internal log information
     */
    public final boolean printOutput() {
        return printOutput;
    }

    /**
     * Determine whether JDS should print internal log information
     *
     * @param value whether JDS should print internal log information
     */
    public final void printOutput(boolean value) {
        this.printOutput = value;
    }

    /**
     * A value indicating whether the underlying database implementation supports callable statements (Stored Procedures)
     *
     * @return true if the underlying database implementation supports callable statements (stored procedures)
     */
    public final boolean supportsStatements() {
        return supportsStatements;
    }

    /**
     * SQL call to save text values
     *
     * @return the default or overridden SQL statement for this operation
     */
    public String saveString() {
        return "{call procStoreText(?,?,?)}";
    }

    /**
     * SQL call to save long values
     *
     * @return the default or overridden SQL statement for this operation
     */
    public String saveLong() {
        return "{call procStoreLong(?,?,?)}";
    }

    /**
     * SQL call to save double values
     *
     * @return the default or overridden SQL statement for this operation
     */
    public String saveDouble() {
        return "{call procStoreDouble(?,?,?)}";
    }

    /**
     * SQL call to save float values
     *
     * @return the default or overridden SQL statement for this operation
     */
    public String saveFloat() {
        return "{call procStoreFloat(?,?,?)}";
    }

    /**
     * SQL call to save integer values
     *
     * @return the default or overridden SQL statement for this operation
     */
    public String saveInteger() {
        return "{call procStoreInteger(?,?,?)}";
    }

    /**
     * SQL call to save datetime values
     *
     * @return the default or overridden SQL statement for this operation
     */
    public String saveDateTime() {
        return "{call procStoreDateTime(?,?,?)}";
    }

    /**
     * SQL call to save datetime values
     *
     * @return the default or overridden SQL statement for this operation
     */
    public String saveZonedDateTime() {
        return "{call procStoreZonedDateTime(?,?,?)}";
    }

    /**
     * SQL call to save entity overview values
     *
     * @return the default or overridden SQL statement for this operation
     */
    public String saveOverview() {
        return "{call procStoreEntityOverview(?,?,?,?)}";
    }

    /**
     * SQL call to bind fields to entities
     *
     * @return the default or overridden SQL statement for this operation
     */
    public String mapClassFields() {
        return "{call procBindEntityFields(?,?)}";
    }

    /**
     * SQL call to bind enums to entities
     *
     * @return the default or overridden SQL statement for this operation
     */
    public String mapClassEnumsImplementation() {
        return "{call procBindEntityEnums(?,?)}";
    }

    /**
     * SQL call to map class names
     *
     * @return the default or overridden SQL statement for this operation
     */
    public String mapClassName() {
        return "{call procRefEntities(?,?)}";
    }

    /**
     * SQL call to save reference enum values
     *
     * @return the default or overridden SQL statement for this operation
     */
    public String mapEnumValues() {
        return "{call procRefEnumValues(?,?,?)}";
    }
}
