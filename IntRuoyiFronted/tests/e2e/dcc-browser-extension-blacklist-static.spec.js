const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')

assert.equal(
  packageJson.scripts['e2e:dcc:browser-extension-blacklist:static'],
  'node tests/e2e/dcc-browser-extension-blacklist-static.spec.js',
  'package.json must expose the DCC browser extension blacklist static contract'
)

for (const required of [
  /export interface ControlledFileBrowserExtensionBlacklistRespVO/,
  /extensionPatterns: string\[\]/,
  /export interface ControlledFileBrowserExtensionBlacklistSaveReqVO/,
  /export const getControlledFileBrowserExtensionBlacklist = async/,
  /url: '\/dcc\/controlled-files\/browser-extension-blacklist'/,
  /export const saveControlledFileBrowserExtensionBlacklist = async/
]) {
  assert.match(workflowApi, required, `workflow API missing extension blacklist contract: ${required}`)
}

assert.match(
  browserPage,
  /<el-form-item\s+v-if="canEditMetadata">[\s\S]*<el-popover/,
  'advanced button container must only render for doc_control users'
)

for (const required of [
  /data-testid="dcc-browser-extension-blacklist-open"/,
  /后缀黑名单/,
  /title="文件后缀黑名单"/,
  /data-testid="dcc-browser-extension-blacklist-input"/,
  /placeholder="例如：\*\.db、\*\.pyc"/,
  /data-testid="dcc-browser-extension-blacklist-save"/,
  /getControlledFileBrowserExtensionBlacklist\(\)/,
  /saveControlledFileBrowserExtensionBlacklist\(\{ extensionPatterns:/,
  /message\.success\('黑名单已保存'\)/,
  /await getList\(\)/
]) {
  assert.match(browserPage, required, `browser page missing extension blacklist UI contract: ${required}`)
}

assert.doesNotMatch(
  browserPage,
  /catch\s*\(\s*\)\s*\{\s*\}|mock|placeholder data|静默成功|fallback|降级/i,
  'extension blacklist UI must not hide errors, introduce mock data, fallback, or downgrade logic'
)

console.log('PASS: DCC browser extension blacklist static contract')
