const fs = require('fs')
const path = require('path')

const listPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/components/ProductListTable.vue'
)
const listSource = fs.readFileSync(listPath, 'utf8')

const assertContains = (pattern, message) => {
  if (!pattern.test(listSource)) {
    throw new Error(message)
  }
}

const assertNotContains = (pattern, message) => {
  if (pattern.test(listSource)) {
    throw new Error(message)
  }
}

assertContains(
  /@click="emit\('create'\)"[\s\S]*?<\s*\/el-button>/,
  `missing create action in ${listPath}`
)
assertContains(
  /@click="emit\('create'\)"[\s\S]*?>[\s\S]*?新增[\s\S]*?<\s*\/el-button>/,
  `create action label should be "新增" in ${listPath}`
)
assertNotContains(
  /@click="emit\('create'\)"[\s\S]*?>[\s\S]*?新增产品[\s\S]*?<\s*\/el-button>/,
  `found obsolete create action label "新增产品" in ${listPath}`
)

assertContains(
  /@click="emit\('import-excel'\)"[\s\S]*?>[\s\S]*?导入[\s\S]*?<\s*\/el-button>/,
  `import action label should be "导入" in ${listPath}`
)
assertNotContains(
  /@click="emit\('import-excel'\)"[\s\S]*?>[\s\S]*?导入 Excel[\s\S]*?<\s*\/el-button>/,
  `found obsolete import action label "导入 Excel" in ${listPath}`
)

assertContains(
  /@click="emit\('export-excel'\)"[\s\S]*?>[\s\S]*?导出[\s\S]*?<\s*\/el-button>/,
  `export action label should be "导出" in ${listPath}`
)
assertNotContains(
  /@click="emit\('export-excel'\)"[\s\S]*?>[\s\S]*?导出 Excel[\s\S]*?<\s*\/el-button>/,
  `found obsolete export action label "导出 Excel" in ${listPath}`
)

assertContains(
  /@click="emit\('batch-generate-sales-countries'\)"[\s\S]*?>[\s\S]*?一键在售国家[\s\S]*?<\s*\/el-button>/,
  `batch countries on sale action label should be "一键在售国家" in ${listPath}`
)
assertNotContains(
  /@click="emit\('batch-generate-selling-points'\)"[\s\S]*?>[\s\S]*?一键卖点[\s\S]*?<\s*\/el-button>/,
  `found obsolete batch selling points action label "一键卖点" in ${listPath}`
)
assertNotContains(
  /batch-generate-selling-points/,
  `found obsolete batch selling points event in ${listPath}`
)

assertContains(
  /@click="handleBatchGenerateNarrationScriptClick"[\s\S]*?>[\s\S]*?一键讲解[\s\S]*?<\s*\/el-button>/,
  `batch narration script action label should be "一键讲解" in ${listPath}`
)
assertNotContains(
  /@click="emit\('batch-generate-narration-script'\)"[\s\S]*?>[\s\S]*?一建讲解[\s\S]*?<\s*\/el-button>/,
  `found typo batch narration script action label in ${listPath}`
)

assertContains(
  /@click="emit\('batch-generate-audio'\)"[\s\S]*?>[\s\S]*?一键语音[\s\S]*?<\s*\/el-button>/,
  `batch audio action label should be "一键语音" in ${listPath}`
)
assertNotContains(
  /<el-tag[\s\S]*v-if="batchAudioAutoCheckLabel"[\s\S]*?>[\s\S]*?batchAudioAutoCheckLabel[\s\S]*?<\s*\/el-tag>/,
  `batch audio auto-check tag should not render inside the toolbar in ${listPath}`
)
assertNotContains(
  /@click="emit\('batch-generate-audio'\)"[\s\S]*?>[\s\S]*?一键生成所有中英文语音[\s\S]*?<\s*\/el-button>/,
  `found obsolete batch audio action label in ${listPath}`
)

assertContains(
  /@click="handleBatchGenerateCoverClick"[\s\S]*?>[\s\S]*?一键封面[\s\S]*?<\s*\/el-button>/,
  `batch cover action label should be "一键封面" in ${listPath}`
)
assertContains(/一键封面任务/, `batch cover task status should render in the fixed task area in ${listPath}`)
assertNotContains(
  /@click="emit\('batch-generate-cover'\)"[\s\S]*?>[\s\S]*?一键生成所有封面[\s\S]*?<\s*\/el-button>/,
  `found obsolete batch cover action label in ${listPath}`
)

assertContains(
  /\.showroom-product-list__toolbar\s*\{[\s\S]*?align-items:\s*center;/,
  `toolbar layout should vertically align controls in ${listPath}`
)
assertContains(
  /\.showroom-product-list__actions\s*\{[\s\S]*?justify-content:\s*flex-end;[\s\S]*?flex-wrap:\s*nowrap;/,
  `toolbar actions should stay on a single row in ${listPath}`
)
assertContains(
  /\.showroom-product-list__toolbar\s*:deep\(\.el-input__wrapper\)[\s\S]*?min-height:\s*40px;/,
  `toolbar input wrappers should use a consistent height in ${listPath}`
)
assertContains(
  /\.showroom-product-list__actions\s*:deep\(\.el-button\)[\s\S]*?min-height:\s*40px;/,
  `toolbar buttons should use a consistent height in ${listPath}`
)

console.log('PASS: showroom product toolbar layout is constrained to a compact single row')
