const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const layoutPath = 'src/layout/Layout.vue'

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
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

const layout = readUtf8(layoutPath)

for (const [label, token] of [
  ['project setting import', "import { Setting } from '@/layout/components/Setting'"],
  ['project setting component render', '<Setting></Setting>'],
  ['project setting self-closing render', '<Setting']
]) {
  assertNotContains(layout, token, label)
}

assertContains(layout, 'renderLayout()', 'main layout render remains')
assertContains(layout, '<Backtop></Backtop>', 'backtop remains')
assertContains(layout, 'handleClickOutside', 'mobile mask close handler remains')

console.log('PASS: global layout no longer renders the floating project setting button')
