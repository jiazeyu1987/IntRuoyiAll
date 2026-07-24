const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const notifyNavigation = readSource('src/utils/notifyMessageNavigation.ts')
const workTaskNavigation = readSource('src/utils/edhrWorkTaskNavigation.ts')

assert.match(
  workTaskNavigation,
  /export const EDHR_WORK_TASK_NOTIFY_PATHS\s*=\s*new Set/,
  'eDHR 工作任务路径白名单必须由统一导航工具导出。'
)

assert.match(
  notifyNavigation,
  /import\s*\{[\s\S]*EDHR_WORK_TASK_NOTIFY_PATHS[\s\S]*navigateToEdhrWorkTask[\s\S]*\}\s*from\s*['"]@\/utils\/edhrWorkTaskNavigation['"]/,
  '站内信导航必须从统一导航工具导入 eDHR 工作任务路径白名单和跳转函数。'
)

assert.doesNotMatch(
  notifyNavigation,
  /export const EDHR_WORK_TASK_NOTIFY_PATHS\s*=\s*new Set/,
  '站内信导航不得重复定义 eDHR 工作任务路径白名单。'
)

assert.doesNotMatch(
  notifyNavigation,
  /EDHR_BATCH_EXECUTION_DETAIL_PATH[\s\S]*EDHR_EXECUTION_DETAIL_PATH[\s\S]*EDHR_APPROVAL_DETAIL_PATH[\s\S]*new Set/,
  '站内信导航不得维护第二套路由集合，避免改一漏一。'
)

console.log('PASS: eDHR notify navigation uses a single source of path truth')
