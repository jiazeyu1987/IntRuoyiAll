const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(repoRoot, relativePath))

assert(
  exists('src/api/mes/pro/dccProjectGovernance.ts'),
  '前端必须提供 MES DCC 项目治理状态 API wrapper。'
)

const batchRecordApi = read('src/api/mes/pro/batchrecordreport/index.ts')
assert.match(batchRecordApi, /routeGovernanceStatus\?: string/)
assert.match(batchRecordApi, /routeUpgradeRequired\?: boolean/)
assert.match(batchRecordApi, /duplicateRoutes\?: BatchRecordDuplicateRouteVO\[\]/)
assert.match(batchRecordApi, /routeUpgradeConfirmed/)
assert.match(batchRecordApi, /expectedRouteId/)
assert.match(batchRecordApi, /expectedRouteVersionId/)
assert.match(batchRecordApi, /data\.append\('routeUpgradeConfirmed', String\(routeUpgradeConfirmed\)\)/)

const importPage = read('src/views/mes/pro/batchrecordformlist/index.vue')
assert.match(importPage, /routeGovernanceStatus/)
assert.match(importPage, /routeUpgradeRequired/)
assert.match(importPage, /duplicateRoutes/)
assert.match(importPage, /是否升版本/)
assert.match(importPage, /存在多条正式路线绑定/)
assert.match(importPage, /routeUpgradeConfirmed:\s*shouldConfirmRouteUpgrade/)
assert.match(importPage, /expectedRouteId:\s*wordImportDialog\.preflight\?\.currentRouteId/)
assert.match(importPage, /expectedRouteVersionId:\s*wordImportDialog\.preflight\?\.currentRouteVersionId/)
assert.match(importPage, /expectedRouteCandidateVersionId:\s*wordImportDialog\.preflight\?\.currentRouteCandidateVersionId/)
assert(
  !importPage.includes('getDccProjectGovernanceStatus') &&
    !importPage.includes('wordImportDialog.projectGovernance') &&
    !importPage.includes('label="项目状态"'),
  '批记录 Word 导入弹窗不得继续展示或加载项目状态区域。'
)

const governanceApi = read('src/api/mes/pro/dccProjectGovernance.ts')
assert.match(governanceApi, /\/mes\/pro\/dcc-project-governance\/status/)
assert.match(governanceApi, /projectNames\.forEach/)
assert.match(governanceApi, /routeStatus/)
assert.match(governanceApi, /routeVersionNos\?: string\[\]/)
assert.match(governanceApi, /mainBatchRecordStatus/)
assert.match(governanceApi, /lossReportStatus/)
assert.match(governanceApi, /lossReportVersionNos\?: string\[\]/)
assert.match(governanceApi, /processInspectionStatus/)
assert.match(governanceApi, /processInspectionVersionNos\?: string\[\]/)
assert.match(governanceApi, /parameterRecordStatus/)
assert.match(governanceApi, /parameterRecordVersionNos\?: string\[\]/)

const projectCodePage = read('src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue')
const projectCodeApi = read('src/api/dcc/controlledFile/projectCodes.ts')
const projectCodePageReqVO = read('../IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/vo/DccProjectCodePageReqVO.java')
const projectCodeMapper = read('../IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/projectcode/DccProjectCodeMapper.java')
const projectCodeService = read('../IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/DccProjectCodeServiceImpl.java')
const mesConfigurationStatusApi = read('../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/api/dcc/projectcode/DccProjectCodeConfigurationStatusApiImpl.java')
const mesGovernanceController = read('../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/dccprojectgovernance/MesProDccProjectGovernanceController.java')
for (const label of ['工艺路线', '主批记录', 'QA规程']) {
  assert(
    projectCodePage.includes(label),
    `DCC 项目代码列表必须展示 ${label} 状态列。`
  )
}
for (const label of ['损耗单', '过程检验单', '参数记录表']) {
  assert(
    !new RegExp(`label="${label}"`).test(projectCodePage),
    `DCC 项目代码列表不得继续展示 ${label} 状态列。`
  )
}
for (const filter of [
  { key: 'routeConfigured', label: '工艺路线配置', queryParam: 'routeConfigured' },
  { key: 'mainBatchRecordConfigured', label: '主批记录配置', queryParam: 'mainBatchRecordConfigured' },
  { key: 'qaRegulationConfigured', label: 'QA规程配置', queryParam: 'qaRegulationConfigured' }
]) {
  assert.match(
    projectCodePage,
    new RegExp(`key: '${filter.key}'[\\s\\S]*label: '${filter.label}'[\\s\\S]*type: 'select'[\\s\\S]*queryParamKey: '${filter.queryParam}'`),
    `DCC 项目代码列表必须提供 ${filter.label} 独立筛选。`
  )
  assert.match(
    projectCodeApi,
    new RegExp(`${filter.key}\\?: boolean`),
    `DCC 项目代码列表请求必须声明 ${filter.queryParam} 参数。`
  )
  assert.match(
    projectCodePageReqVO,
    new RegExp(`private Boolean ${filter.key};`),
    `DCC 项目代码分页请求必须声明 ${filter.key} 字段。`
  )
}
assert.match(
  projectCodePage,
  /projectCodeConfigurationFilterOptions[\s\S]*label: '已配置'[\s\S]*value: true[\s\S]*label: '未配置'[\s\S]*value: false/,
  '三个配置筛选必须使用明确的已配置/未配置选项。'
)
assert.match(projectCodePageReqVO, /工艺路线是否已配置/)
assert.match(projectCodePageReqVO, /主批记录是否已配置/)
assert.match(projectCodePageReqVO, /QA 规程是否已配置/)
assert.match(projectCodeService, /DccProjectCodeConfigurationStatusApi/)
assert.match(projectCodeService, /applyConfigurationFilters/)
assert.doesNotMatch(projectCodeMapper, /FROM mes_|JOIN mes_|from mes_|join mes_/, 'DCC 项目代码 Mapper 不得复制 MES 配置状态 SQL。')
assert.match(mesConfigurationStatusApi, /MesProDccProjectGovernanceService/)
assert.match(mesConfigurationStatusApi, /MesQaInspectionRegulationService/)
assert.match(mesConfigurationStatusApi, /routeConfigured/)
assert.match(mesConfigurationStatusApi, /mainBatchRecordConfigured/)
assert.match(mesConfigurationStatusApi, /qaRegulationConfigured/)
assert.match(projectCodePage, /getDccProjectGovernanceStatus/)
assert.match(projectCodePage, /dccProjectGovernanceByProjectName/)
assert.match(projectCodePage, /formatDccProjectGovernanceStatus/)
assert.match(projectCodePage, /formatDccProjectGovernanceVersions/)
assert.match(
  governanceApi,
  /formSlotStatusRequired\?: boolean/,
  'MES DCC 项目治理状态 API wrapper 必须允许列表显式关闭表单槽位状态。'
)
assert.match(
  governanceApi,
  /params\.append\('formSlotStatusRequired', String\(options\.formSlotStatusRequired\)\)/,
  'MES DCC 项目治理状态 API wrapper 必须把 formSlotStatusRequired 传给后端。'
)
assert.match(
  projectCodePage,
  /getDccProjectGovernanceStatus\(projectNames,\s*\{[\s\S]*routeStatusRequired:\s*true[\s\S]*mainBatchRecordStatusRequired:\s*true[\s\S]*formSlotStatusRequired:\s*false[\s\S]*\}\)/,
  'DCC 项目代码列表展示工艺路线和主批记录状态时，不得请求损耗单、过程检验单、参数记录表状态。'
)
assert.match(
  mesGovernanceController,
  /@RequestParam\(value = "formSlotStatusRequired", defaultValue = "true"\) Boolean formSlotStatusRequired/,
  'MES DCC 项目治理状态接口必须暴露 formSlotStatusRequired 开关，保持旧调用默认完整治理状态。'
)
assert.match(
  mesGovernanceController,
  /dccProjectGovernanceService\.getStatus\(projectNames,\s*Boolean\.TRUE\.equals\(routeStatusRequired\),\s*Boolean\.TRUE\.equals\(mainBatchRecordStatusRequired\),\s*Boolean\.TRUE\.equals\(formSlotStatusRequired\)\)/,
  'MES DCC 项目治理状态接口必须按请求开关调用治理服务。'
)
for (const field of ['routeVersionNos', 'mainBatchRecordVersionNos']) {
  assert(
    projectCodePage.includes(field),
    `DCC 项目代码列表必须可见展示 ${field}。`
  )
}
for (const field of ['lossReportVersionNos', 'processInspectionVersionNos', 'parameterRecordVersionNos']) {
  assert(
    !projectCodePage.includes(field),
    `DCC 项目代码列表不得继续展示 ${field}。`
  )
}
assert.doesNotMatch(projectCodePage, /@click="[^"]*Version[^"]*"/)
assert.doesNotMatch(projectCodePage, /<el-link[^>]*data-testid="dcc-project-code-governance-version/)
assert.match(projectCodePage, /重复/)
assert.match(projectCodePage, /未配置/)

console.log('PASS: DCC project route governance frontend static contract')
