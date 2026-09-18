const fs = require('fs')
const path = require('path')

const read = (relativePath) =>
  fs.readFileSync(path.join(process.cwd(), relativePath), 'utf8')

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

const extractFunction = (source, signature) => {
  const start = source.indexOf(signature)
  if (start < 0) {
    throw new Error(`Missing function signature: ${signature}`)
  }
  const bodyStart = source.indexOf('{', start)
  if (bodyStart < 0) {
    throw new Error(`Missing function body: ${signature}`)
  }
  let depth = 0
  for (let index = bodyStart; index < source.length; index += 1) {
    if (source[index] === '{') depth += 1
    if (source[index] === '}') {
      depth -= 1
      if (depth === 0) return source.slice(start, index + 1)
    }
  }
  throw new Error(`Unclosed function body: ${signature}`)
}

const parserPage = read('src/views/form-center/parser/index.vue')
const qaPanel = read('src/views/form-center/parser/components/QaInspectionRegulationParserPanel.vue')

const parserActionBlock = parserPage.slice(
  parserPage.indexOf('<div class="form-parser-panel-actions">'),
  parserPage.indexOf('</div>', parserPage.indexOf('<div class="form-parser-panel-actions">')) + 6
)
const qaActionBlock = qaPanel.slice(
  qaPanel.indexOf('<div class="qa-parser-panel-actions">'),
  qaPanel.indexOf('</div>', qaPanel.indexOf('<div class="qa-parser-panel-actions">')) + 6
)

assertIncludes(parserActionBlock, '发布', '生产批记录编辑区必须显示发布按钮')
assertIncludes(parserActionBlock, '@click="handlePublishPlaceholder"', '生产批记录发布按钮必须绑定占位处理函数')
assertIncludes(parserActionBlock, '@click="handleApplyEditedJson"', '生产批记录应用按钮必须保留')
if (parserActionBlock.indexOf('发布') > parserActionBlock.indexOf('应用')) {
  throw new Error('生产批记录发布按钮必须位于应用按钮左侧')
}

assertIncludes(qaActionBlock, '发布', 'QA 检验规程编辑区必须显示发布按钮')
assertIncludes(qaActionBlock, '@click="handleQaPublishPlaceholder"', 'QA 发布按钮必须绑定占位处理函数')
assertIncludes(qaActionBlock, '@click="applyQaEditedJson"', 'QA 应用按钮必须保留')
if (qaActionBlock.indexOf('发布') > qaActionBlock.indexOf('应用')) {
  throw new Error('QA 检验规程发布按钮必须位于应用按钮左侧')
}

const parserPublishHandler = extractFunction(parserPage, 'const handlePublishPlaceholder =')
const qaPublishHandler = extractFunction(qaPanel, 'const handleQaPublishPlaceholder =')

assertIncludes(parserPublishHandler, "message.info('发布功能待实现')", '生产批记录发布占位必须提示待实现')
assertIncludes(qaPublishHandler, "message.info('发布功能待实现')", 'QA 发布占位必须提示待实现')
for (const handler of [parserPublishHandler, qaPublishHandler]) {
  assertNotIncludes(handler, 'request.', '发布占位不得调用请求')
  assertNotIncludes(handler, 'download.', '发布占位不得触发下载')
  assertNotIncludes(handler, '发布成功', '发布占位不得伪造发布成功')
}

console.log('form parser publish placeholder static contract passed')
