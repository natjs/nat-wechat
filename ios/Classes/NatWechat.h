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

@interface NatWechat : NSObject

@property (nonatomic, strong) NSString *appId;

typedef void (^NatCallback)(id error, id result);

+ (NatWechat *)singletonManger;

- (void)init:(NSString *)appId :(NatCallback)callback;
- (void)checkInstalled:(NatCallback)callBack;
- (void)share:(NSDictionary *)options :(NatCallback)callBack;
- (void)pay:(NSDictionary *)options :(NatCallback)callBack;
- (void)auth:(NSDictionary *)options :(NatCallback)callBack;

@end
