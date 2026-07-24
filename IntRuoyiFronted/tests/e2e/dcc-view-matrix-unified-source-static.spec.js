const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const matrixTable = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryReviewMatrixTable.vue'
)
const matrixDialog = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryMatrixDialog.vue'
)
const lookupDialog = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryReviewMatrixUserLookupDialog.vue'
)
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const categoryApi = readSource('src/api/dcc/controlledFile/fileCategories.ts')
const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')

assert(
  matrixTable.includes('data-testid="dcc-view-matrix-subjects"'),
  'review matrix tab must show effective view subjects'
)
assert(
  matrixTable.includes('data-testid="dcc-pending-preview-rule"'),
  'review matrix tab must show route snapshot pending preview rule'
)
assert(
  matrixTable.includes('data-testid="dcc-download-rule-summary"'),
  'review matrix tab must show download rule separately'
)
assert(
  matrixTable.includes('data-testid="dcc-view-matrix-risks"'),
  'review matrix tab must show risk markers'
)
assert(
  categoryApi.includes('ControlledFileCategoryReviewMatrixSubjectVO') &&
    categoryApi.includes('downloadRuleSubjects') &&
    categoryApi.includes('risks') &&
    categoryApi.includes('/matrix/effective-preview') &&
    categoryApi.includes('/review-matrix/user-lookup'),
  'category API contract must expose view subjects, risks, effective preview and user lookup'
)
assert(
  matrixDialog.includes('previewCategoryApprovalMatrixEffectiveAccess') &&
    matrixDialog.includes('data-testid="dcc-matrix-effective-preview"') &&
    matrixDialog.includes('data-testid="dcc-matrix-effective-users"') &&
    matrixDialog.includes('data-testid="dcc-matrix-preview-risks"'),
  'matrix dialog must show effective preview, actual users and explicit risks before save'
)
assert(
  matrixTable.includes('按人反查') &&
    matrixTable.includes('CategoryReviewMatrixUserLookupDialog'),
  'review matrix tab must provide reverse lookup by user'
)
assert(
  lookupDialog.includes('data-testid="dcc-user-lookup-table"') &&
    lookupDialog.includes('getReviewMatrixUserLookup'),
  'reverse lookup dialog must query and render user capability rows'
)
assert(
  detailPage.includes('data-testid="dcc-detail-access-explanation"') &&
    detailPage.includes('getControlledFileAccessExplanation') &&
    detailPage.includes('formatAccessExplanation') &&
    detailPage.includes('accessExplanationError') &&
    !detailPage.includes('catch {'),
  'detail page must show why current user can or cannot view without silent catch fallback'
)
assert(
  workflowApi.includes('/dcc/controlled-files/${id}/access-explanation') &&
    workflowApi.includes('ControlledFileAccessExplanationVO'),
  'workflow API must expose access explanation endpoint'
)

console.log('dcc view matrix unified source static checks passed')
