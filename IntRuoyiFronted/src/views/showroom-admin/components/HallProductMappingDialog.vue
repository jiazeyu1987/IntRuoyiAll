<template>
  <el-dialog
    v-model="dialogVisible"
    class="showroom-hall-mapping-dialog"
    destroy-on-close
    title="维护展项"
    width="920px"
  >
    <div v-if="hallRecord" class="showroom-hall-mapping-dialog__body">
      <el-alert
        v-if="productOptionsLoadError"
        :closable="false"
        show-icon
        type="error"
        :title="productOptionsLoadError"
      />

      <div class="showroom-hall-mapping-dialog__toolbar">
        <div>
          <strong>{{ hallRecord.name }}</strong>
          <span class="showroom-hall-mapping-dialog__code">{{ hallRecord.hallCode }}</span>
          <span class="showroom-hall-mapping-dialog__code">
            已选展项 {{ selectedProductIds.length }}
          </span>
        </div>
      </div>

      <el-form v-loading="productOptionsLoading" label-position="top">
        <el-form-item label="选择当前展柜包含的展项">
          <el-select
            v-model="selectedProductIds"
            class="showroom-hall-mapping-dialog__selector"
            collapse-tags
            collapse-tags-tooltip
            :disabled="productOptionsLoading || Boolean(productOptionsLoadError)"
            filterable
            multiple
            placeholder="选择当前展柜包含的展项"
          >
            <el-option
              v-for="option in productOptions"
              :key="option.itemKey"
              :label="`${option.itemType === 'AWARD' ? '奖项' : '产品'} · ${option.productCode} · ${option.nameCn} · V${option.revisionNo}`"
              :value="option.itemKey"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table
        :data="selectedProducts"
        border
        class="showroom-hall-mapping-dialog__table"
        row-key="itemKey"
        empty-text="请先选择展项"
      >
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.itemType === 'AWARD' ? 'warning' : 'primary'" effect="plain">
              {{ row.itemType === 'AWARD' ? '奖项' : '产品' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="编码" min-width="160" prop="productCode" show-overflow-tooltip />
        <el-table-column label="中文名称" min-width="240" prop="nameCn" show-overflow-tooltip />
        <el-table-column label="版本" width="100">
          <template #default="{ row }">V{{ row.revisionNo }}</template>
        </el-table-column>
        <el-table-column label="当前资料状态" width="140">
          <template #default="{ row }">
            <el-tag :type="row.incomplete ? 'warning' : 'success'">
              {{ row.incomplete ? '资料未完善' : '资料完整' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              link
              type="danger"
              :disabled="selectedProductIds.length <= 1"
              @click="handleRemoveProduct(row.itemKey)"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
      <el-button
        type="primary"
        :disabled="productOptionsLoading || Boolean(productOptionsLoadError)"
        :loading="saving"
        @click="handleSaveMapping"
      >
        保存展项
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ShowroomAdminApi } from '@/api/showroom-admin'
import {
  buildHallMappingPayload,
  createSelectedHallProductIds,
  normalizeHallRecord,
  normalizeHallProductCandidateOptions,
  normalizeProductOptions,
  type HallProductOption
} from '@/views/showroom-admin/hall/contracts'

defineOptions({ name: 'HallProductMappingDialog' })

const props = defineProps<{
  modelValue: boolean
  hall?: unknown | null
  products?: unknown[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [payload: { hallId: number }]
}>()

const message = useMessage()
const saving = ref(false)
const productOptionsLoading = ref(false)
const productOptionsLoadError = ref('')
const productOptions = ref<HallProductOption[]>([])
const selectedProductIds = ref<string[]>([])

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const hallRecord = computed(() => (props.hall ? normalizeHallRecord(props.hall) : null))
const productOptionMap = computed(() => {
  return new Map<string, HallProductOption>(
    productOptions.value.map((option) => [option.itemKey, option])
  )
})
const selectedProducts = computed(() => {
  if (productOptionsLoading.value || productOptionsLoadError.value) {
    return []
  }
  return selectedProductIds.value.map((itemKey) => {
    return productOptionMap.value.get(itemKey)
  }).filter((option): option is HallProductOption => Boolean(option))
})

const mergeProductOptions = (target: Map<string, HallProductOption>, source: HallProductOption[]) => {
  source.forEach((option) => {
    target.set(option.itemKey, option)
  })
}

const loadAllProductOptions = async () => {
  productOptionsLoading.value = true
  productOptionsLoadError.value = ''
  try {
    const merged = new Map<string, HallProductOption>()
    mergeProductOptions(merged, normalizeProductOptions(props.products || []))
    mergeProductOptions(
      merged,
      normalizeHallProductCandidateOptions(await ShowroomAdminApi.getHallItemOptions())
    )

    const missingItemKey = selectedProductIds.value.find((itemKey) => !merged.has(itemKey))
    if (missingItemKey !== undefined) {
      throw new Error(`展柜映射展项不存在于完整候选列表：${missingItemKey}`)
    }

    productOptions.value = Array.from(merged.values())
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    productOptions.value = []
    productOptionsLoadError.value = resolved.message
    message.error(resolved.message)
  } finally {
    productOptionsLoading.value = false
  }
}

watch(
  () => [props.modelValue, props.hall] as const,
  ([visible]) => {
    if (!visible || !hallRecord.value) {
      return
    }
    selectedProductIds.value = createSelectedHallProductIds(hallRecord.value)
    void loadAllProductOptions()
  },
  { immediate: true }
)

const handleRemoveProduct = (itemKey: string) => {
  if (selectedProductIds.value.length <= 1) {
    message.warning('至少保留一条展项映射')
    return
  }
  selectedProductIds.value = selectedProductIds.value.filter((currentKey) => currentKey !== itemKey)
}

const handleSaveMapping = async () => {
  saving.value = true
  try {
    if (!hallRecord.value) {
      throw new Error('维护展项入口缺少 hall 数据')
    }
    const payload = buildHallMappingPayload(hallRecord.value, selectedProductIds.value)
    await ShowroomAdminApi.updateHallProductMapping(payload)
    message.success('展柜展项已保存')
    emit('saved', { hallId: hallRecord.value.hallId })
    dialogVisible.value = false
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    message.error(resolved.message)
    throw resolved
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.showroom-hall-mapping-dialog__body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.showroom-hall-mapping-dialog__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-hall-mapping-dialog__code {
  margin-left: 8px;
  color: #4b5563;
}

.showroom-hall-mapping-dialog__selector {
  width: 100%;
}

.showroom-hall-mapping-dialog__table :deep(.el-table__header th) {
  background: #f7f9fc;
}
</style>
