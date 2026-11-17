<template>
  <section v-loading="saving" data-flow-panel="route-loss-reasons">
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mb-12px" />
    <div v-if="!readOnly" class="loss-reason-picker">
      <el-select
        v-model="selectedId"
        filterable
        clearable
        :loading="loading"
        placeholder="输入编码或描述选择损耗原因"
        aria-label="选择损耗原因"
        :disabled="saving"
      >
        <el-option
          v-for="reason in catalog"
          :key="reason.id"
          :value="String(reason.id)"
          :label="`${reason.reasonCode} / ${reason.reasonName}`"
          :disabled="boundIds.has(String(reason.id))"
        />
      </el-select>
      <el-button type="primary" :disabled="!selectedId || saving || loading" @click="add"
        >添加</el-button
      >
      <el-button :disabled="saving || loading" @click="load">刷新原因库</el-button>
    </div>
    <el-table
      :data="boundReasons"
      row-key="id"
      border
      empty-text="当前工序尚未关联损耗原因"
      class="mt-12px"
    >
      <el-table-column prop="reasonCode" label="损耗原因编码" width="160" />
      <el-table-column prop="reasonName" label="损耗原因描述" min-width="200" />
    </el-table>
  </section>
</template>
<script setup lang="ts">
import { listLossReasons, type LossReason } from '@/api/mes/pro/lossreason'
import * as RouteApi from '@/api/mes/pro/route'
const props = defineProps<{
  routeProcessId: number | string
  routeVersionId: number | string
  readOnly: boolean
  snapshot: {
    routeSnapshotSha256: string
    productionProcessConfigs: RouteApi.ProRouteProductionProcessConfigVO[]
  }
}>()
const emit = defineEmits<{ saved: [value: RouteApi.ProRouteProductionProcessConfigSnapshotVO] }>()
const message = useMessage()
const catalog = ref<LossReason[]>([])
const selectedId = ref<string>()
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const config = computed(() =>
  props.snapshot.productionProcessConfigs.find(
    (c) => String(c.routeProcessId) === String(props.routeProcessId)
  )
)
const boundReasons = computed(() => config.value?.lossReasons || [])
const boundIds = computed(() => new Set(boundReasons.value.map((r) => String(r.id))))
const load = async () => {
  loading.value = true
  error.value = ''
  try {
    catalog.value = await listLossReasons()
  } catch (e: any) {
    error.value = e?.message || e?.msg || String(e)
  } finally {
    loading.value = false
  }
}
const add = async () => {
  if (props.readOnly || saving.value || !selectedId.value) return
  const source = catalog.value.find((r) => String(r.id) === selectedId.value)
  if (!source || !config.value || boundIds.value.has(String(source.id))) {
    error.value = '工序配置未加载或损耗原因已添加，请重新选择'
    return
  }
  saving.value = true
  error.value = ''
  try {
    const configs = props.snapshot.productionProcessConfigs.map((c) =>
      String(c.routeProcessId) !== String(props.routeProcessId)
        ? c
        : {
            ...c,
            lossReasons: [
              ...(c.lossReasons || []),
              {
                id: source.id,
                reasonCode: source.reasonCode,
                reasonName: source.reasonName,
                enabled: true
              }
            ]
          }
    )
    await RouteApi.ProRouteApi.saveRouteProductionProcessConfig({
      routeVersionId: props.routeVersionId,
      expectedRouteSnapshotSha256: props.snapshot.routeSnapshotSha256,
      schemaVersion: 1,
      productionProcessConfigs: configs
    })
    emit('saved', await RouteApi.ProRouteApi.getRouteProductionProcessConfig(props.routeVersionId))
    selectedId.value = undefined
    message.success('损耗原因已关联当前工序')
  } catch (e: any) {
    error.value = e?.message || e?.msg || String(e)
  } finally {
    saving.value = false
  }
}
onMounted(() => {
  if (!props.readOnly) void load()
})
</script>
<style scoped>
.loss-reason-picker {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.loss-reason-picker .el-select {
  flex: 1;
  min-width: 220px;
}
</style>
