const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const backendRoot = path.resolve(repoRoot, '..', 'ruoyi-vue-pro')

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function readBackend(relativePath) {
  return fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')
}

const api = read('src/api/srm/tender-project/index.ts')
const page = read('src/views/srm/tender-project/index.vue')
const sql = readBackend('sql/mysql/20260620_srm_d10_1_tender.sql')
const service = readBackend('yudao-module-srm/src/main/java/cn/iocoder/yudao/module/srm/service/tender/SrmTenderProcurementServiceImpl.java')

for (const snippet of [
  '/srm/tender-project/page',
  '/srm/tender-project/get',
  '/srm/tender-project/publish',
  '/srm/tender-project/submit-bid',
  '/srm/tender-project/expert/create',
  '/srm/tender-project/expert/approve',
  '/srm/tender-project/committee',
  '/srm/tender-project/candidate',
  '/srm/tender-project/winning',
  'submissionStartTime: number',
  'submissionEndTime: number'
]) {
  assert(api.includes(snippet), `missing API endpoint: ${snippet}`)
}

for (const snippet of [
  '发布招标项目',
  '提交供应商投标',
  '创建并通过招标专家',
  '组建评标委员会',
  '生成中标候选',
  '确认中标结果',
  'parseDateTimeMillis',
  "new Date(value.replace(' ', 'T')).getTime()",
  '投标截止时间必须晚于投标开始时间。',
  "v-hasPermi=\"['srm:tender-project:publish']\"",
  "v-hasPermi=\"['srm:tender-project:submit-bid']\"",
  "v-hasPermi=\"['srm:tender-project:expert']\"",
  "v-hasPermi=\"['srm:tender-project:committee']\"",
  "v-hasPermi=\"['srm:tender-project:candidate']\"",
  "v-hasPermi=\"['srm:tender-project:winning']\""
]) {
  assert(page.includes(snippet), `missing page contract: ${snippet}`)
}

for (const snippet of [
  "component` = 'srm/tender-project/index'",
  "'srm:tender-project:publish'",
  "'srm:tender-project:submit-bid'",
  "'srm:tender-project:expert'",
  "'srm:tender-project:committee'",
  "'srm:tender-project:candidate'",
  "'srm:tender-project:winning'",
  'srm_tender_notice',
  'srm_tender_document',
  'srm_tender_submission',
  'srm_tender_expert',
  'srm_tender_expert_application',
  'srm_tender_committee_member',
  'srm_tender_candidate',
  'srm_tender_winning_result',
  "'EXPERT_DRAW_APPLICATION'"
]) {
  assert(sql.includes(snippet), `missing SQL contract: ${snippet}`)
}

for (const snippet of [
  'supplierAccessRiskService.checkSupplierEligibility',
  'TENDER_PUBLISH_ATTACHMENT_REQUIRED',
  'TENDER_SUBMISSION_WINDOW_INVALID',
  'TENDER_SUBMISSION_WINDOW_CLOSED',
  'TENDER_SUBMISSION_SUPPLIER_DUPLICATE',
  'TENDER_COMMITTEE_MEMBER_DUPLICATE',
  'TENDER_COMMITTEE_MEMBER_INSUFFICIENT',
  'TENDER_EXPERT_STATUS_INVALID',
  'TENDER_EXPERT_SPECIALTY_MISMATCH',
  'SrmSourcingProjectStatusEnum.WINNING_CONFIRMED',
  'setContractId(null)'
]) {
  assert(service.includes(snippet), `missing service guard: ${snippet}`)
}

assert(!service.includes('createContract'), 'T4 must not create contracts before T5')
assert(!/catch\s*\{\s*\}/.test(page), 'page must not contain empty catch blocks')
assert(!/mock|fallback|默认成功|空列表/.test(`${api}\n${page}`), 'frontend must not introduce mock/fallback/default success')

console.log('PASS: SRM D10-1 tender static contract')
