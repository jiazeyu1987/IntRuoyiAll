import { ElSubMenu, ElMenuItem } from 'element-plus'
import { isUrl } from '@/utils/is'
import { useRenderMenuTitle } from './useRenderMenuTitle'
import { pathResolve } from '@/utils/routerHelper'

const { renderMenuTitle } = useRenderMenuTitle()

export const useRenderMenuItem = () =>
  // allRouters: AppRouteRecordRaw[] = [],
  {
    const { push } = useRouter()

    const handleSubMenuTitleClick = (meta: AppRouteRecordRaw['meta'], fullPath: string) => {
      if (!meta?.hasParentRoute) {
        return undefined
      }
      return (event: MouseEvent) => {
        const target = event.target as HTMLElement | null
        if (target?.closest('.el-sub-menu__icon-arrow')) {
          return
        }
        if (isUrl(fullPath)) {
          window.open(fullPath)
        } else {
          push(fullPath)
        }
      }
    }

    const renderMenuItem = (routers: AppRouteRecordRaw[], parentPath = '/') => {
      return routers
        .filter((v) => !v.meta?.hidden)
        .map((v) => {
          const meta = v.meta ?? {}
          const fullPath = isUrl(v.path) ? v.path : pathResolve(parentPath, v.path) // getAllParentPath<AppRouteRecordRaw>(allRouters, v.path).join('/')
          const visibleChildren = (v.children ?? []).filter((child) => !child.meta?.hidden)

          // Collapse each single-child level after permission and visibility filtering.
          if (visibleChildren.length === 1) {
            return renderMenuItem(visibleChildren, fullPath)[0]
          }

          if (visibleChildren.length === 0) {
            return (
              <ElMenuItem index={fullPath}>
                {{
                  default: () => renderMenuTitle(meta)
                }}
              </ElMenuItem>
            )
          } else {
            return (
              <ElSubMenu index={fullPath}>
                {{
                  title: () => (
                    <div class="flex w-full items-center" onClick={handleSubMenuTitleClick(meta, fullPath)}>
                      {renderMenuTitle(meta)}
                    </div>
                  ),
                  default: () => renderMenuItem(visibleChildren, fullPath)
                }}
              </ElSubMenu>
            )
          }
        })
    }

    return {
      renderMenuItem
    }
  }
