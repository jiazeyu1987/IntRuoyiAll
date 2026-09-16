#!/usr/bin/env node
const { spawnSync } = require('node:child_process')
const path = require('node:path')

const specs = [
  'tests/e2e/edhr-ai-loop-stages-static.spec.cjs',
  'tests/e2e/edhr-ai-loop-contract.spec.cjs',
  'tests/e2e/edhr-ai-loop-runner-static.spec.cjs',
  'tests/e2e/edhr-ai-loop-active-order-static.spec.cjs',
  'tests/e2e/edhr-ai-loop-fixed-template-copy-static.spec.cjs',
  'tests/e2e/edhr-ai-loop-interleaving-static.spec.cjs',
  'tests/e2e/edhr-ai-loop-production-s02-static.spec.cjs',
  'tests/e2e/edhr-ai-loop-pqc-s03-static.spec.cjs',
  'tests/e2e/edhr-ai-loop-completion-s04-static.spec.cjs',
  'tests/e2e/edhr-ai-loop-release-s05-static.spec.cjs',
  'tests/e2e/edhr-ai-loop-report-upload-s06-static.spec.cjs',
  'tests/e2e/edhr-ai-loop-final-release-s07-static.spec.cjs',
  'tests/e2e/edhr-ai-loop-archive-s08-static.spec.cjs',
  'tests/e2e/edhr-ai-loop-report-schema-static.spec.cjs'
]

for (const spec of specs) {
  const result = spawnSync(process.execPath, [path.normalize(spec)], { cwd: process.cwd(), stdio: 'inherit' })
  if (result.error) throw result.error
  if (result.status !== 0) {
    process.exitCode = result.status || 1
    break
  }
}

if (!process.exitCode) console.log(`PASS: eDHR AI loop static suite (${specs.length} specs)`)
