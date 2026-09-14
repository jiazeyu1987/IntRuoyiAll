const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const helperPath = 'src/views/mes/pro/edhr/shared/dateTime.ts'
const helper = read(helperPath)
const recordChangePage = read('src/views/mes/pro/edhr/RecordChangePage.vue')
const formTraceChangeTab = read('src/views/mes/pro/edhr/form-trace/FormTraceChangeTab.vue')

assert.match(helper, /import \{ formatDate \} from '@\/utils\/formatTime'/)
assert.match(helper, /formatEdhrDateTime/)
assert.match(helper, /edhrDateTimeFormatter/)
assert.match(helper, /\/\^\\d\+\$\/\.test\(trimmedValue\)/)
assert.match(helper, /new Date\(Number\(trimmedValue\)\)/)

for (const [sourceName, source] of [
  ['RecordChangePage.vue', recordChangePage],
  ['FormTraceChangeTab.vue', formTraceChangeTab]
]) {
  assert.match(
    source,
    /edhrDateTimeFormatter,\s*formatEdhrDateTime/,
    `${sourceName} 必须导入共享时间格式化函数。`
  )
  for (const propName of ['requestedAt', 'effectiveAt']) {
    assert.match(
      source,
      new RegExp(`label="${propName === 'requestedAt' ? '申请时间' : '生效时间'}"[\\s\\S]*prop="${propName}"[\\s\\S]*:formatter="edhrDateTimeFormatter"`),
      `${sourceName} 的 ${propName} 列必须格式化为 YYYY-MM-DD HH:mm:ss。`
    )
    assert.match(
      source,
      new RegExp(`formatEdhrDateTime\\(selectedChange\\?\\.${propName}\\)`),
      `${sourceName} 的详情 ${propName} 必须格式化为 YYYY-MM-DD HH:mm:ss。`
    )
  }
}

console.log('PASS: eDHR record change time format static contract')
