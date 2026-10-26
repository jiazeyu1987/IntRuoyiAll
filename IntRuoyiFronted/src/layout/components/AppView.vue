<script lang="ts" setup>
import type { RouteLocationNormalizedLoaded } from 'vue-router'
import { useTagsViewStore } from '@/store/modules/tagsView'
import { useAppStore } from '@/store/modules/app'
import { useEdhrListViewport } from '@/hooks/web/useEdhrListViewport'

defineOptions({ name: 'AppView' })

const appStore = useAppStore()

const layout = computed(() => appStore.getLayout)

const fixedHeader = computed(() => appStore.getFixedHeader)

const tagsViewStore = useTagsViewStore()
const currentRoute = useRoute()
const isEdhrPage = computed(() =>
  currentRoute.matched.some((route) => route.path === '/mes/edhr-batch-processing')
)
const contentRef = ref<HTMLElement>()
useEdhrListViewport(contentRef, isEdhrPage)

const resolveKeepAliveName = (route: RouteLocationNormalizedLoaded) =>
  String(route.meta?.keepAliveName || route.name || '')

const getCaches = computed((): string[] => {
  const caches = new Set(tagsViewStore.getCachedViews)
  const keepAliveName = resolveKeepAliveName(currentRoute)
  if (keepAliveName && currentRoute.meta?.noCache !== true) {
    caches.add(keepAliveName)
  }
  return Array.from(caches)
})

const tagsView = computed(() => appStore.getTagsView)
const resolveRouteViewKey = (route: { path: string }) => route.path

//region 无感刷新
const routerAlive = ref(true)
// 无感刷新，防止出现页面闪烁白屏
const reload = () => {
  routerAlive.value = false
  nextTick(() => (routerAlive.value = true))
}
// 为组件后代提供刷新方法
provide('reload', reload)
//endregion

void [layout, fixedHeader, tagsView]
</script>

<template>
  <section
    ref="contentRef"
    :class="{ 'edhr-content': isEdhrPage }"
    class="px-[var(--app-content-padding)] pt-[var(--app-content-padding)] pb-0 w-full bg-[var(--app-content-bg-color)] dark:bg-[var(--el-bg-color)]"
  >
    <router-view v-if="routerAlive">
      <template #default="{ Component, route }">
        <keep-alive :include="getCaches">
          <component :is="Component" :key="resolveRouteViewKey(route)" />
        </keep-alive>
      </template>
    </router-view>
  </section>
</template>

<style scoped>
/* eDHR 列表在分页后结束，不再叠加分页与列表卡片的底部外边距。 */
.edhr-content :deep(.el-pagination),
.edhr-content :deep(.el-card.mb-15px:has(.el-table)) {
  margin-bottom: 0;
}
</style>
