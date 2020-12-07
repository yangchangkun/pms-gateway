package com.pms.app.clouddisk;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 云盘文件管理
 * 
 * @author yangchangkun
 */
@Controller
@RequestMapping("/app/clouddisk/attachment")
public class CloudDiskAttachmentController extends AppBaseController
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
     * 文件列表
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
        String userId = getAppUserId(parameterMap);
        /**
         * 添加目录时所在上级目录的ID
         */
        String dirId = Tool.convertObject(parameterMap.get("dirId"));

        List<PreferClouddiskAttachment> attachmentList = preferClouddiskAttachmentService.selectAttachmentListByDirId(dirId);

        return super.ok().put("attachmentList", attachmentList);
    }

    /**
     * 添加文件
     */
    @RequestMapping("/addFile")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult addFile(HttpServletRequest request, HttpServletResponse response)
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
        String accessory = Tool.convertObject(parameterMap.get("accessory"));
        if(StringUtils.isEmpty(dirId)){
            return super.error("dirId 参数不能为空");
        }
        if(StringUtils.isEmpty(accessory)){
            return super.error("accessory 参数不能为空");
        }
        JSONObject jo = JSON.parseObject(accessory);
        String id = jo.containsKey("id")?jo.getString("id"):"";
        String name = jo.containsKey("name")?jo.getString("name"):"";
        String description = jo.containsKey("description")?jo.getString("description"):"";
        String url = jo.containsKey("url")?jo.getString("url"):"";
        String filePath = jo.containsKey("filePath")?jo.getString("filePath"):"";
        String fileName = jo.containsKey("fileName")?jo.getString("fileName"):"";
        String fileType = jo.containsKey("fileType")?jo.getString("fileType"):"";

        List<PreferClouddiskOwner> ownerList = preferClouddiskOwnerService.selectOwnerListByDirId(dirId);
        boolean dirTag = false;
        if(ownerList!=null && ownerList.size()>0){
            for(PreferClouddiskOwner bean : ownerList){
                if(bean.getUserId().equals(userId)){
                    dirTag = true;
                    break;
                }
            }
        }
        if(!dirTag){
            return super.error("只有目录管理员或创建者才能上传文件");
        }

        PreferClouddiskAttachment entity = new PreferClouddiskAttachment();
        entity.setId(id);
        entity.setName(name);
        entity.setDescription(description);
        entity.setUrl(url);
        entity.setFilePath(filePath);
        entity.setFileName(fileName);
        entity.setFileType(fileType);
        entity.setDirId(dirId);
        entity.setCreateUserId(userId);
        entity.setCreateTime(new Date());
        preferClouddiskAttachmentService.insertEntity(entity);

        return super.ok();
    }

    /**
     * 删除文件
     */
    @RequestMapping("/delFile")
    @ResponseBody
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult delFile(HttpServletRequest request, HttpServletResponse response)
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
        String fileId = Tool.convertObject(parameterMap.get("fileId"));
        if(StringUtils.isEmpty(dirId)){
            return super.error("dirId参数不能为空");
        }
        if(StringUtils.isEmpty(fileId)){
            return super.error("fileId参数不能为空");
        }

        PreferClouddiskAttachment entity = preferClouddiskAttachmentService.selectEntityById(fileId);
        if(entity==null){
            return super.error("您要删除的文件不存在");
        }
        boolean createTag = entity.getCreateUserId().equals(userId);

        List<PreferClouddiskOwner> ownerList = preferClouddiskOwnerService.selectOwnerListByDirId(dirId);
        boolean dirTag = false;
        if(ownerList!=null && ownerList.size()>0){
            for(PreferClouddiskOwner bean : ownerList){
                if(bean.getUserId().equals(userId)){
                    dirTag = true;
                    break;
                }
            }
        }

        if(!createTag && !dirTag){
            return super.error("只有目录管理员或者文件上传至本人才能删除该文件");
        }

        preferClouddiskAttachmentService.deleteByIds(fileId);

        return super.ok();
    }


}