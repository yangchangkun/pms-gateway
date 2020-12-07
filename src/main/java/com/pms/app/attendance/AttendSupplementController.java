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
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.activiti.service.IActivitiQueryService;
import com.pms.core.attendance.domain.AttendGroup;
import com.pms.core.attendance.domain.AttendRecordDaily;
import com.pms.core.attendance.domain.AttendSupplement;
import com.pms.core.attendance.service.IAttendGroupService;
import com.pms.core.attendance.service.IAttendRecordDailyService;
import com.pms.core.attendance.service.IAttendSupplementService;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrMemberJoinService;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.prefer.service.IPreferCalendarService;
import com.pms.core.project.domain.ProBaseEntity;
import com.pms.core.project.service.impl.ProBaseService;
import com.pms.core.project.service.impl.ProTeamService;
import com.pms.core.system.domain.SysDept;
import com.pms.core.system.service.ISysDeptService;
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
 * 补卡
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/attend/supplement")
public class AttendSupplementController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IAttendSupplementService attendSupplementService;
	@Autowired
	private IAttendRecordDailyService attendRecordDailyService;

	@Autowired
	private IAttendGroupService attendGroupService;

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
	private IWorkflowOssService workflowOssService;
	@Autowired
	private IWorkflowCcService workflowCcService;

	@Autowired
	private IPreferCalendarService preferCalendarService;
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

		String today = DateUtils.getCurrentDay();

		AttendSupplement entity = new AttendSupplement();
		entity.setId("");
		if(myJoinProList!=null && myJoinProList.size()>0){
			Map<String, String> map = myJoinProList.get(0);
			entity.setProId(map.get("proId"));
			entity.setProName(map.get("proName"));
		} else {
			entity.setProId("");
			entity.setProName("");
		}
		entity.setSupplementDay(new Date());
		entity.setSupplementType("0");
		entity.setSupplementTime(DateUtils.conversion(today+" 09:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS));
		entity.setReason("");
		entity.setState(0);
		entity.setWorkflowId("");
		//entity.setCreateUserId(userId);
		//entity.setCreateTime(new Date());
		//entity.setLastUpdateUserId(userId);
		//entity.setLastUpdateTime(new Date());

		String supplementMonth = DateUtils.format(entity.getSupplementTime(), DateUtils.YYYY_MM);
		int count = attendSupplementService.countAttendSupplementByMonth(userId, supplementMonth);

		return super.ok().put("entity", entity).put("myJoinProList", myJoinProList).put("count", count);
	}

	/**
	 * 我的补卡列表
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

		String orderBy = "supplement_day desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);


		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<AttendSupplement> list = attendSupplementService.selectList(params);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());

		return super.ok().put("gridDatas", list).put("webPage", page);
	}

	/**
	 * 提交补卡记录
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
		String supplementType = Tool.convertObject(parameterMap.get("supplementType"));
		String supplementTime = Tool.convertObject(parameterMap.get("supplementTime"));
		String reason = Tool.convertObject(parameterMap.get("reason"));

		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

		if(StringUtils.isEmpty(proId)){
			return super.error("项目不能为空");
		}
		if(StringUtils.isEmpty(supplementTime)){
			return super.error("补卡时间不能为空");
		}
		if(StringUtils.isEmpty(supplementType)){
			return super.error("补卡班次不能为空");
		}
		if(StringUtils.isEmpty(reason)){
			return super.error("请描述补卡原因或内容");
		}

		Date time = DateUtils.conversion(supplementTime, DateUtils.YYYY_MM_DD_HH_MM_SS);
		String supplementDay = DateUtils.format(time, DateUtils.YYYY_MM_DD);
		Date day = DateUtils.conversion(supplementDay, DateUtils.YYYY_MM_DD);

		int cnt = attendSupplementService.countAttendSupplement(userId, supplementType, supplementDay);
		if(cnt>0){
			return super.error("该时间段内您有正在申请或已经审批通过的补卡信息,不能重复申请.");
		}


		/**
		 * 不能早于当前时间
		 * 补上班卡，如果在该考勤组的最晚上班时间内有正常打卡记录（例如，总部考勤组9:30之前有打卡记录）就不让补上班卡
		 * 补下班卡，先判断是否有下班打卡，如果有，补卡不能早于原下班打卡时间
		 * 补下班卡，有上班打卡记录，下班补卡不能早于这个时间
		 * 补下班卡，有上班补卡申请记录（提交），下班补卡不能早于这个时间
		 */
		long diffDay = DateUtils.dateSubtraction(new Date(), time);
		if(diffDay>=0){
			return super.error("不能申请超过当前时间的补卡.");
		}
		/**
		 * 查询当日已经打卡的考勤记录
		 */
		AttendRecordDaily daily = null;
		List<AttendRecordDaily> dailyList = attendRecordDailyService.selectTodayList(userId, supplementDay);
		if(dailyList!=null && dailyList.size()>0){
			daily = dailyList.get(0);
		}
		/**
		 * supplementType 0 上班，1 下班
		 * 如果是节假日，不管是否是正常的打卡时间都可以进行补卡
		 */
		if(supplementType.equals("0")){
			if(daily!=null){
				if(daily.getDailyType() != null && daily.getDailyType().equals("1")){
					if(daily.getSignInType().equals("0")){
						return super.error("您有正常的上班打卡记录,不允许再补上班卡。");
					}
				}
				Date signInTime = daily.getSignInTime();
				if(signInTime!=null && DateUtils.dateSubtraction(signInTime, time)>0){
					return super.error("您当日有上班打卡记录。补卡的时间不能晚于您当日上班打卡的时间。");
				}
			}

		} else {
			if(daily!=null){
				if(daily.getSignInTime()!=null && DateUtils.dateSubtraction(time, daily.getSignInTime())>0){
					return super.error("您申请的下班补卡时间不能早于你实际的上班打卡时间。");
				}
				if(daily.getSignOutTime()!=null && DateUtils.dateSubtraction(time, daily.getSignOutTime())>0){
					return super.error("您申请的下班补卡时间不能早于你实际的下班打卡时间。");
				}
			}
			/**
			 * 上班补卡记录
			 */
			AttendSupplement inSupplementEntity = null;
			List<AttendSupplement> inSupplementList = attendSupplementService.selectListByDay(userId, "0", supplementDay);
			if(inSupplementList!=null && inSupplementList.size()>0){
				inSupplementEntity = inSupplementList.get(0);
			}
			if(inSupplementEntity!=null && DateUtils.dateSubtraction(time, inSupplementEntity.getSupplementTime())>0){
				return super.error("您有正在申请或已经审批通过的上班补卡信息，您申请的下班补卡时间不能早于申请的上班补卡时间。");
			}

			/**
			 * 如果补下班卡，但是时间在正常的考勤上班时间范围内，则不允许补卡
			 */
			/**
			 * 查询系统设置的默认的考勤组
			 */
			AttendGroup hitGroup = attendGroupService.selectDefaultGroup();
			if(hitGroup!=null){
				Date signElasticTime = DateUtils.conversion((supplementDay+" "+hitGroup.getSignElasticTime()), DateUtils.YYYY_MM_DD_HH_MM);
				if(DateUtils.dateSubtraction(time, signElasticTime)>0){
					return super.error("正常的考勤上班时间范围内,不允许补下班卡。");
				}
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
		if(time.getTime()<joinProDate.getTime()){
			return super.error("补卡时间早于参与项目的日期");
		}
		if(time.getTime()>departureProDate.getTime()){
			return super.error("补卡时间晚于离开项目的日期");
		}

		String supplementTypeLabel = "上班";
		if(supplementType.equals("1")){
			supplementTypeLabel = "下班";
		}

		/**
		 * 查询职员信息
		 */
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
		String uuid = SnowflakeIdUtil.getId();

		AttendSupplement entity = new AttendSupplement();
		entity.setId(uuid);
		entity.setProId(proId);
		entity.setSupplementDay(day);
		entity.setSupplementType(supplementType);
		entity.setSupplementTime(time);

		entity.setReason(reason);
		entity.setState(0);
		entity.setWorkflowId("");
		entity.setCreateUserId(userId);
		entity.setCreateTime(new Date());
		entity.setLastUpdateUserId(userId);
		entity.setLastUpdateTime(new Date());
		attendSupplementService.insertEntity(entity);

		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<WorkflowOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				WorkflowOss wo = new WorkflowOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setBusinessKey(ActBusinessTypeEnum.attendance_supplement.getCode());
				wo.setBusinessId(uuid);
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

		String businessMemo = staff.getName()+"申请补卡,补卡时间:"+supplementTime+",补卡班次:"+supplementTypeLabel;
		ProcessInstance pi = actBusinessServiceImpl.startProcess(staff.getAccount(), staff.getName(), ActBusinessTypeEnum.attendance_supplement, uuid, businessMemo, otherParam);

		/**
		 * 关联表单和工作流
		 */
		attendSupplementService.updateWorkflowId(pi.getId(), uuid);

		/**
		 * 保存抄送信息
		 */
		if(StringUtils.isNotEmpty(ccAccounts)){
			List<WorkflowCc> ccDatas = new ArrayList<>();
			String[] ccArray = ccAccounts.split(",");
			for(String ccAccount : ccArray){
				WorkflowCc cc = new WorkflowCc();
				cc.setId(SnowflakeIdUtil.getId());
				cc.setBusinessKey(ActBusinessTypeEnum.attendance_supplement.getCode());
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
	 * 查看补卡明细
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
		String supplementId = Tool.convertObject(parameterMap.get("supplementId"));

		if(StringUtils.isEmpty(userId)){
			return super.error("userId参数不能为空");
		}
		if(StringUtils.isEmpty(supplementId)){
			return super.error("ID参数不能为空");
		}

		AttendSupplement entity = attendSupplementService.selectEntityById(supplementId);
		if(entity==null){
			return super.error("未查询到申请单");
		}
		StaffInfo staffInfo = staffInfoService.selectEntityById(entity.getCreateUserId());

		List<Map<String, String>> actHis = activitiQueryService.selectHistoryFlowList(entity.getWorkflowId());
		List<WorkflowOss> ossList = workflowOssService.selectListByBusiness(ActBusinessTypeEnum.attendance_supplement.getCode(), supplementId);

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
		List<StaffInfo> ccStaffList = droolsBusinessServiceImpl.selectWorkFlowCcStaffList(staffInfo.getAccount(), ActBusinessTypeEnum.attendance_supplement.getCode(), supplementId, entity.getWorkflowId(), entity.getState()+"");

		/**
		 * 撤销标志:0 不可撤销，1 可撤销
		 */
		String revokeTag = "0";
		if(entity!=null && entity.getCreateUserId().equals(userId) && entity.getState()==0){
			revokeTag = "1";
		}

		Date supplementTime = entity.getSupplementTime();
		String supplementMonth = DateUtils.format(supplementTime, DateUtils.YYYY_MM);
		int count = attendSupplementService.countAttendSupplementByMonth(entity.getCreateUserId(), supplementMonth);

		return super.ok().put("entity", entity).put("actHis", actHis).put("ossList", ossList).put("candidateList", candidateList).put("ccStaffList", ccStaffList)
				.put("revokeTag", revokeTag)
				.put("supplementMonth", supplementMonth).put("monthCount", count);
	}


	/**
	 * 查看月补卡明细
	 * @return
	 */
	@RequestMapping("/monthSupplementList")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult monthSupplementList(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String applyUserId = Tool.convertObject(parameterMap.get("applyUserId"));
		String month = Tool.convertObject(parameterMap.get("month"));

		if(StringUtils.isEmpty(applyUserId)){
			return super.error("申请人参数不能为空");
		}
		if(StringUtils.isEmpty(month)){
			return super.error("查询月份参数不能为空");
		}

		List<AttendSupplement> supplementList = attendSupplementService.selectListByMonth(applyUserId, month);

		return super.ok().put("supplementList", supplementList);
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

		AttendSupplement entity = attendSupplementService.selectEntityById(id);

		actBusinessServiceImpl.deleteProcessInstance(entity.getWorkflowId(), memo);
		attendSupplementService.revokeApply(id);

		return super.ok();
	}


	/**
	 * 统计某月某员工的补卡次数
	 * @return
	 */
	@RequestMapping("/countAttendSupplementByMonth")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult countAttendSupplementByMonth(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		String month = Tool.convertObject(parameterMap.get("month"));

		if(StringUtils.isEmpty(userId)){
			return super.error("userId参数不能为空");
		}
		if(StringUtils.isEmpty(month)){
			return super.error("月份不能为空");
		}

		int count = attendSupplementService.countAttendSupplementByMonth(userId, month);

		return super.ok().put("count", count);
	}
}
