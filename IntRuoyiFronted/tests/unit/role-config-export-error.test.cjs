const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/system/role/index.vue'), 'utf8')
const handler = source.slice(source.indexOf('const handleExport ='), source.indexOf('const openImport ='))

test('export failure retains the full reason and selection; retry downloads and closes', async () => {
  const exportCategoryIds = { value: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10] }
  const exportLoading = { value: false }
  const exportError = { value: '' }
  const exportDialogVisible = { value: true }
  const reason = '配置包引用缺失：' + '完整菜单标识'.repeat(100)
  let fail = true
  const downloads = []
  const data = { packageVersion: '3' }
  const RoleApi = { exportRoleConfigPackage: async (ids) => {
    assert.equal(exportLoading.value, true)
    assert.equal(exportError.value, '')
    assert.deepEqual(ids, exportCategoryIds.value)
    if (fail) throw new Error(reason)
    return data
  } }
  const download = { json: (...args) => downloads.push(args) }
  const run = Function('exportCategoryIds', 'exportLoading', 'exportError', 'exportDialogVisible', 'RoleApi', 'download',
    handler + '\nreturn handleExport')(exportCategoryIds, exportLoading, exportError, exportDialogVisible, RoleApi, download)
  await run()
  assert.equal(exportError.value, reason)
  assert.equal(exportDialogVisible.value, true)
  assert.equal(exportLoading.value, false)
  assert.equal(exportCategoryIds.value.length, 10)
  assert.deepEqual(downloads, [])
  fail = false
  await run()
  assert.equal(exportError.value, '')
  assert.equal(exportLoading.value, false)
  assert.equal(exportDialogVisible.value, false)
  assert.deepEqual(downloads, [[data, '权限角色配置包.json']])
})
