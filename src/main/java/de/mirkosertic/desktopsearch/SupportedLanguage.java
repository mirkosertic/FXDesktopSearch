/**
 * FreeDesktopSearch - A Search Engine for your Desktop
 * Copyright (C) 2013 Mirko Sertic
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.mirkosertic.desktopsearch;

import java.util.Locale;

public enum SupportedLanguage {
    ar,
    bg,
    br,
    ca,
    ckb,
    cz,
    da,
    de,
    el,
    en,
    es,
    eu,
    fa,
    fi,
    fr,
    ga,
    gl,
    hi,
    hu,
    hy,
    id,
    it,
    lv,
    nl,
    no,
    pt,
    ro,
    ru,
    sv,
    th,
    tr,;

    public static SupportedLanguage getDefault() {
        try {
            return SupportedLanguage.valueOf(Locale.getDefault().getLanguage());
        } catch (final Exception e) {
            return SupportedLanguage.en;
        }
    }

    public Locale toLocale() {
        return new Locale(name());
    }
}