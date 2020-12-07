package com.pms.app.common;

import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.constant.UserConstants;
import com.pms.common.service.QytSsoLoginService;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.StringUtils;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.common.utils.jwt.JwtTokenUtils;
import com.pms.core.activiti.service.IActivitiQueryService;
import com.pms.core.adm.service.IAdmNoticeReaderService;
import com.pms.core.hr.domain.HrMemberJoin;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrMemberJoinService;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.prefer.domain.PreferAppModule;
import com.pms.core.prefer.service.IPreferAppModuleService;
import com.pms.core.system.domain.SysUser;
import com.pms.core.system.service.ISysUserService;
import com.pms.framework.web.base.AppBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * 系统登录接口
 */
@Controller
@RequestMapping("/app/common")
public class SysLoginController extends AppBaseController {
    private String TAG = this.getClass().getSimpleName();

    @Autowired
    private IHrMemberJoinService hrMemberJoinService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private IStaffInfoService staffInfoService;

    @Autowired
    private QytSsoLoginService qytSsoLoginService;

    /**
     * 系统初始化接口
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("login")
    @ResponseBody
    @GatewayAuth(GatewayConstants.un_require_login)
    public AjaxResult login(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String account = Tool.convertObject(parameterMap.get("account"));
        String password = Tool.convertObject(parameterMap.get("password"));

        if(StringUtils.isEmpty(account) || StringUtils.isEmpty(password)){
            return AjaxResult.error("请输入账号和密码");
        }
        //用户信息
        SysUser user = userService.selectUserByAccount(account);

        //账号不存在、密码错误
        if(user == null) {
            return AjaxResult.error("账号或密码错误");
        }

        //账号锁定
        if(user.getStatus().toString().equals(UserConstants.USER_BLOCKED)){
            return AjaxResult.error("账号已被锁定,请联系管理员");
        }

        String appUserId = user.getUserId();

        /**
         * 查询关联的会员ID
         */
        HrMemberJoin hrMemberJoin = hrMemberJoinService.selectEntityByUserId(appUserId);
        if(hrMemberJoin==null){
            return AjaxResult.error("该账号未加入企业");
        }
        String memberId = hrMemberJoin.getMemberId();

        AjaxResult AR = qytSsoLoginService.ssoLogin(hrMemberJoin.getMemberId(), appUserId, password);
        String ssoCode = Tool.convertObject(AR.get("code"));
        if(StringUtils.isEmpty(ssoCode) || !ssoCode.equals("0")){
            return AjaxResult.error("统一登录认证失败,请联系企易通平台.");
        }

        /**
         * 创建JWT token
         */
        String token = JwtTokenUtils.createToken(appUserId);

        StaffInfo staffInfo = staffInfoService.selectByUserId(appUserId);

        return super.ok().put(GatewayConstants.pmsUserId, appUserId)
                .put(GatewayConstants.token, token)
                .put("memberId", memberId)
                .put("staffInfo", staffInfo)
                ;
    }

}
