<template>
  <ContentWrap>
    <UnifiedListTemplate
      class="dcc-training-mine-list-template"
      table-key="dcc.controlledFile.trainingMine.main"
      :query-model="queryParams"
      label-width="82px"
      query-form-test-id="dcc-training-mine-toolbar"
      :filter-definitions="trainingMineQuickFilterDefinitions"
      :quick-filter-state="trainingMineQuickFilter.state"
      :selected-filter-definition="trainingMineQuickFilter.selectedDefinition.value"
      :operator-options="trainingMineQuickFilter.operatorOptions.value"
      :columns="trainingMineColumns"
      :column-saving="trainingMineColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="trainingMineQuickFilter.updateState"
      @quick-filter-query="trainingMineQuickFilter.applyQuickFilter"
      @column-change="saveTrainingMineColumnConfig"
      @pagination="getList"
    >
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="dcc.controlledFile.trainingMine.main"
          :data="list"
          border
          :stripe="true"
          empty-text="当前暂无培训任务"
          :show-overflow-tooltip="true"
          row-key="progressId"
          @header-dragend="handleTrainingMineHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isTrainingMineColumnVisible('title')"
            label="文件标题"
            prop="title"
            show-overflow-tooltip
            :width="getTrainingMineColumnWidthString('title')"
            :min-width="getTrainingMineColumnMinWidthString('title', 220)"
            v-bind="sortColumnAttrs('title')"
          />
          <el-table-column
            v-if="isTrainingMineColumnVisible('fileNumber')"
            label="文件编号"
            prop="fileNumber"
            :width="getTrainingMineColumnWidthString('fileNumber')"
            :min-width="getTrainingMineColumnMinWidthString('fileNumber', 160)"
            v-bind="sortColumnAttrs('fileNumber')"
          />
          <el-table-column
            v-if="isTrainingMineColumnVisible('versionNo')"
            label="版本"
            align="center"
            prop="versionNo"
            :width="getTrainingMineColumnWidthString('versionNo', 100)"
            v-bind="sortColumnAttrs('versionNo')"
          />
          <el-table-column
            v-if="isTrainingMineColumnVisible('departmentNames')"
            label="来源部门"
            prop="departmentNames"
            :width="getTrainingMineColumnWidthString('departmentNames')"
            :min-width="getTrainingMineColumnMinWidthString('departmentNames', 220)"
            v-bind="sortColumnAttrs('departmentNames')"
          >
            <template #default="{ row }">
              {{ buildDepartmentNames(row.departmentIds, deptNameMap) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isTrainingMineColumnVisible('trainingSummary')"
            label="培训摘要"
            prop="trainingSummary"
            :width="getTrainingMineColumnWidthString('trainingSummary')"
            :min-width="getTrainingMineColumnMinWidthString('trainingSummary', 260)"
            v-bind="sortColumnAttrs('trainingSummary')"
          >
            <template #default="{ row }">
              <div class="training-summary" data-testid="dcc-training-summary">
                <div class="training-summary__main">
                  <el-tag :type="row.trainingSummary.tagType">
                    {{ row.trainingSummary.statusLabel }}
                  </el-tag>
                  <span>{{ row.trainingSummary.progressText }}</span>
                </div>
                <el-progress
                  class="training-summary__progress"
                  :percentage="row.trainingSummary.progressPercent"
                  :show-text="false"
                  :stroke-width="6"
                />
                <div class="training-summary__hint">
                  <span>{{ row.trainingSummary.hintText }}</span>
                  <span>{{ row.trainingSummary.timeText }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isTrainingMineColumnVisible('actions')"
            label="操作"
            prop="actions"
            align="center"
            fixed="right"
            :width="getTrainingMineColumnWidthString('actions', 220)"
          >
            <template #default="{ row }">
              <el-button link type="primary" @click="openTask(row.progressId)">进入培训</el-button>
              <el-button link type="primary" @click="openDetail(row.controlledFileId)">文件详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>
</template>

<script lang="ts" setup>
import {
  getFileCategoryList,
  type ControlledFileCategoryVO
} from '@/api/dcc/controlledFile/fileCategories'
import {
  getMyTrainingTaskPage,
  type TrainingTaskProgressVO
} from '@/api/dcc/controlledFile/training'
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { openControlledFileViewer } from '../../shared/viewer-navigation'
import {
  TRAINING_PROGRESS_STATUS_OPTIONS,
  buildDepartmentNames,
  getTrainingTaskSummary,
  resolveTrainingPageErrorMessage
} from '../presentation'

defineOptions({ name: 'DccControlledFileTrainingMine' })

const router = useRouter()
const route = useRoute()
const message = useMessage()

const loading = ref(false)
const total = ref(0)
const categories = ref<ControlledFileCategoryVO[]>([])
const departments = ref<DeptVO[]>([])

type TrainingTaskProgressRow = TrainingTaskProgressVO & {
  trainingSummary: ReturnType<typeof getTrainingTaskSummary>
}

const list = ref<TrainingTaskProgressRow[]>([])

const trainingMineDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'title', label: '文件标题', minWidth: 220 },
  { key: 'fileNumber', label: '文件编号', minWidth: 160 },
  { key: 'versionNo', label: '版本', width: 100 },
  { key: 'departmentNames', label: '来源部门', minWidth: 220 },
  { key: 'trainingSummary', label: '培训摘要', minWidth: 260 },
  { key: 'actions', label: '操作', width: 220, hideable: false, business: false }
]

const {
  columns: trainingMineColumns,
  saving: trainingMineColumnSaving,
  isColumnVisible: isTrainingMineColumnVisible,
  getColumnWidthString: getTrainingMineColumnWidthString,
  getColumnMinWidthString: getTrainingMineColumnMinWidthString,
  handleHeaderDragend: handleTrainingMineHeaderDragend,
  saveConfig: saveTrainingMineColumnConfig
} = useUserTableColumns('dcc.controlledFile.trainingMine.main', trainingMineDefaultColumns)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  categoryId: undefined as number | undefined,
  status: undefined as string | undefined
})

const deptNameMap = computed(() => new Map(departments.value.map((item) => [item.id, item.name])))

const trainingMineQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'categoryId',
    label: '文件类别',
    type: 'select',
    queryParamKey: 'categoryId',
    options: categories.value
      .filter((item): item is ControlledFileCategoryVO & { id: number } => typeof item.id === 'number')
      .map((item) => ({ label: item.name, value: item.id }))
  },
  {
    key: 'status',
    label: '培训状态',
    type: 'select',
    queryParamKey: 'status',
    options: TRAINING_PROGRESS_STATUS_OPTIONS.map((item) => ({ label: item.label, value: item.value }))
  }
])

const loadBaseData = async () => {
  const [categoryList, deptList] = await Promise.all([getFileCategoryList(), getSimpleDeptList()])
  categories.value = categoryList.filter((item) => item.active)
  departments.value = deptList
}

const getList = async () => {
  loading.value = true
  try {
    const data = await getMyTrainingTaskPage(queryParams)
    list.value = data.list.map((item) => ({
      ...item,
      trainingSummary: getTrainingTaskSummary(item)
    }))
    total.value = data.total
  } catch (error) {
    message.error(resolveTrainingPageErrorMessage(error, '培训任务加载失败，请查看错误提示后重试。'))
  } finally {
    loading.value = false
  }
}

const trainingMineQuickFilter = useTableQuickFilter(
  'dcc.controlledFile.trainingMine.main',
  trainingMineQuickFilterDefinitions,
  queryParams,
  getList
)

const openTask = (progressId: number | string) => {
  router.push({
    name: 'DccTrainingTask',
    params: { progressId }
  })
}

const openDetail = (id: number | string) => {
  openControlledFileViewer(router, route, id, 'training-mine')
}

onMounted(async () => {
  await loadBaseData()
  await getList()
})
</script>

<style scoped>
.training-summary {
  min-width: 0;
}

.training-summary__main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
  color: #172033;
  font-weight: 600;
  line-height: 20px;
}

.training-summary__main span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.training-summary__progress {
  margin: 7px 0;
}

.training-summary__hint {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 4px 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}
</style>
