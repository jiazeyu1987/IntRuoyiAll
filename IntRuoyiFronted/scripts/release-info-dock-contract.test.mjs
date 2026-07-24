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

test('ReleaseInfoDock reads release-info.json and exposes version details', () => {
  const source = readText('src/components/ReleaseInfoDock/ReleaseInfoDock.vue')

  assert.match(source, /\/release-info\.json/)
  assert.match(source, /releaseTag/)
  assert.match(source, /changeSet/)
  assert.match(source, /sourceRepos/)
  assert.match(source, /版本信息未生成/)
  assert.match(source, /查看变更/)
  assert.doesNotMatch(source, /position:\s*fixed/)
})
