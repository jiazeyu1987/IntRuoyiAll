<template>
  <component :is="signaturePageShell" :class="{ 'dcc-signature-page--embedded': isEmbedded }">
    <el-tabs v-model="activeTab" :class="{ 'dcc-signature-page__single-tab': singleTab }">
      <el-tab-pane v-if="isTabVisible('records')" label="签名记录" name="records">
        <UnifiedListTemplate
          class="dcc-signature-list-template"
          table-key="dcc.electronicSignature.records"
          :query-model="recordQueryParams"
          :filter-definitions="recordQuickFilterDefinitions"
          :show-quick-filter-label="false"
          :quick-filter-state="recordQuickFilter.state"
          :selected-filter-definition="recordQuickFilter.selectedDefinition.value"
          :operator-options="recordQuickFilter.operatorOptions.value"
          :columns="recordColumns"
          :column-saving="recordColumnSaving"
          :show-column-reset="false"
          :total="recordTotal"
          v-model:page="recordQueryParams.pageNo"
          v-model:limit="recordQueryParams.pageSize"
          @update:quick-filter-state="recordQuickFilter.updateState"
          @quick-filter-query="recordQuickFilter.applyQuickFilter"
          @column-change="saveRecordColumnConfig"
          @pagination="loadRecordPage"
        >
          <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table v-loading="recordLoading" data-user-table-column-explicit data-user-table-key="dcc.electronicSignature.records" :data="recordList" class="mt-16px" @header-dragend="handleRecordHeaderDragend" @sort-change="handleTemplateSortChange">
          <el-table-column v-if="isRecordColumnVisible('fileName')" label="文件名称" prop="fileName" :min-width="getRecordColumnMinWidthString('fileName', 220)" show-overflow-tooltip v-bind="sortColumnAttrs('fileName')">
            <template #default="{ row }">
              <el-button link type="primary" @click="openControlledFileDetail(row.controlledFileId)">
                {{ row.fileName || '-' }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column v-if="isRecordColumnVisible('fileNumber')" label="文件编号" :min-width="getRecordColumnMinWidthString('fileNumber', 150)" prop="fileNumber" show-overflow-tooltip v-bind="sortColumnAttrs('fileNumber')" />
          <template v-if="isRecordColumnVisible('revisionId')">
            <el-table-column
              v-if="isAdvancedSignatureView"
              label="修订ID"
              align="center"
              :width="getRecordColumnWidthString('revisionId', 110)"
              prop="revisionId"
              v-bind="sortColumnAttrs('revisionId')"
            />
          </template>
          <el-table-column v-if="isRecordColumnVisible('versionNo')" label="版本" align="center" :width="getRecordColumnWidthString('versionNo', 100)" prop="versionNo" v-bind="sortColumnAttrs('versionNo')" />
          <el-table-column v-if="isRecordColumnVisible('controlledFileStatus')" label="文件状态" prop="controlledFileStatus" align="center" :width="getRecordColumnWidthString('controlledFileStatus', 120)" v-bind="sortColumnAttrs('controlledFileStatus')">
            <template #default="{ row }">
              <el-tag :type="getDccControlledFileStatusTagType(toControlledFileStatus(row.controlledFileStatus))">
                {{ getDccControlledFileStatusLabel(toControlledFileStatus(row.controlledFileStatus)) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isRecordColumnVisible('signer')" label="签名人" prop="signer" :min-width="getRecordColumnMinWidthString('signer', 170)" v-bind="sortColumnAttrs('signer')">
            <template #default="{ row }">
              <div>{{ row.signerName || row.actorNicknameSnapshot || `用户#${row.signerUserId}` }}</div>
              <div class="signature-record-summary__meta">
                {{ row.actorUsernameSnapshot || '旧版证据未记录' }}
              </div>
            </template>
          </el-table-column>
          <el-table-column v-if="isRecordColumnVisible('actorDeptPost')" label="部门/岗位" prop="actorDeptPost" :min-width="getRecordColumnMinWidthString('actorDeptPost', 180)" v-bind="sortColumnAttrs('actorDeptPost')">
            <template #default="{ row }">
              <div>{{ formatDccSignatureSnapshotValue(row.actorDeptNameSnapshot) }}</div>
              <div class="signature-record-summary__meta">
                {{ formatDccSignatureSnapshotValue(row.actorPostNamesSnapshot) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column v-if="isRecordColumnVisible('actorRoleNamesSnapshot')" label="角色" prop="actorRoleNamesSnapshot" :min-width="getRecordColumnMinWidthString('actorRoleNamesSnapshot', 150)" v-bind="sortColumnAttrs('actorRoleNamesSnapshot')">
            <template #default="{ row }">
              {{ formatDccSignatureSnapshotValue(row.actorRoleNamesSnapshot) }}
            </template>
          </el-table-column>
          <el-table-column v-if="isRecordColumnVisible('signatureSummary')" label="签名摘要" prop="signatureSummary" :min-width="getRecordColumnMinWidthString('signatureSummary', 260)" v-bind="sortColumnAttrs('signatureSummary')">
            <template #default="{ row }">
              <div class="signature-record-summary" data-testid="dcc-signature-action-summary">
                <div class="signature-record-summary__line">
                  <el-tag size="small" type="primary">
                    {{ getDccSignatureMeaningLabel(row.meaningCode) }}
                  </el-tag>
                  <span class="signature-record-summary__main">
                    {{ getDccSignatureTaskActionLabel(row.taskActionResult) }}
                  </span>
                </div>
                <div class="signature-record-summary__meta">
                  目的：{{ formatDccSignatureSnapshotValue(row.signaturePurpose || row.meaningCode) }}
                </div>
                <div class="signature-record-summary__meta">时间：{{ formatDateTimeValue(row.signedAt) }}</div>
              </div>
            </template>
          </el-table-column>
          <template v-if="isRecordColumnVisible('sourceFileHash')">
            <el-table-column
              v-if="isAdvancedSignatureView"
              label="源文件 hash"
              align="center"
              :width="getRecordColumnWidthString('sourceFileHash', 150)"
              prop="sourceFileHash"
              v-bind="sortColumnAttrs('sourceFileHash')"
            >
              <template #default="{ row }">
                <span class="signature-hash">{{ formatDccHashShort(row.sourceFileHashShort) }}</span>
              </template>
            </el-table-column>
          </template>
          <template v-if="isRecordColumnVisible('controlledCopyHash')">
            <el-table-column
              v-if="isAdvancedSignatureView"
              label="副本 hash"
              align="center"
              :width="getRecordColumnWidthString('controlledCopyHash', 150)"
              prop="controlledCopyHash"
              v-bind="sortColumnAttrs('controlledCopyHash')"
            >
              <template #default="{ row }">
                <span class="signature-hash">{{ formatDccHashShort(row.controlledCopyHashShort) }}</span>
              </template>
            </el-table-column>
          </template>
          <el-table-column v-if="isRecordColumnVisible('evidenceSummary')" label="证据摘要" prop="evidenceSummary" :min-width="getRecordColumnMinWidthString('evidenceSummary', 260)" v-bind="sortColumnAttrs('evidenceSummary')">
            <template #default="{ row }">
              <div class="signature-evidence-summary" data-testid="dcc-signature-evidence-summary">
                <div class="signature-evidence-summary__line">
                  <span class="signature-evidence-summary__label">副本</span>
                  <el-tag :type="getDccControlledCopyHashStatusTagType(row.controlledCopyHashStatus)" size="small">
                    {{ getDccControlledCopyHashStatusLabel(row.controlledCopyHashStatus) }}
                  </el-tag>
                </div>
                <div class="signature-evidence-summary__line">
                  <span class="signature-evidence-summary__label">证据</span>
                  <el-tag :type="getDccSignatureEvidenceStatusTagType(row.evidenceStatus)" size="small">
                    {{ getDccSignatureEvidenceStatusLabel(row.evidenceStatus) }}
                  </el-tag>
                </div>
                <div class="signature-record-summary__meta">
                  快照：{{ formatDccSignatureSnapshotValue(row.snapshotStatus) }}
                </div>
                <div class="signature-record-summary__meta">
                  签名图片：{{ formatDccSignatureSnapshotValue(row.signatureImageVerifiedStatus) }}
                </div>
                <div class="signature-record-summary__meta">
                  图片 hash：{{ formatDccHashShort(row.signatureImageSha256Short || row.signatureImageSha256) }}
                </div>
              </div>
            </template>
          </el-table-column>
          <template v-if="isRecordColumnVisible('evidenceHash')">
            <el-table-column
              v-if="isAdvancedSignatureView"
              label="证据 hash"
              align="center"
              :width="getRecordColumnWidthString('evidenceHash', 150)"
              prop="evidenceHash"
              v-bind="sortColumnAttrs('evidenceHash')"
            >
              <template #default="{ row }">
                <span class="signature-hash">{{ formatDccHashShort(row.evidenceHashShort) }}</span>
              </template>
            </el-table-column>
          </template>
          <el-table-column v-if="isRecordColumnVisible('operation')" label="操作" prop="operation" align="center" fixed="right" :width="getRecordColumnWidthString('operation', 180)">
            <template #default="{ row }">
              <div class="signature-action-row">
                <el-button
                  link
                  type="primary"
                  :loading="isPreviewingControlledFile(row.controlledFileId)"
                  @click="openSignaturePdfPreview(row)"
                >
                  查看证据
                </el-button>
                <el-button
                  v-hasPermi="['dcc:controlled-file:query', 'dcc:controlled-file:download']"
                  link
                  type="primary"
                  :loading="isExportingControlledFile(row.controlledFileId)"
                  @click="handleExportSignatureEvidence(row)"
                >
                  下载证据 PDF
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
          </template>
        </UnifiedListTemplate>
      </el-tab-pane>

      <el-tab-pane v-if="isTabVisible('authorizations')" label="签名授权" name="authorizations">
        <UnifiedListTemplate
          class="dcc-signature-list-template"
          table-key="dcc.electronicSignature.authorizations"
          :query-model="authorizationQueryParams"
          :filter-definitions="authorizationQuickFilterDefinitions"
          :show-quick-filter-label="false"
          :quick-filter-state="authorizationQuickFilter.state"
          :selected-filter-definition="authorizationQuickFilter.selectedDefinition.value"
          :operator-options="authorizationQuickFilter.operatorOptions.value"
          :columns="authorizationColumns"
          :column-saving="authorizationColumnSaving"
          :show-column-reset="false"
          :total="authorizationTotal"
          v-model:page="authorizationQueryParams.pageNo"
          v-model:limit="authorizationQueryParams.pageSize"
          @update:quick-filter-state="authorizationQuickFilter.updateState"
          @quick-filter-query="authorizationQuickFilter.applyQuickFilter"
          @column-change="saveAuthorizationColumnConfig"
          @pagination="loadAuthorizationPage"
        >
          <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table v-loading="authorizationLoading" data-user-table-column-explicit data-user-table-key="dcc.electronicSignature.authorizations" :data="authorizationList" class="mt-16px" @header-dragend="handleAuthorizationHeaderDragend" @sort-change="handleTemplateSortChange">
          <el-table-column v-if="isAuthorizationColumnVisible('user')" label="用户" prop="user" :min-width="getAuthorizationColumnMinWidthString('user', 180)" v-bind="sortColumnAttrs('user')">
            <template #default="{ row }">
              {{ getAuthorizationUserLabel(row) }}
            </template>
          </el-table-column>
          <el-table-column v-if="isAuthorizationColumnVisible('deptName')" label="部门" :min-width="getAuthorizationColumnMinWidthString('deptName', 150)" prop="deptName" show-overflow-tooltip v-bind="sortColumnAttrs('deptName')" />
          <el-table-column v-if="isAuthorizationColumnVisible('mobile')" label="手机号" :min-width="getAuthorizationColumnMinWidthString('mobile', 140)" prop="mobile" v-bind="sortColumnAttrs('mobile')" />
          <el-table-column v-if="isAuthorizationColumnVisible('status')" label="用户状态" prop="status" align="center" :width="getAuthorizationColumnWidthString('status', 100)" v-bind="sortColumnAttrs('status')">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'success' : 'info'">
                {{ row.status === 0 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isAuthorizationColumnVisible('authorizationState')" label="授权状态" prop="authorizationState" align="center" :width="getAuthorizationColumnWidthString('authorizationState', 110)" v-bind="sortColumnAttrs('authorizationState')">
            <template #default="{ row }">
              <el-tag :type="getDccSignatureAuthorizationStateTagType(row.authorizationState)">
                {{ getDccSignatureAuthorizationStateLabel(row.authorizationState) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isAuthorizationColumnVisible('electronicSignatureEnabled')" label="启用签名" prop="electronicSignatureEnabled" align="center" :width="getAuthorizationColumnWidthString('electronicSignatureEnabled', 150)" v-bind="sortColumnAttrs('electronicSignatureEnabled')">
            <template #default="{ row }">
              <el-switch
                :model-value="row.electronicSignatureEnabled"
                :loading="authorizationUpdatingUserIds.includes(row.userId)"
                active-text="启用"
                inactive-text="停用"
                inline-prompt
                @change="openAuthorizationChangeDialog(row, $event)"
              />
            </template>
          </el-table-column>
          <el-table-column v-if="isAuthorizationColumnVisible('locked')" label="锁定" prop="locked" align="center" :width="getAuthorizationColumnWidthString('locked', 100)" v-bind="sortColumnAttrs('locked')">
            <template #default="{ row }">
              <el-tag :type="row.locked ? 'danger' : 'info'">
                {{ row.locked ? '已锁定' : '未锁定' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isAuthorizationColumnVisible('lockedUntil')" label="锁定至" prop="lockedUntil" align="center" :width="getAuthorizationColumnWidthString('lockedUntil', 180)" v-bind="sortColumnAttrs('lockedUntil')">
            <template #default="{ row }">
              {{ row.lockedUntil || '-' }}
            </template>
          </el-table-column>
          <el-table-column v-if="isAuthorizationColumnVisible('latestAuditReason')" label="最新审计原因" :min-width="getAuthorizationColumnMinWidthString('latestAuditReason', 220)" prop="latestAuditReason" show-overflow-tooltip v-bind="sortColumnAttrs('latestAuditReason')" />
          <el-table-column v-if="isAuthorizationColumnVisible('latestAuditOperatorName')" label="最新审计人" prop="latestAuditOperatorName" :min-width="getAuthorizationColumnMinWidthString('latestAuditOperatorName', 140)" v-bind="sortColumnAttrs('latestAuditOperatorName')">
            <template #default="{ row }">
              {{ row.latestAuditOperatorName || '-' }}
            </template>
          </el-table-column>
          <el-table-column v-if="isAuthorizationColumnVisible('latestAuditAt')" label="最新审计时间" prop="latestAuditAt" align="center" :width="getAuthorizationColumnWidthString('latestAuditAt', 180)" v-bind="sortColumnAttrs('latestAuditAt')">
            <template #default="{ row }">
              {{ formatDateTimeValue(row.latestAuditAt) }}
            </template>
          </el-table-column>
          <el-table-column v-if="isAuthorizationColumnVisible('operation')" label="操作" prop="operation" align="center" fixed="right" :width="getAuthorizationColumnWidthString('operation', 150)">
            <template #default="{ row }">
              <div class="signature-action-row">
                <el-button
                  v-if="row.locked"
                  link
                  type="primary"
                  :loading="authorizationUpdatingUserIds.includes(row.userId)"
                  @click="openAuthorizationUnlockDialog(row)"
                >
                  解锁
                </el-button>
                <el-button link type="primary" @click="openAuthorizationAudit(row)">审计</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
          </template>
        </UnifiedListTemplate>
      </el-tab-pane>
    </el-tabs>
  </component>

  <el-dialog
    v-model="signaturePdfPreviewDialog.visible"
    :title="signaturePdfPreviewDialog.fileName || '签名证据 PDF'"
    width="920px"
    destroy-on-close
    @closed="clearSignaturePdfPreview"
  >
    <el-alert
      v-if="signaturePdfPreviewDialog.inlineError"
      :closable="false"
      class="mb-16px"
      show-icon
      type="error"
      :title="signaturePdfPreviewDialog.inlineError"
    />
    <div v-loading="signaturePdfPreviewDialog.loading" class="signature-pdf-preview">
      <iframe
        v-if="signaturePdfPreviewDialog.objectUrl"
        :src="signaturePdfPreviewDialog.objectUrl"
        class="signature-pdf-preview__frame"
        title="签名证据 PDF 预览"
      ></iframe>
      <el-empty
        v-else-if="
          !signaturePdfPreviewDialog.loading && !signaturePdfPreviewDialog.inlineError
        "
        description="暂无可预览 PDF"
      />
    </div>
    <template #footer>
      <el-button @click="signaturePdfPreviewDialog.visible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="authorizationActionDialog.visible"
    :title="authorizationActionDialogTitle"
    width="520px"
    destroy-on-close
  >
    <el-alert
      v-if="authorizationActionDialog.inlineError"
      :closable="false"
      class="mb-16px"
      show-icon
      type="error"
      :title="authorizationActionDialog.inlineError"
    />
    <el-descriptions v-if="authorizationActionDialog.target" class="mb-16px" :column="1" border>
      <el-descriptions-item label="目标用户">
        {{ getAuthorizationUserLabel(authorizationActionDialog.target) }}
      </el-descriptions-item>
      <el-descriptions-item label="目标状态">
        {{ authorizationActionTargetText }}
      </el-descriptions-item>
    </el-descriptions>
    <el-form label-width="82px">
      <el-form-item label="原因" :error="authorizationActionDialog.reasonError">
        <el-input
          v-model="authorizationActionDialog.reason"
          :autosize="{ minRows: 3, maxRows: 6 }"
          placeholder="请输入变更原因"
          type="textarea"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeAuthorizationActionDialog">取消</el-button>
      <el-button
        type="primary"
        :loading="authorizationActionDialog.submitting"
        @click="submitAuthorizationActionDialog"
      >
        确认
      </el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="authorizationAuditDrawer.visible" title="授权审计" size="760px">
    <el-alert
      v-if="authorizationAuditDrawer.inlineError"
      :closable="false"
      class="mb-16px"
      show-icon
      type="error"
      :title="authorizationAuditDrawer.inlineError"
    />
    <UnifiedListTemplate
      class="dcc-signature-list-template dcc-signature-audit-template"
      table-key="dcc.electronicSignature.authorizationAudit"
      :query-model="authorizationAuditDrawer"
      :filter-definitions="emptyQuickFilterDefinitions"
      :show-quick-filter="false"
      :show-quick-filter-label="false"
      :quick-filter-state="authorizationAuditQuickFilter.state"
      :selected-filter-definition="authorizationAuditQuickFilter.selectedDefinition.value"
      :operator-options="authorizationAuditQuickFilter.operatorOptions.value"
      :columns="authorizationAuditColumns"
      :column-saving="authorizationAuditColumnSaving"
      :total="authorizationAuditDrawer.total"
      v-model:page="authorizationAuditDrawer.pageNo"
      v-model:limit="authorizationAuditDrawer.pageSize"
      @update:quick-filter-state="authorizationAuditQuickFilter.updateState"
      @quick-filter-query="authorizationAuditQuickFilter.applyQuickFilter"
      @column-change="saveAuthorizationAuditColumnConfig"
      @column-reset="resetAuthorizationAuditColumnConfig"
      @pagination="loadAuthorizationAuditPage"
    >
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="authorizationAuditDrawer.loading"
          data-user-table-column-explicit
          data-user-table-key="dcc.electronicSignature.authorizationAudit"
          :data="authorizationAuditDrawer.list"
          empty-text="暂无授权审计"
          @header-dragend="handleAuthorizationAuditHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isAuthorizationAuditColumnVisible('operatedAt')"
            label="操作时间"
            align="center"
            :width="getAuthorizationAuditColumnWidthString('operatedAt', 180)"
            prop="operatedAt" :formatter="dateTimeValueFormatter"
            v-bind="sortColumnAttrs('operatedAt')"
          />
          <el-table-column
            v-if="isAuthorizationAuditColumnVisible('operatorName')"
            label="操作人"
            :min-width="getAuthorizationAuditColumnMinWidthString('operatorName', 140)"
            prop="operatorName"
            v-bind="sortColumnAttrs('operatorName')"
          />
          <el-table-column
            v-if="isAuthorizationAuditColumnVisible('beforeState')"
            label="变更前"
            align="center"
            :width="getAuthorizationAuditColumnWidthString('beforeState', 120)"
            prop="beforeState"
            v-bind="sortColumnAttrs('beforeState')"
          >
            <template #default="{ row }">
              <el-tag :type="getDccSignatureAuthorizationStateTagType(row.beforeState)" size="small">
                {{ getDccSignatureAuthorizationStateLabel(row.beforeState) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isAuthorizationAuditColumnVisible('afterState')"
            label="变更后"
            align="center"
            :width="getAuthorizationAuditColumnWidthString('afterState', 120)"
            prop="afterState"
            v-bind="sortColumnAttrs('afterState')"
          >
            <template #default="{ row }">
              <el-tag :type="getDccSignatureAuthorizationStateTagType(row.afterState)" size="small">
                {{ getDccSignatureAuthorizationStateLabel(row.afterState) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isAuthorizationAuditColumnVisible('reason')"
            label="原因"
            :min-width="getAuthorizationAuditColumnMinWidthString('reason', 260)"
            prop="reason"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('reason')"
          />
        </el-table>
      </template>
    </UnifiedListTemplate>
  </el-drawer>
</template>

<script lang="ts" setup>
import { ContentWrap } from '@/components/ContentWrap'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
import {
  downloadDccSignatureEvidenceExport,
  fetchDccSignatureEvidencePdfArtifact,
  getDccElectronicSignatureAuthorizationAuditPage,
  getDccElectronicSignatureAuthorizationPage,
  getDccElectronicSignaturePage,
  unlockDccElectronicSignatureAuthorization,
  updateDccElectronicSignatureAuthorization,
  type DccElectronicSignatureAuthorizationAuditVO,
  type DccElectronicSignatureAuthorizationVO,
  type DccElectronicSignatureVO
} from '@/api/dcc/controlledFile/signatures'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { getDccControlledFileStatusLabel, getDccControlledFileStatusTagType } from '../shared/lifecycle'
import {
  DCC_CONTROLLED_COPY_HASH_STATUS_OPTIONS,
  DCC_SIGNATURE_AUTHORIZATION_STATE_OPTIONS,
  DCC_SIGNATURE_EVIDENCE_STATUS_OPTIONS,
  DCC_SIGNATURE_MEANING_OPTIONS,
  DCC_SIGNATURE_TASK_ACTION_OPTIONS,
  formatDccHashShort,
  getDccControlledCopyHashStatusLabel,
  getDccControlledCopyHashStatusTagType,
  getDccSignatureAuthorizationStateLabel,
  getDccSignatureAuthorizationStateTagType,
  getDccSignatureEvidenceStatusLabel,
  getDccSignatureEvidenceStatusTagType,
  getDccSignatureMeaningLabel,
  getDccSignatureTaskActionLabel
} from '../shared/signature-evidence'
import { formatDccSimpleUserLabel } from '../shared/utils'
import { openControlledFileViewer } from '../shared/viewer-navigation'
import { dateTimeValueFormatter, formatDateTimeValue } from '@/utils/formatTime'

defineOptions({ name: 'DccControlledFileSignatures' })

type ActiveTab = 'records' | 'authorizations'
type AuthorizationActionMode = 'enable' | 'disable' | 'unlock'
type ControlledFileStatusLike =
  | 'DRAFT'
  | 'PENDING_DOC_CONTROL_REVIEW'
  | 'PENDING_MATRIX_REVIEW'
  | 'PENDING_MATRIX_APPROVAL'
  | 'PENDING_DOC_CONTROL_APPROVAL'
  | 'FINALIZING'
  | 'TRAINING_IN_PROGRESS'
  | 'PENDING_MANUAL_DISTRIBUTION'
  | 'ACTIVE'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'OBSOLETE'
  | 'SUPERSEDED'
  | 'FINALIZATION_FAILED'
  | undefined

const props = withDefaults(
  defineProps<{
    embedded?: boolean
    initialTab?: ActiveTab
    singleTab?: ActiveTab
  }>(),
  {
    embedded: false,
    initialTab: 'records'
  }
)
const route = useRoute()
const router = useRouter()
const message = useMessage()

const activeTab = ref<ActiveTab>(props.singleTab || props.initialTab)
const isEmbedded = computed(() => props.embedded)
const signaturePageShell = computed(() => (isEmbedded.value ? 'div' : ContentWrap))
const singleTab = computed(() => props.singleTab)
const signerOptions = ref<UserVO[]>([])
const isAdvancedSignatureView = computed(() => false)

const recordDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'fileName', label: '文件名称', minWidth: 220 },
  { key: 'fileNumber', label: '文件编号', minWidth: 150 },
  { key: 'revisionId', label: '修订ID', width: 110 },
  { key: 'versionNo', label: '版本', width: 100 },
  { key: 'controlledFileStatus', label: '文件状态', width: 120 },
  { key: 'signer', label: '签名人', minWidth: 170 },
  { key: 'actorDeptPost', label: '部门/岗位', minWidth: 180 },
  { key: 'actorRoleNamesSnapshot', label: '角色', minWidth: 150 },
  { key: 'signatureSummary', label: '签名摘要', minWidth: 260 },
  { key: 'sourceFileHash', label: '源文件 hash', width: 150 },
  { key: 'controlledCopyHash', label: '副本 hash', width: 150 },
  { key: 'evidenceSummary', label: '证据摘要', minWidth: 260 },
  { key: 'evidenceHash', label: '证据 hash', width: 150 },
  { key: 'operation', label: '操作', width: 180, hideable: false, business: false }
]
const {
  columns: recordColumns,
  saving: recordColumnSaving,
  isColumnVisible: isRecordColumnVisible,
  getColumnWidthString: getRecordColumnWidthString,
  getColumnMinWidthString: getRecordColumnMinWidthString,
  handleHeaderDragend: handleRecordHeaderDragend,
  saveConfig: saveRecordColumnConfig
} = useUserTableColumns('dcc.electronicSignature.records', recordDefaultColumns)

const authorizationDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'user', label: '用户', minWidth: 180 },
  { key: 'deptName', label: '部门', minWidth: 150 },
  { key: 'mobile', label: '手机号', minWidth: 140 },
  { key: 'status', label: '用户状态', width: 100 },
  { key: 'authorizationState', label: '授权状态', width: 110 },
  { key: 'electronicSignatureEnabled', label: '启用签名', width: 150 },
  { key: 'locked', label: '锁定', width: 100 },
  { key: 'lockedUntil', label: '锁定至', width: 180 },
  { key: 'latestAuditReason', label: '最新审计原因', minWidth: 220 },
  { key: 'latestAuditOperatorName', label: '最新审计人', minWidth: 140 },
  { key: 'latestAuditAt', label: '最新审计时间', width: 180 },
  { key: 'operation', label: '操作', width: 150, hideable: false, business: false }
]
const {
  columns: authorizationColumns,
  saving: authorizationColumnSaving,
  isColumnVisible: isAuthorizationColumnVisible,
  getColumnWidthString: getAuthorizationColumnWidthString,
  getColumnMinWidthString: getAuthorizationColumnMinWidthString,
  handleHeaderDragend: handleAuthorizationHeaderDragend,
  saveConfig: saveAuthorizationColumnConfig
} = useUserTableColumns('dcc.electronicSignature.authorizations', authorizationDefaultColumns)

const authorizationAuditDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'operatedAt', label: '操作时间', width: 180 },
  { key: 'operatorName', label: '操作人', minWidth: 140 },
  { key: 'beforeState', label: '变更前', width: 120 },
  { key: 'afterState', label: '变更后', width: 120 },
  { key: 'reason', label: '原因', minWidth: 260 }
]
const {
  columns: authorizationAuditColumns,
  saving: authorizationAuditColumnSaving,
  isColumnVisible: isAuthorizationAuditColumnVisible,
  getColumnWidthString: getAuthorizationAuditColumnWidthString,
  getColumnMinWidthString: getAuthorizationAuditColumnMinWidthString,
  handleHeaderDragend: handleAuthorizationAuditHeaderDragend,
  saveConfig: saveAuthorizationAuditColumnConfig,
  resetConfig: resetAuthorizationAuditColumnConfig
} = useUserTableColumns(
  'dcc.electronicSignature.authorizationAudit',
  authorizationAuditDefaultColumns
)

const recordLoading = ref(false)
const recordTotal = ref(0)
const recordList = ref<DccElectronicSignatureVO[]>([])
const exportingControlledFileIds = ref<number[]>([])
const previewingControlledFileIds = ref<number[]>([])
const recordQueryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  controlledFileId: undefined as number | undefined,
  fileNumber: '',
  revisionId: undefined as number | undefined,
  versionNo: '',
  signerUserId: undefined as number | undefined,
  taskActionResult: undefined as string | undefined,
  meaningCode: undefined as string | undefined,
  controlledCopyHashStatus: undefined as string | undefined,
  evidenceStatus: undefined as string | undefined,
  evidenceHashShort: '',
  quickFilter: undefined as TableQuickFilterValue | undefined
})

const authorizationLoading = ref(false)
const authorizationTotal = ref(0)
const authorizationList = ref<DccElectronicSignatureAuthorizationVO[]>([])
const authorizationUpdatingUserIds = ref<number[]>([])
const authorizationQueryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  username: '',
  mobile: '',
  status: undefined as number | undefined,
  authorizationState: undefined as string | undefined,
  locked: undefined as boolean | undefined,
  quickFilter: undefined as TableQuickFilterValue | undefined
})

const recordQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  { key: 'fileNumber', label: '文件编号', type: 'text', queryParamKey: 'fileNumber', placeholder: '请输入文件编号' },
  { key: 'versionNo', label: '版本', type: 'text', queryParamKey: 'versionNo', placeholder: '请输入版本' },
  {
    key: 'signerUserId',
    label: '签名人',
    type: 'select',
    queryParamKey: 'signerUserId',
    options: signerOptions.value.map((item) => ({
      label: formatDccSimpleUserLabel(item),
      value: item.id
    }))
  },
  {
    key: 'taskActionResult',
    label: '签名动作',
    type: 'select',
    queryParamKey: 'taskActionResult',
    options: DCC_SIGNATURE_TASK_ACTION_OPTIONS
  },
  {
    key: 'meaningCode',
    label: '签名含义',
    type: 'select',
    queryParamKey: 'meaningCode',
    options: DCC_SIGNATURE_MEANING_OPTIONS
  },
  {
    key: 'controlledCopyHashStatus',
    label: '副本摘要',
    type: 'select',
    queryParamKey: 'controlledCopyHashStatus',
    options: DCC_CONTROLLED_COPY_HASH_STATUS_OPTIONS
  },
  {
    key: 'evidenceStatus',
    label: '证据状态',
    type: 'select',
    queryParamKey: 'evidenceStatus',
    options: DCC_SIGNATURE_EVIDENCE_STATUS_OPTIONS
  },
  ...(isAdvancedSignatureView.value
    ? [
        {
          key: 'evidenceHashShort',
          label: '证据 hash',
          type: 'text' as const,
          queryParamKey: 'evidenceHashShort',
          placeholder: '请输入短码'
        }
      ]
    : [])
])

const authorizationQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'username', label: '用户账号', type: 'text', queryParamKey: 'username', placeholder: '请输入用户账号' },
  { key: 'mobile', label: '手机号', type: 'text', queryParamKey: 'mobile', placeholder: '请输入手机号' },
  {
    key: 'status',
    label: '用户状态',
    type: 'select',
    queryParamKey: 'status',
    options: [
      { label: '启用', value: 0 },
      { label: '禁用', value: 1 }
    ]
  },
  {
    key: 'authorizationState',
    label: '授权状态',
    type: 'select',
    queryParamKey: 'authorizationState',
    options: DCC_SIGNATURE_AUTHORIZATION_STATE_OPTIONS
  },
  {
    key: 'locked',
    label: '锁定状态',
    type: 'select',
    queryParamKey: 'locked',
    options: [
      { label: '已锁定', value: true },
      { label: '未锁定', value: false }
    ]
  }
]

const signaturePdfPreviewDialog = reactive({
  visible: false,
  loading: false,
  inlineError: '',
  fileName: '',
  objectUrl: '',
  target: undefined as DccElectronicSignatureVO | undefined
})

const authorizationActionDialog = reactive({
  visible: false,
  submitting: false,
  mode: 'enable' as AuthorizationActionMode,
  target: undefined as DccElectronicSignatureAuthorizationVO | undefined,
  nextEnabled: false,
  reason: '',
  reasonError: '',
  inlineError: ''
})

const authorizationAuditDrawer = reactive({
  visible: false,
  loading: false,
  inlineError: '',
  target: undefined as DccElectronicSignatureAuthorizationVO | undefined,
  list: [] as DccElectronicSignatureAuthorizationAuditVO[],
  pageNo: 1,
  pageSize: 10,
  total: 0,
  quickFilter: undefined as TableQuickFilterValue | undefined
})

const toControlledFileStatus = (status: string | undefined): ControlledFileStatusLike => {
  const allowed = new Set<ControlledFileStatusLike>([
    'DRAFT',
    'PENDING_DOC_CONTROL_REVIEW',
    'PENDING_MATRIX_REVIEW',
    'PENDING_MATRIX_APPROVAL',
    'PENDING_DOC_CONTROL_APPROVAL',
    'FINALIZING',
    'TRAINING_IN_PROGRESS',
    'PENDING_MANUAL_DISTRIBUTION',
    'ACTIVE',
    'REJECTED',
    'WITHDRAWN',
    'OBSOLETE',
    'SUPERSEDED',
    'FINALIZATION_FAILED',
    undefined
  ])
  return allowed.has(status as ControlledFileStatusLike)
    ? (status as ControlledFileStatusLike)
    : undefined
}

const authorizationActionDialogTitle = computed(() => {
  if (authorizationActionDialog.mode === 'unlock') {
    return '解锁电子签名'
  }
  return authorizationActionDialog.nextEnabled ? '启用电子签名授权' : '停用电子签名授权'
})

const authorizationActionTargetText = computed(() => {
  if (authorizationActionDialog.mode === 'unlock') {
    return '解除锁定'
  }
  return authorizationActionDialog.nextEnabled ? '启用授权' : '停用授权'
})

const resolveSignaturePageErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return fallback
}

const getAuthorizationUserLabel = (row: DccElectronicSignatureAuthorizationVO) => {
  const label = formatDccSimpleUserLabel({
    username: row.username,
    nickname: row.userName || row.nickname
  })
  return label === '-' ? `用户#${row.userId}` : label
}

const formatDccSignatureSnapshotValue = (value?: string | number | null) => {
  if (value === undefined || value === null) return '旧版证据未记录'
  const text = String(value).trim()
  return text || '旧版证据未记录'
}

const emptyQuickFilterDefinitions: TableQuickFilterDefinition[] = []

const loadSignerOptions = async () => {
  signerOptions.value = await getSimpleUserList()
}

const buildRecordQueryParams = () => ({
  ...recordQueryParams,
  fileNumber: recordQueryParams.fileNumber?.trim() || undefined,
  revisionId: isAdvancedSignatureView.value ? recordQueryParams.revisionId : undefined,
  versionNo: recordQueryParams.versionNo?.trim() || undefined,
  evidenceHashShort: isAdvancedSignatureView.value
    ? recordQueryParams.evidenceHashShort?.trim() || undefined
    : undefined
})

const loadRecordPage = async () => {
  recordLoading.value = true
  try {
    const data = await getDccElectronicSignaturePage(buildRecordQueryParams())
    recordList.value = data.list
    recordTotal.value = data.total
  } catch (error) {
    message.error(resolveSignaturePageErrorMessage(error, '签名记录加载失败，请查看错误提示后重试。'))
  } finally {
    recordLoading.value = false
  }
}

const loadAuthorizationPage = async () => {
  authorizationLoading.value = true
  try {
    const data = await getDccElectronicSignatureAuthorizationPage({
      ...authorizationQueryParams,
      username: authorizationQueryParams.username?.trim() || undefined,
      mobile: authorizationQueryParams.mobile?.trim() || undefined
    })
    authorizationList.value = data.list
    authorizationTotal.value = data.total
  } catch (error) {
    message.error(resolveSignaturePageErrorMessage(error, '签名授权加载失败，请查看错误提示后重试。'))
  } finally {
    authorizationLoading.value = false
  }
}

const recordQuickFilter = useTableQuickFilter(
  'dcc.electronicSignature.records',
  recordQuickFilterDefinitions,
  recordQueryParams,
  loadRecordPage
)

const authorizationQuickFilter = useTableQuickFilter(
  'dcc.electronicSignature.authorizations',
  authorizationQuickFilterDefinitions,
  authorizationQueryParams,
  loadAuthorizationPage
)

const openControlledFileDetail = (controlledFileId: number) => {
  openControlledFileViewer(router, route, controlledFileId, 'signature')
}

const revokeSignaturePdfPreviewUrl = () => {
  if (!signaturePdfPreviewDialog.objectUrl) {
    return
  }
  URL.revokeObjectURL(signaturePdfPreviewDialog.objectUrl)
  signaturePdfPreviewDialog.objectUrl = ''
}

const clearSignaturePdfPreview = () => {
  revokeSignaturePdfPreviewUrl()
  signaturePdfPreviewDialog.loading = false
  signaturePdfPreviewDialog.inlineError = ''
  signaturePdfPreviewDialog.fileName = ''
  signaturePdfPreviewDialog.target = undefined
}

const isPreviewingControlledFile = (controlledFileId: number) => {
  return previewingControlledFileIds.value.includes(controlledFileId)
}

const setPreviewingControlledFile = (controlledFileId: number, loading: boolean) => {
  if (loading) {
    previewingControlledFileIds.value = [...previewingControlledFileIds.value, controlledFileId]
    return
  }
  previewingControlledFileIds.value = previewingControlledFileIds.value.filter(
    (item) => item !== controlledFileId
  )
}

const openSignaturePdfPreview = async (signature: DccElectronicSignatureVO) => {
  const controlledFileId = signature.controlledFileId
  if (!controlledFileId) {
    message.error('签名证据预览缺少受控文件 ID')
    return
  }
  signaturePdfPreviewDialog.visible = true
  signaturePdfPreviewDialog.loading = true
  signaturePdfPreviewDialog.inlineError = ''
  signaturePdfPreviewDialog.fileName = ''
  signaturePdfPreviewDialog.target = signature
  revokeSignaturePdfPreviewUrl()
  setPreviewingControlledFile(controlledFileId, true)
  try {
    const artifact = await fetchDccSignatureEvidencePdfArtifact(controlledFileId)
    signaturePdfPreviewDialog.fileName = artifact.fileName
    signaturePdfPreviewDialog.objectUrl = URL.createObjectURL(artifact.blob)
  } catch (error) {
    signaturePdfPreviewDialog.inlineError = resolveSignaturePageErrorMessage(
      error,
      '签名证据 PDF 预览失败，请查看错误提示后重试。'
    )
  } finally {
    signaturePdfPreviewDialog.loading = false
    setPreviewingControlledFile(controlledFileId, false)
  }
}

const isExportingControlledFile = (controlledFileId: number) => {
  return exportingControlledFileIds.value.includes(controlledFileId)
}

const setExportingControlledFile = (controlledFileId: number, loading: boolean) => {
  if (loading) {
    exportingControlledFileIds.value = [...exportingControlledFileIds.value, controlledFileId]
    return
  }
  exportingControlledFileIds.value = exportingControlledFileIds.value.filter(
    (item) => item !== controlledFileId
  )
}

const handleExportSignatureEvidence = async (signature: DccElectronicSignatureVO) => {
  const controlledFileId = signature.controlledFileId
  if (!controlledFileId) {
    message.error('签名证据导出缺少受控文件 ID')
    return
  }
  setExportingControlledFile(controlledFileId, true)
  try {
    await downloadDccSignatureEvidenceExport(controlledFileId)
    message.success('签名证据 PDF 已下载')
  } catch (error) {
    message.error(resolveSignaturePageErrorMessage(error, '签名证据导出失败，请查看错误提示后重试。'))
  } finally {
    setExportingControlledFile(controlledFileId, false)
  }
}

const closeAuthorizationActionDialog = () => {
  authorizationActionDialog.visible = false
  authorizationActionDialog.submitting = false
  authorizationActionDialog.target = undefined
  authorizationActionDialog.reason = ''
  authorizationActionDialog.reasonError = ''
  authorizationActionDialog.inlineError = ''
}

const openAuthorizationChangeDialog = (
  row: DccElectronicSignatureAuthorizationVO,
  nextEnabled: boolean | string | number
) => {
  authorizationActionDialog.visible = true
  authorizationActionDialog.submitting = false
  authorizationActionDialog.mode = Boolean(nextEnabled) ? 'enable' : 'disable'
  authorizationActionDialog.target = row
  authorizationActionDialog.nextEnabled = Boolean(nextEnabled)
  authorizationActionDialog.reason = ''
  authorizationActionDialog.reasonError = ''
  authorizationActionDialog.inlineError = ''
}

const openAuthorizationUnlockDialog = (row: DccElectronicSignatureAuthorizationVO) => {
  authorizationActionDialog.visible = true
  authorizationActionDialog.submitting = false
  authorizationActionDialog.mode = 'unlock'
  authorizationActionDialog.target = row
  authorizationActionDialog.nextEnabled = true
  authorizationActionDialog.reason = ''
  authorizationActionDialog.reasonError = ''
  authorizationActionDialog.inlineError = ''
}

const replaceAuthorizationRow = (nextRow: DccElectronicSignatureAuthorizationVO) => {
  const index = authorizationList.value.findIndex((item) => item.userId === nextRow.userId)
  if (index >= 0) {
    authorizationList.value[index] = {
      ...authorizationList.value[index],
      ...nextRow
    }
  }
}

const setAuthorizationUpdating = (userId: number, loading: boolean) => {
  if (loading) {
    authorizationUpdatingUserIds.value = [...authorizationUpdatingUserIds.value, userId]
    return
  }
  authorizationUpdatingUserIds.value = authorizationUpdatingUserIds.value.filter(
    (item) => item !== userId
  )
}

const submitAuthorizationActionDialog = async () => {
  const target = authorizationActionDialog.target
  if (!target) {
    return
  }
  const reason = authorizationActionDialog.reason.trim()
  if (!reason) {
    authorizationActionDialog.reasonError = '请输入变更原因'
    return
  }
  authorizationActionDialog.submitting = true
  authorizationActionDialog.reasonError = ''
  authorizationActionDialog.inlineError = ''
  setAuthorizationUpdating(target.userId, true)
  try {
    const nextRow =
      authorizationActionDialog.mode === 'unlock'
        ? await unlockDccElectronicSignatureAuthorization(target.userId, { reason })
        : await updateDccElectronicSignatureAuthorization(target.userId, {
            electronicSignatureEnabled: authorizationActionDialog.nextEnabled,
            reason
          })
    replaceAuthorizationRow(nextRow)
    message.success(`${authorizationActionTargetText.value}成功`)
    closeAuthorizationActionDialog()
  } catch (error) {
    authorizationActionDialog.inlineError = resolveSignaturePageErrorMessage(
      error,
      '签名授权变更失败，请查看错误提示后重试。'
    )
  } finally {
    setAuthorizationUpdating(target.userId, false)
    authorizationActionDialog.submitting = false
  }
}

const loadAuthorizationAuditPage = async () => {
  const target = authorizationAuditDrawer.target
  if (!target) {
    return
  }
  authorizationAuditDrawer.loading = true
  authorizationAuditDrawer.inlineError = ''
  try {
    const data = await getDccElectronicSignatureAuthorizationAuditPage(target.userId, {
      pageNo: authorizationAuditDrawer.pageNo,
      pageSize: authorizationAuditDrawer.pageSize
    })
    authorizationAuditDrawer.list = data.list
    authorizationAuditDrawer.total = data.total
  } catch (error) {
    authorizationAuditDrawer.inlineError = resolveSignaturePageErrorMessage(
      error,
      '授权审计加载失败，请查看错误提示后重试。'
    )
  } finally {
    authorizationAuditDrawer.loading = false
  }
}

const authorizationAuditQuickFilter = useTableQuickFilter(
  'dcc.electronicSignature.authorizationAudit',
  emptyQuickFilterDefinitions,
  authorizationAuditDrawer,
  loadAuthorizationAuditPage
)

const openAuthorizationAudit = async (row: DccElectronicSignatureAuthorizationVO) => {
  authorizationAuditDrawer.visible = true
  authorizationAuditDrawer.target = row
  authorizationAuditDrawer.pageNo = 1
  authorizationAuditDrawer.pageSize = 10
  authorizationAuditDrawer.list = []
  authorizationAuditDrawer.total = 0
  authorizationAuditDrawer.inlineError = ''
  await authorizationAuditQuickFilter.resetQuickFilter()
}

const isTabVisible = (tab: ActiveTab) => !props.singleTab || props.singleTab === tab

watch(
  () => props.singleTab,
  (value) => {
    if (value) {
      activeTab.value = value
    }
  },
  { immediate: true }
)

watch(
  activeTab,
  async (value) => {
    if (value === 'authorizations' && authorizationList.value.length === 0) {
      await loadAuthorizationPage()
    }
  },
  { immediate: false }
)

onMounted(async () => {
  const tasks: Array<Promise<unknown>> = [loadSignerOptions()]
  if (isTabVisible('records')) {
    tasks.push(loadRecordPage())
  }
  if (isTabVisible('authorizations')) {
    tasks.push(loadAuthorizationPage())
  }
  await Promise.all(tasks)
})

onBeforeUnmount(() => {
  revokeSignaturePdfPreviewUrl()
})
</script>

<style scoped>
.signature-hash {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  color: var(--el-text-color-regular);
  word-break: break-all;
}

.dcc-signature-page--embedded {
  margin: 0;
}

.dcc-signature-page__single-tab :deep(.el-tabs__header) {
  display: none;
}

.signature-pdf-preview {
  display: flex;
  align-items: stretch;
  justify-content: center;
  min-height: 640px;
  overflow: hidden;
  background: #f7f9fc;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.signature-pdf-preview__frame {
  display: block;
  width: 100%;
  height: 640px;
  background: #ffffff;
  border: 0;
}

.signature-record-summary,
.signature-evidence-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  font-size: 12px;
  line-height: 18px;
}

.signature-record-summary__line,
.signature-evidence-summary__line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.signature-record-summary__main {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.signature-record-summary__meta {
  overflow: hidden;
  color: #4b5563;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.signature-evidence-summary__label {
  flex: 0 0 32px;
  color: #4b5563;
}

.signature-action-row {
  display: flex;
  justify-content: center;
  gap: 8px;
}

@media (max-width: 760px) {
  .signature-pdf-preview {
    min-height: 520px;
  }

  .signature-pdf-preview__frame {
    height: 520px;
  }
}
</style>
