const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const previewApi = readSource('src/api/common/filePreview.ts')
const dccViewer = readSource('src/views/dcc/controlled-file/view/index.vue')
const edhrDetail = readSource('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')

assert.match(
  previewApi,
  /export type OnlineFilePreviewSource/,
  '统一在线预览 API 必须暴露 OnlineFilePreviewSource，供 DCC/eDHR 共用。'
)
assert.match(
  previewApi,
  /DCC_CONTROLLED_FILE/,
  '统一在线预览 API 必须保留 DCC 受控文件 source 类型。'
)
assert.match(
  previewApi,
  /EDHR_SPECIAL_NODE_ATTACHMENT/,
  '统一在线预览 API 必须提供 eDHR 特殊节点附件 source 类型。'
)
assert.match(
  previewApi,
  /\/dcc\/file-preview\/files\/\$\{source\.fileId\}\/preview-metadata/,
  'eDHR 普通附件必须通过统一预览元数据接口获取预览上下文。'
)
assert.match(
  previewApi,
  /\/dcc\/file-preview\/files\/\$\{source\.fileId\}\/preview/,
  'eDHR 普通附件二进制内容必须通过统一预览内容接口读取。'
)
assert.match(
  previewApi,
  /getControlledFilePreviewMetadata/,
  'DCC 受控文件必须通过统一预览 API 委托既有 DCC 受控元数据接口，保留原权限/审计。'
)
assert.match(
  previewApi,
  /previewControlledFileWithWatermark/,
  'DCC 受控文件必须通过统一预览 API 委托既有 DCC 受控二进制接口，保留水印和 token。'
)

assert.match(
  dccViewer,
  /from '@\/api\/common\/filePreview'/,
  'DCC 预览组件必须导入统一在线预览 API。'
)
assert.match(
  dccViewer,
  /previewSource\?: OnlineFilePreviewSource \| null/,
  'DCC 预览组件必须支持统一 previewSource，而不只支持 controlledFileId。'
)
assert.match(
  dccViewer,
  /getOnlineFilePreviewMetadata/,
  'DCC 预览组件元数据加载必须走统一在线预览 API。'
)
assert.match(
  dccViewer,
  /previewOnlineFileWithWatermark/,
  'DCC 预览组件二进制读取必须走统一在线预览 API。'
)

assert.match(
  edhrDetail,
  /import ProtectedPdfViewer from '@\/views\/dcc\/controlled-file\/view\/index\.vue'/,
  'eDHR 特殊节点附件预览必须复用 DCC 查阅文件同款预览组件。'
)
assert.match(
  edhrDetail,
  /buildEdhrSpecialNodeAttachmentPreviewSource/,
  'eDHR 必须按 fileId 构建统一预览 source。'
)
assert.match(
  edhrDetail,
  /selectedSpecialNodePreviewSource/,
  'eDHR 必须把统一 previewSource 传给预览组件。'
)
assert.match(
  edhrDetail,
  /<ProtectedPdfViewer[\s\S]*?:preview-source="selectedSpecialNodePreviewSource"/,
  'eDHR 预览弹窗必须通过 preview-source 使用统一预览能力。'
)
assert.doesNotMatch(
  edhrDetail,
  /window\.open\(attachment\.fileUrl/,
  'eDHR 特殊节点附件不得再直接裸开 fileUrl。'
)
assert.match(
  edhrDetail,
  /附件缺少文件编号，无法在线预览/,
  'eDHR 历史附件缺少 fileId 时必须明确报错，不允许降级裸开 URL。'
)

console.log('PASS: unified online file preview static contract')
