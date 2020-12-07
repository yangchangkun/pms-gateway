package com.pms.app.notice;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.core.adm.domain.AdmNotice;
import com.pms.core.adm.domain.AdmNoticeAttachment;
import com.pms.core.adm.domain.AdmNoticeReader;
import com.pms.core.adm.service.IAdmNoticeAttachmentService;
import com.pms.core.adm.service.IAdmNoticeReaderService;
import com.pms.core.adm.service.IAdmNoticeService;
import com.pms.core.attendance.domain.OaDailyPaperEntity;
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
 * 公告管理
 * 
 * @author yangchangkun
 */
@Controller
@RequestMapping("/app/adm/notice")
public class NoticeController extends AppBaseController
{

    @Autowired
    private IAdmNoticeService admNoticeService;
    @Autowired
    private IAdmNoticeAttachmentService admNoticeAttachmentService;
    @Autowired
    private IAdmNoticeReaderService admNoticeReaderService;

    @Autowired
    private ISysUserService userService;

    /**
     * 查询列表
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
        String userId = getAppUserId(parameterMap);

        String searchKey = Tool.convertObject(parameterMap.get("searchKey"));

        int pageNum = Tool.convertStringToInt(parameterMap.get("pageNum"));
        if(pageNum<=0){
            pageNum = 1;
        }
        int pageSize = Tool.convertStringToInt(parameterMap.get("pageSize"));
        if(pageSize<=0 || pageSize>50){
            pageSize = 10;
        }

        String orderBy = " topFlag desc, readTag asc, createTime desc ";

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("state", "1");
        params.put("searchKey", searchKey);

        PageHelper.startPage(pageNum, pageSize, orderBy);
        List<Map<String, Object>> list = admNoticeService.selectMyNoticeList(params);
        PageInfo page = new PageInfo(list);
        /**
         * todo 这里是将page里面的list置空，避免返回客户端数据的时候出现双重数据
         */
        page.setList(new ArrayList());

        return super.ok().put("gridDatas", list).put("webPage", page);

    }

    /**
     * 查询详情
     * @return
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
        String userId = getAppUserId(parameterMap);

        String noticeId = Tool.convertObject(parameterMap.get("noticeId"));

        AdmNotice entity = admNoticeService.selectEntityById(noticeId);

        List<AdmNoticeAttachment> attachmentList = admNoticeAttachmentService.selectEntityByNoticeId(entity.getId());

        /**
         * 保存读取信息
         */
        AdmNoticeReader reader = admNoticeReaderService.selectEntityByNoticeIdAndUserId(noticeId, userId);
        if(reader!=null){
            reader.setReadTag("1");
            reader.setCreateTime(new Date());
            admNoticeReaderService.updateEntity(reader);
        }


        return super.ok().put("entity", entity).put("attachmentList", attachmentList);

    }

}