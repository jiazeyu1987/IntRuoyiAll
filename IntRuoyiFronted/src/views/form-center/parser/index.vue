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
      <el-descriptions :column="2" border>
        <el-descriptions-item label="解析类型">{{ lastResult.parseTypeName }}</el-descriptions-item>
        <el-descriptions-item label="源文件">{{ lastResult.sourceFileName }}</el-descriptions-item>
        <el-descriptions-item label="产品">
          {{ lastResult.mapping.product?.name || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="产品编码">
          {{ lastResult.mapping.product?.code || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="Schema版本">
          {{ lastResult.mapping.schemaVersion }}
        </el-descriptions-item>
        <el-descriptions-item label="工序数">
          {{ lastResult.mapping.processes.length }}
        </el-descriptions-item>
        <el-descriptions-item label="下载文件">{{ lastDownloadName }}</el-descriptions-item>
      </el-descriptions>
      <el-collapse class="form-parser-json-collapse">
        <el-collapse-item title="完整 JSON" name="full-json">
          <pre class="form-parser-json">{{ formatJson(lastResult.mapping) }}</pre>
        </el-collapse-item>
      </el-collapse>
      <el-table
        class="form-parser-result__table"
        :data="lastResult.mapping.processes"
        border
        stripe
      >
        <el-table-column type="expand" width="48">
          <template #default="{ row, $index }">
            <div class="form-parser-detail">
              <section class="form-parser-detail__section">
                <div class="form-parser-detail__title">输入物料</div>
                <el-table
                  :data="row.inputs || []"
                  border
                  empty-text="无输入物料"
                  size="small"
                >
                  <el-table-column label="物料名称(编号)" min-width="220">
                    <template #default="{ row: material }">
                      {{ formatNameCode(material) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="名称" min-width="160" prop="name" />
                  <el-table-column label="编号" min-width="180">
                    <template #default="{ row: material }">
                      {{ formatValue(material.code) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="来源标记" width="110">
                    <template #default="{ row: material }">
                      {{ formatValue(material.sourceCodeLabel) }}
                    </template>
                  </el-table-column>
                </el-table>
              </section>

              <section class="form-parser-detail__section">
                <div class="form-parser-detail__title">输出物料</div>
                <el-table
                  :data="row.outputs || []"
                  border
                  empty-text="无输出物料"
                  size="small"
                >
                  <el-table-column label="物料名称(编号)" min-width="220">
                    <template #default="{ row: material }">
                      {{ formatNameCode(material) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="名称" min-width="160" prop="name" />
                  <el-table-column label="编号" min-width="180">
                    <template #default="{ row: material }">
                      {{ formatValue(material.code) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="来源标记" width="110">
                    <template #default="{ row: material }">
                      {{ formatValue(material.sourceCodeLabel) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="输出物料对应设备（工序级）" min-width="220">
                    <template #default>
                      {{ formatProcessEquipmentOptions(row) }}
                    </template>
                  </el-table-column>
                </el-table>
              </section>

              <section class="form-parser-detail__section">
                <div class="form-parser-detail__title">输出物料-设备-参数对应</div>
                <div class="form-parser-detail__hint">
                  JSON 未提供单个输出物料与设备的一对一字段；此处按同一工序下的设备组关联展示。
                </div>
                <el-table
                  :data="buildOutputEquipmentParameterRows(row)"
                  border
                  empty-text="无输出物料或设备参数"
                  size="small"
                >
                  <el-table-column label="输出物料名称(编号)" min-width="220">
                    <template #default="{ row: relation }">
                      {{ formatNameCode(relation.outputMaterial) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="设备名称(编号)" min-width="240">
                    <template #default="{ row: relation }">
                      {{ relation.equipmentNames }}
                    </template>
                  </el-table-column>
                  <el-table-column label="选择模式" width="100">
                    <template #default="{ row: relation }">
                      {{ formatSelectionMode(relation.selectionMode) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="参数名称" min-width="150">
                    <template #default="{ row: relation }">
                      {{ relation.parameter?.name || '无参数' }}
                    </template>
                  </el-table-column>
                  <el-table-column label="参数范围" min-width="140">
                    <template #default="{ row: relation }">
                      {{ formatParameterRange(relation.parameter) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="默认值" min-width="110">
                    <template #default="{ row: relation }">
                      {{ formatValue(relation.parameter?.ui?.defaultValue) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="单位" width="90">
                    <template #default="{ row: relation }">
                      {{ formatValue(relation.parameter?.ui?.unit) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="实际值" width="100">
                    <template #default="{ row: relation }">
                      {{ formatValue(relation.parameter?.actualValue) }}
                    </template>
                  </el-table-column>
                </el-table>
              </section>

              <section class="form-parser-detail__section">
                <div class="form-parser-detail__title">设备与参数</div>
                <div v-if="row.equipmentGroups?.length" class="form-parser-equipment-groups">
                  <div
                    v-for="(group, groupIndex) in row.equipmentGroups"
                    :key="`${row.name}-${groupIndex}`"
                    class="form-parser-equipment-group"
                  >
                    <div class="form-parser-equipment-group__head">
                      <span>设备组 {{ groupIndex + 1 }}</span>
                      <el-tag size="small" type="success">
                        选择模式：{{ formatSelectionMode(group.selectionMode) }}
                      </el-tag>
                    </div>
                    <el-table
                      :data="group.equipmentOptions || []"
                      border
                      empty-text="无设备"
                      size="small"
                    >
                      <el-table-column label="设备名称(编号)" min-width="220">
                        <template #default="{ row: equipment }">
                          {{ formatNameCode(equipment) }}
                        </template>
                      </el-table-column>
                      <el-table-column label="名称" min-width="160" prop="name" />
                      <el-table-column label="编号" min-width="160">
                        <template #default="{ row: equipment }">
                          {{ formatValue(equipment.code) }}
                        </template>
                      </el-table-column>
                    </el-table>
                    <el-table
                      :data="group.parameters || []"
                      border
                      empty-text="无参数"
                      size="small"
                    >
                      <el-table-column label="设备名称(编号)" min-width="220">
                        <template #default>
                          {{ formatEquipmentGroupOptions(group) }}
                        </template>
                      </el-table-column>
                      <el-table-column label="参数名称" min-width="150" prop="name" />
                      <el-table-column label="参数范围" min-width="140">
                        <template #default="{ row: parameter }">
                          {{ formatParameterRange(parameter) }}
                        </template>
                      </el-table-column>
                      <el-table-column label="默认值" min-width="120">
                        <template #default="{ row: parameter }">
                          {{ formatValue(parameter.ui?.defaultValue) }}
                        </template>
                      </el-table-column>
                      <el-table-column label="最小值" min-width="100">
                        <template #default="{ row: parameter }">
                          {{ formatValue(parameter.ui?.min) }}
                        </template>
                      </el-table-column>
                      <el-table-column label="最大值" min-width="100">
                        <template #default="{ row: parameter }">
                          {{ formatValue(parameter.ui?.max) }}
                        </template>
                      </el-table-column>
                      <el-table-column label="步长" min-width="90">
                        <template #default="{ row: parameter }">
                          {{ formatValue(parameter.ui?.step) }}
                        </template>
                      </el-table-column>
                      <el-table-column label="单位" min-width="90">
                        <template #default="{ row: parameter }">
                          {{ formatValue(parameter.ui?.unit) }}
                        </template>
                      </el-table-column>
                      <el-table-column label="控件" min-width="100">
                        <template #default="{ row: parameter }">
                          {{ formatValue(parameter.ui?.control) }}
                        </template>
                      </el-table-column>
                      <el-table-column label="显示名" min-width="110">
                        <template #default="{ row: parameter }">
                          {{ formatValue(parameter.ui?.displayName) }}
                        </template>
                      </el-table-column>
                      <el-table-column label="可选项" min-width="180">
                        <template #default="{ row: parameter }">
                          {{ formatValue(parameter.ui?.options) }}
                        </template>
                      </el-table-column>
                      <el-table-column label="实际值" min-width="100">
                        <template #default="{ row: parameter }">
                          {{ formatValue(parameter.actualValue) }}
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                </div>
                <el-empty v-else description="无设备与参数" :image-size="44" />
              </section>

              <el-collapse class="form-parser-json-collapse">
                <el-collapse-item title="工序 JSON" :name="`${row.name}-${$index}-json`">
                  <pre class="form-parser-json">{{ formatJson(row) }}</pre>
                </el-collapse-item>
              </el-collapse>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="工序名称" min-width="180" prop="name" />
        <el-table-column label="关键/特殊工序" width="130">
          <template #default="{ row }">
            <el-tag :type="row.criticalProcess ? 'danger' : 'info'">
              {{ row.criticalProcess ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投入物料数" width="120">
          <template #default="{ row }">{{ row.inputs?.length || 0 }}</template>
        </el-table-column>
        <el-table-column label="产出物料数" width="120">
          <template #default="{ row }">{{ row.outputs?.length || 0 }}</template>
        </el-table-column>
        <el-table-column label="设备组数" width="110">
          <template #default="{ row }">{{ row.equipmentGroups?.length || 0 }}</template>
        </el-table-column>
      </el-table>
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

interface BatchRecordOutputEquipmentParameterRow {
  outputMaterial: BatchRecordTotalRecognitionMaterial
  equipmentNames: string
  selectionMode?: string
  parameter?: BatchRecordTotalRecognitionParameter
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

const message = useMessage()
const uploadRef = ref<UploadInstance>()
const fileList = ref<UploadUserFile[]>([])
const productionLoading = ref(false)
const lastResult = ref<ProductionBatchRecordParseResult>()
const lastDownloadName = ref('')
const JSON_EXTENSION = '.json'

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

const formatEquipmentGroupOptions = (group?: BatchRecordTotalRecognitionEquipmentGroup) => {
  return (group?.equipmentOptions || []).map((equipment) => formatNameCode(equipment)).join('、') || '-'
}

const formatProcessEquipmentOptions = (process?: BatchRecordTotalRecognitionProcess) => {
  return (
    process?.equipmentGroups
      ?.flatMap((group) => group.equipmentOptions || [])
      .map((equipment) => formatNameCode(equipment))
      .join('、') || '-'
  )
}

const buildOutputEquipmentParameterRows = (
  process?: BatchRecordTotalRecognitionProcess
): BatchRecordOutputEquipmentParameterRow[] => {
  const outputs = process?.outputs || []
  const equipmentGroups = process?.equipmentGroups || []
  if (!outputs.length || !equipmentGroups.length) {
    return []
  }
  return outputs.flatMap((outputMaterial) =>
    equipmentGroups.flatMap((group) => {
      const parameters = group.parameters?.length ? group.parameters : [undefined]
      return parameters.map((parameter) => ({
        outputMaterial,
        equipmentNames: formatEquipmentGroupOptions(group),
        selectionMode: group.selectionMode,
        parameter
      }))
    })
  )
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

const formatJson = (value: unknown) => JSON.stringify(value, null, 2)

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
  gap: 16px;
}

.form-parser-result__table {
  width: 100%;

  :deep(.el-table__expanded-cell) {
    padding: 0;
  }
}

.form-parser-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  background: #f8fafc;
}

.form-parser-detail__section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-parser-detail__title {
  color: #111827;
  font-size: 14px;
  font-weight: 700;
  line-height: 22px;
}

.form-parser-equipment-groups {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-parser-equipment-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.form-parser-equipment-group__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #111827;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
}

.form-parser-json-collapse {
  :deep(.el-collapse-item__content) {
    padding-bottom: 0;
  }
}

.form-parser-json {
  max-height: 360px;
  margin: 0;
  overflow: auto;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #111827;
  color: #f9fafb;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

:deep(.el-table .cell) {
  word-break: break-word;
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

  .form-parser-equipment-group__head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
