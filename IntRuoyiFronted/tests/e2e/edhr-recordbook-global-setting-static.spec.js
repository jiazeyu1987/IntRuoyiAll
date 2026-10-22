const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const CONFIG_KEY = 'mes.edhr.recordbook.global.enabled'
const GOLDEN_FINGER_PERMISSION = 'mes:pro-batch-record-execution:golden-finger'

const apiPath = 'src/api/mes/pro/edhr/recordbookGlobalSetting.ts'
const api = readSource(apiPath)

assert.ok(api.includes('/mes/pro/edhr-recordbook-setting/global'), 'API wrapper must target the global setting endpoint.')
assert.ok(api.includes('getEdhrRecordbookGlobalSetting'), 'API wrapper must expose a GET helper.')
assert.ok(api.includes('updateEdhrRecordbookGlobalSetting'), 'API wrapper must expose a PUT helper.')
assert.ok(api.includes('EdhrRecordbookGlobalSettingRespVO'), 'API wrapper must type the response VO.')
assert.ok(api.includes('EdhrRecordbookGlobalSettingUpdateReqVO'), 'API wrapper must type the update VO.')

const page = readSource('src/views/mes/pro/edhr-config/index.vue')
assert.ok(page.includes('<RecordbookGlobalSetting />'), 'MES config must mount recordbook control')
assert.ok(page.includes('v-if="canQuery"') && page.includes('hasExplicitConfigPermission(GOLDEN_FINGER_PERMISSION)'), 'MES config requires explicit golden-finger grant')
assert.ok(readSource('src/utils/businessConfigAccess.ts').includes(GOLDEN_FINGER_PERMISSION), 'permission code remains unchanged')
assert.ok(!readSource('src/views/Profile/Index.vue').includes('EdhrRecordbookGlobalSetting'), 'Profile no longer imports recordbook control')

for (const componentPath of [
  'src/views/mes/pro/edhr-config/components/RecordbookGlobalSetting.vue'
]) {
  const component = readSource(componentPath)
  assert.ok(component.includes('el-switch'), `${componentPath} must use an editable switch.`)
  assert.ok(!component.includes('el-descriptions'), `${componentPath} must not render the old red-box metadata block.`)
  for (const removedText of [CONFIG_KEY, '配置键', '当前状态', '最后更新人', '最后更新时间']) {
    assert.ok(!component.slice(0, component.indexOf('</template>')).includes(removedText), `${componentPath} must remove red-box metadata text: ${removedText}`)
  }
  assert.ok(component.includes('role="button"'), `${componentPath} must make the blue-box switch area keyboard/click accessible.`)
  assert.ok(component.includes('@click="handleToggleClick"'), `${componentPath} must toggle from the whole blue-box area.`)
  assert.ok(component.includes('@keydown.enter.space.prevent="handleToggleClick"'), `${componentPath} must support keyboard activation.`)
  assert.ok(component.includes('ElMessageBox.confirm'), `${componentPath} must confirm before toggling.`)
  assert.ok(component.includes('updateEdhrRecordbookGlobalSetting'), `${componentPath} must call the update API.`)
  assert.ok(component.includes('settingEnabled.value = previousValue'), `${componentPath} must restore on cancel or failure.`)
  assert.ok(component.includes('接口保存失败'), `${componentPath} must surface API failure without silent downgrade.`)
}

const detail = readSource('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
assert.ok(detail.includes('getEdhrRecordbookGlobalSetting'), 'Batch detail must read the global setting.')
assert.ok(detail.includes('isGlobalRecordbookEnabled'), 'Batch detail must track global recordbook state.')
assert.ok(
  /v-if="\s*selectedTaskForEvidence\s*&&\s*!isSpecialNode\(selectedTaskForEvidence\)\s*&&\s*isGlobalRecordbookEnabled\s*"/.test(detail),
  'Batch detail must hide the entire fill-carrier control when global recordbook is disabled.'
)
assert.ok(
  detail.includes("if (!isGlobalRecordbookEnabled.value) return 'FORM'"),
  'Batch detail must force FORM when the global switch is disabled.'
)
assert.ok(
  detail.includes("fillCarrier === 'RECORDBOOK' && !isGlobalRecordbookEnabled.value"),
  'Batch detail must reject selecting recordbook while globally disabled.'
)

const executionPage = readSource('src/views/mes/pro/edhr/ExecutionPage.vue')
assert.ok(executionPage.includes('getEdhrRecordbookGlobalSetting'), 'Execution page must read the global setting.')
assert.ok(executionPage.includes('recordbookGlobalDisabledNotice'), 'Execution page must expose a direct-link disabled notice.')
assert.ok(
  executionPage.includes('route.query.fillCarrier === \'RECORDBOOK\'') &&
    executionPage.includes('isGlobalRecordbookEnabled.value === false'),
  'Execution page must detect direct RECORDBOOK URL while global switch is disabled.'
)
assert.ok(
  executionPage.includes('记录本全局开关已关闭') &&
    executionPage.includes('return false'),
  'Execution page must block unrestricted recordbook mode instead of falling back silently.'
)

console.log('PASS: eDHR recordbook global setting frontend static contract')
