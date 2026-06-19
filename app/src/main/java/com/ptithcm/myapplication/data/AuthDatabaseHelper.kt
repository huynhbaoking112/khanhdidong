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
                $COL_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )

        seedUser(db, "admin", "admin123", "System Admin", UserRole.ADMIN)
        seedUser(db, "manager", "manager123", "Project Manager", UserRole.MANAGER)
        seedUser(db, "member", "member123", "Team Member", UserRole.MEMBER)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun login(username: String, password: String): UserSession? {
        val cleanUsername = username.trim().lowercase(Locale.US)
        if (cleanUsername.isEmpty() || password.isEmpty()) return null

        val cursor = readableDatabase.query(
            TABLE_USERS,
            USER_COLUMNS,
            "$COL_USERNAME = ? COLLATE NOCASE AND $COL_PASSWORD_HASH = ?",
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
            "$COL_ID = ?",
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
            "$COL_ID = ? AND $COL_PASSWORD_HASH = ?",
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
            put(COL_CREATED_AT, System.currentTimeMillis())
        }
        db.insert(TABLE_USERS, null, values)
    }

    private fun Cursor.toUserSession(): UserSession = UserSession(
        id = getLong(getColumnIndexOrThrow(COL_ID)),
        username = getString(getColumnIndexOrThrow(COL_USERNAME)),
        fullName = getString(getColumnIndexOrThrow(COL_FULL_NAME)),
        role = UserRole.fromValue(getString(getColumnIndexOrThrow(COL_ROLE)))
    )

    companion object {
        private const val DATABASE_NAME = "task_manager_auth.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_USERS = "users"
        private const val COL_ID = "id"
        private const val COL_USERNAME = "username"
        private const val COL_PASSWORD_HASH = "password_hash"
        private const val COL_FULL_NAME = "full_name"
        private const val COL_ROLE = "role"
        private const val COL_CREATED_AT = "created_at"

        private val USER_COLUMNS = arrayOf(COL_ID, COL_USERNAME, COL_FULL_NAME, COL_ROLE)

        private fun hashPassword(password: String): String {
            val input = "task-manager-auth:$password".toByteArray(Charsets.UTF_8)
            return MessageDigest.getInstance("SHA-256")
                .digest(input)
                .joinToString(separator = "") { "%02x".format(it) }
        }
    }
}
