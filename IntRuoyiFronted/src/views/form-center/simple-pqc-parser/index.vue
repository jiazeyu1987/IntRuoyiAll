<template>
  <ContentWrap v-loading="busy">
    <div class="toolbar">
      <h2>简易PQC解析</h2>
      <el-select
        v-model="projectId"
        aria-label="所属产品"
        placeholder="选择所属产品"
        filterable
        remote
        :remote-method="searchProjects"
        :disabled="!!draft"
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
        <el-button :disabled="!projectId || busy">导入检验规程</el-button>
      </el-upload>
      <el-button :disabled="!draft || published" @click="saveLocal">保存草稿</el-button>
      <el-button @click="restoreLocal">恢复草稿</el-button>
      <el-button
        v-hasPermi="['mes:qc-template:update']"
        type="primary"
        :disabled="!draft || published"
        @click="preview"
        >预览发布</el-button
      >
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-alert
      v-if="published"
      title="已发布到 QA 检验规程。当前内容已锁定，重新导入可开始下一次编辑。"
      type="success"
      :closable="false"
    />
    <el-empty v-if="!draft" description="选择产品，导入 Word，然后直接修改识别结果" />
    <template v-else>
      <el-form :disabled="published" label-position="top">
        <div class="metadata">
          <el-form-item label="规程名称"><el-input v-model="draft.regulationName" /></el-form-item>
          <el-form-item label="文件编号"><el-input v-model="draft.regulationCode" /></el-form-item>
          <el-form-item label="生效日期"
            ><el-date-picker v-model="draft.effectiveDate" value-format="YYYY-MM-DD"
          /></el-form-item>
        </div>
        <p
          >{{ sourceName }} · 发布版本 {{ draft.versionNo }} · {{ draft.processes.length }} 道工序 /
          {{ itemCount }} 个项目</p
        >
        <el-alert
          title="Word 中的 AQL 与系统巡检抽样比例含义不同。请核对抽样要求和比例，再勾选确认。"
          type="warning"
          :closable="false"
        />
        <el-checkbox v-model="samplingConfirmed"
          >已核对首检数量、巡检抽样比例与 Word 抽样要求</el-checkbox
        >
        <el-collapse>
          <el-collapse-item title="检验规则（首检、巡检、末检及异常处理）" name="rules">
            <p class="rule-source">{{ ruleSourceText }}</p>
            <el-button v-if="baseline" @click="reuseRules">使用当前 QA 的检验规则</el-button>
            <el-switch
              v-model="draft.finalInspectionApplicable"
              active-text="适用末检"
              @change="syncFinal"
            />
            <el-input
              v-if="!draft.finalInspectionApplicable"
              v-model="draft.finalInspectionNotApplicableReason"
              placeholder="确认末检不适用的依据"
            />
            <div v-for="rule in draft.inspectionTypeRules" :key="rule.key" class="rule">
              <strong>{{ rule.label }}</strong>
              <el-input v-model="rule.taskRule" placeholder="检验触发和抽样要求" />
              <el-input v-model="rule.releaseGate" placeholder="放行及异常处理要求" />
              <el-input-number
                v-if="rule.key === 'FINAL' && draft.finalInspectionApplicable"
                v-model="rule.fixedQuantity"
                :min="1"
                aria-label="末检数量"
              />
            </div>
          </el-collapse-item>
        </el-collapse>
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
          <section v-if="process">
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
              <el-form-item label="检验项目"><el-input v-model="item.itemName" /></el-form-item>
              <el-form-item label="接受标准"
                ><el-input
                  v-model="item.standardText"
                  type="textarea"
                  :autosize="{ minRows: 3, maxRows: 10 }"
              /></el-form-item>
              <el-form-item label="检验方法"
                ><el-input
                  v-model="item.inspectionMethod"
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
                ><el-input
                  v-model="item.samplingPlanText"
                  type="textarea"
                  @input="samplingConfirmed = false"
              /></el-form-item>
              <div class="metadata">
                <el-form-item
                  v-if="item.applicableInspectionTypes.includes('FIRST')"
                  label="首检数量（件）"
                  ><el-input-number v-model="item.firstInspectionQuantity" :min="1" :precision="0"
                /></el-form-item>
                <el-form-item
                  v-if="item.applicableInspectionTypes.includes('PATROL')"
                  label="系统巡检抽样比例（%，需确认）"
                  ><el-input-number
                    v-model="item.patrolInspectionRatio"
                    :min="0.0001"
                    :max="100"
                    @change="samplingConfirmed = false"
                /></el-form-item>
              </div>
              <el-form-item label="检验器具及设备要求"
                ><el-input v-model="item.inspectionTool"
              /></el-form-item>
              <el-form-item label="选择设备编号（名称自动带出）">
                <el-select
                  :model-value="item.equipmentOptions.map((e) => e.equipmentId)"
                  multiple
                  filterable
                  placeholder="选择设备编号"
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
              <el-button v-if="matchingItem" @click="reuseItem"
                >复用当前 QA 此项目的设备及补充配置</el-button
              >
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
      title="确认发布到 QA 检验规程"
      width="900px"
      :close-on-click-modal="false"
    >
      <template v-if="draft">
        <p>{{ selectedProject }} · {{ draft.regulationName }} · {{ draft.versionNo }}</p>
        <p>{{
          baseline ? '更新已有规程并发布新版本，原版本保留为历史版本。' : '新建检验规程并发布。'
        }}</p>
        <el-alert
          v-for="issue in issues"
          :key="issue"
          :title="issue"
          type="error"
          :closable="false"
        />
        <h3>与当前 QA 规程的差异（{{ changes.length }}）</h3>
        <p v-if="!changes.length">检验业务字段一致，发布版本单独管理。</p>
        <ul
          ><li v-for="change in changes" :key="change">{{ translateDifference(change) }}</li></ul
        >
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
import { computed, ref } from 'vue'
import type { UploadFile } from 'element-plus'
import {
  QcTemplateApi,
  type QaInspectionRegulationParseVO,
  type QaInspectionRegulationPublishedVersionVO
} from '@/api/mes/qc/template'
import {
  getProjectCodePage,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import { DvMachineryApi, type DvMachineryVO } from '@/api/mes/dv/machinery'
import { getTenantId } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'
import { makeDraft, nextVersion, validateDraft, differences, type Draft } from '../parser/simplePqc'

defineOptions({ name: 'FormCenterSimplePqcParser' })
const message = useMessage()
const projectId = ref<number>()
const projects = ref<DccProjectCodeRespVO[]>([])
const draft = ref<Draft>()
const baseline = ref<QaInspectionRegulationPublishedVersionVO | null>(null)
const machinery = ref<DvMachineryVO[]>([])
const sourceName = ref('')
const ruleSourceText = ref('')
const busy = ref(false)
const error = ref('')
const published = ref(false)
const samplingConfirmed = ref(false)
const confirmedUnbound = ref<string[]>([])
const previewVisible = ref(false)
const processIndex = ref(0)
const itemIndex = ref(0)
const process = computed(() => draft.value?.processes[processIndex.value])
const item = computed(() => process.value?.items[itemIndex.value])
const itemCount = computed(() => draft.value?.processes.reduce((n, p) => n + p.items.length, 0))
const selectedProject = computed(
  () => projects.value.find((p) => p.id === projectId.value)?.projectName
)
const changes = computed(() => (draft.value ? differences(draft.value, baseline.value) : []))
const issues = computed(() =>
  draft.value
    ? [
        ...validateDraft(draft.value, confirmedUnbound.value),
        ...(!samplingConfirmed.value ? ['请核对并确认抽样规则'] : [])
      ]
    : []
)
const matchingItem = computed(() => {
  const ps =
    baseline.value?.processes.filter((p) => p.processName === process.value?.processName) || []
  const items =
    ps.length === 1 ? ps[0].items.filter((i) => i.itemName === item.value?.itemName) : []
  return items.length === 1 ? items[0] : undefined
})
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
    projects.value = (
      await getProjectCodePage({ pageNo: 1, pageSize: 100, keyword, status: 'ENABLE' })
    ).list
  } catch (e) {
    error.value = `产品列表加载失败：${String(e)}`
  }
}
async function loadResources() {
  if (!projectId.value) throw new Error('请选择所属产品')
  const current = await QcTemplateApi.getCurrentQaRegulation(projectId.value)
  const versions = await QcTemplateApi.listQaRegulationVersions(projectId.value)
  const options: DvMachineryVO[] = []
  for (let pageNo = 1; ; pageNo++) {
    const page = await DvMachineryApi.getMachineryPage({ pageNo, pageSize: 100 })
    if (!Array.isArray(page.list) || !Number.isFinite(Number(page.total)))
      throw new Error('设备台账返回无效')
    options.push(...page.list)
    if (options.length >= Number(page.total)) break
    if (!page.list.length) throw new Error('设备台账分页不完整')
  }
  if (current) {
    await Promise.all(
      current.processes
        .flatMap((p) => p.items)
        .map(async (item) => {
          const config = await QcTemplateApi.getPqcItemEquipmentConfigBatch(
            projectId.value!,
            current.processes
              .flatMap((p) => p.items)
              .filter((i) => i.itemName === item.itemName)
              .map((i) => i.itemCode)
          )
          item.equipmentOptions = config.equipmentGroups
            .filter((g) => g.enabled !== false)
            .flatMap((g) =>
              g.equipmentNumbers
                .filter((n) => n.enabled !== false)
                .map((n, index) => ({
                  equipmentId: g.equipmentId,
                  equipmentCode: g.equipmentCode || '',
                  equipmentName: g.equipmentName || '',
                  equipmentNumber: n.equipmentNumber,
                  defaultFlag: g.defaultFlag === true && index === 0,
                  sort: g.sort ?? index + 1
                }))
            )
        })
    )
  }
  baseline.value = current
  machinery.value = options
  return versions
}
async function importWord(upload: UploadFile) {
  if (!upload.raw) return
  if (draft.value && !published.value)
    await message.confirm('重新导入将替换当前未发布内容，是否继续？')
  await run(async () => {
    const parsed: QaInspectionRegulationParseVO =
      await QcTemplateApi.parseQaInspectionRegulationJson(upload.raw!)
    const versions = await loadResources()
    const next = makeDraft(parsed, projectId.value!)
    next.regulationId = baseline.value?.regulationId
    next.versionNo = nextVersion(
      parsed.versionNo,
      versions.map((v) => v.versionNo)
    )
    draft.value = next
    sourceName.value = upload.name
    ruleSourceText.value = parsed.ruleSourceText || ''
    published.value = false
    samplingConfirmed.value = false
    confirmedUnbound.value = []
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
function reuseRules() {
  draft.value!.inspectionTypeRules = copy(baseline.value!.inspectionTypeRules)
  draft.value!.finalInspectionApplicable = baseline.value!.finalInspectionApplicable
  draft.value!.finalInspectionNotApplicableReason =
    baseline.value!.finalInspectionNotApplicableReason
}
function reuseItem() {
  const old = matchingItem.value!
  const fields = [
    'standardLowerLimit',
    'standardUpperLimit',
    'standardUnit',
    'standardPrecision',
    'critical',
    'failureRule',
    'equipmentOptions'
  ] as const
  const supplement = Object.fromEntries(
    fields.map((f) => [f, copy(old[f] ?? (f === 'equipmentOptions' ? [] : null))])
  )
  Object.assign(item.value!, supplement)
  confirmedUnbound.value = confirmedUnbound.value.filter((code) => code !== item.value!.itemCode)
  if (!old.equipmentOptions.length) confirmedUnbound.value.push(item.value!.itemCode)
  message.success(
    old.equipmentOptions.length ? '已复用当前 QA 配置' : '已确认沿用当前 QA 的未绑定设备状态'
  )
}
function syncFinal() {
  const rule = draft.value!.inspectionTypeRules.find((r) => r.key === 'FINAL')!
  rule.required = draft.value!.finalInspectionApplicable
  if (!rule.required)
    draft.value!.processes.forEach((p) =>
      p.items.forEach((i) => {
        i.applicableInspectionTypes = i.applicableInspectionTypes.filter((t) => t !== 'FINAL')
      })
    )
}
const storageKey = () => `simple-pqc:${getTenantId()}:${useUserStore().getUser.id}`
function saveLocal() {
  localStorage.setItem(
    storageKey(),
    JSON.stringify({
      draft: draft.value,
      projectId: projectId.value,
      projects: projects.value,
      sourceName: sourceName.value,
      ruleSourceText: ruleSourceText.value,
      samplingConfirmed: samplingConfirmed.value,
      confirmedUnbound: confirmedUnbound.value,
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
    if (!saved.draft?.processes || !saved.projectId) throw new Error('草稿内容无效')
    projectId.value = saved.projectId
    projects.value = saved.projects
    await loadResources()
    draft.value = saved.draft
    sourceName.value = saved.sourceName
    ruleSourceText.value = saved.ruleSourceText || ''
    samplingConfirmed.value = saved.samplingConfirmed
    confirmedUnbound.value = saved.confirmedUnbound || []
    published.value = saved.published
    processIndex.value = 0
    itemIndex.value = 0
  })
}
async function preview() {
  normalize()
  await run(async () => {
    await loadResources()
    previewVisible.value = true
  })
}
async function publish() {
  if (issues.value.length || !draft.value || published.value) return
  await run(async () => {
    const result = await QcTemplateApi.publishSimplePqc(copy(draft.value!))
    published.value = true
    previewVisible.value = false
    saveLocal()
    message.success(`已发布 QA 规程 ${result.versionNo}`)
  })
}
function translateDifference(text: string) {
  const labels: Record<string, string> = {
    itemName: '项目名称',
    inspectionMethod: '检验方法',
    inspectionTool: '检验器具',
    samplingPlanText: '抽样要求',
    standardText: '接受标准',
    resultType: '结果填写方式',
    applicableInspectionTypes: '适用检验类型',
    firstInspectionQuantity: '首检数量',
    patrolInspectionRatio: '巡检抽样比例',
    standardLowerLimit: '下限',
    standardUpperLimit: '上限',
    standardUnit: '单位',
    standardPrecision: '小数位数',
    critical: '关键项目',
    failureRule: '不合格处理',
    equipmentOptions: '设备关联',
    effectiveDate: '生效日期',
    finalInspectionApplicable: '末检适用性',
    finalInspectionNotApplicableReason: '末检不适用依据',
    inspectionTypeRules: '检验规则'
  }
  return text.replace(/：([A-Za-z]+)$/, (_, key) => `：${labels[key] || key}`)
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
.rule {
  display: grid;
  grid-template-columns: 100px 1fr 1fr;
  gap: 12px;
  margin: 12px 0;
}
article {
  border-bottom: 1px solid var(--el-border-color);
  padding: 8px 0;
}
@media (max-width: 900px) {
  .workspace,
  .metadata {
    grid-template-columns: 1fr;
  }
}
</style>
