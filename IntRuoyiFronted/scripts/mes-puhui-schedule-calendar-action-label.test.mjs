import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { test } from 'node:test'

const repoRoot = resolve(import.meta.dirname, '..')
const calendarPath = resolve(
  repoRoot,
  'src/views/mes/pro/puhui-schedule/components/PuhuiScheduleCalendar.vue'
)
const calendarSource = readFileSync(calendarPath, 'utf8')

test('Puhui schedule calendar uses explicit action labels instead of ambiguous status words', () => {
  assert.match(
    calendarSource,
    /isInSchedule\(date\)\s*\?\s*'设为非排产日'\s*:\s*'设为排产日'/,
    'date action button must use explicit action labels'
  )
  assert.doesNotMatch(
    calendarSource,
    /isInSchedule\(date\)\s*\?\s*'休息'\s*:\s*'排产'/,
    'date action button must not show bare status-like labels'
  )
})
