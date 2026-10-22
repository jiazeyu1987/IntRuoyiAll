const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { ref, reactive, computed } = require('vue')
const { parse } = require('vue/compiler-sfc')
const source = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)
const ast = ts.createSourceFile(
  'panel.ts',
  parse(source).descriptor.scriptSetup.content,
  ts.ScriptTarget.Latest,
  true
)
const names = [
  'productionScrapQuantity',
  'normalizeProductionQuantity',
  'updateProductionOutputQuantity',
  'adjustProductionOutputQuantity',
  'getProductionDefectQuantity',
  'updateProductionDefectQuantity',
  'adjustProductionDefectQuantity',
  'assertProductionSubmissionReady',
  'resolveProductionProgressQuantity',
  'resolveProductionLossQuantity',
  'buildProductionFormalSubmitConfirmation',
  'buildFrontlineFormalSubmitPayload',
  'buildProductionLossDetailsFromDraft',
  'buildProductionLossDetailsPayload',
  'buildProductionLossDetailsForSubmitScope',
  'buildProductionMaterialDetailsPayload',
  'buildProductionFieldValues'
]
const selected = ast.statements
  .filter(ts.isVariableStatement)
  .flatMap((s) => [...s.declarationList.declarations])
  .filter((d) => names.includes(d.name.getText(ast)))
assert.equal(
  selected.length,
  names.length,
  'test must execute every real component quantity function'
)
const code = ts.transpileModule(selected.map((d) => `const ${d.getText(ast)}`).join('\n'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText

function harness(materials = false, output = 10, loss = 2) {
  const deps = {
    computed,
    configuredProductionMaterials: ref(
      materials ? [{ key: '100', materialId: 100, materialName: '正式输出物料' }] : []
    ),
    configuredDefectReasons: ref([{ key: '7', reasonId: 7, reasonCode: 'SCRATCH', label: '划伤' }]),
    productionDraft: reactive({ outputQuantity: output }),
    productionDefectDraft: reactive({ 7: loss }),
    productionMaterialDrafts: reactive({
      100: {
        outputQuantity: output,
        defectQuantities: { 7: loss },
        selectedDeviceKeys: [],
        deviceParameters: {}
      }
    }),
    persistActiveProductionMaterialDraft() {},
    findMissingProductionDeviceParameters: () => [],
    buildProductionSelectedDevicesFromKeys: () => [],
    buildProductionDeviceParameterReadingsFromDraft: () => [],
    visibleDeviceCards: ref([]),
    buildProductionSelectedDevicesForSubmitScope: () => [],
    buildProductionParameterReadingsForSubmitScope: () => [],
    buildProductionEquipmentParameterRulesPayloadFromMaterialDetails: () => [],
    buildProductionClearanceConfirmationPayload: () => [],
    productionOrderLabel: ref('WO-OWN'),
    selectedProcessLabel: ref('装配'),
    selectedEmployeeLabel: ref('正式员工'),
    readFrontlineFormalSubmitContext: () => ({
      activeOrderId: 437,
      routeId: 11,
      routeProcessId: 15,
      processId: 21,
      workstationId: 3,
      approveUserId: 99,
      signatureEmployeeId: 341,
      deviceAccountUserId: 341
    }),
    assertProductionSubmitSnapshotContext() {},
    assertFrontlineFormalSubmitContext() {},
    productionSignaturePassword: ref('test-only-signature'),
    buildFrontlineProductionSubmitIdempotencyKey: () => 'fixed-request-key',
    firstRouteQueryText: () => '',
    draft: { fieldValues: {} },
    deviceState: {
      runtimeConfig: {
        frontlineSessionSnapshotId: 'frozen-id',
        frontlineSessionSnapshotHash: 'frozen-hash'
      },
      selectedEmployee: { systemUserId: 341 }
    },
    context: { actualEmployeeId: 341, templateCode: 'PRODUCTION' },
    expectedTemplateCode: ref('PRODUCTION'),
    buildProductionStructuredRawPayload: (raw, formal, materialDetails) => ({
      ...raw,
      materialDetails
    }),
    FRONTLINE_FIELD_CODES: {
      DEVICE: 'device',
      DEVICE_PARAMETERS: 'parameters',
      OUTPUT_QUANTITY: 'output',
      SCRAP_QUANTITY: 'scrap'
    }
  }
  const panel = Function(
    ...Object.keys(deps),
    `${code}; return {${names.join(',')}}`
  )(...Object.values(deps))
  return { panel, deps }
}

for (const materials of [false, true]) {
  for (const [output, loss] of [
    [10, 2],
    [1, 3],
    [10, 12]
  ]) {
    test(`${materials ? 'material' : 'no-material'} submission keeps qualified ${output} and independent loss ${loss}`, () => {
      const { panel } = harness(materials, output, loss)
      const details = panel.assertProductionSubmissionReady()
      const payload = panel.buildFrontlineFormalSubmitPayload({ fieldValues: {} }, details)
      assert.equal(payload.feedbackPayload.outputQuantity, output)
      assert.equal(payload.feedbackPayload.lossQuantity, loss)
      assert.equal(payload.feedbackPayload.laborScrapQuantity, loss)
      assert.equal(payload.actualEmployeeId, 341)
      assert.equal(payload.signatureEmployeeId, 341)
      assert.deepEqual(payload.feedbackPayload.lossDetails, [
        { reasonId: 7, reasonCode: 'SCRATCH', reasonName: '划伤', quantity: loss }
      ])
      assert.equal(panel.buildProductionFieldValues(details).output, output)
      assert.equal(panel.buildProductionFieldValues(details).scrap, loss)
      const summary = panel.buildProductionFormalSubmitConfirmation(details)
      assert.match(summary, new RegExp(`完成数量合计：${output}件`))
      assert.match(summary, new RegExp(`损耗数量合计：${loss}件`))
      assert.match(summary, /完成数量为合格产出，损耗另计/)
      if (materials) {
        assert.equal(payload.materialDetails[0].outputQuantity, output)
        assert.equal(payload.materialDetails[0].lossQuantity, loss)
      }
    })
  }
  test(`${materials ? 'material' : 'no-material'} zero and negative completion still cannot submit`, () => {
    for (const output of [0, -1]) {
      const { panel } = harness(materials, output, 2)
      assert.throws(() => panel.assertProductionSubmissionReady(), /完成数量.*大于 0/)
    }
  })
}
test('actual quantity inputs keep nonnegative integer bounds without a completion-based loss cap', () => {
  const { panel, deps } = harness(false, 1, 0)
  panel.updateProductionOutputQuantity({ target: { value: '-2' } })
  assert.equal(deps.productionDraft.outputQuantity, 0)
  panel.adjustProductionOutputQuantity(-1)
  assert.equal(deps.productionDraft.outputQuantity, 0)
  for (const value of ['Infinity', '-Infinity', 'NaN']) {
    panel.updateProductionOutputQuantity({ target: { value } })
    assert.equal(deps.productionDraft.outputQuantity, 0)
    assert.throws(() => panel.assertProductionSubmissionReady(), /完成数量.*大于 0/)
  }
  panel.updateProductionOutputQuantity({ target: { value: '1' } })
  panel.updateProductionDefectQuantity('7', { target: { value: '3.8' } })
  assert.equal(panel.productionScrapQuantity.value, 3)
  panel.adjustProductionDefectQuantity('7', 1)
  assert.equal(panel.productionScrapQuantity.value, 4)
  panel.updateProductionDefectQuantity('7', { target: { value: '-9' } })
  panel.adjustProductionDefectQuantity('7', -1)
  assert.equal(panel.productionScrapQuantity.value, 0)
})
test('loss payload uses only formal configured reasons and positive detail quantities', () => {
  const { panel } = harness()
  assert.deepEqual(panel.buildProductionLossDetailsFromDraft({ 7: 2, 999: 10 }), [
    { reasonId: 7, reasonCode: 'SCRATCH', reasonName: '划伤', quantity: 2 }
  ])
  assert.deepEqual(panel.buildProductionLossDetailsFromDraft({ 7: 0 }), [])
})
test('multiple materials keep existing minimum qualified progress and independent loss sum', () => {
  const { panel } = harness(true)
  const details = [
    { outputQuantity: 10, lossQuantity: 2 },
    { outputQuantity: 7, lossQuantity: 9 }
  ]
  assert.equal(panel.resolveProductionProgressQuantity(details), 7)
  assert.equal(panel.resolveProductionLossQuantity(details), 11)
})
test('missing signature password and required device parameters still block real payload/readiness', () => {
  const { panel, deps } = harness()
  deps.productionSignaturePassword.value = ' '
  assert.throws(() => panel.buildFrontlineFormalSubmitPayload({}, []), /签名密码/)
  const validatorSource = ts.transpileModule(
    `const validator = ${selected.find((d) => d.name.getText(ast) === 'assertProductionSubmissionReady').initializer.getText(ast)}`,
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }
  ).outputText
  const validator = Function(
    'persistActiveProductionMaterialDraft',
    'buildProductionMaterialDetailsPayload',
    'configuredProductionMaterials',
    'productionDraft',
    'productionScrapQuantity',
    'findMissingProductionDeviceParameters',
    `${validatorSource};return validator`
  )(
    () => {},
    () => [],
    deps.configuredProductionMaterials,
    deps.productionDraft,
    panel.productionScrapQuantity,
    () => ['温度']
  )
  assert.throws(() => validator(), /请填写设备参数：温度/)
})
