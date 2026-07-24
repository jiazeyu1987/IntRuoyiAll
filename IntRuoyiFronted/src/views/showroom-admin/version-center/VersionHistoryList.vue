<template>
  <section class="showroom-version-history-list" data-version-center-history>
    <div class="showroom-version-history-list__header">
      <div>
        <h3 class="showroom-version-history-list__title">历史版本</h3>
        <p class="showroom-version-history-list__tip">只显示具备版本中心读取条件的已发布 bundle。</p>
      </div>
      <el-tag type="info">共 {{ items.length }} 条</el-tag>
    </div>

    <div v-if="items.length === 0" class="showroom-version-history-list__empty">
      暂无可读历史版本
    </div>

    <button
      v-for="item in items"
      :key="item.revisionId"
      class="showroom-version-history-list__item"
      :class="{
        'showroom-version-history-list__item--active': item.revisionId === selectedRevisionId
      }"
      :disabled="!item.selectable"
      type="button"
      @click="emit('select', item.revisionId)"
    >
      <div class="showroom-version-history-list__item-head">
        <strong>V{{ item.revisionNo }}</strong>
        <div class="showroom-version-history-list__tags">
          <span
            v-if="item.currentContent"
            class="showroom-version-history-list__tag showroom-version-history-list__tag--content"
          >
            当前内容
          </span>
          <span
            v-if="item.currentPublic"
            class="showroom-version-history-list__tag showroom-version-history-list__tag--public"
          >
            当前线上
          </span>
        </div>
      </div>
      <p class="showroom-version-history-list__meta">
        {{ item.publishedAt || '发布时间未记录' }}
      </p>
      <ul class="showroom-version-history-list__summary">
        <li v-for="summary in item.diffSummary" :key="summary">{{ summary }}</li>
        <li v-if="item.diffSummary.length === 0">无差异摘要</li>
      </ul>
      <ul v-if="item.blockers.length > 0" class="showroom-version-history-list__blockers">
        <li v-for="blocker in item.blockers" :key="`${blocker.scope}-${blocker.blockerCode}`">
          {{ formatVersionCenterBlocker(blocker) }}
        </li>
      </ul>
      <img
        v-if="item.previewSummaryImageUrl"
        :src="item.previewSummaryImageUrl"
        alt="版本预览摘要图"
        class="showroom-version-history-list__preview"
      />
    </button>
  </section>
</template>

<script setup lang="ts">
import { formatVersionCenterBlocker, type VersionCenterHistoryItem } from './contracts'

defineOptions({ name: 'VersionHistoryList' })

defineProps<{
  items: VersionCenterHistoryItem[]
  selectedRevisionId: number | null
}>()

const emit = defineEmits<{
  select: [revisionId: number]
}>()
</script>

<style scoped>
.showroom-version-history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  padding: 14px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-version-history-list__header,
.showroom-version-history-list__item-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.showroom-version-history-list__title {
  margin: 0;
  color: #172033;
  font-size: 0.98rem;
}

.showroom-version-history-list__tip,
.showroom-version-history-list__meta,
.showroom-version-history-list__empty,
.showroom-version-history-list__summary,
.showroom-version-history-list__blockers {
  margin: 4px 0 0;
  color: #4b5563;
  font-size: 0.82rem;
  line-height: 1.5;
}

.showroom-version-history-list__item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  color: #172033;
  text-align: left;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 8px;
  cursor: pointer;
}

.showroom-version-history-list__item--active {
  border-color: #1677ff;
  box-shadow: inset 0 0 0 1px #1677ff;
}

.showroom-version-history-list__item:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.showroom-version-history-list__tags {
  display: inline-flex;
  gap: 6px;
  flex-wrap: wrap;
}

.showroom-version-history-list__tag {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 0.75rem;
}

.showroom-version-history-list__tag--content {
  color: #1677ff;
  background: #edf5ff;
}

.showroom-version-history-list__tag--public {
  color: #1f8f55;
  background: #eefaf2;
}

.showroom-version-history-list__summary {
  padding-left: 18px;
}

.showroom-version-history-list__blockers {
  padding-left: 18px;
  color: #b42318;
}

.showroom-version-history-list__preview {
  width: 100%;
  height: 120px;
  object-fit: cover;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
}
</style>
