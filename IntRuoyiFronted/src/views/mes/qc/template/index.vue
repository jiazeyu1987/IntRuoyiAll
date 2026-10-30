<!-- MES 质检方案列表 -->
<template>


  <ContentWrap>
    <UnifiedListTemplate
      @query="handleQuery"
      @reset="resetQuery"
      table-key="mes.qc.template.main"
      :query-model="queryParams"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
      :load-filter-rows="loadColumnFilterRows0"
      @filtered-rows="list = $event"
      :list-rows="list"
      @column-filter-reset="getList"
      :columns="listFilterColumns0"
      :show-column-settings="false"
      :column-filter-definitions="[...listColumnFilterDefinitions0, ...allColumnDisplayFilters0]"
    >
      <template #filters>
        <el-form
          class="-mb-15px edhr-single-line-query"
          :model="queryParams"
          ref="queryFormRef"
          :inline="true"
          label-width="100px"
        >
          <el-form-item label="方案编号" prop="code">
            <el-input
              v-model="queryParams.code"
              placeholder="请输入方案编号"
              clearable
              @keyup.enter="handleQuery"
              class="!w-240px"
            />
          </el-form-item>
          <el-form-item label="方案名称" prop="name">
            <el-input
              v-model="queryParams.name"
              placeholder="请输入方案名称"
              clearable
              @keyup.enter="handleQuery"
              class="!w-240px"
            />
          </el-form-item>
          <el-form-item label="检测种类" prop="type">
            <el-select
              v-model="queryParams.type"
              placeholder="请选择检测种类"
              clearable
              class="!w-240px"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.MES_QC_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select
              v-model="queryParams.status"
              placeholder="请选择状态"
              clearable
              class="!w-240px"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </template>
      <template #actions>


        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['mes:qc-template:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['mes:qc-template:export']"
        >
          <Icon icon="ep:download" class="mr-5px" />
          导出
        </el-button>
      </template>
      <template #table="{ resolveRows }">
        <el-table
          border
          v-loading="loading"
          :data="resolveRows(list)"
          :stripe="true"
          :show-overflow-tooltip="true"
        >
          <el-table-column label="方案编号" align="center" prop="code" width="150">
            <template #default="scope">
              <el-link type="primary" @click="openForm('detail', scope.row.id)">
                {{ scope.row.code }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column label="方案名称" align="center" prop="name" min-width="200" />
          <el-table-column label="检测种类" align="center" prop="types" min-width="200">
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.MES_QC_TYPE" :value="scope.row.types" />
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" prop="status" width="100">
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column label="备注" align="center" prop="remark" min-width="150" />
          <el-table-column
            label="创建时间"
            align="center"
            prop="createTime"
            :formatter="dateFormatter"
            width="180px"
          />
          <el-table-column label="操作" align="center" width="130" fixed="right">
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
                v-hasPermi="['mes:qc-template:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['mes:qc-template:delete']"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <!-- 分页 -->
      </template>
    </UnifiedListTemplate>
</ContentWrap>

  <!-- 表单弹窗：添加/修改（编辑时含检测指标项和产品关联子表） -->
  <TemplateForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { loadAllListPages, type ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'
import { getDictOptions as listColumnDictOptions } from '@/utils/dict'

import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { isSearchFormInputEmpty } from '@/utils/search'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { QcTemplateApi, QcTemplateVO } from '@/api/mes/qc/template'
import TemplateForm from './TemplateForm.vue'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'

defineOptions({ name: 'MesQcTemplate' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const list = ref<QcTemplateVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined,
  name: undefined,
  type: undefined,
  status: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await QcTemplateApi.getTemplatePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    resetQuery()
    return
  }
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery(true)
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
    await QcTemplateApi.deleteTemplate(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await QcTemplateApi.exportTemplate(queryParams)
    download.excel(data, '质检方案.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})

const loadColumnFilterRows0 = async () => {
  const params = { ...queryParams }
  const rows = await loadAllListPages<any>((pagination) =>
    QcTemplateApi.getTemplatePage({ ...params, ...pagination })
  )
  return rows
}

const listColumnFilterDefinitions0 = computed<ListColumnFilterDefinition[]>(() => [
  {
    key: 'types',
    label: '检测种类',
    type: 'select',
    options: listColumnDictOptions(DICT_TYPE.MES_QC_TYPE)
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: listColumnDictOptions(DICT_TYPE.COMMON_STATUS)
  }
])

const listFilterColumns0 = [
  {
    key: 'code',
    label: '方案编号',
    visible: true
  },
  {
    key: 'name',
    label: '方案名称',
    visible: true
  },
  {
    key: 'types',
    label: '检测种类',
    visible: true
  },
  {
    key: 'status',
    label: '状态',
    visible: true
  },
  {
    key: 'remark',
    label: '备注',
    visible: true
  },
  {
    key: 'createTime',
    label: '创建时间',
    visible: true
  }
]

const allColumnDisplayFilters0: ListColumnFilterDefinition[] = [
  { key: 'code', label: '方案编号', type: 'text', getValue: (row) => [row.code].join(' ') }
]
</script>

<style scoped>
@media (min-width: 1181px) {
  .edhr-single-line-query {
    display: flex;
    flex-wrap: nowrap;
    gap: 12px;
    align-items: center;
  }
  .edhr-single-line-query > :deep(.el-form-item) {
    flex: 1 1 0;
    min-width: 0;
    margin-right: 0;
  }
  .edhr-single-line-query > :deep(.el-form-item:last-child) {
    flex: 0 0 auto;
  }
  .edhr-single-line-query :deep(.el-form-item__content) {
    min-width: 0;
    flex-wrap: nowrap;
  }
  .edhr-single-line-query :deep(.el-input),
  .edhr-single-line-query :deep(.el-select),
  .edhr-single-line-query :deep(.el-date-editor) {
    width: 100% !important;
    min-width: 0;
  }
}
</style>
