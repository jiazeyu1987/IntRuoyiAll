<template>
  <ContentWrap>
    <UnifiedListTemplate
      single-line-toolbar
      table-key="mes.md.workshop.main"
      :query-model="queryParams"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
      server-filter
      :server-filter-query="getList"
      :columns="listFilterColumns0"
      :show-column-settings="false"
      :column-filter-definitions="[...listColumnFilterDefinitions0, ...allColumnDisplayFilters0]"
    >
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
import { type ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'
import { getDictOptions as listColumnDictOptions } from '@/utils/dict'

import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { MdWorkshopApi, MdWorkshopVO } from '@/api/mes/md/workstation/workshop'
import WorkshopForm from './WorkshopForm.vue'
import { DICT_TYPE } from '@/utils/dict'

defineOptions({ name: 'MesMdWorkshop' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const list = ref<MdWorkshopVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  listFilters: undefined as string | undefined,
  pageNo: 1,
  pageSize: 10
})

/** 查询列表 */
let getListRequestSequence = 0
const getList = async () => {
  const requestId = ++getListRequestSequence
  loading.value = true
  try {
    const data = await MdWorkshopApi.getWorkshopPage(queryParams)
    if (requestId !== getListRequestSequence) return false
    list.value = data.list
    total.value = data.total
    return true
  } finally {
    if (requestId === getListRequestSequence)     loading.value = false
  }
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
