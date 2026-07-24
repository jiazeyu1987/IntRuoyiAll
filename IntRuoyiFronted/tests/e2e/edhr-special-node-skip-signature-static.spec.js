const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const api = read('src/api/mes/pro/edhr/batchExecution.ts')
const detailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')

const assertIncludes = (source, token, message) => {
  if (!source.includes(token)) throw new Error(message)
}

assertIncludes(api, 'reason: string', '特殊节点跳过 API 请求必须包含 reason。')
assertIncludes(api, 'password: string', '特殊节点跳过 API 请求必须包含 password。')
assertIncludes(detailPage, 'Dialog title="跳过特殊节点"', '跳过特殊节点必须使用正式弹窗。')
assertIncludes(detailPage, 'label="跳过原因"', '跳过特殊节点弹窗必须要求跳过原因。')
assertIncludes(detailPage, 'label="签名密码"', '跳过特殊节点弹窗必须要求签名密码。')
assertIncludes(detailPage, 'label="附件"', '跳过特殊节点弹窗必须承载附件证据。')
assertIncludes(detailPage, 'uploadSpecialNodeSkipAttachment', '跳过特殊节点弹窗必须绑定独立的附件上传处理器。')
assertIncludes(detailPage, 'removeSpecialNodeSkipAttachment', '跳过特殊节点弹窗必须绑定独立的附件删除处理器。')
assertIncludes(detailPage, '签名并跳过', '跳过特殊节点确认按钮必须表达签名语义。')
assertIncludes(detailPage, 'specialNodeSkipForm.reason.trim()', '跳过特殊节点提交前必须校验原因。')
assertIncludes(detailPage, 'specialNodeSkipForm.password.trim()', '跳过特殊节点提交前必须校验签名密码。')
assertIncludes(detailPage, 'attachments: [...specialNodeSkipForm.attachments]', '跳过特殊节点提交必须携带附件摘要。')
assertIncludes(detailPage, 'resolveSpecialNodeEvidenceText', '批次详情必须可读展示跳过审计证据。')
assertIncludes(detailPage, 'skipSignatureId', '批次详情必须展示特殊节点跳过签名证据。')

console.log('PASS edhr-special-node-skip-signature-static')
