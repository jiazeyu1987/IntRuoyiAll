import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('schedule calendar imports work-order material demand api for preview overlay demand grouping', () => {
  const source = readText('src/views/mes/pro/task/calendar/index.vue')

  assert.match(source, /ProWorkOrderBomApi/)
  assert.match(source, /loadPreviewMaterialDemandMap/)
  assert.match(source, /previewMaterialDemandByWorkOrderId/)
})

test('schedule calendar switches month calendar cells to preview overlay when preview exists', () => {
  const source = readText('src/views/mes/pro/task/calendar/index.vue')

  assert.match(source, /const isPreviewCalendarOverlayActive = computed/)
  assert.match(source, /const previewCalendarDayMap = computed/)
  assert.match(source, /const activeCalendarDayMap = computed/)
  assert.match(source, /const activeMonthStats = computed/)
  assert.match(source, /info: activeCalendarDayMap\.value\.get\(dateText\)/)
  assert.match(source, /{{ activeMonthStats\.taskCount }}/)
  assert.match(source, /{{ activeMonthStats\.orderCount }}/)
})

test('schedule calendar day detail branches to preview task rows instead of formal dayDetail when preview is active', () => {
  const source = readText('src/views/mes/pro/task/calendar/index.vue')

  assert.match(source, /const previewTaskRowsByDate = computed/)
  assert.match(source, /if \(isPreviewCalendarOverlayActive\.value\) \{\s*return previewTaskRowsByDate\.value\.get\(selectedDate\.value\) \|\| \[\]/)
  assert.match(source, /const activeSelectedDayShiftTaskCount = computed/)
  assert.match(source, /const activeSelectedDayNightShiftTaskCount = computed/)
  assert.match(source, /const selectedDayMaterialRows = computed/)
  assert.match(source, /{{ activeSelectedDayShiftTaskCount }}/)
  assert.match(source, /{{ activeSelectedDayNightShiftTaskCount }}/)
})

test('schedule calendar preview generation loads per-work-order material demand map and clearPreview resets it', () => {
  const source = readText('src/views/mes/pro/task/calendar/index.vue')

  assert.match(source, /await loadPreviewMaterialDemandMap\(request\.workOrderIds\)/)
  assert.match(source, /previewMaterialDemandByWorkOrderId\.value = \{\}/)
  assert.match(source, /const previewMaterialRowsByDate = computed/)
})
