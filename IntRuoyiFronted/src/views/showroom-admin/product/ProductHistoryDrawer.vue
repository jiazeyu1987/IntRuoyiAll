<template>
  <el-drawer
    v-model="drawerVisible"
    class="showroom-product-history-drawer"
    destroy-on-close
    size="960px"
    title="产品版本历史"
  >
    <div v-loading="loading" class="showroom-product-history-drawer__body">
      <el-alert
        v-if="loadError"
        :closable="false"
        show-icon
        type="error"
        :title="loadError"
      />

      <div v-else-if="historyRows.length === 0" class="showroom-product-history-drawer__empty">
        暂无版本历史
      </div>

      <div
        v-for="history in historyRows"
        v-else
        :key="history.revisionId"
        class="showroom-product-history-drawer__card"
      >
        <div class="showroom-product-history-drawer__header">
          <div class="showroom-product-history-drawer__headline">
            <strong>版本 V{{ history.revisionNo }}</strong>
            <el-tag :type="resolveProductStatusTagType(history.status)">
              {{ resolveProductStatusText(history.status) }}
            </el-tag>
          </div>
          <span class="showroom-product-history-drawer__count">
            {{ history.diffItems.length }} 项差异
          </span>
        </div>

        <el-table
          :data="history.diffItems"
          border
          class="showroom-product-history-drawer__table"
          row-key="fieldCode"
        >
          <el-table-column label="差异字段" min-width="140" prop="label" show-overflow-tooltip />
          <el-table-column label="字段编码" width="180" prop="fieldCode" show-overflow-tooltip />
          <el-table-column label="旧值" min-width="220" prop="oldValue" show-overflow-tooltip />
          <el-table-column label="新值" min-width="220" prop="newValue" show-overflow-tooltip />
          <el-table-column label="操作人ID" width="120" prop="operatorId" />
          <el-table-column label="操作动作" width="140" prop="operatorAction" show-overflow-tooltip />
          <el-table-column label="时间" width="180" prop="createdAt" :formatter="dateTimeValueFormatter" show-overflow-tooltip />
        </el-table>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ShowroomAdminApi } from '@/api/showroom-admin'
import { dateTimeValueFormatter } from '@/utils/formatTime'
import {
  normalizeProductHistory,
  resolveProductStatusTagType,
  resolveProductStatusText,
  type ProductVersionHistory
} from './contracts'

defineOptions({ name: 'ShowroomProductHistoryDrawer' })

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    productId?: number | null
  }>(),
  {
    productId: undefined
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const loading = ref(false)
const loadError = ref('')
const historyRows = ref<ProductVersionHistory[]>([])

const drawerVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const requireProductId = () => {
  if (!props.productId) {
    throw new Error('产品历史入口缺少 productId')
  }
  return props.productId
}

const loadHistory = async () => {
  loading.value = true
  loadError.value = ''
  try {
    historyRows.value = normalizeProductHistory(
      await ShowroomAdminApi.getProductHistory(requireProductId())
    )
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    historyRows.value = []
    loadError.value = resolved.message
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.productId] as const,
  ([visible]) => {
    if (!visible) {
      return
    }
    void loadHistory()
  },
  { immediate: true }
)
</script>

<style scoped>
.showroom-product-history-drawer__body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.showroom-product-history-drawer__card {
  padding: 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-product-history-drawer__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.showroom-product-history-drawer__headline {
  display: flex;
  align-items: center;
  gap: 8px;
}

.showroom-product-history-drawer__count {
  color: #4b5563;
  font-size: 0.85rem;
}

.showroom-product-history-drawer__table :deep(.el-table__header th) {
  background: #f7f9fc;
}

.showroom-product-history-drawer__empty {
  padding: 32px 0;
  color: #4b5563;
  text-align: center;
}
</style>
