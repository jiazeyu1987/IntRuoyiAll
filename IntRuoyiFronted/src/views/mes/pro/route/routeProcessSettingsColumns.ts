import type { UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'

export const ROUTE_PROCESS_SETTINGS_TABLE_KEY = 'mes.pro.route.process.settings'
export const ROUTE_PROCESS_SETTINGS_COLUMN_CONFIG_CHANGED_EVENT =
  'mes-pro-route-process-settings-columns-changed'

export type RouteProcessSettingColumnKey =
  | 'sort'
  | 'processCode'
  | 'processName'
  | 'attention'
  | 'capacitySource'
  | 'standardResource'
  | 'standardShiftCapacity'
  | 'productionQuantityFactor'
  | 'shiftCapacity'
  | 'formSlots'
  | 'batchRecordFormNames'
  | 'resourceStatus'
  | 'predecessor'
  | 'successors'
  | 'relationList'
  | 'keyFlag'
  | 'checkFlag'
  | 'workstation'

export const routeProcessSettingsDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'sort', label: '序号', width: 70 },
  { key: 'processCode', label: '工序编码', width: 120 },
  { key: 'processName', label: '工序名称', width: 120 },
  { key: 'workstation', label: '工作站', width: 140, hideable: false },
  { key: 'capacitySource', label: '资源类型', width: 92 },
  { key: 'standardResource', label: '标准资源', width: 105 },
  { key: 'standardShiftCapacity', label: '标准班次产能', width: 125 },
  { key: 'productionQuantityFactor', label: '生产系数', width: 130 },
  { key: 'shiftCapacity', label: '排产策略', width: 320 },
  { key: 'formSlots', label: '表单槽位', minWidth: 220 },
  { key: 'batchRecordFormNames', label: '批记录表单', minWidth: 180 },
  { key: 'resourceStatus', label: '资源状态', width: 110 },
  { key: 'predecessor', label: '前置工序', minWidth: 140 },
  { key: 'successors', label: '后续工序', minWidth: 180 },
  { key: 'relationList', label: '关系清单', minWidth: 220 },
  { key: 'keyFlag', label: '关键工序', width: 80 },
  { key: 'checkFlag', label: '质检确认', width: 120 }
]

const routeProcessSettingsDetailColumnKeys = new Set<RouteProcessSettingColumnKey>(
  routeProcessSettingsDefaultColumns.map((column) => column.key as RouteProcessSettingColumnKey)
)

export const isRouteProcessSettingsDetailColumnKey = (
  key: string
): key is RouteProcessSettingColumnKey =>
  routeProcessSettingsDetailColumnKeys.has(key as RouteProcessSettingColumnKey)
