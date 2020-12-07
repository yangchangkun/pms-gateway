package com.pms.app.scenecloud;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.base.SceneCloudAjaxResult;
import com.pms.common.config.SceneCloudConfigUtils;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.enums.DictTypeEnum;
import com.pms.common.enums.McSysCodeEnum;
import com.pms.common.service.SysMailService;
import com.pms.common.service.SysPushService;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.Tool;
import com.pms.common.utils.encrypt.SceneCloudEncryUtil;
import com.pms.common.utils.encrypt.SceneCloudSignUtil;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 场景云交互接口
 * 
 * @author yangchangkun
 */
@Controller
@RequestMapping("/app/scene/cloud")
public class ScController extends AppBaseController
{

    @Autowired
    private IStaffInfoService staffInfoService;

    @Autowired
    private IStaffGradeService staffGradeService;

    @Autowired
    private SceneCloudConfigUtils sceneCloudConfigUtils;

    /**
     * APP客户端获取场景云的访问地址
     */
    @RequestMapping("/getHomeUri")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult getHomeUri(HttpServletRequest request, HttpServletResponse response)
    {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);
        if(StringUtils.isEmpty(userId)){
            return super.error("用户ID参数不能为空");
        }

        StaffInfo staff = staffInfoService.selectEntityById(userId);
        if(staff==null){
            return super.error("用户信息不存在");
        }

        String sceneCloudHomeUrl = sceneCloudConfigUtils.getHomeUrlPrefix();

        SortedMap<String, Object> inputParam = new TreeMap<>();
        inputParam.put("appid", sceneCloudConfigUtils.getAppId());
        inputParam.put("channelCode", sceneCloudConfigUtils.getChannelCode());
        inputParam.put("timestamp", DateUtils.getCurAllTime2());
        inputParam.put("nonceStr", System.currentTimeMillis());
        inputParam.put("uid", userId);
        inputParam.put("utype", "B003"); //用户类型：B001-司机，B002-乘客，B003-企易通用户
        inputParam.put("signType", "A001"); //

        String sign = "";
        try{
            sign = SceneCloudSignUtil.generateSign(inputParam, sceneCloudConfigUtils.getAppSecret());
        } catch (Exception e){
            e.printStackTrace();
        }

        sceneCloudHomeUrl+="?appid="+inputParam.get("appid");
        sceneCloudHomeUrl+="&channelCode="+inputParam.get("channelCode");
        sceneCloudHomeUrl+="&timestamp="+inputParam.get("timestamp");
        sceneCloudHomeUrl+="&nonceStr="+inputParam.get("nonceStr");
        sceneCloudHomeUrl+="&uid="+inputParam.get("uid");
        sceneCloudHomeUrl+="&utype="+inputParam.get("utype");
        sceneCloudHomeUrl+="&signType="+inputParam.get("signType");
        sceneCloudHomeUrl+="&sign="+sign;
        logger.error("APP客户端获取场景云的访问地址sceneCloudHomeUrl="+sceneCloudHomeUrl);

        return super.ok().put("sceneCloudHomeUrl", sceneCloudHomeUrl);
    }


    /**
     * 接口由场景端提供，托管平台调用。根据入口获取的用户uid和utype，查询该客户的一些信息，用来产品投放匹配和跳转等。
     */
    @RequestMapping("/getUserInfo")
    @ResponseBody
    @GatewayAuth(GatewayConstants.un_require_login)
    public SceneCloudAjaxResult getUserInfo(HttpServletRequest request, HttpServletResponse response)
    {

        String body = RequestUtil.readAsChars(request);
        logger.error("getUserInfo.body="+body);
        if(StringUtils.isEmpty(body)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "请求参数不能为空");
        }
        JSONObject parameterJo = JSONObject.parseObject(body);

        String appid = Tool.convertObject(parameterJo.get("appid"));
        String nonceStr = Tool.convertObject(parameterJo.get("nonceStr"));
        String timestamp = Tool.convertObject(parameterJo.get("timestamp"));
        String signType = Tool.convertObject(parameterJo.get("signType"));
        String sign = Tool.convertObject(parameterJo.get("sign"));
        JSONObject bizContent = parameterJo.getJSONObject("bizContent");

        if(StringUtils.isEmpty(appid)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "appid参数不能为空");
        }
        if(StringUtils.isEmpty(nonceStr)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "nonceStr参数不能为空");
        }
        if(StringUtils.isEmpty(timestamp)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "timestamp参数不能为空");
        }
        if(StringUtils.isEmpty(signType)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "signType参数不能为空");
        }
        if(StringUtils.isEmpty(sign)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "sign参数不能为空");
        }
        if(bizContent.isEmpty()){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "bizContent参数不能为空");
        }

        if(!appid.equals(sceneCloudConfigUtils.getAppId())){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "非法的appid参数");
        }

        SortedMap<String, Object> inputParam = new TreeMap<>();
        inputParam.put("appid", appid);
        inputParam.put("nonceStr", nonceStr);
        inputParam.put("timestamp", timestamp);
        inputParam.put("signType", signType);
        inputParam.put("sign", sign);
        inputParam.put("bizContent", bizContent);

        boolean verifySign = false;
        try{
            verifySign = SceneCloudSignUtil.verifyBizSign(inputParam, sceneCloudConfigUtils.getAppSecret());
        } catch (Exception e){
            e.printStackTrace();
        }
        if(!verifySign){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30001, "验签失败");
        }

        String uid = Tool.convertObject(bizContent.getString("uid"));
        String utype = Tool.convertObject(bizContent.getString("utype"));
        if(StringUtils.isEmpty(uid)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "uid参数不能为空");
        }
        if(StringUtils.isEmpty(utype)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "utype参数不能为空");
        }
        if(!utype.equals("B003")){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "非法的utype参数");
        }

        StaffInfo entity = staffInfoService.selectByUserId(uid);
        if (entity == null) {
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30006, "未检索到职员信息");
        }

        String city = entity.getJoinAddr();
        Date birthday = entity.getBirthday();
        int age = DateUtils.getAgeByBirth(birthday);
        String identity = entity.getIcNum();
        try{
            identity = SceneCloudEncryUtil.md5AsHexUpper(identity);
        } catch (Exception e){
            e.printStackTrace();
        }

        Map<String, Object> memberTag = new HashMap<>();
        memberTag.put("uid", uid);
        memberTag.put("age", age);
        memberTag.put("city", city);
        memberTag.put("identityType", "I001"); //身份证
        memberTag.put("identity", identity);


        return SceneCloudAjaxResult.success().put("result", memberTag);
    }



    /**
     * 接口由场景端提供，银行发起调用。根据用户申请的产品，由提供产品的银行在用户申请产品提交时请求司机的客户数据
     */
    @RequestMapping("/getUserDatas")
    @ResponseBody
    @GatewayAuth(GatewayConstants.un_require_login)
    public SceneCloudAjaxResult getUserDatas(HttpServletRequest request, HttpServletResponse response)
    {
        String body = RequestUtil.readAsChars(request);
        logger.error("getUserDatas.body="+body);
        if(StringUtils.isEmpty(body)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "请求参数不能为空");
        }
        JSONObject parameterJo = JSONObject.parseObject(body);

        String appid = Tool.convertObject(parameterJo.get("appid"));
        String nonceStr = Tool.convertObject(parameterJo.get("nonceStr"));
        String timestamp = Tool.convertObject(parameterJo.get("timestamp"));
        String signType = Tool.convertObject(parameterJo.get("signType"));
        String sign = Tool.convertObject(parameterJo.get("sign"));
        JSONObject bizContent = parameterJo.getJSONObject("bizContent");

        if(StringUtils.isEmpty(appid)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "appid参数不能为空");
        }
        if(StringUtils.isEmpty(nonceStr)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "nonceStr参数不能为空");
        }
        if(StringUtils.isEmpty(timestamp)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "timestamp参数不能为空");
        }
        if(StringUtils.isEmpty(signType)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "signType参数不能为空");
        }
        if(StringUtils.isEmpty(sign)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "sign参数不能为空");
        }
        if(bizContent.isEmpty()){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "bizContent参数不能为空");
        }

        if(!appid.equals(sceneCloudConfigUtils.getAppId())){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "非法的appid参数");
        }

        SortedMap<String, Object> inputParam = new TreeMap<>();
        inputParam.put("appid", appid);
        inputParam.put("nonceStr", nonceStr);
        inputParam.put("timestamp", timestamp);
        inputParam.put("signType", signType);
        inputParam.put("sign", sign);
        inputParam.put("bizContent", bizContent);

        boolean verifySign = false;
        try{
            verifySign = SceneCloudSignUtil.verifyBizSign(inputParam, sceneCloudConfigUtils.getAppSecret());
        } catch (Exception e){
            e.printStackTrace();
        }
        if(!verifySign){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30001, "验签失败");
        }

        String uid = Tool.convertObject(bizContent.getString("uid"));
        String identityType = Tool.convertObject(bizContent.getString("identityType"));
        String identity = Tool.convertObject(bizContent.getString("identity"));
        String bankId = Tool.convertObject(bizContent.getString("bankId"));
        String utype = Tool.convertObject(bizContent.getString("utype"));

        if(StringUtils.isEmpty(uid)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "uid参数不能为空");
        }
        if(StringUtils.isEmpty(utype)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "utype参数不能为空");
        }
        if(!utype.equals("B003")){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "非法的utype参数");
        }

        StaffInfo entity = staffInfoService.selectByUserId(uid);
        if (entity == null) {
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30006, "未检索到职员信息");
        }

        String icNum = entity.getIcNum();
        try{
            icNum = SceneCloudEncryUtil.md5AsHexUpper(icNum);
        } catch (Exception e){
            e.printStackTrace();
        }
        if(!identity.equals(icNum)){
            return SceneCloudAjaxResult.error(SceneCloudAjaxResult.code30002, "identity参数和系统内用户的身份信息不匹配");
        }

        String city = entity.getJoinAddr();
        Date birthday = entity.getBirthday();
        int age = DateUtils.getAgeByBirth(birthday);

        Map<String, Object> memberTag = new HashMap<>();
        memberTag.put("uid", uid);
        memberTag.put("age", age);
        memberTag.put("city", city);
        memberTag.put("mobile", entity.getPhonenumber());

        /**
         * 婚姻状态
         * "已婚",  "未婚"
         */
        String maritalStatusLabel = Tool.convertObject(entity.getMaritalStatus());
        String maritalStatus = "0";
        if(maritalStatusLabel.equals("已婚")){
            maritalStatus = "1";
        }
        memberTag.put("maritalStatus", maritalStatus);

        /**
         * 生育状况
         * "已育",  "未育"
         */
        String fertilityStatusLabel = Tool.convertObject(entity.getFertilityStatus());
        String fertilityStatus = "0";
        if(fertilityStatusLabel.equals("已育")){
            fertilityStatus = "1";
        }
        memberTag.put("fertilityStatus", fertilityStatus);

        /**
         * 最高学历
         * "博士",  "硕士",  "本科",  "大专",  "中专", "高中"
         */
        String eduTypeLabel = Tool.convertObject(entity.getEduType());
        String eduType = "1";
        if(eduTypeLabel.equals("中专")){
            eduType = "2";
        } else if(eduTypeLabel.equals("大专")){
            eduType = "3";
        } else if(eduTypeLabel.equals("本科")){
            eduType = "4";
        } else if(eduTypeLabel.equals("硕士")){
            eduType = "5";
        } else if(eduTypeLabel.equals("博士")){
            eduType = "6";
        }
        memberTag.put("eduType", eduType);

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
            workYearLabel = "1";
        } else if (workYear >= 1 && workYear < 3) {
            workYearLabel = "2";
        } else if (workYear >= 3 && workYear < 5) {
            workYearLabel = "3";
        } else if (workYear >= 5 && workYear < 10) {
            workYearLabel = "4";
        } else {
            workYearLabel = "5";
        }
        memberTag.put("workYear", workYearLabel);

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
            gradeCostLabel = "1";
        } else if (gradeCost >= 100000 && gradeCost < 300000) {
            gradeCostLabel = "2";
        } else if (gradeCost >= 300000 && gradeCost < 500000) {
            gradeCostLabel = "3";
        } else if (gradeCost >= 500000 && gradeCost < 1000000) {
            gradeCostLabel = "4";
        } else {
            gradeCostLabel = "5";
        }
        memberTag.put("gradeCost", gradeCostLabel);

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
        memberTag.put("postCategory", postCategory);

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
        memberTag.put("postLevel", postLevel);

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
        memberTag.put("staffTier", staffTier);

        /**
         * 合同类型
         *  1 全职 ，2 实习 ，3 劳务派遣
         */
        String contractType = Tool.convertObject(entity.getContractType());
        memberTag.put("contractType", contractType);

        /**
         * 是否享受个税专项扣除，0 享受，1不享受
         */
        String insureTaxPolicy = Tool.convertObject(entity.getInsureTaxPolicy());
        memberTag.put("insureTaxPolicy", insureTaxPolicy);


        return SceneCloudAjaxResult.success().put("result", memberTag);
    }

}