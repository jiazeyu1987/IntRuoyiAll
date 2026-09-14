const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)

const persistBlock = source.slice(
  source.indexOf('const persistCurrentPqcTaskDraft = () =>'),
  source.indexOf('const clearPqcTaskDraftsByTaskIds =')
)

assert.match(
  persistBlock,
  /activePqcTaskOptionId\.value/,
  'PQC draft persistence must identify the task by the active task id.'
)
assert.doesNotMatch(
  persistBlock,
  /const taskOption = activePqcTaskOption\.value/,
  'PQC draft persistence must not re-derive the task from the newly selected item tab.'
)
assert.match(
  persistBlock,
  /getPqcTaskOptions\(process\)[\s\S]*pqcTaskId === activeTaskId/,
  'PQC draft persistence must resolve the previous task from the selected process task list.'
)

const submitOptionsBlock = source.slice(
  source.indexOf('const getPqcCurrentSubmitTaskOptions ='),
  source.indexOf('const buildPqcItemResultsPayload =')
)

assert.match(
  submitOptionsBlock,
  /taskStatus !== 'PENDING'[\s\S]*continue/,
  'PQC submission must skip completed tasks when a process is in a mixed task state.'
)

console.log('frontline-pqc-task-draft-isolation-static: PASS')
