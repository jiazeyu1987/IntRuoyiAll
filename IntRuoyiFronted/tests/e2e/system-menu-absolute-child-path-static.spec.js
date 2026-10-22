const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const menuForm = fs.readFileSync(path.join(root, 'src/views/system/menu/MenuForm.vue'), 'utf8')
const router = fs.readFileSync(path.join(root, 'src/utils/routerHelper.ts'), 'utf8')

assert.match(menuForm, /isExternal\(formData\.value\.path\)/, 'menu form must retain external URL validation')
assert.match(menuForm, /formData\.value\.parentId === 0[\s\S]*path\.charAt\(0\) !== '\/'/, 'root menus must still require a leading slash')
assert.doesNotMatch(
  menuForm,
  /formData\.value\.parentId !== 0[\s\S]*path\.charAt\(0\) === '\/'[\s\S]*路径不能以 \/ 开头/,
  'child menus must allow the absolute paths already supported by the router and seed data'
)
assert.match(router, /if \(isUrl\(path\) \|\| path\.startsWith\('\/'\)\)/, 'router must continue to support absolute child paths')
console.log('system-menu-absolute-child-path-static: PASS')
