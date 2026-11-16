<template>
  <ContentWrap>
    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" class="mb-12px" />
    <UnifiedListTemplate
      @query="handleQuery"
      @reset="resetQuery"
      table-key="system.operateLog.main"
      :query-model="queryParams"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
      server-filter
      :server-filter-query="getList"



      :columns="listFilterColumns0"
      :show-column-settings="false"
    >
      <template #filters>
        <el-form
          class="-mb-15px"
          :model="queryParams"
          ref="queryFormRef"
          :inline="true"
          label-width="68px"
        >
          <el-form-item label="操作人" prop="userId">
            <UserSelectV2
              allow-inactive
              v-model="queryParams.userId"
              clearable
              placeholder="请输入操作人员"
              class="!w-240px"
              :user-options="userList"
            />
          </el-form-item>
          <el-form-item label="操作模块" prop="type">
            <el-input
              v-model="queryParams.type"
              placeholder="请输入操作模块"
              clearable
              @keyup.enter="handleQuery"
              class="!w-240px"
            />
          </el-form-item>
          <el-form-item label="操作名" prop="subType">
            <el-input
              v-model="queryParams.subType"
              placeholder="请输入操作名"
              clearable
              @keyup.enter="handleQuery"
              class="!w-240px"
            />
          </el-form-item>
          <el-form-item label="操作内容" prop="action">
            <el-input
              v-model="queryParams.action"
              placeholder="请输入操作内容"
              clearable
              @keyup.enter="handleQuery"
              class="!w-240px"
            />
          </el-form-item>
          <el-form-item label="操作时间" prop="createTime">
            <el-date-picker
              v-model="queryParams.createTime"
              value-format="YYYY-MM-DD HH:mm:ss"
              type="daterange"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
              class="!w-240px"
            />
          </el-form-item>
          <el-form-item label="业务编号" prop="bizId">
            <el-input
              v-model="queryParams.bizId"
              placeholder="请输入业务编号"
              clearable
              @keyup.enter="handleQuery"
              class="!w-240px"
            />
          </el-form-item>
        </el-form>
      </template>
      <template #actions>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['system:operate-log:export']"
        >
          <Icon icon="ep:download" class="mr-5px" />
          导出
        </el-button>
      </template>
      <template #table="{ resolveRows }">
        <el-table stripe border v-loading="loading" :data="resolveRows(list)">
          <el-table-column label="日志编号" align="center" prop="id" width="100" />
          <el-table-column label="操作人" align="center" prop="userName" width="120" />
          <el-table-column label="操作模块" align="center" prop="type" width="120" />
          <el-table-column label="操作名" align="center" prop="subType" width="160" />
          <el-table-column label="操作内容" align="center" prop="action" />
          <el-table-column
            label="操作时间"
            align="center"
            prop="createTime"
            width="180"
            :formatter="dateFormatter"
          />
          <el-table-column label="业务编号" align="center" prop="bizId" width="120" />
          <el-table-column label="操作 IP" align="center" prop="userIp" width="120" />
          <el-table-column label="操作" align="center" fixed="right" width="60">
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="openDetail(scope.row)"
                v-hasPermi="['system:operate-log:query']"
              >
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <!-- 分页 -->
      </template>
    </UnifiedListTemplate>
</ContentWrap>

  <!-- 表单弹窗：详情 -->
  <OperateLogDetail ref="detailRef" />
</template>
<script lang="ts" setup>
import { useLatestListRequest } from '@/hooks/web/useServerListFilter'


import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import * as OperateLogApi from '@/api/system/operatelog'
import OperateLogDetail from './OperateLogDetail.vue'
import * as UserApi from '@/api/system/user'
const userList = ref<UserApi.UserVO[]>([]) // 用户列表

defineOptions({ name: 'SystemOperateLogList' })

const message = useMessage() // 消息弹窗

const loading = ref(true) // 列表的加载中
const loadError = ref('')
const total = ref(0) // 列表的总页数
const list = ref<OperateLogApi.OperateLogVO[]>([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  userId: undefined,
  type: undefined,
  subType: undefined,
  action: undefined,
  createTime: [],
  bizId: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中

/** 查询列表 */
const listPageRequest = useLatestListRequest()
watch(listPageRequest.loading, value => { loading.value = value }, { flush: 'sync' })
const getList = async () => {
  loadError.value = ''
  try {
    return await listPageRequest.run(() => OperateLogApi.getOperateLogPage({ ...queryParams }), data => {
    list.value = data.list
    total.value = data.total
  })
  }
  catch (error) { loadError.value = error instanceof Error ? error.message : '操作日志查询失败'; throw error }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  return getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  return handleQuery()
}

/** 详情操作 */
const detailRef = ref()
const openDetail = (data: OperateLogApi.OperateLogVO) => {
  detailRef.value.open(data)
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await OperateLogApi.exportOperateLog(queryParams)
    download.excel(data, '操作日志.xls')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') message.error(error instanceof Error ? error.message : '操作日志导出失败')
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(async () => {
  await getList()
  // 获得用户列表
  userList.value = await UserApi.getSimpleUserList()
})


const listFilterColumns0 = [
  {
    key: 'id',
    label: '日志编号',
    visible: true
  },
  {
    key: 'userName',
    label: '操作人',
    visible: true
  },
  {
    key: 'type',
    label: '操作模块',
    visible: true
  },
  {
    key: 'subType',
    label: '操作名',
    visible: true
  },
  {
    key: 'action',
    label: '操作内容',
    visible: true
  },
  {
    key: 'createTime',
    label: '操作时间',
    visible: true
  },
  {
    key: 'bizId',
    label: '业务编号',
    visible: true
  },
  {
    key: 'userIp',
    label: '操作 IP',
    visible: true
  }
]
</script>
