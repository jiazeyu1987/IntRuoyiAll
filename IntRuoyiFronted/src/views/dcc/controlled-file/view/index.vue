<template>
  <div
    v-loading="loading"
    class="protected-viewer-shell"
    @copy="blockPreviewInteraction"
    @cut="blockPreviewInteraction"
    @paste="blockPreviewInteraction"
    @contextmenu="blockPreviewInteraction"
    @dragstart="blockPreviewInteraction"
    @scroll.capture="handlePdfScroll"
  >
    <div class="protected-viewer-toolbar">
      <div>
        <div class="protected-viewer-title">{{ title || '受控文件预览' }}</div>
        <div class="protected-viewer-subtitle">
          当前预览会根据文件类型自动切换到 PDF、图片、文本、OnlyOffice、音频或视频只读视图，并继续保留受控阅读限制。
        </div>
      </div>
      <div class="protected-viewer-toolbar__actions">
        <span v-if="registeredPreviewVisible" class="protected-preview-registered-at" data-testid="preview-registered-at" aria-live="polite">
          本次预览登记时间：{{ registeredPreviewLabel }}
        </span>
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

    <div v-if="readingSupported" class="protected-viewer-reading-summary" data-testid="preview-reading-summary" role="status" aria-live="polite">
      <template v-if="readingSummary">
        <span>累计有效阅读时间：{{ readingSummary.accumulatedSeconds }} 秒</span>
        <span>首次打开：{{ formatReadingOpenedAt(readingSummary.firstOpenedAt) }}</span>
        <span>最近打开：{{ formatReadingOpenedAt(readingSummary.lastOpenedAt) }}</span>
      </template>
      <span v-else>{{ readingError ? '阅读统计未取得，请查看错误提示' : '正在读取服务端阅读统计…' }}</span>
    </div>
    <div v-if="readingError" class="protected-viewer-reading-error" data-testid="preview-reading-error">
      <el-alert :closable="false" show-icon type="error" :title="'阅读计时或统计失败：' + readingError" />
      <el-button size="small" @click="loadPreview">重新加载正文后重试计时</el-button>
    </div>
    <el-alert
      v-if="readingCloseError"
      :closable="false"
      class="mb-16px"
      show-icon
      type="error"
      data-testid="preview-reading-close-error"
      :title="'上一正文阅读会话结束未确认：' + readingCloseError"
    />

    <el-alert
      v-if="errorMessage"
      :closable="false"
      class="mb-16px"
      show-icon
      type="error"
      :title="errorMessage"
    />

    <template v-if="resolvedPreviewKind === 'PDF'">
      <div v-if="pageEntries.length" class="protected-viewer-pdf-tools" data-testid="protected-viewer-pdf-tools">
        <div class="protected-viewer-pdf-tools__row">
          <el-button size="small" aria-label="上一页" :disabled="pdfCurrentPage <= 1" @click="goToPdfPage(pdfCurrentPage - 1)">上一页</el-button>
          <span aria-live="polite">第 {{ pdfCurrentPage }} / {{ pageEntries.length }} 页</span>
          <el-button size="small" aria-label="下一页" :disabled="pdfCurrentPage >= pageEntries.length" @click="goToPdfPage(pdfCurrentPage + 1)">下一页</el-button>
          <el-input v-model="pdfPageInput" class="protected-viewer-pdf-tools__page" size="small" aria-label="PDF 页码" inputmode="numeric" @keyup.enter="handlePdfPageJump" />
          <el-button size="small" aria-label="跳转页码" @click="handlePdfPageJump">跳转</el-button>
          <span v-if="pdfNavigationError" role="alert" class="protected-viewer-pdf-tools__error">{{ pdfNavigationError }}</span>
        </div>
        <div class="protected-viewer-pdf-tools__row">
          <el-input v-model="pdfSearchQuery" class="protected-viewer-pdf-tools__query" size="small" aria-label="PDF 正文关键字" :disabled="!pdfCanSearch" placeholder="搜索 PDF 正文" @keyup.enter="handlePdfSearch" />
          <el-button size="small" aria-label="搜索 PDF 正文" :disabled="!pdfCanSearch || !pdfSearchQuery.trim()" @click="handlePdfSearch">搜索</el-button>
          <el-button size="small" aria-label="上一处匹配" :disabled="!pdfSearchMatches.length" @click="handlePdfSearchPrevious">上一处</el-button>
          <el-button size="small" aria-label="下一处匹配" :disabled="!pdfSearchMatches.length" @click="handlePdfSearchNext">下一处</el-button>
          <span role="status" aria-live="polite">{{ pdfSearchMessage }}</span>
        </div>
        <div v-if="pdfSearchMatches[pdfSearchMatchIndex]" class="protected-viewer-pdf-tools__excerpt">
          第 {{ pdfSearchMatches[pdfSearchMatchIndex].pageNumber }} 页：{{ pdfSearchMatches[pdfSearchMatchIndex].excerpt }}
        </div>
        <div v-if="pdfSearchError" role="alert" class="protected-viewer-pdf-tools__error">{{ pdfSearchError }}</div>
      </div>
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

        <div v-for="page in pageEntries" :key="`${renderVersion}-${page.pageNumber}`" :ref="(element) => bindPdfPageRef(page.pageNumber, element)" class="protected-viewer-page">
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
        <div ref="flatPreviewContainerRef" class="protected-viewer-image-wrap protected-viewer-flat-container">
          <div class="protected-viewer-flat-viewport" :style="flatPreviewViewportStyle">
            <div ref="flatPreviewStageRef" class="protected-viewer-transform-stage protected-viewer-flat-stage" :style="flatPreviewTransformStyle">
              <img
                :key="renderVersion" :data-preview-generation="renderVersion" @load="handleMediaLoaded" @error="handleMediaError"
                class="protected-viewer-image" :src="mediaObjectUrl" :alt="resolvedFileName || '受控图片预览'" />
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
      <el-empty v-else-if="!loading && !errorMessage" description="暂无可预览的图片内容" />
    </template>

    <template v-else-if="resolvedPreviewKind === 'VIDEO'">
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
            <el-button size="small" :disabled="viewerZoomPercent >= MAX_VIEWER_ZOOM_PERCENT" aria-label="放大" @click="handleZoomIn">放大</el-button>
            <el-button size="small" :disabled="viewerZoomPercent <= MIN_VIEWER_ZOOM_PERCENT" aria-label="缩小" @click="handleZoomOut">缩小</el-button>
            <el-button size="small" aria-label="旋转" @click="handleRotateRight">旋转</el-button>
            <el-button size="small" :disabled="viewerZoomPercent === DEFAULT_VIEWER_ZOOM_PERCENT && viewerRotationDegrees === 0" aria-label="复原" @click="handleResetViewerTransform">复原</el-button>
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
        <div ref="flatPreviewContainerRef" class="protected-viewer-media-wrap protected-viewer-flat-container">
          <div class="protected-viewer-flat-viewport" :style="flatPreviewViewportStyle">
            <div ref="flatPreviewStageRef" class="protected-viewer-transform-stage protected-viewer-flat-stage" :style="flatPreviewTransformStyle">
              <video
                :key="renderVersion" :data-preview-generation="renderVersion" @loadeddata="handleMediaLoaded" @error="handleMediaError"
                class="protected-viewer-video"
                :src="mediaObjectUrl"
                controls
                controlslist="nodownload noplaybackrate nofullscreen noremoteplayback"
                disableremoteplayback
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
            :key="renderVersion" :data-preview-generation="renderVersion" @loadeddata="handleMediaLoaded" @error="handleMediaError"
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
      <div v-if="textPreviewContent" class="protected-viewer-frame protected-viewer-frame--text protected-viewer-frame--transformable">
        <div class="protected-viewer-text-tools" data-testid="protected-viewer-text-tools">
          <div class="protected-viewer-text-tools__row">
            <el-button size="small" aria-label="上一页" :disabled="textCurrentPage <= 1" @click="goToTextPage(textCurrentPage - 1)">上一页</el-button>
            <span aria-live="polite">第 {{ textCurrentPage }} / {{ textPageEntries.length }} 页（每 {{ TEXT_LINES_PER_PAGE }} 行）</span>
            <el-button size="small" aria-label="下一页" :disabled="textCurrentPage >= textPageEntries.length" @click="goToTextPage(textCurrentPage + 1)">下一页</el-button>
            <el-input v-model="textPageInput" class="protected-viewer-text-tools__page" size="small" aria-label="文本页码" inputmode="numeric" @keyup.enter="handleTextPageJump" />
            <el-button size="small" aria-label="跳转页码" @click="handleTextPageJump">跳转</el-button>
            <span v-if="textNavigationError" role="alert">{{ textNavigationError }}</span>
          </div>
          <div class="protected-viewer-text-tools__row">
            <el-input v-model="textSearchQuery" class="protected-viewer-text-tools__query" size="small" aria-label="文本正文关键字" placeholder="搜索文本正文" @keyup.enter="handleTextSearch" />
            <el-button size="small" aria-label="搜索文本正文" :disabled="!textSearchQuery.trim()" @click="handleTextSearch">搜索</el-button>
            <el-button size="small" aria-label="上一处匹配" :disabled="!textSearchMatches.length" @click="moveTextSearchMatch(-1)">上一处</el-button>
            <el-button size="small" aria-label="下一处匹配" :disabled="!textSearchMatches.length" @click="moveTextSearchMatch(1)">下一处</el-button>
            <span role="status" aria-live="polite">{{ textSearchMessage }}</span>
          </div>
        </div>
        <div v-if="canTransformPreview" class="protected-viewer-transform-controls protected-viewer-transform-controls--sticky" data-testid="protected-viewer-transform-controls">
          <span class="protected-viewer-transform-controls__status">{{ viewerZoomPercent }}% / {{ viewerRotationDegrees }}°</span>
          <div class="protected-viewer-transform-controls__grid">
            <el-button size="small" :disabled="viewerZoomPercent >= MAX_VIEWER_ZOOM_PERCENT" aria-label="放大" @click="handleZoomIn">放大</el-button>
            <el-button size="small" :disabled="viewerZoomPercent <= MIN_VIEWER_ZOOM_PERCENT" aria-label="缩小" @click="handleZoomOut">缩小</el-button>
            <el-button size="small" aria-label="旋转" @click="handleRotateRight">旋转</el-button>
            <el-button size="small" :disabled="viewerZoomPercent === DEFAULT_VIEWER_ZOOM_PERCENT && viewerRotationDegrees === 0" aria-label="复原" @click="handleResetViewerTransform">复原</el-button>
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
        <div ref="flatPreviewContainerRef" class="protected-viewer-text-wrap protected-viewer-flat-container">
          <div class="protected-viewer-flat-viewport" :style="flatPreviewViewportStyle">
            <div ref="flatPreviewStageRef" class="protected-viewer-transform-stage protected-viewer-flat-stage" :style="flatPreviewTransformStyle">
              <pre class="protected-viewer-text"><template v-for="segment in textPageSegments" :key="segment.startIndex"><mark v-if="segment.match" :ref="bindTextSearchMatchRef" :data-active-match="segment.active ? 'true' : undefined" :class="{ 'protected-viewer-text-match--active': segment.active }">{{ segment.text }}</mark><template v-else>{{ segment.text }}</template></template></pre>
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
      <el-empty v-else-if="!loading && !errorMessage" description="暂无可预览的文本内容" />
    </template>

    <template v-else-if="resolvedPreviewKind === 'OFFICE'">
      <OnlyOfficeReadOnlyViewer
        :key="renderVersion"
        :base-url="resolvedOnlyOfficeBaseUrl"
        :document-url="resolvedOnlyOfficeDocumentUrl"
        @opened="handleOfficeOpened"
        @error="handleOfficeError"
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
import { extractPdfTextPages, findPdfTextMatches, type PdfTextMatch, type PdfTextPage } from './pdf-tools'
import { createPreviewReadingApi, type PreviewReadingScope, type PreviewReadingSummary } from '@/api/dcc/controlledFile/readingSession'
import { createPreviewReadingLifecycle } from './reading-lifecycle'
import { getAccessToken, getTenantId, getVisitTenantId } from '@/utils/auth'
import { useCache, CACHE_KEY } from '@/hooks/web/useCache'

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
const emit = defineEmits<{
  opened: [event: { source: OnlineFilePreviewSource; metadata: ControlledFilePreviewMetadataVO }]
}>()

interface ViewerCanvasPageProxy extends PdfTextPage {
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
const pdfPageRefMap = new Map<number, HTMLElement>()
const pdfCurrentPage = ref(1)
const pdfPageInput = ref('1')
const pdfNavigationError = ref('')
const pdfTextPages = ref<string[]>([])
const pdfTextStatus = ref<'idle' | 'loading' | 'ready' | 'error'>('idle')
const pdfSearchError = ref('')
const pdfSearchQuery = ref('')
const pdfSubmittedSearchQuery = ref('')
const pdfSearchMatches = ref<PdfTextMatch[]>([])
const pdfSearchMatchIndex = ref(-1)
const pdfCanSearch = computed(() => pdfTextStatus.value === 'ready' && pdfTextPages.value.some(text => text.trim()))
const pdfSearchMessage = computed(() => {
  if (pdfTextStatus.value === 'loading') return '正在读取 PDF 正文文本…'
  if (pdfTextStatus.value === 'error') return 'PDF 正文搜索不可用，请查看错误提示。'
  if (pdfTextStatus.value !== 'ready') return ''
  if (!pdfCanSearch.value) return '当前 PDF 无可搜索文本，扫描件或纯图页面不支持文字搜索。'
  if (!pdfSubmittedSearchQuery.value) return '可搜索 PDF 正文文本'
  if (!pdfSearchMatches.value.length) return '未找到匹配的正文文本'
  return `第 ${pdfSearchMatchIndex.value + 1} / ${pdfSearchMatches.value.length} 处匹配`
})
const renderVersion = ref(0)
let previewUnmounted = false
interface PreviewIdentitySnapshot {
  actorId: string
  baseTenantId: string
  visitTenantId: string | null
}
const { wsCache: previewIdentityCache } = useCache()
let previewIdentitySnapshot: PreviewIdentitySnapshot | null = null
let previewIdentityInvalidated = false
const identityId = (value: unknown): string => {
  if (typeof value === 'number' && !Number.isSafeInteger(value)) throw new Error('正文身份缓存不合法')
  if ((typeof value !== 'number' && typeof value !== 'string') || !/^[1-9][0-9]*$/.test(String(value))) throw new Error('正文身份缓存尚未就绪')
  return String(value)
}
const readPreviewIdentity = (): PreviewIdentitySnapshot => {
  if (!getAccessToken()) throw new Error('当前登录身份已失效，请重新登录后加载正文')
  const visit = getVisitTenantId()
  return {
    actorId: identityId(previewIdentityCache.get(CACHE_KEY.USER)?.user?.id),
    baseTenantId: identityId(getTenantId()),
    visitTenantId: visit === undefined || visit === null ? null : identityId(visit)
  }
}
const matchesPreviewIdentity = (snapshot: PreviewIdentitySnapshot) => {
  try {
    const current = readPreviewIdentity()
    return current.actorId === snapshot.actorId && current.baseTenantId === snapshot.baseTenantId
      && current.visitTenantId === snapshot.visitTenantId
  } catch {
    // An absent/invalid authenticated cache is an identity loss, handled explicitly below.
    return false
  }
}
const pdfLoadingTask = shallowRef<ViewerLoadingTask | null>(null)
const pdfDocument = shallowRef<ViewerCanvasDocumentProxy | null>(null)
const resolvedPreviewMetadata = shallowRef<ControlledFilePreviewMetadataVO | null>(null)
const registeredPreviewVisible = ref(false)
const clearRegisteredPreview = () => {
  registeredPreviewVisible.value = false
  closePreviewReading()
}
const registeredPreviewLabel = computed(() => {
  if (!registeredPreviewVisible.value) return ''
  const value = resolvedPreviewMetadata.value?.previewRegisteredAt
  if (!value) return '未记录'
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?(?:Z|[+-]\d{2}:\d{2})$/.test(value) || !Number.isFinite(Date.parse(value))) {
    throw new Error('本次浏览登记时间缺少有效服务器时区')
  }
  return new Intl.DateTimeFormat('zh-CN', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23' }).format(new Date(value)) + '（Asia/Shanghai，UTC+08:00）'
})
const markRegisteredPreviewVisible = (generation: number) => {
  if (!isCurrentRenderVersion(generation) || errorMessage.value || resolvedPreviewUnavailableReason.value) return
  registeredPreviewVisible.value = true
  void startPreviewReading(generation).catch(error => {
    if (!isCurrentRenderVersion(generation)) return
    const owner = previewReadingOwner
    if (owner?.generation === generation) reportReadingFailure(owner, error)
    else readingError.value = resolveViewerErrorMessage(error, '正文阅读会话启动失败')
  })
}
const handleMediaLoaded = (event: Event) => {
  const element = event.currentTarget as (HTMLElement & { currentSrc?: string }) | null
  if (!element || element.dataset.previewGeneration !== String(renderVersion.value)
    || element.getAttribute('src') !== mediaObjectUrl.value || element.currentSrc !== mediaObjectUrl.value) return
  markRegisteredPreviewVisible(renderVersion.value)
}
const handleMediaError = (event: Event) => {
  const element = event.currentTarget as (HTMLElement & { currentSrc?: string }) | null
  if (!element || element.dataset.previewGeneration !== String(renderVersion.value) || element.getAttribute('src') !== mediaObjectUrl.value) return
  clearRegisteredPreview()
  errorMessage.value = '当前图片或音视频正文加载失败，请查看文件和受控阅读权限'
}
const resolvedWatermark = shallowRef<ControlledPreviewWatermark | null>(null)
const resolvedPreviewKind = ref<ControlledFilePreviewKind | ''>('')
const resolvedOnlyOfficeBaseUrl = ref('')
const resolvedOnlyOfficeDocumentUrl = ref('')
const resolvedPreviewUnavailableReason = ref('')
const resolvedFileName = ref('')
const mediaObjectUrl = ref('')
const textPreviewContent = ref('')
const flatPreviewContainerRef = ref<HTMLDivElement>()
const flatPreviewStageRef = ref<HTMLDivElement>()
const flatPreviewSize = ref({ width: 0, height: 0 })
const flatPreviewContainerWidth = ref(0)
let flatPreviewResizeObserver: ResizeObserver | null = null
const TEXT_LINES_PER_PAGE = 80
const textCurrentPage = ref(1)
const textPageInput = ref('1')
const textNavigationError = ref('')
const textSearchQuery = ref('')
const textSubmittedSearchQuery = ref('')
const textSearchMatches = ref<Array<{ pageNumber: number; startIndex: number; endIndex: number }>>([])
const textSearchMatchIndex = ref(-1)
const currentPdfBytes = shallowRef<Uint8Array | null>(null)
const MIN_VIEWER_ZOOM_PERCENT = 50
const MAX_VIEWER_ZOOM_PERCENT = 250
const VIEWER_ZOOM_STEP_PERCENT = 25
const DEFAULT_VIEWER_ZOOM_PERCENT = 100
const viewerZoomPercent = ref(DEFAULT_VIEWER_ZOOM_PERCENT)
const viewerRotationDegrees = ref(0)

const invalidatePreviewIdentity = () => {
  if (previewIdentityInvalidated || previewUnmounted) return
  previewIdentityInvalidated = true
  renderVersion.value += 1
  clearRegisteredPreview()
  resolvedPreviewMetadata.value = null
  resolvedOnlyOfficeDocumentUrl.value = ''
  resolvedOnlyOfficeBaseUrl.value = ''
  resolvedPreviewKind.value = ''
  resolvedWatermark.value = null
  loading.value = false
  errorMessage.value = '当前用户或租户已变化，旧正文与阅读会话已失效，请重新加载正文'
  void resetPreviewState().catch(error => {
    console.error('旧身份正文清理失败：' + resolveViewerErrorMessage(error, '正文资源清理失败'))
  })
}
const isCurrentRenderVersion = (currentRenderVersion: number) => {
  if (previewUnmounted || currentRenderVersion !== renderVersion.value || previewIdentityInvalidated) return false
  if (previewIdentitySnapshot && !matchesPreviewIdentity(previewIdentitySnapshot)) {
    invalidatePreviewIdentity()
    return false
  }
  return true
}
const handlePreviewIdentityChange = () => {
  if (previewIdentitySnapshot) isCurrentRenderVersion(renderVersion.value)
}

const resolvedWatermarkText = computed(() => String(resolvedWatermark.value?.text || '').trim())
const resolvedWatermarkLabel = computed(() => getPreviewWatermarkBadgeLabel(resolvedWatermark.value))
const watermarkDetail = computed(() => buildPreviewWatermarkDetail(resolvedWatermark.value))
const watermarkBackground = computed(() => buildPreviewWatermarkBackground(resolvedWatermark.value))
const canTransformPreview = computed(() => ['PDF', 'IMAGE', 'VIDEO', 'TEXT'].includes(resolvedPreviewKind.value))
const viewerScale = computed(() => viewerZoomPercent.value / 100)
const previewTransform = computed(
  () => `scale(${viewerScale.value}) rotate(${viewerRotationDegrees.value}deg)`
)
const pdfPreviewTransformStyle = computed(() => ({
  transform: previewTransform.value,
  transformOrigin: 'center center'
}))
const flatPreviewViewportStyle = computed(() => getPdfPageViewportStyle(flatPreviewSize.value))
const flatPreviewTransformStyle = computed(() => ({
  width: `${flatPreviewContainerWidth.value}px`,
  transform: `translate(-50%, -50%) ${previewTransform.value}`,
  transformOrigin: 'center center'
}))
const normalizedTextContent = computed(() => textPreviewContent.value.replace(/\r\n?/g, '\n'))
const textPageEntries = computed(() => {
  const lines = normalizedTextContent.value.split('\n')
  const pages: Array<{ text: string; startIndex: number; endIndex: number }> = []
  let startIndex = 0
  for (let index = 0; index < lines.length; index += TEXT_LINES_PER_PAGE) {
    const text = lines.slice(index, index + TEXT_LINES_PER_PAGE).join('\n')
    pages.push({ text, startIndex, endIndex: startIndex + text.length })
    startIndex += text.length + 1
  }
  return pages
})
const textPageSegments = computed(() => {
  const page = textPageEntries.value[textCurrentPage.value - 1]
  if (!page) return []
  const segments: Array<{ text: string; startIndex: number; match: boolean; active: boolean }> = []
  let offset = page.startIndex
  textSearchMatches.value.forEach((match, index) => {
    if (match.endIndex <= page.startIndex || match.startIndex >= page.endIndex) return
    const start = Math.max(page.startIndex, match.startIndex)
    const end = Math.min(page.endIndex, match.endIndex)
    if (start > offset) segments.push({ text: normalizedTextContent.value.slice(offset, start), startIndex: offset, match: false, active: false })
    segments.push({ text: normalizedTextContent.value.slice(start, end), startIndex: start, match: true, active: index === textSearchMatchIndex.value })
    offset = end
  })
  if (offset < page.endIndex) segments.push({ text: normalizedTextContent.value.slice(offset, page.endIndex), startIndex: offset, match: false, active: false })
  return segments
})
const textSearchMessage = computed(() => {
  if (!textSubmittedSearchQuery.value) return '可搜索文本正文'
  if (!textSearchMatches.value.length) return '未找到匹配的正文文本'
  return `第 ${textSearchMatchIndex.value + 1} / ${textSearchMatches.value.length} 处匹配`
})
const resolvedOnlinePreviewSource = computed<OnlineFilePreviewSource | null>(() => {
  if (props.previewSource) {
    const source = props.previewSource
    if (source.type === 'DCC_CONTROLLED_FILE') {
      if (props.controlledFileId !== undefined && props.controlledFileId !== null && props.controlledFileId !== ''
        && String(props.controlledFileId) !== String(source.controlledFileId)) throw new Error('正文source与所选文件身份不一致')
      if (source.distribution && source.bodyReadContext !== undefined) throw new Error('下发义务与正文业务上下文不能混用')
      if (!source.distribution) return buildDccControlledFilePreviewSource(source.controlledFileId, source.bodyReadContext)
    }
    return props.previewSource
  }
  if (props.controlledFileId !== undefined && props.controlledFileId !== null && props.controlledFileId !== '') {
    return buildDccControlledFilePreviewSource(props.controlledFileId)
  }
  return null
})


interface ViewerReadingOwner {
  generation: number
  api: ReturnType<typeof createPreviewReadingApi>
  lifecycle: ReturnType<typeof createPreviewReadingLifecycle>
  summarySequence: number
  failed: boolean
  reportedErrors: Set<string>
  identity: PreviewIdentitySnapshot
}
const readingSummary = shallowRef<PreviewReadingSummary | null>(null)
const readingError = ref('')
const readingCloseError = ref('')
const readingSupported = ref(false)
const readingSourceSnapshot = shallowRef<OnlineFilePreviewSource | null>(null)
let previewReadingOwner: ViewerReadingOwner | null = null
let readingClosePending: Promise<void> = Promise.resolve()
let readingPausedForSignature = false
const readingSummaryPending = new Set<Promise<void>>()
let readingPageActive = true
const isReadingOwner = (owner: ViewerReadingOwner) =>
  isCurrentRenderVersion(owner.generation) && previewReadingOwner === owner
const assertReadingIdentity = (owner: ViewerReadingOwner) => {
  if (matchesPreviewIdentity(owner.identity)) return
  if (owner.generation === renderVersion.value) invalidatePreviewIdentity()
  throw new Error('当前用户或租户已变化，不能使用旧身份的阅读凭证')
}
const reportReadingFailure = (owner: ViewerReadingOwner, error: unknown) => {
  owner.failed = true
  const message = resolveViewerErrorMessage(error, '正文阅读计时或统计请求失败')
  if (owner.reportedErrors.has(message)) return
  owner.reportedErrors.add(message)
  if (isReadingOwner(owner)) {
    readingError.value = message
  } else {
    // Old cleanup failures must be observable without exposing token-bearing request objects.
    console.error('旧正文阅读会话结束未确认：' + message)
  }
}
const refreshReadingSummary = (owner: ViewerReadingOwner): Promise<void> => {
  if (readingPausedForSignature) return Promise.resolve()
  const pending = (async () => {
    const sequence = ++owner.summarySequence
    try {
      assertReadingIdentity(owner)
      const summary = await owner.api.summary()
      if (isReadingOwner(owner) && sequence === owner.summarySequence && !owner.failed) readingSummary.value = summary
    } catch (error) {
      if (!isReadingOwner(owner) || sequence !== owner.summarySequence) return
      reportReadingFailure(owner, error)
      // Summary failure must not lose a session that was already accepted by the server.
      void owner.lifecycle.pause().catch(closeError => reportReadingFailure(owner, closeError))
    }
  })()
  readingSummaryPending.add(pending)
  void pending.then(() => readingSummaryPending.delete(pending))
  return pending
}
const closePreviewReading = () => {
  const owner = previewReadingOwner
  previewReadingOwner = null
  readingSummary.value = null
  readingError.value = ''
  readingSupported.value = false
  readingSourceSnapshot.value = null
  if (!owner) return
  readingClosePending = Promise.all([readingClosePending, owner.lifecycle.dispose()])
    .then(() => undefined)
    .catch(error => {
      reportReadingFailure(owner, error)
      readingCloseError.value = resolveViewerErrorMessage(error, '旧正文阅读会话停止失败')
    })
}
const startPreviewReading = async (generation: number) => {
  await readingClosePending
  if (readingPausedForSignature || !isCurrentRenderVersion(generation) || !registeredPreviewVisible.value || props.previewBlob || props.previewKind
    || errorMessage.value || resolvedPreviewUnavailableReason.value) return
  if (previewReadingOwner?.generation === generation) return
  const source = readingSourceSnapshot.value
  const metadata = resolvedPreviewMetadata.value
  if (!source || !metadata || !['PDF', 'TEXT', 'OFFICE', 'IMAGE', 'AUDIO', 'VIDEO'].includes(resolvedPreviewKind.value)) return
  let scope: PreviewReadingScope
  if (source.type === 'DCC_CONTROLLED_FILE' && !source.distribution) {
    // The captured formal source carries its normalized exact context; never infer distribution/attachment identity.
    if (!source.bodyReadContext) throw new Error('正文阅读计时缺少正规版本上下文')
    scope = { kind: 'CONTROLLED_FILE', fileId: String(source.controlledFileId), bodyReadContext: source.bodyReadContext }
  } else if (source.type === 'EDHR_SPECIAL_NODE_ATTACHMENT') {
    if (typeof source.fileId === 'number' && !Number.isSafeInteger(source.fileId)) throw new Error('正文阅读文件身份超出安全整数范围')
    scope = { kind: 'ONLINE_FILE', fileId: String(source.fileId) }
  } else {
    return
  }
  readingSupported.value = true
  if (!previewIdentitySnapshot) throw new Error('正文阅读缺少已验证的用户与租户身份')
  const api = createPreviewReadingApi(scope, metadata)
  const owner: ViewerReadingOwner = {
    generation, api, summarySequence: 0, failed: false, reportedErrors: new Set(), identity: previewIdentitySnapshot,
    lifecycle: createPreviewReadingLifecycle({
      api: {
        start: async clientId => {
          assertReadingIdentity(owner)
          const session = await api.start(clientId)
          if (isReadingOwner(owner) && !owner.failed) void refreshReadingSummary(owner)
          return session
        },
        heartbeat: async sessionId => {
          assertReadingIdentity(owner)
          const session = await api.heartbeat(sessionId)
          if (isReadingOwner(owner) && !owner.failed) void refreshReadingSummary(owner)
          return session
        },
        stop: async sessionId => {
          assertReadingIdentity(owner)
          const session = await api.stop(sessionId)
          if (isReadingOwner(owner) && !owner.failed) void refreshReadingSummary(owner)
          return session
        }
      },
      visible: () => !readingPausedForSignature && isReadingOwner(owner) && !owner.failed && registeredPreviewVisible.value
        && !errorMessage.value && !resolvedPreviewUnavailableReason.value
        && readingPageActive && document.visibilityState === 'visible' && document.hasFocus(),
      newClientId: () => crypto.randomUUID(),
      schedule: (tick, intervalMs) => {
        const timer = window.setInterval(() => {
          void tick().catch(error => reportReadingFailure(owner, error))
        }, intervalMs)
        return () => window.clearInterval(timer)
      },
      onError: error => reportReadingFailure(owner, error)
    })
  }
  previewReadingOwner = owner
  if (readingPageActive && document.visibilityState === 'visible' && document.hasFocus()) {
    await owner.lifecycle.resume()
  } else {
    void refreshReadingSummary(owner)
  }
}
const handleReadingActivityChange = () => {
  if (readingPausedForSignature || !isCurrentRenderVersion(renderVersion.value)) return
  const owner = previewReadingOwner
  if (!owner || owner.failed) return
  const action = readingPageActive && document.visibilityState === 'visible' && document.hasFocus()
    ? owner.lifecycle.resume() : owner.lifecycle.pause()
  void action.catch(error => reportReadingFailure(owner, error))
}
const handleReadingPageHide = () => {
  readingPageActive = false
  const owner = previewReadingOwner
  if (owner) void owner.lifecycle.pause().catch(error => reportReadingFailure(owner, error))
}
const handleReadingPageShow = () => {
  readingPageActive = true
  handleReadingActivityChange()
}
const pauseReading = async () => {
  readingPausedForSignature = true
  await readingClosePending
  if (readingCloseError.value) throw new Error(readingCloseError.value)
  try {
    await previewReadingOwner?.lifecycle.pause()
  } catch (error) {
    readingCloseError.value = resolveViewerErrorMessage(error, '正文阅读会话停止失败')
    throw error
  }
  await Promise.all([...readingSummaryPending])
}
const resumeReading = async () => {
  if (readingCloseError.value) throw new Error(readingCloseError.value)
  readingPausedForSignature = false
  if (previewReadingOwner) {
    if (!previewReadingOwner.failed) await previewReadingOwner.lifecycle.resume()
  } else {
    await startPreviewReading(renderVersion.value)
  }
}
defineExpose({ pauseReading, resumeReading })
const formatReadingOpenedAt = (value: string | null) => value === null ? '未记录'
  : new Intl.DateTimeFormat('zh-CN', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23' }).format(new Date(value)) + '（Asia/Shanghai）'

const bindCanvasRef = (pageNumber: number, element: unknown) => {
  if (element instanceof HTMLCanvasElement) {
    canvasRefMap.set(pageNumber, element)
    return
  }
  canvasRefMap.delete(pageNumber)
}

const bindPdfPageRef = (pageNumber: number, element: unknown) => {
  if (element instanceof HTMLElement) {
    pdfPageRefMap.set(pageNumber, element)
    return
  }
  pdfPageRefMap.delete(pageNumber)
}

const resetPdfTools = () => {
  pdfCurrentPage.value = 1
  pdfPageInput.value = '1'
  pdfNavigationError.value = ''
  pdfTextPages.value = []
  pdfTextStatus.value = 'idle'
  pdfSearchError.value = ''
  pdfSearchQuery.value = ''
  pdfSubmittedSearchQuery.value = ''
  pdfSearchMatches.value = []
  pdfSearchMatchIndex.value = -1
  pdfPageRefMap.clear()
}

const resetTextTools = () => {
  textCurrentPage.value = 1
  textPageInput.value = '1'
  textNavigationError.value = ''
  textSearchQuery.value = ''
  textSubmittedSearchQuery.value = ''
  textSearchMatches.value = []
  textSearchMatchIndex.value = -1
}

const goToTextPage = (pageNumber: number) => {
  if (!Number.isInteger(pageNumber) || pageNumber < 1 || pageNumber > textPageEntries.value.length) {
    textNavigationError.value = `请输入 1 至 ${textPageEntries.value.length} 之间的整数页码`
    return
  }
  textNavigationError.value = ''
  textCurrentPage.value = pageNumber
  textPageInput.value = String(pageNumber)
}

const handleTextPageJump = () => {
  const input = textPageInput.value.trim()
  if (!/^[1-9]\d*$/.test(input)) {
    textNavigationError.value = `请输入 1 至 ${textPageEntries.value.length} 之间的整数页码`
    return
  }
  goToTextPage(Number(input))
}

const handleTextSearch = () => {
  const query = textSearchQuery.value.trim()
  textSubmittedSearchQuery.value = query
  textSearchMatches.value = []
  if (query) {
    const literalPattern = new RegExp(query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'giu')
    let pageIndex = 0
    for (const match of normalizedTextContent.value.matchAll(literalPattern)) {
      const startIndex = match.index!
      while (pageIndex + 1 < textPageEntries.value.length && textPageEntries.value[pageIndex + 1].startIndex <= startIndex) pageIndex++
      textSearchMatches.value.push({ pageNumber: pageIndex + 1, startIndex, endIndex: startIndex + match[0].length })
    }
  }
  textSearchMatchIndex.value = textSearchMatches.value.length ? 0 : -1
  if (textSearchMatches.value.length) goToTextPage(textSearchMatches.value[0].pageNumber)
}

const moveTextSearchMatch = (step: number) => {
  const count = textSearchMatches.value.length
  if (!count) return
  textSearchMatchIndex.value = (textSearchMatchIndex.value + step + count) % count
  goToTextPage(textSearchMatches.value[textSearchMatchIndex.value].pageNumber)
}

const goToPdfPage = (pageNumber: number) => {
  if (!Number.isInteger(pageNumber) || pageNumber < 1 || pageNumber > pageEntries.value.length) {
    pdfNavigationError.value = `请输入 1 至 ${pageEntries.value.length} 之间的整数页码`
    return
  }
  const pageElement = pdfPageRefMap.get(pageNumber)
  const frame = viewerFrameRef.value
  if (!pageElement || !frame) {
    pdfNavigationError.value = '正文页面尚未准备好，请等待加载完成'
    return
  }
  pdfNavigationError.value = ''
  pdfCurrentPage.value = pageNumber
  pdfPageInput.value = String(pageNumber)
  frame.scrollTo({ top: pageElement.offsetTop, behavior: 'instant' })
}

const handlePdfPageJump = () => {
  const input = pdfPageInput.value.trim()
  if (!/^[1-9]\d*$/.test(input)) {
    pdfNavigationError.value = `请输入 1 至 ${pageEntries.value.length} 之间的整数页码`
    return
  }
  goToPdfPage(Number(input))
}

const handlePdfScroll = (event: Event) => {
  const frame = viewerFrameRef.value
  if (!frame || event.target !== frame) return
  for (const page of pageEntries.value) {
    const element = pdfPageRefMap.get(page.pageNumber)
    if (element && element.offsetTop + element.offsetHeight > frame.scrollTop + 24) {
      pdfCurrentPage.value = page.pageNumber
      pdfPageInput.value = String(page.pageNumber)
      return
    }
  }
}

const handlePdfSearch = () => {
  pdfSubmittedSearchQuery.value = pdfSearchQuery.value.trim()
  pdfSearchMatches.value = pdfCanSearch.value
    ? findPdfTextMatches(pdfTextPages.value, pdfSubmittedSearchQuery.value) : []
  pdfSearchMatchIndex.value = pdfSearchMatches.value.length ? 0 : -1
  if (pdfSearchMatches.value.length) goToPdfPage(pdfSearchMatches.value[0].pageNumber)
}

const movePdfSearchMatch = (step: number) => {
  const count = pdfSearchMatches.value.length
  if (!count) return
  pdfSearchMatchIndex.value = (pdfSearchMatchIndex.value + step + count) % count
  goToPdfPage(pdfSearchMatches.value[pdfSearchMatchIndex.value].pageNumber)
}
const handlePdfSearchNext = () => movePdfSearchMatch(1)
const handlePdfSearchPrevious = () => movePdfSearchMatch(-1)

watch(pdfSearchQuery, () => {
  pdfSubmittedSearchQuery.value = ''
  pdfSearchMatches.value = []
  pdfSearchMatchIndex.value = -1
}, { flush: 'sync' })

const bindTextSearchMatchRef = (element: unknown) => {
  if (element instanceof HTMLElement && element.dataset.activeMatch === 'true') {
    element.scrollIntoView({ block: 'nearest', inline: 'nearest' })
  }
}

watch([flatPreviewContainerRef, flatPreviewStageRef], ([container, stage]) => {
  flatPreviewResizeObserver?.disconnect()
  if (!container || !stage || previewUnmounted) return
  const generation = renderVersion.value
  const measure = () => {
    if (!isCurrentRenderVersion(generation) || container !== flatPreviewContainerRef.value || stage !== flatPreviewStageRef.value) return
    flatPreviewContainerWidth.value = container.clientWidth
    flatPreviewSize.value = { width: stage.offsetWidth, height: stage.offsetHeight }
  }
  flatPreviewResizeObserver = new ResizeObserver(measure)
  flatPreviewResizeObserver.observe(container)
  flatPreviewResizeObserver.observe(stage)
  measure()
}, { flush: 'post' })

watch(textSearchQuery, () => {
  textSubmittedSearchQuery.value = ''
  textSearchMatches.value = []
  textSearchMatchIndex.value = -1
}, { flush: 'sync' })

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

const destroyPdfArtifacts = async (currentRenderVersion?: number) => {
  if (currentRenderVersion !== undefined && !isCurrentRenderVersion(currentRenderVersion)) {
    return
  }
  const documentToDestroy = pdfDocument.value
  const loadingTaskToDestroy = pdfLoadingTask.value
  // Detach immediately: a newer load must never destroy the same old document twice.
  pdfDocument.value = null
  pdfLoadingTask.value = null
  pageEntries.value = []
  canvasRefMap.clear()
  resetPdfTools()
  // destroy() cancels active rendering/text tasks; cleanup() rejects while a page renders.
  if (documentToDestroy?.destroy) {
    await documentToDestroy.destroy()
  } else if (loadingTaskToDestroy?.destroy) {
    await loadingTaskToDestroy.destroy()
  }
}

const resetPreviewState = async (currentRenderVersion?: number) => {
    if (currentRenderVersion !== undefined && !isCurrentRenderVersion(currentRenderVersion)) {
      return
    }
  clearRegisteredPreview()
  flatPreviewResizeObserver?.disconnect()
  flatPreviewSize.value = { width: 0, height: 0 }
  flatPreviewContainerWidth.value = 0
  revokeMediaObjectUrl()
  textPreviewContent.value = ''
  resetTextTools()
  currentPdfBytes.value = null
  await destroyPdfArtifacts(currentRenderVersion)
  if (currentRenderVersion !== undefined && !isCurrentRenderVersion(currentRenderVersion)) {
    return
  }
}

const resolvePreviewBlob = async (currentRenderVersion: number) => {
  if (props.previewBlob instanceof Uint8Array) {
    if (!isCurrentRenderVersion(currentRenderVersion)) {
      return null
    }
    return { bytes: props.previewBlob, blob: new Blob([props.previewBlob]) }
  }
  if (props.previewBlob instanceof Blob) {
    const bytes = new Uint8Array(await props.previewBlob.arrayBuffer())
    if (!isCurrentRenderVersion(currentRenderVersion)) {
      return null
    }
    return {
      bytes,
      blob: props.previewBlob
    }
  }
  if (resolvedOnlinePreviewSource.value) {
    const preview = await previewOnlineFileWithWatermark(
      resolvedOnlinePreviewSource.value,
      resolvedPreviewMetadata.value || undefined
    )
    if (!isCurrentRenderVersion(currentRenderVersion)) {
      return null
    }
    resolvedWatermark.value = withTraceableWatermark(
      preview.watermark,
      resolvedWatermark.value?.traceCode
    )
    const bytes = new Uint8Array(await preview.blob.arrayBuffer())
    if (!isCurrentRenderVersion(currentRenderVersion)) {
      return null
    }
    return {
      bytes,
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
  if (!isCurrentRenderVersion(currentRenderVersion)) {
    await loadingTask.destroy?.()
    return
  }
  pdfDocument.value = loadedDocument
  const nextPageEntries: Array<{ pageNumber: number; width: number; height: number }> = []
  for (let pageNumber = 1; pageNumber <= loadedDocument.numPages; pageNumber += 1) {
    const page = await loadedDocument.getPage(pageNumber)
    if (!isCurrentRenderVersion(currentRenderVersion)) {
      page.cleanup?.()
      await loadedDocument.destroy?.()
      return
    }
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
  if (!isCurrentRenderVersion(currentRenderVersion)) {
    return
  }
  const outputScale = window.devicePixelRatio || 1
  for (const pageEntry of pageEntries.value) {
    if (!isCurrentRenderVersion(currentRenderVersion)) {
      return
    }
    const page = await loadedDocument.getPage(pageEntry.pageNumber)
    if (!isCurrentRenderVersion(currentRenderVersion)) {
      page.cleanup?.()
      return
    }
    const canvas = canvasRefMap.get(pageEntry.pageNumber)
    if (!canvas) {
      throw new Error(`第 ${pageEntry.pageNumber} 页预览画布未准备好`)
    }
    const viewport = page.getViewport({ scale })
    const renderViewport = page.getViewport({ scale: scale * outputScale })
    canvas.width = Math.ceil(renderViewport.width)
    canvas.height = Math.ceil(renderViewport.height)
    canvas.style.width = `${Math.ceil(viewport.width)}px`
    canvas.style.height = `${Math.ceil(viewport.height)}px`
    const context = canvas.getContext('2d', { alpha: false })
    if (!context) {
      throw new Error(`第 ${pageEntry.pageNumber} 页无法创建预览画布`)
    }
    await page.render({
      canvasContext: context,
      viewport: renderViewport
    }).promise
    if (!isCurrentRenderVersion(currentRenderVersion)) {
      page.cleanup?.()
      return
    }
    drawControlledPreviewStamp(context, viewport, resolvedWatermark.value, outputScale)
    page.cleanup?.()
  }
}

const loadPdfTextIndex = async (document: ViewerCanvasDocumentProxy, currentRenderVersion: number) => {
  if (!isCurrentRenderVersion(currentRenderVersion)) return
  pdfTextStatus.value = 'loading'
  try {
    const textPages = await extractPdfTextPages(document, () => isCurrentRenderVersion(currentRenderVersion))
    if (textPages === null || !isCurrentRenderVersion(currentRenderVersion)) return
    pdfTextPages.value = textPages
    pdfTextStatus.value = 'ready'
  } catch (error) {
    if (!isCurrentRenderVersion(currentRenderVersion)) return
    pdfTextStatus.value = 'error'
    pdfSearchError.value = resolveViewerErrorMessage(error, 'PDF 正文文本读取失败，无法搜索。')
  }
}

const loadPreview = async () => {
  const currentRenderVersion = renderVersion.value + 1
  renderVersion.value = currentRenderVersion
  previewIdentityInvalidated = false
  previewIdentitySnapshot = null
  loading.value = true
  clearRegisteredPreview()
  errorMessage.value = ''
  resetViewerTransformState()
  resolvedWatermark.value = props.watermark || null
  resolvedPreviewMetadata.value = null
  resolvedPreviewKind.value = ''
  resolvedOnlyOfficeBaseUrl.value = props.onlyofficeBaseUrl || ''
  resolvedOnlyOfficeDocumentUrl.value = props.onlyofficeDocumentUrl || ''
  resolvedPreviewUnavailableReason.value = props.previewUnavailableReason || ''
  resolvedFileName.value = props.title || ''
  try {
    previewIdentitySnapshot = resolvedOnlinePreviewSource.value ? readPreviewIdentity() : null
    await resetPreviewState(currentRenderVersion)
    if (!isCurrentRenderVersion(currentRenderVersion)) return
    if (resolvedOnlinePreviewSource.value && !props.previewKind) {
      const sourceSnapshot = JSON.parse(JSON.stringify(resolvedOnlinePreviewSource.value)) as OnlineFilePreviewSource
      const metadata = await getOnlineFilePreviewMetadata(sourceSnapshot)
      if (!isCurrentRenderVersion(currentRenderVersion)) {
        return
      }
      resolvedPreviewMetadata.value = metadata
      readingSourceSnapshot.value = sourceSnapshot
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

    const previewPayload = await resolvePreviewBlob(currentRenderVersion)
    if (!previewPayload) {
      return
    }

    if (['IMAGE', 'VIDEO', 'AUDIO'].includes(resolvedPreviewKind.value)) {
      if (!isCurrentRenderVersion(currentRenderVersion)) {
        return
      }
      mediaObjectUrl.value = URL.createObjectURL(previewPayload.blob)
      return
    }

    if (resolvedPreviewKind.value === 'TEXT') {
      if (!isCurrentRenderVersion(currentRenderVersion)) {
        return
      }
      textPreviewContent.value = new TextDecoder().decode(previewPayload.bytes)
      await nextTick()
      if (textPreviewContent.value) markRegisteredPreviewVisible(currentRenderVersion)
      return
    }

    if (!isCurrentRenderVersion(currentRenderVersion)) {
      return
    }
    const scale = getProtectedViewerRenderScale(viewerFrameRef.value?.clientWidth)
    currentPdfBytes.value = previewPayload.bytes
    await renderPdfPages(previewPayload.bytes, currentRenderVersion, scale)
    markRegisteredPreviewVisible(currentRenderVersion)
    if (isCurrentRenderVersion(currentRenderVersion) && pdfDocument.value) {
      await loadPdfTextIndex(pdfDocument.value, currentRenderVersion)
    }
  } catch (error) {
    if (isCurrentRenderVersion(currentRenderVersion)) {
      errorMessage.value = resolveViewerErrorMessage(error, '受控预览加载失败，请查看错误提示后重试。')
      clearRegisteredPreview()
    }
  } finally {
    if (isCurrentRenderVersion(currentRenderVersion)) {
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
  previewUnmounted = false
  window.addEventListener('keydown', handleWindowKeydown, true)
  window.addEventListener('focus', handleReadingActivityChange)
  window.addEventListener('blur', handleReadingActivityChange)
  window.addEventListener('pagehide', handleReadingPageHide)
  window.addEventListener('pageshow', handleReadingPageShow)
  window.addEventListener('storage', handlePreviewIdentityChange)
  document.addEventListener('visibilitychange', handleReadingActivityChange)
})

const handleOfficeOpened = (event: { documentUrl: string }) => {
  if (!isCurrentRenderVersion(renderVersion.value)) return
  const source = resolvedOnlinePreviewSource.value
  const metadata = resolvedPreviewMetadata.value
  if (previewUnmounted || resolvedPreviewKind.value !== 'OFFICE' || resolvedPreviewUnavailableReason.value
    || !source || !metadata || event.documentUrl !== resolvedOnlyOfficeDocumentUrl.value
    || event.documentUrl !== metadata.onlyofficeDocumentUrl) return
  const sourceSnapshot = source.type === 'DCC_CONTROLLED_FILE' && source.distribution
    ? { ...source, distribution: { ...source.distribution } } : { ...source }
  emit('opened', { source: sourceSnapshot, metadata: { ...metadata } })
  markRegisteredPreviewVisible(renderVersion.value)
}
const handleOfficeError = (event: { documentUrl: string; message: string }) => {
  if (previewUnmounted || event.documentUrl !== resolvedOnlyOfficeDocumentUrl.value
    || resolvedPreviewKind.value !== 'OFFICE') return
  clearRegisteredPreview()
  errorMessage.value = event.message
}

onBeforeUnmount(() => {
  previewUnmounted = true
  renderVersion.value += 1
  void resetPreviewState()
  clearRegisteredPreview()
  window.removeEventListener('keydown', handleWindowKeydown, true)
  window.removeEventListener('focus', handleReadingActivityChange)
  window.removeEventListener('blur', handleReadingActivityChange)
  window.removeEventListener('pagehide', handleReadingPageHide)
  window.removeEventListener('pageshow', handleReadingPageShow)
  window.removeEventListener('storage', handlePreviewIdentityChange)
  document.removeEventListener('visibilitychange', handleReadingActivityChange)
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
  { immediate: true, deep: true }
)
</script>

<style scoped>
.protected-viewer-shell {
  min-height: 560px;
  user-select: none;
  -webkit-user-select: none;
}

.protected-viewer-reading-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.protected-viewer-reading-error {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
}

.protected-viewer-pdf-tools {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  font-size: 13px;
}

.protected-viewer-pdf-tools__row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.protected-viewer-pdf-tools__page {
  width: 80px;
}

.protected-viewer-pdf-tools__query {
  width: min(100%, 280px);
}

.protected-viewer-pdf-tools__error {
  color: var(--el-color-danger);
}

.protected-viewer-pdf-tools__excerpt {
  overflow-wrap: anywhere;
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

.protected-viewer-text-tools {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
}

.protected-viewer-text-tools__row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.protected-viewer-text-tools__page {
  width: 80px;
}

.protected-viewer-text-tools__query {
  width: min(100%, 280px);
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

.protected-viewer-flat-container {
  display: block;
  min-width: 0;
  padding: 0;
}

.protected-viewer-flat-viewport {
  position: relative;
  margin: 24px auto;
}

.protected-viewer-flat-stage {
  position: absolute;
  top: 50%;
  left: 50%;
  align-items: flex-start;
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
  box-sizing: border-box;
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

.protected-viewer-text mark {
  background: #fff1a8;
  color: inherit;
}

.protected-viewer-text mark.protected-viewer-text-match--active {
  background: #ffcc80;
  box-shadow: inset 0 0 0 1px #a85a00;
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
