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

const api = read('src/api/srm/non-bidding-project/index.ts')
const page = read('src/views/srm/non-bidding-project/index.vue')
const sql = readBackend('sql/mysql/20260619_srm_d8_1_non_bidding.sql')
const service = readBackend('yudao-module-srm/src/main/java/cn/iocoder/yudao/module/srm/service/nonbidding/SrmNonBiddingProcurementServiceImpl.java')

for (const snippet of [
  '/srm/non-bidding-project/page',
  '/srm/non-bidding-project/publish',
  '/srm/non-bidding-project/quote',
  '/srm/non-bidding-project/deal',
  '/srm/non-bidding-project/contractable-page',
  'comparisonSummary?: SrmNonBiddingComparisonSummaryVO',
  'priceTrends: SrmNonBiddingPriceTrendVO[]',
  'quoteMode: string',
  'quoteModeLabel?: string',
  'quoteStartTime: number',
  'quoteEndTime: number'
]) {
  assert(api.includes(snippet), `missing API endpoint: ${snippet}`)
}

for (const snippet of [
  '发布非招标项目',
  '提交供应商报价',
  '确认成交',
  '可建合同来源',
  '比价汇总',
  '价格趋势',
  'quoteRankings',
  '询价模式',
  "publishForm.quoteMode === 'PUBLIC'",
  '公开询价无需填写供应商范围',
  'parseDateTimeMillis',
  "new Date(value.replace(' ', 'T')).getTime()",
  '报价截止时间必须晚于报价开始时间。',
  "v-hasPermi=\"['srm:non-bidding-project:publish']\"",
  "v-hasPermi=\"['srm:non-bidding-project:quote']\"",
  "v-hasPermi=\"['srm:non-bidding-project:deal']\"",
  "v-hasPermi=\"['srm:non-bidding-project:contract']\""
]) {
  assert(page.includes(snippet), `missing page contract: ${snippet}`)
}

for (const snippet of [
  'quote_mode',
  "component` = 'srm/non-bidding-project/index'",
  "'srm:non-bidding-project:publish'",
  "'srm:non-bidding-project:quote'",
  "'srm:non-bidding-project:deal'",
  "'srm:non-bidding-project:contract'",
  'srm_non_bidding_supplier_scope',
  'srm_non_bidding_quote',
  'srm_non_bidding_quote_line'
]) {
  assert(sql.includes(snippet), `missing SQL contract: ${snippet}`)
}

for (const snippet of [
  'supplierAccessRiskService.checkSupplierEligibility',
  'buildComparisonSummary',
  'buildPriceTrends',
  'QUOTE_MODE_PUBLIC',
  'NON_BIDDING_QUOTE_MODE_INVALID',
  'NON_BIDDING_PUBLISH_ATTACHMENT_REQUIRED',
  'NON_BIDDING_QUOTE_SUPPLIER_NOT_INVITED',
  'NON_BIDDING_QUOTE_DUPLICATE',
  'SrmSourcingProjectStatusEnum.DEAL_CONFIRMED',
  'setContractId(null)'
]) {
  assert(service.includes(snippet), `missing service guard: ${snippet}`)
}

assert(!service.includes('createContract'), 'T3 must not create contracts before T5')
assert(!/catch\s*\{\s*\}/.test(page), 'page must not contain empty catch blocks')
assert(!/mock|fallback|默认成功|空列表/.test(`${api}\n${page}`), 'frontend must not introduce mock/fallback/default success')

console.log('PASS: SRM D8-1 non-bidding static contract')
