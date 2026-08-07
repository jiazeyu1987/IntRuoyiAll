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
        <button class="frontline-top-card" type="button" @click="openPicker('process')">
          <span>工序</span>
          <strong>{{ selectedProcessLabel }}</strong>
        </button>
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
                <button
                  type="button"
                  class="pass"
                  :class="{ active: pqcPieceDraftValues[pieceIndex - 1] === '合格' }"
                  :aria-label="`第 ${pieceIndex} 件${activePqcInspectionItem.label}合格`"
                  @click="pqcPieceDraftValues[pieceIndex - 1] = '合格'"
                >
                  合格
                </button>
                <button
                  type="button"
                  class="fail"
                  :class="{ active: pqcPieceDraftValues[pieceIndex - 1] === '不合格' }"
                  :aria-label="`第 ${pieceIndex} 件${activePqcInspectionItem.label}不合格`"
                  @click="pqcPieceDraftValues[pieceIndex - 1] = '不合格'"
                >
                  不合格
                </button>
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
        :aria-label="`${activePqcStandardItem.label}接收标准`"
        @click.self="closePqcStandardDialog"
      >
        <section>
          <h3>{{ activePqcStandardItem.label }}接收标准</h3>
          <p>{{ activePqcStandardItem.standardText || '未配置接收标准说明' }}</p>
          <dl>
            <dt>下限</dt>
            <dd>
              {{
                formatPqcStandardBound(
                  activePqcStandardItem.standardLowerLimit,
                  activePqcStandardItem.standardUnit
                )
              }}
            </dd>
            <dt>上限</dt>
            <dd>
              {{
                formatPqcStandardBound(
                  activePqcStandardItem.standardUpperLimit,
                  activePqcStandardItem.standardUnit
                )
              }}
            </dd>
            <dt>单位</dt>
            <dd>{{ activePqcStandardItem.standardUnit || '未配置' }}</dd>
          </dl>
          <button type="button" @click="closePqcStandardDialog">关闭</button>
        </section>
      </div>

      <div
        v-if="activePqcMethodItem"
        class="frontline-pqc-fact-dialog"
        data-pqc-method-dialog
        role="dialog"
        aria-modal="true"
        :aria-label="`${formatPqcMethodSummary(activePqcMethodItem)}检验方法`"
        @click.self="closePqcMethodDialog"
      >
        <section>
          <h3>{{ formatPqcMethodSummary(activePqcMethodItem) }}</h3>
          <p>{{ formatPqcMethodSummary(activePqcMethodItem) }}</p>
          <button type="button" @click="closePqcMethodDialog">关闭</button>
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
              <div class="pqc-active-summary">
                <h3>{{ formatPqcInspectionTitle(activePqcTabItem) }}</h3>
                <span>{{ getPqcTabStateLabel(activePqcTabItem) }}</span>
                <small data-pqc-inspection-meta>
                  {{ formatPqcInspectionMeta(activePqcTabItem) }} / {{ getPqcProgressText(activePqcTabItem.key) }}
                </small>
              </div>

              <div class="pqc-utility-strip" :aria-label="`${activePqcTabItem.label}质检信息`">
                <label
                  class="pqc-select-card"
                  data-pqc-equipment-card
                  :class="{
                    'is-selected': Boolean(getPqcItemSelection(activePqcTabItem.key).selectedEquipmentId),
                    'is-empty': !getPqcItemSelection(activePqcTabItem.key).selectedEquipmentId
                  }"
                >
                  <span
                    v-if="activePqcTabItem.equipmentRequired"
                    class="pqc-required-dot"
                    aria-hidden="true"
                  ></span>
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

                <label
                  class="pqc-select-card"
                  data-pqc-equipment-number-card
                  :class="{
                    'is-selected': Boolean(getPqcItemSelection(activePqcTabItem.key).selectedEquipmentNumber),
                    'is-empty': !getPqcItemSelection(activePqcTabItem.key).selectedEquipmentNumber
                  }"
                >
                  <span
                    v-if="activePqcTabItem.equipmentRequired"
                    class="pqc-required-dot"
                    aria-hidden="true"
                  ></span>
                  <span>
                    <strong>设备编号</strong>
                    <span>{{ getPqcSelectedEquipmentNumberLabel(activePqcTabItem) }}</span>
                  </span>
                  <em aria-hidden="true">&gt;</em>
                  <select
                    class="pqc-select-native"
                    :value="getPqcItemSelection(activePqcTabItem.key).selectedEquipmentNumber ?? ''"
                    data-pqc-equipment-number-select
                    aria-label="选择设备编号"
                    @change="updatePqcItemSelectedEquipmentNumber(activePqcTabItem.key, $event)"
                  >
                    <option value="">选择设备编号</option>
                    <option
                      v-for="option in getPqcEquipmentNumberOptions(activePqcTabItem.key)"
                      :key="`${option.equipmentId}:${option.equipmentNumber}`"
                      :value="option.equipmentNumber"
                    >
                      {{ option.equipmentNumber }}
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
                <em>{{ getPqcTabStateLabel(item) }}</em>
                <small>
                  <span data-pqc-tab-requirement>{{ formatPqcTabRequirement(item) }}</span>
                  <span data-pqc-tab-progress>{{ getPqcProgressText(item.key) }}</span>
                </small>
              </button>
            </nav>
          </div>
        </section>

        <section class="frontline-work-panel frontline-pqc-fill-panel">
          <div class="frontline-pqc-type-tabs">
            <button
              type="button"
              :class="{ active: pqcDraft.inspectionType === 'FIRST' }"
              @click="selectPqcInspectionType('FIRST')"
            >
              首检
            </button>
            <button
              type="button"
              :class="{ active: pqcDraft.inspectionType === 'PATROL' }"
              @click="selectPqcInspectionType('PATROL')"
            >
              巡检
            </button>
            <button
              type="button"
              :class="{ active: pqcDraft.inspectionType === 'FINAL' }"
              @click="selectPqcInspectionType('FINAL')"
            >
              末检
            </button>
          </div>
          <div
            class="frontline-pqc-round-tabs"
            :style="{ gridTemplateColumns: `repeat(${pqcVisibleRounds.length}, minmax(0, 1fr))` }"
          >
            <button
              v-for="round in pqcVisibleRounds"
              :key="round.value"
              type="button"
              :class="{ active: pqcDraft.patrolRound === round.value }"
              @click="pqcDraft.patrolRound = round.value"
            >
              {{ round.label }}
            </button>
          </div>
          <div class="frontline-pqc-production-source">
            <label for="frontlinePqcProductionSubmit">生产提交</label>
            <select
              id="frontlinePqcProductionSubmit"
              v-model.number="selectedPqcProductionSubmitEventId"
              data-pqc-production-submit-select
              :disabled="productionSubmitCandidates.length <= 1 || Boolean(pqcSubmitReceipt)"
            >
              <option :value="undefined" disabled>请选择正式生产提交记录</option>
              <option
                v-for="candidate in productionSubmitCandidates"
                :key="candidate.eventId"
                :value="candidate.eventId"
              >
                {{ formatPqcProductionSubmitCandidate(candidate) }}
              </option>
            </select>
          </div>
          <div class="frontline-pqc-form-area">
            <div class="frontline-pqc-number-field">
              <label for="frontlinePqcInspectionQuantity">检验</label>
              <button
                type="button"
                aria-label="检验减少"
                :disabled="isPqcInspectionQuantityLocked"
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
                :disabled="isPqcInspectionQuantityLocked"
                @input="updatePqcQuantity('inspectionQuantity', $event)"
              />
              <button
                type="button"
                aria-label="检验增加"
                :disabled="isPqcInspectionQuantityLocked"
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
            <div class="frontline-pqc-defect-description">
              <label for="frontlinePqcDefectDescription">不良</label>
              <textarea
                id="frontlinePqcDefectDescription"
                data-pqc-defect-description
                :value="pqcDraft.defectDescription ?? ''"
                placeholder="出现不良或损耗时手动输入说明"
                rows="3"
                @input="updatePqcDefectDescription"
              ></textarea>
            </div>
          </div>
        </section>
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

      <section v-if="pqcSubmitReceipt" class="frontline-pqc-submit-receipt" data-pqc-submit-receipt>
        <strong>正式提交成功</strong>
        <span>事件 {{ pqcSubmitReceipt.pqcEventId }}</span>
        <span>记录 {{ pqcSubmitReceipt.pqcRecordId }}</span>
        <span>签名 {{ pqcSubmitReceipt.signatureId }}</span>
        <span>{{ pqcSubmitReceipt.inspectionResult === 'SUCCESS' ? '合格' : '不合格' }}</span>
        <time>{{ pqcSubmitReceipt.serverSubmitTime }}</time>
      </section>
      <section
        v-if="pqcSubmitResultUncertain"
        class="frontline-pqc-submit-uncertain"
        data-pqc-submit-uncertain
      >
        PQC正式提交结果不确定，状态确认失败。请刷新页面或联系组长核对后再操作，当前页面已锁定重复提交。
      </section>

      <footer class="frontline-pqc-submit-bar">
        <button
          class="frontline-pqc-reset-button"
          type="button"
          :disabled="Boolean(pqcSubmitReceipt) || pqcSubmitResultUncertain"
          @click="handleResetPqc"
        >
          重填
        </button>
        <button
          class="frontline-pqc-submit-button"
          type="button"
          :disabled="payloadLoading || Boolean(pqcSubmitReceipt) || pqcSubmitResultUncertain"
          @click="handleValidate"
        >
          {{ payloadLoading ? '提交中' : '提交' }}
        </button>
      </footer>
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
            @click="openPicker('process')"
          >
            <div class="top-label">工序</div>
            <div class="top-value">{{ selectedProcessLabel }}</div>
          </button>
          <button
            class="frontline-top-card top-box frontline-production-selection-card"
            type="button"
            data-frontline-production-selection-card
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
          :aria-label="activePicker === 'process' ? '选择工序' : '选择员工'"
          @click.self="closePicker"
        >
          <div class="frontline-picker__card picker-card">
            <h3 class="frontline-picker__title picker-title">
              {{ activePicker === 'process' ? '选工序' : '选择员工' }}
            </h3>
            <div class="frontline-picker__options picker-options">
              <p
                v-if="pickerStatusText"
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
                {{ option.label }}
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
            <div class="panel-title">填数量</div>

            <div class="frontline-production-number-field field">
              <label class="field-label" for="frontlineProductionOutputQuantity">完成数量</label>
              <button
                class="num-btn"
                type="button"
                aria-label="完成数量减少"
                :disabled="isProductionSubmitted || payloadLoading"
                @click="adjustProductionOutputQuantity(-1)"
              >
                -
              </button>
              <input
                class="value-box"
                id="frontlineProductionOutputQuantity"
                :value="productionDraft.outputQuantity ?? ''"
                inputmode="numeric"
                :disabled="isProductionSubmitted || payloadLoading"
                @input="updateProductionOutputQuantity"
              />
              <button
                class="num-btn"
                type="button"
                aria-label="完成数量增加"
                :disabled="isProductionSubmitted || payloadLoading"
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
                    :disabled="isProductionSubmitted || payloadLoading"
                    @click="adjustProductionDefectQuantity(defect.key, -1)"
                  >
                    -
                  </button>
                  <input
                    class="frontline-production-defect-qty defect-qty"
                    :value="getProductionDefectQuantity(defect.key)"
                    inputmode="numeric"
                    :aria-label="`${defect.label}数量`"
                    :disabled="isProductionSubmitted || payloadLoading"
                    @input="updateProductionDefectQuantity(defect.key, $event)"
                  />
                  <button
                    type="button"
                    class="frontline-production-defect-step defect-step"
                    :aria-label="`${defect.label}增加`"
                    :disabled="isProductionSubmitted || payloadLoading"
                    @click="adjustProductionDefectQuantity(defect.key, 1)"
                  >
                    +
                  </button>
                  <span class="frontline-production-defect-unit defect-unit">件</span>
                </div>
              </div>
            </section>
          </section>

          <section
            class="frontline-work-panel panel device-panel frontline-production-device-panel"
            aria-label="设备"
          >
            <div class="panel-title">填设备</div>
            <div
              v-if="visibleDeviceCards.length > 0"
              class="frontline-production-device-tabs device-tabs"
              role="tablist"
              aria-label="设备切换"
            >
              <button
                v-for="device in visibleDeviceCards"
                :key="device.key"
                class="device-tab"
                type="button"
                role="tab"
                :aria-selected="device.key === selectedProductionDeviceKey"
                :class="{ active: device.key === selectedProductionDeviceKey }"
                :disabled="isProductionSubmitted || payloadLoading"
                @click="selectedProductionDeviceKey = device.key"
              >
                {{ device.label }}
              </button>
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
              <div
                v-for="parameter in activeProductionDevice.parameters"
                :key="parameter.parameterCode"
                class="frontline-production-device-param device-param"
              >
                <label
                  class="device-param-label"
                  :for="`frontlineProductionDeviceParameter-${parameter.parameterCode}`"
                >
                  {{ parameter.parameterName || parameter.parameterCode }}
                </label>
                <span
                  v-if="isTextStandardParameter(parameter)"
                  class="frontline-production-device-standard-text"
                  data-frontline-text-parameter-standard
                >
                  {{ parameter.standardText }}
                </span>
                <button
                  v-else
                  class="device-num"
                  type="button"
                  :aria-label="`${parameter.parameterName || parameter.parameterCode}减少`"
                  :disabled="isProductionSubmitted || payloadLoading"
                  @click="adjustProductionDeviceParameter(activeProductionDevice.key, parameter.parameterCode, -1)"
                >
                  -
                </button>
                <input
                  v-if="!isTextStandardParameter(parameter)"
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
                  :disabled="isProductionSubmitted || payloadLoading"
                  @input="updateProductionDeviceParameter(activeProductionDevice.key, parameter.parameterCode, $event)"
                />
                <button
                  v-if="!isTextStandardParameter(parameter)"
                  class="device-num"
                  type="button"
                  :aria-label="`${parameter.parameterName || parameter.parameterCode}增加`"
                  :disabled="isProductionSubmitted || payloadLoading"
                  @click="adjustProductionDeviceParameter(activeProductionDevice.key, parameter.parameterCode, 1)"
                >
                  +
                </button>
                <span v-if="!isTextStandardParameter(parameter)" class="device-unit">
                  {{ parameter.unit || '' }}
                </span>
              </div>
            </div>
          </section>
        </main>

        <footer class="frontline-production-submit-bar bottom">
          <button
            class="frontline-production-reset-button minor-btn"
            type="button"
            :disabled="isProductionSubmitted || payloadLoading"
            @click="handleResetProduction"
          >
            重填
          </button>
          <button
            class="frontline-production-submit-button submit-btn"
            type="button"
            :class="{ 'is-submitted': isProductionSubmitted }"
            :disabled="isSubmitBlocked"
            :data-formal-feedback-id="formalSubmitResult?.feedbackId"
            :data-formal-recordbook-entry-id="formalSubmitResult?.recordbookEntryId"
            :data-formal-process-pool-event-id="formalSubmitResult?.processPoolEventId"
            @click="handleValidate"
          >
            <span>
              {{
                isProductionSubmitted
                  ? `已正式提交 · 报工 ${formalSubmitResult?.feedbackId}`
                  : payloadLoading
                    ? '提交中'
                    : submitConfirmationOpen
                      ? '等待确认'
                      : '正式提交'
              }}
            </span>
            <small v-if="formalSubmitResult">
              记录本 {{ formalSubmitResult.recordbookEntryId }} · 工序池 {{ formalSubmitResult.processPoolEventId }}
            </small>
          </button>
        </footer>
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
          <span>登录密码</span>
          <input
            id="frontlineProductionSignaturePassword"
            v-model="productionSignaturePassword"
            type="password"
            data-production-submit-signature-password
            autocomplete="current-password"
            :disabled="payloadLoading"
            placeholder="请输入当前账号密码"
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
  type FrontlinePqcInspectionSubmitRespVO,
  type FrontlinePqcInspectionSubmitReqVO,
  type FrontlinePqcProductionSubmitCandidateVO,
  type FrontlineRuntimeDeviceParameterVO,
  type ProFrontlineDeviceParameterReadingReqVO,
  type ProFrontlineFeedbackSubmitReqVO,
  type ProFrontlineFeedbackSubmitRespVO,
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
  FRONTLINE_PQC_NO_PENDING_ORDER_TEXT,
  createFrontlineDeviceEmployeeState,
  loadFrontlineDeviceProcesses,
  loadFrontlinePqcActiveOrders,
  preloadFrontlineProductionRuntimeCache,
  preloadFrontlinePqcSwitchingCache,
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
type ProductionDeviceParameterDraft = Record<ProductionDeviceParameterKey, number | undefined>

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

interface ProductionDeviceCard {
  key: string
  deviceId: number
  deviceCode?: string
  deviceName?: string
  label: string
  parameters: FrontlineRuntimeDeviceParameterVO[]
}

interface PqcInspectionItem {
  key: PqcInspectionItemKey
  itemName: string
  label: string
  type: 'number' | 'choice'
  inspectionMethod: string
  standardText: string
  resultType: string
  standardLowerLimit?: number | string
  standardUpperLimit?: number | string
  standardUnit: string
  standardPrecision?: number
  equipmentRequired: boolean
  equipmentOptions: FrontlinePqcEquipmentOptionVO[]
  unit: string
  defaultValue: string
  step: number
}

interface PqcItemSelection {
  selectedEquipmentId?: number
  selectedEquipmentNumber?: string
}

type FrontlinePqcTaskProcess = FrontlineDeviceRouteProcessVO & {
  activeOrderId: number
  pqcTaskId: number
  regulationVersionId: number
  inspectionType: string
  businessDate: string
  shiftCode: string
  roundNo: number
  plannedInspectionQuantity: number
  inspectionItems: NonNullable<FrontlineDeviceRouteProcessVO['inspectionItems']>
}

const props = withDefaults(defineProps<{ mode?: 'production' | 'pqc' }>(), {
  mode: 'production'
})

const message = useMessage()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const catalog = ref<FrontlineTemplateDefinitionVO[]>([])
const payloadLoading = ref(false)
const payloadPreview = ref<FrontlineTemplatePayloadVO>()
const formalSubmitResult = ref<ProFrontlineFeedbackSubmitRespVO>()
const submitConfirmationOpen = ref(false)
const productionFormalSubmitConfirmationText = ref('')
const productionSignaturePassword = ref('')
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

const selectedProductionDeviceKey = ref<string>()
const deviceParameterDraft = reactive<Record<string, ProductionDeviceParameterDraft>>({})

const pqcDraft = reactive({
  inspectionType: undefined as InspectionType | undefined,
  patrolRound: undefined as number | undefined,
  inspectionQuantity: undefined as number | undefined,
  scrapQuantity: undefined as number | undefined,
  defectDescription: undefined as string | undefined
})

const activePqcInspectionKey = ref<PqcInspectionItemKey>()
const activePqcStandardKey = ref<PqcInspectionItemKey>()
const activePqcMethodKey = ref<PqcInspectionItemKey>()
const selectedPqcInspectionKey = ref<PqcInspectionItemKey>()
const pqcPieceDraftValues = ref<string[]>([])
const pqcPieceValues = reactive<Record<string, string[]>>({})
const pqcItemSelections = reactive<Record<PqcInspectionItemKey, PqcItemSelection>>({})
const selectedPqcProductionSubmitEventId = ref<number>()
const pqcSignatureDialogVisible = ref(false)
const pqcSignaturePassword = ref('')
const pqcSubmitReceipt = ref<FrontlinePqcInspectionSubmitRespVO>()
const pqcSubmitResultUncertain = ref(false)

const isPqcMode = computed(() => props.mode === 'pqc')
const PRODUCTION_CANVAS_WIDTH = 1920
const PRODUCTION_CANVAS_HEIGHT = 1080
const productionViewportScale = ref(1)
let productionViewportScaleFrame: number | undefined
let productionViewportResizeObserver: ResizeObserver | undefined
let productionProcessSelectionRequestId = 0
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
const productionSubmitCandidates = computed(() =>
  deviceState.selectedProcess?.productionSubmitCandidates || []
)
const productionSubmitContext = computed(() => deviceState.runtimeConfig?.productionSubmitContext)

const selectedActiveOrder = computed(() => deviceState.selectedActiveOrder)

const productionOrderLabel = computed(() => {
  const selectedOrder = selectedActiveOrder.value
  const submitContext = productionSubmitContext.value
  return selectedOrder?.workOrderCode ||
    selectedOrder?.workOrderName ||
    submitContext?.workOrderCode ||
    submitContext?.workOrderName ||
    firstRouteQueryText(['productionOrderCode', 'workOrderCode', 'orderCode']) ||
    '未选择订单'
})

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

const selectedEmployeeLabel = computed(() => formatEmployeeLabel(deviceState.selectedEmployee))

const formatPqcServerSubmitTime = (value: string | number) => {
  if (typeof value === 'number') {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) {
      throw new Error(`PQC生产提交时间无效：${value}`)
    }
    const pad = (part: number) => String(part).padStart(2, '0')
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
      `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  }
  return value.replace('T', ' ')
}

const formatPqcProductionSubmitCandidate = (candidate: FrontlinePqcProductionSubmitCandidateVO) =>
  `#${candidate.eventId} ${formatPqcServerSubmitTime(candidate.serverSubmitTime)}`

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

const pqcInspectionItems = computed<PqcInspectionItem[]>(() =>
  (deviceState.selectedProcess?.inspectionItems || []).map((item) => ({
    key: item.itemCode,
    itemName: normalizePqcInspectionItemName(item.itemName),
    label: normalizePqcInspectionItemName(item.itemName) || '未配置检验项目名称',
    type: isPqcNumericResultType(item.resultType) ? 'number' : 'choice',
    inspectionMethod: item.inspectionMethod || '',
    standardText: item.standardText || '',
    resultType: item.resultType || '',
    standardLowerLimit: item.standardLowerLimit,
    standardUpperLimit: item.standardUpperLimit,
    standardUnit: item.standardUnit || '',
    standardPrecision: item.standardPrecision,
    // Backend formal itemResults require equipment identity for every QA item.
    equipmentRequired: true,
    equipmentOptions: item.equipmentOptions || [],
    unit: item.standardUnit || '',
    defaultValue: item.standardLowerLimit === undefined || item.standardLowerLimit === null
      ? ''
      : String(item.standardLowerLimit),
    step: resolvePqcNumericStep(item.standardPrecision, item.resultType)
  }))
)

const pqcInspectionItemMap = computed<Record<PqcInspectionItemKey, PqcInspectionItem>>(() =>
  pqcInspectionItems.value.reduce<Record<PqcInspectionItemKey, PqcInspectionItem>>((items, item) => {
    items[item.key] = item
    return items
  }, {})
)

const pqcInspectionItemKeys = computed<PqcInspectionItemKey[]>(() =>
  pqcInspectionItems.value.map((item) => item.key)
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
  if (!pqcDraft.inspectionType || !pqcDraft.patrolRound) {
    return []
  }
  if (pqcDraft.inspectionType === 'FIRST') {
    return [{ value: pqcDraft.patrolRound, label: '首检' }]
  }
  if (pqcDraft.inspectionType === 'FINAL') {
    return [{ value: pqcDraft.patrolRound, label: '末检' }]
  }
  return [{ value: pqcDraft.patrolRound, label: `第 ${pqcDraft.patrolRound} 次` }]
})

const templateModeMismatch = computed(() =>
  Boolean(employeeTemplateCode.value && employeeTemplateCode.value !== expectedTemplateCode.value)
)

const templateBindingMissing = computed(() =>
  Boolean(deviceState.selectedEmployee && !employeeTemplateCode.value)
)

const isProductionSubmitted = computed(() =>
  !isPqcMode.value && Boolean(formalSubmitResult.value)
)

const isSubmitBlocked = computed(() =>
  payloadLoading.value ||
  submitConfirmationOpen.value ||
  isProductionSubmitted.value ||
  templateModeMismatch.value ||
  templateBindingMissing.value ||
  (isPqcMode.value && !deviceState.selectedActiveOrder) ||
  (isPqcMode.value && !hasPqcTaskSnapshot(deviceState.selectedProcess)) ||
  !deviceState.selectedProcess ||
  !deviceState.selectedEmployee
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
  if (isPqcMode.value && productionSubmitCandidates.value.length === 0) {
    return '当前PQC任务没有可绑定的正式生产提交事件'
  }
  if (isPqcMode.value && productionSubmitCandidates.value.length > 1 &&
    !selectedPqcProductionSubmitEventId.value) {
    return '请选择本次PQC对应的生产提交记录'
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

const configuredDeviceCards = computed<ProductionDeviceCard[]>(() =>
  (deviceState.runtimeConfig?.devices || [])
    .filter((device) => Number(device.deviceId || 0) > 0)
    .map((device, index) => ({
      key: String(device.deviceId),
      deviceId: device.deviceId,
      deviceCode: device.deviceCode,
      deviceName: device.deviceName,
      label: device.deviceName || device.deviceCode || `设备 ${index + 1}`,
      parameters: device.parameters || []
    }))
)

const isTextStandardParameter = (parameter: FrontlineRuntimeDeviceParameterVO) =>
  parameter.valueType === 'TEXT_STANDARD'

const visibleDeviceCards = computed(() => configuredDeviceCards.value.slice(0, 3))

const activeProductionDevice = computed(() =>
  visibleDeviceCards.value.find((device) => device.key === selectedProductionDeviceKey.value) ||
  visibleDeviceCards.value[0]
)

const switchableProcessOptions = computed(() => {
  const seen = new Set<string>()
  return deviceState.processOptions.filter((process) => {
    const key = `${process.routeId}-${process.routeProcessId}-${process.processId}`
    if (seen.has(key)) {
      return false
    }
    seen.add(key)
    return true
  })
})

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
    return '待检工单加载中'
  }
  if (deviceState.lastError) {
    return deviceState.lastError
  }
  if (deviceState.activeOrderOptions.length === 0) {
    return FRONTLINE_PQC_NO_PENDING_ORDER_TEXT
  }
  return '未找到匹配的待检工单'
})

const pickerOptions = computed<FrontlinePickerOption[]>(() => {
  if (activePicker.value === 'order') {
    return filteredActiveOrderOptions.value.map((order) => ({
      key: `${order.workOrderId}-${order.routeId}`,
      label: formatActiveOrderLabel(order),
      active: isSameActiveOrder(order, deviceState.selectedActiveOrder),
      activeOrder: order,
      onClick: () => handleSelectActiveOrder(order)
    }))
  }
  if (activePicker.value === 'process') {
    return switchableProcessOptions.value.map((process) => ({
      key: `${process.routeId}-${process.routeProcessId}-${process.processId}`,
      label: formatProcessLabel(process),
      active: isSameProcess(process, deviceState.selectedProcess),
      onClick: () => handleSelectProcess(process)
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
      formalSubmitResult.value = undefined
      pqcSubmitResultUncertain.value = false
    }
  },
  { flush: 'sync' }
)

watch(
  visibleDeviceCards,
  (devices) => {
    const visibleKeys = new Set(devices.map((device) => device.key))
    for (const deviceKey of Object.keys(deviceParameterDraft)) {
      if (!visibleKeys.has(deviceKey)) {
        delete deviceParameterDraft[deviceKey]
      }
    }
    if (!devices.length) {
      selectedProductionDeviceKey.value = undefined
      return
    }
    for (const device of devices) {
      if (!deviceParameterDraft[device.key]) {
        deviceParameterDraft[device.key] = {}
      }
      for (const parameter of device.parameters) {
        if (!parameter.parameterCode || isTextStandardParameter(parameter)) {
          continue
        }
        const params = deviceParameterDraft[device.key]
        if (params[parameter.parameterCode] === undefined && parameter.defaultValue !== undefined) {
          params[parameter.parameterCode] = normalizeProductionParameter(parameter.defaultValue)
        }
      }
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

function normalizeProductionParameter(value: unknown) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    return undefined
  }
  return Math.max(0, parsed)
}

const toFiniteProductionParameterNumber = (value: unknown) => {
  if (value === undefined || value === null || String(value).trim() === '') {
    return undefined
  }
  const parsed = Number(String(value).replace(/,/g, '').trim())
  return Number.isFinite(parsed) ? parsed : undefined
}

const resolveProductionParameterStatus = (
  value: unknown,
  parameter: FrontlineRuntimeDeviceParameterVO
): ProFrontlineParameterStatus => {
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
    value === '' ? undefined : normalizeProductionParameter(value)
}

const adjustProductionDeviceParameter = (
  deviceKey: string,
  parameterKey: ProductionDeviceParameterKey,
  delta: number
) => {
  const params = ensureProductionDeviceParameters(deviceKey)
  params[parameterKey] = Math.max(0, Number(params[parameterKey] || 0) + delta)
}

const handleResetProduction = () => {
  if (isProductionSubmitted.value) {
    return
  }
  productionDraft.outputQuantity = undefined
  for (const defect of configuredDefectReasons.value) {
    productionDefectDraft[defect.key] = 0
  }
  for (const deviceKey of Object.keys(deviceParameterDraft)) {
    delete deviceParameterDraft[deviceKey]
  }
}

const normalizePqcQuantity = (value?: number) => {
  if (!Number.isFinite(value)) {
    return 0
  }
  return Math.max(0, Math.trunc(Number(value)))
}

const isPqcNumericResultType = (resultType?: string) => {
  const normalized = String(resultType || '').toUpperCase()
  return ['NUMBER', 'NUMERIC', 'DECIMAL', 'MEASURE', 'MEASURED_VALUE'].includes(normalized)
}

const resolvePqcNumericStep = (precision?: number, resultType?: string) => {
  if (!isPqcNumericResultType(resultType)) {
    return 0
  }
  if (precision && precision > 0) {
    return Number(`0.${'0'.repeat(Math.max(0, precision - 1))}1`)
  }
  return 1
}

const hasPqcTaskSnapshot = (
  process?: FrontlineDeviceRouteProcessVO
): process is FrontlinePqcTaskProcess => Boolean(
  process?.activeOrderId &&
  process?.pqcTaskId &&
  process?.regulationVersionId &&
  process?.inspectionType &&
  process?.businessDate &&
  process?.shiftCode &&
  process?.roundNo &&
  process?.plannedInspectionQuantity &&
  process?.inspectionItems?.length
)

const isPqcInspectionQuantityLocked = computed(() =>
  isPqcMode.value && hasPqcTaskSnapshot(deviceState.selectedProcess)
)

const resolvePqcInspectionType = (inspectionType?: string): InspectionType => {
  if (inspectionType === 'FIRST' || inspectionType === 'PATROL' || inspectionType === 'FINAL') {
    return inspectionType
  }
  throw new Error(`PQC任务检验类型${inspectionType || '空'}无效。`)
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

const getPqcItemSelection = (itemKey: PqcInspectionItemKey) => {
  if (!pqcItemSelections[itemKey]) {
    pqcItemSelections[itemKey] = {}
  }
  return pqcItemSelections[itemKey]
}

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
  [
    option.equipmentName || option.equipmentCode || `设备${option.equipmentId}`,
    option.equipmentCode
  ].filter(Boolean).join(' / ')

const getPqcEquipmentNumberOptions = (itemKey: PqcInspectionItemKey) => {
  const item = pqcInspectionItemMap.value[itemKey]
  if (!item) {
    return []
  }
  const selectedEquipmentId = getPqcItemSelection(itemKey).selectedEquipmentId
  return item.equipmentOptions.filter((option) =>
    selectedEquipmentId ? option.equipmentId === selectedEquipmentId : true
  )
}

const updatePqcItemSelectedEquipment = (itemKey: PqcInspectionItemKey, event: Event) => {
  const value = (event.target as HTMLSelectElement).value
  const selection = getPqcItemSelection(itemKey)
  selection.selectedEquipmentId = value ? Number(value) : undefined
  const firstNumber = getPqcEquipmentNumberOptions(itemKey)[0]?.equipmentNumber
  selection.selectedEquipmentNumber = firstNumber || undefined
}

const updatePqcItemSelectedEquipmentNumber = (itemKey: PqcInspectionItemKey, event: Event) => {
  const selection = getPqcItemSelection(itemKey)
  selection.selectedEquipmentNumber = (event.target as HTMLSelectElement).value || undefined
}

const openPqcStandardDialog = (itemKey: PqcInspectionItemKey) => {
  activePqcStandardKey.value = itemKey
}

const openPqcMethodDialog = (itemKey: PqcInspectionItemKey) => {
  activePqcMethodKey.value = itemKey
}

const closePqcStandardDialog = () => {
  activePqcStandardKey.value = undefined
}

const closePqcMethodDialog = () => {
  activePqcMethodKey.value = undefined
}

const formatPqcStandardBound = (value?: number | string, unit?: string) => {
  if (value === undefined || value === null || value === '') {
    return '未配置'
  }
  return `${value}${unit || ''}`
}

const applyPqcTaskSnapshotToDraft = (process: FrontlineDeviceRouteProcessVO) => {
  if (!hasPqcTaskSnapshot(process)) {
    throw new Error('当前工序缺少PQC任务或QA规程快照。')
  }
  pqcDraft.inspectionType = resolvePqcInspectionType(process.inspectionType)
  pqcDraft.patrolRound = process.roundNo
  pqcDraft.inspectionQuantity = process.plannedInspectionQuantity
  pqcDraft.scrapQuantity = undefined
  pqcDraft.defectDescription = undefined
  const candidates = process.productionSubmitCandidates || []
  selectedPqcProductionSubmitEventId.value = candidates.length === 1
    ? candidates[0].eventId
    : undefined
  pqcSignatureDialogVisible.value = false
  pqcSignaturePassword.value = ''
  pqcSubmitReceipt.value = undefined
  pqcSubmitResultUncertain.value = false
  clearPqcPieceValues()
}

const getPqcPieceStateKey = (itemKey: PqcInspectionItemKey) => {
  const process = deviceState.selectedProcess
  if (!process || !pqcDraft.inspectionType || !pqcDraft.patrolRound) {
    return undefined
  }
  return [
    process.activeOrderId,
    process.pqcTaskId,
    process.regulationVersionId,
    process.routeId,
    process.routeProcessId,
    process.processId,
    pqcDraft.inspectionType,
    pqcDraft.patrolRound,
    itemKey
  ].join(':')
}

const getPqcStoredPieceValues = (itemKey: PqcInspectionItemKey) => {
  const stateKey = getPqcPieceStateKey(itemKey)
  if (!stateKey) {
    return []
  }
  const item = pqcInspectionItemMap.value[itemKey]
  if (!item) {
    throw new Error(`PQC检验项目${itemKey}不在当前QA规程快照中。`)
  }
  const quantity = pqcInspectionQuantity.value
  const values = pqcPieceValues[stateKey] || []
  while (values.length < quantity) {
    values.push(item.defaultValue)
  }
  pqcPieceValues[stateKey] = values
  return values
}

const getPqcCompletedCount = (itemKey: PqcInspectionItemKey) =>
  getPqcStoredPieceValues(itemKey)
    .slice(0, pqcInspectionQuantity.value)
    .filter((value) => value.trim().length > 0).length

const getPqcProgressText = (itemKey: PqcInspectionItemKey) =>
  `已填 ${getPqcCompletedCount(itemKey)}/${pqcInspectionQuantity.value}`

const selectPqcInspectionTab = (itemKey: PqcInspectionItemKey) => {
  selectedPqcInspectionKey.value = itemKey
}

const formatPqcTabRequirement = (item: PqcInspectionItem) =>
  item.equipmentRequired ? '设备必填' : '设备必填（正式提交必填）'

const getPqcTabStateLabel = (item: PqcInspectionItem) => {
  if (activePqcTabKey.value === item.key) {
    return '当前'
  }
  const completedCount = getPqcCompletedCount(item.key)
  if (pqcInspectionQuantity.value > 0 && completedCount >= pqcInspectionQuantity.value) {
    return '完成'
  }
  if (!getPqcItemSelection(item.key).selectedEquipmentId) {
    return '待选'
  }
  return '未检'
}

const getPqcSelectedEquipmentLabel = (item: PqcInspectionItem) => {
  const selectedEquipmentId = getPqcItemSelection(item.key).selectedEquipmentId
  const selectedOption = item.equipmentOptions.find((option) =>
    option.equipmentId === selectedEquipmentId
  )
  if (selectedOption) {
    return formatPqcEquipmentLabel(selectedOption)
  }
  return item.equipmentOptions.length ? '选择检验设备' : '缺少检验设备配置'
}

const getPqcSelectedEquipmentNumberLabel = (item: PqcInspectionItem) =>
  getPqcItemSelection(item.key).selectedEquipmentNumber ||
  '选择设备编号'

const formatPqcInspectionItemTabLabel = (item: PqcInspectionItem) =>
  formatPqcMethodSummary(item)

const formatPqcStandardSummary = (item: PqcInspectionItem) => {
  if (item.standardText) {
    return item.standardText
  }
  const lower = formatPqcStandardBound(item.standardLowerLimit, item.standardUnit)
  const upper = formatPqcStandardBound(item.standardUpperLimit, item.standardUnit)
  if (lower !== '未配置' || upper !== '未配置') {
    return `${lower} ~ ${upper}`
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
  normalizePqcInspectionMethodLabel(item.inspectionMethod) || '未配置检验方法'

const formatPqcInspectionTitle = (item: PqcInspectionItem) =>
  formatPqcMethodSummary(item)

const formatPqcResultType = (resultType: string) => {
  const normalized = resultType.trim().toUpperCase()
  if (normalized === 'NUMBER' || normalized === 'NUMERIC') {
    return '数值'
  }
  if (normalized === 'BOOLEAN' || normalized === 'CHOICE' || normalized === 'PASS_FAIL') {
    return '合格/不合格'
  }
  return resultType || '未配置'
}

const formatPqcInspectionMeta = (item: PqcInspectionItem) =>
  [
    `判定: ${formatPqcResultType(item.resultType)}`,
    item.standardUnit ? `单位: ${item.standardUnit}` : '',
    `设备: ${item.equipmentOptions.length}项`
  ].filter(Boolean).join(' / ')

const requirePqcItemSelection = (item: PqcInspectionItem) => {
  const selection = getPqcItemSelection(item.key)
  if (!item.equipmentOptions.length) {
    throw new Error(`${item.label}缺少正式检验设备配置。`)
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

const assertPqcSubmissionItemEquipmentSelections = () => {
  for (const item of pqcInspectionItems.value) {
    requirePqcItemSelection(item)
  }
}

const getPqcExactPieceValuesForSubmit = (itemKey: PqcInspectionItemKey) => {
  const item = pqcInspectionItemMap.value[itemKey]
  if (!item) {
    throw new Error(`PQC检验项目${itemKey}不在当前QA规程快照中。`)
  }
  const stateKey = getPqcPieceStateKey(itemKey)
  if (!stateKey) {
    throw new Error(`${item.label}缺少PQC任务上下文，无法提交逐件检验。`)
  }
  const values = pqcPieceValues[stateKey] || []
  if (values.length !== pqcInspectionQuantity.value) {
    throw new Error(`${item.label}样本数量${values.length}与任务计划数量${pqcInspectionQuantity.value}不一致。`)
  }
  return values.map((value) => String(value ?? '').trim())
}

const assertPqcSubmissionSampleQuantities = () => {
  for (const itemKey of pqcInspectionItemKeys.value) {
    getPqcExactPieceValuesForSubmit(itemKey)
  }
}

const buildPqcItemResultsPayload = () =>
  pqcInspectionItems.value.map((item) => {
    const { selection } = requirePqcItemSelection(item)
    return {
      itemCode: item.key,
      selectedEquipmentId: selection.selectedEquipmentId!,
      selectedEquipmentNumber: selection.selectedEquipmentNumber!,
      sampleValues: getPqcExactPieceValuesForSubmit(item.key)
    }
  })

const buildPqcItemDetailsPayload = () =>
  pqcInspectionItems.value.map((item) => {
    const { selection, selectedOption } = requirePqcItemSelection(item)
    return {
      itemCode: item.key,
      itemName: item.itemName,
      selectedEquipmentId: selection.selectedEquipmentId,
      selectedEquipmentCode: selectedOption?.equipmentCode,
      selectedEquipmentName: selectedOption?.equipmentName,
      selectedEquipmentNumber: selection.selectedEquipmentNumber,
      standardText: item.standardText,
      standardLowerLimit: item.standardLowerLimit,
      standardUpperLimit: item.standardUpperLimit,
      standardUnit: item.standardUnit,
      standardPrecision: item.standardPrecision,
      inspectionMethod: item.inspectionMethod,
      resultType: item.resultType,
      sampleValues: getPqcExactPieceValuesForSubmit(item.key)
    }
  })

const getPqcCurrentChoiceValues = (itemKey: PqcInspectionItemKey) =>
  getPqcStoredPieceValues(itemKey).slice(0, pqcInspectionQuantity.value)

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
    const error = new Error('请先选择工序，再填写逐件检验。')
    message.error(error.message)
    throw error
  }
  if (!hasPqcTaskSnapshot(deviceState.selectedProcess)) {
    const error = new Error('当前工序缺少PQC任务或QA规程快照，无法填写逐件检验。')
    message.error(error.message)
    throw error
  }
  if (pqcInspectionQuantity.value <= 0) {
    const error = new Error('请先填写大于 0 的检验数量。')
    message.error(error.message)
    throw error
  }
}

const openPqcPieceInspection = (itemKey: PqcInspectionItemKey) => {
  assertPqcPieceContext()
  activePqcInspectionKey.value = itemKey
  pqcPieceDraftValues.value = getPqcStoredPieceValues(itemKey).slice()
}

const closePqcPieceInspection = (saveChanges: boolean) => {
  const itemKey = activePqcInspectionKey.value
  if (saveChanges && itemKey) {
    const stateKey = getPqcPieceStateKey(itemKey)
    if (!stateKey) {
      const error = new Error('当前工序上下文已失效，无法保存逐件检验。')
      message.error(error.message)
      throw error
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
  assertPqcPieceContext()
  const values = getPqcStoredPieceValues(itemKey)
  for (let index = 0; index < pqcInspectionQuantity.value; index += 1) {
    values[index] = result
  }
}

const stepPqcPieceValue = (index: number, delta: number) => {
  const item = activePqcInspectionItem.value
  if (!item || item.type !== 'number') {
    const error = new Error('当前检验项目不是数值项目，无法调整数值。')
    message.error(error.message)
    throw error
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

const selectPqcInspectionType = (inspectionType: InspectionType) => {
  if (isPqcMode.value && deviceState.selectedProcess?.inspectionType) {
    const taskInspectionType = resolvePqcInspectionType(deviceState.selectedProcess.inspectionType)
    if (inspectionType !== taskInspectionType) {
      message.error('PQC检验类型来自任务快照，不能在前端切换。')
      return
    }
  }
  pqcDraft.inspectionType = inspectionType
  pqcDraft.patrolRound = deviceState.selectedProcess?.roundNo || 1
}

const updatePqcQuantity = (field: PqcQuantityField, event: Event) => {
  if (field === 'inspectionQuantity' && isPqcInspectionQuantityLocked.value) {
    return
  }
  const inputValue = (event.target as HTMLInputElement).value
  pqcDraft[field] = inputValue === '' ? undefined : normalizePqcQuantity(Number(inputValue))
}

const updatePqcDefectDescription = (event: Event) => {
  const inputValue = (event.target as HTMLTextAreaElement).value
  pqcDraft.defectDescription = inputValue || undefined
}

const adjustPqcQuantity = (field: PqcQuantityField, delta: number) => {
  if (field === 'inspectionQuantity' && isPqcInspectionQuantityLocked.value) {
    return
  }
  pqcDraft[field] = normalizePqcQuantity(pqcDraft[field]) + delta
  if (pqcDraft[field] < 0) {
    pqcDraft[field] = 0
  }
}

const handleResetPqc = () => {
  if (pqcSubmitReceipt.value || pqcSubmitResultUncertain.value) {
    return
  }
  for (const itemKey of pqcInspectionItemKeys.value) {
    const stateKey = getPqcPieceStateKey(itemKey)
    if (stateKey) {
      delete pqcPieceValues[stateKey]
    }
  }
  activePqcInspectionKey.value = undefined
  pqcPieceDraftValues.value = []
  pqcDraft.defectDescription = undefined
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

const handleHome = () => {
  router.push('/')
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
    message.error(resolveErrorMessage(error))
    throw error
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
  await preloadFrontlineProductionRuntimeCache(deviceState, switchableProcessOptions.value)
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
    message.error(resolveErrorMessage(error))
    throw error
  }
}

const findInitialProcess = (
  processes: FrontlineDeviceRouteProcessVO[] = switchableProcessOptions.value
) => {
  const requestedRouteId = context.routeId
  const requestedRouteProcessId = context.routeProcessId
  const requestedProcessId = context.processId
  if (requestedRouteId || requestedRouteProcessId || requestedProcessId) {
    const matchedProcess = processes.find((process) =>
      (!requestedRouteId || process.routeId === requestedRouteId) &&
      (!requestedRouteProcessId || process.routeProcessId === requestedRouteProcessId) &&
      (!requestedProcessId || process.processId === requestedProcessId)
    )
    if (matchedProcess) {
      return matchedProcess
    }
  }
  const fallbackProcess = isPqcMode.value
    ? processes.find(hasPqcTaskSnapshot) || processes[0]
    : processes[0]
  return fallbackProcess
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

const findCurrentLoginEmployee = () =>
  deviceState.employeeOptions.find((employee) => isCurrentLoginEmployee(employee))

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

const handleSelectActiveOrder = async (activeOrder: FrontlineActiveOrderVO) => {
  selectedPqcProductionSubmitEventId.value = undefined
  pqcSubmitReceipt.value = undefined
  pqcSubmitResultUncertain.value = false
  pqcSignatureDialogVisible.value = false
  pqcSignaturePassword.value = ''
  let processes: FrontlineDeviceRouteProcessVO[]
  try {
    processes = await selectFrontlinePqcActiveOrder(deviceState, activeOrder)
  } catch (error) {
    message.error(resolveErrorMessage(error))
    closePicker()
    return
  }
  applyActiveOrderToContext(activeOrder)
  employeeTemplateCode.value = undefined
  payloadPreview.value = undefined
  const initialProcess = findInitialProcess(processes)
  if (initialProcess) {
    await handleSelectProcess(initialProcess)
  } else {
    closePicker()
  }
}

const handleSelectProcess = async (process: FrontlineDeviceRouteProcessVO) => {
  const shouldClosePickerImmediately = !isPqcMode.value
  const selectionRequestId = shouldClosePickerImmediately
    ? ++productionProcessSelectionRequestId
    : 0
  if (shouldClosePickerImmediately) {
    closePicker()
  }

  if (isPqcMode.value) {
    await selectFrontlinePqcProcess(deviceState, process)
  } else {
    await selectFrontlineProcess(deviceState, process)
  }
  if (shouldClosePickerImmediately && selectionRequestId !== productionProcessSelectionRequestId) {
    return
  }
  applyProcessToContext(process)
  employeeTemplateCode.value = undefined
  payloadPreview.value = undefined
  pqcSubmitResultUncertain.value = false
  const initialEmployee = findInitialEmployee()
  if (initialEmployee) {
    await handleSelectEmployee(initialEmployee)
  } else if (isPqcMode.value) {
    const error = new Error('当前登录账号未返回PQC人员候选，无法进入PQC填写。')
    message.error(error.message)
    throw error
  }
  if (!shouldClosePickerImmediately) {
    closePicker()
  }
}

const handleSelectEmployee = async (employee: FrontlineEmployeeCandidateVO) => {
  const shouldClosePickerImmediately = !isPqcMode.value
  const selectionRequestId = shouldClosePickerImmediately
    ? ++productionEmployeeSelectionRequestId
    : 0
  if (isPqcMode.value && !isCurrentLoginEmployee(employee)) {
    const error = new Error('一线PQC员工已锁定为当前登录账号，不能切换。')
    message.error(error.message)
    throw error
  }
  if (shouldClosePickerImmediately) {
    closePicker()
  }
  const result = isPqcMode.value
    ? await switchFrontlinePqcActualEmployee(deviceState, employee.userId)
    : await switchFrontlineActualEmployee(deviceState, employee.userId)
  if (shouldClosePickerImmediately && selectionRequestId !== productionEmployeeSelectionRequestId) {
    return
  }
  context.actualEmployeeId = result.actualEmployeeId
  const templateCode = resolveTemplateCode(result.template?.templateNo, result.template?.templateType)
  employeeTemplateCode.value = templateCode
  payloadPreview.value = undefined
  if (!shouldClosePickerImmediately) {
    closePicker()
  }
}

const assertProductionSubmissionReady = () => {
  if (!productionDraft.outputQuantity || productionDraft.outputQuantity <= 0) {
    throw new Error('请填写完成数量')
  }
  if (productionScrapQuantity.value > productionDraft.outputQuantity) {
    throw new Error('损耗数量不能大于完成数量')
  }
  const device = activeProductionDevice.value
  if (!device) {
    return
  }
  const missingParameters = device.parameters
    .filter((parameter) => !isTextStandardParameter(parameter))
    .filter((parameter) =>
      toFiniteProductionParameterNumber(
        getProductionDeviceParameter(device.key, parameter.parameterCode)
      ) === undefined
    )
    .map((parameter) => parameter.parameterName || parameter.parameterCode)
  if (missingParameters.length) {
    throw new Error(`请填写设备参数：${missingParameters.join('、')}`)
  }
}

const buildProductionFormalSubmitConfirmation = () => {
  const device = activeProductionDevice.value
  const parameterSummary = device
    ? device.parameters.map((parameter) => {
        const label = parameter.parameterName || parameter.parameterCode
        if (isTextStandardParameter(parameter)) {
          return `${label}=${parameter.standardText || '未配置'}`
        }
        const value = getProductionDeviceParameter(device.key, parameter.parameterCode)
        const status = resolveProductionParameterStatus(value, parameter)
        const statusLabel = status === 'NORMAL' ? '' : '（参数异常）'
        return `${label}=${value}${parameter.unit || ''}${statusLabel}`
      }).join('、')
    : ''
  return [
    `生产订单：${productionOrderLabel.value}`,
    `工序：${selectedProcessLabel.value}`,
    `实际员工：${selectedEmployeeLabel.value}`,
    `完成数量：${productionDraft.outputQuantity}件`,
    `损耗数量：${productionScrapQuantity.value}件`,
    `设备：${device?.label || '无设备'}`,
    `设备参数：${parameterSummary || (device ? '无数值参数' : '无设备参数')}`,
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
    message.error('请输入当前登录账号的电子签名密码。')
    return
  }
  resolveProductionFormalSubmitConfirmation(true)
}

const handleProductionFormalSubmit = async () => {
  if (payloadLoading.value || submitConfirmationOpen.value || formalSubmitResult.value) {
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
    formalSubmitResult.value = await ProFeedbackApi.frontlineSubmit(formalPayload)
    message.success(`正式提交成功，报工编号 ${formalSubmitResult.value.feedbackId}`)
  } finally {
    payloadLoading.value = false
  }
}

const assertPqcFormalSubmissionReady = () => {
  const process = deviceState.selectedProcess
  const candidates = productionSubmitCandidates.value
  if (!deviceState.selectedActiveOrder) {
    throw new Error('请选择活跃订单。')
  }
  if (!process || !hasPqcTaskSnapshot(process)) {
    throw new Error('当前工序缺少待执行PQC任务或发布态QA规程快照。')
  }
  if (!pqcInspectionItems.value.length) {
    throw new Error('当前工序缺少发布态QA检验项目，无法正式提交。')
  }
  if (!deviceState.selectedEmployee || !isCurrentLoginEmployee(deviceState.selectedEmployee)) {
    throw new Error('当前登录账号不是本次PQC实际填写员工。')
  }
  if (!candidates.length) {
    throw new Error('当前PQC任务没有可绑定的正式生产提交事件。')
  }
  if (!selectedPqcProductionSubmitEventId.value) {
    throw new Error(candidates.length > 1
      ? '请选择本次PQC对应的生产提交记录。'
      : '缺少本次PQC对应的生产提交记录。')
  }
  if (!candidates.some((candidate) =>
    candidate.eventId === selectedPqcProductionSubmitEventId.value)) {
    throw new Error('所选生产提交记录不属于当前PQC任务工序。')
  }
}

const handleValidate = async () => {
  if (pqcSubmitResultUncertain.value) {
    message.error('PQC正式提交结果不确定，请刷新页面或联系组长核对后再操作。')
    return
  }
  if (templateBindingMissing.value) {
    const error = new Error('当前员工缺少一线填写模板，无法提交。')
    message.error(error.message)
    return
  }
  if (templateModeMismatch.value) {
    const error = new Error(statusText.value)
    message.error(error.message)
    return
  }
  if (!isPqcMode.value) {
    try {
      await handleProductionFormalSubmit()
    } catch (error) {
      message.error(resolveErrorMessage(error))
    }
    return
  }
  try {
    assertPqcFormalSubmissionReady()
    assertPqcSubmissionItemEquipmentSelections()
    assertPqcSubmissionSampleQuantities()
    validatePqcDefectDescription()
  } catch (error) {
    message.error(resolveErrorMessage(error))
    return
  }
  Object.assign(draft.fieldValues, buildPqcFieldValues())
  payloadLoading.value = true
  try {
    assertFormalPayloadContext()
    const templatePayload = buildFrontlineTemplatePayload(context, draft.fieldValues)
    payloadPreview.value = await FrontlineTemplateApi.validatePayload(templatePayload)
    pqcSignaturePassword.value = ''
    pqcSignatureDialogVisible.value = true
  } catch (error) {
    message.error(resolveErrorMessage(error))
  } finally {
    payloadLoading.value = false
  }
}

const closePqcSignatureDialog = () => {
  if (payloadLoading.value) {
    return
  }
  pqcSignatureDialogVisible.value = false
  pqcSignaturePassword.value = ''
}

const recoverPqcSubmitReceiptAfterUncertainError = async (submitError: unknown) => {
  const process = deviceState.selectedProcess
  if (!process?.pqcTaskId) {
    return false
  }
  try {
    const recoveredReceipt = await ProFeedbackApi.getFrontlinePqcSubmitReceipt({
      pqcTaskId: process.pqcTaskId
    })
    if (!recoveredReceipt) {
      return false
    }
    pqcSubmitReceipt.value = recoveredReceipt
    pqcSignatureDialogVisible.value = false
    message.success(`PQC正式提交已完成，已恢复事件编号 ${recoveredReceipt.pqcEventId}`)
    return true
  } catch (confirmationError) {
    pqcSubmitResultUncertain.value = true
    pqcSignatureDialogVisible.value = false
    message.error(
      `PQC正式提交结果不确定，状态确认失败：${resolveErrorMessage(confirmationError)}；` +
      `原始提交错误：${resolveErrorMessage(submitError)}。请刷新页面或联系组长核对后再操作。`
    )
    return true
  }
}

const handleConfirmPqcSubmit = async () => {
  if (payloadLoading.value || pqcSubmitReceipt.value || pqcSubmitResultUncertain.value) {
    return
  }
  if (!pqcSignaturePassword.value.trim()) {
    message.error('请输入当前登录账号的电子签名密码。')
    return
  }
  if (!payloadPreview.value) {
    message.error('缺少已校验的PQC正式提交载荷。')
    return
  }
  try {
    assertPqcFormalSubmissionReady()
    assertPqcSubmissionItemEquipmentSelections()
    assertPqcSubmissionSampleQuantities()
    validatePqcDefectDescription()
  } catch (error) {
    message.error(resolveErrorMessage(error))
    return
  }
  let submitPayload: FrontlinePqcInspectionSubmitReqVO
  try {
    submitPayload = buildPqcInspectionSubmitPayload(payloadPreview.value)
  } catch (error) {
    message.error(resolveErrorMessage(error))
    return
  }
  payloadLoading.value = true
  try {
    pqcSubmitReceipt.value = await ProFeedbackApi.submitFrontlinePqcInspection(
      submitPayload
    )
    pqcSignatureDialogVisible.value = false
    message.success(`PQC正式提交成功，事件编号 ${pqcSubmitReceipt.value.pqcEventId}`)
  } catch (error) {
    const recovered = await recoverPqcSubmitReceiptAfterUncertainError(error)
    if (!recovered) {
      message.error(resolveErrorMessage(error))
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
  if (!context.processId || !context.routeProcessId) {
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
}

const readFrontlineFormalSubmitContext = (): FrontlineFormalSubmitContext => {
  const selectedProcess = deviceState.selectedProcess
  const serverContext = deviceState.runtimeConfig?.productionSubmitContext
  return {
    workOrderId: serverContext?.workOrderId,
    workOrderCode: serverContext?.workOrderCode,
    taskId: serverContext?.taskId,
    routeId: serverContext?.routeId,
    routeProcessId: serverContext?.routeProcessId,
    processId: serverContext?.processId,
    workstationId: serverContext?.workstationId,
    deviceId: activeProductionDevice.value?.key
      ? Number(activeProductionDevice.value.key)
      : selectedProcess?.deviceId,
    deviceAccountUserId: Number(userStore.getUser?.id || 0),
    itemId: serverContext?.itemId,
    approveUserId: serverContext?.approveUserId,
    recordbookId: serverContext?.recordbookId,
    signatureEmployeeId: context.actualEmployeeId,
    signaturePassword: productionSignaturePassword.value.trim(),
    scheduleOrderId: serverContext?.scheduleOrderId,
    scheduleOrderProcessId: serverContext?.scheduleOrderProcessId,
    scheduledQuantity: serverContext?.scheduledQuantity,
    expireDate: serverContext?.expireDate ? String(serverContext.expireDate) : undefined
  }
}

const assertFrontlineFormalSubmitContext = (formalContext: FrontlineFormalSubmitContext) => {
  const missingFields: string[] = []
  const requiredFields: Array<[keyof FrontlineFormalSubmitContext, string]> = [
    ['workOrderId', '订单上下文'],
    ['taskId', '生产任务'],
    ['routeId', '路线'],
    ['routeProcessId', '路线工序'],
    ['processId', '工序'],
    ['workstationId', '工作站'],
    ['deviceAccountUserId', '设备账号'],
    ['itemId', '产品物料'],
    ['approveUserId', '班组长审批人'],
    ['recordbookId', '记录本'],
    ['signaturePassword', '签名'],
    ['signatureEmployeeId', '签名员工']
  ]
  for (const [field, label] of requiredFields) {
    const value = formalContext[field]
    if (value === undefined || value === null || value === '' || Number(value) <= 0) {
      missingFields.push(label)
    }
  }
  if (!productionDraft.outputQuantity || productionDraft.outputQuantity <= 0) {
    missingFields.push('产出数量')
  }
  if (
    formalContext.signatureEmployeeId &&
    context.actualEmployeeId &&
    formalContext.signatureEmployeeId !== context.actualEmployeeId
  ) {
    throw new Error('签名员工必须等于实际填写员工，无法提交。')
  }
  if (
    formalContext.signatureEmployeeId &&
    currentLoginUserId.value &&
    formalContext.signatureEmployeeId !== currentLoginUserId.value
  ) {
    throw new Error('当前登录账号必须是实际填写员工，无法完成电子签名。')
  }
  if (missingFields.length) {
    throw new Error(`缺少${missingFields.join('、')}，无法提交。`)
  }
}

const buildFrontlineFormalSubmitPayload = (
  rawPayload: FrontlineTemplatePayloadReqVO
): ProFrontlineFeedbackSubmitReqVO => {
  const formalContext = readFrontlineFormalSubmitContext()
  assertFrontlineFormalSubmitContext(formalContext)
  const signaturePassword = productionSignaturePassword.value.trim()
  if (!signaturePassword) {
    throw new Error('请输入当前登录账号的电子签名密码。')
  }
  const selectedDevice = activeProductionDevice.value
  const equipmentParameters = selectedDevice
    ? { [selectedDevice.label]: buildProductionDeviceParameterPayload(selectedDevice.key) }
    : {}
  return {
    feedbackPayload: {
      workstationId: formalContext.workstationId!,
      routeId: formalContext.routeId!,
      processId: formalContext.processId!,
      workOrderId: formalContext.workOrderId!,
      taskId: formalContext.taskId!,
      scheduleOrderId: formalContext.scheduleOrderId,
      scheduleOrderProcessId: formalContext.scheduleOrderProcessId,
      itemId: formalContext.itemId!,
      expireDate: formalContext.expireDate,
      scheduledQuantity: formalContext.scheduledQuantity,
      outputQuantity: productionDraft.outputQuantity!,
      lossQuantity: productionScrapQuantity.value,
      lossDetails: buildProductionLossDetailsPayload(),
      selectedDevice: buildProductionSelectedDevicePayload(),
      deviceParameterReadings: buildProductionDeviceParameterReadingsPayload(),
      laborScrapQuantity: productionScrapQuantity.value,
      materialScrapQuantity: 0,
      otherScrapQuantity: 0,
      approveUserId: formalContext.approveUserId!,
      remark: firstRouteQueryText(['feedbackRemark', 'remark'])
    },
    recordbookPayload: {
      recordbookId: formalContext.recordbookId!,
      entryTitle:
        firstRouteQueryText(['recordbookEntryTitle']) ||
        `一线报工-${formalContext.workOrderCode || formalContext.workOrderId}-${formalContext.taskId}`,
      entryContent: {
        fieldValues: { ...draft.fieldValues },
        defects: { ...productionDefectDraft },
        productionOrder: productionOrderLabel.value,
        process: selectedProcessLabel.value,
        employee: selectedEmployeeLabel.value
      },
      equipmentParameters,
      tagCodes: [],
      idempotencyKey:
        `frontline-submit-${formalContext.workOrderId}-${formalContext.taskId}-${formalContext.routeProcessId}-${context.actualEmployeeId}`,
      remark: firstRouteQueryText(['recordbookRemark'])
    },
    processPoolSubmissionIdempotencyKey:
      `frontline-process-pool-${formalContext.workOrderId}-${formalContext.taskId}-${formalContext.routeProcessId}-${context.actualEmployeeId}`,
    processPoolContext: {
      workOrderId: formalContext.workOrderId!,
      taskId: formalContext.taskId!,
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
    rawPayload: buildProductionStructuredRawPayload(rawPayload) as unknown as Record<string, unknown>
  }
}

const buildProductionDeviceParameterPayload = (deviceKey: string) => {
  const params = deviceParameterDraft[deviceKey] || {}
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined)
  )
}

const buildProductionLossDetailsPayload = (): ProFrontlineLossDetailReqVO[] =>
  configuredDefectReasons.value
    .map((defect) => ({
      reasonId: defect.reasonId,
      reasonCode: defect.reasonCode,
      reasonName: defect.label,
      quantity: productionDefectDraft[defect.key] || 0
    }))
    .filter((defect) => defect.quantity > 0)

const buildProductionSelectedDevicePayload = (): ProFrontlineSelectedDeviceReqVO | undefined => {
  const device = activeProductionDevice.value
  if (!device) {
    return undefined
  }
  return {
    deviceId: device.deviceId,
    deviceCode: device.deviceCode,
    deviceName: device.deviceName || device.label
  }
}

const buildProductionDeviceParameterReadingsPayload =
  (): ProFrontlineDeviceParameterReadingReqVO[] => {
    const device = activeProductionDevice.value
    if (!device) {
      return []
    }
    return device.parameters
      .filter((parameter) => !isTextStandardParameter(parameter))
      .map<ProFrontlineDeviceParameterReadingReqVO | undefined>((parameter) => {
        const value = getProductionDeviceParameter(device.key, parameter.parameterCode)
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

const buildProductionEquipmentParameterRulesPayload = () =>
  activeProductionDevice.value
    ? Object.fromEntries([[
      activeProductionDevice.value.label,
      activeProductionDevice.value.parameters.map((parameter) => ({
        parameterCode: parameter.parameterCode,
        parameterName: parameter.parameterName,
        unit: parameter.unit,
        lowerLimit: parameter.lowerLimit,
        upperLimit: parameter.upperLimit,
        valueType: parameter.valueType,
        standardText: parameter.standardText
      }))
    ]])
    : {}

const buildProductionStructuredRawPayload = (rawPayload: FrontlineTemplatePayloadReqVO) => ({
  ...rawPayload,
  lossDetails: buildProductionLossDetailsPayload(),
  lossReasonDetails: buildProductionLossDetailsPayload(),
  selectedDevice: buildProductionSelectedDevicePayload(),
  deviceParameterReadings: buildProductionDeviceParameterReadingsPayload(),
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

const buildPqcPieceValuesPayload = () => {
  const values: Record<string, string[]> = {}
  for (const itemKey of pqcInspectionItemKeys.value) {
    values[itemKey] = getPqcExactPieceValuesForSubmit(itemKey)
  }
  return values
}

const buildPqcInspectionSubmitPayload = (
  validatedPayload: FrontlineTemplatePayloadVO
): FrontlinePqcInspectionSubmitReqVO => {
  const activeOrder = deviceState.selectedActiveOrder
  const process = deviceState.selectedProcess
  const employee = deviceState.selectedEmployee
  const actualEmployeeId = context.actualEmployeeId
  const productionSubmitEventId = selectedPqcProductionSubmitEventId.value
  const missingFormalContext: string[] = []
  if (!productionSubmitEventId) {
    missingFormalContext.push('productionSubmitEventId')
  }
  if (!activeOrder || !process || !employee || !actualEmployeeId ||
    !hasPqcTaskSnapshot(process) || !pqcDraft.inspectionType || !pqcDraft.patrolRound ||
    missingFormalContext.length) {
    throw new Error(`缺少PQC正式提交上下文：${missingFormalContext.join('、')}，无法提交。`)
  }
  const inspectionResult = resolvePqcResult()
  const itemResults = buildPqcItemResultsPayload()
  const pqcItemDetails = buildPqcItemDetailsPayload()
  return {
    activeOrderId: process.activeOrderId,
    pqcTaskId: process.pqcTaskId,
    productionSubmitEventId,
    regulationVersionId: process.regulationVersionId,
    workOrderId: activeOrder.workOrderId,
    routeId: process.routeId,
    routeProcessId: process.routeProcessId,
    processId: process.processId,
    inspectionType: pqcDraft.inspectionType,
    businessDate: process.businessDate,
    shiftCode: process.shiftCode,
    roundNo: pqcDraft.patrolRound,
    actualInspectionQuantity: pqcInspectionQuantity.value,
    scrapQuantity: normalizePqcQuantity(pqcDraft.scrapQuantity),
    signaturePassword: pqcSignaturePassword.value,
    nonconformanceDescription: normalizePqcDefectDescription(),
    itemResults: buildPqcItemResultsPayload(),
    rawPayload: {
      pqcDraft: {
        inspectionType: pqcDraft.inspectionType,
        patrolRound: pqcDraft.patrolRound,
        inspectionQuantity: normalizePqcQuantity(pqcDraft.inspectionQuantity),
        scrapQuantity: normalizePqcQuantity(pqcDraft.scrapQuantity),
        defectDescription: normalizePqcDefectDescription()
      },
      pqcPieceValues: buildPqcPieceValuesPayload(),
      pqcItemDetails,
      itemResults,
      fieldValues: { ...draft.fieldValues },
      inspectionResult,
      selectedActiveOrder: { ...activeOrder },
      selectedProcess: { ...process },
      selectedEmployee: { ...employee },
      templatePayload: validatedPayload
    },
    clientSubmitTime: formatLocalDateTime()
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

const resolvePqcResult = () => {
  if (normalizePqcQuantity(pqcDraft.scrapQuantity) > 0) {
    return FRONTLINE_PQC_RESULTS.DETECTION_FAILED
  }
  for (const itemKey of pqcInspectionItemKeys.value) {
    const item = pqcInspectionItemMap.value[itemKey]
    const values = getPqcCurrentChoiceValues(itemKey)
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

const normalizePqcDefectDescription = () => {
  const value = pqcDraft.defectDescription?.trim()
  return value || undefined
}

const validatePqcDefectDescription = () => {
  if (resolvePqcResult() !== FRONTLINE_PQC_RESULTS.DETECTION_FAILED) {
    return
  }
  if (!normalizePqcDefectDescription()) {
    const error = new Error('PQC检验不合格时必须手动填写不良说明。')
    message.error(error.message)
    throw error
  }
}

const applyActiveOrderToContext = (activeOrder: FrontlineActiveOrderVO) => {
  context.workOrderId = activeOrder.workOrderId
  context.routeId = activeOrder.routeId
}

const applyProcessToContext = (process: FrontlineDeviceRouteProcessVO) => {
  context.routeId = process.routeId
  context.routeProcessId = process.routeProcessId
  context.processId = process.processId
  if (isPqcMode.value) {
    applyPqcTaskSnapshotToDraft(process)
  }
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
  left?: FrontlineDeviceRouteProcessVO,
  right?: FrontlineDeviceRouteProcessVO
) =>
  Boolean(left && right) &&
  left.routeId === right.routeId &&
  left.routeProcessId === right.routeProcessId &&
  left.processId === right.processId

const isSameActiveOrder = (
  left?: FrontlineActiveOrderVO,
  right?: FrontlineActiveOrderVO
) =>
  Boolean(left && right) &&
  left.workOrderId === right.workOrderId &&
  left.routeId === right.routeId

const formatActiveOrderLabel = (activeOrder?: FrontlineActiveOrderVO) => {
  if (!activeOrder) {
    return '未选择'
  }
  return activeOrder.workOrderCode || activeOrder.workOrderName || `订单 ${activeOrder.workOrderId}`
}

const formatProcessLabel = (process?: FrontlineDeviceRouteProcessVO) => {
  if (!process) {
    return '未选择'
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
  const processes = await loadFrontlineDeviceProcesses(deviceState)
  const initialProcess = findInitialProcess(processes)
  if (initialProcess) {
    await handleSelectProcess(initialProcess)
  }
}

const resolveErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return '提交失败'
}

onMounted(async () => {
  document.addEventListener('fullscreenchange', syncPqcFullscreenState)
  window.addEventListener('resize', scheduleProductionViewportScaleUpdate)
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
    const initialActiveOrder = activeOrders.find((order) =>
      order.workOrderId === context.workOrderId &&
      (!context.routeId || order.routeId === context.routeId)
    ) || activeOrders[0]
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
})

onUnmounted(() => {
  resolveProductionFormalSubmitConfirmation(false)
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
  grid-template-rows: 130px 1fr 126px;
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
    grid-template-rows: minmax(118px, auto) minmax(0, 1fr) 104px;
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
  padding: 26px;
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
  width: auto;
  max-width: 1480px;
  height: auto;
  min-height: 820px;
  margin: 0 auto;
  grid-template-rows: minmax(118px, auto) minmax(0, 1fr) 104px;
  gap: 18px;
  padding: 24px;
  border-radius: 22px;
  box-shadow: 0 26px 70px rgba(36, 50, 43, 0.14);
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-operator-top.is-pqc,
.frontline-operator-panel:fullscreen .frontline-operator-top.is-pqc {
  grid-template-columns: minmax(480px, 1.55fr) minmax(220px, 0.85fr) minmax(200px, 1fr) 150px;
  gap: 12px;
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-operator-main.is-pqc,
.frontline-operator-panel:fullscreen .frontline-operator-main.is-pqc {
  grid-template-columns: minmax(760px, 1.72fr) minmax(390px, 0.78fr);
  gap: 28px;
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
  font-size: 28px;
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
  grid-template-columns: 280px minmax(0, 1fr);
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
  grid-template-columns: 1fr 1fr 240px;
  gap: 20px;

  &.is-pqc {
    grid-template-columns: minmax(480px, 1.55fr) minmax(220px, 0.85fr) minmax(200px, 1fr) 150px;
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

.frontline-top-card.is-login-employee {
  cursor: default;
  opacity: 1;
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
    grid-template-columns: minmax(700px, 1.55fr) minmax(430px, 0.95fr);
    gap: 28px;
  }
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
  grid-template-rows: auto auto auto minmax(0, 1fr);
  gap: 16px;
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
  grid-template-rows: auto 98px 1fr;
  gap: 18px;
  overflow: hidden;
}

.frontline-production-device-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  min-width: 0;

  button {
    min-width: 0;
    height: 98px;
    padding: 0;
    border: 3px solid var(--frontline-line);
    border-radius: 20px;
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

.frontline-production-device-current {
  display: grid;
  align-content: start;
  gap: 14px;
  min-width: 0;
  min-height: 0;
  padding: 18px;
  border: 3px solid var(--frontline-line);
  border-radius: 24px;
  background: #fbfdfb;
}

.frontline-production-device-empty {
  display: grid;
  grid-row: 2 / span 2;
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

.frontline-production-device-param {
  display: grid;
  grid-template-columns: 126px 70px minmax(0, 1fr) 70px 58px;
  gap: 10px;
  align-items: center;
  min-width: 0;

  label {
    font-size: 30px;
    font-weight: 900;
    line-height: 1.1;
  }

  button,
  input {
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

  &.is-submitted:disabled {
    background: #1f3a32;
    opacity: 1;
  }

  small {
    display: block;
    margin-top: 4px;
    font-size: 0.42em;
    font-weight: 700;
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

.frontline-production-stage .frontline-production-fullscreen-toggle {
  font-size: var(--frontline-production-top-action-font-size, 42px);
}

.frontline-production-stage .frontline-production-reset-button,
.frontline-production-stage .frontline-production-submit-button {
  font-size: var(--frontline-production-footer-action-font-size, 54px);
}

.frontline-pqc-inspection-list {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 0;
  min-height: 100%;
}

.frontline-pqc-content-panel {
  align-content: stretch;
  gap: 0;
}

.frontline-pqc-content-item {
  display: grid;
  align-content: start;
  gap: 10px;
  min-width: 0;
  padding-bottom: 16px;
  overflow: visible;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--frontline-ink);
}

.pqc-active-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px 16px;
  align-items: center;
  min-width: 0;

  h3 {
    margin: 0;
    min-width: 0;
    overflow: hidden;
    font-size: 38px;
    font-weight: 900;
    line-height: 1;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    padding: 8px 14px;
    border-radius: 999px;
    background: #edf3ef;
    color: #4b6258;
    font-size: 20px;
    font-weight: 900;
    white-space: nowrap;
  }

  small {
    grid-column: 1 / -1;
    min-width: 0;
    overflow: hidden;
    color: #4b6258;
    font-size: 20px;
    font-weight: 900;
    line-height: 1.25;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
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
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  align-items: start;
  min-height: 150px;
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
  grid-template-columns: minmax(0, 1fr) auto;
  grid-template-rows: auto auto;
  gap: 4px 8px;
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
  text-align: left;
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
    overflow: hidden;
    font-size: 24px;
    font-weight: 900;
    line-height: 1;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  em {
    display: inline-grid;
    place-items: center;
    min-width: 44px;
    height: 24px;
    padding: 0 8px;
    border-radius: 999px;
    background: #edf3ef;
    color: #4b6258;
    font-size: 14px;
    font-style: normal;
    font-weight: 900;
    white-space: nowrap;
  }

  small {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 6px;
    align-items: center;
    min-width: 0;
    overflow: visible;
    color: var(--frontline-muted);
    font-size: 13px;
    font-weight: 900;
    line-height: 1.05;
    white-space: nowrap;

    span {
      min-width: 0;
      overflow: visible;
      white-space: nowrap;
    }
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

    em {
      background: #f4d98d;
      color: #5a4311;
    }
  }

  &:focus-visible {
    outline: 5px solid #86c8ad;
    outline-offset: 2px;
  }
}

.frontline-pqc-fill-panel {
  grid-template-rows: 86px 104px minmax(0, 1fr);
  gap: 14px;
  overflow: hidden;
  padding: 18px;
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
  button {
    padding: 0 14px;
    font-size: 36px;
  }
}

.frontline-pqc-form-area {
  display: grid;
  align-content: start;
  gap: 12px;
  min-width: 0;
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

.frontline-pqc-defect-description {
  display: grid;
  grid-template-columns: 116px minmax(0, 1fr);
  gap: 8px 10px;
  align-items: start;
  min-width: 0;

  label {
    padding-top: 12px;
    font-size: 25px;
    font-weight: 900;
  }

  textarea {
    width: 100%;
    min-width: 0;
    min-height: 96px;
    box-sizing: border-box;
    border: 3px solid var(--frontline-line);
    border-radius: 18px;
    padding: 12px 14px;
    background: #ffffff;
    color: var(--frontline-ink);
    font: inherit;
    font-size: 24px;
    font-weight: 800;
    resize: vertical;
  }
}

.frontline-pqc-submit-bar {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 24px;
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

.frontline-pqc-submit-receipt {
  position: absolute;
  right: 32px;
  bottom: 128px;
  z-index: 45;
  display: grid;
  grid-template-columns: repeat(3, auto);
  gap: 8px 18px;
  max-width: calc(100% - 64px);
  padding: 18px 22px;
  border: 3px solid #15815f;
  border-radius: 8px;
  background: #ffffff;
  color: var(--frontline-ink);
  font-size: 18px;

  strong {
    color: #126d51;
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
  grid-auto-rows: minmax(100px, 1fr);
  gap: 10px;
  align-content: start;
  min-height: 0;
  padding-right: 8px;
  overflow-y: auto;
}

.frontline-pqc-piece-row {
  display: grid;
  grid-template-rows: 24px 52px;
  gap: 4px;
  align-items: center;
  min-width: 0;
  min-height: 100px;
  padding: 6px 10px;
  border: 3px solid var(--frontline-line);
  border-radius: 16px;
  background: #f8faf8;

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
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  min-width: 0;

  button {
    height: 56px;
    border: 3px solid var(--frontline-line);
    border-radius: 12px;
    background: #ffffff;
    color: var(--frontline-ink);
    font-size: 24px;
    font-weight: 900;
    cursor: pointer;

    &.pass.active {
      border-color: #86c8ad;
      background: #dff2ea;
      color: #15815f;
    }

    &.fail.active {
      border-color: #dfa8a2;
      background: #f8dfdc;
      color: #b9382f;
    }
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

  .frontline-pqc-choice-actions,
  .frontline-pqc-type-tabs,
  .frontline-pqc-round-tabs,
  .frontline-pqc-number-field,
  .frontline-pqc-defect-description,
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
