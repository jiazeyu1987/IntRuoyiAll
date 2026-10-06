const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
for (const [file, loader] of [
  ['src/views/Profile/components/ProfileWorkbench.vue', 'loadEdhrRows'],
  ['src/store/modules/profileWorkbenchTodoBadge.ts', 'loadEdhrWorkTaskTodoTotal']
]) {
  const source = read(file).split(`const ${loader} = async () => {`)[1]?.split('\n}')[0]
  assert.ok(source, `missing production loader: ${loader}`)
  assert.match(source, /getEdhrWorkTaskMyPage\([\s\S]*includeOverdue:\s*true/, `${loader} must explicitly include actionable OVERDUE tasks`)
  assert.doesNotMatch(source, /status:\s*EDHR_WORK_TASK_STATUS_TODO/, `${loader} must not restrict the combined query to TODO`)
}
assert.match(read('src/api/mes/pro/edhr/workTask.ts'), /includeOverdue\?:\s*boolean/, 'API must type the explicit combined option')
console.log('PASS: profile eDHR overdue static contract')
