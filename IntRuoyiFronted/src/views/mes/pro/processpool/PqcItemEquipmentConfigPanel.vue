<template>
  <section class="pqc-item-equipment-config" data-pqc-item-equipment-config-panel>
    <header class="pqc-item-equipment-config__header">
      <div>
        <div class="pqc-item-equipment-config__title">检验设备</div>
        <div class="pqc-item-equipment-config__hint">
          当前项目：{{ props.projectName }}；配置在当前租户内共用。
        </div>
      </div>
      <el-button type="primary" :loading="saving" :disabled="!selectedItemCode" @click="saveConfig">
        保存配置
      </el-button>
    </header>

    <el-alert
      v-if="loadError"
      class="mb-12px"
      :title="loadError"
      type="error"
      :closable="false"
      show-icon
    />

    <el-alert
      v-if="selectedItem && configurationConsistent === false"
      class="mb-12px"
      title="同名检验项目的设备配置当前不一致，保存后将统一覆盖该名称对应的全部项目编号。"
      type="warning"
      :closable="false"
      show-icon
    />

    <div class="pqc-item-equipment-config__selector">
      <el-form label-width="96px">
        <el-form-item label="检验项目">
          <el-select
            v-model="selectedItemCode"
            filterable
            class="pqc-item-equipment-config__item-select"
            placeholder="请选择检验名称"
            :loading="itemsLoading"
            data-pqc-item-equipment-item-select
            @change="handleItemChange"
          >
            <el-option
              v-for="item in filteredItems"
              :key="item.itemCode"
              :label="item.itemName"
              :value="item.itemCode"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div v-if="selectedItem" class="pqc-item-equipment-config__item-meta">
        <span>项目名称：{{ selectedItem.projectName }}</span>
        <span>检验名称：{{ selectedItem.itemName }}</span>
        <span v-if="selectedItem.inspectionMethod">检验方法：{{ selectedItem.inspectionMethod }}</span>
        <span v-if="selectedItem.samplingPlanText">抽样规则：{{ selectedItem.samplingPlanText }}</span>
      </div>
    </div>

    <div v-if="selectedItemCode" class="pqc-item-equipment-config__body" v-loading="configLoading">
      <div class="pqc-item-equipment-config__toolbar">
        <span>检验项目 -> 检验设备 -> 设备编号</span>
        <el-button type="primary" plain @click="addEquipmentGroup">
          <Icon icon="ep:plus" class="mr-5px" />
          新增检验设备
        </el-button>
      </div>

      <el-empty
        v-if="!draftGroups.length"
        description="当前检验项目未配置检验设备；一线 PQC 不需要选择设备。"
      />

      <div
        v-for="(group, groupIndex) in draftGroups"
        :key="group.localKey"
        class="pqc-item-equipment-config__group"
        data-pqc-item-equipment-group
      >
        <div class="pqc-item-equipment-config__group-head">
          <el-form label-width="88px" class="pqc-item-equipment-config__group-form">
            <el-form-item label="检验设备">
              <el-select
                v-model="group.equipmentId"
                filterable
                remote
                clearable
                reserve-keyword
                placeholder="选择设备台账"
                :remote-method="searchMachinery"
                :loading="machineryLoading"
                class="pqc-item-equipment-config__equipment-select"
                data-pqc-item-equipment-device-select
                @focus="ensureMachineryOptions"
                @change="handleEquipmentChange(group)"
              >
                <el-option
                  v-for="machinery in machineryOptions"
                  :key="machinery.id"
                  :label="formatMachineryLabel(machinery)"
                  :value="machinery.id"
                />
              </el-select>
            </el-form-item>
          </el-form>
          <div class="pqc-item-equipment-config__group-actions">
            <el-switch
              v-model="group.enabled"
              active-text="启用"
              inactive-text="停用"
              inline-prompt
            />
            <el-button link type="danger" @click="removeEquipmentGroup(groupIndex)">删除</el-button>
          </div>
        </div>

        <div class="pqc-item-equipment-config__numbers">
          <div class="pqc-item-equipment-config__numbers-head">
            <strong>设备编号</strong>
            <el-button link type="primary" @click="addEquipmentNumber(group)">
              <Icon icon="ep:plus" class="mr-5px" />
              新增编号
            </el-button>
          </div>
          <div
            v-for="(numberRow, numberIndex) in group.equipmentNumbers"
            :key="numberRow.localKey"
            class="pqc-item-equipment-config__number-row"
          >
            <el-input
              v-model="numberRow.equipmentNumber"
              placeholder="例如 FM001"
              maxlength="64"
              data-pqc-item-equipment-number-input
            />
            <el-switch
              v-model="numberRow.enabled"
              active-text="启用"
              inactive-text="停用"
              inline-prompt
            />
            <el-button link type="danger" @click="removeEquipmentNumber(group, numberIndex)">
              删除
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DvMachineryApi, type DvMachineryVO } from '@/api/mes/dv/machinery'
import {
  QcTemplateApi,
  type PqcItemEquipmentConfigVO,
  type PqcItemEquipmentItemVO
} from '@/api/mes/qc/template'

defineOptions({ name: 'PqcItemEquipmentConfigPanel' })

const props = defineProps<{
  dccProjectCodeId: number
  projectName: string
}>()

type DraftNumber = {
  localKey: string
  id?: number
  equipmentNumber: string
  enabled: boolean
  sort?: number
}

type DraftGroup = {
  localKey: string
  id?: number
  equipmentId?: number
  equipmentCode?: string
  equipmentName?: string
  enabled: boolean
  defaultFlag?: boolean
  sort?: number
  equipmentNumbers: DraftNumber[]
}

const items = ref<PqcItemEquipmentItemVO[]>([])
const selectedItemCode = ref('')
const draftGroups = ref<DraftGroup[]>([])
const machineryOptions = ref<DvMachineryVO[]>([])
const itemsLoading = ref(false)
const configLoading = ref(false)
const machineryLoading = ref(false)
const saving = ref(false)
const loadError = ref('')
const configurationConsistent = ref(true)
let itemsLoadSerial = 0

const filteredItems = computed(() => {
  const groupedByItemName = new Map<string, PqcItemEquipmentItemVO>()
  items.value
    .filter((item) => item.dccProjectCodeId === props.dccProjectCodeId)
    .forEach((item) => {
      const itemCodes = Array.from(new Set([...(item.itemCodes || []), item.itemCode]))
      const existing = groupedByItemName.get(item.itemName)
      if (!existing) {
        groupedByItemName.set(item.itemName, { ...item, itemCodes })
      } else {
        existing.itemCodes = Array.from(new Set([...(existing.itemCodes || []), ...itemCodes]))
      }
    })
  return Array.from(groupedByItemName.values())
})

const selectedItem = computed(() =>
  filteredItems.value.find((item) => item.itemCode === selectedItemCode.value)
)

const createLocalKey = () => `${Date.now()}-${Math.random().toString(36).slice(2)}`

const formatMachineryLabel = (machinery: DvMachineryVO) =>
  [machinery.code, machinery.name].filter(Boolean).join(' / ')

const mergeMachineryOptions = (pages: Array<{ list?: DvMachineryVO[] }>) => {
  const optionMap = new Map<number, DvMachineryVO>()
  pages.forEach((page) => {
    ;(page?.list || []).forEach((machinery) => {
      if (machinery?.id) {
        optionMap.set(machinery.id, machinery)
      }
    })
  })
  return Array.from(optionMap.values())
}

const toDraftGroups = (config?: PqcItemEquipmentConfigVO): DraftGroup[] =>
  (config?.equipmentGroups || []).map((group) => ({
    localKey: createLocalKey(),
    id: group.id,
    equipmentId: group.equipmentId,
    equipmentCode: group.equipmentCode,
    equipmentName: group.equipmentName,
    enabled: group.enabled !== false,
    defaultFlag: group.defaultFlag === true,
    sort: group.sort,
    equipmentNumbers: (group.equipmentNumbers || []).map((number) => ({
      localKey: createLocalKey(),
      id: number.id,
      equipmentNumber: number.equipmentNumber,
      enabled: number.enabled !== false,
      sort: number.sort
    }))
  }))

const loadItems = async () => {
  const loadSerial = ++itemsLoadSerial
  itemsLoading.value = true
  loadError.value = ''
  try {
    const nextItems = await QcTemplateApi.getPqcItemEquipmentItems(props.dccProjectCodeId)
    if (loadSerial === itemsLoadSerial) {
      items.value = nextItems
    }
  } catch (error) {
    if (loadSerial === itemsLoadSerial) {
      loadError.value = '检验项目列表加载失败，请稍后重试。'
    }
    throw error
  } finally {
    if (loadSerial === itemsLoadSerial) {
      itemsLoading.value = false
    }
  }
}

const loadConfig = async (itemCode: string) => {
  if (!itemCode) return
  configLoading.value = true
  loadError.value = ''
  configurationConsistent.value = true
  try {
    const itemCodes = selectedItem.value?.itemCodes || [itemCode]
    const config = await QcTemplateApi.getPqcItemEquipmentConfigBatch(
      props.dccProjectCodeId,
      itemCodes
    )
    configurationConsistent.value = config.configurationConsistent !== false
    draftGroups.value = toDraftGroups(config)
  } catch (error) {
    loadError.value = '检验设备配置加载失败，请稍后重试。'
    throw error
  } finally {
    configLoading.value = false
  }
}

const resetSelectedItemState = () => {
  selectedItemCode.value = ''
  draftGroups.value = []
  configurationConsistent.value = true
  loadError.value = ''
}

const handleItemChange = async (itemCode: string) => {
  draftGroups.value = []
  await loadConfig(itemCode)
}

const searchMachinery = async (keyword = '') => {
  machineryLoading.value = true
  const normalizedKeyword = keyword.trim()
  try {
    if (!normalizedKeyword) {
      const page = await DvMachineryApi.getMachineryPage({
        pageNo: 1,
        pageSize: 50
      })
      machineryOptions.value = Array.isArray(page?.list) ? page.list : []
      return
    }
    const pages = await Promise.all([
      DvMachineryApi.getMachineryPage({
        pageNo: 1,
        pageSize: 50,
        code: normalizedKeyword
      }),
      DvMachineryApi.getMachineryPage({
        pageNo: 1,
        pageSize: 50,
        name: normalizedKeyword
      })
    ])
    machineryOptions.value = mergeMachineryOptions(pages)
  } catch (error) {
    ElMessage.error('设备台账加载失败，请稍后重试。')
    throw error
  } finally {
    machineryLoading.value = false
  }
}

const ensureMachineryOptions = async () => {
  if (!machineryOptions.value.length) {
    await searchMachinery('')
  }
}

const handleEquipmentChange = (group: DraftGroup) => {
  const selected = machineryOptions.value.find((item) => item.id === group.equipmentId)
  group.equipmentCode = selected?.code
  group.equipmentName = selected?.name
}

const addEquipmentGroup = async () => {
  await ensureMachineryOptions()
  draftGroups.value.push({
    localKey: createLocalKey(),
    enabled: true,
    defaultFlag: false,
    sort: draftGroups.value.length,
    equipmentNumbers: [
      {
        localKey: createLocalKey(),
        equipmentNumber: '',
        enabled: true,
        sort: 0
      }
    ]
  })
}

const removeEquipmentGroup = (index: number) => {
  draftGroups.value.splice(index, 1)
}

const addEquipmentNumber = (group: DraftGroup) => {
  group.equipmentNumbers.push({
    localKey: createLocalKey(),
    equipmentNumber: '',
    enabled: true,
    sort: group.equipmentNumbers.length
  })
}

const removeEquipmentNumber = (group: DraftGroup, index: number) => {
  group.equipmentNumbers.splice(index, 1)
}

const buildSavePayload = () => {
  if (!selectedItemCode.value) {
    throw new Error('请选择检验项目。')
  }
  return {
    dccProjectCodeId: props.dccProjectCodeId,
    itemCode: selectedItemCode.value,
    itemCodes: selectedItem.value?.itemCodes || [selectedItemCode.value],
    itemNameSnapshot: selectedItem.value?.itemName,
    equipmentGroups: draftGroups.value.map((group, groupIndex) => {
      if (!group.equipmentId) {
        throw new Error('检验设备不能为空。')
      }
      const equipmentNumbers = group.equipmentNumbers
        .map((numberRow, numberIndex) => ({
          id: numberRow.id,
          equipmentNumber: numberRow.equipmentNumber.trim(),
          enabled: numberRow.enabled,
          sort: numberRow.sort ?? numberIndex
        }))
        .filter((numberRow) => Boolean(numberRow.equipmentNumber))
      if (!equipmentNumbers.length) {
        throw new Error('每个检验设备至少需要一个设备编号。')
      }
      return {
        id: group.id,
        equipmentId: group.equipmentId,
        equipmentCode: group.equipmentCode,
        equipmentName: group.equipmentName,
        enabled: group.enabled,
        defaultFlag: group.defaultFlag === true,
        sort: group.sort ?? groupIndex,
        equipmentNumbers
      }
    })
  }
}

const saveConfig = async () => {
  let payload
  try {
    payload = buildSavePayload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '检验设备配置不完整。')
    return
  }
  saving.value = true
  try {
    const saved = await QcTemplateApi.savePqcItemEquipmentConfigBatch(payload)
    configurationConsistent.value = saved.configurationConsistent !== false
    draftGroups.value = toDraftGroups(saved)
    ElMessage.success('检验设备配置已保存')
  } catch (error) {
    ElMessage.error('检验设备配置保存失败，请稍后重试。')
    throw error
  } finally {
    saving.value = false
  }
}

watch(
  () => props.dccProjectCodeId,
  async () => {
    resetSelectedItemState()
    await loadItems()
  },
  { immediate: true }
)

onMounted(async () => {
  await ensureMachineryOptions()
})
</script>

<style scoped>
.pqc-item-equipment-config {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.pqc-item-equipment-config__header,
.pqc-item-equipment-config__toolbar,
.pqc-item-equipment-config__group-head,
.pqc-item-equipment-config__numbers-head,
.pqc-item-equipment-config__number-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pqc-item-equipment-config__title {
  color: #1f2937;
  font-size: 16px;
  font-weight: 700;
}

.pqc-item-equipment-config__hint,
.pqc-item-equipment-config__item-meta {
  color: #6b7280;
  font-size: 13px;
}

.pqc-item-equipment-config__selector,
.pqc-item-equipment-config__group {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px;
  background: #fff;
}

.pqc-item-equipment-config__item-select,
.pqc-item-equipment-config__project-select,
.pqc-item-equipment-config__equipment-select {
  width: 100%;
}

.pqc-item-equipment-config__item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-left: 96px;
}

.pqc-item-equipment-config__body,
.pqc-item-equipment-config__numbers {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pqc-item-equipment-config__group-form {
  flex: 1;
}

.pqc-item-equipment-config__group-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.pqc-item-equipment-config__group-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pqc-item-equipment-config__number-row {
  justify-content: flex-start;
}

.pqc-item-equipment-config__number-row .el-input {
  max-width: 320px;
}
</style>
