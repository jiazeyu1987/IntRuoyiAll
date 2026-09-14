const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

const panel = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

function includes(source, token, message) {
  assert.ok(source.includes(token), message)
}

function notIncludes(source, token, message) {
  assert.ok(!source.includes(token), message)
}

const pqcHeaderMatch = panel.match(/<header class="frontline-operator-top is-pqc">([\s\S]*?)<\/header>/)
assert.ok(pqcHeaderMatch, 'PQC 顶部区域必须存在。')
const pqcHeader = pqcHeaderMatch[1]

includes(panel, 'loadFrontlinePqcActiveOrders', 'PQC 生产订单必须来自活跃订单池。')
includes(panel, 'selectFrontlinePqcActiveOrder', '选择 PQC 活跃订单后必须加载该订单工艺路线工序。')
includes(panel, 'selectFrontlinePqcProcess', 'PQC 工序必须来自所选活跃订单对应工艺路线。')

includes(pqcHeader, 'data-pqc-login-employee-card', 'PQC 员工卡必须标记为登录员工只读卡。')
includes(pqcHeader, 'disabled', 'PQC 员工卡必须禁用点击交互。')
includes(pqcHeader, 'aria-disabled="true"', 'PQC 员工卡必须暴露不可交互语义。')
notIncludes(pqcHeader, "@click=\"openPicker('employee')\"", 'PQC 员工卡不得打开员工选择器。')

includes(panel, 'const currentLoginUserId = computed(', 'PQC 员工锁定必须读取当前登录用户。')
includes(panel, 'const isCurrentLoginEmployee = (employee?: FrontlineEmployeeCandidateVO)', '必须按登录用户判断候选员工是否为本人。')
includes(panel, 'const findCurrentLoginEmployee = ()', 'PQC 初始化员工必须只查找当前登录人。')
includes(panel, 'if (isPqcMode.value && picker === \'employee\')', 'PQC 模式打开员工选择器必须被拦截。')
includes(panel, 'if (isPqcMode.value && !isCurrentLoginEmployee(employee))', 'PQC 模式必须拒绝选择非本人。')
includes(panel, 'return findCurrentLoginEmployee()', 'PQC 初始员工必须忽略路由员工参数并返回登录人。')
includes(panel, 'if (!isPqcMode.value) {\n    context.actualEmployeeId = firstRouteQueryNumber([\'actualEmployeeId\']) ?? context.actualEmployeeId\n  }', 'PQC 模式不得从路由 actualEmployeeId 覆盖登录员工。')
