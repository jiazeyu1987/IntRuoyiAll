<!-- ERP 产品库存列表 -->
<template>
  <doc-alert title="【库存】产品库存、库存明细" url="https://doc.iocoder.cn/erp/stock/" />

  <ContentWrap class="erp-viewport-list">
    <UnifiedListTemplate
      :show-filter-reset="false"
      table-key="erp.stock.stock.main"
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
          v-show="false"
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['erp:stock:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          v-show="false"
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['erp:stock:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
        <el-button
          type="primary"
          plain
          @click="handleSyncKingdeeStocks"
          :loading="kingdeeSyncLoading"
          v-hasPermi="['erp:stock:query']"
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
          data-user-table-key="erp.stock.stock.main"
          @header-dragend="handleHeaderDragend"
         height="100%">
          <el-table-column
            v-if="isColumnVisible('productName')"
            column-key="productName"
            label="产品名称"
            align="center"
            prop="productName"
            :min-width="getColumnWidthString('productName', 140)"
          />
          <el-table-column
            v-if="isColumnVisible('unitName')"
            column-key="unitName"
            label="产品单位"
            align="center"
            prop="unitName"
            :min-width="getColumnWidthString('unitName', 140)"
          />
          <el-table-column
            v-if="isColumnVisible('categoryName')"
            column-key="categoryName"
            label="产品分类"
            align="center"
            prop="categoryName"
            :min-width="getColumnWidthString('categoryName', 140)"
          />
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isColumnVisible('count')"
            column-key="count"
            label="库存量"
            align="center"
            prop="count"
            :min-width="getColumnWidthString('count', 140)"
          />
          <el-table-column
            v-if="isColumnVisible('warehouseName')"
            column-key="warehouseName"
            label="仓库"
            align="center"
            prop="warehouseName"
            :min-width="getColumnWidthString('warehouseName', 140)"
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
import download from '@/utils/download'
import { StockApi, StockVO } from '@/api/erp/stock/stock'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'
import { ProductApi, ProductVO } from '@/api/erp/product/product'
import { WarehouseApi, WarehouseVO } from '@/api/erp/stock/warehouse'

/** ERP 产品库存列表 */
defineOptions({ name: 'ErpStock' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const list = ref<StockVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  productId: undefined,
  warehouseId: undefined
})

const exportLoading = ref(false) // 导出的加载中
const productList = ref<ProductVO[]>([]) // 产品列表
const warehouseList = ref<WarehouseVO[]>([]) // 仓库列表

/** 查询列表 */
const kingdeeSyncLoading = ref(false)

const getList = async () => {
  loading.value = true
  try {
    const data = await StockApi.getStockPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */

/** 重置按钮操作 */

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await (StockApi as any).deleteStock(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await StockApi.exportStock(queryParams)
    download.excel(data, '产品库存.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
const handleSyncKingdeeStocks = async () => {
  kingdeeSyncLoading.value = true
  try {
    await ErpKingdeeSyncApi.runIncrementalSync('STOCK')
    message.success('ERP 库存增量同步任务已提交')
    await getList()
    productList.value = await ProductApi.getProductSimpleList()
    warehouseList.value = await WarehouseApi.getWarehouseSimpleList()
  } finally {
    kingdeeSyncLoading.value = false
  }
}
void handleDelete

const filterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'productId',
    label: '产品',
    type: 'select',
    virtualized: true,
    queryParamKey: 'productId',
    options: productList.value.map((item) => ({ label: item.name, value: item.id }))
  },
  {
    key: 'warehouseId',
    label: '仓库',
    type: 'select',
    queryParamKey: 'warehouseId',
    options: warehouseList.value.map((item) => ({ label: item.name, value: item.id }))
  }
])
const defaultColumns: UserTableColumnDefinition[] = [
  {
    key: 'productName',
    label: '产品名称',
    sortable: false,
    minWidth: 140
  },
  {
    key: 'unitName',
    label: '产品单位',
    sortable: false,
    minWidth: 140
  },
  {
    key: 'categoryName',
    label: '产品分类',
    sortable: false,
    minWidth: 140
  },
  {
    key: 'count',
    label: '库存量',
    sortable: false,
    minWidth: 140
  },
  {
    key: 'warehouseName',
    label: '仓库',
    sortable: false,
    minWidth: 140
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
} = useUserTableColumns('erp.stock.stock.main', defaultColumns)
const quickFilter = useTableQuickFilter(
  'erp.stock.stock.main',
  filterDefinitions,
  queryParams,
  getList
)

onMounted(async () => {
  await getList()
  // 加载产品、仓库列表
  productList.value = await ProductApi.getProductSimpleList()
  warehouseList.value = await WarehouseApi.getWarehouseSimpleList()
})
</script>
