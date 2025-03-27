package com.reeman.phone.utils.placeholder;

import com.reeman.phone.mode.CurrentTaskMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaceholderContent {

    public static final List<PlaceholderItem> ITEMS = new ArrayList<>();
    public static final Map<String, PlaceholderItem> ITEM_MAP = new HashMap<>();

    /**
     * 使用真实数据初始化项目
     * @param realItems 真实数据的列表
     */
    public static void initialize(List<CurrentTaskMode> realItems) {
        ITEMS.clear(); // 清空旧数据
        ITEM_MAP.clear(); // 清空旧映射
        for (CurrentTaskMode item : realItems) {
            List<String> contents = item.getContent(); // 获取内容列表
            List<String> details = item.getDetails(); // 获取详细信息列表

            // 使用 StringBuilder 来构建内容字符串
            StringBuilder contentBuilder = new StringBuilder();

            for (int i = 0; i < details.size(); i++) {
                contentBuilder.append(details.get(i)); // 添加当前内容
                if (i < details.size() - 1) { // 如果不是最后一个元素
                    contentBuilder.append(" --> "); // 添加分隔符
                }
            }

            // 将拼接后的内容转换为字符串
            String contentString = contentBuilder.toString();

            // 创建 PlaceholderItem，传入拼接后的内容字符串
            addItem(new PlaceholderItem(item.getId(), contents, contentString)); // 假设 PlaceholderItem 的构造函数接收字符串
        }
    }


    private static void addItem(PlaceholderItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    public static class PlaceholderItem {
        public final String id;
        public final List<String> content;
        public final String details;

        public PlaceholderItem(String id, List<String> content, String details) {
            this.id = id;
            this.content = content;
            this.details = details;
        }

        @Override
        public String toString() {
            return content.toString();
        }
    }
}
