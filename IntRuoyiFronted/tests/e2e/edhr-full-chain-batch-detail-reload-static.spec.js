const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const scriptPath = path.resolve(__dirname, 'edhr-full-chain-multi-user-real-flow.e2e.js')
const source = fs.readFileSync(scriptPath, 'utf8')

assert.ok(
  source.includes('isSameBatchDetailPage'),
  '完整链路脚本必须识别当前已在同一批次详情页，避免同路由 goto 不触发详情接口。'
)

assert.ok(
  /if \(isSameBatchDetailPage\(page, batchId\)\)[\s\S]*page\.reload\(\{ waitUntil: 'domcontentloaded', timeout: 60000 \}\)/.test(source),
  '同一批次详情页再次读取详情时必须通过 reload 触发真实页面重新请求，而不是等待不会发生的响应。'
)

assert.ok(
  source.includes('waitForApiResponse(') && source.includes('ENDPOINTS.batchGet'),
  '批次详情读取仍必须等待真实前端发出的批次详情接口响应。'
)

console.log('PASS: eDHR full-chain batch detail reload static contract')
