const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/infra/runtime-control/index.vue'),
  'utf8'
)

const failures = []

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    failures.push(`missing ${label}: ${expected}`)
  }
}

function assertNotContains(source, forbidden, label) {
  if (source.includes(forbidden)) {
    failures.push(`forbidden ${label}: ${forbidden}`)
  }
}

assertContains(
  page,
  'loadFoolproofData().catch',
  'isolated foolproof data load error handler'
)
assertContains(page, '运维矩阵：', 'runtime matrix top-level error context')
assertNotContains(page, '傻瓜式运维：', 'foolproof top-level error context')
assertContains(page, 'let foolproofLoadFailed = false', 'foolproof failure state initializer')
assertContains(page, 'foolproofLoadFailed = true', 'foolproof failure state marker')
assertContains(
  page,
  'connected.value = errors.length === 0 && !foolproofLoadFailed',
  'foolproof failure affects connection status'
)

if (failures.length) {
  throw new Error(
    `runtime-control foolproof top-level error contract is not satisfied:\n- ${failures.join('\n- ')}`
  )
}

console.log('PASS: runtime-control hides foolproof top-level timeout errors')
