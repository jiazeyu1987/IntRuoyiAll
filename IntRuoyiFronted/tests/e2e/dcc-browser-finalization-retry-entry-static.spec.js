const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const browserPath = path.resolve(__dirname, '../../src/views/dcc/controlled-file/browser/index.vue')
const source = fs.readFileSync(browserPath, 'utf8')

assert.match(
  source,
  /getSelectedVersion\(row\)\.status === 'FINALIZATION_FAILED'[\s\S]*data-testid="dcc-controlled-browser-retry-publish"[\s\S]*@click="openManagement\(getSelectedVersion\(row\)\.id\)"[\s\S]*重试发布/,
  'FINALIZATION_FAILED browser versions must expose a visible management entry for the existing retry action'
)

console.log('DCC browser finalization retry entry static checks passed')
