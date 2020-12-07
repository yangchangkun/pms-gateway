package com.pms.app.clouddisk;

import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.prefer.domain.PreferClouddiskAttachment;
import com.pms.core.prefer.domain.PreferClouddiskDir;
import com.pms.core.prefer.domain.PreferClouddiskOwner;
import com.pms.core.prefer.service.IPreferClouddiskAttachmentService;
import com.pms.core.prefer.service.IPreferClouddiskDirService;
import com.pms.core.prefer.service.IPreferClouddiskOwnerService;
import com.pms.core.system.domain.SysUser;
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
 * 云盘管理
 * 
 * @author yangchangkun
 */
@Controller
@RequestMapping("/app/clouddisk/disk")
public class CloudDiskController extends AppBaseController
{

    @Autowired
    private IPreferClouddiskDirService preferClouddiskDirService;
    @Autowired
    private IPreferClouddiskOwnerService preferClouddiskOwnerService;
    @Autowired
    private IPreferClouddiskAttachmentService preferClouddiskAttachmentService;

    @Autowired
    private ISysUserService userService;

    /**
     * 查询列表
     * @return
     */
    @RequestMapping("/disk")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult disk(HttpServletRequest request, HttpServletResponse response)
    {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);

        String dirId = Tool.convertObject(parameterMap.get("dirId"));

        /**
         * 查询当前目录的实体
         */
        PreferClouddiskDir dirEntity = new PreferClouddiskDir();
        if(!StringUtils.isEmpty(dirId)){
            dirEntity = preferClouddiskDirService.selectEntityById(dirId);
        }
        if(dirEntity==null){
            dirEntity = new  PreferClouddiskDir();
        }

        /**
         * 查询当前目录的管理员
         */
        List<PreferClouddiskOwner> ownerList = new ArrayList<>();
        if(!StringUtils.isEmpty(dirId)){
            ownerList = preferClouddiskOwnerService.selectOwnerListByDirId(dirId);
        }

        /**
         * 查询下级目录列表
         */
        List<PreferClouddiskDir> childDirList = preferClouddiskDirService.selectMyDiskList(userId, dirId);

        /**
         * 查询当前目录的附件
         */
        List<PreferClouddiskAttachment> attachmentList = preferClouddiskAttachmentService.selectAttachmentListByDirId(dirId);

        return super.ok()
                .put("dirEntity", dirEntity)
                .put("ownerList", ownerList)
                .put("childDirList", childDirList)
                .put("attachmentList", attachmentList);

    }

    /**
     * 查询目录
     * @return
     */
    @RequestMapping("/childDirList")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult childDirList(HttpServletRequest request, HttpServletResponse response)
    {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);

        String dirId = Tool.convertObject(parameterMap.get("dirId"));

        /**
         * 查询下级目录列表
         */
        List<PreferClouddiskDir> childDirList = preferClouddiskDirService.selectMyDiskList(userId, dirId);

        return super.ok()
                .put("childDirList", childDirList);

    }

    /**
     * 添加目录
     */
    @RequestMapping("/addDir")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult addDir(HttpServletRequest request, HttpServletResponse response)
    {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);
        /**
         * 添加目录时所在上级目录的ID
         */
        String parentDirId = Tool.convertObject(parameterMap.get("parentDirId"));
        String dirName = Tool.convertObject(parameterMap.get("dirName"));
        int dirType = Tool.convertStringToInt(parameterMap.get("dirType"));
        if(StringUtils.isEmpty(dirName)){
            return super.error("dirName参数不能为空");
        }

        PreferClouddiskDir dirBean = preferClouddiskDirService.selectDiskByName(dirName, parentDirId);
        if(dirBean!=null){
            return super.error("您要创建的目录名称重复");
        }

        SysUser userInfo = userService.selectUserById(userId);

        PreferClouddiskDir dirEntity = new PreferClouddiskDir();
        dirEntity.setParentId(parentDirId);
        dirEntity.setName(dirName);
        dirEntity.setCategory(1);
        dirEntity.setType(dirType);
        dirEntity.setState(0);
        dirEntity.setCreateBy(userInfo.getUserId());
        dirEntity.setCreateTime(new Date());
        preferClouddiskDirService.insertEntity(dirEntity);

        PreferClouddiskOwner ownerEntity = new PreferClouddiskOwner();
        ownerEntity.setDirId(dirEntity.getId());
        ownerEntity.setUserId(userId);
        ownerEntity.setType(0);
        preferClouddiskOwnerService.insertEntity(ownerEntity);

        return super.ok();
    }

    /**
     * 修改目录
     */
    @RequestMapping("/editDir")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult editDir(HttpServletRequest request, HttpServletResponse response)
    {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);
        /**
         * 添加目录时所在上级目录的ID
         */
        String dirId = Tool.convertObject(parameterMap.get("dirId"));
        String parentDirId = Tool.convertObject(parameterMap.get("parentDirId"));
        String dirName = Tool.convertObject(parameterMap.get("dirName"));
        int dirType = Tool.convertStringToInt(parameterMap.get("dirType"));
        if(StringUtils.isEmpty(parentDirId)){
            return super.error("parentDirId参数不能为空");
        }
        if(StringUtils.isEmpty(dirId)){
            return super.error("dirId参数不能为空");
        }
        if(StringUtils.isEmpty(dirName)){
            return super.error("dirName参数不能为空");
        }

        PreferClouddiskDir dirEntity = preferClouddiskDirService.selectEntityById(dirId);
        if(dirEntity==null){
            return super.error("您要修改的目录不存在");
        }

        PreferClouddiskDir dirBean = preferClouddiskDirService.selectDiskByName(dirName, parentDirId);
        if (dirBean!=null && !dirBean.getId().equals(dirId))
        {
            return super.error("您要修改的目录名称重复");
        }

        List<PreferClouddiskOwner> ownerList = preferClouddiskOwnerService.selectOwnerListByDirId(dirId);
        boolean allowDelTag = false;
        if(ownerList!=null && ownerList.size()>0){
            for(PreferClouddiskOwner bean : ownerList){
                if(bean.getUserId().equals(userId)){
                    allowDelTag = true;
                    break;
                }
            }
        }
        if(!allowDelTag){
            return super.error("您不是该目录的管理员,不能删除该目录");
        }

        SysUser userInfo = userService.selectUserById(userId);

        dirEntity.setName(dirName);
        dirEntity.setType(dirType);
        dirEntity.setUpdateBy(userInfo.getUserId());
        dirEntity.setUpdateTime(new Date());
        preferClouddiskDirService.updateEntity(dirEntity);

        return super.ok();
    }

    /**
     * 删除目录
     */
    @RequestMapping("/removeDir")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult removeDir(HttpServletRequest request, HttpServletResponse response)
    {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);

        String dirId = Tool.convertObject(parameterMap.get("dirId"));
        if(StringUtils.isEmpty(dirId)){
            return super.error("dirId参数不能为空");
        }

        PreferClouddiskDir dirEntity = preferClouddiskDirService.selectEntityById(dirId);
        if(dirEntity==null){
            return super.error("您要删除的目录不存在");
        }

        List<PreferClouddiskOwner> ownerList = preferClouddiskOwnerService.selectOwnerListByDirId(dirId);
        boolean allowDelTag = false;
        if(ownerList!=null && ownerList.size()>0){
            for(PreferClouddiskOwner bean : ownerList){
                if(bean.getUserId().equals(userId)){
                    allowDelTag = true;
                    break;
                }
            }
        }
        if(!allowDelTag){
            return super.error("您不是该目录的管理员,不能删除该目录");
        }

        preferClouddiskDirService.deleteByIds(dirId);

        return super.ok();
    }

    /**
     * 分配管理员
     */
    @RequestMapping("/allocationOwner")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult allocationOwner(HttpServletRequest request, HttpServletResponse response)
    {
        /**
         * 所有参数
         */
        Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
        String userId = getAppUserId(parameterMap);

        /**
         * 添加目录时所在上级目录的ID
         */
        String dirId = Tool.convertObject(parameterMap.get("dirId"));
        String ownerUserIds = Tool.convertObject(parameterMap.get("ownerUserIds"));
        if(StringUtils.isEmpty(dirId)){
            return super.error("dirId参数不能为空");
        }
        if(StringUtils.isEmpty(ownerUserIds)){
            return super.error("ownerUserIds参数不能为空");
        }

        /**
         * 目录创建者
         */
        PreferClouddiskOwner dirCreateUser = null;
        List<PreferClouddiskOwner> ownerList = preferClouddiskOwnerService.selectOwnerListByDirId(dirId);
        if(ownerList!=null && ownerList.size()>0){
            for(PreferClouddiskOwner bean : ownerList){
                if(bean.getType()==0){
                    dirCreateUser = bean;
                    break;
                }
            }
        }

        List<PreferClouddiskOwner> datas = new ArrayList<>();
        String[] ownerUserIdArray = ownerUserIds.split(",");
        for(String ownerUserId : ownerUserIdArray){
            if(dirCreateUser!=null && !ownerUserId.equals(dirCreateUser.getUserId())){
                PreferClouddiskOwner ownerEntity = new PreferClouddiskOwner();
                ownerEntity.setId(SnowflakeIdUtil.getId());
                ownerEntity.setDirId(dirId);
                ownerEntity.setUserId(userId);
                ownerEntity.setType(1);
                datas.add(ownerEntity);
            }
        }

        if(datas!=null && datas.size()>0){
            preferClouddiskOwnerService.allocationOwner(dirId, datas);
        }

        return super.ok();
    }

}