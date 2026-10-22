const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(
  repoRoot,
  'src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'
)
const source = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')
const uploadFilePath = path.join(repoRoot, 'src/components/UploadFile/src/UploadFile.vue')
const uploadSource = fs.readFileSync(uploadFilePath, 'utf8')
const utilityPath = path.join(repoRoot, 'src/utils/fileName.ts')
const utilitySource = fs.readFileSync(utilityPath, 'utf8')

if (!source.includes("import { resolveUrlPathFileName } from '@/utils/fileName'")) {
  throw new Error('Nonconformance review page must use the shared filename protocol.')
}

if (!utilitySource.includes('new URL(url, origin).pathname')) {
  throw new Error('URL-only filename resolution must parse pathname with URL semantics.')
}

if (!utilitySource.includes('return decodeURIComponent(value)')) {
  throw new Error('URL-only filename resolution must decode a valid pathname segment.')
}

if (/while\s*\(/.test(utilitySource) || /decodeURIComponent\(.*decodeURIComponent/.test(utilitySource)) {
  throw new Error('Filename protocol must not repeatedly decode or guess a second URL layer.')
}

if (source.includes('decodeURIComponent(') || uploadSource.includes('decodeURIComponent(')) {
  throw new Error('Consumers must use the shared filename protocol instead of decoding persisted names.')
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

if (!uploadSource.includes("defineEmits(['update:modelValue', 'update:fileNames'])")) {
  throw new Error('UploadFile must publish authoritative original names with model URLs.')
}

const materialsParserMatch = source.match(
  /const parseReviewMaterialsJson = \(review\?: EdhrNonconformanceReviewRespVO\):[\s\S]*?\n\}\n\nconst resolveReviewMaterialName/
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
