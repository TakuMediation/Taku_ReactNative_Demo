import { useEffect, useRef } from 'react';
import type { ViewProps } from 'react-native';
import NativeBanner from '../specs/ATBannerViewNativeComponent';
import { ATAdEvents } from '../events/ATAdEvents';
import type { ATAdEventPayload } from '../types';

/**
 * Banner Fabric 组件封装。
 * 位置/布局由 RN style 控制；width/height 为请求广告尺寸（dp）。
 * 事件 V1 走唯一 BannerCall 通道，按本视图 placementID 过滤后回调 onAdEvent。
 */
export interface ATBannerViewProps extends ViewProps {
  placementID: string;
  /** 请求广告宽(dp)。 */
  width: number;
  /** 请求广告高(dp)；isAdaptiveHeight 时可忽略。 */
  height: number;
  isAdaptiveHeight?: boolean;
  onAdEvent?: (payload: ATAdEventPayload) => void;
}

export function ATBannerViewComponent(props: ATBannerViewProps) {
  const {
    placementID,
    width,
    height,
    isAdaptiveHeight,
    onAdEvent,
    style,
    ...rest
  } = props;

  const cb = useRef(onAdEvent);
  cb.current = onAdEvent;

  useEffect(() => {
    ATAdEvents.setAdListener(placementID, (p: ATAdEventPayload) =>
      cb.current?.(p)
    );
    return () => ATAdEvents.removeAdListener(placementID);
  }, [placementID]);

  return (
    <NativeBanner
      placementID={placementID}
      adWidth={width}
      adHeight={height}
      isAdaptiveHeight={isAdaptiveHeight}
      style={[{ width, height: isAdaptiveHeight ? undefined : height }, style]}
      {...rest}
    />
  );
}
