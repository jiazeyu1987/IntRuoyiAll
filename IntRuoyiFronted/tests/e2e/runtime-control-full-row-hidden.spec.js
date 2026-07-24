const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/infra/runtime-control/index.vue'),
  'utf8'
)

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

function extractConstArray(source, constName) {
  const match = source.match(new RegExp(`const ${constName} = \\[([\\s\\S]*?)\\]\\n`))
  if (!match) {
    throw new Error(`missing const array: ${constName}`)
  }
  return match[1]
}

function extractFunction(source, functionName) {
  const match = source.match(new RegExp(`const ${functionName} = \\([^)]*\\) => \\{([\\s\\S]*?)\\n\\}`))
  if (!match) {
    throw new Error(`missing function: ${functionName}`)
  }
  return match[1]
}

const componentRows = extractConstArray(page, 'displayComponentRows')
const currentReleaseTagValue = extractFunction(page, 'currentReleaseTagValue')
const shouldShowAccessPath = extractFunction(page, 'shouldShowAccessPath')

assertContains(page, 'displayComponentRows', 'fixed component row source')
assertContains(page, ':data-runtime-component-row="component.key"', 'component row data marker')
assertContains(componentRows, "{ key: 'intruoyi-frontend', label: 'IntRuoyi 前端' }", 'IntRuoyi frontend row')
assertContains(componentRows, "{ key: 'intruoyi-backend', label: 'IntRuoyi 后端' }", 'IntRuoyi backend row')
assertContains(componentRows, "{ key: 'website-frontend', label: 'Website 前端' }", 'Website frontend row')
assertNotContains(componentRows, 'intruoyi-full', 'IntRuoyi full display row')
assertNotContains(componentRows, 'IntRuoyi 整套', 'IntRuoyi full display label')
assertNotContains(page, 'runtime-row--full', 'IntRuoyi full row emphasis class')
assertNotContains(shouldShowAccessPath, 'intruoyi-full', 'IntRuoyi full access path display helper')
assertContains(
  currentReleaseTagValue,
  "statusOf(environment, 'intruoyi-full')?.currentReleaseTag",
  'IntRuoyi full current release source'
)

const componentRowOrder = ['intruoyi-frontend', 'intruoyi-backend', 'website-frontend']
const orderIndexes = componentRowOrder.map((key) => componentRows.indexOf(`key: '${key}'`))
for (const [index, position] of orderIndexes.entries()) {
  if (position < 0) {
    throw new Error(`missing fixed component row: ${componentRowOrder[index]}`)
  }
  if (index > 0 && position <= orderIndexes[index - 1]) {
    throw new Error(`component rows must keep expected order: ${componentRowOrder.join(', ')}`)
  }
}

console.log('PASS: runtime control IntRuoyi full row is hidden from the status matrix')
