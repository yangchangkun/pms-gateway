package com.pms.app.common;


import com.pms.common.annotation.GatewayAuth;
import com.pms.common.base.AjaxResult;
import com.pms.common.config.PlatConfigUtils;
import com.pms.common.constant.GatewayConstants;
import com.pms.common.enums.FileResTypeEnum;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.SnowflakeIdUtil;
import com.pms.common.utils.StringUtils;
import com.pms.common.utils.Tool;
import com.pms.common.utils.file.FileType;
import com.pms.common.utils.http.HttpUtil;
import com.pms.common.utils.http.RequestUtil;
import com.pms.common.utils.image.SimpleImageUtil;
import com.pms.core.hr.service.IStaffInfoService;
import com.pms.core.system.domain.SysOss;
import com.pms.core.system.service.ISysOssService;
import com.pms.framework.web.base.AppBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/app/common")
public class ApiUploadController extends AppBaseController {

    private String TAG = this.getClass().getSimpleName();

    @Autowired
    private PlatConfigUtils platConfigUtils;

    @Autowired
    private ISysOssService sysOssService;

    @Autowired
    private IStaffInfoService staffInfoService;

    /**
     * 可操作的资源列表集合
     */
    public static Map<String, String> resTypeMap = new HashMap<String, String>();
    static {
        for (FileResTypeEnum inter : FileResTypeEnum.values()) {
            resTypeMap.put(inter.getResType(), inter.getResDesc());
        }
    }

    /**
     * 批量上传
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/batchUpload")
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult batchUpload(MultipartHttpServletRequest request, HttpServletResponse response) throws Exception{
        List<SysOss> osses = new ArrayList<>();

        //处理文件上传(支持批量)
        if (request instanceof MultipartHttpServletRequest) {
            /**
             * 所有参数
             */
            Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
            String memberId = getAppUserId(parameterMap);

            String resType = Tool.convertObject(parameterMap.get("resType"));
            if (StringUtils.isEmpty(resType)) {
                return super.error("文件类别不能为空");
            }
            if (!resTypeMap.containsKey(resType)) {
                 return super.error("文件类别不存在");
            }
            logger.info( "batchUpload.resType="+resType);

            String uploadFilePath = platConfigUtils.getOssUploadFilePath(); // 上传文件的保存地址
            logger.info( "batchUpload.uploadFilePath.before="+uploadFilePath);
            String localFileDir = resType + File.separator + DateUtils.getFileDirsByDate();
            if(StringUtils.isNotEmpty(resType)){
                uploadFilePath += localFileDir;
            }
            File targetDir= new File(uploadFilePath);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            logger.info( "batchUpload.uploadFilePath.after="+uploadFilePath);

            List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");

            for (MultipartFile file : files) {
                if (file != null) {

                    //上传文件
                    String originalFilename = file.getOriginalFilename();
                    logger.info( "batchUpload.originalFilename="+originalFilename);
                    int lastComma = file.getOriginalFilename().lastIndexOf(".");
                    String suffix = originalFilename.substring(lastComma);
                    logger.info( "batchUpload.suffix="+suffix);
                    String uuid = SnowflakeIdUtil.getId();
                    String uploadFileName = uuid + suffix; //重新命名文件，便于下载
                    logger.info( "batchUpload.uploadFileName="+uploadFileName);

                    String fileType = FileType.fileType(originalFilename);
                    logger.info( "batchUpload.fileType="+fileType);

                    String localFilePath = localFileDir + File.separator + uploadFileName;
                    logger.info( "batchUpload.localFilePath="+localFilePath);
                    String ossUrl = platConfigUtils.getOssUrl() + File.separator + localFilePath;
                    logger.info( "batchUpload.ossUrl="+ossUrl);

                    File targetFile= new File(uploadFilePath, uploadFileName);
                    file.transferTo(targetFile);

                    SysOss oss = new SysOss();
                    oss.setId(uuid);
                    oss.setResType(resType);
                    oss.setResKey("");
                    oss.setName(originalFilename);
                    oss.setDescription(originalFilename);
                    oss.setUrl(ossUrl);
                    oss.setFilePath(uploadFilePath);
                    oss.setFileName(uploadFileName);
                    oss.setFileType(fileType);
                    oss.setCreateUserId(memberId);
                    osses.add(oss);
                }
            }
            if(osses!=null && osses.size()>0){
                for(SysOss oss : osses){
                    sysOssService.insertEntity(oss);
                }
            } else {
                return super.error("附件类型为必填项");
            }

        } else {
            return super.error("不是multipart/form-data格式");
        }

        return super.ok().put("osses", osses);
    }

    /**
     * 单文件上传
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/upload")
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult upload(MultipartHttpServletRequest request, HttpServletResponse response) throws Exception{
        SysOss oss = new SysOss();

        //处理文件上传(支持批量)
        if (request instanceof MultipartHttpServletRequest) {
            /**
             * 所有参数
             */
            Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
            String memberId = getAppUserId(parameterMap);

            String resType = Tool.convertObject(parameterMap.get("resType"));
            if (StringUtils.isEmpty(resType)) {
                return super.error("文件类别不能为空");
            }
            if (!resTypeMap.containsKey(resType)) {
                return super.error("文件类别不存在");
            }
            logger.info( "upload.resType="+resType);

            String uploadFilePath = platConfigUtils.getOssUploadFilePath(); // 上传文件的保存地址
            logger.info( "upload.uploadFilePath.before="+uploadFilePath);
            String localFileDir = resType + File.separator + DateUtils.getFileDirsByDate();
            if(StringUtils.isNotEmpty(resType)){
                uploadFilePath += localFileDir;
            }
            File targetDir= new File(uploadFilePath);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            logger.info( "upload.uploadFilePath.after="+uploadFilePath);

            List<MultipartFile> files = null;
            if (request instanceof MultipartHttpServletRequest) {
                files = ((MultipartHttpServletRequest) request).getFiles("file");
            }
            MultipartFile file = null;
            if(files!=null && files.size()>0){
                file = files.get(0);
            }

            if (file != null) {

                //上传文件
                String originalFilename = file.getOriginalFilename();
                logger.info( "upload.originalFilename="+originalFilename);
                int lastComma = file.getOriginalFilename().lastIndexOf(".");
                String suffix = originalFilename.substring(lastComma);
                logger.info( "upload.suffix="+suffix);
                String uuid = SnowflakeIdUtil.getId();
                String uploadFileName = uuid + suffix; //重新命名文件，便于下载
                logger.info( "upload.uploadFileName="+uploadFileName);

                String fileType = FileType.fileType(originalFilename);
                logger.info( "upload.fileType="+fileType);

                String localFilePath = localFileDir + File.separator + uploadFileName;
                logger.info( "upload.localFilePath="+localFilePath);
                String ossUrl = platConfigUtils.getOssUrl() + File.separator + localFilePath;
                logger.info( "upload.ossUrl="+ossUrl);

                File targetFile= new File(uploadFilePath, uploadFileName);
                file.transferTo(targetFile.getAbsoluteFile());

                oss.setId(uuid);
                oss.setResType(resType);
                oss.setResKey("");
                oss.setName(originalFilename);
                oss.setDescription(originalFilename);
                oss.setUrl(ossUrl);
                oss.setFilePath(uploadFilePath);
                oss.setFileName(uploadFileName);
                oss.setFileType(fileType);
                oss.setCreateUserId(memberId);

                sysOssService.insertEntity(oss);

            }

        } else {
            return super.error("不是multipart/form-data格式");
        }

        return super.ok().put("oss", oss);
    }


    /**
     * 头像上传
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/avatarUpload")
    @GatewayAuth(GatewayConstants.require_login)
    public AjaxResult avatarUpload(MultipartHttpServletRequest request, HttpServletResponse response) throws Exception{
        SysOss oss = new SysOss();

        //处理文件上传(支持批量)
        if (request instanceof MultipartHttpServletRequest) {
            /**
             * 所有参数
             */
            Map<String, Object> parameterMap = RequestUtil.paramToMap(request);
            String memberId = getAppUserId(parameterMap);

            String resType = FileResTypeEnum.avatar.getResType();

            String uploadFilePath = platConfigUtils.getOssUploadFilePath(); // 上传文件的保存地址
            logger.info( "avatarUpload.uploadFilePath.before="+uploadFilePath);
            String localFileDir = resType + File.separator + DateUtils.getFileDirsByDate();
            if(StringUtils.isNotEmpty(resType)){
                uploadFilePath += localFileDir;
            }
            File targetDir= new File(uploadFilePath);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            logger.info( "avatarUpload.uploadFilePath.after="+uploadFilePath);

            List<MultipartFile> files = null;
            if (request instanceof MultipartHttpServletRequest) {
                files = ((MultipartHttpServletRequest) request).getFiles("file");
            }
            MultipartFile file = null;
            if(files!=null && files.size()>0){
                file = files.get(0);
            }

            if (file != null) {

                //上传文件
                String originalFilename = file.getOriginalFilename();
                logger.info( "avatarUpload.originalFilename="+originalFilename);
                int lastComma = file.getOriginalFilename().lastIndexOf(".");
                String suffix = originalFilename.substring(lastComma);
                logger.info( "avatarUpload.suffix="+suffix);
                String uuid = SnowflakeIdUtil.getId();
                String uploadFileName = uuid + suffix; //重新命名文件，便于下载
                logger.info( "avatarUpload.uploadFileName="+uploadFileName);

                String fileType = FileType.fileType(originalFilename);
                logger.info( "avatarUpload.fileType="+fileType);

                String localFilePath = localFileDir + File.separator + uploadFileName;
                logger.info( "avatarUpload.localFilePath="+localFilePath);
                String ossUrl = platConfigUtils.getOssUrl() + File.separator + localFilePath;
                logger.info( "avatarUpload.ossUrl="+ossUrl);

                File targetFile = new File(uploadFilePath, uploadFileName);
                file.transferTo(targetFile.getAbsoluteFile());

                /**
                 * 压缩图片
                 */
                /*String thumbnailFileName = uuid+"_thumbnail"+suffix; //压缩后的文件名称
                String srcFilePath = uploadFilePath + File.separator + uploadFileName;
                String descFilePath = uploadFilePath + File.separator + thumbnailFileName;
                int width = 480;
                int height = 480;
                SimpleImageUtil.compress(srcFilePath, descFilePath, width, height);*/

                oss.setId(uuid);
                oss.setResType(resType);
                oss.setResKey("");
                oss.setName(originalFilename);
                oss.setDescription(originalFilename);
                oss.setUrl(ossUrl);
                oss.setFilePath(uploadFilePath);
                oss.setFileName(uploadFileName);
                oss.setFileType(fileType);
                oss.setCreateUserId(memberId);

                sysOssService.insertEntity(oss);

                /**
                 * 修改用户头像
                 */
                staffInfoService.updatePhoto(ossUrl, memberId);
            }


        } else {
            return super.error("不是multipart/form-data格式");
        }

        return super.ok().put("oss", oss);
    }


    @RequestMapping(value = "/download/{resId}", method = RequestMethod.GET)
    public void download(@PathVariable("resId") String resId, HttpServletRequest request, HttpServletResponse response) {
        SysOss oss = sysOssService.selectEntity(resId);
        if(oss==null){
            return;
        }
        String filePath = oss.getFilePath();
        String fileName = oss.getFileName();

        //设置文件路径
        File file = new File(filePath , fileName);
        if (file.exists()) {
            response.setContentType("application/force-download");// 设置强制下载不打开
            response.addHeader("Content-Disposition", "attachment;fileName=" + HttpUtil.setFileDownloadHeader(request, oss.getName()));// 设置文件名
            byte[] buffer = new byte[1024];
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);
                OutputStream os = response.getOutputStream();
                int i = bis.read(buffer);
                while (i != -1) {
                    os.write(buffer, 0, i);
                    i = bis.read(buffer);
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(TAG, e);
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }


    /**
     * 预览图片
     * @param resId
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/previewImg/{resId}",produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_GIF_VALUE, MediaType.IMAGE_PNG_VALUE })
    @ResponseBody
    public byte[] previewImg(@PathVariable("resId") String resId) throws IOException {
        SysOss oss = sysOssService.selectEntity(resId);
        if(oss==null){
            return null;
        }
        String filePath = oss.getFilePath();
        String fileName = oss.getFileName();

        //设置文件路径
        File file = new File(filePath , fileName);

        FileInputStream inputStream = new FileInputStream(file);
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes, 0, inputStream.available());
        return bytes;
    }

}