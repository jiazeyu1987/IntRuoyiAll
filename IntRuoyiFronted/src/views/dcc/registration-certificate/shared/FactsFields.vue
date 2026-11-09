<template>
  <el-row :gutter="24">
    <el-col :span="12"><el-form-item label="注册证类型" required><el-select v-model="model.certificateType" :disabled="readonly || lockType"><el-option label="国内" value="DOMESTIC"/><el-option label="CE" value="CE"/><el-option label="FDA" value="FDA"/></el-select></el-form-item></el-col>
    <el-col :span="12"><el-form-item label="产品归属" required><el-select v-model="model.ownProduct" :disabled="readonly || lockType"><el-option label="瑛泰产品（关联系统产品）" :value="true"/><el-option label="其他（不关联系统产品）" :value="false"/></el-select></el-form-item></el-col>
    <el-col v-for="field in [...fieldsFor(model.certificateType),...(model.certificateType==='DOMESTIC' && !model.ownProduct ? ['projectCode'] : [])]" :key="field" :span="['remark','changeDescription','registrationLink'].includes(field) ? 24 : 12">
      <el-form-item :label="factLabels[field]">
        <a v-if="readonly && field === 'registrationLink' && model[field]" :href="model[field]" target="_blank" rel="noopener noreferrer">{{ model[field] }}</a>
        <el-select v-else-if="factOptions(field,model.certificateType).length" v-model="model[field]" :disabled="readonly" clearable><el-option v-for="option in factOptions(field,model.certificateType)" :key="String(option.value)" :label="option.label" :value="option.value"/></el-select>
        <el-date-picker v-else-if="field.endsWith('Date')" v-model="model[field]" :disabled="readonly" type="date" value-format="YYYY-MM-DD" format="YYYY-MM-DD" clearable/>
        <el-input v-else v-model="model[field]" :readonly="readonly" :type="['remark','changeDescription'].includes(field) ? 'textarea' : 'text'" :maxlength="field === 'remark' ? 1024 : 4096"/>
      </el-form-item>
    </el-col>
    <el-col v-if="model.certificateType === 'CE'" :span="24">
      <el-form-item label="证书Rev.记录">
        <div class="w-full"><el-table :data="model.ceCertificates ?? []" border>
          <el-table-column label="Rev." min-width="100"><template #default="{ row }"><el-input v-model="row.revision" :readonly="readonly"/></template></el-table-column>
          <el-table-column label="证书编号（可留空）" min-width="160"><template #default="{ row }"><el-input v-model="row.certificateNo" :readonly="readonly"/></template></el-table-column>
          <el-table-column label="签发日期（可留空）" min-width="180"><template #default="{ row }"><el-date-picker v-model="row.issueDate" :disabled="readonly" type="date" value-format="YYYY-MM-DD"/></template></el-table-column>
          <el-table-column v-if="!readonly" label="操作" width="70"><template #default="{ $index }"><el-button link type="danger" @click="model.ceCertificates?.splice($index,1)">删除</el-button></template></el-table-column>
        </el-table><el-button v-if="!readonly" class="mt-8px" @click="addRevision">添加Rev.记录</el-button></div>
      </el-form-item>
    </el-col>
  </el-row>
</template>
<script setup lang="ts">
import type { CertificateFacts } from './facts'
import { fieldsFor, factLabels, factOptions } from './facts'
const model = defineModel<CertificateFacts>({ required: true })
defineProps<{ readonly?: boolean; lockType?: boolean }>()
const addRevision = () => { model.value.ceCertificates ??= []; model.value.ceCertificates.push({revision:''}) }
</script>
