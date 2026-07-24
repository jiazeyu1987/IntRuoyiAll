<!-- MES 工艺路线产品列表 -->
<template>
  <div class="route-product-list">
    <UnifiedListTemplate
      table-key="mes.pro.route.product"
      :query-model="routeProductQueryParams"
      label-width="88px"
      :filter-definitions="routeProductQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="routeProductQuickFilter.state"
      :selected-filter-definition="routeProductQuickFilter.selectedDefinition.value"
      :operator-options="routeProductQuickFilter.operatorOptions.value"
      :columns="routeProductColumns"
      :column-saving="routeProductColumnSaving"
      :show-column-reset="false"
      :total="filteredRouteProductList.length"
      v-model:page="routeProductQueryParams.pageNo"
      v-model:limit="routeProductQueryParams.pageSize"
      v-model:sort-state="routeProductSortState"
      @update:quick-filter-state="routeProductQuickFilter.updateState"
      @quick-filter-query="routeProductQuickFilter.applyQuickFilter"
      @column-change="saveRouteProductColumnConfig"
      @column-reset="resetRouteProductColumnConfig"
    >
      <template #actions>
        <el-button
          v-if="isEditable"
          type="primary"
          plain
          :disabled="productionConfigActionDisabled"
          :title="productionConfigActionDisabled ? CANDIDATE_EDIT_REQUIRED_MESSAGE : ''"
          @click="openForm('create')"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 关联产品
        </el-button>
        <el-button
          v-if="canBindFromWorkOrders"
          type="primary"
          plain
          :loading="bindFromWorkOrdersLoading"
          :disabled="productionConfigActionDisabled"
          :title="productionConfigActionDisabled ? CANDIDATE_EDIT_REQUIRED_MESSAGE : ''"
          @click="handleBindFromWorkOrders"
        >
          <Icon icon="ep:connection" class="mr-5px" /> 补齐产品
        </el-button>
        <el-button
          v-if="isEditable"
          type="primary"
          :disabled="submitting || productionConfigActionDisabled"
          :title="productionConfigActionDisabled ? CANDIDATE_EDIT_REQUIRED_MESSAGE : ''"
          @click="emit('request-submit')"
        >
          <Icon icon="ep:check" class="mr-5px" /> 保存
        </el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          class="route-product-list__table"
          data-user-table-column-explicit
          data-user-table-key="mes.pro.route.product"
          :data="pagedRouteProductList"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          row-key="id"
          @header-dragend="handleRouteProductHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isRouteProductColumnVisible('itemCode')"
            label="产品物料编码"
            align="center"
            prop="itemCode"
            :width="getRouteProductColumnWidthString('itemCode', 150)"
            v-bind="sortColumnAttrs('itemCode')"
          >
            <template #default="scope">
              <el-link type="primary" @click="handleOpenItemDetail(scope.row)">
                {{ scope.row.itemCode }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRouteProductColumnVisible('itemName')"
            label="产品物料名称"
            align="center"
            prop="itemName"
            :width="getRouteProductColumnWidthString('itemName', 150)"
            v-bind="sortColumnAttrs('itemName')"
          />
          <el-table-column
            v-if="isRouteProductColumnVisible('specification')"
            label="规格型号"
            align="center"
            prop="specification"
            :width="getRouteProductColumnWidthString('specification', 150)"
            v-bind="sortColumnAttrs('specification')"
          />
          <el-table-column
            v-if="isRouteProductColumnVisible('unitName')"
            label="单位"
            align="center"
            prop="unitName"
            :width="getRouteProductColumnWidthString('unitName', 80)"
            v-bind="sortColumnAttrs('unitName')"
          />
          <el-table-column
            v-if="isRouteProductColumnVisible('quantity')"
            label="生产数量"
            align="center"
            prop="quantity"
            :width="getRouteProductColumnWidthString('quantity', 100)"
            v-bind="sortColumnAttrs('quantity')"
          />
          <el-table-column
            v-if="isRouteProductColumnVisible('productionTime')"
            label="生产用时"
            align="center"
            prop="productionTime"
            :width="getRouteProductColumnWidthString('productionTime', 120)"
            v-bind="sortColumnAttrs('productionTime')"
          >
            <template #default="scope">
              <span v-if="scope.row.productionTime">
                {{ scope.row.productionTime }}
                <dict-tag :type="DICT_TYPE.MES_TIME_UNIT_TYPE" :value="scope.row.timeUnitType" />
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRouteProductColumnVisible('remark')"
            label="备注"
            align="center"
            prop="remark"
            :min-width="getRouteProductColumnMinWidthString('remark', 120)"
            v-bind="sortColumnAttrs('remark')"
          />
          <el-table-column
            v-if="isEditable && isRouteProductColumnVisible('operation')"
            label="操作"
            align="center"
            prop="operation"
            :width="getRouteProductColumnWidthString('operation', 170)"
            fixed="right"
          >
            <template #default="scope">
              <el-button
                link
                type="primary"
                :disabled="productionConfigActionDisabled"
                @click="openForm('update', scope.row)"
              >
                编辑
              </el-button>
              <el-button
                link
                type="primary"
                :disabled="productionConfigActionDisabled"
                @click="openCopyForm(scope.row)"
              >
                复制
              </el-button>
              <el-button
                link
                type="danger"
                :disabled="productionConfigActionDisabled"
                @click="handleDelete(scope.row.id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>

    <!-- 表单弹窗：添加/修改 -->
    <Dialog :title="formTitle" v-model="formVisible" width="960px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="产品" prop="itemId">
              <MdItemSelect v-model="formData.itemId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生产数量" prop="quantity">
              <el-input-number
                v-model="formData.quantity"
                :min="1"
                controls-position="right"
                class="!w-1/1"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生产用时" prop="productionTime">
              <el-input-number
                v-model="formData.productionTime"
                :min="0"
                :precision="2"
                controls-position="right"
                class="!w-1/1"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时间单位" prop="timeUnitType">
              <el-select v-model="formData.timeUnitType" placeholder="请选择" class="!w-1/1">
                <el-option
                  v-for="dict in getStrDictOptions(DICT_TYPE.MES_TIME_UNIT_TYPE)"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <!-- 编辑时展示产品 BOM 配置 -->
      <template v-if="formType2 === 'update' && formData.id">
        <el-divider content-position="left">产品 BOM 配置</el-divider>
        <RouteProductBomList
          :routeId="routeId"
          :productId="formData.itemId"
          :productName="formData.itemName"
          :route-version-edit-context="routeVersionEditContext"
        />
      </template>
      <template #footer>
        <el-button type="primary" @click="submitForm" :disabled="formLoading">确 定</el-button>
        <el-button @click="formVisible = false">取 消</el-button>
      </template>
    </Dialog>

    <Dialog :title="copyFormTitle" v-model="copyFormVisible" width="960px">
      <el-form ref="copyFormRef" :model="copyFormData" :rules="copyFormRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="源产品">
              <el-input :model-value="copySourceProductText" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标产品" prop="targetItemId">
              <MdItemSelect v-model="copyFormData.targetItemId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生产数量" prop="quantity">
              <el-input-number
                v-model="copyFormData.quantity"
                :min="1"
                controls-position="right"
                class="!w-1/1"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生产用时" prop="productionTime">
              <el-input-number
                v-model="copyFormData.productionTime"
                :min="0"
                :precision="2"
                controls-position="right"
                class="!w-1/1"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时间单位" prop="timeUnitType">
              <el-select v-model="copyFormData.timeUnitType" placeholder="请选择" class="!w-1/1">
                <el-option
                  v-for="dict in getStrDictOptions(DICT_TYPE.MES_TIME_UNIT_TYPE)"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="copyFormData.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitCopyForm" :disabled="copyFormLoading">
          确 定
        </el-button>
        <el-button @click="copyFormVisible = false">取 消</el-button>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { getStrDictOptions, DICT_TYPE } from '@/utils/dict'
import {
  ProRouteProductApi,
  type ProRouteProductVO,
  type ProRouteProductBindFromWorkOrdersRespVO
} from '@/api/mes/pro/route/product'
import type { RouteVersionEditContext } from '@/api/mes/pro/route'
import MdItemSelect from '@/views/mes/md/item/components/MdItemSelect.vue'
import RouteProductBomList from './RouteProductBomList.vue'
import { isRouteConfirmCancel, resolveRouteOperationErrorMessage } from './routeError'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'RouteProductList' })

const props = defineProps<{
  routeId: number
  formType: string
  submitting?: boolean
  routeVersionEditContext?: RouteVersionEditContext
}>()
const emit = defineEmits<{
  'request-submit': []
}>()

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗
const { push } = useRouter()

const isEditable = computed(() => ['create', 'update'].includes(props.formType)) // 是否为编辑模式
const CANDIDATE_EDIT_REQUIRED_MESSAGE = '请先创建候选版本，在候选版本中编辑生产配置。'
const isDraftCandidateEdit = computed(
  () =>
    Boolean(props.routeVersionEditContext?.routeVersionId) &&
    props.routeVersionEditContext?.lifecycleStatus === 'DRAFT'
)
const productionConfigActionDisabled = computed(
  () => !isEditable.value || !isDraftCandidateEdit.value
)
const requireCandidateRouteVersionId = (actionName: string) => {
  if (!isDraftCandidateEdit.value) {
    throw new Error(`${actionName}失败：${CANDIDATE_EDIT_REQUIRED_MESSAGE}`)
  }
  return props.routeVersionEditContext!.routeVersionId
}
const canBindFromWorkOrders = computed(
  () => ['detail', 'update'].includes(props.formType) && !!props.routeId
)

// ==================== 列表 ====================
const loading = ref(false) // 列表的加载中
const list = ref<ProRouteProductVO[]>([]) // 列表的数据
const bindFromWorkOrdersLoading = ref(false)
const routeProductTableKey = 'mes.pro.route.product'
const routeProductDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'itemCode', label: '产品物料编码', width: 150, hideable: false },
  { key: 'itemName', label: '产品物料名称', width: 150 },
  { key: 'specification', label: '规格型号', width: 150 },
  { key: 'unitName', label: '单位', width: 80 },
  { key: 'quantity', label: '生产数量', width: 100 },
  { key: 'productionTime', label: '生产用时', width: 120 },
  { key: 'remark', label: '备注', minWidth: 120 },
  { key: 'operation', label: '操作', width: 170, hideable: false, business: false, sortable: false }
]
const {
  columns: routeProductColumns,
  saving: routeProductColumnSaving,
  isColumnVisible: isRouteProductColumnVisible,
  getColumnWidthString: getRouteProductColumnWidthString,
  getColumnMinWidthString: getRouteProductColumnMinWidthString,
  handleHeaderDragend: handleRouteProductHeaderDragend,
  saveConfig: saveRouteProductColumnConfig,
  resetConfig: resetRouteProductColumnConfig
} = useUserTableColumns(routeProductTableKey, routeProductDefaultColumns)
const routeProductQueryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  quickFilter: undefined as TableQuickFilterValue | undefined
})
const routeProductQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  { key: 'itemCode', label: '产品物料编码', type: 'text', placeholder: '请输入物料编码' },
  { key: 'itemName', label: '产品物料名称', type: 'text', placeholder: '请输入物料名称' },
  { key: 'specification', label: '规格型号', type: 'text', placeholder: '请输入规格型号' },
  { key: 'remark', label: '备注', type: 'text', placeholder: '请输入备注' }
])
const routeProductQuickFilter = useTableQuickFilter(
  routeProductTableKey,
  routeProductQuickFilterDefinitions,
  routeProductQueryParams,
  () => undefined
)
const routeProductSortState = ref<{
  key?: string
  prop?: string
  order?: 'ascending' | 'descending' | null
}>({})
const normalizeRouteProductText = (value: unknown) => String(value ?? '').trim().toLowerCase()
const matchesRouteProductQuickFilter = (row: ProRouteProductVO) => {
  const quickFilter = routeProductQueryParams.quickFilter
  if (!quickFilter?.fieldKey) return true
  const rowValue = normalizeRouteProductText(row[quickFilter.fieldKey as keyof ProRouteProductVO])
  const filterValue = normalizeRouteProductText(quickFilter.value)
  if (!filterValue) return true
  if (quickFilter.operator === 'eq') {
    return rowValue === filterValue
  }
  return rowValue.includes(filterValue)
}
const compareRouteProductValue = (left: unknown, right: unknown) => {
  const leftNumber = Number(left)
  const rightNumber = Number(right)
  const leftText = String(left ?? '').trim()
  const rightText = String(right ?? '').trim()
  if (leftText && rightText && Number.isFinite(leftNumber) && Number.isFinite(rightNumber)) {
    return leftNumber - rightNumber
  }
  return leftText.localeCompare(rightText, 'zh-CN', { numeric: true })
}
const filteredRouteProductList = computed(() => list.value.filter(matchesRouteProductQuickFilter))
const sortedRouteProductList = computed(() => {
  const sortProp = routeProductSortState.value.prop || routeProductSortState.value.key
  const sortOrder = routeProductSortState.value.order
  if (!sortProp || !sortOrder) return filteredRouteProductList.value
  const direction = sortOrder === 'ascending' ? 1 : -1
  return [...filteredRouteProductList.value].sort((left, right) => {
    const leftValue = left[sortProp as keyof ProRouteProductVO]
    const rightValue = right[sortProp as keyof ProRouteProductVO]
    return compareRouteProductValue(leftValue, rightValue) * direction
  })
})
const pagedRouteProductList = computed(() => {
  const pageNo = Math.max(1, routeProductQueryParams.pageNo || 1)
  const pageSize = Math.max(1, routeProductQueryParams.pageSize || 10)
  const start = (pageNo - 1) * pageSize
  return sortedRouteProductList.value.slice(start, start + pageSize)
})

watch(
  () => filteredRouteProductList.value.length,
  (total) => {
    const pageSize = Math.max(1, routeProductQueryParams.pageSize || 10)
    if (routeProductQueryParams.pageNo > 1 && (routeProductQueryParams.pageNo - 1) * pageSize >= total) {
      routeProductQueryParams.pageNo = 1
    }
  }
)

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    list.value = await ProRouteProductApi.getRouteProductListByRoute(props.routeId)
    routeProductQueryParams.pageNo = 1
  } finally {
    loading.value = false
  }
}

// ==================== 添加/编辑表单 ====================
const formVisible = ref(false) // 表单弹窗的是否展示
const formTitle = ref('') // 表单弹窗的标题
const formLoading = ref(false) // 表单的加载中
const formType2 = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref<any>({}) // 表单数据
const formRules = reactive({
  itemId: [{ required: true, message: '产品不能为空', trigger: 'change' }]
})
const formRef = ref() // 表单 Ref

// ==================== 复制表单 ====================
const copyFormVisible = ref(false)
const copyFormTitle = ref('复制产品')
const copyFormLoading = ref(false)
const copyFormRef = ref()
const copySourceProductText = ref('')
const copyFormData = ref<any>({})
const copyFormRules = reactive({
  targetItemId: [{ required: true, message: '目标产品不能为空', trigger: 'change' }]
})

/** 添加/修改操作 */
const openForm = (type: string, row?: ProRouteProductVO) => {
  requireCandidateRouteVersionId('产品绑定打开')
  formVisible.value = true
  formTitle.value = type === 'create' ? '关联产品' : '编辑产品'
  formType2.value = type
  if (type === 'create') {
    formData.value = {
      routeId: props.routeId,
      quantity: 1,
      productionTime: 1,
      timeUnitType: 'MINUTE'
    }
  } else {
    formData.value = { ...row }
  }
  formRef.value?.resetFields()
}

const openCopyForm = (row: ProRouteProductVO) => {
  if (!row.id) {
    throw new Error('复制关联产品失败：缺少源关联产品编号')
  }
  copyFormVisible.value = true
  copyFormTitle.value = '复制产品'
  copySourceProductText.value = `${row.itemCode || ''} ${row.itemName || ''}`.trim()
  copyFormData.value = {
    routeVersionId: requireCandidateRouteVersionId('产品复制打开'),
    sourceRouteProductId: row.id,
    targetItemId: undefined,
    quantity: row.quantity,
    productionTime: row.productionTime,
    timeUnitType: row.timeUnitType,
    remark: row.remark
  }
  copyFormRef.value?.resetFields()
}

/** 打开关联物料详情页 */
const handleOpenItemDetail = async (row: ProRouteProductVO) => {
  await push({
    name: 'MesMdItem',
    query: {
      code: row.itemCode,
      openDetailId: String(row.itemId)
    }
  })
}

/** 提交表单 */
const submitForm = async () => {
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const payload = {
      ...formData.value,
      routeVersionId: requireCandidateRouteVersionId('产品绑定保存')
    }
    if (formType2.value === 'create') {
      await ProRouteProductApi.createRouteProduct(payload)
      message.success(t('common.createSuccess'))
    } else {
      await ProRouteProductApi.updateRouteProduct(payload)
      message.success(t('common.updateSuccess'))
    }
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const submitCopyForm = async () => {
  const valid = await copyFormRef.value.validate()
  if (!valid) return
  copyFormLoading.value = true
  try {
    await ProRouteProductApi.copyRouteProduct({
      ...copyFormData.value,
      routeVersionId: requireCandidateRouteVersionId('产品复制保存')
    })
    message.success('复制成功')
    copyFormVisible.value = false
    await getList()
  } finally {
    copyFormLoading.value = false
  }
}

const buildBindFromWorkOrdersPreviewMessage = (
  preview: ProRouteProductBindFromWorkOrdersRespVO
) => {
  return `将按当前工艺路线名称，从生产工单中补齐同名产品编号。\n新增 ${preview.createdCount} 个，跳过 ${preview.existingCount} 个，冲突 ${preview.conflictCount} 个。是否继续？`
}

const confirmBindFromWorkOrdersPreview = async (
  preview: ProRouteProductBindFromWorkOrdersRespVO
) => {
  await message.confirm(buildBindFromWorkOrdersPreviewMessage(preview), '产品补齐预览')
}

const handleBindFromWorkOrders = async () => {
  bindFromWorkOrdersLoading.value = true
  try {
    const routeVersionId = requireCandidateRouteVersionId('产品补齐保存')
    const preview = await ProRouteProductApi.previewBindFromWorkOrders({
      routeId: props.routeId,
      routeVersionId: requireCandidateRouteVersionId('产品补齐保存')
    })
    await confirmBindFromWorkOrdersPreview(preview)
    const result = await ProRouteProductApi.bindFromWorkOrders({ routeId: props.routeId, routeVersionId })
    message.success(
      `补齐完成：新增 ${result.createdCount} 个，跳过 ${result.existingCount} 个，冲突 ${result.conflictCount} 个`
    )
    await getList()
  } finally {
    bindFromWorkOrdersLoading.value = false
  }
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await ProRouteProductApi.deleteRouteProduct(id, requireCandidateRouteVersionId('产品删除'))
    message.success(t('common.delSuccess'))
    await getList()
  } catch (error) {
    if (isRouteConfirmCancel(error)) {
      return
    }
    message.error(resolveRouteOperationErrorMessage(error, '删除关联产品失败，请查看后端返回错误'))
  }
}

/** 监听路线编号变化 */
watch(
  () => props.routeId,
  (val) => {
    if (val) {
      getList()
    }
  },
  { immediate: true }
)
</script>
