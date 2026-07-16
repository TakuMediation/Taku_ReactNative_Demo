import type { HostComponent, ViewProps } from 'react-native';
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import type { Int32 } from 'react-native/Libraries/Types/CodegenTypes';

/**
 * Native Fabric 组件 spec（Codegen 输入；codegenConfig type:"all"）。
 * 深路径 import：codegenNativeCommands/codegenNativeComponent 在 RN 0.72 不从主入口导出，
 * 走深路径两架构都可用（New Arch codegen 静态解析此字面调用；Paper 运行时回退 requireNativeComponent）。
 */
export interface NativeProps extends ViewProps {
  placementID: string;
  adId?: string;
  isAdaptiveHeight?: boolean;
  adWidth?: Int32;
  adHeight?: Int32;
}

type ATNativeAdViewNativeComponentType = HostComponent<NativeProps>;

interface NativeCommands {
  updateAssetView(
    viewRef: React.ElementRef<ATNativeAdViewNativeComponentType>,
    assetViewTag: Int32,
    assetViewName: string
  ): void;
  renderNativeAd(
    viewRef: React.ElementRef<ATNativeAdViewNativeComponentType>
  ): void;
}

export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
  supportedCommands: ['updateAssetView', 'renderNativeAd'],
});

export default codegenNativeComponent<NativeProps>(
  'ATNativeAdView'
) as HostComponent<NativeProps>;
