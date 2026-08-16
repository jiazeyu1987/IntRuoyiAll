const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../../..')
const read = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')

const controller = read(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/md/item/MesMdItemController.java'
)
const service = read('src/main/java/cn/iocoder/yudao/module/mes/service/md/item/MesMdItemService.java')
const serviceImpl = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/md/item/MesMdItemServiceImpl.java'
)
const api = fs.readFileSync(
  path.resolve(moduleRoot, '../../IntRuoyiFronted/src/api/mes/md/item/index.ts'),
  'utf8'
)

assert.match(controller, /@GetMapping\("\/?get-by-code"\)/, '物料接口必须提供按编号精确查询入口')
assert.match(
  controller,
  /@RequestParam\("code"\)\s+@NotBlank\(message = "产品编号不能为空"\)\s+String code/,
  '按编号查询必须校验产品编号必填'
)
assert.match(
  controller,
  /itemService\.getItemByCode\(code\)/,
  'Controller 必须调用正式服务按编号查询'
)
assert.match(service, /MesMdItemDO\s+getItemByCode\(String code\);/, 'Service 必须声明按编号查询')
assert.match(serviceImpl, /itemMapper\.selectByCode\(code\)/, 'Service 实现必须使用精确编码查询')
assert.match(api, /getItemByCode:\s*async \(code: string\)/, '前端 API 必须暴露按编号查询方法')
assert.doesNotMatch(
  api,
  /getItemByCode:[\s\S]*getItemPage/,
  '前端按编号查询不得退回分页模糊查询'
)

console.log('mes-md-item-get-by-code-contract-static PASS')
