package com.pms.app.crm;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
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
import com.pms.core.hr.domain.HrBoleBonus;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.system.domain.SysOss;
import com.pms.core.system.domain.SysUser;
import com.pms.core.system.service.ISysMenuService;
import com.pms.core.system.service.ISysOssService;
import com.pms.core.system.service.ISysUserService;
import com.pms.core.system.service.impl.SysSeqService;
import com.pms.core.workflow.service.IWorkflowCcService;
import com.pms.core.workflow.service.IWorkflowOssService;
import com.pms.framework.web.base.AppBaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author zfl
 * @company jrx
 * @date 2019-12-16 16:43
 */
@Controller
@RequestMapping("/app/crmVisit")
public class CrmCustomerVisitController extends AppBaseController {
    @Autowired
    private ICrmCustomerVisitService crmCustomerVisitService;
    @Autowired
    private IStaffInfoService staffInfoService;
    @Autowired
    private ICrmCustomerInfoService infoService;
    @Autowired
    private ICrmCustomerStakeholderService stakeholderService;
    @Autowired
    private ICrmCustomerVisitOssService ossService;
    @Autowired
    private ICrmCustomerVisitCcService crmCustomerVisitCcService;
    @Autowired
    private ISysOssService sysOssService;
    @Autowired
    private ISysUserService sysUser;
    @Autowired
    private ISysMenuService sysMenuService;
    @Autowired
    private SysSeqService sysSeqService;
    @Autowired
    private ICrmCustomerInfoService crmCustomerInfoService;

    /**
     * 初始化
     *
     * @return
     */
    @RequestMapping("/init")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult init(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);

        StaffInfo staffInfo = staffInfoService.selectByUserId(userId);

        Map<String, Object> parameter = new HashMap<>();

        //查询银行列表
        List<CrmCustomerInfo> infoList = infoService.selectList(parameter);
        //人员列表
        //List<CrmCustomerStakeholder> stakeholders = staffInfoService.selectList();

        CrmCustomerVisit entity = new CrmCustomerVisit();
        entity.setCreateName(staffInfo.getName());
        entity.setCreateUserId(userId);
        entity.setCustId("");
        entity.setCustStakeholderId("");
        entity.setVisitMode("");

        return super.ok().put("infoList", infoList).put("entity",entity);
    }

    /**
     * 查询联系人列表
     *
     * @return
     */
    @RequestMapping("/list")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult list(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);

        String custShortName = Tool.convertObject(parameterMap.get("custShortName"));
        String createName = Tool.convertObject(parameterMap.get("createName"));
        String type = Tool.convertObject(parameterMap.get("type"));

        int pageNum = Tool.convertStringToInt(parameterMap.get("pageNum"));
        if(pageNum<=0){
            pageNum = 1;
        }
        int pageSize = Tool.convertStringToInt(parameterMap.get("pageSize"));
        if(pageSize<=0 || pageSize>50){
            pageSize = 10;
        }

        String orderBy = " cv.visit_time desc ";

        SysUser user = sysUser.selectUserById(userId);

        String createUserId = "";
        String ccAccount = "";
        if(type.equals("toMyList")){
            //抄送我的
            ccAccount = user.getAccount();
        } else {
            //我创建的
            createUserId = userId;
        }


        Map<String, Object> params = new HashMap<>();
        params.put("custShortName", custShortName);
        params.put("createName", createName);
        params.put("createUserId", createUserId);
        params.put("ccAccount", ccAccount);

        PageHelper.startPage(pageNum, pageSize, orderBy);
        List<Map<String, Object>> list = crmCustomerVisitService.selectVisitList(params);
        PageInfo page = new PageInfo(list);
        /**
         * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
         */
        page.setList(new ArrayList());

        return super.ok().put("gridDatas", list).put("webPage", page);

    }

    /**
     * 查询银行详情
     */
    //@RequiresPermissions("crm:customerInfo:detail")
    @RequestMapping("/detail")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult detail(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);

        String id = Tool.convertObject(parameterMap.get("id"));

        Map<String, Object> entity = crmCustomerVisitService.selectVisitDetailById(id);

        if (!entity.containsKey("visitCompleteDay")) {
            entity.put("visitCompleteDay", "");
        }
        if (!entity.containsKey("firstTime")) {
            entity.put("firstTime", "");
        }
        if (!entity.containsKey("visitTime")) {
            entity.put("visitTime", "");
        }
        if (!entity.containsKey("custLeaderId") ||  entity.get("custLeaderId") == null || entity.get("custLeaderId").equals("")) {
            entity.put("flag", "1");
        } else {
            entity.put("flag", "0");
        }

        List<SysOss> ossList = sysOssService.selectListByCustId(id, "");

        List<CrmCustomerVisitCc> ccList = crmCustomerVisitCcService.selectListByVisitId(id);
        Set<String> ccAccounts = new HashSet<>();
        if(ccList!=null && ccList.size()>0){
            for(CrmCustomerVisitCc cc : ccList){
                ccAccounts.add(cc.getCcAccount());
            }
        }
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
                    if(com.pms.common.utils.StringUtils.isNotEmpty(si.getPostName())){
                        si.setPostName("***");
                    }
                    ccStaffList.add(si);
                }
            }
        }

        return super.ok().put("entity", entity).put("ossList", ossList).put("ccStaffList", ccStaffList);
    }

    /**
     * 添加提交
     *
     * @return
     */
    @RequestMapping("/add")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult add(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);
        String id = Tool.convertObject(parameterMap.get("id"));
        String custId = Tool.convertObject(parameterMap.get("custId"));
        String custLeaderId = Tool.convertObject(parameterMap.get("custLeaderId"));
        String custStakeholderId = Tool.convertObject(parameterMap.get("custStakeholderId"));
        String visitMode = Tool.convertObject(parameterMap.get("visitMode"));
        String visitBrief = Tool.convertObject(parameterMap.get("visitBrief"));
        String visitParticipants = Tool.convertObject(parameterMap.get("visitParticipants"));
        String visitDescribe = Tool.convertObject(parameterMap.get("visitDescribe"));
        String visitPlan = Tool.convertObject(parameterMap.get("visitPlan"));
        String visitAdvise = Tool.convertObject(parameterMap.get("visitAdvise"));
        String visitState = Tool.convertObject(parameterMap.get("visitState"));
        String visitConclusion = Tool.convertObject(parameterMap.get("visitConclusion"));
        String visitTime = Tool.convertObject(parameterMap.get("visitTime"));

        String firstTime = Tool.convertObject(parameterMap.get("firstTime"));
        String weekNewCust = Tool.convertObject(parameterMap.get("weekNewCust"));
        String manyTimes = Tool.convertObject(parameterMap.get("manyTimes"));
        String sellStatus = Tool.convertObject(parameterMap.get("sellStatus"));
        String visitQuest = Tool.convertObject(parameterMap.get("visitQuest"));
        String visitCompleteDay = Tool.convertObject(parameterMap.get("visitCompleteDay"));
        String memo = Tool.convertObject(parameterMap.get("memo"));
        String bankStructure = Tool.convertObject(parameterMap.get("bankStructure"));
        String businessDept = Tool.convertObject(parameterMap.get("businessDept"));

        String oss = Tool.convertObject(parameterMap.get("oss"));
        String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

        if (StringUtils.isEmpty(custId)) {
            return super.error("请选择具体的公司");
        }
        if (StringUtils.isEmpty(visitTime)) {
            return super.error("拜访时间不能为空");
        }
        if (StringUtils.isEmpty(firstTime)) {
            return super.error("初次接触时间不能为空");
        }
        if (StringUtils.isEmpty(custStakeholderId)) {
            return super.error("接触人不能为空");
        }

        /**
         * ID
         */
        String applyId = SnowflakeIdUtil.getId();

        CrmCustomerVisit entity = new CrmCustomerVisit();

        entity.setId(applyId);
        entity.setCustId(custId);
        entity.setVisitTime(DateUtils.conversion(visitTime, "yyyy-MM-dd HH:mm:ss"));
        entity.setCreateTime(new Date());
        entity.setCreateUserId(userId);
        entity.setCustLeaderId(custLeaderId);
        entity.setCustStakeholderId(custStakeholderId);
        entity.setUpdateTime(new Date());
        entity.setVisitAdvise(visitAdvise);
        entity.setVisitBrief(visitBrief);
        entity.setVisitConclusion(visitConclusion);
        entity.setVisitDescribe(visitDescribe);
        entity.setVisitMode(visitMode);
        entity.setVisitParticipants(visitParticipants);
        entity.setVisitPlan(visitPlan);
        entity.setVisitState(visitState);
        entity.setFirstTime(firstTime);
        entity.setWeekNewCust(weekNewCust);
        entity.setManyTimes(manyTimes);
        entity.setSellStatu(sellStatus);
        entity.setVisitQuest(visitQuest);
        entity.setMemo(memo);
        entity.setVisitCompleteDay(visitCompleteDay);
        entity.setBankStructure(bankStructure);
        entity.setBusinessDept(businessDept);
        crmCustomerVisitService.insertEntity(entity);

        /**
         * 保存附件
         */
        if (StringUtils.isNotEmpty(oss)) {
            List<CrmCustomerVisitOss> ossDatas = new ArrayList<>();
            String[] ossArray = oss.split(",");
            for (String ossId : ossArray) {
                CrmCustomerVisitOss wo = new CrmCustomerVisitOss();
                wo.setId(SnowflakeIdUtil.getId());
                wo.setVisitId(applyId);
                wo.setOssId(ossId);
                ossDatas.add(wo);
            }
            if (ossDatas != null && ossDatas.size() > 0) {
                ossService.batchInsertEntity(ossDatas);
            }
        }

        /**
         * 保存抄送信息
         */
        if (StringUtils.isNotEmpty(ccAccounts) && visitState.equals("1")) {
            List<CrmCustomerVisitCc> ccDatas = new ArrayList<>();
            String[] ccArray = ccAccounts.split(",");
            for (String ccAccount : ccArray) {
                if(StringUtils.isNotEmpty(ccAccount)){
                    CrmCustomerVisitCc cc = new CrmCustomerVisitCc();
                    cc.setId(SnowflakeIdUtil.getId());
                    cc.setVisitId(applyId);
                    cc.setCcAccount(ccAccount);
                    ccDatas.add(cc);
                }
            }
            if (ccDatas != null && ccDatas.size() > 0) {
                crmCustomerVisitCcService.batchInsertEntity(ccDatas);
            }
        }

        return super.ok();
    }

    /**
     * 更新提交
     *
     * @return
     */
    @RequestMapping("/update")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult update(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);
        String id = Tool.convertObject(parameterMap.get("id"));
        String custId = Tool.convertObject(parameterMap.get("custId"));
        String custLeaderId = Tool.convertObject(parameterMap.get("custLeaderId"));
        String custStakeholderId = Tool.convertObject(parameterMap.get("custStakeholderId"));
        String visitMode = Tool.convertObject(parameterMap.get("visitMode"));
        String visitBrief = Tool.convertObject(parameterMap.get("visitBrief"));
        String visitParticipants = Tool.convertObject(parameterMap.get("visitParticipants"));
        String visitDescribe = Tool.convertObject(parameterMap.get("visitDescribe"));
        String visitPlan = Tool.convertObject(parameterMap.get("visitPlan"));
        String visitAdvise = Tool.convertObject(parameterMap.get("visitAdvise"));
        String visitState = Tool.convertObject(parameterMap.get("visitState"));
        String visitConclusion = Tool.convertObject(parameterMap.get("visitConclusion"));
        String visitTime = Tool.convertObject(parameterMap.get("visitTime"));

        String firstTime = Tool.convertObject(parameterMap.get("firstTime"));
        String weekNewCust = Tool.convertObject(parameterMap.get("weekNewCust"));
        String manyTimes = Tool.convertObject(parameterMap.get("manyTimes"));
        String sellStatus = Tool.convertObject(parameterMap.get("sellStatus"));
        String visitQuest = Tool.convertObject(parameterMap.get("visitQuest"));
        String visitCompleteDay = Tool.convertObject(parameterMap.get("visitCompleteDay"));
        String memo = Tool.convertObject(parameterMap.get("memo"));
        String bankStructure = Tool.convertObject(parameterMap.get("bankStructure"));
        String businessDept = Tool.convertObject(parameterMap.get("businessDept"));

        String oss = Tool.convertObject(parameterMap.get("oss"));
        String ccAccounts = Tool.convertObject(parameterMap.get("ccAccounts"));

        if (StringUtils.isEmpty(custId)) {
            return super.error("请选择具体的公司");
        }

        if (StringUtils.isEmpty(visitTime)) {
            return super.error("拜访时间不能为空");
        }
        if (StringUtils.isEmpty(firstTime)) {
            return super.error("初次接触时间不能为空");
        }
        if (StringUtils.isEmpty(custStakeholderId)) {
            return super.error("接触人不能为空");
        }
        CrmCustomerVisit entity = new CrmCustomerVisit();

        entity.setId(id);
        entity.setCustId(custId);
        entity.setVisitTime(DateUtils.conversion(visitTime, "yyyy-MM-dd HH:mm:ss"));
        entity.setCreateTime(new Date());
        entity.setCreateUserId(userId);
        entity.setCustLeaderId(custLeaderId);
        entity.setCustStakeholderId(custStakeholderId);
        entity.setUpdateTime(new Date());
        entity.setVisitAdvise(visitAdvise);
        entity.setVisitBrief(visitBrief);
        entity.setVisitConclusion(visitConclusion);
        entity.setVisitDescribe(visitDescribe);
        entity.setVisitMode(visitMode);
        entity.setVisitParticipants(visitParticipants);
        entity.setVisitPlan(visitPlan);
        entity.setVisitState(visitState);
        entity.setFirstTime(firstTime);
        entity.setWeekNewCust(weekNewCust);
        entity.setManyTimes(manyTimes);
        entity.setSellStatu(sellStatus);
        entity.setVisitQuest(visitQuest);
        entity.setMemo(memo);
        entity.setVisitCompleteDay(visitCompleteDay);
        entity.setBankStructure(bankStructure);
        entity.setBusinessDept(businessDept);
        crmCustomerVisitService.updateEntity(entity);

        /**
         * 保存附件
         */
        if (StringUtils.isNotEmpty(oss)) {
            List<CrmCustomerVisitOss> ossDatas = new ArrayList<>();
            String[] ossArray = oss.split(",");
            for (String ossId : ossArray) {
                CrmCustomerVisitOss wo = new CrmCustomerVisitOss();
                wo.setId(SnowflakeIdUtil.getId());
                wo.setVisitId(id);
                wo.setOssId(ossId);
                ossDatas.add(wo);
            }
            if (ossDatas != null && ossDatas.size() > 0) {
                ossService.deleteByVisitId(id);
                ossService.batchInsertEntity(ossDatas);
            }
        }

        /**
         * 保存抄送信息
         */
        if (StringUtils.isNotEmpty(ccAccounts) && visitState.equals("1")) {
            List<CrmCustomerVisitCc> ccDatas = new ArrayList<>();
            String[] ccArray = ccAccounts.split(",");
            for (String ccAccount : ccArray) {
                CrmCustomerVisitCc cc = new CrmCustomerVisitCc();
                cc.setId(SnowflakeIdUtil.getId());
                cc.setVisitId(id);
                cc.setCcAccount(ccAccount);
                ccDatas.add(cc);
            }
            if (ccDatas != null && ccDatas.size() > 0) {
                crmCustomerVisitCcService.deleteByVisitId(id);
                crmCustomerVisitCcService.batchInsertEntity(ccDatas);
            }
        }

        return super.ok();
    }


    @ResponseBody
    @RequestMapping("/customerList")
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult customerList(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        //创建者id
        String userId = getAppUserId(parameterMap);


        int pageNum = Tool.convertStringToInt(parameterMap.get("pageNum"));
        if(pageNum<=0){
            pageNum = 1;
        }
        int pageSize = Tool.convertStringToInt(parameterMap.get("pageSize"));
        if(pageSize<=0 || pageSize>50){
            pageSize = 10;
        }

        String orderBy = " ci.id DESC ";


        //查询银行列表
        PageHelper.startPage(pageNum, pageSize, orderBy);
        List<Map<String, Object>> infoList = infoService.selectCustomerInfoList(parameterMap);
        PageInfo page = new PageInfo(infoList);
        /**
         * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
         */
        page.setList(new ArrayList());

        return super.ok().put("infoList", infoList).put("webPage", page);
    }

    @ResponseBody
    @RequestMapping("/addCustomer")
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult addCustomer(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        //创建者id
        String userId = getAppUserId(parameterMap);

        String id = Tool.convertObject(parameterMap.get("id"));
        String custSn = Tool.convertObject(parameterMap.get("custSn"));
        String custFullName = Tool.convertObject(parameterMap.get("custFullName"));
        String custShortName = Tool.convertObject(parameterMap.get("custShortName"));
        String custProvince = Tool.convertObject(parameterMap.get("custProvince"));
        String custCategory = Tool.convertObject(parameterMap.get("custCategory"));
        String custRegion = Tool.convertObject(parameterMap.get("custRegion"));
        String custAssets = Tool.convertObject(parameterMap.get("custAssets"));
        String businessScope = Tool.convertObject(parameterMap.get("businessScope"));


        if (StringUtils.isEmpty(custFullName)) {
            return super.error("客户名称不能为空");
        }
        if (StringUtils.isEmpty(custProvince)) {
            return super.error("省份不能为空");
        }
        if (StringUtils.isEmpty(custShortName)) {
            return super.error("客户简称不能为空");
        }
        if (StringUtils.isEmpty(custCategory)) {
            return super.error("银行类型不能为空");
        }
        if (StringUtils.isEmpty(custRegion)) {
            return super.error("所属区域不能为空");
        }
        if (StringUtils.isEmpty(custAssets)) {
            return super.error("银行放贷规模及资产规模不能为空");
        }
        if (StringUtils.isEmpty(businessScope)) {
            return super.error("异地展业范围不能为空");
        }




        CrmCustomerInfo entity = new CrmCustomerInfo();
        //entity.setId(id);
        //entity.setCustSn(custSn);
        entity.setCustFullName(custFullName);
        entity.setCustShortName(custShortName);
        entity.setCustProvince(custProvince);
        entity.setCustCategory(custCategory);
        entity.setCustRegion(custRegion);
        entity.setCustAssets(custAssets);
        entity.setBusinessScope(businessScope);

        entity.setCreateUserId(userId);
        entity.setCreateTime(new Date());
        entity.setUpdateUserId(userId);
        entity.setUpdateTime(new Date());
        /**
         * 权限控制人员
         */
        List<CrmCustomerOwner> items = new ArrayList<>();
        //创建人（默认添加花总和姚总）
        CrmCustomerOwner owner = new CrmCustomerOwner();
        owner.setId(null);
        owner.setUserId(userId);
        owner.setAuthTag("2");
        owner.setCustId(id);
        items.add(owner);

        entity.setPowerDatas(items);

        if(StringUtils.isEmpty(id)){
            //客户id
            if(StringUtils.isEmpty(id)){
                id = SnowflakeIdUtil.getId();
                entity.setId(id);
            }
            //生成编号
            if(StringUtils.isEmpty(custSn)){
                String currDay = DateUtils.getCurrentDay();
                custSn = sysSeqService.getSeqNumber(Tool.convertStringToInt(SeqTypeEnum.customer.toString()), currDay);
                entity.setCustSn(custSn);
            }
            crmCustomerInfoService.insertEntity(entity);
        } else {
            entity.setId(id);
            entity.setCustSn(custSn);
            crmCustomerInfoService.updateEntity(entity);
        }


        return super.ok().put("id", entity.getId());
    }


    @ResponseBody
    @RequestMapping("/personList")
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult personList(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        //创建者id
        String userId = getAppUserId(parameterMap);

        String custId = Tool.convertObject(parameterMap.get("custId"));

        //查询银行干系人列表
        List<CrmCustomerStakeholder> infoList = stakeholderService.selectEntityByCustId(custId);

        return super.ok().put("infoList", infoList);
    }

    /**
     * 添加客户的组织架构
     *
     * @param request
     * @return
     */
    @ResponseBody
    @RequestMapping("/addPerson")
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult addPerson(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String id = Tool.convertObject(parameterMap.get("id"));
        String custId = Tool.convertObject(parameterMap.get("custId"));
        String name = Tool.convertObject(parameterMap.get("name"));
        String gender = Tool.convertObject(parameterMap.get("gender"));
        String phonenumber = Tool.convertObject(parameterMap.get("phonenumber"));
        String postName = Tool.convertObject(parameterMap.get("postName"));
        String deptName = Tool.convertObject(parameterMap.get("deptName"));
        String parentId = Tool.convertObject(parameterMap.get("parentId"));
        String memo = Tool.convertObject(parameterMap.get("memo"));
        String bankStructure = Tool.convertObject(parameterMap.get("bankStructure"));
        String businessDept = Tool.convertObject(parameterMap.get("businessDept"));
        String cardOssId = Tool.convertObject(parameterMap.get("cardOssId"));
        String flag = Tool.convertObject(parameterMap.get("flag"));
        String leaderPostName = Tool.convertObject(parameterMap.get("leaderPostName"));//领导电话
        String leaderName = Tool.convertObject(parameterMap.get("leaderName"));//领导姓名

        if (StringUtils.isEmpty(custId)) {
            return super.error("干系人未关联银行信息");
        }
        if (StringUtils.isEmpty(name)) {
            return super.error("姓名不能为空");
        }
        if (StringUtils.isEmpty(postName)) {
            return super.error("职务不能为空");
        }
        if (StringUtils.isEmpty(phonenumber)) {
            return super.error("电话号码不能为空");
        }

        CrmCustomerStakeholder entity = new CrmCustomerStakeholder();


        entity.setPhonenumber(phonenumber);
        entity.setCardOssId(cardOssId);
        entity.setMemo(memo);
        entity.setParentId(parentId);
        entity.setPostName(postName);
        entity.setDeptName(deptName);
        entity.setGender(gender);
        entity.setName(name);
        entity.setCustId(custId);
        entity.setFlag(flag);
        entity.setBankStructure(bankStructure);
        entity.setBusinessDept(businessDept);
        entity.setLeaderName(leaderName);
        entity.setLeaderPostName(leaderPostName);
        if (id.equals("")) {
            entity.setId(SnowflakeIdUtil.getId());
            stakeholderService.insertEntity(entity);
        } else {
            entity.setId(id);
            stakeholderService.updateEntity(entity);
        }

        return super.ok().put("id", entity.getId());
    }


    /**
     * 删除附件
     */
    @RequestMapping("/deleteAttachment")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult deleteAttachment(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);
        String id = Tool.convertObject(parameterMap.get("id"));
        sysOssService.deleteByIds(id);
        return super.ok();
    }
}
