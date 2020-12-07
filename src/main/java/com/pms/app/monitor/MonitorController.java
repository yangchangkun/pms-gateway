package com.pms.app.monitor;

import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
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
import com.pms.core.system.domain.SysUser;
import com.pms.core.system.service.ISysUserService;
import com.pms.framework.web.base.AppBaseController;
import com.pms.framework.web.domain.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Map;


/**
 * 服务监控
 */
@Controller
@RequestMapping("/app/monitor/server")
public class MonitorController extends AppBaseController {
    private String TAG = this.getClass().getSimpleName();

    /**
     * 查询参数配置列表
     */
    @RequestMapping("/detail")
    @ResponseBody
    @GatewayAuth(GatewayConstants.un_require_login)
    public AjaxResult detail() throws Exception
    {
        Server server = new Server();
        server.copyTo();
        return super.ok().put("server", server);
    }

}
