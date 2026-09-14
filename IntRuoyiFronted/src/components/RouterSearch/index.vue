<template>
  <ElDialog v-if="isModal" v-model="showSearch" :show-close="false" title="菜单搜索">
    <el-select
      filterable
      :reserve-keyword="false"
      remote
      placeholder="请输入菜单内容"
      :remote-method="remoteMethod"
      style="width: 100%"
      @change="handleChange"
      @visible-change="handleVisibleChange"
    >
      <template v-if="showHistoryGroup">
        <el-option-group label="最近搜索">
          <el-option
            v-for="item in recentOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          >
            <div class="flex items-center justify-between gap-12px">
              <span class="truncate">{{ item.label }}</span>
              <span class="max-w-120px truncate text-[12px] text-[var(--el-text-color-placeholder)]">
                {{ item.query }}
              </span>
            </div>
          </el-option>
        </el-option-group>
      </template>
      <template v-else>
        <el-option
          v-for="item in filteredOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </template>
    </el-select>
  </ElDialog>
  <div v-else-if="alwaysVisible" class="flex items-center">
    <Icon icon="ep:search" :color="color" />
    <el-select
      @click.stop
      filterable
      :reserve-keyword="false"
      remote
      placeholder="请输入菜单内容"
      :remote-method="remoteMethod"
      class="ml2 !w-220px"
      @change="handleChange"
      @visible-change="handleVisibleChange"
    >
      <template v-if="showHistoryGroup">
        <el-option-group label="最近搜索">
          <el-option
            v-for="item in recentOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          >
            <div class="flex items-center justify-between gap-12px">
              <span class="truncate">{{ item.label }}</span>
              <span class="max-w-120px truncate text-[12px] text-[var(--el-text-color-placeholder)]">
                {{ item.query }}
              </span>
            </div>
          </el-option>
        </el-option-group>
      </template>
      <template v-else>
        <el-option
          v-for="item in filteredOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </template>
    </el-select>
  </div>
  <div v-else class="custom-hover" @click.stop="showTopSearch = !showTopSearch">
    <Icon icon="ep:search" :color="color" />
    <el-select
      @click.stop
      filterable
      :reserve-keyword="false"
      remote
      placeholder="请输入菜单内容"
      :remote-method="remoteMethod"
      class="overflow-hidden transition-all-600"
      :class="showTopSearch ? '!w-220px ml2' : '!w-0'"
      @change="handleChange"
      @visible-change="handleVisibleChange"
    >
      <template v-if="showHistoryGroup">
        <el-option-group label="最近搜索">
          <el-option
            v-for="item in recentOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          >
            <div class="flex items-center justify-between gap-12px">
              <span class="truncate">{{ item.label }}</span>
              <span class="max-w-120px truncate text-[12px] text-[var(--el-text-color-placeholder)]">
                {{ item.query }}
              </span>
            </div>
          </el-option>
        </el-option-group>
      </template>
      <template v-else>
        <el-option
          v-for="item in filteredOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </template>
    </el-select>
  </div>
</template>

<script lang="ts" setup>
import { useCache } from '@/hooks/web/useCache'
import { propTypes } from '@/utils/propTypes'

interface SearchHistoryRecord {
  label: string
  value: string
  query: string
}

const ROUTER_SEARCH_HISTORY_KEY = 'routerSearchHistory'
const ROUTER_SEARCH_HISTORY_LIMIT = 20
const ROUTER_SEARCH_ALIASES: Record<string, string[]> = {
  '/mes/pro/mes-process': ['MES工序']
}

defineProps({
  isModal: {
    type: Boolean,
    default: true
  },
  alwaysVisible: {
    type: Boolean,
    default: false
  },
  color: propTypes.string.def('')
})

const { wsCache } = useCache()
const router = useRouter()
const showSearch = ref(false)
const showTopSearch = ref(false)
const value = ref('')

function getSearchRoutes() {
  return router.getRoutes()
}

const recentOptions = ref<SearchHistoryRecord[]>(loadSearchHistory())

const keyword = computed(() => value.value.trim())
const filteredOptions = computed<SearchHistoryRecord[]>(() => {
  if (!keyword.value) {
    return []
  }
  const list = getSearchRoutes().filter((item: any) => {
    return isSearchableRoute(item) && routeMatchesSearchQuery(item, keyword.value)
  })
  return list.map((item) => createSearchRecord(item, keyword.value))
})
const showHistoryGroup = computed(() => !keyword.value && recentOptions.value.length > 0)

function normalizeSearchText(value: unknown) {
  return String(value || '')
    .trim()
    .toLocaleLowerCase()
}

function normalizeSearchPath(path: string) {
  const rawPath = String(path || '').split(/[?#]/)[0].trim()
  const withLeadingSlash = rawPath.startsWith('/') ? rawPath : `/${rawPath}`
  return withLeadingSlash.replace(/\/+/g, '/').replace(/\/$/, '') || '/'
}

function routePathMatches(routePath: string, path: string) {
  const routeParts = normalizeSearchPath(routePath).split('/').filter(Boolean)
  const pathParts = normalizeSearchPath(path).split('/').filter(Boolean)
  if (routeParts.length !== pathParts.length) {
    return false
  }
  return routeParts.every((part, index) => part.startsWith(':') || part === pathParts[index])
}

function findRouteBySearchPath(path: string) {
  return getSearchRoutes().find((route: any) => routePathMatches(route.path, path))
}

function isSearchableRoute(route: any) {
  return Boolean(route?.path && route?.meta?.title && !route.meta?.hidden)
}

function getRouteSearchAliases(route: any) {
  return ROUTER_SEARCH_ALIASES[normalizeSearchPath(route?.path)] || []
}

function routeMatchesSearchQuery(route: any, query: string) {
  const normalizedQuery = normalizeSearchText(query)
  if (!normalizedQuery) {
    return false
  }
  return [route.meta?.title, route.path, ...getRouteSearchAliases(route)].some((text) =>
    normalizeSearchText(text).includes(normalizedQuery)
  )
}

function createSearchRecord(route: any, query: string): SearchHistoryRecord {
  return {
    label: `${route.meta.title ?? String(route.name ?? route.path)}${route.path}`,
    value: route.path,
    query
  }
}

function resolveSearchablePath(path: string) {
  const route = findRouteBySearchPath(path)
  if (!route) {
    return ''
  }
  if (isSearchableRoute(route)) {
    return route.path
  }
  const activeMenu =
    typeof route.meta?.activeMenu === 'string' ? normalizeSearchPath(route.meta.activeMenu) : ''
  if (!activeMenu) {
    return ''
  }
  const activeMenuRoute = findRouteBySearchPath(activeMenu)
  return activeMenuRoute && isSearchableRoute(activeMenuRoute) ? activeMenuRoute.path : ''
}

function resolveSearchableHistoryRecord(record: SearchHistoryRecord) {
  const resolvedPath = resolveSearchablePath(record.value)
  if (!resolvedPath) {
    return undefined
  }
  const route = findRouteBySearchPath(resolvedPath)
  if (!route || !isSearchableRoute(route)) {
    return undefined
  }
  return createSearchRecord(route, record.query)
}

function dedupeSearchHistory(records: SearchHistoryRecord[]) {
  const seen = new Set<string>()
  return records.filter((record) => {
    if (seen.has(record.value)) {
      return false
    }
    seen.add(record.value)
    return true
  })
}

function isSameSearchHistory(stored: unknown[], records: SearchHistoryRecord[]) {
  if (stored.length !== records.length) {
    return false
  }
  return records.every((record, index) => {
    const storedRecord = stored[index] as SearchHistoryRecord
    return (
      storedRecord?.label === record.label &&
      storedRecord?.value === record.value &&
      storedRecord?.query === record.query
    )
  })
}

function loadSearchHistory(): SearchHistoryRecord[] {
  const stored = wsCache.get(ROUTER_SEARCH_HISTORY_KEY)
  if (!Array.isArray(stored)) {
    return []
  }
  const records = stored
    .filter((item): item is SearchHistoryRecord => {
      return (
        typeof item?.label === 'string' &&
        typeof item?.value === 'string' &&
        typeof item?.query === 'string'
      )
    })
    .map((item) => resolveSearchableHistoryRecord(item))
    .filter((item): item is SearchHistoryRecord => Boolean(item))

  const normalizedRecords = dedupeSearchHistory(records)
    .slice(0, ROUTER_SEARCH_HISTORY_LIMIT)

  if (!isSameSearchHistory(stored, normalizedRecords)) {
    wsCache.set(ROUTER_SEARCH_HISTORY_KEY, normalizedRecords)
  }
  return normalizedRecords
}

function persistSearchHistory() {
  wsCache.set(ROUTER_SEARCH_HISTORY_KEY, recentOptions.value.slice(0, ROUTER_SEARCH_HISTORY_LIMIT))
}

function recordSearchHistory(record: SearchHistoryRecord) {
  const normalizedRecord = resolveSearchableHistoryRecord(record)
  if (!normalizedRecord) {
    return
  }
  recentOptions.value = [
    normalizedRecord,
    ...recentOptions.value.filter((item) => item.value !== normalizedRecord.value)
  ].slice(0, ROUTER_SEARCH_HISTORY_LIMIT)
  persistSearchHistory()
}

function remoteMethod(data: string) {
  value.value = data
}

function handleChange(path: string) {
  const resolvedPath = resolveSearchablePath(path)
  const selectedRecord =
    filteredOptions.value.find((item) => item.value === path) ??
    recentOptions.value.find((item) => item.value === path)

  if (selectedRecord && resolvedPath) {
    recordSearchHistory({
      label: selectedRecord.label,
      value: resolvedPath,
      query: keyword.value || selectedRecord.query
    })
  }

  value.value = ''
  if (resolvedPath) {
    router.push({ path: resolvedPath })
  }
  hiddenSearch()
  hiddenTopSearch()
}

function handleVisibleChange(visible: boolean) {
  if (visible) {
    recentOptions.value = loadSearchHistory()
  }
}

function hiddenSearch() {
  showSearch.value = false
}

function hiddenTopSearch() {
  showTopSearch.value = false
}

onMounted(() => {
  window.addEventListener('keydown', listenKey)
  window.addEventListener('click', hiddenTopSearch)
})

onUnmounted(() => {
  window.removeEventListener('keydown', listenKey)
  window.removeEventListener('click', hiddenTopSearch)
})

function listenKey(event: KeyboardEvent) {
  if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
    event.preventDefault()
    showSearch.value = !showSearch.value
  }
}

defineExpose({
  openSearch: () => {
    showSearch.value = true
  }
})
</script>
