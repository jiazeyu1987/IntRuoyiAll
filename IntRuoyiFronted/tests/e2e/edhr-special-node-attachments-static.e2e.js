const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const batchExecutionApi = read('src/api/mes/pro/edhr/batchExecution.ts')
const batchExecutionDetail = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const templateApi = read('src/api/mes/pro/batchrecordreport/index.ts')
const templatePage = read('src/views/mes/pro/batchrecordformlist/index.vue')

assert(
  batchExecutionApi.includes('prepareEdhrBatchSpecialNodeAttachmentUpload'),
  '特殊节点必须提供附件预上传 API 调用。'
)
assert(
  batchExecutionApi.includes('attachments?: EdhrBatchSpecialNodeAttachment[]'),
  '特殊节点完成/跳过请求必须携带附件元数据。'
)
assert(
  batchExecutionDetail.includes('<el-upload') &&
    batchExecutionDetail.includes('specialNodeCompleteForm.attachments'),
  '批次详情页特殊节点完成弹窗必须提供附件上传入口。'
)
assert(
  batchExecutionDetail.includes(':http-request="uploadSpecialNodeSkipAttachment"') &&
    batchExecutionDetail.includes(':on-remove="removeSpecialNodeSkipAttachment"'),
  '特殊节点跳过弹窗必须绑定独立的附件上传与删除处理器。'
)
assert(
  batchExecutionDetail.includes(':http-request="uploadSpecialNodeCompleteAttachment"') &&
    batchExecutionDetail.includes(':on-remove="removeSpecialNodeCompleteAttachment"'),
  '特殊节点完成弹窗必须绑定独立的附件上传与删除处理器。'
)
assert.strictEqual(
  (batchExecutionDetail.match(/const uploadSpecialNodeSkipAttachment = async/g) || []).length,
  1,
  '特殊节点跳过附件上传处理器必须且只能声明一次。'
)
assert.strictEqual(
  (batchExecutionDetail.match(/const uploadSpecialNodeCompleteAttachment = async/g) || []).length,
  1,
  '特殊节点完成附件上传处理器必须且只能声明一次。'
)
assert.strictEqual(
  (batchExecutionDetail.match(/const removeSpecialNodeSkipAttachment = \(file: UploadUserFile\) =>/g) || []).length,
  1,
  '特殊节点跳过附件删除处理器必须且只能声明一次。'
)
assert.strictEqual(
  (batchExecutionDetail.match(/const removeSpecialNodeCompleteAttachment = \(file: UploadUserFile\) =>/g) || []).length,
  1,
  '特殊节点完成附件删除处理器必须且只能声明一次。'
)
assert.strictEqual(
  (batchExecutionDetail.match(/const uploadSpecialNodeAttachment = async/g) || []).length,
  0,
  '批次详情页不得保留重复的通用特殊节点附件上传处理器声明。'
)
assert.strictEqual(
  (batchExecutionDetail.match(/const removeSpecialNodeAttachment = \(file: UploadUserFile\) =>/g) || []).length,
  0,
  '批次详情页不得保留重复的通用特殊节点附件删除处理器声明。'
)
assert(
  batchExecutionApi.includes('reason: string') && batchExecutionApi.includes('password: string'),
  '特殊节点跳过请求必须携带跳过原因和签名密码。'
)
assert(
  batchExecutionDetail.includes('跳过特殊节点') &&
    batchExecutionDetail.includes('specialNodeSkipForm.reason') &&
    batchExecutionDetail.includes('specialNodeSkipForm.password') &&
    batchExecutionDetail.includes('submitSpecialNodeSkip'),
  '批次详情页跳过特殊节点必须打开原因和签名密码弹窗。'
)
assert(
  batchExecutionDetail.includes('attachments: [...specialNodeSkipForm.attachments]'),
  '批次详情页跳过特殊节点时必须把已上传附件一起提交。'
)
assert(
  templateApi.includes("'DEPT'") && templateApi.includes("'DEPTS'"),
  '批记录模板签名来源类型必须声明部门和多部门。'
)
assert(
  templatePage.includes("{ label: '部门', value: 'DEPT' }") &&
    templatePage.includes("{ label: '多部门', value: 'DEPTS' }") &&
    templatePage.includes('getSimpleDeptList'),
  '批记录模板签名位配置必须提供部门组选择。'
)
