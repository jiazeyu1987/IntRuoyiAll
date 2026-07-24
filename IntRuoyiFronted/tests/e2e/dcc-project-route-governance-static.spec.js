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
assert.match(importPage, /存在多条同名工艺路线/)
assert.match(importPage, /routeUpgradeConfirmed:\s*wordImportDialog\.preflight\?\.routeUpgradeRequired/)
assert.match(importPage, /expectedRouteId:\s*wordImportDialog\.preflight\?\.currentRouteId/)
assert.match(importPage, /expectedRouteVersionId:\s*wordImportDialog\.preflight\?\.currentRouteVersionId/)
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
assert.match(governanceApi, /mainBatchRecordStatus/)
assert.match(governanceApi, /lossReportStatus/)
assert.match(governanceApi, /processInspectionStatus/)
assert.match(governanceApi, /parameterRecordStatus/)

const projectCodePage = read('src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue')
for (const label of ['工艺路线', '主批记录', '损耗单', '过程检验单', '参数记录表']) {
  assert(
    projectCodePage.includes(label),
    `DCC 项目代码列表必须展示 ${label} 状态列。`
  )
}
assert.match(projectCodePage, /getDccProjectGovernanceStatus/)
assert.match(projectCodePage, /dccProjectGovernanceByProjectName/)
assert.match(projectCodePage, /formatDccProjectGovernanceStatus/)
assert.match(projectCodePage, /重复/)
assert.match(projectCodePage, /未配置/)

console.log('PASS: DCC project route governance frontend static contract')
