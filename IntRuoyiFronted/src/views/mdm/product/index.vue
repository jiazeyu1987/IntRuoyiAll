<template>
  <ContentWrap class="scheme-d-basic-data-page scheme-d-basic-data-page--mdm-product">
    <div class="mb-16px flex items-center gap-8px">
      <span class="text-18px font-600 text-[var(--el-text-color-primary)]">基础数据 / 展厅主数据</span>
    </div>
    <UnifiedListTemplate
      table-key="mdm.product.main"
      :query-model="queryParams"
      label-width="86px"
      :filter-definitions="productQuickFilterDefinitions"
      :quick-filter-state="productQuickFilter.state"
      :selected-filter-definition="productQuickFilter.selectedDefinition.value"
      :operator-options="productQuickFilter.operatorOptions.value"
      :columns="productColumns"
      :column-saving="productColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="productQuickFilter.updateState"
      @quick-filter-query="handleQuery"
      @column-change="saveProductColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button
          class="scheme-d-btn scheme-d-btn--success"
          type="primary"
          @click="openForm('create')"
          v-hasPermi="['mdm:product:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增
        </el-button>
        <el-button
          class="scheme-d-btn scheme-d-btn--primary"
          @click="openImportDialog"
          v-hasPermi="['mdm:product:import']"
        >
          <Icon icon="ep:upload" class="mr-5px" />
          导入
        </el-button>
        <el-button
          class="scheme-d-btn scheme-d-btn--warning"
          :loading="exportLoading"
          @click="handleExport"
          v-hasPermi="['mdm:product:export']"
        >
          <Icon icon="ep:download" class="mr-5px" />
          导出
        </el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          class="mdm-product-resizable-table"
          data-user-table-column-explicit
          data-user-table-key="mdm.product.main"
          :data="list"
          border
          :allow-drag-last-column="true"
          :show-overflow-tooltip="true"
          @header-dragend="handleProductHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isProductColumnVisible('productCode')"
            label="产品编码"
            prop="productCode"
            :width="getProductColumnWidthString('productCode')"
            :min-width="getProductColumnMinWidthString('productCode', 150)"
            v-bind="sortColumnAttrs('productCode')"
          >
            <template #default="{ row }">
              <el-link :underline="false" type="primary" @click="openForm('update', row.id)">
                {{ row.productCode }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProductColumnVisible('nameCn')"
            label="中文名称"
            prop="nameCn"
            :width="getProductColumnWidthString('nameCn')"
            :min-width="getProductColumnMinWidthString('nameCn', 180)"
            v-bind="sortColumnAttrs('nameCn')"
          />
          <el-table-column
            v-if="isProductColumnVisible('nameEn')"
            label="英文名称"
            prop="nameEn"
            :width="getProductColumnWidthString('nameEn')"
            :min-width="getProductColumnMinWidthString('nameEn', 180)"
            v-bind="sortColumnAttrs('nameEn')"
          >
            <template #default="{ row }">{{ row.nameEn || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isProductColumnVisible('modelSpecification')"
            label="型号规格"
            prop="modelSpecification"
            :width="getProductColumnWidthString('modelSpecification')"
            :min-width="getProductColumnMinWidthString('modelSpecification', 170)"
            v-bind="sortColumnAttrs('modelSpecification')"
          >
            <template #default="{ row }">{{ row.modelSpecification || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isProductColumnVisible('category')"
            label="分类"
            prop="category"
            :width="getProductColumnWidthString('category')"
            :min-width="getProductColumnMinWidthString('category', 130)"
            v-bind="sortColumnAttrs('category')"
          >
            <template #default="{ row }">{{ row.category || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isProductColumnVisible('status')"
            label="状态"
            prop="status"
            :width="getProductColumnWidthString('status', 100)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="{ row }">
              <el-tag class="scheme-d-tag" :type="row.status === 'ENABLE' ? 'success' : 'info'">
                {{ formatStatus(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProductColumnVisible('updateTime')"
            label="更新时间"
            prop="updateTime"
            :width="getProductColumnWidthString('updateTime', 180)"
            :formatter="dateFormatter2"
            v-bind="sortColumnAttrs('updateTime')"
          />
          <el-table-column
            v-if="isProductColumnVisible('actions')"
            label="操作"
            prop="actions"
            fixed="right"
            :width="getProductColumnWidthString('actions', 320)"
          >
            <template #default="{ row }">
              <el-button
                link
                class="scheme-d-row-action scheme-d-row-action--primary"
                type="primary"
                @click="openForm('update', row.id)"
                v-hasPermi="['mdm:product:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                class="scheme-d-row-action scheme-d-row-action--primary"
                type="primary"
                @click="handleReferences(row)"
              >
                引用
              </el-button>
              <el-button
                link
                class="scheme-d-row-action scheme-d-row-action--primary"
                type="primary"
                @click="openLinkedProjectCodeManagement(row)"
              >
                项目代码
              </el-button>
              <el-button
                link
                class="scheme-d-row-action scheme-d-row-action--primary"
                type="primary"
                @click="openLinkedRegistrationCertificateManagement(row)"
              >
                注册证
              </el-button>
              <el-button
                link
                class="scheme-d-row-action"
                :class="row.status === 'ENABLE' ? 'scheme-d-row-action--warning' : 'scheme-d-row-action--success'"
                :type="row.status === 'ENABLE' ? 'warning' : 'success'"
                @click="handleStatusChange(row)"
                v-hasPermi="['mdm:product:update']"
              >
                {{ row.status === 'ENABLE' ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <el-dialog
    v-model="formVisible"
    class="scheme-d-form-control"
    :title="formType === 'create' ? '新增产品主数据' : '编辑产品主数据'"
    width="720px"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="118px">
      <el-form-item label="产品编码" prop="productCode">
        <el-input v-model="formData.productCode" maxlength="64" />
      </el-form-item>
      <el-form-item label="DCC产品编号" prop="dccProductCode">
        <el-input v-model="formData.dccProductCode" clearable maxlength="14" placeholder="14 位字母或数字，可为空" />
      </el-form-item>
      <el-form-item label="中文名称" prop="nameCn">
        <el-input v-model="formData.nameCn" maxlength="255" />
      </el-form-item>
      <el-form-item label="英文名称" prop="nameEn">
        <el-input v-model="formData.nameEn" clearable maxlength="255" />
      </el-form-item>
      <el-form-item label="型号规格" prop="modelSpecification">
        <el-input v-model="formData.modelSpecification" clearable maxlength="255" />
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-input v-model="formData.category" clearable maxlength="128" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio-button label="ENABLE">启用</el-radio-button>
          <el-radio-button label="DISABLE">停用</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="scheme-d-dialog-footer">
        <el-button class="scheme-d-btn scheme-d-btn--neutral" @click="formVisible = false">取消</el-button>
        <el-button
          class="scheme-d-btn scheme-d-btn--success"
          type="primary"
          :loading="formSubmitting"
          @click="submitForm"
        >
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="importVisible" class="scheme-d-form-control" title="产品主数据导入" width="1040px">
    <div class="product-master-import-toolbar">
      <el-upload
        v-model:file-list="importFileList"
        accept=".xlsx,.xls"
        :auto-upload="false"
        :limit="1"
        :on-change="handleImportFileChange"
        :on-remove="handleImportFileRemove"
      >
        <el-button class="scheme-d-btn scheme-d-btn--neutral">
          <Icon icon="ep:folder-opened" class="mr-5px" />
          选择文件
        </el-button>
      </el-upload>
      <el-button class="scheme-d-btn scheme-d-btn--warning" @click="handleDownloadTemplate">
        <Icon icon="ep:document" class="mr-5px" />
        模板
      </el-button>
      <el-button
        class="scheme-d-btn scheme-d-btn--primary"
        type="primary"
        :loading="importPreviewLoading"
        @click="handleImportPreview"
      >
        <Icon icon="ep:view" class="mr-5px" />
        预览差异
      </el-button>
      <el-button
        type="success"
        class="scheme-d-btn scheme-d-btn--success"
        :disabled="!importPreviewResult || importPreviewResult.failureCount > 0"
        :loading="importConfirmLoading"
        @click="handleImportConfirm"
      >
        <Icon icon="ep:circle-check" class="mr-5px" />
        确认全量导入
      </el-button>
    </div>
    <div v-if="importPreviewResult" class="product-master-summary">
      <el-tag class="scheme-d-tag">总数 {{ importPreviewResult.totalCount }}</el-tag>
      <el-tag class="scheme-d-tag" type="success">新增 {{ importPreviewResult.createCount }}</el-tag>
      <el-tag class="scheme-d-tag" type="warning">更新 {{ importPreviewResult.updateCount }}</el-tag>
      <el-tag class="scheme-d-tag" type="info">停用 {{ importPreviewResult.disableCount }}</el-tag>
      <el-tag class="scheme-d-tag">不变 {{ importPreviewResult.unchangedCount }}</el-tag>
      <el-tag class="scheme-d-tag" :type="importPreviewResult.failureCount > 0 ? 'danger' : 'success'">
        失败 {{ importPreviewResult.failureCount }}
      </el-tag>
    </div>
    <el-table
      v-if="importPreviewResult"
      :data="importPreviewResult.rows"
      :show-overflow-tooltip="true"
      height="460"
    >
      <el-table-column label="行号" prop="rowNo" width="80" />
      <el-table-column label="动作" prop="importAction" width="110">
        <template #default="{ row }">
          <el-tag class="scheme-d-tag" :type="importActionTagType(row.importAction)">
            {{ formatImportAction(row.importAction) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="产品编码" prop="productCode" min-width="150" />
      <el-table-column label="DCC产品编号" prop="dccProductCode" min-width="150" />
      <el-table-column label="中文名称" prop="nameCn" min-width="180" />
      <el-table-column label="失败原因" prop="failureReason" min-width="260">
        <template #default="{ row }">{{ row.failureReason || '-' }}</template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <el-dialog v-model="referenceVisible" class="scheme-d-form-control" title="引用情况" width="460px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="产品编码">{{ selectedReferenceProduct?.productCode }}</el-descriptions-item>
      <el-descriptions-item label="DCC引用数">{{ referenceData?.dccReferenceCount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="展厅引用数">{{ referenceData?.showroomReferenceCount ?? '-' }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import type { FormInstance, FormRules, UploadFile, UploadUserFile } from 'element-plus'
import download from '@/utils/download'
import { dateFormatter2 } from '@/utils/formatTime'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import { onActivated } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as ProductApi from '@/api/mdm/product'
import type {
  MdmProductImportPreviewRespVO,
  MdmProductReferenceRespVO,
  MdmProductRespVO,
  MdmProductSaveReqVO
} from '@/api/mdm/product'

defineOptions({ name: 'MdmProduct' })

const route = useRoute()
const router = useRouter()
const PRODUCT_ROUTE_PATH = '/mes/md/showroom-product'

const isProductRoute = () => route.path === PRODUCT_ROUTE_PATH

const productQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'keyword',
    label: '关键词',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '编码、名称、型号'
  },
  {
    key: 'productCode',
    label: '产品编码',
    type: 'text',
    queryParamKey: 'productCode',
    placeholder: '稳定产品编码'
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: [
      { label: '启用', value: ProductApi.MDM_PRODUCT_STATUS_ENABLE },
      { label: '停用', value: ProductApi.MDM_PRODUCT_STATUS_DISABLE }
    ]
  }
]

const productDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'productCode', label: '产品编码', minWidth: 150 },
  { key: 'nameCn', label: '中文名称', minWidth: 180 },
  { key: 'nameEn', label: '英文名称', minWidth: 180 },
  { key: 'modelSpecification', label: '型号规格', minWidth: 170 },
  { key: 'category', label: '分类', minWidth: 130 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'updateTime', label: '更新时间', width: 180 },
  { key: 'actions', label: '操作', width: 320, hideable: false, business: false }
]

const {
  columns: productColumns,
  saving: productColumnSaving,
  isColumnVisible: isProductColumnVisible,
  getColumnWidthString: getProductColumnWidthString,
  getColumnMinWidthString: getProductColumnMinWidthString,
  handleHeaderDragend: handleProductHeaderDragend,
  saveConfig: saveProductColumnConfig
} = useUserTableColumns('mdm.product.main', productDefaultColumns)

const message = useMessage()

const loading = ref(false)
const exportLoading = ref(false)
const total = ref(0)
const list = ref<MdmProductRespVO[]>([])
type MdmProductPageQuery = ProductApi.MdmProductPageReqVO & {
  pageNo: number
  pageSize: number
}

const queryParams = reactive<MdmProductPageQuery>({
  pageNo: 1,
  pageSize: 10,
  productMasterId: undefined,
  keyword: undefined,
  productCode: undefined,
  status: undefined
})

const formVisible = ref(false)
const formSubmitting = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref<FormInstance>()
const formData = reactive<MdmProductSaveReqVO>({
  productCode: '',
  dccProductCode: '',
  nameCn: '',
  nameEn: '',
  modelSpecification: '',
  category: '',
  status: ProductApi.MDM_PRODUCT_STATUS_ENABLE
})
const formRules: FormRules = {
  productCode: [{ required: true, message: '请输入产品编码', trigger: 'blur' }],
  nameCn: [{ required: true, message: '请输入中文名称', trigger: 'blur' }],
  dccProductCode: [
    {
      validator: (_rule, value: string, callback) => {
        const trimmed = value?.trim()
        if (!trimmed || /^[A-Za-z0-9]{14}$/.test(trimmed)) {
          callback()
          return
        }
        callback(new Error('DCC 产品编号必须为 14 位字母或数字'))
      },
      trigger: 'blur'
    }
  ]
}

const importVisible = ref(false)
const importPreviewLoading = ref(false)
const importConfirmLoading = ref(false)
const importFile = ref<File | null>(null)
const importFileList = ref<UploadUserFile[]>([])
const importPreviewResult = ref<MdmProductImportPreviewRespVO | null>(null)

const referenceVisible = ref(false)
const referenceData = ref<MdmProductReferenceRespVO | null>(null)
const selectedReferenceProduct = ref<MdmProductRespVO | null>(null)

const formatStatus = (status: string) => (status === ProductApi.MDM_PRODUCT_STATUS_ENABLE ? '启用' : '停用')
const formatImportAction = (action: string) =>
  ({
    CREATE: '新增',
    UPDATE: '更新',
    DISABLE: '停用',
    UNCHANGED: '不变',
    INVALID: '失败'
  })[action] || action
const importActionTagType = (action: string) =>
  ({
    CREATE: 'success',
    UPDATE: 'warning',
    DISABLE: 'info',
    INVALID: 'danger'
  })[action] || ''

const resolveRouteQueryText = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  if (rawValue === undefined || rawValue === null) {
    return undefined
  }
  const text = String(rawValue).trim()
  return /^[1-9]\d*$/.test(text) ? text : undefined
}

const syncProductQueryFromRoute = () => {
  queryParams.productMasterId = resolveRouteQueryText(route.query.productMasterId)
  if (queryParams.productMasterId) {
    queryParams.pageNo = 1
  }
}

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

const productQuickFilter = useTableQuickFilter(
  'mdm.product.main',
  productQuickFilterDefinitions,
  queryParams,
  getList
)
const productQuickFilterSearchForm = {
  fields: [{ prop: 'value' }]
}

const handleQuery = async (skipEmptyReset = false) => {
  if (
    skipEmptyReset !== true &&
    isSearchFormInputEmpty(productQuickFilterSearchForm, {
      value: productQuickFilter.state.value
    })
  ) {
    await productQuickFilter.resetQuickFilter()
    return
  }
  await productQuickFilter.applyQuickFilter()
}

const resetFormData = () => {
  Object.assign(formData, {
    id: undefined,
    productCode: '',
    dccProductCode: '',
    nameCn: '',
    nameEn: '',
    modelSpecification: '',
    category: '',
    status: ProductApi.MDM_PRODUCT_STATUS_ENABLE
  })
}

const openForm = async (type: 'create' | 'update', id?: number) => {
  resetFormData()
  formType.value = type
  if (type === 'update' && id) {
    Object.assign(formData, await ProductApi.getProduct(id))
  }
  formVisible.value = true
}

const trimNullable = (value: string | null | undefined) => {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

const submitForm = async () => {
  if (!(await formRef.value?.validate())) {
    return
  }
  const payload: MdmProductSaveReqVO = {
    id: formData.id,
    productCode: formData.productCode.trim(),
    dccProductCode: trimNullable(formData.dccProductCode),
    nameCn: formData.nameCn.trim(),
    nameEn: trimNullable(formData.nameEn),
    modelSpecification: trimNullable(formData.modelSpecification),
    category: trimNullable(formData.category),
    status: formData.status
  }
  formSubmitting.value = true
  try {
    if (formType.value === 'create') {
      await ProductApi.createProduct(payload)
    } else {
      await ProductApi.updateProduct(payload)
    }
    message.success('产品主数据已保存')
    formVisible.value = false
    await getList()
  } finally {
    formSubmitting.value = false
  }
}

const handleStatusChange = async (row: MdmProductRespVO) => {
  const nextStatus =
    row.status === ProductApi.MDM_PRODUCT_STATUS_ENABLE
      ? ProductApi.MDM_PRODUCT_STATUS_DISABLE
      : ProductApi.MDM_PRODUCT_STATUS_ENABLE
  await ProductApi.updateProductStatus(row.id, nextStatus)
  message.success(`产品已${formatStatus(nextStatus)}`)
  await getList()
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const data = await ProductApi.exportProductExcel(queryParams)
    download.excel(data, '产品主数据.xls')
  } finally {
    exportLoading.value = false
  }
}

const openImportDialog = () => {
  importVisible.value = true
  importFile.value = null
  importFileList.value = []
  importPreviewResult.value = null
}

const handleImportFileChange = (uploadFile: UploadFile) => {
  importFile.value = uploadFile.raw || null
  importPreviewResult.value = null
}

const handleImportFileRemove = () => {
  importFile.value = null
  importPreviewResult.value = null
}

const handleDownloadTemplate = async () => {
  const data = await ProductApi.getImportTemplate()
  download.excel(data, '产品主数据导入模板.xlsx')
}

const handleImportPreview = async () => {
  if (!importFile.value) {
    message.warning('请选择产品主数据 Excel')
    return
  }
  importPreviewLoading.value = true
  try {
    importPreviewResult.value = await ProductApi.importPreview(importFile.value)
    message.success('导入预览已生成')
  } finally {
    importPreviewLoading.value = false
  }
}

const handleImportConfirm = async () => {
  if (!importPreviewResult.value?.batchId) {
    message.warning('请先生成导入预览')
    return
  }
  importConfirmLoading.value = true
  try {
    importPreviewResult.value = await ProductApi.importConfirm(importPreviewResult.value.batchId)
    message.success('产品主数据已按预览结果更新')
    await getList()
  } finally {
    importConfirmLoading.value = false
  }
}

const handleReferences = async (row: MdmProductRespVO) => {
  selectedReferenceProduct.value = row
  referenceData.value = await ProductApi.getProductReferences(row.id)
  referenceVisible.value = true
}

const openLinkedProjectCodeManagement = (row: MdmProductRespVO) => {
  router.push({
    path: '/mes/md/dcc-project-code',
    query: { productMasterId: String(row.id) }
  })
}

const openLinkedRegistrationCertificateManagement = (row: MdmProductRespVO) => {
  router.push({
    path: '/mdm/registration-certificate',
    query: { productMasterId: String(row.id) }
  })
}

onMounted(() => {
  syncProductQueryFromRoute()
  void getList()
})

let productInitialActivationHandled = false

onActivated(async () => {
  if (!isProductRoute()) {
    return
  }
  if (!productInitialActivationHandled) {
    productInitialActivationHandled = true
    return
  }
  syncProductQueryFromRoute()
  await getList()
})

watch(
  () => [route.path, route.query.productMasterId],
  async () => {
    if (!isProductRoute()) {
      return
    }
    syncProductQueryFromRoute()
    await getList()
  }
)
</script>

<style scoped>
:deep(.mdm-product-resizable-table .el-table__header-wrapper th.el-table__cell) {
  position: relative;
}

:deep(.mdm-product-resizable-table .el-table__header-wrapper th.el-table__cell::after) {
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

:deep(.mdm-product-resizable-table .el-table__header-wrapper th.el-table__cell:hover::after) {
  border-right-color: #1677ff;
}

.product-master-import-toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}

.product-master-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
