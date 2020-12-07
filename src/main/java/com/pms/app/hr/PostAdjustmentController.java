package com.pms.app.hr;

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
import com.pms.core.hr.domain.HrPostAdjustment;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrMemberJoinService;
import com.pms.core.hr.service.IHrPostAdjustmentService;
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
 * 调岗
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/hr/postAdjustment")
public class PostAdjustmentController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IHrPostAdjustmentService hrPostAdjustmentService;

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
		//List<SysDept> deptList = sysDeptService.selectLastDept();
		List<SysDept> deptList = sysDeptService.selectAll();

		HrPostAdjustment entity = new HrPostAdjustment();
		entity.setId("");
		entity.setApplyUserId(staff.getUserId());
		entity.setApplyUserName(staff.getName());
		entity.setJoinDate(staff.getJoinDate());
		entity.setJoinCompanyId(staff.getJoinCompanyId());
		entity.setJoinCompanyName(staff.getJoinCompanyName());
		entity.setJoinDeptId(staff.getJoinDeptId());
		entity.setJoinDeptName(staff.getJoinDeptName());
		entity.setJoinPostName(staff.getPostName());

		entity.setAdjustmentDate(DateUtils.conversion(DateUtils.getCurrentDay()));
		entity.setAdjustmentCompanyId(staff.getJoinCompanyId());
		entity.setAdjustmentCompanyName(staff.getJoinCompanyName());
		if(deptList!=null && deptList.size()>0){
			SysDept dept = deptList.get(0);
			entity.setAdjustmentDeptId(dept.getDeptId());
			entity.setAdjustmentDeptName(dept.getDeptName());
		} else {
			entity.setAdjustmentDeptId("");
			entity.setAdjustmentDeptName("");
		}
		entity.setAdjustmentPostName("");

		entity.setState("0");
		entity.setWorkflowId("");

		entity.setCreateBy(userId);
		entity.setCreateTime(new Date());
		//entity.setUpdateBy(userId);
		//entity.setUpdateTime(new Date());

		return super.ok().put("staff", staff).put("entity", entity).put("deptList", deptList);
	}


	/**
	 * 我的调岗列表
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

		String orderBy = "hpa.create_time desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("applyUserId", userId);


		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<HrPostAdjustment> list = hrPostAdjustmentService.selectList(params);
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

		String joinCompanyId = Tool.convertObject(parameterMap.get("joinCompanyId"));
		String joinCompanyName = Tool.convertObject(parameterMap.get("joinCompanyName"));

		String adjustmentCompanyId = Tool.convertObject(parameterMap.get("adjustmentCompanyId"));
		String adjustmentCompanyName = Tool.convertObject(parameterMap.get("adjustmentCompanyName"));

		String adjustmentDate = Tool.convertObject(parameterMap.get("adjustmentDate"));
		String adjustmentDeptId = Tool.convertObject(parameterMap.get("adjustmentDeptId"));
		String adjustmentPostName = Tool.convertObject(parameterMap.get("adjustmentPostName"));

		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

		if(StringUtils.isEmpty(joinCompanyId) || StringUtils.isEmpty(joinCompanyName)){
			return super.error("请选择原劳动关系所属公司");
		}
		if(StringUtils.isEmpty(adjustmentDate)){
			return super.error("请选择生效日期");
		}
		if(StringUtils.isEmpty(adjustmentCompanyId) || StringUtils.isEmpty(adjustmentCompanyName)){
			return super.error("请选择转入劳动合同所属公司");
		}
		if(StringUtils.isEmpty(adjustmentDeptId)){
			return super.error("请选择转入部门");
		}
		if(StringUtils.isEmpty(adjustmentPostName)){
			return super.error("请填写转入职位名称");
		}

		List<HrPostAdjustment> applyList = hrPostAdjustmentService.selectApplyingByUserId(userId);
		if(applyList!=null && applyList.size()>0){
			return super.error("您有正在处理中的调岗申请信息,不能重复申请");
		}

		StaffInfo staff = staffInfoService.selectEntityById(userId);
		SysDept dept = sysDeptService.selectDeptById(adjustmentDeptId);

		Date joinDate = staff.getJoinDate();
		String joinDeptId = staff.getJoinDeptId();
		String joinPostName = staff.getPostName();

		Date adjustmentDay = DateUtils.conversion(adjustmentDate);

		if(DateUtils.dateSubtraction(joinDate, adjustmentDay)<0){
			return super.error("调岗日期不能晚于入职日期");
		}

		/**
		 * ID
		 */
		String id = SnowflakeIdUtil.getId();

		HrPostAdjustment entity = new HrPostAdjustment();
		entity.setId(id);
		entity.setApplyUserId(userId);
		entity.setJoinDate(joinDate);
		entity.setJoinCompanyId(joinCompanyId);
		entity.setJoinCompanyName(joinCompanyName);
		entity.setJoinDeptId(joinDeptId);
		entity.setJoinPostName(joinPostName);

		entity.setAdjustmentDate(adjustmentDay);
		entity.setAdjustmentCompanyId(adjustmentCompanyId);
		entity.setAdjustmentCompanyName(adjustmentCompanyName);
		entity.setAdjustmentDeptId(adjustmentDeptId);
		entity.setAdjustmentPostName(adjustmentPostName);

		entity.setState("0");
		entity.setWorkflowId("");

		entity.setCreateBy(userId);
		entity.setCreateTime(new Date());
		//entity.setUpdateBy(userId);
		//entity.setUpdateTime(new Date());
		hrPostAdjustmentService.insertEntity(entity);

		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<WorkflowOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				WorkflowOss wo = new WorkflowOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setBusinessKey(ActBusinessTypeEnum.hr_post_adjustment.getCode());
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
		String businessMemo = staff.getName()+"申请调岗,原部门:"+staff.getJoinDeptName()+",原岗位:"+staff.getPostName()+",新部门:"+dept.getDeptName()+",新岗位:"+adjustmentPostName;
		ProcessInstance pi = actBusinessServiceImpl.startProcess(staff.getAccount(), staff.getName(), ActBusinessTypeEnum.hr_post_adjustment, id, businessMemo, otherParam);

		/**
		 * 关联表单和工作流
		 */
		hrPostAdjustmentService.updateWorkflowId(pi.getId(), id);

		/**
		 * 保存抄送信息
		 */
		if(StringUtils.isNotEmpty(ccAccounts)){
			List<WorkflowCc> ccDatas = new ArrayList<>();
			String[] ccArray = ccAccounts.split(",");
			for(String ccAccount : ccArray){
				WorkflowCc cc = new WorkflowCc();
				cc.setId(SnowflakeIdUtil.getId());
				cc.setBusinessKey(ActBusinessTypeEnum.hr_post_adjustment.getCode());
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
		String postAdjustmentId = Tool.convertObject(parameterMap.get("postAdjustmentId"));

		if(StringUtils.isEmpty(userId)){
			return super.error("userId参数不能为空");
		}
		if(StringUtils.isEmpty(postAdjustmentId)){
			return super.error("ID参数不能为空");
		}

		HrPostAdjustment entity = hrPostAdjustmentService.selectEntityById(postAdjustmentId);
		if(entity==null){
			return super.error("未查询到申请单");
		}
		StaffInfo staffInfo = staffInfoService.selectEntityById(entity.getCreateBy());

		List<Map<String, String>> actHis = activitiQueryService.selectHistoryFlowList(entity.getWorkflowId());
		List<WorkflowOss> ossList = workflowOssService.selectListByBusiness(ActBusinessTypeEnum.hr_post_adjustment.getCode(), postAdjustmentId);

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
		List<StaffInfo> ccStaffList = droolsBusinessServiceImpl.selectWorkFlowCcStaffList(staffInfo.getAccount(), ActBusinessTypeEnum.hr_post_adjustment.getCode(), postAdjustmentId, entity.getWorkflowId(), entity.getState());

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

		HrPostAdjustment entity = hrPostAdjustmentService.selectEntityById(id);

		actBusinessServiceImpl.deleteProcessInstance(entity.getWorkflowId(), memo);
		hrPostAdjustmentService.revokeApply(id);

		return super.ok();
	}
}
