package com.pms.app.adm;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pms.activiti.config.ActBusinessTypeEnum;
import com.pms.activiti.service.impl.ActBusinessServiceImpl;
import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.config.PlatConfigUtils;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.enums.McSysCodeEnum;
import com.pms.common.service.QytCompanyInfoService;
import com.pms.common.service.QytSsoLoginService;
import com.pms.common.service.SysPushService;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.common.utils.qr.QRCodeGenerator;
import com.pms.core.activiti.service.IActivitiQueryService;
import com.pms.core.adm.domain.AdmCardApply;
import com.pms.core.adm.service.IAdmCardApplyService;
import com.pms.core.hr.domain.HrLeaveApply;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrLeaveApplyService;
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
import java.io.File;
import java.util.*;

/**
 * 名片申请
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/adm/cardApply")
public class AdmCardController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IAdmCardApplyService admCardApplyService;

	@Autowired
	private IHrLeaveApplyService hrLeaveApplyService;

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

	@Autowired
	private PlatConfigUtils platConfigUtils;

	@Autowired
	private QytCompanyInfoService qytCompanyInfoService;

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

		AdmCardApply entity = new AdmCardApply();
		entity.setId("");
		entity.setApplyUserId(staff.getUserId());
		entity.setApplyUserName(staff.getName());
		entity.setApplyDate(DateUtils.conversion(DateUtils.getCurrentDay()));

		entity.setCardNameCn(staff.getName());
		entity.setCardNameEn(staff.getAccount());
		entity.setPhone(staff.getPhonenumber());
		entity.setEmail(staff.getEmail());
		entity.setDeptName(staff.getJoinDeptName());
		entity.setDeptNameEn("");
		entity.setPostName(staff.getPostName());
		entity.setPostNameEn("");
		entity.setPrintPeriod("2");
		entity.setPrintQuantity(1);
		entity.setOpenTag("1");
		entity.setQrCodeUrl("");

		entity.setMemo("");
		entity.setState("0");


		entity.setWorkflowId("");

		return super.ok().put("entity", entity).put("staff", staff);
	}


	/**
	 * 我的名片列表
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

		String state = Tool.convertObject(parameterMap.get("state")); //状态:0：审核中，1：驳回，2：确认, 3 撤销

		int pageNum = Tool.convertStringToInt(parameterMap.get("pageNum"));
		if(pageNum<=0){
			pageNum = 1;
		}
		int pageSize = Tool.convertStringToInt(parameterMap.get("pageSize"));
		if(pageSize<=0 || pageSize>50){
			pageSize = 10;
		}

		String orderBy = "aca.create_time desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("applyUserId", userId);
		params.put("state", state);

		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<AdmCardApply> list = admCardApplyService.selectList(params);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());

		String cardUrlPrefix = platConfigUtils.getOssUrl().replaceAll("resource", "")+"adm/cardInfo.html?id=";

		return super.ok().put("gridDatas", list).put("webPage", page).put("cardUrlPrefix", cardUrlPrefix);
	}

	/**
	 * 员工名片列表
	 * @return
	 */
	@RequestMapping("/staffCardList")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult staffCardList(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String pmsUserId = getAppUserId(parameterMap);
		String userId = Tool.convertObject(parameterMap.get("userId"));

		String state = "2"; //状态:0：审核中，1：驳回，2：确认, 3 撤销
		String openTag = "0"; //是否公开，0 公开，1 不公开
		if(pmsUserId.equals(userId)){ //说明要查看的是自己的，不需要设置openTag参数值（即查看全部）
			openTag = "";
		}

		int pageNum = Tool.convertStringToInt(parameterMap.get("pageNum"));
		if(pageNum<=0){
			pageNum = 1;
		}
		int pageSize = Tool.convertStringToInt(parameterMap.get("pageSize"));
		if(pageSize<=0 || pageSize>50){
			pageSize = 10;
		}

		String orderBy = "aca.create_time desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("applyUserId", userId);
		params.put("state", state);
		params.put("openTag", openTag);

		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<AdmCardApply> list = admCardApplyService.selectList(params);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());

		String cardUrlPrefix = platConfigUtils.getOssUrl().replaceAll("resource", "")+"adm/cardInfo.html?id=";

		return super.ok().put("gridDatas", list).put("webPage", page).put("cardUrlPrefix", cardUrlPrefix);
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

		String applyDate = Tool.convertObject(parameterMap.get("applyDate"));
		if(StringUtils.isEmpty(applyDate)){
			applyDate = DateUtils.getCurrentDay();
		}

		String cardNameCn = Tool.convertObject(parameterMap.get("cardNameCn"));
		String cardNameEn = Tool.convertObject(parameterMap.get("cardNameEn"));
		String phone = Tool.convertObject(parameterMap.get("phone"));
		String email = Tool.convertObject(parameterMap.get("email"));
		String deptName = Tool.convertObject(parameterMap.get("deptName"));
		String deptNameEn = Tool.convertObject(parameterMap.get("deptNameEn"));
		String postName = Tool.convertObject(parameterMap.get("postName"));
		String postNameEn = Tool.convertObject(parameterMap.get("postNameEn"));
		String printPeriod = Tool.convertObject(parameterMap.get("printPeriod"));
		Integer printQuantity = Tool.convertStringToInt(parameterMap.get("printQuantity"));
		String qrCodeUrl = Tool.convertObject(parameterMap.get("qrCodeUrl"));
		String openTag = Tool.convertObject(parameterMap.get("openTag"));
		if(StringUtils.isEmpty(openTag)){
			openTag = "0";
		}
		String memo = Tool.convertObject(parameterMap.get("memo"));


		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

		if(StringUtils.isEmpty(applyDate)){
			return super.error("请选择申请日期");
		}
		if(StringUtils.isEmpty(cardNameCn)){
			return super.error("请填写名片姓名");
		}
		/*if(StringUtils.isEmpty(cardNameEn)){
			return super.error("请填写名片英文姓名");
		}*/
		if(StringUtils.isEmpty(phone)){
			return super.error("请填写电话");
		}
		if(StringUtils.isEmpty(email)){
			return super.error("请填写email");
		}
		/*if(StringUtils.isEmpty(deptName)){
			return super.error("请填写部门名称");
		}
		if(StringUtils.isEmpty(deptNameEn)){
			return super.error("请填写部门英文名称");
		}
		if(StringUtils.isEmpty(postName)){
			return super.error("请填写职位名称");
		}
		if(StringUtils.isEmpty(postNameEn)){
			return super.error("请填写职位英文名称");
		}*/

		if(printPeriod.equals("2")){
			if(StringUtils.isEmpty(qrCodeUrl)){
				return super.error("您正在申请电子名片，请上传个人微信二维码");
			}
		}

		List<AdmCardApply> applyList = admCardApplyService.selectApplyingByUserId(userId);
		if(applyList!=null && applyList.size()>0){
			return super.error("您有正在处理中的名片印制申请信息,不能重复申请");
		}

		List<HrLeaveApply> leaveApplyList = hrLeaveApplyService.selectApplyingByUserId(userId);
		if(leaveApplyList!=null && leaveApplyList.size()>0){
			return super.error("您有正在处理中的离职申请信息,不能申请名片印制");
		}

		StaffInfo staff = staffInfoService.selectEntityById(userId);

		Date applyDay = DateUtils.conversion(applyDate);

		/**
		 * ID
		 */
		String id = SnowflakeIdUtil.getId();

		AdmCardApply entity = new AdmCardApply();
		entity.setId(id);
		entity.setApplyUserId(userId);
		entity.setApplyDate(applyDay);

		entity.setCardNameCn(cardNameCn);
		entity.setCardNameEn(cardNameEn);
		entity.setPhone(phone);
		entity.setEmail(email);
		entity.setDeptName(deptName);
		entity.setDeptNameEn(deptNameEn);
		entity.setPostName(postName);
		entity.setPostNameEn(postNameEn);
		entity.setPrintPeriod(printPeriod);
		entity.setPrintQuantity(printQuantity);
		entity.setMemo(memo);

		entity.setOpenTag(openTag);
		entity.setQrCodeUrl(qrCodeUrl);

		entity.setState("0");
		entity.setWorkflowId("");

		entity.setCreateBy(userId);
		entity.setCreateTime(new Date());
		//entity.setUpdateBy(userId);
		//entity.setUpdateTime(new Date());
		admCardApplyService.insertEntity(entity);

		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<WorkflowOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				WorkflowOss wo = new WorkflowOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setBusinessKey(ActBusinessTypeEnum.adm_card_apply.getCode());
				wo.setBusinessId(id);
				wo.setOssId(ossId);
				ossDatas.add(wo);
			}
			if(ossDatas!=null && ossDatas.size()>0){
				workflowOssService.batchInsertEntity(ossDatas);
			}
		}

		String printPeriodLabel = "";
		if(printPeriod.equals("0")){
			printPeriodLabel = "印制(4-6天)";
		} else if(printPeriod.equals("1")){
			printPeriodLabel = "加急(1-2天)";
		} else if(printPeriod.equals("2")){
			printPeriodLabel = "电子名片(无需印制)";
		}
		/**
		 * 开始审批进程
		 */
		Map<String, Object> otherParam = new HashMap<>();
		String businessMemo = staff.getName()+"申请印制名片,申请类型:"+printPeriodLabel;
		ProcessInstance pi = actBusinessServiceImpl.startProcess(staff.getAccount(), staff.getName(), ActBusinessTypeEnum.adm_card_apply, id, businessMemo, otherParam);

		/**
		 * 关联表单和工作流
		 */
		admCardApplyService.updateWorkflowId(pi.getId(), id);

		/**
		 * 保存抄送信息
		 */
		if(StringUtils.isNotEmpty(ccAccounts)){
			List<WorkflowCc> ccDatas = new ArrayList<>();
			String[] ccArray = ccAccounts.split(",");
			for(String ccAccount : ccArray){
				WorkflowCc cc = new WorkflowCc();
				cc.setId(SnowflakeIdUtil.getId());
				cc.setBusinessKey(ActBusinessTypeEnum.adm_card_apply.getCode());
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
	 * 查看名片明细
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


		AdmCardApply entity = admCardApplyService.selectEntityById(id);
		if(entity==null){
			return super.error("未查询到申请单");
		}
		StaffInfo staffInfo = staffInfoService.selectEntityById(entity.getCreateBy());

		List<Map<String, String>> actHis = activitiQueryService.selectHistoryFlowList(entity.getWorkflowId());
		List<WorkflowOss> ossList = workflowOssService.selectListByBusiness(ActBusinessTypeEnum.adm_card_apply.getCode(), id);

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
		List<StaffInfo> ccStaffList = droolsBusinessServiceImpl.selectWorkFlowCcStaffList(staffInfo.getAccount(), ActBusinessTypeEnum.adm_card_apply.getCode(), id, entity.getWorkflowId(), entity.getState());

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

		AdmCardApply entity = admCardApplyService.selectEntityById(id);

		actBusinessServiceImpl.deleteProcessInstance(entity.getWorkflowId(), memo);
		admCardApplyService.revokeApply(id);

		return super.ok();
	}



	/**
	 * 查看名片明细
	 * @return
	 */
	@RequestMapping("/myCard")
	@ResponseBody
	@GatewayAuth(GatewayConstants.un_require_login)
	public AjaxResult myCard(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);

		String id = Tool.convertObject(parameterMap.get("id"));

		if(StringUtils.isEmpty(id)){
			return super.error("ID参数不能为空");
		}


		AdmCardApply entity = admCardApplyService.selectEntityById(id);
		if(entity==null){
			return super.error("未查询到名片申请单");
		}

		SysDept companyInfo = sysDeptService.selectCompanyEntity();
		if(companyInfo==null){
			return super.error("未能获取企业信息");
		}

		String companyId = companyInfo.getDeptId();

		/**
		 * 向企易通平台请求企业信息
		 */
		Object platCompany = null;
		AjaxResult AR = qytCompanyInfoService.companyInfo(companyId);
		String resCode = Tool.convertObject(AR.get("code"));
		if(StringUtils.isNotEmpty(resCode) && resCode.equals("0")){
			platCompany = AR.get("platCompany");
		}

		return super.ok().put("entity", entity).put("platCompany", platCompany);
	}

}
