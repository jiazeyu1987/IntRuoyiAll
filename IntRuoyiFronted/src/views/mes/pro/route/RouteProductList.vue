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
          row-key="itemId"
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
                @click="handleDelete(scope.row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>

    <!-- 表单弹窗：添加/修改 -->
    <Dialog :title="formTitle" v-model="formVisible" width="640px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="产品编号" prop="productCode">
          <el-autocomplete
            v-model="formData.productCode"
            :fetch-suggestions="queryRouteProductCodeSuggestions"
            value-key="code"
            placeholder="请输入产品名称或编号"
            clearable
            :trigger-on-focus="false"
            class="!w-1/1"
            @select="handleRouteProductCodeSelect"
            @input="handleRouteProductCodeInput"
          >
            <template #default="{ item }">
              <div
                class="route-product-suggestion"
                :class="
                  item.isLinked
                    ? 'route-product-suggestion--linked'
                    : 'route-product-suggestion--unlinked'
                "
              >
                <span class="route-product-suggestion__code">{{ item.code }}</span>
                <span class="route-product-suggestion__name">{{ item.name }}</span>
                <span class="route-product-suggestion__status">
                  {{ item.isLinked ? '已添加' : '未添加' }}
                </span>
              </div>
            </template>
          </el-autocomplete>
        </el-form-item>
      </el-form>
      <!-- 编辑时展示产品 BOM 配置 -->
      <template v-if="formType2 === 'update' && formData.id && formData.itemId">
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

    <Dialog :title="copyFormTitle" v-model="copyFormVisible" width="640px">
      <el-form ref="copyFormRef" :model="copyFormData" :rules="copyFormRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="源产品">
              <el-input :model-value="copySourceProductText" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标产品编号" prop="targetProductCode">
              <el-autocomplete
                v-model="copyFormData.targetProductCode"
                :fetch-suggestions="queryRouteProductCodeSuggestions"
                value-key="code"
                placeholder="请输入产品名称或编号"
                clearable
                :trigger-on-focus="false"
                class="!w-1/1"
                @select="handleCopyTargetProductCodeSelect"
                @input="handleCopyTargetProductCodeInput"
              >
                <template #default="{ item }">
                  <div
                    class="route-product-suggestion"
                    :class="
                      item.isLinked
                        ? 'route-product-suggestion--linked'
                        : 'route-product-suggestion--unlinked'
                    "
                  >
                    <span class="route-product-suggestion__code">{{ item.code }}</span>
                    <span class="route-product-suggestion__name">{{ item.name }}</span>
                    <span class="route-product-suggestion__status">
                      {{ item.isLinked ? '已添加' : '未添加' }}
                    </span>
                  </div>
                </template>
              </el-autocomplete>
            </el-form-item>
          </el-col>
        </el-row>
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
import { DICT_TYPE } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import { ProRouteProductApi, type ProRouteProductVO } from '@/api/mes/pro/route/product'
import type { MesRouteId, RouteVersionEditContext } from '@/api/mes/pro/route'
import { MdItemApi, type MdItemVO } from '@/api/mes/md/item'
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
// ==================== 列表 ====================
const loading = ref(false) // 列表的加载中
const list = ref<ProRouteProductVO[]>([]) // 列表的数据
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
    list.value = await ProRouteProductApi.getRouteProductListByRoute(
      props.routeId,
      props.routeVersionEditContext?.routeVersionId
    )
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
  productCode: [{ required: true, message: '产品编号不能为空', trigger: 'blur' }]
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
  targetProductCode: [{ required: true, message: '目标产品编号不能为空', trigger: 'blur' }]
})

type RouteProductSuggestion = MdItemVO & { value: string; isLinked: boolean }

const normalizeRouteProductCode = (value: unknown) => String(value ?? '').trim()
const linkedRouteProductIds = computed(
  () => new Set(list.value.map((product) => product.itemId).filter((itemId) => itemId != null))
)
const buildRouteProductSuggestion = (item: MdItemVO): RouteProductSuggestion => ({
  ...item,
  value: item.code,
  isLinked: linkedRouteProductIds.value.has(item.id)
})
const sortRouteProductSuggestions = (
  left: RouteProductSuggestion,
  right: RouteProductSuggestion
) =>
  Number(left.isLinked) - Number(right.isLinked) ||
  left.code.localeCompare(right.code, 'zh-CN', { numeric: true })

const queryRouteProductCodeSuggestions = async (
  queryString: string,
  callback: (items: RouteProductSuggestion[]) => void
) => {
  const keyword = normalizeRouteProductCode(queryString)
  if (!keyword) {
    callback([])
    return
  }
  try {
    const queryParams = {
      pageNo: 1,
      pageSize: 20,
      status: CommonStatusEnum.ENABLE
    }
    const [codeResult, nameResult] = await Promise.all([
      MdItemApi.getItemPage({ ...queryParams, code: keyword }),
      MdItemApi.getItemPage({ ...queryParams, name: keyword })
    ])
    const uniqueItems = new Map<number, MdItemVO>()
    for (const item of [...(codeResult?.list ?? []), ...(nameResult?.list ?? [])]) {
      uniqueItems.set(item.id, item)
    }
    callback([...uniqueItems.values()].map(buildRouteProductSuggestion).sort(sortRouteProductSuggestions))
  } catch (error) {
    callback([])
    message.error(resolveRouteOperationErrorMessage(error, '产品查询失败，请查看后端返回错误'))
  }
}

const applyRouteProductItemToForm = (item: MdItemVO) => {
  formData.value.itemId = item.id
  formData.value.productCode = item.code
  formData.value.itemCode = item.code
  formData.value.itemName = item.name
}

const applyCopyTargetProductItemToForm = (item: MdItemVO) => {
  copyFormData.value.targetItemId = item.id
  copyFormData.value.targetProductCode = item.code
  copyFormData.value.targetItemCode = item.code
}

const handleRouteProductCodeSelect = (item: RouteProductSuggestion) => {
  applyRouteProductItemToForm(item)
}

const handleCopyTargetProductCodeSelect = (item: RouteProductSuggestion) => {
  applyCopyTargetProductItemToForm(item)
}

const handleRouteProductCodeInput = () => {
  if (normalizeRouteProductCode(formData.value.productCode) !== formData.value.itemCode) {
    formData.value.itemId = undefined
    formData.value.itemName = undefined
  }
}

const handleCopyTargetProductCodeInput = () => {
  if (normalizeRouteProductCode(copyFormData.value.targetProductCode) !== copyFormData.value.targetItemCode) {
    copyFormData.value.targetItemId = undefined
  }
}

const resolveProductCode = async (rawCode: unknown, fieldName: string) => {
  const code = normalizeRouteProductCode(rawCode)
  if (!code) {
    throw new Error(fieldName + '不能为空')
  }
  const item = await MdItemApi.getItemByCode(code)
  if (!item?.id) {
    throw new Error('未找到' + fieldName + '：' + code)
  }
  if (item.status !== CommonStatusEnum.ENABLE) {
    throw new Error(fieldName + '已禁用：' + code)
  }
  if (item.code !== code) {
    throw new Error(fieldName + '解析结果不一致：' + code)
  }
  return item
}

const resolveRouteProductCodeForSubmit = async () => {
  const item = await resolveProductCode(formData.value.productCode, '产品编号')
  applyRouteProductItemToForm(item)
  return item
}

const resolveCopyTargetProductCodeForSubmit = async () => {
  const item = await resolveProductCode(copyFormData.value.targetProductCode, '目标产品编号')
  applyCopyTargetProductItemToForm(item)
  copyFormData.value.targetItemCode = item.code
  return item
}

const buildRouteProductSavePayload = (item: MdItemVO, routeVersionId: MesRouteId) => ({
  id: formData.value.id,
  routeId: formData.value.routeId ?? props.routeId,
  routeVersionId,
  itemId: item.id
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
      productCode: undefined,
      itemId: undefined
    }
  } else {
    formData.value = { ...row, productCode: row?.itemCode }
  }
  nextTick(() => formRef.value?.clearValidate())
}

const openCopyForm = (row: ProRouteProductVO) => {
  copyFormVisible.value = true
  copyFormTitle.value = '复制产品'
  copySourceProductText.value = `${row.itemCode || ''} ${row.itemName || ''}`.trim()
  copyFormData.value = {
    routeVersionId: requireCandidateRouteVersionId('产品复制打开'),
    sourceRouteProductId: row.id,
    sourceItemId: row.itemId,
    targetItemId: undefined,
    targetProductCode: undefined,
    targetItemCode: undefined
  }
  nextTick(() => copyFormRef.value?.clearValidate())
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
  let productItem: MdItemVO
  try {
    productItem = await resolveRouteProductCodeForSubmit()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '产品编号校验失败')
    return
  }
  formLoading.value = true
  try {
    const payload = buildRouteProductSavePayload(
      productItem,
      requireCandidateRouteVersionId('产品绑定保存')
    )
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
  let targetItem: MdItemVO
  try {
    targetItem = await resolveCopyTargetProductCodeForSubmit()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '目标产品编号校验失败')
    return
  }
  copyFormLoading.value = true
  try {
    const routeVersionId = requireCandidateRouteVersionId('产品复制保存')
    await ProRouteProductApi.copyCandidateRouteProduct({
      routeId: props.routeId,
      routeVersionId,
      sourceItemId: copyFormData.value.sourceItemId,
      targetItemId: targetItem.id
    })
    message.success('复制成功')
    copyFormVisible.value = false
    await getList()
  } finally {
    copyFormLoading.value = false
  }
}

/** 删除按钮操作 */
const handleDelete = async (row: ProRouteProductVO) => {
  try {
    await message.delConfirm()
    await ProRouteProductApi.deleteCandidateRouteProduct(
      props.routeId,
      row.itemId,
      requireCandidateRouteVersionId('产品删除')
    )
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
  () => [props.routeId, props.routeVersionEditContext?.routeVersionId] as const,
  ([routeId]) => {
    if (routeId) {
      getList()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.route-product-suggestion {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) minmax(180px, 1.4fr) 56px;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.route-product-suggestion--unlinked {
  color: var(--el-color-success);
}

.route-product-suggestion--linked {
  color: var(--el-color-danger);
}

.route-product-suggestion__code,
.route-product-suggestion__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-product-suggestion__status {
  text-align: right;
}
</style>
