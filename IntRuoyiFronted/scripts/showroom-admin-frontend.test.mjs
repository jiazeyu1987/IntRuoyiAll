import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const sliceBetween = (source, startMarker, endMarker) => {
  const startIndex = source.indexOf(startMarker)
  if (startIndex === -1) {
    return ''
  }
  const endIndex = source.indexOf(endMarker, startIndex)
  return endIndex === -1 ? source.slice(startIndex) : source.slice(startIndex, endIndex)
}
const matchRouteBlock = (source, pathName, routeName) =>
  source.match(
    new RegExp(
      String.raw`\{\s*path: '${pathName}'[\s\S]*?name: '${routeName}'[\s\S]*?meta: \{[\s\S]*?\}\s*\}`,
      'm'
    )
  )?.[0] || ''

test('showroom-admin route module registers the back-office shell and children', () => {
  const source = readText('src/router/modules/showroom.ts')
  assert.match(source, /showroomRoutes/)
  assert.match(source, /path: '\/showroom'/)
  assert.match(source, /title: '展柜'/)
  assert.match(source, /ShowroomAdminCompany/)
  assert.match(source, /ShowroomAdminCompanyVersion/)
  assert.match(source, /ShowroomAdminProduct/)
  assert.match(source, /ShowroomAdminHall/)
  assert.match(source, /ShowroomAdminApproval/)
  assert.match(source, /ShowroomAdminHistory/)
  assert.match(source, /ShowroomAdminAssignment/)
  assert.match(source, /ShowroomAdminDiscussion/)
  assert.match(source, /ShowroomAdminNarration/)
  assert.doesNotMatch(source, /ShowroomDisplayHome/)
  assert.doesNotMatch(source, /ShowroomDisplayCompany/)
  assert.doesNotMatch(source, /ShowroomDisplaySettings/)
  assert.doesNotMatch(source, /ShowroomDisplayNarration/)
  assert.doesNotMatch(source, /前台大屏/)
  assert.match(source, /alwaysShow: true/)
  const parentRouteBlock = source.match(/name: 'Showroom',[\\s\\S]*?children:/)?.[0] || ''
  assert.doesNotMatch(parentRouteBlock, /hidden: true/)
  assert.doesNotMatch(source, /path: '\/showroom-admin'/)
  assert.doesNotMatch(source, /path: '\/showroom\/display'/)
})

test('showroom-admin history and narration routes use the shared back-office shell', () => {
  const routeSource = readText('src/router/modules/showroom.ts')
  const adminShellSource = readText('src/views/showroom-admin/index.vue')

  const historyBlock = matchRouteBlock(routeSource, 'history', 'ShowroomAdminHistory')
  const narrationBlock = matchRouteBlock(
    routeSource,
    'narration-workbench',
    'ShowroomAdminNarration'
  )

  assert.ok(historyBlock, 'history route block should exist')
  assert.ok(narrationBlock, 'narration route block should exist')
  assert.match(historyBlock, /component: showroomAdminView/)
  assert.match(narrationBlock, /component: showroomAdminView/)
  assert.doesNotMatch(routeSource, /showroomNarrationWorkbenchView/)
  assert.match(adminShellSource, /CompanyHistoryWorkbench/)
  assert.match(adminShellSource, /v-else-if="activeSection === 'history'"/)
  assert.match(adminShellSource, /NarrationWorkspace/)
  assert.match(adminShellSource, /v-else-if="activeSection === 'narration'"/)
})

test('showroom-admin api module exposes content workflow and narration endpoints', () => {
  const source = readText('src/api/showroom-admin/index.ts')
  assert.match(source, /getCompanyCurrent/)
  assert.match(source, /publishCompany/)
  assert.match(source, /generateCompanyNarrationScript/)
  assert.match(source, /publishProduct/)
  assert.match(source, /generateCompanyNarrationAudio/)
  assert.match(source, /publishCompanyNarration/)
  assert.match(source, /saveCompanyDraft/)
  assert.match(source, /submitCompany/)
  assert.match(source, /getProductPage/)
  assert.match(source, /url: '\/showroom\/product\/publish'/)
  assert.match(source, /ShowroomProductBatchGenerateReqVO/)
  assert.match(source, /ShowroomProductBatchGenerateRespVO/)
  assert.match(source, /batchPublishProducts/)
  assert.match(source, /url: '\/showroom\/product\/batch-publish'/)
  assert.match(source, /ShowroomProductSalesCountriesBatchGenerateRespVO/)
  assert.match(source, /batchGenerateProductSalesCountries/)
  assert.match(source, /url: '\/showroom\/product\/batch-generate-sales-countries'/)
  assert.doesNotMatch(source, /batch-generate-selling-points/)
  assert.match(source, /ShowroomProductBatchGenerateStateRespVO/)
  assert.match(source, /ShowroomProductCoverBatchTaskStateRespVO/)
  assert.match(source, /getProductBatchGenerateCoverImageState/)
  assert.match(source, /url: '\/showroom\/product\/batch-generate-cover-image-state'/)
  assert.match(source, /ShowroomProductNarrationScriptTaskCurrentProductRespVO/)
  assert.match(source, /ShowroomProductNarrationScriptTaskRespVO/)
  assert.match(source, /currentProduct\?: ShowroomProductNarrationScriptTaskCurrentProductRespVO \| null/)
  assert.match(source, /startBatchGenerateNarrationScriptTask/)
  assert.match(source, /getBatchGenerateNarrationScriptTaskStatus/)
  assert.match(source, /batchGenerateProductNarrationAudio/)
  assert.match(source, /getProductBatchGenerateNarrationAudioState/)
  assert.match(source, /batchGenerateProductCoverImage/)
  assert.match(source, /url: '\/showroom\/product\/batch-generate-narration-audio'/)
  assert.match(source, /url: '\/showroom\/product\/batch-generate-narration-audio-state'/)
  assert.match(source, /url: '\/showroom\/product\/batch-generate-narration-script\/start'/)
  assert.match(source, /url: '\/showroom\/product\/batch-generate-narration-script\/status'/)
  assert.match(source, /url: '\/showroom\/product\/batch-generate-cover-image'/)
  assert.match(source, /updateHallProductMapping/)
  assert.match(source, /createAssignment/)
  assert.match(source, /supervisorReject/)
  assert.match(source, /gaoxinReject/)
  assert.match(source, /generateNarrationScript/)
  assert.match(source, /generateNarrationAudio/)
  assert.match(source, /submitNarration/)
  assert.match(source, /supervisorApproveNarration/)
  assert.match(source, /gaoxinApproveNarration/)
  assert.match(source, /publishNarration/)
})

test('system notify detail exposes showroom product navigation from template params', () => {
  const source = readText('src/views/system/notify/my/MyNotifyMessageDetail.vue')
  const apiSource = readText('src/api/system/notify/message/index.ts')

  assert.match(apiSource, /templateParams: Record<string, unknown> \| null/)
  assert.match(source, /showroomProductNavigation/)
  assert.match(source, /notifyTargetType/)
  assert.match(source, /notifyTargetId/)
  assert.match(source, /notifyChangeRequestId/)
  assert.match(source, /rawNotifyOpen/)
  assert.match(source, /notifyOpen:\s*rawNotifyOpen === 'approval' \? 'approval' : rawNotifyOpen === 'edit' \? 'edit' : 'detail'/)
  assert.match(source, /ShowroomAdminProduct/)
  assert.match(source, /查看关联产品/)
  assert.match(source, /router\.resolve/)
  assert.match(source, /window\.location\.assign/)
})

test('showroom-admin back-office view exposes the expected management sections', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  assert.match(source, /CompanyWorkbench/)
  assert.match(source, /CompanyVersionWorkbench/)
  assert.match(source, /ApprovalTaskPanel/)
  assert.match(source, /版本历史/)
  assert.match(source, /AssignmentWorkbench/)
  assert.match(source, /DiscussionWorkbench/)
  assert.match(source, /NarrationWorkspace/)
  assert.match(source, /productNarrationScriptTaskStatus/)
  assert.match(source, /latestProductCoverTaskSummary/)
  assert.match(source, /loadProductCoverTaskState/)
  assert.match(source, /loadProductNarrationScriptTaskStatus/)
  assert.match(source, /handleBatchPublishProducts/)
  assert.match(source, /handleBatchGenerateProductSalesCountries/)
  assert.match(source, /handleStartBatchGenerateNarrationScriptTask/)
  assert.match(source, /:batch-generating-sales-countries="batchGeneratingProductSalesCountries"/)
  assert.match(source, /activeSection === 'companyVersion'/)
  assert.match(source, /activeSection === 'approval'/)
  assert.match(source, /activeSection === 'assignment'/)
  assert.match(source, /activeSection === 'discussion'/)
  assert.match(source, /activeSection === 'narration'/)
})

test('showroom-admin product page auto refreshes narration script task status while active', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /let productNarrationScriptTaskPollingTimer: number \| undefined/)
  assert.match(source, /const clearProductNarrationScriptTaskPolling = \(\) =>/)
  assert.match(source, /const syncProductNarrationScriptTaskPolling = \(\) =>/)
  assert.match(source, /productNarrationScriptTaskStatus\.value\?\.active \|\| productNarrationScriptTaskStatus\.value\?\.running/)
  assert.match(source, /window\.setInterval\(async \(\) =>/)
  assert.match(source, /await loadProductNarrationScriptTaskStatus\(\)/)
  assert.match(source, /onUnmounted\(\(\) => \{/)
})

test('showroom-admin removes redundant banner and uses maximum page size 20', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  assert.doesNotMatch(source, /showroom-admin-toolbar/)
  assert.doesNotMatch(source, /展柜后台/)
  assert.doesNotMatch(source, /结构化内容、审批、指派和讲解资产统一管理/)
  assert.match(source, /pageSize: 20/)
  assert.doesNotMatch(source, /pageSize: 99/)
})

test('showroom-admin content is selected by route without rendering duplicate inner tabs', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  assert.match(source, /showroomAdminSections/)
  assert.match(source, /route\.name/)
  assert.match(source, /activeSection/)
  assert.doesNotMatch(source, /showroom-admin-tabs/)
  assert.doesNotMatch(source, /handleAdminTabChange/)
  assert.doesNotMatch(source, /const activeTab = ref\('company'\)/)
  assert.doesNotMatch(source, /v-model="activeTab"/)
  assert.doesNotMatch(source, /@tab-change=|@update:model-value=.*activeTab/)
})

test('showroom-admin product and hall routes render real list components', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  const loadProductRowsBlock = sliceBetween(
    source,
    'const loadProductRows = async () => {',
    'const loadHallRows = async () => {'
  )
  assert.match(source, /ProductListTable/)
  assert.match(source, /HallListTable/)
  assert.match(source, /productRows/)
  assert.match(source, /hallRows/)
  assert.doesNotMatch(source, /const enrichProductRows = async/)
  assert.match(loadProductRowsBlock, /getProductPage/)
  assert.doesNotMatch(loadProductRowsBlock, /getProduct\(/)
  assert.doesNotMatch(loadProductRowsBlock, /Promise\.all/)
  assert.match(source, /createProduct/)
  assert.match(source, /saveProductDraft/)
  assert.match(source, /publishProduct/)
  assert.match(source, /deleteProduct/)
  assert.match(source, /generateProductNarrationAudio/)
  assert.match(source, /translateProductFieldsToEn/)
  assert.match(source, /batchPublishProducts/)
  assert.match(source, /createHall/)
  assert.match(source, /updateHall/)
  assert.match(source, /deleteHall/)
  assert.match(source, /activeSection === 'product'/)
  assert.match(source, /activeSection === 'hall'/)
  assert.doesNotMatch(source, /产品详情表/)
  assert.doesNotMatch(source, /展柜产品排序/)
  assert.doesNotMatch(source, /\$\{products\.length\} 个产品/)
  assert.doesNotMatch(source, /\$\{halls\.length\} 个展柜/)
})

test('showroom-admin product editor keeps bilingual product tabs while list owns publish entry', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  const productDialogSource =
    source.match(/<el-dialog[\s\S]*?v-model="productDialogVisible"[\s\S]*?<\/el-dialog>/)?.[0] || ''

  assert.match(source, /label="中文"/)
  assert.match(source, /label="English"/)
  assert.match(source, /AI Translate/)
  assert.match(source, /生成讲解稿/)
  assert.doesNotMatch(productDialogSource, /Generate Audio|生成语音/)
  assert.match(source, /handleGenerateProductNarrationAudioFromRow/)
  assert.match(source, /产品新版本已发布/)
  assert.match(source, /ShowroomAdminApi\.translateProductFieldsToEn/)
  assert.match(source, /ShowroomAdminApi\.publishProduct/)
  assert.match(source, /ShowroomAdminApi\.batchPublishProducts/)
  assert.match(source, /sourceRevisionId/)
  assert.match(source, /hasProductNarrationChanges/)
  assert.match(source, /isShowroomPublicity/)
  assert.doesNotMatch(source, /保存并发布/)
})

test('showroom-admin hall rows wire the mapping action into a real dialog workflow', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /HallProductMappingDialog/)
  assert.match(source, /@open-mapping=/)
  assert.match(source, /hallMappingDialogVisible/)
  assert.match(source, /activeHallMappingRecord/)
  assert.match(source, /:products="productRows"/)
  assert.match(source, /handleHallMappingSaved/)
})

test('showroom-admin company route renders the real company workbench instead of a summary row', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  assert.match(source, /CompanyWorkbench/)
  assert.match(source, /activeSection === 'company'/)
  assert.match(source, /<CompanyWorkbench/)
  assert.doesNotMatch(source, /code: 'company-structure'/)
  assert.doesNotMatch(source, /name: '公司结构化字段'/)
})

test('showroom-admin company-version route renders the dedicated company version workbench', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  const companySource = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')
  const versionSource = readText('src/views/showroom-admin/company-version/CompanyVersionWorkbench.vue')

  assert.match(source, /CompanyVersionWorkbench/)
  assert.match(source, /activeSection === 'companyVersion'/)
  assert.match(source, /routeName: 'ShowroomAdminCompanyVersion'/)
  assert.match(versionSource, /公司版本/)
  assert.match(versionSource, /版本历史/)
  assert.match(versionSource, /复制为最新版本/)
  assert.doesNotMatch(companySource, /当前版本与历史版本直接在公司信息页可见/)
  assert.doesNotMatch(companySource, /查看版本/)
  assert.doesNotMatch(companySource, /复制为最新版本/)
})

test('showroom-admin back-office view loads real API data and exposes errors', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  const loadShowroomAdminDataBlock = sliceBetween(
    source,
    'const loadShowroomAdminData = async () => {',
    'const handleOpenCurrentProductFromTask ='
  )
  assert.match(source, /ShowroomAdminApi/)
  assert.match(source, /loadShowroomAdminData/)
  assert.match(source, /getCompanyCurrent/)
  assert.match(source, /getProductPage/)
  assert.match(source, /getHallPage/)
  assert.match(source, /const shouldLoadProductRows = computed\(\(\) =>/)
  assert.match(source, /const shouldLoadHallRows = computed\(\(\) =>/)
  assert.match(loadShowroomAdminDataBlock, /if \(shouldLoadProductRows\.value\)/)
  assert.match(loadShowroomAdminDataBlock, /if \(shouldLoadHallRows\.value\)/)
  assert.doesNotMatch(loadShowroomAdminDataBlock, /Promise\.all\(\[loadProductRows\(\), loadHallRows\(\)\]\)/)
  assert.match(source, /batchPublishingProduct/)
  assert.match(source, /batchGeneratingProductSalesCountries/)
  assert.match(source, /:batch-publishing="batchPublishingProduct"/)
  assert.match(source, /@batch-generate-sales-countries="handleBatchGenerateProductSalesCountries"/)
  assert.match(source, /:cover-task-summary="latestProductCoverTaskSummary"/)
  assert.match(source, /@batch-publish="handleBatchPublishProducts"/)
  assert.match(source, /getProductBatchGenerateNarrationAudioState/)
  assert.match(source, /getProductBatchGenerateCoverImageState/)
  assert.match(source, /loadBatchProductAudioAutoCheckState/)
  assert.match(source, /adminLoadError/)
  assert.doesNotMatch(source, /const sectionRows = \{/)
  assert.doesNotMatch(source, /approval-route/)
})

test('showroom-admin product page keeps cover batch status in the fixed task area instead of toolbar tag', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  const listSource = readText('src/views/showroom-admin/components/ProductListTable.vue')

  assert.match(source, /latestProductCoverTaskSummary/)
  assert.match(source, /normalizeProductCoverTaskState/)
  assert.match(source, /syncProductCoverTaskPolling/)
  assert.match(listSource, /一键封面任务/)
  assert.match(listSource, /允许状态：/)
  assert.match(listSource, /当前执行产品：/)
  assert.match(listSource, /dismissCoverTaskBanner/)
  assert.match(listSource, /handleBatchGenerateCoverClick/)
  assert.match(listSource, /coverTaskVisible/)
  assert.match(listSource, /coverTaskStateLabel/)
  assert.match(listSource, /coverTaskFilterSummary/)
  assert.match(listSource, /coverTaskAllowStatusLabel/)
  assert.match(listSource, /coverTaskCurrentProductText/)
  assert.doesNotMatch(listSource, /<el-tag[\s\S]*v-if="batchAudioAutoCheckLabel"/)
})

test('showroom-admin task banners expose dismiss buttons and reopen on the corresponding batch action', () => {
  const listSource = readText('src/views/showroom-admin/components/ProductListTable.vue')

  assert.match(listSource, /一键讲解任务/)
  assert.match(listSource, /一键封面任务/)
  assert.match(listSource, /dismissNarrationTaskBanner/)
  assert.match(listSource, /dismissCoverTaskBanner/)
  assert.match(listSource, /handleBatchGenerateNarrationScriptClick/)
  assert.match(listSource, /handleBatchGenerateCoverClick/)
  assert.match(listSource, /showNarrationTaskBanner/)
  assert.match(listSource, /showCoverTaskBanner/)
})

test('showroom-admin company route exposes company bilingual narration entry without publicity-role gate', () => {
  const source = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')
  const profileSource = readText('src/views/showroom-admin/company/CompanyProfileForm.vue')
  assert.match(source, /generateCompanyNarrationScript/)
  assert.match(source, /generateCompanyNarrationAudio/)
  assert.match(source, /publishCompanyNarration/)
  assert.match(source, /语音介绍/)
  assert.match(source, /目标字数/)
  assert.match(source, /AI生成中文介绍/)
  assert.match(source, /<el-input-number/)
  assert.match(source, /生成中文音频/)
  assert.match(source, /Generate English Audio/)
  assert.match(source, /保存/)
  assert.match(profileSource, /Translate English Content/)
  const editDialogStart = source.indexOf('title="编辑公司信息"')
  const editDialogEnd = source.indexOf('<template #footer>', editDialogStart)
  assert.notEqual(editDialogStart, -1)
  assert.notEqual(editDialogEnd, -1)
  const editDialogSource = source.slice(editDialogStart, editDialogEnd)
  assert.doesNotMatch(editDialogSource, /showroom_publicity|SHOWROOM_COMPANY_EDITOR_ROLE|canPublishShowroomRelease/)
  assert.doesNotMatch(profileSource, /showroom_publicity|SHOWROOM_COMPANY_EDITOR_ROLE/)
  assert.doesNotMatch(source, /直接发布/)
  assert.doesNotMatch(source, /企宣角色/)
})

test('showroom-admin product list displays one compact status column for missing content', () => {
  const source = readText('src/views/showroom-admin/components/ProductListTable.vue')

  assert.match(source, /<el-table-column label="状态"/)
  assert.doesNotMatch(source, /<el-table-column label="所属公司ID"/)
  assert.match(source, /<el-table-column label="当前版本"/)
  assert.match(source, /contentStatusItems/)
  assert.match(source, /在售国家/)
  assert.match(source, /在售国家\(英\)/)
  assert.doesNotMatch(source, /中卖点/)
  assert.doesNotMatch(source, /英卖点/)
  assert.match(source, /中音频/)
  assert.match(source, /英音频/)
  assert.match(source, /batchAudioAutoCheckLabel/)
  assert.doesNotMatch(source, /<el-table-column label="音频"/)
  assert.doesNotMatch(source, /<el-table-column label="音色"/)
  assert.doesNotMatch(source, /generatingProductId/)
})

test('showroom-admin product list renders total pages and pager actions for the 20-row page size', () => {
  const source = readText('src/views/showroom-admin/components/ProductListTable.vue')

  assert.match(source, /<el-pagination/)
  assert.match(source, /layout="prev, pager, next"/)
  assert.match(source, /:current-page="pageNo"/)
  assert.match(source, /:page-size="pageSize"/)
  assert.match(source, /:total="pageTotal"/)
  assert.match(source, /共 \{\{ pageTotal \}\} 条，共 \{\{ totalPages \}\} 页/)
  assert.match(source, /const totalPages = computed\(\(\) =>/)
  assert.match(source, /Math\.ceil\(props\.pageTotal \/ props\.pageSize\)/)
  assert.match(source, /emit\('page-change', \{ pageNo, pageSize: props\.pageSize \}\)/)
  assert.doesNotMatch(source, /filteredRows/)
  assert.doesNotMatch(source, /filteredRows\.length/)
})

test('showroom-admin product route requests paged data with server-side product filters', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  const apiSource = readText('src/api/showroom-admin/index.ts')
  const buildProductPageParamsBlock = sliceBetween(
    source,
    'const buildProductPageParams = (pageNo: number, pageSize: number) => {',
    'const countProductRows = async () => {'
  )
  const loadProductRowsBlock = sliceBetween(
    source,
    'const loadProductRows = async () => {',
    'const loadHallRows = async () => {'
  )

  assert.match(apiSource, /productId\?: number/)
  assert.match(source, /productPageNo/)
  assert.match(source, /productPageSize/)
  assert.match(source, /productPageTotal/)
  assert.match(source, /productFilters/)
  assert.match(buildProductPageParamsBlock, /pageNo/)
  assert.match(buildProductPageParamsBlock, /pageSize/)
  assert.match(buildProductPageParamsBlock, /productId/)
  assert.match(buildProductPageParamsBlock, /keyword: productFilters\.value\.keyword/)
  assert.match(buildProductPageParamsBlock, /lifecycleStage: productFilters\.value\.lifecycleStage/)
  assert.match(buildProductPageParamsBlock, /incompleteStatus: productFilters\.value\.incompleteStatus/)
  assert.match(buildProductPageParamsBlock, /approvalStatus: productFilters\.value\.approvalStatus/)
  assert.match(loadProductRowsBlock, /buildProductPageParams\(productPageNo\.value, productPageSize\.value\)/)
  assert.match(loadProductRowsBlock, /productPageTotal\.value = nextTotal/)
  assert.match(loadProductRowsBlock, /productRows\.value = normalizeArray\(productPage\.list, 'productPage\.list'\)/)
  assert.match(source, /:page-no="productPageNo"/)
  assert.match(source, /:page-size="productPageSize"/)
  assert.match(source, /:page-total="productPageTotal"/)
  assert.match(source, /:filters="productFilters"/)
  assert.match(source, /@page-change="handleProductPageChange"/)
})

test('showroom-admin consumes notify route query by opening product detail in approval or edit mode', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  const permissionSource = readText('src/permission.ts')
  const notifyRouteBlock = sliceBetween(
    source,
    'const handleNotifyProductRoute = async () => {',
    'watch('
  )

  assert.match(source, /resolveNotifyProductRouteRequest/)
  assert.match(source, /notifyTargetType/)
  assert.match(source, /notifyTargetId/)
  assert.match(source, /notifyChangeRequestId/)
  assert.match(source, /notifyOpen/)
  assert.match(permissionSource, /const redirectPath = from\.query\.redirect \|\| to\.fullPath/)
  assert.match(source, /clearNotifyProductRouteQuery/)
  assert.match(source, /window\.history\.replaceState/)
  assert.match(source, /const rawNotifyOpen = String\(/)
  assert.match(source, /notifyOpen:\s*rawNotifyOpen === 'approval' \? 'approval' : rawNotifyOpen === 'edit' \? 'edit' : 'detail'/)
  assert.match(source, /requestKey: `query:\$\{targetId\}:\$\{notifyChangeRequestId \?\? ''\}:\$\{rawNotifyOpen === 'approval' \? 'approval' : rawNotifyOpen === 'edit' \? 'edit' : 'detail'\}`/)
  assert.match(notifyRouteBlock, /productId: request\.targetId/)
  assert.match(notifyRouteBlock, /await loadProductRows\(\)/)
  assert.match(source, /SHOWROOM_NOTIFY_PRODUCT_TARGET_KEY/)
  assert.match(source, /sessionStorage\.getItem\(SHOWROOM_NOTIFY_PRODUCT_TARGET_KEY\)/)
  assert.match(source, /sessionStorage\.removeItem\(SHOWROOM_NOTIFY_PRODUCT_TARGET_KEY\)/)
  assert.match(notifyRouteBlock, /openProductDetailById\(request\.targetId\)/)
  assert.match(notifyRouteBlock, /await openProductEdit\(\{ productId: request\.targetId \}\)/)
  assert.match(notifyRouteBlock, /productDetailApprovalChangeRequestId\.value = request\.changeRequestId \?\? null/)
  assert.match(notifyRouteBlock, /clearNotifyProductRouteQuery\(\)/)
  assert.match(source, /handleNotifyProductRoute/)
})

test('showroom approval panel uses api wrappers for reject actions instead of raw request posts', () => {
  const source = readText('src/views/showroom-admin/approval/ApprovalTaskPanel.vue')

  assert.match(source, /ShowroomAdminApi\.supervisorReject/)
  assert.match(source, /ShowroomAdminApi\.gaoxinReject/)
  assert.doesNotMatch(source, /request\.post\(\{ url: '\/showroom\/approval\/gaoxin-reject'/)
  assert.doesNotMatch(source, /request\.post\(\{ url: '\/showroom\/approval\/supervisor-reject'/)
})

test('showroom-admin route module is permission controlled instead of initial remaining routes', () => {
  const remainingSource = readText('src/router/modules/remaining.ts')
  const permissionSource = readText('src/store/modules/permission.ts')

  assert.doesNotMatch(remainingSource, /showroomRoutes/)
  assert.doesNotMatch(remainingSource, /from '.\/showroom'/)
  assert.match(permissionSource, /import showroomRoutes from '@\/router\/modules\/showroom'/)
  assert.match(permissionSource, /permissionControlledStaticRoutes/)
  assert.doesNotMatch(permissionSource, /showroomAdminRoutes/)
  assert.doesNotMatch(permissionSource, /showroomFrontstageRoutes/)
})
