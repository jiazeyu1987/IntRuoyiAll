<template>
  <section class="signature-governance-list-pane">
    <UnifiedListTemplate
      table-key="signature.governance.policy.list"
      :query-model="queryParams"
      :filter-definitions="filterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="quickFilter.state"
      :selected-filter-definition="quickFilter.selectedDefinition.value"
      :operator-options="quickFilter.operatorOptions.value"
      :columns="columns"
      :column-saving="columnSaving"
      :total="filteredRows.length"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="quickFilter.updateState"
      @quick-filter-query="quickFilter.applyQuickFilter"
      @column-change="saveColumnConfig"
      @column-reset="resetColumnConfig"
      @pagination="handlePagination"
    >
      <template #actions>
        <el-button
          v-hasPermi="[SIGNATURE_GOVERNANCE_PERMISSIONS.POLICY_QUERY]"
          type="primary"
          :loading="policyLoading"
          @click="loadCurrentPolicy"
        >
          刷新
        </el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="policyLoading"
          data-user-table-column-explicit
          data-user-table-key="signature.governance.policy.list"
          :data="pagedRows"
          :empty-text="policyError || '暂无统一策略结果'"
          :show-overflow-tooltip="true"
          @header-dragend="handleHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column v-if="isColumnVisible('item')" label="事项" prop="item" :width="getColumnWidthString('item', 130)" v-bind="sortColumnAttrs('item')" />
          <el-table-column v-if="isColumnVisible('source')" label="来源" prop="source" :min-width="getColumnMinWidthString('source', 190)" v-bind="sortColumnAttrs('source')" />
          <el-table-column v-if="isColumnVisible('status')" label="状态" prop="status" :width="getColumnWidthString('status', 130)" v-bind="sortColumnAttrs('status')">
            <template #default="{ row }">
              <el-tag :type="row.statusType" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isColumnVisible('keyFields')" label="关键字段" prop="keyFields" :min-width="getColumnMinWidthString('keyFields', 300)" v-bind="sortColumnAttrs('keyFields')" />
          <el-table-column v-if="isColumnVisible('evidence')" label="证据" prop="evidence" :min-width="getColumnMinWidthString('evidence', 240)" v-bind="sortColumnAttrs('evidence')" />
          <el-table-column v-if="isColumnVisible('blockerImpact')" label="阻断影响" prop="blockerImpact" :min-width="getColumnMinWidthString('blockerImpact', 260)" v-bind="sortColumnAttrs('blockerImpact')" />
          <el-table-column v-if="isColumnVisible('operation')" label="操作" fixed="right" :width="getColumnWidthString('operation', 100)">
            <template #default>—</template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </section>
</template>

<script lang="ts" setup>
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
import {
  getCurrentSignatureGovernancePolicy,
  type SignatureGovernancePolicyCurrentRespVO,
  type SignatureGovernancePolicyModuleRespVO
} from '@/api/signature-governance/policy'
import {
  SIGNATURE_GOVERNANCE_PERMISSIONS,
  type SignatureGovernanceBlocker
} from '@/api/signature-governance/shared'

defineOptions({ name: 'PolicyGovernanceListPane' })

type GovernanceRow = {
  id: string
  item: string
  source: string
  status: string
  statusType: string
  keyFields: string
  evidence: string
  blockerImpact: string
}

const defaultColumns: UserTableColumnDefinition[] = [
  { key: 'item', label: '事项', width: 130 },
  { key: 'source', label: '来源', minWidth: 190 },
  { key: 'status', label: '状态', width: 130 },
  { key: 'keyFields', label: '关键字段', minWidth: 300 },
  { key: 'evidence', label: '证据', minWidth: 240 },
  { key: 'blockerImpact', label: '阻断影响', minWidth: 260 },
  { key: 'operation', label: '操作', width: 100 }
]

const {
  columns,
  saving: columnSaving,
  isColumnVisible,
  getColumnWidthString,
  getColumnMinWidthString,
  handleHeaderDragend,
  saveConfig: saveColumnConfig,
  resetConfig: resetColumnConfig
} = useUserTableColumns('signature.governance.policy.list', defaultColumns)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  quickFilter: undefined as TableQuickFilterValue | undefined
})

const filterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'item', label: '事项', type: 'text', placeholder: '请输入事项' },
  { key: 'source', label: '来源', type: 'text', placeholder: '请输入来源' },
  { key: 'status', label: '状态', type: 'text', placeholder: '请输入状态' },
  { key: 'keyFields', label: '关键字段', type: 'text', placeholder: '请输入关键字段' },
  { key: 'evidence', label: '证据', type: 'text', placeholder: '请输入证据' },
  { key: 'blockerImpact', label: '阻断影响', type: 'text', placeholder: '请输入阻断影响' }
]

const policyLoading = ref(false)
const policyError = ref('')
const policyResult = ref<SignatureGovernancePolicyCurrentRespVO>()

const displayValue = (value: unknown) => {
  if (value === undefined || value === null) return '等待来源'
  const text = String(value).trim()
  return text || '等待来源'
}

const boolText = (value: boolean | undefined) => value ? '是' : '否'

const statusTagType = (status?: string) => {
  if (status === 'READY') return 'success'
  if (status === 'BLOCKED') return 'danger'
  if (!status || status === '等待来源') return 'warning'
  return 'info'
}

const moduleStatusText = (row: SignatureGovernancePolicyModuleRespVO) =>
  row.policySourcePresent && row.authorityConfirmed && row.adapterRegistered ? 'READY' : 'BLOCKED'

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const loadCurrentPolicy = async () => {
  policyLoading.value = true
  policyError.value = ''
  try {
    policyResult.value = await getCurrentSignatureGovernancePolicy()
  } catch (error) {
    policyError.value = resolveErrorMessage(error, '统一策略状态加载失败')
  } finally {
    policyLoading.value = false
  }
}

const buildModuleRows = () => {
  const moduleStatuses = policyResult.value?.moduleStatuses || []
  if (moduleStatuses.length > 0) {
    return moduleStatuses.map((moduleStatus): GovernanceRow => {
      const status = moduleStatusText(moduleStatus)
      return {
        id: `module-${moduleStatus.moduleCode}`,
        item: moduleStatus.moduleCode,
        source: displayValue(moduleStatus.policySourceCode || moduleStatus.adapterCode),
        status,
        statusType: statusTagType(status),
        keyFields: `策略 ${displayValue(moduleStatus.policyVersion || moduleStatus.adapterVersion)}；Schema ${displayValue(moduleStatus.evidenceSchemaVersion)}`,
        evidence: `策略源 ${boolText(moduleStatus.policySourcePresent)}；适配器 ${boolText(moduleStatus.adapterRegistered)}；权限 ${boolText(moduleStatus.authorityConfirmed)}`,
        blockerImpact: moduleStatus.blockers?.[0]?.impact || '—'
      }
    })
  }
  return (policyResult.value?.modules || []).map((moduleCode): GovernanceRow => ({
    id: `module-${moduleCode}`,
    item: moduleCode,
    source: '等待来源',
    status: policyResult.value?.status || '等待来源',
    statusType: statusTagType(policyResult.value?.status),
    keyFields: '等待策略模块状态',
    evidence: '等待来源',
    blockerImpact: '—'
  }))
}

const buildBlockerRows = (blockers: SignatureGovernanceBlocker[]) =>
  blockers.map((blocker, index): GovernanceRow => ({
    id: `blocker-${index}-${blocker.code}`,
    item: '阻断项',
    source: blocker.code,
    status: 'BLOCKED',
    statusType: 'danger',
    keyFields: blocker.message,
    evidence: '—',
    blockerImpact: blocker.impact
  }))

const buildClearBlockerRow = (): GovernanceRow => ({
  id: 'blocker-clear',
  item: '阻断项',
  source: '检查结果',
  status: 'CLEAR',
  statusType: 'success',
  keyFields: '暂无阻断',
  evidence: '—',
  blockerImpact: '—'
})

const rows = computed<GovernanceRow[]>(() => {
  const moduleBlockers = (policyResult.value?.moduleStatuses || []).flatMap((moduleStatus) => moduleStatus.blockers || [])
  const blockerRows = buildBlockerRows([...(policyResult.value?.blockers || []), ...moduleBlockers])
  return [
    ...buildModuleRows(),
    ...(blockerRows.length > 0 ? blockerRows : [buildClearBlockerRow()])
  ]
})

const normalizeFilterText = (value: unknown) => String(value ?? '').trim().toLowerCase()

const isMatchedRow = (row: GovernanceRow, quickFilter?: TableQuickFilterValue) => {
  if (!quickFilter) return true
  const actual = normalizeFilterText(row[quickFilter.fieldKey as keyof GovernanceRow])
  const expected = normalizeFilterText(quickFilter.value)
  return quickFilter.operator === 'eq' ? actual === expected : actual.includes(expected)
}

const filteredRows = computed(() => rows.value.filter((row) => isMatchedRow(row, queryParams.quickFilter)))

const pagedRows = computed(() => {
  const start = (queryParams.pageNo - 1) * queryParams.pageSize
  return filteredRows.value.slice(start, start + queryParams.pageSize)
})

const handlePagination = () => {
  if ((queryParams.pageNo - 1) * queryParams.pageSize >= filteredRows.value.length) {
    queryParams.pageNo = 1
  }
}

const quickFilter = useTableQuickFilter(
  'signature.governance.policy.list',
  filterDefinitions,
  queryParams,
  handlePagination
)

watch(rows, () => handlePagination())

onMounted(() => {
  void loadCurrentPolicy()
})
</script>
