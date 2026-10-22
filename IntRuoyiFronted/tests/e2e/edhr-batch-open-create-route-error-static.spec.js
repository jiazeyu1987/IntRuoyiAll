import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')
assert.doesNotMatch(source, /submitOpenOrCreate|createRouteOptionsLoading|createForm/, 'Manual creation and its route picker must be removed.')
console.log('PASS: removed manual batch route picker')
