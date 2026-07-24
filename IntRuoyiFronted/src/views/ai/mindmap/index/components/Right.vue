<template>
  <el-card class="my-card h-full flex-grow">
    <template #header>
      <h3 class="m-0 flex shrink-0 items-center justify-between px-7">
        <span>思维导图预览</span>
        <el-button v-show="isEnd" size="small" type="primary" @click="downloadImage">
          <template #icon>
            <Icon icon="ph:copy-bold" />
          </template>
          下载图片
        </el-button>
      </h3>
    </template>

    <div ref="contentRef" class="hide-scroll-bar h-full box-border">
      <div v-if="isGenerating" ref="mdContainerRef" class="wh-full overflow-y-auto">
        <div class="flex flex-col items-center justify-center" v-html="html"></div>
      </div>

      <div ref="mindMapRef" class="wh-full relative">
        <svg ref="svgRef" :style="{ height: `${contentAreaHeight}px` }" class="w-full" />
        <div ref="toolBarRef" class="absolute bottom-[10px] right-5"></div>
      </div>
    </div>
  </el-card>
</template>

<script lang="ts" setup>
import { Markmap } from 'markmap-view'
import { Transformer } from 'markmap-lib'
import { Toolbar } from 'markmap-toolbar'
import markdownit from 'markdown-it'
import download from '@/utils/download'

const md = markdownit()
const message = useMessage()

const props = defineProps<{
  generatedContent: string
  isEnd: boolean
  isGenerating: boolean
  isStart: boolean
}>()

const contentRef = ref<HTMLDivElement>()
const mdContainerRef = ref<HTMLDivElement>()
const mindMapRef = ref<HTMLDivElement>()
const svgRef = ref<SVGElement>()
const toolBarRef = ref<HTMLDivElement>()
const html = ref('')
const contentAreaHeight = ref(480)
let markMap: Markmap | null = null
let resizeObserver: ResizeObserver | null = null
const transformer = new Transformer()

const syncContentAreaHeight = () => {
  const nextHeight = contentRef.value?.clientHeight || 0
  if (nextHeight > 0) {
    contentAreaHeight.value = nextHeight
  }
  return nextHeight
}

const canRenderMindMap = () => {
  const width = mindMapRef.value?.clientWidth || 0
  const height = syncContentAreaHeight()
  return !!markMap && !!svgRef.value && width > 0 && height > 0
}

onMounted(async () => {
  await nextTick()
  syncContentAreaHeight()
  try {
    markMap = Markmap.create(svgRef.value!)
    const { el } = Toolbar.create(markMap)
    toolBarRef.value?.append(el)
    resizeObserver = new ResizeObserver(() => {
      syncContentAreaHeight()
      update()
    })
    if (contentRef.value) {
      resizeObserver.observe(contentRef.value)
    }
    requestAnimationFrame(update)
  } catch (error) {
    console.error(error)
    message.error('思维导图初始化失败')
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
})

watch(props, ({ generatedContent, isGenerating, isEnd, isStart }) => {
  if (isStart) {
    html.value = ''
  }
  if (isGenerating) {
    html.value = md.render(generatedContent)
  }
  if (isEnd) {
    requestAnimationFrame(update)
  }
})

const update = () => {
  if (!canRenderMindMap()) {
    return
  }
  try {
    const { root } = transformer.transform(processContent(props.generatedContent))
    markMap?.setData(root)
    requestAnimationFrame(() => {
      if (canRenderMindMap()) {
        markMap?.fit()
      }
    })
  } catch (error) {
    console.error(error)
  }
}

const processContent = (text: string) => {
  const lines = text.split('\n')
  const content: string[] = []
  for (let line of lines) {
    if (line.includes('```')) {
      continue
    }
    line = line.replace(/([*_~`>])|(\d+\.)\s/g, '')
    content.push(line)
  }
  return content.join('\n')
}

const downloadImage = () => {
  const svgElement = mindMapRef.value
  if (!svgElement || !svgRef.value) {
    return
  }
  const serializer = new XMLSerializer()
  const source = `<?xml version="1.0" standalone="no"?>\r\n${serializer.serializeToString(svgRef.value)}`
  const base64Url = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(source)}`
  download.image({
    url: base64Url,
    canvasWidth: svgElement.offsetWidth,
    canvasHeight: svgElement.offsetHeight,
    drawWithImageSize: false
  })
}

defineExpose({
  scrollBottom() {
    mdContainerRef.value?.scrollTo(0, mdContainerRef.value?.scrollHeight)
  }
})
</script>

<style lang="scss" scoped>
.hide-scroll-bar {
  -ms-overflow-style: none;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    width: 0;
    height: 0;
  }
}

.my-card {
  display: flex;
  flex-direction: column;

  :deep(.el-card__body) {
    box-sizing: border-box;
    flex-grow: 1;
    overflow-y: auto;
    padding: 0;
    @extend .hide-scroll-bar;
  }
}

:deep(.markmap) {
  width: 100%;
}

:deep(.mm-toolbar-brand) {
  display: none;
}

:deep(.mm-toolbar) {
  display: flex;
  flex-direction: row;
}
</style>
