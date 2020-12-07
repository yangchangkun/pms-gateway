package com.pms.app.hr;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pms.activiti.config.ActBusinessTypeEnum;
import com.pms.activiti.config.ActBusinessVariablesEnum;
import com.pms.activiti.config.ActProRoleEnum;
import com.pms.activiti.service.impl.ActBusinessServiceImpl;
import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.enums.DictTypeEnum;
import com.pms.common.enums.McSysCodeEnum;
import com.pms.common.service.SysPushService;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.activiti.service.IActivitiQueryService;
import com.pms.core.hr.domain.HrLeaveApply;
import com.pms.core.hr.domain.HrLeaveHandover;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrLeaveApplyService;
import com.pms.core.hr.service.IHrLeaveHandoverService;
import com.pms.core.hr.service.IHrMemberJoinService;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.prefer.service.IPreferCalendarService;
import com.pms.core.project.domain.ProBaseEntity;
import com.pms.core.project.service.impl.ProBaseService;
import com.pms.core.project.service.impl.ProTeamService;
import com.pms.core.system.domain.SysDept;
import com.pms.core.system.domain.SysDictData;
import com.pms.core.system.service.ISysDeptService;
import com.pms.core.system.service.ISysDictDataService;
import com.pms.core.system.service.ISysUserService;
import com.pms.core.workflow.domain.WorkflowCc;
import com.pms.core.workflow.domain.WorkflowOss;
import com.pms.core.workflow.service.IWorkflowCcService;
import com.pms.core.workflow.service.IWorkflowOssService;
import com.pms.drools.service.impl.DroolsBusinessServiceImpl;
import com.pms.framework.web.base.AppBaseController;

import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 离职
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/hr/leaveHandover")
public class leaveHandoverController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IHrLeaveHandoverService hrLeaveHandoverService;

	@Autowired
	private IHrLeaveApplyService hrLeaveApplyService;

	@Autowired
	private ProBaseService proBaseService;
	@Autowired
	private ProTeamService proTeamService;

	@Autowired
	private IStaffInfoService staffInfoService;
	@Autowired
	private ISysDeptService sysDeptService;
	@Autowired
	private IHrMemberJoinService hrMemberJoinService;

	@Autowired
	private IPreferCalendarService preferCalendarService;

	@Autowired
	private ISysDictDataService sysDictDataService;

	@Autowired
	private ActBusinessServiceImpl actBusinessServiceImpl;
	@Autowired
	private IActivitiQueryService activitiQueryService;

	@Autowired
	private IWorkflowOssService workflowOssService;
	@Autowired
	private IWorkflowCcService workflowCcService;
	@Autowired
	private DroolsBusinessServiceImpl droolsBusinessServiceImpl;

	@Autowired
	private SysPushService sysPushService;

	/**
	 * 我的转正列表
	 * @return
	 */
	@RequestMapping("/init")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult init(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		StaffInfo staff = staffInfoService.selectEntityById(userId);

		HrLeaveHandover entity = new HrLeaveHandover();
		entity.setId("");
		entity.setApplyId("");
		entity.setApplyUserId(staff.getUserId());
		entity.setApplyUserName(staff.getName());
		entity.setJoinDate(staff.getJoinDate());

		//entity.setProId(proId);

		/*entity.setLastWorkDate(lastWorkDay);
		entity.setLastSalaryDate(lastSalaryDay);
		entity.setLastInsuranceDate(lastInsuranceDay);
		entity.setLastFundDate(lastFundDay);*/
		entity.setHandoverUserId("");
		entity.setHandoverUserName("");

		entity.setHandoverContent("");
		entity.setHandoverAcc("");
		entity.setHandoverAdm("");
		entity.setHandoverFinance("");
		entity.setMemo("");
		entity.setState("0");

		entity.setWorkflowId("");
		entity.setCreateBy(userId);
		entity.setCreateTime(new Date());
		//entity.setUpdateBy(userId);
		//entity.setUpdateTime(new Date());

		/**
		 * 关联最后一次离职信息
		 */
		HrLeaveApply lastLeaveApply = hrLeaveApplyService.selectLastEntityByUserId(userId);
		if(lastLeaveApply!=null && lastLeaveApply.getState().equals("2")){
			entity.setApplyId(lastLeaveApply.getId());
			entity.setLastWorkDate(lastLeaveApply.getLastWorkDate());
			entity.setLastSalaryDate(lastLeaveApply.getLastSalaryDate());
			entity.setLastInsuranceDate(lastLeaveApply.getLastInsuranceDate());
			entity.setLastFundDate(lastLeaveApply.getLastFundDate());

			entity.setProId(lastLeaveApply.getProId());
			entity.setProName(lastLeaveApply.getProName());
		}

		List<StaffInfo> staffList = staffInfoService.selectContactList("", "");
		List<Map<String, String>> handoverList = new ArrayList<>();
		if(staffList!=null && staffList.size()>0){
			for(StaffInfo s : staffList){
				Map<String, String> map = new HashMap<>();
				map.put("userId", s.getUserId());
				map.put("userName", s.getName());
				handoverList.add(map);
			}
		}

		/**
		 * 账号交接事项
		 */
		List<SysDictData> accList = sysDictDataService.selectDictDataByType(DictTypeEnum.hr_leave_handover_acc.getCode());
		/**
		 * 行政交接事项
		 */
		List<SysDictData> admList = sysDictDataService.selectDictDataByType(DictTypeEnum.hr_leave_handover_adm.getCode());

		return super.ok().put("entity", entity).put("handoverList", handoverList).put("accList", accList).put("admList", admList);
	}

	/**
	 * 列表
	 * @return
	 */
	@RequestMapping("/list")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult list(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		int pageNum = Tool.convertStringToInt(parameterMap.get("pageNum"));
		if(pageNum<=0){
			pageNum = 1;
		}
		int pageSize = Tool.convertStringToInt(parameterMap.get("pageSize"));
		if(pageSize<=0 || pageSize>50){
			pageSize = 10;
		}

		String orderBy = "hlh.create_time desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("applyUserId", userId);


		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<HrLeaveHandover> list = hrLeaveHandoverService.selectList(params);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());

		return super.ok().put("gridDatas", list).put("webPage", page);
	}

	/**
	 * 提交
	 * @return
	 */
	@RequestMapping("/add")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult add(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String applyId = Tool.convertObject(parameterMap.get("applyId"));
		String proId = Tool.convertObject(parameterMap.get("proId"));
		String lastWorkDate = Tool.convertObject(parameterMap.get("lastWorkDate"));
		String lastSalaryDate = Tool.convertObject(parameterMap.get("lastSalaryDate"));
		String lastInsuranceDate = Tool.convertObject(parameterMap.get("lastInsuranceDate"));
		String lastFundDate = Tool.convertObject(parameterMap.get("lastFundDate"));
		String handoverUserId = Tool.convertObject(parameterMap.get("handoverUserId"));
		String handoverContent = Tool.convertObject(parameterMap.get("handoverContent"));
		String handoverAcc = Tool.convertObject(parameterMap.get("handoverAcc"));
		String handoverAdm = Tool.convertObject(parameterMap.get("handoverAdm"));
		String handoverFinance = Tool.convertObject(parameterMap.get("handoverFinance"));
		String memo = Tool.convertObject(parameterMap.get("memo"));

		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

		if(StringUtils.isEmpty(proId)){
			return super.error("请选择项目,如果你未加入项目请联系项目经理");
		}
		if(StringUtils.isEmpty(lastWorkDate)){
			return super.error("请选择最后工作日期");
		}
		if(StringUtils.isEmpty(lastSalaryDate)){
			return super.error("请选择最后计薪日期");
		}
		if(StringUtils.isEmpty(lastInsuranceDate)){
			return super.error("请选择社保截止月份");
		}
		if(StringUtils.isEmpty(lastFundDate)){
			return super.error("请选择公积金截止月份");
		}
		if(StringUtils.isEmpty(handoverUserId)){
			return super.error("请选择工作交接人");
		}
		if(StringUtils.isEmpty(handoverContent)){
			return super.error("请填写工作交接内容");
		}
		if(StringUtils.isEmpty(handoverAcc)){
			return super.error("请填写账号交接事项");
		}
		if(StringUtils.isEmpty(handoverAdm)){
			return super.error("请填写行政作交接事项");
		}
		if(StringUtils.isEmpty(handoverFinance)){
			return super.error("请填写行财务交接事项,如没有请填写无");
		}
		if(StringUtils.isEmpty(memo)){
			return super.error("请填写交接备注,如没有请填写无");
		}

		StaffInfo staff = staffInfoService.selectEntityById(userId);

		Date joinDate = staff.getJoinDate();

		Date lastWorkDay = DateUtils.conversion(lastWorkDate);
		Date lastSalaryDay = DateUtils.conversion(lastSalaryDate);

		if(DateUtils.dateSubtraction(joinDate, lastWorkDay)<0){
			return super.error("离职日期不能晚于入职日期");
		}

		/**
		 * 关联最后一次离职信息
		 */
		HrLeaveApply lastLeaveApply = hrLeaveApplyService.selectLastEntityByUserId(userId);
		if(lastLeaveApply!=null && !lastLeaveApply.getState().equals("2")){
			return super.error("您没有离职申请信息或离职申请未审批通过,不能办理交接");
		}

		List<HrLeaveHandover> applyList = hrLeaveHandoverService.selectApplyingByUserId(userId);
		if(applyList!=null && applyList.size()>0){
			return super.error("您有正在处理中的申请信息,不能重复申请");
		}

		/**
		 * 判断离职日期不能在节假日内
		 */
		int year = Tool.convertStringToInt(lastWorkDate.split("-")[0]);
		Map<String, String> holidayMap = preferCalendarService.getHolidayList(year);
		if(holidayMap.containsKey(lastWorkDate)){
			return super.error("最后工作日期不能填写节假日");
		}

		StaffInfo handover = staffInfoService.selectEntityById(handoverUserId);

		/**
		 * ID
		 */
		String uuid = SnowflakeIdUtil.getId();

		HrLeaveHandover entity = new HrLeaveHandover();
		entity.setId(uuid);
		entity.setApplyId(applyId);
		entity.setApplyUserId(staff.getUserId());
		entity.setJoinDate(staff.getJoinDate());

		entity.setProId(proId);

		entity.setLastWorkDate(lastWorkDay);
		entity.setLastSalaryDate(lastSalaryDay);
		entity.setLastInsuranceDate(lastInsuranceDate);
		entity.setLastFundDate(lastFundDate);
		entity.setHandoverUserId(handoverUserId);
		entity.setHandoverContent(handoverContent);
		entity.setHandoverAcc(handoverAcc);
		entity.setHandoverAdm(handoverAdm);
		entity.setHandoverFinance(handoverFinance);
		entity.setMemo(memo);
		entity.setState("0");

		entity.setWorkflowId("");
		entity.setCreateBy(userId);
		entity.setCreateTime(new Date());
		//entity.setUpdateBy(userId);
		//entity.setUpdateTime(new Date());
		hrLeaveHandoverService.insertEntity(entity);

		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<WorkflowOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				WorkflowOss wo = new WorkflowOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setBusinessKey(ActBusinessTypeEnum.hr_leave_handover.getCode());
				wo.setBusinessId(uuid);
				wo.setOssId(ossId);
				ossDatas.add(wo);
			}
			if(ossDatas!=null && ossDatas.size()>0){
				workflowOssService.batchInsertEntity(ossDatas);
			}
		}


		/**
		 * 判断申请人在项目上的角色
		 */
		ProBaseEntity proBaseEntity = proBaseService.queryByProId(proId);
		if(proBaseEntity==null){
			return super.error("项目不存在");
		}
		String proRole = ActProRoleEnum.member.getCode();
		if(StringUtils.isNotEmpty(proBaseEntity.getProManager()) && proBaseEntity.getProManager().equals(userId)){
			proRole = ActProRoleEnum.pm.getCode();
		} else if(StringUtils.isNotEmpty(proBaseEntity.getProChief()) && proBaseEntity.getProChief().equals(userId)){
			proRole = ActProRoleEnum.pd.getCode();
		} else if(StringUtils.isNotEmpty(proBaseEntity.getProLead()) && proBaseEntity.getProLead().equals(userId)){
			proRole = ActProRoleEnum.dm.getCode();
		}

		/**
		 * 开始审批进程
		 */
		Map<String, Object> otherParam = new HashMap<>();
		otherParam.put(ActBusinessVariablesEnum.proId.getCode(), proId);
		otherParam.put(ActBusinessVariablesEnum.proRole.getCode(), proRole);
		otherParam.put(ActBusinessVariablesEnum.proManager.getCode(), Tool.convertObject(proBaseEntity.getProManager()));
		otherParam.put(ActBusinessVariablesEnum.proChief.getCode(),  Tool.convertObject(proBaseEntity.getProChief()));
		otherParam.put(ActBusinessVariablesEnum.deptManager.getCode(),  Tool.convertObject(proBaseEntity.getProLead()));

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
		otherParam.put("assigneeList", assigneeList);

		String businessMemo = staff.getName()+"申请离职交接,交接人:"+handover.getName();
		ProcessInstance pi = actBusinessServiceImpl.startProcess(staff.getAccount(), staff.getName(), ActBusinessTypeEnum.hr_leave_handover, uuid, businessMemo, otherParam);

		/**
		 * 关联表单和工作流
		 */
		hrLeaveHandoverService.updateWorkflowId(pi.getId(), uuid);

		/**
		 * 保存抄送信息
		 */
		if(StringUtils.isNotEmpty(ccAccounts)){
			List<WorkflowCc> ccDatas = new ArrayList<>();
			String[] ccArray = ccAccounts.split(",");
			for(String ccAccount : ccArray){
				WorkflowCc cc = new WorkflowCc();
				cc.setId(SnowflakeIdUtil.getId());
				cc.setBusinessKey(ActBusinessTypeEnum.hr_leave_handover.getCode());
				cc.setBusinessId(uuid);
				cc.setBusinessTitle(businessMemo);
				cc.setBusinessAccount(staff.getAccount());
				cc.setCcAccount(ccAccount);
				cc.setCreateTime(new Date());
				cc.setState(0);
				ccDatas.add(cc);
			}
			if(ccDatas!=null && ccDatas.size()>0){
				workflowCcService.batchInsertEntity(ccDatas);
			}

			/**
			 * 给主动选择的抄送人发送推送消息
			 */
			List<String> userIdList = new ArrayList<>();
			List<StaffInfo> staffList = staffInfoService.selectListByAccounts(ccArray);
			if(staffList!=null && staffList.size()>0){
				for(StaffInfo si : staffList){
					userIdList.add(si.getUserId());
				}
			}
			/**
			 * 将职员ID转换为会员ID
			 */
			List<String> memberIdList = new ArrayList<>();
			if(userIdList!=null && userIdList.size()>0){
				memberIdList = hrMemberJoinService.selectMemberIdListByUserIds(userIdList);
			}
			if(memberIdList!=null && memberIdList.size()>0){
				String pushTitle = "抄送提醒";
				String pushContent = businessMemo+".请知悉.";
				SysDept companyInfo = sysDeptService.selectCompanyEntity();
				sysPushService.sendPush(McSysCodeEnum.pmsGateway.getCode(), companyInfo.getDeptId(), memberIdList, pushTitle, pushContent);
			}
		}

		return super.ok();
	}

	/**
	 * 查看明细
	 * @return
	 */
	@RequestMapping("/detail")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult detail(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		String id = Tool.convertObject(parameterMap.get("id"));

		if(StringUtils.isEmpty(userId)){
			return super.error("userId参数不能为空");
		}
		if(StringUtils.isEmpty(id)){
			return super.error("ID参数不能为空");
		}

		HrLeaveHandover entity = hrLeaveHandoverService.selectEntityById(id);
		if(entity==null){
			return super.error("未查询到申请单");
		}
		StaffInfo staffInfo = staffInfoService.selectEntityById(entity.getCreateBy());

		List<Map<String, String>> actHis = activitiQueryService.selectHistoryFlowList(entity.getWorkflowId());
		List<WorkflowOss> ossList = workflowOssService.selectListByBusiness(ActBusinessTypeEnum.hr_leave_handover.getCode(), id);

		/**
		 * 当前审批候选人
		 */
		List<Map<String, Object>> candidateList = new ArrayList<>();
		if(entity.getState().equals("0")){
			candidateList = activitiQueryService.selectCandidateListByProcInsId(entity.getWorkflowId());
		}

		/**
		 * 查询抄送人
		 */
		List<StaffInfo> ccStaffList = droolsBusinessServiceImpl.selectWorkFlowCcStaffList(staffInfo.getAccount(), ActBusinessTypeEnum.hr_leave_handover.getCode(), id, entity.getWorkflowId(), entity.getState());

		/**
		 * 撤销标志:0 不可撤销，1 可撤销
		 */
		String revokeTag = "0";
		if(entity!=null && entity.getCreateBy().equals(userId) && entity.getState().equals("0")){
			revokeTag = "1";
		}

		return super.ok().put("entity", entity).put("actHis", actHis).put("ossList", ossList).put("candidateList", candidateList).put("revokeTag", revokeTag).put("ccStaffList", ccStaffList);
	}


	/**
	 * 撤销
	 * @return
	 */
	@RequestMapping("/revoke")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult revoke(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String id = Tool.convertObject(parameterMap.get("id"));
		String memo = Tool.convertObject(parameterMap.get("memo"));

		if(StringUtils.isEmpty(id)){
			return super.error("ID参数不能为空");
		}
		if(StringUtils.isEmpty(memo)){
			return super.error("撤销原因不能为空");
		}

		HrLeaveHandover entity = hrLeaveHandoverService.selectEntityById(id);

		actBusinessServiceImpl.deleteProcessInstance(entity.getWorkflowId(), memo);
		hrLeaveHandoverService.revokeApply(id);

		return super.ok();
	}
}
