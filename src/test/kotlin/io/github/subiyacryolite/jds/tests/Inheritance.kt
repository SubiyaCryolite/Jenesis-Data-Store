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
package io.github.subiyacryolite.jds.tests

import io.github.subiyacryolite.jds.tests.common.BaseTestConfig
import io.github.subiyacryolite.jds.tests.common.TestData
import io.github.subiyacryolite.jds.tests.entities.EntityA
import io.github.subiyacryolite.jds.tests.entities.EntityB
import io.github.subiyacryolite.jds.tests.entities.EntityC
import io.github.subiyacryolite.jds.context.DbContext
import io.github.subiyacryolite.jds.Load
import io.github.subiyacryolite.jds.Save
import org.junit.jupiter.api.Test

class Inheritance : BaseTestConfig("Inheritance") {

    @Throws(Exception::class)
    override fun testImpl(dbContext: DbContext) {
        save(dbContext)
        load(dbContext)
    }

    @Throws(Exception::class)
    private fun save(dbContext: DbContext) {
        val save = Save(dbContext, TestData.inheritanceCollection)
        save.call()
    }

    @Throws(Exception::class)
    private fun load(dbContext: DbContext) {
        val entityAs = Load(dbContext, EntityA::class.java)
        val entityBs = Load(dbContext, EntityB::class.java)
        val entityCs = Load(dbContext, EntityC::class.java)
        System.out.printf("All A s [%s]\n", entityAs.call())
        System.out.printf("All B s [%s]\n", entityBs.call())
        System.out.printf("All C s [%s]\n", entityCs.call())
    }

    @Test
    fun postGreSql() {
        testPostgreSql()
    }

    @Test
    fun sqlLite() {
        testSqLite()
    }

    @Test
    fun mariaDb() {
        testMariaDb()
    }

    @Test
    fun mySql() {
        testMySql()
    }

    @Test
    fun oracle() {
        testOracle()
    }

    @Test
    fun transactionalSql() {
        testTransactionalSql()
    }
}
