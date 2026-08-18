const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const panelPath = path.join(
  frontendRoot,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const source = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const extractConstBlock = (name) => {
  const start = source.indexOf(`const ${name} = `)
  assert.ok(start >= 0, `missing const: ${name}`)
  const nextConst = source.indexOf('\nconst ', start + 1)
  assert.ok(nextConst > start, `missing boundary after const: ${name}`)
  return source.slice(start, nextConst)
}

const extractFunctionBlock = (name) => {
  const asyncStart = source.indexOf(`const ${name} = async`)
  const plainStart = source.indexOf(`const ${name} = `)
  const start = asyncStart >= 0 ? asyncStart : plainStart
  assert.ok(start >= 0, `missing function: ${name}`)
  const openIndex = source.indexOf('{', start)
  assert.ok(openIndex > start, `missing function body: ${name}`)
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(openIndex + 1, index)
      }
    }
  }
  assert.fail(`unterminated function: ${name}`)
}

const selectedEmployeeLabelBlock = extractConstBlock('selectedEmployeeLabel')
assert.match(
  selectedEmployeeLabelBlock,
  /isPqcMode\.value[\s\S]*deviceState\.selectedEmployee \|\| currentLoginEmployeeCandidate\.value/,
  'PQC 顶部员工默认显示必须来自当前登录账号，而不是等候选列表命中后才显示。'
)

const currentLoginEmployeeCandidateBlock = extractConstBlock('currentLoginEmployeeCandidate')
assert.match(
  currentLoginEmployeeCandidateBlock,
  /userStore\.getUser/,
  '当前登录员工候选必须读取账号缓存中的当前用户。'
)
assert.match(
  currentLoginEmployeeCandidateBlock,
  /nickname[\s\S]*username/,
  '当前登录员工候选必须优先使用账号姓名，并保留用户名作为正式显示来源。'
)
assert.match(
  currentLoginEmployeeCandidateBlock,
  /userId:[\s\S]*currentLoginUserId\.value[\s\S]*systemUserId:[\s\S]*currentLoginUserId\.value/,
  'PQC 默认员工身份必须使用当前账号用户 ID。'
)

const findCurrentLoginEmployeeBlock = extractFunctionBlock('findCurrentLoginEmployee')
assert.match(
  findCurrentLoginEmployeeBlock,
  /deviceState\.employeeOptions\.find\(\(employee\) => isCurrentLoginEmployee\(employee\)\)[\s\S]*currentLoginEmployeeCandidate\.value/,
  'PQC 初始化员工必须先用正式候选命中本人，候选未回填时使用当前账号员工候选。'
)

const handleSelectEmployeeBlock = extractFunctionBlock('handleSelectEmployee')
assert.match(
  handleSelectEmployeeBlock,
  /if \(isPqcMode\.value && !isCurrentLoginEmployee\(employee\)\)/,
  'PQC 员工锁定当前账号后必须拒绝非本人选择。'
)
assert.match(
  handleSelectEmployeeBlock,
  /if \(isPqcMode\.value && !deviceState\.selectedEmployee && isCurrentLoginEmployee\(employee\)\)[\s\S]*deviceState\.selectedEmployee = employee/,
  'PQC 切换接口返回后，如果候选列表未包含当前账号，仍必须把当前账号设为页面员工上下文。'
)

assert.match(
  source,
  /watch\(currentLoginUserId,[\s\S]*findCurrentLoginEmployee\(\)[\s\S]*handleSelectEmployee\(employee\)/,
  '若用户信息晚于页面初始化到达，PQC 页面必须自动补选当前账号员工。'
)

console.log('PASS: frontline PQC default employee uses current account user')
