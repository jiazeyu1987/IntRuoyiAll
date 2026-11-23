<template>
  <el-button @click="open">批量导入 Excel</el-button>
  <el-dialog v-model="visible" title="批量导入注册证" width="540px" append-to-body destroy-on-close
    :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy">
    <p>选择注册证信息表（.xlsx）。国内注册证、CE 证书和 FDA 证书将作为历史导入记录显示在注册证列表，公司证件跳过。</p>
    <input v-if="!result" type="file" accept=".xlsx" :disabled="busy" @change="selectFile" />
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-12px" />
    <template v-if="result">
      <p v-for="(count, sheet) in result.sheetCounts" :key="sheet">{{ sheet }}：{{ count }} 条</p>
      <strong>新增 {{ result.inserted }} 条，已导入 {{ result.existing }} 条。</strong>
    </template>
    <template #footer>
      <el-button :disabled="busy" @click="visible = false">{{ result ? '关闭' : '取消' }}</el-button>
      <el-button v-if="!result" type="primary" :disabled="!file" :loading="busy" @click="submit">确认导入</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { importRegistrationWorkbook, type WorkbookImportResult } from '@/api/dcc/registrationCertificate/historicalImport'

const emit = defineEmits<{ imported: [] }>()
const visible = ref(false)
const busy = ref(false)
const file = ref<File>()
const result = ref<WorkbookImportResult>()
const error = ref('')

function open() {
  file.value = undefined
  result.value = undefined
  error.value = ''
  visible.value = true
}

function selectFile(event: Event) {
  file.value = (event.target as HTMLInputElement).files?.[0]
  error.value = ''
}

async function submit() {
  if (!file.value || busy.value) return
  busy.value = true
  error.value = ''
  try {
    result.value = await importRegistrationWorkbook(file.value)
    emit('imported')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    busy.value = false
  }
}
</script>
