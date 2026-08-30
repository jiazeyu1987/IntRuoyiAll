const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const moduleRoot = path.resolve(__dirname, '../../..')
const backendRoot = path.resolve(__dirname, '../../../..')
const servicePath = path.join(
  moduleRoot,
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImpl.java'
)
const reportDoPath = path.join(
  moduleRoot,
  'src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecordreport/MesProBatchRecordReportDO.java'
)
const respVoPath = path.join(
  moduleRoot,
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecordreport/vo/BatchRecordReportRespVO.java'
)
const viewPath = path.join(
  moduleRoot,
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportView.java'
)
const migrationPath = path.join(
  backendRoot,
  'sql/mysql/20260830_mes_batch_record_report_project_code.sql'
)
const h2SchemaPath = path.join(moduleRoot, 'src/test/resources/sql/create_tables.sql')

const service = fs.readFileSync(servicePath, 'utf8')
const reportDo = fs.readFileSync(reportDoPath, 'utf8')
const respVo = fs.readFileSync(respVoPath, 'utf8')
const view = fs.readFileSync(viewPath, 'utf8')
const h2Schema = fs.readFileSync(h2SchemaPath, 'utf8')

assert.match(reportDo, /private\s+String\s+projectCode;/, '报表元数据 DO 必须持久化 DCC 项目代码。')
assert.match(respVo, /@Schema\(description = "DCC 项目代码"\)[\s\S]{0,100}private\s+String\s+projectCode;/, '分页返回 VO 必须暴露 DCC 项目代码。')
assert.match(view, /String\s+productName,\s*\r?\n\s*String\s+projectCode,/, '报表列表视图必须携带 DCC 项目代码。')

assert.match(
  service,
  /String\s+normalizedProjectCode\s*=\s*requireReportProjectCode\(selectedDccProjectCode\);/,
  '导入主批记录时必须从选中的 DCC 项目代码记录读取正式 projectCode。'
)
assert.match(
  service,
  /saveGeneratedReports\([\s\S]{0,900}resolveReportProductName\(normalizedProductNames\),\s*normalizedProjectCode\)/,
  '保存主批记录报表元数据时必须把 DCC 项目代码传入保存链路。'
)
assert.match(service, /\.setProjectCode\(normalizedProjectCode\)/, '保存报表元数据时必须写入 projectCode。')
assert.match(service, /\.projectCode\(normalizedProjectCode\)/, '导入返回的报表视图必须带出 projectCode。')
assert.match(
  service,
  /toReportViewFromMetadata[\s\S]{0,700}\.projectCode\(metadata\.getProjectCode\(\)\)/,
  '从报表元数据构建视图时必须带出 projectCode。'
)
assert.match(
  service,
  /toVisibleReportView[\s\S]{0,900}\.projectCode\(metadata\.getProjectCode\(\)\)/,
  '分页列表必须从报表元数据带出 projectCode。'
)
assert.match(
  service,
  /copyReportWithVersionProduct[\s\S]{0,700}\.projectCode\(report\.projectCode\(\)\)/,
  '按版本产品展开列表时必须保留原始 projectCode。'
)
assert.doesNotMatch(
  service,
  /formBindings[\s\S]{0,180}projectCode|projectCode[\s\S]{0,180}formBindings/i,
  'DCC 项目代码不得从表单槽位 formBindings 推断。'
)

assert.ok(fs.existsSync(migrationPath), '必须新增 mes_pro_batch_record_report.project_code 结构迁移。')
const migration = fs.readFileSync(migrationPath, 'utf8')
assert.match(migration, /release-migration:[^\r\n]*type=schema/, '结构迁移必须声明 release-migration 元数据。')
assert.match(migration, /information_schema\.COLUMNS/, '结构迁移必须用 information_schema 做幂等列检查。')
assert.match(
  migration,
  /ADD COLUMN `project_code` varchar\(64\) DEFAULT NULL COMMENT 'DCC项目代码' AFTER `product_name`/,
  '结构迁移必须在 mes_pro_batch_record_report.product_name 后新增 project_code。'
)
assert.doesNotMatch(
  migration,
  /\bUPDATE\b\s+`?mes_pro_batch_record_report`?/i,
  '迁移不得猜测回填旧报表的 DCC 项目代码。'
)
assert.match(
  h2Schema,
  /"project_code"\s+varchar\(64\) DEFAULT NULL/,
  '单元测试 H2 表结构必须包含 project_code。'
)

console.log('PASS: batch-record report project code static contract')
