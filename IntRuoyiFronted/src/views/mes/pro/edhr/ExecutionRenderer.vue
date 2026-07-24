<template>
  <div class="edhr-renderer-shell">
    <div class="edhr-renderer-shell__header">
      <span class="edhr-renderer-shell__title">执行快照</span>
      <span class="edhr-renderer-shell__hint">按模板字段展示执行内容，原始快照可展开核查</span>
    </div>

    <el-alert
      v-if="snapshotState.error"
      :title="snapshotState.error"
      type="error"
      :closable="false"
      show-icon
    />

    <template v-else>
      <div v-if="topLevelFields.length" class="edhr-renderer-shell__section">
        <div class="edhr-renderer-shell__section-title">快照摘要</div>
        <el-descriptions
          :column="2"
          border
          class="edhr-renderer-shell__summary"
        >
          <el-descriptions-item
            v-for="field in topLevelFields"
            :key="field.label"
            :label="field.label"
            min-width="180"
          >
            {{ field.value }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <el-collapse v-model="activeEvidencePanels" class="edhr-renderer-shell__evidence">
        <el-collapse-item title="快照原文" name="raw-snapshot">
          <div class="edhr-renderer-shell__json">
            <pre>{{ snapshotState.prettyJson }}</pre>
          </div>
        </el-collapse-item>
      </el-collapse>
    </template>
  </div>
</template>

<script setup lang="ts">
import { type ProFeedbackEdhrExecutionVO } from '@/api/mes/pro/feedback'

defineOptions({ name: 'EdhrExecutionRenderer' })

const props = defineProps<{
  execution: ProFeedbackEdhrExecutionVO
}>()

const activeEvidencePanels = ref<string[]>([])

const formatSnapshotValue = (value: unknown) => {
  if (value == null) {
    return '--'
  }
  if (typeof value === 'string') {
    return value
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  return JSON.stringify(value)
}

const snapshotState = computed(() => {
  const rawSnapshot = props.execution.executionSnapshotJson
  if (typeof rawSnapshot !== 'string' || !rawSnapshot.trim()) {
    return {
      error: 'eDHR 执行快照缺失，无法渲染执行内容。',
      parsed: undefined as unknown,
      prettyJson: ''
    }
  }

  try {
    const parsed = JSON.parse(rawSnapshot)
    return {
      error: '',
      parsed,
      prettyJson: JSON.stringify(parsed, null, 2)
    }
  } catch (error) {
    const message = error instanceof Error && error.message.trim() ? error.message : '未知解析错误'
    return {
      error: `执行快照解析失败：${message}`,
      parsed: undefined as unknown,
      prettyJson: rawSnapshot
    }
  }
})

const topLevelFields = computed(() => {
  const parsed = snapshotState.value.parsed
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    return []
  }

  return Object.entries(parsed as Record<string, unknown>)
    .filter(([, value]) => value == null || ['string', 'number', 'boolean'].includes(typeof value))
    .slice(0, 12)
    .map(([label, value]) => ({
      label,
      value: formatSnapshotValue(value)
    }))
})
</script>

<style scoped>
.edhr-renderer-shell {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-renderer-shell__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #e5ebf3;
  background: #f7f9fc;
}

.edhr-renderer-shell__title {
  color: #172033;
  font-size: 16px;
  font-weight: 600;
}

.edhr-renderer-shell__hint {
  color: #4b5563;
  font-size: 13px;
}

.edhr-renderer-shell__section {
  padding: 16px 16px 0;
}

.edhr-renderer-shell__section-title {
  margin-bottom: 12px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.edhr-renderer-shell__evidence {
  margin: 16px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-renderer-shell__evidence :deep(.el-collapse-item__header) {
  padding: 0 16px;
  background: #f7f9fc;
}

.edhr-renderer-shell__summary {
  padding: 0;
}

.edhr-renderer-shell__json {
  padding: 0;
}

.edhr-renderer-shell__json pre {
  overflow: auto;
  margin: 0;
  padding: 16px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #fafcff;
  color: #172033;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
