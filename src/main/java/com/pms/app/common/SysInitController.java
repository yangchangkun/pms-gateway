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
 * 系统初始化接口
 */
@Controller
@RequestMapping("/app/common/init")
public class SysInitController extends AppBaseController {
    private String TAG = this.getClass().getSimpleName();

    @Autowired
    private IHrMemberJoinService hrMemberJoinService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private IStaffInfoService staffInfoService;

    @Autowired
    private IActivitiQueryService activitiQueryService;

    @Autowired
    private IAdmNoticeReaderService admNoticeReaderService;

    @Autowired
    private IPreferAppModuleService preferAppModuleService;

    /**
     * 系统初始化接口
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult init(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String memberId = Tool.convertObject(parameterMap.get("memberId"));

        /**
         * 使用企易通平台的会员ID换取数字化平台的userId
         */
        Map<String, Object> applyRecord = hrMemberJoinService.selectCurrMemberInfo(memberId);
        if(applyRecord==null || applyRecord.isEmpty()){
            return super.error("你尚未正式加入该企业或您的账号已经被封禁.");
        }
        String appUserId = Tool.convertObject(applyRecord.get("userId"));
        String state = Tool.convertObject(applyRecord.get("state"));
        if(!state.equals("1")){
            return super.error("你尚未正式加入该企业");
        }

        /**
         * 创建JWT token
         */
        String token = JwtTokenUtils.createToken(appUserId);

        SysUser user = userService.selectUserById(appUserId);
        if(user==null){
            return super.error("不存在用户信息");
        }

        StaffInfo staff = staffInfoService.selectByUserId(appUserId);

        /**
         * 资料是否完整 0 完整，1 不完整
         */
        String intactFlag = "0";
//        Date accountCreateTime = user.getCreateTime();
//        /**
//         * todo 6月21号之后创建的账号需要判断资料是否完整,如果不完整,客户端需要引导职员去补充资料
//         */
//        Date borderTime = DateUtils.conversion("2020-06-21");
//        if(accountCreateTime!=null && DateUtils.dateSubtraction(borderTime, accountCreateTime)>0){
//            if(staff==null
//                    || StringUtils.isEmpty(staff.getUserId())
//                    || StringUtils.isEmpty(staff.getJoinDeptId())
//                    || staff.getJoinDate()==null
//                    || staff.getBirthday()==null
//                    || StringUtils.isEmpty(staff.getEduType())
//                    || StringUtils.isEmpty(staff.getEduDate())
//                    || StringUtils.isEmpty(staff.getWorkBeginDate())
//                    || StringUtils.isEmpty(staff.getIcNum())
//                    || StringUtils.isEmpty(staff.getInsureAccount())
//                    || StringUtils.isEmpty(staff.getInsureBank())
//                    ){
//                intactFlag = "1";
//            }
//        }

        String today = DateUtils.getCurrentDay();
        String week = DateUtils.getWeekday(today);
        String currDate = today + " " + week;



        int myTodoCnt = 0;
        Map<String, Integer> myTodoCntMap = activitiQueryService.countMyTodoListGroupBy(user.getAccount());
        myTodoCnt += myTodoCntMap.get("attendance_cnt");
        myTodoCnt += myTodoCntMap.get("hr_cnt");
        myTodoCnt += myTodoCntMap.get("adm_cnt");

        /**
         * 未读通知数
         */
        int unReaderNoticeCnt = admNoticeReaderService.selectUnReaderCnt(appUserId);

        List<PreferAppModule> appModuleList = preferAppModuleService.selectAppModuleBelongStaff(appUserId);

        return super.ok().put(GatewayConstants.pmsUserId, appUserId)
                .put(GatewayConstants.token, token)
                .put("staffInfo", staff)
                .put("intactFlag", intactFlag)
                .put("currDate", currDate)
                .put("myTodoCntMap", myTodoCntMap)
                .put("myTodoCnt", myTodoCnt)
                .put("unReaderNoticeCnt", unReaderNoticeCnt)
                .put("appModuleList", appModuleList);
    }

}
