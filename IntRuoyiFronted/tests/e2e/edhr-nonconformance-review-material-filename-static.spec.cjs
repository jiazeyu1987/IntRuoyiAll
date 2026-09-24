const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(
  repoRoot,
  'src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'
)
const source = fs.readFileSync(pagePath, 'utf8')
const uploadFilePath = path.join(repoRoot, 'src/components/UploadFile/src/UploadFile.vue')
const uploadSource = fs.readFileSync(uploadFilePath, 'utf8')

const resolveFileNameMatch = source.match(
  /const decodeFileName = \(value: string\) => \{[\s\S]*?const resolveFileName = \(url: string\) => \{[\s\S]*?\n\}/
)

if (!resolveFileNameMatch) {
  throw new Error('Nonconformance review page must define resolveFileName.')
}

const resolveFileNameBlock = resolveFileNameMatch[0]

if (!resolveFileNameBlock.includes('new URL(')) {
  throw new Error('resolveFileName must parse material URLs with URL semantics before decoding.')
}

if (!resolveFileNameBlock.includes('pathname')) {
  throw new Error('resolveFileName must decode the URL pathname, not query or fragment text.')
}

if (!/replace\(\s*\/\\\+\/g,\s*' '\s*\)/.test(resolveFileNameBlock)) {
  throw new Error('resolveFileName must normalize plus signs in uploaded file-name segments.')
}

if (!/while\s*\(\s*decoded\s*!==\s*current\s*\)/.test(resolveFileNameBlock)) {
  throw new Error('resolveFileName must decode repeatedly until a double-encoded filename is stable.')
}

if (resolveFileNameBlock.includes("url.substring(url.lastIndexOf('/') + 1)")) {
  throw new Error('resolveFileName must not directly decode the full final URL segment.')
}

const encodedUrl =
  'http://localhost/admin-api/infra/file/4/get/%E4%B8%8D%E5%90%88%E6%A0%BC+%E8%AF%84%E5%AE%A1.xlsx?token=abc'
const expectedFileName = '不合格 评审.xlsx'

const doubleEncodedUrl =
  'http://localhost/admin-api/infra/file/4/get/%25E4%25B8%258D%25E5%2590%2588%25E6%25A0%25BC%252B%25E8%25AF%2584%25E5%25AE%25A1.xlsx?token=abc'
const decodeRepeatedly = (value) => {
  let current = value
  let decoded = decodeURIComponent(current.replace(/\+/g, ' '))
  while (decoded !== current) {
    current = decoded
    decoded = decodeURIComponent(current.replace(/\+/g, ' '))
  }
  return decoded
}

const actualFileName = decodeRepeatedly(new URL(encodedUrl).pathname.split('/').pop())
const actualDoubleEncodedFileName = decodeRepeatedly(
  new URL(doubleEncodedUrl).pathname.split('/').pop()
)

if (actualFileName !== expectedFileName) {
  throw new Error(`fixture sanity failed: expected ${expectedFileName}, got ${actualFileName}`)
}

if (actualDoubleEncodedFileName !== expectedFileName) {
  throw new Error(
    `double-encoded fixture sanity failed: expected ${expectedFileName}, got ${actualDoubleEncodedFileName}`
  )
}

const handleFileSuccessMatch = uploadSource.match(
  /const handleFileSuccess:[\s\S]*?\n\/\/ 文件数超出提示/
)

if (!handleFileSuccessMatch) {
  throw new Error('UploadFile must define a complete handleFileSuccess implementation.')
}

const handleFileSuccessBlock = handleFileSuccessMatch[0]

if (!/uploadFile\.name/.test(handleFileSuccessBlock)) {
  throw new Error('UploadFile must display the original uploaded file name.')
}

if (/name:\s*res\.data/.test(handleFileSuccessBlock)) {
  throw new Error('UploadFile must not use the server URL as the displayed file name.')
}

if (!uploadSource.includes('fileNameByUrl')) {
  throw new Error('UploadFile must retain original names when the model value is written back.')
}

if (!uploadSource.includes('resolveUploadFileName')) {
  throw new Error('UploadFile must have one URL filename decoder for legacy URL-only values.')
}

const materialsParserMatch = source.match(
  /const parseReviewMaterialsJson = \(review\?: EdhrNonconformanceReviewRespVO\):[\s\S]*?\n\}\n\nconst resolveReviewMaterialDisplay/
)

if (!materialsParserMatch) {
  throw new Error('Nonconformance review page must parse persisted review materials.')
}

const materialsParserBlock = materialsParserMatch[0]

if (!/fileName:\s*typeof material\?\.fileName === 'string'/.test(materialsParserBlock)) {
  throw new Error('Review material parsing must retain activeMaterials.fileName.')
}

const displayBlock = source.match(
  /const resolveReviewMaterialName = \(material: ReviewMaterialDisplay\) => \{[\s\S]*?const resolveReviewMaterialDisplay = \(review\?: EdhrNonconformanceReviewRespVO\) => \{[\s\S]*?\n\}/
)?.[0]

if (!displayBlock || !/fileName/.test(displayBlock)) {
  throw new Error('Review material display must prefer the persisted file name.')
}

console.log('PASS: eDHR nonconformance review material filename static contract')
