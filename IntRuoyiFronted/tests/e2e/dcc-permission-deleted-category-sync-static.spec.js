const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const categoriesPage = readSource('src/views/dcc/controlled-file/categories/index.vue')
const reviewMatrixPage = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryReviewMatrixTable.vue'
)
const viewMatrixPage = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixTable.vue'
)
const directoryAuthorizationPage = readSource(
  'src/views/dcc/controlled-file/components/DirectoryAuthorizationTabPanel.vue'
)

assert.equal(
  packageJson.scripts?.['e2e:dcc:permission-deleted-category-sync:static'],
  'node tests/e2e/dcc-permission-deleted-category-sync-static.spec.js',
  'package.json 必须暴露文控权限删除类别同步静态合同'
)

assert.match(
  categoriesPage,
  /const categoryRevision = ref\(0\)/,
  '类别列表必须维护类别数据修订号，用于同步关联页签。'
)
assert.match(
  categoriesPage,
  /categories\.value = categoryList[\s\S]*categoryRevision\.value \+= 1/,
  '类别列表刷新后必须递增类别数据修订号。'
)

for (const [label, source, tabName, componentName] of [
  ['审阅矩阵', categoriesPage, 'review-matrix', 'CategoryReviewMatrixTable'],
  ['查看矩阵', categoriesPage, 'view-matrix', 'CategoryViewMatrixTable'],
  ['目录授权', categoriesPage, 'directory-auth', 'DirectoryAuthorizationTabPanel']
]) {
  assert.match(
    source,
    new RegExp(
      `<${componentName}[\\s\\S]*?:active="activeTab === '${tabName}'"[\\s\\S]*?:category-revision="categoryRevision"[\\s\\S]*?/?>`
    ),
    `${label}页签必须接收类别列表修订号和当前激活状态。`
  )
}

for (const [label, source, reloadMethod] of [
  ['审阅矩阵', reviewMatrixPage, 'loadRows'],
  ['查看矩阵', viewMatrixPage, 'loadRows'],
  ['目录授权', directoryAuthorizationPage, 'loadDirectoryAuthorizationData']
]) {
  assert.match(
    source,
    /categoryRevision\?: number/,
    `${label}必须声明 categoryRevision 入参。`
  )
  assert.match(source, /active\?: boolean/, `${label}必须声明 active 入参。`)
  assert.match(
    source,
    new RegExp(
      `watch\\([\\s\\S]*props\\.active[\\s\\S]*props\\.categoryRevision[\\s\\S]*if \\(!active\\)[\\s\\S]*return[\\s\\S]*await ${reloadMethod}\\(\\)`
    ),
    `${label}必须在页签激活或类别修订变化后重新加载，避免已删除类别行残留。`
  )
}

console.log('PASS: DCC permission deleted categories sync to related tabs')
