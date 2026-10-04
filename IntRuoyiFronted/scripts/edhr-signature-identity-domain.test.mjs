import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
const source = fs.readFileSync(new URL('../src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts', import.meta.url), 'utf8')
const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
const exports = {}
let requests = []
const api = { switchFrontlineActualEmployee: async (payload) => {
  requests.push(payload)
  return { ...payload, loginUserId: 99, extraVerificationRequired: false, template: { templateNo: payload.identityDomain } }
}}
vm.runInNewContext(js, { exports, require: () => ({ ProFeedbackApi: api }), Map, Promise, Error, console })
const state = exports.createFrontlineDeviceEmployeeState()
state.selectedProcess = { activeOrderId: 1, routeId: 2, routeProcessId: 3, processId: 4 }
state.employeeOptions = [
  { userId: 7, systemUserId: 7, employeeProfileId: 17, nickname: '系统员工' },
  { userId: 7, employeeProfileId: 7, nickname: '临时员工' }
]
await exports.switchFrontlineActualEmployee(state, 7, 'SYSTEM_USER')
assert.equal(state.selectedEmployee.nickname, '系统员工')
await exports.switchFrontlineActualEmployee(state, 7, 'MES_EMPLOYEE_PROFILE')
assert.equal(state.selectedEmployee.nickname, '临时员工')
await exports.switchFrontlineActualEmployee(state, 7, 'SYSTEM_USER')
assert.equal(state.selectedEmployee.nickname, '系统员工')
assert.equal(requests.length, 2, 'domain-specific cached result must be reused without overwriting the other domain')
assert.deepEqual(requests.map(r => r.identityDomain), ['SYSTEM_USER', 'MES_EMPLOYEE_PROFILE'])
console.log('PASS: production switch request, selection and cache separate identical numeric IDs by domain')
