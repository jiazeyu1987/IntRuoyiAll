<template>
  <div class="showroom-hall-workbench">
    <div class="showroom-hall-workbench__toolbar">
      <div>
        <h3 class="showroom-hall-workbench__title">展柜维护工作台</h3>
        <p class="showroom-hall-workbench__subtitle">
          {{ hall ? `${hall.hallCode} · ${hall.name}` : '等待传入真实展柜数据' }}
        </p>
      </div>
      <div class="showroom-hall-workbench__actions">
        <el-button @click="editorVisible = true">编辑展柜</el-button>
        <el-button type="primary" @click="mappingVisible = true">维护展项</el-button>
      </div>
    </div>

    <el-alert
      :closable="false"
      show-icon
      title="该工作台依赖真实 hall 和 products 契约。缺少真实数据时不会伪造映射成功。"
      type="info"
    />

    <HallEditorDialog v-model="editorVisible" :hall="hall" @saved="handleSaved" />
    <HallProductMappingDialog v-model="mappingVisible" :hall="hall" :products="products" @saved="handleSaved" />
  </div>
</template>

<script setup lang="ts">
import HallProductMappingDialog from '@/views/showroom-admin/components/HallProductMappingDialog.vue'
import HallEditorDialog from './HallEditorDialog.vue'
import type { ShowroomHallRecord } from './contracts'

defineOptions({ name: 'ShowroomHallWorkbench' })

defineProps<{
  hall?: ShowroomHallRecord | null
  products?: unknown[]
}>()

const editorVisible = ref(false)
const mappingVisible = ref(false)

const handleSaved = () => {
  editorVisible.value = false
  mappingVisible.value = false
}
</script>

<style scoped>
.showroom-hall-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-hall-workbench__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.showroom-hall-workbench__title {
  margin: 0;
  color: #172033;
  font-size: 1.05rem;
}

.showroom-hall-workbench__subtitle {
  margin: 4px 0 0;
  color: #4b5563;
  font-size: 0.9rem;
}

.showroom-hall-workbench__actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 760px) {
  .showroom-hall-workbench__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .showroom-hall-workbench__actions {
    flex-wrap: wrap;
  }
}
</style>
