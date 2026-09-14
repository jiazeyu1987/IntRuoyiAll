const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(frontRoot, '..')

const readFront = (relativePath) => {
  const absolutePath = path.join(frontRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing frontend file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')
}

const readRepo = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing repo file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')
}

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const assertNavigationBlock = (source, functionName, pathValue, queryKey, queryValuePattern) => {
  const block = extractBetween(source, `const ${functionName}`, '\n}\n')
  assert.match(block, new RegExp(`path:\\s*['"]${pathValue.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}['"]`),
    `${functionName} must navigate to ${pathValue}`)
  assert.match(block, new RegExp(`${queryKey}:\\s*String\\(${queryValuePattern}\\)`),
    `${functionName} must pass ${queryKey} as a string query identity`)
  assert.doesNotMatch(block, /Number\s*\(|parseInt\s*\(/, `${functionName} must not numeric-cast database ids`)
}

const projectCodeApi = readFront('src/api/dcc/controlledFile/projectCodes.ts')
const projectCodePage = readFront('src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue')
const mdmProductApi = readFront('src/api/mdm/product/index.ts')
const mdmProductPage = readFront('src/views/mdm/product/index.vue')
const registrationApi = readFront('src/api/dcc/registrationCertificate/index.ts')
const registrationPage = readFront('src/views/dcc/registration-certificate/index/index.vue')

const projectCodeReq = readRepo('IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/vo/DccProjectCodePageReqVO.java')
const projectCodeMapper = readRepo('IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/projectcode/DccProjectCodeMapper.java')
const mdmProductReq = readRepo('IntRuoyiBackend/yudao-module-mdm/src/main/java/cn/iocoder/yudao/module/mdm/controller/admin/product/vo/MdmProductPageReqVO.java')
const mdmProductMapper = readRepo('IntRuoyiBackend/yudao-module-mdm/src/main/java/cn/iocoder/yudao/module/mdm/dal/mysql/product/MdmProductMapper.java')
const certificateReq = readRepo('IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/controller/admin/query/vo/DccRegistrationCertificatePageReqVO.java')
const certificateQuery = readRepo('IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificatePageQuery.java')
const certificatePageItem = readRepo('IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificatePageItem.java')
const certificateOldItem = readRepo('IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificateOldIndexItem.java')
const certificateMapper = readRepo('IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/dal/mysql/DccRegistrationCertificateQueryMapper.java')
const certificateService = readRepo('IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificateQueryServiceImpl.java')

assert.match(projectCodeApi, /productMasterId\?:\s*number\s*\|\s*string/, 'project-code page request must accept productMasterId query')
assert.match(projectCodeReq, /private\s+Long\s+productMasterId;/, 'backend project-code request must accept productMasterId')
assert.match(projectCodeMapper, /eqIfPresent\(DccProjectCodeDO::getProductMasterId,\s*reqVO\.getProductMasterId\(\)\)/,
  'backend project-code page must filter by productMasterId')
assert.match(projectCodePage, /queryParams\.productMasterId\s*=\s*resolveRouteQueryText\(route\.query\.productMasterId\)/,
  'project-code page must sync linked productMasterId route query into the formal page request')
assert.match(projectCodePage, /const\s+resolveQueryProjectCodeId\s*=\s*\(\)\s*=>\s*resolvePositiveRouteQueryText\(route\.query\.projectCodeId\)/,
  'project-code page must parse linked projectCodeId route query as a string identity')
assert.match(projectCodePage, /const\s+syncDetailFromRoute\s*=\s*async\s*\(\)\s*=>\s*\{[\s\S]*const\s+projectCode\s*=\s*await\s+getProjectCode\(id\)[\s\S]*selectedProjectCode\.value\s*=\s*projectCode/,
  'project-code page must open the linked project-code detail drawer from projectCodeId')
assert.match(projectCodePage, /watch\(\s*\(\)\s*=>\s*\[route\.path,\s*route\.query\.projectCodeId\],[\s\S]*if\s*\(!isProjectCodeRoute\(\)\)\s*\{[\s\S]*return[\s\S]*await\s+syncDetailFromRoute\(\)/,
  'project-code page must resync the detail drawer only when linked projectCodeId changes on the project-code route')
assert.match(projectCodePage, /const\s+PROJECT_CODE_ROUTE_PATH\s*=\s*['"]\/mes\/md\/dcc-project-code['"]/,
  'project-code route path must be explicit')
assert.match(projectCodePage, /const\s+isProjectCodeRoute\s*=\s*\(\)\s*=>\s*route\.path\s*===\s*PROJECT_CODE_ROUTE_PATH/,
  'project-code query sync must be scoped to the active project-code route')
assert.match(projectCodePage, /onActivated\(\s*async\s*\(\)\s*=>\s*\{[\s\S]*if\s*\(!isProjectCodeRoute\(\)\)\s*\{[\s\S]*return[\s\S]*syncProjectCodeQueryFromRoute\(\)[\s\S]*await\s+getList\(\)/,
  'project-code cached activation must reload the linked filter when returning from product or registration pages')
assert.match(projectCodePage, /watch\(\s*\(\)\s*=>\s*\[route\.path,\s*route\.query\.productMasterId\],[\s\S]*if\s*\(!isProjectCodeRoute\(\)\)\s*\{[\s\S]*return/,
  'project-code productMasterId watcher must ignore same-named query keys while another page is active')
assert.match(projectCodePage, /@click="openLinkedProductManagement\(row\)"/,
  'project-code rows must expose a product-management jump')
assert.match(projectCodePage, /@click="openLinkedRegistrationCertificateManagement\(row\)"/,
  'project-code rows must expose a registration-certificate jump')
assertNavigationBlock(projectCodePage, 'openLinkedProductManagement', '/mes/md/showroom-product', 'productMasterId', 'row\\.productMasterId')
assertNavigationBlock(projectCodePage, 'openLinkedRegistrationCertificateManagement', '/mdm/registration-certificate', 'projectCodeId', 'row\\.id')

assert.match(mdmProductApi, /productMasterId\?:\s*number\s*\|\s*string/, 'product page request must accept productMasterId query')
assert.match(mdmProductReq, /private\s+Long\s+productMasterId;/, 'backend product request must accept productMasterId')
assert.match(mdmProductMapper, /eqIfPresent\(MdmProductDO::getId,\s*reqVO\.getProductMasterId\(\)\)/,
  'backend product page must filter by product master id')
assert.match(mdmProductPage, /useRoute\(\)/, 'product management page must read route query for linked entry')
assert.match(mdmProductPage, /queryParams\.productMasterId\s*=\s*resolveRouteQueryText\(route\.query\.productMasterId\)/,
  'product management page must sync productMasterId route query into the formal page request')
assert.match(mdmProductPage, /const\s+PRODUCT_ROUTE_PATH\s*=\s*['"]\/mes\/md\/showroom-product['"]/,
  'product route path must be explicit')
assert.match(mdmProductPage, /const\s+isProductRoute\s*=\s*\(\)\s*=>\s*route\.path\s*===\s*PRODUCT_ROUTE_PATH/,
  'product query sync must be scoped to the active product route')
assert.match(mdmProductPage, /onActivated\(\s*async\s*\(\)\s*=>\s*\{[\s\S]*if\s*\(!isProductRoute\(\)\)\s*\{[\s\S]*return[\s\S]*syncProductQueryFromRoute\(\)[\s\S]*await\s+getList\(\)/,
  'product cached activation must reload linked filters when returning from other pages')
assert.match(mdmProductPage, /watch\(\s*\(\)\s*=>\s*\[route\.path,\s*route\.query\.productMasterId\],[\s\S]*if\s*\(!isProductRoute\(\)\)\s*\{[\s\S]*return/,
  'product watcher must ignore same-named query keys while another page is active')
assert.match(mdmProductPage, /@click="openLinkedProjectCodeManagement\(row\)"/,
  'product rows must expose a project-code jump')
assert.match(mdmProductPage, /@click="openLinkedRegistrationCertificateManagement\(row\)"/,
  'product rows must expose a registration-certificate jump')
assertNavigationBlock(mdmProductPage, 'openLinkedProjectCodeManagement', '/mes/md/dcc-project-code', 'productMasterId', 'row\\.id')
assertNavigationBlock(mdmProductPage, 'openLinkedRegistrationCertificateManagement', '/mdm/registration-certificate', 'productMasterId', 'row\\.id')

assert.match(registrationApi, /projectCodeId\?:\s*number\s*\|\s*string/, 'registration page request must accept projectCodeId query')
assert.match(registrationApi, /productMasterId:\s*number\s*\|\s*string/, 'registration current rows must expose productMasterId')
assert.match(registrationApi, /projectCodeId\?:\s*number\s*\|\s*string/, 'registration current rows must expose projectCodeId')
assert.match(certificateReq, /private\s+Long\s+projectCodeId;/, 'backend registration request must accept projectCodeId')
assert.match(certificateQuery, /private\s+Long\s+projectCodeId;/, 'backend registration query must carry projectCodeId')
for (const source of [certificatePageItem, certificateOldItem]) {
  assert.match(source, /private\s+Long\s+productMasterId;/, 'registration list items must expose productMasterId')
  assert.match(source, /private\s+Long\s+projectCodeId;/, 'registration list items must expose projectCodeId')
}
assert.match(certificateMapper, /AND\s+c\.project_code_id\s*=\s*#\{query\.projectCodeId\}/,
  'registration mapper must filter by projectCodeId')
assert.match(certificateService, /\.productMasterId\(row\.getProductMasterId\(\)\)[\s\S]*\.projectCodeId\(row\.getProjectCodeId\(\)\)/,
  'registration service must copy linked product and project ids to list rows')
assert.match(registrationPage, /useRoute\(\)/, 'registration page must read route query for linked entry')
assert.match(registrationPage, /queryParams\.projectCodeId\s*=\s*resolveRouteQueryText\(route\.query\.projectCodeId\)/,
  'registration page must sync projectCodeId route query into current list request')
assert.match(registrationPage, /const\s+REGISTRATION_CERTIFICATE_ROUTE_PATH\s*=\s*['"]\/mdm\/registration-certificate['"]/,
  'registration certificate route path must be explicit')
assert.match(registrationPage, /const\s+isRegistrationCertificateRoute\s*=\s*\(\)\s*=>\s*route\.path\s*===\s*REGISTRATION_CERTIFICATE_ROUTE_PATH/,
  'registration certificate query sync must be scoped to the active registration route')
assert.match(registrationPage, /onActivated\(\s*async\s*\(\)\s*=>\s*\{[\s\S]*if\s*\(!isRegistrationCertificateRoute\(\)\)\s*\{[\s\S]*return[\s\S]*syncRegistrationCertificateQueryFromRoute\(\)[\s\S]*await\s+loadPage\(\)/,
  'registration cached activation must reload linked filters when returning from product or project-code pages')
assert.match(registrationPage, /watch\(\s*\(\)\s*=>\s*\[route\.path,\s*route\.query\.productMasterId,\s*route\.query\.projectCodeId\],[\s\S]*if\s*\(!isRegistrationCertificateRoute\(\)\)\s*\{[\s\S]*return/,
  'registration watcher must ignore same-named query keys while another page is active')
assert.match(registrationPage, /@click="openLinkedProductManagement\(row\.productMasterId\)"/,
  'registration rows must expose a product-management jump')
assert.match(registrationPage, /@click="openLinkedProjectCodeManagement\(row\.projectCodeId\)"/,
  'registration rows must expose a project-code jump')
assertNavigationBlock(registrationPage, 'openLinkedProductManagement', '/mes/md/showroom-product', 'productMasterId', 'productMasterId')
assertNavigationBlock(registrationPage, 'openLinkedProjectCodeManagement', '/mes/md/dcc-project-code', 'projectCodeId', 'projectCodeId')

for (const source of [projectCodePage, mdmProductPage, registrationPage]) {
  assert.doesNotMatch(source, /mock|defaultSuccess|fallback|降级|吞异常/,
    'cross-navigation must not introduce mock data or fallback success')
}

console.log('PASS: product master cross-navigation static contract')
