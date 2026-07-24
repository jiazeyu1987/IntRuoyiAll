const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const routeFormSource = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteForm.vue'),
  'utf8'
)

if (routeFormSource.includes('width="1320px"')) {
  throw new Error('工艺路线主弹框仍使用固定 1320px 宽度，无法填满屏幕。')
}

if (!routeFormSource.includes('route-form-dialog-width')) {
  throw new Error('工艺路线主弹框缺少统一的满屏宽度常量。')
}

if (!routeFormSource.includes('calc(100vw - 32px)')) {
  throw new Error('工艺路线主弹框宽度应使用视口满宽约束 calc(100vw - 32px)。')
}
