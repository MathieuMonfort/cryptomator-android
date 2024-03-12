package org.cryptomator.data.db.cachecontrol

import android.content.ContentValues
import android.database.Cursor
import android.os.CancellationSignal
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import java.util.Collections
import java.util.UUID

internal class CacheControlledSupportSQLiteDatabase(
	private val delegate: SupportSQLiteDatabase
) : SupportSQLiteDatabase by delegate {

	override fun execSQL(sql: String) {
		return delegate.execSQL(fix(sql))
	}

	override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
		return delegate.execSQL(fix(sql), bindArgs)
	}

	override fun query(query: SupportSQLiteQuery): Cursor {
		return delegate.query(fix(query))
	}

	override fun query(query: SupportSQLiteQuery, cancellationSignal: CancellationSignal?): Cursor {
		return delegate.query(fix(query), cancellationSignal)
	}

	override fun query(query: String): Cursor {
		return delegate.query(fix(query))
	}

	override fun query(query: String, bindArgs: Array<out Any?>): Cursor {
		return delegate.query(fix(query), bindArgs)
	}

	override fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long {
		val processed = helper.insertWithOnConflict(table, null, values, conflictAlgorithm)
		val statement = fixCompile(processed.sql, delegate)
		SimpleSQLiteQuery.bind(statement, processed.bindArgs)

		return statement.executeInsert()
	}

	override fun update(
		table: String,
		conflictAlgorithm: Int,
		values: ContentValues,
		whereClause: String?,
		whereArgs: Array<out Any?>?
	): Int {
		return delegate.update(table, conflictAlgorithm, values, fixWhereClause(whereClause), whereArgs)
	}

	override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int {
		return delegate.delete(table, fixWhereClause(whereClause), whereArgs)
	}

	override fun execPerConnectionSQL(sql: String, bindArgs: Array<out Any?>?) {
		delegate.execPerConnectionSQL(fix(sql), bindArgs)
	}

	override fun compileStatement(sql: String): SupportSQLiteStatement {
		return fixCompile(sql, delegate)
	}
}

private val helper = AOP_SQLiteDatabase()

private val newIdentifier: String
	get() = UUID.randomUUID().toString()

private fun fix(sql: String, statementIdentifier: String = newIdentifier): String {
	return "$sql -- $statementIdentifier"
}

private fun fix(query: SupportSQLiteQuery): SupportSQLiteQuery {
	return CacheControlledSupportSQLiteQuery(query)
}

private fun fixCompile(sql: String, compilerDelegate: SupportSQLiteDatabase): SupportSQLiteStatement {
	return CacheControlledSupportSQLiteStatement(sql, compilerDelegate)
}

private fun fixWhereClause(whereClause: String?): String {
	if (whereClause != null && whereClause.isBlank()) {
		throw IllegalArgumentException()
	}
	return fix(whereClause ?: "1 = 1")
}

private class CacheControlledSupportSQLiteStatement(
	private val sql: String,
	private val delegate: SupportSQLiteDatabase, //This is *not* the owning database, but *the delegate of* the owning database
) : SupportSQLiteStatement {

	private val bindings = mutableListOf<(SupportSQLiteStatement) -> Unit>()

	override fun bindBlob(index: Int, value: ByteArray) {
		bindings.add { statement -> statement.bindBlob(index, value) }
	}

	override fun bindDouble(index: Int, value: Double) {
		bindings.add { statement -> statement.bindDouble(index, value) }
	}

	override fun bindLong(index: Int, value: Long) {
		bindings.add { statement -> statement.bindLong(index, value) }
	}

	override fun bindNull(index: Int) {
		bindings.add { statement -> statement.bindNull(index) }
	}

	override fun bindString(index: Int, value: String) {
		bindings.add { statement -> statement.bindString(index, value) }
	}

	override fun clearBindings() {
		bindings.clear()
	}

	override fun close() {
		//NO-OP
	}

	override fun execute() {
		newBoundStatement().use { it.execute() }
	}

	override fun executeInsert(): Long {
		return newBoundStatement().use { it.executeInsert() }
	}

	override fun executeUpdateDelete(): Int {
		return newBoundStatement().use { it.executeUpdateDelete() }
	}

	override fun simpleQueryForLong(): Long {
		return newBoundStatement().use { it.simpleQueryForLong() }
	}

	override fun simpleQueryForString(): String? {
		return newBoundStatement().use { it.simpleQueryForString() }
	}

	private fun newBoundStatement(): SupportSQLiteStatement {
		return delegate.compileStatement(fix(sql)).also { statement ->
			for (binding: (SupportSQLiteStatement) -> Unit in bindings) {
				binding(statement)
			}
		}
	}
}

private class CacheControlledSupportSQLiteQuery(
	private val delegate: SupportSQLiteQuery
) : SupportSQLiteQuery by delegate {

	private val identifiers: MutableMap<String, String> = Collections.synchronizedMap(mutableMapOf())

	override val sql: String
		get() = fix(delegate.sql, identifiers.computeIfAbsent(delegate.sql) { newIdentifier })
}

private class CacheControlledSupportSQLiteOpenHelper(
	private val delegate: SupportSQLiteOpenHelper
) : SupportSQLiteOpenHelper by delegate {

	override val writableDatabase: SupportSQLiteDatabase
		get() = CacheControlledSupportSQLiteDatabase(delegate.writableDatabase)

	override val readableDatabase: SupportSQLiteDatabase
		get() = CacheControlledSupportSQLiteDatabase(delegate.readableDatabase)
}

class CacheControlledSupportSQLiteOpenHelperFactory(
	private val delegate: SupportSQLiteOpenHelper.Factory
) : SupportSQLiteOpenHelper.Factory {

	override fun create(
		configuration: SupportSQLiteOpenHelper.Configuration
	): SupportSQLiteOpenHelper {
		return CacheControlledSupportSQLiteOpenHelper(delegate.create(configuration))
	}
}

fun SupportSQLiteOpenHelper.Factory.asCacheControlled(): SupportSQLiteOpenHelper.Factory {
	return CacheControlledSupportSQLiteOpenHelperFactory(this)
}