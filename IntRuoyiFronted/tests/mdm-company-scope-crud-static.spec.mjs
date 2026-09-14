import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join, resolve } from 'node:path'

const workingRoot = resolve(process.cwd())
const frontendRoot = existsSync(join(workingRoot, 'src'))
  ? workingRoot
  : join(workingRoot, 'IntRuoyiFronted')
const backendRoot = existsSync(join(workingRoot, 'IntRuoyiBackend'))
  ? join(workingRoot, 'IntRuoyiBackend')
  : resolve(frontendRoot, '..', 'IntRuoyiBackend')

const read = (path) => readFileSync(path, 'utf8')

const apiPath = join(frontendRoot, 'src/api/mdm/companyScope/index.ts')
const pagePath = join(frontendRoot, 'src/views/mdm/company-scope/index.vue')
const controllerPath = join(
  backendRoot,
  'yudao-module-mdm/src/main/java/cn/iocoder/yudao/module/mdm/controller/admin/companyscope/MdmCompanyScopeController.java'
)
const servicePath = join(
  backendRoot,
  'yudao-module-mdm/src/main/java/cn/iocoder/yudao/module/mdm/service/companyscope/MdmCompanyScopeService.java'
)
const saveVoPath = join(
  backendRoot,
  'yudao-module-mdm/src/main/java/cn/iocoder/yudao/module/mdm/controller/admin/companyscope/vo/MdmCompanyScopeSaveReqVO.java'
)
const menuSqlPath = join(backendRoot, 'sql/mysql/20260830_mdm_company_scope_crud_menu.sql')

for (const file of [apiPath, pagePath, controllerPath, servicePath, saveVoPath, menuSqlPath]) {
  assert.equal(existsSync(file), true, `${file} must exist`)
}

const api = read(apiPath)
assert.match(api, /interface\s+MdmCompanyScopeSaveReqVO/, 'frontend API must expose the save contract')
assert.match(api, /createCompanyScope\s*=\s*async/, 'frontend API must expose create')
assert.match(api, /url:\s*['"]\/mdm\/company-scope\/create['"]/, 'create must call the formal create endpoint')
assert.match(api, /updateCompanyScope\s*=\s*async/, 'frontend API must expose update')
assert.match(api, /url:\s*['"]\/mdm\/company-scope\/update['"]/, 'update must call the formal update endpoint')
assert.match(api, /deleteCompanyScope\s*=\s*async/, 'frontend API must expose delete')
assert.match(api, /url:\s*['"]\/mdm\/company-scope\/delete['"]/, 'delete must call the formal delete endpoint')

const page = read(pagePath)
assert.match(page, /基础数据\s*\/\s*授权公司/, 'page title must make authorization scope clear')
assert.match(page, /新增授权公司/, 'page must let admins create authorized company mappings')
assert.match(page, /编辑授权公司/, 'page must let admins edit authorized company mappings')
assert.match(page, /删除授权公司/, 'page must let admins delete authorized company mappings')
assert.match(page, /UserSelectV2/, 'user-scope mappings must be selected from real system users')
assert.match(page, /RoleSelect/, 'role-scope mappings must be selected from real system roles')
assert.match(page, /getEnterpriseSimpleList/, 'company choices must come from formal associated-company master data')
assert.match(page, /MDM_ENTERPRISE_TYPE_OWNED_COMPANY/, 'company choices must be owned-company associated companies')
assert.match(page, /MdmCompanyScopeApi\.createCompanyScope/, 'create action must call the formal API')
assert.match(page, /MdmCompanyScopeApi\.updateCompanyScope/, 'update action must call the formal API')
assert.match(page, /MdmCompanyScopeApi\.deleteCompanyScope/, 'delete action must call the formal API')
assert.match(page, /v-hasPermi="\['mdm:company-scope:create'\]"/, 'create action must be permission guarded')
assert.match(page, /v-hasPermi="\['mdm:company-scope:update'\]"/, 'update action must be permission guarded')
assert.match(page, /v-hasPermi="\['mdm:company-scope:delete'\]"/, 'delete action must be permission guarded')

const controller = read(controllerPath)
assert.match(controller, /@PostMapping\("\/create"\)/, 'backend must expose create endpoint')
assert.match(controller, /@PutMapping\("\/update"\)/, 'backend must expose update endpoint')
assert.match(controller, /@DeleteMapping\("\/delete"\)/, 'backend must expose delete endpoint')
assert.match(controller, /mdm:company-scope:create/, 'create endpoint must require create permission')
assert.match(controller, /mdm:company-scope:update/, 'update endpoint must require update permission')
assert.match(controller, /mdm:company-scope:delete/, 'delete endpoint must require delete permission')

const service = read(servicePath)
assert.match(service, /createCompanyScope\(/, 'service must own generic create behavior for the page')
assert.match(service, /updateCompanyScope\(/, 'service must own generic update behavior for the page')
assert.match(service, /deleteCompanyScope\(/, 'service must own generic delete behavior for the page')

const menuSql = read(menuSqlPath)
assert.match(menuSql, /授权公司/, 'menu migration must create a visible authorized-company page')
assert.match(menuSql, /mdm\/company-scope\/index/, 'menu migration must route to the company scope page')
for (const permission of [
  'mdm:company-scope:query',
  'mdm:company-scope:create',
  'mdm:company-scope:update',
  'mdm:company-scope:delete'
]) {
  assert.match(menuSql, new RegExp(permission), `menu migration must grant ${permission}`)
}
assert.match(menuSql, /system_tenant_package/, 'menu migration must add menu ids to tenant packages')
assert.match(menuSql, /system_role_menu/, 'menu migration must add menu ids to admin role menus')

console.log('PASS: mdm company scope CRUD static contract')
