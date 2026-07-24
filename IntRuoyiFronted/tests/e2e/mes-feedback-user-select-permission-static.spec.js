const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const componentPath = path.resolve(
  process.cwd(),
  'src/views/system/user/components/UserSelectV2.vue'
)
const apiPath = path.resolve(process.cwd(), 'src/api/system/user/index.ts')

assert(fs.existsSync(componentPath), `UserSelectV2 必须存在：${componentPath}`)
assert(fs.existsSync(apiPath), `系统用户 API 文件必须存在：${apiPath}`)

const componentSource = fs.readFileSync(componentPath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert(
  componentSource.includes('UserApi.getSimpleUserList()'),
  'UserSelectV2 回显用户时必须走无需 system:user:query 的 simple-list 接口。'
)

assert(
  !componentSource.includes('UserApi.getUserList(ids)'),
  'UserSelectV2 不应继续用 /system/user/list 做 ID 回显，否则业务页会因缺少 system:user:query 而误报无权限。'
)

assert(
  apiSource.includes("return request.get({ url: '/system/user/simple-list' })"),
  '系统用户精简列表接口必须继续指向 /system/user/simple-list。'
)

console.log('PASS: MES feedback user select permission static contract')
