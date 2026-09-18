const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..', '..', '..', '..', '..')
const read = (...segments) => fs.readFileSync(path.join(repoRoot, ...segments), 'utf8')

const detailModel = read(
  'IntRuoyiBackend',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'service',
  'pro',
  'processpool',
  'team',
  'MesTeamLeaderActiveOrderDetail.java'
)
const detailResp = read(
  'IntRuoyiBackend',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'controller',
  'admin',
  'pro',
  'processpool',
  'team',
  'vo',
  'MesTeamLeaderActiveOrderDetailRespVO.java'
)
const controller = read(
  'IntRuoyiBackend',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'controller',
  'admin',
  'pro',
  'processpool',
  'team',
  'MesProcessPoolTeamLeaderController.java'
)
const service = read(
  'IntRuoyiBackend',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'service',
  'pro',
  'processpool',
  'team',
  'MesTeamLeaderActiveOrderDetailServiceImpl.java'
)
const frontend = read(
  'IntRuoyiFronted',
  'src',
  'views',
  'mes',
  'pro',
  'processpool',
  'components',
  'ActiveOrderSubmissionDetailPanel.vue'
)
const frontendApi = read(
  'IntRuoyiFronted',
  'src',
  'api',
  'mes',
  'pro',
  'processpool',
  'teamLeader.ts'
)

assert.match(
  detailModel,
  /class PqcSubmissionDetail[\s\S]*private List<PqcSubmissionItemDetail> submittedItems[\s\S]*private List<PqcSubmissionItemDetail> processInspectionItems/,
  'PQC detail domain model must separate original submitted items from current process inspection items'
)
assert.match(
  detailResp,
  /class PqcSubmissionDetail[\s\S]*private List<PqcSubmissionItemDetail> submittedItems[\s\S]*private List<PqcSubmissionItemDetail> processInspectionItems/,
  'PQC detail response VO must expose explicit submittedItems and processInspectionItems'
)
assert.match(
  controller,
  /\.setSubmittedItems\(submission\.getSubmittedItems\(\)[\s\S]*\.setProcessInspectionItems\(submission\.getProcessInspectionItems\(\)/,
  'controller must map submittedItems and processInspectionItems separately'
)
assert(
    service.includes('MesProProcessPoolEventMapper') &&
    service.includes('MesProProcessPoolEventRevisionMapper') &&
    service.includes('resolvePqcOriginalSubmittedItems') &&
    service.includes('getBeforePayload()') &&
    service.includes('getRawPayload()'),
  'detail service must load original PQC submitted items from revision beforePayload or event rawPayload'
)
assert(
  frontendApi.includes('submittedItems: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]') &&
    frontendApi.includes('processInspectionItems: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]') &&
    !frontendApi.includes('items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]'),
  'frontend API type must expose explicit PQC item collections and stop using ambiguous items'
)
const pqcPaneStart = frontend.indexOf('label="PQC提交"')
assert(pqcPaneStart >= 0, 'active order detail must keep the PQC提交 main tab')
const pqcPane = frontend.slice(pqcPaneStart, frontend.indexOf('label="领料单"', pqcPaneStart))
assert(
  pqcPane.includes('data-team-leader-active-order-detail-pqc-inner-tabs') &&
    pqcPane.includes('label="原始提交"') &&
    pqcPane.includes('label="过程检验记录"') &&
    pqcPane.includes('data-team-leader-active-order-detail-pqc-process-tabs'),
  'PQC提交 must render inner tabs for original submit and current process inspection record'
)
assert(
  /const pqcInnerActiveTab = ref\('originalSubmissions'\)/.test(frontend),
  'original submit inner tab must be the default PQC tab'
)
assert(
  frontend.includes('buildActiveOrderPqcSubmittedItemRows') &&
    frontend.includes('submission.submittedItems ?? []') &&
    frontend.includes('submission.processInspectionItems ?? []'),
  'frontend helpers must read submittedItems for original tab and processInspectionItems for current record'
)
assert(
  frontend.includes('resolveActiveOrderPqcSubmittedItemIdentityKey') &&
    frontend.includes('sampleCountText') &&
    frontend.includes('resultSummaryText') &&
    frontend.includes('formatPqcInspectionMeasuredValues(items)') &&
    /const buildActiveOrderPqcSubmittedItemRows[\s\S]*new Map/.test(frontend) &&
    !/const buildActiveOrderPqcSubmittedItemRows[\s\S]*=>\s*\n?\s*\(submission\.submittedItems \?\? \[\]\)\.map/.test(frontend),
  'original submitted items must be grouped by inspection item and rendered once per item instead of once per sample'
)
assert(
  pqcPane.includes('<th>检验类型</th>') &&
    pqcPane.includes('<th>检测数量</th>') &&
    pqcPane.includes('<th>检测结果</th>') &&
    pqcPane.includes('row.inspectionTypeText') &&
    pqcPane.includes('row.sampleCountText') &&
    pqcPane.includes('row.resultSummaryText') &&
    !pqcPane.includes('<th>样本</th>') &&
    !pqcPane.includes('row.sampleText'),
  'original submit table must render one aggregated row with inspection type, count and result summary'
)
assert(
  !frontend.includes('pqcSubmission.items') &&
    !frontend.includes('submission.items ?? []'),
  'active order PQC frontend must not use ambiguous submission.items'
)

const originalPane = pqcPane.slice(0, pqcPane.indexOf('label="过程检验记录"'))
assert(originalPane.includes('formatPqcSubmissionQuantity(submission.submittedInspectionQuantity)') &&
  originalPane.includes('formatPqcSubmissionQuantity(submission.submittedScrapQuantity)') &&
  !originalPane.includes('submission.actualInspectionQuantity') &&
  !originalPane.includes('submission.scrapQuantity'),
  'original tab quantities must come from the first submitted snapshot')
assert(controller.includes('.setSubmittedInspectionQuantity(submission.getSubmittedInspectionQuantity())') &&
  controller.includes('.setSubmittedScrapQuantity(submission.getSubmittedScrapQuantity())'),
  'controller must preserve original snapshot quantities')
assert(frontendApi.includes('submittedInspectionQuantity: number') &&
  frontendApi.includes('submittedScrapQuantity: number'), 'original quantities must be in the API contract')

// Execute the actual display helpers, including aggregation and final judgement.
const ts = require(path.join(repoRoot, 'IntRuoyiFronted/node_modules/typescript'))
const vm = require('node:vm')
const helpers = frontend.slice(frontend.indexOf('const normalizeActiveOrderPqcText ='),
  frontend.indexOf('type PqcInspectionRecordSpanNumberKey ='))
const inspectionTypeHelper = frontend.slice(frontend.indexOf('const resolvePqcInspectionTypeText ='),
  frontend.indexOf('const resetTabs ='))
const sandbox = {}
vm.runInNewContext(ts.transpileModule(`${helpers}\n${inspectionTypeHelper}\n` +
  'globalThis.buildRows = buildActiveOrderPqcSubmittedItemRows', {
  compilerOptions: { target: ts.ScriptTarget.ES2020 }
}).outputText, sandbox)
const submittedItems = Array.from({ length: 13 }, (_, index) => ({
  itemCode: 'WIDTH', itemName: '宽度', sampleNo: index + 1,
  measuredValue: index === 12 ? '15.5' : '12.3', judgement: index === 12 ? 'FAILURE' : 'SUCCESS'
}))
const rows = sandbox.buildRows({ pqcTaskId: 1, submittedEventId: 2, inspectionType: 'FIRST',
  submittedItems, processInspectionItems: [{ itemCode: 'WIDTH', measuredValue: '99.9' }] })
assert.equal(rows.length, 1, '13 samples of one item must display as one row')
assert.equal(rows[0].sampleCountText, '13')
assert.equal(rows[0].judgementText, '不通过', 'a later failed sample must fail the row')
assert.equal(rows[0].resultSummaryText, submittedItems.map(item => item.measuredValue).join('，'))
assert(!rows[0].resultSummaryText.includes('99.9'), 'current values must not leak into the original row')

console.log('PASS: active-order-pqc-submit-tabs-static and display helper regression')
