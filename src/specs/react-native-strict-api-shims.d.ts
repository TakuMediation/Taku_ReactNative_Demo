/**
 * react-native strict-api 下的「深路径模块」类型 shim —— 纯类型声明，不参与运行时/打包。
 *
 * 背景：Fabric 组件 spec（ATBannerViewNativeComponent.ts / ATNativeAdViewNativeComponent.ts）
 * 按 RN 标准写法 import 这两个深路径：
 *   · react-native/Libraries/Utilities/codegenNativeComponent
 *   · react-native/Libraries/Types/CodegenTypes（Int32 / Float / WithDefault / *EventHandler）
 * 运行时和 Codegen 用的是 RN 真实文件，没问题；但本仓库 tsconfig 开了
 * `react-native-strict-api` customCondition（只暴露 RN 公共 API），tsc 解析不到这些深路径
 * → 报 TS2307「Cannot find module」。本文件用 `declare module` 替 tsc 补上这两个模块的类型声明。
 *
 * ⚠️ 不要删除：删了 tsc 会对上述两个 spec 报 ~7 处 Cannot find module（CI 直接红）。
 *    这不是历史遗留/冗余文件，是 strict-api 模式编译的硬依赖（phase-2d Banner 起引入）。
 *    `.d.ts` 由 tsconfig include glob 自动纳入，无需 import；增删 Fabric 组件 import 的深路径时同步维护这里。
 */
declare module 'react-native/Libraries/Utilities/codegenNativeComponent' {
  import type { HostComponent } from 'react-native';
  const codegenNativeComponent: <T extends object>(
    componentName: string,
    options?: object
  ) => HostComponent<T>;
  export default codegenNativeComponent;
}

declare module 'react-native/Libraries/Utilities/codegenNativeCommands' {
  const codegenNativeCommands: <T extends object>(options: {
    supportedCommands: ReadonlyArray<keyof T>;
  }) => T;
  export default codegenNativeCommands;
}

declare module 'react-native/Libraries/Types/CodegenTypes' {
  export type Int32 = number;
  export type Float = number;
  export type Double = number;
  export type UnsafeObject = object;
  // RN 真实签名是 WithDefault<Type, DefaultValue> 两个参数（组件 spec 里写 WithDefault<boolean, false> 这种）。
  // 本 shim 只用第一个参数，但第二个必须保留以匹配那个调用形状（删了 spec 里的两参写法会报错）；
  // 因第二参在本声明体内未用，故关掉未用告警。
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  export type WithDefault<T, _D> = T | undefined;
  export type DirectEventHandler<T> = (event: {
    readonly nativeEvent: T;
  }) => void;
  export type BubblingEventHandler<T> = (event: {
    readonly nativeEvent: T;
  }) => void;
}
