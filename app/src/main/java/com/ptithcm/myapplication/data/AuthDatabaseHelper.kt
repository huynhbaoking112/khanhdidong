package com.ptithcm.myapplication.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest
import java.util.Locale

enum class UserRole(val value: String) {
    ADMIN("Admin"),
    MANAGER("Manager"),
    MEMBER("Member");

    companion object {
        fun fromValue(value: String): UserRole =
            values().firstOrNull { it.value == value } ?: MEMBER
    }
}

data class UserSession(
    val id: Long,
    val username: String,
    val fullName: String,
    val role: UserRole
)

data class UserAccount(
    val id: Long,
    val username: String,
    val fullName: String,
    val role: UserRole,
    val isActive: Boolean
)

enum class UserSaveResult {
    SUCCESS,
    USERNAME_EXISTS,
    NOT_FOUND
}

class AuthDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USERNAME TEXT NOT NULL UNIQUE COLLATE NOCASE,
                $COL_PASSWORD_HASH TEXT NOT NULL,
                $COL_FULL_NAME TEXT NOT NULL,
                $COL_ROLE TEXT NOT NULL CHECK($COL_ROLE IN ('Admin', 'Manager', 'Member')),
                $COL_IS_ACTIVE INTEGER NOT NULL DEFAULT 1,
                $COL_IS_DELETED INTEGER NOT NULL DEFAULT 0,
                $COL_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )

        seedUser(db, "admin", "admin123", "System Admin", UserRole.ADMIN)
        seedUser(db, "manager", "manager123", "Project Manager", UserRole.MANAGER)
        seedUser(db, "member", "member123", "Team Member", UserRole.MEMBER)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_IS_ACTIVE INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_IS_DELETED INTEGER NOT NULL DEFAULT 0")
        }
    }

    fun login(username: String, password: String): UserSession? {
        val cleanUsername = username.trim().lowercase(Locale.US)
        if (cleanUsername.isEmpty() || password.isEmpty()) return null

        val cursor = readableDatabase.query(
            TABLE_USERS,
            USER_COLUMNS,
            "$COL_USERNAME = ? COLLATE NOCASE AND $COL_PASSWORD_HASH = ? AND $COL_IS_ACTIVE = 1 AND $COL_IS_DELETED = 0",
            arrayOf(cleanUsername, hashPassword(password)),
            null,
            null,
            null,
            "1"
        )

        return cursor.use { if (it.moveToFirst()) it.toUserSession() else null }
    }

    fun getUserById(userId: Long): UserSession? {
        val cursor = readableDatabase.query(
            TABLE_USERS,
            USER_COLUMNS,
            "$COL_ID = ? AND $COL_IS_ACTIVE = 1 AND $COL_IS_DELETED = 0",
            arrayOf(userId.toString()),
            null,
            null,
            null,
            "1"
        )

        return cursor.use { if (it.moveToFirst()) it.toUserSession() else null }
    }

    fun changePassword(userId: Long, currentPassword: String, newPassword: String): Boolean {
        val currentHash = hashPassword(currentPassword)
        val newHash = hashPassword(newPassword)
        val userExists = readableDatabase.query(
            TABLE_USERS,
            arrayOf(COL_ID),
            "$COL_ID = ? AND $COL_PASSWORD_HASH = ? AND $COL_IS_ACTIVE = 1 AND $COL_IS_DELETED = 0",
            arrayOf(userId.toString(), currentHash),
            null,
            null,
            null,
            "1"
        ).use { it.moveToFirst() }

        if (!userExists) return false

        val values = ContentValues().apply {
            put(COL_PASSWORD_HASH, newHash)
        }
        return writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_ID = ?",
            arrayOf(userId.toString())
        ) == 1
    }

    fun listUsers(): List<UserAccount> {
        val cursor = readableDatabase.query(
            TABLE_USERS,
            ACCOUNT_COLUMNS,
            "$COL_IS_DELETED = 0",
            null,
            null,
            null,
            "$COL_CREATED_AT ASC"
        )

        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(it.toUserAccount())
                }
            }
        }
    }

    fun createUser(
        username: String,
        password: String,
        fullName: String,
        role: UserRole
    ): UserSaveResult {
        val cleanUsername = username.trim().lowercase(Locale.US)
        if (usernameExists(cleanUsername)) return UserSaveResult.USERNAME_EXISTS

        val values = ContentValues().apply {
            put(COL_USERNAME, cleanUsername)
            put(COL_PASSWORD_HASH, hashPassword(password))
            put(COL_FULL_NAME, fullName.trim())
            put(COL_ROLE, role.value)
            put(COL_IS_ACTIVE, 1)
            put(COL_IS_DELETED, 0)
            put(COL_CREATED_AT, System.currentTimeMillis())
        }

        return if (writableDatabase.insert(TABLE_USERS, null, values) == -1L) {
            UserSaveResult.USERNAME_EXISTS
        } else {
            UserSaveResult.SUCCESS
        }
    }

    fun updateUser(userId: Long, fullName: String, role: UserRole): UserSaveResult {
        val values = ContentValues().apply {
            put(COL_FULL_NAME, fullName.trim())
            put(COL_ROLE, role.value)
        }

        val updatedRows = writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_ID = ? AND $COL_IS_DELETED = 0",
            arrayOf(userId.toString())
        )
        return if (updatedRows == 1) UserSaveResult.SUCCESS else UserSaveResult.NOT_FOUND
    }

    fun setUserActive(userId: Long, isActive: Boolean): Boolean {
        val values = ContentValues().apply {
            put(COL_IS_ACTIVE, if (isActive) 1 else 0)
        }
        return writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_ID = ? AND $COL_IS_DELETED = 0",
            arrayOf(userId.toString())
        ) == 1
    }

    fun deleteUser(userId: Long): Boolean {
        val values = ContentValues().apply {
            put(COL_IS_DELETED, 1)
            put(COL_IS_ACTIVE, 0)
        }
        return writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_ID = ? AND $COL_IS_DELETED = 0",
            arrayOf(userId.toString())
        ) == 1
    }

    private fun seedUser(
        db: SQLiteDatabase,
        username: String,
        password: String,
        fullName: String,
        role: UserRole
    ) {
        val values = ContentValues().apply {
            put(COL_USERNAME, username)
            put(COL_PASSWORD_HASH, hashPassword(password))
            put(COL_FULL_NAME, fullName)
            put(COL_ROLE, role.value)
            put(COL_IS_ACTIVE, 1)
            put(COL_IS_DELETED, 0)
            put(COL_CREATED_AT, System.currentTimeMillis())
        }
        db.insert(TABLE_USERS, null, values)
    }

    private fun usernameExists(username: String): Boolean = readableDatabase.query(
        TABLE_USERS,
        arrayOf(COL_ID),
        "$COL_USERNAME = ? COLLATE NOCASE",
        arrayOf(username),
        null,
        null,
        null,
        "1"
    ).use { it.moveToFirst() }

    private fun Cursor.toUserSession(): UserSession = UserSession(
        id = getLong(getColumnIndexOrThrow(COL_ID)),
        username = getString(getColumnIndexOrThrow(COL_USERNAME)),
        fullName = getString(getColumnIndexOrThrow(COL_FULL_NAME)),
        role = UserRole.fromValue(getString(getColumnIndexOrThrow(COL_ROLE)))
    )

    private fun Cursor.toUserAccount(): UserAccount = UserAccount(
        id = getLong(getColumnIndexOrThrow(COL_ID)),
        username = getString(getColumnIndexOrThrow(COL_USERNAME)),
        fullName = getString(getColumnIndexOrThrow(COL_FULL_NAME)),
        role = UserRole.fromValue(getString(getColumnIndexOrThrow(COL_ROLE))),
        isActive = getInt(getColumnIndexOrThrow(COL_IS_ACTIVE)) == 1
    )

    companion object {
        private const val DATABASE_NAME = "task_manager_auth.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_USERS = "users"
        private const val COL_ID = "id"
        private const val COL_USERNAME = "username"
        private const val COL_PASSWORD_HASH = "password_hash"
        private const val COL_FULL_NAME = "full_name"
        private const val COL_ROLE = "role"
        private const val COL_IS_ACTIVE = "is_active"
        private const val COL_IS_DELETED = "is_deleted"
        private const val COL_CREATED_AT = "created_at"

        private val USER_COLUMNS = arrayOf(COL_ID, COL_USERNAME, COL_FULL_NAME, COL_ROLE)
        private val ACCOUNT_COLUMNS = arrayOf(
            COL_ID,
            COL_USERNAME,
            COL_FULL_NAME,
            COL_ROLE,
            COL_IS_ACTIVE
        )

        private fun hashPassword(password: String): String {
            val input = "task-manager-auth:$password".toByteArray(Charsets.UTF_8)
            return MessageDigest.getInstance("SHA-256")
                .digest(input)
                .joinToString(separator = "") { "%02x".format(it) }
        }
    }
}
