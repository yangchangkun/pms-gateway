package com.pms.app.daily;

import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.utils.CalendarHelper;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.attendance.domain.AttendLeaveDetails;
import com.pms.core.attendance.domain.OaDailyPaperEntity;
import com.pms.core.attendance.service.IAttendLeaveDetailsService;
import com.pms.core.attendance.service.IOaDailyPaperService;
import com.pms.core.prefer.service.IPreferCalendarService;
import com.pms.core.project.domain.ProBaseEntity;
import com.pms.core.project.domain.ProRiskLevel;
import com.pms.core.project.service.IProRiskLevelService;
import com.pms.core.project.service.impl.ProBaseService;
import com.pms.core.project.service.impl.ProTeamService;
import com.pms.framework.web.base.AppBaseController;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 日报申请
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/daily/apply")
public class DailyPaperApplyController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IOaDailyPaperService oaDailyPaperService;
	@Autowired
	private IPreferCalendarService preferCalendarService;
	@Autowired
	private ProBaseService proBaseService;
	@Autowired
	private ProTeamService proTeamService;
	@Autowired
	private IAttendLeaveDetailsService attendLeaveDetailsService;
	@Autowired
	private IProRiskLevelService proRiskLevelService;

	/**
	 * 获取我的工作月历
	 * @return
	 */
	@RequestMapping("/calendars")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult myCalendar(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String year = Tool.convertObject(parameterMap.get("year"));
		String month = Tool.convertObject(parameterMap.get("month"));

		int y = Tool.convertStringToInt(year);
		int m = Tool.convertStringToInt(month);

		//查询设置为假日的日期
		Map<String, String> holidayMap = preferCalendarService.getList(y, m);

		//得到正常的日历
		JSONArray jArr = CalendarHelper.getMonthCalendar(y, m, holidayMap);

		/**
		 * 查询已经存在的日报
		 * 并将对应日期报工的状态连接起来以作后续对某个日期报工状态的判断
		 */
		List<OaDailyPaperEntity> dailyList = oaDailyPaperService.queryMyDailyPaperList(userId, year+"-"+month);
		Map<String, String> dailyMap = new HashMap<>();
		if(dailyList!=null && dailyList.size()>0){
			for(OaDailyPaperEntity entity : dailyList){
				String dailyDay = DateUtils.format(entity.getDailyDay());
				int  status = entity.getStatus();
				if(dailyMap.containsKey(dailyDay)){
					String _status = dailyMap.get(dailyDay);
					_status = _status + "" + status;
					dailyMap.put(dailyDay, _status+"");
				} else {
					dailyMap.put(dailyDay, status+"");
				}
			}
		}
		/**
		 * 处理与日历对应的工时情况
		 * state -1未报工，0：审核中，1：驳回，2：确认
		 */
		JSONArray resultJArr = new JSONArray();
		try{
			if(jArr!=null && jArr.length()>0){
				for(int i=0;i<jArr.length();i++){
					JSONObject monthJo = jArr.getJSONObject(i);
					if(monthJo!=null){
						JSONArray weekJArr = monthJo.getJSONArray("week");
						if(weekJArr!=null && weekJArr.length()>0){

							JSONArray week = new JSONArray();

							for(int j=0;j<weekJArr.length();j++){

								JSONObject weekJo = weekJArr.getJSONObject(j);
								String _year = weekJo.isNull("year")?"":weekJo.getString("year");
								String _month = weekJo.isNull("month")?"":weekJo.getString("month");
								String _day = weekJo.isNull("day")?"":weekJo.getString("day");
								String _date = weekJo.isNull("date")?"":weekJo.getString("date");
								String _display = weekJo.isNull("display")?"":weekJo.getString("display");//是否显示日期，0 不显示，1显示
								String _holiday = weekJo.isNull("holiday")?"1":weekJo.getString("holiday");//是否是假日(周末)，0是，1否
								String _state = "-1"; //-1未报工，0：审核中，1：驳回，2：确认

								if(dailyMap.containsKey(_date)){
									String status = dailyMap.get(_date);
									if(status.indexOf("1")!=-1){
										_state = "1";
									} else if(status.indexOf("0")!=-1){
										_state = "0";
									} else if(status.indexOf("2")!=-1){
										_state = "2";
									}
								}
								weekJo.put("state", _state); //是否是假日(周末)，0是，1否
								week.put(weekJo);
							}

							JSONObject weeksJo = new JSONObject();
							weeksJo.put("week", week);
							resultJArr.put(weeksJo);
						}

					}
				}
			}
		} catch (Exception e){
			e.printStackTrace();
			logger.error(TAG, e);
			return super.error(e.getMessage());
		}

		return super.ok().put("list", resultJArr.toString());
	}


	/**
	 * 某日日报初始化
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/dayDailyInit")
	@ResponseBody
	public AjaxResult dayDailyInit(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String day = Tool.convertObject(parameterMap.get("day"));

		String currDay = DateUtils.getCurrentDay();
		if(DateUtils.dateSubtraction(currDay, day, DateUtils.DATE_PATTERN)>0){
			return super.error("未到报工日期");
		}
		//判断当天是否有请假
		List<AttendLeaveDetails> list =  attendLeaveDetailsService.selectDetailsByDate(userId,day);
		if(list != null && list.size()>0){
			for(AttendLeaveDetails entity : list){
				int duration = entity.getLeaveDuration();
				String state = entity.getLeaveState();
				if (duration >= 8 && state.equals("0")){
					return super.error("当天有请假，审批通过后系统自动报工");
				}
			}
		}

		Date dayDate = DateUtils.conversion(day);

		//查询设置为假日的日期
		int y = Tool.convertStringToInt(day.split("-")[0]);
		Map<String, String> holidayMap = preferCalendarService.getHolidayList(y);

		/**
		 * 查询当前账号参与的项目
		 */
		List<ProBaseEntity> joiningList = proBaseService.queryMyJoiningProList(userId);
		/**
		 * 分析我参与的项目中可用于报工的项目
		 */
		List<ProBaseEntity> proList = new ArrayList<>();
		if(joiningList!=null && joiningList.size()>0){
			for(ProBaseEntity pro : joiningList) {
				Date proCreateDay = pro.getProCreateDay();
				Date proEndDay = pro.getProEndDay();

				/**
				 * 查询我在当前这个项目的具体情况
				 */
				Map<String, Object> myProMap = proTeamService.queryProTeamsByProIdAndUserId(pro.getId(), userId);
				Date joinDate = DateUtils.conversion(Tool.convertObject(myProMap.get("joinDate")));
				Date departureDate = DateUtils.conversion(Tool.convertObject(myProMap.get("departureDate")));

				if(holidayMap!=null && holidayMap.containsKey(day)){
					//节假日

				} else if(dayDate.getTime()<proCreateDay.getTime() || dayDate.getTime()>proEndDay.getTime() ){
					//项目在当前日期（day）未开始或已结束

				} else if(dayDate.getTime()<joinDate.getTime() || dayDate.getTime()>departureDate.getTime()){
					//当前日期（day）未加入项目组

				} else {
					proList.add(pro);
				}
			}
		}

		List<OaDailyPaperEntity> dailyList = new ArrayList<>();

		/**
		 * 查询指定日期已经提交的日报
		 * 如果当日存在报工记录，则默认打开的是已经报工的项目
		 * 如果当日不存在报工，则默认打卡上一次报工的项目
		 */
		List<OaDailyPaperEntity> dailyRecord = oaDailyPaperService.queryMyDailyPaperListByDay(userId, day);
		if(dailyRecord!=null && dailyRecord.size()>0){
			/**
			 * 先将已经报工的项目加入最终的列表
			 * 默认是打开的状态
			 */
			for (OaDailyPaperEntity daily : dailyRecord) {
				daily.setDailySwitch(0);
				dailyList.add(daily);
			}

			/**
			 * 再将已经加入可报工但是却没有报工的项目加入日报的列表
			 * 默认状态是关闭
			 */
			if(proList!=null && proList.size()>0) {
				for (ProBaseEntity pro : proList) {
					int dailySwitch = 1; //报工开关 0 报工，1 关闭
					for (OaDailyPaperEntity daily : dailyRecord) {
						if (daily.getProId().equals(pro.getId())) {
							dailySwitch = 0;
						}
					}

					if(dailySwitch==1){
						OaDailyPaperEntity entity = new OaDailyPaperEntity();
						entity.setId(SnowflakeIdUtil.getId());
						entity.setProId(pro.getId());
						entity.setDepartmentId(pro.getProDepartId());
						entity.setDailyDay(dayDate);
						entity.setHoursApply(8);
						entity.setHourlyWage(0d);
						entity.setJobContent("");
						entity.setStatus(-1);
						entity.setUrgeStatus(0);
						entity.setMemo("");
						entity.setWorkingPlace("");
						entity.setDailySource(1);
						entity.setApplyUserId(userId);
						entity.setCreateTime(new Date());

						entity.setApproveUserId("");
						entity.setLastUpdateTime(new Date());

						entity.setProName(pro.getProName());
						entity.setDepartmentName(pro.getProDepartName());

						entity.setHoursProductRatio(0);

						entity.setDailySwitch(1);
						dailyList.add(entity);
					}
				}
			}

		} else {
			/**
			 * 查询指定日期前一天已经提交的日报
			 */
			List<OaDailyPaperEntity> tempDailyList = oaDailyPaperService.queryDailyPaperByDay(userId);

			if(proList!=null && proList.size()>0) {
				for (int i=0;i<proList.size();i++) {
					ProBaseEntity pro = proList.get(i);

					int dailySwitch = 1; //报工开关 0 报工，1 关闭
					for (OaDailyPaperEntity daily : tempDailyList) {
						if (daily.getProId().equals(pro.getId())) {
							dailySwitch = 0;
						}
					}


					OaDailyPaperEntity entity = new OaDailyPaperEntity();
					entity.setId(SnowflakeIdUtil.getId());
					entity.setProId(pro.getId());
					entity.setDepartmentId(pro.getProDepartId());
					entity.setDailyDay(dayDate);
					entity.setHoursApply(8);
					entity.setHourlyWage(0d);
					entity.setJobContent("");
					entity.setStatus(-1);
					entity.setUrgeStatus(0);
					entity.setMemo("");
					entity.setWorkingPlace("");
					entity.setDailySource(1);
					entity.setApplyUserId(userId);
					entity.setCreateTime(new Date());

					entity.setApproveUserId("");
					entity.setLastUpdateTime(new Date());

					entity.setProName(pro.getProName());
					entity.setDepartmentName(pro.getProDepartName());

					entity.setHoursProductRatio(0);

					entity.setDailySwitch(dailySwitch);
					dailyList.add(entity);
				}
			}

		}

		/**
		 * 按照报工开关排序
		 */
		if(dailyList!=null && dailyList.size()>1){
			Comparator<OaDailyPaperEntity> comparator = new Comparator<OaDailyPaperEntity>() {
				@Override
				public int compare(OaDailyPaperEntity o1, OaDailyPaperEntity o2) {
					if(o1.getDailySwitch()<o2.getDailySwitch()){
						return 1;
					}
					return 0;
				}
			};
			Collections.sort(dailyList,comparator);
		}

		return super.ok().put("proList", proList).put("dailyList", dailyList);
	}

	/**
	 * 保存或者被驳回的工时重新提交
	 * @param request
	 * @return
	 */
	@RequestMapping("/saveDaily")
	@ResponseBody
	public AjaxResult saveDaily(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String day = Tool.convertObject(request.getParameter("day"));
		if(StringUtils.isBlank(day)){
			return super.error("未能确定你要报工的日期");
		}

		String dailyJArr = Tool.convertObject(request.getParameter("dailyJArr"));
		if(StringUtils.isBlank(dailyJArr)){
			return super.error("未提交工时");
		}


		Map<String, String> map = new HashMap<>(); //用于去重

		int totalHours = 0;

		List<OaDailyPaperEntity> list = new ArrayList<>();
		try{
			JSONArray jArr = new JSONArray(dailyJArr);
			if(jArr!=null && jArr.length()>0){
				for(int i=0;i<jArr.length();i++){
					JSONObject jo = jArr.getJSONObject(i);
					String id = jo.isNull("id")?"":jo.getString("id");
					String proId = jo.isNull("proId")?"":jo.getString("proId");
					String proName = jo.isNull("proName")?"":jo.getString("proName");
					String departmentId = jo.isNull("departmentId")?"":jo.getString("departmentId");
					String dailyDay = jo.isNull("dailyDay")?"":jo.getString("dailyDay");
					int hoursApply = jo.isNull("hoursApply")?0:jo.getInt("hoursApply");
					String jobContent = jo.isNull("jobContent")?"":jo.getString("jobContent");
					int status = jo.isNull("status")?0:jo.getInt("status");
					if(status<=0){
						status = 0;
					}
					/**
					 * 如果不是2（已确认）的日报提交都要设置为0
					 */
					if(status!=2){
						status = 0;
					}

					String memo = jo.isNull("memo")?"":jo.getString("memo");

					int hoursProductRatio = jo.isNull("hoursProductRatio")?0:jo.getInt("hoursProductRatio");
					String workingPlace = jo.isNull("workingPlace")?"":jo.getString("workingPlace");
					int dailySource = jo.isNull("dailySource")?1:jo.getInt("dailySource");
					/**
					 * 以上属性为报工共有
					 * **************************
					 * 以下属性一般为修改时才有
					 */
					String applyUserId = jo.isNull("applyUserId")?"":jo.getString("applyUserId");
					if(StringUtils.isBlank(applyUserId)){
						applyUserId = userId;
					}
					String createTime = jo.isNull("createTime")?"":jo.getString("createTime");
					if(StringUtils.isBlank(createTime)){
						createTime = DateUtils.getCurAllTime();
					}
					String approveUserId = jo.isNull("approveUserId")?"":jo.getString("approveUserId");
					String lastUpdateTime = jo.isNull("lastUpdateTime")?"":jo.getString("lastUpdateTime");
					if(StringUtils.isBlank(lastUpdateTime)){
						lastUpdateTime = DateUtils.getCurAllTime();
					}

					// 报工开关 0 报工，1 关闭
					int dailySwitch = jo.isNull("dailySwitch")?1:jo.getInt("dailySwitch");

					if(dailySwitch==1){
						continue;
					}
					if(StringUtils.isEmpty(jobContent)){
						return super.error("请填写报工的内容.");
					}

					if(hoursApply>=0){
						totalHours+=hoursApply;
					}

					String key = applyUserId+proId+dailyDay;
					if(map.containsKey(key)){
						continue;
					}
					map.put(key, key);
					/**
					 * 计算每个项目（风险等级表中）是否因为风险等级高二被关闭，关闭了就不让报工
					 */
					ProRiskLevel proRiskLevel = proRiskLevelService.selectEntityById(proId);
					if(proRiskLevel!=null && proRiskLevel.getStatus() == 1){
						return super.error("'"+proName+"'该项目因成本高于预算,暂时关闭!请通知项目经理进行预算变更");
					}
					OaDailyPaperEntity entity = new OaDailyPaperEntity();
					entity.setId(SnowflakeIdUtil.getId());
					entity.setProId(proId);
					entity.setDepartmentId(departmentId);
					entity.setDailyDay(DateUtils.conversion(dailyDay));
					entity.setHoursApply(hoursApply);
					entity.setHourlyWage(0d);
					entity.setJobContent(jobContent);
					entity.setStatus(status);
					entity.setUrgeStatus(0);
					entity.setMemo(memo);
					entity.setHoursProductRatio(hoursProductRatio);
					entity.setWorkingPlace(workingPlace);
					entity.setDailySource(dailySource);
					entity.setApplyUserId(applyUserId);
					entity.setCreateTime(DateUtils.conversion(createTime, DateUtils.DATE_TIME_PATTERN));
					if(!StringUtils.isBlank(approveUserId)){
						entity.setApproveUserId(approveUserId);
						entity.setLastUpdateTime(DateUtils.conversion(lastUpdateTime, DateUtils.DATE_TIME_PATTERN));
					}
					list.add(entity);
				}
			}

			/**
			 * 计算请假占用的工时
			 */
			int leaveTime = oaDailyPaperService.calculateTime(userId,day);

			int day_available_duration = 8;
			if(totalHours>day_available_duration){
				return super.error("您当日的工时已经超过8小时的可用工时.");
			}
			if(totalHours>leaveTime){
				return super.error("您当日的工时已经超过当天可用工时，请注意您当天的请假情况");
			}
			if(list!=null && list.size()>0){
				//删除当日此用户所有的工时并重新保存
				oaDailyPaperService.operateDaily(userId, day,list);
			}
		} catch (Exception e){
			e.printStackTrace();
			logger.error(TAG, e);
			return super.error(e.getMessage());
		}


		return super.ok();
	}


	/**
	 * 查询日工时详情
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/dayDetails")
	@ResponseBody
	public AjaxResult dayDetails(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String day = Tool.convertObject(parameterMap.get("day"));

		/**
		 * 查询指定日期已经提交的日报
		 */
		List<OaDailyPaperEntity> dailyList = oaDailyPaperService.queryMyDailyPaperListByDay(userId, day);

		int availableDuration = 0;
		if(dailyList!=null && dailyList.size()>0){
			for(OaDailyPaperEntity entity : dailyList){
				availableDuration += entity.getHoursApply();
			}
		}



		String tips = "您目前已经提报工时:"+availableDuration;

		return super.ok().put("dailyList", dailyList).put("availableDuration", availableDuration).put("tips", tips);
	}

	/**
	 * 根据ID查询具体的工时
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/details")
	@ResponseBody
	public AjaxResult details(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String id = Tool.convertObject(parameterMap.get("id"));

		OaDailyPaperEntity bean = oaDailyPaperService.getOaDailyPaperEntity(id);

		int day_available_duration = 8;

		bean.setAvailableDuration(day_available_duration);

		return super.ok().put("bean", bean);
	}



	/**
	 * 删除
	 */
	@RequestMapping("/delete")
	@ResponseBody
	public AjaxResult delete(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String day = Tool.convertObject(parameterMap.get("day"));
		String id = Tool.convertObject(parameterMap.get("id"));

		oaDailyPaperService.deleteByIds(id);

		return super.ok();
	}
}
