const assert = require('assert')
const fs = require('fs')
const path = require('path')
const postcss = require('postcss')

const pagePath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)
const source = fs.readFileSync(pagePath, 'utf8')
const scriptMatch = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)
const styles = [...source.matchAll(/<style\b[^>]*>([\s\S]*?)<\/style>/g)].map(
  (match) => match[1]
)

assert(scriptMatch, '班组长工作台必须保留 script setup 区块。')
assert(styles.length > 0, '班组长工作台必须保留样式区块。')

for (const [index, style] of styles.entries()) {
  postcss.parse(style, { from: `${pagePath}?style=${index}` })
}

const script = scriptMatch[1]

for (const functionName of ['resetAbnormalForm', 'openAbnormalDialog']) {
  assert(
    script.includes(`const ${functionName} =`),
    `${functionName} 必须定义在 script setup 区块。`
  )
  assert(
    styles.every((style) => !style.includes(`const ${functionName} =`)),
    `${functionName} 不得出现在 CSS 样式区块。`
  )
}

console.log('team leader workbench SFC style compile contract passed')
