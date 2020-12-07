package com.pms.app.attendance;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pms.activiti.config.ActBusinessTypeEnum;
import com.pms.activiti.config.ActBusinessVariablesEnum;
import com.pms.activiti.config.ActProRoleEnum;
import com.pms.activiti.config.ActRoleEnum;
import com.pms.activiti.service.impl.ActBusinessServiceImpl;
import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.enums.McSysCodeEnum;
import com.pms.common.service.SysPushService;
import com.pms.common.utils.CalendarHelper;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.activiti.service.IActivitiQueryService;
import com.pms.core.attendance.domain.*;
import com.pms.core.attendance.service.*;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrMemberJoinService;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.project.domain.ProBaseEntity;
import com.pms.core.project.service.impl.ProBaseService;
import com.pms.core.project.service.impl.ProTeamService;
import com.pms.core.system.domain.SysDept;
import com.pms.core.system.service.ISysDeptService;
import com.pms.core.system.service.ISysRoleService;
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
 * 出差
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/attend/travel")
public class AttendTravelController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IAttendTravelService attendTravelService;
	@Autowired
	private IAttendTravelDetailsService attendTravelDetailsService;

	@Autowired
	private IAttendEgressDetailsService attendEgressDetailsService;
	@Autowired
	private IAttendLeaveDetailsService attendLeaveDetailsService;

	@Autowired
	private ProBaseService proBaseService;
	@Autowired
	private ProTeamService proTeamService;

	@Autowired
	private IStaffInfoService staffInfoService;
	@Autowired
	private ISysDeptService sysDeptService;
	@Autowired
	private ISysRoleService sysRoleService;

	@Autowired
	private IHrMemberJoinService hrMemberJoinService;

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
					myJoinProList.add(map);
				}
			}
		}

		AttendTravel entity = new AttendTravel();
		if(myJoinProList!=null && myJoinProList.size()>0){
			Map<String, String> map = myJoinProList.get(0);
			entity.setProId(map.get("proId"));
			entity.setProName(map.get("proName"));
		} else {
			entity.setProId("");
			entity.setProName("");
		}
		entity.setStartDay(new Date());
		entity.setEndDay(new Date());
		entity.setOrigin("");
		entity.setDestination("");
		entity.setDuration(0);
		entity.setType("0");
		entity.setReason("");
		entity.setState(0);
		entity.setWorkflowId("");
		//entity.setCreateUserId(userId);
		//entity.setCreateTime(new Date());
		//entity.setLastUpdateUserId(userId);
		//entity.setLastUpdateTime(new Date());

		return super.ok().put("entity", entity).put("myJoinProList", myJoinProList);
	}

	/**
	 * 我的出差列表
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

		String orderBy = "start_day desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);


		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<AttendTravel> list = attendTravelService.selectList(params);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());

		return super.ok().put("gridDatas", list).put("webPage", page);
	}

	/**
	 * 提交出差记录
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

		String proId = Tool.convertObject(parameterMap.get("proId"));
		String startDay = Tool.convertObject(parameterMap.get("startDay"));
		String endDay = Tool.convertObject(parameterMap.get("endDay"));
		String origin = Tool.convertObject(parameterMap.get("origin"));
		String destination = Tool.convertObject(parameterMap.get("destination"));
		String type = Tool.convertObject(parameterMap.get("type"));
		String reason = Tool.convertObject(parameterMap.get("reason"));

		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

		if(StringUtils.isEmpty(proId)){
			return super.error("项目不能为空");
		}
		if(StringUtils.isEmpty(startDay)){
			return super.error("出差开始日期不能为空");
		}
		if(StringUtils.isEmpty(endDay)){
			return super.error("出差结束日期不能为空");
		}
		if(StringUtils.isEmpty(origin)){
			return super.error("出发地不能为空");
		}
		if(StringUtils.isEmpty(destination)){
			return super.error("目的地不能为空");
		}
		if(StringUtils.isEmpty(type)){
			return super.error("请选择出差类型");
		}
		if(StringUtils.isEmpty(reason)){
			return super.error("请描述出差原因或内容");
		}

		Date startDate = DateUtils.conversion(startDay);
		Date endDate = DateUtils.conversion(endDay);
		if(DateUtils.dateSubtraction(startDate, endDate)<0){
			return super.error("出差结束日期不能晚于出差开始日期");
		}

		int cnt = attendTravelDetailsService.countAttendTravelDetails(userId, startDay, endDay);
		if(cnt>0){
			return super.error("该日期段内您有正在申请或已经审批通过的出差信息,不能重复申请.");
		}
		/*int leaveCnt = attendLeaveDetailsService.countAttendLeaveDetails(userId, startDay, endDay);
		if(leaveCnt>0){
			return super.error("您申请出差的时间段有请假申请,不能再申请出差.");
		}*/
		int egressCnt = attendEgressDetailsService.countAttendEgressDetails(userId, startDay, endDay);
		if(egressCnt>0){
			return super.error("您申请出差的时间段有外出申请,不能再申请出差.");
		}

		/**
		 * 时间与参与项目的周期进行对比
		 */
		Map<String, Object> myProMap = proTeamService.queryProTeamsByProIdAndUserId(proId, userId);
		if(myProMap==null || myProMap.isEmpty()){
			return super.error("未能查询到你在该项目的参与周期，请与项目经理确认你是否参与了该项目以及参与周期。");
		}
		Date joinProDate = DateUtils.conversion(Tool.convertObject(myProMap.get("joinDate")));
		Date departureProDate = DateUtils.conversion(Tool.convertObject(myProMap.get("departureDate")));
		if(startDate.getTime()<joinProDate.getTime()){
			return super.error("出差开始日期早于参与项目的日期");
		}
		if(startDate.getTime()>departureProDate.getTime()){
			return super.error("出差开始日期晚于离开项目的日期");
		}
		if(endDate.getTime()<joinProDate.getTime()){
			return super.error("出差结束日期早于参与项目的日期");
		}
		if(endDate.getTime()>departureProDate.getTime()){
			return super.error("出差结束日期晚于离开项目的日期");
		}

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
		 * 所属系统销售角色
		 * 后续会根据销售角色做抄送的区别
		 * 出差：
		 * 普通销售（角色）： 销售总监审核（PM）    CC：姚先友，李岩
		 * 销售总监（角色）： 部门经理审核（BM）    CC：花建和，黄龙，李岩
		 */
		String saleRoleType = "";
		List<String> roleKeys = sysRoleService.selectRoleKeysByUserId(userId);
		if(roleKeys!=null && roleKeys.size()>0){
			if(roleKeys.contains(ActRoleEnum.sale_vp.getCode())){
				saleRoleType = ActRoleEnum.sale_vp.getCode();
			} else if(roleKeys.contains(ActRoleEnum.sale_chief.getCode())){
				saleRoleType = ActRoleEnum.sale_chief.getCode();
			} else if(roleKeys.contains(ActRoleEnum.sale_staff.getCode())){
				saleRoleType = ActRoleEnum.sale_staff.getCode();
			}
		}

		/**
		 * ID
		 */
		String travelId = SnowflakeIdUtil.getId();

		/**
		 * 根据出差开始日期和结束日期自动拆解成出差明细
		 */
		List<AttendTravelDetails> details = new ArrayList<>();

		int duration = 0;

		/**
		 * 获取两个日期之间连续的天数
		 * 注意这里不包括endDate
		 * 所以需要补一天
		 */
		List<String> detailList = CalendarHelper.calculationTravelDays(startDay, endDay);
		if(detailList!=null && detailList.size()>0){
			duration = detailList.size();

			for(String day : detailList){
				AttendTravelDetails bean = new AttendTravelDetails();
				bean.setId(SnowflakeIdUtil.getId());
				bean.setUserId(userId);
				bean.setTravelId(travelId);
				bean.setTravelDay(DateUtils.conversion(day));
				details.add(bean);
			}
		} else {
			return super.error("通过出差开始日期和结束日期未能确定出差的日程");
		}

		AttendTravel entity = new AttendTravel();
		entity.setId(travelId);
		entity.setProId(proId);
		entity.setStartDay(startDate);
		entity.setEndDay(endDate);
		entity.setOrigin(origin);
		entity.setDestination(destination);
		entity.setDuration(duration);
		entity.setType(type);
		entity.setReason(reason);
		entity.setState(0);
		entity.setWorkflowId("");
		entity.setCreateUserId(userId);
		entity.setCreateTime(new Date());
		entity.setLastUpdateUserId(userId);
		entity.setLastUpdateTime(new Date());
		attendTravelService.insertEntity(entity);

		attendTravelDetailsService.batchInsertEntity(details);


		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<WorkflowOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				WorkflowOss wo = new WorkflowOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setBusinessKey(ActBusinessTypeEnum.attendance_travel.getCode());
				wo.setBusinessId(travelId);
				wo.setOssId(ossId);
				ossDatas.add(wo);
			}
			if(ossDatas!=null && ossDatas.size()>0){
				workflowOssService.batchInsertEntity(ossDatas);
			}
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
		otherParam.put(ActBusinessVariablesEnum.saleRoleType.getCode(),  saleRoleType);

		String businessMemo = staff.getName()+"申请出差,地点:从"+origin+"到"+destination+",日期:从"+startDay+"至"+endDay+",共"+duration+"天";
		ProcessInstance pi = actBusinessServiceImpl.startProcess(staff.getAccount(), staff.getName(), ActBusinessTypeEnum.attendance_travel, travelId, businessMemo, otherParam);

		/**
		 * 关联表单和工作流
		 */
		attendTravelService.updateWorkflowId(pi.getId(), travelId);

		/**
		 * 保存抄送信息
		 */
		if(StringUtils.isNotEmpty(ccAccounts)){
			List<WorkflowCc> ccDatas = new ArrayList<>();
			String[] ccArray = ccAccounts.split(",");
			for(String ccAccount : ccArray){
				WorkflowCc cc = new WorkflowCc();
				cc.setId(SnowflakeIdUtil.getId());
				cc.setBusinessKey(ActBusinessTypeEnum.attendance_travel.getCode());
				cc.setBusinessId(travelId);
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
	 * 查看出差明细
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
		String travelId = Tool.convertObject(parameterMap.get("travelId"));

		if(StringUtils.isEmpty(userId)){
			return super.error("userId参数不能为空");
		}
		if(StringUtils.isEmpty(travelId)){
			return super.error("ID参数不能为空");
		}

		AttendTravel entity = attendTravelService.selectEntityById(travelId);
		if(entity==null){
			return super.error("未查询到申请单");
		}
		StaffInfo staffInfo = staffInfoService.selectEntityById(entity.getCreateUserId());

		List<AttendTravelDetails> details = attendTravelDetailsService.selectRecordsByTravelId(travelId);
		List<Map<String, String>> actHis = activitiQueryService.selectHistoryFlowList(entity.getWorkflowId());
		List<WorkflowOss> ossList = workflowOssService.selectListByBusiness(ActBusinessTypeEnum.attendance_travel.getCode(), travelId);

		/**
		 * 当前审批候选人
		 */
		List<Map<String, Object>> candidateList = new ArrayList<>();
		if(entity.getState().toString().equals("0")){
			candidateList = activitiQueryService.selectCandidateListByProcInsId(entity.getWorkflowId());
		}

		/**
		 * 查询抄送人
		 */
		List<StaffInfo> ccStaffList = droolsBusinessServiceImpl.selectWorkFlowCcStaffList(staffInfo.getAccount(), ActBusinessTypeEnum.attendance_travel.getCode(), travelId, entity.getWorkflowId(), entity.getState()+"");

		/**
		 * 撤销标志:0 不可撤销，1 可撤销
		 */
		String revokeTag = "0";
		if(entity!=null && entity.getCreateUserId().equals(userId) && entity.getState()==0){
			revokeTag = "1";
		}

		return super.ok().put("entity", entity).put("details", details).put("actHis", actHis).put("ossList", ossList).put("candidateList", candidateList).put("revokeTag", revokeTag).put("ccStaffList", ccStaffList);
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

		AttendTravel entity = attendTravelService.selectEntityById(id);

		actBusinessServiceImpl.deleteProcessInstance(entity.getWorkflowId(), memo);
		attendTravelService.revokeApply(id);

		return super.ok();
	}
}
