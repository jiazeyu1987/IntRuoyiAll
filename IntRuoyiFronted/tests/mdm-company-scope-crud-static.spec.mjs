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
const enterpriseServicePath = join(
  backendRoot,
  'yudao-module-mdm/src/main/java/cn/iocoder/yudao/module/mdm/service/enterprise/MdmEnterpriseService.java'
)
const saveVoPath = join(
  backendRoot,
  'yudao-module-mdm/src/main/java/cn/iocoder/yudao/module/mdm/controller/admin/companyscope/vo/MdmCompanyScopeSaveReqVO.java'
)
const menuSqlPath = join(backendRoot, 'sql/mysql/20260830_mdm_company_scope_crud_menu.sql')

for (const file of [apiPath, pagePath, controllerPath, servicePath, enterpriseServicePath, saveVoPath, menuSqlPath]) {
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
assert.doesNotMatch(page, /已启用的所属公司自动可用/, 'page must not show the removed notice')
assert.match(page, /getOwnedCompanyPage\(queryParams\)/, 'page must query owned company master data')
assert.match(page, /row-key="id"/, 'each company must use its master ID')
assert.match(page, /v-hasPermi="\['mdm:company-scope:create'\]"/, 'new button must require create permission')
assert.match(page, /createEnabledCompany\(formData\.name\.trim\(\)\)/, 'new company form must submit only its name')
assert.match(page, /updateOwnedCompany\(editingCompanyId\.value, formData\.name\.trim\(\)\)/, 'edit must only update the company name')
assert.match(page, /updateOwnedCompanyStatus\(row\.id, nextStatus\)/, 'status action must update company status')
assert.match(page, /新建所属公司.*编辑所属公司/, 'page must provide creation and editing forms')
assert.doesNotMatch(page, /UserSelectV2|RoleSelect|授权类型|授权公司/, 'company form must not ask for authorization mappings')
const createForm = page.slice(page.indexOf('<el-dialog'), page.indexOf('</el-dialog>'))
assert.doesNotMatch(createForm, /prop="enterpriseCode"|prop="type"|prop="status"/, 'company code and defaults must stay out of the form')
assert.doesNotMatch(page, /createCompanyScope|updateCompanyScope|deleteCompanyScope|getCompanyScopePage/, 'page must not use authorization mappings')
assert.doesNotMatch(page, /key: 'scopeType'|key: 'principalName'|key: 'principalCode'/, 'obsolete columns and filters must be removed')
assert.match(api, /url: ['"]\/mdm\/company-scope\/companies['"]/, 'page must use its company endpoint')
assert.match(api, /createEnabledCompany\s*=\s*async/, 'page API must expose company creation')
assert.match(api, /updateOwnedCompany\s*=\s*async/, 'page API must expose company editing')
assert.match(api, /updateOwnedCompanyStatus\s*=\s*async/, 'page API must expose company status updates')

const controller = read(controllerPath)
assert.match(controller, /@PostMapping\("\/create"\)/, 'backend must expose create endpoint')
assert.match(controller, /@PutMapping\("\/update"\)/, 'backend must expose update endpoint')
assert.match(controller, /@DeleteMapping\("\/delete"\)/, 'backend must expose delete endpoint')
assert.match(controller, /@PostMapping\("\/companies"\)/, 'backend must expose company creation')
assert.match(controller, /@PutMapping\("\/companies"\)/, 'backend must expose company editing')
assert.match(controller, /@PutMapping\("\/companies\/status"\)/, 'backend must expose company status updates')
assert.match(controller, /mdm:company-scope:create/, 'create endpoint must require create permission')
assert.match(controller, /mdm:company-scope:update/, 'update endpoint must require update permission')
assert.match(controller, /mdm:company-scope:delete/, 'delete endpoint must require delete permission')

const service = read(servicePath)
assert.match(service, /createCompanyScope\(/, 'service must own generic create behavior for the page')
assert.match(service, /updateCompanyScope\(/, 'service must own generic update behavior for the page')
assert.match(service, /deleteCompanyScope\(/, 'service must own generic delete behavior for the page')
assert.match(read(enterpriseServicePath), /createEnabledOwnedCompany\(/, 'enterprise service must create automatic-code owned companies')

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

console.log('PASS: mdm enabled company read-only page and retained API contracts')
