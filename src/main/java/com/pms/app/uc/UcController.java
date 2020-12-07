package com.pms.app.uc;

import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.config.YckConfigUtils;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.enums.DictTypeEnum;
import com.pms.common.enums.McSysCodeEnum;
import com.pms.common.service.SysMailService;
import com.pms.common.service.SysPushService;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.RegExpValidatorUtils;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.hr.domain.StaffGrade;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IHrMemberJoinService;
import com.pms.core.hr.service.IStaffGradeService;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.system.domain.SysDept;
import com.pms.core.system.domain.SysDictData;
import com.pms.core.system.domain.SysUser;
import com.pms.core.system.service.ISysDeptService;
import com.pms.core.system.service.ISysDictDataService;
import com.pms.core.system.service.ISysMenuService;
import com.pms.core.system.service.ISysUserService;
import com.pms.framework.web.base.AppBaseController;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 用户信息
 *
 * @author yangchangkun
 */
@Controller
@RequestMapping("/app/uc")
public class UcController extends AppBaseController {

    @Autowired
    private IStaffInfoService staffInfoService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private IStaffGradeService staffGradeService;

    @Autowired
    private ISysMenuService sysMenuService;

    @Autowired
    private IHrMemberJoinService hrMemberJoinService;

    @Autowired
    private ISysDeptService sysDeptService;


    @Autowired
    private ISysDictDataService sysDictDataService;

    @Autowired
    private SysPushService sysPushService;
    @Autowired
    private SysMailService sysMailService;

    /**
     * 查询职员详情
     */
    @RequestMapping("/mine")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult mine(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);

        StaffInfo staff = staffInfoService.selectEntityById(userId);

        SysUser user = userService.selectUserById(userId);

        /**
         * 资料是否完整
         */
        String isIntactFlag = "0";
        if (staff == null
                || StringUtils.isEmpty(staff.getIcType())
                || StringUtils.isEmpty(staff.getIcNum())
                || StringUtils.isEmpty(staff.getIcAddress())
                || staff.getBirthday() == null
                || StringUtils.isEmpty(staff.getPoliticCountenance())
                || StringUtils.isEmpty(staff.getMaritalStatus())
                || StringUtils.isEmpty(staff.getFertilityStatus())
                || StringUtils.isEmpty(staff.getResidence())
                || StringUtils.isEmpty(staff.getEduType())
                || StringUtils.isEmpty(staff.getEduSchool())
                || StringUtils.isEmpty(staff.getEduMajor())
                || StringUtils.isEmpty(staff.getEduDate())
                || StringUtils.isEmpty(staff.getWorkBeginDate())
                || StringUtils.isEmpty(staff.getInsureAccount())
                || StringUtils.isEmpty(staff.getInsureBank())
                ) {
            isIntactFlag = "1";
        }
        return super.ok().put("staff", staff).put("user", user).put("isIntactFlag", isIntactFlag);
    }

    /**
     * 修改保存职员
     */
    @RequestMapping("/edit")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult editSave(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);

        String name = Tool.convertObject(request.getParameter("name"));
        String phonenumber = Tool.convertObject(request.getParameter("phonenumber"));
        String sex = Tool.convertObject(request.getParameter("sex"));


        SysDept companyInfo = sysDeptService.selectCompanyEntity();


        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setPhonenumber(phonenumber);
        user.setSex(sex);

        StaffInfo entity = staffInfoService.selectByUserId(userId);
        entity.setUserId(userId);
        entity.setName(name);
        entity.setSex(sex);
        entity.setBirthday(DateUtils.conversion(Tool.convertObject(request.getParameter("birthday"))));
        entity.setNation(Tool.convertObject(request.getParameter("nation")));
        entity.setPoliticCountenance(Tool.convertObject(request.getParameter("politicCountenance"))); //政治面貌
        entity.setMaritalStatus(Tool.convertObject(request.getParameter("maritalStatus"))); //婚姻状况
        entity.setFertilityStatus(Tool.convertObject(request.getParameter("fertilityStatus"))); //生育状况
        entity.setIcType(Tool.convertObject(request.getParameter("icType"))); //户籍类型
        entity.setIcNum(Tool.convertObject(request.getParameter("icNum"))); //身份证号码
        entity.setIcAddress(Tool.convertObject(request.getParameter("icAddress"))); //户籍地址
        entity.setResidence(Tool.convertObject(request.getParameter("residence"))); //现住址
        entity.setEduType(Tool.convertObject(request.getParameter("eduType"))); //最高学历类型
        entity.setEduSchool(Tool.convertObject(request.getParameter("eduSchool"))); //毕业院校
        entity.setEduMajor(Tool.convertObject(request.getParameter("eduMajor"))); //所学专业
        entity.setEduDate(Tool.convertObject(request.getParameter("eduDate"))); //毕业日期
        entity.setWorkBeginDate(Tool.convertObject(request.getParameter("workBeginDate"))); //参加工作时间
        entity.setLinkName(Tool.convertObject(request.getParameter("linkName"))); //紧急联系人姓名
        entity.setLinkPhone(Tool.convertObject(request.getParameter("linkPhone"))); //紧急联系人电话
        entity.setLinkRelation(Tool.convertObject(request.getParameter("linkRelation"))); //紧急联系人关系
        entity.setInsureAccount(Tool.convertObject(request.getParameter("insureAccount"))); //社保账号(工资卡号)
        entity.setInsureBank(Tool.convertObject(request.getParameter("insureBank"))); //开户行（工资卡）
        entity.setInsureAddr(Tool.convertObject(request.getParameter("insureAddr"))); //社保缴纳地
        //entity.setPhoto(Tool.convertObject(request.getParameter("photo"))); //
        entity.setPhotoIcFront(Tool.convertObject(request.getParameter("photoIcFront"))); //身份证正面
        entity.setPhotoIcVerso(Tool.convertObject(request.getParameter("photoIcVerso"))); //身份证反面
        entity.setPhotoDegree(Tool.convertObject(request.getParameter("photoDegree"))); //学位照片
        entity.setPhotoEducation(Tool.convertObject(request.getParameter("photoEducation"))); //学历照片
        entity.setPhotoFace(Tool.convertObject(request.getParameter("photoFace"))); //一寸证件照

        if (StringUtils.isEmpty(userId)) {
            return super.error("userId不能为空");
        }
        if (StringUtils.isEmpty(name)) {
            return super.error("请填写姓名");
        }
        if (StringUtils.isEmpty(sex)) {
            return super.error("请选择性别");
        }
        if (StringUtils.isEmpty(phonenumber)) {
            return super.error("请填写电话号码");
        }
        if (phonenumber.length() > 11) {
            return super.error("电话号码最多只能填写11位");
        }
        if (entity.getBirthday() == null) {
            return super.error("请填写生日");
        }
        if (StringUtils.isEmpty(entity.getNation())) {
            return super.error("请选择民族");
        }
        if (StringUtils.isEmpty(entity.getPoliticCountenance())) {
            return super.error("请填写政治面貌");
        }
        if (StringUtils.isEmpty(entity.getMaritalStatus())) {
            return super.error("请填写婚姻状况");
        }
        if (StringUtils.isEmpty(entity.getFertilityStatus())) {
            return super.error("请填写生育状况");
        }
        if (StringUtils.isEmpty(entity.getIcType())) {
            return super.error("请填写户籍类型");
        }
        if (StringUtils.isEmpty(entity.getIcNum())) {
            return super.error("请填写身份证号码");
        }
        if (StringUtils.isEmpty(entity.getIcAddress())) {
            return super.error("请填写户籍地址");
        }
        if (StringUtils.isEmpty(entity.getResidence())) {
            return super.error("请填写现住址");
        }
        if (StringUtils.isEmpty(entity.getEduType())) {
            return super.error("请填写最高学历类型");
        }
        if (StringUtils.isEmpty(entity.getEduSchool())) {
            return super.error("请填写毕业院校");
        }
        if (StringUtils.isEmpty(entity.getEduMajor())) {
            return super.error("请填写所学专业");
        }
        if (StringUtils.isEmpty(entity.getEduDate())) {
            return super.error("请填写毕业日期");
        }
        if (entity.getEduDate().length() == 7) {
            entity.setEduDate(entity.getEduDate() + "-01");
        }
        if (entity.getEduDate().length() != 10) {
            return super.error("毕业日期格式不正确,正确格式:YYYY-MM-DD");
        }
        if (StringUtils.isEmpty(entity.getWorkBeginDate())) {
            return super.error("请填写参加工作时间");
        }
        if (entity.getWorkBeginDate().length() == 7) {
            entity.setWorkBeginDate(entity.getWorkBeginDate() + "-01");
        }
        if (entity.getWorkBeginDate().length() != 10) {
            return super.error("参加工作时间格式不正确,正确格式:YYYY-MM-DD");
        }
        if (StringUtils.isEmpty(entity.getLinkName())) {
            return super.error("请填写紧急联系人姓名");
        }
        if (StringUtils.isEmpty(entity.getLinkPhone())) {
            return super.error("请填写紧急联系人电话");
        }
        if (entity.getLinkPhone().length() > 11) {
            return super.error("紧急联系人电话最多只能填写11位");
        }
        if (StringUtils.isEmpty(entity.getLinkRelation())) {
            return super.error("请填写紧急联系人关系");
        }
        if (StringUtils.isEmpty(entity.getInsureAccount())) {
            return super.error("请填写工资卡号");
        }
        if (StringUtils.isEmpty(entity.getInsureBank())) {
            return super.error("请填写工资卡号开户行");
        }
        if (StringUtils.isEmpty(entity.getInsureAddr())) {
            return super.error("请填写社保缴纳地");
        }
        if (StringUtils.isEmpty(entity.getPhotoIcFront())) {
            return super.error("请上传身份证正面照片");
        }
        if (StringUtils.isEmpty(entity.getPhotoIcVerso())) {
            return super.error("请上传身份证反面照片");
        }
        if (StringUtils.isEmpty(entity.getPhotoDegree())) {
            return super.error("请上传学位照片");
        }
        if (StringUtils.isEmpty(entity.getPhotoEducation())) {
            return super.error("请上传学历照片");
        }
        if (StringUtils.isEmpty(entity.getPhotoFace())) {
            return super.error("请上传一寸证件照片");
        }

        staffInfoService.updateEntity(entity);
        userService.updateUserInfo(user);

        StaffInfo staff = staffInfoService.selectEntityById(entity.getUserId());


        /**
         * 给系统运维人员(数据字典sys_devops)发送邮件提醒开通邮箱
         */
        List<String> devopsList = new ArrayList<String>();
        List<SysDictData> dictList = sysDictDataService.selectDictDataByType(DictTypeEnum.sys_devops.getCode());
        if (dictList != null && dictList.size() > 0) {
            for (SysDictData sdd : dictList) {
                devopsList.add(sdd.getDictValue());
            }
        }
        List<StaffInfo> devOpsStaffList = null;
        if (devopsList != null && devopsList.size() > 0) {
            devOpsStaffList = staffInfoService.selectListByAccounts(devopsList.toArray(new String[devopsList.size()]));
        }
        if (devOpsStaffList != null && devOpsStaffList.size() > 0) {
            String subject = "职员补充资料";
            String content = "员工:" + entity.getName() + "，工号:" + entity.getCode() + "，已补全/修改了个人信息，请知悉。";

            for (StaffInfo si : devOpsStaffList) {
                sysMailService.sendMail(McSysCodeEnum.pmsAdmin.getCode(), companyInfo.getDeptId(), si.getEmail(), subject, content);
            }
        }

        return super.ok().put("staffCacheInfo", staff);
    }


    /**
     * 职员档案及成长记录
     */
    @RequestMapping("/staffArchives")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult staffArchives(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);

        String staffId = Tool.convertObject(parameterMap.get("staffId"));

        SysUser user = userService.selectUserById(userId);

        //用户权限列表
        Set<String> permissions = sysMenuService.selectPermsByUserId(user);
        boolean hasPermission = permissions.contains("hr:staffInfo:archives"); //是否具有职员档案及成长记录查看权限
        if (userId.equals(staffId)) {
            hasPermission = true;
        }

        StaffInfo entity = staffInfoService.selectByUserId(staffId);

        List<Map<String, Object>> archivesRecord = new ArrayList<>();
        if (hasPermission) {
            archivesRecord = staffInfoService.searchStaffArchivesRecord(staffId);
        }

        int jobDays = 0;
        Date joinDate = entity.getJoinDate();
        Date departureDate = entity.getDepartureDate();
        if (departureDate == null) {
            departureDate = DateUtils.conversion(DateUtils.getCurrentDay());
        }
        jobDays = DateUtils.dateDiffDay(joinDate, departureDate);

        return super.ok().put("entity", entity).put("archivesRecord", archivesRecord).put("jobDays", jobDays).put("hasPermission", hasPermission);
    }


    /**
     * 职员标签数据
     */
    @RequestMapping("/memberTag")
    @ResponseBody
    @GatewayAuth(GatewayConstants.un_require_login)
    public AjaxResult memberTag(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);

        String memberId = Tool.convertObject(parameterMap.get("memberId"));

        Map<String, Object> memberMap = hrMemberJoinService.selectCurrMemberInfo(memberId);
        if (memberMap == null || memberMap.isEmpty()) {
            return super.error("未检索到会员信息");
        }
        String userId = Tool.convertObject(memberMap.get("userId"));

        SysUser user = userService.selectUserById(userId);
        if (user == null) {
            return super.error("未检索到用户信息");
        }

        StaffInfo entity = staffInfoService.selectByUserId(user.getUserId());
        if (entity == null) {
            return super.error("未检索到职员信息");
        }

        Map<String, Object> memberTag = new HashMap<>();
        memberTag.put("memberId", memberId);
        memberTag.put("mobile", entity.getPhonenumber());
        memberTag.put("icNum", entity.getIcNum());

        /**
         * 婚姻状态
         * "已婚",  "未婚"
         */
        String maritalStatus = Tool.convertObject(entity.getMaritalStatus());
        memberTag.put("maritalStatus", maritalStatus);
        memberTag.put("maritalStatusLabel", maritalStatus);

        /**
         * 生育状况
         * "已育",  "未育"
         */
        String fertilityStatus = Tool.convertObject(entity.getFertilityStatus());
        memberTag.put("fertilityStatus", fertilityStatus);
        memberTag.put("fertilityStatusLabel", fertilityStatus);

        /**
         * 最高学历
         * "博士",  "硕士",  "本科",  "大专",  "中专", "高中"
         */
        String eduType = Tool.convertObject(entity.getEduType());
        memberTag.put("eduType", eduType);
        memberTag.put("eduTypeLabel", eduType);

        /**
         * 开始工作日期
         */
        String workBeginDate = Tool.convertObject(entity.getWorkBeginDate());
        int workYear = 0;
        if (StringUtils.isNotEmpty(workBeginDate)) {
            workYear = DateUtils.getDiffYear(workBeginDate, DateUtils.getCurrentDay(), DateUtils.YYYY_MM_DD);
        }
        String workYearLabel = "";
        if (workYear < 1) {
            workYearLabel = "1年以内";
        } else if (workYear >= 1 && workYear < 3) {
            workYearLabel = "1-3年";
        } else if (workYear >= 3 && workYear < 5) {
            workYearLabel = "3-5年";
        } else if (workYear >= 5 && workYear < 10) {
            workYearLabel = "5-10年";
        } else {
            workYearLabel = "10年以上";
        }
        memberTag.put("workYear", workYear);
        memberTag.put("workYearLabel", workYearLabel);

        /**
         * 职等
         */
        String postRank = Tool.convertObject(entity.getPostRank());
        String levelId = Tool.convertObject(entity.getLevelId());
        StaffGrade staffGrade = staffGradeService.selectEntityById(levelId);
        int gradeCost = 0; //薪宽(年薪)
        if (staffGrade != null) {
            gradeCost = staffGrade.getCost() * 12;
        }
        String gradeCostLabel = "";
        if (gradeCost < 100000) {
            gradeCostLabel = "10万内";
        } else if (gradeCost >= 100000 && gradeCost < 300000) {
            gradeCostLabel = "10-30万";
        } else if (gradeCost >= 300000 && gradeCost < 500000) {
            gradeCostLabel = "30-50万";
        } else if (gradeCost >= 500000 && gradeCost < 1000000) {
            gradeCostLabel = "50-100万";
        } else {
            gradeCostLabel = "100万以上";
        }
        memberTag.put("gradeCost", gradeCost);
        memberTag.put("gradeCostLabel", gradeCostLabel);

        /**
         * 岗位类型
         * A 架构
         * B 业务
         * D 数据
         * F 职能
         * M 管理
         * P 运营
         * S 市场
         * T 技术
         */
        String postCategory = Tool.convertObject(entity.getPostCategory());
        String postCategoryLabel = Tool.convertObject(entity.getPostCategoryLabel());
        memberTag.put("postCategory", postCategory);
        memberTag.put("postCategoryLabel", postCategoryLabel);

        /**
         * 岗位级别
         *  VP VP
         *  EX 专家
         *  SE 资深
         *  HL 高级
         *  ML 中级
         *  EL 初级
         *  PL 预备
         */
        String postLevel = Tool.convertObject(entity.getPostLevel());
        String postLevelLabel = Tool.convertObject(entity.getPostLevelLabel());
        memberTag.put("postLevel", postLevel);
        memberTag.put("postLevelLabel", postLevelLabel);

        /**
         * 职员层级:
         * 0:未评层、
         * 1:骨干层、
         * 2:核心层、
         * 3:普通积极层、
         * 4:普通合格层、
         * 5:普通基本合格层、
         * 6:淘汰层
         */
        String staffTier = Tool.convertObject(entity.getStaffTier());
        String staffTierLabel = Tool.convertObject(entity.getStaffTierLabel());
        memberTag.put("staffTier", staffTier);
        memberTag.put("staffTierLabel", staffTierLabel);

        /**
         * 合同类型
         *  1 全职 ，2 实习 ，3 劳务派遣
         */
        String contractType = Tool.convertObject(entity.getContractType());
        String contractTypeLabel = "";
        if (contractType.equals("1")) {
            contractTypeLabel = "全职";
        } else if (contractType.equals("2")) {
            contractTypeLabel = "实习";
        } else if (contractType.equals("3")) {
            contractTypeLabel = "劳务派遣";
        }
        memberTag.put("contractType", contractType);
        memberTag.put("contractTypeLabel", contractTypeLabel);

        /**
         * 是否享受个税专项扣除，0 享受，1不享受
         */
        String insureTaxPolicy = Tool.convertObject(entity.getInsureTaxPolicy());
        String insureTaxPolicyLabel = "";
        if (insureTaxPolicy.equals("0")) {
            insureTaxPolicyLabel = "享受";
        } else if (insureTaxPolicy.equals("1")) {
            insureTaxPolicyLabel = "不享受";
        }
        memberTag.put("insureTaxPolicy", insureTaxPolicy);
        memberTag.put("insureTaxPolicyLabel", insureTaxPolicyLabel);


        return super.ok().put("memberTag", memberTag);
    }


}