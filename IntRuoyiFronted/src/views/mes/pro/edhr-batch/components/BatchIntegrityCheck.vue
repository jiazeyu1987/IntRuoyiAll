<template>
  <el-button v-hasPermi="['mes:pro-edhr-batch-execution:query']" @click="visible = true">
    完整性校验
  </el-button>
  <el-dialog v-model="visible" title="校验全部可见历史批次" width="780px" :close-on-click-modal="false" @closed="cancel">
    <el-alert title="检查全部可见历史批次的人员、设备、物料证据，不受当前搜索条件影响。取消后当前分块处理完即停止，不再发起下一块。" type="info" :closable="false" />
    <p role="status">{{ statusLabel }} · 已检查 {{ checked }} 个批次 · 异常 {{ errors.length }} 个批次</p>
    <el-alert v-if="failure" :title="failure" type="error" :closable="false" />
    <el-table :data="errors" max-height="360" border>
      <el-table-column prop="batchExecutionCode" label="批次" min-width="160" />
      <el-table-column prop="message" label="证据异常" min-width="420" />
    </el-table>
    <template #footer>
      <el-button v-if="running" :disabled="cancelRequested" @click="cancel">{{ cancelRequested ? '正在停止' : '取消校验' }}</el-button>
      <el-button type="primary" :disabled="running" @click="start">{{ status === 'IDLE' ? '开始校验' : '重新校验' }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { checkIntegrityChunk, type IntegrityError } from '@/api/mes/pro/edhr/integrity'
const visible = ref(false)
const running = ref(false)
const cancelRequested = ref(false)
const checked = ref(0)
const errors = ref<IntegrityError[]>([])
const failure = ref('')
const status = ref<'IDLE' | 'RUNNING' | 'COMPLETED' | 'CANCELLED' | 'FAILED'>('IDLE')
const statusLabel = computed(() => ({ IDLE: '尚未开始', RUNNING: '正在校验', COMPLETED: '校验完成', CANCELLED: '已取消（结果不完整）', FAILED: '校验失败（结果不完整）' })[status.value])
const cancel = () => { cancelRequested.value = true }
onBeforeUnmount(cancel)
const start = async () => {
  if (running.value) return
  running.value = true
  cancelRequested.value = false
  checked.value = 0
  errors.value = []
  failure.value = ''
  status.value = 'RUNNING'
  let afterId: number | undefined
  let upperId: number | undefined
  try {
    while (!cancelRequested.value) {
      const result = await checkIntegrityChunk({ afterId, upperId })
      checked.value += result.checked
      errors.value.push(...result.errors)
      afterId = result.nextId
      upperId = result.upperId
      if (result.status === 'COMPLETED') {
        status.value = 'COMPLETED'
        return
      }
    }
    status.value = 'CANCELLED'
  } catch (error) {
    status.value = 'FAILED'
    failure.value = error instanceof Error ? error.message : String(error)
  } finally {
    running.value = false
  }
}
</script>
