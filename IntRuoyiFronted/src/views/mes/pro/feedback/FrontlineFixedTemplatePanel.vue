<template>
  <section
    ref="frontlinePanelRef"
    class="frontline-operator-panel"
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
        <button class="frontline-top-card" type="button" @click="openPicker('order')">
          <span>生产订单</span>
          <strong class="frontline-top-card__order">{{ productionOrderLabel }}</strong>
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
        :aria-label="`${activePqcMethodItem.label}检验方法`"
        @click.self="closePqcMethodDialog"
      >
        <section>
          <h3>{{ activePqcMethodItem.label }}检验方法</h3>
          <p>{{ activePqcMethodItem.inspectionMethod || '未配置检验方法' }}</p>
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
              :aria-label="`${activePqcTabItem.label}检验详情`"
            >
              <div class="pqc-active-summary">
                <h3>{{ activePqcTabItem.label }}</h3>
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
              暂无检验项目
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
                <strong>{{ item.label }}</strong>
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

      <footer class="frontline-pqc-submit-bar">
        <button
          class="frontline-pqc-reset-button"
          type="button"
          @click="handleResetPqc"
        >
          重填
        </button>
        <button
          class="frontline-pqc-submit-button"
          type="button"
          :disabled="isSubmitBlocked"
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
                @click="adjustProductionOutputQuantity(-1)"
              >
                -
              </button>
              <input
                class="value-box"
                id="frontlineProductionOutputQuantity"
                :value="productionDraft.outputQuantity ?? ''"
                inputmode="numeric"
                @input="updateProductionOutputQuantity"
              />
              <button
                class="num-btn"
                type="button"
                aria-label="完成数量增加"
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
                    @click="adjustProductionDefectQuantity(defect.key, -1)"
                  >
                    -
                  </button>
                  <input
                    class="frontline-production-defect-qty defect-qty"
                    :value="getProductionDefectQuantity(defect.key)"
                    inputmode="numeric"
                    :aria-label="`${defect.label}数量`"
                    @input="updateProductionDefectQuantity(defect.key, $event)"
                  />
                  <button
                    type="button"
                    class="frontline-production-defect-step defect-step"
                    :aria-label="`${defect.label}增加`"
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
            <div class="frontline-production-device-tabs device-tabs" role="tablist" aria-label="设备切换">
              <button
                v-for="device in visibleDeviceCards"
                :key="device.key"
                class="device-tab"
                type="button"
                role="tab"
                :aria-selected="device.key === selectedProductionDeviceKey"
                :class="{ active: device.key === selectedProductionDeviceKey }"
                @click="selectedProductionDeviceKey = device.key"
              >
                {{ device.label }}
              </button>
            </div>
            <div v-if="activeProductionDevice" class="frontline-production-device-current device-current">
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
                <button
                  class="device-num"
                  type="button"
                  :aria-label="`${parameter.parameterName || parameter.parameterCode}减少`"
                  @click="adjustProductionDeviceParameter(activeProductionDevice.key, parameter.parameterCode, -1)"
                >
                  -
                </button>
                <input
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
                  @input="updateProductionDeviceParameter(activeProductionDevice.key, parameter.parameterCode, $event)"
                />
                <button
                  class="device-num"
                  type="button"
                  :aria-label="`${parameter.parameterName || parameter.parameterCode}增加`"
                  @click="adjustProductionDeviceParameter(activeProductionDevice.key, parameter.parameterCode, 1)"
                >
                  +
                </button>
                <span class="device-unit">{{ parameter.unit || '' }}</span>
              </div>
            </div>
          </section>
        </main>

        <footer class="frontline-production-submit-bar bottom">
          <button
            class="frontline-production-reset-button minor-btn"
            type="button"
            @click="handleResetProduction"
          >
            重填
          </button>
          <button
            class="frontline-production-submit-button submit-btn"
            type="button"
            :disabled="isSubmitBlocked || payloadLoading"
            @click="handleValidate"
          >
            {{ payloadLoading ? '提交中' : '提交' }}
          </button>
        </footer>
      </div>
    </div>

    <div v-if="activePicker && isPqcMode" class="frontline-picker picker" @click.self="closePicker">
      <section class="frontline-picker__card picker-card">
        <h3 class="frontline-picker__title picker-title">
          {{
            isPqcMode
              ? activePicker === 'order'
                ? '选择订单'
                : activePicker === 'process' ? '选工序' : '选择员工'
              : activePicker === 'process' ? '选工序' : '选择员工'
          }}
        </h3>
        <div class="frontline-picker__options picker-options">
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
  type FrontlineTemplatePayloadVO
} from '@/api/mes/pro/feedbackFrontlineTemplate'
import {
  ProFeedbackApi,
  type FrontlineActiveOrderVO,
  type FrontlineDeviceRouteProcessVO,
  type FrontlineEmployeeCandidateVO,
  type FrontlinePqcEquipmentOptionVO,
  type FrontlinePqcInspectionSubmitReqVO,
  type FrontlineRuntimeDeviceParameterVO,
  type ProFrontlineDeviceParameterReadingReqVO,
  type ProFrontlineFeedbackSubmitReqVO,
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
  createFrontlineDeviceEmployeeState,
  loadFrontlineDeviceProcesses,
  loadFrontlinePqcActiveOrders,
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
const activePicker = ref<PickerType>()
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
const pqcSignatureId = ref<number>()

const isPqcMode = computed(() => props.mode === 'pqc')
const PRODUCTION_CANVAS_WIDTH = 1920
const PRODUCTION_CANVAS_HEIGHT = 1080
const productionViewportScale = ref(1)
let productionViewportScaleFrame: number | undefined
let productionViewportResizeObserver: ResizeObserver | undefined
const productionStageStyle = computed(() => {
  const scale = productionViewportScale.value
  return {
    '--frontline-production-scale': String(scale),
    width: `${PRODUCTION_CANVAS_WIDTH * scale}px`,
    height: `${PRODUCTION_CANVAS_HEIGHT * scale}px`
  }
})
const currentLoginUserId = computed(() => Number(userStore.getUser?.id || 0))
const pqcProductionSubmitEventId = computed(() =>
  firstRouteQueryNumber(['productionSubmitEventId', 'processPoolEventId'])
)

const productionOrderLabel = computed(() => {
  const selectedOrder = deviceState.selectedActiveOrder
  return selectedOrder?.workOrderCode ||
    selectedOrder?.workOrderName ||
    firstRouteQueryText(['productionOrderCode', 'workOrderCode', 'orderCode']) ||
    '未选择订单'
})

const selectedProcessLabel = computed(() => formatProcessLabel(deviceState.selectedProcess))

const selectedEmployeeLabel = computed(() => formatEmployeeLabel(deviceState.selectedEmployee))

const productionScrapQuantity = computed(() =>
  configuredDefectReasons.value.reduce(
    (total, defect) => total + (productionDefectDraft[defect.key] || 0),
    0
  )
)

const pqcInspectionQuantity = computed(() =>
  normalizePqcQuantity(pqcDraft.inspectionQuantity)
)

const pqcInspectionItems = computed<PqcInspectionItem[]>(() =>
  (deviceState.selectedProcess?.inspectionItems || []).map((item) => ({
    key: item.itemCode,
    label: item.itemName || item.itemCode,
    type: isPqcNumericResultType(item.resultType) ? 'number' : 'choice',
    inspectionMethod: item.inspectionMethod || '',
    standardText: item.standardText || '',
    resultType: item.resultType || '',
    standardLowerLimit: item.standardLowerLimit,
    standardUpperLimit: item.standardUpperLimit,
    standardUnit: item.standardUnit || '',
    standardPrecision: item.standardPrecision,
    equipmentRequired: item.equipmentRequired !== false,
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

const isSubmitBlocked = computed(() =>
  payloadLoading.value ||
  templateModeMismatch.value ||
  templateBindingMissing.value ||
  (isPqcMode.value && !deviceState.selectedActiveOrder) ||
  (isPqcMode.value && !hasPqcTaskSnapshot(deviceState.selectedProcess)) ||
  (isPqcMode.value && !pqcProductionSubmitEventId.value) ||
  (isPqcMode.value && !pqcSignatureId.value) ||
  !deviceState.selectedProcess ||
  !deviceState.selectedEmployee
)

const statusText = computed(() => {
  if (deviceState.lastError) {
    return deviceState.lastError
  }
  if (isPqcMode.value && !deviceState.selectedActiveOrder) {
    return '请选择活跃订单'
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
  if (isPqcMode.value && !pqcProductionSubmitEventId.value) {
    return '缺少生产提交事件，无法提交PQC'
  }
  if (isPqcMode.value && !pqcSignatureId.value) {
    return '请填写签名编号'
  }
  if (templateBindingMissing.value) {
    return '当前员工缺少一线填写模板'
  }
  if (templateModeMismatch.value) {
    return `当前员工绑定的是${formatTemplateName(employeeTemplateCode.value)}，请切换${formatTemplateName(expectedTemplateCode.value)}员工`
  }
  return '准备提交'
})

const configuredDefectReasons = computed<ProductionDefectOption[]>(() =>
  (deviceState.runtimeConfig?.defectReasons || []).map((reason) => ({
    key: String(reason.reasonId),
    reasonId: reason.reasonId,
    reasonCode: reason.reasonCode,
    label: reason.reasonName || reason.reasonCode || `不良原因 ${reason.reasonId}`
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

const pickerOptions = computed(() => {
  if (activePicker.value === 'order') {
    return deviceState.activeOrderOptions.map((order) => ({
      key: `${order.workOrderId}-${order.routeId}`,
      label: formatActiveOrderLabel(order),
      active: isSameActiveOrder(order, deviceState.selectedActiveOrder),
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
        if (!parameter.parameterCode) {
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
  item.equipmentRequired ? '设备必填' : '设备选填'

const getPqcTabStateLabel = (item: PqcInspectionItem) => {
  if (activePqcTabKey.value === item.key) {
    return '当前'
  }
  const completedCount = getPqcCompletedCount(item.key)
  if (pqcInspectionQuantity.value > 0 && completedCount >= pqcInspectionQuantity.value) {
    return '完成'
  }
  if (item.equipmentRequired && !getPqcItemSelection(item.key).selectedEquipmentId) {
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
  return item.equipmentRequired ? '选择检验设备' : '无需指定设备'
}

const getPqcSelectedEquipmentNumberLabel = (item: PqcInspectionItem) =>
  getPqcItemSelection(item.key).selectedEquipmentNumber ||
  (item.equipmentRequired ? '选择设备编号' : '无需设备编号')

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

const formatPqcMethodSummary = (item: PqcInspectionItem) =>
  item.inspectionMethod || '未配置检验方法'

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
  if (item.equipmentRequired && !selection.selectedEquipmentId) {
    throw new Error(`${item.label}未选择检验设备。`)
  }
  if (item.equipmentRequired && !selection.selectedEquipmentNumber) {
    throw new Error(`${item.label}未选择设备编号。`)
  }
  const selectedOption = item.equipmentOptions.find((option) =>
    option.equipmentId === selection.selectedEquipmentId &&
    option.equipmentNumber === selection.selectedEquipmentNumber
  )
  if (item.equipmentRequired && !selectedOption) {
    throw new Error(`${item.label}设备编号不属于所选检验设备。`)
  }
  return { selection, selectedOption }
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
      itemName: item.label,
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

const updatePqcSignatureId = (event: Event) => {
  const inputValue = (event.target as HTMLInputElement).value
  const parsed = Number(inputValue)
  pqcSignatureId.value = Number.isFinite(parsed) && parsed > 0
    ? Math.trunc(parsed)
    : undefined
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
}

const closePicker = () => {
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

const handleProductionFullscreenToggle = async () => {
  try {
    if (isProductionFullscreen.value) {
      await exitProductionFullscreen()
      return
    }
    await enterProductionFullscreen()
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
  return processes[0]
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

const handleSelectActiveOrder = async (activeOrder: FrontlineActiveOrderVO) => {
  const processes = await selectFrontlinePqcActiveOrder(deviceState, activeOrder)
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
  if (isPqcMode.value) {
    await selectFrontlinePqcProcess(deviceState, process)
  } else {
    await selectFrontlineProcess(deviceState, process)
  }
  applyProcessToContext(process)
  employeeTemplateCode.value = undefined
  payloadPreview.value = undefined
  const initialEmployee = findInitialEmployee()
  if (initialEmployee) {
    await handleSelectEmployee(initialEmployee)
  } else if (isPqcMode.value) {
    const error = new Error('当前登录账号未返回PQC人员候选，无法进入PQC填写。')
    message.error(error.message)
    throw error
  }
  closePicker()
}

const handleSelectEmployee = async (employee: FrontlineEmployeeCandidateVO) => {
  if (isPqcMode.value && !isCurrentLoginEmployee(employee)) {
    const error = new Error('一线PQC员工已锁定为当前登录账号，不能切换。')
    message.error(error.message)
    throw error
  }
  const result = isPqcMode.value
    ? await switchFrontlinePqcActualEmployee(deviceState, employee.userId)
    : await switchFrontlineActualEmployee(deviceState, employee.userId)
  context.actualEmployeeId = result.actualEmployeeId
  const templateCode = resolveTemplateCode(result.template?.templateNo, result.template?.templateType)
  employeeTemplateCode.value = templateCode
  payloadPreview.value = undefined
  closePicker()
}

const handleValidate = async () => {
  if (templateBindingMissing.value) {
    const error = new Error('当前员工缺少一线填写模板，无法提交。')
    message.error(error.message)
    throw error
  }
  if (templateModeMismatch.value) {
    const error = new Error(statusText.value)
    message.error(error.message)
    throw error
  }
  if (isPqcMode.value) {
    assertPqcSubmissionSampleQuantities()
    validatePqcDefectDescription()
  }
  Object.assign(
    draft.fieldValues,
    isPqcMode.value ? buildPqcFieldValues() : buildProductionFieldValues()
  )
  payloadLoading.value = true
  try {
    assertFormalPayloadContext()
    const templatePayload = buildFrontlineTemplatePayload(context, draft.fieldValues)
    payloadPreview.value = await FrontlineTemplateApi.validatePayload(templatePayload)
    if (isPqcMode.value) {
      await ProFeedbackApi.submitFrontlinePqcInspection(
        buildPqcInspectionSubmitPayload(payloadPreview.value)
      )
      message.success('已提交')
    } else {
      await ProFeedbackApi.frontlineSubmit(
        buildFrontlineFormalSubmitPayload(payloadPreview.value)
      )
      message.success('已提交')
    }
  } catch (error) {
    message.error(resolveErrorMessage(error))
    throw error
  } finally {
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
  if (isPqcMode.value && !pqcSignatureId.value) {
    missingFields.push('签名编号')
  }
  if (missingFields.length) {
    throw new Error(`缺少${missingFields.join('、')}，无法提交。`)
  }
}

interface FrontlineFormalSubmitContext {
  feedbackCode?: string
  feedbackType?: number
  workOrderId?: number
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
  signatureId?: number
  signatureEmployeeId?: number
  scheduleOrderId?: number
  scheduleOrderProcessId?: number
  scheduledQuantity?: number
  expireDate?: string
}

const readFrontlineFormalSubmitContext = (): FrontlineFormalSubmitContext => {
  const selectedProcess = deviceState.selectedProcess
  return {
    feedbackCode: firstRouteQueryText(['feedbackCode', 'feedbackNo', 'code']),
    feedbackType: firstRouteQueryNumber(['feedbackType', 'type']),
    workOrderId: context.workOrderId,
    taskId: firstRouteQueryNumber(['taskId', 'productionTaskId']),
    routeId: context.routeId,
    routeProcessId: context.routeProcessId,
    processId: context.processId,
    workstationId: selectedProcess?.workstationId ?? firstRouteQueryNumber(['workstationId']),
    deviceId: activeProductionDevice.value?.key
      ? Number(activeProductionDevice.value.key)
      : selectedProcess?.deviceId,
    deviceAccountUserId: Number(userStore.getUser?.id || 0),
    itemId: firstRouteQueryNumber(['itemId', 'productItemId']),
    approveUserId: firstRouteQueryNumber(['approveUserId', 'teamLeaderUserId']),
    recordbookId: firstRouteQueryNumber(['recordbookId', 'frontlineRecordbookId']),
    signatureId: firstRouteQueryNumber(['signatureId']),
    signatureEmployeeId: firstRouteQueryNumber(['signatureEmployeeId']),
    scheduleOrderId: firstRouteQueryNumber(['scheduleOrderId']),
    scheduleOrderProcessId: firstRouteQueryNumber(['scheduleOrderProcessId']),
    scheduledQuantity: firstRouteQueryNumber(['scheduledQuantity']),
    expireDate: firstRouteQueryText(['expireDate'])
  }
}

const assertFrontlineFormalSubmitContext = (formalContext: FrontlineFormalSubmitContext) => {
  const missingFields: string[] = []
  const requiredFields: Array<[keyof FrontlineFormalSubmitContext, string]> = [
    ['feedbackCode', '报工单编号'],
    ['feedbackType', '报工类型'],
    ['workOrderId', '订单上下文'],
    ['taskId', '生产任务'],
    ['routeId', '路线'],
    ['routeProcessId', '路线工序'],
    ['processId', '工序'],
    ['workstationId', '工作站'],
    ['deviceId', '设备'],
    ['deviceAccountUserId', '设备账号'],
    ['itemId', '产品物料'],
    ['approveUserId', '班组长审批人'],
    ['recordbookId', '记录本'],
    ['signatureId', '签名'],
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
  if (missingFields.length) {
    throw new Error(`缺少${missingFields.join('、')}，无法提交。`)
  }
}

const buildFrontlineFormalSubmitPayload = (
  rawPayload: FrontlineTemplatePayloadVO
): ProFrontlineFeedbackSubmitReqVO => {
  const formalContext = readFrontlineFormalSubmitContext()
  assertFrontlineFormalSubmitContext(formalContext)
  const selectedDevice = activeProductionDevice.value
  const equipmentParameters = selectedDevice
    ? { [selectedDevice.label]: buildProductionDeviceParameterPayload(selectedDevice.key) }
    : {}
  return {
    feedbackPayload: {
      code: formalContext.feedbackCode!,
      type: formalContext.feedbackType!,
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
        `一线报工-${formalContext.feedbackCode}`,
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
        firstRouteQueryText(['idempotencyKey']) ||
        `frontline-submit-${formalContext.signatureId}`,
      remark: firstRouteQueryText(['recordbookRemark'])
    },
    processPoolSubmissionIdempotencyKey:
      firstRouteQueryText(['processPoolSubmissionIdempotencyKey']) ||
      `frontline-process-pool-${formalContext.signatureId}-${formalContext.routeProcessId}`,
    processPoolContext: {
      workOrderId: formalContext.workOrderId!,
      taskId: formalContext.taskId!,
      routeId: formalContext.routeId!,
      routeProcessId: formalContext.routeProcessId!,
      processId: formalContext.processId!,
      workstationId: formalContext.workstationId!,
      deviceId: formalContext.deviceId!,
      deviceAccountUserId: formalContext.deviceAccountUserId!,
      templateType: context.templateCode || expectedTemplateCode.value
    },
    actualEmployeeId: context.actualEmployeeId!,
    signatureId: formalContext.signatureId!,
    signatureEmployeeId: formalContext.signatureEmployeeId!,
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
          lowerLimit: parameter.lowerLimit,
          upperLimit: parameter.upperLimit,
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
        valueType: parameter.valueType
      }))
    ]])
    : {}

const buildProductionStructuredRawPayload = (rawPayload: FrontlineTemplatePayloadVO) => ({
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
  const signatureId = pqcSignatureId.value
  const productionSubmitEventId = pqcProductionSubmitEventId.value
  const missingFormalContext: string[] = []
  if (!productionSubmitEventId) {
    missingFormalContext.push('productionSubmitEventId')
  }
  if (!activeOrder || !process || !employee || !actualEmployeeId || !signatureId ||
    !hasPqcTaskSnapshot(process) || !pqcDraft.inspectionType || !pqcDraft.patrolRound ||
    missingFormalContext.length) {
    throw new Error(`缺少PQC正式提交上下文：${missingFormalContext.join('、')}，无法提交。`)
  }
  const inspectionResult = resolvePqcResult()
  const itemResults = buildPqcItemResultsPayload()
  const pqcItemDetails = buildPqcItemDetailsPayload()
  const pqcSubmissionIdempotencyKey =
    firstRouteQueryText(['pqcSubmissionIdempotencyKey']) ||
    `pqc-submit-${process.pqcTaskId}-${signatureId}`
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
    actualEmployeeId,
    pqcSubmissionIdempotencyKey,
    signatureId,
    signatureEmployeeId: actualEmployeeId,
    signatureSnapshot: firstRouteQueryText(['pqcSignatureSnapshot', 'signatureSnapshot']),
    templateType: deviceState.template?.templateNo ||
      employeeTemplateCode.value ||
      context.templateCode ||
      expectedTemplateCode.value,
    inspectionResult,
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
    const values = getPqcExactPieceValuesForSubmit(itemKey)
    if (values.some((value) => value === '不合格')) {
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
  pqcSignatureId.value = firstRouteQueryNumber(['signatureId']) ?? pqcSignatureId.value
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
  const orderText = activeOrder.workOrderCode || activeOrder.workOrderName || `订单 ${activeOrder.workOrderId}`
  const productText = activeOrder.productCode || activeOrder.productName
  const routeText = activeOrder.routeName || activeOrder.routeCode
  return [orderText, productText, routeText].filter(Boolean).join(' / ')
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
  catalog.value = await FrontlineTemplateApi.getCatalog()
  if (isPqcMode.value) {
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
  await loadFrontlineDeviceProcesses(deviceState)
  const initialProcess = findInitialProcess()
  if (initialProcess) {
    await handleSelectProcess(initialProcess)
  }
  Object.assign(draft.fieldValues, buildProductionFieldValues())
})

onUnmounted(() => {
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

.frontline-operator-screen {
  --frontline-bg: #eef3ef;
  --frontline-panel: #ffffff;
  --frontline-ink: #111a15;
  --frontline-muted: #5b665f;
  --frontline-line: #cbd6ce;
  --frontline-dark: #24322b;
  display: grid;
  width: min(100%, 1600px);
  min-height: min(1080px, calc(100vh - 144px));
  box-sizing: border-box;
  grid-template-rows: auto minmax(0, 1fr) 126px;
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
    width: auto;
    height: auto;
    grid-template-rows: 118px minmax(0, 1fr) 104px;
    min-height: 820px;
  }
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
  grid-template-rows: 118px minmax(0, 1fr) 104px;
  gap: 18px;
  padding: 24px;
  border-radius: 22px;
  box-shadow: 0 26px 70px rgba(36, 50, 43, 0.14);
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-operator-top.is-pqc,
.frontline-operator-panel:fullscreen .frontline-operator-top.is-pqc {
  grid-template-columns: 340px 430px minmax(0, 1fr) 210px;
  gap: 18px;
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-operator-main.is-pqc,
.frontline-operator-panel:fullscreen .frontline-operator-main.is-pqc {
  grid-template-columns: minmax(760px, 1.72fr) minmax(390px, 0.78fr);
  gap: 28px;
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-operator-screen.is-pqc .frontline-top-card,
.frontline-operator-panel:fullscreen .frontline-operator-screen.is-pqc .frontline-top-card {
  padding: 18px 22px;

  span {
    font-size: 22px;
  }

  strong {
    margin-top: 10px;
    font-size: 34px;
  }
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-operator-screen.is-pqc .frontline-top-card__order,
.frontline-operator-panel:fullscreen .frontline-operator-screen.is-pqc .frontline-top-card__order {
  font-size: 34px !important;
}

.frontline-operator-panel.is-pqc-fullscreen .frontline-operator-screen.is-pqc .frontline-home-button,
.frontline-operator-panel:fullscreen .frontline-operator-screen.is-pqc .frontline-home-button {
  font-size: 38px;
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
    grid-template-columns: 340px 430px minmax(0, 1fr) 210px;
    gap: 18px;
  }
}

.frontline-operator-top.is-production {
  width: min(100%, 68vw);
  max-width: 1280px;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(132px, 0.58fr);
  justify-self: center;
  align-self: start;

  .frontline-production-selection-card,
  .frontline-production-fullscreen-toggle {
    aspect-ratio: 1920 / 1080;
    min-height: 0;
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

.frontline-top-card__order {
  font-size: 32px !important;
}

.frontline-operator-top.is-pqc {
  .frontline-top-card {
    padding: 18px 22px;

    span {
      font-size: 22px;
      font-weight: 800;
    }

    strong {
      margin-top: 10px;
      font-size: 34px;
    }
  }

  .frontline-top-card__order {
    font-size: 34px !important;
  }

  .frontline-home-button {
    font-size: 38px;
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
  gap: 24px;
  min-width: 0;
  min-height: 0;
  padding: 26px;
  border: 3px solid var(--frontline-line);
  border-radius: 24px;
  background: #fbfdfb;
}

.frontline-production-device-param {
  display: grid;
  grid-template-columns: 150px 82px minmax(0, 1fr) 82px 78px;
  gap: 14px;
  align-items: center;
  min-width: 0;

  label {
    font-size: 38px;
    font-weight: 900;
  }

  button,
  input {
    width: 100%;
    height: 96px;
    min-width: 0;
    border: 3px solid var(--frontline-line);
    border-radius: 16px;
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

    &.is-parameter-out-of-range {
      border-color: #dc2626;
      background: #fff1f2;
      color: #b91c1c;
    }
  }

  span {
    font-size: 34px;
    font-weight: 900;
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
  width: 760px;
  padding: 28px;
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
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  max-height: none;
  overflow: visible;
}

.frontline-operator-panel.is-production-mode .frontline-picker__option {
  height: 112px;
  min-height: 0;
  border: 3px solid var(--frontline-line);
  border-radius: 22px;
  background: #f8faf8;
  color: var(--frontline-ink);
  font-size: 42px;
  font-weight: 900;
}

.frontline-operator-panel.is-production-mode .frontline-picker__option.active {
  border-color: var(--frontline-dark);
  background: var(--frontline-dark);
  color: #ffffff;
}

.frontline-operator-panel.is-production-mode .frontline-picker__close {
  height: 86px;
  border: 3px solid var(--frontline-line);
  border-radius: 22px;
  background: #f8faf8;
  color: var(--frontline-ink);
  font-size: 36px;
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
