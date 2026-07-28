import router from '@/router'
import type { RouteLocationNormalizedLoaded } from 'vue-router'
import { getRawRoute } from '@/utils/routerHelper'
import { defineStore } from 'pinia'
import { store } from '../index'
import { findIndex } from '@/utils'
import { useUserStoreWithOut } from './user'
import { cloneDeep } from 'lodash-es'

const TAGS_VIEW_PATH_IDENTITY_PATHS = new Set([
  'mes/md/workstation',
  'md/workstation',
  'approval-center/manager/form-center/template',
  'mdm/form-center/template',
  'mes/pro/batch-record-form-list',
  'pro/batch-record-form-list',
  'mes/pro/batch-record-template',
  'pro/batch-record-template'
])

const normalizeTagsViewPath = (path: string) =>
  String(path || '')
    .split('?')[0]
    .replace(/\\/g, '/')
    .replace(/\/+/g, '/')
    .replace(/^\/+/, '')
    .replace(/\/+$/, '')

const resolveActiveMenuPath = (view: RouteLocationNormalizedLoaded) => {
  const activeMenu = normalizeTagsViewPath(String(view.meta?.activeMenu || ''))
  return activeMenu ? `/${activeMenu}` : ''
}

export interface TagsViewState {
  visitedViews: RouteLocationNormalizedLoaded[]
  cachedViews: Set<string>
  selectedTag?: RouteLocationNormalizedLoaded
}

export const useTagsViewStore = defineStore('tagsView', {
  state: (): TagsViewState => ({
    visitedViews: [],
    cachedViews: new Set(),
    selectedTag: undefined
  }),
  getters: {
    getVisitedViews(): RouteLocationNormalizedLoaded[] {
      return this.visitedViews
    },
    getCachedViews(): string[] {
      return Array.from(this.cachedViews)
    },
    getSelectedTag(): RouteLocationNormalizedLoaded | undefined {
      return this.selectedTag
    }
  },
  actions: {
    getViewIdentity(view: RouteLocationNormalizedLoaded): string {
      const normalizedPath = normalizeTagsViewPath(view.path)
      if (
        view.meta?.tagsViewKeyMode === 'path' ||
        TAGS_VIEW_PATH_IDENTITY_PATHS.has(normalizedPath)
      ) {
        return normalizedPath ? `/${normalizedPath}` : view.path
      }
      return String(view.meta?.tagsViewKey || view.fullPath)
    },
    normalizeVisitedView(view: RouteLocationNormalizedLoaded): RouteLocationNormalizedLoaded {
      const visitedView = Object.assign({}, view, { title: view.meta?.title || 'no-name' })
      if (visitedView.meta?.tagsViewTitle) {
        visitedView.meta.title = visitedView.meta.tagsViewTitle as string
      }
      return visitedView
    },
    // 新增缓存和tag
    addView(view: RouteLocationNormalizedLoaded): void {
      this.addVisitedView(view)
      this.addCachedView()
    },
    // 新增tag
    addVisitedView(view: RouteLocationNormalizedLoaded) {
      if (view.meta?.noTagsView) return
      const identity = this.getViewIdentity(view)
      const existedView = this.visitedViews.find((v) => this.getViewIdentity(v) === identity)
      const visitedView = this.normalizeVisitedView(view)

      if (existedView) {
        Object.assign(existedView, visitedView)
        return
      }

      if (visitedView.meta) {
        const titleSuffixList: string[] = []
        this.visitedViews.forEach((v) => {
          if (
            this.getViewIdentity(v) !== identity &&
            v.path === visitedView.path &&
            v.meta?.title === visitedView.meta?.title
          ) {
            titleSuffixList.push(v.meta?.titleSuffix || '1')
          }
        })
        if (titleSuffixList.length) {
          let titleSuffix = 1
          while (titleSuffixList.includes(`${titleSuffix}`)) {
            titleSuffix += 1
          }
          visitedView.meta.titleSuffix = titleSuffix === 1 ? undefined : `${titleSuffix}`
        }
      }

      this.visitedViews.push(visitedView)
    },
    replaceActiveMenuView(view: RouteLocationNormalizedLoaded) {
      const activeMenuPath = resolveActiveMenuPath(view)
      if (!activeMenuPath) return undefined

      const viewIndex = this.visitedViews.findIndex(
        (visitedView) => visitedView.path === activeMenuPath
      )
      const previousView =
        viewIndex >= 0 ? cloneDeep(this.visitedViews[viewIndex]) : undefined
      const nextView = this.normalizeVisitedView(cloneDeep(view))

      if (viewIndex >= 0) {
        this.visitedViews.splice(viewIndex, 1, nextView)
      } else {
        this.visitedViews.push(nextView)
      }
      this.setSelectedTag(nextView)
      this.addCachedView()
      return previousView
    },
    restoreActiveMenuView(
      view: RouteLocationNormalizedLoaded,
      snapshot?: RouteLocationNormalizedLoaded
    ) {
      const viewIdentity = this.getViewIdentity(view)
      const viewIndex = this.visitedViews.findIndex(
        (visitedView) => this.getViewIdentity(visitedView) === viewIdentity
      )
      if (viewIndex < 0) return

      if (snapshot) {
        this.visitedViews.splice(viewIndex, 1, snapshot)
      } else {
        this.visitedViews.splice(viewIndex, 1)
      }
      this.addCachedView()
    },
    // 新增缓存
    addCachedView() {
      const cacheMap: Set<string> = new Set()
      for (const v of this.visitedViews) {
        const item = getRawRoute(v)
        const needCache = !item.meta?.noCache
        if (!needCache) {
          continue
        }
        const name = item.name as string
        cacheMap.add(name)
      }
      if (Array.from(this.cachedViews).sort().toString() === Array.from(cacheMap).sort().toString())
        return
      this.cachedViews = cacheMap
    },
    // 删除某个
    delView(view: RouteLocationNormalizedLoaded) {
      this.delVisitedView(view)
      this.addCachedView()
    },
    // 删除tag
    delVisitedView(view: RouteLocationNormalizedLoaded) {
      const identity = this.getViewIdentity(view)
      for (const [i, v] of this.visitedViews.entries()) {
        if (this.getViewIdentity(v) === identity) {
          this.visitedViews.splice(i, 1)
          break
        }
      }
    },
    // 删除缓存
    delCachedView() {
      const route = router.currentRoute.value
      const index = findIndex<string>(this.getCachedViews, (v) => v === route.name)
      // 需要注释，解决“标签页刷新无效”。相关案例：https://github.com/yudaocode/yudao-ui-admin-vue3/issues/180
      // for (const v of this.visitedViews) {
      //   if (v.name === route.name) {
      //     return
      //   }
      // }
      if (index > -1) {
        this.cachedViews.delete(this.getCachedViews[index])
      }
    },
    // 删除所有缓存和tag
    delAllViews() {
      this.delAllVisitedViews()
      this.addCachedView()
    },
    // 删除所有tag
    delAllVisitedViews() {
      const userStore = useUserStoreWithOut()

      // const affixTags = this.visitedViews.filter((tag) => tag.meta.affix)
      this.visitedViews = userStore.getUser
        ? this.visitedViews.filter((tag) => tag?.meta?.affix)
        : []
    },
    // 删除其他
    delOthersViews(view: RouteLocationNormalizedLoaded) {
      this.delOthersVisitedViews(view)
      this.addCachedView()
    },
    // 删除其他tag
    delOthersVisitedViews(view: RouteLocationNormalizedLoaded) {
      this.visitedViews = this.visitedViews.filter((v) => {
        return v?.meta?.affix || v.fullPath === view.fullPath
      })
    },
    // 删除左侧
    delLeftViews(view: RouteLocationNormalizedLoaded) {
      const index = findIndex<RouteLocationNormalizedLoaded>(
        this.visitedViews,
        (v) => v.fullPath === view.fullPath
      )
      if (index > -1) {
        this.visitedViews = this.visitedViews.filter((v, i) => {
          return v?.meta?.affix || v.fullPath === view.fullPath || i > index
        })
        this.addCachedView()
      }
    },
    // 删除右侧
    delRightViews(view: RouteLocationNormalizedLoaded) {
      const index = findIndex<RouteLocationNormalizedLoaded>(
        this.visitedViews,
        (v) => v.fullPath === view.fullPath
      )
      if (index > -1) {
        this.visitedViews = this.visitedViews.filter((v, i) => {
          return v?.meta?.affix || v.fullPath === view.fullPath || i < index
        })
        this.addCachedView()
      }
    },
    updateVisitedView(view: RouteLocationNormalizedLoaded) {
      const identity = this.getViewIdentity(view)
      for (let v of this.visitedViews) {
        if (this.getViewIdentity(v) === identity) {
          v = Object.assign(v, this.normalizeVisitedView(view))
          break
        }
      }
    },
    // 设置当前选中的 tag
    setSelectedTag(tag: RouteLocationNormalizedLoaded) {
      this.selectedTag = tag
    },
    setTitle(title: string, path?: string) {
      for (const v of this.visitedViews) {
        if (v.path === (path ?? this.selectedTag?.path)) {
          v.meta.title = title
          break
        }
      }
    }
  },
  persist: false
})

export const useTagsViewStoreWithOut = () => {
  return useTagsViewStore(store)
}
