const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const boardPage = readSource('src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue')

const extractFunctionBody = (source, signature) => {
  const start = source.indexOf(signature)
  assert.ok(start >= 0, `missing function signature: ${signature}`)
  const bodyStart = source.indexOf('{', start)
  assert.ok(bodyStart >= 0, `missing function body: ${signature}`)
  let depth = 0
  for (let index = bodyStart; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) return source.slice(bodyStart, index + 1)
    }
  }
  throw new Error(`unterminated function body: ${signature}`)
}

const openTaskBody = extractFunctionBody(boardPage, 'const openTask = async (row: EdhrWorkTaskRespVO) =>')

assert.match(
  boardPage,
  /import\s*\{[\s\S]*navigateToEdhrWorkTask[\s\S]*\}\s*from\s*['"]@\/utils\/edhrWorkTaskNavigation['"]/,
  '工作任务看板必须复用统一 eDHR 工作任务导航工具。'
)

assert.doesNotMatch(
  boardPage,
  /import\s*\{\s*openEdhrBatchTask\s*\}\s*from\s*['"]@\/api\/mes\/pro\/edhr\/batchExecution['"]/,
  '工作任务看板不得保留本地 openEdhrBatchTask 打开分支。'
)

assert.doesNotMatch(
  boardPage,
  /const openFillWorkspaceTask\s*=/,
  '工作任务看板填写/返工任务不得维护第二套本地打开逻辑。'
)

assert.match(
  openTaskBody,
  /isFillWorkspaceTask\(row\)[\s\S]*navigateToEdhrWorkTask\(router,\s*row\)/,
  '工作任务看板填写/返工任务点击必须直接委托 navigateToEdhrWorkTask。'
)

console.log('PASS: eDHR work task board uses unified navigation')
