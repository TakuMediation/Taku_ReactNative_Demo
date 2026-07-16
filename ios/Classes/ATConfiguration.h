//
//  ATConfiguration.m
//  topon_flutter_plugin
//
//  Created by GUO PENG on 2021/6/30.
//

#import <Foundation/Foundation.h>

#ifdef DEBUG


#define ATLog(FORMAT, ...) fprintf(stderr,"[%s]:[line %d行] iOS::  %s\n",[[[NSString stringWithUTF8String:__FILE__] lastPathComponent] UTF8String], __LINE__, [[NSString stringWithFormat:FORMAT, ##__VA_ARGS__] UTF8String]);

#else

#define ATLog(FORMAT, ...)

#endif





#define RequestCode  @"requestCode"
#define RequestMessage @"requestMessage"
#define PlacementID @"placementID"
#define CallNameKey @"callbackName"

#define CommonADCallName          @"CommonADCall"
#define AdShowRevenueCallbackKey  @"adShowRevenueCallbackKey"
#define DeepLink @"isDeeplinkSuccess"

#define ExtraDic  @"extraDic"
#define UserExtraKey  @"user_load_extra_data"

#define MultipleLoadBiddingAttemptKey  @"bidding_attempt"
#define MultipleLoadLoadingKey  @"loading"

#pragma mark - funcinit方法相关

#define SetLogEnabled @"setLogEnabled"

#define SetChannelStr @"setChannelStr"

#define SetSubchannelStr @"setSubchannelStr"

#define SetCustomDataDic @"setCustomDataDic"

#define SetExludeBundleIDArray @"setExludeBundleIDArray"

#define SetPlacementCustomData @"setPlacementCustomData"

#define GetGDPRLevel @"getGDPRLevel"

#define GetUserLocation @"getUserLocation"

#define ShowGDPRAuth @"showGDPRAuth"
#define ShowGDPRConsentDialog @"showGDPRConsentDialog"
#define ShowGDPRConsentSecondDialog @"showGDPRConsentSecondDialog"
#define CheckIsEuTraffic @"checkIsEuTraffic"
#define GetSDKVersionName @"getSDKVersionName"
#define StartSDK @"start"
#define PutFilter @"putFilter"
#define RemoveFilters @"removeFilters"
#define RemoveFilterWithPlacementId @"removeFilterWithPlacementId"
#define SetSharedPlacementConfig @"setSharedPlacementConfig"
#define SetAdSourcePrivacyPolicy @"setAdSourcePrivacyPolicy"
#define ShowDebugUI @"showDebuggerUI"

#define SetDataConsentSet @"setDataConsentSet"

#define SetDeniedUploadInfoArray @"deniedUploadDeviceInfo"

#define InitAnyThinkSDK @"initAnyThinkSDK"
#define SetPresetPlacementConfigPath @"setPresetPlacementConfigPath"

#pragma mark - funcReward方法相关

#define LoadRewardedVideo @"loadRewardedVideo"

#define RewardedVideoReady @"rewardedVideoReady"

#define CheckRewardedVideoLoadStatus @"checkRewardedVideoLoadStatus"

#define ShowRewardedVideo @"showRewardedVideo"

#define ShowSceneRewardedVideo @"showSceneRewardedVideo"

#define ShowRewardedVideoWithShowConfig @"showRewardedVideoWithShowConfig"

#define GetRewardedVideoValidAds @"getRewardedVideoValidAds"

#define EntryRewardedVideoScenario @"entryRewardedVideoScenario"

#define AutoLoadRewardedVideo @"autoLoadRewardedVideoAD"

#define CancelAutoLoadRewardedVideo @"cancelAutoLoadRewardedVideoAD"

#define ShowAutoLoadRewardedVideoAD @"showAutoLoadRewardedVideoAD"

#define AutoLoadRewardedVideoSetLocalExtra @"autoLoadRewardedVideoADSetLocalExtra"

#pragma mark - funcSplash方法相关

#define LoadSplashAd @"loadSplash"

#define SplashAdReadyReady @"splashReady"

#define CheckSplashAdLoadStatus @"checkSplashLoadStatus"

#define ShowSplashAd @"showSplash"

#define ShowSceneSplashAd @"showSceneSplash"

#define ShowSplashAdWithShowConfig @"showSplashAdWithShowConfig"

#define GetSplashAdValidAds @"getSplashValidAds"

#define EntrySplashScenario @"entrySplashScenario"

#pragma mark - funcInterstitial方法相关

#define LoadInterstitialAd @"loadInterstitialAd"

#define HasInterstitialAdReady @"hasInterstitialAdReady"

#define GetInterstitialValidAds @"getInterstitialValidAds"

#define CheckInterstitialLoadStatus @"checkInterstitialLoadStatus"

#define ShowInterstitialAd @"showInterstitialAd"

#define ShowSceneInterstitialAd  @"showSceneInterstitialAd"

#define ShowInterstitialAdWithShowConfig  @"showInterstitialAdWithShowConfig"

#define EntryInterstitialScenario @"entryInterstitialScenario"

#define AutoLoadInterstitialAD @"autoLoadInterstitialAD"

#define CancelAutoLoadInterstitialAD @"cancelAutoLoadInterstitialAD"

#define ShowAutoLoadInterstitialADWithPlacementID @"showAutoLoadInterstitialAD"

#define AutoLoadInterstitialADSetLocalExtra @"autoLoadInterstitialADSetLocalExtra"

#pragma mark - funcBanner方法相关

#define BannerPlatformView @"at_banner_platform_view"

#define NativePlatformView @"at_native_platform_view"

#define LoadBannerAd @"loadBannerAd"

#define BannerAdReady @"bannerAdReady"

#define CheckBannerLoadStatus @"checkBannerLoadStatus"

#define GetBannerValidAds @"getBannerValidAds"

#define ShowBannerInRectangle @"showBannerInRectangle"

#define ShowAdInPosition @"showAdInPosition"

#define ShowSceneBannerInRectangle @"showSceneBannerInRectangle"

#define ShowSceneBannerAdInPosition @"showSceneBannerAdInPosition"

#define RemoveBannerAd @"removeBannerAd"

#define HideBannerAd @"hideBannerAd"

#define AfreshShowBannerAd @"afreshShowBannerAd"

#define EntryBannerScenario @"entryBannerScenario"

#pragma mark - funcNative方法相关

#define NativeAdReady @"nativeAdReady"

#define CheckNativeAdLoadStatus @"checkNativeAdLoadStatus"

#define GetNativeValidAds @"getNativeValidAds"

#define LoadNativeAd @"loadNativeAd"

#define ShowNativeAd @"showNativeAd"

#define ShowSceneNativeAd @"showSceneNativeAd"

#define ShowSceneNativeAdWithCustomExt @"showSceneNativeAdWithCustomExt"

#define RemoveNativeAd @"removeNativeAd"

#define EntryNativeScenario @"entryNativeScenario"

#pragma mark - 原生广告相关
#define NativeSize @"size"
#define MediaViewFrame @"mediaViewFrame"

#define Parent @"parent"
#define AppIcon @"appIcon"
#define MainImage @"mainImage"
#define MainTitle @"title"
#define Desc @"desc"
#define AdLogo @"adLogo"
#define Cta @"cta"
#define Dislike @"dislike"



#define IsAdaptiveHeight @"isAdaptiveHeight"
#define NativeRenderType @"nativeRenderType"
#define NativeRenderTypeExpress @"express"
#define NativeRenderTypeSelfRender @"selfRender"

#pragma mark - 屏幕相关
#define SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(v)  ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] != NSOrderedAscending)


#define StatusBarHeight (AT_IS_IPhoneX_All ? 44.f : 20.f)

#define NavBarHeight (44.f+StatusBarHeight)

#define TabBarHeight (AT_IS_IPhoneX_All ? (49.f+34.f) : 49.f)

#define TabbarSafeBottomMargin (AT_IS_IPhoneX_All ? 34.f : 0.f)







#define SCREEN_WIDTH ([[UIScreen mainScreen] bounds].size.width)

#define SCREEN_HEIGHT [UIScreen mainScreen].bounds.size.height

#define KScaleWidth(width) ((width)*(SCREEN_WIDTH/375.f))

#define KScaleHeight(height) ((height)*(SCREEN_HEIGHT/667.0f))

#define IsIphone6P          SCREEN_WIDTH==414
#define SizeScale           (IsIphone6P ? 1.5 : 1)
#define kFontSize(value)    value*SizeScale
#define kFont(value)        [UIFont systemFontOfSize:value*SizeScale]



// 判断 iPad
#define AT_UI_IS_IPAD (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad)

// 判断iPhone X
#define AT_Is_iPhoneX ([UIScreen instancesRespondToSelector:@selector(currentMode)] ? CGSizeEqualToSize(CGSizeMake(1125, 2436), [[UIScreen mainScreen] currentMode].size) : NO)

//判断iPHoneXr | 11
#define AT_Is_iPhoneXR ([UIScreen instancesRespondToSelector:@selector(currentMode)] ? CGSizeEqualToSize(CGSizeMake(828, 1792), [[UIScreen mainScreen] currentMode].size) && !AT_UI_IS_IPAD : NO)

//判断iPHoneXs | 11Pro
#define AT_Is_iPhoneXS ([UIScreen instancesRespondToSelector:@selector(currentMode)] ? CGSizeEqualToSize(CGSizeMake(1125, 2436), [[UIScreen mainScreen] currentMode].size) && !AT_UI_IS_IPAD : NO)

//判断iPhoneXs Max | 11ProMax
#define AT_Is_iPhoneXS_MAX ([UIScreen instancesRespondToSelector:@selector(currentMode)] ? CGSizeEqualToSize(CGSizeMake(1242, 2688), [[UIScreen mainScreen] currentMode].size) && !AT_UI_IS_IPAD : NO)

//判断iPhone12_Mini
#define AT_Is_iPhone12_Mini ([UIScreen instancesRespondToSelector:@selector(currentMode)] ? CGSizeEqualToSize(CGSizeMake(1080, 2340), [[UIScreen mainScreen] currentMode].size) && !AT_UI_IS_IPAD : NO)

//判断iPhone12 | 12Pro
#define AT_Is_iPhone12 ([UIScreen instancesRespondToSelector:@selector(currentMode)] ? CGSizeEqualToSize(CGSizeMake(1170, 2532), [[UIScreen mainScreen] currentMode].size) && !AT_UI_IS_IPAD : NO)

//判断iPhone12 Pro Max
#define AT_Is_iPhone12_ProMax ([UIScreen instancesRespondToSelector:@selector(currentMode)] ? CGSizeEqualToSize(CGSizeMake(1284, 2778), [[UIScreen mainScreen] currentMode].size) && !AT_UI_IS_IPAD : NO)

//x系列
#define AT_IS_IPhoneX_All (AT_Is_iPhoneX || AT_Is_iPhoneXR || AT_Is_iPhoneXS || AT_Is_iPhoneXS_MAX || AT_Is_iPhone12_Mini || AT_Is_iPhone12 || AT_Is_iPhone12_ProMax)

