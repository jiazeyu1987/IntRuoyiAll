<template>
  <div class="onlyoffice-viewer-shell">
    <el-alert
      v-if="errorMessage"
      :closable="false"
      class="mb-16px"
      show-icon
      type="error"
      :title="errorMessage"
    />
    <div v-else class="onlyoffice-viewer-frame">
      <div class="onlyoffice-viewer-toolbar">
        <div class="onlyoffice-viewer-toolbar__hint">
          <span>Ctrl + 滚轮可缩放预览</span>
          <span>当前仅调整查看比例，不放开受控权限</span>
        </div>
        <div class="onlyoffice-viewer-toolbar__actions">
          <el-button
            text
            bg
            size="small"
            aria-label="缩小预览"
            :disabled="!canZoomOut"
            @click="decreaseZoom"
          >
            缩小
          </el-button>
          <div class="onlyoffice-viewer-toolbar__value">{{ previewZoomLabel }}</div>
          <el-button
            text
            bg
            size="small"
            aria-label="重置缩放"
            :disabled="previewZoomValue === DEFAULT_ZOOM_VALUE"
            @click="resetZoom"
          >
            重置
          </el-button>
          <el-button
            text
            bg
            size="small"
            aria-label="放大预览"
            :disabled="!canZoomIn"
            @click="increaseZoom"
          >
            放大
          </el-button>
        </div>
      </div>
      <div
        class="onlyoffice-viewer-frame__stage"
        @mouseenter="isPointerInsideStage = true"
        @mouseleave="handleStageLeave"
        @wheel="handleWheelZoom"
      >
        <div v-if="wheelZoomArmed" class="onlyoffice-viewer-frame__wheel-mask">
          滚轮缩放已激活
        </div>
        <div class="onlyoffice-viewer-frame__scaler" :style="scalerStyle">
          <div
            ref="mountRef"
            :id="containerId"
            class="onlyoffice-viewer-frame__mount"
            :class="{ 'onlyoffice-viewer-frame__mount--wheel-armed': wheelZoomArmed }"
          ></div>
        </div>
      </div>
      <div
        v-if="watermarkBackground"
        class="onlyoffice-viewer-frame__overlay"
        :style="{ backgroundImage: watermarkBackground }"
      ></div>
      <div
        v-if="watermarkDetail"
        class="onlyoffice-viewer-frame__watermark"
      >
        <div class="onlyoffice-viewer-frame__watermark-title">
          <span>{{ watermarkLabel }}</span>
          <span>禁止截图/外传</span>
        </div>
        <div>{{ watermarkDetail }}</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
declare global {
  interface Window {
    DocsAPI?: {
      DocEditor: new (id: string, config: Record<string, unknown>) => {
        destroyEditor?: () => void
      }
    }
    __dccOnlyOfficeScriptPromise?: Promise<void>
  }
}

const props = defineProps<{
  baseUrl?: string
  documentUrl?: string
  documentTitle?: string
  watermarkLabel?: string
  watermarkDetail?: string
  watermarkBackground?: string
  unavailableReason?: string
}>()

const containerId = `dcc-onlyoffice-${Math.random().toString(36).slice(2, 10)}`
const DEFAULT_ZOOM_VALUE = 100
const MIN_ZOOM_VALUE = 60
const MAX_ZOOM_VALUE = 200
const ZOOM_STEP_VALUE = 10
const VIEWER_STAGE_HEIGHT = 720
const errorMessage = ref('')
const mountRef = ref<HTMLDivElement>()
const previewZoomValue = ref(DEFAULT_ZOOM_VALUE)
const isPointerInsideStage = ref(false)
const isZoomModifierPressed = ref(false)
let editorInstance: { destroyEditor?: () => void } | null = null

const onlyOfficeReadOnlyPermissions = {
  edit: false,
  comment: false,
  review: false,
  download: false,
  print: false,
  copy: false
}

const onlyOfficeReadOnlyUser = {
  id: 'dcc-readonly-viewer',
  name: '受控预览'
}

const normalizeZoomValue = (value: number) => {
  if (!Number.isFinite(value)) {
    return DEFAULT_ZOOM_VALUE
  }
  return Math.min(MAX_ZOOM_VALUE, Math.max(MIN_ZOOM_VALUE, Math.round(value)))
}

const previewZoomLabel = computed(() => `${previewZoomValue.value}%`)
const canZoomOut = computed(() => previewZoomValue.value > MIN_ZOOM_VALUE)
const canZoomIn = computed(() => previewZoomValue.value < MAX_ZOOM_VALUE)
const wheelZoomArmed = computed(() => isPointerInsideStage.value && isZoomModifierPressed.value)
const scalerStyle = computed(() => {
  const zoomScale = previewZoomValue.value / 100
  return {
    width: `${100 / zoomScale}%`,
    height: `${VIEWER_STAGE_HEIGHT / zoomScale}px`,
    transform: `scale(${zoomScale})`
  }
})

const trimTrailingSlash = (value?: string) => {
  let output = String(value || '').trim()
  while (output.endsWith('/')) {
    output = output.slice(0, -1)
  }
  return output
}

const resolveFileExtension = (value?: string) => {
  const cleanValue = String(value || '').trim().toLowerCase()
  const index = cleanValue.lastIndexOf('.')
  return index >= 0 ? cleanValue.slice(index + 1) : 'docx'
}

const resolveDocumentType = (extension: string) => {
  if (['xls', 'xlsx', 'ods', 'csv'].includes(extension)) {
    return 'cell'
  }
  if (['ppt', 'pptx', 'odp'].includes(extension)) {
    return 'slide'
  }
  return 'word'
}

const buildDocumentKey = (value: string) => {
  let hash = 0
  for (let index = 0; index < value.length; index += 1) {
    hash = (hash << 5) - hash + value.charCodeAt(index)
    hash |= 0
  }
  return `dcc-${Math.abs(hash)}`
}

const resolveOnlyOfficeErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  if (typeof error === 'string' && error.trim()) {
    return error.trim()
  }
  return 'OnlyOffice 预览初始化失败'
}

const resolveOnlyOfficeDocumentErrorMessage = (event: unknown) => {
  const eventRecord = event && typeof event === 'object' ? event as Record<string, unknown> : {}
  const dataRecord = eventRecord.data && typeof eventRecord.data === 'object'
    ? eventRecord.data as Record<string, unknown>
    : eventRecord
  const errorCode = dataRecord.errorCode || eventRecord.errorCode
  const errorDescription = dataRecord.errorDescription || eventRecord.errorDescription
  const details = [
    errorCode ? `错误码 ${String(errorCode)}` : '',
    errorDescription ? String(errorDescription) : ''
  ].filter(Boolean).join('，')
  return details ? `OnlyOffice 文档加载失败：${details}` : 'OnlyOffice 文档加载失败'
}

const loadOnlyOfficeScript = async (baseUrl: string) => {
  if (window.DocsAPI?.DocEditor) {
    return
  }
  if (!window.__dccOnlyOfficeScriptPromise) {
    window.__dccOnlyOfficeScriptPromise = new Promise<void>((resolve, reject) => {
      const script = document.createElement('script')
      script.src = `${trimTrailingSlash(baseUrl)}/web-apps/apps/api/documents/api.js`
      script.async = true
      script.onload = () => resolve()
      script.onerror = () => {
        window.__dccOnlyOfficeScriptPromise = undefined
        reject(new Error('OnlyOffice 预览脚本加载失败'))
      }
      document.head.appendChild(script)
    })
  }
  await window.__dccOnlyOfficeScriptPromise
}

const destroyEditor = () => {
  editorInstance?.destroyEditor?.()
  editorInstance = null
}

const applyZoom = (value: number) => {
  previewZoomValue.value = normalizeZoomValue(value)
}

const decreaseZoom = () => {
  applyZoom(previewZoomValue.value - ZOOM_STEP_VALUE)
}

const increaseZoom = () => {
  applyZoom(previewZoomValue.value + ZOOM_STEP_VALUE)
}

const resetZoom = () => {
  applyZoom(DEFAULT_ZOOM_VALUE)
}

const syncZoomModifierState = (event?: KeyboardEvent) => {
  isZoomModifierPressed.value = Boolean(event?.ctrlKey || event?.metaKey)
}

const handleWindowKeydown = (event: KeyboardEvent) => {
  syncZoomModifierState(event)
}

const handleWindowKeyup = (event: KeyboardEvent) => {
  syncZoomModifierState(event)
}

const handleWindowBlur = () => {
  isZoomModifierPressed.value = false
}

const handleStageLeave = () => {
  isPointerInsideStage.value = false
}

const handleWheelZoom = (event: WheelEvent) => {
  if (!wheelZoomArmed.value || !event.ctrlKey) {
    return
  }
  event.preventDefault()
  if (event.deltaY > 0) {
    decreaseZoom()
    return
  }
  if (event.deltaY < 0) {
    increaseZoom()
  }
}

const mountEditor = async () => {
  destroyEditor()
  errorMessage.value = props.unavailableReason || ''
  if (errorMessage.value) {
    return
  }
  const baseUrl = trimTrailingSlash(props.baseUrl)
  const documentUrl = String(props.documentUrl || '').trim()
  if (!baseUrl || !documentUrl) {
    errorMessage.value = 'OnlyOffice 预览地址未准备好'
    return
  }
  try {
    await loadOnlyOfficeScript(baseUrl)
    if (!window.DocsAPI?.DocEditor) {
      errorMessage.value = 'OnlyOffice 预览脚本未就绪'
      return
    }
    const extension = resolveFileExtension(props.documentTitle || documentUrl)
    const documentType = resolveDocumentType(extension)
    editorInstance = new window.DocsAPI.DocEditor(containerId, {
      document: {
        fileType: extension,
        key: buildDocumentKey(documentUrl),
        title: props.documentTitle || 'Office 受控预览',
        url: documentUrl,
        permissions: onlyOfficeReadOnlyPermissions
      },
      documentType,
      editorConfig: {
        mode: 'view',
        lang: 'zh-CN',
        user: onlyOfficeReadOnlyUser,
        customization: {
          anonymous: {
            request: false
          },
          chat: false,
          comments: false,
          compactHeader: true,
          compactToolbar: true,
          help: false
        }
      },
      events: {
        onError: (event: unknown) => {
          errorMessage.value = resolveOnlyOfficeDocumentErrorMessage(event)
          destroyEditor()
        }
      },
      type: 'desktop',
      width: '100%',
      height: '720px'
    })
  } catch (error) {
    errorMessage.value = resolveOnlyOfficeErrorMessage(error)
  }
}

watch(
  () => [props.baseUrl, props.documentUrl, props.documentTitle, props.unavailableReason],
  () => {
    void mountEditor()
  },
  { immediate: true }
)

onMounted(() => {
  window.addEventListener('keydown', handleWindowKeydown)
  window.addEventListener('keyup', handleWindowKeyup)
  window.addEventListener('blur', handleWindowBlur)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleWindowKeydown)
  window.removeEventListener('keyup', handleWindowKeyup)
  window.removeEventListener('blur', handleWindowBlur)
  destroyEditor()
})
</script>

<style scoped>
.onlyoffice-viewer-shell {
  min-height: 560px;
}

.onlyoffice-viewer-frame {
  position: relative;
  min-height: 812px;
  padding: 18px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f4f7fb;
}

.onlyoffice-viewer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.onlyoffice-viewer-toolbar__hint {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.onlyoffice-viewer-toolbar__hint span:first-child {
  color: #172033;
  font-weight: 600;
}

.onlyoffice-viewer-toolbar__actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.onlyoffice-viewer-toolbar__value {
  min-width: 62px;
  padding: 0 10px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f7f9fc;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 30px;
  text-align: center;
}

.onlyoffice-viewer-frame__stage {
  position: relative;
  height: 720px;
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fff;
}

.onlyoffice-viewer-frame__scaler {
  transform-origin: top left;
  transition: transform 0.16s ease;
}

.onlyoffice-viewer-frame__mount {
  height: 100%;
  overflow: hidden;
  background: #fff;
}

.onlyoffice-viewer-frame__mount--wheel-armed :deep(iframe) {
  pointer-events: none;
}

.onlyoffice-viewer-frame__wheel-mask {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 4;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(22, 119, 255, 0.12);
  border: 1px solid rgba(22, 119, 255, 0.28);
  color: #1677ff;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  pointer-events: none;
}

.onlyoffice-viewer-frame__overlay {
  position: absolute;
  inset: 72px 18px 18px;
  pointer-events: none;
  background-repeat: repeat;
  background-position: 0 0;
  border-radius: 6px;
  z-index: 2;
}

.onlyoffice-viewer-frame__watermark {
  position: absolute;
  right: 30px;
  bottom: 28px;
  z-index: 3;
  max-width: 360px;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(55, 65, 81, 0.58);
  border: 1px solid rgba(251, 191, 36, 0.9);
  color: #f9fafb;
  pointer-events: none;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.28);
  backdrop-filter: blur(6px);
  font-size: 12px;
  line-height: 18px;
}

.onlyoffice-viewer-frame__watermark-title {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.onlyoffice-viewer-frame__watermark-title span:first-child {
  color: #fcd34d;
  font-weight: 800;
}

.onlyoffice-viewer-frame__watermark-title span:last-child {
  color: #fdba74;
  font-weight: 700;
}
</style>
