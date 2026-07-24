import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('schedule calendar removes the right-side override form and list', () => {
  const source = readText('src/views/mes/pro/task/calendar/index.vue')

  assert.doesNotMatch(source, /按日期覆盖班次/)
  assert.doesNotMatch(source, /overrideDateForm/)
  assert.doesNotMatch(source, /overrideDateEntries/)
  assert.doesNotMatch(source, /addOverrideDate\(/)
  assert.doesNotMatch(source, /removeOverrideDate\(/)
})

test('schedule calendar exposes inline shift editor options on day cells', () => {
  const source = readText('src/views/mes/pro/task/calendar/index.vue')

  assert.match(source, /calendarShiftEditorDate = ref\(''\)/)
  assert.match(source, /calendarShiftOptions/)
  assert.match(source, /白班/)
  assert.match(source, /夜班/)
  assert.match(source, /双班/)
  assert.match(source, /休息/)
  assert.match(source, /calendar-shift-editor/)
  assert.match(source, /applyCalendarShiftMode\(cell\.date, option\.value\)/)
  assert.match(source, /恢复默认/)
  assert.match(source, /clearCalendarShiftMode\(cell\.date\)/)
  assert.match(source, /calendarShiftEditorDate\.value === nextDate \? '' : nextDate/)
  assert.match(source, /已覆盖/)
  assert.match(source, /可编辑/)
})

test('schedule calendar only allows editing from system today onward', () => {
  const source = readText('src/views/mes/pro/task/calendar/index.vue')

  assert.match(source, /const todayDateLabel = computed\(\(\) => dayjs\(\)\.format\('YYYY-MM-DD'\)\)/)
  assert.match(source, /function canEditCalendarDate\(dateText: string\)/)
  assert.match(source, /return normalizeDate\(dateText\) >= todayDateLabel\.value/)
  assert.match(source, /仅可修改今天及未来日期/)
})

test('schedule calendar shows re-schedule messaging and keeps saveRules persistence', () => {
  const source = readText('src/views/mes/pro/task/calendar/index.vue')
  const applyFunctionStart = source.indexOf('function applyCalendarShiftMode')
  const applyFunctionEnd = source.indexOf('function clearCalendarShiftMode')
  const applyFunctionSource = source.slice(applyFunctionStart, applyFunctionEnd)

  assert.match(source, /rulesStatusText = computed/)
  assert.match(source, /班次规则已变更，请先保存规则后重新排产/)
  assert.match(source, /班次规则已变更，请重新生成预览后重新排产/)
  assert.match(source, /排程规则已更新，请重新生成预览后再发布排产/)
  assert.match(source, /ProScheduleCalendarApi\.updateRules\(buildRulesPayload\(\)\)/)
  assert.doesNotMatch(applyFunctionSource, /ProScheduleCalendarApi\.updateRules/)
})

test('schedule calendar opens the inline editor before awaiting day detail reload', () => {
  const source = readText('src/views/mes/pro/task/calendar/index.vue')
  const selectFunctionStart = source.indexOf('async function selectCalendarDate')
  const selectFunctionEnd = source.indexOf('async function changeMonth')
  const selectFunctionSource = source.slice(selectFunctionStart, selectFunctionEnd)
  const openEditorIndex = selectFunctionSource.indexOf("calendarShiftEditorDate.value = editable ? nextDate : ''")
  const loadDayDetailIndex = selectFunctionSource.indexOf('await loadDayDetail(nextDate)')

  assert.notEqual(selectFunctionStart, -1)
  assert.notEqual(selectFunctionEnd, -1)
  assert.match(selectFunctionSource, /const editable = canEditCalendarDate\(nextDate\)/)
  assert.notEqual(openEditorIndex, -1)
  assert.notEqual(loadDayDetailIndex, -1)
  assert.ok(
    openEditorIndex < loadDayDetailIndex,
    'expected editable-day popup state to be set before awaiting day detail reload'
  )
})
