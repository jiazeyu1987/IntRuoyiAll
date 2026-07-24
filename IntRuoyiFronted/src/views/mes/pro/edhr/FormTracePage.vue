<template>
  <ContentWrap>
    <div class="edhr-form-trace-page">
      <el-tabs v-model="activeTab" class="edhr-form-trace-page__tabs" @tab-change="handleTabChange">
        <el-tab-pane label="作废" name="change" lazy>
          <FormTraceChangeTab />
        </el-tab-pane>
        <el-tab-pane label="驳回" name="reject" lazy>
          <FormTraceReleaseTab trace-mode="reject" />
        </el-tab-pane>
        <el-tab-pane label="放行" name="release" lazy>
          <FormTraceReleaseTab />
        </el-tab-pane>
      </el-tabs>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import FormTraceChangeTab from './form-trace/FormTraceChangeTab.vue'
import FormTraceReleaseTab from './form-trace/FormTraceReleaseTab.vue'

defineOptions({ name: 'MesProFeedbackEdhrFormTrace' })

type FormTraceTabName = 'change' | 'reject' | 'release'

const route = useRoute()
const router = useRouter()

const resolveTabFromRoute = (): FormTraceTabName => {
  if (route.query.tab === 'release') return 'release'
  if (route.query.tab === 'reject') return 'reject'
  return 'change'
}
const activeTab = ref<FormTraceTabName>(resolveTabFromRoute())

watch(
  () => route.query.tab,
  () => {
    activeTab.value = resolveTabFromRoute()
  }
)

const handleTabChange = async (tabName: string | number) => {
  const tab = tabName === 'release' ? 'release' : tabName === 'reject' ? 'reject' : 'change'
  if (route.query.tab === tab || (tab === 'change' && !route.query.tab)) {
    return
  }
  await router.replace({
    path: route.path,
    query: {
      ...route.query,
      tab
    }
  })
}
</script>

<style scoped>
.edhr-form-trace-page {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.edhr-form-trace-page__tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.edhr-form-trace-page__tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #dbe3ef;
}

.edhr-form-trace-page__tabs :deep(.el-tabs__item) {
  color: #4b5563;
  font-weight: 600;
}

.edhr-form-trace-page__tabs :deep(.el-tabs__item.is-active) {
  color: #1677ff;
}
</style>
