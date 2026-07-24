const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const scriptPath = path.resolve(__dirname, 'edhr-full-chain-multi-user-real-flow.e2e.js')
const source = fs.readFileSync(scriptPath, 'utf8')

assert.ok(
  source.includes("fillLabeledTextarea(dialog, '审批意见'"),
  '审批通过脚本必须按“审批意见”标签填写意见，不能依赖 textarea 顺序。'
)

assert.ok(
  !/approveExecution[\s\S]*dialog\.locator\('textarea'\)\.last\(\)/.test(source),
  '审批通过脚本不得使用最后一个 textarea，避免误填“时间原因”。'
)

console.log('PASS edhr-approval-comment-target-static')
