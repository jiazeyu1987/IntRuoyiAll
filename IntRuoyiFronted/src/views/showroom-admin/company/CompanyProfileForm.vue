<template>
  <div class="showroom-company-profile-form">
    <el-form class="showroom-company-profile-form__body" label-position="top">
      <el-form-item :label="language === 'zh' ? '公司封面' : 'Company Cover'">
        <div class="showroom-company-profile-form__cover-field">
          <UploadImg
            :limit="1"
            :model-value="form.coverImage"
            height="220px"
            width="100%"
            @update:model-value="updateCoverImage"
          />
          <p class="showroom-company-profile-form__cover-tip">
            {{
              language === 'zh'
                ? '支持上传单张公司封面图片，保存后会直接写入当前公司信息。'
                : 'Upload a single company cover image. After saving, it will be written directly into the current company record.'
            }}
          </p>
        </div>
      </el-form-item>

      <el-form-item :label="language === 'zh' ? '公司名称' : 'Company Name'">
        <el-input
          :model-value="language === 'zh' ? form.displayName : form.displayNameEn"
          @update:model-value="
            (value) => (language === 'zh' ? updateDisplayName(value) : updateDisplayNameEn(value))
          "
        />
      </el-form-item>

      <div class="showroom-company-profile-form__toolbar">
        <div>
          <h4>{{ language === 'zh' ? '公司介绍卡片' : 'Company Content' }}</h4>
          <p v-if="language === 'zh'">
            中文内容作为其他语言内容的基线，后续新增语言时继续沿用 tab 承载。
          </p>
          <p v-else>
            You can translate from the current Chinese content first, then continue editing the
            English text manually.
          </p>
        </div>
        <el-button
          v-if="language === 'en'"
          :disabled="!canTranslateEnglishFields"
          :loading="translatingEnglishFields"
          plain
          type="primary"
          @click="emit('translate-english')"
        >
          Translate English Content
        </el-button>
      </div>

      <section
        v-for="definition in companyFieldDefinitions"
        :key="definition.key"
        class="showroom-company-profile-form__field-pair"
      >
        <div class="showroom-company-profile-form__field-header">
          <h5>{{ language === 'zh' ? definition.label : definition.labelEn }}</h5>
          <span v-if="language === 'en'">对应中文：{{ definition.label }}</span>
        </div>
        <el-form-item :label="language === 'zh' ? definition.label : definition.labelEn">
          <el-input
            :model-value="
              language === 'zh'
                ? form.fields[definition.key]
                : form.fields[resolveCompanyEnglishFieldKey(definition.key)]
            "
            @update:model-value="
              (value) =>
                language === 'zh'
                  ? updateField(definition.key, value)
                  : updateEnglishField(definition.key, value)
            "
            :rows="definition.key === 'stock_info' ? 4 : 5"
            type="textarea"
          />
        </el-form-item>
      </section>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import {
  companyFieldDefinitions,
  resolveCompanyEnglishFieldKey,
  type CompanyDraftForm
} from './contracts'

defineOptions({ name: 'CompanyProfileForm' })

const props = defineProps<{
  form: CompanyDraftForm
  language: 'zh' | 'en'
  translatingEnglishFields?: boolean
  canTranslateEnglishFields?: boolean
}>()

const emit = defineEmits<{
  (event: 'update:form', value: CompanyDraftForm): void
  (event: 'translate-english'): void
}>()

const updateDisplayName = (value: string) => {
  emit('update:form', {
    ...props.form,
    displayName: value
  })
}

const updateDisplayNameEn = (value: string) => {
  emit('update:form', {
    ...props.form,
    displayNameEn: value
  })
}

const updateCoverImage = (value: string) => {
  emit('update:form', {
    ...props.form,
    coverImage: value
  })
}

const updateField = (fieldKey: keyof CompanyDraftForm['fields'], value: string) => {
  emit('update:form', {
    ...props.form,
    fields: {
      ...props.form.fields,
      [fieldKey]: value
    }
  })
}

const updateEnglishField = (fieldKey: keyof CompanyDraftForm['fields'], value: string) => {
  emit('update:form', {
    ...props.form,
    fields: {
      ...props.form.fields,
      [resolveCompanyEnglishFieldKey(fieldKey)]: value
    }
  })
}
</script>

<style scoped>
.showroom-company-profile-form {
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-company-profile-form__cover-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.showroom-company-profile-form__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.showroom-company-profile-form__toolbar h4,
.showroom-company-profile-form__field-header h5 {
  margin: 0;
  color: #172033;
}

.showroom-company-profile-form__toolbar p {
  margin: 6px 0 0;
  color: #4b5563;
  font-size: 0.88rem;
  line-height: 1.6;
}

.showroom-company-profile-form__field-pair {
  padding: 14px 16px;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 8px;
}

.showroom-company-profile-form__field-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.showroom-company-profile-form__field-header span {
  color: #4b5563;
  font-size: 0.88rem;
}

.showroom-company-profile-form__cover-tip {
  margin: 0;
  color: #4b5563;
  font-size: 0.88rem;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .showroom-company-profile-form__toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
