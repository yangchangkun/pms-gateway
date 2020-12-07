package com.pms.app.common;

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
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrMemberJoinService;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.prefer.domain.PreferAppModule;
import com.pms.core.prefer.service.IPreferAppModuleService;
import com.pms.core.system.domain.SysDictData;
import com.pms.core.system.domain.SysUser;
import com.pms.core.system.service.ISysDictDataService;
import com.pms.core.system.service.ISysUserService;
import com.pms.framework.web.base.AppBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 数据字典获取接口
 */
@Controller
@RequestMapping("/app/common/dict")
public class SysDictDataController extends AppBaseController {
    private String TAG = this.getClass().getSimpleName();

    @Autowired
    private ISysDictDataService dictDataService;

    /**
     * 系统初始化接口
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("")
    @ResponseBody
    @GatewayAuth(GatewayConstants.un_require_login)
    public AjaxResult init(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String appUserId = getAppUserId(parameterMap);

        String dictTypes = Tool.convertObject(parameterMap.get("dictTypes"));

        Map<String,Object> map = new HashMap<>();
        if(dictTypes != null){
            String[] dts = dictTypes.split(",");
            for(String dt : dts){
                List<SysDictData> list = dictDataService.selectDictDataByType(dt);
                map.put(dt,list);
            }
        }

        return ok().put("dataMap", map);

    }

}
