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
import com.pms.core.hr.domain.HrBoleBonus;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrBoleBonusService;
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
 * 婚育津贴
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/hr/bole")
public class BoleBonusController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IHrBoleBonusService hrBoleBonusService;

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

		HrBoleBonus entity = new HrBoleBonus();
		entity.setId("");
		entity.setApplyUserId(userId);
		entity.setApplyUserName(staff.getName());
		//entity.setProId(proId);
		entity.setApplyDate(DateUtils.conversion(DateUtils.getCurrentDay()));
		entity.setGrantType("2");
		entity.setJoinUserId("");
		entity.setJoinUserName("");
		entity.setPostName("");
		entity.setPostRank("");
		entity.setPositiveDate(null);
		entity.setBonus(1000d);
		entity.setMemo("");
		entity.setState("0");
		entity.setWorkflowId("");
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
	 * 我的伯乐奖金列表
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

		String orderBy = "hbb.create_time desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("applyUserId", userId);


		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<HrBoleBonus> list = hrBoleBonusService.selectList(params);
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

		String joinUserId = Tool.convertObject(parameterMap.get("joinUserId"));

		String proId = Tool.convertObject(parameterMap.get("proId"));
		String grantType = Tool.convertObject(parameterMap.get("grantType"));
		String memo = Tool.convertObject(parameterMap.get("memo"));

		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

		if(StringUtils.isEmpty(joinUserId)){
			return super.error("请选择您要推荐的职员");
		}
		if(StringUtils.isEmpty(proId)){
			return super.error("请选择所在项目");
		}
		if(StringUtils.isEmpty(grantType)){
			return super.error("请选择发放方式");
		}

		StaffInfo joinStaff = staffInfoService.selectEntityById(joinUserId);
		String joinUserName = joinStaff.getName();
		String postName = joinStaff.getPostName();
		//String postRank = joinStaff.getPostRank();
		String postLevel = joinStaff.getPostLevel();
		Date positiveDate = joinStaff.getPositiveDate();
		if(StringUtils.isEmpty(joinUserName)){
			return super.error("您要推荐的职员未填写姓名.");
		}
		if(StringUtils.isEmpty(postName)){
			return super.error("您要推荐的职员未填写职位.");
		}
		if(positiveDate==null){
			return super.error("您要推荐的职员未填写转正日期或转正申请为办理完成.");
		}
		Double bonus = 800d;
		if(postLevel.indexOf("VP")!=-1 || postLevel.indexOf("EX")!=-1 || postLevel.indexOf("SE")!=-1 || postLevel.indexOf("HL")!=-1){
			bonus = 2000d;
		} else if(postLevel.indexOf("ML")!=-1){
			bonus = 1500d;
		}

		List<HrBoleBonus> recList = hrBoleBonusService.selectRecommendByUserId(joinUserId);
		if(recList!=null && recList.size()>0){
			return super.error("您要推荐的职员有正在审批中或已被推荐的信息,不能重复申请");
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

		HrBoleBonus entity = new HrBoleBonus();
		entity.setId(id);
		entity.setApplyUserId(userId);
		entity.setApplyUserName(staff.getName());
		entity.setProId(proId);
		entity.setApplyDate(new Date());
		entity.setGrantType(grantType);
		entity.setJoinUserId(joinUserId);
		entity.setJoinUserName(joinUserName);
		entity.setPostName(postName);
		entity.setPostRank("");
		entity.setPositiveDate(positiveDate);
		entity.setBonus(bonus);
		entity.setMemo(memo);

		entity.setState("0");
		entity.setWorkflowId("");

		entity.setCreateUserId(userId);
		entity.setCreateTime(new Date());
		//entity.setUpdateUserId((userId);
		//entity.setUpdateTime(new Date());
		hrBoleBonusService.insertEntity(entity);

		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<WorkflowOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				WorkflowOss wo = new WorkflowOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setBusinessKey(ActBusinessTypeEnum.hr_bole_bonus.getCode());
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

		String businessMemo = staff.getName()+"申请伯乐奖金,奖金:"+bonus+",被推荐人姓名:"+joinUserName+",职位:"+postName+",转正日期:"+DateUtils.format(positiveDate);
		ProcessInstance pi = actBusinessServiceImpl.startProcess(staff.getAccount(), staff.getName(), ActBusinessTypeEnum.hr_bole_bonus, id, businessMemo, otherParam);

		/**
		 * 关联表单和工作流
		 */
		hrBoleBonusService.updateWorkflowId(pi.getId(), id);

		/**
		 * 保存抄送信息
		 */
		if(StringUtils.isNotEmpty(ccAccounts)){
			List<WorkflowCc> ccDatas = new ArrayList<>();
			String[] ccArray = ccAccounts.split(",");
			for(String ccAccount : ccArray){
				WorkflowCc cc = new WorkflowCc();
				cc.setId(SnowflakeIdUtil.getId());
				cc.setBusinessKey(ActBusinessTypeEnum.hr_bole_bonus.getCode());
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
	 * 查看伯乐奖金明细
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

		HrBoleBonus entity = hrBoleBonusService.selectEntityById(id);
		if(entity==null){
			return super.error("未查询到申请单");
		}
		StaffInfo staffInfo = staffInfoService.selectEntityById(entity.getCreateUserId());

		List<Map<String, String>> actHis = activitiQueryService.selectHistoryFlowList(entity.getWorkflowId());
		List<WorkflowOss> ossList = workflowOssService.selectListByBusiness(ActBusinessTypeEnum.hr_bole_bonus.getCode(), id);

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
		List<StaffInfo> ccStaffList = droolsBusinessServiceImpl.selectWorkFlowCcStaffList(staffInfo.getAccount(), ActBusinessTypeEnum.hr_bole_bonus.getCode(), id, entity.getWorkflowId(), entity.getState());

		/**
		 * 撤销标志:0 不可撤销，1 可撤销
		 */
		String revokeTag = "0";
		if(entity!=null && entity.getCreateUserId().equals(userId) && entity.getState().equals("0")){
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

		HrBoleBonus entity = hrBoleBonusService.selectEntityById(id);

		actBusinessServiceImpl.deleteProcessInstance(entity.getWorkflowId(), memo);
		hrBoleBonusService.revokeApply(id);

		return super.ok();
	}
}
