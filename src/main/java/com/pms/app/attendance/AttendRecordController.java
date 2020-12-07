package com.pms.app.attendance;

import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.utils.*;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.attendance.domain.AttendGroup;
import com.pms.core.attendance.domain.AttendRecordDaily;
import com.pms.core.attendance.domain.AttendRecordDetail;
import com.pms.core.attendance.service.IAttendGroupAddressService;
import com.pms.core.attendance.service.IAttendGroupService;
import com.pms.core.attendance.service.IAttendRecordDailyService;
import com.pms.core.attendance.service.IAttendRecordDetailService;
import com.pms.core.prefer.service.IPreferCalendarService;
import com.pms.framework.web.base.AppBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 考勤打卡
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/attend/record")
public class AttendRecordController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IAttendRecordDetailService attendRecordDetailService;
	@Autowired
	private IAttendRecordDailyService attendRecordDailyService;

	@Autowired
	private IAttendGroupService attendGroupService;
	@Autowired
	private IAttendGroupAddressService attendGroupAddressService;

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
		Double lng = Tool.convertStringToDouble(parameterMap.get("lng"));
		Double lat = Tool.convertStringToDouble(parameterMap.get("lat"));
		if(lng<=0 || lat<=0) {
			return super.error("未能获取您的经纬度");
		}

		String today = DateUtils.getCurrentDay();

		/**
		 * 查询职员已经加入的考勤组
		 */
		List<AttendGroup> myGroupList = attendGroupService.selectMyGroupList(userId);
		if(myGroupList==null || myGroupList.size()<=0){
			return super.error("您尚未加入任何考勤组,请联系管理员为您进行设置");
		}

		/**
		 * 上班打卡记录
		 */
		AttendRecordDetail signInEntity = new AttendRecordDetail();
		signInEntity.setId("");
		signInEntity.setUserId(userId);
		signInEntity.setAttendGroupId("");
		signInEntity.setAttendGroupName("");
		signInEntity.setAttendTime(null);
		signInEntity.setAttendLng(null);
		signInEntity.setAttendLat(null);
		signInEntity.setAttendAddr("");
		signInEntity.setFingerPrint("");

		/**
		 * 下班打卡记录
		 */
		AttendRecordDetail signOutEntity = new AttendRecordDetail();
		signOutEntity.setId("");
		signOutEntity.setUserId(userId);
		signOutEntity.setAttendGroupId("");
		signOutEntity.setAttendGroupName("");
		signOutEntity.setAttendTime(null);
		signOutEntity.setAttendLng(null);
		signOutEntity.setAttendLat(null);
		signOutEntity.setAttendAddr("");
		signOutEntity.setFingerPrint("");

		/**
		 * 获取当日打卡记录
		 * 并分析出上班打卡和下班打卡
		 */
		List<AttendRecordDetail> detailList = attendRecordDetailService.selectTodayList(userId, today);
		if(detailList!=null && detailList.size()>0){
			AttendRecordDetail fstDetail = detailList.get(0); //第一条打卡记录
			signInEntity = fstDetail;
		}
		if(detailList!=null && detailList.size()>1){
			AttendRecordDetail lstDetail = detailList.get(detailList.size()-1); //最后一条打卡记录
			signOutEntity = lstDetail;
		}


		boolean allowTag = false; //允许打卡标识
		boolean allowSpeedTag = false; //允许极速打卡标识
		boolean isOutTag = false;//外勤标识
		boolean afreshTag = false;//允许刷新上班打卡的标志
		AttendGroup actGroup = null; //实际的考勤组(如果没进入考勤点范围，则为系统设置的考勤组，否则为实际进入范围的考勤组)
		AttendGroup hitGroup = null; //命中的考勤组(系统设置的考勤组)
		List<Map<String, Object>> pointList = new ArrayList<>(); //考勤组对应的打卡范围
		String attendAdress = ""; //如果进入打卡范围，则这里为范围内设定的打卡地址的名称
		String tips = "未能定位您的位置或您未进入打卡范围"; //打卡的提示

		/**
		 * 取出第一条考勤组作为当前用户的默认考勤组
		 */
		if(myGroupList!=null && myGroupList.size()>0){
			hitGroup = myGroupList.get(0);
			actGroup = myGroupList.get(0);
			/**
			 * 如果考勤组未关联地址，说明是外勤打卡
			 */
			if(hitGroup.getAddrCnt()<=0){
				allowTag = true;
				isOutTag = true;
				tips = "可以在任意范围内打卡";
			}
		}

		/**
		 * 是否命中了范围内的考勤点，如果命中要跳出整个嵌套的循环
		 * 这里主要是解决在多个有考勤点的考勤组，如果不结束全部的循环，则会找到最后一个考勤组,这会导致优先级的问题
		 * 比如某人在特殊考勤组，默认又在总部考勤组，只要找到特殊考勤组就要跳出循环，不继续判断总部考勤组
		 */
		boolean isHit = false; //是否命中了考勤范围内考勤点
		/**
		 * 判断当前是否处于打卡范围内
		 * 如果在范围内，满足时间的条件，则认为可以极速打卡，
		 * 并且就算是外勤组，只要进入打卡范围内，就不能算外勤标识。
		 * 注意，这里只判断考勤范围，不设置考勤组
		 */
		for(int i=0;i<myGroupList.size()&&!isHit;i++){
			AttendGroup group = myGroupList.get(i);
			pointList = attendGroupAddressService.selectAddressByGpsAndGroupId(group.getGroupId(), lng, lat);
			if(pointList!=null && pointList.size()>0){
				for(Map<String, Object> map : pointList){
					String name = Tool.convertObject(map.get("name"));
					Double radius = Tool.convertStringToDouble(map.get("radius"));
					Double distance = Tool.convertStringToDouble(map.get("distance"));
					if(distance <= radius){
						isHit = true;
						actGroup = group;

						allowTag = true;
						isOutTag = false;
						attendAdress = name;
						tips = "您已进入打卡范围:" + name;

						Date now = DateUtils.getNowDate();
						String time = DateUtils.format(now, DateUtils.HHMM);
						int currH = Tool.convertStringToInt(time.replaceAll(":", ""));
						if(currH>=730 && currH<=930){
							allowSpeedTag = true;
						}
						break;
					}
				}
			}
		}

		/**
		 * 如果当日有上班打卡记录，显示“更新打卡”，点击更新打卡，更新上班卡。
		 * （注意，只在正常的上班班次时间内可以更新打卡）
		 */
		if(allowTag && actGroup!=null && StringUtils.isNotEmpty(signInEntity.getId())){
			Date signElasticTime = DateUtils.conversion((today+" "+actGroup.getSignElasticTime()), DateUtils.YYYY_MM_DD_HH_MM);
			if(DateUtils.dateSubtraction(new Date(), signElasticTime)>0){
				afreshTag = true;
			}
		}

		/**
		 * 查询当日已经打卡的考勤记录
		 */
		AttendRecordDaily daily = null;
		List<AttendRecordDaily> dailyList = attendRecordDailyService.selectTodayList(userId, today);
		if(dailyList!=null && dailyList.size()>0){
			daily = dailyList.get(0);
			if(daily!=null){
				/**
				 * todo 注意这里的特殊处理，如果下班时间超过18:00，就都认为下班正常
				 */
				if(daily.getSignOutTime()!=null){
					String time = DateUtils.format(daily.getSignOutTime(), DateUtils.HHMM);
					int currH = Tool.convertStringToInt(time.replaceAll(":", ""));
					if(currH>1800){
						daily.setSignOutType("0");
					}
				}

			}
		}

		return super.ok()
				.put("today", today)
				.put("allowTag", allowTag)
				.put("allowSpeedTag", allowSpeedTag)
				.put("isOutTag", isOutTag)
				.put("afreshTag", afreshTag)
				.put("tips", tips)
				.put("hitGroup", hitGroup)
				.put("actGroup", actGroup)
				.put("pointList", pointList)
				.put("attendAdress", attendAdress)
				.put("signInEntity", signInEntity)
				.put("signOutEntity", signOutEntity)
				.put("daily", daily);
	}

	/**
	 * 打卡
	 * @return
	 */
	@RequestMapping("/sign")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult sign(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String fingerPrint = Tool.convertObject(parameterMap.get("fingerPrint"));
		Double lng = Tool.convertStringToDouble(parameterMap.get("lng"));
		Double lat = Tool.convertStringToDouble(parameterMap.get("lat"));
		String addr = Tool.convertObject(parameterMap.get("addr"));

		Date signTime = new Date();

		return attendRecordDailyService.attendanceSign(userId, signTime, lng, lat, addr, fingerPrint);
	}


	/**
	 * 更新上班打卡
	 * @return
	 */
	@RequestMapping("/afreshInSign")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult afreshInSign(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);

		String fingerPrint = Tool.convertObject(parameterMap.get("fingerPrint"));
		Double lng = Tool.convertStringToDouble(parameterMap.get("lng"));
		Double lat = Tool.convertStringToDouble(parameterMap.get("lat"));
		String addr = Tool.convertObject(parameterMap.get("addr"));

		Date signTime = new Date();

		return attendRecordDailyService.afreshInSign(userId, signTime, lng, lat, addr, fingerPrint);
	}

}
