const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const assertIncludes = (content, expected, message) => {
  if (!content.includes(expected)) throw new Error(message)
}

const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')

assertIncludes(
  executionPage,
  'type PendingAttachmentChange',
  '执行页必须声明待保存附件变更类型'
)
assertIncludes(
  executionPage,
  'pendingAttachmentChanges',
  '执行页必须计算待保存附件变更'
)
assertIncludes(
  executionPage,
  'buildAttachmentChangeRequest',
  '执行页必须把附件变更转换为 attachmentChanges 请求'
)
assertIncludes(
  executionPage,
  'attachmentChanges: pendingAttachmentChanges.value.map(buildAttachmentChangeRequest)',
  '字段审计保存请求必须提交 attachmentChanges'
)
assertIncludes(
  executionPage,
  'workTaskId: workTaskId.value',
  '附件保存请求必须携带当前真实 workTaskId'
)
assertIncludes(
  executionPage,
  'attachmentType: resolveAttachmentType(field.componentKind)',
  '附件保存请求必须根据上传控件提交 attachmentType'
)
assertIncludes(
  executionPage,
  'hasPendingFieldAuditChanges',
  '保存门禁必须同时考虑普通字段变更和附件变更'
)
assertIncludes(
  executionPage,
  '待保存附件',
  '页面必须展示待保存附件摘要'
)
assertIncludes(
  executionPage,
  '附件元数据不完整',
  '附件变更缺少正式元数据时必须暴露真实错误'
)
assertIncludes(
  executionPage,
  'resolveAttachmentDraftUrls',
  '执行页必须从上传组件结果提取附件 URL 列表'
)

console.log('eDHR execution attachment save static checks passed')
