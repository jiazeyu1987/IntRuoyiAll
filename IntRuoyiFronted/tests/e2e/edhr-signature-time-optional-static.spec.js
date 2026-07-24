const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const sourcePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/signatureTime.ts')
const source = fs.readFileSync(sourcePath, 'utf8')

assert(
  source.includes("String(form.selectedSignedAt || '').trim()") &&
    source.includes("String(form.selectedTimeZone || '').trim()") &&
    source.includes("String(form.selectedTimeReason || '').trim()"),
  '签名时间 payload 构造必须兼容 Element Plus 空日期 null，并先区分用户是否真正选择了人工签名时间。'
)

assert(
  /if \(!selectedSignedAt && !selectedTimeReason\) \{\s*return undefined\s*\}/.test(source),
  '仅有默认签名时区时，不得强制用户选择人工签名时间。'
)

assert(
  /if \(selectedTimeReason && !selectedSignedAt\) \{[\s\S]*请选择签名时间/.test(source) &&
    /if \(selectedSignedAt && !selectedTimeReason\) \{[\s\S]*签名时间原因不能为空/.test(source),
  '用户填写签名时间或原因任一项时，必须要求签名时间和原因成对完整。'
)

console.log('PASS: eDHR signature time optional static contract')
