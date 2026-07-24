const fs = require('fs')
const path = require('path')

const listPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/components/ProductListTable.vue'
)
const listSource = fs.readFileSync(listPath, 'utf8')

if (/>\s*生成\s*<\/el-button>/.test(listSource)) {
  throw new Error(`found obsolete product narration action label "生成" in ${listPath}`)
}

if (!/emit\('assign', row\.raw\)[\s\S]*?>\s*指派\s*<\/el-button>[\s\S]*emit\('open-audio-dialog', row\.raw\)[\s\S]*?>\s*语音\s*<\/el-button>/.test(listSource)) {
  throw new Error(`missing row-level audio action next to assignment in ${listPath}`)
}

if (!/>\s*指派\s*<\/el-button>/.test(listSource)) {
  throw new Error(`missing whole-product assignment action label "指派" in ${listPath}`)
}

if (/>\s*指派填写\s*<\/el-button>/.test(listSource)) {
  throw new Error(`found obsolete whole-product assignment action label "指派填写" in ${listPath}`)
}

if (!/>\s*基础\s*<\/el-button>/.test(listSource)) {
  throw new Error(`missing product basic info action label "基础" in ${listPath}`)
}

if (/>\s*基础信息\s*<\/el-button>/.test(listSource)) {
  throw new Error(`found obsolete product basic info action label "基础信息" in ${listPath}`)
}

if (!/>\s*详细信息\s*<\/el-button>/.test(listSource)) {
  throw new Error(`missing product detail action label "详细信息" in ${listPath}`)
}

if (!listSource.includes('指派中')) {
  throw new Error(`missing filling status option in ${listPath}`)
}

const dialogPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/product/ProductWholeAssignmentDialog.vue'
)
const dialogSource = fs.readFileSync(dialogPath, 'utf8')

if (!dialogSource.includes('产品整单指派')) {
  throw new Error(`missing whole-product assignment dialog in ${dialogPath}`)
}

if (!dialogSource.includes('${user.nickname} / ${user.username}')) {
  throw new Error(`missing nickname and username search label in ${dialogPath}`)
}

if (!dialogSource.includes('SHOWROOM_PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE')) {
  throw new Error(`missing whole-product assignment field code in ${dialogPath}`)
}

const indexPath = path.resolve(__dirname, '../../src/views/showroom-admin/index.vue')
const indexSource = fs.readFileSync(indexPath, 'utf8')

if (!indexSource.includes('@assign="openProductWholeAssignment"')) {
  throw new Error(`missing product list assignment binding in ${indexPath}`)
}

if (!indexSource.includes('@open-audio-dialog="openProductAudioDialog"')) {
  throw new Error(`missing product list row audio dialog binding in ${indexPath}`)
}

if (!indexSource.includes('<ProductWholeAssignmentDialog')) {
  throw new Error(`missing whole-product assignment dialog mount in ${indexPath}`)
}

console.log('PASS: showroom product whole-assignment simplified flow is wired in the frontend source')
