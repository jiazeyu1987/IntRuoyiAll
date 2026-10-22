import type { App } from 'vue'
// 需要全局引入一些组件，如ElScrollbar，不然一些下拉项样式有问题
import {
  ElLoading,
  ElScrollbar,
  ElButton,
  ElMessageBox,
  ElMessage,
  ElNotification,
  ZINDEX_INJECTION_KEY
} from 'element-plus'

const plugins = [ElLoading, ElMessageBox, ElMessage, ElNotification]

const components = [ElScrollbar, ElButton]

export const setupElementPlus = (app: App<Element>) => {
  // Detached service instances inherit the same app context and layer counter as Dialog.
  app.provide(ZINDEX_INJECTION_KEY, { current: 0 })
  plugins.forEach((plugin) => {
    app.use(plugin)
  })

  components.forEach((component) => {
    app.component(component.name, component)
  })
}
