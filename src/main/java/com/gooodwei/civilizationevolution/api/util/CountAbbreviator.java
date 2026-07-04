package com.gooodwei.civilizationevolution.api.util;

import java.util.Locale;

/**
 * 超大数量缩写工具，照搬 SophisticatedCore {@code CountAbbreviator}。
 *
 * <p>当物品数量超过 4 个字符时使用缩写后缀（k/m/b），确保在 16×16 像素的
 * 槽位右下角能正常显示。
 *
 * <p>格式化效果（默认 maxChars=4）：
 * <table>
 *   <tr><th>数量</th><th>输出</th></tr>
 *   <tr><td>1–9999</td><td>"1", "999", "1,728", "9,999"</td></tr>
 *   <tr><td>10000–999999</td><td>"10.0k", "999k"</td></tr>
 *   <tr><td>1M–999M</td><td>"1.00m", "999m"</td></tr>
 *   <tr><td>≥1B</td><td>"1.00b"</td></tr>
 * </table>
 */
public final class CountAbbreviator {
    /** 千分位后缀 */
    private static final String[] SUFFIXES = {"k", "m", "b"};

    private CountAbbreviator() {}

    /**
     * 缩写数量，最多 4 个字符。
     */
    public static String abbreviate(int count) {
        return abbreviate(count, 4);
    }

    /**
     * 缩写数量，最多 {@code maxChars} 个字符（含后缀）。
     *
     * @param count    原始数量
     * @param maxChars 最大字符数（含后缀，最少 3）
     * @return 格式化字符串
     */
    public static String abbreviate(int count, int maxChars) {
        if (count < 0) count = 0;
        if (count == 0) return "0";

        int digits = digits(count);
        if (digits <= maxChars) {
            // 不缩写，加千分位逗号
            return String.format(Locale.ROOT, "%,d", count);
        }

        // 需要缩写：计算千分位指数
        // 例：count=1728, digits=4, maxChars=4 → needAbbrev=true
        // thousandsExponent = ((4-4)/3)+1 = 1 → suffix="k"
        int thousandsExp = ((digits - maxChars) / 3) + 1;
        int suffixIdx = Math.min(thousandsExp, SUFFIXES.length) - 1;
        if (suffixIdx < 0) suffixIdx = 0;

        double divided = count / Math.pow(1000, suffixIdx + 1);
        int wholeDigits = digits - (suffixIdx + 1) * 3;
        int precision = Math.max(0, maxChars - 1 - wholeDigits);

        // 格式化数字部分
        String numStr;
        if (precision == 0) {
            numStr = String.valueOf((int) Math.round(divided));
        } else {
            numStr = String.format(Locale.ROOT, "%." + precision + "f", divided);
        }

        return numStr + SUFFIXES[suffixIdx];
    }

    /** 正整数的十进制位数（1→1, 10→2, 1728→4） */
    private static int digits(int n) {
        if (n <= 0) return 1;
        return (int) Math.log10(n) + 1;
    }
}
