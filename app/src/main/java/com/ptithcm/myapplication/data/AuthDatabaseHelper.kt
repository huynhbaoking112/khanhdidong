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

enum class ProjectStatus(val value: String) {
    PLANNING("Planning"),
    ACTIVE("Active"),
    COMPLETED("Completed");

    companion object {
        fun fromValue(value: String): ProjectStatus =
            values().firstOrNull { it.value == value } ?: PLANNING
    }
}

data class ProjectSummary(
    val id: Long,
    val name: String,
    val description: String,
    val status: ProjectStatus,
    val createdByName: String,
    val memberIds: Set<Long>,
    val memberNames: List<String>
)

enum class ProjectSaveResult {
    SUCCESS,
    DUPLICATE_NAME,
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
        createProjectTables(db)
        seedProject(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_IS_ACTIVE INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_IS_DELETED INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 3) {
            createProjectTables(db)
            seedProject(db)
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

    fun listProjectsForUser(user: UserSession): List<ProjectSummary> {
        val whereClause = when (user.role) {
            UserRole.ADMIN -> "p.$PROJECT_IS_DELETED = 0"
            UserRole.MANAGER -> "p.$PROJECT_IS_DELETED = 0 AND p.$PROJECT_CREATED_BY = ?"
            UserRole.MEMBER -> "p.$PROJECT_IS_DELETED = 0 AND p.$PROJECT_ID IN (SELECT $MEMBER_PROJECT_ID FROM $TABLE_PROJECT_MEMBERS WHERE $MEMBER_USER_ID = ?)"
        }
        val whereArgs = when (user.role) {
            UserRole.ADMIN -> emptyArray()
            else -> arrayOf(user.id.toString())
        }
        val sql = """
            SELECT p.$PROJECT_ID, p.$PROJECT_NAME, p.$PROJECT_DESCRIPTION, p.$PROJECT_STATUS,
                   u.$COL_FULL_NAME AS creator_name
            FROM $TABLE_PROJECTS p
            JOIN $TABLE_USERS u ON u.$COL_ID = p.$PROJECT_CREATED_BY
            WHERE $whereClause
            ORDER BY p.$PROJECT_CREATED_AT DESC
        """.trimIndent()

        return readableDatabase.rawQuery(sql, whereArgs).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val projectId = cursor.getLong(cursor.getColumnIndexOrThrow(PROJECT_ID))
                    add(
                        ProjectSummary(
                            id = projectId,
                            name = cursor.getString(cursor.getColumnIndexOrThrow(PROJECT_NAME)),
                            description = cursor.getString(cursor.getColumnIndexOrThrow(PROJECT_DESCRIPTION)),
                            status = ProjectStatus.fromValue(
                                cursor.getString(cursor.getColumnIndexOrThrow(PROJECT_STATUS))
                            ),
                            createdByName = cursor.getString(cursor.getColumnIndexOrThrow("creator_name")),
                            memberIds = getProjectMemberIds(projectId),
                            memberNames = getProjectMemberNames(projectId)
                        )
                    )
                }
            }
        }
    }

    fun createProject(
        name: String,
        description: String,
        status: ProjectStatus,
        createdBy: Long,
        memberIds: Set<Long>
    ): ProjectSaveResult {
        if (projectNameExists(name.trim())) return ProjectSaveResult.DUPLICATE_NAME

        val db = writableDatabase
        db.beginTransaction()
        return try {
            val projectId = db.insert(
                TABLE_PROJECTS,
                null,
                ContentValues().apply {
                    put(PROJECT_NAME, name.trim())
                    put(PROJECT_DESCRIPTION, description.trim())
                    put(PROJECT_STATUS, status.value)
                    put(PROJECT_CREATED_BY, createdBy)
                    put(PROJECT_IS_DELETED, 0)
                    put(PROJECT_CREATED_AT, System.currentTimeMillis())
                }
            )
            if (projectId == -1L) return ProjectSaveResult.DUPLICATE_NAME

            replaceProjectMembers(db, projectId, memberIds + createdBy)
            db.setTransactionSuccessful()
            ProjectSaveResult.SUCCESS
        } finally {
            db.endTransaction()
        }
    }

    fun updateProject(
        projectId: Long,
        name: String,
        description: String,
        status: ProjectStatus,
        memberIds: Set<Long>
    ): ProjectSaveResult {
        if (projectNameExists(name.trim(), exceptProjectId = projectId)) return ProjectSaveResult.DUPLICATE_NAME

        val db = writableDatabase
        db.beginTransaction()
        return try {
            val updatedRows = db.update(
                TABLE_PROJECTS,
                ContentValues().apply {
                    put(PROJECT_NAME, name.trim())
                    put(PROJECT_DESCRIPTION, description.trim())
                    put(PROJECT_STATUS, status.value)
                },
                "$PROJECT_ID = ? AND $PROJECT_IS_DELETED = 0",
                arrayOf(projectId.toString())
            )
            if (updatedRows != 1) return ProjectSaveResult.NOT_FOUND

            replaceProjectMembers(db, projectId, memberIds)
            db.setTransactionSuccessful()
            ProjectSaveResult.SUCCESS
        } finally {
            db.endTransaction()
        }
    }

    fun deleteProject(projectId: Long): Boolean {
        val values = ContentValues().apply {
            put(PROJECT_IS_DELETED, 1)
        }
        return writableDatabase.update(
            TABLE_PROJECTS,
            values,
            "$PROJECT_ID = ? AND $PROJECT_IS_DELETED = 0",
            arrayOf(projectId.toString())
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

    private fun createProjectTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PROJECTS (
                $PROJECT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $PROJECT_NAME TEXT NOT NULL UNIQUE COLLATE NOCASE,
                $PROJECT_DESCRIPTION TEXT NOT NULL DEFAULT '',
                $PROJECT_STATUS TEXT NOT NULL CHECK($PROJECT_STATUS IN ('Planning', 'Active', 'Completed')),
                $PROJECT_CREATED_BY INTEGER NOT NULL,
                $PROJECT_IS_DELETED INTEGER NOT NULL DEFAULT 0,
                $PROJECT_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($PROJECT_CREATED_BY) REFERENCES $TABLE_USERS($COL_ID)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PROJECT_MEMBERS (
                $MEMBER_PROJECT_ID INTEGER NOT NULL,
                $MEMBER_USER_ID INTEGER NOT NULL,
                PRIMARY KEY($MEMBER_PROJECT_ID, $MEMBER_USER_ID),
                FOREIGN KEY($MEMBER_PROJECT_ID) REFERENCES $TABLE_PROJECTS($PROJECT_ID),
                FOREIGN KEY($MEMBER_USER_ID) REFERENCES $TABLE_USERS($COL_ID)
            )
            """.trimIndent()
        )
    }

    private fun seedProject(db: SQLiteDatabase) {
        val hasProject = db.query(
            TABLE_PROJECTS,
            arrayOf(PROJECT_ID),
            null,
            null,
            null,
            null,
            null,
            "1"
        ).use { it.moveToFirst() }
        if (hasProject) return

        val adminId = findUserId(db, "admin") ?: return
        val managerId = findUserId(db, "manager") ?: return
        val memberId = findUserId(db, "member") ?: return
        val projectId = db.insert(
            TABLE_PROJECTS,
            null,
            ContentValues().apply {
                put(PROJECT_NAME, "Task Manager Mobile App")
                put(PROJECT_DESCRIPTION, "Build the course project with authentication, projects and tasks.")
                put(PROJECT_STATUS, ProjectStatus.ACTIVE.value)
                put(PROJECT_CREATED_BY, managerId)
                put(PROJECT_IS_DELETED, 0)
                put(PROJECT_CREATED_AT, System.currentTimeMillis())
            }
        )
        if (projectId != -1L) replaceProjectMembers(db, projectId, setOf(adminId, managerId, memberId))
    }

    private fun findUserId(db: SQLiteDatabase, username: String): Long? = db.query(
        TABLE_USERS,
        arrayOf(COL_ID),
        "$COL_USERNAME = ? COLLATE NOCASE AND $COL_IS_DELETED = 0",
        arrayOf(username),
        null,
        null,
        null,
        "1"
    ).use { if (it.moveToFirst()) it.getLong(it.getColumnIndexOrThrow(COL_ID)) else null }

    private fun replaceProjectMembers(db: SQLiteDatabase, projectId: Long, memberIds: Set<Long>) {
        db.delete(TABLE_PROJECT_MEMBERS, "$MEMBER_PROJECT_ID = ?", arrayOf(projectId.toString()))
        memberIds.forEach { userId ->
            db.insert(
                TABLE_PROJECT_MEMBERS,
                null,
                ContentValues().apply {
                    put(MEMBER_PROJECT_ID, projectId)
                    put(MEMBER_USER_ID, userId)
                }
            )
        }
    }

    private fun getProjectMemberIds(projectId: Long): Set<Long> = readableDatabase.query(
        TABLE_PROJECT_MEMBERS,
        arrayOf(MEMBER_USER_ID),
        "$MEMBER_PROJECT_ID = ?",
        arrayOf(projectId.toString()),
        null,
        null,
        null
    ).use { cursor ->
        buildSet {
            while (cursor.moveToNext()) add(cursor.getLong(cursor.getColumnIndexOrThrow(MEMBER_USER_ID)))
        }
    }

    private fun getProjectMemberNames(projectId: Long): List<String> {
        val sql = """
            SELECT u.$COL_FULL_NAME
            FROM $TABLE_PROJECT_MEMBERS pm
            JOIN $TABLE_USERS u ON u.$COL_ID = pm.$MEMBER_USER_ID
            WHERE pm.$MEMBER_PROJECT_ID = ? AND u.$COL_IS_DELETED = 0
            ORDER BY u.$COL_FULL_NAME ASC
        """.trimIndent()
        return readableDatabase.rawQuery(sql, arrayOf(projectId.toString())).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow(COL_FULL_NAME)))
            }
        }
    }

    private fun projectNameExists(name: String, exceptProjectId: Long? = null): Boolean {
        val where = if (exceptProjectId == null) {
            "$PROJECT_NAME = ? COLLATE NOCASE AND $PROJECT_IS_DELETED = 0"
        } else {
            "$PROJECT_NAME = ? COLLATE NOCASE AND $PROJECT_ID != ? AND $PROJECT_IS_DELETED = 0"
        }
        val args = if (exceptProjectId == null) arrayOf(name) else arrayOf(name, exceptProjectId.toString())
        return readableDatabase.query(
            TABLE_PROJECTS,
            arrayOf(PROJECT_ID),
            where,
            args,
            null,
            null,
            null,
            "1"
        ).use { it.moveToFirst() }
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
        private const val DATABASE_VERSION = 3

        private const val TABLE_USERS = "users"
        private const val COL_ID = "id"
        private const val COL_USERNAME = "username"
        private const val COL_PASSWORD_HASH = "password_hash"
        private const val COL_FULL_NAME = "full_name"
        private const val COL_ROLE = "role"
        private const val COL_IS_ACTIVE = "is_active"
        private const val COL_IS_DELETED = "is_deleted"
        private const val COL_CREATED_AT = "created_at"

        private const val TABLE_PROJECTS = "projects"
        private const val PROJECT_ID = "id"
        private const val PROJECT_NAME = "name"
        private const val PROJECT_DESCRIPTION = "description"
        private const val PROJECT_STATUS = "status"
        private const val PROJECT_CREATED_BY = "created_by"
        private const val PROJECT_IS_DELETED = "is_deleted"
        private const val PROJECT_CREATED_AT = "created_at"

        private const val TABLE_PROJECT_MEMBERS = "project_members"
        private const val MEMBER_PROJECT_ID = "project_id"
        private const val MEMBER_USER_ID = "user_id"

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
