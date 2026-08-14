const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(
  repoRoot,
  'IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)
const apiPath = path.join(
  repoRoot,
  'IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts'
)

assert(fs.existsSync(pagePath), '生产组长工作台页面必须存在。')
assert(fs.existsSync(apiPath), '生产组长报工 API 包装必须存在。')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert.match(
  pageSource,
  /<el-tab-pane\s+label="报工管理"\s+name="report"\s+data-production-leader-module-tab-report\s*\/>/,
  '生产组长工作台必须保留“报工管理”页签。'
)
assert.match(
  pageSource,
  /<el-table[\s\S]{0,260}:data="submissionList"/,
  '报工管理表格必须绑定正式接口返回的 submissionList。'
)
assert.match(
  pageSource,
  /const data = await getTeamLeaderSubmissionPage\(buildSubmissionParams\(\)\)[\s\S]{0,120}submissionList\.value = data\.list \|\| \[\]/,
  '报工管理列表必须直接使用正式分页接口返回顺序，不能改成本地分页后再排序。'
)
assert.match(
  pageSource,
  /allocationView:\s*isProductionLeader\.value[\s\S]{0,120}\?\s*isProductionReportHistoryTab\.value[\s\S]{0,80}\?\s*'HISTORY'[\s\S]{0,80}:\s*'WORKBENCH'/,
  '报工管理页签请求必须继续发送 WORKBENCH 视图，报工历史才发送 HISTORY。'
)
assert.doesNotMatch(
  pageSource,
  /submissionList\.value\s*=\s*\(?data\.list\s*\|\|\s*\[\]\)?\s*\.sort\(/,
  '报工管理不得在前端对当前页做本地 sort；倒序必须由后端分页 SQL 保证。'
)
assert.match(
  apiSource,
  /export const getTeamLeaderSubmissionPage = async \(params: TeamLeaderSubmissionPageReqVO\)[\s\S]{0,220}url:\s*'\/mes\/pro\/process-pool\/team-leader\/submission\/page'[\s\S]{0,80}params/,
  '前端必须继续调用生产组长报工正式分页接口。'
)

console.log('PASS team-leader-submission-desc-sort-static')
