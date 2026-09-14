import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const read = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(frontendRoot, relativePath))

const companyScopePagePath = 'src/views/mdm/company-scope/index.vue'
const historicalImportPagePath = 'src/views/dcc/registration-certificate/historical-import/index.vue'
const companyScopeApiPath = 'src/api/mdm/companyScope/index.ts'
const historicalImportApiPath = 'src/api/dcc/registrationCertificate/historicalImport.ts'

assert.ok(exists(companyScopePagePath), '企业公司范围菜单必须有可解析的页面组件')
assert.ok(exists(historicalImportPagePath), '注册证历史导入菜单必须有可解析的页面组件')
assert.ok(exists(companyScopeApiPath), '企业公司范围页面必须使用正式后端 API')
assert.ok(exists(historicalImportApiPath), '注册证历史导入页面必须使用正式后端 API')

const companyScopePage = read(companyScopePagePath)
const historicalImportPage = read(historicalImportPagePath)
const companyScopeApi = read(companyScopeApiPath)
const historicalImportApi = read(historicalImportApiPath)

assert.match(companyScopePage, /defineOptions\(\{\s*name:\s*['"]MdmCompanyScope['"]\s*\}\)/)
assert.match(companyScopePage, /data-testid="mdm-company-scope-page"/)
assert.match(companyScopePage, /getCompanyScopePage/)
assert.match(companyScopeApi, /url:\s*['"]\/mdm\/company-scope\/page['"]/)
assert.match(companyScopePage, /scopeType/)
assert.match(companyScopePage, /companyName/)

assert.match(
  historicalImportPage,
  /defineOptions\(\{\s*name:\s*['"]DccRegistrationCertificateHistoricalImport['"]\s*\}\)/
)
assert.match(historicalImportPage, /data-testid="dcc-registration-certificate-historical-import-page"/)
assert.match(historicalImportPage, /getHistoricalImportPage/)
assert.match(historicalImportApi, /url:\s*['"]\/dcc\/registration-certificates\/historical-import\/page['"]/)
assert.match(historicalImportPage, /sourceHash/)
assert.match(historicalImportPage, /restrictedReasons/)

console.log('PASS: registration-certificate import tabs static contract')
