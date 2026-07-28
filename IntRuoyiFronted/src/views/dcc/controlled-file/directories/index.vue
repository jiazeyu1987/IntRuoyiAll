<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :inline="true"
      :model="queryParams"
      label-width="82px"
    >
      <el-form-item label="目录名称" prop="name">
        <el-input
          v-model="queryParams.name"
          class="!w-240px"
          clearable
          placeholder="请输入目录名称"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="启用状态" prop="active">
        <el-select
          v-model="queryParams.active"
          class="!w-180px"
          clearable
          placeholder="请选择启用状态"
        >
          <el-option
            v-for="item in ACTIVE_STATUS_OPTIONS"
            :key="String(item.value)"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" />
          查询
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-5px" />
          重置
        </el-button>
        <el-button
          v-hasPermi="['dcc:controlled-file:directory:manage']"
          plain
          type="primary"
          @click="openForm('create')"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新建目录
        </el-button>
        <el-button plain type="info" @click="getList">
          <Icon icon="ep:refresh-right" class="mr-5px" />
          刷新目录树
        </el-button>
        <TreeExpandActions @expand="expandAll" @collapse="collapseAll" />
        <el-button
          v-hasPermi="['dcc:controlled-file:directory:manage']"
          plain
          type="success"
          :disabled="directories.length > 0"
          :loading="importLoading"
          @click="handleImportFromIntAuth"
        >
          <Icon icon="ep:download" class="mr-5px" />
          NAS同步
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      v-if="loadErrorMessage"
      :closable="false"
      show-icon
      title="受控文件目录加载失败"
      type="error"
      class="mb-12px"
      data-testid="dcc-controlled-file-directory-load-error"
    >
      <template #default>
        {{ loadErrorMessage }}
      </template>
    </el-alert>

    <el-table
      v-loading="loading"
      :data="filteredDirectories"
      data-user-table-column-explicit
      row-key="id"
      :default-expand-all="isExpandAll"
      v-if="refreshTable"
      lazy
      :load="loadDirectoryChildren"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column label="目录名称" min-width="280" prop="name" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="directory-name-cell">
            <span>{{ row.name }}</span>
            <el-tag
              v-if="resolveDirectoryChildLoadError(row)"
              type="danger"
              size="small"
              :title="resolveDirectoryChildLoadError(row)"
            >
              子目录加载失败
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column align="center" fixed="right" label="操作" width="320">
        <template #default="{ row }">
          <el-button
            v-hasPermi="['dcc:controlled-file:access-rule:manage']"
            link
            type="primary"
            @click="openAccessRules(row)"
          >
            访问规则
          </el-button>
          <el-button
            v-hasPermi="['dcc:controlled-file:directory:manage']"
            link
            type="primary"
            @click="openForm('create', row)"
          >
            新建
          </el-button>
          <el-button
            v-hasPermi="['dcc:controlled-file:directory:manage']"
            link
            type="primary"
            @click="openForm('update', row)"
          >
            编辑
          </el-button>
          <el-button
            v-hasPermi="['dcc:controlled-file:directory:manage']"
            link
            type="danger"
            :loading="deleteLoadingId === row.id || deletePrecheckLoadingId === row.id"
            @click="handleDeleteParentFolder(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <DirectoryForm v-if="directoryFormMounted" ref="formRef" @success="getList" />

  <el-dialog
    v-model="deleteDialogVisible"
    destroy-on-close
    title="删除父文件夹"
    width="480px"
  >
    <el-alert
      :closable="false"
      show-icon
      type="error"
      class="mb-16px"
      title="此操作会删除该目录、全部子目录、目录内所有状态的受控文件和底层上传文件。"
    />
    <el-form label-position="top">
      <el-form-item label="目录名称">
        <el-input :model-value="deleteTargetDirectory?.name || '-'" disabled />
      </el-form-item>
      <el-form-item label="确认文本">
        <el-input
          v-model="deleteConfirmText"
          autocomplete="off"
          placeholder="请输入 PROD"
          @keyup.enter="submitDeleteParentFolder"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="deleteDialogVisible = false">取消</el-button>
      <el-button
        type="danger"
        :loading="deleteSubmitting"
        :disabled="deleteConfirmText.trim() !== 'PROD'"
        @click="submitDeleteParentFolder"
      >
        确认删除
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { isSearchFormInputEmpty } from '@/utils/search'
import {
  deleteDirectorySubtree,
  getDirectoryActiveNasTransfer,
  getDirectoryChildren,
  importDirectoriesFromIntAuth,
  searchDirectories,
  stopDirectoryActiveNasTransfer,
  type ControlledFileDirectoryVO
} from '@/api/dcc/controlledFile/directories'
import { ACTIVE_STATUS_OPTIONS } from '../shared/options'
import { resolveControlledFileReadErrorMessage } from '../shared/utils'
import TreeExpandActions from '@/components/TreeExpandActions/index.vue'
import { useTreeTableExpand } from '@/utils/treeExpand'

defineOptions({ name: 'DccControlledFileDirectories' })

const DirectoryForm = defineAsyncComponent(() => import('./components/DirectoryForm.vue'))

const message = useMessage()
const router = useRouter()
const loading = ref(false)
const importLoading = ref(false)
const directories = ref<ControlledFileDirectoryVO[]>([])
const queryFormRef = ref()
const formRef = ref()
const loadErrorMessage = ref('')
const childLoadErrorMessages = reactive<Record<string, string>>({})
const directoryFormMounted = ref(false)
const deleteDialogVisible = ref(false)
const deleteConfirmText = ref('')
const deleteTargetDirectory = ref<ControlledFileDirectoryVO>()
const deleteSubmitting = ref(false)
const deleteLoadingId = ref<number>()
const deletePrecheckLoadingId = ref<number>()
const { isExpandAll, refreshTable, expandAll, collapseAll } = useTreeTableExpand(false)

const queryParams = reactive<{
  name: string
  active?: boolean
}>({
  name: '',
  active: undefined
})
const appliedQueryParams = reactive<{
  name: string
  active?: boolean
}>({
  name: '',
  active: undefined
})

const filteredDirectories = computed(() => {
  if (!appliedQueryParams.name && appliedQueryParams.active === undefined) {
    return directories.value
  }
  return filterTree(directories.value)
})

const clearDirectoryChildLoadErrors = () => {
  for (const key of Object.keys(childLoadErrorMessages)) {
    delete childLoadErrorMessages[key]
  }
}

const resolveDirectoryChildLoadError = (row: ControlledFileDirectoryVO) =>
  row.id ? childLoadErrorMessages[String(row.id)] || '' : ''

const getList = async () => {
  loading.value = true
  try {
    directories.value = await getDirectoryChildren()
    clearDirectoryChildLoadErrors()
    loadErrorMessage.value = ''
  } catch (error) {
    directories.value = []
    loadErrorMessage.value = resolveControlledFileReadErrorMessage(
      error,
      '目录树加载失败，请确认 `/admin-api/dcc/directories/children` 接口已正常发布后重试。'
    )
  } finally {
    loading.value = false
  }
}

const loadDirectoryChildren = async (
  row: ControlledFileDirectoryVO,
  _treeNode: unknown,
  resolve: (children: ControlledFileDirectoryVO[]) => void
) => {
  if (!row.id) {
    resolve([])
    return
  }
  try {
    const children = await getDirectoryChildren(row.id)
    row.children = children
    resolve(children)
    delete childLoadErrorMessages[String(row.id)]
  } catch (error) {
    resolve([])
    childLoadErrorMessages[String(row.id)] = resolveControlledFileReadErrorMessage(
      error,
      '子目录加载失败，请确认 `/admin-api/dcc/directories/children` 接口已正常发布后重试。'
    )
  }
}

const handleImportFromIntAuth = async () => {
  importLoading.value = true
  try {
    const result = await importDirectoriesFromIntAuth()
    message.success(`已导入 ${result.importedCount} 个目录节点，根目录 ${result.rootCount} 个`)
    await getList()
  } finally {
    importLoading.value = false
  }
}

const filterTree = (nodes: ControlledFileDirectoryVO[]): ControlledFileDirectoryVO[] => {
  const nameKeyword = appliedQueryParams.name.toLowerCase()
  const activeValue = appliedQueryParams.active
  return nodes
    .map((item) => {
      const children = item.children ? filterTree(item.children) : undefined
      return children ? { ...item, children } : { ...item, children: undefined }
    })
    .filter((item) => {
      const nameMatch = !nameKeyword || item.name.toLowerCase().includes(nameKeyword)
      const activeMatch = activeValue === undefined || item.active === activeValue
      return (nameMatch && activeMatch) || Boolean(item.children?.length)
    })
}

const handleQuery = async (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    await resetQuery()
    return
  }
  appliedQueryParams.name = queryParams.name.trim()
  appliedQueryParams.active = queryParams.active
  loading.value = true
  try {
    directories.value = appliedQueryParams.name
      ? await searchDirectories(appliedQueryParams.name, 100)
      : await getDirectoryChildren()
    clearDirectoryChildLoadErrors()
    loadErrorMessage.value = ''
  } catch (error) {
    directories.value = []
    loadErrorMessage.value = resolveControlledFileReadErrorMessage(
      error,
      '目录查询失败，请确认 `/admin-api/dcc/directories/search` 或 `/admin-api/dcc/directories/children` 接口已正常发布后重试。'
    )
  } finally {
    loading.value = false
  }
}

const resetQuery = async () => {
  queryFormRef.value?.resetFields()
  await handleQuery(true)
}

const openForm = async (type: 'create' | 'update', row?: ControlledFileDirectoryVO) => {
  directoryFormMounted.value = true
  await import('./components/DirectoryForm.vue')
  await nextTick()
  if (!formRef.value?.open) {
    throw new Error('文档目录表单组件加载失败')
  }
  formRef.value.open(type, {
    id: row?.id,
    parentId: type === 'create' ? row?.id ?? undefined : undefined,
    directories: row ? [row] : directories.value
  })
}

const openAccessRules = (row: ControlledFileDirectoryVO) => {
  router.push({
    path: '/dcc/controlled-file/categories',
    query: {
      tab: 'directory-auth',
      directoryId: row.id
    }
  })
}

const handleDeleteParentFolder = async (row: ControlledFileDirectoryVO) => {
  if (!row.id) {
    return
  }
  deletePrecheckLoadingId.value = row.id
  try {
    const activeTransfer = await getDirectoryActiveNasTransfer(row.id)
    if (activeTransfer.active) {
      const shouldContinue = await confirmStopActiveNasTransfer(activeTransfer.taskId)
      if (!shouldContinue) {
        return
      }
      const stoppedTransfer = await stopDirectoryActiveNasTransfer(row.id)
      if (stoppedTransfer.active) {
        message.warning('后台收集正在停止，请稍后重新删除父文件夹。')
        return
      }
      message.success('已停止后台收集，请继续确认删除父文件夹。')
    }
    deleteTargetDirectory.value = row
    deleteConfirmText.value = ''
    deleteDialogVisible.value = true
  } catch (error) {
    message.error(
      resolveControlledFileReadErrorMessage(error, '删除父文件夹前置检查失败，请查看错误提示后重试。')
    )
  } finally {
    deletePrecheckLoadingId.value = undefined
  }
}

const confirmStopActiveNasTransfer = async (taskId?: number): Promise<boolean> => {
  try {
    const taskText = taskId ? `任务 ${taskId}` : '当前任务'
    await message.confirm(
      `该目录下仍有后台收集正在运行（${taskText}）。确认停止后台收集后再继续删除父文件夹？`,
      '确认停止后台收集'
    )
    return true
  } catch {
    return false
  }
}

const submitDeleteParentFolder = async () => {
  const target = deleteTargetDirectory.value
  if (!target?.id || deleteConfirmText.value.trim() !== 'PROD') {
    return
  }
  deleteSubmitting.value = true
  deleteLoadingId.value = target.id
  try {
    const result = await deleteDirectorySubtree(target.id, {
      confirmText: deleteConfirmText.value.trim()
    })
    message.success(
      `已删除 ${result.directoryCount} 个目录、${result.controlledFileCount} 个受控文件、${result.infraFileCount} 个底层文件`
    )
    deleteDialogVisible.value = false
    deleteTargetDirectory.value = undefined
    deleteConfirmText.value = ''
    await getList()
  } catch (error) {
    message.error(resolveControlledFileReadErrorMessage(error, '删除父文件夹失败，请查看错误提示后重试。'))
  } finally {
    deleteSubmitting.value = false
    deleteLoadingId.value = undefined
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.directory-name-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.directory-summary {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.directory-summary__line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.directory-summary__meta {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}
</style>
