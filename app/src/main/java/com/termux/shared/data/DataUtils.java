//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.termux.shared.data;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class DataUtils {
    public static final int TRANSACTION_SIZE_LIMIT_IN_BYTES = 102400;
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public DataUtils() {
    }

    public static String getTruncatedCommandOutput(String text, int maxLength, boolean fromEnd, boolean onNewline, boolean addPrefix) {
        if (text == null) {
            return null;
        } else {
            String prefix = "(truncated) ";
            if (addPrefix) {
                maxLength -= prefix.length();
            }

            if (maxLength >= 0 && text.length() >= maxLength) {
                if (fromEnd) {
                    text = text.substring(0, maxLength);
                } else {
                    int cutOffIndex = text.length() - maxLength;
                    if (onNewline) {
                        int nextNewlineIndex = text.indexOf(10, cutOffIndex);
                        if (nextNewlineIndex != -1 && nextNewlineIndex != text.length() - 1) {
                            cutOffIndex = nextNewlineIndex + 1;
                        }
                    }

                    text = text.substring(cutOffIndex);
                }

                if (addPrefix) {
                    text = prefix + text;
                }

                return text;
            } else {
                return text;
            }
        }
    }

    public static void replaceSubStringsInStringArrayItems(String[] array, String find, String replace) {
        if (array != null && array.length != 0) {
            for(int i = 0; i < array.length; ++i) {
                array[i] = array[i].replace(find, replace);
            }

        }
    }

    public static float getFloatFromString(String value, float def) {
        if (value == null) {
            return def;
        } else {
            try {
                return Float.parseFloat(value);
            } catch (Exception var3) {
                return def;
            }
        }
    }

    public static int getIntFromString(String value, int def) {
        if (value == null) {
            return def;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (Exception var3) {
                return def;
            }
        }
    }

    public static String getStringFromInteger(Integer value, String def) {
        return value == null ? def : String.valueOf(value);
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for(int j = 0; j < bytes.length; ++j) {
            int v = bytes[j] & 255;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 15];
        }

        return new String(hexChars);
    }

    public static int getIntStoredAsStringFromBundle(Bundle bundle, String key, int def) {
        return bundle == null ? def : getIntFromString(bundle.getString(key, Integer.toString(def)), def);
    }

    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    public static float rangedOrDefault(float value, float def, float min, float max) {
        return !(value < min) && !(value > max) ? value : def;
    }

    public static String getSpaceIndentedString(String string, int count) {
        return string != null && !string.isEmpty() ? getIndentedString(string, "    ", count) : string;
    }

    public static String getTabIndentedString(String string, int count) {
        return string != null && !string.isEmpty() ? getIndentedString(string, "\t", count) : string;
    }

    public static String getIndentedString(String string, @NonNull String indent, int count) {
        return string != null && !string.isEmpty() ? string.replaceAll("(?m)^", repeat(indent, Math.max(count, 1))) : string;
    }

    static String repeat(String string, int count) {
        if (count <= 0) return "";

        StringBuilder builder = new StringBuilder(string.length() * count);

        while (count > 0) {
            builder.append(string);
            count--;
        }

        return builder.toString();
    }

    public static <T> T getDefaultIfNull(@Nullable T object, @Nullable T def) {
        return object == null ? def : object;
    }

    public static String getDefaultIfUnset(@Nullable String value, String def) {
        return value != null && !value.isEmpty() ? value : def;
    }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static long getSerializedSize(Serializable object) {
        if (object == null) {
            return 0L;
        } else {
            try {
                ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);
                objectOutputStream.writeObject(object);
                objectOutputStream.flush();
                objectOutputStream.close();
                return (long)byteOutputStream.toByteArray().length;
            } catch (Exception var3) {
                return -1L;
            }
        }
    }
}
