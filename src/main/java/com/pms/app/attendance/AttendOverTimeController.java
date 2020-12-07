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
import com.pms.core.attendance.domain.*;
import com.pms.core.attendance.service.*;
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
 * 加班
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/attend/overtime")
public class AttendOverTimeController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IAttendOvertimeService attendOvertimeService;
	@Autowired
	private IAttendOvertimeDetailsService attendOvertimeDetailsService;

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

		String currDt = DateUtils.format(new Date(), DateUtils.YYYY_MM_DD_HH_MM_SS);

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

		AttendOvertime entity = new AttendOvertime();
		if(myJoinProList!=null && myJoinProList.size()>0){
			Map<String, String> map = myJoinProList.get(0);
			entity.setProId(map.get("proId"));
			entity.setProName(map.get("proName"));
		} else {
			entity.setProId("");
			entity.setProName("");
		}
		entity.setStartTime(DateUtils.conversion(currDt, DateUtils.YYYY_MM_DD_HH));
		entity.setEndTime(DateUtils.conversion(currDt, DateUtils.YYYY_MM_DD_HH));
		entity.setDuration(0.0);
		entity.setType("0");
		entity.setReason("");
		entity.setState("0");
		entity.setWorkflowId("");
		//entity.setCreateUserId(userId);
		//entity.setCreateTime(new Date());
		//entity.setLastUpdateUserId(userId);
		//entity.setLastUpdateTime(new Date());

		return super.ok().put("entity", entity).put("myJoinProList", myJoinProList);
	}

	/**
	 * 我的加班列表
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

		String orderBy = " ot.start_time desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);


		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<AttendOvertime> list = attendOvertimeService.selectList(params);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());

		return super.ok().put("gridDatas", list).put("webPage", page);
	}

	/**
	 * 提交加班记录
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
		String type = Tool.convertObject(parameterMap.get("type"));
		String reason = Tool.convertObject(parameterMap.get("reason"));

		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

		if(StringUtils.isEmpty(proId)){
			return super.error("项目不能为空");
		}
		if(StringUtils.isEmpty(startTime)){
			return super.error("加班开始时间不能为空");
		}
		if(StringUtils.isEmpty(endTime)){
			return super.error("加班结束时间不能为空");
		}
		if(StringUtils.isEmpty(type)){
			return super.error("请选择加班类型");
		}
		if(StringUtils.isEmpty(reason)){
			return super.error("请描述加班原因或内容");
		}

		Date startDate = DateUtils.conversion(startTime, DateUtils.YYYY_MM_DD_HH);
		Date endDate = DateUtils.conversion(endTime, DateUtils.YYYY_MM_DD_HH);
		if(DateUtils.dateSubtraction(startDate, endDate)<0){
			return super.error("加班结束时间不能晚于加班开始时间");
		}

		String startYear = DateUtils.format(startDate, DateUtils.YYYY);
		String endYear = DateUtils.format(endDate, DateUtils.YYYY);
		if(!startYear.equals(endYear)){
			return super.error("不允许跨年申请加班.");
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
			return super.error("加班开始时间早于参与项目的日期");
		}
		if(startDate.getTime()>departureProDate.getTime()){
			return super.error("加班开始时间晚于离开项目的日期");
		}
		if(endDate.getTime()<joinProDate.getTime()){
			return super.error("结束加班时间早于参与项目的日期");
		}
		if(endDate.getTime()>departureProDate.getTime()){
			return super.error("结束加班时间晚于离开项目的日期");
		}

		String startDay = DateUtils.format(startDate, DateUtils.YYYY_MM_DD);
		String endDay = DateUtils.format(endDate, DateUtils.YYYY_MM_DD);

		/*int overTimeCnt = attendOvertimeDetailsService.countAttendOvertimeDetails(userId, startDay, endDay);
		if(overTimeCnt>0){
			return super.error("该时间段内您有正在申请或已经审批通过的加班信息,不能重复申请.");
		}*/

		/**
		 * 获取该年的节假日
		 */
		Map<String, String> yearHolidayMap = preferCalendarService.getHolidayList(Tool.convertStringToInt(startYear));

		/**
		 * ID
		 */
		String overTimeId = SnowflakeIdUtil.getId();


		/**
		 * 根据加班开始时间和结束时间自动拆解成加班明细
		 */
		List<AttendOvertimeDetails> details = new ArrayList<>();

		double duration = 0;

		List<Map<String, Object>> detailList = CalendarHelper.calculationOverTimeDuty(startTime, endTime, yearHolidayMap);
		if(detailList!=null && detailList.size()>0){
			for(Map<String, Object> map : detailList){
				String overtimeDay =  Tool.convertObject(map.get("day"));
				String overtimeStartTime = Tool.convertObject(map.get("startTime"));
				String overtimeEndTime = Tool.convertObject(map.get("endTime"));
				double overtimeDuration = Tool.convertStringToDouble(map.get("duration"));
				duration += overtimeDuration;

				AttendOvertimeDetails bean = new AttendOvertimeDetails();
				bean.setId(SnowflakeIdUtil.getId());
				bean.setUserId(userId);
				bean.setOvertimeId(overTimeId);
				bean.setOvertimeDay(DateUtils.conversion(overtimeDay));
				bean.setOvertimeStartTime(DateUtils.conversion(overtimeStartTime, DateUtils.YYYY_MM_DD_HH));
				bean.setOvertimeEndTime(DateUtils.conversion(overtimeEndTime, DateUtils.YYYY_MM_DD_HH));
				bean.setOvertimeDuration(overtimeDuration);
				details.add(bean);
			}
		} else {
			return super.error("通过加班开始时间和结束时间未能确定加班明细.请确定您选择的加班时段是否符合加班规则的要求。");
		}

		/**
		 * 查询日期段内的外出数据
		 * 并分析和要请假的时段是否有时间上的重叠
		 */
		List<AttendOvertimeDetails> historyOverTimeDetails = attendOvertimeDetailsService.selectAttendOvertimeDetailsBetweenDay(userId, startDay, endDay);
		boolean overlapOverTimeTag = false;
		if(historyOverTimeDetails!=null && historyOverTimeDetails.size()>0){
			for(AttendOvertimeDetails overDetails : historyOverTimeDetails){
				Date startDate1 = overDetails.getOvertimeStartTime();
				Date endDate1 = overDetails.getOvertimeEndTime();

				for(AttendOvertimeDetails currDetails : details){
					Date startDate2 = currDetails.getOvertimeStartTime();
					Date endDate2 = currDetails.getOvertimeEndTime();

					if(startDate1.getTime()<endDate2.getTime() && endDate1.getTime()>startDate2.getTime()){
						overlapOverTimeTag = true;
						break;
					}
				}
			}
		}
		if(overlapOverTimeTag){
			return super.error("您申请加班的时间段有加班申请,不能再申请同时段的加班.");
		}


		AttendOvertime overtime = new AttendOvertime();
		overtime.setId(overTimeId);
		overtime.setProId(proId);
		overtime.setStartTime(startDate);
		overtime.setEndTime(endDate);
		overtime.setDuration(duration);
		overtime.setType(type);
		overtime.setReason(reason);
		overtime.setState("0");
		overtime.setWorkflowId("");
		overtime.setCreateUserId(userId);
		overtime.setCreateTime(new Date());
		overtime.setUpdateUserId(userId);
		overtime.setUpdateTime(new Date());
		attendOvertimeService.insertEntity(overtime);

		attendOvertimeDetailsService.batchInsertEntity(details);


		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<WorkflowOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				WorkflowOss wo = new WorkflowOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setBusinessKey(ActBusinessTypeEnum.attendance_overtime.getCode());
				wo.setBusinessId(overTimeId);
				wo.setOssId(ossId);
				ossDatas.add(wo);
			}
			if(ossDatas!=null && ossDatas.size()>0){
				workflowOssService.batchInsertEntity(ossDatas);
			}


		}

		String typeLabel = "部门要求";
		if("1".equals(type)){
			typeLabel = "客户要求";
		} else if("2".equals(type)){
			typeLabel = "个人申请";
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
		 * 开始审批进程
		 */
		Map<String, Object> otherParam = new HashMap<>();
		otherParam.put(ActBusinessVariablesEnum.proId.getCode(), proId);
		otherParam.put(ActBusinessVariablesEnum.proRole.getCode(), proRole);
		otherParam.put(ActBusinessVariablesEnum.proManager.getCode(), Tool.convertObject(proBaseEntity.getProManager()));
		otherParam.put(ActBusinessVariablesEnum.proChief.getCode(),  Tool.convertObject(proBaseEntity.getProChief()));
		otherParam.put(ActBusinessVariablesEnum.deptManager.getCode(),  Tool.convertObject(proBaseEntity.getProLead()));

		String businessMemo = staff.getName()+"申请加班,加班类型:"+typeLabel+",加班时间:从"+startTime+"至"+endTime+",共"+duration+"小时";
		ProcessInstance pi = actBusinessServiceImpl.startProcess(staff.getAccount(), staff.getName(), ActBusinessTypeEnum.attendance_overtime, overTimeId, businessMemo, otherParam);

		/**
		 * 关联表单和工作流
		 */
		attendOvertimeService.updateWorkflowId(pi.getId(), overTimeId);

		/**
		 * 保存抄送信息
		 */
		if(StringUtils.isNotEmpty(ccAccounts)){
			List<WorkflowCc> ccDatas = new ArrayList<>();
			String[] ccArray = ccAccounts.split(",");
			for(String ccAccount : ccArray){
				WorkflowCc cc = new WorkflowCc();
				cc.setId(SnowflakeIdUtil.getId());
				cc.setBusinessKey(ActBusinessTypeEnum.attendance_overtime.getCode());
				cc.setBusinessId(overTimeId);
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
	 * 查看加班明细
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
		String overtimeId = Tool.convertObject(parameterMap.get("overtimeId"));

		if(StringUtils.isEmpty(userId)){
			return super.error("userId参数不能为空");
		}
		if(StringUtils.isEmpty(overtimeId)){
			return super.error("ID参数不能为空");
		}

		AttendOvertime entity = attendOvertimeService.selectEntityById(overtimeId);
		if(entity==null){
			return super.error("未查询到申请单");
		}
		StaffInfo staffInfo = staffInfoService.selectEntityById(entity.getCreateUserId());

		List<AttendOvertimeDetails> details = attendOvertimeDetailsService.selectRecordsByOvertimeId(overtimeId);
		List<Map<String, String>> actHis = activitiQueryService.selectHistoryFlowList(entity.getWorkflowId());
		List<WorkflowOss> ossList = workflowOssService.selectListByBusiness(ActBusinessTypeEnum.attendance_overtime.getCode(), overtimeId);

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
		List<StaffInfo> ccStaffList = droolsBusinessServiceImpl.selectWorkFlowCcStaffList(staffInfo.getAccount(), ActBusinessTypeEnum.attendance_overtime.getCode(), overtimeId, entity.getWorkflowId(), entity.getState());

		/**
		 * 撤销标志:0 不可撤销，1 可撤销
		 */
		String revokeTag = "0";
		if(entity!=null && entity.getCreateUserId().equals(userId) && entity.getState().equals("0")){
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

		AttendOvertime entity = attendOvertimeService.selectEntityById(id);

		actBusinessServiceImpl.deleteProcessInstance(entity.getWorkflowId(), memo);
		attendOvertimeService.revokeApply(id);

		return super.ok();
	}
}
