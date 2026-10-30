<!-- ERP 即时库存列表 -->
<template>
  <ContentWrap class="erp-viewport-list">
    <UnifiedListTemplate
      ref="codeSyncListTemplate"
      table-key="erp.production.inventory-list.main"
      single-line-toolbar
      :query-model="queryParams"
      :filter-definitions="filterDefinitions"
      :quick-filter-state="quickFilter.state"
      :selected-filter-definition="quickFilter.selectedDefinition.value"
      :operator-options="quickFilter.operatorOptions.value"
      :columns="columns"
      :column-saving="columnSaving"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="quickFilter.updateState"
      @quick-filter-query="quickFilter.applyQuickFilter"
      @column-change="saveColumnConfig"
      @pagination="getList"
      :load-filter-rows="loadColumnFilterRows0"
      @filtered-rows="list = $event"
      :list-rows="list"
      @column-filter-reset="getList"
    >
      <template #actions>
        <SyncByCodeButton sync-type="INVENTORY_DETAIL" code-label="物料编码" :on-synced="handleCodeSynced" />
        <el-button v-if="queryParams.syncCode" plain @click="clearCodeSyncFilter">显示全部</el-button>
        <RecentFullSyncButton sync-type="STOCK" />
      </template>
      <template #table="{ resolveRows }">
        <el-table
          v-loading="loading || columnLoading"
          :data="resolveRows(list)"
          :stripe="true"
          :show-overflow-tooltip="true"
          border
          row-key="id"
          data-user-table-column-explicit
          data-user-table-key="erp.production.inventory-list.main"
          @header-dragend="handleHeaderDragend"
         height="100%">
          <el-table-column
            v-if="isColumnVisible('materialNumber')"
            column-key="materialNumber"
            label="物料编码"
            align="center"
            prop="materialNumber"
            fixed="left"
            :width="getColumnWidthString('materialNumber', 160)"
          />
          <el-table-column
            v-if="isColumnVisible('materialName')"
            column-key="materialName"
            label="物料名称"
            align="center"
            prop="materialName"
            :width="getColumnWidthString('materialName', 180)"
          />
          <el-table-column
            v-if="isColumnVisible('materialSpecification')"
            column-key="materialSpecification"
            label="规格型号"
            align="center"
            prop="materialSpecification"
            :width="getColumnWidthString('materialSpecification', 180)"
          />
          <el-table-column
            v-if="isColumnVisible('warehouseName')"
            column-key="warehouseName"
            label="仓库"
            align="center"
            prop="warehouseName"
            :width="getColumnWidthString('warehouseName', 180)"
          />
          <el-table-column
            v-if="isColumnVisible('lotNumber')"
            column-key="lotNumber"
            label="批号"
            align="center"
            prop="lotNumber"
            :width="getColumnWidthString('lotNumber', 140)"
          />
          <el-table-column
            v-if="isColumnVisible('unitName')"
            column-key="unitName"
            label="单位"
            align="center"
            prop="unitName"
            :width="getColumnWidthString('unitName', 90)"
          />
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isColumnVisible('quantity')"
            column-key="quantity"
            label="库存数量"
            align="center"
            prop="quantity"
            :width="getColumnWidthString('quantity', 120)"
          />
          <el-table-column
            v-if="isColumnVisible('stockOrgName')"
            column-key="stockOrgName"
            label="库存组织"
            align="center"
            prop="stockOrgName"
            :width="getColumnWidthString('stockOrgName', 180)"
          />
          <el-table-column
            v-if="isColumnVisible('sourceModifyTime')"
            column-key="sourceModifyTime"
            label="ERP修改时间"
            align="center"
            prop="sourceModifyTime"
            :formatter="dateFormatter"
            :width="getColumnWidthString('sourceModifyTime', 180)"
          />
          <el-table-column
            v-if="isColumnVisible('lastSyncTime')"
            column-key="lastSyncTime"
            label="最后同步时间"
            align="center"
            prop="lastSyncTime"
            :formatter="dateFormatter"
            :width="getColumnWidthString('lastSyncTime', 180)"
          />
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>
</template>
<script setup lang="ts">
import SyncByCodeButton from '@/views/erp/components/SyncByCodeButton.vue'
import { useErpCodeSyncFilter } from '@/views/erp/components/useErpCodeSyncFilter'

import { loadAllListPages } from '@/hooks/web/useListColumnFilter'

import RecentFullSyncButton from '@/views/erp/components/RecentFullSyncButton.vue'
import '@/views/erp/components/viewport-list.css'

import { erpListIntegerColumnFormatter } from '@/views/erp/utils/integerDisplay'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'
import { dateFormatter } from '@/utils/formatTime'
import { ErpInventoryListApi, ErpInventoryListVO } from '@/api/erp/production/inventory-list'

defineOptions({ name: 'ErpInventoryList' })

const loading = ref(true)
const list = ref<ErpInventoryListVO[]>([])
const total = ref(0)
const queryParams = reactive({
  syncCode: undefined as string | undefined,
  pageNo: 1,
  pageSize: 10,
  materialNumber: undefined,
  materialName: undefined,
  warehouseName: undefined,
  lotNumber: undefined
})

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpInventoryListApi.getPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}


let initialActivation = true
onActivated(() => {
  if (initialActivation) {
    initialActivation = false
    return
  }
  getList()
})

const filterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'materialNumber',
    label: '物料编码',
    type: 'text',
    queryParamKey: 'materialNumber',
    operators: ['contains']
  },
  {
    key: 'materialName',
    label: '物料名称',
    type: 'text',
    queryParamKey: 'materialName',
    operators: ['contains']
  },
  {
    key: 'warehouseName',
    label: '仓库',
    type: 'text',
    queryParamKey: 'warehouseName',
    operators: ['contains']
  },
  {
    key: 'lotNumber',
    label: '批号',
    type: 'text',
    queryParamKey: 'lotNumber',
    operators: ['contains']
  }
]
const defaultColumns: UserTableColumnDefinition[] = [
  {
    key: 'materialNumber',
    label: '物料编码',
    sortable: false,
    width: 160
  },
  {
    key: 'materialName',
    label: '物料名称',
    sortable: false,
    width: 180
  },
  {
    key: 'materialSpecification',
    label: '规格型号',
    sortable: false,
    width: 180
  },
  {
    key: 'warehouseName',
    label: '仓库',
    sortable: false,
    width: 180
  },
  {
    key: 'lotNumber',
    label: '批号',
    sortable: false,
    width: 140
  },
  {
    key: 'unitName',
    label: '单位',
    sortable: false,
    width: 90
  },
  {
    key: 'quantity',
    label: '库存数量',
    sortable: false,
    width: 120
  },
  {
    key: 'stockOrgName',
    label: '库存组织',
    sortable: false,
    width: 180
  },
  {
    key: 'sourceModifyTime',
    label: 'ERP修改时间',
    sortable: false,
    width: 180
  },
  {
    key: 'lastSyncTime',
    label: '最后同步时间',
    sortable: false,
    width: 180
  }
]
const {
  columns,
  loading: columnLoading,
  saving: columnSaving,
  isColumnVisible,
  getColumnWidthString,
  handleHeaderDragend,
  saveConfig: saveColumnConfig
} = useUserTableColumns('erp.production.inventory-list.main', defaultColumns)
const quickFilter = useTableQuickFilter(
  'erp.production.inventory-list.main',
  filterDefinitions,
  queryParams,
  getList
)

onMounted(() => {
  getList()
})

const loadColumnFilterRows0 = async () => {
  const params = { ...queryParams }
  const rows = await loadAllListPages<any>((pagination) =>
    ErpInventoryListApi.getPage({ ...params, ...pagination })
  )
  return rows
}

const codeSyncListTemplate = ref<InstanceType<typeof UnifiedListTemplate>>()
const { handleCodeSynced, clearCodeSyncFilter } = useErpCodeSyncFilter(
  queryParams, quickFilter.state, getList, () => codeSyncListTemplate.value!.resetColumnFilters()
)
</script>
