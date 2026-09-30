import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const read = relativePath => readFileSync(
  fileURLToPath(new URL(`../${relativePath}`, import.meta.url)),
  'utf8'
)

const browser = read('src/views/dcc/controlled-file/browser/index.vue')
const viewer = read('src/views/dcc/controlled-file/view/index.vue')
const timeForm = read('src/views/intern-user/time-maintenance/InternUserDccTimeForm.vue')
const auditDialog = read('src/views/intern-user/time-maintenance/InternUserTimeAuditDialog.vue')
const timeApi = read('src/api/intern-user/timeMaintenance/index.ts')

const extract = (source, marker, nextMarker) => {
  const start = source.indexOf(marker)
  assert.notEqual(start, -1, `missing ${marker}`)
  const end = nextMarker ? source.indexOf(nextMarker, start) : source.length
  assert.notEqual(end, -1, `missing end marker for ${marker}`)
  return source.slice(start, end)
}

test('URB-009 keeps browser time-maintenance IDs as exact strings', () => {
  const timeFormOpen = extract(timeForm, 'const open =', 'const submitForm =')
  const auditOpen = extract(auditDialog, 'const open =', 'defineExpose')
  const browserTimeFormOpen = extract(browser, 'const openDccTimeForm =', 'const openDccTimeAudit =')
  const browserAuditOpen = extract(browser, 'const openDccTimeAudit =', 'const handleMetadataSaved =')

  assert.match(timeApi, /controlledFileId: string/)
  assert.match(timeApi, /targetId: string/)
  assert.match(timeApi, /getDccPublishedTimeAudits = \(controlledFileId: string\)/)
  assert.match(timeApi, /getDccObsoletedTimeAudits = \(controlledFileId: string\)/)
  assert.match(timeFormOpen, /row: \{ id: string;/)
  assert.match(auditOpen, /targetId: string/)
  assert.match(browserTimeFormOpen, /id: String\(file\.id\)/)
  assert.match(browserAuditOpen, /open\(kind, String\(file\.id\), title\)/)
  assert.doesNotMatch(browserTimeFormOpen, /Number\(file\.id\)/)
  assert.doesNotMatch(browserAuditOpen, /Number\(file\.id\)/)
})

test('URB-010 gates every asynchronous preview write by request generation', () => {
  const resolveBlob = extract(viewer, 'const resolvePreviewBlob =', 'const clonePdfBytesForWorker =')
  const loadPreview = extract(viewer, 'const loadPreview =', 'const handleWindowKeydown =')
  const resetState = extract(viewer, 'const resetPreviewState =', 'const resolvePreviewBlob =')

  assert.match(viewer, /const isCurrentRenderVersion = \(currentRenderVersion: number\) =>/)
  assert.match(resolveBlob, /currentRenderVersion: number/)
  assert.match(resolveBlob, /isCurrentRenderVersion\(currentRenderVersion\)/)
  assert.match(resetState, /currentRenderVersion\??: number/)
  assert.match(resetState, /isCurrentRenderVersion\(currentRenderVersion\)/)
  assert.match(loadPreview, /if \(!isCurrentRenderVersion\(currentRenderVersion\)\)\s*\{\s*return\s*\}/)
  assert.match(loadPreview, /if \(isCurrentRenderVersion\(currentRenderVersion\)\) \{[\s\S]*errorMessage\.value/)
  assert.match(loadPreview, /finally \{[\s\S]*if \(isCurrentRenderVersion\(currentRenderVersion\)/)
})

test('URB-016 reports check-in success separately from list refresh failure', () => {
  const submitCheckin = extract(browser, 'const submitCheckin =', 'const mergeCheckinResult =')

  assert.match(submitCheckin, /const updatedFile = await checkinControlledFile\(/)
  assert.match(submitCheckin, /message\.success\(`文件已检入，新版本为 \$\{versionIdentity\}（ID \$\{updatedFile\.id\}）`\)/)
  assert.match(submitCheckin, /let refreshError: unknown[\s\S]*try \{[\s\S]*await getList\(\)[\s\S]*\} catch \(error\)/)
  assert.match(submitCheckin, /已成功检入[\s\S]*刷新列表失败/)
  assert.match(submitCheckin, /updatedFile\.id/)
  assert.doesNotMatch(submitCheckin, /await getList\(\)\n\s*mergeCheckinResult\(updatedFile, baseId\)\n\s*message\.success/)
})

console.log('DCC upload/revision/browser static contracts passed')
