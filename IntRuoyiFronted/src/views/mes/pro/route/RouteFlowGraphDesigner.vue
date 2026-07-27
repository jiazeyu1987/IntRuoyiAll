<template>
  <div
    class="route-flow-graph-designer"
    :class="{ 'is-maximized': isRouteFlowMaximized }"
    :data-flow-layout-revision="autoLayoutRevision"
  >
    <div class="route-flow-graph-designer__toolbar">
      <div class="route-flow-graph-designer__summary">
        <el-button
          class="route-flow-graph-designer__toolbar-back"
          data-flow-action="back-route-list"
          @click="handleRequestBack"
        >
          <Icon icon="ep:back" class="mr-5px" />
          返回
        </el-button>
        <div class="route-flow-graph-designer__route-title">
          <span class="route-flow-graph-designer__route-name" :title="props.routeName">
            {{ props.routeName }}
          </span>
          <div
            aria-label="工艺路线版本标识"
            class="route-flow-graph-designer__version-summary"
            data-flow-status="route-version-summary"
          >
            <span
              class="route-flow-graph-designer__version-pill route-flow-graph-designer__version-pill--current"
              :title="currentRouteVersionViewLabel"
            >
              {{ currentRouteVersionViewLabel }}
            </span>
          </div>
        </div>
      </div>
      <span
        v-if="selectedNode"
        aria-live="polite"
        class="route-flow-graph-designer__selected-full-name"
        :title="selectedNodeFullName"
      >
        {{ selectedNodeFullName }}
      </span>
      <div class="route-flow-graph-designer__actions">
        <el-input
          v-model="searchKeyword"
          class="route-flow-graph-designer__search"
          clearable
          placeholder="搜索工序"
          @input="handleSearch"
        >
          <template #prefix>
            <Icon icon="ep:search" />
          </template>
        </el-input>
        <el-button
          data-flow-action="toggle-route-flow-maximize"
          :aria-pressed="isRouteFlowMaximized"
          @click="handleToggleRouteFlowMaximized"
        >
          <Icon :icon="isRouteFlowMaximized ? 'ep:close' : 'ep:full-screen'" class="mr-5px" />
          {{ isRouteFlowMaximized ? '恢复' : '最大化' }}
        </el-button>
        <el-button
          :disabled="routeFlowWriteControlsDisabled || loading || routeNodes.length === 0"
          @click="handleAutoLayout"
        >
          <Icon icon="ep:rank" class="mr-5px" />
          自动布局
        </el-button>
        <el-button
          v-if="showRouteFlowMutationControls"
          data-flow-action="add-route-process"
          :disabled="routeFlowWriteControlsDisabled || loading || processOptionsLoading || routeProcessSaving"
          plain
          type="primary"
          @click="handleOpenRouteProcessAddDialog"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          添加工序
        </el-button>
        <div
          v-if="showRouteFlowMutationControls"
          class="route-flow-graph-designer__connection-control"
        >
          <el-button
            data-flow-action="connect-route-process"
            :disabled="routeFlowWriteControlsDisabled || loading || routeNodes.length < 1"
            plain
            type="primary"
            @click="handleConnectionPopoverToggle"
          >
            <Icon icon="ep:share" class="mr-5px" />
            连接工序
          </el-button>
          <div
            v-if="connectionPopoverVisible"
            class="route-flow-graph-designer__connection-popover"
            data-flow-panel="connection-selector"
            @click.stop
          >
            <div class="route-flow-graph-designer__connection-selector">
              <label class="route-flow-graph-designer__connection-field">
                <span>起始工序</span>
                <el-autocomplete
                  v-model="connectionSourceInputText"
                  clearable
                  data-flow-field="connection-source"
                  :disabled="routeFlowWriteControlsDisabled"
                  fit-input-width
                  :fetch-suggestions="queryConnectionSourceSuggestions"
                  placeholder="请选择起始工序"
                  :teleported="false"
                  trigger-on-focus
                  value-key="value"
                  @clear="handleConnectionSourceClear"
                  @select="handleConnectionSourceSelect"
                  @update:model-value="handleConnectionSourceInput"
                />
              </label>
              <Icon class="route-flow-graph-designer__connection-arrow" icon="ep:right" />
              <label class="route-flow-graph-designer__connection-field">
                <span>目标工序</span>
                <el-autocomplete
                  v-model="connectionTargetInputText"
                  clearable
                  data-flow-field="connection-target"
                  :disabled="routeFlowWriteControlsDisabled"
                  fit-input-width
                  :fetch-suggestions="queryConnectionTargetSuggestions"
                  placeholder="请选择目标工序"
                  :teleported="false"
                  trigger-on-focus
                  value-key="value"
                  @clear="handleConnectionTargetClear"
                  @select="handleConnectionTargetSelect"
                  @update:model-value="handleConnectionTargetInput"
                />
              </label>
              <el-button
                :disabled="connectionConfirmDisabled"
                data-flow-action="confirm-route-process-connection"
                type="primary"
                @click="handleConfirmConnection"
              >
                确认
              </el-button>
              <div
                v-if="connectionPreviousIncomingEdge"
                class="route-flow-graph-designer__connection-replacement"
                data-flow-hint="connection-replacement"
              >
                <Icon icon="ep:warning" />
                <span>
                  目标工序当前入口为「{{ nodeLabel(connectionPreviousIncomingSource) }}」，
                  确认后将替换为「{{ nodeLabel(connectionSelectedSource) }}」。
                </span>
              </div>
            </div>
          </div>
        </div>
        <el-tag
          v-if="graphDirty"
          class="route-flow-graph-designer__unsaved"
          data-flow-status="unsaved"
          effect="dark"
          type="warning"
        >
          未保存
        </el-tag>
        <el-button
          v-if="showRouteFlowMutationControls"
          class="route-flow-graph-designer__toolbar-save"
          data-flow-action="save-route-flow"
          :disabled="routeFlowWriteControlsDisabled || props.submitting || loading || saving || routeProcessSaving"
          type="primary"
          @click="handleRequestSubmit"
        >
          保 存
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="route-flow-graph-designer__main">
      <aside
        :aria-busy="
          selectedProcessDetailLoading ||
          selectedProcessMachineryLoading ||
          selectedProcessAttributesLoading
        "
        class="route-flow-graph-designer__process-detail-sidebar"
        data-flow-panel="selected-process-detail"
      >
        <el-empty v-if="routeNodes.length === 0" :image-size="46" description="暂无工序" />
        <template v-else-if="selectedBoundaryType">
          <div
            class="route-flow-graph-designer__boundary-detail"
            data-flow-panel="selected-boundary-detail"
          >
            <h4>{{ boundaryLabel(selectedBoundaryType) }}</h4>
            <template v-if="selectedBoundaryType === 'START'">
              <div class="route-flow-graph-designer__selected-detail-list">
                <div
                  class="route-flow-graph-designer__selected-detail-item"
                  :class="{ 'is-selected': selectedBoundaryDetailFieldKey === 'batchRecordAttachment' }"
                  data-flow-boundary-field="batchRecordAttachment"
                >
                  <div class="route-flow-graph-designer__selected-detail-content">
                    <button
                      aria-label="查看批记录附件负责人字段明细"
                      :aria-pressed="selectedBoundaryDetailFieldKey === 'batchRecordAttachment'"
                      class="route-flow-graph-designer__selected-detail-button"
                      data-flow-action="select-boundary-detail-field"
                      title="查看批记录附件负责人字段明细"
                      type="button"
                      @click="handleSelectBoundaryDetailField('batchRecordAttachment')"
                    >
                      <span>批记录附件</span>
                    </button>
                  </div>
                </div>
              </div>
            </template>
            <template v-else-if="selectedBoundaryType === 'END'">
              <div class="route-flow-graph-designer__selected-detail-list">
                <div
                  class="route-flow-graph-designer__selected-detail-item"
                  :class="{ 'is-selected': selectedBoundaryDetailFieldKey === 'releaseOwner' }"
                  data-flow-boundary-field="releaseOwner"
                >
                  <div class="route-flow-graph-designer__selected-detail-content">
                    <button
                      aria-label="查看放行责任人字段明细"
                      :aria-pressed="selectedBoundaryDetailFieldKey === 'releaseOwner'"
                      class="route-flow-graph-designer__selected-detail-button"
                      data-flow-action="select-boundary-detail-field"
                      title="查看放行责任人字段明细"
                      type="button"
                      @click="handleSelectBoundaryDetailField('releaseOwner')"
                    >
                      <span>放行责任人</span>
                    </button>
                  </div>
                </div>
              </div>
            </template>
            <template v-else>
              <p>边界节点属性暂为只读，本次仅维护与工序的连接关系。</p>
              <el-empty
                v-if="selectedBoundaryRelations.length === 0"
                :image-size="38"
                description="暂无边界关系"
              />
              <div v-else class="route-flow-graph-designer__boundary-relation-list">
                <div
                  v-for="edge in selectedBoundaryRelations"
                  :key="boundaryEdgeKey(edge)"
                  class="route-flow-graph-designer__boundary-relation-item"
                >
                  <span>{{ boundaryEdgeSourceLabel(edge) }}</span>
                  <Icon icon="ep:right" />
                  <strong>{{ boundaryEdgeTargetLabel(edge) }}</strong>
                </div>
              </div>
            </template>
          </div>
        </template>
        <el-empty v-else-if="!selectedNode" :image-size="46" description="请选择工序查看详情" />
        <template v-else>
          <div
            v-if="showRouteFlowMutationControls"
            class="route-flow-graph-designer__selected-actions"
          >
            <el-button
              data-flow-action="delete-route-process"
              :disabled="routeFlowWriteControlsDisabled || routeProcessSaving"
              plain
              size="small"
              type="danger"
              @click="handleRouteProcessDelete"
            >
              <Icon icon="ep:delete" class="mr-5px" />
              删除工序
            </el-button>
          </div>
          <div class="route-flow-graph-designer__process-detail-field-picker">
            <el-select
              v-model="selectedProcessDetailFieldToAdd"
              data-flow-field="process-config-item-select"
              :disabled="
                processDetailInterestMutationDisabled ||
                processDetailFieldSelectOptions.length === 0
              "
              filterable
              placeholder="添加配置项"
              size="small"
            >
              <el-option
                v-for="field in processDetailFieldSelectOptions"
                :key="field.key"
                :label="field.label"
                :value="field.key"
                :disabled="field.disabled"
              />
            </el-select>
            <el-button
              :disabled="processDetailInterestMutationDisabled || !selectedProcessDetailFieldToAdd"
              circle
              data-flow-action="add-process-config-item"
              size="small"
              type="primary"
              @click="handleAddProcessDetailField"
            >
              <Icon icon="ep:plus" />
            </el-button>
          </div>
          <el-empty
            v-if="selectedProcessDetailFields.length === 0"
            :image-size="38"
            description="从下拉选择配置项后点击加号添加"
          />
          <div v-else class="route-flow-graph-designer__selected-detail-list">
            <div
              v-for="field in selectedProcessDetailFields"
              :key="field.key"
              :data-flow-detail-field="field.key"
              :data-capacity-source-focus="
                field.key === highlightedProcessDetailFieldKey ? 'true' : undefined
              "
              class="route-flow-graph-designer__selected-detail-item"
              :class="{
                'is-capacity-source-focus': field.key === highlightedProcessDetailFieldKey,
                'is-selected': selectedProcessDetailFieldKey === field.key
              }"
            >
              <div
                :aria-busy="field.loading"
                class="route-flow-graph-designer__selected-detail-content"
              >
                <button
                  :aria-label="`查看${field.label}字段明细`"
                  :aria-pressed="selectedProcessDetailFieldKey === field.key"
                  class="route-flow-graph-designer__selected-detail-button"
                  data-flow-action="select-process-detail-field"
                  data-flow-detail-field-button
                  :title="`查看${field.label}字段明细`"
                  type="button"
                  @click="handleSelectProcessDetailField(field.key)"
                >
                  <span>{{ field.label }}</span>
                </button>
              </div>
              <el-button
                :disabled="processDetailInterestMutationDisabled"
                circle
                data-flow-action="remove-process-detail-field"
                size="small"
                type="danger"
                @click.stop="handleRemoveProcessDetailField(field.key)"
              >
                <Icon icon="ep:delete" />
              </el-button>
            </div>
          </div>
        </template>
      </aside>
      <div
        ref="graphCanvasRef"
        class="route-flow-graph-designer__canvas"
        :class="{ 'is-readonly': !canMutateRouteFlow }"
      >
        <VueFlow
          v-model:edges="displayFlowEdges"
          v-model:nodes="displayFlowNodes"
          class="route-flow-graph-designer__flow"
          :connection-mode="ConnectionMode.Strict"
          :default-edge-options="defaultEdgeOptions"
          :delete-key-code="null"
          :edges-connectable="canMutateRouteFlow"
          :edges-updatable="canMutateRouteFlow"
          :fit-view-on-init="false"
          :max-zoom="1.2"
          :min-zoom="0.2"
          :nodes-connectable="canMutateRouteFlow"
          :nodes-draggable="canMutateRouteFlow"
          :nodes-focusable="true"
          :pan-on-drag="true"
          :prevent-scrolling="true"
          @connect="handleConnect"
          @edge-click="handleEdgeClick"
          @edge-update="handleEdgeUpdate"
          @edges-change="handleEdgesChange"
          @node-click="handleNodeClick"
          @node-drag-stop="handleNodeDragStop"
          @nodes-change="handleNodesChange"
        >
          <template #node-route-process="{ data }">
            <button
              class="route-flow-graph-designer__node"
              :class="{
                'is-key': data.routeNode.keyFlag,
                'has-flags': data.routeNode.keyFlag || data.routeNode.checkFlag,
                'is-invalid': invalidRouteProcessIds.has(data.routeNode.routeProcessId),
                'is-highlight': highlightedRouteProcessId === data.routeNode.routeProcessId,
                'is-selected': selectedRouteProcessId === data.routeNode.routeProcessId,
                'is-binding-bound': getRouteNodeBindingStatus(data.routeNode) === 'bound',
                'is-binding-missing': getRouteNodeBindingStatus(data.routeNode) === 'missing'
              }"
              data-flow-node="route-process"
              :data-route-process-id="data.routeNode.routeProcessId"
              type="button"
              @click="handleRouteProcessNodeClick(data.routeNode)"
              @keydown="handleRouteProcessNodeKeydown($event, data.routeNode)"
              @pointerdown="handleNodePointerDown(data.routeNode)"
            >
              <span class="route-flow-graph-designer__node-sort">{{
                data.routeNode.sort || '-'
              }}</span>
              <span class="route-flow-graph-designer__node-name">{{
                nodeLabel(data.routeNode)
              }}</span>
              <span
                v-if="
                  selectedProcessDetailFieldKey === FORM_SLOT_AGGREGATE_FIELD_KEY &&
                  getRouteNodeAdditionalFormCount(data.routeNode) > 0
                "
                class="route-flow-graph-designer__node-form-count-badge"
                :aria-label="`已绑定 ${getRouteNodeAdditionalFormCount(data.routeNode)} 个表单`"
                :title="`已绑定 ${getRouteNodeAdditionalFormCount(data.routeNode)} 个表单`"
              >
                {{ getRouteNodeAdditionalFormCount(data.routeNode) }}
              </span>
              <span
                v-if="data.routeNode.keyFlag || data.routeNode.checkFlag"
                class="route-flow-graph-designer__node-flags"
              >
                <el-tag v-if="data.routeNode.keyFlag" size="small" type="warning" effect="light">
                  关键
                </el-tag>
                <el-tag v-if="data.routeNode.checkFlag" size="small" type="success" effect="light">
                  质检
                </el-tag>
              </span>
              <Handle
                id="target-left"
                class="route-flow-graph-designer__handle is-in is-left"
                data-flow-handle="target"
                :data-route-process-id="data.routeNode.routeProcessId"
                type="target"
                :position="Position.Left"
              />
              <Handle
                id="source-right"
                class="route-flow-graph-designer__handle is-out is-right"
                data-flow-handle="source"
                :data-route-process-id="data.routeNode.routeProcessId"
                type="source"
                :position="Position.Right"
                @pointerdown="handlePortPointerDown(data.routeNode)"
              />
              <Handle
                id="target-top"
                class="route-flow-graph-designer__handle is-anchor is-in is-top"
                data-flow-handle="target"
                :data-route-process-id="data.routeNode.routeProcessId"
                type="target"
                :position="Position.Top"
              />
              <Handle
                id="target-right"
                class="route-flow-graph-designer__handle is-anchor is-in is-right"
                data-flow-handle="target"
                :data-route-process-id="data.routeNode.routeProcessId"
                type="target"
                :position="Position.Right"
              />
              <Handle
                id="target-bottom"
                class="route-flow-graph-designer__handle is-anchor is-in is-bottom"
                data-flow-handle="target"
                :data-route-process-id="data.routeNode.routeProcessId"
                type="target"
                :position="Position.Bottom"
              />
              <Handle
                id="source-left"
                class="route-flow-graph-designer__handle is-anchor is-out is-left"
                data-flow-handle="source"
                :data-route-process-id="data.routeNode.routeProcessId"
                type="source"
                :position="Position.Left"
                @pointerdown="handlePortPointerDown(data.routeNode)"
              />
              <Handle
                id="source-top"
                class="route-flow-graph-designer__handle is-anchor is-out is-top"
                data-flow-handle="source"
                :data-route-process-id="data.routeNode.routeProcessId"
                type="source"
                :position="Position.Top"
                @pointerdown="handlePortPointerDown(data.routeNode)"
              />
              <Handle
                id="source-bottom"
                class="route-flow-graph-designer__handle is-anchor is-out is-bottom"
                data-flow-handle="source"
                :data-route-process-id="data.routeNode.routeProcessId"
                type="source"
                :position="Position.Bottom"
                @pointerdown="handlePortPointerDown(data.routeNode)"
              />
            </button>
          </template>
          <template #node-route-boundary="{ data }">
            <button
              class="route-flow-graph-designer__boundary-node"
              :class="{ 'is-selected': selectedBoundaryType === data.boundaryType }"
              data-flow-node="route-boundary"
              :data-flow-boundary="data.boundaryType"
              type="button"
              @click="handleBoundaryNodeSelect(data.boundaryType)"
              @pointerdown="handleBoundaryNodeSelect(data.boundaryType)"
            >
              <span>{{ data.label }}</span>
              <Handle
                v-if="data.boundaryType === 'START'"
                id="source-right"
                class="route-flow-graph-designer__handle is-out is-right"
                data-flow-handle="source"
                type="source"
                :position="Position.Right"
              />
              <Handle
                v-if="data.boundaryType === 'END'"
                id="target-left"
                class="route-flow-graph-designer__handle is-in is-left"
                data-flow-handle="target"
                type="target"
                :position="Position.Left"
              />
            </button>
          </template>
        </VueFlow>

        <div v-if="routeNodes.length === 0 && !loading" class="route-flow-graph-designer__empty">
          当前路线暂无工序，请点击“添加工序”在关系图中维护。
        </div>
      </div>

      <aside class="route-flow-graph-designer__panel">
        <div
          v-if="selectedEdge || selectedBoundaryEdge"
          class="route-flow-graph-designer__panel-section route-flow-graph-designer__selected-edge-section"
        >
          <h4>当前关系</h4>
          <div class="route-flow-graph-designer__selected-edge-summary">
            <span :title="selectedRelationSourceLabel">{{ selectedRelationSourceLabel }}</span>
            <Icon icon="ep:right" />
            <span :title="selectedRelationTargetLabel">{{ selectedRelationTargetLabel }}</span>
          </div>
          <el-button
            v-if="showRouteFlowMutationControls"
            data-flow-action="delete-selected-edge"
            :disabled="routeFlowWriteControlsDisabled"
            plain
            size="small"
            type="danger"
            @click="handleSelectedEdgeDelete"
          >
            <Icon icon="ep:delete" class="mr-5px" />
            删除连接线
          </el-button>
        </div>
        <div
          class="route-flow-graph-designer__panel-section route-flow-graph-designer__selected-field-section"
          data-flow-panel="selected-field-detail"
        >
          <h4>字段明细</h4>
          <p
            v-if="!selectedProcessDetailField && !(selectedBoundaryType === 'END' && selectedBoundaryDetailFieldKey === 'releaseOwner') && !(selectedBoundaryType === 'START' && selectedBoundaryDetailFieldKey === 'batchRecordAttachment')"
            class="route-flow-graph-designer__selected-field-empty"
          >
            点击左侧字段查看明细
          </p>
          <template v-else-if="selectedBoundaryType === 'START' && selectedBoundaryDetailFieldKey === 'batchRecordAttachment'">
            <div class="route-flow-graph-designer__selected-field-grid">
              <span>当前工序</span>
              <strong>工序开始</strong>
              <span>字段名称</span>
              <strong>批记录附件</strong>
              <span>字段来源</span>
              <strong>附件上传负责人</strong>
            </div>
            <div
              v-loading="batchRecordAttachmentOwnersLoading"
              class="route-flow-graph-designer__selected-detail-editor"
              :data-flow-field-editor="selectedBoundaryDetailFieldKey"
              data-flow-panel="batch-record-attachment-owner-detail"
            >
              <el-alert
                v-if="batchRecordAttachmentOwnersLoadError"
                :title="batchRecordAttachmentOwnersLoadError"
                :closable="false"
                show-icon
                type="error"
              />
              <div class="route-flow-graph-designer__record-binding-toolbar">
                <span>批记录附件负责人</span>
                <div class="route-flow-graph-designer__record-binding-toolbar-actions">
                  <el-button
                    data-flow-action="init-batch-record-attachment-owners"
                    :disabled="batchRecordAttachmentOwnerControlsDisabled"
                    :loading="batchRecordAttachmentOwnersInitializing"
                    link
                    size="small"
                    type="primary"
                    @click="handleBatchRecordAttachmentOwnerInit"
                  >
                    初始化默认角色
                  </el-button>
                  <el-button
                    data-flow-action="save-batch-record-attachment-owners"
                    :disabled="batchRecordAttachmentOwnerControlsDisabled || batchRecordAttachmentOwners.length === 0"
                    :loading="batchRecordAttachmentOwnersSaving"
                    link
                    size="small"
                    type="primary"
                    @click="handleBatchRecordAttachmentOwnerSave"
                  >
                    保存
                  </el-button>
                </div>
              </div>
              <div class="route-flow-graph-designer__record-binding-list">
                <div
                  v-for="owner in batchRecordAttachmentOwners"
                  :key="owner.attachmentCode"
                  class="route-flow-graph-designer__record-binding-item"
                  :data-batch-record-attachment-owner="owner.attachmentCode"
                >
                  <span class="route-flow-graph-designer__record-binding-label">
                    {{ owner.attachmentName }}
                  </span>
                  <strong>{{ owner.defaultRoleName }}</strong>
                  <el-select
                    :model-value="owner.candidateSourceType"
                    data-batch-record-attachment-owner-source-type
                    :disabled="batchRecordAttachmentOwnerControlsDisabled"
                    placeholder="负责人来源"
                    size="small"
                    @change="(value) => handleBatchRecordAttachmentOwnerSourceTypeChange(owner, String(value))"
                  >
                    <el-option
                      v-for="item in BATCH_RECORD_ATTACHMENT_CANDIDATE_SOURCE_OPTIONS"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                  <el-select
                    :model-value="owner.candidateSourceIds"
                    data-batch-record-attachment-owner-candidate
                    filterable
                    multiple
                    :disabled="batchRecordAttachmentOwnerControlsDisabled"
                    :loading="isBatchRecordAttachmentOwnerCandidateOptionsLoading(owner)"
                    placeholder="请选择负责人"
                    size="small"
                    :teleported="false"
                    @change="(value) => handleBatchRecordAttachmentOwnerCandidateIdsChange(owner, value as Array<number | string>)"
                    @visible-change="(visible) => visible && loadBatchRecordAttachmentOwnerCandidateOptions(owner)"
                  >
                    <el-option
                      v-for="item in buildBatchRecordAttachmentOwnerCandidateOptions(owner)"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                  <span class="route-flow-graph-designer__selected-detail-note">
                    已授权：{{ formatBatchRecordAttachmentAssignedUsers(owner) }}
                  </span>
                </div>
              </div>
            </div>
          </template>
          <template v-else-if="selectedBoundaryType === 'END' && selectedBoundaryDetailFieldKey === 'releaseOwner'">
            <div class="route-flow-graph-designer__selected-field-grid">
              <span>当前工序</span>
              <strong>工序结束</strong>
              <span>字段名称</span>
              <strong>放行责任人</strong>
              <span>字段来源</span>
              <strong>最终放行规则</strong>
            </div>
            <div
              v-loading="releaseApprovalRuleLoading"
              class="route-flow-graph-designer__selected-detail-editor"
              :data-flow-field-editor="selectedBoundaryDetailFieldKey"
              data-flow-panel="release-owner-detail"
            >
              <el-alert
                v-if="releaseApprovalRuleLoadError"
                :title="releaseApprovalRuleLoadError"
                :closable="false"
                show-icon
                type="error"
              />
              <el-radio-group
                v-model="releaseApprovalRuleForm.candidateSourceType"
                data-flow-release-owner-source-type
                :disabled="releaseApprovalRuleControlsDisabled"
                size="small"
                @change="handleReleaseApprovalRuleSourceTypeChange"
              >
                <el-radio-button label="USER">具体人员</el-radio-button>
                <el-radio-button label="ROLE_GROUP">权限角色</el-radio-button>
              </el-radio-group>
              <el-select
                v-model="releaseApprovalRuleForm.candidateSourceId"
                data-flow-release-owner-candidate
                clearable
                filterable
                :disabled="releaseApprovalRuleControlsDisabled"
                :loading="releaseApprovalRuleCandidateOptionsLoading"
                placeholder="请选择放行责任人"
                size="small"
                :teleported="false"
                @visible-change="(visible) => visible && loadReleaseApprovalRuleCandidateOptions()"
              >
                <el-option
                  v-for="option in releaseApprovalRuleCandidateOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
              <span
                v-if="releaseApprovalRuleForm.candidateSourceType === 'ROLE_GROUP'"
                class="route-flow-graph-designer__selected-detail-note"
              >
                拥有该权限角色的人员均可放行
              </span>
              <el-button
                data-flow-action="save-release-owner"
                :disabled="releaseApprovalRuleControlsDisabled || !releaseApprovalRuleForm.candidateSourceId"
                :loading="releaseApprovalRuleSaving"
                size="small"
                type="primary"
                @click="handleReleaseApprovalRuleSave"
              >
                保存放行责任人
              </el-button>
            </div>
          </template>
          <template v-else>
            <div class="route-flow-graph-designer__selected-field-grid">
              <span>当前工序</span>
              <strong :title="selectedNodeFullName">{{ selectedNodeFullName || '-' }}</strong>
              <span>字段名称</span>
              <strong>{{ selectedProcessDetailField.label }}</strong>
              <span>字段来源</span>
              <strong>{{ selectedProcessDetailFieldSource }}</strong>
            </div>
            <div
              v-if="selectedProcessDetailField.coverageStatus"
              class="route-flow-graph-designer__selected-field-coverage"
              :class="`is-${selectedProcessDetailField.coverageStatus}`"
              data-flow-panel="selected-field-coverage-status"
            >
              {{ selectedProcessDetailField.coverageStatus === 'covered' ? '已覆盖' : '未覆盖' }}
            </div>
            <div
              v-if="selectedProcessDetailField.key === 'relationList'"
              class="route-flow-graph-designer__relation-detail"
              data-flow-panel="relation-list-detail"
            >
              <h4>关系清单</h4>
              <el-empty
                v-if="visibleRouteRelationEdges.length === 0 && visibleBoundaryRelationEdges.length === 0"
                :image-size="46"
                description="暂无关系"
              />
              <div v-else class="route-flow-graph-designer__relation-list">
                <button
                  v-for="edge in visibleBoundaryRelationEdges"
                  :key="boundaryEdgeKey(edge)"
                  class="route-flow-graph-designer__relation-item"
                  :class="{ 'is-selected': selectedEdgeKey === boundaryEdgeKey(edge) }"
                  :data-edge-key="boundaryEdgeKey(edge)"
                  data-flow-action="select-boundary-edge-list"
                  type="button"
                  @click="handleBoundaryEdgeSelect(edge)"
                >
                  <span>{{ boundaryEdgeSourceLabel(edge) }}</span>
                  <Icon icon="ep:right" />
                  <span>{{ boundaryEdgeTargetLabel(edge) }}</span>
                  <el-button
                    v-if="showRouteFlowMutationControls"
                    :data-edge-key="boundaryEdgeKey(edge)"
                    data-flow-action="delete-boundary-edge-list"
                    :disabled="routeFlowWriteControlsDisabled"
                    link
                    size="small"
                    type="danger"
                    @click.stop="handleBoundaryEdgeDelete(edge)"
                  >
                    删除
                  </el-button>
                </button>
                <button
                  v-for="edge in visibleRouteRelationEdges"
                  :key="edgeKey(edge)"
                  class="route-flow-graph-designer__relation-item"
                  :class="{ 'is-selected': selectedEdgeKey === edgeKey(edge) }"
                  :data-edge-key="edgeKey(edge)"
                  data-flow-action="select-edge-list"
                  type="button"
                  @click="handleEdgeSelect(edge)"
                >
                  <span>{{ nodeLabel(findNode(edge.sourceRouteProcessId)) }}</span>
                  <Icon icon="ep:right" />
                  <span>{{ nodeLabel(findNode(edge.targetRouteProcessId)) }}</span>
                  <el-button
                    v-if="showRouteFlowMutationControls"
                    :data-edge-key="edgeKey(edge)"
                    data-flow-action="delete-edge-list"
                    :disabled="routeFlowWriteControlsDisabled"
                    link
                    size="small"
                    type="danger"
                    @click.stop="handleEdgeDelete(edge)"
                  >
                    删除
                  </el-button>
                </button>
              </div>
            </div>
            <template v-else>
            <el-skeleton
              v-if="selectedProcessDetailField.loading"
              animated
              class="route-flow-graph-designer__process-detail-loading"
            >
              <template #template>
                <el-skeleton-item variant="text" />
              </template>
            </el-skeleton>
            <div class="route-flow-graph-designer__selected-field-value">
              <span>字段值</span>
              <template v-if="selectedProcessDetailField.key === 'formSlots'">
                <div
                  v-if="buildFormSlotViewSummaryItems().length"
                  class="route-flow-graph-designer__form-slot-view-summary"
                  data-form-slot-view-summary="true"
                >
                  <div
                    v-for="item in buildFormSlotViewSummaryItems()"
                    :key="item.key"
                    class="route-flow-graph-designer__form-slot-view-summary-item"
                    :data-form-slot-view-summary-item="item.key"
                  >
                    <strong :title="item.formName">
                      {{ item.index }}. {{ item.formName }}
                    </strong>
                    <span>填写人：{{ item.fillerSummary }}</span>
                    <span>工序独立：{{ item.processIndependentSummary }}</span>
                  </div>
                </div>
                <strong v-else :title="formatProcessDetailText(selectedProcessDetailField.value)">
                  {{ formatProcessDetailText(selectedProcessDetailField.value) }}
                </strong>
              </template>
              <strong v-else :title="formatProcessDetailText(selectedProcessDetailField.value)">
                {{ formatProcessDetailText(selectedProcessDetailField.value) }}
              </strong>
            </div>
            <div
              v-if="selectedProcessDetailField.links?.length"
              class="route-flow-graph-designer__selected-detail-links"
            >
              <el-button
                v-for="link in selectedProcessDetailField.links"
                :key="link.key"
                :title="link.label"
                data-flow-action="open-process-detail-link"
                :data-flow-detail-link-field="selectedProcessDetailField.key"
                :data-flow-detail-link-key="link.key"
                link
                size="small"
                type="primary"
                @click.stop="handleProcessDetailLinkClick(link)"
              >
                {{ link.label }}
              </el-button>
            </div>
                <div
                  v-if="selectedProcessDetailField.key === 'workstation'"
                  class="route-flow-graph-designer__workstation-detail"
                  data-testid="route-flow-workstation-capacity-override-card"
                >
                  <div
                    v-if="isSelectedProcessCapacityOverrideActive"
                    class="route-flow-graph-designer__workstation-capacity"
                  >
                    <span
                      class="route-flow-graph-designer__workstation-capacity-original"
                      data-flow-capacity="original-shift-capacity"
                    >
                      原班次产能：{{ formatRouteProcessIntegerShiftCapacity(selectedRouteProcess?.processShiftCapacityTotal) }}
                    </span>
                    <button
                      :disabled="capacityOverrideButtonDisabled"
                      :title="capacityOverrideButtonTitle"
                      class="route-flow-graph-designer__capacity-override-link"
                      data-flow-action="open-capacity-override-value"
                      type="button"
                      @click.stop="openCapacityOverrideDialog"
                    >
                      覆盖产能：{{ formatRouteProcessIntegerCapacity(selectedProcessAttributes.hourlyCapacity) }} 产能/h
                    </button>
                    <strong data-flow-capacity="override-shift-capacity">
                      覆盖班次产能：{{ formatRouteProcessIntegerShiftCapacity(selectedProcessCapacityOverrideShiftCapacity) }}
                    </strong>
                  </div>
                  <div class="route-flow-graph-designer__workstation-capacity-actions">
                    <button
                      :disabled="capacityOverrideButtonDisabled"
                      :title="capacityOverrideButtonTitle"
                      class="route-flow-graph-designer__capacity-override-button"
                      :class="{ 'is-loading': capacityOverrideSaving }"
                      data-flow-action="open-capacity-override-dialog"
                      type="button"
                      @click.stop="openCapacityOverrideDialog"
                    >
                      {{ capacityOverrideSaving ? '保存中' : '产能覆盖' }}
                    </button>
                  </div>
                </div>
                  <div
                    v-if="isProcessDetailFieldEditable(selectedProcessDetailField.key)"
                    class="route-flow-graph-designer__selected-detail-editor"
                    :data-flow-field-editor="selectedProcessDetailField.key"
                    >
                      <div
                        v-if="isFormSlotAggregateDetailField(selectedProcessDetailField.key)"
                        class="route-flow-graph-designer__record-binding-list"
                        data-form-slot-aggregate="true"
                        :data-route-process-id="selectedProcessAttributes.routeProcessId"
                        >
                        <div class="route-flow-graph-designer__record-binding-toolbar">
                          <span>动态表单列表</span>
                          <div class="route-flow-graph-designer__record-binding-toolbar-actions">
                            <el-popover
                              v-model:visible="processFormBindingCopyPopoverVisible"
                              placement="bottom"
                              trigger="click"
                              :width="360"
                              :disabled="recordBindingEditorDisabled || getProcessFormBindingCopySourceOptions().length === 0"
                              @hide="handleProcessFormBindingCopyPopoverHide"
                            >
                              <div class="route-flow-graph-designer__copy-form-binding-panel">
                                <span>选择同一路线下其他工序的表单绑定关系</span>
                                <el-select
                                  :model-value="processFormBindingCopySourceRouteProcessId"
                                  data-route-process-setting-field="copy-process-form-bindings-source"
                                  filterable
                                  placeholder="请选择来源工序"
                                  size="small"
                                  :teleported="false"
                                  @change="(value) => handleProcessFormBindingCopySourceChange(value as number | string | null)"
                                >
                                  <el-option
                                    v-for="item in getProcessFormBindingCopySourceOptions()"
                                    :key="item.value"
                                    :label="item.label"
                                    :value="item.value"
                                  />
                                </el-select>
                                <el-button
                                  data-flow-action="confirm-copy-process-form-bindings"
                                  :disabled="!processFormBindingCopySourceRouteProcessId"
                                  size="small"
                                  type="primary"
                                  @click="copySelectedProcessFormBindingsFromSource"
                                >
                                  复制到当前工序
                                </el-button>
                              </div>
                              <template #reference>
                                <el-button
                                  data-flow-action="copy-process-form-bindings"
                                  :disabled="recordBindingEditorDisabled || getProcessFormBindingCopySourceOptions().length === 0"
                                  link
                                  size="small"
                                  type="primary"
                                >
                                  复制
                                </el-button>
                              </template>
                            </el-popover>
                            <el-button
                              data-flow-action="add-form-binding"
                              :disabled="recordBindingEditorDisabled"
                              link
                              size="small"
                              type="primary"
                              @click="addSelectedRecordBinding"
                            >
                              新增表单
                            </el-button>
                          </div>
                        </div>
                        <el-empty
                          v-if="selectedRecordBindings.length === 0"
                          description="暂无表单，点击新增表单后从表单中心模板选择"
                          :image-size="56"
                        />
                        <div
                          v-for="(binding, bindingIndex) in selectedRecordBindings"
                          :key="binding.formBindingKey"
                          class="route-flow-graph-designer__record-binding-item"
                          :data-form-binding-key="binding.formBindingKey"
                        >
                          <span class="route-flow-graph-designer__record-binding-label">
                            表单 {{ bindingIndex + 1 }}
                          </span>
                          <el-select
                            :model-value="binding.formTemplateId"
                            clearable
                            data-route-process-setting-field="form-template"
                            :disabled="recordBindingEditorDisabled"
                            filterable
                            :loading="formTemplateOptionLoading"
                            placeholder="请选择表单中心模板"
                            remote
                            reserve-keyword
                            size="small"
                            @change="(templateId) => handleSelectedRecordBindingTemplateChange(binding, templateId as number | string | null)"
                            @visible-change="(visible) => visible && loadFormTemplateOptions()"
                            :remote-method="loadFormTemplateOptions"
                          >
                            <el-option
                              v-for="item in buildFormTemplateOptions(binding)"
                              :key="item.templateId"
                              :label="buildFormTemplateOptionLabel(item)"
                              :value="item.templateId"
                            />
                          </el-select>
                          <div
                            v-if="binding.formTemplateId"
                            class="route-flow-graph-designer__shared-form-binding"
                          >
                            <div class="route-flow-graph-designer__record-binding-scope">
                              <span>工序独立</span>
                              <el-switch
                                :model-value="isRecordBindingProcessIndependent(binding)"
                                data-route-process-setting-field="process-independent-switch"
                                :disabled="recordBindingEditorDisabled"
                                inline-prompt
                                active-text="开"
                                inactive-text="关"
                                @change="(value) => handleRecordBindingProcessIndependentChange(binding, Boolean(value))"
                              />
                            </div>
                            <el-select
                              :model-value="binding.candidateSourceType || ''"
                              clearable
                              data-route-process-setting-field="candidate-source-type"
                              :disabled="recordBindingEditorDisabled"
                              placeholder="覆盖填写人来源"
                              size="small"
                              @change="(value) => handleSelectedRecordBindingCandidateSourceTypeChange(binding, String(value))"
                            >
                              <el-option
                                v-for="item in RECORD_BINDING_CANDIDATE_SOURCE_OPTIONS"
                                :key="item.value"
                                :label="item.label"
                                :value="item.value"
                              />
                            </el-select>
                            <el-select
                              :model-value="getRecordBindingCandidateSourceId(binding)"
                              clearable
                              data-route-process-setting-field="candidate-source-id"
                              :disabled="recordBindingEditorDisabled || !binding.candidateSourceType"
                              filterable
                              :loading="isRecordBindingCandidateOptionsLoading(binding)"
                              placeholder="请选择填写人"
                              size="small"
                              :teleported="false"
                              @change="(value) => handleSelectedRecordBindingCandidateIdChange(binding, value as number | string | null)"
                              @visible-change="(visible) => visible && loadRecordBindingCandidateOptions(binding)"
                            >
                              <el-option
                                v-for="item in buildRecordBindingCandidateOptions(binding)"
                                :key="item.value"
                                :label="item.label"
                                :value="item.value"
                              />
                            </el-select>
                            <el-button
                              v-if="hasRecordBindingFillerOverride(binding)"
                              data-flow-action="clear-form-binding-filler-override"
                              :disabled="recordBindingEditorDisabled"
                              link
                              size="small"
                              type="primary"
                              @click="clearSelectedRecordBindingFillerOverride(binding)"
                            >
                              恢复默认
                            </el-button>
                          </div>
                          <div class="route-flow-graph-designer__record-binding-actions">
                            <el-button
                              data-flow-action="remove-form-binding"
                              :disabled="recordBindingEditorDisabled"
                              link
                              size="small"
                              type="danger"
                              @click="removeSelectedRecordBinding(binding)"
                            >
                              删除
                            </el-button>
                          </div>
                        </div>
                      </div>
                    <el-input-number
                      v-else-if="selectedProcessDetailField.key === 'productionQuantityFactor'"
                      :model-value="selectedProcessAttributes.productionQuantityFactor"
                      :min="0.000001"
                      :precision="2"
                      :step="0.01"
                      controls-position="right"
                      size="small"
                      @change="handleProductionQuantityFactorChange"
                    />
                    <el-select
                      v-else-if="selectedProcessDetailField.key === 'predecessor'"
                      :model-value="selectedPredecessorRouteProcessIds"
                      collapse-tags
                      collapse-tags-tooltip
                      filterable
                      multiple
                      placeholder="选择前置工序"
                      size="small"
                      :teleported="false"
                      @change="handlePredecessorChange"
                    >
                      <el-option
                        v-for="node in routeProcessRelationOptions"
                        :key="node.routeProcessId"
                        :label="nodeLabel(node)"
                        :value="node.routeProcessId"
                      />
                    </el-select>
                    <el-select
                      v-else-if="selectedProcessDetailField.key === 'successors'"
                      :model-value="selectedSuccessorRouteProcessIds"
                      collapse-tags
                      collapse-tags-tooltip
                      filterable
                      multiple
                      placeholder="选择后续工序"
                      size="small"
                      :teleported="false"
                      @change="handleSuccessorsChange"
                    >
                      <el-option
                        v-for="node in routeProcessRelationOptions"
                        :key="node.routeProcessId"
                        :label="nodeLabel(node)"
                        :value="node.routeProcessId"
                      />
                    </el-select>
                    <el-switch
                      v-else-if="selectedProcessDetailField.key === 'keyFlag'"
                      :model-value="Boolean(selectedNode?.keyFlag)"
                      active-text="是"
                      inactive-text="否"
                      inline-prompt
                      @change="handleKeyProcessToggle"
                    />
                    <el-switch
                      v-else-if="selectedProcessDetailField.key === 'checkFlag'"
                      :model-value="Boolean(selectedNode?.checkFlag)"
                      active-text="是"
                      inactive-text="否"
                      inline-prompt
                      @change="handleCheckFlagToggle"
                    />
                  </div>
            </template>
          </template>
        </div>
      </aside>
    </div>

    <el-dialog v-model="routeProcessDialogVisible" append-to-body title="添加工序" width="460px">
      <el-form
        ref="routeProcessFormRef"
        :model="routeProcessForm"
        :rules="routeProcessRules"
        label-width="86px"
      >
        <el-form-item label="工序" prop="processId">
          <el-select
            v-model="routeProcessForm.processId"
            data-flow-action="select-route-process"
            :disabled="routeFlowWriteControlsDisabled"
            filterable
            :loading="processOptionsLoading"
            placeholder="请选择工序"
            style="width: 100%"
          >
            <el-option
              v-for="process in availableProcessOptions"
              :key="process.id"
              :label="formatProcessOption(process)"
              :value="process.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="routeProcessDialogVisible = false">取消</el-button>
        <el-button
          data-flow-action="submit-add-route-process"
          :disabled="routeFlowWriteControlsDisabled"
          :loading="routeProcessSaving"
          type="primary"
          @click="handleRouteProcessAdd"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="capacityOverrideDialogVisible"
      append-to-body
      data-testid="route-flow-capacity-override-dialog"
      title="产能覆盖"
      width="420px"
    >
      <el-form
        ref="capacityOverrideFormRef"
        :model="capacityOverrideForm"
        :rules="capacityOverrideRules"
        label-width="96px"
      >
        <el-form-item label="产能/h" prop="hourlyCapacity">
          <el-input-number
            v-model="capacityOverrideForm.hourlyCapacity"
            :disabled="routeFlowWriteControlsDisabled"
            :min="0.000001"
            :precision="6"
            :step="1"
            controls-position="right"
            data-flow-field="capacity-override-hourly-capacity"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeCapacityOverrideDialog">取消</el-button>
        <el-button
          :disabled="routeFlowWriteControlsDisabled"
          :loading="capacityOverrideSaving"
          data-flow-action="submit-capacity-override"
          type="primary"
          @click="submitCapacityOverride"
        >
          确认
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="capacityWorkstationRepairDialogVisible"
      append-to-body
      data-testid="route-flow-capacity-workstation-repair-dialog"
      title="先绑定工作站"
      width="620px"
    >
      <el-form
        ref="capacityWorkstationRepairFormRef"
        :model="capacityWorkstationRepairForm"
        :rules="capacityWorkstationRepairRules"
        label-width="112px"
      >
        <el-form-item label="绑定方式">
          <el-radio-group
            v-model="capacityWorkstationRepairMode"
            :disabled="routeFlowWriteControlsDisabled"
          >
            <el-radio-button label="reuse">绑定已有工作站</el-radio-button>
            <el-radio-button label="create">新建工作站并绑定</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <template v-if="capacityWorkstationRepairMode === 'reuse'">
          <el-form-item label="已绑定工序" prop="sourceRouteProcessId">
            <el-select
              v-model="capacityWorkstationRepairForm.sourceRouteProcessId"
              data-flow-field="capacity-workstation-repair-source-route-process"
              :disabled="routeFlowWriteControlsDisabled"
              filterable
              :loading="capacityWorkstationRepairWorkstationLoading"
              placeholder="请选择已绑定工作站的工序"
              style="width: 100%"
            >
              <el-option
                v-for="routeProcess in boundRouteProcessOptions"
                :key="routeProcess.value"
                :label="formatBoundRouteProcessOption(routeProcess)"
                :value="routeProcess.value"
              />
            </el-select>
          </el-form-item>
          <div class="route-flow-capacity-workstation-repair__hint">
            选择工序后，当前工序会绑定和该工序一样的工作站；不会直接弹出工作站编号列表。
          </div>
        </template>
        <template v-else>
          <el-form-item label="所在车间" prop="workshopId">
            <el-select
              v-model="capacityWorkstationRepairForm.workshopId"
              data-flow-field="capacity-workstation-repair-workshop"
              :disabled="routeFlowWriteControlsDisabled"
              filterable
              :loading="capacityWorkstationRepairWorkshopLoading"
              placeholder="请选择车间"
              style="width: 100%"
            >
              <el-option
                v-for="workshop in capacityWorkstationRepairWorkshopOptions"
                :key="workshop.id"
                :label="workshop.name"
                :value="workshop.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="班次小时">
            <div
              class="route-flow-capacity-workstation-repair__readonly"
              data-flow-field="capacity-workstation-repair-shift-hours-readonly"
            >
              <span>{{ capacityWorkstationRepairShiftHoursText }}</span>
              <small>来自排产员工作台设置</small>
            </div>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="capacityWorkstationRepairDialogVisible = false">取消</el-button>
        <el-button
          :disabled="routeFlowWriteControlsDisabled"
          :loading="capacityWorkstationRepairSaving"
          data-flow-action="submit-capacity-workstation-repair"
          type="primary"
          @click="submitCapacityWorkstationRepair"
        >
          绑定后继续设定产能
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import {
  ConnectionMode,
  Handle,
  MarkerType,
  Position,
  VueFlow,
  useVueFlow,
  type Connection,
  type Edge,
  type EdgeChange,
  type EdgeMouseEvent,
  type EdgeUpdateEvent,
  type Node,
  type NodeChange,
  type NodeDragEvent,
  type NodeMouseEvent
} from '@vue-flow/core'
import {
  ProRouteApi,
  type ProRouteVO,
  type ProRouteScheduleConfigVO,
  type ProRouteVersionLifecycleStatus,
  type ProRouteVersionVO,
  type RouteFlowBoundaryEdgeVO,
  type RouteFlowBoundaryType,
  type RouteFlowEdgeVO,
  type RouteFlowGraphSaveReqVO,
  type RouteFlowNodeVO,
  type RouteFlowRouteProcessUpdateReqVO,
  type RouteVersionEditContext,
  type RouteFlowValidationVO
} from '@/api/mes/pro/route'
import {
  ProProcessApi,
  type ProProcessMachineryVO,
  type ProProcessVO
} from '@/api/mes/pro/process'
import {
  ProRouteProcessApi,
  type ProRouteProcessMachineryVO,
  type ProRouteProcessRelationVO,
  type ProRouteProcessVO
} from '@/api/mes/pro/route/process'
import { MdWorkstationApi, type MdWorkstationVO } from '@/api/mes/md/workstation'
import { MdWorkshopApi, type MdWorkshopVO } from '@/api/mes/md/workstation/workshop'
import { AutoCodeRecordApi } from '@/api/mes/md/autocode/record'
import {
  SchedulerWorkbenchApi,
  type SchedulerWorkbenchShiftHoursVO
} from '@/api/mes/pro/schedulerWorkbench'
import { CommonStatusEnum } from '@/utils/constants'
import { MesAutoCodeRuleCode } from '@/views/mes/utils/constants'
import { useCache } from '@/hooks/web/useCache'
import { useUserTableColumns } from '@/hooks/web/useUserTableColumns'
import { useUserStoreWithOut } from '@/store/modules/user'
import {
  getUserTableColumnConfig,
  saveUserTableColumnConfig,
  type UserTableColumnConfigColumnVO
} from '@/api/system/userTableColumnConfig'
import {
  ProRouteFlowConfigApi,
  type ProRouteBatchRecordAttachmentOwnerVO,
  type ProRouteFlowBatchRecordVO,
  type ProRouteFlowFormBindingSaveVO,
  type ProRouteFlowFormBindingVO,
  type ProRouteFlowFormSlotType,
  type ProRouteFlowProcessConfigSaveVO,
  type ProRouteFlowProcessConfigVO,
  type ProRouteFlowRequiredPolicy
} from '@/api/mes/pro/route/flowconfig'
import {
  getTemplatePool,
  type FormTemplateListItemVO
} from '@/api/form-center/template'
import {
  getEdhrRouteReleaseApprovalRule,
  saveEdhrRouteReleaseApprovalRule,
  type EdhrWorkTaskAssignmentRuleRespVO,
  type EdhrWorkTaskReleaseApprovalCandidateSourceType
} from '@/api/mes/pro/edhr/workTask'
import type { EdhrProcessFormCandidateSourceType } from '@/api/mes/pro/edhr/processFormPermissionRule'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { getSimpleRoleList, type RoleVO } from '@/api/system/role'
import {
  ROUTE_PROCESS_SETTINGS_COLUMN_CONFIG_CHANGED_EVENT,
  ROUTE_PROCESS_SETTINGS_TABLE_KEY,
  isRouteProcessSettingsDetailColumnKey,
  routeProcessSettingsDefaultColumns,
  type RouteProcessSettingColumnKey
} from './routeProcessSettingsColumns'
import {
  buildRouteCandidateEditQuery,
  ensureSameSourceDraftCandidateForProductionConfig
} from './routeCandidateEntry'

defineOptions({ name: 'RouteFlowGraphDesigner' })

type RouteFlowNodeData = {
  routeNode: RouteFlowNodeVO
}
type BoundaryFlowNodeData = {
  boundaryType: RouteFlowBoundaryType
  label: string
}

type RouteFlowVueNode = Node<RouteFlowNodeData | BoundaryFlowNodeData>
type RouteFlowVueEdge = Edge<{
  routeEdge?: RouteFlowEdgeVO
  boundaryEdge?: RouteFlowBoundaryEdgeVO
}>
type ProcessDetailFieldKey = RouteProcessSettingColumnKey
type RouteFlowLastSelectionState = {
  routeProcessId: number
  detailFieldKey?: ProcessDetailFieldKey
}
type RouteFlowSelectionRestoreSource = 'explicit' | 'cache'
const ROUTE_FLOW_LAST_SELECTION_CACHE_PREFIX = 'mes.pro.route.flow.lastSelection'
type ProcessDetailFieldOption = {
  key: ProcessDetailFieldKey
  label: string
  value: string | number | boolean | null | undefined
  links?: ProcessDetailLinkItem[]
  loading?: boolean
  coverageStatus?: ProcessDetailCoverageStatus
}
type ProcessDetailCoverageStatus = 'covered' | 'missing'
type ProcessDetailFieldSelectOption = ProcessDetailFieldOption & {
  disabled?: boolean
}
type ProcessDetailLinkItem = {
  key: string
  label: string
  onClick: () => void | Promise<void>
}
type FormSlotViewSummaryItem = {
  key: string
  index: number
  formName: string
  fillerSummary: string
  processIndependentSummary: string
}
type ProcessDetailCapacitySourceFocus = 'resource' | 'schedule'
type BoundaryDetailFieldKey = 'releaseOwner' | 'batchRecordAttachment'
type BatchRecordAttachmentOwnerDraft = ProRouteBatchRecordAttachmentOwnerVO & {
  candidateSourceType: EdhrProcessFormCandidateSourceType
  candidateSourceIds: number[]
  candidateSourceNames: string[]
}
type ReleaseApprovalRuleForm = {
  candidateSourceType: EdhrWorkTaskReleaseApprovalCandidateSourceType
  candidateSourceId?: number
  enabled: boolean
  remark: string
}
type ReleaseApprovalRuleCandidateOption = {
  label: string
  value: number
}
type RecordBindingCandidateOption = {
  label: string
  value: number
}
type RecordBindingCopySourceOption = {
  label: string
  value: string
  routeProcessId: number
  binding: RouteFlowRecordBinding
}
type ProcessFormBindingCopySourceOption = {
  label: string
  value: number
  routeProcessId: number
  bindings: RouteFlowRecordBinding[]
}
type ProcessDetailMachineryTarget = Pick<
  ProRouteProcessMachineryVO,
  'machineryId' | 'machineryCode' | 'machineryName'
>
type AutoLayoutOptions = {
  notify?: boolean
  focusRouteProcessId?: number
}
type RouteFlowLayoutPosition = {
  x: number
  y: number
}
type HeightAwareTailChainLayoutOptions = {
  positions: Map<number, RouteFlowLayoutPosition>
  incoming: Map<number, number[]>
  outgoing: Map<number, number[]>
  rowCapacity: number
  compareRouteProcessIds: (leftId: number, rightId: number) => number
}
const DEFAULT_AUTO_LAYOUT_OPTIONS: AutoLayoutOptions = {}

const props = defineProps<{
  routeId: number
  routeName: string
  activeRouteVersionNo?: string
  formType: string
  submitting: boolean
  targetRouteProcessId?: number
  routeVersionEditContext?: RouteVersionEditContext
}>()
const emit = defineEmits<{
  saved: []
  'request-submit': []
  'request-back': []
}>()

const message = useMessage()
const route = useRoute()
const router = useRouter()
const { wsCache } = useCache()
const userStore = useUserStoreWithOut()
const isRouteFlowMaximized = ref(false)
const isEditable = computed(() => ['create', 'update'].includes(props.formType))
const isFrozenRouteVersionView = computed(
  () =>
    Boolean(props.routeVersionEditContext?.routeVersionId) &&
    props.routeVersionEditContext?.lifecycleStatus !== 'DRAFT'
)
const showRouteFlowMutationControls = computed(() => isEditable.value || isFrozenRouteVersionView.value)
const routeFlowWriteControlsDisabled = computed(
  () => !isEditable.value || isFrozenRouteVersionView.value
)
const canMutateRouteFlow = computed(() => !routeFlowWriteControlsDisabled.value)
const CANDIDATE_EDIT_REQUIRED_MESSAGE = '请先创建候选版本，在候选版本中编辑生产配置。'
const CAPACITY_OVERRIDE_AUTO_OPEN_QUERY_VALUE = '1'
const isDraftCandidateEdit = computed(
  () =>
    Boolean(props.routeVersionEditContext?.routeVersionId) &&
    props.routeVersionEditContext?.lifecycleStatus === 'DRAFT'
)
const resolveRouteVersionStatusLabel = (status?: ProRouteVersionLifecycleStatus) => {
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    PENDING_APPROVAL: '审批中',
    READY_TO_PUBLISH: '待发布',
    ACTIVE: '已发布',
    SUPERSEDED: '已替代',
    REJECTED: '已驳回',
    CANCELLED: '已取消'
  }
  return labels[String(status || '')] || String(status || '-')
}
const resolveCurrentRouteVersionViewName = (status?: ProRouteVersionLifecycleStatus) => {
  if (!status) return props.activeRouteVersionNo ? '已发布版本' : '未发布版本'
  if (status === 'DRAFT') return '草稿版本'
  if (status === 'PENDING_APPROVAL') return '审批中候选'
  if (status === 'READY_TO_PUBLISH') return '待发布版本'
  return `${resolveRouteVersionStatusLabel(status)}候选`
}
const currentRouteVersionNoText = computed(
  () => props.routeVersionEditContext?.versionNo || props.activeRouteVersionNo || '未生成版本'
)
const currentRouteVersionViewLabel = computed(
  () =>
    `当前查看：${resolveCurrentRouteVersionViewName(props.routeVersionEditContext?.lifecycleStatus)} ${currentRouteVersionNoText.value}`
)
const requireCandidateRouteVersionId = (actionName: string) => {
  if (!isDraftCandidateEdit.value) {
    throw new Error(`${actionName}失败：${CANDIDATE_EDIT_REQUIRED_MESSAGE}`)
  }
  return props.routeVersionEditContext!.routeVersionId
}
const resolveRouteVersionIdForRead = (activeRouteVersionId?: number) =>
  props.routeVersionEditContext?.routeVersionId || activeRouteVersionId
const resolveRouteVersionIdForSave = () => requireCandidateRouteVersionId('流转关系图保存')
const resolveRouteFlowGraphReadRouteVersionId = () =>
  props.routeVersionEditContext?.lifecycleStatus === 'ACTIVE'
    ? undefined
    : props.routeVersionEditContext?.routeVersionId
const { fitView, setCenter } = useVueFlow()

const NODE_WIDTH = 156
const NODE_HEIGHT = 68
const BOUNDARY_NODE_WIDTH = 132
const BOUNDARY_NODE_HEIGHT = 54
const COLUMN_GAP = 230
const ROW_GAP = 118
const LAYOUT_LEFT_PADDING = 80
const LAYOUT_TOP_PADDING = 72
const MAX_VISIBLE_COLUMNS = 5
const PROCESS_START_NODE_ID = 'process-start'
const PROCESS_END_NODE_ID = 'process-end'
type ConnectionSourceRouteProcessId = number | typeof PROCESS_START_NODE_ID
type ConnectionTargetRouteProcessId = number | typeof PROCESS_END_NODE_ID
type ConnectionProcessOption = {
  routeProcessId: ConnectionSourceRouteProcessId | ConnectionTargetRouteProcessId
  processName?: string
  processCode?: string
  sort?: number
}
type ConnectionAutocompleteOption = ConnectionProcessOption & {
  value: string
}
type RouteFlowRecordInstanceScope = 'PROCESS' | 'BATCH_SHARED'
type RouteFlowRecordBinding = Omit<ProRouteFlowFormBindingVO, 'formTemplateId'> & {
  formBindingKey: string
  formTemplateId?: number | null
  formTemplateName?: string | null
}
type RouteFlowRecordBindingFillerOverride = {
  candidateSourceType: EdhrProcessFormCandidateSourceType | null
  candidateSourceIds: number[]
  candidateSourceNames: string[]
}
type RouteFlowLegacyBatchRecord = ProRouteFlowBatchRecordVO
type SelectedProcessAttributes = {
  routeProcessId?: number
  routeVersionId?: number
  routeScheduleConfigId?: number | null
  scheduleConfigVersion?: string | null
  capacityMode?: ProRouteScheduleConfigVO['capacityMode'] | null
  productionQuantityFactor?: number
  hourlyCapacity?: number
  shiftHours?: number
  infiniteDurationQuantityFactor?: number
  infiniteDurationBaseMinutes?: number
  nightShiftEnabled?: boolean | null
  calendarRuleId?: number | null
  remark?: string | null
}
type SelectedProcessAttributesDraft = SelectedProcessAttributes & {
  routeProcessId: number
  recordBindings: RouteFlowRecordBinding[]
  legacyBatchRecords: RouteFlowLegacyBatchRecord[]
}
type SelectedProcessRouteConfigCache = {
  key: string
  routeInfo: ProRouteVO
  readableRouteVersionId: number
  scheduleConfigs: ProRouteFlowProcessConfigVO[]
  batchConfigs: ProRouteFlowProcessConfigVO[]
  routeScheduleConfigs: ProRouteScheduleConfigVO[]
}
type RouteNodeBindingStatus = 'none' | 'bound' | 'missing'
type CapacityWorkstationRepairMode = 'reuse' | 'create'
type CapacityWorkstationRepairForm = {
  sourceRouteProcessId?: string
  workshopId?: number
}
type CapacityWorkstationRepairSourceOption = {
  value: string
  sourceType: 'route-process' | 'workstation'
  sourceRouteProcessId?: number
  processId?: number
  processCode?: string
  processName?: string
  sort?: number
  workstationId: number
  workstationCode?: string
  workstationName?: string
  shiftHours?: number
  shiftCapacity?: number
}
type CapacityWorkstationRepairBinding = {
  workstationId: number
  workstationCode?: string
  workstationName?: string
  shiftHours?: number
  shiftCapacity?: number
}
const DEFAULT_PRODUCTION_QUANTITY_FACTOR = 1
const PRODUCTION_QUANTITY_FACTOR_OVERRIDE_TOLERANCE = 0.000001
const CAPACITY_OVERRIDE_DIFF_TOLERANCE = 0.00001
const CAPACITY_WORKSTATION_REPAIR_WORKSTATION_PAGE_SIZE = 200
const PROCESS_DETAIL_FIELD_CONFIG_TABLE_KEY = 'mes.pro.route.flow.detailFields'
const FORM_SLOT_AGGREGATE_FIELD_KEY: RouteProcessSettingColumnKey = 'formSlots'
const PROCESS_DETAIL_STANDALONE_RESOURCE_FIELD_KEYS = new Set<RouteProcessSettingColumnKey>([
  'capacitySource',
  'standardResource',
  'standardShiftCapacity'
])
const PROCESS_DETAIL_HIDDEN_FIELD_KEYS = new Set<RouteProcessSettingColumnKey>([
  'shiftCapacity',
  'resourceStatus'
])
const PROCESS_DETAIL_EDITABLE_FIELD_KEYS = new Set<RouteProcessSettingColumnKey>([
  'productionQuantityFactor',
  'predecessor',
  'successors',
  'keyFlag',
  'checkFlag',
  FORM_SLOT_AGGREGATE_FIELD_KEY
])
const ROUTE_NODE_BINDING_STATUS_FIELD_KEYS = new Set<RouteProcessSettingColumnKey>([
  FORM_SLOT_AGGREGATE_FIELD_KEY,
  'batchRecordFormNames',
  'productionQuantityFactor',
  'keyFlag',
  'checkFlag',
  'workstation'
])
const RECORD_BINDING_SLOT_TYPES: ProRouteFlowFormSlotType[] = [
  'MAIN',
  'LOSS_REPORT',
  'PROCESS_INSPECTION',
  'PARAMETER_RECORD'
]
const ADDITIONAL_RECORD_BINDING_SLOT_TYPES = RECORD_BINDING_SLOT_TYPES.filter(
  (slot) => slot !== 'MAIN'
)
const RECORD_BINDING_CANDIDATE_SOURCE_OPTIONS: Array<{
  label: string
  value: EdhrProcessFormCandidateSourceType
}> = [
  { label: '个人', value: 'USERS' },
  { label: '权限角色', value: 'ROLE' }
]
const BATCH_RECORD_ATTACHMENT_CANDIDATE_SOURCE_OPTIONS = RECORD_BINDING_CANDIDATE_SOURCE_OPTIONS
const BATCH_RECORD_ATTACHMENT_DEFAULT_ITEMS = [
  {
    attachmentCode: 'INCOMING_INSPECTION_REPORT',
    attachmentName: '来料检报告',
    defaultRoleName: '来料检报告上传1',
    sort: 1
  },
  {
    attachmentCode: 'STERILIZATION_REPORT',
    attachmentName: '灭菌报告',
    defaultRoleName: '灭菌报告上传1',
    sort: 2
  },
  {
    attachmentCode: 'FINISHED_PRODUCT_INSPECTION_REPORT',
    attachmentName: '成品检报告',
    defaultRoleName: '成品检报告上传1',
    sort: 3
  },
  {
    attachmentCode: 'FINISHED_PRODUCT_INSPECTION_RECORD',
    attachmentName: '成品检记录',
    defaultRoleName: '成品检记录上传1',
    sort: 4
  }
] as const
const BATCH_RECORD_ATTACHMENT_SORT_BY_CODE = new Map<string, number>(
  BATCH_RECORD_ATTACHMENT_DEFAULT_ITEMS.map((item) => [item.attachmentCode, item.sort])
)
const PROCESS_DETAIL_FIELD_KEYS: ProcessDetailFieldKey[] = routeProcessSettingsDefaultColumns.map(
  (column) => column.key as RouteProcessSettingColumnKey
)
  .filter((key): key is ProcessDetailFieldKey => isRouteProcessSettingsDetailColumnKey(key))
  .filter((key) => !PROCESS_DETAIL_STANDALONE_RESOURCE_FIELD_KEYS.has(key))
  .filter((key) => !PROCESS_DETAIL_HIDDEN_FIELD_KEYS.has(key))
const PROCESS_DETAIL_FIELD_KEY_SET = new Set<string>(PROCESS_DETAIL_FIELD_KEYS)
const DEFAULT_PROCESS_DETAIL_FIELD_KEYS: ProcessDetailFieldKey[] = [
  'sort',
  'processCode',
  'processName',
  'workstation',
  'batchRecordFormNames'
].filter((key): key is ProcessDetailFieldKey => PROCESS_DETAIL_FIELD_KEY_SET.has(key))
const REQUIRED_PROCESS_DETAIL_FIELD_KEYS: ProcessDetailFieldKey[] = [
  'batchRecordFormNames'
].filter((key): key is ProcessDetailFieldKey => PROCESS_DETAIL_FIELD_KEY_SET.has(key))
const CAPACITY_SOURCE_FOCUS_FIELD_KEYS: Record<string, ProcessDetailFieldKey[]> = {
  resource: ['workstation'],
  schedule: ['workstation']
}
const loading = ref(false)
const saving = ref(false)
const autoLayoutEntryPending = ref(false)
const routeProcessSaving = ref(false)
const processOptionsLoading = ref(false)
const graphVersion = ref(0)
const validationStatus = ref('UNINITIALIZED')
const routeNodes = ref<RouteFlowNodeVO[]>([])
const routeProcessRows = ref<ProRouteProcessVO[]>([])
const routeEdges = ref<RouteFlowEdgeVO[]>([])
const boundaryEdges = ref<RouteFlowBoundaryEdgeVO[]>([])
const flowNodes = ref<RouteFlowVueNode[]>([])
const flowEdges = ref<RouteFlowVueEdge[]>([])
const graphCanvasRef = ref<HTMLElement>()
const processOptions = ref<ProProcessVO[]>([])
const pendingDeletedRouteProcessIds = ref<Set<number>>(new Set())
const nextDraftRouteProcessId = ref(-1)
const invalidRouteProcessIds = ref<Set<number>>(new Set())
const selectedRouteProcessId = ref<number | null>(null)
const selectedBoundaryType = ref<RouteFlowBoundaryType | null>(null)
const selectedBoundaryDetailFieldKey = ref<BoundaryDetailFieldKey>()
const selectedEdgeKey = ref('')
const connectionPopoverVisible = ref(false)
const connectionSourceInputText = ref('')
const connectionTargetInputText = ref('')
const connectionSourceRouteProcessId = ref<ConnectionSourceRouteProcessId | null>(null)
const connectionTargetRouteProcessId = ref<ConnectionTargetRouteProcessId | null>(null)
const graphDirty = ref(false)
const autoLayoutRevision = ref(0)
const highlightedRouteProcessId = ref<number | null>(null)
const highlightedProcessDetailFieldKey = ref<ProcessDetailFieldKey>()
const searchKeyword = ref('')
const selectedProcessDetail = ref<ProProcessVO>()
const selectedProcessMachineryList = ref<ProProcessMachineryVO[]>([])
const selectedProcessDetailLoading = ref(false)
const selectedProcessMachineryLoading = ref(false)
const selectedProcessAttributesLoading = ref(false)
const selectedProcessAttributesSaving = ref(false)
const selectedProcessAttributes = reactive<SelectedProcessAttributes>({})
const selectedRecordBindings = ref<RouteFlowRecordBinding[]>([])
const selectedLegacyBatchRecords = ref<RouteFlowLegacyBatchRecord[]>([])
const formTemplateOptions = ref<FormTemplateListItemVO[]>([])
const formTemplateOptionLoading = ref(false)
const recordBindingUserOptions = ref<UserVO[]>([])
const recordBindingUserOptionsLoading = ref(false)
const recordBindingRoleOptions = ref<RoleVO[]>([])
const recordBindingRoleOptionsLoading = ref(false)
const recordBindingCopySourceByKey = reactive<Record<string, string>>({})
const processFormBindingCopyPopoverVisible = ref(false)
const processFormBindingCopySourceRouteProcessId = ref<number | null>(null)
const selectedProcessAttributeDrafts = reactive<Record<number, SelectedProcessAttributesDraft>>({})
const selectedProcessAttributeBaselines = reactive<Record<number, string>>({})
const selectedProcessRouteConfigCache = ref<SelectedProcessRouteConfigCache>()
const routeProcessKeyFlagBaselines = reactive<Record<number, boolean>>({})
const routeProcessCheckFlagBaselines = reactive<Record<number, boolean>>({})
const routeProcessWorkstationIdBaselines = reactive<Record<number, number | null>>({})
const capacityOverrideDialogVisible = ref(false)
const capacityOverrideSaving = ref(false)
const capacityOverrideFormRef = ref()
const capacityOverrideForm = reactive<{ hourlyCapacity?: number }>({})
const capacityOverrideRouteVersionId = ref<number | null>(null)
const capacityOverrideRouteProcessId = ref<number | null>(null)
const capacityOverrideCandidateCreating = ref(false)
const capacityOverrideRepairHourlyCapacity = ref<number | undefined>()
const capacityWorkstationRepairDialogVisible = ref(false)
const capacityWorkstationRepairSaving = ref(false)
const capacityWorkstationRepairWorkstationLoading = ref(false)
const capacityWorkstationRepairWorkshopLoading = ref(false)
const capacityWorkstationRepairShiftHoursLoading = ref(false)
const capacityWorkstationRepairMode = ref<CapacityWorkstationRepairMode>('reuse')
const capacityWorkstationRepairFormRef = ref()
const capacityWorkstationRepairForm = reactive<CapacityWorkstationRepairForm>({})
const capacityWorkstationRepairWorkstationOptions = ref<MdWorkstationVO[]>([])
const capacityWorkstationRepairWorkshopOptions = ref<MdWorkshopVO[]>([])
const capacityWorkstationRepairShiftHoursSetting = ref<SchedulerWorkbenchShiftHoursVO>()
let capacityOverrideAutoOpening = false
const processDetailInterestLoading = ref(false)
const processDetailInterestSaving = ref(false)
const processDetailInterestReady = ref(false)
const processDetailInterestAvailable = ref(true)
const selectedProcessDetailFieldToAdd = ref<ProcessDetailFieldKey>()
const selectedProcessDetailFieldKeys = ref<ProcessDetailFieldKey[]>([])
const selectedProcessDetailFieldKey = ref<ProcessDetailFieldKey>()
const releaseApprovalRuleLoading = ref(false)
const releaseApprovalRuleSaving = ref(false)
const releaseApprovalRuleLoaded = ref(false)
const releaseApprovalRuleLoadError = ref('')
const releaseApprovalRuleUserOptionsLoading = ref(false)
const releaseApprovalRuleUserOptions = ref<UserVO[]>([])
const releaseApprovalRuleRoleOptionsLoading = ref(false)
const releaseApprovalRuleRoleOptions = ref<RoleVO[]>([])
const currentReleaseApprovalRule = ref<EdhrWorkTaskAssignmentRuleRespVO | null>(null)
const batchRecordAttachmentOwnersLoading = ref(false)
const batchRecordAttachmentOwnersSaving = ref(false)
const batchRecordAttachmentOwnersInitializing = ref(false)
const batchRecordAttachmentOwnersLoaded = ref(false)
const batchRecordAttachmentOwnersLoadError = ref('')
const batchRecordAttachmentOwners = ref<BatchRecordAttachmentOwnerDraft[]>([])
const releaseApprovalRuleForm = reactive<ReleaseApprovalRuleForm>({
  candidateSourceType: 'USER',
  candidateSourceId: undefined,
  enabled: true,
  remark: ''
})
let selectedProcessDetailRequestId = 0
let selectedProcessRouteConfigCachePromise:
  | { key: string; promise: Promise<SelectedProcessRouteConfigCache> }
  | undefined
const {
  columns: routeProcessSettingColumns,
  loadConfig: loadRouteProcessSettingColumnConfig
} = useUserTableColumns(ROUTE_PROCESS_SETTINGS_TABLE_KEY, routeProcessSettingsDefaultColumns)
const routeProcessDialogVisible = ref(false)
const routeProcessFormRef = ref()
const routeProcessForm = reactive<{ processId?: number }>({
  processId: undefined
})

const routeProcessRules = {
  processId: [{ required: true, message: '请选择工序', trigger: 'change' }]
}

const validateCapacityOverrideHourlyCapacity = (
  _rule: unknown,
  value: number | undefined,
  callback: (error?: Error) => void
) => {
  const hourlyCapacity = normalizeHourlyCapacity(value)
  if (hourlyCapacity === undefined || hourlyCapacity <= 0) {
    callback(new Error('产能覆盖必须大于 0'))
    return
  }
  callback()
}

const capacityOverrideRules = {
  hourlyCapacity: [{ validator: validateCapacityOverrideHourlyCapacity, trigger: 'blur' }]
}

const validateCapacityWorkstationRepairSource = (
  _rule: unknown,
  value: number | undefined,
  callback: (error?: Error) => void
) => {
  if (capacityWorkstationRepairMode.value !== 'reuse') {
    callback()
    return
  }
  if (!value) {
    callback(new Error('请选择已绑定工作站的工序'))
    return
  }
  callback()
}

const validateCapacityWorkstationRepairWorkshop = (
  _rule: unknown,
  value: number | undefined,
  callback: (error?: Error) => void
) => {
  if (capacityWorkstationRepairMode.value !== 'create') {
    callback()
    return
  }
  if (!value) {
    callback(new Error('请选择车间'))
    return
  }
  callback()
}

const capacityWorkstationRepairRules = {
  sourceRouteProcessId: [{ validator: validateCapacityWorkstationRepairSource, trigger: 'change' }],
  workshopId: [{ validator: validateCapacityWorkstationRepairWorkshop, trigger: 'change' }]
}

const selectedNode = computed(() => {
  return routeNodes.value.find((node) => node.routeProcessId === selectedRouteProcessId.value)
})
const mergeRouteProcessRowWithNode = (
  row: ProRouteProcessVO | undefined,
  node: RouteFlowNodeVO
): ProRouteProcessVO => {
  const routeProcessWorkstationId = props.routeVersionEditContext
    ? node.routeProcessWorkstationId
    : node.routeProcessWorkstationId ?? row?.workstationId
  return {
    ...(row || {
      id: node.routeProcessId,
      routeId: props.routeId,
      processId: node.processId,
      sort: node.sort || 0,
      predecessors: [],
      successors: []
    }),
    id: row?.id ?? node.routeProcessId,
    routeId: row?.routeId ?? props.routeId,
    processId: row?.processId ?? node.processId,
    processCode: node.processCode ?? row?.processCode,
    processName: node.processName ?? row?.processName,
    sort: node.sort ?? row?.sort ?? 0,
    keyFlag: node.keyFlag ?? row?.keyFlag,
    checkFlag: node.checkFlag ?? row?.checkFlag,
    routeProcessWorkstationId,
    workstationId: node.workstationId,
    workstationCode: node.workstationCode,
    workstationName: node.workstationName
  }
}
const candidateAwareRouteProcessRows = computed(() => {
  const rowsById = new Map<number, ProRouteProcessVO>()
  routeProcessRows.value.forEach((row) => {
    const routeProcessId = Number(row.id)
    if (Number.isFinite(routeProcessId)) {
      rowsById.set(routeProcessId, row)
    }
  })
  routeNodes.value.forEach((node) => {
    const routeProcessId = Number(node.routeProcessId)
    if (!Number.isFinite(routeProcessId)) return
    rowsById.set(routeProcessId, mergeRouteProcessRowWithNode(rowsById.get(routeProcessId), node))
  })
  return Array.from(rowsById.values())
})
const selectedRouteProcess = computed(() => {
  return candidateAwareRouteProcessRows.value.find(
    (row) => Number(row.id) === Number(selectedRouteProcessId.value)
  )
})
const selectedPredecessorRouteProcessIds = computed(() => {
  const routeProcessId = selectedRouteProcessId.value
  if (!routeProcessId) return []
  return routeEdges.value
    .filter((edge) => Number(edge.targetRouteProcessId) === Number(routeProcessId))
    .map((edge) => edge.sourceRouteProcessId)
})
const selectedSuccessorRouteProcessIds = computed(() => {
  const routeProcessId = selectedRouteProcessId.value
  if (!routeProcessId) return []
  return routeEdges.value
    .filter((edge) => Number(edge.sourceRouteProcessId) === Number(routeProcessId))
    .map((edge) => edge.targetRouteProcessId)
})
const routeProcessRelationOptions = computed(() =>
  sortedActiveRouteNodes.value.filter(
    (node) => Number(node.routeProcessId) !== Number(selectedRouteProcessId.value)
  )
)
const capacityWorkstationRepairTargetRouteProcess = computed(() => {
  const routeProcessId = capacityOverrideRouteProcessId.value || selectedProcessAttributes.routeProcessId
  return candidateAwareRouteProcessRows.value.find((row) => Number(row.id) === Number(routeProcessId))
})
const getBoundRouteProcessWorkstationId = (routeProcess?: ProRouteProcessVO) =>
  props.routeVersionEditContext
    ? numericValue(routeProcess?.routeProcessWorkstationId)
    : numericValue(routeProcess?.routeProcessWorkstationId ?? routeProcess?.workstationId)

const buildRouteProcessRepairSourceOptions = (): CapacityWorkstationRepairSourceOption[] =>
  candidateAwareRouteProcessRows.value
    .filter((row) => {
      const routeProcessId = Number(row.id)
      const targetRouteProcessId = Number(
        capacityOverrideRouteProcessId.value || selectedProcessAttributes.routeProcessId
      )
      return (
        Number.isFinite(routeProcessId) &&
        Number.isFinite(targetRouteProcessId) &&
        routeProcessId !== targetRouteProcessId &&
        Boolean(getBoundRouteProcessWorkstationId(row))
      )
    })
    .map((row) => ({
      value: `route-process:${row.id}`,
      sourceType: 'route-process',
      sourceRouteProcessId: row.id,
      processId: row.processId,
      processCode: row.processCode,
      processName: row.processName,
      sort: row.sort,
      workstationId: getBoundRouteProcessWorkstationId(row)!,
      workstationCode: row.workstationCode,
      workstationName: row.workstationName,
      shiftHours: row.shiftHours,
      shiftCapacity: row.processShiftCapacityTotal
    }))

const buildGlobalWorkstationRepairSourceOptions = (): CapacityWorkstationRepairSourceOption[] =>
  capacityWorkstationRepairWorkstationOptions.value
    .map((workstation): CapacityWorkstationRepairSourceOption | undefined => {
      const workstationId = numericValue(workstation.id)
      const processId = numericValue(workstation.processId)
      if (!workstationId || !processId) return undefined
      if (workstation.status !== undefined && Number(workstation.status) !== CommonStatusEnum.ENABLE) {
        return undefined
      }
      return {
        value: `workstation:${workstationId}`,
        sourceType: 'workstation' as const,
        processId,
        processName: workstation.processName,
        workstationId,
        workstationCode: workstation.code,
        workstationName: workstation.name,
        shiftHours: numericValue(workstation.shiftHours),
        shiftCapacity: numericValue(workstation.todayCapacity)
      }
    })
    .filter((option): option is CapacityWorkstationRepairSourceOption => Boolean(option))

const capacityWorkstationRepairShiftHours = computed(() =>
  numericValue(capacityWorkstationRepairShiftHoursSetting.value?.shiftHours)
)

const capacityWorkstationRepairShiftHoursText = computed(() => {
  if (capacityWorkstationRepairShiftHoursLoading.value) return '读取中...'
  const shiftHours = capacityWorkstationRepairShiftHours.value
  if (shiftHours === undefined || shiftHours <= 0) return '未统一配置'
  return `${formatRouteProcessCapacity(shiftHours)} 小时`
})

const compareCapacityWorkstationRepairSourceOptions = (
  left: CapacityWorkstationRepairSourceOption,
  right: CapacityWorkstationRepairSourceOption
) => {
  const sourceRank = (option: CapacityWorkstationRepairSourceOption) =>
    option.sourceType === 'route-process' ? 0 : 1
  const rankResult = sourceRank(left) - sourceRank(right)
  if (rankResult !== 0) return rankResult
  const sortResult = (left.sort || 0) - (right.sort || 0)
  if (sortResult !== 0) return sortResult
  return `${left.processCode || ''}${left.processName || ''}${left.workstationCode || ''}`.localeCompare(
    `${right.processCode || ''}${right.processName || ''}${right.workstationCode || ''}`,
    'zh-Hans-CN'
  )
}

const boundRouteProcessOptions = computed(() => {
  const seenWorkstationIds = new Set<number>()
  return [
    ...buildRouteProcessRepairSourceOptions(),
    ...buildGlobalWorkstationRepairSourceOptions()
  ]
    .filter((option) => {
      if (seenWorkstationIds.has(option.workstationId)) return false
      seenWorkstationIds.add(option.workstationId)
      return true
    })
    .sort(compareCapacityWorkstationRepairSourceOptions)
})
const selectedNodeFullName = computed(() => {
  return selectedNode.value ? nodeLabel(selectedNode.value) : ''
})

const formatBoundRouteProcessOption = (routeProcess: CapacityWorkstationRepairSourceOption) => {
  const processName = routeProcess.processName || '未命名工序'
  const shiftCapacity = numericValue(routeProcess.shiftCapacity)
  const shiftCapacityLabel =
    shiftCapacity === undefined
      ? '产能未配置'
      : `${formatRouteProcessIntegerShiftCapacity(shiftCapacity)}/班次`
  return `${processName}（${shiftCapacityLabel}）`
}

const isProcessDetailLinkLabelVisible = (label: string) => {
  return Boolean(label && label !== '-')
}

const buildProcessDetailTextLink = (
  key: string,
  label: string,
  onClick: ProcessDetailLinkItem['onClick']
): ProcessDetailLinkItem[] => {
  if (!isProcessDetailLinkLabelVisible(label)) return []
  return [{ key, label, onClick }]
}

const normalizePositiveProcessId = (value?: number | string | null) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const buildWorkstationProcessQuery = (processId?: number | string | null) => {
  const normalizedProcessId = normalizePositiveProcessId(processId)
  return normalizedProcessId ? { processId: String(normalizedProcessId) } : {}
}

const openProcessTargetLink = async () => {
  const processId = selectedNode.value?.processId || selectedRouteProcess.value?.processId
  if (!processId) {
    throw new Error(`工序跳转缺少工序编号: routeProcessId=${selectedRouteProcessId.value}`)
  }
  await persistRouteFlowReturnState()
  await router.push({
    path: '/mes/pro/process',
    query: {
      openId: String(processId),
      code: selectedRouteProcess.value?.processCode || selectedNode.value?.processCode || undefined
    }
  })
}

const openRecordBindingTargetLink = async (binding: RouteFlowRecordBinding) => {
  if (!binding.formTemplateId) {
    throw new Error(`批记录表单跳转缺少表单模板: routeProcessId=${selectedRouteProcessId.value}`)
  }
  await persistRouteFlowReturnState()
  await router.push({
    path: '/mes/pro/batch-record-form-list',
    query: {
      formTemplateId: String(binding.formTemplateId),
      formSlotType: normalizeRecordBindingSlotType(binding.formSlotType, binding.formBindingKey)
    }
  })
}

const openLegacyBatchRecordTargetLink = async (report: RouteFlowLegacyBatchRecord) => {
  const reportId = normalizeNullableText(report.batchRecordReportId)
  if (!reportId) {
    throw new Error(`批记录表单跳转缺少报表编号: routeProcessId=${selectedRouteProcessId.value}`)
  }
  await persistRouteFlowReturnState()
  await router.push({
    path: '/mes/pro/batch-record-form-list',
    query: {
      reportId,
      formSlotType: requireBatchRecordFormSlotType(report)
    }
  })
}
const openWorkstationTargetLink = async () => {
  const row = selectedRouteProcess.value
  const workstationCode = row?.workstationCode?.trim()
  if (!row?.workstationId || !workstationCode) {
    throw new Error(
      `工作站跳转缺少工作站编码: routeProcessId=${row?.id}, workstationId=${row?.workstationId}`
    )
  }
  await persistRouteFlowReturnState()
  await router.push({
    path: '/mes/md/workstation',
    query: {
      openId: String(row.workstationId),
      code: workstationCode,
      ...buildWorkstationProcessQuery(row.processId)
    }
  })
}

const openMachineryTargetLink = async (machinery: ProcessDetailMachineryTarget) => {
  if (!machinery.machineryId) {
    throw new Error(`设备跳转缺少设备编号: routeProcessId=${selectedRouteProcessId.value}`)
  }
  await persistRouteFlowReturnState()
  await router.push({
    path: '/mes/dv/machinery',
    query: { openId: String(machinery.machineryId) }
  })
}

const buildProcessDetailMachineryLinks = (): ProcessDetailLinkItem[] =>
  selectedProcessMachineryList.value
    .filter((machinery) => Boolean(machinery.machineryId))
    .map((machinery) => ({
      key: String(machinery.machineryId),
      label:
        [machinery.machineryCode, machinery.machineryName].filter(Boolean).join(' / ') ||
        String(machinery.machineryId),
      onClick: () => openMachineryTargetLink(machinery)
    }))

const openWorkstationConfigOrDetailLink = async () => {
  if (selectedRouteProcess.value?.workstationId) {
    await openWorkstationTargetLink()
    return
  }
  await openSelectedProcessDetailFocusLink('workstation', 'resource')
}

const buildWorkstationTargetLink = (row?: ProRouteProcessVO) =>
  buildProcessDetailTextLink(
    'workstation-target',
    `工作站：${formatRouteProcessWorkstation(row)}`,
    () => openWorkstationConfigOrDetailLink()
  )

const formatMachineryTargetLabel = (machinery: ProcessDetailMachineryTarget) =>
  [machinery.machineryCode, machinery.machineryName].filter(Boolean).join(' / ') ||
  (machinery.machineryId ? String(machinery.machineryId) : '未绑定')

const buildWorkstationMachineryLinks = (row?: ProRouteProcessVO): ProcessDetailLinkItem[] => {
  const machineryList = row?.machineryList || []
  if (machineryList.length === 0) {
    return buildProcessDetailTextLink('workstation-machinery-empty', '绑定设备：未绑定', () =>
      openWorkstationConfigOrDetailLink()
    )
  }
  return machineryList.map((machinery, index) => ({
    key: `workstation-machinery-${machinery.machineryId || index}`,
    label: `设备：${formatMachineryTargetLabel(machinery)}`,
    onClick: () => openMachineryTargetLink(machinery)
  }))
}

const buildWorkstationShiftCapacityLink = (row?: ProRouteProcessVO) =>
  buildProcessDetailTextLink(
    'workstation-shift-capacity',
    `班次产能：${formatRouteProcessIntegerShiftCapacity(row?.processShiftCapacityTotal)}`,
    () => openSelectedProcessDetailFocusLink('workstation', 'schedule')
  )

const buildWorkstationDetailLinks = (row?: ProRouteProcessVO): ProcessDetailLinkItem[] => [
  ...buildWorkstationTargetLink(row),
  ...buildWorkstationMachineryLinks(row),
  ...buildWorkstationShiftCapacityLink(row)
]

const handleProcessDetailLinkClick = async (link: ProcessDetailLinkItem) => {
  try {
    await link.onClick()
  } catch (error) {
    message.error(resolveErrorMessage(error, '详情跳转失败，请检查数据后重试。'))
    throw error
  }
}

const focusRouteProcessNode = async (routeProcessId: number) => {
  if (!Number.isFinite(routeProcessId) || !findNode(routeProcessId)) {
    throw new Error(`流转关系图定位失败：路线工序不存在 routeProcessId=${routeProcessId}`)
  }
  selectedRouteProcessId.value = routeProcessId
  selectedBoundaryType.value = null
  selectedEdgeKey.value = ''
  highlightedRouteProcessId.value = routeProcessId
  await nextTick()
  focusNode(routeProcessId)
  await router.push({
    query: {
      ...route.query,
      tab: 'flow',
      routeProcessId: String(routeProcessId)
    }
  })
}

const openRouteProcessRelationLink = async (relation: ProRouteProcessRelationVO) => {
  await focusRouteProcessNode(relation.routeProcessId)
}

const openSelectedProcessDetailFocusLink = async (
  fieldKey: ProcessDetailFieldKey,
  capacitySourceFocus?: ProcessDetailCapacitySourceFocus
) => {
  const routeProcessId = selectedRouteProcessId.value
  if (!routeProcessId) {
    throw new Error(`流转关系图定位失败：缺少选中路线工序 field=${fieldKey}`)
  }
  selectedBoundaryType.value = null
  selectedEdgeKey.value = ''
  highlightedProcessDetailFieldKey.value = fieldKey
  selectedProcessDetailFieldKeys.value = normalizeProcessDetailFieldKeys([
    fieldKey,
    ...selectedProcessDetailFieldKeys.value
  ])
  await nextTick()
  focusNode(routeProcessId)
  await scrollProcessDetailField(fieldKey)
  await router.push({
    query: {
      ...route.query,
      tab: 'flow',
      routeProcessId: String(routeProcessId),
      capacitySourceFocus
    }
  })
}

const normalizeRecordBindingInstanceScope = (
  instanceScope?: string | null
): RouteFlowRecordInstanceScope => (instanceScope === 'BATCH_SHARED' ? 'BATCH_SHARED' : 'PROCESS')

const RECORD_BINDING_REQUIRED_POLICIES: ProRouteFlowRequiredPolicy[] = [
  'REQUIRED',
  'CONDITIONAL_REQUIRED',
  'OPTIONAL',
  'SKIPPABLE_CONTROLLED'
]

const normalizeRecordBindingRequiredPolicy = (
  requiredPolicy?: string | null
): ProRouteFlowRequiredPolicy => {
  const normalized = String(requiredPolicy || '').trim() as ProRouteFlowRequiredPolicy
  return RECORD_BINDING_REQUIRED_POLICIES.includes(normalized) ? normalized : 'REQUIRED'
}

const normalizeNullableText = (value?: string | null) => {
  const normalized = String(value || '').trim()
  return normalized || null
}

const normalizeRecordBindingCandidateSourceType = (
  candidateSourceType?: string | null
): EdhrProcessFormCandidateSourceType | null => {
  const normalized = String(candidateSourceType || '').trim()
  if (normalized === 'USER' || normalized === 'USERS') return 'USERS'
  if (normalized === 'ROLE') return 'ROLE'
  return null
}

const normalizeRecordBindingCandidateIds = (candidateSourceIds?: Array<number | string> | null) =>
  Array.from(
    new Set(
      (candidateSourceIds || [])
        .map((id) => Number(id))
        .filter((id) => Number.isFinite(id) && id > 0)
    )
  )

const normalizeRecordBindingCandidateNames = (candidateSourceNames?: string[] | null) =>
  Array.from(
    new Set(
      (candidateSourceNames || [])
        .map((name) => String(name || '').trim())
        .filter(Boolean)
    )
  )

const isRecordBindingSlotType = (value?: string | null): value is ProRouteFlowFormSlotType =>
  RECORD_BINDING_SLOT_TYPES.includes(value as ProRouteFlowFormSlotType)

const normalizeRecordBindingSlotType = (
  formSlotType?: string | null,
  formBindingKey?: string | null
): ProRouteFlowFormSlotType => {
  const normalizedFormSlotType = normalizeNullableText(formSlotType)
  if (isRecordBindingSlotType(normalizedFormSlotType)) return normalizedFormSlotType
  const normalizedBindingKey = normalizeNullableText(formBindingKey)
  if (isRecordBindingSlotType(normalizedBindingKey)) return normalizedBindingKey
  return 'MAIN'
}

const resolveRecordBindingSlotType = (
  formSlotType?: string | null,
  formBindingKey?: string | null
): ProRouteFlowFormSlotType | undefined => {
  const normalizedFormSlotType = normalizeNullableText(formSlotType)
  if (isRecordBindingSlotType(normalizedFormSlotType)) return normalizedFormSlotType
  const normalizedBindingKey = normalizeNullableText(formBindingKey)
  if (isRecordBindingSlotType(normalizedBindingKey)) return normalizedBindingKey
  return undefined
}

const SHARED_FORM_FILLABLE_SCOPE_JSON = JSON.stringify({
  ranges: Array.from({ length: 100 }, (_, sourceTableIndex) => ({
    sourceTableIndex,
    startRow: 0,
    endRow: 99999
  }))
})

const buildSharedRecordBindingKey = (
  binding: Pick<RouteFlowRecordBinding, 'formSlotType' | 'formBindingKey' | 'formTemplateId'>
) => {
  const formTemplateId = Number(binding.formTemplateId || 0)
  if (!Number.isFinite(formTemplateId) || formTemplateId <= 0) return null
  const formSlotType = normalizeRecordBindingSlotType(binding.formSlotType, binding.formBindingKey)
  return `${formSlotType}_${formTemplateId}`
}

let localFormBindingSequence = 1

const createLocalFormBindingKey = () => `FORM_BINDING_${Date.now()}_${localFormBindingSequence++}`

const resolveNextAdditionalRecordBindingSlotType = (): ProRouteFlowFormSlotType => {
  const usedSlotTypes = new Set(
    selectedRecordBindings.value
      .map((binding) => normalizeRecordBindingSlotType(binding.formSlotType, binding.formBindingKey))
      .filter((slot) => slot !== 'MAIN')
  )
  return (
    ADDITIONAL_RECORD_BINDING_SLOT_TYPES.find((slot) => !usedSlotTypes.has(slot)) ||
    ADDITIONAL_RECORD_BINDING_SLOT_TYPES[ADDITIONAL_RECORD_BINDING_SLOT_TYPES.length - 1]
  )
}

const createEmptyRecordBinding = (): RouteFlowRecordBinding => ({
  formBindingKey: createLocalFormBindingKey(),
  formSlotType: resolveNextAdditionalRecordBindingSlotType(),
  formTemplateId: null,
  formTemplateName: null,
  instanceScope: 'BATCH_SHARED',
  sharedFormKey: null,
  fillableScopeJson: null,
  recordbookEnabled: true,
  requiredPolicy: 'REQUIRED',
  candidateSourceType: null,
  candidateSourceIds: [],
  candidateSourceNames: [],
  reportSort: selectedRecordBindings.value.length + 1,
  remark: null
})

const normalizeFormBinding = (
  report: ProRouteFlowFormBindingVO,
  index: number
): RouteFlowRecordBinding | undefined => {
  const formTemplateId = Number(report.formTemplateId)
  if (!Number.isFinite(formTemplateId) || formTemplateId <= 0) return undefined
  const formSlotType = normalizeRecordBindingSlotType(report.formSlotType, report.formBindingKey)
  const formBindingKey = normalizeNullableText(report.formBindingKey) || `FORM_BINDING_${index + 1}`
  const instanceScope = normalizeRecordBindingInstanceScope(report.instanceScope)
  return {
    ...report,
    formBindingKey,
    formSlotType,
    formTemplateId,
    formTemplateName: report.formTemplateName || report.formTemplateNameSnapshot || null,
    instanceScope,
    sharedFormKey:
      instanceScope === 'BATCH_SHARED'
        ? report.sharedFormKey || buildSharedRecordBindingKey({ formSlotType, formBindingKey, formTemplateId })
        : null,
    fillableScopeJson:
      instanceScope === 'BATCH_SHARED'
        ? report.fillableScopeJson || SHARED_FORM_FILLABLE_SCOPE_JSON
        : null,
    recordbookEnabled: true,
    requiredPolicy: 'REQUIRED',
    candidateSourceType: normalizeRecordBindingCandidateSourceType(report.candidateSourceType),
    candidateSourceIds: normalizeRecordBindingCandidateIds(report.candidateSourceIds),
    candidateSourceNames: normalizeRecordBindingCandidateNames(report.candidateSourceNames),
    reportSort: report.reportSort || index + 1
  }
}

const buildRecordBindings = (
  row?: Pick<ProRouteFlowProcessConfigVO, 'formBindings'>
): RouteFlowRecordBinding[] => {
  return (row?.formBindings || [])
    .slice()
    .sort((first, second) => (first.reportSort || 0) - (second.reportSort || 0))
    .map((report, index) => normalizeFormBinding(report, index))
    .filter((binding): binding is RouteFlowRecordBinding => Boolean(binding))
}

const normalizeLegacyBatchRecord = (
  report: ProRouteFlowBatchRecordVO,
  index: number
): RouteFlowLegacyBatchRecord | undefined => {
  const batchRecordReportId = normalizeNullableText(report.batchRecordReportId)
  if (!batchRecordReportId) return undefined
  return {
    ...report,
    batchRecordReportId,
    batchRecordReportCode: normalizeNullableText(report.batchRecordReportCode) || null,
    batchRecordReportName: normalizeNullableText(report.batchRecordReportName) || null,
    formSlotType: resolveRecordBindingSlotType(report.formSlotType),
    reportSort: report.reportSort || index + 1
  }
}

const buildLegacyBatchRecords = (
  reports?: ProRouteFlowProcessConfigVO['batchRecordReports']
): RouteFlowLegacyBatchRecord[] =>
  (reports || [])
    .slice()
    .sort((first, second) => (first.reportSort || 0) - (second.reportSort || 0))
    .map((report, index) => normalizeLegacyBatchRecord(report, index))
    .filter((report): report is RouteFlowLegacyBatchRecord => Boolean(report))

const isRecordBindingConfigured = (binding?: Pick<RouteFlowRecordBinding, 'formTemplateId'>) =>
  Number(binding?.formTemplateId || 0) > 0

const isLegacyBatchRecordConfigured = (report?: Pick<RouteFlowLegacyBatchRecord, 'batchRecordReportId'>) =>
  Boolean(normalizeNullableText(report?.batchRecordReportId))

const isMainBatchRecordForm = (report: RouteFlowLegacyBatchRecord) =>
  resolveRecordBindingSlotType(report.formSlotType) === 'MAIN'

const requireBatchRecordFormSlotType = (report: RouteFlowLegacyBatchRecord) => {
  const formSlotType = resolveRecordBindingSlotType(report.formSlotType)
  if (!formSlotType) {
    throw new Error(`批记录表单缺少槽位类型: reportId=${report.batchRecordReportId}`)
  }
  return formSlotType
}

const getRouteNodeBatchRecordBindings = (node: RouteFlowNodeVO): RouteFlowRecordBinding[] => {
  const draftRecordBindings = selectedProcessAttributeDrafts[node.routeProcessId]?.recordBindings
  if (draftRecordBindings) return draftRecordBindings
  const batchConfig = findRouteProcessConfig(
    selectedProcessRouteConfigCache.value?.batchConfigs || [],
    node.routeProcessId
  )
  return buildRecordBindings(batchConfig)
}

const getRouteNodeLegacyBatchRecords = (node: RouteFlowNodeVO): RouteFlowLegacyBatchRecord[] => {
  const draftBatchRecordForms = selectedProcessAttributeDrafts[node.routeProcessId]?.legacyBatchRecords
  if (draftBatchRecordForms) return draftBatchRecordForms
  const batchConfig = findRouteProcessConfig(
    selectedProcessRouteConfigCache.value?.batchConfigs || [],
    node.routeProcessId
  )
  return buildLegacyBatchRecords(batchConfig?.batchRecordReports)
}

const getRouteNodeBatchRecordForms = (node: RouteFlowNodeVO) =>
  getRouteNodeLegacyBatchRecords(node).filter(isMainBatchRecordForm)

const getRouteNodeAdditionalFormCount = (node: RouteFlowNodeVO) => {
  return getRouteNodeBatchRecordBindings(node).filter(
    (binding) => normalizeRecordBindingSlotType(binding.formSlotType, binding.formBindingKey) !== 'MAIN'
  ).length
}

const isRouteNodeFormSlotConfigured = (node: RouteFlowNodeVO) =>
  getRouteNodeAdditionalFormCount(node) > 0

const isRouteNodeBatchRecordFormConfigured = (node: RouteFlowNodeVO) =>
  getRouteNodeBatchRecordForms(node).some(isLegacyBatchRecordConfigured)

const isRouteNodeWorkstationBound = (node: RouteFlowNodeVO) => {
  const routeProcess = candidateAwareRouteProcessRows.value.find(
    (row) => Number(row.id) === Number(node.routeProcessId)
  )
  return Boolean(getBoundRouteProcessWorkstationId(routeProcess))
}

const getRouteNodeBindingStatus = (node: RouteFlowNodeVO): RouteNodeBindingStatus => {
  const fieldKey = selectedProcessDetailFieldKey.value
  if (!fieldKey || !ROUTE_NODE_BINDING_STATUS_FIELD_KEYS.has(fieldKey)) return 'none'
  if (fieldKey === FORM_SLOT_AGGREGATE_FIELD_KEY) {
    return getRouteNodeAdditionalFormCount(node) > 0 ? 'bound' : 'none'
  }
  if (fieldKey === 'batchRecordFormNames') {
    return isRouteNodeBatchRecordFormConfigured(node) ? 'bound' : 'missing'
  }
  if (fieldKey === 'productionQuantityFactor') {
    return isRouteNodeProductionQuantityFactorOverridden(node) ? 'bound' : 'missing'
  }
  if (fieldKey === 'workstation') {
    return isRouteNodeWorkstationBound(node) ? 'bound' : 'missing'
  }
  if (fieldKey === 'keyFlag') return Boolean(node.keyFlag) ? 'bound' : 'missing'
  if (fieldKey === 'checkFlag') return Boolean(node.checkFlag) ? 'bound' : 'missing'
  return 'none'
}

const isFormSlotAggregateDetailField = (fieldKey: ProcessDetailFieldKey) =>
  fieldKey === FORM_SLOT_AGGREGATE_FIELD_KEY

const isBatchSharedBinding = (binding?: RouteFlowRecordBinding) =>
  normalizeRecordBindingInstanceScope(binding?.instanceScope) === 'BATCH_SHARED'

const isRecordBindingProcessIndependent = (binding?: RouteFlowRecordBinding) =>
  normalizeRecordBindingInstanceScope(binding?.instanceScope) === 'PROCESS'

const syncSelectedRecordBindingsToDraft = () => {
  const { draft } = ensureSelectedProcessAttributeDraft()
  draft.recordBindings = resequenceRecordBindings(selectedRecordBindings.value)
  markGraphDraftChanged()
}

const dedupeFormTemplateOptions = (items: FormTemplateListItemVO[]) => {
  const optionByTemplateId = new Map<number, FormTemplateListItemVO>()
  items.forEach((item) => {
    if (!optionByTemplateId.has(item.templateId)) {
      optionByTemplateId.set(item.templateId, item)
    }
  })
  return Array.from(optionByTemplateId.values())
}

const loadFormTemplateOptions = async (templateName?: string) => {
  formTemplateOptionLoading.value = true
  try {
    const data = await getTemplatePool({
      pageNo: 1,
      pageSize: 50,
      templateName: templateName || undefined,
      status: 'PUBLISHED'
    })
    formTemplateOptions.value = dedupeFormTemplateOptions(data.list || [])
  } finally {
    formTemplateOptionLoading.value = false
  }
}

const buildFormTemplateOptions = (binding: RouteFlowRecordBinding) => {
  const options = formTemplateOptions.value
  if (!binding.formTemplateId) return options
  const exists = options.some((item) => Number(item.templateId) === Number(binding.formTemplateId))
  if (exists) return options
  return [
    {
      templateId: Number(binding.formTemplateId),
      templateName: binding.formTemplateName || `模板 ${binding.formTemplateId}`,
      versionNo: '',
      status: 'PUBLISHED' as const,
      updatedTime: ''
    },
    ...options
  ]
}

const buildFormTemplateOptionLabel = (item: FormTemplateListItemVO) => item.templateName

const loadRecordBindingUserOptions = async () => {
  if (recordBindingUserOptions.value.length > 0) return
  recordBindingUserOptionsLoading.value = true
  try {
    recordBindingUserOptions.value = await getSimpleUserList()
  } finally {
    recordBindingUserOptionsLoading.value = false
  }
}

const loadRecordBindingRoleOptions = async () => {
  if (recordBindingRoleOptions.value.length > 0) return
  recordBindingRoleOptionsLoading.value = true
  try {
    recordBindingRoleOptions.value = await getSimpleRoleList()
  } finally {
    recordBindingRoleOptionsLoading.value = false
  }
}

const loadRecordBindingCandidateOptions = async (binding: RouteFlowRecordBinding) => {
  if (normalizeRecordBindingCandidateSourceType(binding.candidateSourceType) === 'ROLE') {
    await loadRecordBindingRoleOptions()
    return
  }
  await loadRecordBindingUserOptions()
}

const isRecordBindingCandidateOptionsLoading = (binding: RouteFlowRecordBinding) =>
  normalizeRecordBindingCandidateSourceType(binding.candidateSourceType) === 'ROLE'
    ? recordBindingRoleOptionsLoading.value
    : recordBindingUserOptionsLoading.value

const getRecordBindingCandidateSourceId = (binding: RouteFlowRecordBinding) =>
  normalizeRecordBindingCandidateIds(binding.candidateSourceIds)[0]

const hasRecordBindingFillerOverride = (binding: RouteFlowRecordBinding) =>
  Boolean(
    normalizeRecordBindingCandidateSourceType(binding.candidateSourceType) &&
      getRecordBindingCandidateSourceId(binding)
  )

const buildRecordBindingCandidateOptions = (
  binding: RouteFlowRecordBinding
): RecordBindingCandidateOption[] => {
  const candidateSourceType = normalizeRecordBindingCandidateSourceType(binding.candidateSourceType)
  const options =
    candidateSourceType === 'ROLE'
      ? recordBindingRoleOptions.value.map((role) => ({
          label: formatRoleOptionLabel(role),
          value: role.id
        }))
      : recordBindingUserOptions.value.map((user) => ({
          label: formatUserOptionLabel(user),
          value: user.id
        }))
  const selectedId = getRecordBindingCandidateSourceId(binding)
  if (!selectedId || options.some((option) => Number(option.value) === Number(selectedId))) {
    return options
  }
  return [
    {
      label: normalizeRecordBindingCandidateNames(binding.candidateSourceNames)[0] || String(selectedId),
      value: selectedId
    },
    ...options
  ]
}

const buildRecordBindingCandidateSummary = (binding: RouteFlowRecordBinding) => {
  const sourceType = normalizeRecordBindingCandidateSourceType(binding.candidateSourceType)
  const selectedId = getRecordBindingCandidateSourceId(binding)
  if (!sourceType || !selectedId) return '默认使用表单填写人'
  const names = normalizeRecordBindingCandidateNames(binding.candidateSourceNames)
  const sourceLabel = sourceType === 'ROLE' ? '角色' : '个人'
  return `覆盖${sourceLabel}：${names[0] || selectedId}`
}

const formatRecordBindingFillerSummary = (binding: RouteFlowRecordBinding) => {
  const sourceType = normalizeRecordBindingCandidateSourceType(binding.candidateSourceType)
  const candidateSourceNames = normalizeRecordBindingCandidateNames(binding.candidateSourceNames)
  const candidateSourceIds = normalizeRecordBindingCandidateIds(binding.candidateSourceIds)
  if (!sourceType || candidateSourceIds.length === 0) return '未配置'
  const sourceLabel = sourceType === 'ROLE' ? '角色' : '个人'
  const displayValue = candidateSourceNames.join('、') || candidateSourceIds.join('、')
  return `${sourceLabel}：${displayValue}`
}

const formatRecordBindingProcessIndependentSummary = (binding: RouteFlowRecordBinding) =>
  isRecordBindingProcessIndependent(binding) ? '是' : '否'

const hasDuplicateFormTemplate = (
  binding: RouteFlowRecordBinding,
  formTemplateId?: number | string | null
) =>
  Boolean(
    formTemplateId &&
      selectedRecordBindings.value.some(
        (item) =>
          item.formBindingKey !== binding.formBindingKey &&
          Number(item.formTemplateId || 0) === Number(formTemplateId)
      )
  )

const validateDuplicateFormTemplate = (
  bindings: Array<{ formBindingKey?: string | null; formTemplateId?: number | null }>
) => {
  const templateIds = new Set<number>()
  for (const binding of bindings) {
    const templateId = Number(binding.formTemplateId || 0)
    if (!templateId) continue
    if (templateIds.has(templateId)) {
      throw new Error('同一工序表单重复：同一个表单模板只能选择一次。')
    }
    templateIds.add(templateId)
  }
}

const applyRecordBindingInstanceScope = (
  binding: RouteFlowRecordBinding,
  instanceScope: RouteFlowRecordInstanceScope
) => {
  binding.instanceScope = instanceScope
  binding.sharedFormKey = instanceScope === 'BATCH_SHARED' ? buildSharedRecordBindingKey(binding) : null
  binding.fillableScopeJson =
    instanceScope === 'BATCH_SHARED' ? SHARED_FORM_FILLABLE_SCOPE_JSON : null
  binding.requiredPolicy = 'REQUIRED'
}

const resolveRouteWideRecordBindingInstanceScope = (
  formTemplateId?: number | string | null
): RouteFlowRecordInstanceScope => {
  const templateId = Number(formTemplateId || 0)
  if (!Number.isFinite(templateId) || templateId <= 0) return 'BATCH_SHARED'
  for (const node of routeNodes.value) {
    const matchedBinding = getRouteNodeBatchRecordBindings(node).find(
      (binding) => Number(binding.formTemplateId || 0) === templateId
    )
    if (matchedBinding) {
      return normalizeRecordBindingInstanceScope(matchedBinding.instanceScope)
    }
  }
  return 'BATCH_SHARED'
}

const applyRecordBindingProcessIndependentByTemplate = (
  bindings: RouteFlowRecordBinding[],
  formTemplateId: number,
  processIndependent: boolean
) => {
  const instanceScope: RouteFlowRecordInstanceScope = processIndependent
    ? 'PROCESS'
    : 'BATCH_SHARED'
  let changed = false
  bindings.forEach((binding) => {
    if (Number(binding.formTemplateId || 0) === formTemplateId) {
      applyRecordBindingInstanceScope(binding, instanceScope)
      changed = true
    }
  })
  return changed
}

const syncRouteWideRecordBindingProcessIndependent = (
  formTemplateId: number,
  processIndependent: boolean
) => {
  let changed = false
  routeNodes.value.forEach((node) => {
    const draft = getOrCreateRouteProcessAttributeDraft(node.routeProcessId)
    if (applyRecordBindingProcessIndependentByTemplate(draft.recordBindings, formTemplateId, processIndependent)) {
      changed = true
      if (Number(selectedProcessAttributes.routeProcessId) === Number(node.routeProcessId)) {
        selectedRecordBindings.value = cloneRecordBindings(draft.recordBindings)
      }
    }
  })
  if (changed) {
    markGraphDraftChanged()
  }
}

const applyRecordBindingFillerOverride = (
  binding: RouteFlowRecordBinding,
  filler: RouteFlowRecordBindingFillerOverride
) => {
  binding.candidateSourceType = normalizeRecordBindingCandidateSourceType(filler.candidateSourceType)
  binding.candidateSourceIds = normalizeRecordBindingCandidateIds(filler.candidateSourceIds)
  binding.candidateSourceNames = normalizeRecordBindingCandidateNames(filler.candidateSourceNames)
}

const buildRecordBindingFillerOverride = (
  binding: RouteFlowRecordBinding
): RouteFlowRecordBindingFillerOverride => ({
  candidateSourceType: normalizeRecordBindingCandidateSourceType(binding.candidateSourceType),
  candidateSourceIds: normalizeRecordBindingCandidateIds(binding.candidateSourceIds),
  candidateSourceNames: normalizeRecordBindingCandidateNames(binding.candidateSourceNames)
})

const serializeRecordBindingFillerOverride = (binding: RouteFlowRecordBinding) =>
  JSON.stringify(buildRecordBindingFillerOverride(binding))

const applyRouteWideRecordBindingFillerByTemplate = (
  bindings: RouteFlowRecordBinding[],
  formTemplateId: number,
  filler: RouteFlowRecordBindingFillerOverride
) => {
  let changed = false
  bindings.forEach((binding) => {
    if (Number(binding.formTemplateId || 0) === formTemplateId && isBatchSharedBinding(binding)) {
      const before = serializeRecordBindingFillerOverride(binding)
      applyRecordBindingFillerOverride(binding, filler)
      changed = changed || before !== serializeRecordBindingFillerOverride(binding)
    }
  })
  return changed
}

const syncRouteWideRecordBindingFillerByTemplate = (sourceBinding: RouteFlowRecordBinding) => {
  const formTemplateId = Number(sourceBinding.formTemplateId || 0)
  if (!Number.isFinite(formTemplateId) || formTemplateId <= 0 || !isBatchSharedBinding(sourceBinding)) {
    return false
  }
  const filler = buildRecordBindingFillerOverride(sourceBinding)
  let changed = false
  routeNodes.value.forEach((node) => {
    const draft = getOrCreateRouteProcessAttributeDraft(node.routeProcessId)
    if (applyRouteWideRecordBindingFillerByTemplate(draft.recordBindings, formTemplateId, filler)) {
      changed = true
      if (Number(selectedProcessAttributes.routeProcessId) === Number(node.routeProcessId)) {
        selectedRecordBindings.value = cloneRecordBindings(draft.recordBindings)
      }
    }
  })
  if (changed) {
    markGraphDraftChanged()
  }
  return changed
}

const updateRecordBindingTemplate = (
  binding: RouteFlowRecordBinding,
  formTemplateId?: number | string | null
) => {
  if (formTemplateId && hasDuplicateFormTemplate(binding, formTemplateId)) {
    message.error('同一工序表单重复：同一个表单模板只能选择一次。')
    return false
  }
  const templateId = Number(formTemplateId || 0)
  const option = formTemplateOptions.value.find((item) => Number(item.templateId) === templateId)
  binding.formTemplateId = templateId > 0 ? templateId : null
  binding.formTemplateName = option?.templateName || null
  binding.permissionRule = null
  if (!templateId) {
    binding.instanceScope = 'BATCH_SHARED'
    binding.sharedFormKey = null
    binding.fillableScopeJson = null
    binding.requiredPolicy = 'REQUIRED'
    binding.candidateSourceType = null
    binding.candidateSourceIds = []
    binding.candidateSourceNames = []
  } else {
    applyRecordBindingInstanceScope(binding, resolveRouteWideRecordBindingInstanceScope(templateId))
  }
  return true
}

const handleSelectedRecordBindingTemplateChange = (
  binding: RouteFlowRecordBinding,
  formTemplateId?: number | string | null
) => {
  if (!binding || recordBindingEditorDisabled.value) return
  if (!updateRecordBindingTemplate(binding, formTemplateId)) return
  syncSelectedRecordBindingsToDraft()
}

const handleRecordBindingProcessIndependentChange = (
  binding: RouteFlowRecordBinding,
  processIndependent: boolean
) => {
  if (!binding || recordBindingEditorDisabled.value) return
  const formTemplateId = Number(binding.formTemplateId || 0)
  if (!Number.isFinite(formTemplateId) || formTemplateId <= 0) {
    message.error('请先选择表单后再设置工序独立。')
    return
  }
  syncRouteWideRecordBindingProcessIndependent(formTemplateId, processIndependent)
}

const addSelectedRecordBinding = async () => {
  if (recordBindingEditorDisabled.value) return
  selectedRecordBindings.value = [...selectedRecordBindings.value, createEmptyRecordBinding()]
  syncSelectedRecordBindingsToDraft()
  if (formTemplateOptions.value.length === 0) {
    await loadFormTemplateOptions()
  }
}

const removeSelectedRecordBinding = (binding: RouteFlowRecordBinding) => {
  if (recordBindingEditorDisabled.value) return
  selectedRecordBindings.value = selectedRecordBindings.value.filter(
    (item) => item.formBindingKey !== binding.formBindingKey
  )
  delete recordBindingCopySourceByKey[binding.formBindingKey]
  syncSelectedRecordBindingsToDraft()
}

const moveSelectedRecordBinding = (index: number, direction: -1 | 1) => {
  if (recordBindingEditorDisabled.value) return
  const nextIndex = index + direction
  if (nextIndex < 0 || nextIndex >= selectedRecordBindings.value.length) return
  const next = [...selectedRecordBindings.value]
  const [item] = next.splice(index, 1)
  next.splice(nextIndex, 0, item)
  selectedRecordBindings.value = next
  syncSelectedRecordBindingsToDraft()
}

const handleSelectedRecordBindingCandidateSourceTypeChange = (
  binding: RouteFlowRecordBinding,
  candidateSourceType: string
) => {
  if (!binding || recordBindingEditorDisabled.value) return
  applyRecordBindingFillerOverride(binding, {
    candidateSourceType: normalizeRecordBindingCandidateSourceType(candidateSourceType),
    candidateSourceIds: [],
    candidateSourceNames: []
  })
  if (!syncRouteWideRecordBindingFillerByTemplate(binding)) {
    syncSelectedRecordBindingsToDraft()
  }
  if (!binding.candidateSourceType) return
  void loadRecordBindingCandidateOptions(binding)
}

const handleSelectedRecordBindingCandidateIdChange = (
  binding: RouteFlowRecordBinding,
  candidateSourceId?: number | string | null
) => {
  if (!binding || recordBindingEditorDisabled.value) return
  const id = Number(candidateSourceId || 0)
  if (!Number.isFinite(id) || id <= 0) {
    applyRecordBindingFillerOverride(binding, {
      candidateSourceType: normalizeRecordBindingCandidateSourceType(binding.candidateSourceType),
      candidateSourceIds: [],
      candidateSourceNames: []
    })
    if (!syncRouteWideRecordBindingFillerByTemplate(binding)) {
      syncSelectedRecordBindingsToDraft()
    }
    return
  }
  const option = buildRecordBindingCandidateOptions(binding).find(
    (item) => Number(item.value) === Number(id)
  )
  applyRecordBindingFillerOverride(binding, {
    candidateSourceType: normalizeRecordBindingCandidateSourceType(binding.candidateSourceType),
    candidateSourceIds: [id],
    candidateSourceNames: option?.label ? [option.label] : []
  })
  if (!syncRouteWideRecordBindingFillerByTemplate(binding)) {
    syncSelectedRecordBindingsToDraft()
  }
}

const clearSelectedRecordBindingFillerOverride = (binding: RouteFlowRecordBinding) => {
  if (!binding || recordBindingEditorDisabled.value) return
  applyRecordBindingFillerOverride(binding, {
    candidateSourceType: null,
    candidateSourceIds: [],
    candidateSourceNames: []
  })
  if (!syncRouteWideRecordBindingFillerByTemplate(binding)) {
    syncSelectedRecordBindingsToDraft()
  }
}

const validateBatchSharedRecordBinding = (binding: RouteFlowRecordBinding) => {
  if (!isBatchSharedBinding(binding)) return
  if (!buildSharedRecordBindingKey(binding)) {
    throw new Error(`${getFormBindingDisplayName(binding)}共享表单缺少共享身份。`)
  }
  JSON.parse(SHARED_FORM_FILLABLE_SCOPE_JSON)
}

const validateRecordBindingCandidateSource = (binding: RouteFlowRecordBinding) => {
  const displayName = getFormBindingDisplayName(binding)
  const sourceType = normalizeRecordBindingCandidateSourceType(binding.candidateSourceType)
  const sourceIds = normalizeRecordBindingCandidateIds(binding.candidateSourceIds)
  if (!sourceType) {
    throw new Error(`${displayName}填写人配置缺少来源。`)
  }
  if (sourceIds.length !== 1) {
    throw new Error(`${displayName}填写人配置必须选择一个人员或一个权限角色。`)
  }
  binding.candidateSourceType = sourceType
  binding.candidateSourceIds = sourceIds
  binding.candidateSourceNames = normalizeRecordBindingCandidateNames(binding.candidateSourceNames)
}

const getFormBindingDisplayName = (binding: RouteFlowRecordBinding) =>
  binding.formTemplateName ||
  binding.formTemplateNameSnapshot ||
  (binding.formTemplateId ? `模板 ${binding.formTemplateId}` : '未选择表单')

const getLegacyBatchRecordDisplayName = (report: RouteFlowLegacyBatchRecord) =>
  report.batchRecordReportName ||
  report.batchRecordReportCode ||
  report.batchRecordReportId ||
  '未命名批记录表单'

const buildRecordBindingCopySourceValue = (routeProcessId: number, binding: RouteFlowRecordBinding) =>
  `${routeProcessId}::${binding.formBindingKey}`

const getRecordBindingCopySourceOptions = (
  targetBinding: RouteFlowRecordBinding
): RecordBindingCopySourceOption[] => {
  const currentRouteProcessId = selectedProcessAttributes.routeProcessId
  return routeNodes.value
    .filter((node) => node.routeProcessId !== currentRouteProcessId)
    .flatMap((node) =>
      getRouteNodeBatchRecordBindings(node)
        .filter((binding) => isRecordBindingConfigured(binding))
        .map((binding, index) => ({
          label: `${nodeLabel(node)} / ${index + 1}. ${getFormBindingDisplayName(binding)} / ${buildRecordBindingCandidateSummary(binding)}`,
          value: buildRecordBindingCopySourceValue(node.routeProcessId, binding),
          routeProcessId: node.routeProcessId,
          binding
        }))
    )
    .filter((option) => option.value !== buildRecordBindingCopySourceValue(currentRouteProcessId || 0, targetBinding))
}

const getProcessFormBindingCopySourceOptions = (): ProcessFormBindingCopySourceOption[] => {
  const currentRouteProcessId = selectedProcessAttributes.routeProcessId
  return routeNodes.value
    .filter((node) => node.routeProcessId !== currentRouteProcessId)
    .map((node) => {
      const bindings = getRouteNodeBatchRecordBindings(node).filter((binding) =>
        isRecordBindingConfigured(binding)
      )
      return {
        label: `${nodeLabel(node)}（${bindings.length} 个表单）`,
        value: node.routeProcessId,
        routeProcessId: node.routeProcessId,
        bindings
      }
    })
    .filter((option) => option.bindings.length > 0)
}

const findProcessFormBindingCopySourceOption = () => {
  const sourceRouteProcessId = processFormBindingCopySourceRouteProcessId.value
  if (!sourceRouteProcessId) return undefined
  return getProcessFormBindingCopySourceOptions().find(
    (option) => option.routeProcessId === sourceRouteProcessId
  )
}

const findRecordBindingCopySourceOption = (
  targetBinding: RouteFlowRecordBinding
): RecordBindingCopySourceOption | undefined => {
  const sourceValue = recordBindingCopySourceByKey[targetBinding.formBindingKey]
  if (!sourceValue) return undefined
  return getRecordBindingCopySourceOptions(targetBinding).find((option) => option.value === sourceValue)
}

const handleRecordBindingCopySourceChange = (
  binding: RouteFlowRecordBinding,
  sourceValue: string
) => {
  if (!sourceValue) {
    delete recordBindingCopySourceByKey[binding.formBindingKey]
    return
  }
  recordBindingCopySourceByKey[binding.formBindingKey] = sourceValue
}

const handleProcessFormBindingCopySourceChange = (routeProcessId?: number | string | null) => {
  const normalizedRouteProcessId = Number(routeProcessId || 0)
  processFormBindingCopySourceRouteProcessId.value =
    Number.isFinite(normalizedRouteProcessId) && normalizedRouteProcessId > 0
      ? normalizedRouteProcessId
      : null
}

const handleProcessFormBindingCopyPopoverHide = () => {
  processFormBindingCopySourceRouteProcessId.value = null
}

const copySelectedRecordBindingFromSource = (targetBinding: RouteFlowRecordBinding) => {
  if (recordBindingEditorDisabled.value) return
  const sourceOption = findRecordBindingCopySourceOption(targetBinding)
  if (!sourceOption) {
    message.error('请选择要复制的来源表单槽位。')
    return
  }
  const sourceBinding = sourceOption.binding
  if (sourceBinding.formTemplateId && hasDuplicateFormTemplate(targetBinding, sourceBinding.formTemplateId)) {
    message.error('同一工序表单重复：同一个表单模板只能选择一次。')
    return
  }
  const instanceScope = normalizeRecordBindingInstanceScope(sourceBinding.instanceScope)
  const targetKey = targetBinding.formBindingKey
  const targetSort = targetBinding.reportSort
  Object.assign(targetBinding, {
    formSlotType: sourceBinding.formSlotType,
    formTemplateId: sourceBinding.formTemplateId,
    formTemplateName: sourceBinding.formTemplateName || sourceBinding.formTemplateNameSnapshot || null,
    formTemplateNameSnapshot: sourceBinding.formTemplateNameSnapshot || null,
    lastPublishedTemplateVersionId: sourceBinding.lastPublishedTemplateVersionId || null,
    lastPublishedTemplateVersionNo: sourceBinding.lastPublishedTemplateVersionNo || null,
    instanceScope,
    sharedFormKey: instanceScope === 'BATCH_SHARED' ? buildSharedRecordBindingKey(sourceBinding) : null,
    fillableScopeJson:
      instanceScope === 'BATCH_SHARED' ? SHARED_FORM_FILLABLE_SCOPE_JSON : null,
    recordCategory: sourceBinding.recordCategory || null,
    validationProfile: sourceBinding.validationProfile || null,
    requiredPolicy: normalizeRecordBindingRequiredPolicy(sourceBinding.requiredPolicy),
    requiredConditionJson: sourceBinding.requiredConditionJson || null,
    ownerRoleKey: sourceBinding.ownerRoleKey || null,
    archiveVisibility: sourceBinding.archiveVisibility || null,
    permissionRule: null,
    candidateSourceType: normalizeRecordBindingCandidateSourceType(sourceBinding.candidateSourceType),
    candidateSourceIds: normalizeRecordBindingCandidateIds(sourceBinding.candidateSourceIds),
    candidateSourceNames: normalizeRecordBindingCandidateNames(sourceBinding.candidateSourceNames),
    remark: sourceBinding.remark || null,
    formBindingKey: targetKey,
    reportSort: targetSort
  })
  delete recordBindingCopySourceByKey[targetKey]
  syncSelectedRecordBindingsToDraft()
  message.success('已复制表单槽位配置')
}

const copySelectedProcessFormBindingsFromSource = () => {
  if (recordBindingEditorDisabled.value) return
  const sourceOption = findProcessFormBindingCopySourceOption()
  if (!sourceOption) {
    message.error('请选择要复制的来源工序。')
    return
  }
  const sourceBindings = sourceOption.bindings
  selectedRecordBindings.value = sourceBindings.map(copyRecordBindingForSelectedProcess)
  Object.keys(recordBindingCopySourceByKey).forEach((key) => {
    delete recordBindingCopySourceByKey[key]
  })
  processFormBindingCopySourceRouteProcessId.value = null
  processFormBindingCopyPopoverVisible.value = false
  syncSelectedRecordBindingsToDraft()
  message.success('已复制工序表单绑定关系')
}

const copyRecordBindingForSelectedProcess = (
  sourceBinding: RouteFlowRecordBinding,
  index: number
): RouteFlowRecordBinding => {
  const formBindingKey = createLocalFormBindingKey()
  const formSlotType = normalizeRecordBindingSlotType(
    sourceBinding.formSlotType,
    sourceBinding.formBindingKey
  )
  const formTemplateId = Number(sourceBinding.formTemplateId || 0)
  const normalizedFormTemplateId =
    Number.isFinite(formTemplateId) && formTemplateId > 0 ? formTemplateId : null
  const instanceScope = normalizeRecordBindingInstanceScope(sourceBinding.instanceScope)
  return {
    formBindingKey,
    formSlotType,
    formTemplateId: normalizedFormTemplateId,
    formTemplateName: sourceBinding.formTemplateName || sourceBinding.formTemplateNameSnapshot || null,
    formTemplateNameSnapshot: sourceBinding.formTemplateNameSnapshot || null,
    lastPublishedTemplateVersionId: sourceBinding.lastPublishedTemplateVersionId || null,
    lastPublishedTemplateVersionNo: sourceBinding.lastPublishedTemplateVersionNo || null,
    instanceScope,
    sharedFormKey:
      instanceScope === 'BATCH_SHARED'
        ? buildSharedRecordBindingKey({
            formSlotType,
            formBindingKey,
            formTemplateId: normalizedFormTemplateId
          })
        : null,
    fillableScopeJson:
      instanceScope === 'BATCH_SHARED' ? SHARED_FORM_FILLABLE_SCOPE_JSON : null,
    recordCategory: sourceBinding.recordCategory || null,
    validationProfile: sourceBinding.validationProfile || null,
    requiredPolicy: normalizeRecordBindingRequiredPolicy(sourceBinding.requiredPolicy),
    requiredConditionJson: sourceBinding.requiredConditionJson || null,
    ownerRoleKey: sourceBinding.ownerRoleKey || null,
    archiveVisibility: sourceBinding.archiveVisibility || null,
    permissionScopeId: sourceBinding.permissionScopeId ?? sourceBinding.permissionRule?.permissionScopeId ?? null,
    permissionRule: null,
    candidateSourceType: normalizeRecordBindingCandidateSourceType(sourceBinding.candidateSourceType),
    candidateSourceIds: normalizeRecordBindingCandidateIds(sourceBinding.candidateSourceIds),
    candidateSourceNames: normalizeRecordBindingCandidateNames(sourceBinding.candidateSourceNames),
    reportSort: index + 1,
    remark: sourceBinding.remark || null
  }
}

const buildRecordBindingValue = (binding: RouteFlowRecordBinding) =>
  getFormBindingDisplayName(binding)

const getSelectedBatchRecordForms = () =>
  selectedLegacyBatchRecords.value.filter(isMainBatchRecordForm)

const buildBatchRecordFormValue = () => {
  const displayNames = getSelectedBatchRecordForms()
    .filter(isLegacyBatchRecordConfigured)
    .map(getLegacyBatchRecordDisplayName)
  return displayNames.length ? displayNames.join('、') : '未配置'
}

const buildBatchRecordFormLinks = (): ProcessDetailLinkItem[] =>
  getSelectedBatchRecordForms()
    .filter(isLegacyBatchRecordConfigured)
    .map((report, index) => ({
      key: `batch-record-form-${report.batchRecordReportId || index}`,
      label: getLegacyBatchRecordDisplayName(report),
      onClick: () => openLegacyBatchRecordTargetLink(report)
    }))

const buildFormSlotSummaryValue = () => {
  const configuredSlots = selectedRecordBindings.value
    .filter((binding) => isRecordBindingConfigured(binding))
    .map((binding, index) => `${index + 1}. ${buildRecordBindingValue(binding)}`)
  return configuredSlots.length ? configuredSlots.join('；') : '未配置'
}

const buildFormSlotViewSummaryItems = () => {
  const summaryItems: FormSlotViewSummaryItem[] = selectedRecordBindings.value
    .filter((binding) => isRecordBindingConfigured(binding))
    .map((binding, index) => ({
      key: binding.formBindingKey || `${binding.formTemplateId || 'form'}-${index}`,
      index: index + 1,
      formName: getFormBindingDisplayName(binding),
      fillerSummary: formatRecordBindingFillerSummary(binding),
      processIndependentSummary: formatRecordBindingProcessIndependentSummary(binding)
    }))
  return summaryItems
}

const buildFormSlotSummaryLinks = (): ProcessDetailLinkItem[] => []

const resolveRouteProcessPredecessors = (row?: ProRouteProcessVO) => {
  if (row?.predecessors?.length) return row.predecessors
  return row?.predecessor ? [row.predecessor] : []
}

const buildRouteProcessRelationLinks = (
  relationKind: 'predecessor' | 'successor',
  relations: ProRouteProcessRelationVO[]
): ProcessDetailLinkItem[] =>
  relations.map((relation) => ({
    key: `${relationKind}-${relation.routeProcessId}`,
    label: relation.processName || relation.processCode || `工序 ${relation.routeProcessId}`,
    onClick: () => openRouteProcessRelationLink(relation)
  }))

const buildSelectedProcessTargetLinks = (
  fieldKey: ProcessDetailFieldKey,
  value?: string | number | boolean | null
) =>
  buildProcessDetailTextLink(`process-${fieldKey}`, formatProcessDetailText(value), () =>
    openProcessTargetLink()
  )

const buildProcessDetailFocusLinks = (
  fieldKey: ProcessDetailFieldKey,
  value: string | number | boolean | null | undefined,
  capacitySourceFocus?: ProcessDetailCapacitySourceFocus
) =>
  buildProcessDetailTextLink(
    `focus-${fieldKey}`,
    formatProcessDetailText(value),
    () => openSelectedProcessDetailFocusLink(fieldKey, capacitySourceFocus)
  )

const buildProcessDetailValueLinks = (
  fieldKey: ProcessDetailFieldKey,
  value?: string | number | boolean | null
): ProcessDetailLinkItem[] => {
  const routeProcess = selectedRouteProcess.value
  if (fieldKey === 'processCode' || fieldKey === 'processName') {
    return buildSelectedProcessTargetLinks(fieldKey, value)
  }
  if (fieldKey === 'workstation') {
    return buildWorkstationDetailLinks(routeProcess)
  }
  if (fieldKey === 'standardResource') {
    if (routeProcess?.capacitySource === 'MACHINE') {
      const machineryLinks = buildProcessDetailMachineryLinks()
      if (machineryLinks.length > 0) return machineryLinks
    }
    if (routeProcess?.capacitySource === 'WORKER') {
      return buildWorkstationDetailLinks(routeProcess)
    }
    return buildProcessDetailFocusLinks(fieldKey, value, 'resource')
  }
  if (fieldKey === 'predecessor') {
    return buildRouteProcessRelationLinks('predecessor', resolveRouteProcessPredecessors(routeProcess))
  }
  if (fieldKey === 'successors') {
    return buildRouteProcessRelationLinks('successor', routeProcess?.successors || [])
  }
  if (fieldKey === 'capacitySource' || fieldKey === 'resourceStatus') {
    return buildProcessDetailFocusLinks(fieldKey, value, 'resource')
  }
  if (
    fieldKey === 'standardShiftCapacity' ||
    fieldKey === 'productionQuantityFactor' ||
    fieldKey === 'shiftCapacity'
  ) {
    return buildProcessDetailFocusLinks(fieldKey, value, 'schedule')
  }
  return buildProcessDetailFocusLinks(fieldKey, value)
}

const getRouteProcessSettingColumnLabel = (
  key: RouteProcessSettingColumnKey,
  fallback: string
) => routeProcessSettingColumns.value.find((column) => column.key === key)?.label || fallback

const normalizeRouteProcessCapacityValue = (value?: number | string | null) => {
  if (value === undefined || value === null || value === '') return undefined
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : undefined
}

const formatRouteProcessCapacity = (value?: number | string | null) => {
  const numberValue = normalizeRouteProcessCapacityValue(value)
  if (numberValue === undefined) return '未配置'
  return numberValue.toLocaleString('zh-CN', {
    maximumFractionDigits: 6
  })
}

const formatRouteProcessIntegerCapacity = (value?: number | string | null) => {
  const numberValue = normalizeRouteProcessCapacityValue(value)
  if (numberValue === undefined) return '未配置'
  return numberValue.toLocaleString('zh-CN', {
    maximumFractionDigits: 0
  })
}

const formatRouteProcessShiftCapacity = (value?: number | string | null) => {
  const numberValue = normalizeRouteProcessCapacityValue(value)
  if (numberValue === undefined) return '未配置'
  return `${formatRouteProcessCapacity(numberValue)}/班次`
}

const formatRouteProcessIntegerShiftCapacity = (value?: number | string | null) => {
  const numberValue = normalizeRouteProcessCapacityValue(value)
  if (numberValue === undefined) return '未配置'
  return formatRouteProcessIntegerCapacity(numberValue)
}

const calculateCapacityOverrideShiftCapacity = (
  hourlyCapacity?: number | string | null,
  shiftHours?: number | string | null
) => {
  const hourlyCapacityValue = normalizeRouteProcessCapacityValue(hourlyCapacity)
  const shiftHoursValue = normalizeRouteProcessCapacityValue(shiftHours)
  if (hourlyCapacityValue === undefined || shiftHoursValue === undefined) return undefined
  return Number((hourlyCapacityValue * shiftHoursValue).toFixed(6))
}

const isRouteProcessCapacityOverrideDifferentFromDefault = (
  hourlyCapacity?: number | string | null,
  shiftHours?: number | string | null,
  defaultShiftCapacity?: number | string | null
) => {
  const overrideShiftCapacity = calculateCapacityOverrideShiftCapacity(hourlyCapacity, shiftHours)
  const overrideShiftCapacityValue = normalizeRouteProcessCapacityValue(overrideShiftCapacity)
  if (overrideShiftCapacityValue === undefined) return false
  const defaultShiftCapacityValue = normalizeRouteProcessCapacityValue(defaultShiftCapacity)
  if (defaultShiftCapacityValue === undefined) return true
  return (
    Math.abs(overrideShiftCapacityValue - defaultShiftCapacityValue) >
    CAPACITY_OVERRIDE_DIFF_TOLERANCE
  )
}

const isSelectedProcessCapacityOverrideActive = computed(() => {
  return (
    normalizeScheduleCapacityMode(selectedProcessAttributes.capacityMode) === 'MANUAL_OVERRIDE' &&
    isRouteProcessCapacityOverrideDifferentFromDefault(
      selectedProcessAttributes.hourlyCapacity,
      selectedProcessAttributes.shiftHours,
      selectedRouteProcess.value?.processShiftCapacityTotal
    )
  )
})

const selectedProcessCapacityOverrideShiftCapacity = computed(() =>
  calculateCapacityOverrideShiftCapacity(
    selectedProcessAttributes.hourlyCapacity,
    selectedProcessAttributes.shiftHours
  )
)

const capacityOverrideButtonDisabled = computed(
  () =>
    routeFlowWriteControlsDisabled.value ||
    !selectedNode.value ||
    (isDraftCandidateEdit.value &&
      selectedProcessAttributesLoading.value) ||
    capacityOverrideSaving.value ||
    capacityOverrideCandidateCreating.value
)
const capacityOverrideButtonTitle = computed(() =>
  isDraftCandidateEdit.value ? '产能覆盖' : CANDIDATE_EDIT_REQUIRED_MESSAGE
)

const formatRouteProcessWorkerQuantity = (value?: number | null) => {
  const numberValue = normalizeRouteProcessCapacityValue(value)
  return numberValue === undefined ? '未配置' : `${numberValue.toLocaleString('zh-CN')}人`
}

const formatRouteProcessMachineQuantity = (value?: number | null) => {
  const numberValue = normalizeRouteProcessCapacityValue(value)
  return numberValue === undefined ? '未配置' : `${numberValue.toLocaleString('zh-CN')}台`
}

const getRouteProcessCapacitySourceLabel = (value?: ProRouteProcessVO['capacitySource']) => {
  if (value === 'MACHINE') return '设备'
  if (value === 'WORKER') return '人工'
  return '未配置'
}

const getRouteProcessStandardResourceLabel = (row?: ProRouteProcessVO) => {
  if (!row) return '-'
  if (row.capacitySource === 'MACHINE') return formatRouteProcessMachineQuantity(row.machineryQuantityTotal)
  if (row.capacitySource === 'WORKER') return formatRouteProcessWorkerQuantity(row.workerQuantityTotal)
  return '未配置'
}

const getRouteProcessResourceStatusLabel = (value?: ProRouteProcessVO['resourceStatus']) => {
  if (value === 'NORMAL') return '正常'
  if (value === 'REPAIR') return '设备维修'
  if (value === 'CAPACITY_MISSING') return '产能缺失'
  if (value === 'UNCONFIGURED') return '资源未配置'
  return '未配置'
}

const formatRouteProcessPredecessors = (row?: ProRouteProcessVO) => {
  if (selectedRouteProcessId.value && Number(row?.id) === Number(selectedRouteProcessId.value)) {
    return formatRouteProcessIdList(selectedPredecessorRouteProcessIds.value)
  }
  const relations = resolveRouteProcessPredecessors(row)
  const names = relations
    .map((predecessor) => predecessor.processName || predecessor.processCode)
    .filter(Boolean)
  return names.length > 0 ? names.join('、') : '-'
}

const formatRouteProcessSuccessors = (row?: ProRouteProcessVO) => {
  if (selectedRouteProcessId.value && Number(row?.id) === Number(selectedRouteProcessId.value)) {
    return formatRouteProcessIdList(selectedSuccessorRouteProcessIds.value)
  }
  const names = (row?.successors || [])
    .map((successor) => successor.processName || successor.processCode)
    .filter(Boolean)
  return names.length > 0 ? names.join('、') : '-'
}

const formatRouteProcessWorkstation = (row?: ProRouteProcessVO) =>
  row?.workstationCode || row?.workstationName || '-'

function formatRouteProcessIdList(routeProcessIds: number[]) {
  const names = routeProcessIds
    .map((routeProcessId) => routeNodes.value.find((node) => node.routeProcessId === routeProcessId))
    .map((node) => (node ? nodeLabel(node) : ''))
    .filter(Boolean)
  return names.length > 0 ? names.join('、') : '-'
}

const formatRouteProcessScheduleStrategySummary = () => {
  const capacityMode = normalizeScheduleCapacityMode(selectedProcessAttributes.capacityMode)
  if (capacityMode === 'RESOURCE_CALCULATED') return '资源计算'
  if (capacityMode === 'INFINITE_FORMULA') return '无限公式'
  if (!isSelectedProcessCapacityOverrideActive.value) return '资源计算'
  return `产能覆盖：${formatRouteProcessCapacity(selectedProcessAttributes.hourlyCapacity)} 产能/h`
}

const processDetailFieldOptions = computed<ProcessDetailFieldOption[]>(() => {
  const routeProcess = selectedRouteProcess.value
  const node = selectedNode.value
  const attributeLoading = selectedProcessAttributesLoading.value
  return [
    {
      key: 'sort',
      label: getRouteProcessSettingColumnLabel('sort', '序号'),
      value: routeProcess?.sort ?? node?.sort,
      links: buildProcessDetailValueLinks('sort', routeProcess?.sort ?? node?.sort)
    },
    {
      key: 'processCode',
      label: getRouteProcessSettingColumnLabel('processCode', '工序编码'),
      value: routeProcess?.processCode ?? node?.processCode,
      links: buildProcessDetailValueLinks('processCode', routeProcess?.processCode ?? node?.processCode)
    },
    {
      key: 'processName',
      label: getRouteProcessSettingColumnLabel('processName', '工序名称'),
      value: routeProcess?.processName ?? node?.processName,
      links: buildProcessDetailValueLinks('processName', routeProcess?.processName ?? node?.processName)
    },
    {
      key: 'productionQuantityFactor',
      label: getRouteProcessSettingColumnLabel('productionQuantityFactor', '生产系数'),
      value: selectedProcessAttributes.productionQuantityFactor,
      links: buildProcessDetailValueLinks(
        'productionQuantityFactor',
        selectedProcessAttributes.productionQuantityFactor
      ),
      loading: attributeLoading,
      coverageStatus: attributeLoading ? undefined : getSelectedProductionQuantityFactorCoverageStatus()
    },
    {
      key: 'shiftCapacity',
      label: getRouteProcessSettingColumnLabel('shiftCapacity', '排产策略'),
      value: formatRouteProcessScheduleStrategySummary(),
      links: buildProcessDetailValueLinks('shiftCapacity', formatRouteProcessScheduleStrategySummary()),
      loading: attributeLoading
    },
    {
      key: 'formSlots',
      label: getRouteProcessSettingColumnLabel('formSlots', '表单槽位'),
      value: buildFormSlotSummaryValue(),
      links: buildFormSlotSummaryLinks(),
      loading: attributeLoading
    },
    {
      key: 'batchRecordFormNames',
      label: getRouteProcessSettingColumnLabel('batchRecordFormNames', '批记录表单'),
      value: buildBatchRecordFormValue(),
      links: buildBatchRecordFormLinks(),
      loading: attributeLoading
    },
    {
      key: 'resourceStatus',
      label: getRouteProcessSettingColumnLabel('resourceStatus', '资源状态'),
      value: routeProcess?.resourceStatusReason || getRouteProcessResourceStatusLabel(routeProcess?.resourceStatus),
      links: buildProcessDetailValueLinks(
        'resourceStatus',
        routeProcess?.resourceStatusReason || getRouteProcessResourceStatusLabel(routeProcess?.resourceStatus)
      )
    },
    {
      key: 'predecessor',
      label: getRouteProcessSettingColumnLabel('predecessor', '前置工序'),
      value: formatRouteProcessPredecessors(routeProcess),
      links: buildProcessDetailValueLinks('predecessor', formatRouteProcessPredecessors(routeProcess))
    },
    {
      key: 'successors',
      label: getRouteProcessSettingColumnLabel('successors', '后续工序'),
      value: formatRouteProcessSuccessors(routeProcess),
      links: buildProcessDetailValueLinks('successors', formatRouteProcessSuccessors(routeProcess))
    },
    {
      key: 'relationList',
      label: getRouteProcessSettingColumnLabel('relationList', '关系清单'),
      value: buildRouteProcessRelationListSummary(),
      links: []
    },
    {
      key: 'keyFlag',
      label: getRouteProcessSettingColumnLabel('keyFlag', '关键工序'),
      value: Boolean(node?.keyFlag),
      links: buildProcessDetailValueLinks('keyFlag', Boolean(node?.keyFlag))
    },
    {
      key: 'checkFlag',
      label: getRouteProcessSettingColumnLabel('checkFlag', '质检确认'),
      value: node?.checkFlag ?? routeProcess?.checkFlag,
      links: buildProcessDetailValueLinks('checkFlag', node?.checkFlag ?? routeProcess?.checkFlag)
    },
    {
      key: 'workstation',
      label: getRouteProcessSettingColumnLabel('workstation', '工作站'),
      value: formatRouteProcessWorkstation(routeProcess),
      links: buildProcessDetailValueLinks('workstation', formatRouteProcessWorkstation(routeProcess))
    }
  ]
})
const processDetailFieldOptionMap = computed(
  () => new Map(processDetailFieldOptions.value.map((field) => [field.key, field]))
)
const selectedProcessDetailField = computed(() =>
  selectedProcessDetailFieldKey.value
    ? processDetailFieldOptionMap.value.get(selectedProcessDetailFieldKey.value)
    : undefined
)
const getProcessDetailFieldSourceLabel = (fieldKey?: ProcessDetailFieldKey) => {
  if (!fieldKey) return '-'
  return isFormSlotAggregateDetailField(fieldKey) ? '表单槽位' : '基础字段'
}
const selectedProcessDetailFieldSource = computed(() =>
  getProcessDetailFieldSourceLabel(selectedProcessDetailFieldKey.value)
)
const processDetailFieldSelectOptions = computed<ProcessDetailFieldSelectOption[]>(() => {
  const selectedFieldKeySet = new Set(selectedProcessDetailFieldKeys.value)
  const slotOptions: ProcessDetailFieldSelectOption[] = []
  const basicOptions: ProcessDetailFieldSelectOption[] = []
  processDetailFieldOptions.value
    .filter((field) => PROCESS_DETAIL_FIELD_KEY_SET.has(field.key))
    .forEach((field) => {
      const disabled = selectedFieldKeySet.has(field.key)
      const option = {
        ...field,
        label: disabled ? `${field.label}（已添加）` : field.label,
        disabled
      }
      if (isFormSlotAggregateDetailField(field.key)) {
        slotOptions.push(option)
      } else {
        basicOptions.push(option)
      }
    })
  return [...slotOptions, ...basicOptions]
})
const availableProcessDetailFieldOptions = computed(() =>
  processDetailFieldSelectOptions.value.filter((field) => !field.disabled)
)
const isProcessDetailFieldEditable = (fieldKey: ProcessDetailFieldKey) =>
  canMutateRouteFlow.value &&
  isDraftCandidateEdit.value &&
  PROCESS_DETAIL_EDITABLE_FIELD_KEYS.has(fieldKey)
const selectedProcessDetailFields = computed(() =>
  selectedProcessDetailFieldKeys.value
    .map((key) => processDetailFieldOptionMap.value.get(key))
    .filter((field): field is ProcessDetailFieldOption => Boolean(field))
)

const syncSelectedProcessDetailFieldToAdd = () => {
  if (
    selectedProcessDetailFieldToAdd.value &&
    !availableProcessDetailFieldOptions.value.some(
      (field) => field.key === selectedProcessDetailFieldToAdd.value
    )
  ) {
    selectedProcessDetailFieldToAdd.value = undefined
  }
}

const normalizeProcessDetailFieldKey = (rawKey?: string | null) => {
  const key = String(rawKey || '').trim()
  if (PROCESS_DETAIL_STANDALONE_RESOURCE_FIELD_KEYS.has(key as RouteProcessSettingColumnKey)) {
    return 'workstation'
  }
  return key
}

const normalizeProcessDetailFieldKeys = (fieldKeys?: Array<string | null | undefined>) => {
  const uniqueKeys = new Set<ProcessDetailFieldKey>()
  for (const rawKey of fieldKeys || []) {
    const key = normalizeProcessDetailFieldKey(rawKey)
    if (PROCESS_DETAIL_FIELD_KEY_SET.has(key)) {
      uniqueKeys.add(key as ProcessDetailFieldKey)
    }
  }
  return Array.from(uniqueKeys)
}

const mergeRequiredProcessDetailFieldKeys = (fieldKeys: Array<string | null | undefined>) => {
  const selectedFieldKeys = normalizeProcessDetailFieldKeys(fieldKeys)
  const selectedFieldKeySet = new Set(selectedFieldKeys)
  return normalizeProcessDetailFieldKeys([
    ...selectedFieldKeys,
    ...REQUIRED_PROCESS_DETAIL_FIELD_KEYS.filter((key) => !selectedFieldKeySet.has(key))
  ])
}

const buildRouteFlowVersionCachePart = () => {
  const routeVersionId = props.routeVersionEditContext?.routeVersionId
  if (routeVersionId) return String(routeVersionId)
  throw new Error('流转关系图选择记忆失败：缺少路线版本编号。')
}

const buildRouteFlowLastSelectionCacheKey = () => {
  const userId = userStore.getUser?.id
  if (!userId) {
    throw new Error('流转关系图选择记忆失败：缺少当前用户编号。')
  }
  return `${ROUTE_FLOW_LAST_SELECTION_CACHE_PREFIX}:${userId}:${props.routeId}:${buildRouteFlowVersionCachePart()}`
}

const removeRouteFlowLastSelection = () => {
  wsCache.delete(buildRouteFlowLastSelectionCacheKey())
}

const readRouteFlowLastSelection = () => {
  const cachedState = wsCache.get(buildRouteFlowLastSelectionCacheKey()) as
    | Partial<RouteFlowLastSelectionState>
    | undefined
  if (!cachedState) return undefined
  const routeProcessId = Number(cachedState.routeProcessId)
  if (!Number.isFinite(routeProcessId) || routeProcessId === 0) {
    removeRouteFlowLastSelection()
    return undefined
  }
  const nextState: RouteFlowLastSelectionState = { routeProcessId }
  if (cachedState.detailFieldKey) {
    const detailFieldKey = normalizeProcessDetailFieldKey(String(cachedState.detailFieldKey))
    if (PROCESS_DETAIL_FIELD_KEY_SET.has(detailFieldKey as RouteProcessSettingColumnKey)) {
      nextState.detailFieldKey = detailFieldKey as ProcessDetailFieldKey
    } else {
      wsCache.set(buildRouteFlowLastSelectionCacheKey(), nextState)
    }
  }
  return nextState
}

const persistRouteFlowLastSelection = (selection: RouteFlowLastSelectionState) => {
  const routeProcessId = Number(selection.routeProcessId)
  if (!Number.isFinite(routeProcessId) || routeProcessId === 0) {
    removeRouteFlowLastSelection()
    return
  }
  const nextState: RouteFlowLastSelectionState = { routeProcessId }
  if (selection.detailFieldKey) {
    const detailFieldKey = normalizeProcessDetailFieldKey(selection.detailFieldKey)
    if (selectedProcessDetailFieldKeys.value.includes(detailFieldKey as ProcessDetailFieldKey)) {
      nextState.detailFieldKey = detailFieldKey as ProcessDetailFieldKey
    }
  }
  wsCache.set(buildRouteFlowLastSelectionCacheKey(), nextState)
}

const clearRouteFlowLastSelectionDetailField = (fieldKey?: ProcessDetailFieldKey) => {
  const cachedSelection = readRouteFlowLastSelection()
  if (!cachedSelection?.detailFieldKey) return
  if (fieldKey && cachedSelection.detailFieldKey !== fieldKey) return
  const nextState: RouteFlowLastSelectionState = {
    routeProcessId: cachedSelection.routeProcessId
  }
  wsCache.set(buildRouteFlowLastSelectionCacheKey(), nextState)
}

const clearRouteFlowLastSelectionRouteProcess = (routeProcessIds: Set<number>) => {
  const cachedSelection = readRouteFlowLastSelection()
  if (!cachedSelection || !routeProcessIds.has(cachedSelection.routeProcessId)) return
  removeRouteFlowLastSelection()
}

const selectRouteProcessNode = (
  routeProcessId: number,
  options: { persist?: boolean } = {}
) => {
  const nextRouteProcessId = Number(routeProcessId)
  if (!Number.isFinite(nextRouteProcessId) || !findNode(nextRouteProcessId)) {
    throw new Error(`流转关系图选择失败：路线工序不存在 routeProcessId=${routeProcessId}`)
  }
  selectedRouteProcessId.value = nextRouteProcessId
  selectedBoundaryType.value = null
  selectedBoundaryDetailFieldKey.value = undefined
  selectedEdgeKey.value = ''
  if (options.persist) {
    persistRouteFlowLastSelection({
      routeProcessId: nextRouteProcessId,
      detailFieldKey: selectedProcessDetailFieldKey.value
    })
  }
}

const selectProcessDetailField = (
  fieldKey: ProcessDetailFieldKey,
  options: { persist?: boolean } = {}
) => {
  if (!selectedProcessDetailFieldKeys.value.includes(fieldKey)) {
    throw new Error(`字段明细选择失败：配置项不存在 fieldKey=${fieldKey}`)
  }
  selectedProcessDetailFieldKey.value = fieldKey
  if (options.persist && selectedRouteProcessId.value !== null) {
    persistRouteFlowLastSelection({
      routeProcessId: selectedRouteProcessId.value,
      detailFieldKey: fieldKey
    })
  }
}

const handleSelectProcessDetailField = (fieldKey: ProcessDetailFieldKey) => {
  selectProcessDetailField(fieldKey, { persist: true })
}

const normalizeCapacitySourceFocus = () => {
  const focus = normalizeRouteQueryText(route.query.capacitySourceFocus)
  return CAPACITY_SOURCE_FOCUS_FIELD_KEYS[focus] ? focus : ''
}

const scrollProcessDetailField = async (fieldKey?: ProcessDetailFieldKey) => {
  await nextTick()
  if (!fieldKey) return
  const target = document.querySelector(`[data-flow-detail-field="${fieldKey}"]`)
  target?.scrollIntoView({ block: 'center', behavior: 'smooth' })
}

const scrollCapacitySourceFocusField = async () => {
  await scrollProcessDetailField(highlightedProcessDetailFieldKey.value)
}

const focusProcessDetailFieldsForCapacitySource = async () => {
  const focus = normalizeCapacitySourceFocus()
  if (!focus) {
    highlightedProcessDetailFieldKey.value = undefined
    return
  }
  const focusFieldKeys = CAPACITY_SOURCE_FOCUS_FIELD_KEYS[focus]
  const nextFieldKeys = normalizeProcessDetailFieldKeys([
    ...focusFieldKeys,
    ...selectedProcessDetailFieldKeys.value
  ])
  selectedProcessDetailFieldKeys.value = nextFieldKeys
  highlightedProcessDetailFieldKey.value = focusFieldKeys[0]
  syncSelectedProcessDetailFieldToAdd()
  await scrollCapacitySourceFocusField()
}

const resolveSavedProcessDetailFieldKeys = (
  columns?: UserTableColumnConfigColumnVO[] | null
): ProcessDetailFieldKey[] => {
  if (!columns) return mergeRequiredProcessDetailFieldKeys([...DEFAULT_PROCESS_DETAIL_FIELD_KEYS])
  return mergeRequiredProcessDetailFieldKeys(
    columns.filter((column) => column.visible !== false).map((column) => column.key)
  )
}

const buildProcessDetailFieldConfigColumns = (
  fieldKeys: ProcessDetailFieldKey[]
): UserTableColumnConfigColumnVO[] => {
  const selectedFieldKeys = mergeRequiredProcessDetailFieldKeys(fieldKeys)
  const selectedFieldKeySet = new Set(selectedFieldKeys)
  return [
    ...selectedFieldKeys.map((key) => ({ key, visible: true })),
    ...PROCESS_DETAIL_FIELD_KEYS.filter((key) => !selectedFieldKeySet.has(key)).map((key) => ({
      key,
      visible: false
    }))
  ]
}

const loadProcessDetailFieldConfig = async () => {
  processDetailInterestLoading.value = true
  try {
    const config = await getUserTableColumnConfig(PROCESS_DETAIL_FIELD_CONFIG_TABLE_KEY)
    selectedProcessDetailFieldKeys.value = resolveSavedProcessDetailFieldKeys(config?.columns)
    processDetailInterestReady.value = true
    processDetailInterestAvailable.value = true
    syncSelectedProcessDetailFieldToAdd()
    if (routeNodes.value.length > 0) {
      restoreRouteFlowReturnState()
    }
    await focusProcessDetailFieldsForCapacitySource()
  } catch (error) {
    processDetailInterestAvailable.value = false
    message.error(resolveErrorMessage(error, '关注列配置加载失败，请刷新后重试。'))
    throw error
  } finally {
    processDetailInterestLoading.value = false
  }
}

const saveProcessDetailFieldConfig = async (nextFieldKeys: ProcessDetailFieldKey[]) => {
  processDetailInterestSaving.value = true
  try {
    const selectedFieldKeys = mergeRequiredProcessDetailFieldKeys(nextFieldKeys)
    await saveUserTableColumnConfig({
      tableKey: PROCESS_DETAIL_FIELD_CONFIG_TABLE_KEY,
      columns: buildProcessDetailFieldConfigColumns(selectedFieldKeys)
    })
    selectedProcessDetailFieldKeys.value = selectedFieldKeys
    processDetailInterestReady.value = true
    processDetailInterestAvailable.value = true
    syncSelectedProcessDetailFieldToAdd()
  } catch (error) {
    processDetailInterestAvailable.value = false
    message.error(resolveErrorMessage(error, '关注列配置保存失败，请刷新后重试。'))
    throw error
  } finally {
    processDetailInterestSaving.value = false
  }
}

const processDetailInterestMutationDisabled = computed(
  () =>
    routeFlowWriteControlsDisabled.value ||
    processDetailInterestLoading.value ||
    processDetailInterestSaving.value ||
    !processDetailInterestReady.value ||
    !processDetailInterestAvailable.value
)

const recordBindingEditorDisabled = computed(
  () =>
    routeFlowWriteControlsDisabled.value ||
    !isDraftCandidateEdit.value ||
    selectedProcessAttributesLoading.value ||
    selectedProcessAttributesSaving.value
)

const resetSelectedProcessAttributes = () => {
  selectedProcessAttributes.routeProcessId = undefined
  selectedProcessAttributes.routeVersionId = undefined
  selectedProcessAttributes.routeScheduleConfigId = undefined
  selectedProcessAttributes.scheduleConfigVersion = undefined
  selectedProcessAttributes.capacityMode = undefined
  selectedProcessAttributes.productionQuantityFactor = DEFAULT_PRODUCTION_QUANTITY_FACTOR
  selectedProcessAttributes.hourlyCapacity = undefined
  selectedProcessAttributes.shiftHours = undefined
  selectedProcessAttributes.infiniteDurationQuantityFactor = undefined
  selectedProcessAttributes.infiniteDurationBaseMinutes = undefined
  selectedProcessAttributes.nightShiftEnabled = undefined
  selectedProcessAttributes.calendarRuleId = undefined
  selectedProcessAttributes.remark = undefined
  selectedRecordBindings.value = []
  selectedLegacyBatchRecords.value = []
}

const numericValue = (value?: number | string | null) => {
  if (value === undefined || value === null || value === '') return undefined
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : undefined
}

const positiveNumber = (value?: number | string | null) => {
  const numeric = numericValue(value)
  return numeric !== undefined && numeric > 0
}

const normalizeProductionQuantityFactor = (value?: number | string | null) => {
  const numeric = numericValue(value)
  return numeric === undefined ? DEFAULT_PRODUCTION_QUANTITY_FACTOR : Number(numeric.toFixed(2))
}

const isProductionQuantityFactorOverridden = (value?: number | string | null) =>
  Math.abs(normalizeProductionQuantityFactor(value) - DEFAULT_PRODUCTION_QUANTITY_FACTOR) >
  PRODUCTION_QUANTITY_FACTOR_OVERRIDE_TOLERANCE

const getSelectedProductionQuantityFactorCoverageStatus = (): ProcessDetailCoverageStatus =>
  isProductionQuantityFactorOverridden(selectedProcessAttributes.productionQuantityFactor)
    ? 'covered'
    : 'missing'

const normalizeShiftCapacity = (value?: number | string | null) => {
  const numeric = numericValue(value)
  return numeric === undefined ? undefined : Number(numeric.toFixed(6))
}

const normalizeHourlyCapacity = (value?: number | string | null) => {
  const numeric = numericValue(value)
  return numeric === undefined ? undefined : Number(numeric.toFixed(6))
}

const normalizeScheduleCapacityMode = (mode?: ProRouteScheduleConfigVO['capacityMode'] | null) =>
  mode === 'FINITE_HOURLY' ? 'MANUAL_OVERRIDE' : mode || 'RESOURCE_CALCULATED'

const findRouteProcessConfig = <T extends { routeProcessId?: number | null }>(
  rows: T[] | undefined,
  routeProcessId: number
) => (rows || []).find((item) => Number(item.routeProcessId) === Number(routeProcessId))

const isRouteNodeProductionQuantityFactorOverridden = (node: RouteFlowNodeVO) => {
  const routeProcessId = Number(node.routeProcessId)
  const draft = selectedProcessAttributeDrafts[routeProcessId]
  const scheduleRow = findRouteProcessConfig(
    selectedProcessRouteConfigCache.value?.scheduleConfigs,
    routeProcessId
  )
  return isProductionQuantityFactorOverridden(
    draft?.productionQuantityFactor ?? scheduleRow?.productionQuantityFactor
  )
}

const buildSelectedProcessRouteConfigCacheKey = () =>
  `${props.routeId}:${props.routeVersionEditContext?.routeVersionId || 'active'}`

const clearSelectedProcessRouteConfigCache = () => {
  selectedProcessRouteConfigCache.value = undefined
  selectedProcessRouteConfigCachePromise = undefined
}

const clearSelectedProcessAttributeDrafts = () => {
  Object.keys(selectedProcessAttributeDrafts).forEach((key) => {
    delete selectedProcessAttributeDrafts[Number(key)]
  })
  Object.keys(selectedProcessAttributeBaselines).forEach((key) => {
    delete selectedProcessAttributeBaselines[Number(key)]
  })
}

const clearRouteProcessKeyFlagBaselines = () => {
  Object.keys(routeProcessKeyFlagBaselines).forEach((key) => {
    delete routeProcessKeyFlagBaselines[Number(key)]
  })
}

const clearRouteProcessCheckFlagBaselines = () => {
  Object.keys(routeProcessCheckFlagBaselines).forEach((key) => {
    delete routeProcessCheckFlagBaselines[Number(key)]
  })
}

const clearRouteProcessWorkstationIdBaselines = () => {
  Object.keys(routeProcessWorkstationIdBaselines).forEach((key) => {
    delete routeProcessWorkstationIdBaselines[Number(key)]
  })
}

const cloneRecordBindings = (bindings: RouteFlowRecordBinding[]) =>
  bindings.map((binding) => ({
    ...binding,
    candidateSourceIds: normalizeRecordBindingCandidateIds(binding.candidateSourceIds),
    candidateSourceNames: normalizeRecordBindingCandidateNames(binding.candidateSourceNames),
    permissionRule: binding.permissionRule ? { ...binding.permissionRule } : binding.permissionRule
  }))

const cloneLegacyBatchRecords = (records: RouteFlowLegacyBatchRecord[] = []) =>
  records.map((record) => ({ ...record }))

const resequenceRecordBindings = (bindings: RouteFlowRecordBinding[]) =>
  cloneRecordBindings(bindings).map((binding, index) => ({
    ...binding,
    reportSort: index + 1
  }))

const resequenceLegacyBatchRecords = (records: RouteFlowLegacyBatchRecord[]) =>
  cloneLegacyBatchRecords(records).map((record, index) => ({
    ...record,
    reportSort: index + 1
  }))

const buildSelectedProcessAttributesDraftSnapshot = (draft: SelectedProcessAttributesDraft) => ({
  routeProcessId: draft.routeProcessId,
  routeVersionId: draft.routeVersionId ?? null,
  routeScheduleConfigId: draft.routeScheduleConfigId ?? null,
  capacityMode: draft.capacityMode ?? null,
  productionQuantityFactor: normalizeProductionQuantityFactor(draft.productionQuantityFactor),
  hourlyCapacity: normalizeHourlyCapacity(draft.hourlyCapacity) ?? null,
  shiftHours: numericValue(draft.shiftHours) ?? null,
  infiniteDurationQuantityFactor: numericValue(draft.infiniteDurationQuantityFactor) ?? null,
  infiniteDurationBaseMinutes: numericValue(draft.infiniteDurationBaseMinutes) ?? null,
  nightShiftEnabled: draft.nightShiftEnabled ?? false,
  calendarRuleId: draft.calendarRuleId ?? null,
  remark: draft.remark || null,
  legacyBatchRecords: resequenceLegacyBatchRecords(draft.legacyBatchRecords)
    .filter(isLegacyBatchRecordConfigured)
    .map((report) => ({
      ...report,
      formSlotType: requireBatchRecordFormSlotType(report),
      reportSort: report.reportSort || null,
      remark: report.remark || null
    })),
  recordBindings: resequenceRecordBindings(draft.recordBindings)
    .map((binding) => {
      const instanceScope = normalizeRecordBindingInstanceScope(binding.instanceScope)
      return {
        formBindingKey: binding.formBindingKey || createLocalFormBindingKey(),
        formSlotType: normalizeRecordBindingSlotType(binding.formSlotType, binding.formBindingKey),
        formTemplateId: binding.formTemplateId || null,
        formTemplateName: binding.formTemplateName || null,
        instanceScope,
        sharedFormKey: instanceScope === 'BATCH_SHARED' ? buildSharedRecordBindingKey(binding) : null,
        fillableScopeJson:
          instanceScope === 'BATCH_SHARED' ? SHARED_FORM_FILLABLE_SCOPE_JSON : null,
        recordbookEnabled: true,
        requiredPolicy: 'REQUIRED',
        permissionScopeId: binding.permissionScopeId ?? binding.permissionRule?.permissionScopeId ?? null,
        candidateSourceType: binding.candidateSourceType || null,
        candidateSourceIds: normalizeRecordBindingCandidateIds(binding.candidateSourceIds),
        candidateSourceNames: normalizeRecordBindingCandidateNames(binding.candidateSourceNames),
        reportSort: binding.reportSort || null,
        remark: binding.remark || null
      }
    })
})

const serializeSelectedProcessAttributesDraft = (draft: SelectedProcessAttributesDraft) =>
  JSON.stringify(buildSelectedProcessAttributesDraftSnapshot(draft))

const serializeSelectedProcessScheduleDraft = (draft: SelectedProcessAttributesDraft) => {
  const {
    recordBindings: _recordBindings,
    legacyBatchRecords: _legacyBatchRecords,
    ...scheduleSnapshot
  } = buildSelectedProcessAttributesDraftSnapshot(draft)
  return JSON.stringify(scheduleSnapshot)
}

const serializeSelectedProcessRecordBindingDraft = (draft: SelectedProcessAttributesDraft) => {
  const snapshot = buildSelectedProcessAttributesDraftSnapshot(draft)
  return JSON.stringify({
    legacyBatchRecords: snapshot.legacyBatchRecords,
    recordBindings: snapshot.recordBindings
  })
}

const parseSelectedProcessAttributeBaseline = (baseline?: string) => {
  if (!baseline) return undefined
  try {
    return JSON.parse(baseline) as ReturnType<typeof buildSelectedProcessAttributesDraftSnapshot>
  } catch (_error) {
    return undefined
  }
}

const hasSelectedProcessScheduleDraftChanged = (draft: SelectedProcessAttributesDraft) => {
  const baselineSnapshot = parseSelectedProcessAttributeBaseline(
    selectedProcessAttributeBaselines[draft.routeProcessId]
  )
  if (!baselineSnapshot) return true
  const {
    recordBindings: _recordBindings,
    legacyBatchRecords: _legacyBatchRecords,
    ...baselineScheduleSnapshot
  } = baselineSnapshot
  return JSON.stringify(baselineScheduleSnapshot) !== serializeSelectedProcessScheduleDraft(draft)
}

const hasSelectedScheduleCapacityDraftChanges = hasSelectedProcessScheduleDraftChanged

const hasSelectedProcessRecordBindingDraftChanged = (draft: SelectedProcessAttributesDraft) => {
  const baselineSnapshot = parseSelectedProcessAttributeBaseline(
    selectedProcessAttributeBaselines[draft.routeProcessId]
  )
  if (!baselineSnapshot) return true
  const baselineRecordBindingSnapshot = {
    legacyBatchRecords: baselineSnapshot.legacyBatchRecords || [],
    recordBindings: baselineSnapshot.recordBindings || []
  }
  return (
    JSON.stringify(baselineRecordBindingSnapshot) !==
    serializeSelectedProcessRecordBindingDraft(draft)
  )
}

const cloneSelectedProcessAttributesDraft = (
  draft: SelectedProcessAttributesDraft
): SelectedProcessAttributesDraft => ({
  ...draft,
  recordBindings: cloneRecordBindings(draft.recordBindings),
  legacyBatchRecords: cloneLegacyBatchRecords(draft.legacyBatchRecords)
})

const applySelectedProcessAttributesDraft = (draft: SelectedProcessAttributesDraft) => {
  selectedProcessAttributes.routeProcessId = draft.routeProcessId
  selectedProcessAttributes.routeVersionId = draft.routeVersionId
  selectedProcessAttributes.routeScheduleConfigId = draft.routeScheduleConfigId
  selectedProcessAttributes.scheduleConfigVersion = draft.scheduleConfigVersion
  selectedProcessAttributes.capacityMode = draft.capacityMode
  selectedProcessAttributes.productionQuantityFactor = normalizeProductionQuantityFactor(
    draft.productionQuantityFactor
  )
  selectedProcessAttributes.hourlyCapacity = normalizeHourlyCapacity(draft.hourlyCapacity)
  selectedProcessAttributes.shiftHours = numericValue(draft.shiftHours)
  selectedProcessAttributes.infiniteDurationQuantityFactor = numericValue(
    draft.infiniteDurationQuantityFactor
  )
  selectedProcessAttributes.infiniteDurationBaseMinutes = numericValue(
    draft.infiniteDurationBaseMinutes
  )
  selectedProcessAttributes.nightShiftEnabled = draft.nightShiftEnabled ?? false
  selectedProcessAttributes.calendarRuleId = draft.calendarRuleId ?? null
  selectedProcessAttributes.remark = draft.remark ?? null
  selectedRecordBindings.value = cloneRecordBindings(draft.recordBindings)
  selectedLegacyBatchRecords.value = cloneLegacyBatchRecords(draft.legacyBatchRecords)
}

const buildSelectedProcessAttributesDraft = (
  node: RouteFlowNodeVO,
  routeVersionId: number,
  scheduleRow?: ProRouteFlowProcessConfigVO,
  routeScheduleConfig?: ProRouteScheduleConfigVO,
  routeProcess?: ProRouteProcessVO,
  batchRow?: ProRouteFlowProcessConfigVO
): SelectedProcessAttributesDraft => ({
  routeProcessId: node.routeProcessId,
  routeVersionId,
  routeScheduleConfigId: routeScheduleConfig?.id ?? null,
  scheduleConfigVersion: routeScheduleConfig?.configVersion ?? null,
  capacityMode: normalizeScheduleCapacityMode(routeScheduleConfig?.capacityMode),
  productionQuantityFactor: normalizeProductionQuantityFactor(scheduleRow?.productionQuantityFactor),
  hourlyCapacity: normalizeHourlyCapacity(routeScheduleConfig?.hourlyCapacity),
  shiftHours: numericValue(routeScheduleConfig?.shiftHours ?? routeProcess?.shiftHours),
  infiniteDurationQuantityFactor: numericValue(routeScheduleConfig?.infiniteDurationQuantityFactor),
  infiniteDurationBaseMinutes: numericValue(routeScheduleConfig?.infiniteDurationBaseMinutes),
  nightShiftEnabled: routeScheduleConfig?.nightShiftEnabled ?? false,
  calendarRuleId: routeScheduleConfig?.calendarRuleId ?? null,
  remark: routeScheduleConfig?.remark ?? scheduleRow?.remark ?? null,
  recordBindings: buildRecordBindings(batchRow),
  legacyBatchRecords: buildLegacyBatchRecords(batchRow?.batchRecordReports)
})

const getOrCreateRouteProcessAttributeDraft = (
  routeProcessId: number
): SelectedProcessAttributesDraft => {
  const existingDraft = selectedProcessAttributeDrafts[routeProcessId]
  if (existingDraft) return existingDraft
  const node = routeNodes.value.find((item) => Number(item.routeProcessId) === Number(routeProcessId))
  if (!node) {
    throw new Error(`工序独立同步失败：路线工序不存在 routeProcessId=${routeProcessId}`)
  }
  const configCache = selectedProcessRouteConfigCache.value
  if (!configCache) {
    throw new Error('工序独立同步失败：工序配置仍在加载，请稍后重试。')
  }
  const routeProcess = routeProcessRows.value.find((row) => Number(row.id) === Number(routeProcessId))
  const serverDraft = buildSelectedProcessAttributesDraft(
    node,
    configCache.readableRouteVersionId,
    findRouteProcessConfig(configCache.scheduleConfigs, routeProcessId),
    findRouteProcessConfig(configCache.routeScheduleConfigs, routeProcessId),
    routeProcess,
    findRouteProcessConfig(configCache.batchConfigs, routeProcessId)
  )
  selectedProcessAttributeDrafts[routeProcessId] = cloneSelectedProcessAttributesDraft(serverDraft)
  selectedProcessAttributeBaselines[routeProcessId] =
    serializeSelectedProcessAttributesDraft(serverDraft)
  return selectedProcessAttributeDrafts[routeProcessId]
}

const loadSelectedProcessRouteConfigCache = async (
  key: string
): Promise<SelectedProcessRouteConfigCache> => {
  const [routeInfo, scheduleConfigs, batchConfigs] = await Promise.all([
    ProRouteApi.getRoute(props.routeId),
    ProRouteFlowConfigApi.getProcessConfigList(
      props.routeId,
      'SCHEDULE',
      props.routeVersionEditContext?.routeVersionId
    ),
    ProRouteFlowConfigApi.getProcessConfigList(
      props.routeId,
      'BATCH',
      props.routeVersionEditContext?.routeVersionId
    )
  ])
  const readableRouteVersionId = resolveRouteVersionIdForRead(routeInfo.activeRouteVersionId)
  if (!readableRouteVersionId) {
    throw new Error('加载工序属性失败：当前路线缺少激活版本。')
  }
  const routeScheduleConfigs =
    await ProRouteApi.getScheduleConfigListByRouteVersion(readableRouteVersionId)
  const cache = {
    key,
    routeInfo,
    readableRouteVersionId,
    scheduleConfigs,
    batchConfigs,
    routeScheduleConfigs
  }
  selectedProcessRouteConfigCache.value = cache
  return cache
}

const ensureSelectedProcessRouteConfigCache = async () => {
  const key = buildSelectedProcessRouteConfigCacheKey()
  if (selectedProcessRouteConfigCache.value?.key === key) {
    return selectedProcessRouteConfigCache.value
  }
  if (selectedProcessRouteConfigCachePromise?.key === key) {
    return await selectedProcessRouteConfigCachePromise.promise
  }
  const promise = loadSelectedProcessRouteConfigCache(key).finally(() => {
    if (selectedProcessRouteConfigCachePromise?.key === key) {
      selectedProcessRouteConfigCachePromise = undefined
    }
  })
  selectedProcessRouteConfigCachePromise = { key, promise }
  return await promise
}

const loadSelectedProcessAttributes = async (node: RouteFlowNodeVO, requestId: number) => {
  selectedProcessAttributesLoading.value = true
  resetSelectedProcessAttributes()
  try {
    const configCache = await ensureSelectedProcessRouteConfigCache()
    if (!isSelectedProcessDetailRequestCurrent(requestId, node.routeProcessId)) return
    const scheduleRow = findRouteProcessConfig(configCache.scheduleConfigs, node.routeProcessId)
    const routeScheduleConfig = findRouteProcessConfig(
      configCache.routeScheduleConfigs,
      node.routeProcessId
    )
    const batchRow = findRouteProcessConfig(configCache.batchConfigs, node.routeProcessId)
    const routeProcessId = node.routeProcessId
    if (!selectedProcessAttributeDrafts[routeProcessId]) {
      const routeProcess = routeProcessRows.value.find(
        (row) => Number(row.id) === Number(node.routeProcessId)
      )
      const serverDraft = buildSelectedProcessAttributesDraft(
        node,
        configCache.readableRouteVersionId,
        scheduleRow,
        routeScheduleConfig,
        routeProcess,
        batchRow
      )
      selectedProcessAttributeDrafts[routeProcessId] = cloneSelectedProcessAttributesDraft(serverDraft)
      selectedProcessAttributeBaselines[routeProcessId] =
        serializeSelectedProcessAttributesDraft(serverDraft)
    }
    applySelectedProcessAttributesDraft(selectedProcessAttributeDrafts[routeProcessId])
  } catch (error) {
    if (!isSelectedProcessDetailRequestCurrent(requestId, node.routeProcessId)) return
    message.error(resolveErrorMessage(error, '加载工序属性失败'))
  } finally {
    if (isSelectedProcessDetailRequestCurrent(requestId, node.routeProcessId)) {
      selectedProcessAttributesLoading.value = false
      void tryOpenCapacityOverrideFromRouteQuery()
    }
  }
}

const buildFormBindingSaveRows = (
  bindings: RouteFlowRecordBinding[]
): ProRouteFlowFormBindingSaveVO[] => {
  const rows = resequenceRecordBindings(bindings)
    .filter((binding) => Boolean(binding.formTemplateId))
    .map((binding, index): ProRouteFlowFormBindingSaveVO => {
      validateBatchSharedRecordBinding(binding)
      validateRecordBindingCandidateSource(binding)
      const instanceScope = normalizeRecordBindingInstanceScope(binding.instanceScope)
      return {
        formBindingKey: binding.formBindingKey || createLocalFormBindingKey(),
        formSlotType: normalizeRecordBindingSlotType(binding.formSlotType, binding.formBindingKey),
        formTemplateId: Number(binding.formTemplateId),
        formTemplateName: binding.formTemplateName || null,
        instanceScope: instanceScope,
        sharedFormKey: instanceScope === 'BATCH_SHARED' ? buildSharedRecordBindingKey(binding) : null,
        fillableScopeJson:
          instanceScope === 'BATCH_SHARED' ? SHARED_FORM_FILLABLE_SCOPE_JSON : null,
        recordbookEnabled: true,
        requiredPolicy: 'REQUIRED',
        permissionScopeId: binding.permissionScopeId ?? binding.permissionRule?.permissionScopeId ?? null,
        candidateSourceType: binding.candidateSourceType,
        candidateSourceIds: binding.candidateSourceIds,
        candidateSourceNames: binding.candidateSourceNames,
        reportSort: index + 1,
        remark: binding.remark || null
      }
    })
  validateDuplicateFormTemplate(rows)
  return rows
}

const buildLegacyBatchRecordSaveRows = (
  records: RouteFlowLegacyBatchRecord[]
): ProRouteFlowBatchRecordVO[] =>
  resequenceLegacyBatchRecords(records)
    .filter(isLegacyBatchRecordConfigured)
    .map((report, index) => ({
      ...report,
      batchRecordReportId: report.batchRecordReportId,
      formSlotType: requireBatchRecordFormSlotType(report),
      reportSort: index + 1,
      remark: report.remark || null
    }))

const buildSelectedProcessConfigSaveRow = (
  draft: SelectedProcessAttributesDraft
): ProRouteFlowProcessConfigSaveVO => {
  if (!draft.routeProcessId) {
    throw new Error('保存工序属性失败：缺少目标路线工序。')
  }
  const productionQuantityFactor = normalizeProductionQuantityFactor(draft.productionQuantityFactor)
  if (!positiveNumber(productionQuantityFactor)) {
    throw new Error('保存工序属性失败：生产系数必须大于 0。')
  }
  return {
    routeProcessId: draft.routeProcessId,
    enabled: true,
    productionQuantityFactor,
    batchRecordReports: buildLegacyBatchRecordSaveRows(draft.legacyBatchRecords),
    formBindings: buildFormBindingSaveRows(draft.recordBindings),
    remark: draft.remark || null
  }
}

const buildSelectedProcessRecordBindingConfigSaveRow = (
  draft: SelectedProcessAttributesDraft
): ProRouteFlowProcessConfigSaveVO => {
  if (!draft.routeProcessId) {
    throw new Error('保存工序属性失败：缺少目标路线工序。')
  }
  return {
    routeProcessId: draft.routeProcessId,
    enabled: true,
    batchRecordReports: buildLegacyBatchRecordSaveRows(draft.legacyBatchRecords),
    formBindings: buildFormBindingSaveRows(draft.recordBindings),
    remark: draft.remark || null
  }
}

const resolveSelectedHourlyCapacity = (draft: SelectedProcessAttributesDraft) => {
  const hourlyCapacity = normalizeHourlyCapacity(draft.hourlyCapacity)
  if (hourlyCapacity === undefined || hourlyCapacity <= 0) {
    throw new Error('保存工序属性失败：产能覆盖必须大于 0。')
  }
  return hourlyCapacity
}

const saveSelectedScheduleCapacity = async (
  draft: SelectedProcessAttributesDraft,
  options: Record<string, unknown> = {}
) => {
  const editingRouteVersionId = requireCandidateRouteVersionId('工序属性保存')
  if (!draft.routeProcessId) {
    throw new Error('保存班次产能失败：缺少目标路线工序。')
  }
  const capacityMode = normalizeScheduleCapacityMode(draft.capacityMode)
  const payload: ProRouteScheduleConfigVO = {
    id: draft.routeScheduleConfigId ?? undefined,
    routeVersionId: editingRouteVersionId,
    routeProcessId: draft.routeProcessId,
    capacityMode,
    nightShiftEnabled: draft.nightShiftEnabled ?? false,
    calendarRuleId: draft.calendarRuleId ?? undefined,
    remark: draft.remark || undefined
  }
  if (capacityMode === 'MANUAL_OVERRIDE') {
    payload.hourlyCapacity = resolveSelectedHourlyCapacity(draft)
  } else if (capacityMode === 'INFINITE_FORMULA') {
    if (!positiveNumber(draft.infiniteDurationQuantityFactor)) {
      throw new Error('保存工序属性失败：无限公式数量系数必须大于 0。')
    }
    if (draft.infiniteDurationBaseMinutes === undefined || draft.infiniteDurationBaseMinutes < 0) {
      throw new Error('保存工序属性失败：无限公式基础分钟不能小于 0。')
    }
    payload.infiniteDurationQuantityFactor = draft.infiniteDurationQuantityFactor
    payload.infiniteDurationBaseMinutes = draft.infiniteDurationBaseMinutes
  }
  await ProRouteApi.saveScheduleConfig(payload, options)
}

const buildCapacityOverrideCandidateRouteQuery = (candidate: ProRouteVersionVO) => {
  const routeProcessId = selectedProcessAttributes.routeProcessId || selectedRouteProcessId.value
  return buildRouteCandidateEditQuery(candidate, {
    ...route.query,
    tab: 'flow',
    routeProcessId: routeProcessId ? String(routeProcessId) : undefined,
    capacityOverride: CAPACITY_OVERRIDE_AUTO_OPEN_QUERY_VALUE
  })
}

const createDraftCandidateForCapacityOverride = async () => {
  const candidateResult = await ensureSameSourceDraftCandidateForProductionConfig({
    routeId: props.routeId,
    actionName: '产能覆盖打开',
    changeReason: '产能覆盖创建候选版本',
    confirm: (content, title) => message.confirm(content, title),
    success: (content) => message.success(content),
    existingConfirmMessage:
      '产能覆盖需要在路线候选版本中编辑。确认后会进入已有候选版本，不会直接影响当前生产版本。是否继续？',
    existingConfirmTitle: '进入候选版本',
    createConfirmMessage:
      '产能覆盖需要先创建路线候选版本。确认后会创建候选版本并进入编辑，不会直接影响当前生产版本。是否继续？',
    createConfirmTitle: '创建候选版本',
    existingSuccessMessage: '正在进入候选版本产能覆盖编辑',
    createdSuccessMessage: '候选版本已创建，正在进入产能覆盖编辑'
  })
  if (!candidateResult) return undefined
  const candidate = candidateResult.candidate
  await router.push({
    name: 'MesProRouteEdit',
    params: { id: props.routeId },
    query: buildCapacityOverrideCandidateRouteQuery(candidate)
  })
  return candidate
}

const ensureCapacityOverrideCandidateContext = async () => {
  if (isDraftCandidateEdit.value) return true
  capacityOverrideCandidateCreating.value = true
  try {
    await createDraftCandidateForCapacityOverride()
    return false
  } finally {
    capacityOverrideCandidateCreating.value = false
  }
}

const openCapacityOverrideDialogForDraft = async () => {
  if (!canMutateRouteFlow.value) return
  try {
    if (selectedProcessAttributesLoading.value) {
      throw new Error('产能覆盖打开失败：工序属性仍在加载，请稍后重试。')
    }
    if (!selectedProcessAttributes.routeProcessId) {
      throw new Error('产能覆盖打开失败：缺少目标路线工序。')
    }
    capacityOverrideRouteVersionId.value = requireCandidateRouteVersionId('产能覆盖打开')
    capacityOverrideRouteProcessId.value = selectedProcessAttributes.routeProcessId
    capacityOverrideForm.hourlyCapacity =
      normalizeScheduleCapacityMode(selectedProcessAttributes.capacityMode) === 'MANUAL_OVERRIDE'
        ? normalizeHourlyCapacity(selectedProcessAttributes.hourlyCapacity)
        : undefined
    capacityOverrideDialogVisible.value = true
    await nextTick()
    capacityOverrideFormRef.value?.clearValidate?.()
  } catch (error) {
    message.error(resolveErrorMessage(error, '产能覆盖处理失败'))
  }
}

const openCapacityOverrideDialog = async () => {
  if (!canMutateRouteFlow.value) return
  try {
    const candidateReady = await ensureCapacityOverrideCandidateContext()
    if (!candidateReady) return
    await openCapacityOverrideDialogForDraft()
  } catch (error) {
    message.error(resolveErrorMessage(error, '产能覆盖处理失败'))
  }
}

const clearCapacityOverrideAutoOpenQuery = async () => {
  if (normalizeRouteQueryText(route.query.capacityOverride) !== CAPACITY_OVERRIDE_AUTO_OPEN_QUERY_VALUE) {
    return
  }
  const nextQuery: Record<string, string | string[] | undefined> = { ...route.query }
  delete nextQuery.capacityOverride
  await router.replace({ query: nextQuery })
}

const closeCapacityOverrideDialog = async () => {
  capacityOverrideDialogVisible.value = false
  await clearCapacityOverrideAutoOpenQuery()
}

const tryOpenCapacityOverrideFromRouteQuery = async () => {
  if (capacityOverrideAutoOpening) return
  if (capacityOverrideDialogVisible.value) return
  if (normalizeRouteQueryText(route.query.capacityOverride) !== CAPACITY_OVERRIDE_AUTO_OPEN_QUERY_VALUE) {
    return
  }
  if (!isDraftCandidateEdit.value || selectedProcessAttributesLoading.value) return
  if (!selectedProcessAttributes.routeProcessId) return
  capacityOverrideAutoOpening = true
  try {
    await openCapacityOverrideDialogForDraft()
  } finally {
    capacityOverrideAutoOpening = false
  }
}

const syncCapacityOverrideDraftBaseline = async (routeVersionId: number, routeProcessId: number) => {
  const routeScheduleConfigs = await ProRouteApi.getScheduleConfigListByRouteVersion(routeVersionId)
  const routeScheduleConfig = findRouteProcessConfig(routeScheduleConfigs, routeProcessId)
  if (!routeScheduleConfig) {
    throw new Error('产能覆盖保存后回读失败：未找到路线工序排产配置。')
  }
  const draft = selectedProcessAttributeDrafts[routeProcessId]
  if (!draft) {
    throw new Error('产能覆盖保存后同步失败：未找到当前工序属性草稿。')
  }
  draft.routeScheduleConfigId = routeScheduleConfig.id ?? null
  draft.scheduleConfigVersion = routeScheduleConfig.configVersion ?? null
  draft.capacityMode = normalizeScheduleCapacityMode(routeScheduleConfig.capacityMode)
  draft.hourlyCapacity = normalizeHourlyCapacity(routeScheduleConfig.hourlyCapacity)
  draft.shiftHours = numericValue(routeScheduleConfig.shiftHours ?? draft.shiftHours)
  draft.nightShiftEnabled = routeScheduleConfig.nightShiftEnabled ?? false
  draft.calendarRuleId = routeScheduleConfig.calendarRuleId ?? null
  draft.remark = routeScheduleConfig.remark ?? draft.remark ?? null
  selectedProcessAttributeBaselines[routeProcessId] = serializeSelectedProcessAttributesDraft(draft)
  if (selectedProcessAttributes.routeProcessId === routeProcessId) {
    applySelectedProcessAttributesDraft(draft)
  }
}

const resetCapacityWorkstationRepairForm = () => {
  capacityWorkstationRepairForm.sourceRouteProcessId = undefined
  capacityWorkstationRepairForm.workshopId = undefined
}

const loadCapacityWorkstationRepairWorkshops = async () => {
  if (capacityWorkstationRepairWorkshopOptions.value.length > 0) return
  capacityWorkstationRepairWorkshopLoading.value = true
  try {
    capacityWorkstationRepairWorkshopOptions.value = await MdWorkshopApi.getWorkshopSimpleList()
  } finally {
    capacityWorkstationRepairWorkshopLoading.value = false
  }
}

const loadCapacityWorkstationRepairWorkstations = async () => {
  if (capacityWorkstationRepairWorkstationOptions.value.length > 0) return
  capacityWorkstationRepairWorkstationLoading.value = true
  try {
    const workstations: MdWorkstationVO[] = []
    let pageNo = 1
    let total = 0
    do {
      const pageResult = await MdWorkstationApi.getWorkstationPage({
        pageNo,
        pageSize: CAPACITY_WORKSTATION_REPAIR_WORKSTATION_PAGE_SIZE,
        status: CommonStatusEnum.ENABLE
      })
      const pageList = Array.isArray(pageResult?.list) ? pageResult.list : []
      workstations.push(...pageList)
      total = Number(pageResult?.total ?? workstations.length)
      pageNo += 1
    } while (workstations.length < total)
    capacityWorkstationRepairWorkstationOptions.value = workstations
  } finally {
    capacityWorkstationRepairWorkstationLoading.value = false
  }
}

const loadCapacityWorkstationRepairShiftHoursSetting = async (force = false) => {
  if (!force && capacityWorkstationRepairShiftHoursSetting.value) return
  if (force) {
    capacityWorkstationRepairShiftHoursSetting.value = undefined
  }
  capacityWorkstationRepairShiftHoursLoading.value = true
  try {
    capacityWorkstationRepairShiftHoursSetting.value =
      await SchedulerWorkbenchApi.getShiftHoursSetting()
  } finally {
    capacityWorkstationRepairShiftHoursLoading.value = false
  }
}

const openCapacityWorkstationRepairDialog = async () => {
  if (!canMutateRouteFlow.value) return
  resetCapacityWorkstationRepairForm()
  await loadCapacityWorkstationRepairWorkstations()
  capacityWorkstationRepairMode.value = boundRouteProcessOptions.value.length > 0 ? 'reuse' : 'create'
  capacityWorkstationRepairDialogVisible.value = true
  if (capacityWorkstationRepairMode.value === 'create') {
    await Promise.all([
      loadCapacityWorkstationRepairWorkshops(),
      loadCapacityWorkstationRepairShiftHoursSetting(true)
    ])
  }
  await nextTick()
  capacityWorkstationRepairFormRef.value?.clearValidate?.()
}

const buildCapacityWorkstationName = (
  targetRouteProcess: ProRouteProcessVO,
  workstationCode: string
) => {
  const processName = targetRouteProcess.processName || targetRouteProcess.processCode || '路线工序'
  return `${processName}-工作站-${workstationCode}`
}

const applyCapacityWorkstationBindingLocally = (
  targetRouteProcess: ProRouteProcessVO,
  binding: CapacityWorkstationRepairBinding
) => {
  const routeProcessId = Number(targetRouteProcess.id)
  routeNodes.value = routeNodes.value.map((node) =>
    Number(node.routeProcessId) === routeProcessId
      ? {
          ...node,
          routeProcessWorkstationId: binding.workstationId,
          workstationId: binding.workstationId,
          workstationCode: binding.workstationCode ?? node.workstationCode,
          workstationName: binding.workstationName ?? node.workstationName
        }
      : node
  )

  let rowUpdated = false
  routeProcessRows.value = routeProcessRows.value.map((row) => {
    if (Number(row.id) !== routeProcessId) return row
    rowUpdated = true
    return {
      ...row,
      routeProcessWorkstationId: binding.workstationId,
      workstationId: binding.workstationId,
      workstationCode: binding.workstationCode ?? row.workstationCode,
      workstationName: binding.workstationName ?? row.workstationName,
      shiftHours: binding.shiftHours ?? row.shiftHours,
      processShiftCapacityTotal: binding.shiftCapacity ?? row.processShiftCapacityTotal
    }
  })
  if (!rowUpdated) {
    const node = findNode(routeProcessId)
    if (node) {
      routeProcessRows.value.push({
        ...buildRouteProcessRowFromNode(node),
        routeProcessWorkstationId: binding.workstationId,
        workstationId: binding.workstationId,
        workstationCode: binding.workstationCode ?? node.workstationCode,
        workstationName: binding.workstationName ?? node.workstationName,
        shiftHours: binding.shiftHours,
        processShiftCapacityTotal: binding.shiftCapacity
      })
    }
  }
}

const clearSelectedProcessAttributeDraftForRouteProcess = (routeProcessId: number) => {
  delete selectedProcessAttributeDrafts[routeProcessId]
  delete selectedProcessAttributeBaselines[routeProcessId]
}

const markRouteProcessGraphSaveClean = () => {
  graphDirty.value = false
  pendingDeletedRouteProcessIds.value = new Set()
  nextDraftRouteProcessId.value = -1
  resetRouteProcessKeyFlagBaselines()
  resetRouteProcessCheckFlagBaselines()
  resetRouteProcessWorkstationIdBaselines()
}

const bindCapacityWorkstationToRouteProcess = async (
  targetRouteProcess: ProRouteProcessVO,
  binding: CapacityWorkstationRepairBinding
) => {
  requireCandidateRouteVersionId('工作站绑定')
  if (!targetRouteProcess.id) {
    throw new Error('工作站绑定失败：缺少目标路线工序编号。')
  }
  if (!targetRouteProcess.processId) {
    throw new Error('工作站绑定失败：缺少目标工序编号。')
  }
  applyCapacityWorkstationBindingLocally(targetRouteProcess, binding)
  syncFlowElements()
  markGraphDraftChanged()
  const result = await persistRouteProcessDraftChanges()
  applyValidation(result)
  if (!result.valid) {
    throw new Error(buildValidationErrorMessage(result))
  }
  markRouteProcessGraphSaveClean()
}

const resolveCapacityWorkstationRepairShiftHoursForCreate = () => {
  const shiftHours = capacityWorkstationRepairShiftHours.value
  if (shiftHours === undefined || shiftHours <= 0) {
    throw new Error('新建工作站失败：请先在排产员工作台统一保存班次小时。')
  }
  return shiftHours
}

const createCapacityWorkstationForRouteProcess = async (
  targetRouteProcess: ProRouteProcessVO
): Promise<CapacityWorkstationRepairBinding> => {
  if (!targetRouteProcess.processId) {
    throw new Error('新建工作站失败：缺少目标工序编号。')
  }
  const workshopId = capacityWorkstationRepairForm.workshopId
  if (!workshopId) {
    throw new Error('新建工作站失败：请选择车间。')
  }
  const shiftHours = resolveCapacityWorkstationRepairShiftHoursForCreate()
  const code = await AutoCodeRecordApi.generateAutoCode(MesAutoCodeRuleCode.MD_WORKSTATION_CODE)
  const name = buildCapacityWorkstationName(targetRouteProcess, code)
  const workstationId = await MdWorkstationApi.createWorkstation({
    code,
    name,
    workshopId,
    processId: targetRouteProcess.processId,
    shiftHours,
    status: CommonStatusEnum.ENABLE
  } as MdWorkstationVO)
  const parsedWorkstationId = Number(workstationId)
  if (!Number.isFinite(parsedWorkstationId) || parsedWorkstationId <= 0) {
    throw new Error('新建工作站失败：工作站创建接口未返回有效编号。')
  }
  return {
    workstationId: parsedWorkstationId,
    workstationCode: code,
    workstationName: name,
    shiftHours
  }
}

const refreshCapacityWorkstationRepairBinding = async (routeProcessId: number) => {
  selectedRouteProcessId.value = routeProcessId
  const targetNode = findNode(routeProcessId)
  if (!targetNode) {
    throw new Error('工作站绑定后刷新失败：未找到目标路线工序节点。')
  }
  clearSelectedProcessRouteConfigCache()
  clearSelectedProcessAttributeDraftForRouteProcess(routeProcessId)
  await loadSelectedProcessDetail(targetNode)
}

const submitCapacityWorkstationRepair = async () => {
  if (!canMutateRouteFlow.value) return
  capacityWorkstationRepairSaving.value = true
  const previousHourlyCapacity =
    capacityOverrideRepairHourlyCapacity.value ?? capacityOverrideForm.hourlyCapacity
  try {
    await capacityWorkstationRepairFormRef.value?.validate?.()
    const targetRouteProcess = capacityWorkstationRepairTargetRouteProcess.value
    if (!targetRouteProcess) {
      throw new Error('工作站绑定失败：未找到目标路线工序。')
    }
    if (capacityWorkstationRepairMode.value === 'reuse') {
      const sourceOption = boundRouteProcessOptions.value.find(
        (row) => row.value === capacityWorkstationRepairForm.sourceRouteProcessId
      )
      if (!sourceOption?.workstationId) {
        throw new Error('工作站绑定失败：请选择已绑定工作站的工序。')
      }
      await bindCapacityWorkstationToRouteProcess(
        targetRouteProcess,
        sourceOption
      )
    } else {
      const workstationBinding = await createCapacityWorkstationForRouteProcess(targetRouteProcess)
      await bindCapacityWorkstationToRouteProcess(targetRouteProcess, workstationBinding)
    }
    const routeProcessId = Number(targetRouteProcess.id)
    await refreshCapacityWorkstationRepairBinding(routeProcessId)
    capacityWorkstationRepairDialogVisible.value = false
    await openCapacityOverrideDialogForDraft()
    if (normalizeHourlyCapacity(previousHourlyCapacity) !== undefined) {
      capacityOverrideForm.hourlyCapacity = previousHourlyCapacity
    }
    capacityOverrideRepairHourlyCapacity.value = undefined
    message.success('工作站已绑定，可以继续设定产能')
  } catch (error) {
    message.error(resolveErrorMessage(error, '工作站绑定失败'))
  } finally {
    capacityWorkstationRepairSaving.value = false
  }
}

const submitCapacityOverride = async () => {
  if (!canMutateRouteFlow.value) return
  capacityOverrideSaving.value = true
  try {
    await capacityOverrideFormRef.value?.validate?.()
    const routeVersionId = capacityOverrideRouteVersionId.value
    if (!routeVersionId) {
      throw new Error('产能覆盖保存失败：缺少候选版本上下文，请重新打开产能覆盖弹框。')
    }
    const routeProcessId = capacityOverrideRouteProcessId.value || selectedProcessAttributes.routeProcessId
    if (!routeProcessId) {
      throw new Error('产能覆盖保存失败：缺少目标路线工序。')
    }
    const targetRouteProcess = capacityWorkstationRepairTargetRouteProcess.value
    const routeProcessWorkstationId = getBoundRouteProcessWorkstationId(targetRouteProcess)
    const shiftHours = numericValue(selectedProcessAttributes.shiftHours)
    if (
      routeProcessWorkstationId === undefined ||
      routeProcessWorkstationId <= 0 ||
      shiftHours === undefined ||
      shiftHours <= 0
    ) {
      capacityOverrideRepairHourlyCapacity.value = normalizeHourlyCapacity(
        capacityOverrideForm.hourlyCapacity
      )
      capacityOverrideDialogVisible.value = false
      await nextTick()
      await openCapacityWorkstationRepairDialog()
      return
    }
    const hourlyCapacity = normalizeHourlyCapacity(capacityOverrideForm.hourlyCapacity)
    if (hourlyCapacity === undefined || hourlyCapacity <= 0) {
      throw new Error('产能覆盖保存失败：产能覆盖必须大于 0。')
    }
    const capacityModeToSave = isRouteProcessCapacityOverrideDifferentFromDefault(
      hourlyCapacity,
      shiftHours,
      selectedRouteProcess.value?.processShiftCapacityTotal
    )
      ? 'MANUAL_OVERRIDE'
      : 'RESOURCE_CALCULATED'
    const payload: ProRouteScheduleConfigVO = {
      id: selectedProcessAttributes.routeScheduleConfigId ?? undefined,
      routeVersionId,
      routeProcessId,
      capacityMode: capacityModeToSave,
      nightShiftEnabled: selectedProcessAttributes.nightShiftEnabled ?? false,
      calendarRuleId: selectedProcessAttributes.calendarRuleId ?? undefined,
      remark: selectedProcessAttributes.remark || undefined
    }
    if (capacityModeToSave === 'MANUAL_OVERRIDE') {
      payload.hourlyCapacity = hourlyCapacity
    }
    await ProRouteApi.saveScheduleConfig(payload)
    await syncCapacityOverrideDraftBaseline(routeVersionId, routeProcessId)
    await closeCapacityOverrideDialog()
    message.success('产能覆盖已保存')
  } catch (error) {
    message.error(resolveErrorMessage(error, '产能覆盖保存失败'))
  } finally {
    capacityOverrideSaving.value = false
  }
}

watch(capacityOverrideDialogVisible, (visible) => {
  if (visible) return
  capacityOverrideRouteVersionId.value = null
  capacityOverrideRouteProcessId.value = null
})

watch(capacityWorkstationRepairMode, async (mode) => {
  if (mode === 'create') {
    try {
      await Promise.all([
        loadCapacityWorkstationRepairWorkshops(),
        loadCapacityWorkstationRepairShiftHoursSetting(true)
      ])
    } catch (error) {
      message.error(resolveErrorMessage(error, '班次小时读取失败'))
    }
  }
  await nextTick()
  capacityWorkstationRepairFormRef.value?.clearValidate?.()
})

const getChangedSelectedProcessAttributeDrafts = () =>
  Object.values(selectedProcessAttributeDrafts).filter((draft) => {
    const baseline = selectedProcessAttributeBaselines[draft.routeProcessId]
    return baseline !== serializeSelectedProcessAttributesDraft(draft)
  })

const getChangedSelectedProcessScheduleDrafts = () =>
  getChangedSelectedProcessAttributeDrafts().filter(hasSelectedScheduleCapacityDraftChanges)

const getChangedSelectedProcessRecordBindingDrafts = () =>
  Object.values(selectedProcessAttributeDrafts).filter(hasSelectedProcessRecordBindingDraftChanged)

const hasSelectedProcessAttributeDraftChanges = () =>
  getChangedSelectedProcessAttributeDrafts().length > 0

const saveSelectedProcessAttributeDrafts = async () => {
  const scheduleChangedDrafts = getChangedSelectedProcessScheduleDrafts()
  const recordBindingChangedDrafts = getChangedSelectedProcessRecordBindingDrafts()
  if (scheduleChangedDrafts.length === 0 && recordBindingChangedDrafts.length === 0) return
  selectedProcessAttributesSaving.value = true
  try {
    const editingRouteVersionId = requireCandidateRouteVersionId('工序属性保存')
    if (scheduleChangedDrafts.length > 0) {
      const scheduleProcessConfigs = scheduleChangedDrafts.map(buildSelectedProcessConfigSaveRow)
      await ProRouteFlowConfigApi.saveScheduleConfig({
        routeId: props.routeId,
        routeVersionId: editingRouteVersionId,
        processConfigs: scheduleProcessConfigs
      }, { ignoreErrorMessage: true })
      for (const draft of scheduleChangedDrafts) {
        await saveSelectedScheduleCapacity(draft, { ignoreErrorMessage: true })
      }
    }
    if (recordBindingChangedDrafts.length > 0) {
      const batchProcessConfigs = recordBindingChangedDrafts.map(
        buildSelectedProcessRecordBindingConfigSaveRow
      )
      await ProRouteFlowConfigApi.saveBatchRecordConfig({
        routeId: props.routeId,
        routeVersionId: editingRouteVersionId,
        processConfigs: batchProcessConfigs.map((processConfig) => ({
          routeProcessId: processConfig.routeProcessId,
          enabled: true,
          batchRecordReports: processConfig.batchRecordReports,
          formBindings: processConfig.formBindings,
          remark: processConfig.remark
        }))
      }, { ignoreErrorMessage: true })
    }
  } finally {
    selectedProcessAttributesSaving.value = false
  }
}

const normalizeRouteQueryText = (value: unknown) => {
  if (Array.isArray(value)) return value[0] ? String(value[0]) : ''
  return value ? String(value) : ''
}

const resolveExplicitRouteFlowRouteProcessId = () => {
  const routeProcessId = Number(
    props.targetRouteProcessId || normalizeRouteQueryText(route.query.routeProcessId)
  )
  return Number.isFinite(routeProcessId) && routeProcessId > 0 ? routeProcessId : null
}

const resolveRouteFlowReturnState = () => {
  const restoredRouteProcessId = resolveExplicitRouteFlowRouteProcessId()
  return { restoredRouteProcessId }
}

const restoreRouteFlowSelection = (
  selection: RouteFlowLastSelectionState,
  options: { source: RouteFlowSelectionRestoreSource }
) => {
  const routeProcessId = Number(selection.routeProcessId)
  if (!Number.isFinite(routeProcessId) || !findNode(routeProcessId)) {
    if (options.source === 'cache') {
      removeRouteFlowLastSelection()
    }
    return false
  }
  selectRouteProcessNode(routeProcessId, { persist: false })
  const detailFieldKey = selection.detailFieldKey
  if (!detailFieldKey) return true
  if (!processDetailInterestReady.value) return true
  if (selectedProcessDetailFieldKeys.value.includes(detailFieldKey)) {
    selectProcessDetailField(detailFieldKey, { persist: false })
    return true
  }
  if (options.source === 'cache') {
    clearRouteFlowLastSelectionDetailField(detailFieldKey)
  }
  return true
}

const restoreRouteFlowReturnState = () => {
  const explicitRouteProcessId = resolveExplicitRouteFlowRouteProcessId()
  if (explicitRouteProcessId) {
    restoreRouteFlowSelection({
      routeProcessId: explicitRouteProcessId
    }, { source: 'explicit' })
    return
  }
  const cachedSelection = readRouteFlowLastSelection()
  if (cachedSelection) {
    restoreRouteFlowSelection(cachedSelection, { source: 'cache' })
  }
}

const persistRouteFlowReturnState = async () => {
  const nextQuery: Record<string, string | string[] | undefined> = { ...route.query, tab: 'flow' }
  if (selectedRouteProcessId.value) {
    nextQuery.routeProcessId = String(selectedRouteProcessId.value)
  } else {
    delete nextQuery.routeProcessId
  }
  if (
    normalizeRouteQueryText(route.query.tab) === normalizeRouteQueryText(nextQuery.tab) &&
    normalizeRouteQueryText(route.query.routeProcessId) ===
      normalizeRouteQueryText(nextQuery.routeProcessId)
  ) {
    return
  }
  await router.replace({ query: nextQuery })
}

const defaultEdgeOptions = {
  type: 'smoothstep',
  animated: false,
  markerEnd: MarkerType.ArrowClosed,
  style: {
    stroke: '#1677ff',
    strokeWidth: 2.2
  }
}

const createStartBoundaryConnectionOption = (): ConnectionProcessOption => ({
  routeProcessId: PROCESS_START_NODE_ID,
  processName: boundaryLabel('START')
})
const createEndBoundaryConnectionOption = (): ConnectionProcessOption => ({
  routeProcessId: PROCESS_END_NODE_ID,
  processName: boundaryLabel('END')
})
const sortedActiveRouteNodes = computed(() => {
  return routeNodes.value
    .filter(isActiveRouteNode)
    .slice()
    .sort(
      (left, right) =>
        (left.sort || 0) - (right.sort || 0) || left.routeProcessId - right.routeProcessId
    )
})
const connectionSourceOptions = computed(() => {
  return [createStartBoundaryConnectionOption(), ...sortedActiveRouteNodes.value]
})
const connectionTargetOptions = computed(() => {
  const processTargets = sortedActiveRouteNodes.value.filter(
    (node) => node.routeProcessId !== connectionSourceRouteProcessId.value
  )
  if (connectionSourceRouteProcessId.value === PROCESS_START_NODE_ID) {
    return processTargets
  }
  return [createEndBoundaryConnectionOption(), ...processTargets]
})
const connectionSelectedSource = computed(() => {
  if (connectionSourceRouteProcessId.value === null) return undefined
  if (connectionSourceRouteProcessId.value === PROCESS_START_NODE_ID) {
    return createStartBoundaryConnectionOption()
  }
  return findNode(connectionSourceRouteProcessId.value)
})
const connectionSelectedTarget = computed(() => {
  if (connectionTargetRouteProcessId.value === null) return undefined
  if (connectionTargetRouteProcessId.value === PROCESS_END_NODE_ID) {
    return createEndBoundaryConnectionOption()
  }
  return findNode(connectionTargetRouteProcessId.value)
})
const buildConnectionAutocompleteOptions = (
  options: ConnectionProcessOption[],
  queryString: string
) => {
  const keyword = queryString.trim().toLowerCase()
  return options
    .map((option) => ({
      ...option,
      value: formatConnectionOption(option)
    }))
    .filter((option) => {
      if (!keyword) return true
      return [option.value, option.processName, option.processCode, String(option.sort || '')]
        .filter(Boolean)
        .some((item) => String(item).toLowerCase().includes(keyword))
    })
}
const queryConnectionSourceSuggestions = (
  queryString: string,
  callback: (items: ConnectionAutocompleteOption[]) => void
) => {
  callback(buildConnectionAutocompleteOptions(connectionSourceOptions.value, queryString))
}
const queryConnectionTargetSuggestions = (
  queryString: string,
  callback: (items: ConnectionAutocompleteOption[]) => void
) => {
  callback(buildConnectionAutocompleteOptions(connectionTargetOptions.value, queryString))
}
const connectionPreviousIncomingEdge = computed(() => {
  if (connectionTargetRouteProcessId.value === null) return undefined
  if (connectionTargetRouteProcessId.value === PROCESS_END_NODE_ID) return undefined
  if (connectionSourceRouteProcessId.value !== PROCESS_START_NODE_ID) return undefined
  return routeEdges.value.find(
    (edge) =>
      edge.targetRouteProcessId === connectionTargetRouteProcessId.value &&
      edge.sourceRouteProcessId !== connectionSourceRouteProcessId.value
  )
})
const connectionPreviousIncomingSource = computed(() => {
  if (!connectionPreviousIncomingEdge.value) return undefined
  return findNode(connectionPreviousIncomingEdge.value.sourceRouteProcessId)
})
const connectionConfirmDisabled = computed(() => {
  return (
    routeFlowWriteControlsDisabled.value ||
    loading.value ||
    saving.value ||
    routeProcessSaving.value ||
    connectionSourceRouteProcessId.value === null ||
    connectionTargetRouteProcessId.value === null ||
    connectionSourceRouteProcessId.value === connectionTargetRouteProcessId.value
  )
})
const selectedEdge = computed(() => {
  return routeEdges.value.find((edge) => edgeKey(edge) === selectedEdgeKey.value)
})
const selectedEdgeSource = computed(() => {
  return routeNodes.value.find(
    (node) => node.routeProcessId === selectedEdge.value?.sourceRouteProcessId
  )
})
const selectedEdgeTarget = computed(() => {
  return routeNodes.value.find(
    (node) => node.routeProcessId === selectedEdge.value?.targetRouteProcessId
  )
})
const selectedBoundaryEdge = computed(() => {
  return boundaryEdges.value.find((edge) => boundaryEdgeKey(edge) === selectedEdgeKey.value)
})
const selectedBoundaryRelations = computed(() => {
  if (!selectedBoundaryType.value) return []
  return boundaryEdges.value.filter((edge) => edge.boundaryType === selectedBoundaryType.value)
})
const visibleBoundaryRelationEdges = computed(() => {
  const routeProcessId = selectedRouteProcessId.value
  if (!routeProcessId) return boundaryEdges.value
  return boundaryEdges.value.filter(
    (edge) => Number(edge.routeProcessId) === Number(routeProcessId)
  )
})
const visibleRouteRelationEdges = computed(() => {
  const routeProcessId = selectedRouteProcessId.value
  if (!routeProcessId) return routeEdges.value
  return routeEdges.value.filter(
    (edge) =>
      Number(edge.sourceRouteProcessId) === Number(routeProcessId) ||
      Number(edge.targetRouteProcessId) === Number(routeProcessId)
  )
})
function buildRouteProcessRelationListSummary() {
  const boundarySummaries = visibleBoundaryRelationEdges.value.map(
    (edge) => `${boundaryEdgeSourceLabel(edge)} -> ${boundaryEdgeTargetLabel(edge)}`
  )
  const routeSummaries = visibleRouteRelationEdges.value.map((edge) => {
    const source = nodeLabel(findNode(edge.sourceRouteProcessId))
    const target = nodeLabel(findNode(edge.targetRouteProcessId))
    return `${source} -> ${target}`
  })
  const summaries = [...boundarySummaries, ...routeSummaries].filter((summary) => summary.trim())
  return summaries.length > 0 ? summaries.join('；') : '暂无关系'
}
const selectedRelationSourceLabel = computed(() => {
  if (selectedBoundaryEdge.value) return boundaryEdgeSourceLabel(selectedBoundaryEdge.value)
  return nodeLabel(selectedEdgeSource.value)
})
const selectedRelationTargetLabel = computed(() => {
  if (selectedBoundaryEdge.value) return boundaryEdgeTargetLabel(selectedBoundaryEdge.value)
  return nodeLabel(selectedEdgeTarget.value)
})
const availableProcessOptions = computed(() => {
  const usedProcessIds = new Set(routeNodes.value.map((node) => Number(node.processId)))
  return processOptions.value.filter(
    (process) => process.id && !usedProcessIds.has(Number(process.id))
  )
})
const loadGraph = async () => {
  loading.value = true
  clearSelectedProcessRouteConfigCache()
  let shouldAdjustViewport = false
  try {
    const [graph, routeProcessList] = await Promise.all([
        ProRouteApi.getRouteProcessFlowGraph(props.routeId, resolveRouteFlowGraphReadRouteVersionId()),
      ProRouteProcessApi.getRouteProcessListByRoute(props.routeId)
    ])
    routeNodes.value = normalizeNodes(graph.nodes || [])
    routeProcessRows.value = routeProcessList || []
    routeEdges.value = (graph.edges || []).map((edge) => ({
      ...edge,
      relationType: 'NORMAL'
    }))
    boundaryEdges.value = (graph.boundaryEdges || [])
      .map((edge) => ({ ...edge }))
      .sort((left, right) => {
        const typeResult = left.boundaryType.localeCompare(right.boundaryType)
        if (typeResult !== 0) return typeResult
        return (left.sort || 0) - (right.sort || 0)
      })
    pendingDeletedRouteProcessIds.value = new Set()
    nextDraftRouteProcessId.value = -1
    connectionPopoverVisible.value = false
    connectionSourceInputText.value = ''
    connectionTargetInputText.value = ''
    connectionSourceRouteProcessId.value = null
    connectionTargetRouteProcessId.value = null
    graphDirty.value = false
    clearSelectedProcessAttributeDrafts()
    clearRouteProcessKeyFlagBaselines()
    clearRouteProcessCheckFlagBaselines()
    clearRouteProcessWorkstationIdBaselines()
    applyDefaultKeyProcessLocally()
    resetRouteProcessKeyFlagBaselines()
    resetRouteProcessCheckFlagBaselines()
    resetRouteProcessWorkstationIdBaselines()
    syncFlowElements()
    applyValidation(graph)
    restoreRouteFlowReturnState()
    await focusProcessDetailFieldsForCapacitySource()
    if (selectedRouteProcessId.value && selectedNode.value) {
      await loadSelectedProcessDetail(selectedNode.value)
    } else {
      selectedRouteProcessId.value = null
      await loadSelectedProcessDetail()
    }
    shouldAdjustViewport = routeNodes.value.length > 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '加载流转关系图失败'))
    throw error
  } finally {
    loading.value = false
    if (shouldAdjustViewport) {
      await completeGraphLoadViewport()
    }
  }
}

const normalizeNodes = (sourceNodes: RouteFlowNodeVO[]) => {
  const sorted = sourceNodes.slice().sort((left, right) => (left.sort || 0) - (right.sort || 0))
  return sorted.map((node, index) => {
    const defaultPosition = defaultNodePosition(index)
    return {
      ...node,
      x: typeof node.x === 'number' ? node.x : defaultPosition.x,
      y: typeof node.y === 'number' ? node.y : defaultPosition.y
    }
  })
}

const findDefaultKeyProcessNode = () => {
  if (routeNodes.value.length === 0) return undefined
  const sorted = routeNodes.value.slice().sort((left, right) => {
    const sortResult = (left.sort || 0) - (right.sort || 0)
    return sortResult !== 0 ? sortResult : left.routeProcessId - right.routeProcessId
  })
  return sorted[sorted.length - 1]
}

const applyDefaultKeyProcessLocally = () => {
  if (!canMutateRouteFlow.value || routeNodes.value.length === 0) return
  if (routeNodes.value.some((node) => Boolean(node.keyFlag))) return
  const defaultNode = findDefaultKeyProcessNode()
  if (!defaultNode) return
  syncRouteNodeKeyFlag(defaultNode.routeProcessId, true)
}

const resetRouteProcessKeyFlagBaselines = () => {
  clearRouteProcessKeyFlagBaselines()
  routeNodes.value
    .filter((node) => !isDraftRouteProcessId(node.routeProcessId))
    .forEach((node) => {
      routeProcessKeyFlagBaselines[node.routeProcessId] = Boolean(node.keyFlag)
    })
}

const resetRouteProcessCheckFlagBaselines = () => {
  clearRouteProcessCheckFlagBaselines()
  routeNodes.value
    .filter((node) => !isDraftRouteProcessId(node.routeProcessId))
    .forEach((node) => {
      routeProcessCheckFlagBaselines[node.routeProcessId] = Boolean(node.checkFlag)
    })
}

const normalizeRouteProcessWorkstationId = (workstationId?: number | null) => {
  const normalized = Number(workstationId)
  return Number.isFinite(normalized) && normalized > 0 ? normalized : null
}

const resetRouteProcessWorkstationIdBaselines = () => {
  clearRouteProcessWorkstationIdBaselines()
  routeNodes.value
    .filter((node) => !isDraftRouteProcessId(node.routeProcessId))
    .forEach((node) => {
      routeProcessWorkstationIdBaselines[node.routeProcessId] =
        normalizeRouteProcessWorkstationId(node.routeProcessWorkstationId)
    })
}

const getChangedRouteProcessKeyFlagNodes = () =>
  routeNodes.value.filter((node) => {
    if (isDraftRouteProcessId(node.routeProcessId)) return false
    const baseline = routeProcessKeyFlagBaselines[node.routeProcessId]
    return baseline !== undefined && baseline !== Boolean(node.keyFlag)
  })

const hasRouteProcessKeyFlagDraftChanges = () =>
  getChangedRouteProcessKeyFlagNodes().length > 0

const getChangedRouteProcessCheckFlagNodes = () =>
  routeNodes.value.filter((node) => {
    if (isDraftRouteProcessId(node.routeProcessId)) return false
    const baseline = routeProcessCheckFlagBaselines[node.routeProcessId]
    return baseline !== undefined && baseline !== Boolean(node.checkFlag)
  })

const hasRouteProcessCheckFlagDraftChanges = () =>
  getChangedRouteProcessCheckFlagNodes().length > 0

const getChangedRouteProcessWorkstationNodes = () =>
  routeNodes.value.filter((node) => {
    if (isDraftRouteProcessId(node.routeProcessId)) return false
    const baseline = routeProcessWorkstationIdBaselines[node.routeProcessId]
    return (
      baseline !== undefined &&
      baseline !== normalizeRouteProcessWorkstationId(node.routeProcessWorkstationId)
    )
  })

const getChangedRouteProcessUpdateNodes = () => {
  const nodesById = new Map<number, RouteFlowNodeVO>()
  getChangedRouteProcessKeyFlagNodes().forEach((node) => {
    nodesById.set(node.routeProcessId, node)
  })
  getChangedRouteProcessCheckFlagNodes().forEach((node) => {
    nodesById.set(node.routeProcessId, node)
  })
  getChangedRouteProcessWorkstationNodes().forEach((node) => {
    nodesById.set(node.routeProcessId, node)
  })
  return Array.from(nodesById.values())
}

const hasRouteProcessUpdateDraftChanges = () =>
  hasRouteProcessKeyFlagDraftChanges() ||
  hasRouteProcessCheckFlagDraftChanges() ||
  getChangedRouteProcessWorkstationNodes().length > 0

const buildRouteProcessUpdatePayload = (): RouteFlowRouteProcessUpdateReqVO[] => {
  const changedNodes = getChangedRouteProcessUpdateNodes()
  const orderedNodes = [
    ...changedNodes.filter((node) => !Boolean(node.keyFlag)),
    ...changedNodes.filter((node) => Boolean(node.keyFlag))
  ]
  return orderedNodes.map((node) => ({
    id: node.routeProcessId,
    routeId: props.routeId,
    processId: node.processId,
    sort: node.sort || 0,
    workstationId: node.routeProcessWorkstationId,
    keyFlag: Boolean(node.keyFlag),
    checkFlag: Boolean(node.checkFlag)
  }))
}

const syncFlowElements = () => {
  flowNodes.value = routeNodes.value.filter(isActiveRouteNode).map(toFlowNode)
  flowEdges.value = []
  void nextTick(() => {
    flowEdges.value = routeEdges.value.filter(isActiveRouteEdge).map(toFlowEdge)
  })
}

const removeRouteProcessesFromDraft = (removedRouteProcessIds: number[]) => {
  if (removedRouteProcessIds.length === 0) return
  const removedRouteProcessIdSet = new Set(removedRouteProcessIds)
  clearRouteFlowLastSelectionRouteProcess(removedRouteProcessIdSet)
  const persistedRemovedRouteProcessIds = removedRouteProcessIds.filter(
    (routeProcessId) => !isDraftRouteProcessId(routeProcessId)
  )
  if (persistedRemovedRouteProcessIds.length > 0) {
    pendingDeletedRouteProcessIds.value = new Set([
      ...pendingDeletedRouteProcessIds.value,
      ...persistedRemovedRouteProcessIds
    ])
  }
  routeNodes.value = routeNodes.value.filter(
    (node) => !removedRouteProcessIdSet.has(node.routeProcessId)
  )
  routeEdges.value = routeEdges.value.filter(
    (edge) =>
      !removedRouteProcessIdSet.has(edge.sourceRouteProcessId) &&
      !removedRouteProcessIdSet.has(edge.targetRouteProcessId)
  )
  boundaryEdges.value = boundaryEdges.value.filter(
    (edge) => !removedRouteProcessIdSet.has(edge.routeProcessId)
  )
  flowNodes.value = flowNodes.value.filter((node) => !removedRouteProcessIdSet.has(Number(node.id)))
  flowEdges.value = flowEdges.value.filter(
    (edge) =>
      !removedRouteProcessIdSet.has(Number(edge.source)) &&
      !removedRouteProcessIdSet.has(Number(edge.target))
  )
  if (
    selectedRouteProcessId.value !== null &&
    removedRouteProcessIdSet.has(selectedRouteProcessId.value)
  ) {
    selectedRouteProcessId.value = null
    selectedProcessDetail.value = undefined
  }
  if (
    selectedEdge.value &&
    (removedRouteProcessIdSet.has(selectedEdge.value.sourceRouteProcessId) ||
      removedRouteProcessIdSet.has(selectedEdge.value.targetRouteProcessId))
  ) {
    selectedEdgeKey.value = ''
  }
  if (
    selectedBoundaryEdge.value &&
    removedRouteProcessIdSet.has(selectedBoundaryEdge.value.routeProcessId)
  ) {
    selectedEdgeKey.value = ''
  }
  markGraphDraftChanged()
}

const confirmRemoveRouteProcessFromDraft = async (node: RouteFlowNodeVO) => {
  await message.confirm(
    `确认从当前工艺路线删除工序「${nodeLabel(node)}」吗？该工序相关连接线也会被清理。`
  )
  removeRouteProcessesFromDraft([node.routeProcessId])
  message.warning('工序已删除为草稿，请点击顶部保存后生效。')
}

const syncRouteNodesFromFlowModel = (nodes: RouteFlowVueNode[]) => {
  if (!canMutateRouteFlow.value || flowNodes.value.length === 0) return
  const previousRouteProcessIds = new Set(
    flowNodes.value
      .filter((node) => !isBoundaryNodeId(node.id) && !isPendingDeletedFlowNode(node))
      .map((node) => Number(node.id))
      .filter((routeProcessId) => Number.isFinite(routeProcessId))
  )
  const nextRouteProcessIds = new Set(
    nodes
      .filter((node) => !isBoundaryNodeId(node.id) && !isPendingDeletedFlowNode(node))
      .map((node) => Number(node.id))
      .filter((routeProcessId) => Number.isFinite(routeProcessId))
  )
  const removedRouteProcessIds = Array.from(previousRouteProcessIds).filter(
    (routeProcessId) => !nextRouteProcessIds.has(routeProcessId)
  )
  removeRouteProcessesFromDraft(removedRouteProcessIds)
}

const toFlowNode = (node: RouteFlowNodeVO): RouteFlowVueNode => ({
  id: String(node.routeProcessId),
  type: 'route-process',
  position: { x: node.x || 0, y: node.y || 0 },
  data: { routeNode: node },
  selectable: true,
  draggable: canMutateRouteFlow.value,
  connectable: canMutateRouteFlow.value,
  width: NODE_WIDTH,
  height: NODE_HEIGHT
})

const toFlowEdge = (edge: RouteFlowEdgeVO): RouteFlowVueEdge => {
  const handles = resolveEdgeHandles(edge)
  return {
    id: edgeKey(edge),
    source: String(edge.sourceRouteProcessId),
    target: String(edge.targetRouteProcessId),
    sourceHandle: handles.sourceHandle,
    targetHandle: handles.targetHandle,
    type: 'smoothstep',
    markerEnd: MarkerType.ArrowClosed,
    data: { routeEdge: edge },
    selectable: true,
    animated: false,
    style: {
      stroke: selectedEdgeKey.value === edgeKey(edge) ? '#f56c6c' : '#1677ff',
      strokeWidth: selectedEdgeKey.value === edgeKey(edge) ? 3 : 2.2
    }
  }
}

const displayFlowNodes = computed<RouteFlowVueNode[]>({
  get: () => [...createBoundaryFlowNodes(), ...flowNodes.value],
  set: (nodes) => {
    syncRouteNodesFromFlowModel(nodes)
    flowNodes.value = nodes.filter(
      (node) => !isBoundaryNodeId(node.id) && !isPendingDeletedFlowNode(node)
    )
  }
})
const displayFlowEdges = computed<RouteFlowVueEdge[]>({
  get: () => [...createBoundaryFlowEdges(), ...flowEdges.value],
  set: (edges) => {
    flowEdges.value = edges.filter((edge) => !isBoundaryEdgeId(edge.id))
  }
})

const resolveEdgeHandles = (edge: RouteFlowEdgeVO) => {
  const source = findNode(edge.sourceRouteProcessId)
  const target = findNode(edge.targetRouteProcessId)
  if (!source || !target) {
    return { sourceHandle: 'source-right', targetHandle: 'target-left' }
  }
  const dx = (target.x || 0) - (source.x || 0)
  const dy = (target.y || 0) - (source.y || 0)
  if (Math.abs(dy) > Math.abs(dx)) {
    return dy >= 0
      ? { sourceHandle: 'source-bottom', targetHandle: 'target-top' }
      : { sourceHandle: 'source-top', targetHandle: 'target-bottom' }
  }
  return dx >= 0
    ? { sourceHandle: 'source-right', targetHandle: 'target-left' }
    : { sourceHandle: 'source-left', targetHandle: 'target-right' }
}

const applyValidation = (result: RouteFlowValidationVO) => {
  graphVersion.value = result.graphVersion || 0
  validationStatus.value = result.validationStatus || 'UNINITIALIZED'
  invalidRouteProcessIds.value = new Set(result.invalidRouteProcessIds || [])
}

const buildPayload = (): RouteFlowGraphSaveReqVO => ({
  routeId: props.routeId,
  routeVersionId: resolveRouteVersionIdForSave(),
  graphVersion: graphVersion.value,
  edges: routeEdges.value.filter(isActiveRouteEdge).map((edge) => ({
    sourceRouteProcessId: edge.sourceRouteProcessId,
    targetRouteProcessId: edge.targetRouteProcessId,
    relationType: 'NORMAL'
  })),
  boundaryEdges: boundaryEdges.value.filter(isActiveBoundaryEdge).map((edge) => ({
    boundaryType: edge.boundaryType,
    routeProcessId: edge.routeProcessId,
    sort: edge.sort
  })),
  layouts: routeNodes.value.filter(isActiveRouteNode).map((node) => ({
    routeProcessId: node.routeProcessId,
    x: Math.round(node.x || 0),
    y: Math.round(node.y || 0),
    width: NODE_WIDTH,
    height: NODE_HEIGHT
  })),
  routeProcessCreates: routeNodes.value
    .filter(isActiveRouteNode)
    .filter((node) => isDraftRouteProcessId(node.routeProcessId))
    .map((node) => ({
      clientRouteProcessId: node.routeProcessId,
      routeId: props.routeId,
      processId: node.processId,
      sort: node.sort || 0,
      prepareTime: node.prepareTime || 0,
      waitTime: node.waitTime || 0,
      colorCode: node.colorCode || '#00AEF3',
      keyFlag: Boolean(node.keyFlag),
      checkFlag: Boolean(node.checkFlag)
    })),
  routeProcessUpdates: buildRouteProcessUpdatePayload(),
  routeProcessDeletes: Array.from(pendingDeletedRouteProcessIds.value)
})

const buildValidationErrorMessage = (result: RouteFlowValidationVO) => {
  const messages = result.validationMessages || []
  return (
    messages
      .slice(0, 3)
      .map((item) => item.message)
      .filter(Boolean)
      .join('；') || '流转关系图校验未通过，请修正后再保存'
  )
}

const remapPersistedRouteProcessId = (
  routeProcessId: number | undefined | null,
  persistedRouteProcessIdMap: Map<number, number>
) => {
  if (routeProcessId === undefined || routeProcessId === null) return routeProcessId
  return persistedRouteProcessIdMap.get(Number(routeProcessId)) ?? routeProcessId
}

const normalizePersistedRouteProcessIdMap = (routeProcessIdMap?: Record<string, number>) => {
  const entries: Array<[number, number]> = Object.entries(routeProcessIdMap || {})
    .map(([draftRouteProcessId, persistedRouteProcessId]) => [
      Number(draftRouteProcessId),
      Number(persistedRouteProcessId)
    ] as [number, number])
    .filter(
      ([draftRouteProcessId, persistedRouteProcessId]) =>
        isDraftRouteProcessId(draftRouteProcessId) &&
        Number.isFinite(persistedRouteProcessId) &&
        persistedRouteProcessId > 0
    )
  return new Map<number, number>(entries)
}

const remapRouteProcessEdgeKey = (
  currentEdgeKey: string,
  persistedRouteProcessIdMap: Map<number, number>
) => {
  if (!currentEdgeKey) return currentEdgeKey
  const parts = currentEdgeKey.split('->')
  if (parts.length !== 2) return currentEdgeKey
  return parts
    .map((part) => {
      const routeProcessId = Number(part)
      if (!Number.isFinite(routeProcessId)) return part
      return String(remapPersistedRouteProcessId(routeProcessId, persistedRouteProcessIdMap))
    })
    .join('->')
}

const buildRouteProcessRowFromNode = (node: RouteFlowNodeVO): ProRouteProcessVO => ({
  id: node.routeProcessId,
  routeId: props.routeId,
  processId: node.processId,
  processCode: node.processCode,
  processName: node.processName,
  sort: node.sort || 0,
  predecessors: [],
  successors: [],
  prepareTime: node.prepareTime,
  waitTime: node.waitTime,
  colorCode: node.colorCode,
  keyFlag: Boolean(node.keyFlag),
  checkFlag: Boolean(node.checkFlag),
  routeProcessWorkstationId: node.routeProcessWorkstationId,
  workstationId: node.workstationId,
  workstationCode: node.workstationCode,
  workstationName: node.workstationName
})

const remapSelectedProcessAttributeDrafts = (
  persistedRouteProcessIdMap: Map<number, number>
) => {
  Object.keys(selectedProcessAttributeDrafts).forEach((key) => {
    const draftRouteProcessId = Number(key)
    const persistedRouteProcessId = persistedRouteProcessIdMap.get(draftRouteProcessId)
    if (!persistedRouteProcessId) return
    const draft = selectedProcessAttributeDrafts[draftRouteProcessId]
    delete selectedProcessAttributeDrafts[draftRouteProcessId]
    delete selectedProcessAttributeBaselines[draftRouteProcessId]
    selectedProcessAttributeDrafts[persistedRouteProcessId] = {
      ...draft,
      routeProcessId: persistedRouteProcessId
    }
  })
}

const applyPersistedRouteProcessIdMap = (routeProcessIdMap?: Record<string, number>) => {
  const persistedRouteProcessIdMap = normalizePersistedRouteProcessIdMap(routeProcessIdMap)
  if (persistedRouteProcessIdMap.size === 0) return
  const persistedRouteProcessIds = new Set(persistedRouteProcessIdMap.values())
  routeNodes.value = routeNodes.value.map((node) => ({
    ...node,
    routeProcessId: remapPersistedRouteProcessId(
      node.routeProcessId,
      persistedRouteProcessIdMap
    ) as number
  }))
  routeEdges.value = routeEdges.value.map((edge) => ({
    ...edge,
    sourceRouteProcessId: remapPersistedRouteProcessId(
      edge.sourceRouteProcessId,
      persistedRouteProcessIdMap
    ) as number,
    targetRouteProcessId: remapPersistedRouteProcessId(
      edge.targetRouteProcessId,
      persistedRouteProcessIdMap
    ) as number
  }))
  boundaryEdges.value = boundaryEdges.value.map((edge) => ({
    ...edge,
    routeProcessId: remapPersistedRouteProcessId(
      edge.routeProcessId,
      persistedRouteProcessIdMap
    ) as number
  }))
  routeProcessRows.value = routeProcessRows.value
    .filter((row) => !persistedRouteProcessIdMap.has(Number(row.id)))
    .map((row) => ({
      ...row,
      id: remapPersistedRouteProcessId(row.id, persistedRouteProcessIdMap) as number | undefined
    }))
  const routeProcessRowIds = new Set(
    routeProcessRows.value
      .map((row) => Number(row.id))
      .filter((routeProcessId) => Number.isFinite(routeProcessId))
  )
  routeNodes.value
    .filter((node) => persistedRouteProcessIds.has(node.routeProcessId))
    .forEach((node) => {
      if (routeProcessRowIds.has(node.routeProcessId)) return
      routeProcessRows.value.push(buildRouteProcessRowFromNode(node))
      routeProcessRowIds.add(node.routeProcessId)
    })
  selectedRouteProcessId.value = remapPersistedRouteProcessId(
    selectedRouteProcessId.value,
    persistedRouteProcessIdMap
  ) as number | null
  selectedEdgeKey.value = remapRouteProcessEdgeKey(selectedEdgeKey.value, persistedRouteProcessIdMap)
  highlightedRouteProcessId.value = remapPersistedRouteProcessId(
    highlightedRouteProcessId.value,
    persistedRouteProcessIdMap
  ) as number | null
  invalidRouteProcessIds.value = new Set(
    Array.from(invalidRouteProcessIds.value).map(
      (routeProcessId) =>
        remapPersistedRouteProcessId(routeProcessId, persistedRouteProcessIdMap) as number
    )
  )
  if (typeof connectionSourceRouteProcessId.value === 'number') {
    connectionSourceRouteProcessId.value = remapPersistedRouteProcessId(
      connectionSourceRouteProcessId.value,
      persistedRouteProcessIdMap
    ) as number
  }
  if (typeof connectionTargetRouteProcessId.value === 'number') {
    connectionTargetRouteProcessId.value = remapPersistedRouteProcessId(
      connectionTargetRouteProcessId.value,
      persistedRouteProcessIdMap
    ) as number
  }
  if (selectedProcessAttributes.routeProcessId) {
    selectedProcessAttributes.routeProcessId = remapPersistedRouteProcessId(
      selectedProcessAttributes.routeProcessId,
      persistedRouteProcessIdMap
    ) as number
  }
  capacityOverrideRouteProcessId.value = remapPersistedRouteProcessId(
    capacityOverrideRouteProcessId.value,
    persistedRouteProcessIdMap
  ) as number | null
  remapSelectedProcessAttributeDrafts(persistedRouteProcessIdMap)
  syncFlowElements()
}

const markGraphSaveClean = () => {
  graphDirty.value = false
  pendingDeletedRouteProcessIds.value = new Set()
  nextDraftRouteProcessId.value = -1
  resetRouteProcessKeyFlagBaselines()
  resetRouteProcessCheckFlagBaselines()
  resetRouteProcessWorkstationIdBaselines()
  Object.values(selectedProcessAttributeDrafts).forEach((draft) => {
    selectedProcessAttributeBaselines[draft.routeProcessId] =
      serializeSelectedProcessAttributesDraft(draft)
  })
}

const validateBeforeSubmit = async () => {
  saving.value = true
  try {
    syncRouteNodesFromFlow()
    getChangedSelectedProcessScheduleDrafts().forEach((draft) => {
      buildSelectedProcessConfigSaveRow(draft)
      if (normalizeScheduleCapacityMode(draft.capacityMode) === 'MANUAL_OVERRIDE') {
        resolveSelectedHourlyCapacity(draft)
      }
    })
    getChangedSelectedProcessRecordBindingDrafts().forEach((draft) => {
      buildSelectedProcessRecordBindingConfigSaveRow(draft)
    })
    const result = await ProRouteApi.validateRouteProcessFlowGraph(buildPayload(), {
      ignoreErrorMessage: true
    })
    applyValidation(result)
    if (!result.valid) {
      throw new Error(buildValidationErrorMessage(result))
    }
    return result
  } finally {
    saving.value = false
  }
}

const saveFromParent = async () => {
  saving.value = true
  try {
    syncRouteNodesFromFlow()
    const result = await persistRouteProcessDraftChanges()
    applyValidation(result)
    if (!result.valid) {
      throw new Error(buildValidationErrorMessage(result))
    }
    applyPersistedRouteProcessIdMap(result.routeProcessIdMap)
    await saveSelectedProcessAttributeDrafts()
    markGraphSaveClean()
    emit('saved')
    return result
  } finally {
    saving.value = false
  }
}

const persistRouteProcessDraftChanges = async () => {
  return await ProRouteApi.saveRouteProcessFlowGraph(buildPayload(), {
    ignoreErrorMessage: true
  })
}

const hasWorkspaceDraftChanges = () =>
  graphDirty.value ||
  hasSelectedProcessAttributeDraftChanges() ||
  hasRouteProcessUpdateDraftChanges()

const discardWorkspaceDraftChanges = () => {
  graphDirty.value = false
  clearSelectedProcessAttributeDrafts()
  clearRouteProcessKeyFlagBaselines()
  clearRouteProcessCheckFlagBaselines()
  clearRouteProcessWorkstationIdBaselines()
}

const handleRequestBack = () => {
  emit('request-back')
}

const handleRequestSubmit = () => {
  if (routeFlowWriteControlsDisabled.value) return
  emit('request-submit')
}

const collectStartBoundaryRootIds = () =>
  new Set(
    boundaryEdges.value
      .filter((edge) => edge.boundaryType === 'START')
      .map((edge) => edge.routeProcessId)
  )

const normalizeStartBoundaryTargetsAsRoots = () => {
  const startRouteProcessIds = collectStartBoundaryRootIds()
  if (startRouteProcessIds.size === 0) return false
  const retainedEdges = routeEdges.value.filter(
    (edge) => !startRouteProcessIds.has(edge.targetRouteProcessId)
  )
  if (retainedEdges.length === routeEdges.value.length) return false
  routeEdges.value = retainedEdges
  selectedEdgeKey.value = ''
  markGraphDraftChanged()
  return true
}

const applyAutoLayout = async (options: AutoLayoutOptions = DEFAULT_AUTO_LAYOUT_OPTIONS) => {
  if (!canMutateRouteFlow.value) return false
  const notify = options.notify !== false
  const normalizedStartRoots = normalizeStartBoundaryTargetsAsRoots()
  let positions: Map<number, RouteFlowLayoutPosition>
  try {
    positions = buildBranchLayoutPositions()
  } catch (error) {
    message.error(resolveErrorMessage(error, '关系图自动布局失败'))
    return false
  }

  routeNodes.value.forEach((node) => {
    const position = positions.get(node.routeProcessId)
    if (!position) return
    node.x = position.x
    node.y = position.y
  })
  selectedEdgeKey.value = ''
  syncFlowElements()
  await fitGraphAfterLayout(options.focusRouteProcessId)
  autoLayoutRevision.value += 1
  if (normalizedStartRoots) {
    message.warning('已按工序开始关系调整首工序入口，请保存后生效')
  }
  if (notify) {
    message.success('已按当前关系自动布局')
  }
  return true
}

const handleAutoLayout = async () => {
  return await applyAutoLayout()
}

const fitGraphAfterLayout = async (focusRouteProcessId?: number) => {
  await nextTick()
  await new Promise<void>((resolve) => {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => resolve())
    })
  })
  if (focusRouteProcessId !== undefined) {
    handleFitBranch(focusRouteProcessId)
    return
  }
  handleFitScreen()
}

const runPendingEntryAutoLayout = async () => {
  if (!autoLayoutEntryPending.value || loading.value || routeNodes.value.length === 0) return false
  autoLayoutEntryPending.value = false
  return await handleAutoLayout()
}

const completeGraphLoadViewport = async () => {
  const didRunAutoLayout = await runPendingEntryAutoLayout()
  await nextTick()
  if (!didRunAutoLayout) {
    handleFitScreen()
  }
}

const autoLayoutOnEntry = async () => {
  autoLayoutEntryPending.value = true
  await nextTick()
  await runPendingEntryAutoLayout()
}

const handleFitScreen = () => {
  if (flowNodes.value.length === 0) return
  fitView({ padding: 0.18, duration: 260, maxZoom: 1 })
}

const refreshRouteFlowViewport = async () => {
  await nextTick()
  window.requestAnimationFrame(() => {
    handleFitScreen()
  })
}

const handleExitRouteFlowMaximized = async () => {
  if (!isRouteFlowMaximized.value) return
  isRouteFlowMaximized.value = false
  await refreshRouteFlowViewport()
}

const handleToggleRouteFlowMaximized = async () => {
  isRouteFlowMaximized.value = !isRouteFlowMaximized.value
  await refreshRouteFlowViewport()
}

const handleRouteFlowMaximizeKeydown = (event: KeyboardEvent) => {
  if (event.key !== 'Escape' || !isRouteFlowMaximized.value) return
  void handleExitRouteFlowMaximized()
}

const collectBranchRouteProcessIds = (sourceRouteProcessId: number) => {
  if (!findNode(sourceRouteProcessId)) return []
  const branchRouteProcessIds = new Set<number>()
  const pending = [sourceRouteProcessId]
  while (pending.length > 0) {
    const routeProcessId = pending.shift()
    if (routeProcessId === undefined || branchRouteProcessIds.has(routeProcessId)) continue
    branchRouteProcessIds.add(routeProcessId)
    routeEdges.value
      .filter((edge) => edge.sourceRouteProcessId === routeProcessId)
      .map((edge) => edge.targetRouteProcessId)
      .forEach((targetRouteProcessId) => pending.push(targetRouteProcessId))
  }
  return Array.from(branchRouteProcessIds)
}

const handleFitBranch = (sourceRouteProcessId: number) => {
  const branchRouteProcessIds = collectBranchRouteProcessIds(sourceRouteProcessId)
  if (branchRouteProcessIds.length === 0) {
    handleFitScreen()
    return
  }
  fitView({
    nodes: branchRouteProcessIds.map(String),
    padding: 0.5,
    duration: 260,
    minZoom: 0.65,
    maxZoom: 1
  })
}

const handleSearch = () => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    highlightedRouteProcessId.value = null
    return
  }
  const matched = routeNodes.value.find((node) => nodeLabel(node).toLowerCase().includes(keyword))
  highlightedRouteProcessId.value = matched?.routeProcessId || null
  if (matched) {
    selectRouteProcessNode(matched.routeProcessId, { persist: false })
    focusNode(matched.routeProcessId)
  }
}

const handleConnect = async (connection: Connection) => {
  if (!canMutateRouteFlow.value || !connection.source || !connection.target) return
  if (connection.source === PROCESS_START_NODE_ID && !isBoundaryNodeId(connection.target)) {
    const boundaryEdgeAdded = addBoundaryEdge('START', Number(connection.target))
    if (!boundaryEdgeAdded) return
    await applyAutoLayout({
      notify: false,
      focusRouteProcessId: undefined
    })
    return
  }
  if (connection.target === PROCESS_END_NODE_ID && !isBoundaryNodeId(connection.source)) {
    const boundaryEdgeAdded = addBoundaryEdge('END', Number(connection.source))
    if (!boundaryEdgeAdded) return
    await applyAutoLayout({
      notify: false,
      focusRouteProcessId: Number(connection.source)
    })
    return
  }
  if (isBoundaryNodeId(connection.source) || isBoundaryNodeId(connection.target)) {
    message.warning('工序开始只能连接首工序，末工序只能连接工序结束')
    return
  }
  const edgeAdded = addEdge(Number(connection.source), Number(connection.target))
  if (!edgeAdded) return
  await applyAutoLayout({
    notify: false,
    focusRouteProcessId: Number(connection.source)
  })
}

const handleConnectionPopoverToggle = () => {
  if (routeFlowWriteControlsDisabled.value) return
  connectionPopoverVisible.value = !connectionPopoverVisible.value
}

const clearConnectionTargetSelection = () => {
  connectionTargetInputText.value = ''
  connectionTargetRouteProcessId.value = null
}

const syncConnectionTargetForSource = (routeProcessId?: ConnectionSourceRouteProcessId | null) => {
  if (
    routeProcessId === connectionTargetRouteProcessId.value ||
    (connectionTargetRouteProcessId.value === PROCESS_END_NODE_ID &&
      typeof routeProcessId !== 'number')
  ) {
    clearConnectionTargetSelection()
  }
}

const handleConnectionSourceInput = (value: string) => {
  if (value === nodeLabel(connectionSelectedSource.value)) return
  connectionSourceRouteProcessId.value = null
  clearConnectionTargetSelection()
}

const handleConnectionTargetInput = (value: string) => {
  if (value === nodeLabel(connectionSelectedTarget.value)) return
  connectionTargetRouteProcessId.value = null
}

const handleConnectionSourceClear = () => {
  connectionSourceInputText.value = ''
  connectionSourceRouteProcessId.value = null
  clearConnectionTargetSelection()
}

const handleConnectionTargetClear = () => {
  clearConnectionTargetSelection()
}

const handleConnectionSourceSelect = (option: ConnectionAutocompleteOption) => {
  if (option.routeProcessId === PROCESS_END_NODE_ID) return
  connectionSourceRouteProcessId.value = option.routeProcessId
  connectionSourceInputText.value = option.value
  syncConnectionTargetForSource(option.routeProcessId)
}

const handleConnectionTargetSelect = (option: ConnectionAutocompleteOption) => {
  if (option.routeProcessId === PROCESS_START_NODE_ID) return
  connectionTargetRouteProcessId.value = option.routeProcessId
  connectionTargetInputText.value = option.value
}

const handleConfirmConnection = async () => {
  if (!canMutateRouteFlow.value) return
  if (
    connectionSourceRouteProcessId.value === null ||
    connectionTargetRouteProcessId.value === null
  ) {
    message.warning('请选择起始工序和目标工序')
    return
  }

  const sourceId = connectionSourceRouteProcessId.value
  const targetId = connectionTargetRouteProcessId.value
  let edgeAdded = false
  let focusRouteProcessId: number | undefined
  if (sourceId === PROCESS_START_NODE_ID) {
    if (targetId === PROCESS_END_NODE_ID) {
      message.warning('工序开始不能直接连接工序结束')
      return
    }
    edgeAdded = addBoundaryEdge('START', targetId)
  } else if (targetId === PROCESS_END_NODE_ID) {
    edgeAdded = addBoundaryEdge('END', sourceId)
    focusRouteProcessId = sourceId
  } else {
    edgeAdded = addEdge(sourceId, targetId)
    focusRouteProcessId = sourceId
  }
  if (!edgeAdded) {
    return
  }

  await applyAutoLayout({
    notify: false,
    focusRouteProcessId
  })
  clearConnectionTargetSelection()
  message.success('连接已加入未保存草稿，并完成自动布局')
}

const handleNodeDragStop = (event: NodeDragEvent) => {
  if (!canMutateRouteFlow.value) return
  const draggedNode = event.node
  if (isBoundaryNodeId(draggedNode.id)) return
  const routeProcessId = Number(draggedNode.id)
  const node = routeNodes.value.find((item) => item.routeProcessId === routeProcessId)
  if (!node) return
  node.x = Math.max(0, Math.round(draggedNode.position.x))
  node.y = Math.max(0, Math.round(draggedNode.position.y))
  syncFlowElements()
}

const handleNodeClick = (event: NodeMouseEvent) => {
  if (event.node.id === PROCESS_START_NODE_ID) {
    handleBoundaryNodeSelect('START')
    return
  }
  if (event.node.id === PROCESS_END_NODE_ID) {
    handleBoundaryNodeSelect('END')
    return
  }
  const routeProcessId = Number(event.node.id)
  selectRouteProcessNode(routeProcessId, { persist: true })
}

const handleEdgeClick = (event: EdgeMouseEvent) => {
  selectedEdgeKey.value = event.edge.id
  syncFlowElements()
}

const handleEdgeSelect = (edge: RouteFlowEdgeVO) => {
  selectedEdgeKey.value = edgeKey(edge)
  syncFlowElements()
}

const handleBoundaryEdgeSelect = (edge: RouteFlowBoundaryEdgeVO) => {
  selectedEdgeKey.value = boundaryEdgeKey(edge)
  syncFlowElements()
}

const handleBoundaryNodeSelect = (boundaryType: RouteFlowBoundaryType) => {
  selectedRouteProcessId.value = null
  selectedBoundaryType.value = boundaryType
  selectedBoundaryDetailFieldKey.value =
    boundaryType === 'START' ? 'batchRecordAttachment' : 'releaseOwner'
  selectedEdgeKey.value = ''
  if (boundaryType === 'END') {
    void loadReleaseApprovalRuleDetail()
  }
  if (boundaryType === 'START') {
    void loadBatchRecordAttachmentOwners()
  }
}

const handleEdgesChange = async (changes: EdgeChange[]) => {
  if (!canMutateRouteFlow.value) {
    syncFlowElements()
    return
  }
  const removedIds = changes
    .filter((change) => change.type === 'remove')
    .flatMap((change) => ('id' in change ? [change.id] : []))
  if (removedIds.length === 0) return
  const removedBoundaryEdgeIds = removedIds.filter(isBoundaryEdgeId)
  const removedRouteEdgeIds = removedIds.filter((id) => !isBoundaryEdgeId(id))
  const removedRouteEdges = routeEdges.value.filter((edge) =>
    removedRouteEdgeIds.includes(edgeKey(edge))
  )
  const boundaryEdgeChanged = boundaryEdges.value.some((edge) =>
    removedBoundaryEdgeIds.includes(boundaryEdgeKey(edge))
  )
  const routeEdgeChanged = removedRouteEdges.length > 0
  if (!boundaryEdgeChanged && !routeEdgeChanged) return
  boundaryEdges.value = boundaryEdges.value.filter(
    (edge) => !removedBoundaryEdgeIds.includes(boundaryEdgeKey(edge))
  )
  routeEdges.value = routeEdges.value.filter((edge) => !removedRouteEdgeIds.includes(edgeKey(edge)))
  selectedEdgeKey.value = ''
  markGraphDraftChanged()
  syncFlowElements()
  if (routeEdgeChanged) {
    await applyAutoLayout({
      notify: false,
      focusRouteProcessId: removedRouteEdges[0].sourceRouteProcessId
    })
  }
}

const handleNodePointerDown = (node: RouteFlowNodeVO) => {
  selectRouteProcessNode(node.routeProcessId, { persist: false })
}

const handleRouteProcessNodeClick = (node: RouteFlowNodeVO) => {
  selectRouteProcessNode(node.routeProcessId, { persist: true })
}

const handleRouteProcessNodeKeydown = async (event: KeyboardEvent, node: RouteFlowNodeVO) => {
  if (!canMutateRouteFlow.value || !['Delete', 'Backspace'].includes(event.key)) return
  event.preventDefault()
  event.stopPropagation()
  selectRouteProcessNode(node.routeProcessId, { persist: false })
  try {
    await confirmRemoveRouteProcessFromDraft(node)
  } catch (error) {
    if (isCancelError(error)) return
    throw error
  }
}

const handlePortPointerDown = (node: RouteFlowNodeVO) => {
  selectRouteProcessNode(node.routeProcessId, { persist: false })
}

const loadProcessOptions = async () => {
  if (processOptions.value.length > 0) return
  processOptionsLoading.value = true
  try {
    processOptions.value = await ProProcessApi.getProcessSimpleList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '加载工序列表失败'))
    throw error
  } finally {
    processOptionsLoading.value = false
  }
}

const handleOpenRouteProcessAddDialog = async () => {
  if (!canMutateRouteFlow.value) return
  await loadProcessOptions()
  routeProcessForm.processId = undefined
  routeProcessDialogVisible.value = true
  await nextTick()
  routeProcessFormRef.value?.clearValidate?.()
}

const handleRouteProcessAdd = async () => {
  if (!canMutateRouteFlow.value) return
  await routeProcessFormRef.value?.validate?.()
  if (!routeProcessForm.processId) return
  const maxSort = routeNodes.value.reduce((max, node) => Math.max(max, node.sort || 0), 0)
  const process = processOptions.value.find(
    (item) => Number(item.id) === Number(routeProcessForm.processId)
  )
  if (!process?.id) {
    message.error('添加工序失败：工序不存在')
    return
  }
  const routeProcessId = nextDraftRouteProcessId.value
  nextDraftRouteProcessId.value -= 1
  const position = defaultNodePosition(routeNodes.value.length)
  routeNodes.value = [
    ...routeNodes.value,
    {
      routeProcessId,
      processId: process.id,
      processCode: process.code,
      processName: process.name,
      sort: maxSort + 1,
      x: position.x,
      y: position.y,
      prepareTime: 0,
      waitTime: 0,
      colorCode: '#00AEF3',
      keyFlag: false,
      checkFlag: false
    }
  ]
  routeProcessDialogVisible.value = false
  selectedRouteProcessId.value = routeProcessId
  selectedEdgeKey.value = ''
  markGraphDraftChanged()
  syncFlowElements()
  message.warning('工序已添加为草稿，请点击顶部保存后生效。')
}

const handleRouteProcessDelete = async () => {
  if (!selectedNode.value || !canMutateRouteFlow.value) return
  const node = selectedNode.value
  try {
    await confirmRemoveRouteProcessFromDraft(node)
  } catch (error) {
    if (isCancelError(error)) return
    throw error
  }
}

const isSelectedProcessDetailRequestCurrent = (requestId: number, routeProcessId: number) => {
  if (requestId !== selectedProcessDetailRequestId) return false
  return Number(selectedRouteProcessId.value) === Number(routeProcessId)
}

const loadSelectedProcessDetail = async (node?: RouteFlowNodeVO) => {
  const requestId = ++selectedProcessDetailRequestId
  selectedProcessDetail.value = undefined
  selectedProcessMachineryList.value = []
  resetSelectedProcessAttributes()
  selectedProcessDetailLoading.value = Boolean(node)
  selectedProcessMachineryLoading.value = Boolean(node)
  selectedProcessAttributesLoading.value = Boolean(node)
  if (!node) {
    selectedProcessAttributesLoading.value = false
    return
  }

  const routeProcessId = node.routeProcessId
  const processDetailPromise = ProProcessApi.getProcess(node.processId, { routeId: props.routeId })
    .then((processDetail) => {
      if (!isSelectedProcessDetailRequestCurrent(requestId, routeProcessId)) return
      selectedProcessDetail.value = processDetail
    })
    .catch((error) => {
      if (!isSelectedProcessDetailRequestCurrent(requestId, routeProcessId)) return
      message.error(resolveErrorMessage(error, '加载工序设置详情失败'))
    })
    .finally(() => {
      if (!isSelectedProcessDetailRequestCurrent(requestId, routeProcessId)) return
      selectedProcessDetailLoading.value = false
    })

  const machineryListPromise = ProProcessApi.getProcessMachineryList(node.processId)
    .then((machineryList) => {
      if (!isSelectedProcessDetailRequestCurrent(requestId, routeProcessId)) return
      selectedProcessMachineryList.value = machineryList
    })
    .catch((error) => {
      if (!isSelectedProcessDetailRequestCurrent(requestId, routeProcessId)) return
      message.error(resolveErrorMessage(error, '加载工序关联设备失败'))
    })
    .finally(() => {
      if (!isSelectedProcessDetailRequestCurrent(requestId, routeProcessId)) return
      selectedProcessMachineryLoading.value = false
    })

  const attributesPromise = loadSelectedProcessAttributes(node, requestId)

  await Promise.allSettled([processDetailPromise, machineryListPromise, attributesPromise])
}

const syncRouteNodeKeyFlag = (routeProcessId: number, keyFlag: boolean) => {
  routeNodes.value = routeNodes.value.map((node) =>
    Number(node.routeProcessId) === Number(routeProcessId) ? { ...node, keyFlag } : node
  )
}

const syncRouteNodeCheckFlag = (routeProcessId: number, checkFlag: boolean) => {
  routeNodes.value = routeNodes.value.map((node) =>
    Number(node.routeProcessId) === Number(routeProcessId) ? { ...node, checkFlag } : node
  )
}

const handleKeyProcessToggle = async (enabled: boolean) => {
  if (!selectedNode.value || !isProcessDetailFieldEditable('keyFlag')) return
  if (Boolean(selectedNode.value.keyFlag) === enabled) return
  if (enabled) {
    routeNodes.value = routeNodes.value.map((node) => ({
      ...node,
      keyFlag: Number(node.routeProcessId) === Number(selectedNode.value?.routeProcessId)
    }))
  } else {
    syncRouteNodeKeyFlag(selectedNode.value.routeProcessId, false)
  }
  markGraphDraftChanged()
  syncFlowElements()
  message.warning('关键工序已保存为草稿，请点击顶部保存后生效。')
}

function handleCheckFlagToggle(enabled: boolean) {
  if (!selectedNode.value || !isProcessDetailFieldEditable('checkFlag')) return
  if (Boolean(selectedNode.value.checkFlag) === enabled) return
  syncRouteNodeCheckFlag(selectedNode.value.routeProcessId, enabled)
  markGraphDraftChanged()
  syncFlowElements()
  message.warning('质检确认已保存为草稿，请点击顶部保存后生效。')
}

const markGraphDraftChanged = () => {
  graphDirty.value = true
  validationStatus.value = 'UNINITIALIZED'
  invalidRouteProcessIds.value = new Set()
}

const wouldCreateCycle = (
  sourceRouteProcessId: number,
  targetRouteProcessId: number,
  edges: RouteFlowEdgeVO[]
) => {
  const outgoing = new Map<number, number[]>()
  edges.forEach((edge) => {
    const targets = outgoing.get(edge.sourceRouteProcessId) || []
    targets.push(edge.targetRouteProcessId)
    outgoing.set(edge.sourceRouteProcessId, targets)
  })
  const pending = [targetRouteProcessId]
  const visited = new Set<number>()
  while (pending.length > 0) {
    const routeProcessId = pending.shift()
    if (routeProcessId === undefined || visited.has(routeProcessId)) continue
    if (routeProcessId === sourceRouteProcessId) return true
    visited.add(routeProcessId)
    pending.push(...(outgoing.get(routeProcessId) || []))
  }
  return false
}

const addEdge = (
  sourceRouteProcessId: number,
  targetRouteProcessId: number,
  replacedEdgeKey = ''
) => {
  if (sourceRouteProcessId === targetRouteProcessId) {
    message.warning('不能连接到自身工序')
    return false
  }
  const candidateEdges = routeEdges.value.filter((edge) => edgeKey(edge) !== replacedEdgeKey)
  const exists = candidateEdges.some(
    (edge) =>
      edge.sourceRouteProcessId === sourceRouteProcessId &&
      edge.targetRouteProcessId === targetRouteProcessId
  )
  if (exists) {
    message.warning('该流转关系已存在')
    return false
  }
  if (wouldCreateCycle(sourceRouteProcessId, targetRouteProcessId, candidateEdges)) {
    message.warning('该连接会形成循环，不能保存')
    syncFlowElements()
    return false
  }
  const nextEdge: RouteFlowEdgeVO = {
    sourceRouteProcessId,
    targetRouteProcessId,
    relationType: 'NORMAL'
  }
  routeEdges.value = [...candidateEdges, nextEdge]
  selectedRouteProcessId.value = null
  selectedBoundaryType.value = null
  selectedEdgeKey.value = `${sourceRouteProcessId}->${targetRouteProcessId}`
  markGraphDraftChanged()
  syncFlowElements()
  return true
}

const normalizeRouteProcessIdList = (values: number[]) => {
  const ids = values
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value) && value > 0)
  return Array.from(new Set(ids))
}

const ensureRouteProcessRelationTargets = (routeProcessIds: number[]) => {
  routeProcessIds.forEach((routeProcessId) => {
    if (!findNode(routeProcessId)) {
      throw new Error(`流转关系调整失败：路线工序 ${routeProcessId} 不存在。`)
    }
  })
}

const replaceRouteProcessIncomingEdges = (
  targetRouteProcessId: number,
  sourceRouteProcessIds: number[]
) => {
  const nextSourceRouteProcessIds = normalizeRouteProcessIdList(sourceRouteProcessIds)
  ensureRouteProcessRelationTargets([targetRouteProcessId, ...nextSourceRouteProcessIds])
  const retainedEdges = routeEdges.value.filter(
    (edge) => Number(edge.targetRouteProcessId) !== Number(targetRouteProcessId)
  )
  const nextEdges: RouteFlowEdgeVO[] = []
  nextSourceRouteProcessIds.forEach((sourceRouteProcessId) => {
    if (sourceRouteProcessId === targetRouteProcessId) {
      throw new Error('流转关系调整失败：不能把当前工序设为自己的前置工序。')
    }
    if (wouldCreateCycle(sourceRouteProcessId, targetRouteProcessId, [...retainedEdges, ...nextEdges])) {
      throw new Error('流转关系调整失败：前置工序会形成循环。')
    }
    nextEdges.push({
      sourceRouteProcessId,
      targetRouteProcessId,
      relationType: 'NORMAL'
    })
  })
  routeEdges.value = [...retainedEdges, ...nextEdges]
  if (nextEdges.length > 0) {
    boundaryEdges.value = boundaryEdges.value.filter(
      (edge) => edge.boundaryType !== 'START' || edge.routeProcessId !== targetRouteProcessId
    )
  }
}

const replaceRouteProcessOutgoingEdges = (
  sourceRouteProcessId: number,
  targetRouteProcessIds: number[]
) => {
  const nextTargetRouteProcessIds = normalizeRouteProcessIdList(targetRouteProcessIds)
  ensureRouteProcessRelationTargets([sourceRouteProcessId, ...nextTargetRouteProcessIds])
  const retainedEdges = routeEdges.value.filter(
    (edge) => Number(edge.sourceRouteProcessId) !== Number(sourceRouteProcessId)
  )
  const nextEdges: RouteFlowEdgeVO[] = []
  nextTargetRouteProcessIds.forEach((targetRouteProcessId) => {
    if (sourceRouteProcessId === targetRouteProcessId) {
      throw new Error('流转关系调整失败：不能把当前工序设为自己的后续工序。')
    }
    if (wouldCreateCycle(sourceRouteProcessId, targetRouteProcessId, [...retainedEdges, ...nextEdges])) {
      throw new Error('流转关系调整失败：后续工序会形成循环。')
    }
    nextEdges.push({
      sourceRouteProcessId,
      targetRouteProcessId,
      relationType: 'NORMAL'
    })
  })
  routeEdges.value = [...retainedEdges, ...nextEdges]
  if (nextEdges.length > 0) {
    boundaryEdges.value = boundaryEdges.value.filter(
      (edge) => edge.boundaryType !== 'END' || edge.routeProcessId !== sourceRouteProcessId
    )
  }
}

const ensureSelectedProcessAttributeDraft = () => {
  const routeProcessId = selectedProcessAttributes.routeProcessId || selectedRouteProcessId.value
  if (!routeProcessId) {
    throw new Error('工序属性调整失败：缺少目标路线工序。')
  }
  const draft = selectedProcessAttributeDrafts[routeProcessId]
  if (!draft) {
    throw new Error('工序属性调整失败：工序属性仍在加载，请稍后重试。')
  }
  return { routeProcessId, draft }
}

const handleProductionQuantityFactorChange = (value: number | undefined) => {
  try {
    if (!isProcessDetailFieldEditable('productionQuantityFactor')) return
    const productionQuantityFactor = normalizeProductionQuantityFactor(value)
    if (!positiveNumber(productionQuantityFactor)) {
      throw new Error('生产系数必须大于 0。')
    }
    const { routeProcessId, draft } = ensureSelectedProcessAttributeDraft()
    selectedProcessAttributes.productionQuantityFactor = productionQuantityFactor
    selectedProcessAttributeDrafts[routeProcessId].productionQuantityFactor = productionQuantityFactor
    draft.productionQuantityFactor = productionQuantityFactor
    markGraphDraftChanged()
  } catch (error) {
    message.error(resolveErrorMessage(error, '生产系数调整失败'))
  }
}

const handlePredecessorChange = (values: number[]) => {
  try {
    if (!selectedNode.value || !isProcessDetailFieldEditable('predecessor')) return
    replaceRouteProcessIncomingEdges(selectedNode.value.routeProcessId, values)
    markGraphDraftChanged()
    syncFlowElements()
  } catch (error) {
    syncFlowElements()
    message.error(resolveErrorMessage(error, '前置工序调整失败'))
  }
}

const handleSuccessorsChange = (values: number[]) => {
  try {
    if (!selectedNode.value || !isProcessDetailFieldEditable('successors')) return
    replaceRouteProcessOutgoingEdges(selectedNode.value.routeProcessId, values)
    markGraphDraftChanged()
    syncFlowElements()
  } catch (error) {
    syncFlowElements()
    message.error(resolveErrorMessage(error, '后续工序调整失败'))
  }
}

const handleAddProcessDetailField = async () => {
  const fieldKey = selectedProcessDetailFieldToAdd.value
  if (!fieldKey || selectedProcessDetailFieldKeys.value.includes(fieldKey)) return
  if (processDetailInterestMutationDisabled.value) return
  const previousFieldKeys = [...selectedProcessDetailFieldKeys.value]
  const nextFieldKeys = [...previousFieldKeys, fieldKey]
  selectedProcessDetailFieldKeys.value = nextFieldKeys
  syncSelectedProcessDetailFieldToAdd()
  try {
    await saveProcessDetailFieldConfig(nextFieldKeys)
  } catch (error) {
    selectedProcessDetailFieldKeys.value = previousFieldKeys
    syncSelectedProcessDetailFieldToAdd()
    throw error
  }
}

const handleRemoveProcessDetailField = async (fieldKey: ProcessDetailFieldKey) => {
  if (processDetailInterestMutationDisabled.value) return
  const previousFieldKeys = [...selectedProcessDetailFieldKeys.value]
  const previousSelectedProcessDetailFieldKey = selectedProcessDetailFieldKey.value
  const nextFieldKeys = selectedProcessDetailFieldKeys.value.filter((key) => key !== fieldKey)
  selectedProcessDetailFieldKeys.value = nextFieldKeys
  if (selectedProcessDetailFieldKey.value === fieldKey) {
    selectedProcessDetailFieldKey.value = undefined
  }
  syncSelectedProcessDetailFieldToAdd()
  try {
    await saveProcessDetailFieldConfig(nextFieldKeys)
    clearRouteFlowLastSelectionDetailField(fieldKey)
  } catch (error) {
    selectedProcessDetailFieldKeys.value = previousFieldKeys
    selectedProcessDetailFieldKey.value = previousSelectedProcessDetailFieldKey
    syncSelectedProcessDetailFieldToAdd()
    throw error
  }
}

const addBoundaryEdge = (boundaryType: RouteFlowBoundaryType, routeProcessId: number) => {
  if (!Number.isFinite(routeProcessId) || !findNode(routeProcessId)) {
    message.warning('边界关系必须连接当前路线工序')
    return false
  }
  if (
    boundaryEdges.value.some(
      (edge) => edge.boundaryType === boundaryType && edge.routeProcessId === routeProcessId
    )
  ) {
    message.warning('该边界关系已存在')
    return false
  }
  const retainedBoundaryEdges = boundaryEdges.value
  const removedIncomingEdges =
    boundaryType === 'START'
      ? routeEdges.value.filter((edge) => edge.targetRouteProcessId === routeProcessId)
      : []
  if (boundaryType === 'START' && removedIncomingEdges.length > 0) {
    routeEdges.value = routeEdges.value.filter((edge) => edge.targetRouteProcessId !== routeProcessId)
  }
  const nextSort =
    retainedBoundaryEdges.filter((edge) => edge.boundaryType === boundaryType).length + 1
  const edge: RouteFlowBoundaryEdgeVO = { boundaryType, routeProcessId, sort: nextSort }
  boundaryEdges.value = [...retainedBoundaryEdges, edge]
  selectedRouteProcessId.value = null
  selectedBoundaryType.value = boundaryType
  selectedEdgeKey.value = boundaryEdgeKey(edge)
  markGraphDraftChanged()
  syncFlowElements()
  if (removedIncomingEdges.length > 0) {
    const targetNode = findNode(routeProcessId)
    message.warning(
      `已将「${targetNode ? nodeLabel(targetNode) : routeProcessId}」入口调整为工序开始`
    )
  }
  return true
}

const handleEdgeUpdate = async (event: EdgeUpdateEvent) => {
  if (!canMutateRouteFlow.value || !event.connection.source || !event.connection.target) {
    syncFlowElements()
    return
  }
  if (
    isBoundaryNodeId(event.connection.source) ||
    isBoundaryNodeId(event.connection.target) ||
    isBoundaryEdgeId(event.edge.id)
  ) {
    syncFlowElements()
    return
  }
  const edgeAdded = addEdge(
    Number(event.connection.source),
    Number(event.connection.target),
    event.edge.id
  )
  if (!edgeAdded) return
  await applyAutoLayout({
    notify: false,
    focusRouteProcessId: Number(event.connection.source)
  })
}

const handleEdgeDelete = async (edge: RouteFlowEdgeVO) => {
  if (!canMutateRouteFlow.value) return
  const previousEdgeCount = routeEdges.value.length
  routeEdges.value = routeEdges.value.filter((item) => edgeKey(item) !== edgeKey(edge))
  if (routeEdges.value.length === previousEdgeCount) return
  selectedEdgeKey.value = ''
  markGraphDraftChanged()
  syncFlowElements()
  await applyAutoLayout({
    notify: false,
    focusRouteProcessId: edge.sourceRouteProcessId
  })
}

const handleBoundaryEdgeDelete = (edge: RouteFlowBoundaryEdgeVO) => {
  if (!canMutateRouteFlow.value) return
  boundaryEdges.value = boundaryEdges.value.filter(
    (item) => boundaryEdgeKey(item) !== boundaryEdgeKey(edge)
  )
  selectedEdgeKey.value = ''
  markGraphDraftChanged()
  syncFlowElements()
}

const handleSelectedEdgeDelete = async () => {
  if (!canMutateRouteFlow.value) return
  if (selectedBoundaryEdge.value) {
    handleBoundaryEdgeDelete(selectedBoundaryEdge.value)
    return
  }
  if (selectedEdge.value) {
    await handleEdgeDelete(selectedEdge.value)
  }
}

const handleNodesChange = (changes: NodeChange[]) => {
  if (!canMutateRouteFlow.value) return
  const removedRouteProcessIds = changes
    .flatMap((change) => {
      if (change.type !== 'remove' || !('id' in change)) return []
      return [Number(change.id)]
    })
    .filter(
      (routeProcessId) => Number.isFinite(routeProcessId) && !isBoundaryNodeId(routeProcessId)
    )
  removeRouteProcessesFromDraft(removedRouteProcessIds)
}

const isTextEntryTarget = (target: EventTarget | null) => {
  if (!(target instanceof HTMLElement)) return false
  return target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)
}

const isRouteProcessNodeEventTarget = (target: EventTarget | null) => {
  return (
    target instanceof HTMLElement && Boolean(target.closest('[data-flow-node="route-process"]'))
  )
}

const handleCanvasDeleteKeydown = async (event: KeyboardEvent) => {
  if (!canMutateRouteFlow.value || !['Delete', 'Backspace'].includes(event.key)) return
  if (isTextEntryTarget(event.target)) return
  if (isRouteProcessNodeEventTarget(event.target)) return
  if (selectedNode.value) {
    event.preventDefault()
    try {
      await confirmRemoveRouteProcessFromDraft(selectedNode.value)
    } catch (error) {
      if (isCancelError(error)) return
      throw error
    }
    return
  }
  if (selectedEdge.value) {
    event.preventDefault()
    await handleEdgeDelete(selectedEdge.value)
    return
  }
  if (selectedBoundaryEdge.value) {
    event.preventDefault()
    handleBoundaryEdgeDelete(selectedBoundaryEdge.value)
  }
}

const focusNode = (routeProcessId: number) => {
  const node = routeNodes.value.find((item) => item.routeProcessId === routeProcessId)
  if (!node) return
  setCenter((node.x || 0) + NODE_WIDTH / 2, (node.y || 0) + NODE_HEIGHT / 2, {
    zoom: 1,
    duration: 260
  })
}

const defaultNodePosition = (index: number) => {
  const row = Math.floor(index / MAX_VISIBLE_COLUMNS)
  const columnInRow = index % MAX_VISIBLE_COLUMNS
  const column = row % 2 === 0 ? columnInRow : MAX_VISIBLE_COLUMNS - 1 - columnInRow
  return {
    x: LAYOUT_LEFT_PADDING + column * COLUMN_GAP,
    y: LAYOUT_TOP_PADDING + row * ROW_GAP
  }
}

const syncRouteNodesFromFlow = () => {
  const positions = new Map(
    flowNodes.value
      .filter((node) => !isBoundaryNodeId(node.id))
      .map((node) => [
        Number(node.id),
        { x: Math.round(node.position.x), y: Math.round(node.position.y) }
      ])
  )
  routeNodes.value = routeNodes.value.map((node) => {
    const position = positions.get(node.routeProcessId)
    return position ? { ...node, x: position.x, y: position.y } : node
  })
}

const buildTopologicalOrder = (
  nodesById: Map<number, RouteFlowNodeVO>,
  incoming: Map<number, number[]>,
  outgoing: Map<number, number[]>,
  roots: number[],
  compareRouteProcessIds: (leftId: number, rightId: number) => number
) => {
  const remainingIncoming = new Map<number, number[]>()
  nodesById.forEach((_node, routeProcessId) => {
    remainingIncoming.set(routeProcessId, [...(incoming.get(routeProcessId) || [])])
  })
  const pending = roots.slice().sort(compareRouteProcessIds)
  const topologicalOrder: number[] = []
  while (pending.length > 0) {
    const routeProcessId = pending.shift()
    if (routeProcessId === undefined) continue
    topologicalOrder.push(routeProcessId)
    ;(outgoing.get(routeProcessId) || []).forEach((targetRouteProcessId) => {
      const nextIncoming = (remainingIncoming.get(targetRouteProcessId) || []).filter(
        (sourceRouteProcessId) => sourceRouteProcessId !== routeProcessId
      )
      remainingIncoming.set(targetRouteProcessId, nextIncoming)
      if (nextIncoming.length === 0) {
        pending.push(targetRouteProcessId)
        pending.sort(compareRouteProcessIds)
      }
    })
  }
  if (topologicalOrder.length !== nodesById.size) {
    throw new Error('关系图存在循环，无法自动布局')
  }
  return topologicalOrder
}

const resolveLayerYPositions = (
  layerRouteProcessIds: number[],
  yById: Map<number, number>,
  compareRouteProcessIds: (leftId: number, rightId: number) => number
) => {
  if (layerRouteProcessIds.length === 0) return
  const sorted = layerRouteProcessIds.slice().sort((leftId, rightId) => {
    const yDelta =
      (yById.get(leftId) || LAYOUT_TOP_PADDING) - (yById.get(rightId) || LAYOUT_TOP_PADDING)
    return Math.abs(yDelta) > 0.001 ? yDelta : compareRouteProcessIds(leftId, rightId)
  })
  const preferredAverage =
    sorted.reduce(
      (total, routeProcessId) => total + (yById.get(routeProcessId) || LAYOUT_TOP_PADDING),
      0
    ) / sorted.length
  let previousY = LAYOUT_TOP_PADDING - ROW_GAP
  sorted.forEach((routeProcessId) => {
    const nextY = Math.max(
      yById.get(routeProcessId) || LAYOUT_TOP_PADDING,
      previousY + ROW_GAP
    )
    yById.set(routeProcessId, nextY)
    previousY = nextY
  })
  const adjustedAverage =
    sorted.reduce(
      (total, routeProcessId) => total + (yById.get(routeProcessId) || LAYOUT_TOP_PADDING),
      0
    ) / sorted.length
  const shift = preferredAverage - adjustedAverage
  let minY = Number.POSITIVE_INFINITY
  sorted.forEach((routeProcessId) => {
    const shiftedY = (yById.get(routeProcessId) || LAYOUT_TOP_PADDING) + shift
    yById.set(routeProcessId, shiftedY)
    minY = Math.min(minY, shiftedY)
  })
  if (minY < LAYOUT_TOP_PADDING) {
    const offset = LAYOUT_TOP_PADDING - minY
    sorted.forEach((routeProcessId) => {
      yById.set(routeProcessId, (yById.get(routeProcessId) || LAYOUT_TOP_PADDING) + offset)
    })
  }
}

const resolveAutoLayoutRowCapacity = () => {
  const canvasHeight = graphCanvasRef.value?.clientHeight
  if (!canvasHeight || !Number.isFinite(canvasHeight)) {
    throw new Error('关系图画布高度不可用，无法自动布局')
  }
  const availableHeight = Math.max(0, canvasHeight - LAYOUT_TOP_PADDING - NODE_HEIGHT)
  return Math.max(1, Math.floor(availableHeight / ROW_GAP) + 1)
}

const buildHeightAwareLinearLayoutPositions = (
  topologicalOrder: number[],
  rowCapacity: number
) => {
  const positions = new Map<number, RouteFlowLayoutPosition>()
  const shouldWrapByHeight = topologicalOrder.length > rowCapacity
  topologicalOrder.forEach((routeProcessId, index) => {
    if (!shouldWrapByHeight) {
      positions.set(routeProcessId, {
        x: LAYOUT_LEFT_PADDING + index * COLUMN_GAP,
        y: LAYOUT_TOP_PADDING
      })
      return
    }
    const columnIndex = Math.floor(index / rowCapacity)
    const rowIndex = index % rowCapacity
    const resolvedRowIndex =
      columnIndex % 2 === 0 ? rowIndex : rowCapacity - 1 - rowIndex
    positions.set(routeProcessId, {
      x: LAYOUT_LEFT_PADDING + columnIndex * COLUMN_GAP,
      y: LAYOUT_TOP_PADDING + resolvedRowIndex * ROW_GAP
    })
  })
  return positions
}

const applyHeightAwareTailChainLayoutPositions = ({
  positions,
  incoming,
  outgoing,
  rowCapacity,
  compareRouteProcessIds
}: HeightAwareTailChainLayoutOptions) => {
  const positionedTailRouteProcessIds = new Set<number>()
  const maxTailY = LAYOUT_TOP_PADDING + (rowCapacity - 1) * ROW_GAP
  const anchorRouteProcessIds = Array.from(outgoing.keys()).sort(compareRouteProcessIds)
  anchorRouteProcessIds.forEach((anchorRouteProcessId) => {
    const parents = incoming.get(anchorRouteProcessId) || []
    const children = outgoing.get(anchorRouteProcessId) || []
    if (!(parents.length > 1 || children.length > 1)) return
    if (children.length !== 1) return
    const anchorPosition = positions.get(anchorRouteProcessId)
    if (!anchorPosition) return

    const chainRouteProcessIds: number[] = []
    const activeRouteProcessIds = new Set<number>([anchorRouteProcessId])
    let currentRouteProcessId: number | undefined = children[0]
    while (currentRouteProcessId !== undefined) {
      if (
        activeRouteProcessIds.has(currentRouteProcessId) ||
        positionedTailRouteProcessIds.has(currentRouteProcessId)
      ) {
        break
      }
      const currentParents = incoming.get(currentRouteProcessId) || []
      if (currentParents.length !== 1) break
      chainRouteProcessIds.push(currentRouteProcessId)
      activeRouteProcessIds.add(currentRouteProcessId)
      const currentChildren = outgoing.get(currentRouteProcessId) || []
      if (currentChildren.length !== 1) break
      currentRouteProcessId = currentChildren[0]
    }
    if (chainRouteProcessIds.length <= rowCapacity) return

    const wrappedPositions = buildHeightAwareLinearLayoutPositions(chainRouteProcessIds, rowCapacity)
    const wrappedYValues = Array.from(wrappedPositions.values()).map((position) => position.y)
    const minWrappedY = Math.min(...wrappedYValues)
    const maxWrappedY = Math.max(...wrappedYValues)
    const firstWrappedPosition = wrappedPositions.get(chainRouteProcessIds[0])
    let yOffset = anchorPosition.y - (firstWrappedPosition?.y || LAYOUT_TOP_PADDING)
    if (maxWrappedY + yOffset > maxTailY) {
      yOffset = maxTailY - maxWrappedY
    }
    if (minWrappedY + yOffset < LAYOUT_TOP_PADDING) {
      yOffset = LAYOUT_TOP_PADDING - minWrappedY
    }

    chainRouteProcessIds.forEach((routeProcessId) => {
      const position = wrappedPositions.get(routeProcessId)
      if (!position) return
      position.x = anchorPosition.x + COLUMN_GAP + (position.x - LAYOUT_LEFT_PADDING)
      position.y = Math.round(position.y + yOffset)
      positions.set(routeProcessId, position)
      positionedTailRouteProcessIds.add(routeProcessId)
    })
  })
  return positions
}

const buildMergedGraphLayoutPositions = (
  nodesById: Map<number, RouteFlowNodeVO>,
  incoming: Map<number, number[]>,
  outgoing: Map<number, number[]>,
  roots: number[],
  compareRouteProcessIds: (leftId: number, rightId: number) => number,
  rowCapacity: number
) => {
  const topologicalOrder = buildTopologicalOrder(
    nodesById,
    incoming,
    outgoing,
    roots,
    compareRouteProcessIds
  )
  const depthById = new Map<number, number>()
  nodesById.forEach((_node, routeProcessId) => depthById.set(routeProcessId, 0))
  topologicalOrder.forEach((routeProcessId) => {
    const sourceDepth = depthById.get(routeProcessId) || 0
    ;(outgoing.get(routeProcessId) || []).forEach((targetRouteProcessId) => {
      depthById.set(targetRouteProcessId, Math.max(depthById.get(targetRouteProcessId) || 0, sourceDepth + 1))
    })
  })
  const hasIndirectPathToTarget = (sourceRouteProcessId: number, targetRouteProcessId: number) => {
    const pending = (outgoing.get(sourceRouteProcessId) || []).filter(
      (nextRouteProcessId) => nextRouteProcessId !== targetRouteProcessId
    )
    const visited = new Set<number>()
    while (pending.length > 0) {
      const routeProcessId = pending.shift()
      if (routeProcessId === undefined || visited.has(routeProcessId)) continue
      if (routeProcessId === targetRouteProcessId) return true
      visited.add(routeProcessId)
      pending.push(...(outgoing.get(routeProcessId) || []))
    }
    return false
  }
  const enforceChildDepths = () => {
    let changed = false
    topologicalOrder.forEach((routeProcessId) => {
      const sourceDepth = depthById.get(routeProcessId) || 0
      ;(outgoing.get(routeProcessId) || []).forEach((targetRouteProcessId) => {
        const expectedTargetDepth = sourceDepth + 1
        if ((depthById.get(targetRouteProcessId) || 0) < expectedTargetDepth) {
          depthById.set(targetRouteProcessId, expectedTargetDepth)
          changed = true
        }
      })
    })
    return changed
  }
  const alignMergeParentDepths = () => {
    let changed = false
    topologicalOrder.forEach((routeProcessId) => {
      const parents = incoming.get(routeProcessId) || []
      if (parents.length <= 1) return
      const targetDepth = depthById.get(routeProcessId) || 0
      parents.forEach((parentRouteProcessId) => {
        if (hasIndirectPathToTarget(parentRouteProcessId, routeProcessId)) return
        if ((depthById.get(parentRouteProcessId) || 0) < targetDepth - 1) {
          depthById.set(parentRouteProcessId, targetDepth - 1)
          changed = true
        }
      })
    })
    return changed
  }
  for (let index = 0; index < nodesById.size * nodesById.size; index += 1) {
    const alignedMergeParents = alignMergeParentDepths()
    const enforcedChildDepths = enforceChildDepths()
    if (!alignedMergeParents && !enforcedChildDepths) break
    if (index === nodesById.size * nodesById.size - 1) {
      throw new Error('关系图层级无法稳定，无法自动布局')
    }
  }

  const layers = new Map<number, number[]>()
  topologicalOrder.forEach((routeProcessId) => {
    const depth = depthById.get(routeProcessId) || 0
    const layer = layers.get(depth) || []
    layer.push(routeProcessId)
    layers.set(depth, layer)
  })
  layers.forEach((layerRouteProcessIds) => layerRouteProcessIds.sort(compareRouteProcessIds))
  const layerDepths = Array.from(layers.keys()).sort((left, right) => left - right)
  const yById = new Map<number, number>()
  layers.forEach((layerRouteProcessIds) => {
    layerRouteProcessIds.forEach((routeProcessId, index) => {
      yById.set(routeProcessId, LAYOUT_TOP_PADDING + index * ROW_GAP)
    })
  })

  const averageY = (routeProcessIds: number[]) => {
    return (
      routeProcessIds.reduce(
        (total, routeProcessId) => total + (yById.get(routeProcessId) || LAYOUT_TOP_PADDING),
        0
      ) / routeProcessIds.length
    )
  }
  const hasMultipleParents = (routeProcessId: number) =>
    (incoming.get(routeProcessId) || []).length > 1
  const alignMergeNodesToParents = () => {
    layerDepths.forEach((depth) => {
      const layerRouteProcessIds = layers.get(depth) || []
      layerRouteProcessIds.forEach((routeProcessId) => {
        const parents = incoming.get(routeProcessId) || []
        if (parents.length > 1) {
          yById.set(routeProcessId, averageY(parents))
        }
      })
      resolveLayerYPositions(layerRouteProcessIds, yById, compareRouteProcessIds)
    })
  }
  const alignParentsToChildren = () => {
    layerDepths
      .slice()
      .reverse()
      .forEach((depth) => {
        const layerRouteProcessIds = layers.get(depth) || []
        layerRouteProcessIds.forEach((routeProcessId) => {
          const children = outgoing.get(routeProcessId) || []
          if (children.length > 0 && !hasMultipleParents(routeProcessId)) {
            yById.set(routeProcessId, averageY(children))
          }
        })
        resolveLayerYPositions(layerRouteProcessIds, yById, compareRouteProcessIds)
      })
  }
  alignParentsToChildren()
  alignMergeNodesToParents()
  alignParentsToChildren()
  alignMergeNodesToParents()

  const positions = new Map<number, RouteFlowLayoutPosition>()
  nodesById.forEach((_node, routeProcessId) => {
    positions.set(routeProcessId, {
      x: LAYOUT_LEFT_PADDING + (depthById.get(routeProcessId) || 0) * COLUMN_GAP,
      y: Math.round(yById.get(routeProcessId) || LAYOUT_TOP_PADDING)
    })
  })
  applyHeightAwareTailChainLayoutPositions({
    positions,
    incoming,
    outgoing,
    rowCapacity,
    compareRouteProcessIds
  })
  return positions
}

const buildBranchLayoutPositions = () => {
  const rowCapacity = resolveAutoLayoutRowCapacity()
  const nodesById = new Map(routeNodes.value.map((node) => [node.routeProcessId, node] as const))
  const startBoundaryRootIds = collectStartBoundaryRootIds()
  const incoming = new Map<number, number[]>()
  const outgoing = new Map<number, number[]>()
  routeNodes.value.forEach((node) => {
    incoming.set(node.routeProcessId, [])
    outgoing.set(node.routeProcessId, [])
  })
  routeEdges.value.forEach((edge) => {
    if (!nodesById.has(edge.sourceRouteProcessId) || !nodesById.has(edge.targetRouteProcessId)) {
      throw new Error('关系图存在无效连接，无法自动布局')
    }
    if (startBoundaryRootIds.has(edge.targetRouteProcessId)) {
      return
    }
    incoming.get(edge.targetRouteProcessId)!.push(edge.sourceRouteProcessId)
    outgoing.get(edge.sourceRouteProcessId)!.push(edge.targetRouteProcessId)
  })

  const compareRouteProcessIds = (leftId: number, rightId: number) => {
    const left = nodesById.get(leftId)
    const right = nodesById.get(rightId)
    return (left?.sort || 0) - (right?.sort || 0) || leftId - rightId
  }
  outgoing.forEach((targetIds) => targetIds.sort(compareRouteProcessIds))

  const roots = routeNodes.value
    .filter((node) => (incoming.get(node.routeProcessId) || []).length === 0)
    .map((node) => node.routeProcessId)
    .sort(compareRouteProcessIds)
  const hasMergeTargets = Array.from(incoming.values()).some((sourceIds) => sourceIds.length > 1)
  if (hasMergeTargets) {
    return buildMergedGraphLayoutPositions(
      nodesById,
      incoming,
      outgoing,
      roots,
      compareRouteProcessIds,
      rowCapacity
    )
  }
  const hasTreeBranches =
    roots.length > 1 || Array.from(outgoing.values()).some((targetIds) => targetIds.length > 1)
  if (!hasTreeBranches) {
    const topologicalOrder = buildTopologicalOrder(
      nodesById,
      incoming,
      outgoing,
      roots,
      compareRouteProcessIds
    )
    return buildHeightAwareLinearLayoutPositions(topologicalOrder, rowCapacity)
  }
  const positions = new Map<number, RouteFlowLayoutPosition>()
  const activeRouteProcessIds = new Set<number>()
  const visitedRouteProcessIds = new Set<number>()
  let nextLeafRow = 0

  const placeSubtree = (routeProcessId: number, depth: number): number => {
    if (activeRouteProcessIds.has(routeProcessId)) {
      throw new Error('关系图存在循环，无法自动布局')
    }
    if (visitedRouteProcessIds.has(routeProcessId)) {
      throw new Error('关系图存在重复路径，无法自动布局')
    }
    activeRouteProcessIds.add(routeProcessId)

    const children = outgoing.get(routeProcessId) || []
    let y: number
    if (children.length === 0) {
      y = LAYOUT_TOP_PADDING + nextLeafRow * ROW_GAP
      nextLeafRow += 1
    } else {
      const childYPositions = children.map((childRouteProcessId) =>
        placeSubtree(childRouteProcessId, depth + 1)
      )
      const firstChildY = childYPositions[0]
      const lastChildY = childYPositions[childYPositions.length - 1]
      y = (firstChildY + lastChildY) / 2
    }

    activeRouteProcessIds.delete(routeProcessId)
    visitedRouteProcessIds.add(routeProcessId)
    positions.set(routeProcessId, {
      x: LAYOUT_LEFT_PADDING + depth * COLUMN_GAP,
      y: Math.round(y)
    })
    return y
  }

  roots.forEach((rootRouteProcessId, rootIndex) => {
    placeSubtree(rootRouteProcessId, 0)
    if (rootIndex < roots.length - 1) {
      nextLeafRow += 1
    }
  })

  if (visitedRouteProcessIds.size !== routeNodes.value.length) {
    throw new Error('关系图存在循环，无法自动布局')
  }
  applyHeightAwareTailChainLayoutPositions({
    positions,
    incoming,
    outgoing,
    rowCapacity,
    compareRouteProcessIds
  })
  return positions
}

const createBoundaryFlowNodes = (): RouteFlowVueNode[] => {
  if (routeNodes.value.length === 0) return []
  const { entryRouteProcessIds, terminalRouteProcessIds } = findBoundaryRouteProcessIds()
  const minX = Math.min(...routeNodes.value.map((node) => node.x || 0))
  const maxX = Math.max(...routeNodes.value.map((node) => node.x || 0))
  const startNodes = entryRouteProcessIds
    .map(findNode)
    .filter((node): node is RouteFlowNodeVO => Boolean(node))
  const terminalBoundaryNode = resolveTerminalBoundaryNode(terminalRouteProcessIds)
  const startBoundaryX = startNodes.length
    ? Math.min(...startNodes.map((node) => node.x || 0)) - COLUMN_GAP
    : minX - COLUMN_GAP
  const endBoundaryX = terminalBoundaryNode
    ? (terminalBoundaryNode.x || 0) + COLUMN_GAP
    : maxX + COLUMN_GAP
  return [
    {
      id: PROCESS_START_NODE_ID,
      type: 'route-boundary',
      position: {
        x: Math.round(startBoundaryX),
        y: resolveBoundaryY(startNodes)
      },
      data: { boundaryType: 'START', label: '工序开始' },
      selectable: true,
      draggable: false,
      connectable: canMutateRouteFlow.value,
      width: BOUNDARY_NODE_WIDTH,
      height: BOUNDARY_NODE_HEIGHT
    },
    {
      id: PROCESS_END_NODE_ID,
      type: 'route-boundary',
      position: {
        x: Math.round(endBoundaryX),
        y: terminalBoundaryNode ? resolveBoundaryY([terminalBoundaryNode]) : 72
      },
      data: { boundaryType: 'END', label: '工序结束' },
      selectable: true,
      draggable: false,
      connectable: canMutateRouteFlow.value,
      width: BOUNDARY_NODE_WIDTH,
      height: BOUNDARY_NODE_HEIGHT
    }
  ]
}

const createBoundaryFlowEdges = (): RouteFlowVueEdge[] => {
  if (routeNodes.value.length === 0) return []
  return boundaryEdges.value.filter(isActiveBoundaryEdge).map((boundaryEdge) => {
    const startBoundary = boundaryEdge.boundaryType === 'START'
    const id = boundaryEdgeKey(boundaryEdge)
    return {
      id,
      source: startBoundary ? PROCESS_START_NODE_ID : String(boundaryEdge.routeProcessId),
      target: startBoundary ? String(boundaryEdge.routeProcessId) : PROCESS_END_NODE_ID,
      sourceHandle: 'source-right',
      targetHandle: 'target-left',
      type: 'smoothstep',
      markerEnd: MarkerType.ArrowClosed,
      data: { boundaryEdge },
      selectable: true,
      animated: false,
      style: boundaryEdgeStyle(id)
    }
  })
}

const findBoundaryRouteProcessIds = () => {
  const entryRouteProcessIds = boundaryEdges.value
    .filter(isActiveBoundaryEdge)
    .filter((edge) => edge.boundaryType === 'START')
    .sort((left, right) => (left.sort || 0) - (right.sort || 0))
    .map((edge) => edge.routeProcessId)
  const terminalRouteProcessIds = boundaryEdges.value
    .filter(isActiveBoundaryEdge)
    .filter((edge) => edge.boundaryType === 'END')
    .sort((left, right) => (left.sort || 0) - (right.sort || 0))
    .map((edge) => edge.routeProcessId)
  return {
    entryRouteProcessIds,
    terminalRouteProcessIds
  }
}

const resolveTerminalBoundaryNode = (terminalRouteProcessIds: number[]) => {
  const terminalNodes = terminalRouteProcessIds
    .map(findNode)
    .filter((node): node is RouteFlowNodeVO => Boolean(node))
    .sort((left, right) => {
      const sortResult = (left.sort || 0) - (right.sort || 0)
      return sortResult !== 0 ? sortResult : left.routeProcessId - right.routeProcessId
    })
  return terminalNodes[terminalNodes.length - 1]
}

const resolveBoundaryY = (nodes: RouteFlowNodeVO[]) => {
  if (nodes.length === 0) return 72
  const centerY =
    nodes.reduce((total, node) => total + (node.y || 0) + NODE_HEIGHT / 2, 0) / nodes.length
  return Math.round(centerY - BOUNDARY_NODE_HEIGHT / 2)
}

const boundaryEdgeStyle = (id: string) => ({
  stroke: selectedEdgeKey.value === id ? '#f56c6c' : '#7aa7df',
  strokeWidth: selectedEdgeKey.value === id ? 3 : 1.8,
  strokeDasharray: '6 4'
})

const isBoundaryNodeId = (nodeId: string | number) => {
  return [PROCESS_START_NODE_ID, PROCESS_END_NODE_ID].includes(String(nodeId))
}

const isBoundaryEdgeId = (edgeId: string | number) => {
  const id = String(edgeId)
  return id.startsWith(`${PROCESS_START_NODE_ID}->`) || id.endsWith(`->${PROCESS_END_NODE_ID}`)
}

const isDraftRouteProcessId = (routeProcessId: number) => {
  return routeProcessId < 0
}

const isActiveRouteProcessId = (routeProcessId: number) => {
  return !pendingDeletedRouteProcessIds.value.has(Number(routeProcessId))
}

const isActiveRouteNode = (node: RouteFlowNodeVO) => {
  return isActiveRouteProcessId(node.routeProcessId)
}

const isActiveRouteEdge = (edge: RouteFlowEdgeVO) => {
  return (
    isActiveRouteProcessId(edge.sourceRouteProcessId) &&
    isActiveRouteProcessId(edge.targetRouteProcessId)
  )
}

const isActiveBoundaryEdge = (edge: RouteFlowBoundaryEdgeVO) => {
  return isActiveRouteProcessId(edge.routeProcessId)
}

const isPendingDeletedFlowNode = (node: RouteFlowVueNode) => {
  const routeProcessId = Number(node.id)
  return Number.isFinite(routeProcessId) && pendingDeletedRouteProcessIds.value.has(routeProcessId)
}

const edgeKey = (edge: RouteFlowEdgeVO) => {
  return `${edge.sourceRouteProcessId}->${edge.targetRouteProcessId}`
}

const boundaryEdgeKey = (edge: RouteFlowBoundaryEdgeVO) => {
  return edge.boundaryType === 'START'
    ? `${PROCESS_START_NODE_ID}->${edge.routeProcessId}`
    : `${edge.routeProcessId}->${PROCESS_END_NODE_ID}`
}

const boundaryLabel = (boundaryType: RouteFlowBoundaryType) => {
  return boundaryType === 'START' ? '工序开始' : '工序结束'
}

const boundaryEdgeSourceLabel = (edge: RouteFlowBoundaryEdgeVO) => {
  return edge.boundaryType === 'START'
    ? boundaryLabel('START')
    : nodeLabel(findNode(edge.routeProcessId))
}

const boundaryEdgeTargetLabel = (edge: RouteFlowBoundaryEdgeVO) => {
  return edge.boundaryType === 'END'
    ? boundaryLabel('END')
    : nodeLabel(findNode(edge.routeProcessId))
}

const nodeLabel = (node?: ConnectionProcessOption) => {
  if (!node) return '-'
  return node.processName || node.processCode || `工序 ${node.routeProcessId}`
}

const formatProcessDetailText = (value?: string | number | boolean | null) => {
  if (value === undefined || value === null || value === '') return '-'
  if (value === true) return '是'
  if (value === false) return '否'
  return String(value)
}

const findNode = (routeProcessId: number) => {
  return routeNodes.value.find((node) => node.routeProcessId === routeProcessId)
}

const resolveErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

const formatUserOptionLabel = (user: UserVO) => {
  if (user.nickname && user.username) return `${user.nickname}（${user.username}）`
  return user.nickname || user.username || String(user.id)
}

const formatRoleOptionLabel = (role: RoleVO) => {
  if (role.name && role.code) return `${role.name}（${role.code}）`
  return role.name || role.code || String(role.id)
}

const normalizeReleaseApprovalRuleCandidateSourceType = (
  candidateSourceType?: string
): EdhrWorkTaskReleaseApprovalCandidateSourceType => {
  if (!candidateSourceType || candidateSourceType === 'USER') return 'USER'
  if (candidateSourceType === 'ROLE_GROUP') return 'ROLE_GROUP'
  throw new Error(`放行责任人候选类型不支持：${candidateSourceType}`)
}

const resolveReleaseApprovalRuleCandidateId = (
  rule?: EdhrWorkTaskAssignmentRuleRespVO | null
) => {
  if (!rule) return undefined
  if (normalizeReleaseApprovalRuleCandidateSourceType(rule.candidateSourceType) === 'ROLE_GROUP') {
    return rule.candidateSourceId
  }
  return rule.candidateSourceId || rule.assigneeUserId
}

const resetReleaseApprovalRuleForm = () => {
  currentReleaseApprovalRule.value = null
  releaseApprovalRuleForm.candidateSourceType = 'USER'
  releaseApprovalRuleForm.candidateSourceId = undefined
  releaseApprovalRuleForm.enabled = true
  releaseApprovalRuleForm.remark = ''
}

const resetBatchRecordAttachmentOwners = () => {
  batchRecordAttachmentOwnersLoaded.value = false
  batchRecordAttachmentOwnersLoadError.value = ''
  batchRecordAttachmentOwners.value = []
}

const fillReleaseApprovalRuleForm = (rule?: EdhrWorkTaskAssignmentRuleRespVO | null) => {
  currentReleaseApprovalRule.value = rule || null
  releaseApprovalRuleForm.candidateSourceType = normalizeReleaseApprovalRuleCandidateSourceType(
    rule?.candidateSourceType
  )
  releaseApprovalRuleForm.candidateSourceId = resolveReleaseApprovalRuleCandidateId(rule)
  releaseApprovalRuleForm.enabled = typeof rule?.enabled === 'boolean' ? rule.enabled : true
  releaseApprovalRuleForm.remark = rule?.remark || ''
}

const loadReleaseApprovalRuleUserOptions = async () => {
  if (releaseApprovalRuleUserOptions.value.length > 0) return
  releaseApprovalRuleUserOptionsLoading.value = true
  try {
    releaseApprovalRuleUserOptions.value = await getSimpleUserList()
  } catch (error) {
    const errorMessage = resolveErrorMessage(error, '放行责任人用户列表加载失败。')
    releaseApprovalRuleLoadError.value = errorMessage
    message.error(errorMessage)
  } finally {
    releaseApprovalRuleUserOptionsLoading.value = false
  }
}

const loadReleaseApprovalRuleRoleOptions = async () => {
  if (releaseApprovalRuleRoleOptions.value.length > 0) return
  releaseApprovalRuleRoleOptionsLoading.value = true
  try {
    releaseApprovalRuleRoleOptions.value = await getSimpleRoleList()
  } catch (error) {
    const errorMessage = resolveErrorMessage(error, '放行责任人角色列表加载失败。')
    releaseApprovalRuleLoadError.value = errorMessage
    message.error(errorMessage)
  } finally {
    releaseApprovalRuleRoleOptionsLoading.value = false
  }
}

const loadReleaseApprovalRuleCandidateOptions = async () => {
  if (releaseApprovalRuleForm.candidateSourceType === 'ROLE_GROUP') {
    await loadReleaseApprovalRuleRoleOptions()
    return
  }
  await loadReleaseApprovalRuleUserOptions()
}

const loadReleaseApprovalRuleDetail = async () => {
  if (!props.routeId) {
    releaseApprovalRuleLoadError.value = '请先保存工艺路线，再配置放行责任人。'
    return
  }
  releaseApprovalRuleLoading.value = true
  releaseApprovalRuleLoadError.value = ''
  try {
    const [rule] = await Promise.all([
      getEdhrRouteReleaseApprovalRule(props.routeId),
      loadReleaseApprovalRuleUserOptions(),
      loadReleaseApprovalRuleRoleOptions()
    ])
    fillReleaseApprovalRuleForm(rule || null)
    releaseApprovalRuleLoaded.value = true
  } catch (error) {
    resetReleaseApprovalRuleForm()
    const errorMessage = resolveErrorMessage(error, '放行责任人规则加载失败。')
    releaseApprovalRuleLoadError.value = errorMessage
    message.error(errorMessage)
  } finally {
    releaseApprovalRuleLoading.value = false
  }
}

const normalizeBatchRecordAttachmentOwnerCandidateSourceType = (
  candidateSourceType?: string | null
): EdhrProcessFormCandidateSourceType => {
  return normalizeRecordBindingCandidateSourceType(candidateSourceType) || 'ROLE'
}

const normalizeBatchRecordAttachmentOwner = (
  owner: ProRouteBatchRecordAttachmentOwnerVO
): BatchRecordAttachmentOwnerDraft => ({
  ...owner,
  sort: Number(owner.sort || BATCH_RECORD_ATTACHMENT_SORT_BY_CODE.get(owner.attachmentCode) || 0),
  candidateSourceType: normalizeBatchRecordAttachmentOwnerCandidateSourceType(owner.candidateSourceType),
  candidateSourceIds: normalizeRecordBindingCandidateIds(owner.candidateSourceIds),
  candidateSourceNames: normalizeRecordBindingCandidateNames(owner.candidateSourceNames),
  assignedUserIds: normalizeRecordBindingCandidateIds(owner.assignedUserIds),
  assignedUserNames: normalizeRecordBindingCandidateNames(owner.assignedUserNames)
})

const normalizeBatchRecordAttachmentOwners = (owners: ProRouteBatchRecordAttachmentOwnerVO[]) =>
  owners.map(normalizeBatchRecordAttachmentOwner).sort((first, second) => first.sort - second.sort)

const resolveBatchRecordAttachmentOwnerReadRouteVersionId = () =>
  props.routeVersionEditContext?.lifecycleStatus === 'ACTIVE'
    ? undefined
    : props.routeVersionEditContext?.routeVersionId

const loadBatchRecordAttachmentOwnerCandidateOptions = async (
  owner: Pick<BatchRecordAttachmentOwnerDraft, 'candidateSourceType'>
) => {
  if (owner.candidateSourceType === 'ROLE') {
    await loadRecordBindingRoleOptions()
    return
  }
  await loadRecordBindingUserOptions()
}

const loadBatchRecordAttachmentOwners = async (force = false) => {
  if (batchRecordAttachmentOwnersLoaded.value && !force) return
  if (!props.routeId) {
    batchRecordAttachmentOwnersLoadError.value = '请先保存工艺路线，再配置批记录附件负责人。'
    return
  }
  batchRecordAttachmentOwnersLoading.value = true
  batchRecordAttachmentOwnersLoadError.value = ''
  try {
    const [owners] = await Promise.all([
      ProRouteFlowConfigApi.getBatchRecordAttachmentOwners(
        props.routeId,
        resolveBatchRecordAttachmentOwnerReadRouteVersionId()
      ),
      loadRecordBindingUserOptions(),
      loadRecordBindingRoleOptions()
    ])
    batchRecordAttachmentOwners.value = normalizeBatchRecordAttachmentOwners(owners)
    batchRecordAttachmentOwnersLoaded.value = true
  } catch (error) {
    batchRecordAttachmentOwners.value = []
    const errorMessage = resolveErrorMessage(error, '批记录附件负责人加载失败。')
    batchRecordAttachmentOwnersLoadError.value = errorMessage
    message.error(errorMessage)
  } finally {
    batchRecordAttachmentOwnersLoading.value = false
  }
}

const isBatchRecordAttachmentOwnerCandidateOptionsLoading = (
  owner: Pick<BatchRecordAttachmentOwnerDraft, 'candidateSourceType'>
) =>
  owner.candidateSourceType === 'ROLE'
    ? recordBindingRoleOptionsLoading.value
    : recordBindingUserOptionsLoading.value

const buildBatchRecordAttachmentOwnerCandidateOptions = (
  owner: BatchRecordAttachmentOwnerDraft
): RecordBindingCandidateOption[] => {
  const baseOptions =
    owner.candidateSourceType === 'ROLE'
      ? recordBindingRoleOptions.value.map((role) => ({
          label: formatRoleOptionLabel(role),
          value: role.id
        }))
      : recordBindingUserOptions.value.map((user) => ({
          label: formatUserOptionLabel(user),
          value: user.id
        }))
  const optionById = new Map(baseOptions.map((option) => [Number(option.value), option]))
  owner.candidateSourceIds.forEach((id, index) => {
    if (optionById.has(Number(id))) return
    optionById.set(Number(id), {
      label: owner.candidateSourceNames[index] || String(id),
      value: id
    })
  })
  return Array.from(optionById.values())
}

const formatBatchRecordAttachmentAssignedUsers = (owner: BatchRecordAttachmentOwnerDraft) => {
  const assignedNames = normalizeRecordBindingCandidateNames(owner.assignedUserNames)
  if (assignedNames.length > 0) return assignedNames.join('、')
  const assignedIds = normalizeRecordBindingCandidateIds(owner.assignedUserIds)
  return assignedIds.length > 0 ? assignedIds.join('、') : '待初始化'
}

const handleBatchRecordAttachmentOwnerSourceTypeChange = (
  owner: BatchRecordAttachmentOwnerDraft,
  candidateSourceType: string
) => {
  if (batchRecordAttachmentOwnerControlsDisabled.value) return
  owner.candidateSourceType = normalizeBatchRecordAttachmentOwnerCandidateSourceType(candidateSourceType)
  owner.candidateSourceIds = []
  owner.candidateSourceNames = []
  void loadBatchRecordAttachmentOwnerCandidateOptions(owner)
}

const handleBatchRecordAttachmentOwnerCandidateIdsChange = (
  owner: BatchRecordAttachmentOwnerDraft,
  candidateSourceIds?: Array<number | string>
) => {
  if (batchRecordAttachmentOwnerControlsDisabled.value) return
  const ids = normalizeRecordBindingCandidateIds(candidateSourceIds)
  const options = buildBatchRecordAttachmentOwnerCandidateOptions(owner)
  owner.candidateSourceIds = ids
  owner.candidateSourceNames = ids.map(
    (id) => options.find((option) => Number(option.value) === Number(id))?.label || String(id)
  )
}

const handleBatchRecordAttachmentOwnerInit = async () => {
  try {
    if (!props.routeId) {
      throw new Error('请先保存工艺路线，再初始化批记录附件负责人。')
    }
    const routeVersionId = requireCandidateRouteVersionId('批记录附件负责人初始化')
    batchRecordAttachmentOwnersInitializing.value = true
    batchRecordAttachmentOwnersLoadError.value = ''
    const owners = await ProRouteFlowConfigApi.initBatchRecordAttachmentOwners({
      routeId: props.routeId,
      routeVersionId
    })
    batchRecordAttachmentOwners.value = normalizeBatchRecordAttachmentOwners(owners)
    batchRecordAttachmentOwnersLoaded.value = true
    message.success('批记录附件默认角色已初始化')
  } catch (error) {
    const errorMessage = resolveErrorMessage(error, '批记录附件默认角色初始化失败。')
    batchRecordAttachmentOwnersLoadError.value = errorMessage
    message.error(errorMessage)
  } finally {
    batchRecordAttachmentOwnersInitializing.value = false
  }
}

const handleBatchRecordAttachmentOwnerSave = async () => {
  try {
    if (!props.routeId) {
      throw new Error('请先保存工艺路线，再保存批记录附件负责人。')
    }
    const routeVersionId = requireCandidateRouteVersionId('批记录附件负责人保存')
    const invalidOwner = batchRecordAttachmentOwners.value.find(
      (owner) => owner.candidateSourceIds.length === 0
    )
    if (invalidOwner) {
      throw new Error(`请先选择${invalidOwner.attachmentName}负责人。`)
    }
    batchRecordAttachmentOwnersSaving.value = true
    batchRecordAttachmentOwnersLoadError.value = ''
    await ProRouteFlowConfigApi.saveBatchRecordAttachmentOwners({
      routeId: props.routeId,
      routeVersionId,
      items: batchRecordAttachmentOwners.value.map((owner) => ({
        attachmentCode: owner.attachmentCode,
        candidateSourceType: owner.candidateSourceType,
        candidateSourceIds: owner.candidateSourceIds,
        candidateSourceNames: owner.candidateSourceNames,
        remark: owner.remark || null
      }))
    })
    message.success('批记录附件负责人已保存')
    await loadBatchRecordAttachmentOwners(true)
  } catch (error) {
    const errorMessage = resolveErrorMessage(error, '批记录附件负责人保存失败。')
    batchRecordAttachmentOwnersLoadError.value = errorMessage
    message.error(errorMessage)
  } finally {
    batchRecordAttachmentOwnersSaving.value = false
  }
}

const handleSelectBoundaryDetailField = (fieldKey: BoundaryDetailFieldKey) => {
  selectedBoundaryDetailFieldKey.value = fieldKey
  if (fieldKey === 'releaseOwner' && !releaseApprovalRuleLoaded.value) {
    void loadReleaseApprovalRuleDetail()
  }
  if (fieldKey === 'batchRecordAttachment' && !batchRecordAttachmentOwnersLoaded.value) {
    void loadBatchRecordAttachmentOwners()
  }
}

const releaseApprovalRuleControlsDisabled = computed(
  () =>
    routeFlowWriteControlsDisabled.value ||
    releaseApprovalRuleLoading.value ||
    releaseApprovalRuleSaving.value ||
    !props.routeId
)

const batchRecordAttachmentOwnerControlsDisabled = computed(
  () =>
    routeFlowWriteControlsDisabled.value ||
    batchRecordAttachmentOwnersLoading.value ||
    batchRecordAttachmentOwnersSaving.value ||
    batchRecordAttachmentOwnersInitializing.value ||
    !props.routeId ||
    !isDraftCandidateEdit.value
)

const releaseApprovalRuleCandidateOptionsLoading = computed(() =>
  releaseApprovalRuleForm.candidateSourceType === 'ROLE_GROUP'
    ? releaseApprovalRuleRoleOptionsLoading.value
    : releaseApprovalRuleUserOptionsLoading.value
)

const releaseApprovalRuleCandidateOptions = computed<ReleaseApprovalRuleCandidateOption[]>(() =>
  releaseApprovalRuleForm.candidateSourceType === 'ROLE_GROUP'
    ? releaseApprovalRuleRoleOptions.value.map((role) => ({
        label: formatRoleOptionLabel(role),
        value: role.id
      }))
    : releaseApprovalRuleUserOptions.value.map((user) => ({
        label: formatUserOptionLabel(user),
        value: user.id
      }))
)

const handleReleaseApprovalRuleSourceTypeChange = (candidateSourceType: string | number | boolean) => {
  releaseApprovalRuleForm.candidateSourceType = normalizeReleaseApprovalRuleCandidateSourceType(
    String(candidateSourceType)
  )
  releaseApprovalRuleForm.candidateSourceId = undefined
  void loadReleaseApprovalRuleCandidateOptions()
}

const handleReleaseApprovalRuleSave = async () => {
  if (!props.routeId) {
    releaseApprovalRuleLoadError.value = '请先保存工艺路线，再配置放行责任人。'
    return
  }
  if (!releaseApprovalRuleForm.candidateSourceId) {
    releaseApprovalRuleLoadError.value = '请选择放行责任人。'
    return
  }
  releaseApprovalRuleSaving.value = true
  releaseApprovalRuleLoadError.value = ''
  try {
    const savedRule = await saveEdhrRouteReleaseApprovalRule({
      routeId: props.routeId,
      candidateSourceType: releaseApprovalRuleForm.candidateSourceType,
      candidateSourceId: releaseApprovalRuleForm.candidateSourceId,
      enabled: releaseApprovalRuleForm.enabled,
      remark: releaseApprovalRuleForm.remark?.trim() || '流转关系图工序结束放行责任人'
    })
    fillReleaseApprovalRuleForm(savedRule)
    releaseApprovalRuleLoaded.value = true
    message.success('放行责任人已保存')
  } catch (error) {
    const errorMessage = resolveErrorMessage(error, '放行责任人保存失败。')
    releaseApprovalRuleLoadError.value = errorMessage
    message.error(errorMessage)
  } finally {
    releaseApprovalRuleSaving.value = false
  }
}

const formatProcessOption = (process: ProProcessVO) => {
  return [process.code, process.name].filter(Boolean).join(' / ') || String(process.id)
}

const formatConnectionOption = (node: ConnectionProcessOption) => {
  return node.processName || ''
}

const isCancelError = (error: unknown) => {
  const messageText = error instanceof Error ? error.message : String(error)
  return messageText === 'cancel' || messageText === 'close'
}

const handleBeforeUnload = (event: BeforeUnloadEvent) => {
  if (!hasWorkspaceDraftChanges()) return
  event.preventDefault()
  event.returnValue = ''
}

watch(selectedRouteProcessId, () => {
  void loadSelectedProcessDetail(selectedNode.value)
})

watch(selectedProcessDetailFieldKeys, (fieldKeys) => {
  if (
    selectedProcessDetailFieldKey.value &&
    !fieldKeys.includes(selectedProcessDetailFieldKey.value)
  ) {
    if (!processDetailInterestSaving.value) {
      clearRouteFlowLastSelectionDetailField(selectedProcessDetailFieldKey.value)
    }
    selectedProcessDetailFieldKey.value = undefined
  }
})

watch(
  () => route.query.capacitySourceFocus,
  () => {
    void focusProcessDetailFieldsForCapacitySource()
  }
)

watch(
  () => [
    normalizeRouteQueryText(route.query.capacityOverride),
    props.routeVersionEditContext?.routeVersionId,
    props.routeVersionEditContext?.lifecycleStatus,
    selectedProcessAttributesLoading.value,
    selectedProcessAttributes.routeProcessId
  ],
  () => {
    void tryOpenCapacityOverrideFromRouteQuery()
  },
  { flush: 'post' }
)

watch(
  () => [props.routeId, props.routeVersionEditContext?.routeVersionId],
  () => {
    resetBatchRecordAttachmentOwners()
    if (props.routeId) {
      loadGraph()
      if (
        selectedBoundaryType.value === 'START' &&
        selectedBoundaryDetailFieldKey.value === 'batchRecordAttachment'
      ) {
        void loadBatchRecordAttachmentOwners()
      }
    }
  },
  { immediate: true }
)

const handleRouteProcessSettingColumnConfigChanged = () => {
  void loadRouteProcessSettingColumnConfig()
}

onMounted(() => {
  void loadProcessDetailFieldConfig()
  window.addEventListener('keydown', handleCanvasDeleteKeydown)
  window.addEventListener('keydown', handleRouteFlowMaximizeKeydown)
  window.addEventListener('beforeunload', handleBeforeUnload)
  window.addEventListener(
    ROUTE_PROCESS_SETTINGS_COLUMN_CONFIG_CHANGED_EVENT,
    handleRouteProcessSettingColumnConfigChanged
  )
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleCanvasDeleteKeydown)
  window.removeEventListener('keydown', handleRouteFlowMaximizeKeydown)
  window.removeEventListener('beforeunload', handleBeforeUnload)
  window.removeEventListener(
    ROUTE_PROCESS_SETTINGS_COLUMN_CONFIG_CHANGED_EVENT,
    handleRouteProcessSettingColumnConfigChanged
  )
})

defineExpose({
  autoLayoutOnEntry,
  validateBeforeSubmit,
  saveFromParent,
  hasWorkspaceDraftChanges,
  discardWorkspaceDraftChanges
})
</script>

<style scoped>
.route-flow-graph-designer {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: calc(100vh - 210px);
  min-height: 480px;
  max-height: none;
  overflow: hidden;
}

.route-flow-graph-designer.is-maximized {
  position: fixed;
  inset: 0;
  z-index: 2200;
  box-sizing: border-box;
  width: 100vw;
  height: 100vh;
  min-height: 0;
  padding: 12px;
  background: #f7f9fc;
}

.route-flow-graph-designer__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px 8px 0 0;
}

.route-flow-graph-designer__summary,
.route-flow-graph-designer__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.route-flow-graph-designer__summary {
  display: grid;
  grid-template-columns: auto minmax(180px, 1fr);
  flex: 1 1 auto;
  color: #4b5563;
  font-size: 13px;
}

.route-flow-graph-designer__toolbar-save,
.route-flow-graph-designer__toolbar-back {
  min-width: 64px;
}

.route-flow-graph-designer__summary strong {
  color: #172033;
  font-size: 14px;
}

.route-flow-capacity-workstation-repair__hint {
  margin: -4px 0 14px 112px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.route-flow-capacity-workstation-repair__readonly {
  display: flex;
  min-height: 32px;
  align-items: center;
  gap: 10px;
  color: #172033;
}

.route-flow-capacity-workstation-repair__readonly small {
  color: #6b7280;
  font-size: 12px;
}

.route-flow-graph-designer__route-name,
.route-flow-graph-designer__selected-full-name {
  max-width: min(360px, 100%);
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-flow-graph-designer__route-name {
  justify-self: start;
}

.route-flow-graph-designer__route-title {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.route-flow-graph-designer__version-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.route-flow-graph-designer__version-pill {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  min-height: 22px;
  padding: 2px 8px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f7f9fc;
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
}

.route-flow-graph-designer__version-pill strong {
  color: inherit;
  font-size: inherit;
  font-weight: 700;
}

.route-flow-graph-designer__version-pill--current {
  border-color: #cfe3ff;
  background: #eef6ff;
  color: #1677ff;
}

.route-flow-graph-designer__selected-full-name {
  flex: 1 1 180px;
  text-align: center;
}

.route-flow-graph-designer__search {
  width: 190px;
}

.route-flow-graph-designer__unsaved {
  flex: 0 0 auto;
  min-width: 68px;
  justify-content: center;
  font-weight: 700;
  white-space: nowrap;
}

.route-flow-graph-designer__connection-control {
  position: relative;
  display: inline-flex;
}

.route-flow-graph-designer__connection-popover {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 30;
  width: min(650px, calc(100vw - 32px));
  padding: 12px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  box-shadow: 0 10px 28px rgb(23 32 51 / 16%);
}

.route-flow-graph-designer__connection-selector {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr) auto;
  align-items: end;
  gap: 10px;
}

.route-flow-graph-designer__connection-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  color: #263247;
  font-size: 13px;
  font-weight: 600;
}

.route-flow-graph-designer__connection-field :deep(.el-select__wrapper) {
  min-height: 40px;
}

.route-flow-graph-designer__connection-arrow {
  align-self: end;
  margin-bottom: 12px;
  color: #6b7280;
}

.route-flow-graph-designer__connection-selector > .el-button {
  min-height: 40px;
}

.route-flow-graph-designer__connection-replacement {
  display: flex;
  grid-column: 1 / -1;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  color: #8a4b08;
  font-size: 12px;
  line-height: 1.5;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 6px;
}

.route-flow-graph-designer__main {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 260px;
  min-height: 0;
  flex: 1;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-top: 0;
  border-radius: 0 0 8px 8px;
  overflow: hidden;
}

.route-flow-graph-designer__canvas {
  position: relative;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: #f7f9fc;
}

.route-flow-graph-designer__canvas.is-readonly {
  cursor: default;
}

.route-flow-graph-designer__flow {
  width: 100%;
  height: 100%;
  background: linear-gradient(#edf1f6 1px, transparent 1px),
    linear-gradient(90deg, #edf1f6 1px, transparent 1px), #f7f9fc;
  background-size: 24px 24px;
}

.route-flow-graph-designer__node {
  position: relative;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  grid-template-rows: auto;
  align-content: center;
  align-items: center;
  gap: 4px 8px;
  width: 156px;
  height: 68px;
  padding: 9px 12px;
  text-align: left;
  color: #172033;
  background: #ffffff;
  border: 1px solid #c9d6e8;
  border-radius: 8px;
  box-shadow: 0 8px 18px rgb(23 32 51 / 8%);
  cursor: move;
}

.route-flow-graph-designer__node.has-flags {
  grid-template-rows: auto auto;
}

.route-flow-graph-designer__node.is-key {
  border-color: #e6a23c;
}

.route-flow-graph-designer__node.is-invalid {
  border-color: #f56c6c;
  background: #fff7f6;
}

.route-flow-graph-designer__node.is-highlight {
  outline: 3px solid rgb(22 119 255 / 22%);
}

.route-flow-graph-designer__node.is-binding-bound {
  border-color: #67c23a;
  background: #f7fff4;
  box-shadow:
    0 0 0 2px rgb(103 194 58 / 18%),
    0 8px 18px rgb(23 32 51 / 8%);
}

.route-flow-graph-designer__node.is-binding-missing {
  border-color: #f56c6c;
  background: #fff7f6;
  box-shadow:
    0 0 0 2px rgb(245 108 108 / 18%),
    0 8px 18px rgb(23 32 51 / 8%);
}

.route-flow-graph-designer__node.is-selected {
  border-color: #7c3aed;
  box-shadow:
    0 0 0 2px rgb(124 58 237 / 22%),
    0 10px 22px rgb(124 58 237 / 16%);
}

.route-flow-graph-designer__node-form-count-badge {
  position: absolute;
  right: 8px;
  top: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 28px;
  padding: 0 3px;
  color: #7c4a03;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  background: #fffbeb;
  border: 3px solid #facc15;
  border-radius: 2px;
  box-shadow: 0 2px 5px rgb(124 74 3 / 14%);
}

.route-flow-graph-designer__boundary-node {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 132px;
  height: 54px;
  color: #1677ff;
  font-size: 14px;
  font-weight: 700;
  background: #eaf3ff;
  border: 1px dashed #7aa7df;
  border-radius: 8px;
  box-shadow: 0 8px 18px rgb(22 119 255 / 10%);
  cursor: pointer;
}

.route-flow-graph-designer__boundary-node.is-selected {
  color: #0f5fc2;
  background: #dcecff;
  border-color: #1677ff;
  box-shadow:
    0 0 0 2px rgb(22 119 255 / 22%),
    0 10px 22px rgb(22 119 255 / 16%);
}

.route-flow-graph-designer__node-sort {
  display: inline-flex;
  grid-row: 1;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: #1677ff;
  font-weight: 600;
  background: #eaf3ff;
  border-radius: 6px;
}

.route-flow-graph-designer__node.has-flags .route-flow-graph-designer__node-sort {
  grid-row: 1 / 3;
}

.route-flow-graph-designer__node-name {
  min-width: 0;
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-flow-graph-designer__node-flags {
  display: flex;
  align-items: center;
  gap: 4px;
}

.route-flow-graph-designer__handle {
  z-index: 3;
  box-sizing: border-box;
  width: 2px;
  height: 2px;
  opacity: 0;
  pointer-events: none;
}

.route-flow-graph-designer__handle.is-visible {
  width: 24px;
  height: 24px;
  opacity: 1;
  pointer-events: auto;
  background: #ffffff;
  border: 2px solid #1677ff;
  box-shadow: 0 2px 8px rgb(22 119 255 / 22%);
  cursor: crosshair;
}

.route-flow-graph-designer__handle.is-anchor {
  background: transparent;
  border: 0;
  box-shadow: none;
}

.route-flow-graph-designer__handle.is-in.is-left {
  left: -13px;
}

.route-flow-graph-designer__handle.is-out.is-left {
  left: -25px;
}

.route-flow-graph-designer__handle.is-in.is-right {
  right: -25px;
}

.route-flow-graph-designer__handle.is-out.is-right {
  right: -19px;
}

.route-flow-graph-designer__handle.is-in.is-top {
  top: -13px;
}

.route-flow-graph-designer__handle.is-out.is-top {
  top: -19px;
}

.route-flow-graph-designer__handle.is-in.is-bottom {
  bottom: -13px;
}

.route-flow-graph-designer__handle.is-out.is-bottom {
  bottom: -19px;
}

.route-flow-graph-designer__empty {
  position: absolute;
  left: 50%;
  top: 50%;
  color: #6b7280;
  transform: translate(-50%, -50%);
}

.route-flow-graph-designer__panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  overflow: hidden;
  background: #ffffff;
  border-left: 1px solid #dbe3ef;
}

.route-flow-graph-designer__panel-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  color: #4b5563;
  font-size: 13px;
}

.route-flow-graph-designer__selected-edge-section {
  flex: 0 0 auto;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf1f6;
}

.route-flow-graph-designer__selected-field-section {
  flex: 1 1 auto;
  min-height: 0;
  padding: 10px;
  overflow-y: auto;
  overscroll-behavior: contain;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  scrollbar-gutter: stable;
}

.route-flow-graph-designer__selected-field-empty {
  margin: 0;
  padding: 8px;
  color: #6b7280;
  background: #fafcff;
  border: 1px dashed #dbe3ef;
  border-radius: 6px;
}

.route-flow-graph-designer__selected-field-grid {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  gap: 6px 8px;
  align-items: center;
  min-width: 0;
}

.route-flow-graph-designer__selected-field-grid span,
.route-flow-graph-designer__selected-field-value span {
  color: #6b7280;
  font-size: 12px;
}

.route-flow-graph-designer__selected-field-grid strong,
.route-flow-graph-designer__selected-field-value strong {
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-flow-graph-designer__selected-field-coverage {
  width: fit-content;
  min-width: 64px;
  margin-top: 10px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
  border: 1px solid transparent;
  border-radius: 6px;
}

.route-flow-graph-designer__selected-field-coverage.is-covered {
  color: #14804a;
  background: #eaf8ee;
  border-color: #b7e4c7;
}

.route-flow-graph-designer__selected-field-coverage.is-missing {
  color: #c94040;
  background: #fff1f0;
  border-color: #f8c5c5;
}

.route-flow-graph-designer__selected-field-value {
  display: flex;
  flex-direction: column;
  gap: 4px;
  justify-content: center;
  min-width: 0;
  padding-top: 8px;
  border-top: 1px solid #edf1f6;
}

.route-flow-graph-designer__form-slot-view-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.route-flow-graph-designer__form-slot-view-summary-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  padding: 8px;
  background: #fafcff;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
}

.route-flow-graph-designer__form-slot-view-summary-item strong {
  white-space: normal;
}

.route-flow-graph-designer__form-slot-view-summary-item span {
  line-height: 1.45;
}

.route-flow-graph-designer__relation-detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  max-height: min(420px, 48vh);
  padding-top: 8px;
  border-top: 1px solid #edf1f6;
}

.route-flow-graph-designer__selected-edge-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 6px;
  min-width: 0;
  padding: 8px;
  color: #263247;
  background: #fafcff;
  border: 1px solid #edf1f6;
  border-radius: 6px;
}

.route-flow-graph-designer__selected-edge-summary span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-flow-graph-designer__process-detail-sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  padding: 12px;
  overflow: hidden;
  background: #ffffff;
  border-right: 1px solid #dbe3ef;
}

.route-flow-graph-designer__boundary-detail {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
}

.route-flow-graph-designer__boundary-detail h4,
.route-flow-graph-designer__boundary-detail p {
  margin: 0;
}

.route-flow-graph-designer__boundary-detail h4 {
  color: #172033;
  font-size: 14px;
}

.route-flow-graph-designer__boundary-detail p {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.6;
}

.route-flow-graph-designer__boundary-relation-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 0;
  overflow-y: auto;
}

.route-flow-graph-designer__boundary-relation-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 6px;
  padding: 8px;
  color: #4b5563;
  background: #fafcff;
  border: 1px solid #edf1f6;
  border-radius: 6px;
}

.route-flow-graph-designer__boundary-relation-item span,
.route-flow-graph-designer__boundary-relation-item strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-flow-graph-designer__boundary-relation-item strong {
  color: #263247;
  font-weight: 600;
}

.route-flow-graph-designer__process-detail-field-picker {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 30px;
  align-items: center;
  gap: 6px;
}

.route-flow-graph-designer__process-detail-field-picker :deep(.el-select__wrapper) {
  min-height: 30px;
}

.route-flow-graph-designer__selected-detail-list {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  overflow-y: auto;
}

.route-flow-graph-designer__selected-detail-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 26px;
  align-items: stretch;
  gap: 8px;
  padding: 8px;
  border: 1px solid #edf1f6;
  border-radius: 6px;
}

.route-flow-graph-designer__selected-detail-item.is-capacity-source-focus {
  border-color: rgb(22 119 255 / 36%);
  background: #f0f7ff;
  box-shadow: inset 3px 0 0 #1677ff;
}

.route-flow-graph-designer__selected-detail-item.is-selected {
  border-color: rgb(22 119 255 / 45%);
  background: #f5f9ff;
}

.route-flow-graph-designer__selected-detail-item :deep(.el-button.is-circle) {
  width: 24px;
  height: 24px;
  min-height: 24px;
  padding: 0;
}

.route-flow-graph-designer__selected-detail-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  justify-content: center;
  min-width: 0;
}

.route-flow-graph-designer__selected-detail-content span {
  color: #6b7280;
  font-size: 12px;
}

.route-flow-graph-designer__selected-detail-button {
  display: flex;
  width: 100%;
  max-width: 100%;
  min-height: 38px;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  text-align: left;
  color: #1677ff;
  background: #ffffff;
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
}

.route-flow-graph-designer__selected-detail-button span {
  overflow: hidden;
  color: inherit;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-flow-graph-designer__selected-detail-button[aria-pressed='true'] {
  color: #0b63ce;
  background: #eef6ff;
  border-color: #b9d7ff;
}

.route-flow-graph-designer__selected-detail-button:hover,
.route-flow-graph-designer__selected-detail-button:focus-visible {
  color: #0b63ce;
  background: #f4f8ff;
  border-color: #b9d7ff;
  outline: none;
}

.route-flow-graph-designer__selected-detail-content strong {
  overflow: hidden;
  color: #263247;
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-flow-graph-designer__process-detail-loading {
  width: min(120px, 100%);
  padding-top: 2px;
}

.route-flow-graph-designer__process-detail-loading :deep(.el-skeleton__item) {
  height: 14px;
  margin: 0;
}

.route-flow-graph-designer__selected-detail-links {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
  min-width: 0;
}

.route-flow-graph-designer__selected-detail-links :deep(.el-button) {
  max-width: 100%;
  min-height: 20px;
  margin-left: 0;
  padding: 0;
  line-height: 1.35;
  text-align: left;
  white-space: normal;
}

.route-flow-graph-designer__selected-detail-editor {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.route-flow-graph-designer__selected-detail-editor :deep(.el-input-number),
.route-flow-graph-designer__selected-detail-editor :deep(.el-select) {
  width: 100%;
}

.route-flow-graph-designer__selected-detail-editor :deep(.el-input-number .el-input__wrapper),
.route-flow-graph-designer__selected-detail-editor :deep(.el-select__wrapper) {
  min-height: 30px;
}

.route-flow-graph-designer__selected-detail-editor :deep(.el-switch) {
  align-self: flex-start;
}

.route-flow-graph-designer__record-binding-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
}

.route-flow-graph-designer__record-binding-toolbar,
.route-flow-graph-designer__record-binding-toolbar-actions,
.route-flow-graph-designer__record-binding-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.route-flow-graph-designer__record-binding-toolbar-actions {
  justify-content: flex-end;
}

.route-flow-graph-designer__record-binding-toolbar span {
  color: #263247;
  font-size: 13px;
  font-weight: 600;
}

.route-flow-graph-designer__record-binding-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  background: #fafcff;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
}

.route-flow-graph-designer__record-binding-label {
  color: #172033;
  font-size: 13px;
  font-weight: 700;
}

.route-flow-graph-designer__shared-form-binding {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 6px;
}

.route-flow-graph-designer__record-binding-scope {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 30px;
  gap: 8px;
  color: #4b5563;
  font-size: 12px;
}

.route-flow-graph-designer__record-binding-scope span {
  color: #263247;
  font-weight: 600;
}

.route-flow-graph-designer__copy-form-binding-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.route-flow-graph-designer__copy-form-binding-panel span {
  color: #4b5563;
  font-size: 12px;
}

.route-flow-graph-designer__copy-form-binding-panel :deep(.el-select) {
  width: 100%;
}

.route-flow-graph-designer__workstation-detail,
.route-flow-graph-designer__workstation-capacity {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.route-flow-graph-designer__workstation-capacity {
  padding-top: 4px;
  border-top: 1px dashed #e5ebf3;
  color: #263247;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.route-flow-graph-designer__workstation-capacity-original {
  color: #9ca3af;
  text-decoration: line-through;
}

.route-flow-graph-designer__workstation-capacity-actions {
  display: flex;
  justify-content: flex-start;
  padding-top: 2px;
}

.route-flow-graph-designer__capacity-override-button,
.route-flow-graph-designer__capacity-override-link {
  font-family: inherit;
  cursor: pointer;
}

.route-flow-graph-designer__capacity-override-button {
  height: 24px;
  padding: 0 11px;
  color: #009688;
  font-size: 12px;
  line-height: 22px;
  background: #ecfdf8;
  border: 1px solid #a7e8df;
  border-radius: 4px;
}

.route-flow-graph-designer__capacity-override-button:hover,
.route-flow-graph-designer__capacity-override-button:focus-visible {
  color: #ffffff;
  background: #009688;
  border-color: #009688;
  outline: none;
}

.route-flow-graph-designer__capacity-override-button:disabled,
.route-flow-graph-designer__capacity-override-link:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.route-flow-graph-designer__capacity-override-button.is-loading {
  cursor: wait;
}

.route-flow-graph-designer__capacity-override-link {
  display: inline;
  width: fit-content;
  padding: 0;
  color: #009688;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
  text-align: left;
  background: transparent;
  border: 0;
}

.route-flow-graph-designer__capacity-override-link:hover,
.route-flow-graph-designer__capacity-override-link:focus-visible {
  color: #00796b;
  text-decoration: underline;
  outline: none;
}

.route-flow-graph-designer__selected-detail-content
  .route-flow-graph-designer__process-detail-attention {
  display: -webkit-box;
  overflow: hidden;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.route-flow-graph-designer__panel-section h4 {
  margin: 0;
  color: #172033;
  font-size: 14px;
}

.route-flow-graph-designer__relation-list {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 6px;
  min-height: 0;
  overflow: auto;
}

.route-flow-graph-designer__relation-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr) auto;
  gap: 5px;
  align-items: center;
  width: 100%;
  padding: 6px 8px;
  text-align: left;
  color: #4b5563;
  background: #fafcff;
  border: 1px solid #edf1f6;
  border-radius: 6px;
  cursor: pointer;
}

.route-flow-graph-designer__relation-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-flow-graph-designer__relation-item.is-selected {
  color: #1677ff;
  background: #eaf3ff;
  border-color: #9ec9ff;
}

:deep(.vue-flow__edge.selected .vue-flow__edge-path) {
  stroke: #f56c6c;
  stroke-width: 3;
}
</style>
