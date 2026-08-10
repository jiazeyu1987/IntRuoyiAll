const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const teamApi = readUtf8('src/api/mes/pro/processpool/teamLeader.ts')
const feedbackApi = readUtf8('src/api/mes/pro/feedback/index.ts')
const leaderPage = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const frontlinePanel = readUtf8('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

assert.match(
  teamApi,
  /DeviceParameterValueType = 'INTEGER' \| 'DECIMAL' \| 'TEXT_STANDARD' \| 'SELECT'/,
  '工序配置参数类型必须支持 SELECT 下拉框。'
)
assert.match(teamApi, /optionValues\?: string\[\]/, '工序配置参数必须透传下拉选项。')
assert.match(teamApi, /defaultText\?: string \| null/, '工序配置参数必须透传文本默认值。')
assert.match(teamApi, /decimalScale\?: number \| null/, '工序配置参数必须透传小数位数。')
assert.match(feedbackApi, /optionValues\?: string\[\]/, '一线运行态必须携带下拉选项。')
assert.match(feedbackApi, /defaultText\?: string \| null/, '一线运行态必须携带文本默认值。')
assert.match(feedbackApi, /textValue\?: string/, '一线正式提交载荷必须支持下拉文本读数。')

for (const label of ['清洗次数', '清洗介质', '清洗功率', '室温', '清洗时间']) {
  assert.ok(leaderPage.includes(label), '粗洗参数模板必须包含' + label + '。')
}
assert.match(leaderPage, /value="SELECT"/, '工序配置参数弹窗必须支持选择下拉框类型。')
assert.match(
  leaderPage,
  /data-team-leader-process-config-option-values/,
  '参数弹窗必须可配置下拉选项。'
)
assert.match(
  leaderPage,
  /data-team-leader-process-config-default-text/,
  '参数弹窗必须可配置文本默认值。'
)
assert.match(
  leaderPage,
  /data-team-leader-process-config-decimal-scale/,
  '参数弹窗必须可配置小数位数。'
)
assert.match(
  leaderPage,
  /清洗次数[\s\S]*targetValue:\s*2[\s\S]*lowerLimit:\s*undefined[\s\S]*upperLimit:\s*undefined/,
  '清洗次数模板必须是默认 2 且无上下限。'
)
assert.match(
  leaderPage,
  /清洗介质[\s\S]*valueType:\s*'SELECT'[\s\S]*optionValues:\s*\['自来水', '纯净水'\][\s\S]*defaultText:\s*'自来水'/,
  '清洗介质模板必须是自来水/纯净水下拉，默认自来水。'
)
assert.match(
  leaderPage,
  /清洗功率[\s\S]*valueType:\s*'INTEGER'[\s\S]*lowerLimit:\s*20[\s\S]*upperLimit:\s*30/,
  '清洗功率模板必须是 20 到 30 的整数。'
)
assert.match(
  leaderPage,
  /室温[\s\S]*valueType:\s*'DECIMAL'[\s\S]*lowerLimit:\s*20[\s\S]*targetValue:\s*26[\s\S]*upperLimit:\s*30[\s\S]*decimalScale:\s*1/,
  '室温模板必须是 20 到 30、默认 26、1 位小数。'
)
assert.match(
  leaderPage,
  /清洗时间[\s\S]*targetValue:\s*30[\s\S]*lowerLimit:\s*undefined[\s\S]*upperLimit:\s*undefined/,
  '清洗时间模板必须是默认 30 且无上下限。'
)

assert.match(frontlinePanel, /isSelectParameter/, '一线页面必须区分下拉参数。')
assert.match(frontlinePanel, /data-frontline-select-parameter/, '一线页面必须渲染下拉参数控件。')
assert.match(frontlinePanel, /textValue/, '一线提交必须传递下拉文本读数。')
assert.match(frontlinePanel, /decimalScale/, '一线页面必须按配置小数位归一化。')

console.log('rough-wash-device-parameter-config-static PASS')
