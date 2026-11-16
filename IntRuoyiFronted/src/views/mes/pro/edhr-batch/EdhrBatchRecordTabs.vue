<template>
  <el-tabs
    :model-value="activeTab"
    class="edhr-batch-record-tabs"
    data-edhr-batch-record-tabs
    @tab-click="handleTabClick"
  >
    <el-tab-pane label="批次执行" name="execution">
      <template #label>
        <span class="edhr-tab-label"><Icon icon="lucide:layers-2" aria-hidden="true" />批次执行</span>
      </template>
    </el-tab-pane>
    <el-tab-pane label="历史追溯" name="history">
      <template #label>
        <span class="edhr-tab-label"><Icon icon="lucide:history" aria-hidden="true" />历史追溯</span>
      </template>
    </el-tab-pane>
    <el-tab-pane label="不合格评审" name="nonconformanceReview">
      <template #label>
        <span class="edhr-tab-label"><Icon icon="lucide:clipboard-x" aria-hidden="true" />不合格评审</span>
      </template>
    </el-tab-pane>
    <el-tab-pane label="偏差管理" name="deviation">
      <template #label>
        <span class="edhr-tab-label"><Icon icon="lucide:triangle-alert" aria-hidden="true" />偏差管理</span>
      </template>
    </el-tab-pane>
    <el-tab-pane label="作废" name="voided">
      <template #label>
        <span class="edhr-tab-label"><Icon icon="lucide:file-x-2" aria-hidden="true" />作废</span>
      </template>
    </el-tab-pane>
  </el-tabs>
</template>

<script setup lang="ts">
defineOptions({ name: 'MesProEdhrBatchRecordTabs' })

type EdhrBatchRecordTab =
  | 'execution'
  | 'history'
  | 'voided'
  | 'nonconformanceReview'
  | 'deviation'
type EdhrBatchTabPane = {
  props?: {
    name?: string | number
  }
}

const props = defineProps<{
  activeTab: EdhrBatchRecordTab
}>()

const router = useRouter()

const routeByTab: Partial<Record<EdhrBatchRecordTab, string>> = {
  execution: '/mes/pro/feedback/edhr-batch-execution',
  history: '/mes/pro/feedback/edhr-batch-history',
  voided: '/mes/pro/feedback/edhr-batch-voided',
  nonconformanceReview: '/mes/pro/feedback/edhr-nonconformance-review',
  deviation: '/mes/pro/feedback/edhr-deviation'
}

const navigateToTab = async (name: string | number | undefined) => {
  const nextTab = String(name) as EdhrBatchRecordTab
  const nextPath = routeByTab[nextTab]
  if (!nextPath) {
    throw new Error(`未知 eDHR 批记录页签：${String(name)}`)
  }
  if (nextTab === props.activeTab || router.currentRoute.value.path === nextPath) {
    return
  }
  const currentQuery = { ...router.currentRoute.value.query }
  await router.push({ path: nextPath, query: currentQuery })
}

const handleTabClick = async (pane: EdhrBatchTabPane) => {
  await navigateToTab(pane.props?.name)
}
</script>

<style scoped>
.edhr-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.edhr-batch-record-tabs {
  border: 1px solid #dbe3ef;
  border-radius: 8px 8px 0 0;
  background: #ffffff;
  padding: 0 16px;
}
</style>
