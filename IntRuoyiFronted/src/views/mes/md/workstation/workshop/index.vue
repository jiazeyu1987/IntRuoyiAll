<template>


  <ContentWrap>
    <UnifiedListTemplate
      @query="handleQuery"
      @reset="resetQuery"
      table-key="mes.md.workshop.main"
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
          label-width="68px"
        >
          <el-form-item label="车间编码" prop="code">
            <el-input
              v-model="queryParams.code"
              placeholder="请输入车间编码"
              clearable
              @keyup.enter="handleQuery"
              class="!w-240px"
            />
          </el-form-item>
          <el-form-item label="车间名称" prop="name">
            <el-input
              v-model="queryParams.name"
              placeholder="请输入车间名称"
              clearable
              @keyup.enter="handleQuery"
              class="!w-240px"
            />
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
          v-hasPermi="['mes:md-workshop:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增
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
          <el-table-column label="车间编码" align="center" prop="code">
            <template #default="scope">
              <el-link type="primary" @click="openForm('detail', scope.row.id)">
                {{ scope.row.code }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column label="车间名称" align="center" prop="name" width="150" />
          <el-table-column label="面积" align="center" prop="area" />
          <el-table-column label="负责人" align="center" prop="chargeUserName" />
          <el-table-column label="状态" align="center" prop="status">
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column label="备注" align="center" prop="remark" />
          <el-table-column label="操作" align="center" width="150">
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
                v-hasPermi="['mes:md-workshop:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['mes:md-workshop:delete']"
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

  <!-- 表单弹窗：添加/修改 -->
  <WorkshopForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { loadAllListPages, type ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'
import { getDictOptions as listColumnDictOptions } from '@/utils/dict'

import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { isSearchFormInputEmpty } from '@/utils/search'
import { MdWorkshopApi, MdWorkshopVO } from '@/api/mes/md/workstation/workshop'
import WorkshopForm from './WorkshopForm.vue'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'

defineOptions({ name: 'MesMdWorkshop' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const list = ref<MdWorkshopVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined,
  name: undefined,
  status: undefined
})
const queryFormRef = ref() // 搜索的表单

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await MdWorkshopApi.getWorkshopPage(queryParams)
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
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await MdWorkshopApi.deleteWorkshop(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 初始化 **/
onMounted(() => {
  getList()
})

const loadColumnFilterRows0 = async () => {
  const params = { ...queryParams }
  const rows = await loadAllListPages<any>((pagination) =>
    MdWorkshopApi.getWorkshopPage({ ...params, ...pagination })
  )
  return rows
}

const listColumnFilterDefinitions0 = computed<ListColumnFilterDefinition[]>(() => [
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
    label: '车间编码',
    visible: true
  },
  {
    key: 'name',
    label: '车间名称',
    visible: true
  },
  {
    key: 'area',
    label: '面积',
    visible: true
  },
  {
    key: 'chargeUserName',
    label: '负责人',
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
  }
]

const allColumnDisplayFilters0: ListColumnFilterDefinition[] = [
  { key: 'code', label: '车间编码', type: 'text', getValue: (row) => [row.code].join(' ') }
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
