const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { renderToString } = require('vue/server-renderer')
const { parse, compileScript } = require('vue/compiler-sfc')
const elementPlus = require('element-plus')
const root = 'src/views/dcc/registration-certificate/shared/'
const transpile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const facts = {}
new Function('exports', transpile(fs.readFileSync(root + 'facts.ts', 'utf8')))(facts)
const script = compileScript(parse(fs.readFileSync(root + 'FactsFields.vue', 'utf8')).descriptor, { id: 'ce-creation', inlineTemplate: true })
const component = {}
new Function('exports', 'require', transpile(script.content))(component, id => id === './facts' ? facts : require(id))
const render = (type, creation, extra = {}) => {
  const app = vue.createSSRApp({ render: () => vue.h(elementPlus.ElForm, { labelWidth: '140px' }, () => vue.h(component.default, { modelValue: { certificateType: type, ownProduct: false, classification: 'IIa', certificateStatus: 'VALID', expiryDate: '2030-01-01', ceCertificates: [] }, creation, ...extra })) })
  app.use(elementPlus.default)
  app.provide(elementPlus.ID_INJECTION_KEY, { prefix: 1024, current: 0 })
  app.provide(elementPlus.ZINDEX_INJECTION_KEY, { current: 0 })
  return renderToString(app)
}

test('CE creation removes four inputs while retaining expiry, remarks and revision records', async () => {
  const html = await render('CE', true)
  for (const label of ['Initial Issue Date', '型号规格', '变更描述']) assert.ok(!html.includes(label), label)
  assert.ok(html.includes('批准日期'))
  for (const label of ['有效期至', '产品英文名称', '公告机构代码', '备注', '证书Rev.记录']) assert.ok(html.includes(label), label)
  assert.doesNotThrow(() => facts.validateFacts({ certificateType: 'CE', ownProduct: false, classification: 'IIa', certificateStatus: 'VALID', expiryDate: '2030-01-01', ceCertificates: [] }))
  assert.throws(() => facts.validateFacts({ certificateType: 'CE', classification: 'IIa', certificateStatus: 'VALID' }), /请填写有效期至/)
})

test('creation has independent fields and record forms keep change details separate from approval dates', async () => {
  const ce = await render('CE', false)
  for (const label of ['型号规格', '变更描述', '有效期至', '证书Rev.记录']) assert.ok(ce.includes(label), label)
  for (const label of ['Initial Issue Date', '首次获证日期']) assert.ok(!ce.includes(label), label)
  const domestic = await render('DOMESTIC', true)
  assert.ok(domestic.includes('批准日期'))
  assert.ok(!domestic.includes('变更描述'))
  for (const label of ['首次获证日期', '有效期至', '证件状态']) assert.ok(domestic.includes(label), label)
  const fda = await render('FDA', true)
  assert.ok(!fda.includes('变更描述'))
  assert.ok(fda.includes('批准日期'))
  assert.ok((await render('FDA', false)).includes('变更描述'))
  const domesticRecord = await render('DOMESTIC', false)
  for (const label of ['首次获证日期', '有效期至', '变更描述']) assert.ok(domesticRecord.includes(label), label)
  assert.ok(domesticRecord.includes('批准日期'))
})

test('creation schema and validation reject removed fields while change validation remains independent', () => {
  for (const type of ['DOMESTIC', 'CE', 'FDA']) {
    const model = type === 'FDA'
      ? { certificateType: type, ownProduct: false, classification: 'II', submissionType: 'PMA', customerHeld: false, approvalLetterStatus: 'VALID', factoryRegistrationStatus: 'ACTIVE', approvalDate: '2026-01-01' }
      : { certificateType: type, ownProduct: false, classification: 'II', certificateStatus: 'VALID', approvalDate: '2025-01-01', expiryDate: '2030-01-01', ...(type === 'DOMESTIC' ? { certificateNo: 'CN-1', projectCode: 'CUSTOMER-CN', firstObtainedDate: '2025-01-01', effectiveDate: '2025-01-02' } : {}) }
    assert.doesNotThrow(() => facts.validateCreationFacts(model))
    const removed = ['changeDescription', ...(type === 'CE' ? ['firstObtainedDate', 'modelSpecifications'] : [])]
    for (const field of removed) {
      assert.ok(!facts.creationFieldsFor(type).includes(field))
      const invalid = { ...model, [field]: field === 'modelSpecifications' ? ['A'] : field.endsWith('Date') ? '2025-01-01' : '变更内容' }
      assert.throws(() => facts.validateCreationFacts(invalid), /新建.*不支持/)
      assert.doesNotThrow(() => facts.validateFacts(invalid))
    }
  }
})

test('domestic and CE creation require explicit approval dates with their own date order', () => {
  for (const certificateType of ['DOMESTIC', 'CE']) {
    const model = { certificateType, ownProduct: false, classification: 'II', certificateStatus: 'VALID', expiryDate: '2027-01-20', approvalDate: '2026-11-27', ...(certificateType === 'DOMESTIC' ? { certificateNo: 'CN-1', projectCode: 'CUSTOMER-CN', firstObtainedDate: '2026-11-26', effectiveDate: '2026-11-28' } : {}) }
    assert.ok(facts.creationFieldsFor(certificateType).includes('approvalDate'))
    assert.doesNotThrow(() => facts.validateCreationFacts(model))
    for (const approvalDate of [undefined, null, '']) assert.throws(() => facts.validateCreationFacts({ ...model, approvalDate }), /请填写批准日期/)
    const invalid = { ...model, approvalDate: certificateType === 'CE' ? model.expiryDate : '2026-11-29' }
    assert.throws(() => facts.validateCreationFacts(invalid), certificateType === 'CE' ? /批准日期必须早于有效期至/ : /批准日期不能晚于生效日期/)
  }
})

test('change certificate sections omit original domestic and CE approval dates but readonly facts retain them', async () => {
  for (const type of ['DOMESTIC', 'CE']) {
    assert.ok((await render(type, true, { creationSection: 'certificate' })).includes('批准日期'))
    assert.ok(!(await render(type, true, { creationSection: 'certificate', change: true })).includes('批准日期'))
    assert.ok((await render(type, false, { readonly: true })).includes('批准日期'))
  }
  assert.ok((await render('FDA', true, { creationSection: 'certificate', change: true })).includes('批准日期'), 'FDA edits its approval facts without a separate change approval date')
})


test('other domestic creation exposes and requires its own project code without requiring a system project', async () => {
  const model = { certificateType: 'DOMESTIC', ownProduct: false, classification: 'I', certificateStatus: 'VALID', certificateNo: 'CN-OTHER', firstObtainedDate: '2026-11-28', approvalDate: '2026-11-28', effectiveDate: '2026-11-28', expiryDate: '2028-11-28' }
  for (const projectCode of [undefined, '', '  ']) assert.throws(() => facts.validateCreationFacts({ ...model, projectCode }), /请填写实际项目代码/)
  assert.doesNotThrow(() => facts.validateCreationFacts({ ...model, projectCode: 'CUSTOMER-CN' }))
  assert.doesNotThrow(() => facts.validateCreationFacts({ ...model, ownProduct: true }))
  assert.ok((await render('DOMESTIC', true, { creationSection: 'attributes' })).includes('实际项目代码'))
  assert.ok(!(await render('CE', true, { creationSection: 'attributes' })).includes('实际项目代码'))
})
