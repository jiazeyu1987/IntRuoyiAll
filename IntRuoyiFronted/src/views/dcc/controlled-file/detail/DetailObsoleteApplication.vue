<template>
  <el-dialog :model-value="modelValue" title="作废申请" width="920px" :close-on-click-modal="false"
    :before-close="beforeClose" @update:model-value="close">
    <section v-loading="loading">
      <el-alert v-if="error || session.error" :title="error || session.error" type="error" :closable="false" />
      <p>{{ file.sourceOriginalFileName || file.fileName || file.title }} · 文件编号 {{ file.fileNumber }} · 版本 {{ file.versionNo }}</p>
      <p>直属主管审核 → 部门负责人指派及会签 → 批准，批准后作废并结束。</p>
      <p v-if="session.draft">草稿 {{ session.draft.instanceCode }} · 本次属性第 {{ session.draft.attributeRound }} 轮</p>
      <template v-if="session.sourceSelectionRequired">
        <p>存在多份已终止的作废申请，请明确选择本次重办来源。</p>
        <div v-for="candidate in session.candidates" :key="String(candidate.reworkSourceFormInstanceId)">
          <p>{{ candidate.reworkSourceFormInstanceCode }} · {{ sourceStatus(candidate.reworkSourceStatus) }} · 版本 {{ candidate.reworkSourceVersionNo }}
            · 原原因：{{ candidate.reason }}</p>
          <p v-if="!candidate.available">不可选择：{{ candidate.unavailableReason }}</p>
          <el-button :data-testid="`dcc-obsolete-source-${candidate.reworkSourceFormInstanceId}`"
            :disabled="loading || busy || session.sourceRefreshRequired || !candidate.available" @click="selectSource(candidate)">选择此申请来源</el-button>
        </div>
      </template>
      <template v-if="session.source">
        <p>重办来源 {{ session.source.instanceCode }} · {{ sourceStatus(session.source.reworkSourceStatus!) }} · 版本 {{ session.source.versionNo }}</p>
        <p>原原因：{{ session.source.reason }}</p>
        <p>下方修改仅用于新申请。原申请、原流程和原签名保持只读；确认重新发起后生成正式新草稿。</p>
      </template>
      <el-button v-if="session.sourceRefreshRequired" :disabled="busy || loading" @click="reloadEntry">重新读取申请来源</el-button>
      <el-alert v-if="session.draft && !session.draft.preparationReady"
        :title="session.draft.unavailableReason || '本次审批准备未完成，请核对后准备审批'"
        type="warning" :closable="false" />
      <el-form label-width="110px" :disabled="loading || busy || session.sourceSelectionRequired || session.sourceRefreshRequired">
        <el-form-item label="作废原因"><el-input v-model="reason" type="textarea" /></el-form-item>
        <el-form-item label="会签部门">
          <el-select v-model="selectedDepartments" multiple filterable>
            <el-option v-for="department in departments" :key="String(department.id)" :label="department.name" :value="String(department.id)" />
          </el-select>
        </el-form-item>
        <template v-if="snapshot">
          <ProjectAttributesFields :model-value="snapshot.actual" :readonly="loading || busy || session.sourceRefreshRequired" @update:model-value="editAttributes" />
        </template>
        <p v-else>填写原因并保存正式草稿后，系统固定本次项目默认属性；随后可修改本次实际值。</p>
      </el-form>
      <el-collapse v-if="snapshot" class="my-12px">
          <el-collapse-item :title="session.source ? '原申请冻结默认来源（只读）' : '本次草稿默认来源（只读）'" name="defaults">
          <ProjectAttributesFields :model-value="snapshot.defaultSource" readonly />
        </el-collapse-item>
      </el-collapse>
      <el-collapse v-if="reworkEvidence">
        <el-collapse-item title="重办来源的原审批规则（只读）" name="source-matrix">
          <el-table data-testid="dcc-obsolete-source-matrix" :data="reworkEvidence.reworkSourceApprovalMatrix!.nodes" size="small">
            <el-table-column prop="stageType" label="阶段" /><el-table-column prop="subjectName" label="参与对象" />
            <el-table-column prop="marker" label="参与标记" /><el-table-column prop="approveMethod" label="批准方式" />
            <el-table-column prop="approveRatio" label="比例" /><el-table-column prop="ruleRemark" label="原规则说明" min-width="220" />
          </el-table>
        </el-collapse-item>
        <el-collapse-item title="重办来源的关联与签名证据（只读）" name="source-relations">
          <p>已记录关联签名阶段：{{ reworkEvidence.reworkSourceRelations!.recorded ? '是' : '否' }}。关联文件以原申请固定版本展示。</p>
          <p>原始上传关联</p>
          <el-table :data="reworkEvidence.reworkSourceRelations!.originalUploadSelections" size="small">
            <el-table-column prop="fileNumber" label="文件编号" /><el-table-column prop="fileName" label="文件名称" />
            <el-table-column prop="versionNo" label="版本" />
            <el-table-column prop="origin" label="来源" /><el-table-column prop="supplemental" label="补充关联" />
          </el-table>
          <template v-for="stage in reworkEvidence.reworkSourceRelations!.stages" :key="stage.snapshot.header.id">
            <p>签名凭证 {{ stage.snapshot.header.signatureId }} · {{ stage.snapshot.header.signedAt }}
              · {{ stage.signatureEvidenceStatus }} · 签名身份匹配：{{ stage.signatureIdentityMatches ? '是' : '否' }}</p>
            <el-table :data="stage.snapshot.members" size="small">
              <el-table-column prop="fileNumberSnapshot" label="文件编号" /><el-table-column prop="fileNameSnapshot" label="文件名称" />
              <el-table-column prop="versionNoSnapshot" label="版本" />
              <el-table-column prop="origin" label="来源" /><el-table-column prop="supplemental" label="补充关联" />
            </el-table>
          </template>
          <p>完整关联集合</p>
          <el-table data-testid="dcc-obsolete-source-complete-relations" :data="reworkEvidence.reworkSourceRelations!.completeSelections" size="small">
            <el-table-column prop="fileNumber" label="文件编号" /><el-table-column prop="fileName" label="文件名称" />
            <el-table-column prop="versionNo" label="版本" />
            <el-table-column prop="origin" label="来源" /><el-table-column prop="supplemental" label="补充关联" />
          </el-table>
        </el-collapse-item>
      </el-collapse>
      <template v-if="session.draft?.approvalMatrixAfter">
        <p class="font-600">本轮原始审批规则（{{ session.draft.approvalMatrixChanged ? '存在变化，请逐项核对' : '原配置未变化' }}）</p>
        <el-collapse>
          <el-collapse-item v-if="session.draft.approvalMatrixBefore" title="原规则" name="before">
            <el-table :data="session.draft.approvalMatrixBefore.nodes" size="small">
              <el-table-column prop="stageType" label="阶段" />
              <el-table-column prop="subjectName" label="参与对象" />
              <el-table-column prop="marker" label="参与标记" />
              <el-table-column prop="approveMethod" label="批准方式" />
              <el-table-column prop="approveRatio" label="比例" />
              <el-table-column prop="ruleRemark" label="原规则说明" min-width="220" />
            </el-table>
          </el-collapse-item>
          <el-collapse-item title="本次规则" name="after">
            <el-table :data="session.draft.approvalMatrixAfter.nodes" size="small">
              <el-table-column prop="stageType" label="阶段" />
              <el-table-column prop="subjectName" label="参与对象" />
              <el-table-column prop="marker" label="参与标记" />
              <el-table-column prop="approveMethod" label="批准方式" />
              <el-table-column prop="approveRatio" label="比例" />
              <el-table-column prop="ruleRemark" label="原规则说明" min-width="220" />
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </template>
    </section>
    <template #footer>
      <el-button :disabled="busy" @click="close(false)">取消</el-button>
      <el-button v-if="session.source" data-testid="dcc-obsolete-reopen" :loading="busy"
        :disabled="loading || busy || session.sourceRefreshRequired || !reason.trim()" @click="reopen">重新发起作废申请</el-button>
      <el-button data-testid="dcc-obsolete-save-draft" :loading="busy"
        :disabled="loading || busy || !!session.source || session.sourceSelectionRequired || session.sourceRefreshRequired || !reason.trim()" @click="saveDraft">保存草稿</el-button>
      <el-button :disabled="loading || busy || !inputsSaved" @click="preview">核对审批规则</el-button>
      <el-button :disabled="loading || busy || !inputsSaved || !session.draft?.approvalMatrixAfter" @click="prepare">准备审批</el-button>
      <el-button data-testid="dcc-obsolete-submit" type="primary" :loading="busy" :disabled="!canSubmit" @click="submit">确认并提交</el-button>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { ref, reactive, computed, watch, onBeforeUnmount } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import { obsoleteControlledFile, getControlledFileObsoleteApplicationDraft, type ControlledFileVO,
  type ControlledFileObsoleteReworkSourceVO } from '@/api/dcc/controlledFile/workflow'
import { generateUUID } from '@/utils'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'
import ProjectAttributesFields from '../project-attributes/ProjectAttributesFields.vue'
import { validateAttributes, type ProjectAttributes, type AttributeSnapshot } from '../project-attributes/state'
import { ObsoleteApplicationSession, type ObsoleteApplicationInput, type ObsoleteMatrixSelection } from './obsolete-application'
const props = defineProps<{ modelValue: boolean; file: ControlledFileVO; allowed: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; submitted: [] }>()
const userStore = useUserStore()
const snapshot = ref<AttributeSnapshot>()
const departments = ref<DeptVO[]>([])
const selectedDepartments = ref<string[]>([])
const reason = ref(''), error = ref(''), loading = ref(false), pending = ref(false)
let generation = 0
const scopeKey = () => JSON.stringify([generation, props.modelValue, String(props.file.id), props.file.versionNo, String(props.file.dccProjectCodeId || ''),
  String(userStore.getUser?.id || ''), String(getVisitTenantId() || getTenantId() || '')])
const session = reactive(new ObsoleteApplicationSession(
  () => ({ key: scopeKey(), fileId: String(props.file.id), projectId: String(props.file.dccProjectCodeId || ''), versionNo: props.file.versionNo,
    actorId: String(userStore.getUser?.id || ''), tenantId: String(getVisitTenantId() || getTenantId() || '') }),
  getControlledFileObsoleteApplicationDraft, obsoleteControlledFile, generateUUID,
  () => ({ reason: reason.value.trim(), selectedSignoffDepartmentIds: [...selectedDepartments.value], projectAttributes: snapshot.value?.actual })))
const busy = computed(() => pending.value || session.busy)
const sourceStatus = (status: 'REJECTED' | 'ABANDONED') => status === 'REJECTED' ? '已拒绝' : '已放弃'
const reworkEvidence = computed(() => {
  const value = session.source || session.draft
  return value?.reworkSourceApprovalMatrix && value.reworkSourceRelations ? value : undefined
})
const input = (): ObsoleteApplicationInput => ({
  reason: reason.value.trim(), selectedSignoffDepartmentIds: [...selectedDepartments.value],
  ...(snapshot.value ? { projectAttributes: validateAttributes(snapshot.value.actual) } : {})
})
const inputsSaved = computed(() => !session.sourceRefreshRequired && !!snapshot.value && session.isSaved({ reason: reason.value.trim(),
  selectedSignoffDepartmentIds: [...selectedDepartments.value], projectAttributes: snapshot.value.actual }))
const canSubmit = computed(() => !loading.value && !busy.value && !session.sourceRefreshRequired && !!session.draft?.preparationReady && inputsSaved.value)
const adoptSaved = () => {
  const draft = session.draft || session.source
  if (!draft) return
  reason.value = draft.reason
  if (!Array.isArray(draft.selectedSignoffDepartmentIds)) throw new Error('正式作废申请会签部门事实缺失')
  selectedDepartments.value = [...draft.selectedSignoffDepartmentIds]
  snapshot.value = { projectId: draft.dccProjectCodeId, applicationType: 'OBSOLETE',
    defaultSource: JSON.parse(JSON.stringify(draft.defaultSource)), actual: JSON.parse(JSON.stringify(draft.actualAttributes)) }
}
const editAttributes = (actual: ProjectAttributes) => {
  if (snapshot.value) snapshot.value = { ...snapshot.value, actual }
}
const fingerprint = () => JSON.stringify([reason.value, selectedDepartments.value, snapshot.value])
const requireEligible = () => {
  if (!props.allowed || !props.file.dccProjectCodeId) throw new Error('当前文件不具备作废申请资格或正式项目身份')
  if (!reason.value.trim()) throw new Error('请填写作废原因')
  if (selectedDepartments.value.some(id => !departments.value.some(department => String(department.id) === id)))
    throw new Error('本次会签部门包含失效条目，请重新选择')
}
watch(() => [props.modelValue, String(props.file.id), props.file.versionNo, String(props.file.dccProjectCodeId || ''), props.allowed,
  String(userStore.getUser?.id || ''), String(getVisitTenantId() || getTenantId() || '')], async ([visible]) => {
  const token = ++generation
  session.clear(); snapshot.value = undefined; selectedDepartments.value = []; reason.value = ''; error.value = ''; pending.value = false
  if (!visible) return
  loading.value = true
  try {
    if (!props.allowed || !props.file.dccProjectCodeId) throw new Error('当前文件不具备作废申请资格或正式项目身份')
    const [loaded, rows] = await Promise.all([session.load(), getSimpleDeptList()])
    if (token !== generation) return
    if (!loaded) throw new Error(session.error || '作废草稿读取失败')
    departments.value = rows
    adoptSaved()
  } catch (cause) { if (token === generation) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (token === generation) loading.value = false }
}, { immediate: true, flush: 'sync' })
const perform = async (phase: 'OPEN' | 'PREVIEW' | 'SAVE' | 'SUBMIT', prepareExecution?: boolean, selection?: ObsoleteMatrixSelection) => {
  const token = generation, key = scopeKey(), original = fingerprint()
  error.value = ''
  try {
    requireEligible()
    const accepted = await session.execute(phase, input(), prepareExecution, selection)
    if (token !== generation || key !== scopeKey()) return false
    if (original !== fingerprint()) { session.invalidateInputs(); throw new Error('申请内容已改变，请重新读取已保存草稿并核对') }
    if (!accepted) throw new Error(session.error || '作废阶段未完成')
    adoptSaved()
    return true
  } catch (cause) { if (token === generation) error.value = cause instanceof Error ? cause.message : String(cause); return false }
}
const saveDraft = async () => {
  if (busy.value || loading.value || session.source || session.sourceSelectionRequired || session.sourceRefreshRequired) return
  const key = scopeKey()
  pending.value = true
  try { await perform(session.draft ? 'SAVE' : 'OPEN', false) }
  finally { if (key === scopeKey()) pending.value = false }
}
const selectSource = async (candidate: ControlledFileObsoleteReworkSourceVO) => {
  if (busy.value || loading.value || session.sourceRefreshRequired || !candidate.available) return
  const key = scopeKey(), original = fingerprint()
  pending.value = true; error.value = ''
  try {
    const accepted = await session.selectSource(candidate)
    if (key !== scopeKey()) return
    if (original !== fingerprint()) { session.invalidateInputs(); throw new Error('申请内容已改变，请重新读取来源') }
    if (!accepted) throw new Error(session.error || '作废来源读取失败')
    adoptSaved()
  } catch (cause) { if (key === scopeKey()) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (key === scopeKey()) pending.value = false }
}
const reloadEntry = async () => {
  if (busy.value || loading.value) return
  const key = scopeKey(), original = fingerprint()
  loading.value = true; error.value = ''
  try {
    const accepted = await session.load()
    if (key !== scopeKey()) return
    if (!accepted) throw new Error(session.error || '作废来源重新读取失败')
    if (original !== fingerprint()) throw new Error('申请内容已改变，请重新核对读取结果')
    // Keep the user's edited inputs; reading a different source never submits or silently replaces them.
  } catch (cause) { if (key === scopeKey()) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (key === scopeKey()) loading.value = false }
}
const reopen = async () => {
  if (busy.value || loading.value || !session.source || session.sourceRefreshRequired) return
  const source = session.source, key = scopeKey(), original = fingerprint(), sourceDigest = source.reworkSourceDigest
  pending.value = true; error.value = ''
  try {
    requireEligible()
    await ElMessageBox.confirm(`确认以 ${source.instanceCode} / ${source.versionNo} 的已终止申请重新发起作废？将使用下方本次原因、会签部门和实际属性创建新草稿，原申请及原签名保持只读。`,
      '确认重新发起作废申请', { type: 'warning' })
    if (key !== scopeKey()) return
    if (original !== fingerprint() || session.source?.reworkSourceDigest !== sourceDigest) throw new Error('申请内容或来源已改变，请重新核对')
    await perform('OPEN')
  } catch (cause) { if (cause !== 'cancel' && cause !== 'close' && key === scopeKey()) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (key === scopeKey()) pending.value = false }
}
const preview = async () => {
  if (busy.value || !inputsSaved.value) return
  const key = scopeKey()
  pending.value = true
  try { await perform('PREVIEW') }
  finally { if (key === scopeKey()) pending.value = false }
}
const prepare = async () => {
  if (busy.value || !inputsSaved.value || !session.draft?.approvalMatrixAfter) return
  const draft = session.draft, key = scopeKey(), original = fingerprint()
  const after = draft.approvalMatrixAfter
  pending.value = true
  try {
    requireEligible()
    await ElMessageBox.confirm(draft.approvalMatrixChanged
      ? '请核对本轮原规则与本次规则；确认后按本次规则准备审批。' : '确认按显示的本轮审批规则准备？',
    '确认审批准备', { type: 'warning' })
    if (key !== scopeKey() || original !== fingerprint()) throw new Error('申请内容已改变，请重新核对')
    await perform('SAVE', true, {
      expectedApprovalRouteId: after.routeId, expectedApprovalRouteVersionNo: after.routeVersionNo,
      expectedApprovalMatrixDigest: after.matrixDigest, approvalMatrixBaselineToken: draft.approvalMatrixBaselineToken || undefined,
      approvalMatrixConfirmationToken: draft.approvalMatrixChanged ? draft.approvalMatrixConfirmationToken : null,
      matrixChangeConfirmed: draft.approvalMatrixChanged
    })
  } catch (cause) { if (cause !== 'cancel' && cause !== 'close' && key === scopeKey()) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (key === scopeKey()) pending.value = false }
}
const submit = async () => {
  if (!canSubmit.value) return
  const key = scopeKey(), original = fingerprint()
  pending.value = true
  try {
    requireEligible()
    const actual = validateAttributes(snapshot.value!.actual)
    const choice = (value: string) => {
      if (value === 'Y') return '是'
      if (value === 'N') return '否'
      if (value === 'NA') return '不适用'
      throw new Error('本次实际属性包含无效选项，请重新核对')
    }
    const attributeSummary = `目标市场：${actual.targetMarkets.join('、')}${actual.otherMarket ? `（其他市场说明：${actual.otherMarket}）` : ''}；是否为注册人：${choice(actual.licenseHolder)}；是否为生产方：${choice(actual.actualManufacturer)}；文件转移：${choice(actual.documentTransfer)}${actual.transferTo ? `，转移至：${actual.transferTo}` : ''}`
    await ElMessageBox.confirm(`确认提交作废申请：${props.file.fileNumber} / ${props.file.versionNo}；原因：${reason.value}；会签部门：${selectedDepartments.value.map(id => departments.value.find(row => String(row.id) === id)?.name || id).join('、')}；${attributeSummary}。直属主管、会签及批准完成后作废。`,
      '确认提交作废申请', { type: 'warning' })
    if (key !== scopeKey() || original !== fingerprint()) throw new Error('申请内容已改变，请重新保存并确认')
    if (await perform('SUBMIT')) { emit('submitted'); emit('update:modelValue', false) }
  } catch (cause) { if (cause !== 'cancel' && cause !== 'close' && key === scopeKey()) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (key === scopeKey()) pending.value = false }
}
const beforeClose = (done: () => void) => { if (!busy.value) { generation++; session.clear(); done() } }
const close = (value: boolean) => { if (!busy.value) { generation++; session.clear(); emit('update:modelValue', value) } }
onBeforeUnmount(() => { generation++; session.clear() })
</script>
