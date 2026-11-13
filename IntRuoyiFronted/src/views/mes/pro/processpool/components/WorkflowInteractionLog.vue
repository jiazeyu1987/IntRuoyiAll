<template>
  <el-alert v-if="saveError" :title="saveError" type="error" :closable="false" class="mb-8px" />
</template>
<script setup lang="ts">
defineProps<{ saveError: string; saving: number }>()
</script>
