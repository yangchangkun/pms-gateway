package com.pms.app.daily;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.attendance.domain.OaDailyPaperEntity;
import com.pms.core.attendance.service.IOaDailyPaperService;
import com.pms.core.project.domain.ProBaseEntity;
import com.pms.core.project.service.impl.ProBaseService;
import com.pms.framework.web.base.AppBaseController;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 日报审批
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/daily/approval")
public class DailyPaperApprovalController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IOaDailyPaperService oaDailyPaperService;

	@RequestMapping("/myApprovalList")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult myCalendar(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String day = Tool.convertObject(parameterMap.get("day"));
		String proId = Tool.convertObject(parameterMap.get("proId"));
		String userKey = Tool.convertObject(parameterMap.get("userKey"));

		int pageNum = Tool.convertStringToInt(parameterMap.get("pageNum"));
		if(pageNum<=0){
			pageNum = 1;
		}
		int pageSize = Tool.convertStringToInt(parameterMap.get("pageSize"));
		if(pageSize<=0 || pageSize>50){
			pageSize = 10;
		}

		String orderBy = "dp.daily_day DESC, dp.id ASC ";

		Map<String, Object> params = new HashMap<>();
		params.put("approvalId", userId);
		params.put("status", "0");
		params.put("day", day);
		params.put("proId", proId);
		params.put("userKey", userKey);
		params.put("pageNum", pageNum);
		params.put("pageSize", pageSize);

		PageHelper.startPage(pageNum, pageSize, orderBy);
		List<OaDailyPaperEntity> list = oaDailyPaperService.selectApproveDailyPaperList(params);
		PageInfo page = new PageInfo(list);
		/**
		 * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
		 */
		page.setList(new ArrayList());

		return super.ok().put("gridDatas", list).put("webPage", page);
	}


	/**
	 * 批量审批报工
	 */
	@RequestMapping("/batchApprove")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult batchApprove(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String state = Tool.convertObject(parameterMap.get("state"));
		String memo = Tool.convertObject(parameterMap.get("memo"));
		String checkArray = Tool.convertObject(parameterMap.get("checkArray"));

		oaDailyPaperService.approveDailyPaperForApp(checkArray.split(","), state, memo, userId);

		return super.ok();

	}

}
