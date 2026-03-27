-- Tạo dining_table_id trong InvoiceItem
ALTER TABLE InvoiceItem
    ADD dining_table_id INT;

ALTER TABLE InvoiceItem
    ADD CONSTRAINT FK_invoiceitem_diningtable
        FOREIGN KEY (dining_table_id)
            REFERENCES DiningTable(dining_table_id);

CREATE INDEX idx_invoiceitem_dining_table
    ON InvoiceItem(dining_table_id);