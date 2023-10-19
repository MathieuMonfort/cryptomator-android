package org.cryptomator.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.cryptomator.data.db.migrations.Migration12To13
import javax.inject.Singleton
import dagger.Module
import dagger.Provides
import timber.log.Timber

@Module
class DatabaseModule {

	@Singleton
	@Provides
	fun provideCryptomatorDatabase(context: Context, migrations: Array<Migration>): CryptomatorDatabase {
		Timber.tag("Database").i("Building database")
		return Room.databaseBuilder(context, CryptomatorDatabase::class.java, "Cryptomator") //
			.addMigrations(*migrations) //
			.addMigrations(Migration12To13) //
			.addCallback(DatabaseCallback) //
			.build() //Fails if no migration is found (especially when downgrading)
			.also { //
				Timber.tag("Database").i("Database built successfully")
			}
	}

	@Singleton
	@Provides
	fun provideCloudDao(database: CryptomatorDatabase): CloudDao {
		return database.cloudDao()
	}

	@Singleton
	@Provides
	fun provideUpdateCheckDao(database: CryptomatorDatabase): UpdateCheckDao {
		return database.updateCheckDao()
	}

	@Singleton
	@Provides
	fun provideVaultDao(database: CryptomatorDatabase): VaultDao {
		return database.vaultDao()
	}

	@Singleton
	@Provides
	internal fun provideMigrations(
		upgrade0To1: Upgrade0To1, //
		upgrade1To2: Upgrade1To2, //
		upgrade2To3: Upgrade2To3, //
		upgrade3To4: Upgrade3To4, //
		upgrade4To5: Upgrade4To5, //
		upgrade5To6: Upgrade5To6, //
		upgrade6To7: Upgrade6To7, //
		upgrade7To8: Upgrade7To8, //
		upgrade8To9: Upgrade8To9, //
		upgrade9To10: Upgrade9To10, //
		upgrade10To11: Upgrade10To11, //
		upgrade11To12: Upgrade11To12, //
	): Array<Migration> = arrayOf(
		upgrade0To1,
		upgrade1To2,
		upgrade2To3,
		upgrade5To6,
		upgrade3To4,
		upgrade4To5,
		upgrade6To7,
		upgrade7To8,
		upgrade8To9,
		upgrade9To10,
		upgrade10To11,
		upgrade11To12,
	)
}

object DatabaseCallback : RoomDatabase.Callback() {

	override fun onCreate(db: SupportSQLiteDatabase) {
		Timber.tag("Database").i("Created database (v%s)", db.version)
	}

	override fun onOpen(db: SupportSQLiteDatabase) {
		Timber.tag("Database").i("Opened database (v%s)", db.version)
	}

	override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
		//This should not be called
		throw UnsupportedOperationException("Destructive migration is not supported")
	}
}