<template>
  <Teleport to="body">
    <div
      v-for="panel in panels"
      :key="panel.tableKey"
      class="user-table-column-global-settings"
      :class="{ 'is-open': panel.open }"
      :style="panel.style"
      data-user-table-column-global-enhancer
    >
      <button
        class="user-table-column-global-settings__button"
        type="button"
        title="显示字段"
        @click="togglePanel(panel.tableKey)"
      >
        <Icon icon="ep:setting" class="mr-5px" />
        显示字段
      </button>
      <div v-if="panel.open" class="user-table-column-global-settings__popover">
        <div class="user-table-column-global-settings__title">显示字段</div>
        <div class="user-table-column-global-settings__hint">至少保留 1 个业务字段</div>
        <label
          v-for="column in panel.columns"
          :key="column.key"
          class="user-table-column-global-settings__check"
        >
          <input
            type="checkbox"
            :checked="column.visible"
            :disabled="!column.hideable"
            @change="toggleColumn(panel.tableKey, column.key, ($event.target as HTMLInputElement).checked)"
          />
          <span>{{ column.label }}</span>
        </label>
      </div>
      <button
        class="user-table-column-global-settings__button"
        type="button"
        :disabled="panel.saving"
        @click="resetPanel(panel.tableKey)"
      >
        重置
      </button>
    </div>
  </Teleport>
</template>

<script lang="ts" setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUserTableColumnConfig,
  resetUserTableColumnConfig,
  saveUserTableColumnConfig,
  type UserTableColumnConfigColumnVO
} from '@/api/system/userTableColumnConfig'

defineOptions({ name: 'UserTableColumnGlobalEnhancer' })

type EnhancedColumn = {
  key: string
  label: string
  visible: boolean
  width?: number
  hideable: boolean
  business: boolean
}

type EnhancedTable = {
  tableKey: string
  tableEl: HTMLElement
  wrapperEl: HTMLElement
  columns: EnhancedColumn[]
  open: boolean
  saving: boolean
  loaded: boolean
  style: Record<string, string>
  dragWidthSnapshot?: Map<string, number>
  observer?: MutationObserver
}

const TARGET_ROUTE_PREFIXES = [
  '/mes/pro/workorder',
  '/mes/pro/work-order',
  '/mes/pro/scheduleorder',
  '/mes/pro/schedule-order',
  '/mes/pro/scheduler-workbench',
  '/mes/pro/task',
  '/mes/pro/edhr',
  '/mes/pro/feedback/edhr',
  '/mes/pro/edhr-batch',
  '/mes/pro/edhr-work-task',
  '/mes/pro/edhr-release',
  '/mes/pro/edhr-traveler',
  '/mes/pro/edhr-validation',
  '/dcc/controlled-file',
  '/approval-center'
]

const EXPLICIT_TABLE_KEYS = new Set([
  'mes.pro.workorder.main',
  'mes.pro.scheduleOrder.main',
  'mes.pro.edhrBatch.execution.main',
  'dcc.controlledFile.browser.main',
  'dcc.controlledFile.browser.adminStyle',
  'dcc.controlledFile.detail.signatureEvidence'
])

const STRUCTURAL_LABELS = new Set(['操作', '启用', '选择', '展开'])

const route = useRoute()
const tableMap = reactive(new Map<string, EnhancedTable>())
let scanTimer: number | undefined
let bodyObserver: MutationObserver | undefined
let scrollListenerAttached = false
let resizeTableKey: string | undefined

const panels = computed(() =>
  Array.from(tableMap.values()).filter((table) => table.tableEl.isConnected && table.columns.length > 0)
)

const isTargetRoute = () => {
  const path = route.path || window.location.pathname
  return TARGET_ROUTE_PREFIXES.some((prefix) => path === prefix || path.startsWith(`${prefix}/`))
}

const normalizeText = (value?: string | null) => String(value || '').replace(/\s+/g, ' ').trim()

const normalizeKeyPart = (value: string) =>
  value
    .trim()
    .replace(/[^\p{L}\p{N}]+/gu, '-')
    .replace(/^-+|-+$/g, '')
    .toLowerCase()

const routeTableKeyPrefix = () => {
  const path = normalizeText(route.path || window.location.pathname)
    .replace(/^\/+|\/+$/g, '')
    .replace(/\//g, '.')
  return path ? `auto.${path}` : 'auto.current'
}

const isExplicitlyConfigured = (tableEl: HTMLElement) =>
  tableEl.hasAttribute('data-user-table-column-explicit') ||
  Boolean(tableEl.closest('[data-user-table-column-explicit-scope]'))

const resolveTableIdentity = (tableEl: HTMLElement, index: number) => {
  const dataKey = tableEl.getAttribute('data-user-table-key')
  if (dataKey && EXPLICIT_TABLE_KEYS.has(dataKey)) {
    return undefined
  }
  if (dataKey) {
    return dataKey
  }
  const testId = tableEl.getAttribute('data-testid')
  if (testId) {
    return `${routeTableKeyPrefix()}.${normalizeKeyPart(testId)}`
  }
  const title =
    tableEl.closest('.el-dialog, .el-drawer, .el-card')?.querySelector('.el-dialog__title, .el-drawer__title, .el-card__header')?.textContent ||
    tableEl.closest('[class*="drawer"], [class*="dialog"], [class*="wrap"]')?.querySelector('h1,h2,h3,.section-title,.preview-title')?.textContent
  const normalizedTitle = normalizeKeyPart(normalizeText(title))
  return `${routeTableKeyPrefix()}.${normalizedTitle || 'table'}-${index + 1}`
}

const resolveColumnKey = (headerCell: HTMLElement, label: string, index: number) => {
  const columnId = headerCell.className.match(/el-table_[^\s]+_column_([^\s]+)/)?.[1]
  const normalizedLabel = normalizeKeyPart(label)
  return normalizedLabel || columnId || `column-${index + 1}`
}

const normalizeWidth = (width?: number | string | null) => {
  if (width == null || width === '') return undefined
  const value = Number(width)
  if (!Number.isFinite(value) || value <= 0) return undefined
  return Math.round(value)
}

const readColumns = (tableEl: HTMLElement): EnhancedColumn[] => {
  const headerCells = Array.from(tableEl.querySelectorAll<HTMLElement>('.el-table__header-wrapper th'))
    .filter((cell) => !cell.classList.contains('gutter') && !cell.classList.contains('is-hidden'))
  const seen = new Set<string>()
  return headerCells
    .map((cell, index) => {
      const label = normalizeText(cell.querySelector('.cell')?.textContent || cell.textContent)
      const key = resolveColumnKey(cell, label, index)
      const width = normalizeWidth(cell.getBoundingClientRect().width || cell.style.width)
      const hideable = Boolean(label) && !STRUCTURAL_LABELS.has(label) && !cell.classList.contains('el-table-column--selection')
      const business = hideable
      return { key, label: label || `列 ${index + 1}`, visible: true, width, hideable, business }
    })
    .filter((column) => {
      if (seen.has(column.key)) return false
      seen.add(column.key)
      return true
    })
}

const readColumnWidthSnapshot = (tableEl: HTMLElement) => {
  const snapshot = new Map<string, number>()
  readColumns(tableEl).forEach((column) => {
    const width = normalizeWidth(column.width)
    if (width) {
      snapshot.set(column.key, width)
    }
  })
  return snapshot
}

const findColumnCells = (tableEl: HTMLElement, columnKey: string, columnIndex: number) => {
  const headerCells = Array.from(tableEl.querySelectorAll<HTMLElement>('.el-table__header-wrapper th'))
    .filter((cell) => !cell.classList.contains('gutter') && !cell.classList.contains('is-hidden'))
  const targetHeader = headerCells[columnIndex]
  const className = targetHeader ? Array.from(targetHeader.classList).find((item) => item.includes('column_')) : undefined
  const selector = className ? `.${className}` : undefined
  const cells = selector
    ? Array.from(tableEl.querySelectorAll<HTMLElement>(selector))
    : [targetHeader].filter(Boolean) as HTMLElement[]
  return cells.filter((cell) => {
    const label = normalizeText(cell.querySelector('.cell')?.textContent || cell.textContent)
    return cell === targetHeader || resolveColumnKey(cell, label, columnIndex) === columnKey
  })
}

const applyTableState = (table: EnhancedTable) => {
  table.columns.forEach((column, index) => {
    const width = normalizeWidth(column.width)
    for (const cell of findColumnCells(table.tableEl, column.key, index)) {
      cell.style.display = column.visible ? '' : 'none'
      if (column.visible && width) {
        cell.style.width = `${width}px`
        cell.style.minWidth = `${width}px`
      }
    }
  })
  updatePanelPosition(table)
}

const mergeSavedColumns = (table: EnhancedTable, savedColumns?: UserTableColumnConfigColumnVO[] | null) => {
  const savedMap = new Map((savedColumns || []).map((column) => [column.key, column]))
  table.columns = table.columns.map((column) => {
    const saved = savedMap.get(column.key)
    if (!saved) return column
    return {
      ...column,
      visible: column.hideable ? saved.visible !== false : true,
      width: normalizeWidth(saved.width) || column.width
    }
  })
}

const loadPanel = async (table: EnhancedTable) => {
  try {
    const config = await getUserTableColumnConfig(table.tableKey)
    mergeSavedColumns(table, config?.columns)
    table.loaded = true
    applyTableState(table)
  } catch (error) {
    ElMessage.error('列表列配置加载失败，请刷新后重试。')
    throw error
  }
}

const updatePanelPosition = (table: EnhancedTable) => {
  const rect = table.tableEl.getBoundingClientRect()
  if (!rect.width || !rect.height) return
  table.style = {
    top: `${Math.max(rect.top + window.scrollY + 8, 8)}px`,
    left: `${Math.max(rect.right + window.scrollX - 284, 8)}px`
  }
}

const updateAllPanelPositions = () => {
  for (const table of tableMap.values()) {
    updatePanelPosition(table)
  }
}

const attachTableObserver = (table: EnhancedTable) => {
  table.observer?.disconnect()
  table.observer = new MutationObserver(() => {
    const nextColumns = readColumns(table.tableEl)
    if (nextColumns.length === 0) return
    const currentMap = new Map(table.columns.map((column) => [column.key, column]))
    table.columns = nextColumns.map((column) => {
      const current = currentMap.get(column.key)
      return current ? { ...column, visible: current.visible, width: column.width || current.width } : column
    })
    applyTableState(table)
  })
  table.observer.observe(table.tableEl, { childList: true, subtree: true, attributes: true, attributeFilter: ['style', 'class'] })
}

const isHeaderResizeGesture = (target: EventTarget | null) => {
  if (!(target instanceof HTMLElement)) return false
  return Boolean(
    target.closest('.el-table__column-resize-proxy') ||
      target.closest('.el-table__header-wrapper th .cell') ||
      target.closest('.el-table__header-wrapper th')
  )
}

const handleManagedHeaderPointerDown = (event: MouseEvent | PointerEvent) => {
  if (!isHeaderResizeGesture(event.target)) return
  const wrapperEl = (event.target as HTMLElement).closest<HTMLElement>('.el-table')
  if (!wrapperEl) return
  const tableKey = wrapperEl.dataset.userTableColumnGlobalKey
  if (!tableKey) return
  const table = tableMap.get(tableKey)
  if (!table || isExplicitlyConfigured(table.tableEl)) return
  table.dragWidthSnapshot = readColumnWidthSnapshot(table.tableEl)
  resizeTableKey = table.tableKey
}

const finalizeManagedColumnResize = async () => {
  const tableKey = resizeTableKey
  resizeTableKey = undefined
  if (!tableKey) return
  const table = tableMap.get(tableKey)
  const previousSnapshot = table?.dragWidthSnapshot
  if (!table || !previousSnapshot) return
  table.dragWidthSnapshot = undefined

  const currentColumns = readColumns(table.tableEl)
  if (currentColumns.length === 0) return
  const currentMap = new Map(currentColumns.map((column) => [column.key, column]))
  let changed = false
  table.columns = table.columns.map((column) => {
    const current = currentMap.get(column.key)
    const nextWidth = normalizeWidth(current?.width)
    const previousWidth = previousSnapshot.get(column.key)
    if (nextWidth && previousWidth && Math.abs(nextWidth - previousWidth) > 1) {
      changed = true
      return { ...column, width: nextWidth }
    }
    return column
  })
  if (!changed) return
  applyTableState(table)
  await savePanel(table.tableKey, { silentSuccess: true })
}

const registerTable = (tableEl: HTMLElement, index: number) => {
  if (isExplicitlyConfigured(tableEl)) return
  if (tableEl.closest('[data-user-table-column-global-enhancer]')) return
  const wrapperEl = tableEl.closest<HTMLElement>('.el-table') || tableEl
  const columns = readColumns(wrapperEl)
  if (columns.length === 0) return
  const tableKey = resolveTableIdentity(wrapperEl, index)
  if (!tableKey || tableMap.has(tableKey)) return
  const table: EnhancedTable = {
    tableKey,
    tableEl: wrapperEl,
    wrapperEl,
    columns,
    open: false,
    saving: false,
    loaded: false,
    style: {},
    dragWidthSnapshot: undefined
  }
  tableMap.set(tableKey, table)
  wrapperEl.dataset.userTableColumnGlobalKey = tableKey
  wrapperEl.classList.add('user-table-column-global-managed')
  wrapperEl.addEventListener('mousedown', handleManagedHeaderPointerDown, true)
  wrapperEl.addEventListener('pointerdown', handleManagedHeaderPointerDown, true)
  attachTableObserver(table)
  void loadPanel(table)
  updatePanelPosition(table)
}

const pruneDisconnectedTables = () => {
  for (const [key, table] of tableMap) {
    if (!table.tableEl.isConnected) {
      table.wrapperEl.removeEventListener('mousedown', handleManagedHeaderPointerDown, true)
      table.wrapperEl.removeEventListener('pointerdown', handleManagedHeaderPointerDown, true)
      table.observer?.disconnect()
      tableMap.delete(key)
    }
  }
}

const scanTables = () => {
  if (!isTargetRoute()) {
    for (const table of tableMap.values()) {
      table.wrapperEl.removeEventListener('mousedown', handleManagedHeaderPointerDown, true)
      table.wrapperEl.removeEventListener('pointerdown', handleManagedHeaderPointerDown, true)
      table.observer?.disconnect()
    }
    tableMap.clear()
    return
  }
  pruneDisconnectedTables()
  const tables = Array.from(document.querySelectorAll<HTMLElement>('.el-table'))
  tables.forEach((tableEl, index) => registerTable(tableEl, index))
  updateAllPanelPositions()
}

const scheduleScan = () => {
  if (scanTimer) {
    window.clearTimeout(scanTimer)
  }
  scanTimer = window.setTimeout(() => {
    scanTimer = undefined
    scanTables()
  }, 120)
}

const togglePanel = (tableKey: string) => {
  const table = tableMap.get(tableKey)
  if (!table) return
  table.open = !table.open
  updatePanelPosition(table)
}

const toggleColumn = async (tableKey: string, columnKey: string, visible: boolean) => {
  const table = tableMap.get(tableKey)
  if (!table) return
  const column = table.columns.find((item) => item.key === columnKey)
  if (!column || !column.hideable) return
  const visibleBusinessCount = table.columns.filter(
    (item) => item.business && item.visible && item.key !== columnKey
  ).length
  if (!visible && column.business && visibleBusinessCount < 1) {
    ElMessage.warning('至少保留 1 个业务字段')
    return
  }
  column.visible = visible
  applyTableState(table)
  await savePanel(tableKey, { silentSuccess: true })
}

const savePanel = async (tableKey: string, options: { silentSuccess?: boolean } = {}) => {
  const table = tableMap.get(tableKey)
  if (!table) return
  table.saving = true
  try {
    await saveUserTableColumnConfig({
      tableKey,
      columns: table.columns.map((column) => ({
        key: column.key,
        visible: column.visible,
        width: normalizeWidth(column.width)
      }))
    })
    if (!options.silentSuccess) {
      ElMessage.success('列表列配置已保存')
    }
  } catch (error) {
    ElMessage.error('列表列配置保存失败，请检查后端接口。')
    throw error
  } finally {
    table.saving = false
  }
}

const resetPanel = async (tableKey: string) => {
  const table = tableMap.get(tableKey)
  if (!table) return
  await ElMessageBox.confirm('确认重置当前列表的显示字段和列宽配置？', '重置列表列配置', {
    confirmButtonText: '重置',
    cancelButtonText: '取消',
    type: 'warning'
  })
  table.saving = true
  try {
    await resetUserTableColumnConfig(tableKey)
    table.columns = readColumns(table.tableEl).map((column) => ({ ...column, visible: true }))
    table.open = false
    applyTableState(table)
    ElMessage.success('列表列配置已重置')
  } catch (error) {
    ElMessage.error('列表列配置重置失败，请检查后端接口。')
    throw error
  } finally {
    table.saving = false
  }
}

watch(
  () => route.fullPath,
  async () => {
    await nextTick()
    scheduleScan()
  }
)

onMounted(() => {
  bodyObserver = new MutationObserver(scheduleScan)
  bodyObserver.observe(document.body, { childList: true, subtree: true })
  window.addEventListener('resize', updateAllPanelPositions)
  window.addEventListener('scroll', updateAllPanelPositions, true)
  window.addEventListener('mouseup', finalizeManagedColumnResize)
  window.addEventListener('pointerup', finalizeManagedColumnResize)
  scrollListenerAttached = true
  scheduleScan()
})

onBeforeUnmount(() => {
  if (scanTimer) {
    window.clearTimeout(scanTimer)
  }
  bodyObserver?.disconnect()
  for (const table of tableMap.values()) {
    table.wrapperEl.removeEventListener('mousedown', handleManagedHeaderPointerDown, true)
    table.wrapperEl.removeEventListener('pointerdown', handleManagedHeaderPointerDown, true)
    table.observer?.disconnect()
  }
  tableMap.clear()
  window.removeEventListener('resize', updateAllPanelPositions)
  if (scrollListenerAttached) {
    window.removeEventListener('scroll', updateAllPanelPositions, true)
  }
  window.removeEventListener('mouseup', finalizeManagedColumnResize)
  window.removeEventListener('pointerup', finalizeManagedColumnResize)
})
</script>
