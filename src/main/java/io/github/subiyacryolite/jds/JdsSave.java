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

import io.github.subiyacryolite.jds.events.JdsPostSaveListener;
import io.github.subiyacryolite.jds.events.JdsPreSaveListener;
import io.github.subiyacryolite.jds.events.OnPostSaveEventArguments;
import io.github.subiyacryolite.jds.events.OnPreSaveEventArguments;
import javafx.beans.property.*;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * This class is responsible for persisting on or more {@link JdsEntity JdsEntities}
 */
public class JdsSave implements Callable<Boolean> {

    private final JdsDb jdsDb;
    private final int batchSize;
    private final Connection connection;
    private final Collection<? extends JdsEntity> entities;
    private final boolean recursiveInnerCall;

    /**
     * @param jdsDb
     * @param entities
     */
    public JdsSave(final JdsDb jdsDb, final Collection<? extends JdsEntity> entities) throws SQLException, ClassNotFoundException {
        this(jdsDb, jdsDb.getConnection(), 0, entities, false);
    }

    /**
     * @param jdsDb
     * @param batchSize
     * @param entities
     */
    public JdsSave(final JdsDb jdsDb, final int batchSize, final Collection<? extends JdsEntity> entities) throws SQLException, ClassNotFoundException {
        this(jdsDb, jdsDb.getConnection(), batchSize, entities, false);
    }

    private JdsSave(final JdsDb jdsDb, Connection connection, final int batchSize, final Collection<? extends JdsEntity> entities, boolean recursiveInnerCall) {
        this.jdsDb = jdsDb;
        this.batchSize = batchSize;
        this.entities = entities;
        this.connection = connection;
        this.recursiveInnerCall = recursiveInnerCall;
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public Boolean call() throws Exception {
        JdsSaveContainer saveContainer = new JdsSaveContainer();
        List<Collection<JdsEntity>> batchEntities = new ArrayList<>();
        setupBatches(batchSize, entities, saveContainer, batchEntities);
        int step = 0;
        int stepsRequired = batchEntities.size() + 1;
        for (Collection<JdsEntity> current : batchEntities) {
            saveInner(jdsDb, current, saveContainer, step);
            step++;
            if (jdsDb.printOutput())
                System.out.printf("Processed batch [%s of %s]\n", step, stepsRequired);
        }
        return true;
    }

    /**
     * @param batchSize
     * @param entities
     * @param container
     * @param batchEntities
     */
    private void setupBatches(int batchSize, Collection<? extends JdsEntity> entities, final JdsSaveContainer container, List<Collection<JdsEntity>> batchEntities) {
        //create batches
        int currentBatch = 0;
        //default bach is 0 or -1 which means one large chunk. Anything above is a single batch
        int iteration = 0;
        if (batchSize > 0) {
            for (JdsEntity jdsEntity : entities) {
                if (currentBatch == batchSize) {
                    currentBatch++;
                    iteration = 0;
                }
                if (iteration == 0) {
                    createBatchCollection(container, batchEntities);
                }
                batchEntities.get(currentBatch).add(jdsEntity);
                iteration++;
            }
        } else {
            //single large batch, good luck
            createBatchCollection(container, batchEntities);
            for (JdsEntity jdsEntity : entities) {
                batchEntities.get(0).add(jdsEntity);
            }
        }
    }

    /**
     * @param saveContainer
     * @param batchEntities
     */
    private void createBatchCollection(final JdsSaveContainer saveContainer, final List<Collection<JdsEntity>> batchEntities) {
        batchEntities.add(new ArrayList<>());
        saveContainer.overviews.add(new HashSet<>());
        //primitives
        saveContainer.localDateTimes.add(new HashMap<>());
        saveContainer.zonedDateTimes.add(new HashMap<>());
        saveContainer.localTimes.add(new HashMap<>());
        saveContainer.localDates.add(new HashMap<>());
        saveContainer.strings.add(new HashMap<>());
        saveContainer.booleans.add(new HashMap<>());
        saveContainer.floats.add(new HashMap<>());
        saveContainer.doubles.add(new HashMap<>());
        saveContainer.longs.add(new HashMap<>());
        saveContainer.integers.add(new HashMap<>());
        //blob
        saveContainer.blobs.add(new HashMap<>());
        //arrays
        saveContainer.stringArrays.add(new HashMap<>());
        saveContainer.dateTimeArrays.add(new HashMap<>());
        saveContainer.floatArrays.add(new HashMap<>());
        saveContainer.doubleArrays.add(new HashMap<>());
        saveContainer.longArrays.add(new HashMap<>());
        saveContainer.integerArrays.add(new HashMap<>());
        //enums
        saveContainer.enums.add(new HashMap<>());
        //objects
        saveContainer.objects.add(new HashMap<>());
        //object arrays
        saveContainer.objectArrays.add(new HashMap<>());
    }

    /**
     * @param database
     * @param entities
     * @param saveContainer
     * @param step
     */
    private void saveInner(final JdsDb database, final Collection<JdsEntity> entities, final JdsSaveContainer saveContainer, final int step) throws Exception {
        //fire
        int sequence = 0;
        for (final JdsEntity entity : entities) {
            if (entity == null) continue;
            if (entity instanceof JdsPreSaveListener) {
                ((JdsPreSaveListener) entity).onPreSave(new OnPreSaveEventArguments(step, sequence, entities.size()));
            }
            //update the modified date to time of commit
            entity.setDateModified(LocalDateTime.now());
            saveContainer.overviews.get(step).add(entity.getOverview());
            //assign properties
            saveContainer.booleans.get(step).put(entity.getEntityGuid(), entity.booleanProperties);
            saveContainer.localDateTimes.get(step).put(entity.getEntityGuid(), entity.localDateTimeProperties);
            saveContainer.zonedDateTimes.get(step).put(entity.getEntityGuid(), entity.zonedDateTimeProperties);
            saveContainer.localTimes.get(step).put(entity.getEntityGuid(), entity.localTimeProperties);
            saveContainer.localDates.get(step).put(entity.getEntityGuid(), entity.localDateProperties);
            saveContainer.strings.get(step).put(entity.getEntityGuid(), entity.stringProperties);
            saveContainer.floats.get(step).put(entity.getEntityGuid(), entity.floatProperties);
            saveContainer.doubles.get(step).put(entity.getEntityGuid(), entity.doubleProperties);
            saveContainer.longs.get(step).put(entity.getEntityGuid(), entity.longProperties);
            saveContainer.integers.get(step).put(entity.getEntityGuid(), entity.integerProperties);
            //assign blobs
            saveContainer.blobs.get(step).put(entity.getEntityGuid(), entity.blobProperties);
            //assign lists
            saveContainer.stringArrays.get(step).put(entity.getEntityGuid(), entity.stringArrayProperties);
            saveContainer.dateTimeArrays.get(step).put(entity.getEntityGuid(), entity.dateTimeArrayProperties);
            saveContainer.floatArrays.get(step).put(entity.getEntityGuid(), entity.floatArrayProperties);
            saveContainer.doubleArrays.get(step).put(entity.getEntityGuid(), entity.doubleArrayProperties);
            saveContainer.longArrays.get(step).put(entity.getEntityGuid(), entity.longArrayProperties);
            saveContainer.integerArrays.get(step).put(entity.getEntityGuid(), entity.integerArrayProperties);
            //assign enums
            saveContainer.enums.get(step).put(entity.getEntityGuid(), entity.enumProperties);
            //assign objects
            saveContainer.objectArrays.get(step).put(entity.getEntityGuid(), entity.objectArrayProperties);
            saveContainer.objects.get(step).put(entity.getEntityGuid(), entity.objectProperties);
            sequence++;
        }
        //share one connection for raw saves, helps with performance
        try {
            saveOverviews(connection, saveContainer.overviews.get(step));
            //properties
            saveBooleans(connection, saveContainer.booleans.get(step));
            saveStrings(connection, saveContainer.strings.get(step));
            saveDatesAndDateTimes(connection, saveContainer.localDateTimes.get(step), saveContainer.localDates.get(step));
            saveZonedDateTimes(connection, saveContainer.zonedDateTimes.get(step));
            saveTimes(connection, saveContainer.localTimes.get(step));
            saveLongs(connection, saveContainer.longs.get(step));
            saveDoubles(connection, saveContainer.doubles.get(step));
            saveIntegers(connection, saveContainer.integers.get(step));
            saveFloats(connection, saveContainer.floats.get(step));
            //blobs
            saveBlobs(connection, saveContainer.blobs.get(step));
            //array properties [NOTE arrays have old entries deleted first, for cases where a user reduced the amount of entries in the collection]
            saveArrayDates(connection, saveContainer.dateTimeArrays.get(step));
            saveArrayStrings(connection, saveContainer.stringArrays.get(step));
            saveArrayLongs(connection, saveContainer.longArrays.get(step));
            saveArrayDoubles(connection, saveContainer.doubleArrays.get(step));
            saveArrayIntegers(connection, saveContainer.integerArrays.get(step));
            saveArrayFloats(connection, saveContainer.floatArrays.get(step));
            //enums
            saveEnums(connection, saveContainer.enums.get(step));
            //objects and object arrays
            saveArrayObjects(connection, saveContainer.objectArrays.get(step));
            bindAndSaveInnerObjects(connection, saveContainer.objects.get(step));
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (!recursiveInnerCall)
                connection.close();
        }
        sequence = 0;
        for (final JdsEntity entity : entities) {
            if (entity instanceof JdsPostSaveListener) {
                ((JdsPostSaveListener) entity).onPostSave(new OnPostSaveEventArguments(sequence, entities.size()));
            }
            sequence++;
        }
    }

    /**
     * @param connection
     * @param overviews
     */
    private void saveOverviews(final Connection connection, final HashSet<JdsEntityOverview> overviews) {
        int record = 0;
        int recordTotal = overviews.size();
        try (PreparedStatement upsert = jdsDb.supportsStatements() ? connection.prepareCall(jdsDb.saveOverview()) : connection.prepareStatement(jdsDb.saveOverview())) {
            connection.setAutoCommit(false);
            for (JdsEntityOverview overview : overviews) {
                record++;
                //EntityGuid,ParentEntityGuid,DateCreated,DateModified,EntityId
                upsert.setString(1, overview.getEntityGuid());
                upsert.setTimestamp(2, Timestamp.valueOf(overview.getDateCreated()));
                upsert.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                upsert.setLong(4, overview.getEntityCode());
                upsert.addBatch();
                if (jdsDb.printOutput())
                    System.out.printf("Saving Overview [%s of %s]\n", record, recordTotal);
            }
            upsert.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }
    /**
     * @param connection
     * @param booleanProperties
     * @implNote Booleans are saved as integers behind the scenes
     */
    private void saveBooleans(final Connection connection, final Map<String, Map<Long, SimpleBooleanProperty>> booleanProperties) {
        int record = 0;
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,IntegerValue) VALUES(?,?,?)";
        try (PreparedStatement upsert = jdsDb.supportsStatements() ? connection.prepareCall(jdsDb.saveInteger()) : connection.prepareStatement(jdsDb.saveInteger());
             PreparedStatement log = connection.prepareStatement(logSql)) {
            connection.setAutoCommit(false);
            for (Map.Entry<String, Map<Long, SimpleBooleanProperty>> entry : booleanProperties.entrySet()) {
                record++;
                int innerRecord = 0;
                int innerRecordSize = entry.getValue().size();
                if (innerRecordSize == 0) continue;
                String entityGuid = entry.getKey();
                for (Map.Entry<Long, SimpleBooleanProperty> recordEntry : entry.getValue().entrySet()) {
                    innerRecord++;
                    long fieldId = recordEntry.getKey();
                    int value = recordEntry.getValue().get() ? 1 : 0;
                    upsert.setString(1, entityGuid);
                    upsert.setLong(2, fieldId);
                    upsert.setInt(3, value);
                    upsert.addBatch();
                    if (jdsDb.printOutput())
                        System.out.printf("Updating record [%s]. Boolean field [%s of %s]\n", record, innerRecord, innerRecordSize);
                    if (!jdsDb.logEdits()) continue;
                    log.setString(1, entityGuid);
                    log.setLong(2, fieldId);
                    log.setInt(3, value);
                    log.addBatch();
                }
            }
            upsert.executeBatch();
            log.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param integerProperties
     */
    private void saveIntegers(final Connection connection, final Map<String, Map<Long, SimpleIntegerProperty>> integerProperties) {
        int record = 0;
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,IntegerValue) VALUES(?,?,?)";
        try (PreparedStatement upsert = jdsDb.supportsStatements() ? connection.prepareCall(jdsDb.saveInteger()) : connection.prepareStatement(jdsDb.saveInteger());
             PreparedStatement log = connection.prepareStatement(logSql)) {
            connection.setAutoCommit(false);
            for (Map.Entry<String, Map<Long, SimpleIntegerProperty>> entry : integerProperties.entrySet()) {
                record++;
                int innerRecord = 0;
                int innerRecordSize = entry.getValue().size();
                if (innerRecordSize == 0) continue;
                String entityGuid = entry.getKey();
                for (Map.Entry<Long, SimpleIntegerProperty> recordEntry : entry.getValue().entrySet()) {
                    innerRecord++;
                    long fieldId = recordEntry.getKey();
                    int value = recordEntry.getValue().get();
                    upsert.setString(1, entityGuid);
                    upsert.setLong(2, fieldId);
                    upsert.setInt(3, value);
                    upsert.addBatch();
                    if (jdsDb.printOutput())
                        System.out.printf("Updating record [%s]. Integer field [%s of %s]\n", record, innerRecord, innerRecordSize);
                    if (!jdsDb.logEdits()) continue;
                    log.setString(1, entityGuid);
                    log.setLong(2, fieldId);
                    log.setInt(3, value);
                    log.addBatch();
                }
            }
            upsert.executeBatch();
            log.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param blobProperties
     */
    private void saveBlobs(final Connection connection, final Map<String, Map<Long, SimpleBlobProperty>> blobProperties) {
        int record = 0;
        try (PreparedStatement upsert = jdsDb.supportsStatements() ? connection.prepareCall(jdsDb.saveFloat()) : connection.prepareStatement(jdsDb.saveFloat());
             PreparedStatement log = connection.prepareStatement("INSERT INTO JdsStoreBlob(EntityGuid,FieldId,Value) VALUES(?,?,?)")) {
            connection.setAutoCommit(false);
            for (Map.Entry<String, Map<Long, SimpleBlobProperty>> entry : blobProperties.entrySet()) {
                record++;
                int innerRecord = 0;
                int innerRecordSize = entry.getValue().size();
                if (innerRecordSize == 0) continue;
                String entityGuid = entry.getKey();
                for (Map.Entry<Long, SimpleBlobProperty> recordEntry : entry.getValue().entrySet()) {
                    innerRecord++;
                    long fieldId = recordEntry.getKey();
                    byte[] value = recordEntry.getValue().get();
                    upsert.setString(1, entityGuid);
                    upsert.setLong(2, fieldId);
                    upsert.setBytes(3, value);
                    upsert.addBatch();
                    if (jdsDb.printOutput())
                        System.out.printf("Updating record [%s]. Blob field [%s of %s]\n", record, innerRecord, innerRecordSize);
                }
            }
            upsert.executeBatch();
            log.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param floatProperties
     */
    private void saveFloats(final Connection connection, final Map<String, Map<Long, SimpleFloatProperty>> floatProperties) {
        int record = 0;
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,FloatValue) VALUES(?,?,?)";
        try (PreparedStatement upsert = jdsDb.supportsStatements() ? connection.prepareCall(jdsDb.saveFloat()) : connection.prepareStatement(jdsDb.saveFloat());
             PreparedStatement log = connection.prepareStatement(logSql)) {
            connection.setAutoCommit(false);
            for (Map.Entry<String, Map<Long, SimpleFloatProperty>> entry : floatProperties.entrySet()) {
                record++;
                int innerRecord = 0;
                int innerRecordSize = entry.getValue().size();
                if (innerRecordSize == 0) continue;
                String entityGuid = entry.getKey();
                for (Map.Entry<Long, SimpleFloatProperty> recordEntry : entry.getValue().entrySet()) {
                    innerRecord++;
                    long fieldId = recordEntry.getKey();
                    float value = recordEntry.getValue().get();
                    upsert.setString(1, entityGuid);
                    upsert.setLong(2, fieldId);
                    upsert.setFloat(3, value);
                    upsert.addBatch();
                    if (jdsDb.printOutput())
                        System.out.printf("Updating record [%s]. Float field [%s of %s]\n", record, innerRecord, innerRecordSize);
                    if (!jdsDb.logEdits()) continue;
                    log.setString(1, entityGuid);
                    log.setLong(2, fieldId);
                    log.setFloat(3, value);
                    log.addBatch();
                }
            }
            upsert.executeBatch();
            log.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param doubleProperties
     */
    private void saveDoubles(final Connection connection, final Map<String, Map<Long, SimpleDoubleProperty>> doubleProperties) {
        int record = 0;
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,DoubleValue) VALUES(?,?,?)";
        try (PreparedStatement upsert = jdsDb.supportsStatements() ? connection.prepareCall(jdsDb.saveDouble()) : connection.prepareStatement(jdsDb.saveDouble());
             PreparedStatement log = connection.prepareStatement(logSql)) {
            connection.setAutoCommit(false);
            for (Map.Entry<String, Map<Long, SimpleDoubleProperty>> entry : doubleProperties.entrySet()) {
                record++;
                int innerRecord = 0;
                int innerRecordSize = entry.getValue().size();
                if (innerRecordSize == 0) continue;
                String entityGuid = entry.getKey();
                for (Map.Entry<Long, SimpleDoubleProperty> recordEntry : entry.getValue().entrySet()) {
                    innerRecord++;
                    long fieldId = recordEntry.getKey();
                    double value = recordEntry.getValue().get();
                    upsert.setString(1, entityGuid);
                    upsert.setLong(2, fieldId);
                    upsert.setDouble(3, value);
                    upsert.addBatch();
                    if (jdsDb.printOutput())
                        System.out.printf("Updating record [%s]. Double field [%s of %s]\n", record, innerRecord, innerRecordSize);
                    if (!jdsDb.logEdits()) continue;
                    log.setString(1, entityGuid);
                    log.setLong(2, fieldId);
                    log.setDouble(3, value);
                    log.addBatch();
                }
            }
            upsert.executeBatch();
            log.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param longProperties
     */
    private void saveLongs(final Connection connection, final Map<String, Map<Long, SimpleLongProperty>> longProperties) {
        int record = 0;
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,LongValue) VALUES(?,?,?)";
        try (PreparedStatement upsert = jdsDb.supportsStatements() ? connection.prepareCall(jdsDb.saveLong()) : connection.prepareStatement(jdsDb.saveLong());
             PreparedStatement log = connection.prepareStatement(logSql)) {
            connection.setAutoCommit(false);
            for (Map.Entry<String, Map<Long, SimpleLongProperty>> entry : longProperties.entrySet()) {
                record++;
                int innerRecord = 0;
                int innerRecordSize = entry.getValue().size();
                if (innerRecordSize == 0) continue;
                String entityGuid = entry.getKey();
                for (Map.Entry<Long, SimpleLongProperty> recordEntry : entry.getValue().entrySet()) {
                    innerRecord++;
                    long fieldId = recordEntry.getKey();
                    long value = recordEntry.getValue().get();
                    upsert.setString(1, entityGuid);
                    upsert.setLong(2, fieldId);
                    upsert.setLong(3, value);
                    upsert.addBatch();
                    if (jdsDb.printOutput())
                        System.out.printf("Updating record [%s]. Long field [%s of %s]\n", record, innerRecord, innerRecordSize);
                    if (!jdsDb.logEdits()) continue;
                    log.setString(1, entityGuid);
                    log.setLong(2, fieldId);
                    log.setLong(3, value);
                    log.addBatch();
                }
            }
            upsert.executeBatch();
            log.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param stringProperties
     */
    private void saveStrings(final Connection connection, final Map<String, Map<Long, SimpleStringProperty>> stringProperties) {
        int record = 0;
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,TextValue) VALUES(?,?,?)";
        try (PreparedStatement upsert = jdsDb.supportsStatements() ? connection.prepareCall(jdsDb.saveString()) : connection.prepareStatement(jdsDb.saveString());
             PreparedStatement log = connection.prepareStatement(logSql)) {
            connection.setAutoCommit(false);
            for (Map.Entry<String, Map<Long, SimpleStringProperty>> entry : stringProperties.entrySet()) {
                record++;
                int innerRecord = 0;
                int innerRecordSize = entry.getValue().size();
                if (innerRecordSize == 0) continue;
                String entityGuid = entry.getKey();
                for (Map.Entry<Long, SimpleStringProperty> recordEntry : entry.getValue().entrySet()) {
                    innerRecord++;
                    long fieldId = recordEntry.getKey();
                    String value = recordEntry.getValue().get();
                    upsert.setString(1, entityGuid);
                    upsert.setLong(2, fieldId);
                    upsert.setString(3, value);
                    upsert.addBatch();
                    if (jdsDb.printOutput())
                        System.out.printf("Updating record [%s]. Text field [%s of %s]\n", record, innerRecord, innerRecordSize);
                    if (!jdsDb.logEdits()) continue;
                    log.setString(1, entityGuid);
                    log.setLong(2, fieldId);
                    log.setString(3, value);
                    log.addBatch();
                }
            }
            upsert.executeBatch();
            log.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param localDateTimeProperties
     * @param localDateProperties
     */
    private void saveDatesAndDateTimes(final Connection connection, final Map<String, Map<Long, SimpleObjectProperty<Temporal>>> localDateTimeProperties, final Map<String, Map<Long, SimpleObjectProperty<Temporal>>> localDateProperties) {
        int record = 0;
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,DateTimeValue) VALUES(?,?,?)";
        try (PreparedStatement upsert = jdsDb.supportsStatements() ? connection.prepareCall(jdsDb.saveDateTime()) : connection.prepareStatement(jdsDb.saveDateTime());
             PreparedStatement log = connection.prepareStatement(logSql)) {
            connection.setAutoCommit(false);
            for (Map.Entry<String, Map<Long, SimpleObjectProperty<Temporal>>> entry : localDateTimeProperties.entrySet()) {
                record++;
                int innerRecord = 0;
                int innerRecordSize = entry.getValue().size();
                if (innerRecordSize == 0) continue;
                String entityGuid = entry.getKey();
                for (Map.Entry<Long, SimpleObjectProperty<Temporal>> recordEntry : entry.getValue().entrySet()) {
                    innerRecord++;
                    long fieldId = recordEntry.getKey();
                    LocalDateTime localDateTime = (LocalDateTime) recordEntry.getValue().get();
                    upsert.setString(1, entityGuid);
                    upsert.setLong(2, fieldId);
                    upsert.setTimestamp(3, Timestamp.valueOf(localDateTime));
                    upsert.addBatch();
                    if (jdsDb.printOutput())
                        System.out.printf("Updating record [%s]. LocalDateTime field [%s of %s]\n", record, innerRecord, innerRecordSize);
                    if (!jdsDb.logEdits()) continue;
                    log.setString(1, entityGuid);
                    log.setLong(2, fieldId);
                    log.setTimestamp(3, Timestamp.valueOf(localDateTime));
                    log.addBatch();
                }
            }
            for (Map.Entry<String, Map<Long, SimpleObjectProperty<Temporal>>> entry : localDateProperties.entrySet()) {
                record++;
                int innerRecord = 0;
                int innerRecordSize = entry.getValue().size();
                if (innerRecordSize == 0) continue;
                String entityGuid = entry.getKey();
                for (Map.Entry<Long, SimpleObjectProperty<Temporal>> recordEntry : entry.getValue().entrySet()) {
                    innerRecord++;
                    long fieldId = recordEntry.getKey();
                    LocalDate localDate = (LocalDate) recordEntry.getValue().get();
                    upsert.setString(1, entityGuid);
                    upsert.setLong(2, fieldId);
                    upsert.setTimestamp(3, Timestamp.valueOf(localDate.atStartOfDay()));
                    upsert.addBatch();
                    if (jdsDb.printOutput())
                        System.out.printf("Updating record [%s]. LocalDate field [%s of %s]\n", record, innerRecord, innerRecordSize);
                    if (!jdsDb.logEdits()) continue;
                    log.setString(1, entityGuid);
                    log.setLong(2, fieldId);
                    log.setTimestamp(3, Timestamp.valueOf(localDate.atStartOfDay()));
                    log.addBatch();
                }
            }
            upsert.executeBatch();
            log.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param localTimeProperties
     */
    private void saveTimes(final Connection connection, final Map<String, Map<Long, SimpleObjectProperty<Temporal>>> localTimeProperties) {
        int record = 0;
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,IntegerValue) VALUES(?,?,?)";
        try (PreparedStatement upsert = jdsDb.supportsStatements() ? connection.prepareCall(jdsDb.saveTime()) : connection.prepareStatement(jdsDb.saveTime());
             PreparedStatement log = connection.prepareStatement(logSql)) {
            connection.setAutoCommit(false);
            for (Map.Entry<String, Map<Long, SimpleObjectProperty<Temporal>>> entry : localTimeProperties.entrySet()) {
                record++;
                int innerRecord = 0;
                int innerRecordSize = entry.getValue().size();
                if (innerRecordSize == 0) continue;
                String entityGuid = entry.getKey();
                for (Map.Entry<Long, SimpleObjectProperty<Temporal>> recordEntry : entry.getValue().entrySet()) {
                    innerRecord++;
                    long fieldId = recordEntry.getKey();
                    LocalTime localTime = (LocalTime) recordEntry.getValue().get();
                    int secondOfDay = localTime.toSecondOfDay();
                    upsert.setString(1, entityGuid);
                    upsert.setLong(2, fieldId);
                    upsert.setInt(3, secondOfDay);
                    upsert.addBatch();
                    if (jdsDb.printOutput())
                        System.out.printf("Updating record [%s]. LocalTime field [%s of %s]\n", record, innerRecord, innerRecordSize);
                    if (!jdsDb.logEdits()) continue;
                    log.setString(1, entityGuid);
                    log.setLong(2, fieldId);
                    log.setInt(3, secondOfDay);
                    log.addBatch();
                }
            }
            upsert.executeBatch();
            log.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param zonedDateProperties
     */
    private void saveZonedDateTimes(final Connection connection, final Map<String, Map<Long, SimpleObjectProperty<Temporal>>> zonedDateProperties) {
        int record = 0;
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,LongValue) VALUES(?,?,?)";
        try (PreparedStatement upsert = jdsDb.supportsStatements() ? connection.prepareCall(jdsDb.saveZonedDateTime()) : connection.prepareStatement(jdsDb.saveZonedDateTime());
             PreparedStatement log = connection.prepareStatement(logSql)) {
            connection.setAutoCommit(false);
            for (Map.Entry<String, Map<Long, SimpleObjectProperty<Temporal>>> entry : zonedDateProperties.entrySet()) {
                record++;
                int innerRecord = 0;
                int innerRecordSize = entry.getValue().size();
                if (innerRecordSize == 0) continue;
                String entityGuid = entry.getKey();
                for (Map.Entry<Long, SimpleObjectProperty<Temporal>> recordEntry : entry.getValue().entrySet()) {
                    innerRecord++;
                    long fieldId = recordEntry.getKey();
                    ZonedDateTime zonedDateTime = (ZonedDateTime) recordEntry.getValue().get();
                    upsert.setString(1, entityGuid);
                    upsert.setLong(2, fieldId);
                    upsert.setLong(3, zonedDateTime.toEpochSecond());
                    upsert.addBatch();
                    if (jdsDb.printOutput())
                        System.out.printf("Updating record [%s]. ZonedDateTime field [%s of %s]\n", record, innerRecord, innerRecordSize);
                    if (!jdsDb.logEdits()) continue;
                    log.setString(1, entityGuid);
                    log.setLong(2, fieldId);
                    log.setLong(3, zonedDateTime.toEpochSecond());
                    log.addBatch();
                }
            }
            upsert.executeBatch();
            log.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * Save all dates in one go
     *
     * @param connection
     * @param dateTimeArrayProperties
     * @implNote Arrays have old entries deleted first. This for cases where a user may have reduced the amount of entries in the collection i.e [3,4,5]to[3,4]
     */
    private void saveArrayDates(final Connection connection, final Map<String, Map<Long, SimpleListProperty<LocalDateTime>>> dateTimeArrayProperties) {
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,DateTimeValue,Sequence) VALUES(?,?,?,?)";
        String deleteSql = "DELETE FROM JdsStoreDateTimeArray WHERE FieldId = ? AND EntityGuid = ?";
        String insertSql = "INSERT INTO JdsStoreDateTimeArray (Sequence,Value,FieldId,EntityGuid) VALUES (?,?,?,?)";
        try (PreparedStatement log = connection.prepareStatement(logSql);
             PreparedStatement delete = connection.prepareStatement(deleteSql);
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            connection.setAutoCommit(false);
            int record = 0;
            for (Map.Entry<String, Map<Long, SimpleListProperty<LocalDateTime>>> entry : dateTimeArrayProperties.entrySet()) {
                record++;
                String entityGuid = entry.getKey();
                final SimpleIntegerProperty index = new SimpleIntegerProperty(0);
                for (Map.Entry<Long, SimpleListProperty<LocalDateTime>> it : entry.getValue().entrySet()) {
                    Long fieldId = it.getKey();
                    int innerRecord = 0;
                    int innerTotal = it.getValue().get().size();
                    for (LocalDateTime value : it.getValue().get()) {
                        if (jdsDb.logEdits()) {
                            log.setString(1, entityGuid);
                            log.setLong(2, fieldId);
                            log.setTimestamp(3, Timestamp.valueOf(value));
                            log.setInt(4, index.get());
                            log.addBatch();
                        }
                        delete.setLong(1, fieldId);
                        delete.setString(2, entityGuid);
                        delete.addBatch();
                        //insert
                        insert.setInt(1, index.get());
                        insert.setTimestamp(2, Timestamp.valueOf(value));
                        insert.setLong(3, fieldId);
                        insert.setString(4, entityGuid);
                        insert.addBatch();
                        index.set(index.get() + 1);
                        if (jdsDb.printOutput())
                            System.out.printf("Inserting array record [%s]. DateTime field [%s of %s]\n", record, innerRecord, innerTotal);
                    }
                }
            }
            log.executeBatch();
            delete.executeBatch();
            insert.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param floatArrayProperties
     * @implNote Arrays have old entries deleted first. This for cases where a user may have reduced the amount of entries in the collection i.e [3,4,5]to[3,4]
     */
    private void saveArrayFloats(final Connection connection, final Map<String, Map<Long, SimpleListProperty<Float>>> floatArrayProperties) {
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,FloatValue,Sequence) VALUES(?,?,?,?)";
        String deleteSql = "DELETE FROM JdsStoreFloatArray WHERE FieldId = ? AND EntityGuid = ?";
        String insertSql = "INSERT INTO JdsStoreFloatArray (FieldId,EntityGuid,Value,Sequence) VALUES (?,?,?,?)";
        try (PreparedStatement log = connection.prepareStatement(logSql);
             PreparedStatement delete = connection.prepareStatement(deleteSql);
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            connection.setAutoCommit(false);
            int record = 0;
            for (Map.Entry<String, Map<Long, SimpleListProperty<Float>>> entry : floatArrayProperties.entrySet()) {
                record++;
                String entityGuid = entry.getKey();
                final SimpleIntegerProperty index = new SimpleIntegerProperty(0);
                for (Map.Entry<Long, SimpleListProperty<Float>> it : entry.getValue().entrySet()) {
                    Long fieldId = it.getKey();
                    int innerRecord = 0;
                    int innerTotal = it.getValue().get().size();
                    for (Float value : it.getValue().get()) {
                        if (jdsDb.logEdits()) {
                            log.setString(1, entityGuid);
                            log.setLong(2, fieldId);
                            log.setFloat(3, value);
                            log.setInt(4, index.get());
                            log.addBatch();
                        }
                        //delete
                        delete.setLong(1, fieldId);
                        delete.setString(2, entityGuid);
                        delete.addBatch();
                        //insert
                        insert.setInt(1, index.get());
                        insert.setFloat(2, value);
                        insert.setLong(3, fieldId);
                        insert.setString(4, entityGuid);
                        insert.addBatch();
                        index.set(index.get() + 1);
                        if (jdsDb.printOutput())
                            System.out.printf("Inserting array record [%s]. Float field [%s of %s]\n", record, innerRecord, innerTotal);

                    }
                }
            }
            log.executeBatch();
            delete.executeBatch();
            insert.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param integerArrayProperties
     * @implNote Arrays have old entries deleted first. This for cases where a user may have reduced the amount of entries in the collection i.e [3,4,5] to [3,4]
     */
    private void saveArrayIntegers(final Connection connection, final Map<String, Map<Long, SimpleListProperty<Integer>>> integerArrayProperties) {
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,IntegerValue,Sequence) VALUES(?,?,?,?)";
        String deleteSql = "DELETE FROM JdsStoreIntegerArray WHERE FieldId = ? AND EntityGuid = ?";
        String insertSql = "INSERT INTO JdsStoreIntegerArray (FieldId,EntityGuid,Sequence,Value) VALUES (?,?,?,?)";
        try (PreparedStatement log = connection.prepareStatement(logSql);
             PreparedStatement delete = connection.prepareStatement(deleteSql);
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            connection.setAutoCommit(false);
            int record = 0;
            for (Map.Entry<String, Map<Long, SimpleListProperty<Integer>>> entry : integerArrayProperties.entrySet()) {
                record++;
                String entityGuid = entry.getKey();
                final SimpleIntegerProperty index = new SimpleIntegerProperty(0);
                for (Map.Entry<Long, SimpleListProperty<Integer>> it : entry.getValue().entrySet()) {
                    Long fieldId = it.getKey();
                    int innerRecord = 0;
                    int innerTotal = it.getValue().get().size();
                    for (Integer value : it.getValue().get()) {
                        if (jdsDb.logEdits()) {
                            log.setString(1, entityGuid);
                            log.setLong(2, fieldId);
                            log.setInt(3, value);
                            log.setInt(4, index.get());
                            log.addBatch();
                        }
                        //delete
                        delete.setLong(1, fieldId);
                        delete.setString(2, entityGuid);
                        delete.addBatch();
                        //insert
                        insert.setInt(1, index.get());
                        insert.setInt(2, value);
                        insert.setLong(3, fieldId);
                        insert.setString(4, entityGuid);
                        insert.addBatch();
                        index.set(index.get() + 1);
                        if (jdsDb.printOutput())
                            System.out.printf("Inserting array record [%s]. Integer field [%s of %s]\n", record, innerRecord, innerTotal);
                    }
                }
            }
            log.executeBatch();
            delete.executeBatch();
            insert.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param doubleArrayProperties
     * @implNote Arrays have old entries deleted first. This for cases where a user may have reduced the amount of entries in the collection i.e [3,4,5]to[3,4]
     */
    private void saveArrayDoubles(final Connection connection, final Map<String, Map<Long, SimpleListProperty<Double>>> doubleArrayProperties) {
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,DoubleValue,Sequence) VALUES(?,?,?,?)";
        String deleteSql = "DELETE FROM JdsStoreDoubleArray WHERE FieldId = ? AND EntityGuid = ?";
        String insertSql = "INSERT INTO JdsStoreDoubleArray (FieldId,EntityGuid,Sequence,Value) VALUES (?,?,?,?)";
        try (PreparedStatement log = connection.prepareStatement(logSql);
             PreparedStatement delete = connection.prepareStatement(deleteSql);
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            connection.setAutoCommit(false);
            int record = 0;
            for (Map.Entry<String, Map<Long, SimpleListProperty<Double>>> entry : doubleArrayProperties.entrySet()) {
                record++;
                String entityGuid = entry.getKey();
                final SimpleIntegerProperty index = new SimpleIntegerProperty(0);
                for (Map.Entry<Long, SimpleListProperty<Double>> it : entry.getValue().entrySet()) {
                    Long fieldId = it.getKey();
                    int innerRecord = 0;
                    int innerTotal = it.getValue().get().size();
                    for (Double value : it.getValue().get()) {
                        if (jdsDb.logEdits()) {
                            log.setString(1, entityGuid);
                            log.setLong(2, fieldId);
                            log.setDouble(3, value);
                            log.setInt(4, index.get());
                            log.addBatch();
                        }
                        //delete
                        delete.setLong(1, fieldId);
                        delete.setString(2, entityGuid);
                        delete.addBatch();
                        //insert
                        insert.setInt(1, index.get());
                        insert.setDouble(2, value);
                        insert.setLong(3, fieldId);
                        insert.setString(4, entityGuid);
                        insert.addBatch();
                        index.set(index.get() + 1);
                        if (jdsDb.printOutput())
                            System.out.printf("Inserting array record [%s]. Double field [%s of %s]\n", record, innerRecord, innerTotal);
                    }
                }
            }
            log.executeBatch();
            delete.executeBatch();
            insert.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param longArrayProperties
     * @implNote Arrays have old entries deleted first. This for cases where a user may have reduced the amount of entries in the collection i.e [3,4,5]to[3,4]
     */
    private void saveArrayLongs(final Connection connection, final Map<String, Map<Long, SimpleListProperty<Long>>> longArrayProperties) {
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,LongValue,Sequence) VALUES(?,?,?,?)";
        String deleteSql = "DELETE FROM JdsStoreDoubleArray WHERE FieldId = ? AND EntityGuid = ?";
        String insertSql = "INSERT INTO JdsStoreDoubleArray (FieldId,EntityGuid,Sequence,Value) VALUES (?,?,?,?)";
        try (PreparedStatement log = connection.prepareStatement(logSql);
             PreparedStatement delete = connection.prepareStatement(deleteSql);
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            connection.setAutoCommit(false);
            int record = 0;
            for (Map.Entry<String, Map<Long, SimpleListProperty<Long>>> entry : longArrayProperties.entrySet()) {
                record++;
                String entityGuid = entry.getKey();
                final SimpleIntegerProperty index = new SimpleIntegerProperty(0);
                for (Map.Entry<Long, SimpleListProperty<Long>> it : entry.getValue().entrySet()) {
                    Long fieldId = it.getKey();
                    int innerRecord = 0;
                    int innerTotal = it.getValue().get().size();
                    for (Long value : it.getValue().get()) {
                        if (jdsDb.logEdits()) {
                            log.setString(1, entityGuid);
                            log.setLong(2, fieldId);
                            log.setLong(3, value);
                            log.setInt(4, index.get());
                            log.addBatch();
                        }
                        //delete
                        delete.setLong(1, fieldId);
                        delete.setString(2, entityGuid);
                        delete.addBatch();
                        //insert
                        insert.setInt(1, index.get());
                        insert.setLong(2, value);
                        insert.setLong(3, fieldId);
                        insert.setString(4, entityGuid);
                        insert.addBatch();
                        index.set(index.get() + 1);
                        if (jdsDb.printOutput())
                            System.out.printf("Inserting array record [%s]. Long field [%s of %s]\n", record, innerRecord, innerTotal);
                    }
                }
            }
            log.executeBatch();
            delete.executeBatch();
            insert.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param stringArrayProperties
     * @implNote Arrays have old entries deleted first. This for cases where a user may have reduced the amount of entries in the collection i.e [3,4,5]to[3,4]
     */
    private void saveArrayStrings(final Connection connection, final Map<String, Map<Long, SimpleListProperty<String>>> stringArrayProperties) {
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,TextValue,Sequence) VALUES(?,?,?,?)";
        String deleteSql = "DELETE FROM JdsStoreTextArray WHERE FieldId = ? AND EntityGuid = ?";
        String insertSql = "INSERT INTO JdsStoreTextArray (FieldId,EntityGuid,Sequence,Value) VALUES (?,?,?,?)";
        try (PreparedStatement log = connection.prepareStatement(logSql);
             PreparedStatement delete = connection.prepareStatement(deleteSql);
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            connection.setAutoCommit(false);
            int record = 0;
            for (Map.Entry<String, Map<Long, SimpleListProperty<String>>> entry : stringArrayProperties.entrySet()) {
                record++;
                String entityGuid = entry.getKey();
                final SimpleIntegerProperty index = new SimpleIntegerProperty(0);
                for (Map.Entry<Long, SimpleListProperty<String>> it : entry.getValue().entrySet()) {
                    Long fieldId = it.getKey();
                    int innerRecord = 0;
                    int innerTotal = it.getValue().get().size();
                    for (String value : it.getValue().get()) {
                        if (jdsDb.logEdits()) {
                            log.setString(1, entityGuid);
                            log.setLong(2, fieldId);
                            log.setString(3, value);
                            log.setInt(4, index.get());
                            log.addBatch();
                        }
                        //delete
                        delete.setLong(1, fieldId);
                        delete.setString(2, entityGuid);
                        delete.addBatch();
                        //insert
                        insert.setInt(1, index.get());
                        insert.setString(2, value);
                        insert.setLong(3, fieldId);
                        insert.setString(4, entityGuid);
                        insert.addBatch();
                        index.set(index.get() + 1);
                        if (jdsDb.printOutput())
                            System.out.printf("Inserting array record [%s]. String field [%s of %s]\n", record, innerRecord, innerTotal);
                    }
                }
            }
            log.executeBatch();
            delete.executeBatch();
            insert.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param enumStrings
     * @apiNote Enums are actually saved as index based integer arrays
     * @implNote Arrays have old entries deleted first. This for cases where a user may have reduced the amount of entries in the collection i.e [3,4,5]to[3,4]
     */
    private void saveEnums(final Connection connection, final Map<String, Map<JdsFieldEnum, SimpleListProperty<String>>> enumStrings) {
        int record = 0;
        int recordTotal = enumStrings.size();
        String logSql = "INSERT INTO JdsStoreOldFieldValues(EntityGuid,FieldId,IntegerValue,Sequence) VALUES(?,?,?,?)";
        String deleteSql = "DELETE FROM JdsStoreIntegerArray WHERE FieldId = ? AND EntityGuid = ?";
        String insertSql = "INSERT INTO JdsStoreIntegerArray (FieldId,EntityGuid,Sequence,Value) VALUES (?,?,?,?)";
        try (PreparedStatement log = connection.prepareStatement(logSql);
             PreparedStatement delete = connection.prepareStatement(deleteSql);
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            connection.setAutoCommit(false);
            for (Map.Entry<String, Map<JdsFieldEnum, SimpleListProperty<String>>> entry : enumStrings.entrySet()) {
                record++;
                String entityGuid = entry.getKey();
                for (Map.Entry<JdsFieldEnum, SimpleListProperty<String>> fieldEnums : entry.getValue().entrySet()) {
                    int sequence = 0;
                    JdsFieldEnum fieldId = fieldEnums.getKey();
                    ObservableList<String> textValues = fieldEnums.getValue().get();
                    if (textValues.size() == 0) continue;
                    for (String enumText : textValues) {
                        if (jdsDb.logEdits()) {
                            log.setString(1, entityGuid);
                            log.setLong(2, fieldId.getField().getId());
                            log.setInt(3, fieldId.getIndex(enumText));
                            log.setInt(4, sequence);
                            log.addBatch();
                        }
                        //delete
                        delete.setLong(1, fieldId.getField().getId());
                        delete.setString(2, entityGuid);
                        delete.addBatch();
                        //insert
                        insert.setLong(1, fieldId.getField().getId());
                        insert.setString(2, entityGuid);
                        insert.setInt(3, sequence);
                        insert.setInt(4, fieldId.getIndex(enumText));
                        insert.addBatch();
                        if (jdsDb.printOutput())
                            System.out.printf("Updating enum [%s]. Object field [%s of %s]\n", sequence, record, recordTotal);
                        sequence++;
                    }
                }
            }
            log.executeBatch();
            delete.executeBatch();
            insert.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param objectArrayProperties
     * @implNote Arrays have old entries deleted first. This for cases where a user may have reduced the amount of entries in the collection i.e [3,4,5]to[3,4]
     * @implNote For the love of Christ don't use parallel stream here
     */
    private void saveArrayObjects(final Connection connection, final Map<String, Map<Long, SimpleListProperty<JdsEntity>>> objectArrayProperties) throws Exception {
        if (objectArrayProperties.isEmpty()) return;
        final Collection<JdsEntity> jdsEntities = new ArrayList<>();
        final Collection<JdsParentEntityBinding> parentEntityBindings = new ArrayList<>();
        final Collection<JdsParentChildBinding> parentChildBindings = new ArrayList<>();
        final IntegerProperty record = new SimpleIntegerProperty(0);
        final BooleanProperty changesMade = new SimpleBooleanProperty(false);
        for (Map.Entry<String, Map<Long, SimpleListProperty<JdsEntity>>> serviceCodeEntities : objectArrayProperties.entrySet()) {
            String parentGuid = serviceCodeEntities.getKey();
            for (Map.Entry<Long, SimpleListProperty<JdsEntity>> serviceCodeEntity : serviceCodeEntities.getValue().entrySet()) {
                record.set(0);
                changesMade.set(false);
                serviceCodeEntity.getValue().stream().filter(jdsEntity -> jdsEntity != null).forEach(jdsEntity -> {
                    if (!changesMade.get()) {
                        //only clear if changes are made. else you wipe out old bindings regardless
                        changesMade.set(true);
                        JdsParentEntityBinding parentEntityBinding = new JdsParentEntityBinding();
                        parentEntityBinding.parentGuid = parentGuid;
                        parentEntityBinding.EntityId = serviceCodeEntity.getKey();
                        parentEntityBindings.add(parentEntityBinding);

                    }
                    JdsParentChildBinding parentChildBinding = new JdsParentChildBinding();
                    parentChildBinding.parentGuid = parentGuid;
                    parentChildBinding.childGuid = jdsEntity.getEntityGuid();
                    parentChildBindings.add(parentChildBinding);
                    jdsEntities.add(jdsEntity);
                    record.set(record.get() + 1);
                    System.out.printf("Binding array object %s\n", record.get());
                });
            }
        }
        //save children first
        new JdsSave(jdsDb, connection, -1, jdsEntities, true).call();

        //bind children below
        try (PreparedStatement clearOldBindings = connection.prepareStatement("DELETE FROM JdsStoreEntityBinding WHERE ParentEntityGuid = ? AND ChildEntityId = ?");
             PreparedStatement writeNewBindings = connection.prepareStatement("INSERT INTO JdsStoreEntityBinding(ParentEntityGuid,ChildEntityGuid,ChildEntityId) Values(?,?,?)")) {
            connection.setAutoCommit(false);
            for (JdsParentEntityBinding parentEntityBinding : parentEntityBindings) {
                clearOldBindings.setString(1, parentEntityBinding.parentGuid);
                clearOldBindings.setLong(2, parentEntityBinding.EntityId);
                clearOldBindings.addBatch();
            }
            for (JdsEntity jdsEntity : jdsEntities) {
                writeNewBindings.setString(1, getParent(parentChildBindings, jdsEntity.getEntityGuid()));
                writeNewBindings.setString(2, jdsEntity.getEntityGuid());
                writeNewBindings.setLong(3, jdsEntity.getEntityCode());
                writeNewBindings.addBatch();
            }
            int[] res2 = clearOldBindings.executeBatch();
            int[] res3 = writeNewBindings.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param connection
     * @param objectProperties
     * @implNote For the love of Christ don't use parallel stream here
     */
    private void bindAndSaveInnerObjects(final Connection connection, final Map<String, Map<Long, SimpleObjectProperty<JdsEntity>>> objectProperties) throws Exception {
        if (objectProperties.isEmpty()) return;//prevent stack overflow :)
        final IntegerProperty record = new SimpleIntegerProperty(0);
        final BooleanProperty changesMade = new SimpleBooleanProperty(false);
        final Collection<JdsParentEntityBinding> parentEntityBindings = new ArrayList<>();
        final Collection<JdsParentChildBinding> parentChildBindings = new ArrayList<>();
        final Collection<JdsEntity> jdsEntities = new ArrayList<>();
        for (Map.Entry<String, Map<Long, SimpleObjectProperty<JdsEntity>>> entry : objectProperties.entrySet()) {
            String parentGuid = entry.getKey();
            for (Map.Entry<Long, SimpleObjectProperty<JdsEntity>> recordEntry : entry.getValue().entrySet()) {
                record.set(0);
                JdsEntity jdsEntity = recordEntry.getValue().get();
                changesMade.set(false);
                if (jdsEntity != null) {
                    if (!changesMade.get()) {
                        changesMade.set(true);
                        JdsParentEntityBinding parentEntityBinding = new JdsParentEntityBinding();
                        parentEntityBinding.parentGuid = parentGuid;
                        parentEntityBinding.EntityId = recordEntry.getKey();
                        parentEntityBindings.add(parentEntityBinding);
                    }
                    jdsEntities.add(jdsEntity);
                    JdsParentChildBinding parentChildBinding = new JdsParentChildBinding();
                    parentChildBinding.parentGuid = parentGuid;
                    parentChildBinding.childGuid = jdsEntity.getEntityGuid();
                    parentChildBindings.add(parentChildBinding);
                    record.set(record.get() + 1);
                    System.out.printf("Binding object %s\n", record.get());
                }
            }
        }
        //save children first
        new JdsSave(jdsDb, connection, -1, jdsEntities, true).call();

        //bind children below
        try (PreparedStatement clearOldBindings = connection.prepareStatement("DELETE FROM JdsStoreEntityBinding WHERE ParentEntityGuid = ? AND ChildEntityId = ?");
             PreparedStatement writeNewBindings = connection.prepareStatement("INSERT INTO JdsStoreEntityBinding(ParentEntityGuid,ChildEntityGuid,ChildEntityId) Values(?,?,?)")) {
            connection.setAutoCommit(false);
            for (JdsParentEntityBinding parentEntityBinding : parentEntityBindings) {
                clearOldBindings.setString(1, parentEntityBinding.parentGuid);
                clearOldBindings.setLong(2, parentEntityBinding.EntityId);
                clearOldBindings.addBatch();
            }
            for (JdsEntity jdsEntity : jdsEntities) {
                writeNewBindings.setString(1, getParent(parentChildBindings, jdsEntity.getEntityGuid()));
                writeNewBindings.setString(2, jdsEntity.getEntityGuid());
                writeNewBindings.setLong(3, jdsEntity.getEntityCode());
                writeNewBindings.addBatch();
            }
            int[] res2 = clearOldBindings.executeBatch();
            int[] res3 = writeNewBindings.executeBatch();
            connection.commit();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * @param jdsParentChildBindings
     * @param childGuid
     * @return
     */
    private String getParent(final Collection<JdsParentChildBinding> jdsParentChildBindings, final String childGuid) {
        Optional<JdsParentChildBinding> any = jdsParentChildBindings.stream().filter(parentChildBinding -> parentChildBinding.childGuid.equals(childGuid)).findAny();
        return any.isPresent() ? any.get().parentGuid : "";
    }

    /**
     * @param jdsDb
     * @param batchSize
     * @param entities
     * @deprecated please refer to <a href="https://github.com/SubiyaCryolite/Jenesis-Data-Store"> the readme</a> for the most up to date CRUD approach
     */
    public static void save(final JdsDb jdsDb, final int batchSize, final Collection<? extends JdsEntity> entities) throws Exception {
        new JdsSave(jdsDb, batchSize, entities).call();
    }

    /**
     * @param jdsDb
     * @param batchSize
     * @param entities
     * @deprecated please refer to <a href="https://github.com/SubiyaCryolite/Jenesis-Data-Store"> the readme</a> for the most up to date CRUD approach
     */
    public static void save(final JdsDb jdsDb, final int batchSize, final JdsEntity... entities) throws Exception {
        save(jdsDb, batchSize, Arrays.asList(entities));
    }
}
