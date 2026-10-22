// Runner unit tests only: no browser, server, or business writes are performed.
const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

const source = fs.readFileSync(path.join(__dirname, 'edhr-ai-loop/runner.cjs'), 'utf8')
const start = source.indexOf('async function completePqcReleaseNonconformanceReview(')
const end = source.indexOf('\nfunction assertPqcReleaseApprovedReceipt(', start)
assert.ok(start >= 0 && end > start)

function harness({ wrongSource = false, uploadFailure = false } = {}) {
  const actions = []
  const order = { activeOrderId: '91', workOrderCode: 'ORDER-91' }
  const created = { id: '71', reviewCode: 'NCR-71', activeOrderId: '91',
    sourceType: 'ACTIVE_ORDER', sourceId: wrongSource ? '92' : '91', reviewStatus: 'pending_review' }
  const disposed = { ...created, reviewStatus: 'closed', disposition: 'concession_release',
    qaSignature: 'QA-SIGNATURE-71', reviewMaterialUrl: '/materials/evidence.pdf' }
  const createPredicate = () => false
  const disposePredicate = () => false
  const children = {
    page: ['[data-edhr-ncr-page]', '[data-edhr-ncr-create-dialog][data-selected-active-order-id="91"]',
      '[data-edhr-ncr-review-dialog]', '[data-pqc-production-release-page]'],
    releaseRow: ['[data-pqc-production-release-nonconformance]'],
    '[data-edhr-ncr-page]': ['.el-table__row'],
    '.el-table__row': ['[data-edhr-ncr-review-process]'],
    '[data-edhr-ncr-create-dialog][data-selected-active-order-id="91"]': [
      '[data-edhr-ncr-create-reason] textarea', '[data-edhr-ncr-create-submit]'],
    '[data-edhr-ncr-review-dialog]': ['[data-edhr-ncr-review-material]',
      '[data-edhr-ncr-review-opinion] textarea', '[data-edhr-ncr-signature-password] input',
      '[data-edhr-ncr-concession-release]', '[data-edhr-ncr-disposition-result]', '[data-edhr-ncr-qa-signature]'],
    '[data-edhr-ncr-review-material]': ['input[type="file"]', 'li']
  }
  function locator(name) {
    return {
      locator(selector) {
        assert.ok(children[name]?.includes(selector), `Unreachable locator ${name} -> ${selector}`)
        return locator(selector)
      },
      filter(filter) {
        if (name === '.el-table__row') assert.deepEqual(filter.has, { exactText: 'NCR-71' })
        if (name === 'li') assert.equal(filter.hasText, 'evidence.pdf')
        if (name === '[data-edhr-ncr-qa-signature]') assert.equal(filter.hasText, disposed.qaSignature)
        actions.push(['filter', name])
        return this
      },
      async waitFor({ state }) { actions.push(['wait', name, state]) },
      async getAttribute(attribute) {
        if (attribute === 'data-pqc-production-release-application-id') return '31'
        assert.equal(attribute, 'data-selected-active-order-id')
        return '91'
      },
      async fill() { actions.push(['fill', name]) },
      async click() { actions.push(['click', name]) },
      async setInputFiles() {
        actions.push(['upload', name])
        if (uploadFailure) throw new Error('upload failed')
      }
    }
  }
  const page = { ...locator('page'),
    getByText(text, options) { assert.equal(options.exact, true); return { exactText: text } },
    waitForResponse(predicate) {
      assert.ok(predicate === createPredicate || predicate === disposePredicate)
      return Promise.resolve({ code: 0, data: predicate === createPredicate ? created : disposed })
    },
    async goBack() { actions.push(['back']) }
  }
  const context = { assert, path, signaturePassword: 'unit-test-only',
    isNonconformanceReviewCreateResponse: createPredicate,
    isNonconformanceReviewDisposeResponse: disposePredicate,
    readCommonResult: async response => response,
    createNonconformanceReviewFixture: () => path.join('unit-fixtures', 'evidence.pdf') }
  vm.createContext(context)
  vm.runInContext(source.slice(start, end) + '\nglobalThis.run = completePqcReleaseNonconformanceReview', context)
  return { actions, execute: () => context.run(page, order, locator('releaseRow'), 'unit-fixtures') }
}

test('NCR runner waits for the selected order and uses the exact created review dialog', async () => {
  const h = harness()
  const result = await h.execute()
  assert.equal(result.reviewId, '71')
  assert.equal(result.sourceId, '91')
  const clicks = h.actions.filter(action => action[0] === 'click').map(action => action[1])
  assert.deepEqual(clicks, ['[data-pqc-production-release-nonconformance]',
    '[data-edhr-ncr-create-submit]', '[data-edhr-ncr-review-process]', '[data-edhr-ncr-concession-release]'])
  assert.ok(h.actions.some(action => action[0] === 'wait' && action[1].includes('data-selected-active-order-id="91"')))
  assert.equal(h.actions.at(-2)[0], 'back')
})

test('NCR source mismatch stops before selecting or disposing a review', async () => {
  const h = harness({ wrongSource: true })
  await assert.rejects(h.execute(), /来源ID必须为当前活跃订单/)
  assert.ok(!h.actions.some(action => action[1] === '[data-edhr-ncr-review-process]'))
})

test('NCR upload failure stops without a disposition or another create click', async () => {
  const h = harness({ uploadFailure: true })
  await assert.rejects(h.execute(), /upload failed/)
  assert.equal(h.actions.filter(action => action[0] === 'click' && action[1] === '[data-edhr-ncr-create-submit]').length, 1)
  assert.ok(!h.actions.some(action => action[0] === 'click' && action[1] === '[data-edhr-ncr-concession-release]'))
})
