const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const read = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const migration = read('sql/mysql/20260811_mes_qa_dcc_project_scope.sql').replace(/\r\n/g, '\n')
const regulationDo = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/qa/regulation/MesQaInspectionRegulationDO.java'
)
const processDo = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/qa/regulation/MesQaInspectionRegulationProcessDO.java'
)
const versionDo = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/qa/regulation/MesQaInspectionRegulationVersionDO.java'
)
const itemDo = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/qa/regulation/MesQaInspectionRegulationItemDO.java'
)
const taskDo = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/pqc/MesPqcInspectionTaskDO.java'
)
const taskMapper = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/pqc/MesPqcInspectionTaskMapper.java'
)
const processPoolDo = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/MesProProcessPoolDO.java'
)
const processPoolEventDo = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/MesProProcessPoolEventDO.java'
)
const processPoolPqcRecordDo = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/MesProProcessPoolPqcRecordDO.java'
)

assert.match(regulationDo, /private Long dccProjectCodeId;/)
assert.match(processDo, /@TableName\("mes_qa_inspection_regulation_process"\)/)
assert.match(processDo, /private Long regulationVersionId;/)
assert.match(processDo, /private String processCode;/)
assert.match(processDo, /private String processName;/)
assert.match(processDo, /private Integer sort;/)
assert.match(versionDo, /private LocalDate effectiveDate;/)
assert.match(versionDo, /private String inspectionTypeRulesJson;/)
assert.match(itemDo, /private Long qaProcessId;/)
assert.match(itemDo, /private Integer itemSort;/)
assert.match(itemDo, /private Boolean critical;/)
assert.match(itemDo, /private String failureRule;/)
assert.match(itemDo, /private String sourceNote;/)
assert.match(itemDo, /private Integer sourceOriginalPage;/)
assert.match(itemDo, /private String sourceOriginalItem;/)
assert.match(itemDo, /private String sourceOriginalExcerpt;/)
assert.match(itemDo, /private String sourceOriginalMethod;/)
assert.match(taskDo, /private Long qaProcessId;/)
assert.match(processPoolDo, /private Long qaProcessId;/)
assert.match(processPoolEventDo, /private Long qaProcessId;/)
assert.match(processPoolPqcRecordDo, /private Long qaProcessId;/)

assert.match(migration, /^-- release-migration: .*type=schema; riskLevel=high\n/)
assert.match(migration, /ADD COLUMN `dcc_project_code_id` bigint DEFAULT NULL COMMENT 'DCC项目代码ID'/)
assert.match(migration, /CREATE TABLE IF NOT EXISTS `mes_qa_inspection_regulation_process`/)
assert.match(migration, /UNIQUE KEY `uk_mes_qa_regulation_dcc_project`/)
assert.match(migration, /DROP INDEX `uk_mes_qa_regulation_route_process`/)
assert.match(migration, /DROP INDEX `uk_mes_qa_regulation_code`/)
assert.match(migration, /KEY `idx_mes_qa_regulation_code`/)
assert.match(migration, /ADD COLUMN `qa_process_id` bigint DEFAULT NULL COMMENT 'QA工序ID'/)
assert.match(migration, /ADD COLUMN `item_sort` int DEFAULT NULL COMMENT 'QA工序内项目排序'/)
assert.match(migration, /ADD COLUMN `effective_date` date DEFAULT NULL COMMENT '生效日期'/)
assert.match(migration, /ADD COLUMN `inspection_type_rules_json` longtext COMMENT '检验类型规则JSON'/)
assert.match(migration, /ADD COLUMN `critical` bit\(1\) DEFAULT NULL COMMENT '是否关键检验项目'/)
assert.match(migration, /ADD COLUMN `failure_rule` varchar\(1024\) DEFAULT NULL COMMENT '不合格处理规则'/)
assert.match(migration, /ADD COLUMN `source_original_excerpt` text COMMENT '来源原文摘录'/)
assert.match(migration, /UNIQUE KEY `uk_mes_pqc_task_qa_identity`/)
assert.match(migration, /DROP INDEX `uk_mes_pqc_task_identity`/)
assert.match(migration, /MODIFY COLUMN `route_process_id` bigint NULL/)
assert.match(migration, /MODIFY COLUMN `process_id` bigint NULL/)
assert.match(migration, /uk_mes_pro_process_pool_qa_context/)
assert.match(migration, /uk_mes_pro_process_pool_event_qa_idem/)
assert.match(migration, /PQC QA工序池为空/)
assert.match(
  migration,
  /`tenant_id`, `active_order_id`, `regulation_version_id`, `qa_process_id`, `inspection_type`, `business_date`, `shift_code`, `round_no`, `deleted`/
)
assert.doesNotMatch(
  migration.match(/UNIQUE KEY `uk_mes_pqc_task_qa_identity`[\s\S]*?;/)[0],
  /route_process_id|(?<!qa_)process_id/
)

assert.match(taskMapper, /selectByQaIdentity\(Long activeOrderId, Long regulationVersionId,\s+Long qaProcessId,/)
assert.match(taskMapper, /MesPqcInspectionTaskDO::getQaProcessId/)

console.log('PASS: DCC-owned QA schema and QA-process PQC identity contract')
