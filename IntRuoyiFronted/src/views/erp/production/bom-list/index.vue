<!-- ERP 物料清单列表 -->
<template>
  <ContentWrap class="erp-viewport-list">
    <UnifiedListTemplate
      :show-filter-reset="false"
      table-key="erp.production.bom-list.main"
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
    >
      <template #actions>
        <el-button
          type="primary"
          plain
          :loading="kingdeeSyncLoading"
          @click="handleIncrementalSync"
          v-hasPermi="['erp:kingdee-sync:query']"
        >
          <Icon icon="ep:refresh-right" class="mr-5px" /> 增量同步
        </el-button>
      </template>
      <template #table>
        <el-table
          v-loading="loading || columnLoading"
          :data="list"
          :stripe="true"
          :show-overflow-tooltip="true"
          border
          row-key="id"
          data-user-table-column-explicit
          data-user-table-key="erp.production.bom-list.main"
          @header-dragend="handleHeaderDragend"
         height="100%">
          <el-table-column
            v-if="isColumnVisible('parentMaterialCode')"
            column-key="parentMaterialCode"
            label="父项物料编码"
            align="center"
            prop="parentMaterialCode"
            fixed="left"
            :width="getColumnWidthString('parentMaterialCode', 160)"
          />
          <el-table-column
            v-if="isColumnVisible('parentMaterialName')"
            column-key="parentMaterialName"
            label="父项物料名称"
            align="center"
            prop="parentMaterialName"
            :width="getColumnWidthString('parentMaterialName', 180)"
          />
          <el-table-column
            v-if="isColumnVisible('bomNumber')"
            column-key="bomNumber"
            label="BOM编号"
            align="center"
            prop="bomNumber"
            :width="getColumnWidthString('bomNumber', 150)"
          />
          <el-table-column
            v-if="isColumnVisible('bomType')"
            column-key="bomType"
            label="BOM类型"
            align="center"
            prop="bomType"
            :width="getColumnWidthString('bomType', 110)"
          />
          <el-table-column
            v-if="isColumnVisible('documentStatus')"
            column-key="documentStatus"
            label="审核状态"
            align="center"
            prop="documentStatus"
            :width="getColumnWidthString('documentStatus', 110)"
          />
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isColumnVisible('lineNo')"
            column-key="lineNo"
            label="行号"
            align="center"
            prop="lineNo"
            :width="getColumnWidthString('lineNo', 80)"
          />
          <el-table-column
            v-if="isColumnVisible('childMaterialCode')"
            column-key="childMaterialCode"
            label="子项物料编码"
            align="center"
            prop="childMaterialCode"
            :width="getColumnWidthString('childMaterialCode', 160)"
          />
          <el-table-column
            v-if="isColumnVisible('childMaterialName')"
            column-key="childMaterialName"
            label="子项物料名称"
            align="center"
            prop="childMaterialName"
            :width="getColumnWidthString('childMaterialName', 180)"
          />
          <el-table-column
            v-if="isColumnVisible('childMaterialSpecification')"
            column-key="childMaterialSpecification"
            label="规格型号"
            align="center"
            prop="childMaterialSpecification"
            :width="getColumnWidthString('childMaterialSpecification', 200)"
          />
          <el-table-column
            v-if="isColumnVisible('childUnitName')"
            column-key="childUnitName"
            label="单位"
            align="center"
            prop="childUnitName"
            :width="getColumnWidthString('childUnitName', 90)"
          />
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isColumnVisible('numerator')"
            column-key="numerator"
            label="分子"
            align="center"
            prop="numerator"
            :width="getColumnWidthString('numerator', 100)"
          />
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isColumnVisible('denominator')"
            column-key="denominator"
            label="分母"
            align="center"
            prop="denominator"
            :width="getColumnWidthString('denominator', 100)"
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
import { ErpBomListApi, ErpBomListVO } from '@/api/erp/production/bom-list'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'

defineOptions({ name: 'ErpBomList' })

const message = useMessage()
const loading = ref(true)
const kingdeeSyncLoading = ref(false)
const list = ref<ErpBomListVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  bomNumber: undefined,
  parentMaterialCode: undefined,
  parentMaterialName: undefined,
  childMaterialCode: undefined,
  childMaterialName: undefined
})

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpBomListApi.getPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleIncrementalSync = async () => {
  kingdeeSyncLoading.value = true
  try {
    await ErpKingdeeSyncApi.runIncrementalSync('BOM')
    message.success('产品 BOM 增量同步任务已提交')
    await getList()
  } finally {
    kingdeeSyncLoading.value = false
  }
}

onActivated(() => {
  getList()
})

const filterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'bomNumber',
    label: 'BOM编号',
    type: 'text',
    queryParamKey: 'bomNumber',
    operators: ['contains']
  },
  {
    key: 'parentMaterialCode',
    label: '父项编码',
    type: 'text',
    queryParamKey: 'parentMaterialCode',
    operators: ['contains']
  },
  {
    key: 'parentMaterialName',
    label: '父项名称',
    type: 'text',
    queryParamKey: 'parentMaterialName',
    operators: ['contains']
  },
  {
    key: 'childMaterialCode',
    label: '子项编码',
    type: 'text',
    queryParamKey: 'childMaterialCode',
    operators: ['contains']
  },
  {
    key: 'childMaterialName',
    label: '子项名称',
    type: 'text',
    queryParamKey: 'childMaterialName',
    operators: ['contains']
  }
]
const defaultColumns: UserTableColumnDefinition[] = [
  {
    key: 'parentMaterialCode',
    label: '父项物料编码',
    sortable: false,
    width: 160
  },
  {
    key: 'parentMaterialName',
    label: '父项物料名称',
    sortable: false,
    width: 180
  },
  {
    key: 'bomNumber',
    label: 'BOM编号',
    sortable: false,
    width: 150
  },
  {
    key: 'bomType',
    label: 'BOM类型',
    sortable: false,
    width: 110
  },
  {
    key: 'documentStatus',
    label: '审核状态',
    sortable: false,
    width: 110
  },
  {
    key: 'lineNo',
    label: '行号',
    sortable: false,
    width: 80
  },
  {
    key: 'childMaterialCode',
    label: '子项物料编码',
    sortable: false,
    width: 160
  },
  {
    key: 'childMaterialName',
    label: '子项物料名称',
    sortable: false,
    width: 180
  },
  {
    key: 'childMaterialSpecification',
    label: '规格型号',
    sortable: false,
    width: 200
  },
  {
    key: 'childUnitName',
    label: '单位',
    sortable: false,
    width: 90
  },
  {
    key: 'numerator',
    label: '分子',
    sortable: false,
    width: 100
  },
  {
    key: 'denominator',
    label: '分母',
    sortable: false,
    width: 100
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
} = useUserTableColumns('erp.production.bom-list.main', defaultColumns)
const quickFilter = useTableQuickFilter(
  'erp.production.bom-list.main',
  filterDefinitions,
  queryParams,
  getList
)

onMounted(() => {
  getList()
})
</script>
