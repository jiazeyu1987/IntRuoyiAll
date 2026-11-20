const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse, compileScript } = require('vue/compiler-sfc')
const { chromium } = require('playwright')

// Component regression with real Element Plus controls inside the signing form.
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const compile = source => ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText
const componentCode = file => compile(compileScript(parse(read(file)).descriptor, {
  id: file, inlineTemplate: true
}).content)
const base = 'src/views/dcc/controlled-file/'

;(async () => {
  const browser = await chromium.launch({ headless: true })
  try {
    for (const kind of ['detail', 'relations']) {
      const page = await browser.newPage()
      try {
        await page.setContent('<div id="app"></div>')
        await page.addStyleTag({ path: path.join(root, 'node_modules/element-plus/dist/index.css') })
        await page.addScriptTag({ path: path.join(root, 'node_modules/vue/dist/vue.global.js') })
        await page.addScriptTag({ path: path.join(root, 'node_modules/element-plus/dist/index.full.js') })
        await page.evaluate(({ kind, code, actions, arrangement, versions }) => {
          const load = (code, dependencies = {}) => {
            const exports = {}
            new Function('exports', 'require', code)(exports, id => {
              if (id === 'vue') return Vue
              if (Object.hasOwn(dependencies, id)) return dependencies[id]
              throw Error('Unexpected dependency: ' + id)
            })
            return exports
          }
          const Component = load(code, {
            '@/views/system/user/components/UserSelectV2.vue': { default: {
              props: ['modelValue', 'disabled', 'userOptions'], emits: ['update:modelValue'],
              setup: (props, { emit }) => () => Vue.h(ElementPlus.ElSelect, {
                modelValue: props.modelValue, disabled: props.disabled,
                'onUpdate:modelValue': value => emit('update:modelValue', value)
              }, { default: () => props.userOptions.map(user => Vue.h(ElementPlus.ElOption, { value: user.id, label: user.nickname })) })
            } },
            '../workflow/workflow-actions': load(actions),
            '../shared/version-display': load(versions),
            './arrangement-form': load(arrangement)
          }).default
          const state = Vue.reactive({ disabled: false, rows: [], saved: [], contextKey: 'task-1' })
          let panel
          const relations = [{ masterId: '101', fileName: '关联文件', versionNo: 'AB/1' }]
          const users = [{ id: '201', name: '整改负责人甲' }]
          const app = Vue.createApp({ setup: () => () => Vue.h(ElementPlus.ElForm, {}, {
            default: () => Vue.h(ElementPlus.ElFormItem, { label: '补充关联' }, {
              default: () => Vue.h(Component, kind === 'detail' ? {
                ref: value => { panel = value }, relations, users, saved: state.saved,
                contextKey: state.contextKey, disabled: state.disabled
              } : {
                ref: value => { panel = value }, relations, assignees: users,
                modelValue: state.rows, readonly: state.disabled,
                'onUpdate:modelValue': rows => { state.rows = rows }
              })
            })
          }) })
          app.use(ElementPlus).mount('#app')
          window.fixture = { state, validate: () => panel.validate() }
        }, {
          kind,
          code: componentCode(base + (kind === 'detail' ? 'detail/DetailRelationArrangements.vue' : 'relations/DccRelationArrangements.vue')),
          actions: compile(read(base + 'workflow/workflow-actions.ts')),
          arrangement: compile(read(base + 'relations/arrangement-form.ts')),
          versions: compile(read(base + 'shared/version-display.ts'))
        })
        const checkbox = page.locator('.el-checkbox__inner')
        const checked = () => page.locator('input[type=checkbox]').isChecked()
        await checkbox.click()
        await page.locator('.el-select').waitFor({ state: 'visible', timeout: 2000 })
        assert.equal(await checked(), true, kind + ': clicking the visible box selects remediation')
        await page.locator('.el-select').click()
        await page.getByRole('option', { name: '整改负责人甲' }).click()
        const deadline = page.locator('.el-table input.el-input__inner')
        await deadline.fill('2026-12-01 12:00:00')
        await deadline.press('Tab')
        assert.deepEqual(await page.evaluate(() => fixture.validate()), [
          { relatedMasterId: '101', assigneeUserId: '201', dueAt: '2026-12-01 12:00:00' }
        ])
        await checkbox.click()
        assert.equal(await checked(), false)
        assert.equal(await page.locator('.el-select').count(), 0)
        assert.deepEqual(await page.evaluate(() => fixture.validate()), [])
        await page.evaluate(() => { fixture.state.disabled = true })
        await checkbox.click({ force: true })
        assert.equal(await checked(), false, kind + ': disabled controls cannot add rows')
        await page.evaluate(() => { fixture.state.disabled = false })
        await page.locator('input[type=checkbox]').focus()
        await page.keyboard.press('Space')
        assert.equal(await checked(), true, kind + ': keyboard selection still works')
        console.log('PASS ' + kind + ': click, fields, validation, deselection, disabled and keyboard')
      } finally { await page.close() }
    }
  } finally { await browser.close() }
})().catch(error => { console.error(error); process.exitCode = 1 })
