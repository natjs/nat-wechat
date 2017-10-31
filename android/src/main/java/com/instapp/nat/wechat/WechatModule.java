package com.instapp.nat.wechat;

/**
 * Created by Acathur on 19/10/2017.
 * Copyright (c) 2017 Instapp. All rights reserved.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.webkit.URLUtil;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXAppExtendObject;
import com.tencent.mm.opensdk.modelmsg.WXEmojiObject;
import com.tencent.mm.opensdk.modelmsg.WXFileObject;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXMusicObject;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXVideoObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class WechatModule {

    public static String APP_ID = "";
    public static final String TAG = "Nat.wechat";

    public static final String ERROR_WECHAT_NOT_INITED = "程序未初始化";
    public static final String ERROR_WECHAT_NOT_INSTALLED = "未安装微信";
    public static final String ERROR_INVALID_PARAMETERS = "参数格式错误";
    public static final String ERROR_SEND_REQUEST_FAILED = "发送请求失败";
    public static final String ERROR_WECHAT_RESPONSE_USER_CANCEL = "用户取消";
    public static final String ERROR_WECHAT_RESPONSE_COMMON = "普通错误";
    public static final String ERROR_WECHAT_RESPONSE_SENT_FAILED = "发送失败";
    public static final String ERROR_WECHAT_RESPONSE_AUTH_DENIED = "授权失败";
    public static final String ERROR_WECHAT_RESPONSE_UNSUPPORT = "微信不支持";
    public static final String ERROR_WECHAT_RESPONSE_UNKNOWN = "未知错误";

    public static final String EXTERNAL_STORAGE_IMAGE_PREFIX = "external://";

    public static final String KEY_ARG_MESSAGE = "content";
    public static final String KEY_ARG_SCENE = "scene";
    public static final String KEY_ARG_TEXT = "text";
    public static final String KEY_ARG_MESSAGE_TITLE = "title";
    public static final String KEY_ARG_MESSAGE_DESCRIPTION = "description";
    public static final String KEY_ARG_MESSAGE_THUMB = "thumbUrl";
    public static final String KEY_ARG_MESSAGE_MEDIA_TYPE = "type";
    public static final String KEY_ARG_MESSAGE_MEDIA_TEXT = "text";
    public static final String KEY_ARG_MESSAGE_MEDIA_FILE = "filePath";
    public static final String KEY_ARG_MESSAGE_MEDIA_DATAURL = "dataUrl";
    public static final String KEY_ARG_MESSAGE_MEDIA_MUSICURL = "link";
    public static final String KEY_ARG_MESSAGE_MEDIA_WEBPAGEURL = "link";
    public static final String KEY_ARG_MESSAGE_MEDIA_EXTINFO = "extInfo";
    public static final String KEY_ARG_MESSAGE_MEDIA_URL = "url";

    public static final int TYPE_WECHAT_SHARING_TEXT = 0;
    public static final int TYPE_WECHAT_SHARING_APP = 1;
    public static final int TYPE_WECHAT_SHARING_EMOTION = 2;
    public static final int TYPE_WECHAT_SHARING_FILE = 3;
    public static final int TYPE_WECHAT_SHARING_IMAGE = 4;
    public static final int TYPE_WECHAT_SHARING_MUSIC = 5;
    public static final int TYPE_WECHAT_SHARING_VIDEO = 6;
    public static final int TYPE_WECHAT_SHARING_WEBPAGE = 7;
    public static final int TYPE_WECHAT_SHARING_MINIPROGRAM = 8;

    public static final int SCENE_SESSION = 0;
    public static final int SCENE_TIMELINE = 1;
    public static final int SCENE_FAVORITE = 2;

    public static final int MAX_THUMBNAIL_SIZE = 320;

    protected static IWXAPI wxAPI;

    private Context mContext;
    private AsyncListener mAsyncListener;
    private static volatile WechatModule instance = null;

    private WechatModule(Context context){
        mContext = context;
    }

    public static WechatModule getInstance(Context context) {
        if (instance == null) {
            synchronized (WechatModule.class) {
                if (instance == null) {
                    instance = new WechatModule(context);
                }
            }
        }

        return instance;
    }

    public interface AsyncListener {
        void onSuccess(JSONObject result);
        void onError(int code, String msg);
    }

    public void initWXAPI(String appId) {
        if (appId != null) {
            APP_ID = appId;
        }

        IWXAPI api = getWXAPI();

        if (api != null) {
            api.registerApp(APP_ID);
        }
    }

    /**
     * Get weixin api
     * @return wxAPI
     */
    public IWXAPI getWXAPI() {
        if (wxAPI == null && !APP_ID.isEmpty()) {
            wxAPI = WXAPIFactory.createWXAPI(mContext, APP_ID, true);
        }

        return wxAPI;
    }

    public static IWXAPI getWXAPIWithContext(Context ctx) {
        if (wxAPI == null && !APP_ID.isEmpty()) {
            wxAPI = WXAPIFactory.createWXAPI(ctx, APP_ID, true);
        }

        return wxAPI;
    }

    private void callbackError(final int code, final String msg, final ModuleResultListener listener) {
        JSONObject result = new JSONObject();
        JSONObject error = new JSONObject();

        error.put("code", code);
        error.put("msg", msg);
        result.put("error", error);

        listener.onResult(result);
        return;
    }

    public void init(final String appId, final ModuleResultListener listener) {
        initWXAPI(appId);

        listener.onResult(true);
    }

    public void share(final JSONObject params, final ModuleResultListener listener)
            throws JSONException {
        final IWXAPI api = getWXAPI();

        if (api == null) {
            callbackError(301101, ERROR_WECHAT_NOT_INITED, listener);
        }

        // check if installed
        if (!api.isWXAppInstalled()) {
            callbackError(301201, ERROR_WECHAT_NOT_INSTALLED, listener);
        }

        final SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction();

        switch (params.getIntValue(KEY_ARG_SCENE)) {
            case SCENE_SESSION:
                req.scene = SendMessageToWX.Req.WXSceneSession;
                break;

            case SCENE_TIMELINE:
                req.scene = SendMessageToWX.Req.WXSceneTimeline;
                break;

            case SCENE_FAVORITE:
                req.scene = SendMessageToWX.Req.WXSceneFavorite;
                break;

            default:
                req.scene = SendMessageToWX.Req.WXSceneSession;
        }

        Runnable shareRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    req.message = buildSharingMessage(params);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to build sharing message.", e);
                    callbackError(301301, "Failed to build sharing message.", listener);
                }

                if (api.sendReq(req)) {
                    Log.i(TAG, "Message has been called successfully.");

                    mAsyncListener = new AsyncListener() {
                        @Override
                        public void onSuccess(JSONObject result) {
                            listener.onResult(result);
                        }

                        @Override
                        public void onError(int code, String msg) {
                            callbackError(code, msg, listener);
                        }
                    };
                } else {
                    Log.i(TAG, "Message has been called unsuccessfully.");
                    callbackError(301302, "Unknown error", listener);
                }
            }
        };

        Thread shareThread = new Thread(shareRunnable);
        shareThread.start();
    }

    public void auth(final JSONObject params, final ModuleResultListener listener) {
        final IWXAPI api = getWXAPI();

        final SendAuth.Req req = new SendAuth.Req();
        try {
            req.scope = params.getString("scope");
            req.state = params.getString("state");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());

            req.scope = "snsapi_userinfo";
            req.state = "wechat";
        }

        if (api.sendReq(req)) {
            Log.i(TAG, "Auth request has been sent successfully.");

            mAsyncListener = new AsyncListener() {
                @Override
                public void onSuccess(JSONObject result) {
                    listener.onResult(result);
                }

                @Override
                public void onError(int code, String msg) {
                    callbackError(code, msg, listener);
                }
            };
        } else {
            Log.i(TAG, "Auth request has been sent unsuccessfully.");

            // send error
            callbackError(301401, ERROR_SEND_REQUEST_FAILED, listener);
        }
    }

    public void pay(final JSONObject params, final ModuleResultListener listener) {

        final IWXAPI api = getWXAPI();

        PayReq req = new PayReq();

        try {
            req.appId = APP_ID;
            if (params.getString("mch_id") != null) {
                req.partnerId = params.getString("mch_id");
                req.prepayId = params.getString("prepay_id");
                req.nonceStr = params.getString("nonce");
            } else {
                req.partnerId = params.getString("partnerid");
                req.prepayId = params.getString("prepayid");
                req.nonceStr = params.getString("noncestr");
            }
            req.timeStamp = params.getString("timestamp");
            req.sign = params.getString("sign");
            req.packageValue = "Sign=WXPay";
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());

            callbackError(301501, ERROR_INVALID_PARAMETERS, listener);
        }

        if (api.sendReq(req)) {
            Log.i(TAG, "Payment request has been sent successfully.");

            mAsyncListener = new AsyncListener() {
                @Override
                public void onSuccess(JSONObject result) {
                    listener.onResult(result);
                }

                @Override
                public void onError(int code, String msg) {
                    callbackError(code, msg, listener);
                }
            };
        } else {
            Log.i(TAG, "Payment request has been sent unsuccessfully.");

            // send error

            callbackError(301401, ERROR_SEND_REQUEST_FAILED, listener);
        }
    }

    public boolean isInstalled() {
        final IWXAPI api = getWXAPI();

        return api.isWXAppInstalled();
    }

    public void checkInstalled(final ModuleResultListener listener) {
        listener.onResult(isInstalled());
    }

    protected WXMediaMessage buildSharingMessage(JSONObject params)
            throws JSONException {
        Log.d(TAG, "Start building message.");

        // media parameters
        WXMediaMessage.IMediaObject mediaObject = null;
        WXMediaMessage wxMediaMessage = new WXMediaMessage();

        String text = params.getString(KEY_ARG_TEXT);

        if (text != null) {
            WXTextObject textObject = new WXTextObject();
            textObject.text = text;
            mediaObject = textObject;
            wxMediaMessage.description = textObject.text;
        } else {
            JSONObject content = params.getJSONObject(KEY_ARG_MESSAGE);

            wxMediaMessage.title = content.getString(KEY_ARG_MESSAGE_TITLE);
            wxMediaMessage.description = content.getString(KEY_ARG_MESSAGE_DESCRIPTION);

            // thumbnail
            Bitmap thumbnail = getThumbnail(content.getString(KEY_ARG_MESSAGE_THUMB));
            if (thumbnail != null) {
                wxMediaMessage.setThumbImage(thumbnail);
                thumbnail.recycle();
            }

            // check types
            int type = content.getIntValue(KEY_ARG_MESSAGE_MEDIA_TYPE);

            if (type == 0) {
                type = TYPE_WECHAT_SHARING_WEBPAGE;
            }

            switch (type) {
                case TYPE_WECHAT_SHARING_APP:
                    WXAppExtendObject appObject = new WXAppExtendObject();
                    appObject.extInfo = content.getString(KEY_ARG_MESSAGE_MEDIA_EXTINFO);
                    appObject.filePath = content.getString(KEY_ARG_MESSAGE_MEDIA_URL);
                    mediaObject = appObject;
                    break;

                case TYPE_WECHAT_SHARING_EMOTION:
                    WXEmojiObject emoObject = new WXEmojiObject();
                    InputStream emoji = getFileInputStream(content.getString(KEY_ARG_MESSAGE_MEDIA_DATAURL));
                    if (emoji != null) {
                        try {
                            emoObject.emojiData = Utils.readBytes(emoji);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    mediaObject = emoObject;
                    break;

                case TYPE_WECHAT_SHARING_FILE:
                    WXFileObject fileObject = new WXFileObject();
                    fileObject.filePath = content.getString(KEY_ARG_MESSAGE_MEDIA_FILE);
                    mediaObject = fileObject;
                    break;

                case TYPE_WECHAT_SHARING_IMAGE:
                    Bitmap image = getBitmap(content.getString(KEY_ARG_MESSAGE_MEDIA_DATAURL), 0);
                    mediaObject = new WXImageObject(image);
                    image.recycle();
                    break;

                case TYPE_WECHAT_SHARING_MUSIC:
                    WXMusicObject musicObject = new WXMusicObject();
                    musicObject.musicUrl = content.getString(KEY_ARG_MESSAGE_MEDIA_MUSICURL);
                    musicObject.musicDataUrl = content.getString(KEY_ARG_MESSAGE_MEDIA_DATAURL);
                    mediaObject = musicObject;
                    break;

                case TYPE_WECHAT_SHARING_VIDEO:
                    WXVideoObject videoObject = new WXVideoObject();
                    videoObject.videoUrl = content.getString(KEY_ARG_MESSAGE_MEDIA_DATAURL);
                    mediaObject = videoObject;
                    break;

                case TYPE_WECHAT_SHARING_WEBPAGE:
                default:
                    mediaObject = new WXWebpageObject(content.getString(KEY_ARG_MESSAGE_MEDIA_WEBPAGEURL));
            }
        }

        wxMediaMessage.mediaObject = mediaObject;

        return wxMediaMessage;
    }

    private String buildTransaction() {
        return String.valueOf(System.currentTimeMillis());
    }

    private String buildTransaction(final String type) {
        return type + System.currentTimeMillis();
    }

    protected Bitmap getThumbnail(String url) {
        return getBitmap(url, MAX_THUMBNAIL_SIZE);
    }

    protected Bitmap getBitmap(String url, int maxSize) {
        Bitmap bmp = null;

        if (url == null) {
            return null;
        }

        try {
            // get input stream
            InputStream inputStream = getFileInputStream(url);
            if (inputStream == null) {
                return null;
            }

            // decode it
            // @TODO make sure the image is not too big, or it will cause out of memory
            BitmapFactory.Options options = new BitmapFactory.Options();
            bmp = BitmapFactory.decodeStream(inputStream, null, options);

            // scale
            if (maxSize > 0 && (options.outWidth > maxSize || options.outHeight > maxSize)) {

                Log.d(TAG, String.format("Bitmap was decoded, dimension: %d x %d, max allowed size: %d.",
                        options.outWidth, options.outHeight, maxSize));

                int width = 0;
                int height = 0;

                if (options.outWidth > options.outHeight) {
                    width = maxSize;
                    height = width * options.outHeight / options.outWidth;
                } else {
                    height = maxSize;
                    width = height * options.outWidth / options.outHeight;
                }

                Bitmap scaled = Bitmap.createScaledBitmap(bmp, width, height, true);
                bmp.recycle();

                bmp = scaled;
            }

            inputStream.close();

        } catch (JSONException e) {
            bmp = null;
            e.printStackTrace();
        } catch (IOException e) {
            bmp = null;
            e.printStackTrace();
        }

        return bmp;
    }

    /**
     * Get input stream from a url
     *
     * @param url file url
     * @return stream
     */
    protected InputStream getFileInputStream(String url) {
        try {

            InputStream inputStream = null;

            if (URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url)) {

                File file = Utils.downloadAndCacheFile(mContext, url);

                if (file == null) {
                    Log.d(TAG, String.format("File could not be downloaded from %s.", url));
                    return null;
                }

                url = file.getAbsolutePath();
                inputStream = new FileInputStream(file);

                Log.d(TAG, String.format("File was downloaded and cached to %s.", url));

            } else if (url.startsWith("data:image")) {  // base64 image

                String imageDataBytes = url.substring(url.indexOf(",") + 1);
                byte imageBytes[] = Base64.decode(imageDataBytes.getBytes(), Base64.DEFAULT);
                inputStream = new ByteArrayInputStream(imageBytes);

                Log.d(TAG, "Image is in base64 format.");

            } else if (url.startsWith(EXTERNAL_STORAGE_IMAGE_PREFIX)) { // external path

                url = Environment.getExternalStorageDirectory().getAbsolutePath() + url.substring(EXTERNAL_STORAGE_IMAGE_PREFIX.length());
                inputStream = new FileInputStream(url);

                Log.d(TAG, String.format("File is located on external storage at %s.", url));

            } else if (!url.startsWith("/")) { // relative path
//              TODO: relative path support

//                inputStream = cordova.getActivity().getApplicationContext().getAssets().open(url);
//
//                Log.d(TAG, String.format("File is located in assets folder at %s.", url));

            } else {

                inputStream = new FileInputStream(url);

                Log.d(TAG, String.format("File is located at %s.", url));

            }

            return inputStream;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * on Response
     */
    public void onResp(int errCode, JSONObject result, String message) {
        if (mAsyncListener == null) {
            return;
        }

        if (errCode == 0) {
            mAsyncListener.onSuccess(result);
        } else {
            mAsyncListener.onError(errCode, message);
        }

        mAsyncListener = null;
    }
}
