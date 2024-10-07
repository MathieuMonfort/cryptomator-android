package org.cryptomator.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import org.cryptomator.data.db.entities.CloudEntity
import org.cryptomator.data.db.entities.UpdateCheckEntity
import org.cryptomator.data.db.entities.VaultEntity

@Database(version = 13, entities = [CloudEntity::class, UpdateCheckEntity::class, VaultEntity::class])
abstract class CryptomatorDatabase : RoomDatabase() {

}