<script lang="tsx">
import { usePermissionStore } from '@/store/modules/permission'
import { useAppStore } from '@/store/modules/app'

import { ElScrollbar } from 'element-plus'
import { Icon } from '@/components/Icon'
import { Menu } from '@/layout/components/Menu'
import { pathResolve } from '@/utils/routerHelper'
import { cloneDeep } from 'lodash-es'
import { filterMenusPath, initTabMap, tabPathMap } from './helper'
import { useDesign } from '@/hooks/web/useDesign'
import { isUrl } from '@/utils/is'
import {
  hasApprovalTodoBadgeRoute,
  useApprovalTodoBadgeStore
} from '@/store/modules/approvalTodoBadge'
import {
  hasProfileWorkbenchTodoBadgeRoute,
  useProfileWorkbenchTodoBadgeStore
} from '@/store/modules/profileWorkbenchTodoBadge'

const { getPrefixCls, variables } = useDesign()

const prefixCls = getPrefixCls('tab-menu')

export default defineComponent({
  name: 'TabMenu',
  setup() {
    const { push, currentRoute } = useRouter()

    const { t } = useI18n()

    const appStore = useAppStore()

    const collapse = computed(() => appStore.getCollapse)

    const fixedMenu = computed(() => appStore.getFixedMenu)

    const permissionStore = usePermissionStore()
    const approvalTodoBadgeStore = useApprovalTodoBadgeStore()
    const profileWorkbenchTodoBadgeStore = useProfileWorkbenchTodoBadgeStore()

    const routers = computed(() => permissionStore.getRouters)

    const tabRouters = computed(() => unref(routers).filter((v) => !v?.meta?.hidden))

    const shouldShowApprovalTodoBadge = (meta?: AppRouteRecordRaw['meta']) =>
      Boolean(meta?.approvalTodoBadge && approvalTodoBadgeStore.loaded)

    const shouldShowProfileWorkbenchTodoBadge = (meta?: AppRouteRecordRaw['meta']) =>
      Boolean(meta?.personalWorkbenchTodoBadge && profileWorkbenchTodoBadgeStore.loaded)

    const reportApprovalTodoBadgeError = (error: unknown) => {
      console.error('审批待办数量加载失败', error)
    }

    const loadApprovalTodoBadge = () => {
      if (!hasApprovalTodoBadgeRoute(unref(routers))) {
        return
      }
      void approvalTodoBadgeStore.ensureTodoTotalLoaded().catch(reportApprovalTodoBadgeError)
    }

    const reportProfileWorkbenchTodoBadgeError = (error: unknown) => {
      console.error('个人工作台待处理数量加载失败', error)
    }

    const loadProfileWorkbenchTodoBadge = () => {
      if (!hasProfileWorkbenchTodoBadgeRoute(unref(routers))) {
        return
      }
      void profileWorkbenchTodoBadgeStore
        .ensureTodoTotalLoaded()
        .catch(reportProfileWorkbenchTodoBadgeError)
    }

    const setCollapse = () => {
      appStore.setCollapse(!unref(collapse))
    }

    watch(
      () => routers.value,
      (routers: AppRouteRecordRaw[]) => {
        initTabMap(routers)
        filterMenusPath(routers, routers)
        loadApprovalTodoBadge()
        loadProfileWorkbenchTodoBadge()
      },
      {
        immediate: true,
        deep: true
      }
    )

    const showTitle = ref(true)

    watch(
      () => collapse.value,
      (collapse: boolean) => {
        if (!collapse) {
          setTimeout(() => {
            showTitle.value = !collapse
          }, 200)
        } else {
          showTitle.value = !collapse
        }
      }
    )

    // 是否显示菜单
    const showMenu = ref(unref(fixedMenu) ? true : false)

    // tab高亮
    const tabActive = ref('')

    const resolveCurrentTopLevelPath = () => `/${unref(currentRoute).path.split('/')[1]}`

    const resolveChildrenForTopLevelPath = (path: string) =>
      unref(tabRouters).find(
        (v) =>
          (v.meta?.alwaysShow || (v?.children?.length && v?.children?.length > 1)) && v.path === path
      )?.children

    const syncMenuTabRoutersByRoute = () => {
      const path = resolveCurrentTopLevelPath()
      const children = resolveChildrenForTopLevelPath(path)

      tabActive.value = path

      if (!children?.length) {
        permissionStore.setMenuTabRouters([])
        if (unref(fixedMenu)) {
          showMenu.value = false
        }
        return
      }

      permissionStore.setMenuTabRouters(
        cloneDeep(children).map((v) => {
          v.path = pathResolve(path, v.path)
          return v
        })
      )

      if (unref(fixedMenu)) {
        showMenu.value = true
      }
    }

    onMounted(() => {
      syncMenuTabRoutersByRoute()
      loadApprovalTodoBadge()
      loadProfileWorkbenchTodoBadge()
    })

    watch(
      [() => unref(currentRoute).path, tabRouters, fixedMenu],
      () => {
        syncMenuTabRoutersByRoute()
      },
      {
        immediate: true,
        deep: true
      }
    )

    // tab点击事件
    const tabClick = (item: AppRouteRecordRaw) => {
      if (isUrl(item.path)) {
        window.open(item.path)
        return
      }
      const newPath = item.children ? item.path : item.path.split('/')[0]
      const oldPath = unref(tabActive)
      tabActive.value = item.children ? item.path : item.path.split('/')[0]
      if (item.children) {
        if (newPath === oldPath || !unref(showMenu)) {
          showMenu.value = unref(fixedMenu) ? true : !unref(showMenu)
        }
        if (unref(showMenu)) {
          permissionStore.setMenuTabRouters(
            cloneDeep(item.children).map((v) => {
              v.path = pathResolve(unref(tabActive), v.path)
              return v
            })
          )
        }
      } else {
        push(item.path)
        permissionStore.setMenuTabRouters([])
        showMenu.value = false
      }
    }

    // 设置高亮
    const isActive = (currentPath: string) => {
      const { path } = unref(currentRoute)
      if (tabPathMap[currentPath].includes(path)) {
        return true
      }
      return false
    }

    const mouseleave = () => {
      if (!unref(showMenu) || unref(fixedMenu)) return
      showMenu.value = false
    }

    return () => (
      <div
        id={`${variables.namespace}-menu`}
        class={[
          prefixCls,
          'relative bg-[var(--left-menu-bg-color)] layout-border__right',
          {
            'w-[var(--tab-menu-max-width)]': !unref(collapse),
            'w-[var(--tab-menu-min-width)]': unref(collapse)
          }
        ]}
        onMouseleave={mouseleave}
      >
        <ElScrollbar class="!h-[calc(100%-var(--tab-menu-collapse-height))]">
          <div>
            {() => {
              return unref(tabRouters).map((v) => {
                const item = (
                  v.meta?.alwaysShow || (v?.children?.length && v?.children?.length > 1)
                    ? v
                    : {
                        ...(v?.children && v?.children[0]),
                        path: pathResolve(v.path, (v?.children && v?.children[0])?.path as string)
                      }
                ) as AppRouteRecordRaw
                return (
                  <div
                    class={[
                      `${prefixCls}__item`,
                      'text-center text-12px relative py-12px cursor-pointer',
                      {
                        'is-active': isActive(v.path)
                      }
                    ]}
                    onClick={() => {
                      tabClick(item)
                    }}
                  >
                    <div>
                      <Icon icon={item?.meta?.icon}></Icon>
                    </div>
                    {!unref(showTitle) ? undefined : (
                      <p class="mt-5px break-words px-2px">
                        {t(item.meta?.title)}
                        {shouldShowApprovalTodoBadge(item.meta) ? (
                          <span
                            class="approval-todo-badge"
                            aria-label={`待办数量 ${approvalTodoBadgeStore.todoTotal}`}
                          >
                            {approvalTodoBadgeStore.todoTotal}
                          </span>
                        ) : undefined}
                        {shouldShowProfileWorkbenchTodoBadge(item.meta) ? (
                          <span
                            class="personal-workbench-todo-badge"
                            aria-label={`个人工作台待处理数量 ${profileWorkbenchTodoBadgeStore.todoTotal}`}
                          >
                            {profileWorkbenchTodoBadgeStore.todoTotal}
                          </span>
                        ) : undefined}
                      </p>
                    )}
                  </div>
                )
              })
            }}
          </div>
        </ElScrollbar>
        <div
          class={[
            `${prefixCls}--collapse`,
            'text-center h-[var(--tab-menu-collapse-height)] leading-[var(--tab-menu-collapse-height)] cursor-pointer'
          ]}
          onClick={setCollapse}
        >
          <Icon icon={unref(collapse) ? 'ep:d-arrow-right' : 'ep:d-arrow-left'}></Icon>
        </div>
        <Menu
          class={[
            '!absolute top-0 z-11',
            {
              '!left-[var(--tab-menu-min-width)]': unref(collapse),
              '!left-[var(--tab-menu-max-width)]': !unref(collapse),
              '!w-[var(--left-menu-max-width)]': unref(showMenu) || unref(fixedMenu),
              '!w-0': !unref(showMenu) && !unref(fixedMenu)
            }
          ]}
          style="transition: width var(--transition-time-02), left var(--transition-time-02);"
        ></Menu>
      </div>
    )
  }
})
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-tab-menu;

.#{$prefix-cls} {
  transition: all var(--transition-time-02);

  &__item {
    color: var(--left-menu-text-color);
    transition: all var(--transition-time-02);

    &:hover {
      color: var(--left-menu-text-active-color);
      // background-color: var(--left-menu-bg-active-color);
    }
  }

  &--collapse {
    color: var(--left-menu-text-color);
    background-color: var(--left-menu-bg-light-color);
  }

  .is-active {
    color: var(--left-menu-text-active-color);
    background-color: var(--left-menu-bg-active-color);
  }

  .approval-todo-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 24px;
    max-width: none;
    height: 18px;
    padding: 0 6px;
    margin-left: 6px;
    overflow: visible;
    color: #1677ff;
    font-size: 12px;
    font-weight: 600;
    line-height: 18px;
    white-space: nowrap;
    vertical-align: middle;
    background: #e8f2ff;
    border: 1px solid #c7ddff;
    border-radius: 9px;
    box-sizing: border-box;
  }

  .personal-workbench-todo-badge {
    @extend .approval-todo-badge;
  }
}
</style>
