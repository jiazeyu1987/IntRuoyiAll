const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const contextPath = path.join(root, 'src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts')
const apiPath = path.join(root, 'src/api/mes/pro/feedback/index.ts')

const panel = fs.readFileSync(panelPath, 'utf8')
const context = fs.readFileSync(contextPath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

function includes(source, token, message) {
  assert.ok(source.includes(token), message)
}

function notIncludes(source, token, message) {
  assert.ok(!source.includes(token), message)
}

includes(panel, "type PickerType = 'order' | 'process' | 'employee'", 'PQC 选择器类型必须包含订单。')
includes(panel, '@click="openPicker(\'order\')"', 'PQC 顶部生产订单卡片必须打开订单选择器。')
includes(panel, 'loadFrontlinePqcActiveOrders', 'PQC 页面必须加载当前活跃订单来源。')
includes(panel, 'selectFrontlinePqcActiveOrder', '选择订单后必须按活跃订单加载工艺路线工序。')
includes(panel, 'selectFrontlinePqcProcess', 'PQC 工序选择必须走活跃订单对应路线工序链路。')
includes(panel, 'switchFrontlinePqcActualEmployee', 'PQC 员工切换必须走 PQC 人员链路。')
includes(panel, 'isPqcMode.value', 'PQC 初始化必须与生产模式分支隔离。')
notIncludes(panel, '|| activeOrders[0]', 'PQC 初始化不得在 URL 未指定订单时自动选择第一个待检订单。')
includes(panel, 'const initialActiveOrder = context.workOrderId', 'PQC 初始化只有在路由上下文明确指定 workOrderId 时才恢复选中订单。')

includes(context, 'activeOrderOptions', '前端状态必须持有当前活跃订单候选。')
includes(context, 'getFrontlinePqcActiveOrders', 'PQC 活跃订单必须来自后端当前活跃订单接口。')
includes(context, 'getFrontlinePqcActiveOrderProcesses', 'PQC 工序必须来自所选活跃订单对应路线。')
includes(context, 'getFrontlinePqcEmployeeCandidates', 'PQC 员工必须来自所有 PQC 员工和 PQC 组长。')
includes(context, 'switchFrontlinePqcActualEmployee', 'PQC 员工切换必须使用独立接口，不能复用设备账号员工绑定。')

includes(api, '/mes/pro/feedback/frontline/device-account/pqc/active-orders', 'API 必须提供 PQC 当前活跃订单接口。')
includes(api, '/mes/pro/feedback/frontline/device-account/pqc/active-order/processes', 'API 必须提供按活跃订单加载工序接口。')
includes(api, '/mes/pro/feedback/frontline/device-account/pqc/personnel', 'API 必须提供 PQC 人员接口。')
includes(api, '/mes/pro/feedback/frontline/device-account/pqc/switch-employee', 'API 必须提供 PQC 员工切换接口。')

notIncludes(panel, "if (isPqcMode.value) {\n    const error = new Error('PQC 详细检验内容尚未纳入正式模板字段", 'PQC 模式不得继续用未纳入正式模板阻塞提交。')
