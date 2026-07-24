const { assertApprovalCenterStandardListContract } = require('./approval-center-standard-list-template-contract-helper')

assertApprovalCenterStandardListContract({
  routePath: 'my-initiated',
  routeName: 'ApprovalCenterMyInitiated',
  routeTitle: '我发起的',
  viewType: 'MY_INITIATED',
  tableKey: 'approval.center.myInitiated'
})

console.log('PASS: approval center MY_INITIATED standard list template static contract')
