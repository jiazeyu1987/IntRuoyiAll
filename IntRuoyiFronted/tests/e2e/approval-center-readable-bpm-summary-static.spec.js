const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const approvalCenter = fs.readFileSync(path.join(root, 'src/views/approval-center/index.vue'), 'utf8')

const businessColumnStart = approvalCenter.indexOf('label="业务摘要"')
const businessColumnEnd = approvalCenter.indexOf('</el-table-column>', businessColumnStart)
assert.notEqual(businessColumnStart, -1, '审批中心必须保留业务摘要列。')
assert.notEqual(businessColumnEnd, -1, '审批中心业务摘要列表模板必须完整。')

const businessColumn = approvalCenter.slice(businessColumnStart, businessColumnEnd)

assert.ok(
  approvalCenter.includes('const CHINESE_CHARACTER_PATTERN = /[\\u4e00-\\u9fff]/'),
  '审批中心需要识别中文字符，不能把“中文标题 + 英文业务编号”误判成未配置中文标题。'
)
assert.ok(
  approvalCenter.includes('const BUSINESS_CONTEXT_CODE_PATTERN = /^(?=.*\\d)[A-Za-z0-9][A-Za-z0-9._/-]*$/'),
  '业务上下文编号识别必须要求编号中带数字，避免纯英文状态码被当作可读业务内容展示。'
)

const titleResolverStart = approvalCenter.indexOf('const resolveBusinessTitleLabel')
const titleResolverEnd = approvalCenter.indexOf('const resolveBusinessIdentifierLabel', titleResolverStart)
assert.notEqual(titleResolverStart, -1, '审批中心必须保留业务标题中文化函数。')
assert.notEqual(titleResolverEnd, -1, '审批中心业务标题中文化函数边界必须可识别。')
const titleResolver = approvalCenter.slice(titleResolverStart, titleResolverEnd)

assert.match(
  titleResolver,
  /return replacedTitle/,
  '审批中心必须直接显示正式业务标题，不能用配置占位文案隐藏审批内容。'
)
assert.doesNotMatch(
  titleResolver,
  /未配置中文标题/,
  '审批中心不得再用“未配置中文标题”替换已有的审批内容。'
)

const identifierResolverStart = approvalCenter.indexOf('const resolveBusinessIdentifierLabel')
const identifierResolverEnd = approvalCenter.indexOf('const resolveNodeNameLabel', identifierResolverStart)
assert.notEqual(identifierResolverStart, -1, '审批中心必须保留业务编号显示函数。')
assert.notEqual(identifierResolverEnd, -1, '审批中心业务编号显示函数边界必须可识别。')
const identifierResolver = approvalCenter.slice(identifierResolverStart, identifierResolverEnd)

assert.match(
  identifierResolver,
  /return `业务编号：\$\{businessCode\}`/,
  '审批中心必须显示真实业务编号，并使用中文标签说明。'
)
assert.doesNotMatch(
  identifierResolver,
  /containsEnglishLetters\(businessCode\)\s*\?\s*'业务编号已配置'/,
  '审批中心不得用“业务编号已配置”隐藏真实业务编号。'
)

const contextValueResolverStart = approvalCenter.indexOf('const resolveBusinessContextValueLabel')
const contextValueResolverEnd = approvalCenter.indexOf('const normalizeDccContextTag', contextValueResolverStart)
assert.notEqual(contextValueResolverStart, -1, '审批中心必须保留业务上下文值显示函数。')
assert.notEqual(contextValueResolverEnd, -1, '审批中心业务上下文值显示函数边界必须可识别。')
const contextValueResolver = approvalCenter.slice(contextValueResolverStart, contextValueResolverEnd)

assert.match(
  contextValueResolver,
  /containsEnglishLetters\(replacedText\)[\s\S]{0,160}!containsChineseCharacters\(replacedText\)[\s\S]{0,160}!isBusinessContextCode\(replacedText\)/,
  '业务上下文中的工单号、批次号等正式编号必须直接展示，不得被“未配置中文值”隐藏。'
)

assert.match(
  businessColumn,
  /v-if="row\.businessContextTags\?\.length"/,
  '业务上下文标签必须对所有模块展示，不能只限 DCC。'
)
assert.doesNotMatch(
  businessColumn,
  /row\.moduleCode === 'DCC' && row\.businessContextTags\?\.length/,
  'BPM/eDHR 等模块的业务上下文标签不得被 DCC 条件挡住。'
)

console.log('PASS: approval center readable BPM summary static contract')
