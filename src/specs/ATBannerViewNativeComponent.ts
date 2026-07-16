import type { HostComponent, ViewProps } from 'react-native';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import type { Int32 } from 'react-native/Libraries/Types/CodegenTypes';

/**
 * Banner 组件 spec。深路径 import codegenNativeComponent：RN 0.72 不从主入口导出，
 * 走深路径两架构都可用（New Arch codegen 解析；Paper 运行时回退 requireNativeComponent）。
 */
export interface NativeProps extends ViewProps {
  placementID: string;
  isAdaptiveHeight?: boolean;
  adWidth?: Int32;
  adHeight?: Int32;
}

export default codegenNativeComponent<NativeProps>(
  'ATBannerView'
) as HostComponent<NativeProps>;
