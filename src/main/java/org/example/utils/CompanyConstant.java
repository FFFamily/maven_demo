package org.example.utils;

import java.util.HashMap;
import java.util.Map;

public class CompanyConstant {
    public static HashMap<String,String> mapping = new HashMap<>();
    static {
        mapping.put("江苏中南物业服务有限公司（总部）","江苏中南物业服务有限公司");
        mapping.put("江苏中南物业服务有限公司（商管）","江苏中南物业服务有限公司");
        mapping.put("江苏中南物业服务有限公司（住宅）","江苏中南物业服务有限公司");
        mapping.put("江苏中南物业服务有限公司平湖分公司","江苏中南物业服务有限公司");
    }

    public static String getNewCompanyByOldCompany(String oldCompany){
        return mapping.get(oldCompany) == null ? oldCompany : mapping.get(oldCompany);
    }
}
