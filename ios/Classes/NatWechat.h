//
//  NatWechat.h
//
//  Created by Acathur on 17/10/1.
//  Copyright © 2017 Instapp. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <WechatOpenSDK/WXApi.h>
#import <WechatOpenSDK/WXApiObject.h>

#define WXResTypeShare      @"Wechat:Share"
#define WXResTypePay        @"Wechat:Pay"
#define WXResTypeAuth       @"Wechat:Auth"
#define WXResTypeAddCard    @"Wechat:AddCard"

@interface NatWechat : NSObject <WXApiDelegate>

@property (nonatomic, strong) NSString *appId;

typedef void (^NatCallback)(id error, id result);

+ (NatWechat *)singletonManger;

+ (void)initWXAPI:(NSString *)appId;
- (void)init:(NSString *)appId :(NatCallback)callback;
- (void)checkInstalled:(NatCallback)callback;
- (void)share:(NSDictionary *)options :(NatCallback)callback;
- (void)pay:(NSDictionary *)options :(NatCallback)callback;
- (void)auth:(NSDictionary *)options :(NatCallback)callback;

@end
