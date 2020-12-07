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
import com.pms.common.enums.McSysCodeEnum;
import com.pms.common.service.SysPushService;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.activiti.service.IActivitiQueryService;
import com.pms.core.attendance.domain.AttendTravel;
import com.pms.core.attendance.service.IAttendTravelService;
import com.pms.core.hr.domain.HrVisitSubsidy;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrMemberJoinService;
import com.pms.core.hr.service.IHrVisitSubsidyService;
import com.pms.core.hr.service.IStaffInfoService;
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
 * 探亲补助
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/hr/visitSubsidy")
public class VisitSubsidyController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IHrVisitSubsidyService hrVisitSubsidyService;

	@Autowired
	private IAttendTravelService attendTravelService;

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

		HrVisitSubsidy entity = new HrVisitSubsidy();
		entity.setId("");
		entity.setApplyUserId(userId);
		entity.setApplyUserName(staff.getName());

		entity.setBaseAddress(staff.getJoinAddr());
		entity.setTravelAddress("");
		entity.setTravelStartDay(new Date());
		entity.setTravelEndDay(new Date());
		entity.setTravelDays(1);
		entity.setSubsidyStartDay(new Date());
		entity.setSubsidyEndDay(new Date());
		entity.setSubsidyDays(1);
		entity.setSubsidyPrice(0d);

		entity.setState("0");
		entity.setWorkflowId("");

		entity.setCreateBy(userId);
		entity.setCreateTime(new Date());
		//entity.setUpdateBy(userId);
		//entity.setUpdateTime(new Date());
		if(myJoinProList!=null && myJoinProList.size()>0){
			Map<String, String> map = myJoinProList.get(0);
			entity.setProId(map.get("proId"));
			entity.setProName(map.get("proName"));
		} else {
			entity.setProId("");
			entity.setProName("");
		}

		return super.ok().put("entity", entity).put("staff", staff).put("myJoinProList", myJoinProList);
	}


	/**
	 * 我的探亲补助列表
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

		String orderBy = " hvs.create_time desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("applyUserId", userId);


		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<HrVisitSubsidy> list = hrVisitSubsidyService.selectList(params);
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

		String proId = Tool.convertObject(parameterMap.get("proId"));

		String baseAddress = Tool.convertObject(parameterMap.get("baseAddress"));
		String travelAddress = Tool.convertObject(parameterMap.get("travelAddress"));

		String travelStartDay = Tool.convertObject(parameterMap.get("travelStartDay"));
		String travelEndDay = Tool.convertObject(parameterMap.get("travelEndDay"));
		int travelDays = Tool.convertStringToInt(parameterMap.get("travelDays"));

		String subsidyStartDay = Tool.convertObject(parameterMap.get("subsidyStartDay"));
		String subsidyEndDay = Tool.convertObject(parameterMap.get("subsidyEndDay"));
		int subsidyDays = Tool.convertStringToInt(parameterMap.get("subsidyDays"));
		Double subsidyPrice = Tool.convertStringToDouble(parameterMap.get("subsidyPrice"));

		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

		if(StringUtils.isEmpty(proId)){
			return super.error("请选择所在项目");
		}
		if(StringUtils.isEmpty(baseAddress)){
			return super.error("请输入归属地");
		}
		if(StringUtils.isEmpty(travelAddress)){
			return super.error("请输入出差地");
		}
		if(StringUtils.isEmpty(travelStartDay)){
			return super.error("请选择出差开始日期");
		}
		if(StringUtils.isEmpty(travelEndDay)){
			return super.error("请选择预计出差结束日期");
		}
		if(travelDays<=0){
			return super.error("请输入已出差天数");
		}
		if(StringUtils.isEmpty(subsidyStartDay)){
			return super.error("请选择补助开始日期");
		}
		if(StringUtils.isEmpty(subsidyEndDay)){
			return super.error("请选择补助结束日期");
		}
		if(subsidyDays<=0){
			return super.error("请输入补助天数");
		}
		if(subsidyPrice<=0){
			return super.error("请输入补助金额");
		}

		Date travelStartDate = DateUtils.conversion(travelStartDay);
		Date travelEndDate = DateUtils.conversion(travelEndDay);
		Date subsidyStartDate = DateUtils.conversion(subsidyStartDay);
		Date subsidyEndDate = DateUtils.conversion(subsidyEndDay);

		int diffTravelDays = DateUtils.dateDiffDay(travelStartDate, travelEndDate);
		if(diffTravelDays<0){
			return super.error("预计出差结束日期不能早于出差开始日期");
		}
		//travelDays = (diffTravelDays+1);

		int diffSubsidyDays = DateUtils.dateDiffDay(subsidyStartDate, subsidyEndDate);
		if(diffSubsidyDays<0){
			return super.error("补助结束日期不能早于补助开始日期");
		}
		//subsidyDays = (diffSubsidyDays+1);
		if(subsidyDays<30){
			return super.error("补助日期区间不能少于30天");
		}

		List<HrVisitSubsidy> applyList = hrVisitSubsidyService.selectApplyingList(userId, subsidyStartDay, subsidyEndDay);
		if(applyList!=null && applyList.size()>0){
			return super.error("您有正在处理中或已经审批通过的同类型的申请信息,不能重复申请");
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
		String id = SnowflakeIdUtil.getId();

		HrVisitSubsidy entity = new HrVisitSubsidy();
		entity.setId(id);
		entity.setApplyUserId(userId);

		entity.setProId(proId);
		entity.setBaseAddress(baseAddress);
		entity.setTravelAddress(travelAddress);
		entity.setTravelStartDay(travelStartDate);
		entity.setTravelEndDay(travelEndDate);
		entity.setTravelDays(travelDays);
		entity.setSubsidyStartDay(subsidyStartDate);
		entity.setSubsidyEndDay(subsidyEndDate);
		entity.setSubsidyDays(subsidyDays);
		entity.setSubsidyPrice(subsidyPrice);

		entity.setState("0");
		entity.setWorkflowId("");

		entity.setCreateBy(userId);
		entity.setCreateTime(new Date());
		//entity.setUpdateBy(userId);
		//entity.setUpdateTime(new Date());
		hrVisitSubsidyService.insertEntity(entity);

		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<WorkflowOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				WorkflowOss wo = new WorkflowOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setBusinessKey(ActBusinessTypeEnum.hr_visit_subsidy.getCode());
				wo.setBusinessId(id);
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

		String businessMemo = staff.getName()+"申请出差探亲补助,补助开始日期:"+subsidyStartDay+",补助结束日期:"+subsidyEndDay+",补助天数:"+subsidyDays+",补助金额:"+subsidyPrice;
		ProcessInstance pi = actBusinessServiceImpl.startProcess(staff.getAccount(), staff.getName(), ActBusinessTypeEnum.hr_visit_subsidy, id, businessMemo, otherParam);

		/**
		 * 关联表单和工作流
		 */
		hrVisitSubsidyService.updateWorkflowId(pi.getId(), id);

		/**
		 * 保存抄送信息
		 */
		if(StringUtils.isNotEmpty(ccAccounts)){
			List<WorkflowCc> ccDatas = new ArrayList<>();
			String[] ccArray = ccAccounts.split(",");
			for(String ccAccount : ccArray){
				WorkflowCc cc = new WorkflowCc();
				cc.setId(SnowflakeIdUtil.getId());
				cc.setBusinessKey(ActBusinessTypeEnum.hr_visit_subsidy.getCode());
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
	 * 查看探亲补助明细
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
		String visitSubsidyId = Tool.convertObject(parameterMap.get("visitSubsidyId"));

		if(StringUtils.isEmpty(userId)){
			return super.error("userId参数不能为空");
		}
		if(StringUtils.isEmpty(visitSubsidyId)){
			return super.error("ID参数不能为空");
		}

		HrVisitSubsidy entity = hrVisitSubsidyService.selectEntityById(visitSubsidyId);
		if(entity==null){
			return super.error("未查询到申请单");
		}
		StaffInfo staffInfo = staffInfoService.selectEntityById(entity.getCreateBy());

		List<Map<String, String>> actHis = activitiQueryService.selectHistoryFlowList(entity.getWorkflowId());
		List<WorkflowOss> ossList = workflowOssService.selectListByBusiness(ActBusinessTypeEnum.hr_visit_subsidy.getCode(), visitSubsidyId);

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
		List<StaffInfo> ccStaffList = droolsBusinessServiceImpl.selectWorkFlowCcStaffList(staffInfo.getAccount(), ActBusinessTypeEnum.hr_visit_subsidy.getCode(), visitSubsidyId, entity.getWorkflowId(), entity.getState());

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

		HrVisitSubsidy entity = hrVisitSubsidyService.selectEntityById(id);

		actBusinessServiceImpl.deleteProcessInstance(entity.getWorkflowId(), memo);
		hrVisitSubsidyService.revokeApply(id);

		return super.ok();
	}




	/**
	 * 查看探亲补助的参考记录
	 * @return
	 */
	@RequestMapping("/referenceHis")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult referenceHis(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String excludeId = Tool.convertObject(parameterMap.get("excludeId"));
		String applyUserId = Tool.convertObject(parameterMap.get("applyUserId"));

		if(StringUtils.isEmpty(applyUserId)){
			return super.error("applyUserId 参数不能为空");
		}

		/**
		 * 最近10条补助申请
		 *
		 */
		List<HrVisitSubsidy> subsidyHisList = hrVisitSubsidyService.selectTopTenByUserId(excludeId, applyUserId);

		/**
		 * 最近10条出差申请
		 *
		 */
		List<AttendTravel> travelHisList = attendTravelService.selectTopTenByUserId(applyUserId);

		return super.ok().put("subsidyHisList", subsidyHisList).put("travelHisList", travelHisList);
	}
}
