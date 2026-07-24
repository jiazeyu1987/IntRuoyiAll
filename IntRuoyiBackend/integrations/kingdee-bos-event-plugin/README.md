# IntRuoyi Kingdee BOS Event Plugin

This project builds a Kingdee BOS operation service plugin DLL for sending signed document change events to IntRuoyi.

## Build

```powershell
dotnet build .\IntRuoyi.Kingdee.BosEventPlugin.csproj -c Release
```

If the Kingdee client DLL directory is different, pass:

```powershell
dotnet build .\IntRuoyi.Kingdee.BosEventPlugin.csproj -c Release -p:KingdeeK3CloudClientDir="C:\Program Files (x86)\Kingdee\K3Cloud\DeskClient\K3CloudClient"
```

## Runtime Configuration

Set these environment variables on the Kingdee test server before registering the plugin:

- `INT_RUOYI_KINGDEE_CALLBACK_URL`: IntRuoyi callback endpoint, for example `https://<host>/admin-api/erp/kingdee/events/callback`.
- `INT_RUOYI_KINGDEE_CALLBACK_SECRET`: shared HMAC secret matching `yudao.erp.kingdee.event-callback-secret`.
- `INT_RUOYI_KINGDEE_TENANT_ID`: IntRuoyi tenant id sent as the `tenant-id` HTTP header.

Do not commit real secrets to this repository.

## Plugin Class

Register this class as an operation service plugin on the target Kingdee documents:

```text
IntRuoyi.Kingdee.BosEventPlugin.KingdeeEventCallbackOperationPlugin, IntRuoyi.Kingdee.BosEventPlugin
```

Initial target FormIds from the current IntRuoyi integration:

- `PRD_MO`: production order.
- `ENG_BOM`: BOM.
- `PRD_PPBOM`: production material list.

The plugin sends one event for each `DataEntity` in `EndOperationTransaction`. Missing callback configuration, missing bill `Id`/`BillNo`, HTTP failure, invalid IntRuoyi JSON response, or non-zero IntRuoyi result code fails the Kingdee operation immediately.
