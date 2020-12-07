package com.pms.app.hr;

import com.pms.activiti.config.ActBusinessTypeEnum;
import com.pms.activiti.config.ActBusinessVariablesEnum;
import com.pms.activiti.domain.Act;
import com.pms.activiti.service.impl.ActBusinessServiceImpl;
import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.enums.DictTypeEnum;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.activiti.service.IActivitiQueryService;
import com.pms.core.hr.service.IHrLeaveHandoverService;
import com.pms.core.system.domain.SysDictData;
import com.pms.core.system.domain.SysUser;
import com.pms.core.system.service.ISysDictDataService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 考勤审批
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/hr/approval")
public class HrApprovalController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private ISysUserService sysUserService;
	@Autowired
	private ActBusinessServiceImpl actBusinessServiceImpl;

	@Autowired
	private ISysDictDataService sysDictDataService;

	@Autowired
	private IHrLeaveHandoverService hrLeaveHandoverService;

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
		int opinion = Tool.convertStringToInt(parameterMap.get("opinion"));
		String memo = Tool.convertObject(parameterMap.get("memo"));
		/**
		 * 以下两个字段为转正审批通过时必填
		 */
		String postLevel = Tool.convertObject(parameterMap.get("postLevel"));
		String staffTier = Tool.convertObject(parameterMap.get("staffTier"));

		SysUser user = sysUserService.selectUserById(userId);
		String account = user.getAccount();

		if(StringUtils.isEmpty(taskId)){
			return super.error("审批的任务ID不能为空");
		}

		Task task = actBusinessServiceImpl.getTask(taskId);
		if(task==null){
			return super.error("由于他人已经审批或申请人撤销申请，您的审批任务已经不存在.");
		}
		Map<String, Object> paramMap = actBusinessServiceImpl.searchActHistoryVar(task.getProcessInstanceId());
		String businessId = Tool.convertObject(paramMap.get(ActBusinessVariablesEnum.businessId.getCode()));
		String businessType = Tool.convertObject(paramMap.get(ActBusinessVariablesEnum.businessType.getCode()));
		if(StringUtils.isNotEmpty(businessType) && (businessType.equals(ActBusinessTypeEnum.hr_join_apply.getCode()) || businessType.equals(ActBusinessTypeEnum.hr_leave_apply.getCode()) || businessType.equals(ActBusinessTypeEnum.hr_leave_handover.getCode()))){
			if(StringUtils.isEmpty(memo)){
				return super.error("审批意见必须填写");
			}
		}
		if(StringUtils.isNotEmpty(businessType) && (businessType.equals(ActBusinessTypeEnum.hr_join_apply.getCode()))){
			if(opinion==0){
				if(StringUtils.isEmpty(postLevel)){
					return super.error("请选择岗位级别");
				}
				if(StringUtils.isEmpty(staffTier) || staffTier.equals("0")){
					return super.error("请选择职员层级");
				}
			}
		}

		/**
		 * 离职交接会签阶段
		 * 只要有一人驳回，则整个流程结束
		 */
		boolean countersignOver = false;
		if(StringUtils.isNotEmpty(businessType) && businessType.equals(ActBusinessTypeEnum.hr_leave_handover.getCode())){
			if(opinion==1){

				/**
				 * 分配会签任务的人员
				 */
				List<String> assigneeList = new ArrayList<String>();
				List<SysDictData> dictList = sysDictDataService.selectDictDataByType(DictTypeEnum.hr_leave_handler.getCode());
				if(dictList!=null && dictList.size()>0){
					for(SysDictData sdd : dictList){
						assigneeList.add(sdd.getDictValue());
					}
				} else {
					assigneeList.add("gaoleiming");
					assigneeList.add("liuhaiying");
					assigneeList.add("chenxiyao");
					assigneeList.add("wangjunxia");
					assigneeList.add("huhaiyan");
					assigneeList.add("heyanchan");
					assigneeList.add("wengguohai");
				}

				if(assigneeList.contains(account)){
					countersignOver = true;
				}
			}
		}

		if(StringUtils.isNotEmpty(businessType) && (businessType.equals(ActBusinessTypeEnum.hr_join_apply.getCode()))){
			actBusinessServiceImpl.completeJoinApplyTask(account, user.getUserName(), taskId, opinion, memo, postLevel, staffTier);
		} else {
			actBusinessServiceImpl.completeTask(account, user.getUserName(), taskId, opinion, memo);
		}

		if(countersignOver){
			actBusinessServiceImpl.deleteProcessInstance(task.getProcessInstanceId(), memo);
			hrLeaveHandoverService.updateState(opinion, businessId);
		}


		return super.ok();
	}


}
