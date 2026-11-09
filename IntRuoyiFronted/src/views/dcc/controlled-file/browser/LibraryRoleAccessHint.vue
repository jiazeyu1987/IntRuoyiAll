<template>
  <el-button v-if="checkPermi(['system:role:query'])" @click="router.push('/system/role/permission-role')">
    权限角色授权
  </el-button>
</template>
<script setup lang="ts">
import { useRouter } from 'vue-router'
import { checkPermi } from '@/utils/permission'
defineProps<{ scope: 'QMS' | 'GENERAL' }>()
const router = useRouter()
</script>
