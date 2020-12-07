package com.pms.app.adm;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pms.activiti.config.ActBusinessTypeEnum;
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
import com.pms.core.adm.domain.AdmSealApply;
import com.pms.core.adm.service.IAdmSealApplyService;
import com.pms.core.hr.domain.HrLeaveApply;
import com.pms.core.hr.domain.HrLeaveHandover;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrLeaveApplyService;
import com.pms.core.hr.service.IHrLeaveHandoverService;
import com.pms.core.hr.service.IHrMemberJoinService;
import com.pms.core.hr.service.IStaffInfoService;
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
 * 印章申请
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/adm/sealApply")
public class AdmSealController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IAdmSealApplyService admSealApplyService;

	@Autowired
	private IHrLeaveApplyService hrLeaveApplyService;

	@Autowired
	private IHrLeaveHandoverService hrLeaveHandoverService;

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

		AdmSealApply entity = new AdmSealApply();
		entity.setId("");
		entity.setApplyUserId(staff.getUserId());
		entity.setApplyUserName(staff.getName());

		entity.setUseUserId(staff.getUserId());
		entity.setUseUserName(staff.getName());

		entity.setApplyDate(new Date());

		entity.setBorrowDate(new Date());
		entity.setRevertDate(new Date());
		entity.setFileCategory("0");
		entity.setFileName("");
		entity.setFileQuantity(1);
		entity.setSealCategory("0");
		entity.setSealUseType("0");

		entity.setMemo("");
		entity.setState("0");
		entity.setWorkflowId("");

		return super.ok().put("entity", entity).put("staff", staff);
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

		String orderBy = "asa.create_time desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("applyUserId", userId);

		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<AdmSealApply> list = admSealApplyService.selectList(params);
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

		String useUserId = Tool.convertObject(parameterMap.get("useUserId"));
		String applyDate = Tool.convertObject(parameterMap.get("applyDate"));
		if(StringUtils.isEmpty(applyDate)){
			applyDate = DateUtils.getCurrentDay();
		}
		String borrowDate = Tool.convertObject(parameterMap.get("borrowDate"));
		String revertDate = Tool.convertObject(parameterMap.get("revertDate"));
		String fileCategory = Tool.convertObject(parameterMap.get("fileCategory"));
		String fileName = Tool.convertObject(parameterMap.get("fileName"));
		Integer fileQuantity = Tool.convertStringToInt(parameterMap.get("fileQuantity"));
		String sealCategory = Tool.convertObject(parameterMap.get("sealCategory"));
		String sealUseType = Tool.convertObject(parameterMap.get("sealUseType"));
		String memo = Tool.convertObject(parameterMap.get("memo"));

		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

		if(StringUtils.isEmpty(useUserId)){
			return super.error("请选择印章使用人");
		}
		if(StringUtils.isEmpty(applyDate)){
			return super.error("请选择申请日期");
		}
		if(StringUtils.isEmpty(fileCategory)){
			return super.error("请选择用印文件类别");
		}
		if(StringUtils.isEmpty(fileName)){
			return super.error("请填写用印文件名称");
		}
		if(StringUtils.isEmpty(sealCategory)){
			return super.error("请选择印章类别");
		}
		if(StringUtils.isEmpty(sealUseType)){
			return super.error("请选择用印类别");
		}
		if("1".equals(sealUseType)){
			if(StringUtils.isEmpty(borrowDate)){
				return super.error("请选择借用日期");
			}
			if(StringUtils.isEmpty(revertDate)){
				return super.error("请选择归还日期");
			}
		}

		List<HrLeaveApply> leaveApplyList = hrLeaveApplyService.selectApplyingByUserId(userId);
		if(leaveApplyList!=null && leaveApplyList.size()>0){
			return super.error("您有正在处理中的离职申请信息,不能申请印章");
		}
		/*if("1".equals(sealUseType)){
			List<AdmSealApply> applyingList = admSealApplyService.selectApplyingByBorrowDate(sealCategory, borrowDate);
			if(applyingList!=null && applyingList.size()>0){
				return super.error("您申请要外带的印章已经借出");
			}
		}*/

		/**
		 * 如果是人事离职类文件的印章是很轻
		 * 要判断印章使用人的离职信息
		 */
		if(fileCategory.equals("2")){
			HrLeaveHandover useLeaveHandover = hrLeaveHandoverService.selectLastEntityByUserId(useUserId);
			if(useLeaveHandover==null){
				return super.error("印章使用人没有离职交接信息，不能申请人事(离职)类文件的用章.");
			}
			if(!useLeaveHandover.getState().equals("2")){
				return super.error("印章使用人没有已经审批通过的离职交接信息，不能申请人事(离职)类文件的用章.");
			}
		}




		Date applyDay = DateUtils.conversion(applyDate);
		Date borrowDay = null;
		Date revertDay = null;
		if("1".equals(sealUseType)){
			borrowDay = DateUtils.conversion(borrowDate);
			revertDay = DateUtils.conversion(revertDate);
		}

		/**
		 * ID
		 */
		String id = SnowflakeIdUtil.getId();

		AdmSealApply entity = new AdmSealApply();
		entity.setId(id);
		entity.setApplyUserId(userId);
		entity.setApplyDate(applyDay);

		entity.setUseUserId(useUserId);

		entity.setBorrowDate(borrowDay);
		entity.setRevertDate(revertDay);
		entity.setFileCategory(fileCategory);
		entity.setFileName(fileName);
		entity.setFileQuantity(fileQuantity);
		entity.setSealCategory(sealCategory);
		entity.setSealUseType(sealUseType);
		entity.setMemo(memo);

		entity.setState("0");
		entity.setWorkflowId("");

		entity.setCreateBy(userId);
		entity.setCreateTime(new Date());
		//entity.setUpdateBy(userId);
		//entity.setUpdateTime(new Date());
		admSealApplyService.insertEntity(entity);

		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<WorkflowOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				WorkflowOss wo = new WorkflowOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setBusinessKey(ActBusinessTypeEnum.adm_seal_apply.getCode());
				wo.setBusinessId(id);
				wo.setOssId(ossId);
				ossDatas.add(wo);
			}
			if(ossDatas!=null && ossDatas.size()>0){
				workflowOssService.batchInsertEntity(ossDatas);
			}
		}

		String sealCategoryLabel = "公章";
		if("1".equals(sealCategory)){
			sealCategoryLabel = "合同章";
		} else if("2".equals(sealCategory)){
			sealCategoryLabel = "法人章";
		}
		String sealUseTypeLabel = "盖章";
		if("1".equals(sealUseType)){
			sealUseTypeLabel = "外带";
		}

		StaffInfo staff = staffInfoService.selectEntityById(userId); //申请人
		StaffInfo useStaff = staffInfoService.selectByUserId(useUserId);//使用人

		/**
		 * 开始审批进程
		 */
		Map<String, Object> otherParam = new HashMap<>();
		String businessMemo = staff.getName()+"为"+useStaff.getName()+"申请"+sealCategoryLabel+sealUseTypeLabel+",用印文件："+fileName+",文件份数:"+fileQuantity;
		ProcessInstance pi = actBusinessServiceImpl.startProcess(staff.getAccount(), staff.getName(), ActBusinessTypeEnum.adm_seal_apply, id, businessMemo, otherParam);

		/**
		 * 关联表单和工作流
		 */
		admSealApplyService.updateWorkflowId(pi.getId(), id);

		/**
		 * 保存抄送信息
		 */
		//如果申请人和使用人不是同一人，并且使用人不在主动抄送的人员内，则要抄送使用人
		if(!userId.equals(useUserId) && !ccAccounts.contains(useStaff.getAccount())){
			ccAccounts+=","+useStaff.getAccount();
		}
		if(StringUtils.isNotEmpty(ccAccounts)){
			List<WorkflowCc> ccDatas = new ArrayList<>();
			String[] ccArray = ccAccounts.split(",");
			for(String ccAccount : ccArray){
				if(StringUtils.isNotEmpty(ccAccount)){
					WorkflowCc cc = new WorkflowCc();
					cc.setId(SnowflakeIdUtil.getId());
					cc.setBusinessKey(ActBusinessTypeEnum.adm_seal_apply.getCode());
					cc.setBusinessId(id);
					cc.setBusinessTitle(businessMemo);
					cc.setBusinessAccount(staff.getAccount());
					cc.setCcAccount(ccAccount);
					cc.setCreateTime(new Date());
					cc.setState(0);
					ccDatas.add(cc);
				}
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
	 * 明细
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

		AdmSealApply entity = admSealApplyService.selectEntityById(id);
		if(entity==null){
			return super.error("未查询到申请单");
		}

		/**
		 * 使用人
		 */
		StaffInfo useStaffInfo = staffInfoService.selectEntityById(entity.getUseUserId());

		/**
		 * 申请人
		 */
		StaffInfo applyStaffInfo = staffInfoService.selectEntityById(entity.getCreateBy());

		List<Map<String, String>> actHis = activitiQueryService.selectHistoryFlowList(entity.getWorkflowId());
		List<WorkflowOss> ossList = workflowOssService.selectListByBusiness(ActBusinessTypeEnum.adm_seal_apply.getCode(), id);

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
		List<StaffInfo> ccStaffList = droolsBusinessServiceImpl.selectWorkFlowCcStaffList(applyStaffInfo.getAccount(), ActBusinessTypeEnum.adm_seal_apply.getCode(), id, entity.getWorkflowId(), entity.getState());

		/**
		 * 撤销标志:0 不可撤销，1 可撤销
		 */
		String revokeTag = "0";
		if(entity!=null && entity.getCreateBy().equals(userId) && entity.getState().equals("0")){
			revokeTag = "1";
		}

		return super.ok().put("entity", entity).put("useStaffInfo", useStaffInfo).put("actHis", actHis).put("ossList", ossList).put("candidateList", candidateList).put("revokeTag", revokeTag).put("ccStaffList", ccStaffList);
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

		AdmSealApply entity = admSealApplyService.selectEntityById(id);

		actBusinessServiceImpl.deleteProcessInstance(entity.getWorkflowId(), memo);
		admSealApplyService.revokeApply(id);

		return super.ok();
	}
}
