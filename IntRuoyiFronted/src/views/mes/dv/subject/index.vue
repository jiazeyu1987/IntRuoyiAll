<template>
  <doc-alert title="【设备】点检保养项目、点检保养方案" url="https://doc.iocoder.cn/mes/dv/check-plan/" />

  <ContentWrap>
    <UnifiedListTemplate
      table-key="mes.dv.subject.main"
      :query-model="queryParams"
      label-width="100px"
      :filter-definitions="subjectQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="subjectQuickFilter.state"
      :selected-filter-definition="subjectQuickFilter.selectedDefinition.value"
      :operator-options="subjectQuickFilter.operatorOptions.value"
      :columns="subjectColumns"
      :column-saving="subjectColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="subjectQuickFilter.updateState"
      @quick-filter-query="handleQuickFilterQuery"
      @column-change="saveSubjectColumnConfig"
      @column-reset="resetSubjectColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['mes:dv-subject:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['mes:dv-subject:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </template>

      <template #table>
        <el-table
          v-loading="loading"
          class="subject-standard-list-table"
          data-user-table-column-explicit
          data-user-table-key="mes.dv.subject.main"
          :data="list"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          @header-dragend="handleSubjectHeaderDragend"
        >
          <el-table-column
            v-if="isSubjectColumnVisible('code')"
            label="项目编码"
            align="center"
            prop="code"
            :width="getSubjectColumnWidthString('code', 120)"
          />
          <el-table-column
            v-if="isSubjectColumnVisible('name')"
            label="项目名称"
            align="center"
            prop="name"
            :min-width="getSubjectColumnMinWidthString('name', 150)"
          />
          <el-table-column
            v-if="isSubjectColumnVisible('type')"
            label="项目类型"
            align="center"
            prop="type"
            :width="getSubjectColumnWidthString('type', 100)"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.MES_DV_SUBJECT_TYPE" :value="scope.row.type" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isSubjectColumnVisible('content')"
            label="项目内容"
            align="center"
            prop="content"
            :min-width="getSubjectColumnMinWidthString('content', 200)"
          />
          <el-table-column
            v-if="isSubjectColumnVisible('standard')"
            label="标准"
            align="center"
            prop="standard"
            :min-width="getSubjectColumnMinWidthString('standard', 200)"
          />
          <el-table-column
            v-if="isSubjectColumnVisible('status')"
            label="状态"
            align="center"
            prop="status"
            :width="getSubjectColumnWidthString('status', 80)"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isSubjectColumnVisible('createTime')"
            label="创建时间"
            align="center"
            prop="createTime"
            :formatter="dateFormatter"
            :width="getSubjectColumnWidthString('createTime', 180)"
          />
          <el-table-column
            v-if="isSubjectColumnVisible('operation')"
            label="操作"
            align="center"
            prop="operation"
            :width="getSubjectColumnWidthString('operation', 130)"
          >
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
                v-hasPermi="['mes:dv-subject:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['mes:dv-subject:delete']"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <SubjectForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { DvSubjectApi, DvSubjectVO } from '@/api/mes/dv/subject'
import SubjectForm from './SubjectForm.vue'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'MesDvSubject' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const SUBJECT_TABLE_KEY = 'mes.dv.subject.main'

const subjectDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '项目编码', width: 120 },
  { key: 'name', label: '项目名称', minWidth: 150 },
  { key: 'type', label: '项目类型', width: 100 },
  { key: 'content', label: '项目内容', minWidth: 200 },
  { key: 'standard', label: '标准', minWidth: 200 },
  { key: 'status', label: '状态', width: 80 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'operation', label: '操作', width: 130, hideable: false, business: false }
]

const {
  columns: subjectColumns,
  saving: subjectColumnSaving,
  isColumnVisible: isSubjectColumnVisible,
  getColumnWidthString: getSubjectColumnWidthString,
  getColumnMinWidthString: getSubjectColumnMinWidthString,
  handleHeaderDragend: handleSubjectHeaderDragend,
  saveConfig: saveSubjectColumnConfig,
  resetConfig: resetSubjectColumnConfig
} = useUserTableColumns(SUBJECT_TABLE_KEY, subjectDefaultColumns)

const loading = ref(true) // 列表的加载中
const list = ref<DvSubjectVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined as string | undefined,
  name: undefined as string | undefined,
  type: undefined as number | string | undefined,
  status: undefined as number | string | undefined
})
const exportLoading = ref(false) // 导出的加载中

const subjectQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'code',
    label: '项目编码',
    type: 'text',
    queryParamKey: 'code',
    placeholder: '请输入项目编码'
  },
  {
    key: 'name',
    label: '项目名称',
    type: 'text',
    queryParamKey: 'name',
    placeholder: '请输入项目名称'
  },
  {
    key: 'type',
    label: '项目类型',
    type: 'select',
    queryParamKey: 'type',
    options: getIntDictOptions(DICT_TYPE.MES_DV_SUBJECT_TYPE).map((dict) => ({
      label: dict.label,
      value: dict.value as string | number | boolean
    }))
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: getIntDictOptions(DICT_TYPE.COMMON_STATUS).map((dict) => ({
      label: dict.label,
      value: dict.value as string | number | boolean
    }))
  }
])

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await DvSubjectApi.getSubjectPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const subjectQuickFilter = useTableQuickFilter(
  SUBJECT_TABLE_KEY,
  subjectQuickFilterDefinitions,
  queryParams,
  getList
)

const handleQuickFilterQuery = async () => {
  queryParams.pageNo = 1
  await subjectQuickFilter.applyQuickFilter()
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await DvSubjectApi.deleteSubject(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await DvSubjectApi.exportSubject(queryParams)
    download.excel(data, '点检保养项目.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>

<style scoped>
.subject-standard-list-table {
  width: 100%;
}
</style>
