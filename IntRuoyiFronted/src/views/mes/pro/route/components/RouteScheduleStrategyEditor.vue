<template>
  <div class="route-schedule-strategy-editor">
    <el-radio-group
      :model-value="modelValue || 'RESOURCE_CALCULATED'"
      :disabled="disabled"
      size="small"
      class="route-schedule-strategy-editor__modes"
      @update:model-value="(value) => emit('update:modelValue', value as CapacityMode)"
    >
      <el-radio-button label="RESOURCE_CALCULATED">资源计算</el-radio-button>
      <el-radio-button label="MANUAL_OVERRIDE">产能覆盖</el-radio-button>
      <el-radio-button label="INFINITE_FORMULA">无限公式</el-radio-button>
    </el-radio-group>

    <div v-if="modelValue === 'FINITE_HOURLY'" class="route-schedule-strategy-editor__legacy">
      <CapacitySourceTag value="FINITE_HOURLY" />
      <span>历史策略仅展示，保存前请选择新策略。</span>
    </div>

    <div v-if="activeMode === 'MANUAL_OVERRIDE'" class="route-schedule-strategy-editor__field">
      <span>产能覆盖</span>
      <el-input-number
        :model-value="hourlyCapacity"
        :disabled="disabled"
        :min="0.000001"
        :precision="6"
        controls-position="right"
        size="small"
        @update:model-value="(value) => emit('update:hourlyCapacity', value as number | undefined)"
      />
      <span class="route-schedule-strategy-editor__unit">产能/h</span>
    </div>

    <div v-else-if="activeMode === 'INFINITE_FORMULA'" class="route-schedule-strategy-editor__formula">
      <el-input-number
        :model-value="infiniteDurationQuantityFactor"
        :disabled="disabled"
        :min="0.000001"
        :precision="2"
        :step="0.01"
        controls-position="right"
        size="small"
        placeholder="数量系数"
        @update:model-value="(value) => emit('update:infiniteDurationQuantityFactor', value as number | undefined)"
      />
      <el-input-number
        :model-value="infiniteDurationBaseMinutes"
        :disabled="disabled"
        :min="0"
        :precision="0"
        controls-position="right"
        size="small"
        placeholder="基础分钟"
        @update:model-value="(value) => emit('update:infiniteDurationBaseMinutes', value as number | undefined)"
      />
    </div>

    <div class="route-schedule-strategy-editor__calendar">
      <el-switch
        :model-value="Boolean(nightShiftEnabled)"
        :disabled="disabled"
        active-text="夜班"
        inactive-text="白班"
        size="small"
        @update:model-value="handleNightShiftChange"
      />
      <el-input-number
        v-if="nightShiftEnabled"
        :model-value="calendarRuleId ?? undefined"
        :disabled="disabled"
        :min="1"
        :precision="0"
        controls-position="right"
        placeholder="日历规则"
        size="small"
        @update:model-value="handleCalendarRuleIdChange"
      />
    </div>

    <RouteResourceCapacityPreview
      v-if="routeProcessId"
      :route-process-id="routeProcessId"
      @loaded="preview = $event"
    />
    <CapacityDiffHint
      v-if="activeMode === 'MANUAL_OVERRIDE'"
      :manual-hourly-capacity="hourlyCapacity"
      :resource-hourly-capacity="preview?.resourceCapacityHourly"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ProRouteResourceCapacityPreviewVO, ProRouteScheduleConfigVO } from '@/api/mes/pro/route'
import CapacityDiffHint from './CapacityDiffHint.vue'
import CapacitySourceTag from './CapacitySourceTag.vue'
import RouteResourceCapacityPreview from './RouteResourceCapacityPreview.vue'

type CapacityMode = ProRouteScheduleConfigVO['capacityMode']

const props = defineProps<{
  modelValue?: CapacityMode | null
  routeProcessId?: number | null
  hourlyCapacity?: number
  infiniteDurationQuantityFactor?: number
  infiniteDurationBaseMinutes?: number
  nightShiftEnabled?: boolean | null
  calendarRuleId?: number | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: CapacityMode]
  'update:hourlyCapacity': [value?: number]
  'update:infiniteDurationQuantityFactor': [value?: number]
  'update:infiniteDurationBaseMinutes': [value?: number]
  'update:nightShiftEnabled': [value: boolean]
  'update:calendarRuleId': [value: number | null]
}>()

const preview = ref<ProRouteResourceCapacityPreviewVO | null>(null)
const activeMode = computed(() => (props.modelValue === 'FINITE_HOURLY' ? 'MANUAL_OVERRIDE' : props.modelValue || 'RESOURCE_CALCULATED'))

const handleNightShiftChange = (value: string | number | boolean) => {
  const enabled = Boolean(value)
  emit('update:nightShiftEnabled', enabled)
  if (!enabled) {
    emit('update:calendarRuleId', null)
  }
}

const handleCalendarRuleIdChange = (value?: number) => {
  emit('update:calendarRuleId', value == null ? null : Number(value))
}
</script>

<style scoped>
.route-schedule-strategy-editor {
  width: 100%;
}

.route-schedule-strategy-editor__modes,
.route-schedule-strategy-editor__field,
.route-schedule-strategy-editor__formula,
.route-schedule-strategy-editor__calendar,
.route-schedule-strategy-editor__legacy {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.route-schedule-strategy-editor__field,
.route-schedule-strategy-editor__formula,
.route-schedule-strategy-editor__calendar,
.route-schedule-strategy-editor__legacy {
  margin-top: 8px;
}

.route-schedule-strategy-editor__legacy {
  color: #c2410c;
  font-size: 13px;
}

.route-schedule-strategy-editor__unit {
  color: #4b5563;
  font-size: 12px;
}
</style>
