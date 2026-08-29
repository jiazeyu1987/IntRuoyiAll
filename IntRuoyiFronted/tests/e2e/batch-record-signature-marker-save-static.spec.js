const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')
const api = read('src/api/mes/pro/batchrecordreport/index.ts')

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

assert.match(
  api,
  /signatureCellMarkers\?:\s*BatchRecordReportSignatureCellMarkerVO\[\]/,
  '保存填写配置请求必须显式支持签名单元格 marker。'
)

const markerBuilderBody = matchFunctionBody(dialog, 'buildSignatureMarkersForSave')
assert.match(
  markerBuilderBody,
  /sortRules\(rules\)[\s\S]*filter\(isSignatureRuleForSave\)/,
  '保存前必须只从电子签名规则生成签名 marker。'
)
assert.match(
  markerBuilderBody,
  /sheetLayout\.value\?\.rows\?\.\[String\(rule\.rowIndex\)\]\?\.cells\?\.\[String\(rule\.columnIndex\)\]/,
  '保存前必须优先读取原布局里已有的 edhrSignature，避免覆盖提交签名或复核签名语义。'
)
assert.match(
  markerBuilderBody,
  /enabled:\s*true[\s\S]*actionType:\s*existingMarker\?\.actionType\s*\|\|\s*'FORM_REVIEW'[\s\S]*signatureCellKey:\s*existingMarker\?\.signatureCellKey\s*\|\|\s*buildTemplateFieldIdentity\(rule\.rowIndex,\s*rule\.columnIndex\)/,
  '签名 marker 必须启用，并具备 actionType 与 signatureCellKey。'
)

const confirmAllRulesBody = matchFunctionBody(dialog, 'confirmAllRules')
assert.match(
  confirmAllRulesBody,
  /const rules = ruleRows\.value\.map\(toManualReviewedRule\)/,
  '保存前必须冻结人工确认后的规则集合。'
)
assert.match(
  confirmAllRulesBody,
  /signatureCellMarkers:\s*buildSignatureMarkersForSave\(rules\)/,
  '保存填写配置必须把签名 marker 与 rules 一起提交。'
)

console.log('PASS: batch record signature marker save static contract')
