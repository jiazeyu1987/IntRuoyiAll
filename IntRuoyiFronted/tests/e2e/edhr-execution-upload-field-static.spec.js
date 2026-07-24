const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const assertIncludes = (content, expected, message) => {
  if (!content.includes(expected)) throw new Error(message)
}

const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
const fieldAuditApi = read('src/api/mes/pro/edhr/fieldAudit.ts')

assertIncludes(executionPage, "from '@/components/UploadFile'", '执行页必须复用平台上传组件')
assertIncludes(executionPage, "componentKind === 'upload-file'", '执行页必须识别 upload-file 字段')
assertIncludes(executionPage, "componentKind === 'upload-image'", '执行页必须识别 upload-image 字段')
assertIncludes(executionPage, "componentKind === 'upload-images'", '执行页必须识别 upload-images 字段')
assertIncludes(executionPage, '<UploadFile', '执行页必须渲染普通附件上传控件')
assertIncludes(executionPage, '<UploadImg', '执行页必须渲染单图上传控件')
assertIncludes(executionPage, '<UploadImgs', '执行页必须渲染多图上传控件')
assertIncludes(executionPage, 'isAttachmentComponentKind(field.componentKind)', '附件字段必须有独立判定')
assertIncludes(
  executionPage,
  '!isAttachmentComponentKind(field.componentKind)',
  '附件字段不得进入普通 pendingFieldChanges'
)
assertIncludes(
  executionPage,
  'eDHR 受控附件',
  '执行页必须提示上传结果需要进入受控附件绑定链路'
)
assertIncludes(
  fieldAuditApi,
  'attachmentChanges',
  '前端字段审计保存契约必须声明 attachmentChanges，避免只提交 URL 字符串'
)

console.log('eDHR execution upload field static checks passed')
