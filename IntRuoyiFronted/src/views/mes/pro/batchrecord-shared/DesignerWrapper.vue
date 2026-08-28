<template>
  <doc-alert :title="pageTitle" url="https://doc.iocoder.cn/report/" />

  <ContentWrap v-if="loadErrorMessage">
    <el-alert :title="loadErrorMessage" type="error" :closable="false" show-icon />
  </ContentWrap>

  <ContentWrap v-else :bodyStyle="{ padding: '0px' }" class="!mb-0">
    <IFrame
      v-if="!loading && src"
      :src="src"
      :sameOriginChromeMode="sameOriginChromeMode"
      @preview-blocked="handlePreviewBlocked"
    />
    <div v-else class="designer-loading">
      <el-skeleton :rows="8" animated />
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { getRefreshToken } from '@/utils/auth'
import { BatchRecordReportApi } from '@/api/mes/pro/batchrecordreport'

defineOptions({ name: 'MesProBatchRecordReportDesignerWrapper' })

const props = withDefaults(
  defineProps<{
    designerTitle?: string
    previewTitle?: string
  }>(),
  {
    designerTitle: '电子批记录报表设计器',
    previewTitle: '电子批记录报表预览'
  }
)

const route = useRoute()

const loading = ref(true)
const src = ref('')
const loadErrorMessage = ref('')
const viewMode = ref<'preview' | 'designer'>('designer')
const reportMode = computed<'preview' | 'edit'>(() => (route.query.reportMode === 'edit' ? 'edit' : 'preview'))

const pageTitle = computed(() =>
  viewMode.value === 'preview' ? props.previewTitle : props.designerTitle
)

const sameOriginChromeMode = computed(() =>
  reportMode.value === 'edit'
    ? 'jmreport-designer-edit'
    : viewMode.value === 'preview'
      ? 'jmreport-viewer'
      : 'off'
)

const isPreviewPath = (path: string) => path.includes('/jmreport/view/')
const isDesignerPath = (path: string) => path.includes('/jmreport/index/')

const normalizePreviewPath = (path: string) => {
  const marker = '/jmreport/view/'
  const markerIndex = path.indexOf(marker)
  return markerIndex >= 0 ? path.slice(markerIndex) : path
}

const normalizeDesignerPath = (path: string) => {
  const marker = '/jmreport/index/'
  const markerIndex = path.indexOf(marker)
  return markerIndex >= 0 ? path.slice(markerIndex) : path
}

const appendToken = (path: string, useBaseUrl = true) => {
  const token = getRefreshToken()
  const separator = path.includes('?') ? '&' : '?'
  const withToken = `${path}${token ? `${separator}token=${encodeURIComponent(token)}` : ''}`
  return useBaseUrl ? `${import.meta.env.VITE_BASE_URL}${withToken}` : withToken
}

const ensureSameOriginPreviewSupport = () => {
  if (!import.meta.env.VITE_PROXY_TARGET) {
    throw new Error('当前预览模式缺少 VITE_PROXY_TARGET，同源 /jmreport 代理未启用，无法隐藏 viewer 工具条。')
  }
}

const ensureSameOriginDesignerEditSupport = () => {
  if (import.meta.env.DEV && !import.meta.env.VITE_PROXY_TARGET) {
    throw new Error('当前编辑模式缺少 VITE_PROXY_TARGET，同源 /jmreport 代理未启用，无法适配 Jimu 编辑器。')
  }
}

const handlePreviewBlocked = (message: string) => {
  loadErrorMessage.value = message
  src.value = ''
  loading.value = false
}

const loadDesigner = async () => {
  loading.value = true
  loadErrorMessage.value = ''
  try {
    const reportId = String(route.params.reportId || route.query.reportId || '')
    if (!reportId) {
      loadErrorMessage.value = '缺少报表 ID，无法打开电子批记录设计器。'
      return
    }
    if (reportMode.value === 'edit') {
      const data = await BatchRecordReportApi.getEditPath(reportId)
      if (!isDesignerPath(data.path)) {
        throw new Error(`电子批记录编辑路径不是 Jimu 设计器路径：${data.path}`)
      }
      ensureSameOriginDesignerEditSupport()
      viewMode.value = 'designer'
      src.value = appendToken(normalizeDesignerPath(data.path), false)
    } else {
      const data = await BatchRecordReportApi.getDesignerPath(reportId)
      const previewPath = isPreviewPath(data.path)
      viewMode.value = previewPath ? 'preview' : 'designer'
      if (data.path.includes('/jmreport/view/')) {
        ensureSameOriginPreviewSupport()
        src.value = appendToken(normalizePreviewPath(data.path), false)
      } else {
        src.value = appendToken(data.path)
      }
    }
  } catch (error: any) {
    loadErrorMessage.value = error?.message || '电子批记录设计器路径加载失败，请联系管理员。'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDesigner()
})

watch(
  () => [route.params.reportId, route.query.reportId, route.query.reportMode] as const,
  () => {
    void loadDesigner()
  }
)
</script>

<style scoped>
.designer-loading {
  padding: 16px;
}
</style>
