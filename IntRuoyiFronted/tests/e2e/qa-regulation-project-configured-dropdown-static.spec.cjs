const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const qaPagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/QaRegulationPage.vue'
)
const qaSource = fs.readFileSync(qaPagePath, 'utf8')

const headerStart = qaSource.indexOf('<div class="qa-regulation-page__header">')
const loadErrorStart = qaSource.indexOf('v-if="dccProjectCodeLoadError"', headerStart)
const headerEnd = qaSource.lastIndexOf('</div>', loadErrorStart)

assert.ok(headerStart >= 0 && headerEnd > headerStart, 'QA header must exist.')

const headerSource = qaSource.slice(headerStart, headerEnd)
const loadOptionsStart = qaSource.indexOf('const loadDccProjectCodeOptions = async')
const retryStart = qaSource.indexOf('const retryLoadDccProjectCodes =', loadOptionsStart)

assert.ok(
  loadOptionsStart >= 0 && retryStart > loadOptionsStart,
  'DCC project option loading function must remain available.'
)

const loadOptionsSource = qaSource.slice(loadOptionsStart, retryStart)

assert.match(
  qaSource,
  /type QaInspectionRegulationProjectStatusVO/,
  'QA page must import the formal product-level QA regulation status type.'
)
assert.match(
  qaSource,
  /const qaRegulationProjectStatusByProductId\s*=\s*ref\(new Map<number,\s*QaInspectionRegulationProjectStatusVO>\(\)\)/,
  'QA page must keep product QA regulation status keyed by formal product ID.'
)
assert.match(
  qaSource,
  /const hasQaProductRuleDraftConfiguration\s*=\s*\(\s*snapshot:\s*QaProductRuleDraftSnapshot \| undefined\s*\)\s*=>[\s\S]*snapshot\.regulationItems\.length > 0/,
  'Dropdown configured state must treat the page-visible QA rule draft as the same configured口径.'
)
assert.match(
  qaSource,
  /const isDccProjectCodeConfigured\s*=\s*\(\s*project:\s*DccProjectCodeRespVO\s*\)\s*=>[\s\S]*qaRegulationProjectStatusByProductId\.value\.get\(productId\)\?\.configured === true[\s\S]*hasQaProductRuleDraftConfiguration\(resolveQaProductRuleDraftSnapshot\(productId, project\)\)/,
  'Dropdown configured state must merge formal project-statuses and current page draft data through one product identity口径.'
)
assert.match(
  qaSource,
  /const sortDccProjectCodeOptionsByQaStatus\s*=\s*\(\s*projects:\s*DccProjectCodeRespVO\[\]\s*\)\s*=>[\s\S]*isDccProjectCodeConfigured\(left\)[\s\S]*isDccProjectCodeConfigured\(right\)[\s\S]*return rightConfiguredScore - leftConfiguredScore/,
  'DCC project options must sort configured products before unconfigured products.'
)
assert.match(
  qaSource,
  /const DCC_PROJECT_CODE_PAGE_SIZE\s*=\s*200/,
  'QA dropdown must request the maximum validated DCC page size before complete configured sorting.'
)
assert.match(
  qaSource,
  /const loadCompleteDccProjectCodeOptions\s*=\s*async \(keyword: string\)\s*=>[\s\S]*while \(true\)[\s\S]*pageSize:\s*DCC_PROJECT_CODE_PAGE_SIZE[\s\S]*const total = Number\(data\.total\)[\s\S]*options\.length >= total[\s\S]*pageNo \+= 1[\s\S]*return mergeDccProjectCodeOptions\(options\)/,
  'QA dropdown must load the complete matching DCC candidate set before sorting configured products.'
)
const sortStart = qaSource.indexOf('const sortDccProjectCodeOptionsByQaStatus')
const sortEnd = qaSource.indexOf('const cloneQaRegulationItems', sortStart)
assert.ok(sortStart >= 0 && sortEnd > sortStart, 'DCC project configured sorting block must exist.')
const sortSource = qaSource.slice(sortStart, sortEnd)
assert.doesNotMatch(
  sortSource,
  /PRESSURE_PUMP_PROJECT_CODE|BALLOON_PRESSURE_PUMP_PROJECT_CODE|按压式|球囊/,
  'Configured-first sorting must not hardcode pressure pump products as fixed first/second options.'
)
assert.match(
  loadOptionsSource,
  /const options = await loadCompleteDccProjectCodeOptions\(keyword\.trim\(\)\)[\s\S]*const productIds = resolveDccProjectCodeProductIds\(mergedOptions\)[\s\S]*await QcTemplateApi\.getQaRegulationProjectStatuses\(productIds\)[\s\S]*qaRegulationProjectStatusByProductId\.value = createQaRegulationProjectStatusMap\(projectStatuses\)[\s\S]*dccProjectCodeOptions\.value = sortDccProjectCodeOptionsByQaStatus\(mergedOptions\)/,
  'Loading dropdown options must fetch formal QA statuses before assigning sorted options.'
)
assert.match(
  headerSource,
  /<el-option[\s\S]*:class="getDccProjectCodeOptionClass\(project\)"[\s\S]*<span[\s\S]*class="qa-regulation-page__project-option-label"[\s\S]*:class="\{ 'is-configured': isDccProjectCodeConfigured\(project\) \}"[\s\S]*\{\{ formatDccProjectCodeOption\(project\) \}\}/,
  'Dropdown option content must apply a stable green marker class to configured products.'
)
assert.match(
  qaSource,
  /\.qa-regulation-page__project-option-label\.is-configured\s*\{[\s\S]*color:\s*#00a896[\s\S]*font-weight:\s*700/,
  'Configured dropdown option label must be visibly green and emphasized.'
)

console.log('PASS qa-regulation-project-configured-dropdown-static')
