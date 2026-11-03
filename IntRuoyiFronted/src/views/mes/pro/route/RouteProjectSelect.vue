<template>
  <div class="w-full">
    <el-select
      :model-value="modelValue"
      filterable
      clearable
      :loading="loading"
      :disabled="disabled || !!error"
      class="w-full"
      placeholder="请选择尚无工艺流程的项目代码"
      @visible-change="(visible) => visible && load()"
      @update:model-value="emit('update:modelValue', $event || undefined)"
    >
      <el-option
        v-for="project in projects"
        :key="project.id"
        :value="project.id"
        :disabled="isOccupied(project)"
        :label="`${project.projectCode} / ${project.projectName}${isOccupied(project) ? '（已有工艺流程）' : ''}`"
      />
    </el-select>
    <el-alert v-if="error" :title="error" type="error" :closable="false">
      <el-button link type="primary" @click="load">重新加载</el-button>
    </el-alert>
  </div>
</template>
<script setup lang="ts">
import { getRouteProjectOptions, type RouteProjectOption } from '@/api/mes/pro/route'
import { resolveRouteOperationErrorMessage } from './routeError'
const props = defineProps<{ modelValue?: number; routeId?: number | string; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: number | undefined] }>()
const projects = ref<RouteProjectOption[]>([])
const loading = ref(false)
const error = ref('')
const isOccupied = (project: RouteProjectOption) =>
  project.occupied && project.routeIds.some((id) => String(id) !== String(props.routeId))
async function load() {
  loading.value = true
  error.value = ''
  try {
    projects.value = await getRouteProjectOptions()
    const selected = projects.value.find((p) => p.id === props.modelValue)
    if (selected && isOccupied(selected)) emit('update:modelValue', undefined)
  } catch (cause) {
    projects.value = []
    error.value = resolveRouteOperationErrorMessage(cause, '项目代码加载失败')
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
