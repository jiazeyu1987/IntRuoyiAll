<template>
  <el-tabs
    :model-value="activeTab"
    class="edhr-batch-record-tabs"
    data-edhr-batch-record-tabs
    @tab-click="handleTabClick"
  >
    <el-tab-pane label="批次执行" name="execution" />
    <el-tab-pane label="批记录页面关系图" name="pageGraph" />
  </el-tabs>
</template>

<script setup lang="ts">
defineOptions({ name: 'MesProEdhrBatchRecordTabs' })

type EdhrBatchRecordTab =
  | 'execution'
  | 'history'
  | 'pageGraph'
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
  pageGraph: '/mes/pro/feedback/edhr-batch-page-graph'
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
  await router.push({ path: nextPath })
}

const handleTabClick = async (pane: EdhrBatchTabPane) => {
  await navigateToTab(pane.props?.name)
}
</script>

<style scoped>
.edhr-batch-record-tabs {
  border: 1px solid #dbe3ef;
  border-radius: 8px 8px 0 0;
  background: #ffffff;
  padding: 0 16px;
}
</style>
