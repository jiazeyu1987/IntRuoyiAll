<template>
  <el-empty v-if="!timeline.length" description="暂无时间线记录" :image-size="48" />
  <ol v-else class="publication-timeline" aria-label="发布后续完整时间线">
    <li v-for="event in timeline" :key="event.eventId" class="publication-timeline__event">
      <span class="publication-timeline__sequence">{{ event.sequenceNo }}</span>
      <div class="publication-timeline__body">
        <div class="publication-timeline__heading">
          <strong>{{ event.actionLabel }}</strong>
          <time>{{ formatDateTimeValue(event.occurredAt) }}</time>
        </div>
        <div class="publication-timeline__meta">
          <span>{{ event.sourceLabel }}</span>
          <span>{{ event.objectLabel }}</span>
          <span>{{ event.actorId ? `操作人 #${event.actorId}` : '系统' }}</span>
        </div>
        <div v-if="event.statusBeforeLabel || event.statusAfterLabel" class="publication-timeline__line">
          状态：{{ event.statusBeforeLabel || '无' }} → {{ event.statusAfterLabel || '无' }}
        </div>
        <div v-if="event.decisionLabel" class="publication-timeline__line">结论：{{ event.decisionLabel }}</div>
        <div
          v-if="event.attemptCount !== null && event.attemptCount !== undefined"
          class="publication-timeline__line"
        >
          发送尝试次数：{{ event.attemptCount }} 次
        </div>
        <div
          v-if="event.assigneeBefore || event.assigneeAfter"
          class="publication-timeline__line publication-timeline__assignment"
        >
          <span>原负责人：{{ event.assigneeBefore ? `#${event.assigneeBefore}` : '未记录' }}</span>
          <span>新负责人：{{ event.assigneeAfter ? `#${event.assigneeAfter}` : '未记录' }}</span>
        </div>
        <div v-if="event.linkedRevisionControlledFileId" class="publication-timeline__line">
          <span>关联版本 ID：{{ event.linkedRevisionControlledFileId }}</span>
          <span v-if="event.linkedRevisionVersion">；关联版本号：{{ event.linkedRevisionVersion }}</span>
          <span v-else>；版本号未记录</span>
        </div>
        <div v-if="event.directionLabels.length" class="publication-timeline__line">
          关系：{{ event.directionLabels.join(' / ') }}
        </div>
        <div v-if="event.reason" class="publication-timeline__line">原因：{{ event.reason }}</div>
        <div v-if="event.errorSummary" class="publication-timeline__error" role="alert">
          错误摘要：{{ event.errorSummary }}
        </div>
      </div>
    </li>
  </ol>
</template>

<script lang="ts" setup>
import type { DccPublicationTimelineEventVO } from '@/api/dcc/controlledFile/publicationFollowup'
import { formatDateTimeValue } from '@/utils/formatTime'

defineProps<{ timeline: DccPublicationTimelineEventVO[] }>()
</script>

<style scoped>
.publication-timeline { display: grid; gap: 0; margin: 0; padding: 0; list-style: none; }
.publication-timeline__event { display: grid; grid-template-columns: 30px minmax(0, 1fr); gap: 10px; padding: 10px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.publication-timeline__sequence { display: grid; width: 26px; height: 26px; place-items: center; border-radius: 50%; background: var(--el-color-primary-light-9); color: var(--el-color-primary); font-weight: 600; }
.publication-timeline__body { min-width: 0; }
.publication-timeline__heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.publication-timeline__heading strong { color: #172033; font-size: 14px; }
.publication-timeline__heading time, .publication-timeline__meta { color: var(--el-text-color-secondary); font-size: 12px; }
.publication-timeline__meta { display: flex; flex-wrap: wrap; gap: 6px 12px; margin-top: 4px; }
.publication-timeline__line, .publication-timeline__error { margin-top: 5px; overflow-wrap: anywhere; font-size: 13px; line-height: 20px; }
.publication-timeline__assignment { display: flex; flex-wrap: wrap; gap: 4px 16px; }
.publication-timeline__error { color: var(--el-color-danger); }
@media (max-width: 768px) {
  .publication-timeline__heading { flex-direction: column; gap: 4px; }
  .publication-timeline__event { grid-template-columns: 26px minmax(0, 1fr); }
}
</style>
