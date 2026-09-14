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

const roughWashDialogStart = leaderPage.indexOf('data-team-leader-rough-wash-config-dialog')
const roughWashDialogEnd = leaderPage.indexOf(
  'data-team-leader-process-config-parameter-dialog',
  roughWashDialogStart
)
assert.ok(roughWashDialogStart >= 0, '粗洗参数必须使用专用所见即所得配置弹窗。')
assert.ok(roughWashDialogEnd > roughWashDialogStart, '粗洗专用弹窗必须与通用参数弹窗隔离。')
const roughWashDialog = leaderPage.slice(roughWashDialogStart, roughWashDialogEnd)

for (const anchor of [
  'data-rough-wash-cleaning-count',
  'data-rough-wash-cleaning-medium',
  'data-rough-wash-power-lower',
  'data-rough-wash-power-default',
  'data-rough-wash-power-upper',
  'data-rough-wash-room-temperature-lower',
  'data-rough-wash-room-temperature-default',
  'data-rough-wash-room-temperature-upper',
  'data-rough-wash-cleaning-time'
]) {
  assert.ok(roughWashDialog.includes(anchor), '粗洗专用弹窗缺少所见即所得控件：' + anchor)
}
assert.ok(
  roughWashDialog.includes('data-team-leader-rough-wash-frontline-preview'),
  '粗洗专用弹窗必须提供一线填设备实时预览。'
)
for (const technicalLabel of ['参数编码', '值类型', '原文标准', '小数位数']) {
  assert.ok(!roughWashDialog.includes(technicalLabel), '粗洗专用弹窗不得暴露技术字段：' + technicalLabel)
}
assert.match(
  leaderPage,
  /const submitRoughWashParameterConfig = async \(\) =>[\s\S]*buildRoughWashParameterSavePayloads\(row, device\)[\s\S]*saveTeamProcessConfigDeviceParameterRule/,
  '粗洗专用弹窗必须通过一次保存入口提交五条正式参数规则。'
)
assert.match(
  leaderPage,
  /resolveCleaningWashProcessConfig\(row, device\)[\s\S]*openRoughWashParameterConfigDialog\(row, device, cleaningWashConfig\)/,
  '粗洗超声波清洗机必须进入专用配置弹窗。'
)
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
  /kind:\s*'ROUGH_WASH'[\s\S]*parameterCodePrefix:\s*'ROUGH_WASH'[\s\S]*defaultCleaningMedium:\s*'自来水'[\s\S]*cleaningMediumOptions:\s*\['自来水', '纯化水'\]/,
  '粗洗固定清洗参数必须默认自来水。'
)
assert.match(
  leaderPage,
  /parameterCode:\s*parameterCodePrefix \+ '_MEDIUM'[\s\S]*valueType:\s*'SELECT'[\s\S]*optionValues:\s*\[\.\.\.cleaningMediumOptions\][\s\S]*defaultText:\s*defaultCleaningMedium/,
  '清洗介质模板必须使用各工序正式下拉选项和默认值。'
)
assert.match(
  leaderPage,
  /清洗功率[\s\S]*valueType:\s*'INTEGER'[\s\S]*lowerLimit:\s*20[\s\S]*targetValue:\s*25[\s\S]*upperLimit:\s*30/,
  '清洗功率模板必须是下限 20、默认 25、上限 30 的整数。'
)
assert.match(
  leaderPage,
  /const roughWashParameterForm = reactive\(\{[\s\S]*powerLower:\s*20,[\s\S]*powerDefault:\s*25,[\s\S]*powerUpper:\s*30/,
  '粗洗参数表单必须使用清洗功率默认 25。'
)
assert.match(
  leaderPage,
  /case 'ROUGH_WASH_POWER':[\s\S]*lowerLimit:\s*powerLower,[\s\S]*targetValue:\s*powerDefault,[\s\S]*upperLimit:\s*powerUpper/,
  '清洗功率保存载荷必须写入可配置默认值。'
)
assert.match(
  roughWashDialog,
  /预览清洗功率[\s\S]*roughWashParameterForm\.powerDefault|roughWashParameterForm\.powerDefault[\s\S]*预览清洗功率/,
  '一线预览必须显示清洗功率默认值。'
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
assert.match(
  frontlinePanel,
  /<select[\s\S]*class="device-value device-select"[\s\S]*data-frontline-select-parameter/,
  '一线下拉参数必须使用设备参数下拉专用控件类，避免浏览器默认小控件。'
)
assert.match(
  frontlinePanel,
  /button,\s*input,\s*select[\s\S]*height:\s*72px[\s\S]*border:\s*3px solid var\(--frontline-line\)[\s\S]*background:\s*#f8faf8[\s\S]*font-weight:\s*900/,
  '一线下拉参数必须复用设备参数控件的高度、边框、背景和字体权重。'
)
assert.match(
  frontlinePanel,
  /select\.device-select[\s\S]*grid-column:\s*2 \/ 5[\s\S]*font-size:\s*32px[\s\S]*text-align-last:\s*center/,
  '一线下拉参数必须占用与数值控件一致的输入区域并居中显示。'
)
assert.match(frontlinePanel, /textValue/, '一线提交必须传递下拉文本读数。')
assert.match(frontlinePanel, /decimalScale/, '一线页面必须按配置小数位归一化。')

console.log('rough-wash-device-parameter-config-static PASS')
