import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
import { parse, compileScript } from 'vue/compiler-sfc'
// Isolated component fixture; this is not real business E2E.

const require = createRequire(import.meta.url)
const sourceDir = fileURLToPath(new URL('../src/views/mes/pro/feedback/', import.meta.url))
const source = readFileSync(sourceDir + 'FrontlineFixedTemplatePanel.vue', 'utf8')
const parsed = parse(source).descriptor
const elements = []
function visit(node) {
  if (node.type === 1) elements.push(node)
  for (const child of node.children || []) visit(child)
}
visit(parsed.template.ast)
const summary = elements.find(node => node.props?.some(prop => prop.name === 'data-pqc-review-summary'))
const dialog = elements.find(node => node.props?.some(prop => prop.name === 'ref' && prop.value?.content === 'pqcReviewHistoryDialog'))
assert.ok(summary && dialog)

test('actual review view opens locally and preserves rejection history when current review becomes pending', async () => {
  const fixture = { pqcTaskId: 31, roundNo: 2, submittedEventId: 701, taskStatus: 'SUBMITTED', currentReviewStatus: 'REJECTED',
    reviewHistory: [{ reviewId: 81, eventId: 701, reviewStatus: 'REJECTED', reviewRound: 1,
      reviewRemark: '请更正设备', reviewedAt: '2026-11-13T08:30:00', reviewerUserId: 6, reviewerName: '复核人员', revisionId: 11 }] }
  const component = `<template>${summary.loc.source}${dialog.loc.source}</template><script setup>
    import {ref,computed} from 'vue';
    import {getPqcReviewLabel,getPqcReviewCounts,formatPqcReviewStatus,formatPqcReviewTime} from './frontlinePqcReviewDisplay';
    const activePqcTaskOption=ref(${JSON.stringify(fixture)});
    window.currentTask=activePqcTaskOption;
    const pqcReviewHistoryDialog=ref();
    const pqcReviewLabel=computed(()=>getPqcReviewLabel(activePqcTaskOption.value));
    const pqcReviewCounts=computed(()=>getPqcReviewCounts([activePqcTaskOption.value]));
    </script>`
  const compiled = compileScript(parse(component).descriptor, { id: 'review-history-test', inlineTemplate: true })
  const bundle = await createRequire(require.resolve('vite'))('esbuild').build({
    stdin: { contents: `import {createApp} from 'vue';import View from './review-history-test.vue';createApp(View).mount('#app')`, resolveDir: sourceDir, loader: 'js' },
    bundle: true, write: false, format: 'iife', platform: 'browser',
    define: { 'process.env.NODE_ENV': '"test"', __VUE_OPTIONS_API__: 'true', __VUE_PROD_DEVTOOLS__: 'false', __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'false' },
    plugins: [{ name: 'review-test-view', setup(build) {
      build.onLoad({ filter: /review-history-test\.vue$/ }, () => ({ contents: compiled.content, loader: 'js', resolveDir: sourceDir }))
      build.onResolve({ filter: /review-history-test\.vue$/ }, () => ({ path: sourceDir + 'review-history-test.vue' }))
    } }]
  })
  const browser = await require('playwright').chromium.launch({ headless: true })
  try {
    const page = await browser.newPage(), requests = [], errors = []
    page.on('request', request => requests.push(request.url()))
    page.on('pageerror', error => errors.push(error.message))
    await page.setContent('<div id="app"></div>')
    await page.addScriptTag({ content: bundle.outputFiles[0].text })
    assert.match(await page.locator('[data-pqc-review-summary]').innerText(), /退回待修改/)
    await page.locator('[data-pqc-review-history-open]').click()
    assert.equal(await page.locator('dialog').evaluate(el => el.open), true)
    assert.match(await page.locator('dialog').innerText(), /701/)
    assert.match(await page.locator('[data-pqc-review-history]').innerText(), /81.*第 1 轮/s)
    assert.match(await page.locator('[data-pqc-review-history]').innerText(), /请更正设备/)
    await page.getByRole('button', { name: '关闭', exact: true }).click()
    await page.evaluate(() => { window.currentTask.value.currentReviewStatus = 'PENDING' })
    assert.match(await page.locator('[data-pqc-review-summary]').innerText(), /复核状态：待复核/)
    await page.locator('[data-pqc-review-history-open]').click()
    assert.match(await page.locator('[data-pqc-review-history]').innerText(), /退回修改/)
    await page.evaluate(() => { window.currentTask.value.reviewHistory = undefined })
    assert.match(await page.locator('[data-pqc-review-history-missing]').innerText(), /数据缺失/)
    await page.evaluate(() => { window.currentTask.value.reviewHistory = [] })
    assert.match(await page.locator('[data-pqc-review-history-empty]').innerText(), /当前正式事件暂无复核记录/)
    await page.evaluate(() => { window.currentTask.value.submittedEventId = undefined })
    assert.match(await page.locator('[data-pqc-review-history-empty]').innerText(), /尚未正式提交/)
    assert.deepEqual(requests, [])
    assert.deepEqual(errors, [])
  } finally { await browser.close() }
})
