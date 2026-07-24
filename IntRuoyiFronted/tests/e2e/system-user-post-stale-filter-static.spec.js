const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const userFormPath = path.join(repoRoot, 'src', 'views', 'system', 'user', 'UserForm.vue')
const source = fs.readFileSync(userFormPath, 'utf8')

assert(
  source.includes('const normalizeAvailablePostIds = (postIds: unknown) =>'),
  'UserForm must normalize stale post ids before submitting user updates'
)

assert(
  source.includes('postList.value = await PostApi.getSimplePostList()') &&
    source.includes('formData.value.postIds = normalizeAvailablePostIds(formData.value.postIds)'),
  'UserForm must filter loaded user postIds against the active simple post list'
)

assert(
  source.includes('.filter((postId) => validPostIds.has(postId))'),
  'UserForm must remove deleted or unavailable post ids instead of preserving stale values'
)

console.log('PASS: system user form filters stale post ids')
