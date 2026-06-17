package com.psy.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.psy.data.db.dao.LedgerDao
import com.psy.data.db.entity.LedgerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LedgerDaoTest {

    private lateinit var db: PsyDatabase
    private lateinit var dao: LedgerDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PsyDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.ledgerDao()
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    @Test
    fun `insert then observe returns the ledger`() = runTest {
        val inserted = LedgerEntity(id = 0, name = "Personal", icon = "wallet", currency = "VND", createdAt = 1000L)
        dao.insert(inserted)

        val ledgers = dao.observeAll().first()

        assertEquals(1, ledgers.size)
        assertEquals(inserted.copy(id = 1), ledgers[0])
    }
}
