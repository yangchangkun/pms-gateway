package com.pms.app.common;

import com.pms.activiti.service.ActProcessService;
import com.pms.activiti.service.impl.ActProcessServiceImpl;
import com.pms.common.page.WebPage;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.RequestUtil;
import com.pms.framework.web.base.AppBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 项目审批控制器
 *
 * @author yangchangkun
 * @create 2018-02-02 10:40
 */
@Controller
@RequestMapping("/app/common/act")
public class ActivitiHelpController extends AppBaseController {

	private String TAG = this.getClass().getSimpleName();

	@Autowired
	private ActProcessServiceImpl actProcessServiceImpl;

	/**
	 * 获取流程图像，已执行节点和流程线高亮显示
	 * 注意这里要在ShiroConfig的shirFilter进行filterMap.put("/activiti.jpg", "anon")配置;
	 */
	@RequestMapping("/getActivitiProccessImage")
	public void getActivitiProccessImage(HttpServletRequest request, HttpServletResponse response) {
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);

		try {

			String processInstanceId = Tool.convertObject(parameterMap.get("processInstanceId"));

			/**
			 * 获取图片
			 */
			InputStream imageStream = actProcessServiceImpl.getActivitiProccessImage(processInstanceId);

			response.setContentType("image/png");
			OutputStream os = response.getOutputStream();
			int bytesRead = 0;
			byte[] buffer = new byte[8192];
			while ((bytesRead = imageStream.read(buffer, 0, 8192)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			os.close();
			imageStream.close();
		} catch (Exception e) {
			logger.error(TAG, e);
			e.printStackTrace();
		}
	}


	/**
	 * 获取流程定义的图片
	 * 注意这里要在ShiroConfig的shirFilter进行filterMap.put("/activiti.jpg", "anon")配置;
	 */
	@RequestMapping("/getActivitiProccessDefinitionImage")
	public void getActivitiProccessDefinitionImage(HttpServletRequest request, HttpServletResponse response) {
		/**
		 * 所有参数
		 */
		Map<String, Object> parameterMap = RequestUtil.paramToMap(request);

		try {

			String key = Tool.convertObject(parameterMap.get("key"));

			int pageNum = Tool.convertStringToInt(0);
			int pageSize = Tool.convertStringToInt(1);
			WebPage page = new WebPage(pageNum, pageSize);

			List<Map<String, Object>> list = actProcessServiceImpl.selectProcessDefinitionList(parameterMap, page);
			if(list!=null && list.size()>0){

				Map<String, Object> resultMap = list.get(0);
				String deploymentId = Tool.convertObject(resultMap.get("deploymentId"));
				String diagramResourceName = Tool.convertObject(resultMap.get("diagramResourceName"));

				/**
				 * 获取图片
				 */
				InputStream imageStream = actProcessServiceImpl.findImageStream(deploymentId, diagramResourceName);

				response.setContentType("image/png");
				OutputStream os = response.getOutputStream();
				int bytesRead = 0;
				byte[] buffer = new byte[8192];
				while ((bytesRead = imageStream.read(buffer, 0, 8192)) != -1) {
					os.write(buffer, 0, bytesRead);
				}
				os.close();
				imageStream.close();
			}
		} catch (Exception e) {
			logger.error(TAG, e);
			e.printStackTrace();
		}
	}

}
