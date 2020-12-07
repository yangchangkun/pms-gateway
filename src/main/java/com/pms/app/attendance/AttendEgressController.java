package com.pms.app.attendance;

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
import com.pms.common.utils.CalendarHelper;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.activiti.service.IActivitiQueryService;
import com.pms.core.attendance.domain.AttendEgress;
import com.pms.core.attendance.domain.AttendEgressDetails;
import com.pms.core.attendance.domain.AttendLeaveDetails;
import com.pms.core.attendance.service.IAttendEgressDetailsService;
import com.pms.core.attendance.service.IAttendEgressService;
import com.pms.core.attendance.service.IAttendLeaveDetailsService;
import com.pms.core.attendance.service.IAttendTravelDetailsService;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrMemberJoinService;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.prefer.service.IPreferCalendarService;
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
 * 外出
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/attend/egress")
public class AttendEgressController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IAttendEgressService attendEgressService;
	@Autowired
	private IAttendEgressDetailsService attendEgressDetailsService;

	@Autowired
	private IAttendTravelDetailsService attendTravelDetailsService;
	@Autowired
	private IAttendLeaveDetailsService attendLeaveDetailsService;

	@Autowired
	private ProBaseService proBaseService;
	@Autowired
	private ProTeamService proTeamService;

	@Autowired
	private IWorkflowOssService workflowOssService;
	@Autowired
	private IWorkflowCcService workflowCcService;

	@Autowired
	private IStaffInfoService staffInfoService;
	@Autowired
	private ISysDeptService sysDeptService;
	@Autowired
	private IHrMemberJoinService hrMemberJoinService;

	@Autowired
	private IPreferCalendarService preferCalendarService;

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

		String today = DateUtils.getCurrentDay();

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

		AttendEgress entity = new AttendEgress();
		if(myJoinProList!=null && myJoinProList.size()>0){
			Map<String, String> map = myJoinProList.get(0);
			entity.setProId(map.get("proId"));
			entity.setProName(map.get("proName"));
		} else {
			entity.setProId("");
			entity.setProName("");
		}
		entity.setStartTime(DateUtils.conversion(today+" 09:00", DateUtils.YYYY_MM_DD_HH_MM));
		entity.setEndTime(DateUtils.conversion(today+" 18:00", DateUtils.YYYY_MM_DD_HH_MM));
		entity.setDuration(0.0);
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
	 * 我的外出列表
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

		String orderBy = "start_time desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);


		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<AttendEgress> list = attendEgressService.selectList(params);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());

		return super.ok().put("gridDatas", list).put("webPage", page);
	}

	/**
	 * 提交外出记录
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
		String startTime = Tool.convertObject(parameterMap.get("startTime"));
		String endTime = Tool.convertObject(parameterMap.get("endTime"));
		String reason = Tool.convertObject(parameterMap.get("reason"));

		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

		if(StringUtils.isEmpty(proId)){
			return super.error("项目不能为空");
		}
		if(StringUtils.isEmpty(startTime)){
			return super.error("外出开始时间不能为空");
		}
		if(StringUtils.isEmpty(endTime)){
			return super.error("外出结束时间不能为空");
		}
		if(StringUtils.isEmpty(reason)){
			return super.error("请描述外出原因或内容");
		}

		Date startDate = DateUtils.conversion(startTime+":00", DateUtils.YYYY_MM_DD_HH_MM_SS);
		Date endDate = DateUtils.conversion(endTime+":00", DateUtils.YYYY_MM_DD_HH_MM_SS);
		if(DateUtils.dateSubtraction(startDate, endDate)<0){
			return super.error("外出结束时间不能晚于外出开始时间");
		}

		String startDay = DateUtils.format(startDate, DateUtils.YYYY_MM_DD);
		String endDay = DateUtils.format(endDate, DateUtils.YYYY_MM_DD);

		/*int cnt = attendEgressDetailsService.countAttendEgressDetails(userId, startDay, endDay);
		if(cnt>0){
			return super.error("该时间段内您有正在申请或已经审批通过的外出信息,不能重复申请.");
		}
		int leaveCnt = attendLeaveDetailsService.countAttendLeaveDetails(userId, startDay, endDay);
		if(leaveCnt>0){
			return super.error("您申请外出的时间段有请假申请,不能再申请外出.");
		}*/
		int travelCnt = attendTravelDetailsService.countAttendTravelDetails(userId, startDay, endDay);
		if(travelCnt>0){
			return super.error("您申请外出的时间段有出差申请,不能再申请外出.");
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
			return super.error("外出开始时间早于参与项目的日期");
		}
		if(startDate.getTime()>departureProDate.getTime()){
			return super.error("外出开始时间晚于离开项目的日期");
		}
		if(endDate.getTime()<joinProDate.getTime()){
			return super.error("外出结束时间早于参与项目的日期");
		}
		if(endDate.getTime()>departureProDate.getTime()){
			return super.error("外出结束时间晚于离开项目的日期");
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
		 * ID
		 */
		String egressId = SnowflakeIdUtil.getId();


		/**
		 * 根据外出开始时间和结束时间自动拆解成外出明细
		 */
		List<AttendEgressDetails> details = new ArrayList<>();

		double duration = 0;

		List<Map<String, String>> detailList = CalendarHelper.calculationEgressDuty(startTime, endTime);
		if(detailList!=null && detailList.size()>0){
			for(Map<String, String> map : detailList){
				String overtimeDay = map.get("day");
				String overtimeStartTime = map.get("startTime");
				String overtimeEndTime = map.get("endTime");
				double overtimeDuration = Tool.convertStringToDouble(map.get("duration"));
				duration += overtimeDuration;

				AttendEgressDetails bean = new AttendEgressDetails();
				bean.setId(SnowflakeIdUtil.getId());
				bean.setUserId(userId);
				bean.setEgressId(egressId);
				bean.setEgressDay(DateUtils.conversion(overtimeDay));
				bean.setEgressStartTime(DateUtils.conversion(overtimeStartTime, DateUtils.YYYY_MM_DD_HH_MM_SS));
				bean.setEgressEndTime(DateUtils.conversion(overtimeEndTime, DateUtils.YYYY_MM_DD_HH_MM_SS));
				bean.setEgressDuration(overtimeDuration);
				details.add(bean);
			}
		} else {
			return super.error("通过外出开始时间和结束时间未能确定外出核算考勤明细.");
		}

		/**
		 * 查询日期段内的外出数据
		 * 并分析和要请假的时段是否有时间上的重叠
		 */
		List<AttendEgressDetails> historyEgressDetails = attendEgressDetailsService.selectAttendEgressDetailsBetweenDay(userId, startDay, endDay);
		boolean overlapEgressTag = false;
		if(historyEgressDetails!=null && historyEgressDetails.size()>0){
			for(AttendEgressDetails egressDetails : historyEgressDetails){
				Date startDate1 = egressDetails.getEgressStartTime();
				Date endDate1 = egressDetails.getEgressEndTime();

				for(AttendEgressDetails currDetails : details){
					Date startDate2 = currDetails.getEgressStartTime();
					Date endDate2 = currDetails.getEgressEndTime();

					if(startDate1.getTime()<endDate2.getTime() && endDate1.getTime()>startDate2.getTime()){
						overlapEgressTag = true;
						break;
					}
				}
			}
		}
		if(overlapEgressTag){
			return super.error("您申请外出的时间段有外出申请,不能再申请同时段的外出.");
		}

		/**
		 * 查询日期段内的请假数据
		 * 并分析和要请假的时段是否有时间上的重叠
		 */
		List<AttendLeaveDetails> historyLeaveDetails = attendLeaveDetailsService.selectAttendLeaveDetailsBetweenDay(userId, startDay, endDay);
		boolean overlapLeaveTag = false;
		if(historyLeaveDetails!=null && historyLeaveDetails.size()>0){
			for(AttendLeaveDetails historyDetails : historyLeaveDetails){
				Date startDate1 = historyDetails.getLeaveStartTime();
				Date endDate1 = historyDetails.getLeaveEndTime();

				for(AttendEgressDetails currDetails : details){
					Date startDate2 = currDetails.getEgressStartTime();
					Date endDate2 = currDetails.getEgressEndTime();

					if(startDate1.getTime()<endDate2.getTime() && endDate1.getTime()>startDate2.getTime()){
						overlapLeaveTag = true;
						break;
					}
					/*if( (startDate2.getTime()+1>=startDate1.getTime() && startDate2.getTime()<endDate1.getTime())
							||
							(endDate2.getTime()+1>=startDate1.getTime() && endDate2.getTime()<endDate1.getTime())
							){

						overlapLeaveTag = true;
						break;
					}*/
				}
			}
		}
		if(overlapLeaveTag){
			return super.error("该时间段内您有正在申请或已经审批通过的请假信息,不能重复申请.");
		}



		AttendEgress entity = new AttendEgress();
		entity.setId(egressId);
		entity.setProId(proId);
		entity.setStartTime(startDate);
		entity.setEndTime(endDate);
		entity.setDuration(duration);
		entity.setReason(reason);
		entity.setState(0);
		entity.setWorkflowId("");
		entity.setCreateUserId(userId);
		entity.setCreateTime(new Date());
		entity.setLastUpdateUserId(userId);
		entity.setLastUpdateTime(new Date());
		attendEgressService.insertEntity(entity);

		attendEgressDetailsService.batchInsertEntity(details);

		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<WorkflowOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				WorkflowOss wo = new WorkflowOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setBusinessKey(ActBusinessTypeEnum.attendance_egress.getCode());
				wo.setBusinessId(egressId);
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

		String businessMemo = staff.getName()+"申请外出,时间:从"+startTime+"至"+endTime+",共"+duration+"小时";
		ProcessInstance pi = actBusinessServiceImpl.startProcess(staff.getAccount(), staff.getName(), ActBusinessTypeEnum.attendance_egress, egressId, businessMemo, otherParam);

		/**
		 * 关联表单和工作流
		 */
		attendEgressService.updateWorkflowId(pi.getId(), egressId);

		/**
		 * 保存抄送信息
		 */
		if(StringUtils.isNotEmpty(ccAccounts)){
			List<WorkflowCc> ccDatas = new ArrayList<>();
			String[] ccArray = ccAccounts.split(",");
			for(String ccAccount : ccArray){
				WorkflowCc cc = new WorkflowCc();
				cc.setId(SnowflakeIdUtil.getId());
				cc.setBusinessKey(ActBusinessTypeEnum.attendance_egress.getCode());
				cc.setBusinessId(egressId);
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
	 * 查看外出明细
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
		String egressId = Tool.convertObject(parameterMap.get("egressId"));

		if(StringUtils.isEmpty(userId)){
			return super.error("userId参数不能为空");
		}
		if(StringUtils.isEmpty(egressId)){
			return super.error("ID参数不能为空");
		}

		AttendEgress entity = attendEgressService.selectEntityById(egressId);
		if(entity==null){
			return super.error("未查询到申请单");
		}
		StaffInfo staffInfo = staffInfoService.selectEntityById(entity.getCreateUserId());

		List<AttendEgressDetails> details = attendEgressDetailsService.selectRecordsByEgressId(egressId);
		List<Map<String, String>> actHis = activitiQueryService.selectHistoryFlowList(entity.getWorkflowId());
		List<WorkflowOss> ossList = workflowOssService.selectListByBusiness(ActBusinessTypeEnum.attendance_egress.getCode(), egressId);

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
		List<StaffInfo> ccStaffList = droolsBusinessServiceImpl.selectWorkFlowCcStaffList(staffInfo.getAccount(), ActBusinessTypeEnum.attendance_egress.getCode(), egressId, entity.getWorkflowId(), entity.getState()+"");

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

		AttendEgress entity = attendEgressService.selectEntityById(id);

		actBusinessServiceImpl.deleteProcessInstance(entity.getWorkflowId(), memo);
		attendEgressService.revokeApply(id);

		return super.ok();
	}
}
