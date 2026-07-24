const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const assertIncludes = (content, expected, message) => {
  if (!content.includes(expected)) throw new Error(message)
}

const attachmentApi = read('src/api/mes/pro/edhr/attachment.ts')
const fieldAuditApi = read('src/api/mes/pro/edhr/fieldAudit.ts')
const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
const uploadFile = read('src/components/UploadFile/src/UploadFile.vue')
const uploadImg = read('src/components/UploadFile/src/UploadImg.vue')
const uploadImgs = read('src/components/UploadFile/src/UploadImgs.vue')

assertIncludes(
  attachmentApi,
  '/mes/pro/batch-record-execution/attachment/prepare-upload',
  'eDHR 附件 API 必须调用专用 prepare-upload 接口'
)
assertIncludes(attachmentApi, "formData.append('executionId'", 'prepareUpload 必须提交 executionId')
assertIncludes(attachmentApi, "formData.append('workTaskId'", 'prepareUpload 必须提交 workTaskId')
assertIncludes(attachmentApi, "formData.append('file'", 'prepareUpload 必须提交 multipart file')
assertIncludes(attachmentApi, 'uploadToken: string', 'prepareUpload 响应必须包含 uploadToken')
assertIncludes(attachmentApi, 'fileId: number', 'prepareUpload 响应必须包含 fileId')
assertIncludes(attachmentApi, 'storageConfigId: number', 'prepareUpload 响应必须包含 storageConfigId')
assertIncludes(attachmentApi, 'storagePath: string', 'prepareUpload 响应必须包含 storagePath')
assertIncludes(attachmentApi, 'sha256: string', 'prepareUpload 响应必须包含 sha256')
assertIncludes(
  attachmentApi,
  'storageRetentionJson: string',
  'prepareUpload 响应必须包含 storageRetentionJson'
)
assertIncludes(
  attachmentApi,
  'storageRetentionHash: string',
  'prepareUpload 响应必须包含 storageRetentionHash'
)
assertIncludes(
  attachmentApi,
  'EdhrAttachmentPrepareUploadApiResp',
  'prepareUpload wrapper 必须显式建模后端业务响应外层'
)
assertIncludes(
  attachmentApi,
  'return response.data',
  'prepareUpload wrapper 必须返回后端 data 内的结构化元数据'
)
assertIncludes(
  fieldAuditApi,
  "attachmentAction: 'ADD' | 'REPLACE' | 'VOID'",
  '附件保存动作必须与后端追加账本动作一致'
)
assertIncludes(fieldAuditApi, 'workTaskId: number', '附件保存契约必须携带工作任务 ID')
assertIncludes(fieldAuditApi, "attachmentType: 'FILE' | 'IMAGE'", '附件保存契约必须携带附件类型')
assertIncludes(fieldAuditApi, 'storageRetentionJson?: string', '附件保存契约必须携带存储保留证据')
assertIncludes(fieldAuditApi, 'storageRetentionHash?: string', '附件保存契约必须携带存储保留证据 hash')
assertIncludes(
  executionPage,
  "attachmentAction: 'ADD'",
  '执行页新增附件动作必须提交后端支持的 ADD'
)
assertIncludes(
  executionPage,
  'prepareEdhrAttachmentUpload',
  '执行页附件上传必须调用 eDHR 专用 prepareUpload API'
)
assertIncludes(
  executionPage,
  'createEdhrAttachmentUploadRequest',
  '执行页必须为附件上传组件注入 eDHR 专用上传请求'
)
assertIncludes(
  executionPage,
  ':http-request="createEdhrAttachmentUploadRequest(field)"',
  '执行页上传组件必须显式使用 eDHR 专用上传请求'
)
assertIncludes(
  executionPage,
  'attachmentMetadataByUrl',
  '执行页必须保存 prepareUpload 返回的结构化附件元数据'
)
assertIncludes(
  executionPage,
  'storageRetentionJson: metadata?.storageRetentionJson',
  'pending 附件必须携带 storageRetentionJson'
)
assertIncludes(
  executionPage,
  'storageRetentionHash: metadata?.storageRetentionHash',
  'pending 附件必须携带 storageRetentionHash'
)
assertIncludes(
  executionPage,
  'workTaskId: workTaskId.value',
  'pending 附件必须携带当前工作任务 ID'
)
assertIncludes(
  executionPage,
  'attachmentType: resolveAttachmentType(field.componentKind)',
  'pending 附件必须按控件类型映射附件类型'
)
assertIncludes(
  executionPage,
  'storageRetentionJson: change.storageRetentionJson',
  '保存请求必须提交 storageRetentionJson'
)
assertIncludes(
  executionPage,
  'storageRetentionHash: change.storageRetentionHash',
  '保存请求必须提交 storageRetentionHash'
)
for (const [name, content] of [
  ['UploadFile', uploadFile],
  ['UploadImg', uploadImg],
  ['UploadImgs', uploadImgs]
]) {
  assertIncludes(content, 'httpRequest: propTypes.func.def(undefined)', `${name} 必须暴露自定义上传请求 prop`)
  assertIncludes(content, 'props.httpRequest || defaultHttpRequest', `${name} 必须在未传自定义请求时继续使用默认上传`)
}

console.log('eDHR attachment prepare-upload API static checks passed')
