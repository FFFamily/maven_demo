package org.example;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.enitty.OracleData;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.utils.CompanyConstant;
import org.example.utils.CompanyTypeConstant;
import org.example.新老系统.Find2022;
import org.example.新老系统.Find2023;
import org.example.新老系统.Find2024;
import org.example.新老系统.FindUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@SpringBootTest
public class MergeService {
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private Find2022 find2022;
    @Resource
    private Find2023 find2023;
    @Resource
    private Find2024 find2024;
    @Resource
    private FindUtil findUtil;
//    @Resource
//    private FindAllBalance findAllBalance;
    @Data
    @AllArgsConstructor
    public static class Item{
        private String selectPath;
        // 江苏中南物业服务有限公司
        private String selectCompanyName;
    }

    @Test
    void init(){
        File file = new File("src/main/java/org/example/excel/zhong_nan/detail");
        for (String fileName : file.list()) {
            String name = fileName.replace(".xlsx", "");
            System.out.println("2023-当前文件："+name);
            try {
                // 老系统数据
                List<Step6OldDetailExcel> excels = findUtil.readPropertyExcel(fileName);
                Map<String, List<Step6OldDetailExcel>> companyMap = excels.stream().collect(Collectors.groupingBy(item -> {
                    String companyName = item.getCompanyName().split("-")[0];
                    return CompanyConstant.getNewCompanyByOldCompany(companyName);
                }));
                for (String newCompanyName : companyMap.keySet()) {
                    System.out.println("map.put(\"" + newCompanyName + "\", \""+name+"\");");
                }
            }catch (Exception e){
                System.out.println("异常- 当前公司为：" + DateUtil.date());
            }
        }
    }
    @Test
    void  test(){
        List<String> allCompany = findAllCompany();
        Map<String, String> map = initMap();
        Map<String, List<String>> fileMap = allCompany.stream().collect(Collectors.groupingBy(item -> map.getOrDefault(item, "其他")));
        String selectCompany = "江苏中南物业服务有限公司余杭分公司";
        String fileFilter = null;
        for (String key : fileMap.keySet()) {
            if (fileMap.get(key).stream().anyMatch(item -> item.equals(selectCompany))){
                fileFilter = key;
            }
        }
        for (String file : fileMap.keySet()) {
            String fileName = file + ".xlsx";
            if (!file.equals(fileFilter)){
                continue;
            }
            log("2023-当前文件："+file);
            // 老系统 excel 数据
            Map<String, List<Step6OldDetailExcel>> companyMap;
            if (file.equals("其他")){
                companyMap = new HashMap<>();
            }else {
                List<Step6OldDetailExcel> excels = findUtil.readPropertyExcel(fileName);
                companyMap = excels.stream().collect(Collectors.groupingBy(item -> {
                    String companyName = item.getCompanyName().split("-")[0];
                    return CompanyConstant.getNewCompanyByOldCompany(companyName);
                }));
            }
            List<String> companyList = fileMap.get(file);
            for (String company : companyList) {
                if (company.equals(selectCompany)){
                    continue;
                }
                String type = CompanyTypeConstant.mapping.get(company);
                if (type.equals(CompanyTypeConstant.ZHONG_NAN)){
                    // 只有中南的才跑
                    log("当前公司："+company);
                    mergeAll(companyMap.getOrDefault(company,new ArrayList<>()),company);
                }
            }
        }



    }

    void mergeAll(List<Step6OldDetailExcel> list, String newCompanyName){
//        File file = new File("src/main/java/org/example/excel/zhong_nan/detail");
//        for (String fileName : file.list()) {

//            if (!name.equals("物业杭州公司")){
//                return;
//            }
            try {
                // 老系统数据


//                for (String newCompanyName : companyMap.keySet()) {
                    System.out.println("开始- 当前公司为：" + newCompanyName + ": " + DateUtil.date());
                    if (!newCompanyName.equals("江苏中南物业服务有限公司余杭分公司")){
                        return;
                    }
                    List<OracleData> list1 = find2022.find(newCompanyName);
                    List<OracleData> list2 = find2023.find(list, newCompanyName);
                    List<OracleData> list3 = find2024.find(newCompanyName);
                    List<OracleData> xsList = new ArrayList<>();
                    xsList.addAll(list1);
                    xsList.addAll(list2);
                    xsList.addAll(list3);
//                findAllBalance.find(selectPath,newCompanyName);
                    File excelFile = new File(newCompanyName + "-总序时账" + ".xlsx");
                    if (excelFile.exists()) {
                        System.out.println("文件存在");
                        List<OracleData> oldList = new ArrayList<>();
                        EasyExcel.read(excelFile, Step6OldDetailExcel.class,
                                new PageReadListener<OracleData>(oldList::addAll));
                        oldList.addAll(xsList);
                        EasyExcel.write(excelFile.getName(), OracleData.class).sheet("组合结果").doWrite(oldList);
                    } else {
                        EasyExcel.write(excelFile.getName(), OracleData.class).sheet("组合结果").doWrite(xsList);
                    }
                    System.out.println("结束- 当前公司为：" + newCompanyName + ": " + DateUtil.date());
//                }
            }catch (Exception e){
                System.out.println("异常- 当前公司为：" + DateUtil.date());
            }
//        }
    }

    private void log(String msg){
        System.out.println(msg);
    }

    private Map<String, String> initMap() {
        Map<String, String> map = new HashMap<>();
        map.put("江苏中南物业服务有限公司", "物业上海公司1");
        map.put("江苏中南物业服务有限公司嘉兴分公司", "物业上海公司2");
        map.put("江苏中南物业服务有限公司上海第二分公司", "物业上海公司2");
        map.put("南通海门区中南物业管理有限公司", "物业上海公司2");
        map.put("江苏中南物业服务有限公司海宁分公司", "物业上海公司2");
        map.put("江苏中南物业服务有限公司上海分公司", "物业上海公司3");
        map.put("江苏中南物业服务有限公司海门分公司", "物业上海公司3");
        map.put("上海多经矩阵电子商务有限公司", "物业上海公司3");
        map.put("江苏中南物业服务有限公司如皋分公司", "物业上海公司3");
        map.put("江苏中南物业服务有限公司南通分公司", "物业上海公司3");
        map.put("江苏中南物业服务有限公司东台分公司", "物业上海公司3");
        map.put("青岛中南物业管理有限公司盐城分公司", "物业上海公司3");
        map.put("江苏中南物业服务有限公司海安分公司", "物业上海公司3");
        map.put("江苏中南物业服务有限公司乍浦分公司", "物业上海公司3");
        map.put("江苏中南物业服务有限公司泰兴分公司", "物业上海公司3");
        map.put("江苏中南物业服务有限公司盐城分公司", "物业上海公司3");
        map.put("江苏中南物业服务有限公司天津分公司", "物业北京公司");
        map.put("唐山中南国际旅游度假物业服务有限责任公司", "物业北京公司");
        map.put("江苏中南物业服务有限公司邯郸分公司", "物业北京公司");
        map.put("江苏中南物业服务有限公司固安分公司", "物业北京公司");
        map.put("江苏中南物业服务有限公司淮安分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司扬州分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司太仓分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司镇江分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司徐州分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司无锡分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司张家港分公司", "物业南京公司");
        map.put("青岛中南物业管理有限公司溧水分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司常州分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司昆山分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司丹阳分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司苏州分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司连云港分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司南京分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司常熟分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司宿迁分公司", "物业南京公司");
        map.put("江苏中南物业服务有限公司晋江分公司", "物业厦门公司");
        map.put("江苏中南物业服务有限公司福州分公司", "物业厦门公司");
        map.put("江苏中南物业服务有限公司莆田分公司", "物业厦门公司");
        map.put("江苏中南物业服务有限公司泉州分公司", "物业厦门公司");
        map.put("江苏中南物业服务有限公司厦门分公司", "物业厦门公司");
        map.put("江苏中南物业服务有限公司马鞍山分公司", "物业合肥公司");
        map.put("江苏中南物业服务有限公司蚌埠分公司", "物业合肥公司");
        map.put("江苏中南物业服务有限公司利辛分公司", "物业合肥公司");
        map.put("江苏中南物业服务有限公司长丰分公司", "物业合肥公司");
        map.put("江苏中南物业服务有限公司合肥分公司", "物业合肥公司");
        map.put("江苏中南物业服务有限公司淮南分公司", "物业合肥公司");
        map.put("江苏中南物业服务有限公司南充分公司", "物业成都公司");
        map.put("江苏中南物业服务有限公司成都分公司", "物业成都公司");
        map.put("江苏中南物业服务有限公司仁寿分公司", "物业成都公司");
        map.put("江苏中南物业服务有限公司西安分公司", "物业成都公司");
        map.put("江苏中南物业服务有限公司宁波分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司常山分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司杭州分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司慈溪分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司德清分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司东阳分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司诸暨分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司绍兴分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司建德分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司湖州分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司余姚分公司", "物业杭州公司");
        map.put("余姚中锦物业服务有限公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司金华分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司台州分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司宁波杭州湾新区分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司温州分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司桐庐分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司宁波奉化分公司", "物业杭州公司");
        map.put("江苏中南物业服务有限公司嵊州分公司", "物业杭州公司");
        map.put("青岛中南物业管理有限公司营口分公司", "物业沈阳公司");
        map.put("江苏中南物业服务有限公司抚顺分公司", "物业沈阳公司");
        map.put("青岛中南物业管理有限公司沈阳分公司", "物业沈阳公司");
        map.put("青岛中南物业管理有限公司泰安分公司", "物业济南公司");
        map.put("江苏中南物业服务有限公司即墨分公司", "物业济南公司");
        map.put("青岛锦琴物业服务有限公司", "物业济南公司");
        map.put("江苏中南物业服务有限公司潍坊分公司", "物业济南公司");
        map.put("江苏中南物业服务有限公司日照分公司", "物业济南公司");
        map.put("江苏中南物业服务有限公司龙口分公司", "物业济南公司");
        map.put("青岛中南物业管理有限公司东营分公司", "物业济南公司");
        map.put("江苏中南物业服务有限公司平度分公司", "物业济南公司");
        map.put("青岛中南物业管理有限公司菏泽分公司", "物业济南公司");
        map.put("江苏中南物业服务有限公司淄博分公司", "物业济南公司");
        map.put("江苏中南物业服务有限公司临沂分公司", "物业济南公司");
        map.put("青岛中南物业管理有限公司", "物业济南公司");
        map.put("江苏中南物业服务有限公司威海分公司", "物业济南公司");
        map.put("江苏中南物业服务有限公司青岛分公司", "物业济南公司");
        map.put("青岛中南物业管理有限公司寿光分公司", "物业济南公司");
        map.put("江苏中南物业服务有限公司寿光分公司", "物业济南公司");
        map.put("江苏中南物业服务有限公司邹城分公司", "物业济南公司");
        map.put("青岛中南物业管理有限公司李沧分公司", "物业济南公司");
        map.put("江苏中南物业服务有限公司济宁分公司", "物业济南公司");
        map.put("江苏中南物业服务有限公司许昌分公司", "物业济南公司");
        map.put("青岛中南物业管理有限公司烟台分公司", "物业济南公司");
        map.put("江苏中南物业服务有限公司佛山高明分公司", "物业深圳公司");
        map.put("海南中南物业服务有限公司", "物业深圳公司");
        map.put("江苏中南物业服务有限公司江门分公司", "物业深圳公司");
        map.put("江苏中南物业服务有限公司万宁分公司", "物业深圳公司");
        map.put("江苏中南物业服务有限公司揭阳分公司", "物业深圳公司");
        map.put("江苏中南物业服务有限公司中山分公司", "物业深圳公司");
        map.put("海南中南物业服务有限公司儋州分公司", "物业深圳公司");
        map.put("江苏中南物业服务有限公司梅州分公司", "物业深圳公司");
        map.put("江苏中南物业服务有限公司湛江分公司", "物业深圳公司");
        map.put("江苏中南物业服务有限公司惠州分公司", "物业深圳公司");
        map.put("江苏中南物业服务有限公司昌江分公司", "物业深圳公司");
        map.put("江苏中南物业服务有限公司佛山顺德分公司", "物业深圳公司");
        map.put("江苏中南物业服务有限公司南宁分公司", "物业深圳公司");
        map.put("江苏中南物业服务有限公司佛山三水分公司", "物业深圳公司");
        map.put("江苏中南物业服务有限公司江津分公司", "物业重庆公司");
        map.put("江苏中南物业服务有限公司昆明分公司", "物业重庆公司");
        map.put("江苏中南物业服务有限公司贵阳分公司", "物业重庆公司");
        map.put("江苏中南物业服务有限公司重庆分公司", "物业重庆公司");
        return map;
    }

    private List<String> findAllCompany(){
        List<String> companyList = jdbcTemplate.queryForList(
                "select z.\"公司段描述\" from ZDPROD_EXPDP_20241120 z GROUP BY z.\"公司段描述\" ",
                String.class
        );
        return companyList;
    }
}
