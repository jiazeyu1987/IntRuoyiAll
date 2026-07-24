<!-- ERP 产品列表 -->
<template>
  <doc-alert title="【产品】产品信息、分类、单位" url="https://doc.iocoder.cn/erp/product/" />

  <ContentWrap>
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="产品编码/条码" prop="barCode">
        <el-input
          v-model="queryParams.barCode"
          placeholder="请输入产品编码/条码"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-tree-select
          v-model="queryParams.categoryId"
          :data="categoryList"
          :props="defaultProps"
          check-strictly
          default-expand-all
          placeholder="请输入分类"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['erp:product:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['erp:product:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
        <el-button
          type="warning"
          plain
          @click="handleSyncKingdeeProducts"
          :loading="kingdeeSyncLoading"
          v-hasPermi="['erp:product:create']"
        >
          <Icon icon="ep:refresh" class="mr-5px" /> 增量同步
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="条码" align="center" prop="barCode" />
      <el-table-column label="名称" align="center" prop="name" />
      <el-table-column label="规格" align="center" prop="standard" />
      <el-table-column label="分类" align="center" prop="categoryName" />
      <el-table-column label="单位" align="center" prop="unitName" />
      <el-table-column
        label="采购价格"
        align="center"
        prop="purchasePrice"
        :formatter="erpPriceTableColumnFormatter"
      />
      <el-table-column
        label="销售价格"
        align="center"
        prop="salePrice"
        :formatter="erpPriceTableColumnFormatter"
      />
      <el-table-column
        label="最低价格"
        align="center"
        prop="minPrice"
        :formatter="erpPriceTableColumnFormatter"
      />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180px"
      />
      <el-table-column label="操作" align="center" width="110">
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
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <ProductForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { ProductApi, ProductVO } from '@/api/erp/product/product'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'
import { ProductCategoryApi, ProductCategoryVO } from '@/api/erp/product/category'
import ProductForm from './ProductForm.vue'
import { DICT_TYPE } from '@/utils/dict'
import { defaultProps, handleTree } from '@/utils/tree'
import { erpPriceTableColumnFormatter } from '@/utils'

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
const queryFormRef = ref()
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

const handleQuery = (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    resetQuery()
    return
  }
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery(true)
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
    download.excel(data, '产品.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

const handleSyncKingdeeProducts = async () => {
  kingdeeSyncLoading.value = true
  try {
    await ErpKingdeeSyncApi.runIncrementalSyncJob('kingdeeProductItemSyncJob')
    message.success('ERP 商品增量同步任务已提交')
    await loadCategoryList()
    await getList()
  } finally {
    kingdeeSyncLoading.value = false
  }
}

const loadCategoryList = async () => {
  const categoryData = await ProductCategoryApi.getProductCategorySimpleList()
  categoryList.value = handleTree(categoryData, 'id', 'parentId')
}

onMounted(async () => {
  await getList()
  await loadCategoryList()
})
</script>
