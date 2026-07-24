const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const editPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const formContent = read('src/views/mes/pro/route/RouteFormContent.vue')

assert.match(
  editPage,
  /@success="handleSaved"/,
  '候选版本编辑页必须监听保存成功事件，用于保存后提示提交发布/审批。'
)

assert.match(
  editPage,
  /type RouteFormSavedPayload = \{[\s\S]*promptRouteVersionSubmit\?:\s*boolean[\s\S]*\}/,
  'RouteEditPage 必须定义保存成功事件载荷，区分普通保存和退出前保存。'
)

assert.match(
  editPage,
  /type SubmitRouteCandidateVersionOptions = \{[\s\S]*confirmMessage\?:\s*string[\s\S]*confirmTitle\?:\s*string[\s\S]*\}[\s\S]*const submitRouteCandidateVersion = async \(options: SubmitRouteCandidateVersionOptions = \{\}\)/,
  '候选版本提交逻辑必须抽成可复用函数，支持保存后提示和顶部提交共用正式提交接口。'
)

assert.match(
  editPage,
  /const confirmSubmitRouteCandidateVersionAfterSave = async \(\) => \{[\s\S]*context\.lifecycleStatus !== 'DRAFT'[\s\S]*submitRouteCandidateVersion\(\{[\s\S]*草稿已保存[\s\S]*提交发布[\s\S]*审批通过后自动发布生效[\s\S]*\}\)[\s\S]*\}[\s\S]*const handleSaved = async \(payload\?: RouteFormSavedPayload\) => \{[\s\S]*payload\?\.promptRouteVersionSubmit === false[\s\S]*confirmSubmitRouteCandidateVersionAfterSave\(\)/s,
  'DRAFT 候选版本保存成功后必须提示是否立即提交发布，非 DRAFT 或退出前保存不得弹出发布提示。'
)

assert.match(
  editPage,
  /const handleSubmitRouteCandidateVersion = async \(\) => \{[\s\S]*submitRouteCandidateVersion\(\{[\s\S]*提交后当前候选版本将进入审批阶段[\s\S]*审批通过后自动发布生效[\s\S]*\}\)/,
  '顶部“提交发布”按钮必须继续走同一提交函数并保留提交前确认。'
)

assert.doesNotMatch(
  editPage,
  /promptRouteVersionSignaturePassword|电子签名发布|signaturePassword/,
  '保存后确认发布不得弹出提交者电子签名；审批动作签名在审批中心完成。'
)
assert.match(
  editPage,
  /ProRouteApi\.submitAndPublishRouteCandidateVersion\(\{[\s\S]*id:\s*context\.routeVersionId[\s\S]*\}\)/,
  '保存后确认发布必须只提交候选版本 ID。'
)

assert.match(
  formContent,
  /type RouteFormSubmitOptions = \{[\s\S]*promptRouteVersionSubmit\?:\s*boolean[\s\S]*\}/,
  'RouteFormContent 必须支持保存选项，允许退出前保存跳过发布提示。'
)

assert.match(
  formContent,
  /const submitForm = async \(options: RouteFormSubmitOptions = \{\}\) => \{[\s\S]*emit\('success',\s*\{[\s\S]*promptRouteVersionSubmit:\s*options\.promptRouteVersionSubmit !== false[\s\S]*\}\)/,
  '普通保存成功事件必须携带 promptRouteVersionSubmit=true，供页面弹出发布提示。'
)

assert.match(
  formContent,
  /confirmFlowGraphDraftSaveBeforeExit[\s\S]*await submitForm\(\{\s*promptRouteVersionSubmit:\s*false\s*\}\)/,
  '退出前“保存草稿”只应保存并离开，不得再弹保存后发布提示。'
)

console.log('PASS: mes route candidate save publish prompt static contract')
