import { onBeforeUnmount, onMounted, watch, type Ref } from 'vue'
import '@/styles/edhr-list-viewport.css'

/** Size standard list tables across the app, and ordinary tables under the eDHR menu. */
export function useEdhrListViewport(root: Ref<HTMLElement | undefined>, enabled: Ref<boolean>) {
  let frame = 0
  let mutations: MutationObserver | undefined
  let resize: ResizeObserver | undefined
  const tables = new Set<HTMLElement>()
  const excluded = '.el-dialog, .el-drawer, .el-table__expanded-cell, .active-order-detail-layout, [data-list-viewport="embedded"]'
  const visible = (element: HTMLElement) => element.getClientRects().length > 0
  const pixels = (value: string) => Number.parseFloat(value) || 0

  const clear = (table: HTMLElement) => {
    table.classList.remove('edhr-viewport-table')
    table.style.removeProperty('--edhr-list-height')
  }

  const update = () => {
    frame = 0
    const container = root.value
    if (!container) return
    for (const table of tables) {
      if (!container.contains(table) || (!enabled.value && !table.closest('.unified-list-template__table-shell'))) {
        clear(table)
        tables.delete(table)
      }
    }

    const selector = enabled.value ? '.el-table' : '.unified-list-template__table-shell .el-table'
    for (const table of Array.from(container.querySelectorAll<HTMLElement>(selector))) {
      if (!visible(table)) continue
      if (table.closest(excluded) || table.parentElement?.closest('.el-table')) {
        clear(table)
        tables.delete(table)
        continue
      }
      // Outside eDHR, tables with an explicit height already own their scrolling layout.
      if (!enabled.value && table.style.height && table.style.height !== 'auto') {
        clear(table)
        tables.delete(table)
        continue
      }
      // These lists own their height; the form center also has a side-by-side preview.
      if (table.closest('.erp-viewport-list, .erp-runtime-panel, .erp-configuration-screen, .edhr-record-viewport, .batch-record-form-layout, .unified-list-template--contained')) {
        if (table.classList.contains('edhr-viewport-table')) clear(table)
        tables.delete(table)
        continue
      }

      let reserved = 0
      let scrollTop = 0
      let node: HTMLElement = table
      while (node !== container && node.parentElement) {
        const parent = node.parentElement
        const parentStyle = getComputedStyle(parent)
        reserved += pixels(parentStyle.paddingBottom) + pixels(parentStyle.borderBottomWidth)
        reserved += pixels(getComputedStyle(node).marginBottom)
        for (let sibling = node.nextElementSibling; sibling; sibling = sibling.nextElementSibling) {
          if (!(sibling instanceof HTMLElement) || !visible(sibling)) continue
          const style = getComputedStyle(sibling)
          if (style.position === 'absolute' || style.position === 'fixed') continue
          // A neighbouring grid/flex column does not consume this column's vertical space.
          const nodeRect = node.getBoundingClientRect()
          const siblingRect = sibling.getBoundingClientRect()
          if (siblingRect.left >= nodeRect.right || siblingRect.right <= nodeRect.left) continue
          reserved += siblingRect.height + pixels(style.marginTop) + pixels(style.marginBottom)
        }
        node = parent
      }
      // Scrolling the application must not enlarge its tables on the next data refresh.
      for (let parent = table.parentElement; parent; parent = parent.parentElement) {
        scrollTop += parent.scrollTop
      }
      const height = Math.max(100, Math.floor(window.innerHeight - table.getBoundingClientRect().top - scrollTop - reserved))
      const value = `${height}px`
      if (table.style.getPropertyValue('--edhr-list-height') !== value) {
        table.style.setProperty('--edhr-list-height', value)
      }
      table.classList.add('edhr-viewport-table')
      tables.add(table)
    }
  }

  const schedule = () => {
    if (!frame) frame = requestAnimationFrame(update)
  }

  onMounted(() => {
    mutations = new MutationObserver((records) => {
      if (records.some(record => !tables.has(record.target as HTMLElement))) schedule()
    })
    if (root.value) {
      mutations.observe(root.value, { childList: true, subtree: true, attributes: true, attributeFilter: ['class', 'style'] })
      resize = new ResizeObserver(schedule)
      resize.observe(root.value)
    }
    window.addEventListener('resize', schedule)
    schedule()
  })
  watch(enabled, schedule, { flush: 'post' })
  onBeforeUnmount(() => {
    mutations?.disconnect()
    resize?.disconnect()
    window.removeEventListener('resize', schedule)
    cancelAnimationFrame(frame)
    tables.forEach(clear)
  })
}
