const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')
const assert = require('node:assert/strict')

const vuePath = resolve(__dirname, '../../src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const source = readFileSync(vuePath, 'utf8')

assert(
  !source.includes('工序任务索引'),
  '工序复盘左侧不应再渲染“工序任务索引”标题'
)

assert(
  !source.includes('edhr-batch-detail__task-index-'),
  '工序复盘左侧不应保留 task-index 专属 DOM 或样式类'
)

assert(
  source.includes('已填写表单'),
  '工序复盘左侧应保留“已填写表单”入口'
)

assert(
  source.includes('工序证据链'),
  '工序复盘右侧应保留工序证据链入口'
)

assert(
  source.includes('aria-label="已填写批记录"'),
  '工序复盘左侧导航语义应聚焦已填写批记录'
)

console.log('PASS mes-edhr-batch-review-remove-task-index-static')
