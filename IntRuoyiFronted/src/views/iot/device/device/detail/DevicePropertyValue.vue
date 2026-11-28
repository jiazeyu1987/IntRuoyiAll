<template>
  <el-alert v-if="presentation.error" :title="presentation.error" type="error" :closable="false" />
  <dl v-else class="device-property-value">
    <div v-for="(row, index) in presentation.rows" :key="index">
      <dt v-if="definition.dataType === 'struct' || definition.dataType === 'array'">{{ row.label }}</dt>
      <dd>{{ row.value }}</dd>
    </div>
  </dl>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { presentPropertyValue, type PropertyValueDefinition } from './propertyValuePresentation'
const props = defineProps<{ value: unknown; definition: PropertyValueDefinition }>()
const presentation = computed(() => presentPropertyValue(props.value, props.definition))
</script>
<style scoped>
.device-property-value { margin: 0; text-align: left; overflow-wrap: anywhere; }
.device-property-value > div + div { margin-top: 6px; }
.device-property-value dt { color: var(--el-text-color-secondary); }
.device-property-value dd { margin: 0; white-space: pre-wrap; }
</style>
