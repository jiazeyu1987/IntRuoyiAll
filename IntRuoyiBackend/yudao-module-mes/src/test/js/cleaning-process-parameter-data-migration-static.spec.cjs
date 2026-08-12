const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const migrationPath = path.join(
  backendRoot,
  'sql/mysql/20260811_mes_process_pool_cleaning_process_parameter_data.sql'
)
assert.ok(fs.existsSync(migrationPath), '必须新增清洗工序正式参数数据迁移。')

const sql = fs.readFileSync(migrationPath, 'utf8').replace(/\r\n/g, '\n')

assert.match(
  sql,
  /dependsOn=20260811_mes_process_pool_cleaning_wash_parameter_data/,
  '清洗工序迁移必须依赖既有粗洗/精洗参数迁移。'
)
assert.match(sql, /START TRANSACTION;[\s\S]*COMMIT;/, '五条关联参数迁移必须位于同一事务。')
assert.match(
  sql,
  /process\.`name` = '清洗工序'/,
  '迁移范围必须精确限定正式清洗工序，不能命中粗洗或精洗。'
)
assert.match(
  sql,
  /'CLEANING_COUNT'[\s\S]*'CLEANING_MEDIUM'[\s\S]*'CLEANING_POWER'[\s\S]*'CLEANING_ROOM_TEMPERATURE'[\s\S]*'CLEANING_TIME'/,
  '清洗工序五条既有规则必须规范为独立 CLEANING 参数身份。'
)
assert.match(
  sql,
  /rule\.parameter_code = 'CLEANING_MEDIUM'[\s\S]*rule\.value_type = 'SELECT'[\s\S]*JSON_ARRAY\('纯化水',\s*'自来水'\)[\s\S]*rule\.default_text = '纯化水'/,
  '清洗介质必须迁移为默认纯化水的纯化水/自来水下拉参数。'
)
assert.match(
  sql,
  /rule\.parameter_code = 'CLEANING_ROOM_TEMPERATURE'[\s\S]*rule\.parameter_name = '室温'[\s\S]*rule\.lower_limit = 20[\s\S]*rule\.default_value = 26[\s\S]*rule\.upper_limit = 30[\s\S]*rule\.value_type = 'DECIMAL'[\s\S]*rule\.decimal_scale = 1/,
  '清洗温度必须迁移为 20-30℃、默认 26、1 位小数的数值参数。'
)
assert.match(
  sql,
  /Duplicate cleaning process parameter rules would collide after normalization/,
  '迁移必须在正式参数身份冲突时 fail fast。'
)
assert.match(
  sql,
  /No active cleaning process ultrasonic cleaner parameter rules found/,
  '迁移必须在目标设备参数不存在时 fail fast，禁止零行成功。'
)
assert.doesNotMatch(
  sql,
  /process\.`name` LIKE '%(?:粗洗|精洗)%'/,
  '清洗工序迁移不得扩大到粗洗或精洗。'
)

console.log('cleaning-process-parameter-data-migration-static PASS')
