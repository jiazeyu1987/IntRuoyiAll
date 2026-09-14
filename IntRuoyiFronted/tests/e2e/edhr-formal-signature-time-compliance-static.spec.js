const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()

const files = [
  'src/views/mes/pro/edhr/ApprovalDetailPage.vue',
  'src/views/mes/pro/edhr/ExecutionPage.vue',
  'src/views/mes/pro/edhr/signatureTime.ts'
]

for (const file of files) {
  const source = fs.readFileSync(path.resolve(root, file), 'utf8')
  assert.ok(
    !source.includes('placeholder="可选择人工签名时间"'),
    `${file} 正式电子签名入口不得继续暴露人工选择签名时间输入框。`
  )
  assert.ok(
    !source.includes('选择人工签名时间时必须说明原因'),
    `${file} 正式电子签名入口不得继续收集人工签名时间原因。`
  )
}

const signatureTime = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/edhr/signatureTime.ts'), 'utf8')
assert.ok(
  /return undefined/.test(signatureTime) && !signatureTime.includes('selectedSignedAt: normalizeSelectedSignedAt'),
  '正式签名 payload 不得携带 selectedSignedAt；签名时间必须由后端系统时间生成。'
)
