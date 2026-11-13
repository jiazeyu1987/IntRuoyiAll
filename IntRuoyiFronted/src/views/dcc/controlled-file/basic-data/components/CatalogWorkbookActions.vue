<template>
  <el-button v-if="canImport" :disabled="busy" @click="dialogVisible = true">
    <Icon icon="ep:upload" class="mr-5px" />{{ props.importLabel }}
  </el-button>
  <el-button
    v-if="!props.importOnly && canExport"
    :loading="exporting"
    :disabled="importing"
    @click="exportWorkbook"
  >
    <Icon icon="ep:download" class="mr-5px" />导出
  </el-button>
  <Dialog
    v-model="dialogVisible"
    title="DCC 产品目录导入"
    width="420"
    :close-on-click-modal="false"
    :close-on-press-escape="!importing"
    :before-close="beforeClose"
    @closed="resetFile"
  >
    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      :auto-upload="false"
      :limit="1"
      :on-change="selectFile"
      :on-exceed="handleExceed"
      accept=".xlsx"
      :disabled="importing"
      action="none"
      aria-label="选择导入工作簿"
      drag
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
    </el-upload>
    <template #footer>
      <el-button type="primary" :disabled="!file" :loading="importing" @click="importWorkbook"
        >确 定</el-button
      >
      <el-button :disabled="importing" @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import type { UploadInstance, UploadProps, UploadUserFile } from 'element-plus'
import request from '@/config/axios'
import download from '@/utils/download'
import { checkPermi, checkRole } from '@/utils/permission'

const props = withDefaults(defineProps<{ importLabel?: string; importOnly?: boolean }>(), {
  importLabel: '导入',
  importOnly: false
})
const emit = defineEmits<{ success: [] }>()
const message = useMessage()
const exporting = ref(false)
const importing = ref(false)
const busy = computed(() => exporting.value || importing.value)
const dialogVisible = ref(false)
const uploadRef = ref<UploadInstance>()
const fileList = ref<UploadUserFile[]>([])
const file = computed(() => fileList.value[0]?.raw)
const canExport = computed(
  () =>
    checkRole(['super_admin', 'tenant_admin']) ||
    [
      'mdm:product:query',
      'dcc:project-code:query',
      'mdm:enterprise:query',
      'mdm:company-scope:query',
      'dcc:registration-certificate:query-current'
    ].every((p) => checkPermi([p]))
)
const canImport = computed(
  () =>
    checkRole(['super_admin', 'tenant_admin', 'dcc_project_code_admin']) ||
    [
      'mdm:product:create',
      'mdm:product:update',
      'dcc:project-code:create',
      'dcc:project-code:update',
      'mdm:enterprise:create',
      'mdm:enterprise:update',
      'mdm:company-scope:create',
      'mdm:company-scope:update',
      'dcc:registration-certificate:config:update'
    ].every((p) => checkPermi([p]))
)
const resetFile = () => {
  fileList.value = []
  uploadRef.value?.clearFiles()
}
const beforeClose = (done: () => void) => {
  if (!importing.value) done()
}
const handleExceed: UploadProps['onExceed'] = () => {
  message.error('只能上传一个文件，请先移除已选文件')
}
const selectFile: UploadProps['onChange'] = (uploadFile) => {
  const selected = uploadFile.raw
  if (!selected) return
  if (!selected.name.toLowerCase().endsWith('.xlsx') || selected.size > 20 * 1024 * 1024) {
    message.error('请选择 20MB 以内的 .xlsx 文件')
    resetFile()
    return
  }
}
const exportWorkbook = async () => {
  exporting.value = true
  try {
    const data = await request.download({ url: '/dcc/product-catalog/workbook/export' })
    download.excel(data, 'DCC产品目录及绑定关系.xlsx')
  } finally {
    exporting.value = false
  }
}
const importWorkbook = async () => {
  if (!file.value || importing.value) return
  importing.value = true
  try {
    const data = new FormData()
    data.append('file', file.value)
    const result = await request.post<{
      created: number
      updated: number
      localReferences: number
    }>({
      url: '/dcc/product-catalog/workbook/import',
      data,
      timeout: 300000,
      headersType: 'multipart/form-data'
    })
    message.success(
      `导入成功：新增 ${result.created} 条，更新 ${result.updated} 条（含绑定明细）${result.localReferences ? `；${result.localReferences} 项源租户人员或流程引用未迁移` : ''}`
    )
    dialogVisible.value = false
    resetFile()
    emit('success')
  } finally {
    importing.value = false
  }
}
</script>
