const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const projectRoot = path.resolve(__dirname, '../..')

for (const page of [
  'src/views/mes/pro/batchrecordformlist/index.vue',
  'src/views/mes/pro/batchrecordformlist/index.vue'
]) {
  const source = fs.readFileSync(path.join(projectRoot, page), 'utf8')

  assert(
    source.includes('batch-record-word-import-form__action-row'),
    `${page} 必须保留重建 V1.0 / 升版导入操作区。`
  )
  assert(
    !source.includes('v-if="isWordImportRouteDuplicateBlocked(wordImportDialog.preflight)"'),
    `${page} 不得在导入弹窗内展示同名工艺路线阻断提示。`
  )
  assert(
    !source.includes("wordImportDialog.selectedAction === 'REBUILD_V1' && wordImportDialog.preflight.referenceBlockers?.length"),
    `${page} 不得在导入弹窗内展示历史引用绑定提示。`
  )
}

console.log('PASS: batch-record Word import dialog blocking alerts hidden')
