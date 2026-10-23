const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const uploadPagePath = path.join(repoRoot, 'src/views/dcc/controlled-file/upload/index.vue')
const workflowApiPath = path.join(repoRoot, 'src/api/dcc/controlledFile/workflow.ts')
const packageJsonPath = path.join(repoRoot, 'package.json')

const uploadPage = fs.readFileSync(uploadPagePath, 'utf8')
const workflowApi = fs.readFileSync(workflowApiPath, 'utf8')
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'))

const assert = (condition, message) => {
  if (!condition) {
    throw new Error(message)
  }
}

const requireIn = (source, token, message) => assert(source.includes(token), message)

assert(
  packageJson.scripts['e2e:dcc:upload-project-taxonomy-revision:static'] ===
    'node tests/e2e/dcc-upload-project-taxonomy-revision-static.spec.js',
  'package.json 必须提供 DCC 上传项目分类升版静态契约脚本'
)

for (const field of ['dccProjectCodeId', 'fileTypeTaxonomyId']) {
  requireIn(workflowApi, field, `workflow API 必须声明提交字段：${field}`)
  requireIn(uploadPage, field, `上传页必须维护表单字段：${field}`)
}

requireIn(
  workflowApi,
  '/dcc/controlled-files/submit',
  'workflow API 必须提供普通上传创建并送审接口'
)
requireIn(
  workflowApi,
  'submitControlledFile',
  'workflow API 必须导出普通上传创建并送审函数'
)
requireIn(workflowApi, 'fileTypeTaxonomyIds', '受控文件查询参数必须支持文件分类范围')

for (const token of [
  'getProjectCodePage',
  'DCC_PROJECT_CODE_STATUS_ENABLE',
  'getProjectCodeFileTemplate',
  'projectFileTemplateItems',
  'selectedProjectTemplateStageId',
  'selectedProjectTemplateTypeId',
  'label="DCC项目"',
  'label="阶段"',
  'label="文件类型"',
  'label="文件列表"',
  '请选择 DCC 项目',
  'handleProjectTemplateFileSelect',
  "formData.changeType = 'NEW'",
  'submitControlledFile'
]) {
  requireIn(uploadPage, token, `上传页缺少项目模板新建上传契约：${token}`)
}

for (const removedRevisionUi of [
  'data-testid="dcc-upload-revision-candidates"',
  'label="升版目标"',
  'handleRevisionCandidateSelect',
  'resolveHistoryRevisionTarget',
  'handleHistoryFileNameSelect',
  'revisionTargetPreflightBlockReason',
  "formData.changeType = 'REVISION'",
  'await resolveHistoryRevisionTarget(item.value)'
]) {
  assert(
    !uploadPage.includes(removedRevisionUi),
    `上传页不得提供或自动触发升版入口：${removedRevisionUi}`
  )
}

for (const preserved of [
  'label="文件类别"',
  'label="提交目录"',
  'getControlledFileUploadNameOptions',
  'validateControlledFileSelection',
  'validateDrawingPdfUpload'
]) {
  requireIn(uploadPage, preserved, `上传页必须保留既有上传契约：${preserved}`)
}

for (const removed of [
  'previewControlledFileRoute',
  'handleRoutePreview',
  '预览路线',
  '审批路线预览'
]) {
  assert(!uploadPage.includes(removed), `上传页必须移除路线预览展示契约：${removed}`)
}

for (const unclassifiedLandingToken of [
  '按规则发布到“未分类”',
  '自动提交到未分类目录',
  'defaultUnclassified'
]) {
  assert(
    !uploadPage.includes(unclassifiedLandingToken),
    `上传页不得把缺少默认目录配置自动落位到未分类：${unclassifiedLandingToken}`
  )
}

requireIn(
  uploadPage,
  '该类别未配置正式默认目录，请联系文控管理员配置后再提交。',
  '上传页必须在缺少正式默认目录时失败关闭'
)

console.log('DCC upload project taxonomy new-file static contract passed.')
