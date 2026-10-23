"""Pure exact26 development configuration preparation/readonly validation. No DB executor."""
from __future__ import annotations
import argparse
import copy
import hashlib
import importlib.util
import json
import sys
import unicodedata
from pathlib import Path

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('g43_frozen_g28_primitives',HERE/'g28_config26.py')
old=importlib.util.module_from_spec(spec);sys.modules[spec.name]=old;spec.loader.exec_module(old)
base=old.base
VERSION='2026-10-dcc-integration-01'
DESCRIPTOR={'status':'DEVELOPMENT_TEST_ONLY','tenantId':'1','policyVersion':VERSION,'policyHash':old.POLICY_SHA,'coverageReportHash':old.COVERAGE_SHA}

def require(value,message):
    if not value:raise ValueError(message)

def fixed_environment():
    """Offline contract fixture only; Root must derive actual values from the live approved source session."""
    return {'database':'ruoyi-vue-pro','serverUuid':old.UUID,'connectionHost':'127.0.0.1','connectionPort':23306,
            'mysqlVersion':'8.0.40','timeZone':'+08:00','connectionCharset':'utf8mb4','connectionCollation':'utf8mb4_unicode_ci',
            'tenantId':1,'sourceSchemaReady':True,'allApplicationWritersExcluded':True}

def verify_context(p,descriptor,environment,write_authorized):
    require(write_authorized is True,'actual development configuration write authorization required')
    require(descriptor==DESCRIPTOR and all(type(descriptor.get(k))is str for k in DESCRIPTOR),'exact development-only descriptor required; no QA identity/signature')
    require(environment==fixed_environment() and all(type(environment[k])is type(v)for k,v in fixed_environment().items()),'exact fresh local23306/source UUID/Shanghai/schema/writer contract required')
    require(p.get('tenantId')==1 and p.get('database')=='ruoyi-vue-pro' and p.get('policyVersion')==VERSION
            and p['sources']['policy']['sha256']==old.POLICY_SHA and p['formalCoverage']['reportSha256']==old.COVERAGE_SHA,'exact candidate context changed')
    require(len(p.get('operations',[]))==26 and {x['operationId']for x in p['operations']}==set(old.SCOPE)
            and base.payload_hash(p['operations'])==p['exactOperationPayloadHash'],'exact26 payload changed')
    require(base.sha(base.POLICY)==old.POLICY_SHA,'authoritative policy bytes drift')
    for op in p['operations']:
        for column,key in base.FIELDS.items():require(type(op.get(key))is str and 0<len(op[key])<=base.LENGTHS[column],'candidate exact field type/length changed')

def namespace(value):
    return ''.join(c for c in unicodedata.normalize('NFKD',value.rstrip(' ').casefold())if not unicodedata.combining(c))

def plan(rows,p,descriptor,environment,*,write_authorized=False):
    verify_context(p,descriptor,environment,write_authorized)
    require(isinstance(rows,list),'actual full operation row list required')
    targets=set(old.SCOPE)|{base.PUBLISH}
    for row in rows:
        require(type(row.get('tenant_id'))is int and row['tenant_id']>=1 and type(row.get('operation_id'))is str,'actual tenant/operation row types required')
        if row['tenant_id']==1 and namespace(row['operation_id'])in targets:
            require(all(type(row.get(k))is int and row[k]in(0,1)for k in ['active','deleted']),'exact database bit integer values required')
    publishes=[r for r in rows if r['tenant_id']==1 and namespace(r['operation_id'])==base.PUBLISH]
    expected_publish={'operation_id':base.PUBLISH,'policy_version':'2026-09-approved-01','source_locator':p['preservedPublish']['observedSourceLocator'],'active':1,'deleted':0,'applicability':'GXP'}
    require(len(publishes)==1 and all(type(publishes[0].get(k))is type(v)and publishes[0][k]==v for k,v in expected_publish.items()),'preserved publish09 baseline conflict')
    inserts=[];found_count=0
    for op in p['operations']:
        exact={'tenant_id':1,**{db:op[key]for db,key in base.FIELDS.items()},'active':1,'deleted':0}
        found=[r for r in rows if r['tenant_id']==1 and namespace(r['operation_id'])==op['operationId']]
        require(len(found)<=1 and (not found or all(type(found[0].get(k))is type(v)and found[0][k]==v for k,v in exact.items())),'whole batch existing namespace/payload/version/state conflict')
        if found:found_count+=1
        else:inserts.append(exact)
    require(found_count in (0,26),'partial26 namespace forbidden; review actual prior execution before repair')
    return {'status':'DEVELOPMENT_TEST_ONLY_PLAN_NOT_SQL_EXECUTION','insertRows':inserts,'insertCount':len(inserts),'qualityVersionInsertCount':0,'updateCount':0,'deleteCount':0,'executionPerformed':False}

def postflight(before,after,before_versions,after_versions,p,descriptor,environment,*,expected_inserts):
    require(type(expected_inserts)is int and expected_inserts in (0,26),'exact first26/replay0 count required')
    require(before_versions==after_versions,'quality/approval/version records changed; development package never writes them')
    ids=[r.get('id')for r in after];require(all(type(x)is int and x>0 for x in ids)and len(ids)==len(set(ids)),'exact actual operation row ids required')
    byid={r['id']:r for r in after}
    require(all(type(r.get('id'))is int and r['id']in byid and byid[r['id']]==r for r in before),'original operation metadata/payload changed')
    oldids={r['id']for r in before};new=[r for r in after if r['id']not in oldids]
    require(len(new)==expected_inserts and all(r.get('tenant_id')==1 and r.get('operation_id')in old.SCOPE for r in new),'outside exact26 insertion scope')
    require(plan(after,p,descriptor,environment,write_authorized=True)['insertCount']==0,'postflight exact26 allmatch required')
    return {'status':'DEVELOPMENT_TEST_ONLY_READONLY_POSTFLIGHT','operationInsertCount':len(new),'qualityVersionInsertCount':0,'originalOperationsUnchanged':True,'allQualityVersionRowsUnchanged':True,'actualDatabaseExecutionProven':False}

def sql(p):
    columns=list(base.FIELDS);quote=base.quote
    routine='dcc_g43_dev26_config';temp='tmp_dcc_g43_dev26';lock='dcc:g43:dev26:tenant1'
    defs=',\n'.join(f' `{c}` VARCHAR({base.LENGTHS[c]}) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL'for c in columns)
    rows=',\n'.join('  -- exact-operation: '+op['operationId']+'\n('+', '.join(quote(op[base.FIELDS[c]])for c in columns)+')'for op in p['operations'])
    target=', '.join(quote(x)for x in sorted(old.SCOPE));fields=' AND\n      '.join(f'BINARY existing.`{c}` <=> BINARY candidate.`{c}`'for c in columns)
    columnlist=', '.join('`'+c+'`'for c in columns);selectlist=', '.join('candidate.`'+c+'`'for c in columns)
    schema_conditions=' OR\n    '.join(f"(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME={quote(c)} AND COLUMN_TYPE={quote('varchar('+str(base.LENGTHS[c])+')')} AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1"for c in columns)
    return f"""-- DEVELOPMENT_TEST_ONLY: exact26 operations, no QA approval/version row or business action.
-- Candidate version {VERSION}; policySHA {old.POLICY_SHA}; coverageSHA {old.COVERAGE_SHA}
-- Root-only actual execution after fresh local23306 identity, all-row backup/writer exclusion.
-- No preDROP, INSERT IGNORE/upsert/update/delete/--force. Helper collision stops before CALL.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone='+08:00';
DELIMITER $$
CREATE PROCEDURE `{routine}`()
BEGIN
  DECLARE changed_rows INT DEFAULT 0;
  DECLARE conflicts INT DEFAULT 0;
  DECLARE existing_rows INT DEFAULT 0;
  DECLARE lock_owned INT DEFAULT 0;
  DECLARE temporary_owned INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF temporary_owned=1 THEN DROP TEMPORARY TABLE `{temp}`; END IF;
    IF lock_owned=1 THEN DO RELEASE_LOCK('{lock}'); END IF;
    RESIGNAL;
  END;
  IF @dcc_g43_dev_only IS NULL OR @dcc_g43_dev_only<>1 OR @dcc_g43_write_authorized IS NULL OR @dcc_g43_write_authorized<>1
    OR @dcc_g43_policy_sha IS NULL OR BINARY @dcc_g43_policy_sha<>BINARY {quote(old.POLICY_SHA)}
    OR @dcc_g43_coverage_sha IS NULL OR BINARY @dcc_g43_coverage_sha<>BINARY {quote(old.COVERAGE_SHA)}
    OR DATABASE()<>'ruoyi-vue-pro' OR @@server_uuid<>{quote(old.UUID)} OR @@version<>'8.0.40' OR @@autocommit<>1
    OR @@character_set_connection<>'utf8mb4' OR @@collation_connection<>'utf8mb4_unicode_ci' OR @@session.time_zone<>'+08:00'
    OR FIND_IN_SET('STRICT_TRANS_TABLES',@@session.sql_mode)=0 OR FIND_IN_SET('NO_ENGINE_SUBSTITUTION',@@session.sql_mode)=0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='exact authorized local source development session required';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version','dcc_legacy_source_name_scope','dcc_legacy_source_name_evidence','dcc_source_name_reservation') AND ENGINE='InnoDB')<>5
    OR (SELECT COUNT(*) FROM infra_release_migration WHERE migration_id='20261003_dcc_legacy_source_name_occupancy' AND sha256='621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a' AND status='APPLIED' AND target_environment='test' AND deleted=b'0')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='actual source sidecar prerequisite missing';
  END IF;
  IF {schema_conditions}
    OR (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME IN ('active','deleted') AND COLUMN_TYPE='bit(1)' AND IS_NULLABLE='NO')<>2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='exact operation schema field types/collation missing';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND INDEX_NAME='uk_gxp_audit_policy_operation')<>3
    OR (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND INDEX_NAME='uk_gxp_audit_policy_operation' AND NON_UNIQUE=0 AND SUB_PART IS NULL AND ((SEQ_IN_INDEX=1 AND COLUMN_NAME='tenant_id') OR (SEQ_IN_INDEX=2 AND COLUMN_NAME='operation_id') OR (SEQ_IN_INDEX=3 AND COLUMN_NAME='policy_version')))<>3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='exact operation unique namespace index required';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND EVENT_OBJECT_TABLE='gxp_audit_policy_operation')<>0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='unexpected operation-table trigger blocks development insert';
  END IF;
  SELECT GET_LOCK('{lock}',0) INTO lock_owned;
  IF lock_owned IS NULL OR lock_owned<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='development policy writer lock unavailable'; END IF;
  CREATE TEMPORARY TABLE `{temp}` (
{defs},
 PRIMARY KEY (`operation_id`)
  ) ENGINE=InnoDB;
  SET temporary_owned=1;
  INSERT INTO `{temp}` ({columnlist}) VALUES
{rows};
  IF (SELECT COUNT(*) FROM `{temp}`)<>26 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='candidate scope not exact26'; END IF;
  SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
  START TRANSACTION;
  SELECT id FROM gxp_audit_policy_operation WHERE tenant_id=1 AND (operation_id IN ({target}) OR operation_id={quote(base.PUBLISH)}) ORDER BY id FOR UPDATE;
  IF (SELECT COUNT(*) FROM gxp_audit_policy_operation WHERE tenant_id=1 AND operation_id={quote(base.PUBLISH)})<>1
    OR (SELECT COUNT(*) FROM gxp_audit_policy_operation WHERE tenant_id=1 AND BINARY operation_id=BINARY {quote(base.PUBLISH)} AND BINARY policy_version=BINARY '2026-09-approved-01' AND active=b'1' AND deleted=b'0' AND BINARY applicability=BINARY 'GXP' AND BINARY source_locator=BINARY {quote(p['preservedPublish']['observedSourceLocator'])})<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='preserved publish09 baseline conflict';
  END IF;
  SELECT COUNT(*) INTO conflicts FROM (
    SELECT candidate.operation_id FROM `{temp}` candidate JOIN gxp_audit_policy_operation existing ON existing.tenant_id=1 AND existing.operation_id=candidate.operation_id
    GROUP BY candidate.operation_id HAVING COUNT(*)<>1 OR SUM(existing.active=b'1' AND existing.deleted=b'0' AND {fields})<>1
  ) invalid_rows;
  IF conflicts<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='existing namespace payload/version/state conflict'; END IF;
  SELECT COUNT(*) INTO existing_rows FROM gxp_audit_policy_operation WHERE tenant_id=1 AND operation_id IN ({target});
  IF existing_rows NOT IN (0,26) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='partial26 namespace requires reviewed repair'; END IF;
  IF existing_rows=0 THEN
    INSERT INTO gxp_audit_policy_operation (`tenant_id`, {columnlist}, `active`, `deleted`)
    SELECT 1, {selectlist}, b'1', b'0' FROM `{temp}` candidate;
    SET changed_rows=ROW_COUNT();
    IF changed_rows<>26 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='exact26 insertion count failed'; END IF;
  END IF;
  IF (SELECT COUNT(*) FROM gxp_audit_policy_operation existing JOIN `{temp}` candidate ON existing.tenant_id=1 AND existing.operation_id=candidate.operation_id
      WHERE existing.active=b'1' AND existing.deleted=b'0' AND {fields})<>26 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='post-insert exact26 payload failed';
  END IF;
  COMMIT;
  DROP TEMPORARY TABLE `{temp}`;
  SET temporary_owned=0;
  DO RELEASE_LOCK('{lock}');
  SET lock_owned=0;
  SELECT 'DEVELOPMENT_TEST_ONLY' AS configuration_scope,changed_rows AS inserted_operations,0 AS quality_version_inserts,0 AS updated_operations,0 AS deleted_operations;
END$$
DELIMITER ;
CALL `{routine}`();
DROP PROCEDURE `{routine}`;
"""

def readonly_sql():
    return """-- READONLY actual source capture. Root keeps full raw rows and all original metadata.
SELECT DATABASE() AS database_name,@@server_uuid AS server_uuid,@@version AS mysql_version,@@session.time_zone AS time_zone,@@character_set_connection AS connection_charset,@@collation_connection AS connection_collation,@@session.sql_mode AS sql_mode;
SELECT TABLE_NAME,ENGINE,TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version','dcc_legacy_source_name_scope','dcc_legacy_source_name_evidence','dcc_source_name_reservation');
SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLLATION_NAME,ORDINAL_POSITION FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' ORDER BY ORDINAL_POSITION;
SELECT TABLE_NAME,INDEX_NAME,NON_UNIQUE,SEQ_IN_INDEX,COLUMN_NAME,SUB_PART FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' ORDER BY INDEX_NAME,SEQ_IN_INDEX;
SELECT * FROM gxp_audit_policy_operation ORDER BY id;
SELECT * FROM gxp_audit_policy_version ORDER BY id;
SELECT migration_id,sha256,status,target_environment,deleted+0 AS deleted FROM infra_release_migration WHERE migration_id='20261003_dcc_legacy_source_name_occupancy';
SELECT ROUTINE_NAME,ROUTINE_TYPE,CREATED,LAST_ALTERED FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME='dcc_g43_dev26_config';
SELECT TRIGGER_NAME,EVENT_MANIPULATION,ACTION_TIMING FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND EVENT_OBJECT_TABLE='gxp_audit_policy_operation';
"""

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('mode',choices=['prepare','preflight','postflight']);parser.add_argument('--before',type=Path);parser.add_argument('--after',type=Path);parser.add_argument('--output',type=Path);args=parser.parse_args();p=old.package()
    if args.mode=='prepare':
        (HERE/'g43-dev-policy.json').write_text(json.dumps(DESCRIPTOR,indent=2)+'\n',encoding='utf-8')
        (HERE/'g43-dev-config26.review.sql').write_text(sql(p),encoding='utf-8')
        for mode in ['preflight','postflight']:(HERE/f'g43-dev-config26-readonly-{mode}.sql').write_text(readonly_sql(),encoding='utf-8')
        impact={'status':'DEVELOPMENT_TEST_ONLY_PREPARED_NOT_EXECUTED','descriptor':DESCRIPTOR,'operations':p['operations'],'exactOperationPayloadHash':p['exactOperationPayloadHash'],'firstInsertCount':26,'replayInsertCount':0,'qualityVersionInsertCount':0,'updateCount':0,'deleteCount':0,'actualDatabaseExecutor':False,'actualQualityApproved':False,'permanentTable':'gxp_audit_policy_operation','environmentContract':fixed_environment()}
        (HERE/'g43-dev-config26-impact.json').write_text(json.dumps(impact,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');print('DEVELOPMENT_TEST_ONLY_PREPARED_EXACT26_NO_QUALITY_RECORD');return
    require(args.before and args.output and not args.output.exists(),'exact readonly input/new receipt output required')
    before=json.loads(args.before.read_text(encoding='utf-8-sig'));environment=before['environment']
    if args.mode=='preflight':result=plan(before['operationRows'],p,DESCRIPTOR,environment,write_authorized=True)
    else:
        require(args.after,'actual readonly after rows required');after=json.loads(args.after.read_text(encoding='utf-8-sig'));require(after['environment']==environment,'actual environment drift')
        expected=plan(before['operationRows'],p,DESCRIPTOR,environment,write_authorized=True)['insertCount']
        result=postflight(before['operationRows'],after['operationRows'],before['qualityVersionRows'],after['qualityVersionRows'],p,DESCRIPTOR,environment,expected_inserts=expected)
    result['inputRawSha256']=hashlib.sha256(args.before.read_bytes()).hexdigest()
    if args.after:result['afterRawSha256']=hashlib.sha256(args.after.read_bytes()).hexdigest()
    args.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');print(result['status'])

if __name__=='__main__':main()
