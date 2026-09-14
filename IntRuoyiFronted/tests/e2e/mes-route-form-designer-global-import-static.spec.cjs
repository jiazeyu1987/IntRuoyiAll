const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const projectRoot = path.resolve(__dirname, '..', '..')

const read = (relativePath) =>
  fs.readFileSync(path.join(projectRoot, relativePath), 'utf8')

const formCreatePlugin = read('src/plugins/formCreate/index.ts')
const bpmEditor = read('src/views/bpm/form/editor/index.vue')
const infraBuild = read('src/views/infra/build/index.vue')

assert.ok(
  !formCreatePlugin.includes("@form-create/designer") &&
    !formCreatePlugin.includes('app.use(FcDesigner)'),
  'setupFormCreate must not globally import or install FcDesigner; non-designer pages such as MES route edit should not load form-designer chunk.'
)

assert.match(
  bpmEditor,
  /import\s+FcDesigner\s+from\s+['"]@form-create\/designer['"]/,
  'BPM form editor must locally import FcDesigner because global installation is forbidden.'
)

assert.match(
  infraBuild,
  /import\s+FcDesigner\s+from\s+['"]@form-create\/designer['"]/,
  'Infra build designer must locally import FcDesigner because global installation is forbidden.'
)

console.log('mes-route-form-designer-global-import-static: PASS')
