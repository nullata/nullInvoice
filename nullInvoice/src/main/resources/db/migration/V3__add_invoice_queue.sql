-- Queue table for async invoice generation
CREATE TABLE IF NOT EXISTS `invoice_requests` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  -- PENDING -> COMPLETED | FAILED
  `supplier_id` bigint unsigned NOT NULL,
  `request_payload` longtext NOT NULL,
  `invoice_id` bigint unsigned DEFAULT NULL,
  `error_message` varchar(1024) DEFAULT NULL,
  `attempts` int unsigned NOT NULL DEFAULT '0',
  `max_attempts` int unsigned NOT NULL DEFAULT '3',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `completed_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_invoice_requests_status` (`status`),
  KEY `idx_invoice_requests_supplier` (`supplier_id`),
  CONSTRAINT `fk_invoice_requests_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`),
  CONSTRAINT `fk_invoice_requests_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `parties` (`id`)
);

-- Idempotency link from invoices back to the queue row that created it.
-- Nullable, so existing rows and sync-path invoices are unaffected.
ALTER TABLE `invoices`
  ADD COLUMN `request_id` bigint unsigned DEFAULT NULL,
  ADD CONSTRAINT `ux_invoices_request_id` UNIQUE (`request_id`),
  ADD CONSTRAINT `fk_invoices_request` FOREIGN KEY (`request_id`) REFERENCES `invoice_requests` (`id`);
