package com.pms.app.common;

import com.pms.activiti.config.ActProRoleEnum;
import com.pms.activiti.config.ActRoleEnum;
import com.pms.activiti.service.impl.ActProcessServiceImpl;
import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.page.WebPage;
import com.pms.common.utils.StringUtils;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.project.domain.ProBaseEntity;
import com.pms.core.project.service.impl.ProBaseService;
import com.pms.core.system.service.ISysRoleService;
import com.pms.drools.service.impl.DroolsBusinessServiceImpl;
import com.pms.framework.web.base.AppBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * 规则引擎帮助类
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/common/drools")
public class DroolsHelpController extends AppBaseController {

	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private DroolsBusinessServiceImpl droolsBusinessServiceImpl;

	@Autowired
	private ProBaseService proBaseService;

	@Autowired
	private IStaffInfoService staffInfoService;

	@Autowired
	private ISysRoleService sysRoleService;

	/**
	 * 获取抄送人员的列表
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/getCcStaffList")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult getCcStaffList(HttpServletRequest request, HttpServletResponse response) {
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String businessType = Tool.convertObject(parameterMap.get("businessType"));
		String businessId = "";
		String proId = Tool.convertObject(parameterMap.get("proId"));
		String days = Tool.convertObject(parameterMap.get("days"));

		String proRole = ActProRoleEnum.member.getCode();
		if(StringUtils.isNotEmpty(proId)){
			/**
			 * 判断申请人在项目上的角色
			 */
			ProBaseEntity proBaseEntity = proBaseService.queryByProId(proId);
			if(proBaseEntity!=null){
				if(StringUtils.isNotEmpty(proBaseEntity.getProManager()) && proBaseEntity.getProManager().equals(userId)){
					proRole = ActProRoleEnum.pm.getCode();
				} else if(StringUtils.isNotEmpty(proBaseEntity.getProChief()) && proBaseEntity.getProChief().equals(userId)){
					proRole = ActProRoleEnum.pd.getCode();
				} else if(StringUtils.isNotEmpty(proBaseEntity.getProLead()) && proBaseEntity.getProLead().equals(userId)){
					proRole = ActProRoleEnum.dm.getCode();
				}
			}
		}

		/**
		 * 所属系统销售角色
		 * 后续会根据销售角色做抄送的区别
		 * 出差：
		 * 普通销售（角色）： 销售总监审核（PM）    CC：姚先友，李岩
		 * 销售总监（角色）： 部门经理审核（BM）    CC：花建和，黄龙，李岩
		 */
		String saleRoleType = "";
		List<String> roleKeys = sysRoleService.selectRoleKeysByUserId(userId);
		if(roleKeys!=null && roleKeys.size()>0){
			if(roleKeys.contains(ActRoleEnum.sale_vp.getCode())){
				saleRoleType = ActRoleEnum.sale_vp.getCode();
			} else if(roleKeys.contains(ActRoleEnum.sale_chief.getCode())){
				saleRoleType = ActRoleEnum.sale_chief.getCode();
			} else if(roleKeys.contains(ActRoleEnum.sale_staff.getCode())){
				saleRoleType = ActRoleEnum.sale_staff.getCode();
			}
		}

		Map<String, Object> otherParam = new HashMap<>();
		if(StringUtils.isNotEmpty(proId)){
			otherParam.put("proId", proId);
		}
		if(StringUtils.isNotEmpty(proRole)){
			otherParam.put("proRole", proRole);
		}
		if(StringUtils.isNotEmpty(days)){
			otherParam.put("days", days);
		}
		if(StringUtils.isNotEmpty(saleRoleType)){
			otherParam.put("saleRoleType", saleRoleType);
		}

		StaffInfo staff = staffInfoService.selectEntityById(userId);

		Set<String> ccAccounts = droolsBusinessServiceImpl.getCcStaffList(staff.getAccount(), businessType, businessId, otherParam );

		List<StaffInfo> ccStaffList = new ArrayList<>();
		if(ccAccounts!=null && ccAccounts.size()>0){
			List<StaffInfo> list = staffInfoService.selectListByAccounts(ccAccounts.toArray(new String[ccAccounts.size()]));
			if(list!=null && list.size()>0){
				for(StaffInfo si : list){
					/**
					 * 手机号脱敏
					 */
					si.setPhonenumber(Tool.mobileEncrypt(si.getPhonenumber()));
					/**
					 * 岗位脱敏
					 */
					if(StringUtils.isNotEmpty(si.getPostName())){
						si.setPostName("***");
					}

					/**
					 * 排除申请人
					 */
					if(!si.getAccount().equals(staff.getAccount())){
						ccStaffList.add(si);
					}
				}
			}
		}
		return super.ok().put("ccStaffList", ccStaffList);
	}

}
