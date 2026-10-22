-- release-target-preflight: migrationId=20261022_dcc_product_catalog_project_columns; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='dcc_product_catalog')
  AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='dcc_product_catalog'
    AND ((column_name='project_name' AND (column_type<>'varchar(255)' OR is_nullable<>'YES'))
      OR (column_name='project_code' AND (column_type<>'varchar(64)' OR is_nullable<>'YES'))))
THEN 'TARGET_PREFLIGHT_PASS:20261022_dcc_product_catalog_project_columns' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261022_dcc_product_catalog_project_columns' END;
