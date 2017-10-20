//
//  NatWechat.m
//
//  Created by Acathur on 17/10/1.
//  Copyright © 2017 Instapp. All rights reserved.
//

#import "NatWechat.h"

static int const MAX_THUMBNAIL_SIZE = 320;

@interface NatWechat ()
@end

@implementation NatWechat

+ (NatWechat *)singletonManger{
    static id manager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        manager = [[self alloc] init];
    });
    return manager;
}

- (void)initWXAPI:(NSString *)appId {
    self.wechatAppId = appId;
    [WXApi registerApp: appId];
}

- (void)init:(NSString *)appId :(NatCallback)callback {
    [self initWXAPI: appId];

    callback(nil)
}

- (void)isInstalled {
    return [WXApi isWXAppInstalled];
}

- (void)checkInstalled:(NatCallback)callBack {
    callBack(nil, [self isInstalled]);
}

- (void)share:(NSDictionary *)options :(NatCallback)callBack {
    // if not installed
    if (![WXApi isWXAppInstalled]) {
        callback(@{@"error":@{@"msg":"微信未安装", @"code":"301201"}}, nil);
        return;
    }

    SendMessageToWXReq* req = [[SendMessageToWXReq alloc] init];
    
    // check the scene
    if ([options objectForKey:@"scene"]) {
        req.scene = (int)[[options objectForKey:@"scene"] integerValue];
    } else {
        req.scene = WXSceneTimeline;
    }
    
    // message or text
    NSDictionary *message = [options objectForKey:@"message"];
    
    if (message) {
        req.bText = NO;
        
        // async
        [self.commandDelegate runInBackground:^{
            req.message = [self buildSharingMessage:message];
            if (![WXApi sendReq:req]) {
                callback(@{@"error":@{@"msg":"发送请求失败", @"code":"301401"}}, nil);
            }
        }];
    } else {
        req.bText = YES;
        req.text = [options objectForKey:@"text"];
        
        if (![WXApi sendReq:req]) {
            callback(@{@"error":@{@"msg":"发送请求失败", @"code":"301401"}}, nil);
        }
    }
}

- (void)pay:(NSDictionary *)options :(NatCallback)callback {
    
    // check required parameters
    NSArray *requiredParams;

    if ([options objectForKey:@"mch_id"]) {
        requiredParams = @[@"mch_id", @"prepay_id", @"timestamp", @"nonce", @"sign"];
    } else {
        requiredParams = @[@"partnerid", @"prepayid", @"timestamp", @"noncestr", @"sign"];
    }
    
    for (NSString *key in requiredParams) {
        if (![options objectForKey:key]) {
            callback(@{@"error":@{@"msg":"参数格式错误", @"code":"301501"}}, nil);
            return;
        }
    }
    
    PayReq *req = [[PayReq alloc] init];
    req.partnerId = [options objectForKey:requiredParams[0]];
    req.prepayId = [options objectForKey:requiredParams[1]];
    req.timeStamp = [[options objectForKey:requiredParams[2]] intValue];
    req.nonceStr = [options objectForKey:requiredParams[3]];
    req.package = @"Sign=WXPay";
    req.sign = [options objectForKey:requiredParams[4]];
    
    if ([WXApi sendReq:req]) {
    } else {
        callback(@{@"error":@{@"msg":"发送请求失败", @"code":"301401"}}, nil);
    }
}

- (void)auth:(NSDictionary *)options :(NatCallback)callback {

    SendAuthReq* req =[[SendAuthReq alloc] init];

    if ([options objectForKey:@"scene"]) {
        req.scope = [options objectForKey:@"scene"];
    } else {
        req.scope = @"snsapi_userinfo";
    }

    if ([options objectForKey:@"state"]) {
        req.state = [options objectForKey:@"state"];
    }
    
    UIViewController *vc = [self getCurrentVC];
    
    if ([WXApi sendAuthReq:req viewController:vc delegate:self]) {
    } else {
        callback(@{@"error":@{@"msg":"发送请求失败", @"code":"301401"}}, nil);
    }
}

#pragma mark "Private methods"

- (WXMediaMessage *)buildSharingMessage:(NSDictionary *)message
{
    WXMediaMessage *wxMediaMessage = [WXMediaMessage message];
    wxMediaMessage.title = [message objectForKey:@"title"];
    wxMediaMessage.description = [message objectForKey:@"description"];
    wxMediaMessage.mediaTagName = [message objectForKey:@"mediaTagName"];
    wxMediaMessage.messageExt = [message objectForKey:@"messageExt"];
    wxMediaMessage.messageAction = [message objectForKey:@"messageAction"];
    if ([message objectForKey:@"thumb"])
    {
        [wxMediaMessage setThumbImage:[self getUIImageFromURL:[message objectForKey:@"thumb"]]];
    }
    
    // media parameters
    id mediaObject = nil;
    NSDictionary *media = [message objectForKey:@"media"];
    
    // check types
    NSInteger type = [[media objectForKey:@"type"] integerValue];
    switch (type)
    {
        case CDVWXSharingTypeApp:
            mediaObject = [WXAppExtendObject object];
            ((WXAppExtendObject*)mediaObject).extInfo = [media objectForKey:@"extInfo"];
            ((WXAppExtendObject*)mediaObject).url = [media objectForKey:@"url"];
            break;
            
        case CDVWXSharingTypeEmotion:
            mediaObject = [WXEmoticonObject object];
            ((WXEmoticonObject*)mediaObject).emoticonData = [self getNSDataFromURL:[media objectForKey:@"emotion"]];
            break;
            
        case CDVWXSharingTypeFile:
            mediaObject = [WXFileObject object];
            ((WXFileObject*)mediaObject).fileData = [self getNSDataFromURL:[media objectForKey:@"file"]];
            ((WXFileObject*)mediaObject).fileExtension = [media objectForKey:@"fileExtension"];
            break;
            
        case CDVWXSharingTypeImage:
            mediaObject = [WXImageObject object];
            ((WXImageObject*)mediaObject).imageData = [self getNSDataFromURL:[media objectForKey:@"image"]];
            break;
            
        case CDVWXSharingTypeMusic:
            mediaObject = [WXMusicObject object];
            ((WXMusicObject*)mediaObject).musicUrl = [media objectForKey:@"musicUrl"];
            ((WXMusicObject*)mediaObject).musicDataUrl = [media objectForKey:@"musicDataUrl"];
            break;
            
        case CDVWXSharingTypeVideo:
            mediaObject = [WXVideoObject object];
            ((WXVideoObject*)mediaObject).videoUrl = [media objectForKey:@"videoUrl"];
            break;
            
        case CDVWXSharingTypeWebPage:
        default:
            mediaObject = [WXWebpageObject object];
            ((WXWebpageObject *)mediaObject).webpageUrl = [media objectForKey:@"webpageUrl"];
    }
    
    wxMediaMessage.mediaObject = mediaObject;
    return wxMediaMessage;
}

- (NSData *)getNSDataFromURL:(NSString *)url
{
    NSData *data = nil;
    
    if ([url hasPrefix:@"http://"] || [url hasPrefix:@"https://"])
    {
        data = [NSData dataWithContentsOfURL:[NSURL URLWithString:url]];
    }
    else if ([url hasPrefix:@"data:image"])
    {
        // a base 64 string
        NSURL *base64URL = [NSURL URLWithString:url];
        data = [NSData dataWithContentsOfURL:base64URL];
    }
    else if ([url rangeOfString:@"temp:"].length != 0)
    {
        url =  [NSTemporaryDirectory() stringByAppendingPathComponent:[url componentsSeparatedByString:@"temp:"][1]];
        data = [NSData dataWithContentsOfFile:url];
    }
    else
    {
        // local file
        url = [[NSBundle mainBundle] pathForResource:[url stringByDeletingPathExtension] ofType:[url pathExtension]];
        data = [NSData dataWithContentsOfFile:url];
    }
    
    return data;
}

- (UIImage *)getUIImageFromURL:(NSString *)url
{
    NSData *data = [self getNSDataFromURL:url];
    UIImage *image = [UIImage imageWithData:data];
    
    if (image.size.width > MAX_THUMBNAIL_SIZE || image.size.height > MAX_THUMBNAIL_SIZE)
    {
        CGFloat width = 0;
        CGFloat height = 0;
        
        // calculate size
        if (image.size.width > image.size.height)
        {
            width = MAX_THUMBNAIL_SIZE;
            height = width * image.size.height / image.size.width;
        }
        else
        {
            height = MAX_THUMBNAIL_SIZE;
            width = height * image.size.width / image.size.height;
        }
        
        // scale it
        UIGraphicsBeginImageContext(CGSizeMake(width, height));
        [image drawInRect:CGRectMake(0, 0, width, height)];
        UIImage *scaled = UIGraphicsGetImageFromCurrentImageContext();
        UIGraphicsEndImageContext();
        
        return scaled;
    }
    
    return image;
}

- (UIViewController *)getCurrentVC
{
    UIViewController *result = nil;
    
    UIWindow * window = [[UIApplication sharedApplication] keyWindow];
    if (window.windowLevel != UIWindowLevelNormal)
    {
        NSArray *windows = [[UIApplication sharedApplication] windows];
        for(UIWindow * tmpWin in windows)
        {
            if (tmpWin.windowLevel == UIWindowLevelNormal)
            {
                window = tmpWin;
                break;
            }
        }
    }
    
    UIView *frontView = [[window subviews] objectAtIndex:0];
    id nextResponder = [frontView nextResponder];
    
    if ([nextResponder isKindOfClass:[UIViewController class]])
        result = nextResponder;
    else
        result = window.rootViewController;
    
    return result;
}



@end
