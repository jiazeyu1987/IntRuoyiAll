const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const reviewCopyApiPath = 'IntRuoyiFronted/src/api/mes/pro/processpool/reviewCopy.ts'
const timelineApiPath = 'IntRuoyiFronted/src/api/mes/pro/processpool/index.ts'
const timelinePagePath = 'IntRuoyiFronted/src/views/mes/pro/processpool/TimelinePage.vue'

assert(fs.existsSync(path.join(repoRoot, reviewCopyApiPath)), 'F5 审核副本前端 API wrapper 必须独立存在。')

const reviewCopyApi = read(reviewCopyApiPath)
const timelineApi = read(timelineApiPath)
const timelinePage = read(timelinePagePath)

assert(reviewCopyApi.includes("import request from '@/config/axios'"), '审核副本 API 必须使用正式 axios wrapper。')
assert(reviewCopyApi.includes('ProcessPoolReviewCopyGenerateSubmitReqVO'), '审核副本 API 必须导出提交请求 VO。')
assert(reviewCopyApi.includes('ProcessPoolReviewCopyFieldMappingVO'), '审核副本 API 必须导出字段映射 VO。')
assert(reviewCopyApi.includes('generateSubmitProcessPoolReviewCopy'), '审核副本 API 必须导出稳定写方法。')
assert(reviewCopyApi.includes("url: '/mes/pro/process-pool/review-copy/generate-submit'"),
  '审核副本 API URL 必须稳定。')
assert(/request\.post\s*<\s*number\s*>\s*\(/.test(reviewCopyApi), '审核副本生成提交必须是 POST number 响应。')

for (const field of [
  'eventId',
  'reviewerUserId',
  'reviewerSignatureId',
  'reviewerSignatureUserId',
  'reviewerSignatureSnapshot',
  'fieldMappings',
  'fieldCode',
  'fieldName',
  'lowerLimit',
  'upperLimit',
  'sourceQuantityFragmentId',
  'affectsAllocation'
]) {
  assert(reviewCopyApi.includes(field), `审核副本 API 请求契约必须包含 ${field}。`)
}

assert(!reviewCopyApi.includes('/timeline/'), '审核副本写 API 不得复用时间轴接口。')
assert(!/request\.(get|put|delete|upload|download)\(/.test(reviewCopyApi), '审核副本 wrapper 只允许暴露 F5 POST 写请求。')
assert(!/request\.(post|put|delete|upload|download)\(/.test(timelineApi), '时间轴 API 模块必须继续保持只读。')
assert(!/@click="[^"]*(create|update|submit|allocate|generate|fifo|auditCopy)/i.test(timelinePage),
  '时间轴页面不得新增审核副本写入口。')

console.log('PASS process-pool-review-copy-api-static')
