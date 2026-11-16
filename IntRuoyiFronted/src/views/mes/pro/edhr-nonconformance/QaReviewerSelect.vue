<template>
  <UserSelectV2
    :model-value="modelValue"
    :load-user-options="getQaReviewerCandidates"
    placeholder="请选择具备QA处置权限的责任人"
    @update:model-value="emit('update:modelValue', $event)"
  />
</template>
<script setup lang="ts">
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import { getQaReviewerCandidates } from '@/api/mes/pro/edhr/nonconformanceReview'
defineProps<{ modelValue?: string | number }>()
const emit = defineEmits<{ 'update:modelValue': [value: string | number | undefined] }>()
</script>
