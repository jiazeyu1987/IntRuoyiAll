<template>
  <Teleport to="body" :disabled="!expanded">
    <section
      ref="panel"
      class="controlled-upload-preview"
      :class="{ 'is-expanded': expanded }"
      :role="expanded ? 'dialog' : 'region'"
      :aria-modal="expanded || undefined"
      aria-label="正文预览"
      tabindex="-1"
      @keydown="onPanelKeydown"
    >
      <header class="preview-heading">
        <h3>正文预览</h3>
        <span class="preview-filename">{{ file?.name || '未选择主文件' }}</span>
      </header>
      <div v-if="supported" class="preview-toolbar" aria-label="正文预览工具">
        <el-button size="small" :disabled="!ready" @click="fitWidth">适应宽度</el-button>
        <div class="toolbar-group">
          <el-button size="small" aria-label="缩小正文" :disabled="!ready || zoom <= 25" @click="changeZoom(-25)">−</el-button>
          <span class="zoom-label">{{ Math.round(zoom) }}%</span>
          <el-button size="small" aria-label="放大正文" :disabled="!ready || zoom >= 300" @click="changeZoom(25)">+</el-button>
        </div>
        <div v-if="kind !== 'image'" class="toolbar-group">
          <el-button size="small" :disabled="!ready || pageNumber <= 1" @click="pageNumber--">上一页</el-button>
          <span>页码</span>
          <el-input-number v-model="pageNumber" size="small" aria-label="正文页码" :min="1" :max="pageCount || 1" :controls="false" :disabled="!ready" @change="normalizePage" />
          <span>/ {{ pageCount }}</span>
          <el-button size="small" :disabled="!ready || pageNumber >= pageCount" @click="pageNumber++">下一页</el-button>
          <span v-if="kind === 'text'">每 60 行一页</span>
        </div>
        <el-button class="expand-button" size="small" type="primary" plain @click="toggleExpanded">
          {{ expanded ? '收起预览' : '放大预览' }}
        </el-button>
      </div>
      <div v-if="kind === 'pdf' || kind === 'text' || kind === 'docx'" class="preview-search">
        <el-input v-model="query" size="small" placeholder="搜索正文" aria-label="正文搜索关键词" clearable @keyup.enter="search" />
        <el-button size="small" :disabled="!ready || searching || !query.trim()" @click="search">搜索</el-button>
        <el-button size="small" :disabled="!matches.length || searching" @click="navigateMatch(-1)">上一处</el-button>
        <el-button size="small" :disabled="!matches.length || searching" @click="navigateMatch(1)">下一处</el-button>
        <span role="status">{{ searchStatus }}</span>
      </div>
      <div ref="viewport" class="preview-viewport" :aria-busy="busy">
        <p v-if="!file" class="preview-message">选择受控主文件后查看正文</p>
        <p v-else-if="!supported" class="preview-message">{{ file.name.toLowerCase().endsWith('.doc') ? '旧版 DOC 格式不支持本地预览，请另存为 DOCX 后选择文件。' : '该格式暂不支持本地正文预览，请在上传前使用对应软件核对文件。' }}</p>
        <p v-else-if="error" class="preview-message preview-error" role="alert">{{ error }}</p>
        <template v-else>
          <p v-if="busy" class="preview-message" role="status">正在加载正文…</p>
          <canvas v-if="kind === 'pdf'" v-show="ready" ref="canvas" aria-label="PDF 正文"></canvas>
          <div v-else-if="kind === 'docx'" ref="wordContainer" class="word-document" :style="{ zoom: zoom / 100 }"></div>
          <pre v-else-if="kind === 'text'" class="text-page" :style="{ fontSize: `${14 * zoom / 100}px` }"><template v-for="(part, index) in textParts" :key="index"><mark v-if="part.match" :class="{ 'active-match': part.index === activeTextMatch }">{{ part.text }}</mark><template v-else>{{ part.text }}</template></template></pre>
          <img v-else-if="imageUrl" :key="imageUrl" :src="imageUrl" :alt="file.name" :style="{ width: `${imageWidth * zoom / 100}px` }" @load="onImageLoad" @error="onImageError" />
        </template>
      </div>
    </section>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { getDocument, GlobalWorkerOptions, type PdfDocumentProxyLike, type PdfPageProxyLike } from '../view/vendor/pdf.min.mjs'

const props = defineProps<{ file?: File }>()
GlobalWorkerOptions.workerSrc = `${import.meta.env.BASE_URL.replace(/\/?$/, '/')}pdfjs/pdf.worker.mjs`

const kind = computed(() => {
  const extension = props.file?.name.split('.').pop()?.toLowerCase()
  if (extension === 'pdf') return 'pdf'
  if (extension === 'docx') return 'docx'
  if (extension === 'txt') return 'text'
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg', 'avif'].includes(extension || '')) return 'image'
  return 'unsupported'
})
const supported = computed(() => !!props.file && kind.value !== 'unsupported')
const panel = ref<HTMLElement>()
const viewport = ref<HTMLElement>()
const canvas = ref<HTMLCanvasElement>()
const wordContainer = ref<HTMLElement>()
let wordPages: HTMLElement[] = []
let wordText: string[] = []
const imageUrl = ref('')
const imageWidth = ref(0)
const ready = ref(false)
const busy = ref(false)
const error = ref('')
const zoom = ref(100)
const pageNumber = ref(1)
const pageCount = ref(0)
const expanded = ref(false)
let fit = true
let sequence = 0
let renderSequence = 0
let loadingTask: ReturnType<typeof getDocument> | undefined
interface SearchablePdfPage extends PdfPageProxyLike {
  getTextContent(): Promise<{ items: Array<{ str?: string }> }>
}
interface SearchablePdf extends PdfDocumentProxyLike {
  getPage(page: number): Promise<SearchablePdfPage>
}
let pdf: SearchablePdf | undefined
let renderTask: { cancel: () => void; promise: Promise<void> } | undefined
let observer: ResizeObserver | undefined
let previousFocus: HTMLElement | null = null
const textPages = ref<string[]>([])
const query = ref('')
const searchedQuery = ref('')
const searching = ref(false)
const searchStatus = ref('')
const matches = ref<Array<{ page: number; offset: number }>>([])
const matchIndex = ref(-1)
let searchSequence = 0
const activeTextMatch = computed(() => {
  const match = matches.value[matchIndex.value]
  if (!match || match.page !== pageNumber.value) return -1
  return matches.value.slice(0, matchIndex.value).filter(item => item.page === match.page).length
})
const textParts = computed(() => {
  const text = textPages.value[pageNumber.value - 1] || ''
  const found = matches.value.filter(item => item.page === pageNumber.value)
  const parts: Array<{ text: string; match: boolean; index: number }> = []
  let start = 0
  found.forEach((item, index) => {
    parts.push({ text: text.slice(start, item.offset), match: false, index: -1 })
    start = item.offset + searchedQuery.value.length
    parts.push({ text: text.slice(item.offset, start), match: true, index })
  })
  parts.push({ text: text.slice(start), match: false, index: -1 })
  return parts
})
const showMatch = async () => {
  const match = matches.value[matchIndex.value]
  if (!match) return
  pageNumber.value = match.page
  searchStatus.value = `第 ${matchIndex.value + 1} / ${matches.value.length} 处（第 ${match.page} 页）`
  await nextTick()
  viewport.value?.querySelector('.active-match')?.scrollIntoView({ block: 'center' })
}
const navigateMatch = (direction: number) => {
  matchIndex.value = (matchIndex.value + direction + matches.value.length) % matches.value.length
  void showMatch()
}
watch(query, () => {
  searchSequence++
  searching.value = false
  matches.value = []
  searchedQuery.value = ''
  searchStatus.value = ''
  matchIndex.value = -1
})
const search = async () => {
  if (!ready.value || !query.value.trim() || searching.value) return
  const current = ++searchSequence
  const document = pdf
  const term = query.value.trim()
  searching.value = true
  searchStatus.value = '正在搜索正文…'
  matches.value = []
  const found: Array<{ page: number; offset: number }> = []
  try {
    for (let page = 1; page <= pageCount.value; page++) {
      const text = kind.value === 'text' ? textPages.value[page - 1] : kind.value === 'docx' ? wordText[page - 1]
        : (await (await document!.getPage(page)).getTextContent()).items.map(item => item.str || '').join(' ')
      if (current !== searchSequence) return
      let offset = text.toLocaleLowerCase().indexOf(term.toLocaleLowerCase())
      while (offset !== -1) {
        found.push({ page, offset })
        offset = text.toLocaleLowerCase().indexOf(term.toLocaleLowerCase(), offset + term.length)
      }
    }
    matches.value = found
    searchedQuery.value = term
    matchIndex.value = found.length ? 0 : -1
    if (found.length) await showMatch()
    else searchStatus.value = kind.value === 'pdf' ? '未找到匹配正文；扫描 PDF 需先具备可搜索文字。' : '未找到匹配正文。'
  } catch (cause) {
    if (current === searchSequence) searchStatus.value = `正文搜索失败：${cause instanceof Error ? cause.message : String(cause)}`
  } finally { if (current === searchSequence) searching.value = false }
}

const fail = (cause: unknown) => {
  error.value = `正文预览失败：${cause instanceof Error ? cause.message : String(cause)}`
  ready.value = false
  busy.value = false
}
const renderPage = async () => {
  const document = pdf
  if (!document || !canvas.value || !viewport.value) return
  const current = ++renderSequence
  renderTask?.cancel()
  renderTask = undefined
  busy.value = true
  try {
    const page = await document.getPage(pageNumber.value)
    if (current !== renderSequence) return
    const natural = page.getViewport({ scale: 1 })
    if (fit) zoom.value = Math.max(1, (viewport.value.clientWidth - 32) / natural.width * 100)
    const view = page.getViewport({ scale: zoom.value / 100 })
    // 独立画布避免快速翻页时，取消中的任务继续写入可见正文。
    const target = window.document.createElement('canvas')
    const context = target.getContext('2d')
    if (!context) throw new Error('浏览器无法创建正文画布')
    const ratio = window.devicePixelRatio || 1
    target.width = Math.ceil(view.width * ratio)
    target.height = Math.ceil(view.height * ratio)
    target.style.width = `${view.width}px`
    target.style.height = `${view.height}px`
    context.scale(ratio, ratio)
    renderTask = page.render({ canvasContext: context, viewport: view }) as typeof renderTask
    await renderTask.promise
    if (current !== renderSequence) return
    const visible = canvas.value
    visible.width = target.width
    visible.height = target.height
    visible.style.width = target.style.width
    visible.style.height = target.style.height
    visible.getContext('2d')!.drawImage(target, 0, 0)
    ready.value = true
    viewport.value.scrollTop = 0
  } catch (cause) {
    if (current === renderSequence) fail(cause)
  } finally {
    if (current === renderSequence) busy.value = false
  }
}
const fitImage = () => {
  if (viewport.value && imageWidth.value) zoom.value = Math.max(1, (viewport.value.clientWidth - 32) / imageWidth.value * 100)
}
const showWordPage = () => {
  wordPages.forEach((page, index) => { page.style.display = index === pageNumber.value - 1 ? '' : 'none' })
  if (viewport.value) viewport.value.scrollTop = 0
}
const fitWord = () => {
  const page = wordPages[pageNumber.value - 1]
  if (page?.offsetWidth && viewport.value) zoom.value = Math.max(1, (viewport.value.clientWidth - 32) / page.offsetWidth * 100)
}
const fitWidth = () => {
  fit = true
  if (kind.value === 'pdf') void renderPage()
  else if (kind.value === 'image') fitImage()
  else if (kind.value === 'docx') fitWord()
  else zoom.value = 100
}
const changeZoom = (delta: number) => {
  fit = false
  zoom.value = Math.min(300, Math.max(25, zoom.value + delta))
  if (kind.value === 'pdf') void renderPage()
}
const normalizePage = () => { if (!pageNumber.value) pageNumber.value = 1 }
watch(pageNumber, () => {
  if (kind.value === 'pdf' && pageNumber.value) void renderPage()
  else if (kind.value === 'docx') { showWordPage(); if (fit) fitWord() }
  else if (viewport.value) viewport.value.scrollTop = 0
})
const onImageLoad = (event: Event) => {
  if ((event.target as HTMLImageElement).src !== imageUrl.value) return
  imageWidth.value = (event.target as HTMLImageElement).naturalWidth
  ready.value = true
  busy.value = false
  fitImage()
}
const onImageError = (event: Event) => {
  if ((event.target as HTMLImageElement).src === imageUrl.value) fail(new Error('图片无法解码或格式不受浏览器支持'))
}
const toggleExpanded = async () => {
  if (!expanded.value) previousFocus = document.activeElement as HTMLElement
  expanded.value = !expanded.value
  await nextTick()
  if (expanded.value) panel.value?.focus()
  else previousFocus?.focus()
  if (ready.value && fit) fitWidth()
}
const onPanelKeydown = (event: KeyboardEvent) => {
  if (!expanded.value) return
  if (event.key === 'Escape') { event.preventDefault(); void toggleExpanded() }
  if (event.key !== 'Tab') return
  const elements = panel.value?.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled)')
  if (!elements?.length) return
  const first = elements[0], last = elements[elements.length - 1]
  if (event.shiftKey && (document.activeElement === first || document.activeElement === panel.value)) {
    event.preventDefault(); last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault(); first.focus()
  }
}
const release = () => {
  renderSequence++
  renderTask?.cancel()
  renderTask = undefined
  const task = loadingTask
  loadingTask = undefined
  pdf = undefined
  if (task) void Promise.resolve(task.destroy?.()).catch((cause: unknown) => console.error('释放本地 PDF 预览失败', cause))
  if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
  imageUrl.value = ''
  wordPages = []
  wordText = []
  wordContainer.value?.replaceChildren()
}
watch(() => props.file, async (file) => {
  const current = ++sequence
  release()
  ready.value = false
  busy.value = false
  error.value = ''
  zoom.value = 100
  pageNumber.value = 1
  pageCount.value = 0
  imageWidth.value = 0
  searchSequence++
  query.value = ''
  searchedQuery.value = ''
  matches.value = []
  matchIndex.value = -1
  searchStatus.value = ''
  searching.value = false
  textPages.value = []
  fit = true
  if (expanded.value) await toggleExpanded()
  if (!file || !supported.value || current !== sequence) return
  busy.value = true
  try {
    if (kind.value === 'image') {
      imageUrl.value = URL.createObjectURL(file)
      return
    }
    const bytes = await file.arrayBuffer()
    if (current !== sequence) return
    if (kind.value === 'docx') {
      const { renderAsync } = await import('docx-preview')
      if (current !== sequence) return
      const stage = window.document.createElement('div')
      await renderAsync(bytes, stage, stage, {
        className: 'upload-docx', inWrapper: false, breakPages: true,
        ignoreLastRenderedPageBreak: false, renderAltChunks: false, useBase64URL: true
      })
      if (current !== sequence) return
      const pages = Array.from(stage.querySelectorAll<HTMLElement>('section.upload-docx'))
      if (!pages.length) throw new Error('DOCX 中没有可显示的正文页面')
      // 本地只读预览不打开文档内链接，也不保留任何外部图片地址。
      stage.querySelectorAll('a').forEach(link => link.removeAttribute('href'))
      stage.querySelectorAll('img').forEach(image => {
        if (!image.src.startsWith('data:')) image.removeAttribute('src')
      })
      wordPages = pages
      wordText = pages.map(page => page.textContent || '')
      pageCount.value = pages.length
      await nextTick()
      if (current !== sequence) return
      wordContainer.value!.replaceChildren(...Array.from(stage.childNodes))
      showWordPage()
      fitWord()
      ready.value = true
      busy.value = false
      return
    }
    if (kind.value === 'text') {
      const data = new Uint8Array(bytes)
      const encoding = data[0] === 0xff && data[1] === 0xfe ? 'utf-16le'
        : data[0] === 0xfe && data[1] === 0xff ? 'utf-16be' : 'utf-8'
      let text: string
      try { text = new TextDecoder(encoding, { fatal: true }).decode(data) }
      catch (cause) { throw new Error('TXT 编码无效，请使用 UTF-8 或带 BOM 的 UTF-16 文件进行预览。', { cause }) }
      const lines = text.replace(/\r\n?/g, '\n').split('\n')
      for (let start = 0; start < lines.length; start += 60) textPages.value.push(lines.slice(start, start + 60).join('\n'))
      pageCount.value = textPages.value.length
      ready.value = true
      busy.value = false
      return
    }
    const task = getDocument({ data: new Uint8Array(bytes), useWorkerFetch: false, isEvalSupported: false, enableXfa: false, stopAtErrors: true })
    loadingTask = task
    const document = await task.promise
    if (current !== sequence) return
    pdf = document as SearchablePdf
    pageCount.value = document.numPages
    await nextTick()
    await renderPage()
  } catch (cause) {
    if (current === sequence) fail(cause)
  }
}, { immediate: true })
onMounted(() => {
  observer = new ResizeObserver(() => { if (ready.value && fit) fitWidth() })
  if (viewport.value) observer.observe(viewport.value)
})
onBeforeUnmount(() => { sequence++; searchSequence++; observer?.disconnect(); release() })
</script>

<style scoped>
.controlled-upload-preview {
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: clamp(520px, calc(100vh - 250px), 1000px);
  overflow: hidden;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
}
.preview-heading {
  display: flex;
  align-items: baseline;
  gap: 14px;
  padding: 14px 18px 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.preview-heading h3 {
  flex-shrink: 0;
  margin: 0;
  padding-left: 10px;
  border-left: 4px solid var(--el-color-primary);
  font-size: 16px;
  line-height: 22px;
}
.preview-filename {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  overflow-wrap: anywhere;
}
.preview-toolbar, .toolbar-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.preview-toolbar {
  padding: 10px 14px;
  font-size: 12px;
}
.preview-search {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 14px 10px;
  font-size: 12px;
}
.preview-search .el-input { width: 160px; }
.preview-search .el-button + .el-button { margin-left: 0; }
.preview-search span { color: var(--el-text-color-secondary); }
.text-page {
  margin: 0;
  padding: 20px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.8;
  background: var(--el-bg-color);
}
.text-page mark { background: var(--el-color-warning-light-7); }
.text-page .active-match { outline: 2px solid var(--el-color-primary); }
.word-document { width: max-content; margin: 0 auto; }
.word-document :deep(section.upload-docx) {
  background: white;
  box-shadow: 0 2px 8px rgb(0 0 0 / 12%);
}
.preview-toolbar .el-button + .el-button { margin-left: 0; }
.toolbar-group .el-input-number { width: 48px; }
.zoom-label { min-width: 40px; text-align: center; }
.expand-button { margin-left: auto; }
.preview-viewport {
  flex: 1;
  min-height: 0;
  margin: 0 10px 10px;
  padding: 16px;
  overflow: auto;
  background: var(--el-fill-color);
  border-radius: 5px;
}
.preview-viewport canvas, .preview-viewport img {
  display: block;
  max-width: none;
  margin: 0 auto;
  background: white;
  box-shadow: 0 2px 8px rgb(0 0 0 / 12%);
}
.preview-message {
  margin: 40px auto;
  max-width: 360px;
  color: var(--el-text-color-secondary);
  text-align: center;
  line-height: 1.8;
  font-size: 13px;
}
.preview-error { color: var(--el-color-danger); }
.is-expanded {
  position: fixed;
  inset: 12px;
  z-index: 3000;
  height: auto;
  box-shadow: 0 0 0 100vmax rgb(0 0 0 / 45%);
}
@media (max-width: 767px) {
  .preview-heading { flex-wrap: wrap; gap: 6px; }
  .is-expanded { inset: 4px; }
}
</style>
