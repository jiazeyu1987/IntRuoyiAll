<template>
  <ContentWrap v-loading="busy">
    <div class="toolbar">
      <h2>简易通用解析</h2>
      <el-select
        v-model="projectId"
        aria-label="规程所属项目"
        placeholder="选择规程所属项目"
        filterable
        remote
        :remote-method="searchProjects"
        :disabled="!!draft && !published"
        style="width: 320px"
      >
        <el-option
          v-for="p in projects"
          :key="p.id"
          :value="p.id"
          :label="`${p.projectCode} / ${p.projectName}`"
        />
      </el-select>
      <el-upload
        :auto-upload="false"
        :show-file-list="false"
        accept=".docx"
        :on-change="importWord"
        :disabled="!projectId || busy"
      >
        <el-button :disabled="!projectId || busy">解析通用规程</el-button>
      </el-upload>
      <el-button :disabled="!draft || published" @click="saveLocal">保存草稿</el-button>
      <el-button @click="restoreLocal">恢复草稿</el-button>
      <el-button @click="router.push('/mes/pro/process-pool/common-regulation')"
        >管理通用规程套</el-button
      >
      <el-button
        v-hasPermi="['mes:qc-template:update']"
        type="primary"
        :disabled="!draft || published"
        @click="preview"
        >预览发布</el-button
      >
    </div>
    <el-alert
      title="解析、修改和预览均不会导入正式规程；确认发布后，再到通用检验规程页面将文档加入对应规程套。"
      type="info"
      :closable="false"
    />
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-alert
      v-if="published"
      title="已正式导入并发布通用检验规程，可前往通用检验规程页面维护规程套和产品关联。"
      type="success"
      :closable="false"
    />
    <el-empty
      v-if="!draft"
      description="选择规程所属项目，解析 Word 后编辑并预览，确认发布后才正式导入"
    />
    <template v-else>
      <el-radio-group v-model="viewMode" aria-label="解析查看模式">
        <el-radio-button value="edit">编辑内容</el-radio-button>
        <el-radio-button value="trial">试填预览</el-radio-button>
      </el-radio-group>
      <SimplePqcTrialPreview
        v-if="viewMode === 'trial'"
        :draft="draft"
        :process-index="processIndex"
        @select-process="selectProcess"
      />
      <el-form v-show="viewMode === 'edit'" :disabled="published" label-position="top">
        <div ref="metadataEditor" class="metadata">
          <el-form-item label="规程名称"
            ><el-input v-model="draft.regulationName" aria-label="规程名称"
          /></el-form-item>
          <el-form-item label="文件编号"
            ><el-input v-model="draft.regulationCode" aria-label="文件编号"
          /></el-form-item>
          <el-form-item label="发布版本"
            ><el-input v-model="draft.versionNo" aria-label="发布版本"
          /></el-form-item>
          <el-form-item label="生效日期"
            ><el-date-picker v-model="draft.effectiveDate" value-format="YYYY-MM-DD"
          /></el-form-item>
        </div>
        <div class="workspace">
          <aside>
            <el-button @click="addProcess">添加工序</el-button>
            <div v-for="(p, pi) in draft.processes" :key="p.processCode" class="process">
              <button
                type="button"
                :class="{ active: pi === processIndex }"
                @click="selectProcess(pi)"
                >{{ pi + 1 }}. {{ p.processName }}（{{ p.items.length }}项）</button
              >
              <template v-if="pi === processIndex">
                <button
                  v-for="(i, ii) in p.items"
                  :key="i.itemCode"
                  type="button"
                  :class="{ active: ii === itemIndex }"
                  @click="itemIndex = ii"
                  >{{ ii + 1 }}. {{ i.itemName || '新检验项目' }}</button
                >
              </template>
            </div>
          </aside>
          <section v-if="process" ref="itemEditor">
            <div class="toolbar">
              <el-input v-model="process.processName" aria-label="工序名称" style="width: 240px" />
              <el-button @click="addItem">添加检验项目</el-button>
              <el-button :disabled="processIndex === 0" @click="moveProcess(-1)"
                >工序上移</el-button
              >
              <el-button
                :disabled="processIndex === draft.processes.length - 1"
                @click="moveProcess(1)"
                >工序下移</el-button
              >
              <el-button type="danger" plain @click="removeProcess">删除工序</el-button>
            </div>
            <template v-if="item">
              <div class="toolbar">
                <el-button :disabled="itemIndex === 0" @click="moveItem(-1)">项目上移</el-button>
                <el-button :disabled="itemIndex === process.items.length - 1" @click="moveItem(1)"
                  >项目下移</el-button
                >
                <el-select
                  placeholder="移动到其他工序"
                  aria-label="移动到其他工序"
                  @change="transferItem"
                >
                  <el-option
                    v-for="(p, pi) in draft.processes"
                    :key="p.processCode"
                    :label="p.processName"
                    :value="pi"
                    :disabled="pi === processIndex"
                  />
                </el-select>
                <el-button type="danger" plain @click="removeItem">删除项目</el-button>
              </div>
              <el-form-item label="检验项目"
                ><el-input v-model="item.itemName" aria-label="检验项目"
              /></el-form-item>
              <el-form-item label="接受标准"
                ><el-input
                  v-model="item.standardText"
                  aria-label="接受标准"
                  type="textarea"
                  :autosize="{ minRows: 3, maxRows: 10 }"
              /></el-form-item>
              <el-form-item label="检验方法"
                ><el-input
                  v-model="item.inspectionMethod"
                  aria-label="检验方法"
                  type="textarea"
                  :autosize="{ minRows: 3, maxRows: 10 }"
              /></el-form-item>
              <div class="metadata">
                <el-form-item label="结果填写方式"
                  ><el-select v-model="item.resultType"
                    ><el-option label="合格 / 不合格" value="BOOLEAN" /><el-option
                      label="测量数值"
                      value="NUMERIC" /><el-option label="文字记录" value="TEXT" /></el-select
                ></el-form-item>
                <el-form-item label="适用检验"
                  ><el-checkbox-group v-model="item.applicableInspectionTypes"
                    ><el-checkbox value="FIRST">首检</el-checkbox
                    ><el-checkbox value="PATROL">巡检</el-checkbox
                    ><el-checkbox v-if="draft.finalInspectionApplicable" value="FINAL"
                      >末检</el-checkbox
                    ></el-checkbox-group
                  ></el-form-item
                >
                <el-form-item label="关键检验项目"
                  ><el-switch v-model="item.critical"
                /></el-form-item>
              </div>
              <div v-if="item.resultType === 'NUMERIC'" class="metadata">
                <el-form-item label="下限"
                  ><el-input-number v-model="item.standardLowerLimit"
                /></el-form-item>
                <el-form-item label="上限"
                  ><el-input-number v-model="item.standardUpperLimit"
                /></el-form-item>
                <el-form-item label="单位"><el-input v-model="item.standardUnit" /></el-form-item>
                <el-form-item label="小数位数"
                  ><el-input-number v-model="item.standardPrecision" :min="0" :max="10"
                /></el-form-item>
              </div>
              <el-form-item label="抽样要求"
                ><el-input v-model="item.samplingPlanText" type="textarea"
              /></el-form-item>
              <div class="metadata">
                <el-form-item
                  v-if="item.applicableInspectionTypes.includes('FIRST')"
                  label="首检数量（件）"
                  ><el-input-number v-model="item.firstInspectionQuantity" :min="1" :precision="0"
                /></el-form-item>
                <el-form-item
                  v-if="item.applicableInspectionTypes.includes('PATROL')"
                  label="系统巡检抽样比例（%）"
                  ><el-input-number v-model="item.patrolInspectionRatio" :min="0.0001" :max="100"
                /></el-form-item>
              </div>
              <el-form-item label="检验器具及设备要求"
                ><el-input v-model="item.inspectionTool"
              /></el-form-item>
              <el-form-item ref="equipmentEditor" label="选择设备编号（名称自动带出）">
                <el-select
                  :model-value="item.equipmentOptions.map((e) => e.equipmentId)"
                  multiple
                  filterable
                  placeholder="选择设备编号"
                  aria-label="选择设备编号"
                  @change="selectEquipment"
                >
                  <el-option
                    v-for="m in machinery"
                    :key="m.id"
                    :label="`${m.code} / ${m.name}`"
                    :value="m.id"
                  />
                </el-select>
              </el-form-item>
              <el-button @click="applyEquipment">应用到相同器具要求的项目</el-button>
              <el-form-item label="不合格处理要求"
                ><el-input v-model="item.failureRule" type="textarea"
              /></el-form-item>
            </template>
          </section>
        </div>
      </el-form>
    </template>
    <el-dialog
      v-model="previewVisible"
      title="确认发布到通用检验规程"
      width="900px"
      :close-on-click-modal="false"
      @closed="focusIssue"
    >
      <template v-if="draft">
        <p>{{ selectedProject }} · {{ draft.regulationName }} · {{ draft.versionNo }}</p>
        <p>{{
          draft.regulationId
            ? '发布通用规程新版本，历史规程及已绑定套版本保留。新版本需加入对应规程套后使用。'
            : '新建通用检验规程并发布，发布后可加入对应规程套。'
        }}</p>
        <el-form :disabled="published" label-position="top">
          <el-form-item label="末检适用性">
            <el-switch
              v-model="draft.finalInspectionApplicable"
              active-text="适用末检"
              @change="syncFinal"
            />
          </el-form-item>
          <el-form-item v-if="!draft.finalInspectionApplicable" label="末检不适用依据">
            <el-input
              v-model="draft.finalInspectionNotApplicableReason"
              aria-label="末检不适用依据"
              placeholder="确认无需末检可点击下方按钮，也可填写具体依据"
            />
            <el-button class="confirm-final" @click="confirmNoFinal">确认无需末检</el-button>
          </el-form-item>
          <template v-for="rule in draft.inspectionTypeRules" :key="rule.key">
            <el-form-item
              v-if="rule.key === 'FINAL' && draft.finalInspectionApplicable"
              label="末检数量"
            >
              <el-input-number v-model="rule.fixedQuantity" :min="1" aria-label="末检数量" />
            </el-form-item>
          </template>
        </el-form>
        <section v-if="issues.length" class="publish-issues" aria-label="发布前待确认事项">
          <h3>还有 {{ issues.length }} 项需要确认</h3>
          <p>补齐后即可发布。点击“去修改”会直接打开对应位置，已修改的内容会保留。</p>
          <div v-for="(issue, index) in issues" :key="index" class="publish-issue">
            <span>{{ issue.message }}</span>
            <el-button v-if="issue.field === 'final'" size="small" @click="confirmNoFinal"
              >确认无需末检</el-button
            >
            <el-button v-else size="small" @click="editIssue(issue)">去修改</el-button>
          </div>
        </section>
        <el-collapse
          ><el-collapse-item
            v-for="p in draft.processes"
            :key="p.processCode"
            :title="`${p.processName}（${p.items.length}项）`"
          >
            <article v-for="i in p.items" :key="i.itemCode"
              ><h4>{{ i.itemName }}</h4
              ><p>标准：{{ i.standardText }}</p
              ><p>方法：{{ i.inspectionMethod }}</p
              ><p
                >填写方式：{{
                  { BOOLEAN: '合格 / 不合格', NUMERIC: '测量数值', TEXT: '文字记录' }[i.resultType]
                }}；{{ i.critical ? '关键项目' : '普通项目' }}</p
              ><p v-if="i.resultType === 'NUMERIC'"
                >数值范围：{{ i.standardLowerLimit }} ～ {{ i.standardUpperLimit }}
                {{ i.standardUnit }}；小数位数：{{ i.standardPrecision }}</p
              ><p v-if="i.failureRule">不合格处理：{{ i.failureRule }}</p
              ><p
                >抽样：{{ i.samplingPlanText }}；首检
                {{ i.firstInspectionQuantity ?? '不适用' }} 件；巡检
                {{ i.patrolInspectionRatio ?? '不适用' }}%</p
              ><p
                >设备：{{
                  i.equipmentOptions
                    .map((e) => `${e.equipmentNumber} / ${e.equipmentName}`)
                    .join('、') || i.inspectionTool
                }}</p
              ></article
            >
          </el-collapse-item></el-collapse
        >
      </template>
      <template #footer
        ><el-button @click="previewVisible = false">返回修改</el-button
        ><el-button
          type="primary"
          :loading="busy"
          :disabled="issues.length > 0 || published"
          @click="publish"
          >确认发布</el-button
        ></template
      >
    </el-dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import SimplePqcTrialPreview from '../parser/components/SimplePqcTrialPreview.vue'
import type { UploadFile } from 'element-plus'
import { SimpleCommonParserApi } from '@/api/mes/qc/template/simpleCommonParser'
import { useRouter } from 'vue-router'
import {
  getProjectCodePage,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import { DvMachineryApi, type DvMachineryVO } from '@/api/mes/dv/machinery'
import { getTenantId } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'
import { validateCommonDraft, type DraftIssue } from './draft'
import type { SimpleCommonDraft as Draft } from '@/api/mes/qc/template/simpleCommonParser'

defineOptions({ name: 'FormCenterSimpleCommonParser' })
const message = useMessage()
const router = useRouter()
const projectId = ref<number>()
const projects = ref<DccProjectCodeRespVO[]>([])
const draft = ref<Draft>()
const machinery = ref<DvMachineryVO[]>([])
const busy = ref(false)
const error = ref('')
const published = ref(false)
const previewVisible = ref(false)
const viewMode = ref<'edit' | 'trial'>('edit')
const processIndex = ref(0)
const itemIndex = ref(0)
const metadataEditor = ref<HTMLElement>()
const itemEditor = ref<HTMLElement>()
const equipmentEditor = ref<{ $el: HTMLElement }>()
const pendingIssue = ref<DraftIssue>()
const process = computed(() => draft.value?.processes[processIndex.value])
const item = computed(() => process.value?.items[itemIndex.value])
const selectedProject = computed(
  () => projects.value.find((p) => p.id === projectId.value)?.projectName
)
const issues = computed(() =>
  draft.value
    ? [
        ...validateCommonDraft(draft.value),
        ...(!draft.value.versionNo.trim()
          ? [{ message: '请填写发布版本', field: 'metadata' as const }]
          : [])
      ]
    : []
)
const copy = <T,>(value: T): T => JSON.parse(JSON.stringify(value))
async function run(action: () => Promise<void>) {
  busy.value = true
  error.value = ''
  try {
    await action()
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
    message.error(error.value)
  } finally {
    busy.value = false
  }
}
async function searchProjects(keyword = '') {
  try {
    const selected = projects.value.find((p) => p.id === projectId.value)
    const result = (
      await getProjectCodePage({ pageNo: 1, pageSize: 100, keyword, status: 'ENABLE' })
    ).list
    projects.value =
      selected && !result.some((p) => p.id === selected.id) ? [selected, ...result] : result
  } catch (e) {
    error.value = `项目列表加载失败：${String(e)}`
  }
}
async function loadResources() {
  if (!projectId.value) throw new Error('请选择规程所属项目')
  const options: DvMachineryVO[] = []
  for (let pageNo = 1; ; pageNo++) {
    const page = await DvMachineryApi.getMachineryPage({ pageNo, pageSize: 100 })
    if (!Array.isArray(page.list) || !Number.isFinite(Number(page.total)))
      throw new Error('设备台账返回无效')
    options.push(...page.list)
    if (options.length >= Number(page.total)) break
    if (!page.list.length) throw new Error('设备台账分页不完整')
  }
  machinery.value = options
}
async function importWord(upload: UploadFile) {
  if (!upload.raw) return
  if (!projectId.value) {
    message.error('请选择规程所属项目')
    return
  }
  if (draft.value && !published.value)
    await message.confirm('重新解析将替换当前未发布内容，是否继续？')
  await run(async () => {
    const parsed = await SimpleCommonParserApi.parse(upload.raw!, projectId.value!)
    await loadResources()
    draft.value = parsed
    published.value = false
    viewMode.value = 'edit'
    processIndex.value = 0
    itemIndex.value = 0
  })
}
function selectProcess(index: number) {
  processIndex.value = index
  itemIndex.value = 0
}
function normalize() {
  draft.value?.processes.forEach((p, pi) => {
    p.sort = pi + 1
    p.items.forEach((i, ii) => {
      i.itemSort = ii + 1
    })
  })
}
function addProcess() {
  draft.value!.processes.push({
    processCode: `P-${crypto.randomUUID()}`,
    processName: '新工序',
    sort: draft.value!.processes.length + 1,
    items: []
  })
  processIndex.value = draft.value!.processes.length - 1
  addItem()
}
function addItem() {
  process.value!.items.push({
    itemCode: `I-${crypto.randomUUID()}`,
    itemName: '',
    itemSort: process.value!.items.length + 1,
    standardText: '',
    inspectionMethod: '',
    inspectionTool: '',
    samplingPlanText: '',
    resultType: 'BOOLEAN',
    applicableInspectionTypes: [],
    critical: false,
    equipmentOptions: []
  })
  itemIndex.value = process.value!.items.length - 1
}
async function removeProcess() {
  await message.confirm('删除此工序及其全部检验项目？')
  draft.value!.processes.splice(processIndex.value, 1)
  processIndex.value = Math.max(0, processIndex.value - 1)
  itemIndex.value = 0
  normalize()
}
async function removeItem() {
  await message.confirm('删除此检验项目？')
  process.value!.items.splice(itemIndex.value, 1)
  itemIndex.value = Math.max(0, itemIndex.value - 1)
  normalize()
}
function moveProcess(offset: number) {
  const list = draft.value!.processes
  const i = processIndex.value
  ;[list[i], list[i + offset]] = [list[i + offset], list[i]]
  processIndex.value += offset
  normalize()
}
function moveItem(offset: number) {
  const list = process.value!.items
  const i = itemIndex.value
  ;[list[i], list[i + offset]] = [list[i + offset], list[i]]
  itemIndex.value += offset
  normalize()
}
function transferItem(pi: number) {
  const moved = process.value!.items.splice(itemIndex.value, 1)[0]
  draft.value!.processes[pi].items.push(moved)
  processIndex.value = pi
  itemIndex.value = draft.value!.processes[pi].items.length - 1
  normalize()
}
function selectEquipment(ids: number[]) {
  item.value!.equipmentOptions = ids.map((id, index) => {
    const m = machinery.value.find((m) => m.id === id)
    if (!m?.code) throw new Error('设备编号不存在，请刷新设备目录')
    return {
      equipmentId: m.id,
      equipmentCode: m.code,
      equipmentNumber: m.code,
      equipmentName: m.name,
      defaultFlag: index === 0,
      sort: index + 1
    }
  })
}
async function applyEquipment() {
  const matches = draft
    .value!.processes.flatMap((p) => p.items)
    .filter((i) => i.inspectionTool === item.value!.inspectionTool)
  await message.confirm(`将当前设备选择应用到器具要求完全相同的 ${matches.length} 个项目？`)
  matches.forEach((i) => {
    i.equipmentOptions = copy(item.value!.equipmentOptions)
  })
}
function syncFinal() {
  const rule = draft.value!.inspectionTypeRules.find((r) => r.key === 'FINAL')!
  rule.required = draft.value!.finalInspectionApplicable
  if (rule.required) draft.value!.finalInspectionNotApplicableReason = ''
  if (!rule.required)
    draft.value!.processes.forEach((p) =>
      p.items.forEach((i) => {
        i.applicableInspectionTypes = i.applicableInspectionTypes.filter((t) => t !== 'FINAL')
      })
    )
}
const storageKey = () => `simple-common:${getTenantId()}:${useUserStore().getUser.id}`
function saveLocal() {
  localStorage.setItem(
    storageKey(),
    JSON.stringify({
      draft: draft.value,
      projectId: projectId.value,
      projects: projects.value,
      published: published.value
    })
  )
  message.success('草稿已保存')
}
async function restoreLocal() {
  await run(async () => {
    const raw = localStorage.getItem(storageKey())
    if (!raw) throw new Error('没有已保存的草稿')
    const saved = JSON.parse(raw)
    if (!saved.draft?.processes || !saved.projectId || saved.draft.ownerModule !== 'MES_QA_COMMON')
      throw new Error('草稿内容无效')
    projectId.value = saved.projectId
    projects.value = saved.projects
    await loadResources()
    draft.value = saved.draft
    viewMode.value = 'edit'
    published.value = saved.published
    processIndex.value = 0
    itemIndex.value = 0
  })
}
async function preview() {
  normalize()
  previewVisible.value = true
}
function confirmNoFinal() {
  draft.value!.finalInspectionNotApplicableReason = '经人工确认，本规程不适用末检。'
}
function editIssue(issue: DraftIssue) {
  pendingIssue.value = issue
  previewVisible.value = false
  viewMode.value = 'edit'
  if (issue.processIndex != null) {
    selectProcess(issue.processIndex)
    itemIndex.value = issue.itemIndex ?? 0
  }
}
async function focusIssue() {
  const issue = pendingIssue.value
  if (!issue) return
  pendingIssue.value = undefined
  await nextTick()
  const target =
    issue.field === 'equipment'
      ? equipmentEditor.value?.$el
      : issue.field === 'metadata'
        ? metadataEditor.value
        : itemEditor.value
  target?.scrollIntoView({ block: 'center' })
  target?.querySelector<HTMLElement>('input, textarea, button')?.focus({ preventScroll: true })
}
async function publish() {
  if (issues.value.length || !draft.value || published.value) return
  await run(async () => {
    const result = await SimpleCommonParserApi.publish(copy(draft.value!))
    published.value = true
    previewVisible.value = false
    saveLocal()
    message.success(`已发布通用规程 ${result.versionNo}`)
  })
}
void searchProjects()
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.toolbar h2 {
  margin: 0 auto 0 0;
}
.metadata {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.workspace {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 24px;
  margin-top: 20px;
}
aside {
  border-right: 1px solid var(--el-border-color);
  padding-right: 16px;
}
.process {
  margin: 12px 0;
}
.process button {
  display: block;
  width: 100%;
  text-align: left;
  padding: 10px;
  border: 0;
  background: transparent;
  cursor: pointer;
  color: var(--el-text-color-primary);
}
.process button.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
article {
  border-bottom: 1px solid var(--el-border-color);
  padding: 8px 0;
}
.confirm-final {
  margin-top: 8px;
}
.publish-issues {
  padding: 16px;
  border: 1px solid var(--el-color-warning-light-5);
  border-radius: 4px;
  background: var(--el-color-warning-light-9);
}
.publish-issues h3 {
  margin: 0 0 8px;
}
.publish-issue {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
}
@media (max-width: 900px) {
  .workspace,
  .metadata {
    grid-template-columns: 1fr;
  }
}
</style>
