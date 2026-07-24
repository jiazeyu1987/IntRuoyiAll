-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260518_dcc_approval_task_name_fix; type=data; riskLevel=medium
SET NAMES utf8mb4;

SET @dcc_controlled_file_approval_bpmn = '<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://www.flowable.org/processdef">
  <process id="dcc-controlled-file-approval" name="DCC Controlled File Approval" isExecutable="true">
    <userTask id="APPLICANT_REWORK" name="申请人修改">
      <extensionElements>
        <flowable:candidateStrategy>36</flowable:candidateStrategy>
        <flowable:approveMethod>1</flowable:approveMethod>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_applicant_rework_doc_control_review" sourceRef="APPLICANT_REWORK" targetRef="DOC_CONTROL_REVIEW" />

    <startEvent id="startEvent" name="Start" />
    <sequenceFlow id="flow_start_doc_control_review" sourceRef="startEvent" targetRef="DOC_CONTROL_REVIEW" />

    <userTask id="DOC_CONTROL_REVIEW" name="文控审核">
      <extensionElements>
        <flowable:candidateStrategy>35</flowable:candidateStrategy>
        <flowable:approveMethod>1</flowable:approveMethod>
      </extensionElements>
    </userTask>

    <sequenceFlow id="flow_doc_control_review_matrix_review" sourceRef="DOC_CONTROL_REVIEW" targetRef="MATRIX_REVIEW" />

    <userTask id="MATRIX_REVIEW" name="审核会签">
      <extensionElements>
        <flowable:candidateStrategy>34</flowable:candidateStrategy>
        <flowable:approveMethod>2</flowable:approveMethod>
      </extensionElements>
      <multiInstanceLoopCharacteristics isSequential="false" flowable:collection="${coll_userList}" flowable:elementVariable="assignee">
        <completionCondition>${ nrOfCompletedInstances/nrOfInstances >= 1.00 }</completionCondition>
      </multiInstanceLoopCharacteristics>
    </userTask>

    <sequenceFlow id="flow_matrix_review_matrix_approval" sourceRef="MATRIX_REVIEW" targetRef="MATRIX_APPROVAL" />

    <userTask id="MATRIX_APPROVAL" name="批准">
      <extensionElements>
        <flowable:candidateStrategy>34</flowable:candidateStrategy>
        <flowable:approveMethod>3</flowable:approveMethod>
      </extensionElements>
      <multiInstanceLoopCharacteristics isSequential="false" flowable:collection="${coll_userList}" flowable:elementVariable="assignee">
        <completionCondition>${ nrOfCompletedInstances > 0 }</completionCondition>
      </multiInstanceLoopCharacteristics>
    </userTask>

    <sequenceFlow id="flow_matrix_approval_doc_control_approval" sourceRef="MATRIX_APPROVAL" targetRef="DOC_CONTROL_APPROVAL" />

    <userTask id="DOC_CONTROL_APPROVAL" name="文控批准">
      <extensionElements>
        <flowable:candidateStrategy>34</flowable:candidateStrategy>
        <flowable:approveMethod>1</flowable:approveMethod>
      </extensionElements>
    </userTask>

    <sequenceFlow id="flow_doc_control_approval_end" sourceRef="DOC_CONTROL_APPROVAL" targetRef="endEvent" />
    <endEvent id="endEvent" name="End" />
  </process>
</definitions>';

DROP PROCEDURE IF EXISTS ensure_dcc_applicant_rework_process_present;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_applicant_rework_process_present()
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM ACT_RE_PROCDEF
      WHERE KEY_ = 'dcc-controlled-file-approval'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'dcc-controlled-file-approval process definition is missing';
  END IF;
  IF NOT EXISTS (
      SELECT 1
      FROM ACT_RE_MODEL
      WHERE KEY_ = 'dcc-controlled-file-approval'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'dcc-controlled-file-approval model is missing';
  END IF;
END$$
DELIMITER ;
CALL ensure_dcc_applicant_rework_process_present();
DROP PROCEDURE IF EXISTS ensure_dcc_applicant_rework_process_present;

START TRANSACTION;

UPDATE ACT_GE_BYTEARRAY b
JOIN ACT_RE_MODEL m
  ON m.EDITOR_SOURCE_VALUE_ID_ = b.ID_
 AND m.KEY_ = 'dcc-controlled-file-approval'
SET b.BYTES_ = CONVERT(@dcc_controlled_file_approval_bpmn USING BINARY)
WHERE CONVERT(b.BYTES_ USING utf8mb4) NOT LIKE '%id="APPLICANT_REWORK"%'
   OR CONVERT(b.BYTES_ USING utf8mb4) NOT LIKE '%flow_applicant_rework_doc_control_review%';

UPDATE ACT_GE_BYTEARRAY b
JOIN ACT_RE_PROCDEF d
  ON d.DEPLOYMENT_ID_ = b.DEPLOYMENT_ID_
 AND d.RESOURCE_NAME_ = b.NAME_
 AND d.KEY_ = 'dcc-controlled-file-approval'
SET b.BYTES_ = CONVERT(@dcc_controlled_file_approval_bpmn USING BINARY)
WHERE CONVERT(b.BYTES_ USING utf8mb4) NOT LIKE '%id="APPLICANT_REWORK"%'
   OR CONVERT(b.BYTES_ USING utf8mb4) NOT LIKE '%flow_applicant_rework_doc_control_review%';

UPDATE ACT_RU_TASK t
JOIN ACT_RE_PROCDEF d
  ON d.ID_ = t.PROC_DEF_ID_
 AND d.KEY_ = 'dcc-controlled-file-approval'
SET t.NAME_ = '申请人修改'
WHERE t.TASK_DEF_KEY_ = 'APPLICANT_REWORK';

UPDATE ACT_HI_TASKINST t
JOIN ACT_RE_PROCDEF d
  ON d.ID_ = t.PROC_DEF_ID_
 AND d.KEY_ = 'dcc-controlled-file-approval'
SET t.NAME_ = '申请人修改'
WHERE t.TASK_DEF_KEY_ = 'APPLICANT_REWORK';

UPDATE ACT_HI_ACTINST a
JOIN ACT_RE_PROCDEF d
  ON d.ID_ = a.PROC_DEF_ID_
 AND d.KEY_ = 'dcc-controlled-file-approval'
SET a.ACT_NAME_ = '申请人修改'
WHERE a.ACT_TYPE_ = 'userTask'
  AND a.ACT_ID_ = 'APPLICANT_REWORK';

COMMIT;
