package com.instapp.nat.wechat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;


/**
 * Created by Acathur on 19/10/2017.
 * Copyright (c) 2017 Instapp. All rights reserved.
 */

public class EntryActivity extends Activity implements IWXAPIEventHandler {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IWXAPI api = WechatModule.getWXAPIWithContext(this);

        if (api == null) {
            startMainActivity();
        } else {
            api.handleIntent(getIntent(), this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        IWXAPI api = WechatModule.getWXAPIWithContext(this);
        if (api == null) {
            startMainActivity();
        } else {
            api.handleIntent(intent, this);
        }

    }

    @Override
    public void onResp(BaseResp resp) {
        Log.d(WechatModule.TAG, "[BaseResp]");
        Log.d(WechatModule.TAG, resp.toString());

        String message = null;
        JSONObject result = new JSONObject();

        if (resp.errCode == 0) {
            switch (resp.getType()) {
                case ConstantsAPI.COMMAND_SENDAUTH:
                    SendAuth.Resp res = ((SendAuth.Resp) resp);

                    result.put("code", res.code);
                    result.put("state", res.state);
                    result.put("country", res.country);
                    result.put("lang", res.lang);
                    break;

                default:
                    break;
            }
        } else {
            switch (resp.errCode) {
                case BaseResp.ErrCode.ERR_USER_CANCEL:
                    message = WechatModule.ERROR_WECHAT_RESPONSE_USER_CANCEL;
                    break;
                case BaseResp.ErrCode.ERR_AUTH_DENIED:
                    message = WechatModule.ERROR_WECHAT_RESPONSE_AUTH_DENIED;
                    break;
                case BaseResp.ErrCode.ERR_SENT_FAILED:
                    message = WechatModule.ERROR_WECHAT_RESPONSE_SENT_FAILED;
                    break;
                case BaseResp.ErrCode.ERR_UNSUPPORT:
                    message = WechatModule.ERROR_WECHAT_RESPONSE_UNSUPPORT;
                    break;
                case BaseResp.ErrCode.ERR_COMM:
                    message = WechatModule.ERROR_WECHAT_RESPONSE_COMMON;
                    break;
                default:
                    message = WechatModule.ERROR_WECHAT_RESPONSE_UNKNOWN;
                    break;
            }

            message = resp.errStr != null ? resp.errStr : message;
        }

        WechatModule.getInstance(this).onResp(resp.errCode, result, message);

        finish();
    }

    @Override
    public void onReq(BaseReq req) {
        finish();
    }

    protected void startMainActivity() {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage(getApplicationContext().getPackageName());
        getApplicationContext().startActivity(intent);
    }
}
