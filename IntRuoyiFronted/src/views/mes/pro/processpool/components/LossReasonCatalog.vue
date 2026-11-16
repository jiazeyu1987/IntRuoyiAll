<template>
  <section data-loss-reason-catalog>
    <el-form inline @submit.prevent="load">
      <el-form-item label="编码 / 描述">
        <el-input
          v-model="keyword"
          clearable
          placeholder="请输入编码或损耗原因描述"
          @keyup.enter="load"
        />
      </el-form-item>
      <el-form-item><el-button :loading="loading" @click="load">查询</el-button></el-form-item>
      <el-form-item>
        <el-button
          v-hasPermi="['mes:pro-process-pool-team-leader:maintain']"
          type="primary"
          @click="createVisible = true"
          >新增</el-button
        >
        <el-button
          v-hasPermi="['mes:pro-process-pool-team-leader:maintain']"
          @click="importVisible = true"
          >导入</el-button
        >
        <el-button
          v-hasPermi="['mes:pro-process-pool-team-leader:query']"
          :loading="exporting"
          @click="handleExport"
          >导出</el-button
        >
      </el-form-item>
    </el-form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mb-12px" />
    <el-table v-loading="loading" :data="pagedRows" row-key="id" border stripe max-height="650">
      <el-table-column prop="reasonCode" label="损耗原因编码" width="180" />
      <el-table-column
        prop="reasonName"
        label="损耗原因描述"
        min-width="280"
        show-overflow-tooltip
      />
      <el-table-column prop="createTime" label="创建时间" width="210" :formatter="dateFormatter" />
    </el-table>
    <Pagination v-model:page="page" v-model:limit="pageSize" :total="rows.length" />
    <Dialog
      v-model="createVisible"
      title="新增损耗原因"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form @submit.prevent="handleCreate">
        <el-form-item label="损耗原因描述" required>
          <el-input
            v-model="description"
            maxlength="128"
            show-word-limit
            placeholder="请输入损耗原因描述"
            :disabled="saving"
          />
        </el-form-item>
      </el-form>
      <el-alert v-if="createError" :title="createError" type="error" :closable="false" />
      <template #footer>
        <el-button :disabled="saving" @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">新增</el-button>
      </template>
    </Dialog>
    <Dialog
      v-model="importVisible"
      title="导入损耗原因"
      width="560px"
      :close-on-click-modal="false"
    >
      <p>仅新增损耗原因，编码自动生成；如有错误，整批不导入。模板只需填写损耗原因描述。</p>
      <el-button :loading="templateLoading" @click="handleTemplate">下载导入模板</el-button>
      <input
        class="mt-16px"
        type="file"
        accept=".xlsx"
        aria-label="损耗原因导入文件"
        :disabled="importing"
        @change="selectFile"
      />
      <el-alert
        v-if="importError"
        :title="importError"
        type="error"
        :closable="false"
        class="mt-12px"
      />
      <template #footer>
        <el-button :disabled="importing" @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" :disabled="!file" @click="handleImport"
          >导入</el-button
        >
      </template>
    </Dialog>
  </section>
</template>
<script setup lang="ts">
import * as Api from '@/api/mes/pro/lossreason'
import download from '@/utils/download'
import { dateFormatter } from '@/utils/formatTime'
const message = useMessage()
const rows = ref<Api.LossReason[]>([])
const keyword = ref('')
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const error = ref('')
const createVisible = ref(false)
const description = ref('')
const createError = ref('')
const saving = ref(false)
const importVisible = ref(false)
const file = ref<File>()
const importing = ref(false)
const importError = ref('')
const exporting = ref(false)
const templateLoading = ref(false)
const pagedRows = computed(() =>
  rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value)
)
const errorText = (e: any) => e?.message || e?.msg || String(e)
const load = async () => {
  loading.value = true
  error.value = ''
  try {
    rows.value = await Api.listLossReasons(keyword.value.trim())
    page.value = 1
  } catch (e) {
    error.value = errorText(e)
  } finally {
    loading.value = false
  }
}
const handleCreate = async () => {
  if (saving.value) return
  if (!description.value.trim()) {
    createError.value = '请输入损耗原因描述'
    return
  }
  saving.value = true
  createError.value = ''
  try {
    await Api.createLossReason(description.value.trim())
    createVisible.value = false
    description.value = ''
    message.success('损耗原因已新增')
    await load()
  } catch (e) {
    createError.value = errorText(e)
  } finally {
    saving.value = false
  }
}
const selectFile = (event: Event) => {
  file.value = (event.target as HTMLInputElement).files?.[0]
  importError.value = ''
}
const handleImport = async () => {
  if (!file.value || importing.value) return
  importing.value = true
  importError.value = ''
  try {
    const data = new FormData()
    data.append('file', file.value)
    const count = await Api.importLossReasons(data)
    importVisible.value = false
    file.value = undefined
    message.success(`成功导入${count}条损耗原因`)
    await load()
  } catch (e) {
    importError.value = errorText(e)
  } finally {
    importing.value = false
  }
}
const handleTemplate = async () => {
  templateLoading.value = true
  try {
    download.excel(await Api.downloadLossReasonTemplate(), '损耗原因导入模板.xlsx')
  } catch (e) {
    importError.value = errorText(e)
  } finally {
    templateLoading.value = false
  }
}
const handleExport = async () => {
  exporting.value = true
  try {
    download.excel(await Api.exportLossReasons(keyword.value.trim()), '损耗原因.xlsx')
  } catch (e) {
    error.value = errorText(e)
  } finally {
    exporting.value = false
  }
}
onMounted(load)
</script>
