package com.nimasystems.lightcast.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.ANResponse;
import com.androidnetworking.common.RequestBuilder;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.OkHttpResponseAndJSONObjectRequestListener;
import com.nimasystems.lightcast.encryption.AESCrypt;
import com.nimasystems.lightcast.logging.LcLogger;
import com.nimasystems.lightcast.utils.StringUtils;

import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.JNCryptor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Route;

abstract public class ApiCallBase implements UnauthorizedInterceptorListener {

    public enum RequestType {

        Get(1), Post(2), Put(3), Delete(4);

        private final int value;

        RequestType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static final String DEFAULT_REQUEST_CHARSET = "UTF-8";
    public static final int DEFAULT_MAX_RETRIES = 3;

    public static final String DEFAULT_XDG_HEADER_NAME = "X-Device-UUID";
    public static final String DEFAULT_XAT_HEADER_NAME = "X-User-Ac";
    public static final String DEFAULT_X_CLIENT_API_LEVEL_HEADER_NAME = "X-App-Version";
    public static final String DEFAULT_XTZ_HEADER_NAME = "X-TZ";

    public static final String DEFAULT_ACCEPTED_MIMETYPE = "application/json";

    public static final String DEFAULT_ERROR_EXTRA_DATA_KEY = "data";

    public static final int DEFAULT_CONNECT_TIMEOUT = 10000;
    public static final int DEFAULT_READ_TIMEOUT = 30000;
    public static final int DEFAULT_MAX_RETRIES_TIMEOUT = 20000;

    protected int mConnectTimeout = DEFAULT_CONNECT_TIMEOUT;
    protected int mReadTimeout = DEFAULT_READ_TIMEOUT;
    protected int mWriteTimeout = DEFAULT_READ_TIMEOUT;

    //protected final Logger mLogger = LoggerFactory.getLogger(this.getClass());

    protected LcLogger mLogger;

    protected List<Header> mRequestHeaders;
    protected String mConnectionUrl;
    protected String mRequestUserAgent;
    protected int mResponseStatusCode;
    protected List<Header> mResponseHeaders;
    protected ResponseBody mResponseBody;
    protected JSONObject mResponseJson;
    protected boolean mResponseIsSuccess;
    protected Context mContext;
    protected String mAcceptedMimetype = DEFAULT_ACCEPTED_MIMETYPE;
    protected int mClientAPILevel;
    protected String mServerHostname;
    protected Integer mServerPort;
    protected String mServerAddress;
    protected String mQueryPathPrefix;
    protected String mAccessToken;
    protected String mDeviceFriendlyName;
    protected String mDeviceGuid;
    protected String mOldDeviceGuid;
    protected String mLocale;

    protected List<Interceptor> mInterceptors;

    protected String errorExtrDataKey = DEFAULT_ERROR_EXTRA_DATA_KEY;

    private String oAuth2BearerToken;
    private boolean oAuth2AuthenticationEnabled;

    protected boolean mCallbackOnMainThread = true;
    protected boolean mSynchronizedCallback;
    protected ApiServerErrorModel mServerError;
    protected boolean mIsSynchronous;
    protected boolean mIsCancelled;
    protected boolean mIsSuccessful;
    protected int mLastErrorCode;
    protected String mLastErrorMessage;
    protected ApiCallTaskDelegate mDelegate;
    protected boolean mUseSSL;
    protected boolean mSSLTrustAll;

    private Executor mExecutor;
    private boolean mDebug;
    private String mConnectionId;
    private boolean mHttpAuthEnabled;
    private String mHttpAuthUsername;
    private String mHttpAuthPassword;
    private boolean mIsBusy;

    protected TimeZone mClientTimezone;
    protected TimeZone mServerTimezone;

    private String mDeviceGuidHeaderName = DEFAULT_XDG_HEADER_NAME;
    private String mUserAccessTokenHeaderName = DEFAULT_XAT_HEADER_NAME;
    private String mTimezoneHeaderName = DEFAULT_XTZ_HEADER_NAME;
    private String mAppVersionHeaderName = DEFAULT_X_CLIENT_API_LEVEL_HEADER_NAME;

    public String getObjectTag() {
        return objectTag;
    }

    private String objectTag;

    private ApiCallBaseErrorFilter errorFilterListener;

    public interface ApiCallBaseErrorFilter {
        boolean onError(@NonNull final ApiCallBase apiCall, final ApiServerErrorModel error);
    }

    public ApiCallBase(Context context) {
        mContext = context;
        init();
    }

    public ApiCallBase(Context context, ApiCallTaskDelegate delegate) {
        mContext = context;
        mDelegate = delegate;
        init();
    }

    protected void init() {
        objectTag = UUID.randomUUID().toString();
        mRequestHeaders = new ArrayList<>();
        trustAllCerts = new TrustManager[]{mX509TrustManager};
        mInterceptors = new ArrayList<>();
    }

    public ApiCallBase setDelegate(ApiCallTaskDelegate delegate) {
        mDelegate = delegate;
        return this;
    }

    public void addInterceptor(Interceptor interceptor) {
        mInterceptors.add(interceptor);
    }

    public void removeInterceptor(Interceptor interceptor) {
        mInterceptors.remove(interceptor);
    }

    public void removeAllInterceptors() {
        mInterceptors.clear();
    }

    public static String encrDid(String deviceGuid, String deviceFriendlyName, String oldDeviceGuid) {

        if (StringUtils.isNullOrEmpty(deviceGuid)) {
            return null;
        }

        String friendlyName = !StringUtils.isNullOrEmpty(deviceFriendlyName) ? deviceFriendlyName
                : "-unknown-";
        String joined = deviceGuid + "|" + friendlyName + "|" + "android" +
                (!StringUtils.isNullOrEmpty(oldDeviceGuid) ? "|" + oldDeviceGuid : "");

        String tkr;

        try {
            tkr = new AESCrypt().encrypt(joined);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return tkr;
    }

    public static String encrDid2(String deviceGuid, String deviceFriendlyName, String oldDeviceGuid, String encrKey) {

        if (StringUtils.isNullOrEmpty(deviceGuid) || StringUtils.isNullOrEmpty(encrKey)) {
            return null;
        }

        String friendlyName = !StringUtils.isNullOrEmpty(deviceFriendlyName) ? deviceFriendlyName
                : "-unknown-";
        String joined = deviceGuid + "|" + friendlyName + "|" + "android" +
                (!StringUtils.isNullOrEmpty(oldDeviceGuid) ? "|" + oldDeviceGuid : "");

        String tkr = null;

        JNCryptor cryptor = new AES256JNCryptor();
        byte[] plaintext = joined.getBytes();

        try {
            byte[] ct = cryptor.encryptData(plaintext, encrKey.toCharArray());
            tkr = new String(android.util.Base64.encode(ct, android.util.Base64.DEFAULT), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tkr;
    }

    public ApiCallBase setErrorFilterListener(ApiCallBaseErrorFilter errorFilterListener) {
        this.errorFilterListener = errorFilterListener;
        return this;
    }

    public void setErrorExtrDataKey(String errorExtrDataKey) {
        this.errorExtrDataKey = errorExtrDataKey;
    }

    protected static ApiServerErrorModel parseResponseForError(String errorExtrDataKey, JSONObject obj) {

        ApiServerErrorModel err;

        if (obj != null) {
            if (obj.has("error")) {
                JSONObject objError = obj.optJSONObject("error");

                if (objError != null) {
                    String domain = objError.optString("domain");
                    Integer code = objError.optInt("code");
                    String message = objError.optString("message");

                    err = new ApiServerErrorModel();
                    err.code = code;
                    err.domainName = domain;
                    err.message = message;

                    // ###### warning - fix! (was data)
                    err.extraData = objError.optJSONObject(errorExtrDataKey);

                    // validation errors
                    JSONObject vJErrs = objError
                            .optJSONObject("validation_errors");
                    if (vJErrs != null) {
                        ArrayList<ApiServerValidationError> vErrs = new ArrayList<>();
                        ApiServerValidationError verr;

                        Iterator<?> keys = vJErrs.keys();
                        while (keys.hasNext()) {
                            String key = (String) keys.next();
                            String error = vJErrs.optString(key);

                            verr = new ApiServerValidationError();
                            verr.fieldName = key;
                            verr.message = error;
                            vErrs.add(verr);
                        }

                        err.validationErrors = vErrs;
                    } else {
                        // second kind of errors
                        JSONArray vJErrs2 = objError
                                .optJSONArray("validation_failures");

                        if (vJErrs2 != null) {
                            ArrayList<ApiServerValidationError> vErrs = new ArrayList<>();
                            ApiServerValidationError verr;

                            for (int i = 0; i <= vJErrs2.length() - 1; i++) {
                                JSONObject jobj = vJErrs2.optJSONObject(i);

                                if (jobj != null) {
                                    verr = new ApiServerValidationError();
                                    verr.fieldName = jobj.optString("name");
                                    verr.message = jobj.optString("message");

                                    if (!StringUtils.isNullOrEmpty(verr.fieldName)) {
                                        vErrs.add(verr);
                                    }
                                }
                            }

                            err.validationErrors = vErrs;
                        }
                    }

                    return err;
                }
            }
        }

        return null;
    }

    protected boolean parseJSONResponse(@NonNull JSONObject response) {
        return true;
    }

    abstract protected String getRequestQueryPath();

    public String getConnectionId() {
        return mConnectionId;
    }

    public ApiCallBase setSynchronizedCallback(boolean syncCallback) {
        mSynchronizedCallback = syncCallback;
        return this;
    }

    public void setoAuth2BearerToken(String oAuth2BearerToken) {
        this.oAuth2BearerToken = oAuth2BearerToken;
    }

    public void setoAuth2AuthenticationEnabled(boolean oAuth2AuthenticationEnabled) {
        this.oAuth2AuthenticationEnabled = oAuth2AuthenticationEnabled;
    }

    public ApiCallBase setExecutor(Executor mExecutor) {
        this.mExecutor = mExecutor;
        return this;
    }

    public ApiCallBase setCallbackOnMainThread(boolean callbackOnMainThread) {
        this.mCallbackOnMainThread = callbackOnMainThread;
        return this;
    }

    public String getRequestPreparedQueryPath() {
        String ret = null;
        String qp = getRequestQueryPath();
        String query = getRequestQuery();

        if (qp != null) {
            ret = (!StringUtils.isNullOrEmpty(this.mQueryPathPrefix) ? this.mQueryPathPrefix : "") +
                    qp +
                    (query != null ? "?" + query : "");
        }

        return ret;
    }

    protected String getRequestQuery() {
        return null;
    }

    public int getClientAPILevel() {
        return mClientAPILevel;
    }

    public ApiCallBase setClientAPILevel(int apiLevel) {
        mClientAPILevel = apiLevel;
        return this;
    }

    public String getHttpAuthUsername() {
        return mHttpAuthUsername;
    }

    public ApiCallBase setHttpAuthUsername(String username) {
        mHttpAuthUsername = username;
        return this;
    }

    public String getHttpAuthPassword() {
        return mHttpAuthPassword;
    }

    public ApiCallBase setHttpAuthPassword(String password) {
        mHttpAuthPassword = password;
        return this;
    }

    public boolean getHttpAuthEnabled() {
        return mHttpAuthEnabled;
    }

    public ApiCallBase setHttpAuthEnabled(boolean enabled) {
        mHttpAuthEnabled = enabled;
        return this;
    }

    public boolean getSSLTrustAll() {
        return mSSLTrustAll;
    }

    public ApiCallBase setSSLTrustAll(boolean trustAll) {
        mSSLTrustAll = trustAll;
        return this;
    }

    public String getRequestUserAgent() {
        return mRequestUserAgent;
    }

    public ApiCallBase setRequestUserAgent(String ua) {
        mRequestUserAgent = ua;
        return this;
    }

    public boolean getDebug() {
        return mDebug;
    }

    public ApiCallBase setDebug(boolean debug) {
        mDebug = debug;
        return this;
    }

    protected void logDebugOrInfo(String str) {
        if (mLogger != null) {
            if (mDebug) {
                mLogger.info(mConnectionId + ": " + str);
            } else {
                mLogger.debug(mConnectionId + ": " + str);
            }
        }
    }

    protected void logInfo(String str) {
        if (mLogger != null) {
            mLogger.info(mConnectionId + ": " + str);
        }
    }

    protected void logError(String str) {
        if (mLogger != null) {
            mLogger.err(mConnectionId + ": " + str);
        }
    }

    protected void logDebug(String str) {
        if (mLogger != null) {
            mLogger.debug(mConnectionId + ": " + str);
        }
    }

    protected boolean initialize() {
        return true;
    }

    public ApiCallBase setConnectTimeout(int timeout) {
        mConnectTimeout = timeout;
        return this;
    }

    public ApiCallBase setReadTimeout(int timeout) {
        mReadTimeout = timeout;
        return this;
    }

    public void setClientTimezone(TimeZone timezone) {
        mClientTimezone = timezone;
    }

    public TimeZone getServerTimezone() {
        return mServerTimezone;
    }

    public ApiServerErrorModel getServerError() {
        return mServerError;
    }

    public JSONObject getResponseJson() {
        return mResponseJson;
    }

    public ResponseBody getResponseBody() {
        return mResponseBody;
    }

    public int getResponseStatusCode() {
        return mResponseStatusCode;
    }

    public List<Header> getResponseHeaders() {
        return mResponseHeaders;
    }

    public boolean getResponseIsSuccess() {
        return mResponseIsSuccess;
    }

    public boolean getIsBusy() {
        return mIsBusy;
    }

    public ApiCallBase setUseSSL(boolean useSSL) {
        mUseSSL = useSSL;
        return this;
    }

    public void setDeviceGuidHeaderName(String deviceGuidHeaderName) {
        this.mDeviceGuidHeaderName = deviceGuidHeaderName;
    }

    public void setUserAccessTokenHeaderName(String userAccessTokenHeaderName) {
        this.mUserAccessTokenHeaderName = userAccessTokenHeaderName;
    }

    public void setTimezoneHeaderName(String timezoneHeaderName) {
        this.mTimezoneHeaderName = timezoneHeaderName;
    }

    public void setAppVersionHeaderName(String appVersionHeaderName) {
        this.mAppVersionHeaderName = appVersionHeaderName;
    }

    @NonNull
    protected RequestType getRequestType() {
        return RequestType.Get;
    }

    public ApiCallBase setAccessToken(String accessToken) {
        mAccessToken = accessToken;
        return this;
    }

    public ApiCallBase setServerPort(Integer port) {
        mServerPort = port;
        return this;
    }

    public ApiCallBase setServerHostname(String hostname) {
        mServerHostname = hostname;
        return this;
    }

    public ApiCallBase setServerAddress(String address) {
        mServerAddress = address;
        return this;
    }

    public ApiCallBase setQueryPathPrefix(String prefix) {
        mQueryPathPrefix = prefix;
        return this;
    }

    public ApiCallBase setDeviceFriendlyName(String friendlyName) {
        mDeviceFriendlyName = friendlyName;
        return this;
    }

    public ApiCallBase setDeviceGuid(String deviceGuid) {
        mDeviceGuid = deviceGuid;
        return this;
    }

    public ApiCallBase setOldDeviceGuid(String deviceGuid) {
        mOldDeviceGuid = deviceGuid;
        return this;
    }

    public ApiCallBase setLocale(String locale) {
        mLocale = locale;
        return this;
    }

    public boolean isSuccessful() {
        return mIsSuccessful;
    }

    protected boolean validateRequest() {
        return true;
    }

    protected boolean getIncludeVerification() {
        return true;
    }

    public String getLastErrorMessage() {
        return mLastErrorMessage;
    }

    public int getLastErrorCode() {
        return mLastErrorCode;
    }

    public synchronized String getDeviceIdEncrypted(boolean useSecondMethod, String key) {
        // encrypt / hash the device id
        String deviceId;

        if (useSecondMethod) {
            deviceId = encrDid2(mDeviceGuid, mDeviceFriendlyName, mOldDeviceGuid, key);
        } else {
            deviceId = encrDid(mDeviceGuid, mDeviceFriendlyName, mOldDeviceGuid);
        }

        return escapeHeaderValue(deviceId);
    }

    protected void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        System.setProperty("http.keepAlive", "false");
    }

    public List<Header> getRequestHeaders() {
        return mRequestHeaders;
    }

    protected Header[] getAllHeaders() {
        List<Header> headers = prepareRequestHeaders();
        //noinspection ToArrayCallWithZeroLengthArrayArgument
        return headers.toArray(new Header[headers.size()]);
    }

    protected boolean requiresUserAuthorization() {
        return false;
    }

    protected List<Header> prepareRequestHeaders() {
        return mRequestHeaders;
    }

    protected String getRequestCharset() {
        return DEFAULT_REQUEST_CHARSET;
    }

    protected void prepareMyHeaders(String serverHostname, String charset) {

        String hostnameEnc = escapeHeaderValue(serverHostname);
        String accessToken = mAccessToken;

        // charset
        mRequestHeaders.add(new Header("Charset", charset
                .toLowerCase(Locale.US)));

        // host
        if (!StringUtils.isNullOrEmpty(hostnameEnc)) {
            mRequestHeaders.add(new Header("Host", hostnameEnc));
        }

        // user agent
        if (!StringUtils.isNullOrEmpty(mRequestUserAgent)) {
            mRequestHeaders
                    .add(new Header("User-Agent", mRequestUserAgent));
        }

        // client timezone
        // DEFAULT_XTZ_HEADER_NAME
        if (mClientTimezone != null) {
            mRequestHeaders
                    .add(new Header(DEFAULT_XTZ_HEADER_NAME, mClientTimezone.getID()));
        }

        // access token
        if (!StringUtils.isNullOrEmpty(accessToken)) {
            mRequestHeaders.add(new Header(mUserAccessTokenHeaderName, accessToken));
        }

        // OAuth2
        if (oAuth2AuthenticationEnabled && !StringUtils.isNullOrEmpty(oAuth2BearerToken)) {
            mRequestHeaders.add(new Header("Authorization", "Bearer " + oAuth2BearerToken));
        }

        // device id
        String dIdEnc = mDeviceGuid;

        if (!StringUtils.isNullOrEmpty(dIdEnc)) {
            mRequestHeaders.add(new Header(mDeviceGuidHeaderName, dIdEnc));
        }

        // client API Level
        if (mClientAPILevel > 0) {
            mRequestHeaders.add(new Header(mAppVersionHeaderName, String.valueOf(mClientAPILevel)));
        }

        // accept language fixing
        String ac = mLocale;
        ac = (ac != null) ? ac.replace('_', '-') : null;

        if (!StringUtils.isNullOrEmpty(ac)) {
            //noinspection ConstantConditions
            mRequestHeaders.add(new Header("Accept-Language", ac != null ? ac
                    .toLowerCase(Locale.US) : null));
        }

        // let the server know we expect JSON
        if (!StringUtils.isNullOrEmpty(mAcceptedMimetype)) {
            mRequestHeaders.add(new Header("Accept", mAcceptedMimetype));
        }
    }

    protected void onConnectionStart() {
        //log(getRequestType().toString().toUpperCase() + ": " + mConnectionUrl);
    }

    protected void onConnectionFinish() {
        //logDebug("Connection finish (" + mConnectionUrl + ")");

        mOKHttpClient = null;
    }

    protected void onConnectionCancel() {
        //logDebug("Connection cancel (" + mConnectionUrl + ")");
    }

    protected void handleConnectionError(Response response, ANError error) {
        String errorBody = error != null ? error.getErrorBody() : null;

        if (errorBody != null) {
            try {
                mResponseJson = new JSONObject(errorBody);
            } catch (JSONException e) {
                //
            }
        }

        mResponseStatusCode = response != null ? response.code() : 0;
        mResponseHeaders = makeResponseHeaders(response != null ? response.headers() : null);
        mResponseBody = response != null ? response.body() : null;
        mResponseIsSuccess = false;

        logError("Connection failure (" + mConnectionUrl + "), Status code: "
                + mResponseStatusCode + ", Error: " +
                (error != null ? error.getErrorBody() : (response != null ? response.message() : ""))
        );
    }

    protected void handleConnectionSuccess(Response response, JSONObject responseBody) {
        mResponseJson = responseBody;

        /*logDebug("Connection success (" + mConnectionUrl + "), Status code: "
                + mResponseStatusCode);*/
    }

    protected void onConnectionSuccess(Response response, JSONObject responseBody) {
        if (response != null) {
            mResponseStatusCode = response.code();
            mResponseHeaders = makeResponseHeaders(response.headers());
            mResponseBody = response.body();
        }

        mResponseIsSuccess = (mResponseStatusCode == 200 || mResponseStatusCode == 304) ||
                responseBody == null;

        if (mResponseIsSuccess) {
            handleConnectionSuccess(response, responseBody);
        } else {
            handleConnectionError(response, null);
        }

        doPostOperations(mResponseIsSuccess);
    }

    protected void onConnectionFailure(Response response, ANError error) {
        Response response2 = error != null ? error.getResponse() : null;
        response2 = response2 != null ? response2 : response;

        handleConnectionError(response2, error);

        if (error != null) {
            logError("\n\uD83D\uDC1E\uD83D\uDC1E\uD83D\uDC1E\uD83D\uDC1E\uD83D\uDC1E\uD83D\uDC1E\n\n" +
                    "SR (" + error.getErrorDetail() + "): " + error.getMessage() + "\n\n" +
                    "\n\n\uD83D\uDC1E\uD83D\uDC1E\uD83D\uDC1E\uD83D\uDC1E\uD83D\uDC1E\uD83D\uDC1E\n");
        }

        doPostOperations(mResponseIsSuccess);
    }

    protected List<Header> makeResponseHeaders(Headers headers) {
        if (headers == null || headers.size() < 1) {
            return null;
        }

        int i;

        List<Header> allh = new ArrayList<>();

        for (i = 0; i <= headers.size() - 1; i++) {
            allh.add(new Header(headers.name(i), headers.value(i)));
        }

        return allh;
    }

    public Header getResponseHeader(String name) {
        if (mResponseHeaders == null) {
            return null;
        }

        Header h = null;
        String n;

        for (Header ah : mResponseHeaders) {
            n = ah.getKey();
            if (n.equalsIgnoreCase(name)) {
                h = ah;
                break;
            }
        }

        return h;
    }

    public String getResponseHeaderValue(String name) {
        Header header = getResponseHeader(name);
        return (header != null ? header.getValue() : null);
    }

    protected void parseResponseHeaders() {

        if (mResponseHeaders == null) {
            return;
        }

        // timezone
        String tzCode = getResponseHeaderValue(mTimezoneHeaderName);

        if (!StringUtils.isNullOrEmpty(tzCode)) {
            mServerTimezone = TimeZone.getTimeZone(tzCode);
        }
    }

    protected void doPostOperations(boolean isConnectionSuccess) {
        parseResponseHeaders();

        boolean parseSuccess = parseResponse();

        mIsBusy = false;
        mIsSuccessful = isConnectionSuccess && parseSuccess;

        // notify the delegate
        if (mDelegate != null) {
            if (mCallbackOnMainThread) {
                if (!mSynchronizedCallback && (mIsSynchronous || isOnMainThread())) {
                    if (isOnMainThread()) {
                        if (mIsSuccessful) {
                            mDelegate.didFinishTask(this);
                        } else {
                            boolean shouldProcess = errorFilterListener == null || errorFilterListener.onError(this, mServerError);

                            if (shouldProcess) {
                                mDelegate.didFinishTaskWithError(this, mLastErrorCode,
                                        mLastErrorMessage, mServerError);
                            }
                        }
                    } else {
                        Handler mainHandler = new Handler(mContext.getMainLooper());
                        Runnable myRunnable = new Runnable() {
                            public void run() {
                                if (mIsSuccessful) {
                                    mDelegate.didFinishTask(ApiCallBase.this);
                                } else {
                                    boolean shouldProcess = errorFilterListener == null || errorFilterListener.onError(ApiCallBase.this, mServerError);

                                    if (shouldProcess) {
                                        mDelegate.didFinishTaskWithError(ApiCallBase.this, mLastErrorCode,
                                                mLastErrorMessage, mServerError);
                                    }
                                }
                            }
                        };
                        mainHandler.post(myRunnable);
                    }
                } else {
                    Handler mainHandler = new Handler(mContext.getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        public void run() {
                            if (mIsSuccessful) {
                                mDelegate.didFinishTask(ApiCallBase.this);
                            } else {
                                boolean shouldProcess = errorFilterListener == null || errorFilterListener.onError(ApiCallBase.this, mServerError);

                                if (shouldProcess) {
                                    mDelegate.didFinishTaskWithError(ApiCallBase.this,
                                            mLastErrorCode, mLastErrorMessage,
                                            mServerError);
                                }
                            }
                        }
                    };
                    mainHandler.post(myRunnable);
                }
            } else {
                if (mIsSuccessful) {
                    mDelegate.didFinishTask(ApiCallBase.this);
                } else {
                    boolean shouldProcess = errorFilterListener == null || errorFilterListener.onError(this, mServerError);

                    if (shouldProcess) {
                        mDelegate.didFinishTaskWithError(ApiCallBase.this,
                                mLastErrorCode, mLastErrorMessage,
                                mServerError);
                    }
                }
            }
        }
    }

    protected boolean isSynchronous() {
        return mIsSynchronous;
    }

    protected boolean needsSynchronization() {
        return !isOnMainThread();
    }

    protected boolean isOnMainThread() {
        return (Looper.myLooper() == Looper.getMainLooper());
    }

    private TrustManager[] trustAllCerts;

    private X509TrustManager mX509TrustManager = new X509TrustManager() {
        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        }

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }
    };

    private HostnameVerifier mHostnameVerifier = new HostnameVerifier() {
        @SuppressLint("BadHostnameVerifier")
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    protected boolean parseResponse() {

        boolean isSuccessful = false;

        try {
            if (mResponseJson != null) {
                logDebug("HTTP Response: " + mResponseJson.toString());

                // parse for a server error
                mServerError = parseResponseForError(errorExtrDataKey, mResponseJson);

                if (mServerError != null) {
                    mLastErrorCode = mServerError.code;
                    mLastErrorMessage = mServerError.message;
                } else {
                    isSuccessful = parseJSONResponse(mResponseJson);
                }
            } else {
                isSuccessful = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            logError("Unhandled exception while parsing the response: " + e);

            mLastErrorMessage = e.getMessage();
        }

        return isSuccessful;
    }

    protected String getRequestContentType() {
        return null;
    }

    protected RequestParams getRequestParams() {
        return null;
    }
    protected  JSONObject getRequestBodyJSON(){
        return null;
    }
    protected int getRequestMaxRetries() {
        return DEFAULT_MAX_RETRIES;
    }

    protected int getRequestMaxRetriesTimeout() {
        return DEFAULT_MAX_RETRIES_TIMEOUT;
    }

    protected String getConnectionUrl(boolean useSSL, String serverAddress,
                                      String serverHostname, Integer serverPort, String queryPath, String requestQuery) {
        return (useSSL ? "https://" : "http://")
                + (!StringUtils.isNullOrEmpty(serverAddress) ? serverAddress : serverHostname)
                + (serverPort != null && serverPort > 0 ? ":" + serverPort : "")
                + (!StringUtils.isNullOrEmpty(queryPath) ? queryPath : "")
                + (!StringUtils.isNullOrEmpty(requestQuery) ? "?"
                + requestQuery : "");
    }

    protected void beforeExecute() {
        //
    }

    protected boolean verifyCallParams() {
        return true;
    }

    protected String filterServerHostname(String serverHostname) {
        return serverHostname;
    }

    protected String filterServerAddress(String serverAddress) {
        return serverAddress;
    }

    protected Integer filterServerPort(Integer serverPort) {
        return serverPort;
    }

    protected boolean filterUseSSL(boolean useSSL) {
        return useSSL;
    }

    protected OkHttpClient.Builder getHttpClientBuilder() {
        return new OkHttpClient().newBuilder();
    }

    protected HashMap<String, String> preparedParams(RequestParams rp) {
        HashMap<String, String> ret = new HashMap<>();

        RequestParams requestParams = rp != null ? filterRequestParams(rp) : null;

        if (requestParams != null) {
            ConcurrentHashMap<String, String> p1 = requestParams.getUrlParams();

            for (String key : p1.keySet()) {
                String val = p1.get(key);

                if (val != null && !StringUtils.isNullOrEmpty(val) /*&& !val.equals("0")*/) {
                    ret.put(key, val);
                }
            }

            ConcurrentHashMap<String, Object> p2 = requestParams.getUrlParamsWithObjects();

            for (String key : p2.keySet()) {
                Object val = p2.get(key);
                String v = "";

                /*if (val instanceof Boolean) {
                    v = ((Boolean) val) ? "1" : "";
                } else if (val instanceof Number) {
                    v = String.valueOf(val);
                    v = v.equals("0") ? "" : v;
                } else*/
                if (val instanceof HashMap) {
                    v = new JSONObject((HashMap) val).toString();
                } else if (val instanceof List) {
                    v = new JSONArray((List) val).toString();
                }

                if (!StringUtils.isNullOrEmpty(v)) {
                    ret.put(key, v);
                }
            }
        }

        return ret;
    }

    private OkHttpClient mOKHttpClient;

    @SuppressLint("ObsoleteSdkInt")
    protected boolean executeInternal(boolean synchronous,
                                      String serverHostname, String serverAddress, Integer serverPort,
                                      boolean useSSL) {

        if (!verifyCallParams()) {
            if (mDebug) {
                logError("call params verification failed");
            }
            return false;
        }

        // check authorization
        if (requiresUserAuthorization() && StringUtils.isNullOrEmpty(mDeviceGuid)) {
            return false;
        }

        beforeExecute();

        serverHostname = filterServerHostname(serverHostname);
        serverPort = filterServerPort(serverPort);
        serverAddress = filterServerAddress(serverAddress);
        useSSL = filterUseSSL(useSSL);

        if (StringUtils.isNullOrEmpty(serverHostname)) {
            if (mDebug) {
                logError("invalid hostname");
            }
            return false;
        }

        if (mIsBusy) {
            // already running
            return false;
        }

        if (!initialize()) {
            if (mDebug) {
                logError("initialize failed");
            }
            return false;
        }

        if (!validateRequest()) {
            if (mDebug) {
                logError("validate request failed");
            }
            return false;
        }

        mIsBusy = true;
        mIsSynchronous = synchronous;

        mConnectionId = StringUtils.getRandomString();
        mIsCancelled = false;
        mLastErrorCode = 0;
        mLastErrorMessage = "";
        mIsSuccessful = false;
        mServerError = null;

        RequestType rt = getRequestType();

        String queryPath = getRequestPreparedQueryPath();

        // prepare the url
        mConnectionUrl = getConnectionUrl(useSSL, serverAddress,
                serverHostname, serverPort, queryPath, null);

        //logDebug("URL: " + mConnectionUrl);

        String charset = getRequestCharset();

        // http://stackoverflow.com/questions/2172752/httpsurlconnection-and-intermittent-connections
        if (useSSL || Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            disableConnectionReuseIfNecessary();
        }

        boolean trustSSL = (!mUseSSL || mSSLTrustAll);

        // prepare the request
        RequestBuilder builder;

        RequestParams rparams = getRequestParams();
        rparams = rparams != null ? rparams : new RequestParams();

        JSONObject jsonBody = getRequestBodyJSON();

        HashMap<String, String> preparedRequestParams = preparedParams(rparams);

        logDebugOrInfo(getRequestType().toString().toUpperCase() + " " + mConnectionUrl + ": " + preparedRequestParams);

        ConcurrentHashMap<String, RequestParams.FileWrapper> fparams = rparams.getFileParams();
        boolean requestIsMultipart = fparams != null && fparams.size() > 0;

        if (requestIsMultipart) {
            builder = new ANRequest.MultiPartBuilder<>(mConnectionUrl);

            //noinspection ConstantConditions
            if (builder instanceof ANRequest.MultiPartBuilder) {
                ANRequest.MultiPartBuilder bb = (ANRequest.MultiPartBuilder) builder;
                bb.addMultipartParameter(preparedRequestParams);

                for (String key : fparams.keySet()) {
                    RequestParams.FileWrapper obj = fparams.get(key);

                    if (obj != null) {
                        bb.addMultipartFile(key, obj.file);
                    }
                }
            }

        } else if (rt == RequestType.Post) {
            builder = new ANRequest.PostRequestBuilder<>(mConnectionUrl);
            if(jsonBody!=null){
                ((ANRequest.PostRequestBuilder) builder).addJSONObjectBody(jsonBody);
                if (preparedRequestParams != null) {
                    ((ANRequest.PostRequestBuilder) builder).addQueryParameter(preparedRequestParams);
                }
            }else if (preparedRequestParams != null) {
                ((ANRequest.PostRequestBuilder) builder).addBodyParameter(preparedRequestParams);
            }

        } else if (rt == RequestType.Put) {
            builder = new ANRequest.PutRequestBuilder(mConnectionUrl);

            if(jsonBody!=null){
                ((ANRequest.PostRequestBuilder) builder).addJSONObjectBody(jsonBody);
                if (preparedRequestParams != null) {
                    ((ANRequest.PostRequestBuilder) builder).addQueryParameter(preparedRequestParams);
                }
            }else if (preparedRequestParams != null) {
                ((ANRequest.PostRequestBuilder) builder).addBodyParameter(preparedRequestParams);
            }
        } else if (rt == RequestType.Delete) {
            builder = new ANRequest.DeleteRequestBuilder(mConnectionUrl);

            if(jsonBody!=null){
                ((ANRequest.PostRequestBuilder) builder).addJSONObjectBody(jsonBody);
                if (preparedRequestParams != null) {
                    ((ANRequest.PostRequestBuilder) builder).addQueryParameter(preparedRequestParams);
                }
            }else if (preparedRequestParams != null) {
                ((ANRequest.PostRequestBuilder) builder).addBodyParameter(preparedRequestParams);
            }
        } else {
            builder = new ANRequest.GetRequestBuilder(mConnectionUrl);

            if (preparedRequestParams != null) {
                builder.addQueryParameter(preparedRequestParams);
            }
        }

        // set the headers
        prepareMyHeaders(serverHostname, charset);

        Header[] allHeaders = getAllHeaders();

        /*if (allHeaders != null) {
            logDebug("Prepared headers");

            for (Header h : allHeaders) {
                logDebug(h.getKey() + ": " + h.getValue());
            }
        }*/

        if (allHeaders != null) {
            for (Header header : allHeaders) {
                builder.addHeaders(header.getKey(), header.getValue());
            }
        }

        // user agent
        if (!StringUtils.isNullOrEmpty(mRequestUserAgent)) {
            builder.addHeaders("User-Agent", mRequestUserAgent);
        }

        // prepare OkHttpClient

        OkHttpClient.Builder httpClientBuilder = getHttpClientBuilder();

        // http auth
        if (mHttpAuthEnabled) {
            //logDebug("HTTP AUTH ENABLED");

            httpClientBuilder.authenticator(new Authenticator() {
                @Override
                public Request authenticate(@NonNull Route route, @NonNull Response response) {
                    String credential = Credentials.basic(mHttpAuthUsername, mHttpAuthPassword);
                    return response.request().newBuilder().header("Authorization", credential).build();
                }
            });
        }

        // listen additionally to fetch the body of the response when code is not 200
        UnauthorisedInterceptor unathInterceptor = new UnauthorisedInterceptor(mContext);
        unathInterceptor.setListener(this);
        httpClientBuilder.addInterceptor(unathInterceptor);

        // add other interceptors
        for (Interceptor i : mInterceptors) {
            httpClientBuilder.addInterceptor(i);
        }

        if (this.mDebug) {
            // custom interceptor for prefetching a small part of the respnse (for error checking)
            httpClientBuilder.addInterceptor(pec);
        }

        // SSL trusting for fake / self-generated certificates
        if (trustSSL) {
            //logDebug("TRUST SSL enabled");

            // Create a trust manager that does not validate certificate chains
            // Install the all-trusting trust manager
            final SSLContext sslContext;

            try {
                sslContext = SSLContext.getInstance("SSL");

                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                // Create an ssl socket factory with our all-trusting manager
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                httpClientBuilder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
                httpClientBuilder.hostnameVerifier(mHostnameVerifier);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // timeouts
        httpClientBuilder.connectTimeout(mConnectTimeout, TimeUnit.MILLISECONDS);
        httpClientBuilder.writeTimeout(mWriteTimeout, TimeUnit.MILLISECONDS);
        httpClientBuilder.readTimeout(mReadTimeout, TimeUnit.MILLISECONDS);

        mOKHttpClient = httpClientBuilder.build();

        builder.setOkHttpClient(mOKHttpClient);
        builder.setTag(objectTag);
        builder.setExecutor(mExecutor);

        // prep the request

        ANRequest request;

        if (requestIsMultipart) {
            request = ((ANRequest.MultiPartBuilder) builder).build();
        } else if (rt == RequestType.Post) {
            request = ((ANRequest.PostRequestBuilder) builder).build();
        } else if (rt == RequestType.Put) {
            request = ((ANRequest.PutRequestBuilder) builder).build();
        } else if (rt == RequestType.Delete) {
            request = ((ANRequest.DeleteRequestBuilder) builder).build();
        } else {
            request = ((ANRequest.GetRequestBuilder) builder).build();
        }

        onConnectionStart();

        if (synchronous) {
            try {
                ANResponse response = request.executeForJSONObject();

                Response r = response.getOkHttpResponse();
                boolean success;

                if (r != null) {
                    int responseCode = r.code();
                    success = (responseCode == 200 || responseCode == 304) &&
                            response.getError() == null;

                    if (!success) {
                        onConnectionFailure(r, response.getError());
                    } else {
                        onConnectionSuccess(r, (JSONObject) response.getResult());
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                onConnectionFailure(null, null);
            } finally {
                onConnectionFinish();
            }
        } else {
            request.getAsOkHttpResponseAndJSONObject(new OkHttpResponseAndJSONObjectRequestListener() {
                @Override
                public void onResponse(Response okHttpResponse, JSONObject response) {
                    try {
                        onConnectionSuccess(okHttpResponse, response);
                    } finally {
                        onConnectionFinish();
                    }
                }

                @Override
                public void onError(ANError anError) {
                    try {
                        onConnectionFailure(null, anError);
                    } finally {
                        onConnectionFinish();
                    }
                }
            });
        }

        return true;
    }

    private Interceptor pec = new Interceptor() {
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {

            Response response = chain.proceed(chain.request());

            String responseBodyString = response.peekBody(Long.MAX_VALUE).string();
            logDebug("\n\uD83D\uDC1E " + "R: " + chain.request().url() + "\n\n" +
                    responseBodyString + "\n\n \uD83D\uDC1E\n");

            return response;
        }
    };

    protected RequestParams filterRequestParams(RequestParams requestParams) {
        return requestParams;
    }

    protected String escapeHeaderValue(String headerValue) {
        if (headerValue == null || headerValue.length() < 1) {
            return "";
        }

        headerValue = headerValue.replace("\n", "");
        headerValue = headerValue.replace("\r", "");
        return headerValue;
    }

    public boolean makeCall() {
        boolean ret = makeCall(true);

        if (!ret) {
            doPostOperations(false);
        }

        return ret;
    }

    public boolean startAsyncCall() {
        boolean ret = makeCall(false);

        if (!ret) {
            doPostOperations(false);
        }

        return ret;
    }

    public boolean execute() {
        return startAsyncCall();
    }

    protected boolean makeCall(boolean synchronous) {

        // override synchronous if looper not prepared
        if (Looper.myLooper() == null) {
            synchronous = true;
        }

        return executeInternal(synchronous, mServerHostname, mServerAddress, mServerPort, mUseSSL);
    }

    public boolean getIsCancelled() {
        return mIsCancelled;
    }

    /**
     * Wrong getter
     *
     * @deprecated use {@link #getIsCancelled()} instead.
     */
    @Deprecated
    public boolean isCancelled() {
        return getIsCancelled();
    }

    public void cancel(boolean interrupt) {
        if (!mIsCancelled && mIsBusy && objectTag != null) {
            mIsCancelled = true;
            logInfo("Cancelling the request (" + mConnectionUrl + ")");
            AndroidNetworking.forceCancel(objectTag);
        }
    }

    public void cancel() {
        cancel(true);
    }

    public Context getContext() {
        return mContext;
    }

    public LcLogger getLogger() {
        return mLogger;
    }

    public void setLogger(LcLogger logger) {
        mLogger = logger;
    }

    @Override
    public void OnUnauthorizedInterceptorAction(int responseCode, JSONObject responseJSON) {
        mResponseJson = responseJSON;
    }
}
