const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { parse, compileTemplate } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../../..')
const filename = path.join(root, 'IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const source = fs.readFileSync(filename, 'utf8')
const { descriptor, errors } = parse(source)
assert.deepEqual(errors, [])
assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'manager-reject' }).errors, [])

const rejectButtons = [...descriptor.template.content.matchAll(/<el-button\b[^>]*data-edhr-batch-action="reject"[^>]*>/g)]
assert.equal(rejectButtons.length, 2, '普通和放行锁定分支均须保留驳回入口')
for (const [button] of rejectButtons) {
  const binding = button.match(/v-hasPermi="\[([^\]]+)\]"/)
  assert.ok(binding, '驳回按钮必须受权限控制')
  const permissions = [...binding[1].matchAll(/'([^']+)'/g)].map(match => match[1])
  assert.deepEqual(permissions, ['mes:pro-production-release:pqc-reject', 'mes:pro-edhr-release:approve'])
  assert.equal(permissions.some(permission => ['mes:pro-edhr-release:approve'].includes(permission)), true)
  assert.equal(permissions.some(permission => ['mes:pro-edhr-release:query'].includes(permission)), false)
  assert.match(button, /@click="handleRejectClick\(row\)"/)
}

const controller = fs.readFileSync(path.join(root, 'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrNonconformanceReviewController.java'), 'utf8')
assert.match(controller, /@PostMapping\("\/reject-batch"\)[\s\S]*?@PreAuthorize\("@ss\.hasAnyPermissions\('mes:pro-production-release:pqc-reject', 'mes:pro-edhr-release:approve'\)"\)/)
console.log('PASS manager release approval permission enables both reject buttons and the matching endpoint; query-only permission is excluded')
