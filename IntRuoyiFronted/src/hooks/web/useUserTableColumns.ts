import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUserTableColumnConfig,
  resetUserTableColumnConfig,
  saveUserTableColumnConfig,
  type UserTableColumnConfigColumnVO
} from '@/api/system/userTableColumnConfig'

export interface UserTableColumnDefinition {
  key: string
  label: string
  visible?: boolean
  width?: number
  minWidth?: number
  hideable?: boolean
  business?: boolean
  sortable?: boolean | 'custom'
  sortProp?: string
  sortOrders?: Array<'ascending' | 'descending' | null>
}

export interface UserTableColumnState extends UserTableColumnDefinition {
  visible: boolean
  width?: number
}

const normalizeWidth = (width?: number | string | null) => {
  if (width == null || width === '') return undefined
  const value = Number(width)
  if (!Number.isFinite(value) || value <= 0) return undefined
  return Math.round(value)
}

export const useUserTableColumns = (
  tableKey: string,
  defaultColumns: UserTableColumnDefinition[]
) => {
  const loading = ref(false)
  const saving = ref(false)
  const columns = ref<UserTableColumnState[]>([])

  const buildDefaultColumns = () =>
    defaultColumns.map((column) => ({
      ...column,
      visible: column.visible !== false,
      width: normalizeWidth(column.width)
    }))

  const applyColumns = (savedColumns?: UserTableColumnConfigColumnVO[] | null) => {
    const savedMap = new Map((savedColumns || []).map((column) => [column.key, column]))
    columns.value = buildDefaultColumns().map((column) => {
      const saved = savedMap.get(column.key)
      if (!saved) return column
      return {
        ...column,
        visible: column.hideable === false ? true : saved.visible !== false,
        width: normalizeWidth(saved.width) || column.width
      }
    })
  }

  const visibleColumns = computed(() => columns.value.filter((column) => column.visible))

  const isColumnVisible = (key: string) => {
    const column = columns.value.find((item) => item.key === key)
    return column ? column.visible : true
  }

  const getColumnWidth = (key: string, fallback?: number) => {
    const column = columns.value.find((item) => item.key === key)
    return column?.width || fallback
  }

  const getColumnWidthString = (key: string, fallback?: number) => {
    const width = getColumnWidth(key, fallback)
    return width == null ? undefined : String(width)
  }

  const getColumnMinWidth = (key: string, fallback?: number) => {
    const column = columns.value.find((item) => item.key === key)
    return column?.minWidth || fallback
  }

  const getColumnMinWidthString = (key: string, fallback?: number) => {
    const width = getColumnMinWidth(key, fallback)
    return width == null ? undefined : String(width)
  }

  const loadConfig = async () => {
    loading.value = true
    try {
      const config = await getUserTableColumnConfig(tableKey)
      applyColumns(config?.columns)
    } catch (error) {
      applyColumns()
      ElMessage.error('列表列配置加载失败，请刷新后重试。')
      throw error
    } finally {
      loading.value = false
    }
  }

  const saveConfig = async (
    columnsOrOptions: UserTableColumnState[] | { silentSuccess?: boolean } = {}
  ) => {
    const options = Array.isArray(columnsOrOptions) ? { silentSuccess: true } : columnsOrOptions
    saving.value = true
    try {
      await saveUserTableColumnConfig({
        tableKey,
        columns: columns.value.map((column) => ({
          key: column.key,
          visible: column.visible,
          width: column.width
        }))
      })
      if (!options.silentSuccess) {
        ElMessage.success('列表列配置已保存')
      }
    } catch (error) {
      ElMessage.error('列表列配置保存失败，请检查后端接口。')
      throw error
    } finally {
      saving.value = false
    }
  }

  const autoSaveConfig = async () => {
    await saveConfig({ silentSuccess: true })
  }

  // Element Plus el-table header-dragend handler. Persist width immediately after drag ends.
  const handleHeaderDragend = async (newWidth: number, _oldWidth: number, column: any) => {
    const columnKey = String(column?.property || column?.rawColumnKey || column?.label || '')
    const target = columns.value.find((item) => item.key === columnKey)
    if (!target) return
    target.width = normalizeWidth(newWidth) || target.width
    await autoSaveConfig()
  }

  const resetConfig = async () => {
    await ElMessageBox.confirm('确认重置当前列表的显示字段和列宽配置？', '重置列表列配置', {
      confirmButtonText: '重置',
      cancelButtonText: '取消',
      type: 'warning'
    })
    saving.value = true
    try {
      await resetUserTableColumnConfig(tableKey)
      applyColumns()
      ElMessage.success('列表列配置已重置')
    } catch (error) {
      ElMessage.error('列表列配置重置失败，请检查后端接口。')
      throw error
    } finally {
      saving.value = false
    }
  }

  applyColumns()
  onMounted(loadConfig)

  return {
    loading,
    saving,
    columns,
    visibleColumns,
    isColumnVisible,
    getColumnWidth,
    getColumnWidthString,
    getColumnMinWidth,
    getColumnMinWidthString,
    handleHeaderDragend,
    loadConfig,
    saveConfig,
    autoSaveConfig,
    resetConfig
  }
}
