<template>
  <section v-loading="loading" class="route-craft-binding">
    <el-form label-width="70px" @submit.prevent>
      <div v-for="(row, index) in rows" :key="row.key" class="route-craft-binding__row">
        <el-form-item :label="'工艺 ' + (index + 1)">
          <el-select
            v-model="row.craftId"
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
        <el-form-item label="执行设备">
          <el-select
            v-model="row.deviceIds"
            multiple
            placeholder="请选择执行设备"
            :disabled="disabled || saving"
          >
            <el-option
              v-for="device in devices"
              :key="device.deviceId"
              :value="device.deviceId"
              :label="device.deviceCode + ' / ' + device.deviceName"
            />
          </el-select>
        </el-form-item>
        <el-button
          v-if="!disabled && rows.length > 1"
          v-hasPermi="['mes:pro-route:version-update']"
          link
          type="danger"
          size="small"
          :disabled="saving"
          @click="rows.splice(index, 1)"
          >移除此项</el-button
        >
      </div>
      <div class="route-craft-binding__actions">
        <el-button
          v-if="!disabled"
          v-hasPermi="['mes:pro-route:version-update']"
          size="small"
          :disabled="loading || saving"
          @click="addRow"
          >添加工艺</el-button
        >
        <el-button
          v-if="!disabled"
          v-hasPermi="['mes:pro-route:version-update']"
          type="primary"
          size="small"
          :loading="saving"
          :disabled="!canApply || loading"
          @click="bindCraft"
          >应用到当前工序</el-button
        >
        <el-button
          v-if="!disabled && !bindings.length && hasExistingParameters"
          v-hasPermi="['mes:pro-route:version-update']"
          type="primary"
          plain
          size="small"
          :disabled="loading || saving"
          @click="bindExisting"
          >关联现有参数工艺</el-button
        >
        <el-button size="small" :disabled="loading || saving" @click="load">刷新工艺列表</el-button>
      </div>
    </el-form>
    <p v-if="!loading && !crafts.length">当前工序尚无工艺，请在工艺库新增或从现有流程初始化。</p>
    <p v-if="bindings.length">参数由工艺库统一维护，各工艺的执行设备保存在当前流程中。</p>
    <p v-else-if="hasExistingParameters"
      >“关联现有参数工艺”仅关联完全匹配的工艺，保留全部原参数和设备。</p
    >
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
  </section>
</template>
<script setup lang="ts">
import { CraftApi, type Craft } from '@/api/mes/pro/craft'
import type {
  ProRouteCraftBindingVO,
  ProRouteProcessDeviceParameterDeviceVO
} from '@/api/mes/pro/route/flowconfig'
const props = defineProps<{
  processId: number
  routeProcessId: number
  routeVersionId: number | string
  snapshotSha256: string
  bindings: ProRouteCraftBindingVO[]
  devices: ProRouteProcessDeviceParameterDeviceVO[]
  disabled: boolean
}>()
const emit = defineEmits<{ saved: [] }>()
interface BindingRow {
  key: number
  craftId?: number
  deviceIds: number[]
}
const crafts = ref<Craft[]>([]),
  rows = ref<BindingRow[]>([]),
  loading = ref(false),
  saving = ref(false),
  error = ref('')
let generation = 0,
  nextKey = 0
const explain = (e: unknown) => (e instanceof Error ? e.message : String(e))
const hasExistingParameters = computed(() =>
  props.devices.some((device) => device.parameters?.length)
)
const canApply = computed(
  () => rows.value.length > 0 && rows.value.every((row) => row.craftId && row.deviceIds.length)
)
function addRow() {
  rows.value.push({ key: nextKey++, deviceIds: [] })
}
function resetRows() {
  rows.value = props.bindings.map((binding) => ({
    key: nextKey++,
    craftId: binding.craftId,
    deviceIds: [...binding.craftDeviceIds]
  }))
  if (!rows.value.length) addRow()
}
async function load() {
  const current = ++generation
  loading.value = true
  error.value = ''
  crafts.value = []
  resetRows()
  try {
    const result = await CraftApi.list(props.processId)
    if (current === generation) crafts.value = result
  } catch (e) {
    if (current === generation) error.value = explain(e)
  } finally {
    if (current === generation) loading.value = false
  }
}
async function save(action: () => Promise<unknown>) {
  if (saving.value || props.disabled) return
  const current = generation
  saving.value = true
  error.value = ''
  try {
    await action()
    if (current === generation) emit('saved')
  } catch (e) {
    if (current === generation) error.value = explain(e)
  } finally {
    saving.value = false
  }
}
const target = () => ({
  routeVersionId: props.routeVersionId,
  routeProcessId: props.routeProcessId,
  expectedRouteSnapshotSha256: props.snapshotSha256
})
async function bindCraft() {
  if (!canApply.value) return
  const selected = rows.value.map((row) => ({
    craftId: row.craftId!,
    deviceIds: [...row.deviceIds]
  }))
  if (new Set(selected.map((row) => row.craftId)).size !== selected.length) {
    error.value = '工艺不能重复选择'
    return
  }
  const deviceIds = selected.flatMap((row) => row.deviceIds)
  if (new Set(deviceIds).size !== deviceIds.length) {
    error.value = '同一设备不能重复分配到多个工艺'
    return
  }
  await save(() => CraftApi.bind({ ...target(), bindings: selected }))
}
async function bindExisting() {
  await save(() => CraftApi.bindExisting(target()))
}
watch(() => [props.processId, props.routeProcessId, props.routeVersionId], load, {
  immediate: true
})
watch(() => props.bindings, resetRows)
</script>
<style scoped>
.route-craft-binding {
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 12px;
}
.route-craft-binding__row {
  margin-bottom: 12px;
}
.route-craft-binding__row .el-select {
  width: 100%;
  min-width: 0;
}
.route-craft-binding__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.route-craft-binding__actions .el-button {
  margin-left: 0;
}
.route-craft-binding p {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
