const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const Vue = require('vue')
const { renderToString } = require('vue/server-renderer')
const { parse } = require('vue/compiler-sfc')

const source = fs.readFileSync(path.join(__dirname, 'components/ActiveOrderSubmissionDetailPanel.vue'), 'utf8')
const { descriptor } = parse(source)
const script = descriptor.scriptSetup.content
const warningComputeds = script.slice(script.indexOf('const inputMaterialSources ='),
  script.indexOf('const productionMaterialListActualUsageByCode ='))
const template = descriptor.template.content.match(
  /<div\s+v-if="inputMaterialMatchWarnings.length"[\s\S]*?<\/div>\s*<\/div>/
)[0]

test('material mismatch renders a red advisory and updates when the order changes', async () => {
  const props = Vue.reactive({ detail: {
    inputMaterialUsages: [{ materialCode: 'A006.011.2008', actualQuantity: 1120 }],
    processes: [{ processName: '单包装工序', inputMaterials: [{
      materialCode: 'A006.011.2003',
      sourceMatchWarning: '冻结工艺输入物料 A006.011.2003 未匹配正式领料记录，请核对用料。'
    }] }]
  } })
  const warnings = new Function('computed', 'props',
    warningComputeds + '\nreturn inputMaterialMatchWarnings')(Vue.computed, props)
  const render = () => renderToString(Vue.createSSRApp({
    setup: () => ({ inputMaterialMatchWarnings: warnings }), template
  }))
  const html = await render()
  assert.match(html, /单包装工序：冻结工艺输入物料 A006\.011\.2003 未匹配正式领料记录/)
  assert.match(html, /role="status"/)
  assert.match(source, /\.active-order-material-match-warnings\s*\{[^}]*color: var\(--el-color-danger\)/)
  assert.equal(props.detail.inputMaterialUsages[0].materialCode, 'A006.011.2008')
  props.detail = { processes: [], inputMaterialUsages: [] }
  assert.deepEqual(warnings.value, [])
  assert.doesNotMatch(await render(), /未匹配正式领料/)
})
