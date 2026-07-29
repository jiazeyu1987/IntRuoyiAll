const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const executionPage = fs
  .readFileSync(path.join(repoRoot, 'src/views/mes/pro/edhr/ExecutionPage.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

assert.match(
  executionPage,
  /type\s+AssistGridSubjectType\s*=\s*'USERS'\s*\|\s*'ROLE'/,
  '辅助表格 rowKey 必须区分个人和角色责任主体。'
)
assert.match(
  executionPage,
  /const\s+parseAssistGridRowKey[\s\S]*ASSIST_GRID_\(USERS\|ROLE\)[\s\S]*ASSIST_GRID_U/,
  '填写页必须同时解析正式 USERS/ROLE 和旧版 U 辅助表格 rowKey。'
)
assert.match(
  executionPage,
  /const\s+rawActiveAssistRows\s*=\s*computed/,
  '填写页必须先保留未经填写人筛选的原始 assistRows，供范围合同校验。'
)
assert.match(
  executionPage,
  /const\s+resolveAssistRowsForCurrentFiller[\s\S]*rowsAreServerScoped[\s\S]*selectedUserId/,
  '填写页必须在字段构造前按当前填写人解析正式 assistRows 范围。'
)
assert.match(
  executionPage,
  /gridKey\.subjectType\s*===\s*'USERS'[\s\S]*gridKey\.subjectId\s*===\s*selectedUserId/,
  '未过滤快照中的个人辅助表格必须只保留当前填写人的责任主体。'
)
assert.match(
  executionPage,
  /角色辅助表格[\s\S]*缺少当前填写人的正式责任范围/,
  '预览快照无法解析角色责任主体时必须 fail fast，不能猜测或合并角色网格。'
)
assert.match(
  executionPage,
  /const\s+activeAssistRowsState\s*=\s*computed[\s\S]*visibleAssistRowsFromExecutionQueryState\.value\.present/,
  '填写页必须区分后端已过滤的 executionPageQuery 与未过滤的运行快照。'
)
assert.match(
  executionPage,
  /const\s+assistGridPositionConflictError\s*=\s*computed[\s\S]*同时映射了不同字段/,
  '筛选后的同一辅助网格位置若仍有不同字段，必须明确报告配置冲突。'
)
assert.match(
  executionPage,
  /const\s+assistSourceFields\s*=\s*computed[\s\S]*assistGridContractError\.value[\s\S]*return\s+\[\]/,
  '辅助网格合同错误时必须停止构造渲染字段，避免错误 DOM 继续叠加。'
)
assert.match(
  executionPage,
  /configuredGridPositionFieldMap[\s\S]*existingFieldIdentity\s*===\s*field\.fieldIdentity[\s\S]*return/,
  '同一正式字段被重复引用到同一辅助格时必须只构造一次。'
)
assert.doesNotMatch(
  executionPage,
  /\.edhr-fill-workspace__assist-row[\s\S]{0,240}z-index\s*:/,
  '输入框叠加不得通过 z-index 遮挡修复。'
)

console.log('PASS: eDHR assist grid current filler isolation static contract')
