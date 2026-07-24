START TRANSACTION;

SET @product_id := (
  SELECT id
  FROM showroom_product
  WHERE product_code = 'product_001'
    AND deleted = b'0'
  LIMIT 1
);

UPDATE showroom_product_revision AS bad
JOIN showroom_product_revision AS good
  ON good.product_id = @product_id
 AND good.revision_no = 2
 AND good.deleted = b'0'
SET
  bad.registration_certificate = good.registration_certificate,
  bad.indication_content = good.indication_content
WHERE bad.product_id = @product_id
  AND bad.revision_no IN (3, 4, 5, 6)
  AND bad.deleted = b'0'
  AND (
    bad.registration_certificate LIKE '%?%'
    OR bad.indication_content LIKE '%?%'
  );

UPDATE showroom_product_revision AS bad
JOIN showroom_product_revision AS good
  ON good.product_id = @product_id
 AND good.revision_no = 2
 AND good.deleted = b'0'
SET
  bad.name_cn = good.name_cn
WHERE bad.product_id = @product_id
  AND bad.revision_no = 3
  AND bad.deleted = b'0'
  AND bad.name_cn LIKE '%?%';

COMMIT;
