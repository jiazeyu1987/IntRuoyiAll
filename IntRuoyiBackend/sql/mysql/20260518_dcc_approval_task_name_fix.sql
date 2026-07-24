-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
SET NAMES utf8mb4;

START TRANSACTION;

UPDATE ACT_RU_TASK t
JOIN ACT_RE_PROCDEF d
  ON d.ID_ = t.PROC_DEF_ID_
 AND d.KEY_ = 'dcc-controlled-file-approval'
SET t.NAME_ = CASE t.TASK_DEF_KEY_
    WHEN 'DOC_CONTROL_REVIEW' THEN '文控审核'
    WHEN 'MATRIX_REVIEW' THEN '审核会签'
    WHEN 'MATRIX_APPROVAL' THEN '批准'
    WHEN 'DOC_CONTROL_APPROVAL' THEN '文控批准'
    ELSE t.NAME_
END
WHERE t.NAME_ LIKE '%?%'
  AND t.TASK_DEF_KEY_ IN (
    'DOC_CONTROL_REVIEW',
    'MATRIX_REVIEW',
    'MATRIX_APPROVAL',
    'DOC_CONTROL_APPROVAL'
  );

UPDATE ACT_HI_TASKINST t
JOIN ACT_RE_PROCDEF d
  ON d.ID_ = t.PROC_DEF_ID_
 AND d.KEY_ = 'dcc-controlled-file-approval'
SET t.NAME_ = CASE t.TASK_DEF_KEY_
    WHEN 'DOC_CONTROL_REVIEW' THEN '文控审核'
    WHEN 'MATRIX_REVIEW' THEN '审核会签'
    WHEN 'MATRIX_APPROVAL' THEN '批准'
    WHEN 'DOC_CONTROL_APPROVAL' THEN '文控批准'
    ELSE t.NAME_
END
WHERE t.NAME_ LIKE '%?%'
  AND t.TASK_DEF_KEY_ IN (
    'DOC_CONTROL_REVIEW',
    'MATRIX_REVIEW',
    'MATRIX_APPROVAL',
    'DOC_CONTROL_APPROVAL'
  );

UPDATE ACT_HI_ACTINST a
JOIN ACT_RE_PROCDEF d
  ON d.ID_ = a.PROC_DEF_ID_
 AND d.KEY_ = 'dcc-controlled-file-approval'
SET a.ACT_NAME_ = CASE a.ACT_ID_
    WHEN 'DOC_CONTROL_REVIEW' THEN '文控审核'
    WHEN 'MATRIX_REVIEW' THEN '审核会签'
    WHEN 'MATRIX_APPROVAL' THEN '批准'
    WHEN 'DOC_CONTROL_APPROVAL' THEN '文控批准'
    ELSE a.ACT_NAME_
END
WHERE a.ACT_TYPE_ = 'userTask'
  AND a.ACT_NAME_ LIKE '%?%'
  AND a.ACT_ID_ IN (
    'DOC_CONTROL_REVIEW',
    'MATRIX_REVIEW',
    'MATRIX_APPROVAL',
    'DOC_CONTROL_APPROVAL'
  );

UPDATE ACT_GE_BYTEARRAY b
JOIN ACT_RE_MODEL m
  ON m.EDITOR_SOURCE_VALUE_ID_ = b.ID_
 AND m.KEY_ = 'dcc-controlled-file-approval'
SET b.BYTES_ = REGEXP_REPLACE(
    REGEXP_REPLACE(
      REGEXP_REPLACE(
        REGEXP_REPLACE(
          CONVERT(b.BYTES_ USING utf8mb4),
          '<userTask id="DOC_CONTROL_REVIEW" name="[^"]*">',
          '<userTask id="DOC_CONTROL_REVIEW" name="文控审核">'
        ),
        '<userTask id="MATRIX_REVIEW" name="[^"]*">',
        '<userTask id="MATRIX_REVIEW" name="审核会签">'
      ),
      '<userTask id="MATRIX_APPROVAL" name="[^"]*">',
      '<userTask id="MATRIX_APPROVAL" name="批准">'
    ),
    '<userTask id="DOC_CONTROL_APPROVAL" name="[^"]*">',
    '<userTask id="DOC_CONTROL_APPROVAL" name="文控批准">'
  )
WHERE CONVERT(b.BYTES_ USING utf8mb4) REGEXP '<userTask id="(DOC_CONTROL_REVIEW|MATRIX_REVIEW|MATRIX_APPROVAL|DOC_CONTROL_APPROVAL)" name="[^"]*">';

UPDATE ACT_GE_BYTEARRAY b
JOIN ACT_RE_PROCDEF d
  ON d.DEPLOYMENT_ID_ = b.DEPLOYMENT_ID_
 AND d.RESOURCE_NAME_ = b.NAME_
 AND d.KEY_ = 'dcc-controlled-file-approval'
SET b.BYTES_ = REGEXP_REPLACE(
    REGEXP_REPLACE(
      REGEXP_REPLACE(
        REGEXP_REPLACE(
          CONVERT(b.BYTES_ USING utf8mb4),
          '<userTask id="DOC_CONTROL_REVIEW" name="[^"]*">',
          '<userTask id="DOC_CONTROL_REVIEW" name="文控审核">'
        ),
        '<userTask id="MATRIX_REVIEW" name="[^"]*">',
        '<userTask id="MATRIX_REVIEW" name="审核会签">'
      ),
      '<userTask id="MATRIX_APPROVAL" name="[^"]*">',
      '<userTask id="MATRIX_APPROVAL" name="批准">'
    ),
    '<userTask id="DOC_CONTROL_APPROVAL" name="[^"]*">',
    '<userTask id="DOC_CONTROL_APPROVAL" name="文控批准">'
  )
WHERE CONVERT(b.BYTES_ USING utf8mb4) REGEXP '<userTask id="(DOC_CONTROL_REVIEW|MATRIX_REVIEW|MATRIX_APPROVAL|DOC_CONTROL_APPROVAL)" name="[^"]*">';

COMMIT;
