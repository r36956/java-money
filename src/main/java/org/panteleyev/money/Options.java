/*
 * Copyright (c) 2015, 2017, Petr Panteleyev <petr@panteleyev.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.panteleyev.money;

import java.io.File;
import java.util.prefs.Preferences;

class Options {
    private static final double DEFAULT_WIDTH = 1024;
    private static final double DEFAULT_HEIGHT = 768;
    private static final int    AUTO_COMPLETE_LENGTH = 3;

    private enum Option {
        DB_FILE("dbFile"),
        SHOW_DEACTIVATED_ACCOUNTS("showDeactivatedAccounts"),
        MAIN_WINDOW_WIDTH("mainWindowWidth"),
        MAIN_WINDOW_HEIGHT("mainWindowHeight"),
        AUTO_COMPLETE_LENGTH("autoCompleteLength");

        private final String s;

        Option(String s) {
            this.s = s;
        }

        public String toString() {
            return s;
        }
    }

    private static final Preferences PREFS;

    // Cached values
    private static int autoCompleteLength = AUTO_COMPLETE_LENGTH;

    static void setDbFile(File file) {
        PREFS.put(Option.DB_FILE.toString(), (file == null)? "" : file.getAbsolutePath());
    }

    static File getDbFile() {
        String path = PREFS.get(Option.DB_FILE.toString(), null);
        return (path == null || path.isEmpty()) ? null : new File(path);
    }

    static void setShowDeactivatedAccounts(boolean show) {
        PREFS.putBoolean(Option.SHOW_DEACTIVATED_ACCOUNTS.toString(), show);
    }

    static boolean getShowDeactivatedAccounts() {
        return PREFS.getBoolean(Option.SHOW_DEACTIVATED_ACCOUNTS.toString(), false);
    }

    static void setMainWindowWidth(double x) {
        PREFS.putDouble(Option.MAIN_WINDOW_WIDTH.toString(), x);
    }

    static double getMainWindowWidth() {
        return PREFS.getDouble(Option.MAIN_WINDOW_WIDTH.toString(), DEFAULT_WIDTH);
    }

    static void setMainWindowHeight(double x) {
        PREFS.putDouble(Option.MAIN_WINDOW_HEIGHT.toString(), x);
    }

    static double getMainWindowHeight() {
        return PREFS.getDouble(Option.MAIN_WINDOW_HEIGHT.toString(), DEFAULT_HEIGHT);
    }

    static int getAutoCompleteLength() {
        return autoCompleteLength;
    }

    static void setAutoCompleteLength(int x) {
        autoCompleteLength = x;
        PREFS.putInt(Option.AUTO_COMPLETE_LENGTH.toString(), x);
    }

    static {
        PREFS = Preferences.userNodeForPackage(MoneyApplication.class);

        // Load values into cache
        autoCompleteLength = PREFS.getInt(Option.AUTO_COMPLETE_LENGTH.toString(), AUTO_COMPLETE_LENGTH);
    }
}
