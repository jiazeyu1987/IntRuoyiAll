const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const uploadPagePath = path.join(repoRoot, 'src/views/dcc/controlled-file/upload/index.vue')
const submitterPath = path.join(repoRoot, 'src/views/dcc/controlled-file/upload/submitter.ts')
const workflowApiPath = path.join(repoRoot, 'src/api/dcc/controlledFile/workflow.ts')
const packageJsonPath = path.join(repoRoot, 'package.json')

const uploadPage = fs.readFileSync(uploadPagePath, 'utf8')
const submitter = fs.readFileSync(submitterPath, 'utf8')
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

for (const field of [
  'dccProjectCodeId',
  'fileTypeTaxonomyId',
  'revisionTargetControlledFileId'
]) {
  requireIn(workflowApi, field, `workflow API 必须声明提交字段：${field}`)
  requireIn(submitter, field, `submitter 必须传递提交字段：${field}`)
  requireIn(uploadPage, field, `上传页必须维护表单字段：${field}`)
}

requireIn(
  workflowApi,
  '/dcc/controlled-files/upload-revision-candidates',
  'workflow API 必须提供上传专用升版候选接口'
)
requireIn(
  workflowApi,
  'getControlledFileUploadRevisionCandidates',
  'workflow API 必须导出升版候选查询函数'
)
requireIn(workflowApi, 'fileTypeTaxonomyIds', '受控文件查询参数必须支持文件分类范围')

for (const token of [
  "getProjectCodePage",
  "DCC_PROJECT_CODE_STATUS_ENABLE",
  "getFileTypeTaxonomyUploadOptions",
  "handleTree",
  'label="DCC项目"',
  'label="文件分类"',
  '请选择至少三级文件分类',
  '请选择 DCC 项目',
  'resolveHistoryRevisionTarget',
  'clearRevisionTargetSelection'
]) {
  requireIn(uploadPage, token, `上传页缺少项目/文件分类/自动升版契约：${token}`)
}

for (const removedRevisionUi of [
  'data-testid="dcc-upload-revision-candidates"',
  'label="升版目标"',
  'handleRevisionCandidateSelect'
]) {
  assert(!uploadPage.includes(removedRevisionUi), `上传页不得提供手动升版目标选择入口：${removedRevisionUi}`)
}

for (const autoRevisionToken of [
  '@select="handleHistoryFileNameSelect"',
  "formData.changeType = 'REVISION'",
  'await resolveHistoryRevisionTarget(item.value)',
  "formData.changeType = 'NEW'",
  '请选择历史文件名称后再升版',
  'revisionTargetPreflightBlockReason'
]) {
  requireIn(uploadPage, autoRevisionToken, `上传页必须按历史文件名称自动判定新建/升版：${autoRevisionToken}`)
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
  'availableCategories',
  '未配置专属目录',
  '按规则发布到“未分类”',
  'defaultUnclassified'
]) {
  requireIn(uploadPage, unclassifiedLandingToken, `上传页必须把未绑定提交目录的文件类别自动落位到未分类：${unclassifiedLandingToken}`)
}

assert(
  !uploadPage.includes('请先在 DCC 文件类别维护目录绑定'),
  '上传页不得要求提交人手工维护文件类别提交目录绑定'
)

console.log('DCC upload project taxonomy revision static contract passed.')
