<template>
  <section class="ops-card">
    <div class="ops-card__head">
      <div>
        <div class="ops-card__title">探针状态</div>
        <div class="ops-card__meta">{{ formatRuntimeDate(props.latest?.sampledAt) }}</div>
      </div>
      <div class="ops-card__actions">
        <el-tag :type="opsTagType(props.latest?.status)">{{ opsStatusText(props.latest?.status) }}</el-tag>
        <el-button type="primary" :loading="props.loading" @click="emit('run')">
          <Icon icon="ep:cpu" class="mr-4px" />
          执行探针
        </el-button>
      </div>
    </div>
    <el-table
      :data="props.latest?.probes || []"
      height="242"
      size="small"
      empty-text="暂无探针"
      v-loading="props.loading"
    >
      <el-table-column label="环境" width="86">
        <template #default="{ row }">{{ environmentText(row.environment) }}</template>
      </el-table-column>
      <el-table-column label="组件" prop="component" min-width="142" show-overflow-tooltip />
      <el-table-column label="类型" prop="probeType" width="96" show-overflow-tooltip />
      <el-table-column label="目标地址" prop="url" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" width="92">
        <template #default="{ row }">
          <el-tag :type="opsTagType(row.status)">{{ opsStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="HTTP" prop="httpStatusCode" width="82" />
      <el-table-column label="耗时" width="88">
        <template #default="{ row }">{{ row.durationMillis ?? '-' }}ms</template>
      </el-table-column>
      <el-table-column label="错误" prop="error" min-width="168" show-overflow-tooltip />
    </el-table>
    <div v-if="props.latest?.alert" class="probe-alert">
      {{ props.latest.alert.title }}：{{ props.latest.alert.content }}
    </div>
  </section>
</template>

<script setup lang="ts">
import type { RuntimeControlProbeLatestVO } from '@/api/infra/runtimeControl'
import { environmentText, formatRuntimeDate, opsStatusText, opsTagType } from './shared'

const props = withDefaults(
  defineProps<{
    latest?: RuntimeControlProbeLatestVO
    loading?: boolean
  }>(),
  {
    loading: false
  }
)

const emit = defineEmits<{
  (e: 'run'): void
}>()
</script>

<style scoped>
.ops-card {
  min-width: 0;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.ops-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid #edf1f6;
}

.ops-card__title {
  color: #172033;
  font-size: 14px;
  font-weight: 700;
}

.ops-card__meta {
  margin-top: 2px;
  color: #4b5563;
  font-size: 12px;
}

.ops-card__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.probe-alert {
  padding: 8px 12px;
  color: #b42318;
  font-size: 12px;
  line-height: 18px;
  background: #fff7f7;
  border-top: 1px solid #edf1f6;
}
</style>
