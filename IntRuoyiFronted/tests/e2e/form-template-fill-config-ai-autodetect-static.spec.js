const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const dialogPath = path.join(
  frontendRoot,
  'src',
  'views',
  'form-center',
  'template',
  'components',
  'FormTemplateFillConfigDialog.vue'
)
const indexPath = path.join(frontendRoot, 'src', 'views', 'form-center', 'template', 'index.vue')
const apiPath = path.join(frontendRoot, 'src', 'api', 'form-center', 'template.ts')
const realE2ePath = path.join(frontendRoot, 'tests', 'e2e', 'form-template-fill-config-ai-autodetect-real.e2e.cjs')

const dialog = fs.readFileSync(dialogPath, 'utf8')
const indexPage = fs.readFileSync(indexPath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')
assert.ok(fs.existsSync(realE2ePath), 'real Playwright E2E entry must exist')

assert.match(dialog, /AI 自动识别可在任意版本执行/)
assert.match(dialog, /handleAutoDetect/)
assert.match(dialog, /pendingAiCandidates/)
assert.match(dialog, /应用识别结果/)
assert.match(dialog, /applyAiCandidates/)
assert.match(dialog, /draft-version-ready/)
assert.match(api, /fill-rule-auto-detect/)
assert.match(api, /FormTemplateFillRuleAutoDetectRespVO/)
assert.match(api, /timeout:\s*180000/)
assert.match(api, /sourceVersionNo/)
assert.match(api, /targetStatus/)
assert.match(api, /draftCreated/)
assert.match(indexPage, /@draft-version-ready="handleDraftVersionReady"/)
assert.match(indexPage, /selectTemplateVersion/)

const autoDetectDisabledBlock = dialog.match(/const aiDetectDisabled = computed\([\s\S]*?\n\)/)
assert.ok(autoDetectDisabledBlock, 'aiDetectDisabled computed state must be present')
assert.doesNotMatch(
  autoDetectDisabledBlock[0],
  /readonlyMode/,
  'AI 自动识别只读取已保存模板版本并生成候选，不能因为当前版本只读而禁用按钮'
)

const autoDetectBlock = dialog.match(/const handleAutoDetect[\s\S]*?\r?\n}\r?\n/)
assert.ok(autoDetectBlock, 'handleAutoDetect must be present')
assert.doesNotMatch(autoDetectBlock[0], /emit\(['"]save['"]/) // AI detection must never save directly
assert.match(autoDetectBlock[0], /emit\(['"]draft-version-ready['"]/)
assert.match(autoDetectBlock[0], /preserveAiCandidatesOnReload\.value/)
assert.match(autoDetectBlock[0], /response\.versionNo !== versionNo/)

const applyCandidatesBlock = dialog.match(/const applyAiCandidates[\s\S]*?\r?\n}\r?\n/)
assert.ok(applyCandidatesBlock, 'applyAiCandidates must be present')
assert.match(
  applyCandidatesBlock[0],
  /readonlyMode\.value/,
  '应用 AI 候选会改变当前编辑态，仍必须受草稿/只读规则限制'
)

console.log('form-template-fill-config-ai-autodetect-static: PASS')
