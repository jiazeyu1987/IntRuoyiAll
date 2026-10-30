<!-- MES 工艺路线产品列表 -->
<template>
  <div class="route-product-list">
    <div class="route-product-list__actions">
      <el-button
        type="primary"
        link
        size="small"
        :disabled="productionConfigActionDisabled || loading || !!loadError"
        :title="productionConfigActionDisabled ? CANDIDATE_EDIT_REQUIRED_MESSAGE : ''"
        @click="openForm('create')"
      >
        <Icon icon="ep:plus" class="mr-5px" /> 添加产品
      </el-button>
      <el-button
        v-if="isEditable"
        type="primary"
        link
        size="small"
        :loading="submitting"
        :disabled="submitting || productionConfigActionDisabled"
        :title="productionConfigActionDisabled ? CANDIDATE_EDIT_REQUIRED_MESSAGE : ''"
        @click="emit('request-submit')"
      >
        保存
      </el-button>
    </div>
    <p v-if="productionConfigActionDisabled" class="route-product-list__edit-hint">
      当前为只读浏览，请点击修改后在草稿版本中维护关联产品。
    </p>
    <div v-loading="loading" class="route-product-list__content">
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
      <p v-else-if="!loading && list.length === 0" class="route-product-list__empty">
        暂无关联产品
      </p>
      <ul v-else class="route-product-list__codes" aria-label="关联产品编码">
        <li v-for="product in list" :key="product.itemId" class="route-product-list__row">
          <button
            type="button"
            class="route-product-list__code"
            @click="handleOpenItemDetail(product)"
          >
            {{ product.itemCode }}
          </button>
          <div class="route-product-list__row-actions">
            <el-button
              link
              type="danger"
              size="small"
              :disabled="productionConfigActionDisabled"
              :aria-label="'删除关联产品 ' + product.itemCode"
              :title="productionConfigActionDisabled ? CANDIDATE_EDIT_REQUIRED_MESSAGE : '删除关联产品'"
              @click="handleDelete(product)"
            >
              删除
            </el-button>
          </div>
        </li>
      </ul>
    </div>

    <!-- 表单弹窗：添加/修改 -->
    <ProductNumberSelector
      v-model="productSelectorVisible"
      :linked-item-ids="list.map((product) => product.itemId)"
      :submitting="batchSubmitting"
      @confirm="submitSelectedProducts"
    />
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
import { CommonStatusEnum } from '@/utils/constants'
import { ProRouteProductApi, type ProRouteProductVO } from '@/api/mes/pro/route/product'
import { ProRouteApi, type MesRouteId, type RouteVersionEditContext } from '@/api/mes/pro/route'
import { MdItemApi, type MdItemVO } from '@/api/mes/md/item'
import RouteProductBomList from './RouteProductBomList.vue'
import ProductNumberSelector from '@/components/MesProductSelector/ProductNumberSelector.vue'
import { isRouteConfirmCancel, resolveRouteOperationErrorMessage } from './routeError'
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
  if (!isEditable.value || !isDraftCandidateEdit.value) {
    throw new Error(`${actionName}失败：${CANDIDATE_EDIT_REQUIRED_MESSAGE}`)
  }
  return props.routeVersionEditContext!.routeVersionId
}

const checkProjectAssociation = async () => {
  try {
    const binding = await ProRouteApi.getRouteDccProjectBinding(props.routeId)
    if (!binding.bound || !binding.dccProjectCodeId) {
      message.warning('请先关联项目')
      return false
    }
    return true
  } catch (error) {
    message.error(resolveRouteOperationErrorMessage(error, '项目关联检查失败，请重试'))
    return false
  }
}
// ==================== 列表 ====================
const loading = ref(false) // 列表的加载中
const list = ref<ProRouteProductVO[]>([]) // 列表的数据
const loadError = ref('')

/** 查询列表 */
const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    list.value = await ProRouteProductApi.getRouteProductListByRoute(
      props.routeId,
      props.routeVersionEditContext?.routeVersionId
    )
  } catch (error) {
    loadError.value = resolveRouteOperationErrorMessage(error, '关联产品加载失败，请查看后端返回错误')
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
const productSelectorVisible = ref(false)
const batchSubmitting = ref(false)

const submitSelectedProducts = async (items: MdItemVO[]) => {
  if (batchSubmitting.value) return
  batchSubmitting.value = true
  let completed = 0
  try {
    const routeId = props.routeId
    const routeVersionId = requireCandidateRouteVersionId('产品批量关联')
    if (!(await checkProjectAssociation())) return
    list.value = await ProRouteProductApi.getRouteProductListByRoute(routeId, routeVersionId)
    const linkedIds = new Set(list.value.map((product) => product.itemId))
    const pending = [...new Map(items.map((item) => [item.id, item])).values()]
      .filter((item) => !linkedIds.has(item.id))
    // 候选版本保存会更新同一份快照，顺序提交以保留前一个编号的关联。
    for (const item of pending) {
      if (props.routeId !== routeId || requireCandidateRouteVersionId('产品批量关联') !== routeVersionId) {
        throw new Error('路线版本已切换，请重新打开编号选择器')
      }
      await ProRouteProductApi.createRouteProduct({ routeId, routeVersionId, itemId: item.id })
      completed++
      list.value.push({ routeId, routeVersionId, itemId: item.id, itemCode: item.code, itemName: item.name })
    }
    list.value = await ProRouteProductApi.getRouteProductListByRoute(routeId, routeVersionId)
    loadError.value = ''
    message.success(completed ? `已关联 ${completed} 个产品编号` : '所选产品编号均已关联')
    productSelectorVisible.value = false
  } catch (error) {
    message.error(`已关联 ${completed} 个编号；${resolveRouteOperationErrorMessage(error, '产品关联失败，请重试未完成的编号')}`)
  } finally {
    batchSubmitting.value = false
  }
}

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
const openForm = async (type: string, row?: ProRouteProductVO) => {
  requireCandidateRouteVersionId('产品绑定打开')
  if (!(await checkProjectAssociation())) return
  if (type === 'create') {
    productSelectorVisible.value = true
    return
  }
  formVisible.value = true
  formTitle.value = '编辑产品'
  formType2.value = type
  formData.value = { ...row, productCode: row?.itemCode }
  nextTick(() => formRef.value?.clearValidate())
}

const openCopyForm = async (row: ProRouteProductVO) => {
  const routeVersionId = requireCandidateRouteVersionId('产品复制打开')
  if (!(await checkProjectAssociation())) return
  copyFormVisible.value = true
  copyFormTitle.value = '复制产品'
  copySourceProductText.value = `${row.itemCode || ''} ${row.itemName || ''}`.trim()
  copyFormData.value = {
    routeVersionId,
    sourceRouteProductId: row.id,
    sourceItemId: row.itemId,
    targetItemId: undefined,
    targetProductCode: undefined,
    targetItemCode: undefined
  }
  nextTick(() => copyFormRef.value?.clearValidate())
}

const handleProductCommand = (command: string, row: ProRouteProductVO) => {
  requireCandidateRouteVersionId('产品维护')
  if (command === 'edit') {
    openForm('update', row)
  } else if (command === 'copy') {
    openCopyForm(row)
  }
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
  if (!(await checkProjectAssociation())) return
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
    await ProRouteProductApi.updateRouteProduct(payload)
    message.success(t('common.updateSuccess'))
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const submitCopyForm = async () => {
  const valid = await copyFormRef.value.validate()
  if (!valid) return
  if (!(await checkProjectAssociation())) return
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
    await message.delConfirm(`确认删除关联产品“${row.itemCode}”？`, '删除关联产品')
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
    productSelectorVisible.value = false
    if (routeId) {
      getList()
    }
  },
  { immediate: true }
)

</script>

<style scoped>
.route-product-list {
  width: 100%;
  min-width: 0;
}

.route-product-list__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.route-product-list__content {
  min-height: 48px;
}

.route-product-list__codes {
  margin: 0;
  padding: 0;
  list-style: none;
}

.route-product-list__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.route-product-list__row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.route-product-list__code {
  min-width: 0;
  padding: 0;
  color: var(--el-color-primary);
  font: inherit;
  line-height: 1.6;
  text-align: left;
  white-space: normal;
  overflow-wrap: anywhere;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.route-product-list__code:hover {
  text-decoration: underline;
}

.route-product-list__edit-hint {
  margin: 0 0 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.route-product-list__empty {
  margin: 0;
  padding: 12px 0;
  color: var(--el-text-color-secondary);
}

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
