<template>
  <ContentWrap>
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="140px"
      v-loading="formLoading"
    >
      <el-card shadow="never">
        <template #header>
          <div class="flex items-center justify-between gap-12px">
            <CardTitle title="NAS 管理" />
            <div class="flex items-center gap-12px">
              <el-button
                @click="handleTest"
                v-hasPermi="['infra:nas:test']"
                :loading="testLoading"
              >
                测试连接
              </el-button>
              <el-button
                type="primary"
                @click="onSubmit"
                v-hasPermi="['infra:nas:update']"
                :loading="saveLoading"
              >
                保存
              </el-button>
            </div>
          </div>
        </template>

        <el-form-item label="NAS 服务器" prop="server">
          <el-input v-model="formData.server" placeholder="请输入 NAS 服务器地址" />
        </el-form-item>
        <div class="mb-16px rounded-[6px] border border-[#dbe3ef] bg-[#f7f9fc] px-12px py-10px">
          <div class="flex items-center justify-between gap-12px">
            <div class="text-[13px] font-600 text-[#172033]">补充连接参数</div>
            <el-button text type="primary" @click="toggleOptionalFields">
              {{ shouldShowOptionalFields ? '收起补充参数' : '补充端口 / 域' }}
            </el-button>
          </div>
          <div v-if="!shouldShowOptionalFields" class="mt-6px text-[12px] leading-[18px] text-[#6b7280]">
            当前只显示已设置的可选参数；如需补录端口或域，请点击右侧展开。
          </div>
        </div>
        <el-form-item v-if="shouldShowPortField" label="NAS 端口" prop="port">
          <el-input-number
            v-model="formData.port"
            class="!w-220px"
            :min="1"
            :max="65535"
            :step="1"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="共享名" prop="share">
          <el-input v-model="formData.share" placeholder="请输入共享名" />
        </el-form-item>
        <el-form-item v-if="shouldShowDomainField" label="域" prop="domain">
          <el-input v-model="formData.domain" placeholder="请输入域；如无可留空" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            show-password
            placeholder="请输入密码"
          />
        </el-form-item>

        <el-alert
          v-if="testResult"
          :type="testResult.type"
          :title="testResult.title"
          :description="testResult.description"
          show-icon
          :closable="false"
        />

        <el-divider />

        <div class="flex items-center justify-between gap-12px mb-12px">
          <div class="text-[14px] font-600 text-[#172033]">目录结构</div>
          <div class="flex items-center gap-8px">
            <el-button
              @click="handleRefreshDirectory"
              :disabled="!canRefreshDirectory"
              :loading="directoryLoading"
              v-hasPermi="['infra:nas:query']"
            >
              刷新目录
            </el-button>
            <el-button
              type="danger"
              plain
              @click="handleStartControlAudit"
              :loading="controlAuditDialog.loading"
              v-if="canControlAuditPermission"
            >
              统计未受控文件
            </el-button>
            <el-button
              @click="handleToggleSelectionMode"
              :disabled="!directoryTree.length"
              v-hasPermi="['infra:nas:query']"
            >
              {{ selectionMode ? '取消选择' : '选择' }}
            </el-button>
            <input
              ref="localFolderInputRef"
              type="file"
              webkitdirectory
              multiple
              class="hidden"
              @change="handleLocalFolderSelected"
            />
            <el-button
              type="success"
              @click="handleTriggerLocalFolderImport"
              :disabled="!canTriggerLocalFolderImport"
              v-if="canTransferPermission"
            >
              导入文件夹
            </el-button>
            <el-button
              type="primary"
              @click="handleExportSelection"
              :disabled="!selectionMode || !selectedDirectoryPaths.length"
              v-hasPermi="['infra:nas:query']"
            >
              导出
            </el-button>
            <el-button
              type="warning"
              @click="handleOpenTransferDialog"
              :disabled="!selectionMode || !selectedDirectoryPaths.length"
              v-if="canTransferPermission"
            >
              转移
            </el-button>
          </div>
        </div>

        <div v-if="directorySummary" class="mb-12px text-[13px] text-[#4b5563] leading-[20px]">
          <div>共享根：{{ directorySummary.rootPath }}</div>
          <div>当前层路径：{{ directorySummary.currentPath || '根目录' }}</div>
          <div>当前层目录数：{{ directorySummary.directoryCount }}</div>
          <div v-if="selectionMode">已选择目录数：{{ selectedDirectoryPaths.length }}</div>
        </div>

        <el-alert
          v-if="directorySkipped.length"
          type="warning"
          title="部分目录已跳过"
          :description="`已跳过 ${directorySkipped.length} 个无权限目录，请查看下方列表。`"
          show-icon
          :closable="false"
          class="mb-12px"
        />

        <el-alert
          v-if="directoryError"
          type="error"
          title="刷新目录失败"
          :description="directoryError"
          show-icon
          :closable="false"
          class="mb-12px"
        />

        <div
          class="rounded-[6px] border border-[#dbe3ef] bg-[#fafcff] min-h-[240px] p-12px"
          v-loading="directoryLoading"
        >
          <el-empty
            v-if="!directoryTree.length"
            :description="
              canRefreshDirectory ? '点击刷新目录后同步 NAS 目录结构' : '测试连接成功后即可刷新目录'
            "
          />
          <el-tree
            ref="directoryTreeRef"
            v-else
            :key="directoryTreeKey"
            :data="directoryTree"
            node-key="path"
            :props="treeProps"
            :show-checkbox="selectionMode"
            check-strictly
            lazy
            :load="loadNasNode"
            empty-text="暂无目录"
            @check="handleDirectoryCheck"
          >
            <template #default="{ data }">
              <div class="flex items-center gap-8px py-2px">
                <span class="text-[#172033]">{{ data.name }}</span>
                <span v-if="data.path !== data.name" class="text-[12px] text-[#8a94a6]">
                  {{ data.path }}
                </span>
              </div>
            </template>
          </el-tree>
        </div>

        <div
          v-if="directorySkipped.length"
          class="mt-12px rounded-[6px] border border-[#f4d7a3] bg-[#fffaf0] p-12px"
        >
          <div class="mb-8px text-[13px] font-600 text-[#9a5b00]">已跳过目录</div>
          <div
            v-for="item in directorySkipped"
            :key="item.path"
            class="mb-6px text-[12px] leading-[18px] text-[#6b7280]"
          >
            <div>{{ item.path }}</div>
            <div class="text-[#9a5b00]">{{ item.reason }}</div>
          </div>
        </div>
      </el-card>
    </el-form>

    <el-dialog
      v-model="transferDialog.visible"
      :title="transferDialogTitle"
      width="760px"
      destroy-on-close
    >
      <el-alert
        v-if="transferDialog.errorMessage"
        type="error"
        :title="transferDialog.errorMessage"
        show-icon
        :closable="false"
        class="mb-16px"
      />
      <el-form ref="transferFormRef" :model="transferDialog.form" :rules="transferFormRules" label-width="96px">
        <el-form-item label="已选目录">
          <div class="flex flex-wrap gap-8px">
            <el-tag v-for="path in selectedTransferPaths" :key="path" type="info" effect="plain">
              {{ path }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="模板类别" prop="templateCategoryId">
          <el-select
            v-model="transferDialog.form.templateCategoryId"
            class="!w-420px"
            clearable
            filterable
            placeholder="请选择 DCC 模板类别"
          >
            <el-option
              v-for="item in transferDialog.categoryOptions"
              :key="item.id"
              :label="item.directoryId ? item.name : `${item.name}（自动落位未分类）`"
              :value="item.id as number"
            />
          </el-select>
          <div
            v-if="selectedTransferCategory && !selectedTransferCategory.directoryId"
            class="text-12px text-[var(--el-color-info)] mt-4px"
          >
            当前模板类别未绑定受控目录，系统将自动落位到未分类目录。
          </div>
        </el-form-item>
        <el-form-item label="DCC 项目" prop="dccProjectCodeId">
          <el-select
            v-model="transferDialog.form.dccProjectCodeId"
            class="!w-420px"
            clearable
            filterable
            remote
            reserve-keyword
            :loading="transferDialog.projectCodeOptionsLoading"
            :remote-method="loadTransferProjectCodeOptions"
            placeholder="请选择 DCC 项目"
          >
            <el-option
              v-for="item in transferDialog.projectCodeOptions"
              :key="item.id"
              :label="formatDccProjectCodeOption(item)"
              :value="item.id as number"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="产品编号">
          <el-input
            :model-value="selectedTransferProjectCode?.projectCode || ''"
            class="!w-420px"
            readonly
            placeholder="选择 DCC 项目后自动生成"
          />
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker
            v-model="transferDialog.form.effectiveDate"
            class="!w-220px"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择生效日期"
          />
        </el-form-item>
        <el-form-item label="导入规则">
          <div class="rounded-[6px] border border-[#dbe3ef] bg-[#f7f9fc] px-12px py-10px text-[13px] leading-[20px] text-[#4b5563]">
            <div>文件名称=源文件名</div>
            <div>文件编号=去扩展名文件名</div>
            <div>初始版本=V1.0</div>
            <div v-if="transferDialog.sourceType === 'LOCAL_FOLDER'">本地导入不采集 NAS 权限快照</div>
          </div>
        </el-form-item>
      </el-form>

      <div
        v-if="transferDialog.result"
        class="mt-12px rounded-[6px] border border-[#dbe3ef] bg-[#fafcff] p-12px"
      >
        <div class="mb-10px flex items-center justify-between gap-12px">
          <div class="text-[14px] font-600 text-[#172033]">转移任务</div>
          <el-tag :type="resolveTransferTaskStatusType(transferDialog.result.status)">
            {{ resolveTransferTaskStatusLabel(transferDialog.result.status) }}
          </el-tag>
        </div>
        <div class="grid grid-cols-2 gap-10px text-[13px] text-[#4b5563] md:grid-cols-4">
          <div>任务编号：{{ transferDialog.result.taskId }}</div>
          <div>待处理条目：{{ transferDialog.result.remainingPendingCount }}</div>
          <div>新建目录：{{ transferDialog.result.createdDirectoryCount }}</div>
          <div>复用目录：{{ transferDialog.result.reusedDirectoryCount }}</div>
          <div>新建类别：{{ transferDialog.result.createdCategoryCount }}</div>
          <div>复用类别：{{ transferDialog.result.reusedCategoryCount }}</div>
          <div>成功文件：{{ transferDialog.result.createdFileCount }}</div>
          <div>失败条目：{{ transferDialog.result.failedFileCount }}</div>
          <div>仅下载预览：{{ transferDialog.result.skippedPreviewOnlyCount }}</div>
          <div v-if="transferDialog.result.completedAt">完成时间：{{ transferDialog.result.completedAt }}</div>
        </div>
        <div
          v-if="transferDialog.result.sourceType === 'LOCAL_FOLDER'"
          class="mt-12px rounded-[6px] border border-[#cfe3ff] bg-[#f5f9ff] px-12px py-10px text-[12px] text-[#2457a6]"
        >
          <div class="mb-8px flex items-center justify-between gap-12px">
            <span class="font-600">上传进度</span>
            <span>分片上传 {{ transferDialog.uploadChunkIndex }} / {{ transferDialog.uploadChunkCount }}</span>
          </div>
          <el-progress :percentage="localFolderUploadProgressPercent" :stroke-width="6" />
          <div class="mt-8px grid grid-cols-1 gap-6px md:grid-cols-3">
            <div>
              已上传文件：{{ transferDialog.result.uploadedFileCount }} /
              {{ transferDialog.result.expectedFileCount }}
            </div>
            <div>
              已上传大小：{{ formatLocalFolderBytes(transferDialog.result.uploadedTotalBytes) }} /
              {{ formatLocalFolderBytes(transferDialog.result.expectedTotalBytes) }}
            </div>
            <div v-if="transferDialog.sessionCreatedAt">
              会话创建：{{ transferDialog.sessionCreatedAt }}
            </div>
          </div>
        </div>
        <div
          v-if="isTransferTaskActive(transferDialog.result.status)"
          class="mt-12px rounded-[6px] border border-[#cfe3ff] bg-[#f5f9ff] px-12px py-10px text-[12px] text-[#2457a6]"
        >
          任务正在后台执行，前端会自动轮询最新状态。
        </div>
        <div
          v-if="transferDialog.result.sourceType === 'LOCAL_FOLDER'"
          class="mt-12px rounded-[6px] border border-[#dbe3ef] bg-white px-12px py-10px text-[12px] text-[#4b5563]"
        >
          本地导入不采集 NAS 权限快照。
        </div>
        <div
          v-if="transferDialog.result.lastFailureMessage"
          class="mt-12px rounded-[6px] border border-[#f4d7a3] bg-[#fffaf0] px-12px py-10px text-[12px] text-[#9a5b00]"
        >
          最近错误：{{ transferDialog.result.lastFailureMessage }}
        </div>
        <div
          v-if="transferDialog.result.failureReportPath || transferDialog.result.failureReportError"
          class="mt-12px rounded-[6px] border border-[#dbe3ef] bg-white px-12px py-10px text-[12px] text-[#4b5563]"
        >
          <div v-if="transferDialog.result.failureReportPath">
            失败报告：{{ transferDialog.result.failureReportPath }}
          </div>
          <div v-if="transferDialog.result.failureReportGeneratedAt">
            生成时间：{{ transferDialog.result.failureReportGeneratedAt }}
          </div>
          <div v-if="transferDialog.result.failureReportError" class="text-[#b42318]">
            报告生成错误：{{ transferDialog.result.failureReportError }}
          </div>
        </div>

        <NasPermissionRestorePanel
          v-if="transferDialog.result.sourceType !== 'LOCAL_FOLDER'"
          :task-id="transferDialog.result.taskId"
          :transfer-status="transferDialog.result.status"
        />

        <el-table
          v-if="transferDialog.result.failures.length"
          :data="transferDialog.result.failures"
          class="mt-12px"
          max-height="260"
        >
          <el-table-column label="NAS 路径" min-width="220" prop="nasPath" show-overflow-tooltip />
          <el-table-column label="阶段" width="120" prop="stage" />
          <el-table-column label="原因" min-width="220" prop="reason" show-overflow-tooltip />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="transferDialog.visible = false">关闭</el-button>
        <el-button
          type="primary"
          :loading="transferDialog.submitting"
          :disabled="!canSubmitTransfer"
          @click="handleSubmitTransfer"
        >
          {{ transferDialog.sourceType === 'LOCAL_FOLDER' ? '确认导入' : '确认转移' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="controlAuditDialog.visible"
      title="统计未受控文件"
      width="1080px"
      destroy-on-close
    >
      <el-alert
        v-if="controlAuditDialog.errorMessage"
        type="error"
        title="统计任务失败"
        :description="controlAuditDialog.errorMessage"
        show-icon
        :closable="false"
        class="mb-16px"
      />
      <template v-if="controlAuditDialog.result">
      <div class="rounded-[6px] border border-[#dbe3ef] bg-[#fafcff] p-12px">
        <div class="mb-10px flex items-center justify-between gap-12px">
          <div class="text-[14px] font-600 text-[#172033]">NAS 受控状态统计任务</div>
          <el-tag :type="resolveControlAuditStatusType(controlAuditDialog.result.status)">
            {{ resolveControlAuditStatusLabel(controlAuditDialog.result.status) }}
          </el-tag>
        </div>
        <div class="grid grid-cols-1 gap-10px text-[13px] text-[#4b5563] md:grid-cols-2">
          <div>任务编号：{{ controlAuditDialog.result.taskId }}</div>
          <div>NAS 共享：{{ controlAuditDialog.result.nasShareName || '-' }}</div>
          <div>当前扫描目录：{{ controlAuditDialog.result.currentPath || '-' }}</div>
          <div>已扫描文件数：{{ controlAuditDialog.result.scannedFileCount }}</div>
          <div>已跳过目录数：{{ controlAuditDialog.result.skippedDirectoryCount }}</div>
          <div>未受控数量：{{ controlAuditDialog.result.notControlledFileCount }}</div>
          <div>待确认数量：{{ controlAuditDialog.result.ambiguousFileCount }}</div>
          <div>来源缺失数量：{{ controlAuditDialog.result.sourceMissingCount }}</div>
          <div>无法扫描的文件数量：{{ controlAuditDialog.result.unscannedFileCountLabel || '未知' }}</div>
          <div v-if="controlAuditDialog.result.completedAt">完成时间：{{ controlAuditDialog.result.completedAt }}</div>
        </div>
        <div class="mt-12px rounded-[6px] border border-[#dbe3ef] bg-white px-12px py-10px text-[12px] leading-[20px] text-[#4b5563]">
          <div>固定扫描目录：1. QMS documents、2.DHF、3.DMR</div>
          <div>遇到无权限子目录会跳过该目录及其子树，并在报告“跳过目录”工作表记录。</div>
        </div>
      </div>
      <div
        v-if="controlAuditDialog.result.status === 'COMPLETED'"
        class="mt-14px rounded-[6px] border border-[#dbe3ef] bg-white p-12px"
      >
        <el-alert
          v-if="controlAuditFiles.errorMessage"
          type="error"
          title="未受控文件下载失败"
          :description="controlAuditFiles.errorMessage"
          show-icon
          :closable="false"
          class="mb-12px"
        />
        <div class="mb-10px flex flex-wrap items-center justify-between gap-10px">
          <div>
            <div class="text-[14px] font-600 text-[#172033]">未受控文件下载与归类</div>
            <div class="mt-4px text-[12px] text-[#6b7280]">
              仅自动处理已唯一匹配项目代码、item 和文件分类的文件；无法唯一识别的文件保持“未分类/待处理”。
            </div>
          </div>
          <div class="flex flex-wrap gap-8px">
            <el-button
              :loading="controlAuditFiles.recognizing"
              @click="handleRecognizeNasUncontrolledFiles"
            >
              识别并刷新
            </el-button>
            <el-button
              type="success"
              :loading="controlAuditFiles.importing"
              :disabled="!canImportNasUncontrolledSelectedFiles"
              @click="handleDownloadSelectedNasUncontrolledFilesToLocal"
            >
              下载选中文件到本地并归类
            </el-button>
          </div>
        </div>
        <el-table
          v-loading="controlAuditFiles.loading"
          :data="controlAuditFiles.rows"
          row-key="auditFileId"
          max-height="360"
          @selection-change="handleNasUncontrolledFileSelectionChange"
        >
          <el-table-column
            type="selection"
            width="46"
            :selectable="isNasUncontrolledFileImportSelectable"
          />
          <el-table-column label="NAS 相对路径" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.normalizedRelativePath || row.fileName }}
            </template>
          </el-table-column>
          <el-table-column label="识别状态" width="150">
            <template #default="{ row }">
              <el-tag :type="resolveNasUncontrolledClassificationTagType(row.classificationStatus)">
                {{ resolveNasUncontrolledClassificationLabel(row.classificationStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="本地目标相对路径" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.expectedLocalRelativePath || '未分类/待处理' }}
            </template>
          </el-table-column>
          <el-table-column label="本地写入" width="120">
            <template #default="{ row }">
              {{ resolveNasUncontrolledDownloadStatusLabel(row.downloadStatus) }}
            </template>
          </el-table-column>
          <el-table-column label="归档状态" min-width="150">
            <template #default="{ row }">
              <el-tag
                v-if="row.archiveErrorCode === 'ARCHIVE_METADATA_REQUIRED'"
                type="warning"
              >
                归档元数据待补齐
              </el-tag>
              <span v-else>{{ resolveNasUncontrolledArchiveStatusLabel(row.archiveStatus) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="原因" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.archiveError || row.localWriteError || row.classificationReason || '-' }}
            </template>
          </el-table-column>
        </el-table>
      </div>
      </template>
      <el-empty v-else description="尚未创建统计任务" />
      <template #footer>
        <el-button @click="controlAuditDialog.visible = false">关闭</el-button>
        <el-button
          type="primary"
          :loading="controlAuditDialog.downloading"
          :disabled="controlAuditDialog.result?.status !== 'COMPLETED'"
          @click="handleDownloadControlAuditReport(false)"
        >
          重新下载报告
        </el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ElMessageBox, type ElTree } from 'element-plus'
import { CardTitle } from '@/components/Card'
import { downloadByData } from '@/utils/filt'
import { getFileCategoryList, type ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import NasPermissionRestorePanel from './components/NasPermissionRestorePanel.vue'
import {
  LOCAL_FOLDER_IMPORT_CHUNK_BYTES,
  completeLocalFolderImportSession,
  createLocalFolderImportSession,
  getLocalFolderImportUploadState,
  getNasTransferTaskState,
  transferNasDirectories,
  uploadLocalFolderImportChunk,
  type ControlledFileLocalFolderImportUploadStateRespVO,
  type ControlledFileLocalFolderImportSessionCreateReqVO,
  type ControlledFileNasTransferRespVO
} from '@/api/dcc/controlledFile/workflow'
import {
  DCC_PROJECT_CODE_STATUS_ENABLE,
  getProjectCodePage,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import {
  getNasConfig,
  saveNasConfig,
  listNasFiles,
  startNasControlAudit,
  getNasControlAuditTask,
  getNasControlAuditFiles,
  recognizeNasControlAuditFiles,
  importSelectedNasUncontrolledFiles,
  downloadNasUncontrolledImportContent,
  recordNasUncontrolledImportLocalWriteResult,
  downloadNasControlAuditReport,
  testNasConfig,
  type NasConfigVO,
  type NasFileItemVO,
  type NasDirectoryTreeSkippedVO,
  type NasControlAuditTaskRespVO,
  type DccNasControlAuditFileRespVO
} from '@/api/system/nas'
import { checkPermi } from '@/utils/permission'

defineOptions({ name: 'SystemNasManagement' })

const { t } = useI18n()
const message = useMessage()
const NAS_TRANSFER_LAST_TASK_ID_KEY = 'int-ruoyi:nas-transfer:last-task-id'
const NAS_CONTROL_AUDIT_LAST_TASK_ID_KEY = 'int-ruoyi:nas-control-audit:last-task-id'
const NAS_TRANSFER_CONFIRM_MODAL_CLASS = 'nas-transfer-confirm-message-box-overlay'

type TransferSourceType = 'NAS' | 'LOCAL_FOLDER'

interface LocalFolderSelection {
  files: File[]
  relativePaths: string[]
  rootDirectoryName: string
  totalSize: number
}

interface NasUncontrolledFileSystemWritableFileStream {
  write(data: Blob): Promise<void>
  close(): Promise<void>
}

interface NasUncontrolledFileSystemFileHandle {
  createWritable(): Promise<NasUncontrolledFileSystemWritableFileStream>
}

interface NasUncontrolledFileSystemDirectoryHandle {
  getDirectoryHandle(
    name: string,
    options?: { create?: boolean }
  ): Promise<NasUncontrolledFileSystemDirectoryHandle>
  getFileHandle(name: string, options?: { create?: boolean }): Promise<NasUncontrolledFileSystemFileHandle>
}

type NasUncontrolledWindow = Window &
  typeof globalThis & {
    showDirectoryPicker?: () => Promise<NasUncontrolledFileSystemDirectoryHandle>
  }

const formLoading = ref(false)
const saveLoading = ref(false)
const testLoading = ref(false)
const directoryLoading = ref(false)
const directoryTreeKey = ref(0)
const directoryTreeRef = ref<InstanceType<typeof ElTree>>()
const localFolderInputRef = ref<HTMLInputElement>()
const formRef = ref()
const transferFormRef = ref()
const formData = ref<NasConfigVO>({
  server: '',
  port: undefined,
  share: '',
  domain: undefined,
  username: '',
  password: ''
})
const testResult = ref<{
  type: 'success' | 'error'
  title: string
  description: string
} | null>(null)
interface NasDirectoryNode {
  name: string
  path: string
  children?: NasDirectoryNode[]
  leaf?: boolean
}

const directoryTree = ref<NasDirectoryNode[]>([])
const directorySummary = ref<{
  rootPath: string
  currentPath: string
  directoryCount: number
} | null>(null)
const directorySkipped = ref<NasDirectoryTreeSkippedVO[]>([])
const selectionMode = ref(false)
const selectedDirectoryPaths = ref<string[]>([])
const directoryError = ref('')
const canTransferPermission = computed(
  () =>
    checkPermi(['dcc:controlled-file:submit']) &&
    checkPermi(['dcc:controlled-file:directory:manage']) &&
    checkPermi(['dcc:controlled-file:category:manage'])
)
const canControlAuditPermission = computed(
  () => checkPermi(['infra:nas:query']) && checkPermi(['dcc:controlled-file:query'])
)
const transferDialog = reactive<{
  visible: boolean
  submitting: boolean
  sourceType: TransferSourceType
  errorMessage: string
  categoryOptions: ControlledFileCategoryVO[]
  projectCodeOptions: DccProjectCodeRespVO[]
  projectCodeOptionsLoading: boolean
  result: ControlledFileNasTransferRespVO | null
  localFolder: LocalFolderSelection
  sessionCreatedAt: string
  uploadChunkIndex: number
  uploadChunkCount: number
  form: {
    templateCategoryId?: number
    dccProjectCodeId?: number
    effectiveDate: string
  }
}>({
  visible: false,
  submitting: false,
  sourceType: 'NAS',
  errorMessage: '',
  categoryOptions: [],
  projectCodeOptions: [],
  projectCodeOptionsLoading: false,
  result: null,
  localFolder: {
    files: [],
    relativePaths: [],
    rootDirectoryName: '',
    totalSize: 0
  },
  sessionCreatedAt: '',
  uploadChunkIndex: 0,
  uploadChunkCount: 0,
  form: {
    templateCategoryId: undefined,
    dccProjectCodeId: undefined,
    effectiveDate: ''
  }
})
const controlAuditDialog = reactive<{
  visible: boolean
  loading: boolean
  downloading: boolean
  errorMessage: string
  result: NasControlAuditTaskRespVO | null
}>({
  visible: false,
  loading: false,
  downloading: false,
  errorMessage: '',
  result: null
})
const controlAuditFiles = reactive<{
  loading: boolean
  recognizing: boolean
  importing: boolean
  errorMessage: string
  rows: DccNasControlAuditFileRespVO[]
  selectedRows: DccNasControlAuditFileRespVO[]
  pageNo: number
  pageSize: number
  total: number
}>({
  loading: false,
  recognizing: false,
  importing: false,
  errorMessage: '',
  rows: [],
  selectedRows: [],
  pageNo: 1,
  pageSize: 50,
  total: 0
})
let transferTaskPollingTimer: number | undefined
let controlAuditPollingTimer: number | undefined
const autoDownloadedControlAuditTaskIds = new Set<number>()
const treeProps = {
  children: 'children',
  label: 'name',
  isLeaf: 'leaf'
}
const shouldShowOptionalField = (value?: string | null) => Boolean(value?.trim())
const hasConfiguredPort = computed(() => typeof formData.value.port === 'number' && formData.value.port > 0)
const shouldShowOptionalFields = ref(false)
const shouldShowPortField = computed(() => shouldShowOptionalFields.value || hasConfiguredPort.value)
const shouldShowDomainField = computed(
  () => shouldShowOptionalFields.value || shouldShowOptionalField(formData.value.domain)
)
const hasCompleteNasConfig = computed(() =>
  Boolean(
    formData.value.server?.trim() &&
      formData.value.share?.trim() &&
      formData.value.username?.trim() &&
      formData.value.password?.trim()
  )
)
const canRefreshDirectory = computed(
  () => testResult.value?.type === 'success' || hasCompleteNasConfig.value
)
const hasActiveTransferTask = computed(() =>
  ['UPLOADING', 'WAITING', 'RUNNING'].includes(transferDialog.result?.status || '')
)
const canTriggerLocalFolderImport = computed(
  () =>
    !hasActiveTransferTask.value ||
    (transferDialog.result?.sourceType === 'LOCAL_FOLDER' &&
      transferDialog.result?.status === 'UPLOADING')
)
const isLocalFolderTransfer = computed(() => transferDialog.sourceType === 'LOCAL_FOLDER')
const transferDialogTitle = computed(() =>
  isLocalFolderTransfer.value ? '导入文件夹到 DCC' : '转移到 DCC'
)
const selectedTransferPaths = computed(() =>
  isLocalFolderTransfer.value
    ? transferDialog.localFolder.rootDirectoryName
      ? [transferDialog.localFolder.rootDirectoryName]
      : []
    : selectedDirectoryPaths.value
)
const canResumeLocalFolderUpload = computed(
  () =>
    isLocalFolderTransfer.value &&
    transferDialog.result?.sourceType === 'LOCAL_FOLDER' &&
    transferDialog.result?.status === 'UPLOADING' &&
    transferDialog.localFolder.files.length > 0
)
const canSubmitTransfer = computed(
  () =>
    selectedTransferPaths.value.length > 0 &&
    (!hasActiveTransferTask.value || canResumeLocalFolderUpload.value)
)
const canImportNasUncontrolledSelectedFiles = computed(
  () =>
    canTransferPermission.value &&
    controlAuditDialog.result?.status === 'COMPLETED' &&
    controlAuditFiles.selectedRows.some((row) => isNasUncontrolledFileImportSelectable(row)) &&
    !controlAuditFiles.importing
)
const selectedTransferCategory = computed(() =>
  transferDialog.categoryOptions.find((item) => item.id === transferDialog.form.templateCategoryId)
)
const selectedTransferProjectCode = computed(() =>
  transferDialog.projectCodeOptions.find((item) => item.id === transferDialog.form.dccProjectCodeId)
)
const localFolderUploadProgressPercent = computed(() => {
  const result = transferDialog.result
  if (!result || result.sourceType !== 'LOCAL_FOLDER' || !result.expectedFileCount) {
    return 0
  }
  return Math.min(100, Math.round((result.uploadedFileCount / result.expectedFileCount) * 100))
})

const formRules = reactive({
  server: [{ required: true, message: 'NAS 服务器不能为空', trigger: 'blur' }],
  port: [{ required: true, message: 'NAS 端口不能为空', trigger: 'change' }],
  share: [{ required: true, message: '共享名不能为空', trigger: 'blur' }],
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }]
})
const transferFormRules = reactive({
  templateCategoryId: [{ required: true, message: '请选择 DCC 模板类别', trigger: 'change' }],
  dccProjectCodeId: [{ required: true, message: '请选择 DCC 项目', trigger: 'change' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }]
})

const getConfig = async () => {
  formLoading.value = true
  try {
    const data = await getNasConfig()
    if (data) {
      formData.value = {
        server: data.server || '',
        port: typeof data.port === 'number' && data.port > 0 ? data.port : undefined,
        share: data.share || '',
        domain: shouldShowOptionalField(data.domain) ? data.domain?.trim() : undefined,
        username: data.username || '',
        password: data.password || ''
      }
      shouldShowOptionalFields.value =
        (typeof data.port === 'number' && data.port > 0) || shouldShowOptionalField(data.domain)
    }
  } finally {
    formLoading.value = false
  }
}

const toggleOptionalFields = () => {
  shouldShowOptionalFields.value = !shouldShowOptionalFields.value
  if (shouldShowOptionalFields.value) {
    return
  }
  if (!hasConfiguredPort.value) {
    formData.value.port = undefined
  }
  if (!shouldShowOptionalField(formData.value.domain)) {
    formData.value.domain = undefined
  }
}

const buildNasConfigPayload = (): NasConfigVO => ({
  server: formData.value.server?.trim() || '',
  port: shouldShowPortField.value ? formData.value.port : undefined,
  share: formData.value.share?.trim() || '',
  domain: shouldShowDomainField.value ? formData.value.domain?.trim() || undefined : undefined,
  username: formData.value.username?.trim() || '',
  password: formData.value.password || ''
})

const resetDirectoryView = () => {
  directoryTree.value = []
  directorySummary.value = null
  directorySkipped.value = []
  selectionMode.value = false
  selectedDirectoryPaths.value = []
  directoryError.value = ''
  directoryTreeKey.value += 1
}

const mapDirectoryItems = (items: NasFileItemVO[]) => {
  return items
    .filter((item) => item.dir)
    .map((item) => ({
      name: item.name,
      path: item.path,
      leaf: false
    }))
}

const appendSkippedDirectory = (path: string, reason: string) => {
  if (directorySkipped.value.some((item) => item.path === path && item.reason === reason)) {
    return
  }
  directorySkipped.value = [...directorySkipped.value, { path, reason }]
}

const handleDirectoryCheck = () => {
  selectedDirectoryPaths.value =
    (directoryTreeRef.value?.getCheckedKeys(false) as string[] | undefined) || []
}

const handleToggleSelectionMode = async () => {
  selectionMode.value = !selectionMode.value
  selectedDirectoryPaths.value = []
  await nextTick()
  directoryTreeRef.value?.setCheckedKeys([])
}

const handleExportSelection = () => {
  if (!selectedDirectoryPaths.value.length) return
  message.warning(`已选择 ${selectedDirectoryPaths.value.length} 个目录，导出功能暂未开放`)
}

const resetLocalFolderSelection = () => {
  transferDialog.localFolder = {
    files: [],
    relativePaths: [],
    rootDirectoryName: '',
    totalSize: 0
  }
  transferDialog.sessionCreatedAt = ''
  transferDialog.uploadChunkIndex = 0
  transferDialog.uploadChunkCount = 0
}

const getFileRelativePath = (file: File) =>
  ((file as File & { webkitRelativePath?: string }).webkitRelativePath || '').trim()

const validateLocalFolderRelativePath = (relativePath: string, rootDirectoryName?: string) => {
  if (
    !relativePath ||
    relativePath.includes('\\') ||
    relativePath.startsWith('/') ||
    /^[A-Za-z]:/.test(relativePath) ||
    relativePath.endsWith('/')
  ) {
    throw new Error('本地文件夹相对路径不合法，无法导入')
  }
  const segments = relativePath.split('/')
  if (segments.length < 2) {
    throw new Error('本地文件夹相对路径缺少根目录，无法导入')
  }
  if (segments.some((segment) => !segment || segment === '.' || segment === '..')) {
    throw new Error('本地文件夹相对路径包含非法目录段，无法导入')
  }
  if (rootDirectoryName && segments[0] !== rootDirectoryName) {
    throw new Error('本地文件夹相对路径根目录不一致，无法导入')
  }
  return {
    rootDirectoryName: segments[0],
    relativePath
  }
}

const validateLocalFolderFiles = (files: File[]): LocalFolderSelection => {
  if (!files.length) {
    throw new Error('本地文件夹为空，无法导入')
  }
  const totalSize = files.reduce((sum, file) => sum + file.size, 0)
  let rootDirectoryName = ''
  const relativePaths: string[] = []
  const seenPaths = new Set<string>()
  for (const file of files) {
    const webkitRelativePath = getFileRelativePath(file)
    if (!webkitRelativePath) {
      throw new Error('浏览器未返回 webkitRelativePath，无法导入本地文件夹')
    }
    const validated = validateLocalFolderRelativePath(webkitRelativePath, rootDirectoryName || undefined)
    rootDirectoryName = rootDirectoryName || validated.rootDirectoryName
    if (seenPaths.has(validated.relativePath)) {
      throw new Error('本地文件夹存在重复相对路径，无法导入')
    }
    seenPaths.add(validated.relativePath)
    relativePaths.push(validated.relativePath)
  }
  return {
    files,
    relativePaths,
    rootDirectoryName,
    totalSize
  }
}

const requestNasUncontrolledDirectoryHandle = async () => {
  const showDirectoryPicker = (window as NasUncontrolledWindow).showDirectoryPicker
  if (typeof showDirectoryPicker !== 'function') {
    throw new Error('当前浏览器不支持 showDirectoryPicker，无法下载未受控文件到指定本地目录')
  }
  return await showDirectoryPicker.call(window)
}

const validateNasUncontrolledLocalRelativePath = (relativePath: string) => {
  if (
    !relativePath ||
    relativePath.includes('\\') ||
    relativePath.startsWith('/') ||
    /^[A-Za-z]:/.test(relativePath) ||
    relativePath.endsWith('/')
  ) {
    throw new Error('未受控文件本地相对路径不合法，无法创建导入任务')
  }
  const segments = relativePath.split('/')
  if (segments.some((segment) => !segment || segment === '.' || segment === '..')) {
    throw new Error('未受控文件本地相对路径包含非法目录段，无法创建导入任务')
  }
  return segments
}

const getNasUncontrolledLocalTargetFileHandle = async (
  directoryHandle: NasUncontrolledFileSystemDirectoryHandle,
  relativePath: string
) => {
  const segments = validateNasUncontrolledLocalRelativePath(relativePath)
  const fileName = segments[segments.length - 1]
  let currentDirectoryHandle = directoryHandle
  for (const directoryName of segments.slice(0, -1)) {
    currentDirectoryHandle = await currentDirectoryHandle.getDirectoryHandle(directoryName, {
      create: true
    })
  }
  return await currentDirectoryHandle.getFileHandle(fileName, { create: true })
}

const writeNasUncontrolledBlobToLocalFile = async (
  directoryHandle: NasUncontrolledFileSystemDirectoryHandle,
  relativePath: string,
  blob: Blob
) => {
  const fileHandle = await getNasUncontrolledLocalTargetFileHandle(directoryHandle, relativePath)
  const writable = await fileHandle.createWritable()
  await writable.write(blob)
  await writable.close()
}

const buildLocalFolderImportSessionPayload = (): ControlledFileLocalFolderImportSessionCreateReqVO => ({
  templateCategoryId: transferDialog.form.templateCategoryId as number,
  dccProjectCodeId: transferDialog.form.dccProjectCodeId as number,
  productMasterId: null,
  effectiveDate: transferDialog.form.effectiveDate,
  rootDirectoryName: transferDialog.localFolder.rootDirectoryName,
  expectedFileCount: transferDialog.localFolder.files.length,
  expectedTotalBytes: transferDialog.localFolder.totalSize
})

const getLocalFolderFileName = (relativePath: string) => relativePath.split('/').pop() || ''

const getLocalFolderFileChunkCount = (file: File) => {
  if (file.size <= 0) {
    return 1
  }
  return Math.ceil(file.size / LOCAL_FOLDER_IMPORT_CHUNK_BYTES)
}

const calculateBlobSha256 = async (blob: Blob) => {
  if (!globalThis.crypto?.subtle) {
    throw new Error('当前浏览器不支持分片校验，无法导入本地文件夹')
  }
  const buffer = await blob.arrayBuffer()
  const digest = await globalThis.crypto.subtle.digest('SHA-256', buffer)
  return Array.from(new Uint8Array(digest), (item) => item.toString(16).padStart(2, '0')).join('')
}

const buildLocalFolderUploadStateFileMap = (
  uploadState: ControlledFileLocalFolderImportUploadStateRespVO
) => {
  const fileMap = new Map<string, Set<number>>()
  for (const fileState of uploadState.files || []) {
    fileMap.set(fileState.relativePath, new Set(fileState.uploadedChunkIndexes || []))
  }
  return fileMap
}

const validateLocalFolderUploadStateMatchesSelection = (
  uploadState: ControlledFileLocalFolderImportUploadStateRespVO,
  selection: LocalFolderSelection
) => {
  if (uploadState.status !== 'UPLOADING') {
    throw new Error('本地文件夹导入任务已不处于上传中，无法继续上传')
  }
  if (uploadState.rootDirectoryName !== selection.rootDirectoryName) {
    throw new Error('本次选择的本地文件夹与上传任务根目录不一致，无法继续上传')
  }
  if (
    uploadState.expectedFileCount !== selection.files.length ||
    uploadState.expectedTotalBytes !== selection.totalSize
  ) {
    throw new Error('本次选择的本地文件夹文件数量或大小与上传任务不一致，无法继续上传')
  }
}

const resolveLocalFolderImportSession = async () => {
  const activeTask = transferDialog.result
  if (activeTask?.sourceType === 'LOCAL_FOLDER' && activeTask.status === 'UPLOADING') {
    const uploadState = await getLocalFolderImportUploadState(activeTask.taskId)
    validateLocalFolderUploadStateMatchesSelection(uploadState, transferDialog.localFolder)
    return {
      task: activeTask,
      uploadState
    }
  }
  const task = await createLocalFolderImportSession(buildLocalFolderImportSessionPayload())
  transferDialog.sessionCreatedAt = new Date().toISOString()
  applyTransferTaskResult(task)
  const uploadState = await getLocalFolderImportUploadState(task.taskId)
  validateLocalFolderUploadStateMatchesSelection(uploadState, transferDialog.localFolder)
  return {
    task,
    uploadState
  }
}

const buildLocalFolderImportChunkFormData = async (
  file: File,
  relativePath: string,
  chunkIndex: number,
  totalChunks: number
) => {
  const start = chunkIndex * LOCAL_FOLDER_IMPORT_CHUNK_BYTES
  const end = Math.min(file.size, start + LOCAL_FOLDER_IMPORT_CHUNK_BYTES)
  const chunk = file.slice(start, end)
  const formData = new FormData()
  formData.append('relativePath', relativePath)
  formData.append('fileName', getLocalFolderFileName(relativePath))
  formData.append('fileSize', String(file.size))
  formData.append('chunkIndex', String(chunkIndex))
  formData.append('totalChunks', String(totalChunks))
  formData.append('chunkSha256', await calculateBlobSha256(chunk))
  formData.append('contentType', file.type || 'application/octet-stream')
  formData.append('chunk', chunk, file.name)
  return formData
}

const countRemainingLocalFolderChunks = (
  selection: LocalFolderSelection,
  uploadState: ControlledFileLocalFolderImportUploadStateRespVO
) => {
  const completedRelativePaths = new Set(uploadState.uploadedRelativePaths || [])
  const uploadedChunkIndexesByPath = buildLocalFolderUploadStateFileMap(uploadState)
  let remainingChunkCount = 0
  selection.files.forEach((file, index) => {
    const relativePath = selection.relativePaths[index]
    if (completedRelativePaths.has(relativePath)) {
      return
    }
    const totalChunks = getLocalFolderFileChunkCount(file)
    const uploadedChunkIndexes = uploadedChunkIndexesByPath.get(relativePath) || new Set<number>()
    remainingChunkCount += Math.max(0, totalChunks - uploadedChunkIndexes.size)
  })
  return remainingChunkCount
}

const uploadLocalFolderImportChunks = async (
  taskId: number,
  uploadState: ControlledFileLocalFolderImportUploadStateRespVO
) => {
  const completedRelativePaths = new Set(uploadState.uploadedRelativePaths || [])
  const uploadedChunkIndexesByPath = buildLocalFolderUploadStateFileMap(uploadState)
  transferDialog.uploadChunkCount = countRemainingLocalFolderChunks(
    transferDialog.localFolder,
    uploadState
  )
  transferDialog.uploadChunkIndex = 0

  for (const [fileIndex, file] of transferDialog.localFolder.files.entries()) {
    const relativePath = transferDialog.localFolder.relativePaths[fileIndex]
    if (completedRelativePaths.has(relativePath)) {
      continue
    }
    const totalChunks = getLocalFolderFileChunkCount(file)
    const uploadedChunkIndexes =
      uploadedChunkIndexesByPath.get(relativePath) || new Set<number>()
    for (let chunkIndex = 0; chunkIndex < totalChunks; chunkIndex += 1) {
      if (uploadedChunkIndexes.has(chunkIndex)) {
        continue
      }
      transferDialog.uploadChunkIndex += 1
      const result = await uploadLocalFolderImportChunk(
        taskId,
        await buildLocalFolderImportChunkFormData(file, relativePath, chunkIndex, totalChunks)
      )
      uploadedChunkIndexes.add(chunkIndex)
      uploadedChunkIndexesByPath.set(relativePath, uploadedChunkIndexes)
      if (result.fileCompleted) {
        completedRelativePaths.add(relativePath)
      }
      applyTransferTaskResult(result.task)
    }
  }
}

const formatLocalFolderBytes = (bytes?: number | null) => {
  if (!bytes || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let value = bytes
  let unitIndex = 0
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex += 1
  }
  return `${value.toFixed(unitIndex === 0 ? 0 : 2)} ${units[unitIndex]}`
}

const loadTransferCategoryOptions = async () => {
  if (!transferDialog.categoryOptions.length) {
    const categories = await getFileCategoryList()
    transferDialog.categoryOptions = categories.filter((item) => item.active)
  }
  const otherCategory = transferDialog.categoryOptions.find((item) => item.name === '其他')
  if (!otherCategory?.id) {
    throw new Error('DCC 模板类别缺少启用的“其他”，请先在 DCC 文件类别中补齐后再转移')
  }
  transferDialog.form.templateCategoryId = otherCategory.id
}

const loadTransferProjectCodeOptions = async (keyword = '') => {
  transferDialog.projectCodeOptionsLoading = true
  try {
    const data = await getProjectCodePage({
      pageNo: 1,
      pageSize: 50,
      status: DCC_PROJECT_CODE_STATUS_ENABLE,
      keyword: keyword.trim() || undefined
    })
    transferDialog.projectCodeOptions = data.list
    if (
      transferDialog.form.dccProjectCodeId &&
      !transferDialog.projectCodeOptions.some((item) => item.id === transferDialog.form.dccProjectCodeId)
    ) {
      transferDialog.form.dccProjectCodeId = undefined
    }
  } finally {
    transferDialog.projectCodeOptionsLoading = false
  }
}

const escapeHtml = (value: string) =>
  value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')

const formatDccProjectCodeOption = (item: DccProjectCodeRespVO) =>
  [item.projectName, item.projectCode, item.docControlNo].filter(Boolean).join(' / ')

const clearTransferTaskPolling = () => {
  if (transferTaskPollingTimer) {
    window.clearTimeout(transferTaskPollingTimer)
    transferTaskPollingTimer = undefined
  }
}

const clearControlAuditPolling = () => {
  if (controlAuditPollingTimer) {
    window.clearTimeout(controlAuditPollingTimer)
    controlAuditPollingTimer = undefined
  }
}

const closeTransferDialogOnRouteLeave = () => {
  clearTransferTaskPolling()
  clearControlAuditPolling()
  transferDialog.visible = false
  controlAuditDialog.visible = false
  transferDialog.submitting = false
  controlAuditDialog.loading = false
}

const isTransferTaskActive = (status?: string | null) =>
  ['UPLOADING', 'WAITING', 'RUNNING'].includes(status || '')

const isControlAuditTaskActive = (status?: string | null) =>
  ['WAITING', 'RUNNING'].includes(status || '')

const persistLastTransferTaskId = (taskId?: number | null) => {
  if (typeof taskId !== 'number' || !Number.isSafeInteger(taskId) || taskId <= 0) {
    throw new Error('NAS 转移任务编号无效，无法保存任务上下文')
  }
  localStorage.setItem(NAS_TRANSFER_LAST_TASK_ID_KEY, String(taskId))
}

const clearLastTransferTaskId = () => {
  localStorage.removeItem(NAS_TRANSFER_LAST_TASK_ID_KEY)
}

const readLastTransferTaskId = () => {
  const rawTaskId = localStorage.getItem(NAS_TRANSFER_LAST_TASK_ID_KEY)
  if (!rawTaskId) return undefined
  const taskId = Number(rawTaskId)
  if (!Number.isSafeInteger(taskId) || taskId <= 0) {
    throw new Error('最近 NAS 转移任务编号无效，请重新发起转移')
  }
  return taskId
}

const persistLastControlAuditTaskId = (taskId?: number | null) => {
  if (typeof taskId !== 'number' || !Number.isSafeInteger(taskId) || taskId <= 0) {
    throw new Error('NAS 受控统计任务编号无效，无法保存任务上下文')
  }
  localStorage.setItem(NAS_CONTROL_AUDIT_LAST_TASK_ID_KEY, String(taskId))
}

const clearLastControlAuditTaskId = () => {
  localStorage.removeItem(NAS_CONTROL_AUDIT_LAST_TASK_ID_KEY)
}

const readLastControlAuditTaskId = () => {
  const rawTaskId = localStorage.getItem(NAS_CONTROL_AUDIT_LAST_TASK_ID_KEY)
  if (!rawTaskId) return undefined
  const taskId = Number(rawTaskId)
  if (!Number.isSafeInteger(taskId) || taskId <= 0) {
    throw new Error('最近 NAS 受控统计任务编号无效，请重新发起统计')
  }
  return taskId
}

const getErrorMessage = (error: unknown) => {
  if (error instanceof Error) return error.message
  return String(error || '')
}

const isNasTransferTaskNotFoundError = (error: unknown) => {
  const errorMessage = getErrorMessage(error)
  return (
    errorMessage.includes('nas transfer task not found') ||
    errorMessage.includes('最近 NAS 转移任务编号无效') ||
    errorMessage.includes('NAS 转移任务不存在')
  )
}

const clearStaleNasTransferTask = () => {
  clearTransferTaskPolling()
  clearLastTransferTaskId()
  transferDialog.result = null
}

const resetNasControlAuditFiles = () => {
  controlAuditFiles.rows = []
  controlAuditFiles.selectedRows = []
  controlAuditFiles.total = 0
  controlAuditFiles.errorMessage = ''
}

const clearStaleControlAuditTask = () => {
  clearControlAuditPolling()
  clearLastControlAuditTaskId()
  controlAuditDialog.result = null
  resetNasControlAuditFiles()
}

const loadNasControlAuditFilePage = async (taskId = controlAuditDialog.result?.taskId) => {
  if (!taskId) return
  controlAuditFiles.loading = true
  controlAuditFiles.errorMessage = ''
  try {
    const data = await getNasControlAuditFiles(taskId, {
      pageNo: controlAuditFiles.pageNo,
      pageSize: controlAuditFiles.pageSize
    })
    controlAuditFiles.rows = data.list || []
    controlAuditFiles.total = data.total || 0
    controlAuditFiles.selectedRows = []
  } catch (error: any) {
    controlAuditFiles.errorMessage = error?.message || '未受控文件明细加载失败'
  } finally {
    controlAuditFiles.loading = false
  }
}

const applyTransferTaskResult = (result: ControlledFileNasTransferRespVO) => {
  persistLastTransferTaskId(result.taskId)
  transferDialog.sourceType = result.sourceType === 'LOCAL_FOLDER' ? 'LOCAL_FOLDER' : 'NAS'
  transferDialog.result = result
  transferDialog.errorMessage = ''
}

const resolveTransferTaskStatusLabel = (status?: string | null) => {
  if (status === 'UPLOADING') return '上传中'
  if (status === 'RUNNING') return '执行中'
  if (status === 'COMPLETED') return '已完成'
  if (status === 'FAILED') return '已失败'
  return '排队中'
}

const resolveTransferTaskStatusType = (status?: string | null) => {
  if (status === 'UPLOADING') return 'primary'
  if (status === 'RUNNING') return 'primary'
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

const applyControlAuditTaskResult = (result: NasControlAuditTaskRespVO) => {
  persistLastControlAuditTaskId(result.taskId)
  controlAuditDialog.result = result
  controlAuditDialog.errorMessage =
    result.status === 'FAILED' ? result.failureReason || 'NAS 受控状态统计任务失败' : ''
  if (result.status === 'COMPLETED') {
    void loadNasControlAuditFilePage(result.taskId)
  } else {
    resetNasControlAuditFiles()
  }
}

const resolveControlAuditStatusLabel = (status?: string | null) => {
  if (status === 'RUNNING') return '扫描中'
  if (status === 'COMPLETED') return '已完成'
  if (status === 'FAILED') return '已失败'
  return '排队中'
}

const resolveControlAuditStatusType = (status?: string | null) => {
  if (status === 'RUNNING') return 'primary'
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

const resolveNasUncontrolledClassificationLabel = (status?: string | null) => {
  if (status === 'MATCHED') return '已匹配'
  if (status === 'UNCLASSIFIED_PENDING') return '未分类/待处理'
  if (status === 'AMBIGUOUS') return '待确认'
  if (status === 'PENDING_RECOGNITION') return '待识别'
  return status || '未知'
}

const resolveNasUncontrolledClassificationTagType = (status?: string | null) => {
  if (status === 'MATCHED') return 'success'
  if (status === 'UNCLASSIFIED_PENDING') return 'warning'
  if (status === 'AMBIGUOUS') return 'warning'
  return 'info'
}

const resolveNasUncontrolledDownloadStatusLabel = (status?: string | null) => {
  if (status === 'SELECTED') return '已选择'
  if (status === 'LOCAL_WRITTEN') return '已写入'
  if (status === 'LOCAL_WRITE_FAILED') return '写入失败'
  return '未下载'
}

const resolveNasUncontrolledArchiveStatusLabel = (status?: string | null) => {
  if (status === 'ARCHIVED') return '已归档'
  if (status === 'FAILED') return '归档失败'
  if (status === 'NOT_STARTED') return '未开始'
  return status || '未开始'
}

const isNasUncontrolledFileImportSelectable = (row: DccNasControlAuditFileRespVO) =>
  ['MATCHED', 'UNCLASSIFIED_PENDING', 'AMBIGUOUS'].includes(row.classificationStatus || '') &&
  Boolean(row.auditFileId && row.sourceSignature?.trim() && row.expectedLocalRelativePath?.trim()) &&
  row.downloadStatus !== 'LOCAL_WRITTEN' &&
  row.archiveStatus !== 'ARCHIVED'

const handleNasUncontrolledFileSelectionChange = (rows: DccNasControlAuditFileRespVO[]) => {
  controlAuditFiles.selectedRows = rows.filter((row) => isNasUncontrolledFileImportSelectable(row))
}

const handleRecognizeNasUncontrolledFiles = async () => {
  const taskId = controlAuditDialog.result?.taskId
  if (!taskId) return
  controlAuditFiles.recognizing = true
  controlAuditFiles.errorMessage = ''
  try {
    const result = await recognizeNasControlAuditFiles(taskId)
    await loadNasControlAuditFilePage(taskId)
    message.success(
      `识别完成：已匹配 ${result.matchedCount}，未分类/待处理 ${result.unclassifiedPendingCount}，待确认 ${result.ambiguousCount}`
    )
  } catch (error: any) {
    controlAuditFiles.errorMessage = error?.message || '未受控文件识别失败'
  } finally {
    controlAuditFiles.recognizing = false
  }
}

const createNasUncontrolledImportIdempotencyKey = () => {
  if (typeof globalThis.crypto?.randomUUID !== 'function') {
    throw new Error('当前浏览器不支持 crypto.randomUUID，无法创建未受控文件导入任务')
  }
  return globalThis.crypto.randomUUID()
}

const buildNasUncontrolledImportSelectedFiles = () => {
  const selectedRows = controlAuditFiles.selectedRows.filter((row) =>
    isNasUncontrolledFileImportSelectable(row)
  )
  if (!selectedRows.length) {
    throw new Error('请先选择可下载的未受控文件')
  }
  return selectedRows.map((row) => {
    const localRelativePath = row.expectedLocalRelativePath?.trim() || ''
    validateNasUncontrolledLocalRelativePath(localRelativePath)
    return {
      auditFileId: row.auditFileId,
      sourceSignature: row.sourceSignature.trim(),
      localRelativePath
    }
  })
}

const handleDownloadSelectedNasUncontrolledFilesToLocal = async () => {
  const auditTaskId = controlAuditDialog.result?.taskId
  if (!auditTaskId) return
  controlAuditFiles.importing = true
  controlAuditFiles.errorMessage = ''
  try {
    const selectedFiles = buildNasUncontrolledImportSelectedFiles()
    const directoryHandle = await requestNasUncontrolledDirectoryHandle()
    const importTask = await importSelectedNasUncontrolledFiles(auditTaskId, {
      selectionScope: 'EXPLICIT_SELECTED_FILES',
      idempotencyKey: createNasUncontrolledImportIdempotencyKey(),
      selectedFiles
    })

    let latestTask = importTask
    for (const selectedFile of selectedFiles) {
      const blob = await downloadNasUncontrolledImportContent(
        latestTask.taskId,
        selectedFile.auditFileId,
        selectedFile.sourceSignature,
        selectedFile.localRelativePath
      )
      try {
        await writeNasUncontrolledBlobToLocalFile(
          directoryHandle,
          selectedFile.localRelativePath,
          blob
        )
        latestTask = await recordNasUncontrolledImportLocalWriteResult(
          latestTask.taskId,
          selectedFile.auditFileId,
          {
            sourceSignature: selectedFile.sourceSignature,
            localRelativePath: selectedFile.localRelativePath,
            localWriteStatus: 'LOCAL_WRITTEN'
          }
        )
      } catch (error: any) {
        await recordNasUncontrolledImportLocalWriteResult(
          latestTask.taskId,
          selectedFile.auditFileId,
          {
            sourceSignature: selectedFile.sourceSignature,
            localRelativePath: selectedFile.localRelativePath,
            localWriteStatus: 'LOCAL_WRITE_FAILED',
            localWriteErrorCode: 'LOCAL_WRITE_FAILED',
            localWriteError: error?.message || '本地写入失败'
          }
        )
        throw error
      }
    }

    await loadNasControlAuditFilePage(auditTaskId)
    const metadataBlocked = controlAuditFiles.rows.some(
      (row) => row.archiveErrorCode === 'ARCHIVE_METADATA_REQUIRED'
    )
    if (metadataBlocked) {
      message.warning('文件已写入本地，部分文件缺少正式归档元数据，已标记为归档元数据待补齐')
    } else {
      message.success('未受控文件已下载到本地并完成归类回写')
    }
  } catch (error: any) {
    controlAuditFiles.errorMessage =
      error?.name === 'AbortError'
        ? '已取消本地目录选择，未创建未受控文件导入任务'
        : error?.message || '未受控文件下载到本地失败'
  } finally {
    controlAuditFiles.importing = false
  }
}

const handleDownloadControlAuditReport = async (showSuccessMessage = true) => {
  const task = controlAuditDialog.result
  if (!task || task.status !== 'COMPLETED') {
    controlAuditDialog.errorMessage = 'NAS 受控状态统计报告尚未生成，无法下载'
    return false
  }
  controlAuditDialog.downloading = true
  try {
    const blob = await downloadNasControlAuditReport(task.taskId)
    const fileName = task.reportFileName || `nas-control-audit-${task.taskId}.xlsx`
    downloadByData(
      blob,
      fileName,
      blob.type ||
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    )
    if (showSuccessMessage) {
      message.success('NAS 受控状态统计报告已下载')
    }
    return true
  } catch (error: any) {
    controlAuditDialog.errorMessage = error?.message || 'NAS 受控状态统计报告下载失败'
    return false
  } finally {
    controlAuditDialog.downloading = false
  }
}

const scheduleControlAuditPolling = (taskId?: number) => {
  if (!taskId) return
  clearControlAuditPolling()
  controlAuditPollingTimer = window.setTimeout(async () => {
    const previousStatus = controlAuditDialog.result?.status
    try {
      const result = await getNasControlAuditTask(taskId)
      applyControlAuditTaskResult(result)
      if (!isControlAuditTaskActive(result.status)) {
        clearControlAuditPolling()
        if (
          isControlAuditTaskActive(previousStatus) &&
          result.status === 'COMPLETED' &&
          !autoDownloadedControlAuditTaskIds.has(result.taskId)
        ) {
          autoDownloadedControlAuditTaskIds.add(result.taskId)
          if (await handleDownloadControlAuditReport(false)) {
            message.success('NAS 受控状态统计完成，报告已自动下载')
          }
        }
        return
      }
      scheduleControlAuditPolling(result.taskId)
    } catch (error: any) {
      clearControlAuditPolling()
      const errorMessage = error?.message || 'NAS 受控状态统计任务状态获取失败'
      if (
        errorMessage.includes('nas control audit task not found') ||
        errorMessage.includes('NAS 受控统计任务不存在') ||
        errorMessage.includes('最近 NAS 受控统计任务编号无效')
      ) {
        clearStaleControlAuditTask()
        controlAuditDialog.errorMessage = '最近 NAS 受控统计任务已不存在，请重新发起统计'
        message.warning('最近 NAS 受控统计任务已不存在，请重新发起统计')
        return
      }
      controlAuditDialog.errorMessage = errorMessage
    }
  }, 3000)
}

const confirmControlAuditBeforeStart = async () => {
  const messageHtml = `
    <div class="text-[13px] leading-[20px] text-[#374151]">
      <div>即将统计 NAS 共享下固定三个目录：</div>
      <ul class="mt-8px list-disc pl-18px">
        <li>1. QMS documents</li>
        <li>2.DHF</li>
        <li>3.DMR</li>
      </ul>
      <div class="mt-8px text-[12px] text-[#92400e]">
        遇到无权限子目录会跳过该目录及其子树，并在报告“跳过目录”工作表记录。
      </div>
      <div class="mt-4px text-[12px] text-[#4b5563]">扫描会在后台执行，完成后自动下载 Excel 报告。</div>
    </div>
  `
  try {
    await ElMessageBox.confirm(messageHtml, '统计未受控文件确认', {
      confirmButtonText: '确认统计',
      cancelButtonText: '取消',
      type: 'warning',
      modalClass: NAS_TRANSFER_CONFIRM_MODAL_CLASS,
      dangerouslyUseHTMLString: true
    })
    return true
  } catch {
    return false
  }
}

const handleStartControlAudit = async () => {
  if (!canControlAuditPermission.value) return
  const confirmed = await confirmControlAuditBeforeStart()
  if (!confirmed) return
  controlAuditDialog.visible = true
  controlAuditDialog.loading = true
  controlAuditDialog.errorMessage = ''
  try {
    clearControlAuditPolling()
    const result = await startNasControlAudit()
    applyControlAuditTaskResult(result)
    if (isControlAuditTaskActive(result.status)) {
      scheduleControlAuditPolling(result.taskId)
      return
    }
    if (result.status === 'COMPLETED') {
      if (await handleDownloadControlAuditReport(false)) {
        message.success('NAS 受控状态统计完成，报告已自动下载')
      }
    }
  } catch (error: any) {
    controlAuditDialog.errorMessage = error?.message || 'NAS 受控状态统计任务创建失败'
  } finally {
    controlAuditDialog.loading = false
  }
}

const scheduleTransferTaskPolling = (taskId?: number) => {
  if (!taskId) return
  clearTransferTaskPolling()
  transferTaskPollingTimer = window.setTimeout(async () => {
    const previousStatus = transferDialog.result?.status
    try {
      const result = await getNasTransferTaskState(taskId)
      applyTransferTaskResult(result)
      if (!isTransferTaskActive(result.status)) {
        clearTransferTaskPolling()
        if (isTransferTaskActive(previousStatus) && result.status === 'COMPLETED') {
          await ElMessageBox.alert('全部转移结束', '提示', {
            confirmButtonText: '确定',
            type: 'success',
            showClose: false,
            closeOnClickModal: false,
            closeOnPressEscape: false
          })
        }
        if (isTransferTaskActive(previousStatus) && result.status === 'FAILED') {
          transferDialog.errorMessage = result.lastFailureMessage || 'NAS 转移任务失败'
        }
        return
      }
      scheduleTransferTaskPolling(result.taskId)
    } catch (error: any) {
      clearTransferTaskPolling()
      if (isNasTransferTaskNotFoundError(error)) {
        clearStaleNasTransferTask()
        transferDialog.errorMessage = '最近 NAS 转移任务已不存在，请重新发起转移'
        message.warning('最近 NAS 转移任务已不存在，请重新发起转移')
        return
      }
      transferDialog.errorMessage = error?.message || 'NAS 转移任务状态获取失败'
    }
  }, 3000)
}

const confirmTransferBeforeSubmit = async () => {
  const previewPaths = selectedDirectoryPaths.value.slice(0, 5)
  const hiddenCount = selectedDirectoryPaths.value.length - previewPaths.length
  const pathListHtml = previewPaths
    .map((path) => `<li class="leading-[20px]">${escapeHtml(path)}</li>`)
    .join('')
  const overflowHtml =
    hiddenCount > 0
      ? `<div class="mt-6px text-[12px] text-[#6b7280]">其余 ${hiddenCount} 个已选目录已省略显示。</div>`
      : ''
  const messageHtml = `
    <div class="text-[13px] leading-[20px] text-[#374151]">
      <div>即将开始 NAS 转移，本次共选择 <strong>${selectedDirectoryPaths.value.length}</strong> 个根目录。</div>
      <ul class="mt-8px list-disc pl-18px">${pathListHtml}</ul>
      ${overflowHtml}
      <div class="mt-8px text-[12px] text-[#92400e]">
        为避免包含 10000+ 子文件夹或子文件的大目录在确认前卡顿，系统不会预先递归统计整棵子树数量。
      </div>
      <div class="mt-4px text-[12px] text-[#4b5563]">确认后将直接开始后台转移。</div>
    </div>
  `
  try {
    await ElMessageBox.confirm(messageHtml, '开始转移确认', {
      confirmButtonText: '确认开始',
      cancelButtonText: '返回检查',
      type: 'warning',
      modalClass: NAS_TRANSFER_CONFIRM_MODAL_CLASS,
      dangerouslyUseHTMLString: true
    })
    return true
  } catch {
    return false
  }
}

const confirmLocalFolderImportBeforeSubmit = async () => {
  const previewPaths = transferDialog.localFolder.relativePaths.slice(0, 5)
  const hiddenCount = transferDialog.localFolder.relativePaths.length - previewPaths.length
  const pathListHtml = previewPaths
    .map((path) => `<li class="leading-[20px]">${escapeHtml(path)}</li>`)
    .join('')
  const overflowHtml =
    hiddenCount > 0
      ? `<div class="mt-6px text-[12px] text-[#6b7280]">其余 ${hiddenCount} 个文件已省略显示。</div>`
      : ''
  const messageHtml = `
    <div class="text-[13px] leading-[20px] text-[#374151]">
      <div>即将导入本地文件夹 <strong>${escapeHtml(transferDialog.localFolder.rootDirectoryName)}</strong> 到 DCC。</div>
      <div class="mt-4px">本次共选择 <strong>${transferDialog.localFolder.files.length}</strong> 个文件。</div>
      <ul class="mt-8px list-disc pl-18px">${pathListHtml}</ul>
      ${overflowHtml}
      <div class="mt-8px text-[12px] text-[#4b5563]">确认后将按分片上传，断开后可继续同一任务。</div>
      <div class="mt-4px text-[12px] text-[#4b5563]">上传完成后将触发后台导入。</div>
      <div class="mt-4px text-[12px] text-[#4b5563]">本地导入不采集 NAS 权限快照。</div>
    </div>
  `
  try {
    await ElMessageBox.confirm(messageHtml, '开始导入确认', {
      confirmButtonText: '确认导入',
      cancelButtonText: '返回检查',
      type: 'warning',
      modalClass: NAS_TRANSFER_CONFIRM_MODAL_CLASS,
      dangerouslyUseHTMLString: true
    })
    return true
  } catch {
    return false
  }
}

const openTransferDialog = async (sourceType: TransferSourceType) => {
  transferDialog.sourceType = sourceType
  transferDialog.errorMessage = ''
  if (!hasActiveTransferTask.value) {
    transferDialog.result = null
  }
  if (!transferDialog.form.effectiveDate) {
    transferDialog.form.effectiveDate = new Date().toISOString().slice(0, 10)
  }
  try {
    await Promise.all([loadTransferCategoryOptions(), loadTransferProjectCodeOptions()])
    transferDialog.visible = true
    if (hasActiveTransferTask.value) {
      scheduleTransferTaskPolling(transferDialog.result?.taskId)
    }
  } catch (error: any) {
    message.error(error?.message || 'DCC 转移条件加载失败')
  }
}

const handleOpenTransferDialog = async () => {
  if (!selectedDirectoryPaths.value.length) return
  resetLocalFolderSelection()
  await openTransferDialog('NAS')
}

const handleTriggerLocalFolderImport = () => {
  if (
    hasActiveTransferTask.value &&
    !(transferDialog.result?.sourceType === 'LOCAL_FOLDER' && transferDialog.result?.status === 'UPLOADING')
  ) {
    return
  }
  localFolderInputRef.value?.click()
}

const handleLocalFolderSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  if (!files.length) {
    return
  }
  try {
    transferDialog.localFolder = validateLocalFolderFiles(files)
    await openTransferDialog('LOCAL_FOLDER')
  } catch (error: any) {
    resetLocalFolderSelection()
    message.error(error?.message || '本地文件夹导入准备失败')
  }
}

const handleSubmitTransfer = async () => {
  if (transferDialog.sourceType === 'LOCAL_FOLDER') {
    await handleSubmitLocalFolderImport()
    return
  }
  const valid = await transferFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  const confirmed = await confirmTransferBeforeSubmit()
  if (!confirmed) {
    return
  }
  transferDialog.submitting = true
  transferDialog.errorMessage = ''
  try {
    clearTransferTaskPolling()
    const result = await transferNasDirectories({
      selectedNasPaths: [...selectedDirectoryPaths.value],
      templateCategoryId: transferDialog.form.templateCategoryId as number,
      dccProjectCodeId: transferDialog.form.dccProjectCodeId as number,
      productMasterId: null,
      effectiveDate: transferDialog.form.effectiveDate
    })
    applyTransferTaskResult(result)
    message.success('NAS 转移任务已创建')
    if (isTransferTaskActive(transferDialog.result?.status)) {
      scheduleTransferTaskPolling(transferDialog.result?.taskId)
    }
  } catch (error: any) {
    transferDialog.result = null
    transferDialog.errorMessage = error?.message || 'NAS 目录转移失败'
  } finally {
    transferDialog.submitting = false
  }
}

const handleSubmitLocalFolderImport = async () => {
  const valid = await transferFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!transferDialog.localFolder.files.length) {
    transferDialog.errorMessage = '本地文件夹为空，无法导入'
    return
  }
  const confirmed = await confirmLocalFolderImportBeforeSubmit()
  if (!confirmed) {
    return
  }
  transferDialog.submitting = true
  transferDialog.errorMessage = ''
  try {
    clearTransferTaskPolling()
    const { task, uploadState } = await resolveLocalFolderImportSession()
    await uploadLocalFolderImportChunks(task.taskId, uploadState)
    const result = await completeLocalFolderImportSession(task.taskId)
    applyTransferTaskResult(result)
    message.success('本地文件夹导入任务已创建')
    if (isTransferTaskActive(transferDialog.result?.status)) {
      scheduleTransferTaskPolling(transferDialog.result?.taskId)
    }
  } catch (error: any) {
    if (!(transferDialog.result?.sourceType === 'LOCAL_FOLDER' && transferDialog.result?.status === 'UPLOADING')) {
      transferDialog.result = null
    }
    transferDialog.errorMessage = error?.message || '本地文件夹导入失败'
  } finally {
    transferDialog.submitting = false
  }
}

const validateForm = async () => {
  if (!formRef.value) return false
  const fieldsToValidate = ['server', 'share', 'username', 'password']
  if (shouldShowPortField.value) {
    fieldsToValidate.push('port')
  }
  if (typeof formRef.value.clearValidate === 'function') {
    formRef.value.clearValidate()
  }
  if (typeof formRef.value.validateField === 'function') {
    try {
      await formRef.value.validateField(fieldsToValidate)
      return true
    } catch {
      return false
    }
  }
  return await formRef.value.validate()
}

const onSubmit = async () => {
  const valid = await validateForm()
  if (!valid) return
  saveLoading.value = true
  try {
    await saveNasConfig(buildNasConfigPayload())
    message.success(t('common.updateSuccess'))
    await getConfig()
  } finally {
    saveLoading.value = false
  }
}

const handleTest = async () => {
  const valid = await validateForm()
  if (!valid) return
  testLoading.value = true
  testResult.value = null
  try {
    const data = await testNasConfig(buildNasConfigPayload())
    testResult.value = {
      type: 'success',
      title: data.message || 'NAS 连接成功',
      description: `根路径：${data.rootPath}；根目录条目数：${data.itemCount}`
    }
    resetDirectoryView()
    message.success('NAS 测试连接成功')
  } catch (error: any) {
    const errorMessage = error?.message || 'NAS 连接测试失败'
    testResult.value = {
      type: 'error',
      title: 'NAS 连接失败',
      description: errorMessage
    }
    resetDirectoryView()
  } finally {
    testLoading.value = false
  }
}

const handleRefreshDirectory = async () => {
  if (!canRefreshDirectory.value) return
  directoryLoading.value = true
  directoryError.value = ''
  directorySkipped.value = []
  selectedDirectoryPaths.value = []
  try {
    const data = await listNasFiles('')
    const rootDirectories = mapDirectoryItems(data.items || [])
    directoryTree.value = rootDirectories
    directorySummary.value = {
      rootPath: data.rootPath,
      currentPath: data.currentPath || '',
      directoryCount: rootDirectories.length
    }
    directoryTreeKey.value += 1
    message.success('NAS 目录已同步')
  } catch (error: any) {
    resetDirectoryView()
    directoryError.value = error?.message || '刷新目录失败'
  } finally {
    directoryLoading.value = false
  }
}

const loadNasNode = async (node: any, resolve: (data: NasDirectoryNode[]) => void) => {
  if (!node?.data?.path) {
    resolve(directoryTree.value)
    return
  }

  const currentNode = node.data as NasDirectoryNode
  directoryLoading.value = true
  directoryError.value = ''
  try {
    const data = await listNasFiles(currentNode.path)
    const children = mapDirectoryItems(data.items || [])
    currentNode.leaf = children.length === 0
    resolve(children)
  } catch (error: any) {
    const errorMessage = error?.message || `读取目录 ${currentNode.path} 失败`
    currentNode.leaf = true
    appendSkippedDirectory(currentNode.path, errorMessage)
    directoryError.value = errorMessage
    resolve([])
  } finally {
    directoryLoading.value = false
  }
}

const restoreLastTransferTask = async () => {
  try {
    const taskId = readLastTransferTaskId()
    if (!taskId || transferDialog.result) return
    const result = await getNasTransferTaskState(taskId)
    applyTransferTaskResult(result)
    if (isTransferTaskActive(result.status)) {
      transferDialog.visible = true
      scheduleTransferTaskPolling(result.taskId)
      return
    }
    if (result.status === 'FAILED') {
      transferDialog.visible = true
      transferDialog.errorMessage = result.lastFailureMessage || '最近 NAS 转移任务失败'
    }
  } catch (error: any) {
    clearTransferTaskPolling()
    if (isNasTransferTaskNotFoundError(error)) {
      clearStaleNasTransferTask()
      transferDialog.visible = false
      transferDialog.errorMessage = ''
      message.warning('最近 NAS 转移任务已不存在，请重新发起转移')
      return
    }
    transferDialog.visible = true
    transferDialog.errorMessage = error?.message || '最近 NAS 转移任务恢复失败'
  }
}

const restoreLastControlAuditTask = async () => {
  try {
    const taskId = readLastControlAuditTaskId()
    if (!taskId || controlAuditDialog.result) return
    const result = await getNasControlAuditTask(taskId)
    applyControlAuditTaskResult(result)
    if (isControlAuditTaskActive(result.status)) {
      controlAuditDialog.visible = true
      scheduleControlAuditPolling(result.taskId)
      return
    }
    if (result.status === 'FAILED') {
      controlAuditDialog.visible = true
      controlAuditDialog.errorMessage = result.failureReason || '最近 NAS 受控统计任务失败'
      return
    }
    if (result.status === 'COMPLETED') {
      controlAuditDialog.visible = true
    }
  } catch (error: any) {
    clearControlAuditPolling()
    const errorMessage = error?.message || '最近 NAS 受控统计任务恢复失败'
    if (
      errorMessage.includes('nas control audit task not found') ||
      errorMessage.includes('NAS 受控统计任务不存在') ||
      errorMessage.includes('最近 NAS 受控统计任务编号无效')
    ) {
      clearStaleControlAuditTask()
      controlAuditDialog.visible = false
      controlAuditDialog.errorMessage = ''
      message.warning('最近 NAS 受控统计任务已不存在，请重新发起统计')
      return
    }
    controlAuditDialog.visible = true
    controlAuditDialog.errorMessage = errorMessage
  }
}

onMounted(() => {
  getConfig()
  restoreLastTransferTask()
  restoreLastControlAuditTask()
})

onBeforeRouteLeave(() => {
  closeTransferDialogOnRouteLeave()
  return true
})

onUnmounted(() => {
  clearTransferTaskPolling()
  clearControlAuditPolling()
})
</script>

<style scoped>
:global(.nas-transfer-confirm-message-box-overlay) {
  z-index: 3200 !important;
}
</style>
