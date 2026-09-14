<template>
  <doc-alert title="【仓库】调拨单、装箱管理" url="https://doc.iocoder.cn/mes/wm/transfer/" />

  <ContentWrap>
    <el-form
      ref="queryFormRef"
      :model="queryParams"
      :inline="true"
      label-width="100px"
      class="-mb-15px"
    >
      <el-form-item label="转移单编号" prop="code">
        <el-input
          v-model="queryParams.code"
          placeholder="请输入转移单编号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="转移单名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入转移单名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="转移单类型" prop="type">
        <el-select
          v-model="queryParams.type"
          placeholder="请选择转移单类型"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.MES_WM_TRANSFER_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="单据状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择单据状态"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.MES_WM_TRANSFER_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['mes:wm-transfer:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="转移单编号" align="center" prop="code" min-width="160">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('detail', scope.row.id)">
            {{ scope.row.code }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="转移单名称" align="center" prop="name" min-width="160" />
      <el-table-column label="转移单类型" align="center" prop="type" min-width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.MES_WM_TRANSFER_TYPE" :value="scope.row.type" />
        </template>
      </el-table-column>
      <el-table-column label="是否配送" align="center" prop="deliveryFlag" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.deliveryFlag ? 'success' : 'info'">
            {{ scope.row.deliveryFlag ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="转移日期"
        align="center"
        prop="transferDate"
        :formatter="dateFormatter2"
        width="180"
      />
      <el-table-column label="单据状态" align="center" prop="status" min-width="110">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.MES_WM_TRANSFER_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('detail', scope.row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <TransferForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import { dateFormatter2 } from '@/utils/formatTime'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import download from '@/utils/download'
import { WmTransferApi, WmTransferVO } from '@/api/mes/wm/transfer'
import TransferForm from './TransferForm.vue'

defineOptions({ name: 'MesWmTransfer' })

const message = useMessage() // 消息弹窗

const loading = ref(true) // 列表的加载中
const list = ref<WmTransferVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const exportLoading = ref(false) // 导出的加载中
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined,
  name: undefined,
  type: undefined,
  status: undefined
})
const queryFormRef = ref() // 搜索的表单
const formRef = ref() // 表单弹窗

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await WmTransferApi.getTransferPage(queryParams)
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
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await WmTransferApi.exportTransfer(queryParams)
    download.excel(data, '转移单.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>
