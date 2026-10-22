"""Prepare exact tenant1 missing GxP operation registration; no database/network executor."""
from __future__ import annotations
import copy
import hashlib
import importlib.util
import json
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
BACKEND = ROOT / 'IntRuoyiBackend'
MAIN_TASK = Path('C:/IntRuoyiAll-int_main/doc/tasks/20261001-dcc-integration-unblock')
POLICY = BACKEND / 'config/gxp-audit-policy.yaml'
CORE = BACKEND / 'sql/mysql/20260908_gxp_audit_trail_core.sql'
PUBLISH = 'dcc.controlled-file.publish'
SCOPE = ('dcc.folder-template.save','dcc.folder-template.delete','dcc.project-attributes.configure',
 'dcc.project-product.resubmit','dcc.project-reference.create','dcc.project-reference.cancel','dcc.relation.replace',
 'dcc.relation.arrange','dcc.relation.controlled','dcc.relation.notification.retry','dcc.project-folder.create',
 'dcc.project-folder.update','dcc.project-file-placement.bind','dcc.project-folder.delete','dcc.project-product.reviewer-config',
 'dcc.project-product.create','dcc.project-product.review','dcc.project-product.approve','dcc.project-product.complete',
 'dcc.project-product.write-failed','dcc.project-product.retry','dcc.controlled-file.control','dcc.controlled-file.activate',
 'dcc.controlled-file.auto-obsolete','dcc.controlled-file.obsolete')
FIELDS = {'policy_version':'policyVersion','operation_id':'operationId','source_type':'sourceType','source_locator':'sourceLocator',
 'domain':'domain','subject_type':'subjectType','action_type':'actionType','reason_policy':'reasonPolicy','signature_policy':'signaturePolicy',
 'state_policy':'statePolicy','retention_class':'retentionClass','test_ids':'testIds','owner':'owner','applicability':'applicability'}
DB_FIELDS = tuple(FIELDS)
LENGTHS = {'policy_version':128,'operation_id':128,'source_type':64,'source_locator':512,'domain':64,'subject_type':64,'action_type':64,
 'reason_policy':64,'signature_policy':64,'state_policy':64,'retention_class':64,'test_ids':512,'owner':128,'applicability':64}
def sha(file):return hashlib.sha256(file.read_bytes()).hexdigest()
def formal_parser():
    path = BACKEND / 'script/gxp_audit_coverage_gate.py'
    spec=importlib.util.spec_from_file_location('g22_formal_policy_parser',path)
    module=importlib.util.module_from_spec(spec);sys.modules[spec.name]=module;spec.loader.exec_module(module)
    return module
def payload_hash(operations):
    exact=[{key:row[key] for key in FIELDS.values()} for row in operations]
    return hashlib.sha256(json.dumps(exact,sort_keys=True,separators=(',',':'),ensure_ascii=False).encode('utf-8')).hexdigest()

def prepare_package():
    parser=formal_parser();version,all_rows=parser.parse_policy(POLICY)
    dcc=[row for row in all_rows if row.operation_id.startswith('dcc.')]
    if len(dcc)!=26 or len({row.operation_id for row in dcc})!=26 or {row.operation_id for row in dcc}!={*SCOPE,PUBLISH}:
        raise ValueError('candidate must contain exactly preserved publish plus explicit25 DCC scope')
    text=POLICY.read_text(encoding='utf-8')
    approval=re.search(r'^approvalReference:\s*(\S+)\s*$',text,re.M)
    operations=[]
    for operation in dcc:
        if parser.REQUIRED_FIELDS-operation.values.keys():raise ValueError('formal parser required field missing')
        if not parser.source_locator_exists(BACKEND,operation):raise ValueError('sourceLocator unresolved: '+operation.operation_id)
        if operation.operation_id==PUBLISH:continue
        values={'policyVersion':version,**operation.values,'sourceLocatorVerified':True}
        for column,key in FIELDS.items():
            value=values[key]
            if not isinstance(value,str) or not value or len(value)>LENGTHS[column]:raise ValueError('exact field missing/overflow: '+key)
        if values['applicability']!='GXP' or values['domain']!='DCC':raise ValueError('unsupported applicability/domain')
        if not re.fullmatch(r'\[[^\[\]\n]+\]',values['testIds']):raise ValueError('formal testIds literal missing')
        operations.append(values)
    operations.sort(key=lambda row:row['operationId'])
    schema_log=MAIN_TASK/'g14-schema-complete.log';active_log=MAIN_TASK/'g21-gxp-active-policy.log'
    schema=schema_log.read_text(encoding='utf-8-sig')
    for column in DB_FIELDS:
        shape=f'schema\tgxp_audit_policy_operation\t{column}\tvarchar({LENGTHS[column]})\tNO\t'
        if shape not in schema:raise ValueError('recorded real policy schema shape mismatch: '+column)
    for column in ('active','deleted'):
        if f'schema\tgxp_audit_policy_operation\t{column}\tbit(1)\tNO\t' not in schema:raise ValueError('recorded bit shape mismatch')
    if 'index\tgxp_audit_policy_operation\tuk_gxp_audit_policy_operation\t0\t3\tpolicy_version\tNULL' not in schema:
        raise ValueError('recorded unique identity index missing')
    user_core=BACKEND/'sql/mysql/ruoyi-vue-pro.sql'
    user_text=user_core.read_text(encoding='utf-8')
    user_block=user_text[user_text.index('CREATE TABLE `system_users`'):user_text.index('CREATE TABLE `system_users`')+5500]
    for column,column_type in [('tenant_id','bigint'),('id','bigint'),('status','tinyint'),('deleted','bit(1)')]:
        if not re.search(rf'`{column}`\s+{re.escape(column_type)}',user_block):raise ValueError('formal enabled-user base schema missing: '+column)
    observed=[line.split('\t') for line in active_log.read_text(encoding='utf-8-sig').splitlines() if line.strip()]
    if len(observed)!=1 or observed[0][0]!=PUBLISH or observed[0][1]!='2026-09-approved-01':raise ValueError('readonly active policy baseline drift')
    publish=next(row for row in dcc if row.operation_id==PUBLISH)
    if observed[0][4]!=publish.source_locator:raise ValueError('observed publish source locator mismatch')
    approval_log=MAIN_TASK/'g21-audit-approval-readiness.log'
    old_approval=approval_log.read_text(encoding='utf-8-sig').splitlines()[0].split('\t')
    if len(old_approval)!=6 or old_approval[0]!='2026-09-approved-01':raise ValueError('recorded old policy approval baseline mismatch')
    annotations=parser.discover_annotations(BACKEND)
    formal_report=parser.canonical_report(version,all_rows,annotations)
    coverage_hash=hashlib.sha256(formal_report.encode('utf-8')).hexdigest()
    return {'status':'prepared_not_executed_quality_approval_pending','executionAuthorized':False,'tenantId':1,'database':'ruoyi-vue-pro',
      'candidateDccOperationCount':26,'policyVersion':version,'candidateApprovalReferenceObserved':approval.group(1) if approval else None,
      'qualityApproval':{'approvalReference':None,'approvedBy':None,'approvedAt':None,'coverageReportHash':None,
        'required':'Actual confirmed quality approval registry row for the exact candidate policy hash; operation SQL never creates it. Separate version registration requires its own authorization and actual approval facts.'},
      'sources':{'policy':{'path':'IntRuoyiBackend/config/gxp-audit-policy.yaml','sha256':sha(POLICY)},
        'formalUserCore':{'path':'IntRuoyiBackend/sql/mysql/ruoyi-vue-pro.sql','sha256':sha(user_core),'boundary':'recorded current schema did not cover system_users; fresh readonly inspection required before version template'},
        'formalParser':{'path':'IntRuoyiBackend/script/gxp_audit_coverage_gate.py','sha256':sha(BACKEND/'script/gxp_audit_coverage_gate.py')},
        'coreSql':{'path':'IntRuoyiBackend/sql/mysql/20260908_gxp_audit_trail_core.sql','sha256':sha(CORE)},
        'recordedRealSchema':{'path':'main/doc/tasks/20261001-dcc-integration-unblock/g14-schema-complete.log','sha256':sha(schema_log)},
        'readonlyActiveBaseline':{'path':'main/doc/tasks/20261001-dcc-integration-unblock/g21-gxp-active-policy.log','sha256':sha(active_log)}},
      'formalCoverage':{'kind':'formal parser canonical registration/source mapping; not executed business-test results or QA approval',
        'reportSha256':coverage_hash,'registeredOperationCount':len(all_rows),'discoveredAnnotationCount':len(annotations)},
      'preservedOldPolicyApproval':{'policyVersion':old_approval[0],'policyHash':old_approval[1],'approvedBy':old_approval[2],
        'approvedAt':old_approval[3],'approvalReference':old_approval[4],'coverageReportHash':old_approval[5],
        'meaning':'read-only historical record; does not prove that account1 is the current QA person and cannot approve the new file'},
      'qualityRegistration':{'executionAuthorized':False,'qualityRoleConfirmed':False,'tenantId':1,
        'approvedBy':None,'approvedAt':None,'approvalReference':None,'policyVersion':version,
        'policyHash':sha(POLICY),'coverageReportHash':coverage_hash,'maximumInsertedVersionRows':1,'updateCount':0,'deleteCount':0,
        'table':'gxp_audit_policy_version','separateFrom25OperationConfiguration':True,
        'requiredAuthorization':'Name the actual quality/QA approver and formal enabled same-tenant account, actual approval time/reference, and explicitly authorize local-test version registration. Developer/admin identity is not substituted.'},
      'preservedPublish':{'operationId':PUBLISH,'observedPolicyVersion':observed[0][1],'observedSourceLocator':observed[0][4],
        'action':'preserve all original fields/active/timestamps/row identity; never insert/update/delete this operation',
        'candidateDefinitionForReview':{'policyVersion':version,**publish.values}},
      'operations':operations,'exactOperationPayloadHash':payload_hash(operations),
      'impact':{'permanentDmlTables':['gxp_audit_policy_operation'],'readOnlyApprovalTable':'gxp_audit_policy_version',
        'firstInsertMaximum':25,'repeatExactInsertCount':0,'updateCount':0,'deleteCount':0,
        'forbidden':['business objects','gxp_audit_event','ledger','roles/permissions','BPM','gxp_audit_policy_version registration','coverage report registration','old publish row','other tenant/other operation rows'],
        'temporaryRoutine':'dcc_gxp25_tenant1_config','temporaryTable':'tmp_dcc_gxp25_tenant1',
        'runtimeBenefit':'Allows these declared existing DCC service mutations to append required GxP audit events; does not grant users business permissions or perform any business mutation.'},
      'executionPrerequisites':['User explicit tenant1 exact25 config-write approval (independent of pending19 migration question)',
        'Genuine quality approvalReference/approvedBy/approvedAt and exact-hash coverage approval; registry row preexisting or separately authorized registration',
        'Root fresh readonly target connection/schema/current all-operation snapshot and preserved publish snapshot',
        'Root table backup and pause policy writers; SQL rehearsal/negative/repeat verification before local test apply'],
      'notEvidenceOf':['MySQL execution','runtime strategy registered','quality approval','business UI E2E','whole goal complete']}

def validate_approval(package, tenant, approval):
    if tenant!=1 or package.get('tenantId')!=1:raise ValueError('tenant scope must be exactly1')
    if not approval or approval.get('approved') is not True:raise ValueError('real quality approval required')
    if approval.get('tenantId')!=1 or approval.get('policyVersion')!=package['policyVersion'] or approval.get('policyHash')!=package['sources']['policy']['sha256']:
        raise ValueError('quality approval policy hash/version/tenant mismatch')
    reference=approval.get('approvalReference')
    if not isinstance(reference,str) or not reference.strip() or len(reference)>256 or re.search(r'pending|placeholder|待确认',reference,re.I):raise ValueError('real nonpending quality approvalReference required')
    if reference==package['preservedOldPolicyApproval']['approvalReference']:raise ValueError('old approval reference cannot authorize new candidate file')
    actor=str(approval.get('approvedBy',''))
    if not re.fullmatch(r'[1-9][0-9]*',actor) or int(actor)>9223372036854775807:raise ValueError('actual quality approver identity required')
    if not re.fullmatch(r'\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}',str(approval.get('approvedAt',''))):raise ValueError('quality approval time required')
    if not re.fullmatch(r'[0-9a-f]{64}',str(approval.get('coverageReportHash',''))) or approval['coverageReportHash']=='0'*64:raise ValueError('actual coverage report hash required')
    if approval['coverageReportHash']!=package['formalCoverage']['reportSha256']:raise ValueError('approved coverage report hash does not match prepared formal report')

def plan(existing_rows, package, tenant, approval):
    validate_approval(package,tenant,approval)
    if len(package.get('operations',[]))!=25 or {row['operationId'] for row in package['operations']}!=set(SCOPE):raise ValueError('exact25 payload scope mismatch')
    if payload_hash(package['operations'])!=package['exactOperationPayloadHash']:raise ValueError('exact payload hash conflict')
    if sha(POLICY)!=package['sources']['policy']['sha256']:raise ValueError('source policy drift')
    rows=copy.deepcopy(existing_rows)
    active_publish=[row for row in rows if row.get('tenant_id')==1 and str(row.get('operation_id','')).rstrip(' ').casefold()==PUBLISH]
    if len(active_publish)!=1 or any(active_publish[0].get(key)!=value for key,value in {'operation_id':PUBLISH,'policy_version':'2026-09-approved-01',
       'active':1,'deleted':0,'applicability':'GXP','source_locator':package['preservedPublish']['observedSourceLocator']}.items()):raise ValueError('preserved publish baseline conflict')
    inserts=[]
    for operation in package['operations']:
        exact={'tenant_id':1,**{column:operation[key] for column,key in FIELDS.items()},'active':1,'deleted':0}
        found=[row for row in rows if row.get('tenant_id')==1 and str(row.get('operation_id','')).rstrip(' ').casefold()==operation['operationId']]
        if len(found)>1:raise ValueError('duplicate policy operation conflict')
        if found and any(found[0].get(column)!=value for column,value in exact.items()):raise ValueError('existing payload/version/active conflict: '+operation['operationId'])
        if not found:inserts.append(exact)
    return {'status':'VALIDATED_OFFLINE_ONLY','insertRows':inserts,'insertCount':len(inserts),'updateCount':0,'deleteCount':0,
      'preservedPublish':active_publish[0],'executionPerformed':False}

def quote(value):
    if not isinstance(value,str) or '\x00' in value or '\n' in value or '\r' in value or '\\' in value:raise ValueError('unsafe SQL literal')
    return "'"+value.replace("'","''")+"'"

def quality_registration_plan(existing_versions,package,approval,*,quality_role_confirmed=False,write_authorized=False):
    if quality_role_confirmed is not True:raise ValueError('actual quality role identity must be explicitly confirmed')
    if write_authorized is not True:raise ValueError('separate local-test quality registration write authorization required')
    validate_approval(package,1,approval)
    exact={'tenant_id':1,'policy_version':package['policyVersion'],'policy_hash':package['sources']['policy']['sha256'],
      'approved_by':int(approval['approvedBy']),'approved_at':approval['approvedAt'],'approval_reference':approval['approvalReference'],
      'coverage_report_hash':package['formalCoverage']['reportSha256']}
    found=[row for row in existing_versions if row.get('tenant_id')==1 and str(row.get('policy_version','')).rstrip(' ').casefold()==package['policyVersion']]
    if len(found)>1 or found and any(found[0].get(key)!=value for key,value in exact.items()):raise ValueError('quality version registration conflict, no overwrite')
    return {'insertRows':[] if found else [exact],'updateCount':0,'deleteCount':0,'executionPerformed':False}

def render_quality_registration_sql(package):
    version=quote(package['policyVersion']);policy_hash=quote(package['sources']['policy']['sha256']);coverage=quote(package['formalCoverage']['reportSha256'])
    old_reference=quote(package['preservedOldPolicyApproval']['approvalReference'])
    return f'''-- REVIEW ONLY: separate maximum1 tenant1 quality-version registration; not a25-operation config or production CSV approval.
-- Actual quality approver/time/reference variables intentionally unset; never default to admin/account1/old approval.
-- Prepared candidate and source mapping hashes are fixed; no policy file modification.
DELIMITER $$
CREATE PROCEDURE `dcc_gxp1_tenant1_quality`(IN quality_reference text, IN quality_approver bigint, IN quality_time datetime)
BEGIN
  DECLARE row_count int DEFAULT 0;
  DECLARE lock_owned int DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF lock_owned=1 THEN DO RELEASE_LOCK('dcc:gxp25:tenant1'); END IF;
    RESIGNAL;
  END;
  IF DATABASE() IS NULL OR DATABASE()<>'ruoyi-vue-pro' OR @dcc_gxp_quality_version_write_authorized IS NULL OR @dcc_gxp_quality_version_write_authorized<>1
    OR @dcc_gxp_actual_quality_role_confirmed IS NULL OR @dcc_gxp_actual_quality_role_confirmed<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='separate local-test write and actual quality-role identity confirmed required';
  END IF;
  IF quality_reference IS NULL OR TRIM(quality_reference)='' OR CHAR_LENGTH(quality_reference)>256 OR LOWER(quality_reference) LIKE '%pending%'
    OR LOWER(quality_reference) LIKE '%placeholder%' OR BINARY quality_reference=BINARY {old_reference}
    OR quality_approver IS NULL OR quality_approver<1 OR quality_time IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='actual new quality approval person/time/reference required';
  END IF;
  SELECT GET_LOCK('dcc:gxp25:tenant1',10) INTO lock_owned;
  IF lock_owned IS NULL OR lock_owned<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='policy registration lock unavailable'; END IF;
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME='gxp_audit_policy_version' AND ENGINE='InnoDB')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='transactional quality table engine required';
  END IF;
  SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
  START TRANSACTION;
  -- Existence/enablement is required but is not a substituted QA-role approval proof.
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_users'
    AND ((COLUMN_NAME IN ('id','tenant_id') AND DATA_TYPE='bigint') OR (COLUMN_NAME='status' AND DATA_TYPE='tinyint') OR (COLUMN_NAME='deleted' AND COLUMN_TYPE='bit(1)')))<>4 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='actual system_users identity schema must be readonly verified';
  END IF;
  IF (SELECT COUNT(*) FROM `system_users` WHERE id=quality_approver AND tenant_id=1 AND status=0 AND deleted=b'0')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='quality approver must be an actual enabled tenant1 account';
  END IF;
  SELECT id FROM `gxp_audit_policy_version` WHERE tenant_id=1 AND policy_version={version} FOR UPDATE;
  IF (SELECT COUNT(*) FROM `gxp_audit_policy_version` WHERE tenant_id=1 AND policy_version={version})>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='duplicate quality version conflict';
  END IF;
  IF EXISTS (SELECT 1 FROM `gxp_audit_policy_version` WHERE tenant_id=1 AND policy_version={version}
    AND NOT(BINARY policy_version <=> BINARY {version} AND BINARY policy_hash <=> BINARY {policy_hash}
      AND approved_by <=> quality_approver AND approved_at <=> quality_time
      AND BINARY approval_reference <=> BINARY quality_reference AND BINARY coverage_report_hash <=> BINARY {coverage})) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='existing approved quality version conflict, no overwrite';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM `gxp_audit_policy_version` WHERE tenant_id=1 AND policy_version={version}) THEN
    INSERT INTO `gxp_audit_policy_version` (`tenant_id`,`policy_version`,`policy_hash`,`approved_by`,`approved_at`,`approval_reference`,`coverage_report_hash`)
    VALUES(1,{version},{policy_hash},quality_approver,quality_time,quality_reference,{coverage});
    SET row_count=ROW_COUNT();
    IF row_count<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='quality version insert count must be1'; END IF;
  END IF;
  COMMIT;
  DO RELEASE_LOCK('dcc:gxp25:tenant1');
  SELECT row_count AS inserted_quality_version,0 AS updated_quality_version,0 AS deleted_quality_version;
END$$
DELIMITER ;
CALL `dcc_gxp1_tenant1_quality`(@dcc_gxp_quality_approval_reference,@dcc_gxp_quality_approved_by,@dcc_gxp_quality_approved_at);
DROP PROCEDURE `dcc_gxp1_tenant1_quality`;
'''

def render_sql(package):
    columns=list(DB_FIELDS);columns_sql=', '.join('`'+column+'`' for column in columns)
    matching=' AND\n      '.join(f'BINARY existing.`{column}` <=> BINARY candidate.`{column}`' for column in columns)
    exact_ids=', '.join(quote(value) for value in sorted(SCOPE))
    payloads=[]
    for operation in package['operations']:
        payloads.append('-- exact-operation: '+operation['operationId']+'\n('+', '.join(quote(operation[FIELDS[column]]) for column in columns)+')')
    table_shape=',\n  '.join(f'`{column}` varchar({LENGTHS[column]}) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL' for column in columns)
    publish=package['preservedPublish'];policy_hash=package['sources']['policy']['sha256']
    return f'''-- REVIEW ONLY: not executed; exact25 tenant1 operation registration, separate from19SQL migration scope.
-- Source policy raw SHA256: {policy_hash}
-- Real quality approval variables intentionally unset; fill only from an actual confirmed registry record.
-- Requires fresh connection, policy-writer pause, backup and reviewed MySQL rehearsal. Never use --force.
-- Only permanent DML target is gxp_audit_policy_operation; no existing row is changed.
-- CREATE PROCEDURE collision fails; no preemptive DROP of an existing routine.
DELIMITER $$
CREATE PROCEDURE `dcc_gxp25_tenant1_config`(IN quality_reference text, IN quality_approver bigint)
BEGIN
  DECLARE changed_rows int DEFAULT 0;
  DECLARE conflict_rows int DEFAULT 0;
  DECLARE missing_rows int DEFAULT 0;
  DECLARE lock_owned int DEFAULT 0;
  DECLARE temporary_owned int DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF temporary_owned=1 THEN DROP TEMPORARY TABLE `tmp_dcc_gxp25_tenant1`; END IF;
    IF lock_owned=1 THEN DO RELEASE_LOCK('dcc:gxp25:tenant1'); END IF;
    RESIGNAL;
  END;
  IF DATABASE() IS NULL OR DATABASE() <> 'ruoyi-vue-pro' OR @dcc_gxp_tenant1_write_authorized IS NULL OR @dcc_gxp_tenant1_write_authorized <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='tenant1 exact25 config-write authorization required';
  END IF;
  IF quality_reference IS NULL OR TRIM(quality_reference)='' OR CHAR_LENGTH(quality_reference)>256 OR LOWER(quality_reference) LIKE '%pending%'
    OR LOWER(quality_reference) LIKE '%placeholder%' OR BINARY quality_reference=BINARY {quote(package['preservedOldPolicyApproval']['approvalReference'])}
    OR quality_approver IS NULL OR quality_approver<1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='real quality approval record required, not candidate PENDING';
  END IF;
  SELECT GET_LOCK('dcc:gxp25:tenant1',10) INTO lock_owned;
  IF lock_owned IS NULL OR lock_owned<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='policy config lock unavailable'; END IF;
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version') AND ENGINE='InnoDB')<>2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='transactional audit table engines required';
  END IF;
  CREATE TEMPORARY TABLE `tmp_dcc_gxp25_tenant1` (
  {table_shape},
  PRIMARY KEY (`operation_id`)
  ) ENGINE=InnoDB;
  SET temporary_owned=1;
  INSERT INTO `tmp_dcc_gxp25_tenant1` ({columns_sql}) VALUES
  {',\n  '.join(payloads)};
  IF (SELECT COUNT(*) FROM `tmp_dcc_gxp25_tenant1`)<>25 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='candidate scope not25'; END IF;
  SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
  START TRANSACTION;
  -- Lock actual approval row and policy range; ordinary policy writers must be paused by Root.
  SELECT id FROM `gxp_audit_policy_version` WHERE tenant_id=1 AND BINARY policy_version=BINARY {quote(package['policyVersion'])} FOR UPDATE;
  IF (SELECT COUNT(*) FROM `gxp_audit_policy_version` WHERE tenant_id=1
    AND BINARY policy_version=BINARY {quote(package['policyVersion'])} AND BINARY policy_hash=BINARY {quote(policy_hash)}
    AND BINARY approval_reference=BINARY quality_reference AND approved_by=quality_approver AND approved_at IS NOT NULL
    AND BINARY coverage_report_hash=BINARY {quote(package['formalCoverage']['reportSha256'])})<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='exact-hash approved quality registry missing/conflicting';
  END IF;
  SELECT id FROM `gxp_audit_policy_operation` WHERE tenant_id=1
    AND (operation_id IN ({exact_ids}) OR operation_id='dcc.controlled-file.publish') ORDER BY id FOR UPDATE;
  IF (SELECT COUNT(*) FROM `gxp_audit_policy_operation` WHERE tenant_id=1 AND operation_id='dcc.controlled-file.publish')<>1
    OR (SELECT COUNT(*) FROM `gxp_audit_policy_operation` WHERE tenant_id=1 AND BINARY operation_id=BINARY 'dcc.controlled-file.publish'
      AND BINARY policy_version=BINARY {quote(publish['observedPolicyVersion'])} AND active=b'1' AND deleted=b'0' AND BINARY applicability=BINARY 'GXP'
      AND BINARY source_locator=BINARY {quote(publish['observedSourceLocator'])})<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='preserved publish baseline conflict';
  END IF;
  SELECT COUNT(*) INTO conflict_rows FROM (
    SELECT candidate.operation_id FROM `tmp_dcc_gxp25_tenant1` candidate
    JOIN `gxp_audit_policy_operation` existing ON existing.tenant_id=1 AND existing.operation_id=candidate.operation_id
    GROUP BY candidate.operation_id HAVING COUNT(*)<>1 OR SUM(existing.active=b'1' AND existing.deleted=b'0' AND
      {matching})<>1
  ) conflicts;
  IF conflict_rows<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='existing policy payload conflict; no overwrite allowed'; END IF;
  SELECT COUNT(*) INTO missing_rows FROM `tmp_dcc_gxp25_tenant1` candidate
    WHERE NOT EXISTS (SELECT 1 FROM `gxp_audit_policy_operation` existing WHERE existing.tenant_id=1 AND existing.operation_id=candidate.operation_id);
  IF missing_rows>0 THEN
  INSERT INTO `gxp_audit_policy_operation` (`tenant_id`, {columns_sql}, `active`, `deleted`)
  SELECT 1, {', '.join('candidate.`'+column+'`' for column in columns)}, b'1', b'0'
  FROM `tmp_dcc_gxp25_tenant1` candidate
  WHERE NOT EXISTS (SELECT 1 FROM `gxp_audit_policy_operation` existing
    WHERE existing.tenant_id=1 AND existing.operation_id=candidate.operation_id);
  SET changed_rows=ROW_COUNT();
  END IF;
  IF changed_rows<0 OR changed_rows>25 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='insert count outside25 scope'; END IF;
  IF (SELECT COUNT(*) FROM `gxp_audit_policy_operation` existing JOIN `tmp_dcc_gxp25_tenant1` candidate
    ON existing.tenant_id=1 AND existing.operation_id=candidate.operation_id
    WHERE existing.active=b'1' AND existing.deleted=b'0' AND {matching})<>25 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='post-insert exact25 payload validation failed';
  END IF;
  COMMIT;
  DROP TEMPORARY TABLE `tmp_dcc_gxp25_tenant1`;
  SET temporary_owned=0;
  DO RELEASE_LOCK('dcc:gxp25:tenant1');
  SELECT changed_rows AS inserted_operations, 0 AS updated_operations, 0 AS deleted_operations;
END$$
DELIMITER ;
CALL `dcc_gxp25_tenant1_config`(@dcc_gxp_quality_approval_reference, @dcc_gxp_quality_approved_by);
DROP PROCEDURE `dcc_gxp25_tenant1_config`;
'''

def main():
    package=prepare_package()
    sql=render_sql(package)
    (HERE/'g22-gxp-25-operation-impact.json').write_text(json.dumps(package,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (HERE/'g22-gxp-25-operation-config.review.sql').write_text(sql,encoding='utf-8')
    (HERE/'g22-gxp-quality-version-registration.review.sql').write_text(render_quality_registration_sql(package),encoding='utf-8')
    (HERE/'g22-quality-approval-input.template.json').write_text(json.dumps({
      'scope':'Local tenant1 test configuration only; not production CSV approval',
      'tenantId':1,'policyVersion':package['policyVersion'],'policyHash':package['sources']['policy']['sha256'],
      'coverageReportHash':package['formalCoverage']['reportSha256'],
      'actualQualityApproval':{'approved':None,'approverName':None,'qualityRoleBasis':None,'approvedBy':None,
        'approvedAt':None,'timeZone':'Asia/Shanghai','approvalReference':None,'qualitySignatureEvidenceReference':None},
      'explicitLocalWriteAuthorization':{'registerMaximumOneNewVersionRow':False,'registerExact25OperationRows':False},
      'instructions':'Input actual independently confirmed facts in a separate copy; do not guess IDs, use admin by default, reuse old approval, or place credentials in this file.'
    },ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    parser=formal_parser();version,operations=parser.parse_policy(POLICY)
    report=parser.canonical_report(version,operations,parser.discover_annotations(BACKEND))
    (HERE/'g22-formal-policy-source-coverage.txt').write_bytes(report.encode('utf-8'))
    (HERE/'g22-gxp-readonly-preflight.sql').write_text('''-- READ ONLY. Fresh target/source/hash and actual quality actor checks before any config authorization.
SELECT DATABASE() AS actual_database,@@hostname AS actual_host,@@port AS actual_server_port;
SELECT TABLE_NAME,ENGINE,TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
 AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version','system_users');
SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT,EXTRA,COLLATION_NAME FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version','system_users')
 ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT TABLE_NAME,INDEX_NAME,NON_UNIQUE,SEQ_IN_INDEX,COLUMN_NAME,SUB_PART FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version')
 ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX;
SELECT id,tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,reason_policy,
 signature_policy,state_policy,retention_class,test_ids,owner,applicability,HEX(active) AS active_hex,HEX(deleted) AS deleted_hex,
 create_time,update_time,creator,updater FROM gxp_audit_policy_operation ORDER BY tenant_id,operation_id,policy_version,id;
SELECT id,tenant_id,policy_version,policy_hash,approved_by,approved_at,approval_reference,coverage_report_hash,create_time
 FROM gxp_audit_policy_version ORDER BY tenant_id,policy_version,id;
-- Provide the actual externally-confirmed quality account ID; no account/admin assumption.
SELECT id,tenant_id,username,nickname,status,HEX(deleted) AS deleted_hex FROM system_users
 WHERE tenant_id=1 AND id=@dcc_gxp_quality_approved_by;
''',encoding='utf-8')
    print('PREPARED ONLY:26 candidate operations,25 exact tenant1 inserts; real approval pending; no database executor.')
if __name__=='__main__':main()
