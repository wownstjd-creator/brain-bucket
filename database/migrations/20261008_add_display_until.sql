-- Existing database update. Safe to repeat: skips an existing column.
-- Run in MySQL, not PowerShell. Does not modify existing post data.
USE brain_bucket;

SET @display_until_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'posts'
      AND COLUMN_NAME = 'display_until'
);
SET @display_until_sql = IF(
    @display_until_exists = 0,
    'ALTER TABLE posts ADD COLUMN display_until DATE NULL',
    'SELECT ''display_until already exists; skipped'' AS migration_result'
);
PREPARE display_until_stmt FROM @display_until_sql;
EXECUTE display_until_stmt;
DEALLOCATE PREPARE display_until_stmt;
SHOW COLUMNS FROM posts LIKE 'display_until';
