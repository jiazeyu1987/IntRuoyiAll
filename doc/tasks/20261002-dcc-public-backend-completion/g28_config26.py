"""Own-task exact26 audit preparation only; imports G22 pure renders without its old writer."""
from __future__ import annotations
import copy, datetime, hashlib, importlib.util, json, re, sys
from pathlib import Path
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
OLD=ROOT/'doc/tasks/20261002-dcc-public-browser'
spec=importlib.util.spec_from_file_location('g28_immutable_g22_primitives',OLD/'g22_gxp_configuration.py')
base=importlib.util.module_from_spec(spec);sys.modules[spec.name]=base;spec.loader.exec_module(base)
SCOPE=tuple(base.SCOPE)+('dcc.controlled-file.legacy-name-occupancy.activate',)
POLICY_SHA='661af676e1406e86659806af8be8f46abd17d101d871d3af8d5b3b736873c894'
COVERAGE_SHA='3acca207c875de851a1e645dec083a82311bdccd98523f1d48465e728d9e394d'
UUID='92ca05d0-aec8-11f1-a944-02b4e226a5ef'
def verify_originals():
 for row in json.loads((HERE/'g28-original-g22-fingerprints.json').read_text(encoding='utf-8'))['assets']:
  p=Path(row['path'])
  if base.sha(p)!=row['sha256'] or p.stat().st_size!=row['bytes']:raise ValueError('original G22 provenance drift')
def package():
 verify_originals()
 original=json.loads((OLD/'g22-gxp-25-operation-impact.json').read_text(encoding='utf-8'))
 parser=base.formal_parser();version,allops=parser.parse_policy(base.POLICY)
 if base.sha(base.POLICY)!=POLICY_SHA or len(allops)!=34:raise ValueError('accepted authoritative candidate drift')
 annotations=parser.discover_annotations(base.BACKEND);report=parser.canonical_report(version,allops,annotations)
 if len(annotations)!=12 or hashlib.sha256(report.encode()).hexdigest()!=COVERAGE_SHA:raise ValueError('formal source coverage drift')
 dcc=[o for o in allops if o.operation_id.startswith('dcc.')]
 if len(dcc)!=27 or {o.operation_id for o in dcc}!={*SCOPE,base.PUBLISH}:raise ValueError('exact26 plus preserved publish scope required')
 operations=[]
 for op in dcc:
  if op.operation_id==base.PUBLISH:continue
  if parser.REQUIRED_FIELDS-op.values.keys() or not parser.source_locator_exists(base.BACKEND,op):raise ValueError('formal operation source invalid')
  values={'policyVersion':version,**op.values,'sourceLocatorVerified':True}
  for column,key in base.FIELDS.items():
   if type(values[key]) is not str or not values[key] or len(values[key])>base.LENGTHS[column]:raise ValueError('exact declared field invalid')
  operations.append(values)
 operations.sort(key=lambda x:x['operationId'])
 oldmap={x['operationId']:x for x in original['operations']}
 if any(all(x[k]==oldmap[x['operationId']][k] for k in base.FIELDS.values()) is not True for x in operations if x['operationId'] in oldmap):raise ValueError('old25 operation payload drift')
 p=copy.deepcopy(original);p.update(status='PREPARED_EXACT26_NO_EXECUTOR_QUALITY_UNAPPROVED',executionAuthorized=False,candidateDccOperationCount=27,operations=operations,exactOperationPayloadHash=base.payload_hash(operations))
 p['sources']['policy']['sha256']=POLICY_SHA;p['sources']['formalParser']['sha256']=base.sha(base.BACKEND/'script/gxp_audit_coverage_gate.py');p['formalCoverage'].update(reportSha256=COVERAGE_SHA,registeredOperationCount=34,discoveredAnnotationCount=12)
 p['qualityRegistration'].update(policyHash=POLICY_SHA,coverageReportHash=COVERAGE_SHA,separateFrom25OperationConfiguration=False,separateFrom26OperationConfiguration=True,executionAuthorized=False,qualityRoleConfirmed=False)
 p['qualityApproval'].update(status='尚未批准',approvedBy=None,approvedAt=None,approvalReference=None,coverageReportHash=None)
 p['impact'].update(firstInsertMaximum=26,temporaryRoutine='dcc_gxp26_tenant1_config',temporaryTable='tmp_dcc_gxp26_tenant1');p['executionPrerequisites'][0]='Exact26 local configuration write approval; old conditional exact25 authorization does not extend to26'
 p['sources']['immutableOriginalG22Impact']={'path':str(OLD/'g22-gxp-25-operation-impact.json'),'sha256':base.sha(OLD/'g22-gxp-25-operation-impact.json')}
 p['sourceServerUuid']=UUID;p['preparedSourceNamespaceDelta']=['dcc.controlled-file.legacy-name-occupancy.activate']
 return p
def approval(package, value, *, write_authorized):
 if write_authorized is not True:raise ValueError('separate exact26/max1 actual write approval required')
 base.validate_approval(package,1,value)
 for field in ['qualityRoleBasis','qualitySignatureEvidenceReference']:
  if type(value.get(field)) is not str or not value[field].strip() or len(value[field])>2000 or re.search('pending|placeholder|尚未|待确认',value[field],re.I):raise ValueError('actual signed quality facts missing')
 role=str(value.get('qualityRoleId',''))
 if not re.fullmatch('[1-9][0-9]*',role) or int(role)>9223372036854775807:raise ValueError('actual registered quality-role ID required')
 if value.get('timeZone')!='Asia/Shanghai':raise ValueError('actual Shanghai approval time required')
 try:datetime.datetime.strptime(value['approvedAt'],'%Y-%m-%d %H:%M:%S')
 except (ValueError,TypeError):raise ValueError('actual valid approval calendar time required') from None
def plan(rows,p,a,*,write_authorized=False):
 approval(p,a,write_authorized=write_authorized)
 if len(p['operations'])!=26 or {x['operationId'] for x in p['operations']}!=set(SCOPE) or base.payload_hash(p['operations'])!=p['exactOperationPayloadHash'] or base.sha(base.POLICY)!=p['sources']['policy']['sha256']:raise ValueError('exact26 payload policy identity conflict')
 for r in rows:
  if type(r.get('tenant_id')) is not int or r['tenant_id']<1:raise ValueError('actual row tenant integer required')
  if r['tenant_id']==1 and str(r.get('operation_id','')).rstrip().casefold() in {base.PUBLISH,*SCOPE}:
   for key in ['active','deleted']:
    if type(r.get(key)) is not int or r[key] not in (0,1):raise ValueError('actual bit rowtypes required')
  if 'operation_id' not in r or type(r['operation_id']) is not str:raise ValueError('actual row operation identity required')
 publishes=[r for r in rows if r.get('tenant_id')==1 and r['operation_id'].rstrip().casefold()==base.PUBLISH]
 if len(publishes)!=1 or any(publishes[0].get(k)!=v for k,v in {'operation_id':base.PUBLISH,'policy_version':'2026-09-approved-01','source_locator':p['preservedPublish']['observedSourceLocator'],'active':1,'deleted':0,'applicability':'GXP'}.items()):raise ValueError('preserved publish baseline conflict')
 inserts=[]
 for o in p['operations']:
  exact={'tenant_id':1,**{db:o[key] for db,key in base.FIELDS.items()},'active':1,'deleted':0}
  found=[r for r in rows if r.get('tenant_id')==1 and r['operation_id'].rstrip().casefold()==o['operationId']]
  if len(found)>1 or found and any(type(found[0].get(k)) is not type(v) or found[0].get(k)!=v for k,v in exact.items()):raise ValueError('whole batch existing row payload/version/state conflict')
  if not found:inserts.append(exact)
 return {'status':'VALIDATED_OFFLINE_ONLY_NOT_SQL_EXECUTION','insertRows':inserts,'insertCount':len(inserts),'updateCount':0,'deleteCount':0,'executionPerformed':False}
def quality_plan(rows,p,a,*,write_authorized=False):
 approval(p,a,write_authorized=write_authorized)
 return base.quality_registration_plan(rows,p,a,quality_role_confirmed=True,write_authorized=write_authorized)
def postflight(before_operations,after_operations,before_versions,after_versions,p,a,*,expected_operation_inserts,expected_version_inserts):
 if type(expected_operation_inserts) is not int or not 0<=expected_operation_inserts<=26 or type(expected_version_inserts) is not int or expected_version_inserts not in (0,1):raise ValueError('explicit actual count bounds required')
 def unchanged(before,after,label):
  ids=[x.get('id') for x in after]
  if any(type(x) is not int or x<1 for x in ids) or len(ids)!=len(set(ids)):raise ValueError('actual '+label+' primary keys invalid')
  byid={r['id']:r for r in after}
  if any(type(r.get('id')) is not int or r['id'] not in byid or byid[r['id']]!=r for r in before):raise ValueError('original '+label+' row/metadata changed')
  beforeids={r['id'] for r in before};return [r for r in after if r['id'] not in beforeids]
 opnew=unchanged(before_operations,after_operations,'operation');vnew=unchanged(before_versions,after_versions,'quality version')
 if len(opnew)!=expected_operation_inserts or len(vnew)!=expected_version_inserts:raise ValueError('actual insert counts differ')
 if any(r.get('tenant_id')!=1 or r.get('operation_id') not in SCOPE for r in opnew):raise ValueError('out of exact26 insert scope')
 expected_version={'tenant_id':1,'policy_version':p['policyVersion'],'policy_hash':p['sources']['policy']['sha256'],'approved_by':int(a['approvedBy']),'approved_at':a['approvedAt'],'approval_reference':a['approvalReference'],'coverage_report_hash':p['formalCoverage']['reportSha256']}
 if any(any(type(r.get(k)) is not type(v) or r.get(k)!=v for k,v in expected_version.items()) for r in vnew):raise ValueError('out of exact1 quality-version insert scope')
 if plan(after_operations,p,a,write_authorized=True)['insertCount']!=0:raise ValueError('postflight exact26 missing payload')
 quality=quality_plan(after_versions,p,a,write_authorized=True)
 if quality['insertRows']:raise ValueError('postflight actual exact quality record missing')
 return {'status':'VALIDATED_READONLY_ROWS_NOT_DATABASE_EXECUTION','operationInserts':len(opnew),'qualityVersionInserts':len(vnew),'originalRowsUnchanged':True}
def _adapt(text):
 text=text.replace('exact25','exact26').replace('a25-operation','a26-operation').replace('outside25','outside26').replace('not25','not26').replace('scope not25','scope not26').replace('dcc_gxp25','dcc_gxp26').replace('tmp_dcc_gxp25','tmp_dcc_gxp26').replace('dcc:gxp25','dcc:gxp26').replace('dcc_gxp1_tenant1_quality','dcc_gxp26_tenant1_quality').replace('@dcc_gxp_','@dcc_gxp26_')
 return re.sub(r'([<>]=?|<>)25(?![0-9])',lambda m:m[1]+'26',text)
def _environment_guards():
 return f"""  IF @@server_uuid<>{base.quote(UUID)} OR FIND_IN_SET('STRICT_TRANS_TABLES',@@session.sql_mode)=0 AND FIND_IN_SET('STRICT_ALL_TABLES',@@session.sql_mode)=0
    OR @@character_set_connection<>'utf8mb4' OR @@collation_connection<>'utf8mb4_unicode_ci' OR @@session.time_zone<>'+08:00' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fresh strict UTF8 Shanghai exact-source session required';
  END IF;
  IF @dcc_gxp26_actual_quality_role_confirmed IS NULL OR @dcc_gxp26_actual_quality_role_confirmed<>1
    OR @dcc_gxp26_quality_signature_evidence IS NULL OR TRIM(@dcc_gxp26_quality_signature_evidence)=''
    OR CHAR_LENGTH(@dcc_gxp26_quality_signature_evidence)>2000
    OR LOWER(@dcc_gxp26_quality_signature_evidence) LIKE '%pending%' OR LOWER(@dcc_gxp26_quality_signature_evidence) LIKE '%placeholder%'
    OR @dcc_gxp26_quality_signature_evidence LIKE '%尚未%' OR @dcc_gxp26_quality_signature_evidence LIKE '%待确认%'
    OR @dcc_gxp26_quality_role_basis IS NULL OR TRIM(@dcc_gxp26_quality_role_basis)='' OR CHAR_LENGTH(@dcc_gxp26_quality_role_basis)>2000
    OR LOWER(@dcc_gxp26_quality_role_basis) LIKE '%pending%' OR LOWER(@dcc_gxp26_quality_role_basis) LIKE '%placeholder%'
    OR @dcc_gxp26_quality_role_basis LIKE '%尚未%' OR @dcc_gxp26_quality_role_basis LIKE '%待确认%'
    OR @dcc_gxp26_quality_role_id IS NULL OR @dcc_gxp26_quality_role_id<1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='actual signed quality-role facts required, not default account';
  END IF;
"""
def _quality_actor_guard():
 return """  SELECT u.id FROM system_users u JOIN system_user_role ur ON ur.user_id=u.id AND ur.tenant_id=u.tenant_id AND ur.deleted=b'0'
    JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=u.tenant_id AND r.deleted=b'0' AND r.status=0
    WHERE u.id=quality_approver AND u.tenant_id=1 AND u.status=0 AND u.deleted=b'0' AND r.id=@dcc_gxp26_quality_role_id FOR UPDATE;
  IF (SELECT COUNT(*) FROM system_users u JOIN system_user_role ur ON ur.user_id=u.id AND ur.tenant_id=u.tenant_id AND ur.deleted=b'0'
      JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=u.tenant_id AND r.deleted=b'0' AND r.status=0
      WHERE u.id=quality_approver AND u.tenant_id=1 AND u.status=0 AND u.deleted=b'0' AND r.id=@dcc_gxp26_quality_role_id)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='actual enabled tenant1 quality account and registered role required';
  END IF;
"""
def operation_sql(p):
 oldscope=base.SCOPE
 try:base.SCOPE=SCOPE;text=base.render_sql(p)
 finally:base.SCOPE=oldscope
 text=_adapt(text);text=text.replace('  SELECT GET_LOCK(',_environment_guards()+'  SELECT GET_LOCK(',1)
 text=text.replace("quality_approver IS NULL OR quality_approver<1 THEN","quality_approver IS NULL OR quality_approver<1 OR @dcc_gxp26_quality_approved_at IS NULL THEN")
 text=text.replace('AND approved_at IS NOT NULL','AND approved_at <=> @dcc_gxp26_quality_approved_at')
 text=text.replace('  START TRANSACTION;','  START TRANSACTION;\n'+_quality_actor_guard(),1)
 return text
def quality_sql(p):
 text=_adapt(base.render_quality_registration_sql(p));text=text.replace('  SELECT GET_LOCK(',_environment_guards()+'  SELECT GET_LOCK(',1);text=text.replace('  START TRANSACTION;','  START TRANSACTION;\n'+_quality_actor_guard(),1);return text
def _readonly(p):
 target=','.join(base.quote(x) for x in sorted(SCOPE));return f"""-- READ ONLY preparation; execute only Root fresh source verification, never business mutations.
SELECT DATABASE() AS actual_database,@@server_uuid AS actual_uuid,@@version AS mysql_version,@@session.sql_mode AS sql_mode,@@session.time_zone AS time_zone,@@character_set_connection AS connection_charset,@@collation_connection AS connection_collation;
SELECT TABLE_NAME,ENGINE,TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version','system_users','system_role','system_user_role');
SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT,EXTRA,COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version','system_users','system_role','system_user_role') ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT TABLE_NAME,INDEX_NAME,NON_UNIQUE,SEQ_IN_INDEX,COLUMN_NAME,SUB_PART FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version') ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX;
SELECT * FROM gxp_audit_policy_operation ORDER BY tenant_id,operation_id,policy_version,id;
SELECT * FROM gxp_audit_policy_version ORDER BY tenant_id,policy_version,id;
SELECT u.id,u.tenant_id,u.username,u.nickname,u.status,HEX(u.deleted) AS deleted_hex,r.id AS role_id,r.code AS role_code,r.status AS role_status,HEX(r.deleted) AS role_deleted FROM system_users u LEFT JOIN system_user_role ur ON ur.user_id=u.id AND ur.tenant_id=u.tenant_id AND ur.deleted=b'0' LEFT JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=u.tenant_id WHERE u.tenant_id=1 AND u.id=@dcc_gxp26_quality_approved_by;
SELECT ROUTINE_NAME,ROUTINE_TYPE,DEFINER,CREATED,LAST_ALTERED,SQL_MODE,ROUTINE_DEFINITION FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME IN ('dcc_gxp26_tenant1_config','dcc_gxp26_tenant1_quality');
-- Root compares every preserved row/column/metadata against frozen preflight; no query returns secrets.
SELECT COUNT(*) AS exact26_namespace_rows FROM gxp_audit_policy_operation WHERE tenant_id=1 AND operation_id IN ({target});
"""
def main():
 p=package();verify_originals()
 (HERE/'g28-config26-impact.json').write_text(json.dumps(p,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 (HERE/'g28-config26-operation.review.sql').write_text(operation_sql(p),encoding='utf-8')
 (HERE/'g28-config26-quality-version.review.sql').write_text(quality_sql(p),encoding='utf-8')
 (HERE/'g28-config26-readonly-preflight.sql').write_text(_readonly(p),encoding='utf-8');(HERE/'g28-config26-readonly-postflight.sql').write_text(_readonly(p),encoding='utf-8')
 empty={'scope':'Local tenant1 exact26 candidate only; not approved','tenantId':1,'policyVersion':p['policyVersion'],'policyHash':POLICY_SHA,'coverageReportHash':COVERAGE_SHA,'actualQualityApproval':{'approved':None,'approverName':None,'qualityRoleBasis':None,'qualityRoleId':None,'approvedBy':None,'approvedAt':None,'timeZone':'Asia/Shanghai','approvalReference':None,'qualitySignatureEvidenceReference':None},'explicitLocalWriteAuthorization':{'registerMaximumOneNewVersionRow':False,'registerExact26OperationRows':False}}
 (HERE/'g28-config26-quality-empty-template.json').write_text(json.dumps(empty,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 contract={'status':'BLOCKED_ACTUAL_QA_AND_EXACT26_PERMISSION_MISSING','actualDatabaseExecutor':False,'candidatePolicyHash':POLICY_SHA,'coverageHash':COVERAGE_SHA,'requiredScopeCount':26,'maximumQualityVersionInsert':1,'separateAuthorizationFlags':['@dcc_gxp26_tenant1_write_authorized','@dcc_gxp26_quality_version_write_authorized'],'requiredQualityInputs':list(empty['actualQualityApproval']),'postflightMethod':'Validate actual rows through pure plan with real approved input; assert insertCount==0 and all baseline non-target/preexisting rows unchanged; old publish09 fullrowhash unchanged','mysqlFirstRepeatExecuted':False,'mysqlMode':'Fresh session SERIALIZABLE, strict mode, UTF8 unicode_ci exact sourceUUID, +08:00; routine collisions fail no preDROP/force; owner-specific cleanup after actual evidence only','originalG22Preserved':True}
 (HERE/'g28-config26-input-contract.json').write_text(json.dumps(contract,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 verify_originals();print('PREPARED exact26/max1 only; no DB executor or actualQA; original G22 immutable')
if __name__=='__main__':main()
