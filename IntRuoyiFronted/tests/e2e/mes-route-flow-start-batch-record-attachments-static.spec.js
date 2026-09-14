const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const componentPath = path.join(
  frontendRoot,
  'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
)
const flowConfigApiPath = path.join(frontendRoot, 'src/api/mes/pro/route/flowconfig.ts')
const componentSource = fs.readFileSync(componentPath, 'utf8')
const flowConfigApiSource = fs.readFileSync(flowConfigApiPath, 'utf8')

const assertMatch = (content, pattern, message) => {
  assert.match(content, pattern, message)
}

const assertNotMatch = (content, pattern, message) => {
  assert.doesNotMatch(content, pattern, message)
}

assertMatch(
  flowConfigApiSource,
  /BatchRecordAttachmentOwner[\s\S]*getBatchRecordAttachmentOwners[\s\S]*initBatchRecordAttachmentOwners[\s\S]*saveBatchRecordAttachmentOwners/,
  '前端路线流程配置 API 必须暴露批记录附件负责人读取、初始化和保存接口。'
)
assertMatch(
  componentSource,
  /type BoundaryDetailFieldKey = 'releaseOwner' \| 'batchRecordAttachment'/,
  '边界节点字段 key 必须包含工序开始批记录附件。'
)
assertMatch(
  componentSource,
  /selectedBoundaryType === 'START'[\s\S]*data-flow-boundary-field="batchRecordAttachment"[\s\S]*批记录附件/,
  '批记录附件入口只能挂在工序开始节点左侧。'
)
assertNotMatch(
  componentSource,
  /selectedBoundaryType === 'END'[\s\S]*data-flow-boundary-field="batchRecordAttachment"/,
  '批记录附件不得出现在工序结束节点。'
)
assertMatch(
  componentSource,
  /handleBoundaryNodeSelect[\s\S]*boundaryType === 'START' \? 'batchRecordAttachment'/,
  '点击工序开始节点时必须默认选中批记录附件。'
)
assertMatch(
  componentSource,
  /data-flow-panel="batch-record-attachment-owner-detail"[\s\S]*来料检报告[\s\S]*灭菌报告[\s\S]*成品检报告[\s\S]*成品检记录/,
  '右侧批记录附件负责人配置必须展示 4 个固定记录/报告。'
)
assertMatch(
  componentSource,
  /来料检报告上传1[\s\S]*灭菌报告上传1[\s\S]*成品检报告上传1[\s\S]*成品检记录上传1/,
  '4 个批记录附件必须展示对应默认上传角色名称。'
)
assertMatch(
  componentSource,
  /getSimpleUserList[\s\S]*getSimpleRoleList/,
  '负责人选择必须使用当前租户启用用户和启用角色的精简列表。'
)

console.log('PASS: MES route flow start batch record attachments static contract')
