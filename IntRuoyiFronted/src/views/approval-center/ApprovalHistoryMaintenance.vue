<template>
  <el-button v-if="checkRole(['super_admin'])" v-hasPermi="['bpm:task:query']" @click="open">
    历史归属核验
  </el-button>
  <el-dialog
    v-model="visible"
    title="审批历史归属核验与修复"
    width="90%"
    :close-on-click-modal="false"
  >
    <el-alert
      title="只依据原始业务来源、提交版本及流程证据修复。存在任何阻塞时禁止执行；不会修改历史来源或推断归属。服务须由部署方显式启用。"
      type="warning"
      :closable="false"
      show-icon
    />
    <el-form label-width="100px" class="mt-4" @submit.prevent="inspect">
      <el-form-item label="核验批次">
        <el-input
          v-model="batchId"
          :disabled="busy"
          maxlength="48"
          placeholder="输入可追溯批次编号（英文、数字、下划线、短横线）"
          @input="invalidate"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="busy" @click="inspect">核验当前租户</el-button>
        <el-button
          v-hasPermi="['bpm:task:update']"
          type="danger"
          :disabled="!canExecute"
          :loading="busy"
          @click="execute"
        >
          执行已核验修复
        </el-button>
      </el-form-item>
    </el-form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <template v-if="report">
      <el-descriptions :column="3" border class="mb-4">
        <el-descriptions-item label="租户 / 批次"
          >{{ report.tenantId }} / {{ report.batchId }}</el-descriptions-item
        >
        <el-descriptions-item label="核验结论">{{
          statusLabel(report.status)
        }}</el-descriptions-item>
        <el-descriptions-item label="流程数 / 阻塞数"
          >{{ report.findings.length }} / {{ blockedCount }}</el-descriptions-item
        >
        <el-descriptions-item label="运行任务 / 历史任务"
          >{{ report.runtimeTasks }} / {{ report.historicTasks }}</el-descriptions-item
        >
        <el-descriptions-item label="发起实例 / 抄送"
          >{{ report.initiatedInstances }} / {{ report.copies }}</el-descriptions-item
        >
        <el-descriptions-item label="修复时间">{{
          report.migratedAt || '尚未执行'
        }}</el-descriptions-item>
        <el-descriptions-item label="核验摘要" :span="3">{{
          report.inspectionDigest
        }}</el-descriptions-item>
      </el-descriptions>
      <el-table
        :data="report.findings"
        row-key="processId"
        border
        max-height="460"
        empty-text="核验范围内无历史流程"
      >
        <el-table-column prop="processId" label="流程 ID" min-width="290" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="证据结论 / 阻塞原因（原码）" min-width="340" />
        <el-table-column label="已核实来源 / 版本" min-width="240">
          <template #default="{ row }">
            <template v-if="row.authority">
              <div>{{ row.authority.authorityType }} / {{ row.authority.authorityId }}</div>
              <div>{{ row.authority.authorityVersion }}</div>
              <div>{{ row.authority.writerCode }}</div>
              <div>证据摘要：{{ row.sourceProof }}</div>
            </template>
            <span v-else>原始证据未通过核验</span>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { checkRole } from '@/utils/permission'
import {
  executeApprovalHistory,
  inspectApprovalHistory,
  type HistoryFinding,
  type HistoryReport
} from '@/api/approval-center/historyBackfill'

const emit = defineEmits<{ repaired: [] }>()
const visible = ref(false)
const busy = ref(false)
const batchId = ref('')
const report = ref<HistoryReport>()
const error = ref('')
const blockedCount = computed(
  () => report.value?.findings.filter((f) => f.status === 'BLOCKED').length ?? 0
)
const canExecute = computed(
  () =>
    !busy.value &&
    report.value?.status === 'READY' &&
    report.value.batchId === batchId.value &&
    blockedCount.value === 0 &&
    report.value.findings.some((f) => f.status === 'INSERT')
)
const statusLabel = (status: HistoryReport['status'] | HistoryFinding['status']) =>
  ({
    BLOCKED: '证据不足，已阻塞',
    READY: '核验完成',
    APPLIED: '已修复',
    INSERT: '可修复',
    EXISTING: '已存在且一致'
  })[status]
const invalidate = () => {
  report.value = undefined
  error.value = ''
}
const open = () => {
  invalidate()
  visible.value = true
}
const failure = (cause: unknown) => {
  error.value = cause instanceof Error ? cause.message : String(cause)
}
const inspect = async () => {
  invalidate()
  if (!/^[A-Za-z0-9][A-Za-z0-9_-]{0,47}$/.test(batchId.value)) {
    error.value = '批次编号须以英文或数字开头，仅允许英文、数字、下划线、短横线，长度 1 至 48 位。'
    return
  }
  busy.value = true
  try {
    report.value = await inspectApprovalHistory(batchId.value)
  } catch (cause) {
    failure(cause)
  } finally {
    busy.value = false
  }
}
const execute = async () => {
  if (!canExecute.value || !report.value) return
  const inspected = report.value
  // Lock the inspected batch while the operator reviews the confirmation.
  busy.value = true
  try {
    await ElMessageBox.confirm(
      `将修复当前租户 ${inspected.tenantId} 的 ${inspected.findings.filter((f) => f.status === 'INSERT').length} 条历史归属，并保存审计记录。批次：${inspected.batchId}。`,
      '确认执行历史归属修复',
      { type: 'warning', confirmButtonText: '确认修复', cancelButtonText: '取消' }
    )
  } catch (cause) {
    busy.value = false
    if (cause !== 'cancel' && cause !== 'close') failure(cause)
    return
  }
  error.value = ''
  try {
    report.value = await executeApprovalHistory(inspected.batchId, inspected.inspectionDigest)
    ElMessage.success('历史归属修复完成，请刷新待办核验结果')
    emit('repaired')
  } catch (cause) {
    report.value = undefined // A failed/stale execution requires a fresh inspection.
    failure(cause)
  } finally {
    busy.value = false
  }
}
</script>
