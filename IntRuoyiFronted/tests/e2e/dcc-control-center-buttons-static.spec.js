const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function assertMatches(source, pattern, label) {
  if (!pattern.test(source)) {
    throw new Error(`missing ${label}: ${pattern}`)
  }
}

function assertNotContains(source, forbidden, label) {
  if (source.includes(forbidden)) {
    throw new Error(`unexpected ${label}: ${forbidden}`)
  }
}

const distributionTab = readUtf8(
  'src/views/dcc/controlled-file/categories/components/CategoryDistributionRulesTab.vue'
)
const trainingTab = readUtf8('src/views/dcc/controlled-file/categories/components/CategoryTrainingRulesTab.vue')

for (const [source, label] of [
  [distributionTab, 'DCC distribution rules tab'],
  [trainingTab, 'DCC training rules tab']
]) {
  assertMatches(source, />\s*刷新\s*</, `${label} refresh button copy`)
  assertMatches(source, />\s*编辑\s*</, `${label} edit action copy`)
  assertMatches(source, />\s*预览\s*</, `${label} preview action copy`)
  assertNotContains(source, '閲嶇疆', `garbled ${label} reset button copy`)
}

console.log('PASS: DCC control-center button copy is readable')
