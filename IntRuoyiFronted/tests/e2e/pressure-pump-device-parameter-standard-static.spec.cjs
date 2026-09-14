const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const teamApi = readUtf8('src/api/mes/pro/processpool/teamLeader.ts')
const feedbackApi = readUtf8('src/api/mes/pro/feedback/index.ts')
const leaderPage = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const frontlinePanel = readUtf8('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

assert.match(teamApi, /standardText:\s*string/, '工序配置接口必须返回并保存参数原文标准。')
assert.match(teamApi, /'TEXT_STANDARD'/, '工序配置接口必须声明文本标准类型。')
assert.match(
  teamApi,
  /getTeamDeviceList[\s\S]*\/mes\/pro\/process-pool\/team-leader\/team-device\/list/,
  '首次映射工序设备前必须能从正式班组设备列表取得候选项。'
)
assert.match(feedbackApi, /standardText:\s*string/, '一线运行配置必须携带参数原文标准。')

assert.match(
  leaderPage,
  /data-team-leader-process-config-standard-text/,
  '生产组长工序配置列表必须显示原文标准。'
)
assert.match(
  leaderPage,
  /value="TEXT_STANDARD"/,
  '生产组长必须可维护文本标准。'
)
assert.match(
  leaderPage,
  /processConfigParameterForm\.standardText/,
  '参数标准表单必须编辑并提交原文标准。'
)
assert.match(
  leaderPage,
  /processConfigParameterForm\.valueType\s*!==\s*'TEXT_STANDARD'/,
  '文本标准不得要求输入数值上下限。'
)
assert.match(
  leaderPage,
  /const processConfigDeviceOptions = computed\(\(\) => teamDeviceOptions\.value\)/,
  '工序设备候选必须来自班组设备列表，不能依赖已有映射反推。'
)
assert.match(
  leaderPage,
  /await loadTeamDeviceOptions\(\)[\s\S]*ElMessage\.success\('设备已新增'\)/,
  '新增班组设备后必须刷新首次映射候选。'
)

assert.match(
  frontlinePanel,
  /data-frontline-text-parameter-standard/,
  '一线页面必须只读展示文本标准。'
)
assert.match(
  frontlinePanel,
  /isTextStandardParameter/,
  '一线页面必须区分文本标准与数值读数。'
)
assert.match(
  frontlinePanel,
  /filter\(\(parameter\) => !isTextStandardParameter\(parameter\)\)/,
  '文本标准不得进入设备参数读数提交。'
)

console.log('pressure-pump-device-parameter-standard-static PASS')
