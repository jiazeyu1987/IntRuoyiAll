const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const apiSource = fs.readFileSync(path.join(root, 'src/api/pay/order/index.ts'), 'utf8')
const cashierSource = fs.readFileSync(path.join(root, 'src/views/pay/cashier/index.vue'), 'utf8')

assert.match(
  apiSource,
  /export const getOrder = async \(id: number\) =>/,
  '支付订单查询必须只接收订单编号'
)
assert.doesNotMatch(
  apiSource,
  /getOrder = async \(id: number,\s*sync\?: boolean\)/,
  '支付订单 GET 查询不得携带同步写入开关'
)
assert.match(
  apiSource,
  /export const syncOrder = async \(id: number\)[\s\S]*request\.post\(\{[\s\S]*url: '\/pay\/order\/sync'[\s\S]*params: \{ id \}[\s\S]*\}\)/,
  '支付订单同步必须使用独立 POST 命令'
)
assert.match(
  cashierSource,
  /const data = await PayOrderApi\.syncOrder\(id\.value\)/,
  '收银台首次加载需要通过显式同步命令刷新订单'
)
assert.doesNotMatch(
  cashierSource,
  /PayOrderApi\.getOrder\(id\.value,\s*true\)/,
  '收银台不得通过 GET 参数触发同步'
)
assert.match(
  cashierSource,
  /const data = await PayOrderApi\.getOrder\(id\.value\)/,
  '收银台轮询必须保持纯查询'
)

console.log('PASS: pay order read/write boundary static contract')
