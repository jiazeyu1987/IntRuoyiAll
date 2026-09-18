const fs = require('fs')
const path = require('path')

const read = (relativePath) =>
  fs.readFileSync(path.join(process.cwd(), relativePath), 'utf8')

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
const qaPanel = read('src/views/form-center/parser/components/QaInspectionRegulationParserPanel.vue')

const productionParseHandler = extractFunction(
  parserPage,
  'const parseAndDownloadProductionBatchRecord ='
)
const qaParseHandler = extractFunction(qaPanel, 'const parseWordFile =')

assertNotIncludes(
  productionParseHandler,
  'download.json',
  '生产批记录解析完成后不得自动下载 JSON'
)
assertNotIncludes(
  productionParseHandler,
  '已下载 JSON 文件',
  '生产批记录解析成功提示不得声称已下载 JSON'
)
assertNotIncludes(
  qaParseHandler,
  'download.json',
  'QA 检验规程解析完成后不得自动下载 JSON'
)
assertNotIncludes(
  qaParseHandler,
  '已下载 JSON 文件',
  'QA 检验规程解析成功提示不得声称已下载 JSON'
)

assertIncludes(
  parserPage,
  '@click="downloadCurrentRecognitionJson"',
  '生产批记录仍需保留显式的当前 JSON 下载按钮'
)
assertIncludes(
  qaPanel,
  '@click="downloadQaCurrentJson"',
  'QA 检验规程仍需保留显式的当前 JSON 下载按钮'
)

console.log('form parser auto JSON download regression contract passed')
