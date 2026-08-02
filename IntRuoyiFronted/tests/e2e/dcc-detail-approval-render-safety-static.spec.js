const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const detailPagePath = 'src/views/dcc/controlled-file/detail/index.vue'
const detailPage = readSource(detailPagePath)
const normalizedDetailPage = detailPage.replace(/\r\n/g, '\n')

assert.equal(
  packageJson.scripts['e2e:dcc:detail-approval-render-safety:static'],
  'node tests/e2e/dcc-detail-approval-render-safety-static.spec.js',
  'package.json must expose the focused approval render safety contract'
)

assert.doesNotMatch(
  normalizedDetailPage,
  /\}\)const\s+openControlledBrowserLocation/,
  'DCC detail must not glue computed close tokens to openControlledBrowserLocation'
)

assert.match(
  detailPage,
  /<div class="text-15px font-600">审批阶段进度<\/div>/,
  'approval handling page must keep the stage progress anchor visible for real approvers'
)

const dialogVisibleMatches = [...detailPage.matchAll(/v-model(?::visible)?="([A-Za-z0-9_]+)\.visible"/g)]
assert.ok(dialogVisibleMatches.length > 0, 'DCC detail must declare visible state for dialog v-models')

const uniqueDialogNames = [...new Set(dialogVisibleMatches.map((match) => match[1]))]
for (const dialogName of uniqueDialogNames) {
  assert.match(
    detailPage,
    new RegExp(`const\\s+${dialogName}\\s*=\\s*reactive\\(\\{[\\s\\S]*?visible:\\s*false`),
    `dialog state ${dialogName} must be initialized before render can read .visible`
  )
}

for (const requiredDialogName of ['actionDialog', 'taskActionDialog', 'controlledPrintDialog']) {
  assert.ok(
    uniqueDialogNames.includes(requiredDialogName),
    `approval detail render safety must cover ${requiredDialogName}`
  )
  assert.match(
    detailPage,
    new RegExp(`const\\s+${requiredDialogName}\\s*=\\s*reactive\\(`),
    `${requiredDialogName} must be exposed from setup state instead of being an implicit render global`
  )
}

console.log('PASS: DCC detail approval render safety static contract')
