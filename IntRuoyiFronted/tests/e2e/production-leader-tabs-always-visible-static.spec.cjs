const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const page = fs.readFileSync(
  path.join(process.cwd(), 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const productionTabStrips = [
  ...page.matchAll(/<el-tabs\b[^>]*data-production-leader-module-tabs[^>]*>/g)
].map((match) => match[0])

assert.ok(productionTabStrips.length > 0, '生产组长功能页签容器必须存在。')
for (const tabStrip of productionTabStrips) {
  assert.match(
    tabStrip,
    /v-if="showProductionModuleTabs"/,
    '每个生产组长功能页签容器都必须只由生产组长模块开关控制显示。'
  )
  assert.doesNotMatch(
    tabStrip,
    /v-if="showProductionResponsibleRoutes"/,
    '负责路线加载状态或空数据不能隐藏生产组长功能页签。'
  )
}

assert.match(
  page,
  /const\s+showProductionModuleTabs\s*=\s*computed\([\s\S]*props\.showProductionModuleTabs[\s\S]*activeLeaderTab\.value\s*===\s*'PRODUCTION'/,
  '生产组长功能页签开关必须继续受模块配置和当前组长类型控制。'
)

console.log('PASS: production leader module tabs remain visible independently of responsible routes')
