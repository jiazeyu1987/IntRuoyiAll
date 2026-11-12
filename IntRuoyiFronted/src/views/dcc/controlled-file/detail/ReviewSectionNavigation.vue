<template>
  <nav ref="navigation" class="review-section-navigation" aria-label="审核页内定位">
    <button v-for="(section, index) in sections" :key="section.label" type="button"
      :class="{ 'is-active': active === index }" :aria-current="active === index ? 'location' : undefined"
      @click="navigate(index)">{{ section.label }}</button>
  </nav>
</template>
<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, onActivated, watchPostEffect } from 'vue'
const props = defineProps<{ sections: { label: string; element: () => HTMLElement | undefined }[] }>()
const navigation = ref<HTMLElement>()
const active = ref(0)
let frame = 0

const getScrollContainer = () => {
  let container = navigation.value?.parentElement
  while (container && container !== document.body && container !== document.documentElement) {
    if (/auto|scroll/.test(getComputedStyle(container).overflowY)) return container
    container = container.parentElement
  }
  return undefined
}
const getScrollPosition = () => {
  const container = getScrollContainer()
  const viewportTop = container ? container.getBoundingClientRect().top + container.clientTop : 0
  const stickyTop = navigation.value ? parseFloat(getComputedStyle(navigation.value).top) || 0 : 0
  return {
    container,
    scrollTop: container ? container.scrollTop : window.scrollY,
    viewportHeight: container ? container.clientHeight : window.innerHeight,
    scrollHeight: container ? container.scrollHeight : document.documentElement.scrollHeight,
    // Use the sticky position, even when the navigation has not reached it yet.
    headingTop: viewportTop + stickyTop + (navigation.value?.getBoundingClientRect().height || 0) + 12
  }
}
const update = () => {
  const position = getScrollPosition()
  const sections = props.sections.map((section, index) => ({ element: section.element(), index }))
    .filter(section => section.element)
  let current = sections[0]?.index || 0
  sections.forEach(({ element, index }) => {
    if (element!.getBoundingClientRect().top <= position.headingTop + 2) current = index
  })
  if (position.scrollTop > 0 && position.scrollTop + position.viewportHeight >= position.scrollHeight - 2) {
    current = sections[sections.length - 1]?.index || 0
  }
  active.value = current
}
const scheduleUpdate = () => {
  cancelAnimationFrame(frame)
  frame = requestAnimationFrame(update)
}
const navigate = (index: number) => {
  const element = props.sections[index].element()
  if (!element) return
  const position = getScrollPosition()
  active.value = index
  const top = position.scrollTop + element.getBoundingClientRect().top - position.headingTop
  if (position.container) position.container.scrollTo({ top, behavior: 'smooth' })
  else window.scrollTo({ top, behavior: 'smooth' })
}
// Async detail content and responsive navigation heights can change without a scroll event.
watchPostEffect((onCleanup) => {
  const observer = new ResizeObserver(scheduleUpdate)
  const elements = [navigation.value, getScrollContainer(), ...props.sections.map(section => section.element())]
  elements.forEach(element => { if (element) observer.observe(element) })
  scheduleUpdate()
  onCleanup(() => observer.disconnect())
})
onMounted(() => {
  window.addEventListener('scroll', scheduleUpdate, true)
  window.addEventListener('resize', scheduleUpdate)
  scheduleUpdate()
})
onActivated(scheduleUpdate)
onBeforeUnmount(() => {
  cancelAnimationFrame(frame)
  window.removeEventListener('scroll', scheduleUpdate, true)
  window.removeEventListener('resize', scheduleUpdate)
})
</script>
<style scoped>
.review-section-navigation {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  gap: 24px;
  overflow-x: auto;
  margin-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color);
  background: var(--app-content-bg-color);
}
button {
  flex-shrink: 0;
  padding: 12px 16px;
  border: 0;
  border-bottom: 3px solid transparent;
  background: transparent;
  color: var(--el-text-color-primary);
  font-size: 15px;
  cursor: pointer;
}
button.is-active { border-bottom-color: #008577; color: #008577; font-weight: 600; }
button:focus-visible { outline: 2px solid #008577; outline-offset: -2px; }
</style>
