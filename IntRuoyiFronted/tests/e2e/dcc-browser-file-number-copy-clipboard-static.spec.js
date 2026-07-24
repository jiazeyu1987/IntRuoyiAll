import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const browserSource = readText('src/views/dcc/controlled-file/browser/index.vue')

test('dcc browser file number copy must write through the legacy clipboard path', () => {
  assert.match(
    browserSource,
    /const \{ copy: copyToClipboard \} = useClipboard\(\{ legacy: true \}\)/,
    'browser copy action must enable legacy clipboard writing so the button really copies on supported browser contexts'
  )
  assert.match(
    browserSource,
    /await copyToClipboard\(normalizedFileNumber\)/,
    'browser copy action must still write the normalized file number'
  )
  assert.match(
    browserSource,
    /message\.success\('文件编号已复制'\)/,
    'browser copy action must keep the success toast after a real write'
  )
  assert.match(
    browserSource,
    /message\.error\('文件编号复制失败，请检查浏览器剪贴板权限或浏览器限制。'\)/,
    'browser copy action must still expose clipboard failures'
  )
  assert.match(browserSource, /throw error/, 'browser copy failure must remain fail-fast')
})

console.log('PASS: DCC browser file number clipboard contract')
