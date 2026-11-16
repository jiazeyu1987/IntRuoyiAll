<template>
  <el-button v-if="canExport" :loading="exporting" title="导出全部现行审批策略、依赖流程和审批角色定义，供新租户初始化；不含账号、人员授权和审批历史" @click="exportPolicies">
    <Icon icon="ep:download" class="mr-5px" />导出审批策略
  </el-button>
  <el-button v-if="canImport" :loading="importing" @click="fileInput?.click()">
    <Icon icon="ep:upload" class="mr-5px" />导入审批策略
  </el-button>
  <input ref="fileInput" type="file" accept=".json,application/json" hidden @change="readFile" />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { BusinessApprovalPolicyApi } from '@/api/bpm/businessApprovalPolicy'
import { checkPermi } from '@/utils/permission'
import { authContextRevision } from '@/utils/auth'
import download from '@/utils/download'
import { parsePolicyTransfer } from './policyTransfer'

const emit = defineEmits<{ imported: [] }>()
const message = useMessage()
const canExport = computed(() => ['bpm:business-approval-policy:query', 'bpm:model:query']
  .every(permission => checkPermi([permission])))
const canImport = computed(() => [
  'bpm:business-approval-policy:create', 'bpm:business-approval-policy:publish', 'bpm:business-approval-policy:disable',
  'bpm:model:create', 'bpm:model:deploy', 'bpm:category:create', 'system:role:create', 'system:role-category:create'
].every(permission => checkPermi([permission])))
const fileInput = ref<HTMLInputElement>()
const exporting = ref(false)
const importing = ref(false)

async function exportPolicies() {
  const revision = authContextRevision.value
  exporting.value = true
  try {
    const result = await BusinessApprovalPolicyApi.exportPolicies(() => revision === authContextRevision.value)
    if (revision !== authContextRevision.value) throw new Error('租户或登录状态已变更，请重新导出')
    download.json(new Blob([JSON.stringify(result, null, 2)]), '审批策略.json')
    message.success(`已导出${result.policies.length}条审批策略及${result.processes.length}个依赖流程`)
  } catch (cause) {
    message.error(cause instanceof Error ? cause.message : '审批策略导出失败')
  } finally {
    exporting.value = false
  }
}

async function readFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || importing.value) return
  importing.value = true
  const revision = authContextRevision.value
  try {
    const parsed = parsePolicyTransfer(await file.text())
    if (revision !== authContextRevision.value) throw new Error('租户或登录状态已变更，请重新选择文件')
    const count = await BusinessApprovalPolicyApi.importPolicies(parsed, () => revision === authContextRevision.value)
    if (revision !== authContextRevision.value) throw new Error('导入请求的租户状态已变更，请切回原租户核对结果')
    message.success(`已按原配置导入${count}条审批策略`)
    if (parsed.processes.some(process => process.roles.length > 0)) {
      message.warning('依赖流程已配置，请在角色管理中核对审批人员；新增审批角色尚未分配人员')
    }
    emit('imported')
  } catch (cause) {
    message.error(cause instanceof Error ? cause.message : '审批策略导入失败')
  } finally {
    importing.value = false
  }
}
</script>
