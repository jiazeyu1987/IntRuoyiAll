<template>
  <section
    class="controlled-content-state-strip"
    :data-testid="testId || 'controlled-content-state-strip'"
  >
    <div class="controlled-content-state-strip__main">
      <div class="controlled-content-state-strip__row">
        <span class="controlled-content-state-strip__title">{{ title }}</span>
        <el-tag v-if="versionText" type="info">{{ versionText }}</el-tag>
        <el-tag v-if="statusLabel" :type="resolvedStatusType">{{ statusLabel }}</el-tag>
        <el-tag v-if="candidateCount > 0" type="warning">
          打开候选 {{ candidateCount }}
        </el-tag>
        <el-tag v-if="editable" type="success">可编辑</el-tag>
        <el-tag v-else-if="readonly" type="info">只读</el-tag>
      </div>
      <div v-if="metadataText" class="controlled-content-state-strip__meta">
        {{ metadataText }}
      </div>
      <div v-if="hint" class="controlled-content-state-strip__hint">
        {{ hint }}
      </div>
      <div v-if="normalizedBlockers.length" class="controlled-content-state-strip__blockers">
        <el-tag
          v-for="blocker in normalizedBlockers"
          :key="blocker"
          type="danger"
          effect="plain"
          class="controlled-content-state-strip__blocker"
        >
          {{ blocker }}
        </el-tag>
      </div>
    </div>
    <div v-if="$slots.actions" class="controlled-content-state-strip__actions">
      <slot name="actions"></slot>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type ControlledContentStateTagType = 'success' | 'warning' | 'info' | 'danger' | 'primary' | ''

const props = withDefaults(
  defineProps<{
    title: string
    testId?: string
    versionNo?: string | number | null
    statusLabel?: string | null
    statusType?: ControlledContentStateTagType
    modeLabel?: string | null
    sourceLabel?: string | null
    hint?: string | null
    candidateCount?: number
    blockers?: ReadonlyArray<string>
    readonly?: boolean
    editable?: boolean
  }>(),
  {
    testId: '',
    versionNo: '',
    statusLabel: '',
    statusType: 'info',
    modeLabel: '',
    sourceLabel: '',
    hint: '',
    candidateCount: 0,
    blockers: () => [],
    readonly: false,
    editable: false
  }
)

const versionText = computed(() => {
  if (props.versionNo === undefined || props.versionNo === null || props.versionNo === '') {
    return ''
  }
  return `版本 ${props.versionNo}`
})

const resolvedStatusType = computed(() => props.statusType || 'info')

const metadataText = computed(() =>
  [props.modeLabel, props.sourceLabel].filter((item): item is string => Boolean(item)).join(' / ')
)

const normalizedBlockers = computed(() =>
  props.blockers.map((item) => String(item || '').trim()).filter(Boolean)
)
</script>

<style scoped lang="scss">
.controlled-content-state-strip {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f7f9fc;
  color: #172033;
}

.controlled-content-state-strip__main {
  display: grid;
  flex: 1;
  gap: 6px;
  min-width: 0;
}

.controlled-content-state-strip__row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.controlled-content-state-strip__title {
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
}

.controlled-content-state-strip__meta,
.controlled-content-state-strip__hint {
  color: #6b7280;
  font-size: 13px;
  line-height: 20px;
}

.controlled-content-state-strip__blockers {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.controlled-content-state-strip__blocker {
  max-width: 100%;
}

.controlled-content-state-strip__actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 680px) {
  .controlled-content-state-strip {
    flex-direction: column;
  }

  .controlled-content-state-strip__actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
