const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const leaderPage = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

assert.match(
  leaderPage,
  /kind:\s*'FINE_WASH'[\s\S]*processKeyword:\s*'精洗'[\s\S]*parameterCodePrefix:\s*'FINE_WASH'[\s\S]*defaultCleaningMedium:\s*'纯化水'[\s\S]*buttonLabel:\s*'精洗参数'/,
  '精洗必须注册为固定清洗参数配置类型并默认纯化水。'
)
assert.match(
  leaderPage,
  /resolveCleaningWashProcessConfig[\s\S]*deviceText\.includes\('超声波清洗机'\)[\s\S]*processText\.includes\(config\.processKeyword\)/,
  '精洗超声波清洗机必须进入固定清洗参数专用配置入口。'
)
assert.match(
  leaderPage,
  /buttonLabel:\s*'精洗参数'/,
  '精洗超声波清洗机操作按钮必须显示精洗参数。'
)
assert.match(
  leaderPage,
  /FINE_WASH:\s*FINE_WASH_DEVICE_PARAMETER_TEMPLATES/,
  '精洗必须使用 FINE_WASH 前缀生成自己的正式参数编码。'
)
assert.match(
  leaderPage,
  /kind:\s*'FINE_WASH'[\s\S]*cleaningMediumOptions:\s*\['自来水', '纯化水'\][\s\S]*parameterCode:\s*parameterCodePrefix \+ '_MEDIUM'[\s\S]*parameterName:\s*'清洗介质'[\s\S]*valueType:\s*'SELECT'[\s\S]*optionValues:\s*\[\.\.\.cleaningMediumOptions\][\s\S]*defaultText:\s*defaultCleaningMedium/,
  '精洗清洗介质必须使用与粗洗一致的下拉选项。'
)
assert.match(
  leaderPage,
  /parameterCode:\s*parameterCodePrefix \+ '_ROOM_TEMPERATURE'[\s\S]*parameterName:\s*'室温'[\s\S]*valueType:\s*'DECIMAL'[\s\S]*lowerLimit:\s*20[\s\S]*targetValue:\s*26[\s\S]*upperLimit:\s*30[\s\S]*decimalScale:\s*1/,
  '精洗室温必须是 20 到 30、默认 26、1 位小数的数字参数。'
)
assert.doesNotMatch(
  leaderPage,
  /FINE_WASH_ROOM_TEMPERATURE[\s\S]*valueType:\s*'TEXT_STANDARD'/,
  '精洗室温不得继续作为室温文本标准。'
)
assert.match(
  leaderPage,
  /case 'FINE_WASH_MEDIUM':[\s\S]*optionValues:\s*template\.optionValues \? \[\.\.\.template\.optionValues\] : undefined[\s\S]*defaultText:\s*roughWashParameterForm\.cleaningMedium/,
  '精洗清洗介质保存载荷必须写入下拉选项。'
)
assert.match(
  leaderPage,
  /resetRoughWashParameterForm = \(config: CleaningWashProcessConfig\)[\s\S]*roughWashParameterForm\.cleaningMedium = config\.defaultCleaningMedium/,
  '精洗打开固定参数配置时必须按工序默认介质初始化。'
)
assert.match(
  leaderPage,
  /case 'FINE_WASH_ROOM_TEMPERATURE':[\s\S]*targetValue:\s*roomTemperatureDefault[\s\S]*decimalScale:\s*1/,
  '精洗室温保存载荷必须写入数字默认值和小数位。'
)

console.log('fine-wash-device-parameter-config-static PASS')
