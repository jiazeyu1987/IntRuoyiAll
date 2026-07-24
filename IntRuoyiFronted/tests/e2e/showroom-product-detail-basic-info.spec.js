const fs = require('fs')
const path = require('path')

const filePath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/product/ProductDetailDialog.vue'
)
const source = fs.readFileSync(filePath, 'utf8')

if (source.includes('生成语音')) {
  throw new Error(`found obsolete generate audio action in ${filePath}`)
}

if (source.includes('<el-form-item label="讲解稿">')) {
  throw new Error(`found obsolete narration field in ${filePath}`)
}

if (source.includes('生成讲解稿')) {
  throw new Error(`found obsolete generate narration action in ${filePath}`)
}

if (source.includes('narrationDraft')) {
  throw new Error(`found obsolete narration draft state in ${filePath}`)
}

if (source.includes('narrationLoadError')) {
  throw new Error(`found obsolete narration load error state in ${filePath}`)
}

if (source.includes('getNarration')) {
  throw new Error(`found obsolete narration fetch in ${filePath}`)
}

console.log('PASS: showroom product detail dialog no longer owns narration UI')
