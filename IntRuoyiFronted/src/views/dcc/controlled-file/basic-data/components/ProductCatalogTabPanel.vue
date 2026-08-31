<template>
  <ContentWrap class="scheme-d-basic-data-page scheme-d-basic-data-page--dcc-product-catalog">
    <div class="mb-16px flex items-center gap-8px">
      <span class="text-18px font-600 text-[var(--el-text-color-primary)]">基础数据 / DCC产品目录</span>
    </div>
    <UnifiedListTemplate
      class="dcc-product-catalog-list-template"
      table-key="dcc.productCatalog.main"
      :query-model="queryParams"
      label-width="76px"
      :filter-definitions="productCatalogQuickFilterDefinitions"
      :quick-filter-state="productCatalogQuickFilter.state"
      :selected-filter-definition="productCatalogQuickFilter.selectedDefinition.value"
      :operator-options="productCatalogQuickFilter.operatorOptions.value"
      :columns="productCatalogColumns"
      :column-saving="productCatalogColumnSaving"
      :total="total"
      single-line-toolbar
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      v-model:sort-state="productCatalogSortState"
      @update:quick-filter-state="productCatalogQuickFilter.updateState"
      @quick-filter-query="productCatalogQuickFilter.applyQuickFilter"
      @column-change="saveProductCatalogColumnConfig"
      @column-reset="resetProductCatalogColumnConfig"
      @sort-change="handleProductCatalogSortChange"
      @pagination="getList"
    >
      <template #actions>
        <el-button
          class="scheme-d-btn scheme-d-btn--success"
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['dcc:project-code:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增产品目录
        </el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          class="dcc-product-catalog-resizable-table"
          data-user-table-column-explicit
          data-user-table-key="dcc.productCatalog.main"
          :data="list"
          border
          :allow-drag-last-column="true"
          :stripe="true"
          :show-overflow-tooltip="true"
          @header-dragend="handleProductCatalogHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isProductCatalogColumnVisible('dataSource')"
            label="数据来源"
            prop="dataSource"
            :width="getProductCatalogColumnWidthString('dataSource')"
            :min-width="getProductCatalogColumnMinWidthString('dataSource', 120)"
            v-bind="sortColumnAttrs('dataSource')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('categoryLevel1')"
            label="产品类别 I"
            prop="categoryLevel1"
            :width="getProductCatalogColumnWidthString('categoryLevel1')"
            :min-width="getProductCatalogColumnMinWidthString('categoryLevel1', 180)"
            v-bind="sortColumnAttrs('categoryLevel1')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('categoryLevel2')"
            label="产品类别 II"
            prop="categoryLevel2"
            :width="getProductCatalogColumnWidthString('categoryLevel2')"
            :min-width="getProductCatalogColumnMinWidthString('categoryLevel2', 180)"
            v-bind="sortColumnAttrs('categoryLevel2')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('productSequence')"
            label="产品序号"
            prop="productSequence"
            :width="getProductCatalogColumnWidthString('productSequence')"
            :min-width="getProductCatalogColumnMinWidthString('productSequence', 100)"
            v-bind="sortColumnAttrs('productSequence')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('product')"
            label="产品"
            prop="product"
            :width="getProductCatalogColumnWidthString('product')"
            :min-width="getProductCatalogColumnMinWidthString('product', 220)"
            v-bind="sortColumnAttrs('product')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('productCode')"
            label="产品编码"
            prop="productCode"
            :width="getProductCatalogColumnWidthString('productCode')"
            :min-width="getProductCatalogColumnMinWidthString('productCode', 120)"
            v-bind="sortColumnAttrs('productCode')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('projectName')"
            label="项目名称"
            prop="projectName"
            :width="getProductCatalogColumnWidthString('projectName')"
            :min-width="getProductCatalogColumnMinWidthString('projectName', 180)"
            v-bind="sortColumnAttrs('projectName')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('projectCode')"
            label="项目代码"
            prop="projectCode"
            :width="getProductCatalogColumnWidthString('projectCode')"
            :min-width="getProductCatalogColumnMinWidthString('projectCode', 120)"
            v-bind="sortColumnAttrs('projectCode')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('registrationCertificateName')"
            label="注册证名称"
            prop="registrationCertificateName"
            :width="getProductCatalogColumnWidthString('registrationCertificateName')"
            :min-width="
              getProductCatalogColumnMinWidthString('registrationCertificateName', 220)
            "
            v-bind="sortColumnAttrs('registrationCertificateName')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('registrationCertificateNumber')"
            label="注册证号"
            prop="registrationCertificateNumber"
            :width="getProductCatalogColumnWidthString('registrationCertificateNumber')"
            :min-width="
              getProductCatalogColumnMinWidthString('registrationCertificateNumber', 180)
            "
            v-bind="sortColumnAttrs('registrationCertificateNumber')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('certificateHolder')"
            label="持证人"
            prop="certificateHolder"
            :width="getProductCatalogColumnWidthString('certificateHolder')"
            :min-width="getProductCatalogColumnMinWidthString('certificateHolder', 160)"
            v-bind="sortColumnAttrs('certificateHolder')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('registrationPlace')"
            label="注册地"
            prop="registrationPlace"
            :width="getProductCatalogColumnWidthString('registrationPlace')"
            :min-width="getProductCatalogColumnMinWidthString('registrationPlace', 120)"
            v-bind="sortColumnAttrs('registrationPlace')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('effectiveDate')"
            label="生效日期"
            prop="effectiveDate"
            :width="getProductCatalogColumnWidthString('effectiveDate', 120)"
            v-bind="sortColumnAttrs('effectiveDate')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('expiryDate')"
            label="有效期至"
            prop="expiryDate"
            :width="getProductCatalogColumnWidthString('expiryDate', 120)"
            v-bind="sortColumnAttrs('expiryDate')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('classification')"
            label="分类"
            prop="classification"
            :width="getProductCatalogColumnWidthString('classification')"
            :min-width="getProductCatalogColumnMinWidthString('classification', 120)"
            v-bind="sortColumnAttrs('classification')"
          />
          <el-table-column
            v-if="isProductCatalogColumnVisible('productStatus')"
            label="产品状态"
            prop="productStatus"
            :width="getProductCatalogColumnWidthString('productStatus', 120)"
            v-bind="sortColumnAttrs('productStatus')"
          >
            <template #default="{ row }">{{ formatProductStatus(row.productStatus) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isProductCatalogColumnVisible('registrationInfoLink')"
            label="注册证信息链接"
            prop="registrationInfoLink"
            :width="getProductCatalogColumnWidthString('registrationInfoLink')"
            :min-width="getProductCatalogColumnMinWidthString('registrationInfoLink', 150)"
            v-bind="sortColumnAttrs('registrationInfoLink')"
          >
            <template #default="{ row }">
              <el-link
                v-if="row.registrationInfoLink"
                :href="row.registrationInfoLink"
                target="_blank"
                type="primary"
              >
                查看链接
              </el-link>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProductCatalogColumnVisible('remark')"
            label="备注"
            prop="remark"
            :width="getProductCatalogColumnWidthString('remark')"
            :min-width="getProductCatalogColumnMinWidthString('remark', 220)"
            v-bind="sortColumnAttrs('remark')"
          >
            <template #default="{ row }">{{ row.remark || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isProductCatalogColumnVisible('actions')"
            label="操作"
            prop="actions"
            fixed="right"
            :width="getProductCatalogColumnWidthString('actions', 130)"
          >
            <template #default="{ row }">
              <el-button
                link
                class="scheme-d-row-action scheme-d-row-action--primary"
                type="primary"
                @click="openForm('update', row)"
                v-hasPermi="['dcc:project-code:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                class="scheme-d-row-action scheme-d-row-action--danger"
                type="danger"
                @click="handleDelete(row)"
                v-hasPermi="['dcc:project-code:delete']"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <Dialog v-model="formVisible" class="scheme-d-form-control" title="产品目录维护" width="820px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="126px"
    >
      <el-form-item label="数据来源" prop="dataSource">
        <el-select
          v-model="formData.dataSource"
          class="!w-100%"
          :disabled="formType === 'update'"
          placeholder="请选择数据来源"
        >
          <el-option
            v-for="item in dataSourceOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="产品类别 I" prop="categoryLevel1">
        <el-input v-model="formData.categoryLevel1" placeholder="请输入产品类别 I" />
      </el-form-item>
      <el-form-item label="产品类别 II" prop="categoryLevel2">
        <el-input v-model="formData.categoryLevel2" placeholder="请输入产品类别 II" />
      </el-form-item>
      <el-form-item label="产品序号" prop="productSequence">
        <el-input v-model="formData.productSequence" placeholder="请输入产品序号" />
      </el-form-item>
      <el-form-item label="产品" prop="product">
        <el-input v-model="formData.product" placeholder="请输入产品" />
      </el-form-item>
      <el-form-item label="产品编码" prop="productCode">
        <el-input v-model="formData.productCode" placeholder="请输入产品编码" />
      </el-form-item>
      <el-form-item label="项目名称" prop="projectName">
        <el-input v-model="formData.projectName" placeholder="请输入项目名称" />
      </el-form-item>
      <el-form-item label="项目代码" prop="projectCode">
        <el-input v-model="formData.projectCode" placeholder="请输入项目代码" />
      </el-form-item>
      <el-form-item label="注册证名称" prop="registrationCertificateName">
        <el-input v-model="formData.registrationCertificateName" placeholder="请输入注册证名称" />
      </el-form-item>
      <el-form-item label="注册证号" prop="registrationCertificateNumber">
        <el-input v-model="formData.registrationCertificateNumber" placeholder="请输入注册证号" />
      </el-form-item>
      <el-form-item label="持证人" prop="certificateHolder">
        <el-input v-model="formData.certificateHolder" placeholder="请输入持证人" />
      </el-form-item>
      <el-form-item label="注册地" prop="registrationPlace">
        <el-input v-model="formData.registrationPlace" placeholder="请输入注册地" />
      </el-form-item>
      <el-form-item label="生效日期" prop="effectiveDate">
        <el-input v-model="formData.effectiveDate" placeholder="例如 2026-07-03" />
      </el-form-item>
      <el-form-item label="有效期至" prop="expiryDate">
        <el-input v-model="formData.expiryDate" placeholder="例如 2031-07-03" />
      </el-form-item>
      <el-form-item label="分类" prop="classification">
        <el-input v-model="formData.classification" placeholder="请输入分类" />
      </el-form-item>
      <el-form-item label="注册证信息链接" prop="registrationInfoLink">
        <el-input v-model="formData.registrationInfoLink" placeholder="请输入注册证信息链接" />
      </el-form-item>
      <el-form-item label="产品状态" prop="productStatus">
        <el-select v-model="formData.productStatus" class="!w-100%" clearable placeholder="请选择产品状态">
          <el-option
            v-for="item in productStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" :rows="3" type="textarea" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="scheme-d-dialog-footer">
        <el-button
          class="scheme-d-btn scheme-d-btn--success"
          type="primary"
          :disabled="formLoading"
          @click="submitForm"
        >
          确定
        </el-button>
        <el-button class="scheme-d-btn scheme-d-btn--neutral" @click="formVisible = false">取消</el-button>
      </div>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormRules } from 'element-plus'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import type {
  DccProductCatalogPageReqVO,
  DccProductCatalogRespVO,
  DccProductCatalogSaveReqVO,
  DccProductCatalogUpdateReqVO
} from '@/api/dcc/controlledFile/productCatalog'
import {
  createProductCatalog,
  deleteProductCatalog,
  getProductCatalogPage,
  updateProductCatalog
} from '@/api/dcc/controlledFile/productCatalog'

defineOptions({ name: 'ProductCatalogTabPanel' })

const message = useMessage()
const loading = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const total = ref(0)
const list = ref<DccProductCatalogRespVO[]>([])
const formRef = ref()

const productStatusOptions = [
  { label: '在研(N)', value: 'N' },
  { label: '在售(S)', value: 'S' },
  { label: '已取消(C)', value: 'C' }
]

const dataSourceOptions = [
  { label: '瑛泰产品', value: '瑛泰产品' }
]

const productCatalogQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'keyword',
    label: '关键词',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '产品、编码、持证人'
  },
  {
    key: 'categoryLevel1',
    label: '产品类别 I',
    type: 'text',
    queryParamKey: 'categoryLevel1',
    placeholder: '产品类别 I'
  },
  {
    key: 'categoryLevel2',
    label: '产品类别 II',
    type: 'text',
    queryParamKey: 'categoryLevel2',
    placeholder: '产品类别 II'
  },
  {
    key: 'productStatus',
    label: '产品状态',
    type: 'select',
    queryParamKey: 'productStatus',
    options: productStatusOptions
  },
  {
    key: 'dataSource',
    label: '数据来源',
    type: 'select',
    queryParamKey: 'dataSource',
    options: dataSourceOptions
  },
  {
    key: 'projectCodeNotBlank',
    label: '项目代码',
    type: 'select',
    queryParamKey: 'projectCodeNotBlank',
    options: [{ label: '不为空', value: true }]
  }
]

const productCatalogDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'dataSource', label: '数据来源', minWidth: 120 },
  { key: 'categoryLevel1', label: '产品类别 I', minWidth: 180 },
  { key: 'categoryLevel2', label: '产品类别 II', minWidth: 180 },
  { key: 'productSequence', label: '产品序号', minWidth: 100 },
  { key: 'product', label: '产品', minWidth: 220 },
  { key: 'productCode', label: '产品编码', minWidth: 120 },
  { key: 'projectName', label: '项目名称', minWidth: 180, sortable: 'custom' },
  { key: 'projectCode', label: '项目代码', minWidth: 120, sortable: 'custom' },
  { key: 'registrationCertificateName', label: '注册证名称', minWidth: 220 },
  { key: 'registrationCertificateNumber', label: '注册证号', minWidth: 180 },
  { key: 'certificateHolder', label: '持证人', minWidth: 160 },
  { key: 'registrationPlace', label: '注册地', minWidth: 120 },
  { key: 'effectiveDate', label: '生效日期', width: 120 },
  { key: 'expiryDate', label: '有效期至', width: 120 },
  { key: 'classification', label: '分类', minWidth: 120 },
  { key: 'productStatus', label: '产品状态', width: 120 },
  { key: 'registrationInfoLink', label: '注册证信息链接', minWidth: 150 },
  { key: 'remark', label: '备注', minWidth: 220 },
  { key: 'actions', label: '操作', width: 130, hideable: false, business: false }
]

const {
  columns: productCatalogColumns,
  saving: productCatalogColumnSaving,
  isColumnVisible: isProductCatalogColumnVisible,
  getColumnWidthString: getProductCatalogColumnWidthString,
  getColumnMinWidthString: getProductCatalogColumnMinWidthString,
  handleHeaderDragend: handleProductCatalogHeaderDragend,
  saveConfig: saveProductCatalogColumnConfig,
  resetConfig: resetProductCatalogColumnConfig
} = useUserTableColumns('dcc.productCatalog.main', productCatalogDefaultColumns)

type DccProductCatalogPageQuery = DccProductCatalogPageReqVO & {
  pageNo: number
  pageSize: number
}

type ProductCatalogSortChange = {
  prop?: string
  order?: 'ascending' | 'descending' | null
}

const productCatalogSortState = ref<ProductCatalogSortChange>({})
const PRODUCT_CATALOG_SERVER_SORT_FIELDS = new Set(['projectName', 'projectCode'])

const queryParams = reactive<DccProductCatalogPageQuery>({
  pageNo: 1,
  pageSize: 10,
  keyword: undefined,
  categoryLevel1: undefined,
  categoryLevel2: undefined,
  productStatus: undefined,
  dataSource: undefined,
  projectCodeNotBlank: undefined,
  sortField: undefined,
  sortOrder: undefined
})

const formData = ref<DccProductCatalogUpdateReqVO>({
  dataSource: '瑛泰产品',
  originalRowNo: 0,
  categoryLevel1: '',
  categoryLevel2: '',
  productSequence: '',
  product: '',
  productCode: '',
  projectName: '',
  projectCode: '',
  registrationCertificateName: '',
  registrationCertificateNumber: '',
  certificateHolder: '',
  registrationPlace: '',
  effectiveDate: '',
  expiryDate: '',
  classification: '',
  registrationInfoLink: '',
  productStatus: '',
  remark: ''
})

const formRules = reactive<FormRules>({
  dataSource: [{ required: true, message: '数据来源不能为空', trigger: 'change' }],
  product: [{ required: true, message: '产品不能为空', trigger: 'blur' }]
})

const resetFormData = () => {
  formData.value = {
    dataSource: '瑛泰产品',
    originalRowNo: 0,
    categoryLevel1: '',
    categoryLevel2: '',
    productSequence: '',
    product: '',
    productCode: '',
    projectName: '',
    projectCode: '',
    registrationCertificateName: '',
    registrationCertificateNumber: '',
    certificateHolder: '',
    registrationPlace: '',
    effectiveDate: '',
    expiryDate: '',
    classification: '',
    registrationInfoLink: '',
    productStatus: '',
    remark: ''
  }
  formRef.value?.resetFields()
}

const getList = async () => {
  loading.value = true
  try {
    const data = await getProductCatalogPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const productCatalogQuickFilter = useTableQuickFilter(
  'dcc.productCatalog.main',
  productCatalogQuickFilterDefinitions,
  queryParams,
  getList
)

const handleProductCatalogSortChange = ({ prop, order }: ProductCatalogSortChange) => {
  queryParams.pageNo = 1
  const sortField = prop || ''
  if (!order || !PRODUCT_CATALOG_SERVER_SORT_FIELDS.has(sortField)) {
    queryParams.sortField = undefined
    queryParams.sortOrder = undefined
    getList()
    return
  }
  queryParams.sortField = sortField
  queryParams.sortOrder = order === 'ascending' ? 'asc' : 'desc'
  getList()
}

const openForm = (type: 'create' | 'update', row?: DccProductCatalogRespVO) => {
  formVisible.value = true
  formType.value = type
  resetFormData()
  if (type === 'update' && row) {
    formData.value = {
      dataSource: row.dataSource,
      originalRowNo: row.originalRowNo,
      categoryLevel1: row.categoryLevel1 || '',
      categoryLevel2: row.categoryLevel2 || '',
      productSequence: row.productSequence || '',
      product: row.product || '',
      productCode: row.productCode || '',
      projectName: row.projectName || '',
      projectCode: row.projectCode || '',
      registrationCertificateName: row.registrationCertificateName || '',
      registrationCertificateNumber: row.registrationCertificateNumber || '',
      certificateHolder: row.certificateHolder || '',
      registrationPlace: row.registrationPlace || '',
      effectiveDate: row.effectiveDate || '',
      expiryDate: row.expiryDate || '',
      classification: row.classification || '',
      registrationInfoLink: row.registrationInfoLink || '',
      productStatus: row.productStatus || '',
      remark: row.remark || ''
    }
  }
}

const buildSavePayload = (): DccProductCatalogSaveReqVO => ({
  dataSource: formData.value.dataSource,
  categoryLevel1: formData.value.categoryLevel1,
  categoryLevel2: formData.value.categoryLevel2,
  productSequence: formData.value.productSequence,
  product: formData.value.product,
  productCode: formData.value.productCode,
  projectName: formData.value.projectName,
  projectCode: formData.value.projectCode,
  registrationCertificateName: formData.value.registrationCertificateName,
  registrationCertificateNumber: formData.value.registrationCertificateNumber,
  certificateHolder: formData.value.certificateHolder,
  registrationPlace: formData.value.registrationPlace,
  effectiveDate: formData.value.effectiveDate,
  expiryDate: formData.value.expiryDate,
  classification: formData.value.classification,
  registrationInfoLink: formData.value.registrationInfoLink,
  productStatus: formData.value.productStatus,
  remark: formData.value.remark
})

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) {
    return
  }
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await createProductCatalog(buildSavePayload())
      message.success('新增产品目录成功')
    } else {
      await updateProductCatalog({
        ...buildSavePayload(),
        originalRowNo: formData.value.originalRowNo
      })
      message.success('编辑产品目录成功')
    }
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (row: DccProductCatalogRespVO) => {
  try {
    await message.delConfirm(`确认删除产品目录“${row.product || row.productCode || row.originalRowNo}”吗？`)
  } catch {
    return
  }
  loading.value = true
  try {
    await deleteProductCatalog(row.dataSource, row.originalRowNo)
    message.success('删除产品目录成功')
    await getList()
  } finally {
    loading.value = false
  }
}

const formatProductStatus = (status?: string | null) => {
  const normalized = status?.trim()
  const labels: Record<string, string> = {
    N: '在研(N)',
    S: '在售(S)',
    C: '已取消(C)'
  }
  if (!normalized) {
    return '-'
  }
  return labels[normalized] || normalized
}

onMounted(async () => {
  await getList()
})
</script>

<style scoped>
@media (min-width: 1181px) {
  .dcc-product-catalog-list-template.unified-list-template--single-line-toolbar
    :deep(.unified-list-template__query-form) {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .dcc-product-catalog-list-template.unified-list-template--single-line-toolbar
    :deep(.unified-list-template__multi-filter) {
    min-width: 0;
  }

  .dcc-product-catalog-list-template.unified-list-template--single-line-toolbar
    :deep(.table-multi-filter),
  .dcc-product-catalog-list-template.unified-list-template--single-line-toolbar
    :deep(.table-multi-filter__tabs-empty) {
    min-width: 0;
  }
}

:deep(.dcc-product-catalog-resizable-table .el-table__header-wrapper th.el-table__cell) {
  position: relative;
}

:deep(.dcc-product-catalog-resizable-table .el-table__header-wrapper th.el-table__cell::after) {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 2;
  width: 8px;
  height: 100%;
  content: '';
  cursor: col-resize;
  border-right: 2px solid transparent;
}

:deep(.dcc-product-catalog-resizable-table .el-table__header-wrapper th.el-table__cell:hover::after) {
  border-right-color: #1677ff;
}

</style>
