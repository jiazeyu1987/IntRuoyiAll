import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import vm from 'node:vm'

import { parse, compileScript } from '@vue/compiler-sfc'
import ts from 'typescript'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const loadProductListSetup = () => {
  const filePath = path.join(root, 'src/views/showroom-admin/components/ProductListTable.vue')
  const source = fs.readFileSync(filePath, 'utf8')
  const { descriptor } = parse(source, { filename: filePath })
  const script = compileScript(descriptor, { id: 'product-list-table-narration-test' })
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
  return component.setup({ products: [], loading: false, filters: {}, pageNo: 1, pageSize: 20, pageTotal: 0 }, { expose() {}, emit() {} })
}

const missingNarrationSnapshot = {
  productId: 201,
  productCode: 'P-201',
  currentRevisionId: 5201,
  live: false,
  editable: true,
  incomplete: false,
  revision: {
    revisionId: 5301,
    revisionNo: 2,
    status: 'DRAFT',
    nameCn: '无讲解稿产品',
    nameEn: 'Narration Missing Product',
    fields: {
      owner_company_id: 9001,
      product_owner_type: 'YINGTAI',
      lifecycle_stage: 'REGISTERED'
    }
  },
  displayRevision: {
    revisionId: 5301,
    revisionNo: 2,
    status: 'DRAFT',
    nameCn: '无讲解稿产品',
    nameEn: 'Narration Missing Product',
    fields: {
      owner_company_id: 9001,
      product_owner_type: 'YINGTAI',
      lifecycle_stage: 'REGISTERED'
    }
  }
}

const noAudioNarrationSnapshot = {
  productId: 202,
  productCode: 'P-202',
  currentRevisionId: 5202,
  live: false,
  editable: true,
  incomplete: false,
  revision: {
    revisionId: 5302,
    revisionNo: 3,
    status: 'DRAFT',
    nameCn: '有讲解稿未生成音频',
    nameEn: 'Narration Without Audio',
    fields: {
      owner_company_id: 9001,
      product_owner_type: 'YINGTAI',
      lifecycle_stage: 'REGISTERED'
    }
  },
  displayRevision: {
    revisionId: 5302,
    revisionNo: 3,
    status: 'DRAFT',
    nameCn: '有讲解稿未生成音频',
    nameEn: 'Narration Without Audio',
    fields: {
      owner_company_id: 9001,
      product_owner_type: 'YINGTAI',
      lifecycle_stage: 'REGISTERED'
    }
  },
  latestNarration: {
    narrationVersionId: 7202,
    language: 'ZH',
    audienceType: 'PUBLIC',
    status: 'DRAFT',
    live: false,
    audioReady: false,
    audioUrl: '',
    voice: ''
  }
}

test('ProductListTable exposes the row-level voice action next to assignment', () => {
  const source = readText('src/views/showroom-admin/components/ProductListTable.vue')

  assert.match(
    source,
    /emit\('assign', row\.raw\)[\s\S]*?>\s*指派\s*<\/el-button>[\s\S]*emit\('open-audio-dialog', row\.raw\)[\s\S]*?>\s*语音\s*<\/el-button>/
  )
})

test('normalizeProductRows exposes narration presence separately from audio readiness', () => {
  const { normalizeProductRows } = loadProductListSetup()

  const [missingNarrationRow, noAudioNarrationRow] = normalizeProductRows([
    missingNarrationSnapshot,
    noAudioNarrationSnapshot
  ])

  assert.equal(missingNarrationRow.hasNarrationScript, false)
  assert.equal(noAudioNarrationRow.hasNarrationScript, true)
  assert.equal(noAudioNarrationRow.latestNarrationAudioUrl, '')
})
