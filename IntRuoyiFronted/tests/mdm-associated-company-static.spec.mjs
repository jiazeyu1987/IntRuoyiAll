import { readFileSync, existsSync } from 'node:fs'
import { join, resolve } from 'node:path'
import assert from 'node:assert/strict'

const workingRoot = resolve(process.cwd())
const frontendRoot = existsSync(join(workingRoot, 'src'))
  ? workingRoot
  : join(workingRoot, 'IntRuoyiFronted')
const backendRoot = existsSync(join(workingRoot, 'IntRuoyiBackend'))
  ? join(workingRoot, 'IntRuoyiBackend')
  : resolve(frontendRoot, '..', 'IntRuoyiBackend')

const read = (path) => readFileSync(path, 'utf8')
const normalize = (content) => content.replace(/\s+/g, ' ')

const apiPath = join(frontendRoot, 'src/api/mdm/enterprise/index.ts')
const pagePath = join(frontendRoot, 'src/views/mdm/enterprise/index.vue')
const menuSqlPath = join(backendRoot, 'sql/mysql/20260829_mdm_associated_company_menu.sql')
const uploadServicePath = join(
  backendRoot,
  'yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/upload/DccRegistrationCertificateUploadService.java'
)

assert.equal(existsSync(apiPath), true, 'MDM enterprise frontend API wrapper must exist')
assert.equal(existsSync(pagePath), true, 'MDM enterprise page must exist')
assert.equal(existsSync(menuSqlPath), true, 'associated company menu migration must exist')

const api = read(apiPath)
assert.match(api, /MDM_ENTERPRISE_TYPE_OWNED_COMPANY\s*=\s*'OWNED_COMPANY'/)
assert.match(api, /MDM_ENTERPRISE_TYPE_ENTRUSTED_PARTY\s*=\s*'ENTRUSTED_PARTY'/)
assert.match(api, /MDM_ENTERPRISE_STATUS_ENABLE\s*=\s*'ENABLE'/)
assert.match(api, /MDM_ENTERPRISE_STATUS_DISABLE\s*=\s*'DISABLE'/)
assert.match(normalize(api), /getEnterprisePage\s*=\s*async\s*\([^)]*params:[^)]*MdmEnterprisePageReqVO[^)]*\)[^{]*=>\s*\{[^}]*url:\s*'\/mdm\/enterprise\/page'/)
assert.match(api, /url:\s*'\/mdm\/enterprise\/get'/)
assert.match(api, /url:\s*'\/mdm\/enterprise\/create'/)
assert.match(api, /url:\s*'\/mdm\/enterprise\/update'/)
assert.match(api, /\/mdm\/enterprise\/delete/)
assert.match(api, /url:\s*'\/mdm\/enterprise\/update-status'/)
assert.match(api, /url:\s*'\/mdm\/enterprise\/simple-list'/)

const page = read(pagePath)
assert.match(page, /defineOptions\(\{\s*name:\s*'MdmEnterprise'\s*\}\)/)
assert.match(page, /基础数据\s*\/\s*关联公司/)
assert.match(page, /table-key="mdm\.enterprise\.main"/)
assert.match(page, /v-hasPermi="\['mdm:enterprise:create'\]"/)
assert.match(page, /v-hasPermi="\['mdm:enterprise:update'\]"/)
assert.match(page, /v-hasPermi="\['mdm:enterprise:delete'\]"/)
assert.match(page, /label="公司编码"/)
assert.match(page, /label="公司名称"/)
assert.match(page, /label="公司类型"/)
assert.match(page, /label="状态"/)
assert.match(page, /新增关联公司/)
assert.match(page, /编辑关联公司/)
assert.match(page, /handleDelete/)
assert.match(page, /MdmEnterpriseApi\.createEnterprise/)
assert.match(page, /MdmEnterpriseApi\.updateEnterprise/)
assert.match(page, /MdmEnterpriseApi\.deleteEnterprise/)
assert.match(page, /MdmEnterpriseApi\.updateEnterpriseStatus/)

const menuSql = read(menuSqlPath)
assert.match(menuSql, /release-migration:/)
assert.match(menuSql, /CONVERT\(UNHEX\('E585B3E88194E585ACE58FB8'\) USING utf8mb4\)/)
assert.match(menuSql, /mdm\/enterprise\/index/)
assert.match(menuSql, /MdmEnterprise/)
assert.match(menuSql, /mdm:enterprise:query/)
assert.match(menuSql, /mdm:enterprise:create/)
assert.match(menuSql, /mdm:enterprise:update/)
assert.match(menuSql, /mdm:enterprise:delete/)
assert.match(menuSql, /system_tenant_package/)
assert.match(menuSql, /system_role_menu/)

const uploadService = read(uploadServicePath)
assert.match(uploadService, /MdmEnterpriseTypeEnum\.OWNED_COMPANY\.getType\(\)/)
assert.match(uploadService, /companyScopeApi\.getEnabledCompanyIdsForUser\(actorId\)/)
assert.match(uploadService, /enterpriseApi\.getEnabledEnterprises\(companyIds/)
