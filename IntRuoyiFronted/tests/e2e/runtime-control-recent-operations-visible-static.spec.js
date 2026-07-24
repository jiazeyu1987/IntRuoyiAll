const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function compact(source) {
  return source.replace(/\s+/g, '')
}

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`missing ${label}: ${expected}`)
  }
}

function assertNotContains(source, forbidden, label) {
  if (source.includes(forbidden)) {
    throw new Error(`forbidden ${label}: ${forbidden}`)
  }
}

const page = readUtf8('src/views/infra/runtime-control/index.vue')
const compactPage = compact(page)

assertNotContains(
  compactPage,
  'Promise.all([RuntimeControlApi.getRuntimeControlOverview(),RuntimeControlApi.getRuntimeControlOperations()])',
  'overview and recent operations bound to the same Promise.all'
)
assertContains(
  page,
  'operations.value = operationsResp',
  'recent operations assignment'
)
assertContains(
  page,
  '最近操作：',
  'recent operations load error context'
)
assertContains(
  page,
  '运维矩阵：',
  'runtime matrix load error context'
)

console.log('PASS: runtime control recent operations load independently from overview')
