import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('Menu mounts the release info dock at the sidebar footer', () => {
  const menu = readText('src/layout/components/Menu/src/Menu.vue')
  const app = readText('src/App.vue')

  assert.match(menu, /ReleaseInfoDock/)
  assert.match(menu, /<ReleaseInfoDock\s*\/>/)
  assert.match(menu, /release-info-menu-footer/)
  assert.doesNotMatch(app, /ReleaseInfoDock/)
})

test('ReleaseInfoDock reads release-info.json and exposes git diff changes only', () => {
  const source = readText('src/components/ReleaseInfoDock/ReleaseInfoDock.vue')

  assert.match(source, /\/release-info\.json/)
  assert.match(source, /releaseTag/)
  assert.match(source, /changeSet/)
  assert.match(source, /版本信息未生成/)
  assert.match(source, /gitChangeItems/)
  assert.match(source, /gitChanges/)
  assert.match(source, /slice\(0,\s*10\)/)
  assert.match(source, /版本变化（最多 10 条）/)
  assert.doesNotMatch(source, />源码提交</)
  assert.doesNotMatch(source, />摘要</)
  assert.doesNotMatch(source, />变更项</)
  assert.doesNotMatch(source, /position:\s*fixed/)
})
