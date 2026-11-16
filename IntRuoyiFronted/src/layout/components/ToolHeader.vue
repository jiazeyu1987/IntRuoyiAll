<script lang="tsx">
import { defineComponent, computed, ref, watch } from 'vue'
import { Message } from '@/layout/components//Message'
import { Collapse } from '@/layout/components/Collapse'
import { UserInfo } from '@/layout/components/UserInfo'
import { Breadcrumb } from '@/layout/components/Breadcrumb'
import RouterSearch from '@/components/RouterSearch/index.vue'
import { useAppStore } from '@/store/modules/app'
import { useDesign } from '@/hooks/web/useDesign'
import { getTenantList } from '@/api/system/tenant'
import { authContextRevision, getTenantId, getVisitTenantId } from '@/utils/auth'

const { getPrefixCls, variables } = useDesign()

const prefixCls = getPrefixCls('tool-header')

const appStore = useAppStore()

const breadcrumb = computed(() => appStore.getBreadcrumb)
const hamburger = computed(() => appStore.getHamburger)
const search = computed(() => appStore.search)
const layout = computed(() => appStore.getLayout)
const message = computed(() => appStore.getMessage)

export default defineComponent({
  name: 'ToolHeader',
  setup() {
    const tenantEnabled = import.meta.env.VITE_APP_TENANT_ENABLE === 'true'
    const tenantName = ref('')
    const tenantError = ref('')
    const currentTenantId = computed(() => {
      void authContextRevision.value
      return getVisitTenantId() || getTenantId()
    })

    if (tenantEnabled) {
      watch(
        currentTenantId,
        async (tenantId, _, onCleanup) => {
          let active = true
          onCleanup(() => (active = false))
          tenantName.value = ''
          tenantError.value = ''
          if (!tenantId) {
            tenantError.value = '未获取到当前租户'
            return
          }
          try {
            const tenants: { id: number; name: string }[] = await getTenantList()
            if (!active) return
            const tenant = tenants.find((item) => String(item.id) === String(tenantId))
            if (!tenant?.name) {
              tenantError.value = '当前租户名称不存在'
              return
            }
            tenantName.value = tenant.name
          } catch (error) {
            if (!active) return
            tenantError.value = '租户名称加载失败'
            console.error('加载顶部栏当前租户名称失败', error)
          }
        },
        { immediate: true }
      )
    }

    return () => (
      <div
        id={`${variables.namespace}-tool-header`}
        class={[
          prefixCls,
          'h-[var(--top-tool-height)] relative px-[var(--top-tool-p-x)] flex items-center justify-between',
          'dark:bg-[var(--el-bg-color)]'
        ]}
      >
        {layout.value !== 'top' ? (
          <div class="h-full min-w-0 flex items-center">
            {hamburger.value && layout.value !== 'cutMenu' ? (
              <Collapse class="custom-hover" color="var(--top-header-text-color)"></Collapse>
            ) : undefined}
            {breadcrumb.value ? <Breadcrumb class="lt-md:hidden"></Breadcrumb> : undefined}
          </div>
        ) : undefined}
        {tenantEnabled ? (
          <div
            class={`${prefixCls}__tenant`}
            title={tenantError.value || tenantName.value}
            aria-label="当前租户"
            aria-live="polite"
          >
            {tenantError.value || tenantName.value}
          </div>
        ) : undefined}
        <div class="h-full flex items-center ml-auto">
          {search.value ? (
            <RouterSearch
              isModal={false}
              alwaysVisible={true}
              color="var(--top-header-text-color)"
            />
          ) : undefined}
          {message.value ? (
            <Message class="custom-hover" color="var(--top-header-text-color)"></Message>
          ) : undefined}
          <UserInfo></UserInfo>
        </div>
      </div>
    )
  }
})
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-tool-header;

.#{$prefix-cls} {
  transition: left var(--transition-time-02);

  &__tenant {
    position: absolute;
    left: 50%;
    max-width: 300px;
    overflow: hidden;
    font-size: 16px;
    font-weight: 600;
    color: var(--top-header-text-color);
    text-overflow: ellipsis;
    white-space: nowrap;
    transform: translateX(-50%);
  }

  @media (width < 1200px) {
    &__tenant {
      position: static;
      min-width: 0;
      margin: 0 12px;
      text-align: center;
      transform: none;
      flex: 1;
    }
  }
}
</style>
