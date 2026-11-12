<template>
  <nav ref="navigation" class="review-section-navigation" aria-label="审核页内定位">
    <button v-for="(section, index) in sections" :key="section.label" type="button"
      :class="{ 'is-active': active === index }" :aria-current="active === index ? 'location' : undefined"
      @click="navigate(index)">{{ section.label }}</button>
  </nav>
</template>
<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
const props = defineProps<{ sections: { label: string; element: () => HTMLElement | undefined }[] }>()
const navigation = ref<HTMLElement>()
const active = ref(0)
let frame = 0
const update = () => {
  const threshold = (navigation.value?.getBoundingClientRect().bottom || 0) + 24
  let current = 0
  props.sections.forEach((section, index) => {
    const element = section.element()
    if (element && element.getBoundingClientRect().top <= threshold) current = index
  })
  let container = navigation.value?.parentElement
  while (container) {
    if (container.scrollHeight > container.clientHeight && /auto|scroll/.test(getComputedStyle(container).overflowY)) {
      if (container.scrollTop > 0 && container.scrollTop + container.clientHeight >= container.scrollHeight - 2)
        current = props.sections.length - 1
      break
    }
    container = container.parentElement
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
  active.value = index
  element.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
onMounted(() => {
  window.addEventListener('scroll', scheduleUpdate, true)
  window.addEventListener('resize', scheduleUpdate)
  scheduleUpdate()
})
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
