import assert from 'node:assert/strict'
import fs from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import path from 'node:path'
import test from 'node:test'
import vm from 'node:vm'

import { parse, compileScript } from 'vue/compiler-sfc'
import ts from 'typescript'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const sliceBetween = (source, startMarker, endMarker) => {
  const startIndex = source.indexOf(startMarker)
  assert.notEqual(startIndex, -1, `missing start marker: ${startMarker}`)
  const endIndex = source.indexOf(endMarker, startIndex)
  assert.notEqual(endIndex, -1, `missing end marker: ${endMarker}`)
  return source.slice(startIndex, endIndex)
}

const extractConstArrowFunction = (source, functionName) => {
  const marker = `const ${functionName} = `
  const startIndex = source.indexOf(marker)
  assert.notEqual(startIndex, -1, `missing function marker: ${marker}`)
  const functionStartIndex = startIndex + marker.length
  const bodyStartIndex = source.indexOf('{', functionStartIndex)
  assert.notEqual(bodyStartIndex, -1, `missing function body for: ${functionName}`)
  let depth = 0
  for (let index = bodyStartIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') {
      depth += 1
    } else if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(functionStartIndex, index + 1)
      }
    }
  }
  throw new Error(`missing function end for: ${functionName}`)
}

const loadIndexFunction = (functionName) => {
  const source = readText('src/views/showroom-admin/index.vue')
  const functionSource = extractConstArrowFunction(source, functionName)
  const transformed = stripTypeScriptTypes(
    `const ${functionName} = ${functionSource}; result = ${functionName}`,
    { mode: 'transform' }
  )
  const sandbox = {
    result: null,
    normalizeId(value, label) {
      if (value === undefined || value === null || value === '') {
        throw new Error(`${label} is required`)
      }
      const normalized = Number(value)
      if (!Number.isFinite(normalized)) {
        throw new Error(`${label} must be numeric`)
      }
      return normalized
    }
  }
  vm.createContext(sandbox)
  vm.runInContext(transformed, sandbox)
  return sandbox.result
}

const loadProductListSetup = (overrides = {}) => {
  const filePath = path.join(root, 'src/views/showroom-admin/components/ProductListTable.vue')
  const source = fs.readFileSync(filePath, 'utf8')
  const { descriptor } = parse(source, { filename: filePath })
  const script = compileScript(descriptor, { id: 'product-list-table-test' })
  const compiled = ts.transpileModule(script.content, {
    compilerOptions: {
      target: ts.ScriptTarget.ES2020,
      module: ts.ModuleKind.CommonJS
    }
  }).outputText

  const vueStub = {
    defineComponent: (options) => options,
    reactive: (value) => value,
    watch: () => undefined,
    computed: (getter) => ({
      get value() {
        return getter()
      }
    })
  }

  const sandbox = {
    exports: {},
    module: { exports: {} },
    require(specifier) {
      if (specifier === 'vue') {
        return vueStub
      }
      throw new Error(`Unexpected dependency: ${specifier}`)
    },
    reactive: vueStub.reactive,
    watch: vueStub.watch,
    computed: vueStub.computed
  }

  vm.createContext(sandbox)
  vm.runInContext(compiled, sandbox)

  const component = sandbox.module.exports.default || sandbox.exports.default
  return component.setup({ products: [], loading: false, ...overrides }, { expose() {}, emit() {} })
}

const incompleteSnapshot = {
  productId: 26,
  productCode: 'P-26',
  currentRevisionId: 1026,
  live: false,
  editable: false,
  incomplete: true,
  revision: {
    revisionId: 1026,
    revisionNo: 1,
    status: 'DRAFT',
    nameCn: '一次性使用射频房间隔穿刺针',
    nameEn: '',
    fields: {
      lifecycle_stage: 'R_AND_D'
    }
  },
  displayRevision: {
    revisionId: 1026,
    revisionNo: 1,
    status: 'DRAFT',
    nameCn: '一次性使用射频房间隔穿刺针',
    nameEn: '',
    fields: {
      lifecycle_stage: 'R_AND_D'
    }
  }
}

const completeSnapshot = {
  productId: 88,
  productCode: 'P-88',
  currentRevisionId: 2088,
  live: true,
  editable: true,
  incomplete: false,
  revision: {
    revisionId: 2088,
    revisionNo: 3,
    status: 'APPROVED',
    nameCn: '球囊导管',
    nameEn: 'Balloon Catheter',
    fields: {
      owner_company_id: 9001,
      product_owner_type: 'YINGTAI',
      lifecycle_stage: 'REGISTERED'
    }
  },
  displayRevision: {
    revisionId: 2088,
    revisionNo: 3,
    status: 'PUBLISHED',
    nameCn: '球囊导管',
    nameEn: 'Balloon Catheter',
    fields: {
      owner_company_id: 9001,
      product_owner_type: 'YINGTAI',
      lifecycle_stage: 'REGISTERED',
      cover_image: '/admin-api/infra/file/get?id=98001'
    }
  },
  latestNarration: {
    narrationVersionId: 3001,
    language: 'ZH',
    audienceType: 'PUBLIC',
    status: 'PUBLISHED',
    live: true,
    audioReady: true,
    audioUrl: '/admin-api/infra/file/get?id=99001',
    voice: 'ruoxi'
  }
}

const noAudioNarrationSnapshot = {
  ...completeSnapshot,
  productId: 99,
  productCode: 'P-99',
  latestNarration: {
    narrationVersionId: 3002,
    language: 'ZH',
    audienceType: 'PUBLIC',
    status: 'DRAFT',
    live: false,
    audioReady: false,
    audioUrl: '',
    voice: ''
  }
}

const audioWithoutVoiceSnapshot = {
  ...completeSnapshot,
  productId: 100,
  productCode: 'P-100',
  latestNarration: {
    narrationVersionId: 3003,
    language: 'ZH',
    audienceType: 'PUBLIC',
    status: 'PUBLISHED',
    live: true,
    audioReady: true,
    audioUrl: '/admin-api/infra/file/get?id=99002',
    voice: ''
  }
}

const latestDraftWithPublishedDisplaySnapshot = {
  productId: 101,
  productCode: 'P-101',
  currentRevisionId: 4101,
  live: true,
  editable: true,
  incomplete: false,
  revision: {
    revisionId: 4201,
    revisionNo: 5,
    status: 'PENDING_GAOXIN_APPROVAL',
    nameCn: '球囊导管 Draft',
    nameEn: 'Balloon Catheter Draft',
    fields: {
      owner_company_id: 9001,
      product_owner_type: 'YINGTAI',
      lifecycle_stage: 'R_AND_D',
      cover_image: '/admin-api/infra/file/get?id=98099'
    },
    activeAssignment: {
      assignmentId: 8001,
      assigneeUserId: 700,
      status: 'OPEN'
    }
  },
  displayRevision: {
    revisionId: 4101,
    revisionNo: 4,
    status: 'PUBLISHED',
    nameCn: '球囊导管 Live',
    nameEn: 'Balloon Catheter Live',
    fields: {
      owner_company_id: 9001,
      product_owner_type: 'YINGTAI',
      lifecycle_stage: 'REGISTERED',
      cover_image: '/admin-api/infra/file/get?id=98011'
    }
  }
}

const camelCaseCoverSnapshot = {
  ...completeSnapshot,
  productId: 102,
  productCode: 'P-102',
  currentRevisionId: 4102,
  displayRevision: {
    revisionId: 4102,
    revisionNo: 6,
    status: 'PUBLISHED',
    nameCn: '球囊导管 Camel',
    nameEn: 'Balloon Catheter Camel',
    fields: {
      owner_company_id: 9001,
      product_owner_type: 'YINGTAI',
      lifecycle_stage: 'REGISTERED',
      coverImage: 'https://cdn.example.com/showroom/product-cover-camel.png'
    }
  }
}

const narrationTaskStatusSnapshot = {
  active: false,
  running: false,
  keyword: '',
  lifecycleStage: '',
  incompleteStatus: '',
  approvalStatus: '',
  matchedCount: 1,
  skippedCompletedCount: 0,
  generatedLanguageCount: 0,
  failedCount: 0,
  remainingCount: 0,
  startedAt: null,
  lastRunAt: null,
  completedAt: null,
  currentProduct: null,
  lastFailure: null,
  lastFailureAt: null
}

const narrationTaskActiveSnapshot = {
  ...narrationTaskStatusSnapshot,
  active: true,
  remainingCount: 1
}

const coverTaskSummarySnapshot = {
  startAllowed: false,
  active: true,
  running: false,
  keyword: '',
  lifecycleStage: '',
  incompleteStatus: '',
  approvalStatus: '',
  matchedCount: 1,
  publishedCount: 1,
  skippedUnpublishedCount: 0,
  skippedExistingCount: 0,
  succeededCount: 0,
  failedCount: 0,
  remainingPendingCount: 1,
  taskId: 2,
  taskStatus: 'WAITING',
  nextCheckAt: '2026-05-22 15:20',
  lastRunAt: null,
  completedAt: null,
  lastFailureMessage: '',
  currentProduct: null,
  failures: [],
  updatedAt: Date.now()
}

const coverTaskIdleSummarySnapshot = {
  ...coverTaskSummarySnapshot,
  startAllowed: true,
  active: false,
  running: false,
  remainingPendingCount: 0,
  taskStatus: 'COMPLETED',
  nextCheckAt: '',
  updatedAt: Date.now()
}

test('ProductListTable component exists for showroom product management', () => {
  const componentPath = path.join(
    root,
    'src/views/showroom-admin/components/ProductListTable.vue'
  )

  assert.ok(fs.existsSync(componentPath), 'ProductListTable.vue must exist')
})

test('ProductListTable renders required product list columns', () => {
  const source = readText('src/views/showroom-admin/components/ProductListTable.vue')

  for (const label of [
    '中文名称',
    '当前版本',
    '审批状态',
    'BU',
    '持证人',
    '获证状态',
    '在售国家',
    '封面',
    '状态',
    '操作'
  ]) {
    assert.match(source, new RegExp(label))
  }
  assert.doesNotMatch(source, /<el-table-column label="产品编码"/)
  assert.doesNotMatch(source, /<el-table-column label="资料状态"/)
  assert.doesNotMatch(source, /<el-table-column[\s\S]*label="指派对象"/)
  assert.match(source, /<el-table-column label="BU"/)
  assert.doesNotMatch(source, /<el-table-column label="英文名称"/)
  assert.match(source, /label="持证人"[\s\S]*prop="ownerTypeText"/)
  assert.doesNotMatch(source, /label="产品归属\/类型"[\s\S]*prop="ownerTypeText"/)
  assert.match(source, /<el-table-column label="获证状态" width="110">/)
  assert.match(source, /label="在售国家"[\s\S]*prop="targetMarket"/)
  assert.match(source, /<el-table-column label="封面"/)
  assert.doesNotMatch(source, /<el-table-column label="生命周期" width="120">/)
  assert.doesNotMatch(source, /<el-table-column label="所属公司ID"/)
  assert.match(source, /<el-table-column label="当前版本"/)
  assert.ok(source.indexOf('label="审批状态"') < source.indexOf('label="BU"'))
  assert.ok(source.indexOf('label="获证状态"') < source.indexOf('label="在售国家"'))
  assert.ok(source.indexOf('label="在售国家"') < source.indexOf('label="封面"'))
  assert.ok(source.indexOf('label="获证状态"') < source.indexOf('label="封面"'))
  assert.ok(source.indexOf('label="封面"') < source.indexOf('label="状态"'))
  assert.match(
    source,
    /label="操作"[\s\S]*:width="manageProducts \? 360 : rowEditableExists \? 240 : 180"/
  )

  assert.match(source, /<el-table\b/)
  assert.match(source, /defineProps/)
  assert.match(source, /products/)
  assert.match(source, /新增/)
  assert.match(source, /导入/)
  assert.match(source, /导入无产品图底表/)
  assert.match(source, /导出/)
  assert.match(source, /全部发布/)
  assert.match(source, /一键在售国家/)
  assert.doesNotMatch(source, /一键卖点/)
  assert.match(source, /一键讲解/)
  assert.match(source, /一键语音/)
  assert.match(source, /batchAudioAutoCheckLabel/)
  assert.match(source, /一键封面/)
  assert.match(source, /一键封面任务/)
  assert.match(source, /允许状态：/)
  assert.match(source, /当前执行产品：/)
  assert.match(source, /关闭/)
  assert.match(source, /查询/)
  assert.match(source, /重置/)
  assert.match(source, /未上传/)
  assert.match(source, /基础/)
  assert.match(source, /语音/)
  assert.match(source, /详细/)
  assert.match(source, /删除/)
  assert.match(source, /defineEmits/)
  assert.match(source, /create/)
  assert.match(source, /export-excel/)
  assert.match(source, /import-excel/)
  assert.match(source, /import-base-workbook/)
  assert.match(source, /batch-publish/)
  assert.match(source, /batch-generate-sales-countries/)
  assert.doesNotMatch(source, /batch-generate-selling-points/)
  assert.match(source, /batch-generate-narration-script/)
  assert.match(source, /batch-generate-audio/)
  assert.match(source, /batch-generate-cover/)
  assert.match(source, /open-audio-dialog/)
  assert.match(source, /detail/)
  assert.match(source, /search/)
  assert.match(source, /delete/)
  assert.doesNotMatch(source, /label="音频"/)
  assert.doesNotMatch(source, /label="音色"/)
})

test('ProductListTable toolbar only exposes product name search with adjacent query and reset actions', () => {
  const source = readText('src/views/showroom-admin/components/ProductListTable.vue')
  const toolbarBlock = sliceBetween(
    source,
    '<div class="showroom-product-list__toolbar">',
    '<div v-if="showNarrationTaskBanner"'
  )

  assert.match(toolbarBlock, /class="showroom-product-list__search-group"/)
  assert.match(toolbarBlock, /placeholder="搜索产品名称"/)
  assert.doesNotMatch(toolbarBlock, /placeholder="搜索产品编码 \/ 中文名 \/ 英文名"/)
  assert.doesNotMatch(toolbarBlock, /showroom-product-list__tenant/)
  assert.doesNotMatch(toolbarBlock, /showroom-product-list__filter/)
  assert.doesNotMatch(toolbarBlock, /placeholder="生命周期"/)
  assert.doesNotMatch(toolbarBlock, /placeholder="资料状态"/)
  assert.doesNotMatch(toolbarBlock, /placeholder="审批状态"/)
  assert.doesNotMatch(toolbarBlock, /model-value="瑛泰医疗"/)
  assert.ok(
    toolbarBlock.indexOf('placeholder="搜索产品名称"') < toolbarBlock.indexOf('@click="emitSearch"'),
    '查询按钮应紧跟搜索框之后'
  )
  assert.ok(
    toolbarBlock.indexOf('@click="emitSearch"') < toolbarBlock.indexOf('@click="resetFilters"'),
    '重置按钮应位于查询按钮之后'
  )
  assert.ok(
    toolbarBlock.indexOf('@click="resetFilters"') <
      toolbarBlock.indexOf('class="showroom-product-list__actions"'),
    '查询和重置按钮应放在页面级操作按钮左侧'
  )
  assert.match(
    source,
    /keyword: draftFilters\.keyword\.trim\(\),\s+lifecycleStage: '',\s+incompleteStatus: '',\s+approvalStatus: ''/
  )
})

test('ShowroomProductImportForm normalizes upload result arrays before rendering summary', () => {
  const source = readText('src/views/showroom-admin/product/ShowroomProductImportForm.vue')

  assert.match(source, /normalizeProductImportResult/)
  assert.match(source, /payloadData/)
  assert.match(source, /Array\.isArray\(source\.successProductCodes\)/)
  assert.match(source, /Array\.isArray\(source\.skippedProductCodes\)/)
  assert.match(source, /Array\.isArray\(source\.failures\)/)
  assert.match(
    source,
    /const result = normalizeProductImportResult\([\s\S]*ShowroomAdminApi\.importProductExcel\(formData\)[\s\S]*\)/
  )
  assert.match(source, /await ShowroomAdminApi\.importProductBaseWorkbook\(formData\)/)
})

test('showroom-admin index reads batch audio auto-check state on product page load', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /batchProductAudioAutoCheckState/)
  assert.match(source, /batchProductAudioAutoCheckLabel/)
  assert.match(source, /loadBatchProductAudioAutoCheckState/)
  assert.match(source, /getProductBatchGenerateNarrationAudioState/)
  assert.match(source, /await loadBatchProductAudioAutoCheckState\(\)/)
  assert.match(source, /:batch-audio-auto-check-enabled="batchProductAudioAutoCheckState\?\.enabled"/)
  assert.match(source, /:batch-audio-auto-check-label="batchProductAudioAutoCheckLabel"/)
})

test('ProductListTable renders current narration task product in the task banner', () => {
  const source = readText('src/views/showroom-admin/components/ProductListTable.vue')

  assert.match(source, /interface NarrationScriptTaskCurrentProduct/)
  assert.match(source, /currentProduct\?: NarrationScriptTaskCurrentProduct \| null/)
  assert.match(source, /narrationScriptTaskCurrentProductText/)
  assert.match(source, /当前执行产品/)
})

test('ProductListTable exposes incomplete content and avoids summary-row page body', () => {
  const source = readText('src/views/showroom-admin/components/ProductListTable.vue')
  const adminSource = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /资料未完善/)
  assert.doesNotMatch(source, /产品详情表/)
  assert.doesNotMatch(source, /共\s*\d+\s*个产品/)
  assert.doesNotMatch(adminSource, /产品详情表/)
  assert.doesNotMatch(adminSource, /共\s*\d+\s*个产品/)
})

test('ProductListTable declares explicit field normalization instead of mock data', () => {
  const source = readText('src/views/showroom-admin/components/ProductListTable.vue')

  assert.match(source, /normalizeProductRows/)
  assert.match(source, /resolveLifecycleStage/)
  assert.match(source, /resolveApprovalStatus/)
  assert.match(source, /resolveIncompleteStatus/)
  assert.doesNotMatch(source, /mock/i)
  assert.doesNotMatch(source, /demo/i)
  assert.doesNotMatch(source, /const products = ref/)
})

test('ProductListTable matches the real snapshot plus revision backend contract', () => {
  const source = readText('src/views/showroom-admin/components/ProductListTable.vue')

  for (const contractField of [
    'productId',
    'productCode',
    'currentRevisionId',
    'incomplete',
    'live',
    'revision',
    'displayRevision',
    'fields',
    'target_market',
    'owner_company_id',
    'product_owner_type',
    'lifecycle_stage',
    'cover_image',
    'status',
    'latestNarration',
    'audioUrl',
    'voice'
  ]) {
    assert.match(source, new RegExp(contractField))
  }

  assert.doesNotMatch(source, /ownerCompanyName/)
  assert.doesNotMatch(source, /更新时间/)
})

test('row-level product audio resolves the latest draft revision before currentRevisionId', () => {
  const resolveProductAudioSourceRevisionId = loadIndexFunction('resolveProductAudioSourceRevisionId')

  assert.equal(
    resolveProductAudioSourceRevisionId({
      productId: 1,
      currentRevisionId: 2666,
      revision: {
        revisionId: 2667,
        status: 'DRAFT'
      }
    }),
    2667
  )
  assert.equal(
    resolveProductAudioSourceRevisionId({
      productId: 1,
      currentRevisionId: 2666,
      displayRevision: {
        revisionId: 2667
      }
    }),
    2667
  )
  assert.equal(
    resolveProductAudioSourceRevisionId({
      productId: 1,
      currentRevisionId: 2666,
      displayRevisionId: 2667
    }),
    2667
  )
  assert.equal(
    resolveProductAudioSourceRevisionId({
      productId: 1,
      currentRevisionId: 2666
    }),
    2666
  )

  const source = readText('src/views/showroom-admin/index.vue')
  const handler = source.match(
    /const handleGenerateProductNarrationAudioFromRow = async[\s\S]*?(?=\nconst handleGenerateProductNarrationScript = async)/
  )?.[0] || ''
  assert.match(handler, /const sourceRevisionId = resolveProductAudioSourceRevisionId\(product\)/)
  assert.match(handler, /sourceRevisionId/)
  assert.match(handler, /formatShowroomStructuredError\(error, '产品音频生成'\)/)
})

test('product detail resolves the latest editable draft revision before displayRevision and currentRevisionId', () => {
  const resolveProductDetailRevisionId = loadIndexFunction('resolveProductDetailRevisionId')

  assert.equal(
    resolveProductDetailRevisionId({
      productId: 1,
      currentRevisionId: 2666,
      revision: {
        revisionId: 2667,
        status: 'DRAFT',
        editable: true
      },
      displayRevision: {
        revisionId: 2666,
        status: 'PUBLISHED',
        editable: false
      }
    }),
    2667
  )
  assert.equal(
    resolveProductDetailRevisionId({
      productId: 1,
      currentRevisionId: 2666,
      displayRevision: {
        revisionId: 2666,
        status: 'PUBLISHED'
      }
    }),
    2666
  )
  assert.equal(
    resolveProductDetailRevisionId({
      productId: 1,
      currentRevisionId: 2666
    }),
    2666
  )
})

test('ProductListTable exposes publicity-only batch media props and action loading bindings', () => {
  const source = readText('src/views/showroom-admin/components/ProductListTable.vue')

  assert.match(source, /canBatchGeneratePublishedMedia\?: boolean/)
  assert.match(source, /batchGeneratingSalesCountries\?: boolean/)
  assert.match(source, /batchGeneratingNarrationScript\?: boolean/)
  assert.match(source, /batchPublishing\?: boolean/)
  assert.match(source, /narrationScriptTaskActive\?: boolean/)
  assert.match(source, /narrationScriptTaskStatus\?:/)
  assert.match(source, /coverTaskSummary\?: CoverTaskSummary \| null/)
  assert.match(source, /startAllowed\?: boolean/)
  assert.match(source, /active\?: boolean/)
  assert.match(source, /running\?: boolean/)
  assert.match(source, /currentProduct\?: CoverTaskCurrentProduct \| null/)
  assert.match(source, /batchGeneratingAudio\?: boolean/)
  assert.match(source, /batchGeneratingCover\?: boolean/)
  assert.match(source, /batchAudioAutoCheckEnabled\?: boolean/)
  assert.match(source, /batchAudioAutoCheckLabel\?: string/)
  assert.match(source, /exportingExcel\?: boolean/)
  assert.match(source, /importingExcel\?: boolean/)
  assert.match(source, /<el-button\s+v-if="manageProducts"[\s\S]*:loading="Boolean\(importingExcel\)"[\s\S]*@click="emit\('import-excel'\)"/)
  assert.match(source, /导入无产品图底表/)
  assert.match(source, /@click="emit\('import-base-workbook'\)"/)
  assert.match(source, /<el-button\s+v-if="manageProducts"[\s\S]*:loading="Boolean\(exportingExcel\)"[\s\S]*@click="emit\('export-excel'\)"/)
  assert.match(source, /<el-button\s+v-if="canBatchGeneratePublishedMedia"[\s\S]*:loading="Boolean\(batchPublishing\)"[\s\S]*@click="emit\('batch-publish'\)"/)
  assert.match(source, /<el-button\s+v-if="canBatchGeneratePublishedMedia"[\s\S]*:loading="Boolean\(batchGeneratingSalesCountries\)"[\s\S]*@click="emit\('batch-generate-sales-countries'\)"/)
  assert.match(source, /<el-button\s+v-if="canBatchGeneratePublishedMedia"[\s\S]*:disabled="Boolean\(narrationScriptTaskActive\)"[\s\S]*:loading="Boolean\(batchGeneratingNarrationScript\)"[\s\S]*@click="handleBatchGenerateNarrationScriptClick"/)
  assert.match(source, /<el-button\s+v-if="canBatchGeneratePublishedMedia"[\s\S]*:loading="Boolean\(batchGeneratingAudio\)"[\s\S]*@click="emit\('batch-generate-audio'\)"/)
  assert.match(source, /coverTaskVisible/)
  assert.match(source, /coverTaskAllowStatusLabel/)
  assert.match(source, /coverTaskCurrentProductText/)
  assert.match(source, /dismissNarrationTaskBanner/)
  assert.match(source, /dismissCoverTaskBanner/)
  assert.match(source, /handleBatchGenerateNarrationScriptClick/)
  assert.match(source, /handleBatchGenerateCoverClick/)
  assert.doesNotMatch(source, /<el-tag[\s\S]*v-if="batchAudioAutoCheckLabel"/)
  assert.match(source, /<el-button\s+v-if="canBatchGeneratePublishedMedia"[\s\S]*:loading="Boolean\(batchGeneratingCover\)"[\s\S]*@click="handleBatchGenerateCoverClick"/)
})

test('task banners can be dismissed and reopen on next batch action click', () => {
  const setup = loadProductListSetup({
    narrationScriptTaskStatus: narrationTaskActiveSnapshot,
    coverTaskSummary: coverTaskSummarySnapshot
  })

  assert.equal(setup.showNarrationTaskBanner.value, true)
  assert.equal(setup.showCoverTaskBanner.value, true)

  setup.dismissNarrationTaskBanner()
  setup.dismissCoverTaskBanner()

  assert.equal(setup.showNarrationTaskBanner.value, false)
  assert.equal(setup.showCoverTaskBanner.value, false)

  setup.handleBatchGenerateNarrationScriptClick()
  setup.handleBatchGenerateCoverClick()

  assert.equal(setup.showNarrationTaskBanner.value, true)
  assert.equal(setup.showCoverTaskBanner.value, true)
})

test('task banners stay hidden when backend tasks are not running', () => {
  const setup = loadProductListSetup({
    narrationScriptTaskStatus: narrationTaskStatusSnapshot,
    coverTaskSummary: coverTaskIdleSummarySnapshot
  })

  assert.equal(setup.showNarrationTaskBanner.value, false)
  assert.equal(setup.showCoverTaskBanner.value, false)
})

test('normalizeProductRows keeps incomplete products renderable while exposing incomplete status', () => {
  const { normalizeProductRows } = loadProductListSetup()

  const rows = normalizeProductRows([incompleteSnapshot])

  assert.equal(rows.length, 1)
  assert.equal(rows[0].productId, '26')
  assert.equal(rows[0].nameCn, '一次性使用射频房间隔穿刺针')
  assert.equal(rows[0].nameEn, '')
  assert.equal(rows[0].revisionNo, 'V1')
  assert.equal(rows[0].ownerCompanyId, '')
  assert.equal(rows[0].ownerTypeText, '未完善')
  assert.equal(rows[0].lifecycleStage, 'R_AND_D')
  assert.equal(rows[0].lifecycleText, '研发中')
  assert.equal(rows[0].incompleteStatus, 'INCOMPLETE')
  assert.equal(rows[0].incompleteText, '资料未完善')
  assert.equal(rows[0].approvalStatus, 'DRAFT')
  assert.equal(rows[0].approvalText, '草稿')
  assert.equal(rows[0].coverImageUrl, '')
  assert.equal(rows[0].latestNarrationAudioUrl, '')
  assert.equal(rows[0].latestNarrationVoice, '')
})

test('normalizeProductRows preserves the complete product contract', () => {
  const { normalizeProductRows } = loadProductListSetup()

  const rows = normalizeProductRows([completeSnapshot])

  assert.equal(rows.length, 1)
  assert.equal(rows[0].productId, '88')
  assert.equal(rows[0].currentRevisionId, '2088')
  assert.equal(rows[0].nameCn, '球囊导管')
  assert.equal(rows[0].nameEn, 'Balloon Catheter')
  assert.equal(rows[0].revisionNo, 'V3')
  assert.equal(rows[0].ownerCompanyId, '9001')
  assert.equal(rows[0].ownerTypeText, '瑛泰医疗')
  assert.equal(rows[0].ownerType, 'YINGTAI')
  assert.equal(rows[0].lifecycleStage, 'REGISTERED')
  assert.equal(rows[0].lifecycleText, '已注册')
  assert.equal(rows[0].incompleteStatus, 'COMPLETE')
  assert.equal(rows[0].approvalStatus, 'APPROVED')
  assert.equal(rows[0].targetMarket, '')
  assert.equal(rows[0].coverImageUrl, '/admin-api/infra/file/get?id=98001')
  assert.equal(rows[0].latestNarrationAudioUrl, '/admin-api/infra/file/get?id=99001')
  assert.equal(rows[0].latestNarrationVoice, 'ruoxi')
})

test('normalizeProductRows exposes the chinese sales country from target_market', () => {
  const { normalizeProductRows } = loadProductListSetup()

  const rows = normalizeProductRows([
    {
      ...completeSnapshot,
      productId: 188,
      productCode: 'P-188',
      displayRevision: {
        ...completeSnapshot.displayRevision,
        fields: {
          ...completeSnapshot.displayRevision.fields,
          target_market: '中国、巴西'
        }
      }
    }
  ])

  assert.equal(rows.length, 1)
  assert.equal(rows[0].targetMarket, '中国、巴西')
})

test('normalizeProductRows keeps latest narration empty when newest zh narration has no audio', () => {
  const { normalizeProductRows } = loadProductListSetup()

  const rows = normalizeProductRows([noAudioNarrationSnapshot])

  assert.equal(rows.length, 1)
  assert.equal(rows[0].latestNarrationAudioUrl, '')
  assert.equal(rows[0].latestNarrationVoice, '')
})

test('normalizeProductRows exposes bilingual countries on sale and audio readiness in one status column', () => {
  const { normalizeProductRows } = loadProductListSetup()
  const rows = normalizeProductRows([
    {
      productId: 301,
      productCode: 'P-301',
      currentRevisionId: 5301,
      live: false,
      editable: true,
      incomplete: false,
      revision: {
        revisionId: 5301,
        revisionNo: 6,
        status: 'DRAFT',
        nameCn: '状态列探针',
        nameEn: 'Status Probe',
        fields: {
          owner_company_id: 9001,
          product_owner_type: 'YINGTAI',
          lifecycle_stage: 'REGISTERED',
          target_market: '中国、欧盟',
          target_market_en: 'China, European Union',
          core_selling_points: '',
          core_selling_points_en: ''
        }
      },
      displayRevision: {
        revisionId: 5301,
        revisionNo: 6,
        status: 'DRAFT',
        nameCn: '状态列探针',
        nameEn: 'Status Probe',
        fields: {
          owner_company_id: 9001,
          product_owner_type: 'YINGTAI',
          lifecycle_stage: 'REGISTERED',
          target_market: '中国、欧盟',
          target_market_en: 'China, European Union',
          core_selling_points: '',
          core_selling_points_en: ''
        },
        narrations: [
          {
            narrationVersionId: 9301,
            language: 'ZH',
            audienceType: 'PUBLIC',
            status: 'PUBLISHED',
            live: true,
            audioReady: true
          },
          {
            narrationVersionId: 9302,
            language: 'EN',
            audienceType: 'PUBLIC',
            status: 'DRAFT',
            live: false,
            audioReady: false
          }
        ]
      }
    }
  ])

  assert.equal(JSON.stringify(rows[0].contentStatusItems.map((item) => item.ready)), JSON.stringify([true, true, true, false]))
  assert.equal(
    JSON.stringify(rows[0].contentStatusItems.map((item) => item.shortLabel)),
    JSON.stringify(['在售国家', '在售国家(英)', '中音频', '英音频'])
  )
})

test('normalizeProductRows keeps displayRevision text fields while preferring latest saved cover image', () => {
  const { normalizeProductRows } = loadProductListSetup()

  const rows = normalizeProductRows([latestDraftWithPublishedDisplaySnapshot])

  assert.equal(rows.length, 1)
  assert.equal(rows[0].productId, '101')
  assert.equal(rows[0].currentRevisionId, '4101')
  assert.equal(rows[0].nameCn, '球囊导管 Live')
  assert.equal(rows[0].nameEn, 'Balloon Catheter Live')
  assert.equal(rows[0].revisionNo, 'V4')
  assert.equal(rows[0].lifecycleStage, 'REGISTERED')
  assert.equal(rows[0].approvalStatus, 'PENDING_GAOXIN_APPROVAL')
  assert.equal(rows[0].approvalText, '审核中')
  assert.equal(rows[0].coverImageUrl, '/admin-api/infra/file/get?id=98099')
  assert.equal(rows[0].activeAssignmentAssigneeLabel, '用户 #700')
})

test('normalizeProductRows allows audio rows whose historical voice is empty', () => {
  const { normalizeProductRows } = loadProductListSetup()

  const rows = normalizeProductRows([audioWithoutVoiceSnapshot])

  assert.equal(rows.length, 1)
  assert.equal(rows[0].latestNarrationAudioUrl, '/admin-api/infra/file/get?id=99002')
  assert.equal(rows[0].latestNarrationVoice, '')
})

test('normalizeProductRows accepts camelCase coverImage alias from display fields', () => {
  const { normalizeProductRows } = loadProductListSetup()

  const rows = normalizeProductRows([camelCaseCoverSnapshot])

  assert.equal(rows.length, 1)
  assert.equal(rows[0].productId, '102')
  assert.equal(rows[0].coverImageUrl, 'https://cdn.example.com/showroom/product-cover-camel.png')
})
