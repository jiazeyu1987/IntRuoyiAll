const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const workflowApi = read('src/api/dcc/controlledFile/workflow.ts')
const workbench = read('src/views/dcc/controlled-file/workbench/index.vue')

for (const [name, key] of [
  ['CONTROLLED_FILE_PROCESS_DEFINITION_KEY', 'dcc-controlled-file-approval'],
  ['CONTROLLED_FILE_UPLOAD_PROCESS_DEFINITION_KEY', 'dcc-controlled-file-upload'],
  ['CONTROLLED_FILE_REVISION_PROCESS_DEFINITION_KEY', 'dcc-controlled-file-revision'],
  ['CONTROLLED_FILE_OBSOLETE_PROCESS_DEFINITION_KEY', 'dcc-controlled-file-obsolete']
]) {
  assert.match(
    workflowApi,
    new RegExp(`export const ${name}\\s*=\\s*'${key}'`),
    `workflow API must export ${name}`
  )
}

assert.match(
  workflowApi,
  /candidateSourceType:\s*'USER'\s*\|\s*'POSITION'\s*\|\s*'DEPT'/,
  'workflow route preview type must include DEPT candidate source'
)

const keyArrayMatch = workbench.match(/const DCC_APPROVAL_PROCESS_DEFINITION_KEYS = \[([\s\S]*?)\]/)
assert.ok(keyArrayMatch, 'DCC workbench must define approval process key array')
const keyArray = keyArrayMatch[1]
for (const name of [
  'CONTROLLED_FILE_PROCESS_DEFINITION_KEY',
  'CONTROLLED_FILE_UPLOAD_PROCESS_DEFINITION_KEY',
  'CONTROLLED_FILE_REVISION_PROCESS_DEFINITION_KEY',
  'CONTROLLED_FILE_OBSOLETE_PROCESS_DEFINITION_KEY',
  'EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_KEY'
]) {
  assert.ok(keyArray.includes(name), `DCC workbench must query ${name}`)
}

assert.match(
  workbench,
  /businessObjectId[\s\S]*businessKey/,
  'DCC workbench must prefer process businessObjectId before numeric businessKey'
)
assert.match(
  workbench,
  /缺少受控文件业务对象 ID，无法定位受控文件/,
  'DCC workbench must fail visibly when neither businessObjectId nor numeric businessKey can locate a file'
)

console.log('PASS: DCC three workflow process key static contract')
