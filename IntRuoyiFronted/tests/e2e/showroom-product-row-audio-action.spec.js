const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const listSource = readText('src/views/showroom-admin/components/ProductListTable.vue')
const indexSource = readText('src/views/showroom-admin/index.vue')
const audioDialogSource = readText('src/views/showroom-admin/product/ProductAudioDialog.vue')
const productDialogSource =
  indexSource.match(/<el-dialog[\s\S]*?v-model="productDialogVisible"[\s\S]*?<\/el-dialog>/)?.[0] ||
  ''

assert.match(
  listSource,
  /emit\('assign', row\.raw\)[\s\S]*?>\s*指派\s*<\/el-button>[\s\S]*emit\('open-audio-dialog', row\.raw\)[\s\S]*?>\s*语音\s*<\/el-button>/,
  '产品列表行操作区必须在“指派”旁边展示“语音”并触发行级语音弹框事件'
)

assert.doesNotMatch(
  listSource,
  /:loading="String\(generatingAudioProductId \?\? ''\) === row\.productId"/,
  '行级语音按钮不应再直接绑定旧的生成 loading 状态'
)

assert.match(
  indexSource,
  /@open-audio-dialog="openProductAudioDialog"/,
  '产品列表必须绑定行级语音弹框处理函数'
)

assert.match(
  indexSource,
  /<ProductAudioDialog[\s\S]*v-model="productAudioDialogVisible"[\s\S]*@generated="handleProductAudioDialogGenerated"/,
  '页面必须挂载产品语音弹框并监听生成后的刷新事件'
)

assert.match(
  indexSource,
  /const openProductAudioDialog = async \(product: Record<string, unknown>\) => \{[\s\S]*productAudioDialogVisible\.value = true/,
  '点击产品行语音按钮时必须先打开语音弹框，而不是直接生成'
)

assert.match(
  indexSource,
  /const handleProductAudioDialogGenerated = async \(\) => \{[\s\S]*await loadProductRows\(\)/,
  '弹框内生成成功后必须刷新产品列表'
)

assert.match(
  indexSource,
  /const resolveProductAudioSourceRevisionId = \(product: Record<string, unknown>\) => \{[\s\S]*throw new Error\('产品缺少来源版本，无法打开语音弹框'\)/,
  '产品缺少来源版本时必须直接失败，不得静默兜底'
)

assert.match(
  indexSource,
  /const handleGenerateProductNarrationAudioFromRow = async \(product: Record<string, unknown>\)[\s\S]*ShowroomAdminApi\.generateProductNarrationAudio\([\s\S]*productId[\s\S]*sourceRevisionId[\s\S]*await loadProductRows\(\)/,
  '语音弹框内生成必须继续复用现有单品语音生成 API 并刷新列表'
)

assert.ok(audioDialogSource.includes('title="产品语音"'), '产品语音弹框必须固定标题为“产品语音”')
assert.ok(audioDialogSource.includes("label: '中文语音'"), '产品语音弹框必须包含中文语音区域')
assert.ok(audioDialogSource.includes("label: '英文语音'"), '产品语音弹框必须包含英文语音区域')
assert.ok(audioDialogSource.includes('生成中英文语音'), '产品语音弹框必须提供生成中英文语音按钮')

assert.doesNotMatch(
  productDialogSource,
  /Generate Audio|生成语音/,
  '编辑产品弹框不应再展示单品语音生成按钮'
)

console.log('PASS: showroom product row audio action is wired outside the edit dialog')
