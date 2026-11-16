<template>
  <ContentWrap class="simple-parser-layout" v-loading="busy" :element-loading-text="busyText">
    <SimpleParserHeader
      v-model:project-id="projectId"
      :requires-project="false"
      show-project
      title="简易通用解析"
      import-accept=".doc,.docx"
      :projects="projects"
      :project-loading="projectLoading"
      :busy="busy"
      :has-content="!!draft"
      :published="published"
      publish-permission="mes:qc-template:update"
      @search="searchProjects"
      @import="importWord"
      @export="exportWord"
      @save="saveLocal"
      @restore="restoreLocal"
      @preview="preview"
    />
    <el-button
      v-hasPermi="['mes:qc-template:query']"
      class="mb-16px"
      @click="
        router.push({
          path: '/mes/pro/process-pool/common-regulation',
          query: publishedResult ? { regulationId: publishedResult.regulationId } : {}
        })
      "
    >
      查看已发布规程
    </el-button>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <section v-if="published" class="parser-published-result">
      <el-result
        icon="success"
        title="发布完成"
        :sub-title="`${draft?.regulationName} · ${draft?.versionNo}`"
      >
        <template #extra>
          <el-button type="primary" @click="viewResult">查看结果</el-button>
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept=".doc,.docx"
            :disabled="busy"
            :on-change="importWord"
          >
            <el-button :disabled="busy">重新导入</el-button>
          </el-upload>
        </template>
      </el-result>
    </section>
    <ParserRegulationResult
      v-if="publishedResult"
      v-model="resultVisible"
      :result="publishedResult"
      :show-project="false"
    />
    <el-empty v-if="!draft" description="导入通用规程 Word，编辑后预览发布" />
    <template v-else>
      <el-radio-group v-model="viewMode" class="parser-mode-tabs" aria-label="解析查看模式">
        <el-radio-button value="edit">编辑内容</el-radio-button>
        <el-radio-button value="trial">试填预览</el-radio-button>
      </el-radio-group>
      <p class="parser-draft-status">{{
        dirty ? '有未保存修改' : savedAt ? `已保存：${savedAt}` : '内容已保存'
      }}</p>
      <ParserTrialNavigation
        v-if="viewMode === 'trial'"
        :index="processIndex"
        :names="draft.processes.map((p) => p.processName)"
        @select="selectProcess"
        @edit="viewMode = 'edit'"
      />
      <SimplePqcTrialPreview
        v-show="viewMode === 'trial'"
        :key="trialSession"
        :draft="draft"
        :process-index="processIndex"
        @select-process="selectProcess"
      />
      <el-form v-show="viewMode === 'edit'" :disabled="published" label-position="top">
        <div ref="metadataEditor" class="metadata parser-metadata">
          <el-form-item label="规程名称"
            ><el-input
              v-model="draft.regulationName"
              data-issue-field="regulationName"
              aria-label="规程名称"
          /></el-form-item>
          <el-form-item label="文件编号"
            ><el-input
              v-model="draft.regulationCode"
              data-issue-field="regulationCode"
              aria-label="文件编号"
          /></el-form-item>
          <el-form-item label="发布版本"
            ><el-input v-model="draft.versionNo" data-issue-field="versionNo" aria-label="发布版本"
          /></el-form-item>
          <el-form-item label="生效日期"
            ><el-date-picker
              v-model="draft.effectiveDate"
              data-issue-field="effectiveDate"
              value-format="YYYY-MM-DD"
          /></el-form-item>
        </div>
        <div ref="workspaceEditor" class="workspace parser-workspace">
          <aside class="parser-sidebar">
            <el-button data-issue-field="addProcess" @click="addProcess">添加工序</el-button>
            <Draggable
              :model-value="draft.processes"
              @update:model-value="reorderProcesses"
              data-issue-field="processes"
              item-key="processCode"
              handle=".parser-drag-handle"
              :disabled="busy || published"
            >
              <template #item="{ element: p, index: pi }">
                <div class="process">
                  <span v-if="!published" class="parser-drag-handle" title="拖动排序">⠿</span>
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
              </template>
            </Draggable>
          </aside>
          <section v-if="process" ref="itemEditor" class="parser-editor">
            <ParserProcessToolbar
              :index="processIndex"
              :count="draft.processes.length"
              :can-undo="!!history.length"
              :disabled="busy || published"
              @undo="undo"
              @move="moveProcess"
              @remove="removeProcess"
            />
            <div class="toolbar">
              <el-input
                v-model="process.processName"
                data-issue-field="processName"
                aria-label="工序名称"
                style="width: 240px"
              />
              <el-button data-issue-field="addItem" @click="addItem">添加检验项目</el-button>
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
                ><el-input
                  v-model="item.itemName"
                  data-issue-field="itemName"
                  aria-label="检验项目"
              /></el-form-item>
              <el-form-item label="接受标准"
                ><el-input
                  v-model="item.standardText"
                  data-issue-field="standardText"
                  aria-label="接受标准"
                  type="textarea"
                  :autosize="{ minRows: 3, maxRows: 10 }"
              /></el-form-item>
              <el-form-item label="检验方法"
                ><el-input
                  v-model="item.inspectionMethod"
                  data-issue-field="inspectionMethod"
                  aria-label="检验方法"
                  type="textarea"
                  :autosize="{ minRows: 3, maxRows: 10 }"
              /></el-form-item>
              <div class="metadata">
                <el-form-item label="结果填写方式"
                  ><el-select v-model="item.resultType" data-issue-field="resultType"
                    ><el-option label="合格 / 不合格" value="BOOLEAN" /><el-option
                      label="测量数值"
                      value="NUMERIC" /><el-option label="文字记录" value="TEXT" /></el-select
                ></el-form-item>
                <el-form-item label="适用检验"
                  ><el-checkbox-group
                    v-model="item.applicableInspectionTypes"
                    data-issue-field="applicableInspectionTypes"
                    ><el-checkbox value="FIRST">首检</el-checkbox
                    ><el-checkbox value="PATROL">巡检</el-checkbox
                    ><el-checkbox v-if="draft.finalInspectionApplicable" value="FINAL"
                      >末检</el-checkbox
                    ></el-checkbox-group
                  ></el-form-item
                >
                <el-form-item label="关键检验项目"
                  ><el-switch v-model="item.critical" data-issue-field="critical"
                /></el-form-item>
              </div>
              <div v-if="item.resultType === 'NUMERIC'" class="metadata">
                <el-form-item label="下限"
                  ><el-input-number
                    v-model="item.standardLowerLimit"
                    data-issue-field="standardLowerLimit"
                /></el-form-item>
                <el-form-item label="上限"
                  ><el-input-number
                    v-model="item.standardUpperLimit"
                    data-issue-field="standardUpperLimit"
                /></el-form-item>
                <el-form-item label="单位"
                  ><el-input v-model="item.standardUnit" data-issue-field="standardUnit"
                /></el-form-item>
                <el-form-item label="小数位数"
                  ><el-input-number
                    v-model="item.standardPrecision"
                    data-issue-field="standardPrecision"
                    :min="0"
                    :max="10"
                /></el-form-item>
              </div>
              <el-form-item label="抽样要求"
                ><el-input
                  v-model="item.samplingPlanText"
                  data-issue-field="samplingPlanText"
                  type="textarea"
              /></el-form-item>
              <div class="metadata">
                <el-form-item
                  v-if="item.applicableInspectionTypes.includes('FIRST')"
                  label="首检数量（件）"
                  ><el-input-number
                    v-model="item.firstInspectionQuantity"
                    data-issue-field="firstInspectionQuantity"
                    :min="1"
                    :precision="0"
                /></el-form-item>
                <el-form-item
                  v-if="item.applicableInspectionTypes.includes('PATROL')"
                  label="巡检检验水平 / AQL"
                  ><SamplingRuleEditor
                    v-model:inspection-level="item.inspectionLevel"
                    v-model:aql="item.aql"
                /></el-form-item>
              </div>
              <p
                v-if="
                  item.applicableInspectionTypes.includes('PATROL') &&
                  /AQL/i.test(item.samplingPlanText)
                "
                >{{ MATRIX_SAMPLING_RULE }}</p
              >
              <el-form-item label="检验器具及设备要求"
                ><el-input v-model="item.inspectionTool" data-issue-field="inspectionTool"
              /></el-form-item>
              <el-form-item label="需要设备">
                <el-switch
                  :model-value="needsEquipment(item)"
                  aria-label="需要设备"
                  @change="toggleEquipment"
                />
                <span>关闭后保留检验要求，无需绑定设备或填写编号。</span>
              </el-form-item>
              <el-form-item
                v-if="needsEquipment(item)"
                ref="equipmentEditor"
                label="选择设备编号（名称自动带出）"
              >
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
              <el-button :disabled="!needsEquipment(item)" @click="applyEquipment"
                >应用到相同器具要求的项目</el-button
              >
              <el-form-item label="不合格处理要求"
                ><el-input
                  v-model="item.failureRule"
                  data-issue-field="failureRule"
                  type="textarea"
              /></el-form-item>
            </template>
          </section>
          <section v-else class="parser-editor"
            ><ParserProcessToolbar
              :index="-1"
              :count="0"
              :can-undo="!!history.length"
              :disabled="busy || published"
              @undo="undo"
              @move="moveProcess"
              @remove="removeProcess" /><el-empty description="暂无工序，请添加工序"
          /></section>
        </div>
      </el-form>
      <ParserIssueList
        v-if="!published"
        :issues="issues"
        @locate="(index) => editIssue(issues[index])"
      />
    </template>
    <el-dialog
      v-model="previewVisible"
      title="预览发布"
      width="min(1000px, 95vw)"
      :close-on-click-modal="false"
      :close-on-press-escape="!busy"
      :show-close="!busy"
      @closed="focusIssue"
      @opened="focusFinalIssue"
    >
      <template v-if="draft">
        <h3>发布内容</h3>
        <p>{{ draft.regulationName }} · {{ draft.versionNo }}</p>
        <p>{{
          draft.regulationId
            ? '发布通用规程新版本，历史版本和产品QA已有绑定保留。可在产品QA规程中选择关联新版。'
            : '新建并发布通用检验规程，发布后可在通用规程页面维护，并在产品QA规程中关联。'
        }}</p>
        <el-form ref="finalEditor" :disabled="published || busy" label-position="top">
          <el-form-item label="末检适用性">
            <el-switch
              v-model="draft.finalInspectionApplicable"
              data-issue-field="finalInspectionApplicable"
              active-text="适用末检"
              @change="syncFinal"
            />
          </el-form-item>
          <el-form-item v-if="!draft.finalInspectionApplicable" label="末检不适用依据">
            <el-input
              v-model="draft.finalInspectionNotApplicableReason"
              data-issue-field="finalInspectionNotApplicableReason"
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
                {{ formatInspectionLevel(i) }}</p
              ><p
                v-if="
                  i.applicableInspectionTypes.includes('PATROL') && /AQL/i.test(i.samplingPlanText)
                "
                >{{ MATRIX_SAMPLING_RULE }}</p
              ><p
                >设备：{{
                  i.equipmentRequired === false
                    ? '无需设备'
                    : i.equipmentOptions
                        .map((e) => `${e.equipmentNumber} / ${e.equipmentName}`)
                        .join('、') || i.inspectionTool
                }}</p
              ><p v-if="i.equipmentUnboundReason"
                >器具未绑定原因：{{ i.equipmentUnboundReason }}</p
              ></article
            >
          </el-collapse-item></el-collapse
        >
        <ParserIssueList
          :issues="issues"
          :disabled="busy"
          @locate="(index) => editIssue(issues[index])"
        />
      </template>
      <template #footer
        ><el-button :disabled="busy" @click="previewVisible = false">返回修改</el-button
        ><el-button
          type="primary"
          :loading="busy"
          :disabled="busy || issues.length > 0 || published"
          @click="publish"
          >确认发布</el-button
        ></template
      >
    </el-dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import SamplingRuleEditor from '@/views/mes/qc/template/SamplingRuleEditor.vue'
import { computed, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import Draggable from 'vuedraggable'
import { useParserEditing } from '../parser/useParserEditing'
import ParserProcessToolbar from '../parser/components/ParserProcessToolbar.vue'
import ParserTrialNavigation from '../parser/components/ParserTrialNavigation.vue'
import ParserIssueList from '../parser/components/ParserIssueList.vue'
import SimpleParserHeader from '../parser/components/SimpleParserHeader.vue'
import SimplePqcTrialPreview from '../parser/components/SimplePqcTrialPreview.vue'
import ParserRegulationResult from '../parser/components/ParserRegulationResult.vue'
import type { UploadFile } from 'element-plus'
import { SimpleCommonParserApi } from '@/api/mes/qc/template/simpleCommonParser'
import { QcTemplateApi, type QaInspectionRegulationPublishedVersionVO } from '@/api/mes/qc/template'
import {
  needsEquipment,
  MATRIX_SAMPLING_RULE,
  nextVersion,
  formatInspectionLevel
} from '../parser/simplePqc'
import { DvMachineryApi, type DvMachineryVO } from '@/api/mes/dv/machinery'
import { getTenantId } from '@/utils/auth'
import {
  getSimpleParserProjects,
  type SimpleParserProject
} from '@/api/mes/pro/batchrecordreport/simpleParserProjects'
import { useUserStore } from '@/store/modules/user'
import { validateCommonDraft, type DraftIssue } from './draft'
import type { SimpleCommonDraft as Draft } from '@/api/mes/qc/template/simpleCommonParser'
import { createPqcWord, readPqcWord, pqcWordFilename, pqcWordMime } from '../parser/simplePqcWord'

defineOptions({ name: 'FormCenterSimpleCommonParser' })
const message = useMessage()
const router = useRouter()
const busyText = ref('正在处理，请稍候…')
const trialSession = ref(0)
const finalEditor = ref<{ $el: HTMLElement }>()
const draft = ref<Draft>()
const machinery = ref<DvMachineryVO[]>([])
const busy = ref(false)
const error = ref('')
const projectId = ref<number>()
const projects = ref<SimpleParserProject[]>([])
const projectLoading = ref(false)
let projectQuery = 0
async function searchProjects(keyword = '') {
  const query = ++projectQuery
  projectLoading.value = true
  try {
    const candidates = await getSimpleParserProjects(keyword)
    if (query !== projectQuery) return
    const selected = projects.value.find((project) => project.id === projectId.value)
    projects.value =
      selected && !candidates.some((project) => project.id === selected.id)
        ? [selected, ...candidates]
        : candidates
  } catch (cause) {
    if (query === projectQuery) error.value = `项目列表加载失败：${String(cause)}`
  } finally {
    if (query === projectQuery) projectLoading.value = false
  }
}
void searchProjects()
const published = ref(false)
const publishedResult = ref<QaInspectionRegulationPublishedVersionVO>()
const resultVisible = ref(false)
const previewVisible = ref(false)
const viewMode = ref<'edit' | 'trial'>('edit')
const processIndex = ref(0)
const itemIndex = ref(0)
const metadataEditor = ref<HTMLElement>()
const workspaceEditor = ref<HTMLElement>()
const finalIssuePending = ref(false)
const itemEditor = ref<HTMLElement>()
const equipmentEditor = ref<{ $el: HTMLElement }>()
const pendingIssue = ref<DraftIssue>()
const process = computed(() => draft.value?.processes[processIndex.value])
const item = computed(() => process.value?.items[itemIndex.value])
const issues = computed(() =>
  draft.value
    ? [
        ...validateCommonDraft(draft.value),
        ...(!draft.value.versionNo.trim()
          ? [{ message: '请填写发布版本', field: 'metadata' as const, target: 'versionNo' }]
          : [])
      ]
    : []
)
const copy = <T,>(value: T): T => JSON.parse(JSON.stringify(value))
const {
  history,
  dirty,
  savedAt,
  reset: resetEditing,
  markSaved,
  undo
} = useParserEditing(
  () => (draft.value ? { draft: draft.value } : undefined),
  (state) => {
    const code = process.value?.processCode,
      itemCode = item.value?.itemCode
    draft.value = state.draft
    processIndex.value = Math.max(
      0,
      state.draft.processes.findIndex((p) => p.processCode === code)
    )
    itemIndex.value = Math.max(
      0,
      process.value?.items.findIndex((i) => i.itemCode === itemCode) ?? 0
    )
  },
  published
)
function reorderProcesses(list: Draft['processes']) {
  if (busy.value || published.value) return
  const code = process.value?.processCode
  draft.value!.processes = list
  normalize()
  processIndex.value = Math.max(
    0,
    list.findIndex((p) => p.processCode === code)
  )
}
function viewResult() {
  resultVisible.value = true
}

async function run(action: () => Promise<void>, text = '正在处理，请稍候…') {
  if (busy.value) return
  busyText.value = text
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
async function loadResources() {
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
async function confirmReplace(text: string) {
  try {
    await message.confirm(text)
    return true
  } catch (reason) {
    if (reason === 'cancel' || reason === 'close') return false
    throw reason
  }
}
async function importWord(upload: UploadFile) {
  if (busy.value) return
  if (!upload.raw) return
  if (!/\.docx?$/i.test(upload.raw.name)) {
    message.error('请选择 DOC 或 DOCX 文件')
    return
  }
  if (dirty.value && !(await confirmReplace('导入新文档将替换当前未保存内容，是否继续？'))) return
  await run(async () => {
    const restored = /\.docx$/i.test(upload.raw!.name)
      ? readPqcWord(
          await upload.raw!.arrayBuffer(),
          String(getTenantId()),
          undefined,
          'MES_QA_COMMON'
        )
      : null
    const parsed = restored ? restored.draft : await SimpleCommonParserApi.parse(upload.raw!)
    if (restored) {
      const versions = await QcTemplateApi.listCommonRegulationReleasedVersions()
      parsed.versionNo = nextVersion(
        parsed.versionNo,
        versions
          .filter((v) => v.commonRegulationCode === parsed.regulationCode.trim())
          .map((v) => v.versionNo)
      )
      parsed.expectedDraftRevision = 0
    }
    await loadResources()
    draft.value = parsed
    published.value = false
    publishedResult.value = undefined
    resultVisible.value = false
    viewMode.value = 'edit'
    processIndex.value = 0
    itemIndex.value = 0
    trialSession.value++
    resetEditing()
  }, '正在解析，请稍候…')
}
async function exportWord() {
  if (!draft.value) return
  await run(async () => {
    const bytes = createPqcWord(copy(draft.value!), String(getTenantId()), [])
    const url = URL.createObjectURL(new Blob([new Uint8Array(bytes)], { type: pqcWordMime }))
    const link = document.createElement('a')
    link.href = url
    link.download = pqcWordFilename(draft.value!)
    document.body.appendChild(link)
    link.click()
    link.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
    message.success('Word 已导出，可通过“导入 Word”还原完整内容')
  }, '正在导出，请稍候…')
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
  if (!(await confirmReplace('删除此工序及其全部检验项目？删除后可撤销。'))) return
  draft.value!.processes.splice(processIndex.value, 1)
  processIndex.value = Math.max(0, processIndex.value - 1)
  itemIndex.value = 0
  normalize()
}
async function removeItem() {
  if (!(await confirmReplace('删除此检验项目？删除后可撤销。'))) return
  process.value!.items.splice(itemIndex.value, 1)
  itemIndex.value = Math.max(0, itemIndex.value - 1)
  normalize()
}
function moveProcess(offset: number) {
  const list = draft.value!.processes
  const i = processIndex.value
  if (busy.value || published.value || i + offset < 0 || i + offset >= list.length) return
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
function toggleEquipment(required: boolean) {
  item.value!.equipmentRequired = required
  if (!required) {
    item.value!.equipmentOptions = []
    item.value!.equipmentUnboundReason = undefined
  }
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
  if (ids.length) {
    item.value!.equipmentRequired = true
    item.value!.equipmentUnboundReason = undefined
  }
}
async function applyEquipment() {
  const matches = draft
    .value!.processes.flatMap((p) => p.items)
    .filter((i) => i.equipmentRequired !== false && i.inspectionTool === item.value!.inspectionTool)
  await message.confirm(`将当前设备选择应用到器具要求完全相同的 ${matches.length} 个项目？`)
  matches.forEach((i) => {
    i.equipmentRequired = item.value!.equipmentRequired
    i.equipmentOptions = copy(item.value!.equipmentOptions)
    if (i.equipmentOptions.length) i.equipmentUnboundReason = undefined
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
  try {
    const time = new Date().toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false })
    localStorage.setItem(
      storageKey(),
      JSON.stringify({
        projectId: projectId.value,
        draft: draft.value,
        published: published.value,
        publishedResult: publishedResult.value,
        savedAt: time
      })
    )
    markSaved(time)
    message.success('草稿已保存在当前浏览器')
  } catch (cause) {
    error.value = `${published.value ? '发布已成功，但本地结果保存失败' : '草稿保存失败'}：${String(cause)}`
    message.error(error.value)
  }
}
async function restoreLocal() {
  if (busy.value) return
  if (dirty.value && !(await confirmReplace('恢复草稿将替换当前未保存内容，是否继续？'))) return
  await run(async () => {
    const raw = localStorage.getItem(storageKey())
    if (!raw) throw new Error('没有已保存的草稿')
    const saved = JSON.parse(raw)
    if (!saved.draft?.processes || saved.draft.ownerModule !== 'MES_QA_COMMON')
      throw new Error('草稿内容无效')
    if (saved.published && !saved.publishedResult?.publishedVersionId)
      throw new Error('已发布草稿缺少正式结果，请重新导入文档后发布')
    await loadResources()
    projects.value = await getSimpleParserProjects()
    if (saved.projectId && !projects.value.some((project) => project.id === saved.projectId))
      throw new Error('草稿参考项目不存在或已停用，请重新选择项目')
    projectId.value = saved.projectId
    draft.value = saved.draft
    delete draft.value!.dccProjectCodeId
    viewMode.value = 'edit'
    published.value = saved.published
    publishedResult.value = saved.publishedResult
    resultVisible.value = false
    processIndex.value = 0
    itemIndex.value = 0
    trialSession.value++
    resetEditing(true, saved.savedAt || '')
  }, '正在恢复草稿，请稍候…')
}
async function preview() {
  normalize()
  previewVisible.value = true
}
function confirmNoFinal() {
  draft.value!.finalInspectionNotApplicableReason = '经人工确认，本规程不适用末检。'
}
async function editIssue(issue: DraftIssue) {
  if (busy.value || published.value) return
  if (issue.field === 'final') {
    finalIssuePending.value = true
    if (previewVisible.value) focusFinalIssue()
    else previewVisible.value = true
    return
  }
  pendingIssue.value = issue
  const closing = previewVisible.value
  previewVisible.value = false
  viewMode.value = 'edit'
  if (issue.processIndex != null) {
    selectProcess(issue.processIndex)
    itemIndex.value = issue.itemIndex ?? 0
  }
  if (!closing) await focusIssue()
}
function focusFinalIssue() {
  if (!finalIssuePending.value) return
  finalIssuePending.value = false
  const target = finalEditor.value?.$el.querySelector<HTMLElement>(
    '[data-issue-field="finalInspectionNotApplicableReason"]'
  )
  target?.scrollIntoView({ block: 'center' })
  ;(target?.matches('input, textarea')
    ? target
    : target?.querySelector<HTMLElement>('input, textarea')
  )?.focus({ preventScroll: true })
}
async function focusIssue() {
  const issue = pendingIssue.value
  if (!issue) return
  pendingIssue.value = undefined
  await nextTick()
  const container =
    issue.field === 'equipment'
      ? equipmentEditor.value?.$el
      : issue.field === 'metadata'
        ? metadataEditor.value
        : issue.field === 'process'
          ? workspaceEditor.value
          : itemEditor.value
  const target = issue.target
    ? container?.querySelector<HTMLElement>(`[data-issue-field="${issue.target}"]`)
    : container
  target?.scrollIntoView({ block: 'center' })
  ;(target?.matches('input, textarea, button')
    ? target
    : target?.querySelector<HTMLElement>('input, textarea, button')
  )?.focus({ preventScroll: true })
}
async function publish() {
  if (issues.value.length || !draft.value || published.value) return
  await run(async () => {
    const result = await SimpleCommonParserApi.publish(copy(draft.value!))
    publishedResult.value = result
    published.value = true
    previewVisible.value = false
    saveLocal()
    message.success(`已发布通用规程 ${result.versionNo}`)
  }, '正在发布，请稍候…')
}
</script>

<style scoped>
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
@media (max-width: 900px) {
  .workspace,
  .metadata {
    grid-template-columns: 1fr;
  }
}
</style>

<style scoped src="../parser/simpleParserLayout.css"></style>
