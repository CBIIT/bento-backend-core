package gov.nih.nci.bento.utility;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StrUtil {

    public static String getBoolText(String text) {
        String strPattern = "(?i)(\\bfalse\\b|\\btrue\\b)";
        return getString(strPattern, text).toLowerCase();
    }

    public static String getIntText(String text) {
        String strPattern = "(\\b[0-9]+\\b)";
        return getString(strPattern, text);
    }

    private static String getString(String strPattern, String text) {
        String str = Optional.ofNullable(text).orElse("");
        Pattern pattern = Pattern.compile(strPattern);
        Matcher matcher = pattern.matcher(str);
        return matcher.find() ? matcher.group(1) : "";
    }
}