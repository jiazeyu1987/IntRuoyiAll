<template>
  <section v-loading="loading" class="process-craft-binding" aria-label="已绑定工艺">
    <h3>已绑定工艺</h3>
    <el-form label-width="100px" @submit.prevent>
      <el-form-item label="工艺路线版本">
        <el-select
          v-model="selectedVersionId"
          placeholder="请选择要查看或绑定的版本"
          :disabled="loading || creating"
          @change="loadConfig"
        >
          <el-option
            v-for="version in versions"
            :key="version.id"
            :value="version.id"
            :label="`${version.versionNo}（${versionStatusLabel(version)}）`"
          />
        </el-select>
        <el-button class="ml-8px" :disabled="loading || creating" @click="loadVersions(selectedVersionId)">刷新</el-button>
      </el-form-item>
    </el-form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-alert
      v-if="selectedVersion?.legacyProductionConfigMissing"
      title="该旧版未保存生产配置快照，无法确认历史工艺绑定。请选择草稿迁移现有正式生产配置，再维护工艺绑定；审批发布后生效。"
      type="warning"
      :closable="false"
      show-icon
    />
    <el-alert
      v-if="!error && !loading && !versions.length"
      title="当前路线没有版本，请先在工艺流程中建立版本。"
      type="warning"
      :closable="false"
      show-icon
    />
    <template v-if="config">
      <el-alert
        :title="editable ? '选择工艺和执行设备后，点击“应用到当前工序”保存到草稿版本；审批发布后生效。' : '当前版本仅供查看。请选择草稿版本增加工艺绑定，或从当前生效版本新建草稿。'"
        type="info"
        :closable="false"
        show-icon
      />
      <p v-if="!config.craftBindings.length">当前版本的该工序尚未绑定工艺。</p>
      <RouteCraftBinding
        v-if="editable || config.craftBindings.length"
        :key="config.routeVersionId"
        :process-id="processId"
        :route-process-id="routeProcessId"
        :route-version-id="config.routeVersionId"
        :snapshot-sha256="config.routeSnapshotSha256"
        :bindings="config.craftBindings"
        :devices="config.devices || []"
        :disabled="!editable"
        @saved="handleSaved"
      />
      <el-alert
        v-if="editable && !config.devices?.length"
        title="当前工序尚未配置执行设备，请先通过“打开工序设置”配置设备，再绑定工艺。"
        type="warning"
        :closable="false"
        show-icon
      />
    </template>
    <el-form
      v-if="!readonly && selectedVersion?.active && (config || selectedVersion.legacyProductionConfigMissing) && !error"
      v-hasPermi="['mes:pro-route:version-create']"
      label-width="100px"
      class="mt-12px"
      @submit.prevent
    >
      <el-form-item label="新建草稿原因">
        <el-input v-model="changeReason" maxlength="512" :disabled="creating" placeholder="请填写本次增加工艺绑定的原因" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" plain :loading="creating" :disabled="!changeReason.trim() || loading" @click="createDraft">新建草稿并绑定工艺</el-button>
      </el-form-item>
    </el-form>
    <el-form v-if="canMigrate" label-width="150px" class="mt-12px" @submit.prevent>
      <el-form-item label="缺失工序超量比例（%）">
        <el-input-number v-model="migrationPercent" :min="0" :max="100" :precision="4" :disabled="creating" />
      </el-form-item>
      <p>仅补齐未配置的超量比例，保留已有比例、当前草稿的其他配置。现有正式设备、参数和损耗原因将迁入此草稿。</p>
      <el-form-item>
        <el-button type="primary" plain :loading="creating" @click="migrateProductionConfig">迁移生产配置到当前草稿</el-button>
      </el-form-item>
    </el-form>
  </section>
</template>

<script setup lang="ts">
import { ProRouteApi, type ProRouteVersionVO } from '@/api/mes/pro/route'
import { ProRouteFlowConfigApi, type ProRouteProcessDeviceParameterConfigVO } from '@/api/mes/pro/route/flowconfig'
import { checkPermi } from '@/utils/permission'
import RouteCraftBinding from '../route/components/RouteCraftBinding.vue'

const props = defineProps<{
  processId: number
  routeId: number
  routeProcessId: number
  readonly: boolean
}>()
const message = useMessage()
const versions = ref<ProRouteVersionVO[]>([])
const selectedVersionId = ref<number>()
const config = ref<ProRouteProcessDeviceParameterConfigVO>()
const loading = ref(false)
const creating = ref(false)
const error = ref('')
const changeReason = ref('')
const migrationPercent = ref<number>()
let generation = 0
let configSequence = 0
const selectedVersion = computed(() => versions.value.find((item) => item.id === selectedVersionId.value))
const canMigrate = computed(() => !props.readonly && !loading.value &&
  selectedVersion.value?.legacyProductionConfigMissing &&
  selectedVersion.value.lifecycleStatus === 'DRAFT' && !selectedVersion.value.active &&
  checkPermi(['mes:pro-route:version-update']))
const editable = computed(() => !props.readonly && !loading.value && !error.value &&
  selectedVersion.value?.lifecycleStatus === 'DRAFT' && !selectedVersion.value.active &&
  checkPermi(['mes:pro-route:version-update']))
const versionStatusLabel = (version: ProRouteVersionVO) => {
  const labels: Record<string, string> = {
    DRAFT: '草稿', PENDING_APPROVAL: '审批中', READY_TO_PUBLISH: '待发布', ACTIVE: '生效中',
    SUPERSEDED: '历史版本', REJECTED: '已驳回', CANCELLED: '已作废'
  }
  return labels[version.lifecycleStatus] || version.lifecycleStatus
}
const explain = (e: unknown) => e instanceof Error ? e.message : String(e)

async function loadVersions(preferredId?: number) {
  const current = ++generation
  loading.value = true
  error.value = ''
  config.value = undefined
  selectedVersionId.value = undefined
  versions.value = []
  try {
    const result = await ProRouteApi.getRouteVersionList(props.routeId)
    if (current !== generation) return
    versions.value = result
    if (preferredId) {
      if (!result.some((item) => item.id === preferredId)) throw new Error('指定的路线版本不存在，请刷新版本列表后核查。')
      selectedVersionId.value = preferredId
    } else {
      const active = result.filter((item) => item.active)
      if (active.length > 1) throw new Error('当前路线存在多个生效版本，请联系管理员核查。')
      if (active.length === 1) selectedVersionId.value = active[0].id
      else if (result.length === 1) selectedVersionId.value = result[0].id
    }
    await loadConfig()
  } catch (e) {
    if (current === generation) error.value = explain(e)
  } finally {
    if (current === generation) loading.value = false
  }
}

async function loadConfig() {
  const current = generation
  const sequence = ++configSequence
  const versionId = selectedVersionId.value
  config.value = undefined
  error.value = ''
  if (!versionId) return
  if (selectedVersion.value?.legacyProductionConfigMissing) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const result = await ProRouteFlowConfigApi.getRouteProcessDeviceParameterConfig(versionId, props.routeProcessId)
    if (current !== generation || sequence !== configSequence) return
    if (String(result.processId) !== String(props.processId)) throw new Error('所选版本中的工序与当前工序不一致，请核查路线配置。')
    if (!result.routeSnapshotSha256) throw new Error('路线版本缺少校验信息，无法绑定工艺。')
    config.value = result
  } catch (e) {
    if (current === generation && sequence === configSequence) error.value = explain(e)
  } finally {
    if (current === generation && sequence === configSequence) loading.value = false
  }
}

async function handleSaved() {
  message.success('工艺绑定已保存到草稿版本，审批发布后生效')
  await loadConfig()
}

async function createDraft() {
  if (creating.value || props.readonly || !selectedVersion.value?.active || !changeReason.value.trim()) return
  const current = generation
  creating.value = true
  error.value = ''
  try {
    const version = await ProRouteApi.createRouteCandidateVersion({
      routeId: props.routeId,
      sourceRouteVersionId: selectedVersion.value.id,
      changeReason: changeReason.value.trim()
    })
    if (current !== generation) return
    changeReason.value = ''
    await loadVersions(version.id)
  } catch (e) {
    if (current === generation) error.value = explain(e)
  } finally {
    creating.value = false
  }
}

async function migrateProductionConfig() {
  if (!canMigrate.value || creating.value) return
  const version = selectedVersion.value!
  if (!version.routeSnapshotSha256) {
    error.value = '草稿缺少版本校验信息，请刷新后重试。'
    return
  }
  const current = generation
  creating.value = true
  error.value = ''
  try {
    await ProRouteApi.migrateProductionConfig({
      routeVersionId: version.id,
      expectedRouteSnapshotSha256: version.routeSnapshotSha256,
      missingOveragePercent: migrationPercent.value
    })
    if (current !== generation) return
    message.success('生产配置已迁入草稿，请核对设备和参数后绑定工艺；审批发布后生效')
    migrationPercent.value = undefined
    await loadVersions(version.id)
  } catch (e) {
    if (current === generation) error.value = explain(e)
  } finally {
    creating.value = false
  }
}

watch(() => [props.processId, props.routeId, props.routeProcessId], () => loadVersions(), { immediate: true })
onBeforeUnmount(() => { generation++ })
defineExpose({ selectedVersion })
</script>

<style scoped>
.process-craft-binding { border-top: 1px solid var(--el-border-color-lighter); padding-top: 12px; }
.process-craft-binding h3 { margin: 0 0 12px; font-size: 14px; }
.process-craft-binding .el-select { width: min(360px, 100%); }
.process-craft-binding p { color: var(--el-text-color-secondary); font-size: 13px; }
</style>
