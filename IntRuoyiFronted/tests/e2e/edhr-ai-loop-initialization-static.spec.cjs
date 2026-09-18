const assert = require('node:assert/strict')
const fs = require('node:fs')
const source = fs.readFileSync('tests/e2e/edhr-ai-loop/runner.cjs', 'utf8')
const select = source.slice(source.indexOf('async function selectFrontlineProductionOrder'),source.indexOf('async function selectFrontlineProductionProcess'))
assert.match(select, /data-frontline-production-material-tab[\s\S]*waitFor[\s\S]*data-frontline-production-active-order-card/)
console.log('PASS: wait initial production material render before opening order picker')

assert.ok(source.includes('[data-frontline-production-process-current]:not(:disabled)'))
assert.ok(source.includes('[data-pqc-process-current]:not(:disabled)'))

assert.ok(source.includes('[data-pqc-order-option-code] strong:text-is('))
