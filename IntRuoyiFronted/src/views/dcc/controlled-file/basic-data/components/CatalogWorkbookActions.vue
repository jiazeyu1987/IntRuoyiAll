<template>
  <el-button v-if="canImport" :disabled="busy" @click="dialogVisible = true">
    <Icon icon="ep:upload" class="mr-5px" />导入
  </el-button>
  <el-button v-if="canExport" :loading="exporting" :disabled="importing" @click="exportWorkbook">
    <Icon icon="ep:download" class="mr-5px" />导出
  </el-button>
  <el-dialog
    v-model="dialogVisible"
    title="导入 DCC 产品目录及绑定关系"
    width="600px"
    :close-on-click-modal="false"
    :close-on-press-escape="!importing"
    :show-close="!importing"
  >
    <el-alert
      type="info"
      :closable="false"
      title="请使用完整 Excel 工作簿。产品目录全租户共享，导入复用已有目录；项目、注册证、公司及绑定关系合并到当前租户，保留文件外数据。校验失败整批不保存。"
    />
    <p
      >包含产品目录、项目代码、注册证、关联公司、授权公司及绑定关系。注册证附件和审批记录不包含在内，不能通过导入改变正式状态。跨租户不迁移账号授权、人员及流程引用，保留目标已有引用。被引用的历史主数据保持原删除状态；快照及绑定明细按原数据迁移。</p
    >
    <input
      ref="fileInput"
      type="file"
      accept=".xlsx"
      :disabled="importing"
      aria-label="选择导入工作簿"
      @change="selectFile"
    />
    <template #footer>
      <el-button :disabled="importing" @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :disabled="!file" :loading="importing" @click="importWorkbook"
        >确认合并导入</el-button
      >
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import request from '@/config/axios'
import download from '@/utils/download'
import { checkPermi, checkRole } from '@/utils/permission'

const emit = defineEmits<{ success: [] }>()
const message = useMessage()
const exporting = ref(false)
const importing = ref(false)
const busy = computed(() => exporting.value || importing.value)
const dialogVisible = ref(false)
const file = ref<File>()
const fileInput = ref<HTMLInputElement>()
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
    checkRole(['super_admin', 'tenant_admin']) ||
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
const selectFile = (event: Event) => {
  file.value = undefined
  const selected = (event.target as HTMLInputElement).files?.[0]
  if (!selected) return
  if (!selected.name.toLowerCase().endsWith('.xlsx') || selected.size > 20 * 1024 * 1024) {
    message.error('请选择 20MB 以内的 .xlsx 文件')
    return
  }
  file.value = selected
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
      headersType: 'multipart/form-data'
    })
    message.success(
      `导入成功：新增 ${result.created} 条，更新 ${result.updated} 条（含绑定明细）${result.localReferences ? `；${result.localReferences} 项源租户人员或流程引用未迁移` : ''}`
    )
    dialogVisible.value = false
    file.value = undefined
    if (fileInput.value) fileInput.value.value = ''
    emit('success')
  } finally {
    importing.value = false
  }
}
</script>
