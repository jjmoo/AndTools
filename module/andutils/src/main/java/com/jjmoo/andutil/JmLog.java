package com.jjmoo.andutil;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * @author Zohn
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class JmLog {
    private static final String TAG = "JmLog";

    public static final int LEVEL_D = Log.DEBUG;
    public static final int LEVEL_I = Log.INFO;
    public static final int LEVEL_W = Log.WARN;
    public static final int LEVEL_E = Log.ERROR;

    private static final int JAVA_LOG_LEVEL_INFO = 800;
    private static final int JAVA_LOG_LEVEL_WARNING = 900;
    private static final int JAVA_LOG_LEVEL_SEVERE = 1000;

    private static boolean sDebug;
    private static String sPrefix = TAG;
    private static Logger sLogger = new Logger(){};

    public static void setup(Context context, String tagPrefix, boolean debug, boolean saveToFile) {
        sDebug = debug;
        if (null != tagPrefix && !tagPrefix.isEmpty() && TAG.equals(sPrefix)) {
            sPrefix = tagPrefix;
        }
        try {
            Class.forName("android.util.Log");
            if (!(sLogger instanceof AndroidLogger)) {
                sLogger = new AndroidLogger(context, saveToFile);
            }
        } catch (Exception e) {
            sLogger = new JavaLogger();
        }
    }

    public static void d(String tag, Object msg) {
        sLogger.log(LEVEL_D, tag, msg);
    }

    public static void d(String tag, Object msg, Throwable th) {
        sLogger.log(LEVEL_D, tag, msg, th);
    }

    public static void i(String tag, Object msg) {
        sLogger.log(LEVEL_I, tag, msg);
    }

    public static void i(String tag, Object msg, Throwable th) {
        sLogger.log(LEVEL_I, tag, msg, th);
    }

    public static void w(String tag, Object msg) {
        sLogger.log(LEVEL_W, tag, msg);
    }

    public static void w(String tag, Object msg, Throwable th) {
        sLogger.log(LEVEL_W, tag, msg, th);
    }

    public static void e(String tag, Object msg) {
        sLogger.log(LEVEL_E, tag, msg);
    }

    public static void e(String tag, Object msg, Throwable th) {
        sLogger.log(LEVEL_E, tag, msg, th);
    }

    public static void log(int level, String tag, Object msg) {
        sLogger.log(level, tag, msg);
    }

    public static void log(int level, String tag, Object msg, Throwable th) {
        sLogger.log(level, tag, msg, th);
    }

    public static void log(Level level, String tag, Object msg) {
        if (Level.OFF != level) {
            sLogger.log(mapLevel(level), tag, msg);
        }
    }

    public static void log(Level level, String tag, Object msg, Throwable th) {
        if (Level.OFF != level) {
            sLogger.log(mapLevel(level), tag, msg, th);
        }
    }

    public interface Logger {
        /**
         * print a log of specific level
         * @param level android log level
         * @param tag tag
         * @param msg msg
         */
        default void log(int level, String tag, Object msg) {
            throw new IllegalStateException("Please call JmLog.setup() first!");
        }

        /**
         * print a log of specific level with throwable
         * @param level android log level
         * @param tag tag
         * @param msg msg
         * @param th throwable
         */
        default void log(int level, String tag, Object msg, Throwable th) {
            throw new IllegalStateException("Please call JmLog.setup() first!");
        }
    }

    public static class AndroidLogger implements Logger {
        private final boolean mSaveFile;
        private File mFileOut = null;

        public AndroidLogger(Context context, boolean saveToFile) {
            mSaveFile = saveToFile;
            String processName = Utils.getProcessName(context, android.os.Process.myPid());
            String nick = processName.contains(":") ? processName.split(":")[1] : "default";
            if (mSaveFile) {
                File folder = new File(context.getFilesDir() + "/log");
                if (folder.exists() || folder.mkdirs()) {
                    mFileOut = new File(folder + "/" + nick + "-" +
                            Utils.FILE_FORMAT.format(new Date()) + ".log");
                } else {
                    Log.e(getTag(TAG), "AndroidLogger # failed to create folder: " + folder);
                }
                Executor.runOnBgThreadDelayed(this::checkStorage, 60_000);
            }
            modifyPrefix(nick);
        }

        @Override
        public void log(int level, String tag, Object msg) {
            if (sDebug || level > LEVEL_D) {
                switch (level) {
                    case LEVEL_I:
                        Log.i(getTag(tag), getMsg(msg));
                        break;
                    case LEVEL_W:
                        Log.w(getTag(tag), getMsg(msg));
                        break;
                    case LEVEL_E:
                        Log.e(getTag(tag), getMsg(msg));
                        break;
                    default:
                        Log.d(getTag(tag), getMsg(msg));
                        break;
                }
            }
            if (mSaveFile && null != mFileOut) {
                Executor.runOnBgThread(() -> writeToFile(level, getTag(tag), getMsg(msg)));
            }
        }

        @Override
        public void log(int level, String tag, Object msg, Throwable th) {
            if (sDebug || level > LEVEL_D) {
                switch (level) {
                    case LEVEL_I:
                        Log.i(getTag(tag), getMsg(msg), th);
                        break;
                    case LEVEL_W:
                        Log.w(getTag(tag), getMsg(msg), th);
                        break;
                    case LEVEL_E:
                        Log.e(getTag(tag), getMsg(msg), th);
                        break;
                    default:
                        Log.d(getTag(tag), getMsg(msg), th);
                        break;
                }
            }
            if (mSaveFile && null != mFileOut) {
                Executor.runOnBgThread(() -> writeToFile(level, getTag(tag), getMsg(msg), th));
            }
        }

        private synchronized void writeToFile(int level, String tag, String msg) {
            try (FileWriter fw = new FileWriter(mFileOut, true)) {
                fw.append(getReadableLog(level, tag, msg)).append('\n');
                fw.flush();
            } catch (IOException e) {
                Log.w(getTag(TAG), "writeToFile # failed to write file: " + mFileOut);
            }
        }

        private synchronized void writeToFile(int level, String tag, String msg, Throwable th) {
            try (FileWriter fw = new FileWriter(mFileOut, true)) {
                fw.append(getReadableLog(level, tag, msg)).append('\n');
                for (String line : getReadableLogs(level, tag, msg, th)) {
                    fw.append(line).append('\n');
                }
                fw.flush();
            } catch (IOException e) {
                Log.w(getTag(TAG), "writeToFile # failed to write file: " + mFileOut);
            }
        }

        private void modifyPrefix(String nickName) {
            String prefix = nickName;
            int len = prefix.length();
            int expectLen = 4;
            if (expectLen != len) {
                if (expectLen - 1 - 1 == len) {
                    prefix = "-" + prefix + "-";
                } else if (len > expectLen) {
                    prefix = prefix.substring(0, expectLen);
                } else if (1 == len) {
                    prefix = "-" + prefix + "--";
                } else {
                    prefix = prefix + "-";
                }
            }
            sPrefix = "<" + prefix + "> " + sPrefix;
        }

        private void checkStorage() {
            // TODO by zhuo.peng
        }
    }

    public static class JavaLogger implements Logger {
        @Override
        public void log(int level, String tag, Object msg) {
            System.out.println(getReadableLog(level, getTag(tag), getMsg(msg)));
        }

        @Override
        public void log(int level, String tag, Object msg, Throwable th) {
            for (String line : getReadableLogs(level, getTag(tag), getMsg(msg), th)) {
                System.out.println(line);
            }
        }
    }

    private static int mapLevel(Level level) {
        int value = level.intValue();
        if (value < JAVA_LOG_LEVEL_INFO) {
            return LEVEL_D;
        } else if (value < JAVA_LOG_LEVEL_WARNING) {
            return LEVEL_I;
        } else if (value < JAVA_LOG_LEVEL_SEVERE) {
            return LEVEL_W;
        } else {
            return LEVEL_E;
        }
    }

    private static String getTag(String rawTag) {
        return sPrefix.concat("/").concat(rawTag);
    }

    private static String getMsg(Object msgObj) {
        if (null == msgObj) {
            return "### null msg ###";
        } else {
            String msg = msgObj.toString();
            if (msg.isEmpty()) {
                return "### empty msg ###";
            } else {
                return msg;
            }
        }
    }

    private static String getReadableLog(int level, String tag, String msg) {
        return String.format(Locale.US, "%s  %s  %s:  %s",
                Utils.DATE_FORMAT.format(new Date()), getLevelName(level), tag, msg
        );
    }

    private static List<String> getReadableLogs(int level, String tag, String msg, Throwable th) {
        List<String> lines = new ArrayList<>();
        lines.add(getReadableLog(level, tag, msg));
        String lineSeparator = "\n";
        for (String line : Utils.getStackTrace(th).split(lineSeparator)) {
            lines.add(getReadableLog(level, tag, line));
        }
        return lines;
    }

    private static String getLevelName(int level) {
        switch (level) {
            case LEVEL_D:
                return "D";
            case LEVEL_I:
                return "I";
            case LEVEL_W:
                return "W";
            case LEVEL_E:
            default:
                return "E";
        }
    }

    public static class Utils {
        @SuppressLint("ConstantLocale")
        public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
                "MM-dd HH:mm:ss.SSS", Locale.getDefault());
        @SuppressLint("ConstantLocale")
        public static final SimpleDateFormat FILE_FORMAT = new SimpleDateFormat(
                "yyyyMMdd-HHmm-ssSSS", Locale.getDefault());

        public static String getSimpleName(Object obj) {
            return null == obj ? null : obj.getClass().getSimpleName();
        }

        public static String getProcessName(Context context, int pid) {
            ActivityManager am = (ActivityManager) context
                    .getSystemService(Context.ACTIVITY_SERVICE);
            if (null != am) {
                List<ActivityManager.RunningAppProcessInfo> processInfoList =
                        am.getRunningAppProcesses();
                if (null != processInfoList) {
                    for (ActivityManager.RunningAppProcessInfo appProcess : processInfoList) {
                        if (appProcess.pid == pid) {
                            return appProcess.processName;
                        }
                    }
                }
            }
            return "NA";
        }

        public static String getStackTrace(Throwable e) {
            try (StringWriter sw = new StringWriter();
                 PrintWriter pw = new PrintWriter(sw)) {
                e.printStackTrace(pw);
                pw.flush();
                sw.flush();
                return sw.toString();
            } catch (IOException ex) {
                throw new RuntimeException(e);
            }
        }

        public static void enterMethod() {
            logEnterMethod(null);
        }

        public static void enterMethod(String name1, Object arg1) {
            if (null != name1) {
                logEnterMethod(name1 + "=" + String.valueOf(arg1));
            } else {
                logEnterMethod(null);
            }
        }

        public static void enterMethod(String name1, Object arg1, String name2, Object arg2) {
            if (null != name1 && null != name2) {
                logEnterMethod(name1 + "=" + String.valueOf(arg1)
                        + ", " + name2 + "=" + String.valueOf(arg2));
            } else {
                logEnterMethod(null);
            }
        }

        public static void exitMethod() {
            logExitMethod(null);
        }

        public static void exitMethod(String extra) {
            logExitMethod(extra);
        }

        private static void logEnterMethod(String extra) {
            if (sDebug) {
                StackTraceElement traceElement = new Exception().getStackTrace()[2];
                String className = traceElement.getClassName();
                int index = className.lastIndexOf('.');
                int indexDollar = className.indexOf('$');
                if (indexDollar >= 0) {
                    char ch = className.charAt(indexDollar + 1);
                    char zero = '0';
                    char nine = '9';
                    if (ch < zero || ch > nine) {
                        index = index > indexDollar ? index : indexDollar;
                    }
                }
                String tag = className.substring(index + 1);
                if (null != extra) {
                    d(tag, traceElement.getMethodName() + " # ENTER ... " + extra);
                } else {
                    d(tag, traceElement.getMethodName() + " # ENTER ...");
                }
            }
        }

        private static void logExitMethod(String extra) {
            if (sDebug) {
                StackTraceElement traceElement = new Exception().getStackTrace()[2];
                String className = traceElement.getClassName();
                String tag = className.substring(className.lastIndexOf('.') + 1);
                if (null != extra) {
                    d(tag, traceElement.getMethodName() + " # EXIT ... " + extra);
                } else {
                    d(tag, traceElement.getMethodName() + " # EXIT ...");
                }
            }
        }
    }
}
