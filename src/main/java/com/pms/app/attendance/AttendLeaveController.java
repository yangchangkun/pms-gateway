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
import com.pms.common.enums.LeaveTypeEnum;
import com.pms.common.enums.McSysCodeEnum;
import com.pms.common.service.SysPushService;
import com.pms.common.utils.CalendarHelper;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.activiti.service.IActivitiQueryService;
import com.pms.core.attendance.domain.AttendEgressDetails;
import com.pms.core.attendance.domain.AttendLeave;
import com.pms.core.attendance.domain.AttendLeaveDetails;
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
 * 请假
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/attend/leave")
public class AttendLeaveController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IAttendLeaveService attendLeaveService;
	@Autowired
	private IAttendLeaveDetailsService attendLeaveDetailsService;

	@Autowired
	private IAttendEgressDetailsService attendEgressDetailsService;

	@Autowired
	private IAttendRecordDailyService attendRecordDailyService;

	@Autowired
	private IAttendHolidayYearService attendHolidayYearService;

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

		AttendLeave entity = new AttendLeave();
		if(myJoinProList!=null && myJoinProList.size()>0){
			Map<String, String> map = myJoinProList.get(0);
			entity.setProId(map.get("proId"));
			entity.setProName(map.get("proName"));
		} else {
			entity.setProId("");
			entity.setProName("");
		}
		entity.setLeaveType("3");
		entity.setStartTime(DateUtils.conversion(DateUtils.getCurrentDay()+" 09", DateUtils.YYYY_MM_DD_HH));
		entity.setEndTime(DateUtils.conversion(DateUtils.getCurrentDay()+" 18", DateUtils.YYYY_MM_DD_HH));
		entity.setDuration(0);
		entity.setReason("");
		entity.setState(0);
		entity.setWorkflowId("");
		//entity.setCreateUserId(userId);
		//entity.setCreateTime(new Date());
		//entity.setLastUpdateUserId(userId);
		//entity.setLastUpdateTime(new Date());

		String today = DateUtils.getCurrentDay();
		/**
		 * 获取职员假期信息（包括年假、调休和可请假次数）
		 */
		Map<String, Double> staffHolidayMap = attendLeaveService.getStaffHolidayMap(userId);
		int useSickQuantityOfQuarter = attendLeaveService.countSickLeaveByQuarter(userId, today); //本季度请假次数
		int useSickQuantityOfCurrMonth = attendLeaveService.countSickLeaveByCurrMonth(userId, today); //本月请假次数
		staffHolidayMap.put("useSickQuantityOfQuarter", Tool.convertStringToDouble(useSickQuantityOfQuarter));
		staffHolidayMap.put("useSickQuantityOfCurrMonth", Tool.convertStringToDouble(useSickQuantityOfCurrMonth));

		return super.ok().put("entity", entity)
				.put("myJoinProList", myJoinProList)
				.put("staffHolidayMap", staffHolidayMap);
	}

	/**
	 * 我的请假列表
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
		List<AttendLeave> list = attendLeaveService.selectList(params);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());

		return super.ok().put("gridDatas", list).put("webPage", page);
	}

	/**
	 * 提交请假记录
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
		String leaveType = Tool.convertObject(parameterMap.get("leaveType"));
		String startTime = Tool.convertObject(parameterMap.get("startTime"));
		String endTime = Tool.convertObject(parameterMap.get("endTime"));
		String reason = Tool.convertObject(parameterMap.get("reason"));

		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

		if(StringUtils.isEmpty(proId)){
			return super.error("项目不能为空");
		}
		if(StringUtils.isEmpty(startTime)){
			return super.error("请假开始时间不能为空");
		}
		if(StringUtils.isEmpty(endTime)){
			return super.error("请假结束时间不能为空");
		}
		if(StringUtils.isEmpty(reason)){
			return super.error("请描述请假原因或内容");
		}

		Date startDate = DateUtils.conversion(startTime, DateUtils.YYYY_MM_DD_HH);
		Date endDate = DateUtils.conversion(endTime, DateUtils.YYYY_MM_DD_HH);
		if(DateUtils.dateSubtraction(startDate, endDate)<0){
			return super.error("请假结束时间不能晚于请假开始时间");
		}

		int startH = DateUtils.getHoursByDateTimeStr(DateUtils.format(startDate, DateUtils.YYYY_MM_DD_HH_MM_SS));
		int endH = DateUtils.getHoursByDateTimeStr(DateUtils.format(endDate, DateUtils.YYYY_MM_DD_HH_MM_SS));
		if(startH!=9 && startH!=14 && startH!=18){
			return super.error("您选择的请假开始时间不符合规则的要求,目前只能选择[09、14、18]");
		}
		if(endH!=9 && endH!=14 && endH!=18){
			return super.error("您选择的请假结束时间不符合规则的要求,目前只能选择[09、14、18]");
		}

		String startDay = DateUtils.format(startDate, DateUtils.YYYY_MM_DD);
		String endDay = DateUtils.format(endDate, DateUtils.YYYY_MM_DD);
		Date sDay = DateUtils.conversion(startDay, DateUtils.YYYY_MM_DD);
		Date eDay = DateUtils.conversion(endDay, DateUtils.YYYY_MM_DD);

		/*int cnt = attendLeaveDetailsService.countAttendLeaveDetails(userId, startDay, endDay);
		if(cnt>0){
			return super.error("该时间段内您有正在申请或已经审批通过的请假信息,不能重复申请.");
		}
		int egressCnt = attendEgressDetailsService.countAttendEgressDetails(userId, startDay, endDay);
		if(egressCnt>0){
			return super.error("您申请请假的时间段有外出申请,不能再申请请假.");
		}*/
		/*int travelCnt = attendTravelDetailsService.countAttendTravelDetails(userId, startDay, endDay);
		if(travelCnt>0){
			return super.error("您申请请假的时间段有出差申请,不能再申请请假.");
		}*/


		StaffInfo staff = staffInfoService.selectEntityById(userId);

		/*Date joinDate = staff.getJoinDate();
		Date probationDate = DateUtils.addMonths(joinDate, 3); //试用期
		String probationDay = DateUtils.format(probationDate, DateUtils.YYYY_MM_DD);*/
		String probationDay = DateUtils.format(staff.getPositiveDate(), DateUtils.YYYY_MM_DD);
		String today = DateUtils.getCurrentDay();
		/**
		 * 员工在试用期的时候只能请 事假、调休、病假
		 */
		long diffDay = DateUtils.dateSubtraction(probationDay, today, DateUtils.YYYY_MM_DD);
		if(diffDay<0){
			if(!leaveType.equals(LeaveTypeEnum.absence_leave.getCode()) && !leaveType.equals(LeaveTypeEnum.sick_leave.getCode()) && !leaveType.equals(LeaveTypeEnum.rest_leave.getCode())){
				return super.error("您正处于试用期期间，只能请事假、病假和调休");
			}
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
		if(sDay.getTime()<joinProDate.getTime()){
			return super.error("请假开始时间早于参与项目的日期");
		}
		if(sDay.getTime()>departureProDate.getTime()){
			return super.error("请假开始时间晚于离开项目的日期");
		}
		if(eDay.getTime()<joinProDate.getTime()){
			return super.error("请假结束时间早于参与项目的日期");
		}
		if(eDay.getTime()>departureProDate.getTime()){
			return super.error("请假结束时间晚于离开项目的日期");
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
		 * 获取职员假期信息（包括年假、调休和可请假次数）
		 */
		Map<String, Double> staffHolidayMap = attendLeaveService.getStaffHolidayMap(userId);


		/**
		 * ID
		 */
		String leaveId = SnowflakeIdUtil.getId();



		/**
		 * 根据请假开始时间和结束时间自动拆解成请假明细
		 */
		List<AttendLeaveDetails> details = new ArrayList<>();

		/**
		 * 累积时长
		 */
		int duration = 0;

		/**
		 * 通过选择定的时间分析每日的请假明细
		 */
		Map<String, String> holidayMap = preferCalendarService.getHolidayOfSection(startDay, endDay, "0");
		List<Map<String, String>> detailList = CalendarHelper.calculationLeaveDuty(startTime, endTime, holidayMap);
		if(detailList!=null && detailList.size()>0){
			for(Map<String, String> map : detailList){
				String overtimeDay = map.get("day");
				String overtimeStartTime = map.get("startTime");
				String overtimeEndTime = map.get("endTime");
				int overtimeDuration = Tool.convertStringToInt(map.get("duration"));
				duration += overtimeDuration;

				AttendLeaveDetails bean = new AttendLeaveDetails();
				bean.setId(SnowflakeIdUtil.getId());
				bean.setUserId(userId);
				bean.setLeaveId(leaveId);
				bean.setLeaveDay(DateUtils.conversion(overtimeDay));
				bean.setLeaveStartTime(DateUtils.conversion(overtimeStartTime, DateUtils.YYYY_MM_DD_HH));
				bean.setLeaveEndTime(DateUtils.conversion(overtimeEndTime, DateUtils.YYYY_MM_DD_HH));
				bean.setLeaveDuration(overtimeDuration);
				details.add(bean);
			}
		} else {
			return super.error("通过请假开始时间和结束时间未能确定请假时长。请确认您是否在节假日请假?");
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

				for(AttendLeaveDetails currDetails : details){
					Date startDate2 = currDetails.getLeaveStartTime();
					Date endDate2 = currDetails.getLeaveEndTime();

					if(startDate1.getTime()<endDate2.getTime() && endDate1.getTime()>startDate2.getTime()){
						overlapLeaveTag = true;
						break;
					}
					/*if(
							(startDate1.getTime()+1>startDate2.getTime() && startDate1.getTime()<endDate2.getTime())
									|| (endDate1.getTime()>startDate2.getTime() && endDate1.getTime()-1<endDate2.getTime())
									|| (startDate2.getTime()+1>startDate1.getTime() && startDate2.getTime()<endDate1.getTime())
									|| (endDate2.getTime()>startDate1.getTime() && endDate2.getTime()-1<endDate1.getTime())
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

				for(AttendLeaveDetails currDetails : details){
					Date startDate2 = currDetails.getLeaveStartTime();
					Date endDate2 = currDetails.getLeaveEndTime();

					if(startDate1.getTime()<endDate2.getTime() && endDate1.getTime()>startDate2.getTime()){
						overlapEgressTag = true;
						break;
					}

					/*if( (startDate2.getTime()+1>=startDate1.getTime() && startDate2.getTime()<endDate1.getTime())
							||
							(endDate2.getTime()+1>=startDate1.getTime() && endDate2.getTime()<endDate1.getTime())
							){

						overlapEgressTag = true;
						break;
					}*/
				}
			}
		}
		if(overlapEgressTag){
			return super.error("您申请请假的时间段有外出申请,不能再申请请假.");
		}

		/**
		 * 请病假，2天及以上，必须上传附件，请婚假，必须上传附件---具体的高雷鸣提供附件后补虬
		 */
		if(leaveType.equals(LeaveTypeEnum.sick_leave.getCode()) ){
			if(duration>=16){
				if(StringUtils.isEmpty(oss)){
					return super.error("请病假，2天及以上，必须上传附件");
				}
			}

			int useSickQuantityOfQuarter = attendLeaveService.countSickLeaveByQuarter(userId, startDay); //本季度请假次数
			int useSickQuantityOfCurrMonth = attendLeaveService.countSickLeaveByCurrMonth(userId, startDay); //本月请假次数

			if(useSickQuantityOfCurrMonth>=1){
				return super.error("病假一个月只能申请一次");
			}
			if(useSickQuantityOfQuarter>=2){
				return super.error("病假一个季度只能申请两次");
			}

		} else if(leaveType.equals(LeaveTypeEnum.marriage_leave.getCode())){
			if(StringUtils.isEmpty(oss)){
				return super.error("请婚假，必须上传附件");
			}
			Map<String, Integer> countLeaveTypeMap = attendLeaveService.countAttendLeaveTypeByUserId(userId);
			if(countLeaveTypeMap.containsKey(LeaveTypeEnum.marriage_leave.getCode()) && countLeaveTypeMap.get(LeaveTypeEnum.marriage_leave.getCode())>0){
				return super.error("婚假只能请一次");
			}
		} else if(leaveType.equals(LeaveTypeEnum.rest_leave.getCode())){
			double validRestDuration = staffHolidayMap.get("validRestDuration");

			/**
			 * todo 目前先不检验实际的可用调休时长，由各部门自行决定
			 */
			/*if(duration > validRestDuration){
				return super.error("可用调休时长已经超过您实际申请的调休时长.");
			}*/
		} else if(leaveType.equals(LeaveTypeEnum.year_leave.getCode())){
			/**
			 * 可用年假
			 */
			double validYearHolidays = staffHolidayMap.get("validYearHolidays");
			double validYearHolidaysDuration = validYearHolidays*8;
			if(duration > validYearHolidaysDuration){
				return super.error("可用年假时长已经超过您实际申请的时长.");
			}
		}

		AttendLeave entity = new AttendLeave();
		entity.setId(leaveId);
		entity.setProId(proId);
		entity.setLeaveType(leaveType);
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
		attendLeaveService.insertEntity(entity);

		attendLeaveDetailsService.batchInsertEntity(details);


		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<WorkflowOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				WorkflowOss wo = new WorkflowOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setBusinessKey(ActBusinessTypeEnum.attendance_leave.getCode());
				wo.setBusinessId(leaveId);
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
		otherParam.put(ActBusinessVariablesEnum.days.getCode(), detailList.size()); //传入请假天数

		String leaveTypeLabel = "其他";
		if(leaveType.equals("0")){
			leaveTypeLabel = "事假";
		} else if(leaveType.equals("1")){
			leaveTypeLabel = "病假";
		} else if(leaveType.equals("2")){
			leaveTypeLabel = "调休";
		} else if(leaveType.equals("3")){
			leaveTypeLabel = "年假";
		} else if(leaveType.equals("4")){
			leaveTypeLabel = "婚假";
		} else if(leaveType.equals("5")){
			leaveTypeLabel = "丧假";
		} else if(leaveType.equals("6")){
			leaveTypeLabel = "产假";
		} else if(leaveType.equals("7")){
			leaveTypeLabel = "陪产假";
		} else if(leaveType.equals("8")){
			leaveTypeLabel = "产检假";
		} else if(leaveType.equals("9")){
			leaveTypeLabel = "其它";
		}
		String businessMemo = staff.getName()+"申请请假,请假类型:"+leaveTypeLabel+",请假时间:从"+startTime+"至"+endTime+",共"+duration+"小时";
		ProcessInstance pi = actBusinessServiceImpl.startProcess(staff.getAccount(), staff.getName(), ActBusinessTypeEnum.attendance_leave, leaveId, businessMemo, otherParam);

		/**
		 * 关联表单和工作流
		 */
		attendLeaveService.updateWorkflowId(pi.getId(), leaveId);

		/**
		 * 保存抄送信息
		 */
		if(StringUtils.isNotEmpty(ccAccounts)){
			List<WorkflowCc> ccDatas = new ArrayList<>();
			String[] ccArray = ccAccounts.split(",");
			for(String ccAccount : ccArray){
				WorkflowCc cc = new WorkflowCc();
				cc.setId(SnowflakeIdUtil.getId());
				cc.setBusinessKey(ActBusinessTypeEnum.attendance_leave.getCode());
				cc.setBusinessId(leaveId);
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
	 * 查看请假明细
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
		String leaveId = Tool.convertObject(parameterMap.get("leaveId"));

		if(StringUtils.isEmpty(userId)){
			return super.error("userId参数不能为空");
		}
		if(StringUtils.isEmpty(leaveId)){
			return super.error("ID参数不能为空");
		}

		AttendLeave entity = attendLeaveService.selectEntityById(leaveId);
		if(entity==null){
			return super.error("未查询到申请单");
		}
		StaffInfo staffInfo = staffInfoService.selectEntityById(entity.getCreateUserId());

		List<AttendLeaveDetails> details = attendLeaveDetailsService.selectRecordsByLeaveId(leaveId);
		List<Map<String, String>> actHis = activitiQueryService.selectHistoryFlowList(entity.getWorkflowId());
		List<WorkflowOss> ossList = workflowOssService.selectListByBusiness(ActBusinessTypeEnum.attendance_leave.getCode(), leaveId);

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
		List<StaffInfo> ccStaffList = droolsBusinessServiceImpl.selectWorkFlowCcStaffList(staffInfo.getAccount(), ActBusinessTypeEnum.attendance_leave.getCode(), leaveId, entity.getWorkflowId(), entity.getState()+"");

		/**
		 * 获取职员假期信息（包括年假、调休和可请假次数）
		 */
		Map<String, Double> staffHolidayMap = attendLeaveService.getStaffHolidayMap(entity.getCreateUserId());
		int useSickQuantityOfQuarter = attendLeaveService.countSickLeaveByQuarter(userId, DateUtils.format(entity.getStartTime())); //本季度请假次数
		int useSickQuantityOfCurrMonth = attendLeaveService.countSickLeaveByCurrMonth(userId, DateUtils.format(entity.getStartTime())); //本月请假次数
		staffHolidayMap.put("useSickQuantityOfQuarter", Tool.convertStringToDouble(useSickQuantityOfQuarter));
		staffHolidayMap.put("useSickQuantityOfCurrMonth", Tool.convertStringToDouble(useSickQuantityOfCurrMonth));

		/**
		 * 撤销标志:0 不可撤销，1 可撤销
		 */
		String revokeTag = "0";
		if(entity!=null && entity.getCreateUserId().equals(userId) && entity.getState()==0){
			revokeTag = "1";
		}

		return super.ok().put("entity", entity).put("details", details).put("actHis", actHis).put("ossList", ossList).put("ccStaffList", ccStaffList)
				.put("candidateList", candidateList)
				.put("staffHolidayMap", staffHolidayMap)
				.put("revokeTag", revokeTag);
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

		AttendLeave entity = attendLeaveService.selectEntityById(id);

		actBusinessServiceImpl.deleteProcessInstance(entity.getWorkflowId(), memo);
		attendLeaveService.revokeApply(id);

		return super.ok();
	}
}
