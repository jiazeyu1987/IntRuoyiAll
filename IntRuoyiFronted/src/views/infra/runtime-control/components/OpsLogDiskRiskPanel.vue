<template>
  <section class="ops-card">
    <div class="ops-card__head">
      <div>
        <div class="ops-card__title">日志与磁盘风险</div>
        <div class="ops-card__meta">{{ formatRuntimeDate(props.capacity?.sampledAt) }}</div>
      </div>
      <el-tag :type="opsTagType(props.capacity?.status)">{{ opsStatusText(props.capacity?.status) }}</el-tag>
    </div>
    <div class="metric-grid" v-loading="props.loading">
      <div class="metric-item">
        <div class="metric-item__head">
          <span>磁盘</span>
          <el-tag :type="opsTagType(props.capacity?.disk?.status)">
            {{ opsStatusText(props.capacity?.disk?.status) }}
          </el-tag>
        </div>
        <el-progress
          :percentage="progressValue(props.capacity?.disk?.usagePercent)"
          :stroke-width="6"
          :show-text="false"
        />
        <div class="metric-item__row">
          <span>{{ percentText(props.capacity?.disk?.usagePercent) }}</span>
          <span>{{ bytesText(props.capacity?.disk?.usableBytes) }} 可用</span>
        </div>
        <div class="metric-item__path">{{ props.capacity?.disk?.path || '-' }}</div>
        <div v-if="props.capacity?.disk?.reason" class="metric-item__reason">
          {{ props.capacity.disk.reason }}
        </div>
      </div>
      <div class="metric-item">
        <div class="metric-item__head">
          <span>日志目录</span>
          <el-tag :type="opsTagType(props.capacity?.logDirectory?.status)">
            {{ opsStatusText(props.capacity?.logDirectory?.status) }}
          </el-tag>
        </div>
        <div class="metric-item__row">
          <span>大小 {{ bytesText(props.capacity?.logDirectory?.sizeBytes) }}</span>
          <span>增长 {{ bytesText(props.capacity?.logDirectory?.growthBytes) }}</span>
        </div>
        <div class="metric-item__path">{{ props.capacity?.logDirectory?.path || '-' }}</div>
        <div v-if="props.capacity?.logDirectory?.reason" class="metric-item__reason">
          {{ props.capacity.logDirectory.reason }}
        </div>
      </div>
    </div>
    <div v-if="props.capacity?.reasons?.length" class="risk-reasons">
      {{ joinReasons(props.capacity.reasons) }}
    </div>
    <div v-if="props.capacity?.alert" class="risk-alert">
      {{ props.capacity.alert.title }}：{{ props.capacity.alert.content }}
    </div>
  </section>
</template>

<script setup lang="ts">
import type { RuntimeControlCapacityStatusVO } from '@/api/infra/runtimeControl'
import {
  bytesText,
  formatRuntimeDate,
  joinReasons,
  opsStatusText,
  opsTagType,
  percentText
} from './shared'

const props = withDefaults(
  defineProps<{
    capacity?: RuntimeControlCapacityStatusVO
    loading?: boolean
  }>(),
  {
    loading: false
  }
)

const progressValue = (value?: number) => {
  if (value === undefined || value === null || Number.isNaN(value)) return 0
  return Math.max(0, Math.min(100, Number(value)))
}
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

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 12px;
}

.metric-item {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 10px;
  background: #fafcff;
  border: 1px solid #edf1f6;
  border-radius: 6px;
}

.metric-item__head,
.metric-item__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.metric-item__head {
  color: #263247;
  font-size: 13px;
  font-weight: 700;
}

.metric-item__row,
.metric-item__path {
  color: #4b5563;
  font-size: 12px;
}

.metric-item__path {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-item__reason,
.risk-reasons,
.risk-alert {
  color: #b42318;
  font-size: 12px;
  line-height: 18px;
}

.risk-reasons,
.risk-alert {
  padding: 8px 12px;
  border-top: 1px solid #edf1f6;
}

.risk-alert {
  background: #fff7f7;
}

@media (max-width: 900px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
