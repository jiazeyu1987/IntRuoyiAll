-- release-target-preflight: migrationId=20260726_mes_edhr_release_dossier_requirements; allowedEnvironments=test,backup,prod
-- Do not replace existing configuration: validate JSON before extracting every mandatory boolean, including null/missing fields.
SELECT CASE WHEN

  (SELECT COUNT(*) FROM infra_config WHERE config_key='mes.edhr.release.dossier.requirements' AND deleted=b'0')<=1
  AND NOT EXISTS (SELECT 1 FROM infra_config WHERE config_key='mes.edhr.release.dossier.requirements' AND deleted=b'0'
    AND CASE WHEN JSON_VALID(value) THEN (JSON_TYPE(CAST(value AS JSON))<>'OBJECT' OR COALESCE(JSON_TYPE(JSON_EXTRACT(value,'$.incomingInspectionReportRequired')),'MISSING')<>'BOOLEAN' OR COALESCE(JSON_TYPE(JSON_EXTRACT(value,'$.sterilizationReportRequired')),'MISSING')<>'BOOLEAN' OR COALESCE(JSON_TYPE(JSON_EXTRACT(value,'$.finishedProductInspectionReportRequired')),'MISSING')<>'BOOLEAN' OR COALESCE(JSON_TYPE(JSON_EXTRACT(value,'$.finishedProductInspectionRecordRequired')),'MISSING')<>'BOOLEAN') ELSE 1 END)
THEN 'TARGET_PREFLIGHT_PASS:20260726_mes_edhr_release_dossier_requirements' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260726_mes_edhr_release_dossier_requirements' END;
