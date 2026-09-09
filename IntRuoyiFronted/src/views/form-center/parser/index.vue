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
              :loading="productionLoading"
              type="primary"
              @click="handleProductionBatchRecord"
            >
              <Icon icon="ep:upload-filled" />
              生产批记录
            </el-button>
          </template>
        </el-upload>
        <el-button
          class="scheme-d-btn scheme-d-btn--neutral"
          :disabled="productionLoading"
          @click="handleUnsupportedParseType('QA检验规程')"
        >
          <Icon icon="ep:document" />
          QA检验规程
        </el-button>
        <el-button
          class="scheme-d-btn scheme-d-btn--neutral"
          :disabled="productionLoading"
          @click="handleUnsupportedParseType('过程检验记录')"
        >
          <Icon icon="ep:document-checked" />
          过程检验记录
        </el-button>
      </div>
    </div>

    <el-empty v-if="!lastResult" description="请选择生产批记录 Word 文件" />

    <div v-else class="form-parser-result">
      <el-tree
        class="form-parser-tree"
        :data="batchRecordTreeData"
        default-expand-all
        :expand-on-click-node="false"
        :indent="24"
        node-key="id"
      >
        <template #default="{ data }">
          <div :class="['form-parser-tree-node', `form-parser-tree-node--${data.kind}`]">
            <div class="form-parser-tree-node__line">
              <span class="form-parser-tree-node__label">{{ data.label }}</span>
              <span v-if="data.valueText" class="form-parser-tree-node__value">
                {{ data.valueText }}
              </span>
            </div>
            <div v-if="data.meta?.length" class="form-parser-tree-node__meta">
              <span
                v-for="meta in data.meta"
                :key="`${data.id}-${meta.label}`"
                class="form-parser-tree-node__meta-item"
              >
                {{ meta.label }}：{{ meta.value }}
              </span>
            </div>
            <div v-if="data.parameter" class="form-parser-tree-parameter-ui">
              <div class="form-parser-tree-parameter-ui__label">
                <span>{{ getParameterDisplayName(data.parameter) }}</span>
                <span
                  v-if="shouldShowParameterTargetRange(data.parameter)"
                  class="form-parser-tree-parameter-ui__range"
                >
                  目标范围：{{ formatParameterTargetRange(data.parameter) }}
                </span>
              </div>
              <div class="form-parser-tree-parameter-ui__control">
                <el-input-number
                  v-if="isNumberParameterControl(data.parameter)"
                  :controls="true"
                  :max="toNumberModelValue(data.parameter.ui?.max)"
                  :min="toNumberModelValue(data.parameter.ui?.min)"
                  :model-value="toNumberModelValue(getParameterPreviewValue(data as BatchRecordTreeNode))"
                  :step="toNumberModelValue(data.parameter.ui?.step)"
                  @update:model-value="setParameterPreviewValue(data as BatchRecordTreeNode, $event)"
                />
                <el-select
                  v-else-if="isSelectParameterControl(data.parameter)"
                  :model-value="toSelectModelValue(getParameterPreviewValue(data as BatchRecordTreeNode))"
                  placeholder="-"
                  @update:model-value="setParameterPreviewValue(data as BatchRecordTreeNode, $event)"
                >
                  <el-option
                    v-for="option in data.parameter.ui?.options || []"
                    :key="formatValue(option)"
                    :label="formatValue(option)"
                    :value="formatValue(option)"
                  />
                </el-select>
                <el-input
                  v-else-if="isTextParameterControl(data.parameter)"
                  :model-value="toTextModelValue(getParameterPreviewValue(data as BatchRecordTreeNode))"
                  @update:model-value="setParameterPreviewValue(data as BatchRecordTreeNode, $event)"
                />
                <el-tag v-else type="warning">
                  未识别控件：{{ formatValue(data.parameter.ui?.control) }}
                </el-tag>
                <span
                  v-if="formatValue(data.parameter.ui?.unit) !== '-'"
                  class="form-parser-tree-parameter-ui__unit"
                >
                  {{ formatValue(data.parameter.ui?.unit) }}
                </span>
              </div>
            </div>
          </div>
        </template>
      </el-tree>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import type { UploadFile, UploadFiles, UploadInstance, UploadUserFile } from 'element-plus'
import download from '@/utils/download'
import { BatchRecordReportApi } from '@/api/mes/pro/batchrecordreport'

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

interface BatchRecordTreeMeta {
  label: string
  value: string
}

interface BatchRecordTreeNode {
  id: string
  label: string
  kind:
    | 'root'
    | 'product'
    | 'process'
    | 'section'
    | 'material'
    | 'equipment'
    | 'parameter'
    | 'field'
    | 'empty'
  valueText?: string
  meta?: BatchRecordTreeMeta[]
  parameter?: BatchRecordTotalRecognitionParameter
  previewValueKey?: string
  children?: BatchRecordTreeNode[]
}

const message = useMessage()
const uploadRef = ref<UploadInstance>()
const fileList = ref<UploadUserFile[]>([])
const productionLoading = ref(false)
const lastResult = ref<ProductionBatchRecordParseResult>()
const lastDownloadName = ref('')
const parameterPreviewValues = reactive<Record<string, unknown>>({})
const JSON_EXTENSION = '.json'
const batchRecordTreeData = computed<BatchRecordTreeNode[]>(() => {
  if (!lastResult.value) {
    return []
  }
  return buildBatchRecordTreeData(lastResult.value, lastDownloadName.value)
})

const handleProductionBatchRecord = () => {
  if (productionLoading.value) {
    return
  }
}

const handleUnsupportedParseType = (label: string) => {
  message.warning(`${label}解析暂未开放`)
}

const handleExceed = (_files: File[], uploadFiles: UploadUserFile[]) => {
  uploadFiles.splice(0, uploadFiles.length)
  message.error('最多只能上传一个 doc/docx 文件')
}

const handleProductionFileChange = async (uploadFile: UploadFile, uploadFiles: UploadFiles) => {
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

const parseAndDownloadProductionBatchRecord = async (file: File) => {
  productionLoading.value = true
  try {
    const totalRecognitionJson = await BatchRecordReportApi.parseProductionBatchRecordTotalRecognitionJson(
      file
    )
    const mapping = parseTotalRecognitionJson(totalRecognitionJson)
    initializeParameterPreviewValues(mapping)
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

const isWordFile = (filename: string) => /\.(doc|docx)$/i.test(filename)

const buildJsonDownloadName = (sourceFileName?: string) => {
  const baseName = (sourceFileName || '生产批记录解析')
    .replace(/\.(doc|docx)$/i, '')
    .replace(/[\\/:*?"<>|]/g, '_')
    .trim()
  return `${baseName || '生产批记录解析'}${JSON_EXTENSION}`
}

const parseTotalRecognitionJson = (totalRecognitionJson: string): BatchRecordTotalRecognitionJson => {
  const parsed = JSON.parse(totalRecognitionJson) as BatchRecordTotalRecognitionJson
  if (!parsed || typeof parsed !== 'object') {
    throw new Error('生产批记录 JSON 结构无效')
  }
  if (!parsed.product || typeof parsed.product !== 'object') {
    throw new Error('生产批记录 JSON 缺少 product')
  }
  if (typeof parsed.schemaVersion !== 'number') {
    throw new Error('生产批记录 JSON 缺少 schemaVersion')
  }
  if (!Array.isArray(parsed.processes)) {
    throw new Error('生产批记录 JSON 缺少 processes')
  }
  return parsed
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

const formatParameterRange = (parameter?: BatchRecordTotalRecognitionParameter) => {
  if (parameter?.referenceValue) {
    return parameter.referenceValue
  }
  const min = formatValue(parameter?.ui?.min)
  const max = formatValue(parameter?.ui?.max)
  const unit = formatValue(parameter?.ui?.unit)
  if (min !== '-' && max !== '-') {
    return `${min}-${max}${unit === '-' ? '' : unit}`
  }
  if (min !== '-') {
    return `≥${min}${unit === '-' ? '' : unit}`
  }
  if (max !== '-') {
    return `≤${max}${unit === '-' ? '' : unit}`
  }
  return '-'
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

const renderParameterUiPreview = (group?: BatchRecordTotalRecognitionEquipmentGroup) => {
  return group?.parameters || []
}

const buildParameterPreviewValueKey = (
  processIndex: number,
  outputIndex: number,
  groupIndex: number,
  equipmentIndex: number,
  parameterIndex: number
) => {
  return `${processIndex}:${outputIndex}:${groupIndex}:${equipmentIndex}:${parameterIndex}`
}

const initializeParameterPreviewValues = (mapping: BatchRecordTotalRecognitionJson) => {
  Object.keys(parameterPreviewValues).forEach((key) => {
    delete parameterPreviewValues[key]
  })
  mapping.processes.forEach((process, processIndex) => {
    const outputIndexes = process.outputs?.length
      ? process.outputs.map((_, outputIndex) => outputIndex)
      : [-1]
    const equipmentGroups = process.equipmentGroups || []
    outputIndexes.forEach((outputIndex) => {
      equipmentGroups.forEach((group, groupIndex) => {
        const equipmentIndexes = group.equipmentOptions?.length
          ? group.equipmentOptions.map((_, equipmentIndex) => equipmentIndex)
          : [-1]
      const parameters = group.parameters || []
        equipmentIndexes.forEach((equipmentIndex) => {
          parameters.forEach((parameter, parameterIndex) => {
            const previewKey = buildParameterPreviewValueKey(
              processIndex,
              outputIndex,
              groupIndex,
              equipmentIndex,
              parameterIndex
            )
            parameterPreviewValues[previewKey] = parameter.ui?.defaultValue
          })
        })
      })
    })
  })
}

const getParameterPreviewValue = (node: BatchRecordTreeNode) => {
  const key = node.previewValueKey
  if (!key) {
    return node.parameter?.ui?.defaultValue
  }
  if (Object.prototype.hasOwnProperty.call(parameterPreviewValues, key)) {
    return parameterPreviewValues[key]
  }
  return node.parameter?.ui?.defaultValue
}

const setParameterPreviewValue = (node: BatchRecordTreeNode, value: unknown) => {
  if (!node.previewValueKey) {
    return
  }
  parameterPreviewValues[node.previewValueKey] = value
}

const getParameterDisplayName = (parameter?: BatchRecordTotalRecognitionParameter) => {
  const displayName = formatValue(parameter?.ui?.displayName)
  if (displayName !== '-') {
    return displayName
  }
  return formatValue(parameter?.name)
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

const isNumberParameterControl = (parameter?: BatchRecordTotalRecognitionParameter) => {
  return parameter?.ui?.control === 'number'
}

const isSelectParameterControl = (parameter?: BatchRecordTotalRecognitionParameter) => {
  return parameter?.ui?.control === 'select'
}

const isTextParameterControl = (parameter?: BatchRecordTotalRecognitionParameter) => {
  return parameter?.ui?.control === 'text' || parameter?.ui?.control === 'input'
}

const formatParameterTargetRange = (parameter?: BatchRecordTotalRecognitionParameter) => {
  const min = formatValue(parameter?.ui?.min)
  const max = formatValue(parameter?.ui?.max)
  const unit = formatValue(parameter?.ui?.unit)
  if (min !== '-' && max !== '-') {
    return `${min} - ${max}${unit === '-' ? '' : ` ${unit}`}`
  }
  return formatParameterRange(parameter)
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

const formatEquipmentGroupOptions = (group?: BatchRecordTotalRecognitionEquipmentGroup) => {
  return (group?.equipmentOptions || []).map((equipment) => formatNameCode(equipment)).join('、') || '-'
}

const buildMeta = (label: string, value: unknown): BatchRecordTreeMeta => ({
  label,
  value: formatValue(value)
})

const createEmptyTreeNode = (id: string, label: string): BatchRecordTreeNode => ({
  id,
  kind: 'empty',
  label,
  valueText: '-'
})

const isRecordValue = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

const buildUnknownValueTreeNode = (id: string, label: string, value: unknown): BatchRecordTreeNode => {
  if (Array.isArray(value)) {
    return {
      id,
      kind: 'field',
      label,
      meta: [buildMeta('数量', value.length)],
      children: value.length
        ? value.map((item, index) => buildUnknownValueTreeNode(`${id}-${index}`, `第 ${index + 1} 项`, item))
        : [createEmptyTreeNode(`${id}-empty`, '空数组')]
    }
  }
  if (isRecordValue(value)) {
    const entries = Object.entries(value)
    return {
      id,
      kind: 'field',
      label,
      children: entries.length
        ? entries.map(([key, childValue]) => buildUnknownValueTreeNode(`${id}-${key}`, key, childValue))
        : [createEmptyTreeNode(`${id}-empty`, '空对象')]
    }
  }
  return {
    id,
    kind: 'field',
    label,
    valueText: formatValue(value)
  }
}

const buildExtraFieldNodes = (
  source: unknown,
  knownKeys: string[],
  parentId: string
): BatchRecordTreeNode[] => {
  if (!isRecordValue(source)) {
    return []
  }
  const knownKeySet = new Set(knownKeys)
  return Object.entries(source)
    .filter(([key]) => !knownKeySet.has(key))
    .map(([key, value]) => buildUnknownValueTreeNode(`${parentId}-extra-${key}`, `额外字段：${key}`, value))
}

const buildBatchRecordTreeData = (
  result: ProductionBatchRecordParseResult,
  downloadName: string
): BatchRecordTreeNode[] => {
  const mapping = result.mapping
  return [
    {
      id: 'batch-record-recognition-root',
      kind: 'root',
      label: '生产批记录解析结果',
      meta: [
        buildMeta('解析类型', result.parseTypeName),
        buildMeta('源文件', result.sourceFileName),
        buildMeta('下载文件', downloadName),
        buildMeta('Schema版本', mapping.schemaVersion),
        buildMeta('工序数', mapping.processes.length)
      ],
      children: [
        {
          id: 'batch-record-product',
          kind: 'product',
          label: '产品',
          meta: [buildMeta('名称', mapping.product?.name), buildMeta('编号', mapping.product?.code)],
          children: buildExtraFieldNodes(mapping.product, ['name', 'code'], 'batch-record-product')
        },
        {
          id: 'batch-record-processes',
          kind: 'section',
          label: '工序',
          meta: [buildMeta('数量', mapping.processes.length)],
          children: mapping.processes.length
            ? mapping.processes.map((process, processIndex) => buildProcessTreeNode(process, processIndex))
            : [createEmptyTreeNode('batch-record-processes-empty', '无工序')]
        },
        ...buildExtraFieldNodes(mapping, ['product', 'schemaVersion', 'processes'], 'batch-record-root')
      ]
    }
  ]
}

const buildProcessTreeNode = (
  process: BatchRecordTotalRecognitionProcess,
  processIndex: number
): BatchRecordTreeNode => {
  const processId = `process-${processIndex}`
  const children: BatchRecordTreeNode[] = [
    {
      id: `${processId}-inputs`,
      kind: 'section',
      label: '输入物料',
      meta: [buildMeta('数量', process.inputs?.length || 0)],
      children: process.inputs?.length
        ? process.inputs.map((material, inputIndex) =>
            buildInputMaterialTreeNode(material, processIndex, inputIndex)
          )
        : [createEmptyTreeNode(`${processId}-inputs-empty`, '无输入物料')]
    },
    {
      id: `${processId}-outputs`,
      kind: 'section',
      label: '输出物料',
      meta: [buildMeta('数量', process.outputs?.length || 0)],
      children: process.outputs?.length
        ? process.outputs.map((material, outputIndex) =>
            buildOutputMaterialTreeNode(material, process, processIndex, outputIndex)
          )
        : [createEmptyTreeNode(`${processId}-outputs-empty`, '无输出物料')]
    }
  ]
  if (!process.outputs?.length) {
    children.push({
      id: `${processId}-equipment-without-output`,
      kind: 'section',
      label: '设备',
      meta: [buildMeta('设备组数', process.equipmentGroups?.length || 0)],
      children: buildEquipmentDeviceTreeNodes(processIndex, -1, process.equipmentGroups || [])
    })
  }
  children.push(...buildExtraFieldNodes(process, ['name', 'criticalProcess', 'inputs', 'outputs', 'equipmentGroups'], processId))
  return {
    id: processId,
    kind: 'process',
    label: `工序：${formatValue(process.name)}`,
    meta: [
      buildMeta('关键/特殊工序', process.criticalProcess ? '是' : '否'),
      buildMeta('投入物料数', process.inputs?.length || 0),
      buildMeta('产出物料数', process.outputs?.length || 0),
      buildMeta('设备组数', process.equipmentGroups?.length || 0)
    ],
    children
  }
}

const buildInputMaterialTreeNode = (
  material: BatchRecordTotalRecognitionMaterial,
  processIndex: number,
  inputIndex: number
): BatchRecordTreeNode => {
  const materialId = `process-${processIndex}-input-${inputIndex}`
  return {
    id: materialId,
    kind: 'material',
    label: `输入物料：${formatNameCode(material)}`,
    meta: [
      buildMeta('物料名称(编号)', formatNameCode(material)),
      buildMeta('名称', material.name),
      buildMeta('编号', material.code),
      buildMeta('来源标记', material.sourceCodeLabel)
    ],
    children: buildExtraFieldNodes(material, ['name', 'code', 'sourceCodeLabel'], materialId)
  }
}

const buildOutputMaterialTreeNode = (
  material: BatchRecordTotalRecognitionMaterial,
  process: BatchRecordTotalRecognitionProcess,
  processIndex: number,
  outputIndex: number
): BatchRecordTreeNode => {
  const materialId = `process-${processIndex}-output-${outputIndex}`
  return {
    id: materialId,
    kind: 'material',
    label: `输出物料：${formatNameCode(material)}`,
    meta: [
      buildMeta('物料名称(编号)', formatNameCode(material)),
      buildMeta('名称', material.name),
      buildMeta('编号', material.code),
      buildMeta('来源标记', material.sourceCodeLabel)
    ],
    children: [
      ...buildEquipmentDeviceTreeNodes(processIndex, outputIndex, process.equipmentGroups || []),
      ...buildExtraFieldNodes(material, ['name', 'code', 'sourceCodeLabel'], materialId)
    ]
  }
}

const buildEquipmentDeviceTreeNodes = (
  processIndex: number,
  outputIndex: number,
  equipmentGroups: BatchRecordTotalRecognitionEquipmentGroup[]
): BatchRecordTreeNode[] => {
  if (!equipmentGroups.length) {
    return [createEmptyTreeNode(`process-${processIndex}-output-${outputIndex}-equipment-empty`, '无设备')]
  }
  return equipmentGroups.flatMap((group, groupIndex) => {
    const equipmentOptions = group.equipmentOptions?.length ? group.equipmentOptions : [undefined]
    const parameters = renderParameterUiPreview(group)
    return equipmentOptions.map((equipment, equipmentIndex) => {
      const equipmentId = `process-${processIndex}-output-${outputIndex}-group-${groupIndex}-equipment-${equipmentIndex}`
      const equipmentLabel = equipment ? `设备：${formatNameCode(equipment)}` : `设备组 ${groupIndex + 1}`
      return {
        id: equipmentId,
        kind: 'equipment',
        label: equipmentLabel,
        meta: [
          buildMeta('设备名称(编号)', equipment ? formatNameCode(equipment) : formatEquipmentGroupOptions(group)),
          buildMeta('设备组', groupIndex + 1),
          buildMeta('选择模式', formatSelectionMode(group.selectionMode)),
          buildMeta('参数数', parameters.length)
        ],
        children: [
          ...(parameters.length
            ? parameters.map((parameter, parameterIndex) =>
                buildParameterTreeNode(
                  parameter,
                  processIndex,
                  outputIndex,
                  groupIndex,
                  equipmentIndex,
                  parameterIndex
                )
              )
            : [createEmptyTreeNode(`${equipmentId}-parameters-empty`, '无设备参数')]),
          ...(equipment
            ? buildExtraFieldNodes(equipment, ['name', 'code', 'sourceCodeLabel'], equipmentId)
            : []),
          ...buildExtraFieldNodes(group, ['equipmentOptions', 'parameters', 'selectionMode'], equipmentId)
        ]
      }
    })
  })
}

const buildParameterTreeNode = (
  parameter: BatchRecordTotalRecognitionParameter,
  processIndex: number,
  outputIndex: number,
  groupIndex: number,
  equipmentIndex: number,
  parameterIndex: number
): BatchRecordTreeNode => {
  const parameterId = `process-${processIndex}-output-${outputIndex}-group-${groupIndex}-equipment-${equipmentIndex}-parameter-${parameterIndex}`
  const ui = parameter.ui
  return {
    id: parameterId,
    kind: 'parameter',
    label: `设备参数：${getParameterDisplayName(parameter)}`,
    parameter,
    previewValueKey: buildParameterPreviewValueKey(
      processIndex,
      outputIndex,
      groupIndex,
      equipmentIndex,
      parameterIndex
    ),
    meta: [
      buildMeta('参数名称', parameter.name),
      buildMeta('参考值', parameter.referenceValue),
      buildMeta('实际值', parameter.actualValue),
      buildMeta('控件', ui?.control),
      buildMeta('默认值', ui?.defaultValue),
      buildMeta('最小值', ui?.min),
      buildMeta('最大值', ui?.max),
      buildMeta('步长', ui?.step),
      buildMeta('单位', ui?.unit),
      buildMeta('显示名', ui?.displayName),
      buildMeta('可选项', ui?.options)
    ],
    children: [
      ...buildExtraFieldNodes(parameter, ['name', 'referenceValue', 'actualValue', 'ui'], parameterId),
      ...buildExtraFieldNodes(
        ui,
        ['control', 'defaultValue', 'step', 'min', 'max', 'unit', 'displayName', 'options'],
        `${parameterId}-ui`
      )
    ]
  }
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

.form-parser-result {
  display: flex;
  flex-direction: column;
}

.form-parser-tree {
  width: 100%;
  padding: 8px 0;
  background: #fff;
}

.form-parser-tree-node {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
  padding: 6px 0;
}

.form-parser-tree-node__line {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 8px;
}

.form-parser-tree-node__label {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
}

.form-parser-tree-node__value {
  color: #6b7280;
  font-size: 13px;
  line-height: 20px;
}

.form-parser-tree-node__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.form-parser-tree-node__meta-item {
  display: inline-flex;
  max-width: 100%;
  padding: 2px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  line-height: 18px;
  word-break: break-word;
}

.form-parser-tree-node--root > .form-parser-tree-node__line .form-parser-tree-node__label,
.form-parser-tree-node--process > .form-parser-tree-node__line .form-parser-tree-node__label {
  color: #111827;
  font-size: 14px;
}

.form-parser-tree-node--parameter {
  padding: 10px 0;
}

.form-parser-tree-parameter-ui {
  display: grid;
  align-items: center;
  grid-template-columns: minmax(140px, 220px) minmax(260px, 420px);
  gap: 10px;
  max-width: 720px;
  padding: 10px;
  border: 1px solid #d7e2db;
  border-radius: 8px;
  background: #f7faf8;
}

.form-parser-tree-parameter-ui__label {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  line-height: 18px;
}

.form-parser-tree-parameter-ui__range {
  color: #315c52;
  font-size: 12px;
  font-weight: 600;
  line-height: 16px;
}

.form-parser-tree-parameter-ui__control {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;

  :deep(.el-input-number) {
    width: 210px;
  }

  :deep(.el-select),
  :deep(.el-input) {
    width: 280px;
    max-width: 100%;
  }
}

.form-parser-tree-parameter-ui__unit {
  flex: 0 0 auto;
  color: #111827;
  font-size: 13px;
  font-weight: 700;
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

  .form-parser-tree-parameter-ui {
    grid-template-columns: 1fr;
  }
}
</style>
