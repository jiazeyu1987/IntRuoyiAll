import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('approval task panel renders humanized diff and preview bindings instead of raw machine payloads', () => {
  const source = readText('src/views/showroom-admin/approval/ApprovalTaskPanel.vue')

  assert.match(source, /label="字段"[\s\S]*prop="label"/)
  assert.match(source, /label="旧值"[\s\S]*(prop="oldValue"|row\.oldValue)/)
  assert.match(source, /label="新值"[\s\S]*(prop="newValue"|row\.newValue)/)
  assert.match(source, /label="Live 值"[\s\S]*prop="liveValue"/)
  assert.match(source, /label="目标值"[\s\S]*prop="targetValue"/)
  assert.doesNotMatch(source, /prop="oldValueJson"/)
  assert.doesNotMatch(source, /prop="newValueJson"/)
  assert.doesNotMatch(source, /flattenPreviewRows/)
})

test('approval contracts expose humanized label and preview row fields', () => {
  const source = readText('src/views/showroom-admin/approval/contracts.ts')

  assert.match(source, /label: string/)
  assert.match(source, /oldValue: string/)
  assert.match(source, /newValue: string/)
  assert.match(source, /export interface ShowroomApprovalPreviewRow/)
  assert.match(source, /rows: ShowroomApprovalPreviewRow\[]/)
})
