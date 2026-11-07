DELETE FROM "system_electronic_signature";
DELETE FROM "mes_pqc_inspection_piece_detail";
ALTER TABLE "mes_pro_process_pool_event" ALTER COLUMN "tenant_id" SET DEFAULT 0;
