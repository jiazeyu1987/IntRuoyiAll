const fs = require('fs')
const path = require('path')
const assert = require('assert')

const page = fs.readFileSync(path.join(process.cwd(), 'src/views/mes/pro/route/index.vue'), 'utf8')
const api = fs.readFileSync(path.join(process.cwd(), 'src/api/mes/pro/route/index.ts'), 'utf8')

assert(
  page.includes('data-route-version-action="migrate-production-config"') &&
    page.includes('migrateLegacyProductionConfigFromFormalSource'),
  'RED: route version workspace must expose an explicit production-config migration action'
)

assert(
  page.includes('从现有生产组长正式配置迁移') &&
    page.includes('只创建草稿候选版本') &&
    page.includes('缺少任何正式配置时将阻止迁移'),
  'RED: migration confirmation must identify the source, draft result, and fail-fast behavior'
)

assert(
  api.includes('migrateLegacyProductionConfig?: boolean') &&
    api.includes('missingOveragePercent?: number') &&
    page.includes('migrateLegacyProductionConfig: true') &&
    page.includes('missingOveragePercent'),
  'RED: migration must use an explicit request flag without changing ordinary candidate creation'
)

assert(
  page.includes('缺失超量比例统一补值') && page.includes('message.prompt'),
  'RED: migration must require an explicit frontend value for missing historical overage limits'
)

console.log('GREEN: explicit route production-config migration entry is present')
