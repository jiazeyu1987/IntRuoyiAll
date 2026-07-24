<template>
  <div class="showroom-discussion-workbench">
    <div class="showroom-discussion-workbench__toolbar">
      <div>
        <h3 class="showroom-discussion-workbench__title">产品讨论工作台</h3>
        <p class="showroom-discussion-workbench__subtitle">
          先选择真实产品，再进入字段 / 模块 / 审批单锚点下的讨论线程。
        </p>
      </div>
      <div class="showroom-discussion-workbench__actions">
        <el-select v-model="selectedProductId" filterable placeholder="请选择产品">
          <el-option
            v-for="product in productOptions"
            :key="product.value"
            :label="product.label"
            :value="product.value"
          />
        </el-select>
      </div>
    </div>

    <el-alert
      v-if="productOptions.length === 0"
      :closable="false"
      show-icon
      title="当前没有可用于讨论的真实产品数据"
      type="warning"
    />

    <ProductDiscussionPanel :product-id="selectedProductId" />
  </div>
</template>

<script setup lang="ts">
import ProductDiscussionPanel from './ProductDiscussionPanel.vue'
import { buildProductOptions } from './contracts'

defineOptions({ name: 'DiscussionWorkbench' })

const props = withDefaults(
  defineProps<{
    products?: unknown[]
  }>(),
  {
    products: () => []
  }
)

const productOptions = computed(() => {
  try {
    return buildProductOptions(props.products)
  } catch {
    return []
  }
})

const selectedProductId = ref<number | null>(null)

watch(
  productOptions,
  (nextValue) => {
    if (!nextValue.find((item) => item.value === selectedProductId.value)) {
      selectedProductId.value = nextValue[0]?.value ?? null
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.showroom-discussion-workbench {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.showroom-discussion-workbench__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px 8px 0 0;
}

.showroom-discussion-workbench__title {
  margin: 0;
  color: #172033;
  font-size: 1.05rem;
}

.showroom-discussion-workbench__subtitle {
  margin: 4px 0 0;
  color: #4b5563;
  font-size: 0.9rem;
}

.showroom-discussion-workbench__actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 760px) {
  .showroom-discussion-workbench__toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
