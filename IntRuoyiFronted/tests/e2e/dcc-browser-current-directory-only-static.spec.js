const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.resolve(process.cwd(), 'src/views/dcc/controlled-file/browser/index.vue'),
  'utf8'
)

const extractConstFunctionBlock = (functionName) => {
  const marker = `const ${functionName} = `
  const start = source.indexOf(marker)
  assert.notEqual(start, -1, `missing function block: ${functionName}`)
  const nextConst = source.indexOf('\n\nconst ', start + marker.length)
  return nextConst === -1 ? source.slice(start) : source.slice(start, nextConst)
}

const browserRequestParamsBlock = extractConstFunctionBlock('buildBrowserRequestParams')
const batchRecognitionReqBlock = extractConstFunctionBlock('buildBatchRecognitionReq')

assert.match(
  browserRequestParamsBlock,
  /if \(isCurrentDirectorySearch\.value\) \{[\s\S]*requestParams\.directoryId = selectedDirectoryId\.value/,
  '点击目录树后，文件查阅列表请求必须继续传当前目录 directoryId。'
)

assert.match(
  browserRequestParamsBlock,
  /requestParams\.includeDescendantDirectories = false/,
  '点击目录树后，文件查阅列表请求必须显式只查当前目录直属文件，不包含子目录。'
)

assert.doesNotMatch(
  browserRequestParamsBlock,
  /requestParams\.includeDescendantDirectories = isCurrentDirectorySearch\.value/,
  '文件查阅列表请求不得再把当前目录查询扩展为当前目录 + 子目录。'
)

assert.match(
  batchRecognitionReqBlock,
  /includeDescendantDirectories: isCurrentDirectorySearch\.value/,
  '批量识别仍然保留“当前文件夹及子文件夹”的原有范围，不受列表直属查询调整影响。'
)

console.log('PASS: dcc browser current directory only static contract')
