import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const repoRoot = process.cwd()
const vitePluginConfigPath = path.join(repoRoot, 'build', 'vite', 'index.ts')
const viteConfigPath = path.join(repoRoot, 'vite.config.ts')

test('auto-import disables dts emission during build to avoid EMFILE churn', () => {
  const source = fs.readFileSync(vitePluginConfigPath, 'utf8')

  assert.match(
    source,
    /const autoImportDts = isBuild \? false : 'src\/types\/auto-imports\.d\.ts'/,
    'build-time auto-import dts should be disabled explicitly'
  )
  assert.match(
    source,
    /dts: autoImportDts/,
    'AutoImport should consume the build-aware dts setting'
  )
})

test('vite build caps parallel file operations to reduce EMFILE risk', () => {
  const source = fs.readFileSync(viteConfigPath, 'utf8')

  assert.match(
    source,
    /maxParallelFileOps:\s*1/,
    'vite build should set rollup maxParallelFileOps guard'
  )
  assert.match(
    source,
    /gracefulify\(fs\)/,
    'vite config should gracefulify fs to absorb transient EMFILE pressure'
  )
})
