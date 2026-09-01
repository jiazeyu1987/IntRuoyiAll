import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const backendRoot = path.resolve(process.cwd())

function read(relativePath) {
  return fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')
}

const providerPath = path.join(
  backendRoot,
  'yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/config/ErpInvoiceVoucherPrintKingdeeConfigProvider.java'
)

assert.ok(
  fs.existsSync(providerPath),
  'ERP module must provide a Spring bean for InvoiceVoucherPrintKingdeeConfigProvider'
)

const providerSource = fs.readFileSync(providerPath, 'utf8')
const authControllerSource = read(
  'yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/auth/AuthController.java'
)
const responseSource = read(
  'yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/auth/vo/AuthInvoiceVoucherPrintTicketValidateRespVO.java'
)
const localYaml = read('yudao-server/src/main/resources/application-local.yaml')

assert.match(providerSource, /@Service/)
assert.match(providerSource, /implements\s+InvoiceVoucherPrintKingdeeConfigProvider/)
assert.match(providerSource, /ErpKingdeeConfigService/)
assert.match(providerSource, /getEffectiveProperties\(\)/)
assert.match(providerSource, /KingdeeConfigSnapshot\.builder\(\)/)
for (const field of ['BaseUrl', 'AcctId', 'Username', 'Password', 'AppId', 'AppSecret', 'Lcid']) {
  assert.match(providerSource, new RegExp(`get${field}\\(\\)`))
}
assert.match(providerSource, /requireNotBlank\(properties\.getAppId\(\),\s*"appId"\)/)
assert.match(providerSource, /requireNotBlank\(properties\.getAppSecret\(\),\s*"appSecret"\)/)
assert.doesNotMatch(providerSource, /System\.getenv|@Value|KINGDEE_ENV_PATH/)

assert.match(authControllerSource, /respVO\.setKingdeeConfig\(buildInvoiceVoucherPrintKingdeeConfig\(\)\)/)
assert.match(responseSource, /private\s+KingdeeConfig\s+kingdeeConfig/)
assert.match(responseSource, /private\s+String\s+appId/)
assert.match(responseSource, /private\s+String\s+appSecret/)
assert.match(localYaml, /app-id:\s+\$\{PRODUCTION_PLAN_ERP_K3CLOUD_APP_ID:/)
assert.match(localYaml, /app-secret:\s+\$\{PRODUCTION_PLAN_ERP_K3CLOUD_APP_SECRET:/)

console.log('invoice voucher print Kingdee config bridge static contract passed')
