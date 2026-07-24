<template>
  <div class="keyword-workbench">
    <div class="keyword-workbench__toolbar">
      <div class="keyword-workbench__filters">
        <el-input
          v-model="filters.keyword"
          class="keyword-workbench__search"
          clearable
          placeholder="请输入中文关键词或 English Keyword"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-button type="primary" @click="openCreate">新增关键词</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" row-key="id" class="keyword-workbench__table">
      <el-table-column label="中文关键词" min-width="260" prop="nameZh" />
      <el-table-column label="English Keyword" min-width="320" prop="nameEn" />
      <el-table-column label="更新时间" min-width="180">
        <template #default="{ row }">
          {{ formatDate(row.updateTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row.id)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      :page="pagination.pageNo"
      :limit="pagination.pageSize"
      :total="pagination.total"
      @pagination="handlePageChange"
    />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form label-width="120px">
        <el-form-item label="中文关键词" required>
          <el-input v-model="form.nameZh" maxlength="255" placeholder="请输入中文关键词" />
        </el-form-item>
        <el-form-item label="English Keyword" required>
          <el-input v-model="form.nameEn" maxlength="255" placeholder="请输入 English Keyword" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { formatDate } from '@/utils/formatTime'
import {
  ShowroomAdminApi,
  type ShowroomKeywordPageReqVO,
  type ShowroomKeywordPageRowRespVO,
  type ShowroomKeywordSaveReqVO
} from '@/api/showroom-admin'

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('关键词中英对照')
const rows = ref<ShowroomKeywordPageRowRespVO[]>([])
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})
const filters = reactive<ShowroomKeywordPageReqVO>({
  keyword: ''
})
const form = reactive<ShowroomKeywordSaveReqVO>({
  id: undefined,
  nameZh: '',
  nameEn: ''
})

const loadRows = async () => {
  loading.value = true
  try {
    const page = await ShowroomAdminApi.getKeywordPage({
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      keyword: filters.keyword?.trim() || undefined
    })
    rows.value = page.list
    pagination.total = page.total
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error('关键词列表加载失败')
    message.error(resolved.message)
    throw resolved
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  pagination.pageNo = 1
  await loadRows()
}

const handleReset = async () => {
  filters.keyword = ''
  pagination.pageNo = 1
  await loadRows()
}

const handlePageChange = async ({ page, limit }: { page: number; limit: number }) => {
  pagination.pageNo = page
  pagination.pageSize = limit
  await loadRows()
}

const resetForm = () => {
  form.id = undefined
  form.nameZh = ''
  form.nameEn = ''
}

const openCreate = () => {
  resetForm()
  dialogTitle.value = '新增关键词'
  dialogVisible.value = true
}

const openEdit = async (id: number) => {
  loading.value = true
  try {
    const detail = await ShowroomAdminApi.getKeyword(id)
    form.id = detail.id
    form.nameZh = detail.nameZh
    form.nameEn = detail.nameEn
    dialogTitle.value = '编辑关键词'
    dialogVisible.value = true
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error('关键词详情加载失败')
    message.error(resolved.message)
    throw resolved
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  const payload: ShowroomKeywordSaveReqVO = {
    id: form.id,
    nameZh: form.nameZh.trim(),
    nameEn: form.nameEn.trim()
  }
  if (!payload.nameZh || !payload.nameEn) {
    const resolved = new Error('中文关键词和 English Keyword 均为必填项')
    message.error(resolved.message)
    throw resolved
  }
  saving.value = true
  try {
    if (payload.id) {
      await ShowroomAdminApi.updateKeyword(payload)
      message.success('关键词更新成功')
    } else {
      await ShowroomAdminApi.createKeyword(payload)
      message.success('关键词创建成功')
    }
    dialogVisible.value = false
    await loadRows()
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error('关键词保存失败')
    message.error(resolved.message)
    throw resolved
  } finally {
    saving.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await message.confirm('确认删除当前关键词吗？')
  } catch {
    return
  }
  try {
    await ShowroomAdminApi.deleteKeyword(id)
    message.success('关键词删除成功')
    await loadRows()
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error('关键词删除失败')
    message.error(resolved.message)
    throw resolved
  }
}

onMounted(async () => {
  await loadRows()
})
</script>

<style scoped>
.keyword-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.keyword-workbench__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.keyword-workbench__filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.keyword-workbench__search {
  width: 320px;
}

.keyword-workbench__table :deep(.el-table__header th) {
  background: #f7f9fc;
}

@media (max-width: 768px) {
  .keyword-workbench__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .keyword-workbench__filters {
    width: 100%;
  }

  .keyword-workbench__search {
    width: 100%;
  }
}
</style>
