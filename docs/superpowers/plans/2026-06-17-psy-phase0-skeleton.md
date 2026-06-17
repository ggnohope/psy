# Psy Phase 0 — Project Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up the Psy monorepo so that the Android app builds and runs with Hilt/Room/Compose and the Candy Pop theme wired in, and the Go backend boots, connects to Postgres, and serves `/health` — all covered by smoke tests.

**Architecture:** Monorepo at `~/Codes/psy` with `android/` (single-module Kotlin app, packages `data/domain/ui/di`, MVVM) and `backend/` (Go chi service + Postgres). Phase 0 builds only the skeleton + one real TDD'd domain unit (money formatting); feature logic comes in later plans.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Hilt, Room, Retrofit + kotlinx.serialization, Navigation Compose, DataStore, Coil, Lottie; Go (chi v5, pgx), Postgres.

> **Version note:** Versions below are known-good as of 2026-06. If Android Studio's New Project wizard scaffolds newer Kotlin/AGP/Compose-BOM versions, keep the wizard's versions and only add the libraries this plan introduces. Do not downgrade what the wizard produced.

---

## File Structure

**Android (`android/`)** — created by Android Studio wizard, then modified:
- `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml` — build config + version catalog
- `app/build.gradle.kts` — module deps + plugins (Hilt, KSP, kotlin-serialization)
- `app/src/main/AndroidManifest.xml` — declares `PsyApplication`
- `app/src/main/java/com/psy/PsyApplication.kt` — `@HiltAndroidApp`
- `app/src/main/java/com/psy/MainActivity.kt` — `@AndroidEntryPoint`, sets Compose content
- `app/src/main/java/com/psy/domain/util/Money.kt` — minor-unit money formatting (first TDD unit)
- `app/src/main/java/com/psy/ui/theme/{Color,Type,Shape,Theme}.kt` — Candy Pop design system
- `app/src/main/java/com/psy/data/db/{PsyDatabase,entity/LedgerEntity,dao/LedgerDao}.kt` — Room skeleton
- `app/src/main/java/com/psy/di/{DatabaseModule,NetworkModule}.kt` — Hilt modules
- `app/src/test/java/com/psy/domain/util/MoneyTest.kt` — JVM unit test
- `app/src/test/java/com/psy/data/db/LedgerDaoTest.kt` — Robolectric Room test

**Backend (`backend/`)**:
- `go.mod`, `go.sum`
- `cmd/server/main.go` — entrypoint, wires router + db
- `internal/api/{router,health}.go` + `internal/api/health_test.go` — chi router + `/health`
- `internal/db/db.go` — pgx pool + `Ping`
- `internal/db/migrate.go` + `internal/db/migrations/0001_init.sql` — migration runner + initial schema
- `internal/config/config.go` — env config
- `.env.example`

---

## Task 1: Bootstrap Android project via Android Studio

**Files:**
- Create (via wizard): `android/` Empty Activity (Compose) project

- [ ] **Step 1: Create the base project in Android Studio**

In Android Studio: **New Project → Empty Activity (Compose)**. Set:
- Name: `Psy`
- Package name: `com.psy`
- Save location: `/Users/hoalam/Codes/psy/android`
- Language: Kotlin
- Minimum SDK: **API 26 (Android 8.0)**
- Build configuration language: **Kotlin DSL (build.gradle.kts) + Version Catalog**

- [ ] **Step 2: Verify the base project builds**

Run:
```bash
cd /Users/hoalam/Codes/psy/android && ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit the untouched scaffold**

```bash
cd /Users/hoalam/Codes/psy
git add android/
git commit -m "chore(android): bootstrap empty Compose project"
```

---

## Task 2: Add the version catalog entries

**Files:**
- Modify: `android/gradle/libs.versions.toml`

- [ ] **Step 1: Add versions, libraries, and plugins to the catalog**

Merge these into `android/gradle/libs.versions.toml` (keep any wizard-generated entries; add what's missing):

```toml
[versions]
hilt = "2.53.1"
ksp = "2.1.0-1.0.29"          # must match the kotlin version's KSP build
room = "2.6.1"
retrofit = "2.11.0"
kotlinxSerialization = "1.7.3"
retrofitSerialization = "1.0.0"
navigationCompose = "2.8.5"
datastore = "1.1.1"
coil = "2.7.0"
lottie = "6.6.0"
coroutines = "1.9.0"
turbine = "1.2.0"
robolectric = "4.14.1"
coreTesting = "2.2.0"

[libraries]
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version = "1.2.0" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
retrofit-serialization = { module = "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter", version.ref = "retrofitSerialization" }
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }
lottie-compose = { module = "com.airbnb.android:lottie-compose", version.ref = "lottie" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
androidx-arch-core-testing = { module = "androidx.arch.core:core-testing", version.ref = "coreTesting" }

[plugins]
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version = "2.1.0" }
```

- [ ] **Step 2: Register plugins in the root build file**

In `android/build.gradle.kts`, add to the top-level `plugins {}` block (alias only, `apply false`):

```kotlin
plugins {
    // ...wizard-generated aliases kept as-is...
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

- [ ] **Step 3: Sync and verify**

Run:
```bash
cd /Users/hoalam/Codes/psy/android && ./gradlew help -q
```
Expected: completes with no version-catalog or plugin-resolution errors.

- [ ] **Step 4: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add android/gradle/libs.versions.toml android/build.gradle.kts
git commit -m "build(android): add version catalog entries for Hilt/Room/Retrofit"
```

---

## Task 3: Wire app module plugins and dependencies

**Files:**
- Modify: `android/app/build.gradle.kts`

- [ ] **Step 1: Apply plugins**

In `android/app/build.gradle.kts`, add to the `plugins {}` block:

```kotlin
plugins {
    // ...wizard-generated aliases (application, kotlin.android, compose compiler) kept...
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}
```

- [ ] **Step 2: Add dependencies**

In the same file's `dependencies {}` block, add:

```kotlin
dependencies {
    // ...wizard-generated compose/core deps kept...

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.serialization)

    implementation(libs.navigation.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.lottie.compose)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.room.testing)
}
```

- [ ] **Step 3: Verify build**

Run:
```bash
cd /Users/hoalam/Codes/psy/android && ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL` (Hilt/KSP processors run with no generated code yet).

- [ ] **Step 4: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add android/app/build.gradle.kts
git commit -m "build(android): wire Hilt, Room, Retrofit deps in app module"
```

---

## Task 4: Money formatting (first real TDD unit)

**Files:**
- Create: `android/app/src/main/java/com/psy/domain/util/Money.kt`
- Test: `android/app/src/test/java/com/psy/domain/util/MoneyTest.kt`

Amounts are stored as minor-unit `Long` (per spec §5). `Money.formatMinor` renders a minor-unit
amount as a grouped decimal string with a currency suffix.

- [ ] **Step 1: Write the failing test**

`android/app/src/test/java/com/psy/domain/util/MoneyTest.kt`:
```kotlin
package com.psy.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    @Test
    fun `formats whole amount with grouping and suffix`() {
        assertEquals("2,450,000 đ", Money.formatMinor(amountMinor = 245_000_000, fractionDigits = 2, suffix = "đ"))
    }

    @Test
    fun `formats amount with fractional digits`() {
        assertEquals("12.34 $", Money.formatMinor(amountMinor = 1234, fractionDigits = 2, suffix = "$"))
    }

    @Test
    fun `formats zero-fraction currency without decimals`() {
        assertEquals("7,000 đ", Money.formatMinor(amountMinor = 7_000, fractionDigits = 0, suffix = "đ"))
    }

    @Test
    fun `formats negative amount with leading minus`() {
        assertEquals("-45.00 $", Money.formatMinor(amountMinor = -4500, fractionDigits = 2, suffix = "$"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/hoalam/Codes/psy/android && ./gradlew :app:testDebugUnitTest --tests "com.psy.domain.util.MoneyTest"
```
Expected: FAIL — `Money` is unresolved (compilation error).

- [ ] **Step 3: Write minimal implementation**

`android/app/src/main/java/com/psy/domain/util/Money.kt`:
```kotlin
package com.psy.domain.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/** Formatting helpers for amounts stored as minor units (e.g. cents, đồng). */
object Money {

    /**
     * Renders a minor-unit amount as a grouped decimal string with a currency suffix.
     *
     * @param amountMinor amount in minor units (e.g. 245_000_000 == 2,450,000.00)
     * @param fractionDigits decimal places for the currency (0 for VND, 2 for USD)
     * @param suffix currency symbol appended after a space (e.g. "đ", "$")
     */
    fun formatMinor(amountMinor: Long, fractionDigits: Int, suffix: String): String {
        val divisor = Math.pow(10.0, fractionDigits.toDouble())
        val value = abs(amountMinor) / divisor

        val symbols = DecimalFormatSymbols(Locale.US) // ',' grouping, '.' decimal
        val pattern = buildString {
            append("#,##0")
            if (fractionDigits > 0) {
                append('.')
                repeat(fractionDigits) { append('0') }
            }
        }
        val formatted = DecimalFormat(pattern, symbols).format(value)
        val sign = if (amountMinor < 0) "-" else ""
        return "$sign$formatted $suffix"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
cd /Users/hoalam/Codes/psy/android && ./gradlew :app:testDebugUnitTest --tests "com.psy.domain.util.MoneyTest"
```
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add android/app/src/main/java/com/psy/domain/util/Money.kt android/app/src/test/java/com/psy/domain/util/MoneyTest.kt
git commit -m "feat(domain): add Money.formatMinor for minor-unit amounts"
```

---

## Task 5: Candy Pop design system (theme)

**Files:**
- Create: `android/app/src/main/java/com/psy/ui/theme/Color.kt`
- Create: `android/app/src/main/java/com/psy/ui/theme/Shape.kt`
- Create: `android/app/src/main/java/com/psy/ui/theme/Type.kt`
- Create: `android/app/src/main/java/com/psy/ui/theme/Theme.kt`

Replace the wizard-generated theme files with the Candy Pop palette (vibrant pastel:
violet/sky primary, pink accent, white surfaces, large rounded shapes).

- [ ] **Step 1: Write Color.kt**

`android/app/src/main/java/com/psy/ui/theme/Color.kt`:
```kotlin
package com.psy.ui.theme

import androidx.compose.ui.graphics.Color

// Candy Pop palette
val CandyViolet = Color(0xFFA18CFF)
val CandySky = Color(0xFF7FD8FF)
val CandyPink = Color(0xFFFF8FD6)
val CandyPinkDeep = Color(0xFFFF5FA2)
val CandyGreen = Color(0xFF22C55E)

val SurfaceLight = Color(0xFFF4F0FF)
val OnSurfaceLight = Color(0xFF2B2640)
val SurfaceDark = Color(0xFF1C1830)
val OnSurfaceDark = Color(0xFFEDE9FF)
```

- [ ] **Step 2: Write Shape.kt**

`android/app/src/main/java/com/psy/ui/theme/Shape.kt`:
```kotlin
package com.psy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Large, rounded "cute" shapes
val CandyShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)
```

- [ ] **Step 3: Write Type.kt**

`android/app/src/main/java/com/psy/ui/theme/Type.kt`:
```kotlin
package com.psy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Default to system FontFamily.Default for now; swap to a rounded font
// (e.g. Quicksand / Baloo 2 in res/font) in the Theming & Lock plan.
private val CandyFont = FontFamily.Default

val CandyTypography = Typography(
    headlineMedium = TextStyle(fontFamily = CandyFont, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
    titleMedium = TextStyle(fontFamily = CandyFont, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = CandyFont, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = CandyFont, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
)
```

- [ ] **Step 4: Write Theme.kt**

`android/app/src/main/java/com/psy/ui/theme/Theme.kt`:
```kotlin
package com.psy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CandyViolet,
    secondary = CandySky,
    tertiary = CandyPink,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
)

private val DarkColors = darkColorScheme(
    primary = CandyViolet,
    secondary = CandySky,
    tertiary = CandyPink,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
)

@Composable
fun PsyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CandyTypography,
        shapes = CandyShapes,
        content = content,
    )
}
```

- [ ] **Step 5: Delete wizard theme leftovers and verify build**

Delete any wizard-generated `ui/theme/*.kt` that conflict (e.g. old `Color.kt`/`Theme.kt`/`Type.kt`
under a different package or with a `PsyTheme`/`<Name>Theme` duplicate). Then:
```bash
cd /Users/hoalam/Codes/psy/android && ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add android/app/src/main/java/com/psy/ui/theme/
git commit -m "feat(ui): add Candy Pop design system (color/shape/type/theme)"
```

---

## Task 6: Room database skeleton + DAO test

**Files:**
- Create: `android/app/src/main/java/com/psy/data/db/entity/LedgerEntity.kt`
- Create: `android/app/src/main/java/com/psy/data/db/dao/LedgerDao.kt`
- Create: `android/app/src/main/java/com/psy/data/db/PsyDatabase.kt`
- Test: `android/app/src/test/java/com/psy/data/db/LedgerDaoTest.kt`

`LedgerEntity` is the seed entity proving Room is wired; full schema lands in the Core Bookkeeping plan.

- [ ] **Step 1: Write the failing DAO test**

`android/app/src/test/java/com/psy/data/db/LedgerDaoTest.kt`:
```kotlin
package com.psy.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
    fun tearDown() = db.close()

    @Test
    fun `insert then observe returns the ledger`() = runTest {
        dao.insert(LedgerEntity(id = 1, name = "Personal", icon = "wallet", currency = "VND", createdAt = 1000L))

        val ledgers = dao.observeAll().first()

        assertEquals(1, ledgers.size)
        assertEquals("Personal", ledgers[0].name)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/hoalam/Codes/psy/android && ./gradlew :app:testDebugUnitTest --tests "com.psy.data.db.LedgerDaoTest"
```
Expected: FAIL — `PsyDatabase`, `LedgerDao`, `LedgerEntity` unresolved.

- [ ] **Step 3: Write the entity**

`android/app/src/main/java/com/psy/data/db/entity/LedgerEntity.kt`:
```kotlin
package com.psy.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ledgers")
data class LedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val currency: String,
    val createdAt: Long,
)
```

- [ ] **Step 4: Write the DAO**

`android/app/src/main/java/com/psy/data/db/dao/LedgerDao.kt`:
```kotlin
package com.psy.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.psy.data.db.entity.LedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ledger: LedgerEntity)

    @Query("SELECT * FROM ledgers ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<LedgerEntity>>
}
```

- [ ] **Step 5: Write the database**

`android/app/src/main/java/com/psy/data/db/PsyDatabase.kt`:
```kotlin
package com.psy.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.psy.data.db.dao.LedgerDao
import com.psy.data.db.entity.LedgerEntity

@Database(entities = [LedgerEntity::class], version = 1, exportSchema = false)
abstract class PsyDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
}
```

- [ ] **Step 6: Run test to verify it passes**

Run:
```bash
cd /Users/hoalam/Codes/psy/android && ./gradlew :app:testDebugUnitTest --tests "com.psy.data.db.LedgerDaoTest"
```
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add android/app/src/main/java/com/psy/data/db/ android/app/src/test/java/com/psy/data/db/
git commit -m "feat(data): add Room skeleton (PsyDatabase, LedgerEntity, LedgerDao)"
```

---

## Task 7: Hilt application, modules, and MainActivity

**Files:**
- Create: `android/app/src/main/java/com/psy/PsyApplication.kt`
- Create: `android/app/src/main/java/com/psy/di/DatabaseModule.kt`
- Create: `android/app/src/main/java/com/psy/di/NetworkModule.kt`
- Modify: `android/app/src/main/java/com/psy/MainActivity.kt`
- Modify: `android/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Write the Application class**

`android/app/src/main/java/com/psy/PsyApplication.kt`:
```kotlin
package com.psy

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PsyApplication : Application()
```

- [ ] **Step 2: Register the Application in the manifest**

In `android/app/src/main/AndroidManifest.xml`, add `android:name=".PsyApplication"` to the
`<application>` tag, and add `<uses-permission android:name="android.permission.INTERNET" />`
above `<application>`.

- [ ] **Step 3: Write the database Hilt module**

`android/app/src/main/java/com/psy/di/DatabaseModule.kt`:
```kotlin
package com.psy.di

import android.content.Context
import androidx.room.Room
import com.psy.data.db.PsyDatabase
import com.psy.data.db.dao.LedgerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PsyDatabase =
        Room.databaseBuilder(context, PsyDatabase::class.java, "psy.db").build()

    @Provides
    fun provideLedgerDao(db: PsyDatabase): LedgerDao = db.ledgerDao()
}
```

- [ ] **Step 4: Write the network Hilt module**

`android/app/src/main/java/com/psy/di/NetworkModule.kt`:
```kotlin
package com.psy.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Emulator-to-host loopback. Override per build type in a later plan.
    private const val BASE_URL = "http://10.0.2.2:8080/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideRetrofit(json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
```

- [ ] **Step 5: Update MainActivity to use Hilt + PsyTheme**

`android/app/src/main/java/com/psy/MainActivity.kt`:
```kotlin
package com.psy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.psy.ui.theme.PsyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PsyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Text(text = "Psy", modifier = Modifier.padding(padding))
                }
            }
        }
    }
}
```

- [ ] **Step 6: Build and run the app**

Run:
```bash
cd /Users/hoalam/Codes/psy/android && ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`. Then install on an emulator/device from Android Studio and confirm it
launches showing "Psy" with the Candy Pop background — Hilt graph builds at startup with no crash.

- [ ] **Step 7: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add android/app/src/main/java/com/psy/ android/app/src/main/AndroidManifest.xml
git commit -m "feat(android): wire Hilt app, DI modules, and PsyTheme in MainActivity"
```

---

## Task 8: Go module + chi router with /health (TDD)

**Files:**
- Create: `backend/go.mod`
- Create: `backend/internal/config/config.go`
- Create: `backend/internal/api/router.go`
- Create: `backend/internal/api/health.go`
- Test: `backend/internal/api/health_test.go`

- [ ] **Step 1: Initialize the Go module and add chi**

Run:
```bash
cd /Users/hoalam/Codes/psy/backend
go mod init github.com/hoalam/psy/backend
go get github.com/go-chi/chi/v5@v5.1.0
```
Expected: `go.mod` and `go.sum` created with chi v5.

- [ ] **Step 2: Write the failing health test**

`backend/internal/api/health_test.go`:
```go
package api

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestHealthReturnsOK(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/health", nil)
	rec := httptest.NewRecorder()

	NewRouter().ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusOK)
	}

	var body map[string]string
	if err := json.NewDecoder(rec.Body).Decode(&body); err != nil {
		t.Fatalf("decode body: %v", err)
	}
	if body["status"] != "ok" {
		t.Fatalf("status field = %q, want %q", body["status"], "ok")
	}
}
```

- [ ] **Step 3: Run test to verify it fails**

Run:
```bash
cd /Users/hoalam/Codes/psy/backend && go test ./internal/api/
```
Expected: FAIL — `NewRouter` undefined (build error).

- [ ] **Step 4: Write the health handler**

`backend/internal/api/health.go`:
```go
package api

import (
	"encoding/json"
	"net/http"
)

func handleHealth(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}
```

- [ ] **Step 5: Write the router**

`backend/internal/api/router.go`:
```go
package api

import (
	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
)

// NewRouter builds the HTTP router with all routes mounted.
func NewRouter() chi.Router {
	r := chi.NewRouter()
	r.Use(middleware.Recoverer)
	r.Get("/health", handleHealth)
	return r
}
```

- [ ] **Step 6: Run test to verify it passes**

Run:
```bash
cd /Users/hoalam/Codes/psy/backend && go test ./internal/api/
```
Expected: PASS (`ok ... internal/api`).

- [ ] **Step 7: Write the config loader**

`backend/internal/config/config.go`:
```go
package config

import "os"

type Config struct {
	Port        string
	DatabaseURL string
}

func Load() Config {
	return Config{
		Port:        getenv("PORT", "8080"),
		DatabaseURL: getenv("DATABASE_URL", "postgres://psy:psy@localhost:5432/psy?sslmode=disable"),
	}
}

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
```

- [ ] **Step 8: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add backend/go.mod backend/go.sum backend/internal/
git commit -m "feat(backend): add chi router with /health and config loader"
```

---

## Task 9: Postgres connection + migration runner

**Files:**
- Create: `backend/internal/db/db.go`
- Create: `backend/internal/db/migrate.go`
- Create: `backend/internal/db/migrations/0001_init.sql`
- Test: `backend/internal/db/migrate_test.go`

Uses `pgxpool` for the connection and a tiny embedded-SQL migration runner (no external migration tool, per the zero-extra-tooling preference).

- [ ] **Step 1: Add pgx**

Run:
```bash
cd /Users/hoalam/Codes/psy/backend && go get github.com/jackc/pgx/v5@v5.7.1
```
Expected: pgx added to `go.mod`.

- [ ] **Step 2: Write the initial migration SQL**

`backend/internal/db/migrations/0001_init.sql`:
```sql
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    google_sub  TEXT NOT NULL UNIQUE,
    email       TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS snapshots (
    user_id     BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    version     BIGINT NOT NULL,
    blob        BYTEA NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

- [ ] **Step 3: Write the connection helper**

`backend/internal/db/db.go`:
```go
package db

import (
	"context"

	"github.com/jackc/pgx/v5/pgxpool"
)

// Connect opens a pgx connection pool and verifies it with a ping.
func Connect(ctx context.Context, url string) (*pgxpool.Pool, error) {
	pool, err := pgxpool.New(ctx, url)
	if err != nil {
		return nil, err
	}
	if err := pool.Ping(ctx); err != nil {
		pool.Close()
		return nil, err
	}
	return pool, nil
}
```

- [ ] **Step 4: Write the failing migration test**

This test is skipped unless `TEST_DATABASE_URL` is set, so the suite stays green without a DB but
exercises the real path in CI/local when a Postgres is available.

`backend/internal/db/migrate_test.go`:
```go
package db

import (
	"context"
	"os"
	"testing"
)

func TestMigrateCreatesTables(t *testing.T) {
	url := os.Getenv("TEST_DATABASE_URL")
	if url == "" {
		t.Skip("set TEST_DATABASE_URL to run migration test")
	}
	ctx := context.Background()

	pool, err := Connect(ctx, url)
	if err != nil {
		t.Fatalf("connect: %v", err)
	}
	defer pool.Close()

	if err := Migrate(ctx, pool); err != nil {
		t.Fatalf("migrate: %v", err)
	}

	var exists bool
	err = pool.QueryRow(ctx,
		`SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'snapshots')`,
	).Scan(&exists)
	if err != nil {
		t.Fatalf("query: %v", err)
	}
	if !exists {
		t.Fatal("snapshots table was not created")
	}
}
```

- [ ] **Step 5: Run test to verify it fails**

Run:
```bash
cd /Users/hoalam/Codes/psy/backend && go test ./internal/db/
```
Expected: FAIL — `Migrate` undefined (build error). (When `TEST_DATABASE_URL` is unset the test
would skip, but compilation fails first because `Migrate` does not exist.)

- [ ] **Step 6: Write the migration runner**

`backend/internal/db/migrate.go`:
```go
package db

import (
	"context"
	"embed"
	"sort"

	"github.com/jackc/pgx/v5/pgxpool"
)

//go:embed migrations/*.sql
var migrationFS embed.FS

// Migrate applies all embedded migration files in lexical order.
func Migrate(ctx context.Context, pool *pgxpool.Pool) error {
	entries, err := migrationFS.ReadDir("migrations")
	if err != nil {
		return err
	}
	names := make([]string, 0, len(entries))
	for _, e := range entries {
		names = append(names, e.Name())
	}
	sort.Strings(names)

	for _, name := range names {
		sqlBytes, err := migrationFS.ReadFile("migrations/" + name)
		if err != nil {
			return err
		}
		if _, err := pool.Exec(ctx, string(sqlBytes)); err != nil {
			return err
		}
	}
	return nil
}
```

- [ ] **Step 7: Run test to verify it passes (or skips cleanly)**

Run:
```bash
cd /Users/hoalam/Codes/psy/backend && go test ./internal/db/
```
Expected: PASS — either `ok` (skipped when `TEST_DATABASE_URL` unset) or full run if a test DB is configured.

- [ ] **Step 8: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add backend/go.mod backend/go.sum backend/internal/db/
git commit -m "feat(backend): add pgx connection and embedded migration runner"
```

---

## Task 10: Wire main.go and boot smoke test

**Files:**
- Create: `backend/cmd/server/main.go`
- Create: `backend/.env.example`

- [ ] **Step 1: Write main.go**

`backend/cmd/server/main.go`:
```go
package main

import (
	"context"
	"log"
	"net/http"

	"github.com/hoalam/psy/backend/internal/api"
	"github.com/hoalam/psy/backend/internal/config"
	"github.com/hoalam/psy/backend/internal/db"
)

func main() {
	cfg := config.Load()
	ctx := context.Background()

	pool, err := db.Connect(ctx, cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("db connect: %v", err)
	}
	defer pool.Close()

	if err := db.Migrate(ctx, pool); err != nil {
		log.Fatalf("migrate: %v", err)
	}

	r := api.NewRouter()
	log.Printf("listening on :%s", cfg.Port)
	if err := http.ListenAndServe(":"+cfg.Port, r); err != nil {
		log.Fatalf("serve: %v", err)
	}
}
```

- [ ] **Step 2: Write .env.example**

`backend/.env.example`:
```bash
PORT=8080
DATABASE_URL=postgres://psy:psy@localhost:5432/psy?sslmode=disable
```

- [ ] **Step 3: Verify it compiles and vets**

Run:
```bash
cd /Users/hoalam/Codes/psy/backend && go build ./... && go vet ./...
```
Expected: no output, exit 0.

- [ ] **Step 4: Manual boot smoke test (requires local Postgres)**

Start a Postgres (e.g. `docker run --rm -e POSTGRES_USER=psy -e POSTGRES_PASSWORD=psy -e POSTGRES_DB=psy -p 5432:5432 postgres:16`), then:
```bash
cd /Users/hoalam/Codes/psy/backend && go run ./cmd/server &
sleep 2 && curl -s localhost:8080/health
```
Expected: `{"status":"ok"}`. Stop the server afterward (`kill %1`).

- [ ] **Step 5: Run the full backend test suite**

Run:
```bash
cd /Users/hoalam/Codes/psy/backend && go test ./...
```
Expected: all packages `ok` (db test skips without `TEST_DATABASE_URL`).

- [ ] **Step 6: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add backend/cmd/ backend/.env.example
git commit -m "feat(backend): wire main.go to boot router + db migrations"
```

---

## Task 11: Phase 0 verification gate

**Files:** none (verification only)

- [ ] **Step 1: Android — full build + unit tests**

Run:
```bash
cd /Users/hoalam/Codes/psy/android && ./gradlew :app:assembleDebug :app:testDebugUnitTest
```
Expected: `BUILD SUCCESSFUL`; `MoneyTest` (4) and `LedgerDaoTest` (1) pass.

- [ ] **Step 2: Backend — build, vet, test**

Run:
```bash
cd /Users/hoalam/Codes/psy/backend && go build ./... && go vet ./... && go test ./...
```
Expected: exit 0, all packages `ok`.

- [ ] **Step 3: Confirm git history is clean and complete**

Run:
```bash
cd /Users/hoalam/Codes/psy && git --no-pager log --oneline && git status -s
```
Expected: commits for tasks 1–10 present; working tree clean.

---

## Self-Review Notes

- **Spec coverage (Phase 0 scope):** monorepo layout (§4) → Tasks 1, 8–10; Android stack Hilt/Room/Compose/Retrofit (§3) → Tasks 2–3, 6–7; Candy Pop design system (§3) → Task 5; minor-unit amounts (§5) → Task 4; Go chi + Postgres + initial schema for users/snapshots (§3, §7) → Tasks 8–9. Feature logic (CRUD, stats, budget, backup, auth, lock) is intentionally out of Phase 0 and covered by Plans 1–5.
- **Type consistency:** `NewRouter()` (Task 8) reused in Tasks 9–10; `Connect`/`Migrate` signatures (Task 9) match call sites in `main.go` (Task 10); `PsyDatabase.ledgerDao()` (Task 6) matches `DatabaseModule` (Task 7); `Money.formatMinor(amountMinor, fractionDigits, suffix)` is self-contained.
- **No placeholders:** every code step contains complete, runnable content.
