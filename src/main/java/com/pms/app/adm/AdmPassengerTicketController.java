package com.pms.app.adm;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pms.activiti.config.ActBusinessTypeEnum;
import com.pms.activiti.config.ActBusinessVariablesEnum;
import com.pms.activiti.config.ActProRoleEnum;
import com.pms.activiti.service.impl.ActBusinessServiceImpl;
import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.enums.McSysCodeEnum;
import com.pms.common.service.SysPushService;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.activiti.service.IActivitiQueryService;
import com.pms.core.adm.domain.AdmPassengerTicket;
import com.pms.core.adm.service.IAdmPassengerTicketService;
import com.pms.core.hr.domain.HrLeaveApply;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrLeaveApplyService;
import com.pms.core.hr.service.IHrMemberJoinService;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.project.domain.ProBaseEntity;
import com.pms.core.project.service.impl.ProBaseService;
import com.pms.core.project.service.impl.ProTeamService;
import com.pms.core.system.domain.SysDept;
import com.pms.core.system.service.ISysDeptService;
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
 * 机票申请
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/adm/passengerTicket")
public class AdmPassengerTicketController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IAdmPassengerTicketService admPassengerTicketService;

	@Autowired
	private IHrLeaveApplyService hrLeaveApplyService;

	@Autowired
	private IWorkflowOssService workflowOssService;
	@Autowired
	private IWorkflowCcService workflowCcService;

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
	private ActBusinessServiceImpl actBusinessServiceImpl;
	@Autowired
	private IActivitiQueryService activitiQueryService;
	@Autowired
	private DroolsBusinessServiceImpl droolsBusinessServiceImpl;

	@Autowired
	private SysPushService sysPushService;


	/**
	 * 初始化
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

		List<Map<String, String>> myJoinProList = new ArrayList<>();
		List<ProBaseEntity> list = proBaseService.queryMyJoiningProList(userId);
		if(list!=null && list.size()>0){
			Date currDayDate = DateUtils.conversion(DateUtils.getCurrentDay());
			for(ProBaseEntity pro : list){
				Date proCreateDay = pro.getProCreateDay();
				Date proEndDay = pro.getProEndDay();

				/**
				 * 查询我在当前这个项目的具体情况
				 */
				Map<String, Object> myProMap = proTeamService.queryProTeamsByProIdAndUserId(pro.getId(), userId);
				Date joinDate = DateUtils.conversion(Tool.convertObject(myProMap.get("joinDate")));
				Date departureDate = DateUtils.conversion(Tool.convertObject(myProMap.get("departureDate")));

				if(currDayDate.getTime()<proCreateDay.getTime() || currDayDate.getTime()>proEndDay.getTime() ){
					//项目在当前日期（day）未开始或已结束

				} else if(currDayDate.getTime()<joinDate.getTime() || currDayDate.getTime()>departureDate.getTime()){
					//当前日期（day）未加入项目组

				} else {
					Map<String, String> map = new HashMap<>();
					map.put("proId", pro.getId());
					map.put("proName", pro.getProName());
					map.put("proSerial", pro.getProSerial());
					myJoinProList.add(map);
				}
			}
		}

		AdmPassengerTicket entity = new AdmPassengerTicket();
		entity.setId("");
		entity.setApplyUserId(staff.getUserId());
		entity.setApplyUserName(staff.getName());
		entity.setApplyDate(DateUtils.conversion(DateUtils.getCurrentDay()));
		entity.setApplyPhone(staff.getPhonenumber());
		entity.setApplyIc(staff.getIcNum());

		if(myJoinProList!=null && myJoinProList.size()>0){
			Map<String, String> map = myJoinProList.get(0);
			entity.setProId(map.get("proId"));
			entity.setProName(map.get("proName"));
			entity.setProSerial(map.get("proSerial"));
		} else {
			entity.setProId("");
			entity.setProName("");
			entity.setProSerial("");
		}
		entity.setTripType("0");
		entity.setOrigin("");
		entity.setDestination("");
		entity.setDepartDay(DateUtils.conversion(DateUtils.getCurrentDay()));
		entity.setRevertDay(null);

		entity.setMemo("");
		entity.setState("0");
		entity.setWorkflowId("");

		return super.ok().put("entity", entity).put("staff", staff).put("myJoinProList", myJoinProList);
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

		String orderBy = "apt.create_time desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("applyUserId", userId);

		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<AdmPassengerTicket> list = admPassengerTicketService.selectList(params);
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

		String applyPhone = Tool.convertObject(parameterMap.get("applyPhone"));
		String applyIc = Tool.convertObject(parameterMap.get("applyIc"));
		String applyDate = Tool.convertObject(parameterMap.get("applyDate"));
		if(StringUtils.isEmpty(applyDate)){
			applyDate = DateUtils.getCurrentDay();
		}
		String proId = Tool.convertObject(parameterMap.get("proId"));
		String tripType = Tool.convertObject(parameterMap.get("tripType"));
		String origin = Tool.convertObject(parameterMap.get("origin"));
		String destination = Tool.convertObject(parameterMap.get("destination"));
		String departDay = Tool.convertObject(parameterMap.get("departDay"));
		String revertDay = Tool.convertObject(parameterMap.get("revertDay"));
		String memo = Tool.convertObject(parameterMap.get("memo"));

		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

		if(StringUtils.isEmpty(applyPhone)){
			return super.error("请填写机票申请人联系电话");
		}
		if(StringUtils.isEmpty(applyIc)){
			return super.error("请填写机票申请人身份证号");
		}
		if(StringUtils.isEmpty(applyDate)){
			return super.error("请选择申请日期");
		}
		if(StringUtils.isEmpty(proId)){
			return super.error("请选择项目");
		}
		if(StringUtils.isEmpty(tripType)){
			return super.error("请选择行程类型");
		}
		if(StringUtils.isEmpty(origin)){
			return super.error("请填写出发地");
		}
		if(StringUtils.isEmpty(destination)){
			return super.error("请填写目的地");
		}
		if(origin.equals(destination)){
			return super.error("出发地和目的地不能为同一个地址");
		}
		if(StringUtils.isEmpty(departDay)){
			return super.error("请选择出发日期");
		}
		if(tripType.equals("1") && StringUtils.isEmpty(revertDay)){
			return super.error("请选择返回日期");
		}


		List<HrLeaveApply> leaveApplyList = hrLeaveApplyService.selectApplyingByUserId(userId);
		if(leaveApplyList!=null && leaveApplyList.size()>0){
			return super.error("您有正在处理中的离职申请信息,不能申请出差");
		}

		Date applyDay = DateUtils.conversion(applyDate);
		Date departDate = DateUtils.conversion(departDay);
		Date revertDate = null;
		if(tripType.equals("1") && StringUtils.isNotEmpty(revertDay)){
			revertDate = DateUtils.conversion(revertDay);
		}

		/*if(DateUtils.dateSubtraction(applyDay, departDate)<0){
			return super.error("出发日期不能晚于申请日期");
		}*/
		if(tripType.equals("1")){
			if(DateUtils.dateSubtraction(departDate, revertDate)<0){
				return super.error("返回日期不能晚于出发日期");
			}
		}

		/**
		 * 时间与参与项目的周期进行对比
		 */
		/*Map<String, Object> myProMap = proTeamService.queryProTeamsByProIdAndUserId(proId, userId);
		if(myProMap==null || myProMap.isEmpty()){
			return super.error("未能查询到你在该项目的参与周期，请与项目经理确认你是否参与了该项目以及参与周期。");
		}
		Date joinProDate = DateUtils.conversion(Tool.convertObject(myProMap.get("joinDate")));
		Date departureProDate = DateUtils.conversion(Tool.convertObject(myProMap.get("departureDate")));
		if(departDate.getTime()<joinProDate.getTime()){
			return super.error("出发日期早于参与项目的日期");
		}
		if(departDate.getTime()>departureProDate.getTime()){
			return super.error("出发日期晚于离开项目的日期");
		}
		if(revertDate!=null){
			if(revertDate.getTime()<joinProDate.getTime()){
				return super.error("返回日期早于参与项目的日期");
			}
			if(revertDate.getTime()>departureProDate.getTime()){
				return super.error("返回日期晚于离开项目的日期");
			}
		}*/

		StaffInfo staff = staffInfoService.selectEntityById(userId);

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
		 * ID
		 */
		String id = SnowflakeIdUtil.getId();

		AdmPassengerTicket entity = new AdmPassengerTicket();
		entity.setId(id);
		entity.setApplyUserId(userId);
		entity.setApplyDate(applyDay);
		entity.setApplyPhone(applyPhone);
		entity.setApplyIc(applyIc);

		entity.setProId(proId);
		entity.setTripType(tripType);
		entity.setOrigin(origin);
		entity.setDestination(destination);
		entity.setDepartDay(departDate);
		entity.setRevertDay(revertDate);
		entity.setMemo(memo);

		entity.setState("0");
		entity.setWorkflowId("");

		entity.setCreateBy(userId);
		entity.setCreateTime(new Date());
		//entity.setUpdateBy(userId);
		//entity.setUpdateTime(new Date());
		admPassengerTicketService.insertEntity(entity);

		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<WorkflowOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				WorkflowOss wo = new WorkflowOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setBusinessKey(ActBusinessTypeEnum.adm_passenger_ticket.getCode());
				wo.setBusinessId(id);
				wo.setOssId(ossId);
				ossDatas.add(wo);
			}
			if(ossDatas!=null && ossDatas.size()>0){
				workflowOssService.batchInsertEntity(ossDatas);
			}
		}

		String tripTypeLabel = "单程";
		if(tripType.equals("1")){
			tripTypeLabel = "往返";
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

		String businessMemo = staff.getName()+"申请"+tripTypeLabel+"机票,地点:从"+origin+"到"+destination+",时间:"+departDay+"~"+revertDay;
		ProcessInstance pi = actBusinessServiceImpl.startProcess(staff.getAccount(), staff.getName(), ActBusinessTypeEnum.adm_passenger_ticket, id, businessMemo, otherParam);

		/**
		 * 关联表单和工作流
		 */
		admPassengerTicketService.updateWorkflowId(pi.getId(), id);

		/**
		 * 保存抄送信息
		 */
		if(StringUtils.isNotEmpty(ccAccounts)){
			List<WorkflowCc> ccDatas = new ArrayList<>();
			String[] ccArray = ccAccounts.split(",");
			for(String ccAccount : ccArray){
				WorkflowCc cc = new WorkflowCc();
				cc.setId(SnowflakeIdUtil.getId());
				cc.setBusinessKey(ActBusinessTypeEnum.adm_passenger_ticket.getCode());
				cc.setBusinessId(id);
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
	 * 查看机票明细
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

		AdmPassengerTicket entity = admPassengerTicketService.selectEntityById(id);
		if(entity==null){
			return super.error("未查询到申请单");
		}
		StaffInfo staffInfo = staffInfoService.selectEntityById(entity.getCreateBy());

		List<Map<String, String>> actHis = activitiQueryService.selectHistoryFlowList(entity.getWorkflowId());
		List<WorkflowOss> ossList = workflowOssService.selectListByBusiness(ActBusinessTypeEnum.adm_passenger_ticket.getCode(), id);

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
		List<StaffInfo> ccStaffList = droolsBusinessServiceImpl.selectWorkFlowCcStaffList(staffInfo.getAccount(), ActBusinessTypeEnum.adm_passenger_ticket.getCode(), id, entity.getWorkflowId(), entity.getState());

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

		AdmPassengerTicket entity = admPassengerTicketService.selectEntityById(id);

		actBusinessServiceImpl.deleteProcessInstance(entity.getWorkflowId(), memo);
		admPassengerTicketService.revokeApply(id);

		return super.ok();
	}
}
