<template>
  <doc-alert :title="pageTitle" url="https://doc.iocoder.cn/report/" />

  <ContentWrap v-if="loadErrorMessage">
    <el-alert :title="loadErrorMessage" type="error" :closable="false" show-icon />
  </ContentWrap>

  <ContentWrap v-else :body-style="{ padding: '0px' }" class="!mb-0 form-template-designer-wrapper">
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
import { BatchRecordReportApi } from '@/api/mes/pro/batchrecordreport'
import { getRefreshToken } from '@/utils/auth'

defineOptions({ name: 'FormCenterTemplateDesignerWrapper' })

const FORM_TEMPLATE_REPORT_PREFIX = 'FORMTPL:'

const props = withDefaults(
  defineProps<{
    designerTitle?: string
    previewTitle?: string
  }>(),
  {
    designerTitle: '表单模板 Jimu 编辑器',
    previewTitle: '表单模板预览'
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

const normalizeRouteQueryText = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  return typeof rawValue === 'string' && rawValue.trim() ? rawValue.trim() : ''
}

const appendToken = (path: string, useBaseUrl = true) => {
  const token = getRefreshToken()
  const separator = path.includes('?') ? '&' : '?'
  const withToken = `${path}${token ? `${separator}token=${encodeURIComponent(token)}` : ''}`
  return useBaseUrl ? `${import.meta.env.VITE_BASE_URL}${withToken}` : withToken
}

const ensureFormTemplateReportId = (reportId: string) => {
  if (!reportId.startsWith(FORM_TEMPLATE_REPORT_PREFIX)) {
    throw new Error(`表单模板 Jimu 编辑入口收到非模板报表 ID：${reportId}`)
  }
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
    const reportId = normalizeRouteQueryText(route.query.reportId)
    if (!reportId) {
      loadErrorMessage.value = '缺少表单模板 Jimu 报表 ID，无法打开表单模板 Jimu 编辑器。'
      return
    }
    ensureFormTemplateReportId(reportId)
    if (reportMode.value === 'edit') {
      const data = await BatchRecordReportApi.getEditPath(reportId)
      if (!isDesignerPath(data.path)) {
        throw new Error(`表单模板 Jimu 编辑路径不是设计器路径：${data.path}`)
      }
      ensureSameOriginDesignerEditSupport()
      viewMode.value = 'designer'
      src.value = appendToken(normalizeDesignerPath(data.path), false)
    } else {
      const data = await BatchRecordReportApi.getDesignerPath(reportId)
      const previewPath = isPreviewPath(data.path)
      viewMode.value = previewPath ? 'preview' : 'designer'
      if (previewPath) {
        ensureSameOriginPreviewSupport()
        src.value = appendToken(normalizePreviewPath(data.path), false)
      } else {
        src.value = appendToken(data.path)
      }
    }
  } catch (error: any) {
    loadErrorMessage.value = error?.message || '表单模板 Jimu 编辑器路径加载失败，请联系管理员。'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDesigner()
})

watch(
  () => [route.query.reportId, route.query.reportMode] as const,
  () => {
    void loadDesigner()
  }
)
</script>

<style scoped>
.form-template-designer-wrapper {
  border: none;
  background: transparent;
}

.form-template-designer-wrapper :deep(.el-card__body) {
  padding: 0 !important;
}

.designer-loading {
  padding: 16px;
}
</style>
