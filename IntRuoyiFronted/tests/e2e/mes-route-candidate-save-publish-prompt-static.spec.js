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
  '候选版本编辑页必须监听保存成功事件，用于标记草稿已保存。'
)

assert.doesNotMatch(
  editPage,
  /type RouteFormSavedPayload|promptRouteVersionSubmit/,
  '普通保存不再区分保存后发布提示载荷，保存必须只保存草稿。'
)

assert.match(
  editPage,
  /type SubmitRouteCandidateVersionOptions = \{[\s\S]*confirmMessage\?:\s*string[\s\S]*confirmTitle\?:\s*string[\s\S]*\}[\s\S]*const submitRouteCandidateVersion = async \(options: SubmitRouteCandidateVersionOptions = \{\}\)/,
  '候选版本提交逻辑必须保留为显式提交发布入口的正式提交接口。'
)

assert.doesNotMatch(
  editPage,
  /confirmSubmitRouteCandidateVersionAfterSave|草稿已保存，是否立即提交发布/,
  'DRAFT 候选版本普通保存成功后不得提示立即提交发布，避免草稿被推进审批后无法继续编辑。'
)

assert.match(
  editPage,
  /const handleSaved = async \(\) => \{[\s\S]*markListEditCandidateDraftSaved\(\)[\s\S]*clearListEditDraftExitQuery\(\)[\s\S]*\}/,
  '普通保存成功后只应标记草稿已保存并清理列表直建草稿退出标记。'
)

assert.match(
  editPage,
  /const handleSubmitRouteCandidateVersion = async \(\) => \{[\s\S]*submitRouteCandidateVersion\(\{[\s\S]*提交后当前候选版本将进入审批阶段[\s\S]*审批通过后自动发布生效[\s\S]*\}\)/,
  '显式“提交发布”入口必须继续走提交函数并保留提交前确认。'
)

assert.doesNotMatch(
  editPage,
  /promptRouteVersionSignaturePassword|电子签名发布|signaturePassword/,
  '显式提交发布不得弹出提交者电子签名；审批动作签名在审批中心完成。'
)
assert.match(
  editPage,
  /ProRouteApi\.getRouteVersion\(context\.routeVersionId\)[\s\S]*ProRouteApi\.submitAndPublishRouteCandidateVersion\(\{[\s\S]*id:\s*latestVersion\.id[\s\S]*\}\)/,
  '显式提交发布必须刷新最新草稿状态后只提交候选版本 ID。'
)

assert.doesNotMatch(
  formContent,
  /type RouteFormSubmitOptions|promptRouteVersionSubmit/,
  'RouteFormContent 保存不应再携带保存后发布提示选项。'
)

assert.match(
  formContent,
  /const submitForm = async \(\) => \{[\s\S]*emit\('success'\)/,
  '普通保存成功事件只通知保存完成，不携带提交发布提示载荷。'
)

assert.match(
  formContent,
  /confirmFlowGraphDraftSaveBeforeExit[\s\S]*await submitForm\(\)/,
  '退出前“保存草稿”只应保存并离开，不得再弹保存后发布提示。'
)

console.log('PASS: mes route candidate save keeps draft editable static contract')
