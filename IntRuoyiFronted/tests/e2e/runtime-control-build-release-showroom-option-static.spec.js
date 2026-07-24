const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8')
}

function assertIncludes(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`missing ${label}: ${expected}`)
  }
}

const viewSource = read('src/views/infra/runtime-control/index.vue')
const apiSource = read('src/api/infra/runtimeControl/index.ts')

assertIncludes(apiSource, 'includeShowroomBuildPackage?: boolean', 'request contract field')
assertIncludes(apiSource, 'component?: string', 'release package component field')
assertIncludes(apiSource, 'includeShowroomBuildPackage?: boolean', 'release package showroom field')
assertIncludes(viewSource, 'includeShowroomBuildPackage: false', 'default unchecked state')
assertIncludes(viewSource, '发布展厅构筑包', 'showroom checkbox label')
assertIncludes(
  viewSource,
  '当前选中的展厅构筑包会覆盖服务器的展厅数据，是否继续？',
  'showroom overwrite confirmation'
)
assertIncludes(viewSource, 'handleShowroomBuildPackageChange', 'checkbox change handler')
assertIncludes(
  viewSource,
  'operationDialog.includeShowroomBuildPackage',
  'request payload source'
)

console.log('PASS: runtime-control build-release showroom option static contract')
