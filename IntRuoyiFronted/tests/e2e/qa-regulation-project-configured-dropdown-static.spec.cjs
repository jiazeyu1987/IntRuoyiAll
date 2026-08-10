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
  /const isDccProjectCodeConfigured\s*=\s*\(\s*project:\s*DccProjectCodeRespVO\s*\)\s*=>[\s\S]*qaRegulationProjectStatusByProductId\.value\.get\(productId\)\?\.configured === true/,
  'Dropdown configured state must come from formal project-statuses response by product ID.'
)
assert.match(
  qaSource,
  /const sortDccProjectCodeOptionsByQaStatus\s*=\s*\(\s*projects:\s*DccProjectCodeRespVO\[\]\s*\)\s*=>[\s\S]*isDccProjectCodeConfigured\(left\)[\s\S]*isDccProjectCodeConfigured\(right\)[\s\S]*return rightConfiguredScore - leftConfiguredScore/,
  'DCC project options must sort configured products before unconfigured products.'
)
assert.match(
  loadOptionsSource,
  /const productIds = resolveDccProjectCodeProductIds\(options\)[\s\S]*await QcTemplateApi\.getQaRegulationProjectStatuses\(productIds\)[\s\S]*qaRegulationProjectStatusByProductId\.value = createQaRegulationProjectStatusMap\(projectStatuses\)[\s\S]*dccProjectCodeOptions\.value = sortDccProjectCodeOptionsByQaStatus\(options\)/,
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
