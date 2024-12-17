package org.example.utils;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import org.example.enitty.zhong_nan.*;
import org.example.寻找等级.FindNccZhongNanLevel;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class CoverNewDate {
    @Resource
    private FindNccZhongNanLevel findNccZhongNanLevel;
    public void cover(String startTime,Step6OldDetailExcel data) {
        try {
            if (data.getV() == null && data.getW() == null){
                throw new RuntimeException("无法计算金额");
            }
            String projectName = data.getProjectName();
            if (!isBackProject2022(projectName)){
                return;
            }
            String time = data.getTime();
            DateTime date = DateUtil.parseDate(time);
            if (startTime.equals("2022")){
                if (date.isBefore(DateUtil.parse("2022-01-01")) || date.isAfter(DateUtil.parse("2022-12-31"))) {
                    return;
                }
            }else if (startTime.equals("2023-1-6")){
                if (date.isBefore(DateUtil.parse("2023-01-01")) || date.isAfter(DateUtil.parse("2023-06-30"))) {
                    return;
                }
            }else if (startTime.equals("2023-7-12")){
                if (date.isBefore(DateUtil.parse("2023-07-01")) || date.isAfter(DateUtil.parse("2023-12-31"))) {
                    return;
                }
            } else {
                throw new RuntimeException();
            }
            // 年
            int year = date.year();
            // 月
            int month = date.month() + 1;
            StringBuilder builder = new StringBuilder();
            StringBuilder nameBuilder = new StringBuilder();
            // 1- 机构代码
            String companyName = data.getCompanyName();
            ZNCompanyMapping znCompanyMapping = findNccZhongNanLevel.znCompanyMapping.get(companyName);
            String fmsCompanyCode = znCompanyMapping.getFMSCompanyCode();
            builder.append(fmsCompanyCode).append(".");
            nameBuilder.append(appendNameStr(znCompanyMapping.getFMSCompanyName())).append(".");
            data.setCompanyCode(fmsCompanyCode);
            data.setCompanyName(CompanyConstant.getNewCompanyByOldCompany(data.getCompanyName().split("-")[0]));
            // 2- 部门
//                                    String orgName = data.getOrgName();
//                                    ZNOrgMapping znOrgMapping = findNccZhongNanLevel.znOrgMapping.get(orgName);
//                                    String fmsOrgCode = znOrgMapping.getFMSOrgCode();
//                                    builder.append(fmsOrgCode).append(".");
            builder.append("0").append(".");
            nameBuilder.append("-").append(".");
            // 3-科目代码 4-子目代码
            findProjectInfoByTime(data,year,month,builder,nameBuilder);
            // 5-产品代码
            String eventName = data.getEventName();
            ZNEventMapping znEventMapping = findNccZhongNanLevel.znEventMapping.get(companyName + eventName);
            String fmsProductCode = znEventMapping == null ?  null : znEventMapping.getFmsProductCode();
            String fmsProductName = znEventMapping == null ?  null : znEventMapping.getFmsProductName();
            builder.append(appendStr(fmsProductCode) ).append(".");
            nameBuilder.append(appendNameStr(fmsProductName) ).append(".");
            // 6-地区代码
            String fmsAreaCode = "0";
            builder.append(fmsAreaCode).append(".");
            nameBuilder.append("-").append(".");
            // 7-SBU
            String fmsSBU = "0";
            builder.append(fmsSBU).append(".");
            nameBuilder.append("-").append(".");
            // 8-ICP
            String customerName = data.getCustomerName();
            ZNIPCMapping znipcMapping = findNccZhongNanLevel.znipcMapping.get(customerName);
            String icp = znipcMapping == null ? null : znipcMapping.getFmsICPCode();
            builder.append(appendStr(icp)).append(".");
            nameBuilder.append(appendNameStr(znipcMapping == null ? null : customerName)).append(".");
            // 9-项目代码
            String fmsEventCode = znEventMapping == null ? null : znEventMapping.getFmsEventCode();
            String fmsEventName = znEventMapping == null ? null : znEventMapping.getFmsEventName();
            builder.append(appendStr(fmsEventCode) ).append(".");
            nameBuilder.append(appendNameStr(fmsEventName) ).append(".");
            // 10-备用
            String standby  = "0";
            builder.append(standby);
            nameBuilder.append("-");
            String onlySign = builder.toString();
            String onlySignName = nameBuilder.toString();
            data.setOnlySign(onlySign);
            data.setOnlySignName(onlySignName);
            // 辅助核算
            String auxiliaryAccounting = "";
            String auxiliaryAccountingCode = "";
            if (icp != null){
                auxiliaryAccounting += "-";
                auxiliaryAccountingCode += "0";
            }else {
                auxiliaryAccounting += data.getCustomerName() != null ? data.getCustomerName() : data.getPersonalName() == null ? "-" : data.getPersonalName();
                auxiliaryAccountingCode += data.getCustomerName() != null ? data.getCustomerCode() : data.getPersonalName() == null ? "-" : data.getPersonalCode();
            }
            data.setAuxiliaryAccounting(auxiliaryAccounting);
            data.setAuxiliaryAccountingCode(auxiliaryAccountingCode);

        }catch (Exception e){
//                                    System.out.println("解析中南老系统明细数据出错: "+e.getMessage());
            System.out.println(data);
            e.printStackTrace();
        }
    }



    public String getProjectName(Step6OldDetailExcel data) {
        try {
            String time = data.getTime();
            DateTime date = DateUtil.parseDate(time);
            // 年
            int year = date.year();
            // 月
            int month = date.month() + 1;
            StringBuilder builder = new StringBuilder();
            StringBuilder nameBuilder = new StringBuilder();
            // 3-科目代码 4-子目代码
            return findProjectInfoByTime(data,year,month,builder,nameBuilder);
        }catch (Exception e){
            System.out.println(data);
            e.printStackTrace();
        }
        return null;
    }
    private String findProjectInfoByTime(Step6OldDetailExcel data ,int year,int month,StringBuilder builder,StringBuilder nameBuilder){

        String projectCode = data.getProjectCode();
        ZNProjectMapping znProjectMapping = findNccZhongNanLevel.znProjectMapping.get(projectCode);
        // 3-科目代码
        String fmsProjectCode =  znProjectMapping.getFmsProjectCode();
        String fmsProjectName = znProjectMapping.getFmsProjectName();
        // 4-子目
        String fmsChildProjectCode = znProjectMapping.getFmsChildProjectCode();
        String fmsChildProjectName = znProjectMapping.getFmsChildProjectName();
        if (year == 2022){
            // 使用默认
        }else if (year == 2023 || year == 2024){
            String customerName = data.getCustomerName();
            ZNRelationMapping znRelationMapping = findNccZhongNanLevel.znRelationMapping.get(customerName);
            if (znRelationMapping != null){
                String project = getDataProject(fmsProjectCode);
                if (project.equals("合同负债")){
                    project = "预收账款";
                }
                ZNRelationProjectMapping znRelationProjectMapping = findNccZhongNanLevel.znRelationProjectMapping.get(project);
                fmsProjectCode = znRelationProjectMapping.getFmsProjectCode();
                fmsProjectName = znRelationProjectMapping.getFmsProjectName();
                fmsChildProjectCode = znRelationProjectMapping.getFmsChildProjectCode();
                fmsChildProjectName = znRelationProjectMapping.getFmsChildProjectName();
            }
        }
        data.setProject(getDataProject(fmsProjectCode));
        builder.append(appendStr(fmsProjectCode) ).append(".");
        builder.append(appendStr(fmsChildProjectCode) ).append(".");
        nameBuilder.append(appendNameStr(fmsProjectName)).append(".");
        nameBuilder.append(appendNameStr(fmsChildProjectName)).append(".");
        return appendNameStr(fmsProjectName);
    }

    private String getDataProject(String fmsProjectCode){
        String project = fmsProjectCode.substring(0,4);
        switch (project) {
            case "1122":
                return "应收账款";
            case "2202":
                return "应付账款";
            case "2203":
                return "合同负债";
            case "2205":
                return "预收账款";
            case "1123":
                return "预付账款";
            case "1221":
                return "其他应收款";
            case "2241":
                return "其他应付款";
        }
        return "未知";
    }

    public String appendStr(String str){
        return  str == null ? "0" : str;
    }
    public String appendNameStr(String str){
        return  str == null ? "-" : str;
    }

    private Boolean isBackProject2022(String projectName) {
        return projectName.startsWith("应付账款")
                || projectName.startsWith("预付账款")
                || projectName.startsWith("合同负债")
                || projectName.startsWith("预收账款")
                || projectName.startsWith("应收账款")
                || projectName.startsWith("其他应付款")
                || projectName.startsWith("其他应收款")
                || projectName.startsWith("应收票据");
    }
}
