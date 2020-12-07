package com.pms.app.hr;

import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.config.YckConfigUtils;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.constant.UserConstants;
import com.pms.common.enums.DictTypeEnum;
import com.pms.common.enums.McSysCodeEnum;
import com.pms.common.service.SysMailService;
import com.pms.common.service.SysPushService;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.RegExpValidatorUtils;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.hr.domain.StaffGrade;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IStaffGradeService;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.system.domain.*;
import com.pms.core.system.service.*;
import com.pms.drools.service.impl.DroolsBusinessServiceImpl;
import com.pms.framework.web.base.AppBaseController;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
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
@RequestMapping("/app/hr/accountCreate")
public class AccountCreateController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private ISysUserService sysUserService;
	@Autowired
	private IStaffInfoService staffInfoService;
	@Autowired
	private ISysDeptService sysDeptService;
	@Autowired
	private ISysRoleService sysRoleService;
	@Autowired
	private IStaffGradeService staffGradeService;

	@Autowired
	private ISysMenuService sysMenuService;

	@Autowired
	private YckConfigUtils yckConfigUtils;

	@Autowired
	private ISysDictDataService sysDictDataService;

	@Autowired
	private SysPushService sysPushService;
	@Autowired
	private SysMailService sysMailService;

	@Autowired
	private DroolsBusinessServiceImpl droolsBusinessServiceImpl;

	/**
	 * 查询职员职位级别列表(排除禁用)
	 */
	@RequestMapping("/staffGradeList")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult staffGradeList(HttpServletRequest request, HttpServletResponse response)
	{
		List<StaffGrade> gradeList = staffGradeService.selectNormalList();
		return super.ok().put("gradeList", gradeList);
	}

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

		SysUser user = sysUserService.selectUserById(userId);
		StaffInfo staff = staffInfoService.selectEntityById(userId);

		/**
		 * 查询部门
		 */
		//List<SysDept> deptList = sysDeptService.selectLastDept();
		List<SysDept> deptList = sysDeptService.selectAll();

		Date today = DateUtils.getNowDate();

		Map<String, Object> entity = new HashMap<>();
		entity.put("account", "");
		entity.put("code", "");
		entity.put("userName", "");
		entity.put("email", "@riveretech.com");

		entity.put("joinCompanyName", "");
		entity.put("deptId", staff.getJoinDeptId());
		entity.put("deptName", staff.getJoinDeptName());
		entity.put("joinAddr", "北京");
		entity.put("joinDate", DateUtils.format(today));

		entity.put("postName", "");
		entity.put("postCategory", "T");
		entity.put("postLevel", "");
		entity.put("staffTier", "0");
		entity.put("postRank", "");
		entity.put("levelId", "");

		entity.put("probationPeriod", 2);
		entity.put("positiveDate", DateUtils.format(DateUtils.addMonths(today, 2)));

		entity.put("contractType", "全职");
		entity.put("contractBeginDate", DateUtils.format(today));
		entity.put("contractFinalDate", DateUtils.convertAddDay(DateUtils.format(DateUtils.addMonths(today, 24)),-1));

		//用户权限列表
		Set<String> permissions = sysMenuService.selectPermsByUserId(user);
		boolean hasPermission = permissions.contains("system:user:add"); //是否具有创建用户的权限

		return super.ok().put("entity", entity).put("deptList", deptList).put("hasPermission", hasPermission);
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

		String account = Tool.convertObject(parameterMap.get("account"));
		String code = Tool.convertObject(parameterMap.get("code"));
		String userName = Tool.convertObject(parameterMap.get("userName"));
		String email = Tool.convertObject(parameterMap.get("email"));

		String deptId = Tool.convertObject(parameterMap.get("deptId"));

		String joinCompanyName = Tool.convertObject(parameterMap.get("joinCompanyName"));
		String joinAddr = Tool.convertObject(parameterMap.get("joinAddr"));
		String joinDate = Tool.convertObject(parameterMap.get("joinDate"));
		String positiveDate = Tool.convertObject(parameterMap.get("positiveDate"));
		String probationPeriod = Tool.convertObject(parameterMap.get("probationPeriod"));
		String postName = Tool.convertObject(parameterMap.get("postName"));
		String postCategory = Tool.convertObject(parameterMap.get("postCategory"));
		String postLevel = Tool.convertObject(parameterMap.get("postLevel"));
		String postRank = Tool.convertObject(parameterMap.get("postRank"));
		String levelId = Tool.convertObject(parameterMap.get("levelId"));

		String contractType = Tool.convertObject(parameterMap.get("contractType"));
		String contractBeginDate = Tool.convertObject(parameterMap.get("contractBeginDate"));
		String contractFinalDate = Tool.convertObject(parameterMap.get("contractFinalDate"));

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

		if(StringUtils.isEmpty(joinCompanyName)){
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

		SysDept companyInfo = sysDeptService.selectCompanyEntity();

		StaffInfo staff = staffInfoService.selectEntityById(userId);

		SysDept dept = sysDeptService.selectDeptById(deptId);

		SysUser user = new SysUser();
		user.setUserId(SnowflakeIdUtil.getId());
		user.setAccount(account);
		user.setUserName(userName);
		user.setUserType(0);
		user.setCode(code);
		user.setPhonenumber("");
		user.setEmail(email);
		user.setSex("2");
		user.setAvatar("");
		user.setDeptId(dept.getDeptId());
		user.setStatus(1);
		user.setDelFlag("0");
		user.setCreateBy(staff.getUserId());
		user.setCreateTime(new Date());

		if(sysUserService.checkAccountUnique(account).equals(UserConstants.USER_ACCOUNT_NOT_UNIQUE)){
			return error("账号重复");
		}
		String checkUniqueResult = sysUserService.checkUnique(user);
		if(checkUniqueResult.equals(UserConstants.USER_CODE_NOT_UNIQUE)){
			return error("工号重复");
		}
		/*if(checkUniqueResult.equals(UserConstants.USER_PHONE_NOT_UNIQUE)){
			return error("手机号重复");
		}*/
		if(checkUniqueResult.equals(UserConstants.USER_EMAIL_NOT_UNIQUE)){
			return error("邮箱重复");
		}

		StaffInfo info = new StaffInfo();
		info.setName(user.getUserName());
		info.setEmail(user.getEmail());
		info.setSex(user.getSex());
		info.setJoinCompanyId("");
		info.setJoinCompanyName(joinCompanyName);
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
		info.setPostRank(postRank);
		info.setLevelId(levelId);
		info.setContractType(contractType);
		info.setContractBeginDate(DateUtils.conversion(contractBeginDate));
		info.setContractFinalDate(contractFinalDate);

		user.setInfo(info);

		SysRole roleEntity = sysRoleService.selectRoleByRoleKey("staff");
		if(roleEntity==null){
			return super.error("请联系管理员先添加默认的职员角色");
		}
		List<String> roleIds = new ArrayList<>();
		roleIds.add(roleEntity.getRoleId());
		user.setRoleIds(roleIds);



		SysUserScope scope = new SysUserScope();
		scope.setScopeData("3");
		scope.setScopeProject("3");
		scope.setScopeDataExpand("");
		user.setScope(scope);

		sysUserService.insertUser(user);

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
			String content = "员工"+userName+"已入职，工号:"+code+"，请为员工开通邮箱账号（"+email+"）和vpn账号，开通完毕后请将账号与密码发送给员工本人。";

			for(StaffInfo si : devOpsStaffList){
				sysMailService.sendMail(McSysCodeEnum.pmsAdmin.getCode(), companyInfo.getDeptId(), si.getEmail(), subject, content);
			}
		}

		return super.ok();
	}

}
