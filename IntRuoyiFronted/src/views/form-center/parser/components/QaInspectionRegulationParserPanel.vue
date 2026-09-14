<template>
  <el-empty v-if="!qaPreviewJson" description="请选择 QA 检验规程 DOCX 文件" />

  <div v-else class="qa-parser-workbench">
    <section class="qa-parser-json-editor">
      <header class="qa-parser-panel-head">
        <div>
          <div class="qa-parser-panel-title">QA检验规程 JSON编辑</div>
          <div class="qa-parser-panel-subtitle">{{ qaLastDownloadName }}</div>
        </div>
        <div class="qa-parser-panel-actions">
          <el-button type="primary" @click="applyQaEditedJson">
            <Icon icon="ep:check" />
            应用
          </el-button>
          <el-button @click="downloadQaCurrentJson">
            <Icon icon="ep:download" />
            下载当前JSON
          </el-button>
        </div>
      </header>

      <div class="qa-parser-json-search">
        <el-input
          v-model="qaJsonSearchKeyword"
          clearable
          placeholder="查找关键词"
          @clear="resetQaJsonSearchPosition"
          @input="resetQaJsonSearchPosition"
          @keyup.enter="locateNextQaJsonKeyword"
        >
          <template #prefix>
            <Icon icon="ep:search" />
          </template>
        </el-input>
        <el-button @click="locateNextQaJsonKeyword">
          <Icon icon="ep:search" />
          查找下一个
        </el-button>
        <span class="qa-parser-json-search__count" aria-live="polite">
          {{ qaJsonSearchMatchLabel }}
        </span>
      </div>

      <el-input
        ref="qaJsonEditorInputRef"
        v-model="qaEditableJson"
        class="qa-parser-json-input"
        type="textarea"
        :autosize="{ minRows: 28, maxRows: 44 }"
        spellcheck="false"
        @input="resetQaJsonSearchPosition"
      />
    </section>

    <section class="qa-parser-pqc-preview">
      <header class="qa-parser-panel-head">
        <div>
          <div class="qa-parser-panel-title">一线PQC预览</div>
          <div class="qa-parser-panel-subtitle">
            {{ qaPreviewJson.regulationName }}（{{ qaPreviewJson.regulationCode }}）
            · {{ qaPreviewJson.versionNo }} · {{ qaPreviewJson.effectiveDate }}
          </div>
        </div>
      </header>

      <div class="qa-parser-pqc-shell">
        <header class="qa-parser-pqc-process-nav">
          <button
            type="button"
            class="qa-parser-nav-button"
            :disabled="!canGoPreviousQaProcess"
            aria-label="上一道工序"
            @click="goPreviousQaProcess"
          >
            <Icon icon="ep:arrow-left-bold" />
          </button>
          <button type="button" class="qa-parser-process-card" @click="openQaProcessSelector">
            <span>工序</span>
            <strong>{{ qaCurrentProcessTitle }}</strong>
          </button>
          <button
            type="button"
            class="qa-parser-nav-button"
            :disabled="!canGoNextQaProcess"
            aria-label="下一道工序"
            @click="goNextQaProcess"
          >
            <Icon icon="ep:arrow-right-bold" />
          </button>
        </header>

        <main v-if="qaCurrentProcess && qaCurrentItem" class="qa-parser-pqc-body">
          <section class="qa-parser-inspection-panel">
            <div class="qa-parser-item-heading">
              <span>{{ qaCurrentItem.itemCode }}</span>
              <strong>{{ qaCurrentItem.itemName }}</strong>
            </div>

            <div class="qa-parser-fact-strip">
              <button type="button" @click="openQaFactDialog('equipment')">
                <span>检验设备</span>
                <strong>{{ qaCurrentItem.inspectionTool }}</strong>
              </button>
              <button type="button" class="is-primary" @click="openQaFactDialog('standard')">
                <span>接受标准</span>
                <strong>{{ compactText(qaCurrentItem.standardText) }}</strong>
              </button>
              <button type="button" @click="openQaFactDialog('method')">
                <span>检验方法</span>
                <strong>{{ compactText(qaCurrentItem.inspectionMethod) }}</strong>
              </button>
            </div>

            <div class="qa-parser-result-actions">
              <button
                type="button"
                class="is-pass"
                :class="{ active: qaCurrentItemResult === 'PASS' }"
                @click="setQaCurrentItemResult('PASS')"
              >
                全部合格
              </button>
              <button
                type="button"
                class="is-fail"
                :class="{ active: qaCurrentItemResult === 'FAIL' }"
                @click="setQaCurrentItemResult('FAIL')"
              >
                全部不良
              </button>
              <button
                type="button"
                class="is-piece"
                :class="{ active: qaCurrentItemResult === 'PIECE' }"
                @click="setQaCurrentItemResult('PIECE')"
              >
                逐件选择
              </button>
            </div>

            <nav class="qa-parser-item-tabs" aria-label="PQC检验项目切换">
              <button
                v-for="(item, itemIndex) in qaCurrentProcess.items"
                :key="item.itemCode"
                type="button"
                :class="{ active: itemIndex === qaCurrentItemIndex }"
                :aria-pressed="itemIndex === qaCurrentItemIndex"
                @click="selectQaItem(itemIndex)"
              >
                {{ item.itemName }}
              </button>
            </nav>
          </section>

          <section class="qa-parser-fill-panel">
            <div class="qa-parser-inspection-types">
              <button
                v-for="inspectionType in qaAvailableInspectionTypes"
                :key="inspectionType"
                type="button"
                :class="{ active: inspectionType === qaSelectedInspectionType }"
                @click="selectQaInspectionType(inspectionType)"
              >
                {{ formatQaInspectionType(inspectionType) }}
              </button>
            </div>

            <div class="qa-parser-number-field">
              <span>检验数量</span>
              <el-input-number v-model="qaInspectionQuantity" :min="1" :precision="0" />
              <b>件</b>
            </div>

            <dl class="qa-parser-sampling-details">
              <div>
                <dt>抽样方案</dt>
                <dd>{{ qaCurrentItem.samplingPlanText }}</dd>
              </div>
              <div>
                <dt>首检数量</dt>
                <dd>{{ formatOptionalNumber(qaCurrentItem.firstInspectionQuantity, '件') }}</dd>
              </div>
              <div>
                <dt>巡检比例</dt>
                <dd>{{ formatPatrolRatio(qaCurrentItem.patrolInspectionRatio) }}</dd>
              </div>
              <div>
                <dt>结果类型</dt>
                <dd>{{ qaCurrentItem.resultType }}</dd>
              </div>
            </dl>
          </section>
        </main>

        <div v-else class="qa-parser-pqc-empty">当前工序未包含检验项目</div>

        <footer class="qa-parser-pqc-footer">
          <button type="button" disabled>重填</button>
          <button type="button" class="is-primary" disabled>正式提交</button>
        </footer>
      </div>
    </section>
  </div>

  <el-dialog
    v-if="qaPreviewJson"
    v-model="qaProcessSelectorVisible"
    title="选择工序"
    width="600px"
    append-to-body
  >
    <div class="qa-parser-process-selector">
      <p>共 {{ qaPreviewJson.processes.length }} 个工序</p>
      <button
        v-for="(process, processIndex) in qaPreviewJson.processes"
        :key="process.processCode"
        type="button"
        :class="{ active: processIndex === qaCurrentProcessIndex }"
        @click="selectQaProcess(processIndex)"
      >
        <span>{{ process.sort }}</span>
        <strong>{{ process.processName }}</strong>
        <em>{{ process.processCode }}</em>
      </button>
    </div>
  </el-dialog>

  <el-dialog v-model="qaFactDialogVisible" :title="qaFactDialogTitle" width="680px" append-to-body>
    <div v-if="qaCurrentItem" class="qa-parser-fact-dialog">
      <template v-if="qaFactDialogType === 'equipment'">
        <strong>检验器具及设备</strong>
        <p>{{ qaCurrentItem.inspectionTool }}</p>
      </template>
      <template v-else-if="qaFactDialogType === 'standard'">
        <strong>接受标准</strong>
        <p>{{ qaCurrentItem.standardText }}</p>
      </template>
      <template v-else>
        <strong>检验方法</strong>
        <p>{{ qaCurrentItem.inspectionMethod }}</p>
        <strong>抽样方案</strong>
        <p>{{ qaCurrentItem.samplingPlanText }}</p>
      </template>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import download from '@/utils/download'
import {
  QcTemplateApi,
  type QaInspectionRegulationParseVO,
  type QaInspectionRegulationParsedItemVO
} from '@/api/mes/qc/template'

defineOptions({ name: 'QaInspectionRegulationParserPanel' })

type QaInspectionType = 'FIRST' | 'PATROL'
type QaPreviewResult = 'PASS' | 'FAIL' | 'PIECE'
type QaFactDialogType = 'equipment' | 'standard' | 'method'

const message = useMessage()
const qaJsonEditorInputRef = ref<{ textarea?: HTMLTextAreaElement }>()
const qaEditableJson = ref('')
const qaPreviewJson = ref<QaInspectionRegulationParseVO>()
const qaLastDownloadName = ref('')
const qaJsonSearchKeyword = ref('')
const qaJsonSearchMatchIndex = ref(-1)
const qaJsonSearchMatchTotal = ref(0)
const qaJsonSearchLastKeyword = ref('')
const qaJsonSearchLastContent = ref('')
const qaCurrentProcessIndex = ref(0)
const qaCurrentItemIndex = ref(0)
const qaProcessSelectorVisible = ref(false)
const qaSelectedInspectionType = ref<QaInspectionType>('FIRST')
const qaInspectionQuantity = ref(1)
const qaItemResults = reactive<Record<string, QaPreviewResult | undefined>>({})
const qaFactDialogVisible = ref(false)
const qaFactDialogType = ref<QaFactDialogType>('standard')

const qaCurrentProcess = computed(() =>
  qaPreviewJson.value?.processes[qaCurrentProcessIndex.value]
)

const qaCurrentItem = computed(() => qaCurrentProcess.value?.items[qaCurrentItemIndex.value])

const qaCurrentProcessTitle = computed(() => {
  if (!qaCurrentProcess.value) {
    return '-'
  }
  return `${qaCurrentProcessIndex.value + 1}. ${qaCurrentProcess.value.processName}`
})

const canGoPreviousQaProcess = computed(() => qaCurrentProcessIndex.value > 0)

const canGoNextQaProcess = computed(() => {
  const processCount = qaPreviewJson.value?.processes.length || 0
  return qaCurrentProcessIndex.value < processCount - 1
})

const qaAvailableInspectionTypes = computed<QaInspectionType[]>(() =>
  qaCurrentItem.value?.applicableInspectionTypes || []
)

const qaCurrentItemResult = computed(() =>
  qaCurrentItem.value ? qaItemResults[qaCurrentItem.value.itemCode] : undefined
)

const qaJsonSearchMatchLabel = computed(() => {
  if (!qaJsonSearchKeyword.value.trim()) {
    return ''
  }
  return qaJsonSearchMatchTotal.value
    ? `${qaJsonSearchMatchIndex.value + 1} / ${qaJsonSearchMatchTotal.value}`
    : '0 / 0'
})

const qaFactDialogTitle = computed(() => {
  if (qaFactDialogType.value === 'equipment') {
    return '检验设备'
  }
  if (qaFactDialogType.value === 'method') {
    return '检验方法'
  }
  return '接受标准'
})

const parseWordFile = async (file: File): Promise<boolean> => {
  if (!file.name.toLowerCase().endsWith('.docx')) {
    message.error('QA 检验规程仅支持 DOCX 文件')
    return false
  }
  try {
    const parsed = await QcTemplateApi.parseQaInspectionRegulationJson(file)
    const mapping = validateQaRecognitionJson(parsed)
    qaEditableJson.value = stringifyQaRecognitionJson(mapping)
    qaPreviewJson.value = mapping
    qaLastDownloadName.value = buildQaJsonDownloadName(file.name)
    resetQaJsonSearchPosition()
    initializeQaPreviewState(mapping)
    download.json(
      new Blob([qaEditableJson.value], { type: 'application/json;charset=utf-8' }),
      qaLastDownloadName.value
    )
    message.success('QA 检验规程解析完成，已下载 JSON 文件')
    return true
  } catch (error) {
    message.error(resolveQaParseErrorMessage(error, 'QA 检验规程解析失败，请检查 DOCX 内容'))
    return false
  }
}

const applyQaEditedJson = () => {
  try {
    const parsed = JSON.parse(qaEditableJson.value) as unknown
    const mapping = validateQaRecognitionJson(parsed)
    qaPreviewJson.value = mapping
    resetQaJsonSearchPosition()
    initializeQaPreviewState(mapping)
    message.success('已应用 QA JSON 到右侧一线PQC预览')
  } catch (error) {
    message.error(resolveQaParseErrorMessage(error, 'QA JSON 应用失败'))
  }
}

const downloadQaCurrentJson = () => {
  if (!qaPreviewJson.value) {
    message.error('没有可下载的 QA JSON')
    return
  }
  download.json(
    new Blob([stringifyQaRecognitionJson(qaPreviewJson.value)], {
      type: 'application/json;charset=utf-8'
    }),
    qaLastDownloadName.value || 'QA检验规程.json'
  )
}

const initializeQaPreviewState = (mapping: QaInspectionRegulationParseVO) => {
  qaCurrentProcessIndex.value = 0
  qaCurrentItemIndex.value = 0
  Object.keys(qaItemResults).forEach((key) => delete qaItemResults[key])
  initializeQaItemControls(mapping.processes[0]?.items[0])
}

const initializeQaItemControls = (item?: QaInspectionRegulationParsedItemVO) => {
  const availableTypes = item?.applicableInspectionTypes || []
  qaSelectedInspectionType.value = availableTypes[0] || 'FIRST'
  qaInspectionQuantity.value = Math.max(1, item?.firstInspectionQuantity || 1)
}

const goPreviousQaProcess = () => {
  if (!canGoPreviousQaProcess.value) return
  qaCurrentProcessIndex.value -= 1
  qaCurrentItemIndex.value = 0
  initializeQaItemControls(qaCurrentProcess.value?.items[0])
}

const goNextQaProcess = () => {
  if (!canGoNextQaProcess.value) return
  qaCurrentProcessIndex.value += 1
  qaCurrentItemIndex.value = 0
  initializeQaItemControls(qaCurrentProcess.value?.items[0])
}

const openQaProcessSelector = () => {
  qaProcessSelectorVisible.value = true
}

const selectQaProcess = (processIndex: number) => {
  qaCurrentProcessIndex.value = processIndex
  qaCurrentItemIndex.value = 0
  qaProcessSelectorVisible.value = false
  initializeQaItemControls(qaCurrentProcess.value?.items[0])
}

const selectQaItem = (itemIndex: number) => {
  qaCurrentItemIndex.value = itemIndex
  initializeQaItemControls(qaCurrentProcess.value?.items[itemIndex])
}

const selectQaInspectionType = (inspectionType: QaInspectionType) => {
  qaSelectedInspectionType.value = inspectionType
  qaInspectionQuantity.value =
    inspectionType === 'FIRST' ? Math.max(1, qaCurrentItem.value?.firstInspectionQuantity || 1) : 1
}

const setQaCurrentItemResult = (result: QaPreviewResult) => {
  if (qaCurrentItem.value) {
    qaItemResults[qaCurrentItem.value.itemCode] = result
  }
}

const openQaFactDialog = (type: QaFactDialogType) => {
  qaFactDialogType.value = type
  qaFactDialogVisible.value = true
}

const resetQaJsonSearchPosition = () => {
  qaJsonSearchMatchIndex.value = -1
  qaJsonSearchMatchTotal.value = 0
  qaJsonSearchLastKeyword.value = ''
  qaJsonSearchLastContent.value = ''
}

const findQaJsonKeywordMatches = () => {
  const keyword = qaJsonSearchKeyword.value.trim()
  const matches: Array<{ start: number; end: number }> = []
  if (!keyword) return matches
  let startIndex = 0
  while (startIndex <= qaEditableJson.value.length) {
    const start = qaEditableJson.value.indexOf(keyword, startIndex)
    if (start < 0) break
    matches.push({ start, end: start + keyword.length })
    startIndex = start + keyword.length
  }
  return matches
}

const locateNextQaJsonKeyword = async () => {
  const keyword = qaJsonSearchKeyword.value.trim()
  if (!keyword) {
    resetQaJsonSearchPosition()
    message.warning('请输入查找关键词')
    return
  }
  const matches = findQaJsonKeywordMatches()
  qaJsonSearchMatchTotal.value = matches.length
  if (!matches.length) {
    qaJsonSearchMatchIndex.value = -1
    message.warning(`未找到关键词：${keyword}`)
    return
  }
  if (
    qaJsonSearchLastKeyword.value !== keyword ||
    qaJsonSearchLastContent.value !== qaEditableJson.value
  ) {
    qaJsonSearchMatchIndex.value = -1
    qaJsonSearchLastKeyword.value = keyword
    qaJsonSearchLastContent.value = qaEditableJson.value
  }
  qaJsonSearchMatchIndex.value = (qaJsonSearchMatchIndex.value + 1) % matches.length
  const match = matches[qaJsonSearchMatchIndex.value]
  await nextTick()
  const textarea = qaJsonEditorInputRef.value?.textarea
  if (!textarea) {
    message.error('QA JSON编辑器未就绪')
    return
  }
  textarea.focus()
  textarea.setSelectionRange(match.start, match.end)
  const lineHeight = Number.parseFloat(window.getComputedStyle(textarea).lineHeight)
  if (!Number.isFinite(lineHeight) || lineHeight <= 0) {
    message.error('QA JSON编辑器行高无效')
    return
  }
  const lineIndex = qaEditableJson.value.slice(0, match.start).split('\n').length - 1
  textarea.scrollTop = Math.max(lineIndex * lineHeight - textarea.clientHeight / 2, 0)
}

const validateQaRecognitionJson = (value: unknown): QaInspectionRegulationParseVO => {
  if (!isRecordValue(value)) {
    throw new Error('QA 检验规程 JSON 顶层必须是对象')
  }
  if (value.schemaVersion !== 1) {
    throw new Error('QA 检验规程 JSON schemaVersion 必须为 1')
  }
  for (const field of [
    'sourceFileName',
    'regulationCode',
    'regulationName',
    'versionNo',
    'effectiveDate'
  ]) {
    requireQaText(value[field], `QA 检验规程 JSON ${field}`)
  }
  if (!Array.isArray(value.processes) || !value.processes.length) {
    throw new Error('QA 检验规程 JSON processes 必须是非空数组')
  }
  value.processes.forEach((process, processIndex) => {
    if (!isRecordValue(process)) {
      throw new Error(`QA 检验规程 JSON processes[${processIndex}] 必须是对象`)
    }
    requireQaText(process.processCode, `processes[${processIndex}].processCode`)
    requireQaText(process.processName, `processes[${processIndex}].processName`)
    requireQaPositiveInteger(process.sort, `processes[${processIndex}].sort`)
    if (!Array.isArray(process.items) || !process.items.length) {
      throw new Error(`QA 检验规程 JSON processes[${processIndex}].items 必须是非空数组`)
    }
    process.items.forEach((item, itemIndex) => validateQaItem(item, processIndex, itemIndex))
  })
  return value as unknown as QaInspectionRegulationParseVO
}

const validateQaItem = (value: unknown, processIndex: number, itemIndex: number) => {
  const path = `processes[${processIndex}].items[${itemIndex}]`
  if (!isRecordValue(value)) {
    throw new Error(`QA 检验规程 JSON ${path} 必须是对象`)
  }
  for (const field of [
    'itemCode',
    'itemName',
    'inspectionMethod',
    'inspectionTool',
    'samplingPlanText',
    'standardText',
    'resultType'
  ]) {
    requireQaText(value[field], `${path}.${field}`)
  }
  requireQaPositiveInteger(value.itemSort, `${path}.itemSort`)
  if (
    !Array.isArray(value.applicableInspectionTypes) ||
    !value.applicableInspectionTypes.length ||
    value.applicableInspectionTypes.some((type) => type !== 'FIRST' && type !== 'PATROL')
  ) {
    throw new Error(`QA 检验规程 JSON ${path}.applicableInspectionTypes 无效`)
  }
  validateQaOptionalNumber(value.firstInspectionQuantity, `${path}.firstInspectionQuantity`)
  validateQaOptionalNumber(value.patrolInspectionRatio, `${path}.patrolInspectionRatio`)
}

const requireQaText = (value: unknown, path: string) => {
  if (typeof value !== 'string' || !value.trim()) {
    throw new Error(`QA 检验规程 JSON ${path} 必须是非空文本`)
  }
}

const requireQaPositiveInteger = (value: unknown, path: string) => {
  if (typeof value !== 'number' || !Number.isInteger(value) || value <= 0) {
    throw new Error(`QA 检验规程 JSON ${path} 必须是正整数`)
  }
}

const validateQaOptionalNumber = (value: unknown, path: string) => {
  if (value !== undefined && value !== null && (typeof value !== 'number' || !Number.isFinite(value))) {
    throw new Error(`QA 检验规程 JSON ${path} 必须是数字或空值`)
  }
}

const isRecordValue = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value)

const stringifyQaRecognitionJson = (mapping: QaInspectionRegulationParseVO) =>
  JSON.stringify(mapping, null, 2)

const buildQaJsonDownloadName = (fileName: string) =>
  `${fileName.replace(/\.docx$/i, '')}.json`

const formatQaInspectionType = (inspectionType: QaInspectionType) =>
  inspectionType === 'FIRST' ? '首检' : '巡检'

const formatOptionalNumber = (value: number | undefined, unit: string) =>
  value === undefined || value === null ? '不适用' : `${value}${unit}`

const formatPatrolRatio = (value: number | undefined) =>
  value === undefined || value === null ? '不适用' : `AQL ${value}`

const compactText = (value: string) => (value.length > 32 ? `${value.slice(0, 32)}...` : value)

const resolveQaParseErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as { response?: { data?: { msg?: string } } })?.response?.data?.msg
  if (responseMessage?.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  if (typeof error === 'string' && error.trim()) return error
  if (isRecordValue(error) && typeof error.msg === 'string' && error.msg.trim()) return error.msg
  return fallback
}

defineExpose({ parseWordFile })
</script>

<style scoped lang="scss">
.qa-parser-workbench {
  display: grid;
  grid-template-columns: minmax(360px, 0.82fr) minmax(600px, 1.18fr);
  gap: 12px;
  align-items: start;
}

.qa-parser-json-editor,
.qa-parser-pqc-preview {
  min-width: 0;
  border: 1px solid #d6dfda;
  border-radius: 6px;
  background: #fff;
  padding: 14px;
}

.qa-parser-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.qa-parser-panel-title {
  color: #17251f;
  font-size: 17px;
  font-weight: 700;
}

.qa-parser-panel-subtitle {
  margin-top: 4px;
  color: #60736a;
  font-size: 12px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.qa-parser-panel-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.qa-parser-json-search {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) auto 56px;
  gap: 8px;
  margin-bottom: 10px;
}

.qa-parser-json-search__count {
  display: grid;
  place-items: center;
  min-height: 32px;
  color: #52645c;
  font-variant-numeric: tabular-nums;
}

.qa-parser-json-input :deep(textarea) {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.65;
  resize: vertical;
}

.qa-parser-pqc-shell {
  display: grid;
  grid-template-rows: auto minmax(520px, auto) 70px;
  min-height: 680px;
  overflow: hidden;
  border: 2px solid #c8d6cf;
  border-radius: 6px;
  background: #f4f8f6;
}

.qa-parser-pqc-process-nav {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr) 64px;
  gap: 10px;
  padding: 12px;
  border-bottom: 1px solid #c8d6cf;
  background: #fff;
}

.qa-parser-nav-button,
.qa-parser-process-card {
  min-height: 68px;
  border: 2px solid #cbd8d1;
  border-radius: 6px;
  background: #fff;
  color: #17251f;
}

.qa-parser-nav-button {
  display: grid;
  place-items: center;
  font-size: 24px;
}

.qa-parser-nav-button:disabled {
  color: #adb8b2;
  cursor: not-allowed;
}

.qa-parser-process-card {
  display: grid;
  place-items: center;
  gap: 2px;
  padding: 6px 12px;
}

.qa-parser-process-card span {
  color: #587066;
  font-size: 13px;
}

.qa-parser-process-card strong {
  max-width: 100%;
  overflow: hidden;
  font-size: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qa-parser-pqc-body {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(240px, 0.65fr);
  gap: 12px;
  min-height: 0;
  padding: 12px;
}

.qa-parser-inspection-panel,
.qa-parser-fill-panel {
  min-width: 0;
  border: 2px solid #cbd8d1;
  border-radius: 6px;
  background: #fff;
}

.qa-parser-inspection-panel {
  display: grid;
  grid-template-rows: auto auto minmax(150px, 1fr) auto;
  gap: 14px;
  padding: 14px;
}

.qa-parser-item-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #dde5e1;
}

.qa-parser-item-heading span {
  color: #5a7066;
  font-size: 12px;
}

.qa-parser-item-heading strong {
  color: #12251d;
  font-size: 22px;
  text-align: right;
}

.qa-parser-fact-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.qa-parser-fact-strip button {
  display: grid;
  gap: 5px;
  min-width: 0;
  min-height: 92px;
  padding: 11px;
  border: 1px solid #bfd0c7;
  border-radius: 6px;
  background: #f9fbfa;
  color: #20372d;
  text-align: left;
}

.qa-parser-fact-strip button.is-primary {
  border-color: #168a68;
  background: #eff9f5;
}

.qa-parser-fact-strip span {
  color: #597067;
  font-size: 12px;
}

.qa-parser-fact-strip strong {
  display: -webkit-box;
  overflow: hidden;
  font-size: 14px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.qa-parser-result-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  align-content: center;
}

.qa-parser-result-actions button {
  min-height: 82px;
  border: 2px solid #c8d6cf;
  border-radius: 6px;
  background: #fff;
  color: #1d3329;
  font-size: 18px;
  font-weight: 700;
}

.qa-parser-result-actions .is-pass.active {
  border-color: #158c68;
  background: #158c68;
  color: #fff;
}

.qa-parser-result-actions .is-fail.active {
  border-color: #c74848;
  background: #c74848;
  color: #fff;
}

.qa-parser-result-actions .is-piece.active {
  border-color: #3f6252;
  background: #3f6252;
  color: #fff;
}

.qa-parser-item-tabs {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 7px;
  max-height: 124px;
  overflow-y: auto;
  padding-top: 12px;
  border-top: 1px solid #dde5e1;
}

.qa-parser-item-tabs button {
  min-height: 46px;
  padding: 7px;
  border: 1px solid #c8d6cf;
  border-radius: 5px;
  background: #fff;
  color: #20372d;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.qa-parser-item-tabs button.active {
  border-color: #168a68;
  background: #168a68;
  color: #fff;
}

.qa-parser-fill-panel {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 16px;
  padding: 14px;
}

.qa-parser-inspection-types {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(90px, 1fr));
  gap: 8px;
}

.qa-parser-inspection-types button {
  min-height: 42px;
  border: 1px solid #bfcfc7;
  border-radius: 5px;
  background: #fff;
  color: #254237;
  font-weight: 700;
}

.qa-parser-inspection-types button.active {
  border-color: #20352d;
  background: #20352d;
  color: #fff;
}

.qa-parser-number-field {
  display: grid;
  grid-template-columns: auto minmax(120px, 1fr) auto;
  align-items: center;
  gap: 10px;
  color: #20352d;
  font-weight: 700;
}

.qa-parser-number-field :deep(.el-input-number) {
  width: 100%;
}

.qa-parser-sampling-details {
  display: grid;
  align-content: start;
  gap: 10px;
  margin: 0;
}

.qa-parser-sampling-details div {
  padding: 11px;
  border-left: 3px solid #168a68;
  background: #f2f7f4;
}

.qa-parser-sampling-details dt {
  color: #607269;
  font-size: 12px;
}

.qa-parser-sampling-details dd {
  margin: 4px 0 0;
  color: #1d3329;
  font-size: 14px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.qa-parser-pqc-empty {
  display: grid;
  place-items: center;
  min-height: 520px;
  color: #6b7b74;
}

.qa-parser-pqc-footer {
  display: grid;
  grid-template-columns: minmax(120px, 0.4fr) minmax(220px, 1fr);
  gap: 14px;
  padding: 10px 12px;
  border-top: 1px solid #c8d6cf;
  background: #fff;
}

.qa-parser-pqc-footer button {
  border: 1px solid #c8d6cf;
  border-radius: 6px;
  background: #f4f6f5;
  color: #76827c;
  font-size: 18px;
  font-weight: 700;
}

.qa-parser-pqc-footer button.is-primary {
  border-color: #9bb9ad;
  background: #9bb9ad;
  color: #fff;
}

.qa-parser-process-selector {
  display: grid;
  gap: 8px;
}

.qa-parser-process-selector p {
  margin: 0 0 4px;
  color: #61736a;
}

.qa-parser-process-selector button {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 8px 12px;
  border: 1px solid #cad7d0;
  border-radius: 5px;
  background: #fff;
  color: #20352d;
  text-align: left;
}

.qa-parser-process-selector button.active {
  border-color: #168a68;
  background: #eef8f4;
}

.qa-parser-process-selector em {
  color: #6f8078;
  font-size: 12px;
  font-style: normal;
}

.qa-parser-fact-dialog {
  color: #20352d;
}

.qa-parser-fact-dialog strong {
  display: block;
  margin-top: 12px;
}

.qa-parser-fact-dialog strong:first-child {
  margin-top: 0;
}

.qa-parser-fact-dialog p {
  margin: 7px 0 0;
  padding: 12px;
  border-left: 3px solid #168a68;
  background: #f3f7f5;
  line-height: 1.65;
  white-space: pre-wrap;
}

@media (max-width: 1280px) {
  .qa-parser-workbench {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .qa-parser-panel-head,
  .qa-parser-panel-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .qa-parser-json-search,
  .qa-parser-pqc-body,
  .qa-parser-fact-strip,
  .qa-parser-result-actions {
    grid-template-columns: 1fr;
  }

  .qa-parser-pqc-shell {
    grid-template-rows: auto auto 70px;
  }
}
</style>
