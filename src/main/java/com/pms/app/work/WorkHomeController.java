package com.pms.app.work;

import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.activiti.service.IActivitiQueryService;
import com.pms.core.adm.service.IAdmNoticeReaderService;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.system.service.ISysUserService;
import com.pms.framework.web.base.AppBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 工作台首页
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/work/home")
public class WorkHomeController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private ISysUserService userService;

	@Autowired
	private IStaffInfoService staffInfoService;

	@Autowired
	private IActivitiQueryService activitiQueryService;

	@Autowired
	private IAdmNoticeReaderService admNoticeReaderService;

	/**
	 * 获取角标
	 * @return
	 */
	@RequestMapping("/badge")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult badge(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String appUserId = getAppUserId(parameterMap);

		StaffInfo staff = staffInfoService.selectByUserId(appUserId);

		int myTodoCnt = 0;
		Map<String, Integer> myTodoCntMap = activitiQueryService.countMyTodoListGroupBy(staff.getAccount());
		myTodoCnt += myTodoCntMap.get("attendance_cnt");
		myTodoCnt += myTodoCntMap.get("hr_cnt");
		myTodoCnt += myTodoCntMap.get("adm_cnt");

		/**
		 * 未读通知数
		 */
		int unReaderNoticeCnt = admNoticeReaderService.selectUnReaderCnt(appUserId);

		return super.ok().put(GatewayConstants.pmsUserId, appUserId)
				.put("myTodoCntMap", myTodoCntMap)
				.put("myTodoCnt", myTodoCnt)
				.put("unReaderNoticeCnt", unReaderNoticeCnt);
	}

}
