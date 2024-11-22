package org.example.分类;

import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 辅助
 */
@Data
public class AssistantResult {
    // 索引
    private String index;
    // 匹配字段
    @ColumnWidth(180)
    private String field;
    // 类型
    private String type;

    private Integer isIncludeUp;
}
