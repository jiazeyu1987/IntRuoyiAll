import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '..')

const readText = (relativePath) => readFileSync(path.join(repoRoot, relativePath), 'utf8')

const profileIndexSource = readText('src/views/Profile/Index.vue')
const componentIndexSource = readText('src/views/Profile/components/index.ts')
const workbenchSource = readText('src/views/Profile/components/ProfileWorkbench.vue')

test('profile page keeps reset password and adds personal workbench tab', () => {
  assert.match(profileIndexSource, /<ProfileWorkbench\s*\/>/)
  assert.match(profileIndexSource, /name="workbench"/)
  assert.match(profileIndexSource, /个人工作台/)
  assert.match(profileIndexSource, /name="resetPwd"/)
  assert.match(profileIndexSource, /<ResetPwd\s*\/>/)
})

test('profile component barrel exports workbench component', () => {
  assert.match(componentIndexSource, /import ProfileWorkbench from '\.\/ProfileWorkbench\.vue'/)
  assert.match(componentIndexSource, /ProfileWorkbench/)
})

test('profile workbench reuses existing module APIs and permission checks', () => {
  assert.match(workbenchSource, /getTaskTodoPage/)
  assert.match(workbenchSource, /getTaskDonePage/)
  assert.match(workbenchSource, /getMyTrainingTaskPage/)
  assert.match(workbenchSource, /getDccElectronicSignaturePage/)
  assert.match(workbenchSource, /getEdhrWorkTaskStats/)
  assert.match(workbenchSource, /checkPermi/)
  assert.match(workbenchSource, /CONTROLLED_FILE_PROCESS_DEFINITION_KEY/)
  assert.match(workbenchSource, /EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_KEY/)
})

test('profile workbench exposes real BPM, DCC, and eDHR entry routes', () => {
  assert.match(workbenchSource, /\/bpm\/task\/todo/)
  assert.match(workbenchSource, /\/bpm\/task\/done/)
  assert.match(workbenchSource, /\/bpm\/process-instance\/my/)
  assert.match(workbenchSource, /\/dcc\/controlled-file\/approval-tasks/)
  assert.match(workbenchSource, /\/dcc\/controlled-file\/training-mine/)
  assert.match(workbenchSource, /\/dcc\/controlled-file\/signatures/)
  assert.match(workbenchSource, /\/mes\/pro\/feedback\/edhr-work-task/)
  assert.match(workbenchSource, /\/mes\/pro\/feedback\/edhr-signatures/)
})

test('profile workbench documents cross-module approval boundary and permission state', () => {
  assert.match(workbenchSource, /data-testid="profile-workbench-summary"/)
  assert.match(workbenchSource, /DCC \/ eDHR 的审批与电子签名仍在各自模块完成/)
  assert.match(workbenchSource, /无权限/)
  assert.match(workbenchSource, /个人工作台加载失败/)
})
