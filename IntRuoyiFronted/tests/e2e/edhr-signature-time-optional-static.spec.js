const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const sourcePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/signatureTime.ts')
const source = fs.readFileSync(sourcePath, 'utf8')

assert(
  /buildSignatureTimePayload[\s\S]*return undefined/.test(source),
  '正式电子签名 payload 必须始终省略人工选择时间，由后端系统时间生成。'
)

assert(
  !source.includes('normalizeSelectedSignedAt(selectedSignedAt)'),
  '正式电子签名 payload 不得再携带 selectedSignedAt。'
)

console.log('PASS: eDHR signature time compliance static contract')
