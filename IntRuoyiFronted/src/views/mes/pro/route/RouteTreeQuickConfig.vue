<template>
  <el-button v-if="canEdit" link type="primary" :disabled="disabled || visible"
    :aria-label="`快捷修改${label}`" @click="open">修改</el-button>
  <Dialog v-model="visible" :title="`修改${label}`" width="720px"
    :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving">
    <div v-loading="loading">
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <p v-if="ready" class="quick-config__notice">{{ versionLabel }} · 保存到草稿，发布后生效</p>
      <el-form v-if="ready" label-width="100px" :disabled="saving || disabled">
        <el-form-item v-if="kind === 'coefficient'" label="损耗系数" required>
          <el-input-number v-model="coefficient" :min="1" :precision="4" :step="0.01" aria-label="损耗系数" />
        </el-form-item>
        <template v-else>
          <div v-for="(leader, index) in leaders" :key="index" class="quick-config__leader">
            <el-form-item label="负责范围" required>
              <el-select v-model="leader.productionLineId" aria-label="负责范围">
                <el-option v-for="line in lines" :key="String(line.productionLineId)" :value="String(line.productionLineId)"
                  :label="line.productionLineName || String(line.productionLineId)" />
              </el-select>
            </el-form-item>
            <el-form-item label="组长来源" required>
              <el-select v-model="leader.candidateSourceType" aria-label="组长来源" @change="leader.candidateSourceIds = []; leader.candidateSourceNames = []">
                <el-option label="账号" value="USERS" /><el-option label="权限角色" value="ROLE" />
              </el-select>
            </el-form-item>
            <el-form-item label="生产组长" required>
              <UserSelectV2 v-if="leader.candidateSourceType === 'USERS'" v-model="leader.candidateSourceIds" multiple
                :user-options="users" :selected-users="leader.candidateSourceIds.map((id, i) => ({ id, nickname: leader.candidateSourceNames?.[i] }))"
                placeholder="请选择生产组长" />
              <el-select v-else v-model="leader.candidateSourceIds" multiple filterable aria-label="生产组长角色" placeholder="请选择权限角色">
                <el-option v-for="role in roles" :key="role.id" :value="role.id" :label="role.name" />
              </el-select>
            </el-form-item>
            <el-button link type="danger" @click="leaders.splice(index, 1)">移除配置</el-button>
          </div>
          <el-button :disabled="!lines.length" @click="addLeader">添加生产组长配置</el-button>
        </template>
      </el-form>
    </div>
    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="!ready || loading || disabled" @click="save">保存草稿</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { ProRouteApi, type MesRouteId, type ProRouteVO } from '@/api/mes/pro/route'
import { ProRouteFlowConfigApi, type ProRouteStartProductionLeaderVO, type ProRouteStartProductionLeaderProductionLineVO } from '@/api/mes/pro/route/flowconfig'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { getSimpleRoleList, type RoleVO } from '@/api/system/role'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import { hasPermission } from '@/directives/permission/hasPermi'
import { ensureSameSourceDraftCandidateForProductionConfig, resolveSameSourceDraftCandidateForProductionConfig } from './routeCandidateEntry'
import { resolveRouteOperationErrorMessage } from './routeError'

const props = defineProps<{ route: ProRouteVO; kind: 'coefficient' | 'leaders'; disabled: boolean }>()
const emit = defineEmits<{ busy: [value: boolean]; changed: [] }>()
const message = useMessage()
const label = computed(() => props.kind === 'coefficient' ? '损耗系数' : '生产组长')
const canEdit = computed(() => ['mes:pro-route:version-query', 'mes:pro-route:version-create',
  ...(props.kind === 'coefficient' ? ['mes:pro-route:version-update']
    : ['mes:pro-route:batch-record-config:query', 'mes:pro-route:batch-record-config:update'])]
  .every(permission => hasPermission([permission])))
const visible = ref(false), loading = ref(false), saving = ref(false), ready = ref(false), error = ref('')
const coefficient = ref<number>(), versionLabel = ref('')
const leaders = ref<ProRouteStartProductionLeaderVO[]>([])
const lines = ref<ProRouteStartProductionLeaderProductionLineVO[]>([])
const users = ref<UserVO[]>([]), roles = ref<RoleVO[]>([])
let revision = 0, disposed = false
let readVersionId: MesRouteId | undefined, activeVersionId: MesRouteId | undefined, editingRouteId: number | undefined
let baselineCoefficient: number | undefined, baselineLeaders = ''
watch(visible, value => {
  emit('busy', value)
  if (!value) { revision++; ready.value = false }
}, { flush: 'sync' })
onBeforeUnmount(() => { disposed = true; revision++; emit('busy', false) })
const requireContext = (request: number) => {
  if (disposed || request !== revision || !visible.value || props.route.id !== editingRouteId)
    throw new Error('流程或编辑窗口已变化，请重新打开')
  if (!canEdit.value || props.disabled) throw new Error('当前配置不可编辑，请检查权限或正在进行的操作')
}
const serializeLeaders = (items: ProRouteStartProductionLeaderVO[]) => JSON.stringify(items.map(item => ({
  productionLineId: String(item.productionLineId), candidateSourceType: item.candidateSourceType,
  candidateSourceIds: item.candidateSourceIds.map(String), remark: item.remark || null
})))
const open = async () => {
  if (!canEdit.value || props.disabled || visible.value || disposed) return
  visible.value = true; loading.value = true; ready.value = false; error.value = ''
  coefficient.value = undefined; leaders.value = []; lines.value = []; users.value = []; roles.value = []
  const request = ++revision
  editingRouteId = props.route.id
  try {
    if (editingRouteId == null) throw new Error('缺少工艺流程编号')
    const [route, versions] = await Promise.all([ProRouteApi.getRoute(editingRouteId), ProRouteApi.getRouteVersionList(editingRouteId)])
    requireContext(request)
    if (String(route.id) !== String(editingRouteId)) throw new Error('流程响应与当前所选流程不一致')
    const draft = resolveSameSourceDraftCandidateForProductionConfig(route, versions, `快捷修改${label.value}`)
    activeVersionId = route.activeRouteVersionId
    readVersionId = draft ? draft.id : route.activeRouteVersionId
    if (readVersionId == null) throw new Error('当前流程没有可读取的版本')
    versionLabel.value = draft ? `草稿 ${draft.versionNo}` : '基于已发布版本'
    if (props.kind === 'coefficient') {
      const data = await ProRouteApi.getRouteLossCoefficient(readVersionId)
      requireContext(request)
      if (!Number.isFinite(Number(data.lossCoefficient)) || data.lossCoefficient == null) throw new Error('损耗系数数据无效')
      coefficient.value = baselineCoefficient = Number(data.lossCoefficient)
    } else {
      const [items, ranges, accounts, roleOptions] = await Promise.all([
        ProRouteFlowConfigApi.getRouteStartProductionLeaders(editingRouteId, readVersionId),
        ProRouteFlowConfigApi.getRouteStartProductionLeaderProductionLines(editingRouteId, readVersionId),
        getSimpleUserList(), getSimpleRoleList()
      ])
      requireContext(request)
      if (items.some(item => !['USERS', 'ROLE'].includes(item.candidateSourceType))) throw new Error('存在不支持的生产组长来源，请在流程配置中核对')
      baselineLeaders = serializeLeaders(items)
      leaders.value = items.map(item => ({ ...item, productionLineId: String(item.productionLineId), candidateSourceIds: [...item.candidateSourceIds] }))
      lines.value = ranges; users.value = accounts; roles.value = roleOptions
    }
    ready.value = true
  } catch (failure) {
    if (!disposed && request === revision) error.value = resolveRouteOperationErrorMessage(failure, '加载配置失败')
  } finally { if (request === revision) loading.value = false }
}
const addLeader = () => {
  const line = lines.value[0]
  if (!line || saving.value || props.disabled) return
  leaders.value.push({ productionLineId: String(line.productionLineId), candidateSourceType: 'USERS', candidateSourceIds: [], candidateSourceNames: [] })
}
const buildLeaderItems = () => leaders.value.map(leader => ({ ...leader, candidateSourceNames: leader.candidateSourceIds.map(id => {
  if (leader.candidateSourceType === 'ROLE') {
    const role = roles.value.find(item => String(item.id) === String(id))
    if (!role) throw new Error('所选角色已失效，请重新选择')
    return role.name
  }
  const user = users.value.find(item => String(item.id) === String(id))
  if (!user) throw new Error('所选账号已失效，请重新选择')
  return `${user.nickname}（${user.username}）`
}) }))
const validate = () => {
  if (props.kind === 'coefficient') {
    if (coefficient.value == null || !Number.isFinite(coefficient.value) || coefficient.value < 1) throw new Error('损耗系数必须大于等于1')
    return coefficient.value !== baselineCoefficient
  }
  if (!leaders.value.length) throw new Error('请至少配置一位生产组长')
  for (const leader of leaders.value) {
    if (!lines.value.some(line => String(line.productionLineId) === String(leader.productionLineId))) throw new Error('生产组长负责范围必须来自当前流程')
    if (!['USERS', 'ROLE'].includes(leader.candidateSourceType) || !leader.candidateSourceIds.length) throw new Error('请选择生产组长账号或权限角色')
  }
  buildLeaderItems()
  return serializeLeaders(leaders.value) !== baselineLeaders
}
const save = async () => {
  if (saving.value || !ready.value || props.disabled) return
  const request = revision
  let draftCreated = false
  saving.value = true; error.value = ''
  try {
    requireContext(request)
    if (!validate()) { visible.value = false; return }
    const result = await ensureSameSourceDraftCandidateForProductionConfig({
      routeId: editingRouteId!, actionName: `快捷修改${label.value}`, changeReason: `树形管理快捷修改${label.value}`
    })
    if (!result) throw new Error('未获取到可编辑草稿')
    draftCreated = result.created
    requireContext(request)
    const candidate = result.candidate
    if (String(candidate.sourceRouteVersionId) !== String(activeVersionId) ||
      (String(readVersionId) !== String(activeVersionId) && String(candidate.id) !== String(readVersionId)))
      throw new Error('流程版本已变化，请重新打开后修改')
    if (props.kind === 'coefficient') {
      const current = await ProRouteApi.getRouteLossCoefficient(candidate.id)
      requireContext(request)
      if (Number(current.lossCoefficient) !== baselineCoefficient) throw new Error('损耗系数已被其他操作修改，请重新打开')
      await ProRouteApi.saveRouteLossCoefficient({ routeVersionId: candidate.id,
        expectedRouteSnapshotSha256: current.routeSnapshotSha256, lossCoefficient: coefficient.value! })
    } else {
      const current = await ProRouteFlowConfigApi.getRouteStartProductionLeaders(editingRouteId!, candidate.id)
      requireContext(request)
      if (serializeLeaders(current) !== baselineLeaders) throw new Error('生产组长配置已被其他操作修改，请重新打开')
      await ProRouteFlowConfigApi.saveRouteStartProductionLeaders({ routeId: editingRouteId!, routeVersionId: candidate.id, items: buildLeaderItems() })
    }
    if (!disposed && request === revision) {
      message.success(`${label.value}已保存到草稿，请点击上方“发布”生效`)
      visible.value = false
      emit('changed')
    }
  } catch (failure) {
    if (!disposed && request === revision) {
      error.value = resolveRouteOperationErrorMessage(failure, '保存失败')
      message.error(error.value)
    }
    if (draftCreated && !disposed) emit('changed')
  } finally { saving.value = false }
}
</script>

<style scoped>
.quick-config__notice { color: var(--el-text-color-secondary); }
.quick-config__leader { padding: 12px; margin-bottom: 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 4px; }
.quick-config__leader .el-select { width: 100%; }
</style>
