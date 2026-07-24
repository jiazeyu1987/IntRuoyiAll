const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(repoRoot, 'src/views/bpm/model/form/index.vue'), 'utf8')

assert.match(
  source,
  /const\s+syncProcessData\s*=\s*\(\)\s*=>\s*\{[\s\S]*?formData\.value\.type\s*===\s*BpmModelType\.BPMN[\s\S]*?processData\.value\s*=\s*formData\.value\.bpmnXml[\s\S]*?formData\.value\.type\s*===\s*BpmModelType\.SIMPLE[\s\S]*?processData\.value\s*=\s*formData\.value\.simpleModel[\s\S]*?\}/,
  'BPM model form must centralize processData sync from bpmnXml/simpleModel.'
)

assert.match(
  source,
  /formData\.value\s*=\s*await\s+ModelApi\.getModel\(modelId\)[\s\S]*?syncProcessData\(\)/,
  'Update/copy model loading must sync returned bpmnXml into processData before opening process design.'
)

assert.match(
  source,
  /watch\(\s*\(\)\s*=>\s*\[\s*formData\.value\.type\s*,\s*formData\.value\.bpmnXml\s*,\s*formData\.value\.simpleModel\s*\]/,
  'Process data watcher must include bpmnXml and simpleModel, not only type.'
)

assert.match(
  source,
  /immediate:\s*true/,
  'Process data watcher must still initialize new-flow designer data immediately.'
)

console.log('PASS: BPM model form processData sync static contract')
