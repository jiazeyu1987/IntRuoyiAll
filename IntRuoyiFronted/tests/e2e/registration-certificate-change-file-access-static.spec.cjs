const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const detailPage = fs.readFileSync(
  path.resolve(__dirname, '..', '..', 'src', 'views', 'dcc', 'registration-certificate', 'detail', 'index.vue'),
  'utf8'
)

const changeHistoryFileBlock = detailPage.match(
  /<div class="change-history__file">[\s\S]*?<\/div>\s*<\/section>/u
)?.[0] || ''

assert.match(changeHistoryFileBlock,
  /item\.fileStatus\s*===\s*'BOUND'\s*&&\s*item\.changeStatus\s*===\s*'APPLIED'\s*&&\s*item\.businessFileId\s*&&\s*item\.originalFileName/u,
  '变更批件必须仅在已绑定且审批生效后显示下载或申请下载操作')
assert.match(changeHistoryFileBlock, /openAttachmentPreview\(item\.businessFileId,\s*item\.originalFileName\)/u,
  '变更批件必须复用注册证文件在线查看')
assert.match(changeHistoryFileBlock, /v-if="canDirectDownload\(item\.businessFileId\)"[\s\S]*?downloadAttachment\(item\.businessFileId\)/u,
  '有直接下载权限时必须可下载变更批件')
assert.match(changeHistoryFileBlock, /v-else[\s\S]*?openDownloadRequest\(item\.businessFileId\)/u,
  '无直接下载权限时必须提供下载申请')
assert.match(changeHistoryFileBlock, /registration-certificate-change-attachment-preview/u,
  '变更批件查看按钮必须提供稳定测试标识')
assert.match(changeHistoryFileBlock, /registration-certificate-change-attachment-download/u,
  '变更批件下载按钮必须提供稳定测试标识')

console.log('注册证变更批件文件访问静态合同通过')
