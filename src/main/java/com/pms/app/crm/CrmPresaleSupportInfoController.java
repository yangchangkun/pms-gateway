package com.pms.app.crm;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pms.activiti.config.ActBusinessTypeEnum;
import com.pms.activiti.config.ActProRoleEnum;
import com.pms.activiti.config.ActRoleEnum;
import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.enums.SeqTypeEnum;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.crm.domain.*;
import com.pms.core.crm.service.*;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.project.domain.ProBaseEntity;
import com.pms.core.system.domain.SysDictData;
import com.pms.core.system.service.ISysDictDataService;
import com.pms.core.system.service.ISysUserService;
import com.pms.core.system.service.impl.SysSeqService;
import com.pms.drools.service.impl.DroolsBusinessServiceImpl;
import com.pms.framework.web.base.AppBaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


/**
 * 销售售前技术支持申请 信息操作处理
 * 
 * @author zfl
 * @date 2020-08-28
 */
@Controller
@RequestMapping("/app/crm/presaleSupportInfo")
public class CrmPresaleSupportInfoController extends AppBaseController
{
	@Autowired
	private ICrmPresaleSupportInfoService crmPresaleSupportInfoService;
	@Autowired
	private SysSeqService sysSeqService;
	@Autowired
	private ISysUserService sysUserService;
	@Autowired
	private IStaffInfoService staffInfoService;

	@Autowired
	private DroolsBusinessServiceImpl droolsBusinessServiceImpl;

	@Autowired
	private ICrmPresaleSupportFlowService crmPresaleSupportFlowService;
	@Autowired
	private ICrmPresaleSupportTeamService crmPresaleSupportTeamService;
	@Autowired
	private ICrmPresaleSupportOssService crmPresaleSupportOssService;
	@Autowired
	private ISysDictDataService dictDataService;


	/**
	 * 查询与‘我’相关的销售售前技术支持申请列表
	 * 根据 类型来获取列表
	 * 0、我申请的
	 * 1、我支持的
	 * 2、抄送我的
	 */
	@PostMapping("/myList")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult list(HttpServletRequest request, HttpServletResponse response)
	{
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		String type = Tool.convertObject(parameterMap.get("type")); //人员类型：0 发起人，1、支持人员，2 抄送人员,3 审批人员

		if(StringUtils.isEmpty(type)){
			return super.error("查询类型不能为空");
		}

		if(type.equals("0")){
			parameterMap.put("createUserId",userId);
		} else if(type.equals("1")){
			parameterMap.put("recordType", 1);
			parameterMap.put("supportUserId",userId);
		} else if(type.equals("2")){
			parameterMap.put("recordType", 1);
			parameterMap.put("ccUserId",userId);
		} else if(type.equals("3")){
			parameterMap.put("recordType", 1);
			parameterMap.put("approveUserId",userId);
		}

		int pageNum = Tool.convertStringToInt(parameterMap.get("pageNum"));
		if(pageNum<=0){
			pageNum = 1;
		}
		int pageSize = Tool.convertStringToInt(parameterMap.get("pageSize"));
		if(pageSize<=0 || pageSize>50){
			pageSize = 10;
		}

		String orderBy = " cpsi.create_time desc ";

		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<CrmPresaleSupportInfo> list = crmPresaleSupportInfoService.selectList(parameterMap);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());

		String chiefFlag = "0"; //销售总监标志 0 销售， 1 销售总监
		List<Map<String, Object>> userList = sysUserService.selectUserListByRoleKey("", ActRoleEnum.sale_chief.getCode());
		if(userList != null && userList.size()>0){
			for(Map<String, Object> map: userList){
				String _userId = Tool.convertObject(map.get("userId"));
				String _userName = Tool.convertObject(map.get("userName"));

				if(userId.equals(_userId)){
					chiefFlag = "1";
					break;
				}
			}
		}

        return ok().put("gridDatas", list).put("webPage", page).put("chiefFlag", chiefFlag);
	}


	/**
	 * 售前技术支持初始化接口
	 * 通常，APP在添加的时候需要调用该接口
	 * 在编辑的时候，调用详情
	 */
	@RequestMapping("/init")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult init(HttpServletRequest request, HttpServletResponse response)
	{
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		StaffInfo staff = staffInfoService.selectEntityById(userId);

		String chiefFlag = "0"; //销售总监标志 0 销售， 1 销售总监
		List<Map<String, Object>> chiefUserList = sysUserService.selectUserListByRoleKey("", ActRoleEnum.sale_chief.getCode());
		List<String> chiefUserIds = new ArrayList<>();
		if(chiefUserList != null && chiefUserList.size()>0){
			for(Map<String, Object> map: chiefUserList){
				String _userId = Tool.convertObject(map.get("userId"));
				String _userName = Tool.convertObject(map.get("userName"));
				chiefUserIds.add(_userId);

				if(chiefFlag.equals("0") && userId.equals(_userId)){
					chiefFlag = "1";
				}
			}
		}
		List<StaffInfo> chiefStaffList = new ArrayList<>();
		if(chiefUserIds!=null && chiefUserIds.size()>0){
			List<StaffInfo> list = staffInfoService.selectListByUserIds(chiefUserIds.toArray(new String[chiefUserIds.size()]));
			if(list!=null && list.size()>0){
				for(StaffInfo si : list){
					/**
					 * 排除申请人
					 */
					if(!si.getUserId().equals(userId)){
						chiefStaffList.add(si);
					}
				}
			}
		}


		String vpFlag = "0"; //营销总监标志 0 不是， 1 是
		//营销总监
		List<Map<String, Object>> vpUserList = sysUserService.selectUserListByRoleKey("", ActRoleEnum.sale_vp.getCode());
		if(vpUserList != null && vpUserList.size()>0){
			for(Map<String, Object> map: chiefUserList){
				String _userId = Tool.convertObject(map.get("userId"));
				String _userName = Tool.convertObject(map.get("userName"));

				if(userId.equals(_userId)){
					vpFlag = "1";
					break;
				}
			}
		}

		/**
		 * 获取抄送人员信息
		 */
		String businessType = ActBusinessTypeEnum.crm_presale_support.getCode();
		String businessId = "";
		Map<String, Object> otherParam = new HashMap<>();

		Set<String> ccAccounts = droolsBusinessServiceImpl.getCcStaffList(staff.getAccount(), businessType, businessId, otherParam );

		List<StaffInfo> ccStaffList = new ArrayList<>();
		if(ccAccounts!=null && ccAccounts.size()>0){
			List<StaffInfo> list = staffInfoService.selectListByAccounts(ccAccounts.toArray(new String[ccAccounts.size()]));
			if(list!=null && list.size()>0){
				for(StaffInfo si : list){
					ccStaffList.add(si);
				}
			}
		}

		return super.ok().put("chiefFlag", chiefFlag)
				.put("vpFlag", vpFlag)
				.put("chiefStaffList", chiefStaffList)
				.put("ccStaffList", ccStaffList);
	}

    /**
     * 查询销售售前技术支持申请详情
     */
    @RequestMapping("/detail")
    @ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
    public AjaxResult detail(HttpServletRequest request, HttpServletResponse response)
    {
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String id = Tool.convertObject(parameterMap.get("id"));
		if(StringUtils.isEmpty(id)){
			return super.error("ID不能为空");
		}

		CrmPresaleSupportInfo entity = crmPresaleSupportInfoService.selectEntityById(id);
		if(entity==null){
			return super.error("记录不存在");
		}

		List<CrmPresaleSupportTeam> teams = crmPresaleSupportTeamService.selectListByPssId(entity.getId());
		List<CrmPresaleSupportFlow> flows = crmPresaleSupportFlowService.selectListByPssId(entity.getId());
		List<CrmPresaleSupportOss> oss = crmPresaleSupportOssService.selectListByPssId(entity.getId());

		String chiefFlag = "0"; //销售总监标志 0 不是， 1 是
		if(StringUtils.isNotEmpty(entity.getChiefUserId()) && entity.getChiefUserId().equals(userId)){
			chiefFlag = "1";
		}
		List<Map<String, Object>> chiefUserList = sysUserService.selectUserListByRoleKey("", ActRoleEnum.sale_chief.getCode());
		List<String> chiefUserIds = new ArrayList<>();
		if(chiefUserList != null && chiefUserList.size()>0){
			for(Map<String, Object> map: chiefUserList){
				String _userId = Tool.convertObject(map.get("userId"));
				String _userName = Tool.convertObject(map.get("userName"));
				chiefUserIds.add(_userId);
			}
		}
		List<StaffInfo> chiefStaffList = new ArrayList<>();
		if(chiefUserIds!=null && chiefUserIds.size()>0){
			List<StaffInfo> list = staffInfoService.selectListByUserIds(chiefUserIds.toArray(new String[chiefUserIds.size()]));
			if(list!=null && list.size()>0){
				for(StaffInfo si : list){
					/**
					 * 排除申请人
					 */
					if(!si.getUserId().equals(userId)){
						chiefStaffList.add(si);
					}
				}
			}
		}

		String vpFlag = "0"; //营销总监标志 0 不是， 1 是
		if(StringUtils.isNotEmpty(entity.getVpUserId()) && entity.getVpUserId().equals(userId)){
			vpFlag = "1";
		}

		String signFlag = "0"; //签收标志 0：否，1：是
		CrmPresaleSupportTeam teamEntity = crmPresaleSupportTeamService.selectEntityByPssIdAndUserId(id, userId);
		if(teamEntity!=null  && Tool.convertObject(teamEntity.getState()).equals("1")){
			signFlag = "1";
		}

		List<String> ccUserIds = new ArrayList<>();
		if(teams!=null && teams.size()>0){
			for(CrmPresaleSupportTeam t : teams){
				if(t.getType().equals("2")){
					ccUserIds.add(t.getUserId());
				}
			}
		}
		List<StaffInfo> ccStaffList = new ArrayList<>();
		if(ccUserIds!=null && ccUserIds.size()>0){
			List<StaffInfo> list = staffInfoService.selectListByUserIds(ccUserIds.toArray(new String[ccUserIds.size()]));
			if(list!=null && list.size()>0){
				for(StaffInfo si : list){
					ccStaffList.add(si);
				}
			}
		}

        return super.ok().put("entity", entity).put("teams", teams).put("flows", flows).put("oss", oss)
				.put("chiefFlag", chiefFlag)
				.put("vpFlag", vpFlag)
				.put("signFlag", signFlag)
				.put("chiefStaffList", chiefStaffList)
				.put("ccStaffList", ccStaffList);
    }

	/**
	 * 新增保存销售售前技术支持申请
	 */
	@RequestMapping("/add")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult addSave(HttpServletRequest request, HttpServletResponse response)
	{
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String recordType = Tool.convertObject(parameterMap.get("recordType"));
		String customerId = Tool.convertObject(parameterMap.get("customerId"));
		String projectType = Tool.convertObject(parameterMap.get("projectType"));
		String supportType = Tool.convertObject(parameterMap.get("supportType"));
		String executeAddr = Tool.convertObject(parameterMap.get("executeAddr"));
		String executeBeginDay = Tool.convertObject(parameterMap.get("executeBeginDay"));
		String executeEndDay = Tool.convertObject(parameterMap.get("executeEndDay"));
		String applyIndex = Tool.convertObject(parameterMap.get("applyIndex"));
		String linkState = Tool.convertObject(parameterMap.get("linkState"));
		String saleDescribe = Tool.convertObject(parameterMap.get("saleDescribe"));
		String supportRequire = Tool.convertObject(parameterMap.get("supportRequire"));

		String chiefFlag = Tool.convertObject(parameterMap.get("chiefFlag"));//0销售 1销售总监
		String chiefId = Tool.convertObject(parameterMap.get("chiefId")); //销售总监，如果提交人不是总监，则要选择总监
		String proId = Tool.convertObject(parameterMap.get("proId")); //项目ID，如果提交人是销售总监，则要选择项目


		String oss = Tool.convertObject(parameterMap.get("oss"));
		//抄送人的id
		String ccUserIds = Tool.convertObject(parameterMap.get("ccUserIds"));

		if(StringUtils.isEmpty(customerId)){
			return super.error("客户名称不能为空");
		}

		if(chiefFlag.equals("0")){
			if(StringUtils.isEmpty(chiefId)){
				return super.error("销售总监不能为空");
			}
		} else {
			if(StringUtils.isEmpty(proId)){
				return super.error("项目不能为空");
			}
		}

		String chiefUserId = ""; //销售总监userId
		String leaderUserId = ""; //部门经理userId
		String vpUserId = "";//营销总监userId
		String approveUserId = ""; //当前审批人的userId

		//草稿就不用判断其他的填写条件
		if(recordType.equals("1")){
			if(StringUtils.isEmpty(projectType)){
				return super.error("项目类型不能为空");
			}
			if(StringUtils.isEmpty(supportType)){
				return super.error("技术支持类型不能为空");
			}
			if(StringUtils.isEmpty(executeAddr)){
				return super.error("执行地点不能为空");
			}
			if(StringUtils.isEmpty(executeBeginDay)){
				return super.error("执行的开始时间不能为空");
			}
			if(StringUtils.isEmpty(executeEndDay)){
				return super.error("执行的完成时间不能为空");
			}
			if(StringUtils.isEmpty(applyIndex)){
				return super.error("项目第几次申请不能为空");
			}
			if(StringUtils.isEmpty(linkState)){
				return super.error("交流状态不能为空");
			}
			if(StringUtils.isEmpty(saleDescribe)){
				return super.error("销售信息不能为空");
			}
			if(StringUtils.isEmpty(supportRequire)){
				return super.error("支持要求不能为空");
			}
			if(StringUtils.isEmpty(chiefFlag)){
				return super.error("当前账户的总监标志不能为空");
			}
		}
		//获取统计部门是第几级
		List<SysDictData> levelList = dictDataService.selectDictDataByType("report_dept_level");
		int level=3;//默认第三级
		if(levelList != null && levelList.size()>0){
			level=Integer.parseInt(levelList.get(0).getDictValue());
		}

		//获取提交人所在的部门的部门经理
		String leaderId = sysUserService.selectUserIdByKey(level,ActRoleEnum.dm.getCode(), userId);
		if(StringUtils.isEmpty(leaderId)){
			return super.error("您所在部门暂无部门经理,该申请不能提交.");
		}
		leaderUserId = leaderId;

		//营销总监
		List<Map<String, Object>> userList = sysUserService.selectUserListByRoleKey("", ActRoleEnum.sale_vp.getCode());
		String vpId = "";
		if(userList != null && userList.size()>0){
			for(Map<String, Object> map: userList) {
				String _userId = Tool.convertObject(map.get("userId"));
				String _userName = Tool.convertObject(map.get("userName"));
				vpId = _userId;
			}
		}
		if(StringUtils.isEmpty(vpId)){
			return super.error("系统未设置营销总监,该申请不能提交.");
		}
		vpUserId = vpId;

		if(chiefFlag.equals("0")){
			//提交人为普通销售
			chiefUserId = chiefId;
			approveUserId = chiefId;
		} else {
			//提交人为销售总监
			chiefUserId = userId;
			approveUserId = leaderId;
		}


		String pssId = SnowflakeIdUtil.getId();

		String currDay = DateUtils.getCurrentDay();
		String recordSn = sysSeqService.getSeqNumber(Tool.convertStringToInt(SeqTypeEnum.crmPresale.toString()), currDay);

		CrmPresaleSupportInfo entity = new CrmPresaleSupportInfo();
		entity.setId(pssId);
		entity.setRecordSn(recordSn);
		entity.setApplyIndex(Tool.convertStringToInt(applyIndex));
		entity.setApproveState(0);
		entity.setChiefUserId(chiefUserId);
		entity.setApproveUserId(approveUserId);
		entity.setCustomerId(customerId);
		entity.setExecuteAddr(executeAddr);
		entity.setExecuteBeginDay(DateUtils.conversion(executeBeginDay));
		entity.setExecuteEndDay(DateUtils.conversion(executeEndDay));
		entity.setLinkState(Tool.convertStringToInt(linkState));
		entity.setManagerUserId(leaderUserId);
		entity.setProId(proId);
		entity.setProjectType(projectType);
		entity.setRecordType(Tool.convertStringToInt(recordType));
		entity.setSaleDescribe(saleDescribe);
		entity.setSaleReport("");
		entity.setSupportRequire(supportRequire);
		entity.setSupportType(supportType);
		entity.setVpUserId(vpUserId);
		entity.setCreateTime(new Date());
		entity.setCreateUserId(userId);
		entity.setLastUpdateTime(new Date());
		entity.setUpdateUserId(userId);

		crmPresaleSupportInfoService.insertEntity(entity);
		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<CrmPresaleSupportOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				CrmPresaleSupportOss wo = new CrmPresaleSupportOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setPssId(pssId);
				wo.setOssId(ossId);
				ossDatas.add(wo);
			}
			if(ossDatas!=null && ossDatas.size()>0){
				crmPresaleSupportOssService.batchInsertEntity(pssId, ossDatas);
			}
		}
		/**
		 * 添加抄送人
		 */
		if(StringUtils.isNotEmpty(ccUserIds)){
			List<CrmPresaleSupportTeam> teamDatas = new ArrayList<>();
			String[] ccArray = ccUserIds.split(",");
			if(ccArray!=null && ccArray.length>0){
				for(String ccUserId : ccArray){
					CrmPresaleSupportTeam wo = new CrmPresaleSupportTeam();
					wo.setId(SnowflakeIdUtil.getId());
					wo.setPssId(pssId);
					wo.setUserId(ccUserId);
					wo.setType("2");
					wo.setState("");
					teamDatas.add(wo);
				}
			}

			if(teamDatas!=null && teamDatas.size()>0){
				crmPresaleSupportTeamService.batchInsertCcEntity(pssId, teamDatas);
			}

		}
        return super.ok();

	}

	/**
	 * 修改保存销售售前技术支持申请
	 */
	@RequestMapping("/edit")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult editSave(HttpServletRequest request, HttpServletResponse response)
	{
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String id = Tool.convertObject(parameterMap.get("id"));
		String recordType = Tool.convertObject(parameterMap.get("recordType"));
		String customerId = Tool.convertObject(parameterMap.get("customerId"));
		String projectType = Tool.convertObject(parameterMap.get("projectType"));
		String supportType = Tool.convertObject(parameterMap.get("supportType"));
		String executeAddr = Tool.convertObject(parameterMap.get("executeAddr"));
		String executeBeginDay = Tool.convertObject(parameterMap.get("executeBeginDay"));
		String executeEndDay = Tool.convertObject(parameterMap.get("executeEndDay"));
		String applyIndex = Tool.convertObject(parameterMap.get("applyIndex"));
		String linkState = Tool.convertObject(parameterMap.get("linkState"));
		String saleDescribe = Tool.convertObject(parameterMap.get("saleDescribe"));
		String supportRequire = Tool.convertObject(parameterMap.get("supportRequire"));

		String chiefFlag = Tool.convertObject(parameterMap.get("chiefFlag"));//0销售 1销售总监
		String chiefId = Tool.convertObject(parameterMap.get("chiefId")); //销售总监，如果提交人不是总监，则要选择总监
		String proId = Tool.convertObject(parameterMap.get("proId")); //项目ID，如果提交人死活销售总监，则要选择项目


		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccUserIds = Tool.convertObject(parameterMap.get("ccUserIds"));

		if(StringUtils.isEmpty(customerId)){
			return super.error("客户名称不能为空");
		}

		if(chiefFlag.equals("0")){
			if(StringUtils.isEmpty(chiefId)){
				return super.error("销售总监不能为空");
			}
		} else {
			if(StringUtils.isEmpty(proId)){
				return super.error("项目不能为空");
			}
		}

		String chiefUserId = ""; //销售总监userId
		String leaderUserId = ""; //部门经理userId
		String vpUserId = "";//营销总监userId
		String approveUserId = ""; //当前审批人的userId

		//草稿就不用判断其他的填写条件
		if(recordType.equals("1")){
			if(StringUtils.isEmpty(projectType)){
				return super.error("项目类型不能为空");
			}
			if(StringUtils.isEmpty(supportType)){
				return super.error("技术支持类型不能为空");
			}
			if(StringUtils.isEmpty(executeAddr)){
				return super.error("执行地点不能为空");
			}
			if(StringUtils.isEmpty(executeBeginDay)){
				return super.error("执行的开始时间不能为空");
			}
			if(StringUtils.isEmpty(executeEndDay)){
				return super.error("执行的完成时间不能为空");
			}
			if(StringUtils.isEmpty(applyIndex)){
				return super.error("项目第几次申请不能为空");
			}
			if(StringUtils.isEmpty(linkState)){
				return super.error("交流状态不能为空");
			}
			if(StringUtils.isEmpty(saleDescribe)){
				return super.error("销售信息不能为空");
			}
			if(StringUtils.isEmpty(supportRequire)){
				return super.error("支持要求不能为空");
			}
			if(StringUtils.isEmpty(chiefFlag)){
				return super.error("当前账户的总监标志不能为空");
			}
		}

		//获取统计部门是第几级
		List<SysDictData> levelList = dictDataService.selectDictDataByType("report_dept_level");
		int level=3;//默认第三级
		if(levelList != null && levelList.size()>0){
			level=Integer.parseInt(levelList.get(0).getDictValue());
		}
		//获取提交人所在的部门的部门经理
		String leaderId = sysUserService.selectUserIdByKey(level,ActRoleEnum.dm.getCode(), userId);
		if(StringUtils.isEmpty(leaderId)){
			return super.error("您所在部门暂无部门经理,该申请不能提交.");
		}
		leaderUserId = leaderId;

		//营销总监
		List<Map<String, Object>> userList = sysUserService.selectUserListByRoleKey("", ActRoleEnum.sale_vp.getCode());
		String vpId = "";
		if(userList != null && userList.size()>0){
			for(Map<String, Object> map: userList) {
				String _userId = Tool.convertObject(map.get("userId"));
				String _userName = Tool.convertObject(map.get("userName"));
				vpId = _userId;
			}
		}
		if(StringUtils.isEmpty(vpId)){
			return super.error("系统未设置营销总监,该申请不能提交.");
		}
		vpUserId = vpId;

		if(chiefFlag.equals("0")){
			//提交人为普通销售
			chiefUserId = chiefId;
			approveUserId = chiefId;
		} else {
			//提交人为销售总监
			chiefUserId = userId;
			approveUserId = leaderId;
		}


		CrmPresaleSupportInfo entity = crmPresaleSupportInfoService.selectEntityById(id);
		entity.setApplyIndex(Tool.convertStringToInt(applyIndex));
		entity.setApproveState(0);
		entity.setChiefUserId(chiefUserId);
		entity.setApproveUserId(approveUserId);
		entity.setCustomerId(customerId);
		entity.setExecuteAddr(executeAddr);
		entity.setExecuteBeginDay(DateUtils.conversion(executeBeginDay));
		entity.setExecuteEndDay(DateUtils.conversion(executeEndDay));
		entity.setLinkState(Tool.convertStringToInt(linkState));
		entity.setManagerUserId(leaderUserId);
		entity.setProId(proId);
		entity.setProjectType(projectType);
		entity.setRecordType(Tool.convertStringToInt(recordType));
		entity.setSaleDescribe(saleDescribe);
		entity.setSaleReport("");
		entity.setSupportRequire(supportRequire);
		entity.setSupportType(supportType);
		entity.setVpUserId(vpUserId);
		entity.setLastUpdateTime(new Date());
		entity.setUpdateUserId(userId);

		crmPresaleSupportInfoService.updateEntity(entity);
		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<CrmPresaleSupportOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				CrmPresaleSupportOss wo = new CrmPresaleSupportOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setPssId(id);
				wo.setOssId(ossId);
				ossDatas.add(wo);
			}
			if(ossDatas!=null && ossDatas.size()>0){
				crmPresaleSupportOssService.batchInsertEntity(id, ossDatas);
			}
		}
		/**
		 * 添加抄送人
		 */
		if(StringUtils.isNotEmpty(ccUserIds)){
			List<CrmPresaleSupportTeam> teamDatas = new ArrayList<>();
			String[] ccArray = ccUserIds.split(",");
			if(ccArray!=null && ccArray.length>0){
				for(String ccUserId : ccArray){
					CrmPresaleSupportTeam wo = new CrmPresaleSupportTeam();
					wo.setId(SnowflakeIdUtil.getId());
					wo.setPssId(id);
					wo.setUserId(ccUserId);
					wo.setType("2");
					wo.setState("");
					teamDatas.add(wo);
				}
			}

			if(teamDatas!=null && teamDatas.size()>0){
				crmPresaleSupportTeamService.batchInsertCcEntity(id, teamDatas);
			}

		}
		return super.ok();
	}

	/**
	 * 销售报告反馈
	 */
	@RequestMapping("/feedback")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult feedback(HttpServletRequest request, HttpServletResponse response)
	{
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String id = Tool.convertObject(parameterMap.get("id"));
		String saleReport = Tool.convertObject(parameterMap.get("saleReport"));
		String oss = Tool.convertObject(parameterMap.get("oss"));
		String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

		if(StringUtils.isEmpty(id)){
			return super.error("ID参数不能为空");
		}
		if(StringUtils.isEmpty(saleReport)){
			return super.error("销售报告不能为空");
		}
		CrmPresaleSupportInfo entity = crmPresaleSupportInfoService.selectEntityById(id);
		if(entity==null){
			return super.error("您要反馈的记录不存在");
		}

		entity.setSaleReport(saleReport);
		crmPresaleSupportInfoService.updateEntity(entity);

		/**
		 * 保存附件
		 */
		if(StringUtils.isNotEmpty(oss)){
			List<CrmPresaleSupportOss> ossDatas = new ArrayList<>();
			String[] ossArray = oss.split(",");
			for(String ossId : ossArray){
				CrmPresaleSupportOss wo = new CrmPresaleSupportOss();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setPssId(id);
				wo.setOssId(ossId);
				ossDatas.add(wo);
			}
			if(ossDatas!=null && ossDatas.size()>0){
				crmPresaleSupportOssService.batchInsertEntity(id, ossDatas);
			}
		}
		/**
		 * 添加抄送人
		 */
		if(StringUtils.isNotEmpty(ccAccounts)){
			List<CrmPresaleSupportTeam> teamDatas = new ArrayList<>();
			String[] accountArray = ccAccounts.split(",");
			for(String accountId : accountArray){
				CrmPresaleSupportTeam wo = new CrmPresaleSupportTeam();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setPssId(id);
				wo.setUserId(accountId);
				wo.setType("2");
				wo.setState("");
				teamDatas.add(wo);
			}
			if(teamDatas!=null && teamDatas.size()>0){
				crmPresaleSupportTeamService.batchInsertCcEntity(id, teamDatas);
			}
		}

		return super.ok();
	}


	/**
	 * 支持人签收
	 */
	@RequestMapping("/signFor")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult signFor(HttpServletRequest request, HttpServletResponse response)
	{
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String pssId = Tool.convertObject(parameterMap.get("pssId"));
		if(StringUtils.isEmpty(pssId)){
			return super.error("记录ID参数不能为空");
		}

		CrmPresaleSupportTeam entity = crmPresaleSupportTeamService.selectEntityByPssIdAndUserId(pssId, userId);
		if(entity==null){
			return super.error("您要签收的记录不存在");
		}
		entity.setState("1");

		crmPresaleSupportTeamService.updateEntity(entity);

		return super.ok();
	}


	/**
	 * ‘我’审批销售技术支持
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/approval")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult approval(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String pssId = Tool.convertObject(parameterMap.get("pssId"));
		int approveOpinion = Tool.convertStringToInt(parameterMap.get("approveOpinion")); //审批结论：0 同意，1 驳回
		String approveMemo = Tool.convertObject(parameterMap.get("approveMemo"));
		String proId = Tool.convertObject(parameterMap.get("proId")); //如果是销售总监审批，则要同时指定项目
		String teams = Tool.convertObject(parameterMap.get("teams")); //如果是营销总监审批，则要同时指定支持人员（多个用半角符号分隔）

		if(StringUtils.isEmpty(pssId)){
			return super.error("审批记录ID不能为空");
		}
		if(StringUtils.isEmpty(approveMemo)){
			return super.error("审批意见不能为空");
		}

		//查询审批前的销售售前技术支持信息
		CrmPresaleSupportInfo info = crmPresaleSupportInfoService.selectEntityById(pssId);
		if(info == null){
			return super.error("审批记录不存在");
		}

		if(approveOpinion==1){
			CrmPresaleSupportFlow flow = new CrmPresaleSupportFlow();
			flow.setId(SnowflakeIdUtil.getId());
			flow.setPssId(pssId);
			flow.setApproveUserId(userId);
			flow.setApproveMemo(approveMemo);
			flow.setApproveOpinion(approveOpinion);
			flow.setApproveTime(new Date());

			crmPresaleSupportFlowService.insertEntity(flow);

			/**
			 * 驳回的信息
			 * 要同时修改记录的状态并将当前审批人信息置空
			 */
			info.setApproveState(1);
			info.setApproveUserId("");
			crmPresaleSupportInfoService.updateEntity(info);

			return super.ok();
		}

		/**
		 * ********************* 以下为审批通过的处理
		 */

		String chiefFlag = "0"; //销售总监标志 0 销售， 1 销售总监
		if(info.getChiefUserId().equals(userId)){
			chiefFlag = "1";
		}
		if(chiefFlag.equals("1")){
			if(StringUtils.isEmpty(proId)){
				return super.error("项目不能为空");
			} else {
				info.setProId(proId);
			}
		}

		List<CrmPresaleSupportTeam> teamDatas = new ArrayList<>();
		String vpFlag = "0"; //营销总监标志 0 不是， 1 是
		if(info.getVpUserId().equals(userId)){
			vpFlag = "1";
		}
		if(vpFlag.equals("1")){
			if(StringUtils.isEmpty(teams)){
				return super.error("支持人员不能为空");
			}
			String[] userIdArray = teams.split(",");
			for(String uid : userIdArray){
				CrmPresaleSupportTeam wo = new CrmPresaleSupportTeam();
				wo.setId(SnowflakeIdUtil.getId());
				wo.setPssId(pssId);
				wo.setUserId(uid);
				wo.setType("1");
				wo.setState("0");
				teamDatas.add(wo);
			}
		}



		/**
		 * 处理下一节点审批人信息和状态
		 */
		String approveId = "";
		int approveState = approveOpinion;
		if(userId.equals(info.getVpUserId())){
			approveState = 4;
			approveId = "";
		}else if(userId.equals(info.getManagerUserId())){
			approveState = 3;
			approveId = info.getVpUserId();
		}else if(userId.equals(info.getChiefUserId())){
			approveState = 2;
			approveId = info.getManagerUserId();
		}

		info.setApproveState(approveState);
		info.setApproveUserId(approveId);


		CrmPresaleSupportFlow flow = new CrmPresaleSupportFlow();
		flow.setId(SnowflakeIdUtil.getId());
		flow.setPssId(pssId);
		flow.setApproveUserId(userId);
		flow.setApproveMemo(approveMemo);
		flow.setApproveOpinion(approveOpinion);
		flow.setApproveTime(new Date());

		crmPresaleSupportFlowService.insertEntity(flow);

		crmPresaleSupportInfoService.updateEntity(info);

		if(teamDatas!=null && teamDatas.size()>0){
			crmPresaleSupportTeamService.batchInsertSupportEntity(pssId, teamDatas);
		}

		/**
		 * 营销总监审批，说明流程结束
		 * 要将关联表单的审批人信息置空
		 */
		if(approveState==4){
			crmPresaleSupportInfoService.updateProcessFinishApproveInfo(pssId);
		}

		return ok();
	}

}
