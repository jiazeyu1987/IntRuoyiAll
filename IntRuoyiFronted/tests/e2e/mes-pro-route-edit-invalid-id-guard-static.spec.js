const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (file) => fs.readFileSync(path.join(repoRoot, file), 'utf8')

const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')

assert.match(
  routeEditPage,
  /routeEditBlockingError/,
  'RouteEditPage must expose a blocking error state instead of rendering an empty basic-info form.'
)
assert.match(
  routeEditPage,
  /v-if="routeEditBlockingError"[\s\S]{0,800}返回列表/,
  'Invalid or failed route edit loads must show a visible error with a return-to-list action.'
)
assert.match(
  routeEditPage,
  /<RouteFormContent[\s\S]{0,260}v-else/,
  'RouteFormContent must only render after the hidden edit route has a valid load target.'
)
assert.doesNotMatch(
  routeEditPage,
  /throw new Error\('编辑工艺路线失败：缺少有效路线编号'\)/,
  'Invalid route IDs must not leave the initial empty form visible through an async watcher throw.'
)

console.log('PASS: route edit invalid id renders blocking error instead of blank basic form')
