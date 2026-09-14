const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(root, 'IntRuoyiFronted')
const backendRoot = path.join(root, 'IntRuoyiBackend/yudao-module-mes')

const read = (file) => fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n')

const api = read(path.join(frontendRoot, 'src/api/mes/pro/feedback/index.ts'))
const context = read(path.join(frontendRoot, 'src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts'))
const backendConfigRecord = read(path.join(
  backendRoot,
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfig.java'
))
const backendRuntimeService = read(path.join(
  backendRoot,
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceImpl.java'
))
const backendController = read(path.join(
  backendRoot,
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesFrontlineDeviceAccountController.java'
))
const backendResp = read(path.join(
  backendRoot,
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlineRuntimeConfigRespVO.java'
))

const extractFunctionBlock = (source, name) => {
  const starts = [
    source.indexOf('const ' + name + ' = ('),
    source.indexOf('const ' + name + ' = async'),
    source.indexOf('private MesFrontlineEmployeeSwitchResult ' + name),
    source.indexOf('private static MesFrontlineEmployeeSwitchResult ' + name)
  ].filter((index) => index >= 0).sort((a, b) => a - b)
  assert.ok(starts.length > 0, 'missing function ' + name)
  const start = starts[0]
  const openIndex = source.indexOf('{', start)
  assert.ok(openIndex > start, 'missing function body ' + name)
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) return source.slice(openIndex + 1, index)
    }
  }
  assert.fail('unterminated function ' + name)
}

assert.match(
  api,
  /employeeSwitchSnapshots:\s*FrontlineSwitchActualEmployeeRespVO\[\]/,
  'runtime-config API type must carry read-only template snapshots for every selectable employee.'
)
assert.match(
  backendConfigRecord,
  /List<MesFrontlineEmployeeSwitchResult>\s+employeeSwitchSnapshots/,
  'backend runtime config record must include read-only switch snapshots for every selectable employee.'
)
assert.match(
  backendResp,
  /private\s+List<MesFrontlineSwitchEmployeeRespVO>\s+employeeSwitchSnapshots/,
  'runtime-config response VO must expose all selectable employee template snapshots.'
)
assert.match(
  backendRuntimeService,
  /MesFrontlineTemplateResolver\s+templateResolver/,
  'runtime-config service must resolve the default employee template directly through the read-only template resolver.'
)
assert.match(
  backendRuntimeService,
  /resolveEmployeeSwitchSnapshots\(loginUserId,\s*process,\s*employees\)/,
  'runtime-config service must build every selectable employee switch snapshot while constructing runtime-config.'
)
assert.match(
  backendRuntimeService,
  /employees\.stream\(\)[\s\S]*map\(employee\s*->[\s\S]*templateResolver\.resolve\(new MesFrontlineTemplateRequest\([\s\S]*toList\(\)/,
  'runtime-config service must resolve a formal template snapshot for each selectable employee.'
)
assert.doesNotMatch(
  backendRuntimeService,
  /switchActualEmployee|MesFrontlineEmployeeSwitchService/,
  'runtime-config service must not call the employee switch POST service while preloading snapshots.'
)
assert.match(
  backendController,
  /respVO\.setEmployeeSwitchSnapshots\(config\.employeeSwitchSnapshots\(\)\.stream\(\)[\s\S]*map\(MesFrontlineDeviceAccountController::toSwitchEmployeeRespVO\)[\s\S]*toList\(\)\)/,
  'controller must map every employee switch snapshot into the runtime-config response.'
)

const cacheRuntimeBlock = extractFunctionBlock(context, 'cacheFrontlineRuntimeConfig')
assert.match(
  cacheRuntimeBlock,
  /runtimeConfig\.employeeSwitchSnapshots\.forEach\(\(snapshot\)\s*=>[\s\S]*cacheFrontlineEmployeeSwitchResult\(state,[\s\S]*snapshot/,
  'frontline cache must seed employee-switch cache from every runtime-config employee template snapshot.'
)
assert.doesNotMatch(
  cacheRuntimeBlock,
  /ProFeedbackApi\.switchFrontlineActualEmployee/,
  'preloading runtime config must not call the employee switch POST wrapper.'
)

console.log('PASS: frontline production first-switch template snapshot static contract')
