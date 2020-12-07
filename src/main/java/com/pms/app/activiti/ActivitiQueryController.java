package com.pms.app.activiti;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pms.activiti.domain.Act;
import com.pms.activiti.service.impl.ActBusinessServiceImpl;
import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.activiti.service.IActivitiQueryService;
import com.pms.core.attendance.domain.OaDailyPaperEntity;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.system.domain.SysUser;
import com.pms.core.system.service.ISysUserService;
import com.pms.core.workflow.service.IWorkflowCcService;
import com.pms.framework.web.base.AppBaseController;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考勤审批
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/activiti/query")
public class ActivitiQueryController extends AppBaseController {

	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IActivitiQueryService activitiQueryService;

	@Autowired
	private IWorkflowCcService workflowCcService;

	@Autowired
	private IStaffInfoService staffInfoService;

	/**
	 * 统计我的任务数量
	 * @return
	 */
	@RequestMapping("/count")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult count(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		StaffInfo staff = staffInfoService.selectEntityById(userId);
		String account = staff.getAccount();

		int myApplyCnt = activitiQueryService.countMyApplyList(account);
		int myTodoCnt = activitiQueryService.countMyTodoList(account);
		int myDoneCnt = activitiQueryService.countMyDoneList(account);
		int myUnReadCcCnt = workflowCcService.countCcStaffByAccount(account); //我的未读抄送信息数

		return super.ok().put("myApplyCnt", myApplyCnt).put("myTodoCnt", myTodoCnt).put("myDoneCnt", myDoneCnt).put("myUnReadCcCnt", myUnReadCcCnt);
	}

	/**
	 * 我的申请列表
	 * @return
	 */
	@RequestMapping("/myApplyList")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult myApplyList(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		StaffInfo staff = staffInfoService.selectEntityById(userId);

		String account = staff.getAccount();
		String businessType = Tool.convertObject(parameterMap.get("businessType"));
		String searchKey = Tool.convertObject(parameterMap.get("searchKey"));
		String fromDate = Tool.convertObject(parameterMap.get("fromDate"));
		String toDate = Tool.convertObject(parameterMap.get("toDate"));

		int pageNum = Tool.convertStringToInt(parameterMap.get("pageNum"));
		if(pageNum<=0){
			pageNum = 1;
		}
		int pageSize = Tool.convertStringToInt(parameterMap.get("pageSize"));
		if(pageSize<=0 || pageSize>50){
			pageSize = 10;
		}
		String orderBy = " vfhd.happen_day DESC, ahp.id_ DESC ";

		Map<String, Object> params = new HashMap<>();
		params.put("account", account);
		params.put("businessType", businessType);
		params.put("searchKey", searchKey);
		params.put("fromDate", fromDate);
		params.put("toDate", toDate);

		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<Map<String, Object>> list = activitiQueryService.selectMyApplyList(params);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());

		return super.ok().put("gridDatas", list).put("webPage", page);
	}

	/**
	 * 我的待办列表
	 * @return
	 */
	@RequestMapping("/myTodoList")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult myTodoList(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		StaffInfo staff = staffInfoService.selectEntityById(userId);

		String assignee = staff.getAccount();
		String account = Tool.convertObject(parameterMap.get("account"));
		String businessType = Tool.convertObject(parameterMap.get("businessType"));
		String searchKey = Tool.convertObject(parameterMap.get("searchKey"));
		String fromDate = Tool.convertObject(parameterMap.get("fromDate"));
		String toDate = Tool.convertObject(parameterMap.get("toDate"));

		int pageNum = Tool.convertStringToInt(parameterMap.get("pageNum"));
		if(pageNum<=0){
			pageNum = 1;
		}
		int pageSize = Tool.convertStringToInt(parameterMap.get("pageSize"));
		if(pageSize<=0 || pageSize>50){
			pageSize = 10;
		}
		String orderBy = " vfhd.happen_day DESC, ari.id_ desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("userType", "candidate");
		params.put("assignee", assignee);
		params.put("account", account);
		params.put("businessType", businessType);
		params.put("searchKey", searchKey);
		params.put("fromDate", fromDate);
		params.put("toDate", toDate);

		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<Map<String, Object>> list = activitiQueryService.selectMyTodoList(params);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());

		return super.ok().put("gridDatas", list).put("webPage", page);
	}

	/**
	 * 我的已办列表
	 * @return
	 */
	@RequestMapping("/myDoneList")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult myDoneList(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		StaffInfo staff = staffInfoService.selectEntityById(userId);

		String assignee = staff.getAccount();
		String account = Tool.convertObject(parameterMap.get("account"));
		String businessType = Tool.convertObject(parameterMap.get("businessType"));
		String searchKey = Tool.convertObject(parameterMap.get("searchKey"));
		String fromDate = Tool.convertObject(parameterMap.get("fromDate"));
		String toDate = Tool.convertObject(parameterMap.get("toDate"));

		int pageNum = Tool.convertStringToInt(parameterMap.get("pageNum"));
		if(pageNum<=0){
			pageNum = 1;
		}
		int pageSize = Tool.convertStringToInt(parameterMap.get("pageSize"));
		if(pageNum<=0){
			pageNum = 1;
		}
		if(pageSize<=0 || pageSize>50){
			pageSize = 10;
		}
		String orderBy = " vfhd.happen_day DESC, aht.id_ desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("pageNum", pageNum);
		params.put("pageSize", pageSize);
		params.put("assignee", assignee);
		params.put("account", account);
		params.put("businessType", businessType);
		params.put("searchKey", searchKey);
		params.put("fromDate", fromDate);
		params.put("toDate", toDate);

		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<Map<String, Object>> list = activitiQueryService.selectMyDoneList(params);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());


		return super.ok().put("gridDatas", list).put("webPage", page);
	}

	/**
	 * 我的抄送列表
	 * @return
	 */
	@RequestMapping("/ccToMeList")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult ccToMeList(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		StaffInfo staff = staffInfoService.selectEntityById(userId);

		String account =  staff.getAccount();
		String businessType = Tool.convertObject(parameterMap.get("businessType"));
		String searchKey = Tool.convertObject(parameterMap.get("searchKey"));
		String fromDate = Tool.convertObject(parameterMap.get("fromDate"));
		String toDate = Tool.convertObject(parameterMap.get("toDate"));

		int pageNum = Tool.convertStringToInt(parameterMap.get("pageNum"));
		if(pageNum<=0){
			pageNum = 1;
		}
		int pageSize = Tool.convertStringToInt(parameterMap.get("pageSize"));
		if(pageSize<=0 || pageSize>50){
			pageSize = 10;
		}
		String orderBy = " cc.state asc, cc.id desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("account", account);
		params.put("businessType", businessType);
		params.put("searchKey", searchKey);
		params.put("fromDate", fromDate);
		params.put("toDate", toDate);

		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<Map<String, Object>> list = activitiQueryService.selectCcToMeList(params);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());

		return super.ok().put("gridDatas", list).put("webPage", page);
	}

	/**
	 * 将抄送给我的消息设置为已读
	 * @return
	 */
	@RequestMapping("/setCcState")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult setCcState(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		StaffInfo staff = staffInfoService.selectEntityById(userId);

		String account =  staff.getAccount();
		String id = Tool.convertObject(parameterMap.get("id"));

		workflowCcService.setReadState(account, id);

		return super.ok();
	}

}
