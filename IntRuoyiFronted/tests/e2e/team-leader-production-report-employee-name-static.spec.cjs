const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const employeeColumnStart = page.indexOf('prop="employeeUser"')
assert.ok(employeeColumnStart >= 0, '生产组长/PQC 组长提交列表必须存在员工列。')
const employeeColumnBlockStart = page.lastIndexOf('<el-table-column', employeeColumnStart)
const employeeColumnBlockEnd = page.indexOf('</el-table-column>', employeeColumnStart)
assert.ok(
  employeeColumnBlockStart >= 0 && employeeColumnBlockEnd > employeeColumnBlockStart,
  '员工列表列模板必须可定位。'
)
const employeeColumnBlock = page.slice(employeeColumnBlockStart, employeeColumnBlockEnd)

assert.match(
  employeeColumnBlock,
  /row\.actualEmployeeUserName\s*\|\|\s*'--'/,
  '员工列必须显示后端返回的实际员工姓名；姓名缺失时显示空态，不得退回显示用户编号。'
)
assert.doesNotMatch(
  employeeColumnBlock,
  /actualEmployeeUserId/,
  '员工列不得把 actualEmployeeUserId 当作显示文案，否则用户会看到编号而不是姓名。'
)

for (const detailMarker of [
  '<el-descriptions-item :label="employeeDetailLabel">',
  '<el-descriptions-item label="PQC检验员">'
]) {
  const detailStart = page.indexOf(detailMarker)
  if (detailStart < 0) continue
  const detailEnd = page.indexOf('</el-descriptions-item>', detailStart)
  const detailBlock = page.slice(detailStart, detailEnd)
  assert.match(
    detailBlock,
    /actualEmployeeUserName\s*\|\|\s*'--'/,
    '提交详情中的员工字段也必须显示姓名空态，不得显示编号。'
  )
  assert.doesNotMatch(
    detailBlock,
    /actualEmployeeUserId/,
    '提交详情员工字段不得退回显示 actualEmployeeUserId。'
  )
}

console.log('PASS: team leader production report employee column renders employee name only')
