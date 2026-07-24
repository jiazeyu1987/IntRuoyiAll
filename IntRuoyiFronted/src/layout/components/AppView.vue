<script lang="ts" setup>
import { useTagsViewStore } from '@/store/modules/tagsView'
import { useAppStore } from '@/store/modules/app'
import { Footer } from '@/layout/components/Footer'

defineOptions({ name: 'AppView' })

const appStore = useAppStore()

const layout = computed(() => appStore.getLayout)

const fixedHeader = computed(() => appStore.getFixedHeader)

const footer = computed(() => appStore.getFooter)

const tagsViewStore = useTagsViewStore()
const currentRoute = useRoute()

const getCaches = computed((): string[] => {
  const caches = new Set(tagsViewStore.getCachedViews)
  if (currentRoute.name && currentRoute.meta?.noCache !== true) {
    caches.add(String(currentRoute.name))
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
    :class="[
      'p-[var(--app-content-padding)] w-full bg-[var(--app-content-bg-color)] dark:bg-[var(--el-bg-color)]',
      {
        '!min-h-[calc(100vh-var(--top-tool-height)-var(--tags-view-height)-var(--app-footer-height))] pb-0':
          footer
      }
    ]"
  >
    <router-view v-if="routerAlive">
      <template #default="{ Component, route }">
        <keep-alive :include="getCaches">
          <component :is="Component" :key="resolveRouteViewKey(route)" />
        </keep-alive>
      </template>
    </router-view>
  </section>
  <Footer v-if="footer" />
</template>
