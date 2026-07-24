const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const viewMatrixTable = fs.readFileSync(
  path.join(root, 'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixTable.vue'),
  'utf8'
)

assert.ok(
  !viewMatrixTable.includes('label="下载规则"'),
  '查看矩阵总览表不得显示下载规则列'
)

assert.ok(
  !viewMatrixTable.includes('formatDownload(') &&
    !viewMatrixTable.includes('downloadRuleSummary') &&
    !viewMatrixTable.includes('downloadRuleSubjects'),
  '查看矩阵总览表不得继续格式化或展示下载规则'
)

console.log('dcc view matrix hide download column static contract PASS')
