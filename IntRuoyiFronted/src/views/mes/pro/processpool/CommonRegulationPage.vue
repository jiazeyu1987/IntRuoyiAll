<template>
  <div class="common-regulation-page" data-common-regulation-editor>
    <ContentWrap class="common-regulation-page__header-wrap">
      <div class="common-regulation-page__header">
        <h2>通用检验规程</h2>
        <div class="common-regulation-page__selector">
          <el-select
            :model-value="selectedRegulationId"
            aria-label="已发布通用检验规程"
            placeholder="请选择已发布通用检验规程"
            filterable
            clearable
            :loading="listLoading"
            :disabled="busy || !initialized"
            @change="selectRegulation"
          >
            <el-option
              v-for="regulation in regulations"
              :key="regulation.commonRegulationId"
              :value="regulation.commonRegulationId"
              :label="`${regulation.commonRegulationCode} / ${regulation.commonRegulationName}`"
            />
          </el-select>
          <el-select
            v-if="loaded"
            :model-value="loaded.publishedVersionId"
            aria-label="通用规程版本"
            :disabled="busy"
            @change="selectVersion"
          >
            <el-option
              v-for="version in versions"
              :key="version.versionId"
              :value="version.versionId"
              :label="`${version.versionNo} / ${statusText(version.lifecycleStatus)}`"
            />
          </el-select>
          <el-button :disabled="busy" :loading="listLoading" @click="refresh">刷新</el-button>
        </div>
        <div class="common-regulation-page__actions">
          <el-button v-hasPermi="['mes:qc-template:update']" :disabled="busy" @click="openParser">
            导入通用
          </el-button>
          <el-button v-if="draft" :disabled="busy" @click="copyCode">复制编号</el-button>
          <el-button v-if="editable" :disabled="busy" :loading="saving" @click="saveDraft">保存草稿</el-button>
          <el-button v-if="editable" type="primary" :disabled="busy" @click="previewPublish">发布</el-button>
        </div>
      </div>
      <el-alert
        v-if="error"
        class="mt-12px"
        :title="error"
        type="error"
        show-icon
        :closable="false"
      />
    </ContentWrap>
    <ContentWrap v-if="draft" class="common-regulation-page__tabs">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="总览" name="overview" />
        <el-tab-pane label="检验项目" name="items" />
      </el-tabs>
      <el-alert
        v-if="historical"
        title="当前为历史版本，仅供查看；请选择当前发布版本或草稿进行维护。"
        type="info"
        show-icon
        :closable="false"
      />
      <el-alert
        v-else-if="otherDraft"
        title="该规程已有未发布草稿，请继续草稿后保存或发布。"
        type="info"
        show-icon
        :closable="false"
      >
        <el-button link type="primary" @click="selectVersion(otherDraft.versionId)"
          >继续草稿 {{ otherDraft.versionNo }}</el-button
        >
      </el-alert>
      <el-alert
        v-else-if="loaded?.lifecycleStatus === 'DRAFT'"
        :title="`正在编辑未发布草稿 ${loaded.versionNo}；当前正式版本仍为 ${currentPublished?.versionNo || '无'}。`"
        type="info"
        show-icon
        :closable="false"
      />
      <div v-else-if="editable" class="common-regulation-page__revision">
        <span>修改后另存为新版本</span>
        <el-input v-model="draft.versionNo" aria-label="待发布版本号" :disabled="busy" />
        <span v-if="dirty">有未保存修改</span>
      </div>
    </ContentWrap>
    <ContentWrap v-if="!draft" v-loading="loading">
      <el-empty description="请选择已发布通用检验规程；新规程可通过简易通用解析导入发布。" />
    </ContentWrap>
    <ContentWrap v-else v-loading="loading">
      <el-form
        v-if="activeTab === 'overview'"
        :disabled="!editable || busy"
        label-width="100px"
        class="common-regulation-page__overview"
      >
        <h3>规程信息</h3>
        <el-form-item label="规程编号"
          ><el-input v-model="draft.regulationCode" aria-label="通用规程编号" disabled
        /></el-form-item>
        <el-form-item label="规程名称"
          ><el-input v-model="draft.regulationName" aria-label="通用规程名称"
        /></el-form-item>
        <el-form-item label="编辑版本"
          ><el-input
            v-model="draft.versionNo"
            aria-label="通用规程编辑版本"
            :disabled="loaded?.lifecycleStatus === 'DRAFT'"
        /></el-form-item>
        <el-form-item label="检验规则">
          <div class="common-regulation-page__rules">
            <div v-for="rule in draft.inspectionTypeRules" :key="rule.key">
              <el-switch
                v-model="rule.required"
                :active-text="rule.label"
                :disabled="rule.key === 'FINAL'"
              />
              <el-input v-model="rule.roundLabel" :aria-label="`${rule.label}轮次`" />
              <el-input
                v-if="rule.key === 'FINAL' && rule.required"
                v-model.number="rule.fixedQuantity"
                aria-label="末检固定数量"
                type="number"
                min="1"
              />
            </div>
          </div>
        </el-form-item>
        <el-form-item label="内容统计"
          >{{ draft.processes.length }} 道工序，{{ itemCount }} 个检验项目</el-form-item
        >
      </el-form>
      <RegulationItemsEditor
        v-else
        v-model:draft="draft"
        :disabled="!editable || busy"
        :machinery="machinery"
      />
    </ContentWrap>
    <el-dialog
      v-model="publishVisible"
      title="发布通用检验规程"
      width="620px"
      :close-on-click-modal="false"
    >
      <template v-if="draft">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="规程"
            >{{ draft.regulationCode }} / {{ draft.regulationName }}</el-descriptions-item
          >
          <el-descriptions-item label="当前发布">{{
            currentPublished?.versionNo
          }}</el-descriptions-item>
          <el-descriptions-item label="本次发布">{{ draft.versionNo }}</el-descriptions-item>
          <el-descriptions-item label="检验内容"
            >{{ draft.processes.length }} 道工序，{{ itemCount }} 个检验项目</el-descriptions-item
          >
        </el-descriptions>
        <p>发布后保留历史版本。产品QA已有绑定和已生成PQC任务继续使用原版本。</p>
      </template>
      <template #footer>
        <el-button :disabled="publishing" @click="publishVisible = false">返回修改</el-button>
        <el-button type="primary" :loading="publishing" @click="publish">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useClipboard } from '@vueuse/core'
import { hasPermission } from '@/directives/permission/hasPermi'
import {
  QcTemplateApi,
  type QaCommonRegulationVersionOptionVO,
  type QaInspectionRegulationVersionOptionVO,
  type QaInspectionRegulationPublishedVersionVO
} from '@/api/mes/qc/template'
import * as MachineryApi from '@/api/mes/dv/machinery'
import type { Draft } from '@/views/form-center/parser/simplePqc'
import RegulationItemsEditor from './RegulationItemsEditor.vue'
import {
  createCommonEditorDraft,
  commonEditorIssues,
  normalizeCommonEditorDraft
} from './commonRegulationEditor'

defineOptions({ name: 'MesProProcessPoolCommonRegulation' })
const router = useRouter()
const route = useRoute()
const { copy } = useClipboard()
const regulations = ref<QaCommonRegulationVersionOptionVO[]>([])
const versions = ref<QaInspectionRegulationVersionOptionVO[]>([])
const selectedRegulationId = ref<number>()
const loaded = ref<QaInspectionRegulationPublishedVersionVO>()
const draft = ref<Draft>()
const machinery = ref<MachineryApi.DvMachineryVO[]>([])
const activeTab = ref('overview')
const listLoading = ref(false)
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const publishVisible = ref(false)
const error = ref('')
const baseline = ref('')
const busy = computed(() => loading.value || listLoading.value || saving.value || publishing.value)
const dirty = computed(() => !!draft.value && JSON.stringify(draft.value) !== baseline.value)
const currentPublished = computed(() => versions.value.find((version) => version.currentPublished))
const historical = computed(() => loaded.value?.lifecycleStatus === 'RETIRED')
const otherDraft = computed(() =>
  versions.value.find(
    (version) =>
      version.lifecycleStatus === 'DRAFT' && version.versionId !== loaded.value?.publishedVersionId
  )
)
const editable = computed(
  () =>
    initialized.value &&
    !!loaded.value &&
    !historical.value &&
    !otherDraft.value &&
    hasPermission(['mes:qc-template:update'])
)
const itemCount = computed(
  () => draft.value?.processes.reduce((count, process) => count + process.items.length, 0) || 0
)
const statusText = (status: string) =>
  (({ DRAFT: '草稿', PUBLISHED: '已发布', RETIRED: '历史版本' }) as Record<string, string>)[
    status
  ] || status
const errorMessage = (cause: unknown) => (cause instanceof Error ? cause.message : String(cause))
const initialized = ref(false)

async function confirmDiscard() {
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('当前修改尚未保存，确认放弃修改？', '切换规程内容', {
      confirmButtonText: '放弃修改',
      cancelButtonText: '继续编辑',
      type: 'warning',
      distinguishCancelAndClose: true
    })
    return true
  } catch (cause) {
    if (cause === 'cancel' || cause === 'close') return false
    throw cause
  }
}
async function loadList() {
  regulations.value = await QcTemplateApi.listCommonRegulationPublishedVersions()
}
async function loadSelected(regulationId: number, versionId?: number) {
  loading.value = true
  error.value = ''
  try {
    const [options, content] = await Promise.all([
      QcTemplateApi.listCommonRegulationVersions(regulationId),
      QcTemplateApi.getCommonRegulationEditorVersion(regulationId, versionId)
    ])
    if (
      content.regulationId !== regulationId ||
      (versionId && content.publishedVersionId !== versionId)
    )
      throw new Error('返回规程或版本与所选内容不一致')
    versions.value = options
    loaded.value = content
    draft.value = createCommonEditorDraft(content, options)
    baseline.value = JSON.stringify(draft.value)
    selectedRegulationId.value = regulationId
    await router.replace({ query: { ...route.query, regulationId: String(regulationId) } })
  } catch (cause) {
    error.value = '通用规程加载失败：' + errorMessage(cause)
    draft.value = undefined
    loaded.value = undefined
    versions.value = []
  } finally {
    loading.value = false
  }
}
async function selectRegulation(regulationId?: number) {
  if (busy.value || regulationId === selectedRegulationId.value || !(await confirmDiscard())) return
  if (regulationId) await loadSelected(regulationId)
  else {
    selectedRegulationId.value = undefined
    draft.value = undefined
    loaded.value = undefined
    versions.value = []
    baseline.value = ''
    const query = { ...route.query }
    delete query.regulationId
    await router.replace({ query })
  }
}
async function selectVersion(versionId: number) {
  if (busy.value || versionId === loaded.value?.publishedVersionId || !(await confirmDiscard()))
    return
  await loadSelected(selectedRegulationId.value!, versionId)
}
function validPayload() {
  if (!editable.value || !draft.value) throw new Error('请选择可编辑的当前版本或草稿')
  const payload = normalizeCommonEditorDraft(draft.value)
  const issues = commonEditorIssues(payload)
  if (
    loaded.value?.lifecycleStatus !== 'DRAFT' &&
    versions.value.some((version) => version.versionNo === payload.versionNo)
  )
    issues.unshift('版本号已存在，请使用新的版本号')
  if (issues.length) throw new Error(issues.slice(0, 6).join('；'))
  return payload
}
async function saveDraft() {
  let payload: Draft
  try {
    payload = validPayload()
  } catch (cause) {
    ElMessage.error(errorMessage(cause))
    return
  }
  saving.value = true
  error.value = ''
  let committed = false
  try {
    const result = await QcTemplateApi.saveQaRegulationDraft(payload)
    committed = true
    await loadSelected(result.regulationId, result.draftVersionId)
    if (error.value) throw new Error(error.value)
    ElMessage.success(`通用规程草稿 ${result.versionNo} 已保存`)
  } catch (cause) {
    error.value =
      (committed ? '草稿已保存，但重新加载失败：' : '保存草稿失败：') + errorMessage(cause)
    ElMessage.error(error.value)
  } finally {
    saving.value = false
  }
}
function previewPublish() {
  try {
    validPayload()
    publishVisible.value = true
  } catch (cause) {
    ElMessage.error(errorMessage(cause))
  }
}
async function publish() {
  if (publishing.value) return
  let payload: Draft
  try {
    payload = validPayload()
  } catch (cause) {
    ElMessage.error(errorMessage(cause))
    return
  }
  publishing.value = true
  error.value = ''
  let committed = false
  try {
    const result = await QcTemplateApi.publishQaRegulation(payload)
    committed = true
    publishVisible.value = false
    await loadList()
    await loadSelected(result.regulationId, result.publishedVersionId)
    if (error.value) throw new Error(error.value)
    ElMessage.success(`通用检验规程 ${result.versionNo} 已发布`)
  } catch (cause) {
    error.value = (committed ? '规程已发布，但重新加载失败：' : '发布失败：') + errorMessage(cause)
    ElMessage.error(error.value)
  } finally {
    publishing.value = false
  }
}
async function refresh() {
  if (busy.value || !(await confirmDiscard())) return
  if (!initialized.value) {
    await initialize()
    return
  }
  listLoading.value = true
  error.value = ''
  try {
    await loadList()
    const regulationId = route.query.regulationId
      ? Number(route.query.regulationId)
      : selectedRegulationId.value
    if (regulationId) {
      if (!regulations.value.some((regulation) => regulation.commonRegulationId === regulationId))
        throw new Error('指定的通用规程未发布或无权查看')
      await loadSelected(regulationId)
    }
  } catch (cause) {
    error.value = '规程目录加载失败：' + errorMessage(cause)
  } finally {
    listLoading.value = false
  }
}
async function copyCode() {
  if (!draft.value) return
  try {
    await copy(draft.value.regulationCode)
    ElMessage.success('规程编号已复制')
  } catch (cause) {
    ElMessage.error('复制失败：' + errorMessage(cause))
  }
}
function openParser() {
  void router.push('/mdm/form-center/simple-common-parser')
}
async function initialize() {
  initialized.value = false
  error.value = ''
  listLoading.value = true
  try {
    await loadList()
    const options: MachineryApi.DvMachineryVO[] = []
    let pageNo = 1
    while (true) {
      const page = await MachineryApi.DvMachineryApi.getMachineryPage({ pageNo, pageSize: 100 })
      if (!Array.isArray(page?.list) || !Number.isFinite(Number(page.total)))
        throw new Error('设备目录分页数据无效')
      options.push(...page.list)
      if (options.length >= Number(page.total)) break
      if (!page.list.length) throw new Error('设备目录分页提前结束')
      pageNo++
    }
    machinery.value = options
    if (route.query.regulationId) {
      const regulationId = Number(route.query.regulationId)
      if (!regulations.value.some((regulation) => regulation.commonRegulationId === regulationId))
        throw new Error('指定的通用规程未发布或无权查看')
      await loadSelected(regulationId)
    }
    initialized.value = true
  } catch (cause) {
    error.value = '通用规程初始化失败：' + errorMessage(cause)
  } finally {
    listLoading.value = false
  }
}
onMounted(initialize)
watch(
  () => route.query.regulationId,
  (regulationId) => {
    if (
      route.path === '/mes/pro/process-pool/common-regulation' &&
      initialized.value &&
      !busy.value &&
      regulationId &&
      Number(regulationId) !== selectedRegulationId.value
    )
      void refresh()
  }
)
onActivated(() => {
  if (initialized.value && !busy.value && !dirty.value) void refresh()
})
onBeforeRouteLeave(async () => !busy.value && (await confirmDiscard()))
</script>

<style scoped>
.common-regulation-page {
  display: grid;
  gap: 0;
}
.common-regulation-page__header-wrap,
.common-regulation-page__tabs {
  margin-bottom: 0 !important;
}
.common-regulation-page__header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}
.common-regulation-page h2 {
  margin: 0;
  font-size: 20px;
  color: #172033;
}
.common-regulation-page__selector {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 1 520px;
  min-width: 260px;
}
.common-regulation-page__selector .el-select {
  flex: 1;
  min-width: 0;
}
.common-regulation-page__published {
  margin-left: auto;
  font-size: 13px;
}
.common-regulation-page__published strong {
  margin-left: 8px;
}
.common-regulation-page__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  width: 100%;
}
.common-regulation-page__actions label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  white-space: nowrap;
}
.common-regulation-page__actions .el-select {
  width: 190px;
}
.common-regulation-page__actions :deep(.el-date-editor) {
  width: 180px;
}
.common-regulation-page__actions .el-button + .el-button {
  margin-left: 0;
}
.common-regulation-page__revision {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 13px;
}
.common-regulation-page__revision .el-input {
  width: 120px;
}
.common-regulation-page__overview {
  max-width: 1000px;
}
.common-regulation-page__rules {
  display: grid;
  gap: 8px;
  width: 100%;
}
.common-regulation-page__rules > div {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.common-regulation-page__rules .el-switch {
  width: 120px;
}
.common-regulation-page__rules .el-input {
  width: 250px;
}
@media (max-width: 640px) {
  .common-regulation-page__selector {
    flex-basis: 100%;
  }
  .common-regulation-page__published {
    margin-left: 0;
  }
  .common-regulation-page__actions {
    justify-content: flex-start;
  }
}
</style>
