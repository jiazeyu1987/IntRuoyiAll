-- release-target-preflight: migrationId=20260729_dcc_product_catalog_project_code_columns; allowedEnvironments=test,backup,prod
-- Columns may legitimately not exist before this migration. Validate compatible existing types and unambiguous original-source row identity; original SQL intentionally overwrites the registered project mapping.
SELECT CASE WHEN

  NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='dcc_product_catalog'
    AND ((column_name='project_name' AND (data_type<>'varchar' OR character_maximum_length<255 OR is_nullable<>'YES'))
      OR (column_name='project_code' AND (data_type<>'varchar' OR character_maximum_length<64 OR is_nullable<>'YES'))))
  AND NOT EXISTS (SELECT 1 FROM dcc_product_catalog
    WHERE HEX(data_source)='E7919BE6B3B0E4BAA7E59381' AND deleted=b'0' AND original_row_no IN (2,3,4,5,6,7,9,10,12,13,15,16,17,19,20,21,22,23,26,27,28,34,35,36,37,38,39,40,41,42,43,47,49,50,51,52,53,54,55,57,58,59,60,61,62,64,66,68,69,70,79,80,81,82,83,84,85,86,87,88,89,90,95,97,102,106,108,109,110,113,114,115,116,117,118,119,121,122,124,125,126,127,128,130,131,132,134,135,136,137,139,140,141,149,151,152,153,157,166,168,169,170,171,172,174,175,177,178,179,180,181,182,184,185,186)
    GROUP BY tenant_id,original_row_no HAVING COUNT(*)>1)

THEN 'TARGET_PREFLIGHT_PASS:20260729_dcc_product_catalog_project_code_columns' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260729_dcc_product_catalog_project_code_columns' END;
