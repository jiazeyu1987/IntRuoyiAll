import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('sidebar shell logo uses the sidebar-only brand asset', () => {
  const source = readText('src/layout/components/Logo/src/Logo.vue')
  assert.match(source, /sidebar-brand-logo\.svg/)
  assert.doesNotMatch(source, /src="@\/assets\/imgs\/logo\.png"/)
  assert.match(source, /object-contain/)
  assert.doesNotMatch(source, /w-\[calc\(var\(--logo-height\)-10px\)\]/)
})

test('other shared logo entry points still use the shared logo asset', () => {
  for (const relativePath of [
    'src/views/Login/Login.vue',
    'src/views/Login/SocialLogin.vue',
    'src/views/Login/components/QrCodeForm.vue',
    'src/views/ai/music/index/list/audioBar/index.vue'
  ]) {
    const source = readText(relativePath)
    assert.match(source, /logo\.png/, `${relativePath} should continue to reference the shared logo asset`)
  }
})

test('sidebar-only logo asset exists', () => {
  assert.ok(fs.existsSync(path.join(root, 'src/assets/imgs/sidebar-brand-logo.svg')))
})
