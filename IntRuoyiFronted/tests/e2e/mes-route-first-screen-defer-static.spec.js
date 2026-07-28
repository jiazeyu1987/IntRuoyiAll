const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const readSource = (...segments) => fs.readFileSync(path.join(repoRoot, ...segments), 'utf8')

const routeIndex = readSource('src', 'views', 'mes', 'pro', 'route', 'index.vue')
const routeForm = readSource('src', 'views', 'mes', 'pro', 'route', 'RouteForm.vue')
const routeFormContent = readSource('src', 'views', 'mes', 'pro', 'route', 'RouteFormContent.vue')

assert.match(
  routeIndex,
  /const\s+RouteForm\s*=\s*defineAsyncComponent\(\s*\(\)\s*=>\s*import\('\.\/RouteForm\.vue'\)\s*\)/,
  '工艺流程列表首屏必须按需加载新增/详情弹窗，避免同步拉入表单与关系图设计器。'
)
assert.match(
  routeIndex,
  /const\s+RouteWorkbookExcelImportForm\s*=\s*defineAsyncComponent\(\s*\(\)\s*=>\s*import\('\.\/RouteWorkbookExcelImportForm\.vue'\)\s*\)/,
  '工艺流程列表首屏必须按需加载 Excel 导入弹窗。'
)
assert.doesNotMatch(
  routeIndex,
  /import\s+Route(Form|WorkbookExcelImportForm)\s+from\s+['"]\.\/Route(Form|WorkbookExcelImportForm)\.vue['"]/,
  '工艺流程列表不得静态导入隐藏弹窗组件。'
)

assert.match(
  routeForm,
  /const\s+RouteFormContent\s*=\s*defineAsyncComponent\(\s*\(\)\s*=>\s*import\('\.\/RouteFormContent\.vue'\)\s*\)/,
  '工艺路线弹窗壳必须按需加载表单内容，避免列表首屏同步拉入完整表单链路。'
)
assert.doesNotMatch(
  routeForm,
  /import\s+RouteFormContent\s+from\s+['"]\.\/RouteFormContent\.vue['"]/,
  '工艺路线弹窗壳不得静态导入表单内容。'
)

for (const [componentName, fileName] of [
  ['RouteFlowGraphDesigner', 'RouteFlowGraphDesigner.vue'],
  ['RouteProductList', 'RouteProductList.vue']
]) {
  assert.match(
    routeFormContent,
    new RegExp(
      `const\\s+${componentName}\\s*=\\s*defineAsyncComponent\\(\\s*\\(\\)\\s*=>\\s*import\\('\\./${fileName}'\\)\\s*\\)`
    ),
    `工艺路线表单页签组件 ${componentName} 必须按需加载。`
  )
  assert.doesNotMatch(
    routeFormContent,
    new RegExp(`import\\s+${componentName}\\s+from\\s+['"]\\./${fileName}['"]`),
    `工艺路线表单不得静态导入 ${componentName}。`
  )
}

console.log('PASS: MES route first screen defers hidden route dialogs and heavy tab components.')
