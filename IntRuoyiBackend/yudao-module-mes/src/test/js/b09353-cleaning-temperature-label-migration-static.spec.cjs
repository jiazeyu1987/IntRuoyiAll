const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const migrationPath = path.join(
  backendRoot,
  'sql/mysql/20260812_mes_process_pool_b09353_cleaning_temperature_label.sql'
)
assert.ok(fs.existsSync(migrationPath), '必须新增 B09353 清洗温度文案迁移。')

const sql = fs.readFileSync(migrationPath, 'utf8').replace(/\r\n/g, '\n')

assert.match(
  sql,
  /dependsOn=20260812_mes_process_pool_b04091_cleaning_temperature_label/,
  'B09353 文案迁移必须依赖 B04091 同类文案迁移，保持迁移顺序明确。'
)
assert.match(sql, /START TRANSACTION;[\s\S]*COMMIT;/, 'B09353 文案迁移必须在事务中执行。')
assert.match(
  sql,
  /process\.`name` = '清洗工序'[\s\S]*device\.`device_code` = 'B09353'/,
  '迁移范围必须精确限定清洗工序 B09353 设备。'
)
assert.match(
  sql,
  /rule\.`parameter_code` = 'CLEANING_ROOM_TEMPERATURE'[\s\S]*rule\.`parameter_name` = '清洗温度'/,
  'B09353 清洗温度参数名称必须更新为清洗温度。'
)
assert.match(
  sql,
  /REPLACE\(rule\.`standard_text`, '室温', '清洗温度'\)/,
  'B09353 标准描述必须同步从室温改为清洗温度。'
)
assert.match(
  sql,
  /No active B09353 cleaning temperature parameter rule found/,
  '迁移必须在目标规则缺失时 fail fast，禁止零行成功。'
)
assert.doesNotMatch(
  sql,
  /process\.`name` LIKE '%(?:粗洗|精洗)%'/,
  'B09353 文案迁移不得扩大到粗洗或精洗。'
)
assert.doesNotMatch(
  sql,
  /device\.`device_name` LIKE '%B09353%'/,
  'B09353 文案迁移必须使用正式设备编码，不得靠名称模糊匹配。'
)
assert.doesNotMatch(
  sql,
  /OR\s+rule\.`parameter_name`\s+IN/,
  'B09353 文案迁移不得用参数名称兜底扩大范围，必须锁定 CLEANING_ROOM_TEMPERATURE。'
)

console.log('b09353-cleaning-temperature-label-migration-static PASS')
