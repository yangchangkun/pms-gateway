package com.pms.app.contact;

import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.utils.StringUtils;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.hr.domain.StaffInfo;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.system.domain.SysDept;
import com.pms.core.system.domain.SysDeptManager;
import com.pms.core.system.domain.SysUser;
import com.pms.core.system.service.ISysDeptManagerService;
import com.pms.core.system.service.ISysDeptService;
import com.pms.core.system.service.ISysUserService;
import com.pms.framework.web.base.AppBaseController;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 用户信息
 * 
 * @author yangchangkun
 */
@Controller
@RequestMapping("/app/contact")
public class ContactController extends AppBaseController
{

    @Autowired
    private IStaffInfoService staffInfoService;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private ISysDeptManagerService sysDeptManagerService;

    /**
     * 查询联系人列表
     * @return
     */
    @RequestMapping("/list")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult list(HttpServletRequest request, HttpServletResponse response)
    {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String searchKey = Tool.convertObject(parameterMap.get("searchKey"));
        String deptId = Tool.convertObject(parameterMap.get("deptId"));

        List<StaffInfo> datas = staffInfoService.selectContactList(searchKey, deptId);
        /**
         * 数据脱敏
         */
        List<StaffInfo> list = new ArrayList<>();
        if(datas!=null && datas.size()>0){
            for(StaffInfo staffInfo : datas){
                /**
                 * 手机号脱敏
                 */
                staffInfo.setPhonenumber(Tool.mobileEncrypt(staffInfo.getPhonenumber()));
                /**
                 * 岗位脱敏
                 */
                if(StringUtils.isNotEmpty(staffInfo.getPostName())){
                    staffInfo.setPostName("***");
                }
                list.add(staffInfo);
            }
        }

        List<Map<String, Object>> deptList = staffInfoService.groupStaffByDept();

        return super.ok().put("gridDatas", list).put("deptList", deptList);

    }

    /**
     * 查询联系人列表
     * @return
     */
    @RequestMapping("/select")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult select(HttpServletRequest request, HttpServletResponse response)
    {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String searchKey = Tool.convertObject(parameterMap.get("searchKey"));
        String deptId = Tool.convertObject(parameterMap.get("deptId"));
        String status = Tool.convertObject(parameterMap.get("status"));

        List<StaffInfo> datas = staffInfoService.selectContactList(searchKey, deptId, status);
        /**
         * 数据脱敏
         */
        List<StaffInfo> list = new ArrayList<>();
        if(datas!=null && datas.size()>0){
            for(StaffInfo staffInfo : datas){
                /**
                 * 手机号脱敏
                 */
                staffInfo.setPhonenumber(Tool.mobileEncrypt(staffInfo.getPhonenumber()));
                /**
                 * 岗位脱敏
                 */
                if(StringUtils.isNotEmpty(staffInfo.getPostName())){
                    staffInfo.setPostName("***");
                }
                list.add(staffInfo);
            }
        }

        List<Map<String, Object>> deptList = staffInfoService.groupStaffByDept();

        return super.ok().put("gridDatas", list).put("deptList", deptList);

    }

    /**
     * 查询联系人列表
     * 只返回简要信息
     * @return
     */
    @RequestMapping("/simpleList")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult simpleList(HttpServletRequest request, HttpServletResponse response)
    {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String searchKey = Tool.convertObject(parameterMap.get("searchKey"));
        String deptId = Tool.convertObject(parameterMap.get("deptId"));

        List<StaffInfo> datas = staffInfoService.selectContactSimpleList(searchKey, deptId);
        /**
         * 数据脱敏
         */
        List<StaffInfo> list = new ArrayList<>();
        if(datas!=null && datas.size()>0){
            for(StaffInfo staffInfo : datas){
                /**
                 * 手机号脱敏
                 */
                staffInfo.setPhonenumber(Tool.mobileEncrypt(staffInfo.getPhonenumber()));
                /**
                 * 岗位脱敏
                 */
                if(StringUtils.isNotEmpty(staffInfo.getPostName())){
                    staffInfo.setPostName("***");
                }
                list.add(staffInfo);
            }
        }
        List<Map<String, Object>> deptList = staffInfoService.groupStaffByDept();

        return super.ok().put("gridDatas", list).put("deptList", deptList);

    }

    /**
     * 查询职员详情
     */
    @RequestMapping("/detail")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult detail(HttpServletRequest request, HttpServletResponse response)
    {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = Tool.convertObject(parameterMap.get("userId"));

        StaffInfo staff = staffInfoService.selectEntityById(userId);
        /**
         * 手机号脱敏
         */
        staff.setPhonenumber(Tool.mobileEncrypt(staff.getPhonenumber()));
        /**
         * 岗位脱敏
         */
        if(StringUtils.isNotEmpty(staff.getPostName())){
            staff.setPostName("***");
        }

        SysUser user = sysUserService.selectUserById(userId);
        user.setPhonenumber(Tool.mobileEncrypt(user.getPhonenumber()));

        return super.ok().put("staff", staff).put("user", user);
    }



    /**
     * 查询组织结构
     */
    @RequestMapping("/organization")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult organization(HttpServletRequest request, HttpServletResponse response)
    {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = Tool.convertObject(parameterMap.get("userId"));

        Map<String, Object> resultMap = staffInfoService.selectOrganization();
        List<Map<String, Object>> deptList = (List<Map<String, Object>>) resultMap.get("deptList");
        List<StaffInfo> staffList = (List<StaffInfo>) resultMap.get("staffList");

        return super.ok().put("deptList", deptList).put("staffList", staffList);
    }


    /**
     * 部门职员管理
     * 通常用于判断是否具有调整权限（部门管理员）
     */
    @RequestMapping("/deptStaffManagerInit")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult deptStaffManagerInit(HttpServletRequest request, HttpServletResponse response)
    {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap); //当前操作人员的用户ID
        String deptId = Tool.convertObject(parameterMap.get("deptId"));

        List<SysDeptManager> managerList = sysDeptManagerService.selectListByUserId(userId);
        if(managerList==null || managerList.size()<=0){
            return super.error("您尚未成为部门管理员,不能维护部门职员信息.");
        }

        SysDept dept = deptService.selectDeptById(deptId);
        /**
         * 当前要管理的部门祖级ID（包括当前部门）
         */
        String cDeptAncestors = dept.getAncestors().replaceAll("'", "")+","+dept.getDeptId();

        /**
         * 判断当前要维护的部门是否是自己管辖的部门范围内
         * 以自己管理的当前要维护的部门的祖级部门的最高层级的部门为准（即范围最大化原则）
         */
        String governDeptAncestors = "";
        for(SysDeptManager deptManager : managerList){
            String mDeptAncestors = deptManager.getAncestors().replaceAll("'", "")+","+deptManager.getDeptId();
            if(cDeptAncestors.indexOf(mDeptAncestors)!=-1 || cDeptAncestors.equals(mDeptAncestors)){
                governDeptAncestors = mDeptAncestors;
                break;
            }
        }

        if(StringUtils.isEmpty(governDeptAncestors)){
            return super.error("您不是该部门管理员,或该部门不在您管辖范围内,不能维护部门职员信息.");
        }

        /**
         * 当前部门下职员
         */
        List<StaffInfo> currStaffList = staffInfoService.selectSimpleListByDeptId(deptId);
        /**
         * 管辖部门范围内的职员
         */
        List<StaffInfo> governStaffList = staffInfoService.selectSimpleListByDeptAncestors(governDeptAncestors);

        return super.ok()
                .put("dept", dept)
                .put("currStaffList", currStaffList)
                .put("governStaffList", governStaffList);
    }


    /**
     * 批量调整部门下的人员
     */
    @RequestMapping("/deptStaffManagerSave")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult deptStaffManagerSave(HttpServletRequest request, HttpServletResponse response)
    {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap); //当前操作人员的用户ID

        String deptId = Tool.convertObject(parameterMap.get("deptId")); //部门ID
        String userIds = Tool.convertObject(parameterMap.get("userIds")); //职员userId（多个用半角逗号分隔）
        if(StringUtils.isEmpty(deptId)){
            return super.error("部门ID参数不能为空");
        }
        if(StringUtils.isEmpty(userIds)){
            return super.error("用户ID参数不能为空");
        }

        SysDept dept = deptService.selectDeptById(deptId);
        List<String> staffUserIds = Arrays.asList(userIds.split(",")); //[a, b, c]

        /**
         * 批量变更员工部门
         */
        staffInfoService.batchAdjustDept(dept.getDeptId(), dept.getDeptName(), staffUserIds);

        return super.ok();
    }

}