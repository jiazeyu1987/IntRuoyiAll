<template>
  <el-tag v-if="['OBSOLETE','SUPERSEDED'].includes(status || '')" type="warning">已作废 · 仅文控可查询归档及下载</el-tag>
</template>
<script setup lang="ts">
defineProps<{fileId:string|number;versionNo:string;fileName:string;fileNumber?:string;status?:string;referenceProjectId?:string;referenceFolderId?:string}>()
</script>
