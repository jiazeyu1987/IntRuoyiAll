<template>
  <doc-alert title="工作流手册" url="https://doc.iocoder.cn/bpm/" />

  <ContentWrap>
    <UnifiedListTemplate
      table-key="bpm.category.main"
      :query-model="queryParams"
      label-width="88px"
      :filter-definitions="categoryQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="categoryQuickFilter.state"
      :selected-filter-definition="categoryQuickFilter.selectedDefinition.value"
      :operator-options="categoryQuickFilter.operatorOptions.value"
      :columns="categoryColumns"
      :column-saving="categoryColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="categoryQuickFilter.updateState"
      @quick-filter-query="handleQuery"
      @column-change="saveCategoryColumnConfig"
      @column-reset="resetCategoryColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['bpm:category:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          :data="list"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          data-user-table-column-explicit
          data-user-table-key="bpm.category.main"
          @header-dragend="handleCategoryHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isCategoryColumnVisible('id')"
            label="分类编号"
            align="center"
            prop="id"
            :width="getCategoryColumnWidthString('id', 110)"
            v-bind="sortColumnAttrs('id')"
          />
          <el-table-column
            v-if="isCategoryColumnVisible('name')"
            label="分类名"
            align="center"
            prop="name"
            :width="getCategoryColumnWidthString('name')"
            :min-width="getCategoryColumnMinWidthString('name', 160)"
            v-bind="sortColumnAttrs('name')"
          />
          <el-table-column
            v-if="isCategoryColumnVisible('code')"
            label="分类标志"
            align="center"
            prop="code"
            :width="getCategoryColumnWidthString('code')"
            :min-width="getCategoryColumnMinWidthString('code', 160)"
            v-bind="sortColumnAttrs('code')"
          />
          <el-table-column
            v-if="isCategoryColumnVisible('description')"
            label="分类描述"
            align="center"
            prop="description"
            :width="getCategoryColumnWidthString('description')"
            :min-width="getCategoryColumnMinWidthString('description', 200)"
            v-bind="sortColumnAttrs('description')"
          />
          <el-table-column
            v-if="isCategoryColumnVisible('status')"
            label="分类状态"
            align="center"
            prop="status"
            :width="getCategoryColumnWidthString('status', 110)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCategoryColumnVisible('sort')"
            label="分类排序"
            align="center"
            prop="sort"
            :width="getCategoryColumnWidthString('sort', 110)"
            v-bind="sortColumnAttrs('sort')"
          />
          <el-table-column
            v-if="isCategoryColumnVisible('createTime')"
            label="创建时间"
            align="center"
            prop="createTime"
            :formatter="dateFormatter"
            :width="getCategoryColumnWidthString('createTime', 180)"
            v-bind="sortColumnAttrs('createTime')"
          />
          <el-table-column
            v-if="isCategoryColumnVisible('actions')"
            label="操作"
            align="center"
            prop="actions"
            fixed="right"
            :width="getCategoryColumnWidthString('actions', 140)"
          >
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
                v-hasPermi="['bpm:category:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['bpm:category:delete']"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <CategoryForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { CategoryApi, CategoryVO } from '@/api/bpm/category'
import CategoryForm from './CategoryForm.vue'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

/** BPM 流程分类 列表 */
defineOptions({ name: 'BpmCategory' })

const message = useMessage()
const { t } = useI18n()

const categoryDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'id', label: '分类编号', width: 110 },
  { key: 'name', label: '分类名', minWidth: 160 },
  { key: 'code', label: '分类标志', minWidth: 160 },
  { key: 'description', label: '分类描述', minWidth: 200 },
  { key: 'status', label: '分类状态', width: 110 },
  { key: 'sort', label: '分类排序', width: 110 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'actions', label: '操作', width: 140, hideable: false, business: false }
]

const {
  columns: categoryColumns,
  saving: categoryColumnSaving,
  isColumnVisible: isCategoryColumnVisible,
  getColumnWidthString: getCategoryColumnWidthString,
  getColumnMinWidthString: getCategoryColumnMinWidthString,
  handleHeaderDragend: handleCategoryHeaderDragend,
  saveConfig: saveCategoryColumnConfig,
  resetConfig: resetCategoryColumnConfig
} = useUserTableColumns('bpm.category.main', categoryDefaultColumns)

const loading = ref(true)
const list = ref<CategoryVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined as string | undefined,
  code: undefined as string | undefined,
  status: undefined as number | undefined,
  createTime: [] as string[]
})

const categoryQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  { key: 'name', label: '分类名', type: 'text', queryParamKey: 'name', placeholder: '请输入分类名' },
  { key: 'code', label: '分类标志', type: 'text', queryParamKey: 'code', placeholder: '请输入分类标志' },
  {
    key: 'status',
    label: '分类状态',
    type: 'select',
    queryParamKey: 'status',
    options: getIntDictOptions(DICT_TYPE.COMMON_STATUS)
  },
  { key: 'createTime', label: '创建时间', type: 'dateRange', queryParamKey: 'createTime' }
])

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await CategoryApi.getCategoryPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const categoryQuickFilter = useTableQuickFilter(
  'bpm.category.main',
  categoryQuickFilterDefinitions,
  queryParams,
  getList
)

/** 搜索按钮操作 */
const handleQuery = async () => {
  await categoryQuickFilter.applyQuickFilter()
}

/** 重置按钮操作 */
const resetQuery = async () => {
  await categoryQuickFilter.resetQuickFilter()
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
    await CategoryApi.deleteCategory(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    throw error
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
