const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const workspace = path.resolve(__dirname, '..', '..', '..')
const workTask = fs.readFileSync(path.join(workspace,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java'), 'utf8')
const deviationPage = fs.readFileSync(path.join(workspace,
  'IntRuoyiFronted/src/views/mes/pro/edhr-deviation/index.vue'), 'utf8')

assert.match(workTask, /edhr-deviation\?deviationId=\" \+ task\.getBusinessScopeId\(\)/)
assert.match(deviationPage, /route\.query\.deviationId/)
