using Kingdee.BOS.Core.DynamicForm.PlugIn;
using Kingdee.BOS.Core.DynamicForm.PlugIn.Args;
using Kingdee.BOS.Orm.DataEntity;
using System;
using System.Globalization;
using System.IO;
using System.Net.Http;
using System.Runtime.Serialization;
using System.Runtime.Serialization.Json;
using System.Security.Cryptography;
using System.Text;

namespace IntRuoyi.Kingdee.BosEventPlugin
{
    /// <summary>
    /// Sends a signed event callback to IntRuoyi after Kingdee completes a document operation transaction.
    /// </summary>
    public sealed class KingdeeEventCallbackOperationPlugin : AbstractOperationServicePlugIn
    {
        private const string CallbackUrlKey = "INT_RUOYI_KINGDEE_CALLBACK_URL";
        private const string CallbackSecretKey = "INT_RUOYI_KINGDEE_CALLBACK_SECRET";
        private const string TenantIdKey = "INT_RUOYI_KINGDEE_TENANT_ID";
        private const string HeaderSignature = "X-Kingdee-Signature";
        private const string HeaderTimestamp = "X-Kingdee-Timestamp";
        private const string HeaderNonce = "X-Kingdee-Nonce";

        /// <summary>
        /// Sends signed IntRuoyi callback events after the Kingdee operation transaction finishes.
        /// </summary>
        /// <param name="e">Kingdee operation transaction arguments.</param>
        public override void EndOperationTransaction(EndOperationTransactionArgs e)
        {
            base.EndOperationTransaction(e);

            if (e == null || e.DataEntitys == null || e.DataEntitys.Length == 0)
            {
                throw new InvalidOperationException("Kingdee operation has no data entities to notify.");
            }

            string callbackUrl = RequiredEnvironment(CallbackUrlKey);
            string callbackSecret = RequiredEnvironment(CallbackSecretKey);
            string tenantId = RequiredEnvironment(TenantIdKey);
            string formId = GetFormId();
            string operation = GetOperation();
            string eventTime = DateTime.Now.ToString("yyyy-MM-ddTHH:mm:ss", CultureInfo.InvariantCulture);

            foreach (DynamicObject dataEntity in e.DataEntitys)
            {
                SendEvent(callbackUrl, callbackSecret, tenantId, BuildPayload(dataEntity, formId, operation, eventTime));
            }
        }

        private KingdeeEventPayload BuildPayload(DynamicObject dataEntity, string formId, string operation, string eventTime)
        {
            string sourceFid = GetRequiredValue(dataEntity, "Id", "FID", "FId");
            string billNo = GetRequiredValue(dataEntity, "BillNo", "FBillNo");
            return new KingdeeEventPayload
            {
                EventId = BuildEventId(formId, sourceFid, billNo, operation, eventTime),
                FormId = formId,
                SourceFid = sourceFid,
                BillNo = billNo,
                Operation = operation,
                EventTime = eventTime
            };
        }

        private void SendEvent(string callbackUrl, string callbackSecret, string tenantId, KingdeeEventPayload payload)
        {
            string body = Serialize(payload);
            string timestamp = DateTime.Now.ToString("yyyy-MM-ddTHH:mm:ss", CultureInfo.InvariantCulture);
            string nonce = Guid.NewGuid().ToString("N");
            string signature = HmacSha256Hex(timestamp + "\n" + nonce + "\n" + body, callbackSecret);

            using (HttpClient client = new HttpClient())
            using (StringContent content = new StringContent(body, Encoding.UTF8, "application/json"))
            {
                client.Timeout = TimeSpan.FromSeconds(15);
                content.Headers.ContentType.CharSet = "utf-8";
                client.DefaultRequestHeaders.TryAddWithoutValidation("tenant-id", tenantId);
                client.DefaultRequestHeaders.TryAddWithoutValidation(HeaderSignature, signature);
                client.DefaultRequestHeaders.TryAddWithoutValidation(HeaderTimestamp, timestamp);
                client.DefaultRequestHeaders.TryAddWithoutValidation(HeaderNonce, nonce);

                HttpResponseMessage response = client.PostAsync(callbackUrl, content).GetAwaiter().GetResult();
                string responseBody = response.Content.ReadAsStringAsync().GetAwaiter().GetResult();
                EnsureIntRuoyiSuccess(response, responseBody);
            }
        }

        private static void EnsureIntRuoyiSuccess(HttpResponseMessage response, string responseBody)
        {
            if (!response.IsSuccessStatusCode)
            {
                throw new InvalidOperationException(
                    "IntRuoyi callback HTTP failed: " + (int)response.StatusCode + " " + responseBody);
            }

            CallbackResponse callbackResponse;
            try
            {
                callbackResponse = Deserialize<CallbackResponse>(responseBody);
            }
            catch (Exception ex)
            {
                throw new InvalidOperationException("IntRuoyi callback response is not valid JSON: " + responseBody, ex);
            }

            if (callbackResponse == null || callbackResponse.Code != 0)
            {
                string message = callbackResponse == null ? responseBody : callbackResponse.Msg;
                throw new InvalidOperationException("IntRuoyi callback rejected the event: " + message);
            }
        }

        private string GetFormId()
        {
            string formId = BusinessInfo != null && BusinessInfo.GetForm() != null
                ? BusinessInfo.GetForm().Id
                : null;
            if (string.IsNullOrWhiteSpace(formId))
            {
                throw new InvalidOperationException("Kingdee form id is unavailable.");
            }
            return formId.Trim();
        }

        private string GetOperation()
        {
            string operation = FormOperation != null ? (FormOperation.Operation ?? FormOperation.Id) : null;
            if (string.IsNullOrWhiteSpace(operation))
            {
                throw new InvalidOperationException("Kingdee operation is unavailable.");
            }
            return operation.Trim();
        }

        private static string GetRequiredValue(DynamicObject dataEntity, params string[] propertyNames)
        {
            foreach (string propertyName in propertyNames)
            {
                if (TryGetValue(dataEntity, propertyName, out string value) && !string.IsNullOrWhiteSpace(value))
                {
                    return value.Trim();
                }
            }
            throw new InvalidOperationException("Kingdee bill field is unavailable: " + string.Join("/", propertyNames));
        }

        private static bool TryGetValue(DynamicObject dataEntity, string propertyName, out string value)
        {
            value = null;
            if (dataEntity.DynamicObjectType == null
                || dataEntity.DynamicObjectType.Properties == null
                || !dataEntity.DynamicObjectType.Properties.Contains(propertyName))
            {
                return false;
            }
            object rawValue = dataEntity[propertyName];
            if (rawValue == null)
            {
                return false;
            }
            value = Convert.ToString(rawValue, CultureInfo.InvariantCulture);
            return true;
        }

        private static string RequiredEnvironment(string name)
        {
            string value = Environment.GetEnvironmentVariable(name);
            if (string.IsNullOrWhiteSpace(value))
            {
                throw new InvalidOperationException("Missing required environment variable: " + name);
            }
            return value.Trim();
        }

        private static string BuildEventId(string formId, string sourceFid, string billNo, string operation, string eventTime)
        {
            return Sha256Hex(formId + "|" + sourceFid + "|" + billNo + "|" + operation + "|" + eventTime);
        }

        private static string Serialize<T>(T value)
        {
            using (MemoryStream stream = new MemoryStream())
            {
                DataContractJsonSerializer serializer = new DataContractJsonSerializer(typeof(T));
                serializer.WriteObject(stream, value);
                return Encoding.UTF8.GetString(stream.ToArray());
            }
        }

        private static T Deserialize<T>(string json)
        {
            using (MemoryStream stream = new MemoryStream(Encoding.UTF8.GetBytes(json)))
            {
                DataContractJsonSerializer serializer = new DataContractJsonSerializer(typeof(T));
                return (T)serializer.ReadObject(stream);
            }
        }

        private static string HmacSha256Hex(string text, string secret)
        {
            using (HMACSHA256 hmac = new HMACSHA256(Encoding.UTF8.GetBytes(secret)))
            {
                byte[] digest = hmac.ComputeHash(Encoding.UTF8.GetBytes(text));
                StringBuilder builder = new StringBuilder(digest.Length * 2);
                foreach (byte item in digest)
                {
                    builder.Append(item.ToString("x2", CultureInfo.InvariantCulture));
                }
                return builder.ToString();
            }
        }

        private static string Sha256Hex(string text)
        {
            using (SHA256 sha256 = SHA256.Create())
            {
                byte[] digest = sha256.ComputeHash(Encoding.UTF8.GetBytes(text));
                StringBuilder builder = new StringBuilder(digest.Length * 2);
                foreach (byte item in digest)
                {
                    builder.Append(item.ToString("x2", CultureInfo.InvariantCulture));
                }
                return builder.ToString();
            }
        }

        [DataContract]
        private sealed class KingdeeEventPayload
        {
            [DataMember(Name = "eventId")]
            public string EventId { get; set; }

            [DataMember(Name = "formId")]
            public string FormId { get; set; }

            [DataMember(Name = "sourceFid")]
            public string SourceFid { get; set; }

            [DataMember(Name = "billNo")]
            public string BillNo { get; set; }

            [DataMember(Name = "operation")]
            public string Operation { get; set; }

            [DataMember(Name = "eventTime")]
            public string EventTime { get; set; }
        }

        [DataContract]
        private sealed class CallbackResponse
        {
            [DataMember(Name = "code")]
            public int? Code { get; set; }

            [DataMember(Name = "msg")]
            public string Msg { get; set; }
        }
    }
}
