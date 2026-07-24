import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('showroom admin basic product form exposes real submit-route controls', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /提交审批/)
  assert.match(source, /部门主管|主管审批人/)
  assert.match(source, /审批人/)
  assert.match(source, /企宣角色/)
  assert.match(source, /ShowroomAdminApi\.submitProduct/)
  assert.match(source, /targetRevisionId/)
  assert.match(source, /submitterDeptId/)
  assert.match(source, /supervisorUserId/)
  assert.doesNotMatch(source, /高昕审批人/)
  assert.doesNotMatch(source, /gaoxinUserId/)
})

test('showroom admin product detail dialog submits a real saved revision through the approval route', () => {
  const source = readText('src/views/showroom-admin/product/ProductDetailDialog.vue')

  assert.match(source, /部门主管|主管审批人/)
  assert.match(source, /审批人/)
  assert.match(source, /企宣角色/)
  assert.match(source, /ShowroomAdminApi\.saveProductDraft/)
  assert.match(source, /ShowroomAdminApi\.submitProduct/)
  assert.match(source, /targetRevisionId/)
  assert.match(source, /submitterDeptId/)
  assert.match(source, /supervisorUserId/)
  assert.doesNotMatch(source, /高昕审批人/)
  assert.doesNotMatch(source, /gaoxinUserId/)
  assert.doesNotMatch(source, /targetId:\s*state\.detail\.productId,\s*fieldCodes/s)
})
