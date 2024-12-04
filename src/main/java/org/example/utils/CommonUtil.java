package org.example.utils;

import java.math.BigDecimal;

public class CommonUtil {
    public static String appendErrorMsg(String msg,String... appendMsg){
        if (msg == null){
            return String.join("、",appendMsg);
        }else {
            return msg + "、" + String.join("、",appendMsg);
        }
    }

    public static BigDecimal getBigDecimalValue(BigDecimal number){
        return number == null ? BigDecimal.ZERO : number;
    }

    public static String getZ(BigDecimal money){
        return money == null ? "" : money.compareTo(BigDecimal.ZERO) < 0 ? "("+ money +")" : money.toString();
    }
    public static String getX(BigDecimal v,BigDecimal w){
        return v != null ? "借" : w != null ? "贷" : null;
    }
}
