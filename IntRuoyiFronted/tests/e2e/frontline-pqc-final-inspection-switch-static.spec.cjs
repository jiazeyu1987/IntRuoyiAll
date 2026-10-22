const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (filePath) => fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n')

const viewSource = read(path.join(
  frontendRoot,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
))
const feedbackApiSource = read(path.join(frontendRoot, 'src/api/mes/pro/feedback/index.ts'))

const extractConstFunctionBlock = (source, name) => {
  const start = source.indexOf(`const ${name} = async`)
  assert.ok(start >= 0, `missing function: ${name}`)
  const openIndex = source.indexOf('{', start)
  assert.ok(openIndex > start, `missing function body: ${name}`)
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(openIndex + 1, index)
      }
    }
  }
  assert.fail(`unterminated function: ${name}`)
}

assert.match(
  feedbackApiSource,
  /pqcTaskOptions: FrontlinePqcTaskOptionVO\[\]/,
  'PQC process API type must expose formal task options for switching.'
)
assert.match(
  viewSource,
  /type InspectionType = 'FIRST' \| 'PATROL' \| 'FINAL'/,
  'One-line PQC page must support configured FIRST/PATROL/FINAL inspection types.'
)
assert.match(
  viewSource,
  /const PQC_INSPECTION_RULE_LABELS: Record<FrontlinePqcInspectionRuleKey, string> = \{[\s\S]*FINAL:\s*(?:'末检'|PQC_INSPECTION_TYPE_LABELS\.FINAL)/,
  'PQC page must render FINAL when a formal final-inspection task exists.'
)
assert.doesNotMatch(
  viewSource,
  /@click="selectPqcInspectionType\('FINAL'\)"/,
  'One-line PQC page must not hard-code a FINAL button outside formal task options.'
)
assert.doesNotMatch(
  viewSource,
  /isFinalInspectionSelectable|finalInspectionApplicable === true/,
  'One-line PQC page must not gate task visibility on a frontend final-inspection fallback.'
)
assert.match(
  viewSource,
  /@click="selectPqcInspectionRule\(tab\.ruleKey\)"/,
  'Selecting a configured inspection button must pass the formal rule key.'
)
const selectRuleBlock = extractConstFunctionBlock(viewSource, 'selectPqcInspectionRule')
assert.match(
  selectRuleBlock,
  /selectedPqcInspectionRuleKey\.value = ruleKey[\s\S]*(?:applyPqcTaskOptionToSelectedProcess\(currentRuleOption\)|handleSelectProcess\(targetProcess\))/,
  'Selecting a configured inspection rule must keep the rule selected and apply or jump to the matching task option snapshot.'
)

console.log('PASS: frontline PQC final inspection displays only from formal task options')
