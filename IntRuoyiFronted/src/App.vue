<script lang="ts" setup>
import { getGxpAuditRuntimeMode, type GxpAuditRuntimeMode } from '@/api/system/gxpAuditMode'
import { useAppStore } from '@/store/modules/app'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { useTestModeStore } from '@/store/modules/testMode'
import { useDesign } from '@/hooks/web/useDesign'
import routerSearch from '@/components/RouterSearch/index.vue'
import UserTableColumnGlobalEnhancer from '@/components/UserTableColumnGlobalEnhancer/index.vue'

defineOptions({ name: 'APP' })

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('app')
const appStore = useAppStore()
const userStore = useUserStore()
const testModeStore = useTestModeStore()
const gxpAuditMode = ref<GxpAuditRuntimeMode | null>(null)
watch(
  () => userStore.user.id,
  async (userId) => {
    gxpAuditMode.value = null
    if (!userId) return
    try {
      gxpAuditMode.value = await getGxpAuditRuntimeMode()
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '审计运行模式读取失败。')
    }
  },
  { immediate: true }
)
watch(
  () => userStore.user.id,
  async (userId) => {
    testModeStore.apply(null)
    if (!userId) return
    try {
      await testModeStore.load()
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '测试模式配置读取失败。')
    }
  },
  { immediate: true }
)
const currentSize = computed(() => appStore.getCurrentSize)
const greyMode = computed(() => appStore.getGreyMode)

appStore.setLightTheme()
</script>
<template>
  <ConfigGlobal :size="currentSize">
    <RouterView :class="greyMode ? `${prefixCls}-grey-mode` : ''" />
    <div
      v-if="gxpAuditMode?.mode === 'DEVELOPMENT_DISABLED'"
      class="gxp-development-banner"
      data-gxp-audit-mode="DEVELOPMENT_DISABLED"
      role="status"
    >
      开发模式：GXP 审计及其依赖校验已关闭，结果不作为完整审计验收。
      <div v-if="gxpAuditMode.serverStartedAtUtc">
        本次服务启动（UTC）：<time data-backend-started-at>{{ gxpAuditMode.serverStartedAtUtc }}</time>
      </div>
    </div>
    <UserTableColumnGlobalEnhancer />
    <routerSearch />
  </ConfigGlobal>
</template>
<style lang="scss">
$prefix-cls: #{$namespace}-app;

.gxp-development-banner {
  position: fixed;
  right: 16px;
  bottom: 8px;
  z-index: 10000;
  max-width: 420px;
  padding: 6px 12px;
  color: #7c4a03;
  background: #fff3cd;
  border: 1px solid #e6a23c;
  border-radius: 4px;
  pointer-events: none;
}

.size {
  width: 100%;
  height: 100%;
}

html,
body {
  @extend .size;

  padding: 0 !important;
  margin: 0;
  overflow: hidden;

  #app {
    @extend .size;
  }
}

.#{$prefix-cls}-grey-mode {
  filter: grayscale(100%);
}
</style>
