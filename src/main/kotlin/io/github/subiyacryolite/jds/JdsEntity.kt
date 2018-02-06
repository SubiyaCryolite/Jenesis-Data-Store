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

import io.github.subiyacryolite.jds.JdsExtensions.toLocalTimeSqlFormat
import io.github.subiyacryolite.jds.JdsExtensions.toZonedDateTime
import io.github.subiyacryolite.jds.annotations.JdsEntityAnnotation
import io.github.subiyacryolite.jds.embedded.*
import io.github.subiyacryolite.jds.enums.JdsFieldType
import javafx.beans.property.BlobProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.WritableValue
import java.io.Externalizable
import java.io.IOException
import java.io.ObjectInput
import java.io.ObjectOutput
import java.math.BigDecimal
import java.sql.Connection
import java.sql.Timestamp
import java.time.*
import java.time.temporal.Temporal
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashSet
import kotlin.coroutines.experimental.buildSequence

/**
 * This class allows for all mapping operations in JDS, it also uses
 * [IJdsOverview] to store overview data
 */
abstract class JdsEntity : IJdsEntity {
    override var overview: IJdsOverview = JdsOverview()
    //time constructs
    private val localDateTimeProperties: HashMap<Long, ObjectProperty<Temporal>> = HashMap()
    private val zonedDateTimeProperties: HashMap<Long, ObjectProperty<Temporal>> = HashMap()
    private val localDateProperties: HashMap<Long, ObjectProperty<Temporal>> = HashMap()
    private val localTimeProperties: HashMap<Long, ObjectProperty<Temporal>> = HashMap()
    private val monthDayProperties: HashMap<Long, ObjectProperty<MonthDay>> = HashMap()
    private val yearMonthProperties: HashMap<Long, ObjectProperty<Temporal>> = HashMap()
    private val periodProperties: HashMap<Long, ObjectProperty<Period>> = HashMap()
    private val durationProperties: HashMap<Long, ObjectProperty<Duration>> = HashMap()
    //strings
    private val stringProperties: HashMap<Long, StringProperty> = HashMap()
    //numeric
    private val floatProperties: HashMap<Long, WritableValue<Float>> = HashMap()
    private val doubleProperties: HashMap<Long, WritableValue<Double>> = HashMap()
    private val booleanProperties: HashMap<Long, WritableValue<Boolean>> = HashMap()
    private val longProperties: HashMap<Long, WritableValue<Long>> = HashMap()
    private val integerProperties: HashMap<Long, WritableValue<Int>> = HashMap()
    //arrays
    private val objectArrayProperties: HashMap<JdsFieldEntity<*>, MutableCollection<JdsEntity>> = HashMap()
    private val stringArrayProperties: HashMap<Long, MutableCollection<String>> = HashMap()
    private val dateTimeArrayProperties: HashMap<Long, MutableCollection<LocalDateTime>> = HashMap()
    private val floatArrayProperties: HashMap<Long, MutableCollection<Float>> = HashMap()
    private val doubleArrayProperties: HashMap<Long, MutableCollection<Double>> = HashMap()
    private val longArrayProperties: HashMap<Long, MutableCollection<Long>> = HashMap()
    private val integerArrayProperties: HashMap<Long, MutableCollection<Int>> = HashMap()
    //enumProperties
    private val enumProperties: HashMap<JdsFieldEnum<*>, ObjectProperty<Enum<*>>> = HashMap()
    private val enumCollectionProperties: HashMap<JdsFieldEnum<*>, MutableCollection<Enum<*>>> = HashMap()
    //objects
    private val objectProperties: HashMap<JdsFieldEntity<*>, ObjectProperty<JdsEntity>> = HashMap()
    //blobs
    private val blobProperties: HashMap<Long, BlobProperty> = HashMap()


    init {
        val classHasAnnotation = javaClass.isAnnotationPresent(JdsEntityAnnotation::class.java)
        val superclassHasAnnotation = javaClass.superclass.isAnnotationPresent(JdsEntityAnnotation::class.java)
        if (classHasAnnotation || superclassHasAnnotation) {
            val entityAnnotation = when (classHasAnnotation) {
                true -> javaClass.getAnnotation(JdsEntityAnnotation::class.java)
                false -> javaClass.superclass.getAnnotation(JdsEntityAnnotation::class.java)
            }
            overview.entityId = entityAnnotation.entityId
            overview.version = entityAnnotation.version
        } else {
            throw RuntimeException("You must annotate the class [" + javaClass.canonicalName + "] with [" + JdsEntityAnnotation::class.java + "]")
        }
    }

    /**
     * @param field
     * @param property
     */
    protected fun map(field: JdsField, property: BlobProperty) {
        if (field.type != JdsFieldType.BLOB)
            throw RuntimeException("Please assign the correct type to field [$field]")
        mapField(overview.entityId, field.id)
        blobProperties[field.id] = property
    }

    protected fun mapMonthDay(field: JdsField, property: ObjectProperty<MonthDay>) {
        if (field.type != JdsFieldType.MONTH_DAY)
            throw RuntimeException("Please assign the correct type to field [$field]")
        mapField(overview.entityId, field.id)
        monthDayProperties[field.id] = property
    }

    protected fun mapPeriod(field: JdsField, property: ObjectProperty<Period>) {
        if (field.type != JdsFieldType.PERIOD)
            throw RuntimeException("Please assign the correct type to field [$field]")
        mapField(overview.entityId, field.id)
        periodProperties[field.id] = property
    }

    protected fun mapDuration(field: JdsField, property: ObjectProperty<Duration>) {
        if (field.type != JdsFieldType.DURATION)
            throw RuntimeException("Please assign the correct type to field [$field]")
        mapField(overview.entityId, field.id)
        durationProperties[field.id] = property
    }

    /**
     * @param field
     * @param temporalProperty
     */
    protected fun map(field: JdsField, temporalProperty: ObjectProperty<out Temporal>) {
        val temporal = temporalProperty.get()
        when (temporal) {
            is LocalDateTime -> {
                if (field.type != JdsFieldType.DATE_TIME)
                    throw RuntimeException("Please assign the correct type to field [$field]")
                mapField(overview.entityId, field.id)
                localDateTimeProperties.put(field.id, temporalProperty as ObjectProperty<Temporal>)
            }
            is ZonedDateTime -> {
                if (field.type != JdsFieldType.ZONED_DATE_TIME)
                    throw RuntimeException("Please assign the correct type to field [$field]")
                mapField(overview.entityId, field.id)
                zonedDateTimeProperties.put(field.id, temporalProperty as ObjectProperty<Temporal>)
            }
            is LocalDate -> {
                if (field.type != JdsFieldType.DATE)
                    throw RuntimeException("Please assign the correct type to field [$field]")
                mapField(overview.entityId, field.id)
                localDateProperties.put(field.id, temporalProperty as ObjectProperty<Temporal>)
            }
            is LocalTime -> {
                if (field.type != JdsFieldType.TIME)
                    throw RuntimeException("Please assign the correct type to field [$field]")
                mapField(overview.entityId, field.id)
                localTimeProperties.put(field.id, temporalProperty as ObjectProperty<Temporal>)
            }
            is YearMonth -> {
                if (field.type != JdsFieldType.YEAR_MONTH)
                    throw RuntimeException("Please assign the correct type to field [$field]")
                mapField(overview.entityId, field.id)
                yearMonthProperties.put(field.id, temporalProperty as ObjectProperty<Temporal>)

            }
        }
    }

    /**
     * @param field
     * @param property
     */
    protected fun map(field: JdsField, property: StringProperty) {
        if (field.type != JdsFieldType.STRING)
            throw RuntimeException("Please assign the correct type to field [$field]")
        mapField(overview.entityId, field.id)
        stringProperties[field.id] = property

    }

    /**
     * @param field
     * @param property
     */
    protected fun map(field: JdsField, property: WritableValue<*>) {
        if (field.type != JdsFieldType.DOUBLE && field.type != JdsFieldType.LONG && field.type != JdsFieldType.INT && field.type != JdsFieldType.FLOAT && field.type != JdsFieldType.BOOLEAN)
            throw RuntimeException("Please assign the correct type to field [$field]")
        mapField(overview.entityId, field.id)
        when (field.type) {
            JdsFieldType.DOUBLE -> doubleProperties[field.id] = property as WritableValue<Double>
            JdsFieldType.LONG -> longProperties[field.id] = property as WritableValue<Long>
            JdsFieldType.INT -> integerProperties[field.id] = property as WritableValue<Int>
            JdsFieldType.FLOAT -> floatProperties[field.id] = property as WritableValue<Float>
            JdsFieldType.BOOLEAN -> booleanProperties[field.id] = property as WritableValue<Boolean>
        }
    }


    /**
     * @param field
     * @param properties
     */
    protected fun mapStrings(field: JdsField, properties: MutableCollection<String>) {
        if (field.type != JdsFieldType.STRING_COLLECTION)
            throw RuntimeException("Please assign the correct type to field [$field]")
        mapField(overview.entityId, field.id)
        stringArrayProperties[field.id] = properties
    }

    /**
     * @param field
     * @param properties
     */
    protected fun mapDateTimes(field: JdsField, properties: MutableCollection<LocalDateTime>) {
        if (field.type != JdsFieldType.DATE_TIME_COLLECTION)
            throw RuntimeException("Please assign the correct type to field [$field]")
        mapField(overview.entityId, field.id)
        dateTimeArrayProperties[field.id] = properties
    }

    /**
     * @param field
     * @param properties
     */
    protected fun mapFloats(field: JdsField, properties: MutableCollection<Float>) {
        if (field.type != JdsFieldType.FLOAT_COLLECTION)
            throw RuntimeException("Please assign the correct type to field [$field]")
        mapField(overview.entityId, field.id)
        floatArrayProperties[field.id] = properties
    }

    /**
     * @param field
     * @param properties
     */
    protected fun mapIntegers(field: JdsField, properties: MutableCollection<Int>) {
        if (field.type != JdsFieldType.INT_COLLECTION)
            throw RuntimeException("Please assign the correct type to field [$field]")
        mapField(overview.entityId, field.id)
        integerArrayProperties[field.id] = properties
    }

    /**
     * @param field
     * @param properties
     */
    protected fun mapDoubles(field: JdsField, properties: MutableCollection<Double>) {
        if (field.type != JdsFieldType.DOUBLE_COLLECTION)
            throw RuntimeException("Please assign the correct type to field [$field]")
        mapField(overview.entityId, field.id)
        doubleArrayProperties[field.id] = properties
    }

    /**
     * @param field
     * @param properties
     */
    protected fun mapLongs(field: JdsField, properties: MutableCollection<Long>) {
        if (field.type != JdsFieldType.LONG_COLLECTION)
            throw RuntimeException("Please assign the correct type to field [$field]")
        mapField(overview.entityId, field.id)
        longArrayProperties[field.id] = properties
    }

    /**
     * @param fieldEnum
     * @param property
     */
    protected fun map(fieldEnum: JdsFieldEnum<*>, property: ObjectProperty<out Enum<*>>) {
        if (fieldEnum.field.type != JdsFieldType.ENUM)
            throw RuntimeException("Please assign the correct type to field [$fieldEnum]")
        mapEnums(overview.entityId, fieldEnum.field.id)
        mapField(overview.entityId, fieldEnum.field.id)
        enumProperties.put(fieldEnum, property as ObjectProperty<Enum<*>>)
    }

    /**
     * @param fieldEnum
     * @param properties
     */
    protected fun mapEnums(fieldEnum: JdsFieldEnum<*>, properties: MutableCollection<out Enum<*>>) {
        if (fieldEnum.field.type != JdsFieldType.ENUM_COLLECTION)
            throw RuntimeException("Please assign the correct type to field [$fieldEnum]")
        mapEnums(overview.entityId, fieldEnum.field.id)
        mapField(overview.entityId, fieldEnum.field.id)
        enumCollectionProperties.put(fieldEnum, properties as MutableCollection<Enum<*>>)

    }

    /**
     * @param entity
     * @param property
     */
    protected fun <T : IJdsEntity> map(fieldEntity: JdsFieldEntity<out T>, property: ObjectProperty<out T>) {
        if (fieldEntity.fieldEntity.type != JdsFieldType.ENTITY)
            throw RuntimeException("Please assign the correct type to field [$fieldEntity]")
        if (!objectArrayProperties.containsKey(fieldEntity) && !objectProperties.containsKey(fieldEntity)) {
            objectProperties.put(fieldEntity, property as ObjectProperty<JdsEntity>)
            mapField(overview.entityId, fieldEntity.fieldEntity.id)
        } else {
            throw RuntimeException("You can only bind a class to one property. This class is already bound to one object or object array")
        }
    }

    /**
     * @param fieldEntity
     * @param properties
     */
    protected fun <T : IJdsEntity> map(fieldEntity: JdsFieldEntity<out T>, properties: MutableCollection<out T>) {
        if (fieldEntity.fieldEntity.type != JdsFieldType.ENTITY_COLLECTION)
            throw RuntimeException("Please supply a valid type for JdsFieldEntity")
        if (!objectArrayProperties.containsKey(fieldEntity)) {
            objectArrayProperties.put(fieldEntity, properties as MutableCollection<JdsEntity>)
            mapField(overview.entityId, fieldEntity.fieldEntity.id)
        } else {
            throw RuntimeException("You can only bind a class to one property. This class is already bound to one object or object array")
        }
    }

    /**
     * Copy values from matching fieldIds found in both objects
     *
     * @param source The entity to copy values from
     * @param <T>    A valid JDSEntity
    </T> */
    fun <T : JdsEntity> copy(source: T) {
        copyArrayValues(source)
        copyPropertyValues(source)
        copyOverviewValues(source)
        copyEnumAndEnumArrayValues(source)
        copyObjectAndObjectArrayValues(source)
    }

    /**
     * Copy all header overview information
     *
     * @param source The entity to copy values from
     * @param <T>    A valid JDSEntity
    </T> */
    private fun <T : IJdsEntity> copyOverviewValues(source: T) {
        overview.uuid = source.overview.uuid
        overview.live = source.overview.live
        overview.version = source.overview.version
        overview.parentUuid = source.overview.parentUuid
    }

    /**
     * Copy all property values
     *
     * @param source The entity to copy values from
     * @param <T>    A valid JDSEntity A valid JDSEntity
    </T> */
    private fun <T : JdsEntity> copyPropertyValues(source: T) {
        val dest = this
        source.booleanProperties.entries.forEach {
            if (dest.booleanProperties.containsKey(it.key)) {
                dest.booleanProperties[it.key] = it.value
            }
        }
        source.localDateTimeProperties.entries.forEach {
            if (dest.localDateTimeProperties.containsKey(it.key)) {
                dest.localDateTimeProperties[it.key]?.set(it.value.get())
            }
        }
        source.zonedDateTimeProperties.entries.forEach {
            if (dest.zonedDateTimeProperties.containsKey(it.key)) {
                dest.zonedDateTimeProperties[it.key]?.set(it.value.get())
            }
        }
        source.localTimeProperties.entries.forEach {
            if (dest.localTimeProperties.containsKey(it.key)) {
                dest.localTimeProperties[it.key]?.set(it.value.get())
            }
        }
        source.localDateProperties.entries.forEach {
            if (dest.localDateProperties.containsKey(it.key)) {
                dest.localDateProperties[it.key]?.set(it.value.get())
            }
        }
        source.stringProperties.entries.forEach {
            if (dest.stringProperties.containsKey(it.key)) {
                dest.stringProperties[it.key]?.set(it.value.get())
            }
        }
        source.floatProperties.entries.forEach {
            if (dest.floatProperties.containsKey(it.key)) {
                dest.floatProperties[it.key] = it.value
            }
        }
        source.doubleProperties.entries.forEach {
            if (dest.doubleProperties.containsKey(it.key)) {
                dest.doubleProperties[it.key] = it.value
            }
        }
        source.longProperties.entries.forEach {
            if (dest.longProperties.containsKey(it.key)) {
                dest.longProperties[it.key] = it.value
            }
        }
        source.integerProperties.entries.forEach {
            if (dest.integerProperties.containsKey(it.key)) {
                dest.integerProperties[it.key] = it.value
            }
        }
        source.blobProperties.entries.forEach {
            if (dest.blobProperties.containsKey(it.key)) {
                dest.blobProperties[it.key]?.set(it.value.get()!!)
            }
        }
        source.durationProperties.entries.forEach {
            if (dest.durationProperties.containsKey(it.key)) {
                dest.durationProperties[it.key]?.set(it.value.get())
            }
        }
        source.periodProperties.entries.forEach {
            if (dest.periodProperties.containsKey(it.key)) {
                dest.periodProperties[it.key]?.set(it.value.get())
            }
        }
        source.yearMonthProperties.entries.forEach {
            if (dest.yearMonthProperties.containsKey(it.key)) {
                dest.yearMonthProperties[it.key]?.set(it.value.get())
            }
        }
        source.monthDayProperties.entries.forEach {
            if (dest.monthDayProperties.containsKey(it.key)) {
                dest.monthDayProperties[it.key]?.set(it.value.get()!!)
            }
        }
    }

    /**
     * Copy all property array values
     *
     * @param source The entity to copy values from
     * @param <T>    A valid JDSEntity A valid JDSEntity
    </T> */
    private fun <T : JdsEntity> copyArrayValues(source: T) {
        val dest = this
        source.stringArrayProperties.entries.forEach {
            if (dest.stringArrayProperties.containsKey(it.key)) {
                val entry = dest.stringArrayProperties[it.key]
                entry?.clear()
                entry?.addAll(it.value)
            }
        }
        source.dateTimeArrayProperties.entries.forEach {
            if (dest.dateTimeArrayProperties.containsKey(it.key)) {
                val entry = dest.dateTimeArrayProperties[it.key]
                entry?.clear()
                entry?.addAll(it.value)
            }
        }
        source.floatArrayProperties.entries.forEach {
            if (dest.floatArrayProperties.containsKey(it.key)) {
                val entry = dest.floatArrayProperties[it.key]
                entry?.clear()
                entry?.addAll(it.value)
            }
        }
        source.doubleArrayProperties.entries.forEach {
            if (dest.doubleArrayProperties.containsKey(it.key)) {
                val entry = dest.doubleArrayProperties[it.key]
                entry?.clear()
                entry?.addAll(it.value)
            }
        }
        source.longArrayProperties.entries.forEach {
            if (dest.longArrayProperties.containsKey(it.key)) {
                val entry = dest.longArrayProperties[it.key]
                entry?.clear()
                entry?.addAll(it.value)
            }
        }
        source.integerArrayProperties.entries.forEach {
            if (dest.integerArrayProperties.containsKey(it.key)) {
                val entry = dest.integerArrayProperties[it.key]
                entry?.clear()
                entry?.addAll(it.value)
            }
        }
    }

    /**
     * Copy over object and object array values
     *
     * @param source The entity to copy values from
     * @param <T>    A valid JDSEntity
    </T> */
    private fun <T : JdsEntity> copyObjectAndObjectArrayValues(source: T) {
        val dest = this
        source.objectProperties.entries.forEach {
            if (dest.objectProperties.containsKey(it.key)) {
                dest.objectProperties[it.key]?.set(it.value.get())
            }
        }

        source.objectArrayProperties.entries.forEach {
            if (dest.objectArrayProperties.containsKey(it.key)) {
                val entry = dest.objectArrayProperties[it.key]
                entry?.clear()
                entry?.addAll(it.value)
            }
        }
    }

    /**
     * Copy over object enum values
     *
     * @param source The entity to copy values from
     * @param <T> A valid JDSEntity
    </T> */
    private fun <T : JdsEntity> copyEnumAndEnumArrayValues(source: T) {
        val dest = this
        source.enumCollectionProperties.entries.forEach {
            if (dest.enumCollectionProperties.containsKey(it.key)) {
                val dstEntry = dest.enumCollectionProperties[it.key]
                dstEntry?.clear()
                val it = it.value.iterator()
                while (it.hasNext()) {
                    val nxt = it.next()
                    dstEntry?.add(nxt)
                }
            }
        }
        source.enumProperties.entries.forEach {
            if (dest.enumProperties.containsKey(it.key)) {
                val dstEntry = dest.enumProperties[it.key]
                dstEntry?.value = it.value.value
            }
        }
    }

    @Throws(IOException::class)
    override fun writeExternal(objectOutputStream: ObjectOutput) {
        //fieldEntity and enum maps
        objectOutputStream.writeObject(overview)
        //objects
        objectOutputStream.writeObject(serializeObject(objectProperties))
        //time constructs
        objectOutputStream.writeObject(serializeTemporal(localDateTimeProperties))
        objectOutputStream.writeObject(serializeTemporal(zonedDateTimeProperties))
        objectOutputStream.writeObject(serializeTemporal(localDateProperties))
        objectOutputStream.writeObject(serializeTemporal(localTimeProperties))
        objectOutputStream.writeObject(monthDayProperties)
        objectOutputStream.writeObject(yearMonthProperties)
        objectOutputStream.writeObject(periodProperties)
        objectOutputStream.writeObject(durationProperties)
        //strings
        objectOutputStream.writeObject(serializableString(stringProperties))
        //numeric
        objectOutputStream.writeObject(floatProperties)
        objectOutputStream.writeObject(doubleProperties)
        objectOutputStream.writeObject(booleanProperties)
        objectOutputStream.writeObject(longProperties)
        objectOutputStream.writeObject(integerProperties)
        //blobs
        objectOutputStream.writeObject(serializeBlobs(blobProperties))
        //arrays
        objectOutputStream.writeObject(serializeObjects(objectArrayProperties))
        objectOutputStream.writeObject(serializeStrings(stringArrayProperties))
        objectOutputStream.writeObject(serializeDateTimes(dateTimeArrayProperties))
        objectOutputStream.writeObject(serializeFloats(floatArrayProperties))
        objectOutputStream.writeObject(serializeDoubles(doubleArrayProperties))
        objectOutputStream.writeObject(serializeLongs(longArrayProperties))
        objectOutputStream.writeObject(serializeIntegers(integerArrayProperties))
        //enumProperties
        objectOutputStream.writeObject(serializeEnums(enumProperties))
        objectOutputStream.writeObject(serializeEnumCollections(enumCollectionProperties))
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    override fun readExternal(objectInputStream: ObjectInput) {
        //fieldEntity and enum maps
        overview = objectInputStream.readObject() as JdsOverview
        //objects
        putObject(objectProperties, objectInputStream.readObject() as Map<JdsFieldEntity<*>, JdsEntity>)
        //time constructs
        putTemporal(localDateTimeProperties, objectInputStream.readObject() as Map<Long, Temporal>)
        putTemporal(zonedDateTimeProperties, objectInputStream.readObject() as Map<Long, Temporal>)
        putTemporal(localDateProperties, objectInputStream.readObject() as Map<Long, Temporal>)
        putTemporal(localTimeProperties, objectInputStream.readObject() as Map<Long, Temporal>)
        putMonthDays(monthDayProperties, objectInputStream.readObject() as Map<Long, MonthDay>)
        putYearMonths(yearMonthProperties, objectInputStream.readObject() as Map<Long, Temporal>)
        putPeriods(periodProperties, objectInputStream.readObject() as Map<Long, Period>)
        putDurations(durationProperties, objectInputStream.readObject() as Map<Long, Duration>)
        //string
        putString(stringProperties, objectInputStream.readObject() as Map<Long, String>)
        //numeric
        putFloat(floatProperties, objectInputStream.readObject() as Map<Long, WritableValue<Float>>)
        putDouble(doubleProperties, objectInputStream.readObject() as Map<Long, WritableValue<Double>>)
        putBoolean(booleanProperties, objectInputStream.readObject() as Map<Long, WritableValue<Boolean>>)
        putLong(longProperties, objectInputStream.readObject() as Map<Long, WritableValue<Long>>)
        putInteger(integerProperties, objectInputStream.readObject() as Map<Long, WritableValue<Int>>)
        //blobs
        putBlobs(blobProperties, objectInputStream.readObject() as Map<Long, BlobProperty>)
        //arrays
        putObjects(objectArrayProperties, objectInputStream.readObject() as Map<JdsFieldEntity<*>, List<JdsEntity>>)
        putStrings(stringArrayProperties, objectInputStream.readObject() as Map<Long, List<String>>)
        putDateTimes(dateTimeArrayProperties, objectInputStream.readObject() as Map<Long, List<LocalDateTime>>)
        putFloats(floatArrayProperties, objectInputStream.readObject() as Map<Long, List<Float>>)
        putDoubles(doubleArrayProperties, objectInputStream.readObject() as Map<Long, List<Double>>)
        putLongs(longArrayProperties, objectInputStream.readObject() as Map<Long, List<Long>>)
        putIntegers(integerArrayProperties, objectInputStream.readObject() as Map<Long, List<Int>>)
        //enumProperties
        putEnum(enumProperties, objectInputStream.readObject() as Map<JdsFieldEnum<*>, Enum<*>>)
        putEnums(enumCollectionProperties, objectInputStream.readObject() as Map<JdsFieldEnum<*>, List<Enum<*>>>)
    }

    private fun serializeEnums(input: Map<JdsFieldEnum<*>, ObjectProperty<Enum<*>>>): Map<JdsFieldEnum<*>, Enum<*>> =
            input.entries.associateBy({ it.key }, { it.value.get() })

    private fun serializeBlobs(input: Map<Long, BlobProperty>): Map<Long, BlobProperty> =
            input.entries.associateBy({ it.key }, { it.value })

    /**
     * Create a map that can be serialized
     * @param input an unserializable map
     * @return A serialisable map
     */
    private fun serializeEnumCollections(input: Map<JdsFieldEnum<*>, Collection<Enum<*>>>): Map<JdsFieldEnum<*>, List<Enum<*>>> =
            input.entries.associateBy({ it.key }, { ArrayList(it.value) })

    /**
     * Create a map that can be serialized
     * @param input an unserializable map
     * @return A serialisable map
     */
    private fun serializeIntegers(input: Map<Long, Collection<Int>>): Map<Long, List<Int>> =
            input.entries.associateBy({ it.key }, { ArrayList(it.value) })

    /**
     * Create a map that can be serialized
     * @param input an unserializable map
     * @return A serialisable map
     */
    private fun serializeLongs(input: Map<Long, Collection<Long>>): Map<Long, List<Long>> =
            input.entries.associateBy({ it.key }, { ArrayList(it.value) })

    /**
     * Create a map that can be serialized
     * @param input an unserializable map
     * @return A serialisable map
     */
    private fun serializeDoubles(input: Map<Long, Collection<Double>>): Map<Long, List<Double>> =
            input.entries.associateBy({ it.key }, { ArrayList(it.value) })

    /**
     * Create a map that can be serialized
     * @param input an unserializable map
     * @return A serialisable map
     */
    private fun serializeFloats(input: Map<Long, Collection<Float>>): Map<Long, List<Float>> =
            input.entries.associateBy({ it.key }, { ArrayList(it.value) })

    /**
     * Create a map that can be serialized
     * @param input an unserializable map
     * @return A serialisable map
     */
    private fun serializeDateTimes(input: Map<Long, Collection<LocalDateTime>>): Map<Long, List<LocalDateTime>> =
            input.entries.associateBy({ it.key }, { ArrayList(it.value) })

    /**
     * Create a map that can be serialized
     * @param input an unserializable map
     * @return A serialisable map
     */
    private fun serializeStrings(input: Map<Long, Collection<String?>>): Map<Long, List<String?>> =
            input.entries.associateBy({ it.key }, { ArrayList(it.value) })

    /**
     * Create a map that can be serialized
     * @param input an unserializable map
     * @return A serialisable map
     */
    private fun serializeObjects(input: Map<JdsFieldEntity<*>, Collection<JdsEntity>>): Map<JdsFieldEntity<*>, List<JdsEntity>> =
            input.entries.associateBy({ it.key }, { ArrayList(it.value) })

    /**
     * Create a map that can be serialized
     * @param input an unserializable map
     * @return A serialisable map
     */
    private fun serializeObject(input: Map<JdsFieldEntity<*>, ObjectProperty<JdsEntity>>): Map<JdsFieldEntity<*>, JdsEntity> =
            input.entries.associateBy({ it.key }, { it.value.get() })

    /**
     * Create a map that can be serialized
     * @param input an unserializable map
     * @return A serialisable map
     */
    private fun serializeTemporal(input: Map<Long, ObjectProperty<out Temporal>>): Map<Long, Temporal> =
            input.entries.associateBy({ it.key }, { it.value.get() })

    /**
     * Create a map that can be serialized
     * @param input an unserializable map
     * @return A serialisable map
     */
    private fun serializableString(input: Map<Long, StringProperty>): Map<Long, String> =
            input.entries.associateBy({ it.key }, { it.value.get() })

    private fun putDurations(destination: HashMap<Long, ObjectProperty<Duration>>, source: Map<Long, Duration>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.set(entry.value) }
    }

    private fun putPeriods(destination: HashMap<Long, ObjectProperty<Period>>, source: Map<Long, Period>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.set(entry.value) }
    }

    private fun putYearMonths(destination: HashMap<Long, ObjectProperty<Temporal>>, source: Map<Long, Temporal>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.set(entry.value) }
    }

    private fun putMonthDays(destination: HashMap<Long, ObjectProperty<MonthDay>>, source: Map<Long, MonthDay>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.set(entry.value) }
    }

    private fun putEnums(destination: Map<JdsFieldEnum<*>, MutableCollection<Enum<*>>>, source: Map<JdsFieldEnum<*>, List<Enum<*>>>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.addAll(entry.value) }
    }

    private fun putEnum(destination: Map<JdsFieldEnum<*>, ObjectProperty<Enum<*>>>, source: Map<JdsFieldEnum<*>, Enum<*>>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.set(entry.value) }
    }

    private fun putObjects(destination: Map<JdsFieldEntity<*>, MutableCollection<JdsEntity>>, source: Map<JdsFieldEntity<*>, List<JdsEntity>>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.addAll(entry.value) }
    }

    private fun putStrings(destination: Map<Long, MutableCollection<String>>, source: Map<Long, List<String>>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.addAll(entry.value) }
    }

    private fun putDateTimes(destination: Map<Long, MutableCollection<LocalDateTime>>, source: Map<Long, List<LocalDateTime>>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.addAll(entry.value) }
    }

    private fun putFloats(destination: Map<Long, MutableCollection<Float>>, source: Map<Long, List<Float>>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.addAll(entry.value) }
    }

    private fun putDoubles(destination: Map<Long, MutableCollection<Double>>, source: Map<Long, List<Double>>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.addAll(entry.value) }
    }

    private fun putLongs(destination: Map<Long, MutableCollection<Long>>, source: Map<Long, List<Long>>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.addAll(entry.value) }
    }

    private fun putIntegers(destination: Map<Long, MutableCollection<Int>>, source: Map<Long, List<Int>>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.addAll(entry.value) }
    }

    private fun putInteger(destination: MutableMap<Long, WritableValue<Int>>, source: Map<Long, WritableValue<Int>>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key] = entry.value }
    }

    private fun putBlobs(destination: MutableMap<Long, BlobProperty>, source: Map<Long, BlobProperty>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.set(entry.value.get()!!) }
    }

    private fun putLong(destination: MutableMap<Long, WritableValue<Long>>, source: Map<Long, WritableValue<Long>>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key] = entry.value }
    }

    private fun putBoolean(destination: MutableMap<Long, WritableValue<Boolean>>, source: Map<Long, WritableValue<Boolean>>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key] = entry.value }
    }

    private fun putDouble(destination: MutableMap<Long, WritableValue<Double>>, source: Map<Long, WritableValue<Double>>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key] = entry.value }
    }

    private fun putObject(destination: Map<JdsFieldEntity<*>, ObjectProperty<JdsEntity>>, source: Map<JdsFieldEntity<*>, JdsEntity>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.set(entry.value) }
    }

    private fun putFloat(destination: MutableMap<Long, WritableValue<Float>>, source: Map<Long, WritableValue<Float>>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key] = entry.value }
    }

    private fun putTemporal(destination: Map<Long, ObjectProperty<Temporal>>, source: Map<Long, Temporal>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.set(entry.value) }
    }

    private fun putString(destination: Map<Long, StringProperty>, source: Map<Long, String>) {
        source.entries.filter { entry -> destination.containsKey(entry.key) }.forEach { entry -> destination[entry.key]?.set(entry.value) }
    }

    /**
     * @param step
     * @param saveContainer
     */
    internal fun assign(step: Int, saveContainer: JdsSaveContainer) {
        //==============================================
        //PRIMITIVES
        //==============================================
        saveContainer.booleanProperties[step][overview.compositeKey] = booleanProperties
        saveContainer.stringProperties[step][overview.compositeKey] = stringProperties
        saveContainer.floatProperties[step][overview.compositeKey] = floatProperties
        saveContainer.doubleProperties[step][overview.compositeKey] = doubleProperties
        saveContainer.longProperties[step][overview.compositeKey] = longProperties
        saveContainer.integerProperties[step][overview.compositeKey] = integerProperties
        //==============================================
        //Dates & Time
        //==============================================
        saveContainer.localDateTimeProperties[step][overview.compositeKey] = localDateTimeProperties
        saveContainer.zonedDateTimeProperties[step][overview.compositeKey] = zonedDateTimeProperties
        saveContainer.localTimeProperties[step][overview.compositeKey] = localTimeProperties
        saveContainer.localDateProperties[step][overview.compositeKey] = localDateProperties
        saveContainer.monthDayProperties[step][overview.compositeKey] = monthDayProperties
        saveContainer.yearMonthProperties[step][overview.compositeKey] = yearMonthProperties
        saveContainer.periodProperties[step][overview.compositeKey] = periodProperties
        saveContainer.durationProperties[step][overview.compositeKey] = durationProperties
        //==============================================
        //BLOB
        //==============================================
        saveContainer.blobProperties[step][overview.compositeKey] = blobProperties
        //==============================================
        //Enums
        //==============================================
        saveContainer.enumProperties[step][overview.compositeKey] = enumProperties
        saveContainer.enumCollections[step][overview.compositeKey] = enumCollectionProperties
        //==============================================
        //ARRAYS
        //==============================================
        saveContainer.stringCollections[step][overview.compositeKey] = stringArrayProperties
        saveContainer.localDateTimeCollections[step][overview.compositeKey] = dateTimeArrayProperties
        saveContainer.floatCollections[step][overview.compositeKey] = floatArrayProperties
        saveContainer.doubleCollections[step][overview.compositeKey] = doubleArrayProperties
        saveContainer.longCollections[step][overview.compositeKey] = longArrayProperties
        saveContainer.integerCollections[step][overview.compositeKey] = integerArrayProperties
        //==============================================
        //EMBEDDED OBJECTS
        //==============================================
        objectArrayProperties.forEach { _, value ->
            value.forEach {
                it.overview.parentCompositeKey = overview.compositeKey
                it.overview.parentUuid = overview.uuid
            }
        }
        objectProperties.forEach { _, value ->
            value.value.overview.parentCompositeKey = overview.compositeKey
            value.value.overview.parentUuid = overview.uuid
        }
        saveContainer.objectCollections[step][overview.compositeKey] = objectArrayProperties
        saveContainer.objects[step][overview.compositeKey] = objectProperties
    }

    /**
     * @param embeddedObject
     */
    internal fun assign(embeddedObject: JdsEmbeddedObject) {
        //==============================================
        //PRIMITIVES, also saved to array struct to streamline json
        //==============================================
        booleanProperties.entries.forEach {
            val input = when (it.value.value) {
                true -> 1
                false -> 0
                else -> null
            }
            embeddedObject.b.add(JdsBooleanValues(it.key, input))
        }
        stringProperties.entries.forEach { embeddedObject.s.add(JdsStringValues(it.key, it.value.value)) }
        floatProperties.entries.forEach { embeddedObject.f.add(JdsFloatValues(it.key, it.value.value)) }
        doubleProperties.entries.forEach { embeddedObject.d.add(JdsDoubleValues(it.key, it.value.value)) }
        longProperties.entries.forEach { embeddedObject.l.add(JdsLongValues(it.key, it.value.value)) }
        integerProperties.entries.forEach { embeddedObject.i.add(JdsIntegerEnumValues(it.key, it.value.value)) }
        //==============================================
        //Dates & Time
        //==============================================
        zonedDateTimeProperties.entries.forEach { embeddedObject.l.add(JdsLongValues(it.key, (it.value.value as ZonedDateTime).toInstant().toEpochMilli())) }
        localTimeProperties.entries.forEach { embeddedObject.l.add(JdsLongValues(it.key, (it.value.value as LocalTime).toNanoOfDay())) }
        durationProperties.entries.forEach { embeddedObject.l.add(JdsLongValues(it.key, it.value.value.toNanos())) }
        localDateTimeProperties.entries.forEach { embeddedObject.ldt.add(JdsLocalDateTimeValues(it.key, Timestamp.valueOf(it.value.value as LocalDateTime))) }
        localDateProperties.entries.forEach { embeddedObject.ldt.add(JdsLocalDateTimeValues(it.key, Timestamp.valueOf((it.value.value as LocalDate).atStartOfDay()))) }
        monthDayProperties.entries.forEach { embeddedObject.s.add(JdsStringValues(it.key, it.value.value?.toString())) }
        yearMonthProperties.entries.forEach { embeddedObject.s.add(JdsStringValues(it.key, (it.value.value as YearMonth?)?.toString())) }
        periodProperties.entries.forEach { embeddedObject.s.add(JdsStringValues(it.key, it.value.value?.toString())) }
        //==============================================
        //BLOB
        //==============================================
        blobProperties.entries.forEach { embeddedObject.bl.add(JdsBlobValues(it.key, it.value.get() ?: ByteArray(0))) }
        //==============================================
        //Enums
        //==============================================
        enumProperties.entries.forEach { embeddedObject.i.add(JdsIntegerEnumValues(it.key.field.id, it.value.value.ordinal)) }
        enumCollectionProperties.entries.forEach { it.value.forEach { child -> embeddedObject.i.add(JdsIntegerEnumValues(it.key.field.id, child.ordinal)) } }
        //==============================================
        //ARRAYS
        //==============================================
        stringArrayProperties.entries.forEach { it.value.forEach { child -> embeddedObject.s.add(JdsStringValues(it.key, child)) } }
        dateTimeArrayProperties.entries.forEach { it.value.forEach { child -> embeddedObject.ldt.add(JdsLocalDateTimeValues(it.key, Timestamp.valueOf(child))) } }
        floatArrayProperties.entries.forEach { it.value.forEach { child -> embeddedObject.f.add(JdsFloatValues(it.key, child)) } }
        doubleArrayProperties.entries.forEach { it.value.forEach { child -> embeddedObject.d.add(JdsDoubleValues(it.key, child)) } }
        longArrayProperties.entries.forEach { it.value.forEach { child -> embeddedObject.l.add(JdsLongValues(it.key, child)) } }
        integerArrayProperties.entries.forEach { it.value.forEach { child -> embeddedObject.i.add(JdsIntegerEnumValues(it.key, child)) } }
        //==============================================
        //EMBEDDED OBJECTS
        //==============================================
        objectArrayProperties.forEach { key, itx ->
            itx.forEach { embeddedObject.eo.add(JdsEmbeddedObject(it, key.fieldEntity.id)) }
        }
        objectProperties.forEach { key, it ->
            embeddedObject.eo.add(JdsEmbeddedObject(it.value, key.fieldEntity.id))
        }
    }

    /**
     * @param fieldType
     * @param fieldId
     * @param value
     */
    internal fun populateProperties(fieldType: JdsFieldType, fieldId: Long, value: Any?) {
        when (fieldType) {
        //========================= primitives (can be null)
            JdsFieldType.FLOAT -> floatProperties[fieldId]?.value = when (value) {
                is Double -> value.toFloat()
                else -> value as Float?
            }
            JdsFieldType.DOUBLE -> doubleProperties[fieldId]?.value = value as Double?
            JdsFieldType.LONG -> longProperties[fieldId]?.value = when (value) {
                is Long? -> value
                is BigDecimal -> value.toLong() //Oracle
                is Integer -> value.toLong()
                else -> null
            }
            JdsFieldType.INT -> integerProperties[fieldId]?.value = when (value) {
                is Int? -> value
                is BigDecimal -> value.toInt() //Oracle
                else -> null
            }
            JdsFieldType.BOOLEAN -> booleanProperties[fieldId]?.value = when (value) {
                is Int -> value == 1
                is Boolean? -> value
                is BigDecimal -> value.intValueExact() == 1 //Oracle
                else -> null
            }
        //========================= collections (assumed cannot be null)
            JdsFieldType.DOUBLE_COLLECTION -> doubleArrayProperties[fieldId]?.add(value as Double)
            JdsFieldType.FLOAT_COLLECTION -> floatArrayProperties[fieldId]?.add(value as Float)
            JdsFieldType.LONG_COLLECTION -> longArrayProperties[fieldId]?.add(value as Long)
            JdsFieldType.INT_COLLECTION -> integerArrayProperties[fieldId]?.add(value as Int)
        //========================= enums
            JdsFieldType.ENUM -> enumProperties.filter { it.key.field.id == fieldId }.forEach {
                it.value?.set(when (value) {
                    is BigDecimal -> it.key.valueOf(value.intValueExact())
                    else -> it.key.valueOf(value as Int)
                })
            }
            JdsFieldType.ENUM_COLLECTION -> enumCollectionProperties.filter { it.key.field.id == fieldId }.forEach {
                val enumValues = it.key.enumType.enumConstants
                val index = when (value) { is Int -> value; is BigDecimal -> value.intValueExact(); else -> enumValues.size; }
                if (index < enumValues.size) {
                    it.value.add(enumValues[index] as Enum<*>)
                }
            }
        //========================= strings
            JdsFieldType.STRING -> stringProperties[fieldId]?.set(value as String?)
            JdsFieldType.STRING_COLLECTION -> stringArrayProperties[fieldId]?.add(value as String)
        //========================= dates and times
            JdsFieldType.ZONED_DATE_TIME -> when (value) {
                is Long -> zonedDateTimeProperties[fieldId]?.set(ZonedDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault()))
                is Timestamp -> zonedDateTimeProperties[fieldId]?.set(value.let { ZonedDateTime.ofInstant(it.toInstant(), ZoneOffset.systemDefault()) })
                is String -> zonedDateTimeProperties[fieldId]?.set(value.let { it.toZonedDateTime() })
                is OffsetDateTime -> zonedDateTimeProperties[fieldId]?.set(value.let { it.atZoneSameInstant(ZoneId.systemDefault()) })
            }
            JdsFieldType.DATE -> localDateProperties[fieldId]?.set((value as Timestamp).toLocalDateTime().toLocalDate())
            JdsFieldType.TIME -> when (value) {
                is Long -> localTimeProperties[fieldId]?.set(LocalTime.MIN.plusNanos(value))
                is LocalTime -> localTimeProperties[fieldId]?.set(value)
                is String -> localTimeProperties[fieldId]?.set(value.toLocalTimeSqlFormat())
            }
            JdsFieldType.DURATION -> durationProperties[fieldId]?.set(when (value) {
                is BigDecimal -> Duration.ofNanos(value.longValueExact())//Oracle
                else -> Duration.ofNanos(value as Long)
            })
            JdsFieldType.MONTH_DAY -> monthDayProperties[fieldId]?.value = (value as String).let { MonthDay.parse(it) }
            JdsFieldType.YEAR_MONTH -> yearMonthProperties[fieldId]?.value = (value as String).let { YearMonth.parse(it) }
            JdsFieldType.PERIOD -> periodProperties[fieldId]?.value = (value as String).let { Period.parse(it) }
            JdsFieldType.DATE_TIME -> localDateTimeProperties[fieldId]?.value = (value as Timestamp).let { it.toLocalDateTime() }
            JdsFieldType.DATE_TIME_COLLECTION -> dateTimeArrayProperties[fieldId]?.add((value as Timestamp).let { it.toLocalDateTime() })
        //========================= blob
            JdsFieldType.BLOB -> when (value) {
                is ByteArray -> blobProperties[fieldId]?.set(value)
                null -> blobProperties[fieldId]?.set(ByteArray(0))//Oracle
            }
        }
    }

    /**
     * @param jdsDb
     * @param fieldId
     * @param entityId
     * @param uuid
     * @param innerObjects
     * @param uuids
     */
    internal fun populateObjects(jdsDb: JdsDb, fieldId: Long?, entityId: Long, uuid: String, uuidLocation: String, uuidLocationVersion: Int, parentUuid: String?, innerObjects: ConcurrentLinkedQueue<JdsEntity>, uuids: HashSet<String>) {
        try {
            if (fieldId != null) return
            objectArrayProperties.filter { it.key.fieldEntity.id == fieldId }.forEach {
                val entity = jdsDb.classes[entityId]!!.newInstance()
                entity.overview.uuid = uuid
                entity.overview.uuidLocation = uuidLocation
                entity.overview.uuidLocationVersion = uuidLocationVersion
                entity.overview.parentUuid = parentUuid
                uuids.add(uuid)
                it.value.add(entity)
                innerObjects.add(entity)
            }
            objectProperties.filter { it.key.fieldEntity.id == fieldId }.forEach {
                val jdsEntity = jdsDb.classes[entityId]!!.newInstance()
                jdsEntity.overview.uuid = uuid
                jdsEntity.overview.uuidLocation = uuidLocation
                jdsEntity.overview.uuidLocationVersion = uuidLocationVersion
                jdsEntity.overview.parentUuid = parentUuid
                uuids.add(uuid)
                it.value.set(jdsEntity)
                innerObjects.add(jdsEntity)
            }
        } catch (ex: Exception) {
            ex.printStackTrace(System.err)
        }
    }


    /**
     * Binds all the fieldIds attached to an entity, updates the fieldIds dictionary
     * @param connection the SQL connection to use for DB operations
     * @param entityId   the value representing the entity
     */
    internal fun populateRefFieldRefEntityField(jdsDb: JdsDb, connection: Connection, entityId: Long) = try {
        (if (jdsDb.supportsStatements) connection.prepareCall(jdsDb.populateRefField()) else connection.prepareStatement(jdsDb.populateRefField())).use { populateRefField ->
            (if (jdsDb.supportsStatements) connection.prepareCall(jdsDb.populateRefEntityField()) else connection.prepareStatement(jdsDb.populateRefEntityField())).use { populateRefEntityField ->
                getFields(overview.entityId).forEach {
                    val lookup = JdsField.values[it]!!
                    //1. map this fieldEntity to the fieldEntity dictionary
                    populateRefField.setLong(1, lookup.id)
                    populateRefField.setString(2, lookup.name)
                    populateRefField.setString(3, lookup.description)
                    populateRefField.setInt(4, lookup.type.ordinal)
                    populateRefField.addBatch()
                    //2. map this fieldEntity ID to the entity type
                    populateRefEntityField.setLong(1, entityId)
                    populateRefEntityField.setLong(2, lookup.id)
                    populateRefEntityField.addBatch()
                }
                populateRefField.executeBatch()
                populateRefEntityField.executeBatch()
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace(System.err)
    }

    /**
     * Binds all the enumProperties attached to an entity
     * @param connection
     * @param entityId
     * @param jdsDb
     */
    @Synchronized
    internal fun populateRefEnumRefEntityEnum(jdsDb: JdsDb, connection: Connection, entityId: Long) {
        populateRefEnum(jdsDb, connection, getEnums(overview.entityId))
        populateRefEntityEnum(jdsDb, connection, entityId, getEnums(overview.entityId))
        if (jdsDb.options.isPrintingOutput)
            System.out.printf("Mapped Enums for Entity[%s]\n", entityId)
    }

    /**
     * Binds all the enumProperties attached to an entity
     * @param jdsDb
     * @param connection the SQL connection to use for DB operations
     * @param entityId   the value representing the entity
     * @param fieldIds     the entity's enumProperties
     */
    @Synchronized
    private fun populateRefEntityEnum(jdsDb: JdsDb, connection: Connection, entityId: Long, fieldIds: Set<Long>) = try {
        (if (jdsDb.supportsStatements) connection.prepareCall(jdsDb.populateRefEntityEnum()) else connection.prepareStatement(jdsDb.populateRefEntityEnum())).use {
            for (fieldIds in fieldIds) {
                val jdsFieldEnum = JdsFieldEnum.enums[fieldIds]!!
                for (index in 0 until jdsFieldEnum.values.size) {
                    it.setLong(1, entityId)
                    it.setLong(2, jdsFieldEnum.field.id)
                    it.addBatch()
                }
            }
            it.executeBatch()
        }
    } catch (ex: Exception) {
        ex.printStackTrace(System.err)
    }

    /**
     * Binds all the values attached to an enum
     * @param jdsDb
     * @param connection the SQL connection to use for DB operations
     * @param fieldIds the fieldEntity enum
     */
    @Synchronized
    private fun populateRefEnum(jdsDb: JdsDb, connection: Connection, fieldIds: Set<Long>) = try {
        (if (jdsDb.supportsStatements) connection.prepareCall(jdsDb.populateRefEnum()) else connection.prepareStatement(jdsDb.populateRefEnum())).use {
            for (fieldId in fieldIds) {
                val jdsFieldEnum = JdsFieldEnum.enums[fieldId]!!
                for (index in 0 until jdsFieldEnum.values.size) {
                    it.setLong(1, jdsFieldEnum.field.id)
                    it.setInt(2, index)
                    it.setString(3, jdsFieldEnum.values[index].toString())
                    it.addBatch()
                }
            }
            it.executeBatch()
        }
    } catch (ex: Exception) {
        ex.printStackTrace(System.err)
    }

    /**
     * @param id
     * @param ordinal
     * @return
     */
    fun getReportAtomicValue(id: Long, ordinal: Int): Any? {
        //time constructs
        if (localDateTimeProperties.containsKey(id))
            return Timestamp.valueOf(localDateTimeProperties[id]!!.value as LocalDateTime)
        if (zonedDateTimeProperties.containsKey(id))
            return (zonedDateTimeProperties[id]!!.value as ZonedDateTime)
        if (localDateProperties.containsKey(id))
            return Timestamp.valueOf((localDateProperties[id]!!.value as LocalDate).atStartOfDay())
        if (localTimeProperties.containsKey(id))
            return (localTimeProperties[id]!!.value as LocalTime)
        if (monthDayProperties.containsKey(id))
            return monthDayProperties[id]!!.value.toString()
        if (yearMonthProperties.containsKey(id))
            return yearMonthProperties[id]!!.value.toString()
        if (periodProperties.containsKey(id))
            return periodProperties[id]!!.value.toString()
        if (durationProperties.containsKey(id))
            return durationProperties[id]!!.value.toNanos()
        //string
        if (stringProperties.containsKey(id))
            return stringProperties[id]!!.value
        //primitives
        if (floatProperties.containsKey(id))
            return floatProperties[id]?.value
        if (doubleProperties.containsKey(id))
            return doubleProperties[id]?.value
        if (booleanProperties.containsKey(id))
            return booleanProperties[id]?.value
        if (longProperties.containsKey(id))
            return longProperties[id]?.value
        if (integerProperties.containsKey(id))
            return integerProperties[id]?.value
        enumProperties.filter { it.key.field.id == id && it.value.value.ordinal == ordinal }.forEach {
            return 1
        }
        enumCollectionProperties.filter { it.key.field.id == id }.forEach { it.value.filter { it.ordinal == ordinal }.forEach { return true } }
        //single object references
        objectProperties.filter { it.key.fieldEntity.id == id }.forEach {
            return it.value.value.overview.uuid
        }
        return null
    }

    /**
     * @param table
     */
    override fun registerFields(table: JdsTable) {
        getFields(overview.entityId).forEach { table.registerField(it) }
    }

    /**
     * Ensures child entities have ids that link them to their parent.
     * For frequent refreshes/imports from different sources this is necessary to prevent duplicate entries of the same data
     * @param uuid
     */
    @JvmOverloads
    fun standardizeUUIDs(uuid: String = overview.uuid) {
        standardizeObjectUUIDs(uuid, objectProperties)
        standardizeObjectCollectionUUIDs(uuid, objectArrayProperties)
    }

    /**
     * Ensures child entities have ids that link them to their parent.
     * @param uuid
     * @param objectArrayProperties
     */
    private fun standardizeObjectCollectionUUIDs(uuid: String, objectArrayProperties: HashMap<JdsFieldEntity<*>, MutableCollection<JdsEntity>>) {
        objectArrayProperties.entries.forEach {
            //parent-uuid.entity_id.sequence e.g ab9d2da6-fb64-47a9-9a3c-a6e0a998703f.256.3
            it.value.forEachIndexed { sequence, entry ->
                val entityId = entry.overview.entityId
                val newUUID = "$uuid.$entityId.$sequence"
                entry.overview.uuid = newUUID
                //process children
                standardizeObjectUUIDs(newUUID, entry.objectProperties)
                standardizeObjectCollectionUUIDs(newUUID, entry.objectArrayProperties)
            }
        }
    }

    /**
     * Ensures child entities have ids that link them to their parent.
     * @param uuid
     * @param objectProperties
     */
    private fun standardizeObjectUUIDs(uuid: String, objectProperties: HashMap<JdsFieldEntity<*>, ObjectProperty<JdsEntity>>) {
        //parent-uuid.entity_id.sequence e.g ab9d2da6-fb64-47a9-9a3c-a6e0a998703f.256
        objectProperties.entries.forEach { entry ->
            val entityId = entry.value.value.overview.entityId
            val newUUID = "$uuid.$entityId"
            entry.value.value.overview.uuid = newUUID
            //process children
            standardizeObjectUUIDs(newUUID, entry.value.value.objectProperties)
            standardizeObjectCollectionUUIDs(newUUID, entry.value.value.objectArrayProperties)
        }
    }

    /**
     * Internal helper function that works with all nested objects
     */
    fun getAllEntities(includeThisEntity: Boolean = true): Sequence<JdsEntity> = buildSequence {
        if (includeThisEntity)
            yield(this@JdsEntity)
        objectProperties.values.forEach { yieldAll(it.value.getAllEntities()) }
        objectArrayProperties.values.forEach { it.forEach { yieldAll(it.getAllEntities()) } }
    }

    /**
     * Set this jds entity and all of its children as live?
     */
    fun setAllLive(live: Boolean) {
        getAllEntities().forEach { it.overview.live = live }
    }

    companion object : Externalizable {

        private const val serialVersionUID = 20180106_2125L
        private val allFields = ConcurrentHashMap<Long, MutableSet<Long>>()
        private val allEnums = ConcurrentHashMap<Long, MutableSet<Long>>()

        override fun readExternal(objectInput: ObjectInput) {
            allFields.clear()
            allFields.putAll(objectInput.readObject() as Map<Long, MutableSet<Long>>)
            allEnums.clear()
            allEnums.putAll(objectInput.readObject() as Map<Long, MutableSet<Long>>)
        }

        override fun writeExternal(objectOutput: ObjectOutput) {
            objectOutput.writeObject(allFields)
            objectOutput.writeObject(allEnums)
        }

        internal fun mapField(entityId: Long, fieldId: Long) {
            getFields(entityId).add(fieldId)
        }

        internal fun mapEnums(entityId: Long, fieldId: Long) {
            getEnums(entityId).add(fieldId)
        }

        internal fun getFields(entityId: Long) = allFields.getOrPut(entityId) { HashSet() }

        internal fun getEnums(entityId: Long) = allEnums.getOrPut(entityId) { HashSet() }
    }
}
