<template>
  <ContentWrap class="config-package-toolbar">
    <el-form class="-mb-15px config-package-form" :inline="true" label-width="86px">
      <el-form-item label="密码策略">
        <el-tag effect="plain" type="info">保留目标环境密码</el-tag>
      </el-form-item>
      <el-form-item>
        <el-button
          v-hasPermi="['system:config-package:export']"
          :loading="exportLoading"
          type="primary"
          @click="handleExport"
        >
          <Icon class="mr-5px" icon="ep:download" />
          导出 Excel
        </el-button>
      </el-form-item>
      <el-form-item label="导入文件">
        <el-upload
          ref="uploadRef"
          v-model:file-list="fileList"
          :auto-upload="false"
          :limit="1"
          :on-change="handleFileChange"
          :on-exceed="handleFileExceed"
          :on-remove="handleFileRemove"
          accept=".xlsx,.xls"
          class="config-package-upload"
        >
          <el-button>
            <Icon class="mr-5px" icon="ep:upload" />
            选择 Excel
          </el-button>
        </el-upload>
      </el-form-item>
      <el-form-item>
        <el-button
          v-hasPermi="['system:config-package:import']"
          :disabled="!selectedFile"
          :loading="precheckLoading"
          plain
          type="primary"
          @click="handlePrecheck"
        >
          <Icon class="mr-5px" icon="ep:finished" />
          预检
        </el-button>
        <el-button
          v-hasPermi="['system:config-package:import']"
          :disabled="!precheckResult?.valid"
          :loading="importLoading"
          plain
          type="danger"
          @click="handleImport"
        >
          <Icon class="mr-5px" icon="ep:warning" />
          覆盖导入
        </el-button>
      </el-form-item>
      <el-form-item label="组件清单">
        <el-tag effect="plain" type="info">{{ componentCount }} 个</el-tag>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-if="precheckResult">
    <div class="config-package-summary">
      <el-tag :type="precheckResult.valid ? 'success' : 'danger'" effect="plain">
        {{ precheckResult.valid ? '预检通过' : '预检未通过' }}
      </el-tag>
      <el-tag effect="plain" type="info">包摘要 {{ shortHash(precheckResult.packageSha256) }}</el-tag>
      <el-tag effect="plain" type="info">
        目标摘要 {{ shortHash(precheckResult.targetSnapshotSha256) }}
      </el-tag>
      <el-tag effect="plain" type="warning">变更 {{ totalDiffCount }} 项</el-tag>
    </div>

    <el-alert
      v-if="precheckResult.blockingErrors.length > 0"
      :closable="false"
      class="mt-12px"
      title="阻塞项"
      type="error"
    >
      <ul class="config-package-message-list">
        <li v-for="error in precheckResult.blockingErrors" :key="error">{{ error }}</li>
      </ul>
    </el-alert>

    <el-alert
      v-if="precheckResult.warnings.length > 0"
      :closable="false"
      class="mt-12px"
      title="提示"
      type="warning"
    >
      <ul class="config-package-message-list">
        <li v-for="warning in precheckResult.warnings" :key="warning">{{ warning }}</li>
      </ul>
    </el-alert>

    <el-table class="mt-12px" :data="diffRows" stripe>
      <el-table-column label="范围" min-width="140" prop="label" />
      <el-table-column align="right" label="配置包" prop="packageCount" width="110" />
      <el-table-column align="right" label="当前环境" prop="currentCount" width="110" />
      <el-table-column align="right" label="新增" prop="createCount" width="90" />
      <el-table-column align="right" label="更新" prop="updateCount" width="90" />
      <el-table-column align="right" label="删除" prop="deleteCount" width="90" />
    </el-table>
  </ContentWrap>

  <ContentWrap v-if="importResult">
    <div class="config-package-summary">
      <el-tag effect="plain" type="success">导入完成</el-tag>
      <el-tag effect="plain" type="info">
        目标摘要 {{ shortHash(importResult.targetSnapshotSha256) }}
      </el-tag>
    </div>
    <el-table class="mt-12px" :data="restoredRows" stripe>
      <el-table-column label="范围" min-width="140" prop="label" />
      <el-table-column align="right" label="还原数量" prop="count" width="140" />
    </el-table>
  </ContentWrap>
</template>

<script lang="ts" setup>
import type { UploadFile, UploadFiles, UploadInstance, UploadUserFile } from 'element-plus'
import download from '@/utils/download'
import { getFrontendComponentPaths } from '@/utils/frontendComponentManifest'
import * as ConfigPackageApi from '@/api/system/configPackage'

defineOptions({ name: 'SystemConfigPackage' })

const message = useMessage()

const sheetLabels: Record<string, string> = {
  menus: '菜单',
  roles: '角色',
  role_menu: '角色菜单',
  users: '用户',
  user_role: '用户角色',
  dept: '部门',
  post: '岗位',
  user_post: '用户岗位',
  dict_type: '字典类型',
  dict_data: '字典数据',
  tenant_package: '租户套餐'
}

const availableComponents = getFrontendComponentPaths()
const componentCount = availableComponents.length
const exportLoading = ref(false)
const precheckLoading = ref(false)
const importLoading = ref(false)
const uploadRef = ref<UploadInstance>()
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File>()
const precheckResult = ref<ConfigPackageApi.ConfigPackagePrecheckRespVO>()
const importResult = ref<ConfigPackageApi.ConfigPackageImportRespVO>()

const diffRows = computed(() => {
  return (precheckResult.value?.sheetDiffs || []).map((item) => ({
    ...item,
    label: sheetLabels[item.sheetName] || item.sheetName
  }))
})

const totalDiffCount = computed(() => {
  return diffRows.value.reduce(
    (sum, item) => sum + item.createCount + item.updateCount + item.deleteCount,
    0
  )
})

const restoredRows = computed(() => {
  return Object.entries(importResult.value?.restoredCounts || {}).map(([sheetName, count]) => ({
    sheetName,
    label: sheetLabels[sheetName] || sheetName,
    count
  }))
})

const shortHash = (hash?: string) => {
  if (!hash) {
    return '-'
  }
  return hash.length > 12 ? `${hash.slice(0, 12)}...` : hash
}

const ensureComponentManifest = () => {
  if (availableComponents.length === 0) {
    message.error('当前前端构建组件清单为空')
    return false
  }
  return true
}

const ensureSelectedFile = () => {
  if (!selectedFile.value) {
    message.error('请选择配置包 Excel')
    return false
  }
  return true
}

const handleFileChange = (file: UploadFile, files: UploadFiles) => {
  const rawFile = file.raw
  if (!rawFile) {
    selectedFile.value = undefined
    return
  }
  if (!/\.(xlsx|xls)$/i.test(rawFile.name)) {
    message.error('仅支持 xls、xlsx 格式文件')
    uploadRef.value?.clearFiles()
    selectedFile.value = undefined
    return
  }
  fileList.value = files.slice(-1)
  selectedFile.value = rawFile
  precheckResult.value = undefined
  importResult.value = undefined
}

const handleFileRemove = () => {
  selectedFile.value = undefined
  precheckResult.value = undefined
  importResult.value = undefined
}

const handleFileExceed = () => {
  message.error('最多只能选择一个配置包文件')
}

const showRequestError = (error: unknown, defaultMessage: string) => {
  const text = error instanceof Error ? error.message : typeof error === 'string' ? error : defaultMessage
  message.error(text || defaultMessage)
}

const isCancelError = (error: unknown) => {
  return error === 'cancel' || error === 'close' || (error instanceof Error && error.message === 'cancel')
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const data = await ConfigPackageApi.exportConfigPackage()
    download.excel(data, '系统配置包.xlsx')
    message.success('导出完成')
  } catch (error) {
    showRequestError(error, '导出失败')
  } finally {
    exportLoading.value = false
  }
}

const handlePrecheck = async () => {
  if (!ensureSelectedFile() || !ensureComponentManifest()) {
    return
  }
  precheckLoading.value = true
  try {
    const result = await ConfigPackageApi.precheckConfigPackage(selectedFile.value!, availableComponents)
    precheckResult.value = result
    importResult.value = undefined
    message[result.valid ? 'success' : 'error'](result.valid ? '预检通过' : '预检未通过')
  } catch (error) {
    showRequestError(error, '预检失败')
  } finally {
    precheckLoading.value = false
  }
}

const handleImport = async () => {
  if (!ensureSelectedFile() || !ensureComponentManifest()) {
    return
  }
  if (!precheckResult.value?.valid) {
    message.error('预检通过后才能覆盖导入')
    return
  }
  try {
    await message.confirm('确认覆盖当前环境配置数据吗？')
  } catch (error) {
    if (isCancelError(error)) {
      return
    }
    showRequestError(error, '确认失败')
    return
  }

  importLoading.value = true
  try {
    importResult.value = await ConfigPackageApi.importConfigPackage(
      selectedFile.value!,
      availableComponents,
      precheckResult.value.targetSnapshotSha256
    )
    message.success('导入完成')
  } catch (error) {
    showRequestError(error, '导入失败')
  } finally {
    importLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.config-package-toolbar {
  border-color: #dbe3ef;
}

.config-package-form {
  display: flex;
  flex-wrap: wrap;
  gap: 0 8px;
}

.config-package-form :deep(.el-form-item) {
  margin-right: 12px;
  margin-bottom: 12px;
}

.config-package-upload {
  display: flex;
  align-items: center;
  gap: 10px;
}

.config-package-upload :deep(.el-upload-list) {
  max-width: 280px;
}

.config-package-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.config-package-message-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.7;
}
</style>
