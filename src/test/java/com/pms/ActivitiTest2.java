package com.pms;

import com.pms.activiti.config.ActBusinessTypeEnum;
import com.pms.activiti.domain.Act;
import com.pms.activiti.service.impl.ActBusinessServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;


@RunWith(SpringRunner.class)
@SpringBootTest
public class ActivitiTest2 {

    @Autowired
    private ActBusinessServiceImpl actBusinessServiceImpl;

    /**
     * 启动流程
     */
    @Test
    public void test2() {
        actBusinessServiceImpl.startProcess("yangchangkun", "yangchangkun", ActBusinessTypeEnum.attendance_overtime, "2c908e6361cfb0000161cfb18d260000", "这里是被备注", null);

    }

    /**
     * 查询任务
     */
    @Test
    public void test3() {
        List<Act> list = actBusinessServiceImpl.searchTodoTask("yangchangkun", "");
        for (Act task : list) {
            System.out.println("getTaskId="+task.getTaskId());
            System.out.println("getTaskName="+task.getTaskName());
            System.out.println("getProcDefId="+task.getProcDefId());
            System.out.println("getProcInsId="+task.getProcInsId());
            System.out.println("getTitle="+task.getTitle());
            System.out.println("getBusinessType="+task.getBusinessType());
            System.out.println("getBusinessId="+task.getBusinessId());
            System.out.println("getAssignee="+task.getAssignee());
            System.out.println("getOpinion="+task.getOpinion());
            System.out.println("getComment="+task.getComment());
            System.out.println("=================================");
        }
    }

    /**
     * 完成一个任务
     */
    @Test
    public void test4() {
        actBusinessServiceImpl.completeTask("huajianguo", "huajianguo", "107516", 1, "我不同意立项");
    }

    /**
     * 获取已经办理的任务
     */
    @Test
    public void test5() {
        /*List<Act> list = actBusinessServiceImpl.histoicFlowList("107505");
        for (Act task : list) {
            System.out.println("getTaskId="+task.getTaskId());
            System.out.println("getTaskName="+task.getTaskName());
            System.out.println("getProcDefId="+task.getProcDefId());
            System.out.println("getProcInsId="+task.getProcInsId());
            System.out.println("getTitle="+task.getTitle());
            System.out.println("getBusinessType="+task.getBusinessType());
            System.out.println("getBusinessId="+task.getBusinessId());
            System.out.println("getAssignee="+task.getAssignee());
            System.out.println("getOpinion="+task.getOpinion());
            System.out.println("getComment="+task.getComment());
            System.out.println("=================================");
        }*/
    }
}
