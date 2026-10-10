-- Existing database: select brain_bucket before running. Back up first. No rows deleted.
DROP PROCEDURE IF EXISTS bb_add_column;
DELIMITER $$
CREATE PROCEDURE bb_add_column(IN tabname VARCHAR(64), IN colname VARCHAR(64), IN definition TEXT)
BEGIN
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tabname AND column_name=colname) THEN
 SET @ddl=CONCAT('ALTER TABLE `',tabname,'` ADD COLUMN `',colname,'` ',definition);PREPARE stmt FROM @ddl;EXECUTE stmt;DEALLOCATE PREPARE stmt;
 END IF;
END$$
DELIMITER ;
CALL bb_add_column('posts','display_from','DATE NULL');
CALL bb_add_column('posts','version','BIGINT NOT NULL DEFAULT 0');
CALL bb_add_column('approval_logs','snapshot','LONGTEXT NULL');
CALL bb_add_column('approval_logs','poster_key','VARCHAR(512) NULL');
CALL bb_add_column('approval_logs','evidence_key','VARCHAR(512) NULL');
CALL bb_add_column('approval_logs','original_key','VARCHAR(512) NULL');
CALL bb_add_column('approval_logs','poster_type','VARCHAR(255) NULL');
CALL bb_add_column('approval_logs','evidence_type','VARCHAR(255) NULL');
CALL bb_add_column('approval_logs','original_type','VARCHAR(255) NULL');
DROP PROCEDURE bb_add_column;
