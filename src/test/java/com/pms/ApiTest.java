package com.pms;

import com.alibaba.fastjson.JSONObject;
import com.pms.app.hr.HrApprovalController;
import com.pms.common.base.AjaxResult;
import com.pms.common.utils.DateUtils;
import com.pms.common.utils.StringUtils;
import com.pms.common.utils.Tool;
import com.pms.common.utils.http.HttpUtil;
import com.pms.core.adm.domain.Address;
import com.pms.core.attendance.service.IAttendRecordDailyService;
import com.pms.drools.domain.WorkflowCcBean;
import com.pms.drools.service.impl.DroolsBusinessServiceImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiTest {

    Log logger = LogFactory.getLog(ApiTest.class);

    @Autowired
    private IAttendRecordDailyService attendRecordDailyService;

    /**
     * 启动流程
     */
    @Test
    public void test1() {
        Map<String, Object> params = new HashMap<>();
        params.put("account", "yangchangkun");
        params.put("password", "520XLYbaobei");

        String resp = HttpUtil.post("http://localhost:9008/pms/app/login1", params);
        logger.info("getProductRate.resp ======================"+resp);

    }

    @Test
    public void daka() {
        String userId = "ff80808164ab87b90164d9aa8a850024";
        String fingerPrint = "";
        Double lng = 116.473406d;
        Double lat = 40.012405d;
        String addr = "测试";

        Date signTime = DateUtils.conversion("2020-05-08 17:02:56", DateUtils.YYYY_MM_DD_HH_MM_SS);

        AjaxResult ar = attendRecordDailyService.attendanceSign(userId, signTime, lng, lat, addr, fingerPrint);

    }


    @Test
    public void messageInfoList() {
        Map<String, Object> params = new HashMap<>();
        params.put("searchKey", "");
        params.put("msgFrom", "ff80808164ab87b90164d9aa8a850065");
        params.put("msgTo", "661585834114588672");
        params.put("msgCate", "p2p");
        params.put("pageNum", "0");
        params.put("pageSize", "1");
        params.put("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VySWQiLCJhdWQiOiJmZjgwODA4MTY0YWI4N2I5MDE2NGQ5YWE4YTg1MDA2NSIsImlzcyI6InBtcyIsImV4cCI6MTU5Mzg2NDI4NywidXNlcklkIjoiZmY4MDgwODE2NGFiODdiOTAxNjRkOWFhOGE4NTAwNjUiLCJpYXQiOjE1OTEyNzIyODd9.te5ST70cyvEwzMa2fcjE4x5RH2hVuSrA98FO7QfzErU");
        params.put("appUserId", "ff80808164ab87b90164d9aa8a850065");
        String resp = HttpUtil.post("http://localhost:9008/pms/app/im/messageInfo/list/", params);
        logger.info("getProductRate.resp ======================"+resp);

    }

}
