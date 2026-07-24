const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(repoRoot, relativePath))

const router = read('src/router/modules/remaining.ts')

assert.equal(
  exists('src/views/mes/pro/edhr-version-governance/VersionGovernancePage.vue'),
  false,
  'eDHR 版本治理页面组件必须删除。'
)
assert.equal(
  exists('src/api/mes/pro/edhr/versionGovernance.ts'),
  false,
  'eDHR 版本治理前端 API 包装必须随页面删除，后端能力由后端控制器保留。'
)

for (const forbidden of [
  'pro/feedback/edhr-version-governance',
  '@/views/mes/pro/edhr-version-governance/VersionGovernancePage.vue',
  'MesProFeedbackEdhrVersionGovernance',
  "title: 'eDHR版本治理'",
  "activeMenu: '/mes/pro/feedback/edhr-version-governance'"
]) {
  assert.equal(router.includes(forbidden), false, `remaining.ts 不应继续暴露 ${forbidden}`)
}

console.log('PASS: eDHR version governance frontend menu, page and API wrapper removed')
