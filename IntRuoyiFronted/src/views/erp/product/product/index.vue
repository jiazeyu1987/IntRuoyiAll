<!-- ERP 产品列表 -->
<template>
  <doc-alert title="【产品】产品信息、分类、单位" url="https://doc.iocoder.cn/erp/product/" />

  <ContentWrap class="erp-viewport-list">
    <UnifiedListTemplate
      :show-filter-reset="false"
      table-key="erp.product.product.main"
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
        <el-dropdown trigger="click" v-hasPermi="['erp:product:create', 'erp:product:export']">
          <el-button type="primary" plain :loading="exportLoading || kingdeeSyncLoading">
            高级 <Icon icon="ep:arrow-down" class="ml-5px" />
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-hasPermi="['erp:product:create']" @click="importVisible = true">导入物料</el-dropdown-item>
              <el-dropdown-item v-hasPermi="['erp:product:export']" :disabled="exportLoading" @click="handleExport">导出物料</el-dropdown-item>
              <el-dropdown-item v-hasPermi="['erp:product:create']" :disabled="kingdeeSyncLoading" @click="handleSyncKingdeeProducts">增量同步</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
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
          data-user-table-key="erp.product.product.main"
          @header-dragend="handleHeaderDragend"
         height="100%">
          <el-table-column
            v-if="isColumnVisible('barCode')"
            column-key="barCode"
            label="条码"
            align="center"
            prop="barCode"
            :min-width="getColumnWidthString('barCode', 140)"
          />
          <el-table-column
            v-if="isColumnVisible('name')"
            column-key="name"
            label="名称"
            align="center"
            prop="name"
            :min-width="getColumnWidthString('name', 140)"
          />
          <el-table-column
            v-if="isColumnVisible('standard')"
            column-key="standard"
            label="规格"
            align="center"
            prop="standard"
            :min-width="getColumnWidthString('standard', 140)"
          />
          <el-table-column
            v-if="isColumnVisible('categoryName')"
            column-key="categoryName"
            label="分类"
            align="center"
            prop="categoryName"
            :min-width="getColumnWidthString('categoryName', 140)"
          />
          <el-table-column
            v-if="isColumnVisible('unitName')"
            column-key="unitName"
            label="单位"
            align="center"
            prop="unitName"
            :min-width="getColumnWidthString('unitName', 140)"
          />
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isColumnVisible('purchasePrice')"
            column-key="purchasePrice"
            label="采购价格"
            align="center"
            prop="purchasePrice"
            :min-width="getColumnWidthString('purchasePrice', 140)"
          />
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isColumnVisible('salePrice')"
            column-key="salePrice"
            label="销售价格"
            align="center"
            prop="salePrice"
            :min-width="getColumnWidthString('salePrice', 140)"
          />
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isColumnVisible('minPrice')"
            column-key="minPrice"
            label="最低价格"
            align="center"
            prop="minPrice"
            :min-width="getColumnWidthString('minPrice', 140)"
          />
          <el-table-column
            v-if="isColumnVisible('status')"
            column-key="status"
            label="状态"
            align="center"
            prop="status"
            :min-width="getColumnWidthString('status', 140)"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isColumnVisible('createTime')"
            column-key="createTime"
            label="创建时间"
            align="center"
            prop="createTime"
            :formatter="dateFormatter"
            :width="getColumnWidthString('createTime', 180)"
          />
          <el-table-column
            v-if="false"
            column-key="actions"
            label="操作"
            align="center"
            :width="getColumnWidthString('actions', 110)"
          >
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
                v-hasPermi="['erp:product:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['erp:product:delete']"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <Dialog v-model="importVisible" title="导入物料" width="520px" :fullscreen="false">
    <el-upload :auto-upload="false" :limit="1" accept=".xls,.xlsx" v-model:file-list="importFiles">
      <el-button :disabled="importLoading">选择 Excel 文件</el-button>
    </el-upload>
    <template #footer>
      <el-button type="primary" :loading="importLoading" @click="handleImport">开始导入</el-button>
      <el-button :disabled="importLoading" @click="importVisible = false">关闭</el-button>
    </template>
  </Dialog>
  <ProductForm ref="formRef" @success="getList" />
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
import download from '@/utils/download'
import { ProductApi, ProductVO } from '@/api/erp/product/product'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'
import { ProductCategoryApi, ProductCategoryVO } from '@/api/erp/product/category'
import ProductForm from './ProductForm.vue'
import { DICT_TYPE } from '@/utils/dict'

defineOptions({ name: 'ErpProduct' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<ProductVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  barCode: undefined,
  categoryId: undefined
})

const importVisible = ref(false)
const importLoading = ref(false)
const importFiles = ref<import('element-plus').UploadUserFile[]>([])
const handleImport = async () => {
  const file = importFiles.value[0]?.raw
  if (!file) { message.warning('请选择 Excel 文件'); return }
  importLoading.value = true
  try {
    const count = await ProductApi.importProducts(file)
    message.success(`成功导入 ${count} 条物料`)
    importVisible.value = false
    importFiles.value = []
    await getList()
  } finally { importLoading.value = false }
}
const exportLoading = ref(false)
const kingdeeSyncLoading = ref(false)
const categoryList = ref<ProductCategoryVO[]>([])

const getList = async () => {
  loading.value = true
  try {
    const data = await ProductApi.getProductPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await ProductApi.deleteProduct(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await ProductApi.exportProduct(queryParams)
    download.excel(data, '物料.xls')
  } finally {
    exportLoading.value = false
  }
}

const handleSyncKingdeeProducts = async () => {
  kingdeeSyncLoading.value = true
  try {
    await ErpKingdeeSyncApi.runIncrementalSync('PRODUCT')
    message.success('物料列表增量同步任务已提交')
    await loadCategoryList()
    await getList()
  } finally {
    kingdeeSyncLoading.value = false
  }
}

const loadCategoryList = async () => {
  const categoryData = await ProductCategoryApi.getProductCategorySimpleList()
  categoryList.value = categoryData
}

const filterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'name',
    label: '名称',
    type: 'text',
    queryParamKey: 'name',
    operators: ['contains']
  },
  {
    key: 'barCode',
    label: '产品编码/条码',
    type: 'text',
    queryParamKey: 'barCode',
    operators: ['contains']
  },
  {
    key: 'categoryId',
    label: '分类',
    type: 'select',
    queryParamKey: 'categoryId',
    options: categoryList.value.map((item) => ({ label: item.name, value: item.id }))
  }
])

const defaultColumns: UserTableColumnDefinition[] = [
  {
    key: 'barCode',
    label: '条码',
    sortable: false,
    minWidth: 140
  },
  {
    key: 'name',
    label: '名称',
    sortable: false,
    minWidth: 140
  },
  {
    key: 'standard',
    label: '规格',
    sortable: false,
    minWidth: 140
  },
  {
    key: 'categoryName',
    label: '分类',
    sortable: false,
    minWidth: 140
  },
  {
    key: 'unitName',
    label: '单位',
    sortable: false,
    minWidth: 140
  },
  {
    key: 'purchasePrice',
    label: '采购价格',
    sortable: false,
    minWidth: 140
  },
  {
    key: 'salePrice',
    label: '销售价格',
    sortable: false,
    minWidth: 140
  },
  {
    key: 'minPrice',
    label: '最低价格',
    sortable: false,
    minWidth: 140
  },
  {
    key: 'status',
    label: '状态',
    sortable: false,
    minWidth: 140
  },
  {
    key: 'createTime',
    label: '创建时间',
    sortable: false,
    width: 180
  },
]
const {
  columns,
  loading: columnLoading,
  saving: columnSaving,
  isColumnVisible,
  getColumnWidthString,
  handleHeaderDragend,
  saveConfig: saveColumnConfig
} = useUserTableColumns('erp.product.product.main', defaultColumns)
const quickFilter = useTableQuickFilter(
  'erp.product.product.main',
  filterDefinitions,
  queryParams,
  getList
)

onMounted(async () => {
  await getList()
  await loadCategoryList()
})
</script>
