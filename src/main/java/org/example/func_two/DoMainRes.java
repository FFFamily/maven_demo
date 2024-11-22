package org.example.func_two;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class DoMainRes {
    // 未能匹配的
    private List<OtherInfo2> result1;
    // 匹配
    private List<OtherInfo2> result2;
    public DoMainRes(){
        this.result1 = new ArrayList<>();
        this.result2 = new ArrayList<>();
    }
}
