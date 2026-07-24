<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="420">
    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      :auto-upload="false"
      :disabled="formLoading"
      :limit="1"
      :on-exceed="handleExceed"
      accept=".zip,.xlsx,.xls"
      action="none"
      drag
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip text-center">
          <div v-for="line in dialogTips" :key="line" class="el-upload__tip">
            {{ line }}
          </div>
          <span>标准导入支持 zip 资源包，也兼容历史 xls、xlsx；模板/底表仍使用 xls、xlsx。</span>
          <el-link
            v-if="showTemplateDownload"
            :underline="false"
            style="font-size: 12px; vertical-align: baseline"
            type="primary"
            @click="downloadTemplate"
          >
            下载模板
          </el-link>
        </div>
      </template>
    </el-upload>
    <div class="showroom-product-import-form__same-action">
      <span class="showroom-product-import-form__same-action-label">相同产品处理</span>
      <el-radio-group v-model="sameProductAction" :disabled="formLoading">
        <el-radio-button value="SKIP">跳过</el-radio-button>
        <el-radio-button value="OVERWRITE">覆盖</el-radio-button>
      </el-radio-group>
    </div>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import {
  ShowroomAdminApi,
  type ShowroomProductImportMode,
  type ShowroomProductImportRespVO
} from '@/api/showroom-admin'
import download from '@/utils/download'
import type { UploadUserFile } from 'element-plus'
import { computed, h } from 'vue'

defineOptions({ name: 'ShowroomProductImportForm' })

const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const uploadRef = ref()
const fileList = ref<UploadUserFile[]>([])
type SameProductAction = 'SKIP' | 'OVERWRITE'
const sameProductAction = ref<SameProductAction>('SKIP')
const importMode = ref<ShowroomProductImportMode>('STANDARD')

const emits = defineEmits(['success'])

const dialogTitle = computed(() =>
  importMode.value === 'BASE_WORKBOOK' ? '产品更新底表导入' : '产品资源包 / Excel 导入'
)

const dialogTips = computed(() => {
  if (importMode.value === 'BASE_WORKBOOK') {
    return [
      '支持导入桌面文件“展厅讲解软件产品资料更新底表.xlsx”的当前底表结构。',
      '产品列表沿用 15 列表头；产品图可为空，缺图时会保留系统当前封面。',
      '奖项页读取 A-D 列；已有奖项缺少封面时保留当前封面，新奖项缺少封面会明确失败。'
    ]
  }
  return [
    '优先导入“导出”按钮生成的 zip 资源包；资源包内包含 product-data.xlsx、manifest.json 与讲解音频文件，导入后会直接发布。',
    '产品读取“产品名-中文”“卖点文案”“产品图”列；奖项仍读取 A-E 列基础信息和首图封面。',
    '“讲解音频”页签按目标类型/目标编码/语言回导产品与奖项的双语讲解和包内音频；“关键词中英对照”页签会同步导入关键词双语表。'
  ]
})

const showTemplateDownload = computed(() => importMode.value === 'STANDARD')

const open = async (mode: ShowroomProductImportMode = 'STANDARD') => {
  importMode.value = mode
  dialogVisible.value = true
  await resetForm()
}
defineExpose({ open })

const submitForm = async () => {
  if (fileList.value.length === 0) {
    message.error('请上传文件')
    return
  }

  formLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', fileList.value[0].raw as Blob)
    formData.append('sameProductAction', sameProductAction.value)
    const result = normalizeProductImportResult(
      importMode.value === 'BASE_WORKBOOK'
        ? await ShowroomAdminApi.importProductBaseWorkbook(formData)
        : await ShowroomAdminApi.importProductExcel(formData)
    )
    handleImportSuccess(result)
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    message.error(resolved.message || '导入失败，请稍后重试')
  } finally {
    formLoading.value = false
  }
}

type ProductImportResultPayload =
  | Partial<ShowroomProductImportRespVO>
  | {
      data?: Partial<ShowroomProductImportRespVO>
    }
  | null
  | undefined

const toNumber = (value: unknown) => (typeof value === 'number' && Number.isFinite(value) ? value : 0)

const normalizeProductImportResult = (payload: ProductImportResultPayload): ShowroomProductImportRespVO => {
  const payloadData =
    payload && typeof payload === 'object' && 'data' in payload && payload.data
      ? payload.data
      : payload
  const source =
    payloadData && typeof payloadData === 'object'
      ? (payloadData as Partial<ShowroomProductImportRespVO>)
      : {}
  return {
    totalRows: toNumber(source.totalRows),
    successCount: toNumber(source.successCount),
    skippedCount: toNumber(source.skippedCount),
    failureCount: toNumber(source.failureCount),
    successProductCodes: Array.isArray(source.successProductCodes) ? source.successProductCodes : [],
    skippedProductCodes: Array.isArray(source.skippedProductCodes) ? source.skippedProductCodes : [],
    failures: Array.isArray(source.failures) ? source.failures : [],
    awardTotalRows: toNumber(source.awardTotalRows),
    awardSuccessCount: toNumber(source.awardSuccessCount),
    awardFailureCount: toNumber(source.awardFailureCount),
    awardWarnings: Array.isArray(source.awardWarnings) ? source.awardWarnings : [],
    successAwardCodes: Array.isArray(source.successAwardCodes) ? source.successAwardCodes : [],
    awardFailures: Array.isArray(source.awardFailures) ? source.awardFailures : []
  }
}

const handleImportSuccess = (result: ShowroomProductImportRespVO) => {
  const textLines = [
    `总行数：${result.totalRows}`,
    `成功发布：${result.successCount}`,
    `跳过无变化：${result.skippedCount}`,
    `失败数量：${result.failureCount}`
  ]
  if (result.successProductCodes.length > 0) {
    textLines.push(`成功产品：${result.successProductCodes.join('、')}`)
  }
  if (result.awardTotalRows || result.awardSuccessCount || result.awardFailureCount) {
    textLines.push(
      `奖项总行数：${result.awardTotalRows || 0}`,
      `奖项成功发布：${result.awardSuccessCount || 0}`,
      `奖项失败数量：${result.awardFailureCount || 0}`
    )
  }
  if (result.successAwardCodes?.length) {
    textLines.push(`成功奖项：${result.successAwardCodes.join('、')}`)
  }
  if (result.skippedProductCodes.length > 0) {
    textLines.push(`跳过产品：${result.skippedProductCodes.join('、')}`)
  }
  if (result.awardWarnings?.length) {
    textLines.push(...result.awardWarnings.map((warning) => `奖项提示：${warning}`))
  }
  if (result.failures.length > 0) {
    textLines.push(
      `失败明细：${result.failures
        .map((failure) => `第${failure.rowNo}行 ${failure.productCode || '未填编码'}：${failure.reason}`)
        .join('；')}`
    )
  }
  if (result.awardFailures?.length) {
    textLines.push(
      `奖项失败明细：${result.awardFailures
        .map((failure) => `第${failure.rowNo}行 ${failure.awardCode || '未填编码'}：${failure.reason}`)
        .join('；')}`
    )
  }
  message.alert(renderImportResultLines(textLines))
  dialogVisible.value = false
  emits('success')
}

const renderImportResultLines = (textLines: string[]) =>
  h(
    'div',
    { class: 'showroom-product-import-result' },
    textLines.map((line) => h('div', { class: 'showroom-product-import-result__line' }, line))
  )

const resetForm = async () => {
  fileList.value = []
  sameProductAction.value = 'SKIP'
  formLoading.value = false
  await nextTick()
  uploadRef.value?.clearFiles()
}

const handleExceed = () => {
  message.error('最多只能上传一个文件！')
}

const downloadTemplate = async () => {
  const data = await ShowroomAdminApi.getProductImportTemplate()
  download.excel(data, '产品资料修改版-补充产品资料.xlsx')
}
</script>

<style scoped>
.showroom-product-import-form__same-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
}

.showroom-product-import-form__same-action-label {
  color: var(--el-text-color-regular);
  font-size: 14px;
}
</style>
