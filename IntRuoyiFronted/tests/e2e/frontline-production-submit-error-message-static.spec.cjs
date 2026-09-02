const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const root = path.resolve(__dirname, '../..')
const panelPath = path.join(
  root,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const axiosServicePath = path.join(root, 'src/config/axios/service.ts')
const source = fs.readFileSync(panelPath, 'utf8')
const axiosService = fs.readFileSync(axiosServicePath, 'utf8')

const resolverStart = source.indexOf('const resolveErrorMessage = (error: unknown) => {')
const resolverEnd = source.indexOf('const showFrontlineError = (error: unknown) => {', resolverStart)
assert.ok(resolverStart >= 0 && resolverEnd > resolverStart, 'must locate frontline error resolver')
const resolver = source.slice(resolverStart, resolverEnd)

assert.match(
  resolver,
  /resolveFrontlineErrorMessageCandidate\(record\?\.details\)/,
  'frontline error resolver must inspect axios ApiError details before Error.message'
)
assert.match(
  resolver,
  /isGenericFrontlineErrorMessage\(candidate\)/,
  'frontline error resolver must reject generic system-exception messages when better details exist'
)
assert.doesNotMatch(
  resolver,
  /return error\.message\.trim\(\)/,
  'frontline error resolver must not directly return generic Error.message before checking structured details'
)

const start = source.indexOf('const handleProductionFormalSubmit = async () => {')
const end = source.indexOf('const assertPqcSignatureAndQuantityReady', start)
assert.ok(start >= 0 && end > start, 'must locate production formal submit handler')
const handler = source.slice(start, end)

assert.match(
  handler,
  /try\s*\{[\s\S]*await ProFeedbackApi\.frontlineSubmit\(formalPayload\)[\s\S]*\}\s*catch\s*\(error\)\s*\{[\s\S]*showFrontlineError\(error\)[\s\S]*\}\s*finally\s*\{/,
  'production submit API failure must be caught and displayed in the frontline error panel'
)
assert.doesNotMatch(
  handler,
  /catch\s*\(error\)\s*\{[\s\S]*showFrontlineError\(error\)[\s\S]*throw error/,
  'production submit failures must not bubble into the global system-exception handler after local display'
)
assert.match(
  handler,
  /finally\s*\{[\s\S]*payloadLoading\.value = false[\s\S]*\}/,
  'production submit failure must always restore the loading state'
)

const axiosErrorBranchStart = axiosService.indexOf('(error: AxiosError) => {', axiosService.indexOf('service.interceptors.response.use'))
const axiosErrorBranchEnd = axiosService.indexOf('const refreshToken = async () => {', axiosErrorBranchStart)
assert.ok(axiosErrorBranchStart >= 0 && axiosErrorBranchEnd > axiosErrorBranchStart, 'must locate axios response error branch')
const axiosErrorBranch = axiosService.slice(axiosErrorBranchStart, axiosErrorBranchEnd)
assert.match(
  axiosErrorBranch,
  /const ignoreErrorMessage = \(error\.config as RequestCustomConfig \| undefined\)\?\.ignoreErrorMessage === true/,
  'axios HTTP/network error branch must read per-request ignoreErrorMessage'
)
assert.match(
  axiosErrorBranch,
  /if \(!ignoreErrorMessage\) \{\s*ElMessage\.error\(message\)\s*\}/,
  'axios HTTP/network error branch must suppress the global toast when the caller owns inline error display'
)

console.log('PASS: frontline production submit errors are shown in panel without global bubbling')
