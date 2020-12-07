package com.pms;

import com.pms.common.utils.StringUtils;
import com.pms.core.adm.domain.Address;
import com.pms.drools.domain.WorkflowCcBean;
import com.pms.drools.service.impl.DroolsBusinessServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@RunWith(SpringRunner.class)
@SpringBootTest
public class DroolsTest {

    @Autowired
    private KieSession kieSession;

    @Autowired
    private DroolsBusinessServiceImpl droolsBusinessServiceImpl;

    /**
     * 启动流程
     */
    @Test
    public void test1() {
        // 以下的数据可以从数据库来，这里写死了
        Address address = new Address();
        address.setPostcode("10000");
        // 使用规则引擎
        kieSession.insert(address);
        int ruleFiredCount = kieSession.fireAllRules();
        System.out.println("触发了" + ruleFiredCount + "条规则");
        System.out.println("---------------------------------");

    }


    /**
     * 启动流程
     */
    @Test
    public void test2() {
        WorkflowCcBean bean = new WorkflowCcBean();
        bean.setBusinessType("hr_join_apply");
        bean.setBusinessId("");
        bean.setAccount("yangchangkun");
        bean.setProId("");
        bean.setDays(0);
        // 使用规则引擎
        kieSession.insert(bean);
        int ruleFiredCount = kieSession.fireAllRules(1);
        System.out.println("触发了" + ruleFiredCount + "条规则");
        System.out.println("返回结果：getCcAccounts="+bean.getCcAccounts());
        System.out.println("返回结果：getCcRoleCodes="+bean.getCcRoleCodes());
        System.out.println("---------------------------------");

    }

    /**
     * 启动流程
     */
    @Test
    public void test3() {
        String businessType = "attendance_leave";
        String account = "yangchangkun";
        String businessId = "636327031156809728";
        String proId = "ff80808164d9e95a0164e9e1433b01f8";
        String days = "4";

        Map<String, Object> otherParam = new HashMap<>();
        if(StringUtils.isNotEmpty(proId)){
            otherParam.put("proId", proId);
        }
        if(StringUtils.isNotEmpty(days)){
            otherParam.put("days", days);
        }

        Set<String> ccAccounts = droolsBusinessServiceImpl.getCcStaffList(account, businessType, businessId, otherParam );

    }

}
