<template>
  <el-drawer v-model="drawerVisible" title="事故闭环" size="860px" class="ops-incident-drawer">
    <div class="incident-layout">
      <div class="incident-toolbar">
        <el-button type="primary" :loading="props.loading" @click="emit('refresh')">
          <Icon icon="ep:refresh" class="mr-4px" />
          刷新
        </el-button>
        <el-tag>共 {{ props.page?.total || 0 }} 条</el-tag>
      </div>
      <el-alert
        title="事故闭环只记录告警、操作记录、发布包、备份点、责任人与处理结果，不执行发布、备份、回滚或恢复命令。"
        type="warning"
        :closable="false"
      />

      <el-table
        :data="rows"
        height="260"
        size="small"
        empty-text="暂无事故"
        highlight-current-row
        v-loading="props.loading"
        @current-change="selectIncident"
      >
        <el-table-column label="时间" width="152">
          <template #default="{ row }">{{ formatRuntimeDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="环境" width="86">
          <template #default="{ row }">{{ environmentText(row.environment) }}</template>
        </el-table-column>
        <el-table-column label="动作" width="112">
          <template #default="{ row }">{{ actionText(row.action) }}</template>
        </el-table-column>
        <el-table-column label="级别" prop="severity" width="82" />
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <el-tag :type="opsTagType(row.status)">{{ opsStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
        <el-table-column label="剩余风险" prop="remainingRisk" min-width="168" show-overflow-tooltip />
      </el-table>

      <div class="incident-detail" v-if="selectedIncident">
        <div class="incident-detail__head">
          <div>
            <div class="incident-detail__title">{{ selectedIncident.title }}</div>
            <div class="incident-detail__meta">{{ selectedIncident.description }}</div>
          </div>
          <el-tag :type="opsTagType(selectedIncident.status)">
            {{ opsStatusText(selectedIncident.status) }}
          </el-tag>
        </div>

        <el-table
          :data="selectedIncident.actions || []"
          height="150"
          size="small"
          empty-text="暂无处置动作"
        >
          <el-table-column label="时间" width="152">
            <template #default="{ row }">{{ formatRuntimeDate(row.actedAt) }}</template>
          </el-table-column>
          <el-table-column label="动作" prop="action" min-width="130" show-overflow-tooltip />
          <el-table-column label="操作者" prop="operator" width="108" />
          <el-table-column label="验证" prop="verificationResult" width="96" />
          <el-table-column label="证据" prop="evidence" min-width="180" show-overflow-tooltip />
        </el-table>

        <el-form class="incident-form" label-width="96px">
          <el-form-item label="处置动作">
            <el-input v-model="actionForm.action" maxlength="80" />
          </el-form-item>
          <el-form-item label="验证结果">
            <el-input v-model="actionForm.verificationResult" maxlength="80" />
          </el-form-item>
          <el-form-item label="证据">
            <el-input v-model="actionForm.evidence" type="textarea" :rows="2" maxlength="240" />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="props.submitting"
              @click="emitRecordAction"
            >
              记录处置
            </el-button>
          </el-form-item>
        </el-form>

        <el-form class="incident-form incident-form--gate" label-width="96px">
          <el-form-item label="责任门禁">
            <el-input v-model="closeForm.ownerGateResult" maxlength="40" />
          </el-form-item>
          <el-form-item label="验证门禁">
            <el-input v-model="closeForm.verificationResult" maxlength="40" />
          </el-form-item>
          <el-form-item label="剩余风险">
            <el-input v-model="closeForm.remainingRisk" type="textarea" :rows="2" maxlength="240" />
          </el-form-item>
          <el-form-item label="复盘状态">
            <el-input v-model="closeForm.postmortemStatus" maxlength="40" />
          </el-form-item>
          <el-form-item label="关闭原因">
            <el-input v-model="closeForm.closeReason" type="textarea" :rows="2" maxlength="240" />
          </el-form-item>
          <el-form-item>
            <el-button
              type="danger"
              :disabled="selectedIncident.status === 'CLOSED'"
              :loading="props.submitting"
              @click="emitCloseIncident"
            >
              关闭事故
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-form class="incident-form" label-width="96px">
        <div class="incident-form__title">新建事故</div>
        <el-form-item label="环境">
          <el-select v-model="createForm.environment">
            <el-option label="Local" value="local" />
            <el-option label="Test" value="test" />
            <el-option label="Production" value="prod" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作">
          <el-input v-model="createForm.action" maxlength="80" />
        </el-form-item>
        <el-form-item label="级别">
          <el-input v-model="createForm.severity" maxlength="40" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="createForm.title" maxlength="120" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="2" maxlength="240" />
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="createForm.sourceType">
            <el-option label="DIRECT" value="DIRECT" />
            <el-option label="ALERT" value="ALERT" />
            <el-option label="HIGH_RISK_OPERATION" value="HIGH_RISK_OPERATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源编号">
          <el-input v-model="createForm.sourceId" maxlength="80" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="props.submitting" @click="emitCreateIncident">
            新建事故
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type {
  RuntimeControlIncidentActionReqVO,
  RuntimeControlIncidentCloseReqVO,
  RuntimeControlIncidentCreateReqVO,
  RuntimeControlIncidentVO
} from '@/api/infra/runtimeControl'
import { actionText, environmentText, formatRuntimeDate, opsStatusText, opsTagType } from './shared'

const props = withDefaults(
  defineProps<{
    visible: boolean
    page?: PageResult<RuntimeControlIncidentVO[]>
    loading?: boolean
    submitting?: boolean
  }>(),
  {
    loading: false,
    submitting: false
  }
)

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'refresh'): void
  (e: 'create', data: RuntimeControlIncidentCreateReqVO): void
  (e: 'record', payload: { id: number; data: RuntimeControlIncidentActionReqVO }): void
  (e: 'close', payload: { id: number; data: RuntimeControlIncidentCloseReqVO }): void
}>()

const drawerVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
})

const selectedIncidentId = ref<number>()
const rows = computed(() => props.page?.list || [])
const selectedIncident = computed(() =>
  rows.value.find((incident) => incident.id === selectedIncidentId.value)
)

const actionForm = reactive<RuntimeControlIncidentActionReqVO>({
  action: '',
  verificationResult: 'PASSED',
  evidence: ''
})

const closeForm = reactive<RuntimeControlIncidentCloseReqVO>({
  ownerGateResult: 'PASSED',
  verificationResult: 'PASSED',
  remainingRisk: '',
  postmortemStatus: 'DONE',
  closeReason: ''
})

const createForm = reactive<RuntimeControlIncidentCreateReqVO>({
  environment: 'prod',
  action: 'runtime-control',
  severity: 'P1',
  title: '',
  description: '',
  sourceType: 'DIRECT',
  sourceId: ''
})

const selectIncident = (row?: RuntimeControlIncidentVO) => {
  selectedIncidentId.value = row?.id
}

const emitRecordAction = () => {
  if (!selectedIncident.value) return
  emit('record', {
    id: selectedIncident.value.id,
    data: { ...actionForm }
  })
}

const emitCloseIncident = () => {
  if (!selectedIncident.value) return
  emit('close', {
    id: selectedIncident.value.id,
    data: { ...closeForm }
  })
}

const emitCreateIncident = () => {
  emit('create', { ...createForm })
}

watch(
  rows,
  (items) => {
    if (!selectedIncidentId.value && items.length > 0) {
      selectedIncidentId.value = items[0].id
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.incident-layout {
  display: grid;
  gap: 12px;
}

.incident-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.incident-detail,
.incident-form {
  padding: 12px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.incident-detail__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.incident-detail__title,
.incident-form__title {
  color: #172033;
  font-size: 14px;
  font-weight: 700;
}

.incident-detail__meta {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.incident-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 12px;
}

.incident-form__title {
  grid-column: 1 / -1;
  margin-bottom: 8px;
}

.incident-form--gate {
  margin-top: 12px;
  border-color: #ffd6d6;
}

.incident-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

@media (max-width: 900px) {
  .incident-form {
    grid-template-columns: 1fr;
  }
}
</style>
