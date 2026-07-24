import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('batch record report API module exposes import page designer and delete endpoints', () => {
  const source = readText('src/api/mes/pro/batchrecordreport/index.ts')
  assert.match(source, /importPilotDoc/)
  assert.match(source, /getGeneratedReportPage/)
  assert.match(source, /getDesignerPath/)
  assert.match(source, /deleteGeneratedReport/)
  assert.match(source, /\/mes\/pro\/batch-record-report\/import/)
})

test('electronic batch record page switches into designer wrapper mode by query param', () => {
  const source = readText('src/views/mes/pro/batchrecordtemplate/index.vue')
  assert.match(source, /DesignerWrapper\.vue/)
  assert.match(source, /mode: 'designer'/)
  assert.match(source, /reportId/)
})
