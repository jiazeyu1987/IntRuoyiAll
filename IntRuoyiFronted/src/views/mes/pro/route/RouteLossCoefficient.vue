<template>
  <div v-loading="loading" data-flow-panel="route-loss-coefficient">
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-form label-position="top">
      <el-form-item label="损耗系数">
        <el-input-number v-model="value" :min="1" :precision="4" :step="0.01"
          :disabled="disabled || !loaded || saving" data-route-loss-coefficient />
      </el-form-item>
      <p>适用于当前工艺流程的全部工序，默认1.1。完成下限为订单数量，累计上限为订单数量×损耗系数。</p>
      <p>例如：订单1000件、系数1.1，下限1000件，上限1100件。保存并发布后供后续订单使用，已冻结订单保持原配置。</p>
      <el-button v-if="!disabled" type="primary" :disabled="!loaded || loading" :loading="saving"
        data-save-route-loss-coefficient @click="handleSave">保存</el-button>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ProRouteApi } from '@/api/mes/pro/route'
import type { MesRouteId } from '@/api/mes/pro/route'
const props = defineProps<{ versionId?: MesRouteId; disabled: boolean; afterSave?: (versionId: MesRouteId) => Promise<void> }>()
const message = useMessage()
const value = ref<number>()
const baseline = ref<number>()
const loaded = ref(false)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
let generation = 0
const hasChanges = () => loaded.value && value.value !== baseline.value
const discard = () => { value.value = baseline.value }
const load = async () => {
  const request = ++generation
  loaded.value = false
  error.value = ''
  value.value = undefined
  baseline.value = undefined
  if (!props.versionId) return
  loading.value = true
  try {
    const data = await ProRouteApi.getRouteLossCoefficient(props.versionId)
    if (request !== generation) return
    value.value = baseline.value = Number(data.lossCoefficient)
    loaded.value = true
  } catch (e) {
    if (request === generation) error.value = e instanceof Error ? e.message : String(e)
  } finally {
    if (request === generation) loading.value = false
  }
}
const save = async () => {
  if (!hasChanges()) return
  if (props.disabled || !props.versionId || value.value === undefined || !Number.isFinite(value.value) || value.value < 1) {
    throw new Error('请在草稿版本中填写大于等于1的损耗系数')
  }
  saving.value = true
  error.value = ''
  try {
    const current = await ProRouteApi.getRouteLossCoefficient(props.versionId)
    if (Number(current.lossCoefficient) !== baseline.value) throw new Error('损耗系数已被其他操作修改，请重新打开流程后再设置')
    const submitted = value.value
    await ProRouteApi.saveRouteLossCoefficient({ routeVersionId: props.versionId,
      expectedRouteSnapshotSha256: current.routeSnapshotSha256, lossCoefficient: submitted })
    baseline.value = submitted
    await props.afterSave?.(props.versionId)
  } finally {
    saving.value = false
  }
}
const handleSave = async () => {
  try { await save(); message.success('损耗系数已保存，发布后供后续订单使用') }
  catch (e) { error.value = e instanceof Error ? e.message : String(e) }
}
watch(() => props.versionId, load, { immediate: true })
defineExpose({ hasChanges, discard, save })
</script>
