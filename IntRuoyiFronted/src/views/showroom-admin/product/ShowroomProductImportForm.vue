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
import { computed } from 'vue'
import { showImportResult } from '@/utils/importResult'

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
  const failureCount = result.failureCount + (result.awardFailureCount ?? 0)
  showImportResult({
    title: failureCount ? '产品资料导入存在失败记录' : '产品资料导入完成',
    summary: `成功发布 ${result.successCount} 个产品，跳过 ${result.skippedCount} 个无变化产品，${result.failureCount} 个产品处理失败。`,
    warning: failureCount > 0 || (result.awardWarnings?.length ?? 0) > 0,
    stats: [
      { label: '产品总行数', value: result.totalRows },
      { label: '成功发布产品', value: result.successCount },
      { label: '跳过无变化产品', value: result.skippedCount },
      { label: '产品处理失败', value: result.failureCount },
      { label: '奖项总行数', value: result.awardTotalRows ?? 0 },
      { label: '成功发布奖项', value: result.awardSuccessCount ?? 0 },
      { label: '奖项处理失败', value: result.awardFailureCount ?? 0 }
    ],
    guidance: failureCount ? '请根据下方行号和失败原因修改文件，仅重新导入需要处理的记录。' : undefined,
    sections: [
      { title: '产品失败记录', columns: ['文件行号', '产品编码', '失败原因'], rows: result.failures.map((failure) => [String(failure.rowNo), failure.productCode || '未填编码', failure.reason]), expanded: true },
      { title: '奖项失败记录', columns: ['文件行号', '奖项编码', '失败原因'], rows: (result.awardFailures ?? []).map((failure) => [String(failure.rowNo), failure.awardCode || '未填编码', failure.reason]), expanded: true },
      { title: '奖项处理提示', columns: ['提示内容'], rows: (result.awardWarnings ?? []).map((warning) => [warning]), expanded: true },
      { title: '成功发布的产品', columns: ['产品编码'], rows: result.successProductCodes.map((code) => [code]) },
      { title: '跳过的产品', columns: ['产品编码'], rows: result.skippedProductCodes.map((code) => [code]) },
      { title: '成功发布的奖项', columns: ['奖项编码'], rows: (result.successAwardCodes ?? []).map((code) => [code]) }
    ]
  })
  dialogVisible.value = false
  emits('success')
}

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
