<template><span :class="{ referenced: isReference || projectCount > 0 }"><span v-if="isReference">引用 · 来源项目：{{ sourceProjectName }} · </span><span>引用项目：{{ projectCount }}</span><span v-if="obsolete"> · 源文件已作废</span><span v-else-if="pendingEffect"> · 待生效</span></span></template>
<script setup lang="ts">
defineProps<{ isReference: boolean; projectCount: number; sourceProjectName: string; obsolete?: boolean; pendingEffect?: boolean }>()
</script>
<style scoped>.referenced { color: #b85a00; }</style>
