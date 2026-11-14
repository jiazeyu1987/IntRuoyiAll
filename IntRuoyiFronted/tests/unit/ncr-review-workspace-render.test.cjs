const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { renderToString } = require('vue/server-renderer')
const { parse, compileScript } = require('vue/compiler-sfc')

const file = path.resolve(__dirname, '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewWorkspace.vue')
const { descriptor } = parse(fs.readFileSync(file, 'utf8'), { filename: file })
const script = compileScript(descriptor, { id: 'ncr-workspace-render', inlineTemplate: true })
const shell = vue.defineComponent({ setup: (_, { slots }) => () => vue.h('div', slots.default?.()) })
const exportsObject = {}
vm.runInNewContext(ts.transpileModule(script.content, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText, {
  exports: exportsObject,
  require(id) {
    if (id === 'vue') return vue
    if (id.endsWith('.vue')) return { default: shell }
    assert.equal(id, '@/api/mes/pro/edhr/nonconformanceReview')
    return {
      REVIEW_STATUS_PENDING_REVIEW: 'pending_review',
      DISPOSITION_CONCESSION_RELEASE: 'concession_release',
      DISPOSITION_REWORK: 'rework', DISPOSITION_VOID: 'void'
    }
  }
}, { filename: file })

async function render(overrides) {
  const app = vue.createSSRApp(exportsObject.default, {
    review: { id: 109, reviewCode: 'NCR-109', reviewStatus: 'pending_review', nonconformanceReason: '原因' },
    sourceLabel: '活跃订单', statusLabel: '进行中', frozenAt: '2026-11-14 22:48:13', closedAt: '',
    dispositionLabel: '', dispositionNote: '', canDispose: true, readOnly: false, canAssign: false,
    assigningReviewer: false, submitting: false, materials: [{ url: 'material', fileName: '评审材料.txt' }],
    attempts: [], uploadingCount: 0, opinion: '', signature: '', errors: { materials: '', opinion: '', signature: '' },
    ...overrides
  })
  for (const name of ['el-tag', 'el-button', 'el-alert', 'el-form', 'el-form-item', 'el-input', 'Icon']) app.component(name, shell)
  return renderToString(app)
}

test('historical and unauthorized reviews keep the workspace layout without editing or disposition controls', async () => {
  for (const permissions of [{ readOnly: true }, { canDispose: false }]) {
    const html = await render(permissions)
    assert.match(html, /ncr-workspace__columns/)
    assert.match(html, /评审材料.txt/)
    assert.match(html, /当前评审仅供查看/)
    assert.doesNotMatch(html, /type="file"|让步放行|ncr-workspace__actions/)
  }
})

test('completed reviews show formal materials, opinion, signature and result in the same workspace', async () => {
  const html = await render({
    canDispose: false, review: { id: 109, reviewCode: 'NCR-109', reviewStatus: 'closed', reviewOpinion: '正式评审意见', qaSignature: 'QA签名' },
    statusLabel: '已完成', dispositionLabel: '返工', dispositionNote: '已进入返工流程', closedAt: '2026-11-15 08:00:00'
  })
  for (const value of ['NCR-109', '评审材料.txt', '正式评审意见', 'QA签名', '返工', '2026-11-15 08:00:00']) assert.ok(html.includes(value))
  assert.doesNotMatch(html, /type="file"|ncr-workspace__actions/)
})
