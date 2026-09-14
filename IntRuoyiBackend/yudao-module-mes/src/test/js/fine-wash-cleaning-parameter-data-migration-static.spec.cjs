const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../../..')
const backendRoot = path.resolve(moduleRoot, '..')
const migrationPath = path.join(
  backendRoot,
  'sql/mysql/20260811_mes_process_pool_cleaning_wash_parameter_data.sql'
)

assert.ok(
  fs.existsSync(migrationPath),
  '必须提供正式 SQL 迁移，修正已有精洗/粗洗清洗参数旧 TEXT_STANDARD 数据。'
)

const sql = fs.readFileSync(migrationPath, 'utf8').replace(/\r\n/g, '\n')

assert.match(
  sql,
  /dependsOn=20260810_mes_process_pool_device_parameter_select_options/,
  '迁移必须依赖下拉选项字段迁移。'
)
assert.match(
  sql,
  /SIGNAL SQLSTATE '45000'/,
  '迁移必须在前置条件或唯一键冲突时 fail fast。'
)
assert.match(
  sql,
  /JOIN `mes_pro_process`/,
  '迁移必须按正式工序表定位粗洗/精洗。'
)
assert.match(
  sql,
  /JOIN `mes_pro_process_pool_team_device`/,
  '迁移必须按正式设备表限定超声波清洗机。'
)
assert.match(sql, /精洗/, '迁移必须覆盖精洗工序。')
assert.match(sql, /粗洗/, '迁移必须保留粗洗一致口径。')
assert.match(sql, /超声波清洗机/, '迁移必须只限定超声波清洗机固定参数。')

assert.match(
  sql,
  /`parameter_name`\s*=\s*'清洗介质'[\s\S]*`value_type`\s*=\s*'SELECT'[\s\S]*JSON_ARRAY\('自来水',\s*'纯化水'\)/,
  '清洗介质必须迁移为自来水/纯化水 SELECT 下拉选项。'
)
assert.match(
  sql,
  /`default_text`\s*=\s*CASE[\s\S]*process\.`name` LIKE '%精洗%' THEN '纯化水'[\s\S]*ELSE '自来水'[\s\S]*END/,
  '精洗必须默认纯化水，粗洗必须默认自来水。'
)
assert.doesNotMatch(sql, /纯净水/, '正式清洗介质迁移不得继续保留纯净水。')
const roomTemperatureUpdateStart = sql.indexOf("rule.`parameter_name` = '室温'")
assert.ok(roomTemperatureUpdateStart >= 0, '迁移必须包含室温更新语句。')
const roomTemperatureUpdateEnd = sql.indexOf(';', roomTemperatureUpdateStart)
assert.ok(roomTemperatureUpdateEnd > roomTemperatureUpdateStart, '室温更新语句必须完整结束。')
const roomTemperatureUpdate = sql.slice(roomTemperatureUpdateStart, roomTemperatureUpdateEnd)
for (const [assignment, description] of [
  ["rule.`value_type` = 'DECIMAL'", 'DECIMAL 数字类型'],
  ['rule.`lower_limit` = 20', '下限 20'],
  ['rule.`default_value` = 26', '默认 26'],
  ['rule.`upper_limit` = 30', '上限 30'],
  ['rule.`decimal_scale` = 1', '小数位 1']
]) {
  assert.ok(roomTemperatureUpdate.includes(assignment), `室温迁移必须写入${description}。`)
}
assert.match(
  sql,
  /'FINE_WASH_MEDIUM'[\s\S]*'FINE_WASH_ROOM_TEMPERATURE'/,
  '精洗必须写入 FINE_WASH_* 正式参数编码。'
)
assert.match(
  sql,
  /'ROUGH_WASH_MEDIUM'[\s\S]*'ROUGH_WASH_ROOM_TEMPERATURE'/,
  '粗洗必须保留 ROUGH_WASH_* 正式参数编码。'
)
assert.match(
  sql,
  /START TRANSACTION;[\s\S]*UPDATE `mes_pro_process_pool_device_parameter_rule`[\s\S]*UPDATE `mes_pro_process_pool_device_parameter_rule`[\s\S]*COMMIT;/,
  '清洗介质和室温两条数据更新必须在同一事务中提交。'
)
assert.doesNotMatch(sql, /\bDELETE\b|\bTRUNCATE\b/i, '迁移不得删除已有参数规则。')

console.log('fine-wash-cleaning-parameter-data-migration-static PASS')
