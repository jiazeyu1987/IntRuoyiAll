<template>
  <ContentWrap
    class="simple-parser simple-parser-layout"
    v-loading="busy"
    :element-loading-text="busyText"
  >
    <SimpleParserHeader
      v-model:project-id="projectId"
      title="简易解析"
      import-accept=".doc,.docx"
      :projects="projects"
      :project-loading="projectLoading"
      :busy="busy"
      :has-content="!!recognition"
      :published="!!published"
      import-permission="form:parser:production-batch-record"
      publish-permission="form:parser:production-batch-record:publish"
      @search="searchProjects"
      @import="importWord"
      @export="exportWord"
      @save="saveDraft"
      @restore="restoreDraft"
      @preview="openPublish"
    />
    <el-alert v-if="error" class="status-message" :title="error" type="error" :closable="false" />
    <el-empty v-if="!recognition" description="先选择所属项目，再导入 Word，编辑后预览发布" />
    <template v-else>
      <el-radio-group v-model="mode" class="parser-mode-tabs" aria-label="解析查看模式">
        <el-radio-button value="edit">编辑内容</el-radio-button>
        <el-radio-button value="trial">试填预览</el-radio-button>
      </el-radio-group>
      <p class="parser-draft-status">{{
        dirty ? '有未保存修改' : savedAt ? `已保存：${savedAt}` : '内容已保存'
      }}</p>
      <ParserTrialNavigation
        v-if="mode === 'trial'"
        :index="currentIndex"
        :names="processes.map((p) => p.value.name)"
        @select="(index) => selectProcess(processes[index].id)"
        @edit="mode = 'edit'"
      />
      <div v-show="mode === 'trial'">
        <KeepAlive :key="trialSession"
          ><SimpleTrialPreview
            v-if="current"
            :key="current.id"
            :process="current.value"
            :process-index="currentIndex"
            :processes="processes.map((p) => p.value)"
            @select-process="(index) => selectProcess(processes[index].id)"
        /></KeepAlive>
      </div>
      <div v-show="mode === 'edit'" ref="metadataEditor" class="parser-metadata">
        <el-form
          class="route-name"
          label-position="top"
          :disabled="busy || !!published"
          @submit.prevent
        >
          <el-form-item label="工艺流程名称"
            ><el-input v-model="routeName" aria-label="工艺流程名称" maxlength="128"
          /></el-form-item>
        </el-form>
        <p v-if="filename" class="source-filename">{{ filename }}</p>
      </div>
      <el-alert v-if="deviceError" :title="deviceError" type="error" :closable="false">
        <el-button link type="primary" :loading="deviceLoading" @click="loadDevices"
          >重新加载设备列表</el-button
        >
      </el-alert>
      <section v-if="published" class="published-result">
        <el-result
          icon="success"
          title="发布完成"
          :sub-title="`${published.routeCode} · ${published.routeCandidateVersionNo || published.routeVersionNo}`"
        >
          <template #extra
            ><el-button type="primary" @click="openRoute">查看结果</el-button
            ><el-button @click="showPublished">查看创建清单</el-button
            ><el-upload
              v-hasPermi="['form:parser:production-batch-record']"
              :auto-upload="false"
              :show-file-list="false"
              accept=".doc,.docx"
              :disabled="busy || !projectId"
              :on-change="importWord"
              ><el-button :disabled="busy || !projectId">重新导入</el-button></el-upload
            ></template
          >
        </el-result>
        <p>流程已保存为禁用路线及草稿候选，可在工艺流程中继续完成启用所需配置。</p>
      </section>
      <div v-show="mode === 'edit'" ref="workspaceEditor" class="workbench parser-workspace">
        <aside class="process-list parser-sidebar">
          <div class="section-title"
            ><el-button
              v-if="mode === 'edit'"
              :disabled="busy || !!published"
              data-issue-field="addProcess"
              @click="insertProcess(processes.length)"
              >添加工序</el-button
            ></div
          >
          <Draggable
            :model-value="processes"
            @update:model-value="reorderProcesses"
            item-key="id"
            handle=".drag-handle"
            :disabled="mode !== 'edit' || busy || !!published"
          >
            <template #item="{ element, index }"
              ><div class="process-item" :class="{ active: selectedId === element.id }"
                ><span v-if="mode === 'edit' && !published" class="drag-handle" title="拖动排序"
                  >⠿</span
                ><button type="button" @click="selectProcess(element.id)"
                  ><span>{{ index + 1 }}. {{ element.value.name || '未命名工序' }}</span
                  ><small>{{ processStatus(element, index) }}</small></button
                ></div
              ></template
            >
          </Draggable>
        </aside>
        <main v-if="current" ref="editor" class="process-canvas parser-editor">
          <div class="workbench-bar">
            <span class="review-summary"
              >{{ processes.length }} 道工序<span v-if="issues.length">
                · {{ issues.length }} 项待修正</span
              ></span
            >
          </div>
          <ParserProcessToolbar
            :index="currentIndex"
            :count="processes.length"
            :can-undo="!!history.length"
            :disabled="busy || !!published"
            @undo="undo"
            @move="moveProcess"
            @remove="removeProcess"
          />
          <el-form
            :key="current.id"
            label-position="top"
            :disabled="busy || !!published"
            @submit.prevent
          >
            <div class="section-title">
              <h3
                >工序 {{ currentIndex + 1 }}
                <el-tag :type="current.processId ? 'info' : 'success'">{{
                  current.processId ? `使用已有 ${current.processCode}` : '新建工序'
                }}</el-tag></h3
              >
            </div>
            <div class="process-form">
              <el-form-item label="工序名称"
                ><el-input
                  v-model="current.value.name"
                  data-issue-field="name"
                  aria-label="工序名称"
                  :readonly="!!current.processId"
                  maxlength="128"
              /></el-form-item>
              <el-form-item label="工序属性"
                ><el-checkbox v-model="current.value.criticalProcess"
                  >关键 / 特殊工序</el-checkbox
                ></el-form-item
              >
            </div>
            <div class="reuse-process" data-issue-field="reuse">
              <el-button
                v-if="!reuseVisible && !current.processId"
                link
                type="primary"
                @click="openProcessPicker"
                >选择已有工序（可选）</el-button
              >
              <template v-else>
                <el-select
                  :model-value="current.processId"
                  filterable
                  remote
                  :remote-method="loadProcesses"
                  :loading="processLoading"
                  placeholder="按工序名称查找"
                  aria-label="已有工序"
                  @change="chooseProcess"
                >
                  <el-option
                    v-if="
                      current.processId &&
                      !processOptions.some((item) => item.id === current.processId)
                    "
                    :value="current.processId"
                    :label="`${current.processCode || '已选工序'} / ${current.value.name}`"
                  />
                  <el-option
                    v-for="process in processOptions"
                    :key="process.id"
                    :value="process.id!"
                    :label="`${process.code} / ${process.name}${process.productName ? ' / ' + process.productName : ''}`"
                  />
                </el-select>
                <el-button link @click="useNewProcess">改为新建工序</el-button>
              </template>
            </div>
            <el-alert v-if="processError" :title="processError" type="error" :closable="false" />
            <section v-for="direction in directions" :key="direction.key" class="editor-section">
              <div class="section-title"
                ><h3>{{ direction.label }}</h3
                ><el-button text type="primary" @click="addMaterial(direction.key)"
                  >添加{{ direction.label }}</el-button
                ></div
              >
              <p v-if="!current.value[direction.key]?.length" class="empty-note"
                >未识别到{{ direction.label }}，有遗漏时可添加。</p
              >
              <div
                v-for="(material, index) in current.value[direction.key]"
                :key="index"
                class="material-row"
                :data-issue-field="`${direction.key}-${index}`"
              >
                <el-autocomplete
                  :model-value="material.name || ''"
                  :fetch-suggestions="searchMaterials"
                  :trigger-on-focus="false"
                  value-key="label"
                  :aria-label="`${direction.label}名称${index + 1}`"
                  placeholder="物料名称，可搜索目录纠正"
                  @update:model-value="(name) => editMaterialName(material, name)"
                  @select="(item) => chooseMaterial(material, item)"
                >
                  <template #default="{ item }"
                    ><span>{{ item.code }} / {{ item.name }}</span></template
                  >
                </el-autocomplete>
                <el-input
                  v-model="material.code"
                  :aria-label="`${direction.label}编码${index + 1}`"
                  placeholder="物料编码（无编码时按名称核验）"
                />
                <el-button text type="danger" @click="removeMaterial(direction.key, index)"
                  >删除</el-button
                >
              </div>
              <p v-if="materialError" class="error-text">{{ materialError }}</p>
            </section>
            <section class="editor-section">
              <div class="section-title"
                ><h3>设备与参数</h3
                ><el-button text type="primary" @click="addGroup">添加设备 / 参数组</el-button></div
              >
              <p v-if="!current.value.equipmentGroups?.length" class="empty-note"
                >本工序尚无设备与参数，需要时添加。</p
              >
              <section
                v-for="(group, g) in current.value.equipmentGroups"
                :key="g"
                class="equipment-group"
                :data-issue-field="`group-${g}`"
              >
                <div class="section-title"
                  ><strong>设备 / 参数组 {{ g + 1 }}</strong
                  ><div class="toolbar"
                    ><el-button text type="primary" @click="addEquipment(g)">添加设备</el-button
                    ><el-button
                      text
                      type="primary"
                      :disabled="!!current.craftSelections?.[g]"
                      @click="editParameter(g)"
                      >添加参数</el-button
                    ><el-button text type="danger" @click="removeGroup(g)">删除组</el-button></div
                  ></div
                >
                <el-select
                  v-model="group.selectionMode"
                  :data-issue-field="`selection-${g}`"
                  aria-label="设备选择方式"
                  class="selection-mode"
                  ><el-option label="单选设备" value="SINGLE" /><el-option
                    label="多选设备"
                    value="MULTIPLE"
                /></el-select>
                <div
                  v-for="(equipment, e) in group.equipmentOptions"
                  :key="e"
                  class="equipment-row"
                  :data-issue-field="`equipment-${g}-${e}`"
                >
                  <el-select
                    :model-value="equipment.code || undefined"
                    :aria-label="`设备编号${g + 1}-${e + 1}`"
                    filterable
                    :loading="deviceLoading"
                    :disabled="deviceLoading || !!deviceError"
                    placeholder="请选择设备编号"
                    @change="(code) => chooseEquipment(g, e, code)"
                  >
                    <el-option
                      v-if="equipment.code && !knownDevice(equipment.code)"
                      :value="equipment.code"
                      :label="`${equipment.code} / ${equipment.name || '名称待识别'}（${deviceLoading || deviceError ? '待目录确认' : published ? '已建档' : '新建设备'}）`"
                    />
                    <el-option
                      v-for="device in deviceOptions"
                      :key="device.deviceCode"
                      :value="device.deviceCode"
                      :label="`${device.deviceCode} / ${device.deviceName}${device.enabled ? '' : '（未启用）'}`"
                      :disabled="!device.enabled"
                    />
                  </el-select>
                  <el-input
                    :model-value="knownDevice(equipment.code)?.deviceName ?? equipment.name"
                    :aria-label="`设备名称${g + 1}-${e + 1}`"
                    placeholder="选择编号后自动带出名称"
                    readonly
                  />
                  <el-button text type="danger" @click="removeEquipment(g, e)">删除</el-button>
                </div>
                <p
                  v-if="group.equipmentOptions?.some((equipment) => !equipment.code?.trim())"
                  class="error-text"
                  >缺少设备编号，请从设备列表选择。</p
                >
                <p
                  v-if="
                    !deviceLoading &&
                    !deviceError &&
                    !published &&
                    group.equipmentOptions?.some(
                      (equipment) => equipment.code && !knownDevice(equipment.code)
                    )
                  "
                  class="empty-note"
                  >文档中的新设备编号将在发布预览中列出，确认后建档。</p
                >
                <div v-if="current.processId && group.parameters?.length" class="reuse-craft">
                  <el-select
                    :model-value="current.craftSelections?.[g]"
                    clearable
                    filterable
                    :loading="craftLoading"
                    aria-label="已有工艺"
                    placeholder="默认新建工艺；需要复用时选择"
                    @visible-change="(visible) => visible && loadCrafts()"
                    @change="(id) => chooseCraft(g, id)"
                  >
                    <el-option
                      v-if="
                        current.craftSelections?.[g] &&
                        !craftOptions.some((item) => item.id === current.craftSelections?.[g])
                      "
                      :value="current.craftSelections[g]"
                      label="已选择的工艺"
                    />
                    <el-option
                      v-for="craft in craftOptions"
                      :key="craft.id"
                      :value="craft.id"
                      :label="`${craft.code} / ${craft.name}`"
                    />
                  </el-select>
                  <el-button
                    v-if="current.craftSelections?.[g]"
                    link
                    @click="chooseCraft(g, undefined)"
                    >改为新建工艺</el-button
                  >
                </div>
                <el-alert v-if="craftError" :title="craftError" type="error" :closable="false" />
                <div
                  v-for="(parameter, p) in group.parameters"
                  :key="p"
                  role="button"
                  :tabindex="current.craftSelections?.[g] ? -1 : 0"
                  class="parameter-card"
                  @click="editParameter(g, p)"
                  @keydown.enter.self="editParameter(g, p)"
                >
                  <span
                    ><strong>{{
                      parameter.ui?.displayName || parameter.name || '未命名参数'
                    }}</strong
                    ><small
                      >{{ controlLabel(parameter.ui?.control) }} ·
                      {{ parameter.ui?.unit || '无单位' }}</small
                    ></span
                  >
                  <span
                    ><b>默认值：{{ parameter.ui?.defaultValue ?? '未配置' }}</b
                    ><small>参考：{{ parameter.referenceValue || '未配置' }}</small></span
                  >
                  <span class="parameter-edit">{{
                    current.craftSelections?.[g] ? '使用已有工艺' : '点击编辑'
                  }}</span>
                  <el-button
                    text
                    type="danger"
                    :disabled="!!current.craftSelections?.[g]"
                    @click.stop="removeParameter(g, p)"
                    >删除</el-button
                  >
                </div>
              </section>
            </section>
            <footer class="review-actions"
              ><span>检查完成后可切换工序，或直接查看发布清单。</span
              ><el-button :disabled="currentIndex >= processes.length - 1" @click="nextProcess"
                >下一道工序</el-button
              ></footer
            >
          </el-form>
        </main>
        <section v-else class="parser-editor"
          ><ParserProcessToolbar
            :index="-1"
            :count="0"
            :can-undo="!!history.length"
            :disabled="busy || !!published"
            @undo="undo"
            @move="moveProcess"
            @remove="removeProcess" /><el-empty description="暂无工序，请添加工序"
        /></section>
      </div>
      <ParserIssueList
        v-if="!published"
        :issues="issues"
        :disabled="busy"
        @locate="(index) => editIssue(issues[index])"
      />
    </template>
    <SimpleParameterDialog
      ref="parameterDialog"
      :targets="parameterTargets"
      @save="saveParameter"
    />
    <el-dialog
      v-model="publishVisible"
      :title="published ? '创建结果' : '预览发布'"
      width="min(1000px, 95vw)"
      :close-on-click-modal="false"
      :close-on-press-escape="!publishing"
      :show-close="!publishing"
      @closed="focusIssue"
    >
      <p>所属项目：{{ selectedProject?.projectCode }} / {{ selectedProject?.projectName }}</p>
      <h3>发布内容</h3>
      <p>{{ routeName }} · {{ processes.length }} 道工序</p>
      <template v-if="publishPreview">
        <el-alert
          v-if="!published"
          title="确认后将创建清单中的设备、工序、工艺和新工艺流程。流程先保存为禁用路线及草稿候选，再按现有规则配置与启用。"
          type="info"
          :closable="false"
        />
        <p class="preview-flow"
          ><strong>{{ publishPreview.routeName }}</strong> · 新建工艺流程</p
        >

        <template v-if="publishPreview.unmatchedMaterials?.length">
          <el-alert
            :title="`有 ${publishPreview.unmatchedMaterials.length} 项物料未匹配，可以继续发布。本次只绑定已匹配物料，其余可在工艺路线中补充。`"
            type="warning"
            :closable="false"
            show-icon
          />
          <el-collapse>
            <el-collapse-item title="查看未匹配物料" name="unmatchedMaterials">
              <p v-for="(material, i) in publishPreview.unmatchedMaterials" :key="i">{{
                material
              }}</p>
            </el-collapse-item>
          </el-collapse>
        </template>
        <el-collapse v-if="publishPreview.warnings?.length"
          ><el-collapse-item title="目录名称核对说明" name="warnings"
            ><p v-for="(warning, i) in publishPreview.warnings" :key="i">{{
              warning
            }}</p></el-collapse-item
          ></el-collapse
        >
        <el-tabs v-model="previewTab">
          <el-tab-pane :label="`工序（${publishPreview.processRows.length}）`" name="process"
            ><el-table :data="publishPreview.processRows" max-height="350"
              ><el-table-column label="操作" width="85"
                ><template #default="{ row }"
                  ><el-tag :type="row.action === 'CREATE' ? 'success' : 'info'">{{
                    actionLabel(row.action)
                  }}</el-tag></template
                ></el-table-column
              ><el-table-column label="名称" prop="name" /><el-table-column
                label="编号"
                min-width="150"
                ><template #default="{ row }">{{
                  row.code || '确认后自动生成'
                }}</template></el-table-column
              ></el-table
            ></el-tab-pane
          >
          <el-tab-pane :label="`设备（${publishPreview.deviceRows.length}）`" name="device"
            ><el-table :data="publishPreview.deviceRows" max-height="350"
              ><el-table-column label="操作" width="85"
                ><template #default="{ row }"
                  ><el-tag :type="row.action === 'CREATE' ? 'success' : 'info'">{{
                    actionLabel(row.action)
                  }}</el-tag></template
                ></el-table-column
              ><el-table-column label="设备编号" prop="code" /><el-table-column
                label="设备名称"
                prop="name" /></el-table
          ></el-tab-pane>
          <el-tab-pane :label="`工艺（${publishPreview.craftRows.length}）`" name="craft"
            ><el-table :data="publishPreview.craftRows" max-height="350"
              ><el-table-column label="操作" width="85"
                ><template #default="{ row }"
                  ><el-tag :type="row.action === 'CREATE' ? 'success' : 'info'">{{
                    actionLabel(row.action)
                  }}</el-tag></template
                ></el-table-column
              ><el-table-column label="名称" prop="name" /><el-table-column label="所属工序"
                ><template #default="{ row }">{{
                  processes[row.processIndex]?.value.name
                }}</template></el-table-column
              ><el-table-column label="参数数" prop="parameterCount" width="85" /><el-table-column
                label="编号"
                ><template #default="{ row }">{{
                  row.code || '确认后自动生成'
                }}</template></el-table-column
              ></el-table
            ></el-tab-pane
          >
          <el-tab-pane label="工艺流程" name="route"
            ><ol class="flow-preview"
              ><li v-for="(process, i) in processes" :key="process.id"
                ><strong>{{ i + 1 }}. {{ process.value.name }}</strong
                ><el-tag v-if="process.value.criticalProcess" size="small" type="warning"
                  >关键 / 特殊</el-tag
                ><p v-for="(group, g) in process.value.equipmentGroups" :key="g"
                  >设备：{{
                    group.equipmentOptions
                      ?.map((device) => `${device.code} ${device.name}`)
                      .join('、')
                  }}
                  · {{ group.parameters?.length || 0 }} 项参数</p
                ></li
              ></ol
            ></el-tab-pane
          >
        </el-tabs>
        <el-alert v-if="publishError" :title="publishError" type="error" :closable="false" />
      </template>
      <ParserIssueList
        v-if="!published"
        :issues="previewIssues"
        :disabled="busy"
        @locate="(index) => editIssue(previewIssues[index])"
      />
      <template #footer
        ><el-button :disabled="publishing" @click="publishVisible = false">{{
          published ? '关闭' : '返回修改'
        }}</el-button
        ><el-button v-if="published" type="primary" @click="openRoute">查看结果</el-button
        ><el-button
          v-else
          type="primary"
          :loading="publishing"
          :disabled="busy || !publishPreview || !!previewIssues.length"
          @click="publish"
          >确认发布</el-button
        ></template
      >
    </el-dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { getRouteProjectOptions } from '@/api/mes/pro/route'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import { ElMessageBox, type UploadFile } from 'element-plus'
import Draggable from 'vuedraggable'
import { BatchRecordReportApi } from '@/api/mes/pro/batchrecordreport'
import {
  SimpleParserApi,
  type SimpleParserDevice,
  type SimpleParserRequest,
  type SimpleParserResult
} from '@/api/mes/pro/batchrecordreport/simpleParser'
import { ProProcessApi, type ProProcessVO } from '@/api/mes/pro/process'
import { CraftApi, type Craft, type CraftParameter } from '@/api/mes/pro/craft'
import { MdItemApi, type MdItemVO } from '@/api/mes/md/item'
import { useUserStore } from '@/store/modules/user'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import {
  getProjectCode,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import type {
  BatchRecordTotalRecognitionJson as Recognition,
  BatchRecordTotalRecognitionMaterial as Material,
  BatchRecordTotalRecognitionParameter as Parameter
} from './productionRecognition'
import {
  clone,
  newProcess,
  parseRecognition,
  reviewIssues,
  type EditableProcess,
  type SimpleDraft
} from './simpleRecognition'
import SimpleParserHeader from './components/SimpleParserHeader.vue'
import SimpleParameterDialog from './components/SimpleParameterDialog.vue'
import SimpleTrialPreview from './components/SimpleTrialPreview.vue'
import ParserProcessToolbar from './components/ParserProcessToolbar.vue'
import ParserTrialNavigation from './components/ParserTrialNavigation.vue'
import ParserIssueList from './components/ParserIssueList.vue'
import { useParserEditing } from './useParserEditing'
import type { ReviewIssue } from './simpleRecognition'
import { exportSimpleWord, readSimpleWord, type SimpleWordContent } from './simpleWord'
import wordTemplateUrl from '@/assets/templates/simple-parser-export.docx?url'

defineOptions({ name: 'FormCenterSimpleParserWorkbench' })
const message = useMessage(),
  router = useRouter()
const projectId = ref<number>()
const projects = ref<(DccProjectCodeRespVO & { occupied?: boolean })[]>([])
const projectLoading = ref(false)
const selectedProject = computed(() => projects.value.find((p) => p.id === projectId.value))
let projectQuery = 0
async function searchProjects(keyword = '') {
  const query = ++projectQuery
  projectLoading.value = true
  try {
    const list = await getRouteProjectOptions(keyword)
    if (query !== projectQuery) return
    const selected = selectedProject.value
    projects.value =
      selected && !list.some((p) => p.id === selected.id)
        ? [selected, ...list]
        : list
  } catch (cause) {
    if (query === projectQuery) error.value = `项目列表加载失败：${errorText(cause)}`
  } finally {
    if (query === projectQuery) projectLoading.value = false
  }
}
const recognition = ref<Recognition>(),
  processes = ref<EditableProcess[]>([])
const filename = ref(''),
  routeName = ref(''),
  selectedId = ref(''),
  mode = ref('edit'),
  error = ref('')
const loadingText = ref('正在解析，请稍候…')
const loading = ref(false),
  exporting = ref(false),
  publishing = ref(false),
  previewLoading = ref(false)
const busy = computed(
  () => loading.value || exporting.value || publishing.value || previewLoading.value
)
const busyText = computed(() =>
  publishing.value
    ? '正在发布，请稍候…'
    : exporting.value
      ? '正在导出，请稍候…'
      : previewLoading.value
        ? '正在生成发布预览，请稍候…'
        : loadingText.value
)
const requestId = ref<string>(crypto.randomUUID()),
  publishPreview = ref<SimpleParserResult>(),
  published = ref<SimpleParserResult>(),
  pendingRequest = ref<SimpleParserRequest>()
const publishVisible = ref(false),
  publishError = ref(''),
  previewTab = ref('process')
const deviceOptions = ref<SimpleParserDevice[]>([]),
  deviceLoading = ref(false),
  deviceError = ref('')
const processOptions = ref<ProProcessVO[]>([]),
  processLoading = ref(false),
  processError = ref(''),
  reuseVisible = ref(false)
const craftOptions = ref<Craft[]>([]),
  craftLoading = ref(false),
  craftError = ref(''),
  materialError = ref('')
const directions = [
  { key: 'inputs', label: '输入物料' },
  { key: 'outputs', label: '输出物料' }
] as const
const currentIndex = computed(() => processes.value.findIndex((p) => p.id === selectedId.value))
const current = computed(() => processes.value[currentIndex.value])
const knownDevice = (code?: string | null) =>
  deviceOptions.value.find((device) => device.deviceCode === code)
const issues = computed(() => {
  const result = reviewIssues(processes.value)
  if (!routeName.value.trim()) result.unshift({ process: -1, message: '请填写工艺流程名称' })
  if (!processes.value.length)
    result.push({ process: -1, message: '请添加工序', target: 'addProcess' })
  const reusedProcesses = new Set<number>()
  processes.value.forEach((process, index) => {
    if (process.processId != null) {
      if (reusedProcesses.has(process.processId))
        result.push({
          process: index,
          message: '同一工艺流程不能重复选择同一个已有工序',
          target: 'reuse'
        })
      reusedProcesses.add(process.processId)
    }
    const codes = new Set<string>()
    process.value.equipmentGroups?.forEach((group, g) =>
      group.equipmentOptions?.forEach((equipment, e) => {
        if (equipment.code && codes.has(equipment.code))
          result.push({
            process: index,
            message: `设备 ${equipment.code} 在本工序重复，请保留一处`,
            target: `equipment-${g}-${e}`
          })
        if (equipment.code) codes.add(equipment.code)
      })
    )
  })
  return result
})
const previewIssues = computed(() => [
  ...issues.value,
  ...(publishPreview.value?.blockers.map((message) => ({
    message,
    process: -1,
    locatable: false
  })) || [])
])
const pendingIssue = ref<ReviewIssue>()
async function editIssue(issue: ReviewIssue) {
  if (busy.value || published.value) return
  pendingIssue.value = issue
  const closing = publishVisible.value
  publishVisible.value = false
  if (!closing) await focusIssue()
}
async function focusIssue() {
  const issue = pendingIssue.value
  if (!issue) return
  pendingIssue.value = undefined
  mode.value = 'edit'
  if (issue.process < 0) {
    await nextTick()
    const target =
      issue.target === 'addProcess'
        ? workspaceEditor.value?.querySelector<HTMLElement>('[data-issue-field="addProcess"]')
        : metadataEditor.value?.querySelector<HTMLElement>('[aria-label="工艺流程名称"]')
    target?.scrollIntoView({ block: 'center' })
    target?.focus()
    return
  }
  selectProcess(processes.value[issue.process].id)
  await nextTick()
  if (issue.parameter != null && issue.group != null) {
    editParameter(issue.group, issue.parameter, issue.target)
    return
  }
  const target = issue.target
    ? editor.value?.querySelector<HTMLElement>(`[data-issue-field="${issue.target}"]`)
    : editor.value
  target?.scrollIntoView({ block: 'center' })
  ;(target?.matches('input, textarea, button')
    ? target
    : target?.querySelector<HTMLElement>('input, textarea, button, [tabindex="0"]')
  )?.focus({ preventScroll: true })
}
const errorText = (e: unknown) =>
  e instanceof Error ? e.message : typeof e === 'string' ? e : '操作失败，请按提示重试'
const actionLabel = (action: string) => (action === 'CREATE' ? '新建' : '使用已有')
const controlLabel = (control?: string) =>
  ({ number: '数字', select: '下拉选择', text: '文字', input: '文字' })[control || ''] ||
  '类型待确认'
const trialSession = ref(0)
const editor = ref<HTMLElement>()
const metadataEditor = ref<HTMLElement>()
const workspaceEditor = ref<HTMLElement>()
const publishedFlag = computed(() => !!published.value)
const {
  history,
  dirty,
  savedAt,
  reset: resetEditing,
  markSaved,
  undo
} = useParserEditing(
  () =>
    recognition.value ? { processes: processes.value, routeName: routeName.value } : undefined,
  (state) => {
    const index = currentIndex.value
    processes.value = state.processes
    routeName.value = state.routeName
    if (!processes.value.some((p) => p.id === selectedId.value))
      selectProcess(
        processes.value[Math.max(0, Math.min(index, processes.value.length - 1))]?.id || ''
      )
  },
  publishedFlag
)
let restoring = false
function mutate(action: () => void) {
  if (published.value || busy.value) return
  action()
}
function reorderProcesses(list: EditableProcess[]) {
  mutate(() => {
    processes.value = list
  })
}
function moveProcess(offset: number) {
  const from = currentIndex.value,
    to = from + offset
  if (from < 0 || to < 0 || to >= processes.value.length) return
  mutate(() => {
    const [entry] = processes.value.splice(from, 1)
    processes.value.splice(to, 0, entry)
  })
}
watch(
  [processes, routeName, projectId],
  () => {
    if (restoring || !recognition.value || published.value) return
    requestId.value = crypto.randomUUID()
    publishPreview.value = undefined
    pendingRequest.value = undefined
    publishError.value = ''
  },
  { deep: true, flush: 'sync' }
)
function selectProcess(id: string) {
  selectedId.value = id
  reuseVisible.value = false
  craftOptions.value = []
  craftError.value = ''
  processError.value = ''
  if (current.value?.processId) void loadCrafts()
}
function processStatus(process: EditableProcess, index: number) {
  const count = issues.value.filter((issue) => issue.process === index).length
  return count
    ? `${count} 项待修正`
    : process.processId
      ? `使用已有 ${process.processCode || ''}`
      : '默认新建'
}
async function confirmAction(text: string) {
  try {
    await ElMessageBox.confirm(text, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    return true
  } catch (reason) {
    if (reason === 'cancel' || reason === 'close') return false
    throw reason
  }
}
async function importWord(upload: UploadFile) {
  if (busy.value) return
  if (!projectId.value) {
    message.warning('请先选择所属项目')
    return
  }
  if (!upload.raw || !/\.docx?$/i.test(upload.raw.name)) {
    error.value = '请选择 DOC 或 DOCX 文件'
    return
  }
  if (dirty.value && !(await confirmAction('导入新文档将替换当前未保存内容，是否继续？'))) return
  loadingText.value = '正在解析，请稍候…'
  loading.value = true
  error.value = ''
  try {
    const restored = /\.docx$/i.test(upload.raw.name)
      ? await readSimpleWord(await upload.raw.arrayBuffer())
      : undefined
    let result: Recognition
    if (restored) result = restored.recognition
    else {
      await loadDevices()
      if (deviceError.value) throw new Error(deviceError.value)
      result = parseRecognition(
        await BatchRecordReportApi.parseProductionBatchRecordTotalRecognitionJson(upload.raw)
      )
    }
    restoring = true
    recognition.value = result
    processes.value = restored
      ? restored.processes
      : result.processes.map((value) => ({
          id: crypto.randomUUID(),
          sourceName: value.name,
          value: clone(value)
        }))
    if (!restored)
      processes.value.forEach((process) =>
        process.value.equipmentGroups?.forEach((group) =>
          group.equipmentOptions?.forEach((equipment) => {
            const device = knownDevice(equipment.code)
            if (device) equipment.name = device.deviceName
          })
        )
      )
    filename.value = restored ? restored.filename : upload.raw.name
    routeName.value = restored ? restored.routeName : `${result.product.name || '生产记录'}工艺流程`
    requestId.value = crypto.randomUUID()
    selectedId.value = processes.value[0]?.id || ''
    mode.value = 'edit'
    publishPreview.value = undefined
    published.value = undefined
    pendingRequest.value = undefined
    trialSession.value++
    resetEditing()
    reuseVisible.value = false
    craftOptions.value = []
    craftError.value = ''
    if (current.value?.processId) void loadCrafts()
    message.success(
      restored
        ? `已完整恢复 ${processes.value.length} 道工序`
        : `已识别 ${processes.value.length} 道工序，请检查识别内容`
    )
  } catch (cause) {
    error.value = errorText(cause)
  } finally {
    restoring = false
    loading.value = false
  }
}
async function exportWord() {
  if (busy.value || !recognition.value) return
  exporting.value = true
  error.value = ''
  try {
    // Preserve the editor exactly; publishing may separately resolve names from the device directory.
    const content: SimpleWordContent = clone({
      filename: filename.value,
      routeName: routeName.value,
      recognition: { ...recognition.value, processes: processes.value.map((p) => p.value) },
      processes: processes.value
    })
    const response = await fetch(wordTemplateUrl)
    if (!response.ok) throw new Error(`Word 模板加载失败（${response.status}）`)
    const blob = await exportSimpleWord(await response.arrayBuffer(), content)
    const href = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = href
    link.download = `${(content.routeName.trim() || '简易解析').replace(/[\\/:*?"<>|\u0000-\u001f]/g, '_')}.docx`
    document.body.appendChild(link)
    link.click()
    link.remove()
    setTimeout(() => URL.revokeObjectURL(href), 60_000)
    message.success('Word 已导出，可再次导入恢复当前编辑内容')
  } catch (cause) {
    error.value = `Word 导出失败：${errorText(cause)}`
  } finally {
    exporting.value = false
  }
}
function insertProcess(index: number) {
  mutate(() => {
    const process = newProcess()
    processes.value.splice(index, 0, process)
    selectProcess(process.id)
  })
}
async function removeProcess() {
  if (
    !current.value ||
    !(await confirmAction(
      `删除「${current.value.value.name || '未命名工序'}」及其设备和参数？删除后可撤销。`
    ))
  )
    return
  mutate(() => {
    const index = currentIndex.value
    processes.value.splice(index, 1)
    selectProcess(processes.value[Math.min(index, processes.value.length - 1)]?.id || '')
  })
}
function nextProcess() {
  if (currentIndex.value < processes.value.length - 1)
    selectProcess(processes.value[currentIndex.value + 1].id)
}
function addMaterial(direction: 'inputs' | 'outputs') {
  mutate(() => {
    ;(current.value.value[direction] ??= []).push({ name: '', code: '' })
  })
}
function removeMaterial(direction: 'inputs' | 'outputs', index: number) {
  mutate(() => {
    current.value.value[direction]!.splice(index, 1)
  })
}
async function searchMaterials(keyword: string, callback: (items: any[]) => void) {
  if (!keyword.trim()) {
    callback([])
    return
  }
  try {
    const result = await MdItemApi.getItemPage({
      pageNo: 1,
      pageSize: 30,
      name: keyword.trim(),
      status: 0
    })
    materialError.value = ''
    callback(
      result.list.map((item: MdItemVO) => ({ ...item, label: `${item.code} / ${item.name}` }))
    )
  } catch (cause) {
    materialError.value = `物料目录查询失败：${errorText(cause)}`
    callback([])
  }
}
function editMaterialName(material: Material, name: string) {
  material.name = name
}
function chooseMaterial(material: Material, item: Record<string, any>) {
  mutate(() => {
    material.code = item.code
    material.name = item.name
  })
}
function addGroup() {
  mutate(() => {
    ;(current.value.value.equipmentGroups ??= []).push({
      equipmentOptions: [],
      parameters: [],
      selectionMode: 'SINGLE'
    })
  })
}
async function removeGroup(index: number) {
  const group = current.value.value.equipmentGroups![index]
  if (
    !(await confirmAction(
      `删除本组 ${group.equipmentOptions?.length || 0} 台设备和 ${group.parameters?.length || 0} 个参数？`
    ))
  )
    return
  mutate(() => {
    current.value.value.equipmentGroups!.splice(index, 1)
    const choices = current.value.craftSelections || {}
    current.value.craftSelections = Object.fromEntries(
      Object.entries(choices)
        .filter(([key]) => Number(key) !== index)
        .map(([key, value]) => [Number(key) > index ? Number(key) - 1 : Number(key), value])
    )
  })
}
function addEquipment(index: number) {
  mutate(() => {
    ;(current.value.value.equipmentGroups![index].equipmentOptions ??= []).push({
      name: '',
      code: ''
    })
  })
}
function chooseEquipment(group: number, index: number, code: string) {
  const device = knownDevice(code)
  if (!device) {
    error.value = '所选设备编号已不在设备列表，请重新加载'
    return
  }
  mutate(() => {
    const equipment = current.value.value.equipmentGroups![group].equipmentOptions![index]
    equipment.code = device.deviceCode
    equipment.name = device.deviceName
  })
}
function removeEquipment(group: number, index: number) {
  mutate(() => {
    current.value.value.equipmentGroups![group].equipmentOptions!.splice(index, 1)
  })
}
function removeParameter(group: number, index: number) {
  mutate(() => {
    current.value.value.equipmentGroups![group].parameters!.splice(index, 1)
  })
}
async function loadDevices() {
  deviceLoading.value = true
  deviceError.value = ''
  try {
    deviceOptions.value = await SimpleParserApi.devices()
    if (
      new Set(deviceOptions.value.map((device) => device.deviceCode)).size !==
      deviceOptions.value.length
    )
      throw new Error('设备编号存在重复，请先在设备管理中处理')
  } catch (cause) {
    deviceOptions.value = []
    deviceError.value = `设备列表加载失败：${errorText(cause)}`
  } finally {
    deviceLoading.value = false
  }
}
let processQuery = 0
async function loadProcesses(keyword = '') {
  const query = ++processQuery
  processLoading.value = true
  try {
    const result = await ProProcessApi.getProcessPage({
      pageNo: 1,
      pageSize: 50,
      name: keyword.trim() || undefined,
      status: 0
    })
    if (query !== processQuery) return
    processOptions.value = result.list
    processError.value = ''
  } catch (cause) {
    if (query === processQuery) {
      processOptions.value = []
      processError.value = `工序查询失败：${errorText(cause)}`
    }
  } finally {
    if (query === processQuery) processLoading.value = false
  }
}
function openProcessPicker() {
  reuseVisible.value = true
  loadProcesses(current.value.value.name)
}
function chooseProcess(id: number) {
  const process = processOptions.value.find((item) => item.id === id)
  if (!process) return
  mutate(() => {
    current.value.processId = id
    current.value.processCode = process.code
    current.value.value.name = process.name
    current.value.craftSelections = {}
  })
  craftOptions.value = []
}
function useNewProcess() {
  mutate(() => {
    delete current.value.processId
    delete current.value.processCode
    current.value.craftSelections = {}
  })
  reuseVisible.value = false
  craftOptions.value = []
}
async function loadCrafts() {
  const id = current.value?.processId,
    owner = current.value?.id
  if (!id) return
  craftLoading.value = true
  craftError.value = ''
  try {
    const list = await CraftApi.list(id)
    if (current.value?.id === owner) craftOptions.value = list
  } catch (cause) {
    if (current.value?.id === owner) {
      craftOptions.value = []
      craftError.value = `工艺查询失败：${errorText(cause)}`
    }
  } finally {
    craftLoading.value = false
  }
}
function fromCraft(parameter: CraftParameter): Parameter {
  const numeric = ['INTEGER', 'DECIMAL'].includes(parameter.valueType)
  if (!numeric && !['SELECT', 'TEXT_STANDARD'].includes(parameter.valueType))
    throw new Error(`暂不支持编辑 ${parameter.valueType} 类型工艺，请选择新建工艺`)
  return {
    name: parameter.parameterName,
    referenceValue: parameter.standardText,
    ui: {
      control: numeric ? 'number' : parameter.valueType === 'SELECT' ? 'select' : 'text',
      unit: parameter.unit || '',
      defaultValue: numeric ? parameter.defaultValue : parameter.defaultText,
      min: parameter.lowerLimit,
      max: parameter.upperLimit,
      step: numeric ? 10 ** -(parameter.decimalScale || 0) : undefined,
      options: parameter.optionValuesJson ? JSON.parse(parameter.optionValuesJson) : undefined
    }
  }
}
function chooseCraft(group: number, id?: number) {
  if (!id) {
    mutate(() => {
      if (current.value.craftSelections) delete current.value.craftSelections[group]
    })
    return
  }
  const craft = craftOptions.value.find((item) => item.id === id)
  if (!craft) return
  try {
    const parameters = (JSON.parse(craft.parametersJson) as CraftParameter[]).map(fromCraft)
    mutate(() => {
      ;(current.value.craftSelections ??= {})[group] = id
      current.value.value.equipmentGroups![group].parameters = parameters
    })
  } catch (cause) {
    craftError.value = errorText(cause)
  }
}
const parameterDialog = ref<InstanceType<typeof SimpleParameterDialog>>()
let parameterOrigin: { processId: string; group: number; parameter?: number } | undefined
const parameterTargets = computed(() =>
  processes.value.flatMap(
    (process) =>
      process.value.equipmentGroups?.flatMap((group, index) =>
        process.craftSelections?.[index]
          ? []
          : [
              {
                value: `${process.id}:${index}`,
                label: `${process.value.name || '未命名工序'} / 组${index + 1} ${group.equipmentOptions?.map((item) => item.name).join('、') || '尚未选择设备'}`
              }
            ]
      ) || []
  )
)
function editParameter(group: number, parameter?: number, target?: string) {
  if (busy.value || published.value || current.value.craftSelections?.[group]) return
  parameterOrigin = { processId: current.value.id, group, parameter }
  parameterDialog.value?.open(
    parameter == null
      ? { name: '', ui: { control: 'number', step: 1 } }
      : current.value.value.equipmentGroups![group].parameters![parameter],
    `${current.value.id}:${group}`,
    target
  )
}
function saveParameter(parameter: Parameter, location: string) {
  const [id, group] = location.split(':'),
    origin = parameterOrigin
  const process = processes.value.find((item) => item.id === id),
    destination = process?.value.equipmentGroups?.[Number(group)]
  if (!origin || !destination || process?.craftSelections?.[Number(group)]) {
    error.value = '参数归属已改变，请重新编辑'
    return
  }
  mutate(() => {
    const original = processes.value.find((item) => item.id === origin.processId)!.value
      .equipmentGroups![origin.group]
    if (origin.parameter != null && id === origin.processId && Number(group) === origin.group)
      original.parameters![origin.parameter] = parameter
    else {
      if (origin.parameter != null) original.parameters!.splice(origin.parameter, 1)
      ;(destination.parameters ??= []).push(parameter)
    }
  })
}
function output(): Recognition {
  return {
    ...clone(recognition.value!),
    processes: processes.value.map((item) => {
      const result = clone(item.value)
      result.equipmentGroups?.forEach((group) =>
        group.equipmentOptions?.forEach((equipment) => {
          const device = knownDevice(equipment.code)
          if (device) equipment.name = device.deviceName
        })
      )
      return result
    })
  }
}
function buildRequest(): SimpleParserRequest {
  return {
    requestId: requestId.value,
    projectId: projectId.value!,
    routeName: routeName.value.trim(),
    recognitionJson: output() as unknown as Record<string, unknown>,
    processSelections: processes.value.map((process, processIndex) => ({
      processIndex,
      processId: process.processId,
      craftSelections: Object.entries(process.craftSelections || {}).map(
        ([groupIndex, craftId]) => ({ groupIndex: Number(groupIndex), craftId })
      )
    }))
  }
}
function draftKey() {
  const tenant = getVisitTenantId() ?? getTenantId(),
    user = useUserStore().getUser.id
  if (tenant == null || !user) throw new Error('缺少当前账号信息，无法存取草稿')
  return `form-center:simple-parser:v1:${tenant}:${user}`
}
function persistDraft() {
  if (!selectedProject.value) throw new Error('请先选择所属项目')
  const time = new Date().toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false })
  const draft: SimpleDraft = {
    version: 1,
    filename: filename.value,
    recognition: output(),
    processes: clone(processes.value),
    source: [],
    reviewed: [],
    savedAt: time,
    requestId: requestId.value,
    project: {
      id: selectedProject.value.id,
      projectCode: selectedProject.value.projectCode,
      projectName: selectedProject.value.projectName
    },
    routeName: routeName.value,
    published: published.value
  }
  localStorage.setItem(draftKey(), JSON.stringify(draft))
  markSaved(time)
}
function saveDraft() {
  try {
    persistDraft()
    message.success('草稿已保存在当前浏览器')
  } catch (cause) {
    error.value = `草稿保存失败：${errorText(cause)}`
  }
}
async function restoreDraft() {
  if (busy.value) return
  if (dirty.value && !(await confirmAction('恢复草稿将替换当前未保存内容，是否继续？'))) return
  loadingText.value = '正在恢复草稿，请稍候…'
  loading.value = true
  try {
    const text = localStorage.getItem(draftKey())
    if (!text) {
      message.info('当前账号没有草稿')
      return
    }
    const draft = JSON.parse(text) as SimpleDraft
    if (!draft.project?.id) throw new Error('草稿缺少所属项目，请选择项目后重新导入 Word')
    const project = await getProjectCode(draft.project.id)
    if (project.status !== 'ENABLE') throw new Error('草稿所属项目已停用，无法恢复')
    if (
      draft.version !== 1 ||
      !Array.isArray(draft.processes) ||
      draft.processes.some((item) => !item.id)
    )
      throw new Error('草稿内容无效')
    const result = parseRecognition(
      JSON.stringify({ ...draft.recognition, processes: draft.processes.map((item) => item.value) })
    )
    restoring = true
    projects.value = [project, ...projects.value.filter((p) => p.id !== project.id)]
    projectId.value = project.id
    recognition.value = result
    processes.value = draft.processes
    filename.value = draft.filename
    routeName.value = draft.routeName || `${result.product.name || '生产记录'}工艺流程`
    requestId.value = draft.requestId || crypto.randomUUID()
    published.value = draft.published as SimpleParserResult | undefined
    publishPreview.value = published.value
    pendingRequest.value = undefined
    selectedId.value = processes.value[0]?.id || ''
    trialSession.value++
    resetEditing(true, draft.savedAt)
    error.value = ''
    mode.value = 'edit'
    if (current.value?.processId) await loadCrafts()
    message.success('草稿已恢复')
  } catch (cause) {
    error.value = `草稿恢复失败：${errorText(cause)}`
  } finally {
    restoring = false
    loading.value = false
  }
}
async function openPublish() {
  if (busy.value || published.value) return
  error.value = ''
  if (!projectId.value) {
    message.warning('请先选择所属项目')
    return
  }
  if (issues.value.length) {
    publishPreview.value = undefined
    pendingRequest.value = undefined
    publishVisible.value = true
    return
  }
  if (deviceError.value || deviceLoading.value) {
    error.value = '请先成功加载设备目录，再预览发布'
    return
  }
  previewLoading.value = true
  publishError.value = ''
  try {
    const data = buildRequest(),
      result = await SimpleParserApi.preview(data)
    if (data.requestId !== requestId.value) throw new Error('预览期间内容已修改，请重新预览')
    publishPreview.value = result
    pendingRequest.value = { ...data, previewToken: result.previewToken }
    publishVisible.value = true
    previewTab.value = 'process'
    if (result.routeId) {
      published.value = result
      message.success('这份草稿此前已发布，已恢复创建结果')
    }
  } catch (cause) {
    error.value = `发布预览失败：${errorText(cause)}`
  } finally {
    previewLoading.value = false
  }
}
async function publish() {
  if (
    busy.value ||
    published.value ||
    issues.value.length ||
    !pendingRequest.value ||
    publishPreview.value?.blockers.length
  )
    return
  publishing.value = true
  publishError.value = ''
  try {
    // ponytail: 同一草稿使用同一请求编号重试，先保存再创建，避免断线后重复建档。
    persistDraft()
    const result = await SimpleParserApi.publish(pendingRequest.value)
    published.value = result
    publishPreview.value = result
    publishVisible.value = false
    try {
      persistDraft()
    } catch (cause) {
      error.value = `发布已成功，但本地结果保存失败：${errorText(cause)}`
    }
    message.success('设备、工序、工艺和工艺流程已按清单创建')
  } catch (cause) {
    publishError.value = errorText(cause)
  } finally {
    publishing.value = false
  }
}
function showPublished() {
  publishPreview.value = published.value
  publishVisible.value = true
}
function openRoute() {
  if (published.value?.routeId)
    router.push({
      path: `/mes/pro/route/edit/${published.value.routeId}`,
      query: {
        tab: 'flow',
        routeVersionId: String(
          published.value.routeCandidateVersionId || published.value.routeVersionId
        ),
        routeVersionNo: published.value.routeCandidateVersionNo || published.value.routeVersionNo,
        routeVersionStatus: published.value.routeCandidateVersionId ? 'DRAFT' : 'ACTIVE'
      }
    })
}
function beforeUnload(event: BeforeUnloadEvent) {
  if (dirty.value || busy.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}
window.addEventListener('beforeunload', beforeUnload)
onBeforeUnmount(() => window.removeEventListener('beforeunload', beforeUnload))
onMounted(loadDevices)
onMounted(() => searchProjects())
onBeforeRouteLeave(() => {
  if (busy.value) {
    message.warning('正在处理，请稍后离开')
    return false
  }
  // 切换页签由 KeepAlive 保留未保存内容；刷新或关闭浏览器仍由 beforeUnload 提醒。
  return true
})
</script>

<style scoped lang="scss">
.section-title,
.toolbar,
.workbench-bar,
.review-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.section-title,
.review-actions {
  justify-content: space-between;
}
.source-filename {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  overflow-wrap: anywhere;
}
.toolbar {
  flex-wrap: wrap;
  .el-button + .el-button {
    margin-left: 0;
  }
}
.route-name {
  width: 100%;
}
.workbench-bar {
  flex-wrap: wrap;
  margin-bottom: 12px;
  .toolbar {
    margin-left: auto;
  }
}
.draft-note,
.empty-note,
.review-summary {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.draft-note {
  margin: 8px 0 0;
}
.status-message {
  margin-top: 16px;
}
.workbench {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  align-items: start;
  gap: 22px;
}
.process-list {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 12px;
  position: sticky;
  top: 16px;
  max-height: calc(100vh - 250px);
  overflow-y: auto;
}
.process-item {
  display: flex;
  align-items: center;
  border: 1px solid transparent;
  border-radius: 5px;
  margin-top: 6px;
  button {
    flex: 1;
    padding: 12px 8px;
    border: 0;
    background: transparent;
    color: var(--el-text-color-primary);
    text-align: left;
    cursor: pointer;
    min-width: 0;
  }
  span {
    overflow-wrap: anywhere;
  }
  small {
    display: block;
    margin-top: 6px;
    color: var(--el-text-color-secondary);
  }
  &.active {
    background: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary-light-5);
  }
}
.drag-handle {
  cursor: grab;
  padding-left: 6px;
  color: var(--el-text-color-secondary);
}
.process-canvas {
  min-width: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 24px;
  h3 {
    font-size: 16px;
    margin: 8px 0;
  }
}
.process-form {
  display: grid;
  grid-template-columns: minmax(200px, 1fr) 220px;
  gap: 20px;
  margin-top: 20px;
}
.reuse-process,
.reuse-craft {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  .el-select {
    max-width: 520px;
    flex: 1;
    min-width: 200px;
  }
}
.editor-section {
  border-top: 1px solid var(--el-border-color-light);
  padding: 18px 0;
}
.material-row,
.equipment-row {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) minmax(150px, 1fr) auto;
  align-items: center;
  gap: 10px;
  margin: 12px 0;
  > * {
    min-width: 0;
  }
}
.equipment-group {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
  padding: 18px;
  margin-top: 16px;
}
.selection-mode {
  width: 180px;
  margin: 12px 0 0;
}
.parameter-card {
  display: flex;
  align-items: center;
  width: 100%;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-bg-color);
  padding: 16px;
  gap: 12px;
  margin-top: 10px;
  cursor: pointer;
  > span:first-child,
  > span:nth-child(2) {
    flex: 1;
    overflow-wrap: anywhere;
    min-width: 0;
  }
  small {
    display: block;
    margin-top: 6px;
    color: var(--el-text-color-secondary);
  }
  b {
    font-weight: 500;
  }
  &:hover {
    border-color: var(--el-color-primary);
  }
}
.parameter-edit {
  color: var(--el-color-primary);
  font-size: 12px;
}
.issue-list {
  padding: 16px;
  border-radius: 6px;
  background: var(--el-color-warning-light-9);
  font-size: 13px;
}
.review-actions {
  flex-wrap: wrap;
  margin-top: 18px;
  span {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}
.error-text {
  color: var(--el-color-danger);
  font-size: 13px;
}
.preview-flow {
  margin: 24px 0 16px;
}
.flow-preview {
  padding-left: 24px;
  li {
    padding: 12px;
    border-bottom: 1px solid var(--el-border-color-light);
  }
  p {
    color: var(--el-text-color-secondary);
    margin: 8px 0;
  }
}
.published-result {
  text-align: center;
  margin-bottom: 24px;
}
@media (max-width: 1100px) {
  .workbench {
    grid-template-columns: 185px minmax(0, 1fr);
    gap: 12px;
  }
  .process-canvas {
    padding: 16px;
  }
  .section-title {
    flex-wrap: wrap;
  }
  .process-form {
    grid-template-columns: 1fr;
    gap: 0;
  }
  .parameter-card {
    flex-wrap: wrap;
  }
  .parameter-edit {
    display: none;
  }
}
@media (max-width: 760px) {
  .workbench {
    grid-template-columns: 1fr;
  }
  .process-list {
    position: static;
    max-height: 220px;
  }
  .material-row,
  .equipment-row {
    grid-template-columns: 1fr 1fr;
  }
}
</style>

<style scoped src="./simpleParserLayout.css"></style>
