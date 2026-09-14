const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const uploadPage = readSource('src/views/dcc/controlled-file/upload/index.vue')
const uploadSubmitter = readSource('src/views/dcc/controlled-file/upload/submitter.ts')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const browserPresentation = readSource('src/views/dcc/controlled-file/browser/presentation.ts')
const approvalCenter = readSource('src/views/approval-center/index.vue')

assert.match(
  uploadSubmitter,
  /resolveUploadPreviewErrorMessage/,
  '上传预览必须使用专用错误归因 helper，而不是只透传通用系统异常。'
)
for (const keyword of ['文件存储服务不可用', '文件格式不受支持', '受控文件提交权限', '文件编号已存在']) {
  assert.match(uploadSubmitter, new RegExp(keyword), `上传预览错误归因必须覆盖：${keyword}`)
}
assert.match(
  uploadPage,
  /data-testid="dcc-upload-preview-error"/,
  '上传页必须在上传区域展示上传预览失败原因。'
)
assert.match(
  uploadPage,
  /将创建新的 master 主档/,
  '文件编号未命中现行版本时必须提示将创建 master 主档。'
)
assert.match(
  uploadPage,
  /文件编号已存在，不能重复创建 V1\.0 原版/,
  '文件编号和 V1.0 重复时必须提前提示并阻止提交。'
)
for (const keyword of ['当前没有可选文件类别', 'DCC 项目候选加载失败', '文件分类候选加载失败']) {
  assert.match(uploadPage, new RegExp(keyword), `上传页必须前置展示权限/基础数据缺口：${keyword}`)
}
assert.doesNotMatch(
  uploadSubmitter,
  /没有文件类别上传权限|UPLOAD 权限后再上传/,
  '上传错误归因不得继续把通用访问拒绝解释成文件类别 UPLOAD 权限。'
)

assert.match(
  detailPage,
  /待我审批\/签名处理态/,
  '审批详情页必须明确显示待我审批/签名处理态。'
)
assert.match(detailPage, /只读预览态/, 'viewer 详情页必须明确显示只读预览态。')
assert.match(detailPage, /电子签名审计证据/, '签名弹窗必须说明电子签名审计证据。')
assert.match(detailPage, /提交后流转/, '签名弹窗必须展示提交后流转说明。')
assert.match(detailPage, /查看受控浏览当前有效版/, 'ACTIVE 详情页必须提供当前有效版受控浏览入口。')
assert.match(detailPage, /当前有效版 \/ ACTIVE/, '详情页必须突出当前有效版 / ACTIVE 标识。')
for (const stageName of ['文控审核', '会签审核', '会签批准', '文控批准']) {
  assert.match(detailPage, new RegExp(stageName), `审批进度必须固定覆盖四级节点：${stageName}`)
}
for (const label of ['处理人', '处理时间', '签名状态']) {
  assert.match(detailPage, new RegExp(label), `审批进度必须展示${label}。`)
}

assert.match(browserPresentation, /当前有效版/, '受控浏览版本摘要必须突出当前有效版。')
assert.match(browserPage, /当前有效版 \/ ACTIVE/, '受控浏览列表必须展示当前有效版 / ACTIVE 标签。')

for (const label of ['文件编号', '版本', '文件类型', '申请人']) {
  assert.match(approvalCenter, new RegExp(label), `DCC 审批待办必须展示${label}。`)
}
assert.match(
  approvalCenter,
  /data-testid="approval-center-dcc-key-fields"/,
  'DCC 审批待办必须有专用关键信息区域。'
)

console.log('PASS: DCC original release UX improvements static contract')
