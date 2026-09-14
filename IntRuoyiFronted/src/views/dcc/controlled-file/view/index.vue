<template>
  <div
    v-loading="loading"
    class="protected-viewer-shell"
    @copy="blockPreviewInteraction"
    @cut="blockPreviewInteraction"
    @paste="blockPreviewInteraction"
    @contextmenu="blockPreviewInteraction"
    @dragstart="blockPreviewInteraction"
  >
    <div class="protected-viewer-toolbar">
      <div>
        <div class="protected-viewer-title">{{ title || '受控文件预览' }}</div>
        <div class="protected-viewer-subtitle">
          当前预览会根据文件类型自动切换到 PDF、图片、文本、OnlyOffice、音频或视频只读视图，并继续保留受控阅读限制。
        </div>
      </div>
      <div class="protected-viewer-toolbar__actions">
        <div
          v-if="resolvedWatermarkText"
          class="protected-preview-badge"
          data-testid="protected-preview-badge"
        >
          <span>{{ resolvedWatermarkLabel }}</span>
          <span class="protected-preview-badge__hint">禁止截图/外传</span>
        </div>
        <slot name="actions"></slot>
      </div>
    </div>

    <el-alert
      v-if="errorMessage"
      :closable="false"
      class="mb-16px"
      show-icon
      type="error"
      :title="errorMessage"
    />

    <template v-if="resolvedPreviewKind === 'PDF'">
      <div v-if="pageEntries.length" ref="viewerFrameRef" class="protected-viewer-frame protected-viewer-frame--transformable">
        <div
          v-if="canTransformPreview"
          class="protected-viewer-transform-controls protected-viewer-transform-controls--sticky"
          data-testid="protected-viewer-transform-controls"
        >
          <span class="protected-viewer-transform-controls__status">
            {{ viewerZoomPercent }}% / {{ viewerRotationDegrees }}°
          </span>
          <div class="protected-viewer-transform-controls__grid">
            <el-button
              size="small"
              :disabled="viewerZoomPercent >= MAX_VIEWER_ZOOM_PERCENT"
              aria-label="放大"
              @click="handleZoomIn"
            >
              放大
            </el-button>
            <el-button
              size="small"
              :disabled="viewerZoomPercent <= MIN_VIEWER_ZOOM_PERCENT"
              aria-label="缩小"
              @click="handleZoomOut"
            >
              缩小
            </el-button>
            <el-button size="small" aria-label="旋转" @click="handleRotateRight">
              旋转
            </el-button>
            <el-button
              size="small"
              :disabled="viewerZoomPercent === DEFAULT_VIEWER_ZOOM_PERCENT && viewerRotationDegrees === 0"
              aria-label="复原"
              @click="handleResetViewerTransform"
            >
              复原
            </el-button>
          </div>
        </div>
        <div
          v-if="resolvedWatermarkText"
          class="protected-viewer-corner-watermark"
          data-testid="protected-preview-corner-watermark"
        >
          <div class="protected-viewer-corner-watermark__title">
            <span>{{ resolvedWatermarkLabel }}</span>
            <span class="protected-viewer-corner-watermark__hint">禁止截图/外传</span>
          </div>
          <div class="protected-viewer-corner-watermark__detail">
            {{ watermarkDetail }}
          </div>
        </div>

        <div v-for="page in pageEntries" :key="page.pageNumber" class="protected-viewer-page">
          <div class="protected-viewer-page__header">第 {{ page.pageNumber }} / {{ pageEntries.length }} 页</div>
          <div class="protected-viewer-page__canvas-wrap">
            <div class="protected-viewer-page__canvas-viewport" :style="getPdfPageViewportStyle(page)">
              <div class="protected-viewer-transform-stage" :style="pdfPreviewTransformStyle">
                <canvas
                  :ref="(element) => bindCanvasRef(page.pageNumber, element)"
                  class="protected-viewer-canvas"
                  :height="page.height"
                  :width="page.width"
                ></canvas>
                <div
                  v-if="watermarkBackground"
                  class="protected-viewer-page__overlay"
                  data-testid="protected-preview-watermark-overlay"
                  :style="{ backgroundImage: watermarkBackground }"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else-if="!loading && !errorMessage" description="暂无可预览的 PDF 内容" />
    </template>

    <template v-else-if="resolvedPreviewKind === 'IMAGE'">
      <div v-if="mediaObjectUrl" class="protected-viewer-frame protected-viewer-frame--media protected-viewer-frame--transformable">
        <div
          v-if="canTransformPreview"
          class="protected-viewer-transform-controls protected-viewer-transform-controls--sticky"
          data-testid="protected-viewer-transform-controls"
        >
          <span class="protected-viewer-transform-controls__status">
            {{ viewerZoomPercent }}% / {{ viewerRotationDegrees }}°
          </span>
          <div class="protected-viewer-transform-controls__grid">
            <el-button
              size="small"
              :disabled="viewerZoomPercent >= MAX_VIEWER_ZOOM_PERCENT"
              aria-label="放大"
              @click="handleZoomIn"
            >
              放大
            </el-button>
            <el-button
              size="small"
              :disabled="viewerZoomPercent <= MIN_VIEWER_ZOOM_PERCENT"
              aria-label="缩小"
              @click="handleZoomOut"
            >
              缩小
            </el-button>
            <el-button size="small" aria-label="旋转" @click="handleRotateRight">
              旋转
            </el-button>
            <el-button
              size="small"
              :disabled="viewerZoomPercent === DEFAULT_VIEWER_ZOOM_PERCENT && viewerRotationDegrees === 0"
              aria-label="复原"
              @click="handleResetViewerTransform"
            >
              复原
            </el-button>
          </div>
        </div>
        <div
          v-if="resolvedWatermarkText"
          class="protected-viewer-corner-watermark"
          data-testid="protected-preview-corner-watermark"
        >
          <div class="protected-viewer-corner-watermark__title">
            <span>{{ resolvedWatermarkLabel }}</span>
            <span class="protected-viewer-corner-watermark__hint">禁止截图/外传</span>
          </div>
          <div class="protected-viewer-corner-watermark__detail">
            {{ watermarkDetail }}
          </div>
        </div>
        <div class="protected-viewer-image-wrap">
          <div class="protected-viewer-transform-stage" :style="imagePreviewTransformStyle">
            <img class="protected-viewer-image" :src="mediaObjectUrl" :alt="resolvedFileName || '受控图片预览'" />
            <div
              v-if="watermarkBackground"
              class="protected-viewer-page__overlay"
              data-testid="protected-preview-watermark-overlay"
              :style="{ backgroundImage: watermarkBackground }"
            ></div>
          </div>
        </div>
      </div>
      <el-empty v-else-if="!loading && !errorMessage" description="暂无可预览的图片内容" />
    </template>

    <template v-else-if="resolvedPreviewKind === 'VIDEO'">
      <div v-if="mediaObjectUrl" class="protected-viewer-frame protected-viewer-frame--media">
        <div
          v-if="resolvedWatermarkText"
          class="protected-viewer-corner-watermark"
          data-testid="protected-preview-corner-watermark"
        >
          <div class="protected-viewer-corner-watermark__title">
            <span>{{ resolvedWatermarkLabel }}</span>
            <span class="protected-viewer-corner-watermark__hint">禁止截图/外传</span>
          </div>
          <div class="protected-viewer-corner-watermark__detail">
            {{ watermarkDetail }}
          </div>
        </div>
        <div class="protected-viewer-media-wrap">
          <video
            class="protected-viewer-video"
            :src="mediaObjectUrl"
            controls
            controlslist="nodownload noplaybackrate"
            disablepictureinpicture
          ></video>
          <div
            v-if="watermarkBackground"
            class="protected-viewer-page__overlay"
            data-testid="protected-preview-watermark-overlay"
            :style="{ backgroundImage: watermarkBackground }"
          ></div>
        </div>
      </div>
      <el-empty v-else-if="!loading && !errorMessage" description="暂无可预览的视频内容" />
    </template>

    <template v-else-if="resolvedPreviewKind === 'AUDIO'">
      <div v-if="mediaObjectUrl" class="protected-viewer-frame protected-viewer-frame--media protected-viewer-frame--audio">
        <div
          v-if="resolvedWatermarkText"
          class="protected-viewer-corner-watermark"
          data-testid="protected-preview-corner-watermark"
        >
          <div class="protected-viewer-corner-watermark__title">
            <span>{{ resolvedWatermarkLabel }}</span>
            <span class="protected-viewer-corner-watermark__hint">禁止截图/外传</span>
          </div>
          <div class="protected-viewer-corner-watermark__detail">
            {{ watermarkDetail }}
          </div>
        </div>
        <div class="protected-viewer-audio-card">
          <div class="protected-viewer-audio-title">{{ resolvedFileName || title || '受控音频预览' }}</div>
          <audio
            class="protected-viewer-audio"
            :src="mediaObjectUrl"
            controls
            controlslist="nodownload noplaybackrate"
          ></audio>
          <div
            v-if="watermarkBackground"
            class="protected-viewer-page__overlay"
            data-testid="protected-preview-watermark-overlay"
            :style="{ backgroundImage: watermarkBackground }"
          ></div>
        </div>
      </div>
      <el-empty v-else-if="!loading && !errorMessage" description="暂无可预览的音频内容" />
    </template>

    <template v-else-if="resolvedPreviewKind === 'TEXT'">
      <div v-if="textPreviewContent" class="protected-viewer-frame protected-viewer-frame--text">
        <div
          v-if="resolvedWatermarkText"
          class="protected-viewer-corner-watermark"
          data-testid="protected-preview-corner-watermark"
        >
          <div class="protected-viewer-corner-watermark__title">
            <span>{{ resolvedWatermarkLabel }}</span>
            <span class="protected-viewer-corner-watermark__hint">禁止截图/外传</span>
          </div>
          <div class="protected-viewer-corner-watermark__detail">
            {{ watermarkDetail }}
          </div>
        </div>
        <div class="protected-viewer-text-wrap">
          <pre class="protected-viewer-text">{{ textPreviewContent }}</pre>
          <div
            v-if="watermarkBackground"
            class="protected-viewer-page__overlay"
            data-testid="protected-preview-watermark-overlay"
            :style="{ backgroundImage: watermarkBackground }"
          ></div>
        </div>
      </div>
      <el-empty v-else-if="!loading && !errorMessage" description="暂无可预览的文本内容" />
    </template>

    <template v-else-if="resolvedPreviewKind === 'OFFICE'">
      <OnlyOfficeReadOnlyViewer
        :base-url="resolvedOnlyOfficeBaseUrl"
        :document-url="resolvedOnlyOfficeDocumentUrl"
        :document-title="resolvedFileName || title || 'Office 受控预览'"
        :watermark-label="resolvedWatermarkLabel"
        :watermark-detail="watermarkDetail"
        :watermark-background="watermarkBackground"
        :unavailable-reason="resolvedPreviewUnavailableReason"
      />
    </template>

    <template v-else-if="resolvedPreviewKind === 'DOWNLOAD_ONLY' && !errorMessage">
      <el-empty description="当前文件类型仅支持下载，不提供在线预览" />
    </template>

    <el-empty
      v-else-if="!loading && !errorMessage"
      description="暂无可预览的文件内容"
    />
  </div>
</template>

<script lang="ts" setup>
import type {
  ControlledFilePreviewMetadataVO,
  ControlledFilePreviewKind,
  ControlledPreviewWatermark
} from '@/api/dcc/controlledFile/workflow'
import {
  buildDccControlledFilePreviewSource,
  getOnlineFilePreviewMetadata,
  previewOnlineFileWithWatermark,
  type OnlineFilePreviewSource
} from '@/api/common/filePreview'
import { getDocument, GlobalWorkerOptions } from './vendor/pdf.min.mjs'
import {
  buildPreviewWatermarkBackground,
  buildPreviewWatermarkDetail,
  drawControlledPreviewStamp,
  getPreviewWatermarkBadgeLabel,
  getProtectedViewerRenderScale,
  resolveViewerErrorMessage,
  shouldBlockPreviewShortcut
} from './presentation'
import OnlyOfficeReadOnlyViewer from './OnlyOfficeReadOnlyViewer.vue'

defineOptions({ name: 'ProtectedPdfViewer' })

const protectedViewerWorkerBaseUrl = import.meta.env.BASE_URL.endsWith('/')
  ? import.meta.env.BASE_URL
  : `${import.meta.env.BASE_URL}/`

GlobalWorkerOptions.workerSrc = `${protectedViewerWorkerBaseUrl}pdfjs/pdf.worker.mjs`

const props = defineProps<{
  controlledFileId?: number | string | null
  previewSource?: OnlineFilePreviewSource | null
  previewBlob?: Blob | Uint8Array | null
  previewKind?: ControlledFilePreviewKind | null
  onlyofficeBaseUrl?: string
  onlyofficeDocumentUrl?: string
  previewUnavailableReason?: string
  watermark?: ControlledPreviewWatermark | null
  title?: string
}>()

interface ViewerCanvasPageProxy {
  getViewport: (params: { scale: number }) => { width: number; height: number }
  render: (params: {
    canvasContext: CanvasRenderingContext2D
    viewport: { width: number; height: number }
  }) => { promise: Promise<void> }
  cleanup?: () => void
}

interface ViewerCanvasDocumentProxy {
  numPages: number
  getPage: (pageNumber: number) => Promise<ViewerCanvasPageProxy>
  cleanup?: () => Promise<void> | void
  destroy?: () => Promise<void> | void
}

interface ViewerLoadingTask {
  destroy?: () => Promise<void> | void
}

const loading = ref(false)
const errorMessage = ref('')
const viewerFrameRef = ref<HTMLDivElement>()
const pageEntries = ref<Array<{ pageNumber: number; width: number; height: number }>>([])
const canvasRefMap = new Map<number, HTMLCanvasElement>()
const renderVersion = ref(0)
const pdfLoadingTask = shallowRef<ViewerLoadingTask | null>(null)
const pdfDocument = shallowRef<ViewerCanvasDocumentProxy | null>(null)
const resolvedPreviewMetadata = shallowRef<ControlledFilePreviewMetadataVO | null>(null)
const resolvedWatermark = shallowRef<ControlledPreviewWatermark | null>(null)
const resolvedPreviewKind = ref<ControlledFilePreviewKind | ''>('')
const resolvedOnlyOfficeBaseUrl = ref('')
const resolvedOnlyOfficeDocumentUrl = ref('')
const resolvedPreviewUnavailableReason = ref('')
const resolvedFileName = ref('')
const mediaObjectUrl = ref('')
const textPreviewContent = ref('')
const currentPdfBytes = shallowRef<Uint8Array | null>(null)
const MIN_VIEWER_ZOOM_PERCENT = 50
const MAX_VIEWER_ZOOM_PERCENT = 250
const VIEWER_ZOOM_STEP_PERCENT = 25
const DEFAULT_VIEWER_ZOOM_PERCENT = 100
const viewerZoomPercent = ref(DEFAULT_VIEWER_ZOOM_PERCENT)
const viewerRotationDegrees = ref(0)

const resolvedWatermarkText = computed(() => String(resolvedWatermark.value?.text || '').trim())
const resolvedWatermarkLabel = computed(() => getPreviewWatermarkBadgeLabel(resolvedWatermark.value))
const watermarkDetail = computed(() => buildPreviewWatermarkDetail(resolvedWatermark.value))
const watermarkBackground = computed(() => buildPreviewWatermarkBackground(resolvedWatermark.value))
const canTransformPreview = computed(() => ['PDF', 'IMAGE'].includes(resolvedPreviewKind.value))
const viewerScale = computed(() => viewerZoomPercent.value / 100)
const previewTransform = computed(
  () => `scale(${viewerScale.value}) rotate(${viewerRotationDegrees.value}deg)`
)
const pdfPreviewTransformStyle = computed(() => ({
  transform: previewTransform.value,
  transformOrigin: 'center center'
}))
const imagePreviewTransformStyle = computed(() => ({
  transform: previewTransform.value,
  transformOrigin: 'center center'
}))
const resolvedOnlinePreviewSource = computed<OnlineFilePreviewSource | null>(() => {
  if (props.previewSource) {
    return props.previewSource
  }
  if (props.controlledFileId !== undefined && props.controlledFileId !== null && props.controlledFileId !== '') {
    return buildDccControlledFilePreviewSource(props.controlledFileId)
  }
  return null
})

const bindCanvasRef = (pageNumber: number, element: unknown) => {
  if (element instanceof HTMLCanvasElement) {
    canvasRefMap.set(pageNumber, element)
    return
  }
  canvasRefMap.delete(pageNumber)
}

const blockPreviewInteraction = (event: Event) => {
  event.preventDefault()
  event.stopPropagation()
}

const resetViewerTransformState = () => {
  viewerZoomPercent.value = DEFAULT_VIEWER_ZOOM_PERCENT
  viewerRotationDegrees.value = 0
}

const handleZoomOut = () => {
  if (viewerZoomPercent.value <= MIN_VIEWER_ZOOM_PERCENT) {
    return
  }
  viewerZoomPercent.value = Math.max(
    MIN_VIEWER_ZOOM_PERCENT,
    viewerZoomPercent.value - VIEWER_ZOOM_STEP_PERCENT
  )
}

const handleZoomIn = () => {
  if (viewerZoomPercent.value >= MAX_VIEWER_ZOOM_PERCENT) {
    return
  }
  viewerZoomPercent.value = Math.min(
    MAX_VIEWER_ZOOM_PERCENT,
    viewerZoomPercent.value + VIEWER_ZOOM_STEP_PERCENT
  )
}

const normalizeRotationDegrees = (degrees: number) => ((degrees % 360) + 360) % 360

const handleRotateRight = () => {
  viewerRotationDegrees.value = normalizeRotationDegrees(viewerRotationDegrees.value + 90)
}

const handleResetViewerTransform = () => {
  resetViewerTransformState()
}

const getPdfPageViewportStyle = (page: { width: number; height: number }) => {
  const scale = viewerScale.value
  const isQuarterTurn = viewerRotationDegrees.value % 180 !== 0
  const width = isQuarterTurn ? page.height : page.width
  const height = isQuarterTurn ? page.width : page.height
  return {
    width: `${Math.ceil(width * scale)}px`,
    height: `${Math.ceil(height * scale)}px`
  }
}

const normalizePreviewKind = (value?: ControlledFilePreviewKind | null) => {
  if (value) {
    return value
  }
  if (props.previewBlob) {
    return 'PDF' as ControlledFilePreviewKind
  }
  return '' as ControlledFilePreviewKind | ''
}

const withTraceableWatermark = (
  watermark: ControlledPreviewWatermark | null | undefined,
  traceCode?: string | null
) => {
  if (!watermark) {
    return watermark || null
  }
  const resolvedTraceCode = String(traceCode || watermark.traceCode || '').trim()
  if (!resolvedTraceCode) {
    return watermark
  }
  return {
    ...watermark,
    traceCode: resolvedTraceCode
  }
}

const revokeMediaObjectUrl = () => {
  if (mediaObjectUrl.value) {
    URL.revokeObjectURL(mediaObjectUrl.value)
    mediaObjectUrl.value = ''
  }
}

const destroyPdfArtifacts = async () => {
  pageEntries.value = []
  canvasRefMap.clear()
  if (pdfDocument.value?.cleanup) {
    await pdfDocument.value.cleanup()
  }
  if (pdfDocument.value?.destroy) {
    await pdfDocument.value.destroy()
  } else if (pdfLoadingTask.value?.destroy) {
    await pdfLoadingTask.value.destroy()
  }
  pdfDocument.value = null
  pdfLoadingTask.value = null
}

const resetPreviewState = async () => {
  revokeMediaObjectUrl()
  textPreviewContent.value = ''
  currentPdfBytes.value = null
  await destroyPdfArtifacts()
}

const resolvePreviewBlob = async () => {
  if (props.previewBlob instanceof Uint8Array) {
    return { bytes: props.previewBlob, blob: new Blob([props.previewBlob]) }
  }
  if (props.previewBlob instanceof Blob) {
    return {
      bytes: new Uint8Array(await props.previewBlob.arrayBuffer()),
      blob: props.previewBlob
    }
  }
  if (resolvedOnlinePreviewSource.value) {
    const preview = await previewOnlineFileWithWatermark(
      resolvedOnlinePreviewSource.value,
      resolvedPreviewMetadata.value || undefined
    )
    resolvedWatermark.value = withTraceableWatermark(
      preview.watermark,
      resolvedWatermark.value?.traceCode
    )
    return {
      bytes: new Uint8Array(await preview.blob.arrayBuffer()),
      blob: preview.blob
    }
  }
  return null
}

const clonePdfBytesForWorker = (pdfBytes: Uint8Array) => pdfBytes.slice()

const renderPdfPages = async (
  pdfBytes: Uint8Array,
  currentRenderVersion: number,
  scale: number
) => {
  const loadingTask = getDocument({
    data: clonePdfBytesForWorker(pdfBytes),
    useWorkerFetch: false,
    isEvalSupported: false,
    enableXfa: false,
    stopAtErrors: true
  })
  pdfLoadingTask.value = loadingTask
  const loadedDocument = (await loadingTask.promise) as ViewerCanvasDocumentProxy
  if (currentRenderVersion !== renderVersion.value) {
    await loadingTask.destroy?.()
    return
  }
  pdfDocument.value = loadedDocument
  const nextPageEntries: Array<{ pageNumber: number; width: number; height: number }> = []
  for (let pageNumber = 1; pageNumber <= loadedDocument.numPages; pageNumber += 1) {
    const page = await loadedDocument.getPage(pageNumber)
    const viewport = page.getViewport({ scale })
    nextPageEntries.push({
      pageNumber,
      width: Math.ceil(viewport.width),
      height: Math.ceil(viewport.height)
    })
    page.cleanup?.()
  }
  pageEntries.value = nextPageEntries
  await nextTick()
  const outputScale = window.devicePixelRatio || 1
  for (const pageEntry of pageEntries.value) {
    if (currentRenderVersion !== renderVersion.value) {
      return
    }
    const page = await loadedDocument.getPage(pageEntry.pageNumber)
    const canvas = canvasRefMap.get(pageEntry.pageNumber)
    if (!canvas) {
      continue
    }
    const viewport = page.getViewport({ scale })
    const renderViewport = page.getViewport({ scale: scale * outputScale })
    canvas.width = Math.ceil(renderViewport.width)
    canvas.height = Math.ceil(renderViewport.height)
    canvas.style.width = `${Math.ceil(viewport.width)}px`
    canvas.style.height = `${Math.ceil(viewport.height)}px`
    const context = canvas.getContext('2d', { alpha: false })
    if (!context) {
      continue
    }
    await page.render({
      canvasContext: context,
      viewport: renderViewport
    }).promise
    drawControlledPreviewStamp(context, viewport, resolvedWatermark.value, outputScale)
    page.cleanup?.()
  }
}

const loadPreview = async () => {
  const currentRenderVersion = renderVersion.value + 1
  renderVersion.value = currentRenderVersion
  loading.value = true
  errorMessage.value = ''
  resetViewerTransformState()
  resolvedWatermark.value = props.watermark || null
  resolvedPreviewMetadata.value = null
  resolvedPreviewKind.value = ''
  resolvedOnlyOfficeBaseUrl.value = props.onlyofficeBaseUrl || ''
  resolvedOnlyOfficeDocumentUrl.value = props.onlyofficeDocumentUrl || ''
  resolvedPreviewUnavailableReason.value = props.previewUnavailableReason || ''
  resolvedFileName.value = props.title || ''
  await resetPreviewState()
  try {
    if (resolvedOnlinePreviewSource.value && !props.previewKind) {
      const metadata = await getOnlineFilePreviewMetadata(resolvedOnlinePreviewSource.value)
      resolvedPreviewMetadata.value = metadata
      resolvedPreviewKind.value = metadata.previewKind
      resolvedOnlyOfficeBaseUrl.value = metadata.onlyofficeBaseUrl || ''
      resolvedOnlyOfficeDocumentUrl.value = metadata.onlyofficeDocumentUrl || ''
      resolvedPreviewUnavailableReason.value = metadata.previewUnavailableReason || ''
      resolvedWatermark.value =
        withTraceableWatermark(metadata.watermark || resolvedWatermark.value, metadata.watermarkTraceCode) ||
        resolvedWatermark.value
      resolvedFileName.value = metadata.fileName || resolvedFileName.value
    } else {
      resolvedPreviewKind.value = normalizePreviewKind(props.previewKind)
    }

    if (resolvedPreviewUnavailableReason.value) {
      if (resolvedPreviewKind.value !== 'OFFICE') {
        errorMessage.value = resolvedPreviewUnavailableReason.value
      }
      return
    }

    if (resolvedPreviewKind.value === 'OFFICE' || resolvedPreviewKind.value === 'DOWNLOAD_ONLY') {
      return
    }

    const previewPayload = await resolvePreviewBlob()
    if (!previewPayload) {
      return
    }

    if (['IMAGE', 'VIDEO', 'AUDIO'].includes(resolvedPreviewKind.value)) {
      mediaObjectUrl.value = URL.createObjectURL(previewPayload.blob)
      return
    }

    if (resolvedPreviewKind.value === 'TEXT') {
      textPreviewContent.value = new TextDecoder().decode(previewPayload.bytes)
      return
    }

    const scale = getProtectedViewerRenderScale(viewerFrameRef.value?.clientWidth)
    currentPdfBytes.value = previewPayload.bytes
    await renderPdfPages(previewPayload.bytes, currentRenderVersion, scale)
  } catch (error) {
    errorMessage.value = resolveViewerErrorMessage(error, '受控预览加载失败，请查看错误提示后重试。')
  } finally {
    if (currentRenderVersion === renderVersion.value) {
      loading.value = false
    }
  }
}

const handleWindowKeydown = (event: KeyboardEvent) => {
  if (shouldBlockPreviewShortcut(event)) {
    blockPreviewInteraction(event)
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleWindowKeydown, true)
})

onBeforeUnmount(() => {
  renderVersion.value += 1
  void resetPreviewState()
  window.removeEventListener('keydown', handleWindowKeydown, true)
})

watch(
  () => [
    props.controlledFileId,
    props.previewSource,
    props.previewBlob,
    props.previewKind,
    props.onlyofficeBaseUrl,
    props.onlyofficeDocumentUrl,
    props.previewUnavailableReason,
    props.watermark?.text,
    props.watermark?.traceCode
  ],
  () => {
    loadPreview()
  },
  { immediate: true }
)
</script>

<style scoped>
.protected-viewer-shell {
  min-height: 560px;
  user-select: none;
  -webkit-user-select: none;
}

.protected-viewer-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.protected-viewer-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.protected-viewer-title {
  color: #172033;
  font-size: 18px;
  font-weight: 600;
  line-height: 26px;
}

.protected-viewer-subtitle {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}

.protected-preview-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #fff7ed;
  border: 1px solid #fdba74;
  color: #9a3412;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.protected-preview-badge__hint {
  color: #c2410c;
}

.protected-viewer-transform-controls {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 128px;
  padding: 6px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
}

.protected-viewer-transform-controls--sticky {
  position: sticky;
  top: 0;
  z-index: 12;
  align-self: flex-end;
  box-shadow: 0 8px 20px rgb(23 32 51 / 10%);
}

.protected-viewer-transform-controls__status {
  color: #172033;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.protected-viewer-transform-controls__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
}

.protected-viewer-transform-controls__grid :deep(.el-button) {
  width: 100%;
  min-width: 0;
  padding-right: 0;
  padding-left: 0;
}

.protected-viewer-transform-controls__grid :deep(.el-button + .el-button) {
  margin-left: 0;
}

.protected-viewer-frame {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 18px;
  overflow: auto;
  padding: 18px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f4f7fb;
}

.protected-viewer-frame--transformable {
  max-height: calc(100vh - 180px);
  overscroll-behavior: contain;
}

.protected-viewer-frame--media,
.protected-viewer-frame--text {
  min-height: 480px;
}

.protected-viewer-frame--audio {
  justify-content: center;
}

.protected-viewer-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
}

.protected-viewer-page__header {
  align-self: stretch;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
  text-align: right;
}

.protected-viewer-page__canvas-wrap,
.protected-viewer-image-wrap,
.protected-viewer-media-wrap,
.protected-viewer-text-wrap {
  position: relative;
  display: inline-flex;
  width: 100%;
  justify-content: center;
}

.protected-viewer-page__canvas-wrap,
.protected-viewer-image-wrap {
  overflow: visible;
  padding: 24px;
}

.protected-viewer-page__canvas-viewport {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: width 0.16s ease, height 0.16s ease;
}

.protected-viewer-transform-stage {
  position: relative;
  display: inline-flex;
  justify-content: center;
  transition: transform 0.16s ease;
}

.protected-viewer-canvas,
.protected-viewer-image,
.protected-viewer-video,
.protected-viewer-audio-card {
  max-width: 100%;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 8px 24px rgb(23 32 51 / 8%);
}

.protected-viewer-canvas {
  transition: transform 0.16s ease;
}

.protected-viewer-video {
  width: min(100%, 960px);
  max-height: 70vh;
}

.protected-viewer-audio-card {
  position: relative;
  width: min(100%, 760px);
  padding: 22px;
}

.protected-viewer-audio-title {
  margin-bottom: 14px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  word-break: break-word;
}

.protected-viewer-audio {
  position: relative;
  z-index: 1;
  width: 100%;
}

.protected-viewer-text {
  width: 100%;
  min-height: 420px;
  margin: 0;
  padding: 18px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 8px 24px rgb(23 32 51 / 8%);
  white-space: pre-wrap;
  word-break: break-word;
  color: #172033;
  font-size: 13px;
  line-height: 20px;
}

.protected-viewer-page__overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-repeat: repeat;
  background-position: 0 0;
  border-radius: 6px;
}

.protected-viewer-corner-watermark {
  position: sticky;
  bottom: 16px;
  margin-left: auto;
  max-width: min(420px, calc(100% - 32px));
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(55, 65, 81, 0.58);
  border: 1px solid rgba(251, 191, 36, 0.9);
  color: #f9fafb;
  display: flex;
  flex-direction: column;
  gap: 6px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.28);
  pointer-events: none;
  z-index: 7;
  backdrop-filter: blur(6px);
}

.protected-viewer-corner-watermark__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.protected-viewer-corner-watermark__title span:first-child {
  font-size: 13px;
  font-weight: 800;
  color: #fcd34d;
}

.protected-viewer-corner-watermark__hint {
  font-size: 12px;
  font-weight: 700;
  color: #fdba74;
}

.protected-viewer-corner-watermark__detail {
  font-size: 12px;
  line-height: 1.45;
  color: rgba(255, 255, 255, 0.92);
  word-break: break-all;
}
</style>
