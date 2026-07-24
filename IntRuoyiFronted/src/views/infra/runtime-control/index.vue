<template>
  <div class="runtime-control-page">
    <div class="runtime-toolbar">
      <div class="runtime-title">
        <div class="runtime-title__main">运行控制台</div>
        <div class="runtime-title__meta">
          <span>Local</span>
          <span>Test</span>
          <span>Production</span>
          <span>Backup</span>
        </div>
      </div>
      <div class="runtime-actions">
        <el-tag :type="connected ? 'success' : 'danger'" effect="light">
          {{ connected ? '连接正常' : '等待重连' }}
        </el-tag>
        <el-button :loading="loading" @click="loadOverview">
          <Icon icon="ep:refresh" class="mr-5px" />
          刷新
        </el-button>
      </div>
    </div>

    <div v-if="lastError" class="runtime-error">
      {{ lastError }}
    </div>

    <div class="ops-toolbar">
      <el-button
        v-for="action in operationActions"
        :key="action.action"
        :type="action.type"
        :plain="action.type !== 'primary'"
        :disabled="!canOperate || operationSubmitting"
        @click="openOperation(action.action)"
      >
        <Icon :icon="action.icon" class="mr-5px" />
        {{ action.label }}
      </el-button>
      <el-button plain type="primary" @click="incidentDrawerVisible = true">
        <Icon icon="ep:warning" class="mr-5px" />
        事故闭环
      </el-button>
      <el-tag v-if="!canOperate" type="warning" effect="light">无运维操作权限</el-tag>
    </div>

    <div class="release-status-panel" v-loading="opsLoading.releaseStatus">
      <div class="release-status-panel__head">
        <div>
          <div class="panel-title">发布状态</div>
          <div class="release-status-panel__meta">
            <span>测试服：{{ releaseStatus?.testCurrentReleaseTag || currentReleaseTagText('test') }}</span>
            <span>已验证：{{ releaseStatus?.latestTestedReleaseTag || '-' }}</span>
            <span>候选包：{{ releaseStatus?.releasePackages?.length || releasePackages.length }}</span>
          </div>
        </div>
        <el-button :loading="opsLoading.releaseStatus" @click="loadReleaseStatus">
          <Icon icon="ep:refresh" class="mr-5px" />
          刷新发布状态
        </el-button>
      </div>
      <table class="release-status-table">
        <thead>
          <tr>
            <th>环境</th>
            <th>当前发布包</th>
            <th>后端</th>
            <th>前端</th>
            <th>最近发布操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="environment in releaseStatusEnvironments" :key="environment">
            <td>{{ environmentLabel(environment) }}</td>
            <td>{{ releaseStatusCurrentTag(environment) }}</td>
            <td>
              <el-tag :type="statusTagType(releaseStatusComponent(environment, 'intruoyi-backend')?.status)">
                {{ statusText(releaseStatusComponent(environment, 'intruoyi-backend')?.status) }}
              </el-tag>
            </td>
            <td>
              <el-tag :type="statusTagType(releaseStatusComponent(environment, 'intruoyi-frontend')?.status)">
                {{ statusText(releaseStatusComponent(environment, 'intruoyi-frontend')?.status) }}
              </el-tag>
            </td>
            <td>{{ releaseStatusOperationText(environment) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="runtime-matrix">
      <table>
        <thead>
          <tr>
            <th class="component-col">组件</th>
            <th v-for="environment in displayEnvironments" :key="environment.key">
              <div class="runtime-env-heading">{{ environment.label }}</div>
              <div v-if="environment.key !== 'local'" class="runtime-current-release">
                当前发布包：{{ currentReleaseTagText(environment.key) }}
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="component in displayComponentRows"
            :key="component.key"
            :data-runtime-component-row="component.key"
          >
            <td class="component-cell">
              <div class="component-name">{{ component.label }}</div>
              <div class="component-key">{{ component.key }}</div>
            </td>
            <td v-for="environment in displayEnvironments" :key="`${environment.key}-${component.key}`">
              <div class="status-block">
                <div class="status-line">
                  <el-tag :type="statusTagType(statusOf(environment.key, component.key)?.status)">
                    {{ statusText(statusOf(environment.key, component.key)?.status) }}
                  </el-tag>
                  <span class="runtime-state">
                    {{ statusOf(environment.key, component.key)?.runtimeState || '-' }}
                  </span>
                </div>
                <div v-if="shouldShowAccessPath(component.key)" class="runtime-path">
                  <span class="runtime-path__label">访问路径：</span>
                  <a
                    v-if="statusOf(environment.key, component.key)?.url"
                    class="runtime-link"
                    :href="statusOf(environment.key, component.key)?.url"
                    target="_blank"
                  >
                    {{ statusOf(environment.key, component.key)?.url }}
                  </a>
                  <span v-else class="runtime-path__value">-</span>
                </div>
                <a
                  v-else-if="statusOf(environment.key, component.key)?.url"
                  class="runtime-link"
                  :href="statusOf(environment.key, component.key)?.url"
                  target="_blank"
                >
                  {{ statusOf(environment.key, component.key)?.url }}
                </a>
                <div class="runtime-http">
                  {{ statusOf(environment.key, component.key)?.httpStatus || '-' }}
                </div>
                <div class="runtime-last">
                  {{ lastOperationText(statusOf(environment.key, component.key)?.lastOperation) }}
                </div>
                <el-tooltip
                  :content="statusOf(environment.key, component.key)?.blockedReason || '重启组件'"
                  placement="top"
                >
                  <el-button
                    link
                    type="primary"
                    :disabled="!canRestart(environment.key, component.key)"
                    v-hasPermi="['infra:runtime-control:restart']"
                    @click="openRestart(environment.key, component.key)"
                  >
                    <Icon icon="ep:switch-button" class="mr-4px" />
                    重启
                  </el-button>
                </el-tooltip>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="ops-grid">
      <OpsProbeStatusPanel
        :latest="probeLatest"
        :loading="opsLoading.probes"
        @run="runProbes"
      />
      <OpsLogDiskRiskPanel :capacity="capacityStatus" :loading="opsLoading.capacity" />
    </div>

    <div class="remote-root-panel">
      <div class="remote-root-panel__head">
        <div>
          <div class="panel-title">远程根分区</div>
          <div class="remote-root-panel__meta">
            <span>targetEnvironment={{ remoteRootTargetEnvironment }}</span>
            <span>{{ remoteRootDiskStatus?.serverHost || selectedRootDiskTarget?.host || '-' }}</span>
            <span>{{ formatRuntimeDate(remoteRootDiskStatus?.sampledAt) }}</span>
          </div>
        </div>
        <div class="remote-root-panel__actions">
          <el-radio-group v-model="remoteRootTargetEnvironment" @change="onRemoteRootTargetChange">
            <el-radio-button
              v-for="item in rootDiskTargetOptions"
              :key="item.value"
              :label="item.value"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
          <el-button :loading="opsLoading.remoteRootDisk" @click="() => loadRemoteRootDiskStatus(true)">
            <Icon icon="ep:refresh" class="mr-5px" />
            刷新根分区
          </el-button>
          <el-button
            plain
            type="danger"
            :disabled="!canOperate"
            :loading="remoteRootCleanupSubmitting"
            @click="openRemoteRootCleanupDialog"
          >
            <Icon icon="ep:delete" class="mr-5px" />
            清理临时目录
          </el-button>
        </div>
      </div>
      <div class="remote-root-grid" v-loading="opsLoading.remoteRootDisk">
        <div class="remote-root-metric">
          <span>可用空间</span>
          <strong>{{ bytesText(remoteRootDiskStatus?.availableBytes) }}</strong>
        </div>
        <div class="remote-root-metric">
          <span>使用率</span>
          <strong>{{ percentText(remoteRootDiskStatus?.usagePercent) }}</strong>
        </div>
        <div class="remote-root-metric">
          <span>inode 使用率</span>
          <strong>{{ percentText(remoteRootDiskStatus?.inodeUsagePercent) }}</strong>
        </div>
        <div class="remote-root-metric">
          <span>备份临时目录</span>
          <strong>{{ bytesText(remoteRootDiskStatus?.backupTempBytes) }}</strong>
        </div>
        <div class="remote-root-metric">
          <span>/tmp</span>
          <strong>{{ bytesText(remoteRootDiskStatus?.tmpBytes) }}</strong>
        </div>
      </div>
      <div class="remote-root-paths">
        <span>/opt/intruoyi/ops/backup/tmp</span>
        <span>/tmp</span>
      </div>
      <div v-if="remoteRootCleanupResult" class="remote-root-result">
        上次清理：删除 {{ remoteRootCleanupResult.deletedEntryCount ?? 0 }} 个顶层条目，清理后可用
        {{ bytesText(remoteRootCleanupResult.after?.availableBytes) }}
      </div>
    </div>

    <div class="backup-summary-panel">
      <div class="panel-title">备份策略</div>
      <div class="backup-summary-grid">
        <div class="backup-summary-item">
          <span class="backup-summary-item__label">当前模式</span>
          <strong>{{ backupModeText(latestBackupPoint) }}</strong>
        </div>
        <div class="backup-summary-item">
          <span class="backup-summary-item__label">DCC 链状态</span>
          <strong>{{ latestBackupPoint?.dccChainStatus || '-' }}</strong>
        </div>
        <div class="backup-summary-item">
          <span class="backup-summary-item__label">当前保留策略</span>
          <strong>{{ retentionPolicyText }}</strong>
        </div>
        <div class="backup-summary-item">
          <span class="backup-summary-item__label">最近备份点</span>
          <strong>{{ latestBackupPoint?.backupId || '-' }}</strong>
        </div>
        <div class="backup-summary-item">
          <span class="backup-summary-item__label">当前 imageTag</span>
          <strong>{{ latestBackupPoint?.imageTag || '-' }}</strong>
        </div>
      </div>
      <el-table :data="backupPoints.slice(0, 5)" height="240" empty-text="暂无备份点">
        <el-table-column label="备份编号" prop="backupId" min-width="150" />
        <el-table-column label="模式" width="150">
          <template #default="{ row }">{{ backupModeText(row) }}</template>
        </el-table-column>
        <el-table-column label="DCC 链" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.dccChainStatus)">{{ row.dccChainStatus || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="新增" width="90">
          <template #default="{ row }">{{ row.objectAddedCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="修改" width="90">
          <template #default="{ row }">{{ row.objectModifiedCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="删除" width="90">
          <template #default="{ row }">{{ row.objectDeletedCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="复用" width="90">
          <template #default="{ row }">{{ row.objectReusedCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="演练" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.rehearsalStatus)">{{ rehearsalStatusText(row.rehearsalStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.recoverabilityStatus)">{{ row.recoverabilityStatus || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="不可恢复原因" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ unrecoverableReasonText(row) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <div class="operation-panel">
      <div class="panel-title">最近操作</div>
      <el-table :data="operations" height="240" empty-text="暂无操作记录">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">
            {{ operationRequestedAtText(row) }}
          </template>
        </el-table-column>
        <el-table-column label="环境" prop="environment" width="90" />
        <el-table-column label="动作" min-width="140">
          <template #default="{ row }">
            {{ operationActionText(row) }}
          </template>
        </el-table-column>
        <el-table-column label="范围" width="100">
          <template #default="{ row }">
            {{ operationPublishScopeText(row) }}
          </template>
        </el-table-column>
        <el-table-column label="对象" min-width="130">
          <template #default="{ row }">
            {{ operationTargetText(row) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="原因" prop="reason" min-width="180" show-overflow-tooltip />
        <el-table-column label="摘要" prop="summary" min-width="180" show-overflow-tooltip />
        <el-table-column label="日志" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.resultLogPath" @click="openLog(row)">
              查看日志
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="restartDialog.visible" title="确认重启" width="460px">
      <el-form label-width="92px">
        <el-form-item label="环境">
          <el-input :model-value="environmentLabel(restartDialog.environment)" disabled />
        </el-form-item>
        <el-form-item label="组件">
          <el-input :model-value="componentLabel(restartDialog.component)" disabled />
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input
            v-model="restartDialog.reason"
            type="textarea"
            :rows="3"
            maxlength="120"
            show-word-limit
          />
        </el-form-item>
        <el-form-item v-if="operationEnvironmentRequiresProdConfirm(restartDialog.environment)" label="生产确认" required>
          <el-input v-model="restartDialog.prodConfirmText" placeholder="输入 PROD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="restartDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="restarting" @click="submitRestart">确认重启</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="operationDialog.visible" :title="operationDialog.title" width="620px">
      <el-form label-width="100px">
        <el-form-item label="动作">
          <el-input :model-value="operationDialog.label" disabled />
        </el-form-item>
        <el-form-item
          v-if="operationSupportsTargetEnvironment(operationDialog.action)"
          :label="operationTargetEnvironmentLabel(operationDialog.action)"
          required
        >
          <el-radio-group v-model="operationDialog.targetEnvironment">
            <el-radio-button
              v-for="item in operationTargetEnvironmentOptions(operationDialog.action)"
              :key="item.value"
              :label="item.value"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-else-if="operationEnvironmentText(operationDialog.action)" label="目标环境">
          <el-input :model-value="operationEnvironmentText(operationDialog.action)" disabled />
        </el-form-item>
        <el-form-item v-if="operationSourceDirectoryText(operationDialog.action)" label="来源目录">
          <el-input :model-value="operationSourceDirectoryText(operationDialog.action)" disabled />
        </el-form-item>
        <el-form-item v-if="operationTargetDirectoryText(operationDialog.action)" label="目标目录">
          <el-input :model-value="operationTargetDirectoryText(operationDialog.action)" disabled />
        </el-form-item>
        <el-form-item v-if="operationDialog.action === 'apply-test-db-sql'" label="SQL 文件" required>
          <el-input
            v-model="operationDialog.sqlPath"
            placeholder="输入本机 SQL 文件绝对路径"
          />
        </el-form-item>
        <el-form-item v-if="operationSupportsPublishScope(operationDialog.action)" label="发布范围" required>
          <div class="publish-scope-field">
            <el-radio-group v-model="operationDialog.publishScope">
              <el-radio-button label="code-only">只发代码</el-radio-button>
              <el-radio-button label="with-data">带数据发布</el-radio-button>
            </el-radio-group>
            <el-checkbox v-model="operationDialog.includeOnlyOffice">发布 OnlyOffice</el-checkbox>
            <el-checkbox
              v-model="operationDialog.includeShowroomBuildPackage"
              @change="handleShowroomBuildPackageChange"
            >
              发布展厅构筑包
            </el-checkbox>
            <div v-if="operationDialog.publishScope === 'with-data'" class="publish-scope-hint">
              带数据发布会覆盖目标环境数据库和文件对象
            </div>
          </div>
        </el-form-item>
        <el-form-item v-if="operationSupportsSmartReleaseReport(operationDialog.action)" label="Smart Release">
          <div class="smart-release-field">
            <el-checkbox v-model="operationDialog.enableSmartReleaseReport">report-only 报告/预检</el-checkbox>
            <el-button :loading="operationPreview.loading" @click="previewOperationCommand">
              <Icon icon="ep:view" class="mr-5px" />
              预览命令
            </el-button>
          </div>
        </el-form-item>
        <el-form-item v-if="operationUsesReleaseTag(operationDialog.action)" label="发布包" required>
          <el-select
            v-if="operationUsesReleaseTagSelector(operationDialog.action)"
            v-model="operationDialog.releaseTag"
            class="w-full"
            clearable
            filterable
            :loading="opsLoading.releasePackages"
            :placeholder="operationRequiresTestedReleasePackage(operationDialog.action) ? '选择已测试通过发布包' : '选择 ReleasePackage 发布包'"
          >
            <el-option
              v-for="item in selectableReleasePackages"
              :key="item.releaseTag"
              :label="item.releaseTag"
              :value="item.releaseTag"
            >
              <div
                class="release-package-option"
                :class="releasePackageUsageClass(item.releaseTag)"
              >
                <span class="release-package-option__name">{{ item.releaseTag }}</span>
                <span v-if="releasePackageUsageText(item.releaseTag)" class="release-package-option__status">
                  {{ releasePackageUsageText(item.releaseTag) }}
                </span>
                <span class="release-package-option__status">
                  {{ releasePackageOnlyOfficeText(item) }}
                </span>
                <span class="release-package-option__status">
                  {{ releasePackageShowroomText(item) }}
                </span>
              </div>
              <div v-if="item.manifestPath" class="release-package-option__path">
                {{ item.manifestPath }}
              </div>
            </el-option>
          </el-select>
          <el-input
            v-else
            v-model="operationDialog.releaseTag"
            placeholder="输入 NAS 发布包编号，例如 26-05-29 10:30:00"
          />
        </el-form-item>
        <el-form-item
          v-if="operationUsesCurrentTestReleaseTag(operationDialog.action)"
          label="当前测试服发布包"
          required
        >
          <el-input :model-value="testCurrentReleaseTag || '无'" disabled />
        </el-form-item>
        <el-form-item v-if="operationDialog.action === 'mark-release-tested'" label="验证结论" required>
          <el-input
            v-model="operationDialog.testConclusion"
            type="textarea"
            :rows="2"
            maxlength="120"
            show-word-limit
          />
        </el-form-item>
        <el-form-item
          v-if="operationDialog.action === 'mark-release-tested'"
          label="恢复集候选"
          required
        >
          <OpsCandidatePicker
            v-model="operationDialog.selectedRecoverySetCandidateId"
            mode="restore"
            :restore-candidates="restoreCandidates"
          />
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input
            v-model="operationDialog.reason"
            type="textarea"
            :rows="3"
            maxlength="160"
            show-word-limit
          />
        </el-form-item>
        <el-form-item v-if="operationDialog.action === 'rollback-app'" label="版本候选" required>
          <OpsCandidatePicker
            v-model="operationDialog.selectedImageCandidateId"
            mode="rollback"
            :rollback-candidates="rollbackCandidates"
          />
        </el-form-item>
        <el-form-item
          v-if="['rehearsal', 'restore-data'].includes(operationDialog.action)"
          label="恢复集候选"
          required
        >
          <OpsCandidatePicker
            v-model="operationDialog.selectedRecoverySetCandidateId"
            mode="restore"
            :restore-candidates="restoreCandidates"
          />
        </el-form-item>
        <el-form-item v-if="operationRequiresOwner(operationDialog.action)" label="责任人" required>
          <el-input :model-value="operationRequiredOwnerText || '未配置'" disabled />
        </el-form-item>
        <el-form-item v-if="operationRequiresProd(operationDialog.action)" label="生产确认" required>
          <el-input v-model="operationDialog.prodConfirmText" placeholder="输入 PROD" />
        </el-form-item>
        <el-form-item v-if="operationExpectedResultText(operationDialog.action)" label="预期结果">
          <div class="operation-expected-result">
            {{ operationExpectedResultText(operationDialog.action) }}
          </div>
        </el-form-item>
        <el-form-item v-if="operationPreview.content" label="命令预览">
          <pre class="operation-command-preview">{{ operationPreview.content }}</pre>
        </el-form-item>
      </el-form>
      <el-alert
        v-if="operationBlockReason"
        class="operation-block-alert"
        type="error"
        :closable="false"
        :title="operationBlockReason"
      />
      <template #footer>
        <el-button @click="operationDialog.visible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="Boolean(operationBlockReason)"
          :loading="operationSubmitting"
          @click="submitOperation"
        >
          确认执行
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="remoteRootCleanupDialog.visible" title="清理远程根分区临时目录" width="520px">
      <el-form label-width="100px">
        <el-form-item label="目标环境">
          <el-input :model-value="rootDiskTargetText(remoteRootTargetEnvironment)" disabled />
        </el-form-item>
        <el-form-item label="固定 IP">
          <el-input :model-value="selectedRootDiskTarget?.host || ''" disabled />
        </el-form-item>
        <el-form-item label="清理目录">
          <div class="remote-root-paths">
            <span>/opt/intruoyi/ops/backup/tmp</span>
            <span>/tmp</span>
          </div>
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input
            v-model="remoteRootCleanupDialog.reason"
            type="textarea"
            :rows="3"
            maxlength="160"
            show-word-limit
          />
        </el-form-item>
        <el-form-item
          v-if="remoteRootCleanupRequiresProdConfirm(remoteRootTargetEnvironment)"
          label="高危确认"
          required
        >
          <el-input v-model="remoteRootCleanupDialog.prodConfirmText" placeholder="输入 PROD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="remoteRootCleanupDialog.visible = false">取消</el-button>
        <el-button type="danger" :loading="remoteRootCleanupSubmitting" @click="submitRemoteRootCleanup">
          确认清理
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logDialog.visible" :title="logDialog.title" width="820px" class="runtime-log-dialog">
      <div class="log-toolbar">
        <el-tag :type="statusTagType(logDialog.status)">{{ statusText(logDialog.status) }}</el-tag>
        <el-tag v-if="logDialog.truncated" type="warning" effect="light">已截取尾部</el-tag>
        <el-button :loading="logDialog.loading" @click="loadOperationLog">
          <Icon icon="ep:refresh" class="mr-5px" />
          刷新
        </el-button>
      </div>
      <pre class="log-content">{{ logDialog.content || '暂无日志内容' }}</pre>
    </el-dialog>

    <OpsIncidentDrawer
      v-model:visible="incidentDrawerVisible"
      :page="incidentPage"
      :loading="opsLoading.incidents"
      :submitting="incidentSubmitting"
      @refresh="loadIncidentsPage"
      @create="createIncident"
      @record="recordIncidentAction"
      @close="closeIncident"
    />
  </div>
</template>

<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as RuntimeControlApi from '@/api/infra/runtimeControl'
import type {
  RuntimeControlOperationVO,
  RuntimeControlStatusVO
} from '@/api/infra/runtimeControl'
import { formatDate } from '@/utils/formatTime'
import { checkPermi } from '@/utils/permission'
import { useMessage } from '@/hooks/web/useMessage'
import OpsCandidatePicker from './components/OpsCandidatePicker.vue'
import OpsIncidentDrawer from './components/OpsIncidentDrawer.vue'
import OpsLogDiskRiskPanel from './components/OpsLogDiskRiskPanel.vue'
import OpsProbeStatusPanel from './components/OpsProbeStatusPanel.vue'
import { bytesText, formatRuntimeDate, percentText } from './components/shared'

type OperationPublishScope = RuntimeControlApi.RuntimeControlPublishScope
type OperationTargetEnvironment = RuntimeControlApi.RuntimeControlTargetEnvironment
type RootDiskTargetEnvironment = RuntimeControlApi.RuntimeControlRootDiskTargetEnvironment

defineOptions({ name: 'InfraRuntimeControl' })

const message = useMessage()
const loading = ref(false)
const restarting = ref(false)
const operationSubmitting = ref(false)
const incidentSubmitting = ref(false)
const connected = ref(true)
const lastError = ref('')
const overview = ref<RuntimeControlApi.RuntimeControlOverviewVO>()
const operations = ref<RuntimeControlOperationVO[]>([])
const incidentPage = ref<PageResult<RuntimeControlApi.RuntimeControlIncidentVO[]>>({ list: [], total: 0 })
const ownerMatrix = ref<RuntimeControlApi.RuntimeControlOwnerMatrixVO[]>([])
const rollbackCandidates = ref<RuntimeControlApi.RuntimeControlRollbackCandidateVO[]>([])
const restoreCandidates = ref<RuntimeControlApi.RuntimeControlRestoreCandidateVO[]>([])
const releasePackages = ref<RuntimeControlApi.RuntimeControlReleasePackageVO[]>([])
const releaseStatus = ref<RuntimeControlApi.RuntimeControlReleaseStatusVO>()
const backupPoints = ref<RuntimeControlApi.RuntimeControlBackupPointVO[]>([])
const probeLatest = ref<RuntimeControlApi.RuntimeControlProbeLatestVO>()
const capacityStatus = ref<RuntimeControlApi.RuntimeControlCapacityStatusVO>()
const remoteRootTargetEnvironment = ref<RootDiskTargetEnvironment>('test')
const remoteRootDiskStatus = ref<RuntimeControlApi.RuntimeControlRemoteRootDiskStatusVO>()
const remoteRootCleanupResult = ref<RuntimeControlApi.RuntimeControlRemoteRootCleanupVO>()
const incidentDrawerVisible = ref(false)
const remoteRootCleanupSubmitting = ref(false)
let pollingTimer: number | undefined
let logPollingTimer: number | undefined

const opsLoading = reactive({
  ownerMatrix: false,
  candidates: false,
  releaseStatus: false,
  releasePackages: false,
  backupPoints: false,
  probes: false,
  capacity: false,
  remoteRootDisk: false,
  incidents: false
})

const displayEnvironments = [
  { key: 'local', label: 'Local' },
  { key: 'test', label: 'Test' },
  { key: 'prod', label: 'Production' },
  { key: 'backup', label: 'Backup' }
]

const displayComponentRows = [
  { key: 'intruoyi-frontend', label: 'IntRuoyi 前端' },
  { key: 'intruoyi-backend', label: 'IntRuoyi 后端' },
  { key: 'website-frontend', label: 'Website 前端' }
]

const releaseStatusEnvironments = ['test', 'prod', 'backup']

const operationActions = [
  { action: 'build-release', label: '构建发布包', icon: 'ep:box', type: 'primary' },
  { action: 'publish-test', label: '部署发布包到测试服', icon: 'ep:upload', type: 'primary' },
  { action: 'apply-test-db-sql', label: '测试服数据库快应用', icon: 'ep:coin', type: 'warning' },
  { action: 'mark-release-tested', label: '标记测试通过', icon: 'ep:circle-check', type: 'success' },
  { action: 'promote-prod', label: '上线已验证发布包', icon: 'ep:promotion', type: 'warning' },
  { action: 'promote-backup', label: '上线备份服务器', icon: 'ep:connection', type: 'warning' },
  { action: 'backup-now', label: '立即备份', icon: 'ep:folder-checked', type: 'success' },
  { action: 'rehearsal', label: '恢复演练', icon: 'ep:video-play', type: 'warning' },
  { action: 'rollback-app', label: '回滚版本', icon: 'ep:refresh-left', type: 'warning' },
  { action: 'restore-data', label: '恢复数据', icon: 'ep:warning', type: 'danger' }
]

const RELEASE_PACKAGE_ROOT = 'Backup/ReleasePackage'
const BACKUP_PACKAGE_ROOT = 'Backup/BackupPackage'
const DEFAULT_BUILD_RELEASE_REASON = '默认发布'
const DEFAULT_PUBLISH_TEST_REASON = '默认备份'
const DEFAULT_APPLY_TEST_DB_SQL_REASON = '测试服数据库 SQL 快应用'
const DEFAULT_PROMOTE_PROD_REASON = '默认发布'
const DEFAULT_PROMOTE_BACKUP_REASON = '默认发布'
const backupTargetEnvironmentOptions = [
  { label: '测试服', value: 'test' }
] satisfies Array<{ label: string; value: OperationTargetEnvironment }>
const restoreTargetEnvironmentOptions = [
  { label: '测试服', value: 'test' },
  { label: '备份服务器', value: 'backup' }
] satisfies Array<{ label: string; value: OperationTargetEnvironment }>
const rollbackTargetEnvironmentOptions = [
  { label: '测试服', value: 'test' },
  { label: '备份服务器', value: 'backup' }
] satisfies Array<{ label: string; value: OperationTargetEnvironment }>
const rootDiskTargetOptions = [
  { label: '测试服', value: 'test', host: '172.30.30.58' },
  { label: '正式服', value: 'prod', host: '172.30.30.57' },
  { label: '备份服务器', value: 'backup', host: '172.30.30.59' }
] satisfies Array<{ label: string; value: RootDiskTargetEnvironment; host: string }>

const padDatePart = (value: number) => String(value).padStart(2, '0')

const formatDefaultReleaseTag = (date = new Date()) => {
  const year = String(date.getFullYear()).slice(-2)
  const month = padDatePart(date.getMonth() + 1)
  const day = padDatePart(date.getDate())
  const hour = padDatePart(date.getHours())
  const minute = padDatePart(date.getMinutes())
  const second = padDatePart(date.getSeconds())
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

const restartDialog = reactive({
  visible: false,
  environment: '',
  component: '',
  reason: '',
  prodConfirmText: ''
})

const operationDialog = reactive<{
  visible: boolean
  title: string
  action: string
  label: string
  publishScope: OperationPublishScope
  includeOnlyOffice: boolean
  includeShowroomBuildPackage: boolean
  enableSmartReleaseReport: boolean
  targetEnvironment: OperationTargetEnvironment
  releaseTag: string
  testConclusion: string
  sqlPath: string
  reason: string
  prodConfirmText: string
  selectedImageCandidateId: string
  selectedRecoverySetCandidateId: string
}>({
  visible: false,
  title: '',
  action: '',
  label: '',
  publishScope: 'code-only',
  includeOnlyOffice: false,
  includeShowroomBuildPackage: false,
  enableSmartReleaseReport: false,
  targetEnvironment: 'test',
  releaseTag: '',
  testConclusion: '',
  sqlPath: '',
  reason: '',
  prodConfirmText: '',
  selectedImageCandidateId: '',
  selectedRecoverySetCandidateId: ''
})

const logDialog = reactive({
  visible: false,
  title: '查看日志',
  operationId: '',
  status: '',
  content: '',
  truncated: false,
  loading: false
})

const operationPreview = reactive({
  loading: false,
  content: ''
})

const remoteRootCleanupDialog = reactive({
  visible: false,
  reason: '',
  prodConfirmText: ''
})

const canOperate = computed(() => checkPermi(['infra:runtime-control:operate']))
const selectedRootDiskTarget = computed(() =>
  rootDiskTargetOptions.find((item) => item.value === remoteRootTargetEnvironment.value)
)

const operationBlockReason = computed(() => {
  return (
    operationOwnerBlockReason.value ||
    operationReleasePackageBlockReason.value ||
    candidateBlockReason(operationDialog.action)
  )
})

const testCurrentReleaseTag = computed(() => currentReleaseTagValue('test'))

const testUsedReleaseTags = computed(() => {
  return new Set(
    operations.value
      .filter(
        (operation) =>
          operation.action === 'publish-test' &&
          operation.environment === 'test' &&
          operation.status === 'succeeded'
      )
      .map((operation) => operation.parameters?.releaseTag?.trim())
      .filter((releaseTag): releaseTag is string => Boolean(releaseTag))
  )
})

const testedReleaseTags = computed(() => {
  return new Set(
    releasePackages.value
      .filter((item) => item.tested)
      .map((item) => item.releaseTag?.trim())
      .filter((releaseTag): releaseTag is string => Boolean(releaseTag))
  )
})

const selectableReleasePackages = computed(() => {
  const availablePackages = releasePackages.value.filter(
    (item) => item.status !== 'BLOCKED' && item.checksumPresent !== false
  )
  if (!operationRequiresTestedReleasePackage(operationDialog.action)) {
    return availablePackages
  }
  return availablePackages.filter((item) => item.tested)
})

const operationRequiredOwnerText = computed(() => {
  if (!operationRequiresOwner(operationDialog.action)) {
    return ''
  }
  const owner = ownerMatrix.value.find(
    (row) =>
      row.environment === operationEnvironmentKey(operationDialog.action) &&
      row.action === operationDialog.action &&
      row.role === operationOwnerRole(operationDialog.action) &&
      row.required
  )
  if (!owner) {
    return ''
  }
  return owner.ownerName || (owner.ownerUserId ? String(owner.ownerUserId) : '')
})

const operationOwnerBlockReason = computed(() => {
  if (!operationRequiresOwner(operationDialog.action)) {
    return ''
  }
  return operationRequiredOwnerText.value
    ? ''
    : `缺少${operationOwnerRoleText(operationDialog.action)}，禁止提交`
})

const operationReleasePackageBlockReason = computed(() => {
  if (operationDialog.action === 'publish-test' && !releasePackages.value.length) {
    return `${RELEASE_PACKAGE_ROOT} 中没有可部署发布包`
  }
  if (operationDialog.action === 'promote-prod' && !selectableReleasePackages.value.length) {
    return `${RELEASE_PACKAGE_ROOT} 中没有已测试通过的发布包`
  }
  if (operationDialog.action === 'promote-backup' && !selectableReleasePackages.value.length) {
    return `${RELEASE_PACKAGE_ROOT} 中没有已测试通过的发布包`
  }
  return ''
})

const loadOverview = async () => {
  if (loading.value) {
    return
  }
  loading.value = true
  const errors: string[] = []
  let foolproofLoadFailed = false
  try {
    await Promise.all([
      RuntimeControlApi.getRuntimeControlOverview()
        .then((overviewResp) => {
          overview.value = overviewResp
        })
        .catch((error) => {
          errors.push(`运维矩阵：${errorMessage(error)}`)
        }),
      RuntimeControlApi.getRuntimeControlOperations()
        .then((operationsResp) => {
          operations.value = operationsResp
        })
        .catch((error) => {
          errors.push(`最近操作：${errorMessage(error)}`)
        }),
      loadFoolproofData().catch(() => {
        foolproofLoadFailed = true
      })
    ])
    connected.value = errors.length === 0 && !foolproofLoadFailed
    lastError.value = errors.join('；')
  } finally {
    loading.value = false
  }
}

const loadOverviewMatrix = async () => {
  overview.value = await RuntimeControlApi.getRuntimeControlOverview()
}

const loadFoolproofData = async () => {
  await Promise.all([
    loadOwnerMatrix(),
    loadCandidates(),
    loadBackupPoints(),
    loadReleaseStatus(),
    loadLatestProbes(),
    loadCapacityStatus(),
    loadRemoteRootDiskStatus(),
    loadIncidentsPage()
  ])
}

const loadOwnerMatrix = async () => {
  opsLoading.ownerMatrix = true
  try {
    ownerMatrix.value = await RuntimeControlApi.getRuntimeControlOwnerMatrix()
  } finally {
    opsLoading.ownerMatrix = false
  }
}

const loadCandidates = async () => {
  opsLoading.candidates = true
  const errors: string[] = []
  try {
    restoreCandidates.value = await RuntimeControlApi.getRuntimeControlRestoreCandidates()
  } catch (error) {
    restoreCandidates.value = []
    errors.push(`恢复集候选：${errorMessage(error)}`)
  }
  try {
    rollbackCandidates.value = await RuntimeControlApi.getRuntimeControlRollbackCandidates()
  } catch (error) {
    rollbackCandidates.value = []
    errors.push(`回滚镜像候选：${errorMessage(error)}`)
  } finally {
    opsLoading.candidates = false
  }
  if (errors.length) {
    throw new Error(errors.join('；'))
  }
}

const loadBackupPoints = async () => {
  opsLoading.backupPoints = true
  try {
    backupPoints.value = await RuntimeControlApi.getRuntimeControlBackupPoints()
  } finally {
    opsLoading.backupPoints = false
  }
}

const loadReleaseStatus = async () => {
  opsLoading.releaseStatus = true
  try {
    releaseStatus.value = await RuntimeControlApi.getRuntimeControlReleaseStatus()
    releasePackages.value = releaseStatus.value.releasePackages || []
  } finally {
    opsLoading.releaseStatus = false
  }
}

const latestBackupPoint = computed(() => backupPoints.value[0])

const retentionPolicyText = computed(() => {
  const latest = latestBackupPoint.value
  if (!latest) return '-'
  const parts: string[] = []
  if (latest.retentionKeepLast != null) parts.push(`最近 ${latest.retentionKeepLast} 个`)
  if (latest.retentionKeepDays != null) parts.push(`${latest.retentionKeepDays} 天`)
  if (latest.retentionMaxNasUsedPercent != null) parts.push(`NAS <= ${latest.retentionMaxNasUsedPercent}%`)
  return parts.length ? parts.join(' / ') : '-'
})

const backupModeText = (item?: RuntimeControlApi.RuntimeControlBackupPointVO) => {
  if (!item) return '-'
  if (item.dccBackupMode) return item.dccBackupMode
  return item.backupMode || '-'
}

const rehearsalStatusText = (status?: string) => {
  if (!status) return '-'
  if (status === 'not-run') return '未演练'
  return status
}

const unrecoverableReasonText = (item: RuntimeControlApi.RuntimeControlBackupPointVO) => {
  const reasons = item.unrecoverableReasons || []
  return reasons.length ? reasons.join('；') : '-'
}

const loadReleasePackages = async () => {
  opsLoading.releasePackages = true
  try {
    releasePackages.value = await RuntimeControlApi.getRuntimeControlReleasePackages()
  } finally {
    opsLoading.releasePackages = false
  }
}

const loadLatestProbes = async () => {
  opsLoading.probes = true
  try {
    probeLatest.value = await RuntimeControlApi.getRuntimeControlLatestProbes()
  } finally {
    opsLoading.probes = false
  }
}

const loadCapacityStatus = async () => {
  opsLoading.capacity = true
  try {
    capacityStatus.value = await RuntimeControlApi.getRuntimeControlCapacityStatus()
  } finally {
    opsLoading.capacity = false
  }
}

const loadRemoteRootDiskStatus = async (showError = false) => {
  opsLoading.remoteRootDisk = true
  try {
    const status = await RuntimeControlApi.getRuntimeControlRemoteRootDiskStatus(
      remoteRootTargetEnvironment.value
    )
    assertRemoteRootStatusProof(status)
    remoteRootDiskStatus.value = status
  } catch (error) {
    if (showError) {
      reportActionError(error)
    }
    throw error
  } finally {
    opsLoading.remoteRootDisk = false
  }
}

const onRemoteRootTargetChange = () => {
  remoteRootDiskStatus.value = undefined
  remoteRootCleanupResult.value = undefined
  remoteRootCleanupDialog.visible = false
  remoteRootCleanupDialog.prodConfirmText = ''
}

const openRemoteRootCleanupDialog = () => {
  remoteRootCleanupDialog.reason = `运行控制台清理${rootDiskTargetText(remoteRootTargetEnvironment.value)}根分区临时目录`
  remoteRootCleanupDialog.prodConfirmText = ''
  remoteRootCleanupDialog.visible = true
}

const submitRemoteRootCleanup = async () => {
  const reason = remoteRootCleanupDialog.reason.trim()
  if (!reason) {
    message.warning('请填写清理原因')
    return
  }
  if (
    remoteRootCleanupRequiresProdConfirm(remoteRootTargetEnvironment.value) &&
    remoteRootCleanupDialog.prodConfirmText !== 'PROD'
  ) {
    message.warning('正式服/备用服务器清理必须输入 PROD')
    return
  }
  remoteRootCleanupSubmitting.value = true
  try {
    const result = await RuntimeControlApi.cleanupRemoteRootTemporaryFiles({
      targetEnvironment: remoteRootTargetEnvironment.value,
      reason,
      prodConfirmText: remoteRootCleanupDialog.prodConfirmText
    })
    assertRemoteRootCleanupProof(result)
    remoteRootCleanupResult.value = result
    remoteRootDiskStatus.value = result.after || result.before
    remoteRootCleanupDialog.visible = false
    message.success('远程临时目录清理已完成')
  } catch (error) {
    reportActionError(error)
  } finally {
    remoteRootCleanupSubmitting.value = false
  }
}

const loadIncidentsPage = async () => {
  opsLoading.incidents = true
  try {
    incidentPage.value = await RuntimeControlApi.getRuntimeControlIncidentsPage({
      pageNo: 1,
      pageSize: 20
    })
  } finally {
    opsLoading.incidents = false
  }
}

const runProbes = async () => {
  opsLoading.probes = true
  try {
    probeLatest.value = await RuntimeControlApi.runRuntimeControlProbes()
  } catch (error) {
    reportActionError(error)
  } finally {
    opsLoading.probes = false
  }
}

const createIncident = async (data: RuntimeControlApi.RuntimeControlIncidentCreateReqVO) => {
  incidentSubmitting.value = true
  try {
    await RuntimeControlApi.createRuntimeControlIncident(data)
    message.success('事故已创建')
    await loadIncidentsPage()
  } catch (error) {
    reportActionError(error)
  } finally {
    incidentSubmitting.value = false
  }
}

const recordIncidentAction = async (payload: {
  id: number
  data: RuntimeControlApi.RuntimeControlIncidentActionReqVO
}) => {
  incidentSubmitting.value = true
  try {
    await RuntimeControlApi.recordRuntimeControlIncidentAction(payload.id, payload.data)
    message.success('处置动作已记录')
    await loadIncidentsPage()
  } catch (error) {
    reportActionError(error)
  } finally {
    incidentSubmitting.value = false
  }
}

const closeIncident = async (payload: {
  id: number
  data: RuntimeControlApi.RuntimeControlIncidentCloseReqVO
}) => {
  incidentSubmitting.value = true
  try {
    await RuntimeControlApi.closeRuntimeControlIncident(payload.id, payload.data)
    message.success('事故已关闭')
    await loadIncidentsPage()
  } catch (error) {
    reportActionError(error)
  } finally {
    incidentSubmitting.value = false
  }
}

const statusOf = (environment: string, component: string): RuntimeControlStatusVO | undefined => {
  return overview.value?.statuses?.[environment]?.[component]
}

const environmentLabel = (environment: string) => {
  return displayEnvironments.find((item) => item.key === environment)?.label || environment
}

const componentLabel = (component: string) => {
  if (component === 'intruoyi-full') return 'IntRuoyi 整套'
  return displayComponentRows.find((item) => item.key === component)?.label || component
}

const currentReleaseTagValue = (environment: string) => {
  return (
    statusOf(environment, 'intruoyi-full')?.currentReleaseTag ||
    statusOf(environment, 'intruoyi-backend')?.currentReleaseTag ||
    statusOf(environment, 'intruoyi-frontend')?.currentReleaseTag ||
    statusOf(environment, 'website-frontend')?.currentReleaseTag
  )
}

const currentReleaseTagText = (environment: string) => {
  return currentReleaseTagValue(environment) || '无'
}

const releaseStatusComponent = (environment: string, component: string) => {
  return releaseStatus.value?.targetStates?.[environment]?.[component] || statusOf(environment, component)
}

const releaseStatusCurrentTag = (environment: string) => {
  return (
    releaseStatusComponent(environment, 'intruoyi-full')?.currentReleaseTag ||
    releaseStatusComponent(environment, 'intruoyi-backend')?.currentReleaseTag ||
    releaseStatusComponent(environment, 'intruoyi-frontend')?.currentReleaseTag ||
    releaseStatusComponent(environment, 'website-frontend')?.currentReleaseTag ||
    '无'
  )
}

const releaseStatusOperationText = (environment: string) => {
  const operation = releaseStatus.value?.recentOperations?.find((item) => item.environment === environment)
  if (!operation) return '-'
  return `${operationActionLabel(operation.action || '')} / ${statusText(operation.status)} / ${formatRuntimeDate(operation.requestedAt)}`
}

const releasePackageUsageText = (releaseTag: string) => {
  if (testedReleaseTags.value.has(releaseTag)) return '测试通过'
  if (releaseTag === testCurrentReleaseTag.value) return '当前测试服'
  if (testUsedReleaseTags.value.has(releaseTag)) return '曾部署测试服'
  return ''
}

const releasePackageOnlyOfficeText = (item: RuntimeControlApi.RuntimeControlReleasePackageVO) => {
  return item.onlyOfficeIncluded ? '包含 OnlyOffice' : '不含 OnlyOffice'
}

const releasePackageShowroomText = (item: RuntimeControlApi.RuntimeControlReleasePackageVO) => {
  return item.includeShowroomBuildPackage ? '包含展厅构筑包' : '不含展厅构筑包'
}

const releasePackageUsageClass = (releaseTag: string) => {
  if (testedReleaseTags.value.has(releaseTag)) return 'tested-release'
  if (releaseTag === testCurrentReleaseTag.value) return 'current-test-release'
  if (testUsedReleaseTags.value.has(releaseTag)) return 'used-test-release'
  return ''
}

const shouldShowAccessPath = (component: string) => {
  return ['intruoyi-frontend', 'website-frontend'].includes(component)
}

const canRestart = (environment: string, component: string) => {
  return Boolean(statusOf(environment, component)?.actionEnabled) && checkPermi(['infra:runtime-control:restart'])
}

const operationRequiresProd = (action: string) => {
  if (['promote-prod', 'promote-backup'].includes(action)) return true
  if (['backup-now', 'rollback-app', 'restore-data'].includes(action)) {
    return operationEnvironmentRequiresProdConfirm(operationDialog.targetEnvironment)
  }
  return false
}

const operationEnvironmentRequiresProdConfirm = (environment: string) => {
  return ['prod', 'backup'].includes(environment)
}

const operationRequiresOwner = (action: string) => {
  return ['promote-prod', 'promote-backup', 'rollback-app', 'rehearsal', 'restore-data'].includes(
    action
  )
}

const operationOwnerRole = (action: string) => {
  if (action === 'restore-data') return 'data-owner'
  if (action === 'rehearsal') return 'ops-owner'
  return 'release-owner'
}

const operationOwnerRoleText = (action: string) => {
  if (operationOwnerRole(action) === 'ops-owner') return '运维责任人'
  return operationOwnerRole(action) === 'data-owner' ? '数据责任人' : '发布责任人'
}

const operationEnvironmentKey = (action: string) => {
  if (action === 'build-release') return 'release'
  if (['publish-test', 'apply-test-db-sql', 'mark-release-tested', 'rehearsal'].includes(action)) return 'test'
  if (action === 'backup-now' || action === 'rollback-app' || action === 'restore-data') return operationDialog.targetEnvironment
  if (action === 'promote-backup') return 'backup'
  return 'prod'
}

const operationEnvironmentText = (action: string) => {
  if (!action) return ''
  if (action === 'build-release') return '发布包仓库'
  return environmentLabel(operationEnvironmentKey(action))
}

const operationSupportsPublishScope = (action: string) => {
  return action === 'build-release'
}

const operationSupportsSmartReleaseReport = (action: string) => {
  return ['build-release', 'publish-test', 'promote-prod', 'promote-backup'].includes(action)
}

const operationSupportsTargetEnvironment = (action: string) => {
  return action === 'backup-now' || action === 'restore-data' || action === 'rollback-app'
}

const operationRequiresTargetEnvironment = (action: string) => {
  return operationSupportsTargetEnvironment(action)
}

const operationTargetEnvironmentOptions = (action: string) => {
  if (action === 'rollback-app') return rollbackTargetEnvironmentOptions
  if (action === 'restore-data') return restoreTargetEnvironmentOptions
  if (action === 'backup-now') return backupTargetEnvironmentOptions
  return []
}

const operationTargetEnvironmentLabel = (action: string) => {
  if (action === 'rollback-app') return '回滚目标'
  return action === 'restore-data' ? '恢复目标' : '备份环境'
}

const operationTargetEnvironmentText = (environment: string) => {
  return (
    [...backupTargetEnvironmentOptions, ...restoreTargetEnvironmentOptions].find(
      (item) => item.value === environment
    )?.label || environmentLabel(environment)
  )
}

const rootDiskTargetText = (environment: RootDiskTargetEnvironment) => {
  return rootDiskTargetOptions.find((item) => item.value === environment)?.label || environment
}

const remoteRootCleanupRequiresProdConfirm = (environment: RootDiskTargetEnvironment) => {
  return ['prod', 'backup'].includes(environment)
}

const assertRemoteRootStatusProof = (
  status?: RuntimeControlApi.RuntimeControlRemoteRootDiskStatusVO
) => {
  const expected = selectedRootDiskTarget.value
  if (!status || !expected) {
    throw new Error('远程根分区状态缺少目标证明')
  }
  if (status.targetEnvironment !== remoteRootTargetEnvironment.value || status.serverHost !== expected.host) {
    throw new Error(`远程根分区状态目标证明不匹配：${status.targetEnvironment}/${status.serverHost}`)
  }
  if (status.mountPoint !== '/') {
    throw new Error(`远程根分区状态挂载点不正确：${status.mountPoint}`)
  }
}

const assertRemoteRootCleanupProof = (
  result?: RuntimeControlApi.RuntimeControlRemoteRootCleanupVO
) => {
  const expected = selectedRootDiskTarget.value
  if (!result || !expected) {
    throw new Error('远程根分区清理结果缺少目标证明')
  }
  if (result.targetEnvironment !== remoteRootTargetEnvironment.value || result.serverHost !== expected.host) {
    throw new Error(`远程根分区清理目标证明不匹配：${result.targetEnvironment}/${result.serverHost}`)
  }
  if (result.cleanupPaths?.join('|') !== '/opt/intruoyi/ops/backup/tmp|/tmp') {
    throw new Error(`远程根分区清理目录不在允许列表：${result.cleanupPaths?.join(',') || '空'}`)
  }
  assertRemoteRootStatusProof(result.before)
  assertRemoteRootStatusProof(result.after)
}

const operationUsesReleaseTag = (action: string) => {
  return ['build-release', 'publish-test', 'promote-prod', 'promote-backup'].includes(action)
}

const operationUsesReleaseTagSelector = (action: string) => {
  return ['publish-test', 'promote-prod', 'promote-backup'].includes(action)
}

const operationUsesCurrentTestReleaseTag = (action: string) => {
  return action === 'mark-release-tested'
}

const operationRequiresReleaseTag = (action: string) => {
  return ['publish-test', 'promote-prod', 'promote-backup'].includes(action)
}

const operationRequiresTestedReleasePackage = (action: string) => {
  return ['promote-prod', 'promote-backup'].includes(action)
}

const releaseTagDirectoryText = () => {
  const releaseTag = operationDialog.releaseTag.trim()
  return releaseTag || '<releaseTag>'
}

const operationSourceDirectoryText = (action: string) => {
  if (action === 'apply-test-db-sql') {
    return operationDialog.sqlPath.trim() || '<SQL 文件>'
  }
  if (action === 'backup-now') {
    return `${operationTargetEnvironmentText(operationDialog.targetEnvironment)} 当前 MySQL / MinIO / 文件对象 / 运行态`
  }
  if (['publish-test', 'promote-prod', 'promote-backup'].includes(action)) {
    return `${RELEASE_PACKAGE_ROOT}/${releaseTagDirectoryText()}`
  }
  if (action === 'rollback-app') {
    return `${RELEASE_PACKAGE_ROOT}/<rollbackCandidate>`
  }
  if (action === 'restore-data') {
    return `${BACKUP_PACKAGE_ROOT}/<backupId>`
  }
  if (action === 'rehearsal') {
    return `${BACKUP_PACKAGE_ROOT}/<backupId>`
  }
  return ''
}

const operationTargetDirectoryText = (action: string) => {
  if (action === 'build-release') {
    return `${RELEASE_PACKAGE_ROOT}/${releaseTagDirectoryText()}`
  }
  if (action === 'backup-now') {
    return `${BACKUP_PACKAGE_ROOT}/<backupId>`
  }
  if (action === 'restore-data') {
    return `${operationTargetEnvironmentText(operationDialog.targetEnvironment)} 当前 MySQL / MinIO / 文件对象`
  }
  if (action === 'rehearsal') {
    return '测试服恢复演练槽位'
  }
  if (action === 'rollback-app') {
    return `${operationTargetEnvironmentText(operationDialog.targetEnvironment)} 当前应用版本`
  }
  if (action === 'mark-release-tested') {
    return `${RELEASE_PACKAGE_ROOT}/${testCurrentReleaseTag.value || '<当前测试服releaseTag>'}/tested.json`
  }
  if (action === 'apply-test-db-sql') {
    return '测试服 intruoyi-mysql / ruoyi-vue-pro'
  }
  return ''
}

const operationExpectedResultText = (action: string) => {
  const texts: Record<string, string> = {
    'build-release': '生成 release manifest、checksum、镜像标签；发布包只进入发布包列表。',
    'publish-test': '部署所选发布包到测试服，并记录测试服当前 releaseTag。',
    'apply-test-db-sql': '只应用明确 SQL，不同步数据库整库、MinIO 或发布包状态。',
    'mark-release-tested': '只标记当前测试服正在运行的 releaseTag，并绑定已验证的恢复集候选。',
    'promote-prod': '上线已测试通过的发布包，并记录正式服发布历史。',
    'promote-backup': '上线已测试通过的发布包，并记录备用服务器发布历史。',
    'backup-now': `${operationTargetEnvironmentText(operationDialog.targetEnvironment)}生成真实备份点；备份只进入备份点列表。`,
    rehearsal:
      '在测试服恢复演练槽位验证所选恢复集，并写回演练报告；不覆盖当前运行中的测试服、备份服务器或正式服务器数据。',
    'rollback-app':
      '兼容性成立后只回滚程序；只回滚应用版本，只覆盖所选测试服或备份服务器应用版本，不恢复数据库、MinIO、Redis、业务文件或配置，禁止影响正式服务器程序和数据。',
    'restore-data':
      '恢复同一恢复集的 MySQL / MinIO / 文件对象；程序版本指纹、Redis 策略和配置清单仅作为恢复集证据展示，不会自动切换程序版本、执行 Redis 处理或覆盖目标运行配置；恢复数据只覆盖所选测试服或备份服务器，禁止影响正式服务器程序和数据。'
  }
  return texts[action] || ''
}

const operationActionText = (operation: RuntimeControlOperationVO) => {
  return operation.actionLabel || operation.action || '重启'
}

function operationActionLabel(action: string) {
  return operationActions.find((item) => item.action === action)?.label || action
}

const operationPublishScopeText = (operation: RuntimeControlOperationVO) => {
  const publishScope = operation.parameters?.publishScope
  if (publishScope === 'code-only') return '只发代码'
  if (publishScope === 'with-data') return '带数据发布'
  return '-'
}

const operationTargetText = (operation: RuntimeControlOperationVO) => {
  if (operation.component === 'ops') return operation.environment
  return componentLabel(operation.component)
}

const operationRequestedAtText = (operation: RuntimeControlOperationVO) => {
  const value = operation.requestedAt
  if (!value) return '-'
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    return formatDate(new Date(year, month - 1, day, hour, minute, second))
  }
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : formatDate(date)
}

const openOperation = async (actionValue: string) => {
  const action = operationActions.find((item) => item.action === actionValue)
  if (!action || !canOperate.value) {
    return
  }
  operationDialog.visible = true
  operationDialog.title = action.label
  operationDialog.action = action.action
  operationDialog.label = action.label
  operationDialog.publishScope = 'code-only'
  operationDialog.includeOnlyOffice = false
  operationDialog.includeShowroomBuildPackage = false
  operationDialog.enableSmartReleaseReport = false
  operationDialog.targetEnvironment =
    action.action === 'backup-now' || action.action === 'rollback-app' || action.action === 'restore-data'
      ? 'test'
      : 'prod'
  operationDialog.releaseTag = action.action === 'build-release' ? formatDefaultReleaseTag() : ''
  operationDialog.testConclusion =
    action.action === 'mark-release-tested' ? '回归通过，允许上线正式服' : ''
  operationDialog.reason =
    action.action === 'build-release'
      ? DEFAULT_BUILD_RELEASE_REASON
      : action.action === 'publish-test'
        ? DEFAULT_PUBLISH_TEST_REASON
        : action.action === 'apply-test-db-sql'
          ? DEFAULT_APPLY_TEST_DB_SQL_REASON
        : action.action === 'promote-prod'
          ? DEFAULT_PROMOTE_PROD_REASON
          : action.action === 'promote-backup'
            ? DEFAULT_PROMOTE_BACKUP_REASON
          : ''
  operationDialog.sqlPath = ''
  operationDialog.prodConfirmText = ''
  operationDialog.selectedImageCandidateId = ''
  operationDialog.selectedRecoverySetCandidateId = ''
  operationPreview.content = ''

  if (operationUsesCurrentTestReleaseTag(action.action)) {
    try {
      await loadOverviewMatrix()
    } catch (error) {
      reportActionError(error)
    }
  }

  if (operationUsesReleaseTagSelector(action.action)) {
    try {
      await loadReleasePackages()
    } catch (error) {
      releasePackages.value = []
      reportActionError(error)
    }
  }

  if (['rollback-app', 'rehearsal', 'restore-data', 'mark-release-tested'].includes(action.action)) {
    try {
      await loadCandidates()
      if (action.action === 'rollback-app') {
        operationDialog.selectedImageCandidateId =
          rollbackCandidates.value.find((candidate) => candidate.status === 'AVAILABLE')?.candidateId || ''
      }
      if (['rehearsal', 'restore-data', 'mark-release-tested'].includes(action.action)) {
        operationDialog.selectedRecoverySetCandidateId =
          restoreCandidates.value.find((candidate) => candidate.status === 'AVAILABLE')?.candidateId || ''
      }
    } catch (error) {
      reportActionError(error)
    }
  }
}

const handleShowroomBuildPackageChange = async (checked: boolean | string | number) => {
  if (!checked) {
    return
  }
  try {
    await message.confirm('当前选中的展厅构筑包会覆盖服务器的展厅数据，是否继续？')
  } catch {
    operationDialog.includeShowroomBuildPackage = false
  }
}

const submitOperation = async () => {
  const reason = operationDialog.reason.trim()
  if (!reason) {
    message.warning('请填写操作原因')
    return
  }
  if (operationRequiresProd(operationDialog.action) && operationDialog.prodConfirmText !== 'PROD') {
    message.warning('生产相关操作必须输入 PROD')
    return
  }
  if (operationRequiresTargetEnvironment(operationDialog.action) && !operationDialog.targetEnvironment) {
    message.warning(operationDialog.action === 'restore-data' ? '请选择恢复目标' : '请选择备份环境')
    return
  }
  const releaseTag = operationDialog.releaseTag.trim()
  if (operationRequiresReleaseTag(operationDialog.action) && !releaseTag) {
    message.warning('请选择 NAS 发布包编号')
    return
  }
  if (operationUsesCurrentTestReleaseTag(operationDialog.action) && !testCurrentReleaseTag.value) {
    message.warning('测试服当前没有可标记的发布包')
    return
  }
  if (operationDialog.action === 'mark-release-tested' && !operationDialog.testConclusion.trim()) {
    message.warning('请填写验证结论')
    return
  }
  if (operationDialog.action === 'apply-test-db-sql' && !operationDialog.sqlPath.trim()) {
    message.warning('请填写 SQL 文件路径')
    return
  }
  if (operationBlockReason.value) {
    message.warning(operationBlockReason.value)
    return
  }

  operationSubmitting.value = true
  try {
    const operation = await RuntimeControlApi.executeRuntimeControlAction(
      buildOperationActionRequest(reason, releaseTag)
    )
    operationDialog.visible = false
    message.success('运维动作已提交')
    await loadOverview()
    await openLog(operation)
  } catch (error) {
    reportActionError(error)
  } finally {
    operationSubmitting.value = false
  }
}

const buildOperationActionRequest = (reason: string, releaseTag: string) => ({
  action: operationDialog.action,
  reason,
  prodConfirmText: operationDialog.prodConfirmText,
  targetEnvironment: operationSubmitTargetEnvironment(),
  publishScope: operationSupportsPublishScope(operationDialog.action)
    ? operationDialog.publishScope
    : undefined,
  includeOnlyOffice:
    operationDialog.action === 'build-release' ? operationDialog.includeOnlyOffice : undefined,
  includeShowroomBuildPackage:
    operationDialog.action === 'build-release'
      ? operationDialog.includeShowroomBuildPackage
      : undefined,
  enableSmartReleaseReport: operationSupportsSmartReleaseReport(operationDialog.action)
    ? operationDialog.enableSmartReleaseReport
    : undefined,
  releaseTag: operationUsesReleaseTag(operationDialog.action) && releaseTag ? releaseTag : undefined,
  sqlPath: operationDialog.action === 'apply-test-db-sql' ? operationDialog.sqlPath.trim() : undefined,
  testConclusion:
    operationDialog.action === 'mark-release-tested'
      ? operationDialog.testConclusion.trim()
      : undefined,
  selectedImageCandidateId:
    operationDialog.action === 'rollback-app'
      ? operationDialog.selectedImageCandidateId
      : undefined,
  selectedRecoverySetCandidateId:
    ['rehearsal', 'restore-data', 'mark-release-tested'].includes(operationDialog.action)
      ? operationDialog.selectedRecoverySetCandidateId
      : undefined
})

const previewOperationCommand = async () => {
  const reason = operationDialog.reason.trim()
  if (!reason) {
    message.warning('请填写操作原因')
    return
  }
  const releaseTag = operationDialog.releaseTag.trim()
  if (operationRequiresReleaseTag(operationDialog.action) && !releaseTag) {
    message.warning('请选择 NAS 发布包编号')
    return
  }
  if (operationBlockReason.value) {
    message.warning(operationBlockReason.value)
    return
  }
  operationPreview.loading = true
  operationPreview.content = ''
  try {
    const preview = await RuntimeControlApi.previewRuntimeControlAction(
      buildOperationActionRequest(reason, releaseTag)
    )
    operationPreview.content = [
      `action=${preview.action}`,
      `environment=${preview.environment}`,
      `script=${preview.scriptPath}`,
      `enableSmartReleaseReport=${preview.enableSmartReleaseReport}`,
      `arguments=${preview.arguments.join(' ')}`
    ].join('\n')
  } catch (error) {
    reportActionError(error)
  } finally {
    operationPreview.loading = false
  }
}

const operationSubmitTargetEnvironment = () => {
  return operationDialog.action === 'rollback-app'
    ? operationDialog.targetEnvironment
    : operationDialog.action === 'restore-data'
      ? operationDialog.targetEnvironment
      : operationDialog.action === 'backup-now'
        ? operationDialog.targetEnvironment
        : undefined
}

const candidateBlockReason = (action: string) => {
  if (action === 'rollback-app') {
    return candidateReason(
      rollbackCandidates.value,
      operationDialog.selectedImageCandidateId,
      '回滚版本',
      'selectedImageCandidateId'
    )
  }
  if (['rehearsal', 'restore-data', 'mark-release-tested'].includes(action)) {
    return candidateReason(
      restoreCandidates.value,
      operationDialog.selectedRecoverySetCandidateId,
      action === 'mark-release-tested' ? '标记测试通过' : action === 'rehearsal' ? '恢复演练' : '恢复数据',
      'selectedRecoverySetCandidateId'
    )
  }
  return ''
}

const candidateReason = (
  candidates: RuntimeControlApi.RuntimeControlCandidateVO[],
  candidateId: string,
  actionLabel: string,
  fieldName: string
) => {
  if (!candidates.length) {
    return `服务端未返回${actionLabel}候选，禁止提交`
  }
  if (!candidateId) {
    const blockedReasons = candidates
      .filter((candidate) => candidate.status === 'BLOCKED')
      .flatMap((candidate) => candidate.blockedReasons || [])
    return blockedReasons.length
      ? `${actionLabel}候选全部被阻断：${blockedReasons.join('；')}`
      : `请选择服务端${actionLabel}候选`
  }
  const candidate = candidates.find((item) => item.candidateId === candidateId)
  if (!candidate) {
    return `${fieldName} 候选不存在，请刷新候选清单`
  }
  if (candidate.status === 'BLOCKED') {
    return `${fieldName} 候选被阻断：${(candidate.blockedReasons || []).join('；') || '服务端判定不可用'}`
  }
  if (candidate.status !== 'AVAILABLE') {
    return `${fieldName} 候选状态不可用：${candidate.status}`
  }
  return ''
}

const openLog = async (operation: RuntimeControlOperationVO) => {
  if (!operation.operationId) return
  logDialog.visible = true
  logDialog.operationId = operation.operationId
  logDialog.title = `${operationActionText(operation)} / ${operation.operationId}`
  logDialog.status = operation.status
  logDialog.content = ''
  logDialog.truncated = false
  await loadOperationLog()
  startLogPolling()
}

const loadOperationLog = async () => {
  if (!logDialog.operationId || logDialog.loading) {
    return
  }
  logDialog.loading = true
  try {
    const log = await RuntimeControlApi.getRuntimeControlOperationLog(logDialog.operationId)
    logDialog.status = log.status
    logDialog.content = log.content
    logDialog.truncated = log.truncated
  } catch (error) {
    reportActionError(error)
  } finally {
    logDialog.loading = false
  }
}

const startLogPolling = () => {
  stopLogPolling()
  logPollingTimer = window.setInterval(loadOperationLog, 3000)
}

const stopLogPolling = () => {
  if (logPollingTimer) {
    window.clearInterval(logPollingTimer)
    logPollingTimer = undefined
  }
}

const openRestart = async (environment: string, component: string) => {
  restartDialog.environment = environment
  restartDialog.component = component
  restartDialog.reason = ''
  restartDialog.prodConfirmText = ''

  if (operationEnvironmentRequiresProdConfirm(environment)) {
    restartDialog.visible = true
    return
  }

  try {
    await message.confirm(`确认重启 ${environmentLabel(environment)} / ${componentLabel(component)}？`)
    restartDialog.reason = '控制台手动重启'
    await submitRestart()
  } catch (error) {
    if (error) {
      reportActionError(error)
    }
  }
}

const submitRestart = async () => {
  const reason = restartDialog.reason.trim()
  if (!reason) {
    message.warning('请填写重启原因')
    return
  }
  if (
    operationEnvironmentRequiresProdConfirm(restartDialog.environment) &&
    restartDialog.prodConfirmText !== 'PROD'
  ) {
    message.warning('生产环境必须输入 PROD')
    return
  }

  restarting.value = true
  try {
    await RuntimeControlApi.restartRuntimeControl({
      environment: restartDialog.environment,
      component: restartDialog.component,
      reason,
      prodConfirmText: restartDialog.prodConfirmText
    })
    restartDialog.visible = false
    message.success('重启命令已提交')
    await loadOverview()
  } catch (error) {
    reportActionError(error)
  } finally {
    restarting.value = false
  }
}

const statusTagType = (status?: string) => {
  if (
    status === 'running' ||
    status === 'RUNNING' ||
    status === 'succeeded' ||
    status === 'SUCCEEDED' ||
    status === 'PASS' ||
    status === 'PASSED' ||
    status === 'AVAILABLE' ||
    status === 'COMPLETE' ||
    status === 'RECOVERABLE'
  ) {
    return 'success'
  }
  if (status === 'degraded' || status === 'WARN' || status === 'not-run' || status === 'PARTIAL') return 'warning'
  if (
    status === 'stopped' ||
    status === 'failed' ||
    status === 'error' ||
    status === 'NO_GO' ||
    status === 'BLOCKED' ||
    status === 'FAILED' ||
    status === 'INCOMPLETE' ||
    status === 'UNRECOVERABLE'
  ) {
    return 'danger'
  }
  return 'info'
}

const statusText = (status?: string) => {
  const textMap: Record<string, string> = {
    running: '运行中',
    RUNNING: '运行中',
    succeeded: '成功',
    SUCCEEDED: '成功',
    degraded: '异常',
    stopped: '已停止',
    failed: '失败',
    error: '错误',
    PASS: '通过',
    WARN: '预警',
    BLOCKED: '已阻断',
    NO_GO: '不放行',
    AVAILABLE: '可用',
    FAILED: '失败',
    PARTIAL: '部分完成'
  }
  return status ? textMap[status] || status : '-'
}

const lastOperationText = (operation?: RuntimeControlOperationVO) => {
  if (!operation) return '最近操作：-'
  return `最近操作：${statusText(operation.status)} ${operation.summary || ''}`
}

const errorMessage = (error: unknown) => {
  return error instanceof Error ? error.message : String(error || '运行控制台请求失败')
}

const reportActionError = (error: unknown) => {
  const msg = errorMessage(error)
  lastError.value = msg
  message.error(msg)
}

onMounted(() => {
  loadOverview()
  pollingTimer = window.setInterval(loadOverview, 60000)
})

onBeforeUnmount(() => {
  if (pollingTimer) {
    window.clearInterval(pollingTimer)
  }
  stopLogPolling()
})

watch(
  () => logDialog.visible,
  (visible) => {
    if (!visible) {
      stopLogPolling()
    }
  }
)
</script>

<style scoped>
.runtime-control-page {
  min-height: 100%;
  padding: 16px;
  background: #f7f9fc;
  color: #172033;
}

.runtime-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
}

.runtime-title__main {
  font-size: 18px;
  font-weight: 700;
  line-height: 24px;
}

.runtime-title__meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
}

.runtime-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.runtime-error {
  padding: 10px 16px;
  color: #b42318;
  background: #fff1f0;
  border-right: 1px solid #dbe3ef;
  border-left: 1px solid #dbe3ef;
}

.publish-scope-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.smart-release-field {
  display: flex;
  align-items: center;
  gap: 12px;
}

.publish-scope-hint,
.operation-expected-result,
.operation-block-alert {
  font-size: 12px;
  line-height: 18px;
}

.operation-expected-result {
  width: 100%;
  min-height: 32px;
  padding: 6px 10px;
  color: #263247;
  background: #fafcff;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
}

.operation-command-preview {
  width: 100%;
  min-height: 80px;
  max-height: 180px;
  padding: 8px 10px;
  overflow: auto;
  color: #172033;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  background: #fafcff;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
}

.publish-scope-hint {
  color: #b42318;
}

.operation-block-alert {
  margin-top: 8px;
}

.ops-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 10px 16px;
  background: #ffffff;
  border-right: 1px solid #dbe3ef;
  border-left: 1px solid #dbe3ef;
  border-top: 1px solid #edf1f6;
}

.release-status-panel {
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-bottom: 0;
}

.release-status-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.release-status-panel__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.release-status-panel__meta {
  color: #4b5563;
  font-size: 12px;
}

.release-status-table {
  width: 100%;
  margin-top: 12px;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 0.9rem;
}

.release-status-table th {
  height: 46px;
  padding: 7px 10px;
  background: #f7f9fc;
  border-bottom: 1px solid #e5ebf3;
  color: #263247;
  font-weight: 600;
  text-align: left;
}

.release-status-table td {
  height: 52px;
  padding: 7px 10px;
  border-bottom: 1px solid #edf1f6;
  color: #172033;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.release-status-table tr:hover td {
  background: #fafcff;
}

.runtime-matrix {
  overflow-x: auto;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 0 0 8px 8px;
}

.runtime-matrix table {
  width: 100%;
  min-width: 1060px;
  border-collapse: collapse;
  table-layout: fixed;
}

.runtime-matrix th {
  height: 58px;
  padding: 7px 10px;
  color: #263247;
  font-weight: 700;
  text-align: left;
  background: #f7f9fc;
  border-bottom: 1px solid #edf1f6;
}

.runtime-env-heading {
  line-height: 20px;
}

.runtime-current-release {
  margin-top: 4px;
  color: #5f6f89;
  font-size: 12px;
  font-weight: 400;
  line-height: 18px;
  word-break: break-all;
}

.release-package-option {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 8px;
}

.release-package-option__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.release-package-option__status {
  flex: none;
  font-size: 12px;
}

.release-package-option.current-test-release .release-package-option__name,
.release-package-option.current-test-release .release-package-option__status,
.release-package-option.tested-release .release-package-option__name,
.release-package-option.tested-release .release-package-option__status {
  color: #16833d;
  font-weight: 600;
}

.release-package-option.used-test-release .release-package-option__name,
.release-package-option.used-test-release .release-package-option__status {
  color: #b7791f;
  font-weight: 600;
}

.runtime-matrix td {
  min-height: 52px;
  padding: 10px;
  vertical-align: top;
  border-bottom: 1px solid #edf1f6;
}

.component-col {
  width: 190px;
}

.component-cell {
  background: #fafcff;
}

.component-name {
  font-weight: 700;
}

.component-key,
.runtime-http,
.runtime-last,
.runtime-state {
  color: #4b5563;
  font-size: 12px;
}

.status-block {
  display: grid;
  gap: 6px;
}

.status-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.runtime-link {
  overflow: hidden;
  color: #1677ff;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.runtime-path {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  color: #4b5563;
  font-size: 12px;
}

.runtime-path__label,
.runtime-path__value {
  color: #4b5563;
}

.ops-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.operation-panel {
  margin-top: 14px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.remote-root-panel {
  margin-top: 14px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.remote-root-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.remote-root-panel__meta,
.remote-root-panel__actions,
.remote-root-paths {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.remote-root-panel__meta {
  color: #4b5563;
  font-size: 12px;
}

.remote-root-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.remote-root-metric {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 10px 12px;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 6px;
}

.remote-root-metric span,
.remote-root-paths,
.remote-root-result {
  color: #4b5563;
  font-size: 12px;
}

.remote-root-metric strong {
  overflow: hidden;
  color: #172033;
  font-size: 16px;
  line-height: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remote-root-paths,
.remote-root-result {
  margin-top: 10px;
}

.remote-root-paths span {
  padding: 3px 8px;
  color: #263247;
  background: #fafcff;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
}

.backup-summary-panel {
  margin-top: 14px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.backup-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.backup-summary-item {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 6px;
}

.backup-summary-item__label {
  color: #4b5563;
  font-size: 12px;
}

.panel-title {
  margin-bottom: 10px;
  font-weight: 700;
}

.log-toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 10px;
}

.log-content {
  min-height: 360px;
  max-height: 520px;
  padding: 12px;
  overflow: auto;
  color: #172033;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  background: #f7f9fc;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
}

@media (max-width: 1100px) {
  .ops-grid {
    grid-template-columns: 1fr;
  }

  .backup-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .remote-root-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .runtime-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .runtime-actions {
    width: 100%;
    justify-content: space-between;
  }

  .backup-summary-grid {
    grid-template-columns: 1fr;
  }

  .remote-root-panel__head {
    flex-direction: column;
  }

  .remote-root-panel__actions {
    width: 100%;
  }

  .remote-root-grid {
    grid-template-columns: 1fr;
  }
}
</style>
