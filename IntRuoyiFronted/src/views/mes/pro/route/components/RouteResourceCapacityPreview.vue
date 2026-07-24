<template>
  <div class="route-resource-capacity-preview" :aria-busy="loading">
    <div class="route-resource-capacity-preview__summary">
      <span class="route-resource-capacity-preview__label">资源计算</span>
      <CapacitySourceTag :value="previewData?.capacitySource || 'UNCONFIGURED'" />
      <span class="route-resource-capacity-preview__value">
        {{ formatCapacity(previewData?.resourceCapacityHourly) }} / 小时
      </span>
    </div>
    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      :title="loadError"
      class="route-resource-capacity-preview__alert"
    />
    <el-alert
      v-else-if="previewData?.blockingIssues?.length"
      type="warning"
      :closable="false"
      show-icon
      class="route-resource-capacity-preview__alert"
    >
      <template #title>
        {{ previewData.blockingIssues.length }} 项资源阻断：{{ previewData.blockingIssues.map((item) => item.message || item.code).join('；') }}
      </template>
    </el-alert>
    <div v-if="previewData?.workstationRows?.length" class="route-resource-capacity-preview__rows">
      <div
        v-for="row in previewData.workstationRows"
        :key="row.workstationId || row.workstationCode"
        class="route-resource-capacity-preview__row"
      >
        <span class="route-resource-capacity-preview__station">{{ row.workstationName || row.workstationCode || '未命名工作站' }}</span>
        <span>{{ row.productionLineName || '产线未配置' }}</span>
        <span>{{ formatCapacity(row.hourlyCapacity) }} / 小时</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ProRouteApi, type ProRouteResourceCapacityPreviewVO } from '@/api/mes/pro/route'
import CapacitySourceTag from './CapacitySourceTag.vue'

const props = defineProps<{
  routeProcessId?: number | null
  preview?: ProRouteResourceCapacityPreviewVO | null
}>()

const emit = defineEmits<{
  loaded: [preview: ProRouteResourceCapacityPreviewVO]
}>()

const loading = ref(false)
const loadError = ref('')
const previewData = ref<ProRouteResourceCapacityPreviewVO | null>(props.preview ?? null)

const formatCapacity = (value?: number | null) => {
  if (value === undefined || value === null || Number.isNaN(Number(value))) return '0'
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 6 })
}

watch(
  () => props.preview,
  (value) => {
    if (value) previewData.value = value
  },
  { immediate: true }
)

watch(
  () => props.routeProcessId,
  async (routeProcessId) => {
    if (!routeProcessId || props.preview) return
    loading.value = true
    loadError.value = ''
    try {
      const result = await ProRouteApi.getScheduleResourcePreview(routeProcessId)
      previewData.value = result
      emit('loaded', result)
    } catch (error) {
      loadError.value = error instanceof Error ? error.message : '资源预览加载失败'
    } finally {
      loading.value = false
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.route-resource-capacity-preview {
  margin-top: 8px;
  padding: 8px 10px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fafcff;
  color: #263247;
  font-size: 13px;
}

.route-resource-capacity-preview__summary,
.route-resource-capacity-preview__row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 28px;
}

.route-resource-capacity-preview__label,
.route-resource-capacity-preview__station {
  font-weight: 600;
  color: #172033;
}

.route-resource-capacity-preview__value {
  font-variant-numeric: tabular-nums;
}

.route-resource-capacity-preview__alert,
.route-resource-capacity-preview__rows {
  margin-top: 8px;
}

.route-resource-capacity-preview__row {
  justify-content: space-between;
  border-top: 1px solid #edf1f6;
}
</style>
