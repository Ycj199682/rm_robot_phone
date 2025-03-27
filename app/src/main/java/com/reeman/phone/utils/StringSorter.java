package com.reeman.phone.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StringSorter {

    // 对列表中的字符串进行排序，并返回排序后的列表
    public static List<String> sortByPrefixAndNumber(List<String> data) {
        Collections.sort(data, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                // 如果某个字符串长度小于2，则不参与排序，保持原位置
                if (s1.length() < 2 && s2.length() < 2) {
                    return 0; // 两者都不够字符，保持相对位置
                }
                if (s1.length() < 2) {
                    return 1; // s1 不参与排序，放在后面
                }
                if (s2.length() < 2) {
                    return -1; // s2 不参与排序，放在后面
                }

                // 获取前两个汉字
                String prefix1 = s1.substring(0, 2);
                String prefix2 = s2.substring(0, 2);

                // 比较前两个汉字
                int prefixComparison = prefix1.compareTo(prefix2);
                if (prefixComparison != 0) {
                    return prefixComparison; // 如果前两个汉字不同，直接返回比较结果
                }

                // 前两个汉字相同，提取末尾数字进行比较
                Integer num1 = extractNumber(s1);
                Integer num2 = extractNumber(s2);
                return num1.compareTo(num2); // 数字比较
            }
        });
        return data; // 返回排好序的列表
    }

    // 提取字符串末尾的数字
    private static Integer extractNumber(String s) {
        StringBuilder number = new StringBuilder();
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                number.insert(0, c);
            } else {
                break; // 遇到非数字字符时停止
            }
        }
        return number.length() > 0 ? Integer.parseInt(number.toString()) : 0;
    }
}
