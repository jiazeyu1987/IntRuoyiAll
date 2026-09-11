<template>
  <ContentWrap class="form-parser-page">
    <div class="form-parser-page__head">
      <h2>表单解析</h2>
      <div class="form-parser-page__actions">
        <el-upload
          ref="uploadRef"
          v-model:file-list="fileList"
          :auto-upload="false"
          :limit="1"
          :on-change="handleProductionFileChange"
          :on-exceed="handleExceed"
          :show-file-list="false"
          accept=".doc,.docx"
        >
          <template #trigger>
            <el-button
              v-hasPermi="['form:parser:production-batch-record']"
              class="scheme-d-btn scheme-d-btn--primary"
              :disabled="qaRegulationLoading"
              :loading="productionLoading"
              type="primary"
              @click="handleProductionBatchRecord"
            >
              <Icon icon="ep:upload-filled" />
              生产批记录
            </el-button>
          </template>
        </el-upload>
        <el-upload
          ref="qaRegulationUploadRef"
          v-model:file-list="qaRegulationFileList"
          :auto-upload="false"
          :limit="1"
          :on-change="handleQaRegulationFileChange"
          :on-exceed="handleQaRegulationExceed"
          :show-file-list="false"
          accept=".docx"
        >
          <template #trigger>
            <el-button
              class="scheme-d-btn scheme-d-btn--neutral"
              :disabled="productionLoading"
              :loading="qaRegulationLoading"
              @click="handleQaInspectionRegulation"
            >
              <Icon icon="ep:document" />
              QA检验规程
            </el-button>
          </template>
        </el-upload>
        <el-button
          class="scheme-d-btn scheme-d-btn--neutral"
          :disabled="productionLoading || qaRegulationLoading"
          @click="handleUnsupportedParseType('过程检验记录')"
        >
          <Icon icon="ep:document-checked" />
          过程检验记录
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeParserResultTab" class="form-parser-result-tabs">
      <el-tab-pane label="生产批记录" name="production-batch-record">
        <el-empty
          v-if="!lastResult || !previewRecognitionJson"
          description="请选择生产批记录 Word 文件"
        />

    <div v-else class="form-parser-workbench">
      <section class="form-parser-json-editor">
        <div class="form-parser-panel-head">
          <div>
            <div class="form-parser-panel-title">JSON编辑</div>
            <div class="form-parser-panel-subtitle">{{ lastDownloadName }}</div>
          </div>
          <div class="form-parser-panel-actions">
            <el-button type="primary" @click="handleApplyEditedJson">
              <Icon icon="ep:check" />
              应用
            </el-button>
            <el-button @click="downloadCurrentRecognitionJson">
              <Icon icon="ep:download" />
              下载当前JSON
            </el-button>
          </div>
        </div>
        <div class="form-parser-json-search">
          <el-input
            v-model="jsonSearchKeyword"
            class="form-parser-json-search__input"
            clearable
            placeholder="查找关键词"
            @clear="resetJsonSearchPosition"
            @input="resetJsonSearchPosition"
            @keyup.enter="locateNextJsonKeyword"
          >
            <template #prefix>
              <Icon icon="ep:search" />
            </template>
          </el-input>
          <el-button @click="locateNextJsonKeyword">
            <Icon icon="ep:search" />
            查找下一个
          </el-button>
          <span class="form-parser-json-search__count" aria-live="polite">
            {{ jsonSearchMatchLabel }}
          </span>
        </div>
        <el-input
          ref="jsonEditorInputRef"
          v-model="editableRecognitionJson"
          class="form-parser-json-input"
          type="textarea"
          :autosize="{ minRows: 28, maxRows: 44 }"
          spellcheck="false"
          @input="resetJsonSearchPosition"
        />
      </section>

      <section class="form-parser-frontline-preview">
        <div class="form-parser-panel-head">
          <div>
            <div class="form-parser-panel-title">一线生产预览</div>
            <div class="form-parser-panel-subtitle">{{ lastResult.sourceFileName }}</div>
          </div>
        </div>

        <div class="form-parser-frontline-shell">
          <div class="form-parser-frontline-header">
            <div class="form-parser-process-switcher">
              <button
                type="button"
                class="form-parser-nav-button"
                :disabled="!canGoPreviousProcess"
                aria-label="上一道工序"
                @click="goPreviousProcess"
              >
                <Icon icon="ep:arrow-left-bold" />
              </button>
              <button
                type="button"
                class="form-parser-process-card"
                @click="openProcessSelector"
              >
                <span>工序</span>
                <strong>{{ currentProcessTitle }}</strong>
              </button>
              <button
                type="button"
                class="form-parser-nav-button"
                :disabled="!canGoNextProcess"
                aria-label="下一道工序"
                @click="goNextProcess"
              >
                <Icon icon="ep:arrow-right-bold" />
              </button>
            </div>
          </div>

          <div v-if="currentProcess" class="form-parser-frontline-body">
            <div class="form-parser-production-panel">
              <div class="form-parser-quantity-row">
                <span>完成数量</span>
                <el-input-number
                  v-model="productionQuantity"
                  :min="0"
                  :precision="0"
                  controls-position="right"
                />
                <b>件</b>
              </div>
              <div class="form-parser-quantity-row form-parser-quantity-row--readonly">
                <span>损耗数量</span>
                <strong>{{ lossQuantity }}</strong>
                <b>件</b>
              </div>

              <div class="form-parser-material-block">
                <div class="form-parser-block-title">输入物料</div>
                <div v-if="currentProcessInputs.length" class="form-parser-material-list">
                  <button
                    v-for="(material, inputIndex) in currentProcessInputs"
                    :key="`input-${inputIndex}-${formatNameCode(material)}`"
                    type="button"
                    class="form-parser-material-chip"
                  >
                    {{ formatNameCode(material) }}
                  </button>
                </div>
                <div v-else class="form-parser-empty-text">未识别输入物料</div>
              </div>

              <div class="form-parser-material-block">
                <div class="form-parser-block-title">输出物料</div>
                <div v-if="currentProcessOutputs.length" class="form-parser-material-list">
                  <button
                    v-for="(material, outputIndex) in currentProcessOutputs"
                    :key="`output-${outputIndex}-${formatNameCode(material)}`"
                    type="button"
                    class="form-parser-material-chip form-parser-material-chip--output"
                  >
                    {{ formatNameCode(material) }}
                  </button>
                </div>
                <div v-else class="form-parser-empty-text">未识别输出物料</div>
              </div>
            </div>

            <div class="form-parser-equipment-panel">
              <div class="form-parser-confirm-strip">
                <button
                  type="button"
                  :class="[
                    'form-parser-confirm-card',
                    { 'form-parser-confirm-card--active': checklistStates.clearance }
                  ]"
                  @click="checklistStates.clearance = !checklistStates.clearance"
                >
                  <Icon icon="ep:select" />
                  清场
                </button>
                <button
                  type="button"
                  :class="[
                    'form-parser-confirm-card',
                    { 'form-parser-confirm-card--active': checklistStates.material }
                  ]"
                  @click="checklistStates.material = !checklistStates.material"
                >
                  <Icon icon="ep:select" />
                  物料
                </button>
                <button
                  type="button"
                  :class="[
                    'form-parser-confirm-card',
                    { 'form-parser-confirm-card--active': checklistStates.clean }
                  ]"
                  @click="checklistStates.clean = !checklistStates.clean"
                >
                  <Icon icon="ep:select" />
                  清洁
                </button>
              </div>

              <div v-if="visiblePreviewDeviceCards.length" class="form-parser-device-list">
                <div
                  class="form-parser-device-tabs device-tabs"
                  :style="{ '--frontline-device-tab-count': visiblePreviewDeviceCards.length }"
                  role="tablist"
                  aria-label="设备切换"
                >
                  <div
                    v-for="device in visiblePreviewDeviceCards"
                    :key="device.key"
                    class="form-parser-device-tab-card device-tab-card"
                    :class="{ active: selectedPreviewDeviceKeys.includes(device.key) }"
                  >
                    <button
                      class="form-parser-device-tab device-tab"
                      type="button"
                      role="checkbox"
                      :aria-checked="selectedPreviewDeviceKeys.includes(device.key)"
                      @click="togglePreviewDeviceSelection(device)"
                    >
                      <span class="form-parser-device-tab-selection device-tab-selection" aria-hidden="true">
                        {{ selectedPreviewDeviceKeys.includes(device.key) ? '✓' : '' }}
                      </span>
                      <span class="form-parser-device-tab-code device-tab-code">{{ device.label }}</span>
                    </button>
                    <label class="form-parser-device-metering-validity">
                      <input
                        type="checkbox"
                        checked
                        disabled
                        :aria-label="`${device.label}计量状态未记录`"
                      />
                      <span aria-hidden="true">✓</span>
                      <em>计量状态：未记录</em>
                    </label>
                  </div>
                </div>

                <div v-if="activePreviewDevice" class="form-parser-device-current">
                  <div v-if="activePreviewDevice.parameters.length" class="form-parser-parameter-list">
                    <div
                      v-for="(parameter, parameterIndex) in activePreviewDevice.parameters"
                      :key="`${activePreviewDevice.key}-parameter-${parameterIndex}`"
                      class="form-parser-parameter-field"
                    >
                      <div class="form-parser-parameter-field__label">
                        <span>{{ getParameterDisplayName(parameter) }}</span>
                        <em v-if="shouldShowParameterTargetRange(parameter)">
                          目标范围：{{ formatParameterTargetRange(parameter) }}
                        </em>
                        <em>默认值：{{ formatValue(parameter.ui?.defaultValue) }}</em>
                      </div>
                      <div class="form-parser-parameter-field__control">
                        <el-input-number
                          v-if="isNumberParameterControl(parameter)"
                          :controls="true"
                          :max="toNumberModelValue(parameter.ui?.max)"
                          :min="toNumberModelValue(parameter.ui?.min)"
                          :model-value="
                            toNumberModelValue(
                              getParameterPreviewValue(
                                currentProcessIndex,
                                activePreviewDevice.groupIndex,
                                activePreviewDevice.equipmentIndex,
                                parameterIndex,
                                parameter
                              )
                            )
                          "
                          :step="toNumberModelValue(parameter.ui?.step) ?? 1"
                          @update:model-value="
                            handleParameterPreviewValueChange(
                              currentProcessIndex,
                              activePreviewDevice.groupIndex,
                              activePreviewDevice.equipmentIndex,
                              parameterIndex,
                              $event
                            )
                          "
                        />
                        <el-select
                          v-else-if="isSelectParameterControl(parameter)"
                          :model-value="
                            toSelectModelValue(
                              getParameterPreviewValue(
                                currentProcessIndex,
                                activePreviewDevice.groupIndex,
                                activePreviewDevice.equipmentIndex,
                                parameterIndex,
                                parameter
                              )
                            )
                          "
                          @update:model-value="
                            handleParameterPreviewValueChange(
                              currentProcessIndex,
                              activePreviewDevice.groupIndex,
                              activePreviewDevice.equipmentIndex,
                              parameterIndex,
                              $event
                            )
                          "
                        >
                          <el-option
                            v-for="option in parameter.ui?.options || []"
                            :key="formatValue(option)"
                            :label="formatValue(option)"
                            :value="formatValue(option)"
                          />
                        </el-select>
                        <el-input
                          v-else-if="isTextParameterControl(parameter)"
                          :model-value="
                            toTextModelValue(
                              getParameterPreviewValue(
                                currentProcessIndex,
                                activePreviewDevice.groupIndex,
                                activePreviewDevice.equipmentIndex,
                                parameterIndex,
                                parameter
                              )
                            )
                          "
                          @update:model-value="
                            handleParameterPreviewValueChange(
                              currentProcessIndex,
                              activePreviewDevice.groupIndex,
                              activePreviewDevice.equipmentIndex,
                              parameterIndex,
                              $event
                            )
                          "
                        />
                        <el-tag v-else type="warning">
                          未识别控件：{{ formatValue(parameter.ui?.control) }}
                        </el-tag>
                        <b v-if="formatValue(parameter.ui?.unit) !== '-'">
                          {{ formatValue(parameter.ui?.unit) }}
                        </b>
                      </div>
                    </div>
                  </div>
                  <div v-else class="form-parser-empty-text">未识别设备参数</div>
                </div>

                <div v-else class="form-parser-empty-text form-parser-empty-text--panel">
                  请选择设备
                </div>
              </div>
              <div v-else class="form-parser-empty-text form-parser-empty-text--panel">
                未识别设备
              </div>
            </div>
          </div>

          <div v-else class="form-parser-frontline-empty">当前 JSON 未包含工序</div>

          <div class="form-parser-frontline-footer">
            <button type="button" class="form-parser-footer-button" disabled>重填</button>
            <button type="button" class="form-parser-footer-button form-parser-footer-button--primary" disabled>
              正式提交
            </button>
          </div>
        </div>
      </section>
    </div>

        <el-dialog
          v-if="previewRecognitionJson"
          v-model="processSelectorVisible"
          title="选择工序"
          width="560px"
          append-to-body
        >
          <div class="form-parser-process-selector">
            <div class="form-parser-process-selector__summary">
              共 {{ previewRecognitionJson.processes.length }} 个工序
            </div>
            <button
              v-for="(process, processIndex) in previewRecognitionJson.processes"
              :key="`process-option-${processIndex}`"
              type="button"
              :class="[
                'form-parser-process-option',
                { 'form-parser-process-option--active': processIndex === currentProcessIndex }
              ]"
              @click="handleSelectProcess(processIndex)"
            >
              <span>{{ processIndex + 1 }}</span>
              <strong>{{ formatValue(process.name) }}</strong>
              <em>{{ process.criticalProcess ? '关键/特殊工序' : '普通工序' }}</em>
            </button>
          </div>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="QA检验规程" name="qa-inspection-regulation">
        <QaInspectionRegulationParserPanel ref="qaRegulationParserRef" />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script setup lang="ts">
import type { UploadFile, UploadFiles, UploadInstance, UploadUserFile } from 'element-plus'
import download from '@/utils/download'
import { BatchRecordReportApi } from '@/api/mes/pro/batchrecordreport'
import QaInspectionRegulationParserPanel from './components/QaInspectionRegulationParserPanel.vue'

defineOptions({ name: 'FormCenterParser' })

interface BatchRecordTotalRecognitionProduct {
  name?: string
  code?: string
}

interface BatchRecordTotalRecognitionMaterial {
  code?: string | null
  name?: string
  sourceCodeLabel?: string | null
}

interface BatchRecordTotalRecognitionEquipmentOption {
  code?: string | null
  name?: string
  sourceCodeLabel?: string | null
}

interface BatchRecordTotalRecognitionParameterUi {
  control?: string
  defaultValue?: unknown
  step?: unknown
  min?: unknown
  max?: unknown
  unit?: string
  displayName?: string
  options?: unknown[]
}

interface BatchRecordTotalRecognitionParameter {
  name?: string
  referenceValue?: string
  actualValue?: string
  ui?: BatchRecordTotalRecognitionParameterUi
}

interface BatchRecordTotalRecognitionEquipmentGroup {
  equipmentOptions?: BatchRecordTotalRecognitionEquipmentOption[]
  parameters?: BatchRecordTotalRecognitionParameter[]
  selectionMode?: string
}

interface BatchRecordTotalRecognitionProcess {
  name: string
  criticalProcess?: boolean
  inputs?: BatchRecordTotalRecognitionMaterial[]
  outputs?: BatchRecordTotalRecognitionMaterial[]
  equipmentGroups?: BatchRecordTotalRecognitionEquipmentGroup[]
}

interface BatchRecordTotalRecognitionJson {
  product: BatchRecordTotalRecognitionProduct
  schemaVersion: number
  processes: BatchRecordTotalRecognitionProcess[]
}

interface ProductionBatchRecordParseResult {
  parseTypeName: string
  sourceFileName: string
  mapping: BatchRecordTotalRecognitionJson
}

interface BatchRecordPreviewDeviceCard {
  key: string
  groupIndex: number
  equipmentIndex: number
  group: BatchRecordTotalRecognitionEquipmentGroup
  equipment?: BatchRecordTotalRecognitionEquipmentOption
  label: string
  selectionMode?: string
  parameters: BatchRecordTotalRecognitionParameter[]
}

interface QaInspectionRegulationParserExpose {
  parseWordFile: (file: File) => Promise<boolean>
}

const message = useMessage()
const uploadRef = ref<UploadInstance>()
const qaRegulationUploadRef = ref<UploadInstance>()
const qaRegulationParserRef = ref<QaInspectionRegulationParserExpose>()
const jsonEditorInputRef = ref<{ textarea?: HTMLTextAreaElement }>()
const fileList = ref<UploadUserFile[]>([])
const qaRegulationFileList = ref<UploadUserFile[]>([])
const productionLoading = ref(false)
const qaRegulationLoading = ref(false)
const activeParserResultTab = ref('production-batch-record')
const lastResult = ref<ProductionBatchRecordParseResult>()
const lastDownloadName = ref('')
const editableRecognitionJson = ref('')
const jsonSearchKeyword = ref('')
const jsonSearchMatchIndex = ref(-1)
const jsonSearchMatchTotal = ref(0)
const jsonSearchLastKeyword = ref('')
const jsonSearchLastContent = ref('')
const previewRecognitionJson = ref<BatchRecordTotalRecognitionJson>()
const currentProcessIndex = ref(0)
const processSelectorVisible = ref(false)
const selectedPreviewDeviceKeys = ref<string[]>([])
const selectedPreviewDeviceKey = ref('')
const productionQuantity = ref<number | undefined>()
const lossQuantity = ref(0)
const parameterPreviewValues = reactive<Record<string, unknown>>({})
const checklistStates = reactive({
  clearance: true,
  material: true,
  clean: true
})
const JSON_EXTENSION = '.json'

const currentProcess = computed(() => {
  const processes = previewRecognitionJson.value?.processes || []
  return processes[currentProcessIndex.value]
})

const currentProcessInputs = computed(() => currentProcess.value?.inputs || [])

const currentProcessOutputs = computed(() => currentProcess.value?.outputs || [])

const currentProcessEquipmentGroups = computed(() => currentProcess.value?.equipmentGroups || [])

const visiblePreviewDeviceCards = computed<BatchRecordPreviewDeviceCard[]>(() => {
  return currentProcessEquipmentGroups.value.flatMap((group, groupIndex) => {
    return resolveEquipmentOptions(group).map((equipment, equipmentIndex) => ({
      key: buildPreviewDeviceKey(currentProcessIndex.value, groupIndex, equipmentIndex),
      groupIndex,
      equipmentIndex,
      group,
      equipment,
      label: formatEquipmentName(equipment, group, groupIndex),
      selectionMode: group.selectionMode,
      parameters: group.parameters || []
    }))
  })
})

const activePreviewDevice = computed(() =>
  selectedPreviewDeviceKeys.value.includes(selectedPreviewDeviceKey.value || '')
    ? visiblePreviewDeviceCards.value.find((device) => device.key === selectedPreviewDeviceKey.value)
    : undefined
)

const jsonSearchMatchLabel = computed(() => {
  if (!jsonSearchKeyword.value.trim()) {
    return ''
  }
  if (!jsonSearchMatchTotal.value) {
    return '0 / 0'
  }
  return `${jsonSearchMatchIndex.value + 1} / ${jsonSearchMatchTotal.value}`
})

const currentProcessTitle = computed(() => {
  if (!currentProcess.value) {
    return '-'
  }
  return `${currentProcessIndex.value + 1}. ${formatValue(currentProcess.value.name)}`
})

const canGoPreviousProcess = computed(() => currentProcessIndex.value > 0)

const canGoNextProcess = computed(() => {
  const processCount = previewRecognitionJson.value?.processes.length || 0
  return currentProcessIndex.value < processCount - 1
})

const buildPreviewDeviceKey = (
  processIndex: number,
  groupIndex: number,
  equipmentIndex: number
) => `${processIndex}:${groupIndex}:${equipmentIndex}`

const handleProductionBatchRecord = () => {
  activeParserResultTab.value = 'production-batch-record'
  if (productionLoading.value) {
    return
  }
}

const handleQaInspectionRegulation = () => {
  activeParserResultTab.value = 'qa-inspection-regulation'
}

const handleUnsupportedParseType = (label: string) => {
  message.warning(`${label}解析暂未开放`)
}

const handleExceed = (_files: File[], uploadFiles: UploadUserFile[]) => {
  uploadFiles.splice(0, uploadFiles.length)
  message.error('最多只能上传一个 doc/docx 文件')
}

const handleQaRegulationExceed = (_files: File[], uploadFiles: UploadUserFile[]) => {
  uploadFiles.splice(0, uploadFiles.length)
  message.error('最多只能上传一个 QA 检验规程 DOCX 文件')
}

const handleProductionFileChange = async (uploadFile: UploadFile, uploadFiles: UploadFiles) => {
  activeParserResultTab.value = 'production-batch-record'
  fileList.value = uploadFiles.slice(-1)
  const rawFile = uploadFile.raw
  if (!rawFile) {
    message.error('文件读取失败')
    return
  }
  if (!isWordFile(rawFile.name)) {
    uploadRef.value?.clearFiles()
    fileList.value = []
    message.error('请上传 doc/docx 文件')
    return
  }
  await parseAndDownloadProductionBatchRecord(rawFile)
}

const handleQaRegulationFileChange = async (uploadFile: UploadFile, uploadFiles: UploadFiles) => {
  activeParserResultTab.value = 'qa-inspection-regulation'
  qaRegulationFileList.value = uploadFiles.slice(-1)
  const rawFile = uploadFile.raw
  if (!rawFile) {
    message.error('QA 检验规程文件读取失败')
    return
  }
  if (!rawFile.name.toLowerCase().endsWith('.docx')) {
    qaRegulationUploadRef.value?.clearFiles()
    qaRegulationFileList.value = []
    message.error('QA 检验规程仅支持 DOCX 文件')
    return
  }
  qaRegulationLoading.value = true
  try {
    await nextTick()
    const parserPanel = qaRegulationParserRef.value
    if (!parserPanel) {
      message.error('QA 检验规程解析面板未就绪')
      return
    }
    await parserPanel.parseWordFile(rawFile)
  } finally {
    qaRegulationLoading.value = false
    qaRegulationUploadRef.value?.clearFiles()
    qaRegulationFileList.value = []
  }
}

const parseAndDownloadProductionBatchRecord = async (file: File) => {
  productionLoading.value = true
  try {
    const totalRecognitionJson = await BatchRecordReportApi.parseProductionBatchRecordTotalRecognitionJson(
      file
    )
    const mapping = parseTotalRecognitionJson(totalRecognitionJson)
    editableRecognitionJson.value = stringifyRecognitionJson(mapping)
    resetJsonSearchPosition()
    previewRecognitionJson.value = mapping
    initializePreviewState(mapping)
    const downloadName = buildJsonDownloadName(file.name)
    const jsonBlob = new Blob([JSON.stringify(mapping, null, 2)], {
      type: 'application/json;charset=utf-8'
    })
    download.json(jsonBlob, downloadName)
    lastResult.value = {
      parseTypeName: '生产批记录',
      sourceFileName: file.name,
      mapping
    }
    lastDownloadName.value = downloadName
    message.success('解析完成，已下载 JSON 文件')
  } catch (error) {
    message.error(resolveParseErrorMessage(error, '解析失败，请检查 Word 文件内容'))
    throw error
  } finally {
    productionLoading.value = false
    uploadRef.value?.clearFiles()
    fileList.value = []
  }
}

const handleApplyEditedJson = () => {
  try {
    const mapping = parseTotalRecognitionJson(editableRecognitionJson.value)
    previewRecognitionJson.value = mapping
    if (lastResult.value) {
      lastResult.value = {
        ...lastResult.value,
        mapping
      }
    }
    resetJsonSearchPosition()
    initializePreviewState(mapping)
    message.success('已应用 JSON 到右侧预览')
  } catch (error) {
    message.error(resolveParseErrorMessage(error, 'JSON 应用失败'))
  }
}

const downloadCurrentRecognitionJson = () => {
  if (!previewRecognitionJson.value) {
    message.error('没有可下载的 JSON')
    return
  }
  const jsonBlob = new Blob([stringifyRecognitionJson(previewRecognitionJson.value)], {
    type: 'application/json;charset=utf-8'
  })
  download.json(jsonBlob, lastDownloadName.value || buildJsonDownloadName(lastResult.value?.sourceFileName))
}

const goPreviousProcess = () => {
  if (!canGoPreviousProcess.value) {
    return
  }
  currentProcessIndex.value -= 1
  initializePreviewDeviceSelection()
}

const goNextProcess = () => {
  if (!canGoNextProcess.value) {
    return
  }
  currentProcessIndex.value += 1
  initializePreviewDeviceSelection()
}

const openProcessSelector = () => {
  processSelectorVisible.value = true
}

const handleSelectProcess = (processIndex: number) => {
  currentProcessIndex.value = processIndex
  processSelectorVisible.value = false
  initializePreviewDeviceSelection()
}

const resetJsonSearchPosition = () => {
  jsonSearchMatchIndex.value = -1
  jsonSearchMatchTotal.value = 0
  jsonSearchLastKeyword.value = ''
  jsonSearchLastContent.value = ''
}

const findJsonKeywordMatches = () => {
  const keyword = jsonSearchKeyword.value.trim()
  const matches: Array<{ start: number; end: number }> = []
  if (!keyword) {
    return matches
  }
  let startIndex = 0
  while (startIndex <= editableRecognitionJson.value.length) {
    const start = editableRecognitionJson.value.indexOf(keyword, startIndex)
    if (start < 0) {
      break
    }
    matches.push({ start, end: start + keyword.length })
    startIndex = start + keyword.length
  }
  return matches
}

const locateNextJsonKeyword = async () => {
  const keyword = jsonSearchKeyword.value.trim()
  if (!keyword) {
    resetJsonSearchPosition()
    message.warning('请输入查找关键词')
    return
  }
  const matches = findJsonKeywordMatches()
  jsonSearchMatchTotal.value = matches.length
  if (!matches.length) {
    jsonSearchMatchIndex.value = -1
    message.warning(`未找到关键词：${keyword}`)
    return
  }
  if (
    jsonSearchLastKeyword.value !== keyword ||
    jsonSearchLastContent.value !== editableRecognitionJson.value
  ) {
    jsonSearchMatchIndex.value = -1
    jsonSearchLastKeyword.value = keyword
    jsonSearchLastContent.value = editableRecognitionJson.value
  }
  jsonSearchMatchIndex.value = (jsonSearchMatchIndex.value + 1) % matches.length
  const match = matches[jsonSearchMatchIndex.value]
  await nextTick()
  const textarea = jsonEditorInputRef.value?.textarea
  if (!textarea) {
    message.error('JSON编辑器未就绪')
    return
  }
  textarea.focus()
  textarea.setSelectionRange(match.start, match.end)
  textarea.scrollTop = calculateJsonKeywordScrollTop(textarea, match.start)
}

const calculateJsonKeywordScrollTop = (textarea: HTMLTextAreaElement, matchStart: number) => {
  const lineHeight = Number.parseFloat(window.getComputedStyle(textarea).lineHeight)
  if (!Number.isFinite(lineHeight) || lineHeight <= 0) {
    message.error('JSON编辑器行高无效')
    return textarea.scrollTop
  }
  const lineIndex = editableRecognitionJson.value.slice(0, matchStart).split('\n').length - 1
  return Math.max(lineIndex * lineHeight - textarea.clientHeight / 2, 0)
}

const initializePreviewState = (mapping: BatchRecordTotalRecognitionJson) => {
  const processCount = mapping.processes.length
  if (!processCount) {
    currentProcessIndex.value = 0
  } else if (currentProcessIndex.value >= processCount) {
    currentProcessIndex.value = processCount - 1
  }
  productionQuantity.value = undefined
  lossQuantity.value = 0
  checklistStates.clearance = true
  checklistStates.material = true
  checklistStates.clean = true
  initializeParameterPreviewValues(mapping)
  initializePreviewDeviceSelection()
}

const initializePreviewDeviceSelection = () => {
  const firstDevice = visiblePreviewDeviceCards.value[0]
  if (!firstDevice) {
    selectedPreviewDeviceKeys.value = []
    selectedPreviewDeviceKey.value = ''
    return
  }
  selectedPreviewDeviceKeys.value = [firstDevice.key]
  selectedPreviewDeviceKey.value = firstDevice.key
}

const togglePreviewDeviceSelection = (device: BatchRecordPreviewDeviceCard) => {
  const selected = new Set(selectedPreviewDeviceKeys.value)
  if (selected.has(device.key)) {
    selected.delete(device.key)
    clearPreviewDeviceParameterValues(device)
    selectedPreviewDeviceKeys.value = [...selected]
    if (selectedPreviewDeviceKey.value === device.key) {
      selectedPreviewDeviceKey.value = selectedPreviewDeviceKeys.value[0] || ''
    }
    return
  }
  if (device.selectionMode === 'SINGLE') {
    visiblePreviewDeviceCards.value
      .filter((visibleDevice) => visibleDevice.groupIndex === device.groupIndex)
      .forEach((visibleDevice) => {
        selected.delete(visibleDevice.key)
        clearPreviewDeviceParameterValues(visibleDevice)
      })
  }
  selected.add(device.key)
  selectedPreviewDeviceKeys.value = [...selected]
  selectedPreviewDeviceKey.value = device.key
}

const clearPreviewDeviceParameterValues = (device: BatchRecordPreviewDeviceCard) => {
  device.parameters.forEach((_parameter, parameterIndex) => {
    const previewKey = buildParameterPreviewValueKey(
      currentProcessIndex.value,
      device.groupIndex,
      device.equipmentIndex,
      parameterIndex
    )
    delete parameterPreviewValues[previewKey]
  })
}

const initializeParameterPreviewValues = (mapping: BatchRecordTotalRecognitionJson) => {
  Object.keys(parameterPreviewValues).forEach((key) => {
    delete parameterPreviewValues[key]
  })
  mapping.processes.forEach((process, processIndex) => {
    const equipmentGroups = process.equipmentGroups || []
    equipmentGroups.forEach((group, groupIndex) => {
      const equipmentIndexes = resolveEquipmentOptions(group).map((_, equipmentIndex) => equipmentIndex)
      equipmentIndexes.forEach((equipmentIndex) => {
        const parameters = group.parameters || []
        parameters.forEach((parameter, parameterIndex) => {
          const previewKey = buildParameterPreviewValueKey(
            processIndex,
            groupIndex,
            equipmentIndex,
            parameterIndex
          )
          parameterPreviewValues[previewKey] = parameter.ui?.defaultValue
        })
      })
    })
  })
}

const buildParameterPreviewValueKey = (
  processIndex: number,
  groupIndex: number,
  equipmentIndex: number,
  parameterIndex: number
) => {
  return `${processIndex}:${groupIndex}:${equipmentIndex}:${parameterIndex}`
}

const getParameterPreviewValue = (
  processIndex: number,
  groupIndex: number,
  equipmentIndex: number,
  parameterIndex: number,
  parameter: BatchRecordTotalRecognitionParameter
) => {
  const key = buildParameterPreviewValueKey(processIndex, groupIndex, equipmentIndex, parameterIndex)
  if (Object.prototype.hasOwnProperty.call(parameterPreviewValues, key)) {
    return parameterPreviewValues[key]
  }
  return parameter.ui?.defaultValue
}

const handleParameterPreviewValueChange = (
  processIndex: number,
  groupIndex: number,
  equipmentIndex: number,
  parameterIndex: number,
  value: unknown
) => {
  const key = buildParameterPreviewValueKey(processIndex, groupIndex, equipmentIndex, parameterIndex)
  parameterPreviewValues[key] = value
}

const isWordFile = (filename: string) => /\.(doc|docx)$/i.test(filename)

const buildJsonDownloadName = (sourceFileName?: string) => {
  const baseName = (sourceFileName || '生产批记录解析')
    .replace(/\.(doc|docx)$/i, '')
    .replace(/[\\/:*?"<>|]/g, '_')
    .trim()
  return `${baseName || '生产批记录解析'}${JSON_EXTENSION}`
}

const stringifyRecognitionJson = (mapping: BatchRecordTotalRecognitionJson) => {
  return JSON.stringify(mapping, null, 2)
}

const parseTotalRecognitionJson = (totalRecognitionJson: string): BatchRecordTotalRecognitionJson => {
  let parsed: unknown
  try {
    parsed = JSON.parse(totalRecognitionJson)
  } catch (error) {
    throw new Error(resolveParseErrorMessage(error, '生产批记录 JSON 格式错误'))
  }
  return validateTotalRecognitionJson(parsed)
}

const validateTotalRecognitionJson = (parsed: unknown): BatchRecordTotalRecognitionJson => {
  if (!isRecordValue(parsed)) {
    throw new Error('生产批记录 JSON 结构无效')
  }
  if (!isRecordValue(parsed.product)) {
    throw new Error('生产批记录 JSON 缺少 product')
  }
  if (typeof parsed.schemaVersion !== 'number') {
    throw new Error('生产批记录 JSON 缺少 schemaVersion')
  }
  if (!Array.isArray(parsed.processes)) {
    throw new Error('生产批记录 JSON 缺少 processes')
  }
  parsed.processes.forEach((process, processIndex) => {
    validateProcess(process, `processes[${processIndex}]`)
  })
  return parsed as unknown as BatchRecordTotalRecognitionJson
}

const validateProcess = (process: unknown, path: string) => {
  if (!isRecordValue(process)) {
    throw new Error(`生产批记录 JSON ${path} 必须是对象`)
  }
  if (typeof process.name !== 'string' || !process.name.trim()) {
    throw new Error(`生产批记录 JSON ${path}.name 缺失`)
  }
  validateOptionalObjectArray(process, 'inputs', `${path}.inputs`)
  validateOptionalObjectArray(process, 'outputs', `${path}.outputs`)
  const equipmentGroups = validateOptionalObjectArray(
    process,
    'equipmentGroups',
    `${path}.equipmentGroups`
  )
  equipmentGroups?.forEach((group, groupIndex) => {
    validateEquipmentGroup(group, `${path}.equipmentGroups[${groupIndex}]`)
  })
}

const validateEquipmentGroup = (group: Record<string, unknown>, path: string) => {
  validateOptionalObjectArray(group, 'equipmentOptions', `${path}.equipmentOptions`)
  const parameters = validateOptionalObjectArray(group, 'parameters', `${path}.parameters`)
  parameters?.forEach((parameter, parameterIndex) => {
    validateParameter(parameter, `${path}.parameters[${parameterIndex}]`)
  })
}

const validateParameter = (parameter: Record<string, unknown>, path: string) => {
  if (parameter.ui !== undefined && parameter.ui !== null && !isRecordValue(parameter.ui)) {
    throw new Error(`生产批记录 JSON ${path}.ui 必须是对象`)
  }
  const ui = parameter.ui
  if (isRecordValue(ui) && ui.options !== undefined && !Array.isArray(ui.options)) {
    throw new Error(`生产批记录 JSON ${path}.ui.options 必须是数组`)
  }
}

const validateOptionalObjectArray = (
  source: Record<string, unknown>,
  field: string,
  path: string
): Record<string, unknown>[] | undefined => {
  const value = source[field]
  if (value === undefined || value === null) {
    return undefined
  }
  if (!Array.isArray(value)) {
    throw new Error(`生产批记录 JSON ${path} 必须是数组`)
  }
  value.forEach((item, index) => {
    if (!isRecordValue(item)) {
      throw new Error(`生产批记录 JSON ${path}[${index}] 必须是对象`)
    }
  })
  return value as Record<string, unknown>[]
}

const isRecordValue = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

const resolveEquipmentOptions = (group?: BatchRecordTotalRecognitionEquipmentGroup) => {
  return group?.equipmentOptions?.length ? group.equipmentOptions : [undefined]
}

const formatNameCode = (
  item?: {
    name?: string
    code?: string | null
    sourceCodeLabel?: string | null
  }
) => {
  const name = formatValue(item?.name)
  const code = formatValue(item?.code || item?.sourceCodeLabel)
  if (name === '-' && code === '-') {
    return '-'
  }
  if (code === '-') {
    return name
  }
  return `${name}(${code})`
}

const formatEquipmentName = (
  equipment: BatchRecordTotalRecognitionEquipmentOption | undefined,
  group: BatchRecordTotalRecognitionEquipmentGroup,
  groupIndex: number
) => {
  if (equipment) {
    return formatNameCode(equipment)
  }
  const options = formatEquipmentGroupOptions(group)
  if (options !== '-') {
    return options
  }
  return `设备组 ${groupIndex + 1}`
}

const formatEquipmentGroupOptions = (group?: BatchRecordTotalRecognitionEquipmentGroup) => {
  return (group?.equipmentOptions || []).map((equipment) => formatNameCode(equipment)).join('、') || '-'
}

const formatSelectionMode = (selectionMode?: string) => {
  if (selectionMode === 'SINGLE') {
    return '单选'
  }
  if (selectionMode === 'MULTIPLE') {
    return '多选'
  }
  return formatValue(selectionMode)
}

const getParameterDisplayName = (parameter?: BatchRecordTotalRecognitionParameter) => {
  const displayName = formatValue(parameter?.ui?.displayName)
  if (displayName !== '-') {
    return displayName
  }
  return formatValue(parameter?.name)
}

const formatParameterTargetRange = (parameter?: BatchRecordTotalRecognitionParameter) => {
  const min = formatValue(parameter?.ui?.min)
  const max = formatValue(parameter?.ui?.max)
  const unit = formatValue(parameter?.ui?.unit)
  if (min !== '-' && max !== '-') {
    return `${min} - ${max}${unit === '-' ? '' : ` ${unit}`}`
  }
  if (min !== '-') {
    return `≥ ${min}${unit === '-' ? '' : ` ${unit}`}`
  }
  if (max !== '-') {
    return `≤ ${max}${unit === '-' ? '' : ` ${unit}`}`
  }
  return formatValue(parameter?.referenceValue)
}

const shouldShowParameterTargetRange = (parameter?: BatchRecordTotalRecognitionParameter) => {
  if (
    toNumberModelValue(parameter?.ui?.min) !== undefined &&
    toNumberModelValue(parameter?.ui?.max) !== undefined
  ) {
    return true
  }
  const referenceValue = formatValue(parameter?.referenceValue)
  return /[-~～至±]/.test(referenceValue)
}

const isNumberParameterControl = (parameter?: BatchRecordTotalRecognitionParameter) => {
  return parameter?.ui?.control === 'number'
}

const isSelectParameterControl = (parameter?: BatchRecordTotalRecognitionParameter) => {
  return parameter?.ui?.control === 'select'
}

const isTextParameterControl = (parameter?: BatchRecordTotalRecognitionParameter) => {
  return parameter?.ui?.control === 'text' || parameter?.ui?.control === 'input'
}

const toNumberModelValue = (value: unknown): number | undefined => {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) {
      return parsed
    }
  }
  return undefined
}

const toSelectModelValue = (value: unknown): string | undefined => {
  const formatted = formatValue(value)
  if (formatted === '-') {
    return undefined
  }
  return formatted
}

const toTextModelValue = (value: unknown): string => {
  const formatted = formatValue(value)
  if (formatted === '-') {
    return ''
  }
  return formatted
}

const formatValue = (value: unknown): string => {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (Array.isArray(value)) {
    return value.map((item) => formatValue(item)).join('、') || '-'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

const resolveParseErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as { response?: { data?: { msg?: string } } })?.response?.data?.msg
  const directMessage = (error as { message?: string })?.message
  return responseMessage || directMessage || fallback
}
</script>

<style lang="scss" scoped>
.form-parser-page {
  min-height: calc(100vh - 160px);

  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    gap: 18px;
  }
}

.form-parser-page__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  h2 {
    margin: 0;
    color: #1f2937;
    font-size: 20px;
    font-weight: 700;
    line-height: 28px;
  }
}

.form-parser-page__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;

  :deep(.el-upload) {
    display: inline-flex;
  }

  :deep(.el-button) {
    min-width: 136px;
  }

  :deep(.iconify) {
    margin-right: 6px;
  }
}

.form-parser-result-tabs {
  min-width: 0;

  :deep(.el-tabs__header) {
    margin-bottom: 14px;
  }

  :deep(.el-tabs__item) {
    min-width: 132px;
    height: 42px;
    padding: 0 18px;
    color: #52635b;
    font-weight: 700;
  }

  :deep(.el-tabs__item.is-active) {
    color: #168a68;
  }

  :deep(.el-tabs__active-bar) {
    background-color: #168a68;
  }
}

.form-parser-workbench {
  display: grid;
  align-items: start;
  grid-template-columns: minmax(460px, 0.88fr) minmax(640px, 1.12fr);
  gap: 16px;
}

.form-parser-json-editor,
.form-parser-frontline-preview {
  min-width: 0;
  padding: 16px;
  border: 1px solid #dce4e0;
  border-radius: 8px;
  background: #fff;
}

.form-parser-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;

  > div:first-child {
    min-width: 0;
  }
}

.form-parser-panel-title {
  color: #10231f;
  font-size: 16px;
  font-weight: 800;
  line-height: 24px;
}

.form-parser-panel-subtitle {
  max-width: 100%;
  overflow: hidden;
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-parser-panel-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;

  :deep(.iconify) {
    margin-right: 4px;
  }
}

.form-parser-json-editor {
  .form-parser-panel-head {
    align-items: stretch;
    flex-direction: column;
  }

  .form-parser-panel-actions {
    justify-content: flex-start;

    :deep(.el-button) {
      flex: 1 1 140px;
      min-width: 0;
    }
  }
}

.form-parser-json-input {
  :deep(.el-textarea__inner) {
    font-family: 'Cascadia Mono', Consolas, monospace;
    font-size: 13px;
    line-height: 20px;
    white-space: pre;
  }
}

.form-parser-json-search {
  display: grid;
  align-items: center;
  grid-template-columns: minmax(180px, 1fr) auto 58px;
  gap: 8px;
  margin-bottom: 10px;

  :deep(.iconify) {
    margin-right: 4px;
  }
}

.form-parser-json-search__input {
  min-width: 0;
}

.form-parser-json-search__count {
  color: #49615a;
  font-size: 13px;
  font-weight: 800;
  line-height: 20px;
  text-align: right;
  white-space: nowrap;
}

.form-parser-frontline-shell {
  display: flex;
  overflow: hidden;
  flex-direction: column;
  border: 1px solid #cfd9d4;
  border-radius: 8px;
  background: #eef5f0;
}

.form-parser-frontline-header {
  display: grid;
  grid-template-columns: minmax(0, 480px);
  gap: 10px;
  justify-content: center;
  padding: 10px;
}

.form-parser-process-switcher,
.form-parser-production-panel,
.form-parser-equipment-panel,
.form-parser-frontline-footer {
  border: 1px solid #cad6d0;
  border-radius: 8px;
  background: #fff;
}

.form-parser-process-switcher {
  display: grid;
  min-height: 72px;
  grid-template-columns: 74px minmax(0, 1fr) 74px;
  gap: 10px;
  padding: 10px;
}

.form-parser-nav-button,
.form-parser-process-card {
  border: 1px solid #cad6d0;
  border-radius: 8px;
  background: #fff;
  color: #071814;
}

.form-parser-nav-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 34px;

  &:disabled {
    color: #a9b4af;
    cursor: not-allowed;
  }
}

.form-parser-process-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  span {
    color: #50645e;
    font-size: 16px;
    font-weight: 700;
    line-height: 20px;
  }

  strong {
    display: block;
    max-width: 100%;
    overflow: hidden;
    color: #071814;
    font-size: 26px;
    font-weight: 900;
    line-height: 34px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.form-parser-frontline-body {
  display: grid;
  grid-template-columns: minmax(300px, 0.92fr) minmax(360px, 1.08fr);
  gap: 12px;
  padding: 0 10px 10px;
}

.form-parser-production-panel,
.form-parser-equipment-panel {
  min-width: 0;
  min-height: 500px;
  padding: 16px;
}

.form-parser-quantity-row {
  display: grid;
  align-items: center;
  grid-template-columns: 130px minmax(150px, 1fr) 34px;
  gap: 12px;
  margin-bottom: 14px;
  color: #071814;

  span {
    font-size: 20px;
    font-weight: 800;
  }

  b {
    font-size: 18px;
  }

  :deep(.el-input-number) {
    width: 100%;
  }
}

.form-parser-quantity-row--readonly {
  strong {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 40px;
    border: 1px solid #d7e0dc;
    border-radius: 8px;
    background: #edf3f0;
    font-size: 28px;
    line-height: 36px;
  }
}

.form-parser-material-block {
  margin-top: 20px;
}

.form-parser-block-title {
  margin-bottom: 10px;
  color: #10231f;
  font-size: 18px;
  font-weight: 800;
  line-height: 24px;
}

.form-parser-material-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.form-parser-material-chip {
  max-width: 100%;
  padding: 8px 12px;
  border: 1px solid #d0dbd6;
  border-radius: 8px;
  background: #fff;
  color: #0f2922;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
  text-align: left;
  word-break: break-word;
}

.form-parser-material-chip--output {
  border-color: #1c8b69;
  background: #f1fbf7;
}

.form-parser-confirm-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 14px;
}

.form-parser-confirm-card {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 44px;
  border: 1px solid #cfd9d4;
  border-radius: 8px;
  background: #fff;
  color: #12352c;
  cursor: pointer;
  font-size: 14px;
  font-weight: 800;

  .iconify {
    color: #9aa7a2;
    font-size: 20px;
  }
}

.form-parser-confirm-card--active {
  border-color: #168767;
  color: #12352c;

  .iconify {
    color: #168767;
  }
}

.form-parser-device-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-parser-device-tabs {
  display: grid;
  grid-template-columns: repeat(var(--frontline-device-tab-count, 1), minmax(0, 1fr));
  gap: 12px;
  min-width: 0;
}

.form-parser-device-tab-card {
  display: grid;
  grid-template-rows: minmax(0, 1fr) 36px;
  min-width: 0;
  height: 110px;
  overflow: hidden;
  border: 3px solid #cad6d0;
  border-radius: 20px;
  background: #f8faf8;

  &.active {
    border-color: #15815f;
  }
}

.form-parser-device-tab {
  position: relative;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  padding: 0 38px;
  border: 0;
  background: #20352d;
  color: #fff;
  cursor: pointer;
  font-size: 20px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-parser-device-tab-selection {
  position: absolute;
  top: 10px;
  left: 10px;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border: 2px solid #fff;
  border-radius: 5px;
  color: #fff;
  font-size: 17px;
  line-height: 1;
}

.form-parser-device-tab-card.active .form-parser-device-tab-selection {
  border-color: #9ce0c1;
  background: #15815f;
}

.form-parser-device-tab-code {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.form-parser-device-metering-validity {
  display: grid;
  align-items: center;
  justify-content: center;
  grid-template-columns: 18px auto;
  gap: 6px;
  min-width: 0;
  min-height: 36px;
  padding: 0 6px;
  background: #fff;
  color: #071814;
  cursor: default;

  input {
    position: absolute;
    opacity: 0;
    pointer-events: none;
  }

  span {
    display: grid;
    place-items: center;
    width: 18px;
    height: 18px;
    border-radius: 5px;
    background: #15815f;
    color: #fff;
    font-size: 14px;
    font-weight: 900;
    line-height: 1;
  }

  em {
    min-width: 0;
    overflow: hidden;
    font-size: 14px;
    font-style: normal;
    font-weight: 900;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.form-parser-device-current {
  display: grid;
  align-content: start;
  gap: 10px;
  min-width: 0;
  min-height: 280px;
  overflow: auto;
  padding: 14px;
  border: 3px solid #cad6d0;
  border-radius: 24px;
  background: #fbfdfb;
}

.form-parser-parameter-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
}

.form-parser-parameter-field {
  display: grid;
  align-items: center;
  grid-template-columns: minmax(128px, 0.45fr) minmax(220px, 0.55fr);
  gap: 12px;
}

.form-parser-parameter-field__label {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;

  span {
    color: #071814;
    font-size: 16px;
    font-weight: 900;
    line-height: 22px;
  }

  em {
    color: #335f55;
    font-size: 12px;
    font-style: normal;
    font-weight: 700;
    line-height: 16px;
  }
}

.form-parser-parameter-field__control {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;

  :deep(.el-input-number),
  :deep(.el-select),
  :deep(.el-input) {
    width: 100%;
  }

  b {
    flex: 0 0 auto;
    color: #071814;
    font-size: 16px;
    font-weight: 900;
  }
}

.form-parser-empty-text,
.form-parser-frontline-empty {
  color: #6b7280;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
}

.form-parser-empty-text--panel {
  display: flex;
  min-height: 220px;
  align-items: center;
  justify-content: center;
  border: 1px dashed #cbd5d1;
  border-radius: 8px;
}

.form-parser-frontline-empty {
  display: flex;
  min-height: 360px;
  align-items: center;
  justify-content: center;
  margin: 0 10px 10px;
  border: 1px dashed #cbd5d1;
  border-radius: 8px;
  background: #fff;
}

.form-parser-frontline-footer {
  display: grid;
  grid-template-columns: minmax(140px, 0.32fr) minmax(220px, 0.68fr);
  gap: 12px;
  padding: 12px;
  border-width: 1px 0 0;
  border-radius: 0;
}

.form-parser-footer-button {
  min-height: 54px;
  border: 1px solid #cfd9d4;
  border-radius: 8px;
  background: #fff;
  color: #1f2937;
  cursor: default;
  font-size: 24px;
  font-weight: 800;
}

.form-parser-footer-button--primary {
  border-color: #168767;
  background: #168767;
  color: #fff;
}

.form-parser-process-selector {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-parser-process-selector__summary {
  color: #49615a;
  font-size: 14px;
  font-weight: 800;
  line-height: 20px;
}

.form-parser-process-option {
  display: grid;
  align-items: center;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d7e0dc;
  border-radius: 8px;
  background: #fff;
  color: #10231f;
  cursor: pointer;
  text-align: left;

  span {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: #eef5f0;
    font-weight: 900;
  }

  strong {
    overflow: hidden;
    font-size: 15px;
    line-height: 22px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  em {
    color: #587069;
    font-size: 12px;
    font-style: normal;
    font-weight: 800;
  }
}

.form-parser-process-option--active {
  border-color: #168767;
  background: #f0fbf6;
}

@media (max-width: 1180px) {
  .form-parser-workbench,
  .form-parser-frontline-body {
    grid-template-columns: 1fr;
  }

  .form-parser-frontline-header {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .form-parser-page__head {
    align-items: stretch;
    flex-direction: column;
  }

  .form-parser-page__actions {
    justify-content: flex-start;

    :deep(.el-button) {
      width: 100%;
      min-width: 0;
    }

    :deep(.el-upload) {
      width: 100%;
    }
  }

  .form-parser-json-editor,
  .form-parser-frontline-preview {
    padding: 12px;
  }

  .form-parser-panel-head,
  .form-parser-panel-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .form-parser-process-switcher,
  .form-parser-quantity-row,
  .form-parser-parameter-field,
  .form-parser-json-search,
  .form-parser-frontline-footer,
  .form-parser-process-option {
    grid-template-columns: 1fr;
  }
}
</style>
