<template>
  <div class="showroom-dashboard-cards">
    <article
      v-for="card in cards"
      :key="card.key"
      class="showroom-dashboard-cards__card"
    >
      <div class="showroom-dashboard-cards__label-row">
        <span class="showroom-dashboard-cards__label">{{ card.label }}</span>
        <el-tag :type="card.tagType" effect="plain">{{ card.value }}</el-tag>
      </div>
      <strong class="showroom-dashboard-cards__value">{{ card.value }}</strong>
      <p class="showroom-dashboard-cards__helper">{{ card.helper }}</p>
    </article>
  </div>
</template>

<script setup lang="ts">
import type { ShowroomDashboardCard } from './contracts'

defineOptions({ name: 'ShowroomDashboardCards' })

defineProps<{
  cards: ShowroomDashboardCard[]
}>()
</script>

<style scoped>
.showroom-dashboard-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.showroom-dashboard-cards__card {
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-dashboard-cards__label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.showroom-dashboard-cards__label {
  color: #263247;
  font-size: 0.9rem;
  font-weight: 600;
}

.showroom-dashboard-cards__value {
  display: block;
  margin-top: 14px;
  color: #172033;
  font-size: 1.35rem;
  line-height: 1.2;
}

.showroom-dashboard-cards__helper {
  margin: 8px 0 0;
  color: #4b5563;
  font-size: 0.85rem;
}

@media (max-width: 960px) {
  .showroom-dashboard-cards {
    grid-template-columns: 1fr;
  }
}
</style>
