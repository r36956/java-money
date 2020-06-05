package org.panteleyev.money.app;

/*
 * Copyright (c) Petr Panteleyev. All rights reserved.
 * Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */

import javafx.stage.Stage;
import org.panteleyev.money.MoneyApplication;
import java.io.File;
import java.util.Optional;
import java.util.prefs.Preferences;

public class Options {
    private Options() {
    }

    private static final double DEFAULT_WIDTH = 1024.0;
    private static final double DEFAULT_HEIGHT = 768.0;
    private static final int AUTO_COMPLETE_LENGTH = 3;
    private static final String OPTIONS_DIRECTORY = ".money-manager";


    private enum Option {
        SHOW_DEACTIVATED_ACCOUNTS("showDeactivatedAccounts"),
        MAIN_WINDOW_WIDTH("mainWindowWidth"),
        MAIN_WINDOW_HEIGHT("mainWindowHeight"),
        AUTO_COMPLETE_LENGTH("autoCompleteLength"),
        YM_TOKEN("ym_token"),
        ACCOUNT_CLOSING_DAY_DELTA("accountClosingDayDelta"),
        LAST_STATEMENT_DIR("lastStatementDir"),
        LAST_EXPORT_DIR("lastExportDir");

        private final String s;

        Option(String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(MoneyApplication.class);

    // Cached values
    private static int autoCompleteLength = PREFS.getInt(Option.AUTO_COMPLETE_LENGTH.toString(), AUTO_COMPLETE_LENGTH);

    static boolean getShowDeactivatedAccounts() {
        return PREFS.getBoolean(Option.SHOW_DEACTIVATED_ACCOUNTS.toString(), false);
    }

    static void setShowDeactivatedAccounts(boolean show) {
        PREFS.putBoolean(Option.SHOW_DEACTIVATED_ACCOUNTS.toString(), show);
    }

    static double getMainWindowWidth() {
        return PREFS.getDouble(Option.MAIN_WINDOW_WIDTH.toString(), DEFAULT_WIDTH);
    }

    static void setMainWindowWidth(double x) {
        PREFS.putDouble(Option.MAIN_WINDOW_WIDTH.toString(), x);
    }

    static double getMainWindowHeight() {
        return PREFS.getDouble(Option.MAIN_WINDOW_HEIGHT.toString(), DEFAULT_HEIGHT);
    }

    static void setMainWindowHeight(double x) {
        PREFS.putDouble(Option.MAIN_WINDOW_HEIGHT.toString(), x);
    }

    public static int getAutoCompleteLength() {
        return autoCompleteLength;
    }

    static void setAutoCompleteLength(int x) {
        autoCompleteLength = x;
        PREFS.putInt(Option.AUTO_COMPLETE_LENGTH.toString(), x);
    }

    public static void setLastStatementDir(String dir) {
        PREFS.put(Option.LAST_STATEMENT_DIR.toString(), dir);
    }

    public static String getLastStatementDir() {
        return PREFS.get(Option.LAST_STATEMENT_DIR.toString(), "");
    }

    public static void setYandexMoneyToken(String token) {
        PREFS.put(Option.YM_TOKEN.toString(), token);
    }

    public static String getYandexMoneyToken() {
        return PREFS.get(Option.YM_TOKEN.toString(), "");
    }

    static Optional<File> getLastExportDir() {
        var dir = PREFS.get(Option.LAST_EXPORT_DIR.toString(), null);
        return dir == null || dir.isEmpty() ? Optional.empty() : Optional.of(new File(dir));
    }

    static void setLastExportDir(String dir) {
        PREFS.put(Option.LAST_EXPORT_DIR.toString(), dir);
    }

    static int getAccountClosingDayDelta() {
        return PREFS.getInt(Option.ACCOUNT_CLOSING_DAY_DELTA.toString(), 10);
    }

    static void setAccountClosingDayDelta(int delta) {
        PREFS.putInt(Option.ACCOUNT_CLOSING_DAY_DELTA.toString(), delta);
    }

    static void saveStageDimensions(Class<?> parentClass, Stage stage) {
        var key = parentClass.getSimpleName().toLowerCase() + "_size";
        var value = String.format("%d;%d;%d;%d",
            (int)stage.getX(),
            (int)stage.getY(),
            (int)stage.getWidth(),
            (int)stage.getHeight());
        PREFS.put(key, value);
    }

    static void loadStageDimensions(Class<?> parentClass, Stage stage) {
        var key = parentClass.getSimpleName().toLowerCase() + "_size";
        var parts = PREFS.get(key, "").split(";");
        if (parts.length != 4) {
            return;
        }

        stage.setX(Double.parseDouble(parts[0]));
        stage.setY(Double.parseDouble(parts[1]));
        stage.setWidth(Double.parseDouble(parts[2]));
        stage.setHeight(Double.parseDouble(parts[3]));
    }

    public static File getSettingsDirectory() {
        var dir = new File(System.getProperty("user.home") + File.separator + OPTIONS_DIRECTORY);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                throw new RuntimeException("Options directory cannot be opened/created");
            }
        } else {
            if (!dir.isDirectory()) {
                throw new RuntimeException("Options directory cannot be opened/created");
            }
        }

        return dir;
    }
}