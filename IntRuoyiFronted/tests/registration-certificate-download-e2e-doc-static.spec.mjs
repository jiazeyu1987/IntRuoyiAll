import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd().endsWith('IntRuoyiFronted') ? join(process.cwd(), '..') : process.cwd()
const doc = readFileSync(
  join(root, 'e2e_test/registration/download/registration-certificate-download-e2e-acceptance.md'),
  'utf8'
)

assert.match(doc, /下载授权有效期：注册部经理审批通过后 24 小时内有效；超过 24 小时后必须失效。/)
assert.match(doc, /`注册证批准日期` 指注册证版本上的批准日期/)
assert.match(doc, /基础格式：`项目代码_注册证批准日期_产品名称_注册证号 \+ 原扩展名`；若项目代码为空，则项目代码段按空字符串处理，文件名仍保留下划线分隔/)
assert.match(doc, /项目代码不存在时按空字符串校验。/)
assert.match(doc, /项目代码为空时保留空项目代码段。/)
assert.match(doc, /审批通过时间只用于授权有效期判断。/)

for (const staleText of [
  '3 天',
  '三天',
  '3天',
  '下载授权审批通过日期',
  '批准日期与 E2E-4 记录的审批通过日期一致'
]) {
  assert.doesNotMatch(doc, new RegExp(staleText), `download E2E doc must not keep stale wording: ${staleText}`)
}

console.log('PASS: registration certificate download E2E doc matches approved 24-hour filename rules')
