package com.pms.app.project;

import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.project.domain.*;
import com.pms.core.project.service.IProBaseMybatisService;
import com.pms.core.project.service.IProBudgetCostService;
import com.pms.core.project.service.IProBudgetOtherService;
import com.pms.core.project.service.impl.*;
import com.pms.framework.web.base.AppBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 项目
 * 
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/pro")
public class ProBaseController extends AppBaseController {

	@Autowired
	private IProBaseMybatisService proBaseMybatisService;

	@Autowired
	private ProBaseService proBaseService;

	@Autowired
	private ProExpandService proExpandService;
	@Autowired
	private ProTeamService proTeamService;
	@Autowired
	private ProStakeholdersService proStakeholdersService;
	@Autowired
	private ProWbsService proWbsService;
	@Autowired
	private ProAttachmentService proAttachmentService;
	@Autowired
	private ProChangeApplyService proChangeApplyService;
	@Autowired
	private ProReceivableService proReceivableService;
	@Autowired
	private ProPactService proPactService;
	@Autowired
	private IProBudgetCostService proBudgetCostService;
	@Autowired
	private IProBudgetOtherService proBudgetOtherService;

	/**
	 * 查询所有我项目的列表
	 */
	@RequestMapping("/myProject")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult myProject(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		List<ProBaseEntity> list = proBaseMybatisService.selectList(params);

		return super.ok().put("list", list);
	}

	/**
	 * 查询所有我主导项目的列表
	 */
	@RequestMapping("/authProjectList")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult authProjectList(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		Map<String, Object> params = new HashMap<>();
		params.put("authOperateUserId", userId);
		List<ProBaseEntity> list = proBaseMybatisService.selectList(params);

		return super.ok().put("list", list);
	}

	/**
	 * 查询所有我参与的项目的列表
	 * 用于报工或提交工作流的表单
	 */
	@RequestMapping("/myJoinProjectList")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult MyJoinProjectList(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		List<ProBaseEntity> myJoinProList = new ArrayList<>();

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
					myJoinProList.add(pro);
				}
			}
		}

		return super.ok().put("list", myJoinProList);
	}

	/**
	 * 项目详细信息以及与之关联的信息
	 */
	@RequestMapping("/proDetails")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult myCalendar(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		String proId = Tool.convertObject(parameterMap.get("id"));

		ProBaseEntity pro = proBaseMybatisService.selectEntityById(proId);

		List<Map<String, Object>> teams = new ArrayList<>();
		if(pro!=null){
			teams = proTeamService.queryProTeamsByProId(pro.getId());
		}

		List<Map<String, Object>> stakeholders = new ArrayList<>();
		if(pro!=null){
			stakeholders = proStakeholdersService.queryProStakeholdersByProId(pro.getId());
		}

		List<Map<String, Object>> wbs = new ArrayList<>();
		if(pro!=null){
			wbs = proWbsService.queryProWbsByProId(pro.getId());
		}

		List<Map<String, Object>> changes = new ArrayList<>();
		if(pro!=null){
			changes = proChangeApplyService.queryProChangeApplyList(pro.getId());
		}

		ProExpandEntity proExpandEntity = null;
		if(pro != null){
			proExpandEntity = proExpandService.queryProExpandEntity(pro.getId());
		}
		if(pro!=null){
			if(proExpandEntity==null){
				proExpandEntity = new ProExpandEntity();
				proExpandEntity.setProId(pro.getId());
				proExpandEntity.setProExpensesSoft(0d);
				proExpandEntity.setProExpensesHardware(0d);
				proExpandEntity.setProExpensesCultivate(0d);
			}
			pro.setProExpand(proExpandEntity);
		}

		List<Map<String, Object>> attas = new ArrayList<>();
		if(pro!=null){
			attas = proAttachmentService.queryProAttachmentsByProId(pro.getId());
		}

		List<ProReceivableEntity> recs = new ArrayList<>();
		if(pro!=null){
			recs = proReceivableService.queryProReceivableList(pro.getId());
		}

		List<ProPactEntity> pacts = new ArrayList<>();
		if(pro!=null){
			pacts = proPactService.queryByProId(pro.getId());
		}


		List<ProBudgetCost> budgetCosts = proBudgetCostService.selectListByProId(pro.getId());

		List<ProBudgetOther> budgetOthers = proBudgetOtherService.selectListByProId(pro.getId());

		return super.ok()
				.put("pro", pro)
				.put("teams", teams)
				.put("stakeholders", stakeholders)
				.put("wbs", wbs)
				.put("changes", changes)
				.put("attas", attas)
				.put("recs", recs)
				.put("pacts", pacts)
				.put("budgetCosts", budgetCosts)
				.put("budgetOthers", budgetOthers);
	}

}
