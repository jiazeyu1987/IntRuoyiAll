<template>
  <ContentWrap>
    <div class="process-pool-fifo">
      <div class="process-pool-fifo__header">
        <div>
          <div class="process-pool-fifo__title">FIFO 编排</div>
          <div class="process-pool-fifo__subtitle">
            从资源池可用 OUTPUT 数量按生产工单计划开始时间分配。
          </div>
        </div>
      </div>

      <el-alert v-if="submitError" :title="submitError" type="error" :closable="false" show-icon />
      <el-alert
        v-if="allocationResult"
        :title="`FIFO 已分配数量：${allocationResult.totalAllocatedQuantity || 0}`"
        type="success"
        :closable="false"
        show-icon
      />

      <el-form class="process-pool-fifo__form" :model="fifoForm" label-width="170px" @submit.prevent>
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-form-item label="分配批次号">
              <el-input v-model="fifoForm.allocationBatchNo" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="来源工序ID">
              <el-input-number v-model="fifoForm.sourceProcessId" :min="1" :controls="false" class="process-pool-fifo__number" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="目标路线工序ID">
              <el-input-number v-model="fifoForm.targetRouteProcessId" :min="1" :controls="false" class="process-pool-fifo__number" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="目标工序ID">
              <el-input-number v-model="fifoForm.targetProcessId" :min="1" :controls="false" class="process-pool-fifo__number" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="目标生产工单ID列表">
          <el-input
            v-model="fifoForm.targetWorkOrderIdsText"
            type="textarea"
            :rows="4"
            placeholder="多个生产工单 ID 用英文逗号分隔，例如 101,102,103"
          />
        </el-form-item>

        <div class="process-pool-fifo__actions">
          <el-button type="primary" :loading="submitLoading" @click="handleAllocate">
            <Icon icon="ep:sort" class="mr-5px" />
            执行 FIFO 分配
          </el-button>
        </div>
      </el-form>

      <el-table
        v-if="allocationResult?.lines?.length"
        :data="allocationResult.lines"
        border
        class="process-pool-fifo__table"
      >
        <el-table-column prop="sourceQuantityFragmentId" label="来源数量片段" min-width="150" />
        <el-table-column prop="sourceEventId" label="来源事件" min-width="120" />
        <el-table-column prop="targetWorkOrderId" label="目标工单ID" min-width="130" />
        <el-table-column prop="targetWorkOrderCode" label="目标工单" min-width="160" />
        <el-table-column prop="allocatedQuantity" label="分配数量" min-width="120" />
        <el-table-column prop="allocationStatus" label="状态" min-width="120" />
      </el-table>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import {
  allocateAvailableProcessPoolOutput,
  type ProcessPoolFifoOrchestrationAllocateRespVO
} from '@/api/mes/pro/processpool/fifoOrchestration'

defineOptions({ name: 'MesProProcessPoolFifoOrchestration' })

const fifoForm = reactive({
  allocationBatchNo: `FIFO-${Date.now()}`,
  sourceProcessId: undefined as number | undefined,
  targetRouteProcessId: undefined as number | undefined,
  targetProcessId: undefined as number | undefined,
  targetWorkOrderIdsText: ''
})

const submitLoading = ref(false)
const submitError = ref('')
const allocationResult = ref<ProcessPoolFifoOrchestrationAllocateRespVO>()

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const requirePositiveNumber = (value: number | undefined, label: string) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`${label}必须大于 0`)
  }
  return parsed
}

const parseTargetWorkOrderIds = () => {
  const ids = fifoForm.targetWorkOrderIdsText
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item) && item > 0)
  if (!ids.length) {
    throw new Error('目标生产工单ID列表不能为空')
  }
  return ids
}

const handleAllocate = async () => {
  submitLoading.value = true
  submitError.value = ''
  allocationResult.value = undefined
  try {
    if (!fifoForm.allocationBatchNo.trim()) {
      throw new Error('分配批次号不能为空')
    }
    allocationResult.value = await allocateAvailableProcessPoolOutput({
      allocationBatchNo: fifoForm.allocationBatchNo.trim(),
      sourceProcessId: requirePositiveNumber(fifoForm.sourceProcessId, '来源工序ID'),
      targetRouteProcessId: requirePositiveNumber(fifoForm.targetRouteProcessId, '目标路线工序ID'),
      targetProcessId: requirePositiveNumber(fifoForm.targetProcessId, '目标工序ID'),
      targetWorkOrderIds: parseTargetWorkOrderIds()
    })
    ElMessage.success('FIFO 分配完成')
  } catch (error) {
    submitError.value = resolveErrorMessage(error, 'FIFO 分配失败')
    ElMessage.error(submitError.value)
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.process-pool-fifo {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.process-pool-fifo__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.process-pool-fifo__title {
  font-size: 18px;
  font-weight: 600;
  line-height: 28px;
  color: var(--el-text-color-primary);
}

.process-pool-fifo__subtitle {
  margin-top: 4px;
  font-size: 13px;
  line-height: 20px;
  color: var(--el-text-color-secondary);
}

.process-pool-fifo__form {
  max-width: 1100px;
}

.process-pool-fifo__number {
  width: 100%;
}

.process-pool-fifo__actions {
  display: flex;
  justify-content: flex-end;
}

.process-pool-fifo__table {
  margin-top: 8px;
}
</style>
