const fs = require('fs')
const path = require('path')

const frontendRoot = path.resolve(__dirname, '../..')

const read = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')

const assertIncludes = (content, expected, message) => {
  if (!content.includes(expected)) {
    throw new Error(message || `Expected content to include: ${expected}`)
  }
}

const assertNotIncludes = (content, unexpected, message) => {
  if (content.includes(unexpected)) {
    throw new Error(message || `Expected content not to include: ${unexpected}`)
  }
}

const parserPage = read('src/views/form-center/parser/index.vue')
const routerHelper = read('src/utils/routerHelper.ts')

assertIncludes(parserPage, '生产批记录', 'production batch record action must remain')
assertIncludes(parserPage, 'QA检验规程', 'QA inspection regulation action must remain')
assertNotIncludes(parserPage, '过程检验记录', 'highlighted process inspection action must be removed')
assertNotIncludes(parserPage, 'handleUnsupportedParseType', 'unused unsupported parser handler must be removed')

assertIncludes(
  routerHelper,
  "const FORM_CENTER_PARSER_ROUTE_COMPONENT = 'form-center/parser/index'",
  'form parser route component must have an explicit metadata override constant'
)
assertIncludes(
  routerHelper,
  'FORM_CENTER_PARSER_ROUTE_COMPONENT === componentPath',
  'parser route metadata override must match the exact component path'
)
assertIncludes(
  routerHelper,
  'meta.hideFooter = true',
  'parser route metadata override must hide the global footer'
)

console.log('form parser highlighted content removal static contract passed')
