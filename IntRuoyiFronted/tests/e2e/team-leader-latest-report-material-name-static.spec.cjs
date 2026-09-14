const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

assert.match(
  page,
  /const\s+submissionMaterialNameCache\s*=\s*ref<Record<string,\s*string>>\(\{\}\)/,
  'latest report rows must keep a reactive material-name cache keyed by material id'
)
assert.match(
  page,
  /const\s+enrichSubmissionMaterialNames\s*=\s*async\s*\(\s*rows:\s*ProcessPoolTimelineEventVO\[\]\s*\)/,
  'latest report list load must enrich missing submitted material names before display'
)
assert.match(
  page,
  /await\s+enrichSubmissionMaterialNames\(list\)[\s\S]*submissionList\.value\s*=\s*list/,
  'submission list must resolve formal material names before assigning the visible list'
)
assert.match(
  page,
  /isPlaceholderMaterialName[\s\S]*物料名称未记录/,
  'placeholder text 物料名称未记录 must be treated as missing, not as a real material name'
)
assert.match(
  page,
  /resolveSubmissionMaterialTitle[\s\S]*!isPlaceholderMaterialName\(item\.materialName\)[\s\S]*submissionMaterialNameCache\.value\[String\(item\.materialId\)\]/,
  'expanded material title must read the enriched formal material name cache'
)
assert.match(
  page,
  /throw new Error\(`物料 \$\{item\.materialId\} 缺少真实物料名称，不能展示报工明细`\)/,
  'missing formal material names must fail visibly instead of silently falling back to placeholders'
)

console.log('PASS: latest report material titles use formal material names')
