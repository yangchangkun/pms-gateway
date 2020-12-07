package com.pms.gateway.interceptor;

import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.Constants;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.constant.UserConstants;
import com.pms.common.utils.StringUtils;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.common.utils.jwt.JwtTokenUtils;
import com.pms.core.system.domain.SysUser;
import com.pms.core.system.service.ISysUserService;
import com.pms.core.system.service.ISysUserTokenService;
import com.pms.framework.web.exception.gateway.ApiAccountBlockException;
import com.pms.framework.web.exception.gateway.ApiUnLoginException;
import com.pms.framework.web.exception.gateway.ApiUnRegisterException;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

/**
 * 拦截器
 * @author yangchangkun
 * @create 2019-06-18 16:18
 */
@Component
public class GatewayInterceptor implements HandlerInterceptor {

    protected Logger logger = LoggerFactory.getLogger(getClass());
    private String TAG = this.getClass().getSimpleName();

    @Autowired
    private ISysUserService sysUserService;
    @Autowired
    private ISysUserTokenService sysUserTokenService;

    /**
     * 进入controller层之前拦截请求
     * @param httpServletRequest
     * @param httpServletResponse
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler) throws Exception {
        logger.info("---------------------GatewayInterceptor.preHandle----------------------------");

        showRequestInfo(httpServletRequest);

        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(httpServletRequest);
        /**
         * 排除sign字段的参数
         */
        //Map<String, Object> unSignMap = RequestUtil.paramToMapSign(httpServletRequest);

        /**
         * 验证签名
         */
        /*if(!parameterMap.containsKey(GatewayConstants.sign)){
            throw new ApiSignException();
        }
        String clientSign = Tool.convertObject(parameterMap.get(GatewayConstants.sign));
        logger.info("客户端签名 clientSign =》 "+clientSign);
        String serverSign = RequestUtil.createMD5Sign(unSignMap);
        logger.info("服务端签名 serverSign =》 "+serverSign);
        boolean checkSignResult = clientSign.equals(serverSign);
        logger.info("签名对比结果 checkSignResult =》 "+checkSignResult);

        if(!checkSignResult){
            throw new ApiSignException();
        }*/

        int result = loginHandler(httpServletRequest, parameterMap, handler);
        logger.info("登录判断结果 result[0:通过] =》 "+result);
        if(result==1){
            //throw new ApiUnLoginException();
            throw new RuntimeException("您尚未登录,请先登录");
        } else if(result==2){
            //throw new ApiIllegalTokenException();
            //org.apache.http.HttpStatus.SC_UNAUTHORIZED = 401;
            //如果token不存在或已过去，直接返回401
            AjaxResult r = AjaxResult.error(HttpStatus.SC_UNAUTHORIZED, "invalid token");
            String json = com.alibaba.fastjson.JSON.toJSONString(r);
            httpServletResponse.getWriter().print(json);
            return false;
        } else if(result==3){
            //throw new ApiUnRegisterException();
            throw new RuntimeException("您尚未注册,请联系管理员.");
        } else if(result==4){
            //throw new ApiAccountBlockException();
            throw new RuntimeException("您的账号已经被禁用,请联系管理员.");
        } else if(result==5){
            throw new Exception();
        }

        return true;

    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {
        logger.info("--------------处理请求完成后视图渲染之前的处理操作---------------");
    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
        logger.info("---------------视图渲染之后的操作-------------------------");
        showResponseInfo(httpServletResponse);
    }


    /**
     * 显示请求信息
     * @param request
     */
    private void showRequestInfo(HttpServletRequest request){
        /**
         * 打印路径信息
         */
        logger.info("Request path：getServerName = " + request.getServerName());
        logger.info("Request path：getServerPort = " + request.getServerPort());
        logger.info("Request path：getContextPath = " + request.getContextPath());
        logger.info("Request path：getServletPath = " + request.getServletPath());
        logger.info("Request path：getQueryString = " + request.getQueryString());
        logger.info("Request path：getRequestURI = " + request.getRequestURI());
        logger.info("Request path：getRequestURL = " + request.getRequestURL());
        logger.info("Request path：getRemoteAddr = " + request.getRemoteAddr());

        /**
         * 打印头信息
         */
        Enumeration hNames = request.getHeaderNames();
        while (hNames.hasMoreElements()) {
            String name = (String) hNames.nextElement();
            String value = request.getHeader(name);
            logger.info("Request header：field name = " + name + " => value = " + value);
        }

        /**
         * 打印参数
         */
       /* Enumeration pNames = request.getParameterNames();
        while (pNames.hasMoreElements()) {
            String name = (String) pNames.nextElement();
            String value = request.getParameter(name);
            logger.info("Request parameter：field name = " + name + " => value = " + value);
        }*/

        Map rMap = request.getParameterMap();
        Iterator rIter = rMap.keySet().iterator();

        while (rIter.hasNext()) {
            Object key = rIter.next();
            String value = request.getParameter(key.toString());
            if (key == null || value == null){
                continue;
            }
            logger.info("Request parameter：field name = " + key + " => value = " + value);
        }

    }

    /**
     * 显示响应信息
     * @param response
     */
    private void showResponseInfo(HttpServletResponse response){
        logger.info("response status value => " + response.getStatus());
        /**
         * 打印头信息
         */
        Collection<String> names = response.getHeaderNames();
        for(String name : names){
            String value = response.getHeader(name);
            logger.info("response header：field name = " + name + " value = " + value);
        }

    }

    /**
     * 是否有权限
     *
     * @param handler
     * @return 0 正常；1 未登录；2token过期；3 未注册; 4 账号封禁 5 系统异常(通常是注解没有具体的参数值)
     */
    private int loginHandler(HttpServletRequest httpServletRequest, Map<String, Object> parameterMap, Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            // 获取方法上的注解
            GatewayAuth gatewayAuth = handlerMethod.getMethod().getAnnotation(GatewayAuth.class);
            // 如果方法上的注解为空 则获取类的注解
            if (gatewayAuth == null) {
                gatewayAuth = handlerMethod.getMethod().getDeclaringClass().getAnnotation(GatewayAuth.class);
            }
            if(gatewayAuth ==null){
                return 0;
            }
            /**
             * 得到注册参数
             */
            String annParam = gatewayAuth.value();
            if(StringUtils.isEmpty(annParam)){
                return 5;
            }
            if(annParam.equals(GatewayConstants.un_require_login)){
                return 0;
            }

            String token = Tool.convertObject(parameterMap.get(GatewayConstants.token));
            if (StringUtils.isEmpty(token)) {
                return 1;
            }

            try {
                JwtTokenUtils.verifyToken(token);
            } catch (Exception e){
                logger.error(TAG, e);
                e.printStackTrace();
                return 2;
            }
            String userId = JwtTokenUtils.getUserId(token);
            if(StringUtils.isEmpty(userId)){
                return 2;
            }

            SysUser user = sysUserService.selectUserById(userId);
            if(user==null){
                return 3;
            }
            if(user.getStatus()==0){
                return 4;
            }

        }
        return 0;
    }

}
