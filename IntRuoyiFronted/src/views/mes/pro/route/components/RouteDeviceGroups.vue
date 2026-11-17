<template>
  <section v-loading="saving" class="route-device-groups">
    <strong>设备组配置</strong>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-alert
      v-if="!config"
      title="请先保存新增工序，再配置设备组。"
      type="info"
      :closable="false"
    />
    <template v-else>
      <p v-if="!groups.length">当前工序尚无设备组，请新增设备组并选择设备。</p>
      <el-form label-width="80px">
        <div v-for="(group, index) in groups" :key="index" class="route-device-groups__group">
          <el-form-item label="设备组">
            <el-input
              v-model="group.deviceGroupKey"
              :disabled="disabled"
              placeholder="请输入设备组名称"
            />
          </el-form-item>
          <el-form-item label="选择方式">
            <el-select v-model="group.selectionMode" :disabled="disabled">
              <el-option label="单选设备" value="SINGLE" />
              <el-option label="多选设备" value="MULTIPLE" />
            </el-select>
          </el-form-item>
          <el-form-item label="设备">
            <el-select
              v-model="group.deviceIds"
              multiple
              filterable
              :disabled="disabled || loading"
              :loading="loading"
              placeholder="请选择设备"
              aria-label="设备组设备"
            >
              <el-option
                v-for="device in options"
                :key="device.deviceId"
                :value="device.deviceId"
                :label="device.deviceCode + ' / ' + device.deviceName"
                :disabled="
                  groups.some(
                    (other) => other !== group && other.deviceIds.includes(device.deviceId)
                  )
                "
              />
            </el-select>
          </el-form-item>
          <el-button v-if="!disabled" link type="danger" @click="groups.splice(index, 1)"
            >删除设备组</el-button
          >
        </div>
      </el-form>
      <div v-if="!disabled" class="route-device-groups__actions">
        <el-button v-hasPermi="['mes:pro-route:version-update']" :disabled="saving" @click="add"
          >新增设备组</el-button
        >
        <el-button
          v-hasPermi="['mes:pro-route:version-update']"
          type="primary"
          :disabled="saving || loading || !loaded"
          @click="save"
          >保存设备组</el-button
        >
        <el-button :disabled="saving || loading" @click="load">刷新设备列表</el-button>
      </div>
    </template>
  </section>
</template>
<script setup lang="ts">
import { ProRouteApi, type ProRouteProductionProcessConfigSnapshotVO } from '@/api/mes/pro/route'
import {
  ProRouteFlowConfigApi,
  type ProRouteProcessDeviceParameterDeviceVO
} from '@/api/mes/pro/route/flowconfig'

const props = defineProps<{
  routeProcessId: number | string
  routeVersionId: number | string
  snapshot: ProRouteProductionProcessConfigSnapshotVO
  disabled: boolean
}>()
const emit = defineEmits<{ saved: [snapshot: ProRouteProductionProcessConfigSnapshotVO] }>()
interface DeviceGroup extends Record<string, unknown> {
  deviceGroupKey: string
  selectionMode: string
  deviceIds: number[]
}
const config = computed(() =>
  props.snapshot.productionProcessConfigs.find(
    (c) => String(c.routeProcessId) === String(props.routeProcessId)
  )
)
const groups = ref<DeviceGroup[]>([])
const options = ref<ProRouteProcessDeviceParameterDeviceVO[]>([])
const loading = ref(false),
  loaded = ref(false),
  saving = ref(false),
  error = ref('')
const message = useMessage()
const explain = (e: any) => e?.message || e?.msg || String(e)
watch(
  config,
  () => {
    groups.value = JSON.parse(JSON.stringify(config.value?.deviceSelectionGroups || []))
  },
  { immediate: true }
)
function add() {
  let index = groups.value.length + 1
  while (groups.value.some((g) => g.deviceGroupKey === `设备组${index}`)) index++
  groups.value.push({ deviceGroupKey: `设备组${index}`, selectionMode: 'SINGLE', deviceIds: [] })
}
async function load() {
  loading.value = true
  loaded.value = false
  error.value = ''
  try {
    options.value = await ProRouteFlowConfigApi.getRouteDeviceOptions()
    loaded.value = true
  } catch (e) {
    error.value = explain(e)
  } finally {
    loading.value = false
  }
}
async function save() {
  if (props.disabled || saving.value || !config.value || !loaded.value) return
  error.value = ''
  const keys = groups.value.map((g) => g.deviceGroupKey.trim())
  const ids = groups.value.flatMap((g) => g.deviceIds)
  if (
    keys.some((key) => !key) ||
    new Set(keys).size !== keys.length ||
    groups.value.some((g) => !g.deviceIds.length) ||
    new Set(ids).size !== ids.length
  ) {
    error.value = '请填写不重复的设备组名称，并为每组选择设备；设备不能重复分组。'
    return
  }
  const missingRule = config.value.parameterRules?.find((r) => !ids.includes(Number(r.deviceId)))
  if (missingRule) {
    error.value = `参数“${missingRule.parameterName || missingRule.parameterCode}”（${missingRule.parameterCode}）引用的设备未加入本工序设备组，请先处理参数及工艺关联。`
    return
  }
  saving.value = true
  let written = false
  try {
    await ProRouteApi.saveRouteProductionProcessConfig({
      routeVersionId: props.routeVersionId,
      expectedRouteSnapshotSha256: props.snapshot.routeSnapshotSha256,
      schemaVersion: 1,
      productionProcessConfigs: props.snapshot.productionProcessConfigs.map((c) =>
        c === config.value
          ? {
              ...c,
              deviceSelectionGroups: groups.value.map((g) => ({
                ...g,
                deviceGroupKey: g.deviceGroupKey.trim()
              }))
            }
          : c
      )
    })
    written = true
    emit('saved', await ProRouteApi.getRouteProductionProcessConfig(props.routeVersionId))
    message.success('设备组已保存')
  } catch (e) {
    error.value = (written ? '设备组已保存，但重新读取配置失败：' : '') + explain(e)
  } finally {
    saving.value = false
  }
}
watch(
  () => props.disabled,
  (disabled) => {
    if (!disabled && !loaded.value && !loading.value) void load()
  },
  { immediate: true }
)
</script>
<style scoped>
.route-device-groups {
  margin: 12px 0;
}
.route-device-groups__group {
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.route-device-groups .el-select {
  width: 100%;
}
.route-device-groups__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
}
.route-device-groups__actions .el-button {
  margin-left: 0;
}
</style>
