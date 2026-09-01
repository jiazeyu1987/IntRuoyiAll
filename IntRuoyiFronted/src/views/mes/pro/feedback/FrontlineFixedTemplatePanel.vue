<template>
  <section
    ref="frontlinePanelRef"
    class="frontline-operator-panel"
    data-pqc-fullscreen-root
    :class="{
      'is-pqc-fullscreen': isPqcFullscreen,
      'is-production-mode': !isPqcMode,
      'is-production-fullscreen': isProductionFullscreen
    }"
  >
    <div
      v-if="isPqcMode"
      class="frontline-operator-screen is-pqc"
      data-frontline-pqc-operator
    >
      <header class="frontline-operator-top is-pqc">
        <button
          class="frontline-top-card frontline-top-card--order-summary"
          type="button"
          data-pqc-order-summary-card
          @click="openPicker('order')"
        >
          <div class="frontline-order-summary__field is-order">
            <span>生产订单</span>
            <strong
              class="frontline-order-summary__value is-order"
              data-pqc-order-code
            >
              {{ productionOrderLabel }}
            </strong>
          </div>
          <div v-if="selectedActiveOrder" class="frontline-order-summary__field">
            <span>产品名称</span>
            <strong class="frontline-order-summary__value" data-pqc-product-name>
              {{ selectedActiveOrder.productName }}
            </strong>
          </div>
          <div v-if="selectedActiveOrder" class="frontline-order-summary__field is-quantity">
            <span>产品数量</span>
            <strong class="frontline-order-summary__value" data-pqc-product-quantity>
              {{ selectedOrderQuantityLabel }}
            </strong>
          </div>
        </button>
        <div
          class="frontline-top-card frontline-production-process-nav-card frontline-pqc-process-nav-card"
          data-pqc-process-nav-card
        >
          <button
            class="frontline-production-process-nav-button"
            type="button"
            data-pqc-process-previous
            aria-label="前一个工序"
            :disabled="isPqcProcessPreviousDisabled"
            @click.stop="handleNavigatePqcProcess(-1)"
          >
            <span class="frontline-production-process-nav-icon is-previous" aria-hidden="true"></span>
          </button>
          <button
            class="frontline-production-process-current"
            type="button"
            data-pqc-process-current
            :disabled="isPqcProcessNavigationBlocked"
            @click="openPicker('process')"
          >
            <span>工序</span>
            <strong>{{ selectedProcessLabel }}</strong>
          </button>
          <button
            class="frontline-production-process-nav-button"
            type="button"
            data-pqc-process-next
            aria-label="后一个工序"
            :disabled="isPqcProcessNextDisabled"
            @click.stop="handleNavigatePqcProcess(1)"
          >
            <span class="frontline-production-process-nav-icon is-next" aria-hidden="true"></span>
          </button>
        </div>
        <button
          class="frontline-top-card is-login-employee"
          type="button"
          data-pqc-login-employee-card
          disabled
          aria-disabled="true"
        >
          <span>员工</span>
          <strong>{{ selectedEmployeeLabel }}</strong>
        </button>
        <button
          class="frontline-home-button frontline-pqc-fullscreen-toggle"
          type="button"
          data-pqc-fullscreen-toggle
          :aria-label="pqcFullscreenActionText"
          :aria-pressed="isPqcFullscreen"
          @click="handlePqcFullscreenToggle"
        >
          {{ pqcFullscreenActionText }}
        </button>
      </header>

      <div
        v-if="activePqcInspectionItem"
        class="frontline-pqc-piece-modal"
        data-pqc-piece-modal
        role="dialog"
        aria-modal="true"
        :aria-label="`${activePqcInspectionItem.label}逐件检验`"
        @click.self="closePqcPieceInspection(false)"
      >
        <section class="frontline-pqc-piece-dialog">
          <h3>{{ activePqcInspectionItem.label }}（{{ pqcInspectionQuantity }}件）</h3>
          <div class="frontline-pqc-piece-list" data-pqc-piece-list>
            <article
              v-for="pieceIndex in pqcInspectionQuantity"
              :key="pieceIndex"
              class="frontline-pqc-piece-row"
            >
              <strong>{{ pieceIndex }}</strong>
              <div
                v-if="activePqcInspectionItem.type === 'number'"
                class="frontline-pqc-piece-value-control"
              >
                <button
                  type="button"
                  :aria-label="`第 ${pieceIndex} 件${activePqcInspectionItem.label}减少`"
                  @click="stepPqcPieceValue(pieceIndex - 1, -activePqcInspectionItem.step)"
                >
                  -
                </button>
                <input
                  :value="pqcPieceDraftValues[pieceIndex - 1]"
                  type="number"
                  :step="activePqcInspectionItem.step"
                  :aria-label="`第 ${pieceIndex} 件${activePqcInspectionItem.label}`"
                  @input="updatePqcPieceDraftValue(pieceIndex - 1, $event)"
                />
                <button
                  type="button"
                  :aria-label="`第 ${pieceIndex} 件${activePqcInspectionItem.label}增加`"
                  @click="stepPqcPieceValue(pieceIndex - 1, activePqcInspectionItem.step)"
                >
                  +
                </button>
                <span>{{ activePqcInspectionItem.unit }}</span>
              </div>
              <div v-else class="frontline-pqc-piece-choice">
                <el-switch
                  class="frontline-pqc-piece-switch"
                  data-pqc-piece-choice-switch
                  inline-prompt
                  active-text="合格"
                  inactive-text="不合格"
                  :model-value="pqcPieceDraftValues[pieceIndex - 1] === '合格'"
                  :aria-label="`第 ${pieceIndex} 件${activePqcInspectionItem.label}${pqcPieceDraftValues[pieceIndex - 1] || '合格'}`"
                  @update:model-value="updatePqcPieceChoice(pieceIndex - 1, $event)"
                />
              </div>
            </article>
          </div>
          <footer class="frontline-pqc-piece-actions">
            <button type="button" @click="closePqcPieceInspection(false)">返回</button>
            <button type="button" class="primary" @click="closePqcPieceInspection(true)">
              完成
            </button>
          </footer>
        </section>
      </div>

      <div
        v-if="activePqcStandardItem"
        class="frontline-pqc-fact-dialog"
        data-pqc-standard-dialog
        role="dialog"
        aria-modal="true"
        aria-labelledby="pqc-standard-dialog-title"
        @click.self="closePqcStandardDialog"
      >
        <section class="frontline-pqc-fact-dialog__panel" data-pqc-standard-dialog-panel>
          <header class="frontline-pqc-fact-dialog__header">
            <div>
              <span class="frontline-pqc-fact-dialog__eyebrow">接收标准</span>
              <h3 id="pqc-standard-dialog-title">{{ activePqcStandardItem.label }}</h3>
            </div>
            <button
              type="button"
              class="frontline-pqc-fact-dialog__close"
              aria-label="关闭接收标准弹框"
              @click="closePqcStandardDialog"
            >
              ×
            </button>
          </header>
          <div class="frontline-pqc-fact-dialog__body is-standard">
            <article class="frontline-pqc-fact-dialog__detail" data-pqc-standard-detail-text>
              <span>标准说明</span>
              <p>{{ activePqcStandardItem.acceptanceStandard || '未配置接收标准说明' }}</p>
            </article>
          </div>
          <footer class="frontline-pqc-fact-dialog__footer">
            <button type="button" @click="closePqcStandardDialog">关闭</button>
          </footer>
        </section>
      </div>

      <div
        v-if="activePqcMethodItem"
        class="frontline-pqc-fact-dialog"
        data-pqc-method-dialog
        role="dialog"
        aria-modal="true"
        aria-labelledby="pqc-method-dialog-title"
        @click.self="closePqcMethodDialog"
      >
        <section class="frontline-pqc-fact-dialog__panel" data-pqc-method-dialog-panel>
          <header class="frontline-pqc-fact-dialog__header">
            <div>
              <span class="frontline-pqc-fact-dialog__eyebrow">检验方法</span>
              <h3 id="pqc-method-dialog-title">{{ activePqcMethodItem.samplingPlanText }}</h3>
            </div>
            <button
              type="button"
              class="frontline-pqc-fact-dialog__close"
              aria-label="关闭检验方法弹框"
              @click="closePqcMethodDialog"
            >
              ×
            </button>
          </header>
          <div class="frontline-pqc-fact-dialog__body">
            <article class="frontline-pqc-fact-dialog__detail" data-pqc-method-detail-text>
              <span>方法说明</span>
              <p>{{ formatPqcMethodSummary(activePqcMethodItem) }}</p>
            </article>
            <article
              class="frontline-pqc-fact-dialog__detail is-equipment"
              data-pqc-method-equipment-text
            >
              <span>检验器具及设备</span>
              <p>{{ activePqcMethodItem.inspectionTool }}</p>
            </article>
          </div>
          <footer class="frontline-pqc-fact-dialog__footer">
            <button type="button" @click="closePqcMethodDialog">关闭</button>
          </footer>
        </section>
      </div>

      <main class="frontline-operator-main is-pqc">
        <section
          class="frontline-work-panel frontline-pqc-content-panel"
          data-frontline-pqc-inspection-content
        >
          <div class="frontline-pqc-inspection-list">
            <article
              v-if="activePqcTabItem"
              class="frontline-pqc-content-item"
              data-pqc-active-inspection-panel
              :data-pqc-inspection-entry="activePqcTabItem.key"
              :aria-label="`${formatPqcInspectionTitle(activePqcTabItem)}检验详情`"
            >
              <div class="pqc-utility-strip" :aria-label="`${activePqcTabItem.label}质检信息`">
                <label
                  v-if="hasPqcEquipmentOptions(activePqcTabItem)"
                  class="pqc-select-card"
                  data-pqc-equipment-card
                  :class="{
                    'is-selected': Boolean(getPqcItemSelection(activePqcTabItem.key).selectedEquipmentId),
                    'is-empty': !getPqcItemSelection(activePqcTabItem.key).selectedEquipmentId
                  }"
                >
                  <span>
                    <strong>检验设备</strong>
                    <span>{{ getPqcSelectedEquipmentLabel(activePqcTabItem) }}</span>
                  </span>
                  <em aria-hidden="true">&gt;</em>
                  <select
                    class="pqc-select-native"
                    :value="getPqcItemSelection(activePqcTabItem.key).selectedEquipmentId ?? ''"
                    data-pqc-equipment-select
                    aria-label="选择检验设备"
                    @change="updatePqcItemSelectedEquipment(activePqcTabItem.key, $event)"
                  >
                    <option value="">选择检验设备</option>
                    <option
                      v-for="option in getUniquePqcEquipmentOptions(activePqcTabItem)"
                      :key="option.equipmentId"
                      :value="option.equipmentId"
                    >
                      {{ formatPqcEquipmentLabel(option) }}
                    </option>
                  </select>
                </label>

                <button
                  type="button"
                  class="pqc-fact-card is-primary"
                  data-pqc-standard-button
                  @click="openPqcStandardDialog(activePqcTabItem.key)"
                >
                  <strong>接收标准</strong>
                  <span>{{ formatPqcStandardSummary(activePqcTabItem) }}</span>
                </button>
                <button
                  type="button"
                  class="pqc-fact-card"
                  data-pqc-method-button
                  @click="openPqcMethodDialog(activePqcTabItem.key)"
                >
                  <strong>检验方法</strong>
                  <span>{{ formatPqcMethodSummary(activePqcTabItem) }}</span>
                </button>
              </div>

              <div
                class="frontline-pqc-choice-actions"
                :class="{ 'is-number': activePqcTabItem.type === 'number' }"
              >
                <button
                  v-if="activePqcTabItem.type === 'choice'"
                  type="button"
                  class="pass"
                  :class="{ active: isPqcBulkChoiceActive(activePqcTabItem.key, '合格') }"
                  @click="applyPqcBulkChoice(activePqcTabItem.key, '合格')"
                >
                  全部合格
                </button>
                <button
                  v-if="activePqcTabItem.type === 'choice'"
                  type="button"
                  class="fail"
                  :class="{ active: isPqcBulkChoiceActive(activePqcTabItem.key, '不合格') }"
                  @click="applyPqcBulkChoice(activePqcTabItem.key, '不合格')"
                >
                  全部不良
                </button>
                <button
                  type="button"
                  class="manual"
                  data-pqc-piece-open-button
                  :class="{ active: isPqcManualChoiceActive(activePqcTabItem.key) }"
                  @click="openPqcPieceInspection(activePqcTabItem.key)"
                >
                  <span>{{ activePqcTabItem.type === 'number' ? '逐件填写' : '逐件选择' }}</span>
                  <em>{{ getPqcProgressText(activePqcTabItem.key) }}</em>
                  <strong aria-hidden="true">&gt;</strong>
                </button>
              </div>
            </article>

            <div v-else class="frontline-pqc-empty-state" data-pqc-empty-inspection>
              {{ pqcInspectionEmptyText }}
            </div>

            <nav
              v-if="pqcInspectionItems.length"
              class="pqc-item-tabs"
              data-pqc-inspection-tabs
              aria-label="PQC检验项目切换"
            >
              <button
                v-for="item in pqcInspectionItems"
                :key="item.key"
                type="button"
                class="pqc-item-tab"
                data-pqc-inspection-tab
                :class="{ active: activePqcTabKey === item.key }"
                :aria-pressed="activePqcTabKey === item.key"
                @click="selectPqcInspectionTab(item.key)"
              >
                <strong>{{ formatPqcInspectionItemTabLabel(item) }}</strong>
              </button>
            </nav>
            <div
              class="frontline-inline-error-slot"
              data-frontline-error-slot
              :class="{ 'is-visible': Boolean(frontlineErrorMessage) }"
              role="alert"
              aria-live="assertive"
              aria-atomic="true"
            >
              <template v-if="frontlineErrorMessage">
                <Icon icon="ep:warning-filled" :size="28" aria-hidden="true" />
                <span data-frontline-error-message>{{ frontlineErrorMessage }}</span>
                <button
                  type="button"
                  data-frontline-error-dismiss
                  aria-label="关闭错误提示"
                  @click="clearFrontlineError"
                >
                  <Icon icon="ep:close" :size="24" aria-hidden="true" />
                </button>
              </template>
            </div>
          </div>
        </section>

        <section class="frontline-work-panel frontline-pqc-fill-panel">
          <div class="frontline-pqc-type-tabs">
            <button
              v-for="tab in pqcInspectionTypeTabs"
              :key="tab.type"
              type="button"
              :data-pqc-inspection-type-tab="tab.type"
              :class="{ active: pqcDraft.inspectionType === tab.type }"
              @click="selectPqcInspectionType(tab.type)"
            >
              {{ tab.label }}
            </button>
          </div>
          <div class="frontline-pqc-round-tabs">
            <button
              v-for="round in pqcVisibleRounds"
              :key="round.value"
              type="button"
              :class="{ active: activePqcTaskOption?.pqcTaskId === round.value }"
              @click="selectPqcInspectionTaskOption(round.value)"
            >
              {{ round.label }}
            </button>
          </div>
          <div class="frontline-pqc-form-area">
            <div class="frontline-pqc-number-field">
              <label for="frontlinePqcInspectionQuantity">检验</label>
              <button
                type="button"
                aria-label="检验减少"
                @click="adjustPqcQuantity('inspectionQuantity', -1)"
              >
                -
              </button>
              <input
                id="frontlinePqcInspectionQuantity"
                :value="pqcDraft.inspectionQuantity ?? ''"
                type="number"
                min="0"
                inputmode="numeric"
                @input="updatePqcQuantity('inspectionQuantity', $event)"
              />
              <button
                type="button"
                aria-label="检验增加"
                @click="adjustPqcQuantity('inspectionQuantity', 1)"
              >
                +
              </button>
              <span>件</span>
            </div>
            <div class="frontline-pqc-number-field">
              <label for="frontlinePqcScrapQuantity">损耗</label>
              <button
                type="button"
                aria-label="损耗减少"
                @click="adjustPqcQuantity('scrapQuantity', -1)"
              >
                -
              </button>
              <input
                id="frontlinePqcScrapQuantity"
                :value="pqcDraft.scrapQuantity ?? ''"
                type="number"
                min="0"
                inputmode="numeric"
                @input="updatePqcQuantity('scrapQuantity', $event)"
              />
              <button
                type="button"
                aria-label="损耗增加"
                @click="adjustPqcQuantity('scrapQuantity', 1)"
              >
                +
              </button>
              <span>件</span>
            </div>
          </div>
        </section>
        <footer class="frontline-pqc-submit-bar">
          <button
            class="frontline-pqc-reset-button"
            type="button"
            :disabled="payloadLoading || pqcSubmitResultUncertain"
            @click="handleResetPqc"
          >
            重填
          </button>
          <button
            class="frontline-pqc-submit-button"
            type="button"
            :disabled="isPqcSubmitBlocked"
            @click="handleValidate"
          >
            {{ payloadLoading ? '提交中' : '提交' }}
          </button>
        </footer>
      </main>

      <div
        v-if="pqcSignatureDialogVisible"
        class="frontline-pqc-signature-modal"
        data-pqc-signature-dialog
        role="dialog"
        aria-modal="true"
        aria-label="PQC电子签名"
      >
        <section class="frontline-pqc-signature-dialog">
          <h3>电子签名</h3>
          <p>确认后将生成本次PQC正式提交签名。</p>
          <label for="frontlinePqcSignaturePassword">登录密码</label>
          <input
            id="frontlinePqcSignaturePassword"
            v-model="pqcSignaturePassword"
            type="password"
            autocomplete="current-password"
            @keyup.enter="handleConfirmPqcSubmit"
          />
          <div>
            <button type="button" :disabled="payloadLoading" @click="closePqcSignatureDialog">
              取消
            </button>
            <button type="button" :disabled="payloadLoading" @click="handleConfirmPqcSubmit">
              {{ payloadLoading ? '签名提交中' : '确认签名并提交' }}
            </button>
          </div>
        </section>
      </div>

      <section
        v-if="pqcSubmitResultUncertain"
        class="frontline-pqc-submit-uncertain"
        data-pqc-submit-uncertain
      >
        PQC正式提交结果不确定，状态确认失败。请刷新页面或联系组长核对后再操作，当前页面已锁定重复提交。
      </section>

    </div>

    <div
      v-else
      class="frontline-production-stage"
      data-frontline-production-stage
      :style="productionStageStyle"
    >
      <div
        class="frontline-operator-screen screen"
        data-frontline-production-operator
      >
        <header
          class="frontline-operator-top top is-production"
          data-frontline-production-selection-grid
        >
          <button
            class="frontline-top-card top-box frontline-production-selection-card"
            type="button"
            data-frontline-production-selection-card
            data-frontline-production-active-order-card
            :disabled="payloadLoading || submitConfirmationOpen || productionSubmitSuccessOpen"
            @click="openPicker('order')"
          >
            <div class="frontline-production-order-summary" aria-label="活跃订单">
              <span
                class="frontline-production-order-summary__value"
                data-frontline-production-order-code
              >
                {{ productionOrderLabel }}
              </span>
              <span
                v-if="productionBatchCodeLabel"
                class="frontline-production-order-summary__value"
                data-frontline-production-batch-code
              >
                {{ productionBatchCodeLabel }}
              </span>
              <span
                v-if="selectedActiveOrder"
                class="frontline-production-order-summary__value"
                data-frontline-production-product-name
              >
                {{ productionProductNameLabel }}
              </span>
            </div>
          </button>
          <div
            class="frontline-top-card top-box frontline-production-selection-card frontline-production-process-nav-card"
            data-frontline-production-selection-card
            data-frontline-production-process-nav-card
          >
            <button
              class="frontline-production-process-nav-button"
              type="button"
              data-frontline-process-previous
              aria-label="前一个工序"
              :disabled="isProductionProcessPreviousDisabled"
              @click.stop="handleNavigateProductionProcess(-1)"
            >
              <span class="frontline-production-process-nav-icon is-previous" aria-hidden="true"></span>
            </button>
            <button
              class="frontline-production-process-current"
              type="button"
              :disabled="isProductionProcessNavigationBlocked"
              @click="openPicker('process')"
            >
              <div class="top-label">工序</div>
              <div class="top-value">{{ selectedProcessLabel }}</div>
            </button>
            <button
              class="frontline-production-process-nav-button"
              type="button"
              data-frontline-process-next
              aria-label="后一个工序"
              :disabled="isProductionProcessNextDisabled"
              @click.stop="handleNavigateProductionProcess(1)"
            >
              <span class="frontline-production-process-nav-icon is-next" aria-hidden="true"></span>
            </button>
          </div>
          <button
            class="frontline-top-card top-box frontline-production-selection-card"
            type="button"
            data-frontline-production-selection-card
            data-frontline-production-employee-card
            :disabled="payloadLoading || submitConfirmationOpen || productionSubmitSuccessOpen"
            @click="openPicker('employee')"
          >
            <div class="top-label">员工</div>
            <div class="top-value">{{ selectedEmployeeLabel }}</div>
          </button>
          <button
            class="frontline-home-button home-btn frontline-production-fullscreen-toggle"
            type="button"
            data-production-fullscreen-toggle
            :aria-label="productionFullscreenActionText"
            :aria-pressed="isProductionFullscreen"
            @click="handleProductionFullscreenToggle"
          >
            {{ productionFullscreenActionText }}
          </button>
        </header>

        <section
          v-if="activePicker"
          class="frontline-picker picker"
          :class="{ 'frontline-picker--production-order': activePicker === 'order' }"
          :aria-label="activePicker === 'order' ? '选择活跃订单' : activePicker === 'process' ? '选择工序' : '选择员工'"
          @click.self="closePicker"
        >
          <div class="frontline-picker__card picker-card">
            <div class="frontline-picker__heading">
              <h3 class="frontline-picker__title picker-title">
                {{ activePicker === 'order' ? '选择活跃订单' : activePicker === 'process' ? '选工序' : '选择员工' }}
              </h3>
              <input
                v-if="activePicker === 'order'"
                ref="activeOrderSearchInputRef"
                v-model="activeOrderKeyword"
                class="frontline-picker__order-search"
                type="search"
                data-frontline-production-order-search-input
                aria-label="输入订单号筛选活跃订单"
                placeholder="输入订单号"
                autocomplete="off"
                spellcheck="false"
                @keydown.enter="handleActiveOrderSearchEnter"
              />
            </div>
            <div class="frontline-picker__options picker-options">
              <p
                v-if="activePicker === 'order' && pickerOptions.length === 0"
                class="frontline-picker__empty"
                role="status"
                aria-live="polite"
              >
                {{ activeOrderPickerEmptyText }}
              </p>
              <p
                v-else-if="pickerStatusText"
                class="frontline-picker__empty"
                role="status"
                aria-live="polite"
              >
                {{ pickerStatusText }}
              </p>
              <button
                v-for="option in pickerOptions"
                :key="option.key"
                class="frontline-picker__option picker-option"
                type="button"
                :class="{ active: option.active }"
                @click="option.onClick"
              >
                <span
                  v-if="activePicker === 'order' && option.activeOrder"
                  class="frontline-order-picker-option"
                >
                  <span class="frontline-order-picker-option__row">
                    <span>编码</span>
                    <strong class="frontline-order-picker-option__value is-code">
                      {{ option.activeOrder.workOrderCode }}
                    </strong>
                  </span>
                  <span class="frontline-order-picker-option__row">
                    <span>产品</span>
                    <strong class="frontline-order-picker-option__value">
                      {{ option.activeOrder.productName }}
                    </strong>
                  </span>
                  <span class="frontline-order-picker-option__row">
                    <span>数量</span>
                    <strong class="frontline-order-picker-option__value">
                      {{ formatProductionQuantity(option.activeOrder.quantity) }}
                    </strong>
                  </span>
                </span>
                <span v-else>{{ option.label }}</span>
              </button>
            </div>
            <button class="frontline-picker__close picker-close" type="button" @click="closePicker">
              返回
            </button>
          </div>
        </section>

        <main class="frontline-operator-main frontline-production-main main">
          <section
            class="frontline-work-panel panel quantity-panel frontline-production-quantity-panel"
            aria-label="数量与不良"
          >
            <div class="frontline-production-number-field field">
              <label class="field-label" for="frontlineProductionOutputQuantity">完成数量</label>
              <button
                class="num-btn"
                type="button"
                aria-label="完成数量减少"
                :disabled="payloadLoading"
                @click="adjustProductionOutputQuantity(-1)"
              >
                -
              </button>
              <input
                class="value-box"
                id="frontlineProductionOutputQuantity"
                :value="productionDraft.outputQuantity ?? ''"
                inputmode="numeric"
                :disabled="payloadLoading"
                @input="updateProductionOutputQuantity"
              />
              <button
                class="num-btn"
                type="button"
                aria-label="完成数量增加"
                :disabled="payloadLoading"
                @click="adjustProductionOutputQuantity(1)"
              >
                +
              </button>
              <span class="unit">件</span>
            </div>

            <div class="frontline-production-number-field field total is-total">
              <label class="field-label" for="frontlineProductionScrapQuantity">损耗数量</label>
              <input
                class="value-box"
                id="frontlineProductionScrapQuantity"
                :value="productionScrapQuantity"
                inputmode="numeric"
                readonly
              />
              <span class="unit">件</span>
            </div>

            <section class="frontline-production-defect-section defect-section" aria-label="不良明细">
              <div class="frontline-production-defect-title defect-title">不良明细</div>
              <div class="frontline-production-defect-grid defect-grid">
                <div
                  v-for="defect in configuredDefectReasons"
                  :key="defect.key"
                  class="frontline-production-defect-card defect-card"
                  :class="{ active: getProductionDefectQuantity(defect.key) > 0 }"
                  :data-defect-key="defect.key"
                >
                  <span class="frontline-production-defect-name defect-name">{{ defect.label }}</span>
                  <button
                    type="button"
                    class="frontline-production-defect-step defect-step"
                    :aria-label="`${defect.label}减少`"
                    :disabled="payloadLoading"
                    @click="adjustProductionDefectQuantity(defect.key, -1)"
                  >
                    -
                  </button>
                  <input
                    class="frontline-production-defect-qty defect-qty"
                    :value="getProductionDefectQuantity(defect.key)"
                    inputmode="numeric"
                    :aria-label="`${defect.label}数量`"
                    :disabled="payloadLoading"
                    @input="updateProductionDefectQuantity(defect.key, $event)"
                  />
                  <button
                    type="button"
                    class="frontline-production-defect-step defect-step"
                    :aria-label="`${defect.label}增加`"
                    :disabled="payloadLoading"
                    @click="adjustProductionDefectQuantity(defect.key, 1)"
                  >
                    +
                  </button>
                  <span class="frontline-production-defect-unit defect-unit">件</span>
                </div>
              </div>
            </section>
            <section
              v-if="configuredProductionMaterials.length > 0"
              class="frontline-production-material-tabs"
              data-frontline-production-material-tabs
              role="tablist"
              aria-label="物料切换"
            >
              <button
                v-for="material in configuredProductionMaterials"
                :key="material.key"
                class="frontline-production-material-tab"
                :class="{
                  'is-selected': material.key === selectedProductionMaterialKey,
                  'is-complete': isProductionMaterialCompletionEntered(material.key)
                }"
                type="button"
                role="tab"
                data-frontline-production-material-tab
                :aria-selected="material.key === selectedProductionMaterialKey"
                :aria-label="`${material.materialName} ${material.materialCode}`"
                :disabled="payloadLoading"
                @click="switchProductionMaterial(material.key)"
              >
                <strong>{{ material.materialName }}</strong>
                <small>{{ material.materialCode }}</small>
                <small
                  v-if="material.batchCodes.length > 0"
                  class="frontline-production-material-batches"
                >
                  批号 {{ material.batchCodes.join('、') }}
                </small>
              </button>
            </section>
            <div
              class="frontline-inline-error-slot"
              data-frontline-error-slot
              :class="{ 'is-visible': Boolean(frontlineErrorMessage) }"
              role="alert"
              aria-live="assertive"
              aria-atomic="true"
            >
              <template v-if="frontlineErrorMessage">
                <Icon icon="ep:warning-filled" :size="28" aria-hidden="true" />
                <span data-frontline-error-message>{{ frontlineErrorMessage }}</span>
                <button
                  type="button"
                  data-frontline-error-dismiss
                  aria-label="关闭错误提示"
                  @click="clearFrontlineError"
                >
                  <Icon icon="ep:close" :size="24" aria-hidden="true" />
                </button>
              </template>
            </div>
          </section>

          <section
            class="frontline-work-panel panel device-panel frontline-production-device-panel"
            aria-label="设备"
          >
            <div
              v-if="visibleDeviceCards.length > 0"
              class="frontline-production-device-tabs device-tabs"
              :style="{ '--frontline-device-tab-count': visibleDeviceCards.length }"
              role="tablist"
              aria-label="设备切换"
            >
              <div
                v-for="device in visibleDeviceCards"
                :key="device.key"
                class="frontline-production-device-card device-tab-card"
                :class="{ active: device.key === selectedProductionDeviceKey }"
              >
                <button
                  class="device-tab"
                  type="button"
                  role="tab"
                  :aria-selected="device.key === selectedProductionDeviceKey"
                  :disabled="payloadLoading"
                  @click="selectedProductionDeviceKey = device.key"
                >
                  <span class="device-tab-code">{{ device.label }}</span>
                </button>
                <label class="frontline-production-device-metering-validity">
                  <input
                    type="checkbox"
                    :checked="isProductionDeviceMeteringValid(device.key)"
                    :disabled="payloadLoading"
                    :aria-label="device.label + '在计量效期内'"
                    data-frontline-device-metering-validity
                    @change="updateProductionDeviceMeteringValidity(device.key, $event)"
                  />
                  <span aria-hidden="true">✓</span>
                  <em>在计量效期内</em>
                </label>
              </div>
            </div>
            <div
              v-else
              class="frontline-production-device-empty device-empty"
              data-frontline-production-no-device-empty
            >
              无设备
            </div>
            <div
              v-if="activeProductionDevice && visibleDeviceCards.length > 0"
              class="frontline-production-device-current device-current"
            >
              <article
                v-if="activeProductionSelfCheckNarrative"
                class="frontline-production-self-check"
                data-frontline-production-self-check
                aria-label="生产自检"
              >
                <h3>{{ activeProductionSelfCheckNarrative.title }}</h3>
                <section>
                  <strong>合格标准：</strong>
                  <p>{{ activeProductionSelfCheckNarrative.standard }}</p>
                </section>
                <section>
                  <strong>检验方法：</strong>
                  <p
                    v-for="method in activeProductionSelfCheckNarrative.inspectionMethods"
                    :key="method"
                  >
                    {{ method }}
                  </p>
                </section>
              </article>
              <template v-else>
                <div
                  v-for="parameter in getProductionDeviceDetailParameters(activeProductionDevice)"
                  :key="parameter.parameterCode"
                  class="frontline-production-device-param device-param"
                >
                <label
                  class="device-param-label"
                  :for="`frontlineProductionDeviceParameter-${parameter.parameterCode}`"
                >
                  <span class="device-param-name">
                    {{ parameter.parameterName || parameter.parameterCode }}
                  </span>
                  <small
                    v-if="!isTextStandardParameter(parameter) && formatProductionParameterTargetRange(parameter)"
                    class="device-param-range"
                    data-frontline-device-parameter-range
                  >
                    {{ formatProductionParameterTargetRange(parameter) }}
                  </small>
                </label>
                <span
                  v-if="isTextStandardParameter(parameter)"
                  class="frontline-production-device-standard-text"
                  data-frontline-text-parameter-standard
                >
                  {{ parameter.standardText }}
                </span>
                <button
                  v-else-if="isNumericProductionParameter(parameter)"
                  class="device-num"
                  type="button"
                  :aria-label="`${parameter.parameterName || parameter.parameterCode}减少`"
                  :disabled="payloadLoading"
                  @click="adjustProductionDeviceParameter(activeProductionDevice.key, parameter, -1)"
                >
                  -
                </button>
                <select
                  v-else-if="isSelectParameter(parameter)"
                  class="device-value device-select"
                  :id="`frontlineProductionDeviceParameter-${parameter.parameterCode}`"
                  :value="getProductionDeviceParameter(activeProductionDevice.key, parameter.parameterCode)"
                  :aria-label="parameter.parameterName || parameter.parameterCode"
                  :disabled="payloadLoading"
                  data-frontline-select-parameter
                  @change="updateProductionDeviceSelectParameter(activeProductionDevice.key, parameter.parameterCode, $event)"
                >
                  <option value="">请选择</option>
                  <option
                    v-for="option in parameter.optionValues || []"
                    :key="option"
                    :value="option"
                  >
                    {{ option }}
                  </option>
                </select>
                <label
                  v-else-if="isBooleanParameter(parameter)"
                  class="frontline-production-device-boolean"
                  :for="`frontlineProductionDeviceParameter-${parameter.parameterCode}`"
                >
                  <input
                    :id="`frontlineProductionDeviceParameter-${parameter.parameterCode}`"
                    type="checkbox"
                    :checked="getProductionDeviceParameter(activeProductionDevice.key, parameter.parameterCode) === true"
                    :disabled="payloadLoading"
                    :aria-label="parameter.parameterName || parameter.parameterCode"
                    data-frontline-boolean-parameter
                    @change="updateProductionDeviceBooleanParameter(activeProductionDevice.key, parameter.parameterCode, $event)"
                  />
                  <span aria-hidden="true">✓</span>
                  <em>
                    {{ getProductionDeviceParameter(activeProductionDevice.key, parameter.parameterCode) === true ? '已选' : '未选' }}
                  </em>
                </label>
                <input
                  v-if="isNumericProductionParameter(parameter)"
                  class="device-value"
                  :class="{
                    'is-parameter-out-of-range': resolveProductionParameterStatus(
                      getProductionDeviceParameter(activeProductionDevice.key, parameter.parameterCode),
                      parameter
                    ) !== 'NORMAL'
                  }"
                  :id="`frontlineProductionDeviceParameter-${parameter.parameterCode}`"
                  :value="getProductionDeviceParameter(activeProductionDevice.key, parameter.parameterCode)"
                  :data-parameter-status="resolveProductionParameterStatus(
                    getProductionDeviceParameter(activeProductionDevice.key, parameter.parameterCode),
                    parameter
                  )"
                  :aria-label="[
                    parameter.parameterName || parameter.parameterCode,
                    resolveProductionParameterStatus(
                      getProductionDeviceParameter(activeProductionDevice.key, parameter.parameterCode),
                      parameter
                    ) === 'NORMAL' ? '' : '参数异常'
                  ].filter(Boolean).join('，')"
                  inputmode="decimal"
                  :disabled="payloadLoading"
                  @input="updateProductionDeviceParameter(activeProductionDevice.key, parameter.parameterCode, $event)"
                />
                <button
                  v-if="isNumericProductionParameter(parameter)"
                  class="device-num"
                  type="button"
                  :aria-label="`${parameter.parameterName || parameter.parameterCode}增加`"
                  :disabled="payloadLoading"
                  @click="adjustProductionDeviceParameter(activeProductionDevice.key, parameter, 1)"
                >
                  +
                </button>
                <span v-if="isNumericProductionParameter(parameter)" class="device-unit">
                  {{ parameter.unit || '' }}
                </span>
                </div>
              </template>
            </div>
            <div
              class="frontline-production-clearance-confirmations"
              data-production-clearance-confirmations
              aria-label="清场确认"
            >
              <div
                v-for="confirmation in FRONTLINE_PRODUCTION_CLEARANCE_CONFIRMATIONS"
                :key="confirmation.key"
                class="frontline-production-clearance-confirmation"
              >
                <button
                  type="button"
                  class="frontline-production-clearance-label"
                  data-production-clearance-detail-trigger
                  :aria-label="`${confirmation.label}完整说明`"
                  @click="openProductionClearanceConfirmationDetail(confirmation.key)"
                >
                  <span>{{ confirmation.label }}</span>
                  <small>详情</small>
                </button>
                <label class="frontline-production-clearance-checkbox">
                  <input
                    v-model="productionClearanceConfirmationDraft[confirmation.key]"
                    type="checkbox"
                    data-production-clearance-checkbox
                    :disabled="payloadLoading"
                    :aria-label="`${confirmation.label}是否确认`"
                  />
                  <span aria-hidden="true">✓</span>
                  <em>已选</em>
                </label>
              </div>
            </div>
          </section>

          <footer class="frontline-production-submit-bar bottom">
            <button
              class="frontline-production-reset-button minor-btn"
              type="button"
              :disabled="payloadLoading || productionSubmitSuccessOpen"
              @click="handleResetProduction"
            >
              重填
            </button>
            <button
              class="frontline-production-submit-button submit-btn"
              type="button"
              :disabled="isSubmitBlocked"
              @click="handleValidate"
            >
              <span>
                {{
                  payloadLoading
                    ? '提交中'
                    : submitConfirmationOpen
                      ? '等待确认'
                      : '正式提交'
                }}
              </span>
            </button>
          </footer>
        </main>
      </div>
    </div>

    <div
      v-if="submitConfirmationOpen && !isPqcMode"
      class="frontline-production-submit-confirmation-modal"
      data-production-submit-confirmation-dialog
      role="dialog"
      aria-modal="true"
      aria-labelledby="frontlineProductionSubmitConfirmationTitle"
      @click.self="cancelProductionFormalSubmitConfirmation"
    >
      <section class="frontline-production-submit-confirmation-dialog">
        <h3 id="frontlineProductionSubmitConfirmationTitle">确认正式提交</h3>
        <p data-production-submit-confirmation-message>
          {{ productionFormalSubmitConfirmationText }}
        </p>
        <label
          class="frontline-production-submit-confirmation-signature"
          for="frontlineProductionSignaturePassword"
        >
          <span>签名密码</span>
          <input
            id="frontlineProductionSignaturePassword"
            v-model="productionSignaturePassword"
            type="password"
            data-production-submit-signature-password
            autocomplete="current-password"
            :disabled="payloadLoading"
            placeholder="请输入所选员工签名密码"
            @keydown.enter.prevent="confirmProductionFormalSubmitConfirmation"
          />
        </label>
        <div class="frontline-production-submit-confirmation-actions">
          <button
            type="button"
            data-production-submit-confirm-cancel
            :disabled="payloadLoading"
            @click="cancelProductionFormalSubmitConfirmation"
          >
            取消
          </button>
          <button
            type="button"
            data-production-submit-confirm-accept
            :disabled="payloadLoading"
            @click="confirmProductionFormalSubmitConfirmation"
          >
            {{ payloadLoading ? '提交中' : '确认提交' }}
          </button>
        </div>
      </section>
    </div>

    <div
      v-if="productionSubmitSuccessOpen && !isPqcMode"
      class="frontline-production-submit-success-modal"
      data-production-submit-success-dialog
      role="dialog"
      aria-modal="true"
      aria-labelledby="frontlineProductionSubmitSuccessTitle"
    >
      <section class="frontline-production-submit-success-dialog">
        <Icon
          icon="ep:circle-check-filled"
          :size="96"
          class="frontline-production-submit-success-icon"
          aria-hidden="true"
        />
        <div class="frontline-production-submit-success-copy">
          <span>正式提交成功</span>
          <h3 id="frontlineProductionSubmitSuccessTitle">提交成功</h3>
          <p data-production-submit-success-message>{{ productionSubmitSuccessText }}</p>
        </div>
        <button
          type="button"
          data-production-submit-success-continue
          @click="closeProductionSubmitSuccessDialog"
        >
          <Icon icon="ep:right" :size="32" aria-hidden="true" />
          继续报工
        </button>
      </section>
    </div>

    <div
      v-if="activeProductionClearanceConfirmation && !isPqcMode"
      class="frontline-production-clearance-confirmation-modal"
      data-production-clearance-confirmation-dialog
      role="dialog"
      aria-modal="true"
      aria-labelledby="frontlineProductionClearanceConfirmationTitle"
      @click.self="closeProductionClearanceConfirmationDetail"
    >
      <section class="frontline-production-clearance-confirmation-dialog">
        <header class="frontline-production-clearance-confirmation-header">
          <span>清场确认</span>
          <h3 id="frontlineProductionClearanceConfirmationTitle">
            {{ activeProductionClearanceConfirmation.label }}
          </h3>
        </header>
        <p data-production-clearance-confirmation-description>
          {{ activeProductionClearanceConfirmation.description }}
        </p>
        <button
          type="button"
          data-production-clearance-confirmation-close
          @click="closeProductionClearanceConfirmationDetail"
        >
          知道了
        </button>
      </section>
    </div>

    <div
      v-if="activePicker && isPqcMode"
      class="frontline-picker picker"
      data-pqc-process-picker
      :class="{
        'frontline-picker--production-order': activePicker === 'order',
        'frontline-picker--production-process': activePicker === 'process'
      }"
      @click.self="closePicker"
    >
      <section class="frontline-picker__card picker-card">
        <div class="frontline-picker__heading">
          <h3 class="frontline-picker__title picker-title">
            {{
              isPqcMode
                ? activePicker === 'order'
                  ? '选择订单'
                  : activePicker === 'process' ? '选工序' : '选择员工'
                : activePicker === 'process' ? '选工序' : '选择员工'
            }}
          </h3>
          <input
            v-if="activePicker === 'order'"
            ref="activeOrderSearchInputRef"
            v-model="activeOrderKeyword"
            class="frontline-picker__order-search"
            type="search"
            data-pqc-order-search-input
            aria-label="输入订单号筛选活跃订单"
            placeholder="输入订单号"
            autocomplete="off"
            spellcheck="false"
            @keydown.enter="handleActiveOrderSearchEnter"
          />
        </div>
        <div class="frontline-picker__options picker-options">
          <p
            v-if="activePicker === 'order' && pickerOptions.length === 0"
            class="frontline-picker__empty"
            data-pqc-order-empty-state
            aria-live="polite"
          >
            {{ activeOrderPickerEmptyText }}
          </p>
          <button
            v-for="option in pickerOptions"
            :key="option.key"
            class="frontline-picker__option picker-option"
            type="button"
            :class="{ active: option.active }"
            :data-pqc-order-option="activePicker === 'order' ? 'true' : undefined"
            :aria-label="option.activeOrder
              ? `编码 ${option.activeOrder.workOrderCode}，产品 ${option.activeOrder.productName}，数量 ${formatProductionQuantity(option.activeOrder.quantity)}`
              : option.label"
            @click="option.onClick"
          >
            <span
              v-if="activePicker === 'order' && option.activeOrder"
              class="frontline-order-picker-option"
            >
              <span class="frontline-order-picker-option__row" data-pqc-order-option-code>
                <span>编码</span>
                <strong class="frontline-order-picker-option__value is-code">
                  {{ option.activeOrder.workOrderCode }}
                </strong>
              </span>
              <span class="frontline-order-picker-option__row" data-pqc-order-option-product>
                <span>产品</span>
                <strong class="frontline-order-picker-option__value">
                  {{ option.activeOrder.productName }}
                </strong>
              </span>
              <span class="frontline-order-picker-option__row" data-pqc-order-option-quantity>
                <span>数量</span>
                <strong class="frontline-order-picker-option__value">
                  {{ formatProductionQuantity(option.activeOrder.quantity) }}
                </strong>
              </span>
            </span>
            <span v-else>{{ option.label }}</span>
          </button>
        </div>
        <button class="frontline-picker__close picker-close" type="button" @click="closePicker">
          返回
        </button>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import {
  FRONTLINE_FIELD_CODES,
  FRONTLINE_PQC_RESULTS,
  FRONTLINE_TEMPLATE_CODES,
  FrontlineTemplateApi,
  type FrontlineTemplateCode,
  type FrontlineTemplateDefinitionVO,
  type FrontlineTemplatePayloadReqVO,
  type FrontlineTemplatePayloadVO
} from '@/api/mes/pro/feedbackFrontlineTemplate'
import {
  ProFeedbackApi,
  type FrontlineActiveOrderVO,
  type FrontlineDeviceRouteProcessVO,
  type FrontlineEmployeeCandidateVO,
  type FrontlinePqcEquipmentOptionVO,
  type FrontlinePqcInspectionItemVO,
  type FrontlinePqcProcessVO,
  type FrontlinePqcItemResultSubmitReqVO,
  type FrontlinePqcInspectionSubmitReqVO,
  type FrontlinePqcInspectionSubmitRespVO,
  type FrontlinePqcResultType,
  type FrontlinePqcTaskSummaryState,
  type FrontlinePqcTaskStatus,
  type FrontlinePqcTaskOptionVO,
  type FrontlineRuntimeDeviceVO,
  type FrontlineRuntimeDeviceParameterVO,
  type FrontlineRuntimeMaterialVO,
  type ProFrontlineDeviceParameterReadingReqVO,
  type ProFrontlineFeedbackSubmitRespVO,
  type ProFrontlineFeedbackSubmitReqVO,
  type ProFrontlineFeedbackMaterialReqVO,
  type ProFrontlineLossDetailReqVO,
  type ProFrontlineParameterStatus,
  type ProFrontlineSelectedDeviceReqVO
} from '@/api/mes/pro/feedback'
import { useUserStore } from '@/store/modules/user'
import {
  buildFrontlineTemplatePayload,
  createFrontlineDefaultValues,
  resetFrontlineTemplateDraftForContext,
  resolveFrontlineContextKey,
  type FrontlineTemplateContext,
  type FrontlineTemplateDraft
} from './frontlineTemplate'
import {
  isExecutableFrontlinePqcTaskOption,
  resolveFrontlinePqcTaskAvailabilityIssue
} from './frontlinePqcTaskAvailability'
import {
  FRONTLINE_PQC_NO_PENDING_ORDER_TEXT,
  FrontlinePqcStaleActiveOrderSelectionError,
  FrontlineProductionStaleActiveOrderSelectionError,
  buildFrontlineActiveOrderPickerKey,
  createFrontlineDeviceEmployeeState,
  invalidateFrontlinePqcProcessCacheForActiveOrder,
  isSameFrontlineActiveOrder,
  loadFrontlineProductionActiveOrders,
  loadFrontlinePqcActiveOrders,
  preloadFrontlineProductionRuntimeCache,
  preloadFrontlinePqcSwitchingCache,
  selectFrontlineProductionActiveOrder,
  selectFrontlineProcess,
  selectFrontlinePqcActiveOrder,
  selectFrontlinePqcProcess,
  switchFrontlineActualEmployee,
  switchFrontlinePqcActualEmployee
} from './frontlineDeviceEmployeeContext'

type PickerType = 'order' | 'process' | 'employee'
type InspectionType = 'FIRST' | 'PATROL' | 'FINAL'
type PqcInspectionItemKey = string
type PqcChoiceResult = '合格' | '不合格'
type PqcQuantityField = 'inspectionQuantity' | 'scrapQuantity'
type ProductionDefectKey = string
type ProductionDeviceParameterKey = string
type ProductionDeviceParameterDraft = Record<ProductionDeviceParameterKey, number | string | boolean | undefined>
type ProductionDeviceMeteringValidityDraft = Record<string, boolean | undefined>
type ProductionClearanceConfirmationKey = 'workplace' | 'validity' | 'material' | 'cleaning'
type ProductionMaterialDraftState = {
  outputQuantity?: number
  defectQuantities: Record<ProductionDefectKey, number>
  selectedDeviceKey?: string
  deviceParameters: Record<string, ProductionDeviceParameterDraft>
  deviceMeteringValidity: ProductionDeviceMeteringValidityDraft
}
type FrontlineEmployeeSwitchResult = {
  actualEmployeeId: number
  template?: {
    templateNo?: string
    templateType?: string
  }
}

const PQC_INSPECTION_TYPE_LABELS: Record<InspectionType, string> = {
  FIRST: '首检',
  PATROL: '巡检',
  FINAL: '末检'
}

const FRONTLINE_PRODUCTION_CLEARANCE_CONFIRMATIONS: readonly ProductionClearanceConfirmationItem[] = [
  {
    key: 'workplace',
    label: '清场',
    description:
      '工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；墙面、地面、天花板、灯具等环境清洁应符合《INT/PD/6.4工作环境控制程序》。具体清场标准及内容依据《INT/GL/7.5.8-03清场管理制度》执行。'
  },
  {
    key: 'validity',
    label: '效期',
    description: '是否在计量效期内。'
  },
  {
    key: 'material',
    label: '物料',
    description: '所有的物料转移到指定的区域存放并标识。'
  },
  {
    key: 'cleaning',
    label: '清洁',
    description:
      '按《INT/GL/7.5.8-03清场管理制度》、《INT/PD/6.4工作环境控制程序》规程执行清洁设备、工器具及环境。'
  }
]

const createDefaultProductionClearanceConfirmations = () =>
  Object.fromEntries(
    FRONTLINE_PRODUCTION_CLEARANCE_CONFIRMATIONS.map((confirmation) => [confirmation.key, true])
  ) as Record<ProductionClearanceConfirmationKey, boolean>

interface FrontlinePickerOption {
  key: string
  label: string
  active: boolean
  activeOrder?: FrontlineActiveOrderVO
  onClick: () => void | Promise<void>
}

interface ProductionDefectOption {
  key: ProductionDefectKey
  reasonId: number
  reasonCode: string
  label: string
}

interface ProductionMaterialOption extends FrontlineRuntimeMaterialVO {
  key: string
}

interface ProductionDeviceCard {
  key: string
  deviceId: number
  deviceCode?: string
  deviceName?: string
  label: string
  parameters: FrontlineRuntimeDeviceParameterVO[]
}

interface ProductionSelfCheckNarrative {
  title: string
  standard: string
  inspectionMethods: readonly string[]
}

const PRESSURE_PUMP_DETECTION_DEVICE_CODE = 'G01143'
const PRESSURE_PUMP_DETECTION_PROCESS_KEYWORD = '检测'
const PRODUCTION_DEVICE_METERING_VALIDITY_PARAMETER_CODES = new Set([
  'METERING_VALID',
  'METERING_VALIDITY_WITHIN_PERIOD'
])
const PRESSURE_PUMP_DETECTION_SELF_CHECK_NARRATIVE: ProductionSelfCheckNarrative = {
  title: '生产自检',
  standard: '压力泵整体外观无黑点、杂质、花纹、划痕等外观缺陷；气密性检测合格。',
  inspectionMethods: [
    '外观检测方法：对组装完成的球囊扩张压力泵产品进行外观检测，目测压力泵内无异物，仔细检测。',
    '气密性检测方法：',
    '1、低压检验：将整体组装检测合格的压力泵装在气密性检测工装上，通过长脚接头接上8atm气源，打开气源，观察压力表指针，应可以匀速指示到测试压力值，不应有升压缓慢或直接从低压跳到8atm现象，撤掉气源后，压力表应可以迅速回零；',
    '高压检测：将低压检测合格的压力泵装到气密性检测工装上，通用长脚接头接上30atm气源，打开气源，观察压力表应能匀速上升到指定压力，到达最大压力后10s内压力表指针应无跳压、降压的现象，撤掉气源后，压力表应可以迅速回零'
  ]
}

interface ProductionClearanceConfirmationItem {
  key: ProductionClearanceConfirmationKey
  label: string
  description: string
}

interface PqcInspectionItem {
  key: PqcInspectionItemKey
  itemName: string
  label: string
  type: 'number' | 'choice'
  inspectionMethod: string
  standardText: string
  acceptanceStandard: string
  processInspectionMethod: string
  inspectionTool: string | null
  samplingPlanText: string | null
  resultType: FrontlinePqcResultType
  standardLowerLimit?: number | string
  standardUpperLimit?: number | string
  standardUnit: string
  standardPrecision?: number
  equipmentRequired: boolean
  equipmentOptions: FrontlinePqcEquipmentOptionVO[]
  lastSelectedEquipmentId?: number
  lastSelectedEquipmentNumber?: string
  unit: string
  defaultValue: string
  step: number
}

interface PqcItemSelection {
  selectedEquipmentId?: number
  selectedEquipmentNumber?: string
}

type PqcTaskDraftKey = string

interface PqcTaskDraftState {
  inspectionQuantity?: number
  scrapQuantity?: number
}

type PqcTaskOptionSnapshot = FrontlinePqcTaskOptionVO & {
  inspectionType: InspectionType
  inspectionItems: NonNullable<FrontlinePqcTaskOptionVO['inspectionItems']>
}

const props = withDefaults(defineProps<{ mode?: 'production' | 'pqc' }>(), {
  mode: 'production'
})

const message = useMessage()

const PARAMETER_AUDIT_REASON_TEXT = {
  DEVICE_ID_MISSING: '参数读数缺少设备',
  PARAMETER_CODE_MISSING: '参数读数缺少参数编码',
  SELECTED_DEVICE_ID_MISSING: '本次报工未确定所选设备',
  DEVICE_MISMATCH: '参数读数设备与所选设备不一致',
  DUPLICATE_PARAMETER: '同一设备参数存在重复读数',
  RULE_NOT_FOUND: '冻结标准中找不到对应参数',
  CONTEXT_MISMATCH: '参数快照与本次报工上下文不一致',
  SNAPSHOT_MISSING_LEGACY: '历史订单缺少参数冻结快照',
  SNAPSHOT_HASH_MISMATCH: '参数冻结快照完整性校验失败'
} as const

const showParameterAuditWarning = (submitResult: ProFrontlineFeedbackSubmitRespVO) => {
  const unresolvedItems = submitResult.auditItems.filter(
    (item) => item.resolutionStatus === 'UNRESOLVED'
  )
  if (!unresolvedItems.length) {
    return
  }
  const warnings = unresolvedItems.map((item) => {
    const reasonCode = item.reasonCode
    if (!reasonCode || !(reasonCode in PARAMETER_AUDIT_REASON_TEXT)) {
      throw new Error(`未知设备参数审计原因：${reasonCode || 'EMPTY'}`)
    }
    return PARAMETER_AUDIT_REASON_TEXT[reasonCode as keyof typeof PARAMETER_AUDIT_REASON_TEXT]
  })
  message.warning(`报工已成功，设备参数需复核：${[...new Set(warnings)].join('；')}`)
}
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const catalog = ref<FrontlineTemplateDefinitionVO[]>([])
const payloadLoading = ref(false)
const payloadPreview = ref<FrontlineTemplatePayloadVO>()
const submitConfirmationOpen = ref(false)
const productionSubmitSuccessOpen = ref(false)
const frontlineErrorMessage = ref('')
const resolveErrorMessage = (error: unknown) => {
  if (typeof error === 'string' && error.trim()) {
    return error.trim()
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return '提交失败'
}
const showFrontlineError = (error: unknown) => {
  frontlineErrorMessage.value = resolveErrorMessage(error)
}
const clearFrontlineError = () => {
  frontlineErrorMessage.value = ''
}
const productionFormalSubmitConfirmationText = ref('')
const productionSignaturePassword = ref('')
const createProductionSubmitDraftKey = () =>
  `draft-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
const FRONTLINE_PRODUCTION_IDEMPOTENCY_KEY_MAX_LENGTH = 128
const productionSubmitDraftKey = ref(createProductionSubmitDraftKey())
let productionFormalSubmitConfirmationResolver: ((confirmed: boolean) => void) | undefined
const activePicker = ref<PickerType>()
const activeOrderKeyword = ref('')
const activeOrderSearchInputRef = ref<HTMLInputElement>()
const deviceState = reactive(createFrontlineDeviceEmployeeState())
const employeeTemplateCode = ref<FrontlineTemplateCode>()
const frontlinePanelRef = ref<HTMLElement>()
const isPqcFullscreen = ref(false)
const isProductionFullscreen = ref(false)
const pqcFullscreenActionText = computed(() =>
  isPqcFullscreen.value ? '主页' : '最大化'
)
const productionFullscreenActionText = computed(() =>
  isProductionFullscreen.value ? '主页' : '最大化'
)

const expectedTemplateCode = computed<FrontlineTemplateCode>(() =>
  props.mode === 'pqc'
    ? FRONTLINE_TEMPLATE_CODES.PQC_SIMPLIFIED
    : FRONTLINE_TEMPLATE_CODES.PRODUCTION_SIMPLIFIED
)

const context = reactive<FrontlineTemplateContext>({
  templateCode: expectedTemplateCode.value
})

const draft = reactive<FrontlineTemplateDraft>({
  fieldValues: createFrontlineDefaultValues(context.templateCode)
})

const productionDraft = reactive({
  outputQuantity: undefined as number | undefined
})

const productionDefectDraft = reactive<Record<ProductionDefectKey, number>>({})

const productionMaterialDrafts = reactive<Record<string, ProductionMaterialDraftState>>({})
const selectedProductionMaterialKey = ref<string>()
const selectedProductionDeviceKey = ref<string>()
const deviceParameterDraft = reactive<Record<string, ProductionDeviceParameterDraft>>({})
const deviceMeteringValidityDraft = reactive<ProductionDeviceMeteringValidityDraft>({})
const productionClearanceConfirmationDraft = reactive<Record<ProductionClearanceConfirmationKey, boolean>>(
  createDefaultProductionClearanceConfirmations()
)
const activeProductionClearanceConfirmationKey = ref<ProductionClearanceConfirmationKey>()

const pqcDraft = reactive({
  inspectionType: undefined as InspectionType | undefined,
  patrolRound: undefined as number | undefined,
  inspectionQuantity: undefined as number | undefined,
  scrapQuantity: undefined as number | undefined
})

const pqcTaskDrafts = reactive<Record<PqcTaskDraftKey, PqcTaskDraftState>>({})
const activePqcInspectionKey = ref<PqcInspectionItemKey>()
const activePqcStandardKey = ref<PqcInspectionItemKey>()
const activePqcMethodKey = ref<PqcInspectionItemKey>()
const selectedPqcInspectionKey = ref<PqcInspectionItemKey>()
const activePqcTaskOptionId = ref<number>()
const pqcPieceDraftValues = ref<string[]>([])
const pqcPieceValues = reactive<Record<string, string[]>>({})
const pqcItemSelections = reactive<Record<PqcInspectionItemKey, PqcItemSelection>>({})
const pqcSignatureDialogVisible = ref(false)
const pqcSignaturePassword = ref('')
const pqcSubmitResultUncertain = ref(false)

const isPqcMode = computed(() => props.mode === 'pqc')
const routeBatchExecutionId = computed(() =>
  firstRouteQueryNumber(['batchExecutionId', 'edhrBatchExecutionId'])
)
const PRODUCTION_CANVAS_WIDTH = 1920
const PRODUCTION_CANVAS_HEIGHT = 1080
const productionViewportScale = ref(1)
let productionViewportScaleFrame: number | undefined
let productionViewportResizeObserver: ResizeObserver | undefined
let processSelectionRequestId = 0
let activeOrderSelectionRequestId = 0
let productionEmployeeSelectionRequestId = 0
const productionStageStyle = computed(() => {
  const scale = productionViewportScale.value
  return {
    '--frontline-production-scale': String(scale),
    '--frontline-production-top-action-font-size': `${42 / scale}px`,
    '--frontline-production-footer-action-font-size': `${54 / scale}px`,
    width: `${PRODUCTION_CANVAS_WIDTH * scale}px`,
    height: `${PRODUCTION_CANVAS_HEIGHT * scale}px`
  }
})
const currentLoginUserId = computed(() => Number(userStore.getUser?.id || 0))
const currentLoginEmployeeCandidate = computed<FrontlineEmployeeCandidateVO | undefined>(() => {
  const userId = currentLoginUserId.value
  if (!userId) {
    return undefined
  }
  const username = userStore.getUser?.username?.trim()
  const nickname = userStore.getUser?.nickname?.trim()
  const displayName = username || nickname || String(userId)
  return {
    userId: currentLoginUserId.value,
    username,
    nickname: displayName,
    systemUserId: currentLoginUserId.value,
    employeeName: displayName,
    displayName,
    employeeType: 'FORMAL'
  }
})
const selectedActiveOrder = computed(() => deviceState.selectedActiveOrder)

const productionOrderLabel = computed(() => {
  const selectedOrder = selectedActiveOrder.value
  if (!selectedOrder) {
    return '未选择'
  }
  const workOrderCode = selectedOrder.workOrderCode?.trim()
  if (!workOrderCode) {
    throw new Error(`一线活跃订单缺少正式订单号：workOrderId=${selectedOrder.workOrderId}`)
  }
  return workOrderCode
})

const productionBatchCodeLabel = computed(() =>
  selectedActiveOrder.value?.batchCode?.trim() || ''
)

const productionProductNameLabel = computed(() =>
  selectedActiveOrder.value?.productName?.trim() || ''
)

const formatProductionQuantity = (quantity: number) => {
  if (!Number.isFinite(quantity) || quantity <= 0) {
    throw new Error(`PQC 活跃订单生产数量无效：${quantity}`)
  }
  return String(quantity)
}

const selectedOrderQuantityLabel = computed(() =>
  selectedActiveOrder.value
    ? formatProductionQuantity(selectedActiveOrder.value.quantity)
    : ''
)

const selectedProcessLabel = computed(() => formatProcessLabel(deviceState.selectedProcess))

const selectedEmployeeLabel = computed(() =>
  isPqcMode.value
    ? formatPqcLoginEmployeeLabel(deviceState.selectedEmployee || currentLoginEmployeeCandidate.value)
    : formatEmployeeLabel(deviceState.selectedEmployee)
)
const productionSubmitSuccessText = computed(() => `${selectedEmployeeLabel.value}提交成功`)

const productionScrapQuantity = computed(() =>
  configuredDefectReasons.value.reduce(
    (total, defect) => total + (productionDefectDraft[defect.key] || 0),
    0
  )
)

const pqcInspectionQuantity = computed(() =>
  normalizePqcQuantity(pqcDraft.inspectionQuantity)
)

const normalizePqcInspectionItemName = (itemName?: string) =>
  itemName?.trim() || ''

const isFrontlinePqcProcess = (
  process?: FrontlineDeviceRouteProcessVO | FrontlinePqcProcessVO
): process is FrontlinePqcProcessVO => Boolean(process && 'qaProcessId' in process)

const isFrontlineProductionProcess = (
  process?: FrontlineDeviceRouteProcessVO | FrontlinePqcProcessVO
): process is FrontlineDeviceRouteProcessVO => Boolean(process && 'routeProcessId' in process)

const hasPqcTaskOptionSnapshot = (
  option?: FrontlinePqcTaskOptionVO
): option is PqcTaskOptionSnapshot => Boolean(
  option &&
  option.taskStatus === 'PENDING' &&
  isExecutableFrontlinePqcTaskOption(option)
)

const getPqcTaskOptions = (process?: FrontlinePqcProcessVO) =>
  (process?.pqcTaskOptions || []).filter(hasPqcTaskOptionSnapshot)

const mapPqcInspectionItem = (item: FrontlinePqcInspectionItemVO): PqcInspectionItem => ({
    key: item.itemCode,
    itemName: normalizePqcInspectionItemName(item.itemName),
    label: normalizePqcInspectionItemName(item.itemName) || '未配置检验项目名称',
    type: isPqcNumericResultType(item.resultType) ? 'number' : 'choice',
    inspectionMethod: item.inspectionMethod || '',
    standardText: item.standardText || '',
    acceptanceStandard: item.acceptanceStandard || item.standardText || '',
    processInspectionMethod: item.processInspectionMethod || item.inspectionMethod || '',
    inspectionTool: item.inspectionTool,
    samplingPlanText: item.samplingPlanText,
    resultType: item.resultType,
    standardLowerLimit: item.standardLowerLimit,
    standardUpperLimit: item.standardUpperLimit,
    standardUnit: item.standardUnit || '',
    standardPrecision: item.standardPrecision,
    equipmentRequired: item.equipmentRequired === true,
    equipmentOptions: item.equipmentOptions || [],
    lastSelectedEquipmentId: item.lastSelectedEquipmentId,
    lastSelectedEquipmentNumber: item.lastSelectedEquipmentNumber,
    unit: item.standardUnit || '',
    defaultValue: isPqcNumericResultType(item.resultType)
      ? (item.standardLowerLimit === undefined || item.standardLowerLimit === null
        ? ''
        : String(item.standardLowerLimit))
      : '合格',
    step: resolvePqcNumericStep(item.standardPrecision, item.resultType)
})

const normalizePqcTaskOptionItemKey = (option?: PqcTaskOptionSnapshot) =>
  option?.qaItemCode?.trim() ||
  (option?.inspectionItems.length === 1 ? option.inspectionItems[0]?.itemCode : undefined)

const pqcTaskOptionIncludesItem = (
  option: PqcTaskOptionSnapshot,
  itemKey?: PqcInspectionItemKey
) => !itemKey ||
  option.qaItemCode?.trim() === itemKey ||
  option.inspectionItems.some((item) => item.itemCode === itemKey)

const resolvePqcProcessItemKey = (
  process: FrontlinePqcProcessVO,
  itemKey?: PqcInspectionItemKey
) => process.inspectionItems.some((item) => item.itemCode === itemKey)
  ? itemKey
  : undefined

const resolveSelectedPqcInspectionItemKey = (process: FrontlinePqcProcessVO) => {
  const selectedKey = resolvePqcProcessItemKey(process, selectedPqcInspectionKey.value)
  if (selectedKey) {
    return selectedKey
  }
  const selectedTask = activePqcTaskOptionId.value
    ? getPqcTaskOptions(process).find((option) => option.pqcTaskId === activePqcTaskOptionId.value)
    : undefined
  const taskItemKey = resolvePqcProcessItemKey(process, normalizePqcTaskOptionItemKey(selectedTask))
  return taskItemKey || process.inspectionItems[0]?.itemCode
}

const getPqcTaskOptionsForInspectionItem = (
  process: FrontlinePqcProcessVO,
  itemKey?: PqcInspectionItemKey
) => getPqcTaskOptions(process).filter((option) => pqcTaskOptionIncludesItem(option, itemKey))

const preferPqcTaskOption = (
  options: PqcTaskOptionSnapshot[],
  preferredInspectionType?: InspectionType
) => {
  const orderedTypes: InspectionType[] = preferredInspectionType
    ? [preferredInspectionType, 'FIRST', 'PATROL', 'FINAL']
    : ['FIRST', 'PATROL', 'FINAL']
  for (const inspectionType of orderedTypes) {
    const option = options.find((candidate) => candidate.inspectionType === inspectionType)
    if (option) {
      return option
    }
  }
  return options[0]
}

const getDefaultPqcTaskOption = (process: FrontlinePqcProcessVO) => {
  const selectedItemKey = resolveSelectedPqcInspectionItemKey(process)
  return preferPqcTaskOption(getPqcTaskOptionsForInspectionItem(process, selectedItemKey))
}

const getSelectedPqcTaskOption = (process: FrontlinePqcProcessVO) => {
  const selectedItemKey = resolveSelectedPqcInspectionItemKey(process)
  const selectedTaskId = activePqcTaskOptionId.value
  const selectedTask = selectedTaskId
    ? getPqcTaskOptions(process).find((option) =>
      option.pqcTaskId === selectedTaskId && pqcTaskOptionIncludesItem(option, selectedItemKey)
    )
    : undefined
  return selectedTask || getDefaultPqcTaskOption(process)
}

const pqcInspectionTypeTabs = computed<{ type: InspectionType; label: string }[]>(() => {
  const process = deviceState.selectedProcess
  if (!isFrontlinePqcProcess(process)) {
    return []
  }
  const seenTypes = new Set<InspectionType>()
  return getPqcTaskOptionsForInspectionItem(process, activePqcTabKey.value)
    .reduce<{ type: InspectionType; label: string }[]>((tabs, option) => {
      if (seenTypes.has(option.inspectionType)) {
        return tabs
      }
      seenTypes.add(option.inspectionType)
      tabs.push({
        type: option.inspectionType,
        label: PQC_INSPECTION_TYPE_LABELS[option.inspectionType]
      })
      return tabs
    }, [])
})

const activePqcTaskOption = computed<PqcTaskOptionSnapshot | undefined>(() => {
  const process = deviceState.selectedProcess
  if (!isFrontlinePqcProcess(process)) {
    return undefined
  }
  return getSelectedPqcTaskOption(process)
})

const pqcTaskAvailabilityIssue = computed(() =>
  isFrontlinePqcProcess(deviceState.selectedProcess)
    ? resolveFrontlinePqcTaskAvailabilityIssue(deviceState.selectedProcess)
    : undefined
)

const withPqcTaskOption = (
  process: FrontlinePqcProcessVO,
  option: PqcTaskOptionSnapshot | undefined
): FrontlinePqcProcessVO => {
  if (!option) {
    return process
  }
  return {
    ...process,
    regulationVersionId: option.regulationVersionId,
    qaProcessId: option.qaProcessId,
    pqcTaskOptions: process.pqcTaskOptions.map((taskOption) =>
      taskOption.pqcTaskId === option.pqcTaskId ? { ...taskOption, ...option } : taskOption
    )
  }
}

const pqcInspectionItems = computed<PqcInspectionItem[]>(() =>
  isFrontlinePqcProcess(deviceState.selectedProcess)
    ? deviceState.selectedProcess.inspectionItems.map(mapPqcInspectionItem)
    : []
)

const pqcTaskInspectionItems = computed<PqcInspectionItem[]>(() =>
  (activePqcTaskOption.value?.inspectionItems || []).map(mapPqcInspectionItem)
)

const pqcInspectionItemMap = computed<Record<PqcInspectionItemKey, PqcInspectionItem>>(() =>
  pqcInspectionItems.value.reduce<Record<PqcInspectionItemKey, PqcInspectionItem>>((items, item) => {
    items[item.key] = item
    return items
  }, {})
)

const pqcInspectionItemKeys = computed<PqcInspectionItemKey[]>(() =>
  pqcTaskInspectionItems.value.map((item) => item.key)
)

const activePqcInspectionItem = computed(() =>
  activePqcInspectionKey.value
    ? pqcInspectionItemMap.value[activePqcInspectionKey.value]
    : undefined
)

const activePqcTabKey = computed(() => {
  const selectedKey = selectedPqcInspectionKey.value
  if (selectedKey && pqcInspectionItemMap.value[selectedKey]) {
    return selectedKey
  }
  const process = deviceState.selectedProcess
  if (isFrontlinePqcProcess(process)) {
    const taskItemKey = resolvePqcProcessItemKey(
      process,
      normalizePqcTaskOptionItemKey(activePqcTaskOption.value)
    )
    if (taskItemKey) {
      return taskItemKey
    }
  }
  return pqcInspectionItems.value[0]?.key
})

const activePqcTabItem = computed(() =>
  activePqcTabKey.value
    ? pqcInspectionItemMap.value[activePqcTabKey.value]
    : undefined
)

const activePqcStandardItem = computed(() =>
  activePqcStandardKey.value
    ? pqcInspectionItemMap.value[activePqcStandardKey.value]
    : undefined
)

const activePqcMethodItem = computed(() =>
  activePqcMethodKey.value
    ? pqcInspectionItemMap.value[activePqcMethodKey.value]
    : undefined
)

const pqcVisibleRounds = computed(() => {
  if (!isFrontlinePqcProcess(deviceState.selectedProcess) || !pqcDraft.inspectionType) {
    return []
  }
  return getPqcTaskOptions(deviceState.selectedProcess)
    .filter((option) =>
      option.inspectionType === pqcDraft.inspectionType &&
      pqcTaskOptionIncludesItem(option, activePqcTabKey.value)
    )
    .map((option) => ({
      value: option.pqcTaskId,
      label: formatPqcTaskOptionLabel(option)
    }))
})

const templateModeMismatch = computed(() =>
  Boolean(employeeTemplateCode.value && employeeTemplateCode.value !== expectedTemplateCode.value)
)

const templateBindingMissing = computed(() =>
  Boolean(deviceState.selectedEmployee && !employeeTemplateCode.value)
)

const isSubmitBlocked = computed(() =>
  payloadLoading.value ||
  submitConfirmationOpen.value ||
  productionSubmitSuccessOpen.value ||
  templateModeMismatch.value ||
  templateBindingMissing.value ||
  !deviceState.selectedActiveOrder ||
  (isPqcMode.value && !hasPqcTaskSnapshot(deviceState.selectedProcess)) ||
  !deviceState.selectedProcess ||
  !deviceState.selectedEmployee
)

const isPqcSubmitBlocked = computed(() =>
  payloadLoading.value ||
  pqcSubmitResultUncertain.value ||
  deviceState.loadingEmployees ||
  deviceState.loadingTemplate
)

const statusText = computed(() => {
  if (deviceState.lastError) {
    return deviceState.lastError
  }
  if (isPqcMode.value && !deviceState.selectedActiveOrder) {
    return deviceState.activeOrderOptions.length === 0
      ? FRONTLINE_PQC_NO_PENDING_ORDER_TEXT
      : '请选择待检工单'
  }
  if (!deviceState.selectedProcess) {
    return '请选择工序'
  }
  if (isPqcMode.value && !hasPqcTaskSnapshot(deviceState.selectedProcess)) {
    return '当前工序缺少PQC任务或QA规程快照'
  }
  if (!deviceState.selectedEmployee) {
    return '请选择员工'
  }
  if (templateBindingMissing.value) {
    return '当前员工缺少一线填写模板'
  }
  if (templateModeMismatch.value) {
    return `当前员工绑定的是${formatTemplateName(employeeTemplateCode.value)}，请切换${formatTemplateName(expectedTemplateCode.value)}员工`
  }
  return '准备提交'
})

const pqcInspectionEmptyText = computed(() => {
  if (deviceState.loadingProcesses || deviceState.loadingTemplate) {
    return '正在加载正式检验项目'
  }
  if (deviceState.lastError) {
    return deviceState.lastError
  }
  if (!deviceState.selectedActiveOrder) {
    return '请先选择活跃订单'
  }
  if (!deviceState.selectedProcess) {
    return '请先选择PQC工序'
  }
  if (pqcTaskAvailabilityIssue.value?.message) {
    return pqcTaskAvailabilityIssue.value.message
  }
  if (!activePqcTaskOption.value) {
    return pqcTaskAvailabilityIssue.value?.message || '当前工序缺少PQC任务或QA规程快照'
  }
  return '当前工序缺少发布态QA检验项目'
})

const configuredDefectReasons = computed<ProductionDefectOption[]>(() =>
  (deviceState.runtimeConfig?.defectReasons || []).map((reason) => ({
    key: String(reason.reasonId),
    reasonId: reason.reasonId,
    reasonCode: reason.reasonCode,
    label: reason.reasonName
  }))
)

const configuredProductionMaterials = computed<ProductionMaterialOption[]>(() =>
  (deviceState.runtimeConfig?.materials || []).map((material) => ({
    ...material,
    key: String(material.materialId)
  }))
)

const resolveProductionDeviceTabLabel = (
  device: Pick<FrontlineRuntimeDeviceVO, 'deviceCode'>
) => {
  const deviceCode = device.deviceCode?.trim()
  if (!deviceCode) {
    throw new Error('当前设备缺少正式设备编号，不能渲染填设备卡片')
  }
  return deviceCode
}

const configuredDeviceCards = computed<ProductionDeviceCard[]>(() =>
  (deviceState.runtimeConfig?.devices || [])
    .filter((device) => Number(device.deviceId || 0) > 0)
    .map((device) => ({
      key: String(device.deviceId),
      deviceId: device.deviceId,
      deviceCode: device.deviceCode,
      deviceName: device.deviceName,
      label: resolveProductionDeviceTabLabel(device),
      parameters: device.parameters || []
    }))
)

const isTextStandardParameter = (parameter: FrontlineRuntimeDeviceParameterVO) =>
  parameter.valueType === 'TEXT_STANDARD'

const isSelectParameter = (parameter: FrontlineRuntimeDeviceParameterVO) =>
  parameter.valueType === 'SELECT'

const isBooleanParameter = (parameter: FrontlineRuntimeDeviceParameterVO) =>
  parameter.valueType === 'BOOLEAN'

const isProductionDeviceMeteringValidityParameter = (
  parameter: FrontlineRuntimeDeviceParameterVO
) =>
  PRODUCTION_DEVICE_METERING_VALIDITY_PARAMETER_CODES.has(
    parameter.parameterCode.trim().toUpperCase()
  )

const isNumericProductionParameter = (parameter: FrontlineRuntimeDeviceParameterVO) =>
  !isTextStandardParameter(parameter) &&
  !isSelectParameter(parameter) &&
  !isBooleanParameter(parameter)

const resolveProductionBooleanParameterDefault = (
  parameter: FrontlineRuntimeDeviceParameterVO
) => {
  if (parameter.defaultValue === undefined || parameter.defaultValue === null) {
    throw new Error(`BOOLEAN 设备参数缺少 0/1 默认值：${parameter.parameterCode}`)
  }
  const defaultValue = Number(parameter.defaultValue)
  if (defaultValue !== 0 && defaultValue !== 1) {
    throw new Error(`BOOLEAN 设备参数缺少 0/1 默认值：${parameter.parameterCode}`)
  }
  return defaultValue === 1
}

const visibleDeviceCards = computed(() => configuredDeviceCards.value)

const getProductionDeviceMeteringValidityParameters = (device?: ProductionDeviceCard) =>
  (device?.parameters || []).filter(isProductionDeviceMeteringValidityParameter)

const syncProductionDeviceMeteringValidityParameterDraft = (
  deviceKey: string,
  checked: boolean
) => {
  const device = visibleDeviceCards.value.find((item) => item.key === deviceKey)
  if (!device) {
    return
  }
  if (!deviceParameterDraft[deviceKey]) {
    deviceParameterDraft[deviceKey] = {}
  }
  const params = deviceParameterDraft[deviceKey]
  for (const parameter of getProductionDeviceMeteringValidityParameters(device)) {
    params[parameter.parameterCode] = checked
  }
}

const syncProductionDeviceMeteringValidityDraft = (devices: ProductionDeviceCard[]) => {
  const visibleKeys = new Set(devices.map((device) => device.key))
  for (const deviceKey of Object.keys(deviceMeteringValidityDraft)) {
    if (!visibleKeys.has(deviceKey)) {
      delete deviceMeteringValidityDraft[deviceKey]
    }
  }
  for (const device of devices) {
    if (deviceMeteringValidityDraft[device.key] === undefined) {
      deviceMeteringValidityDraft[device.key] = true
    }
    syncProductionDeviceMeteringValidityParameterDraft(
      device.key,
      deviceMeteringValidityDraft[device.key] !== false
    )
  }
}

const syncProductionDeviceParameterDraft = (devices: ProductionDeviceCard[]) => {
  const visibleKeys = new Set(devices.map((device) => device.key))
  for (const deviceKey of Object.keys(deviceParameterDraft)) {
    if (!visibleKeys.has(deviceKey)) {
      delete deviceParameterDraft[deviceKey]
    }
  }
  for (const device of devices) {
    if (!deviceParameterDraft[device.key]) {
      deviceParameterDraft[device.key] = {}
    }
    const params = deviceParameterDraft[device.key]
    for (const parameter of getProductionSubmittableParameters(device)) {
      if (!parameter.parameterCode || isTextStandardParameter(parameter)) {
        continue
      }
      if (params[parameter.parameterCode] !== undefined) {
        continue
      }
      if (isSelectParameter(parameter)) {
        const defaultText = parameter.defaultText?.trim()
        if (defaultText) {
          params[parameter.parameterCode] = defaultText
        }
        continue
      }
      if (isBooleanParameter(parameter)) {
        params[parameter.parameterCode] = resolveProductionBooleanParameterDefault(parameter)
        continue
      }
      if (parameter.defaultValue !== undefined && parameter.defaultValue !== null) {
        params[parameter.parameterCode] = normalizeProductionParameter(parameter, parameter.defaultValue)
      }
    }
  }
}

const resetProductionDeviceParameterDraft = () => {
  for (const deviceKey of Object.keys(deviceParameterDraft)) {
    delete deviceParameterDraft[deviceKey]
  }
  syncProductionDeviceParameterDraft(visibleDeviceCards.value)
}

const resetProductionDeviceMeteringValidityDraft = () => {
  for (const deviceKey of Object.keys(deviceMeteringValidityDraft)) {
    delete deviceMeteringValidityDraft[deviceKey]
  }
  syncProductionDeviceMeteringValidityDraft(visibleDeviceCards.value)
}

const cloneProductionDeviceParameters = (
  source: Record<string, ProductionDeviceParameterDraft>
) => Object.fromEntries(
  Object.entries(source).map(([deviceKey, parameters]) => [deviceKey, { ...parameters }])
) as Record<string, ProductionDeviceParameterDraft>

function replaceReactiveRecord<T>(target: Record<string, T>, source: Record<string, T>) {
  for (const key of Object.keys(target)) {
    delete target[key]
  }
  Object.assign(target, source)
}

const createProductionMaterialDraftState = (): ProductionMaterialDraftState => ({
  outputQuantity: undefined,
  defectQuantities: Object.fromEntries(
    configuredDefectReasons.value.map((defect) => [defect.key, 0])
  ),
  selectedDeviceKey: visibleDeviceCards.value[0]?.key,
  deviceParameters: cloneProductionDeviceParameters(deviceParameterDraft),
  deviceMeteringValidity: { ...deviceMeteringValidityDraft }
})

const persistActiveProductionMaterialDraft = () => {
  const materialKey = selectedProductionMaterialKey.value
  if (!materialKey || !productionMaterialDrafts[materialKey]) {
    return
  }
  productionMaterialDrafts[materialKey] = {
    outputQuantity: productionDraft.outputQuantity,
    defectQuantities: { ...productionDefectDraft },
    selectedDeviceKey: selectedProductionDeviceKey.value,
    deviceParameters: cloneProductionDeviceParameters(deviceParameterDraft),
    deviceMeteringValidity: { ...deviceMeteringValidityDraft }
  }
}

const restoreProductionMaterialDraft = (materialKey: string) => {
  const materialDraft = productionMaterialDrafts[materialKey]
  if (!materialDraft) {
    throw new Error(`报工物料草稿不存在：${materialKey}`)
  }
  productionDraft.outputQuantity = materialDraft.outputQuantity
  replaceReactiveRecord(productionDefectDraft, { ...materialDraft.defectQuantities })
  for (const defect of configuredDefectReasons.value) {
    if (productionDefectDraft[defect.key] === undefined) {
      productionDefectDraft[defect.key] = 0
    }
  }
  replaceReactiveRecord(
    deviceParameterDraft,
    cloneProductionDeviceParameters(materialDraft.deviceParameters)
  )
  syncProductionDeviceParameterDraft(visibleDeviceCards.value)
  replaceReactiveRecord(deviceMeteringValidityDraft, { ...materialDraft.deviceMeteringValidity })
  syncProductionDeviceMeteringValidityDraft(visibleDeviceCards.value)
  selectedProductionDeviceKey.value = visibleDeviceCards.value.some(
    (device) => device.key === materialDraft.selectedDeviceKey
  )
    ? materialDraft.selectedDeviceKey
    : visibleDeviceCards.value[0]?.key
}

const clearProductionMaterialDrafts = () => {
  for (const materialKey of Object.keys(productionMaterialDrafts)) {
    delete productionMaterialDrafts[materialKey]
  }
  selectedProductionMaterialKey.value = undefined
  productionDraft.outputQuantity = undefined
  replaceReactiveRecord(productionDefectDraft, {})
  resetProductionDeviceParameterDraft()
  resetProductionDeviceMeteringValidityDraft()
}

const syncProductionMaterialDrafts = (materials: ProductionMaterialOption[]) => {
  if (!materials.length) {
    clearProductionMaterialDrafts()
    return
  }
  const materialKeys = new Set(materials.map((material) => material.key))
  for (const materialKey of Object.keys(productionMaterialDrafts)) {
    if (!materialKeys.has(materialKey)) {
      delete productionMaterialDrafts[materialKey]
    }
  }
  for (const material of materials) {
    if (!productionMaterialDrafts[material.key]) {
      productionMaterialDrafts[material.key] = createProductionMaterialDraftState()
    }
  }
  const activeMaterialKey = selectedProductionMaterialKey.value
  selectedProductionMaterialKey.value = activeMaterialKey && materialKeys.has(activeMaterialKey)
    ? activeMaterialKey
    : materials[0].key
  restoreProductionMaterialDraft(selectedProductionMaterialKey.value)
}

const resetProductionMaterialDrafts = () => {
  clearProductionMaterialDrafts()
  syncProductionMaterialDrafts(configuredProductionMaterials.value)
}

const switchProductionMaterial = (materialKey: string) => {
  if (materialKey === selectedProductionMaterialKey.value) {
    return
  }
  if (!productionMaterialDrafts[materialKey]) {
    throw new Error(`报工物料不属于当前冻结工序：${materialKey}`)
  }
  persistActiveProductionMaterialDraft()
  selectedProductionMaterialKey.value = materialKey
  restoreProductionMaterialDraft(materialKey)
}

const isProductionMaterialCompletionEntered = (materialKey: string) =>
  materialKey === selectedProductionMaterialKey.value
    ? productionDraft.outputQuantity !== undefined
    : productionMaterialDrafts[materialKey]?.outputQuantity !== undefined

const activeProductionDevice = computed(() =>
  visibleDeviceCards.value.find((device) => device.key === selectedProductionDeviceKey.value) ||
  visibleDeviceCards.value[0]
)

const isProductionSelfCheckNarrativeDevice = (device?: ProductionDeviceCard) =>
  Boolean(
    device?.deviceCode?.trim() === PRESSURE_PUMP_DETECTION_DEVICE_CODE &&
    selectedProcessLabel.value.includes(PRESSURE_PUMP_DETECTION_PROCESS_KEYWORD)
  )

const activeProductionSelfCheckNarrative = computed(() =>
  isProductionSelfCheckNarrativeDevice(activeProductionDevice.value)
    ? PRESSURE_PUMP_DETECTION_SELF_CHECK_NARRATIVE
    : undefined
)

const getProductionSubmittableParameters = (device?: ProductionDeviceCard) => {
  if (!device) {
    return []
  }
  if (isProductionSelfCheckNarrativeDevice(device)) {
    return []
  }
  return device.parameters
}

const getProductionDeviceDetailParameters = (device?: ProductionDeviceCard) =>
  getProductionSubmittableParameters(device).filter(
    (parameter) => !isProductionDeviceMeteringValidityParameter(parameter)
  )

const activeProductionClearanceConfirmation = computed(() =>
  activeProductionClearanceConfirmationKey.value
    ? FRONTLINE_PRODUCTION_CLEARANCE_CONFIRMATIONS.find(
        (confirmation) => confirmation.key === activeProductionClearanceConfirmationKey.value
      )
    : undefined
)

const switchableProcessOptions = computed(() => {
  const seen = new Set<string>()
  return deviceState.processOptions.filter((process) => {
    const key = isFrontlinePqcProcess(process)
      ? `QA-${process.regulationVersionId}-${process.qaProcessId}`
      : `MES-${process.activeOrderId}-${process.routeId}-${process.routeProcessId}-${process.processId}`
    if (seen.has(key)) {
      return false
    }
    seen.add(key)
    return true
  })
})

const switchablePqcProcessOptions = computed(() =>
  switchableProcessOptions.value.filter(isFrontlinePqcProcess)
)

const normalizeActiveOrderKeyword = (value?: string) => (value || '').trim().toLocaleUpperCase()

const filteredActiveOrderOptions = computed(() => {
  const keyword = normalizeActiveOrderKeyword(activeOrderKeyword.value)
  if (!keyword) {
    return deviceState.activeOrderOptions
  }
  return deviceState.activeOrderOptions.filter((order) =>
    normalizeActiveOrderKeyword(order.workOrderCode).includes(keyword)
  )
})

const activeOrderPickerEmptyText = computed(() => {
  if (deviceState.loadingActiveOrders) {
    return isPqcMode.value ? '待检工单加载中' : '活跃订单加载中'
  }
  if (deviceState.lastError) {
    return deviceState.lastError
  }
  if (deviceState.activeOrderOptions.length === 0) {
    return isPqcMode.value ? FRONTLINE_PQC_NO_PENDING_ORDER_TEXT : '当前暂无活跃订单'
  }
  return isPqcMode.value ? '未找到匹配的待检工单' : '未找到匹配的活跃订单'
})

const pickerOptions = computed<FrontlinePickerOption[]>(() => {
  if (activePicker.value === 'order') {
    return filteredActiveOrderOptions.value.map((order) => ({
      key: buildFrontlineActiveOrderPickerKey(order),
      label: formatActiveOrderLabel(order),
      active: isSameFrontlineActiveOrder(order, deviceState.selectedActiveOrder),
      activeOrder: order,
      onClick: () => handleSelectActiveOrder(order)
    }))
  }
  if (activePicker.value === 'process') {
    return switchableProcessOptions.value.map((process) => ({
      key: isFrontlinePqcProcess(process)
        ? `QA-${process.regulationVersionId}-${process.qaProcessId}`
        : `MES-${process.activeOrderId}-${process.routeId}-${process.routeProcessId}-${process.processId}`,
      label: formatProcessLabel(process),
      active: isSameProcess(process, deviceState.selectedProcess),
      onClick: () => handlePickerProcessClick(process)
    }))
  }
  if (activePicker.value === 'employee') {
    return deviceState.employeeOptions.map((employee) => ({
      key: String(employee.userId),
      label: formatEmployeeLabel(employee),
      active: employee.userId === deviceState.selectedEmployee?.userId,
      onClick: () => handleSelectEmployee(employee)
    }))
  }
  return []
})

const pickerStatusText = computed(() => {
  const picker = activePicker.value
  if (picker !== 'process' && picker !== 'employee') {
    return ''
  }
  if (deviceState.lastError) {
    return deviceState.lastError
  }
  if (picker === 'process') {
    if (deviceState.loadingProcesses) {
      return '工序加载中'
    }
    return pickerOptions.value.length === 0 ? '暂无可用工序' : ''
  }
  if (!deviceState.selectedProcess) {
    return deviceState.loadingProcesses ? '工序加载中' : '请先选择工序'
  }
  if (deviceState.loadingEmployees) {
    return '员工加载中'
  }
  return pickerOptions.value.length === 0 ? '当前工序暂无可选员工' : ''
})

const frontlineContextKey = computed(() => resolveFrontlineContextKey(context))

watch(
  expectedTemplateCode,
  (templateCode) => {
    context.templateCode = templateCode
    Object.assign(draft.fieldValues, createFrontlineDefaultValues(templateCode))
    resetProductionMaterialDrafts()
    resetProductionDeviceMeteringValidityDraft()
    resetProductionClearanceConfirmations()
    payloadPreview.value = undefined
  },
  { flush: 'sync' }
)

watch(
  frontlineContextKey,
  (nextKey, previousKey) => {
    const changed = resetFrontlineTemplateDraftForContext(previousKey, nextKey, draft)
    if (changed) {
      Object.assign(draft.fieldValues, createFrontlineDefaultValues(context.templateCode))
      payloadPreview.value = undefined
      productionSubmitDraftKey.value = createProductionSubmitDraftKey()
      resetProductionMaterialDrafts()
      resetProductionDeviceMeteringValidityDraft()
      resetProductionClearanceConfirmations()
      pqcSubmitResultUncertain.value = false
    }
  },
  { flush: 'sync' }
)

watch(
  visibleDeviceCards,
  (devices) => {
    syncProductionDeviceParameterDraft(devices)
    syncProductionDeviceMeteringValidityDraft(devices)
    if (!devices.length) {
      selectedProductionDeviceKey.value = undefined
      return
    }
    if (!devices.some((device) => device.key === selectedProductionDeviceKey.value)) {
      selectedProductionDeviceKey.value = devices[0].key
    }
  },
  { immediate: true }
)

watch(
  configuredDefectReasons,
  (defects) => {
    const configuredKeys = new Set(defects.map((defect) => defect.key))
    for (const key of Object.keys(productionDefectDraft)) {
      if (!configuredKeys.has(key)) {
        delete productionDefectDraft[key]
      }
    }
    for (const defect of defects) {
      if (productionDefectDraft[defect.key] === undefined) {
        productionDefectDraft[defect.key] = 0
      }
    }
  },
  { immediate: true }
)

watch(
  configuredProductionMaterials,
  (materials) => syncProductionMaterialDrafts(materials),
  { immediate: true }
)

watch(
  [productionDraft, configuredDeviceCards, deviceParameterDraft, productionDefectDraft],
  () => {
    if (!isPqcMode.value) {
      Object.assign(draft.fieldValues, buildProductionFieldValues())
    }
  },
  { deep: true }
)

watch(
  [pqcDraft, pqcPieceValues],
  () => {
    if (isPqcMode.value) {
      Object.assign(draft.fieldValues, buildPqcFieldValues())
    }
  },
  { deep: true }
)

const normalizeProductionQuantity = (value: unknown) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    return 0
  }
  return Math.max(0, Math.trunc(parsed))
}

function normalizeProductionParameter(parameter: FrontlineRuntimeDeviceParameterVO, value: unknown) {
  if (value === undefined || value === null || String(value).trim() === '') {
    return undefined
  }
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    return undefined
  }
  const normalized = parameter.valueType === 'INTEGER' ? Math.trunc(parsed) : parsed
  const decimalScale = toFiniteProductionParameterNumber(parameter.decimalScale)
  const scaled = parameter.valueType === 'DECIMAL' &&
    decimalScale !== undefined &&
    Number.isInteger(decimalScale)
    ? Number(normalized.toFixed(decimalScale))
    : normalized
  return Math.max(0, scaled)
}

const toFiniteProductionParameterNumber = (value: unknown) => {
  if (value === undefined || value === null || String(value).trim() === '') {
    return undefined
  }
  const parsed = Number(String(value).replace(/,/g, '').trim())
  return Number.isFinite(parsed) ? parsed : undefined
}

const formatProductionParameterLimitValue = (value: unknown) =>
  String(value).replace(/,/g, '').trim()

const formatProductionParameterTargetRange = (parameter: FrontlineRuntimeDeviceParameterVO) => {
  if (!isNumericProductionParameter(parameter)) {
    return undefined
  }
  const lowerLimit = toFiniteProductionParameterNumber(parameter.lowerLimit)
  const upperLimit = toFiniteProductionParameterNumber(parameter.upperLimit)
  if (lowerLimit === undefined && upperLimit === undefined) {
    return undefined
  }
  const unit = parameter.unit ? ` ${parameter.unit}` : ''
  if (lowerLimit !== undefined && upperLimit !== undefined) {
    return `目标范围：${formatProductionParameterLimitValue(parameter.lowerLimit)} - ${formatProductionParameterLimitValue(parameter.upperLimit)}${unit}`
  }
  if (lowerLimit !== undefined) {
    return `目标范围：≥ ${formatProductionParameterLimitValue(parameter.lowerLimit)}${unit}`
  }
  if (upperLimit !== undefined) {
    return `目标范围：≤ ${formatProductionParameterLimitValue(parameter.upperLimit)}${unit}`
  }
  return undefined
}

const resolveProductionParameterStatus = (
  value: unknown,
  parameter: FrontlineRuntimeDeviceParameterVO
): ProFrontlineParameterStatus => {
  if (!isNumericProductionParameter(parameter)) {
    return 'NORMAL'
  }
  const numericValue = toFiniteProductionParameterNumber(value)
  if (numericValue === undefined) {
    return 'NORMAL'
  }
  const lowerLimit = toFiniteProductionParameterNumber(parameter.lowerLimit)
  const upperLimit = toFiniteProductionParameterNumber(parameter.upperLimit)
  if (lowerLimit !== undefined && numericValue < lowerLimit) {
    return 'BELOW_LOWER'
  }
  if (upperLimit !== undefined && numericValue > upperLimit) {
    return 'ABOVE_UPPER'
  }
  return 'NORMAL'
}

const updateProductionOutputQuantity = (event: Event) => {
  const value = (event.target as HTMLInputElement).value.trim()
  productionDraft.outputQuantity = value === '' ? undefined : normalizeProductionQuantity(value)
}

const adjustProductionOutputQuantity = (delta: number) => {
  productionDraft.outputQuantity = normalizeProductionQuantity(productionDraft.outputQuantity) + delta
  if (productionDraft.outputQuantity < 0) {
    productionDraft.outputQuantity = 0
  }
}

const getProductionDefectQuantity = (defectKey: ProductionDefectKey) =>
  productionDefectDraft[defectKey] || 0

const updateProductionDefectQuantity = (
  defectKey: ProductionDefectKey,
  event: Event
) => {
  productionDefectDraft[defectKey] = normalizeProductionQuantity(
    (event.target as HTMLInputElement).value
  )
}

const adjustProductionDefectQuantity = (
  defectKey: ProductionDefectKey,
  delta: number
) => {
  productionDefectDraft[defectKey] = Math.max(
    0,
    getProductionDefectQuantity(defectKey) + delta
  )
}

const ensureProductionDeviceParameters = (deviceKey: string) => {
  if (!deviceParameterDraft[deviceKey]) {
    deviceParameterDraft[deviceKey] = {}
  }
  return deviceParameterDraft[deviceKey]
}

const getProductionDeviceParameter = (
  deviceKey: string,
  parameterKey: ProductionDeviceParameterKey
) => ensureProductionDeviceParameters(deviceKey)[parameterKey] ?? ''

const updateProductionDeviceParameter = (
  deviceKey: string,
  parameterKey: ProductionDeviceParameterKey,
  event: Event
) => {
  const value = (event.target as HTMLInputElement).value.trim()
  ensureProductionDeviceParameters(deviceKey)[parameterKey] =
    value === '' ? undefined : normalizeProductionParameter(
      activeProductionDevice.value?.parameters.find((parameter) => parameter.parameterCode === parameterKey) || {
        parameterCode: parameterKey,
        standardText: ''
      },
      value
    )
}

const updateProductionDeviceSelectParameter = (
  deviceKey: string,
  parameterKey: ProductionDeviceParameterKey,
  event: Event
) => {
  const value = (event.target as HTMLSelectElement).value.trim()
  ensureProductionDeviceParameters(deviceKey)[parameterKey] = value || undefined
}

const updateProductionDeviceBooleanParameter = (
  deviceKey: string,
  parameterKey: ProductionDeviceParameterKey,
  event: Event
) => {
  ensureProductionDeviceParameters(deviceKey)[parameterKey] =
    (event.target as HTMLInputElement).checked
}

const isProductionDeviceMeteringValid = (deviceKey: string) =>
  deviceMeteringValidityDraft[deviceKey] !== false

const updateProductionDeviceMeteringValidity = (
  deviceKey: string,
  event: Event
) => {
  const checked = (event.target as HTMLInputElement).checked
  deviceMeteringValidityDraft[deviceKey] = checked
  syncProductionDeviceMeteringValidityParameterDraft(deviceKey, checked)
}

const adjustProductionDeviceParameter = (
  deviceKey: string,
  parameter: FrontlineRuntimeDeviceParameterVO,
  delta: number
) => {
  const params = ensureProductionDeviceParameters(deviceKey)
  const parameterKey = parameter.parameterCode
  params[parameterKey] = normalizeProductionParameter(parameter, Number(params[parameterKey] || 0) + delta)
}

const openProductionClearanceConfirmationDetail = (
  key: ProductionClearanceConfirmationKey
) => {
  activeProductionClearanceConfirmationKey.value = key
}

const closeProductionClearanceConfirmationDetail = () => {
  activeProductionClearanceConfirmationKey.value = undefined
}

const resetProductionClearanceConfirmations = () => {
  for (const confirmation of FRONTLINE_PRODUCTION_CLEARANCE_CONFIRMATIONS) {
    productionClearanceConfirmationDraft[confirmation.key] = true
  }
  activeProductionClearanceConfirmationKey.value = undefined
}

const buildProductionClearanceConfirmationPayload = () =>
  FRONTLINE_PRODUCTION_CLEARANCE_CONFIRMATIONS.map((confirmation) => ({
    key: confirmation.key,
    label: confirmation.label,
    confirmed: productionClearanceConfirmationDraft[confirmation.key] === true,
    description: confirmation.description
  }))

const buildProductionDeviceMeteringValidityPayload = () =>
  visibleDeviceCards.value.map((device) => ({
    deviceId: device.deviceId,
    deviceCode: device.deviceCode,
    deviceName: device.deviceName,
    inMeteringValidityPeriod: isProductionDeviceMeteringValid(device.key)
  }))

const resetProductionSubmissionDraft = () => {
  resetProductionMaterialDrafts()
  resetProductionClearanceConfirmations()
  Object.assign(draft.fieldValues, createFrontlineDefaultValues(context.templateCode))
  payloadPreview.value = undefined
  productionSubmitDraftKey.value = createProductionSubmitDraftKey()
}

const handleResetProduction = () => {
  resetProductionSubmissionDraft()
}

const normalizePqcQuantity = (value?: number) => {
  if (!Number.isFinite(value)) {
    return 0
  }
  return Math.max(0, Math.trunc(Number(value)))
}

const getPqcTaskDraftKey = (taskOption: PqcTaskOptionSnapshot | undefined): PqcTaskDraftKey | undefined => {
  const process = deviceState.selectedProcess
  if (!isFrontlinePqcProcess(process) || !taskOption) {
    return undefined
  }
  return [
    process.activeOrderId,
    taskOption.pqcTaskId,
    taskOption.regulationVersionId,
    taskOption.qaProcessId,
    taskOption.inspectionType,
    taskOption.businessDate,
    taskOption.shiftCode,
    taskOption.roundNo
  ].join(':')
}

const createPqcTaskDraftState = (taskOption: PqcTaskOptionSnapshot): PqcTaskDraftState => ({
  inspectionQuantity: taskOption.plannedInspectionQuantity,
  scrapQuantity: undefined
})

const getPqcTaskDraft = (taskOption: PqcTaskOptionSnapshot): PqcTaskDraftState => {
  const key = getPqcTaskDraftKey(taskOption)
  if (!key) {
    throw new Error('缺少PQC任务上下文，无法读取检验方法草稿。')
  }
  if (!pqcTaskDrafts[key]) {
    pqcTaskDrafts[key] = createPqcTaskDraftState(taskOption)
  }
  return pqcTaskDrafts[key]
}

const persistCurrentPqcTaskDraft = () => {
  const process = deviceState.selectedProcess
  const activeTaskId = activePqcTaskOptionId.value
  if (!isFrontlinePqcProcess(process) || activeTaskId === undefined) {
    return
  }
  const taskOption = getPqcTaskOptions(process)
    .find((option) => option.pqcTaskId === activeTaskId)
  const key = getPqcTaskDraftKey(taskOption)
  if (!taskOption || !key) {
    return
  }
  pqcTaskDrafts[key] = {
    inspectionQuantity: pqcDraft.inspectionQuantity ?? taskOption.plannedInspectionQuantity,
    scrapQuantity: pqcDraft.scrapQuantity
  }
}

const clearPqcTaskDraftsByTaskIds = (pqcTaskIds: number[]) => {
  const submittedTaskIds = new Set(pqcTaskIds.map(String))
  for (const key of Object.keys(pqcTaskDrafts)) {
    if (submittedTaskIds.has(key.split(':')[1])) {
      delete pqcTaskDrafts[key]
    }
  }
}

const clearAllPqcTaskDrafts = () => {
  for (const key of Object.keys(pqcTaskDrafts)) {
    delete pqcTaskDrafts[key]
  }
}

const getPqcInspectionQuantityForTask = (taskOption: PqcTaskOptionSnapshot) =>
  normalizePqcQuantity(getPqcTaskDraft(taskOption).inspectionQuantity ?? taskOption.plannedInspectionQuantity)

const isPqcNumericResultType = (resultType: FrontlinePqcResultType) => resultType === 'NUMERIC'

const resolvePqcNumericStep = (precision: number | undefined, resultType: FrontlinePqcResultType) => {
  if (!isPqcNumericResultType(resultType)) {
    return 0
  }
  if (precision && precision > 0) {
    return Number(`0.${'0'.repeat(Math.max(0, precision - 1))}1`)
  }
  return 1
}

const hasPqcTaskSnapshot = (
  process?: FrontlineDeviceRouteProcessVO | FrontlinePqcProcessVO
) => Boolean(isFrontlinePqcProcess(process) && getSelectedPqcTaskOption(process))

const resolvePqcInspectionType = (inspectionType?: string): InspectionType => {
  if (inspectionType === 'FIRST' || inspectionType === 'PATROL' || inspectionType === 'FINAL') {
    return inspectionType
  }
  throw new Error(`PQC任务检验类型${inspectionType || '空'}无效。`)
}

const findPqcTaskOption = (
  process: FrontlinePqcProcessVO,
  inspectionType: InspectionType,
  itemKey?: PqcInspectionItemKey
) => getPqcTaskOptionsForInspectionItem(process, itemKey)
  .find((option) => option.inspectionType === inspectionType)

const formatPqcTaskOptionLabel = (option: PqcTaskOptionSnapshot) =>
  option.inspectionRuleKey === 'PATROL_AM'
    ? '上午巡检'
    : option.inspectionRuleKey === 'PATROL_PM'
      ? '下午巡检'
      : option.inspectionType === 'FIRST'
    ? '首检'
    : option.inspectionType === 'FINAL'
      ? '末检'
      : `第 ${option.roundNo} 次`

const applyPqcTaskOptionToDraft = (option: PqcTaskOptionSnapshot) => {
  const storedDraft = getPqcTaskDraft(option)
  activePqcTaskOptionId.value = option.pqcTaskId
  pqcDraft.inspectionType = option.inspectionType
  pqcDraft.patrolRound = option.roundNo
  pqcDraft.inspectionQuantity = storedDraft.inspectionQuantity ?? option.plannedInspectionQuantity
  pqcDraft.scrapQuantity = storedDraft.scrapQuantity
  pqcSignatureDialogVisible.value = false
  pqcSignaturePassword.value = ''
  pqcSubmitResultUncertain.value = false
  applyPqcItemEquipmentDefaults(option.inspectionItems.map(mapPqcInspectionItem))
}

const clearPqcTaskOptionDraft = () => {
  clearAllPqcTaskDrafts()
  activePqcTaskOptionId.value = undefined
  pqcDraft.inspectionType = undefined
  pqcDraft.patrolRound = undefined
  pqcDraft.inspectionQuantity = undefined
  pqcDraft.scrapQuantity = undefined
  pqcSignatureDialogVisible.value = false
  pqcSignaturePassword.value = ''
  pqcSubmitResultUncertain.value = false
  clearPqcPieceValues()
}

const clearPqcExecutionSelection = () => {
  deviceState.selectedEmployee = undefined
  deviceState.template = undefined
  context.actualEmployeeId = undefined
  employeeTemplateCode.value = undefined
  clearPqcTaskOptionDraft()
}

const applyPqcTaskOptionToSelectedProcess = (option: PqcTaskOptionSnapshot) => {
  const process = deviceState.selectedProcess
  if (!isFrontlinePqcProcess(process)) {
    showFrontlineError('请先选择PQC工序。')
    return
  }
  persistCurrentPqcTaskDraft()
  deviceState.selectedProcess = withPqcTaskOption(process, option)
  deviceState.selectedEmployee = undefined
  deviceState.template = undefined
  context.actualEmployeeId = undefined
  employeeTemplateCode.value = undefined
  applyPqcTaskOptionToDraft(option)
  payloadPreview.value = undefined
}

const clearPqcPieceValues = () => {
  for (const key of Object.keys(pqcPieceValues)) {
    delete pqcPieceValues[key]
  }
  for (const key of Object.keys(pqcItemSelections)) {
    delete pqcItemSelections[key]
  }
  activePqcInspectionKey.value = undefined
  activePqcStandardKey.value = undefined
  activePqcMethodKey.value = undefined
  selectedPqcInspectionKey.value = undefined
  pqcPieceDraftValues.value = []
}

const applyPqcItemEquipmentDefaults = (items: PqcInspectionItem[]) => {
  for (const item of items) {
    const selection = getPqcItemSelection(item.key)
    if (selection.selectedEquipmentId || selection.selectedEquipmentNumber) {
      continue
    }
    if (!item.lastSelectedEquipmentId || !item.lastSelectedEquipmentNumber) {
      continue
    }
    const selectedOption = item.equipmentOptions.find((option) =>
      option.equipmentId === item.lastSelectedEquipmentId &&
      option.equipmentNumber === item.lastSelectedEquipmentNumber
    )
    if (!selectedOption) {
      continue
    }
    selection.selectedEquipmentId = item.lastSelectedEquipmentId
    selection.selectedEquipmentNumber = item.lastSelectedEquipmentNumber
  }
}

const getPqcItemSelection = (itemKey: PqcInspectionItemKey) => {
  if (!pqcItemSelections[itemKey]) {
    pqcItemSelections[itemKey] = {}
  }
  return pqcItemSelections[itemKey]
}

const hasPqcEquipmentOptions = (item: PqcInspectionItem) => item.equipmentOptions.length > 0

const getUniquePqcEquipmentOptions = (item: PqcInspectionItem) => {
  const seen = new Set<number>()
  return item.equipmentOptions.filter((option) => {
    if (!option.equipmentId || seen.has(option.equipmentId)) {
      return false
    }
    seen.add(option.equipmentId)
    return true
  })
}

const formatPqcEquipmentLabel = (option: FrontlinePqcEquipmentOptionVO) =>
  [option.equipmentName, option.equipmentNumber].filter(Boolean).join(' / ')

const updatePqcItemSelectedEquipment = (itemKey: PqcInspectionItemKey, event: Event) => {
  const value = (event.target as HTMLSelectElement).value
  const selection = getPqcItemSelection(itemKey)
  const selectedEquipmentId = value ? Number(value) : undefined
  const item = pqcInspectionItemMap.value[itemKey]
  const selectedOption = item?.equipmentOptions.find((option) => option.equipmentId === selectedEquipmentId)
  if (selectedEquipmentId && !selectedOption) {
    throw new Error((item?.label || itemKey) + '所选检验设备不存在于当前 QA 版本。')
  }
  selection.selectedEquipmentId = selectedEquipmentId
  selection.selectedEquipmentNumber = selectedOption?.equipmentNumber
}

const openPqcStandardDialog = (itemKey: PqcInspectionItemKey) => {
  activePqcStandardKey.value = itemKey
}

const requirePqcInspectionDisplayFields = (item: PqcInspectionItem) => {
  const missingFields: string[] = []
  if (!item.inspectionTool?.trim()) {
    missingFields.push('检验器具及设备')
  }
  if (!item.samplingPlanText?.trim()) {
    missingFields.push('抽样方案')
  }
  if (missingFields.length > 0) {
    throw new Error(
      `${item.label}缺少正式${missingFields.join('、')}，请先在QA规程中补齐并重新发布。`
    )
  }
}

const assertPqcInspectionDisplayFieldsReady = () => {
  for (const item of pqcInspectionItems.value) {
    requirePqcInspectionDisplayFields(item)
  }
}

const openPqcMethodDialog = (itemKey: PqcInspectionItemKey) => {
  const item = pqcInspectionItemMap.value[itemKey]
  if (!item) {
    showFrontlineError(`PQC检验项目${itemKey}不在当前QA规程快照中。`)
    return
  }
  try {
    requirePqcInspectionDisplayFields(item)
  } catch (error) {
    showFrontlineError(error)
    return
  }
  activePqcMethodKey.value = itemKey
}

const closePqcStandardDialog = () => {
  activePqcStandardKey.value = undefined
}

const closePqcMethodDialog = () => {
  activePqcMethodKey.value = undefined
}

const applyPqcTaskSnapshotToDraft = (
  process: FrontlinePqcProcessVO | FrontlineDeviceRouteProcessVO
) => {
  if (!isFrontlinePqcProcess(process)) {
    clearPqcTaskOptionDraft()
    return
  }
  const taskSnapshot = getDefaultPqcTaskOption(process)
  if (!taskSnapshot) {
    clearPqcTaskOptionDraft()
    return
  }
  resolvePqcInspectionType(taskSnapshot.inspectionType)
  applyPqcTaskOptionToDraft(taskSnapshot)
}

const getPqcPieceStateKeyForTask = (
  itemKey: PqcInspectionItemKey,
  taskOption: PqcTaskOptionSnapshot | undefined
) => {
  const process = deviceState.selectedProcess
  if (!isFrontlinePqcProcess(process) || !taskOption) {
    return undefined
  }
  return [
    process.activeOrderId,
    taskOption.pqcTaskId,
    taskOption.regulationVersionId,
    process.routeId,
    taskOption.qaProcessId,
    taskOption.inspectionType,
    taskOption.roundNo,
    itemKey
  ].join(':')
}

const getPqcPieceStateKey = (itemKey: PqcInspectionItemKey) =>
  getPqcPieceStateKeyForTask(itemKey, activePqcTaskOption.value)

const ensurePqcDefaultPieceValuesForTask = (
  itemKey: PqcInspectionItemKey,
  taskOption: PqcTaskOptionSnapshot
) => {
  const stateKey = getPqcPieceStateKeyForTask(itemKey, taskOption)
  if (!stateKey) {
    return []
  }
  const item = pqcInspectionItemMap.value[itemKey]
  if (!item) {
    throw new Error(`PQC检验项目${itemKey}不在当前QA规程快照中。`)
  }
  const quantity = getPqcInspectionQuantityForTask(taskOption)
  const values = pqcPieceValues[stateKey] || []
  const hasMaterializableDefault = item.type === 'choice' || String(item.defaultValue ?? '').trim().length > 0
  while (hasMaterializableDefault && values.length < quantity) {
    values.push(item.defaultValue)
  }
  if (item.type === 'choice') {
    for (let index = 0; index < quantity; index += 1) {
      if (!String(values[index] ?? '').trim()) {
        values[index] = item.defaultValue
      }
    }
  }
  pqcPieceValues[stateKey] = values
  return values
}

const resizePqcPieceValuesForCurrentTask = (taskOption: PqcTaskOptionSnapshot) => {
  const quantity = getPqcInspectionQuantityForTask(taskOption)
  for (const item of taskOption.inspectionItems.map(mapPqcInspectionItem)) {
    const stateKey = getPqcPieceStateKeyForTask(item.key, taskOption)
    if (!stateKey) {
      continue
    }
    pqcPieceValues[stateKey] = (pqcPieceValues[stateKey] || []).slice(0, quantity)
  }
}

const getPqcStoredPieceValuesForTask = (
  itemKey: PqcInspectionItemKey,
  taskOption: PqcTaskOptionSnapshot
) => ensurePqcDefaultPieceValuesForTask(itemKey, taskOption)

const getPqcStoredPieceValues = (itemKey: PqcInspectionItemKey) => {
  const taskOption = activePqcTaskOption.value
  if (!taskOption) {
    return []
  }
  return getPqcStoredPieceValuesForTask(itemKey, taskOption)
}

const getPqcCompletedCount = (itemKey: PqcInspectionItemKey) =>
  getPqcStoredPieceValues(itemKey)
    .slice(0, pqcInspectionQuantity.value)
    .filter((value) => value.trim().length > 0).length

const getPqcProgressText = (itemKey: PqcInspectionItemKey) =>
  `已填 ${getPqcCompletedCount(itemKey)}/${pqcInspectionQuantity.value}`

const selectPqcInspectionTab = async (itemKey: PqcInspectionItemKey) => {
  selectedPqcInspectionKey.value = itemKey
  const process = deviceState.selectedProcess
  if (!isFrontlinePqcProcess(process)) {
    return
  }
  const option = preferPqcTaskOption(
    getPqcTaskOptionsForInspectionItem(process, itemKey),
    pqcDraft.inspectionType
  )
  if (!option) {
    showFrontlineError('当前检验方法暂无待执行PQC任务。')
    return
  }
  if (activePqcTaskOptionId.value !== option.pqcTaskId) {
    applyPqcTaskOptionToSelectedProcess(option)
    selectedPqcInspectionKey.value = itemKey
    await switchPqcCurrentLoginEmployeeForActiveTask()
  }
}

const getPqcSelectedEquipmentLabel = (item: PqcInspectionItem) => {
  const selectedEquipmentId = getPqcItemSelection(item.key).selectedEquipmentId
  const selectedOption = item.equipmentOptions.find((option) =>
    option.equipmentId === selectedEquipmentId
  )
  if (selectedOption) {
    return formatPqcEquipmentLabel(selectedOption)
  }
  return hasPqcEquipmentOptions(item) ? '可选检验设备' : ''
}

const formatPqcInspectionItemTabLabel = (item: PqcInspectionItem) =>
  item.itemName || '未配置检验项目名称'

const formatPqcStandardSummary = (item: PqcInspectionItem) => {
  if (item.acceptanceStandard) {
    return item.acceptanceStandard
  }
  return '未配置接收标准'
}

const normalizePqcInspectionMethodLabel = (inspectionMethod: string) => {
  const trimmedMethod = inspectionMethod.trim()
  const methodDisplayLabels: Record<string, string> = {
    'Visual inspection': '目视检验',
    'visual inspection': '目视检验'
  }
  return methodDisplayLabels[trimmedMethod] || methodDisplayLabels[trimmedMethod.toLowerCase()] || trimmedMethod
}

const formatPqcMethodSummary = (item: PqcInspectionItem) =>
  normalizePqcInspectionMethodLabel(item.processInspectionMethod) || '未配置检验方法'

const formatPqcInspectionTitle = (item: PqcInspectionItem) =>
  formatPqcMethodSummary(item)

function assertPqcItemEquipmentSelection(item: PqcInspectionItem) {
  const selection = getPqcItemSelection(item.key)
  if (!hasPqcEquipmentOptions(item)) {
    if (selection.selectedEquipmentId || selection.selectedEquipmentNumber) {
      throw new Error(`${item.label}未配置检验设备，不能提交设备选择。`)
    }
    return { selection, selectedOption: undefined }
  }
  if (!selection.selectedEquipmentId) {
    throw new Error(`${item.label}未选择检验设备。`)
  }
  if (!selection.selectedEquipmentNumber) {
    throw new Error(`${item.label}未选择设备编号。`)
  }
  const selectedOption = item.equipmentOptions.find((option) =>
    option.equipmentId === selection.selectedEquipmentId &&
    option.equipmentNumber === selection.selectedEquipmentNumber
  )
  if (!selectedOption) {
    throw new Error(`${item.label}设备编号不属于所选检验设备。`)
  }
  return { selection, selectedOption }
}

const requirePqcItemSelection = (item: PqcInspectionItem) =>
  assertPqcItemEquipmentSelection(item)

const getPqcExactPieceValuesForTask = (
  itemKey: PqcInspectionItemKey,
  taskOption: PqcTaskOptionSnapshot | undefined
) => {
  const item = pqcInspectionItemMap.value[itemKey]
  if (!item) {
    throw new Error(`PQC检验项目${itemKey}不在当前QA规程快照中。`)
  }
  if (!taskOption) {
    throw new Error(`${item.label}缺少PQC任务上下文，无法提交逐件检验。`)
  }
  const stateKey = getPqcPieceStateKeyForTask(itemKey, taskOption)
  if (!stateKey) {
    throw new Error(`${item.label}缺少PQC任务上下文，无法提交逐件检验。`)
  }
  const quantity = getPqcInspectionQuantityForTask(taskOption)
  const values = ensurePqcDefaultPieceValuesForTask(itemKey, taskOption)
  if (values.length !== quantity) {
    throw new Error(`${item.label}样本数量${values.length}与实际检验数量${quantity}不一致。`)
  }
  return values.map((value) => String(value ?? '').trim())
}

const getPqcExactPieceValuesForSubmit = (itemKey: PqcInspectionItemKey) => {
  return getPqcExactPieceValuesForTask(itemKey, activePqcTaskOption.value)
}

const assertPqcSubmissionSampleQuantities = () => {
  for (const itemKey of pqcInspectionItemKeys.value) {
    getPqcExactPieceValuesForSubmit(itemKey)
  }
}

const assertPqcSubmissionSampleQuantitiesForTask = (taskOption: PqcTaskOptionSnapshot) => {
  for (const item of taskOption.inspectionItems.map(mapPqcInspectionItem)) {
    getPqcExactPieceValuesForTask(item.key, taskOption)
  }
}

const getPqcCurrentSubmitTaskOptions = () => {
  const process = deviceState.selectedProcess
  const activeOption = activePqcTaskOption.value
  if (!isFrontlinePqcProcess(process) || !activeOption) {
    throw new Error('缺少PQC任务上下文，无法提交。')
  }
  const scopeOptions = getPqcTaskOptions(process).filter((option) =>
    option.inspectionType === activeOption.inspectionType &&
    option.businessDate === activeOption.businessDate &&
    option.shiftCode === activeOption.shiftCode &&
    option.roundNo === activeOption.roundNo
  )
  const completedScopeOptions = process.pqcTaskOptions
    .filter((option) =>
      option.taskStatus !== 'PENDING' &&
      option.inspectionType === activeOption.inspectionType &&
      option.businessDate === activeOption.businessDate &&
      option.shiftCode === activeOption.shiftCode &&
      option.roundNo === activeOption.roundNo
    )
  const submitOptions: PqcTaskOptionSnapshot[] = []
  const submittedTaskIds = new Set<number>()
  for (const item of pqcInspectionItems.value) {
    const option = scopeOptions.find((option) => pqcTaskOptionIncludesItem(option, item.key))
    if (!option) {
      if (completedScopeOptions.some((completedOption) =>
        pqcTaskOptionIncludesItem(completedOption, item.key)
      )) {
        continue
      }
      throw new Error(`${item.label}缺少${formatPqcTaskOptionLabel(activeOption)}PQC任务。`)
    }
    if (!submittedTaskIds.has(option.pqcTaskId)) {
      submittedTaskIds.add(option.pqcTaskId)
      submitOptions.push(option)
    }
  }
  return submitOptions
}

const buildPqcItemResultsPayload = (
  taskOption: PqcTaskOptionSnapshot | undefined = activePqcTaskOption.value
): FrontlinePqcItemResultSubmitReqVO[] =>
  (taskOption?.inspectionItems || []).map(mapPqcInspectionItem).map((item) => {
    const { selection, selectedOption } = assertPqcItemEquipmentSelection(item)
    const payload: FrontlinePqcItemResultSubmitReqVO = {
      itemCode: item.key,
      sampleValues: getPqcExactPieceValuesForTask(item.key, taskOption)
    }
    if (selectedOption) {
      payload.selectedEquipmentId = selection.selectedEquipmentId
      payload.selectedEquipmentNumber = selection.selectedEquipmentNumber
    }
    return payload
  }).filter((item) =>
    item.sampleValues.length > 0 ||
    Boolean(item.selectedEquipmentId || item.selectedEquipmentNumber)
  )

const buildPqcItemDetailsPayload = (
  taskOption: PqcTaskOptionSnapshot | undefined = activePqcTaskOption.value
) =>
  (taskOption?.inspectionItems || []).map(mapPqcInspectionItem).map((item) => {
    const { selection, selectedOption } = requirePqcItemSelection(item)
    return {
      itemCode: item.key,
      itemName: item.itemName,
      selectedEquipmentId: selectedOption ? selection.selectedEquipmentId : undefined,
      selectedEquipmentCode: selectedOption?.equipmentCode,
      selectedEquipmentName: selectedOption?.equipmentName,
      selectedEquipmentNumber: selectedOption ? selection.selectedEquipmentNumber : undefined,
      standardText: item.acceptanceStandard,
      standardLowerLimit: item.standardLowerLimit,
      standardUpperLimit: item.standardUpperLimit,
      standardUnit: item.standardUnit,
      standardPrecision: item.standardPrecision,
      inspectionMethod: item.processInspectionMethod,
      resultType: item.resultType,
      sampleValues: getPqcExactPieceValuesForTask(item.key, taskOption)
    }
  }).filter((item) =>
    item.sampleValues.length > 0 ||
    Boolean(item.selectedEquipmentId || item.selectedEquipmentNumber)
  )

const assertPqcCurrentProcessAllMethodSubmissionReady = () => {
  assertPqcSubmissionSampleQuantities()
  for (const taskOption of getPqcCurrentSubmitTaskOptions()) {
    assertPqcSubmissionSampleQuantitiesForTask(taskOption)
    for (const item of taskOption.inspectionItems.map(mapPqcInspectionItem)) {
      assertPqcItemEquipmentSelection(item)
    }
  }
}

const getPqcCurrentChoiceValues = (itemKey: PqcInspectionItemKey) =>
  getPqcStoredPieceValues(itemKey).slice(0, pqcInspectionQuantity.value)

const getPqcCurrentChoiceValuesForTask = (
  itemKey: PqcInspectionItemKey,
  taskOption: PqcTaskOptionSnapshot
) => getPqcStoredPieceValuesForTask(itemKey, taskOption)
  .slice(0, getPqcInspectionQuantityForTask(taskOption))

const isPqcBulkChoiceActive = (
  itemKey: PqcInspectionItemKey,
  result: PqcChoiceResult
) => {
  const values = getPqcCurrentChoiceValues(itemKey)
  return values.length > 0 && values.every((value) => value === result)
}

const isPqcManualChoiceActive = (itemKey: PqcInspectionItemKey) => {
  const values = getPqcCurrentChoiceValues(itemKey)
  const completed = values.filter((value) => value.trim().length > 0).length
  const allPass = values.length > 0 && values.every((value) => value === '合格')
  const allFail = values.length > 0 && values.every((value) => value === '不合格')
  return completed > 0 && !allPass && !allFail
}

const assertPqcPieceContext = () => {
  if (!deviceState.selectedProcess) {
    throw new Error('请先选择工序，再填写逐件检验。')
  }
  if (!hasPqcTaskSnapshot(deviceState.selectedProcess)) {
    throw new Error('当前工序缺少PQC任务或QA规程快照，无法填写逐件检验。')
  }
  if (pqcInspectionQuantity.value <= 0) {
    throw new Error('请先填写大于 0 的检验数量。')
  }
}

const openPqcPieceInspection = (itemKey: PqcInspectionItemKey) => {
  try {
    assertPqcPieceContext()
  } catch (error) {
    showFrontlineError(error)
    return
  }
  activePqcInspectionKey.value = itemKey
  pqcPieceDraftValues.value = getPqcStoredPieceValues(itemKey).slice()
}

const closePqcPieceInspection = (saveChanges: boolean) => {
  const itemKey = activePqcInspectionKey.value
  if (saveChanges && itemKey) {
    const stateKey = getPqcPieceStateKey(itemKey)
    if (!stateKey) {
      showFrontlineError('当前工序上下文已失效，无法保存逐件检验。')
      return
    }
    pqcPieceValues[stateKey] = pqcPieceDraftValues.value.slice()
  }
  activePqcInspectionKey.value = undefined
  pqcPieceDraftValues.value = []
}

const applyPqcBulkChoice = (
  itemKey: PqcInspectionItemKey,
  result: PqcChoiceResult
) => {
  try {
    assertPqcPieceContext()
  } catch (error) {
    showFrontlineError(error)
    return
  }
  const values = getPqcStoredPieceValues(itemKey)
  for (let index = 0; index < pqcInspectionQuantity.value; index += 1) {
    values[index] = result
  }
}

const updatePqcPieceChoice = (index: number, value: boolean | string | number) => {
  if (value === true) {
    pqcPieceDraftValues.value[index] = '合格'
    return
  }
  if (value === false) {
    pqcPieceDraftValues.value[index] = '不合格'
    return
  }
  showFrontlineError(`逐件检验结果无效：${String(value)}`)
}

const stepPqcPieceValue = (index: number, delta: number) => {
  const item = activePqcInspectionItem.value
  if (!item || item.type !== 'number') {
    showFrontlineError('当前检验项目不是数值项目，无法调整数值。')
    return
  }
  const current = Number(pqcPieceDraftValues.value[index] || item.defaultValue)
  const precision = item.step < 1 ? String(item.step).split('.')[1]?.length || 0 : 0
  pqcPieceDraftValues.value[index] = String(
    Number((current + delta).toFixed(precision))
  )
}

const updatePqcPieceDraftValue = (index: number, event: Event) => {
  pqcPieceDraftValues.value[index] = (event.target as HTMLInputElement).value
}

const selectPqcInspectionType = async (inspectionType: InspectionType) => {
  const process = deviceState.selectedProcess
  if (!isFrontlinePqcProcess(process)) {
    showFrontlineError('请先选择PQC工序。')
    return
  }
  const itemKey = activePqcTabKey.value
  const option = findPqcTaskOption(process, inspectionType, itemKey)
  if (!option) {
    showFrontlineError(`当前检验方法缺少${PQC_INSPECTION_TYPE_LABELS[inspectionType]}PQC任务。`)
    return
  }
  if (activePqcTaskOption.value?.pqcTaskId === option.pqcTaskId) {
    return
  }
  applyPqcTaskOptionToSelectedProcess(option)
  selectedPqcInspectionKey.value = itemKey
  await switchPqcCurrentLoginEmployeeForActiveTask()
}

const selectPqcInspectionTaskOption = async (pqcTaskId: number) => {
  const process = deviceState.selectedProcess
  const itemKey = activePqcTabKey.value
  const option = getPqcTaskOptions(isFrontlinePqcProcess(process) ? process : undefined)
    .find((taskOption) =>
      taskOption.pqcTaskId === pqcTaskId && pqcTaskOptionIncludesItem(taskOption, itemKey)
    )
  if (!option) {
    showFrontlineError('当前工序缺少对应的PQC任务。')
    return
  }
  if (activePqcTaskOption.value?.pqcTaskId === option.pqcTaskId) {
    return
  }
  applyPqcTaskOptionToSelectedProcess(option)
  selectedPqcInspectionKey.value = itemKey
  await switchPqcCurrentLoginEmployeeForActiveTask()
}

const updatePqcQuantity = (field: PqcQuantityField, event: Event) => {
  const inputValue = (event.target as HTMLInputElement).value
  pqcDraft[field] = inputValue === '' ? undefined : normalizePqcQuantity(Number(inputValue))
  persistCurrentPqcTaskDraft()
  if (field === 'inspectionQuantity' && activePqcTaskOption.value) {
    resizePqcPieceValuesForCurrentTask(activePqcTaskOption.value)
  }
}

const adjustPqcQuantity = (field: PqcQuantityField, delta: number) => {
  pqcDraft[field] = Math.max(0, normalizePqcQuantity(pqcDraft[field]) + delta)
  persistCurrentPqcTaskDraft()
  if (field === 'inspectionQuantity' && activePqcTaskOption.value) {
    resizePqcPieceValuesForCurrentTask(activePqcTaskOption.value)
  }
}

const resolvePqcTaskSummaryState = (
  pqcTaskOptions: FrontlinePqcTaskOptionVO[]
): FrontlinePqcTaskSummaryState => {
  if (pqcTaskOptions.length === 0) {
    return 'NOT_CREATED'
  }
  const statusSet = new Set(pqcTaskOptions.map((option) => option.taskStatus))
  return statusSet.size === 1 ? [...statusSet][0] : 'MIXED'
}

const updatePqcSubmittedTasksInProcess = (
  process: FrontlinePqcProcessVO,
  submittedTaskIds: Set<number>
): FrontlinePqcProcessVO => {
  const pqcTaskOptions = process.pqcTaskOptions.map((taskOption) =>
    submittedTaskIds.has(taskOption.pqcTaskId)
      ? { ...taskOption, taskStatus: 'SUBMITTED' as FrontlinePqcTaskStatus }
      : taskOption
  )
  const countStatus = (status: FrontlinePqcTaskStatus) =>
    pqcTaskOptions.filter((taskOption) => taskOption.taskStatus === status).length
  return {
    ...process,
    pqcTaskOptions,
    taskSummary: {
      ...process.taskSummary,
      state: resolvePqcTaskSummaryState(pqcTaskOptions),
      totalCount: pqcTaskOptions.length,
      pendingCount: countStatus('PENDING'),
      submittedCount: countStatus('SUBMITTED'),
      confirmedCount: countStatus('CONFIRMED'),
      cancelledCount: countStatus('CANCELLED')
    }
  }
}

const syncPqcSubmittedTasksInProcessOptions = (
  process: FrontlinePqcProcessVO,
  submittedTaskIds: Set<number>
) => {
  deviceState.processOptions = deviceState.processOptions.map((candidate) =>
    isFrontlinePqcProcess(candidate) &&
    candidate.activeOrderId === process.activeOrderId &&
    candidate.regulationVersionId === process.regulationVersionId &&
    candidate.qaProcessId === process.qaProcessId
      ? updatePqcSubmittedTasksInProcess(candidate, submittedTaskIds)
      : candidate
  )
}

const markPqcTasksSubmittedAndSelectNext = (submittedPqcTaskIds: number[]) => {
  const process = deviceState.selectedProcess
  if (!isFrontlinePqcProcess(process)) {
    return
  }
  const submittedTaskIds = new Set(submittedPqcTaskIds)
  const updatedProcess = updatePqcSubmittedTasksInProcess(process, submittedTaskIds)
  syncPqcSubmittedTasksInProcessOptions(process, submittedTaskIds)
  invalidateFrontlinePqcProcessCacheForActiveOrder(deviceState, process.activeOrderId)
  const nextOption = getDefaultPqcTaskOption(updatedProcess)
  deviceState.selectedProcess = nextOption
    ? withPqcTaskOption(updatedProcess, nextOption)
    : updatedProcess
  if (nextOption) {
    applyPqcTaskOptionToDraft(nextOption)
  } else {
    clearPqcTaskOptionDraft()
  }
}

const markPqcTaskSubmittedAndSelectNext = (submittedPqcTaskId: number) => {
  markPqcTasksSubmittedAndSelectNext([submittedPqcTaskId])
}

const resetPqcSubmissionDrafts = (submittedPqcTaskIds: number[] = []) => {
  if (submittedPqcTaskIds.length > 0) {
    clearPqcTaskDraftsByTaskIds(submittedPqcTaskIds)
  } else {
    clearAllPqcTaskDrafts()
  }
  clearPqcPieceValues()
  pqcDraft.scrapQuantity = undefined
  payloadPreview.value = undefined
  pqcSignatureDialogVisible.value = false
  pqcSignaturePassword.value = ''
  if (submittedPqcTaskIds.length > 0) {
    if (submittedPqcTaskIds.length === 1) {
      markPqcTaskSubmittedAndSelectNext(submittedPqcTaskIds[0])
    } else {
      markPqcTasksSubmittedAndSelectNext(submittedPqcTaskIds)
    }
  }
}

const resetPqcSubmissionDraft = (submittedPqcTaskId?: number) => {
  if (submittedPqcTaskId !== undefined) {
    resetPqcSubmissionDrafts([submittedPqcTaskId])
    return
  }
  resetPqcSubmissionDrafts()
}

const handleResetPqc = () => {
  if (pqcSubmitResultUncertain.value) {
    return
  }
  resetPqcSubmissionDraft()
}

const openPicker = (picker: PickerType) => {
  if (isPqcMode.value && picker === 'employee') {
    return
  }
  activePicker.value = picker
  if (picker === 'order') {
    activeOrderKeyword.value = ''
    nextTick(() => activeOrderSearchInputRef.value?.focus())
  }
}

const closePicker = () => {
  if (activePicker.value === 'order') {
    activeOrderKeyword.value = ''
  }
  activePicker.value = undefined
}

const parseCssPixelValue = (value: string) => {
  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const resolveProductionViewportScale = () => {
  const panel = frontlinePanelRef.value
  if (!panel || isPqcMode.value) {
    return 1
  }
  const rect = panel.getBoundingClientRect()
  const style = window.getComputedStyle(panel)
  const availableWidth = Math.max(
    0,
    rect.width - parseCssPixelValue(style.paddingLeft) - parseCssPixelValue(style.paddingRight)
  )
  const widthScale = availableWidth / PRODUCTION_CANVAS_WIDTH
  const fullscreenHeightScale = isProductionFullscreen.value
    ? Math.max(
      0,
      rect.height - parseCssPixelValue(style.paddingTop) - parseCssPixelValue(style.paddingBottom)
    ) / PRODUCTION_CANVAS_HEIGHT
    : 1
  const nextScale = Math.min(1, widthScale, fullscreenHeightScale)
  if (!Number.isFinite(nextScale) || nextScale <= 0) {
    return 1
  }
  return nextScale
}

const updateProductionViewportScale = () => {
  productionViewportScale.value = resolveProductionViewportScale()
}

const scheduleProductionViewportScaleUpdate = () => {
  if (isPqcMode.value) {
    productionViewportScale.value = 1
    return
  }
  if (productionViewportScaleFrame !== undefined) {
    window.cancelAnimationFrame(productionViewportScaleFrame)
  }
  productionViewportScaleFrame = window.requestAnimationFrame(() => {
    productionViewportScaleFrame = undefined
    updateProductionViewportScale()
  })
}

const syncPqcFullscreenState = () => {
  isPqcFullscreen.value = isPqcMode.value && document.fullscreenElement === frontlinePanelRef.value
  isProductionFullscreen.value = !isPqcMode.value && document.fullscreenElement === frontlinePanelRef.value
  scheduleProductionViewportScaleUpdate()
}

const enterPqcFullscreen = async () => {
  const panel = frontlinePanelRef.value
  if (!panel) {
    throw new Error('PQC填写最大化区域尚未加载。')
  }
  if (typeof panel.requestFullscreen !== 'function') {
    throw new Error('当前浏览器不支持PQC填写最大化。')
  }
  await panel.requestFullscreen()
  syncPqcFullscreenState()
}

const exitPqcFullscreen = async () => {
  if (!document.fullscreenElement) {
    syncPqcFullscreenState()
    return
  }
  if (typeof document.exitFullscreen !== 'function') {
    throw new Error('当前浏览器不支持退出PQC填写最大化。')
  }
  await document.exitFullscreen()
  syncPqcFullscreenState()
}

const handlePqcFullscreenToggle = async () => {
  try {
    if (isPqcFullscreen.value) {
      await exitPqcFullscreen()
      return
    }
    await enterPqcFullscreen()
    await preloadFrontlinePqcSwitchingCache(deviceState)
  } catch (error) {
    showFrontlineError(error)
    return
  }
}

const enterProductionFullscreen = async () => {
  const panel = frontlinePanelRef.value
  if (!panel) {
    throw new Error('一线生产填写最大化区域尚未加载。')
  }
  if (typeof panel.requestFullscreen !== 'function') {
    throw new Error('当前浏览器不支持一线生产填写最大化。')
  }
  await panel.requestFullscreen()
  syncPqcFullscreenState()
}

const exitProductionFullscreen = async () => {
  if (!document.fullscreenElement) {
    syncPqcFullscreenState()
    return
  }
  if (typeof document.exitFullscreen !== 'function') {
    throw new Error('当前浏览器不支持退出一线生产填写最大化。')
  }
  await document.exitFullscreen()
  syncPqcFullscreenState()
}

const preloadProductionRuntimeCacheForFullscreen = async () => {
  if (isPqcMode.value) {
    return
  }
  await preloadFrontlineProductionRuntimeCache(
    deviceState,
    switchableProcessOptions.value.filter(isFrontlineProductionProcess)
  )
}

const handleProductionFullscreenToggle = async () => {
  try {
    if (isProductionFullscreen.value) {
      await exitProductionFullscreen()
      return
    }
    await enterProductionFullscreen()
    await preloadProductionRuntimeCacheForFullscreen()
  } catch (error) {
    showFrontlineError(error)
    return
  }
}

interface ProductionInitialProcessIdentity {
  routeId?: number
  routeProcessId?: number
  processId?: number
}

const findInitialProcess = (
  processes: Array<FrontlineDeviceRouteProcessVO | FrontlinePqcProcessVO> = switchableProcessOptions.value,
  requestedIdentity: ProductionInitialProcessIdentity = context
) => {
  if (isPqcMode.value) {
    const qaProcesses = processes.filter(isFrontlinePqcProcess)
    return qaProcesses.find((process) =>
      Boolean(getDefaultPqcTaskOption(process))
    ) || qaProcesses[0]
  }
  const productionProcesses = processes.filter(isFrontlineProductionProcess)
  const requestedRouteId = requestedIdentity.routeId
  const requestedRouteProcessId = requestedIdentity.routeProcessId
  const requestedProcessId = requestedIdentity.processId
  if (requestedRouteId || requestedRouteProcessId || requestedProcessId) {
    const matchedProcess = productionProcesses.find((process) =>
      (!requestedRouteId || process.routeId === requestedRouteId) &&
      (!requestedRouteProcessId || process.routeProcessId === requestedRouteProcessId) &&
      (!requestedProcessId || process.processId === requestedProcessId)
    )
    if (matchedProcess) {
      return matchedProcess
    }
  }
  return productionProcesses[0]
}

const isCurrentLoginEmployee = (employee?: FrontlineEmployeeCandidateVO) => {
  const loginUserId = currentLoginUserId.value
  return Boolean(
    employee &&
    loginUserId &&
    (
      employee.userId === loginUserId ||
      employee.systemUserId === loginUserId
    )
  )
}

const findCurrentLoginEmployee = () => {
  return deviceState.employeeOptions.find((employee) => isCurrentLoginEmployee(employee)) ||
    currentLoginEmployeeCandidate.value
}

const findInitialEmployee = () => {
  if (isPqcMode.value) {
    return findCurrentLoginEmployee()
  }
  const requestedActualEmployeeId = context.actualEmployeeId
  if (requestedActualEmployeeId) {
    const matchedEmployee = deviceState.employeeOptions.find((employee) =>
      employee.userId === requestedActualEmployeeId ||
      employee.systemUserId === requestedActualEmployeeId ||
      employee.employeeProfileId === requestedActualEmployeeId
    )
    if (matchedEmployee) {
      return matchedEmployee
    }
  }
  return deviceState.employeeOptions[0]
}

const handleActiveOrderSearchEnter = async () => {
  const keyword = normalizeActiveOrderKeyword(activeOrderKeyword.value)
  if (!keyword) {
    return
  }
  const exactMatch = filteredActiveOrderOptions.value.find(
    (order) => normalizeActiveOrderKeyword(order.workOrderCode) === keyword
  )
  const targetOrder = exactMatch || (
    filteredActiveOrderOptions.value.length === 1
      ? filteredActiveOrderOptions.value[0]
      : undefined
  )
  if (targetOrder) {
    await handleSelectActiveOrder(targetOrder)
  }
}

const handleSelectActiveOrder = async (
  activeOrder: FrontlineActiveOrderVO,
  requestedProcessIdentity?: ProductionInitialProcessIdentity
) => {
  if (!isPqcMode.value) {
    const selectionRequestId = ++activeOrderSelectionRequestId
    processSelectionRequestId += 1
    productionEmployeeSelectionRequestId += 1
    applyActiveOrderToContext(activeOrder)
    employeeTemplateCode.value = undefined
    payloadPreview.value = undefined
    closePicker()
    let processes: FrontlineDeviceRouteProcessVO[]
    try {
      processes = await selectFrontlineProductionActiveOrder(deviceState, activeOrder)
    } catch (error) {
      if (
        selectionRequestId !== activeOrderSelectionRequestId ||
        error instanceof FrontlineProductionStaleActiveOrderSelectionError
      ) {
        return
      }
      showFrontlineError(error)
      return
    }
    if (selectionRequestId !== activeOrderSelectionRequestId) {
      return
    }
    const initialProcess = findInitialProcess(processes, requestedProcessIdentity)
    if (!initialProcess || !isFrontlineProductionProcess(initialProcess)) {
      const error = new Error(
        '生产工单 ' + (activeOrder.workOrderCode || activeOrder.workOrderId) +
        ' 的正式工艺路线没有可用工序。'
      )
      showFrontlineError(error)
      return
    }
    try {
      await handleSelectProcess(initialProcess)
    } catch (error) {
      showFrontlineError(error)
    }
    return
  }
  pqcSubmitResultUncertain.value = false
  pqcSignatureDialogVisible.value = false
  pqcSignaturePassword.value = ''
  clearPqcExecutionSelection()
  const selectionRequestId = ++activeOrderSelectionRequestId
  let processes: FrontlinePqcProcessVO[]
  try {
    processes = await selectFrontlinePqcActiveOrder(
      deviceState,
      activeOrder,
      currentLoginUserId.value
    )
  } catch (error) {
    if (selectionRequestId !== activeOrderSelectionRequestId ||
      error instanceof FrontlinePqcStaleActiveOrderSelectionError) {
      return
    }
    showFrontlineError(error)
    closePicker()
    return
  }
  if (selectionRequestId !== activeOrderSelectionRequestId) {
    return
  }
  applyActiveOrderToContext(activeOrder)
  employeeTemplateCode.value = undefined
  payloadPreview.value = undefined
  const initialProcess = findInitialProcess(processes)
  if (initialProcess) {
    try {
      await handleSelectProcess(initialProcess)
    } catch (error) {
      showFrontlineError(error)
    }
  } else {
    closePicker()
  }
}

const handleSelectProcess = async (
  process: FrontlineDeviceRouteProcessVO | FrontlinePqcProcessVO
) => {
  const shouldClosePickerImmediately = true
  const selectionRequestId = ++processSelectionRequestId
  if (isPqcMode.value && !isFrontlinePqcProcess(process)) {
    showFrontlineError('PQC只能选择QA规程工序。')
    return
  }
  if (!isPqcMode.value && !isFrontlineProductionProcess(process)) {
    showFrontlineError('生产填写只能选择MES工艺路线工序。')
    return
  }
  const selectedProcess = isPqcMode.value && isFrontlinePqcProcess(process)
    ? withPqcTaskOption(process, getDefaultPqcTaskOption(process))
    : process
  if (shouldClosePickerImmediately) {
    closePicker()
  }

  if (isFrontlinePqcProcess(selectedProcess)) {
    clearPqcExecutionSelection()
    await selectFrontlinePqcProcess(deviceState, selectedProcess)
  } else {
    await selectFrontlineProcess(deviceState, selectedProcess)
  }
  if (selectionRequestId !== processSelectionRequestId) {
    return
  }
  applyProcessToContext(selectedProcess)
  if (
    isFrontlineProductionProcess(selectedProcess) &&
    deviceState.selectedActiveOrder?.routeId !== undefined &&
    deviceState.selectedActiveOrder.routeId !== selectedProcess.routeId
  ) {
    deviceState.selectedActiveOrder = undefined
    context.workOrderId = undefined
  }
  if (isPqcMode.value) {
    applyPqcTaskSnapshotToDraft(selectedProcess)
  }
  employeeTemplateCode.value = undefined
  payloadPreview.value = undefined
  pqcSubmitResultUncertain.value = false
  const initialEmployee = findInitialEmployee()
  if (initialEmployee) {
    await handleSelectEmployee(initialEmployee)
  } else if (isPqcMode.value) {
    showFrontlineError('当前登录账号未返回PQC人员候选，无法进入PQC填写。')
    return
  }
}

const handlePickerProcessClick = async (
  process: FrontlineDeviceRouteProcessVO | FrontlinePqcProcessVO
) => {
  try {
    await handleSelectProcess(process)
  } catch (error) {
    showFrontlineError(error)
  }
}

const handleNavigateProductionProcess = async (direction: -1 | 1) => {
  const targetProcess = direction < 0
    ? previousProductionProcess.value
    : nextProductionProcess.value
  if (!targetProcess || isProductionProcessNavigationBlocked.value) {
    return
  }
  await handleSelectProcess(targetProcess)
}

const handleNavigatePqcProcess = async (direction: -1 | 1) => {
  const targetProcess = direction < 0
    ? previousPqcProcess.value
    : nextPqcProcess.value
  if (!targetProcess || isPqcProcessNavigationBlocked.value) {
    return
  }
  await handleSelectProcess(targetProcess)
}

const handleSelectEmployee = async (employee: FrontlineEmployeeCandidateVO) => {
  if (isPqcMode.value && !isCurrentLoginEmployee(employee)) {
    showFrontlineError('一线PQC员工必须使用当前登录账号。')
    return
  }
  const shouldClosePickerImmediately = !isPqcMode.value
  const selectionRequestId = shouldClosePickerImmediately
    ? ++productionEmployeeSelectionRequestId
    : 0
  if (shouldClosePickerImmediately) {
    closePicker()
  }
  let result: FrontlineEmployeeSwitchResult
  try {
    result = isPqcMode.value
      ? await switchFrontlinePqcActualEmployee(deviceState, activePqcTaskOption.value, employee.userId)
      : await switchFrontlineActualEmployee(deviceState, employee.userId)
  } catch (error) {
    showFrontlineError(error)
    return
  }
  if (shouldClosePickerImmediately && selectionRequestId !== productionEmployeeSelectionRequestId) {
    return
  }
  if (isPqcMode.value && !deviceState.selectedEmployee && isCurrentLoginEmployee(employee)) {
    deviceState.selectedEmployee = employee
  }
  context.actualEmployeeId = result.actualEmployeeId
  const templateCode = resolveTemplateCode(result.template?.templateNo, result.template?.templateType)
  employeeTemplateCode.value = templateCode
  payloadPreview.value = undefined
  if (!shouldClosePickerImmediately) {
    closePicker()
  }
}

watch(currentLoginUserId, async () => {
  if (!isPqcMode.value || deviceState.selectedEmployee || !activePqcTaskOption.value) {
    return
  }
  if (!isFrontlinePqcProcess(deviceState.selectedProcess)) {
    return
  }
  const employee = findCurrentLoginEmployee()
  if (!employee) {
    return
  }
  await handleSelectEmployee(employee)
})

const assertProductionSubmissionReady = () => {
  persistActiveProductionMaterialDraft()
  if (!configuredProductionMaterials.value.length) {
    throw new Error('当前工序没有冻结物料，无法提交')
  }
  const missingMaterials = configuredProductionMaterials.value.filter(
    (material) => productionMaterialDrafts[material.key]?.outputQuantity === undefined
  )
  if (missingMaterials.length) {
    throw new Error(`请填写完成数量：${missingMaterials.map((material) => material.materialName).join('、')}`)
  }
  const invalidLossMaterials = configuredProductionMaterials.value.filter((material) => {
    const materialDraft = productionMaterialDrafts[material.key]
    const lossQuantity = Object.values(materialDraft.defectQuantities)
      .reduce((total, quantity) => total + quantity, 0)
    return lossQuantity > materialDraft.outputQuantity!
  })
  if (invalidLossMaterials.length) {
    throw new Error(`损耗数量不能大于完成数量：${invalidLossMaterials.map((material) => material.materialName).join('、')}`)
  }
  const device = activeProductionDevice.value
  if (!device) {
    return
  }
  const missingParameters = getProductionSubmittableParameters(device)
    .filter((parameter) => !isTextStandardParameter(parameter))
    .filter((parameter) => {
      const value = getProductionDeviceParameter(device.key, parameter.parameterCode)
      if (isBooleanParameter(parameter)) {
        return typeof value !== 'boolean'
      }
      if (isSelectParameter(parameter)) {
        return typeof value !== 'string' || !value.trim()
      }
      return toFiniteProductionParameterNumber(value) === undefined
    })
    .map((parameter) => parameter.parameterName || parameter.parameterCode)
  if (missingParameters.length) {
    throw new Error(`请填写设备参数：${missingParameters.join('、')}`)
  }
}

const buildProductionFormalSubmitConfirmation = () => {
  const materialDetails = buildProductionMaterialDetailsPayload()
  const progressQuantity = Math.min(...materialDetails.map((material) => material.outputQuantity))
  const materialSummary = configuredProductionMaterials.value
    .map((material, index) => {
      const detail = materialDetails[index]
      return `${material.materialName}=完成${detail.outputQuantity}件、损耗${detail.lossQuantity}件`
    })
    .join('；')
  const device = activeProductionDevice.value
  const parameterSummary = device
    ? getProductionSubmittableParameters(device).map((parameter) => {
        const label = parameter.parameterName || parameter.parameterCode
        if (isTextStandardParameter(parameter)) {
          return `${label}=${parameter.standardText || '未配置'}`
        }
        const value = getProductionDeviceParameter(device.key, parameter.parameterCode)
        if (isBooleanParameter(parameter)) {
          return `${label}=${value === true ? '是' : '否'}`
        }
        const status = resolveProductionParameterStatus(value, parameter)
        const statusLabel = status === 'NORMAL' ? '' : '（参数异常）'
        return `${label}=${value}${parameter.unit || ''}${statusLabel}`
      }).join('、')
    : ''
  const clearanceSummary = buildProductionClearanceConfirmationPayload()
    .map((confirmation) => `${confirmation.label}=${confirmation.confirmed ? '是' : '否'}`)
    .join('、')
  return [
    `生产订单：${productionOrderLabel.value}`,
    `工序：${selectedProcessLabel.value}`,
    `实际员工：${selectedEmployeeLabel.value}`,
    `物料：${materialSummary}`,
    `工序进度：${progressQuantity}件`,
    `完成数量：${productionDraft.outputQuantity}件`,
    `损耗数量：${productionScrapQuantity.value}件`,
    `设备：${device?.label || '无设备'}`,
    `设备参数：${
      activeProductionSelfCheckNarrative.value
        ? '生产自检说明'
        : parameterSummary || (device ? '无数值参数' : '无设备参数')
    }`,
    `清场确认：${clearanceSummary}`,
    '正式提交后不可修改，请核对无误后确认。'
  ].join('；')
}

const clearProductionFormalSubmitConfirmation = () => {
  productionFormalSubmitConfirmationText.value = ''
  submitConfirmationOpen.value = false
  productionFormalSubmitConfirmationResolver = undefined
}

const resolveProductionFormalSubmitConfirmation = (confirmed: boolean) => {
  const resolver = productionFormalSubmitConfirmationResolver
  if (!resolver) {
    return
  }
  if (!confirmed) {
    productionSignaturePassword.value = ''
  }
  clearProductionFormalSubmitConfirmation()
  resolver(confirmed)
}

const requestProductionFormalSubmitConfirmation = (confirmationText: string): Promise<boolean> => {
  if (productionFormalSubmitConfirmationResolver) {
    throw new Error('Production formal submit confirmation is already open.')
  }
  productionSignaturePassword.value = ''
  productionFormalSubmitConfirmationText.value = confirmationText
  submitConfirmationOpen.value = true
  return new Promise((resolve) => {
    productionFormalSubmitConfirmationResolver = resolve
  })
}

const cancelProductionFormalSubmitConfirmation = () => {
  resolveProductionFormalSubmitConfirmation(false)
}

const confirmProductionFormalSubmitConfirmation = () => {
  if (payloadLoading.value) {
    return
  }
  if (!productionSignaturePassword.value.trim()) {
    showFrontlineError('请输入所选员工的电子签名密码。')
    return
  }
  resolveProductionFormalSubmitConfirmation(true)
}

const openProductionSubmitSuccessDialog = () => {
  productionSubmitSuccessOpen.value = true
}

const closeProductionSubmitSuccessDialog = () => {
  productionSubmitSuccessOpen.value = false
}

const handleProductionFormalSubmit = async () => {
  if (
    payloadLoading.value ||
    submitConfirmationOpen.value ||
    productionSubmitSuccessOpen.value
  ) {
    return
  }
  assertProductionSubmissionReady()
  Object.assign(draft.fieldValues, buildProductionFieldValues())
  assertFormalPayloadContext()
  const templatePayload = buildFrontlineTemplatePayload(context, draft.fieldValues)
  const confirmed = await requestProductionFormalSubmitConfirmation(buildProductionFormalSubmitConfirmation())
  if (!confirmed) {
    return
  }
  const formalPayload = (() => {
    try {
      return buildFrontlineFormalSubmitPayload(templatePayload)
    } finally {
      productionSignaturePassword.value = ''
    }
  })()

  payloadLoading.value = true
  try {
    const submitResult = await ProFeedbackApi.frontlineSubmit(formalPayload)
    resetProductionSubmissionDraft()
    openProductionSubmitSuccessDialog()
    showParameterAuditWarning(submitResult)
  } finally {
    payloadLoading.value = false
  }
}

const assertPqcSignatureAndQuantityReady = (requirePassword = false) => {
  if (pqcInspectionQuantity.value <= 0) {
    throw new Error('PQC检验数量必须大于0。')
  }
  if (requirePassword && !pqcSignaturePassword.value.trim()) {
    throw new Error('请输入所选员工的电子签名密码。')
  }
}

const switchPqcCurrentLoginEmployeeForActiveTask = async () => {
  if (!isPqcMode.value || !activePqcTaskOption.value) {
    return
  }
  if (!isFrontlinePqcProcess(deviceState.selectedProcess)) {
    showFrontlineError('请先选择PQC工序。')
    return
  }
  const employee = findCurrentLoginEmployee()
  if (!employee) {
    showFrontlineError('当前登录账号未返回PQC人员候选，无法进入PQC填写。')
    return
  }
  await handleSelectEmployee(employee)
}

const assertPqcFormalSubmissionReady = () => {
  if (deviceState.loadingEmployees || deviceState.loadingTemplate) {
    throw new Error('PQC人员和任务正在切换，请稍后再提交。')
  }
  if (pqcTaskAvailabilityIssue.value) {
    throw new Error(pqcTaskAvailabilityIssue.value.message)
  }
  if (!deviceState.selectedEmployee || !activePqcTaskOption.value) {
    throw new Error('请先完成PQC人员和任务切换。')
  }
}

const handleValidate = async () => {
  clearFrontlineError()
  if (pqcSubmitResultUncertain.value) {
    showFrontlineError('PQC正式提交结果不确定，请刷新页面或联系组长核对后再操作。')
    return
  }
  if (!isPqcMode.value) {
    if (templateBindingMissing.value) {
      const error = new Error('当前员工缺少一线填写模板，无法提交。')
      showFrontlineError(error)
      return
    }
    if (templateModeMismatch.value) {
      const error = new Error(statusText.value)
      showFrontlineError(error)
      return
    }
    try {
      await handleProductionFormalSubmit()
    } catch (error) {
      showFrontlineError(error)
    }
    return
  }
  try {
    assertPqcFormalSubmissionReady()
    assertPqcSignatureAndQuantityReady()
    assertPqcCurrentProcessAllMethodSubmissionReady()
    assertPqcInspectionDisplayFieldsReady()
  } catch (error) {
    showFrontlineError(error)
    return
  }
  Object.assign(draft.fieldValues, buildPqcFieldValues())
  payloadPreview.value = undefined
  pqcSignaturePassword.value = ''
  pqcSignatureDialogVisible.value = true
}

const closePqcSignatureDialog = () => {
  if (payloadLoading.value) {
    return
  }
  pqcSignatureDialogVisible.value = false
  pqcSignaturePassword.value = ''
}

const recoverPqcSubmitReceiptAfterUncertainError = async (
  submitError: unknown,
  pqcTaskId: number | undefined = activePqcTaskOption.value?.pqcTaskId,
  resetRecoveredDraft = true
) => {
  const process = deviceState.selectedProcess
  if (!isFrontlinePqcProcess(process) || !pqcTaskId) {
    return false
  }
  try {
    const recoveredReceipt = await ProFeedbackApi.getFrontlinePqcSubmitReceipt({
      pqcTaskId
    })
    if (!recoveredReceipt) {
      return false
    }
    if (resetRecoveredDraft) {
      resetPqcSubmissionDraft(recoveredReceipt.pqcTaskId)
    }
    message.success(`PQC正式提交已完成，已恢复事件编号 ${recoveredReceipt.pqcEventId}`)
    return true
  } catch (confirmationError) {
    pqcSubmitResultUncertain.value = true
    pqcSignatureDialogVisible.value = false
    showFrontlineError(
      `PQC正式提交结果不确定，状态确认失败：${resolveErrorMessage(confirmationError)}；` +
      `原始提交错误：${resolveErrorMessage(submitError)}。请刷新页面或联系组长核对后再操作。`
    )
    return true
  }
}

const handleConfirmPqcSubmit = async () => {
  if (payloadLoading.value || pqcSubmitResultUncertain.value) {
    return
  }
  if (!pqcSignaturePassword.value.trim()) {
    showFrontlineError('请输入所选员工的电子签名密码。')
    return
  }
  try {
    assertPqcSignatureAndQuantityReady(true)
    assertPqcCurrentProcessAllMethodSubmissionReady()
  } catch (error) {
    showFrontlineError(error)
    return
  }
  let submitPayloads: FrontlinePqcInspectionSubmitReqVO[]
  try {
    submitPayloads = buildPqcInspectionSubmitPayloads()
  } catch (error) {
    showFrontlineError(error)
    return
  }
  payloadLoading.value = true
  try {
    const submitReceipts: FrontlinePqcInspectionSubmitRespVO[] = []
    for (const submitPayload of submitPayloads) {
      try {
        const submitReceipt = await ProFeedbackApi.submitFrontlinePqcInspection(
          submitPayload
        )
        submitReceipts.push(submitReceipt)
      } catch (error) {
        const recovered = await recoverPqcSubmitReceiptAfterUncertainError(
          error,
          submitPayload.pqcTaskId,
          false
        )
        if (!recovered || pqcSubmitResultUncertain.value) {
          throw error
        }
      }
    }
    resetPqcSubmissionDrafts(submitPayloads.map((payload) => payload.pqcTaskId))
    clearFrontlineError()
    const eventIds = submitReceipts.map((receipt) => receipt.pqcEventId).join('、')
    const receiptText = eventIds || '已恢复正式回执'
    message.success(
      submitPayloads.length > 1
        ? `PQC正式提交成功，已提交 ${submitPayloads.length} 个检验方法，事件编号 ${receiptText}`
        : `PQC正式提交成功，事件编号 ${receiptText}`
    )
  } catch (error) {
    if (!pqcSubmitResultUncertain.value) {
      showFrontlineError(error)
    }
  } finally {
    pqcSignaturePassword.value = ''
    payloadLoading.value = false
  }
}

const assertFormalPayloadContext = () => {
  const missingFields: string[] = []
  if (isPqcMode.value && !context.workOrderId) {
    missingFields.push('订单上下文')
  }
  if (!context.routeId) {
    missingFields.push('路线')
  }
  if (!isPqcMode.value && (!context.processId || !context.routeProcessId)) {
    missingFields.push('工序')
  }
  if (!context.actualEmployeeId) {
    missingFields.push('员工')
  }
  if (missingFields.length) {
    throw new Error(`缺少${missingFields.join('、')}，无法提交。`)
  }
}

interface FrontlineFormalSubmitContext {
  activeOrderId?: number
  workOrderId?: number
  workOrderCode?: string
  taskId?: number
  routeId?: number
  routeProcessId?: number
  processId?: number
  workstationId?: number
  deviceId?: number
  deviceAccountUserId?: number
  itemId?: number
  approveUserId?: number
  recordbookId?: number
  signatureEmployeeId?: number
  signaturePassword?: string
  scheduleOrderId?: number
  scheduleOrderProcessId?: number
  scheduledQuantity?: number
  expireDate?: string
  frontlineSessionSnapshotId?: string
  frontlineSessionSnapshotHash?: string
  activeOrderProcessSnapshotId?: number
}

const readFrontlineFormalSubmitContext = (): FrontlineFormalSubmitContext => {
  const selectedActiveOrder = deviceState.selectedActiveOrder
  const serverContext = deviceState.runtimeConfig?.productionSubmitContext
  return {
    activeOrderId: selectedActiveOrder?.activeOrderId,
    workOrderId: selectedActiveOrder?.workOrderId,
    workOrderCode: selectedActiveOrder?.workOrderCode,
    taskId: serverContext?.taskId,
    routeId: serverContext?.routeId,
    routeProcessId: serverContext?.routeProcessId,
    processId: serverContext?.processId,
    workstationId: serverContext?.workstationId,
    deviceId: activeProductionDevice.value?.key
      ? Number(activeProductionDevice.value.key)
      : undefined,
    deviceAccountUserId: Number(userStore.getUser?.id || 0),
    itemId: serverContext?.itemId,
    approveUserId: serverContext?.approveUserId,
    recordbookId: serverContext?.recordbookId,
    signatureEmployeeId: context.actualEmployeeId,
    signaturePassword: productionSignaturePassword.value.trim(),
    scheduleOrderId: serverContext?.scheduleOrderId,
    scheduleOrderProcessId: serverContext?.scheduleOrderProcessId,
    scheduledQuantity: serverContext?.scheduledQuantity,
    expireDate: serverContext?.expireDate ? String(serverContext.expireDate) : undefined,
    frontlineSessionSnapshotId: deviceState.runtimeConfig?.frontlineSessionSnapshotId,
    frontlineSessionSnapshotHash: deviceState.runtimeConfig?.frontlineSessionSnapshotHash,
    activeOrderProcessSnapshotId: serverContext?.activeOrderProcessSnapshotId
  }
}

const assertProductionSubmitSnapshotContext = (formalContext: FrontlineFormalSubmitContext) => {
  const selectedProcess = deviceState.selectedProcess
  const selectedEmployee = deviceState.selectedEmployee
  const snapshotContext = deviceState.runtimeConfig?.productionSubmitContext
  if (!isFrontlineProductionProcess(selectedProcess)) {
    throw new Error('当前提交快照缺少正式生产工序，无法提交。')
  }
  if (!snapshotContext) {
    throw new Error('当前提交快照缺少正式运行配置，无法提交。')
  }
  if (
    formalContext.routeId !== selectedProcess.routeId ||
    formalContext.routeProcessId !== selectedProcess.routeProcessId ||
    formalContext.processId !== selectedProcess.processId
  ) {
    throw new Error('当前提交快照与所选工序不一致，请重新选择活跃订单或工序。')
  }
  if (
    snapshotContext.routeId !== formalContext.routeId ||
    snapshotContext.routeProcessId !== formalContext.routeProcessId ||
    snapshotContext.processId !== formalContext.processId
  ) {
    throw new Error('当前提交快照与运行配置不一致，请刷新后重试。')
  }
  if (!selectedEmployee || selectedEmployee.userId !== formalContext.signatureEmployeeId) {
    throw new Error('当前提交快照与所选员工不一致，请重新选择员工。')
  }
  if (!formalContext.frontlineSessionSnapshotId || !formalContext.frontlineSessionSnapshotHash) {
    throw new Error('缺少一线生产会话快照，无法提交。')
  }
}

const assertFrontlineFormalSubmitContext = (formalContext: FrontlineFormalSubmitContext) => {
  const missingFields: string[] = []
  const requiredFields: Array<[keyof FrontlineFormalSubmitContext, string]> = [
    ['activeOrderId', '活跃订单编号'],
    ['workOrderId', '活跃订单'],
    ['routeId', '路线'],
    ['routeProcessId', '路线工序'],
    ['processId', '工序'],
    ['workstationId', '工作站'],
    ['deviceAccountUserId', '设备账号'],
    ['approveUserId', '班组长审批人'],
    ['activeOrderProcessSnapshotId', '活跃订单工序快照'],
    ['signaturePassword', '签名'],
    ['signatureEmployeeId', '签名员工']
  ]
  for (const [field, label] of requiredFields) {
    const value = formalContext[field]
    if (value === undefined || value === null || value === '' || Number(value) <= 0) {
      missingFields.push(label)
    }
  }
  if (
    formalContext.signatureEmployeeId &&
    context.actualEmployeeId &&
    formalContext.signatureEmployeeId !== context.actualEmployeeId
  ) {
    throw new Error('签名员工必须等于实际填写员工，无法提交。')
  }
  const activeOrder = selectedActiveOrder.value
  const selectedProcess = deviceState.selectedProcess
  if (
    !activeOrder ||
    !isFrontlineProductionProcess(selectedProcess) ||
    activeOrder.activeOrderId !== formalContext.activeOrderId ||
    activeOrder.routeId !== selectedProcess.routeId
  ) {
    throw new Error('订单与工序上下文不一致，请重新选择活跃订单或工序。')
  }
  if (missingFields.length) {
    throw new Error(`缺少${missingFields.join('、')}，无法提交。`)
  }
}

const buildFrontlineProductionSubmitIdempotencyKey = () => {
  const submitIdempotencyKey = `frontline-submit-${productionSubmitDraftKey.value}`
  if (submitIdempotencyKey.length > FRONTLINE_PRODUCTION_IDEMPOTENCY_KEY_MAX_LENGTH) {
    throw new Error('一线生产提交幂等键超过服务端长度限制，无法提交。')
  }
  return submitIdempotencyKey
}

const buildFrontlineFormalSubmitPayload = (
  rawPayload: FrontlineTemplatePayloadReqVO
): ProFrontlineFeedbackSubmitReqVO => {
  const formalContext = readFrontlineFormalSubmitContext()
  assertProductionSubmitSnapshotContext(formalContext)
  assertFrontlineFormalSubmitContext(formalContext)
  const signaturePassword = productionSignaturePassword.value.trim()
  if (!signaturePassword) {
    throw new Error('请输入所选员工的电子签名密码。')
  }
  const selectedDevice = activeProductionDevice.value
  const materialDetails = buildProductionMaterialDetailsPayload()
  const progressQuantity = Math.min(...materialDetails.map((material) => material.outputQuantity))
  const totalLossQuantity = materialDetails.reduce(
    (total, material) => total + material.lossQuantity,
    0
  )
  const equipmentParameters = selectedDevice
    ? { [selectedDevice.label]: buildProductionDeviceParameterPayload(selectedDevice.key) }
    : {}
  const submitIdempotencyKey = buildFrontlineProductionSubmitIdempotencyKey()
  const recordbookPayload = formalContext.recordbookId
    ? {
        recordbookId: formalContext.recordbookId,
        entryTitle:
          firstRouteQueryText(['recordbookEntryTitle']) ||
          `一线报工-${selectedProcessLabel.value}-${selectedEmployeeLabel.value}`,
        entryContent: {
          fieldValues: { ...draft.fieldValues },
          defects: { ...productionDefectDraft },
          materialDetails,
          clearanceConfirmations: buildProductionClearanceConfirmationPayload(),
          productionOrder: productionOrderLabel.value,
          process: selectedProcessLabel.value,
          employee: selectedEmployeeLabel.value
        },
        equipmentParameters,
        tagCodes: [],
        idempotencyKey: submitIdempotencyKey,
        remark: firstRouteQueryText(['recordbookRemark'])
      }
    : undefined
  const runtimeConfig = deviceState.runtimeConfig!
  return {
    materialDetails,
    feedbackPayload: {
      workstationId: formalContext.workstationId!,
      routeId: formalContext.routeId!,
      processId: formalContext.processId!,
      workOrderId: formalContext.workOrderId,
      taskId: formalContext.taskId,
      activeOrderProcessSnapshotId: formalContext.activeOrderProcessSnapshotId,
      scheduleOrderId: formalContext.scheduleOrderId,
      scheduleOrderProcessId: formalContext.scheduleOrderProcessId,
      itemId: formalContext.itemId,
      expireDate: formalContext.expireDate,
      scheduledQuantity: formalContext.scheduledQuantity,
      outputQuantity: progressQuantity,
      lossQuantity: totalLossQuantity,
      lossDetails: buildProductionLossDetailsPayload(),
      selectedDevice: buildProductionSelectedDevicePayload(),
      deviceParameterReadings: buildProductionDeviceParameterReadingsPayload(),
      laborScrapQuantity: totalLossQuantity,
      materialScrapQuantity: 0,
      otherScrapQuantity: 0,
      approveUserId: formalContext.approveUserId!,
      remark: firstRouteQueryText(['feedbackRemark', 'remark'])
    },
    recordbookPayload,
    processPoolSubmissionIdempotencyKey: submitIdempotencyKey,
    processPoolContext: {
      activeOrderId: formalContext.activeOrderId!,
      workOrderId: formalContext.workOrderId,
      taskId: formalContext.taskId,
      routeId: formalContext.routeId!,
      routeProcessId: formalContext.routeProcessId!,
      processId: formalContext.processId!,
      workstationId: formalContext.workstationId!,
      deviceId: formalContext.deviceId,
      deviceAccountUserId: formalContext.deviceAccountUserId!,
      templateType: context.templateCode || expectedTemplateCode.value
    },
    actualEmployeeId: context.actualEmployeeId!,
    signatureEmployeeId: formalContext.signatureEmployeeId!,
    signaturePassword,
    frontlineSessionSnapshotId: runtimeConfig.frontlineSessionSnapshotId,
    frontlineSessionSnapshotHash: runtimeConfig.frontlineSessionSnapshotHash,
    rawPayload: buildProductionStructuredRawPayload(
      rawPayload,
      formalContext,
      materialDetails
    ) as unknown as Record<string, unknown>
  }
}

const buildProductionDeviceParameterPayload = (deviceKey: string) => {
  const device = visibleDeviceCards.value.find((item) => item.key === deviceKey)
  const params = deviceParameterDraft[deviceKey] || {}
  const parameterCodes = new Set(
    getProductionSubmittableParameters(device).map((parameter) => parameter.parameterCode)
  )
  return Object.fromEntries(
    Object.entries(params).filter(
      ([parameterCode, value]) => parameterCodes.has(parameterCode) && value !== undefined
    )
  )
}

const buildProductionLossDetailsFromDraft = (
  defectQuantities: Record<ProductionDefectKey, number>
): ProFrontlineLossDetailReqVO[] =>
  configuredDefectReasons.value
    .map((defect) => ({
      reasonId: defect.reasonId,
      reasonCode: defect.reasonCode,
      reasonName: defect.label,
      quantity: defectQuantities[defect.key] || 0
    }))
    .filter((defect) => defect.quantity > 0)

const buildProductionLossDetailsPayload = (): ProFrontlineLossDetailReqVO[] =>
  buildProductionLossDetailsFromDraft(productionDefectDraft)

const buildProductionSelectedDeviceFromDevice = (
  device?: ProductionDeviceCard
): ProFrontlineSelectedDeviceReqVO | undefined => {
  if (!device) {
    return undefined
  }
  return {
    deviceId: device.deviceId,
    deviceCode: device.deviceCode,
    deviceName: device.deviceName || device.label
  }
}

const buildProductionSelectedDevicePayload = (): ProFrontlineSelectedDeviceReqVO | undefined =>
  buildProductionSelectedDeviceFromDevice(activeProductionDevice.value)

const buildProductionDeviceParameterReadingsFromDraft = (
  device: ProductionDeviceCard | undefined,
  parameterDraft: Record<string, ProductionDeviceParameterDraft>
): ProFrontlineDeviceParameterReadingReqVO[] => {
    if (!device) {
      return []
    }
    const parameterValues = parameterDraft[device.key] || {}
    return getProductionSubmittableParameters(device)
      .filter((parameter) => !isTextStandardParameter(parameter))
      .map<ProFrontlineDeviceParameterReadingReqVO | undefined>((parameter) => {
        const value = parameterValues[parameter.parameterCode]
        if (isSelectParameter(parameter)) {
          const textValue = typeof value === 'string' ? value.trim() : ''
          if (!textValue) {
            return undefined
          }
          return {
            deviceId: device.deviceId,
            deviceCode: device.deviceCode,
            deviceName: device.deviceName || device.label,
            parameterCode: parameter.parameterCode,
            parameterName: parameter.parameterName,
            unit: parameter.unit,
            textValue,
            lowerLimit: parameter.lowerLimit ?? undefined,
            upperLimit: parameter.upperLimit ?? undefined,
            parameterStatus: 'NORMAL'
          }
        }
        if (isBooleanParameter(parameter)) {
          if (typeof value !== 'boolean') {
            throw new Error(`BOOLEAN 设备参数值无效：${parameter.parameterCode}`)
          }
          const booleanValue = value
          return {
            deviceId: device.deviceId,
            deviceCode: device.deviceCode,
            deviceName: device.deviceName || device.label,
            parameterCode: parameter.parameterCode,
            parameterName: parameter.parameterName,
            unit: parameter.unit,
            value: booleanValue ? 1 : 0,
            parameterStatus: 'NORMAL'
          }
        }
        const numericValue = toFiniteProductionParameterNumber(value)
        if (numericValue === undefined) {
          return undefined
        }
        return {
          deviceId: device.deviceId,
          deviceCode: device.deviceCode,
          deviceName: device.deviceName || device.label,
          parameterCode: parameter.parameterCode,
          parameterName: parameter.parameterName,
          unit: parameter.unit,
          value: numericValue,
          lowerLimit: parameter.lowerLimit ?? undefined,
          upperLimit: parameter.upperLimit ?? undefined,
          parameterStatus: resolveProductionParameterStatus(value, parameter)
        }
      })
      .filter((item): item is ProFrontlineDeviceParameterReadingReqVO => item !== undefined)
  }

const buildProductionDeviceParameterReadingsPayload =
  (): ProFrontlineDeviceParameterReadingReqVO[] =>
    buildProductionDeviceParameterReadingsFromDraft(
      activeProductionDevice.value,
      deviceParameterDraft
    )

const buildProductionMaterialDetailsPayload = (): ProFrontlineFeedbackMaterialReqVO[] => {
  persistActiveProductionMaterialDraft()
  return configuredProductionMaterials.value.map((material) => {
    const materialDraft = productionMaterialDrafts[material.key]
    if (!materialDraft || materialDraft.outputQuantity === undefined) {
      throw new Error(`请填写完成数量：${material.materialName}`)
    }
    const lossDetails = buildProductionLossDetailsFromDraft(materialDraft.defectQuantities)
    const selectedDevice = visibleDeviceCards.value.find(
      (device) => device.key === materialDraft.selectedDeviceKey
    )
    return {
      materialId: material.materialId,
      outputQuantity: materialDraft.outputQuantity,
      lossQuantity: lossDetails.reduce((total, detail) => total + detail.quantity, 0),
      lossDetails,
      selectedDevice: buildProductionSelectedDeviceFromDevice(selectedDevice),
      deviceParameterReadings: buildProductionDeviceParameterReadingsFromDraft(
        selectedDevice,
        materialDraft.deviceParameters
      )
    }
  })
}

const buildProductionEquipmentParameterRulesPayload = () =>
  activeProductionDevice.value
    ? Object.fromEntries([[
      activeProductionDevice.value.label,
      getProductionSubmittableParameters(activeProductionDevice.value).map((parameter) => ({
        parameterCode: parameter.parameterCode,
        parameterName: parameter.parameterName,
        unit: parameter.unit,
        lowerLimit: parameter.lowerLimit,
        upperLimit: parameter.upperLimit,
        valueType: parameter.valueType,
        standardText: parameter.standardText,
        optionValues: parameter.optionValues,
        defaultText: parameter.defaultText,
        decimalScale: parameter.decimalScale
      }))
    ]])
    : {}

const buildProductionStructuredRawPayload = (
  rawPayload: FrontlineTemplatePayloadReqVO,
  formalContext: FrontlineFormalSubmitContext,
  materialDetails: ProFrontlineFeedbackMaterialReqVO[]
) => ({
  ...rawPayload,
  activeOrderProcess: {
    activeOrderId: formalContext.activeOrderId,
    activeOrderProcessSnapshotId: formalContext.activeOrderProcessSnapshotId,
    workOrderId: formalContext.workOrderId,
    routeId: formalContext.routeId,
    routeProcessId: formalContext.routeProcessId,
    processId: formalContext.processId
  },
  materialDetails,
  lossDetails: buildProductionLossDetailsPayload(),
  lossReasonDetails: buildProductionLossDetailsPayload(),
  selectedDevice: buildProductionSelectedDevicePayload(),
  deviceParameterReadings: buildProductionDeviceParameterReadingsPayload(),
  deviceMeteringValidity: buildProductionDeviceMeteringValidityPayload(),
  clearanceConfirmations: buildProductionClearanceConfirmationPayload(),
  equipmentParameterRules: buildProductionEquipmentParameterRulesPayload()
})

const buildProductionFieldValues = () => {
  const selectedDevice = activeProductionDevice.value
  return {
    [FRONTLINE_FIELD_CODES.DEVICE]: selectedDevice ? selectedDevice.label : '无设备',
    [FRONTLINE_FIELD_CODES.DEVICE_PARAMETERS]: selectedDevice
      ? {
          [selectedDevice.label]: buildProductionDeviceParameterPayload(selectedDevice.key)
        }
      : {},
    [FRONTLINE_FIELD_CODES.OUTPUT_QUANTITY]: productionDraft.outputQuantity,
    [FRONTLINE_FIELD_CODES.SCRAP_QUANTITY]: productionScrapQuantity.value
  }
}

const buildPqcFieldValues = () => ({
  [FRONTLINE_FIELD_CODES.PQC_RESULT]: resolvePqcResult()
})

const buildPqcPieceValuesPayloadForTask = (taskOption: PqcTaskOptionSnapshot) => {
  const values: Record<string, string[]> = {}
  for (const item of taskOption.inspectionItems.map(mapPqcInspectionItem)) {
    values[item.key] = getPqcExactPieceValuesForTask(item.key, taskOption)
  }
  return values
}

const buildPqcPieceValuesPayload = () => {
  const taskOption = activePqcTaskOption.value
  if (!taskOption) {
    return {}
  }
  const values: Record<string, string[]> = {}
  for (const item of taskOption.inspectionItems.map(mapPqcInspectionItem)) {
    values[item.key] = getPqcExactPieceValuesForSubmit(item.key)
  }
  return values
}

const buildPqcInspectionSubmitPayloadForTask = (
  taskOption: PqcTaskOptionSnapshot
): FrontlinePqcInspectionSubmitReqVO => {
  assertPqcInspectionDisplayFieldsReady()
  const activeOrder = deviceState.selectedActiveOrder
  const process = deviceState.selectedProcess
  const employee = deviceState.selectedEmployee
  if (!isFrontlinePqcProcess(process) || !employee) {
    throw new Error('缺少PQC任务上下文，无法提交。')
  }
  assertPqcSubmissionSampleQuantitiesForTask(taskOption)
  const taskDraft = getPqcTaskDraft(taskOption)
  const inspectionResult = resolvePqcResultForTask(taskOption)
  const itemResults = buildPqcItemResultsPayload(taskOption)
  const pqcItemDetails = buildPqcItemDetailsPayload(taskOption)
  const taskFieldValues = {
    [FRONTLINE_FIELD_CODES.PQC_RESULT]: inspectionResult
  }
  return {
    activeOrderId: process.activeOrderId,
    pqcTaskId: taskOption.pqcTaskId,
    regulationVersionId: taskOption.regulationVersionId,
    qaProcessId: taskOption.qaProcessId,
    actualEmployeeId: employee.userId,
    workOrderId: activeOrder?.workOrderId,
    routeId: process.routeId,
    inspectionType: taskOption.inspectionType,
    businessDate: taskOption.businessDate,
    shiftCode: taskOption.shiftCode,
    roundNo: taskOption.roundNo,
    actualInspectionQuantity: getPqcInspectionQuantityForTask(taskOption),
    scrapQuantity: normalizePqcQuantity(taskDraft.scrapQuantity),
    signaturePassword: pqcSignaturePassword.value,
    itemResults,
    rawPayload: {
      pqcDraft: {
        inspectionType: taskOption.inspectionType,
        patrolRound: taskOption.roundNo,
        inspectionQuantity: getPqcInspectionQuantityForTask(taskOption),
        scrapQuantity: normalizePqcQuantity(taskDraft.scrapQuantity)
      },
      pqcPieceValues: buildPqcPieceValuesPayloadForTask(taskOption),
      pqcItemDetails,
      itemResults,
      fieldValues: taskFieldValues,
      inspectionResult,
      selectedActiveOrder: activeOrder ? { ...activeOrder } : undefined,
      selectedProcess: { ...process },
      selectedEmployee: employee ? { ...employee } : undefined
    },
    clientSubmitTime: formatLocalDateTime()
  }
}

function buildPqcInspectionSubmitPayloads(): FrontlinePqcInspectionSubmitReqVO[] {
  persistCurrentPqcTaskDraft()
  const taskOptions = getPqcCurrentSubmitTaskOptions()
  const payloads = taskOptions.map((taskOption) =>
    buildPqcInspectionSubmitPayloadForTask(taskOption))
  if (
    taskOptions.length === 1 &&
    taskOptions[0].pqcTaskId === activePqcTaskOption.value?.pqcTaskId
  ) {
    payloads[0] = buildPqcInspectionSubmitPayload()
  }
  return payloads
}

const buildPqcInspectionSubmitPayload = (): FrontlinePqcInspectionSubmitReqVO => {
  const taskOption = activePqcTaskOption.value
  if (!taskOption) {
    throw new Error('缺少PQC任务上下文，无法提交。')
  }
  const payload = buildPqcInspectionSubmitPayloadForTask(taskOption)
  return {
    ...payload,
    itemResults: buildPqcItemResultsPayload(),
    rawPayload: {
      ...(payload.rawPayload || {}),
      pqcDraft: {
        ...(payload.rawPayload?.pqcDraft as Record<string, unknown> | undefined)
      },
      pqcPieceValues: buildPqcPieceValuesPayload(),
      itemResults: buildPqcItemResultsPayload()
    }
  }
}

const formatLocalDateTime = (date = new Date()) => {
  const pad = (value: number) => String(value).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join('-') + `T${[
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds())
  ].join(':')}`
}

const resolvePqcResultForTask = (taskOption: PqcTaskOptionSnapshot) => {
  const taskDraft = getPqcTaskDraft(taskOption)
  if (normalizePqcQuantity(taskDraft.scrapQuantity) > 0) {
    return FRONTLINE_PQC_RESULTS.DETECTION_FAILED
  }
  for (const taskItem of taskOption.inspectionItems.map(mapPqcInspectionItem)) {
    const itemKey = taskItem.key
    const item = pqcInspectionItemMap.value[itemKey]
    const values = getPqcCurrentChoiceValuesForTask(itemKey, taskOption)
    if (values.some((value) => value === '不合格')) {
      return FRONTLINE_PQC_RESULTS.DETECTION_FAILED
    }
    if (item?.type === 'number' && values.filter((value) => value.trim().length > 0).some((value) => {
      const measuredValue = Number(value)
      const lowerLimit = item.standardLowerLimit === undefined || item.standardLowerLimit === null
        ? undefined
        : Number(item.standardLowerLimit)
      const upperLimit = item.standardUpperLimit === undefined || item.standardUpperLimit === null
        ? undefined
        : Number(item.standardUpperLimit)
      return !Number.isFinite(measuredValue) ||
        (lowerLimit !== undefined && measuredValue < lowerLimit) ||
        (upperLimit !== undefined && measuredValue > upperLimit)
    })) {
      return FRONTLINE_PQC_RESULTS.DETECTION_FAILED
    }
  }
  return FRONTLINE_PQC_RESULTS.DETECTION_SUCCESS
}

const resolvePqcResult = () => {
  const taskOption = activePqcTaskOption.value
  return taskOption
    ? resolvePqcResultForTask(taskOption)
    : FRONTLINE_PQC_RESULTS.DETECTION_SUCCESS
}

const applyActiveOrderToContext = (activeOrder: FrontlineActiveOrderVO) => {
  context.workOrderId = activeOrder.workOrderId
  context.routeId = activeOrder.routeId
  context.routeProcessId = undefined
  context.processId = undefined
  context.actualEmployeeId = undefined
}

const applyProcessToContext = (
  process: FrontlineDeviceRouteProcessVO | FrontlinePqcProcessVO
) => {
  context.routeId = process.routeId
  context.routeProcessId = isFrontlineProductionProcess(process) ? process.routeProcessId : undefined
  context.processId = isFrontlineProductionProcess(process) ? process.processId : undefined
}

const hydrateContextFromRoute = () => {
  context.workOrderId = firstRouteQueryNumber(['workOrderId', 'productionOrderId', 'orderId'])
  context.routeId = firstRouteQueryNumber(['routeId']) ?? context.routeId
  context.routeProcessId = firstRouteQueryNumber(['routeProcessId']) ?? context.routeProcessId
  context.processId = firstRouteQueryNumber(['processId']) ?? context.processId
  if (!isPqcMode.value) {
    context.actualEmployeeId = firstRouteQueryNumber(['actualEmployeeId']) ?? context.actualEmployeeId
  }
  productionDraft.outputQuantity = firstRouteQueryNumber(['outputQuantity', 'submitQuantity']) ?? productionDraft.outputQuantity
  const queryTemplateCode = resolveTemplateCode(firstRouteQueryText(['templateCode', 'templateNo']))
  employeeTemplateCode.value = queryTemplateCode
  context.templateCode = expectedTemplateCode.value
}

const firstRouteQueryText = (keys: string[]) => {
  for (const key of keys) {
    const value = route.query[key]
    const text = Array.isArray(value) ? value[0] : value
    if (text) {
      return String(text)
    }
  }
  return undefined
}

const firstRouteQueryNumber = (keys: string[]) => {
  const text = firstRouteQueryText(keys)
  if (!text) {
    return undefined
  }
  const value = Number(text)
  return Number.isFinite(value) && value > 0 ? value : undefined
}

const resolveTemplateCode = (
  templateNo?: string,
  templateType?: string
): FrontlineTemplateCode | undefined => {
  if (templateNo === FRONTLINE_TEMPLATE_CODES.PRODUCTION_SIMPLIFIED) {
    return FRONTLINE_TEMPLATE_CODES.PRODUCTION_SIMPLIFIED
  }
  if (templateNo === FRONTLINE_TEMPLATE_CODES.PQC_SIMPLIFIED) {
    return FRONTLINE_TEMPLATE_CODES.PQC_SIMPLIFIED
  }
  if (templateType === 'PRODUCTION') {
    return FRONTLINE_TEMPLATE_CODES.PRODUCTION_SIMPLIFIED
  }
  if (templateType === 'PQC') {
    return FRONTLINE_TEMPLATE_CODES.PQC_SIMPLIFIED
  }
  return undefined
}

const isSameProcess = (
  left?: FrontlineDeviceRouteProcessVO | FrontlinePqcProcessVO,
  right?: FrontlineDeviceRouteProcessVO | FrontlinePqcProcessVO
) => {
  if (isFrontlinePqcProcess(left) && isFrontlinePqcProcess(right)) {
    return left.regulationVersionId === right.regulationVersionId &&
      left.qaProcessId === right.qaProcessId
  }
  if (isFrontlineProductionProcess(left) && isFrontlineProductionProcess(right)) {
    return left.activeOrderId === right.activeOrderId &&
      left.routeId === right.routeId &&
      left.routeProcessId === right.routeProcessId &&
      left.processId === right.processId
  }
  return false
}

const selectedProductionProcessIndex = computed(() => {
  if (isPqcMode.value || !deviceState.selectedProcess) {
    return -1
  }
  return switchableProcessOptions.value.findIndex((process) =>
    isFrontlineProductionProcess(process) && isSameProcess(process, deviceState.selectedProcess)
  )
})

const previousProductionProcess = computed(() => {
  const selectedIndex = selectedProductionProcessIndex.value
  return selectedIndex > 0
    ? switchableProcessOptions.value[selectedIndex - 1]
    : undefined
})

const nextProductionProcess = computed(() => {
  const selectedIndex = selectedProductionProcessIndex.value
  return selectedIndex >= 0 && selectedIndex < switchableProcessOptions.value.length - 1
    ? switchableProcessOptions.value[selectedIndex + 1]
    : undefined
})

const isProductionProcessNavigationBlocked = computed(() =>
  isPqcMode.value ||
  payloadLoading.value ||
  submitConfirmationOpen.value ||
  productionSubmitSuccessOpen.value ||
  deviceState.loadingProcesses ||
  deviceState.loadingEmployees ||
  deviceState.loadingTemplate
)

const isProductionProcessPreviousDisabled = computed(() =>
  isProductionProcessNavigationBlocked.value || !previousProductionProcess.value
)

const isProductionProcessNextDisabled = computed(() =>
  isProductionProcessNavigationBlocked.value || !nextProductionProcess.value
)

const selectedPqcProcessIndex = computed(() => {
  if (!isPqcMode.value || !isFrontlinePqcProcess(deviceState.selectedProcess)) {
    return -1
  }
  return switchablePqcProcessOptions.value.findIndex((process) =>
    isSameProcess(process, deviceState.selectedProcess)
  )
})

const previousPqcProcess = computed(() => {
  const selectedIndex = selectedPqcProcessIndex.value
  return selectedIndex > 0
    ? switchablePqcProcessOptions.value[selectedIndex - 1]
    : undefined
})

const nextPqcProcess = computed(() => {
  const selectedIndex = selectedPqcProcessIndex.value
  return selectedIndex >= 0 && selectedIndex < switchablePqcProcessOptions.value.length - 1
    ? switchablePqcProcessOptions.value[selectedIndex + 1]
    : undefined
})

const isPqcProcessNavigationBlocked = computed(() =>
  !isPqcMode.value ||
  payloadLoading.value ||
  pqcSignatureDialogVisible.value ||
  pqcSubmitResultUncertain.value ||
  deviceState.loadingProcesses ||
  deviceState.loadingEmployees ||
  deviceState.loadingTemplate
)

const isPqcProcessPreviousDisabled = computed(() =>
  isPqcProcessNavigationBlocked.value || !previousPqcProcess.value
)

const isPqcProcessNextDisabled = computed(() =>
  isPqcProcessNavigationBlocked.value || !nextPqcProcess.value
)

const formatActiveOrderLabel = (activeOrder?: FrontlineActiveOrderVO) => {
  if (!activeOrder) {
    return '未选择'
  }
  return activeOrder.workOrderCode || activeOrder.workOrderName || `订单 ${activeOrder.workOrderId}`
}

const formatProcessLabel = (
  process?: FrontlineDeviceRouteProcessVO | FrontlinePqcProcessVO
) => {
  if (!process) {
    return '未选择'
  }
  if (isFrontlinePqcProcess(process)) {
    const sortText = process.qaProcessSort ? `${process.qaProcessSort}. ` : ''
    return `${sortText}${process.qaProcessName || process.qaProcessCode || process.qaProcessId}`
  }
  const sortText = process.sort ? `${process.sort}. ` : ''
  return `${sortText}${process.processName || process.processCode || process.processId}`
}

const formatEmployeeLabel = (employee?: FrontlineEmployeeCandidateVO) => {
  if (!employee) {
    return '未选择'
  }
  return employee.nickname || employee.username || String(employee.userId)
}

const formatPqcLoginEmployeeLabel = (employee?: FrontlineEmployeeCandidateVO) => {
  if (!employee) {
    return '未选择'
  }
  if (isCurrentLoginEmployee(employee)) {
    const username = userStore.getUser?.username?.trim()
    if (username) {
      return username
    }
    return '未选择'
  }
  return formatEmployeeLabel(employee)
}

const formatTemplateName = (templateCode?: FrontlineTemplateCode) => {
  if (templateCode === FRONTLINE_TEMPLATE_CODES.PQC_SIMPLIFIED) {
    return 'PQC填写'
  }
  if (templateCode === FRONTLINE_TEMPLATE_CODES.PRODUCTION_SIMPLIFIED) {
    return '生产填写'
  }
  return '未知模板'
}

const initializeProductionSelection = async () => {
  const activeOrders = await loadFrontlineProductionActiveOrders(deviceState)
  const requestedActiveOrder = context.workOrderId
    ? activeOrders.find((order) =>
      order.workOrderId === context.workOrderId &&
      (!context.routeId || order.routeId === context.routeId)
    )
    : undefined
  const initialActiveOrder = requestedActiveOrder || activeOrders[0]
  if (initialActiveOrder) {
    const requestedProcessIdentity = requestedActiveOrder
      ? {
          routeId: requestedActiveOrder.routeId,
          routeProcessId: context.routeProcessId,
          processId: context.processId
        }
      : undefined
    await handleSelectActiveOrder(initialActiveOrder, requestedProcessIdentity)
  }
}

onMounted(async () => {
  document.addEventListener('fullscreenchange', syncPqcFullscreenState)
  window.addEventListener('resize', scheduleProductionViewportScaleUpdate)
  try {
    if (!isPqcMode.value) {
      if (typeof ResizeObserver !== 'function') {
        throw new Error('当前浏览器不支持一线生产填写页面缩放观察。')
      }
      productionViewportResizeObserver = new ResizeObserver(scheduleProductionViewportScaleUpdate)
      if (frontlinePanelRef.value) {
        productionViewportResizeObserver.observe(frontlinePanelRef.value)
      }
    }
    syncPqcFullscreenState()
    hydrateContextFromRoute()
    const catalogRequest = FrontlineTemplateApi.getCatalog()
    if (isPqcMode.value) {
      catalog.value = await catalogRequest
      const activeOrders = await loadFrontlinePqcActiveOrders(deviceState)
      const requestedActiveOrder = context.workOrderId
        ? activeOrders.find((order) =>
          order.workOrderId === context.workOrderId &&
          (!context.routeId || order.routeId === context.routeId)
        )
        : undefined
      const initialActiveOrder = requestedActiveOrder || activeOrders[0]
      if (initialActiveOrder) {
        await handleSelectActiveOrder(initialActiveOrder)
      }
      Object.assign(draft.fieldValues, buildPqcFieldValues())
      return
    }
    const [loadedCatalog] = await Promise.all([
      catalogRequest,
      initializeProductionSelection()
    ])
    catalog.value = loadedCatalog
    Object.assign(draft.fieldValues, buildProductionFieldValues())
  } catch (error) {
    showFrontlineError(error)
  }
})

onUnmounted(() => {
  resolveProductionFormalSubmitConfirmation(false)
  closeProductionSubmitSuccessDialog()
  document.removeEventListener('fullscreenchange', syncPqcFullscreenState)
  window.removeEventListener('resize', scheduleProductionViewportScaleUpdate)
  if (productionViewportScaleFrame !== undefined) {
    window.cancelAnimationFrame(productionViewportScaleFrame)
    productionViewportScaleFrame = undefined
  }
  if (productionViewportResizeObserver) {
    productionViewportResizeObserver.disconnect()
    productionViewportResizeObserver = undefined
  }
})
</script>

<style scoped lang="scss">
.frontline-operator-panel {
  --frontline-bg: #eef3ef;
  --frontline-panel: #ffffff;
  --frontline-ink: #111a15;
  --frontline-muted: #5b665f;
  --frontline-line: #cbd6ce;
  --frontline-dark: #24322b;
  position: relative;
}

.frontline-operator-panel.is-production-mode {
  display: grid;
  place-items: center;
  width: 100%;
  min-height: calc(100vh - 96px);
  margin: 0;
  padding: 24px 0;
  overflow-x: hidden;
  overflow-y: auto;
  background: #dfe8e2;
  color: #111a15;
  font-family:
    "Microsoft YaHei UI",
    "PingFang SC",
    "Noto Sans CJK SC",
    sans-serif;
}

.frontline-operator-screen,
.frontline-operator-screen * {
  box-sizing: border-box;
}

.frontline-production-stage {
  position: relative;
  width: 1920px;
  height: 1080px;
  max-width: 100%;
  flex: 0 0 auto;
}

.frontline-operator-screen {
  --frontline-bg: #eef3ef;
  --frontline-panel: #ffffff;
  --frontline-ink: #111a15;
  --frontline-muted: #5b665f;
  --frontline-line: #cbd6ce;
  --frontline-dark: #24322b;
  display: grid;
  width: 1920px;
  height: 1080px;
  box-sizing: border-box;
  grid-template-rows: minmax(130px, auto) minmax(0, 1fr);
  gap: 20px;
  padding: 28px;
  overflow: hidden;
  position: relative;
  background: var(--frontline-bg);
  color: var(--frontline-ink);
  font-family:
    "Microsoft YaHei UI",
    "PingFang SC",
    "Noto Sans CJK SC",
    sans-serif;

  &.is-pqc {
    position: relative;
    width: auto;
    height: auto;
    grid-template-rows: minmax(118px, auto) minmax(0, 1fr);
    min-height: 820px;
  }
}

.frontline-production-stage .frontline-operator-screen {
  position: absolute;
  inset: 0;
  transform: scale(var(--frontline-production-scale, 1));
  transform-origin: top left;
}

.frontline-operator-screen button,
.frontline-operator-screen input {
  font: inherit;
}

.frontline-operator-screen:fullscreen {
  width: 100vw;
  height: 100vh;
  min-height: 100vh;
  box-sizing: border-box;
  border-radius: 0;
}

.frontline-operator-panel.is-pqc-fullscreen,
.frontline-operator-panel:fullscreen {
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 12px 16px;
  box-sizing: border-box;
  overflow: hidden;
  background:
    radial-gradient(circle at 12% 10%, rgba(255, 255, 255, 0.76), transparent 28%),
    linear-gradient(135deg, #eef3ef 0%, #e1ebe4 100%);
}

.frontline-operator-panel.is-production-fullscreen,
.frontline-operator-panel.is-production-mode:fullscreen {
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  display: grid;
  place-items: center;
  overflow: auto;
  background: #dfe8e2;
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-operator-screen.is-pqc,
.frontline-operator-panel:fullscreen .frontline-operator-screen.is-pqc {
  width: 100%;
  max-width: none;
  height: 100%;
  min-height: 0;
  margin: 0;
  grid-template-rows: minmax(118px, auto) minmax(0, 1fr);
  gap: 16px;
  padding: 16px;
  border-radius: 18px;
  box-shadow: 0 18px 46px rgba(36, 50, 43, 0.12);
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-operator-top.is-pqc,
.frontline-operator-panel:fullscreen .frontline-operator-top.is-pqc {
  grid-template-columns: minmax(480px, 1.45fr) minmax(360px, 1.2fr) minmax(140px, 0.45fr) 150px;
  gap: 12px;
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-operator-main.is-pqc,
.frontline-operator-panel:fullscreen .frontline-operator-main.is-pqc {
  grid-template-columns: minmax(0, 1.28fr) minmax(0, 0.92fr);
  grid-template-rows: minmax(0, 1fr) 112px;
  gap: 18px 22px;
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-operator-screen.is-pqc .frontline-top-card,
.frontline-operator-panel:fullscreen .frontline-operator-screen.is-pqc .frontline-top-card {
  padding: 14px 16px;

  span {
    font-size: 16px;
  }

  strong {
    margin-top: 6px;
    font-size: 22px;
  }
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-operator-screen.is-pqc .frontline-home-button,
.frontline-operator-panel:fullscreen .frontline-operator-screen.is-pqc .frontline-home-button {
  font-size: 56px;
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-pqc-fill-panel,
.frontline-operator-panel:fullscreen .frontline-pqc-fill-panel {
  padding: 26px 20px;
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-pqc-number-field,
.frontline-operator-panel:fullscreen .frontline-pqc-number-field {
  grid-template-columns: 128px 58px minmax(54px, 1fr) 58px 42px;
  gap: 8px;
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-pqc-submit-bar,
.frontline-operator-panel:fullscreen .frontline-pqc-submit-bar {
  grid-template-columns: 280px 360px minmax(0, 1fr);
  gap: 24px;
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-pqc-reset-button,
.frontline-operator-panel.is-pqc-fullscreen .frontline-pqc-submit-button,
.frontline-operator-panel:fullscreen .frontline-pqc-reset-button,
.frontline-operator-panel:fullscreen .frontline-pqc-submit-button {
  border-radius: 28px;
  font-size: 50px;
}

.frontline-operator-top {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1.35fr) minmax(0, 0.9fr) 240px;
  gap: 20px;

  &.is-pqc {
    grid-template-columns: minmax(480px, 1.45fr) minmax(360px, 1.2fr) minmax(140px, 0.45fr) 150px;
    gap: 12px;
  }
}

.frontline-top-card,
.frontline-home-button {
  min-width: 0;
  border: 3px solid var(--frontline-line);
  border-radius: 22px;
  font: inherit;
}

.frontline-top-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 22px 26px;
  background: var(--frontline-panel);
  text-align: left;
  cursor: pointer;

  span {
    color: var(--frontline-muted);
    font-size: 28px;
    font-weight: 700;
    line-height: 1;
  }

  .top-label {
    color: var(--frontline-muted);
    font-size: 28px;
    font-weight: 700;
  }

  strong,
  .top-value {
    min-width: 0;
    margin-top: 12px;
    overflow: hidden;
    font-size: 42px;
    font-weight: 900;
    line-height: 1.1;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.frontline-production-selection-card[data-frontline-production-active-order-card] {
  padding: 14px 22px;
}

.frontline-production-order-summary {
  display: grid;
  width: 100%;
  min-width: 0;
  gap: 5px;
  align-content: center;
  text-overflow: clip;
  white-space: normal;
  overflow-wrap: anywhere;
}

.frontline-production-order-summary .frontline-production-order-summary__value {
  display: block;
  min-width: 0;
  overflow: visible;
  color: var(--frontline-ink);
  font-size: 28px;
  font-weight: 900;
  line-height: 1.08;
  text-overflow: clip;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.frontline-top-card.is-login-employee {
  cursor: default;
  opacity: 1;
}

.frontline-production-process-nav-card {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr) 140px;
  align-items: stretch;
  gap: 12px;
  padding: 14px 16px;
  cursor: default;
}

.frontline-pqc-process-nav-card {
  grid-template-columns: 72px minmax(0, 1fr) 72px;
  gap: 10px;
  padding: 10px 12px;
}

.frontline-production-process-current {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: center;
  cursor: pointer;
}

.frontline-production-process-nav-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  min-width: 0;
  padding: 0 10px;
  border: 3px solid var(--frontline-line);
  border-radius: 22px;
  background: var(--frontline-panel);
  color: var(--frontline-ink);
  font: inherit;
  font-size: 112px;
  font-weight: 900;
  text-align: center;
  cursor: pointer;
}

.frontline-production-process-nav-icon {
  position: relative;
  display: block;
  width: 78px;
  height: 46px;
  color: currentColor;
}

.frontline-production-process-nav-icon::before {
  position: absolute;
  top: 50%;
  right: 4px;
  left: 10px;
  height: 14px;
  border-radius: 999px;
  background: currentColor;
  content: '';
  transform: translateY(-50%);
}

.frontline-production-process-nav-icon::after {
  position: absolute;
  top: 50%;
  left: 4px;
  width: 34px;
  height: 34px;
  border-left: 14px solid currentColor;
  border-bottom: 14px solid currentColor;
  content: '';
  transform: translateY(-50%) rotate(45deg);
  transform-origin: center;
}

.frontline-production-process-nav-icon.is-next {
  transform: scaleX(-1);
}

.frontline-pqc-process-nav-card .frontline-production-process-nav-button {
  padding: 0 4px;
  border-radius: 16px;
}

.frontline-pqc-process-nav-card .frontline-production-process-nav-icon {
  width: 48px;
  height: 30px;
}

.frontline-pqc-process-nav-card .frontline-production-process-nav-icon::before {
  right: 4px;
  left: 8px;
  height: 9px;
}

.frontline-pqc-process-nav-card .frontline-production-process-nav-icon::after {
  left: 4px;
  width: 22px;
  height: 22px;
  border-left: 9px solid currentColor;
  border-bottom: 9px solid currentColor;
}

.frontline-production-process-current:disabled,
.frontline-production-process-nav-button:disabled {
  cursor: not-allowed;
  opacity: 0.46;
}

.frontline-operator-top.is-pqc {
  .frontline-top-card {
    padding: 14px 16px;

    span {
      font-size: 18px;
      font-weight: 800;
    }

    strong {
      margin-top: 6px;
      overflow: visible;
      font-size: 26px;
      text-overflow: clip;
      white-space: normal;
      overflow-wrap: anywhere;
    }
  }

  .frontline-home-button {
    font-size: 30px;
  }
}

.frontline-top-card--order-summary {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(0, 1fr) minmax(112px, auto);
  gap: 14px;
  align-items: stretch;
  padding: 14px 16px;
}

.frontline-order-summary__field {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  padding-left: 14px;
  border-left: 2px solid var(--frontline-line);

  &.is-order {
    padding-left: 0;
    border-left: 0;
  }
}

.frontline-order-summary__value {
  margin-top: 6px !important;
  overflow: visible !important;
  font-size: 22px !important;
  line-height: 1.15 !important;
  text-overflow: clip !important;
  white-space: normal !important;
  overflow-wrap: anywhere;

  &.is-order {
    font-size: 24px !important;
  }
}

.frontline-home-button {
  background: var(--frontline-dark);
  color: #ffffff;
  font-size: 42px;
  font-weight: 900;
  cursor: pointer;
}

.frontline-operator-main {
  display: grid;
  grid-template-columns: 1050px 1fr;
  gap: 28px;
  min-height: 0;

  &.is-pqc {
    grid-template-columns: minmax(620px, 1.28fr) minmax(500px, 0.92fr);
    grid-template-rows: minmax(0, 1fr) 104px;
    gap: 22px 24px;
  }
}

.frontline-production-main {
  grid-template-rows: minmax(0, 1fr) 126px;
  column-gap: 28px;
  row-gap: 20px;
}

.frontline-work-panel {
  display: grid;
  align-content: start;
  gap: 22px;
  min-width: 0;
  padding: 26px;
  border: 3px solid var(--frontline-line);
  border-radius: 28px;
  background: var(--frontline-panel);

  h3,
  .panel-title {
    margin: 0;
    font-size: 48px;
    font-weight: 900;
    line-height: 1;
  }
}

.frontline-production-quantity-panel {
  grid-column: 1;
  grid-row: 1;
  grid-template-rows: auto auto minmax(0, 1fr) auto auto;
  gap: 16px;
}

.frontline-production-material-tabs {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  min-width: 0;
}

.frontline-production-material-tab {
  display: grid;
  gap: 4px;
  min-width: 0;
  min-height: 88px;
  padding: 10px 14px;
  border: 4px solid var(--frontline-line);
  border-radius: 14px;
  background: #eef1ef;
  color: var(--frontline-ink);
  cursor: pointer;

  strong,
  small {
    min-width: 0;
    overflow-wrap: anywhere;
    letter-spacing: 0;
  }

  strong {
    font-size: 28px;
    line-height: 1.1;
  }

  small {
    font-size: 18px;
    font-weight: 800;
    line-height: 1.1;
  }

  &.is-selected {
    border-color: #111915;
    box-shadow: inset 0 0 0 2px #ffffff;
  }

  &.is-complete {
    border-color: #116b4e;
    background: #17835f;
    color: #ffffff;
  }
}

.frontline-production-material-batches {
  overflow-wrap: anywhere;
  color: inherit;
  line-height: 1.25;
}

.frontline-inline-error-slot {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) 36px;
  gap: 12px;
  align-items: center;
  min-height: 54px;
  min-width: 0;
  padding: 5px 12px;
  overflow: hidden;
  border: 3px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: #b42318;
  opacity: 0;
  pointer-events: none;

  span {
    min-width: 0;
    font-size: 24px;
    font-weight: 900;
    line-height: 1.25;
    overflow-wrap: anywhere;
  }

  button {
    display: grid;
    width: 36px;
    height: 36px;
    padding: 0;
    place-items: center;
    border: 0;
    border-radius: 6px;
    background: transparent;
    color: currentColor;
    cursor: pointer;
  }
}

.frontline-inline-error-slot.is-visible {
  border: 3px solid #e85d5d;
  background: #fff2f2;
  opacity: 1;
  pointer-events: auto;
}

.frontline-production-number-field {
  display: grid;
  grid-template-columns: 250px 82px minmax(190px, 1fr) 82px 50px;
  gap: 16px;
  align-items: center;
  min-width: 0;

  &.is-total {
    grid-template-columns: 250px minmax(0, 1fr) 50px;
  }

  label {
    font-size: 36px;
    font-weight: 900;
    line-height: 1.15;
  }

  button,
  input {
    width: 100%;
    height: 96px;
    min-width: 0;
    border: 3px solid var(--frontline-line);
    border-radius: 18px;
    background: #f8faf8;
    color: var(--frontline-ink);
    text-align: center;
    font-weight: 900;
  }

  button {
    padding: 0;
    font-size: 50px;
    cursor: pointer;
  }

  input {
    font-size: 52px;

    &[readonly] {
      background: #eef3ef;
    }
  }

  span {
    font-size: 34px;
    font-weight: 800;
  }
}

.frontline-production-defect-section {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
}

.frontline-production-defect-title {
  font-size: 32px;
  font-weight: 900;
  line-height: 1;
}

.frontline-production-defect-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(4, minmax(0, 1fr));
  gap: 10px;
  min-height: 0;
}

.frontline-production-defect-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 58px 76px 58px 34px;
  gap: 8px;
  align-items: center;
  min-width: 0;
  min-height: 0;
  padding: 0 10px;
  border: 3px solid var(--frontline-line);
  border-radius: 16px;
  background: #f8faf8;
  color: var(--frontline-ink);
  font-weight: 900;
  text-align: left;

  &.active {
    border-color: #15815f;
    background: #dff2ea;
  }
}

.frontline-production-defect-name {
  min-width: 0;
  font-size: 24px;
  line-height: 1.15;
}

.frontline-production-defect-step,
.frontline-production-defect-qty {
  width: 100%;
  height: 54px;
  min-width: 0;
  border: 3px solid var(--frontline-line);
  border-radius: 12px;
  background: #ffffff;
  color: var(--frontline-ink);
  text-align: center;
  font-weight: 900;
}

.frontline-production-defect-step {
  padding: 0;
  font-size: 34px;
  cursor: pointer;
}

.frontline-production-defect-qty {
  font-size: 30px;
}

.frontline-production-defect-unit {
  font-size: 24px;
  font-weight: 900;
  white-space: nowrap;
}

.frontline-production-device-panel {
  grid-column: 2;
  grid-row: 1 / 3;
  grid-template-rows: 118px minmax(0, 1fr) auto;
  gap: 12px;
  overflow: hidden;
}

.frontline-production-device-tabs {
  display: grid;
  grid-template-columns: repeat(var(--frontline-device-tab-count, 1), minmax(0, 1fr));
  gap: 12px;
  min-width: 0;
}

.frontline-production-device-card {
  display: grid;
  grid-template-rows: minmax(0, 1fr) 36px;
  min-width: 0;
  height: 110px;
  border: 3px solid var(--frontline-line);
  border-radius: 20px;
  background: #f8faf8;
  overflow: hidden;

  &.active {
    border-color: var(--frontline-dark);
  }
}

.frontline-production-device-card .device-tab {
  min-width: 0;
  min-height: 0;
  padding: 0;
  border: 0;
  background: var(--frontline-dark);
  color: #ffffff;
  font-size: 30px;
  font-weight: 900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.frontline-production-device-metering-validity {
  display: grid;
  grid-template-columns: 18px auto;
  justify-content: center;
  align-items: center;
  gap: 6px;
  min-width: 0;
  min-height: 36px;
  padding: 0 6px;
  background: #ffffff;
  color: var(--frontline-ink);
  cursor: pointer;

  input {
    position: absolute;
    opacity: 0;
    pointer-events: none;
  }

  span {
    display: grid;
    place-items: center;
    width: 18px;
    height: 18px;
    border-radius: 5px;
    background: #15815f;
    color: #ffffff;
    font-size: 14px;
    font-weight: 900;
    line-height: 1;
  }

  em {
    min-width: 0;
    font-size: 14px;
    font-style: normal;
    font-weight: 900;
    letter-spacing: 0;
    white-space: nowrap;
  }

  input:not(:checked) + span {
    border: 2px solid #8a9490;
    background: #ffffff;
    color: transparent;
  }

  input:disabled + span,
  input:disabled + span + em {
    opacity: 0.48;
  }
}

.frontline-production-device-card.active .frontline-production-device-metering-validity {
  color: var(--frontline-ink);
}

.frontline-production-device-current {
  display: grid;
  align-content: start;
  gap: 10px;
  min-width: 0;
  min-height: 0;
  padding: 14px;
  border: 3px solid var(--frontline-line);
  border-radius: 24px;
  background: #fbfdfb;
  overflow: auto;
}

.frontline-production-self-check {
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 18px 20px;
  border: 3px solid var(--frontline-line);
  border-radius: 18px;
  background: #f8faf8;
  color: var(--frontline-ink);
  line-height: 1.45;
  overflow-wrap: anywhere;

  h3,
  p {
    margin: 0;
  }

  h3 {
    padding-bottom: 10px;
    border-bottom: 2px solid var(--frontline-line);
    font-size: 32px;
    font-weight: 900;
  }

  section {
    display: grid;
    gap: 6px;
  }

  strong {
    font-size: 24px;
    font-weight: 900;
  }

  p {
    font-size: 20px;
    font-weight: 700;
  }
}

.frontline-production-device-boolean {
  display: grid;
  grid-column: 2 / -1;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  min-height: 58px;
  padding: 0 16px;
  border: 3px solid var(--frontline-line);
  border-radius: 14px;
  background: #ffffff;
  color: var(--frontline-ink);
  font-size: 24px;
  font-weight: 900;
  cursor: pointer;

  input {
    position: absolute;
    width: 1px;
    height: 1px;
    opacity: 0;
    pointer-events: none;
  }

  span {
    display: grid;
    place-items: center;
    width: 32px;
    height: 32px;
    border: 3px solid #15815f;
    border-radius: 8px;
    background: #15815f;
    color: #ffffff;
    font-size: 22px;
    line-height: 1;
  }

  em {
    min-width: 0;
    font-style: normal;
    white-space: nowrap;
  }

  input:not(:checked) + span {
    border-color: #8a9490;
    background: #ffffff;
    color: transparent;
  }

  input:disabled + span,
  input:disabled + span + em {
    opacity: 0.48;
  }
}

.frontline-production-device-empty {
  display: grid;
  grid-row: 1 / 3;
  place-items: center;
  min-width: 0;
  min-height: 0;
  padding: 26px;
  border: 3px solid var(--frontline-line);
  border-radius: 24px;
  background: #fbfdfb;
  color: var(--frontline-ink);
  font-size: 42px;
  font-weight: 900;
}

.frontline-production-clearance-confirmations {
  display: grid;
  grid-row: 3;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(0, auto));
  gap: 10px;
  min-width: 0;
  min-height: 0;
  padding: 10px 12px;
  border: 3px solid #0ea5e9;
  border-radius: 16px;
  background: #f7fbff;
}

.frontline-production-clearance-confirmation {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.frontline-production-clearance-label {
  display: grid;
  grid-template-columns: auto auto;
  justify-content: center;
  align-items: center;
  gap: 8px;
  min-width: 0;
  height: 54px;
  padding: 0 10px;
  border: 2px solid var(--frontline-line);
  border-radius: 14px;
  background: #ffffff;
  color: var(--frontline-ink);
  font-size: 24px;
  font-weight: 900;
  cursor: pointer;

  span,
  small {
    min-width: 0;
    white-space: nowrap;
  }

  small {
    color: #256d85;
    font-size: 17px;
    font-weight: 900;
  }
}

.frontline-production-clearance-checkbox {
  display: grid;
  grid-template-columns: auto auto;
  justify-content: center;
  align-items: center;
  gap: 8px;
  min-width: 82px;
  min-height: 54px;
  padding: 0 10px;
  border: 2px solid var(--frontline-line);
  border-radius: 14px;
  background: #ffffff;
  color: var(--frontline-ink);
  font-size: 20px;
  font-weight: 900;
  cursor: pointer;

  input {
    position: absolute;
    width: 1px;
    height: 1px;
    opacity: 0;
    pointer-events: none;
  }

  span {
    display: grid;
    place-items: center;
    width: 30px;
    height: 30px;
    border: 3px solid #15815f;
    border-radius: 8px;
    background: #15815f;
    color: #ffffff;
    font-size: 22px;
    line-height: 1;
  }

  em {
    font-style: normal;
    white-space: nowrap;
  }

  input:not(:checked) + span {
    background: #ffffff;
    color: transparent;
    border-color: #8a9490;
  }

  input:disabled + span,
  input:disabled + span + em {
    opacity: 0.48;
  }
}

.frontline-production-clearance-confirmation-modal {
  position: absolute;
  inset: 0;
  z-index: 126;
  display: grid;
  place-items: center;
  padding: 48px;
  background: rgba(17, 26, 21, 0.56);
  box-sizing: border-box;
}

.frontline-production-clearance-confirmation-dialog {
  display: grid;
  gap: 22px;
  width: min(100%, 920px);
  max-height: min(76vh, 680px);
  padding: 38px;
  overflow: auto;
  overscroll-behavior: contain;
  border: 4px solid var(--frontline-line);
  border-radius: 28px;
  background: #fffdf4;
  color: var(--frontline-ink);
  box-shadow: 0 24px 80px rgba(17, 26, 21, 0.32);
  font-size: 28px;
  line-height: 1.45;
}

.frontline-production-clearance-confirmation-header {
  display: grid;
  gap: 8px;

  span {
    color: #256d85;
    font-size: 22px;
    font-weight: 900;
  }

  h3 {
    margin: 0;
    font-size: 44px;
    font-weight: 900;
  }
}

.frontline-production-clearance-confirmation-dialog p {
  margin: 0;
  overflow-wrap: anywhere;
}

.frontline-production-clearance-confirmation-dialog button {
  justify-self: end;
  min-width: 180px;
  min-height: 68px;
  padding: 0 28px;
  border: 0;
  border-radius: 18px;
  background: #15815f;
  color: #ffffff;
  font-size: 28px;
  font-weight: 900;
  cursor: pointer;
}

.frontline-production-device-param {
  display: grid;
  grid-template-columns: 224px 70px minmax(0, 1fr) 70px 58px;
  gap: 10px;
  align-items: center;
  min-width: 0;

  .device-param-label {
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-width: 0;
    gap: 4px;
    font-size: 28px;
    font-weight: 900;
    line-height: 1.1;
  }

  .device-param-name {
    display: block;
    min-width: 0;
    white-space: nowrap;
  }

  .device-param-range {
    display: block;
    min-width: 0;
    color: #4b5f55;
    font-size: 18px;
    font-weight: 800;
    line-height: 1.15;
    white-space: nowrap;
  }

  button,
  input,
  select {
    width: 100%;
    height: 72px;
    min-width: 0;
    border: 3px solid var(--frontline-line);
    border-radius: 14px;
    background: #f8faf8;
    color: var(--frontline-ink);
    text-align: center;
    font-weight: 900;
  }

  button {
    padding: 0;
    font-size: 38px;
    cursor: pointer;
  }

  input {
    font-size: 40px;

    &.is-parameter-out-of-range {
      border-color: #dc2626;
      background: #fff1f2;
      color: #b91c1c;
    }
  }

  select.device-select {
    grid-column: 2 / 5;
    padding: 0 42px;
    appearance: auto;
    font-size: 32px;
    cursor: pointer;
    text-align-last: center;
  }

  span {
    font-size: 26px;
    font-weight: 900;
  }

  .frontline-production-device-standard-text {
    grid-column: 2 / -1;
    min-width: 0;
    padding: 14px 18px;
    border: 3px solid var(--frontline-line);
    border-radius: 14px;
    background: #f8faf8;
    font-size: 28px;
    line-height: 1.25;
    overflow-wrap: anywhere;
  }
}

.frontline-production-submit-bar {
  display: grid;
  grid-column: 1;
  grid-row: 2;
  grid-template-columns: 300px 1fr;
  gap: 24px;
  position: relative;
  z-index: 2;
}

.frontline-production-reset-button,
.frontline-production-submit-button {
  border: 0;
  border-radius: 28px;
  font-size: 54px;
  font-weight: 900;
  cursor: pointer;
}

.frontline-production-reset-button {
  border: 3px solid var(--frontline-line);
  background: #ffffff;
  color: var(--frontline-ink);

  &:disabled {
    cursor: not-allowed;
    opacity: 0.48;
  }
}

.frontline-production-submit-button {
  border: 0;
  background: #15815f;
  color: #ffffff;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.48;
  }

}

.frontline-production-submit-confirmation-modal {
  position: absolute;
  inset: 0;
  z-index: 120;
  display: grid;
  place-items: center;
  padding: 48px;
  background: rgba(17, 26, 21, 0.56);
  box-sizing: border-box;
}

.frontline-production-submit-confirmation-dialog {
  display: grid;
  gap: 24px;
  width: min(100%, 860px);
  max-width: 860px;
  padding: 42px;
  border: 4px solid var(--frontline-line);
  border-radius: 28px;
  background: #fffdf4;
  color: var(--frontline-ink);
  box-shadow: 0 24px 80px rgba(17, 26, 21, 0.32);
  font-size: 28px;
  line-height: 1.45;

  h3 {
    margin: 0;
    font-size: 42px;
    font-weight: 900;
  }

  p {
    margin: 0;
  }
}

.frontline-production-submit-confirmation-signature {
  display: grid;
  gap: 10px;
  font-size: 26px;
  font-weight: 800;

  input {
    min-height: 72px;
    padding: 0 22px;
    border: 3px solid var(--frontline-line);
    border-radius: 18px;
    background: #ffffff;
    color: var(--frontline-ink);
    font-size: 28px;
    font-weight: 700;
    outline: none;
    box-sizing: border-box;
  }

  input:focus-visible {
    border-color: #15815f;
    box-shadow: 0 0 0 4px rgba(21, 129, 95, 0.18);
  }
}

.frontline-production-submit-confirmation-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;

  button {
    min-height: 88px;
    border: 0;
    border-radius: 22px;
    font-size: 32px;
    font-weight: 900;
    cursor: pointer;
  }

  button:first-child {
    border: 3px solid var(--frontline-line);
    background: #ffffff;
    color: var(--frontline-ink);
  }

  button:last-child {
    background: #15815f;
    color: #ffffff;
  }

  button:disabled {
    cursor: not-allowed;
    opacity: 0.56;
  }
}

.frontline-production-submit-success-modal {
  position: absolute;
  inset: 0;
  z-index: 130;
  display: grid;
  place-items: center;
  padding: 48px;
  background: rgba(17, 26, 21, 0.62);
  box-sizing: border-box;
}

.frontline-production-submit-success-dialog {
  display: grid;
  justify-items: center;
  gap: 28px;
  width: min(100%, 720px);
  padding: 48px;
  border: 4px solid var(--frontline-line);
  border-radius: 28px;
  background: #ffffff;
  color: var(--frontline-ink);
  box-shadow: 0 24px 80px rgba(17, 26, 21, 0.34);
  text-align: center;
  box-sizing: border-box;

  button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 14px;
    width: 100%;
    min-height: 88px;
    border: 0;
    border-radius: 22px;
    background: #15815f;
    color: #ffffff;
    font-size: 32px;
    font-weight: 900;
    cursor: pointer;
  }

  button:focus-visible {
    outline: 5px solid rgba(21, 129, 95, 0.28);
    outline-offset: 4px;
  }
}

.frontline-production-submit-success-icon {
  width: 96px;
  height: 96px;
  color: #15815f;
}

.frontline-production-submit-success-copy {
  display: grid;
  gap: 12px;

  span {
    color: #15815f;
    font-size: 22px;
    font-weight: 900;
  }

  h3 {
    margin: 0;
    font-size: 48px;
    font-weight: 900;
  }

  p {
    margin: 0;
    color: var(--frontline-muted);
    font-size: 30px;
    font-weight: 800;
  }
}

.frontline-production-stage .frontline-production-fullscreen-toggle {
  font-size: var(--frontline-production-top-action-font-size, 42px);
}

.frontline-production-stage .frontline-production-reset-button,
.frontline-production-stage .frontline-production-submit-button {
  font-size: var(--frontline-production-footer-action-font-size, 54px);
}

.frontline-pqc-inspection-list {
  display: grid;
  grid-template-rows: auto minmax(88px, auto) minmax(0, 1fr);
  gap: 16px;
  min-height: 100%;
}

.frontline-pqc-content-panel {
  grid-column: 1;
  grid-row: 1;
  align-content: stretch;
  gap: 0;
}

.frontline-pqc-content-item {
  display: grid;
  align-content: start;
  gap: 14px;
  min-width: 0;
  padding-bottom: 0;
  overflow: visible;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--frontline-ink);
}

.pqc-utility-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  min-width: 0;
}

.pqc-select-card,
.pqc-fact-card {
  position: relative;
  display: grid;
  min-width: 0;
  min-height: 66px;
  border: 3px solid var(--frontline-line);
  border-radius: 18px;
  background: #ffffff;
  color: var(--frontline-ink);
  font: inherit;
  text-align: left;
}

.pqc-select-card {
  grid-template-columns: minmax(0, 1fr) 34px;
  gap: 8px;
  align-items: center;
  padding: 7px 14px 7px 16px;
  overflow: hidden;

  strong,
  span span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: var(--frontline-muted);
    font-size: 17px;
    font-weight: 900;
    line-height: 1;
  }

  span span {
    display: block;
    margin-top: 6px;
    font-size: 28px;
    font-weight: 900;
    line-height: 1.05;
  }

  em {
    display: grid;
    place-items: center;
    width: 34px;
    height: 34px;
    border-radius: 999px;
    background: var(--frontline-dark);
    color: #ffffff;
    font-size: 20px;
    font-style: normal;
    font-weight: 900;
    line-height: 1;
  }

  &.is-selected {
    border-color: #8cb9a1;
    background: #fbfffc;
  }

  &.is-empty span span {
    color: #7f8f86;
  }
}

.pqc-select-native {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
  opacity: 0;
}

.pqc-fact-card {
  align-content: center;
  gap: 3px;
  padding: 7px 16px;
  cursor: pointer;

  strong,
  span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    font-size: 28px;
    font-weight: 900;
    line-height: 1;
  }

  span {
    color: var(--frontline-muted);
    font-size: 17px;
    font-weight: 900;
    line-height: 1;
  }

  &.is-primary {
    border-color: #8cb9a1;
    background: #dff2ea;
    color: #15815f;
  }
}

.frontline-pqc-fact-dialog {
  position: absolute;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 34px;
  background:
    radial-gradient(circle at 20% 18%, rgba(255, 255, 255, 0.88), transparent 30%),
    rgba(20, 31, 25, 0.42);
  backdrop-filter: blur(14px);
}

.frontline-pqc-fact-dialog__panel {
  display: flex;
  flex-direction: column;
  width: min(920px, 100%);
  max-height: calc(100vh - 88px);
  overflow: hidden;
  border: 3px solid rgba(139, 181, 159, 0.65);
  border-radius: 32px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(242, 249, 245, 0.98)),
    #ffffff;
  box-shadow: 0 34px 90px rgba(20, 31, 25, 0.28);
}

.frontline-pqc-fact-dialog__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 22px;
  padding: 28px 30px 18px;
  border-bottom: 1px solid rgba(139, 181, 159, 0.42);

  h3 {
    margin: 8px 0 0;
    color: #111a15;
    font-size: 42px;
    font-weight: 950;
    line-height: 1.05;
  }
}

.frontline-pqc-fact-dialog__eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 4px 12px;
  border-radius: 999px;
  background: #dff2ea;
  color: #15815f;
  font-size: 18px;
  font-weight: 950;
  letter-spacing: 0.08em;
}

.frontline-pqc-fact-dialog__close {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 56px;
  height: 56px;
  border: 0;
  border-radius: 999px;
  background: #24322b;
  color: #ffffff;
  font: inherit;
  font-size: 34px;
  font-weight: 900;
  line-height: 1;
  cursor: pointer;

  &:focus-visible {
    outline: 5px solid #86c8ad;
    outline-offset: 4px;
  }
}

.frontline-pqc-fact-dialog__body {
  display: grid;
  grid-template-columns: minmax(0, 1.36fr) minmax(270px, 0.64fr);
  gap: 20px;
  min-height: 0;
  padding: 24px 30px 30px;
  overflow: auto;
}

.frontline-pqc-fact-dialog__body.is-standard {
  grid-template-columns: minmax(0, 1fr);
}

.frontline-pqc-fact-dialog__detail {
  min-width: 0;
  padding: 24px;
  border: 2px solid rgba(139, 181, 159, 0.5);
  border-radius: 24px;
  background: #ffffff;

  span {
    color: #15815f;
    font-size: 18px;
    font-weight: 950;
  }

  p {
    margin: 14px 0 0;
    color: #18231d;
    font-size: 28px;
    font-weight: 850;
    line-height: 1.45;
    overflow-wrap: anywhere;
    white-space: pre-wrap;
  }
}

.frontline-pqc-fact-dialog__footer {
  display: flex;
  justify-content: flex-end;
  padding: 0 30px 28px;

  button {
    min-width: 148px;
    min-height: 52px;
    border: 0;
    border-radius: 999px;
    background: #15815f;
    color: #ffffff;
    font: inherit;
    font-size: 24px;
    font-weight: 950;
    cursor: pointer;

    &:focus-visible {
      outline: 5px solid #86c8ad;
      outline-offset: 4px;
    }
  }
}

@media (max-width: 900px) {
  .frontline-pqc-fact-dialog {
    padding: 16px;
  }

  .frontline-pqc-fact-dialog__panel {
    max-height: calc(100vh - 32px);
    border-radius: 24px;
  }

  .frontline-pqc-fact-dialog__header {
    padding: 22px 22px 14px;

    h3 {
      font-size: 32px;
    }
  }

  .frontline-pqc-fact-dialog__body {
    grid-template-columns: 1fr;
    padding: 18px 22px 24px;
  }

  .frontline-pqc-fact-dialog__detail p {
    font-size: 22px;
  }

  .frontline-pqc-fact-dialog__footer {
    padding: 0 22px 22px;
  }
}

.pqc-required-dot {
  position: absolute;
  top: 8px;
  right: 10px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #15815f;
}

.frontline-pqc-empty-state {
  display: grid;
  place-items: center;
  min-height: 260px;
  color: var(--frontline-muted);
  font-size: 32px;
  font-weight: 900;
}

.frontline-pqc-choice-actions {
  display: grid;
  grid-template-columns: 1fr 1fr 1.5fr;
  gap: 8px;
  min-height: 78px;

  &.is-number {
    grid-template-columns: minmax(0, 1fr);
  }

  > button {
    min-width: 0;
    padding: 8px 12px;
    border: 3px solid var(--frontline-line);
    border-radius: 18px;
    background: #f8faf8;
    color: var(--frontline-ink);
    font: inherit;
    font-size: 29px;
    font-weight: 900;
    white-space: nowrap;
    cursor: pointer;

    &:focus-visible {
      outline: 5px solid #86c8ad;
      outline-offset: -8px;
    }

    &.pass.active {
      background: #dff2ea;
      color: #15815f;
    }

    &.fail.active {
      background: #f8dfdc;
      color: #b9382f;
    }
  }

  .manual {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 38px;
    grid-template-rows: auto auto;
    gap: 4px 10px;
    align-items: center;
    padding: 10px 16px;
    text-align: left;

    &.active {
      background: #e7f0eb;
    }

    span {
      font-size: 30px;
      line-height: 1;
    }

    em {
      color: var(--frontline-muted);
      font-size: 25px;
      font-style: normal;
      white-space: nowrap;
    }

    strong {
      grid-column: 2;
      grid-row: 1 / span 2;
      font-size: 40px;
      line-height: 1;
    }
  }
}

.pqc-item-tabs {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(104px, 1fr));
  gap: 8px;
  align-items: start;
  min-height: 104px;
  padding: 0 10px 8px;
  border-top: 3px solid #8cb9a1;
  background: transparent;

  &::before {
    position: absolute;
    inset: 0 0 auto;
    height: 24px;
    border-radius: 0 0 20px 20px;
    background: linear-gradient(180deg, rgba(223, 242, 234, 0.9), rgba(223, 242, 234, 0));
    content: "";
    pointer-events: none;
  }
}

.pqc-item-tab {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 0;
  align-items: center;
  min-width: 0;
  min-height: 68px;
  margin-top: -3px;
  padding: 9px 10px 8px;
  border: 3px solid var(--frontline-line);
  border-top: 0;
  border-radius: 0 0 16px 16px;
  background: #fbfdfb;
  color: var(--frontline-ink);
  font: inherit;
  text-align: center;
  box-shadow: inset 0 7px 0 rgba(203, 214, 206, 0.38);
  cursor: pointer;

  &::before {
    position: absolute;
    top: 0;
    right: 11px;
    left: 11px;
    height: 5px;
    border-radius: 0 0 999px 999px;
    background: transparent;
    content: "";
  }

  strong {
    min-width: 0;
    overflow: visible;
    font-size: 20px;
    font-weight: 900;
    line-height: 1.05;
    white-space: normal;
    word-break: break-word;
  }

  &.active {
    border-color: #d9a441;
    background: #fff4bf;
    color: #111a15;
    box-shadow: 0 8px 18px rgba(98, 76, 24, 0.12);
    transform: translateY(3px);

    &::before {
      display: none;
      background: transparent;
    }
  }

  &:focus-visible {
    outline: 5px solid #86c8ad;
    outline-offset: 2px;
  }
}

.frontline-pqc-fill-panel {
  grid-column: 2;
  grid-row: 1 / 3;
  grid-template-rows: auto auto minmax(min-content, 1fr);
  align-content: stretch;
  gap: 16px;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 22px;
}

.frontline-pqc-type-tabs,
.frontline-pqc-round-tabs {
  display: grid;
  gap: 14px;
  min-width: 0;

  button {
    min-width: 0;
    border: 3px solid var(--frontline-line);
    border-radius: 20px;
    background: #f8faf8;
    color: var(--frontline-ink);
    font-weight: 900;
    cursor: pointer;

    &.active {
      border-color: var(--frontline-dark);
      background: var(--frontline-dark);
      color: #ffffff;
    }
  }
}

.frontline-pqc-type-tabs {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;

  button {
    font-size: 32px;
  }
}

.frontline-pqc-round-tabs {
  grid-template-columns: repeat(auto-fit, minmax(136px, 1fr));
  align-items: stretch;

  button {
    min-height: 64px;
    padding: 8px 12px;
    font-size: 28px;
    line-height: 1.15;
    white-space: normal;
    overflow-wrap: anywhere;
  }
}

.frontline-pqc-form-area {
  display: grid;
  align-content: start;
  align-self: stretch;
  gap: 12px;
  min-width: 0;
  min-height: 0;
  padding: 14px 12px;
  border: 3px solid var(--frontline-line);
  border-radius: 24px;
  background: #fbfdfb;
}

.frontline-pqc-production-source {
  display: grid;
  grid-template-columns: 116px minmax(0, 1fr);
  gap: 10px;
  align-items: center;

  label {
    font-size: 24px;
    font-weight: 900;
  }

  select {
    width: 100%;
    min-width: 0;
    height: 54px;
    border: 3px solid var(--frontline-line);
    border-radius: 14px;
    padding: 0 12px;
    background: #ffffff;
    color: var(--frontline-ink);
    font-size: 20px;
    font-weight: 800;
  }
}

.frontline-pqc-number-field {
  display: grid;
  grid-template-columns: 116px 52px minmax(42px, 1fr) 52px 36px;
  gap: 6px;
  align-items: center;
  min-width: 0;

  label {
    font-size: 25px;
    font-weight: 900;
  }

  button,
  input {
    width: 100%;
    height: 58px;
    min-width: 0;
    border: 3px solid var(--frontline-line);
    border-radius: 16px;
    background: #f8faf8;
    color: var(--frontline-ink);
    text-align: center;
    font-weight: 900;
  }

  button {
    font-size: 32px;
    cursor: pointer;
  }

  input {
    font-size: 32px;
  }

  span {
    font-size: 24px;
    font-weight: 900;
  }
}

.frontline-pqc-submit-bar {
  display: grid;
  grid-column: 1;
  grid-row: 2;
  grid-template-columns: 280px 360px minmax(0, 1fr);
  gap: 24px;
  min-height: 0;
}

.frontline-pqc-signature-modal {
  position: absolute;
  inset: 0;
  z-index: 70;
  display: grid;
  place-items: center;
  background: rgba(17, 26, 21, 0.58);
}

.frontline-pqc-signature-dialog {
  display: grid;
  gap: 16px;
  width: min(520px, calc(100% - 40px));
  padding: 28px;
  border-radius: 8px;
  background: #ffffff;

  h3,
  p {
    margin: 0;
  }

  h3 {
    font-size: 30px;
  }

  label,
  p {
    font-size: 18px;
  }

  input {
    height: 58px;
    border: 2px solid var(--frontline-line);
    border-radius: 6px;
    padding: 0 14px;
    font-size: 24px;
  }

  div {
    display: grid;
    grid-template-columns: 1fr 1.5fr;
    gap: 12px;
  }

  button {
    min-height: 54px;
    border: 0;
    border-radius: 6px;
    background: #e8eee9;
    color: var(--frontline-ink);
    font-size: 18px;
    font-weight: 900;
  }

  button:last-child {
    background: #15815f;
    color: #ffffff;
  }
}

.frontline-pqc-reset-button,
.frontline-pqc-submit-button {
  border: 0;
  border-radius: 28px;
  font-size: 50px;
  font-weight: 900;
  cursor: pointer;
}

.frontline-pqc-reset-button {
  border: 3px solid var(--frontline-line);
  background: #ffffff;
  color: var(--frontline-ink);
}

.frontline-pqc-submit-button {
  background: #15815f;
  color: #ffffff;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.48;
  }
}

.frontline-pqc-piece-modal {
  position: absolute;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  background: rgba(17, 26, 21, 0.5);
}

.frontline-pqc-piece-dialog {
  display: grid;
  grid-template-rows: 86px minmax(0, 1fr) 96px;
  gap: 14px;
  width: min(1580px, calc(100% - 48px));
  height: min(930px, calc(100% - 48px));
  min-height: 0;
  padding: 24px;
  overflow: hidden;
  border: 3px solid var(--frontline-line);
  border-radius: 28px;
  background: #ffffff;

  h3 {
    display: flex;
    align-items: center;
    margin: 0;
    font-size: 48px;
    font-weight: 900;
    line-height: 1;
  }
}

.frontline-pqc-piece-list {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  grid-auto-rows: max-content;
  gap: 10px;
  align-content: start;
  min-height: 0;
  padding-right: 8px;
  overflow-y: auto;
}

.frontline-pqc-piece-row {
  display: grid;
  grid-template-rows: auto auto;
  gap: 8px;
  align-items: start;
  align-self: start;
  height: fit-content;
  min-width: 0;
  min-height: 0;
  padding: 10px 12px;
  background: #f8faf8;
  border: 3px solid var(--frontline-line);
  border-radius: 16px;

  > strong {
    font-size: 24px;
    font-weight: 900;
  }
}

.frontline-pqc-piece-value-control {
  display: grid;
  grid-template-columns: 44px minmax(80px, 1fr) 44px 52px;
  gap: 6px;
  align-items: center;
  min-width: 0;

  button,
  input {
    width: 100%;
    height: 50px;
    min-width: 0;
    padding: 0;
    border: 3px solid var(--frontline-line);
    border-radius: 12px;
    background: #ffffff;
    color: var(--frontline-ink);
    text-align: center;
    font-size: 30px;
    font-weight: 900;
  }

  button {
    cursor: pointer;
  }

  span {
    font-size: 22px;
    font-weight: 900;
    white-space: nowrap;
  }
}

.frontline-pqc-piece-choice {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 56px;
}

.frontline-pqc-piece-switch {
  --el-switch-on-color: #15815f;
  --el-switch-off-color: #b9382f;

  width: 100%;
  height: 56px;

  :deep(.el-switch__core) {
    width: 100% !important;
    height: 56px;
    min-width: 0;
    border: 3px solid var(--frontline-line);
    border-radius: 14px;
  }

  :deep(.el-switch__action) {
    left: 3px;
    width: 44px;
    height: 44px;
  }

  :deep(.el-switch__inner) {
    height: 50px;
    padding: 0 8px 0 52px;
  }

  :deep(.el-switch__inner .is-text) {
    max-width: none;
    font-size: 24px;
    font-weight: 900;
    line-height: 1;
    color: #fff;
  }

  &.is-checked :deep(.el-switch__action) {
    left: calc(100% - 47px);
  }

  &.is-checked :deep(.el-switch__inner) {
    padding: 0 52px 0 8px;
  }
}

.frontline-pqc-piece-actions {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 18px;

  button {
    border: 3px solid var(--frontline-line);
    border-radius: 22px;
    background: #ffffff;
    color: var(--frontline-ink);
    font-size: 40px;
    font-weight: 900;
    cursor: pointer;

    &.primary {
      border-color: #15815f;
      background: #15815f;
      color: #ffffff;
    }
  }
}

.frontline-choice-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;

  button {
    height: 92px;
    border: 3px solid var(--frontline-line);
    border-radius: 20px;
    background: #f8faf8;
    color: var(--frontline-ink);
    font-size: 38px;
    font-weight: 900;
    cursor: pointer;

    &.active {
      border-color: var(--frontline-dark);
      background: var(--frontline-dark);
      color: #ffffff;
    }
  }
}

.frontline-number-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;

  label {
    display: grid;
    gap: 12px;
    min-width: 0;
  }

  span {
    font-size: 32px;
    font-weight: 900;
  }
}

.frontline-submit-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  border: 3px solid var(--frontline-line);
  border-radius: 28px;
  background: var(--frontline-panel);

  span {
    min-width: 0;
    overflow: hidden;
    color: var(--frontline-muted);
    font-size: 30px;
    font-weight: 800;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.el-button) {
    width: 100%;
    height: 72px;
    border-radius: 20px;
    font-size: 36px;
    font-weight: 900;
  }
}

.frontline-picker {
  position: absolute;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: rgba(17, 26, 21, 0.38);
}

.frontline-picker__card {
  display: grid;
  gap: 20px;
  width: min(760px, calc(100% - 80px));
  padding: 28px;
  border: 3px solid var(--frontline-line);
  border-radius: 28px;
  background: var(--frontline-panel);

  h3 {
    margin: 0;
    font-size: 48px;
    font-weight: 900;
    line-height: 1;
  }
}

.frontline-picker__options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  max-height: 520px;
  overflow: auto;

  button {
    min-height: 112px;
    border: 3px solid var(--frontline-line);
    border-radius: 22px;
    background: #f8faf8;
    color: var(--frontline-ink);
    font-size: 34px;
    font-weight: 900;
    cursor: pointer;

    &.active {
      border-color: var(--frontline-dark);
      background: var(--frontline-dark);
      color: #ffffff;
    }
  }
}

.frontline-picker__empty {
  grid-column: 1 / -1;
  align-self: center;
  margin: 0;
  color: #66736c;
  font-size: 28px;
  font-weight: 800;
  text-align: center;
}

.frontline-picker__close {
  height: 86px;
  border: 3px solid var(--frontline-line);
  border-radius: 22px;
  background: #f8faf8;
  color: var(--frontline-ink);
  font-size: 36px;
  font-weight: 900;
  cursor: pointer;
}

.frontline-operator-panel.is-production-mode .frontline-picker {
  z-index: 10;
  display: grid;
  place-items: center;
  border-radius: 0;
  background: rgba(17, 26, 21, 0.38);
}

.frontline-operator-panel.is-production-mode .frontline-picker__card {
  width: min(96%, 1770px);
  aspect-ratio: 1920 / 1080;
  grid-template-rows: auto minmax(0, 1fr) auto;
  padding: 32px;
  border: 3px solid var(--frontline-line);
  border-radius: 28px;
  background: var(--frontline-panel);
}

.frontline-operator-panel.is-production-mode .frontline-picker__title {
  font-size: 48px;
  line-height: 1;
  font-weight: 900;
}

.frontline-operator-panel.is-production-mode .frontline-picker__options {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  align-content: start;
  min-height: 0;
  max-height: none;
  overflow: auto;
}

.frontline-operator-panel.is-production-mode .frontline-picker__option {
  display: flex;
  align-items: center;
  justify-content: center;
  height: auto;
  aspect-ratio: 1920 / 720;
  min-height: 0;
  padding: 6px 8px;
  border: 3px solid var(--frontline-line);
  border-radius: 18px;
  background: #f8faf8;
  color: var(--frontline-ink);
  font-size: 24px;
  font-weight: 900;
  line-height: 1.05;
  text-align: center;
  word-break: break-word;
  overflow: hidden;
}

.frontline-operator-panel.is-production-mode .frontline-picker__option.active {
  border-color: var(--frontline-dark);
  background: var(--frontline-dark);
  color: #ffffff;
}

.frontline-operator-panel.is-production-mode .frontline-picker__close {
  height: 68px;
  border: 3px solid var(--frontline-line);
  border-radius: 18px;
  background: #f8faf8;
  color: var(--frontline-ink);
  font-size: 30px;
  font-weight: 900;
}

.frontline-picker--production-order {
  z-index: 30;
  display: grid;
  place-items: center;
  border-radius: 0;
  background: rgba(17, 26, 21, 0.38);
}

.frontline-picker--production-order .frontline-picker__card {
  width: min(96%, 1770px);
  aspect-ratio: 1920 / 1080;
  grid-template-rows: auto minmax(0, 1fr) auto;
  padding: 32px;
  border: 3px solid var(--frontline-line);
  border-radius: 28px;
  background: var(--frontline-panel);
}

.frontline-picker--production-order .frontline-picker__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-width: 0;
}

.frontline-picker--production-order .frontline-picker__order-search {
  flex: 0 1 620px;
  width: min(620px, 46%);
  height: 72px;
  padding: 0 24px;
  border: 3px solid var(--frontline-line);
  border-radius: 12px;
  outline: none;
  background: #ffffff;
  color: var(--frontline-ink);
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: 0;
}

.frontline-picker--production-order .frontline-picker__order-search:focus {
  border-color: var(--frontline-dark);
  box-shadow: 0 0 0 4px rgba(31, 50, 42, 0.16);
}

.frontline-picker--production-order .frontline-picker__options {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  align-content: start;
  min-height: 0;
  max-height: none;
  overflow: auto;
}

.frontline-picker--production-order .frontline-picker__empty {
  grid-column: 1 / -1;
  align-self: center;
  margin: 0;
  color: #66736c;
  font-size: 32px;
  font-weight: 800;
  text-align: center;
}

.frontline-picker--production-order .frontline-picker__option {
  display: flex;
  align-items: center;
  justify-content: center;
  height: auto;
  min-height: 132px;
  padding: 8px 10px;
  border: 3px solid var(--frontline-line);
  border-radius: 22px;
  background: #f8faf8;
  color: var(--frontline-ink);
  font-size: 15px;
  font-weight: 900;
  line-height: 1.1;
  text-align: center;
  word-break: break-word;
  overflow: visible;
}

.frontline-order-picker-option {
  display: grid;
  grid-template-rows: repeat(3, auto);
  gap: 5px;
  align-content: center;
  width: 100%;
  min-width: 0;
}

.frontline-order-picker-option__row {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 6px;
  align-items: center;
  min-width: 0;
  color: #66736c;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.15;
  text-align: left;
}

.frontline-order-picker-option__value {
  min-width: 0;
  overflow: visible;
  color: var(--frontline-ink);
  font-size: 15px;
  font-weight: 900;
  line-height: 1.15;
  text-align: left;
  text-overflow: clip;
  white-space: normal;
  overflow-wrap: anywhere;
}

.frontline-picker--production-order .frontline-picker__option.active .frontline-order-picker-option__row,
.frontline-picker--production-order .frontline-picker__option.active .frontline-order-picker-option__value {
  color: #ffffff;
}

.frontline-picker--production-order .frontline-picker__close {
  height: 86px;
  border: 3px solid var(--frontline-line);
  border-radius: 22px;
  background: #f8faf8;
  color: var(--frontline-ink);
  font-size: 36px;
  font-weight: 900;
}

.frontline-picker--production-process {
  z-index: 30;
  display: grid;
  place-items: center;
  border-radius: 0;
  background: rgba(17, 26, 21, 0.38);
}

.frontline-picker--production-process .frontline-picker__card {
  width: min(96%, 1770px);
  aspect-ratio: 1920 / 1080;
  grid-template-rows: auto minmax(0, 1fr) auto;
  padding: 32px;
  border: 3px solid var(--frontline-line);
  border-radius: 28px;
  background: var(--frontline-panel);
}

.frontline-picker--production-process .frontline-picker__options {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  align-content: start;
  min-height: 0;
  max-height: none;
  overflow: auto;
}

.frontline-picker--production-process .frontline-picker__option {
  display: flex;
  align-items: center;
  justify-content: center;
  height: auto;
  aspect-ratio: 1920 / 720;
  min-height: 0;
  padding: 6px 8px;
  border: 3px solid var(--frontline-line);
  border-radius: 18px;
  background: #f8faf8;
  color: var(--frontline-ink);
  font-size: 24px;
  font-weight: 900;
  line-height: 1.05;
  text-align: center;
  word-break: break-word;
  overflow: hidden;
}

.frontline-picker--production-process .frontline-picker__option.active {
  border-color: var(--frontline-dark);
  background: var(--frontline-dark);
  color: #ffffff;
}

.frontline-picker--production-process .frontline-picker__close {
  height: 68px;
  border: 3px solid var(--frontline-line);
  border-radius: 18px;
  background: #f8faf8;
  color: var(--frontline-ink);
  font-size: 30px;
  font-weight: 900;
}

.frontline-operator-screen :deep(.el-input-number),
.frontline-operator-screen :deep(.el-input),
.frontline-operator-screen :deep(.el-radio-group) {
  width: 100%;
}

.frontline-operator-screen :deep(.el-input-number .el-input__wrapper),
.frontline-operator-screen :deep(.el-input .el-input__wrapper) {
  min-height: 76px;
  border-radius: 18px;
  font-size: 34px;
}

.frontline-operator-screen :deep(.el-radio-button) {
  flex: 1;
}

.frontline-operator-screen :deep(.el-radio-button__inner) {
  width: 100%;
  min-height: 76px;
  padding: 20px 18px;
  border-radius: 18px;
  font-size: 30px;
  font-weight: 900;
}

@media (max-width: 1280px) {
  .frontline-operator-screen.is-pqc {
    min-height: 860px;
  }

  .frontline-operator-top.is-pqc,
  .frontline-operator-main.is-pqc {
    grid-template-columns: 1fr;
  }

  .frontline-operator-main.is-pqc {
    grid-template-rows: auto auto 104px;
  }

  .frontline-pqc-content-panel,
  .frontline-pqc-fill-panel,
  .frontline-pqc-submit-bar {
    grid-column: auto;
    grid-row: auto;
  }

  .frontline-pqc-choice-actions,
  .frontline-pqc-type-tabs,
  .frontline-pqc-round-tabs,
  .frontline-pqc-number-field,
  .frontline-pqc-submit-bar {
    grid-template-columns: 1fr !important;
  }

  .frontline-pqc-piece-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .frontline-pqc-piece-actions {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
