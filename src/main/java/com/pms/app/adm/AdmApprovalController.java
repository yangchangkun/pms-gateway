package com.pms.app.adm;

import com.pms.activiti.config.ActBusinessStateEnum;
import com.pms.activiti.config.ActBusinessTypeEnum;
import com.pms.activiti.config.ActBusinessVariablesEnum;
import com.pms.activiti.domain.Act;
import com.pms.activiti.service.impl.ActBusinessServiceImpl;
import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.activiti.service.IActivitiQueryService;
import com.pms.core.adm.domain.AdmCardApply;
import com.pms.core.adm.domain.AdmPassengerTicket;
import com.pms.core.adm.domain.AdmSealApply;
import com.pms.core.adm.service.IAdmCardApplyService;
import com.pms.core.adm.service.IAdmPassengerTicketService;
import com.pms.core.adm.service.IAdmSealApplyService;
import com.pms.core.system.domain.SysUser;
import com.pms.core.system.service.ISysUserService;
import com.pms.framework.web.base.AppBaseController;
import org.activiti.engine.task.Task;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 考勤审批
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/adm/approval")
public class AdmApprovalController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private ISysUserService sysUserService;
	@Autowired
	private ActBusinessServiceImpl actBusinessServiceImpl;

	@Autowired
	private IActivitiQueryService activitiQueryService;

	/**
	 * 我的待办列表
	 * @return
	 */
	@RequestMapping("/todoList")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult todoList(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		String businessType = Tool.convertObject(parameterMap.get("businessType"));

		SysUser user = sysUserService.selectUserById(userId);
		String account = user.getAccount();

		List<Act> list = actBusinessServiceImpl.searchTodoTask(account, businessType);

		int myTodoCnt = 0;
		Map<String, Integer> myTodoCntMap = activitiQueryService.countMyTodoListGroupBy(user.getAccount());
		myTodoCnt += myTodoCntMap.get("attendance_cnt");
		myTodoCnt += myTodoCntMap.get("hr_cnt");
		myTodoCnt += myTodoCntMap.get("adm_cnt");

		return super.ok().put("gridDatas", list).put("myTodoCntMap", myTodoCntMap).put("myTodoCnt", myTodoCnt);
	}

	/**
	 * 审批处理
	 * @return
	 */
	@RequestMapping("/handle")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult handle(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String taskId = Tool.convertObject(parameterMap.get("taskId"));
		logger.info( "handle.taskId======="+taskId);
		int opinion = Tool.convertStringToInt(parameterMap.get("opinion"));
		String memo = Tool.convertObject(parameterMap.get("memo"));

		SysUser user = sysUserService.selectUserById(userId);
		String account = user.getAccount();

		if(StringUtils.isEmpty(taskId)){
			return super.error("审批的任务ID不能为空");
		}

		Task task = actBusinessServiceImpl.getTask(taskId);
		if(task==null){
			return super.error("由于他人已经审批或申请人撤销申请，您的审批任务已经不存在.");
		}

		actBusinessServiceImpl.completeTask(account, user.getUserName(), taskId, opinion, memo);

		return super.ok();
	}


}
