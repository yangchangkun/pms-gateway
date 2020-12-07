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
import com.pms.core.hr.domain.HrJoinApply;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrJoinApplyService;
import com.pms.core.hr.service.IHrMemberJoinService;
import com.pms.core.hr.service.IStaffInfoService;
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
 * 转正
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/hr/joinApply")
public class JoinApplyController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IHrJoinApplyService hrJoinApplyService;

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
	private ISysDictDataService dictDataService;

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

		HrJoinApply entity = new HrJoinApply();
		entity.setId("");
		entity.setApplyUserId(staff.getUserId());
		entity.setApplyUserName(staff.getName());
		entity.setJoinDate(staff.getJoinDate());

		entity.setProbationPeriod(staff.getProbationPeriod()+"");
		if(staff.getPositiveDate() != null){
			entity.setOfficialDate(staff.getPositiveDate());
		}else {
			entity.setOfficialDate(DateUtils.addMonths(staff.getJoinDate(), staff.getProbationPeriod()));
		}
		entity.setWorkContent("");
		entity.setWorkAchievement("");
		entity.setTraining("");
		entity.setHarvest("");
		entity.setSelfAssessment("");
		entity.setTarget("");
		entity.setState("0");

		if(myJoinProList!=null && myJoinProList.size()>0){
			Map<String, String> map = myJoinProList.get(0);
			entity.setProId(map.get("proId"));
			entity.setProName(map.get("proName"));
		} else {
			entity.setProId("");
			entity.setProName("");
		}

		entity.setWorkflowId("");
		entity.setCreateBy(userId);
		entity.setCreateTime(new Date());
		entity.setUpdateBy(userId);
		entity.setUpdateTime(new Date());

		return super.ok().put("entity", entity).put("myJoinProList", myJoinProList);
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

		String orderBy = "hja.create_time desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("applyUserId", userId);


		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<HrJoinApply> list = hrJoinApplyService.selectList(params);
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
		String probationPeriod = Tool.convertObject(parameterMap.get("probationPeriod"));
		String officialDate = Tool.convertObject(parameterMap.get("officialDate"));
		String workContent = Tool.convertObject(parameterMap.get("workContent"));
		String workAchievement = Tool.convertObject(parameterMap.get("workAchievement"));
		String training = Tool.convertObject(parameterMap.get("training"));
		String harvest = Tool.convertObject(parameterMap.get("harvest"));
		String selfAssessment = Tool.convertObject(parameterMap.get("selfAssessment"));
		String target = Tool.convertObject(parameterMap.get("target"));

		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

		if(StringUtils.isEmpty(proId)){
			return super.error("请选择项目,如果你未加入项目请联系项目经理");
		}
		if(StringUtils.isEmpty(officialDate)){
			return super.error("请选择转正日期");
		}
		if(StringUtils.isEmpty(workContent)){
			return super.error("请填写工作内容");
		}
		if(StringUtils.isEmpty(workAchievement)){
			return super.error("请填写工作业绩");
		}
		if(StringUtils.isEmpty(training)){
			return super.error("请填写入职培训内容");
		}
		if(StringUtils.isEmpty(harvest)){
			return super.error("请填写成长和收获");
		}
		if(StringUtils.isEmpty(selfAssessment)){
			return super.error("请填写自我评价");
		}
		if(StringUtils.isEmpty(target)){
			return super.error("请填写目标和计划");
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

		Date joinDate = staff.getJoinDate();
		Date officialDay = DateUtils.conversion(officialDate);

		if(DateUtils.dateSubtraction(joinDate, officialDay)<0){
			return super.error("转正日期不能晚于入职日期");
		}

		List<HrJoinApply> applyList = hrJoinApplyService.selectApplyingByUserId(userId);
		if(applyList!=null && applyList.size()>0){
			return super.error("您有正在处理中或已经审批通过的转正申请信息,不能重复申请");
		}

		/**
		 * ID
		 */
		String applyId = SnowflakeIdUtil.getId();

		HrJoinApply entity = new HrJoinApply();
		entity.setId(applyId);
		entity.setApplyUserId(staff.getUserId());
		entity.setJoinDate(staff.getJoinDate());

		entity.setProId(proId);

		entity.setProbationPeriod(probationPeriod);
		entity.setOfficialDate(officialDay);
		entity.setWorkContent(workContent);
		entity.setWorkAchievement(workAchievement);
		entity.setTraining(training);
		entity.setHarvest(harvest);
		entity.setSelfAssessment(selfAssessment);
		entity.setTarget(target);
		entity.setState("0");

		entity.setWorkflowId("");
		entity.setCreateBy(userId);
		entity.setCreateTime(new Date());
		//entity.setUpdateBy(userId);
		//entity.setUpdateTime(new Date());
		hrJoinApplyService.insertEntity(entity);

		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<WorkflowOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				WorkflowOss wo = new WorkflowOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setBusinessKey(ActBusinessTypeEnum.hr_join_apply.getCode());
				wo.setBusinessId(applyId);
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

		String businessMemo = staff.getName()+"申请转正,入职部门:"+staff.getJoinDeptName()+",入职职位"+staff.getPostName();
		ProcessInstance pi = actBusinessServiceImpl.startProcess(staff.getAccount(), staff.getName(), ActBusinessTypeEnum.hr_join_apply, applyId, businessMemo, otherParam);

		/**
		 * 关联表单和工作流
		 */
		hrJoinApplyService.updateWorkflowId(pi.getId(), applyId);

		/**
		 * 保存抄送信息
		 */
		if(StringUtils.isNotEmpty(ccAccounts)){
			List<WorkflowCc> ccDatas = new ArrayList<>();
			String[] ccArray = ccAccounts.split(",");
			for(String ccAccount : ccArray){
				WorkflowCc cc = new WorkflowCc();
				cc.setId(SnowflakeIdUtil.getId());
				cc.setBusinessKey(ActBusinessTypeEnum.hr_join_apply.getCode());
				cc.setBusinessId(applyId);
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

		HrJoinApply entity = hrJoinApplyService.selectEntityById(id);
		if(entity==null){
			return super.error("未查询到申请单");
		}
		StaffInfo staffInfo = staffInfoService.selectEntityById(entity.getCreateBy());

		String joinTips = ""; //转正状态提示
		int d = DateUtils.dateDiffDay(DateUtils.addMonths(entity.getJoinDate(), 3), entity.getOfficialDate());
		if(d<0){
			joinTips = "提前"+Math.abs(d)+"天转正";
		} else {
			joinTips = "延后"+Math.abs(d)+"天转正";
		}

		List<Map<String, String>> actHis = activitiQueryService.selectHistoryFlowList(entity.getWorkflowId());
		List<WorkflowOss> ossList = workflowOssService.selectListByBusiness(ActBusinessTypeEnum.hr_join_apply.getCode(), id);

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
		List<StaffInfo> ccStaffList = droolsBusinessServiceImpl.selectWorkFlowCcStaffList(staffInfo.getAccount(), ActBusinessTypeEnum.hr_join_apply.getCode(), id, entity.getWorkflowId(), entity.getState());

		/**
		 * 撤销标志:0 不可撤销，1 可撤销
		 */
		String revokeTag = "0";
		if(entity!=null && entity.getCreateBy().equals(userId) && entity.getState().equals("0")){
			revokeTag = "1";
		}

		List<SysDictData> staffPostLevelList = dictDataService.selectDictDataByType(DictTypeEnum.staff_post_level.getCode());
		List<SysDictData> staffTierList = dictDataService.selectDictDataByType(DictTypeEnum.staff_tier.getCode());

		Map<String, Object> lastApprovalInfoMap  = activitiQueryService.searchLastApprovalInfo(entity.getWorkflowId());
		String postLevel = "";
		String staffTier = "0";
		String postLevelLabel = "";
		String staffTierLabel = "未评层";

		if(lastApprovalInfoMap!=null && !lastApprovalInfoMap.isEmpty()){
			postLevel = Tool.convertObject(lastApprovalInfoMap.get(ActBusinessVariablesEnum.postLevel.getCode()));
			staffTier = Tool.convertObject(lastApprovalInfoMap.get(ActBusinessVariablesEnum.staffTier.getCode()));
		}
		if(!StringUtils.isEmpty(postLevel) && staffPostLevelList!=null && staffPostLevelList.size()>0){
			for(SysDictData dict : staffPostLevelList){
				if(postLevel.equals(dict.getDictValue())){
					postLevelLabel = dict.getDictLabel();
				}
			}
		}
		if(!StringUtils.isEmpty(staffTier) && staffTierList!=null && staffTierList.size()>0){
			for(SysDictData dict : staffTierList){
				if(staffTier.equals(dict.getDictValue())){
					staffTierLabel = dict.getDictLabel();
				}
			}
		}

		return super.ok().put("entity", entity)
				.put("actHis", actHis)
				.put("ossList", ossList)
				.put("joinTips", joinTips)
				.put("candidateList", candidateList)
				.put("ccStaffList", ccStaffList)
				.put("revokeTag", revokeTag)
				.put("postLevel", postLevel)
				.put("staffTier", staffTier)
				.put("postLevelLabel", postLevelLabel)
				.put("staffTierLabel", staffTierLabel)
				.put("staffPostLevelList", staffPostLevelList)
				.put("staffTierList", staffTierList);
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

		HrJoinApply entity = hrJoinApplyService.selectEntityById(id);

		actBusinessServiceImpl.deleteProcessInstance(entity.getWorkflowId(), memo);
		hrJoinApplyService.revokeApply(id);

		return super.ok();
	}
}
