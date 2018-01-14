package tests

import org.junit.jupiter.api.Test

class AllTests {

    val bulkSaveAndLoad = BulkSaveAndLoad()
    val customReport = CustomReport()
    val customReportJson = CustomReportJson()
    val inheritance = Inheritance()
    val legacyValidation = LegacyValidation()
    val legacyLoadAndSave = LegacyLoadAndSave()
    val nonExisting = NonExisting()
    val timeConstructs = TimeConstructs()
    val loadAndSaveTests = LoadAndSaveTests()

    @Test
    fun testAll() {
        testTransactionalSql()
        testMySql()
        testOracle()
        testPostgreSql()
        testSqLite()
    }

    @Test
    fun testTransactionalSql() {
        bulkSaveAndLoad.testTransactionalSql()
        customReport.testTransactionalSql()
        customReportJson.testTransactionalSql()
        inheritance.testTransactionalSql()
        legacyValidation.testTransactionalSql()
        legacyLoadAndSave.testTransactionalSql()
        nonExisting.testTransactionalSql()
        timeConstructs.testTransactionalSql()
        loadAndSaveTests.testTransactionalSql()
    }

    @Test
    fun testMySql() {
        bulkSaveAndLoad.testMySql()
        customReport.testMySql()
        customReportJson.testMySql()
        inheritance.testMySql()
        legacyValidation.testMySql()
        legacyLoadAndSave.testMySql()
        nonExisting.testMySql()
        timeConstructs.testMySql()
        loadAndSaveTests.testMySql()
    }

    @Test
    fun testOracle() {
        bulkSaveAndLoad.testOracle()
        customReport.testOracle()
        customReportJson.testOracle()
        inheritance.testOracle()
        legacyValidation.testOracle()
        legacyLoadAndSave.testOracle()
        nonExisting.testOracle()
        timeConstructs.testOracle()
        loadAndSaveTests.testOracle()
    }

    @Test
    fun testPostgreSql() {
        bulkSaveAndLoad.testPostgreSql()
        customReport.testPostgreSql()
        customReportJson.testPostgreSql()
        inheritance.testPostgreSql()
        legacyValidation.testPostgreSql()
        legacyLoadAndSave.testPostgreSql()
        nonExisting.testPostgreSql()
        timeConstructs.testPostgreSql()
        loadAndSaveTests.testPostgreSql()
    }

    @Test
    fun testSqLite() {
        bulkSaveAndLoad.testSqLite()
        customReport.testSqLite()
        customReportJson.testSqLite()
        inheritance.testSqLite()
        legacyValidation.testSqLite()
        legacyLoadAndSave.testSqLite()
        nonExisting.testSqLite()
        timeConstructs.testSqLite()
        loadAndSaveTests.testSqLite()
    }
}