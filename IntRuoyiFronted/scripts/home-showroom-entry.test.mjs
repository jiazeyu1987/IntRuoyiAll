import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('home page only keeps the showroom admin entry', () => {
  const source = readText('src/views/Home/Index.vue')

  assert.match(source, /展厅后台入口/)
  assert.match(source, /进入展厅后台/)
  assert.match(source, /openShowroomAdmin/)
  assert.doesNotMatch(source, /进入展厅前台/)
  assert.doesNotMatch(source, /openShowroomFrontstage/)
  assert.doesNotMatch(source, /展厅前台或后台/)
  assert.doesNotMatch(source, /\/showroom\/display\/screen\/home/)
  assert.doesNotMatch(source, /\/showroom\/home/)
  assert.match(source, /router\.push\('\/showroom\/company'\)/)
  assert.doesNotMatch(source, /\/showroom-admin\/company/)
})

test('home page keeps the existing welcome and guidance content', () => {
  const source = readText('src/views/Home/Index.vue')

  assert.match(source, /欢迎使用 IntRuoyi 管理后台/)
  assert.match(source, /当前登录：/)
  assert.match(source, /系统已就绪/)
  assert.match(source, /通过左侧菜单进入业务页面/)
  assert.match(source, /MES、ERP、DCC/)
})
