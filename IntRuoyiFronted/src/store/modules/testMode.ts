import { computed } from 'vue'
import { defineStore } from 'pinia'
import { getConfigKey } from '@/api/infra/config'

export const TEST_MODE_CONFIG_KEY = 'system.ui.test-mode-enabled'

export const parseTestMode = (value: unknown): boolean => {
  if (value !== 'true' && value !== 'false') {
    throw new Error('测试模式配置缺失或无效，请检查 system.ui.test-mode-enabled 参数。')
  }
  return value === 'true'
}

// 未完成配置读取时不展示测试入口；读取失败由调用方明确提示。
export const useTestModeStore = defineStore('test-mode', {
  state: () => ({ enabled: null as boolean | null, revision: 0 }),
  actions: {
    apply(enabled: boolean | null) {
      this.revision++
      this.enabled = enabled
    },
    async load() {
      const revision = ++this.revision
      this.enabled = null
      const value = await getConfigKey(TEST_MODE_CONFIG_KEY)
      // 基础配置可能在登录初始化读取完成前保存，旧读取不得覆盖刚保存的状态。
      if (revision === this.revision) this.enabled = parseTestMode(value)
    }
  }
})

export const useTestMode = () => {
  const store = useTestModeStore()
  return computed(() => store.enabled === true)
}

const testPageSegments = new Set([
  'codex-test-management',
  'codex-test-record',
  'edhr-batch-record-test',
  'edhr-batch-test',
  'template-simulate'
])

const testConfigRouteNames = new Set([
  'MesEdhrGlobalConfig',
  'ErpTableAutoSyncConfig',
  'DccRegistrationCertificateConfig'
])

export const filterTestModeMenus = (routes: AppRouteRecordRaw[]): AppRouteRecordRaw[] =>
  routes
    .filter((route) =>
      !testConfigRouteNames.has(String(route.name)) &&
      !route.path.split('/').some((segment) => testPageSegments.has(segment))
    )
    .map((route) => ({
      ...route,
      ...(route.children ? { children: filterTestModeMenus(route.children) } : {})
    }))
