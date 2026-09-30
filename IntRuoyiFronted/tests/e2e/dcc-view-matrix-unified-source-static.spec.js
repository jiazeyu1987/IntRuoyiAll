const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const reviewMatrixTable = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryReviewMatrixTable.vue'
)
const viewMatrixTable = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixTable.vue'
)
const matrixDialog = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryMatrixDialog.vue'
)
const lookupDialog = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryReviewMatrixUserLookupDialog.vue'
)
const viewLookupDialog = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixUserLookupDialog.vue'
)
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const categoryApi = readSource('src/api/dcc/controlledFile/fileCategories.ts')
const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')

assert(
  viewMatrixTable.includes('data-testid="dcc-view-matrix-table"') &&
    viewMatrixTable.includes('label="可查阅"'),
  'view matrix tab must render the independent view matrix table and readable subject rules'
)
assert(
  viewMatrixTable.includes('data-testid="dcc-view-matrix-effective-preview"') &&
    viewMatrixTable.includes('previewCategoryViewMatrixEffectiveAccess'),
  'view matrix tab must expose current effective view access preview'
)
assert(
  viewMatrixTable.includes('data-testid="dcc-view-matrix-effective-users"'),
  'view matrix preview must show effective view users'
)
assert(
  viewMatrixTable.includes('data-testid="dcc-view-matrix-preview-risks"'),
  'view matrix preview must show explicit risk markers'
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
    matrixDialog.includes('data-testid="dcc-review-matrix-effective-user-groups"') &&
    matrixDialog.includes('data-testid="dcc-matrix-preview-risks"'),
  'matrix dialog must show effective preview, actual users and explicit risks before save'
)
assert(
  reviewMatrixTable.includes('按人反查') &&
    reviewMatrixTable.includes('CategoryReviewMatrixUserLookupDialog'),
  'review matrix tab must provide reverse lookup by user'
)
assert(
  viewMatrixTable.includes('按人反查') &&
    viewMatrixTable.includes('CategoryViewMatrixUserLookupDialog'),
  'view matrix tab must provide independent reverse lookup by user'
)
assert(
  lookupDialog.includes('data-testid="dcc-user-lookup-table"') &&
    lookupDialog.includes('getReviewMatrixUserLookup'),
  'reverse lookup dialog must query and render user capability rows'
)
assert(
  viewLookupDialog.includes('data-testid="dcc-view-matrix-user-lookup-table"') &&
    viewLookupDialog.includes('getViewMatrixUserLookup'),
  'view matrix reverse lookup dialog must query and render view capability rows'
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
