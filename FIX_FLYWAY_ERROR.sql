-- =============================================
-- Script: Fix Flyway migration error
-- Description: Remove failed migration record so it can retry
-- =============================================

USE DATN_SD29_ByHat;
GO

-- Check current Flyway migration status
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;
GO

-- Delete the failed V1.1 migration record (if it exists and failed)
DELETE FROM flyway_schema_history 
WHERE version = '1.1' AND success = 0;
GO

-- Verify deletion
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;
GO

PRINT 'Failed migration record deleted. You can now restart the BE.';
GO
