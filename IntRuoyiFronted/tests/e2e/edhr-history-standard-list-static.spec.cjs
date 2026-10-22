const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..', '..')

const read = (...segments) => fs.readFileSync(path.join(repoRoot, ...segments), 'utf8')

const historyPage = read('IntRuoyiFronted', 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchRecordHistoryPage.vue')
const apiFile = read('IntRuoyiFronted', 'src', 'api', 'mes', 'pro', 'edhr', 'batchExecution.ts')
const reqVo = read(
  'IntRuoyiBackend',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'controller',
  'admin',
  'pro',
  'batchrecord',
  'vo',
  'EdhrBatchExecutionPageReqVO.java'
)
const respVo = read(
  'IntRuoyiBackend',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'controller',
  'admin',
  'pro',
  'batchrecord',
  'vo',
  'EdhrBatchExecutionRespVO.java'
)
const mapper = read(
  'IntRuoyiBackend',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'dal',
  'mysql',
  'pro',
  'batchrecord',
  'MesProEdhrBatchExecutionMapper.java'
)

assert(historyPage.includes('data-edhr-history-standard-list'), '历史追溯页必须声明标准列表根标识')
assert(historyPage.includes('releasedOnly: true'), '历史追溯页必须请求 releasedOnly=true')
assert(historyPage.includes('releaseApprovedTime'), '历史追溯页必须发送放行时间过滤参数')
assert(historyPage.includes('data-edhr-history-detail-action'), '历史追溯列表必须提供行内详情按钮')
assert(historyPage.includes('暂无已放行批次'), '历史追溯标准列表必须显示已放行批次空态')

const removedWorkbenchTokens = [
  'getEdhrBatchReviewTimeline',
  'getLatestEdhrBatchArchive',
  'printEdhrBatchArchive',
  'ActiveOrderSubmissionDetailPanel',
  'EdhrExecutionReadonlyForm',
  '统一时间线',
  '归档目录',
  '正式批记录',
  '详情批记录',
  '打印'
]
for (const token of removedWorkbenchTokens) {
  assert(!historyPage.includes(token), `历史追溯页不得再包含旧工作台内容: ${token}`)
}

assert(apiFile.includes('productName?: string'), '前端分页请求类型必须支持产品名称过滤')
assert(apiFile.includes('releasedOnly?: boolean'), '前端分页请求类型必须支持 releasedOnly')
assert(apiFile.includes('releaseApprovedTime?: string[]'), '前端分页请求类型必须支持放行时间范围')
assert(apiFile.includes('releaseTransactionId?: number'), '前端分页响应类型必须包含放行事务 ID')
assert(apiFile.includes('releaseStatus?: string'), '前端分页响应类型必须包含放行状态')
assert(apiFile.includes('releaseApprovedAt?: string'), '前端分页响应类型必须包含放行时间')

assert(reqVo.includes('private String productName;'), '后端分页请求必须支持产品名称过滤')
assert(reqVo.includes('private Boolean releasedOnly;'), '后端分页请求必须支持 releasedOnly')
assert(reqVo.includes('private LocalDateTime[] releaseApprovedTime;'), '后端分页请求必须支持放行时间范围')
assert(respVo.includes('private Long releaseTransactionId;'), '后端分页响应必须返回放行事务 ID')
assert(respVo.includes('private String releaseStatus;'), '后端分页响应必须返回放行状态')
assert(respVo.includes('private LocalDateTime releaseApprovedAt;'), '后端分页响应必须返回放行时间')
assert(mapper.includes('Boolean.TRUE.equals(reqVO.getReleasedOnly())'), 'Mapper 必须按 releasedOnly 过滤')
assert(mapper.includes('releasedTransactionExistsSql(reqVO.getReleaseApprovedTime())'), 'Mapper 必须把放行时间范围绑定到正式 RELEASED 事务')
assert(mapper.includes('MesProEdhrBatchExecutionDO::getProductName'), 'Mapper 必须按产品名称过滤')

console.log('edhr-history-standard-list-static: PASS')
