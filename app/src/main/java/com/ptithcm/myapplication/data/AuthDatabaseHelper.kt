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
    HAS_TASKS,
    HAS_ASSIGNED_TASKS,
    NOT_FOUND
}

enum class TaskStatus(val value: String) {
    TODO("Todo"),
    DOING("Doing"),
    DONE("Done"),
    OVERDUE("Overdue");

    companion object {
        fun fromValue(value: String): TaskStatus =
            values().firstOrNull { it.value == value } ?: TODO
    }
}

enum class TaskPriority(val value: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    companion object {
        fun fromValue(value: String): TaskPriority =
            values().firstOrNull { it.value == value } ?: MEDIUM
    }
}

data class TaskItem(
    val id: Long,
    val title: String,
    val description: String,
    val projectId: Long,
    val projectName: String,
    val assigneeId: Long,
    val assigneeName: String,
    val status: TaskStatus,
    val priority: TaskPriority,
    val dueDate: String,
    val progress: Int,
    val notes: String,
    val isDeleted: Boolean
)

data class TaskAttachment(
    val id: Long,
    val taskId: Long,
    val displayName: String,
    val uri: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long
)

data class TaskComment(
    val id: Long,
    val taskId: Long,
    val authorName: String,
    val content: String,
    val createdAt: Long
)

data class TaskHistoryEntry(
    val id: Long,
    val taskId: Long,
    val actorName: String,
    val description: String,
    val createdAt: Long
)

enum class TaskSaveResult {
    SUCCESS,
    INVALID_ASSIGNEE,
    NOT_FOUND
}

data class DashboardStats(
    val totalProjects: Int,
    val totalTasks: Int,
    val todoTasks: Int,
    val doingTasks: Int,
    val doneTasks: Int,
    val overdueTasks: Int,
    val highPriorityTasks: Int
)

data class ReportMetric(
    val label: String,
    val count: Int
)

data class ProjectReport(
    val projectName: String,
    val totalTasks: Int,
    val doneTasks: Int
)

data class MemberPerformanceReport(
    val memberName: String,
    val totalTasks: Int,
    val doneTasks: Int
)

data class ReportData(
    val totalProjects: Int,
    val totalTasks: Int,
    val completionPercent: Int,
    val statusMetrics: List<ReportMetric>,
    val priorityMetrics: List<ReportMetric>,
    val projectReports: List<ProjectReport>,
    val memberPerformance: List<MemberPerformanceReport>
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
        createTaskTable(db)
        createTaskAttachmentTable(db)
        createTaskCollaborationTables(db)
        seedTask(db)
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
        if (oldVersion < 4) {
            createTaskTable(db)
            seedTask(db)
        }
        if (oldVersion >= 4 && oldVersion < 5) {
            db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $TASK_PROGRESS INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $TASK_NOTES TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 6) {
            createTaskAttachmentTable(db)
        }
        if (oldVersion < 7) {
            createTaskCollaborationTables(db)
        }
        if (oldVersion >= 6 && oldVersion < 8) {
            db.execSQL("ALTER TABLE $TABLE_TASK_ATTACHMENTS ADD COLUMN $ATTACHMENT_MIME_TYPE TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE $TABLE_TASK_ATTACHMENTS ADD COLUMN $ATTACHMENT_SIZE_BYTES INTEGER NOT NULL DEFAULT 0")
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

    fun updateProfile(userId: Long, fullName: String): UserSaveResult {
        val values = ContentValues().apply {
            put(COL_FULL_NAME, fullName.trim())
        }

        val updatedRows = writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_ID = ? AND $COL_IS_ACTIVE = 1 AND $COL_IS_DELETED = 0",
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
            UserRole.ADMIN -> emptyArray<String>()
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

        val creatorId = getProjectCreatorId(projectId) ?: return ProjectSaveResult.NOT_FOUND
        val safeMemberIds = memberIds + creatorId
        if (hasTasksAssignedOutsideProjectMembers(projectId, safeMemberIds)) {
            return ProjectSaveResult.HAS_ASSIGNED_TASKS
        }

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

            replaceProjectMembers(db, projectId, safeMemberIds)
            db.setTransactionSuccessful()
            ProjectSaveResult.SUCCESS
        } finally {
            db.endTransaction()
        }
    }

    fun deleteProject(projectId: Long): ProjectSaveResult {
        if (countActiveTasksForProject(projectId) > 0) return ProjectSaveResult.HAS_TASKS

        val values = ContentValues().apply {
            put(PROJECT_IS_DELETED, 1)
        }
        val updatedRows = writableDatabase.update(
            TABLE_PROJECTS,
            values,
            "$PROJECT_ID = ? AND $PROJECT_IS_DELETED = 0",
            arrayOf(projectId.toString())
        )
        return if (updatedRows == 1) ProjectSaveResult.SUCCESS else ProjectSaveResult.NOT_FOUND
    }

    fun listTasksForUser(user: UserSession, includeDeleted: Boolean): List<TaskItem> {
        val deletedFilter = if (includeDeleted) "" else "AND t.$TASK_IS_DELETED = 0"
        val roleFilter = when (user.role) {
            UserRole.ADMIN -> ""
            UserRole.MANAGER -> "AND p.$PROJECT_CREATED_BY = ?"
            UserRole.MEMBER -> "AND t.$TASK_ASSIGNEE_ID = ?"
        }
        val args = if (user.role == UserRole.ADMIN) emptyArray<String>() else arrayOf(user.id.toString())
        val sql = """
            SELECT t.$TASK_ID, t.$TASK_TITLE, t.$TASK_DESCRIPTION, t.$TASK_PROJECT_ID,
                   p.$PROJECT_NAME, t.$TASK_ASSIGNEE_ID, u.$COL_FULL_NAME AS assignee_name,
                   t.$TASK_STATUS, t.$TASK_PRIORITY, t.$TASK_DUE_DATE, t.$TASK_PROGRESS,
                   t.$TASK_NOTES, t.$TASK_IS_DELETED
            FROM $TABLE_TASKS t
            JOIN $TABLE_PROJECTS p ON p.$PROJECT_ID = t.$TASK_PROJECT_ID
            JOIN $TABLE_USERS u ON u.$COL_ID = t.$TASK_ASSIGNEE_ID
            WHERE p.$PROJECT_IS_DELETED = 0 $deletedFilter $roleFilter
            ORDER BY t.$TASK_IS_DELETED ASC, t.$TASK_DUE_DATE ASC, t.$TASK_CREATED_AT DESC
        """.trimIndent()

        return readableDatabase.rawQuery(sql, args).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toTaskItem())
            }
        }
    }

    fun listTasksAssignedToUser(userId: Long): List<TaskItem> {
        val sql = """
            SELECT t.$TASK_ID, t.$TASK_TITLE, t.$TASK_DESCRIPTION, t.$TASK_PROJECT_ID,
                   p.$PROJECT_NAME, t.$TASK_ASSIGNEE_ID, u.$COL_FULL_NAME AS assignee_name,
                   t.$TASK_STATUS, t.$TASK_PRIORITY, t.$TASK_DUE_DATE, t.$TASK_PROGRESS,
                   t.$TASK_NOTES, t.$TASK_IS_DELETED
            FROM $TABLE_TASKS t
            JOIN $TABLE_PROJECTS p ON p.$PROJECT_ID = t.$TASK_PROJECT_ID
            JOIN $TABLE_USERS u ON u.$COL_ID = t.$TASK_ASSIGNEE_ID
            WHERE p.$PROJECT_IS_DELETED = 0 AND t.$TASK_IS_DELETED = 0 AND t.$TASK_ASSIGNEE_ID = ?
            ORDER BY t.$TASK_DUE_DATE ASC, t.$TASK_CREATED_AT DESC
        """.trimIndent()

        return readableDatabase.rawQuery(sql, arrayOf(userId.toString())).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toTaskItem())
            }
        }
    }

    fun canUserAccessProject(user: UserSession, projectId: Long): Boolean {
        val whereClause = when (user.role) {
            UserRole.ADMIN -> "$PROJECT_ID = ? AND $PROJECT_IS_DELETED = 0"
            UserRole.MANAGER -> "$PROJECT_ID = ? AND $PROJECT_CREATED_BY = ? AND $PROJECT_IS_DELETED = 0"
            UserRole.MEMBER -> "$PROJECT_ID = ? AND $PROJECT_IS_DELETED = 0 AND $PROJECT_ID IN (SELECT $MEMBER_PROJECT_ID FROM $TABLE_PROJECT_MEMBERS WHERE $MEMBER_USER_ID = ?)"
        }
        val args = if (user.role == UserRole.ADMIN) {
            arrayOf(projectId.toString())
        } else {
            arrayOf(projectId.toString(), user.id.toString())
        }

        return readableDatabase.query(
            TABLE_PROJECTS,
            arrayOf(PROJECT_ID),
            whereClause,
            args,
            null,
            null,
            null,
            "1"
        ).use { it.moveToFirst() }
    }

    fun canUserAccessTask(user: UserSession, taskId: Long, includeDeleted: Boolean): Boolean {
        val deletedFilter = if (includeDeleted) "" else "AND t.$TASK_IS_DELETED = 0"
        val roleFilter = when (user.role) {
            UserRole.ADMIN -> ""
            UserRole.MANAGER -> "AND p.$PROJECT_CREATED_BY = ?"
            UserRole.MEMBER -> "AND t.$TASK_ASSIGNEE_ID = ?"
        }
        val args = if (user.role == UserRole.ADMIN) {
            arrayOf(taskId.toString())
        } else {
            arrayOf(taskId.toString(), user.id.toString())
        }
        val sql = """
            SELECT t.$TASK_ID
            FROM $TABLE_TASKS t
            JOIN $TABLE_PROJECTS p ON p.$PROJECT_ID = t.$TASK_PROJECT_ID
            WHERE t.$TASK_ID = ? AND p.$PROJECT_IS_DELETED = 0 $deletedFilter $roleFilter
            LIMIT 1
        """.trimIndent()

        return readableDatabase.rawQuery(sql, args).use { it.moveToFirst() }
    }

    fun getDashboardStats(user: UserSession): DashboardStats {
        val projects = listProjectsForUser(user)
        val tasks = listTasksForUser(user, includeDeleted = false)

        return DashboardStats(
            totalProjects = projects.size,
            totalTasks = tasks.size,
            todoTasks = tasks.count { it.status == TaskStatus.TODO },
            doingTasks = tasks.count { it.status == TaskStatus.DOING },
            doneTasks = tasks.count { it.status == TaskStatus.DONE },
            overdueTasks = tasks.count { it.status == TaskStatus.OVERDUE },
            highPriorityTasks = tasks.count { it.priority == TaskPriority.HIGH }
        )
    }

    fun getReportData(user: UserSession): ReportData {
        val projects = listProjectsForUser(user)
        val tasks = listTasksForUser(user, includeDeleted = false)
        val completionPercent = if (tasks.isEmpty()) 0 else tasks.count { it.status == TaskStatus.DONE } * 100 / tasks.size

        return ReportData(
            totalProjects = projects.size,
            totalTasks = tasks.size,
            completionPercent = completionPercent,
            statusMetrics = TaskStatus.values().map { status ->
                ReportMetric(status.value, tasks.count { it.status == status })
            },
            priorityMetrics = TaskPriority.values().map { priority ->
                ReportMetric(priority.value, tasks.count { it.priority == priority })
            },
            projectReports = projects.map { project ->
                val projectTasks = tasks.filter { it.projectId == project.id }
                ProjectReport(
                    projectName = project.name,
                    totalTasks = projectTasks.size,
                    doneTasks = projectTasks.count { it.status == TaskStatus.DONE }
                )
            }.sortedByDescending { it.totalTasks },
            memberPerformance = tasks.groupBy { it.assigneeName }
                .map { (memberName, memberTasks) ->
                    MemberPerformanceReport(
                        memberName = memberName,
                        totalTasks = memberTasks.size,
                        doneTasks = memberTasks.count { it.status == TaskStatus.DONE }
                    )
                }
                .sortedByDescending { it.doneTasks }
        )
    }

    fun createTask(
        title: String,
        description: String,
        projectId: Long,
        assigneeId: Long,
        status: TaskStatus,
        priority: TaskPriority,
        dueDate: String,
        createdBy: Long
    ): TaskSaveResult {
        if (!isProjectMember(projectId, assigneeId)) return TaskSaveResult.INVALID_ASSIGNEE

        val values = ContentValues().apply {
            put(TASK_TITLE, title.trim())
            put(TASK_DESCRIPTION, description.trim())
            put(TASK_PROJECT_ID, projectId)
            put(TASK_ASSIGNEE_ID, assigneeId)
            put(TASK_STATUS, status.value)
            put(TASK_PRIORITY, priority.value)
            put(TASK_DUE_DATE, dueDate.trim())
            put(TASK_PROGRESS, if (status == TaskStatus.DONE) 100 else 0)
            put(TASK_NOTES, "")
            put(TASK_CREATED_BY, createdBy)
            put(TASK_IS_DELETED, 0)
            put(TASK_CREATED_AT, System.currentTimeMillis())
        }
        return if (writableDatabase.insert(TABLE_TASKS, null, values) == -1L) {
            TaskSaveResult.NOT_FOUND
        } else {
            TaskSaveResult.SUCCESS
        }
    }

    fun updateTask(
        taskId: Long,
        title: String,
        description: String,
        projectId: Long,
        assigneeId: Long,
        status: TaskStatus,
        priority: TaskPriority,
        dueDate: String
    ): TaskSaveResult {
        if (!isProjectMember(projectId, assigneeId)) return TaskSaveResult.INVALID_ASSIGNEE

        val updatedRows = writableDatabase.update(
            TABLE_TASKS,
            ContentValues().apply {
                put(TASK_TITLE, title.trim())
                put(TASK_DESCRIPTION, description.trim())
                put(TASK_PROJECT_ID, projectId)
                put(TASK_ASSIGNEE_ID, assigneeId)
                put(TASK_STATUS, status.value)
                put(TASK_PRIORITY, priority.value)
                put(TASK_DUE_DATE, dueDate.trim())
            },
            "$TASK_ID = ?",
            arrayOf(taskId.toString())
        )
        return if (updatedRows == 1) TaskSaveResult.SUCCESS else TaskSaveResult.NOT_FOUND
    }

    fun updateTaskDetails(
        taskId: Long,
        status: TaskStatus,
        progress: Int,
        notes: String,
        actorUserId: Long? = null
    ): TaskSaveResult {
        val safeProgress = progress.coerceIn(0, 100)
        val previous = readableDatabase.query(
            TABLE_TASKS,
            arrayOf(TASK_STATUS, TASK_PROGRESS),
            "$TASK_ID = ? AND $TASK_IS_DELETED = 0",
            arrayOf(taskId.toString()),
            null,
            null,
            null,
            "1"
        ).use {
            if (it.moveToFirst()) {
                TaskStatus.fromValue(it.getString(it.getColumnIndexOrThrow(TASK_STATUS))) to
                    it.getInt(it.getColumnIndexOrThrow(TASK_PROGRESS))
            } else {
                null
            }
        } ?: return TaskSaveResult.NOT_FOUND

        val updatedRows = writableDatabase.update(
            TABLE_TASKS,
            ContentValues().apply {
                put(TASK_STATUS, status.value)
                put(TASK_PROGRESS, safeProgress)
                put(TASK_NOTES, notes.trim())
            },
            "$TASK_ID = ? AND $TASK_IS_DELETED = 0",
            arrayOf(taskId.toString())
        )
        if (updatedRows != 1) return TaskSaveResult.NOT_FOUND

        actorUserId?.let { actorId ->
            val changes = buildList {
                if (previous.first != status) add("Status changed from ${previous.first.value} to ${status.value}")
                if (previous.second != safeProgress) add("Progress changed from ${previous.second}% to $safeProgress%")
            }
            changes.forEach { addTaskHistory(taskId, actorId, it) }
        }

        return TaskSaveResult.SUCCESS
    }

    fun deleteTask(taskId: Long): Boolean = setTaskDeleted(taskId, true)

    fun restoreTask(taskId: Long): Boolean = setTaskDeleted(taskId, false)

    fun listTaskAttachments(taskId: Long): List<TaskAttachment> {
        val cursor = readableDatabase.query(
            TABLE_TASK_ATTACHMENTS,
            ATTACHMENT_COLUMNS,
            "$ATTACHMENT_TASK_ID = ?",
            arrayOf(taskId.toString()),
            null,
            null,
            "$ATTACHMENT_CREATED_AT DESC"
        )

        return cursor.use {
            buildList {
                while (it.moveToNext()) add(it.toTaskAttachment())
            }
        }
    }

    fun addTaskAttachment(taskId: Long, displayName: String, uri: String, mimeType: String, sizeBytes: Long): Boolean {
        val taskExists = readableDatabase.query(
            TABLE_TASKS,
            arrayOf(TASK_ID),
            "$TASK_ID = ? AND $TASK_IS_DELETED = 0",
            arrayOf(taskId.toString()),
            null,
            null,
            null,
            "1"
        ).use { it.moveToFirst() }
        if (!taskExists) return false

        val values = ContentValues().apply {
            put(ATTACHMENT_TASK_ID, taskId)
            put(ATTACHMENT_NAME, displayName.ifBlank { "Attachment" }.trim())
            put(ATTACHMENT_URI, uri)
            put(ATTACHMENT_MIME_TYPE, mimeType)
            put(ATTACHMENT_SIZE_BYTES, sizeBytes.coerceAtLeast(0L))
            put(ATTACHMENT_CREATED_AT, System.currentTimeMillis())
        }
        return writableDatabase.insert(TABLE_TASK_ATTACHMENTS, null, values) != -1L
    }

    fun deleteTaskAttachment(taskId: Long, attachmentId: Long): Boolean = writableDatabase.delete(
        TABLE_TASK_ATTACHMENTS,
        "$ATTACHMENT_ID = ? AND $ATTACHMENT_TASK_ID = ?",
        arrayOf(attachmentId.toString(), taskId.toString())
    ) == 1

    fun listTaskComments(taskId: Long): List<TaskComment> {
        val sql = """
            SELECT c.$COMMENT_ID, c.$COMMENT_TASK_ID, u.$COL_FULL_NAME AS author_name,
                   c.$COMMENT_CONTENT, c.$COMMENT_CREATED_AT
            FROM $TABLE_TASK_COMMENTS c
            JOIN $TABLE_USERS u ON u.$COL_ID = c.$COMMENT_USER_ID
            WHERE c.$COMMENT_TASK_ID = ?
            ORDER BY c.$COMMENT_CREATED_AT DESC
        """.trimIndent()

        return readableDatabase.rawQuery(sql, arrayOf(taskId.toString())).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toTaskComment())
            }
        }
    }

    fun addTaskComment(taskId: Long, userId: Long, content: String): Boolean {
        val cleanContent = content.trim()
        if (cleanContent.isEmpty()) return false

        val taskExists = readableDatabase.query(
            TABLE_TASKS,
            arrayOf(TASK_ID),
            "$TASK_ID = ? AND $TASK_IS_DELETED = 0",
            arrayOf(taskId.toString()),
            null,
            null,
            null,
            "1"
        ).use { it.moveToFirst() }
        if (!taskExists) return false

        val now = System.currentTimeMillis()
        val inserted = writableDatabase.insert(
            TABLE_TASK_COMMENTS,
            null,
            ContentValues().apply {
                put(COMMENT_TASK_ID, taskId)
                put(COMMENT_USER_ID, userId)
                put(COMMENT_CONTENT, cleanContent)
                put(COMMENT_CREATED_AT, now)
            }
        ) != -1L
        if (inserted) addTaskHistory(taskId, userId, "Comment added")
        return inserted
    }

    fun listTaskHistory(taskId: Long): List<TaskHistoryEntry> {
        val sql = """
            SELECT h.$HISTORY_ID, h.$HISTORY_TASK_ID, u.$COL_FULL_NAME AS actor_name,
                   h.$HISTORY_DESCRIPTION, h.$HISTORY_CREATED_AT
            FROM $TABLE_TASK_HISTORY h
            JOIN $TABLE_USERS u ON u.$COL_ID = h.$HISTORY_USER_ID
            WHERE h.$HISTORY_TASK_ID = ?
            ORDER BY h.$HISTORY_CREATED_AT DESC
        """.trimIndent()

        return readableDatabase.rawQuery(sql, arrayOf(taskId.toString())).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toTaskHistoryEntry())
            }
        }
    }

    private fun setTaskDeleted(taskId: Long, isDeleted: Boolean): Boolean {
        val values = ContentValues().apply {
            put(TASK_IS_DELETED, if (isDeleted) 1 else 0)
        }
        return writableDatabase.update(
            TABLE_TASKS,
            values,
            "$TASK_ID = ?",
            arrayOf(taskId.toString())
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

    private fun createTaskTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_TASKS (
                $TASK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $TASK_TITLE TEXT NOT NULL,
                $TASK_DESCRIPTION TEXT NOT NULL DEFAULT '',
                $TASK_PROJECT_ID INTEGER NOT NULL,
                $TASK_ASSIGNEE_ID INTEGER NOT NULL,
                $TASK_STATUS TEXT NOT NULL CHECK($TASK_STATUS IN ('Todo', 'Doing', 'Done', 'Overdue')),
                $TASK_PRIORITY TEXT NOT NULL CHECK($TASK_PRIORITY IN ('Low', 'Medium', 'High')),
                $TASK_DUE_DATE TEXT NOT NULL,
                $TASK_PROGRESS INTEGER NOT NULL DEFAULT 0,
                $TASK_NOTES TEXT NOT NULL DEFAULT '',
                $TASK_CREATED_BY INTEGER NOT NULL,
                $TASK_IS_DELETED INTEGER NOT NULL DEFAULT 0,
                $TASK_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($TASK_PROJECT_ID) REFERENCES $TABLE_PROJECTS($PROJECT_ID),
                FOREIGN KEY($TASK_ASSIGNEE_ID) REFERENCES $TABLE_USERS($COL_ID),
                FOREIGN KEY($TASK_CREATED_BY) REFERENCES $TABLE_USERS($COL_ID)
            )
            """.trimIndent()
        )
    }

    private fun createTaskAttachmentTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_TASK_ATTACHMENTS (
                $ATTACHMENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $ATTACHMENT_TASK_ID INTEGER NOT NULL,
                $ATTACHMENT_NAME TEXT NOT NULL,
                $ATTACHMENT_URI TEXT NOT NULL,
                $ATTACHMENT_MIME_TYPE TEXT NOT NULL DEFAULT '',
                $ATTACHMENT_SIZE_BYTES INTEGER NOT NULL DEFAULT 0,
                $ATTACHMENT_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($ATTACHMENT_TASK_ID) REFERENCES $TABLE_TASKS($TASK_ID)
            )
            """.trimIndent()
        )
    }

    private fun createTaskCollaborationTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_TASK_COMMENTS (
                $COMMENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COMMENT_TASK_ID INTEGER NOT NULL,
                $COMMENT_USER_ID INTEGER NOT NULL,
                $COMMENT_CONTENT TEXT NOT NULL,
                $COMMENT_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($COMMENT_TASK_ID) REFERENCES $TABLE_TASKS($TASK_ID),
                FOREIGN KEY($COMMENT_USER_ID) REFERENCES $TABLE_USERS($COL_ID)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_TASK_HISTORY (
                $HISTORY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $HISTORY_TASK_ID INTEGER NOT NULL,
                $HISTORY_USER_ID INTEGER NOT NULL,
                $HISTORY_DESCRIPTION TEXT NOT NULL,
                $HISTORY_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($HISTORY_TASK_ID) REFERENCES $TABLE_TASKS($TASK_ID),
                FOREIGN KEY($HISTORY_USER_ID) REFERENCES $TABLE_USERS($COL_ID)
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

    private fun seedTask(db: SQLiteDatabase) {
        val hasTask = db.query(
            TABLE_TASKS,
            arrayOf(TASK_ID),
            null,
            null,
            null,
            null,
            null,
            "1"
        ).use { it.moveToFirst() }
        if (hasTask) return

        val projectId = db.query(
            TABLE_PROJECTS,
            arrayOf(PROJECT_ID),
            "$PROJECT_IS_DELETED = 0",
            null,
            null,
            null,
            PROJECT_CREATED_AT,
            "1"
        ).use { if (it.moveToFirst()) it.getLong(it.getColumnIndexOrThrow(PROJECT_ID)) else null } ?: return
        val managerId = findUserId(db, "manager") ?: return
        val memberId = findUserId(db, "member") ?: return
        db.insert(
            TABLE_TASKS,
            null,
            ContentValues().apply {
                put(TASK_TITLE, "Design task dashboard")
                put(TASK_DESCRIPTION, "Create the first version of task management screens.")
                put(TASK_PROJECT_ID, projectId)
                put(TASK_ASSIGNEE_ID, memberId)
                put(TASK_STATUS, TaskStatus.TODO.value)
                put(TASK_PRIORITY, TaskPriority.HIGH.value)
                put(TASK_DUE_DATE, "2026-07-01")
                put(TASK_PROGRESS, 0)
                put(TASK_NOTES, "Initial task for the mobile project.")
                put(TASK_CREATED_BY, managerId)
                put(TASK_IS_DELETED, 0)
                put(TASK_CREATED_AT, System.currentTimeMillis())
            }
        )
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

    private fun getProjectCreatorId(projectId: Long): Long? = readableDatabase.query(
        TABLE_PROJECTS,
        arrayOf(PROJECT_CREATED_BY),
        "$PROJECT_ID = ? AND $PROJECT_IS_DELETED = 0",
        arrayOf(projectId.toString()),
        null,
        null,
        null,
        "1"
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.getLong(cursor.getColumnIndexOrThrow(PROJECT_CREATED_BY)) else null
    }

    private fun countActiveTasksForProject(projectId: Long): Int = readableDatabase.query(
        TABLE_TASKS,
        arrayOf("COUNT(*)"),
        "$TASK_PROJECT_ID = ? AND $TASK_IS_DELETED = 0",
        arrayOf(projectId.toString()),
        null,
        null,
        null
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.getInt(0) else 0
    }

    private fun isProjectMember(projectId: Long, userId: Long): Boolean {
        val sql = """
            SELECT pm.$MEMBER_USER_ID
            FROM $TABLE_PROJECT_MEMBERS pm
            JOIN $TABLE_USERS u ON u.$COL_ID = pm.$MEMBER_USER_ID
            WHERE pm.$MEMBER_PROJECT_ID = ?
              AND pm.$MEMBER_USER_ID = ?
              AND u.$COL_IS_ACTIVE = 1
              AND u.$COL_IS_DELETED = 0
            LIMIT 1
        """.trimIndent()

        return readableDatabase.rawQuery(sql, arrayOf(projectId.toString(), userId.toString())).use { it.moveToFirst() }
    }

    private fun addTaskHistory(taskId: Long, userId: Long, description: String): Boolean = writableDatabase.insert(
        TABLE_TASK_HISTORY,
        null,
        ContentValues().apply {
            put(HISTORY_TASK_ID, taskId)
            put(HISTORY_USER_ID, userId)
            put(HISTORY_DESCRIPTION, description)
            put(HISTORY_CREATED_AT, System.currentTimeMillis())
        }
    ) != -1L

    private fun hasTasksAssignedOutsideProjectMembers(projectId: Long, memberIds: Set<Long>): Boolean {
        if (memberIds.isEmpty()) return countActiveTasksForProject(projectId) > 0

        val placeholders = memberIds.joinToString(",") { "?" }
        val args = arrayOf(projectId.toString()) + memberIds.map { it.toString() }.toTypedArray()
        val sql = """
            SELECT $TASK_ID
            FROM $TABLE_TASKS
            WHERE $TASK_PROJECT_ID = ?
              AND $TASK_IS_DELETED = 0
              AND $TASK_ASSIGNEE_ID NOT IN ($placeholders)
            LIMIT 1
        """.trimIndent()

        return readableDatabase.rawQuery(sql, args).use { it.moveToFirst() }
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

    private fun Cursor.toTaskItem(): TaskItem = TaskItem(
        id = getLong(getColumnIndexOrThrow(TASK_ID)),
        title = getString(getColumnIndexOrThrow(TASK_TITLE)),
        description = getString(getColumnIndexOrThrow(TASK_DESCRIPTION)),
        projectId = getLong(getColumnIndexOrThrow(TASK_PROJECT_ID)),
        projectName = getString(getColumnIndexOrThrow(PROJECT_NAME)),
        assigneeId = getLong(getColumnIndexOrThrow(TASK_ASSIGNEE_ID)),
        assigneeName = getString(getColumnIndexOrThrow("assignee_name")),
        status = TaskStatus.fromValue(getString(getColumnIndexOrThrow(TASK_STATUS))),
        priority = TaskPriority.fromValue(getString(getColumnIndexOrThrow(TASK_PRIORITY))),
        dueDate = getString(getColumnIndexOrThrow(TASK_DUE_DATE)),
        progress = getInt(getColumnIndexOrThrow(TASK_PROGRESS)),
        notes = getString(getColumnIndexOrThrow(TASK_NOTES)),
        isDeleted = getInt(getColumnIndexOrThrow(TASK_IS_DELETED)) == 1
    )

    private fun Cursor.toTaskAttachment(): TaskAttachment = TaskAttachment(
        id = getLong(getColumnIndexOrThrow(ATTACHMENT_ID)),
        taskId = getLong(getColumnIndexOrThrow(ATTACHMENT_TASK_ID)),
        displayName = getString(getColumnIndexOrThrow(ATTACHMENT_NAME)),
        uri = getString(getColumnIndexOrThrow(ATTACHMENT_URI)),
        mimeType = getString(getColumnIndexOrThrow(ATTACHMENT_MIME_TYPE)),
        sizeBytes = getLong(getColumnIndexOrThrow(ATTACHMENT_SIZE_BYTES)),
        createdAt = getLong(getColumnIndexOrThrow(ATTACHMENT_CREATED_AT))
    )

    private fun Cursor.toTaskComment(): TaskComment = TaskComment(
        id = getLong(getColumnIndexOrThrow(COMMENT_ID)),
        taskId = getLong(getColumnIndexOrThrow(COMMENT_TASK_ID)),
        authorName = getString(getColumnIndexOrThrow("author_name")),
        content = getString(getColumnIndexOrThrow(COMMENT_CONTENT)),
        createdAt = getLong(getColumnIndexOrThrow(COMMENT_CREATED_AT))
    )

    private fun Cursor.toTaskHistoryEntry(): TaskHistoryEntry = TaskHistoryEntry(
        id = getLong(getColumnIndexOrThrow(HISTORY_ID)),
        taskId = getLong(getColumnIndexOrThrow(HISTORY_TASK_ID)),
        actorName = getString(getColumnIndexOrThrow("actor_name")),
        description = getString(getColumnIndexOrThrow(HISTORY_DESCRIPTION)),
        createdAt = getLong(getColumnIndexOrThrow(HISTORY_CREATED_AT))
    )

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
        private const val DATABASE_VERSION = 8

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

        private const val TABLE_TASKS = "tasks"
        private const val TASK_ID = "id"
        private const val TASK_TITLE = "title"
        private const val TASK_DESCRIPTION = "description"
        private const val TASK_PROJECT_ID = "project_id"
        private const val TASK_ASSIGNEE_ID = "assignee_id"
        private const val TASK_STATUS = "status"
        private const val TASK_PRIORITY = "priority"
        private const val TASK_DUE_DATE = "due_date"
        private const val TASK_PROGRESS = "progress"
        private const val TASK_NOTES = "notes"
        private const val TASK_CREATED_BY = "created_by"
        private const val TASK_IS_DELETED = "is_deleted"
        private const val TASK_CREATED_AT = "created_at"

        private const val TABLE_TASK_ATTACHMENTS = "task_attachments"
        private const val ATTACHMENT_ID = "id"
        private const val ATTACHMENT_TASK_ID = "task_id"
        private const val ATTACHMENT_NAME = "display_name"
        private const val ATTACHMENT_URI = "uri"
        private const val ATTACHMENT_MIME_TYPE = "mime_type"
        private const val ATTACHMENT_SIZE_BYTES = "size_bytes"
        private const val ATTACHMENT_CREATED_AT = "created_at"

        private const val TABLE_TASK_COMMENTS = "task_comments"
        private const val COMMENT_ID = "id"
        private const val COMMENT_TASK_ID = "task_id"
        private const val COMMENT_USER_ID = "user_id"
        private const val COMMENT_CONTENT = "content"
        private const val COMMENT_CREATED_AT = "created_at"

        private const val TABLE_TASK_HISTORY = "task_history"
        private const val HISTORY_ID = "id"
        private const val HISTORY_TASK_ID = "task_id"
        private const val HISTORY_USER_ID = "user_id"
        private const val HISTORY_DESCRIPTION = "description"
        private const val HISTORY_CREATED_AT = "created_at"

        private val USER_COLUMNS = arrayOf(COL_ID, COL_USERNAME, COL_FULL_NAME, COL_ROLE)
        private val ACCOUNT_COLUMNS = arrayOf(
            COL_ID,
            COL_USERNAME,
            COL_FULL_NAME,
            COL_ROLE,
            COL_IS_ACTIVE
        )
        private val ATTACHMENT_COLUMNS = arrayOf(
            ATTACHMENT_ID,
            ATTACHMENT_TASK_ID,
            ATTACHMENT_NAME,
            ATTACHMENT_URI,
            ATTACHMENT_MIME_TYPE,
            ATTACHMENT_SIZE_BYTES,
            ATTACHMENT_CREATED_AT
        )

        private fun hashPassword(password: String): String {
            val input = "task-manager-auth:$password".toByteArray(Charsets.UTF_8)
            return MessageDigest.getInstance("SHA-256")
                .digest(input)
                .joinToString(separator = "") { "%02x".format(it) }
        }
    }
}
