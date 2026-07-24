<template>
  <div class="showroom-product-workbench">
    <div class="showroom-product-workbench__toolbar">
      <div>
        <h3 class="showroom-product-workbench__title">产品详情工作台</h3>
        <p class="showroom-product-workbench__subtitle">
          {{ productId ? `产品 ID：${productId}` : '等待传入真实 productId' }}
        </p>
      </div>
      <div class="showroom-product-workbench__actions">
        <el-button @click="detailVisible = true">编辑详情</el-button>
        <el-button type="primary" @click="historyVisible = true">查看历史</el-button>
      </div>
    </div>

    <el-alert
      :closable="false"
      show-icon
      title="该工作台依赖真实产品契约和 productId。缺少 productId 时将保持空白，不会伪造成功状态。"
      type="info"
    />

    <ProductDetailDialog
      v-model="detailVisible"
      :product-id="productId"
      @saved="handleSaved"
      @submitted="handleSubmitted"
    />
    <ProductHistoryDrawer v-model="historyVisible" :product-id="productId" />
  </div>
</template>

<script setup lang="ts">
import ProductDetailDialog from './ProductDetailDialog.vue'
import ProductHistoryDrawer from './ProductHistoryDrawer.vue'

defineOptions({ name: 'ShowroomProductWorkbench' })

defineProps<{
  productId?: number | null
}>()

const detailVisible = ref(false)
const historyVisible = ref(false)

const handleSaved = () => {
  detailVisible.value = false
}

const handleSubmitted = () => {
  detailVisible.value = false
}
</script>

<style scoped>
.showroom-product-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-product-workbench__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.showroom-product-workbench__title {
  margin: 0;
  color: #172033;
  font-size: 1.05rem;
}

.showroom-product-workbench__subtitle {
  margin: 4px 0 0;
  color: #4b5563;
  font-size: 0.9rem;
}

.showroom-product-workbench__actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 760px) {
  .showroom-product-workbench__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .showroom-product-workbench__actions {
    flex-wrap: wrap;
  }
}
</style>
