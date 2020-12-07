package com.pms.app.attendance;

import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.attendance.domain.*;
import com.pms.core.attendance.service.*;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.prefer.service.IPreferCalendarService;
import com.pms.core.project.domain.ProBaseEntity;
import com.pms.core.project.service.IProBaseMybatisService;
import com.pms.core.system.domain.SysDept;
import com.pms.core.system.domain.SysUser;
import com.pms.core.system.service.ISysDeptService;
import com.pms.core.system.service.ISysUserService;
import com.pms.framework.web.base.AppBaseController;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 考勤统计
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/attend/report")
public class AttendReportController extends AppBaseController {
	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private IAttendReportService attendReportService;

	@Autowired
	private ISysUserService sysUserService;

	@Autowired
	IProBaseMybatisService proBaseMybatisService;

	/**
	 * 个人出勤情况月汇总
	 * @return
	 */
	@RequestMapping("/summary")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult summary(HttpServletRequest request, HttpServletResponse response){
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		/**
		 * 先从请求参数里面取userId，如果参数里面userId有值说明要查询指定员工的考勤
		 * 否则就从token里面取值，这个时候查询的是当前账号人员的考勤
		 */
		String userId = Tool.convertObject(parameterMap.get("userId"));
		if(StringUtils.isEmpty(userId)){
			userId = getAppUserId(parameterMap);
		}

		String month = Tool.convertObject(parameterMap.get("month"));

		if(StringUtils.isEmpty(month)){
			return super.error("请选择您要查看的月份");
		}

		String today = DateUtils.getCurrentDay();

		SysUser userEntity = sysUserService.selectUserById(userId);

		List<Map<String, Object>> attendList = attendReportService.selectMonthAttendListByUserId(userId, month);


		int shouldDays = 0; //应出勤天数(即 工作日)
		double actualDays = 0d; //实际出勤天数
		int qqCount = 0; //缺勤次数
		int qkCount = 0; //缺卡次数
		int cdCount = 0; //迟到次数
		int kgCount = 0; //旷工次数
		int ztCount = 0; //早退次数

		double egressHours = 0d; //外出时长（小时）
		double leaveDays = 0d; //请假天数（小时）
		double travelDays= 0d; //出差天数（天）
		double overtimeHours = 0d; //加班时长（小时）
		int bkCount = 0; //补卡次数

		/**
		 * 考勤快照
		 * map.key:
		 * day:日期
		 * dayType:日期类型，0 节假日、1 工作日
		 * diffDay: 考勤日和当前日期相差的天数，如果小于0说明未到考勤日期
		 * inTime：上班班次时间
		 * outTime：下班班次时间
		 * inLabel： 上班班次描述：缺卡、正常、迟到、            、旷工
		 * outLabel：下班班次描述：缺卡、正常、早退/	缺勤、	早退、旷工
		 * inTag： 上班标签(多个用逗号分隔)：加班(节假日),请假,外出,出差,外勤,补卡
		 * outTag：下班标签(多个用逗号分隔)：加班(节假日),请假,外出,出差,外勤,补卡
		 * inAddr： 上班打卡地址
		 * outAddr: 下班打卡地址
		 * workHours：工时
		 * ps：
		 * 旷工 上下班都没有打卡，且无任何请假和外出记录
		 * 加班 只有节假日才显示加班
		 * 出差 依然需要打卡，按照正常的考勤计算
		 */
		List<Map<String, Object>> attendSnapshotList = new ArrayList<>();

		if(attendList!=null && attendList.size()>0){
			for(Map<String, Object> map : attendList){
				String day = Tool.convertObject(map.get("day")); //日期
				String type = Tool.convertObject(map.get("type")); //日期类型，0假日, 1 正常，
				Date signInTime = DateUtils.conversion(Tool.convertObject(map.get("signInTime")), DateUtils.YYYY_MM_DD_HH_MM_SS); //上班打卡时间
				String signInType = Tool.convertObject(map.get("signInType")); //上班打卡类型：0正常；1迟到；3旷工
				Date signOutTime = DateUtils.conversion(Tool.convertObject(map.get("signOutTime")), DateUtils.YYYY_MM_DD_HH_MM_SS); //下班打卡时间
				String signOutType = Tool.convertObject(map.get("signOutType")); //下班打卡类型：0正常；1早退；2缺勤
				double workHours = Tool.convertStringToDouble(map.get("workHours")); //上下班打卡时间差

				int diffDay = Tool.convertStringToInt(map.get("diffDay")); //考勤日和当前日期相差的天数，如果小于0说明未到考勤日期

				String inId = Tool.convertObject(map.get("inId"));
				Integer inAddrCnt = Tool.convertStringToInt(map.get("inAddrCnt")); //上班考勤组对应的地址数量，如果为0，说明是外勤
				String inAddr = Tool.convertObject(map.get("inAddr"));
				String outId = Tool.convertObject(map.get("outId"));
				Integer outAddrCnt = Tool.convertStringToInt(map.get("outAddrCnt"));//下班考勤组对应的地址数量，如果为0，说明是外勤
				String outAddr = Tool.convertObject(map.get("outAddr"));
				if(inId.equals(outId)){
					//如果上下班是同一条记录，则说明只有上班记录，没有下班记录
					outAddr = "";
				}

				Date egressStartTime = DateUtils.conversion(Tool.convertObject(map.get("egressStartTime")), DateUtils.YYYY_MM_DD_HH_MM_SS); //外出开始时间（精确到小时）
				Date egressEndTime = DateUtils.conversion(Tool.convertObject(map.get("egressEndTime")), DateUtils.YYYY_MM_DD_HH_MM_SS); //外出结束时间（精确到小时）
				double egressDuration = Tool.convertStringToDouble(map.get("egressDuration")); //实际外出时长，单位小时，4：半天，8：全天
				egressHours += egressDuration;

				Date leaveStartTime = DateUtils.conversion(Tool.convertObject(map.get("leaveStartTime")), DateUtils.YYYY_MM_DD_HH_MM_SS); //请假开始时间（精确到小时）
				Date leaveEndTime = DateUtils.conversion(Tool.convertObject(map.get("leaveEndTime")), DateUtils.YYYY_MM_DD_HH_MM_SS); //请假结束时间（精确到小时）
				double leaveDuration = Tool.convertStringToDouble(map.get("leaveDuration")); //实际请假时长，单位小时，4：半天，8：全天
				if(leaveDuration>=8d){
					leaveDays+=1d;
				} else if(leaveDuration>=4d){
					leaveDays+=0.5d;
				}

				Date overtimeStartTime = DateUtils.conversion(Tool.convertObject(map.get("overtimeStartTime")), DateUtils.YYYY_MM_DD_HH_MM_SS); //加班开始时间（精确到小时）
				Date overtimeEndTime = DateUtils.conversion(Tool.convertObject(map.get("overtimeEndTime")), DateUtils.YYYY_MM_DD_HH_MM_SS); //加班开始时间（精确到小时）
				double overtimeDuration = Tool.convertStringToDouble(map.get("overtimeDuration")); //实际加班时长，单位小时
				if(overtimeDuration>=9d){
					overtimeDuration = 9d;
				} else if(overtimeDuration>=4.5d){
					overtimeDuration = 4.5d;
				}
				if(type.equals("0")){ //节假日才算加班
					overtimeHours += overtimeDuration;
				}

				String travelDay = Tool.convertObject(map.get("travelDay")); //出差日期
				if(StringUtils.isNotEmpty(travelDay)){
					travelDays += 1;
				}

				String supplementDay = Tool.convertObject(map.get("supplementDay")); //补卡日期
				Integer sInCnt = Tool.convertStringToInt(map.get("sInCnt"));//上班补卡次数
				Integer sOutCnt = Tool.convertStringToInt(map.get("sOutCnt"));//下班补卡次数
				if(sInCnt>0){
					bkCount += sInCnt;
				}
				if(sOutCnt>0){
					bkCount += sOutCnt;
				}

				Date signNormalTime = DateUtils.conversion((day+" 09:00"), DateUtils.YYYY_MM_DD_HH_MM); //正常上班时间边界
				Date signElasticTime = DateUtils.conversion((day+" 09:30"), DateUtils.YYYY_MM_DD_HH_MM); //最晚上班时间边界
				Date outSettingTime = DateUtils.conversion((day+" 18:00"), DateUtils.YYYY_MM_DD_HH_MM); //正常下班时间边界
				Date dutyTime = DateUtils.conversion((day+" 14:00"), DateUtils.YYYY_MM_DD_HH_MM); //上下午班次边界时间
				/**
				 * 设定的正常的工作时长(小时)
				 * 一般情况下：
				 * signNormalTime = 09:00
				 * signElasticTime = 09:30
				 * outTime = 18:00；特勤组 17:00
				 * defineWorkHours = 9 ；特勤组 8
				 */
				double defineWorkHours = DateUtils.dateDiffHoursDefinite(signNormalTime, outSettingTime); //注意这里包括中午休息的一个小时


				/**
				 * 开始分析***************************************
				 */
				if(type.equals("1") && diffDay>=0){
					shouldDays += 1;
				}

				if(diffDay<0){ //未到考勤日
					Map<String, Object> attendSnapshotMap = new HashMap<>();
					attendSnapshotMap.put("day", day);
					attendSnapshotMap.put("dayType", type);
					attendSnapshotMap.put("diffDay", diffDay);
					attendSnapshotMap.put("inTime", "");
					attendSnapshotMap.put("outTime", "");
					attendSnapshotMap.put("inLabel", "--");
					attendSnapshotMap.put("outLabel", "--");
					attendSnapshotMap.put("inTag", "");
					attendSnapshotMap.put("outTag", "");
					attendSnapshotMap.put("workHours", "--");
					attendSnapshotList.add(attendSnapshotMap);
					continue;
				}

				/**
				 * 分析上下班标签
				 */
				String inTag =""; //上班标签(多个用逗号分隔)：加班(节假日),请假,外出,出差,外勤
				String outTag =""; //下班标签(多个用逗号分隔)：加班(节假日),请假,外出,出差,外勤
				if(type.equals("0")){
					if(overtimeStartTime!=null && DateUtils.dateSubtraction(dutyTime, overtimeStartTime)<0){
						inTag += "加班,";
					}
					if(overtimeEndTime!=null && DateUtils.dateSubtraction(dutyTime, overtimeEndTime)>0){
						outTag += "加班,";
					}
				}

				if(leaveStartTime!=null && DateUtils.dateSubtraction(dutyTime, leaveStartTime)<0){
					inTag += "请假,";
				}
				if(leaveEndTime!=null && DateUtils.dateSubtraction(dutyTime, leaveEndTime)>0){
					outTag += "请假,";
				}

				if(egressStartTime!=null && DateUtils.dateSubtraction(dutyTime, egressStartTime)<0){
					inTag += "外出,";
				}
				if(egressEndTime!=null && DateUtils.dateSubtraction(dutyTime, egressEndTime)>0){
					outTag += "外出,";
				}

				if(StringUtils.isNotEmpty(travelDay)){
					inTag += "出差,";
					outTag += "出差,";
				}

				if(StringUtils.isNotEmpty(signInType) && inAddrCnt==0){
					inTag += "外勤,";
				}
				if(StringUtils.isNotEmpty(signOutType) && outAddrCnt==0){
					outTag += "外勤,";
				}

				if(sInCnt>0){
					inTag += "补卡,";
				}
				if(sOutCnt>0){
					outTag += "补卡,";
				}

				if(StringUtils.isNotEmpty(inTag)){
					inTag = inTag.substring(0, inTag.length()-1);
				}
				if(StringUtils.isNotEmpty(outTag)){
					outTag = outTag.substring(0, outTag.length()-1);
				}



				/**
				 * **********************************分析上班考勤情况
				 */
				Date inTime = signInTime;
				String inLabel = "缺卡";
				if(StringUtils.isNotEmpty(signInType)){
					if(signInType.equals("0")){
						inLabel = "正常";
					} else if(signInType.equals("1")){
						inLabel = "迟到";
					} else if(signInType.equals("3")){
						inLabel = "旷工";
					}
				}
				if(type.equals("0")){ //节假日考勤默认正常
					inLabel = "正常";
				}

				/**
				 * 如果常规上班打卡不正常
				 * 则继续分析外出和请假
				 */
				if(!inLabel.equals("正常")){
					if(leaveStartTime!=null){
						if(inTime==null){
							inTime = leaveStartTime;
						} else {
							if(DateUtils.dateSubtraction(leaveStartTime, inTime)>0){
								inTime = leaveStartTime;
							}
						}

						if (DateUtils.dateSubtraction(dutyTime, inTime)>0){
							inLabel = "旷工";
						} else if(DateUtils.dateSubtraction(signElasticTime, inTime)>0){
							inLabel = "迟到";
						} else {
							inLabel = "正常";
						}
					}
				}
				if(!inLabel.equals("正常")){
					if(egressStartTime!=null){
						if(inTime==null){
							inTime = egressStartTime;
						} else {
							if(DateUtils.dateSubtraction(egressStartTime, inTime)>0){
								inTime = egressStartTime;
							}
						}

						if (DateUtils.dateSubtraction(dutyTime, inTime)>=0){
							inLabel = "旷工";
						} else if(DateUtils.dateSubtraction(signElasticTime, inTime)>0){
							inLabel = "迟到";
						} else {
							inLabel = "正常";
						}
					}
				}
				/**
				 * 如果分析完打卡、请假和外出，依然没有获取到上班时间，则认为上班旷工(只计算工作日)
				 */
				if(type.equals("1") && inTime==null){
					if(!today.equals(day)){
						inLabel = "旷工";
					} else {
						inLabel = "--";
					}

				}

				/**
				 * **********************************分析下班考勤情况
				 */
				Date outTime = signOutTime;
				String outLabel = "缺卡"; //0正常；1早退/缺勤(18点前)；2 早退(不满8小时)；3 旷工
				if(StringUtils.isNotEmpty(signOutType)){
					if(signOutType.equals("0")){
						outLabel = "正常";
					} else if(signOutType.equals("1")){
						outLabel = "缺勤";
					} else if(signOutType.equals("2")){
						outLabel = "早退";
					} else if(signOutType.equals("3")){
						outLabel = "旷工";
					}
				}
				if(type.equals("0")){ //节假日考勤默认正常
					outLabel = "正常";
				}

				/**
				 * 如果常规下班打卡不正常
				 * 则继续分析外出和请假
				 */
				if(!outLabel.equals("正常")){
					if(leaveEndTime!=null){
						/**
						 * 请假又分两种情况
						 * 如果请假的结束时间晚于正常下班时间，则下班的状态不需要判断实际工作时长，都为正常
						 * 否则按照实际工作时长进行判断
						 */
						if(inLabel.equals("正常") && DateUtils.dateSubtraction(outSettingTime, leaveEndTime)>=0){
							outTime = leaveEndTime;
							outLabel = "正常";
						} else {
							if(outTime==null){
								outTime = leaveEndTime;
							} else {
								if(DateUtils.dateSubtraction(outTime, leaveEndTime)>0){
									outTime = leaveEndTime;
								}
							}
							if(inTime==null){
								if (DateUtils.dateSubtraction(dutyTime, outTime)<=0){ //下班卡14点之前，显示下班旷工
									outLabel = "旷工";
								} else if(DateUtils.dateSubtraction(outSettingTime, outTime)<0){
									outLabel = "早退";
								} else {
									outLabel = "正常";
								}
							} else {
								double actualWorkHours = DateUtils.dateDiffHoursDefinite(DateUtils.conversionHH(DateUtils.formatTime(inTime)),
										DateUtils.conversionHH(DateUtils.formatTime(outTime))); //实际的工作时长(小时)

								if (DateUtils.dateSubtraction(dutyTime, outTime)<=0){ //下班卡14点之前，显示下班旷工
									outLabel = "旷工";
								} else {
									if(actualWorkHours>=defineWorkHours){
										outLabel = "正常";
									} else {
										if(DateUtils.dateSubtraction(outSettingTime, outTime)>=0){
											outLabel = "早退";
										} else {
											outLabel = "缺勤";
										}
									}
								}
							}
						}
					}
				}
				if(!outLabel.equals("正常")){
					if(egressEndTime!=null){
						/**
						 * 请假又分两种情况
						 * 如果请假的结束时间晚于正常下班时间，则下班的状态不需要判断实际工作时长，都为正常
						 * 否则按照实际工作时长进行判断
						 */
						if(inLabel.equals("正常") && DateUtils.dateSubtraction(outSettingTime, egressEndTime)>=0){
							outTime = egressEndTime;
							outLabel = "正常";
						} else {
							if(outTime==null){
								outTime = egressEndTime;
							} else {
								if(DateUtils.dateSubtraction(outTime, egressEndTime)>0){
									outTime = egressEndTime;
								}
							}

							if(inTime==null){
								if (DateUtils.dateSubtraction(dutyTime, outTime)<=0){ //下班卡14点之前，显示下班旷工
									outLabel = "旷工";
								} else if(DateUtils.dateSubtraction(outSettingTime, outTime)<0){
									outLabel = "早退";
								} else {
									outLabel = "正常";
								}
							} else {
								double actualWorkHours = DateUtils.dateDiffHoursDefinite(DateUtils.conversionHH(DateUtils.formatTime(inTime)),
										DateUtils.conversionHH(DateUtils.formatTime(outTime))); //实际的工作时长(小时)

								if (DateUtils.dateSubtraction(dutyTime, outTime)<=0){ //下班卡14点之前，显示下班旷工
									outLabel = "旷工";
								} else {
									if(actualWorkHours>=defineWorkHours){
										outLabel = "正常";
									} else {
										if(DateUtils.dateSubtraction(outSettingTime, outTime)>=0){
											outLabel = "早退";
										} else {
											outLabel = "缺勤";
										}
									}
								}
							}
						}
					}
				}
				/**
				 * 如果分析完打卡、请假和外出，依然没有获取到下班时间，则认为下班旷工(只计算工作日)
				 */
				if(type.equals("1") && outTime==null){
					if(!today.equals(day)){
						outLabel = "旷工";
					} else {
						outLabel = "--";
					}
				}

				if(inTime!=null && outTime!=null){
					workHours = DateUtils.dateDiffHoursDefinite(inTime, outTime);
				}

				if(inLabel.equals("正常")){ //注意这里节假日也默认为正常，所以节假日要判断是否有加班时间
					if(type.equals("0")){
						if(overtimeStartTime!=null){
							actualDays += 0.5d;
						}
					} else {
						actualDays += 0.5d;
					}
				} else if(inLabel.equals("迟到")){
					cdCount+=1;
				} else if(inLabel.equals("旷工")){
					kgCount+=1;
				} else if(inLabel.equals("缺卡")){
					qkCount+=1;
				}
				if(outLabel.equals("正常")){
					if(type.equals("0")){
						if(overtimeEndTime!=null){
							actualDays += 0.5d;
						}
					} else {
						actualDays += 0.5d;
					}
				} else if(outLabel.equals("早退")){
					ztCount+=1;
				} else if(outLabel.equals("缺勤")){
					qqCount+=1;
				} else if(outLabel.equals("缺卡")){
					qkCount+=1;
				} else if(outLabel.equals("旷工")){
					if(!inLabel.equals("旷工")){ //以保证旷工一天只统计一次
						kgCount+=1;
					}
				}

				Map<String, Object> attendSnapshotMap = new HashMap<>();
				attendSnapshotMap.put("day", day);
				attendSnapshotMap.put("dayType", type);
				attendSnapshotMap.put("diffDay", diffDay);
				attendSnapshotMap.put("inTime", inTime==null?"":DateUtils.format(inTime, DateUtils.HHMM));
				attendSnapshotMap.put("outTime", outTime==null?"":DateUtils.format(outTime, DateUtils.HHMM));
				attendSnapshotMap.put("inLabel", inLabel);
				attendSnapshotMap.put("outLabel", outLabel);
				attendSnapshotMap.put("inTag", inTag);
				attendSnapshotMap.put("outTag", outTag);
				attendSnapshotMap.put("inAddr", inAddr);
				attendSnapshotMap.put("outAddr", outAddr);
				attendSnapshotMap.put("workHours", new Double(workHours).intValue());
				attendSnapshotList.add(attendSnapshotMap);

			}
		}

		return super.ok()
				.put("userEntity", userEntity)
				.put("shouldDays", shouldDays)
				.put("actualDays", actualDays)
				.put("qqCount", qqCount)
				.put("qkCount", qkCount)
				.put("cdCount", cdCount)
				.put("kgCount", kgCount)
				.put("ztCount", ztCount)
				.put("egressHours", egressHours)
				.put("travelDays", travelDays)
				.put("leaveDays", leaveDays)
				.put("bkCount", bkCount)
				.put("overtimeHours", overtimeHours)
				.put("attendSnapshotList", attendSnapshotList);
	}


	/**
	 * 部门日考勤情况统计
	 * @return
	 */
	@RequestMapping("/attendReportDept")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult attendReportDept(HttpServletRequest request, HttpServletResponse response) {
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		String day = Tool.convertObject(parameterMap.get("day"));
		if(StringUtils.isEmpty(day)){
			day = DateUtils.getCurrentDay();
		}
		String rootDeptId = Tool.convertObject(parameterMap.get("rootDeptId"));

		Map<String, Object> attendCondition = attendReportService.selectActualAttendConditionByDeptAndDay(userId, rootDeptId, day);

		return super.ok()
				.put("day", day)
				.put("attendCondition", attendCondition);

	}

	/**
	 * 部门内成员考勤情况统计
	 * @return
	 */
	@RequestMapping("/attendReportDeptDetail")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult attendReportDeptDetail(HttpServletRequest request, HttpServletResponse response) {
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		String day = Tool.convertObject(parameterMap.get("day"));
		if(StringUtils.isEmpty(day)){
			day = DateUtils.getCurrentDay();
		}
		String deptId = Tool.convertObject(parameterMap.get("deptId"));

		Map<String, Object> attendCondition = attendReportService.selectActualAttendStaffByDeptAndDay(userId, deptId, day);

		return super.ok()
				.put("day", day)
				.put("attendCondition", attendCondition);

	}


	/**
	 * 查询我主导的项目下的考勤情况
	 * @return
	 */
	@RequestMapping("/attendReportProject")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult attendReportProject(HttpServletRequest request, HttpServletResponse response) {
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		String day = Tool.convertObject(parameterMap.get("day"));
		if(StringUtils.isEmpty(day)){
			day = DateUtils.getCurrentDay();
		}

		List<Map<String, Object>> proAttendList = attendReportService.selectActualAttendByProject(userId, day);

		return super.ok()
				.put("day", day)
				.put("proAttendList", proAttendList);

	}

	/**
	 * 查询我主导的项目下的考勤情况
	 * @return
	 */
	@RequestMapping("/attendReportProTeam")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult attendReportProTeam(HttpServletRequest request, HttpServletResponse response) {
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		String day = Tool.convertObject(parameterMap.get("day"));
		if(StringUtils.isEmpty(day)){
			day = DateUtils.getCurrentDay();
		}
		String proId = Tool.convertObject(parameterMap.get("proId"));

		ProBaseEntity proBaseEntity = proBaseMybatisService.selectEntityById(proId);

		List<Map<String, Object>> proTeamAttendList = attendReportService.selectActualAttendByProTeam(userId, proId, day);

		return super.ok()
				.put("day", day)
				.put("proBaseEntity", proBaseEntity)
				.put("proTeamAttendList", proTeamAttendList);

	}


	/**
	 * 查询权限范围（部门权限和项目权限）下的考勤成员
	 * @return
	 */
	@RequestMapping("/attendReportAuthStaffList")
	@ResponseBody
	@GatewayAuth(GatewayConstants.require_login)
	public AjaxResult attendReportAuthStaffList(HttpServletRequest request, HttpServletResponse response) {
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
		String userId = getAppUserId(parameterMap);
		String searchKey = Tool.convertObject(parameterMap.get("searchKey"));

		String today = DateUtils.getCurrentDay();


		List<Map<String, Object>> authStaffList = attendReportService.selectAttendStaffListByAuth(userId, searchKey);

		return super.ok()
				.put("today", today)
				.put("authStaffList", authStaffList);

	}
}
