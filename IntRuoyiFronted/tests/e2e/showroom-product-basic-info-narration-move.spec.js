const fs = require('fs')
const path = require('path')

const adminPagePath = path.resolve(__dirname, '../../src/views/showroom-admin/index.vue')
const detailDialogPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/product/ProductDetailDialog.vue'
)

const adminPageSource = fs.readFileSync(adminPagePath, 'utf8')
const detailDialogSource = fs.readFileSync(detailDialogPath, 'utf8')

const basicInfoDialogMatch = adminPageSource.match(
  /<el-dialog[\s\S]*?v-model="productDialogVisible"[\s\S]*?<\/el-dialog>/
)

if (!basicInfoDialogMatch) {
  throw new Error(`missing product basic info dialog in ${adminPagePath}`)
}

const basicInfoDialogSource = basicInfoDialogMatch[0]

if (!basicInfoDialogSource.includes('label="中文讲解稿"')) {
  throw new Error(`missing narration field in product basic info dialog: ${adminPagePath}`)
}

if (!basicInfoDialogSource.includes('生成讲解稿')) {
  throw new Error(`missing generate narration action in product basic info dialog: ${adminPagePath}`)
}

if (!adminPageSource.includes('productNarrationDraft.zhScriptText')) {
  throw new Error(`missing product narration draft binding in ${adminPagePath}`)
}

if (!adminPageSource.includes('handleGenerateProductNarrationScript')) {
  throw new Error(`missing product narration script handler in ${adminPagePath}`)
}

if (detailDialogSource.includes('label="讲解稿"')) {
  throw new Error(`obsolete narration field still exists in detail dialog: ${detailDialogPath}`)
}

if (detailDialogSource.includes('生成讲解稿')) {
  throw new Error(`obsolete narration action still exists in detail dialog: ${detailDialogPath}`)
}

if (detailDialogSource.includes('narrationDraft')) {
  throw new Error(`obsolete narration draft state still exists in detail dialog: ${detailDialogPath}`)
}

console.log('PASS: showroom product narration moved from detail dialog into basic info dialog')
