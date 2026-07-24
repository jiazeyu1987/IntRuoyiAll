const fs = require('fs')
const path = require('path')
const assert = require('assert')
const vm = require('vm')
const ts = require('typescript')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readOptionalSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  return fs.existsSync(absolutePath) ? fs.readFileSync(absolutePath, 'utf8') : ''
}

const matcherPath =
  'src/views/dcc/controlled-file/categories/components/viewMatrixDepartmentMatcher.ts'
const matcherSource = readOptionalSource(matcherPath)
const dialogSource = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixDialog.vue'
)

assert.ok(matcherSource, '查看矩阵必须提供独立的部门初步对应匹配器')
assert.ok(
  matcherSource.includes('CANDIDATE_SCORE_THRESHOLD') &&
    matcherSource.includes('AUTO_APPLY_SCORE_THRESHOLD') &&
    matcherSource.includes('AUTO_APPLY_LEAD_THRESHOLD'),
  '匹配器必须显式定义候选阈值、自动填入阈值和领先阈值'
)
assert.ok(
  dialogSource.includes('runDepartmentAutoMatch') &&
    !dialogSource.includes('data-testid="dcc-view-matrix-dept-auto-match"') &&
    !dialogSource.includes('data-testid="dcc-view-matrix-dept-unmatched"') &&
    !dialogSource.includes('初步对应部门') &&
    !dialogSource.includes('重新初步对应'),
  '查看矩阵维护弹窗必须保留后台自动对应逻辑，但不得继续展示初步对应部门面板'
)
assert.ok(
  !dialogSource.includes('查看矩阵独立决定发布后浏览、详情和已发布预览') &&
    !dialogSource.includes('data-testid="dcc-view-matrix-dialog-preview"') &&
    !dialogSource.includes('刷新预览'),
  '查看矩阵维护弹窗不得继续展示顶部说明条和底部刷新预览按钮'
)

const compiled = ts.transpileModule(matcherSource, {
  compilerOptions: {
    module: ts.ModuleKind.CommonJS,
    target: ts.ScriptTarget.ES2020
  }
}).outputText
const commonjsExports = {}
const sandbox = {
  exports: commonjsExports,
  module: { exports: commonjsExports },
  require,
  console
}
vm.runInNewContext(compiled, sandbox, { filename: matcherPath })
const matcher = sandbox.module.exports

const departments = [
  { id: 1, name: '瑛泰医疗', parentId: 0 },
  { id: 10, name: '质量体系中心', parentId: 1 },
  { id: 11, name: 'QMS', parentId: 10 },
  { id: 20, name: '研发创新中心', parentId: 1 },
  { id: 21, name: '新品开发部', parentId: 20 },
  { id: 30, name: '生产制造中心', parentId: 1 },
  { id: 31, name: '生产部', parentId: 30 }
]

const qms = matcher.matchDepartmentForViewMatrixLabel('QMS', departments)
assert.strictEqual(qms.status, 'AUTO_APPLIED', 'QMS 必须高置信自动对应')
assert.strictEqual(qms.departmentId, 11, 'QMS 必须命中质量体系中心下的 QMS 部门')
assert.strictEqual(qms.departmentPath, '瑛泰医疗-质量体系中心-QMS')
assert.ok(qms.score >= 85, 'QMS 自动对应相似度必须达到高分阈值')

const newProduct = matcher.matchDepartmentForViewMatrixLabel('新品开发部', departments)
assert.strictEqual(newProduct.status, 'AUTO_APPLIED', '新品开发部必须高置信自动对应')
assert.strictEqual(newProduct.departmentId, 21, '新品开发部必须命中同名部门')

const docControl = matcher.matchDepartmentForViewMatrixLabel('文控', departments)
assert.strictEqual(docControl.status, 'NO_SIMILARITY', '文控无相似部门时必须单独列出')
assert.strictEqual(docControl.candidates.length, 0, '无相似部门不得给出噪声候选')

const companyOnly = matcher.matchDepartmentForViewMatrixLabel('瑛泰医疗', departments)
assert.strictEqual(companyOnly.status, 'ROOT_ONLY', '只命中公司根节点时不得自动选择公司')
assert.strictEqual(companyOnly.departmentId, undefined, '公司根节点不可作为对应部门写入')

const rules = [
  { subjectLabel: '新品开发部', subjectType: 'ROLE', subjectId: 901226, active: true },
  { subjectLabel: '新品开发部', subjectType: undefined, subjectId: undefined, active: true },
  { subjectLabel: '文控', subjectType: 'UNMAPPED_EXCEL', subjectId: undefined, active: true }
]
const result = matcher.applyDepartmentAutoMatchToViewMatrixRules(rules, departments)
assert.strictEqual(result.summary.autoApplied, 2, '同名高分规则必须同步自动填入')
assert.strictEqual(result.summary.noSimilarity, 1, '无相似名称必须计数')
assert.strictEqual(result.rules[0].subjectType, 'DEPT')
assert.strictEqual(result.rules[0].subjectId, 21)
assert.strictEqual(result.rules[1].subjectType, 'DEPT')
assert.strictEqual(result.rules[1].subjectId, 21)
assert.strictEqual(result.rules[2].subjectType, 'UNMAPPED_EXCEL', '无相似规则不得被自动覆盖')

const existingMapped = matcher.applyDepartmentAutoMatchToViewMatrixRules(
  [{ subjectLabel: 'QMS', subjectType: 'DEPT', subjectId: 11, remark: '', active: true }],
  departments
)
assert.strictEqual(existingMapped.matches[0].status, 'EXISTING_DEPT', '已有有效部门映射必须进入初步对应结果')
assert.strictEqual(existingMapped.matches[0].departmentPath, '瑛泰医疗-质量体系中心-QMS')
assert.strictEqual(existingMapped.summary.autoApplied, 1, '已有有效部门映射也必须计入已对应统计')
assert.match(existingMapped.matches[0].reason, /不依赖备注/, '初步对应说明必须明确不依赖备注字段')

console.log('dcc view matrix department auto match PASS')
