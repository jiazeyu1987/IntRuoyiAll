<template>
  <div
    ref="viewportRef"
    class="edhr-template-fit-viewport"
    :class="{ 'is-height-fit': !widthOnly, 'is-width-fit': widthOnly }"
  >
    <div class="edhr-template-fit-viewport__frame" :style="frameStyle">
      <div class="edhr-template-fit-viewport__scaled" :style="scaledStyle">
        <div ref="measureRef" class="edhr-template-fit-viewport__measure">
          <slot></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'EdhrTemplateFitViewport' })

const props = defineProps<{
  widthOnly?: boolean
}>()

const viewportRef = ref<HTMLDivElement>()
const measureRef = ref<HTMLDivElement>()
const scale = ref(1)
const contentSize = ref({ width: 1, height: 1 })
let resizeObserver: ResizeObserver | null = null
let resizeFrame = 0

const scaledWidth = computed(() => Math.max(contentSize.value.width * scale.value, 1))
const scaledHeight = computed(() => Math.max(contentSize.value.height * scale.value, 1))

const frameStyle = computed(() => ({
  width: `${scaledWidth.value}px`,
  height: `${scaledHeight.value}px`
}))

const scaledStyle = computed(() => ({
  width: `${Math.max(contentSize.value.width, 1)}px`,
  height: `${Math.max(contentSize.value.height, 1)}px`,
  transform: `scale(${scale.value})`,
  transformOrigin: 'top left'
}))

const syncViewportScale = () => {
  const viewport = viewportRef.value
  const measure = measureRef.value
  if (!viewport || !measure) return
  const nextWidth = Math.max(measure.scrollWidth, measure.offsetWidth, 1)
  const nextHeight = Math.max(measure.scrollHeight, measure.offsetHeight, 1)
  const nextContentSize = { width: nextWidth, height: nextHeight }
  if (
    contentSize.value.width !== nextContentSize.width ||
    contentSize.value.height !== nextContentSize.height
  ) {
    contentSize.value = nextContentSize
  }
  const widthScale = viewport.clientWidth > 0 ? viewport.clientWidth / nextWidth : 1
  const heightScale = viewport.clientHeight > 0 ? viewport.clientHeight / nextHeight : 1
  const nextScale = props.widthOnly ? widthScale : Math.min(widthScale, heightScale)
  const normalizedScale = Number.isFinite(nextScale) && nextScale > 0 ? nextScale : 1
  if (Math.abs(scale.value - normalizedScale) > 0.0001) {
    scale.value = normalizedScale
  }
}

const scheduleViewportScale = () => {
  if (resizeFrame) {
    cancelAnimationFrame(resizeFrame)
  }
  resizeFrame = requestAnimationFrame(() => {
    resizeFrame = 0
    syncViewportScale()
  })
}

onMounted(() => {
  scheduleViewportScale()
  resizeObserver = new ResizeObserver(() => {
    scheduleViewportScale()
  })
  if (viewportRef.value) {
    resizeObserver.observe(viewportRef.value)
  }
  if (measureRef.value) {
    resizeObserver.observe(measureRef.value)
  }
})

onUpdated(() => {
  scheduleViewportScale()
})

watch(
  () => props.widthOnly,
  () => scheduleViewportScale()
)

onBeforeUnmount(() => {
  if (resizeFrame) {
    cancelAnimationFrame(resizeFrame)
    resizeFrame = 0
  }
  resizeObserver?.disconnect()
  resizeObserver = null
})
</script>

<style scoped>
.edhr-template-fit-viewport {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #fff;
}

.edhr-template-fit-viewport.is-height-fit {
  align-items: center;
}

.edhr-template-fit-viewport.is-width-fit {
  height: auto;
  min-height: 100%;
  overflow: visible;
}

.edhr-template-fit-viewport__frame {
  flex: 0 0 auto;
  max-width: 100%;
  max-height: 100%;
}

.edhr-template-fit-viewport__scaled {
  transform-origin: top left;
}

.edhr-template-fit-viewport__measure {
  display: inline-block;
  vertical-align: top;
}
</style>
