const fs = require('fs')
const path = require('path')
const assert = require('assert')
const vm = require('vm')
const ts = require('typescript')

const root = path.resolve(__dirname, '../..')
const matcherPath =
  'src/views/dcc/controlled-file/categories/components/viewMatrixDepartmentMatcher.ts'
const matcherSource = fs.readFileSync(path.join(root, matcherPath), 'utf8')

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
  { id: 21, name: '新品开发部', parentId: 20 }
]

assert.strictEqual(
  matcher.resolveViewMatrixDepartmentRecognitionStatus(
    [
      { subjectLabel: 'QMS', subjectType: 'DEPT', subjectId: 11, active: true },
      { subjectLabel: '新品开发部', subjectType: 'ROLE', subjectId: 901226, active: true }
    ],
    departments
  ),
  'recognized-all',
  '总览必须把已有部门和可自动对应部门都计为已识别，DCC_FVM_DHF_002 应显示绿色'
)

assert.strictEqual(
  matcher.resolveViewMatrixDepartmentRecognitionStatus(
    [
      { subjectLabel: 'QMS', subjectType: 'DEPT', subjectId: 11, active: true },
      { subjectLabel: '文控', subjectType: 'UNMAPPED_EXCEL', active: true }
    ],
    departments
  ),
  'recognized-partial',
  '仍有主体标签无法对应部门时，总览必须保持黄色'
)

assert.strictEqual(
  matcher.resolveViewMatrixDepartmentRecognitionStatus(
    [
      { subjectLabel: '', subjectType: 'UNMAPPED_EXCEL', active: true },
      { subjectLabel: '文控', subjectType: 'UNMAPPED_EXCEL', active: true }
    ],
    departments
  ),
  'recognized-none',
  '主体标签为空时，总览必须显示红色'
)

console.log('dcc view matrix category name auto match status PASS')
