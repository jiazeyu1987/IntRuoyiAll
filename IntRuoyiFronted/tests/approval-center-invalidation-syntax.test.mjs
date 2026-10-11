import test from 'node:test'
import { readFileSync } from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import { SourceTextModule } from 'node:vm'

const files = [
  'store/modules/approvalCenterInvalidationState.ts',
  'store/modules/approvalCenterInvalidation.ts',
  'store/modules/approvalTodoBadge.ts',
  'views/approval-center/index.vue',
  'views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue',
  'views/mes/pro/production-release/PqcProductionReleasePage.vue',
  'views/mes/pro/edhr-batch/BatchExecutionListPage.vue',
  'views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue',
  'views/mes/pro/edhr/ApprovalDetailPage.vue',
  'views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue',
  'views/mes/pro/feedback/FrontlineReturnCorrectionPanel.vue',
  'views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue',
  'views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue',
  'views/mes/pro/edhr-deviation/index.vue',
  'views/mes/pro/edhr-deviation/DeviationDetail.vue'
]
for (const file of files) {
  test(`TypeScript script syntax only: ${file}`, () => {
    const source = readFileSync(new URL(`../src/${file}`, import.meta.url), 'utf8')
    const script = file.endsWith('.vue') ? source.match(/<script[^>]*>([\s\S]*?)<\/script>/)?.[1] : source
    if (!script) throw Error(`Missing script: ${file}`)
    new SourceTextModule(stripTypeScriptTypes(script, { mode: 'transform' }), { identifier: file })
  })
}
