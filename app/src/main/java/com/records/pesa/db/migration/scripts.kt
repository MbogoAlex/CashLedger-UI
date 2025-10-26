package com.records.pesa.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Insert default values into the 'app_launch_state' table
        database.execSQL("INSERT INTO app_launch_state (user_id, launched) VALUES (NULL, 0)")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Insert default values into the 'app_launch_state' table
        database.execSQL("INSERT INTO app_launch_state (user_id, launched) VALUES (NULL, 0)")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Insert default values into the 'app_launch_state' table
        database.execSQL("INSERT INTO app_launch_state (user_id, launched) VALUES (NULL, 0)")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Insert default values into the 'app_launch_state' table
        database.execSQL("INSERT INTO app_launch_state (user_id, launched) VALUES (NULL, 0)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Insert default values into the 'app_launch_state' table
        database.execSQL("INSERT INTO app_launch_state (user_id, launched) VALUES (NULL, 0)")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Insert default values into the 'app_launch_state' table
        database.execSQL("INSERT INTO app_launch_state (user_id, launched) VALUES (NULL, 0)")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Step 1: Create a new table with the correct schema
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS transaction_new (
                id INTEGER PRIMARY KEY NOT NULL,
                transactionCode TEXT NOT NULL,
                transactionType TEXT NOT NULL,
                transactionAmount REAL NOT NULL,
                transactionCost REAL NOT NULL,
                date TEXT NOT NULL,
                time TEXT NOT NULL,
                sender TEXT NOT NULL,
                recipient TEXT NOT NULL,
                nickName TEXT,          -- Nullable field
                comment TEXT,           -- Nullable field
                balance REAL NOT NULL,
                entity TEXT NOT NULL,
                FOREIGN KEY (id) REFERENCES userAccount(id) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY (id) REFERENCES transactionCategory(id) ON UPDATE CASCADE ON DELETE CASCADE
            )
        """)

        // Step 2: Copy data from the old table to the new table, escaping table names
        database.execSQL("""
            INSERT INTO transaction_new (id, transactionCode, transactionType, transactionAmount, transactionCost, 
                                         date, time, sender, recipient, nickName, comment, balance, entity)
            SELECT id, transactionCode, transactionType, transactionAmount, transactionCost, 
                   date, time, sender, recipient, nickName, comment, balance, entity
            FROM "transaction"
        """)

        // Step 3: Drop the old table, escaping table name
        database.execSQL("DROP TABLE \"transaction\"")

        // Step 4: Rename the new table to the original table name, escaping table names
        database.execSQL("ALTER TABLE transaction_new RENAME TO \"transaction\"")

        // CategoryKeyword

        // Step 1: Create a new table with the correct schema
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS categoryKeyword_new (
                id INTEGER PRIMARY KEY NOT NULL,
                keyword TEXT NOT NULL,
                nickName TEXT,          -- Ensure nickName is nullable
                FOREIGN KEY (id) REFERENCES transactionCategory(id) ON UPDATE CASCADE ON DELETE CASCADE
            )
        """)

        // Step 2: Copy data from the old table to the new table
        database.execSQL("""
            INSERT INTO categoryKeyword_new (id, keyword, nickName)
            SELECT id, keyword, nickName
            FROM categoryKeyword
        """)

        // Step 3: Drop the old table
        database.execSQL("DROP TABLE categoryKeyword")

        // Step 4: Rename the new table to the original table name
        database.execSQL("ALTER TABLE categoryKeyword_new RENAME TO categoryKeyword")

        // UserAccount
        // Step 1: Create a new table with the correct schema
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS userAccount_new (
                id INTEGER PRIMARY KEY NOT NULL,
                fname TEXT,             -- Ensure fname is nullable
                lname TEXT,             -- Ensure lname is nullable
                email TEXT,             -- Ensure email is nullable
                phoneNumber TEXT NOT NULL,
                password TEXT NOT NULL,
                createdAt TEXT NOT NULL
            )
        """)

        // Step 2: Copy data from the old table to the new table
        database.execSQL("""
            INSERT INTO userAccount_new (id, fname, lname, email, phoneNumber, password, createdAt)
            SELECT id, fname, lname, email, phoneNumber, password, createdAt
            FROM userAccount
        """)

        // Step 3: Drop the old table
        database.execSQL("DROP TABLE userAccount")

        // Step 4: Rename the new table to the original table name
        database.execSQL("ALTER TABLE userAccount_new RENAME TO userAccount")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Rename the old `budget` table to `budget_old`
        database.execSQL("ALTER TABLE budget RENAME TO budget_old")

        // Create the new `budget` table with the correct schema
        database.execSQL("""
            CREATE TABLE budget (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                active INTEGER NOT NULL,
                expenditure REAL NOT NULL,
                budgetLimit REAL NOT NULL,
                createdAt TEXT NOT NULL,
                limitDate TEXT NOT NULL,
                limitReached INTEGER NOT NULL,
                limitReachedAt TEXT,
                exceededBy REAL NOT NULL,
                categoryId INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES transactionCategory(id) ON DELETE CASCADE ON UPDATE CASCADE
            )
        """)

        // Copy data from the old `budget` table to the new table (excluding `categoryId`)
        database.execSQL("""
            INSERT INTO budget (
                id, name, active, expenditure, budgetLimit, createdAt, limitDate, 
                limitReached, limitReachedAt, exceededBy
            )
            SELECT 
                id, name, active, expenditure, budgetLimit, createdAt, limitDate, 
                limitReached, limitReachedAt, exceededBy 
            FROM budget_old
        """)

        // Drop the old `budget` table
        database.execSQL("DROP TABLE budget_old")

        // Rename the old `transaction` table to `transaction_old`
        database.execSQL("ALTER TABLE `transaction` RENAME TO `transaction_old`")

        // Create the new `transaction` table with the updated schema
        database.execSQL("""
            CREATE TABLE `transaction` (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transactionCode TEXT NOT NULL,
                transactionType TEXT NOT NULL,
                transactionAmount REAL NOT NULL,
                transactionCost REAL NOT NULL,
                date TEXT NOT NULL,
                time TEXT NOT NULL,
                sender TEXT NOT NULL,
                recipient TEXT NOT NULL,
                nickName TEXT,
                comment TEXT,
                balance REAL NOT NULL,
                entity TEXT NOT NULL,
                userId INTEGER NOT NULL,
                categoryId INTEGER NOT NULL,
                FOREIGN KEY(userId) REFERENCES userAccount(id) ON DELETE CASCADE ON UPDATE CASCADE,
                FOREIGN KEY(categoryId) REFERENCES transactionCategory(id) ON DELETE CASCADE ON UPDATE CASCADE
            )
        """)

        // Copy data from the old `transaction` table to the new table
        // Note: `userId` and `categoryId` are not available in the old schema, so set default values if needed
        database.execSQL("""
            INSERT INTO `transaction` (
                id, transactionCode, transactionType, transactionAmount, transactionCost, 
                date, time, sender, recipient, nickName, comment, balance, entity, userId, categoryId
            )
            SELECT 
                id, transactionCode, transactionType, transactionAmount, transactionCost, 
                date, time, sender, recipient, nickName, comment, balance, entity, 
                0 AS userId, -- Assuming default value for `userId`
                0 AS categoryId -- Assuming default value for `categoryId`
            FROM `transaction_old`
        """)

        // Drop the old `transaction` table
        database.execSQL("DROP TABLE `transaction_old`")

        // Rename the old `categoryKeyword` table to `categoryKeyword_old`
        database.execSQL("ALTER TABLE `categoryKeyword` RENAME TO `categoryKeyword_old`")

        // Create the new `categoryKeyword` table with the updated schema
        database.execSQL("""
            CREATE TABLE `categoryKeyword` (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                keyword TEXT NOT NULL,
                nickName TEXT,
                categoryId INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES transactionCategory(id) ON DELETE CASCADE ON UPDATE CASCADE
            )
        """)

        // Copy data from the old `categoryKeyword` table to the new table
        // Note: `categoryId` is not available in the old schema, so set a default value if needed
        database.execSQL("""
            INSERT INTO `categoryKeyword` (
                id, keyword, nickName, categoryId
            )
            SELECT 
                id, keyword, nickName, 0 AS categoryId -- Assuming default value for `categoryId`
            FROM `categoryKeyword_old`
        """)

        // Drop the old `categoryKeyword` table
        database.execSQL("DROP TABLE `categoryKeyword_old`")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Rename the old `transaction` table to `transaction_old`
        database.execSQL("ALTER TABLE `transaction` RENAME TO `transaction_old`")

        // Create the new `transaction` table without the `categoryId` column
        database.execSQL("""
            CREATE TABLE `transaction` (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transactionCode TEXT NOT NULL,
                transactionType TEXT NOT NULL,
                transactionAmount REAL NOT NULL,
                transactionCost REAL NOT NULL,
                date TEXT NOT NULL,
                time TEXT NOT NULL,
                sender TEXT NOT NULL,
                recipient TEXT NOT NULL,
                nickName TEXT,
                comment TEXT,
                balance REAL NOT NULL,
                entity TEXT NOT NULL,
                userId INTEGER NOT NULL,
                FOREIGN KEY(userId) REFERENCES userAccount(id) ON DELETE CASCADE ON UPDATE CASCADE
            )
        """)

        // Migrate data from the old `transaction` table to the new `transaction` table
        database.execSQL("""
            INSERT INTO `transaction` (
                id, transactionCode, transactionType, transactionAmount, transactionCost, 
                date, time, sender, recipient, nickName, comment, balance, entity, userId
            )
            SELECT 
                id, transactionCode, transactionType, transactionAmount, transactionCost, 
                date, time, sender, recipient, nickName, comment, balance, entity, userId
            FROM `transaction_old`
        """)

        // Drop the old `transaction` table
        database.execSQL("DROP TABLE `transaction_old`")

        // Handle existing category associations if needed
        // Since you already have the join table, you should handle data migration to it separately if necessary
        // For example, you might need to populate the `transaction_category_cross_ref` table based on old data

        // Note: If there are any specific migrations needed for `categoryKeyword`, handle them here
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Step 1: Update the 'budget' table schema (as done earlier)
        database.execSQL("ALTER TABLE `budget` RENAME TO `budget_old`")
        database.execSQL("""
            CREATE TABLE `budget` (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                active INTEGER NOT NULL,
                expenditure REAL NOT NULL,
                budgetLimit REAL NOT NULL,
                createdAt TEXT NOT NULL,
                limitDate TEXT NOT NULL,
                limitReached INTEGER NOT NULL,
                limitReachedAt TEXT,
                exceededBy REAL NOT NULL,
                categoryId INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES transactionCategory(id) ON DELETE CASCADE ON UPDATE CASCADE
            )
        """)
        database.execSQL("""
            INSERT INTO `budget` (
                id, name, active, expenditure, budgetLimit, createdAt, limitDate, 
                limitReached, limitReachedAt, exceededBy, categoryId
            )
            SELECT 
                id, name, active, expenditure, budgetLimit, createdAt, limitDate, 
                limitReached, limitReachedAt, exceededBy, categoryId
            FROM `budget_old`
        """)
        database.execSQL("DROP TABLE `budget_old`")
        database.execSQL("CREATE INDEX `index_budget_categoryId` ON `budget` (categoryId)")

        // Step 2: Update the 'transactionCategory' table schema
        database.execSQL("ALTER TABLE `transactionCategory` RENAME TO `transactionCategory_old`")
        database.execSQL("""
            CREATE TABLE `transactionCategory` (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL
            )
        """)
        database.execSQL("""
            INSERT INTO `transactionCategory` (
                id, name, createdAt, updatedAt
            )
            SELECT 
                id, name, createdAt, updatedAt
            FROM `transactionCategory_old`
        """)
        database.execSQL("DROP TABLE `transactionCategory_old`")
        database.execSQL("CREATE INDEX `index_transactionCategory_createdAt` ON `transactionCategory` (createdAt)")
        database.execSQL("CREATE INDEX `index_transactionCategory_updatedAt` ON `transactionCategory` (updatedAt)")

        // Step 3: Update the 'transaction' table schema
        database.execSQL("ALTER TABLE 'transaction' RENAME TO 'transaction_old'")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS 'transaction' (
                id INTEGER PRIMARY KEY NOT NULL, 
                transactionCode TEXT NOT NULL, 
                transactionType TEXT NOT NULL, 
                transactionAmount REAL NOT NULL, 
                transactionCost REAL NOT NULL, 
                date TEXT NOT NULL, 
                time TEXT NOT NULL, 
                sender TEXT NOT NULL, 
                recipient TEXT NOT NULL, 
                nickName TEXT, 
                comment TEXT, 
                balance REAL NOT NULL, 
                entity TEXT NOT NULL, 
                userId INTEGER NOT NULL, 
                FOREIGN KEY(userId) REFERENCES userAccount(id) ON DELETE CASCADE ON UPDATE CASCADE
            )
        """)
        database.execSQL("""
            INSERT INTO 'transaction' (
                id, transactionCode, transactionType, transactionAmount, transactionCost, 
                date, time, sender, recipient, nickName, comment, balance, entity, userId
            )
            SELECT 
                id, transactionCode, transactionType, transactionAmount, transactionCost, 
                date, time, sender, recipient, nickName, comment, balance, entity, userId
            FROM 'transaction_old'
        """)
        database.execSQL("DROP TABLE 'transaction_old'")
        database.execSQL("CREATE INDEX IF NOT EXISTS 'index_transaction_userId' ON 'transaction'('userId')")
        database.execSQL("CREATE INDEX IF NOT EXISTS 'index_transaction_entity' ON 'transaction'('entity')")
        database.execSQL("CREATE INDEX IF NOT EXISTS 'index_transaction_recipient' ON 'transaction'('recipient')")
        database.execSQL("CREATE INDEX IF NOT EXISTS 'index_transaction_sender' ON 'transaction'('sender')")
        database.execSQL("CREATE INDEX IF NOT EXISTS 'index_transaction_time' ON 'transaction'('time')")
        database.execSQL("CREATE INDEX IF NOT EXISTS 'index_transaction_date' ON 'transaction'('date')")
        database.execSQL("CREATE INDEX IF NOT EXISTS 'index_transaction_transactionType' ON 'transaction'('transactionType')")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS 'index_transaction_transactionCode' ON 'transaction'('transactionCode')")

        // Step 4: Update the 'categoryKeyword' table schema
        database.execSQL("ALTER TABLE `categoryKeyword` RENAME TO `categoryKeyword_old`")
        database.execSQL("""
            CREATE TABLE `categoryKeyword` (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                keyword TEXT NOT NULL,
                nickName TEXT,
                categoryId INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES transactionCategory(id) ON DELETE CASCADE ON UPDATE CASCADE
            )
        """)
        database.execSQL("""
            INSERT INTO `categoryKeyword` (
                id, keyword, nickName, categoryId
            )
            SELECT 
                id, keyword, nickName, categoryId
            FROM `categoryKeyword_old`
        """)
        database.execSQL("DROP TABLE `categoryKeyword_old`")
        database.execSQL("CREATE INDEX `index_categoryKeyword_categoryId` ON `categoryKeyword` (categoryId)")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Drop the existing table if it exists (to handle schema changes)
        database.execSQL("DROP TABLE IF EXISTS `transactionCategoryCrossRef`")

        // Recreate the 'transactionCategoryCrossRef' table with the correct schema
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `transactionCategoryCrossRef` (
                `transactionId` INTEGER NOT NULL,
                `categoryId` INTEGER NOT NULL,
                PRIMARY KEY(`transactionId`, `categoryId`),
                FOREIGN KEY(`transactionId`) REFERENCES `transaction`(`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
                FOREIGN KEY(`categoryId`) REFERENCES `transactionCategory`(`id`) ON DELETE CASCADE ON UPDATE NO ACTION
            )
        """)

        // Create indices for the 'transactionCategoryCrossRef' table
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactionCategoryCrossRef_categoryId` ON `transactionCategoryCrossRef` (`categoryId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactionCategoryCrossRef_transactionId` ON `transactionCategoryCrossRef` (`transactionId`)")
    }
}


val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add the new column 'updatedTimes' to the 'transactionCategory' table
        database.execSQL("ALTER TABLE transactionCategory ADD COLUMN updatedTimes REAL")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Insert default values into the 'app_launch_state' table
        database.execSQL("INSERT INTO app_launch_state (user_id, launched) VALUES (NULL, 0)")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Insert default values into the 'app_launch_state' table
        database.execSQL("INSERT INTO app_launch_state (user_id, launched) VALUES (NULL, 0)")
    }
}

val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create the new deletedTransactions table
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `deletedTransactions` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`entity` TEXT NOT NULL)"
        )
    }
}

val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add the new column `backupWorkerInitiated` to the `user` table
        database.execSQL(
            "ALTER TABLE `user` ADD COLUMN `backupWorkerInitiated` INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create the new UserSession table for authentication token management
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `userSession` (
                `id` INTEGER PRIMARY KEY NOT NULL,
                `userId` INTEGER,
                `accessToken` TEXT,
                `refreshToken` TEXT,
                `tokenType` TEXT,
                `accessTokenExpiresIn` INTEGER,
                `refreshTokenExpiresIn` INTEGER
            )
        """)

        // Insert a default row with id = 1 for the session
        // This table is designed to hold a single row for the current user session
        database.execSQL("""
            INSERT OR IGNORE INTO `userSession` (`id`) VALUES (1)
        """)
    }
}













