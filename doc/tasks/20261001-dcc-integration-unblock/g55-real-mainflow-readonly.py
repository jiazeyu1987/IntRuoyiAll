"""Root-only read receipt for already completed frontend business actions. No writes."""
from pathlib import Path
import json
from datetime import datetime, timezone
from g21_mysql_support import LocalMysql, sha256_file

ROOT = Path(__file__).resolve().parents[3]
TASK = Path(__file__).resolve().parent
FILE = '2054545668044084026'
OUT = Path('C:/IntRuoyiBackups/20261005-dcc-four-direction-runtime/g55-readonly-mainflow-receipt-r2.json')
mysql = LocalMysql('ruoyi-vue-pro')

def read_rows(sql, names):
    rows = []
    for line in mysql.read(sql).splitlines():
        values = line.split('\t')
        if len(values) != len(names):
            raise RuntimeError('Unexpected formal read projection shape')
        rows.append(dict(zip(names, (None if value == 'NULL' else value for value in values))))
    return rows

def collect():
    if OUT.exists():
        raise RuntimeError('Refusing to replace existing final receipt')
    result = {'status': 'READ_ONLY_CORROBORATION_OF_REAL_FRONTEND_ACTIONS',
              'collectedAtUtc': datetime.now(timezone.utc).isoformat(),
              'businessWritesByThisScript': 0, 'tenantId': '1', 'actorUserId': '1',
              'multipleDistinctActorsTested': False}
    result['file'] = read_rows(f"SELECT CAST(id AS CHAR),status,version_no,effective_date,controlled_time,activated_time,distributed_time,need_training,product_source,CAST(product_catalog_id AS CHAR),CAST(product_relation_id AS CHAR),CAST(product_create_request_id AS CHAR),CAST(product_master_id AS CHAR),product_code,product_name,CAST(dcc_project_code_id AS CHAR),CAST(category_id AS CHAR),CAST(directory_id AS CHAR),CAST(file_owner_user_id AS CHAR),CAST(file_owner_signature_id AS CHAR),CAST(published_file_id AS CHAR),CAST(stamped_file_id AS CHAR),file_number,source_original_file_name,process_instance_id FROM dcc_controlled_file WHERE id={FILE} AND tenant_id=1;",
                              ['id','status','version','effectiveDate','controlledTime','activatedTime','distributedTime','needTraining','productSource','productCatalogId','productRelationId','productRequestId','productMasterId','productCode','productName','projectId','categoryId','storageDirectoryId','ownerId','ownerSignatureId','publishedFileId','stampedFileId','fileNumber','sourceOriginalFileName','processInstanceId'])
    result['requests'] = read_rows("SELECT CAST(id AS CHAR),status,CAST(previous_request_id AS CHAR),project_name,project_code,product_code,product_name,CAST(project_leader_user_id AS CHAR),CAST(configured_reviewer_user_id AS CHAR),CAST(generated_project_code_id AS CHAR),CAST(generated_product_catalog_id AS CHAR),review_reason,reject_reason FROM dcc_project_product_create_request WHERE id IN (7,8,9,10) AND tenant_id=1 ORDER BY id;",
                                   ['id','status','previousId','projectName','projectCode','productCode','productName','leaderId','configuredReviewerId','generatedProjectId','generatedCatalogId','reviewReason','rejectReason'])
    result['project'] = read_rows("SELECT CAST(id AS CHAR),project_name,project_code,default_attributes_json FROM dcc_project_code WHERE id=271 AND tenant_id=1;", ['id','projectName','projectCode','defaultAttributesJson'])
    result['ownerRules'] = read_rows("SELECT CAST(id AS CHAR),subject_type,CAST(subject_id AS CHAR),access_level,active FROM dcc_project_access_rule WHERE dcc_project_code_id=271 AND tenant_id=1 AND deleted=0;", ['id','subjectType','subjectId','accessLevel','active'])
    result['mapping'] = read_rows("SELECT CAST(id AS CHAR),CAST(project_code_id AS CHAR),CAST(project_folder_id AS CHAR),CAST(category_id AS CHAR),CAST(base_directory_id AS CHAR),CAST(storage_directory_id AS CHAR) FROM dcc_project_folder_storage_mapping WHERE project_code_id=271 AND tenant_id=1 AND deleted=0;", ['id','projectId','folderId','categoryId','baseDirectoryId','storageDirectoryId'])
    result['placement'] = read_rows(f"SELECT CAST(id AS CHAR),CAST(project_code_id AS CHAR),CAST(project_folder_id AS CHAR),CAST(controlled_file_id AS CHAR),CAST(storage_directory_id AS CHAR) FROM dcc_project_file_placement WHERE controlled_file_id={FILE} AND tenant_id=1 AND deleted=0;", ['id','projectId','folderId','controlledFileId','storageDirectoryId'])
    result['attributes'] = read_rows(f"SELECT application_type,CAST(application_id AS CHAR),application_round,default_source_json,actual_attributes_json,submitted FROM dcc_project_application_attributes WHERE application_id={FILE} AND tenant_id=1 AND deleted=0;", ['applicationType','applicationId','round','defaultSourceJson','actualJson','submitted'])
    result['signatures'] = read_rows(f"SELECT CAST(id AS CHAR),action_type,CAST(actor_id AS CHAR),process_instance_id,version_no,password_verified,evidence_status,signed_at FROM dcc_controlled_file_signature WHERE controlled_file_id={FILE} AND deleted=0 ORDER BY id;", ['id','action','actorId','processInstanceId','version','passwordVerified','evidenceStatus','signedAt'])
    result['distribution'] = read_rows(f"SELECT CAST(id AS CHAR),CAST(controlled_file_id AS CHAR),CAST(department_id AS CHAR),distribution_medium,status FROM dcc_controlled_file_distribution WHERE controlled_file_id={FILE} AND deleted=0;", ['id','controlledFileId','departmentId','medium','status'])
    result['recipients'] = read_rows(f"SELECT CAST(r.id AS CHAR),CAST(r.distribution_id AS CHAR),CAST(r.user_id AS CHAR),CAST(r.message_job_id AS CHAR),r.read_at,r.acknowledged_at FROM dcc_controlled_file_distribution_recipient r JOIN dcc_controlled_file_distribution d ON d.id=r.distribution_id WHERE d.controlled_file_id={FILE} AND r.deleted=0;", ['id','distributionId','userId','messageJobId','readAt','acknowledgedAt'])
    result['projectMessages'] = read_rows("SELECT CAST(id AS CHAR),business_key,CAST(user_id AS CHAR),JSON_UNQUOTE(JSON_EXTRACT(template_params,'$.notifyTargetType')),JSON_UNQUOTE(JSON_EXTRACT(template_params,'$.notifyTargetId')),JSON_UNQUOTE(JSON_EXTRACT(template_params,'$.actionUrl')),CAST(read_status AS UNSIGNED) FROM system_notify_message WHERE tenant_id=1 AND business_key REGEXP '^DCC_PROJECT_PRODUCT:1:(7|8|9|10):' ORDER BY id;", ['id','businessKey','userId','targetType','targetId','actionUrl','read'])
    if len(result['file']) != 1 or len(result['mapping']) != 1 or len(result['placement']) != 1:
        raise RuntimeError('Expected one exact task-owned file, mapping and placement')
    file = result['file'][0]
    if (file['status'],file['version'],file['effectiveDate'],file['productSource'],file['projectId'],file['productCatalogId'],file['productMasterId']) != ('ACTIVE','A/1','2026-10-05','DCC_CATALOG','271','614',None):
        raise RuntimeError('Exact file source or lifecycle differs from real frontend result')
    if not file['distributedTime'] or not file['controlledTime'] or not file['activatedTime']:
        raise RuntimeError('Main lifecycle final facts incomplete')
    if result['mapping'][0]['storageDirectoryId'] != file['storageDirectoryId'] or result['placement'][0]['storageDirectoryId'] != file['storageDirectoryId']:
        raise RuntimeError('Formal logical folder/storage projection mismatch')
    if [row['action'] for row in result['signatures']] != ['ASSIGN','APPROVE','ASSIGN','APPROVE','APPROVE','APPROVE'] or any(row['passwordVerified'] != '1' for row in result['signatures']):
        raise RuntimeError('Six genuine signature actions not corroborated')
    for message in result['projectMessages']:
        if message['targetType'] != 'DCC_PROJECT_PRODUCT_REQUEST' or message['targetId'] != message['businessKey'].split(':')[2]:
            raise RuntimeError('Native request notification exact identity mismatch')
    result['readProjectionCorrection'] = 'Initial receipt used obsolete navigationTargetType/Id diagnostic names; JSON_KEYS formally confirmed notifyTargetType/Id. Initial private receipt retained, no business writes.'
    ui_root = OUT.parent
    steps = ['g53-real-ui-r2/024.json','g53-real-ui-r2/026.json','g53-real-ui-r3/003.json','g53-real-ui-r3/005.json','g53-real-ui-r3/052.json','g53-real-ui-r3/057.json','g53-real-ui-r3/062.json','g53-real-ui-r3/089.json','g53-real-ui-r4/050.json','g53-real-ui-r4/064.json','g53-real-ui-r4/066.json','g53-real-ui-r4/069.json','g53-real-ui-r4/076.json']
    result['visibleUiEvidence'] = [{'path':str(ui_root / step),'sha256':sha256_file(ui_root / step)} for step in steps]
    result['scopeLimitations'] = ['Admin acted in multiple configured roles; distinct accounts are not proved.', 'This branch did not select training.', 'Future activation, revision and obsolete lifecycle were not re-run in this four-direction acceptance.', 'Distribution completed; recipient acknowledgement is a separate pending action.']
    OUT.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'status':result['status'],'path':str(OUT),'sha256':sha256_file(OUT),'fileId':FILE,'signatureCount':len(result['signatures']),'distributedTime':file['distributedTime']},ensure_ascii=False))

if __name__ == '__main__':
    collect()
