const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const categoriesPage = fs.readFileSync(
  path.join(root, 'src/views/dcc/controlled-file/categories/index.vue'),
  'utf8'
)
const permissionTab = fs.readFileSync(
  path.join(root, 'src/views/dcc/controlled-file/categories/components/CategoryPermissionRulesTab.vue'),
  'utf8'
)
const fileCategoriesApi = fs.readFileSync(
  path.join(root, 'src/api/dcc/controlledFile/fileCategories.ts'),
  'utf8'
)

assert.match(categoriesPage, /label="类别权限" name="permission-rules"/)
assert.match(categoriesPage, /CategoryPermissionRulesTab/)
assert.match(permissionTab, /getCategoryPermissionRules/)
assert.match(permissionTab, /replaceCategoryPermissionRules/)
assert.match(permissionTab, /scopeType/)
assert.match(permissionTab, /subjectId/)
assert.match(permissionTab, /CATEGORY_PERMISSION_RULE_MATRIX_ACTIONS/)
assert.match(permissionTab, /DISTRIBUTE/)
assert.match(permissionTab, /保存权限规则/)
assert.match(permissionTab, /保存前请为每条规则选择授权对象/)
assert.match(fileCategoriesApi, /export const getCategoryPermissionRules/)
assert.match(fileCategoriesApi, /export const replaceCategoryPermissionRules/)

console.log('DCC category permission rules editor static contract passed')
