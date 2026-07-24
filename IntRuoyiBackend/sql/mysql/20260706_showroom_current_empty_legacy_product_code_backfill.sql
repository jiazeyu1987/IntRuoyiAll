-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260706_showroom_legacy_product_code_english_name_backfill; type=data; riskLevel=medium
-- 20260706 showroom current empty legacy product code backfill
-- Generated from current-db empty legacy recognition with strict name/proximity gates.
-- Safe guards: target legacy_product_code must still be NULL and product_* must not be occupied.
START TRANSACTION;

-- tenant_id=1 product_149 -> INT-150
-- resolution=CONFLICT_STRONG_UNIQUE_NEAREST; rule=CN_EN_UNIQUE; distance=1
UPDATE showroom_product
SET legacy_product_code = 'product_149'
WHERE tenant_id = 1
AND product_code = 'INT-150'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 1
        AND legacy_product_code = 'product_149'
        AND deleted = 0
    ) occupied
);

-- tenant_id=1 product_066 -> INT-67
-- resolution=CONFLICT_STRONG_UNIQUE_NEAREST; rule=CN_EN_UNIQUE; distance=1
UPDATE showroom_product
SET legacy_product_code = 'product_066'
WHERE tenant_id = 1
AND product_code = 'INT-67'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 1
        AND legacy_product_code = 'product_066'
        AND deleted = 0
    ) occupied
);

-- tenant_id=1 product_081 -> INT-82
-- resolution=STRICT_CN_EN; rule=CN_EN_UNIQUE; distance=1
UPDATE showroom_product
SET legacy_product_code = 'product_081'
WHERE tenant_id = 1
AND product_code = 'INT-82'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 1
        AND legacy_product_code = 'product_081'
        AND deleted = 0
    ) occupied
);

-- tenant_id=1 product_086 -> INT-87
-- resolution=CONFLICT_STRONG_UNIQUE_NEAREST; rule=EN_UNIQUE; distance=1
UPDATE showroom_product
SET legacy_product_code = 'product_086'
WHERE tenant_id = 1
AND product_code = 'INT-87'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 1
        AND legacy_product_code = 'product_086'
        AND deleted = 0
    ) occupied
);

-- tenant_id=1 product_095 -> INT-96
-- resolution=STRICT_EN; rule=EN_UNIQUE; distance=1
UPDATE showroom_product
SET legacy_product_code = 'product_095'
WHERE tenant_id = 1
AND product_code = 'INT-96'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 1
        AND legacy_product_code = 'product_095'
        AND deleted = 0
    ) occupied
);

-- tenant_id=122 product_015 -> INT-15
-- resolution=STRICT_EN; rule=EN_UNIQUE; distance=0
UPDATE showroom_product
SET legacy_product_code = 'product_015'
WHERE tenant_id = 122
AND product_code = 'INT-15'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 122
        AND legacy_product_code = 'product_015'
        AND deleted = 0
    ) occupied
);

-- tenant_id=122 product_149 -> INT-150
-- resolution=CONFLICT_STRONG_UNIQUE_NEAREST; rule=CN_EN_UNIQUE; distance=1
UPDATE showroom_product
SET legacy_product_code = 'product_149'
WHERE tenant_id = 122
AND product_code = 'INT-150'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 122
        AND legacy_product_code = 'product_149'
        AND deleted = 0
    ) occupied
);

-- tenant_id=122 product_066 -> INT-67
-- resolution=CONFLICT_STRONG_UNIQUE_NEAREST; rule=CN_EN_UNIQUE; distance=1
UPDATE showroom_product
SET legacy_product_code = 'product_066'
WHERE tenant_id = 122
AND product_code = 'INT-67'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 122
        AND legacy_product_code = 'product_066'
        AND deleted = 0
    ) occupied
);

-- tenant_id=122 product_081 -> INT-82
-- resolution=STRICT_CN_EN; rule=CN_EN_UNIQUE; distance=1
UPDATE showroom_product
SET legacy_product_code = 'product_081'
WHERE tenant_id = 122
AND product_code = 'INT-82'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 122
        AND legacy_product_code = 'product_081'
        AND deleted = 0
    ) occupied
);

-- tenant_id=122 product_086 -> INT-87
-- resolution=CONFLICT_STRONG_UNIQUE_NEAREST; rule=EN_UNIQUE; distance=1
UPDATE showroom_product
SET legacy_product_code = 'product_086'
WHERE tenant_id = 122
AND product_code = 'INT-87'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 122
        AND legacy_product_code = 'product_086'
        AND deleted = 0
    ) occupied
);

-- tenant_id=122 product_095 -> INT-96
-- resolution=STRICT_EN; rule=EN_UNIQUE; distance=1
UPDATE showroom_product
SET legacy_product_code = 'product_095'
WHERE tenant_id = 122
AND product_code = 'INT-96'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 122
        AND legacy_product_code = 'product_095'
        AND deleted = 0
    ) occupied
);
COMMIT;
