import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('schedule calendar issue dialog splits blocking errors and warnings into separate tabs', () => {
  const source = readText('src/views/mes/pro/task/calendar/index.vue')

  assert.match(source, /<el-tabs[\s\S]*v-model="issueDialogActiveTab"/)
  assert.match(source, /label="错误\s*\/\s*阻塞"/)
  assert.match(source, /label="警告"/)
  assert.match(source, /const issueDialogActiveTab = ref<[^>]+>\('error'\)/)
  assert.match(source, /const issueDialogErrorRows = computed/)
  assert.match(source, /const issueDialogWarningRows = computed/)
  assert.match(
    source,
    /issueDialogActiveTab\.value = issueDialogErrorRows\.value\.length > 0 \? 'error' : 'warning'/
  )
})

test('schedule calendar issue dialog supports direct jumps for process and workstation issue targets', () => {
  const source = readText('src/views/mes/pro/task/calendar/index.vue')

  assert.match(source, /<el-table-column label="工序"/)
  assert.match(source, /<el-table-column label="工作站"/)
  assert.match(source, /openIssueProcess\(row\)/)
  assert.match(source, /openIssueWorkstation\(row\)/)
  assert.match(source, /canOpenIssueProcess\(row\)/)
  assert.match(source, /canOpenIssueWorkstation\(row\)/)
  assert.match(source, /push\(\{\s*path:\s*'\/mes\/pro\/process'/)
  assert.match(source, /push\(\{\s*path:\s*'\/mes\/md\/workstation'/)
})

test('process index supports query-driven detail opening for calendar issue deep links', () => {
  const source = readText('src/views/mes/pro/process/index.vue')

  assert.match(source, /const route = useRoute\(\)/)
  assert.match(source, /queryParams\.code = typeof route\.query\.code === 'string'/)
  assert.match(source, /queryParams\.name = typeof route\.query\.name === 'string'/)
  assert.match(source, /const openId = typeof route\.query\.openId === 'string' \? route\.query\.openId : ''/)
  assert.match(source, /openForm\('detail', Number\(openId\)\)/)
  assert.match(source, /watch\(\s*\(\) => \[route\.query\.code, route\.query\.name, route\.query\.openId\]/)
})

test('workstation index supports query-driven detail opening and process filter for calendar issue deep links', () => {
  const source = readText('src/views/mes/md/workstation/index.vue')

  assert.match(source, /const route = useRoute\(\)/)
  assert.match(source, /queryParams\.code = typeof route\.query\.code === 'string'/)
  assert.match(source, /queryParams\.name = typeof route\.query\.name === 'string'/)
  assert.match(source, /const processIdText = typeof route\.query\.processId === 'string' \? route\.query\.processId : ''/)
  assert.match(source, /queryParams\.processId = processIdText && Number\.isFinite\(processId\) \? processId : undefined/)
  assert.match(source, /const openId = typeof route\.query\.openId === 'string' \? route\.query\.openId : ''/)
  assert.match(source, /openForm\('detail', Number\(openId\)\)/)
  assert.match(
    source,
    /watch\(\s*\(\) => \[route\.query\.code, route\.query\.name, route\.query\.processId, route\.query\.openId\]/
  )
})
