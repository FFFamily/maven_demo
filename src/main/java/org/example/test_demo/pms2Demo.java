package org.example.test_demo;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class pms2Demo {
    @Setter
    @Getter
    static class WorkRuleConfig{
        // 规则名称
        private String name;
        // 规则范围
        private String range;
        // 规则类型
        private Integer type;
        // 打卡时间配置规则
        private List<WorkCheckinConfig> workCheckinConfigs;
    }

    // 打卡时间规则配置
    @Setter
    @Getter
    static class WorkCheckinConfig{
        private String  workTime;
        private String closeTime;
        // 是否开启午休
        private Boolean openNoonRest;
        private String restBeginTime;
        private String restEndTime;
    }

    static class workPrefix{
        public static final String NORMAL = "";
        public static final String OVERSEAS_PERSONNEL = "外派人员";
    }
    // 异常考勤工作类型
    private static final List<WorkTypeEnum> failWorkTypeList = new ArrayList<>();
    // 成功考勤工作类型
    private static final List<WorkTypeEnum> successWorkTypeList = new ArrayList<>();
    private static final WorkRuleConfig globalConfig   = new WorkRuleConfig();
    static {
        // 异常
        failWorkTypeList.add(WorkTypeEnum.NO_DATA);
        failWorkTypeList.add(WorkTypeEnum.ABSENCE_FROM_DUTY);
        failWorkTypeList.add(WorkTypeEnum.COMPENSATORY_LEAVE);
        failWorkTypeList.add(WorkTypeEnum.OVERSEAS_PERSONNEL_COMPENSATORY_LEAVE);
        failWorkTypeList.add(WorkTypeEnum.OTHER_COMPENSATORY_LEAVE);
        failWorkTypeList.add(WorkTypeEnum.COMPASSIONATE_LEAVE);
        failWorkTypeList.add(WorkTypeEnum.ANNUAL_LEAVE);
        failWorkTypeList.add(WorkTypeEnum.MATERNITY_LEAVE);
        failWorkTypeList.add(WorkTypeEnum.SICK_LEAVE);
        // 成功
        successWorkTypeList.add(WorkTypeEnum.SIGN_IN);
        successWorkTypeList.add(WorkTypeEnum.SIGN_OFF);
        // 全局配置
        globalConfig.setName("考勤导入配置");
        globalConfig.setType(1);
        WorkCheckinConfig workCheckinConfig = new WorkCheckinConfig();
        workCheckinConfig.setWorkTime("08:30:00");
        workCheckinConfig.setCloseTime("17:00:00");
        workCheckinConfig.setOpenNoonRest(true);
        workCheckinConfig.setRestBeginTime("12:00:00");
        workCheckinConfig.setRestEndTime("12:30:00");
        globalConfig.setWorkCheckinConfigs(Collections.singletonList(workCheckinConfig));
    }

    @Getter
    @AllArgsConstructor
    // todo 是否可以添加优先级
    enum WorkTypeEnum{
        NO_DATA(""),
        COMPENSATORY_LEAVE("调休假"),
        ABSENCE_FROM_DUTY("缺勤"),
        REST("休息"),
        ANNUAL_LEAVE("年假"),
        MATERNITY_LEAVE("产假"),
        OVERSEAS_PERSONNEL_COMPENSATORY_LEAVE("外派人员调休"),
        OTHER_COMPENSATORY_LEAVE("水站调休"),
        SICK_LEAVE("病假"),
        COMPASSIONATE_LEAVE("事假"),
        BUSINESS_TRAVEL("出差"),
        SIGN_IN("签到"),
        SIGN_OFF("签退"),
        LEAVE_EARLY("早退"),
        LATE("迟到"),
        OTHER("其他")
        ;
        private final String type;
    }
    private static final DateTime DEFAULT_LUNCH_TIME = DateUtil.parse("12:00:00");

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        ExcelReader excelReader = ExcelUtil.getReader("excel/月出勤统计表2024-07.xls", "sheet0");


        excelReader.setIgnoreEmptyRow(false);
        excelReader.setCellEditor((cell,value) -> value);
//        List<Map<String, Object>> excelDataList = excelReader.read();
        List<List<Object>> excelDataList = excelReader.read();
        List<Map<String,Object>> excelDataMapList = new ArrayList<>();
        double lunchBreak = 1.5d;
        AtomicInteger num = new AtomicInteger();
        long toSkip = 2;
        for (List<Object> array : excelDataList) {
            if (toSkip > 0) {
                toSkip--;
                continue;
            }
            Number actualAttendanceHour = BigDecimal.valueOf(Double.parseDouble((String)array.get(6))) ;
//            Object[] array = map.entrySet().toArray();
//            System.out.println("当前数据长度为："+array.length);
            Number all = 0;
            String user = (String) array.get(1);
            String target = "";
            System.out.println("当前人："+user);
            for (int i = 61; i < array.size(); i += 2) {
                if (user.equals(target)){
                    System.out.println("============");
                }
                // 上午
                String morning = (String) array.get(i);
                Number morningNum = calculateHour(formatTime(morning),morning);
                // 下午
                String afternoon = (String) array.get(i + 1);
                Number afternoonNum = calculateHour(formatTime(afternoon),afternoon);
                if (user.equals(target)){
                    System.out.println("当前索引："+excelDataList.get(0).get(i));
                    System.out.println(morningNum);
                    System.out.println(afternoonNum);
                }
                Number total = BigDecimal.valueOf(morningNum.doubleValue() + afternoonNum.doubleValue()).divide(BigDecimal.valueOf(60),2, RoundingMode.DOWN) ;
                if (user.equals(target)){
                    System.out.println("总工时："+total);
                }
                all = all.doubleValue()  + total.doubleValue();
                if (user.equals(target)){
                    System.out.println("============");
                }

            }
//            int i = new BigDecimal(String.valueOf(all)).remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(10)).intValue();
//            if (i < 5){
//                System.out.println("考勤时长："+new BigDecimal(String.valueOf(all)).setScale(0,RoundingMode.FLOOR));
//            }else if (i == 5){
//                System.out.println("考勤时长："+all);
//            }else {
//                System.out.println("考勤时长："+new BigDecimal(String.valueOf(all)).add(BigDecimal.valueOf(0.5)).setScale(0,RoundingMode.FLOOR));
//            }
            System.out.println("考勤时长："+all);
            System.out.println("实际出勤小时："+(actualAttendanceHour.doubleValue() ));
            System.out.println("--------------------------已处理-------------------------" + num.getAndIncrement());
        }
    }


    /**
     *
     * @param message 信息
     * @param originMessage 源头信息
     */
    private static Number calculateHour(String message,String originMessage){
        if (message.equals(WorkTypeEnum.REST.getType())  || Objects.equals(WorkTypeEnum.NO_DATA.getType(), message)){
            return 0;
        }
        // 匹配 24 小时制时间的正则表达式
        String timePattern = "\\b([01]?[0-9]|2[0-3]):[0-5][0-9](?::[0-5][0-9])?\\b";
        Pattern pattern = Pattern.compile(timePattern);
        Matcher matcher = pattern.matcher(message);
        // 只有打卡上下班才会涉及到时间
        if (matcher.find()){
            // 拿到对应的时间
            String time = matcher.group(0);
            Optional<WorkTypeEnum> typeEnum = successWorkTypeList.stream().filter(item -> item.getType().equals(message.split(":")[0])).findFirst();
            if (typeEnum.isPresent()){
                return calculateWorkTime(time,typeEnum.get());
            }
            throw new RuntimeException("类型错误");
        }
        String[] workInfo = message.split(":");
        if (workInfo.length > 2){
            // 特殊数据，证明存在多份操作
            String[] splitMessage = message.split(";");
            System.out.println("==========================数据有问题 "+ Arrays.toString(splitMessage));
            // TODO 要预防未能考虑到的场景，以避免栈溢出
            if (splitMessage.length == 1){
                throw new RuntimeException("程序不支持类型："+message);
            }
            return Arrays.stream(splitMessage)
                    // 当存咋多份操作时，签到操作不生效
                    .filter(item -> !(item.contains(WorkTypeEnum.SIGN_IN.type) || item.contains(WorkTypeEnum.SIGN_OFF.type)))
                    .reduce(0d,(prev,curr) -> prev + calculateHour(curr, message).doubleValue(),(l, r) ->l);
        }
//        System.out.println("workInfo"+ Arrays.toString(workInfo));
        String workType = workInfo[0].trim();
        String workBody = workInfo[1].trim();
        // 若为异常考勤，那么直接考勤为0
        if (failWorkTypeList.stream().anyMatch(item -> item.getType().equals(workType))){
            return 0;
        }
        return calculateWorkTime(workBody, Arrays.stream(WorkTypeEnum.values()).filter(item -> item.getType().equals(workType)).findFirst().orElse(null));
    }


    private static Number calculateUpPartWorkTime(){
        List<WorkCheckinConfig> workCheckinConfigs = globalConfig.getWorkCheckinConfigs();
        WorkCheckinConfig workCheckinConfig = workCheckinConfigs.get(0);
        // 正常打卡时间
        String workTime = workCheckinConfig.getWorkTime();
        if(workCheckinConfig.getOpenNoonRest()){
            // 如果配置了午休时间：午休时间-上班时间
            return DateUtil.between(DateUtil.parse(workCheckinConfig.getRestBeginTime()), DateUtil.parse(workTime), DateUnit.MINUTE);
//            return 4* 60;
        }else {
            return  DateUtil.between(DEFAULT_LUNCH_TIME, DateUtil.parse(workTime), DateUnit.MINUTE);
        }
    }

    private static Number calculateLowerPartWorkTime(String checkinTime){
        List<WorkCheckinConfig> workCheckinConfigs = globalConfig.getWorkCheckinConfigs();
        WorkCheckinConfig workCheckinConfig = workCheckinConfigs.get(0);
        if (workCheckinConfig.getOpenNoonRest()){
            // 如果配置了午休时间: 下班时间-午休时间
            return DateUtil.between(DateUtil.parse(workCheckinConfig.getCloseTime()),DateUtil.parse(workCheckinConfig.getRestEndTime()), DateUnit.MINUTE);
        }else {
            // 没有配置午休：下班时间-默认中午时间
            return DateUtil.between(DateUtil.parse(checkinTime),DEFAULT_LUNCH_TIME, DateUnit.MINUTE);
        }
    }

    /**
     * 将数据格式化
     */
    private static String formatTime(String message){
        // 去除前后空格
        message = message.trim();
        // 移出中文冒号
        message = message.replaceAll("：",":");
        if (Objects.equals(WorkTypeEnum.NO_DATA.getType(), message)){
            // 空数据直接范围
            return message;
        }
        if (message.contains(WorkTypeEnum.REST.getType())){
            // 只有休息才会走到这个流程
            // 只要包含了休息，就格式化为 休息，避免出现类似 休息(中秋) 这样的情况
            return WorkTypeEnum.REST.getType();
        }
        // 换行校验
        int index;
        int lastIndex = 0;
        StringBuilder builder = new StringBuilder();
        String fixedContent = "小时";
        if (!message.contains("小时")){
            return message;
        }
        while ((index = message.indexOf(fixedContent,lastIndex)) != -1){
            builder.append(message,lastIndex,index+fixedContent.length());
            // 添加新的内容
            builder.append(";");
            // 更新lastIndex
            lastIndex = index + fixedContent.length();
        }
        return builder.toString();
    }

    private static Number calculateWorkTime(String checkinTime,WorkTypeEnum typeEnum){
        // 若为打卡上班
        if (WorkTypeEnum.SIGN_IN.equals(typeEnum)){
            return calculateUpPartWorkTime();
        } else if (WorkTypeEnum.SIGN_OFF.equals(typeEnum)){
            return calculateLowerPartWorkTime(checkinTime);
        }else if (WorkTypeEnum.LATE.equals(typeEnum)){
            // 迟到
            String[] split = checkinTime.split("分钟");
            String minute = split[0];
            return calculateUpPartWorkTime().intValue() - Double.parseDouble(minute);
        }else if (WorkTypeEnum.BUSINESS_TRAVEL.equals(typeEnum)){
            // 出差
            String[] split = checkinTime.split("小时");
            String hour = split[0];
            return new BigDecimal(hour).multiply(BigDecimal.valueOf(60));
        }else if (WorkTypeEnum.OTHER.equals(typeEnum)){
            // 其他代表休假，考勤按照0处理
            return 0;
        }else if (WorkTypeEnum.LEAVE_EARLY.equals(typeEnum)){
            // 早退
            System.out.println("迟到拉："+checkinTime);
            String[] split = checkinTime.split("分钟");
            String minute = split[0];
            return calculateLowerPartWorkTime(checkinTime).doubleValue() - Double.parseDouble(minute);
        }else {
            return 0;
        }

    }

}


