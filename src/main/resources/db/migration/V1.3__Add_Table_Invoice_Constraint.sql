-- Migration V1.3: Add constraint to enforce "1 table belongs to at most 1 active invoice"
-- Rule: At any time, a table can only belong to at most 1 invoice with status IN_PROGRESS or RESERVED
-- An invoice can map to multiple tables (merged tables)

-- Step 1: Check for existing violations before adding constraint
DECLARE @violationCount INT;

SELECT @violationCount = COUNT(DISTINCT idt1.dining_table_id)
FROM InvoiceDiningTable idt1
INNER JOIN Invoice i1 ON idt1.invoice_id = i1.invoice_id
WHERE i1.invoice_status IN ('IN_PROGRESS', 'RESERVED')
AND EXISTS (
    SELECT 1
    FROM InvoiceDiningTable idt2
    INNER JOIN Invoice i2 ON idt2.invoice_id = i2.invoice_id
    WHERE idt2.dining_table_id = idt1.dining_table_id
    AND i2.invoice_status IN ('IN_PROGRESS', 'RESERVED')
    AND i2.invoice_id != i1.invoice_id
);

IF @violationCount > 0
BEGIN
    PRINT 'WARNING: Found ' + CAST(@violationCount AS VARCHAR) + ' table(s) attached to multiple active invoices';
    PRINT 'Listing conflicting tables:';
    
    SELECT DISTINCT 
        dt.dining_table_id as table_id,
        dt.table_name,
        STRING_AGG(CAST(i.invoice_id AS VARCHAR) + ':' + i.invoice_code + '(' + i.invoice_status + ')', ', ') as conflicting_invoices
    FROM InvoiceDiningTable idt
    INNER JOIN DiningTable dt ON idt.dining_table_id = dt.dining_table_id
    INNER JOIN Invoice i ON idt.invoice_id = i.invoice_id
    WHERE i.invoice_status IN ('IN_PROGRESS', 'RESERVED')
    AND idt.dining_table_id IN (
        SELECT idt1.dining_table_id
        FROM InvoiceDiningTable idt1
        INNER JOIN Invoice i1 ON idt1.invoice_id = i1.invoice_id
        WHERE i1.invoice_status IN ('IN_PROGRESS', 'RESERVED')
        GROUP BY idt1.dining_table_id
        HAVING COUNT(DISTINCT i1.invoice_id) > 1
    )
    GROUP BY dt.dining_table_id, dt.table_name;
    
    RAISERROR('Data integrity violation: Please resolve conflicts before applying this migration.', 16, 1);
    RETURN;
END;
GO

-- Step 2: Create trigger to enforce constraint on INSERT
IF OBJECT_ID('TR_InvoiceDiningTable_PreventDuplicateActiveTable', 'TR') IS NOT NULL
    DROP TRIGGER TR_InvoiceDiningTable_PreventDuplicateActiveTable;
GO

CREATE TRIGGER TR_InvoiceDiningTable_PreventDuplicateActiveTable
ON InvoiceDiningTable
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    
    -- Check if any inserted table is already attached to another active invoice
    IF EXISTS (
        SELECT 1
        FROM inserted ins
        INNER JOIN Invoice i_new ON ins.invoice_id = i_new.invoice_id
        WHERE i_new.invoice_status IN ('IN_PROGRESS', 'RESERVED')
        AND EXISTS (
            SELECT 1
            FROM InvoiceDiningTable idt_existing
            INNER JOIN Invoice i_existing ON idt_existing.invoice_id = i_existing.invoice_id
            WHERE idt_existing.dining_table_id = ins.dining_table_id
            AND i_existing.invoice_status IN ('IN_PROGRESS', 'RESERVED')
            AND i_existing.invoice_id != i_new.invoice_id
        )
    )
    BEGIN
        ROLLBACK TRANSACTION;
        RAISERROR('Cannot assign table: Table is already attached to another active invoice (IN_PROGRESS or RESERVED)', 16, 1);
        RETURN;
    END;
END;
GO

-- Step 3: Create trigger to enforce constraint on UPDATE (invoice status change)
IF OBJECT_ID('TR_Invoice_PreventDuplicateActiveTable', 'TR') IS NOT NULL
    DROP TRIGGER TR_Invoice_PreventDuplicateActiveTable;
GO

CREATE TRIGGER TR_Invoice_PreventDuplicateActiveTable
ON Invoice
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    -- Only check if invoice_status changed to IN_PROGRESS or RESERVED
    IF UPDATE(invoice_status)
    BEGIN
        -- Check if any table in this invoice is already attached to another active invoice
        IF EXISTS (
            SELECT 1
            FROM inserted ins
            INNER JOIN InvoiceDiningTable idt_new ON ins.invoice_id = idt_new.invoice_id
            WHERE ins.invoice_status IN ('IN_PROGRESS', 'RESERVED')
            AND EXISTS (
                SELECT 1
                FROM InvoiceDiningTable idt_existing
                INNER JOIN Invoice i_existing ON idt_existing.invoice_id = i_existing.invoice_id
                WHERE idt_existing.dining_table_id = idt_new.dining_table_id
                AND i_existing.invoice_status IN ('IN_PROGRESS', 'RESERVED')
                AND i_existing.invoice_id != ins.invoice_id
            )
        )
        BEGIN
            ROLLBACK TRANSACTION;
            RAISERROR('Cannot change invoice status: One or more tables are already attached to another active invoice', 16, 1);
            RETURN;
        END;
    END;
END;
GO

-- Step 4: Add indexes for better query performance
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_Invoice_Status' AND object_id = OBJECT_ID('Invoice')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_Invoice_Status
    ON Invoice (invoice_status)
    INCLUDE (invoice_id, reserved_at, checked_in_at);
    PRINT 'Created index IX_Invoice_Status';
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_Invoice_Status_ReservedAt' AND object_id = OBJECT_ID('Invoice')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_Invoice_Status_ReservedAt
    ON Invoice (invoice_status, reserved_at)
    WHERE reserved_at IS NOT NULL;
    PRINT 'Created index IX_Invoice_Status_ReservedAt';
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_InvoiceDiningTable_TableId_InvoiceId' AND object_id = OBJECT_ID('InvoiceDiningTable')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_InvoiceDiningTable_TableId_InvoiceId
    ON InvoiceDiningTable (dining_table_id, invoice_id);
    PRINT 'Created index IX_InvoiceDiningTable_TableId_InvoiceId';
END;
GO

PRINT 'Migration V1.3 completed successfully';
PRINT 'Triggers created to enforce: 1 table can only belong to at most 1 active invoice (IN_PROGRESS or RESERVED)';
PRINT 'An invoice can still map to multiple tables (merged tables support)';

