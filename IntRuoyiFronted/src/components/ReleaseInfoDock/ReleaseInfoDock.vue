<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'

interface ReleaseChangeSet {
  gitChanges?: string[]
}

interface ReleaseInfo {
  releaseTag?: string
  changeSet?: ReleaseChangeSet
}

const releaseInfo = ref<ReleaseInfo | null>(null)
const loadError = ref('')
const dialogVisible = ref(false)

const currentReleaseTag = computed(() => releaseInfo.value?.releaseTag?.trim() || '')
const statusText = computed(() => currentReleaseTag.value || '版本信息未生成')
const gitChangeItems = computed(() => {
  return (releaseInfo.value?.changeSet?.gitChanges || []).slice(0, 10)
})

const loadReleaseInfo = async () => {
  const response = await fetch('/release-info.json', { cache: 'no-store' })
  if (!response.ok) {
    throw new Error(`release-info.json HTTP ${response.status}`)
  }
  releaseInfo.value = await response.json()
}

onMounted(async () => {
  try {
    await loadReleaseInfo()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : String(error)
  }
})
</script>

<template>
  <div class="release-info-dock" data-testid="release-info-dock">
    <button
      class="release-info-dock__version"
      :class="{ 'is-error': !currentReleaseTag }"
      type="button"
      @click="dialogVisible = true"
    >
      {{ statusText }}
    </button>
  </div>

  <ElDialog v-model="dialogVisible" title="版本变更说明" width="560px" append-to-body>
    <div v-if="releaseInfo" class="release-info-dialog">
      <section class="release-info-dialog__section">
        <h3>Git 变更（最多 10 条）</h3>
        <ul v-if="gitChangeItems.length">
          <li v-for="item in gitChangeItems" :key="item">{{ item }}</li>
        </ul>
        <p v-else>Git 变更未生成</p>
      </section>
    </div>
    <div v-else class="release-info-dialog__error">
      版本信息未生成：{{ loadError || 'release-info.json 不可读' }}
    </div>
  </ElDialog>
</template>

<style lang="scss" scoped>
.release-info-dock {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 42px;
  padding: 7px 8px;
  color: var(--left-menu-text-color);
  background: var(--left-menu-bg-color);
}

.release-info-dock__version {
  max-width: 100%;
  padding: 3px 7px;
  overflow: hidden;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  line-height: 18px;
  color: #1677ff;
  cursor: pointer;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #eef6ff;
  border: 1px solid #cfe5ff;
  border-radius: 5px;
}

.release-info-dock__version.is-error {
  color: #c2410c;
  background: #fff7ed;
  border-color: #fed7aa;
}

.release-info-dock__version:hover,
.release-info-dock__version:focus-visible {
  border-color: #1677ff;
  outline: none;
}

.release-info-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
  color: #263247;
}

.release-info-dialog__section {
  padding-top: 0;
}

.release-info-dialog__section h3 {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #172033;
}

.release-info-dialog__section p,
.release-info-dialog__section ul {
  margin: 0;
}

.release-info-dialog__section ul {
  padding-left: 18px;
}

.release-info-dialog__section li {
  margin: 4px 0;
  overflow-wrap: anywhere;
}

.release-info-dialog__error {
  color: #c2410c;
  overflow-wrap: anywhere;
}

@media (max-width: 640px) {
  .release-info-dock {
    padding: 7px 6px;
  }

  .release-info-dock__version {
    max-width: 82px;
  }
}
</style>
