package com.pms.app.hr;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.constant.UserConstants;
import com.pms.common.enums.DictTypeEnum;
import com.pms.common.enums.McSysCodeEnum;
import com.pms.common.service.QytJoinCompanyCallbackService;
import com.pms.common.service.SysMailService;
import com.pms.common.service.SysPushService;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.RegExpValidatorUtils;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.hr.domain.HrMemberJoin;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrMemberJoinService;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.prefer.domain.PreferAppRole;
import com.pms.core.prefer.domain.PreferAppStaffRole;
import com.pms.core.prefer.service.IPreferAppRoleService;
import com.pms.core.prefer.service.IPreferAppStaffRoleService;
import com.pms.core.system.domain.*;
import com.pms.core.system.service.ISysDeptService;
import com.pms.core.system.service.ISysDictDataService;
import com.pms.core.system.service.ISysRoleService;
import com.pms.core.system.service.ISysUserService;
import com.pms.framework.web.base.AppBaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 接收企易通平台发送的会员入职申请
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/hr/memberJoin")
public class MemberJoinApplyController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IHrMemberJoinService hrMemberJoinService;

	@Autowired
	private ISysUserService sysUserService;

	@Autowired
	private ISysDeptService sysDeptService;

	@Autowired
	private ISysRoleService sysRoleService;

	@Autowired
	private IStaffInfoService staffInfoService;


	@Autowired
	private QytJoinCompanyCallbackService qytJoinCompanyCallbackService;

	@Autowired
	private IPreferAppStaffRoleService preferAppStaffRoleService;

	@Autowired
	private IPreferAppRoleService preferAppRoleService;

	@Autowired
	private ISysDictDataService sysDictDataService;

	@Autowired
	private SysPushService sysPushService;
	@Autowired
	private SysMailService sysMailService;

	/**
	 * 接收企易通平台发送的会员入职申请
	 * @return
	 */
	@RequestMapping("/apply")
	@ResponseBody
	@GatewayAuth(GatewayConstants.un_require_login)
	public AjaxResult apply(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);

		String memberId = Tool.convertObject(parameterMap.get("memberId"));
		String memberName = Tool.convertObject(parameterMap.get("memberName"));
		String mobile = Tool.convertObject(parameterMap.get("mobile"));
		String applyReason = Tool.convertObject(parameterMap.get("applyReason"));


		if(StringUtils.isEmpty(memberId)){
			return super.error("会员ID不能为空");
		}
		if(StringUtils.isEmpty(memberName)){
			return super.error("会员姓名不能为空");
		}
		if(StringUtils.isEmpty(mobile)){
			return super.error("会员手机号不能为空");
		}
		if(StringUtils.isEmpty(applyReason)){
			return super.error("申请理由不能为空");
		}

		HrMemberJoin applyRecord = hrMemberJoinService.selectLastApplyRecordByMemberId(memberId);
		if (applyRecord != null) {
			if(applyRecord.getState()==0){
				return super.error("您的申请还在审核中,请勿重复申请").put("state", applyRecord.getState());
			} else if(applyRecord.getState()==1){
				return super.error("您已经加入了该企业,请勿重复申请").put("state", applyRecord.getState());
			}
		}

		SysDept companyInfo = sysDeptService.selectCompanyEntity();

		String userId = SnowflakeIdUtil.getId();

		HrMemberJoin entity = new HrMemberJoin();
		entity.setUserId(userId);
		entity.setMemberId(memberId);
		entity.setMobile(mobile);
		entity.setState(0);
		entity.setMemberName(memberName);
		entity.setApplyReason(applyReason);
		entity.setRejectReason("");
		entity.setCreateTime(new Date());

		hrMemberJoinService.insertEntity(entity);

		/**
		 * 给系统运维人员(数据字典sys_devops)发送邮件提醒开通邮箱
		 */
		List<String> devopsList = new ArrayList<String>();
		List<SysDictData> dictList = sysDictDataService.selectDictDataByType(DictTypeEnum.sys_devops.getCode());
		if(dictList!=null && dictList.size()>0){
			for(SysDictData sdd : dictList){
				devopsList.add(sdd.getDictValue());
			}
		}
		List<StaffInfo> devOpsStaffList = null;
		if(devopsList!=null && devopsList.size()>0){
			devOpsStaffList = staffInfoService.selectListByAccounts(devopsList.toArray(new String[devopsList.size()]));
		}
		if(devOpsStaffList!=null && devOpsStaffList.size()>0){
			String subject = "加入公司申请";
			String content = "会员"+memberName+"，手机号:"+mobile+"，申请加入公司，理由是:"+applyReason+"，请及时处理该会员的申请。";

			for(StaffInfo si : devOpsStaffList){
				sysMailService.sendMail(McSysCodeEnum.pmsAdmin.getCode(), companyInfo.getDeptId(), si.getEmail(), subject, content);
			}
		}

		return super.ok().put("state", entity.getState());
	}


	/**
	 * 待审批记录列表
	 * @return
	 */
	@RequestMapping("/waitApprovalList")
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

		String orderBy = " create_time desc ";

		Map<String, Object> params = new HashMap<>();
		params.put("applyUserId", userId);


		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<HrMemberJoin> list = hrMemberJoinService.selectWaitApprovalList(params);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());

		return super.ok().put("gridDatas", list).put("webPage", page);
	}


	/**
	 * 待审批记录列表
	 * @return
	 */
	@RequestMapping("/approvalInit")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult approvalInit(HttpServletRequest request, HttpServletResponse response)
	{

		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String appUserId = getAppUserId(parameterMap);

		String id = Tool.convertObject(parameterMap.get("id")); //待审批记录ID

		HrMemberJoin entity = hrMemberJoinService.selectEntityByUserId(id);
		if(entity==null){
			return super.error("未能检索到申请详情");
		}
		if(entity.getState()!=0){
			return super.error("该会员的加入申请已经审核过,请勿重复审核");
		}

		StaffInfo staffInfo = staffInfoService.selectEntityById(entity.getUserId());
		if(staffInfo!=null){
			entity.setState(1);

			entity.setAccount(staffInfo.getAccount());
			entity.setCode(staffInfo.getCode());
			entity.setUserName(staffInfo.getName());
			entity.setEmail(staffInfo.getEmail());
			entity.setJoinCompanyId(staffInfo.getJoinCompanyId());
			entity.setDeptId(staffInfo.getJoinDeptId());
			entity.setDeptName(staffInfo.getJoinDeptName());
			entity.setJoinAddr(staffInfo.getJoinAddr());
			entity.setJoinDate(DateUtils.format(staffInfo.getJoinDate()));
			entity.setPositiveDate(DateUtils.format(staffInfo.getPositiveDate()));
			entity.setProbationPeriod(staffInfo.getProbationPeriod());
			entity.setPostName(staffInfo.getPostName());
			entity.setPostCategory(staffInfo.getPostCategory());
			entity.setPostLevel(staffInfo.getPostLevel());
			entity.setStaffTier(staffInfo.getStaffTier());
			entity.setContractType(staffInfo.getContractType());
			entity.setContractBeginDate(DateUtils.format(staffInfo.getContractBeginDate()));
			entity.setContractFinalDate(staffInfo.getContractFinalDate());
		} else {
			Date today = DateUtils.getNowDate();

			entity.setState(1);

			entity.setAccount("");
			entity.setCode("");
			entity.setUserName(entity.getMemberName());
			entity.setEmail("");
			entity.setJoinCompanyId("");
			entity.setDeptId("");
			entity.setJoinAddr("");
			entity.setJoinDate(DateUtils.format(today));
			entity.setPositiveDate(DateUtils.format(DateUtils.addMonths(today, 2)));
			entity.setProbationPeriod(2);
			entity.setPostName("");
			entity.setPostCategory("");
			entity.setPostLevel("");
			entity.setStaffTier("0");
			entity.setContractType("1");
			entity.setContractBeginDate(DateUtils.format(today));
			entity.setContractFinalDate(DateUtils.convertAddDay(DateUtils.format(DateUtils.addMonths(today, 24)),-1));
		}

		List<SysRole> webRoleList = sysRoleService.selectRoleAll();
		List<PreferAppRole> appRoleList = preferAppRoleService.selectRoleAll();

		return super.ok().put("entity", entity).put("webRoleList", webRoleList).put("appRoleList", appRoleList);
	}

	/**
	 * 加入审批（通过则创建账号）
	 * @return
	 */
	@RequestMapping("/approval")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult createAccount(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String appUserId = getAppUserId(parameterMap);

		String userId = Tool.convertObject(parameterMap.get("userId"));
		String memberId = Tool.convertObject(parameterMap.get("memberId"));
		String mobile = Tool.convertObject(parameterMap.get("mobile"));
		Integer state = Tool.convertStringToInt(parameterMap.get("state"));
		String rejectReason = Tool.convertObject(parameterMap.get("rejectReason"));

		SysDept companyInfo = sysDeptService.selectCompanyEntity();

		HrMemberJoin entity = hrMemberJoinService.selectEntityByUserId(userId);
		if(entity.getState()!=0){
			return super.error("该会员的加入申请已经审核过,请勿重复审核");
		}
		entity.setState(state);
		entity.setRejectReason(rejectReason);
		entity.setLastUpdateTime(new Date());

		/**
		 * 如果审核通过，
		 * 则要判断对应的会员在该企业是否有在职信息
		 * 如果有在职，不让通过
		 */
		if(state==1){
			Map<String, Object> currMemberMap = hrMemberJoinService.selectCurrMemberInfo(memberId);
			if(currMemberMap!=null && !currMemberMap.isEmpty()){
				return super.error("该会员在贵司还有在职信息，不可重复加入。");
			}
		}

		if(state==2){
			if(StringUtils.isEmpty(entity.getRejectReason())){
				return super.error("请输入拒绝原因");
			}
			AjaxResult responseAR = qytJoinCompanyCallbackService.callBack(entity.getMemberId(), companyInfo.getDeptId(), "", entity.getState()+"", entity.getRejectReason());
			String resCode = Tool.convertObject(responseAR.get("code"));
			String resMsg = Tool.convertObject(responseAR.get("msg"));
			if(StringUtils.isNotEmpty(resCode) && resCode.equals("0")){
				/**
				 * 注意，向企易通平台反馈信息成功之后才能修改审批信息
				 */
				hrMemberJoinService.updateEntity(entity);

				/**
				 * 向申请推送信息
				 */
				List<String> memberIdList = new ArrayList<>();
				memberIdList.add(entity.getMemberId());
				String msgTitle = "加入企业申请被驳回";
				String content = "您加入"+companyInfo.getDeptName()+"的申请被驳回,理由:"+entity.getRejectReason();
				sysPushService.sendPush(McSysCodeEnum.pmsAdmin.getCode(), companyInfo.getDeptId(), memberIdList, msgTitle, content);

				return super.ok();
			} else {
				return super.error(resMsg);
			}
		}


		String account = Tool.convertObject(parameterMap.get("account"));
		String code = Tool.convertObject(parameterMap.get("code"));
		String userName = Tool.convertObject(parameterMap.get("userName"));
		String email = Tool.convertObject(parameterMap.get("email"));
		String sex = Tool.convertObject(parameterMap.get("sex"));

		String deptId = Tool.convertObject(parameterMap.get("deptId"));

		String joinCompanyId = Tool.convertObject(parameterMap.get("joinCompanyId"));
		String joinAddr = Tool.convertObject(parameterMap.get("joinAddr"));
		String joinDate = Tool.convertObject(parameterMap.get("joinDate"));
		String positiveDate = Tool.convertObject(parameterMap.get("positiveDate"));
		String probationPeriod = Tool.convertObject(parameterMap.get("probationPeriod"));
		String postName = Tool.convertObject(parameterMap.get("postName"));
		String postCategory = Tool.convertObject(parameterMap.get("postCategory"));
		String postLevel = Tool.convertObject(parameterMap.get("postLevel"));
		//String staffTier = Tool.convertObject(parameterMap.get("staffTier"));

		String contractType = Tool.convertObject(parameterMap.get("contractType"));
		String contractBeginDate = Tool.convertObject(parameterMap.get("contractBeginDate"));
		String contractFinalDate = Tool.convertObject(parameterMap.get("contractFinalDate"));

		String webRoleIds = Tool.convertObject(parameterMap.get("webRoleIds"));
		String appRoleIds = Tool.convertObject(parameterMap.get("appRoleIds"));

		if(StringUtils.isEmpty(account)){
			return super.error("请输入职员账号");
		}
		if(!RegExpValidatorUtils.IsLetter(account)){
			return super.error("账号只能由字母组成");
		}
		if(StringUtils.isEmpty(code)){
			return super.error("请输入职员工号");
		}
		if(StringUtils.isEmpty(userName)){
			return super.error("请输入职员姓名");
		}
		if(StringUtils.isEmpty(email)){
			return super.error("请输入职员邮箱");
		}

		if(StringUtils.isEmpty(deptId)){
			return super.error("请选择入职部门");
		}

		if(StringUtils.isEmpty(joinCompanyId)){
			return super.error("请选择入职公司");
		}
		if(StringUtils.isEmpty(joinAddr)){
			return super.error("请输入入职地址");
		}
		if(StringUtils.isEmpty(joinDate)){
			return super.error("请选择入职日期");
		}
		if(StringUtils.isEmpty(positiveDate)){
			return super.error("请选择转正日期");
		}
		if(StringUtils.isEmpty(probationPeriod)){
			return super.error("请选择试用期");
		}
		if(StringUtils.isEmpty(postName)){
			return super.error("请输入入职职位");
		}
		if(StringUtils.isEmpty(postCategory)){
			return super.error("请选择岗位类别");
		}
		if(StringUtils.isEmpty(postLevel)){
			return super.error("请选择岗位级别");
		}

		if(StringUtils.isEmpty(contractType)){
			return super.error("请选择员工类型");
		}
		if(StringUtils.isEmpty(contractBeginDate)){
			return super.error("首次劳动合同起始时间");
		}
		if(StringUtils.isEmpty(contractFinalDate)){
			return super.error("首次劳动合同终止时间");
		}


		if(StringUtils.isEmpty(webRoleIds)){
			return super.error("请为职员赋予后台操作权限");
		}
		if(StringUtils.isEmpty(appRoleIds)){
			return super.error("请为职员赋予APP操作权限");
		}

		SysDept dept = sysDeptService.selectDeptById(deptId);

		SysUser user = new SysUser();
		user.setUserId(userId); //todo 注意这个地址的赋值的意义
		user.setAccount(account);
		user.setUserName(userName);
		user.setUserType(0);
		user.setCode(code);
		user.setPhonenumber(mobile);
		user.setEmail(email);
		user.setSex(sex);
		user.setAvatar("");
		user.setDeptId(dept.getDeptId());
		user.setStatus(1);
		user.setDelFlag("0");
		user.setCreateBy(appUserId);
		user.setCreateTime(new Date());

		if(sysUserService.checkAccountUnique(user.getAccount()).equals(UserConstants.USER_ACCOUNT_NOT_UNIQUE)){
			return error("账号重复");
		}
		String checkUniqueResult = sysUserService.checkUnique(user);
		if(checkUniqueResult.equals(UserConstants.USER_CODE_NOT_UNIQUE)){
			return error("工号重复");
		} else if(checkUniqueResult.equals(UserConstants.USER_EMAIL_NOT_UNIQUE)){
			return error("邮箱重复");
		}

		StaffInfo info = new StaffInfo();
		info.setName(user.getUserName());
		info.setEmail(user.getEmail());
		info.setSex(user.getSex());
		info.setJoinCompanyId(joinCompanyId);
		info.setJoinDeptId(dept.getDeptId());
		info.setJoinDeptName(dept.getDeptName());
		info.setJoinDate(DateUtils.conversion(joinDate));
		info.setJoinAddr(joinAddr);
		info.setPositiveDate(DateUtils.conversion(positiveDate));
		info.setProbationPeriod(Tool.convertStringToInt(probationPeriod));
		info.setPostName(postName);
		info.setPostCategory(postCategory);
		info.setPostLevel(postLevel);
		info.setStaffTier("0");
		info.setContractType(contractType);
		info.setContractBeginDate(DateUtils.conversion(contractBeginDate));
		info.setContractFinalDate(contractFinalDate);

		user.setInfo(info);

		/**
		 * 为用于赋予角色权限
		 */
		if(!StringUtils.isEmpty(webRoleIds)){
			user.setRoleIds(Arrays.asList(webRoleIds.split(",")));
		}


		SysUserScope scope = new SysUserScope();
		scope.setScopeData("3");
		scope.setScopeProject("3");
		scope.setScopeDataExpand("");
		user.setScope(scope);

		//sysUserService.insertUser(user);

		AjaxResult responseAR = qytJoinCompanyCallbackService.callBack(memberId, companyInfo.getDeptId(), userName, state+"", rejectReason);
		String resCode = Tool.convertObject(responseAR.get("code"));
		String resMsg = Tool.convertObject(responseAR.get("msg"));
		if(StringUtils.isNotEmpty(resCode) && resCode.equals("0")){
			/**
			 * 注意，向企易通平台反馈信息成功之后才能开户
			 */
			hrMemberJoinService.updateEntity(entity);

			sysUserService.insertUser(user);

			/**
			 * 处理APP使用角色
			 */
			List<PreferAppStaffRole> list = new ArrayList<PreferAppStaffRole>();
			if(!StringUtils.isEmpty(appRoleIds)){
				String[] arid = appRoleIds.split(",");
				for (String roleId : arid) {
					PreferAppStaffRole ur = new PreferAppStaffRole();
					ur.setUrId(SnowflakeIdUtil.getId());
					ur.setUserId(user.getUserId());
					ur.setRoleId(roleId);
					list.add(ur);
				}
			}
			if (list.size() > 0) {
				preferAppStaffRoleService.batchInsertAppStaffRole(entity.getUserId(), list);
			}


			/**
			 * 给系统运维人员(数据字典sys_devops)发送邮件提醒开通邮箱
			 */
			List<String> devopsList = new ArrayList<String>();
			List<SysDictData> dictList = sysDictDataService.selectDictDataByType(DictTypeEnum.sys_devops.getCode());
			if(dictList!=null && dictList.size()>0){
				for(SysDictData sdd : dictList){
					devopsList.add(sdd.getDictValue());
				}
			}
			List<StaffInfo> devOpsStaffList = null;
			if(devopsList!=null && devopsList.size()>0){
				devOpsStaffList = staffInfoService.selectListByAccounts(devopsList.toArray(new String[devopsList.size()]));
			}
			if(devOpsStaffList!=null && devOpsStaffList.size()>0){
				String subject = "开通账号";
				String content = "员工"+user.getUserName()+"已入职，工号:"+user.getCode()+"，请为员工开通邮箱账号（"+user.getEmail()+"）和vpn账号，开通完毕后请将账号与密码发送给员工本人。";

				for(StaffInfo si : devOpsStaffList){
					sysMailService.sendMail(McSysCodeEnum.pmsAdmin.getCode(), companyInfo.getDeptId(), si.getEmail(), subject, content);
				}
			}

			/**
			 * 向申请推送信息
			 */
			List<String> memberIdList = new ArrayList<>();
			memberIdList.add(entity.getMemberId());
			String msgTitle = "加入企业申请已通过";
			String content = "您加入"+companyInfo.getDeptName()+"的申请已经通过,欢迎你的加入。协同办公,让工作更高效.";
			sysPushService.sendPush(McSysCodeEnum.pmsAdmin.getCode(), companyInfo.getDeptId(), memberIdList, msgTitle, content);

			return super.ok();

		} else {
			return super.error(resMsg);
		}
	}
}
