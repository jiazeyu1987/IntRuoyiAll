const fs = require('fs')
const path = require('path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const operationButton = read(
  'src/views/bpm/processInstance/detail/ProcessInstanceOperationButton.vue'
)

if (!operationButton.includes('isFormCenterBusinessActionProcess')) {
  throw new Error('Form center BPM approval must identify FORM_ACTION business keys explicitly')
}

if (!/businessKey[^\n]+startsWith\('FORM_ACTION:'\)/.test(operationButton)) {
  throw new Error('Form center BPM approval must detect FORM_ACTION business keys')
}

if (
  !/props\.processInstance\?\.processDefinition\?\.key\s*===\s*CONTROLLED_FILE_PROCESS_DEFINITION_KEY[\s\S]*!\s*isFormCenterBusinessActionProcess\.value/.test(
    operationButton
  )
) {
  throw new Error(
    'DCC controlled-file special approval guard must not block form-center BPM instances'
  )
}

console.log('form-center BPM DCC approval bypass static contract passed')
