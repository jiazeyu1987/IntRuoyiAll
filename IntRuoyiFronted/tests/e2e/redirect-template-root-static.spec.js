const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const redirectPages = [
  {
    path: 'src/views/dcc/controlled-file/approval-tasks/index.vue',
    redirectPattern:
      /router\.replace[\s\S]{0,220}path:\s*'\/approval-center'[\s\S]{0,220}moduleCode:\s*'DCC'[\s\S]{0,120}viewType:\s*'TODO'/
  },
  {
    path: 'src/views/mes/pro/edhr/ApprovalPage.vue',
    redirectPattern:
      /router\.replace[\s\S]{0,260}path:\s*'\/approval-center'[\s\S]{0,220}moduleCode:\s*'EDHR'/
  }
]

for (const page of redirectPages) {
  const source = readSource(page.path)
  assert.doesNotMatch(
    source,
    /<template>\s*<\/template>/,
    `${page.path} must not use an empty template root`
  )
  assert.match(
    source,
    /<template>\s*<div[\s\S]*?<\/div>\s*<\/template>/,
    `${page.path} must keep a concrete template root element for Vue lint/build`
  )
  assert.match(source, page.redirectPattern, `${page.path} must keep unified approval redirect`)
}

console.log('PASS: redirect template root static contract')
