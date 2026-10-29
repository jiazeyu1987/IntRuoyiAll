<!-- 旧入口保留名称，所有选人行为由统一组件承载。 -->
<template><UserSelectV2 v-bind="$attrs" /></template>
<script setup lang="ts">
import UserSelectV2 from './UserSelectV2.vue'
defineOptions({ name: 'UserSelect', inheritAttrs: false })
</script>
