<template>
  <section v-loading="loading" class="route-craft-binding">
    <el-form label-width="70px" @submit.prevent>
      <el-form-item label="工艺">
        <el-select
          v-model="selectedId"
          filterable
          placeholder="请选择当前工序的工艺"
          :disabled="disabled || saving"
        >
          <el-option
            v-for="craft in crafts"
            :key="craft.id"
            :value="craft.id"
            :label="craft.code + ' ' + craft.name"
          />
        </el-select>
      </el-form-item>
      <el-button
        v-if="!disabled"
        v-hasPermi="['mes:pro-route:version-update']"
        type="primary"
        size="small"
        :loading="saving"
        :disabled="!selectedId || loading"
        @click="bindCraft"
        >应用到当前工序</el-button
      >
      <el-button size="small" :disabled="loading || saving" @click="load">刷新工艺列表</el-button>
    </el-form>
    <p v-if="!loading && !crafts.length">当前工序尚无工艺，请在工艺库新增或从现有流程初始化。</p>
    <p v-if="craftId">参数由工艺库统一维护，当前流程保留设备关联。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
  </section>
</template>
<script setup lang="ts">
import { CraftApi, type Craft } from '@/api/mes/pro/craft'
const props = defineProps<{
  processId: number
  routeProcessId: number
  routeVersionId: number | string
  snapshotSha256: string
  craftId?: number
  disabled: boolean
}>()
const emit = defineEmits<{ saved: [] }>()
const crafts = ref<Craft[]>([]),
  selectedId = ref<number>(),
  loading = ref(false),
  saving = ref(false),
  error = ref('')
let generation = 0
const explain = (e: unknown) => (e instanceof Error ? e.message : String(e))
async function load() {
  const current = ++generation
  loading.value = true
  error.value = ''
  crafts.value = []
  selectedId.value = props.craftId
  try {
    const result = await CraftApi.list(props.processId)
    if (current === generation) crafts.value = result
  } catch (e) {
    if (current === generation) error.value = explain(e)
  } finally {
    if (current === generation) loading.value = false
  }
}
async function bindCraft() {
  if (!selectedId.value || saving.value) return
  const current = generation
  saving.value = true
  error.value = ''
  try {
    await CraftApi.bind({
      craftId: selectedId.value,
      routeVersionId: props.routeVersionId,
      routeProcessId: props.routeProcessId,
      expectedRouteSnapshotSha256: props.snapshotSha256
    })
    if (current === generation) emit('saved')
  } catch (e) {
    if (current === generation) error.value = explain(e)
  } finally {
    saving.value = false
  }
}
watch(() => [props.processId, props.routeProcessId, props.routeVersionId], load, {
  immediate: true
})
watch(
  () => props.craftId,
  (id) => {
    selectedId.value = id
  }
)
</script>
<style scoped>
.route-craft-binding {
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 12px;
}
.route-craft-binding p {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
