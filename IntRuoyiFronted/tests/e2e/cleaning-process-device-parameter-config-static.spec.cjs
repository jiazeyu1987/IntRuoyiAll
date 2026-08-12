const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const leaderPage = fs
  .readFileSync(
    path.join(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
    'utf8'
  )
  .replace(/\r\n/g, '\n')

assert.match(
  leaderPage,
  /type CleaningWashProcessKind = 'ROUGH_WASH' \| 'FINE_WASH' \| 'CLEANING'/,
  '固定清洗参数类型必须包含清洗工序。'
)
assert.match(
  leaderPage,
  /kind:\s*'CLEANING'[\s\S]*processKeyword:\s*'清洗'[\s\S]*parameterCodePrefix:\s*'CLEANING'[\s\S]*defaultCleaningMedium:\s*'纯化水'[\s\S]*buttonLabel:\s*'清洗参数'/,
  '清洗工序必须注册正式固定参数配置并默认纯化水。'
)
assert.match(
  leaderPage,
  /kind:\s*'CLEANING'[\s\S]*cleaningMediumOptions:\s*\['纯化水', '自来水'\]/,
  '清洗工序介质选项必须按纯化水、自来水配置。'
)
assert.match(
  leaderPage,
  /CLEANING:\s*CLEANING_DEVICE_PARAMETER_TEMPLATES/,
  '清洗工序必须生成独立 CLEANING 参数模板。'
)
assert.match(
  leaderPage,
  /parameterCode:\s*parameterCodePrefix \+ '_ROOM_TEMPERATURE'[\s\S]*parameterName:\s*'室温'[\s\S]*valueType:\s*'DECIMAL'[\s\S]*targetValue:\s*26/,
  '清洗温度必须沿用精洗数字参数并默认 26。'
)
for (const parameterCode of [
  'CLEANING_COUNT',
  'CLEANING_MEDIUM',
  'CLEANING_POWER',
  'CLEANING_ROOM_TEMPERATURE',
  'CLEANING_TIME'
]) {
  assert.ok(
    leaderPage.includes(`case '${parameterCode}':`),
    `清洗固定参数保存载荷必须支持 ${parameterCode}。`
  )
}
assert.match(
  leaderPage,
  /case 'CLEANING_MEDIUM':[\s\S]*optionValues:\s*template\.optionValues \? \[\.\.\.template\.optionValues\] : undefined[\s\S]*defaultText:\s*roughWashParameterForm\.cleaningMedium/,
  '清洗介质保存时必须保留该工序正式选项顺序和默认值。'
)
assert.match(
  leaderPage,
  /resolveCleaningWashProcessConfig[\s\S]*processText\.includes\(config\.processKeyword\)/,
  '清洗工序入口必须按正式工序映射解析，不得按设备编码合成。'
)

console.log('cleaning-process-device-parameter-config-static PASS')
