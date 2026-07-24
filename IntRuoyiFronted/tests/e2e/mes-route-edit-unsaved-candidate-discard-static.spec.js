const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const listPage = read('src/views/mes/pro/route/index.vue')
const editPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const formContent = read('src/views/mes/pro/route/RouteFormContent.vue')

const getFunctionBody = (source, functionName) => {
  const marker = `const ${functionName} = async`
  const start = source.indexOf(marker)
  assert.notEqual(start, -1, `必须定义 ${functionName}。`)
  const nextFunction = source.indexOf('\nconst ', start + marker.length)
  assert.notEqual(nextFunction, -1, `必须能截取 ${functionName} 函数体。`)
  return source.slice(start, nextFunction === -1 ? source.length : nextFunction)
}

const editHandler = getFunctionBody(listPage, 'handleEditRouteProductionConfig')
assert.match(
  editHandler,
  /candidateResult\.created[\s\S]*routeDraftOrigin:\s*'list-edit'[\s\S]*discardOnUnsavedExit:\s*'1'[\s\S]*:\s*\{\}/,
  '列表“编辑”只有本次新建候选版本后，才允许在编辑页 query 标记未保存退出需丢弃。'
)
assert.match(
  editHandler,
  /buildRouteCandidateEditQuery\(candidateResult\.candidate,\s*\{[\s\S]*\.\.\.draftExitQuery/,
  '列表“编辑”复用既有 DRAFT 时必须进入候选编辑上下文，但不得携带直建草稿丢弃标记。'
)

assert.match(
  editPage,
  /import\s+\{\s*ElMessageBox\s*\}\s+from\s+'element-plus'/,
  'RouteEditPage 必须使用 Element Plus 确认框区分“保存草稿”“不保存草稿”和关闭弹框。'
)
assert.match(
  editPage,
  /const\s+LIST_EDIT_ROUTE_DRAFT_ORIGIN\s*=\s*'list-edit'/,
  'RouteEditPage 必须显式识别列表“编辑”直建候选草稿来源。'
)
assert.match(
  editPage,
  /const\s+shouldPromptUnsavedCandidateDraftBeforeExit\s*=\s*computed\([\s\S]*lifecycleStatus\s*===\s*'DRAFT'[\s\S]*isListEditCandidateDraftExitGuardEnabled[\s\S]*hasRouteCandidateDraftChanges/,
  'DRAFT 候选版本离开前必须触发草稿保存提示：列表直建草稿始终提示，已有草稿存在页面未保存改动时也必须提示。'
)

assert.match(
  editPage,
  /const\s+isListEditCandidateDraftExitGuardEnabled\s*=\s*computed\([\s\S]*routeDraftOrigin[\s\S]*discardOnUnsavedExit/,
  'RouteEditPage 必须单独识别列表直建草稿，避免已有草稿“不保存”误取消候选版本。'
)
assert.match(
  formContent,
  /const\s+hasRouteCandidateDraftChanges\s*=[\s\S]*hasFlowGraphWorkspaceDraftChanges/,
  'RouteFormContent 必须向页面暴露 DRAFT 候选草稿未保存工作区改动。'
)
assert.match(
  formContent,
  /const\s+discardRouteCandidateDraftChanges\s*=[\s\S]*discardWorkspaceDraftChanges/,
  'RouteFormContent 必须向页面暴露已有草稿“不保存”时的本地工作区改动丢弃能力。'
)

const leaveConfirm = getFunctionBody(editPage, 'confirmUnsavedCandidateDraftBeforeExit')
assert.match(
  leaveConfirm,
  /ElMessageBox\.confirm\([\s\S]*保存草稿[\s\S]*不保存草稿[\s\S]*distinguishCancelAndClose:\s*true/,
  '退出列表直建候选草稿时，必须显示“保存草稿 / 不保存草稿”并区分关闭弹框。'
)
assert.match(
  leaveConfirm,
  /await\s+content\.submitForm\(\{\s*promptRouteVersionSubmit:\s*false\s*\}\)/,
  '用户选择“保存草稿”且存在未保存变更时，必须先保存候选草稿再离开页面，且不弹保存后发布提示。'
)
assert.match(
  leaveConfirm,
  /isListEditCandidateDraftExitGuardEnabled\.value[\s\S]*ProRouteApi\.cancelRouteCandidateVersion\(context\.routeVersionId\)/,
  '用户选择“不保存草稿”时，必须调用候选版本取消接口丢弃本次列表直建草稿。'
)
assert.match(
  leaveConfirm,
  /error\s*===\s*'cancel'[\s\S]*cancelRouteCandidateVersion/,
  'Element Plus cancel 分支必须表示“不保存草稿”并丢弃候选版本。'
)
assert.match(
  leaveConfirm,
  /error\s*===\s*'cancel'[\s\S]*discardRouteCandidateDraftChanges/,
  '已有 DRAFT 草稿选择“不保存草稿”时只能丢弃当前页面未保存改动，不能取消已有候选版本。'
)
assert.match(
  leaveConfirm,
  /error\s*===\s*'close'[\s\S]*return\s+false/,
  '关闭退出确认弹框必须中止离开页面，不能默认保存或丢弃。'
)

assert.match(
  editPage,
  /const\s+confirmRouteEditPageLeave\s*=\s*async[\s\S]*confirmUnsavedCandidateDraftBeforeExit\(\)[\s\S]*confirmFlowGraphDraftSaveBeforeExit/,
  'RouteEditPage 离开页面时必须先处理候选草稿保存提示，再回退到普通流转图未保存变更确认。'
)
assert.match(
  editPage,
  /const\s+handleSaved\s*=\s*async[\s\S]*markListEditCandidateDraftSaved\(\)[\s\S]*confirmSubmitRouteCandidateVersionAfterSave/,
  '候选草稿保存成功后必须标记本次草稿已保存，并继续执行保存后提交发布提示。'
)

console.log('PASS: mes route edit unsaved list-edit candidate draft is saved or discarded explicitly')
