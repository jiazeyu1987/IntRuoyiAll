import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const vitePluginConfig = readFileSync('build/vite/index.ts', 'utf8')

test('vite dev avoids regenerating tracked auto-import declarations on Windows', () => {
  assert.match(
    vitePluginConfig,
    /const\s+autoImportDts\s*=\s*false/,
    'AutoImport dts generation must stay disabled for dev and build to avoid EMFILE on Windows'
  )
  assert.doesNotMatch(
    vitePluginConfig,
    /isBuild\s*\?\s*false\s*:\s*['"]src\/types\/auto-imports\.d\.ts['"]/,
    'dev mode must not write src/types/auto-imports.d.ts'
  )
})
