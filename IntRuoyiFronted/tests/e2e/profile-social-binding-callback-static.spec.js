const fs = require('fs')
const path = require('path')

const sourcePath = path.resolve(__dirname, '../../src/views/Profile/components/UserSocial.vue')
const source = fs.readFileSync(sourcePath, "utf8")

if (!/location\.origin.*\/user\/profile\?type=\$\{row\.type\}/.test(source)) {
  throw new Error('DingTalk callback must preserve the query separator and emit ?type=<socialType>')
}
if (source.includes("encodeURIComponent(`type=${row.type}`)")) {
  throw new Error('DingTalk callback must not encode the query assignment inside the callback URL')
}
if (!source.includes("socialAuthRedirect(row.type, encodeURIComponent(redirectUri))")) {
  throw new Error('The outer redirectUri request parameter must remain encoded')
}

console.log('profile social binding callback contract passed')
