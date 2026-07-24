const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/feedback/index.vue')

assert(fs.existsSync(pagePath), '报工列表页面必须存在。')

const pageSource = fs.readFileSync(pagePath, 'utf8')

assert.match(
  pageSource,
  /const feedbackFlexibleColumnKey = computed[\s\S]*excelFeedbackTime/,
  '报工列表必须定义可伸缩列，优先让日期列参与铺满右侧区域。'
)

assert.match(
  pageSource,
  /const getFeedbackColumnLayoutWidthString = [\s\S]*feedbackFlexibleColumnKey\.value[\s\S]*return undefined[\s\S]*getFeedbackColumnWidthString/,
  '报工列表宽度策略必须让伸缩列不绑定固定 width，同时保留其他列的用户列宽配置。'
)

assert.match(
  pageSource,
  /label="日期"[\s\S]*:width="getFeedbackColumnLayoutWidthString\('excelFeedbackTime', 180\)"[\s\S]*:min-width="getFeedbackColumnMinWidthString\('excelFeedbackTime', 180\)"/,
  '日期列必须作为伸缩列使用 min-width 撑开，避免表格右侧留出大块空白。'
)

assert.match(
  pageSource,
  /<el-table[\s\S]*class="feedback-main-table"[\s\S]*data-user-table-key="mes\.pro\.feedback\.main"[\s\S]*>/,
  '报工列表主表必须保留专属样式类和稳定表格 key，确保铺满样式只作用于本列表。'
)

assert.match(
  pageSource,
  /\.feedback-main-table[\s\S]*width: 100%;[\s\S]*\.feedback-main-table :deep\(\.el-table__inner-wrapper\)[\s\S]*width: 100%;/,
  '报工列表主表及 Element Plus 内部容器必须撑满父容器。'
)

console.log('PASS: MES feedback list fill width static contract')
