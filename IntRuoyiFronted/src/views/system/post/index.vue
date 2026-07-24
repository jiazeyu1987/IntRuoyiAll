<template>
  <ContentWrap>
    <div class="mb-12px text-12px text-[var(--el-text-color-secondary)]">
      组织角色：控制组织岗位归属。
    </div>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="组织角色名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入组织角色名称"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="组织角色编码" prop="code">
        <el-input
          v-model="queryParams.code"
          placeholder="请输入组织角色编码"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-240px">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
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
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['system:post:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增组织角色
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['system:post:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出配置包
        </el-button>
        <el-button
          type="warning"
          plain
          @click="openImport"
          :loading="importLoading"
          v-hasPermi="['system:post:create', 'system:post:update']"
        >
          <Icon icon="ep:upload" class="mr-5px" /> 导入配置包
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="checkedIds.length === 0"
          @click="handleDeleteBatch"
          v-hasPermi="['system:post:delete']"
        >
          <Icon icon="ep:delete" class="mr-5px" /> 批量删除
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" @selection-change="handleRowCheckboxChange">
      <el-table-column type="selection" width="55" />
      <el-table-column label="组织角色编号" align="center" prop="id" />
      <el-table-column label="组织角色名称" align="center" prop="name" />
      <el-table-column label="组织角色编码" align="center" prop="code" />
      <el-table-column label="组织角色顺序" align="center" prop="sort" />
      <el-table-column label="组织角色备注" align="center" prop="remark" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['system:post:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['system:post:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <PostForm ref="formRef" @success="getList" />
  <input ref="importInputRef" accept=".json" class="hidden" type="file" @change="handleImportFileChange" />
</template>
<script lang="ts" setup>
import { isSearchFormInputEmpty } from '@/utils/search'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import * as PostApi from '@/api/system/post'
import PostForm from './PostForm.vue'

defineOptions({ name: 'SystemPost' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: '',
  name: '',
  status: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中
const importLoading = ref(false)
const importInputRef = ref<HTMLInputElement>()

/** 查询岗位列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await PostApi.getPostPage(queryParams)
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
    await PostApi.deletePost(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 批量删除按钮操作 */
const checkedIds = ref<number[]>([])
const handleRowCheckboxChange = (rows: PostApi.PostVO[]) => {
  checkedIds.value = rows.map((row) => row.id).filter((id): id is number => id !== undefined)
}

const handleDeleteBatch = async () => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起批量删除
    await PostApi.deletePostList(checkedIds.value)
    checkedIds.value = []
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await PostApi.exportPostConfigPackage()
    download.json(data, '组织角色配置包.json')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

const openImport = () => {
  importInputRef.value?.click()
}

const handleImportFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  importLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    await PostApi.importPostConfigPackage(formData)
    message.success('组织角色配置包导入成功')
    await getList()
  } finally {
    importLoading.value = false
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
