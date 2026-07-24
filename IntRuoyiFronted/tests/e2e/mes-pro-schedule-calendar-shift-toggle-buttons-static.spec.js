const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/task/calendar/index.vue')

assert(fs.existsSync(pagePath), 'Schedule calendar page must exist.')

const source = fs.readFileSync(pagePath, 'utf8')
const gridStart = source.indexOf('<div class="calendar-grid">')
const sidebarStart = source.indexOf('<aside class="sidebar-column">')

assert.notEqual(gridStart, -1, 'Schedule calendar grid must exist.')
assert.notEqual(sidebarStart, -1, 'Schedule calendar sidebar must exist.')

const gridSource = source.slice(gridStart, sidebarStart)

assert.ok(
  !gridSource.includes('calendar-shift-editorDate.value = editable ? nextDate :'),
  'Month grid interactions must not rely on card click opening the shift editor.'
)
assert.ok(
  gridSource.includes('@click="selectCalendarDate(cell.date)"'),
  'Month grid cards must keep date selection click behavior.'
)
assert.ok(
  !/calendarShiftEditorDate\.value = calendarShiftEditorDate\.value === nextDate \? '' : nextDate/.test(source),
  'Card click logic must not toggle the local shift editor on same-date clicks.'
)
assert.match(
  source,
  /async function selectCalendarDate\(date: string\) \{[\s\S]*activeSidebarTab\.value = 'detail'[\s\S]*calendarShiftEditorDate\.value = ''[\s\S]*selectedDate\.value = nextDate[\s\S]*await loadDayDetail\(nextDate\)/,
  'Card click must switch back to the detail tab, hide the local editor, and refresh the selected day detail.'
)

for (const token of [
  'calendar-shortage-button',
  'calendar-shift-toggle-button',
  "resolveCalendarShiftToggleLabel(cell.date)",
  "toggleCalendarShiftMode(cell.date)",
  "applyCalendarShiftMode(dateText, nextMode)"
]) {
  assert.ok(source.includes(token), `Schedule calendar must expose shift toggle token: ${token}`)
}

assert.match(
  source,
  /function resolveCalendarShiftToggleLabel\(dateText: string\)/,
  'Schedule calendar must provide a helper that resolves the shift toggle label from current effective mode.'
)
assert.match(
  source,
  /function resolveCalendarShiftToggleMode\(dateText: string\)/,
  'Schedule calendar must provide a helper that resolves the next shift mode for the toggle button.'
)
assert.match(
  source,
  /function canToggleCalendarShiftMode\(dateText: string\)/,
  'Schedule calendar must provide a helper that limits shift toggle buttons to editable dates.'
)
assert.match(
  source,
  /v-if="canToggleCalendarShiftMode\(cell\.date\)"/,
  'Shift toggle button must only render for editable dates.'
)
assert.match(
  source,
  /@click\.stop="toggleCalendarShiftMode\(cell\.date\)"/,
  'Shift toggle button must use a dedicated click handler.'
)
assert.match(
  source,
  /function toggleCalendarShiftMode\(dateText: string\) \{[\s\S]*const nextMode = resolveCalendarShiftToggleMode\(dateText\)[\s\S]*applyCalendarShiftMode\(dateText, nextMode\)/,
  'Shift toggle handler must derive the next mode and reuse applyCalendarShiftMode.'
)
assert.ok(
  source.includes("return currentMode === 'REST' ? '上班' : '休息'"),
  'Shift toggle label must switch between 上班 and 休息 based on current effective mode.'
)

assert.doesNotMatch(source, /catch\s*\{\s*\}/, 'Shift toggle changes must not introduce empty catch blocks.')

console.log('PASS: MES schedule calendar shift toggle buttons static contract')
