<template>
  <el-collapse v-if="readonly && model.historicalSheet">
    <el-collapse-item :title="'历史导入 · ' + model.historicalSheet + ' 第 ' + model.historicalRow + ' 行'">
      <el-descriptions :column="2" border>
        <el-descriptions-item v-for="(value, label) in model.historicalValues" :key="label" :label="String(label)">{{ value }}</el-descriptions-item>
      </el-descriptions>
    </el-collapse-item>
  </el-collapse>
  <el-row :gutter="24" class="certificate-facts-fields">
    <el-col v-if="!creationSection" :span="12"><el-form-item label="注册证类型" required><el-select v-model="model.certificateType" :disabled="readonly || lockType || lockedFields?.includes('certificateType')"><el-option label="国内" value="DOMESTIC"/><el-option label="CE" value="CE"/><el-option label="FDA" value="FDA"/></el-select></el-form-item></el-col>
    <el-col v-if="!creationSection" :span="12"><el-form-item label="产品归属" required><el-select v-model="model.ownProduct" :disabled="readonly || lockType || lockedFields?.includes('ownProduct')"><el-option label="瑛泰产品（关联系统产品）" :value="true"/><el-option label="其他（不关联系统产品）" :value="false"/></el-select></el-form-item></el-col>
    <el-col v-for="field in visibleFields" :key="field" :span="['remark','changeDescription','registrationLink'].includes(field) ? 24 : 12" :xs="creationSection ? 24 : undefined" :sm="['remark','changeDescription','registrationLink'].includes(field) ? 24 : 12" :md="['remark','changeDescription','registrationLink'].includes(field) ? 24 : creationSection ? 8 : 12">
      <el-form-item :required="!!creationSection && requiredCreationFields.includes(field)" :label="historicalRecord && field === 'certificateStatus' ? '证载状态（历史记录）' : readonly && field === 'certificateStatus' ? '证载状态（原记录）' : historicalRecord && field === 'expiryDate' ? '原有效期至' : factLabels[field]">
        <span v-if="readonly && field === 'certificateStatus' && visualState === 'EXPIRED'">已过期（原记录：{{ factOptions(field,model.certificateType).find(option => option.value === model[field])?.label || '未记录' }}）</span>
        <a v-else-if="readonly && field === 'registrationLink' && model[field]" :href="model[field]" target="_blank" rel="noopener noreferrer">{{ model[field] }}</a>
        <el-select v-else-if="factOptions(field,model.certificateType).length" v-model="model[field]" :disabled="readonly || lockedFields?.includes(field)" clearable><el-option v-for="option in factOptions(field,model.certificateType)" :key="String(option.value)" :label="option.label" :value="option.value"/></el-select>
        <el-date-picker v-else-if="field.endsWith('Date')" v-model="model[field]" :disabled="readonly || lockedFields?.includes(field)" type="date" value-format="YYYY-MM-DD" format="YYYY-MM-DD" clearable/>
        <el-input v-else v-model="model[field]" :readonly="readonly || lockedFields?.includes(field)" :type="['remark','changeDescription','otherContentDescription'].includes(field) ? 'textarea' : 'text'" :maxlength="field === 'remark' ? 1024 : 4096"/>
      </el-form-item>
    </el-col>
    <el-col v-if="model.certificateType === 'CE' && !creation && !hideCeModels" :span="24">
      <el-form-item label="型号规格">
        <div class="w-full">
          <div v-for="(_item, index) in model.modelSpecifications" :key="index" class="flex gap-8px mb-8px">
            <el-input v-model="model.modelSpecifications![index]" :readonly="readonly || lockedFields?.includes('modelSpecifications')" />
            <el-button v-if="!readonly && !lockedFields?.includes('modelSpecifications')" type="danger" @click="model.modelSpecifications?.splice(index, 1)">删除</el-button>
          </div>
          <el-button v-if="!readonly && !lockedFields?.includes('modelSpecifications')" @click="addModelSpecification">添加型号</el-button>
        </div>
      </el-form-item>
    </el-col>
    <el-col v-if="model.certificateType === 'CE' && (!creationSection || creationSection === 'certificate')" :span="24">
      <el-form-item label="证书Rev.记录">
        <div class="w-full"><el-table :data="model.ceCertificates ?? []" border>
          <el-table-column label="Rev." min-width="100"><template #default="{ row }"><el-input v-model="row.revision" :readonly="readonly || lockedFields?.includes('ceCertificates')"/></template></el-table-column>
          <el-table-column label="证书编号（可留空）" min-width="160"><template #default="{ row }"><el-input v-model="row.certificateNo" :readonly="readonly || lockedFields?.includes('ceCertificates')"/></template></el-table-column>
          <el-table-column label="签发日期（可留空）" min-width="180"><template #default="{ row }"><el-date-picker v-model="row.issueDate" :disabled="readonly || lockedFields?.includes('ceCertificates')" type="date" value-format="YYYY-MM-DD"/></template></el-table-column>
          <el-table-column v-if="!readonly && !lockedFields?.includes('ceCertificates')" label="操作" width="70"><template #default="{ $index }"><el-button link type="danger" @click="model.ceCertificates?.splice($index,1)">删除</el-button></template></el-table-column>
        </el-table><el-button v-if="!readonly && !lockedFields?.includes('ceCertificates')" class="mt-8px" @click="addRevision">添加Rev.记录</el-button></div>
      </el-form-item>
    </el-col>
  </el-row>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import type { CertificateFacts } from './facts'
import { fieldsFor, creationFieldsFor, factLabels, factOptions } from './facts'
const model = defineModel<CertificateFacts>({ required: true })
const props = defineProps<{ creationSection?: 'attributes' | 'certificate' | 'supplement'; readonly?: boolean; lockType?: boolean; lockedFields?: string[]; historicalRecord?: boolean; creation?: boolean; change?: boolean; visualState?: string; hideCeModels?: boolean }>()
const attributeFields = ['certificateHolder', 'classification', 'productNameEn', 'tradeName', 'productCode', 'customerHeld']
const visibleFields = computed(() => {
  const fields = (props.creation ? creationFieldsFor(model.value.certificateType) : fieldsFor(model.value.certificateType)).filter(field => !props.change || field !== 'otherContentDescription')
  if (!props.creationSection) return fields
  return fields.filter(field => props.creationSection === 'attributes' ? attributeFields.includes(field) : props.creationSection === 'supplement' ? field === 'remark' : !attributeFields.includes(field) && field !== 'remark')
})
const requiredCreationFields = computed(() => ['classification', ...(model.value.certificateType === 'DOMESTIC' ? ['certificateNo', 'firstObtainedDate', 'effectiveDate', 'expiryDate'] : []), ...(model.value.certificateType === 'CE' ? ['expiryDate'] : []), ...(model.value.certificateType === 'FDA' ? ['submissionType', 'customerHeld', 'approvalLetterStatus', 'factoryRegistrationStatus'] : ['certificateStatus'])])
const addModelSpecification = () => { model.value.modelSpecifications ??= []; model.value.modelSpecifications.push('') }
const addRevision = () => { model.value.ceCertificates ??= []; model.value.ceCertificates.push({revision:''}) }
</script>

<style scoped>
.certificate-facts-fields :deep(.el-form-item__label) { line-height: 20px; align-items: center; }
.certificate-facts-fields :deep(.el-input), .certificate-facts-fields :deep(.el-select), .certificate-facts-fields :deep(.el-date-editor.el-input) { width: 100%; }
</style>
