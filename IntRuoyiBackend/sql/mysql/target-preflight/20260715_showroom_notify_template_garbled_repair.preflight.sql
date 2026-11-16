-- release-target-preflight: migrationId=20260715_showroom_notify_template_garbled_repair; allowedEnvironments=test,backup,prod
-- Match the original migration's repairable subset exactly: unknown names remain unchanged.
-- Never invent a replacement name or claim all historical messages were repaired.
WITH messages AS (
  SELECT tenant_id,template_code,JSON_VALID(template_params) AS valid_json,
    IF(JSON_VALID(template_params),template_params,JSON_OBJECT()) AS params,
    (COALESCE(LOCATE('??',template_nickname),0)>0 OR COALESCE(LOCATE('??',template_content),0)>0) AS affected
  FROM system_notify_message WHERE deleted=b'0'
  AND template_code IN ('SHOWROOM_APPROVAL_PENDING','SHOWROOM_APPROVAL_PUBLISHED','SHOWROOM_APPROVAL_REJECTED')
), resolved AS (
  SELECT m.*,
    CASE WHEN LOCATE('??',JSON_UNQUOTE(JSON_EXTRACT(params,'$.targetName')))=0
      THEN JSON_UNQUOTE(JSON_EXTRACT(params,'$.targetName'))
      WHEN JSON_UNQUOTE(JSON_EXTRACT(params,'$.targetType'))='PRODUCT' THEN pr.name_cn
      WHEN JSON_UNQUOTE(JSON_EXTRACT(params,'$.targetType'))='COMPANY' THEN c.display_name ELSE NULL END AS resolved_name,
    COALESCE(LOCATE('??',JSON_UNQUOTE(JSON_EXTRACT(params,'$.targetName'))),1)<>0 AS uses_business_name,
    CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(params,'$.targetType'))='PRODUCT' AND pr.tenant_id=cr.tenant_id THEN cr.tenant_id
      WHEN JSON_UNQUOTE(JSON_EXTRACT(params,'$.targetType'))='COMPANY' AND c.tenant_id=cr.tenant_id THEN cr.tenant_id ELSE NULL END AS business_tenant
  FROM messages m LEFT JOIN showroom_change_request cr
    ON cr.id=CAST(JSON_UNQUOTE(JSON_EXTRACT(params,'$.changeRequestId')) AS UNSIGNED) AND cr.deleted=b'0'
  LEFT JOIN showroom_product_revision pr ON pr.id=cr.target_revision_id AND pr.deleted=b'0'
  LEFT JOIN showroom_company c ON c.id=cr.target_id AND c.deleted=b'0'
)
SELECT CASE WHEN
(SELECT COUNT(*) FROM system_notify_template WHERE deleted=b'0'
   AND code IN ('SHOWROOM_APPROVAL_PENDING','SHOWROOM_APPROVAL_PUBLISHED','SHOWROOM_APPROVAL_REJECTED'))=3
  AND (SELECT COUNT(DISTINCT code) FROM system_notify_template WHERE deleted=b'0'
   AND code IN ('SHOWROOM_APPROVAL_PENDING','SHOWROOM_APPROVAL_PUBLISHED','SHOWROOM_APPROVAL_REJECTED'))=3
  AND NOT EXISTS (SELECT 1 FROM messages WHERE NOT valid_json)
  AND NOT EXISTS (
    SELECT 1 FROM resolved WHERE affected
      AND resolved_name IS NOT NULL AND LOCATE('??',resolved_name)=0
      AND JSON_CONTAINS_PATH(params,'all','$.targetTypeText','$.targetName','$.targetType','$.changeRequestId')
      AND LOCATE('??',JSON_UNQUOTE(JSON_EXTRACT(params,'$.targetTypeText')))=0
      AND (template_code='SHOWROOM_APPROVAL_PUBLISHED' OR (
        JSON_EXTRACT(params,'$.approvalStage') IS NOT NULL
        AND LOCATE('??',JSON_UNQUOTE(JSON_EXTRACT(params,'$.approvalStage')))=0))
      AND (template_code<>'SHOWROOM_APPROVAL_REJECTED' OR (
        JSON_EXTRACT(params,'$.rejectionReason') IS NOT NULL
        AND LOCATE('??',JSON_UNQUOTE(JSON_EXTRACT(params,'$.rejectionReason')))=0))
      AND (
      COALESCE(JSON_TYPE(JSON_EXTRACT(params,'$.targetTypeText')),'NULL')<>'STRING'
      OR COALESCE(JSON_TYPE(JSON_EXTRACT(params,'$.targetName')),'NULL')<>'STRING'
      OR COALESCE(JSON_TYPE(JSON_EXTRACT(params,'$.targetType')),'NULL')<>'STRING'
      OR COALESCE(JSON_TYPE(JSON_EXTRACT(params,'$.changeRequestId')),'NULL') NOT IN ('INTEGER','STRING')
      OR NOT REGEXP_LIKE(JSON_UNQUOTE(JSON_EXTRACT(params,'$.changeRequestId')),'^[1-9][0-9]{0,18}$','c')
      OR LOCATE('??',JSON_UNQUOTE(JSON_EXTRACT(params,'$.targetTypeText')))>0
      OR resolved_name=''
      OR (uses_business_name AND (business_tenant IS NULL OR business_tenant<>tenant_id))
      OR (template_code IN ('SHOWROOM_APPROVAL_PENDING','SHOWROOM_APPROVAL_REJECTED') AND (
        COALESCE(JSON_TYPE(JSON_EXTRACT(params,'$.approvalStage')),'NULL')<>'STRING'
        OR LOCATE('??',JSON_UNQUOTE(JSON_EXTRACT(params,'$.approvalStage')))>0))
      OR (template_code='SHOWROOM_APPROVAL_REJECTED' AND (
        COALESCE(JSON_TYPE(JSON_EXTRACT(params,'$.rejectionReason')),'NULL')<>'STRING'
        OR LOCATE('??',JSON_UNQUOTE(JSON_EXTRACT(params,'$.rejectionReason')))>0))
    )
  )
THEN 'TARGET_PREFLIGHT_PASS:20260715_showroom_notify_template_garbled_repair'
ELSE 'TARGET_PREFLIGHT_BLOCKED:review-required-before-data-migration' END;
