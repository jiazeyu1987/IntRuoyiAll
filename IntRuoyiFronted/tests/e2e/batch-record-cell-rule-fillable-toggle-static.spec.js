const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')

const assertIncludes = (content, token, message) => assert.ok(content.includes(token), message)
const assertNotIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

const matchFunctionBody = (source, functionName) => {
  const declaration = `const ${functionName} =`
  const start = source.indexOf(declaration)
  assert.notEqual(start, -1, `${functionName} must exist.`)
  const arrowStart = source.indexOf('=>', start)
  assert.notEqual(arrowStart, -1, `${functionName} must be an arrow function.`)
  const bodyStart = source.indexOf('{', arrowStart)
  assert.notEqual(bodyStart, -1, `${functionName} must use a block body.`)

  let depth = 0
  for (let index = bodyStart; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) return source.slice(bodyStart, index + 1)
    }
  }
  assert.fail(`${functionName} block body is not closed.`)
}

const selectRuleCellBody = matchFunctionBody(dialog, 'selectRuleCell')

assertIncludes(dialog, 'selectedCell', '右侧面板必须维护当前选中的真实单元格，即使该单元格没有规则。')
assertIncludes(dialog, 'isSelectedCellFillable', '右侧面板必须有可填写状态开关。')
assertIncludes(dialog, 'enableSelectedCellRule', '白色静态单元格必须通过显式动作转换为可填写规则。')
assertIncludes(dialog, 'disableSelectedCellRule', '蓝色可填写单元格必须能显式切回不可填写。')
assertIncludes(dialog, 'v-if="selectedCell"', '右侧面板必须在无规则的白色单元格选中后继续显示状态说明。')
assertIncludes(dialog, 'v-model="isSelectedCellFillable"', '右侧面板必须把可填写开关绑定到显式状态。')
assertIncludes(dialog, '不可填写', '白色单元格状态必须用文字说明，不得只靠颜色。')
assertIncludes(dialog, 'active-text="可填写"', '可填写状态必须保留在显式开关上。')
assertIncludes(dialog, 'inactive-text="不可填写"', '不可填写状态必须保留在显式开关上。')
assertNotIncludes(dialog, '静态单元格', '右侧面板不得再显示截图红框中的静态说明卡片。')
assertNotIncludes(dialog, '可填写单元格', '右侧面板不得再显示截图红框中的可填写说明卡片。')

assertNotIncludes(
  selectRuleCellBody,
  'buildManualRuleFromCell',
  '点击表格单元格只允许选中，不得自动创建填写规则或把白色单元格变蓝。'
)
assertNotIncludes(
  selectRuleCellBody,
  'ruleRows.value',
  '点击表格单元格不得直接修改规则集合；规则集合只能由显式开关动作修改。'
)

assertIncludes(dialog, 'BatchRecordReportApi.saveCellRules', '保存仍必须复用真实 cell-rules 接口。')
assertIncludes(dialog, 'rules: ruleRows.value.map(toManualReviewedRule)', '保存必须提交当前规则集合。')
assertNotIncludes(dialog, 'ruleRows.value.length > 0', '全部切为不可填写后必须允许保存空规则集合。')
assertNotIncludes(dialog, '当前表单暂无可确认填写规则。', '全部切为不可填写后不得用旧的空规则提示阻止保存。')
assertNotIncludes(dialog, 'catch {}', '前端请求失败不得静默吞掉。')

console.log('PASS: batch record cell rule fillable toggle static contract')
