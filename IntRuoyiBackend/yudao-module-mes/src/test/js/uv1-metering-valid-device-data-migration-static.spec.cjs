const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const migrationPath = path.join(
  backendRoot,
  'sql/mysql/20260811_mes_process_pool_uv1_metering_valid_parameter.sql'
)
const migration = fs.readFileSync(migrationPath, 'utf8').replace(/\r\n/g, '\n')

assert.match(migration, /光固Ⅰ/, '迁移必须精确限定光固Ⅰ。')
assert.doesNotMatch(migration, /光固Ⅱ/, '迁移不得绑定光固Ⅱ。')
assert.match(
  migration,
  /INSERT INTO `mes_pro_process_pool_team_device`/,
  'A05059 缺少正式班组设备时，迁移必须补齐该设备。'
)
assert.match(
  migration,
  /INSERT INTO `mes_pro_process_pool_team_process_device`/,
  '迁移必须把 A05059 绑定到光固Ⅰ正式工序。'
)
assert.match(
  migration,
  /REPLACE\(source_rule\.`parameter_code`,\s*'A05075',\s*'A05059'\)/,
  '迁移必须从同工序 A05075 正式规则映射 A05059 的基础参数编码。'
)
assert.match(
  migration,
  /source_device\.`device_code`\s*=\s*'A05075'/,
  'A05059 的负责人和工序范围必须从正式 A05075 来源设备解析。'
)
assert.match(
  migration,
  /target_device\.`device_code`\s*=\s*'A05059'/,
  '后续绑定和参数必须通过正式 A05059 设备身份定位。'
)
assert.match(
  migration,
  /COUNT\(DISTINCT target_device\.`device_code`\)[\s\S]*<>\s*2/,
  '迁移必须在结束前验证光固Ⅰ两台正式设备均已就绪。'
)
assert.match(migration, /METERING_VALID/, '两台设备都必须新增 METERING_VALID 参数。')
assert.doesNotMatch(migration, /\bDELETE\b|\bTRUNCATE\b/i, '迁移不得删除业务数据。')

console.log('uv1-metering-valid-device-data-migration-static PASS')
